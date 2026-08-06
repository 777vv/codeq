# 研究：扫描服务化（核心接口服务化）/ Research: Scan Service

**特性 (Feature)**: 002-scan-service | **阶段 (Phase)**: 0（研究）| **日期 (Date)**: 2026-08-06
**输入 (Input)**: [plan.md](plan.md) 技术上下文与宪法校验

本特性将 feature 001 的命令行核心判定链路封装为后端服务（REST + 异步 + 持久化）。技术决策如下（无 NEEDS CLARIFICATION 遗留）。

## 1. REST 框架：Spring MVC

**Decision**：`spring-boot-starter-web`（Spring MVC，同步 servlet 容器）。
**Rationale**：Spring Boot 生态主流、团队熟悉；feature 001 已用 Spring Boot。
**Alternatives**：WebFlux（响应式）——异步非阻塞，但本特性扫描是重 CPU/IO 单任务，`@Async` 已足够；WebFlux 学习成本高。**否决**。

## 2. 异步执行：Spring `@Async` + ThreadPoolTaskExecutor

**Decision**：`@Async` + 自定义 `ThreadPoolTaskExecutor`；`TaskDecorator` 把 `traceId` 从提交线程透传到执行线程。
**Rationale**：宪法第四篇 4.3「任务全异步化」；进程内 `@Async` 简单可靠，满足「提交即返回、后台执行」。
**Alternatives**：
- 消息队列（RabbitMQ/Kafka）：解耦、可重试、可水平扩展，但引入中间件运维成本；MVP 过重，留待规模化。
- Spring Batch：批处理框架，扫描是单任务非批，过重。**否决**。

## 3. 持久化：Spring Data JPA + PostgreSQL(prod) / H2(dev)

**Decision**：JPA（Hibernate）；生产 PostgreSQL，开发/测试 H2 内存（profile 切换）。
**Rationale**：JPA 成熟、Spring Data 简化 repository；PostgreSQL 生产级；H2 让 dev 零依赖运行。
**Alternatives**：
- MySQL：可行、团队偏好相关；本特性选 PostgreSQL（功能更全），切换成本低。
- MyBatis：SQL 灵活但样板多；JPA 上手更快。**否决**。
- 纯文件/JSON 存储：查询/回溯弱，违反持久化诉求（spec FR-005/FR-006）。**否决**。

## 4. API 认证：MVP 内网信任（不加）

**Decision**：MVP 不加认证（假设内网部署），预留鉴权扩展点。
**Rationale**：本迭代聚焦能力服务化（spec 无认证 FR）；认证可在后续迭代或网关层加。
**Alternatives**：API Token（Header）——简单，MVP 可加，当前留扩展点；OAuth2/JWT——过重，本阶段无多租户诉求。**否决**。

## 5. TTL 过期清理：`@Scheduled` 定时任务

**Decision**：`@Scheduled`（如每日）删除 N 天前的扫描任务与结果。
**Rationale**：宪法 4.3「数据生命周期 TTL 过期清理」；`@Scheduled` 应用层简单可控。
**Alternatives**：数据库 TTL（PG 分区/事件触发）——复杂；应用层 `@Scheduled` 足够。**否决**。

## 6. 复用 feature 001 核心逻辑

**Decision**：直接注入 feature 001 的 `@Component`（`GitDiffService`/`AstMatcher`/`DiffCoverRunner`/`VerdictEngine`/`JacocoCollector`/`CoverageReportConverter`/`ExecutionDataValidator`），由 `ScanService` 编排。
**Rationale**：feature 001 核心域已是 Spring `@Component`、职责分离（plan 001 结构决策）；服务化直接复用，零重写，满足 spec FR-008。
**Alternatives**：重写——违反 DRY 且风险高。**否决**。

## 7. traceId 贯穿异步（宪法 VIII）

**Decision**：扫描任务创建时生成 `traceId` 存入 `ScanTask` + `MDC`；`@Async` 经 `TaskDecorator` 把 `traceId` 透传到执行线程，日志全程关联。
**Rationale**：宪法 VIII「带 traceId（MDC）」；异步场景 MDC 默认不跨线程，须 `TaskDecorator` 传递。
**Alternatives**：手动传参——侵入性强；`TaskDecorator` 透明传递最优。

## 结论 / Summary

所有决策符合宪法（I–VIII、4.3 异步/TTL、迭代 #2），无 NEEDS CLARIFICATION 遗留，无违规。可进入 Phase 1 设计。
