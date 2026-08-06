package com.codeq.repo;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 扫描任务实体（spec data-model）：任务全生命周期与判定元数据。
 *
 * @author wangtao
 * @date 2026-08-06
 */
@Entity
@Table(name = "scan_task")
public class ScanTaskEntity {

    /** 任务状态机。 */
    public enum Status { PENDING, RUNNING, SUCCESS, FAILED }

    @Id
    private String id;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String repo;
    private String baseline;
    private String release;
    private String jacocoHost;
    private Integer jacocoPort;
    private String coverageXmlPath;
    private String isolationKey;
    private String traceId;

    @Lob
    private String errorMsg;

    private Instant createdAt;
    private Instant startedAt;
    private Instant finishedAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getBaseline() {
        return baseline;
    }

    public void setBaseline(String baseline) {
        this.baseline = baseline;
    }

    public String getRelease() {
        return release;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public String getJacocoHost() {
        return jacocoHost;
    }

    public void setJacocoHost(String jacocoHost) {
        this.jacocoHost = jacocoHost;
    }

    public Integer getJacocoPort() {
        return jacocoPort;
    }

    public void setJacocoPort(Integer jacocoPort) {
        this.jacocoPort = jacocoPort;
    }

    public String getCoverageXmlPath() {
        return coverageXmlPath;
    }

    public void setCoverageXmlPath(String coverageXmlPath) {
        this.coverageXmlPath = coverageXmlPath;
    }

    public String getIsolationKey() {
        return isolationKey;
    }

    public void setIsolationKey(String isolationKey) {
        this.isolationKey = isolationKey;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
