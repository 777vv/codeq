package com.codeq.match;

import com.codeq.model.MethodKey;
import com.codeq.model.MethodMatch;
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
 * AST 精准匹配（宪法 4.1）：JavaParser 把变更行归约到方法，主键「类名 + 方法签名 + 路由」。
 * feature 04 增强：方法指纹（US1，经 {@link MethodFingerprinter}）+ 路由组合（类级 + 方法级，US3）。
 * 行号仅用于归约与可视化，不作匹配主键。
 *
 * @author wangtao
 * @date 2026-08-06
 */
@Component
public class AstMatcher {

    private static final List<String> ROUTE_ANNOTATIONS = List.of(
            "RequestMapping", "GetMapping", "PostMapping",
            "PutMapping", "DeleteMapping", "PatchMapping");

    private final MethodFingerprinter fingerprinter;

    public AstMatcher(MethodFingerprinter fingerprinter) {
        this.fingerprinter = fingerprinter;
    }

    /**
     * 把变更行归约到方法（含指纹）；返回 行号 → {@link MethodMatch}。
     * 未出现在任何方法体内的行不放入返回（由调用方判 YELLOW）。
     */
    public Map<Integer, MethodMatch> mapLinesToMethods(File repo, String relPath,
                                                      Collection<Integer> changedLines) {
        File src = new File(repo, relPath);
        if (!src.isFile() || !relPath.endsWith(".java")) {
            return Map.of();
        }
        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(src);
        } catch (Exception e) {
            return Map.of();
        }
        Map<Integer, MethodMatch> result = new LinkedHashMap<>();
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
            MethodMatch match = new MethodMatch(key, fingerprint(md));
            for (Integer ln : changedLines) {
                if (ln >= begin && ln <= end) {
                    result.put(ln, match);
                }
            }
        }
        return result;
    }

    private String fingerprint(MethodDeclaration md) {
        try {
            return fingerprinter.fingerprint(md);
        } catch (Exception e) {
            return null;
        }
    }

    /** US3 路由组合：类级注解前缀 + 方法级注解路径，斜杠归一。 */
    private String extractRoute(MethodDeclaration md) {
        String methodRoute = null;
        for (String name : ROUTE_ANNOTATIONS) {
            methodRoute = md.getAnnotationByName(name).map(this::annotationValue).orElse(null);
            if (methodRoute != null) {
                break;
            }
        }
        String classPrefix = null;
        for (String name : ROUTE_ANNOTATIONS) {
            classPrefix = md.findAncestor(ClassOrInterfaceDeclaration.class)
                    .flatMap(c -> c.getAnnotationByName(name))
                    .map(this::annotationValue).orElse(null);
            if (classPrefix != null) {
                break;
            }
        }
        return combine(classPrefix, methodRoute);
    }

    /** 斜杠归一组合：{@code /api + /foo → /api/foo}；{@code /api + / → /api}；null 处理。 */
    static String combine(String prefix, String methodRoute) {
        if ((prefix == null || prefix.isBlank()) && (methodRoute == null || methodRoute.isBlank())) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (prefix != null && !prefix.isBlank()) {
            if (!prefix.startsWith("/")) {
                sb.append('/');
            }
            sb.append(stripTrailingSlash(prefix));
        }
        if (methodRoute != null && !methodRoute.isBlank()) {
            if (sb.length() > 0 && !methodRoute.startsWith("/")) {
                sb.append('/');
            }
            sb.append(methodRoute);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
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
