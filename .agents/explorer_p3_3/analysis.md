# Detailed Analysis Report: Phase 3 Requirement R3 — Audit Trail via Spring AOP

## Executive Summary
This document presents the codebase inspection, architectural analysis, and technical design for **Requirement R3: Audit Trail via Spring AOP** for OmniCare EMR. The audit trail captures status transitions of clinical encounters (`Encounter`)—specifically transitions from `PLANNED` to `IN_PROGRESS`, `FINISHED`, or `CANCELLED`—and automatically inserts immutable audit records into the `audit_log` database table without polluting core service business logic.

---

## 1. Existing Codebase Inspection

### 1.1 Overview & Architecture
- **Framework**: Spring Boot 3.3.0, Java 17, Maven.
- **Database**: PostgreSQL (runtime), H2 (testing), Flyway database migrations.
- **Package Structure**:
  - `com.omnicare.emr.entity`: Domain entities (`BaseEntity`, `Patient`, `Practitioner`, `Encounter`, `Observation`, `EncounterStatus`).
  - `com.omnicare.emr.repository`: Spring Data JPA repositories (`EncounterRepository`, `PatientRepository`, etc.).
  - `com.omnicare.emr.service`: Business service interfaces and `impl` subpackage (`EncounterService`, `EncounterServiceImpl`).
  - `com.omnicare.emr.controller`: REST Controllers (`EncounterController`).
  - `com.omnicare.emr.config`: Configuration (`JpaConfig` with `@EnableJpaAuditing`).
  - `src/main/resources/db/migration`: Flyway migrations (`V1__init_schema.sql`, `V2__...`, `V3__create_encounter_and_observation_tables.sql`).

### 1.2 Entity Model Conventions
All existing domain entities (`Patient`, `Practitioner`, `Encounter`, `Observation`) extend `BaseEntity`:
```java
public abstract class BaseEntity {
    private UUID id;            // @GeneratedValue(strategy = GenerationType.UUID)
    private Instant createdAt;  // @CreatedDate
    private Instant updatedAt;  // @LastModifiedDate
    private Long version;       // @Version
    private boolean isDeleted;  // soft delete flag
}
```

### 1.3 Missing Dependency Inspection
Inspection of `omnicare-emr-api/pom.xml` reveals that `spring-boot-starter-aop` is currently **not** declared. To enable AspectJ annotations (`@Aspect`, `@Around`, `@Pointcut`, `@AfterReturning`), `spring-boot-starter-aop` must be added to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

---

## 2. Requirement R3 Detailed Analysis & Design

### 2.1 AuditLog JPA Entity Specification
The `AuditLog` entity maps to the `audit_log` database table. Extending `BaseEntity` preserves project-wide consistency and inherits standard UUID identification and auditing metadata.

#### Fields Mapping:
| Field Name | Java Type | Database Column | Constraints | Description |
|---|---|---|---|---|
| `id` | `UUID` | `id` | `PRIMARY KEY` | Inherited from `BaseEntity` |
| `entityId` | `UUID` | `entity_id` | `NOT NULL` | UUID of audited entity (e.g. Encounter ID) |
| `oldStatus` | `String` | `old_status` | `VARCHAR(32), NULLABLE` | Status before transition (`PLANNED`, `IN_PROGRESS`, etc.) |
| `newStatus` | `String` | `new_status` | `VARCHAR(32), NOT NULL` | Status after transition (`IN_PROGRESS`, `FINISHED`, `CANCELLED`) |
| `changedAt` | `Instant` | `changed_at` | `TIMESTAMP WITH TIME ZONE, NOT NULL` | Exact timestamp of status transition |
| `action` | `String` | `action` | `VARCHAR(64), NOT NULL` | Action descriptor (e.g. `STATUS_TRANSITION`) |
| `createdAt` | `Instant` | `created_at` | `TIMESTAMP WITH TIME ZONE, NOT NULL` | Inherited from `BaseEntity` |
| `updatedAt` | `Instant` | `updated_at` | `TIMESTAMP WITH TIME ZONE, NOT NULL` | Inherited from `BaseEntity` |
| `version` | `Long` | `version` | `BIGINT, NOT NULL DEFAULT 0` | Inherited from `BaseEntity` |
| `isDeleted` | `boolean` | `is_deleted` | `BOOLEAN, NOT NULL DEFAULT FALSE` | Inherited from `BaseEntity` |

### 2.2 Flyway Migration Design (`audit_log` Table)
The Flyway DDL script for `audit_log` table creation:

```sql
-- Table: audit_log
CREATE TABLE audit_log (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    entity_id UUID NOT NULL,
    old_status VARCHAR(32),
    new_status VARCHAR(32) NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    action VARCHAR(64) NOT NULL
);

-- Indexes for audit_log query performance
CREATE INDEX idx_audit_log_entity_id ON audit_log(entity_id);
CREATE INDEX idx_audit_log_changed_at ON audit_log(changed_at);
CREATE INDEX idx_audit_log_action ON audit_log(action);
```

---

## 3. Spring AOP Aspect & Pointcut Architecture

### 3.1 Interception Strategy (`@Around` Advice)
An `@Around` advice is selected because it enables state capture **before** and **after** method execution:
1. **Pre-Execution**: Extract `encounterId` from method parameters and fetch current `oldStatus` from database via `EncounterRepository.findById(encounterId)`.
2. **Execution**: Invoke `joinPoint.proceed()`.
3. **Post-Execution**: Extract `newStatus` from return value (`EncounterResponseDto`) or re-query database.
4. **Transition Verification**: If `oldStatus != newStatus` (or transition from `PLANNED` to `IN_PROGRESS`/`FINISHED`/`CANCELLED`), build and persist an `AuditLog` record.
5. **Rollback Handling**: If `proceed()` throws an exception, aspect execution halts immediately. Because advice runs inside the service method's `@Transactional` boundary, no audit record is written and all database changes roll back.

### 3.2 Pointcut Options

#### Declarative Annotation Approach (Recommended):
Define custom annotation `@AuditStatusChange`:
```java
package com.omnicare.emr.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditStatusChange {
    String action() default "STATUS_TRANSITION";
}
```

#### Execution Pointcut Expression:
```java
@Pointcut("execution(* com.omnicare.emr.service.EncounterService.update*(..)) || " +
          "execution(* com.omnicare.emr.service.EncounterService.finalize*(..)) || " +
          "@annotation(com.omnicare.emr.aop.AuditStatusChange)")
public void encounterStatusChangeMethods() {}
```

---

## 4. Implementation Artifact Designs

### 4.1 `AuditLog.java` Entity
```java
package com.omnicare.emr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "audit_log",
    indexes = {
        @Index(name = "idx_audit_log_entity_id", columnList = "entity_id"),
        @Index(name = "idx_audit_log_changed_at", columnList = "changed_at"),
        @Index(name = "idx_audit_log_action", columnList = "action")
    }
)
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuditLog extends BaseEntity {

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @Column(name = "old_status", length = 32)
    private String oldStatus;

    @Column(name = "new_status", nullable = false, length = 32)
    private String newStatus;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    @Column(name = "action", nullable = false, length = 64)
    private String action;
}
```

### 4.2 `AuditLogRepository.java`
```java
package com.omnicare.emr.repository;

import com.omnicare.emr.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByEntityId(UUID entityId);
    List<AuditLog> findByEntityIdOrderByChangedAtDesc(UUID entityId);
    List<AuditLog> findByAction(String action);
}
```

### 4.3 `EncounterAuditAspect.java`
```java
package com.omnicare.emr.aop;

import com.omnicare.emr.dto.EncounterResponseDto;
import com.omnicare.emr.entity.AuditLog;
import com.omnicare.emr.entity.Encounter;
import com.omnicare.emr.entity.EncounterStatus;
import com.omnicare.emr.repository.AuditLogRepository;
import com.omnicare.emr.repository.EncounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class EncounterAuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final EncounterRepository encounterRepository;

    @Pointcut("execution(* com.omnicare.emr.service.EncounterService.update*(..)) || " +
              "execution(* com.omnicare.emr.service.EncounterService.finalize*(..)) || " +
              "@annotation(com.omnicare.emr.aop.AuditStatusChange)")
    public void encounterStatusChangePointcut() {}

    @Around("encounterStatusChangePointcut()")
    public Object auditEncounterStatusTransition(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        UUID encounterId = extractEncounterId(args);

        EncounterStatus oldStatus = null;
        if (encounterId != null) {
            Optional<Encounter> existingEncounter = encounterRepository.findById(encounterId);
            if (existingEncounter.isPresent()) {
                oldStatus = existingEncounter.get().getStatus();
            }
        }

        // Execute actual business method
        Object result = joinPoint.proceed();

        EncounterStatus newStatus = null;
        if (result instanceof EncounterResponseDto dto) {
            newStatus = dto.getStatus();
            if (encounterId == null) {
                encounterId = dto.getId();
            }
        } else if (encounterId != null) {
            Optional<Encounter> updatedEncounter = encounterRepository.findById(encounterId);
            if (updatedEncounter.isPresent()) {
                newStatus = updatedEncounter.get().getStatus();
            }
        }

        // Audit record generation upon status transition
        if (encounterId != null && newStatus != null && oldStatus != newStatus) {
            log.info("Audit Trail: Intercepted status change for Encounter {}: {} -> {}", 
                    encounterId, oldStatus, newStatus);
            AuditLog auditLog = AuditLog.builder()
                    .entityId(encounterId)
                    .oldStatus(oldStatus != null ? oldStatus.name() : null)
                    .newStatus(newStatus.name())
                    .changedAt(Instant.now())
                    .action("STATUS_TRANSITION")
                    .build();

            auditLogRepository.save(auditLog);
        }

        return result;
    }

    private UUID extractEncounterId(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof UUID uuid) {
                return uuid;
            }
        }
        return null;
    }
}
```

---

## 5. Verification Plan

### 5.1 Unit Verification (`EncounterAuditAspectTest`)
- Mock `AuditLogRepository`, `EncounterRepository`, `ProceedingJoinPoint`.
- Verify aspect intercepts method execution and calls `auditLogRepository.save()` with `oldStatus` and `newStatus`.
- Verify aspect does not invoke `save()` if `oldStatus == newStatus`.

### 5.2 Integration Verification (`AuditLogIntegrationTest`)
- Use `@SpringBootTest` with H2 in-memory database.
- Transition encounter from `PLANNED` to `IN_PROGRESS` and `IN_PROGRESS` to `FINISHED`.
- Assert `audit_log` table contains matching entries with exact `entity_id`, `old_status`, `new_status`, `changed_at`, `action`.

### 5.3 Transactional Rollback Verification
- Trigger invalid operation during status transition (e.g. invalid prescription in `finalizeEncounter`).
- Verify exception is thrown and `auditLogRepository.count()` remains 0.

---

## 6. Summary & Recommendations
1. **Dependency Addition**: Add `spring-boot-starter-aop` to `pom.xml`.
2. **Schema & Entity**: Add `AuditLog` entity and Flyway migration table `audit_log`.
3. **AOP Interceptor**: Implement `@Aspect` `EncounterAuditAspect` using `@Around` advice for state inspection before and after proceed.
4. **Clean Decoupling**: Aspect operates declaratively without injecting auditing concerns into business domain code.
