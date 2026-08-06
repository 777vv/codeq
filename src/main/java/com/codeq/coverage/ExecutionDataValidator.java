package com.codeq.coverage;

import com.codeq.CodeqException;
import com.codeq.ExitCode;
import com.codeq.diff.GitDiffService;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 执行数据来源校验（宪法第四篇 4.2 / spec FR-007）：
 * 业务项目仓库当前 HEAD 必须与待发布分支 HEAD 一致，保证执行数据对应待发布版本；
 * 否则拒绝（退出码 ERROR），避免版本/commit 错配。
 * <p>环境类型（测试 vs 生产）由运维流程保证不在生产/预发挂载探针（宪法红线）。
 * @author wangtao
 * @date 2026-08-06
 */
@Component
public class ExecutionDataValidator {

    private final GitDiffService gitDiff;

    public ExecutionDataValidator(GitDiffService gitDiff) {
        this.gitDiff = gitDiff;
    }

    public void validate(File repo, String release) {
        String repoHead = gitDiff.headCommit(repo, "HEAD");
        String releaseHead = gitDiff.headCommit(repo, release);
        if (!repoHead.equals(releaseHead)) {
            throw new CodeqException(ExitCode.ERROR,
                    "执行数据版本不匹配：业务项目当前 HEAD(" + shortHash(repoHead)
                            + ") ≠ 待发布分支 " + release + "(" + shortHash(releaseHead) + ")。"
                            + "请将业务项目 checkout 到待发布分支，确保测试环境执行数据对应待发布版本。");
        }
    }

    private String shortHash(String h) {
        return (h == null || h.length() < 8) ? String.valueOf(h) : h.substring(0, 8);
    }
}
