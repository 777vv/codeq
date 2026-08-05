package com.codeq.model;

/**
 * 方法主键（宪法第四篇 4.1）：类全限定名 + 方法签名 + 路由标识。
 * <p>行号仅用于可视化，不作匹配主键。
 */
public record MethodKey(String className, String signature, String route) {

    @Override
    public String toString() {
        String base = className + "#" + signature;
        return (route == null || route.isEmpty()) ? base : base + "  [route=" + route + "]";
    }
}
