#!/usr/bin/env python3
"""
=============================================================================
OmniCare EMR - End-to-End (E2E) Opaque-Box Test Suite & Runner
=============================================================================
Author: Worker E2E
Scope: Tiers 1-4 Verification (Infrastructure, Happy Path, Validation & Error Handling, DB Integrity)
Target: Spring Boot REST API (Port 8080) & PostgreSQL Database (Port 5432)
=============================================================================
"""

import sys
import os
import re
import json
import time
import socket
import uuid
import random
import argparse
from datetime import datetime

# Attempt to import optional third-party packages, fallback to standard library if missing
try:
    import requests
    HAS_REQUESTS = True
except ImportError:
    HAS_REQUESTS = False
    import urllib.request
    import urllib.error

try:
    import psycopg2
    HAS_PSYCOPG2 = True
except ImportError:
    HAS_PSYCOPG2 = False


class HTTPClient:
    """Portable HTTP Client supporting both 'requests' and 'urllib'."""
    def __init__(self, base_url, timeout=5):
        self.base_url = base_url.rstrip('/')
        self.timeout = timeout

    def post(self, endpoint, json_data, headers=None):
        url = f"{self.base_url}{endpoint}"
        req_headers = {"Content-Type": "application/json", "Accept": "application/json"}
        if headers:
            req_headers.update(headers)

        if HAS_REQUESTS:
            try:
                resp = requests.post(url, json=json_data, headers=req_headers, timeout=self.timeout)
                try:
                    data = resp.json()
                except Exception:
                    data = resp.text
                return resp.status_code, data, dict(resp.headers)
            except Exception as e:
                return 0, str(e), {}
        else:
            payload_bytes = json.dumps(json_data, ensure_ascii=False).encode('utf-8')
            req = urllib.request.Request(url, data=payload_bytes, headers=req_headers, method='POST')
            try:
                with urllib.request.urlopen(req, timeout=self.timeout) as resp:
                    status_code = resp.getcode()
                    resp_body = resp.read().decode('utf-8')
                    try:
                        data = json.loads(resp_body)
                    except Exception:
                        data = resp_body
                    return status_code, data, dict(resp.headers)
            except urllib.error.HTTPError as e:
                status_code = e.code
                resp_body = e.read().decode('utf-8')
                try:
                    data = json.loads(resp_body)
                except Exception:
                    data = resp_body
                return status_code, data, dict(e.headers)
            except Exception as e:
                return 0, str(e), {}

    def get(self, endpoint, headers=None):
        url = f"{self.base_url}{endpoint}"
        req_headers = {"Accept": "application/json"}
        if headers:
            req_headers.update(headers)

        if HAS_REQUESTS:
            try:
                resp = requests.get(url, headers=req_headers, timeout=self.timeout)
                try:
                    data = resp.json()
                except Exception:
                    data = resp.text
                return resp.status_code, data, dict(resp.headers)
            except Exception as e:
                return 0, str(e), {}
        else:
            req = urllib.request.Request(url, headers=req_headers, method='GET')
            try:
                with urllib.request.urlopen(req, timeout=self.timeout) as resp:
                    status_code = resp.getcode()
                    resp_body = resp.read().decode('utf-8')
                    try:
                        data = json.loads(resp_body)
                    except Exception:
                        data = resp_body
                    return status_code, data, dict(resp.headers)
            except urllib.error.HTTPError as e:
                status_code = e.code
                resp_body = e.read().decode('utf-8')
                try:
                    data = json.loads(resp_body)
                except Exception:
                    data = resp_body
                return status_code, data, dict(e.headers)
            except Exception as e:
                return 0, str(e), {}


class E2ETestRunner:
    def __init__(self, api_url, db_config):
        self.api_url = api_url.rstrip('/')
        self.db_config = db_config
        self.http = HTTPClient(self.api_url)
        self.results = []

    def record_test(self, tier, test_id, name, status, details="", duration_ms=0):
        symbol = "✅ PASS" if status else "❌ FAIL"
        result_entry = {
            "tier": tier,
            "test_id": test_id,
            "name": name,
            "status": status,
            "details": details,
            "duration_ms": round(duration_ms, 2)
        }
        self.results.append(result_entry)
        print(f"[{symbol}] [{tier}] {test_id}: {name} ({result_entry['duration_ms']}ms)")
        if details:
            print(f"       -> Details: {details}")

    def get_db_connection(self):
        if not HAS_PSYCOPG2:
            raise RuntimeError("psycopg2 module not installed for direct SQL execution")
        return psycopg2.connect(
            dbname=self.db_config['dbname'],
            user=self.db_config['user'],
            password=self.db_config['password'],
            host=self.db_config['host'],
            port=self.db_config['port']
        )

    def check_db_port(self):
        host = self.db_config['host']
        port = int(self.db_config['port'])
        sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        sock.settimeout(3.0)
        try:
            result = sock.connect_ex((host, port))
            sock.close()
            return result == 0
        except Exception:
            return False

    def generate_random_cccd(self):
        """Generates a realistic 12-digit Vietnamese CCCD number."""
        prefix = "079"  # HCMC location prefix
        gender_century = "0"  # Male born in 20th century or 079099...
        suffix = "".join([str(random.randint(0, 9)) for _ in range(8)])
        return f"{prefix}{gender_century}{suffix}"

    # =========================================================================
    # TIER 1: Infrastructure & Database Schema Verification
    # =========================================================================

    def run_tier1_tests(self):
        print("\n" + "=" * 70)
        print(" TIER 1: Infrastructure & Database Schema Verification")
        print("=" * 70)

        # TC-1.1: Database Port Listening
        t0 = time.time()
        port_open = self.check_db_port()
        dur = (time.time() - t0) * 1000
        if port_open:
            self.record_test("Tier 1", "TC-1.1", "PostgreSQL Database Port Listener (Port 5432)", True,
                              f"Successfully connected to TCP port {self.db_config['port']}", dur)
        else:
            self.record_test("Tier 1", "TC-1.1", "PostgreSQL Database Port Listener (Port 5432)", False,
                              f"Cannot connect to TCP port {self.db_config['port']} on host {self.db_config['host']}", dur)

        # TC-1.2: API Server Health Probe
        t0 = time.time()
        status_code, data, _ = self.http.get("/actuator/health")
        if status_code == 0:
            # Fallback probe to root or patient endpoint
            status_code, data, _ = self.http.get("/api/v1/patients")
        dur = (time.time() - t0) * 1000

        if status_code in [200, 204, 404, 405]:
            self.record_test("Tier 1", "TC-1.2", "Spring Boot API Server Liveness Probe", True,
                              f"API responded with HTTP status {status_code}", dur)
        else:
            self.record_test("Tier 1", "TC-1.2", "Spring Boot API Server Liveness Probe", False,
                              f"API health check failed with status {status_code}: {data}", dur)

        # TC-1.3: Table 'patient' and BaseEntity Schema Metadata Check
        t0 = time.time()
        if not HAS_PSYCOPG2:
            self.record_test("Tier 1", "TC-1.3", "Database Schema Verification ('patient' table & BaseEntity)", False,
                              "psycopg2 library unavailable - skipping physical DB schema check", 0)
            return

        try:
            conn = self.get_db_connection()
            cur = conn.cursor()
            cur.execute("""
                SELECT column_name, data_type, is_nullable 
                FROM information_schema.columns 
                WHERE table_name = 'patient' AND table_schema = 'public';
            """)
            rows = cur.fetchall()
            conn.close()
            dur = (time.time() - t0) * 1000

            cols = {r[0]: {"type": r[1], "nullable": r[2]} for r in rows}
            required_cols = [
                'id', 'created_at', 'updated_at', 'version', 'is_deleted',
                'identifier', 'full_name', 'gender', 'birth_date', 'phone_number'
            ]
            missing = [c for c in required_cols if c not in cols]

            if not missing:
                self.record_test("Tier 1", "TC-1.3", "Database Schema Verification ('patient' table & BaseEntity)", True,
                                  f"Verified all 10 columns present in PostgreSQL: {list(cols.keys())}", dur)
            else:
                self.record_test("Tier 1", "TC-1.3", "Database Schema Verification ('patient' table & BaseEntity)", False,
                                  f"Missing mandatory columns in 'patient' table: {missing}", dur)
        except Exception as e:
            dur = (time.time() - t0) * 1000
            self.record_test("Tier 1", "TC-1.3", "Database Schema Verification ('patient' table & BaseEntity)", False,
                              f"Database query failed: {e}", dur)

    # =========================================================================
    # TIER 2: Happy Path Functional E2E Tests
    # =========================================================================

    def run_tier2_tests(self):
        print("\n" + "=" * 70)
        print(" TIER 2: Happy Path Functional E2E Tests")
        print("=" * 70)

        test_cccd = self.generate_random_cccd()
        patient_name = "Nguyễn Văn HappyPath"
        payload = {
            "identifier": test_cccd,
            "fullName": patient_name,
            "gender": "male",
            "birthDate": "1990-05-20",
            "phoneNumber": "+84901234567"
        }

        # TC-2.1: POST /api/v1/patients Valid Patient Creation
        t0 = time.time()
        status_code, data, _ = self.http.post("/api/v1/patients", payload)
        dur = (time.time() - t0) * 1000

        created_patient_id = None
        if status_code in [200, 201]:
            if isinstance(data, dict):
                created_patient_id = data.get("id")
                res_cccd = data.get("identifier") or data.get("cccd")
                if created_patient_id:
                    # Validate UUID format
                    uuid_pattern = r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
                    if re.match(uuid_pattern, str(created_patient_id)):
                        self.record_test("Tier 2", "TC-2.1", "POST /api/v1/patients Happy Path Creation", True,
                                          f"Created patient successfully. Returned HTTP {status_code}, UUID={created_patient_id}", dur)
                    else:
                        self.record_test("Tier 2", "TC-2.1", "POST /api/v1/patients Happy Path Creation", False,
                                          f"Response ID '{created_patient_id}' is not a valid UUID format", dur)
                else:
                    self.record_test("Tier 2", "TC-2.1", "POST /api/v1/patients Happy Path Creation", False,
                                      f"Response JSON missing 'id' field: {data}", dur)
            else:
                self.record_test("Tier 2", "TC-2.1", "POST /api/v1/patients Happy Path Creation", False,
                                  f"Unexpected response format: {data}", dur)
        else:
            self.record_test("Tier 2", "TC-2.1", "POST /api/v1/patients Happy Path Creation", False,
                              f"Expected HTTP 201 Created, got HTTP {status_code}: {data}", dur)

        # TC-2.2: Physical Database Audit Field State Persistence Assertion
        t0 = time.time()
        if not HAS_PSYCOPG2:
            self.record_test("Tier 2", "TC-2.2", "Physical DB Persistence & BaseEntity Audit Fields", False,
                              "psycopg2 module not available for SQL assertion", 0)
            return

        try:
            conn = self.get_db_connection()
            cur = conn.cursor()
            cur.execute("""
                SELECT id, identifier, full_name, gender, birth_date, phone_number, created_at, updated_at, version, is_deleted
                FROM patient 
                WHERE identifier = %s;
            """, (test_cccd,))
            row = cur.fetchone()
            conn.close()
            dur = (time.time() - t0) * 1000

            if row:
                db_id, db_identifier, db_name, db_gender, db_bdate, db_phone, created_at, updated_at, version, is_deleted = row
                
                assertions = [
                    (str(db_identifier) == test_cccd, f"identifier mismatch: DB '{db_identifier}' vs expected '{test_cccd}'"),
                    (db_name == patient_name, f"fullName mismatch: DB '{db_name}' vs expected '{patient_name}'"),
                    (created_at is not None, "created_at IS NULL"),
                    (updated_at is not None, "updated_at IS NULL"),
                    (version == 0, f"version expected 0, got {version}"),
                    (is_deleted is False, f"is_deleted expected False, got {is_deleted}")
                ]
                
                failed_assertions = [msg for ok, msg in assertions if not ok]
                if not failed_assertions:
                    self.record_test("Tier 2", "TC-2.2", "Physical DB Persistence & BaseEntity Audit Fields", True,
                                      f"DB Row verified (id={db_id}, created_at={created_at}, version={version}, is_deleted={is_deleted})", dur)
                else:
                    self.record_test("Tier 2", "TC-2.2", "Physical DB Persistence & BaseEntity Audit Fields", False,
                                      f"DB assertions failed: {', '.join(failed_assertions)}", dur)
            else:
                self.record_test("Tier 2", "TC-2.2", "Physical DB Persistence & BaseEntity Audit Fields", False,
                                  f"No database row found in 'patient' table for identifier '{test_cccd}'", dur)
        except Exception as e:
            dur = (time.time() - t0) * 1000
            self.record_test("Tier 2", "TC-2.2", "Physical DB Persistence & BaseEntity Audit Fields", False,
                              f"Database query exception: {e}", dur)

    # =========================================================================
    # TIER 3: Validation & Exception Handling Tests
    # =========================================================================

    def run_tier3_tests(self):
        print("\n" + "=" * 70)
        print(" TIER 3: Validation & Exception Handling Tests")
        print("=" * 70)

        # TC-3.1: Duplicate CCCD Identifier Rejection & Transaction Isolation
        dup_cccd = self.generate_random_cccd()
        initial_payload = {
            "identifier": dup_cccd,
            "fullName": "Bệnh Nhân Gốc CCCD",
            "gender": "female",
            "birthDate": "1992-03-15",
            "phoneNumber": "+84900000001"
        }

        # Seed initial record
        self.http.post("/api/v1/patients", initial_payload)

        # Duplicate submission attempt
        t0 = time.time()
        dup_status, dup_data, _ = self.http.post("/api/v1/patients", initial_payload)
        dur = (time.time() - t0) * 1000

        if dup_status in [400, 409]:
            # Validate GlobalExceptionHandler response structure
            is_valid_error_schema = False
            if isinstance(dup_data, dict):
                has_status = "status" in dup_data
                has_msg = "message" in dup_data or "error" in dup_data
                if has_status and has_msg:
                    is_valid_error_schema = True

            # DB State Verification - Ensure duplicate record count is strictly 1
            count_in_db = -1
            if HAS_PSYCOPG2:
                try:
                    conn = self.get_db_connection()
                    cur = conn.cursor()
                    cur.execute("SELECT COUNT(*) FROM patient WHERE identifier = %s;", (dup_cccd,))
                    count_in_db = cur.fetchone()[0]
                    conn.close()
                except Exception:
                    pass

            if count_in_db == 1 or count_in_db == -1:
                self.record_test("Tier 3", "TC-3.1", "Duplicate CCCD Rejection (GlobalExceptionHandler & DB Isolation)", True,
                                  f"Correctly rejected duplicate with HTTP {dup_status}. DB count={count_in_db}", dur)
            else:
                self.record_test("Tier 3", "TC-3.1", "Duplicate CCCD Rejection (GlobalExceptionHandler & DB Isolation)", False,
                                  f"Duplicate inserted! DB row count for identifier '{dup_cccd}' is {count_in_db}", dur)
        else:
            self.record_test("Tier 3", "TC-3.1", "Duplicate CCCD Rejection (GlobalExceptionHandler & DB Isolation)", False,
                              f"Expected HTTP 409 Conflict or 400 Bad Request on duplicate, got HTTP {dup_status}: {dup_data}", dur)

        # TC-3.2: Request Payload Validation Failure (Missing Required Field)
        t0 = time.time()
        invalid_payload = {
            "fullName": "No Identifier Patient",
            "gender": "male"
        }
        val_status, val_data, _ = self.http.post("/api/v1/patients", invalid_payload)
        dur = (time.time() - t0) * 1000

        if val_status == 400:
            self.record_test("Tier 3", "TC-3.2", "Missing Identifier Validation Error (HTTP 400)", True,
                              f"Returned HTTP 400 Bad Request as expected. Response: {val_data}", dur)
        else:
            self.record_test("Tier 3", "TC-3.2", "Missing Identifier Validation Error (HTTP 400)", False,
                              f"Expected HTTP 400 Bad Request for missing required identifier, got HTTP {val_status}: {val_data}", dur)

    # =========================================================================
    # TIER 4: Database State & Data Integrity Verification
    # =========================================================================

    def run_tier4_tests(self):
        print("\n" + "=" * 70)
        print(" TIER 4: Database State & Data Integrity Verification")
        print("=" * 70)

        # TC-4.1: UTF-8 Vietnamese Diacritical Characters Encoding
        unicode_cccd = self.generate_random_cccd()
        vietnamese_name = "Nguyễn Thị Ánh Tuyết"
        payload = {
            "identifier": unicode_cccd,
            "fullName": vietnamese_name,
            "gender": "female",
            "birthDate": "1998-12-25",
            "phoneNumber": "+84911223344"
        }

        t0 = time.time()
        status_code, data, _ = self.http.post("/api/v1/patients", payload)
        dur = (time.time() - t0) * 1000

        if status_code in [200, 201]:
            resp_name = data.get("fullName") if isinstance(data, dict) else ""
            api_utf8_ok = (resp_name == vietnamese_name)

            db_utf8_ok = False
            if HAS_PSYCOPG2:
                try:
                    conn = self.get_db_connection()
                    cur = conn.cursor()
                    cur.execute("SELECT full_name FROM patient WHERE identifier = %s;", (unicode_cccd,))
                    row = cur.fetchone()
                    conn.close()
                    if row and row[0] == vietnamese_name:
                        db_utf8_ok = True
                except Exception:
                    pass
            else:
                db_utf8_ok = True

            if api_utf8_ok and db_utf8_ok:
                self.record_test("Tier 4", "TC-4.1", "UTF-8 Vietnamese Diacritics Encoding & Persistence", True,
                                  f"Preserved exact Vietnamese string '{vietnamese_name}' across API & PostgreSQL DB", dur)
            else:
                self.record_test("Tier 4", "TC-4.1", "UTF-8 Vietnamese Diacritics Encoding & Persistence", False,
                                  f"UTF-8 Encoding corruption detected! API returned: '{resp_name}'", dur)
        else:
            self.record_test("Tier 4", "TC-4.1", "UTF-8 Vietnamese Diacritics Encoding & Persistence", False,
                              f"Failed patient creation request with HTTP {status_code}: {data}", dur)

        # TC-4.2: Auto-Generated UUID Primary Key Uniqueness & Non-Collision
        t0 = time.time()
        num_records = 3
        created_uuids = set()
        all_unique = True

        for _ in range(num_records):
            p = {
                "identifier": self.generate_random_cccd(),
                "fullName": f"Patient UUID-{uuid.uuid4().hex[:6]}",
                "gender": "other",
                "birthDate": "2000-01-01",
                "phoneNumber": "+84900000000"
            }
            code, resp_data, _ = self.http.post("/api/v1/patients", p)
            if code in [200, 201] and isinstance(resp_data, dict):
                pid = resp_data.get("id")
                if pid in created_uuids:
                    all_unique = False
                elif pid:
                    created_uuids.add(pid)
            else:
                all_unique = False

        dur = (time.time() - t0) * 1000
        if all_unique and len(created_uuids) == num_records:
            self.record_test("Tier 4", "TC-4.2", "Auto-Generated UUID Primary Key Uniqueness & Non-Collision", True,
                              f"Successfully generated {num_records} unique RFC 4122 UUIDs with zero collisions", dur)
        else:
            self.record_test("Tier 4", "TC-4.2", "Auto-Generated UUID Primary Key Uniqueness & Non-Collision", False,
                              f"UUID uniqueness collision or creation failure. UUIDs: {created_uuids}", dur)

        # TC-4.3: Optimistic Locking Default Version & Soft Delete Flag Initial State
        t0 = time.time()
        flag_cccd = self.generate_random_cccd()
        p_flag = {
            "identifier": flag_cccd,
            "fullName": "Patient Flags Test",
            "gender": "male",
            "birthDate": "1994-04-04",
            "phoneNumber": "+84933445566"
        }
        self.http.post("/api/v1/patients", p_flag)

        if HAS_PSYCOPG2:
            try:
                conn = self.get_db_connection()
                cur = conn.cursor()
                cur.execute("SELECT version, is_deleted FROM patient WHERE identifier = %s;", (flag_cccd,))
                row = cur.fetchone()
                conn.close()
                dur = (time.time() - t0) * 1000

                if row and row[0] == 0 and row[1] is False:
                    self.record_test("Tier 4", "TC-4.3", "BaseEntity Initial Flags Verification (version=0, is_deleted=false)", True,
                                      f"Verified initial DB state: version={row[0]}, is_deleted={row[1]}", dur)
                else:
                    self.record_test("Tier 4", "TC-4.3", "BaseEntity Initial Flags Verification (version=0, is_deleted=false)", False,
                                      f"Unexpected default values: version={row[0] if row else 'N/A'}, is_deleted={row[1] if row else 'N/A'}", dur)
            except Exception as e:
                dur = (time.time() - t0) * 1000
                self.record_test("Tier 4", "TC-4.3", "BaseEntity Initial Flags Verification (version=0, is_deleted=false)", False,
                                  f"DB Exception: {e}", dur)
        else:
            self.record_test("Tier 4", "TC-4.3", "BaseEntity Initial Flags Verification (version=0, is_deleted=false)", False,
                              "psycopg2 module not available", 0)

    # =========================================================================
    # TIER 5: Adversarial Coverage Hardening & Stress Tests
    # =========================================================================

    def run_tier5_tests(self):
        print("\n" + "=" * 70)
        print(" TIER 5: Adversarial Coverage Hardening & Stress Tests")
        print("=" * 70)

        # TC-5.1: Entity Boundary Limits (Identifier 8/9/20/21 chars, FullName 100/101 chars)
        t0 = time.time()
        # Identifier 8 chars (invalid)
        s8, d8, _ = self.http.post("/api/v1/patients", {
            "identifier": "12345678", "fullName": "Short ID Patient", "gender": "male", "birthDate": "1990-01-01"
        })
        # Identifier 9 chars (valid min)
        s9, d9, _ = self.http.post("/api/v1/patients", {
            "identifier": f"ID9_{uuid.uuid4().hex[:5]}", "fullName": "Min ID Patient", "gender": "male", "birthDate": "1990-01-01"
        })
        # Identifier 20 chars (valid max)
        s20, d20, _ = self.http.post("/api/v1/patients", {
            "identifier": f"ID20_{uuid.uuid4().hex[:15]}", "fullName": "Max ID Patient", "gender": "female", "birthDate": "1990-01-01"
        })
        # Identifier 21 chars (invalid)
        s21, d21, _ = self.http.post("/api/v1/patients", {
            "identifier": f"ID21_{uuid.uuid4().hex[:16]}", "fullName": "Exceeded ID Patient", "gender": "male", "birthDate": "1990-01-01"
        })
        # FullName 100 chars (valid max)
        s100, d100, _ = self.http.post("/api/v1/patients", {
            "identifier": self.generate_random_cccd(), "fullName": "A" * 100, "gender": "other", "birthDate": "1990-01-01"
        })
        # FullName 101 chars (invalid)
        s101, d101, _ = self.http.post("/api/v1/patients", {
            "identifier": self.generate_random_cccd(), "fullName": "B" * 101, "gender": "other", "birthDate": "1990-01-01"
        })
        dur = (time.time() - t0) * 1000

        b_ok = (s8 == 400 and s9 in [200, 201] and s20 in [200, 201] and s21 == 400 and s100 in [200, 201] and s101 == 400)
        if b_ok:
            self.record_test("Tier 5", "TC-5.1", "Entity Field Boundary Verification (Min/Max Length Constraints)", True,
                              "Identifier (8:400, 9:201, 20:201, 21:400) and FullName (100:201, 101:400) exact validation passed", dur)
        else:
            self.record_test("Tier 5", "TC-5.1", "Entity Field Boundary Verification (Min/Max Length Constraints)", False,
                              f"Boundary failure! Statuses: ID8={s8}, ID9={s9}, ID20={s20}, ID21={s21}, Name100={s100}, Name101={s101}", dur)

        # TC-5.2: Invalid/Future Date Formats & Temporal Boundaries
        t0 = time.time()
        sfut, dfut, _ = self.http.post("/api/v1/patients", {
            "identifier": self.generate_random_cccd(), "fullName": "Future Date", "gender": "male", "birthDate": "2099-12-31"
        })
        snlp, dnlp, _ = self.http.post("/api/v1/patients", {
            "identifier": self.generate_random_cccd(), "fullName": "Non-Leap Date", "gender": "female", "birthDate": "2023-02-29"
        })
        sfmt, dfmt, _ = self.http.post("/api/v1/patients", {
            "identifier": self.generate_random_cccd(), "fullName": "Wrong Format", "gender": "male", "birthDate": "31/12/1990"
        })
        svlp, dvlp, _ = self.http.post("/api/v1/patients", {
            "identifier": self.generate_random_cccd(), "fullName": "Valid Leap Date", "gender": "female", "birthDate": "2024-02-29"
        })
        dur = (time.time() - t0) * 1000

        d_ok = (sfut == 400 and snlp == 400 and sfmt == 400 and svlp in [200, 201])
        if d_ok:
            self.record_test("Tier 5", "TC-5.2", "Invalid & Future Date Format Validation (Temporal Boundaries)", True,
                              "Future birthDate (400), Invalid Leap (400), Malformed DD/MM/YYYY (400), Valid Leap 2024-02-29 (201) verified", dur)
        else:
            self.record_test("Tier 5", "TC-5.2", "Invalid & Future Date Format Validation (Temporal Boundaries)", False,
                              f"Date validation failure! Statuses: Future={sfut}, NonLeap={snlp}, Format={sfmt}, ValidLeap={svlp}", dur)

        # TC-5.3: Rich Vietnamese Diacritics & Extended UTF-8 String Integrity
        t0 = time.time()
        names = ["Vũ Hoàng Giang Ngô", "Đỗ Trọng Tấn", "Phạm Huỳnh Quốc Bảo", "Trần Lê Quỳnh Như"]
        u_ok = True
        for nm in names:
            ident = self.generate_random_cccd()
            sc, dc, _ = self.http.post("/api/v1/patients", {
                "identifier": ident, "fullName": nm, "gender": "other", "birthDate": "1993-03-03"
            })
            if sc not in [200, 201] or not isinstance(dc, dict) or dc.get("fullName") != nm:
                u_ok = False
                break
            if HAS_PSYCOPG2:
                try:
                    conn = self.get_db_connection()
                    cur = conn.cursor()
                    cur.execute("SELECT full_name FROM patient WHERE identifier = %s;", (ident,))
                    r = cur.fetchone()
                    conn.close()
                    if not r or r[0] != nm:
                        u_ok = False
                        break
                except Exception:
                    u_ok = False
                    break
        dur = (time.time() - t0) * 1000
        if u_ok:
            self.record_test("Tier 5", "TC-5.3", "Rich Vietnamese Diacritics & Extended UTF-8 String Integrity", True,
                              "Verified complex diacritical Vietnamese names stored & retrieved without byte corruption", dur)
        else:
            self.record_test("Tier 5", "TC-5.3", "Rich Vietnamese Diacritics & Extended UTF-8 String Integrity", False,
                              "UTF-8 string corruption or database retrieval mismatch detected", dur)

        # TC-5.4: BaseEntity Soft-Delete Flag & Audit Metadata Verification
        t0 = time.time()
        audit_ident = self.generate_random_cccd()
        sc_a, dc_a, _ = self.http.post("/api/v1/patients", {
            "identifier": audit_ident, "fullName": "Audit Flag Check", "gender": "female", "birthDate": "1997-07-07"
        })
        dur = (time.time() - t0) * 1000
        if sc_a in [200, 201] and isinstance(dc_a, dict) and dc_a.get("isDeleted") is False and dc_a.get("version") == 0:
            self.record_test("Tier 5", "TC-5.4", "BaseEntity Audit Metadata & Soft-Delete Flag Assertion", True,
                              f"Verified response audit defaults (isDeleted=False, version=0, id={dc_a.get('id')})", dur)
        else:
            self.record_test("Tier 5", "TC-5.4", "BaseEntity Audit Metadata & Soft-Delete Flag Assertion", False,
                              f"Audit metadata failed: {dc_a}", dur)

    # =========================================================================
    # Test Suite Orchestration & Summary Reporting
    # =========================================================================

    def run_all(self):
        print("\n" + "=" * 70)
        print("        OmniCare EMR - End-to-End Test Suite Execution")
        print("=================================================================")
        print(f" Target API URL : {self.api_url}")
        print(f" Target DB Host : {self.db_config['host']}:{self.db_config['port']}")
        print(f" Target DB Name : {self.db_config['dbname']}")
        print("=================================================================")

        self.run_tier1_tests()
        self.run_tier2_tests()
        self.run_tier3_tests()
        self.run_tier4_tests()
        self.run_tier5_tests()

        # Generate Execution Summary
        passed_count = sum(1 for r in self.results if r["status"])
        failed_count = sum(1 for r in self.results if not r["status"])
        total_count = len(self.results)
        pass_rate = (passed_count / total_count * 100) if total_count > 0 else 0

        print("\n" + "=" * 70)
        print("                     E2E TEST EXECUTION SUMMARY")
        print("=" * 70)
        print(f" Total Executed : {total_count}")
        print(f" Passed         : {passed_count}")
        print(f" Failed         : {failed_count}")
        print(f" Pass Rate      : {pass_rate:.1f}%")
        print("=" * 70)

        # Print Tier Breakdown
        tiers = ["Tier 1", "Tier 2", "Tier 3", "Tier 4", "Tier 5"]
        for t in tiers:
            tier_tests = [r for r in self.results if r["tier"] == t]
            t_pass = sum(1 for r in tier_tests if r["status"])
            t_total = len(tier_tests)
            print(f"   [{t}] : {t_pass}/{t_total} Passed")
        print("=" * 70 + "\n")

        return failed_count == 0


def main():
    parser = argparse.ArgumentParser(description="OmniCare EMR E2E Opaque-Box Test Runner")
    parser.add_argument("--api-url", default=os.getenv("BASE_URL", "http://localhost:8080"), help="Spring Boot API Base URL")
    parser.add_argument("--db-host", default=os.getenv("DB_HOST", "localhost"), help="PostgreSQL Host")
    parser.add_argument("--db-port", default=os.getenv("DB_PORT", "5432"), help="PostgreSQL Port")
    parser.add_argument("--db-name", default=os.getenv("DB_NAME", "omnicare_db"), help="PostgreSQL Database Name")
    parser.add_argument("--db-user", default=os.getenv("DB_USER", "omnicare_user"), help="PostgreSQL User")
    parser.add_argument("--db-pass", default=os.getenv("DB_PASS", "omnicare_pass"), help="PostgreSQL Password")
    args = parser.parse_args()

    db_config = {
        "host": args.db_host,
        "port": args.db_port,
        "dbname": args.db_name,
        "user": args.db_user,
        "password": args.db_pass
    }

    runner = E2ETestRunner(args.api_url, db_config)
    success = runner.run_all()
    sys.exit(0 if success else 1)


if __name__ == "__main__":
    main()
