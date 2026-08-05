package com.codeq.process;

import com.codeq.CodeqException;
import com.codeq.ExitCode;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 子进程通用封装：统一调用外部命令（git / diff-cover），处理超时、UTF-8、退出码与输出捕获。
 * <p>宪法第四篇 4.1：git merge-base / diff-cover 均经命令调用。
 */
@Component
public class ProcessRunner {

    public record Result(int exitCode, String stdout, String stderr) {
        public boolean ok() {
            return exitCode == 0;
        }
    }

    public Result run(List<String> command, File dir, long timeoutSeconds) {
        ProcessBuilder pb = new ProcessBuilder(command);
        if (dir != null) {
            pb.directory(dir);
        }
        pb.redirectErrorStream(false);
        try {
            Process p = pb.start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            String err = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                throw new CodeqException(ExitCode.ERROR, "子进程超时: " + String.join(" ", command));
            }
            return new Result(p.exitValue(), out, err);
        } catch (IOException e) {
            throw new CodeqException(ExitCode.ERROR, "子进程启动失败（命令可能未安装）: " + command.get(0)
                    + " — " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CodeqException(ExitCode.ERROR, "子进程被中断: " + command.get(0), e);
        }
    }
}
