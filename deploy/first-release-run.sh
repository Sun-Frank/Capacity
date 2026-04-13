#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV_FILE="${1:-${SCRIPT_DIR}/.env.prod}"

echo "[1/3] Running first-release precheck..."
bash "${SCRIPT_DIR}/first-release-precheck.sh" "${ENV_FILE}"

echo "[2/3] Running deploy..."
bash "${SCRIPT_DIR}/deploy.sh" "${ENV_FILE}"

echo "[3/3] Running e2e smoke..."
bash "${SCRIPT_DIR}/e2e-smoke.sh" "${ENV_FILE}"

echo
echo "First-release flow completed."
