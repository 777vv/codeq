package com.codeq.model;

/**
 * 方法指纹（US1，RefactorDetector 输入单元）：className + signature + structureHash（规范化 AST 的 SHA-256）。
 *
 * @author wangtao
 * @date 2026-08-07
 */
public record MethodFingerprint(String className, String signature, String structureHash) {
}
