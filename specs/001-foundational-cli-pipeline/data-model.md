# 数据模型：基础链路打通命令行管线 / Data Model

**特性 (Feature)**: 001-foundational-cli-pipeline | **阶段 (Phase)**: 1（设计）| **日期 (Date)**: 2026-08-05

本迭代 CLI-only，**无持久化存储**（不落库）。下列实体为运行时内存对象（用于模块间数据流与契约定义）；迭代 #2 服务化时再定义持久化 schema。

---

## 实体 / Entities

### Repository（仓库）
- `path: String`（必填）——本地代码仓库绝对路径
- `baselineBranch: String`（必填）——线上稳定版本分支
- `releaseBranch: String`（必填）——待发布分支
- **校验**：两分支须存在且有共同祖先（`git merge-base` 非空），否则错误（spec 边界场景）

### TestEnvServiceInstance（测试环境服务实例）
- `host: String`（必填）——Jacoco agent TCP 地址
- `port: int`（必填）——Jacoco agent TCP 端口
- `project / version / commit / taskId: String`——隔离维度（宪法 4.2）
- **校验**：仅测试环境（CLI 不校验环境类型本身，但要求显式绑定；生产/预发数据由使用流程保证不接入）

### IncrementalChange（增量变更）
- `file: String`——文件相对路径
- `changeType: enum {ADD, MODIFY}`（DELETE 不参与覆盖判定）
- `methodKey: MethodKey?`——AST 解析所得主键（可空 = 无法匹配 → 黄色）
- `changedLines: List<Int>`——变更行号（**仅报告可视化**）
- `executedLines: List<Int>`——`diff-cover` 判定已执行的变更行
- `verdict: enum {GREEN, RED, YELLOW, PARTIAL}`——判定

### MethodKey（方法主键，宪法 4.1）
- `className: String`——类全限定名
- `signature: String`——方法名 + 参数类型
- `route: String?`——Spring 路由（`@RequestMapping` 等路径，无则空）

### ExecutionData（执行数据）
- `source: TestEnvServiceInstance`
- `execBlob: byte[]`——Jacoco `.exec` 原始数据
- `coverageXml: String`——经 Jacoco report 转换的 coverage XML（供 `diff-cover`）
- `isolated: IsolationKey`——绑定 项目 / 版本 / commit / 任务 / 实例

### Verdict（判定，spec FR-003 / FR-013）
- 取值：`GREEN`（全执行）/ `RED`（从未执行）/ `YELLOW`（无法匹配）/ `PARTIAL`（部分执行，附行级明细）
- **三色语义固定**（宪法 V）；`PARTIAL` 为提示标记，不改三色语义

### CoverageReport（覆盖报告）
- `repository: Repository`
- `total / green / red / yellow / partial: int`——统计
- `changes: List<IncrementalChange>`——明细
- `format: enum {CONSOLE, HTML, JSON}`

---

## 状态转换（判定）/ State Transitions

变更 → AST 归约 → 行级执行比对 → 判定：

- 全部变更行已执行 → `GREEN`
- 无任何变更行执行 → `RED`
- 无法稳定 AST 归约 → `YELLOW`
- 部分变更行执行 → `PARTIAL`（附未覆盖行明细）

## 关系 / Relationships

```text
Repository  1 — *  IncrementalChange
IncrementalChange  * — 1?  MethodKey
ExecutionData  →  CoverageReport   (经 diff-cover + AstMatcher + VerdictEngine)
TestEnvServiceInstance  1 — 1  ExecutionData (source)
```
