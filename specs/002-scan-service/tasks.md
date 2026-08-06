---
description: "扫描服务化（核心接口服务化）的实现任务清单"
---

# Tasks: 扫描服务化 / Scan Service (核心接口服务化)

**Input**: 设计文档来自 `/specs/002-scan-service/`（plan.md、spec.md、research.md、data-model.md、contracts/api.md、quickstart.md）

**Prerequisites**: plan.md（必需）、spec.md（必需）、data-model.md、contracts/、research.md、quickstart.md

**Tests**: 本特性规格未要求 TDD，故**不生成独立测试任务**。JUnit 5 + Spring Boot Test（H2）依赖将纳入；实现时建议补 API/集成测试，如需 TDD 任务可后续追加。

**Organization**: 任务按 user story 分组，复用 feature 001 核心域（`diff`/`match`/`verdict`/`diffcover`/`coverage`/`report`）。

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: 可并行（不同文件、无依赖）
- **[Story]**: 所属 user story（US1/US2/US3），仅 user story 阶段标注
- 每条任务含确切文件路径

## Path Conventions / 路径约定

- 单 Maven 项目：源码 `src/main/java/com/codeq/`，资源 `src/main/resources/`
- feature 001 核心域包（`diff`/`coverage`/`match`/`diffcover`/`verdict`/`report`/`process`/`model`）**零改动复用**

---

## Phase 1: Setup（项目初始化）

**Purpose**: 引入服务化依赖与配置

- [x] T001 在 `pom.xml` 增加依赖：`spring-boot-starter-web`、`spring-boot-starter-data-jpa`、`spring-boot-starter-validation`、`org.postgresql:postgresql`（runtime）、`com.h2database:h2`（runtime/test），在 `pom.xml`
- [x] T002 [P] 新增 `application.yml`：dev=H2 内存 / prod=PostgreSQL 数据源、`spring.jpa.hibernate.ddl-auto`、扫描线程池大小、TTL 保留天数，在 `src/main/resources/application.yml`
- [x] T003 [P] 创建包结构目录 `src/main/java/com/codeq/{api,api/dto,task,repo,config}/`

---

## Phase 2: Foundational（阻塞前置，所有 user story 依赖）

**Purpose**: 持久化、异步、异常处理、启动模式 —— 所有 story 的基础设施

**⚠️ CRITICAL**: 所有 user story 实现须等本阶段完成

- [x] T004 [P] `ScanTaskEntity`（JPA，表 `scan_task`：id/repo/baseline/release/jacoco 端点/status/isolationKey/traceId/errorMsg/时间戳），在 `src/main/java/com/codeq/repo/ScanTaskEntity.java`
- [x] T005 [P] `ScanResultEntity`（JPA，表 `scan_result`：id/task@OneToOne/pass/green/red/yellow/partial/changes(JSON)/createdAt），在 `src/main/java/com/codeq/repo/ScanResultEntity.java`
- [x] T006 [P] `ScanTaskRepository`、`ScanResultRepository`（Spring Data JPA），在 `src/main/java/com/codeq/repo/ScanTaskRepository.java`、`ScanResultRepository.java`
- [x] T007 [P] `AsyncConfig`：`@EnableAsync` + `ThreadPoolTaskExecutor` + `TaskDecorator`（把 `traceId` 从 MDC 透传到异步线程，宪法 VIII），在 `src/main/java/com/codeq/task/AsyncConfig.java`
- [x] T008 [P] `GlobalExceptionHandler`（`@RestControllerAdvice`：400 参数 / 404 不存在 / 409 状态不允许 / 500 内部，响应带 `traceId`），在 `src/main/java/com/codeq/api/GlobalExceptionHandler.java`
- [x] T009 改造 `CodeqCli` 为**双模式入口**：检测命令行——含 picocli 子命令（check/dump/reset）→ CLI 执行后退出；否则 → Spring Boot web 服务常驻，在 `src/main/java/com/codeq/CodeqCli.java`

**Checkpoint**: 基础设施就绪，可启动 web 服务（空 controller），user story 可展开

---

## Phase 3: User Story 1 - 异步扫描 API (Priority: P1) 🎯 MVP

**Goal**: POST 提交扫描 → 异步执行（复用 feature 001 核心）→ 查询状态/结果

**Independent Test**: POST `/api/scans` → taskId(PENDING) → 轮询至 SUCCESS → GET 结果三色明细（与 `codeq check` 一致）

### Implementation for User Story 1

- [x] T010 [P] [US1] DTO：`CreateScanRequest`、`ScanView`、`ResultView`，在 `src/main/java/com/codeq/api/dto/`
- [x] T011 [US1] `ScanService`：创建 `PENDING` 任务落库 → `@Async` 执行（注入 feature 001 核心 `@Component`：版本校验 → 数据源 → diff → AST → diff-cover → verdict）→ 更新 `SUCCESS`/`FAILED` + 落 `ScanResult`；执行前 `MDC.put(traceId)`，在 `src/main/java/com/codeq/task/ScanService.java`（依赖 T004–T007 + 核心 @Component）
- [x] T012 [US1] `ScanController`：`POST /api/scans`（202+taskId）、`GET /api/scans/{id}`（状态）、`GET /api/scans/{id}/result`（三色+明细，未完成 409），在 `src/main/java/com/codeq/api/ScanController.java`（依赖 T010、T011）

**Checkpoint**: US1 可用——HTTP 提交扫描，轮询获取三色判定（与 CLI 等价）

---

## Phase 4: User Story 2 - 持久化与历史回溯 (Priority: P2)

**Goal**: 按 项目/版本/时间 回溯历史扫描

**Independent Test**: 多次扫描后 `GET /api/scans?project=&version=` 返回列表，每条含统计

### Implementation for User Story 2

- [x] T013 [US2] 历史列表查询：`ScanTaskRepository` 加动态查询（project/version/status/时间范围 + 分页 `Pageable`），`ScanController` 加 `GET /api/scans`（分页响应），在 `src/main/java/com/codeq/repo/ScanTaskRepository.java`、`src/main/java/com/codeq/api/ScanController.java`（依赖 T006、T012）

**Checkpoint**: US2 可用——历史扫描可按维度回溯

---

## Phase 5: User Story 3 - 标准化门禁判定 (Priority: P3)

**Goal**: 发布平台查询「是否可上线」判定

**Independent Test**: 完成扫描 → `GET /api/scans/{id}/verdict` → `{pass, totals}`；有 RED → pass=false

### Implementation for User Story 3

- [x] T014 [US3] 门禁判定端点：`ScanController` 加 `GET /api/scans/{id}/verdict` → `VerdictView{pass, totals}`（存在 RED → pass=false，宪法红线），在 `src/main/java/com/codeq/api/ScanController.java`、`src/main/java/com/codeq/api/dto/VerdictView.java`（依赖 T005）

**Checkpoint**: US3 可用——门禁判定查询就绪（为迭代 #5 铺垫）

---

## Phase 6: Polish（收尾与横切）

**Purpose**: TTL、profile、文档、门禁合规

- [x] T015 [P] `RetentionJob`：`@Scheduled`（如每日）删除超过保留期的 `scan_task`/`scan_result`（宪法 4.3 TTL），在 `src/main/java/com/codeq/config/RetentionJob.java`
- [x] T016 [P] `SchedulingConfig`：`@EnableScheduling`，在 `src/main/java/com/codeq/config/SchedulingConfig.java`
- [x] T017 [P] 对齐 README（服务模式/启动/profile）与 quickstart（curl 场景），在 `README.md`、`specs/002-scan-service/quickstart.md`
- [ ] T018 代码门禁合规：`mvn fmt:format` 格式化 + 清 PMD(p3c)/SpotBugs 告警（宪法 VII），在各 Java 文件
- [ ] T019 运行 quickstart 端到端验证（dev H2 + curl 三场景 + 验证矩阵），对照 `specs/002-scan-service/quickstart.md`

---

## Dependencies & Execution Order / 依赖与执行顺序

### Phase Dependencies / 阶段依赖
- **Setup（Phase 1）**: 无依赖，立即开始
- **Foundational（Phase 2）**: 依赖 Setup——**阻塞所有 user story**
- **User Stories（Phase 3+）**: 均依赖 Foundational；可并行（若有人力）或按优先级 P1→P2→P3
- **Polish（Phase 6）**: 依赖期望完成的 user story

### User Story Dependencies / 各 story 依赖
- **US1（P1）**: Foundational 完成即可开始；**不依赖其他 story**（复用 feature 001 核心）
- **US2（P2）**: 依赖 US1 的 controller/repository 基础（T012/T013 同 controller 加端点）
- **US3（P3）**: 依赖 ScanResult 持久化（T005）+ US1 完成态

### Within Each User Story / story 内部
- DTO → Service → Controller；核心判定复用 feature 001

### Parallel Opportunities / 并行机会
- Phase 1 的 T002、T003 互不冲突
- Phase 2 的 T004–T008 不同文件，可并行（T009 改 main 依赖概念上但可并行）
- US1 的 T010（DTO）与 T011（Service）不同文件可并行（T012 依赖二者）

---

## Parallel Example: Phase 2 Foundational / 并行示例

```bash
# Foundational 内可并行启动（不同文件）：
Task: "ScanTaskEntity in src/main/java/com/codeq/repo/ScanTaskEntity.java"
Task: "ScanResultEntity in src/main/java/com/codeq/repo/ScanResultEntity.java"
Task: "AsyncConfig in src/main/java/com/codeq/task/AsyncConfig.java"
Task: "GlobalExceptionHandler in src/main/java/com/codeq/api/GlobalExceptionHandler.java"
```

---

## Implementation Strategy / 实施策略

### MVP First（仅 User Story 1）/ 优先 MVP
1. 完成 Phase 1 Setup（依赖 + 配置）
2. 完成 Phase 2 Foundational（持久化 + 异步 + 异常 + 双模式）
3. 完成 Phase 3 User Story 1（T010–T012）
4. **停下验证**：POST 扫描 → 轮询 → GET 结果，与 `codeq check` 一致

### Incremental Delivery / 增量交付
1. Setup + Foundational → 服务可启动
2. + US1 → 异步扫描 API 可用（MVP）
3. + US2 → 历史回溯
4. + US3 → 门禁判定查询
5. Polish → TTL + 门禁合规 + 文档

---

## Notes / 备注

- `[P]` = 不同文件、无依赖，可并行
- `[Story]` 标签映射到 user story，便于追溯
- **复用 feature 001 核心**（FR-008）：`ScanService` 注入 `@Component`，零重写
- 测试任务未生成（spec 未要求 TDD）；建议为 `ScanService`/`ScanController` 补 Spring Boot Test（H2）
- 每个任务或逻辑分组后提交（作者 `wangtao <wangtao>`）
- 宪法合规：VII 门禁（T018）、VIII 日志（traceId 经 TaskDecorator 透传，T007/T011）、4.3 异步/TTL（T011/T015）
- 避免：模糊任务、同文件冲突、重写 feature 001 核心
