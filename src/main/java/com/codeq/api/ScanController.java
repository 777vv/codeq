package com.codeq.api;

import com.codeq.api.dto.CreateScanRequest;
import com.codeq.api.dto.ResultView;
import com.codeq.api.dto.ScanView;
import com.codeq.api.dto.TotalsView;
import com.codeq.api.dto.VerdictView;
import com.codeq.repo.ScanResultEntity;
import com.codeq.repo.ScanResultRepository;
import com.codeq.repo.ScanTaskEntity;
import com.codeq.repo.ScanTaskRepository;
import com.codeq.task.ScanService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * 扫描服务 REST 接口（contracts/api.md）：提交扫描 / 查状态 / 查结果 / 历史 / 门禁判定。
 *
 * @author wangtao
 * @date 2026-08-06
 */
@RestController
@RequestMapping("/api/scans")
public class ScanController {

    @Autowired
    private ScanService scanService;

    @Autowired
    private ScanTaskRepository taskRepo;

    @Autowired
    private ScanResultRepository resultRepo;

    @Autowired
    private ObjectMapper mapper;

    /** US1：提交扫描任务 → 202 + taskId + PENDING。 */
    @PostMapping
    public ResponseEntity<Map<String, String>> create(@Valid @RequestBody CreateScanRequest req) {
        String taskId = scanService.createScan(req);
        return ResponseEntity.accepted().body(Map.of("taskId", taskId, "status", "PENDING"));
    }

    /** 查任务状态/元数据。 */
    @GetMapping("/{id}")
    public ScanView get(@PathVariable String id) {
        return ScanView.of(taskRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("任务不存在: " + id)));
    }

    /** US1：查判定结果（未完成 → 409）。 */
    @GetMapping("/{id}/result")
    public ResultView result(@PathVariable String id) throws Exception {
        ScanTaskEntity task = taskRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("任务不存在: " + id));
        if (task.getStatus() != ScanTaskEntity.Status.SUCCESS) {
            throw new IllegalStateException("任务未完成: status=" + task.getStatus());
        }
        ScanResultEntity r = resultRepo.findByTaskId(id)
                .orElseThrow(() -> new NoSuchElementException("结果不存在: " + id));
        Object changes = mapper.readTree(r.getChanges());
        return new ResultView(id, r.isPass(),
                new TotalsView(r.getGreen(), r.getRed(), r.getYellow(), r.getPartial()), changes);
    }

    /** US2：历史扫描列表（按 repo/version/时间过滤 + 分页）。 */
    @GetMapping
    public Page<ScanView> history(@RequestParam(required = false) String repo,
                                  @RequestParam(required = false) String version,
                                  @RequestParam(required = false) Instant from,
                                  @RequestParam(required = false) Instant to,
                                  Pageable pageable) {
        return taskRepo.queryHistory(repo, version, from, to, pageable).map(ScanView::of);
    }

    /** US3：门禁判定查询（存在 RED → pass=false）。 */
    @GetMapping("/{id}/verdict")
    public VerdictView verdict(@PathVariable String id) {
        ScanResultEntity r = resultRepo.findByTaskId(id)
                .orElseThrow(() -> new NoSuchElementException("结果不存在: " + id));
        return new VerdictView(r.isPass(),
                new TotalsView(r.getGreen(), r.getRed(), r.getYellow(), r.getPartial()));
    }
}
