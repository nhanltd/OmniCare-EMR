# E2E Test Infrastructure Specification — OmniCare EMR

**Document Version:** 1.0.0  
**Target System:** OmniCare EMR Backend API (`omnicare-emr-api`)  
**Scope:** Opaque-box End-to-End API Testing & Infrastructure Architecture  
**Author:** E2E Testing Explorer Instance 1  

---

## 1. Overview & Strategy

The OmniCare EMR End-to-End (E2E) testing infrastructure provides an **opaque-box (black-box)** verification suite. The test runner treats the Spring Boot Backend API as a black box, communicating exclusively via HTTP/HTTPS REST endpoints (and direct database verification queries strictly for soft-delete immutable storage auditing).

### Core Principles
1. **Opaque-box Integrity:** Tests interact via exposed REST endpoints (`/api/v1/*`) without internal JVM state access.
2. **Environment Reproducibility:** 1-Click execution powered by Docker Compose.
3. **Data Isolation:** Dynamic fixture generation (Faker / dynamic UUIDs) preventing cross-test data pollution.
4. **Contract Rigor:** Strict schema validation against HL7 FHIR inspired REST contracts and `GlobalExceptionHandler` error structures.

---

## 2. Infrastructure & Tech Stack Selection

### 2.1 Technology Stack Choice

| Component | Recommended Tool | Alternative Stack | Rationale |
| :--- | :--- | :--- | :--- |
| **Test Runner & Framework** | **Python Pytest + Requests** | Java REST-Assured + JUnit 5 | Rapid test writing, expressive fixtures, parameterization, fast test run cycles. |
| **Data Generation** | `Faker` (Python) | `java-faker` | Generates realistic Vietnamese CCCD identifiers, names, phone numbers, and dates. |
| **Schema Validation** | `jsonschema` | `json-schema-validator` | Validates OpenAPI 3.0 & FHIR response contracts automatically. |
| **Reporting** | Allure Framework | JUnit XML / HTML Reports | Interactive visualization of test results, suite execution time, and failure traces. |
| **Container Orchestration** | Docker & Docker Compose | Testcontainers | Matches production environment specification in `R1` & `R2`. |

---

## 3. Container Orchestration & Execution Lifecycle

### 3.1 Docker Compose Environment Topology

```
+-------------------------------------------------------------------------------+
|                             Docker Network (emr-net)                           |
|                                                                               |
|  +--------------------+      +--------------------+      +-----------------+  |
|  |     postgres       | <--- |  omnicare-emr-api  | <--- | e2e-test-runner |  |
|  | (PostgreSQL 15/16) |      | (Spring Boot 3.x)  |      | (Pytest Suite)  |  |
|  | Port: 5432         |      | Port: 8080         |      | Isolated Run    |  |
|  +--------------------+      +--------------------+      +-----------------+  |
+-------------------------------------------------------------------------------+
```

### 3.2 Docker Compose Configuration (`docker-compose.e2e.yml`)

```yaml
version: '3.8'

networks:
  emr-net:
    driver: bridge

services:
  postgres:
    image: postgres:15-alpine
    container_name: omnicare-db-test
    environment:
      POSTGRES_DB: omnicare_emr_test
      POSTGRES_USER: emr_user
      POSTGRES_PASSWORD: emr_password
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U emr_user -d omnicare_emr_test"]
      interval: 3s
      timeout: 3s
      retries: 10
    networks:
      - emr-net

  omnicare-emr-api:
    build:
      context: ./omnicare-emr-api
      dockerfile: Dockerfile
    container_name: omnicare-api-test
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/omnicare_emr_test
      SPRING_DATASOURCE_USERNAME: emr_user
      SPRING_DATASOURCE_PASSWORD: emr_password
      SPRING_JPA_HIBERNATE_DDL_AUTO: create-drop
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy
    healthcheck:
      test: ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"]
      interval: 5s
      timeout: 5s
      retries: 10
    networks:
      - emr-net

  e2e-test-runner:
    build:
      context: ./e2e-tests
      dockerfile: Dockerfile.e2e
    container_name: omnicare-e2e-runner
    environment:
      BASE_URL: http://omnicare-emr-api:8080
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: omnicare_emr_test
      DB_USER: emr_user
      DB_PASS: emr_password
    depends_on:
      omnicare-emr-api:
        condition: service_healthy
    networks:
      - emr-net
```

---

## 4. Test Data Management & Fixture Architecture

### 4.1 Data Isolation Principles
* **Dynamic Generation:** Tests must never hardcode static patient CCCDs or primary keys across test runs.
* **Vietnam Domain Specifics:**
  * `identifier` (CCCD): 12-digit string format (e.g., `079004123456`).
  * `phone_number` / `telecom`: Vietnam mobile format (`+84` prefix or `0` prefix, 10 digits).
  * `birth_date`: ISO-8601 `YYYY-MM-DD`.

### 4.2 Pytest Fixture Structure (`conftest.py` Blueprint)

```python
import pytest
import requests
import os
import uuid
from faker import Faker

fake = Faker(['vi_VN'])
BASE_URL = os.getenv("BASE_URL", "http://localhost:8080")

@pytest.fixture(scope="session")
def api_client():
    session = requests.Session()
    session.headers.update({
        "Content-Type": "application/json",
        "Accept": "application/json"
    })
    return session

@pytest.fixture
def valid_patient_payload():
    return {
        "resourceType": "Patient",
        "identifier": f"0{fake.numeric_dict_str(length=11)}" if hasattr(fake, 'numeric_dict_str') else f"079{fake.random_number(digits=9, fix_len=True)}",
        "name": {
            "family": fake.last_name(),
            "given": fake.first_name()
        },
        "gender": fake.random_element(elements=("male", "female", "other")),
        "birthDate": fake.date_of_birth(minimum_age=1, maximum_age=90).strftime("%Y-%m-%d"),
        "telecom": f"+849{fake.random_number(digits=8, fix_len=True)}"
    }
```

---

## 5. Contract & Error Schema Standards

### 5.1 Success Contract (HTTP 201 Created)
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["id", "status", "message"],
  "properties": {
    "id": {
      "type": "string",
      "format": "uuid"
    },
    "status": {
      "type": "string",
      "enum": ["success", "created", "201"]
    },
    "message": { "type": "string" }
  }
}
```

### 5.2 Error Response Contract (`GlobalExceptionHandler`)
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "type": "object",
  "required": ["timestamp", "status", "error", "message"],
  "properties": {
    "timestamp": { "type": "string" },
    "status": { "type": "integer" },
    "error": { "type": "string" },
    "message": { "type": "string" },
    "errors": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "field": { "type": "string" },
          "message": { "type": "string" }
        }
      }
    },
    "path": { "type": "string" }
  }
}
```

---

## 6. Execution & CI/CD Pipeline Integration

### 6.1 Local One-Command Execution Script (`run_e2e_tests.sh` / `run_e2e_tests.ps1`)
```powershell
# PowerShell E2E Execution Workflow
docker-compose -f docker-compose.e2e.yml down -v
docker-compose -f docker-compose.e2e.yml up --build --exit-code-from e2e-test-runner
```

### 6.2 CI/CD Integration Steps (GitHub Actions)
1. **Build Step:** Compile Spring Boot application and create Docker image.
2. **Spin-up Step:** Run `docker-compose -f docker-compose.e2e.yml up -d postgres omnicare-emr-api`.
3. **Wait-for-Health:** Wait for `/actuator/health` to return `200 OK`.
4. **Test Run:** Run `pytest --alluredir=./allure-results`.
5. **Report Generation:** Upload Allure report artifact upon test completion.
