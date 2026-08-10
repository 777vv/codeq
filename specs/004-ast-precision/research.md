# 研究：AST 精准匹配优化 / Research

**特性 (Feature)**: 004-ast-precision | **阶段 (Phase)**: 0（研究）| **日期 (Date)**: 2026-08-07
**输入 (Input)**: [plan.md](plan.md) 技术上下文与宪法校验

本 feature 增强 feature 01 的 `AstMatcher`。技术决策如下（无 NEEDS CLARIFICATION 遗留）。

## 1. 方法身份指纹：AST normalized hash

**Decision**：用 JavaParser 解析方法体，**规范化**（局部变量名 → 占位、忽略行号 / 空白 / 注释 / 导入顺序），对规范化 AST 结构计算稳定 hash（SHA-256）。
**Rationale**：规范化后，局部变量重命名 / 行内移位不改变结构 → hash 稳定，满足 FR-001 / FR-002。
**Alternatives**：
- 签名 hash（只方法签名）：太粗，方法体改动识别不出。
- 全文本 hash：变量重命名即变，不稳。**否决**。
- 调用图 hash：更稳但复杂，MVP 用结构 hash，后续可增强。

## 2. 重构检测：baseline vs release 方法指纹匹配

**Decision**：解析 baseline 与 release 两份源码的方法集（指纹 + 签名），匹配——若 release 方法在 baseline 无同签名，但有相近指纹（结构相似）→ 标注 `SIGNATURE_CHANGE` / `METHOD_MOVE`。
**Rationale**：跨版本方法身份追踪，识别重构（FR-003 / FR-004），避免误报 RED。
**Alternatives**：
- 仅按签名匹配：签名变即失配，无法识别「重构同一方法」。**否决**。
- 机器学习相似度：过重，MVP 用结构相似度（hash 前缀匹配或归一化编辑距离）。

## 3. 路由组合：类级 + 方法级

**Decision**：解析类级 `@RequestMapping` 的 `path`/`value` 作前缀，与方法级 `@GetMapping` / `@PostMapping` 等的 `path`/`value` 拼接（斜杠归一：`/api` + `/foo` → `/api/foo`）。
**Rationale**：feature 01 仅取方法或类单级；组合覆盖 RESTful 真实路径（FR-005）。
**Alternatives**：仅方法级（feature 01 现状）——丢失类前缀。**否决**。

## 4. 复用 feature 01

**Decision**：在 `com.codeq.match` 增强现有 `AstMatcher` + 新增 `MethodFingerprinter` / `RefactorDetector`；`model` 加 `MethodFingerprint` / `RefactorFlag`；`diff` / `diffcover` / `verdict` 不改。
**Rationale**：FR-006，仅增强 AST 匹配层。
**Alternatives**：重写——违反复用。**否决**。

## 结论 / Summary

所有决策符合宪法（III AST 精准匹配、迭代 #4），复用 feature 01，无 NEEDS CLARIFICATION。可进入 Phase 1。
