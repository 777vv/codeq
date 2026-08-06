package com.codeq.config;

import com.codeq.repo.ScanResultEntity;
import com.codeq.repo.ScanResultRepository;
import com.codeq.repo.ScanTaskEntity;
import com.codeq.repo.ScanTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * TTL 过期清理（宪法 4.3）：定时删除超过保留期的扫描任务与结果。
 *
 * @author wangtao
 * @date 2026-08-06
 */
@Component
public class RetentionJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

    @Autowired
    private ScanTaskRepository taskRepo;

    @Autowired
    private ScanResultRepository resultRepo;

    @Value("${codeq.retention.days:30}")
    private int days;

    /** 每日 03:00 清理（cron 可配 codeq.retention.cron）。 */
    @Scheduled(cron = "${codeq.retention.cron:0 0 3 * * *}")
    @Transactional
    public void cleanup() {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        List<ScanResultEntity> oldResults = resultRepo.findAll(
                (root, query, cb) -> cb.lessThan(root.get("createdAt"), cutoff));
        resultRepo.deleteAll(oldResults);
        List<ScanTaskEntity> oldTasks = taskRepo.findAll(
                (root, query, cb) -> cb.lessThan(root.get("createdAt"), cutoff));
        taskRepo.deleteAll(oldTasks);
        log.info("TTL 清理：删除 {} 条 result、{} 条 task（保留 {} 天，cutoff={}）",
                oldResults.size(), oldTasks.size(), days, cutoff);
    }
}
