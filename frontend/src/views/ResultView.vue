<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { getResult } from '../api/scans';
import type { ResultView as ResultViewT } from '../types';
import DiffViewer from '../components/DiffViewer.vue';
import ExportButton from '../components/ExportButton.vue';

// US2/US3 结果页：判定摘要 + 三色 Diff + 导出。
const props = defineProps<{ id: string }>();
const result = ref<ResultViewT | null>(null);
const errMsg = ref('');

onMounted(async () => {
  try {
    result.value = await getResult(props.id);
  } catch (e) {
    errMsg.value = '获取结果失败: ' + String(e) + '（任务是否已完成 SUCCESS？）';
  }
});
</script>

<template>
  <div>
    <p v-if="errMsg" class="err">{{ errMsg }}</p>
    <div v-if="result">
      <h2>判定：<span :class="result.pass ? 'ok' : 'err'">{{ result.pass ? '可上线' : '禁止上线（存在 RED）' }}</span></h2>
      <p>🟢 {{ result.totals.green }} ｜ 🔴 {{ result.totals.red }} ｜ 🟡 {{ result.totals.yellow }} ｜ ◔ {{ result.totals.partial }}</p>
      <p><ExportButton :result="result" /></p>
      <h3>变更明细（三色 Diff）</h3>
      <DiffViewer :changes="result.changes" />
    </div>
  </div>
</template>
