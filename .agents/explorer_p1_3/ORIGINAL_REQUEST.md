## 2026-07-25T12:40:21+07:00
You are Explorer 3 (DTOs, Mapper, Service, REST Controller & OpenAPI Analyst) working in `c:/Users/nhan/Workspace/OmniCare-EMR/.agents/explorer_p1_3`.

Your task is to analyze existing API controllers, DTOs, mappers, services, and exception handlers in `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/` (e.g. `PatientController.java`, `PatientService.java`, `PatientRequestDto.java`, `PatientResponseDto.java`, `PatientMapper.java`, `GlobalExceptionHandler.java`, `OpenApiConfig.java`).

Design the clean architecture API layer for Practitioner:
1. `PractitionerRequestDto`:
   - Validation annotations (`@NotBlank`, `@NotNull`, `@Email`, etc.)
   - Swagger `@Schema` annotations for input fields
2. `PractitionerResponseDto`:
   - Contains all practitioner fields plus `id`, `createdAt`, `updatedAt`, `version`, `isDeleted`.
   - Swagger `@Schema` annotations
3. `PractitionerMapper`:
   - MapStruct or Component mapper converting entity <-> DTO
4. `PractitionerService` & `PractitionerServiceImpl`:
   - `createPractitioner(PractitionerRequestDto)`: check duplicate `practitionerCode` -> throw `DuplicateResourceException`
   - `getAllPractitioners()`: return list of active practitioners (`isDeleted = false`)
   - `getPractitionerById(UUID)`: return practitioner or throw `ResourceNotFoundException` if missing or soft-deleted
   - `updatePractitioner(UUID, PractitionerRequestDto)`: check duplicate code for other entity ID -> throw `DuplicateResourceException`, update fields, return response DTO
   - `deletePractitioner(UUID)`: soft delete (`isDeleted = true`)
5. `PractitionerController` under `/api/v1/practitioners`:
   - `POST /api/v1/practitioners`: 201 Created
   - `GET /api/v1/practitioners`: 200 OK
   - `GET /api/v1/practitioners/{id}`: 200 OK
   - `PUT /api/v1/practitioners/{id}`: 200 OK
   - `DELETE /api/v1/practitioners/{id}`: 204 No Content (or 200 OK)
   - Include OpenAPI annotations (`@Tag(name = "Practitioner Management")`, `@Operation`, `@ApiResponse`, etc.)
6. `GlobalExceptionHandler`:
   - Verify `DuplicateResourceException` yields HTTP 409 Conflict with RFC 7807 problem details
   - Verify `ResourceNotFoundException` yields HTTP 404 Not Found with RFC 7807 problem details

Write your complete analysis to `.agents/explorer_p1_3/analysis.md` and write your handoff report to `.agents/explorer_p1_3/handoff.md`. Communicate your results back to the parent agent via `send_message`.
