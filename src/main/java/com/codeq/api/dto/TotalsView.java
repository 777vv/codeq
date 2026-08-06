package com.codeq.api.dto;

/**
 * 三色统计（green/red/yellow/partial）。
 *
 * @author wangtao
 * @date 2026-08-06
 */
public record TotalsView(int green, int red, int yellow, int partial) {
}
