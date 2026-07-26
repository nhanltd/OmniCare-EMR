import os
import pytest
import socket
import psycopg2
import requests
from faker import Faker

fake = Faker(['vi_VN'])

BASE_URL = os.getenv("BASE_URL", "http://localhost:8080")
DB_HOST = os.getenv("DB_HOST", "localhost")
DB_PORT = int(os.getenv("DB_PORT", "5432"))
DB_NAME = os.getenv("DB_NAME", "omnicare_db")
DB_USER = os.getenv("DB_USER", "omnicare_user")
DB_PASS = os.getenv("DB_PASS", "omnicare_pass")

@pytest.fixture(scope="session")
def api_url():
    return BASE_URL.rstrip('/')

@pytest.fixture(scope="session")
def db_config():
    return {
        "host": DB_HOST,
        "port": DB_PORT,
        "dbname": DB_NAME,
        "user": DB_USER,
        "password": DB_PASS
    }

@pytest.fixture(scope="session")
def api_client():
    session = requests.Session()
    session.headers.update({
        "Content-Type": "application/json",
        "Accept": "application/json"
    })
    return session

@pytest.fixture
def db_connection(db_config):
    conn = psycopg2.connect(
        dbname=db_config["dbname"],
        user=db_config["user"],
        password=db_config["password"],
        host=db_config["host"],
        port=db_config["port"]
    )
    yield conn
    conn.close()

@pytest.fixture
def random_patient_payload():
    cccd_num = f"079{fake.random_number(digits=9, fix_len=True)}"
    return {
        "identifier": cccd_num,
        "fullName": fake.name(),
        "gender": fake.random_element(elements=("male", "female", "other")),
        "birthDate": fake.date_of_birth(minimum_age=1, maximum_age=90).strftime("%Y-%m-%d"),
        "phoneNumber": f"+849{fake.random_number(digits=8, fix_len=True)}"
    }
