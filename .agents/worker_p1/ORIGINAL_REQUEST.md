## 2026-07-25T05:42:19Z

You are Worker P1 (Phase 1 Implementation Worker) working in `c:/Users/nhan/Workspace/OmniCare-EMR/.agents/worker_p1`.

Your task is to implement Phase 1 of the OmniCare EMR backend in `omnicare-emr-api` based on the detailed designs provided by Explorers 1, 2, and 3:

1. **Flyway Migration & Seed Script**:
   - Path: `omnicare-emr-api/src/main/resources/db/migration/V2__create_practitioner_table_and_seed.sql`
   - DDL for `practitioner` table (`id` UUID PRIMARY KEY, `created_at`, `updated_at`, `version`, `is_deleted`, `practitioner_code` unique, `full_name`, `specialty`, `practitioner_type`, `phone`, `email`, `CONSTRAINT uk_practitioner_code UNIQUE (practitioner_code)`).
   - SQL INSERT statements seeding at least 5 realistic mock practitioners (doctors, nurses, technicians across CARDIOLOGY, PEDIATRICS, GENERAL_SURGERY, DERMATOLOGY, ORTHOPEDICS) with version=0, is_deleted=false, valid timestamps, and unique practitioner codes.

2. **Domain & Repository Architecture**:
   - `PractitionerType` enum (`DOCTOR`, `NURSE`, `TECHNICIAN`) in `com.omnicare.emr.entity` (or `com.omnicare.emr.enums`).
   - `Practitioner` entity in `com.omnicare.emr.entity` extending `BaseEntity` with table mapping `practitioner`, `@EqualsAndHashCode(callSuper = true)`, `@SuperBuilder`, Lombok getters/setters, `@Enumerated(EnumType.STRING)` for `practitionerType`.
   - `PractitionerRepository` interface in `com.omnicare.emr.repository` extending `JpaRepository<Practitioner, UUID>` with methods `existsByPractitionerCode`, `existsByPractitionerCodeAndIdNot`, `findByIdAndIsDeletedFalse`, `findAllByIsDeletedFalse`.

3. **DTOs, Mapper, Exceptions, Service & REST Controller**:
   - `ResourceNotFoundException` in `com.omnicare.emr.exception` annotated with `@ResponseStatus(HttpStatus.NOT_FOUND)`.
   - `GlobalExceptionHandler` update in `com.omnicare.emr.exception` handling `ResourceNotFoundException` (404 Not Found) and `DuplicateResourceException` (409 Conflict) returning RFC 7807 `ProblemDetail` responses.
   - `PractitionerRequestDto` in `com.omnicare.emr.dto` with Jakarta Validation (`@NotBlank`, `@NotNull`, `@Email`, etc.) and Swagger `@Schema` annotations.
   - `PractitionerResponseDto` in `com.omnicare.emr.dto` containing all entity and audit fields (`id`, `createdAt`, `updatedAt`, `version`, `isDeleted`) with `@Schema` annotations.
   - `PractitionerMapper` in `com.omnicare.emr.dto.mapper` (using MapStruct or Spring `@Component`).
   - `PractitionerService` & `PractitionerServiceImpl` in `com.omnicare.emr.service` & `service.impl`:
     - `createPractitioner(PractitionerRequestDto)`: check duplicate `practitionerCode` -> throw `DuplicateResourceException`
     - `getAllPractitioners()`: return active practitioners (`isDeleted = false`)
     - `getPractitionerById(UUID)`: return practitioner or throw `ResourceNotFoundException`
     - `updatePractitioner(UUID, PractitionerRequestDto)`: check duplicate code excluding current ID -> throw `DuplicateResourceException`, update fields, return DTO
     - `deletePractitioner(UUID)`: soft delete (`isDeleted = true`)
   - `PractitionerController` in `com.omnicare.emr.controller` mapping `/api/v1/practitioners`:
     - `POST /api/v1/practitioners` (201 Created)
     - `GET /api/v1/practitioners` (200 OK)
     - `GET /api/v1/practitioners/{id}` (200 OK)
     - `PUT /api/v1/practitioners/{id}` (200 OK)
     - `DELETE /api/v1/practitioners/{id}` (204 No Content or 200 OK)
     - Full OpenAPI 3 / Swagger annotations (`@Tag`, `@Operation`, `@ApiResponses`, etc.).

4. **Unit / Integration Tests**:
   - Write comprehensive unit and/or integration tests for `PractitionerService` and `PractitionerController` in `src/test/java/com/omnicare.emr/`.

5. **Build & Test Verification**:
   - Run `mvn clean test` (or `./mvnw clean test` / `powershell -Command "mvn clean test"`) in `omnicare-emr-api` working directory.
   - Verify that compilation succeeds, Flyway scripts validate/migrate, and all unit/integration tests pass.

Refer to Explorer analysis reports in `.agents/explorer_p1_1/analysis.md`, `.agents/explorer_p1_2/analysis.md`, and `.agents/explorer_p1_3/analysis.md` for exact specifications.
Write your implementation summary and verification results to `.agents/worker_p1/handoff.md`. Communicate your completion message back via `send_message`.
