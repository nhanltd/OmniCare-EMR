# Original User Request

## Initial Request — 2026-07-24T14:42:32Z

# Teamwork Project Prompt — Draft

> Status: Launched
> Goal: Craft prompt → get user approval → delegate to teamwork_preview

Build the core backend infrastructure and initial API for the OmniCare EMR (Electronic Medical Record) system using Spring Boot and PostgreSQL.

Working directory: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api`
Integrity mode: development

## Requirements

### R1. Database Infrastructure
Create a `docker-compose.yml` file in the root of the project to set up a PostgreSQL container. It must run independently and expose the standard port (5432) for the application to connect.

### R2. Spring Boot Initialization and Structure
Initialize a Spring Boot project (Java) with the following dependencies: Spring Web, Spring Data JPA, PostgreSQL Driver, Lombok, and Validation.
Organize the project into the strict directory structure requested (controller, service, repository, entity, dto, exception, config). 
Include `pom.xml`, `Dockerfile`, and a basic `README.md`.

### R3. Core Data Model
Implement an abstract `BaseEntity` containing the administrative fields: `id` (UUID), `createdAt`, `updatedAt`, `version` (Optimistic Locking), and `isDeleted` (Soft Delete).
Implement the `Patient` entity that inherits from `BaseEntity` and map it to the database table using JPA annotations.
Configure `application.yml` to connect to the PostgreSQL database and enable `auto-ddl` to generate tables automatically.

### R4. End-to-End API Implementation
Implement the `PatientRepository` interface extending JpaRepository.
Implement `PatientService` and `PatientServiceImpl` containing business logic (e.g., checking if the patient's identifier/CCCD already exists).
Implement `PatientController` exposing the `POST /api/v1/patients` endpoint to create a new patient.

## Acceptance Criteria

### Infrastructure & Bootstrapping
- [ ] Running `docker-compose up -d` successfully starts the PostgreSQL database.
- [ ] The Spring Boot application compiles and starts successfully without errors.
- [ ] Hibernate auto-generates the `patient` table in the database upon application startup.

### API Functionality
- [ ] Sending a valid `POST /api/v1/patients` request successfully saves a new patient to the database and returns a `201 Created` status with the patient data (including the generated UUID).
- [ ] Sending a `POST` request with an existing patient identifier (CCCD) is caught by the service logic and returns an appropriate error response via the `GlobalExceptionHandler`.
- [ ] The `patient` table correctly tracks `createdAt`, `updatedAt`, `version`, and `isDeleted`.

## Follow-up — 2026-07-25T05:39:00Z

# Teamwork Project Prompt

Implement Phase 1 of the OmniCare EMR roadmap: Practitioner Entity, CRUD REST APIs, and Flyway database seeding.

Working directory: c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api
Integrity mode: development

## Requirements

### R1. Practitioner Entity & Architecture
Create the `Practitioner` entity inheriting from `BaseEntity` with the following standard medical fields:
- `practitionerCode`: String (Unique employee / license number)
- `fullName`: String
- `specialty`: String (e.g., CARDIOLOGY, PEDIATRICS, GENERAL_SURGERY)
- `practitionerType`: Enum (`DOCTOR`, `NURSE`, `TECHNICIAN`)
- `phone`: String
- `email`: String

### R2. CRUD REST APIs
Implement clean architecture layers (`Controller`, `Service`, `Repository`, `Mapper`/`DTO`) for Practitioner under `/api/v1/practitioners`.
Supports Create, Read (List all & get by ID), Update, and Soft Delete.
Integrate OpenAPI/Swagger documentation annotations for all endpoints.

### R3. Database Seeding via Flyway
Create a Flyway migration script (`V2__seed_data.sql`) that inserts at least 5 realistic mock practitioners (including doctors and nurses with various specialties) into the `practitioner` table to serve as test data for clinical phases.

## Acceptance Criteria

### Functionality & Verification
- [ ] Calling `GET /api/v1/practitioners` via Swagger UI or HTTP client successfully returns HTTP 200 OK with the list of at least 5 seeded practitioners.
- [ ] Calling `POST /api/v1/practitioners` with a valid DTO creates a new practitioner in the database and returns HTTP 201 Created with the generated UUID.
- [ ] Calling `POST /api/v1/practitioners` with a duplicate `practitionerCode` returns an appropriate error response handled by `GlobalExceptionHandler` (RFC 7807).
- [ ] The `practitioner` table correctly tracks audit timestamps and versioning (`createdAt`, `updatedAt`, `version`, `isDeleted`).


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



