# Phase 3 Analysis: LIS Webhook API & DiagnosticReport Entity

## Executive Summary
This document provides a comprehensive technical analysis and design specification for **Phase 3 Requirement R1 (LIS Webhook API & DiagnosticReport Entity)** of the OmniCare EMR backend application (`omnicare-emr-api`).

The objective of Requirement R1 is to enable laboratory integration by storing diagnostic report metadata, ordering details, and laboratory test result payloads received from external Laboratory Information Systems (LIS) via a dedicated webhook endpoint `PUT /api/v1/diagnostic-reports/{id}/results`.

---

## 1. Existing Codebase Inspection

### 1.1 Database Migration History (Flyway V1 - V3)
- **`V1__init_schema.sql`**: Created `patient` table with primary key `id UUID`, audit fields (`created_at`, `updated_at`, `version`, `is_deleted`), and patient details (`identifier`, `full_name`, `gender`, `birth_date`, `phone_number`).
- **`V2__create_practitioner_table_and_seed.sql`**: Created `practitioner` table with similar base audit fields and seeded default practitioners (`PRAC-001` through `PRAC-005`).
- **`V3__create_encounter_and_observation_tables.sql`**: Created:
  - `encounter`: Stores clinical encounters linked to `patient(id)` (`fk_encounter_patient`) and `practitioner(id)` (`fk_encounter_practitioner`), with status (`PLANNED`, `IN_PROGRESS`, `FINISHED`, `CANCELLED`). Indexed on `patient_id`, `practitioner_id`, and `status`.
  - `observation`: Stores vital signs and unstructured clinical data in a JSONB column (`value_json`), linked to `encounter(id)` (`fk_observation_encounter`). Indexed on `encounter_id` and a GIN index on `value_json`.

### 1.2 JPA Entity Patterns (`BaseEntity`, `Encounter`, `Observation`)
- All domain entities extend `BaseEntity` (`src/main/java/com/omnicare/emr/entity/BaseEntity.java`), which provides:
  - `@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;`
  - `@CreatedDate Instant createdAt;`
  - `@LastModifiedDate Instant updatedAt;`
  - `@Version Long version;`
  - `boolean isDeleted = false;`
- Entities utilize Lombok annotations (`@Getter`, `@Setter`, `@SuperBuilder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@EqualsAndHashCode(callSuper = true)`).
- Timestamps in entities use `java.time.Instant`, which maps to `TIMESTAMP WITH TIME ZONE` in PostgreSQL/H2.
- Soft deletion pattern: `is_deleted` column filter applied in repository method queries (e.g. `findByIdAndIsDeletedFalse`, `findByEncounterIdAndIsDeletedFalse`).

### 1.3 DTO & Mapping Conventions
- Spring Component MapStruct Mappers (`@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)`).
- Request DTOs use Jakarta Validation annotations (`@NotNull`, `@NotBlank`, `@Size`).
- Controllers return `ResponseEntity<T>` with standard HTTP status codes (`201 CREATED`, `200 OK`, `400 BAD_REQUEST`, `404 NOT_FOUND`, `409 CONFLICT`).

### 1.4 Exception Handling & ProblemDetail (RFC 7807)
- `GlobalExceptionHandler` handles custom domain exceptions:
  - `ResourceNotFoundException` -> HTTP 404 (Title: "Resource Not Found")
  - `EncounterCancelledException` -> HTTP 400 (Title: "Encounter Cancelled", Type: `https://api.omnicare.com/errors/encounter-cancelled`)
  - `DuplicateResourceException` & `DataIntegrityViolationException` -> HTTP 409

---

## 2. Technical Design Specification for Requirement R1

### 2.1 Database Schema Migration Script (`V4__phase3_schema.sql`)
File Location: `src/main/resources/db/migration/V4__phase3_schema.sql`

```sql
-- Table: diagnostic_report
CREATE TABLE diagnostic_report (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    encounter_id UUID NOT NULL,
    ordered_at TIMESTAMP WITH TIME ZONE NOT NULL,
    result_received_at TIMESTAMP WITH TIME ZONE,
    test_code VARCHAR(50) NOT NULL,
    test_name VARCHAR(100) NOT NULL,
    result_value VARCHAR(255),
    unit VARCHAR(50),
    reference_range VARCHAR(100),
    flag VARCHAR(20),
    status VARCHAR(32) NOT NULL,
    CONSTRAINT fk_diagnostic_report_encounter FOREIGN KEY (encounter_id) REFERENCES encounter(id)
);

-- Indexes for diagnostic_report table
CREATE INDEX idx_diagnostic_report_encounter_id ON diagnostic_report(encounter_id);
CREATE INDEX idx_diagnostic_report_status ON diagnostic_report(status);
CREATE INDEX idx_diagnostic_report_test_code ON diagnostic_report(test_code);
```

### 2.2 Enum Definition: `DiagnosticReportStatus.java`
File Location: `src/main/java/com/omnicare/emr/entity/DiagnosticReportStatus.java`

```java
package com.omnicare.emr.entity;

/**
 * Operational status of a diagnostic report.
 */
public enum DiagnosticReportStatus {
    REGISTERED,
    PRELIMINARY,
    FINAL,
    CANCELLED,
    CORRECTED
}
```

### 2.3 JPA Entity: `DiagnosticReport.java`
File Location: `src/main/java/com/omnicare/emr/entity/DiagnosticReport.java`

```java
package com.omnicare.emr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(
    name = "diagnostic_report",
    indexes = {
        @Index(name = "idx_diagnostic_report_encounter_id", columnList = "encounter_id"),
        @Index(name = "idx_diagnostic_report_status", columnList = "status"),
        @Index(name = "idx_diagnostic_report_test_code", columnList = "test_code")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DiagnosticReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "encounter_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_diagnostic_report_encounter")
    )
    private Encounter encounter;

    @Column(name = "ordered_at", nullable = false)
    private Instant orderedAt;

    @Column(name = "result_received_at")
    private Instant resultReceivedAt;

    @Column(name = "test_code", nullable = false, length = 50)
    private String testCode;

    @Column(name = "test_name", nullable = false, length = 100)
    private String testName;

    @Column(name = "result_value", length = 255)
    private String resultValue;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "reference_range", length = 100)
    private String referenceRange;

    @Column(name = "flag", length = 20)
    private String flag;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DiagnosticReportStatus status;
}
```

### 2.4 Repository: `DiagnosticReportRepository.java`
File Location: `src/main/java/com/omnicare/emr/repository/DiagnosticReportRepository.java`

```java
package com.omnicare.emr.repository;

import com.omnicare.emr.entity.DiagnosticReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DiagnosticReportRepository extends JpaRepository<DiagnosticReport, UUID> {

    Optional<DiagnosticReport> findByIdAndIsDeletedFalse(UUID id);

    List<DiagnosticReport> findByEncounterIdAndIsDeletedFalse(UUID encounterId);

    List<DiagnosticReport> findAllByIsDeletedFalse();
}
```

### 2.5 DTO Specifications

#### `DiagnosticReportCreateRequestDto.java`
File Location: `src/main/java/com/omnicare/emr/dto/DiagnosticReportCreateRequestDto.java`
```java
package com.omnicare.emr.dto;

import com.omnicare.emr.entity.DiagnosticReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticReportCreateRequestDto {

    @NotNull(message = "Encounter ID is required")
    private UUID encounterId;

    @NotBlank(message = "Test code is required")
    @Size(max = 50, message = "Test code must not exceed 50 characters")
    private String testCode;

    @NotBlank(message = "Test name is required")
    @Size(max = 100, message = "Test name must not exceed 100 characters")
    private String testName;

    private Instant orderedAt;

    private DiagnosticReportStatus status;
}
```

#### `DiagnosticReportResultUpdateDto.java` (LIS Webhook Payload)
File Location: `src/main/java/com/omnicare/emr/dto/DiagnosticReportResultUpdateDto.java`
```java
package com.omnicare.emr.dto;

import com.omnicare.emr.entity.DiagnosticReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticReportResultUpdateDto {

    @NotBlank(message = "Result value is required")
    @Size(max = 255, message = "Result value must not exceed 255 characters")
    private String resultValue;

    @Size(max = 50, message = "Unit must not exceed 50 characters")
    private String unit;

    @Size(max = 100, message = "Reference range must not exceed 100 characters")
    private String referenceRange;

    @Size(max = 20, message = "Flag must not exceed 20 characters")
    private String flag;

    private DiagnosticReportStatus status;
}
```

#### `DiagnosticReportResponseDto.java`
File Location: `src/main/java/com/omnicare/emr/dto/DiagnosticReportResponseDto.java`
```java
package com.omnicare.emr.dto;

import com.omnicare.emr.entity.DiagnosticReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosticReportResponseDto {

    private UUID id;
    private UUID encounterId;
    private Instant orderedAt;
    private Instant resultReceivedAt;
    private String testCode;
    private String testName;
    private String resultValue;
    private String unit;
    private String referenceRange;
    private String flag;
    private DiagnosticReportStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
}
```

### 2.6 MapStruct Mapper: `DiagnosticReportMapper.java`
File Location: `src/main/java/com/omnicare/emr/dto/mapper/DiagnosticReportMapper.java`

```java
package com.omnicare.emr.dto.mapper;

import com.omnicare.emr.dto.DiagnosticReportCreateRequestDto;
import com.omnicare.emr.dto.DiagnosticReportResponseDto;
import com.omnicare.emr.entity.DiagnosticReport;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = MappingConstants.ComponentModel.SPRING,
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface DiagnosticReportMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "encounter", ignore = true)
    @Mapping(target = "resultReceivedAt", ignore = true)
    @Mapping(target = "resultValue", ignore = true)
    @Mapping(target = "unit", ignore = true)
    @Mapping(target = "referenceRange", ignore = true)
    @Mapping(target = "flag", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    DiagnosticReport toEntity(DiagnosticReportCreateRequestDto requestDto);

    @Mapping(source = "encounter.id", target = "encounterId")
    DiagnosticReportResponseDto toDto(DiagnosticReport entity);
}
```

### 2.7 Service Layer: `DiagnosticReportService` & `DiagnosticReportServiceImpl`

#### `DiagnosticReportService.java`
File Location: `src/main/java/com/omnicare/emr/service/DiagnosticReportService.java`
```java
package com.omnicare.emr.service;

import com.omnicare.emr.dto.DiagnosticReportCreateRequestDto;
import com.omnicare.emr.dto.DiagnosticReportResponseDto;
import com.omnicare.emr.dto.DiagnosticReportResultUpdateDto;

import java.util.List;
import java.util.UUID;

public interface DiagnosticReportService {

    DiagnosticReportResponseDto createDiagnosticReport(DiagnosticReportCreateRequestDto requestDto);

    DiagnosticReportResponseDto updateDiagnosticReportResults(UUID id, DiagnosticReportResultUpdateDto resultDto);

    DiagnosticReportResponseDto getDiagnosticReportById(UUID id);

    List<DiagnosticReportResponseDto> getDiagnosticReportsByEncounterId(UUID encounterId);
}
```

#### Key Logic in `DiagnosticReportServiceImpl.java`:
1. **`createDiagnosticReport`**:
   - Fetch encounter via `encounterRepository.findByIdAndIsDeletedFalse(requestDto.getEncounterId())`. Throw `ResourceNotFoundException` if not found.
   - Check if `encounter.getStatus() == EncounterStatus.CANCELLED`. If true, throw `EncounterCancelledException("Cannot create diagnostic report for cancelled encounter with ID: " + encounter.getId())`.
   - Convert request DTO to entity via `diagnosticReportMapper.toEntity(requestDto)`.
   - Set `encounter`.
   - If `orderedAt` is null, set `orderedAt = Instant.now()`.
   - If `status` is null, set `status = DiagnosticReportStatus.REGISTERED`.
   - Save entity and return mapped `DiagnosticReportResponseDto`.

2. **`updateDiagnosticReportResults`**:
   - Fetch report via `diagnosticReportRepository.findByIdAndIsDeletedFalse(id)`. Throw `ResourceNotFoundException` if not found.
   - Check if `report.getEncounter().getStatus() == EncounterStatus.CANCELLED`. If true, throw `EncounterCancelledException("Cannot update diagnostic report results for cancelled encounter with ID: " + report.getEncounter().getId())`.
   - Update fields from `resultDto`: `resultValue`, `unit`, `referenceRange`, `flag`.
   - If `resultDto.getStatus()` is provided, set `status = resultDto.getStatus()`; else set `status = DiagnosticReportStatus.FINAL`.
   - **Crucial Requirement**: Set `resultReceivedAt = Instant.now()`.
   - Save entity and return mapped `DiagnosticReportResponseDto`.

3. **`getDiagnosticReportsByEncounterId`**:
   - Verify encounter exists using `encounterRepository.existsByIdAndIsDeletedFalse(encounterId)`. Throw `ResourceNotFoundException` if false.
   - Query reports via `diagnosticReportRepository.findByEncounterIdAndIsDeletedFalse(encounterId)`.
   - Map entities to DTO list and return.

### 2.8 REST Controller: `DiagnosticReportController.java`
File Location: `src/main/java/com/omnicare/emr/controller/DiagnosticReportController.java`

Endpoints to expose:
- **`POST /api/v1/diagnostic-reports`**:
  - Request Body: `@Valid DiagnosticReportCreateRequestDto`
  - Status: `201 CREATED`
  - Responses: 201, 400 (Validation / Encounter Cancelled), 404 (Encounter not found).
- **`PUT /api/v1/diagnostic-reports/{id}/results`**:
  - Path Variable: `@PathVariable("id") UUID id`
  - Request Body: `@Valid DiagnosticReportResultUpdateDto`
  - Status: `200 OK`
  - Behavior: Updates report with test results and sets `resultReceivedAt` to current timestamp (`Instant.now()`).
  - Responses: 200, 400 (Validation / Encounter Cancelled), 404 (Report or Encounter not found).
- **`GET /api/v1/diagnostic-reports/{id}`**:
  - Path Variable: `@PathVariable("id") UUID id`
  - Status: `200 OK`
  - Responses: 200, 404.
- **`GET /api/v1/diagnostic-reports?encounterId={encounterId}`**:
  - Query Parameter: `@RequestParam("encounterId") UUID encounterId`
  - Status: `200 OK`
  - Responses: 200, 404.

---

## 3. Risk Analysis & Edge Cases

| Area | Potential Risk | Mitigation / Design Decision |
|---|---|---|
| Concurrent Webhook Updates | Optimistic locking collision (`ObjectOptimisticLockingFailureException`) when LIS sends multiple fast result updates. | `BaseEntity` has `@Version Long version`. Spring Data JPA automatically enforces optimistic locking. |
| Cancelled Encounters | LIS posts lab result after clinical encounter has been CANCELLED. | Explicit check in `updateDiagnosticReportResults` enforcing `EncounterCancelledException` (HTTP 400). |
| Timestamp Precision | Divergence between `OffsetDateTime` in DTOs and `Instant` in `BaseEntity`. | Standardize on `java.time.Instant` across Entity, DTO, and Services for UTC consistency. |
| Result Value Data Format | LIS result values can be numerical ("13.5"), text ("Negative"), or complex formatted strings ("120/80"). | `result_value` column is defined as `VARCHAR(255)` to accommodate all qualitative and quantitative formats. |

---

## 4. Architectural Summary & Verification Blueprint
This design seamlessly integrates with the existing architecture:
- Extends `BaseEntity` for consistency across all JPA entities.
- Follows Flyway migration sequence `V4__phase3_schema.sql`.
- Employs MapStruct mappers and Spring Data JPA repositories.
- Adheres to RFC 7807 error responses established in Phase 1 & 2.
