# codeq 前端（扫描可视化看板）

Vue3 + Vite + TypeScript + Monaco Editor。复用后端 feature 02 REST API（FR-009），后端零改动。

## 开发 / Dev

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173（proxy /api → 后端 :8080）
```

> 先启动后端（项目根）：
> ```bash
> mvn -DskipTests package
> java -jar target/codeq-0.1.0-SNAPSHOT.jar   # 监听 :8080（dev=H2）
> ```

## 生产构建（前后端不分离，宪法 4.3）

```bash
cd frontend && npm run build                # → frontend/dist（vue-tsc 检查 + vite build）
cp -r dist/* ../src/main/resources/static/  # 拷到后端静态资源，由 Spring Boot 同源托管
java -jar target/codeq-0.1.0-SNAPSHOT.jar   # 浏览器访问 http://localhost:8080
```

## 技术栈

- Vue3（`<script setup>`）+ Vite + TypeScript
- `vue-router`（`/` 提交表单、`/scans/:id` 结果页）
- `axios`（HTTP / 轮询）
- `monaco-editor`（三色 Diff 可视化，宪法 4.3；直接 + Vite `?worker`）

## 脚本

- `npm run dev`：开发服务器（HMR，proxy /api）
- `npm run build`：类型检查 + 生产构建 → `dist/`
- `npm run preview`：预览构建产物

## 说明

- Monaco 当前为全量打包（结果页 chunk 较大），后续可优化为按需语言。
- 详细组件契约见 [../specs/003-scan-dashboard/contracts/components.md](../specs/003-scan-dashboard/contracts/components.md)。
