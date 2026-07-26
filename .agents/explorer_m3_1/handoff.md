# Handoff Report — Milestone M3 Technical Exploration

## 1. Observation
- **Repository Structure**: Located under `omnicare-emr-api/src/main/java/com/omnicare/emr`. Package structure exists for `controller`, `dto`, `entity`, `exception`, `repository`, `service`.
- **Existing Entities**:
  - `BaseEntity.java` (`com.omnicare.emr.entity.BaseEntity`): `id` (UUID PK), `createdAt` (Instant), `updatedAt` (Instant), `version` (Long), `isDeleted` (boolean).
  - `Patient.java` (`com.omnicare.emr.entity.Patient`): extends `BaseEntity`, table `patient`, columns: `identifier` (String, length 20, unique), `fullName` (String, length 100), `gender` (String, length 10), `birthDate` (LocalDate), `phoneNumber` (String, length 15).
- **Configuration**:
  - `JpaConfig.java` is annotated with `@EnableJpaAuditing`.
  - `application.yml` configures PostgreSQL at `localhost:5432/omnicare_db`.
- **E2E Integration Requirements**:
  - `e2e-tests/test_tier2_happy_path.py`: expects `POST /api/v1/patients` to return HTTP 201 with JSON body containing UUID `id`, and verifies physical DB record (`version=0`, `is_deleted=false`).
  - `e2e-tests/test_tier3_validation.py`: checks duplicate CCCD identifier rejection (returning 409 or 400 with `status` or `error` key in JSON response) and missing identifier payload validation (400 Bad Request).
  - `e2e-tests/test_tier4_integrity.py`: verifies UTF-8 Vietnamese diacritics persistence (`fullName = "Nguyễn Thị Ánh Tuyết"`) and UUID uniqueness across requests.

## 2. Logic Chain
1. **Repository Design**: `PatientRepository` extending `JpaRepository<Patient, UUID>` with `boolean existsByIdentifier(String identifier)` allows O(1) duplicate checks using indexed identifier column.
2. **DTO Layer**:
   - `PatientRequestDto` uses Bean Validation (`@NotBlank`, `@Size`, `@Past`) to enforce API input constraints before service execution.
   - `PatientResponseDto` maps entity fields including UUID, audit metadata, and `@JsonProperty("isDeleted")` to guarantee Jackson serializes boolean getter as `"isDeleted"` matching E2E assertions.
   - `ErrorResponseDto` standardizes error payloads (`timestamp`, `status`, `error`, `message`, `path`).
3. **Exception Handling**:
   - `DuplicateResourceException` represents duplicate identifier conflicts.
   - `GlobalExceptionHandler` with `@RestControllerAdvice` intercepts `DuplicateResourceException` (409 Conflict), `MethodArgumentNotValidException` (400 Bad Request), and unhandled exceptions (500 Internal Server Error).
4. **Service & Controller**:
   - `PatientServiceImpl` handles transactionality (`@Transactional`), checks duplicate identifier, persists entity, and returns `PatientResponseDto`.
   - `PatientController` exposes `@PostMapping` at `/api/v1/patients` with `@Valid` payload validation, returning HTTP `201 Created`.

## 3. Caveats
- Database migration (Flyway/Liquibase) is currently relying on Hibernate `ddl-auto: update` as configured in `application.yml`.
- Entity `Patient` uses `String gender`. Validation on `PatientRequestDto` currently restricts `@Size(max = 10)`. If a strict enum (e.g. `MALE`, `FEMALE`, `OTHER`) is mandated in future milestones, a converter or enum validation will be required.

## 4. Conclusion
The technical analysis and Java code blueprints created in `analysis.md` provide complete, production-ready source code definitions fully aligned with existing domain models, database schema, Spring Boot 3.2 conventions, and E2E test assertions.

## 5. Verification Method
- **Unit & Web Layer Verification**: Run `mvn test` inside `omnicare-emr-api`.
- **End-to-End Verification**: Run PyTest suite in `e2e-tests/`:
  `pytest test_tier1_infrastructure.py test_tier2_happy_path.py test_tier3_validation.py test_tier4_integrity.py`
