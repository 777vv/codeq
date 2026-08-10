---
description: "AST 精准匹配优化的实现任务清单"
---

# Tasks: AST 精准匹配优化 / AST Precision Refinement

**Input**: 设计文档来自 `/specs/004-ast-precision/`（plan.md、spec.md、research.md、data-model.md、contracts/ast-matcher.md、quickstart.md）

**Prerequisites**: plan.md（必需）、spec.md（必需）、data-model.md、contracts/、research.md、quickstart.md

**Tests**: 本特性规格未要求 TDD，故**不生成独立测试任务**。建议为 `MethodFingerprinter` / `RefactorDetector` 补 JUnit（指纹确定性、重构识别）。

**Organization**: 任务按 user story 分组；复用 feature 01 判定链路（FR-006），仅增强 `match/` + 新增 `model`。

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: 可并行（不同文件、无依赖）
- **[Story]**: 所属 user story（US1/US2/US3），仅 user story 阶段标注
- 每条任务含确切文件路径

## Path Conventions / 路径约定

- 复用 feature 01 项目：`src/main/java/com/codeq/{match,model,report,diff}/`
- 判定链路（`diff`/`diffcover`/`verdict`）零改动，仅增强 `match` + 新增 `model`

---

## Phase 1: Setup（确认基础设施）

**Purpose**: 确认 feature 01 依赖与结构可扩展

- [x] T001 [P] 确认 `javaparser-core` 已在 `pom.xml`（feature 01），`com.codeq.match` 包可扩展（无需新依赖），在 `pom.xml`、`src/main/java/com/codeq/match/`

---

## Phase 2: Foundational（阻塞前置，所有 user story 依赖）

**Purpose**: 新增 model 值对象 + 增强 IncrementalChange

**⚠️ CRITICAL**: 所有 user story 实现须等本阶段完成

- [x] T002 [P] `model/MethodFingerprint.java`（record：className + signature + structureHash），在 `src/main/java/com/codeq/model/MethodFingerprint.java`
- [x] T003 [P] `model/RefactorFlag.java`（enum：NONE / SIGNATURE_CHANGE / METHOD_MOVE），在 `src/main/java/com/codeq/model/RefactorFlag.java`
- [x] T004 增强 `model/IncrementalChange.java`：新增 `fingerprint: String` 与 `refactorFlag: RefactorFlag`（默认 NONE）字段 + getter/setter，在 `src/main/java/com/codeq/model/IncrementalChange.java`

**Checkpoint**: model 就绪，user story 可展开

---

## Phase 3: User Story 1 - 方法身份指纹 (Priority: P1) 🎯 MVP

**Goal**: 规范化 AST 指纹，局部变量重命名/行内移位身份稳定

**Independent Test**: 方法内变量重命名（不改签名）→ 指纹不变 → 识别为同一方法

### Implementation for User Story 1

- [x] T005 [US1] `match/MethodFingerprinter.java`：JavaParser 解析方法体 → 规范化（局部变量名→占位、忽略行号/空白/注释）→ SHA-256 `structureHash`；确定性强（相同 AST 相同 hash，FR-007），在 `src/main/java/com/codeq/match/MethodFingerprinter.java`
- [x] T006 [US1] 增强 `match/AstMatcher.java`：`mapLinesToMethods` 时为每个方法计算 fingerprint，填入 `MethodKey`/`IncrementalChange.fingerprint`，在 `src/main/java/com/codeq/match/AstMatcher.java`（依赖 T004、T005）

**Checkpoint**: US1 可用——变量重命名不改变方法身份，判定不变

---

## Phase 4: User Story 2 - 重构识别与标注 (Priority: P2)

**Goal**: baseline vs release 方法匹配，标注重构，避免误报 RED

**Independent Test**: 方法签名变更（int→long）+ 测试调新签名 → 标注 SIGNATURE_CHANGE + 非 RED

### Implementation for User Story 2

- [ ] T007 [US2] `match/RefactorDetector.java`：输入 baseline 与 release 方法集（指纹+签名），匹配——release 方法在 baseline 无同签名但有相似指纹 → `SIGNATURE_CHANGE`/`METHOD_MOVE`，在 `src/main/java/com/codeq/match/RefactorDetector.java`（依赖 T002、T003、T005）
- [ ] T008 [US2] 集成重构检测：`diff/GitDiffService.java` 加 `baselineFileContent(repo, base, path)`（`git show base:path`）；`AstMatcher` 加 `parseMethods` 解析指定文件方法集；判定链路（`verdict/VerdictEngine.java` 或 `cli/CheckCommand.java` + `task/ScanService.java`）在 verdict 前调 `RefactorDetector` 填 `IncrementalChange.refactorFlag`，在 `src/main/java/com/codeq/{diff,match,verdict,cli,task}/`

**Checkpoint**: US2 可用——重构方法被识别标注，被执行则非 RED

---

## Phase 5: User Story 3 - 路由解析增强 (Priority: P3)

**Goal**: 类级 + 方法级路由组合

**Independent Test**: 类 `@RequestMapping("/api")` + 方法 `@GetMapping("/foo")` → 路由 `/api/foo`

### Implementation for User Story 3

- [x] T009 [US3] 增强 `match/AstMatcher.java` 的 `extractRoute`：解析类级 `@RequestMapping` `path`/`value` 作前缀，与方法级注解拼接（斜杠归一：`/api`+`/foo`→`/api/foo`；`/api`+`/`→`/api`），在 `src/main/java/com/codeq/match/AstMatcher.java`

**Checkpoint**: US3 可用——路由主键为组合路径

---

## Phase 6: Polish（收尾与横切）

**Purpose**: 报告展示 + 验证

- [ ] T010 [P] 增强 `report/ReportGenerator.java`：报告展示 `refactorFlag` 标注（重构变更）+ `fingerprint`（追溯），在 `src/main/java/com/codeq/report/ReportGenerator.java`
- [ ] T011 运行 quickstart 验证（codeq-demo 三场景：变量重命名 US1 / 签名变更 US2 / 路由组合 US3），对照 `specs/004-ast-precision/quickstart.md`

---

## Dependencies & Execution Order / 依赖与执行顺序

### Phase Dependencies / 阶段依赖
- **Setup（Phase 1）**: 无依赖，立即开始
- **Foundational（Phase 2）**: 依赖 Setup——**阻塞所有 user story**
- **User Stories（Phase 3+）**: 均依赖 Foundational；US1→US2→US3（US2 的重构检测复用 US1 的指纹）
- **Polish（Phase 6）**: 依赖期望完成的 user story

### User Story Dependencies / 各 story 依赖
- **US1（P1）**: Foundational 完成即可开始；**不依赖其他 story**
- **US2（P2）**: 依赖 US1 的 `MethodFingerprinter`（指纹是重构匹配基础）
- **US3（P3）**: 独立（仅改 `AstMatcher.extractRoute`），可与 US1/US2 并行

### Within Each User Story / story 内部
- model → 组件 → 集成；指纹在 US1，重构检测复用指纹（US2）

### Parallel Opportunities / 并行机会
- Phase 2 的 T002、T003 不同文件，可并行（T004 改 IncrementalChange 依赖二者概念上但可并行）
- US3（T009）与 US1/US2 不同方法/逻辑，可并行

---

## Parallel Example: Phase 2 Foundational / 并行示例

```bash
# Foundational 内可并行（不同文件）：
Task: "model/MethodFingerprint.java"
Task: "model/RefactorFlag.java"
```

---

## Implementation Strategy / 实施策略

### MVP First（仅 User Story 1）/ 优先 MVP
1. 完成 Phase 1 Setup（确认依赖）
2. 完成 Phase 2 Foundational（model 值对象 + IncrementalChange 增强）
3. 完成 Phase 3 User Story 1（T005–T006）
4. **停下验证**：变量重命名 → 指纹稳定 → 判定不变

### Incremental Delivery / 增量交付
1. Setup + Foundational → model 就绪
2. + US1 → 方法身份指纹稳定（MVP）
3. + US2 → 重构识别，降误报
4. + US3 → 路由组合
5. Polish → 报告展示 + 验证

---

## Notes / 备注

- `[P]` = 不同文件、无依赖，可并行
- `[Story]` 标签映射到 user story
- **复用 feature 01 判定链路**（FR-006）：仅增强 `match` + 新增 `model`，`diff`/`diffcover`/`verdict` 不改（US2 集成除外，需 baseline 源码获取）
- 测试任务未生成（spec 未要求 TDD）；建议为 `MethodFingerprinter`/`RefactorDetector` 补 JUnit（指纹确定性、重构识别）
- 每个任务或逻辑分组后提交（作者 `wangtao <wangtao>`）
- 宪法合规：III AST、IX 类头（新类 `@author`/`@date`）、VIII 日志（SLF4J）、VII 门禁
- 避免：模糊任务、同文件冲突、重写 feature 01 核心判定
