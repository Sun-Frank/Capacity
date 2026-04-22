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

BASE_URL="${BASE_URL:-http://127.0.0.1}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-admin123}"
TIMEOUT_SECS="${SMOKE_TIMEOUT_SECS:-20}"

api_get() {
  local url="$1"
  curl -fsS --max-time "${TIMEOUT_SECS}" "${url}" -H "Authorization: Bearer ${TOKEN}"
}

api_post_json() {
  local url="$1"
  local body="$2"
  curl -fsS --max-time "${TIMEOUT_SECS}" -X POST "${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d "${body}"
}

api_put_json() {
  local url="$1"
  local body="$2"
  curl -fsS --max-time "${TIMEOUT_SECS}" -X PUT "${url}" \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" \
    -d "${body}"
}

assert_success() {
  local json="$1"
  local hint="$2"
  JSON_INPUT="${json}" python3 - "$hint" <<'PY'
import json,os,sys
hint=sys.argv[1]
try:
    obj=json.loads(os.environ.get("JSON_INPUT", ""))
except Exception as e:
    print(f"[ERROR] {hint}: invalid JSON: {e}")
    sys.exit(1)
if not obj.get("success", False):
    print(f"[ERROR] {hint}: success=false, message={obj.get('message')}")
    sys.exit(1)
print(f"PASS {hint}")
PY
}

echo "[1/12] health"
curl -fsS --max-time "${TIMEOUT_SECS}" "${BASE_URL}/api/health" >/dev/null
echo "PASS health"

echo "[2/12] login"
LOGIN_JSON="$(curl -fsS --max-time "${TIMEOUT_SECS}" -X POST "${BASE_URL}/api/auth/login" -H "Content-Type: application/json" -d "{\"username\":\"${ADMIN_USERNAME}\",\"password\":\"${ADMIN_PASSWORD}\"}")"
TOKEN="$(JSON_INPUT="${LOGIN_JSON}" python3 - <<'PY'
import json,os
obj=json.loads(os.environ.get("JSON_INPUT", ""))
print((obj.get("data") or {}).get("token") or "")
PY
)"
if [[ -z "${TOKEN}" ]]; then
  echo "[ERROR] Login token missing"
  echo "${LOGIN_JSON}"
  exit 1
fi
echo "PASS login"

echo "[3/12] core pages APIs"
assert_success "$(api_get "${BASE_URL}/api/auth/current")" "auth/current"
assert_success "$(api_get "${BASE_URL}/api/lines")" "lines"
assert_success "$(api_get "${BASE_URL}/api/products/families")" "products/families"
assert_success "$(api_get "${BASE_URL}/api/products/family-lines")" "products/family-lines"
assert_success "$(api_get "${BASE_URL}/api/products")" "products"
assert_success "$(api_get "${BASE_URL}/api/routings/full")" "routings/full"
assert_success "$(api_get "${BASE_URL}/api/ct-lines")" "ct-lines"

echo "[4/12] mrp selectors"
CB_JSON="$(api_get "${BASE_URL}/api/mrp/filters/created-bys")"
assert_success "${CB_JSON}" "mrp/filters/created-bys"
CREATED_BY="$(JSON_INPUT="${CB_JSON}" python3 - <<'PY'
import json,os
obj=json.loads(os.environ.get("JSON_INPUT", ""))
arr=obj.get('data') or []
print(arr[0] if arr else '')
PY
)"
if [[ -z "${CREATED_BY}" ]]; then
  echo "[ERROR] No MRP createdBy found"
  exit 1
fi

FILES_JSON="$(api_get "${BASE_URL}/api/mrp/filters/${CREATED_BY}/files")"
assert_success "${FILES_JSON}" "mrp/filters/{createdBy}/files"
FILE_NAME="$(JSON_INPUT="${FILES_JSON}" python3 - <<'PY'
import json,os
obj=json.loads(os.environ.get("JSON_INPUT", ""))
arr=obj.get('data') or []
print(arr[0] if arr else '')
PY
)"
if [[ -z "${FILE_NAME}" ]]; then
  echo "[ERROR] No MRP file found"
  exit 1
fi

ENC_FILE="$(python3 - <<'PY' "${FILE_NAME}"
import sys,urllib.parse
print(urllib.parse.quote(sys.argv[1], safe=''))
PY
)"
VERSIONS_JSON="$(api_get "${BASE_URL}/api/mrp/filters/${CREATED_BY}/${ENC_FILE}/versions")"
assert_success "${VERSIONS_JSON}" "mrp/filters/{createdBy}/{file}/versions"
VERSION="$(JSON_INPUT="${VERSIONS_JSON}" python3 - <<'PY'
import json,os
obj=json.loads(os.environ.get("JSON_INPUT", ""))
arr=obj.get('data') or []
print(arr[0] if arr else '')
PY
)"
if [[ -z "${VERSION}" ]]; then
  echo "[ERROR] No MRP version found"
  exit 1
fi

echo "[5/12] mrp reports"
assert_success "$(api_get "${BASE_URL}/api/mrp/reports/weekly/by-file?createdBy=${CREATED_BY}&fileName=${ENC_FILE}")" "mrp weekly report"
assert_success "$(api_get "${BASE_URL}/api/mrp/reports/monthly/by-file?createdBy=${CREATED_BY}&fileName=${ENC_FILE}")" "mrp monthly report"

echo "[6/12] static capacity (week/month)"
assert_success "$(api_get "${BASE_URL}/api/capacity-assessment?createdBy=${CREATED_BY}&fileName=${ENC_FILE}&version=${VERSION}")" "capacity week"
assert_success "$(api_get "${BASE_URL}/api/capacity-assessment/monthly?createdBy=${CREATED_BY}&fileName=${ENC_FILE}&version=${VERSION}")" "capacity month"

echo "[7/12] dashboard"
assert_success "$(api_get "${BASE_URL}/api/dashboard/loading?type=static&dimension=week&createdBy=${CREATED_BY}&fileName=${ENC_FILE}&version=${VERSION}")" "dashboard static week"
assert_success "$(api_get "${BASE_URL}/api/dashboard/loading?type=static&dimension=month&createdBy=${CREATED_BY}&fileName=${ENC_FILE}&version=${VERSION}")" "dashboard static month"

echo "[8/12] dynamic calculation"
assert_success "$(api_post_json "${BASE_URL}/api/line-realtime/calculate?version=${VERSION}" '{}')" "line-realtime calculate"
assert_success "$(api_get "${BASE_URL}/api/line-realtime/list?version=${VERSION}")" "line-realtime list"

echo "[9/12] snapshot save+load (week/month)"
SNAP="SMOKE_$(date +%Y%m%d%H%M%S)"
WEEK_BODY="$(python3 - <<'PY' "${CREATED_BY}" "${FILE_NAME}" "${VERSION}" "${SNAP}"
import json,sys
created_by,file_name,version,snap=sys.argv[1:]
print(json.dumps({
  "createdBy": created_by,
  "fileName": file_name,
  "version": version,
  "snapshotName": snap,
  "source": "static",
  "dimension": "week",
  "linesData": {},
  "dates": [],
  "dateLabels": {}
}, ensure_ascii=False))
PY
)"
assert_success "$(api_post_json "${BASE_URL}/api/simulation-snapshots" "${WEEK_BODY}")" "snapshot save week"
assert_success "$(api_get "${BASE_URL}/api/simulation-snapshots/names?createdBy=${CREATED_BY}&fileName=${ENC_FILE}&version=${VERSION}&source=static&dimension=week")" "snapshot names week"
ENC_SNAP="$(python3 - <<'PY' "${SNAP}"
import sys,urllib.parse
print(urllib.parse.quote(sys.argv[1], safe=''))
PY
)"
assert_success "$(api_get "${BASE_URL}/api/simulation-snapshots?createdBy=${CREATED_BY}&fileName=${ENC_FILE}&version=${VERSION}&snapshotName=${ENC_SNAP}&source=static&dimension=week")" "snapshot get week"

MONTH_BODY="$(python3 - <<'PY' "${CREATED_BY}" "${FILE_NAME}" "${VERSION}" "${SNAP}"
import json,sys
created_by,file_name,version,snap=sys.argv[1:]
print(json.dumps({
  "createdBy": created_by,
  "fileName": file_name,
  "version": version,
  "snapshotName": snap,
  "source": "static",
  "dimension": "month",
  "linesData": {},
  "dates": [],
  "dateLabels": {}
}, ensure_ascii=False))
PY
)"
assert_success "$(api_post_json "${BASE_URL}/api/simulation-snapshots" "${MONTH_BODY}")" "snapshot save month"
assert_success "$(api_get "${BASE_URL}/api/simulation-snapshots/names?createdBy=${CREATED_BY}&fileName=${ENC_FILE}&version=${VERSION}&source=static&dimension=month")" "snapshot names month"


echo "[10/12] ct-line inline update"
CT_JSON="$(api_get "${BASE_URL}/api/ct-lines")"
assert_success "${CT_JSON}" "ct-lines list"
CT_PAYLOAD_AND_ID="$(JSON_INPUT="${CT_JSON}" python3 - <<'PY'
import json,os,sys
obj=json.loads(os.environ.get("JSON_INPUT", ""))
rows=((obj.get('data') or {}).get('rows') or [])
if not rows:
  print('')
  sys.exit(0)
r=rows[0]
payload={
  "colB": r.get("colB") or "",
  "colC": r.get("colC") or "",
  "colD": r.get("colD") or "",
  "colF": r.get("colF") or "",
  "colI": r.get("colI") or "",
  "colP": r.get("colP") or "",
  "colW": r.get("colW") or "",
  "colX": r.get("colX") or ""
}
print(str(r.get('id')) + '\t' + json.dumps(payload, ensure_ascii=False))
PY
)"
if [[ -z "${CT_PAYLOAD_AND_ID}" ]]; then
  echo "[ERROR] ct-lines has no rows"
  exit 1
fi
CT_ID="${CT_PAYLOAD_AND_ID%%$'\t'*}"
CT_BODY="${CT_PAYLOAD_AND_ID#*$'\t'}"
assert_success "$(api_put_json "${BASE_URL}/api/ct-lines/${CT_ID}" "${CT_BODY}")" "ct-lines update"

echo "[11/12] fusion pages APIs"
assert_success "$(api_get "${BASE_URL}/api/fusion/line-profiles")" "fusion line profiles"
assert_success "$(api_get "${BASE_URL}/api/fusion/manpower-plans")" "fusion manpower plans"
assert_success "$(api_get "${BASE_URL}/api/fusion/meeting-minutes")" "fusion meeting minutes"

echo "[12/12] frontend entry"
curl -fsS --max-time "${TIMEOUT_SECS}" "${BASE_URL}/" | grep -q '<div id="app">'
echo "PASS frontend index"

echo
echo "Full smoke test completed successfully."
