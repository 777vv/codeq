# 快速验证：基础链路打通 / Quickstart

**特性 (Feature)**: 001-foundational-cli-pipeline | **阶段 (Phase)**: 1 | **日期 (Date)**: 2026-08-05

端到端验证本特性可用的最小流程。实现细节见 `tasks.md`。

## 前置 / Prerequisites

- JDK 21、Maven 3.9+
- git、Python 3 + `pip install diff-cover`
- 一个示例 Java 业务项目，其**测试环境**已以 Jacoco `output=tcpserver` 模式挂载 agent（地址如 `127.0.0.1:6300`），接入见 [../../../docs/integration-java.md](../../../docs/integration-java.md)
- 业务项目仓库内有 `baseline`、`release` 两分支，`release` 含若干增量变更，且已 `mvn compile` 产出 `target/classes`

## 构建 / Build

```bash
mvn -DskipTests package      # 产物 target/codeq-0.1.0-SNAPSHOT.jar
```

## 场景 1：US1 三色报告（本地 coverage.xml，P1）

```bash
java -jar target/codeq-0.1.0-SNAPSHOT.jar check \
  --repo /path/to/sample --baseline baseline --release release \
  --coverage-xml coverage.xml
```

**期望**：控制台输出三色报告，GREEN/RED/YELLOW 与人为构造的执行情况一致；退出码反映是否存在 RED/YELLOW。

## 场景 2：US2 在线 dump / reset（P2）

```bash
# 业务项目须 checkout 到 release 分支（US3 版本校验）
java -jar target/codeq-0.1.0-SNAPSHOT.jar reset --jacoco-host 127.0.0.1 --jacoco-port 6300
# 在测试环境跑一轮测试流量……
java -jar target/codeq-0.1.0-SNAPSHOT.jar check \
  --repo /path/to/sample --baseline baseline --release release \
  --jacoco-host 127.0.0.1 --jacoco-port 6300
```

**期望**：在线 dump → 转 coverage.xml → 三色判定；reset 后覆盖清零。

## 场景 3：US3 确定性 / 版本校验 / 零侵入（P3）

- **确定性**：连续两次 `check` 输出完全一致。
- **版本校验**：业务项目 checkout 到非 release 分支后 `check` → 退出码 `2`（HEAD ≠ release）。
- **零侵入**：接入仅需测试环境 `-javaagent`，无应用代码改动。

## 验证矩阵（对照 spec 验收场景）

| spec 场景 | 此处命令 | 期望 |
|---|---|---|
| US1-AC1 | 场景 1（构造 5 处变更） | 3 绿 / 1 红 / 1 黄 |
| US1-AC2 | `release == baseline` | 零变更 |
| US1-AC3 | 错误的执行数据 | 退出码 `2` |
| US3-AC1 | 连续两次 `check` | 一致 |
| US3-AC2 | 接入检查 | 无应用代码改动 |

> 完整实现与单元 / 集成测试在 `tasks.md` / 实现阶段产出；本文件仅验证指南。
