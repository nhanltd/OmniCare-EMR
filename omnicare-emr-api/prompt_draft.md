# Teamwork Project Prompt — Draft

> Status: Launched
> Goal: Craft prompt → get user approval → delegate to teamwork_preview

Implement Phase 3 (LIS Webhook, Transaction Finalize & Audit Trail) of the OmniCare EMR roadmap using Spring Boot and PostgreSQL.

Working directory: c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api
Integrity mode: development

## Requirements

### R1. Webhook LIS API & DiagnosticReport Entity
Create the `DiagnosticReport` entity inheriting from `BaseEntity` linking to `Encounter` (`encounter_id`). It must include timestamps: `orderedAt` (when lab test was ordered) and `resultReceivedAt` (when LIS webhook delivered result) to prepare for Phase 4 big data analytics.
Implement clean architecture REST API endpoint `PUT /api/v1/diagnostic-reports/{id}/results` accepting standard LIS JSON payload: `{"testCode": "WBC", "testName": "Bạch cầu", "resultValue": "10.5", "unit": "G/L", "referenceRange": "4.0 - 10.0", "flag": "HIGH", "status": "FINAL"}`. Upon receipt, update the report result and set `resultReceivedAt` to the current timestamp.

### R2. Transactional Finalize API with Rollback Verification
Create 2 separate domain entities inheriting from `BaseEntity`:
- `Diagnosis`: fields `icd10Code`, `description`, `encounter` (`encounter_id`)
- `PrescriptionItem`: fields `medicationName`, `dosage`, `frequency`, `duration`, `encounter` (`encounter_id`)
Implement REST API endpoint `POST /api/v1/encounters/{id}/finalize` accepting a combined payload of diagnosis list and prescription list.
Apply `@Transactional` annotation to ensure transactional integrity: when finalizing, save all `Diagnosis` records first, then save all `PrescriptionItem` records. If any prescription item violates business validation rules (e.g., dosage <= 0 or medication out of stock), throw an exception so that the entire transaction automatically rollbacks, leaving zero partial diagnoses in the database.

### R3. Audit Trail via Spring AOP
Create a custom JPA entity `AuditLog` mapping to table `audit_log` with columns: `id`, `entityId` (UUID of the encounter), `oldStatus`, `newStatus`, `changedAt` (timestamp), and `action` (string description).
Implement a Spring AOP Aspect (`@Aspect`) that intercepts any method changing an `Encounter` status (e.g. in `EncounterService` or state transitions from `PLANNED` to `IN_PROGRESS`/`FINISHED`/`CANCELLED`).
The aspect must automatically insert an audit record into `audit_log` whenever an encounter's status changes, cleanly separating audit cross-cutting concerns from core clinical business logic.

## Acceptance Criteria

### Functionality & Verification
- [ ] Calling `PUT /api/v1/diagnostic-reports/{id}/results` with valid LIS JSON payload updates the report and records the `resultReceivedAt` timestamp.
- [ ] Calling `POST /api/v1/encounters/{id}/finalize` with a valid payload saves both `Diagnosis` and `PrescriptionItem` records, changes encounter status to `FINISHED`, and returns HTTP 200/201.
- [ ] Calling `POST /api/v1/encounters/{id}/finalize` where a prescription item is invalid (e.g., dosage <= 0) throws an error and triggers a database transaction rollback, verifying via repository that no `Diagnosis` records were persisted for that encounter.
- [ ] Transitioning an `Encounter` status automatically creates an entry in `audit_log` containing exact `oldStatus`, `newStatus`, and `changedAt` verified via Spring AOP interception without boilerplate logging code in the service methods.
- [ ] Automated unit and integration test suite verifies transactional rollback behavior, LIS webhook processing, and AOP audit logging without errors.

---
*Next: when approved → delegate via invoke_subagent (see Delegation Protocol)*
