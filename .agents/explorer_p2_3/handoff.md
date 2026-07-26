# Handoff Report — Phase 2 REST API Controllers, OpenAPI Swagger & Test Architecture Design

## 1. Observation

Direct observations from the `omnicare-emr-api` codebase:

### 1.1 REST Controllers & OpenAPI Configuration
- **Existing Controllers**:
  - `PatientController.java` (`src/main/java/com/omnicare/emr/controller/PatientController.java`):
    - Annotations: `@RestController`, `@Tag(name = "Patient", description = "Patient Management APIs")`, `@RequestMapping("/api/v1/patients")`, `@RequiredArgsConstructor` (lines 22-26).
    - Endpoints: `POST /api/v1/patients` (`@Operation`, `@ApiResponses`, `@Valid @RequestBody PatientRequestDto`).
  - `PractitionerController.java` (`src/main/java/com/omnicare/emr/controller/PractitionerController.java`):
    - Annotations: `@RestController`, `@Tag(name = "Practitioner Management", description = "APIs for managing healthcare practitioners...")`, `@RequestMapping("/api/v1/practitioners")`, `@RequiredArgsConstructor` (lines 31-35).
    - Endpoints: `POST`, `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` with `@Operation`, `@Parameter`, and `@ApiResponses` (lines 45-132).
- **OpenAPI Setup**:
  - `OpenApiConfig.java` (`src/main/java/com/omnicare/emr/config/OpenApiConfig.java`): Configures `OpenAPI` bean titled "OmniCare EMR API" version `v1.0.0` (lines 13-19).
  - Dependency: `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0` in `pom.xml` (lines 85-89).

### 1.2 Exception Handling & RFC 7807
- `GlobalExceptionHandler.java` (`src/main/java/com/omnicare/emr/exception/GlobalExceptionHandler.java`):
  - Extends `ResponseEntityExceptionHandler` (line 16).
  - Uses Spring Boot 3 / Spring 6 `ProblemDetail` (RFC 7807) for `ResourceNotFoundException` (404), `DuplicateResourceException` (409), `DataIntegrityViolationException` (409), and `Exception` (500) (lines 18-48).
  - Configured in `application.yml` (`src/main/resources/application.yml`): `spring.mvc.problemdetails.enabled: true` (lines 27-29).

### 1.3 Dependencies & Build Configuration (`pom.xml`)
- Spring Boot `3.3.0`, Java 17, Hibernate 6.5.x.
- MapStruct `1.5.5.Final`, Lombok `1.18.36`.
- Test Scope currently contains only `org.springframework.boot:spring-boot-starter-test` (lines 60-64).
- Database driver: `org.postgresql:postgresql` (runtime scope). Currently lacks `com.h2database:h2` or `org.testcontainers:postgresql` in `pom.xml`.

### 1.4 Test Setup & Conventions
- Unit tests exist in `src/test/java/com/omnicare/emr/controller/` and `src/test/java/com/omnicare/emr/service/`:
  - `PatientControllerTest.java` uses `@WebMvcTest(PatientController.class)`, `@Import(GlobalExceptionHandler.class)`, `@MockBean PatientService`, `MockMvc`, Jackson `ObjectMapper`, and `jsonPath(...)` assertions for status codes (201, 400, 409).
  - `PatientServiceImplTest.java` uses JUnit 5 + Mockito.

---

## 2. Logic Chain

1. **Controller Architecture Alignment**:
   - Phase 1 established clear controller patterns: `@RestController`, `@Tag`, `@RequestMapping`, DTO validation via `@Valid`, constructor injection via `@RequiredArgsConstructor`, and OpenAPI documentation using `@Operation` & `@ApiResponses`.
   - `EncounterController` and `ObservationController` must follow identical design patterns to maintain uniform API design, HTTP status code semantics, and Swagger UI documentation.

2. **Phase 2 API Endpoint Design**:
   - `EncounterController` (`/api/v1/encounters`):
     - `POST /api/v1/encounters` -> Delegates to `encounterService.createEncounter(...)`. Returns HTTP 201 Created with `EncounterResponseDto`.
     - `GET /api/v1/encounters` -> Delegates to `encounterService.getAllEncounters()`. Returns HTTP 200 OK with `List<EncounterResponseDto>`.
     - `GET /api/v1/encounters/{id}` -> Delegates to `encounterService.getEncounterById(id)`. Returns HTTP 200 OK or throws `ResourceNotFoundException` (mapped to HTTP 404).
   - `ObservationController` (`/api/v1/observations`):
     - `POST /api/v1/observations` -> Delegates to `observationService.createObservation(...)`. Returns HTTP 201 Created with `ObservationResponseDto`.
       - Validates encounter existence (404 if missing via `ResourceNotFoundException`).
       - Validates encounter status (400 or 409 if status is `CANCELLED` via `EncounterCancelledException`).
     - `GET /api/v1/observations?encounterId={id}` -> Delegates to `observationService.getObservationsByEncounterId(id)`. Returns HTTP 200 OK with `List<ObservationResponseDto>`.

3. **RFC 7807 Error Response Expansion**:
   - To support the clinical business rule violation ("Cannot record observation for CANCELLED encounter"), `GlobalExceptionHandler` must be extended with:
     ```java
     @ExceptionHandler(EncounterCancelledException.class)
     public ProblemDetail handleEncounterCancelledException(EncounterCancelledException ex) {
         ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
         problemDetail.setTitle("Encounter Cancelled");
         problemDetail.setType(URI.create("https://api.omnicare.com/errors/encounter-cancelled"));
         return problemDetail;
     }
     ```

4. **Test Architecture & JSONB Compatibility Design**:
   - **Database Strategy for Testing**:
     - *In-Memory H2 DB*: Add `com.h2database:h2` (scope: `test`) to `pom.xml`. H2 2.x natively supports `JSON` data type. In Hibernate 6, `@JdbcTypeCode(SqlTypes.JSON)` transparently maps `Map<String, Object>` or Jackson `JsonNode` to H2 `JSON` columns.
     - *H2 Property Configuration*: `src/test/resources/application-test.yml` should set `MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1` and `spring.flyway.enabled=false` (or provide H2-compatible Flyway migrations).
     - *PostgreSQL Testcontainers*: Recommended secondary integration tier using `org.testcontainers:postgresql` for 100% native PostgreSQL `JSONB` schema validation in CI/CD pipeline.
   - **Unit Test Layer**:
     - `EncounterControllerTest`: `@WebMvcTest(EncounterController.class)` testing status codes 201, 200, 404, 400.
     - `ObservationControllerTest`: `@WebMvcTest(ObservationController.class)` testing status codes 201, 200, 404, 400, 409.
     - `EncounterServiceTest`: `@ExtendWith(MockitoExtension.class)` testing patient/practitioner lookups, default status assignment (`PLANNED`), persistence logic.
     - `ObservationServiceTest`: `@ExtendWith(MockitoExtension.class)` testing active vs. cancelled encounter checks, JSON payload mapping.
   - **Integration Test Layer**:
     - `EncounterIntegrationTest`: Full Spring context tests verifying REST endpoint execution against test DB.
     - `ObservationIntegrationTest`: End-to-end integration tests verifying:
       1. Vitals JSON payload preservation: `{"bloodPressure": "120/80", "heartRate": 75, "temp": 37.0}` stored and retrieved without data corruption or key re-ordering issues.
       2. RFC 7807 response verification for cancelled encounters (HTTP 400 / 409 with `Content-Type: application/problem+json`).

---

## 3. Caveats

1. **H2 vs PostgreSQL `JSONB` Dialect Differences**:
   - While H2 2.x supports `JSON` data type, PostgreSQL supports `JSONB` (binary format with indexing). Standard `@JdbcTypeCode(SqlTypes.JSON)` works seamlessly across both in Hibernate 6. However, native PostgreSQL SQL queries containing PostgreSQL-specific operators (e.g., `->`, `->>`) will not run against H2 without custom functions. Since Phase 2 uses JPA/Hibernate entities, standard `@JdbcTypeCode` handles cross-database compatibility transparently.
2. **Flyway Execution in Tests**:
   - If Flyway migrations (`V1`, `V2`, `V3`) contain PostgreSQL-specific DDL syntax (such as `VARCHAR(32)` vs `UUID` casting or PostgreSQL extensions), running Flyway against H2 may fail unless H2 PostgreSQL compatibility mode is active or Flyway is disabled in `application-test.yml` with `spring.jpa.hibernate.ddl-auto: create-drop`.
3. **Pom.xml Modifications Required**:
   - The current `pom.xml` does not contain `h2` or `testcontainers`. Implementing agents will need to add `<dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>test</scope></dependency>` to `pom.xml`.

---

## 4. Conclusion & Complete Design Specifications

### 4.1 `EncounterController.java` Blueprint
```java
package com.omnicare.emr.controller;

import com.omnicare.emr.dto.EncounterRequestDto;
import com.omnicare.emr.dto.EncounterResponseDto;
import com.omnicare.emr.service.EncounterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing clinical encounters.
 */
@RestController
@Tag(name = "Encounters", description = "Clinical Encounter Management APIs")
@RequestMapping("/api/v1/encounters")
@RequiredArgsConstructor
public class EncounterController {

    private final EncounterService encounterService;

    /**
     * Endpoint to create a new planned clinical encounter.
     *
     * @param request JSON payload containing encounter details
     * @return 201 Created status with created encounter details
     */
    @Operation(summary = "Create a new clinical encounter", description = "Registers a new clinical encounter for a patient and practitioner. Sets status to PLANNED by default if omitted.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Encounter created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body or validation failure"),
            @ApiResponse(responseCode = "404", description = "Referenced Patient or Practitioner not found")
    })
    @PostMapping
    public ResponseEntity<EncounterResponseDto> createEncounter(@Valid @RequestBody EncounterRequestDto request) {
        EncounterResponseDto response = encounterService.createEncounter(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint to retrieve all clinical encounters.
     *
     * @return 200 OK status with list of active encounters
     */
    @Operation(summary = "Get all encounters", description = "Retrieves a list of all active clinical encounters.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Encounters retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<EncounterResponseDto>> getAllEncounters() {
        List<EncounterResponseDto> response = encounterService.getAllEncounters();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to fetch a specific encounter by UUID.
     *
     * @param id UUID of the encounter
     * @return 200 OK status with encounter details
     */
    @Operation(summary = "Get encounter by ID", description = "Fetches details of a specific clinical encounter by its UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Encounter retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Encounter not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<EncounterResponseDto> getEncounterById(
            @Parameter(description = "UUID of the encounter", required = true)
            @PathVariable("id") UUID id) {
        EncounterResponseDto response = encounterService.getEncounterById(id);
        return ResponseEntity.ok(response);
    }
}
```

### 4.2 `ObservationController.java` Blueprint
```java
package com.omnicare.emr.controller;

import com.omnicare.emr.dto.ObservationRequestDto;
import com.omnicare.emr.dto.ObservationResponseDto;
import com.omnicare.emr.service.ObservationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing clinical observations and vitals.
 */
@RestController
@Tag(name = "Observations", description = "Clinical Observation and Vitals Management APIs")
@RequestMapping("/api/v1/observations")
@RequiredArgsConstructor
public class ObservationController {

    private final ObservationService observationService;

    /**
     * Endpoint to record a new clinical observation / vitals record.
     *
     * @param request JSON payload containing encounter ID and observation JSON payload
     * @return 201 Created status with saved observation details
     */
    @Operation(summary = "Record clinical observation / vitals", description = "Records a new observation or vitals JSON payload linked to an active encounter. Fails if the encounter is CANCELLED or not found.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Observation recorded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or encounter is CANCELLED"),
            @ApiResponse(responseCode = "404", description = "Referenced encounter not found"),
            @ApiResponse(responseCode = "409", description = "Conflict due to cancelled encounter status")
    })
    @PostMapping
    public ResponseEntity<ObservationResponseDto> createObservation(@Valid @RequestBody ObservationRequestDto request) {
        ObservationResponseDto response = observationService.createObservation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint to retrieve observations filtered by encounter ID.
     *
     * @param encounterId UUID of the encounter
     * @return 200 OK status with list of observations for the encounter
     */
    @Operation(summary = "Get observations by encounter ID", description = "Retrieves all clinical observations recorded for a specific encounter ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Observations retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Referenced encounter not found")
    })
    @GetMapping
    public ResponseEntity<List<ObservationResponseDto>> getObservationsByEncounterId(
            @Parameter(description = "UUID of the encounter to filter observations", required = true)
            @RequestParam("encounterId") UUID encounterId) {
        List<ObservationResponseDto> response = observationService.getObservationsByEncounterId(encounterId);
        return ResponseEntity.ok(response);
    }
}
```

### 4.3 Test Architecture Plan

#### Dependencies & Test Properties
- Add H2 Database dependency to `pom.xml`:
  ```xml
  <dependency>
      <groupId>com.h2database</groupId>
      <artifactId>h2</artifactId>
      <scope>test</scope>
  </dependency>
  ```
- Configure `src/test/resources/application-test.yml`:
  ```yaml
  spring:
    datasource:
      url: jdbc:h2:mem:omnicare_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1
      driver-class-name: org.h2.Driver
      username: sa
      password:
    jpa:
      database-platform: org.hibernate.dialect.H2Dialect
      hibernate:
        ddl-auto: create-drop
      show-sql: true
    flyway:
      enabled: false
  ```

#### Unit Test Classes Plan
1. `EncounterControllerTest.java` (`src/test/java/com/omnicare/emr/controller/EncounterControllerTest.java`):
   - `@WebMvcTest(EncounterController.class)`, `@Import(GlobalExceptionHandler.class)`
   - `createEncounter_Success`: POST `/api/v1/encounters` -> 201 Created, verifies response body fields.
   - `createEncounter_PatientNotFound_Returns404`: Mock service throws `ResourceNotFoundException("Patient not found")` -> 404 Not Found (ProblemDetail format).
   - `createEncounter_MissingFields_Returns400`: POST empty request body -> 400 Bad Request.
   - `getAllEncounters_Success`: GET `/api/v1/encounters` -> 200 OK with JSON array.
   - `getEncounterById_Success`: GET `/api/v1/encounters/{id}` -> 200 OK.
   - `getEncounterById_NotFound_Returns404`: GET `/api/v1/encounters/{id}` -> 404 Not Found.

2. `ObservationControllerTest.java` (`src/test/java/com/omnicare/emr/controller/ObservationControllerTest.java`):
   - `@WebMvcTest(ObservationController.class)`, `@Import(GlobalExceptionHandler.class)`
   - `createObservation_Success`: POST `/api/v1/observations` with JSON payload `{"bloodPressure": "120/80", "heartRate": 75, "temp": 37.0}` -> 201 Created.
   - `createObservation_EncounterNotFound_Returns404`: Mock service throws `ResourceNotFoundException("Encounter not found")` -> 404 Not Found.
   - `createObservation_CancelledEncounter_Returns400`: Mock service throws `EncounterCancelledException("Cannot record observation for CANCELLED encounter")` -> 400 Bad Request ProblemDetail (`type: https://api.omnicare.com/errors/encounter-cancelled`).
   - `getObservationsByEncounterId_Success`: GET `/api/v1/observations?encounterId={id}` -> 200 OK.

3. `EncounterServiceImplTest.java` & `ObservationServiceImplTest.java`:
   - Mockito unit tests verifying business rules in isolated service layer.

#### Integration Test Classes Plan
1. `EncounterIntegrationTest.java` (`src/test/java/com/omnicare/emr/integration/EncounterIntegrationTest.java`):
   - `@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`
   - Verifies full flow: seed patient + practitioner in test DB, POST encounter, GET all encounters, GET encounter by ID.

2. `ObservationIntegrationTest.java` (`src/test/java/com/omnicare/emr/integration/ObservationIntegrationTest.java`):
   - `@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`
   - **JSONB Preservation Verification**:
     - POST `/api/v1/observations` with body:
       ```json
       {
         "encounterId": "<active-encounter-id>",
         "valueJson": {
           "bloodPressure": "120/80",
           "heartRate": 75,
           "temp": 37.0
         }
       }
       ```
     - Assert 201 Created.
     - GET `/api/v1/observations?encounterId=<active-encounter-id>`.
     - Assert 200 OK. Verify JSON equality: `$.valueJson.bloodPressure` = `"120/80"`, `$.valueJson.heartRate` = `75`, `$.valueJson.temp` = `37.0`.
   - **Clinical Business Rule Verification**:
     - Seed a `CANCELLED` encounter.
     - POST `/api/v1/observations` targeting `CANCELLED` encounter.
     - Assert status 400 Bad Request, body header `Content-Type: application/problem+json`, `type: "https://api.omnicare.com/errors/encounter-cancelled"`, `detail: "Cannot record observation for CANCELLED encounter"`.

---

## 5. Verification Method

To verify the API Layer and Test Strategy after implementation:
1. **Compilation & Packaging**:
   - Run `mvn clean test-compile` in `omnicare-emr-api`. Ensure zero compilation errors.
2. **Execute Full Test Suite**:
   - Run `mvn test` in `omnicare-emr-api`.
   - All unit tests (`PatientControllerTest`, `PractitionerControllerTest`, `EncounterControllerTest`, `ObservationControllerTest`, `EncounterServiceImplTest`, `ObservationServiceImplTest`) must pass.
   - All integration tests (`EncounterIntegrationTest`, `ObservationIntegrationTest`) must pass.
3. **OpenAPI / Swagger Inspection**:
   - Start the application via `mvn spring-boot:run` and access `http://localhost:8080/v3/api-docs` or `http://localhost:8080/swagger-ui.html`.
   - Verify `Encounters` and `Observations` tags appear with correct operation descriptions and schemas.
4. **Invalidation Conditions**:
   - Any test failure in JSON payload preservation (`valueJson`).
   - Failure to return RFC 7807 `ProblemDetail` on cancelled encounter observation creation.
   - H2 dialect serialization error with `@JdbcTypeCode(SqlTypes.JSON)`.
