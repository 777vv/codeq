package com.codeq.diffcover;

import com.codeq.CodeqException;
import com.codeq.ExitCode;
import com.codeq.process.ProcessRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * 核心覆盖率计算（宪法第四篇 4.1 红线）：复用开源 {@code diff-cover}（Python，子进程调用），
 * 禁止自研核心比对算法——本类仅做「调用 + 解析」的业务适配封装。
 * <p>输入 Jacoco/Cobertura 的 coverage.xml + 基准分支，输出各文件已被执行的行号集合。
 * @author wangtao
 * @date 2026-08-06
 */
@Component
public class DiffCoverRunner {

    private final ProcessRunner runner;
    private final ObjectMapper mapper = new ObjectMapper();

    public DiffCoverRunner(ProcessRunner runner) {
        this.runner = runner;
    }

    /** 返回基准分支 diff 中各文件已被执行的行号集合。 */
    public Map<String, TreeSet<Integer>> coveredLines(File repo, File coverageXml, String baseline) {
        File report;
        try {
            report = Files.createTempFile("codeq-diffcover-", ".json").toFile();
            report.deleteOnExit();
        } catch (Exception e) {
            throw new CodeqException(ExitCode.ERROR, "创建临时文件失败", e);
        }
        List<String> cmd = List.of(
                "diff-cover", coverageXml.getAbsolutePath(),
                "--compare-branch=" + baseline,
                "--json-report=" + report.getAbsolutePath());
        ProcessRunner.Result r = runner.run(cmd, repo, 600);
        if (!r.ok()) {
            throw new CodeqException(ExitCode.ERROR,
                    "diff-cover 执行失败（已安装 Python 3 + pip install diff-cover？）: "
                            + r.stderr().trim());
        }
        try {
            return parse(mapper.readTree(report));
        } catch (CodeqException e) {
            throw e;
        } catch (Exception e) {
            throw new CodeqException(ExitCode.ERROR, "解析 diff-cover 报告失败", e);
        } finally {
            report.delete();
        }
    }

    /** 兼容 diff-cover 不同版本的字段名（covered_lines / executed）。 */
    private Map<String, TreeSet<Integer>> parse(JsonNode root) {
        Map<String, TreeSet<Integer>> map = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> it = root.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> entry = it.next();
            String file = normalize(entry.getKey());
            TreeSet<Integer> set = map.computeIfAbsent(file, k -> new TreeSet<>());
            JsonNode covered = entry.getValue().get("covered_lines");
            if (covered == null) {
                covered = entry.getValue().get("executed");
            }
            if (covered != null) {
                for (JsonNode pair : covered) {
                    if (pair.isArray() && pair.size() == 2) {
                        int start = pair.get(0).asInt();
                        int end = pair.get(1).asInt();
                        for (int i = start; i <= end; i++) {
                            set.add(i);
                        }
                    }
                }
            }
        }
        return map;
    }

    private String normalize(String key) {
        return key.startsWith("./") ? key.substring(2) : key;
    }
}
