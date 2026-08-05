package com.codeq.diff;

import com.codeq.CodeqException;
import com.codeq.ExitCode;
import com.codeq.process.ProcessRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * 增量 diff 获取（宪法第四篇 4.1）：通过 {@code git merge-base} 取双分支共同基线，
 * {@code git diff} 取增量变更清单（文件 + 变更行号）。
 */
@Component
public class GitDiffService {

    private final ProcessRunner runner;

    public GitDiffService(ProcessRunner runner) {
        this.runner = runner;
    }

    /** 双分支共同基线 commit；无共同祖先或分支不存在时抛 ERROR。 */
    public String mergeBase(File repo, String baseline, String release) {
        ProcessRunner.Result r = runner.run(
                List.of("git", "merge-base", baseline, release), repo, 120);
        if (!r.ok()) {
            throw new CodeqException(ExitCode.ERROR,
                    "git merge-base 失败（分支无共同祖先或不存在）: " + r.stderr().trim());
        }
        String base = r.stdout().trim();
        if (base.isEmpty()) {
            throw new CodeqException(ExitCode.ERROR,
                    "两分支无共同祖先: " + baseline + " / " + release);
        }
        return base;
    }

    /** 返回 release 相对 base 的变更：file → 变更行号集合（新增/修改行；删除不计）。 */
    public Map<String, TreeSet<Integer>> changedLines(File repo, String base, String release) {
        ProcessRunner.Result r = runner.run(
                List.of("git", "diff", base, release, "--unified=0", "--no-color"),
                repo, 600);
        if (!r.ok()) {
            throw new CodeqException(ExitCode.ERROR, "git diff 失败: " + r.stderr().trim());
        }
        return DiffParser.parse(r.stdout());
    }

    /** 解析 unified diff，产出 file → 新增/修改行号集合。 */
    static final class DiffParser {

        static Map<String, TreeSet<Integer>> parse(String diff) {
            Map<String, TreeSet<Integer>> map = new LinkedHashMap<>();
            String currentFile = null;
            int newLine = 0;
            for (String raw : diff.split("\n", -1)) {
                if (raw.startsWith("+++ ")) {
                    currentFile = stripPrefix(raw.substring(4).trim());
                    if (currentFile != null) {
                        map.computeIfAbsent(currentFile, k -> new TreeSet<>());
                    }
                    continue;
                }
                if (raw.startsWith("@@")) {
                    newLine = parseNewStart(raw);
                    continue;
                }
                if (currentFile == null) {
                    continue;
                }
                if (raw.startsWith("+")) {
                    map.get(currentFile).add(newLine);
                    newLine++;
                } else if (raw.startsWith("-")) {
                    // 删除行：不影响 release 新行号
                } else if (raw.startsWith(" ")) {
                    newLine++;
                }
                // 其余（"\ No newline"、文件头 "---" 等）忽略
            }
            return map;
        }

        private static int parseNewStart(String hunk) {
            int plus = hunk.indexOf('+');
            if (plus < 0) {
                return 0;
            }
            String tail = hunk.substring(plus + 1);
            int sp = tail.indexOf(' ');
            String nums = (sp >= 0) ? tail.substring(0, sp) : tail;
            int comma = nums.indexOf(',');
            try {
                return Integer.parseInt(comma >= 0 ? nums.substring(0, comma) : nums);
            } catch (NumberFormatException e) {
                return 0;
            }
        }

        private static String stripPrefix(String path) {
            if ("/dev/null".equals(path)) {
                return null;
            }
            if (path.startsWith("b/") || path.startsWith("a/")) {
                return path.substring(2);
            }
            return path;
        }
    }
}
