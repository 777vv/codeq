package com.codeq.match;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 方法身份指纹（US1，宪法 III AST 精准匹配）：规范化 AST（局部变量名→占位、忽略行号/注释）
 * → SHA-256。局部变量重命名 / 行内移位不改变结构 → 指纹稳定（FR-001/FR-002/FR-007）。
 *
 * @author wangtao
 * @date 2026-08-07
 */
@Component
public class MethodFingerprinter {

    /** 计算方法的结构指纹（SHA-256）。 */
    public String fingerprint(MethodDeclaration md) {
        MethodDeclaration copy = md.clone();
        // 规范化：局部变量名 / 参数名 / NameExpr 名 → 占位 "_"，使重命名不影响指纹
        copy.findAll(NameExpr.class).forEach(n -> n.setName("_"));
        copy.findAll(VariableDeclarator.class).forEach(v -> v.setName("_"));
        copy.findAll(Parameter.class).forEach(p -> p.setName("_"));
        return sha256(copy.toString());
    }

    private String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
