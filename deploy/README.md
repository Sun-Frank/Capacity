# CAPICS 一键部署使用说明

## 1. 准备参数

```bash
cp deploy/.env.prod.example deploy/.env.prod
vi deploy/.env.prod
```

必须至少修改：

- `DB_PASSWORD`
- `POSTGRES_ADMIN_PASSWORD`
- `JWT_SECRET`
- `NGINX_SERVER_NAME`
- `APP_CORS_ALLOWED_ORIGINS`

## 2. 执行一键部署

```bash
bash deploy/deploy.sh deploy/.env.prod
```

部署动作包含：

1. 数据库初始化（用户/库/schema.sql）
2. 后端构建 + systemd 服务发布和启动
3. 前端构建 + 发布到 Nginx 根目录
4. Nginx 配置写入 + 重载

## 3. 执行端到端冒烟

```bash
bash deploy/e2e-smoke.sh deploy/.env.prod
```

详细检查项见：

- `deploy/端到端自测清单.md`

## 4. 服务器实机首发（推荐）

```bash
bash deploy/first-release-run.sh deploy/.env.prod
```

该命令会按顺序执行：

1. `first-release-precheck.sh`（参数与环境门禁）
2. `deploy.sh`（数据库初始化 + 后端启动 + 前端发布）
3. `e2e-smoke.sh`（端到端冒烟）

详细首发核对项见：

- `deploy/首发检查清单.md`
