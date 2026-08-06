package com.codeq.task;

import com.codeq.api.dto.CreateScanRequest;
import com.codeq.coverage.CoverageReportConverter;
import com.codeq.coverage.ExecutionDataValidator;
import com.codeq.coverage.JacocoCollector;
import com.codeq.diff.GitDiffService;
import com.codeq.diffcover.DiffCoverRunner;
import com.codeq.match.AstMatcher;
import com.codeq.model.IncrementalChange;
import com.codeq.model.IsolationKey;
import com.codeq.model.Verdict;
import com.codeq.repo.ScanResultEntity;
import com.codeq.repo.ScanResultRepository;
import com.codeq.repo.ScanTaskEntity;
import com.codeq.repo.ScanTaskRepository;
import com.codeq.verdict.VerdictEngine;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jacoco.core.data.ExecutionDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;

/**
 * 扫描服务：创建任务(PENDING)落库 → {@code @Async} 异步执行（复用 feature 01 核心判定链路，FR-008）
 * → 落 ScanResult + SUCCESS/FAILED。traceId 经 TaskDecorator 贯穿异步（宪法 VIII）。
 *
 * @author wangtao
 * @date 2026-08-06
 */
@Service
public class ScanService {

    private static final Logger log = LoggerFactory.getLogger(ScanService.class);

    private final ScanTaskRepository taskRepo;
    private final ScanResultRepository resultRepo;
    private final ExecutionDataValidator validator;
    private final JacocoCollector jacoco;
    private final CoverageReportConverter converter;
    private final GitDiffService gitDiff;
    private final AstMatcher astMatcher;
    private final DiffCoverRunner diffCover;
    private final VerdictEngine verdictEngine;
    private final ObjectMapper mapper;

    /** 自注入以触发 @Async 代理（避免 self-invocation 致异步失效）。 */
    @Autowired
    @Lazy
    private ScanService self;

    public ScanService(ScanTaskRepository taskRepo, ScanResultRepository resultRepo,
                       ExecutionDataValidator validator, JacocoCollector jacoco,
                       CoverageReportConverter converter, GitDiffService gitDiff,
                       AstMatcher astMatcher, DiffCoverRunner diffCover,
                       VerdictEngine verdictEngine, ObjectMapper mapper) {
        this.taskRepo = taskRepo;
        this.resultRepo = resultRepo;
        this.validator = validator;
        this.jacoco = jacoco;
        this.converter = converter;
        this.gitDiff = gitDiff;
        this.astMatcher = astMatcher;
        this.diffCover = diffCover;
        this.verdictEngine = verdictEngine;
        this.mapper = mapper;
    }

    /** 创建扫描任务（同步）→ 落 PENDING → 触发异步执行 → 返回 taskId。 */
    @Transactional
    public String createScan(CreateScanRequest req) {
        String taskId = (req.taskId() != null && !req.taskId().isBlank())
                ? req.taskId() : UUID.randomUUID().toString();
        String traceId = taskId + "-" + Long.toHexString(System.nanoTime());

        ScanTaskEntity task = new ScanTaskEntity();
        task.setId(taskId);
        task.setStatus(ScanTaskEntity.Status.PENDING);
        task.setRepo(req.repo());
        task.setBaseline(req.baseline());
        task.setRelease(req.release());
        task.setJacocoHost(req.jacocoHost());
        task.setJacocoPort(req.jacocoPort());
        task.setCoverageXmlPath(req.coverageXmlPath());
        task.setTraceId(traceId);
        String instance = req.jacocoHost() != null ? req.jacocoHost() + ":" + req.jacocoPort() : "local-file";
        task.setIsolationKey(new IsolationKey(new File(req.repo()).getName(), req.release(),
                "?", req.taskId() != null ? req.taskId() : "default", instance).toString());
        taskRepo.save(task);

        log.info("提交扫描任务 taskId={} repo={}", taskId, req.repo());
        MDC.put("traceId", traceId);
        try {
            self.executeScan(taskId);
        } finally {
            MDC.remove("traceId");
        }
        return taskId;
    }

    /** 异步执行扫描全流程（复用 feature 01 核心）。 */
    @Async("scanExecutor")
    public void executeScan(String taskId) {
        ScanTaskEntity task = taskRepo.findById(taskId).orElseThrow();
        MDC.put("traceId", task.getTraceId());
        File repoDir = new File(task.getRepo());
        File tmpCoverage = null;
        try {
            task.setStatus(ScanTaskEntity.Status.RUNNING);
            task.setStartedAt(Instant.now());
            taskRepo.save(task);

            validator.validate(repoDir, task.getRelease());
            File covXml = resolveCoverage(task, repoDir);
            if (task.getCoverageXmlPath() == null || task.getCoverageXmlPath().isBlank()) {
                tmpCoverage = covXml;
            }

            String base = gitDiff.mergeBase(repoDir, task.getBaseline(), task.getRelease());
            Map<String, TreeSet<Integer>> changed = gitDiff.changedLines(repoDir, base, task.getRelease());
            List<IncrementalChange> changes = changed.isEmpty()
                    ? List.of()
                    : verdictEngine.compute(repoDir, base, task.getRelease(), changed,
                            diffCover.coveredLines(repoDir, covXml, base));

            int green = count(changes, Verdict.GREEN);
            int red = count(changes, Verdict.RED);
            int yellow = count(changes, Verdict.YELLOW);
            int partial = count(changes, Verdict.PARTIAL);
            boolean pass = (red == 0);

            ScanResultEntity result = new ScanResultEntity();
            result.setTask(task);
            result.setPass(pass);
            result.setGreen(green);
            result.setRed(red);
            result.setYellow(yellow);
            result.setPartial(partial);
            result.setChanges(mapper.writeValueAsString(changes));
            resultRepo.save(result);

            task.setStatus(ScanTaskEntity.Status.SUCCESS);
            task.setFinishedAt(Instant.now());
            taskRepo.save(task);
            log.info("扫描完成 taskId={} pass={} G/R/Y/P={}/{}/{}/{}", taskId, pass, green, red, yellow, partial);
        } catch (Exception e) {
            task.setStatus(ScanTaskEntity.Status.FAILED);
            task.setErrorMsg(e.getMessage());
            task.setFinishedAt(Instant.now());
            taskRepo.save(task);
            log.error("扫描失败 taskId={}: {}", taskId, e.getMessage(), e);
        } finally {
            if (tmpCoverage != null) {
                tmpCoverage.delete();
            }
            MDC.clear();
        }
    }

    private File resolveCoverage(ScanTaskEntity task, File repoDir) throws Exception {
        if (task.getCoverageXmlPath() != null && !task.getCoverageXmlPath().isBlank()) {
            return new File(task.getCoverageXmlPath());
        }
        if (task.getJacocoHost() != null && task.getJacocoPort() != null) {
            ExecutionDataStore store = jacoco.dump(task.getJacocoHost(), task.getJacocoPort());
            File tmp = Files.createTempFile("codeq-coverage-", ".xml").toFile();
            tmp.deleteOnExit();
            converter.toXml(store, repoDir, tmp);
            return tmp;
        }
        throw new IllegalStateException("需提供执行数据来源：coverageXmlPath 或 jacocoHost/port");
    }

    private int count(List<IncrementalChange> changes, Verdict v) {
        return (int) changes.stream().filter(c -> c.getVerdict() == v).count();
    }
}
