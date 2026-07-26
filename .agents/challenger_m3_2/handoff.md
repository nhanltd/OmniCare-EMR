# M3 REST API, Validation & Duplicate Conflict Handling Verification Report

## Observation

1. **`PatientController.java` (`src/main/java/com/omnicare/emr/controller/PatientController.java`)**:
   - Lines 31-35:
     ```java
     @PostMapping
     public ResponseEntity<PatientResponseDto> createPatient(@Valid @RequestBody PatientRequestDto request) {
         PatientResponseDto response = patientService.createPatient(request);
         return ResponseEntity.status(HttpStatus.CREATED).body(response);
     }
     ```
   - Correctly maps POST `/api/v1/patients` and applies `@Valid` to trigger Spring Bean Validation.

2. **`PatientServiceImpl.java` (`src/main/java/com/omnicare/emr/service/impl/PatientServiceImpl.java`)**:
   - Lines 25-29:
     ```java
     if (patientRepository.existsByIdentifier(requestDto.getIdentifier())) {
         throw new DuplicateResourceException(
                 "Patient with identifier '" + requestDto.getIdentifier() + "' already exists"
         );
     }
     ```
   - Checks `existsByIdentifier` in Java service layer prior to database insertion.

3. **`PatientRequestDto.java` (`src/main/java/com/omnicare/emr/dto/PatientRequestDto.java`)**:
   - Lines 24-39:
     ```java
     @NotBlank(message = "Identifier is required")
     @Size(min = 9, max = 20, message = "Identifier must be between 9 and 20 characters")
     private String identifier;

     @NotBlank(message = "Full name is required")
     @Size(max = 100, message = "Full name must not exceed 100 characters")
     private String fullName;

     @Size(max = 10, message = "Gender must not exceed 10 characters")
     private String gender;

     @Past(message = "Birth date must be in the past")
     private LocalDate birthDate;

     @Size(max = 15, message = "Phone number must not exceed 15 characters")
     private String phoneNumber;
     ```

4. **`GlobalExceptionHandler.java` (`src/main/java/com/omnicare/emr/exception/GlobalExceptionHandler.java`)**:
   - Lines 21-32: `@ExceptionHandler(DuplicateResourceException.class)` handles `DuplicateResourceException` and returns HTTP 409 CONFLICT.
   - Lines 34-49: `@ExceptionHandler(MethodArgumentNotValidException.class)` handles validation failures and returns HTTP 400 BAD_REQUEST.
   - Lines 51-62: `@ExceptionHandler(Exception.class)` handles all other unhandled exceptions and returns HTTP 500 INTERNAL_SERVER_ERROR.
   - **Missing Handlers**:
     - No `@ExceptionHandler(DataIntegrityViolationException.class)` for database unique constraint violations.
     - No `@ExceptionHandler(HttpMessageNotReadableException.class)` for malformed JSON or unparseable payload types (e.g. invalid date format).

5. **`PatientControllerTest.java` (`src/test/java/com/omnicare/emr/controller/PatientControllerTest.java`)**:
   - Lines 40-73: `createPatient_Returns201Created` tests happy path creation.
   - Lines 75-89: `createPatient_MissingIdentifier_Returns400BadRequest` tests null/missing identifier.
   - Lines 91-108: `createPatient_DuplicateIdentifier_Returns409Conflict` tests mocked `DuplicateResourceException` -> 409 Conflict.
   - **Gaps**: Only 3 test cases exist. Missing tests for identifier min/max length, fullName max length, future/today birthDate, malformed JSON, and multiple validation errors.

6. **`PatientServiceImplTest.java` (`src/test/java/com/omnicare/emr/service/PatientServiceImplTest.java`)**:
   - Lines 63-79: `createPatient_Success` tests normal save.
   - Lines 81-91: `createPatient_DuplicateIdentifier_ThrowsDuplicateResourceException` tests pre-check duplicate detection.
   - **Gaps**: Only 2 test cases exist. Missing tests for field mapping assertions, optional null fields, and database concurrency/integrity exceptions.

7. **Tool Execution Result**:
   - Attempted `run_command` to run `mvn clean test` in `omnicare-emr-api`. The command timed out waiting for user approval ("Permission prompt for action 'command' on target 'mvn clean test' timed out waiting for user response"). Proceeded with static inspection and analytical stress testing.

---

## Adversarial Challenge Report

### Challenge Summary
**Overall risk assessment**: MEDIUM

### Challenges

#### [High] Challenge 1: Concurrency Race Condition in Duplicate Conflict Handling (DB Unique Constraint vs Application Pre-check)
- **Assumption challenged**: Pre-checking `patientRepository.existsByIdentifier()` is sufficient to guarantee non-duplicate insertions and HTTP 409 Conflict responses.
- **Attack scenario**: Two concurrent requests attempt to register a patient with the exact same identifier (`079123456789`) simultaneously. Both requests execute `existsByIdentifier()` concurrently; both receive `false`. Both proceed to `save()`. The database table `patient` has a `@UniqueConstraint(name = "uk_patient_identifier", columnNames = {"identifier"})`. One thread succeeds; the second thread causes Spring Data JPA to throw a `org.springframework.dao.DataIntegrityViolationException`.
- **Blast radius**: Because `GlobalExceptionHandler` does NOT have an explicit `@ExceptionHandler(DataIntegrityViolationException.class)`, the database unique constraint violation falls through to `@ExceptionHandler(Exception.class)` (lines 51-62), returning **HTTP 500 Internal Server Error** instead of **HTTP 409 Conflict**.
- **Mitigation**: Add `@ExceptionHandler(DataIntegrityViolationException.class)` to `GlobalExceptionHandler` and map it to `HttpStatus.CONFLICT` (409).

#### [Medium] Challenge 2: Malformed JSON / Unparseable Payload Returns HTTP 500 instead of HTTP 400
- **Assumption challenged**: All invalid client request payloads are caught by `@Valid` and return HTTP 400 Bad Request.
- **Attack scenario**: A client sends a malformed JSON payload (e.g. invalid syntax `{ "identifier": "079123456789", }` or an unparseable date `"birthDate": "1995-13-45"`). Jackson parsing fails and Spring throws `org.springframework.http.converter.HttpMessageNotReadableException`.
- **Blast radius**: `GlobalExceptionHandler` lacks a handler for `HttpMessageNotReadableException`. The exception falls through to `@ExceptionHandler(Exception.class)`, returning **HTTP 500 Internal Server Error** with raw internal exception details instead of **HTTP 400 Bad Request**.
- **Mitigation**: Add `@ExceptionHandler(HttpMessageNotReadableException.class)` to `GlobalExceptionHandler` returning `HttpStatus.BAD_REQUEST` (400).

#### [Medium] Challenge 3: `@Past` Validation Rejects Newborn Patients Registered Today
- **Assumption challenged**: Birth date must be strictly in the past (`@Past`).
- **Attack scenario**: A medical staff member registers a newborn baby on the same day as birth (`birthDate = LocalDate.now()`).
- **Blast radius**: The `@Past` annotation in `PatientRequestDto.java:35` requires `birthDate` to be strictly less than the current date (`LocalDate.now()`). Today's date is rejected with a validation error (`400 Bad Request`), preventing registration of newborn patients on their day of birth.
- **Mitigation**: Change `@Past` to `@PastOrPresent` in `PatientRequestDto.java:35`.

#### [Low] Challenge 4: Incomplete Test Coverage in `PatientControllerTest` and `PatientServiceImplTest`
- **Assumption challenged**: Unit and WebMvc tests adequately cover the REST API boundary and validation rules.
- **Attack scenario**: Edge cases such as short identifiers (`< 9` chars e.g. `"1234567"`), long identifiers (`> 20` chars), long names (`> 100` chars), and null optional fields pass through unchecked because existing tests only verify 3 happy/basic paths.
- **Blast radius**: Regression risks for input validation rules and mapping logic.
- **Mitigation**: Add test cases for boundary validation (`@Size(min=9, max=20)`, `@Size(max=100)`, `@PastOrPresent`), null optional fields, malformed JSON, and exception handling branches.

---

## Stress Test Results

| Scenario | Expected Behavior | Actual / Predicted Behavior | Result |
|---|---|---|---|
| Valid patient creation payload (POST `/api/v1/patients`) | HTTP 201 Created + JSON body with `id`, `createdAt`, `version=0` | HTTP 201 Created with mapped fields | PASS |
| Missing `identifier` | HTTP 400 Bad Request ("Identifier is required") | HTTP 400 Bad Request with field message | PASS |
| Duplicate `identifier` (Service pre-check) | HTTP 409 Conflict ("Patient with identifier ... already exists") | HTTP 409 Conflict with message | PASS |
| Concurrent Duplicate `identifier` (DB level violation) | HTTP 409 Conflict | HTTP 500 Internal Server Error (`DataIntegrityViolationException` unhandled) | FAIL |
| Malformed JSON or invalid date format payload | HTTP 400 Bad Request ("Invalid request body format") | HTTP 500 Internal Server Error (`HttpMessageNotReadableException` unhandled) | FAIL |
| Newborn patient registered today (`birthDate = LocalDate.now()`) | HTTP 201 Created | HTTP 400 Bad Request ("Birth date must be in the past") | FAIL |
| Identifier length < 9 chars (e.g. `"12345678"`) | HTTP 400 Bad Request | HTTP 400 Bad Request (handled by `@Size`), but NOT covered in test suite | PARTIAL (Untested) |
| Identifier length > 20 chars | HTTP 400 Bad Request | HTTP 400 Bad Request (handled by `@Size`), but NOT covered in test suite | PARTIAL (Untested) |

---

## Logic Chain

1. **Observation 1 & 4**: `PatientController` delegates creation to `PatientServiceImpl`. `PatientServiceImpl` relies on `patientRepository.existsByIdentifier()` to detect duplicates before saving.
2. **Observation 4**: `GlobalExceptionHandler` defines handlers for `DuplicateResourceException` (409), `MethodArgumentNotValidException` (400), and generic `Exception` (500).
3. **Logic Step A**: Under concurrent requests, two threads can evaluate `existsByIdentifier()` simultaneously before either performs `save()`. The second `save()` triggers a database unique constraint violation (`uk_patient_identifier`), resulting in a Spring `DataIntegrityViolationException`.
4. **Logic Step B**: Because `DataIntegrityViolationException` is not caught by `GlobalExceptionHandler`, it falls back to `@ExceptionHandler(Exception.class)`, which outputs an HTTP 500 error instead of the expected HTTP 409 Conflict.
5. **Observation 3**: `PatientRequestDto.birthDate` is annotated with `@Past`.
6. **Logic Step C**: In Java `jakarta.validation.constraints.Past`, today's date (`LocalDate.now()`) evaluates to `false`. Therefore, registering a newborn patient on their day of birth fails validation with HTTP 400 Bad Request.
7. **Observation 5 & 6**: `PatientControllerTest` has only 3 tests; `PatientServiceImplTest` has only 2 tests.
8. **Logic Step D**: The test suites do not test size boundaries (`<9` or `>20`), malformed JSON parsing, newborn birth date edge cases, or database integrity exceptions.

---

## Caveats

- Direct `mvn clean test` execution via `run_command` was not permitted due to user prompt timeout. All observations and conclusions are derived from exhaustive static code inspection, Java / Spring Boot specification analysis, and formal stress analysis of the codebase.
- No DB integration tests (e.g. `@DataJpaTest` or Testcontainers) exist in the project to test live database constraint behavior under concurrent load.

---

## Conclusion

The Milestone M3 REST API implementation in `omnicare-emr-api` correctly handles basic patient registration (HTTP 201), basic DTO validation (HTTP 400 for missing fields), and application-level duplicate detection (HTTP 409). However, **3 design/exception handling defects** and **test coverage gaps** were identified:

1. **Database Duplicate Race Condition**: Missing `@ExceptionHandler(DataIntegrityViolationException.class)` in `GlobalExceptionHandler` causes concurrent duplicate registration attempts to return HTTP 500 instead of HTTP 409.
2. **Malformed Request Handling**: Missing `@ExceptionHandler(HttpMessageNotReadableException.class)` causes unparseable JSON/dates to return HTTP 500 instead of HTTP 400.
3. **Newborn Validation Defect**: `@Past` on `birthDate` prevents registering newborn patients on their date of birth (should be `@PastOrPresent`).
4. **Test Coverage Gaps**: `PatientControllerTest` (3 tests) and `PatientServiceImplTest` (2 tests) lack boundary tests for string lengths, birth dates, malformed input, and exception paths.

---

## Verification Method

1. **Command to run tests**:
   ```bash
   mvn clean test -f omnicare-emr-api/pom.xml
   ```
2. **Files to inspect**:
   - `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/exception/GlobalExceptionHandler.java`
   - `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/main/java/com/omnicare/emr/dto/PatientRequestDto.java`
   - `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/test/java/com/omnicare/emr/controller/PatientControllerTest.java`
   - `c:/Users/nhan/Workspace/OmniCare-EMR/omnicare-emr-api/src/test/java/com/omnicare/emr/service/PatientServiceImplTest.java`
3. **Invalidation conditions**:
   - Adding `DataIntegrityViolationException` and `HttpMessageNotReadableException` handlers to `GlobalExceptionHandler`.
   - Changing `@Past` to `@PastOrPresent` on `PatientRequestDto.birthDate`.
   - Adding missing edge-case unit tests to `PatientControllerTest` and `PatientServiceImplTest`.

---

## Unchallenged Areas

- `OmnicareApiApplication.java` and `JpaConfig.java` — basic Spring Boot configuration out of review scope.
