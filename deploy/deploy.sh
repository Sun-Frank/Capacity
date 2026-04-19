#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
ENV_FILE="${1:-${SCRIPT_DIR}/.env.prod}"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "[ERROR] Missing env file: ${ENV_FILE}"
  echo "Copy ${SCRIPT_DIR}/.env.prod.example to ${SCRIPT_DIR}/.env.prod and update values first."
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

required_vars=(
  DEPLOY_BASE SERVICE_USER JAVA_BIN SPRING_PROFILES_ACTIVE SERVER_PORT
  DB_HOST DB_PORT DB_NAME DB_URL DB_USERNAME DB_PASSWORD
  POSTGRES_ADMIN_USER POSTGRES_ADMIN_PASSWORD
  JWT_SECRET JWT_EXPIRATION APP_CORS_ALLOWED_ORIGINS
  NGINX_SERVER_NAME NGINX_WEB_ROOT NGINX_CONF_PATH
)

for var_name in "${required_vars[@]}"; do
  if [[ -z "${!var_name:-}" ]]; then
    echo "[ERROR] Required variable ${var_name} is empty in ${ENV_FILE}"
    exit 1
  fi
done

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "[ERROR] Command not found: $1"
    exit 1
  fi
}

need_cmd psql
need_cmd mvn
need_cmd npm
need_cmd "${JAVA_BIN}"
need_cmd systemctl
need_cmd nginx

if command -v rsync >/dev/null 2>&1; then
  COPY_FRONTEND_WITH_RSYNC="true"
else
  COPY_FRONTEND_WITH_RSYNC="false"
fi

SUDO=""
if [[ "${EUID}" -ne 0 ]]; then
  SUDO="sudo"
fi

BACKEND_WORK_DIR="${DEPLOY_BASE}/backend"
BACKEND_JAR_PATH="${BACKEND_WORK_DIR}/capics-backend.jar"
BACKEND_ENV_PATH="${BACKEND_WORK_DIR}/capics-backend.env"
SYSTEMD_SERVICE_PATH="/etc/systemd/system/capics-backend.service"
SCHEMA_FILE="${PROJECT_ROOT}/backend/src/main/resources/schema.sql"
FRONTEND_DIST_DIR="${PROJECT_ROOT}/capics-frontend/dist"
ENABLE_HTTPS="${ENABLE_HTTPS:-true}"
SSL_CERT_PATH="${SSL_CERT_PATH:-/etc/letsencrypt/live/${NGINX_SERVER_NAME}/fullchain.pem}"
SSL_CERT_KEY_PATH="${SSL_CERT_KEY_PATH:-/etc/letsencrypt/live/${NGINX_SERVER_NAME}/privkey.pem}"
DB_SCHEMA_MODE="${DB_SCHEMA_MODE:-incremental}" # reset | bootstrap | incremental
MIGRATIONS_DIR="${SCRIPT_DIR}/sql/migrations"

echo "[1/8] Build backend jar..."
(cd "${PROJECT_ROOT}/backend" && mvn -DskipTests clean package)

BUILT_JAR="$(ls -1t "${PROJECT_ROOT}"/backend/target/capics-backend-*.jar | grep -v '\.original$' | head -n 1)"
if [[ -z "${BUILT_JAR}" ]]; then
  echo "[ERROR] Cannot find built jar under backend/target."
  exit 1
fi

echo "[2/8] Build frontend dist..."
(cd "${PROJECT_ROOT}/capics-frontend" && npm ci && npm run build)

if [[ "${DB_SCHEMA_MODE}" != "incremental" ]] && [[ ! -f "${SCHEMA_FILE}" ]]; then
  echo "[ERROR] Missing schema file: ${SCHEMA_FILE}"
  exit 1
fi

echo "[3/8] Initialize database role and database..."
export PGPASSWORD="${POSTGRES_ADMIN_PASSWORD}"

ROLE_EXISTS="$(psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${POSTGRES_ADMIN_USER}" -d postgres -tAc "SELECT 1 FROM pg_roles WHERE rolname='${DB_USERNAME}'")"
if [[ "${ROLE_EXISTS}" != "1" ]]; then
  psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${POSTGRES_ADMIN_USER}" -d postgres -v ON_ERROR_STOP=1 \
    -c "CREATE USER ${DB_USERNAME} WITH PASSWORD '${DB_PASSWORD}';"
else
  psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${POSTGRES_ADMIN_USER}" -d postgres -v ON_ERROR_STOP=1 \
    -c "ALTER USER ${DB_USERNAME} WITH PASSWORD '${DB_PASSWORD}';"
fi

DB_EXISTS="$(psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${POSTGRES_ADMIN_USER}" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='${DB_NAME}'")"
if [[ "${DB_EXISTS}" != "1" ]]; then
  psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${POSTGRES_ADMIN_USER}" -d postgres -v ON_ERROR_STOP=1 \
    -c "CREATE DATABASE ${DB_NAME} OWNER ${DB_USERNAME};"
else
  psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${POSTGRES_ADMIN_USER}" -d postgres -v ON_ERROR_STOP=1 \
    -c "ALTER DATABASE ${DB_NAME} OWNER TO ${DB_USERNAME};"
fi

apply_incremental_migrations() {
  if [[ ! -d "${MIGRATIONS_DIR}" ]]; then
    echo "[INFO] Incremental mode: no migrations directory (${MIGRATIONS_DIR}), skip DB schema changes."
    return 0
  fi

  export PGPASSWORD="${DB_PASSWORD}"
  psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USERNAME}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 <<'SQL'
CREATE TABLE IF NOT EXISTS public.schema_migrations (
    migration_name VARCHAR(255) PRIMARY KEY,
    executed_at TIMESTAMP NOT NULL DEFAULT NOW()
);
SQL

  shopt -s nullglob
  local migration_files=("${MIGRATIONS_DIR}"/*.sql)
  shopt -u nullglob

  if [[ "${#migration_files[@]}" -eq 0 ]]; then
    echo "[INFO] Incremental mode: no migration files under ${MIGRATIONS_DIR}, skip DB schema changes."
    return 0
  fi

  for migration_file in "${migration_files[@]}"; do
    local migration_name
    migration_name="$(basename "${migration_file}")"
    local applied
    applied="$(psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USERNAME}" -d "${DB_NAME}" -tAc "SELECT 1 FROM public.schema_migrations WHERE migration_name='${migration_name}'")"
    if [[ "${applied}" == "1" ]]; then
      echo "[INFO] Skip applied migration: ${migration_name}"
      continue
    fi
    echo "[INFO] Apply migration: ${migration_name}"
    psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USERNAME}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 -f "${migration_file}"
    psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USERNAME}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 \
      -c "INSERT INTO public.schema_migrations(migration_name) VALUES ('${migration_name}')"
  done
}

echo "[4/8] Apply database schema (${DB_SCHEMA_MODE})..."
case "${DB_SCHEMA_MODE}" in
  reset)
    if [[ ! -f "${SCHEMA_FILE}" ]]; then
      echo "[ERROR] Missing schema file: ${SCHEMA_FILE}"
      exit 1
    fi
    echo "[WARN] DB_SCHEMA_MODE=reset will drop/recreate tables using schema.sql"
    export PGPASSWORD="${DB_PASSWORD}"
    psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USERNAME}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 -f "${SCHEMA_FILE}"
    ;;
  bootstrap)
    if [[ ! -f "${SCHEMA_FILE}" ]]; then
      echo "[ERROR] Missing schema file: ${SCHEMA_FILE}"
      exit 1
    fi
    export PGPASSWORD="${DB_PASSWORD}"
    TABLE_COUNT="$(psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USERNAME}" -d "${DB_NAME}" -tAc "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='public' AND table_type='BASE TABLE'")"
    if [[ "${TABLE_COUNT}" == "0" ]]; then
      echo "[INFO] Empty database detected, apply schema.sql once."
      psql -h "${DB_HOST}" -p "${DB_PORT}" -U "${DB_USERNAME}" -d "${DB_NAME}" -v ON_ERROR_STOP=1 -f "${SCHEMA_FILE}"
    else
      echo "[INFO] Existing tables detected (${TABLE_COUNT}), bootstrap mode skips schema.sql."
    fi
    ;;
  incremental)
    apply_incremental_migrations
    ;;
  *)
    echo "[ERROR] Invalid DB_SCHEMA_MODE=${DB_SCHEMA_MODE}. Use: reset | bootstrap | incremental"
    exit 1
    ;;
esac

echo "[5/8] Prepare backend runtime files..."
if ! id -u "${SERVICE_USER}" >/dev/null 2>&1; then
  ${SUDO} useradd --system --create-home --shell /usr/sbin/nologin "${SERVICE_USER}"
fi

${SUDO} mkdir -p "${BACKEND_WORK_DIR}"
${SUDO} cp -f "${BUILT_JAR}" "${BACKEND_JAR_PATH}"
${SUDO} tee "${BACKEND_ENV_PATH}" >/dev/null <<EOF
SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE}
SERVER_PORT=${SERVER_PORT}
DB_URL=${DB_URL}
DB_USERNAME=${DB_USERNAME}
DB_PASSWORD=${DB_PASSWORD}
JWT_SECRET=${JWT_SECRET}
JWT_EXPIRATION=${JWT_EXPIRATION}
APP_CORS_ALLOWED_ORIGINS=${APP_CORS_ALLOWED_ORIGINS}
LOG_LEVEL_APP=${LOG_LEVEL_APP}
LOG_LEVEL_SECURITY=${LOG_LEVEL_SECURITY}
EOF
${SUDO} chmod 600 "${BACKEND_ENV_PATH}"
${SUDO} chown -R "${SERVICE_USER}:${SERVICE_USER}" "${BACKEND_WORK_DIR}"

${SUDO} tee "${SYSTEMD_SERVICE_PATH}" >/dev/null <<EOF
[Unit]
Description=CAPICS Backend Service
After=network.target postgresql.service

[Service]
Type=simple
User=${SERVICE_USER}
WorkingDirectory=${BACKEND_WORK_DIR}
EnvironmentFile=${BACKEND_ENV_PATH}
ExecStart=${JAVA_BIN} -jar ${BACKEND_JAR_PATH}
SuccessExitStatus=143
Restart=always
RestartSec=5
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=multi-user.target
EOF

echo "[6/8] Publish frontend to nginx web root..."
${SUDO} mkdir -p "${NGINX_WEB_ROOT}"
if [[ "${COPY_FRONTEND_WITH_RSYNC}" == "true" ]]; then
  ${SUDO} rsync -a --delete "${FRONTEND_DIST_DIR}/" "${NGINX_WEB_ROOT}/"
else
  ${SUDO} rm -rf "${NGINX_WEB_ROOT:?}/"*
  ${SUDO} cp -r "${FRONTEND_DIST_DIR}/." "${NGINX_WEB_ROOT}/"
fi

echo "[7/8] Write nginx site config..."
if [[ "${ENABLE_HTTPS}" == "true" ]]; then
  if [[ ! -f "${SSL_CERT_PATH}" ]]; then
    echo "[ERROR] SSL cert file not found: ${SSL_CERT_PATH}"
    exit 1
  fi
  if [[ ! -f "${SSL_CERT_KEY_PATH}" ]]; then
    echo "[ERROR] SSL cert key file not found: ${SSL_CERT_KEY_PATH}"
    exit 1
  fi
  ${SUDO} tee "${NGINX_CONF_PATH}" >/dev/null <<EOF
server {
    listen 80;
    server_name ${NGINX_SERVER_NAME};
    return 301 https://\$host\$request_uri;
}

server {
    listen 443 ssl http2;
    server_name ${NGINX_SERVER_NAME};

    ssl_certificate ${SSL_CERT_PATH};
    ssl_certificate_key ${SSL_CERT_KEY_PATH};

    root ${NGINX_WEB_ROOT};
    index index.html;

    location /assets/ {
        try_files \$uri =404;
        expires 30d;
        add_header Cache-Control "public, max-age=2592000, immutable";
    }

    location = /index.html {
        expires -1;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:${SERVER_PORT}/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF
else
  ${SUDO} tee "${NGINX_CONF_PATH}" >/dev/null <<EOF
server {
    listen 80;
    server_name ${NGINX_SERVER_NAME};

    root ${NGINX_WEB_ROOT};
    index index.html;

    location /assets/ {
        try_files \$uri =404;
        expires 30d;
        add_header Cache-Control "public, max-age=2592000, immutable";
    }

    location = /index.html {
        expires -1;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }

    location / {
        try_files \$uri \$uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:${SERVER_PORT}/api/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }
}
EOF
fi

echo "[8/8] Restart services..."
${SUDO} systemctl daemon-reload
${SUDO} systemctl enable capics-backend
if ${SUDO} systemctl is-active --quiet capics-backend; then
  ${SUDO} systemctl restart capics-backend
else
  ${SUDO} systemctl start capics-backend
fi
${SUDO} nginx -t
${SUDO} systemctl reload nginx

echo
echo "Deploy completed."
echo "Backend service: systemctl status capics-backend --no-pager"
echo "Backend health:  curl -fsS http://127.0.0.1:${SERVER_PORT}/api/health"
echo "Nginx check:     curl -I http://127.0.0.1/"
