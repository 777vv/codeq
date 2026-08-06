package com.codeq.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 提交扫描任务请求（US1，contracts/api.md）。
 *
 * @author wangtao
 * @date 2026-08-06
 */
public record CreateScanRequest(
        @NotBlank String repo,
        @NotBlank String baseline,
        @NotBlank String release,
        String jacocoHost,
        Integer jacocoPort,
        String coverageXmlPath,
        String taskId
) {
}
