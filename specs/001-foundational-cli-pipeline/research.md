# 研究：基础链路打通命令行管线 / Research: Foundational CLI Pipeline

**特性 (Feature)**: 001-foundational-cli-pipeline
**阶段 (Phase)**: 0（研究 / Research）
**日期 (Date)**: 2026-08-05
**输入 (Input)**: [plan.md](plan.md) 技术上下文与宪法校验

本文件记录每个关键技术决策的**选择 / 理由 / 备选**，并解决所有 NEEDS CLARIFICATION。本特性 Technical Context 无遗留 NEEDS CLARIFICATION——所有技术点均依据宪法 + 工程实践决策如下。

---

## 1. CLI 框架：Spring Boot 3.x `CommandLineRunner` + Picocli

**Decision (选择)**: Spring Boot 3.x（`@SpringBootApplication` + `CommandLineRunner`）作为运行容器，Picocli 做参数解析与子命令（picocli-spring-boot-starter 集成）。

**Rationale (理由)**:
- 宪法 4.3 字面强制「后端 Java 21 + 框架 Spring Boot 3.x」——遵守优先。
- 为迭代 #2「核心接口服务化」铺垫：核心域逻辑（match / verdict / report）以 Spring Bean 组织，迭代 #2 直接被 `@RestController` / 异步任务复用，无需重写。
- Spring DI 把 `git` / `jacoco` / `diff-cover` 子进程适配器与核心域逻辑解耦，便于单元测试（Mock 适配器）。

**Alternatives (备选)**:
- 纯 Picocli（无 Spring Boot）：更轻、启动更快；但偏离宪法 4.3 字面，且迭代 #2 服务化时要重构装配。**否决**。
- Quarkus / 手写 DI：偏离宪法。**否决**。

---

## 2. 增量 diff 获取：git 子进程（`git merge-base` / `git diff`）

**Decision**: `ProcessBuilder` 调用 `git merge-base <baseline> <release>` 取共同基线，再 `git diff <merge-base> <release>` 取增量变更清单（新增 / 修改文件与位置）。

**Rationale**:
- 宪法 4.1 字面强制「必须通过 `git merge-base` 命令获取双分支共同基线」——明确要求「命令」，故用 git 子进程而非 JGit。
- `git` 命令是事实标准、行为最可信，且不引入 JGit 重依赖。

**Alternatives**:
- JGit（纯 Java）：易测试、无外部依赖；但宪法要求「`git merge-base` 命令」，JGit 是重新实现，字面不符。**否决**（除非修宪）。

**风险**: 需宿主装有 git；CI 与开发机默认具备（quickstart 标注前置）。

---

## 3. 核心覆盖率计算：复用 `diff-cover`（Python，子进程调用）

**Decision**: Java 经 `ProcessBuilder` 调用 `diff-cover`：输入 Jacoco 生成的 `coverage.xml` + 增量 diff，得到「变更行是否被执行」的行级覆盖率；Java 侧做业务适配封装（调用、解析输出、喂给 AST 匹配 / 判定）。**禁止自研核心比对算法**（宪法 4.1）。

**Rationale**:
- 宪法 4.1 红线级强制「增量覆盖比对底层引擎固定复用开源工具 diff-cover，禁止自研核心比对算法，仅做业务适配封装」。
- `diff-cover` 成熟、专为「diff × coverage」设计，避免重写易错的覆盖率与行号对齐逻辑。

**张力与解法**:
- 张力：`diff-cover` 是 Python，与 4.3「后端 Java」存在技术栈差异。
- 解法：Java 作为编排与业务适配层（AST 匹配、三色判定、Jacoco 采集、报告），`diff-cover` 仅做「行级是否覆盖」核心计算，经子进程调用——符合 4.1「仅做业务适配封装」。Python 作为外部工具依赖（类比 git），不构成「后端用 Python」。

**Alternatives**:
- Java 重写 diff-cover 算法：直接违反宪法红线（禁止自研）。**否决**。
- `diff-cover` 作为独立 Python 微服务：本迭代 CLI-only，引入服务化过早（迭代 #2）。**否决**。
- 找 Java 等价物：无成熟等价，且违反「固定复用 diff-cover」。**否决**。

**前置**: 需宿主装有 Python 3 + `pip install diff-cover`（quickstart 标注）。`diff-cover` 输出选 JSON 以便 Java 解析。

---

## 4. AST 精准匹配：JavaParser

**Decision**: JavaParser 解析变更涉及的 Java 源文件，为每个变更方法建立主键「类全限定名 + 方法签名（名 + 参数类型）+ 路由标识（Spring `@RequestMapping` / `@GetMapping` 等路径）」。**行号仅用于报告可视化，不作匹配主键**（宪法 4.1）。

**Rationale**:
- 宪法 4.1 强制「必须接入 AST 语法树解析能力，以『类名 + 方法签名 + 路由标识』作为核心匹配主键」。
- JavaParser 成熟、支持 Java 21 语法，能解析注解取路由路径。
- 解决行号漂移 / 重构误判（宪法 4.1 红线：禁止仅依靠行号匹配）。

**匹配策略**:
- `diff-cover` 给出「变更行是否被执行」（行级）。
- `AstMatcher` 把变更行归约到所属方法（按主键），结合执行情况产出方法级判定；无法稳定归约 → 黄色（人工复核，spec 边界场景）。
- 部分执行的方法 → 行级明细 + `partial` 标记（spec FR-013）。

**Alternatives**:
- Eclipse JDT / ANTLR Java grammar：更重；JavaParser 对源码注解提取更直接。**否决**。
- Spoon：可行但更重；JavaParser 足够。**否决**。

---

## 5. 执行数据采集：`org.jacoco.core` TCP dump / reset

**Decision**: 用 `org.jacoco.core` 远程执行数据接口（`ExecFileLoader` / dump 客户端，TCP）从测试环境 Jacoco agent 拉取当前 `.exec`；支持 reset（重置 agent 计数）与多轮累加（多次 dump 合并）。**仅连接测试环境实例**（宪法 4.2）。`.exec` 经 Jacoco report 转换为 `coverage.xml` 供 `diff-cover` 消费。

**Rationale**:
- 宪法 4.2 强制「Java 项目仅测试环境挂载 Jacoco Agent，采用 TCP 服务模式，支持远程动态 dump、多次累加、一键重置」。
- `org.jacoco.core` 是 Jacoco 官方库，提供 TCP dump 客户端与 `.exec` 解析。
- 数据隔离：CLI 仅接受显式传入的测试环境 host/port，并绑定 项目 / 版本 / commit / 任务 / 实例（宪法 4.2）。

**Alternatives**:
- Jacoco 文件模式（agent 写文件后读取）：不支持远程 dump / reset，且文件落盘耦合业务环境。**否决**（违反 TCP 模式要求）。
- 业务项目侧手工拷贝 `.exec`：非「远程动态 dump」，违背 4.2。**否决**。

**前置**: 业务项目测试环境已以 `output=tcpserver` 模式挂载 Jacoco agent（属接入文档范畴，本 CLI 为消费端）。

---

## 6. 报告格式：控制台彩色（ANSI）+ HTML

**Decision**: 默认控制台彩色文本（🟢 / 🔴 / 🟡 + 摘要统计 + 位置）；可选 `--report html` 生成 HTML（diff 高亮占位，迭代 #3 接 Monaco）；另可 `--report json` 供机器读取。

**Rationale**:
- spec FR-004「人类可读报告」+ 宪法三色统一口径；CLI 基础链路最自然输出为控制台。
- JSON 输出便于迭代 #5 发布门禁 API 复用。

**Alternatives**:
- 仅 HTML：CLI 交互不友好。**否决**。
- 仅控制台：不利存档 / 对接。保留 HTML 可选。

---

## 7. 构建工具：Maven

**Decision**: Maven（`pom.xml`），Java 21，Spring Boot 3.x parent。

**Rationale**: Java + Spring Boot 生态主流；宪法 4.3 提阿里巴巴手册，Maven 与之常见搭配；CI 集成成熟。

**Alternatives**: Gradle（Kotlin DSL）——可行，团队偏好相关；本迭代选 Maven 以降低入门门槛。如团队更熟 Gradle 可后续切换。

---

## 结论 / Summary

所有技术决策均**符合 codeq 宪法**（第四篇技术架构、第六篇迭代顺序、第七篇红线），**无 NEEDS CLARIFICATION 遗留**，**无宪法违规需豁免**。关键张力（`diff-cover` 为 Python）已按宪法 4.1「仅做业务适配封装」消解。可进入 Phase 1 设计。
