#!/usr/bin/env bash
# =============================================================================
# OmniCare EMR - E2E Integration Test Suite Bash Runner
# Author: Worker E2E
# Usage: ./run_e2e_tests.sh [API_URL] [DB_HOST] [DB_PORT]
# =============================================================================

set -e

API_URL="${1:-http://localhost:8080}"
DB_HOST="${2:-localhost}"
DB_PORT="${3:-5432}"
DB_NAME="${4:-omnicare_db}"
DB_USER="${5:-omnicare_user}"
DB_PASS="${6:-omnicare_pass}"

echo "================================================================="
echo "         OmniCare EMR - Opaque-Box E2E Test Suite               "
echo "================================================================="

echo -e "\n[1/3] Probing PostgreSQL TCP Port ($DB_HOST:$DB_PORT)..."
if nc -z -w 3 "$DB_HOST" "$DB_PORT" 2>/dev/null; then
    echo "✅ PostgreSQL TCP Port $DB_PORT is OPEN."
else
    echo "⚠️ Warning: Port $DB_PORT socket probe did not confirm, proceeding with python runner..."
fi

echo -e "\n[2/3] Probing Spring Boot API Liveness ($API_URL)..."
curl -s "$API_URL/actuator/health" > /dev/null && echo "✅ API server responds." || echo "⚠️ API probe did not return 200, proceeding..."

echo -e "\n[3/3] Executing Python E2E Test Suite (Tiers 1-5)..."
python e2e_test_suite.py --api-url "$API_URL" --db-host "$DB_HOST" --db-port "$DB_PORT" --db-name "$DB_NAME" --db-user "$DB_USER" --db-pass "$DB_PASS"
