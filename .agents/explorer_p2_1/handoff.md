# Phase 2 Database Schema Migration & JPA Domain Entities Design Report

**Target Project**: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api`  
**Working Directory**: `c:/Users/nhan/Workspace/OmniCare-EMR/.agents/explorer_p2_1`  
**Author**: Explorer Agent (Phase 2)  
**Date**: 2026-07-25  

---

## 1. Observation

### Existing Database Schema & Entity Architecture
1. **Flyway Migration Scripts**:
   - `V1__init_schema.sql` (`src/main/resources/db/migration/V1__init_schema.sql:1-13`):
     - Table `patient`: `id UUID PRIMARY KEY`, `created_at TIMESTAMP WITH TIME ZONE NOT NULL`, `updated_at TIMESTAMP WITH TIME ZONE NOT NULL`, `version BIGINT NOT NULL`, `is_deleted BOOLEAN NOT NULL DEFAULT FALSE`, `identifier VARCHAR(20) NOT NULL UNIQUE`, `full_name VARCHAR(100) NOT NULL`, `gender VARCHAR(10)`, `birth_date DATE`, `phone_number VARCHAR(15)`.
   - `V2__create_practitioner_table_and_seed.sql` (`src/main/resources/db/migration/V2__create_practitioner_table_and_seed.sql:1-14`):
     - Table `practitioner`: `id UUID PRIMARY KEY`, `created_at TIMESTAMP WITH TIME ZONE NOT NULL`, `updated_at TIMESTAMP WITH TIME ZONE NOT NULL`, `version BIGINT NOT NULL DEFAULT 0`, `is_deleted BOOLEAN NOT NULL DEFAULT FALSE`, `practitioner_code VARCHAR(50) NOT NULL UNIQUE`, `full_name VARCHAR(100) NOT NULL`, `specialty VARCHAR(100) NOT NULL`, `practitioner_type VARCHAR(20) NOT NULL`, `phone VARCHAR(20)`, `email VARCHAR(100)`.

2. **Domain Entities**:
   - `BaseEntity` (`src/main/java/com/omnicare/emr/entity/BaseEntity.java:1-53`):
     - `@MappedSuperclass` with `@EntityListeners(AuditingEntityListener.class)`
     - `@Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;`
     - `@CreatedDate @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;`
     - `@LastModifiedDate @Column(name = "updated_at", nullable = false) private Instant updatedAt;`
     - `@Version @Column(name = "version", nullable = false) private Long version;`
     - `@Builder.Default @Column(name = "is_deleted", nullable = false) private boolean isDeleted = false;`
     - Annotation stack: `@Getter @Setter @SuperBuilder @NoArgsConstructor @AllArgsConstructor`
   - `Patient` (`src/main/java/com/omnicare/emr/entity/Patient.java:1-44`):
     - Extends `BaseEntity`, mapped to table `patient`.
   - `Practitioner` (`src/main/java/com/omnicare/emr/entity/Practitioner.java:1-50`):
     - Extends `BaseEntity`, mapped to table `practitioner`. Uses `@EqualsAndHashCode(callSuper = true)`.
     - Enum property `PractitionerType` mapped via `@Enumerated(EnumType.STRING)`.

3. **Repositories**:
   - `PatientRepository` (`src/main/java/com/omnicare/emr/repository/PatientRepository.java`): extends `JpaRepository<Patient, UUID>`.
   - `PractitionerRepository` (`src/main/java/com/omnicare/emr/repository/PractitionerRepository.java`): extends `JpaRepository<Practitioner, UUID>`, contains methods like `findByIdAndIsDeletedFalse(UUID id)` and `findAllByIsDeletedFalse()`.

4. **Dependencies & Framework Versions** (`pom.xml:8-24, 26-90`):
   - Spring Boot: `3.3.0`
   - Java: `17`
   - Flyway Core + `flyway-database-postgresql:10.10.0`
   - Spring Boot Starter Data JPA (includes Hibernate 6.x)
   - Jackson Databind available on classpath via `spring-boot-starter-web`

---

## 2. Logic Chain

### Reasoning & Design Choices

1. **Table Naming & Column Convention**:
   - Consistent with existing tables `patient` and `practitioner`, table names must be lower-case singular: `encounter` and `observation`.
   - Primary key: `id UUID PRIMARY KEY`.
   - Audit & Soft-Delete columns: `created_at TIMESTAMP WITH TIME ZONE NOT NULL`, `updated_at TIMESTAMP WITH TIME ZONE NOT NULL`, `version BIGINT NOT NULL DEFAULT 0`, `is_deleted BOOLEAN NOT NULL DEFAULT FALSE`.
   - `encounter` foreign key constraints:
     - `patient_id UUID NOT NULL REFERENCES patient(id)`
     - `practitioner_id UUID NOT NULL REFERENCES practitioner(id)`
   - `observation` foreign key constraint:
     - `encounter_id UUID NOT NULL REFERENCES encounter(id)`
   - `value_json` in `observation`: mapped to PostgreSQL native `JSONB` data type (`JSONB NOT NULL`) to store dynamic clinical vitals (e.g., blood pressure, heart rate, temperature).
   - Indexing:
     - Foreign key indexes on `encounter(patient_id)`, `encounter(practitioner_id)`, `encounter(status)`.
     - Foreign key index on `observation(encounter_id)`.
     - GIN index on `observation USING gin(value_json)` for efficient JSON payload query operations.

2. **JPA Mapping & Hibernate 6 JSONB Strategy**:
   - Spring Boot 3.3 / Hibernate 6 natively supports JSON mapping via `@JdbcTypeCode(SqlTypes.JSON)`.
   - Mapped field in `Observation` entity: `@JdbcTypeCode(SqlTypes.JSON) @Column(name = "value_json", columnDefinition = "jsonb", nullable = false) private JsonNode valueJson;` (or `Map<String, Object> valueJson`).
   - Using `com.fasterxml.jackson.databind.JsonNode` allows lossless storage and retrieval of arbitrary JSON tree structures, matching Jackson serialization seamlessly.
   - `EncounterStatus` enum: `@Enumerated(EnumType.STRING)` with values `PLANNED`, `IN_PROGRESS`, `FINISHED`, `CANCELLED`.
   - All entities inherit from `BaseEntity` using Lombok `@SuperBuilder`, `@EqualsAndHashCode(callSuper = true)`, `@NoArgsConstructor`, `@AllArgsConstructor`.

3. **Repository Methods**:
   - In alignment with `PractitionerRepository`, repositories must support soft-delete awareness:
     - `findByIdAndIsDeletedFalse(UUID id)`
     - `findByPatientIdAndIsDeletedFalse(UUID patientId)`
     - `findByPractitionerIdAndIsDeletedFalse(UUID practitionerId)`
     - `findByStatusAndIsDeletedFalse(EncounterStatus status)`
     - `findByEncounterIdAndIsDeletedFalse(UUID encounterId)`

---

## 3. Caveats

1. **Database Dialect**: The `JSONB` data type and `USING gin` index structure are PostgreSQL-specific. H2 in-memory test databases (if used for unit tests) must be configured in PostgreSQL compatibility mode (`MODE=PostgreSQL`) or H2 version 2.x which supports `JSON`/`JSONB`.
2. **Jackson JsonNode vs Map<String, Object>**: `JsonNode` provides strict Jackson tree manipulation. If DTOs pass raw `Map<String, Object>` or custom POJOs, Spring Data JPA / Jackson converts seamlessly. `JsonNode` is recommended as the entity field type.
3. **Soft Delete Filtering**: JPA `findById` does not automatically exclude `is_deleted = true` unless explicit query methods (e.g., `findByIdAndIsDeletedFalse`) or `@Where`/`@SQLRestriction` annotations are applied. Following existing project conventions, explicit repository query methods are provided.

---

## 4. Conclusion

The following complete artifacts are proposed for Phase 2 implementation.

### Artifact 1: Flyway Migration Script `V3__create_encounter_and_observation_tables.sql`
**Path**: `src/main/resources/db/migration/V3__create_encounter_and_observation_tables.sql`

```sql
-- Table: encounter
CREATE TABLE encounter (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    patient_id UUID NOT NULL,
    practitioner_id UUID NOT NULL,
    encounter_date TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(32) NOT NULL,
    reason VARCHAR(512),
    CONSTRAINT fk_encounter_patient FOREIGN KEY (patient_id) REFERENCES patient(id),
    CONSTRAINT fk_encounter_practitioner FOREIGN KEY (practitioner_id) REFERENCES practitioner(id)
);

-- Indexes for encounter table
CREATE INDEX idx_encounter_patient_id ON encounter(patient_id);
CREATE INDEX idx_encounter_practitioner_id ON encounter(practitioner_id);
CREATE INDEX idx_encounter_status ON encounter(status);

-- Table: observation
CREATE TABLE observation (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    encounter_id UUID NOT NULL,
    value_json JSONB NOT NULL,
    CONSTRAINT fk_observation_encounter FOREIGN KEY (encounter_id) REFERENCES encounter(id)
);

-- Indexes for observation table
CREATE INDEX idx_observation_encounter_id ON observation(encounter_id);
CREATE INDEX idx_observation_value_json ON observation USING gin(value_json);
```

---

### Artifact 2: `EncounterStatus.java`
**Path**: `src/main/java/com/omnicare/emr/entity/EncounterStatus.java`

```java
package com.omnicare.emr.entity;

/**
 * Enumeration representing the operational status of a patient encounter.
 */
public enum EncounterStatus {
    PLANNED,
    IN_PROGRESS,
    FINISHED,
    CANCELLED
}
```

---

### Artifact 3: `Encounter.java`
**Path**: `src/main/java/com/omnicare/emr/entity/Encounter.java`

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
    name = "encounter",
    indexes = {
        @Index(name = "idx_encounter_patient_id", columnList = "patient_id"),
        @Index(name = "idx_encounter_practitioner_id", columnList = "practitioner_id"),
        @Index(name = "idx_encounter_status", columnList = "status")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Encounter extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "patient_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_encounter_patient")
    )
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "practitioner_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_encounter_practitioner")
    )
    private Practitioner practitioner;

    @Column(name = "encounter_date", nullable = false)
    private Instant encounterDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private EncounterStatus status;

    @Column(name = "reason", length = 512)
    private String reason;
}
```

---

### Artifact 4: `Observation.java`
**Path**: `src/main/java/com/omnicare/emr/entity/Observation.java`

```java
package com.omnicare.emr.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "observation",
    indexes = {
        @Index(name = "idx_observation_encounter_id", columnList = "encounter_id")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Observation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "encounter_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_observation_encounter")
    )
    private Encounter encounter;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "value_json", columnDefinition = "jsonb", nullable = false)
    private JsonNode valueJson;
}
```

---

### Artifact 5: `EncounterRepository.java`
**Path**: `src/main/java/com/omnicare/emr/repository/EncounterRepository.java`

```java
package com.omnicare.emr.repository;

import com.omnicare.emr.entity.Encounter;
import com.omnicare.emr.entity.EncounterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Encounter entity.
 */
@Repository
public interface EncounterRepository extends JpaRepository<Encounter, UUID> {

    /**
     * Find an active (non-deleted) encounter by ID.
     */
    Optional<Encounter> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Find all active encounters for a specific patient.
     */
    List<Encounter> findByPatientIdAndIsDeletedFalse(UUID patientId);

    /**
     * Find all active encounters for a specific practitioner.
     */
    List<Encounter> findByPractitionerIdAndIsDeletedFalse(UUID practitionerId);

    /**
     * Find all active encounters by status.
     */
    List<Encounter> findByStatusAndIsDeletedFalse(EncounterStatus status);
}
```

---

### Artifact 6: `ObservationRepository.java`
**Path**: `src/main/java/com/omnicare/emr/repository/ObservationRepository.java`

```java
package com.omnicare.emr.repository;

import com.omnicare.emr.entity.Observation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Observation entity.
 */
@Repository
public interface ObservationRepository extends JpaRepository<Observation, UUID> {

    /**
     * Find an active (non-deleted) observation by ID.
     */
    Optional<Observation> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Find all active observations associated with a given encounter ID.
     */
    List<Observation> findByEncounterIdAndIsDeletedFalse(UUID encounterId);
}
```

---

## 5. Verification Method

To independently verify this design once implemented:

1. **Compilation Verification**:
   - Run `./mvnw clean compile` (or `mvn clean compile`) from `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api`.
   - Ensures all JPA annotations, Hibernate 6 imports (`@JdbcTypeCode(SqlTypes.JSON)`), Jackson classes, and Lombok `@SuperBuilder` annotations compile without errors.

2. **Flyway Migration & Schema Verification**:
   - Run `./mvnw flyway:migrate` or start the Spring Boot application against a running PostgreSQL container.
   - Inspect PostgreSQL schema tables:
     - `\d encounter` -> verify foreign keys `fk_encounter_patient`, `fk_encounter_practitioner`, timestamps, indexes.
     - `\d observation` -> verify column `value_json` is type `jsonb`, FK `fk_observation_encounter`, and GIN index `idx_observation_value_json`.

3. **JPA Entity & Repository Integration Tests**:
   - Run standard Maven test suite `./mvnw test`.
   - Verify creation of an `Encounter` associated with valid `Patient` and `Practitioner`.
   - Verify creation of an `Observation` associated with `Encounter` storing JSON node `{"bloodPressure": "120/80", "heartRate": 75}` and retrieving it.
   - Verify `findByIdAndIsDeletedFalse` excludes soft-deleted records.
