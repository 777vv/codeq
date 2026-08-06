# REST API 契约：扫描服务 / API Contract

**特性 (Feature)**: 002-scan-service | **阶段 (Phase)**: 1 | **日期 (Date)**: 2026-08-06

## 通用 / General
- Content-Type: `application/json`
- 路径前缀 `/api`；时间 ISO-8601
- 诊断日志携带 `traceId`（响应头 `X-Trace-Id`，宪法 VIII）

## POST /api/scans — 提交扫描任务（US1）

**Request**
```json
{ "repo": "/path/to/sample", "baseline": "baseline", "release": "release",
  "jacocoHost": "127.0.0.1", "jacocoPort": 6300,
  "coverageXmlPath": null,
  "taskId": null }
```
（`jacocoHost+port` 与 `coverageXmlPath` 二选一；`taskId` 可选自定义）

**Response 202**
```json
{ "taskId": "uuid", "status": "PENDING", "traceId": "..." }
```

## GET /api/scans/{id} — 任务状态与元数据

**Response 200**
```json
{ "taskId", "status", "repo", "baseline", "release",
  "createdAt", "startedAt", "finishedAt", "errorMsg" }
```

## GET /api/scans/{id}/result — 判定结果（US1）

**Response 200**
```json
{ "taskId", "pass",
  "totals": {"green", "red", "yellow", "partial"},
  "changes": [{"file", "methodKey": {"className","signature","route"}, "verdict", "uncoveredLines": []}] }
```
**Response 409**：任务未完成（`status != SUCCESS`）。

## GET /api/scans — 历史列表（US2）

**Query**：`?project=&version=&from=&to=&status=&page=&size=`

**Response 200**
```json
{ "items": [ {"taskId","status","repo","baseline","release","totals","createdAt","finishedAt"} ],
  "page": 0, "size": 20, "total": 42 }
```

## GET /api/scans/{id}/verdict — 门禁判定（US3）

**Response 200**
```json
{ "pass": false, "totals": {"green": 3, "red": 1, "yellow": 1, "partial": 0} }
```

## 错误码 / Errors
- `400`：参数校验失败
- `404`：任务不存在
- `409`：状态不允许（如结果未就绪）
- `500`：内部错误（日志带 `traceId`）

## 不变量（契约约束）
- 执行数据来源校验（FR-010）：非测试环境 / 版本不匹配 → 任务 `FAILED`。
- 复用 feature 001 判定逻辑（FR-008）：API 结果与 `codeq check` 一致。
- 存在 `RED` → `verdict.pass = false`（宪法红线）。
