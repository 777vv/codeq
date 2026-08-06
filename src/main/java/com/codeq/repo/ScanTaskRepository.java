package com.codeq.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;

/**
 * 扫描任务仓储（Spring Data JPA）。历史回溯用 Specification 动态查询（US2）。
 *
 * @author wangtao
 * @date 2026-08-06
 */
public interface ScanTaskRepository
        extends JpaRepository<ScanTaskEntity, String>, JpaSpecificationExecutor<ScanTaskEntity> {

    /** 按项目(目录基名)/版本(release)/时间范围分页查询（US2 T013）。 */
    default Page<ScanTaskEntity> queryHistory(String repo, String release,
                                              Instant from, Instant to, Pageable pageable) {
        return findAll((root, q, cb) -> {
            var preds = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            if (repo != null && !repo.isBlank()) {
                preds.add(cb.equal(root.get("repo"), repo));
            }
            if (release != null && !release.isBlank()) {
                preds.add(cb.equal(root.get("release"), release));
            }
            if (from != null) {
                preds.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                preds.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(preds.toArray(new jakarta.persistence.criteria.Predicate[0]));
        }, pageable);
    }
}
