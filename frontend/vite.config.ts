import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// codeq 前端 Vite 配置：dev proxy /api → 后端 8080；prod build → dist（由 Spring Boot 托管）。
// 注：Monaco（US2）用 monaco-editor 直接 + Vite ?worker 方案，不依赖 vite-plugin-monaco-editor。
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true }
    }
  },
  build: { outDir: 'dist' }
});

