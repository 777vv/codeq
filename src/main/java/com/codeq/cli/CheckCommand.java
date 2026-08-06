package com.codeq.cli;

import com.codeq.CodeqException;
import com.codeq.ExitCode;
import com.codeq.coverage.CoverageReportConverter;
import com.codeq.coverage.ExecutionDataValidator;
import com.codeq.coverage.JacocoCollector;
import com.codeq.diff.GitDiffService;
import com.codeq.diffcover.DiffCoverRunner;
import com.codeq.match.AstMatcher;
import com.codeq.model.IncrementalChange;
import com.codeq.model.IsolationKey;
import com.codeq.model.Verdict;
import com.codeq.report.ReportGenerator;
import com.codeq.verdict.VerdictEngine;
import org.jacoco.core.data.ExecutionDataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.Callable;

/**
 * {@code codeq check} —— 一键全流程检测（宪法第三篇 3.1 唯一入口）：
 * 版本一致性校验 → 增量 diff（git merge-base）→ 核心覆盖率（diff-cover）→ AST 匹配 + 三色判定 → 报告。
 * <p>执行数据来源二选一：本地 {@code --coverage-xml}（US1）或在线 {@code --jacoco-host/--jacoco-port}（US2）。
 * <p>诊断日志经 SLF4J（宪法 VIII），每次执行注入 traceId（MDC）；三色报告属业务输出，走 stdout。
 * @author wangtao
 * @date 2026-08-06
 */
@Component
@Command(name = "check",
        description = "一键全流程检测：增量 diff → 覆盖匹配 → 三色判定 → 报告",
        mixinStandardHelpOptions = true)
public class CheckCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(CheckCommand.class);

    @Autowired
    private GitDiffService gitDiff;

    @Autowired
    private AstMatcher astMatcher;

    @Autowired
    private DiffCoverRunner diffCover;

    @Autowired
    private VerdictEngine verdictEngine;

    @Autowired
    private ReportGenerator report;

    @Autowired
    private JacocoCollector jacoco;

    @Autowired
    private CoverageReportConverter converter;

    @Autowired
    private ExecutionDataValidator validator;

    @Option(names = {"--repo"}, required = true, description = "本地代码仓库路径")
    File repo;

    @Option(names = {"--baseline"}, required = true, description = "基准分支（线上稳定版本）")
    String baseline;

    @Option(names = {"--release"}, required = true, description = "待发布分支")
    String release;

    @Option(names = {"--coverage-xml"}, description = "本地 coverage.xml（与 --jacoco-host 二选一）")
    File coverageXml;

    @Option(names = {"--jacoco-host"}, description = "测试环境 Jacoco agent host（在线 dump）")
    String jacocoHost;

    @Option(names = {"--jacoco-port"}, description = "Jacoco agent TCP 端口")
    Integer jacocoPort;

    @Option(names = {"--task-id"}, defaultValue = "default", description = "测试任务/隔离标识")
    String taskId;

    @Option(names = {"--report"}, defaultValue = "console",
            description = "报告格式：console | html | json（默认 console）")
    String reportFormat;

    @Option(names = {"--out"}, description = "报告输出路径（html/json 时）")
    File out;

    @Override
    public Integer call() {
        if (!repo.isDirectory()) {
            log.error("--repo 不是有效目录: {}", repo);
            return ExitCode.ERROR.code();
        }
        MDC.put("traceId", taskId + "-" + Long.toHexString(System.nanoTime()));
        try {
            // US3 T018：版本一致性校验（执行数据须对应待发布版本）
            try {
                validator.validate(repo, release);
            } catch (CodeqException e) {
                log.error("{}", e.getMessage());
                return ExitCode.ERROR.code();
            }

            File covXml;
            try {
                covXml = resolveCoverageXml();
            } catch (Exception e) {
                log.error("{}", e.getMessage());
                return ExitCode.ERROR.code();
            }

            // 1. 增量基线 + diff（宪法 4.1：git merge-base）
            String base = gitDiff.mergeBase(repo, baseline, release);
            IsolationKey iso = new IsolationKey(repo.getName(), release, base, taskId,
                    jacocoHost != null ? jacocoHost + ":" + jacocoPort : "local-file");
            log.info("隔离键: {}", iso);

            Map<String, TreeSet<Integer>> changed = gitDiff.changedLines(repo, base, release);
            if (changed.isEmpty()) {
                log.info("零增量变更（待发布分支与基准相同），无需校验。");
                return ExitCode.OK.code();
            }
            // 2. 核心覆盖率（宪法 4.1 红线：复用 diff-cover）
            Map<String, TreeSet<Integer>> covered = diffCover.coveredLines(repo, covXml, base);
            // 3. AST 匹配 + 三色判定（宪法 4.1 / 第五篇）；输出确定性排序（US3 T020）
            List<IncrementalChange> changes =
                    verdictEngine.compute(repo, base, release, changed, covered);
            // 4. 报告（FR-004）
            emit(changes, repo.getAbsolutePath());
            // 5. 退出码：全 GREEN→0；存在 RED/YELLOW/PARTIAL→1
            boolean risk = changes.stream().anyMatch(c -> c.getVerdict() != Verdict.GREEN);
            return risk ? ExitCode.RISK.code() : ExitCode.OK.code();
        } finally {
            MDC.remove("traceId");
        }
    }

    /** 解析执行数据来源：本地 coverage.xml 或在线 dump（转 coverage.xml）。 */
    private File resolveCoverageXml() throws Exception {
        if (coverageXml != null) {
            if (!coverageXml.isFile()) {
                throw new IllegalStateException("--coverage-xml 不是有效文件: " + coverageXml);
            }
            return coverageXml;
        }
        if (jacocoHost != null && jacocoPort != null) {
            ExecutionDataStore store = jacoco.dump(jacocoHost, jacocoPort);
            File tmp = Files.createTempFile("codeq-coverage-", ".xml").toFile();
            tmp.deleteOnExit();
            converter.toXml(store, repo, tmp);
            return tmp;
        }
        throw new IllegalStateException("需提供执行数据来源：--coverage-xml 或 --jacoco-host/--jacoco-port");
    }

    private void emit(List<IncrementalChange> changes, String repoPath) {
        String fmt = reportFormat == null ? "console" : reportFormat.toLowerCase();
        switch (fmt) {
            case "json" -> writeOrPrint(report.toJson(changes, repoPath, baseline, release), out);
            case "html" -> writeOrPrint(report.toHtml(changes, repoPath, baseline, release), out);
            default -> report.printConsole(changes, repoPath, baseline, release);
        }
    }

    private void writeOrPrint(String content, File target) {
        if (target == null) {
            // 报告内容属业务输出（宪法 VIII 例外），走 stdout
            System.out.println(content);
            return;
        }
        try {
            Files.writeString(target.toPath(), content);
            log.info("报告已写入: {}", target.getAbsolutePath());
        } catch (Exception e) {
            log.error("写入报告失败: {}", e.getMessage());
        }
    }
}
