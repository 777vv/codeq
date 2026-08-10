package com.codeq.report;

import com.codeq.model.IncrementalChange;
import com.codeq.model.RefactorFlag;
import com.codeq.model.Verdict;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 报告生成（spec FR-004）：控制台彩色（ANSI）+ HTML + JSON 三格式。
 * @author wangtao
 * @date 2026-08-06
 */
@Component
public class ReportGenerator {

    private final ObjectMapper mapper = new ObjectMapper();

    public void printConsole(List<IncrementalChange> changes, String repo, String baseline, String release) {
        long green = count(changes, Verdict.GREEN);
        long red = count(changes, Verdict.RED);
        long yellow = count(changes, Verdict.YELLOW);
        long partial = count(changes, Verdict.PARTIAL);

        System.out.println("codeq 覆盖判定报告");
        System.out.println("仓库: " + repo + "   基准: " + baseline + " → 待发布: " + release);
        System.out.printf("总计 %d 处增量变更：🟢 绿色 %d | 🔴 红色 %d | 🟡 黄色 %d | ◔ partial %d%n",
                changes.size(), green, red, yellow, partial);
        System.out.println("------------------------------------------------------------");
        for (IncrementalChange c : changes) {
            String where = (c.getMethodKey() == null)
                    ? "(方法外 / 无法归约)"
                    : c.getMethodKey().toString();
            String detail = (c.getVerdict() == Verdict.PARTIAL)
                    ? "  未覆盖行 " + uncovered(c) : "";
            String refactor = c.getRefactorFlag() != RefactorFlag.NONE
                    ? " [" + c.getRefactorFlag().name() + "]" : "";
            String fp = c.getFingerprint() != null
                    ? " fp=" + c.getFingerprint().substring(0, Math.min(8, c.getFingerprint().length())) : "";
            System.out.printf("%s %s  %s%s%s%s%n",
                    c.getVerdict().icon(), c.getFile(), where, detail, refactor, fp);
        }
    }

    public String toJson(List<IncrementalChange> changes, String repo, String baseline, String release) {
        ObjectNode root = mapper.createObjectNode();
        root.put("repo", repo);
        root.put("baseline", baseline);
        root.put("release", release);
        ObjectNode totals = root.putObject("totals");
        totals.put("green", count(changes, Verdict.GREEN));
        totals.put("red", count(changes, Verdict.RED));
        totals.put("yellow", count(changes, Verdict.YELLOW));
        totals.put("partial", count(changes, Verdict.PARTIAL));
        ArrayNode arr = root.putArray("changes");
        for (IncrementalChange c : changes) {
            ObjectNode o = arr.addObject();
            o.put("file", c.getFile());
            if (c.getMethodKey() != null) {
                ObjectNode mk = o.putObject("methodKey");
                mk.put("className", c.getMethodKey().className());
                mk.put("signature", c.getMethodKey().signature());
                mk.put("route", c.getMethodKey().route());
            }
            o.put("verdict", c.getVerdict().name());
            o.put("fingerprint", c.getFingerprint());
            o.put("refactorFlag", c.getRefactorFlag().name());
            if (c.getVerdict() == Verdict.PARTIAL) {
                ArrayNode un = o.putArray("uncoveredLines");
                uncovered(c).forEach(un::add);
            }
        }
        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            return "{}";
        }
    }

    public String toHtml(List<IncrementalChange> changes, String repo, String baseline, String release) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html lang='zh'><head><meta charset='utf-8'>");
        sb.append("<title>codeq 覆盖判定报告</title>");
        sb.append("<style>body{font-family:sans-serif} ")
          .append(".RED{color:#d33}.GREEN{color:#2a8}.YELLOW{color:#c80}.PARTIAL{color:#36c}")
          .append(" li{margin:2px 0}</style></head><body>");
        sb.append("<h2>codeq 覆盖判定报告</h2>");
        sb.append("<p>仓库: ").append(repo).append(" ｜ 基准: ").append(baseline)
          .append(" → 待发布: ").append(release).append("</p><ul>");
        for (IncrementalChange c : changes) {
            sb.append("<li class='").append(c.getVerdict().name()).append("'>")
              .append(c.getVerdict().icon()).append(' ')
              .append(c.getMethodKey() == null ? "(方法外)" : c.getMethodKey().toString())
              .append(c.getRefactorFlag() != RefactorFlag.NONE
                      ? " <b>[" + c.getRefactorFlag().name() + "]</b>" : "")
              .append(" <code>").append(c.getFile()).append("</code></li>");
        }
        sb.append("</ul></body></html>");
        return sb.toString();
    }

    private long count(List<IncrementalChange> changes, Verdict v) {
        return changes.stream().filter(c -> c.getVerdict() == v).count();
    }

    private List<Integer> uncovered(IncrementalChange c) {
        return c.getChangedLines().stream()
                .filter(l -> !c.getExecutedLines().contains(l)).toList();
    }
}
