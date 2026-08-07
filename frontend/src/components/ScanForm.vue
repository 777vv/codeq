<script setup lang="ts">
import { computed, reactive } from 'vue';
import type { ScanForm as ScanFormT } from '../types';

// US1 扫描参数表单 + 校验。
const emit = defineEmits<{ (e: 'submit', form: ScanFormT): void }>();

const form = reactive<ScanFormT>({
  repo: '', baseline: '', release: '',
  jacocoHost: '', jacocoPort: 6300, coverageXmlPath: ''
});

const error = computed(() => {
  if (!form.repo || !form.baseline || !form.release) return 'repo / baseline / release 必填';
  const hasJacoco = form.jacocoHost && form.jacocoPort;
  const hasXml = form.coverageXmlPath;
  if (!hasJacoco && !hasXml) return '需提供 Jacoco 端点 或 coverageXmlPath（二选一）';
  return '';
});

function onSubmit() {
  if (!error.value) emit('submit', { ...form });
}
</script>

<template>
  <form @submit.prevent="onSubmit">
    <label>代码仓库路径 (repo)</label>
    <input v-model="form.repo" placeholder="/path/to/sample" />

    <label>基准分支 (baseline)</label>
    <input v-model="form.baseline" placeholder="baseline" />

    <label>待发布分支 (release)</label>
    <input v-model="form.release" placeholder="release" />

    <label>测试环境 Jacoco host（与 coverageXmlPath 二选一）</label>
    <input v-model="form.jacocoHost" placeholder="127.0.0.1" />

    <label>Jacoco 端口</label>
    <input v-model.number="form.jacocoPort" type="number" placeholder="6300" />

    <label>本地 coverageXmlPath（二选一）</label>
    <input v-model="form.coverageXmlPath" placeholder="/path/to/coverage.xml" />

    <div class="err" v-if="error">{{ error }}</div>
    <button type="submit" :disabled="!!error" style="margin-top: 12px">开始扫描</button>
  </form>
</template>
