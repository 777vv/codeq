package com.codeq.model;

/**
 * AstMatcher 归约结果：方法主键 + 指纹（US1，feature 04 增强）。
 *
 * @author wangtao
 * @date 2026-08-07
 */
public record MethodMatch(MethodKey key, String fingerprint) {
}
