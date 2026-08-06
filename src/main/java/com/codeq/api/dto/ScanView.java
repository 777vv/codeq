package com.codeq.api.dto;

import com.codeq.repo.ScanTaskEntity;

import java.time.Instant;

/**
 * 任务状态/元数据视图。
 *
 * @author wangtao
 * @date 2026-08-06
 */
public record ScanView(String taskId, String status, String repo, String baseline, String release,
                       Instant createdAt, Instant startedAt, Instant finishedAt, String errorMsg) {

    public static ScanView of(ScanTaskEntity t) {
        return new ScanView(t.getId(), t.getStatus().name(), t.getRepo(), t.getBaseline(),
                t.getRelease(), t.getCreatedAt(), t.getStartedAt(), t.getFinishedAt(), t.getErrorMsg());
    }
}
