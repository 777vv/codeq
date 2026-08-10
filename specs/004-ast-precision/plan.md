# 实施计划：AST 精准匹配优化 / Implementation Plan: AST Precision Refinement

**分支 (Branch)**: `004-ast-precision` | **日期 (Date)**: 2026-08-07 | **规格 (Spec)**: [spec.md](spec.md)

**输入 (Input)**: 来自 `specs/004-ast-precision/spec.md` 的功能规格

**注 (Note)**: 本文件由 `/speckit-plan` 命令填写。

## 摘要 / Summary

增强 feature 01 的 `AstMatcher`（迭代 #4）：方法身份指纹（规范化 AST 的 SHA-256，局部变量重命名/行内移位身份稳定）、重构检测（baseline vs release 方法匹配，标注签名变更/移动，避免误报 RED）、路由组合（类级 `@RequestMapping` + 方法级注解）。复用 feature 01 判定链路，仅增强 `match` 层 + 新增 model 值对象。

## 技术上下文 / Technical Context

**语言/版本 (Language/Version)**: Java 21（宪法 4.3）

**主依赖 (Primary Dependencies)**:
- `javaparser-core`（已有，feature 01）—— 指纹/重构/路由解析均基于 JavaParser AST
- 复用 feature 01 全部核心 `@Component`（`diff` / `diffcover` / `verdict` / `coverage` 不改）

**存储 (Storage)**: N/A（内存值对象，无持久化）

**测试 (Testing)**: JUnit 5

**目标平台 (Target Platform)**: JVM（CLI / 服务共用）

**项目类型 (Project Type)**: library（`match` 层增强，被 feature 01 判定链路调用）

**构建 (Build)**: Maven

**约束 (Constraints)**:
- 复用 feature 01 判定链路（FR-006），仅增强 `match`
- 指纹确定性（相同 AST → 相同 hash，FR-007）
- 三色语义不变（重构方法被执行 → 非 RED）
- 新 Java 类须过门禁 + 类头（宪法 VII/IX）

**规模/范围 (Scale/Scope)**: 增强 `com.codeq.match` + 新增 model 值对象；非 Spring 框架、大文件性能（迭代 #6）不在范围。

## 宪法校验 / Constitution Check

*门禁 (GATE): Phase 0 研究前通过；Phase 1 设计后复检。*

| 宪法原则 | 校验 | 状态 |
|---|---|---|
| III. AST 精准匹配 | 本 feature 强化 AST（指纹 + 重构 + 路由组合） | ✅ 通过 |
| 复用 feature 01（FR-006） | 仅增强 `match`，`diff`/`diff-cover`/`verdict` 不改 | ✅ 通过 |
| 确定性（FR-007） | 指纹 SHA-256 确定性；相同输入相同输出 | ✅ 通过 |
| 迭代顺序 第六篇 | 本 feature = 迭代 #4「AST 精准匹配优化」 | ✅ 通过 |
| VII. 代码门禁 | 新 Java 类须过 fmt / PMD / SpotBugs | ✅ 通过 |
| VIII. 日志 | SLF4J（match 层日志） | ✅ 通过 |
| IX. 类头 | 新类 `@author wangtao` + `@date` | ✅ 通过 |

**结论**: 无违规，门禁通过。**无需 Complexity Tracking 豁免。**

## 项目结构 / Project Structure

### 本特性文档 / Documentation (this feature)

```text
specs/004-ast-precision/
├── plan.md              # 本文件
├── research.md          # Phase 0（指纹/重构/路由决策）
├── data-model.md        # Phase 1（值对象）
├── contracts/ast-matcher.md  # Phase 1（内部契约）
├── quickstart.md        # Phase 1
└── tasks.md             # /speckit-tasks（本命令不创建）
```

### 源码（增量于 feature 01）/ Source Code (incremental)

```text
src/main/java/com/codeq/
├── match/
│   ├── AstMatcher.java            # 增强：路由组合（类级 + 方法级）
│   ├── MethodFingerprinter.java   # 新增：规范化 AST → SHA-256 指纹
│   └── RefactorDetector.java      # 新增：baseline vs release 方法匹配 → RefactorFlag
├── model/
│   ├── MethodFingerprint.java     # 新增：className + signature + structureHash
│   └── RefactorFlag.java          # 新增：NONE / SIGNATURE_CHANGE / METHOD_MOVE
└── (feature 01 的 diff/diffcover/verdict/coverage/report/process 不变)
```

**结构决策 (Structure Decision)**：在 feature 01 包结构上**新增** `match/MethodFingerprinter`、`match/RefactorDetector`、`model/MethodFingerprint`、`model/RefactorFlag`；增强现有 `match/AstMatcher`（路由组合）与 `model/IncrementalChange`（加 `fingerprint` / `refactorFlag`）。判定链路其余部分零改动。

## 复杂度追踪 / Complexity Tracking

> **仅当宪法校验有需豁免的违规时填写。本特性无违规，留空。**

| 违规 | 为何需要 | 否决的更简方案 |
|------|---------|---------------|
| （无） | — | — |
