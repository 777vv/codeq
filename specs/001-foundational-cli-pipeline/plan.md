# 实施计划：基础链路打通命令行管线 / Implementation Plan: Foundational CLI Pipeline

**分支 (Branch)**: `001-foundational-cli-pipeline` | **日期 (Date)**: 2026-08-05 | **规格 (Spec)**: [spec.md](spec.md)

**输入 (Input)**: 来自 `specs/001-foundational-cli-pipeline/spec.md` 的功能规格

**注 (Note)**: 本文件由 `/speckit-plan` 命令填写；命令定义描述了执行工作流。

## 摘要 / Summary

codeq 平台迭代 #1「基础链路打通」：构建一条**命令行可用**的端到端链路——给定代码仓库、基准分支、待发布分支与测试环境执行数据源，产出三色（绿/红/黄）增量覆盖判定报告。

技术路径：以 Java 21 + Spring Boot 3.x（`CommandLineRunner`）+ Picocli 为入口；通过 git 子进程（`git merge-base`/`git diff`）获取增量；复用开源 `diff-cover`（Python，子进程调用）做核心覆盖率计算；用 `org.jacoco.core` 经 TCP 远程 dump/reset 执行数据；用 JavaParser 做 AST「类名 + 方法签名 + 路由标识」精准匹配；最终产出三色判定与报告。

**范围边界**：本迭代 CLI-only；不含持久化、服务层、前端、发布门禁 API（分别属迭代 #2/#3/#5）。

## 技术上下文 / Technical Context

**语言/版本 (Language/Version)**: Java 21（宪法第四篇 4.3 强制）

**主依赖 (Primary Dependencies)**:
- Spring Boot 3.x（`CommandLineRunner` 入口；宪法 4.3）
- Picocli（命令行参数解析 / 子命令，与 Spring Boot 集成）
- `org.jacoco.core`（测试环境执行数据 TCP dump / reset；宪法 4.2）
- JavaParser（Java 源码 AST 解析，提取「类名 + 方法签名 + 路由」主键；宪法 4.1）
- 外部工具 `diff-cover`（Python 包，子进程调用；宪法 4.1 强制复用，**禁止自研核心比对算法**）
- 外部工具 `git`（子进程 `git merge-base` / `git diff`；宪法 4.1 强制）

**存储 (Storage)**: N/A（本迭代 CLI-only，无持久化；持久化与服务化为迭代 #2）

**测试 (Testing)**: JUnit 5

**目标平台 (Target Platform)**: 可运行 Java 21 + Python 3（含 `diff-cover`）+ git 的开发/CI 机器（**非**业务项目运行时）

**项目类型 (Project Type)**: cli（命令行工具；迭代 #2 演进为 service）

**构建 (Build)**: Maven

**性能目标 (Performance Goals)**: 中等规模仓库（约数十万行 / 数千行增量）单次检查 ≤ 5 分钟（spec SC-006）

**约束 (Constraints)**:
- 仅消费测试环境执行数据；不得读取生产 / 预发 / 本地（宪法 IV）
- 不依赖任何单元测试覆盖率（宪法 I）
- 增量匹配必须叠加 AST 方法签名（宪法 III / 红线）
- 零侵入：CLI 工具本身不改造业务项目代码（宪法 IV）
- 确定性：相同输入 → 相同输出（spec FR-010）

**规模/范围 (Scale/Scope)**: 单 Java/Maven 项目；本迭代聚焦 CLI 链路打通，不含 Web UI、DB、异步队列、发布平台对接。

## 宪法校验 / Constitution Check

*门禁 (GATE): 必须在 Phase 0 研究前通过；Phase 1 设计后复检。*

逐条核对 `.specify/memory/constitution.md`：

| 宪法原则 | 校验 | 状态 |
|---|---|---|
| I. 唯真实流量覆盖（不碰单测） | 仅用 Jacoco 测试环境执行数据；不接入单元测试覆盖率 | ✅ 通过 |
| II. 仅校验增量 | `git merge-base` 取共同基线，仅校验本次 diff | ✅ 通过 |
| III. AST 精准匹配 | JavaParser 提取「类名 + 方法签名 + 路由」主键；`git merge-base` 取基线；复用 `diff-cover` 引擎 | ✅ 通过 |
| IV. 零侵入 + 环境隔离 | Jacoco 仅测试环境；CLI 不改造业务代码；按 项目/版本/commit/任务/实例 隔离 | ✅ 通过 |
| V. 三色判定（固定） | verdict 模块：绿 / 红 / 黄；partial 标记不改三色语义 | ✅ 通过 |
| VI. 职责边界 | 只判「是否执行」；不做用例管理 / 执行 / 断言 | ✅ 通过 |
| 技术架构 4.1 | git merge-base + diff-cover + AST | ✅ 通过 |
| 技术架构 4.2 | Jacoco TCP，仅测试环境，多维隔离 | ✅ 通过 |
| 技术架构 4.3 | Java 21 + Spring Boot 3.x（CLI 经 `CommandLineRunner`） | ✅ 通过 |
| 迭代顺序 第六篇 | 本迭代 = 基础链路（CLI），不含服务化 / 可视化 / 门禁 | ✅ 通过 |
| 红线 第七篇 | 全部遵守（见上行） | ✅ 通过 |

**结论**: 无违规，门禁通过。**无需 Complexity Tracking 豁免。**

> 张力说明：`diff-cover` 为 Python 工具，与「后端 Java」存在技术栈差异；但宪法 4.1「底层引擎固定复用 diff-cover，禁止自研核心比对算法，仅做业务适配封装」为更高优先级的强制约束。故 Java 作为编排与业务适配层（AST 匹配 / 三色判定 / Jacoco 采集 / 报告），`diff-cover` 仅做「行级是否覆盖」核心计算，经子进程调用——合规（详见 [research.md](research.md)）。

## 项目结构 / Project Structure

### 本特性文档 / Documentation (this feature)

```text
specs/001-foundational-cli-pipeline/
├── plan.md              # 本文件（/speckit-plan 产物）
├── research.md          # Phase 0 产物（技术决策）
├── data-model.md        # Phase 1 产物
├── quickstart.md        # Phase 1 产物
├── contracts/           # Phase 1 产物
│   └── cli.md           # CLI 命令契约
└── tasks.md             # /speckit-tasks 产物（本命令不创建）
```

### 源码（仓库根）/ Source Code (repository root)

```text
codeq/
├── pom.xml                          # Maven；Java 21；Spring Boot 3.x + Picocli + jacoco-core + javaparser
├── src/main/java/com/codeq/
│   ├── CodeqCli.java                # @SpringBootApplication + CommandLineRunner + Picocli 命令
│   ├── diff/
│   │   └── GitDiffService.java      # 子进程调用 git merge-base / git diff，产出增量变更清单
│   ├── coverage/
│   │   └── JacocoCollector.java     # org.jacoco.core：TCP dump（拉 .exec）/ reset / 多轮累加
│   ├── match/
│   │   └── AstMatcher.java          # JavaParser：建立「类名 + 方法签名 + 路由」主键
│   ├── diffcover/
│   │   └── DiffCoverRunner.java     # 子进程调用 diff-cover，行级覆盖率计算（业务适配封装）
│   ├── verdict/
│   │   └── VerdictEngine.java       # 三色判定 + partial 标记
│   └── report/
│       └── ReportGenerator.java     # 控制台彩色（ANSI）+ HTML 报告
├── src/test/java/com/codeq/...      # JUnit 5
└── src/main/resources/              # 模板等
```

**结构决策 (Structure Decision)**: 单 Maven 项目，按职责分包（`diff` / `coverage` / `match` / `diffcover` / `verdict` / `report`）。核心域逻辑（`match` / `verdict`）与外部适配（`git` / `jacoco` / `diff-cover` 子进程）分离，便于迭代 #2 被 Spring Boot 服务层复用；CLI 仅作入口编排。

## 复杂度追踪 / Complexity Tracking

> **仅当宪法校验有需豁免的违规时填写。本特性无违规，留空。**

| 违规 | 为何需要 | 否决的更简方案 |
|------|---------|---------------|
| （无） | — | — |
