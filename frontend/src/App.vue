<script setup lang="ts">
// codeq 根组件。
</script>

<template>
  <div class="app">
    <header><h1>codeq 扫描可视化看板</h1></header>
    <main><router-view /></main>
  </div>
</template>

<style>
body { margin: 0; font-family: system-ui, sans-serif; background: #f6f7f9; }
.app { max-width: 960px; margin: 0 auto; padding: 16px; }
h1 { font-size: 20px; }
button { cursor: pointer; padding: 6px 12px; }
input { padding: 6px; width: 100%; box-sizing: border-box; }
label { display: block; margin: 8px 0 2px; font-size: 13px; color: #555; }
.err { color: #d33; margin: 8px 0; }
.ok { color: #2a8; }
</style>
