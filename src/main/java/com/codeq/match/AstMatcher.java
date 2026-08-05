package com.codeq.match;

import com.codeq.model.MethodKey;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AST 精准匹配（宪法第四篇 4.1）：用 JavaParser 把变更行归约到方法，建立
 * 「类全限定名 + 方法签名 + 路由标识」主键。行号仅用于归约与可视化，不作匹配主键。
 */
@Component
public class AstMatcher {

    private static final List<String> ROUTE_ANNOTATIONS = List.of(
            "RequestMapping", "GetMapping", "PostMapping",
            "PutMapping", "DeleteMapping", "PatchMapping");

    /**
     * 把变更行归约到方法主键；返回 行号 → MethodKey。
     * 未出现在任何方法体内的行不放入返回（由调用方判 YELLOW）。
     */
    public Map<Integer, MethodKey> mapLinesToMethods(File repo, String relPath,
                                                     Collection<Integer> changedLines) {
        File src = new File(repo, relPath);
        if (!src.isFile() || !relPath.endsWith(".java")) {
            return Map.of();
        }
        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(src);
        } catch (Exception e) {
            // 解析失败 → 该文件全部行无法匹配
            return Map.of();
        }
        Map<Integer, MethodKey> result = new LinkedHashMap<>();
        for (MethodDeclaration md : cu.findAll(MethodDeclaration.class)) {
            if (md.getRange().isEmpty()) {
                continue;
            }
            int begin = md.getRange().get().begin.line;
            int end = md.getRange().get().end.line;
            String className = md.findAncestor(ClassOrInterfaceDeclaration.class)
                    .flatMap(ClassOrInterfaceDeclaration::getFullyQualifiedName)
                    .orElse("(unknown)");
            String signature = md.getNameAsString() + "("
                    + String.join(",", md.getParameters().stream()
                    .map(p -> p.getTypeAsString()).toList()) + ")";
            String route = extractRoute(md);
            MethodKey key = new MethodKey(className, signature, route);
            for (Integer ln : changedLines) {
                if (ln >= begin && ln <= end) {
                    result.put(ln, key);
                }
            }
        }
        return result;
    }

    private String extractRoute(MethodDeclaration md) {
        for (String name : ROUTE_ANNOTATIONS) {
            String r = md.getAnnotationByName(name).map(this::annotationValue).orElse(null);
            if (r != null) {
                return r;
            }
        }
        // 方法无路由注解时，回退到类级 @RequestMapping
        return md.findAncestor(ClassOrInterfaceDeclaration.class)
                .flatMap(c -> c.getAnnotationByName(ROUTE_ANNOTATIONS.get(0)))
                .map(this::annotationValue)
                .orElse(null);
    }

    private String annotationValue(AnnotationExpr a) {
        if (a instanceof SingleMemberAnnotationExpr s
                && s.getMemberValue() instanceof StringLiteralExpr lit) {
            return lit.asString();
        }
        if (a instanceof NormalAnnotationExpr n) {
            for (MemberValuePair pair : n.getPairs()) {
                String key = pair.getNameAsString();
                if (("value".equals(key) || "path".equals(key))
                        && pair.getValue() instanceof StringLiteralExpr lit) {
                    return lit.asString();
                }
            }
        }
        return null;
    }
}
