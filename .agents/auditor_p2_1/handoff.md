# Forensic Audit Report — OmniCare EMR Phase 2

**Work Product**: `omnicare-emr-api` (Phase 2 Clinical Core Implementation)
**Profile**: General Project
**Integrity Mode**: Development
**Verdict**: CLEAN

---

## 1. Observation

### Source Code Analysis

#### Database Migration Script
- File: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/resources/db/migration/V3__create_encounter_and_observation_tables.sql`
- Lines 1–37: Table `encounter` created with UUID PK, foreign keys `fk_encounter_patient` (to `patient.id`) and `fk_encounter_practitioner` (to `practitioner.id`), audit columns (`created_at`, `updated_at`, `version`, `is_deleted`), indexes `idx_encounter_patient_id`, `idx_encounter_practitioner_id`, `idx_encounter_status`. Table `observation` created with `encounter_id` FK, `value_json JSONB NOT NULL`, index `idx_observation_encounter_id`, and GIN index `idx_observation_value_json USING gin(value_json)`.

#### Entities & Enums
- File: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/entity/EncounterStatus.java`
  - Lines 6–11: Enum defining `PLANNED`, `IN_PROGRESS`, `FINISHED`, `CANCELLED`.
- File: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/entity/Encounter.java`
  - Lines 37–64: `Encounter` extends `BaseEntity`. `@ManyToOne` lazy relationships to `Patient` and `Practitioner`. Status mapped as `@Enumerated(EnumType.STRING)`.
- File: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/entity/Observation.java`
  - Lines 34–47: `Observation` extends `BaseEntity`. `@ManyToOne` lazy relationship to `Encounter`. `valueJson` mapped using Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)` and `@Column(name = "value_json", columnDefinition = "jsonb", nullable = false)`.

#### Repositories
- File: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/repository/EncounterRepository.java`
  - Lines 16–47: Extends `JpaRepository<Encounter, UUID>`. Query methods: `findByIdAndIsDeletedFalse`, `findByPatientIdAndIsDeletedFalse`, `findByPractitionerIdAndIsDeletedFalse`, `findByStatusAndIsDeletedFalse`, `findAllByIsDeletedFalse`, `existsByIdAndIsDeletedFalse`.
- File: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/repository/ObservationRepository.java`
  - Lines 15–31: Extends `JpaRepository<Observation, UUID>`. Query methods: `findByIdAndIsDeletedFalse`, `findByEncounterIdAndIsDeletedFalse`, `findAllByIsDeletedFalse`.

#### Service Implementation & Clinical Validation Rules
- File: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/service/impl/EncounterServiceImpl.java`
  - Lines 36–55: `createEncounter` verifies `patientRepository.findByIdAndIsDeletedFalse` and `practitionerRepository.findByIdAndIsDeletedFalse`. Throws `ResourceNotFoundException` if missing. Assigns default status `EncounterStatus.PLANNED` if `requestDto.getStatus()` is null. Saves via `encounterRepository.save(encounter)`.
- File: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/service/impl/ObservationServiceImpl.java`
  - Lines 34–48: `createObservation` validates encounter existence (`encounterRepository.findByIdAndIsDeletedFalse`), throwing `ResourceNotFoundException` (404) if absent. Checks `if (encounter.getStatus() == EncounterStatus.CANCELLED)` and throws `EncounterCancelledException` (400/409). Saves `valueJson` payload via `observationRepository.save()`.

#### Exception Handling (RFC 7807)
- File: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/exception/GlobalExceptionHandler.java`
  - Lines 42–48: Handles `EncounterCancelledException` by constructing `ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage())`, setting title `"Encounter Cancelled"` and URI type `"https://api.omnicare.com/errors/encounter-cancelled"`.

#### REST Controllers
- File: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/controller/EncounterController.java`
  - Lines 48–52: `@PostMapping` `/api/v1/encounters` returns HTTP 201 Created.
  - Lines 63–86: `@GetMapping` `/api/v1/encounters` and `/api/v1/encounters/{id}` return HTTP 200 OK.
- File: `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/controller/ObservationController.java`
  - Lines 49–53: `@PostMapping` `/api/v1/observations` returns HTTP 201 Created.
  - Lines 66–72: `@GetMapping` `/api/v1/observations?encounterId={id}` returns HTTP 200 OK.

#### Test Suites
- Controller unit tests (`EncounterControllerTest.java`, `ObservationControllerTest.java`): Test WebMvc endpoints, JSON payload serialization, 400 Bad Request on missing fields, 404 Not Found on missing resources, and 400 RFC 7807 on cancelled encounter.
- Service unit tests (`EncounterServiceImplTest.java`, `ObservationServiceImplTest.java`): Verify business rule enforcement, default status `PLANNED`, and exception throwing.
- Integration tests (`EncounterIntegrationTest.java`, `ObservationIntegrationTest.java`): Full `@SpringBootTest` testing with H2 database in PostgreSQL mode, validating real database persistence, JSONB payload preservation, and end-to-end endpoint execution.

---

## 2. Logic Chain

1. **Static Analysis of Core Logic**:
   - Observation 1 & 4 show that entities (`Encounter`, `Observation`) correctly extend `BaseEntity`, specify foreign keys, and map dynamic JSON vitals using Hibernate 6 `@JdbcTypeCode(SqlTypes.JSON)`.
   - Observation 5 shows genuine service logic in `EncounterServiceImpl` and `ObservationServiceImpl`. `ObservationServiceImpl` enforces the clinical rule checking if an encounter is `CANCELLED` and throws `EncounterCancelledException` when recording observations on a cancelled encounter.
   - Observation 6 shows `GlobalExceptionHandler` converts domain exceptions into RFC 7807 standard `ProblemDetail` responses.
   - No hardcoded test responses, fake return constants, facade methods, or dummy stubs were found in any source file.

2. **Test Suite Integrity**:
   - Observation 7 demonstrates comprehensive test coverage across unit controller tests, unit service tests, and full spring boot integration tests.
   - Test cases make explicit assertions on database IDs, status fields, JSON payload structure, and RFC 7807 error responses. No tautological or self-certifying assertions are present.

3. **Behavioral & Database Interaction**:
   - Database migration `V3__create_encounter_and_observation_tables.sql` establishes proper schema constraints and a GIN index on `value_json`.
   - `ObservationIntegrationTest` verifies saving dynamic JSON nodes (`{"bloodPressure": "120/80", "heartRate": 75, "temp": 37.0}`) and retrieving them via GET with complete structural preservation.

---

## 3. Caveats

- **Test Execution Environment**: Maven test execution via `run_command` was attempted (`mvn clean test` and `cmd /c mvn test`), but the environment operating mode timed out waiting for user confirmation. The audit verdict relies on complete static source inspection of all Java classes, Flyway DDL scripts, configuration files, and test suites.

---

## 4. Conclusion

The Phase 2 codebase for `omnicare-emr-api` is **CLEAN**.
All requirements (R1 Encounter Entity & REST APIs, R2 Observation Entity with JSONB & REST APIs, R3 Clinical Business Rules & Validation) have been genuinely implemented with authentic domain models, JPA mapping, Spring Data repositories, MapStruct mappers, RFC 7807 exception handling, and thorough unit and integration test coverage. No cheating, facade implementations, or hardcoded return values were detected.

---

## 5. Verification Method

To independently verify the test suite and project build, execute the following commands in `omnicare-emr-api`:

```bash
cd c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api
mvn clean test
```

Inspect the test results in `target/surefire-reports` or terminal output to verify 100% test pass rate.
