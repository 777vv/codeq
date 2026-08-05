package com.codeq.coverage;

import com.codeq.CodeqException;
import com.codeq.ExitCode;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IBundleCoverage;
import org.jacoco.core.data.ExecutionDataStore;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.ISourceFileLocator;
import org.jacoco.report.xml.XMLFormatter;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.Reader;
import java.util.ArrayList;

/**
 * 将 Jacoco ExecutionDataStore（来自远程 dump）配合业务项目编译产物，
 * 生成 Jacoco XML coverage 报告，供 diff-cover 消费（宪法 4.2）。
 */
@Component
public class CoverageReportConverter {

    public void toXml(ExecutionDataStore store, File repo, File outFile) {
        File classesDir = findClassesDir(repo);
        try {
            CoverageBuilder builder = new CoverageBuilder();
            Analyzer analyzer = new Analyzer(store, builder);
            analyzer.analyzeAll(classesDir);
            IBundleCoverage bundle = builder.getBundle("codeq");

            try (FileOutputStream out = new FileOutputStream(outFile)) {
                IReportVisitor visitor = new XMLFormatter().createVisitor(out);
                visitor.visitInfo(new ArrayList<>(), new ArrayList<>());
                visitor.visitBundle(bundle, NO_SOURCE);
                visitor.visitEnd();
            }
        } catch (Exception e) {
            throw new CodeqException(ExitCode.ERROR,
                    "生成 coverage.xml 失败（业务项目是否已编译？）: " + e.getMessage(), e);
        }
    }

    /** 行级覆盖率来自 bytecode 分析；无需源码定位器（diff-cover 用 class/line 覆盖）。 */
    private static final ISourceFileLocator NO_SOURCE = new ISourceFileLocator() {
        @Override
        public Reader getSourceFile(String packageName, String fileName) {
            return null;
        }

        @Override
        public int getTabWidth() {
            return 4;
        }
    };

    /** 在业务项目仓库下查找编译产物目录（Maven target/classes 或 Gradle build/classes/java/main）。 */
    private File findClassesDir(File repo) {
        File[] candidates = {
                new File(repo, "target/classes"),
                new File(repo, "build/classes/java/main")
        };
        for (File c : candidates) {
            if (c.isDirectory()) {
                return c;
            }
        }
        throw new CodeqException(ExitCode.ERROR,
                "未找到业务项目编译产物目录（target/classes 或 build/classes/java/main），请先编译业务项目: " + repo);
    }
}
