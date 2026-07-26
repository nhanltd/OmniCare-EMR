# Phase 2 Business Logic Layer Design Handoff Report

## Executive Summary
This report provides the architectural design and complete specification for the Phase 2 Business Logic Layer of `omnicare-emr-api`, covering the **Encounter** and **Observation** domains. It details DTO designs with validation annotations, MapStruct mapper configurations, service interfaces and implementation classes with clinical business validation rules, custom exception handling, and RFC 7807 problem detail response mapping.

---

## 1. Observation

Direct observations from examining existing codebase files in `omnicare-emr-api`:

### 1.1 Existing Service Layer Patterns
- **`PatientService.java`** (`c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/service/PatientService.java`): Defines `PatientResponseDto createPatient(PatientRequestDto requestDto);`.
- **`PatientServiceImpl.java`** (`c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/service/impl/PatientServiceImpl.java`): Annotated with `@Service`, `@RequiredArgsConstructor`. Uses `@Transactional` for write operations, checks identifier uniqueness using `patientRepository.existsByIdentifier(...)`, throws `DuplicateResourceException`, converts DTO to Entity using `patientMapper.toEntity(...)`, saves via repository, and returns DTO via `patientMapper.toDto(...)`.
- **`PractitionerService.java`** & **`PractitionerServiceImpl.java`** (`c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/service/impl/PractitionerServiceImpl.java`): Uses `@Transactional(readOnly = true)` for query operations (`getAllPractitioners`, `getPractitionerById`). Uses `findByIdAndIsDeletedFalse(id)` to filter out soft-deleted records and throws `ResourceNotFoundException("Practitioner not found with ID: " + id)` when not found.

### 1.2 Existing DTO & Mapper Conventions
- **DTOs**: `PatientRequestDto`, `PatientResponseDto`, `PractitionerRequestDto`, `PractitionerResponseDto` use Lombok `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`.
- **Validation Annotations**: Request DTOs use `jakarta.validation.constraints` annotations (`@NotBlank`, `@NotNull`, `@Size`, `@PastOrPresent`).
- **Response DTOs**: Base entity audit fields (`id`, `createdAt`, `updatedAt`, `version`, `isDeleted`) are exposed.
- **Mappers**: Mappers use MapStruct (`@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)`). Field ignore annotations `@Mapping(target = "id", ignore = true)`, etc. are placed on `toEntity` and `updateEntityFromDto` methods.

### 1.3 Existing Global Exception Handling
- **`GlobalExceptionHandler.java`** (`c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/exception/GlobalExceptionHandler.java`): Extends `ResponseEntityExceptionHandler` and uses `@RestControllerAdvice`.
- **RFC 7807 Standard**: Exception handlers return Spring's `ProblemDetail` (e.g., `ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage())`) setting custom `title` and `type` URI (`https://api.omnicare.com/errors/...`).
- **Existing Custom Exceptions**:
  - `ResourceNotFoundException` (`HttpStatus.NOT_FOUND` / 404)
  - `DuplicateResourceException` (`HttpStatus.CONFLICT` / 409)

---

## 2. Logic Chain

Based on the observations above and Phase 2 functional requirements:

1. **DTO Design**:
   - `EncounterRequestDto` needs `@NotNull` on `patientId`, `practitionerId`, `encounterDate`. Status is optional in request (can be `null`), defaulting to `PLANNED` in service logic.
   - `EncounterResponseDto` needs nested entity properties mapped directly: `patientId` and `patientName` (from `patient.fullName`), `practitionerId` and `practitionerName` (from `practitioner.fullName`), along with `encounterDate`, `status`, `reason`, and audit metadata (`id`, `createdAt`, `updatedAt`, `version`).
   - `ObservationRequestDto` needs `@NotNull` on `encounterId` and `valueJson` (Jackson `JsonNode` or `Map<String, Object>`).
   - `ObservationResponseDto` needs `id`, `encounterId`, `valueJson`, `createdAt`, `updatedAt`, `version`.

2. **MapStruct Mapper Design**:
   - `EncounterMapper`: Needs explicit `@Mapping` rules to map `patient.id` -> `patientId`, `patient.fullName` -> `patientName`, `practitioner.id` -> `practitionerId`, `practitioner.fullName` -> `practitionerName`. Entity mapping `toEntity` will ignore managed entities (`patient`, `practitioner`) as they must be fetched and assigned by service layer.
   - `ObservationMapper`: Maps `encounter.id` -> `encounterId` in `toDto`, and ignores `encounter` entity in `toEntity`.

3. **Service Layer & Clinical Business Rules**:
   - `EncounterService` / `EncounterServiceImpl`:
     - `createEncounter`: Validates both `patientId` and `practitionerId` exist via `patientRepository.findByIdAndIsDeletedFalse` and `practitionerRepository.findByIdAndIsDeletedFalse`. If either is missing, throws `ResourceNotFoundException`. Defaults `status` to `EncounterStatus.PLANNED` if `requestDto.getStatus()` is `null`. Saves and returns DTO.
     - `getEncounterById`: Retrieves encounter by ID ensuring `isDeleted = false`. Throws `ResourceNotFoundException` if not found.
     - `getEncounters` (or `getAllEncounters`): Returns list of active encounters mapped to DTOs.
   - `ObservationService` / `ObservationServiceImpl`:
     - `createObservation`:
       - **Validation 1**: Checks `Encounter` exists by ID via `encounterRepository.findByIdAndIsDeletedFalse(requestDto.getEncounterId())`. Throws `ResourceNotFoundException` (HTTP 404) if not found.
       - **Validation 2**: Checks `encounter.getStatus() != EncounterStatus.CANCELLED`. If status IS `CANCELLED`, throws custom `EncounterCancelledException`.
       - Stores `valueJson` payload into entity column `value_json`. Saves and returns DTO.
     - `getObservationsByEncounterId`: Validates encounter existence, then returns list of observations for the given encounter ID.

4. **Exception Handling & RFC 7807**:
   - Define `EncounterCancelledException` annotated with `@ResponseStatus(HttpStatus.CONFLICT)` (or `HttpStatus.BAD_REQUEST`).
   - Add handler method `@ExceptionHandler(EncounterCancelledException.class)` in `GlobalExceptionHandler` returning `ProblemDetail` with status `HttpStatus.CONFLICT` (409), title `"Encounter Cancelled"`, type URI `https://api.omnicare.com/errors/encounter-cancelled`.

---

## 3. Detailed Component Specifications

### 3.1 DTO Specifications

#### 3.1.1 `EncounterRequestDto.java`
- **Location**: `com.omnicare.emr.dto.EncounterRequestDto`
```java
package com.omnicare.emr.dto;

import com.omnicare.emr.entity.EncounterStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for creating an Encounter.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncounterRequestDto {

    @NotNull(message = "Patient ID is required")
    private UUID patientId;

    @NotNull(message = "Practitioner ID is required")
    private UUID practitionerId;

    @NotNull(message = "Encounter date is required")
    private Instant encounterDate;

    private EncounterStatus status;

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
```

#### 3.1.2 `EncounterResponseDto.java`
- **Location**: `com.omnicare.emr.dto.EncounterResponseDto`
```java
package com.omnicare.emr.dto;

import com.omnicare.emr.entity.EncounterStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for Encounter response payload.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EncounterResponseDto {

    private UUID id;
    private UUID patientId;
    private String patientName;
    private UUID practitionerId;
    private String practitionerName;
    private Instant encounterDate;
    private EncounterStatus status;
    private String reason;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
```

#### 3.1.3 `ObservationRequestDto.java`
- **Location**: `com.omnicare.emr.dto.ObservationRequestDto`
```java
package com.omnicare.emr.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

/**
 * DTO for recording an Observation (vitals/clinical data).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationRequestDto {

    @NotNull(message = "Encounter ID is required")
    private UUID encounterId;

    @NotNull(message = "Observation JSON payload is required")
    private JsonNode valueJson;
}
```
*Note: `valueJson` can alternatively be typed as `Map<String, Object>` depending on preferred Jackson mapping style; `JsonNode` is recommended for native Jackson tree node support.*

#### 3.1.4 `ObservationResponseDto.java`
- **Location**: `com.omnicare.emr.dto.ObservationResponseDto`
```java
package com.omnicare.emr.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for Observation response payload.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ObservationResponseDto {

    private UUID id;
    private UUID encounterId;
    private JsonNode valueJson;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
```

---

### 3.2 MapStruct Mappers Specification

#### 3.2.1 `EncounterMapper.java`
- **Location**: `com.omnicare.emr.dto.mapper.EncounterMapper`
```java
package com.omnicare.emr.dto.mapper;

import com.omnicare.emr.dto.EncounterRequestDto;
import com.omnicare.emr.dto.EncounterResponseDto;
import com.omnicare.emr.entity.Encounter;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for converting between Encounter entity and DTOs.
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface EncounterMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "patient", ignore = true)
    @Mapping(target = "practitioner", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Encounter toEntity(EncounterRequestDto requestDto);

    @Mapping(source = "patient.id", target = "patientId")
    @Mapping(source = "patient.fullName", target = "patientName")
    @Mapping(source = "practitioner.id", target = "practitionerId")
    @Mapping(source = "practitioner.fullName", target = "practitionerName")
    EncounterResponseDto toDto(Encounter entity);
}
```

#### 3.2.2 `ObservationMapper.java`
- **Location**: `com.omnicare.emr.dto.mapper.ObservationMapper`
```java
package com.omnicare.emr.dto.mapper;

import com.omnicare.emr.dto.ObservationRequestDto;
import com.omnicare.emr.dto.ObservationResponseDto;
import com.omnicare.emr.entity.Observation;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

/**
 * MapStruct mapper for converting between Observation entity and DTOs.
 */
@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ObservationMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "encounter", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    Observation toEntity(ObservationRequestDto requestDto);

    @Mapping(source = "encounter.id", target = "encounterId")
    ObservationResponseDto toDto(Observation entity);
}
```

---

### 3.3 Service Layer Specifications

#### 3.3.1 `EncounterService.java`
- **Location**: `com.omnicare.emr.service.EncounterService`
```java
package com.omnicare.emr.service;

import com.omnicare.emr.dto.EncounterRequestDto;
import com.omnicare.emr.dto.EncounterResponseDto;

import java.util.List;
import java.util.UUID;

/**
 * Service interface defining business operations for Encounter management.
 */
public interface EncounterService {

    /**
     * Creates a new encounter. Defaults status to PLANNED if not specified.
     *
     * @param requestDto creation details
     * @return created encounter response DTO
     * @throws com.omnicare.emr.exception.ResourceNotFoundException if Patient or Practitioner does not exist
     */
    EncounterResponseDto createEncounter(EncounterRequestDto requestDto);

    /**
     * Retrieves an encounter by ID.
     *
     * @param id UUID of the encounter
     * @return encounter response DTO
     * @throws com.omnicare.emr.exception.ResourceNotFoundException if encounter is not found or soft-deleted
     */
    EncounterResponseDto getEncounterById(UUID id);

    /**
     * Retrieves all active (non-soft-deleted) encounters.
     *
     * @return list of encounter response DTOs
     */
    List<EncounterResponseDto> getEncounters();
}
```

#### 3.3.2 `EncounterServiceImpl.java`
- **Location**: `com.omnicare.emr.service.impl.EncounterServiceImpl`
```java
package com.omnicare.emr.service.impl;

import com.omnicare.emr.dto.EncounterRequestDto;
import com.omnicare.emr.dto.EncounterResponseDto;
import com.omnicare.emr.dto.mapper.EncounterMapper;
import com.omnicare.emr.entity.Encounter;
import com.omnicare.emr.entity.EncounterStatus;
import com.omnicare.emr.entity.Patient;
import com.omnicare.emr.entity.Practitioner;
import com.omnicare.emr.exception.ResourceNotFoundException;
import com.omnicare.emr.repository.EncounterRepository;
import com.omnicare.emr.repository.PatientRepository;
import com.omnicare.emr.repository.PractitionerRepository;
import com.omnicare.emr.service.EncounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link EncounterService}.
 */
@Service
@RequiredArgsConstructor
public class EncounterServiceImpl implements EncounterService {

    private final EncounterRepository encounterRepository;
    private final PatientRepository patientRepository;
    private final PractitionerRepository practitionerRepository;
    private final EncounterMapper encounterMapper;

    @Override
    @Transactional
    public EncounterResponseDto createEncounter(EncounterRequestDto requestDto) {
        Patient patient = patientRepository.findByIdAndIsDeletedFalse(requestDto.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + requestDto.getPatientId()));

        Practitioner practitioner = practitionerRepository.findByIdAndIsDeletedFalse(requestDto.getPractitionerId())
                .orElseThrow(() -> new ResourceNotFoundException("Practitioner not found with ID: " + requestDto.getPractitionerId()));

        Encounter encounter = encounterMapper.toEntity(requestDto);
        encounter.setPatient(patient);
        encounter.setPractitioner(practitioner);

        // Clinical Default Rule: default status to PLANNED if null
        if (requestDto.getStatus() != null) {
            encounter.setStatus(requestDto.getStatus());
        } else {
            encounter.setStatus(EncounterStatus.PLANNED);
        }

        Encounter savedEncounter = encounterRepository.save(encounter);
        return encounterMapper.toDto(savedEncounter);
    }

    @Override
    @Transactional(readOnly = true)
    public EncounterResponseDto getEncounterById(UUID id) {
        Encounter encounter = encounterRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found with ID: " + id));

        return encounterMapper.toDto(encounter);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EncounterResponseDto> getEncounters() {
        return encounterRepository.findAllByIsDeletedFalse()
                .stream()
                .map(encounterMapper::toDto)
                .toList();
    }
}
```

#### 3.3.3 `ObservationService.java`
- **Location**: `com.omnicare.emr.service.ObservationService`
```java
package com.omnicare.emr.service;

import com.omnicare.emr.dto.ObservationRequestDto;
import com.omnicare.emr.dto.ObservationResponseDto;

import java.util.List;
import java.util.UUID;

/**
 * Service interface defining business operations for Observation recording.
 */
public interface ObservationService {

    /**
     * Records a new clinical observation for an encounter.
     *
     * @param requestDto observation payload details
     * @return recorded observation response DTO
     * @throws com.omnicare.emr.exception.ResourceNotFoundException if Encounter is not found (HTTP 404)
     * @throws com.omnicare.emr.exception.EncounterCancelledException if Encounter status is CANCELLED (HTTP 409 / 400)
     */
    ObservationResponseDto createObservation(ObservationRequestDto requestDto);

    /**
     * Retrieves all observations associated with an encounter.
     *
     * @param encounterId UUID of the encounter
     * @return list of observation response DTOs
     * @throws com.omnicare.emr.exception.ResourceNotFoundException if Encounter is not found
     */
    List<ObservationResponseDto> getObservationsByEncounterId(UUID encounterId);
}
```

#### 3.3.4 `ObservationServiceImpl.java`
- **Location**: `com.omnicare.emr.service.impl.ObservationServiceImpl`
```java
package com.omnicare.emr.service.impl;

import com.omnicare.emr.dto.ObservationRequestDto;
import com.omnicare.emr.dto.ObservationResponseDto;
import com.omnicare.emr.dto.mapper.ObservationMapper;
import com.omnicare.emr.entity.Encounter;
import com.omnicare.emr.entity.EncounterStatus;
import com.omnicare.emr.entity.Observation;
import com.omnicare.emr.exception.EncounterCancelledException;
import com.omnicare.emr.exception.ResourceNotFoundException;
import com.omnicare.emr.repository.EncounterRepository;
import com.omnicare.emr.repository.ObservationRepository;
import com.omnicare.emr.service.ObservationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link ObservationService}.
 */
@Service
@RequiredArgsConstructor
public class ObservationServiceImpl implements ObservationService {

    private final ObservationRepository observationRepository;
    private final EncounterRepository encounterRepository;
    private final ObservationMapper observationMapper;

    @Override
    @Transactional
    public ObservationResponseDto createObservation(ObservationRequestDto requestDto) {
        // Validation 1: Check Encounter exists (HTTP 404 if missing)
        Encounter encounter = encounterRepository.findByIdAndIsDeletedFalse(requestDto.getEncounterId())
                .orElseThrow(() -> new ResourceNotFoundException("Encounter not found with ID: " + requestDto.getEncounterId()));

        // Validation 2: Check Encounter status is NOT CANCELLED (HTTP 409 / 400 if CANCELLED)
        if (encounter.getStatus() == EncounterStatus.CANCELLED) {
            throw new EncounterCancelledException("Cannot record observation for cancelled encounter with ID: " + encounter.getId());
        }

        Observation observation = observationMapper.toEntity(requestDto);
        observation.setEncounter(encounter);
        observation.setValueJson(requestDto.getValueJson());

        Observation savedObservation = observationRepository.save(observation);
        return observationMapper.toDto(savedObservation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ObservationResponseDto> getObservationsByEncounterId(UUID encounterId) {
        if (!encounterRepository.existsByIdAndIsDeletedFalse(encounterId)) {
            throw new ResourceNotFoundException("Encounter not found with ID: " + encounterId);
        }

        return observationRepository.findByEncounterIdAndIsDeletedFalse(encounterId)
                .stream()
                .map(observationMapper::toDto)
                .toList();
    }
}
```

---

### 3.4 Exception Handling & RFC 7807 Response Mapping

#### 3.4.1 `EncounterCancelledException.java`
- **Location**: `com.omnicare.emr.exception.EncounterCancelledException`
```java
package com.omnicare.emr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an action cannot be performed because an Encounter has been cancelled.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class EncounterCancelledException extends RuntimeException {

    public EncounterCancelledException(String message) {
        super(message);
    }

    public EncounterCancelledException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

#### 3.4.2 Update to `GlobalExceptionHandler.java`
- **Location**: `com.omnicare.emr.exception.GlobalExceptionHandler`
- **Addition**: Add exception handler for `EncounterCancelledException`:

```java
    @ExceptionHandler(EncounterCancelledException.class)
    public ProblemDetail handleEncounterCancelledException(EncounterCancelledException ex) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problemDetail.setTitle("Encounter Cancelled");
        problemDetail.setType(URI.create("https://api.omnicare.com/errors/encounter-cancelled"));
        return problemDetail;
    }
```

*RFC 7807 Response Example:*
```json
{
  "type": "https://api.omnicare.com/errors/encounter-cancelled",
  "title": "Encounter Cancelled",
  "status": 409,
  "detail": "Cannot record observation for cancelled encounter with ID: 123e4567-e89b-12d3-a456-426614174000",
  "instance": "/api/v1/observations"
}
```

---

## 4. Caveats

- **Entity & Repository Dependency**: This design assumes `Encounter` and `Observation` entities and repositories (`EncounterRepository`, `ObservationRepository`) created by `explorer_p2_1` / implementer agents provide methods `findByIdAndIsDeletedFalse(UUID id)`, `existsByIdAndIsDeletedFalse(UUID id)`, and `findByEncounterIdAndIsDeletedFalse(UUID encounterId)`.
- **JSON Field Type**: `JsonNode` (com.fasterxml.jackson.databind.JsonNode) is used for `valueJson`. If the entity layer uses `Map<String, Object>`, MapStruct will auto-convert or MapStruct custom mapping methods can be added.
- **HTTP Status Choice**: HTTP 409 Conflict is specified for `EncounterCancelledException` as it represents a state conflict. If HTTP 400 Bad Request is required by specific API standards, `@ResponseStatus(HttpStatus.BAD_REQUEST)` and `HttpStatus.BAD_REQUEST` in `GlobalExceptionHandler` can be toggled without changing service logic.

---

## 5. Conclusion

The specification for Phase 2 Business Logic Layer is complete, fully aligned with Spring Boot 3 standards, MapStruct best practices, Lombok conventions, and RFC 7807 problem details error responses established in Phase 1. Implementer agents can directly apply these code designs.

---

## 6. Verification Method

To verify the implementation once coded by implementer agents:

1. **Compilation Check**:
   ```bash
   mvn clean compile
   ```
   Verify MapStruct generated implementations (`EncounterMapperImpl`, `ObservationMapperImpl`) compile cleanly without unmapped target warnings or missing bean errors.

2. **Unit Testing**:
   - `EncounterServiceTest`: Verify `createEncounter` sets `PLANNED` status when null, throws `ResourceNotFoundException` when Patient or Practitioner ID is missing.
   - `ObservationServiceTest`: Verify `createObservation` succeeds for active encounters (`PLANNED`, `IN_PROGRESS`, `FINISHED`), throws `ResourceNotFoundException` (404) for missing encounter ID, and throws `EncounterCancelledException` (409) when encounter status is `CANCELLED`.

3. **RFC 7807 Verification**:
   - Execute mock request creating an observation against a cancelled encounter and confirm response payload contains `type: "https://api.omnicare.com/errors/encounter-cancelled"`, `title: "Encounter Cancelled"`, and `status: 409`.
