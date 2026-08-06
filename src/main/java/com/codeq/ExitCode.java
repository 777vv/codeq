package com.codeq;

/**
 * 统一退出码（宪法第三篇 / contracts/cli.md）。
 * @author wangtao
 * @date 2026-08-06
 */
public enum ExitCode {
    /** 全部 GREEN，合规。 */
    OK(0),
    /** 存在 RED / YELLOW / PARTIAL，需人工或拦截。 */
    RISK(1),
    /** 输入错误 / 环境拒绝（如执行数据来源不匹配）。 */
    ERROR(2);

    private final int code;

    ExitCode(int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
