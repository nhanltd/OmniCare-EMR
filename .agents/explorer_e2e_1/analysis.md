# E2E Opaque-Box Test Strategy & Design Document — OmniCare EMR

**Document Status:** Complete Design Specification  
**System Under Test (SUT):** OmniCare EMR Backend API (`omnicare-emr-api`)  
**Working Directory:** `c:/Users/nhan/Workspace/OmniCare-EMR/.agents/explorer_e2e_1`  
**Author:** E2E Testing Explorer Instance 1  

---

## 1. Executive Summary & Test Design Philosophy

This document details the complete Opaque-Box E2E Test Strategy for the OmniCare Electronic Medical Record (EMR) system. The testing suite treats the backend application as a black box exposed via RESTful HTTP APIs backed by a PostgreSQL database container.

### Testing Principles
1. **Opaque-Box Evaluation:** All operations are initiated through standard HTTP requests against exposed endpoints (`POST`, `GET`, `PUT`, `DELETE`).
2. **Tiered Coverage:** Test suites are partitioned into four distinct tiers ranging from basic happy path execution to multi-step concurrent real-world workflows.
3. **Medical Record Integrity (WORM Principle):** Data soft delete (`isDeleted`), optimistic locking (`version`), and audit fields (`createdAt`, `updatedAt`) are thoroughly validated via API responses and direct DB state assertions.
4. **Deterministic Assertions:** Every test case specifies exact HTTP status codes, JSON schema structures, header assertions, and DB state validations.

---

## 2. Requirements & Acceptance Criteria Traceability Matrix

| Requirement | Description | E2E Test Suite Alignment |
| :--- | :--- | :--- |
| **R1** | PostgreSQL container execution | Infrastructure smoke tests & DB connection readiness checks |
| **R2** | Spring Boot initialization & layered architecture | API routing, GlobalExceptionHandler format checks |
| **R3** | `BaseEntity` fields & `Patient` entity | Tier 3 (Audit fields, versioning, soft delete) |
| **R4** | `POST /api/v1/patients`, duplicate CCCD check | Tier 1 (Happy path) & Tier 2 (Duplicate 409 conflict) |
| **Acceptance Criteria 1** | `docker-compose up -d` starts PostgreSQL | Infrastructure setup automation |
| **Acceptance Criteria 2** | Spring Boot startup without errors | Healthcheck `/actuator/health` returns `200 OK` |
| **Acceptance Criteria 3** | Auto-generation of `patient` table | Schema existence check upon test boot |
| **Acceptance Criteria 4** | `POST /api/v1/patients` returns `201 Created` with UUID | Tier 1 Suite (`TC_E2E_101`) |
| **Acceptance Criteria 5** | Duplicate CCCD returns error via GlobalExceptionHandler | Tier 2 Suite (`TC_E2E_207`) |
| **Acceptance Criteria 6** | `patient` table tracks `createdAt`, `updatedAt`, `version`, `isDeleted` | Tier 3 Suite (`TC_E2E_301` - `TC_E2E_305`) |

---

## 3. Comprehensive E2E Test Suite Design

### Tier 1: Feature Coverage (Happy Path Verification)

#### TC_E2E_101: Create Patient with Standard HL7 FHIR Payload
* **Objective:** Verify successful creation of a new patient record with a valid JSON payload.
* **HTTP Method & Endpoint:** `POST /api/v1/patients`
* **Request Headers:**
  ```http
  Content-Type: application/json
  Accept: application/json
  ```
* **Request Payload:**
  ```json
  {
    "resourceType": "Patient",
    "identifier": "079004123456",
    "name": {
      "family": "Nguyễn",
      "given": "Văn A"
    },
    "gender": "male",
    "birthDate": "1990-01-01",
    "telecom": "+84901234567"
  }
  ```
* **Expected Response Status:** `201 Created`
* **Expected Response Headers:**
  * `Location`: `/api/v1/patients/{uuid}` (Matching generated UUID format)
  * `Content-Type`: `application/json`
* **Expected Response Body:**
  ```json
  {
    "id": "pat-123e4567-e89b-12d3-a456-426614174000",
    "status": "success",
    "message": "Hồ sơ bệnh nhân được tạo thành công."
  }
  ```
* **Post-Condition Assertion (Opaque Verification):**
  * Execute `GET /api/v1/patients/{id}` using returned `id`.
  * Assert HTTP `200 OK`.
  * Assert response payload matches stored values: `identifier == "079004123456"`, `fullName == "Nguyễn Văn A"`, `gender == "male"`, `birthDate == "1990-01-01"`, `phoneNumber == "+84901234567"`.

#### TC_E2E_102: Patient Registration with Flattened DTO Format
* **Objective:** Verify API supports standard DTO fields (`identifier`, `fullName`, `gender`, `birthDate`, `phoneNumber`).
* **Request Payload:**
  ```json
  {
    "identifier": "079004987654",
    "fullName": "Trần Thị B",
    "gender": "female",
    "birthDate": "1995-05-15",
    "phoneNumber": "0912345678"
  }
  ```
* **Expected Response Status:** `201 Created`
* **Assertion:** Generated UUID is non-null, valid RFC 4122 UUID v4.

---

### Tier 2: Boundary & Corner Cases (Validation & Error Handling)

#### Suite 2.1: Field Validation Errors (HTTP 400 Bad Request)

| Test Case ID | Target Field | Input Condition / Payload | Expected Status | Error Message / Field Validation |
| :--- | :--- | :--- | :--- | :--- |
| **TC_E2E_201** | `identifier` | Missing field (`null` or omitted) | `400 Bad Request` | `errors[0].field == "identifier"`, `message` indicates mandatory field. |
| **TC_E2E_202** | `identifier` | Empty string `""` or whitespace `"   "` | `400 Bad Request` | Validation rule `@NotBlank` triggered. |
| **TC_E2E_203** | `identifier` | String exceeding length (`> 20` characters, e.g. 25 chars) | `400 Bad Request` | Validation rule `@Size(max=20)` triggered. |
| **TC_E2E_204** | `name`/`fullName` | Missing name structure / blank full name | `400 Bad Request` | Field error on `fullName` or `name`. |
| **TC_E2E_205** | `gender` | Invalid enum value (e.g., `"unknown_gender"`) | `400 Bad Request` | Pattern / Enum validation failure. |
| **TC_E2E_206** | `birthDate` | Malformed date string (e.g., `"01/01/1990"` or future `"2099-01-01"`) | `400 Bad Request` | Invalid date format / `@PastOrPresent` validation error. |

#### Suite 2.2: Duplicate CCCD Conflict (HTTP 409 Conflict)

#### TC_E2E_207: Duplicate CCCD Identifier Registration
1. **Step 1:** Send `POST /api/v1/patients` with `identifier: "079004111222"`. Expect `201 Created`.
2. **Step 2:** Send `POST /api/v1/patients` with identical `identifier: "079004111222"` but different name/phone.
3. **Expected Status:** `409 Conflict` (or `400 Bad Request` depending on exception handling specification).
4. **Expected Response Body (GlobalExceptionHandler):**
   ```json
   {
     "timestamp": "2026-07-24T14:43:20Z",
     "status": 409,
     "error": "Conflict",
     "message": "Bệnh nhân với mã CCCD 079004111222 đã tồn tại trong hệ thống.",
     "path": "/api/v1/patients"
   }
   ```

#### Suite 2.3: HTTP Protocol & Payload Corner Cases

| Test Case ID | Scenario | Input Condition | Expected Status |
| :--- | :--- | :--- | :--- |
| **TC_E2E_208** | Malformed JSON | Request body with trailing comma or unclosed brace `{"identifier": "079"...` | `400 Bad Request` |
| **TC_E2E_209** | Unsupported Content-Type | Sending payload with header `Content-Type: text/plain` | `415 Unsupported Media Type` |
| **TC_E2E_210** | Method Not Allowed | Issuing `PUT /api/v1/patients` without trailing `{id}` | `405 Method Not Allowed` |
| **TC_E2E_211** | Non-Existent Entity Lookup | `GET /api/v1/patients/00000000-0000-0000-0000-000000000000` | `404 Not Found` |

---

### Tier 3: Entity Lifecycle & Soft Delete Verification

#### TC_E2E_301: BaseEntity Mandatory Audit Fields Initialization
* **Objective:** Ensure initial `POST /api/v1/patients` assigns correct baseline values to administrative audit fields.
* **Opaque API / Database Checks:**
  1. `createdAt`: Present, valid ISO-8601 timestamp, non-null, set within +/- 5 seconds of test execution.
  2. `updatedAt`: Present, non-null, equal to `createdAt`.
  3. `version`: Initialized to `0` (or `1`).
  4. `isDeleted`: Boolean `false`.

#### TC_E2E_302: Entity Mutation & Timestamp Update
* **Objective:** Verify `PUT /api/v1/patients/{id}` updates `updatedAt` and increments `version` while preserving `createdAt`.
* **Execution Flow:**
  1. Register patient via `POST /api/v1/patients` -> Record `id`, `createdAt`, `version` (V0).
  2. Wait 1 second.
  3. Send `PUT /api/v1/patients/{id}` with updated `telecom: "+84999888777"`.
  4. Assert Response `200 OK`.
  5. Fetch record `GET /api/v1/patients/{id}`:
     * `createdAt` == Original `createdAt` (Unchanged).
     * `updatedAt` > Original `createdAt`.
     * `version` == `V0 + 1` (Incremented).

#### TC_E2E_303: Optimistic Locking Concurrency Conflict
* **Objective:** Verify JPA Optimistic Locking prevents lost updates when stale version is provided.
* **Execution Flow:**
  1. Fetch patient state -> `version = 1`.
  2. Simulate Client A and Client B obtaining `version = 1`.
  3. Client A executes update -> Version incremented to `2` in DB.
  4. Client B attempts update sending header `If-Match: 1` or body `version: 1`.
  5. **Expected Status:** `409 Conflict` (OptimisticLockException caught by GlobalExceptionHandler).

#### TC_E2E_304: Soft Delete Verification (WORM Compliance)
* **Objective:** Verify `DELETE /api/v1/patients/{id}` performs soft delete instead of SQL physical deletion.
* **Execution Flow:**
  1. Register patient -> Obtain `{id}`.
  2. Issue `DELETE /api/v1/patients/{id}`.
  3. **Expected API Status:** `200 OK` or `204 No Content`.
  4. **Standard GET Lookup:** `GET /api/v1/patients/{id}` returns `404 Not Found`.
  5. **Direct DB Verification (SQL Query):**
     ```sql
     SELECT id, identifier, is_deleted, created_at, updated_at FROM patient WHERE id = '{id}';
     ```
  6. **Assertion:** Row exists in PostgreSQL database table `patient`, with `is_deleted = true`.

#### TC_E2E_305: Duplicate CCCD Attempt Post Soft Delete
* **Objective:** Test business logic behavior when registering a new patient with CCCD of a soft-deleted record.
* **Execution Flow:**
  1. Soft-delete patient with `identifier: "079004999999"`.
  2. Send `POST /api/v1/patients` with `identifier: "079004999999"`.
  3. Assert defined domain policy: Either reactivates soft-deleted record or returns `409 Conflict` preserving immutable history.

---

### Tier 4: Real-World Scenario Testing

#### Scenario 4.1: Reception & Medical Triage Workflow
```
[Reception Desk]                    [EMR API]                    [Database]
       |                                |                            |
       |--- 1. GET /patients?id=... --->|                            |
       |<-- 2. 404 Not Found -----------|                            |
       |                                |                            |
       |--- 3. POST /patients --------->|                            |
       |    (Register New Patient)      |--- 4. INSERT INTO patient->|
       |<-- 5. 201 Created (UUID) ------|                            |
       |                                |                            |
       |--- 6. POST /encounters --------|                            |
       |    (Create Session PLANNED)    |--- 7. INSERT encounter --->|
       |<-- 8. 201 Created -------------|                            |
```
* **Step 1:** Reception checks if patient exists: `GET /api/v1/patients?identifier=079004555666`. Response `404 Not Found`.
* **Step 2:** Create patient record via `POST /api/v1/patients`. Response `201 Created` returning UUID `pat-uuid-001`.
* **Step 3:** Immediately query `GET /api/v1/patients?identifier=079004555666`. Response `200 OK` returning patient array containing `pat-uuid-001`.
* **Step 4:** Initiate reception encounter: `POST /api/v1/encounters` with `{ "patientId": "pat-uuid-001", "status": "PLANNED" }`. Response `201 Created`.

#### Scenario 4.2: High Concurrency Duplicate Registration Race Condition
* **Objective:** Verify database unique constraint & Spring transaction lock under simultaneous creation requests.
* **Execution Setup:**
  * Use `asyncio` or multi-threaded HTTP client to dispatch 10 parallel `POST /api/v1/patients` requests with identical `identifier: "079004888888"` at the exact same millisecond.
* **Expected Outcome:**
  * Exactly **1 request** receives `201 Created`.
  * The remaining **9 requests** receive `409 Conflict`.
  * PostgreSQL `patient` table contains exactly **1 row** for `079004888888`.

#### Scenario 4.3: Bulk Registration & Pagination Stress Verification
* **Objective:** Verify dataset pagination behavior.
* **Execution Setup:**
  * Seed database with 25 distinct patient records via `POST`.
  * Request `GET /api/v1/patients?page=0&size=10`.
* **Expected Response Assertions:**
  ```json
  {
    "content": [ /* 10 patient objects */ ],
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3,
    "last": false
  }
  ```
  * Request `GET /api/v1/patients?page=2&size=10` -> Returns remaining 5 patient objects, `last: true`.

#### Scenario 4.4: System Audit Trail Generation
* **Objective:** Verify compliance logging for administrative operations.
* **Execution Setup:**
  * Create patient record via API.
  * Update patient telephone number.
  * Soft-delete patient record.
* **Verification (Direct DB / Audit Endpoint Query):**
  * Assert `audit_log` table contains 3 records corresponding to `CREATE`, `UPDATE`, `DELETE` with matching `entity_name = 'Patient'`, correct `entity_id`, and valid timestamps.

---

## 4. Test Execution Matrix Summary

| Tier | Suite Name | Test Cases | Target Status Codes | Primary Assertion Focus |
| :--- | :--- | :--- | :--- | :--- |
| **Tier 1** | Feature Coverage | `TC_E2E_101`, `TC_E2E_102` | `201 Created`, `200 OK` | Valid UUID, header Location, JSON schema matching |
| **Tier 2** | Boundary & Validation | `TC_E2E_201` - `TC_E2E_211` | `400 Bad Request`, `409 Conflict`, `405`, `415` | Field error messages, duplicate CCCD handling |
| **Tier 3** | Lifecycle & Soft Delete | `TC_E2E_301` - `TC_E2E_305` | `200 OK`, `204`, `404 Not Found` | `createdAt`, `updatedAt`, `version`, `is_deleted = true` in DB |
| **Tier 4** | Real-World Workflows | `Scenario 4.1` - `Scenario 4.4` | `201`, `409`, `200` | Concurrency safety, pagination integrity, audit logging |

---

## 5. Conclusion & Recommendations for Implementation

1. **Immediate Execution Readiness:** The test suite structure defined above can be executed directly using pytest or REST-assured once `omnicare-emr-api` exposes port 8080.
2. **GlobalExceptionHandler Standardization:** Ensure Spring Boot application maps `DataIntegrityViolationException` and custom `PatientAlreadyExistsException` to HTTP 409 Conflict with structured JSON error fields.
3. **Database Assertion Scripting:** Incorporate direct SQL verification helpers to validate `is_deleted` column state without relying purely on opaque HTTP endpoints.
