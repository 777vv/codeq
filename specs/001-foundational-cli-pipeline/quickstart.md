# 快速验证：基础链路打通 / Quickstart

**特性 (Feature)**: 001-foundational-cli-pipeline | **阶段 (Phase)**: 1 | **日期 (Date)**: 2026-08-05

端到端验证本特性可用的最小流程。实现细节见（后续）`tasks.md`。

## 前置 / Prerequisites

- JDK 21
- Maven 3.9+
- git
- Python 3 + `pip install diff-cover`
- 一个示例 Java 业务项目，其**测试环境**已以 Jacoco `output=tcpserver` 模式挂载 agent（地址如 `127.0.0.1:6300`）
- 仓库内有 `baseline`、`release` 两分支，`release` 含若干增量变更

## 构建 / Build

```bash
mvn -q -DskipTests package      # 产物 target/codeq.jar（占位）
```

## 场景 1：US1 三色报告（P1）

```bash
java -jar target/codeq.jar check \
  --repo /path/to/sample --baseline baseline --release release \
  --jacoco-host 127.0.0.1 --jacoco-port 6300
```

**期望**：控制台输出三色报告，GREEN / RED / YELLOW 与人为构造的执行情况一致；退出码反映是否存在 RED / YELLOW。

## 场景 2：US2 dump / reset / 累加（P2）

```bash
java -jar target/codeq.jar reset --jacoco-host 127.0.0.1 --jacoco-port 6300
# 在测试环境跑一轮测试流量……
java -jar target/codeq.jar check --repo ... --baseline ... --release ... \
  --jacoco-host 127.0.0.1 --jacoco-port 6300 --accumulate
```

**期望**：累加后覆盖反映多轮；reset 后覆盖清零。

## 场景 3：US3 确定性 / 隔离 / 零侵入（P3）

- **确定性**：连续两次 `check` 输出完全一致。
- **拒绝**：传入版本不匹配 / 非测试数据 → 退出码 `2`。
- **零侵入**：示例项目接入仅需测试环境 Jacoco agent，无应用代码改动。

## 验证矩阵（对照 spec 验收场景）

| spec 场景 | 此处命令 | 期望 |
|---|---|---|
| US1-AC1 | 场景 1（构造 5 处变更） | 3 绿 / 1 红 / 1 黄 |
| US1-AC2 | `release == baseline` | 零变更 |
| US1-AC3 | 错误的执行数据 | 退出码 `2` |
| US3-AC1 | 连续两次 `check` | 一致 |
| US3-AC2 | 接入检查 | 无应用代码改动 |

> 完整实现与单元 / 集成测试在 `tasks.md` / 实现阶段产出；本文件仅验证指南。
