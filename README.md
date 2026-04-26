# CAPICS 产能评估系统

CAPICS 是面向汽车电子工厂的产能评估系统，覆盖主数据维护、MRP 计划管理、工艺路线、产线配置、静态产能核算、动态产能模拟与看板分析。

## 文档信息
- 更新日期：2026-04-26
- 文档版本：v1.3.0

## 版本记录
| 版本 | 日期 | 说明 |
|---|---|---|
| v1.3.0 | 2026-04-26 | 同步最新系统能力：MRP对比AI综合分析（互联网+系统内）、静默启动入口、本地启停脚本修复与文档更新。 |
| v1.2.2 | 2026-04-25 | 统一补齐文档更新日期与版本记录。 |
技术栈
- 前端：Vue 3 + Vite
- 后端：Spring Boot 2.7 + Spring Security + JPA
- 数据库：PostgreSQL
- 部署：Nginx + systemd + `deploy/deploy.sh`
- 后端代码：无 Lombok 实现（规避新 JDK 与 Lombok 兼容风险）
## 目录结构
```text
/opt/capics
├─ backend/                  # Spring Boot 后端
├─ capics-frontend/          # Vue 前端
├─ deploy/                   # 部署脚本与环境模板
├─ docs/                     # 专项设计文档
├─ 接口明细表.md
├─ 前端调用映射表.md
├─ 部署手册.md
└─ README.md
```

## 前端路由与菜单
- 登录：`/login`
- 看板：`/`（仪表盘）、`/mrp-compare`（MRP 对比）、`/product-line`（生产线一览）
- 产能分析：`/mrp`、`/capacity-assessment`、`/capacity-assessment-monthly`、`/capacity-realtime`、`/capacity-realtime-monthly`、`/fusion-workbench`
- 主数据：`/products`、`/routing`、`/ct-line`、`/lines`
- 系统：`/users`

## 核心能力
- MRP 导入（v2 模板，Sheet=`Data`，必填列：`Item Number*`、`Release Date*`、`Due Date*`、`Quantity*`、`Reference*`）
- 静态产能核算（周/月）
- 动态产能模拟（周/月）
- 产线-产品（CT-line）导入、异步导入进度查询、页面增改
- MRP 对比（按产品描述聚合后做周/月差异矩阵）
- MRP 对比 AI 差异分析（融合互联网信号与系统内产能风险）
- 快照管理（动态快照 + 静态结果版本）

## 重要变更（已落地）
### 1. 静态结果保存机制（413 根治）
静态产能保存改为“元数据保存 + 后端重算”：
- 前端保存时仅提交：`createdBy/fileName/version/snapshotName/source=static/dimension`
- 后端收到后按条件重算并入库
- 避免前端提交大 `linesData` 导致 `HTTP 413`

### 2. 部署标准路径（服务器）
统一使用：
- 代码根目录：`/opt/capics`
- 后端：`/opt/capics/backend`
- 前端：`/opt/capics/capics-frontend`
- 部署脚本：`/opt/capics/deploy`

### 3. MRP 对比 AI 分析增强
- 后端调用 AI 前会组合两类上下文：
  - 互联网信号：汽车市场行情、汽车电子供应链风险（运行时抓取）
  - 系统内信号：MRP 变化率、差异集中度、周期冲击度、产能风险评分
- AI 结果区支持“结论 + 证据来源（互联网/系统内）”双栏展示。

### 4. 本地启动/停止脚本增强
- 新增静默启动入口（无控制台窗口）：
  - `deploy/local-start-all-silent.bat`
  - `deploy/local-start-all-silent.vbs`
- `deploy/local-stop.ps1` 已修复：
  - 兼容 `.tmp/*.pid` 与 `.tmp/pids/*.pid`
  - PID 失效时按端口兜底停止（后端 `8080` / 前端 `3000,5173`）

## 本地开发
### 后端
```bash
cd backend
mvn -DskipTests clean package
mvn spring-boot:run
```
默认端口：`8080`

### 前端
```bash
cd capics-frontend
npm install
npm run dev
```
默认端口：`3000`

## 一键部署（生产）
```bash
cp deploy/.env.prod.example deploy/.env.prod
# 编辑 deploy/.env.prod
bash deploy/deploy.sh deploy/.env.prod
```

推荐首发流程：
```bash
bash deploy/first-release-run.sh deploy/.env.prod
```

## 快速验证
```bash
bash deploy/e2e-smoke.sh deploy/.env.prod
bash deploy/e2e-full-smoke.sh deploy/.env.prod
```

## 默认账号
- 用户名：`admin`
- 密码：`admin123`

首次上线后请立即修改密码。

## 相关文档
- [接口明细表.md](./接口明细表.md)
- [前端调用映射表.md](./前端调用映射表.md)
- [部署手册.md](./部署手册.md)
- [deploy/README.md](./deploy/README.md)

