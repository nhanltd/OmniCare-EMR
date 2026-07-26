import pytest

def test_tier3_duplicate_cccd_rejection(api_client, api_url, db_connection, random_patient_payload):
    # 1st creation
    res1 = api_client.post(f"{api_url}/api/v1/patients", json=random_patient_payload)
    assert res1.status_code in [200, 201]

    # 2nd creation (duplicate identifier)
    res2 = api_client.post(f"{api_url}/api/v1/patients", json=random_patient_payload)
    assert res2.status_code in [400, 409], f"Expected HTTP 400 or 409 on duplicate CCCD, got {res2.status_code}"

    error_json = res2.json()
    assert "status" in error_json or "error" in error_json, "Error response missing status/error key"

    # DB Integrity Check: Count must be exactly 1
    cursor = db_connection.cursor()
    cursor.execute("SELECT COUNT(*) FROM patient WHERE identifier = %s;", (random_patient_payload["identifier"],))
    count = cursor.fetchone()[0]
    assert count == 1, f"Expected 1 record in DB for duplicate test, found {count}"

def test_tier3_missing_identifier_validation(api_client, api_url, db_connection):
    invalid_payload = {
        "fullName": "Missing Identifier Person",
        "gender": "female"
    }
    response = api_client.post(f"{api_url}/api/v1/patients", json=invalid_payload)
    assert response.status_code == 400, f"Expected HTTP 400 Bad Request, got {response.status_code}"
