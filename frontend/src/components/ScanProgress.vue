<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue';
import { getScan } from '../api/scans';
import type { ScanStatus } from '../types';

// US1 状态轮询（每 2s）至终态。
const props = defineProps<{ taskId: string }>();
const emit = defineEmits<{ (e: 'done'): void; (e: 'failed', msg: string): void }>();

const status = ref<ScanStatus | null>(null);
let timer: ReturnType<typeof setInterval> | undefined;

async function poll() {
  try {
    status.value = await getScan(props.taskId);
    if (status.value.status === 'SUCCESS') {
      stop();
      emit('done');
    } else if (status.value.status === 'FAILED') {
      stop();
      emit('failed', status.value.errorMsg || '扫描失败');
    }
  } catch (e) {
    stop();
    emit('failed', '轮询失败: ' + String(e));
  }
}

function stop() {
  if (timer) {
    clearInterval(timer);
    timer = undefined;
  }
}

onMounted(() => {
  poll();
  timer = setInterval(poll, 2000);
});
onUnmounted(stop);
</script>

<template>
  <div class="progress">
    <div>任务 <code>{{ taskId }}</code> 状态：<b>{{ status?.status || '查询中…' }}</b></div>
    <div v-if="status?.status === 'FAILED'" class="err">{{ status.errorMsg }}</div>
    <div v-else class="ok">扫描进行中，请稍候…</div>
  </div>
</template>
