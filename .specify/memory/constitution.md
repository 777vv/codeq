<!--
Sync Impact Report
==================
Constitution: codeq
Version change: (none — initial fill from unpopulated template) → 1.0.0
Type: Initial ratification

Modified principles: N/A (first population). The user-authored 8-chapter
technical spec (总则 + 第一~第八篇章) was mapped into the template skeleton:
  - Iron laws (Ch2) + red lines (Ch7) → Core Principles I–VI
  - Technical architecture (Ch4) → Section "Technical Architecture & Stack Constraints"
  - Capability boundaries (Ch5) + iteration priority (Ch6) → Section "Capability Boundaries & Iteration Priority"
  - Red lines (Ch7) + final interpretation (Ch8) + preamble → Governance
Verbatim terminology preserved (diff-cover, git merge-base, AST, Jacoco TCP,
Monaco Editor, Vue3, Java 21, Spring Boot 3.x, 三色判定, 增量代码, etc.).

Added sections:
  - Core Principles (I–VI)
  - Technical Architecture & Stack Constraints / 技术架构强制规范
  - Capability Boundaries & Iteration Priority / 能力边界与迭代优先级
  - Governance (Absolute Red Lines + Amendment + Versioning & Compliance)

Removed sections: N/A

Templates / artifacts requiring updates:
  - .specify/templates/plan-template.md   ✅ no change (Constitution Check is data-driven: "[Gates determined based on constitution file]")
  - .specify/templates/spec-template.md   ✅ no change (generic, no principle-specific references)
  - .specify/templates/tasks-template.md  ✅ no change (generic; test-first note already aligns)
  - .claude/skills/speckit-*/SKILL.md     ✅ no change (all load .specify/memory/constitution.md at runtime; speckit-analyze extracts principle names + MUST/SHOULD normative statements dynamically — no hardcoded principle names)
  - README.md / docs/quickstart.md        ✅ n/a (none present)

Follow-up TODOs: none. Ratification date set to initial adoption (2026-08-05).
Source of truth: user-authored codeq technical constitution (8 chapters + preamble), provided via /speckit-constitution.
-->

# codeq Constitution / codeq 技术宪法

> 本文档为 codeq 唯一权威技术宪法，是系统设计、功能开发、架构迭代、业务接入、流程落地、边界定义的最高准则。
> 所有研发迭代、功能改造、逻辑调整、环境适配、门禁规则必须严格遵循本文档；禁止私自变更平台定位、篡改核心判定逻辑、删减底层规则、私自拓展非职责范围内的能力。

**平台核心使命**：彻底解决"开发私改代码、新增接口、增补逻辑未同步测试，导致代码无测试覆盖、线上突发故障"的研发质量顽疾；搭建发布前强制质量安检门禁，全程适配无单元测试的传统业务项目。

**核心底层纲领**：完全不依赖、不读取、不参考单元测试覆盖率，仅以测试环境真实人工/自动化操作流量作为判定依据。

## Core Principles / 核心纲领

### I. Real-Traffic Coverage Only (NON-NEGOTIABLE) / 唯真实流量覆盖

代码覆盖判定的唯一合法数据源是**测试环境的真实执行流量**——人工操作（页面 / Postman）与标准化集成测试自动化流量。

- 平台彻底剥离单元测试体系：不接入、不统计、不参考任何单元测试代码或单元测试覆盖率数据。
- 覆盖比对数据源必须（MUST）为当前待发布分支、待上线构建包对应的测试环境实例。
- 禁止（MUST NOT）使用历史版本、线上版本、旧测试环境、本地调试环境的流量作为新版本覆盖判定依据。

**Rationale**：适配大量无单元测试的传统业务项目，避免"覆盖率 ≠ 真实测试"的假象，回归"变更代码是否被真实执行"的本质。

### II. Incremental-Only Judgment / 仅校验增量

所有校验逻辑仅针对**本次分支的增量变更代码**（待发布分支相对基准分支的 Diff）。

- 基准分支定义：线上稳定运行的正式版本分支。
- 待发布分支定义：开发迭代完成、即将提测上线的开发分支。
- 禁止（MUST NOT）校验存量历史代码的覆盖情况。

**Rationale**：聚焦本次发布风险，避免历史技术债阻塞正常迭代发布。

### III. Precise AST Matching (Never Line-Number-Only) / AST 精准匹配

覆盖匹配必须（MUST）基于 AST 语法树解析，以「类名 + 方法签名 + 路由标识」为核心匹配主键。

- 基线获取必须（MUST）通过 `git merge-base` 取双分支共同基线，杜绝无效差异、伪变更。
- 禁止（MUST NOT）仅依靠代码行号做覆盖匹配，规避代码移位、重构导致的误判；行号仅用于前端可视化。
- 增量覆盖比对底层引擎固定复用开源工具 `diff-cover`，禁止（MUST NOT）自研核心比对算法，仅做业务适配封装。

**Rationale**：行号漂移是覆盖类工具的核心误判来源，AST 主键保证判定稳健、可复现。

### IV. Zero-Intrusion Integration & Environment Isolation / 零侵入接入

业务项目接入平台禁止改造业务代码、禁止引入业务强依赖，全程无侵入。

- Java 项目：仅（MUST）在测试环境挂载 Jacoco Agent，采用 TCP 服务模式，支持远程动态 dump、多次流量累加、一键重置本次覆盖数据；**生产环境严禁挂载任何探针**（MUST NOT）。
- 仅测试环境采集的流量有效；本地调试、预发、生产环境操作数据不纳入判定范围。
- 数据必须（MUST）按 项目 / 版本 / Commit / 测试任务 / 服务实例 多维度隔离，杜绝跨版本、跨任务污染。

**Rationale**：接入成本与侵入性决定平台能否在大量存量业务项目落地；环境隔离是判定可信的前提。

### V. Fixed Three-Color Verdict / 三色判定标准（统一对外口径）

风险判定采用固定三色标准，禁止（MUST NOT）私自修改或放宽：

- 🟢 **绿色（合规已覆盖）**：本次新增/修改的增量代码、接口逻辑、分支逻辑被测试环境真实流量完整执行，无漏执行，符合上线覆盖标准。
- 🔴 **红色（高危漏测）**：本次新增/修改的增量代码无任何人工或自动化测试流量命中、从未被执行，属高危线上风险，**默认禁止上线**。
- 🟡 **黄色（人工复核）**：因代码重构、行号漂移、方法签名微调、代码移位导致系统无法精准匹配的变更代码，不自动判定，需研发/测试人工复核确认。

**Rationale**：统一对外口径，保证全公司发布门禁判定标准一致、可解释、可追溯。

### VI. Scope Integrity (Capability Boundary) / 职责边界不越界

平台专注**代码变更覆盖校验与发布风险安检**，仅判定"增量变更代码是否被真实执行"。

- 不管理测试用例、不执行测试任务、不做测试结果断言判定（MUST NOT）。
- 仅校验代码是否被执行，不校验分支全覆盖、边界场景、异常场景、参数合法性是否测试完整。
- 不替代（MUST NOT）人工测试设计、代码评审、测试断言校验；仅作为风险安检工具。

**Rationale**：明确权责、不越界、不背锅，避免范围蔓延稀释核心价值。

## Technical Architecture & Stack Constraints / 技术架构强制规范

### 工作架构（固定不变）

代码分支增量比对 → 测试环境真实流量采集 → 增量代码覆盖匹配校验 → 漏测风险分级展示 → 发布门禁风险拦截。

平台质量判定结果为公司版本发布的官方唯一代码覆盖合规依据，其他测试平台结果仅作过程参考，不替代本平台门禁判定。

### 唯一操作入口

平台唯一启动方式：用户选择代码仓库、基准分支、待发布分支，绑定对应测试环境服务实例，一键触发全流程检测；无其他自定义检测入口。

### 前后端架构

- 前端：Vue3；后端：Java 21 + Spring Boot 3.x；前后端不分离项目。
- 代码规范满足《阿里巴巴 Java 开发手册》规约。
- **核心逻辑服务端闭环**：代码比对、数据采集、覆盖匹配、风险判定全部在服务端实现；前端仅负责参数录入、进度展示、结果可视化、报告导出。
- **任务全异步化**：扫描、采集、比对任务必须（MUST）采用异步队列执行，杜绝前端超时与任务阻塞。
- 可视化组件：代码 Diff 高亮页面统一使用 Monaco Editor，对齐主流编辑器交互体验。
- 数据生命周期：执行日志、扫描报告、覆盖数据必须（MUST）设置 TTL 过期清理机制，禁止无限堆积。

## Capability Boundaries & Iteration Priority / 能力边界与迭代优先级

### 平台固定可实现能力

- 精准识别开发私自新增、修改、未告知测试的代码逻辑、接口、分支条件。
- 精准判定增量变更代码是否被人工/自动化测试真实执行，适配所有无单元测试的业务项目。
- 输出可视化漏测风险清单，精准定位高危漏测代码位置。
- 提供标准化发布门禁 API，对接公司发布平台，实现漏测代码上线拦截。
- 支持历史扫描任务、覆盖记录回溯，沉淀质量追溯数据。

### 平台固定不可实现能力（不迭代、不背锅）

- 无法判定测试深度（仅校验"是否执行"，不校验覆盖完整性）。
- 无法采集非规范环境流量（本地调试、旧测试环境、生产环境）。
- 无法替代人工测试设计、代码评审、测试断言校验。

### 迭代优先级（固定开发顺序，禁止颠倒）

基础链路打通 → 核心接口服务化 → 前端可视化落地 → AST 精准匹配优化 → 发布门禁闭环落地 → 性能与数据治理优化。

1. **基础链路**：探针采集、分支 Diff、增量覆盖比对的命令行可用链路。
2. **接口服务化**：核心能力封装为后端通用接口、异步任务调度、数据持久化。
3. **可视化**：分支选择、扫描进度、三色 Diff 展示、报告导出。
4. **精准优化**：接入 AST 解析，解决行号漂移、代码重构误判。
5. **门禁闭环**：打通发布平台，实现风险拦截、人工豁免记录、流程闭环。

## Governance / 治理与红线

### 绝对红线禁令（违规必改）

任何场景不得突破：

- 禁止接入、参考、使用任何单元测试覆盖率数据作为漏测判定依据。
- 禁止仅依靠代码行号完成增量覆盖匹配，必须叠加 AST 方法签名匹配。
- 禁止要求业务项目改造代码、引入业务依赖、侵入业务逻辑完成接入。
- 禁止在生产、预发环境挂载代码探针、采集执行数据。
- 禁止使用非当前待发布版本的测试流量做上线合规判定。
- 禁止拓展平台职责边界（测试用例管理、测试执行、断言校验等）。
- 禁止私自修改三色风险判定标准、放宽漏测上线门禁规则。

### 修正程序 / Amendment

- 本文档为唯一官方、权威、通用的 SPEC 技术规范，涵盖架构、逻辑、规则、边界、迭代、红线全部标准；宪法具有最高优先级，凌驾于所有其他实践与文档之上。
- 对**核心纲领（Core Principles I–VI）或红线禁令**的任何变更视为 MAJOR，必须经过评审、记录、并提供迁移方案后方可生效；技术架构与流程细节的实质性扩展为 MINOR；措辞与澄清为 PATCH。
- 所有 plan / spec / tasks 必须通过宪法符合性检查（Constitution Check）；任何偏离本宪法的设计与代码视为不合格迭代，必须整改回滚。

### 版本与合规审查 / Versioning & Compliance

- 版本遵循语义化版本（SemVer）。
- 每次 PR / 评审必须核验宪法合规性；复杂度必须被证明（参考 plan 的 Complexity Tracking）。
- 运行时开发指引参见各 feature 的 `specs/[###-feature]/` 文档。

**Version**: 1.0.0 | **Ratified**: 2026-08-05 | **Last Amended**: 2026-08-05
