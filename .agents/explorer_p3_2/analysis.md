# Technical Analysis: Requirement R2 - Transactional Finalize API & Rollback Verification

## 1. Executive Summary
Requirement R2 introduces clinical encounter finalization functionality into `omnicare-emr-api`. Finalization is an atomic clinical operation where an Encounter status is transitioned from `PLANNED` / `IN_PROGRESS` to `FINISHED`, while simultaneously persisting a collection of clinical `Diagnosis` entries and `PrescriptionItem` entries.

To satisfy regulatory and data integrity requirements, the system must guarantee strict transaction atomicity (`@Transactional`). If any component of the request payload (such as an invalid prescription dosage <= 0 or business rule violation) fails validation during processing, the entire transaction MUST roll back, ensuring zero diagnoses or prescriptions persist and the encounter remains in its prior state.

---

## 2. Domain Model Specifications

### 2.1 Base Entity Inheritance
All new domain entities will extend `com.omnicare.emr.entity.BaseEntity`, inheriting:
- `UUID id` (`@Id @GeneratedValue(strategy = GenerationType.UUID)`)
- `Instant createdAt` (`@CreatedDate`)
- `Instant updatedAt` (`@LastModifiedDate`)
- `Long version` (`@Version` for optimistic locking)
- `boolean isDeleted` (`@Builder.Default private boolean isDeleted = false`)

### 2.2 Entity: `Diagnosis`
- **Class Path**: `com.omnicare.emr.entity.Diagnosis`
- **Table Name**: `diagnosis`
- **Attributes**:
  - `Encounter encounter`: `@ManyToOne(fetch = FetchType.LAZY, optional = false)`, `@JoinColumn(name = "encounter_id", nullable = false, foreignKey = @ForeignKey(name = "fk_diagnosis_encounter"))`
  - `String icd10Code`: `@Column(name = "icd10_code", nullable = false, length = 16)`
  - `String description`: `@Column(name = "description", nullable = false, length = 512)`

### 2.3 Entity: `PrescriptionItem`
- **Class Path**: `com.omnicare.emr.entity.PrescriptionItem`
- **Table Name**: `prescription_item`
- **Attributes**:
  - `Encounter encounter`: `@ManyToOne(fetch = FetchType.LAZY, optional = false)`, `@JoinColumn(name = "encounter_id", nullable = false, foreignKey = @ForeignKey(name = "fk_prescription_item_encounter"))`
  - `String medicationName`: `@Column(name = "medication_name", nullable = false, length = 255)`
  - `Double dosage`: `@Column(name = "dosage", nullable = false)`
  - `String frequency`: `@Column(name = "frequency", nullable = false, length = 100)`
  - `String duration`: `@Column(name = "duration", nullable = false, length = 100)`

---

## 3. Database Migration Design (`Flyway`)

- **File Location**: `src/main/resources/db/migration/V4__create_diagnosis_and_prescription_tables.sql`
- **SQL Script Proposal**:

```sql
-- Table: diagnosis
CREATE TABLE diagnosis (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    encounter_id UUID NOT NULL,
    icd10_code VARCHAR(16) NOT NULL,
    description VARCHAR(512) NOT NULL,
    CONSTRAINT fk_diagnosis_encounter FOREIGN KEY (encounter_id) REFERENCES encounter(id)
);

-- Indexes for diagnosis table
CREATE INDEX idx_diagnosis_encounter_id ON diagnosis(encounter_id);
CREATE INDEX idx_diagnosis_icd10_code ON diagnosis(icd10_code);

-- Table: prescription_item
CREATE TABLE prescription_item (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    encounter_id UUID NOT NULL,
    medication_name VARCHAR(255) NOT NULL,
    dosage DOUBLE PRECISION NOT NULL,
    frequency VARCHAR(100) NOT NULL,
    duration VARCHAR(100) NOT NULL,
    CONSTRAINT fk_prescription_item_encounter FOREIGN KEY (encounter_id) REFERENCES encounter(id)
);

-- Indexes for prescription_item table
CREATE INDEX idx_prescription_item_encounter_id ON prescription_item(encounter_id);
```

---

## 4. API Specification & DTO Design

### 4.1 Endpoint Definition
- **HTTP Method**: `POST`
- **Path**: `/api/v1/encounters/{id}/finalize`
- **Consumes**: `application/json`
- **Produces**: `application/json`

### 4.2 DTO Structure

#### Request DTOs:
1. `FinalizeEncounterRequestDto`
```java
package com.omnicare.emr.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalizeEncounterRequestDto {

    @NotEmpty(message = "At least one diagnosis is required")
    @Valid
    private List<DiagnosisRequestDto> diagnoses;

    @NotEmpty(message = "At least one prescription item is required")
    @Valid
    private List<PrescriptionItemRequestDto> prescriptions;
}
```

2. `DiagnosisRequestDto`
```java
package com.omnicare.emr.dto;

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
public class DiagnosisRequestDto {

    @NotBlank(message = "ICD-10 code is required")
    @Size(max = 16, message = "ICD-10 code must not exceed 16 characters")
    private String icd10Code;

    @NotBlank(message = "Description is required")
    @Size(max = 512, message = "Description must not exceed 512 characters")
    private String description;
}
```

3. `PrescriptionItemRequestDto`
```java
package com.omnicare.emr.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class PrescriptionItemRequestDto {

    @NotBlank(message = "Medication name is required")
    @Size(max = 255, message = "Medication name must not exceed 255 characters")
    private String medicationName;

    @NotNull(message = "Dosage is required")
    @Positive(message = "Dosage must be greater than 0")
    private Double dosage;

    @NotBlank(message = "Frequency is required")
    @Size(max = 100, message = "Frequency must not exceed 100 characters")
    private String frequency;

    @NotBlank(message = "Duration is required")
    @Size(max = 100, message = "Duration must not exceed 100 characters")
    private String duration;
}
```

#### Response DTOs:
1. `FinalizeEncounterResponseDto`
```java
package com.omnicare.emr.dto;

import com.omnicare.emr.entity.EncounterStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinalizeEncounterResponseDto {
    private UUID encounterId;
    private EncounterStatus status;
    private Instant updatedAt;
    private List<DiagnosisResponseDto> diagnoses;
    private List<PrescriptionItemResponseDto> prescriptions;
}
```

2. `DiagnosisResponseDto` & `PrescriptionItemResponseDto`

---

## 5. Persistence & Mapping Layer Design

### 5.1 Repositories
- `DiagnosisRepository extends JpaRepository<Diagnosis, UUID>`
  - Method: `List<Diagnosis> findByEncounterIdAndIsDeletedFalse(UUID encounterId);`
- `PrescriptionItemRepository extends JpaRepository<PrescriptionItem, UUID>`
  - Method: `List<PrescriptionItem> findByEncounterIdAndIsDeletedFalse(UUID encounterId);`

### 5.2 MapStruct Mappers
- `DiagnosisMapper`: Maps `DiagnosisRequestDto` to `Diagnosis` entity and `Diagnosis` entity to `DiagnosisResponseDto`.
- `PrescriptionItemMapper`: Maps `PrescriptionItemRequestDto` to `PrescriptionItem` entity and `PrescriptionItem` entity to `PrescriptionItemResponseDto`.

---

## 6. Service Layer & Transaction Control (`@Transactional`)

### 6.1 Service Interface
Update `EncounterService`:
```java
FinalizeEncounterResponseDto finalizeEncounter(UUID id, FinalizeEncounterRequestDto requestDto);
```

### 6.2 Transaction Execution Order & Rollback Guarantee
In `EncounterServiceImpl.java`:

```java
@Override
@Transactional
public FinalizeEncounterResponseDto finalizeEncounter(UUID encounterId, FinalizeEncounterRequestDto requestDto) {
    // 1. Fetch Encounter
    Encounter encounter = encounterRepository.findByIdAndIsDeletedFalse(encounterId)
            .orElseThrow(() -> new ResourceNotFoundException("Encounter not found with ID: " + encounterId));

    // 2. Validate Encounter Status
    if (encounter.getStatus() == EncounterStatus.CANCELLED) {
        throw new EncounterCancelledException("Cannot finalize a cancelled encounter");
    }
    if (encounter.getStatus() == EncounterStatus.FINISHED) {
        throw new IllegalStateException("Encounter is already finalized");
    }

    // 3. Save Diagnoses FIRST
    List<Diagnosis> diagnoses = requestDto.getDiagnoses().stream()
            .map(dto -> {
                Diagnosis d = diagnosisMapper.toEntity(dto);
                d.setEncounter(encounter);
                return d;
            })
            .toList();
    List<Diagnosis> savedDiagnoses = diagnosisRepository.saveAll(diagnoses);

    // 4. Validate & Process Prescription Items SECOND
    // Business Validation: Validate dosage and items inside the transactional boundary
    List<PrescriptionItem> prescriptions = new ArrayList<>();
    for (PrescriptionItemRequestDto dto : requestDto.getPrescriptions()) {
        if (dto.getDosage() == null || dto.getDosage() <= 0) {
            // Throwing RuntimeException triggers Spring @Transactional rollback.
            // Even though savedDiagnoses were persisted in step 3, full rollback occurs.
            throw new IllegalArgumentException("Invalid prescription item: dosage must be greater than 0");
        }
        PrescriptionItem item = prescriptionItemMapper.toEntity(dto);
        item.setEncounter(encounter);
        prescriptions.add(item);
    }
    List<PrescriptionItem> savedPrescriptions = prescriptionItemRepository.saveAll(prescriptions);

    // 5. Update Encounter Status to FINISHED
    encounter.setStatus(EncounterStatus.FINISHED);
    Encounter updatedEncounter = encounterRepository.save(encounter);

    // 6. Build and return Response DTO
    return FinalizeEncounterResponseDto.builder()
            .encounterId(updatedEncounter.getId())
            .status(updatedEncounter.getStatus())
            .updatedAt(updatedEncounter.getUpdatedAt())
            .diagnoses(savedDiagnoses.stream().map(diagnosisMapper::toDto).toList())
            .prescriptions(savedPrescriptions.stream().map(prescriptionItemMapper::toDto).toList())
            .build();
}
```

---

## 7. Business Validation & Rollback Verification Strategy

### 7.1 Key Technical Principle
Spring `@Transactional` rolls back on any unhandled `RuntimeException` (or `Error`).
By executing database save operations for `Diagnosis` prior to checking or saving `PrescriptionItem` objects, we test that database operations performed mid-transaction are reverted when a validation exception is thrown.

### 7.2 Integration Test Plan (`EncounterFinalizeIntegrationTest.java`)
1. **Happy Path Test**:
   - Request with valid diagnoses and prescriptions.
   - Assert HTTP 200 OK.
   - Assert DB state: status = `FINISHED`, diagnoses count > 0, prescription items count > 0.
2. **Rollback Verification Test**:
   - Save initial encounter in status `PLANNED`.
   - Send payload with valid diagnoses BUT invalid prescription item (e.g. dosage = -5.0).
   - Assert HTTP 400 Bad Request returned.
   - Query DB directly via `diagnosisRepository.findByEncounterIdAndIsDeletedFalse(id)`.
   - **Verify `diagnoses.size() == 0`** (0 diagnoses saved).
   - Query DB for encounter status and verify status remains `PLANNED` (not `FINISHED`).

---

## 8. Summary of File Changes Needed for Phase 3 Implementer
1. `src/main/resources/db/migration/V4__create_diagnosis_and_prescription_tables.sql`
2. `src/main/java/com/omnicare/emr/entity/Diagnosis.java`
3. `src/main/java/com/omnicare/emr/entity/PrescriptionItem.java`
4. `src/main/java/com/omnicare/emr/dto/DiagnosisRequestDto.java`
5. `src/main/java/com/omnicare/emr/dto/DiagnosisResponseDto.java`
6. `src/main/java/com/omnicare/emr/dto/PrescriptionItemRequestDto.java`
7. `src/main/java/com/omnicare/emr/dto/PrescriptionItemResponseDto.java`
8. `src/main/java/com/omnicare/emr/dto/FinalizeEncounterRequestDto.java`
9. `src/main/java/com/omnicare/emr/dto/FinalizeEncounterResponseDto.java`
10. `src/main/java/com/omnicare/emr/dto/mapper/DiagnosisMapper.java`
11. `src/main/java/com/omnicare/emr/dto/mapper/PrescriptionItemMapper.java`
12. `src/main/java/com/omnicare/emr/repository/DiagnosisRepository.java`
13. `src/main/java/com/omnicare/emr/repository/PrescriptionItemRepository.java`
14. Update `src/main/java/com/omnicare/emr/service/EncounterService.java`
15. Update `src/main/java/com/omnicare/emr/service/impl/EncounterServiceImpl.java`
16. Update `src/main/java/com/omnicare/emr/controller/EncounterController.java`
17. Update `src/main/java/com/omnicare/emr/exception/GlobalExceptionHandler.java` (if needed for `IllegalArgumentException` handling)
18. `src/test/java/com/omnicare/emr/integration/EncounterFinalizeIntegrationTest.java`
