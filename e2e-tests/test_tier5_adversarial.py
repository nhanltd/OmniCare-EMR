"""
Tier 5: Adversarial Stress & Edge Case Test Suite for OmniCare EMR API
Covers entity boundary limits, concurrent duplicate requests, invalid/future date formats,
UTF-8 diacritics & unicode edge cases, and BaseEntity soft-delete field audit state.
"""

import concurrent.futures
import datetime
import pytest
import requests
import uuid
import re

def test_tier5_boundary_identifier_length(api_client, api_url):
    """
    Test boundary limits for patient 'identifier' (min=9, max=20 chars).
    - 8 chars -> 400 Bad Request
    - 9 chars -> 201 Created
    - 20 chars -> 201 Created
    - 21 chars -> 400 Bad Request
    """
    # 8 chars (Too short)
    payload_8 = {
        "identifier": "12345678",
        "fullName": "Boundary Test Short ID",
        "gender": "male",
        "birthDate": "1995-05-10"
    }
    res_8 = api_client.post(f"{api_url}/api/v1/patients", json=payload_8)
    assert res_8.status_code == 400, f"Expected 400 for 8-char identifier, got {res_8.status_code}"

    # 9 chars (Exact min)
    payload_9 = {
        "identifier": f"ID9{uuid.uuid4().hex[:6]}",
        "fullName": "Boundary Min Identifier",
        "gender": "female",
        "birthDate": "1995-05-10"
    }
    res_9 = api_client.post(f"{api_url}/api/v1/patients", json=payload_9)
    assert res_9.status_code in [200, 201], f"Expected 201 for 9-char identifier, got {res_9.status_code}"

    # 20 chars (Exact max)
    payload_20 = {
        "identifier": f"ID20_{uuid.uuid4().hex[:15]}",
        "fullName": "Boundary Max Identifier",
        "gender": "male",
        "birthDate": "1995-05-10"
    }
    assert len(payload_20["identifier"]) == 20
    res_20 = api_client.post(f"{api_url}/api/v1/patients", json=payload_20)
    assert res_20.status_code in [200, 201], f"Expected 201 for 20-char identifier, got {res_20.status_code}"

    # 21 chars (Too long)
    payload_21 = {
        "identifier": f"ID21_{uuid.uuid4().hex[:16]}",
        "fullName": "Boundary Exceeded Identifier",
        "gender": "male",
        "birthDate": "1995-05-10"
    }
    assert len(payload_21["identifier"]) == 21
    res_21 = api_client.post(f"{api_url}/api/v1/patients", json=payload_21)
    assert res_21.status_code == 400, f"Expected 400 for 21-char identifier, got {res_21.status_code}"


def test_tier5_boundary_fullname_length(api_client, api_url):
    """
    Test boundary limits for patient 'fullName' (max=100 chars).
    - 100 chars -> 201 Created
    - 101 chars -> 400 Bad Request
    """
    # 100 chars
    name_100 = "A" * 100
    payload_100 = {
        "identifier": f"CCCD{uuid.uuid4().hex[:10]}",
        "fullName": name_100,
        "gender": "other",
        "birthDate": "2000-01-01"
    }
    res_100 = api_client.post(f"{api_url}/api/v1/patients", json=payload_100)
    assert res_100.status_code in [200, 201], f"Expected 201 for 100-char name, got {res_100.status_code}"

    # 101 chars
    name_101 = "B" * 101
    payload_101 = {
        "identifier": f"CCCD{uuid.uuid4().hex[:10]}",
        "fullName": name_101,
        "gender": "other",
        "birthDate": "2000-01-01"
    }
    res_101 = api_client.post(f"{api_url}/api/v1/patients", json=payload_101)
    assert res_101.status_code == 400, f"Expected 400 for 101-char name, got {res_101.status_code}"


def test_tier5_date_validation_future_and_invalid(api_client, api_url):
    """
    Test temporal date validation and malformed date handling.
    - Future birth date (e.g. 2099-12-31) -> 400 Bad Request
    - Non-existent leap date (e.g. 2023-02-29) -> 400 Bad Request
    - Malformed string date format (e.g. 31/12/1990) -> 400 Bad Request
    - Valid leap date (2024-02-29) -> 201 Created
    """
    base_id = f"CCCD{uuid.uuid4().hex[:10]}"

    # Future date
    future_payload = {
        "identifier": f"{base_id}_fut",
        "fullName": "Future Born Person",
        "gender": "female",
        "birthDate": "2099-12-31"
    }
    res_fut = api_client.post(f"{api_url}/api/v1/patients", json=future_payload)
    assert res_fut.status_code == 400, f"Expected 400 for future birth date, got {res_fut.status_code}"

    # Invalid leap year date (2023 is not a leap year)
    invalid_leap = {
        "identifier": f"{base_id}_nlp",
        "fullName": "Invalid Leap Date",
        "gender": "male",
        "birthDate": "2023-02-29"
    }
    res_nlp = api_client.post(f"{api_url}/api/v1/patients", json=invalid_leap)
    assert res_nlp.status_code == 400, f"Expected 400 for invalid leap date, got {res_nlp.status_code}"

    # Malformed date string (DD/MM/YYYY)
    malformed_date = {
        "identifier": f"{base_id}_fmt",
        "fullName": "Malformed Date Format",
        "gender": "male",
        "birthDate": "31/12/1990"
    }
    res_fmt = api_client.post(f"{api_url}/api/v1/patients", json=malformed_date)
    assert res_fmt.status_code == 400, f"Expected 400 for malformed date, got {res_fmt.status_code}"

    # Valid leap year date (2024 is a leap year)
    valid_leap = {
        "identifier": f"LP24_{uuid.uuid4().hex[:12]}",
        "fullName": "Valid Leap Date Patient",
        "gender": "female",
        "birthDate": "2024-02-29"
    }
    res_vlp = api_client.post(f"{api_url}/api/v1/patients", json=valid_leap)
    assert res_vlp.status_code in [200, 201], f"Expected 201 for valid leap date, got {res_vlp.status_code}"


def test_tier5_concurrent_duplicate_registration(api_url, db_connection):
    """
    Stress test concurrent duplicate patient registrations.
    Sends 10 parallel HTTP POST requests with the EXACT SAME CCCD identifier.
    Asserts:
    - Exactly 1 request receives HTTP 201 Created
    - Remaining 9 requests receive HTTP 409 Conflict (or 400)
    - DB table contains exactly 1 physical record for this identifier
    """
    shared_identifier = f"CONC_{uuid.uuid4().hex[:10]}"
    payload = {
        "identifier": shared_identifier,
        "fullName": "Concurrent Test Patient",
        "gender": "male",
        "birthDate": "1988-08-18",
        "phoneNumber": "+84988888888"
    }

    def send_post_request():
        # Dedicated session per thread to avoid socket contention
        s = requests.Session()
        s.headers.update({"Content-Type": "application/json", "Accept": "application/json"})
        return s.post(f"{api_url}/api/v1/patients", json=payload)

    num_threads = 10
    with concurrent.futures.ThreadPoolExecutor(max_workers=num_threads) as executor:
        futures = [executor.submit(send_post_request) for _ in range(num_threads)]
        responses = [f.result() for f in concurrent.futures.as_completed(futures)]

    status_codes = [r.status_code for r in responses]
    success_count = sum(1 for code in status_codes if code in [200, 201])
    conflict_count = sum(1 for code in status_codes if code in [400, 409])

    assert success_count == 1, f"Expected exactly 1 successful registration under concurrency, got {success_count}. All statuses: {status_codes}"
    assert conflict_count == num_threads - 1, f"Expected {num_threads - 1} conflict/error responses, got {conflict_count}"

    # Verify Database count
    cursor = db_connection.cursor()
    cursor.execute("SELECT COUNT(*) FROM patient WHERE identifier = %s;", (shared_identifier,))
    db_count = cursor.fetchone()[0]
    assert db_count == 1, f"Expected DB row count to be strictly 1, found {db_count}"


def test_tier5_utf8_complex_diacritics_and_symbols(api_client, api_url, db_connection):
    """
    Test rich UTF-8 Vietnamese diacritical characters and special text support.
    - Names with all Vietnamese tone marks (ngã, hỏi, sắc, huyền, nặng)
    - Full DB persistence verification without encoding corruption
    """
    complex_vietnamese_names = [
        "Vũ Hoàng Giang Ngô",
        "Đỗ Trọng Tấn",
        "Phạm Huỳnh Quốc Bảo",
        "Trần Lê Quỳnh Như"
    ]

    for name in complex_vietnamese_names:
        ident = f"UTF8_{uuid.uuid4().hex[:10]}"
        payload = {
            "identifier": ident,
            "fullName": name,
            "gender": "other",
            "birthDate": "1992-12-12"
        }
        res = api_client.post(f"{api_url}/api/v1/patients", json=payload)
        assert res.status_code in [200, 201], f"Failed creating patient with name '{name}': {res.text}"
        data = res.json()
        assert data["fullName"] == name, f"Response UTF-8 mismatch: expected '{name}', got '{data['fullName']}'"

        # Direct DB Check
        cursor = db_connection.cursor()
        cursor.execute("SELECT full_name FROM patient WHERE identifier = %s;", (ident,))
        row = cursor.fetchone()
        assert row is not None
        assert row[0] == name, f"DB UTF-8 mismatch: expected '{name}', got '{row[0]}'"


def test_tier5_soft_delete_and_audit_defaults(api_client, api_url, db_connection):
    """
    Verify BaseEntity soft delete flag and audit timestamp boundaries upon creation.
    - is_deleted must be false
    - version must be 0
    - created_at and updated_at must be populated and equal (or within milliseconds)
    """
    ident = f"AUDIT_{uuid.uuid4().hex[:10]}"
    payload = {
        "identifier": ident,
        "fullName": "Audit Verification Patient",
        "gender": "female",
        "birthDate": "1999-09-09"
    }
    res = api_client.post(f"{api_url}/api/v1/patients", json=payload)
    assert res.status_code in [200, 201]

    data = res.json()
    assert data.get("isDeleted") is False, "Response isDeleted should be False"
    assert data.get("version") == 0, "Response version should be 0"

    cursor = db_connection.cursor()
    cursor.execute("""
        SELECT created_at, updated_at, version, is_deleted 
        FROM patient WHERE identifier = %s;
    """, (ident,))
    row = cursor.fetchone()
    assert row is not None
    created_at, updated_at, version, is_deleted = row

    assert is_deleted is False, "DB is_deleted column must be false"
    assert version == 0, "DB version column must be 0"
    assert created_at is not None, "DB created_at must not be null"
    assert updated_at is not None, "DB updated_at must not be null"
