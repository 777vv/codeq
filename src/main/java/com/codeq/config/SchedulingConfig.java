package com.codeq.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 启用定时任务（宪法 4.3 TTL 过期清理，见 {@code RetentionJob}）。
 *
 * @author wangtao
 * @date 2026-08-06
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
