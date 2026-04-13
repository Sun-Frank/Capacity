#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${1:-${SCRIPT_DIR}/.env.prod}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "[ERROR] Missing env file: ${ENV_FILE}"
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

errors=0
warns=0

pass() { echo "[PASS] $1"; }
warn() { echo "[WARN] $1"; warns=$((warns + 1)); }
fail() { echo "[FAIL] $1"; errors=$((errors + 1)); }

require_value() {
  local key="$1"
  local value="${!key:-}"
  if [[ -z "${value}" ]]; then
    fail "${key} is empty"
    return
  fi
  if [[ "${value}" =~ CHANGE_ME|please_change_me|please-change-this-secret ]]; then
    fail "${key} still uses placeholder value"
    return
  fi
  pass "${key} is set"
}

check_cmd() {
  if command -v "$1" >/dev/null 2>&1; then
    pass "command exists: $1"
  else
    fail "command missing: $1"
  fi
}

check_port_free() {
  local port="$1"
  if ss -lntp 2>/dev/null | grep -q ":${port} "; then
    warn "port ${port} is already in use (check if expected)"
  else
    pass "port ${port} is free"
  fi
}

echo "== CAPICS first-release precheck =="

for cmd in bash curl psql java mvn npm nginx systemctl ss; do
  check_cmd "${cmd}"
done

for key in \
  DEPLOY_BASE SERVICE_USER JAVA_BIN SPRING_PROFILES_ACTIVE SERVER_PORT \
  DB_HOST DB_PORT DB_NAME DB_URL DB_USERNAME DB_PASSWORD \
  POSTGRES_ADMIN_USER POSTGRES_ADMIN_PASSWORD \
  JWT_SECRET APP_CORS_ALLOWED_ORIGINS \
  NGINX_SERVER_NAME NGINX_WEB_ROOT NGINX_CONF_PATH; do
  require_value "${key}"
done

if [[ -n "${JWT_SECRET:-}" && "${#JWT_SECRET}" -lt 32 ]]; then
  fail "JWT_SECRET length must be >= 32"
else
  pass "JWT_SECRET length is valid"
fi

if [[ "${APP_CORS_ALLOWED_ORIGINS:-}" == "*" ]]; then
  fail "APP_CORS_ALLOWED_ORIGINS cannot be * in production"
fi

if [[ "${SERVER_PORT:-8080}" != "80" ]]; then
  check_port_free "${SERVER_PORT:-8080}"
fi

if [[ -d "${DEPLOY_BASE:-}" ]]; then
  pass "DEPLOY_BASE exists: ${DEPLOY_BASE}"
else
  warn "DEPLOY_BASE does not exist yet: ${DEPLOY_BASE} (will be created by deploy.sh)"
fi

if systemctl is-active postgresql >/dev/null 2>&1; then
  pass "postgresql service is active"
else
  warn "postgresql service is not active"
fi

if systemctl is-active nginx >/dev/null 2>&1; then
  pass "nginx service is active"
else
  warn "nginx service is not active"
fi

export PGPASSWORD="${POSTGRES_ADMIN_PASSWORD:-}"
if psql -h "${DB_HOST:-127.0.0.1}" -p "${DB_PORT:-5432}" -U "${POSTGRES_ADMIN_USER:-postgres}" -d postgres -tAc "SELECT 1" >/dev/null 2>&1; then
  pass "postgres admin connectivity check passed"
else
  fail "postgres admin connectivity check failed"
fi

if [[ "${errors}" -gt 0 ]]; then
  echo
  echo "Precheck failed with ${errors} error(s), ${warns} warning(s)."
  exit 1
fi

echo
echo "Precheck passed with ${warns} warning(s)."
