# 实施计划：扫描可视化看板（前端可视化落地）/ Implementation Plan: Scan Dashboard

**分支 (Branch)**: `003-scan-dashboard` | **日期 (Date)**: 2026-08-06 | **规格 (Spec)**: [spec.md](spec.md)

**输入 (Input)**: 来自 `specs/003-scan-dashboard/spec.md` 的功能规格

**注 (Note)**: 本文件由 `/speckit-plan` 命令填写；命令定义描述了执行工作流。

## 摘要 / Summary

为 feature 02 扫描服务构建 Vue3 + Monaco Editor 前端（迭代 #3）：扫描提交表单 → 轮询进度 → 三色统计摘要 → Monaco 三色 Diff 可视化（按色过滤）→ HTML 报告导出。前端只调 feature 02 REST API，不重复判定逻辑（FR-009）。前后端不分离：dev 用 Vite proxy，prod 由 Spring Boot 托管静态资源（宪法 4.3）。

## 技术上下文 / Technical Context

**语言/版本 (Language/Version)**: TypeScript 5 + Vue3（前端）；后端沿用 Java 21（feature 02，不改动）

**主依赖 (Primary Dependencies)**:
- Vue3（`<script setup>`）+ Vite + vue-tsc
- `vue-router`（路由）
- `axios`（HTTP / 轮询）
- `monaco-editor` + `vite-plugin-monaco-editor`（三色 Diff 可视化，宪法 4.3）

**复用**: feature 02 REST API（`POST/GET /api/scans`、`/result`、`/verdict`），后端不改动

**存储 (Storage)**: N/A（前端无持久化，状态组件内）

**目标平台 (Target Platform)**: 现代浏览器（Chrome/Edge/Firefox）

**项目类型 (Project Type)**: web-app（前端 SPA，前后端不分离）

**构建 (Build)**: Vite（`npm run build` → `frontend/dist`）；prod 拷到 Spring Boot `src/main/resources/static`

**集成 (Integration)**:
- dev：Vite dev server（5173）`server.proxy['/api']` → `http://localhost:8080`
- prod：`vite build` 产物由 Spring Boot 同源托管（避免 CORS）

**约束 (Constraints)**:
- 仅调 feature 02 REST API（FR-009），前端不实现判定
- 前后端不分离（宪法 4.3）
- Monaco Diff 三色高亮严格对应 verdict

**规模/范围 (Scale/Scope)**: 单 SPA；本迭代不含鉴权 UI（沿用 feature 02 内网信任）、不含 AST 优化（迭代 #4）、不含发布门禁对接（迭代 #5）。

## 宪法校验 / Constitution Check

*门禁 (GATE): Phase 0 研究前通过；Phase 1 设计后复检。*

| 宪法原则 | 校验 | 状态 |
|---|---|---|
| 4.3 前端 Vue3 + Monaco Editor | Vue3 + monaco-editor | ✅ 通过 |
| 4.3 前后端不分离项目 | Vite build → Spring Boot static 托管 | ✅ 通过 |
| 复用 feature 02 判定（FR-009） | 前端只调 REST API | ✅ 通过 |
| 迭代顺序 第六篇 | 本 feature = 迭代 #3「前端可视化落地」 | ✅ 通过 |
| VIII. 日志规约 | 针对 Java；前端 TS 用 `console.*`，不直接约束（见 research） | ✅ 适用性说明 |
| IX. 类头规约 | 针对 Java；本 feature 无新增 Java 类，不约束（若加 Java 须遵循） | ✅ 适用性说明 |

**结论**: 无违规，门禁通过。**无需 Complexity Tracking 豁免。**

> 适用性说明：宪法 VIII（SLF4J）/ IX（Java 类头）针对 Java 代码。本 feature 为前端 TypeScript/Vue，不受其直接约束（前端用 `console.*`、组件无 Java 类头）。后续若为本 feature 新增 Java（如后端导出端点），须遵循 VIII/IX。

## 项目结构 / Project Structure

### 本特性文档 / Documentation (this feature)

```text
specs/003-scan-dashboard/
├── plan.md              # 本文件
├── research.md          # Phase 0
├── data-model.md        # Phase 1（TS 类型 + 组件树）
├── contracts/components.md  # Phase 1（组件契约）
├── quickstart.md        # Phase 1
└── tasks.md             # /speckit-tasks（本命令不创建）
```

### 源码（前端子项目）/ Source Code (frontend)

```text
frontend/
├── package.json
├── vite.config.ts            # base + server.proxy['/api']→:8080 + monaco plugin
├── tsconfig.json
├── index.html
└── src/
    ├── main.ts               # 挂载 + Router
    ├── App.vue
    ├── router/index.ts       # / 与 /scans/:id
    ├── api/scans.ts          # axios 封装（POST/GET/result/verdict）
    ├── types/index.ts        # Verdict/MethodKey/Totals/ScanForm/ScanStatus/ChangeItem/ResultView
    ├── views/
    │   ├── ScanFormView.vue  # US1：表单 + 提交 + 进度
    │   └── ResultView.vue    # US2/US3：Diff + 导出
    └── components/
        ├── ScanForm.vue
        ├── ScanProgress.vue  # 轮询 + 三色摘要
        ├── DiffViewer.vue    # Monaco 三色高亮 + 过滤
        └── ExportButton.vue  # 拼 HTML 下载
```

**结构决策 (Structure Decision)**: 独立 `frontend/` 子项目（Vue3 + Vite + TS）；后端（feature 01/02）零改动。dev 经 Vite proxy 联调；prod `vite build` 产物拷到 Spring Boot `src/main/resources/static` 同源托管。

## 复杂度追踪 / Complexity Tracking

> **仅当宪法校验有需豁免的违规时填写。本特性无违规，留空。**

| 违规 | 为何需要 | 否决的更简方案 |
|------|---------|---------------|
| （无） | — | — |
