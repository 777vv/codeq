# 数据模型：AST 精准匹配优化 / Data Model

**特性 (Feature)**: 004-ast-precision | **阶段 (Phase)**: 1（设计）| **日期 (Date)**: 2026-08-07

增强 feature 01 的 `match` 层。新增值对象（无持久化）。

## 新增实体 / Entities

### MethodFingerprint（方法指纹）
- `className: String`
- `signature: String`
- `structureHash: String`（规范化 AST 的 SHA-256）

### RefactorFlag（重构标注）
- 枚举 `NONE` / `SIGNATURE_CHANGE` / `METHOD_MOVE`

### Route（增强）
- 组合路径 = 类级 `@RequestMapping` 前缀 + 方法级注解路径（斜杠归一）

## 增强 IncrementalChange（feature 01）

- `methodKey.route`：组合路径（US3）
- 新增 `refactorFlag: RefactorFlag`（US2，默认 `NONE`）
- 新增 `fingerprint: String`（US1，方法结构 hash）

## 状态 / 流程

```text
AST 解析（baseline + release）→ 方法指纹 → 跨版本匹配 → 识别签名变更/移动
                                ↓
                        路由组合（类级 + 方法级）
```

## 校验 / Validation

- 指纹确定性：相同 AST → 相同 hash（FR-007）。
- 路由组合斜杠归一：`/api` + `/foo` → `/api/foo`；`/api` + `/` → `/api`。
- 重构标注仅当 baseline 存在相似指纹方法时给出。
