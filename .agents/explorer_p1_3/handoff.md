# Handoff Report: Practitioner API Layer Design (Explorer 3)

**Author**: Explorer 3 (DTOs, Mapper, Service, REST Controller & OpenAPI Analyst)  
**Target Path**: `.agents/explorer_p1_3/handoff.md`  
**Date**: 2026-07-25  

---

## 1. Observation

Directly observed codebase patterns and structural elements in `omnicare-emr-api`:
1. **Existing Controllers & DTOs**:
   - `PatientController.java` (`com.omnicare.emr.controller.PatientController:22-48`): Uses `@RestController`, `@Tag(name = "Patient", description = "Patient Management APIs")`, `@RequestMapping("/api/v1/patients")`, `@Valid @RequestBody`, `@Operation`, and `@ApiResponses`.
   - `PatientRequestDto.java` (`com.omnicare.emr.dto.PatientRequestDto:1-41`): Annotated with Lombok `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` and Jakarta validation annotations (`@NotBlank`, `@Size`, etc.).
   - `PatientResponseDto.java` (`com.omnicare.emr.dto.PatientResponseDto:1-37`): Contains domain fields, `id`, audit timestamps (`createdAt`, `updatedAt`), `version`, and `@JsonProperty("isDeleted") private boolean isDeleted`.
2. **Mapper Pattern**:
   - `PatientMapper.java` (`com.omnicare.emr.dto.mapper.PatientMapper:1-21`): MapStruct interface using `componentModel = MappingConstants.ComponentModel.SPRING` and `unmappedTargetPolicy = ReportingPolicy.IGNORE`.
3. **Service Layer Pattern**:
   - `PatientService.java` (`com.omnicare.emr.service.PatientService:1-19`) & `PatientServiceImpl.java` (`com.omnicare.emr.service.impl.PatientServiceImpl:1-40`): Spring `@Service` with `@Transactional` methods, checking repository existence (`existsByIdentifier`) before persisting, throwing custom exceptions.
4. **Exception Handling & RFC 7807**:
   - `GlobalExceptionHandler.java` (`com.omnicare.emr.exception.GlobalExceptionHandler:1-42`): Extends `ResponseEntityExceptionHandler` with `@RestControllerAdvice`. Maps `DuplicateResourceException` to `ProblemDetail` with `HttpStatus.CONFLICT` (409) and type `URI.create("https://api.omnicare.com/errors/duplicate-resource")`.
   - `DuplicateResourceException.java` (`com.omnicare.emr.exception.DuplicateResourceException:1-20`): Annotated with `@ResponseStatus(HttpStatus.CONFLICT)`.
   - `ResourceNotFoundException.java`: Currently missing in codebase; required for 404 response details.
5. **Explorer 1 & 2 Artifacts**:
   - `Practitioner` entity (`explorer_p1_2/analysis.md:87-136`): Inherits `BaseEntity` (`id`, `createdAt`, `updatedAt`, `version`, `isDeleted`), has `practitionerCode`, `fullName`, `specialty`, `practitionerType`, `phone`, `email`.
   - `PractitionerType` enum (`DOCTOR`, `NURSE`, `TECHNICIAN`).
   - `PractitionerRepository` (`explorer_p1_2/analysis.md:151-201`): Methods `existsByPractitionerCode`, `existsByPractitionerCodeAndIdNot`, `findByIdAndIsDeletedFalse`, `findAllByIsDeletedFalse`.

---

## 2. Logic Chain

1. **DTO Design**: To ensure strict data validation and rich OpenAPI contract documentation, `PractitionerRequestDto` uses Jakarta validation annotations (`@NotBlank`, `@NotNull`, `@Email`, `@Size`) and Swagger `@Schema` annotations with descriptions, examples, and required flags. `PractitionerResponseDto` incorporates all domain fields plus entity audit metadata (`id`, `createdAt`, `updatedAt`, `version`, `isDeleted`).
2. **Mapper Alignment**: Following MapStruct conventions established in `PatientMapper.java`, `PractitionerMapper` provides clean declarative mapping (`toEntity`, `toDto`, `updateEntityFromDto`) ignoring auto-managed audit fields (`id`, `createdAt`, `updatedAt`, `version`, `deleted`).
3. **Service Logic**:
   - Creation logic verifies `existsByPractitionerCode(code)`; throws `DuplicateResourceException` if code is taken.
   - Update logic checks `findByIdAndIsDeletedFalse(id)`; throws `ResourceNotFoundException` if missing/deleted. Verifies `existsByPractitionerCodeAndIdNot(code, id)`; throws `DuplicateResourceException` if code is used by another record. Applies updates via `updateEntityFromDto`.
   - List and Get operations filter by `isDeleted = false`. Missing GET throws `ResourceNotFoundException`.
   - Soft deletion sets `isDeleted = true` after verifying active record existence.
4. **Controller & OpenAPI Contract**: Exposes `/api/v1/practitioners` with `POST` (201 Created), `GET` (200 OK), `GET /{id}` (200 OK), `PUT /{id}` (200 OK), `DELETE /{id}` (204 No Content), decorated with `@Tag(name = "Practitioner Management")`, `@Operation`, and `@ApiResponses`.
5. **Exception Resolution**: Creating `ResourceNotFoundException` and updating `GlobalExceptionHandler` with `@ExceptionHandler(ResourceNotFoundException.class)` ensures all 404 and 409 responses return standard RFC 7807 `ProblemDetail` payloads (`https://api.omnicare.com/errors/resource-not-found` and `https://api.omnicare.com/errors/duplicate-resource`).

---

## 3. Caveats

- **Read-Only Scope**: Per assignment guidelines, this analysis and code specification are provided in `.agents/explorer_p1_3/analysis.md` and `.agents/explorer_p1_3/handoff.md`. Production source files under `omnicare-emr-api/src/main/java` were not modified.
- **Dependency Prerequisites**: The implementation of `PractitionerService` and `PractitionerMapper` depends on the `Practitioner` entity, `PractitionerType` enum, and `PractitionerRepository` specified by Explorer 2.

---

## 4. Conclusion

The design for the Practitioner API layer is fully specified, aligned with existing codebase conventions, compliant with clean architecture, and documented with OpenAPI schemas and RFC 7807 problem details. All 6 requested components (`PractitionerRequestDto`, `PractitionerResponseDto`, `PractitionerMapper`, `PractitionerService`/`PractitionerServiceImpl`, `PractitionerController`, and `GlobalExceptionHandler` update with `ResourceNotFoundException`) are ready for implementation.

---

## 5. Verification Method

1. **Specification File Verification**:
   - Inspect `.agents/explorer_p1_3/analysis.md` for full proposed Java source codes for all 7 classes.
2. **Build and Test Verification (post-implementation)**:
   - Compile code: `mvn clean compile -f omnicare-emr-api/pom.xml`
   - Run tests: `mvn test -f omnicare-emr-api/pom.xml`
3. **Invalidation Conditions**:
   - Failure to throw `DuplicateResourceException` on duplicate `practitionerCode` during creation or update.
   - Return of soft-deleted records (`isDeleted = true`) in `GET /api/v1/practitioners` or `GET /api/v1/practitioners/{id}`.
   - Non-compliant HTTP response status codes (e.g. 500 instead of 404 for missing resource or 409 for duplicate code).
