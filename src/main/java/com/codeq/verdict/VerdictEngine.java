package com.codeq.verdict;

import com.codeq.match.AstMatcher;
import com.codeq.model.IncrementalChange;
import com.codeq.model.MethodKey;
import com.codeq.model.Verdict;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * 三色判定引擎（spec FR-003 / FR-013 / 宪法第五篇）。
 * <p>结合 AstMatcher 的行→方法归约 + diff-cover 的行级执行结果，产出每个变更方法（或方法外变更）的判定：
 * 全执行→GREEN，从未执行→RED，部分执行→PARTIAL（附未覆盖行明细），无法归约→YELLOW。
 * <p>输出做确定性排序（spec FR-010 / US3 T020）：相同输入→相同输出，无随机、无时钟依赖。
 * @author wangtao
 * @date 2026-08-06
 */
@Component
public class VerdictEngine {

    private final AstMatcher astMatcher;

    public VerdictEngine(AstMatcher astMatcher) {
        this.astMatcher = astMatcher;
    }

    public List<IncrementalChange> compute(File repo,
                                           String baseline, String release,
                                           Map<String, TreeSet<Integer>> changedByFile,
                                           Map<String, TreeSet<Integer>> coveredByFile) {
        List<IncrementalChange> result = new ArrayList<>();
        for (Map.Entry<String, TreeSet<Integer>> entry : changedByFile.entrySet()) {
            String file = entry.getKey();
            Set<Integer> changed = entry.getValue();
            Set<Integer> covered = coveredByFile.getOrDefault(file, new TreeSet<>());
            Map<Integer, MethodKey> lineToMethod = astMatcher.mapLinesToMethods(repo, file, changed);

            Map<MethodKey, List<Integer>> byMethod = new LinkedHashMap<>();
            List<Integer> unmapped = new ArrayList<>();
            for (Integer ln : changed) {
                MethodKey mk = lineToMethod.get(ln);
                if (mk == null) {
                    unmapped.add(ln);
                } else {
                    byMethod.computeIfAbsent(mk, k -> new ArrayList<>()).add(ln);
                }
            }

            byMethod.forEach((mk, lines) -> {
                IncrementalChange c = new IncrementalChange();
                c.setFile(file);
                c.setMethodKey(mk);
                c.setChangedLines(lines);
                c.setExecutedLines(executedOf(lines, covered));
                c.setVerdict(verdictOf(c.getChangedLines(), c.getExecutedLines()));
                result.add(c);
            });
            if (!unmapped.isEmpty()) {
                IncrementalChange c = new IncrementalChange();
                c.setFile(file);
                c.setMethodKey(null);
                c.setChangedLines(unmapped);
                c.setExecutedLines(executedOf(unmapped, covered));
                c.setVerdict(Verdict.YELLOW);
                result.add(c);
            }
        }
        result.sort(Comparator
                .comparing((IncrementalChange c) -> c.getFile())
                .thenComparingInt(c -> firstLine(c.getChangedLines())));
        return result;
    }

    private List<Integer> executedOf(List<Integer> lines, Set<Integer> covered) {
        return lines.stream().filter(covered::contains).collect(Collectors.toList());
    }

    private Verdict verdictOf(List<Integer> changed, List<Integer> executed) {
        if (executed.isEmpty()) {
            return Verdict.RED;
        }
        if (executed.size() == changed.size()) {
            return Verdict.GREEN;
        }
        return Verdict.PARTIAL;
    }

    private int firstLine(List<Integer> lines) {
        return lines.isEmpty() ? Integer.MAX_VALUE : lines.get(0);
    }
}
