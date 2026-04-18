# CAPICS One-Click Deploy

## 1) Prepare env file

```bash
cp deploy/.env.prod.example deploy/.env.prod
vi deploy/.env.prod
```

At minimum, update:
- `DB_PASSWORD`
- `POSTGRES_ADMIN_PASSWORD`
- `JWT_SECRET`
- `NGINX_SERVER_NAME`
- `APP_CORS_ALLOWED_ORIGINS`
- `DB_SCHEMA_MODE` (recommended: `incremental` in production)

## 2) Run deploy

```bash
bash deploy/deploy.sh deploy/.env.prod
```

This does:
1. DB initialization (`role`/`database`) + schema step by `DB_SCHEMA_MODE`
2. Backend build + systemd publish/start
3. Frontend build + publish to Nginx web root
4. Nginx config write + reload

## DB schema modes

- `DB_SCHEMA_MODE=incremental` (default): only apply SQL files under `deploy/sql/migrations/*.sql` once each (tracked by `public.schema_migrations`)
- `DB_SCHEMA_MODE=bootstrap`: apply `backend/src/main/resources/schema.sql` only when DB has no tables
- `DB_SCHEMA_MODE=reset`: force apply `schema.sql` (drops/recreates tables)

## 3) Run E2E smoke test

```bash
bash deploy/e2e-smoke.sh deploy/.env.prod
```

## 4) First release flow (recommended)

```bash
bash deploy/first-release-run.sh deploy/.env.prod
```

This runs in order:
1. `first-release-precheck.sh`
2. `deploy.sh`
3. `e2e-smoke.sh`

## HTTPS Configuration

- Set `ENABLE_HTTPS=true` to generate:
  - port `80` -> `443` redirect
  - `listen 443 ssl http2` server block
- Certificate variables:
  - `SSL_CERT_PATH`
  - `SSL_CERT_KEY_PATH`
- Defaults when omitted:
  - `/etc/letsencrypt/live/${NGINX_SERVER_NAME}/fullchain.pem`
  - `/etc/letsencrypt/live/${NGINX_SERVER_NAME}/privkey.pem`
