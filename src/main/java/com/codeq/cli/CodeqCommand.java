package com.codeq.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * Picocli 根命令。子命令在 CodeqCli.main 中以 Spring 注入的 bean 形式注册（确保 @Autowired 生效）。
 */
@Component
@Command(name = "codeq",
        description = "codeq 发布前增量代码覆盖安检 — 三色（绿/红/黄）增量覆盖判定（基础链路 CLI）",
        mixinStandardHelpOptions = true)
public class CodeqCommand implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }
}
