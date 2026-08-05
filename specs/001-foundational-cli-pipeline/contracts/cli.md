# CLI 契约：codeq 命令行 / CLI Contract

**特性 (Feature)**: 001-foundational-cli-pipeline | **阶段 (Phase)**: 1 | **日期 (Date)**: 2026-08-05

本文件定义 codeq CLI（本迭代唯一交互入口，宪法第三篇 3.1）对外暴露的命令、参数、退出码与输出。实现细节见（后续）`tasks.md`。

## 调用形态

Picocli 风格，子命令：

- `codeq check` —— 一键全流程检测（核心，spec US1）
- `codeq dump` —— 从测试环境拉取执行数据（spec US2）
- `codeq reset` —— 重置测试环境执行数据（spec US2）

## codeq check

```bash
codeq check \
  --repo <本地仓库路径> \
  --baseline <基准分支> \
  --release <待发布分支> \
  --jacoco-host <测试环境 host> \
  --jacoco-port <Jacoco TCP 端口> \
  [--accumulate]                  # 多轮累加（默认每次 dump 后累加）
  [--report console|html|json]    # 默认 console
  [--out <报告输出路径>]           # html/json 时
```

**行为**: `git merge-base` diff →（远程 dump 执行数据）→ AST 匹配 → 三色判定 → 报告。

**退出码**:

- `0` = 全部 GREEN（合规）
- `1` = 存在 RED 或 YELLOW 或 PARTIAL（需人工 / 拦截）
- `2` = 输入错误 / 环境拒绝（如执行数据来源不匹配）

## codeq dump

```bash
codeq dump --repo <...> --jacoco-host <...> --jacoco-port <...> [--out <.exec 落盘路径>]
```

拉取当前测试环境执行数据（spec FR-005）。

## codeq reset

```bash
codeq reset --jacoco-host <...> --jacoco-port <...>
```

重置 Jacoco agent 计数（spec FR-006）。

## 输出格式

**console 摘要**:

```text
codeq 覆盖判定报告
仓库: <repo>   基准: <baseline> → 待发布: <release>
总计 5 处增量变更：🟢 绿色 3 | 🔴 红色 1 | 🟡 黄色 1 | ◔ partial 0
- 🔴 com/foo/Bar.java   Bar.update()      (从未执行)
- 🟡 com/foo/Baz.java   Baz.process()     (无法匹配：签名漂移)
...
```

**json 结构**:

```json
{
  "repo": "...", "baseline": "...", "release": "...",
  "totals": {"green": 3, "red": 1, "yellow": 1, "partial": 0},
  "changes": [{"file": "...", "methodKey": {...}, "verdict": "RED", "uncoveredLines": [...]}]
}
```

## 不变量（契约约束）

- 拒绝非测试环境 / 版本不匹配的执行数据（FR-007）→ 退出码 `2`
- 不读取单元测试覆盖率（FR-008）
- 相同输入 → 相同输出（FR-010）
- 仅以「类名 + 方法签名 + 路由」为匹配主键，行号仅用于展示（宪法 4.1）
