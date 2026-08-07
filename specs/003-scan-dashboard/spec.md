# 功能规格说明：扫描可视化看板（前端可视化落地）/ Scan Dashboard

**功能分支 (Feature Branch)**: `003-scan-dashboard`

**创建日期 (Created)**: 2026-08-06

**状态 (Status)**: 草稿 (Draft)

**输入 (Input)**: 迭代 #3「前端可视化落地」（宪法第六篇章）：为 feature 002 的扫描服务构建 Web 界面（Vue3 + Monaco Editor），支持分支选择、扫描进度、三色 Diff 可视化、报告导出。前端复用后端 REST API，不重复判定逻辑。

## 用户场景与测试 / User Scenarios & Testing *(mandatory)*

### 用户故事 1 - 扫描提交与进度 (User Story 1, Priority: P1)

作为发布工程师 / QA，我在 Web 页面选择代码仓库、基准分支、待发布分支（+ 测试环境 Jacoco 端点），点击「开始扫描」，页面提交到后端并实时展示扫描状态（PENDING / RUNNING / SUCCESS / FAILED），完成后展示三色统计摘要。

**为何此优先级 (Why this priority)**：让非命令行用户也能用浏览器完成扫描并可视化进度，是「可视化落地」的核心 MVP，替代 curl/CLI 交互。

**独立测试 (Independent Test)**：页面填表提交 → 状态轮询至 SUCCESS → 显示绿/红/黄/partial 统计。

**验收场景 (Acceptance Scenarios)**：

1. **给定 (Given)** 扫描表单，**当 (When)** 填写 repo/baseline/release/Jacoco 端点并提交，**那么 (Then)** 调用 `POST /api/scans` 并展示任务 ID + 状态。
2. **给定 (Given)** 任务已提交，**当 (When)** 轮询状态，**那么 (Then)** 实时更新为 PENDING/RUNNING/SUCCESS/FAILED。
3. **给定 (Given)** 扫描成功，**当 (When)** 查看摘要，**那么 (Then)** 展示三色统计（绿/红/黄/partial）。
4. **给定 (Given)** 扫描失败，**当 (When)** 查看详情，**那么 (Then)** 展示错误原因。

---

### 用户故事 2 - 三色 Diff 可视化 (User Story 2, Priority: P2)

作为研发 / QA，我用 Monaco Editor 查看增量变更代码，变更按三色高亮（绿=已覆盖、红=漏测、黄=待复核、partial=部分），并可定位漏测方法。

**为何此优先级 (Why this priority)**：直观定位高危漏测代码，对齐主流编辑器体验（宪法 4.3 Monaco Editor）。

**独立测试 (Independent Test)**：扫描完成 → 打开结果页 → Monaco 展示变更文件 + 三色高亮。

**验收场景 (Acceptance Scenarios)**：

1. **给定 (Given)** 扫描结果，**当 (When)** 打开结果页，**那么 (Then)** Monaco Editor 展示每个变更文件，变更行按 verdict 着色。
2. **给定 (Given)** 多个变更，**当 (When)** 按颜色过滤（如只看红色），**那么 (Then)** 仅显示对应变更。
3. **给定 (Given)** 变更方法，**当 (When)** 查看，**那么 (Then)** 显示方法主键（类名 + 方法签名 + 路由）。

---

### 用户故事 3 - 报告导出 (User Story 3, Priority: P3)

作为 QA，我将扫描结果导出为 HTML 报告（三色 Diff + 统计），供离线存档与分享。

**为何此优先级 (Why this priority)**：离线追溯、邮件分享、审计留档；与 feature 001 CLI 的 HTML 报告形态对齐。

**独立测试 (Independent Test)**：结果页点「导出」→ 下载 HTML 报告 → 含统计 + 三色 Diff。

**验收场景 (Acceptance Scenarios)**：

1. **给定 (Given)** 已完成的扫描结果，**当 (When)** 点击导出，**那么 (Then)** 生成并下载 HTML 报告。
2. **给定 (Given)** 导出的 HTML，**当 (When)** 离线打开，**那么 (Then)** 完整显示三色统计与变更明细。

---

### 边界场景 / Edge Cases

- 扫描中网络断开 → 状态轮询失败，提示重试。
- 大量变更文件 → Diff 列表分页或懒加载。
- 空结果（零增量变更）→ 友好提示「无增量变更」。
- 浏览器不支持 Monaco → 降级为纯文本展示。

## 需求 / Requirements *(mandatory)*

### 功能需求 / Functional Requirements

- **FR-001**：前端必须（MUST）提供 Web 页面，让用户填写扫描参数（repo / baseline / release / Jacoco 端点）并提交（调用 `POST /api/scans`）。
- **FR-002**：前端必须（MUST）实时展示扫描状态（轮询 `GET /api/scans/{id}` 至终态）。
- **FR-003**：SUCCESS 时前端必须（MUST）展示三色统计摘要（绿/红/黄/partial 计数）。
- **FR-004**：FAILED 时前端必须（MUST）展示错误原因。
- **FR-005**：前端必须（MUST）用 Monaco Editor 展示增量变更，按三色高亮。
- **FR-006**：前端必须（MUST）支持按颜色过滤变更（如只看红色漏测）。
- **FR-007**：前端必须（MUST）显示变更方法主键（类名 + 方法签名 + 路由）。
- **FR-008**：前端必须（MUST）支持导出 HTML 报告（含三色统计 + 变更明细）。
- **FR-009**：前端必须（MUST）调用 feature 002 REST API（复用后端，不重复判定逻辑）。
- **FR-010**：前端构建产物必须（MUST）可由后端服务托管（前后端不分离，宪法 4.3）。

### 关键实体（消费 feature 02 API）/ Key Entities

- **ScanForm**：提交表单（repo/baseline/release/jacoco 端点）。
- **ScanStatus**：轮询所得任务状态。
- **ChangeItem**：变更明细（file / methodKey / verdict / uncoveredLines）。

## 成功标准 / Success Criteria *(mandatory)*

### 可度量结果 / Measurable Outcomes

- **SC-001**：用户通过浏览器完成一次扫描全流程，无需命令行或 curl。
- **SC-002**：扫描状态实时更新至终态（提交 → 终态可见）。
- **SC-003**：三色统计与后端 API 返回一致。
- **SC-004**：Monaco Diff 三色高亮正确反映 verdict。
- **SC-005**：导出的 HTML 报告含完整统计与变更明细。

## 假设 / Assumptions

- 复用 feature 002 REST API（`POST/GET /api/scans`），前端不实现判定逻辑。
- 前端技术栈 **Vue3 + Monaco Editor**（宪法 4.3 强制）。
- 前后端不分离：Vue 构建产物由 Spring Boot 静态资源托管（或同仓库），具体集成由 `plan.md` 决策。
- 构建工具链（Vite / npm）为技术决策，由 `plan.md` / `research.md` 定（spec 不指定）。
- 鉴权沿用 feature 002（MVP 内网信任）。
