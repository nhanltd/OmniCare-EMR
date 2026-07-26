# Domain & Repository Architecture Analysis: Practitioner Entity & Repository

**Author**: Explorer 2 (Domain & Repository Architecture Analyst)  
**Target Project**: `omnicare-emr-api`  
**Date**: 2026-07-25  

---

## 1. Executive Summary & Objective

This document presents the detailed architectural design and complete implementation specifications for the healthcare practitioner domain model in `omnicare-emr-api`.

The design includes:
1. `PractitionerType` Enum (`DOCTOR`, `NURSE`, `TECHNICIAN`)
2. `Practitioner` Entity inheriting from `BaseEntity` with complete JPA annotations and Lombok configuration.
3. `PractitionerRepository` Spring Data JPA interface supporting identifier uniqueness checks, exclusion checks for updates, and soft-delete-aware retrieval methods.

---

## 2. Analysis of Existing Codebase Patterns

### 2.1 Entity Inheritance (`BaseEntity`)
`BaseEntity.java` (`com.omnicare.emr.entity.BaseEntity`) provides foundational persistence fields:
- `@Id`: `@GeneratedValue(strategy = GenerationType.UUID) private UUID id;`
- `@CreatedDate`: `private Instant createdAt;` (non-updatable)
- `@LastModifiedDate`: `private Instant updatedAt;`
- `@Version`: `private Long version;` (optimistic locking)
- `@Builder.Default`: `private boolean isDeleted = false;` (soft delete)

Entities inheriting from `BaseEntity` use `@SuperBuilder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Getter`, `@Setter`.

### 2.2 Entity Convention (`Patient.java`)
From `Patient.java`, domain entities follow these JPA and Lombok rules:
- `@Entity` and `@Table` with explicit constraint naming (e.g., `uk_patient_identifier`).
- Explicit `@Column` metadata: `name`, `nullable`, `unique`, and length limits.
- Lombok annotations matching parent entity builder hierarchy (`@SuperBuilder`).
- `Practitioner` explicitly includes `@EqualsAndHashCode(callSuper = true)` per specification.

### 2.3 Repository Patterns (`PatientRepository.java`)
From `PatientRepository.java`:
- Package: `com.omnicare.emr.repository`
- Annotated with `@Repository`
- Extends `JpaRepository<Entity, UUID>`
- Uses Spring Data JPA derived query methods for identifier lookups and soft-delete filtering.

---

## 3. Detailed Component Implementation Specifications

### 3.1 `PractitionerType` Enum
- **Package**: `com.omnicare.emr.entity`
- **File**: `omnicare-emr-api/src/main/java/com/omnicare/emr/entity/PractitionerType.java`

```java
package com.omnicare.emr.entity;

/**
 * Enumeration representing the type/role of a healthcare practitioner.
 */
public enum PractitionerType {
    DOCTOR,
    NURSE,
    TECHNICIAN
}
```

---

### 3.2 `Practitioner` Entity
- **Package**: `com.omnicare.emr.entity`
- **File**: `omnicare-emr-api/src/main/java/com/omnicare/emr/entity/Practitioner.java`
- **Table Name**: `practitioner`
- **Unique Constraint**: `uk_practitioner_code` on column `practitioner_code`

#### Column Mappings:
| Field Name | Column Name | Type | JPA Annotations & Constraints |
|---|---|---|---|
| `practitionerCode` | `practitioner_code` | `String` | `@Column(name = "practitioner_code", nullable = false, unique = true, length = 50)` |
| `fullName` | `full_name` | `String` | `@Column(name = "full_name", nullable = false, length = 100)` |
| `specialty` | `specialty` | `String` | `@Column(name = "specialty", nullable = false, length = 100)` |
| `practitionerType` | `practitioner_type` | `PractitionerType` | `@Enumerated(EnumType.STRING)`<br>`@Column(name = "practitioner_type", nullable = false, length = 20)` |
| `phone` | `phone` | `String` | `@Column(name = "phone", length = 20)` |
| `email` | `email` | `String` | `@Column(name = "email", length = 100)` |

#### Proposed Source Code:
```java
package com.omnicare.emr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(
    name = "practitioner",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_practitioner_code", columnNames = {"practitioner_code"})
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class Practitioner extends BaseEntity {

    @Column(name = "practitioner_code", nullable = false, unique = true, length = 50)
    private String practitionerCode;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "specialty", nullable = false, length = 100)
    private String specialty;

    @Enumerated(EnumType.STRING)
    @Column(name = "practitioner_type", nullable = false, length = 20)
    private PractitionerType practitionerType;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;
}
```

---

### 3.3 `PractitionerRepository` Interface
- **Package**: `com.omnicare.emr.repository`
- **File**: `omnicare-emr-api/src/main/java/com/omnicare/emr/repository/PractitionerRepository.java`

#### Query Methods Rationale:
1. `existsByPractitionerCode(String practitionerCode)`: Enables code validation before creating a practitioner.
2. `existsByPractitionerCodeAndIdNot(String practitionerCode, UUID id)`: Enables code uniqueness validation during updates without colliding with the record being updated.
3. `findByIdAndIsDeletedFalse(UUID id)`: Retrieves active practitioner records while filtering out soft-deleted records.
4. `findAllByIsDeletedFalse()`: Fetches all non-deleted practitioners.

#### Proposed Source Code:
```java
package com.omnicare.emr.repository;

import com.omnicare.emr.entity.Practitioner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository for Practitioner entity.
 */
@Repository
public interface PractitionerRepository extends JpaRepository<Practitioner, UUID> {

    /**
     * Check if a practitioner exists with the given practitioner code.
     *
     * @param practitionerCode the practitioner code (e.g., PRAC-001)
     * @return true if practitioner exists, false otherwise
     */
    boolean existsByPractitionerCode(String practitionerCode);

    /**
     * Check if a practitioner exists with the given practitioner code excluding a specific ID.
     * Useful for unique constraint verification during updates.
     *
     * @param practitionerCode the practitioner code
     * @param id the practitioner ID to exclude from match
     * @return true if another practitioner exists with the same code, false otherwise
     */
    boolean existsByPractitionerCodeAndIdNot(String practitionerCode, UUID id);

    /**
     * Find an active (non-soft-deleted) practitioner by ID.
     *
     * @param id the practitioner UUID
     * @return an Optional containing the practitioner if found and not deleted, or empty otherwise
     */
    Optional<Practitioner> findByIdAndIsDeletedFalse(UUID id);

    /**
     * Find all active (non-soft-deleted) practitioners.
     *
     * @return list of active practitioners
     */
    List<Practitioner> findAllByIsDeletedFalse();
}
```

---

## 4. Database Schema Migration Specification

To ensure database consistency with Flyway migrations in `omnicare-emr-api/src/main/resources/db/migration/`:

**File**: `V2__create_practitioner_table.sql`
```sql
CREATE TABLE practitioner (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    practitioner_code VARCHAR(50) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    specialty VARCHAR(100) NOT NULL,
    practitioner_type VARCHAR(20) NOT NULL,
    phone VARCHAR(20),
    email VARCHAR(100),
    CONSTRAINT uk_practitioner_code UNIQUE (practitioner_code)
);
```

---

## 5. Verification & Testing Strategy

### Verification Matrix:
1. **Lombok Hierarchy Check**: Ensure `@SuperBuilder` allows standard builder calls across `BaseEntity` fields (`id`, `createdAt`, `updatedAt`, `version`, `isDeleted`) and `Practitioner` fields.
2. **JPA Audit Integration**: `@EntityListeners(AuditingEntityListener.class)` from `BaseEntity` automatically populates timestamps upon persist/update.
3. **Repository Derived Query Verification**:
   - `existsByPractitionerCode`: verified by Spring Data JPA query generation (`SELECT count(p) > 0 FROM Practitioner p WHERE p.practitionerCode = :practitionerCode`).
   - `existsByPractitionerCodeAndIdNot`: verified by Spring Data JPA query generation (`SELECT count(p) > 0 FROM Practitioner p WHERE p.practitionerCode = :practitionerCode AND p.id <> :id`).
   - `findByIdAndIsDeletedFalse`: verified by Spring Data JPA query generation (`SELECT p FROM Practitioner p WHERE p.id = :id AND p.isDeleted = false`).
   - `findAllByIsDeletedFalse`: verified by Spring Data JPA query generation (`SELECT p FROM Practitioner p WHERE p.isDeleted = false`).
