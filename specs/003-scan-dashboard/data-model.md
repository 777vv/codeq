# 数据模型：扫描可视化看板 / Data Model

**特性 (Feature)**: 003-scan-dashboard | **阶段 (Phase)**: 1（设计）| **日期 (Date)**: 2026-08-06

本 feature 为前端，**无持久化**（消费 feature 02 API）。下列为 TypeScript 类型与组件树。

## TypeScript 类型（src/types，对齐 feature 02 API）

- `Verdict = 'GREEN' | 'RED' | 'YELLOW' | 'PARTIAL'`
- `MethodKey { className: string; signature: string; route?: string }`
- `Totals { green: number; red: number; yellow: number; partial: number }`
- `ScanForm { repo: string; baseline: string; release: string; jacocoHost?: string; jacocoPort?: number; coverageXmlPath?: string; taskId?: string }`
- `ScanStatus { taskId: string; status: 'PENDING'|'RUNNING'|'SUCCESS'|'FAILED'; repo: string; baseline: string; release: string; createdAt: string; startedAt?: string; finishedAt?: string; errorMsg?: string }`
- `ChangeItem { file: string; methodKey?: MethodKey; verdict: Verdict; uncoveredLines?: number[] }`
- `ResultView { taskId: string; pass: boolean; totals: Totals; changes: ChangeItem[] }`

## 组件树 / Component Tree

```text
App.vue
└── <router-view>
    ├── ScanFormView.vue          (US1: 表单 + 提交 + 进度)
    │   ├── ScanForm.vue          (参数表单 + 校验)
    │   └── ScanProgress.vue      (状态轮询 + 三色摘要)
    └── ResultView.vue            (US2/US3: Diff 可视化 + 导出)
        ├── DiffViewer.vue        (Monaco 三色高亮 + 按色过滤)
        └── ExportButton.vue      (导出 HTML 报告)
```

## 组件状态（MVP 组件内 state，无 Pinia）

- **ScanFormView**：form 数据、taskId、status（轮询）、result（SUCCESS 后跳转结果页）。
- **ResultView**：result（`GET /result`）、颜色过滤器。

## API 映射 / API Mapping

| 前端动作 | 后端 API |
|---|---|
| 提交扫描 | `POST /api/scans` → taskId |
| 轮询状态 | `GET /api/scans/{id}` → ScanStatus |
| 取结果/Diff | `GET /api/scans/{id}/result` → ResultView |
| 门禁判定 | `GET /api/scans/{id}/verdict`（可选展示） |
