# 功能规格说明：AST 精准匹配优化 / AST Precision Refinement

**功能分支 (Feature Branch)**: `004-ast-precision`

**创建日期 (Created)**: 2026-08-07

**状态 (Status)**: 草稿 (Draft)

**输入 (Input)**: 迭代 #4「AST 精准匹配优化」（宪法第六篇章）：增强 feature 001 的 AST 匹配——方法身份指纹、重构识别、路由解析增强，解决行号漂移/重构误判，降低 YELLOW/RED 误报。复用 feature 001 判定链路，仅增强 `AstMatcher`。

## 用户场景与测试 / User Scenarios & Testing *(mandatory)*

### 用户故事 1 - 方法身份指纹 (User Story 1, Priority: P1)

作为 QA，即使方法内部局部变量重命名或行内代码移位，codeq 仍识别为同一方法并正确判定覆盖（而非误判），且判定可复现。

**为何此优先级 (Why this priority)**：行号/局部变化不应改变方法身份；稳定指纹提升判定可信度，是「精准优化」的核心 MVP。

**独立测试 (Independent Test)**：方法内变量重命名（不改签名）→ codeq 仍识别该方法 → 覆盖判定不变。

**验收场景 (Acceptance Scenarios)**：

1. **给定 (Given)** 方法局部变量重命名，**当 (When)** 计算指纹，**那么 (Then)** 身份指纹不变，识别为同一方法。
2. **给定 (Given)** 相同输入，**当 (When)** 重复判定，**那么 (Then)** 结果一致（确定性，沿用 feature 01）。

---

### 用户故事 2 - 重构识别与标注 (User Story 2, Priority: P2)

作为研发 / QA，方法签名变更或方法移动（重构）被识别并标注「重构变更」，避免被误判为全新增漏测（RED）。

**为何此优先级 (Why this priority)**：重构但已被测试覆盖的方法不应误报 RED；降低误报是「精准」的关键。

**独立测试 (Independent Test)**：方法签名参数类型变更（int→long）+ 测试调用新签名 → codeq 标注重构 + 正确覆盖判定（非 RED）。

**验收场景 (Acceptance Scenarios)**：

1. **给定 (Given)** 方法签名变更 / 方法移动，**当 (When)** 判定，**那么 (Then)** 报告标注「重构变更」。
2. **给定 (Given)** 重构方法被执行，**当 (When)** 判定，**那么 (Then)** 不误报 RED。

---

### 用户故事 3 - 路由解析增强 (User Story 3, Priority: P3)

作为研发，Spring 路由（类级 `@RequestMapping` 继承、路径组合、路径变量）被完整解析，接口逻辑匹配更准。

**为何此优先级 (Why this priority)**：feature 01 仅基础路由解析；增强覆盖类级继承与路径变量场景，提升接口主键准确度。

**独立测试 (Independent Test)**：类级 `@RequestMapping("/api")` + 方法 `@GetMapping("/foo")` → 路由主键 = `/api/foo`。

**验收场景 (Acceptance Scenarios)**：

1. **给定 (Given)** 类级 + 方法级路由注解，**当 (When)** 解析，**那么 (Then)** 组合路径（类前缀 + 方法路径）。
2. **给定 (Given)** 路径变量 `@PathVariable`，**当 (When)** 展示，**那么 (Then)** 路由主键正确反映。

---

### 边界场景 / Edge Cases

- 方法整体删除 → 不参与判定（feature 01 已处理）。
- 大文件 AST 解析性能 → 属迭代 #6「性能治理」范畴，本迭代不优化。
- 非 Spring 路由（如 JAX-RS）→ 本迭代不支持（聚焦 Spring）。
- 极端重构（方法拆分/合并）→ 标注重构，人工复核。

## 需求 / Requirements *(mandatory)*

### 功能需求 / Functional Requirements

- **FR-001**：系统必须（MUST）为每个变更方法计算稳定身份指纹（基于 AST 结构，不受局部变量名 / 行号影响）。
- **FR-002**：系统必须（MUST）在方法局部变量重命名 / 行内移位时保持方法身份稳定，判定不变。
- **FR-003**：系统必须（MUST）识别方法签名变更 / 方法移动（重构），在报告标注「重构变更」。
- **FR-004**：系统必须（MUST）对重构方法正确判定覆盖（被执行则非 RED）。
- **FR-005**：系统必须（MUST）解析 Spring 类级 `@RequestMapping` 与方法级路由注解的组合路径。
- **FR-006**：系统必须（MUST）复用 feature 01 判定链路（diff / AST / diff-cover / verdict），仅增强 `AstMatcher`。
- **FR-007**：判定必须（MUST）保持确定性（相同输入相同输出）。

### 关键实体（增强 feature 01）/ Key Entities

- **MethodFingerprint**：方法指纹（AST 结构 hash）。
- **RefactorFlag**：重构标注（`SIGNATURE_CHANGE` / `METHOD_MOVE` / `NONE`）。
- **Route**：增强（组合路径 + 类级继承）。

## 成功标准 / Success Criteria *(mandatory)*

### 可度量结果 / Measurable Outcomes

- **SC-001**：方法局部变量重命名 → 判定不变（指纹稳定）。
- **SC-002**：重构方法被执行 → 不误报 RED。
- **SC-003**：类级 + 方法级路由 → 组合路径正确。
- **SC-004**：判定确定性（沿用 feature 01）。

## 假设 / Assumptions

- 复用 feature 01 核心链路，仅增强 `AstMatcher`（feature 01 包 `match/`）。
- 指纹算法、重构检测实现由 `plan.md` / `research.md` 定（spec 不指定）。
- 本迭代聚焦 Java + Spring 路由；非 Spring 框架不在范围。
