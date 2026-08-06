package com.codeq.api.dto;

/**
 * 判定结果视图（含变更明细，changes 为解析后的 JSON 节点）。
 *
 * @author wangtao
 * @date 2026-08-06
 */
public record ResultView(String taskId, boolean pass, TotalsView totals, Object changes) {
}
