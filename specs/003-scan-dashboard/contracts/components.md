# 组件契约：扫描可视化看板 / Component Contract

**特性 (Feature)**: 003-scan-dashboard | **阶段 (Phase)**: 1 | **日期 (Date)**: 2026-08-06

前端组件对外（路由 / 父组件）的契约。后端 API 契约复用 feature 02（见 [../../002-scan-service/contracts/api.md](../../002-scan-service/contracts/api.md)）。

## 路由 / Routes

- `GET /` → `ScanFormView`：扫描提交表单 + 进度
- `GET /scans/:id` → `ResultView`：三色 Diff + 导出

## 组件 / Components

### ScanForm（US1）
- **输入**：无（顶层表单）
- **输出**：`emit('submit', form: ScanForm)`
- **校验**：repo / baseline / release 必填；jacocoHost+port 或 coverageXmlPath 至少其一

### ScanProgress（US1）
- **props**：`taskId: string`
- **行为**：轮询 `GET /api/scans/{id}`（每 2s）至终态；`emit('done', status, result?)`、`emit('failed', errorMsg)`
- **展示**：状态徽标；SUCCESS 时展示三色统计

### DiffViewer（US2）
- **props**：`changes: ChangeItem[]`
- **行为**：Monaco 展示变更文件，按 verdict 着色；颜色过滤器（green / red / yellow / partial / all）
- **展示**：方法主键（className + signature + route）

### ExportButton（US3）
- **props**：`result: ResultView`
- **行为**：点击 → 拼 HTML（统计 + 三色 Diff）→ 下载 `report-<taskId>.html`

## 不变量 / Invariants

- 仅调 feature 02 REST API（FR-009）
- 三色高亮严格对应 verdict（绿 / 红 / 黄 / partial）
- 导出 HTML 含完整统计与变更明细
