<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { createScan } from '../api/scans';
import ScanForm from '../components/ScanForm.vue';
import ScanProgress from '../components/ScanProgress.vue';
import type { ScanForm as ScanFormT } from '../types';

// US1 视图：表单 → 提交 → 进度 → SUCCESS 跳结果页。
const router = useRouter();
const taskId = ref<string | null>(null);
const errMsg = ref('');

async function onSubmit(form: ScanFormT) {
  errMsg.value = '';
  try {
    const r = await createScan(form);
    taskId.value = r.taskId;
  } catch (e) {
    errMsg.value = '提交失败: ' + String(e);
  }
}

function onDone() {
  if (taskId.value) {
    router.push(`/scans/${taskId.value}`);
  }
}

function onFailed(m: string) {
  errMsg.value = m;
}
</script>

<template>
  <ScanForm v-if="!taskId" @submit="onSubmit" />
  <ScanProgress v-else :task-id="taskId" @done="onDone" @failed="onFailed" />
  <div class="err" v-if="errMsg">{{ errMsg }}</div>
</template>
