# CAPICS One-Click Deploy

## 标准服务器路径
- 代码根目录：`/opt/capics`
- 后端目录：`/opt/capics/backend`
- 前端目录：`/opt/capics/capics-frontend`
- 部署目录：`/opt/capics/deploy`

## 文档信息
- 更新日期：2026-04-25
- 文档版本：v1.2.2

## 版本记录
| 版本 | 日期 | 说明 |
|---|---|---|
| v1.2.2 | 2026-04-25 | 统一补齐文档更新日期与版本记录。 |
Local Debug Deploy (Windows)
1) Prepare local env file:
```powershell
Copy-Item deploy/.env.local.example deploy/.env.local
```
Update at least:
- `DB_PASSWORD`
- `POSTGRES_ADMIN_PASSWORD`
- `JWT_SECRET`
- `APP_CORS_ALLOWED_ORIGINS` (keep `http://localhost:3000`)
- `PSQL_BIN` (if your PostgreSQL path is different)
2) One-click local deploy (DB init + build + start):
```powershell
powershell -ExecutionPolicy Bypass -File deploy/local-deploy.ps1
```
Optional: prepare only (do not start backend/frontend):
```powershell
powershell -ExecutionPolicy Bypass -File deploy/local-deploy.ps1 -NoStart
```
3) Stop local backend/frontend:
```powershell
powershell -ExecutionPolicy Bypass -File deploy/local-stop.ps1
```
Local default URLs:
- Frontend: `http://127.0.0.1:3000/login`
- Backend health: `http://127.0.0.1:8080/api/health`
4) One-click start backend only:
```powershell
powershell -ExecutionPolicy Bypass -File deploy/local-start-backend.ps1
```
Optional foreground mode (for direct console logs):
```powershell
powershell -ExecutionPolicy Bypass -File deploy/local-start-backend.ps1 -Foreground
```
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

## 4) Full smoke test

```bash
bash deploy/e2e-full-smoke.sh deploy/.env.prod
```

## 5) First release flow (recommended)

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
