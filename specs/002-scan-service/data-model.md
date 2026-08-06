# 数据模型：扫描服务化 / Data Model

**特性 (Feature)**: 002-scan-service | **阶段 (Phase)**: 1（设计）| **日期 (Date)**: 2026-08-06

本 feature 引入持久化（JPA + PostgreSQL / H2）。下列为实体与表结构。

## 实体 / Entities

### ScanTask（扫描任务）— 表 `scan_task`
- `id`：UUID（主键）
- `repo`：String（仓库路径）
- `baseline`：String
- `release`：String
- `jacocoHost`：String?、`jacocoPort`：Integer?
- `coverageXmlPath`：String?（本地 coverage 输入，与 Jacoco 端点二选一）
- `status`：enum `{PENDING, RUNNING, SUCCESS, FAILED}`
- `isolationKey`：String（序列化 项目 / 版本 / commit / 任务 / 实例）
- `traceId`：String（贯穿异步日志，宪法 VIII）
- `errorMsg`：String?（FAILED 时原因）
- `createdAt`：Instant、`startedAt`：Instant?、`finishedAt`：Instant?

### ScanResult（扫描结果）— 表 `scan_result`
- `id`：UUID（主键）
- `task`：`@OneToOne ScanTask`（外键 taskId）
- `pass`：boolean（门禁判定；存在 RED → false）
- `green` / `red` / `yellow` / `partial`：int（三色统计）
- `changes`：String（JSON：`IncrementalChange` 列表，含 file / methodKey / verdict / uncoveredLines）
- `createdAt`：Instant

### 关系 / Relationships
```
ScanTask  1 — 0..1  ScanResult
```

## 状态转换 / State Transitions

```
PENDING → RUNNING → SUCCESS   （有 ScanResult）
                  ↘ FAILED    （记录 errorMsg）
```

## 校验规则 / Validation

- `repo` / `baseline` / `release` 必填。
- `jacocoHost`+`jacocoPort` 或 `coverageXmlPath` 至少其一。
- 状态机：仅允许 `PENDING → RUNNING → SUCCESS/FAILED`。

## TTL（宪法 4.3）

`scan_task` / `scan_result` 按 `createdAt` 过期（`@Scheduled` 清理）；阈值可配置（如 30 天）。
