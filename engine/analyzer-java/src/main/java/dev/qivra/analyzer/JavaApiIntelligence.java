package dev.qivra.analyzer;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Stream;

import static com.github.javaparser.ParseStart.COMPILATION_UNIT;
import static com.github.javaparser.Providers.provider;

/**
 * Best-effort Java symbol resolution against the scanned workspace and the JDK.
 * No build classpath is downloaded and no customer code is executed.
 */
final class JavaApiIntelligence {
    private static final int MAX_SOURCE_ROOTS = 256;
    private static final int MAX_SOURCE_FILES_PER_ROOT = 20_000;

    private JavaApiIntelligence() {
    }

    static Map<String, Object> enrich(
            Path workspaceRoot,
            List<Map<String, Object>> javaSources,
            List<Map<String, Object>> diagnostics,
            long maxFileBytes
    ) {
        Path root = workspaceRoot.toAbsolutePath().normalize();
        Set<Path> sourceRoots = collectSourceRoots(root, javaSources);
        List<String> setupErrors = new ArrayList<>();
        CombinedTypeSolver typeSolver = new CombinedTypeSolver();
        typeSolver.add(new ReflectionTypeSolver());

        int rootsAdded = 0;
        for (Path sourceRoot : sourceRoots.stream().sorted().limit(MAX_SOURCE_ROOTS).toList()) {
            if (!isSafeDirectory(root, sourceRoot, maxFileBytes)) {
                continue;
            }
            try {
                typeSolver.add(new JavaParserTypeSolver(
                        sourceRoot,
                        new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE)
                ));
                rootsAdded++;
            } catch (RuntimeException error) {
                setupErrors.add(error.getClass().getSimpleName());
            }
        }

        if (sourceRoots.size() > MAX_SOURCE_ROOTS) {
            diagnostics.add(diagnostic(
                    "JAVA_SOURCE_ROOT_LIMIT",
                    "Java symbol resolution was limited to the first " + MAX_SOURCE_ROOTS + " source roots.",
                    null
            ));
        }
        if (!setupErrors.isEmpty()) {
            diagnostics.add(diagnostic(
                    "JAVA_SYMBOL_SOLVER_SOURCE_ROOT_ERROR",
                    "One or more workspace source roots could not be registered: "
                            + String.join(", ", setupErrors.stream().distinct().sorted().toList()),
                    null
            ));
        }

        JavaParser parser = new JavaParser(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.BLEEDING_EDGE)
                .setSymbolResolver(new JavaSymbolSolver(typeSolver)));

        Set<String> workspaceTypes = workspaceTypeNames(javaSources);
        long resolved = 0;
        long unresolved = 0;
        long resolvedJdk = 0;
        long resolvedWorkspace = 0;
        long resolvedOther = 0;

        for (Map<String, Object> javaSource : javaSources) {
            String relativePath = string(javaSource.get("path"));
            List<Map<String, Object>> references = new ArrayList<>();
            javaSource.put("api_references", references);
            if (!"valid".equals(javaSource.get("parse_status")) || blank(relativePath)) {
                javaSource.put("api_resolution_status", "skipped_invalid_source");
                continue;
            }

            Path file = root.resolve(relativePath).normalize();
            if (!file.startsWith(root) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                    || Files.isSymbolicLink(file)) {
                javaSource.put("api_resolution_status", "skipped_unsafe_or_missing_file");
                continue;
            }

            try {
                long size = Files.size(file);
                if (size > maxFileBytes) {
                    javaSource.put("api_resolution_status", "skipped_file_too_large");
                    continue;
                }
                String source = Files.readString(file, StandardCharsets.UTF_8);
                ParseResult<CompilationUnit> parsed = parser.parse(COMPILATION_UNIT, provider(source));
                CompilationUnit unit = parsed.getResult().orElse(null);
                if (unit == null || !parsed.isSuccessful()) {
                    javaSource.put("api_resolution_status", "skipped_symbol_parse_error");
                    continue;
                }

                for (MethodCallExpr call : unit.findAll(MethodCallExpr.class)) {
                    Map<String, Object> reference = resolve(
                            "method_call", call.toString(), relativePath, line(call),
                            call::resolve, workspaceTypes
                    );
                    references.add(reference);
                }
                for (MethodReferenceExpr referenceExpr : unit.findAll(MethodReferenceExpr.class)) {
                    Map<String, Object> reference = resolve(
                            "method_reference", referenceExpr.toString(), relativePath, line(referenceExpr),
                            referenceExpr::resolve, workspaceTypes
                    );
                    references.add(reference);
                }

                references.sort(Comparator
                        .comparingInt((Map<String, Object> item) -> integer(item.get("line"), -1))
                        .thenComparing(item -> string(item.get("expression")))
                        .thenComparing(item -> string(item.get("kind"))));

                long sourceResolved = references.stream()
                        .filter(item -> "resolved".equals(item.get("resolution"))).count();
                long sourceUnresolved = references.size() - sourceResolved;
                resolved += sourceResolved;
                unresolved += sourceUnresolved;
                resolvedJdk += references.stream()
                        .filter(item -> "jdk".equals(item.get("resolution_source"))).count();
                resolvedWorkspace += references.stream()
                        .filter(item -> "workspace_source".equals(item.get("resolution_source"))).count();
                resolvedOther += references.stream()
                        .filter(item -> "type_solver".equals(item.get("resolution_source"))).count();

                javaSource.put("api_resolution_status", "complete");
            } catch (IOException error) {
                javaSource.put("api_resolution_status", "source_read_error");
                diagnostics.add(diagnostic(
                        "JAVA_API_SOURCE_READ_ERROR",
                        error.getClass().getSimpleName(),
                        relativePath
                ));
            }
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("enabled", true);
        summary.put("workspace_source_roots", rootsAdded);
        summary.put("resolved_references", resolved);
        summary.put("unresolved_references", unresolved);
        summary.put("resolved_jdk_references", resolvedJdk);
        summary.put("resolved_workspace_references", resolvedWorkspace);
        summary.put("resolved_other_type_solver_references", resolvedOther);
        return summary;
    }

    private static Map<String, Object> resolve(
            String kind,
            String expression,
            String path,
            int line,
            Resolver resolver,
            Set<String> workspaceTypes
    ) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", stableId(path + "|" + line + "|" + kind + "|" + expression));
        item.put("kind", kind);
        item.put("expression", expression);
        item.put("evidence", Map.of("file", path, "line", line));

        try {
            ResolvedMethodDeclaration method = resolver.resolve();
            String declaringType = method.declaringType().getQualifiedName();
            String signature = method.getQualifiedSignature();
            String resolutionSource = resolutionSource(declaringType, workspaceTypes);
            item.put("resolution", "resolved");
            item.put("resolution_source", resolutionSource);
            item.put("declaring_type", declaringType);
            item.put("method_name", method.getName());
            item.put("qualified_signature", signature);
            item.put("target_identity", signature);
            item.put("return_type", method.getReturnType().describe());
            List<String> parameterTypes = new ArrayList<>();
            for (int index = 0; index < method.getNumberOfParams(); index++) {
                parameterTypes.add(method.getParam(index).getType().describe());
            }
            item.put("parameter_types", parameterTypes);
        } catch (RuntimeException error) {
            item.put("resolution", "unresolved");
            item.put("resolution_source", null);
            item.put("unresolved_reason", error.getClass().getSimpleName());
        }
        return item;
    }

    private static Set<Path> collectSourceRoots(Path root, List<Map<String, Object>> javaSources) {
        Set<Path> roots = new TreeSet<>();
        for (Map<String, Object> javaSource : javaSources) {
            String relativePath = string(javaSource.get("path"));
            String packageName = string(javaSource.get("package"));
            if (blank(relativePath) || blank(packageName)) {
                continue;
            }

            Path file = root.resolve(relativePath).normalize();
            if (!file.startsWith(root) || file.getParent() == null) {
                continue;
            }
            Path candidate = file.getParent();
            String[] packageSegments = packageName.split("\\.");
            boolean matches = true;
            for (int index = packageSegments.length - 1; index >= 0; index--) {
                if (candidate.getFileName() == null
                        || !candidate.getFileName().toString().equals(packageSegments[index])) {
                    matches = false;
                    break;
                }
                candidate = candidate.getParent();
                if (candidate == null) {
                    matches = false;
                    break;
                }
            }
            if (matches && candidate.startsWith(root)) {
                roots.add(candidate);
            }
        }
        return roots;
    }

    private static boolean isSafeDirectory(Path root, Path directory, long maxFileBytes) {
        if (Files.isSymbolicLink(root) || !directory.startsWith(root)
                || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            return false;
        }
        Path current = root;
        for (Path component : root.relativize(directory)) {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) {
                return false;
            }
        }

        int javaFiles = 0;
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.toList()) {
                if (Files.isSymbolicLink(path)) {
                    return false;
                }
                if (Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                        && path.getFileName().toString().endsWith(".java")) {
                    if (++javaFiles > MAX_SOURCE_FILES_PER_ROOT || Files.size(path) > maxFileBytes) {
                        return false;
                    }
                }
            }
        } catch (IOException error) {
            return false;
        }
        return true;
    }

    private static Set<String> workspaceTypeNames(List<Map<String, Object>> javaSources) {
        Set<String> result = new LinkedHashSet<>();
        for (Map<String, Object> source : javaSources) {
            String packageName = string(source.get("package"));
            Object rawTypes = source.get("types");
            if (!(rawTypes instanceof List<?> types)) {
                continue;
            }
            for (Object rawType : types) {
                if (!(rawType instanceof Map<?, ?> type)) {
                    continue;
                }
                String name = string(type.get("name"));
                if (!blank(name)) {
                    result.add(blank(packageName) ? name : packageName + "." + name);
                }
            }
        }
        return Set.copyOf(result);
    }

    private static String resolutionSource(String declaringType, Set<String> workspaceTypes) {
        if (workspaceTypes.contains(declaringType)) {
            return "workspace_source";
        }
        if (declaringType.startsWith("java.")
                || declaringType.startsWith("javax.")
                || declaringType.startsWith("jdk.")
                || declaringType.startsWith("org.w3c.")
                || declaringType.startsWith("org.xml.sax.")) {
            return "jdk";
        }
        return "type_solver";
    }

    private static Map<String, Object> diagnostic(String code, String message, String path) {
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("code", code);
        output.put("message", message);
        output.put("evidence", path == null ? Map.of() : Map.of("file", path));
        return output;
    }

    private static String stableId(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder("java-api:");
            for (int index = 0; index < 12; index++) {
                result.append(String.format("%02x", digest[index]));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable.", impossible);
        }
    }

    private static int line(com.github.javaparser.ast.Node node) {
        return node.getBegin().map(position -> position.line).orElse(-1);
    }

    private static int integer(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    @FunctionalInterface
    private interface Resolver {
        ResolvedMethodDeclaration resolve();
    }
}
