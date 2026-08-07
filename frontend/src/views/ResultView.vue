<script setup lang="ts">
// US2/US3 占位：Monaco 三色 Diff 与报告导出将在后续完善。
defineProps<{ id: string }>();
</script>

<template>
  <div>
    <p>扫描 <code>{{ id }}</code> 结果页</p>
    <p class="ok">（US2/US3 将实现 Monaco 三色 Diff 与报告导出）</p>
  </div>
</template>
