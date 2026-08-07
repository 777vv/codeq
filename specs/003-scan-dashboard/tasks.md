---
description: "扫描可视化看板（前端可视化落地）的实现任务清单"
---

# Tasks: 扫描可视化看板 / Scan Dashboard (前端可视化落地)

**Input**: 设计文档来自 `/specs/003-scan-dashboard/`（plan.md、spec.md、research.md、data-model.md、contracts/components.md、quickstart.md）

**Prerequisites**: plan.md（必需）、spec.md（必需）、data-model.md、contracts/、research.md、quickstart.md

**Tests**: 本特性规格未要求 TDD，故**不生成独立测试任务**。可选后续用 Vitest + Vue Test Utils 补组件测试。

**Organization**: 任务按 user story 分组；前端复用 feature 02 REST API（FR-009），后端零改动。

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: 可并行（不同文件、无依赖）
- **[Story]**: 所属 user story（US1/US2/US3），仅 user story 阶段标注
- 每条任务含确切文件路径（`frontend/` 子项目）

## Path Conventions / 路径约定

- 前端子项目：`frontend/`（Vue3 + Vite + TypeScript）
- 后端（feature 01/02）零改动；`.gitignore` 已忽略 `node_modules/`、`dist/`（含 `frontend/` 子目录）

---

## Phase 1: Setup（前端项目初始化）

**Purpose**: Vue3 + Vite + Monaco 项目骨架

- [x] T001 创建 `frontend/package.json`：依赖 vue（3）、vue-router、axios、monaco-editor；devDependencies：vite、@vitejs/plugin-vue、vite-plugin-monaco-editor、typescript、vue-tsc；scripts: dev/build/preview，在 `frontend/package.json`
- [x] T002 [P] 创建 `frontend/vite.config.ts`（base + `server.proxy['/api']`→`http://localhost:8080` + monaco 插件）、`frontend/tsconfig.json`、`frontend/index.html`，在 `frontend/`
- [x] T003 [P] 创建入口与类型：`frontend/src/main.ts`（挂载 + Router）、`frontend/src/App.vue`（`<router-view/>`）、`frontend/src/router/index.ts`（`/` 与 `/scans/:id`）、`frontend/src/types/index.ts`（Verdict/MethodKey/Totals/ScanForm/ScanStatus/ChangeItem/ResultView），在 `frontend/src/`

---

## Phase 2: Foundational（阻塞前置）

**Purpose**: API 访问层（所有 story 依赖）

**⚠️ CRITICAL**: 所有 user story 实现须等本阶段完成

- [x] T004 [P] `frontend/src/api/scans.ts`：axios 封装 `createScan(form)` / `getScan(id)` / `getResult(id)` / `getVerdict(id)`，baseURL `/api`，在 `frontend/src/api/scans.ts`

**Checkpoint**: API 层就绪，user story 可展开

---

## Phase 3: User Story 1 - 扫描提交与进度 (Priority: P1) 🎯 MVP

**Goal**: 表单提交 → 轮询状态 → 三色统计摘要

**Independent Test**: 表单填写提交 → 状态轮询至 SUCCESS → 显示绿/红/黄/partial 统计

### Implementation for User Story 1

- [x] T005 [P] [US1] `ScanForm.vue`：参数表单（repo/baseline/release/jacocoHost/jacocoPort/coverageXmlPath）+ 校验（必填 + jacoco 或 coverageXml 至少其一）+ `emit('submit', form)`，在 `frontend/src/components/ScanForm.vue`
- [x] T006 [US1] `ScanProgress.vue`：props `taskId`；setInterval（每 2s）轮询 `getScan(id)` 至终态；`emit('done', status, result?)` / `emit('failed', msg)`；展示状态徽标 + SUCCESS 三色统计，在 `frontend/src/components/ScanProgress.vue`（依赖 T004）
- [x] T007 [US1] `ScanFormView.vue`：组合 `ScanForm` + `ScanProgress`；提交调 `createScan` 得 taskId → 轮询；SUCCESS 跳转 `/scans/:id`，在 `frontend/src/views/ScanFormView.vue`（依赖 T005、T006）

**Checkpoint**: US1 可用——浏览器提交扫描、看进度与三色统计

---

## Phase 4: User Story 2 - 三色 Diff 可视化 (Priority: P2)

**Goal**: Monaco 三色高亮 + 按色过滤 + 方法主键

**Independent Test**: 进入结果页 → Monaco 展示变更 + 三色高亮 → 按色过滤

### Implementation for User Story 2

- [x] T008 [US2] `DiffViewer.vue`：props `changes`；Monaco Editor 展示变更文件，按 verdict 着色（绿/红/黄/partial）；颜色过滤器（all/green/red/yellow/partial）；展示方法主键（className+signature+route），在 `frontend/src/components/DiffViewer.vue`（依赖 T003 monaco 配置）
- [x] T009 [US2] `ResultView.vue`：`getResult(id)` 拉取结果 → `DiffViewer` 展示；显示 pass + 三色统计，在 `frontend/src/views/ResultView.vue`（依赖 T004、T008）

**Checkpoint**: US2 可用——Monaco 三色 Diff 可视化

---

## Phase 5: User Story 3 - 报告导出 (Priority: P3)

**Goal**: 导出 HTML 报告（含统计 + 三色 Diff）

**Independent Test**: 结果页点「导出」→ 下载 `report-<id>.html` → 离线打开完整

### Implementation for User Story 3

- [x] T010 [US3] `ExportButton.vue`：props `result`；点击 → 拼 HTML（统计 + 变更明细 + 三色）→ 触发下载 `report-<taskId>.html`；集成进 `ResultView.vue`，在 `frontend/src/components/ExportButton.vue`、`frontend/src/views/ResultView.vue`（依赖 T009）

**Checkpoint**: US3 可用——HTML 报告导出

---

## Phase 6: Polish（收尾与横切）

**Purpose**: 生产集成 + 文档 + 验证

- [x] T011 [P] 生产构建集成：`npm run build` → 将 `frontend/dist` 拷到 `src/main/resources/static`（构建脚本或文档说明，前后端不分离，宪法 4.3），在 `frontend/README.md` 或构建脚本
- [x] T012 [P] 对齐主 README：补「前端可视化」章节（dev: `cd frontend && npm run dev`；prod 构建托管），在 `README.md`
- [ ] T013 运行 quickstart 端到端验证（后端 :8080 + 前端 :5173 + 浏览器三场景），对照 `specs/003-scan-dashboard/quickstart.md`

---

## Dependencies & Execution Order / 依赖与执行顺序

### Phase Dependencies / 阶段依赖
- **Setup（Phase 1）**: 无依赖，立即开始
- **Foundational（Phase 2）**: 依赖 Setup——**阻塞所有 user story**
- **User Stories（Phase 3+）**: 均依赖 Foundational；US1→US2→US3（结果页渐进）
- **Polish（Phase 6）**: 依赖期望完成的 user story

### User Story Dependencies / 各 story 依赖
- **US1（P1）**: Foundational 完成即可开始；**不依赖其他 story**
- **US2（P2）**: 依赖 ResultView（T009）+ US1 的扫描完成态
- **US3（P3）**: 依赖 ResultView（T009/T010 同组件）

### Within Each User Story / story 内部
- 组件 → 视图组合；Monaco 在 US2

### Parallel Opportunities / 并行机会
- Phase 1 的 T002、T003 互不冲突
- Phase 2 T004（API）独立
- US1 的 T005（ScanForm）/T006（ScanProgress）不同文件（T007 依赖二者）

---

## Parallel Example: Phase 1 Setup / 并行示例

```bash
# Setup 内可并行启动（不同文件）：
Task: "vite.config.ts / tsconfig.json / index.html in frontend/"
Task: "main.ts / App.vue / router / types in frontend/src/"
```

---

## Implementation Strategy / 实施策略

### MVP First（仅 User Story 1）/ 优先 MVP
1. 完成 Phase 1 Setup（项目骨架）
2. 完成 Phase 2 Foundational（API 层）
3. 完成 Phase 3 User Story 1（T005–T007）
4. **停下验证**：浏览器提交扫描 → 看进度 → 三色统计

### Incremental Delivery / 增量交付
1. Setup + Foundational → 前端可启动（空页面 + API 层）
2. + US1 → 提交 + 进度 + 统计（MVP）
3. + US2 → Monaco 三色 Diff
4. + US3 → 报告导出
5. Polish → 生产集成 + 文档 + 验证

---

## Notes / 备注

- `[P]` = 不同文件、无依赖，可并行
- `[Story]` 标签映射到 user story
- **复用 feature 02 REST API**（FR-009）：前端只调 `/api/scans`，后端零改动
- 测试任务未生成（spec 未要求 TDD）；建议后续用 Vitest 补组件测试
- 每个任务或逻辑分组后提交（作者 `wangtao <wangtao>`）
- 前端构建需 Node.js 20+ / npm；prod 由 Spring Boot 同源托管（宪法 4.3 不分离）
- 宪法 VIII/IX 针对 Java；前端 TS 用 `console.*`，不直接约束（见 plan 适用性说明）
- 避免：模糊任务、同文件冲突、前端重复实现后端判定逻辑
