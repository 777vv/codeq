package com.codeq.cli;

import com.codeq.ExitCode;
import com.codeq.coverage.JacocoCollector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * {@code codeq reset} —— 重置测试环境 Jacoco agent 计数（spec FR-006）。
 */
@Component
@Command(name = "reset",
        description = "重置测试环境 Jacoco agent 计数（一键重置本次覆盖数据）",
        mixinStandardHelpOptions = true)
public class ResetCommand implements Callable<Integer> {

    @Autowired private JacocoCollector collector;

    @Option(names = {"--jacoco-host"}, required = true, description = "测试环境 Jacoco agent host")
    String host;

    @Option(names = {"--jacoco-port"}, required = true, description = "Jacoco agent TCP 端口")
    int port;

    @Override
    public Integer call() {
        collector.reset(host, port);
        System.out.println("已重置 " + host + ":" + port + " 的 Jacoco 计数。");
        return ExitCode.OK.code();
    }
}
