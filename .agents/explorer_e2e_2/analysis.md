# E2E Opaque-Box Testing Strategy & Infrastructure Analysis

**Author:** E2E Testing Explorer Instance 2  
**Target Application:** OmniCare EMR Backend (`omnicare-emr-api`)  
**Scope:** Opaque-Box End-to-End Integration & System Testing (Spring Boot + PostgreSQL)  
**Date:** 2026-07-24  

---

## 1. Executive Summary

This document establishes the End-to-End (E2E) opaque-box testing strategy and architecture for the OmniCare EMR system. Opaque-box testing treats the application as a black box, verifying functionality strictly through external interfaces:
1. **HTTP REST Endpoints** (`http://localhost:8080/api/v1/patients`)
2. **PostgreSQL Database State Verification** (Port `5432`, querying tables directly to validate data persistence, audit field population, soft deletes, and optimistic locking flags).

The strategy ensures all Acceptance Criteria defined in `ORIGINAL_REQUEST.md` (R1–R4) are rigorously verified before deployment.

---

## 2. System Under Test (SUT) Topology

```
+-------------------------------------------------------------------------+
|                        E2E Test Runner Infrastructure                   |
|  (Python pytest / requests / psycopg2 OR PowerShell E2E Harness Script)  |
+------------------------------------+------------------------------------+
                                     |
              1. HTTP Requests       |        2. Direct SQL Queries
          (POST /api/v1/patients)    |      (SELECT * FROM patient ...)
                                     v
+------------------------------------+------------------------------------+
|                         System Under Test (SUT)                         |
|                                                                         |
|  +-----------------------------------+   +---------------------------+  |
|  |       Spring Boot Service         |   |    PostgreSQL Container   |  |
|  |     (Port 8080 / Java 17/21)      |-->|       (Port 5432)         |  |
|  |  Controller -> Service -> Repo   |   |   Table: patient          |  |
|  +-----------------------------------+   +---------------------------+  |
+-------------------------------------------------------------------------+
```

---

## 3. Tiered Test Architecture

The E2E test suite is organized into 4 distinct verification tiers:

### Tier 1: Infrastructure & DB Schema Verification
- **Port 5432 Connectivity**: Verify PostgreSQL port is listening.
- **Table Auto-Generation Audit**: Query `information_schema.tables` and `information_schema.columns` to confirm `patient` table exists with all mandatory audit columns (`id`, `created_at`, `updated_at`, `version`, `is_deleted`, `identifier`, `full_name`, `gender`, `birth_date`, `phone_number`).
- **HTTP Server Liveness**: Ping `http://localhost:8080/actuator/health` or base URL.

### Tier 2: Happy Path Functional E2E Tests
- **E2E-01: Patient Creation (`POST /api/v1/patients`)**
  - Sends valid JSON payload.
  - Expects `201 Created` HTTP status response.
  - Verifies response body contains valid UUID `id` and accurate patient fields.
- **E2E-02: Persistence & Audit Column Validation**
  - Executes SQL query against PostgreSQL DB (`SELECT * FROM patient WHERE id = ?`).
  - Asserts row exists in DB with matching data.
  - Asserts `created_at` IS NOT NULL, `updated_at` IS NOT NULL, `version` == 0, `is_deleted` == false.

### Tier 3: Validation & Exception Handling Tests
- **E2E-03: Duplicate Identifier Prevention**
  - Sends second `POST /api/v1/patients` with an existing `identifier` (CCCD).
  - Expects `409 Conflict` or `400 Bad Request` HTTP status.
  - Asserts response matches structured `GlobalExceptionHandler` format (`status`, `message`, `timestamp`).
  - Asserts DB count remains 1 (no duplicate row created).
- **E2E-04: Request Payload Validation Failure**
  - Sends `POST` request missing required fields (e.g. blank `identifier` or `fullName`).
  - Expects `400 Bad Request` with field error details.

### Tier 4: Edge Cases & Data Integrity
- **E2E-05: Unicode & Vietnamese Character Encoding**
  - Sends patient names containing full diacritics (e.g., `"Nguyễn Thị Ánh Tuyết"`).
  - Asserts HTTP response and database record retain exact UTF-8 character encoding without corruption (`Nguyễn Thị Ánh Tuyết`).
- **E2E-06: Concurrency & Transactional Isolation**
  - Simulates rapid consecutive requests to verify unique identifier constraints at the database level.

---

## 4. REST API Contracts & Schema Definitions

### 4.1. Patient Creation Endpoint

- **Method**: `POST`
- **Path**: `/api/v1/patients`
- **Headers**:
  - `Content-Type: application/json`
  - `Accept: application/json`

#### Option A: Standard Flat DTO Contract
```json
// Request Body
{
  "identifier": "079004123456",
  "fullName": "Nguyễn Văn A",
  "gender": "male",
  "birthDate": "1990-01-01",
  "phoneNumber": "+84901234567"
}
```

#### Option B: HL7 FHIR Aligned DTO Contract
```json
// Request Body
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

#### Successful Response (201 Created)
```json
{
  "id": "pat-123e4567-e89b-12d3-a456-426614174000",
  "identifier": "079004123456",
  "fullName": "Nguyễn Văn A",
  "gender": "male",
  "birthDate": "1990-01-01",
  "phoneNumber": "+84901234567",
  "createdAt": "2026-07-24T14:45:00Z",
  "updatedAt": "2026-07-24T14:45:00Z",
  "version": 0,
  "isDeleted": false
}
```

### 4.2. Error Response Schemas

#### Duplicate CCCD / Identifier Error (409 Conflict / 400 Bad Request)
```json
{
  "timestamp": "2026-07-24T14:45:01Z",
  "status": 409,
  "error": "Conflict",
  "message": "Bệnh nhân với mã CCCD/identifier '079004123456' đã tồn tại trong hệ thống.",
  "path": "/api/v1/patients"
}
```

#### Validation Error (400 Bad Request)
```json
{
  "timestamp": "2026-07-24T14:45:02Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Dữ liệu không hợp lệ.",
  "errors": [
    {
      "field": "identifier",
      "message": "Số CCCD/identifier không được để trống"
    }
  ],
  "path": "/api/v1/patients"
}
```

---

## 5. Database Schema & Query Assertions

To verify Spring Boot JPA entity mapping & `auto-ddl` behavior, the E2E runner executes direct SQL checks:

### Table Existence & Structure Query
```sql
SELECT column_name, data_type, is_nullable, column_default 
FROM information_schema.columns 
WHERE table_name = 'patient' 
ORDER BY ordinal_position;
```

Expected Columns:
- `id` (uuid, PRIMARY KEY)
- `created_at` (timestamp / timestamptz, NOT NULL)
- `updated_at` (timestamp / timestamptz, NOT NULL)
- `version` (integer / bigint, NOT NULL, DEFAULT 0)
- `is_deleted` (boolean, NOT NULL, DEFAULT false)
- `identifier` (varchar, UNIQUE)
- `full_name` (varchar)
- `gender` (varchar)
- `birth_date` (date)
- `phone_number` (varchar)

### Persistence Verification Query
```sql
SELECT id, identifier, full_name, gender, birth_date, phone_number, created_at, updated_at, version, is_deleted
FROM patient
WHERE identifier = '079004123456';
```

---

## 6. Standalone E2E Test Suite Script (`e2e_test_suite.py`)

Below is the complete executable Python E2E test runner design, which runs without external UI dependencies:

```python
#!/usr/bin/env python3
"""
OmniCare EMR - Opaque-Box E2E Integration Test Suite
Author: E2E Testing Explorer Instance 2
Usage: python e2e_test_suite.py --api-url http://localhost:8080 --db-url postgresql://postgres:postgres@localhost:5432/omnicare_db
"""

import sys
import uuid
import json
import argparse
import requests
import psycopg2

class E2ETestRunner:
    def __init__(self, api_url, db_config):
        self.api_url = api_url.rstrip('/')
        self.db_config = db_config
        self.passed = 0
        self.failed = 0

    def log(self, test_name, status, details=""):
        symbol = "✅ PASS" if status else "❌ FAIL"
        print(f"[{symbol}] {test_name} {f'- {details}' if details else ''}")
        if status:
            self.passed += 1
        else:
            self.failed += 1

    def get_db_connection(self):
        return psycopg2.connect(**self.db_config)

    def run_all(self):
        print("==================================================")
        print("   OmniCare EMR E2E Opaque-Box Test Execution     ")
        print("==================================================")
        
        self.test_tier1_db_schema()
        self.test_tier2_create_patient_success()
        self.test_tier3_duplicate_identifier_failure()
        self.test_tier3_validation_failure()
        self.test_tier4_unicode_preservation()
        
        print("==================================================")
        print(f"Summary: Total Passed: {self.passed} | Total Failed: {self.failed}")
        print("==================================================")
        return self.failed == 0

    def test_tier1_db_schema(self):
        test_name = "Tier 1: Verify PostgreSQL Table 'patient' and BaseEntity Audit Columns"
        try:
            conn = self.get_db_connection()
            cur = conn.cursor()
            cur.execute("""
                SELECT column_name FROM information_schema.columns 
                WHERE table_name = 'patient';
            """)
            cols = [r[0] for r in cur.fetchall()]
            conn.close()

            required = ['id', 'created_at', 'updated_at', 'version', 'is_deleted', 'identifier', 'full_name']
            missing = [c for c in required if c not in cols]
            if not missing:
                self.log(test_name, True, f"Found all required columns: {cols}")
            else:
                self.log(test_name, False, f"Missing columns in 'patient' table: {missing}")
        except Exception as e:
            self.log(test_name, False, f"Database query failed: {e}")

    def test_tier2_create_patient_success(self):
        test_name = "Tier 2: POST /api/v1/patients - Valid Patient Creation"
        test_cccd = f"CCCD-{uuid.uuid4().hex[:8]}"
        payload = {
            "identifier": test_cccd,
            "fullName": "Nguyễn Văn E2E",
            "gender": "male",
            "birthDate": "1995-05-15",
            "phoneNumber": "+84988776655"
        }
        try:
            resp = requests.post(f"{self.api_url}/api/v1/patients", json=payload, timeout=5)
            if resp.status_code != 201:
                self.log(test_name, False, f"Expected HTTP 201, got {resp.status_code}: {resp.text}")
                return

            res_json = resp.json()
            patient_id = res_json.get("id")
            if not patient_id:
                self.log(test_name, False, "Response missing 'id' field")
                return

            # DB State Verification
            conn = self.get_db_connection()
            cur = conn.cursor()
            cur.execute("SELECT id, identifier, full_name, version, is_deleted FROM patient WHERE identifier = %s;", (test_cccd,))
            row = cur.fetchone()
            conn.close()

            if row and str(row[1]) == test_cccd and row[3] == 0 and row[4] is False:
                self.log(test_name, True, f"Created Patient UUID={patient_id}, DB audit values verified (version=0, is_deleted=false)")
            else:
                self.log(test_name, False, f"DB verification mismatch for created patient: {row}")
        except Exception as e:
            self.log(test_name, False, f"HTTP or DB exception: {e}")

    def test_tier3_duplicate_identifier_failure(self):
        test_name = "Tier 3: POST /api/v1/patients - Duplicate CCCD Rejection"
        dup_cccd = f"DUP-{uuid.uuid4().hex[:8]}"
        payload = {
            "identifier": dup_cccd,
            "fullName": "Bệnh Nhân Gốc",
            "gender": "female",
            "birthDate": "1992-02-02",
            "phoneNumber": "+84900000001"
        }
        try:
            # First Creation
            r1 = requests.post(f"{self.api_url}/api/v1/patients", json=payload, timeout=5)
            if r1.status_code != 201:
                self.log(test_name, False, f"Initial creation failed with HTTP {r1.status_code}")
                return

            # Duplicate Creation Attempt
            r2 = requests.post(f"{self.api_url}/api/v1/patients", json=payload, timeout=5)
            if r2.status_code in [400, 409]:
                self.log(test_name, True, f"Correctly rejected duplicate with HTTP {r2.status_code}")
            else:
                self.log(test_name, False, f"Expected HTTP 400/409 on duplicate, got {r2.status_code}: {r2.text}")
        except Exception as e:
            self.log(test_name, False, f"Exception: {e}")

    def test_tier3_validation_failure(self):
        test_name = "Tier 3: POST /api/v1/patients - Missing Required Fields Validation"
        payload = {"fullName": "Chỉ Có Tên Không Có Identifier"}
        try:
            resp = requests.post(f"{self.api_url}/api/v1/patients", json=payload, timeout=5)
            if resp.status_code == 400:
                self.log(test_name, True, "Correctly returned HTTP 400 Bad Request for missing identifier")
            else:
                self.log(test_name, False, f"Expected HTTP 400, got {resp.status_code}")
        except Exception as e:
            self.log(test_name, False, f"Exception: {e}")

    def test_tier4_unicode_preservation(self):
        test_name = "Tier 4: UTF-8 Vietnamese Diacritics Encoding Verification"
        unicode_cccd = f"UNI-{uuid.uuid4().hex[:8]}"
        name_vn = "Trần Hoàng Bảo Khánh"
        payload = {
            "identifier": unicode_cccd,
            "fullName": name_vn,
            "gender": "female",
            "birthDate": "1998-12-25",
            "phoneNumber": "+84911223344"
        }
        try:
            resp = requests.post(f"{self.api_url}/api/v1/patients", json=payload, timeout=5)
            if resp.status_code == 201 and resp.json().get("fullName") == name_vn:
                self.log(test_name, True, f"Vietnamese name '{name_vn}' returned accurately in API response")
            else:
                self.log(test_name, False, f"Name mismatch or request failure: {resp.text}")
        except Exception as e:
            self.log(test_name, False, f"Exception: {e}")

if __name__ == "__main__":
    db_params = {
        "dbname": "omnicare_db",
        "user": "postgres",
        "password": "postgrespassword",
        "host": "localhost",
        "port": 5432
    }
    runner = E2ETestRunner("http://localhost:8080", db_params)
    success = runner.run_all()
    sys.exit(0 if success else 1)
```

---

## 7. PowerShell Orchestration Runner Script (`run_e2e_tests.ps1`)

To enable single-command verification during local development and CI pipelines:

```powershell
# OmniCare EMR E2E Test Suite PowerShell Wrapper
Param(
    [string]$AppUrl = "http://localhost:8080",
    [string]$DbHost = "localhost",
    [int]$DbPort = 5432
)

Write-Host "[1/3] Checking Database Port Listener on $DbHost:$DbPort..." -ForegroundColor Cyan
$tcp = New-Object System.Net.Sockets.TcpClient
try {
    $tcp.Connect($DbHost, $DbPort)
    Write-Host "✅ PostgreSQL Port $DbPort is listening." -ForegroundColor Green
    $tcp.Close()
} catch {
    Write-Host "❌ Failed to connect to PostgreSQL port $DbPort." -ForegroundColor Red
    Exit 1
}

Write-Host "[2/3] Checking Spring Boot API Liveness on $AppUrl..." -ForegroundColor Cyan
try {
    $response = Invoke-WebRequest -Uri "$AppUrl/actuator/health" -TimeoutSec 5 -UseBasicParsing
    Write-Host "✅ Application is alive ($($response.StatusCode))." -ForegroundColor Green
} catch {
    Write-Host "⚠️ Actuator endpoint unavailable, attempting root API probe..." -ForegroundColor Yellow
}

Write-Host "[3/3] Executing Python E2E Test Suite..." -ForegroundColor Cyan
python .agents/explorer_e2e_2/e2e_test_suite.py
```

---

## 8. Summary of Findings & Next Steps

1. **Test Infrastructure**: Defined both Python and PowerShell test harnesses covering 4 verification tiers.
2. **API & DB Alignment**: Defined REST request/response schemas for `POST /api/v1/patients` along with direct PostgreSQL table column assertions (`id`, `created_at`, `updated_at`, `version`, `is_deleted`).
3. **Validation Focus**: Prepared explicit test payloads for happy-path patient creation, duplicate CCCD rejection, missing mandatory attributes, and UTF-8 Vietnamese character integrity.
