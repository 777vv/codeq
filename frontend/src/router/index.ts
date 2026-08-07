import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';

// codeq 前端路由：/ 提交表单；/scans/:id 结果页。
const routes: RouteRecordRaw[] = [
  { path: '/', component: () => import('../views/ScanFormView.vue') },
  { path: '/scans/:id', component: () => import('../views/ResultView.vue'), props: true }
];

export default createRouter({ history: createWebHistory(), routes });
