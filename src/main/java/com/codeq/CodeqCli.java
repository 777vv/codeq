package com.codeq;

import com.codeq.cli.CheckCommand;
import com.codeq.cli.CodeqCommand;
import com.codeq.cli.DumpCommand;
import com.codeq.cli.ResetCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine;

/**
 * codeq CLI 入口（宪法第四篇 4.3：Java 21 + Spring Boot 3.x；CLI 经 CommandLineRunner 形态）。
 * <p>诊断日志统一经 SLF4J（宪法 VIII）；业务报告输出走 stdout。
 * @author wangtao
 * @date 2026-08-06
 */
@SpringBootApplication
public class CodeqCli {

    private static final Logger log = LoggerFactory.getLogger(CodeqCli.class);

    public static void main(String[] args) {
        // 不把命令行 args 传给 Spring，避免其尝试解析 picocli 的 --repo 等参数
        ConfigurableApplicationContext ctx = SpringApplication.run(CodeqCli.class);
        int code;
        try {
            CodeqCommand root = ctx.getBean(CodeqCommand.class);
            CheckCommand check = ctx.getBean(CheckCommand.class);
            DumpCommand dump = ctx.getBean(DumpCommand.class);
            ResetCommand reset = ctx.getBean(ResetCommand.class);
            CommandLine cli = new CommandLine(root);
            // 用 Spring 注入的 bean（已 @Autowired），而非默认反射构造
            cli.addSubcommand("check", check);
            cli.addSubcommand("dump", dump);
            cli.addSubcommand("reset", reset);
            code = cli.execute(args);
        } catch (CodeqException e) {
            log.error("运行失败: {}", e.getMessage());
            code = e.exitCode().code();
        } catch (Exception e) {
            log.error("运行失败: {}", e.getMessage(), e);
            code = ExitCode.ERROR.code();
        } finally {
            ctx.close();
        }
        System.exit(code);
    }
}
