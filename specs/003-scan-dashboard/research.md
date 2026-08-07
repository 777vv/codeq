# 研究：扫描可视化看板 / Research: Scan Dashboard

**特性 (Feature)**: 003-scan-dashboard | **阶段 (Phase)**: 0（研究）| **日期 (Date)**: 2026-08-06
**输入 (Input)**: [plan.md](plan.md) 技术上下文与宪法校验

本特性为 feature 02 扫描服务构建 Vue3 + Monaco 前端。技术决策如下（无 NEEDS CLARIFICATION 遗留）。

## 1. 前端框架：Vue3 + Vite + TypeScript

**Decision**：Vue3（`<script setup>`）+ Vite + TypeScript。
**Rationale**：宪法 4.3 强制 Vue3；Vite 是 Vue3 官方推荐构建工具，HMR 快；TS 类型安全。
**Alternatives**：Nuxt（SSR）——本特性 SPA 足够，Nuxt 过重。**否决**。

## 2. Monaco 集成：vite-plugin-monaco-editor

**Decision**：`monaco-editor` + `vite-plugin-monaco-editor`（Vite 原生集成，按需加载语言/Worker）。
**Rationale**：宪法 4.3 强制 Monaco Editor；该插件解决 Web Worker 打包问题，社区主流。
**Alternatives**：`@guolao/vue-monaco-editor`（Vue 包装）——可行但多一层抽象；直接用 `monaco-editor` 更可控。**否决**（可后续替换）。

## 3. HTTP 与轮询：axios + setInterval 轮询

**Decision**：axios 封装 `/api/scans`；扫描状态用 setInterval（每 2s）轮询至终态，组件卸载清除。
**Rationale**：轮询简单可靠；feature 02 是 HTTP REST，轮询自然契合，无需后端改造。
**Alternatives**：SSE / WebSocket 实时推送——feature 02 未提供，需后端改造；MVP 轮询足够。**否决**（后续可加）。

## 4. 路由：Vue Router

**Decision**：Vue Router（`/` 提交表单、`/scans/:id` 结果）。
**Rationale**：多视图（表单/结果）需路由；Vue Router 是 Vue3 官方方案。
**Alternatives**：手动状态切换——多视图管理混乱。**否决**。

## 5. 报告导出：前端拼 HTML

**Decision**：前端基于 `GET /result` 返回的 JSON（统计 + changes），用模板拼 HTML 报告，触发下载。
**Rationale**：复用已有数据，无需后端新增端点；与 feature 01 CLI HTML 报告形态对齐。
**Alternatives**：后端加 `/export` 端点——需改动 feature 02；前端拼更独立。**否决**（后续可加后端导出）。

## 6. 前后端集成（宪法 4.3 不分离）

**Decision**：
- dev：Vite dev server（5173）配置 `server.proxy['/api']` → `http://localhost:8080`（后端）。
- prod：`vite build` → `frontend/dist` → 由 Spring Boot 作为静态资源托管（拷到 `src/main/resources/static` 或配 resource location）。
**Rationale**：宪法 4.3「前后端不分离项目」；同源托管避免 CORS；dev proxy 解决本地联调。
**Alternatives**：独立前端部署（Nginx）——违反「不分离」。**否决**。

## 7. 复用 feature 02 REST API

**Decision**：前端只调 feature 02 现有接口（`POST/GET /api/scans`、`/result`、`/verdict`），不重复判定。
**Rationale**：spec FR-009；后端判定单一来源。
**Alternatives**：前端实现判定——违反职责分离 + 重复逻辑。**否决**。

## 宪法 VIII / IX 适用性说明

宪法 VIII（SLF4J 日志）与 IX（Java 类头）针对 **Java** 代码。本 feature 为前端 TypeScript/Vue，不受 VIII/IX 直接约束（前端用 `console.*`、组件无 Java 类头）。若后续为本 feature 新增 Java（如后端导出端点），须遵循 VIII/IX。

## 结论 / Summary

所有决策符合宪法（4.3 Vue3/Monaco/不分离、迭代 #3），复用 feature 02，无 NEEDS CLARIFICATION。可进入 Phase 1。
