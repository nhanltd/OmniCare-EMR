# Phase 2 Execution Plan — OmniCare EMR Clinical Core

## Objectives
Implement Phase 2 (Clinical Core) of OmniCare EMR:
1. **Encounter Module**: Entity (`BaseEntity`, `Patient`, `Practitioner`, `encounterDate`, `status`: `PLANNED`/`IN_PROGRESS`/`FINISHED`/`CANCELLED`, `reason`), Repositories, Service, REST Controller (`/api/v1/encounters`), OpenAPI docs.
2. **Observation Module with JSONB**: Entity (`BaseEntity`, `Encounter`, `value_json` JSONB via `@JdbcTypeCode(SqlTypes.JSON)`), Repositories, Service, REST Controller (`/api/v1/observations`), OpenAPI docs.
3. **Clinical Business Rules**:
   - `POST /api/v1/observations` verifies associated `Encounter` exists (HTTP 404 if not found).
   - `POST /api/v1/observations` verifies associated `Encounter` status is NOT `CANCELLED` (HTTP 400 or 409 error via RFC 7807 `GlobalExceptionHandler`).
4. **Flyway Seeding & Database Schema**: Migration script `V3__create_encounter_and_observation_tables.sql` for PostgreSQL schema.
5. **Testing & Forensic Audit**: Unit & Integration tests for CRUD, JSONB, and clinical business rules + Forensic Integrity Audit.

---

## Milestone Breakdown

### Milestone P2-M1: Database Migration & Entities
- Flyway Migration `V3__create_encounter_and_observation_tables.sql` creating:
  - `encounter` table with `id`, `patient_id` (FK patient), `practitioner_id` (FK practitioner), `encounter_date`, `status`, `reason`, and BaseEntity columns (`created_at`, `updated_at`, `version`, `is_deleted`).
  - `observation` table with `id`, `encounter_id` (FK encounter), `value_json` (JSONB), and BaseEntity columns.
- Enums & JPA Entities:
  - `EncounterStatus` enum (`PLANNED`, `IN_PROGRESS`, `FINISHED`, `CANCELLED`).
  - `Encounter` entity extending `BaseEntity`.
  - `Observation` entity extending `BaseEntity` using Hibernate 6 / Spring Boot 3 `@JdbcTypeCode(SqlTypes.JSON)` for `valueJson`.
- Repositories: `EncounterRepository`, `ObservationRepository`.

### Milestone P2-M2: DTOs, Mappers, Service & Business Rules
- Request & Response DTOs:
  - `EncounterRequestDto`, `EncounterResponseDto`
  - `ObservationRequestDto`, `ObservationResponseDto`
- Mappers: `EncounterMapper`, `ObservationMapper` (MapStruct / Component).
- Custom Exceptions: `EncounterNotFoundException`, `EncounterCancelledException` (or validation exceptions handled by `GlobalExceptionHandler`).
- Services: `EncounterService` and `ObservationService` implementations with transactional methods and clinical business rule checks.

### Milestone P2-M3: REST Controllers & OpenAPI
- `EncounterController` (`/api/v1/encounters`):
  - `POST` create planned encounter -> HTTP 201 Created
  - `GET` list encounters & get by ID -> HTTP 200 OK
- `ObservationController` (`/api/v1/observations`):
  - `POST` record vitals -> HTTP 201 Created
  - `GET` retrieve vitals by encounter ID (`?encounterId={id}`) -> HTTP 200 OK
- `GlobalExceptionHandler` integration: mapping domain exceptions to RFC 7807 `ProblemDetail` (HTTP 404, 400/409).
- Swagger / OpenAPI annotations for all endpoints.

### Milestone P2-M4: E2E Integration, Automated Tests & Forensic Audit
- Unit & Integration tests covering:
  - Encounter creation & status checks
  - Observation creation with dynamic JSONB payload
  - JSONB serialization and retrieval by encounter ID
  - Business validation: rejecting observations for non-existent or CANCELLED encounters
- Reviewer checks, Challenger test execution, and Forensic Integrity Audit (`teamwork_preview_auditor`).

---

## Execution Protocol
- **Exploration**: Spawn 3 parallel `teamwork_preview_explorer` subagents to analyze existing codebase, Flyway migration setup, entity JSONB mapping, and clinical business logic architecture.
- **Implementation**: Spawn 1 `teamwork_preview_worker` to build all Phase 2 components based on synthesized exploration.
- **Review & Challenge**: Spawn 2 `teamwork_preview_reviewer` subagents and 2 `teamwork_preview_challenger` subagents.
- **Forensic Audit**: Spawn 1 `teamwork_preview_auditor` to conduct binary-veto integrity audit.
