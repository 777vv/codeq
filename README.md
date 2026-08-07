# codeq · 发布前增量代码覆盖安检平台

> 彻底解决「开发私改代码、新增接口、增补逻辑未同步测试，导致代码无测试覆盖、线上突发故障」的研发质量顽疾——为发布前搭建**强制质量安检门禁**，全程适配**无单元测试**的传统业务项目。

codeq 不管理测试用例、不执行测试、不做断言，只回答一个问题：

> **本次发布分支的增量变更代码，是否被测试环境的真实流量真正执行过？**

---

## 核心理念（与同类工具的根本区别）

- **只认真实流量，不看单测覆盖率**：完全不依赖、不读取、不参考单元测试覆盖率，仅以测试环境真实人工/自动化操作流量作为判定依据。
- **只校验增量**：只判定本次待发布分支相对基准分支的**增量变更**是否被执行，不纠缠历史代码的技术债。
- **零侵入接入**：业务项目不改造代码、不引入强依赖，仅在测试环境挂载采集器即可接入。
- **发布门禁**：判定结果作为公司版本发布的官方唯一代码覆盖合规依据。

完整最高准则见 [`.specify/memory/constitution.md`](.specify/memory/constitution.md)（codeq 技术宪法 v1.2.0）。

---

## 工作流程

```text
代码分支增量比对 → 测试环境真实流量采集 → 增量代码覆盖匹配校验 → 漏测风险分级展示 → 发布门禁风险拦截
```

## 三色判定标准（统一对外口径）

| 颜色 | 含义 | 处置 |
|------|------|------|
| 🟢 绿色 | 增量代码被测试环境真实流量**完整执行**，无漏执行 | 合规，可上线 |
| 🔴 红色 | 增量代码**从未被执行**，无任何流量命中 | 高危漏测，默认禁止上线 |
| 🟡 黄色 | 因重构/行号漂移/签名微调等**无法精准匹配** | 需研发/测试人工复核 |

---

## 技术栈

| 层 | 选型 |
|----|------|
| 后端 | Java 21 + Spring Boot 3.x（核心逻辑服务端闭环、任务全异步化） |
| 前端 | Vue3 + Monaco Editor（扫描表单、进度、三色 Diff 可视化、报告导出） |
| 代码比对 | `git merge-base` 取双分支共同基线 + 复用开源 `diff-cover` 引擎 |
| 精准匹配 | AST 语法树解析，以「类名 + 方法签名 + 路由标识」为主键（行号仅用于可视化） |
| 流量采集 | Java 项目于测试环境挂载 Jacoco Agent（TCP 服务模式，支持远程 dump / 多轮累加 / 一键重置） |
| 代码规范 | Google Java Style + 《阿里巴巴 Java 开发手册》+ 安全扫描（宪法 VII） |
| 日志 | SLF4J（dev 彩色 console / prod JSON + traceId，宪法 VIII） |

> 生产 / 预发环境**严禁**挂载任何探针；仅测试环境采集的流量有效。

---

## 使用方法 / Usage

### 前置 / Prerequisites
- JDK 21、Maven 3.9+、git
- Python 3 + `pip install diff-cover`（核心比对引擎，宪法 4.1 强制复用，禁止自研）
- 前端联调另需 Node.js 20+

### 构建 / Build
```bash
mvn -DskipTests package      # 产物 target/codeq-0.1.0-SNAPSHOT.jar
```

> Windows cmd 中文/emoji 显示乱码时，先 `chcp 65001` 切 UTF-8（或在 PowerShell / IDE 终端跑）。

codeq 有 **四种使用形态**，按调试难度递增：

### 形态 A：CLI + 本地 coverage.xml（最快调试，无需 Jacoco）

用一份现成的 `coverage.xml`（cobertura/jacoco 格式）作为执行数据，直接出三色判定。适合调试核心判定逻辑。

```bash
java -jar target/codeq-0.1.0-SNAPSHOT.jar check \
  --repo <业务项目仓库> --baseline <线上分支> --release <待发布分支> \
  --coverage-xml <path/to/coverage.xml>
```

**调试样例**：见下方「最小调试样例」，开箱跑出 🔴 漏测判定。

### 形态 B：CLI + 测试环境 Jacoco（完整链路）

业务项目测试环境挂 Jacoco agent（`output=tcpserver`，接入见 [docs/integration-java.md](docs/integration-java.md)），跑测试流量后远程 dump 判定：

```bash
java -jar target/codeq-0.1.0-SNAPSHOT.jar check \
  --repo <业务项目> --baseline <线上分支> --release <待发布分支> \
  --jacoco-host <test-host> --jacoco-port 6300

# 辅助子命令
java -jar target/codeq-0.1.0-SNAPSHOT.jar dump  --jacoco-host <h> --jacoco-port <p>  # 探测连通
java -jar target/codeq-0.1.0-SNAPSHOT.jar reset --jacoco-host <h> --jacoco-port <p>  # 重置 agent 计数
```

> 生产 / 预发**严禁**挂载 agent（宪法红线）；仅测试环境流量有效。

**退出码**：`0` 全绿（合规）｜ `1` 有红/黄/partial（需人工或拦截）｜ `2` 输入/版本错误。

### 形态 C：REST 服务（feature 02）

无 CLI 子命令启动 → web 服务常驻（默认 8080）：

```bash
# dev（H2 内存 + 彩色 console 日志）
java -jar target/codeq-0.1.0-SNAPSHOT.jar

# prod（PostgreSQL + JSON 日志，宪法 VIII）
java -Dspring.profiles.active=prod -DDB_URL=jdbc:postgresql://... \
     -DDB_USERNAME=codeq -DDB_PASSWORD=*** -jar target/codeq-0.1.0-SNAPSHOT.jar
```

```bash
curl -X POST localhost:8080/api/scans -H "Content-Type: application/json" -d '{
  "repo":"<业务项目>","baseline":"<线上分支>","release":"<待发布分支>",
  "coverageXmlPath":"<coverage.xml>"}'      # → {"taskId":"...","status":"PENDING"}
curl localhost:8080/api/scans/<taskId>           # 轮询状态
curl localhost:8080/api/scans/<taskId>/result    # 三色明细
curl localhost:8080/api/scans/<taskId>/verdict   # {pass, totals}
curl 'localhost:8080/api/scans?repo=&version='   # 历史回溯
```

接口契约详见 [specs/002-scan-service/contracts/api.md](specs/002-scan-service/contracts/api.md)。

### 形态 D：前端可视化（feature 03）

Vue3 + Monaco Web 界面，复用形态 C 的 REST API：

```bash
# 终端1：后端（形态 C）
# 终端2：前端（dev，:5173 proxy /api → :8080）
cd frontend && npm install && npm run dev      # 浏览器 http://localhost:5173

# 生产构建（前后端不分离，宪法 4.3）：构建产物拷到后端静态资源
cd frontend && npm run build && cp -r dist/* ../src/main/resources/static/
java -jar target/codeq-0.1.0-SNAPSHOT.jar      # 浏览器 http://localhost:8080
```

浏览器：填表（repo/baseline/release/coverageXmlPath 或 Jacoco 端点）→ 开始扫描 → 进度 → Monaco 三色 Diff + 导出 HTML 报告。详见 [specs/003-scan-dashboard/quickstart.md](specs/003-scan-dashboard/quickstart.md)。

### 最小调试样例（形态 A 开箱）

无需业务项目与 Jacoco，用一个最小 Java demo 验证三色判定。在 codeq 仓库**外**准备：

```bash
DEMO=../codeq-demo
mkdir -p "$DEMO/src/main/java/demo" && cd "$DEMO"
git init -b main

# baseline：只有 add 方法
printf 'package demo;\n\npublic class Demo {\n    public int add(int a, int b) {\n        return a + b;\n    }\n}\n' > src/main/java/demo/Demo.java
git add -A && git commit -m baseline && git branch baseline

# release：新增 sub 方法
printf 'package demo;\n\npublic class Demo {\n    public int add(int a, int b) {\n        return a + b;\n    }\n    public int sub(int a, int b) {\n        return a - b;\n    }\n}\n' > src/main/java/demo/Demo.java
git checkout -b release && git add -A && git commit -m "release: add sub"

# coverage.xml：sub 的 return（行8）hits=0 → 漏测
printf '<?xml version="1.0"?>\n<coverage>\n  <packages><package name="demo"><classes><class name="demo.Demo" filename="src/main/java/demo/Demo.java"><lines><line number="5" hits="1"/><line number="8" hits="0"/></lines></class></classes></package></packages>\n</coverage>\n' > coverage.xml

# 跑 codeq（在 codeq 仓库目录）
java -jar ../codeq/target/codeq-0.1.0-SNAPSHOT.jar check \
  --repo "$DEMO" --baseline baseline --release release --coverage-xml coverage.xml
# 期望：🔴 demo.Demo#sub(int,int) 高危漏测，退出码 1
```

把 `coverage.xml` 里 `<line number="8" hits="0"/>` 改成 `hits="1"` → sub 变 🟢 绿色，退出码 `0`。

---

## 仓库结构

```text
codeq/
├── src/main/java/com/codeq/        # 后端 Java（feature 01/02 核心 + REST + JPA + 异步）
├── src/main/resources/             # application.yml、logback-spring.xml
├── frontend/                       # 前端 Vue3 + Vite + Monaco（feature 03）
├── docs/integration-java.md        # 业务项目 Jacoco 零侵入接入
├── specs/                          # 功能规格（每个 feature 一个目录）
│   ├── 001-foundational-cli-pipeline/   # 迭代 #1：CLI 基础链路
│   ├── 002-scan-service/                # 迭代 #2：扫描服务化（REST + JPA）
│   └── 003-scan-dashboard/              # 迭代 #3：前端可视化
├── .specify/                       # Spec Kit 配置 + 宪法（v1.2.0）+ 模板
├── pom.xml
└── README.md
```

---

## 研发工作流（Spec Kit）

本项目使用 [Spec Kit](https://github.com/github/spec-kit) 驱动的规格化开发流程，宪法作为每一阶段的硬性门禁：

```text
/speckit-constitution  →  /speckit-specify  →  /speckit-clarify  →  /speckit-plan
                          （写规格）           （澄清需求）          （实施计划 + 宪法校验）
→  /speckit-tasks  →  /speckit-implement  →  /speckit-checklist  →  /speckit-analyze / /speckit-converge
   （任务拆解）        （实现）                （验收清单）            （分析 / 收敛合规）
```

每个 feature 的产物位于 `specs/<NNN>-<name>/`（spec、plan、research、data-model、contracts、tasks 等）。

---

## 迭代路线（宪法第六篇章，固定顺序）

1. ✅ **基础链路打通**（feature 001）：探针采集 + 分支 Diff + 增量覆盖比对，命令行可用链路
2. ✅ **核心接口服务化**（feature 002）：后端 REST + 异步任务 + JPA 持久化 + 门禁查询
3. ✅ **前端可视化落地**（feature 003）：分支选择、扫描进度、三色 Diff 展示、报告导出
4. ⏭ **AST 精准匹配优化**：解决行号漂移、重构误判
5. ⏭ **发布门禁闭环**：打通发布平台，风险拦截 + 人工豁免记录
6. ⏭ **性能与数据治理优化**：TTL 清理、性能强化

---

## 红线（绝对禁止）

- 禁止接入/参考/使用任何单元测试覆盖率数据作为漏测判定依据
- 禁止仅依靠代码行号完成增量覆盖匹配（必须叠加 AST 方法签名匹配）
- 禁止要求业务项目改造代码或侵入业务逻辑完成接入
- 禁止在生产 / 预发环境挂载探针、采集执行数据
- 禁止使用非当前待发布版本的测试流量做上线合规判定
- 禁止拓展职责边界（测试用例管理 / 测试执行 / 断言校验）
- 禁止私自修改三色判定标准、放宽漏测上线门禁
- 禁止 Java 类缺少类头 `@author`/`@date`、禁止用 `System.out` 打日志（宪法 VII/VIII/IX）

---

## 当前进度

- ✅ 宪法 v1.2.0（9 条 Core Principle：真实流量/增量/AST/零侵入/三色/边界/代码门禁/日志/类头）
- ✅ Feature 001（CLI 基础链路）、002（扫描服务化）、003（前端可视化）—— 实现 + 构建通过
- ⏭ 下一步：AST 精准匹配优化（迭代 #4），或本机实跑联调（CLI/服务/前端）

## License

待定（TBD）。
