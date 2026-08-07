<script setup lang="ts">
import type { ResultView, Verdict } from '../types';

// US3 导出 HTML 报告（统计 + 三色明细），前端基于 result JSON 拼装。
const props = defineProps<{ result: ResultView }>();

const COLOR: Record<Verdict, string> = { GREEN: '#2a8', RED: '#d33', YELLOW: '#c80', PARTIAL: '#36c' };

function buildHtml(r: ResultView): string {
  const rows = r.changes.map(c => {
    const mk = c.methodKey;
    const where = mk ? `${mk.className}#${mk.signature}${mk.route ? ' [' + mk.route + ']' : ''}` : '(方法外)';
    return `<tr><td style="color:${COLOR[c.verdict]}">${c.verdict}</td><td>${c.file}</td><td>${where}</td><td>${c.uncoveredLines?.join(',') || ''}</td></tr>`;
  }).join('');
  return `<!doctype html><html lang="zh"><head><meta charset="utf-8"><title>codeq 报告 ${r.taskId}</title>
<style>body{font-family:sans-serif;padding:16px}table{border-collapse:collapse;width:100%}td,th{border:1px solid #ddd;padding:6px;text-align:left}</style>
</head><body>
<h2>codeq 覆盖判定报告</h2>
<p>任务 <code>${r.taskId}</code> ｜ 判定：<b style="color:${r.pass ? '#2a8' : '#d33'}">${r.pass ? '可上线' : '禁止上线（存在 RED）'}</b></p>
<p>🟢 ${r.totals.green} ｜ 🔴 ${r.totals.red} ｜ 🟡 ${r.totals.yellow} ｜ ◔ ${r.totals.partial}</p>
<table><tr><th>verdict</th><th>file</th><th>method</th><th>未覆盖行</th></tr>${rows}</table>
</body></html>`;
}

function exportHtml() {
  const blob = new Blob([buildHtml(props.result)], { type: 'text/html;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `report-${props.result.taskId}.html`;
  a.click();
  URL.revokeObjectURL(url);
}
</script>

<template>
  <button @click="exportHtml">导出 HTML 报告</button>
</template>
