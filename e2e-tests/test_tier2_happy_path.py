import re
import pytest

def test_tier2_create_patient_success(api_client, api_url, db_connection, random_patient_payload):
    response = api_client.post(f"{api_url}/api/v1/patients", json=random_patient_payload)
    assert response.status_code in [200, 201], f"Expected 201 Created, got HTTP {response.status_code}: {response.text}"

    data = response.json()
    patient_id = data.get("id")
    assert patient_id is not None, "Response JSON missing 'id' field"

    uuid_pattern = r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$'
    assert re.match(uuid_pattern, str(patient_id)), f"Returned ID '{patient_id}' is not a valid UUID"

    # Physical DB verification
    cursor = db_connection.cursor()
    cursor.execute("""
        SELECT id, identifier, full_name, created_at, updated_at, version, is_deleted 
        FROM patient WHERE identifier = %s;
    """, (random_patient_payload["identifier"],))
    row = cursor.fetchone()
    assert row is not None, f"No record found in DB for identifier {random_patient_payload['identifier']}"

    db_id, db_cccd, db_name, created_at, updated_at, version, is_deleted = row
    assert str(db_cccd) == random_patient_payload["identifier"]
    assert db_name == random_patient_payload["fullName"]
    assert created_at is not None
    assert updated_at is not None
    assert version == 0
    assert is_deleted is False
