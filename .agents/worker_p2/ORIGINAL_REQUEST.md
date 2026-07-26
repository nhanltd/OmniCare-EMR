## 2026-07-25T08:10:16Z
You are an Implementation Worker agent for Phase 2 of OmniCare EMR.
Working directory: c:/Users/nhan/Workspace/OmniCare-EMR/.agents/worker_p2
Target project codebase: c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A Forensic Auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Task: Implement Phase 2 (Clinical Core) of OmniCare EMR in `omnicare-emr-api`.

Please review the design reports prepared by Explorers:
- Database & Entities: `c:/Users/nhan/Workspace/OmniCare-EMR/.agents/explorer_p2_1/handoff.md`
- Services & Business Rules: `c:/Users/nhan/Workspace/OmniCare-EMR/.agents/explorer_p2_2/handoff.md`
- Controllers, OpenAPI & Tests: `c:/Users/nhan/Workspace/OmniCare-EMR/.agents/explorer_p2_3/handoff.md`

Component Implementation Checklist:
1. Database Migration:
   - Create `src/main/resources/db/migration/V3__create_encounter_and_observation_tables.sql` with tables `encounter` (FKs to `patient` and `practitioner`, columns `encounter_date`, `status`, `reason`, `BaseEntity` columns) and `observation` (FK to `encounter`, column `value_json` JSONB, `BaseEntity` columns) and appropriate indexes.

2. Domain Entities & Repositories:
   - Create `com.omnicare.emr.entity.EncounterStatus` enum (`PLANNED`, `IN_PROGRESS`, `FINISHED`, `CANCELLED`).
   - Create `com.omnicare.emr.entity.Encounter` entity inheriting from `BaseEntity`.
   - Create `com.omnicare.emr.entity.Observation` entity inheriting from `BaseEntity` with `@JdbcTypeCode(SqlTypes.JSON)` for `JsonNode valueJson`.
   - Create `com.omnicare.emr.repository.EncounterRepository` extending `JpaRepository<Encounter, UUID>` with soft-delete query methods (`findByIdAndIsDeletedFalse`, `findByPatientIdAndIsDeletedFalse`, `findByPractitionerIdAndIsDeletedFalse`, `findByStatusAndIsDeletedFalse`).
   - Create `com.omnicare.emr.repository.ObservationRepository` extending `JpaRepository<Observation, UUID>` with soft-delete query methods (`findByIdAndIsDeletedFalse`, `findByEncounterIdAndIsDeletedFalse`).

3. DTOs & MapStruct Mappers:
   - Create `EncounterRequestDto`, `EncounterResponseDto` in `com.omnicare.emr.dto`.
   - Create `ObservationRequestDto`, `ObservationResponseDto` in `com.omnicare.emr.dto`.
   - Create `EncounterMapper` and `ObservationMapper` in `com.omnicare.emr.dto.mapper` with Spring component model and ignore rules.

4. Exceptions & Business Logic Services:
   - Create `EncounterCancelledException` in `com.omnicare.emr.exception` (returns HTTP 400 Bad Request / 409 Conflict).
   - Update `GlobalExceptionHandler` in `com.omnicare.emr.exception` with RFC 7807 `@ExceptionHandler(EncounterCancelledException.class)` mapping.
   - Create `EncounterService` interface & `EncounterServiceImpl` in `com.omnicare.emr.service`. `createEncounter` defaults status to `PLANNED` if null, validates patient & practitioner exist (throws `ResourceNotFoundException` 404 if missing).
   - Create `ObservationService` interface & `ObservationServiceImpl` in `com.omnicare.emr.service`. `createObservation` validates encounter exists (404 if missing) and status is NOT `CANCELLED` (throws `EncounterCancelledException` if CANCELLED). Stores JSON vitals payload into `value_json`.

5. REST API Controllers & OpenAPI Docs:
   - Create `EncounterController` under `/api/v1/encounters` with POST (create planned encounter), GET (list all encounters), GET `/{id}` (get by ID), with `@Operation` and `@ApiResponses`.
   - Create `ObservationController` under `/api/v1/observations` with POST (record vitals), GET `?encounterId={id}` (get vitals by encounter ID), with `@Operation` and `@ApiResponses`.

6. Dependencies & Test Suite Setup:
   - Add H2 database test dependency in `pom.xml` (`<groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>test</scope>`).
   - Create `src/test/resources/application-test.yml` for testing configuration.
   - Implement unit tests: `EncounterControllerTest`, `ObservationControllerTest`, `EncounterServiceImplTest`, `ObservationServiceImplTest`.
   - Implement integration tests: `EncounterIntegrationTest`, `ObservationIntegrationTest` verifying CRUD, JSONB payload preservation (`{"bloodPressure": "120/80", "heartRate": 75, "temp": 37.0}`), and clinical business rule error handling (404 for missing encounter, 400/409 RFC 7807 for CANCELLED encounter).

7. Compilation & Test Verification:
   - Run Maven build (`mvn clean compile test` or `./mvnw clean compile test`) inside `omnicare-emr-api`.
   - Ensure all tests pass with 0 failures and 0 errors.

Write your implementation report to `c:/Users/nhan/Workspace/OmniCare-EMR/.agents/worker_p2/handoff.md` and `progress.md`. Include the exact build/test output commands and results.
Send a message when finished.
