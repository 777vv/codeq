package com.codeq.task;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步配置（宪法第四篇 4.3 任务全异步化 + 宪法 VIII traceId 贯穿）。
 * <p>ThreadPoolTaskExecutor 执行扫描；TaskDecorator 把提交线程的 traceId(MDC) 透传到执行线程。
 *
 * @author wangtao
 * @date 2026-08-06
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "scanExecutor")
    public Executor scanExecutor(@Value("${codeq.scan.thread-pool-size:4}") int poolSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setThreadNamePrefix("scan-");
        executor.setTaskDecorator(new TraceIdTaskDecorator());
        executor.initialize();
        return executor;
    }

    /** 把主线程 MDC 的 traceId 透传到异步执行线程（宪法 VIII）。 */
    static class TraceIdTaskDecorator implements TaskDecorator {
        @Override
        public Runnable decorate(Runnable runnable) {
            String traceId = MDC.get("traceId");
            return () -> {
                try {
                    if (traceId != null) {
                        MDC.put("traceId", traceId);
                    }
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        }
    }
}
