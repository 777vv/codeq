package com.codeq.model;

/**
 * 三色判定（宪法第五篇 / spec FR-003 / FR-013）。
 * <p>GREEN / RED / YELLOW 语义固定；PARTIAL 为提示标记，不改三色语义。
 */
public enum Verdict {
    GREEN("🟢", "绿色", "合规已覆盖"),
    RED("🔴", "红色", "高危漏测"),
    YELLOW("🟡", "黄色", "无法精准匹配，需人工复核"),
    PARTIAL("◔", "partial", "部分执行，附未覆盖行明细");

    private final String icon;
    private final String label;
    private final String desc;

    Verdict(String icon, String label, String desc) {
        this.icon = icon;
        this.label = label;
        this.desc = desc;
    }

    public String icon() {
        return icon;
    }

    public String label() {
        return label;
    }

    public String desc() {
        return desc;
    }
}
