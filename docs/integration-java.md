# 业务项目接入说明（Java） / Integration Guide

codeq 通过**测试环境**的 Jacoco agent 采集执行数据。接入是**零侵入**的：业务项目无需改动任何应用源代码或生产依赖，仅在测试环境 JVM 启动参数挂载 Jacoco agent。

> 宪法第四篇 4.2：Java 项目仅测试环境挂载 Jacoco Agent（TCP 服务模式）；生产 / 预发环境**严禁**挂载任何探针。

## 前置 / Prerequisites

- 业务项目为 Java（JVM）
- 下载 `jacocoagent.jar`（版本建议与 codeq 使用的 `org.jacoco` 0.8.12 对齐）

## 测试环境挂载 agent（TCP 服务模式）

在测试环境启动业务应用时，加入 JVM 参数：

```bash
-javaagent:/path/to/jacocoagent.jar=output=tcpserver,address=0.0.0.0,port=6300,includes=*
```

- `output=tcpserver`：agent 监听 TCP，由 codeq 远程 dump（宪法 4.2）
- `address` / `port`：codeq 连接的 host / port（对应 `--jacoco-host` / `--jacoco-port`）
- 仅测试环境挂载；**生产 / 预发严禁挂载**（红线）

## codeq 侧使用

```bash
# 探测测试环境连通性
codeq dump  --jacoco-host <test-host> --jacoco-port 6300

# 新一轮测试前重置（清零 agent 计数）
codeq reset --jacoco-host <test-host> --jacoco-port 6300

# 跑测试流量后，一键判定（自动 dump → 转 coverage.xml → 三色判定）
codeq check --repo <业务项目仓库> --baseline <线上分支> --release <待发布分支> \
            --jacoco-host <test-host> --jacoco-port 6300
```

## 零侵入保证 / Zero-Intrusion

- **不修改**业务源代码
- **不引入**业务编译期依赖
- 唯一新增：测试环境 JVM 的 `-javaagent` 启动参数（仅测试环境）
- 生产 / 预发环境不挂载任何探针

## 版本一致性（隔离）/ Isolation

`codeq check` 会校验**业务项目仓库当前 HEAD 与 `--release` 分支 HEAD 一致**（spec FR-007）；不一致则拒绝（退出码 `2`），避免执行数据版本错配。多任务 / 多版本的持久化数据隔离在迭代 #2（服务化）落地。
