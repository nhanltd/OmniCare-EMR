## 2026-07-25T08:58:25Z

You are the Implementation Worker for Phase 3 (LIS Webhook, Transaction Finalize & Audit Trail) of OmniCare EMR.
Working directory for implementation: c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api
Your agent directory: c:/Users/nhan/Workspace/OmniCare-EMR/.agents/worker_p3

Refer to the Explorer Handoff Reports for exact specs:
- LIS Webhook & DiagnosticReport: c:/Users/nhan/Workspace/OmniCare-EMR/.agents/explorer_p3_1/handoff.md and analysis.md
- Transactional Finalize API & Rollback: c:/Users/nhan/Workspace/OmniCare-EMR/.agents/explorer_p3_2/handoff.md and analysis.md
- Audit Trail via Spring AOP: c:/Users/nhan/Workspace/OmniCare-EMR/.agents/explorer_p3_3/handoff.md and analysis.md

Requirements & Tasks to Implement:

1. **Dependency update (`pom.xml`)**:
   Add `spring-boot-starter-aop` to `pom.xml` if not present.

2. **Flyway Migration (`src/main/resources/db/migration/V4__phase3_schema.sql`)**:
   Create tables with proper foreign keys to `encounter(id)`, primary keys (UUID), audit columns (`created_at`, `updated_at`, `version`, `is_deleted`), and indexes:
   - `diagnostic_report`: `id`, `created_at`, `updated_at`, `version`, `is_deleted`, `encounter_id` (FK), `ordered_at` (TIMESTAMP WITH TIME ZONE), `result_received_at` (TIMESTAMP WITH TIME ZONE), `test_code`, `test_name`, `result_value`, `unit`, `reference_range`, `flag`, `status`.
   - `diagnosis`: `id`, `created_at`, `updated_at`, `version`, `is_deleted`, `encounter_id` (FK), `icd10_code`, `description`.
   - `prescription_item`: `id`, `created_at`, `updated_at`, `version`, `is_deleted`, `encounter_id` (FK), `medication_name`, `dosage` (DOUBLE PRECISION or INTEGER), `frequency`, `duration`.
   - `audit_log`: `id`, `created_at`, `updated_at`, `version`, `is_deleted`, `entity_id` (UUID), `old_status`, `new_status`, `changed_at` (TIMESTAMP WITH TIME ZONE), `action`.

3. **Domain Entities (extending `BaseEntity`)**:
   - `DiagnosticReport` (linked to `Encounter`, fields: `orderedAt`, `resultReceivedAt`, `testCode`, `testName`, `resultValue`, `unit`, `referenceRange`, `flag`, `status`). Include `DiagnosticReportStatus` enum (`ORDERED`, `FINAL`, `CANCELLED`).
   - `Diagnosis` (linked to `Encounter`, fields: `icd10Code`, `description`).
   - `PrescriptionItem` (linked to `Encounter`, fields: `medicationName`, `dosage`, `frequency`, `duration`).
   - `AuditLog` (fields: `entityId`, `oldStatus`, `newStatus`, `changedAt`, `action`).

4. **Repositories, DTOs & Mappers**:
   - `DiagnosticReportRepository`, `DiagnosisRepository`, `PrescriptionItemRepository`, `AuditLogRepository`.
   - DTOs: `DiagnosticReportCreateRequestDto`, `DiagnosticReportResultUpdateDto`, `DiagnosticReportResponseDto`, `FinalizeEncounterRequestDto` (containing list of `DiagnosisRequestDto` and `PrescriptionItemRequestDto`), `FinalizeEncounterResponseDto`.
   - MapStruct Mappers: `DiagnosticReportMapper`, `DiagnosisMapper`, `PrescriptionItemMapper`, `AuditLogMapper`.

5. **Services & Controllers**:
   - `DiagnosticReportService` & `DiagnosticReportController`: Endpoint `PUT /api/v1/diagnostic-reports/{id}/results`. Validates report & encounter status (reject if CANCELLED via `EncounterCancelledException`), updates test results, sets `status` (default `FINAL`), and sets `resultReceivedAt = Instant.now()`.
   - `EncounterService` & `EncounterController`: Endpoint `POST /api/v1/encounters/{id}/finalize`. Marked with `@Transactional`. Logic:
     - Check encounter status (throw exception if CANCELLED or already FINISHED).
     - Save all `Diagnosis` entities FIRST via `diagnosisRepository.saveAll(...)`.
     - Process `PrescriptionItem` entities SECOND. Perform business validation (e.g., if `dosage <= 0` or missing medication name, throw runtime exception like `IllegalArgumentException` / `InvalidPrescriptionException`).
     - Throwing runtime exception MUST trigger complete transaction rollback (0 diagnoses persisted, encounter status unchanged).
     - If all valid, set encounter status to `FINISHED`, save encounter, return response DTO.
   - `EncounterAuditAspect` (`@Aspect`) in `com.omnicare.emr.aspect`:
     - Uses `@Around` or `@AfterReturning` advice to intercept encounter status transitions in `EncounterService`.
     - Reads `oldStatus` and `newStatus` (e.g. `PLANNED` -> `FINISHED` or `PLANNED` -> `CANCELLED`).
     - Automatically creates and persists an `AuditLog` entity into `audit_log` with `entityId = encounter.getId()`, `oldStatus`, `newStatus`, `changedAt = Instant.now()`, `action = "ENCOUNTER_STATUS_CHANGE"`.

6. **Automated Unit & Integration Test Suite**:
   - `DiagnosticReportIntegrationTest`: Verify `PUT /api/v1/diagnostic-reports/{id}/results` updates report result & `resultReceivedAt`. Verify rejection on CANCELLED encounter.
   - `EncounterFinalizeIntegrationTest`:
     - Test valid finalize: saves diagnoses and prescriptions, updates status to `FINISHED`.
     - Test invalid prescription (dosage <= 0): throws exception and rolls back transaction. Verify via `diagnosisRepository` that 0 diagnoses were persisted for that encounter.
   - `AuditLogIntegrationTest`: Verify Encounter status transition automatically generates an `AuditLog` entry in `audit_log` without manual audit calls in business logic.

7. **Verification**:
   - Run `mvn clean compile` and `mvn test` in `omnicare-emr-api`. All tests MUST pass.
   - Document build and test outputs in your handoff report `c:/Users/nhan/Workspace/OmniCare-EMR/.agents/worker_p3/handoff.md`.
