# Handoff Report: Phase 3 Requirement R3 — Audit Trail via Spring AOP

## 1. Observation

### 1.1 Existing Codebase State
- **Project Location**: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api`
- **Build Configuration**: `pom.xml` uses Spring Boot `3.3.0` and Java `17`.
- **Entity Conventions**: All current entities inherit from `com.omnicare.emr.entity.BaseEntity` (lines 30-52 of `BaseEntity.java`), which supplies `id` (UUID), `createdAt` (Instant), `updatedAt` (Instant), `version` (Long), and `isDeleted` (boolean).
- **Encounter Model**: `Encounter.java` (lines 58-60) defines status with enum `EncounterStatus` (`PLANNED`, `IN_PROGRESS`, `FINISHED`, `CANCELLED`).
- **Flyway Migrations**: Existing scripts in `src/main/resources/db/migration/` are `V1__init_schema.sql`, `V2__create_practitioner_table_and_seed.sql`, and `V3__create_encounter_and_observation_tables.sql`.
- **Missing AOP Dependency**: `pom.xml` lacks `spring-boot-starter-aop`, which is required for Spring AOP / AspectJ annotations (`@Aspect`, `@Around`, `@Pointcut`).

### 1.2 Target Requirement R3 Requirements
- **JPA Entity**: `AuditLog` mapping to table `audit_log` with columns `id` (UUID), `entityId` (UUID of encounter), `oldStatus` (VARCHAR), `newStatus` (VARCHAR), `changedAt` (timestamp), `action` (VARCHAR).
- **AOP Interceptor**: `@Aspect` component intercepting Encounter status transition methods in `EncounterService`.
- **Automated Logging**: Automatic creation and persistence of audit records on status transitions (`PLANNED` -> `IN_PROGRESS`/`FINISHED`/`CANCELLED`).

---

## 2. Logic Chain

1. **Dependency Analysis**: Aspect-oriented programming requires AspectJ annotations (`@Aspect`, `@Around`, etc.). Adding `spring-boot-starter-aop` to `pom.xml` satisfies this prerequisite cleanly in Spring Boot 3.3.0.
2. **Entity & Database Schema Alignment**: Standardizing `AuditLog` to extend `BaseEntity` maintains consistency across all project domain entities (`Patient`, `Practitioner`, `Encounter`, `Observation`). Mapping `entity_id`, `old_status`, `new_status`, `changed_at`, and `action` columns satisfies all R3 data storage requirements.
3. **Advice Selection (`@Around`)**: Capturing status transitions requires knowledge of both the pre-invocation status (`oldStatus`) and post-invocation status (`newStatus`). An `@Around` advice enables querying `EncounterRepository` before `joinPoint.proceed()` and inspecting the return value or updated entity after `proceed()`.
4. **Transaction Integrity**: Spring AOP advice executes within the transactional proxy context of `@Transactional` service methods. If a business method fails and throws an exception, `joinPoint.proceed()` propagates the error, preventing audit record insertion and ensuring full database rollback.
5. **Decoupling Business Logic**: Intercepting status changes via AOP keeps `EncounterServiceImpl` focused purely on clinical domain logic while centralizing audit compliance.

---

## 3. Caveats

- **AOP Proxy Limitations**: Spring AOP utilizes proxy-based invocation. Internal method calls within the same service bean (e.g. `this.updateStatus()`) bypass the AOP proxy unless invoked through an injected self-reference or separate bean. Service calls from Controllers or other services are fully intercepted.
- **Initial Encounter Creation**: When an encounter is created for the first time, `oldStatus` is `null`. If auditing initial creation is desired, the logic records `oldStatus = null` and `newStatus = "PLANNED"`.
- **Flyway Versioning Coordination**: Explorer 1 (`V4__phase3_schema.sql`) and Explorer 2 are also proposing Flyway migrations for Phase 3. The implementer should combine or sequence Flyway scripts (e.g. `V4__phase3_schema.sql` or `V5__create_audit_log_table.sql`).

---

## 4. Conclusion

Requirement R3 should be implemented following the architectural specifications below:

### 4.1 Dependency Addition (`pom.xml`)
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
```

### 4.2 Flyway Schema Migration (`audit_log` Table)
```sql
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

CREATE INDEX idx_audit_log_entity_id ON audit_log(entity_id);
CREATE INDEX idx_audit_log_changed_at ON audit_log(changed_at);
CREATE INDEX idx_audit_log_action ON audit_log(action);
```

### 4.3 Component Specifications
1. **Entity**: `AuditLog.java` in `com.omnicare.emr.entity`, inheriting `BaseEntity`.
2. **Repository**: `AuditLogRepository.java` in `com.omnicare.emr.repository`, extending `JpaRepository<AuditLog, UUID>`.
3. **Custom Annotation**: `@AuditStatusChange` in `com.omnicare.emr.aop`.
4. **Aspect**: `EncounterAuditAspect.java` in `com.omnicare.emr.aop` with `@Around` advice intercepting `EncounterService` status transitions and persisting `AuditLog` records.

Detailed Java code implementations and complete file designs are documented in `c:/Users/nhan/Workspace/OmniCare-EMR/.agents/explorer_p3_3/analysis.md`.

---

## 5. Verification Method

### 5.1 Verification Commands
To build and run tests after implementation:
```bash
# Clean build and compile
mvn clean compile -f c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/pom.xml

# Execute test suite
mvn test -f c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/pom.xml
```

### 5.2 Specific Test Cases to Verify
1. **Unit Test (`EncounterAuditAspectTest`)**:
   - Verify `EncounterAuditAspect` executes `@Around` advice when `updateStatus` or `finalizeEncounter` is invoked.
   - Assert `AuditLogRepository.save()` is called with `oldStatus = "PLANNED"` and `newStatus = "IN_PROGRESS"`.
   - Assert `save()` is NOT called when `oldStatus.equals(newStatus)`.
2. **Integration Test (`AuditLogIntegrationTest`)**:
   - Perform status transition on Encounter (e.g. `PLANNED` -> `FINISHED`).
   - Query `audit_log` table via `AuditLogRepository.findByEntityId(encounterId)` and verify exact audit entry parameters.
3. **Rollback Verification**:
   - Cause failure during `finalizeEncounter` status change (e.g. invalid dosage).
   - Verify exception propagates and `audit_log` table remains empty (0 audit entries).
