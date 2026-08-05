package com.codeq;

import com.codeq.cli.CheckCommand;
import com.codeq.cli.CodeqCommand;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine;

/**
 * codeq CLI 入口（宪法第四篇 4.3：Java 21 + Spring Boot 3.x；CLI 经 CommandLineRunner 形态）。
 * <p>main 启动 Spring 容器，取 Picocli 根命令 bean 并以 Spring 注入的 CheckCommand 注册为子命令，
 * 执行命令行参数后以退出码退出。
 */
@SpringBootApplication
public class CodeqCli {

    public static void main(String[] args) {
        // 不把命令行 args 传给 Spring，避免其尝试解析 picocli 的 --repo 等参数
        ConfigurableApplicationContext ctx = SpringApplication.run(CodeqCli.class);
        int code;
        try {
            CodeqCommand root = ctx.getBean(CodeqCommand.class);
            CheckCommand check = ctx.getBean(CheckCommand.class);
            CommandLine cli = new CommandLine(root);
            // 用 Spring 注入的 bean（已 @Autowired），而非默认反射构造
            cli.addSubcommand("check", check);
            code = cli.execute(args);
        } catch (CodeqException e) {
            System.err.println("错误: " + e.getMessage());
            code = e.exitCode().code();
        } catch (Exception e) {
            System.err.println("错误: " + e.getMessage());
            code = ExitCode.ERROR.code();
        } finally {
            ctx.close();
        }
        System.exit(code);
    }
}
