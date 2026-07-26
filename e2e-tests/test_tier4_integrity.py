import uuid
import pytest

def test_tier4_unicode_vietnamese_diacritics(api_client, api_url, db_connection, random_patient_payload):
    vietnamese_name = "Nguyễn Thị Ánh Tuyết"
    random_patient_payload["fullName"] = vietnamese_name

    response = api_client.post(f"{api_url}/api/v1/patients", json=random_patient_payload)
    assert response.status_code in [200, 201]

    data = response.json()
    assert data.get("fullName") == vietnamese_name, f"API response corrupted UTF-8 string: {data.get('fullName')}"

    # Verify physical DB record
    cursor = db_connection.cursor()
    cursor.execute("SELECT full_name FROM patient WHERE identifier = %s;", (random_patient_payload["identifier"],))
    row = cursor.fetchone()
    assert row is not None
    assert row[0] == vietnamese_name, f"Database corrupted UTF-8 string: {row[0]}"

def test_tier4_uuid_uniqueness(api_client, api_url):
    created_ids = set()
    num_tests = 3
    for i in range(num_tests):
        payload = {
            "identifier": f"079999{i:06d}",
            "fullName": f"Patient UUID-{i}",
            "gender": "male",
            "birthDate": "1991-01-01",
            "phoneNumber": "+84900000000"
        }
        res = api_client.post(f"{api_url}/api/v1/patients", json=payload)
        if res.status_code in [200, 201]:
            pid = res.json().get("id")
            assert pid not in created_ids, f"Duplicate UUID generated: {pid}"
            created_ids.add(pid)
    assert len(created_ids) == num_tests

def test_tier4_base_entity_flags(db_connection, random_patient_payload, api_client, api_url):
    api_client.post(f"{api_url}/api/v1/patients", json=random_patient_payload)
    cursor = db_connection.cursor()
    cursor.execute("SELECT version, is_deleted FROM patient WHERE identifier = %s;", (random_patient_payload["identifier"],))
    row = cursor.fetchone()
    assert row is not None
    version, is_deleted = row
    assert version == 0
    assert is_deleted is False
