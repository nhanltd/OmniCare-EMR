# Original User Request

## Follow-up — 2026-07-25T08:07:30Z

# Teamwork Project Prompt

Implement Phase 2 (Clinical Core) of the OmniCare EMR roadmap: Encounter and Observation (JSONB) Entities, business validations, and REST APIs.

Working directory: c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api
Integrity mode: development

## Requirements

### R1. Encounter Entity & REST APIs
Create the `Encounter` entity inheriting from `BaseEntity` linking a `Patient` (`patient_id`) and a `Practitioner` (`practitioner_id`).
Include fields: `encounterDate`, `status` (Enum: `PLANNED`, `IN_PROGRESS`, `FINISHED`, `CANCELLED`), and `reason`.
Implement clean architecture REST APIs under `/api/v1/encounters` supporting POST (create planned encounter) and GET (list/details by ID).

### R2. Observation Entity with JSONB & REST APIs
Create the `Observation` entity inheriting from `BaseEntity` linking to an `Encounter` (`encounter_id`).
Implement a JSONB database column `value_json` in PostgreSQL (using native Spring Boot 3 / Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)` or equivalent) to flexibly store dynamic health vitals (blood pressure, heart rate, temperature, SpO2, etc.) as a JSON structure without discrete columns.
Implement clean architecture REST APIs under `/api/v1/observations` supporting POST (record vitals) and GET (retrieve vitals by encounter ID).

### R3. Clinical Business Rules & Validation
When recording an Observation via POST `/api/v1/observations`:
- The associated `Encounter` must exist in the database; if not, throw `ResourceNotFoundException` (HTTP 404).
- The associated `Encounter` must NOT be in `CANCELLED` status; if it is cancelled, throw an appropriate domain/validation exception (HTTP 400 or 409) handled by `GlobalExceptionHandler` (RFC 7807).

## Acceptance Criteria

### Functionality & Verification
- [ ] Calling `POST /api/v1/encounters` with valid `patientId` and `practitionerId` creates an encounter in `PLANNED` status and returns HTTP 201 Created.
- [ ] Calling `POST /api/v1/observations` with a dynamic JSON payload in `value_json` (e.g., `{"bloodPressure": "120/80", "heartRate": 75, "temp": 37.0}`) successfully stores it in PostgreSQL as a JSONB column.
- [ ] Calling `POST /api/v1/observations` for a `CANCELLED` encounter is rejected with a standardized RFC 7807 error response.
- [ ] Calling `GET /api/v1/observations?encounterId={id}` returns the recorded vitals with the exact JSON structure preserved.
- [ ] Automated unit and integration test suite verifies CRUD operations, JSONB serialization/deserialization, and clinical business rule constraints without errors.


## Follow-up — 2026-07-25T08:54:55Z

# Teamwork Project Prompt

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
