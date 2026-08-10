# AstMatcher 增强契约（内部）/ Internal Contract

**特性 (Feature)**: 004-ast-precision | **阶段 (Phase)**: 1 | **日期 (Date)**: 2026-08-07

本 feature 增强**内部组件** `AstMatcher`（`com.codeq.match`），无新外部接口——CLI / REST / 前端契约沿用 feature 01 / 02 / 03。

## 增强点

- `AstMatcher.mapLinesToMethods`：`MethodKey.route` 改为**组合路径**（类级 + 方法级，US3）。
- 新增 `MethodFingerprinter.fingerprint(MethodDeclaration) → MethodFingerprint`（结构 hash，US1）。
- 新增 `RefactorDetector.detect(baselineMethods, releaseMethods) → Map<MethodKey, RefactorFlag>`（US2）。

## 不变量 / Invariants

- 指纹确定性：相同 AST → 相同 hash（FR-007）。
- 路由组合斜杠归一。
- 复用 feature 01 判定链路（`diff` / `diffcover` / `verdict` 不改，FR-006）。
- 重构标注不影响三色语义（重构方法被执行 → 非 RED）。
