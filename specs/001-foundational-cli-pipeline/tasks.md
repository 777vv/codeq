---
description: "基础链路打通命令行管线的实现任务清单"
---

# Tasks: 基础链路打通命令行管线 / Foundational CLI Pipeline (基础链路打通)

**Input**: 设计文档来自 `/specs/001-foundational-cli-pipeline/`（plan.md、spec.md、research.md、data-model.md、contracts/cli.md、quickstart.md）

**Prerequisites**: plan.md（必需）、spec.md（必需）、data-model.md、contracts/、research.md、quickstart.md

**Tests**: 本特性规格未要求 TDD，故**不生成独立测试任务**。JUnit 5 依赖已纳入 pom.xml；实现各模块时建议补单元测试，如需 TDD 任务可后续按需追加。

**Organization**: 任务按 user story 分组，支持各 story 独立实现与测试。

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: 可并行（不同文件、无依赖）
- **[Story]**: 所属 user story（US1/US2/US3），仅 user story 阶段任务标注
- 每条任务含确切文件路径

## Path Conventions / 路径约定

- 单 Maven 项目：源码 `src/main/java/com/codeq/`，测试 `src/test/java/`，资源 `src/main/resources/`

<!--
  下方为针对本特性的真实任务（基于 spec.md 的 user story P1/P2/P3、plan.md 的技术栈与结构、
  data-model.md 的实体、contracts/cli.md 的命令契约生成）。按 user story 组织，每个 story 可独立实现与测试。
-->

## Phase 1: Setup（项目初始化）

**Purpose**: Maven 项目初始化与基本结构

- [ ] T001 创建 Maven 项目 `pom.xml`：Java 21、Spring Boot 3.x parent、依赖（Picocli + picocli-spring-boot-starter、`org.jacoco.core`、JavaParser）、p3c 阿里规约插件、spring-boot-maven-plugin、UTF-8 编码，在 `pom.xml`
- [ ] T002 [P] 创建源码包结构 `src/main/java/com/codeq/{diff,coverage,match,diffcover,verdict,report,process,model}` 与 `src/main/resources/`
- [ ] T003 [P] CodeqCli 入口骨架：`@SpringBootApplication` + `CommandLineRunner` + Picocli 父命令（`check`/`dump`/`reset` 子命令占位），在 `src/main/java/com/codeq/CodeqCli.java`

---

## Phase 2: Foundational（阻塞前置，所有 user story 依赖）

**Purpose**: 所有 user story 共享的核心基础设施

**⚠️ CRITICAL**: 所有 user story 实现须等本阶段完成

- [ ] T004 [P] ProcessRunner 子进程通用封装：统一调用 git/diff-cover，处理超时、UTF-8、退出码、stdout/stderr 捕获，在 `src/main/java/com/codeq/process/ProcessRunner.java`
- [ ] T005 [P] ExitCode 枚举与 CodeqException：统一退出码（0 合规 / 1 有风险 / 2 输入或环境错误）与异常体系，在 `src/main/java/com/codeq/ExitCode.java`、`src/main/java/com/codeq/CodeqException.java`

**Checkpoint**: 基础设施就绪，user story 可并行展开

---

## Phase 3: User Story 1 - 三色增量覆盖报告 (Priority: P1) 🎯 MVP

**Goal**: 一条命令产出三色（绿/红/黄）增量覆盖判定报告（US1 MVP 用本地 `coverage.xml` 作为执行数据输入，独立可测；测试环境 dump 由 US2 接入）

**Independent Test**: 提供已知增量变更 + 已知 `coverage.xml`，运行 `codeq check --coverage-xml ...`，验证 GREEN/RED/YELLOW 与人为构造一致、退出码正确

### Implementation for User Story 1

- [ ] T006 [P] [US1] 域类型：`Verdict`（GREEN/RED/YELLOW/PARTIAL）、`MethodKey`（className+signature+route）、`IncrementalChange`，在 `src/main/java/com/codeq/model/Verdict.java`（及 `MethodKey.java`、`IncrementalChange.java`）
- [ ] T007 [P] [US1] GitDiffService：`ProcessBuilder` 调 `git merge-base <base> <release>` 取基线、`git diff` 取增量，产出 IncrementalChange 清单（文件/变更行/ADD|MODIFY）；无共同祖先报错，在 `src/main/java/com/codeq/diff/GitDiffService.java`
- [ ] T008 [P] [US1] AstMatcher：JavaParser 解析变更源码，建立「类名+方法签名+路由（@RequestMapping 等）」主键，把变更行归约到方法；无法归约标记 YELLOW，在 `src/main/java/com/codeq/match/AstMatcher.java`
- [ ] T009 [US1] DiffCoverRunner：`ProcessBuilder` 调 `diff-cover`（输入 coverage.xml + diff，输出 JSON），解析行级「是否执行」，在 `src/main/java/com/codeq/diffcover/DiffCoverRunner.java`（依赖 T004）
- [ ] T010 [US1] VerdictEngine：结合 AstMatcher 归约 + DiffCoverRunner 行级执行 → 三色判定 + partial（附未覆盖行明细）：全执行→GREEN、从未执行→RED、无法归约→YELLOW、部分→PARTIAL，在 `src/main/java/com/codeq/verdict/VerdictEngine.java`（依赖 T008、T009）
- [ ] T011 [US1] ReportGenerator：控制台彩色（ANSI 🟢/🔴/🟡 + 摘要统计 + 位置）+ HTML + JSON 三格式，在 `src/main/java/com/codeq/report/ReportGenerator.java`（依赖 T010）
- [ ] T012 [US1] `check` 子命令编排：串接 diff→AST→diff-cover→verdict→report；US1 MVP 支持 `--coverage-xml <path>` 本地输入；退出码 0/1/2，在 `src/main/java/com/codeq/CodeqCli.java`（依赖 T007–T011）

**Checkpoint**: User Story 1 独立可用——`codeq check --coverage-xml` 产出完整三色报告

---

## Phase 4: User Story 2 - 执行数据采集控制 (Priority: P2)

**Goal**: 从测试环境远程 dump/reset/累加执行数据，并接入 `check`

**Independent Test**: reset 后覆盖清零；多轮 `--accumulate` 后覆盖反映所有轮次

### Implementation for User Story 2

- [ ] T013 [P] [US2] JacocoCollector：`org.jacoco.core` TCP dump（远程拉 `.exec`）、reset（重置 agent 计数）、多轮累加（合并 `.exec`），在 `src/main/java/com/codeq/coverage/JacocoCollector.java`
- [ ] T014 [P] [US2] CoverageReportConverter：`.exec` → `coverage.xml`（Jacoco report），供 diff-cover 消费，在 `src/main/java/com/codeq/coverage/CoverageReportConverter.java`
- [ ] T015 [US2] `dump` 子命令：从测试环境（`--jacoco-host/--jacoco-port`）拉 `.exec`，可选 `--out` 落盘，在 `src/main/java/com/codeq/CodeqCli.java`（依赖 T013）
- [ ] T016 [US2] `reset` 子命令：重置 Jacoco agent 计数，在 `src/main/java/com/codeq/CodeqCli.java`（依赖 T013）
- [ ] T017 [US2] `check` 接入测试环境数据源：`--jacoco-host/--jacoco-port` → JacocoCollector dump → 转 coverage.xml → 喂判定链路；支持 `--accumulate` 多轮累加，在 `src/main/java/com/codeq/CodeqCli.java`（依赖 T013、T014、T012）

**Checkpoint**: User Stories 1 + 2 均可用——`check` 可直连测试环境采集数据

---

## Phase 5: User Story 3 - 确定性 / 隔离 / 零侵入 (Priority: P3)

**Goal**: 判定可复现、执行数据环境隔离、业务项目零侵入接入

**Independent Test**: 相同输入两次 `check` 输出一致；传入版本不匹配数据→退出码 2；接入示例项目无应用代码改动

### Implementation for User Story 3

- [ ] T018 [P] [US3] ExecutionDataValidator：拒绝非测试环境 / 版本-commit 不匹配的执行数据（退出码 2），在 `src/main/java/com/codeq/coverage/ExecutionDataValidator.java`
- [ ] T019 [P] [US3] IsolationKey：按 项目/版本/commit/任务/实例 绑定执行数据，防交叉污染，在 `src/main/java/com/codeq/model/IsolationKey.java`
- [ ] T020 [US3] 确定性保证：判定过程确定性排序、无随机/无时钟依赖，相同输入→相同输出（增强 VerdictEngine/diff/match 输出稳定排序），在 `src/main/java/com/codeq/verdict/VerdictEngine.java` 等（依赖 T010）
- [ ] T021 [US3] 零侵入接入文档：业务项目测试环境以 Jacoco `output=tcpserver` 挂载 agent 的接入说明（无应用代码改动），在 `docs/integration-java.md`

**Checkpoint**: 全部 user story 独立可用，满足确定性/隔离/零侵入

---

## Phase 6: Polish（收尾与横切）

**Purpose**: 跨 user story 的收尾与质量

- [ ] T022 [P] 对齐 README 与 quickstart：补 JDK 21 / Python 3 + diff-cover / git 前置、实际命令示例，在 `README.md`、`specs/001-foundational-cli-pipeline/quickstart.md`
- [ ] T023 [P] 错误信息与日志规范化（中文、清晰原因、对齐宪法口径），在各模块
- [ ] T024 性能：中等仓库（数十万行）单次检查 ≤ 5min 验证与必要优化（并发 AST 解析 / 流式 diff），在 `src/main/java/com/codeq/{match,diff,diffcover}/`
- [ ] T025 运行 quickstart.md 端到端验证（三场景 + 验证矩阵），对照 `specs/001-foundational-cli-pipeline/quickstart.md`

---

## Dependencies & Execution Order / 依赖与执行顺序

### Phase Dependencies / 阶段依赖

- **Setup（Phase 1）**: 无依赖，立即开始
- **Foundational（Phase 2）**: 依赖 Setup 完成——**阻塞所有 user story**
- **User Stories（Phase 3+）**: 均依赖 Foundational 完成；可并行（若有人力）或按优先级顺序（P1 → P2 → P3）
- **Polish（Phase 6）**: 依赖期望完成的 user story

### User Story Dependencies / 各 story 依赖

- **US1（P1）**: Foundational 完成后即可开始，**不依赖其他 story**（MVP 用本地 `--coverage-xml`）
- **US2（P2）**: Foundational 完成后即可开始；T017 接入 check 依赖 US1 的 T012
- **US3（P3）**: Foundational 完成后即可开始；T020 依赖 US1 的 T010

### Within Each User Story / story 内部顺序

- 域类型（model）→ 外部适配（git/diff-cover/jacoco）→ 核心逻辑（verdict）→ 报告 → 命令编排
- 核心逻辑在集成之前

### Parallel Opportunities / 并行机会

- Phase 1 的 T002、T003 互不冲突，可并行
- Phase 2 的 T004、T005 不同文件，可并行
- US1 内 T006、T007、T008 不同文件，可并行（T009 依赖 T004，T010 依赖 T008/T009）
- US2 内 T013、T014 不同文件，可并行
- US3 内 T018、T019、T021 不同文件，可并行
- 不同 user story 可由不同人并行推进

---

## Parallel Example: User Story 1 / 并行示例

```bash
# US1 内可并行启动（不同文件）：
Task: "域类型 Verdict/MethodKey/IncrementalChange in src/main/java/com/codeq/model/"
Task: "GitDiffService in src/main/java/com/codeq/diff/GitDiffService.java"
Task: "AstMatcher in src/main/java/com/codeq/match/AstMatcher.java"
```

---

## Implementation Strategy / 实施策略

### MVP First（仅 User Story 1）/ 优先 MVP

1. 完成 Phase 1 Setup
2. 完成 Phase 2 Foundational（关键——阻塞所有 story）
3. 完成 Phase 3 User Story 1（T006–T012）
4. **停下验证**：用本地 `coverage.xml` 独立测试 US1 三色判定
5. 必要时演示

### Incremental Delivery / 增量交付

1. Setup + Foundational → 基础就绪
2. + US1 → 独立测试 → 可用 MVP（本地数据源）
3. + US2 → 独立测试 → check 可直连测试环境
4. + US3 → 独立测试 → 确定性/隔离/零侵入达标
5. Polish → 收尾达标

### Parallel Team Strategy / 多人并行

1. 团队共同完成 Setup + Foundational
2. Foundational 完成后：A 做 US1、B 做 US2、C 做 US3（注意 T017/T020 对 US1 的依赖点）

---

## Notes / 备注

- `[P]` = 不同文件、无依赖，可并行
- `[Story]` 标签把任务映射到具体 user story，便于追溯
- 每个 user story 须可独立完成与测试
- **测试任务未生成**（spec 未要求 TDD）；实现时建议为 VerdictEngine/AstMatcher/GitDiffService 等补 JUnit 单测，如需正式 TDD 任务可追加
- 每个任务或逻辑分组后提交（作者 `wangtao <wangtao>`）
- 可在任意 Checkpoint 停下验证
- 避免：模糊任务、同文件冲突、破坏 story 独立性的跨 story 依赖
