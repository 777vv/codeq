package com.codeq;

import com.codeq.cli.CheckCommand;
import com.codeq.cli.CodeqCommand;
import com.codeq.cli.DumpCommand;
import com.codeq.cli.ResetCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine;

import java.util.Set;

/**
 * codeq 入口（双模式）：
 * <ul>
 *   <li>命令行含子命令（check/dump/reset 或 --help）→ CLI 模式（web 容器关闭，执行后退出）</li>
 *   <li>否则 → web 服务常驻（Spring Boot REST，feature 02 扫描服务）</li>
 * </ul>
 * CLI 复用 feature 01 子命令；web 暴露 feature 02 REST 接口。
 *
 * @author wangtao
 * @date 2026-08-06
 */
@SpringBootApplication
public class CodeqCli {

    private static final Logger log = LoggerFactory.getLogger(CodeqCli.class);

    private static final Set<String> CLI_SUBCOMMANDS =
            Set.of("check", "dump", "reset", "--help", "-h", "--version", "-V");

    public static void main(String[] args) {
        boolean cliMode = args.length > 0 && CLI_SUBCOMMANDS.contains(args[0]);
        SpringApplicationBuilder builder = new SpringApplicationBuilder(CodeqCli.class);
        if (cliMode) {
            builder.web(WebApplicationType.NONE);
        }
        ConfigurableApplicationContext ctx = builder.run();
        if (cliMode) {
            runCli(ctx, args);
        }
        // web 模式：Spring Boot 常驻（Tomcat），main 返回不退出
    }

    private static void runCli(ConfigurableApplicationContext ctx, String[] args) {
        int code;
        try {
            CodeqCommand root = ctx.getBean(CodeqCommand.class);
            CheckCommand check = ctx.getBean(CheckCommand.class);
            DumpCommand dump = ctx.getBean(DumpCommand.class);
            ResetCommand reset = ctx.getBean(ResetCommand.class);
            CommandLine cli = new CommandLine(root);
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
