package com.codeq.model;

/**
 * 执行数据隔离键（宪法第四篇 4.2）：按 项目/版本/commit/任务/实例 绑定执行数据，防交叉污染。
 * <p>MVP：用于日志标识与可追溯；持久化多任务隔离在迭代 #2（服务化）落地。
 */
public record IsolationKey(String project, String version, String commit, String taskId, String instance) {

    @Override
    public String toString() {
        return "project=" + project + "  version=" + version + "  commit=" + shortHash(commit)
                + "  task=" + taskId + "  instance=" + instance;
    }

    private String shortHash(String h) {
        return (h == null || h.length() < 8) ? String.valueOf(h) : h.substring(0, 8);
    }
}
