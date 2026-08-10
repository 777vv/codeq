package com.codeq.model;

import java.util.List;

/**
 * 增量变更：一个变更方法（或方法外变更）及其判定（spec FR-003 / FR-013）。
 * feature 04 增强：fingerprint（方法结构指纹，US1）+ refactorFlag（重构标注，US2）。
 *
 * @author wangtao
 * @date 2026-08-06
 */
public class IncrementalChange {

    /** 变更类型。 */
    public enum Type { ADD, MODIFY }

    private String file;
    private Type changeType;
    /** AST 归约所得主键；为 null 表示无法匹配 → YELLOW。 */
    private MethodKey methodKey;
    /** 变更行号（仅可视化用）。 */
    private List<Integer> changedLines;
    /** diff-cover 判定已执行的变更行（changedLines 的子集）。 */
    private List<Integer> executedLines;
    private Verdict verdict;
    /** 方法结构指纹（US1，feature 04）；方法外变更时为 null。 */
    private String fingerprint;
    /** 重构标注（US2，feature 04），默认 NONE。 */
    private RefactorFlag refactorFlag = RefactorFlag.NONE;

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public Type getChangeType() {
        return changeType;
    }

    public void setChangeType(Type changeType) {
        this.changeType = changeType;
    }

    public MethodKey getMethodKey() {
        return methodKey;
    }

    public void setMethodKey(MethodKey methodKey) {
        this.methodKey = methodKey;
    }

    public List<Integer> getChangedLines() {
        return changedLines;
    }

    public void setChangedLines(List<Integer> changedLines) {
        this.changedLines = changedLines;
    }

    public List<Integer> getExecutedLines() {
        return executedLines;
    }

    public void setExecutedLines(List<Integer> executedLines) {
        this.executedLines = executedLines;
    }

    public Verdict getVerdict() {
        return verdict;
    }

    public void setVerdict(Verdict verdict) {
        this.verdict = verdict;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public void setFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public RefactorFlag getRefactorFlag() {
        return refactorFlag;
    }

    public void setRefactorFlag(RefactorFlag refactorFlag) {
        this.refactorFlag = refactorFlag;
    }
}
