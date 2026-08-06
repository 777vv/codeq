package com.codeq.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

/**
 * 扫描结果仓储（Spring Data JPA）。
 *
 * @author wangtao
 * @date 2026-08-06
 */
public interface ScanResultRepository
        extends JpaRepository<ScanResultEntity, String>, JpaSpecificationExecutor<ScanResultEntity> {

    Optional<ScanResultEntity> findByTaskId(String taskId);
}
