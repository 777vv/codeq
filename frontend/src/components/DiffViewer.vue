<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue';
import * as monaco from 'monaco-editor';
import editorWorker from 'monaco-editor/esm/vs/editor/editor.worker?worker';
import type { ChangeItem, Verdict } from '../types';

// US2 Monaco 三色 Diff 可视化（宪法 4.3 Monaco Editor）。
// Monaco Web Worker（Vite ?worker；plaintext 编辑器仅需 editor worker）。
(self as unknown as { MonacoEnvironment: monaco.Environment }).MonacoEnvironment = {
  getWorker: () => new editorWorker()
};

// 注入三色行背景 CSS（Monaco decoration className 生效）。
if (!document.getElementById('codeq-verdict-css')) {
  const style = document.createElement('style');
  style.id = 'codeq-verdict-css';
  style.textContent = `
    .v-GREEN { background: #e6f7ec !important; }
    .v-RED { background: #fde8e8 !important; }
    .v-YELLOW { background: #fff5e0 !important; }
    .v-PARTIAL { background: #e8f0fd !important; }
  `;
  document.head.appendChild(style);
}

const props = defineProps<{ changes: ChangeItem[] }>();
const container = ref<HTMLElement>();
const filter = ref<'ALL' | Verdict>('ALL');
let editor: monaco.editor.IStandaloneCodeEditor | null = null;
let decorations: string[] = [];

const ICON: Record<Verdict, string> = { GREEN: '🟢', RED: '🔴', YELLOW: '🟡', PARTIAL: '◔' };

function visible(): ChangeItem[] {
  return props.changes.filter(c => filter.value === 'ALL' || c.verdict === filter.value);
}

function buildText(): string {
  const items = visible();
  if (items.length === 0) return '(无变更)';
  return items.map(c => {
    const mk = c.methodKey;
    const where = mk ? `${mk.className}#${mk.signature}${mk.route ? ' [' + mk.route + ']' : ''}` : '(方法外 / 无法归约)';
    const uncov = c.uncoveredLines?.length ? `  未覆盖: ${c.uncoveredLines.join(',')}` : '';
    return `${ICON[c.verdict]} ${c.verdict}\t${c.file}\t${where}${uncov}`;
  }).join('\n');
}

function render() {
  if (!editor) return;
  const model = editor.getModel();
  if (model) model.setValue(buildText());
  const decos: monaco.editor.IModelDeltaDecoration[] = visible().map((c, i) => ({
    range: new monaco.Range(i + 1, 1, i + 1, 1),
    options: { isWholeLine: true, className: `v-${c.verdict}` }
  }));
  decorations = editor.deltaDecorations(decorations, decos);
}

onMounted(() => {
  editor = monaco.editor.create(container.value!, {
    value: buildText(), language: 'plaintext', readOnly: true,
    minimap: { enabled: false }, lineNumbers: 'on', fontSize: 13
  });
  render();
});

watch(filter, render);
watch(() => props.changes, render, { deep: true });
onUnmounted(() => editor?.dispose());
</script>

<template>
  <div class="filters">
    <button v-for="f in (['ALL', 'GREEN', 'RED', 'YELLOW', 'PARTIAL'] as const)" :key="f"
            @click="filter = f" :class="{ on: filter === f }">{{ f }}</button>
  </div>
  <div ref="container" class="monaco"></div>
</template>

<style scoped>
.monaco { height: 480px; border: 1px solid #ddd; }
.filters { margin-bottom: 8px; }
.filters button { margin-right: 6px; }
.filters button.on { font-weight: bold; border: 2px solid #333; }
</style>
