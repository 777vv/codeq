package com.codeq;

/**
 * codeq 运行时异常，携带统一退出码（ExitCode）。
 */
public class CodeqException extends RuntimeException {

    private final ExitCode exitCode;

    public CodeqException(ExitCode exitCode, String message) {
        super(message);
        this.exitCode = exitCode;
    }

    public CodeqException(ExitCode exitCode, String message, Throwable cause) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public ExitCode exitCode() {
        return exitCode;
    }
}
