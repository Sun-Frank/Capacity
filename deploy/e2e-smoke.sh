#!/usr/bin/env bash
set -Eeuo pipefail

ENV_FILE="${1:-$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.env.prod}"
if [[ ! -f "${ENV_FILE}" ]]; then
  echo "[ERROR] Missing env file: ${ENV_FILE}"
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

SERVER_PORT="${SERVER_PORT:-8080}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"
BASE_URL="${BASE_URL:-http://127.0.0.1}"

echo "[1/5] Backend direct health check..."
curl -fsS "http://127.0.0.1:${SERVER_PORT}/api/health" >/dev/null
echo "PASS"

echo "[2/5] Nginx health check..."
curl -fsS "${BASE_URL}/api/health" >/dev/null
echo "PASS"

echo "[3/5] Login check..."
LOGIN_RESPONSE="$(curl -fsS -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"${ADMIN_USERNAME}\",\"password\":\"${ADMIN_PASSWORD}\"}")"

TOKEN="$(echo "${LOGIN_RESPONSE}" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')"
if [[ -z "${TOKEN}" ]]; then
  echo "[ERROR] Login response does not contain token."
  echo "${LOGIN_RESPONSE}"
  exit 1
fi
echo "PASS"

echo "[4/5] Authenticated user endpoint..."
curl -fsS "${BASE_URL}/api/auth/current" -H "Authorization: Bearer ${TOKEN}" >/dev/null
echo "PASS"

echo "[5/5] Frontend index check..."
if ! curl -fsS "${BASE_URL}/" | grep -q '<div id="app">'; then
  echo "[ERROR] Frontend index content check failed."
  exit 1
fi
echo "PASS"

echo
echo "E2E smoke test completed successfully."
