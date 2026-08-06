package com.codeq.coverage;

import com.codeq.CodeqException;
import com.codeq.ExitCode;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.core.tools.ExecDumpClient;
import org.jacoco.core.tools.ExecFileLoader;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 测试环境执行数据采集（宪法第四篇 4.2）：org.jacoco.core 的 TCP 服务模式，
 * 支持远程动态 dump、多轮累加（合并）、一键 reset。仅用于测试环境实例。
 * @author wangtao
 * @date 2026-08-06
 */
@Component
public class JacocoCollector {

    /** 远程 dump：拉取测试环境 agent 当前执行数据。 */
    public ExecutionDataStore dump(String host, int port) {
        ExecDumpClient client = new ExecDumpClient();
        try {
            ExecFileLoader loader = client.dump(host, port);
            return loader.getExecutionDataStore();
        } catch (Exception e) {
            throw new CodeqException(ExitCode.ERROR,
                    "Jacoco dump 失败（host=" + host + ", port=" + port + "）: " + e.getMessage(), e);
        }
    }

    /** 远程 reset：重置 agent 计数（dump 后清零本次覆盖数据）。 */
    public void reset(String host, int port) {
        ExecDumpClient client = new ExecDumpClient();
        client.setReset(true);
        try {
            client.dump(host, port);
        } catch (Exception e) {
            throw new CodeqException(ExitCode.ERROR,
                    "Jacoco reset 失败（host=" + host + ", port=" + port + "）: " + e.getMessage(), e);
        }
    }

    /** 多轮累加：合并多个 dump 结果（probe 按 OR 合并）。 */
    public ExecutionDataStore merge(ExecutionDataStore... stores) {
        ExecutionDataStore merged = new ExecutionDataStore();
        Arrays.stream(stores)
                .filter(s -> s != null)
                .forEach(s -> s.accept(merged));
        return merged;
    }
}
