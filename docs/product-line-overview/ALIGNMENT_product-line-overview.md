# ALIGNMENT_product-line-overview

## 原始需求

- 产线一览页面未开发，需要重新开发。
- 页面以图表方式展示产线的产能利用情况。
- 每个生产线代码前三位代表工艺段。
- 页面需要支持按不同工艺段筛选。
- 页面分别展示该工艺段下所有产线的月、周产能汇总情况。
- 本轮先做原型确认，再开始正式修改代码。

## 当前项目上下文

### 页面与路由现状

- 路由 `/product-line` 已存在。
- 视图文件 [ProductLineView.vue](E:\TraeProject\Capacity\capics_V1\capics-frontend\src\views\ProductLineView.vue) 目前仍是占位页面。
- 左侧导航和顶部标题已经把该页定义为“产线一览”。

### 可复用数据源

- 周静态产能核算接口：
  - [capacity.js](E:\TraeProject\Capacity\capics_V1\capics-frontend\src\api\capacity.js)
  - `GET /api/capacity-assessment`
- 月静态产能核算接口：
  - [capacityMonthly.js](E:\TraeProject\Capacity\capics_V1\capics-frontend\src\api\capacityMonthly.js)
  - `GET /api/capacity-assessment/monthly`
- 产线名称接口：
  - [line.js](E:\TraeProject\Capacity\capics_V1\capics-frontend\src\api\line.js)
  - `GET /api/lines`

### 数据结构理解

- 周/月静态核算接口都返回 `lines`，格式为 `lineCode -> 明细行数组`。
- 周接口返回 `weeks`、`weekDates`。
- 月接口返回 `months`、`monthDates`。
- 每条明细行包含：
  - `itemNumber`
  - `description`
  - `componentNumber`
  - `shiftOutput`
  - `shiftWorkers`
  - `ct`
  - `oee`
  - `<week>_demand / <week>_loading`
  - `<month>_demand / <month>_loading`
- 因此本页不必新增后端接口，前端即可按 `lineCode.substring(0, 3)` 聚合工艺段。

## 任务边界

### 本轮范围

- 输出“产线一览”页的 `Align` 和 `Architect` 文档。
- 明确页面目标、数据来源、分层布局、交互流和图表原型。
- 明确正式实现时是否优先复用现有静态核算接口。

### 明确不在本轮范围

- 不修改前端页面代码。
- 不新增后端接口。
- 不处理动态模拟数据。
- 不处理导出、快照保存、权限细化等增强需求。
- 不做移动端之外的复杂交互探索。

## 需求理解

我当前对“产线一览”的理解如下：

1. 这是一个汇总分析页，不是明细录入页。
2. 页面核心价值是“按工艺段看所有产线的周/月利用率状态”，而不是查看单条产线完整明细表。
3. 工艺段来源不是单独字段，而是产线代码前三位。
4. 页面至少应包含：
   - MRP 版本筛选
   - 工艺段筛选
   - 周汇总视图
   - 月汇总视图
   - 工艺段下产线分布图
5. 图表应优先回答三个问题：
   - 当前工艺段整体利用率高不高
   - 哪些产线负荷最高/最低
   - 哪些周或月份出现超载或明显闲置

## 当前工作共识

在未收到新的相反要求前，先按以下共识进入原型设计：

- 数据口径采用“静态产能核算”结果，不接动态模拟。
- 页面以“工艺段总览”作为第一层视图，以“产线分布”作为第二层视图。
- 页面按“周、月分开展示”设计，不混在同一分析区内。
- 利用率主指标采用现有 `loading` 数值，展示时按百分比语义表达：
  - `1.00 = 100%`
  - `0.85 = 85%`
  - `1.12 = 112%`

## 已确认决策

用户已确认以下决策：

1. 页面按“周、月分开展示”走。
2. 不保留“异常提示 / 汇总明细”区。
3. 正式实现时允许新增图表库。

## 风险与待确认点

以下问题不会阻止原型输出，但会影响正式实现细节：

1. 利用率阈值是否统一按 `100%` 判定超载，还是不同工艺段有不同红线。
2. 是否需要保留“全部工艺段”视图，展示跨工艺段横向对比。

## 当前结论

基于现有项目结构和接口能力，可以直接在 `/product-line` 上构建一个“工艺段维度的周/月产能利用率分析页”，无需新增后端接口，下一步进入架构与原型设计。
