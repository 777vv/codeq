package com.codeq.repo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * 扫描结果实体（spec data-model）：门禁判定 pass + 三色统计 + 变更明细(JSON)。
 * 与 ScanTask 一对一，共享主键（task.id == result.id）。
 *
 * @author wangtao
 * @date 2026-08-06
 */
@Entity
@Table(name = "scan_result")
public class ScanResultEntity {

    @Id
    private String id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "task_id")
    private ScanTaskEntity task;

    private boolean pass;
    private int green;
    private int red;
    private int yellow;
    private int partial;

    /** 变更明细 JSON：IncrementalChange 列表。 */
    @Lob
    private String changes;

    private Instant createdAt;

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

    public ScanTaskEntity getTask() {
        return task;
    }

    public void setTask(ScanTaskEntity task) {
        this.task = task;
    }

    public boolean isPass() {
        return pass;
    }

    public void setPass(boolean pass) {
        this.pass = pass;
    }

    public int getGreen() {
        return green;
    }

    public void setGreen(int green) {
        this.green = green;
    }

    public int getRed() {
        return red;
    }

    public void setRed(int red) {
        this.red = red;
    }

    public int getYellow() {
        return yellow;
    }

    public void setYellow(int yellow) {
        this.yellow = yellow;
    }

    public int getPartial() {
        return partial;
    }

    public void setPartial(int partial) {
        this.partial = partial;
    }

    public String getChanges() {
        return changes;
    }

    public void setChanges(String changes) {
        this.changes = changes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
