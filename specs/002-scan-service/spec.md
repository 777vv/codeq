# 功能规格说明：扫描服务化（核心接口服务化）/ Scan Service

**功能分支 (Feature Branch)**: `002-scan-service`

**创建日期 (Created)**: 2026-08-06

**状态 (Status)**: 草稿 (Draft)

**输入 (Input)**: 迭代 #2「核心接口服务化」（宪法第六篇章）：将 feature 001 的命令行核心判定链路封装为后端通用 HTTP 接口，扫描任务**全异步化**执行，扫描与判定结果**持久化**，支持历史回溯与发布门禁查询。CLI 形态保留，核心判定逻辑复用。

## 用户场景与测试 / User Scenarios & Testing *(mandatory)*

### 用户故事 1 - 异步扫描 API (User Story 1, Priority: P1)

作为发布工程师 / CI 流水线，我希望通过 HTTP 提交一次增量覆盖扫描（仓库 + 基准 / 待发布分支 + 测试环境 Jacoco 端点），系统异步执行并返回任务 ID，我可轮询获取三色判定结果——这样无需命令行、可被发布流水线与下游系统调用。

**为何此优先级 (Why this priority)**：这是 feature 001（CLI）演进为服务的关键一步，使平台能力可被系统/流水线集成，是迭代 #2 的核心 MVP。

**独立测试 (Independent Test)**：提交扫描请求 → 收到任务 ID → 轮询至完成 → 获取三色判定（与 feature 001 `codeq check` 等价）。

**验收场景 (Acceptance Scenarios)**：

1. **给定 (Given)** 一个有效扫描请求，**当 (When)** 提交，**那么 (Then)** 返回任务 ID 与状态 `PENDING`。
2. **给定 (Given)** 任务已提交，**当 (When)** 查询状态，**那么 (Then)** 返回 `PENDING` / `RUNNING` / `SUCCESS` / `FAILED` 之一。
3. **给定 (Given)** 任务执行完成，**当 (When)** 查询结果，**那么 (Then)** 返回三色统计 + 明细（与 `codeq check` 一致）。
4. **给定 (Given)** 执行数据来自非测试环境或版本不匹配，**当 (When)** 扫描，**那么 (Then)** 任务 `FAILED` 或结果被拒绝（沿用 feature 001 FR-007）。

---

### 用户故事 2 - 持久化与历史回溯 (User Story 2, Priority: P2)

作为 QA / 研发，扫描任务与判定结果被持久化，我能按 项目 / 版本 / 时间 回溯历史扫描，沉淀质量追溯数据。

**为何此优先级 (Why this priority)**：宪法能力边界「支持历史扫描任务、覆盖记录回溯」；多版本质量可追溯，是服务的价值沉淀。

**独立测试 (Independent Test)**：跑多次扫描 → 按项目/版本查询历史列表 → 每条含时间、分支、三色统计、状态；可查单条完整明细。

**验收场景 (Acceptance Scenarios)**：

1. **给定 (Given)** 多次扫描已完成，**当 (When)** 按项目/版本查询历史，**那么 (Then)** 返回列表，每条含任务元数据 + 三色统计。
2. **给定 (Given)** 某任务 ID，**当 (When)** 查询详情，**那么 (Then)** 返回完整判定明细。

---

### 用户故事 3 - 标准化门禁判定查询 (User Story 3, Priority: P3)

作为发布平台，我通过标准接口查询某次扫描的合规判定（可上线 / 拦截 + 三色统计），用于自动拦截漏测代码上线（为迭代 #5 门禁闭环铺垫）。

**为何此优先级 (Why this priority)**：发布平台需要一个简单明确的「是否可上线」判定；本故事为迭代 #5「发布门禁闭环」提供查询基础。

**独立测试 (Independent Test)**：完成扫描 → 查询门禁判定 → 返回 `{pass, totals}`；存在 RED → `pass=false`。

**验收场景 (Acceptance Scenarios)**：

1. **给定 (Given)** 一个已完成的扫描，**当 (When)** 查询门禁判定，**那么 (Then)** 返回 `{pass, totals{green,red,yellow,partial}}`。
2. **给定 (Given)** 存在 RED 的扫描，**当 (When)** 查询门禁判定，**那么 (Then)** `pass=false`（默认禁止上线，宪法红线）。

---

### 边界场景 / Edge Cases

- 扫描请求参数缺失 / 无效 → 拒绝（`400` 类错误）。
- 同一任务重复提交 → 幂等语义明确（返回已存在任务或新任务，须定义）。
- 异步执行失败（Jacoco 不可达、`diff-cover` 缺失、git 失败）→ 状态 `FAILED` + 错误原因。
- 大仓库扫描耗时长 → 异步不阻塞调用方，状态 `RUNNING`。
- 历史数据按 TTL 过期清理（宪法 4.3）。

## 需求 / Requirements *(mandatory)*

### 功能需求 / Functional Requirements

- **FR-001**：系统必须（MUST）提供 HTTP 接口提交扫描任务（仓库 / 基准分支 / 待发布分支 / 测试环境 Jacoco 端点），返回任务 ID。
- **FR-002**：系统必须（MUST）异步执行扫描全流程（复用 feature 001 核心判定链路），不阻塞调用方。
- **FR-003**：系统必须（MUST）提供任务状态查询（`PENDING` / `RUNNING` / `SUCCESS` / `FAILED`）。
- **FR-004**：系统必须（MUST）提供任务结果查询（三色统计 + 明细，与 `codeq check` 等价）。
- **FR-005**：系统必须（MUST）持久化任务元数据与判定结果。
- **FR-006**：系统必须（MUST）提供历史扫描列表查询（按 项目 / 版本 / 时间 过滤）。
- **FR-007**：系统必须（MUST）提供标准化门禁判定查询（`pass` + 三色统计），存在 `RED` 时 `pass=false`。
- **FR-008**：系统必须（MUST）复用 feature 001 的核心判定逻辑（增量 / AST / 三色 / 版本校验 / 隔离），保持宪法合规。
- **FR-009**：系统必须（MUST）对扫描任务数据设置 TTL 过期清理（宪法 4.3）。
- **FR-010**：系统必须（MUST）拒绝非测试环境 / 版本不匹配的执行数据（沿用 feature 001 FR-007）。
- **FR-011**：诊断日志必须（MUST）经 SLF4J（宪法 VIII），携带 `traceId`（MDC）。

### 关键实体 / Key Entities *(include if feature involves data)*

- **ScanTask（扫描任务）**：任务 ID、仓库、基准 / 待发布分支、Jacoco 端点、状态、创建时间、隔离键。
- **ScanResult（扫描结果）**：任务 ID、三色统计、变更明细列表、门禁判定。
- **Verdict（门禁判定）**：`pass` + 三色统计。
- 注：持久化 schema、字段细节留 `data-model.md` / `plan.md`。

## 成功标准 / Success Criteria *(mandatory)*

### 可度量结果 / Measurable Outcomes

- **SC-001**：发布工程师通过单次 HTTP 调用提交扫描并立即获得任务 ID，无需命令行。
- **SC-002**：异步扫描不阻塞调用方（提交即返回，执行在后台）。
- **SC-003**：API 判定结果与 feature 001 `codeq check` 一致（同一输入相同判定）。
- **SC-004**：历史扫描可按 项目 / 版本 / 时间 回溯。
- **SC-005**：存在 `RED` 的扫描，门禁判定 `pass=false`。

## 假设 / Assumptions

- 复用 feature 001 的核心域逻辑（diff / AST / diff-cover / verdict / report）封装为服务层（模块化决策见 `plan.md`）。
- 数据存储选型、异步任务机制、API 认证为技术决策，由 `plan.md` / `research.md` 定（spec 不指定具体实现）。
- 初始范围仍面向 Java 业务项目（沿用 feature 001）。
- API 形态：HTTP / JSON（REST 细节见 `plan.md` / `contracts/`）。
- CLI（feature 001）保留可用，本 feature 不替换它。
