# API Layer Architecture & Analysis: Practitioner API Component Design

**Author**: Explorer 3 (DTOs, Mapper, Service, REST Controller & OpenAPI Analyst)  
**Target Project**: `omnicare-emr-api`  
**Date**: 2026-07-25  

---

## 1. Executive Summary & Objective

This document presents the complete architectural analysis, design specifications, and proposed production Java implementations for the **Practitioner API Layer** in `omnicare-emr-api`.

Following clean architecture principles and existing project conventions (seen in `PatientController`, `PatientService`, `PatientMapper`, and `GlobalExceptionHandler`), the Practitioner API layer comprises:
1. `ResourceNotFoundException`: Standard runtime exception for missing or soft-deleted resources.
2. `GlobalExceptionHandler`: RFC 7807 `ProblemDetail` mapping for `DuplicateResourceException` (409 Conflict) and `ResourceNotFoundException` (404 Not Found).
3. `PractitionerRequestDto`: Request payload DTO with Jakarta Bean Validation and OpenAPI `@Schema` annotations.
4. `PractitionerResponseDto`: Response payload DTO containing all entity domain fields plus audit metadata (`id`, `createdAt`, `updatedAt`, `version`, `isDeleted`).
5. `PractitionerMapper`: MapStruct interface for entity <-> DTO conversions and entity in-place updates.
6. `PractitionerService` & `PractitionerServiceImpl`: Service layer interface and implementation handling duplicate practitioner code validation, soft-delete filtering, and transaction management.
7. `PractitionerController`: Spring Web REST Controller exposing `/api/v1/practitioners` endpoints (`POST`, `GET`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}`) decorated with OpenAPI annotations.

---

## 2. Analysis of Existing Codebase Patterns

### 2.1 DTO Validation and OpenAPI Annotations
- **Existing Request Pattern** (`PatientRequestDto.java`): Uses Lombok `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` alongside `jakarta.validation.constraints` annotations (`@NotBlank`, `@Size`, `@PastOrPresent`).
- **Existing Response Pattern** (`PatientResponseDto.java`): Maps primary keys, audit fields (`createdAt`, `updatedAt`, `version`), and soft delete status (`@JsonProperty("isDeleted") private boolean isDeleted`).
- **OpenAPI Schema Integration**: Enhances DTOs with `io.swagger.v3.oas.annotations.media.Schema` annotations to produce clear, structured Swagger UI API documentation.

### 2.2 Mapper Component Architecture
- **MapStruct Pattern** (`PatientMapper.java`):
  - Interface annotated with `@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)`.
  - Converts request DTOs to JPA entities and JPA entities to response DTOs cleanly without manual boilerplate.
  - Extended for `PractitionerMapper` to include in-place entity updates via `@MappingTarget`.

### 2.3 Service Layer Architecture
- **Existing Pattern** (`PatientService.java` / `PatientServiceImpl.java`):
  - Interface defined under `com.omnicare.emr.service`.
  - Implementation class located under `com.omnicare.emr.service.impl`, annotated with `@Service` and `@RequiredArgsConstructor`.
  - Read/Write business logic wrapped with `@Transactional` (and read operations optionally `@Transactional(readOnly = true)`).
  - Business validations check identifier existence in repository prior to saving; throws custom runtime exceptions (e.g. `DuplicateResourceException`).

### 2.4 REST Controller & OpenAPI Annotations
- **Existing Pattern** (`PatientController.java`):
  - Path prefix: `/api/v1/patients`.
  - Class level annotations: `@RestController`, `@Tag(name = "...", description = "...")`, `@RequestMapping(...)`, `@RequiredArgsConstructor`.
  - Method level annotations: `@Operation`, `@ApiResponses`, `@ApiResponse`, `@Valid`, `@RequestBody`, `@PathVariable`.
  - HTTP Status Codes: `201 Created` for resource creation, `200 OK` for retrieval/update, `204 No Content` for deletion.

### 2.5 Exception Handling Strategy & RFC 7807 Compliance
- **Existing Pattern** (`GlobalExceptionHandler.java`):
  - Class extends `ResponseEntityExceptionHandler` and uses `@RestControllerAdvice`.
  - Methods return Spring 6 / Spring Boot 3 `org.springframework.http.ProblemDetail`.
  - Custom exceptions (`DuplicateResourceException`) set status `HttpStatus.CONFLICT` (409), set title `"Duplicate Resource"`, and set type `URI.create("https://api.omnicare.com/errors/duplicate-resource")`.
  - To support soft-deletion and missing entity queries, `ResourceNotFoundException` needs to be defined and mapped to `HttpStatus.NOT_FOUND` (404) with title `"Resource Not Found"` and type `URI.create("https://api.omnicare.com/errors/resource-not-found")`.

---

## 3. Clean Architecture API Layer Design & Specifications

### 3.1 `ResourceNotFoundException`
- **Package**: `com.omnicare.emr.exception`
- **File**: `omnicare-emr-api/src/main/java/com/omnicare/emr/exception/ResourceNotFoundException.java`

```java
package com.omnicare.emr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when a requested resource cannot be found or is soft-deleted.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

---

### 3.2 `GlobalExceptionHandler` Update Specification
- **Package**: `com.omnicare.emr.exception`
- **File**: `omnicare-emr-api/src/main/java/com/omnicare/emr/exception/GlobalExceptionHandler.java`

```java
package com.omnicare.emr.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;

/**
 * Controller advice for handling global application exceptions using RFC 7807 (ProblemDetail).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Resource Not Found");
        problemDetail.setType(URI.create("https://api.omnicare.com/errors/resource-not-found"));
        return problemDetail;
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicateResourceException(DuplicateResourceException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Duplicate Resource");
        problemDetail.setType(URI.create("https://api.omnicare.com/errors/duplicate-resource"));
        return problemDetail;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolationException(DataIntegrityViolationException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Duplicate entity or data integrity violation");
        problemDetail.setTitle("Data Integrity Violation");
        problemDetail.setType(URI.create("https://api.omnicare.com/errors/data-integrity-violation"));
        return problemDetail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred"
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setType(URI.create("https://api.omnicare.com/errors/internal-server-error"));
        return problemDetail;
    }
}
```

---

### 3.3 `PractitionerRequestDto`
- **Package**: `com.omnicare.emr.dto`
- **File**: `omnicare-emr-api/src/main/java/com/omnicare/emr/dto/PractitionerRequestDto.java`

```java
package com.omnicare.emr.dto;

import com.omnicare.emr.entity.PractitionerType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO for Practitioner creation and update request payloads.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request body payload for creating or updating a healthcare practitioner")
public class PractitionerRequestDto {

    @NotBlank(message = "Practitioner code is required")
    @Size(max = 50, message = "Practitioner code must not exceed 50 characters")
    @Schema(description = "Unique code or license identifier of the practitioner", example = "PRAC-001", requiredMode = Schema.RequiredMode.REQUIRED)
    private String practitionerCode;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name must not exceed 100 characters")
    @Schema(description = "Full name of the practitioner", example = "Dr. John Doe", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fullName;

    @NotBlank(message = "Specialty is required")
    @Size(max = 100, message = "Specialty must not exceed 100 characters")
    @Schema(description = "Medical specialty or department", example = "CARDIOLOGY", requiredMode = Schema.RequiredMode.REQUIRED)
    private String specialty;

    @NotNull(message = "Practitioner type is required")
    @Schema(description = "Role/type of healthcare practitioner", example = "DOCTOR", requiredMode = Schema.RequiredMode.REQUIRED)
    private PractitionerType practitionerType;

    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    @Schema(description = "Contact phone number", example = "+1-555-0199")
    private String phone;

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Schema(description = "Work email address", example = "john.doe@omnicare.com")
    private String email;
}
```

---

### 3.4 `PractitionerResponseDto`
- **Package**: `com.omnicare.emr.dto`
- **File**: `omnicare-emr-api/src/main/java/com/omnicare/emr/dto/PractitionerResponseDto.java`

```java
package com.omnicare.emr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.omnicare.emr.entity.PractitionerType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for Practitioner API response body.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload containing complete practitioner details and audit metadata")
public class PractitionerResponseDto {

    @Schema(description = "Unique primary key UUID of the practitioner", example = "a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d")
    private UUID id;

    @Schema(description = "Unique practitioner code", example = "PRAC-001")
    private String practitionerCode;

    @Schema(description = "Full name of the practitioner", example = "Dr. John Doe")
    private String fullName;

    @Schema(description = "Medical specialty", example = "CARDIOLOGY")
    private String specialty;

    @Schema(description = "Type/role of practitioner", example = "DOCTOR")
    private PractitionerType practitionerType;

    @Schema(description = "Contact phone number", example = "+1-555-0199")
    private String phone;

    @Schema(description = "Work email address", example = "john.doe@omnicare.com")
    private String email;

    @Schema(description = "Creation timestamp (UTC)", example = "2026-07-25T12:00:00Z")
    private Instant createdAt;

    @Schema(description = "Last update timestamp (UTC)", example = "2026-07-25T12:00:00Z")
    private Instant updatedAt;

    @Schema(description = "Optimistic locking version", example = "0")
    private Long version;

    @JsonProperty("isDeleted")
    @Schema(description = "Soft deletion status flag", example = "false")
    private boolean isDeleted;
}
```

---

### 3.5 `PractitionerMapper`
- **Package**: `com.omnicare.emr.dto.mapper`
- **File**: `omnicare-emr-api/src/main/java/com/omnicare/emr/dto/mapper/PractitionerMapper.java`

```java
package com.omnicare.emr.dto.mapper;

import com.omnicare.emr.dto.PractitionerRequestDto;
import com.omnicare.emr.dto.PractitionerResponseDto;
import com.omnicare.emr.entity.Practitioner;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper converting between Practitioner entity and DTOs.
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface PractitionerMapper {

    /**
     * Maps request DTO to a new Practitioner entity.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Practitioner toEntity(PractitionerRequestDto requestDto);

    /**
     * Maps Practitioner entity to response DTO.
     */
    PractitionerResponseDto toDto(Practitioner entity);

    /**
     * Updates an existing Practitioner entity in-place from a request DTO.
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    void updateEntityFromDto(PractitionerRequestDto requestDto, @MappingTarget Practitioner entity);
}
```

---

### 3.6 `PractitionerService` Interface
- **Package**: `com.omnicare.emr.service`
- **File**: `omnicare-emr-api/src/main/java/com/omnicare/emr/service/PractitionerService.java`

```java
package com.omnicare.emr.service;

import com.omnicare.emr.dto.PractitionerRequestDto;
import com.omnicare.emr.dto.PractitionerResponseDto;

import java.util.List;
import java.util.UUID;

/**
 * Service interface defining business operations for Practitioner management.
 */
public interface PractitionerService {

    /**
     * Creates a new practitioner record.
     *
     * @param requestDto creation details
     * @return created practitioner response details
     * @throws com.omnicare.emr.exception.DuplicateResourceException if practitionerCode already exists
     */
    PractitionerResponseDto createPractitioner(PractitionerRequestDto requestDto);

    /**
     * Retrieves all active (non-soft-deleted) practitioners.
     *
     * @return list of active practitioner response DTOs
     */
    List<PractitionerResponseDto> getAllPractitioners();

    /**
     * Retrieves a practitioner by ID.
     *
     * @param id UUID of the practitioner
     * @return practitioner response details
     * @throws com.omnicare.emr.exception.ResourceNotFoundException if practitioner does not exist or is soft-deleted
     */
    PractitionerResponseDto getPractitionerById(UUID id);

    /**
     * Updates an existing practitioner record.
     *
     * @param id UUID of practitioner to update
     * @param requestDto updated practitioner details
     * @return updated practitioner response details
     * @throws com.omnicare.emr.exception.ResourceNotFoundException if practitioner does not exist or is soft-deleted
     * @throws com.omnicare.emr.exception.DuplicateResourceException if practitionerCode is used by another practitioner
     */
    PractitionerResponseDto updatePractitioner(UUID id, PractitionerRequestDto requestDto);

    /**
     * Performs soft deletion of a practitioner by setting isDeleted = true.
     *
     * @param id UUID of practitioner to delete
     * @throws com.omnicare.emr.exception.ResourceNotFoundException if practitioner does not exist or is already soft-deleted
     */
    void deletePractitioner(UUID id);
}
```

---

### 3.7 `PractitionerServiceImpl` Implementation
- **Package**: `com.omnicare.emr.service.impl`
- **File**: `omnicare-emr-api/src/main/java/com/omnicare/emr/service/impl/PractitionerServiceImpl.java`

```java
package com.omnicare.emr.service.impl;

import com.omnicare.emr.dto.PractitionerRequestDto;
import com.omnicare.emr.dto.PractitionerResponseDto;
import com.omnicare.emr.dto.mapper.PractitionerMapper;
import com.omnicare.emr.entity.Practitioner;
import com.omnicare.emr.exception.DuplicateResourceException;
import com.omnicare.emr.exception.ResourceNotFoundException;
import com.omnicare.emr.repository.PractitionerRepository;
import com.omnicare.emr.service.PractitionerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link PractitionerService}.
 */
@Service
@RequiredArgsConstructor
public class PractitionerServiceImpl implements PractitionerService {

    private final PractitionerRepository practitionerRepository;
    private final PractitionerMapper practitionerMapper;

    @Override
    @Transactional
    public PractitionerResponseDto createPractitioner(PractitionerRequestDto requestDto) {
        if (practitionerRepository.existsByPractitionerCode(requestDto.getPractitionerCode())) {
            throw new DuplicateResourceException(
                    "Practitioner with code '" + requestDto.getPractitionerCode() + "' already exists"
            );
        }

        Practitioner practitioner = practitionerMapper.toEntity(requestDto);
        Practitioner savedPractitioner = practitionerRepository.save(practitioner);

        return practitionerMapper.toDto(savedPractitioner);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PractitionerResponseDto> getAllPractitioners() {
        return practitionerRepository.findAllByIsDeletedFalse()
                .stream()
                .map(practitionerMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PractitionerResponseDto getPractitionerById(UUID id) {
        Practitioner practitioner = practitionerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found with ID: " + id));

        return practitionerMapper.toDto(practitioner);
    }

    @Override
    @Transactional
    public PractitionerResponseDto updatePractitioner(UUID id, PractitionerRequestDto requestDto) {
        Practitioner practitioner = practitionerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found with ID: " + id));

        if (practitionerRepository.existsByPractitionerCodeAndIdNot(requestDto.getPractitionerCode(), id)) {
            throw new DuplicateResourceException(
                    "Practitioner with code '" + requestDto.getPractitionerCode() + "' already exists"
            );
        }

        practitionerMapper.updateEntityFromDto(requestDto, practitioner);
        Practitioner updatedPractitioner = practitionerRepository.save(practitioner);

        return practitionerMapper.toDto(updatedPractitioner);
    }

    @Override
    @Transactional
    public void deletePractitioner(UUID id) {
        Practitioner practitioner = practitionerRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found with ID: " + id));

        practitioner.setDeleted(true);
        practitionerRepository.save(practitioner);
    }
}
```

---

### 3.8 `PractitionerController`
- **Package**: `com.omnicare.emr.controller`
- **File**: `omnicare-emr-api/src/main/java/com/omnicare/emr/controller/PractitionerController.java`

```java
package com.omnicare.emr.controller;

import com.omnicare.emr.dto.PractitionerRequestDto;
import com.omnicare.emr.dto.PractitionerResponseDto;
import com.omnicare.emr.service.PractitionerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing healthcare practitioner resources.
 */
@RestController
@Tag(name = "Practitioner Management", description = "APIs for managing healthcare practitioners (Doctors, Nurses, Technicians)")
@RequestMapping("/api/v1/practitioners")
@RequiredArgsConstructor
public class PractitionerController {

    private final PractitionerService practitionerService;

    /**
     * Endpoint to create a new practitioner.
     *
     * @param request JSON payload containing practitioner details
     * @return 201 Created status with saved practitioner details
     */
    @Operation(summary = "Create a new practitioner", description = "Registers a new healthcare practitioner with their professional and contact details.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Practitioner created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body payload or validation failure"),
            @ApiResponse(responseCode = "409", description = "Practitioner with the given code already exists")
    })
    @PostMapping
    public ResponseEntity<PractitionerResponseDto> createPractitioner(@Valid @RequestBody PractitionerRequestDto request) {
        PractitionerResponseDto response = practitionerService.createPractitioner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint to fetch all active (non-soft-deleted) practitioners.
     *
     * @return 200 OK status with a list of active practitioners
     */
    @Operation(summary = "Get all active practitioners", description = "Retrieves a list of all active (non-soft-deleted) healthcare practitioners.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Practitioners retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<PractitionerResponseDto>> getAllPractitioners() {
        List<PractitionerResponseDto> response = practitionerService.getAllPractitioners();
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to retrieve a practitioner by their UUID.
     *
     * @param id UUID of practitioner to fetch
     * @return 200 OK status with practitioner details
     */
    @Operation(summary = "Get practitioner by ID", description = "Fetches details of a specific active practitioner by their UUID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Practitioner retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Practitioner not found or soft-deleted")
    })
    @GetMapping("/{id}")
    public ResponseEntity<PractitionerResponseDto> getPractitionerById(
            @Parameter(description = "UUID of the practitioner", required = true)
            @PathVariable("id") UUID id) {
        PractitionerResponseDto response = practitionerService.getPractitionerById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to update an existing practitioner record.
     *
     * @param id UUID of practitioner to update
     * @param request updated practitioner payload
     * @return 200 OK status with updated practitioner details
     */
    @Operation(summary = "Update practitioner details", description = "Updates professional and contact details of an existing active practitioner.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Practitioner updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body payload or validation failure"),
            @ApiResponse(responseCode = "404", description = "Practitioner not found or soft-deleted"),
            @ApiResponse(responseCode = "409", description = "Practitioner code is already used by another practitioner")
    })
    @PutMapping("/{id}")
    public ResponseEntity<PractitionerResponseDto> updatePractitioner(
            @Parameter(description = "UUID of the practitioner to update", required = true)
            @PathVariable("id") UUID id,
            @Valid @RequestBody PractitionerRequestDto request) {
        PractitionerResponseDto response = practitionerService.updatePractitioner(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint to soft-delete a practitioner record.
     *
     * @param id UUID of practitioner to soft-delete
     * @return 204 No Content status
     */
    @Operation(summary = "Soft delete practitioner", description = "Soft deletes a practitioner by setting their deletion flag to true.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Practitioner soft-deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Practitioner not found or already soft-deleted")
    })
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deletePractitioner(
            @Parameter(description = "UUID of the practitioner to delete", required = true)
            @PathVariable("id") UUID id) {
        practitionerService.deletePractitioner(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## 4. Integration & Data Flow Architecture

### 4.1 Request Processing Sequence
1. **Client Request**: Client issues HTTP request (`POST`, `GET`, `PUT`, `DELETE`) to `/api/v1/practitioners`.
2. **Spring MVC Validation**: Controller validates request payload `@Valid PractitionerRequestDto`. If validation fails, Spring throws `MethodArgumentNotValidException`.
3. **Controller Execution**: Controller delegates execution to `PractitionerService`.
4. **Service Uniqueness & Active State Verification**:
   - `create`: calls `existsByPractitionerCode(code)`. If `true`, throws `DuplicateResourceException`.
   - `update`: calls `findByIdAndIsDeletedFalse(id)`. If empty, throws `ResourceNotFoundException`. Calls `existsByPractitionerCodeAndIdNot(code, id)`. If `true`, throws `DuplicateResourceException`.
   - `getPractitionerById`: calls `findByIdAndIsDeletedFalse(id)`. If empty, throws `ResourceNotFoundException`.
   - `deletePractitioner`: calls `findByIdAndIsDeletedFalse(id)`. If empty, throws `ResourceNotFoundException`. Sets `isDeleted = true`.
5. **Mapping & Persistence**: Entity is persisted or fetched; MapStruct converts between Entity <-> DTO.
6. **Global Exception Handling**:
   - `DuplicateResourceException` -> HTTP 409 Conflict with RFC 7807 problem details.
   - `ResourceNotFoundException` -> HTTP 404 Not Found with RFC 7807 problem details.

---

## 5. Verification Method

### 5.1 Verification Commands
- **Compilation Check**:
  ```bash
  mvn clean compile -f omnicare-emr-api/pom.xml
  ```
- **MapStruct Code Generation Check**:
  Ensures `PractitionerMapperImpl` is generated cleanly in `target/generated-sources/annotations`.
- **Unit & Integration Testing**:
  ```bash
  mvn test -f omnicare-emr-api/pom.xml
  ```

---
