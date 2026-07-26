import socket
import pytest

def test_tier1_db_port_listening(db_config):
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(3.0)
    result = sock.connect_ex((db_config["host"], db_config["port"]))
    sock.close()
    assert result == 0, f"PostgreSQL port {db_config['port']} is not listening on host {db_config['host']}"

def test_tier1_api_liveness(api_client, api_url):
    response = api_client.get(f"{api_url}/actuator/health")
    if response.status_code == 404:
        response = api_client.get(f"{api_url}/api/v1/patients")
    assert response.status_code in [200, 204, 404, 405], f"API liveness probe failed: HTTP {response.status_code}"

def test_tier1_db_schema(db_connection):
    cursor = db_connection.cursor()
    cursor.execute("""
        SELECT column_name 
        FROM information_schema.columns 
        WHERE table_name = 'patient' AND table_schema = 'public';
    """)
    columns = [row[0] for row in cursor.fetchall()]
    
    required_cols = [
        'id', 'created_at', 'updated_at', 'version', 'is_deleted',
        'identifier', 'full_name', 'gender', 'birth_date', 'phone_number'
    ]
    missing = [c for c in required_cols if c not in columns]
    assert not missing, f"Missing required columns in 'patient' table: {missing}"
