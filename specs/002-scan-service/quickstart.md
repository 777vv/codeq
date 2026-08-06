# 快速验证：扫描服务化 / Quickstart

**特性 (Feature)**: 002-scan-service | **阶段 (Phase)**: 1 | **日期 (Date)**: 2026-08-06

## 前置 / Prerequisites
- JDK 21、Maven 3.9+
- Python 3 + `diff-cover`、git
- 业务项目（测试环境挂 Jacoco agent，见 [docs/integration-java.md](../../../docs/integration-java.md)）
- 数据库：**dev 默认 H2 内存**（零配置）；**生产 PostgreSQL**（`spring.profiles.active=prod` + 数据源配置）

## 构建 / Build
```bash
mvn -DskipTests package      # target/codeq-0.1.0-SNAPSHOT.jar
```

## 运行 / Run

**dev（H2 内存 + 彩色 console 日志）**
```bash
java -jar target/codeq-0.1.0-SNAPSHOT.jar
```

**prod（PostgreSQL + JSON 日志，宪法 VIII）**
```bash
java -Dspring.profiles.active=prod \
     -Dspring.datasource.url=jdbc:postgresql://host:5432/codeq \
     -Dspring.datasource.username=codeq -Dspring.datasource.password=*** \
     -jar target/codeq-0.1.0-SNAPSHOT.jar
```
服务监听 `8080`。

## 场景 1：US1 异步扫描（P1）

```bash
# 提交扫描 → 立即返回 taskId
curl -X POST localhost:8080/api/scans -H 'Content-Type: application/json' -d '{
  "repo":"/path/to/sample","baseline":"baseline","release":"release",
  "jacocoHost":"127.0.0.1","jacocoPort":6300}'
# → {"taskId":"...","status":"PENDING","traceId":"..."}

# 轮询状态
curl localhost:8080/api/scans/<taskId>
# 完成后取结果
curl localhost:8080/api/scans/<taskId>/result
```

## 场景 2：US2 历史回溯（P2）
```bash
curl 'localhost:8080/api/scans?project=sample&version=release'
```

## 场景 3：US3 门禁判定（P3）
```bash
curl localhost:8080/api/scans/<taskId>/verdict
# → {"pass":false,"totals":{"green":3,"red":1,"yellow":1,"partial":0}}
```

## 验证矩阵（对照 spec）
| spec 场景 | curl | 期望 |
|---|---|---|
| US1-AC1 | `POST /api/scans` | `202` + taskId + `PENDING` |
| US1-AC3 | `GET /result`（完成后） | 三色明细 |
| US1-AC4 | 提交版本不匹配的数据 | 任务 `FAILED` |
| US2-AC1 | `GET /api/scans?...` | 历史列表 |
| US3-AC2 | `GET /verdict`（有 RED） | `pass=false` |

> 完整实现与测试在 `tasks.md` / 实现阶段产出；本文件仅验证指南。
