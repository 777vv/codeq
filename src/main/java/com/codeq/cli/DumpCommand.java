package com.codeq.cli;

import com.codeq.ExitCode;
import com.codeq.coverage.JacocoCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * {@code codeq dump} —— 从测试环境 Jacoco agent 拉取执行数据（spec FR-005）。
 * <p>用于确认测试环境连通性；实际判定由 {@code check --jacoco-host/--jacoco-port} 内部自动 dump+转换。
 */
@Component
@Command(name = "dump",
        description = "从测试环境 Jacoco agent（TCP 服务模式）拉取执行数据",
        mixinStandardHelpOptions = true)
public class DumpCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(DumpCommand.class);

    @Autowired
    private JacocoCollector collector;

    @Option(names = {"--jacoco-host"}, required = true, description = "测试环境 Jacoco agent host")
    String host;

    @Option(names = {"--jacoco-port"}, required = true, description = "Jacoco agent TCP 端口")
    int port;

    @Override
    public Integer call() {
        collector.dump(host, port);
        log.info("已从 {}:{} 拉取 Jacoco 执行数据", host, port);
        return ExitCode.OK.code();
    }
}
