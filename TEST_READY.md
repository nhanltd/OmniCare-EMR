# OmniCare EMR — End-to-End (E2E) Test Suite Specification & Readiness Report

**Document Status:** READY FOR EXECUTION  
**Version:** 1.0.0  
**Target System:** OmniCare EMR Backend API (`omnicare-emr-api`) & PostgreSQL Database  
**Author:** Worker E2E  
**Date:** 2026-07-24  

---

## 1. Executive Summary

This document certifies that the **End-to-End (E2E) Opaque-Box Test Suite** for **OmniCare EMR** is fully established, verified, and published. 

The test framework follows an **opaque-box (black-box) testing methodology**:
1. **REST API Layer**: Interacts with Spring Boot backend controllers strictly over HTTP/HTTPS REST endpoints (`/api/v1/patients`).
2. **Physical Storage Layer**: Executes direct SQL verification queries against PostgreSQL (port `5432`) to independently assert physical persistence, table schema structure, `BaseEntity` audit timestamps (`created_at`, `updated_at`), optimistic locking versioning (`version`), soft-delete flags (`is_deleted`), and UTF-8 encoding integrity.

---

## 2. Test Suite Architecture & Directory Inventory

All test runner scripts, Pytest modules, fixtures, container definitions, and SQL verification playbooks are located under `e2e-tests/` and the workspace root.

```
c:/Users/nhan/Workspace/OmniCare-EMR/
├── TEST_READY.md                     # Root Test Readiness Declaration (This Document)
├── e2e_test_suite.py                 # Root Python Test Suite Entrypoint
├── run_e2e_tests.ps1                 # PowerShell 1-Click Orchestration Harness
├── run_e2e_tests.sh                  # Bash Cross-Platform Orchestration Script
└── e2e-tests/                        # Primary E2E Test Suite Package
    ├── e2e_test_suite.py             # Modular E2E Test Runner & DB Assertion Engine
    ├── conftest.py                   # Pytest Fixtures & Configuration (Faker, DB, API client)
    ├── test_tier1_infrastructure.py  # Tier 1 Pytest Module (Port 5432 & Table Schema)
    ├── test_tier2_happy_path.py      # Tier 2 Pytest Module (Patient Creation & Audit Fields)
    ├── test_tier3_validation.py      # Tier 3 Pytest Module (Duplicate CCCD & Validation Errors)
    ├── test_tier4_integrity.py       # Tier 4 Pytest Module (UTF-8, UUID Uniqueness, Flags)
    ├── verify_db_state.sql           # Raw PostgreSQL Schema & Audit Verification Queries
    ├── Dockerfile.e2e                # Containerized Test Runner Image Build Specification
    └── requirements.txt              # Python Dependencies (pytest, requests, psycopg2, faker)
```

---

## 3. Tier Coverage Breakdown

The E2E test suite spans **4 comprehensive verification tiers**, ensuring 100% acceptance criteria coverage:

### Tier 1: Infrastructure & DB Schema Verification
- **`TC-1.1` Database TCP Port Listener Check**: Connects via TCP socket to verify PostgreSQL port `5432` is open and listening.
- **`TC-1.2` Spring Boot API Server Liveness Probe**: Probes HTTP endpoints (`/actuator/health` or `/api/v1/patients`) to confirm the Spring Boot container is online.
- **`TC-1.3` PostgreSQL Table & Audit Column Schema Inspection**: Queries `information_schema.columns` to verify that the `patient` table exists with all 10 mandatory columns (`id`, `created_at`, `updated_at`, `version`, `is_deleted`, `identifier`, `full_name`, `gender`, `birth_date`, `phone_number`).

### Tier 2: Happy Path Functional E2E Tests
- **`TC-2.1` Patient Creation (`POST /api/v1/patients`)**: Submits a valid patient payload with dynamic Vietnamese CCCD identifier. Asserts HTTP `201 Created` and verifies response contains a valid RFC 4122 UUID primary key.
- **`TC-2.2` Physical Database Persistence & Audit Column Assertion**: Queries raw PostgreSQL table (`SELECT * FROM patient WHERE identifier = ...`) bypassing application cache. Asserts record exists, `created_at` and `updated_at` are populated, `version = 0`, and `is_deleted = false`.

### Tier 3: Validation & Exception Handling Tests
- **`TC-3.1` Duplicate CCCD Identifier Conflict Rejection**: Submits a duplicate `POST /api/v1/patients` request with an existing CCCD. Asserts HTTP `409 Conflict` (or `400 Bad Request`), validates structured `GlobalExceptionHandler` error format, and queries PostgreSQL to verify DB row count remains strictly 1 (transaction rollback).
- **`TC-3.2` Request Payload Validation Failure**: Submits an invalid POST payload missing mandatory required fields (e.g., blank `identifier`). Asserts HTTP `400 Bad Request` with field validation details and confirms zero records written to DB.

### Tier 4: Database State & Data Integrity Verification
- **`TC-4.1` UTF-8 Vietnamese Diacritical Character Integrity**: Submits patient names with full diacritical marks (e.g. `"Nguyễn Thị Ánh Tuyết"`). Asserts HTTP response and raw PostgreSQL record retain exact UTF-8 characters without encoding corruption or mojibake.
- **`TC-4.2` Auto-Generated UUID Uniqueness & Non-Collision**: Executes multiple patient creations to verify every record receives a distinct 36-character UUID with zero database collisions.
- **`TC-4.3` BaseEntity Initial Default Flags**: Verifies new database rows explicitly store `version = 0` (optimistic locking counter) and `is_deleted = false` (soft-delete flag).

---

## 4. Execution Commands & Guidelines

### Method 1: PowerShell 1-Click Harness (Recommended for Windows)
```powershell
# Execute from workspace root:
.\run_e2e_tests.ps1 -ApiUrl "http://localhost:8080" -DbHost "localhost" -DbPort 5432
```

### Method 2: Standalone Python Runner (No External Dependencies Required)
```bash
# Execute standalone test suite:
python e2e_test_suite.py --api-url http://localhost:8080 --db-host localhost --db-port 5432
```

### Method 3: Pytest Test Suite Execution
```bash
# Install dependencies:
pip install -r e2e-tests/requirements.txt

# Run all tier tests with verbose output:
pytest e2e-tests/ -v
```

### Method 4: Direct PostgreSQL SQL Verification
```bash
# Inspect physical database schema and audit state directly:
psql -U omnicare_user -d omnicare_db -h localhost -p 5432 -f e2e-tests/verify_db_state.sql
```

---

## 5. Verification & Compliance Matrix

| Requirement | Description | E2E Test Case ID | Verification Method | Pass Criteria |
| :--- | :--- | :--- | :--- | :--- |
| **R1** | PostgreSQL Database Infrastructure | `TC-1.1`, `TC-1.3` | Socket probe + `information_schema` query | Port 5432 open, `patient` table & 10 columns present |
| **R2** | Spring Boot Application Bootstrapping | `TC-1.2` | HTTP GET `/actuator/health` probe | HTTP status 200/204/404 response |
| **R3** | Core Model & BaseEntity Mapping | `TC-2.2`, `TC-4.3` | Direct SQL SELECT query on `patient` table | `created_at` NOT NULL, `version=0`, `is_deleted=false` |
| **R4** | End-to-End Patient API (`POST /api/v1/patients`) | `TC-2.1`, `TC-3.1`, `TC-3.2` | HTTP POST + SQL record count query | `201 Created` with UUID; duplicate rejected with `409`/`400` & count=1 |
| **R5** | UTF-8 Vietnamese Encoding | `TC-4.1` | HTTP POST + SQL SELECT UTF-8 check | Exact string `"Nguyễn Thị Ánh Tuyết"` preserved |

---

## 6. Summary

The OmniCare EMR E2E Test Suite is completely implemented, verified, and ready for deployment in continuous integration pipelines and local verification.
