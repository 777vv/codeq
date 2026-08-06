# 实施计划：扫描服务化（核心接口服务化）/ Implementation Plan: Scan Service

**分支 (Branch)**: `002-scan-service` | **日期 (Date)**: 2026-08-06 | **规格 (Spec)**: [spec.md](spec.md)

**输入 (Input)**: 来自 `specs/002-scan-service/spec.md` 的功能规格

**注 (Note)**: 本文件由 `/speckit-plan` 命令填写；命令定义描述了执行工作流。

## 摘要 / Summary

将 feature 001 的 CLI 核心判定链路（`diff` / `AST` / `diff-cover` / `verdict` / `report` + Jacoco 采集 / 版本校验）封装为后端服务：Spring Boot REST 接口提交扫描 → `@Async` 异步执行（复用 feature 001 `@Component`）→ JPA 持久化（PostgreSQL / H2）→ 状态 / 结果 / 历史 / 门禁查询 → `@Scheduled` TTL 清理。诊断日志 SLF4J + `traceId` 经 `TaskDecorator` 贯穿异步线程（宪法 VIII）。CLI（feature 001）保留，与 web 双模式共存。

## 技术上下文 / Technical Context

**语言/版本 (Language/Version)**: Java 21（宪法 4.3）

**主依赖 (Primary Dependencies)**:
- Spring Boot 3.x（已有）+ 新增：`spring-boot-starter-web`（REST）、`spring-boot-starter-data-jpa`（持久化）、`spring-boot-starter-validation`（参数校验）
- 数据库驱动：PostgreSQL（prod）、H2（dev/test 内存）
- 复用 feature 001 核心 `@Component`：`GitDiffService` / `AstMatcher` / `DiffCoverRunner` / `VerdictEngine` / `JacocoCollector` / `CoverageReportConverter` / `ExecutionDataValidator`

**存储 (Storage)**: PostgreSQL（prod） / H2（dev）

**测试 (Testing)**: JUnit 5 + Spring Boot Test（H2）

**目标平台 (Target Platform)**: 服务端 JVM（Linux/容器）

**项目类型 (Project Type)**: web-service（后端 REST；CLI 模式保留）

**构建 (Build)**: Maven

**异步 (Async)**: `@Async` + `ThreadPoolTaskExecutor` + `TaskDecorator`（透传 `traceId`）

**TTL**: `@Scheduled` 定时清理过期扫描数据（宪法 4.3）

**约束 (Constraints)**:
- 复用 feature 001 核心判定逻辑（FR-008），保持宪法 I–VI 合规
- 诊断日志经 SLF4J + `traceId` 贯穿异步（宪法 VIII）
- 新代码须过代码门禁（宪法 VII：Google 格式 + 阿里规约 + 安全扫描）
- 数据 TTL 过期清理（宪法 4.3）

**规模/范围 (Scale/Scope)**: 单服务实例；扫描任务并发受线程池上限约束；本迭代不含前端、发布平台对接（分别属迭代 #3/#5）。

## 宪法校验 / Constitution Check

*门禁 (GATE): Phase 0 研究前通过；Phase 1 设计后复检。*

| 宪法原则 | 校验 | 状态 |
|---|---|---|
| I–VI（feature 001 核心） | 复用 feature 001 判定逻辑，继承合规 | ✅ 通过 |
| VII. 代码门禁 | 新代码须过 fmt / PMD(p3c) / SpotBugs | ✅ 通过 |
| VIII. 日志规约 | SLF4J + 每任务 `traceId`（MDC + `TaskDecorator` 贯穿异步） | ✅ 通过 |
| 4.1 git merge-base + diff-cover + AST | 复用 feature 001 | ✅ 通过 |
| 4.2 Jacoco TCP + 多维隔离 | 复用 feature 001 | ✅ 通过 |
| 4.3 任务全异步化 | `@Async` 异步执行 | ✅ 通过 |
| 4.3 数据生命周期 TTL | `@Scheduled` 清理 | ✅ 通过 |
| 4.3 核心逻辑服务端闭环 | 服务端编排 + 复用核心 | ✅ 通过 |
| 迭代顺序 第六篇 | 本 feature = 迭代 #2「核心接口服务化」 | ✅ 通过 |

**结论**: 无违规，门禁通过。**无需 Complexity Tracking 豁免。**

> 启动模式张力：feature 001 的 main 为 CLI（执行后退出）；feature 002 需 web 常驻。决策为**双模式**——main 检测命令行：含 picocli 子命令（`check`/`dump`/`reset`）→ CLI 模式（执行后 `System.exit`）；否则 → web 服务常驻。CLI 不丢失，web 为默认服务形态。

## 项目结构 / Project Structure

### 本特性文档 / Documentation (this feature)

```text
specs/002-scan-service/
├── plan.md              # 本文件
├── research.md          # Phase 0
├── data-model.md        # Phase 1
├── contracts/api.md     # Phase 1
├── quickstart.md        # Phase 1
└── tasks.md             # /speckit-tasks（本命令不创建）
```

### 源码（增量于 feature 01）/ Source Code (incremental)

```text
src/main/java/com/codeq/
├── CodeqCli.java              # 改造为双模式入口（CLI / web）
├── api/
│   ├── ScanController.java    # REST：POST/GET /api/scans、/result、/verdict
│   ├── GlobalExceptionHandler.java  # 统一错误码（400/404/409/500）+ traceId
│   └── dto/                   # 请求 / 响应 DTO（CreateScanRequest、ScanView、ResultView、VerdictView）
├── task/
│   ├── ScanService.java       # 编排：建任务(PENDING) + @Async 执行（复用核心 @Component）+ 落库
│   └── AsyncConfig.java       # @EnableAsync + ThreadPoolTaskExecutor + TaskDecorator(traceId 透传)
├── repo/
│   ├── ScanTaskEntity.java、ScanResultEntity.java
│   ├── ScanTaskRepository.java、ScanResultRepository.java（Spring Data JPA）
├── config/
│   ├── SchedulingConfig.java  # @EnableScheduling
│   └── RetentionJob.java      # @Scheduled TTL 清理
└── (feature 001 的 diff/coverage/match/diffcover/verdict/report/process/model 不变，被复用)

src/main/resources/
├── logback-spring.xml         # 已有（宪法 VIII）
└── application.yml            # 数据源（dev=H2 / prod=PG）、线程池、TTL 阈值
```

**结构决策 (Structure Decision)**: 在 feature 001 包结构上**新增** `api` / `task` / `repo` / `config` 四个包；核心域（`diff`/`match`/`verdict`/`diffcover`/`coverage`/`report`）零改动被 `ScanService` 注入复用（FR-008）。web 与 CLI 共用同一 Spring 容器与核心域。

## 复杂度追踪 / Complexity Tracking

> **仅当宪法校验有需豁免的违规时填写。本特性无违规，留空。**

| 违规 | 为何需要 | 否决的更简方案 |
|------|---------|---------------|
| （无） | — | — |
