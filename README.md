# CAPICS 产能评估系统（融合版）

CAPICS 是一个面向汽车电子工厂的产能评估系统，融合了两个历史系统能力（`capics_V1` + `CAPACITY_V2`），提供从基础主数据、MRP 计划、工艺路线、产线参数到静态/动态产能评估的完整闭环。

- 前端：Vue 3 + Vite
- 后端：Spring Boot 2.7 + Spring Security + JPA + PostgreSQL
- 部署：Nginx + Systemd + 一键脚本（数据库初始化 + 后端启动 + 前端发布）
- 代码特性：后端已为**无 Lombok 版本**，规避 JDK25 下 Lombok 兼容问题

---

## 1. 项目目录

```text
capics_V1/
├─ backend/                  # Spring Boot 后端
├─ capics-frontend/          # Vue3 前端（生产使用）
├─ deploy/                   # 一键部署与首发检查脚本
├─ 融合设计说明.md            # 融合改造说明
└─ 部署手册.md                # 详细部署手册
```

说明：根目录 `frontend/` 为历史目录，当前生产部署使用 `capics-frontend/`。

---

## 2. 功能模块

### 2.1 前端路由模块

- `/login` 登录
- `/` 仪表盘
- `/products` 产品与编码族
- `/mrp` MRP 计划管理
- `/routing` 工艺路线
- `/lines` 产线配置
- `/product-line` 编码族定线
- `/capacity-assessment` 静态周产能评估
- `/capacity-assessment-monthly` 静态月产能评估（融合新增）
- `/capacity-realtime` 动态周实时模拟
- `/capacity-realtime-monthly` 动态月实时模拟
- `/fusion-workbench` 融合工作台（产线画像、人力计划、会议纪要）
- `/users` 用户管理

### 2.2 后端核心接口（按控制器分组）

- 认证：`/api/auth/*`
- 健康检查：`/api/health`
- 仪表盘：`/api/dashboard/*`
- 产能评估：`/api/capacity-assessment/*`
- 产线实时模拟：`/api/line-realtime/*`
- MRP：`/api/mrp/*`
- 产品/编码族/定线：`/api/products/*`
- 工艺路线：`/api/routings/*`
- 产线配置：`/api/lines/*`
- 用户：`/api/users/*`
- 快照：`/api/simulation-snapshots/*`
- 融合接口：
  - `/api/fusion/line-profiles`
  - `/api/fusion/manpower-plans`
  - `/api/fusion/meeting-minutes`

---

## 3. 核心业务逻辑

### 3.1 数据依赖顺序（导入/维护建议）

`编码族 -> 产品 -> 编码族定线 -> 工艺路线 -> 产线配置 -> MRP 计划 -> 产能评估/模拟`

### 3.2 融合后的 LOAD 公式

周/月 LOAD 在原公式基础上引入人力因子（`manpower_plan`）：

```text
LOAD = demand * CT / (工作天 * 班次 * 每班小时 * OEE * 3600 * manpowerFactor)
```

- `manpowerFactor` 按 `lineClass + 日期` 取最近一条计划值
- 默认值 `1.0`

---

## 4. 技术栈与版本要求

- Java：11（推荐，不建议直接切到 JDK25 运行生产）
- Maven：3.8+
- Node.js：16+
- PostgreSQL：12+
- Nginx：1.18+
- 操作系统：Ubuntu 20.04+/CentOS 7+

---

## 5. 本地开发启动

### 5.1 后端

```bash
cd backend
mvn clean package -DskipTests
mvn spring-boot:run
```

默认端口：`8080`

### 5.2 前端

```bash
cd capics-frontend
npm install
npm run dev
```

默认端口：`3000`，开发代理 `/api -> http://localhost:8080`。

---

## 6. 生产一键部署（推荐）

### 6.1 配置环境参数

```bash
cp deploy/.env.prod.example deploy/.env.prod
vi deploy/.env.prod
```

至少修改以下字段：

- `DB_PASSWORD`
- `POSTGRES_ADMIN_PASSWORD`
- `JWT_SECRET`（长度 >= 32）
- `NGINX_SERVER_NAME`
- `APP_CORS_ALLOWED_ORIGINS`

### 6.2 首发全流程（预检 + 部署 + 冒烟）

```bash
bash deploy/first-release-run.sh deploy/.env.prod
```

该命令依次执行：

1. `first-release-precheck.sh`
2. `deploy.sh`
3. `e2e-smoke.sh`

---

## 7. 当前生产参数模板（sunfangfang.top）

当前仓库 `deploy/.env.prod` 的域名参数已设置为：

- `NGINX_SERVER_NAME=sunfangfang.top`
- `BASE_URL=http://sunfangfang.top`

建议将 CORS 配置为同时覆盖 HTTP/HTTPS（逗号分隔、不要末尾 `/`）：

```dotenv
APP_CORS_ALLOWED_ORIGINS=http://sunfangfang.top,https://sunfangfang.top
```

否则浏览器可能出现 `Invalid CORS request` 或登录无响应问题。

---

## 8. HTTPS 配置（手工）

一键脚本默认生成 HTTP 站点，HTTPS 需在服务器上补一层证书配置。

典型流程：

```bash
sudo apt-get update
sudo apt-get install -y certbot python3-certbot-nginx
sudo certbot --nginx -d sunfangfang.top -d www.sunfangfang.top
```

证书完成后再执行：

```bash
sudo nginx -t && sudo systemctl reload nginx
```

并同步更新：

- `BASE_URL=https://sunfangfang.top`
- `APP_CORS_ALLOWED_ORIGINS=http://sunfangfang.top,https://sunfangfang.top`

---

## 9. 冒烟验证清单（最小）

```bash
bash deploy/e2e-smoke.sh deploy/.env.prod
```

脚本会验证：

1. 后端直连健康检查
2. Nginx 反代健康检查
3. `admin/admin123` 登录拿 token
4. 受保护接口访问
5. 前端首页加载

---

## 10. 默认账号

数据库初始化脚本会创建默认管理员：

- 用户名：`admin`
- 密码：`admin123`

首次上线后请立即改密（可调用 `/api/auth/reset-password` 或在用户管理页修改）。

---

## 11. 常用运维命令

```bash
# 后端状态/日志
systemctl status capics-backend --no-pager
journalctl -u capics-backend -f

# Nginx
nginx -t
systemctl reload nginx

# 数据库备份
pg_dump -U capics_user capics > backup_$(date +%Y%m%d).sql
```

---

## 12. 关联文档

- `部署手册.md`：完整部署说明与故障排查
- `融合设计说明.md`：融合能力与计算逻辑变更说明
- `deploy/README.md`：一键部署脚本使用说明
- `deploy/首发检查清单.md`：服务器首发核验
- `deploy/端到端自测清单.md`：E2E 自测项
