# 快速验证：扫描可视化看板 / Quickstart

**特性 (Feature)**: 003-scan-dashboard | **阶段 (Phase)**: 1 | **日期 (Date)**: 2026-08-06

## 前置 / Prerequisites
- Node.js 20+、npm
- 后端 feature 02 运行中（dev：`java -jar target/codeq-0.1.0-SNAPSHOT.jar`，监听 8080，H2）
- 业务项目（测试环境挂 Jacoco agent）

## 安装与开发 / Dev
```bash
cd frontend
npm install
npm run dev      # Vite dev server: http://localhost:5173（proxy /api → :8080）
```
浏览器打开 http://localhost:5173

## 场景 1：US1 提交 + 进度
- 表单填 repo / baseline / release + Jacoco 端点 → 「开始扫描」
- 观察 PENDING → RUNNING → SUCCESS
- 显示三色统计

## 场景 2：US2 三色 Diff
- 进入结果页（`/scans/:id`）→ Monaco 展示变更 + 三色高亮
- 按颜色过滤（只看红色漏测）

## 场景 3：US3 导出
- 点「导出报告」→ 下载 `report-<id>.html` → 离线打开

## 生产构建（前后端不分离，宪法 4.3）
```bash
cd frontend && npm run build      # → frontend/dist
# 将 dist 内容拷到 src/main/resources/static（由 Spring Boot 同源托管）
java -jar target/codeq-0.1.0-SNAPSHOT.jar   # 浏览器访问 http://localhost:8080
```

## 验证矩阵（对照 spec）
| spec 场景 | 操作 | 期望 |
|---|---|---|
| US1-AC1 | 表单提交 | taskId + `PENDING` |
| US1-AC2 | 轮询 | 状态更新至终态 |
| US2-AC1 | 结果页 | Monaco 三色高亮 |
| US3-AC1 | 导出 | 下载 HTML 报告 |

> 完整实现与测试在 `tasks.md` / 实现阶段产出；本文件仅验证指南。
