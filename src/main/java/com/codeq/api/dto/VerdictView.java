package com.codeq.api.dto;

/**
 * 门禁判定视图（US3）：pass + 三色统计。存在 RED → pass=false（宪法红线）。
 *
 * @author wangtao
 * @date 2026-08-06
 */
public record VerdictView(boolean pass, TotalsView totals) {
}
