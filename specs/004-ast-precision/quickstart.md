# 快速验证：AST 精准匹配优化 / Quickstart

**特性 (Feature)**: 004-ast-precision | **阶段 (Phase)**: 1 | **日期 (Date)**: 2026-08-07

基于 `codeq-demo`（见主 README 形态 A），构造场景验证 AST 增强。

## 前置 / Prerequisites
- codeq 已构建：`target/codeq-0.1.0-SNAPSHOT.jar`
- demo 项目（`codeq-demo`，baseline/release 分支）

## 场景 1：US1 方法身份指纹（变量重命名）
- release 的 `sub` 方法内变量重命名（`a`→`x`）+ 测试执行 → codeq 仍识别 `sub`（指纹稳定）→ 覆盖判定不变（执行则 GREEN）。
- **期望**：变量重命名不改变方法身份，判定与重命名前一致。

## 场景 2：US2 重构识别（签名变更）
- `sub` 方法签名改为 `sub(long, long)` + 测试调用新签名 → codeq 标注「重构变更」+ 正确判定（非误报 RED）。
- **期望**：报告出现 `SIGNATURE_CHANGE` 标注；被执行则非 RED。

## 场景 3：US3 路由组合
- demo 加 Spring Controller：类 `@RequestMapping("/api")` + 方法 `@GetMapping("/foo")` → 路由主键 = `/api/foo`。
- **期望**：`MethodKey.route` 为组合路径 `/api/foo`。

## 验证
```bash
java -jar target/codeq-0.1.0-SNAPSHOT.jar check \
  --repo <demo> --baseline baseline --release release --coverage-xml <demo>/coverage.xml
```
观察报告：指纹稳定（US1）、重构标注（US2）、组合路由（US3）。

> 完整实现与测试在 `tasks.md` / 实现阶段产出；本文件仅验证指南。
