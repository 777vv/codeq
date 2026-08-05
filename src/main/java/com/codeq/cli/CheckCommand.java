package com.codeq.cli;

import com.codeq.ExitCode;
import com.codeq.diff.GitDiffService;
import com.codeq.diffcover.DiffCoverRunner;
import com.codeq.match.AstMatcher;
import com.codeq.model.IncrementalChange;
import com.codeq.model.Verdict;
import com.codeq.report.ReportGenerator;
import com.codeq.verdict.VerdictEngine;
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
 * 增量 diff（git merge-base）→ 核心覆盖率（diff-cover）→ AST 匹配 + 三色判定 → 报告。
 * <p>US1 MVP 用本地 {@code --coverage-xml} 作为执行数据输入；US2 将支持 {@code --jacoco-host/--jacoco-port} 在线 dump。
 */
@Component
@Command(name = "check",
        description = "一键全流程检测：增量 diff → 覆盖匹配 → 三色判定 → 报告",
        mixinStandardHelpOptions = true)
public class CheckCommand implements Callable<Integer> {

    @Autowired private GitDiffService gitDiff;
    @Autowired private AstMatcher astMatcher;
    @Autowired private DiffCoverRunner diffCover;
    @Autowired private VerdictEngine verdictEngine;
    @Autowired private ReportGenerator report;

    @Option(names = {"--repo"}, required = true, description = "本地代码仓库路径")
    File repo;

    @Option(names = {"--baseline"}, required = true, description = "基准分支（线上稳定版本）")
    String baseline;

    @Option(names = {"--release"}, required = true, description = "待发布分支")
    String release;

    @Option(names = {"--coverage-xml"}, required = true,
            description = "执行数据 coverage.xml（US1 MVP 本地输入）")
    File coverageXml;

    @Option(names = {"--report"}, defaultValue = "console",
            description = "报告格式：console | html | json（默认 console）")
    String reportFormat;

    @Option(names = {"--out"}, description = "报告输出路径（html/json 时）")
    File out;

    @Override
    public Integer call() {
        if (!repo.isDirectory()) {
            System.err.println("错误: --repo 不是有效目录: " + repo);
            return ExitCode.ERROR.code();
        }
        if (!coverageXml.isFile()) {
            System.err.println("错误: --coverage-xml 不是有效文件: " + coverageXml);
            return ExitCode.ERROR.code();
        }

        // 1. 增量基线 + diff（宪法 4.1：git merge-base）
        String base = gitDiff.mergeBase(repo, baseline, release);
        Map<String, TreeSet<Integer>> changed = gitDiff.changedLines(repo, base, release);
        if (changed.isEmpty()) {
            System.out.println("零增量变更（待发布分支与基准相同），无需校验。");
            return ExitCode.OK.code();
        }
        // 2. 核心覆盖率（宪法 4.1 红线：复用 diff-cover）
        Map<String, TreeSet<Integer>> covered = diffCover.coveredLines(repo, coverageXml, base);
        // 3. AST 匹配 + 三色判定（宪法 4.1 / 第五篇）
        List<IncrementalChange> changes =
                verdictEngine.compute(repo, base, release, changed, covered);
        // 4. 报告（FR-004）
        emit(changes, repo.getAbsolutePath());
        // 5. 退出码：全 GREEN→0；存在 RED/YELLOW/PARTIAL→1
        boolean risk = changes.stream().anyMatch(c -> c.getVerdict() != Verdict.GREEN);
        return risk ? ExitCode.RISK.code() : ExitCode.OK.code();
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
            System.out.println(content);
            return;
        }
        try {
            Files.writeString(target.toPath(), content);
            System.err.println("报告已写入: " + target.getAbsolutePath());
        } catch (Exception e) {
            System.err.println("写入报告失败: " + e.getMessage());
        }
    }
}
