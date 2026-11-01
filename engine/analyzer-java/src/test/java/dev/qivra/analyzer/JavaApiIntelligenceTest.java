package dev.qivra.analyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Dependency-free checks for best-effort Java API identity resolution.
 */
public final class JavaApiIntelligenceTest {
    private JavaApiIntelligenceTest() {
    }

    public static void main(String[] args) throws Exception {
        resolvesWorkspaceAndJdkMethodsWithoutBuildClasspath();
        keepsUnresolvedCallsExplicit();
        System.out.println("Java API identity regression checks passed.");
    }

    private static void resolvesWorkspaceAndJdkMethodsWithoutBuildClasspath() throws Exception {
        Path root = Files.createTempDirectory("qivra-java-api-test-");
        try {
            write(root, "src/main/java/org/example/Api.java", """
                    package org.example;
                    public class Api {
                        public String normalize(String value) {
                            return value.trim();
                        }
                    }
                    """);
            write(root, "src/main/java/org/example/Caller.java", """
                    package org.example;
                    import java.util.List;
                    public class Caller {
                        public String run(List<String> values, Api api) {
                            return api.normalize(values.get(0).trim());
                        }
                        public String reference() {
                            return " hello "::trim;
                        }
                    }
                    """);

            List<Map<String, Object>> sources = new ArrayList<>();
            sources.add(source("src/main/java/org/example/Api.java", "org.example",
                    List.of(Map.of("name", "Api", "kind", "class", "line", 2))));
            Map<String, Object> caller = source("src/main/java/org/example/Caller.java", "org.example",
                    List.of(Map.of("name", "Caller", "kind", "class", "line", 3)));
            sources.add(caller);

            Map<String, Object> summary = JavaApiIntelligence.enrich(root, sources, new ArrayList<>(), 2_000_000L);
            List<Map<String, Object>> references = maps(caller.get("api_references"));

            check(number(summary.get("resolved_workspace_references")) >= 1,
                    "workspace method invocation should resolve to its source declaration");
            check(number(summary.get("resolved_jdk_references")) >= 1,
                    "JDK method invocation should resolve through ReflectionTypeSolver");
            check(references.stream().anyMatch(item ->
                            "org.example.Api".equals(item.get("declaring_type"))
                                    && "normalize".equals(item.get("method_name"))),
                    "workspace resolution must expose declaring type and method name");
            check(references.stream().anyMatch(item ->
                            "java.lang.String".equals(item.get("declaring_type"))
                                    && "trim".equals(item.get("method_name"))),
                    "JDK resolution must expose the resolved String method identity");
            check(references.stream().allMatch(item -> {
                Map<String, Object> evidence = map(item.get("evidence"));
                return "src/main/java/org/example/Caller.java".equals(evidence.get("file"))
                        && number(evidence.get("line")) > 0;
            }), "every API reference must retain source-file and line evidence");
        } finally {
            deleteTree(root);
        }
    }

    private static void keepsUnresolvedCallsExplicit() throws Exception {
        Path root = Files.createTempDirectory("qivra-java-api-unresolved-");
        try {
            write(root, "src/main/java/org/example/UnknownCaller.java", """
                    package org.example;
                    public class UnknownCaller {
                        public void run(UnknownType value) {
                            value.notPresent();
                        }
                    }
                    """);

            Map<String, Object> source = source(
                    "src/main/java/org/example/UnknownCaller.java",
                    "org.example",
                    List.of(Map.of("name", "UnknownCaller", "kind", "class", "line", 2))
            );
            Map<String, Object> summary = JavaApiIntelligence.enrich(
                    root, List.of(source), new ArrayList<>(), 2_000_000L
            );

            check(number(summary.get("unresolved_references")) >= 1,
                    "unresolved calls must remain visible instead of being discarded");
            check(maps(source.get("api_references")).stream()
                            .anyMatch(item -> "unresolved".equals(item.get("resolution"))),
                    "unresolved call record should carry an explicit state");
        } finally {
            deleteTree(root);
        }
    }

    private static Map<String, Object> source(String path, String packageName, List<Map<String, Object>> types) {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("path", path);
        source.put("package", packageName);
        source.put("parse_status", "valid");
        source.put("types", types);
        return source;
    }

    private static void write(Path root, String path, String content) throws IOException {
        Path target = root.resolve(path);
        Files.createDirectories(target.getParent());
        Files.writeString(target, content);
    }

    private static void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> raw) {
                Map<String, Object> mapped = new LinkedHashMap<>();
                raw.forEach((key, entry) -> mapped.put(String.valueOf(key), entry));
                result.add(mapped);
            }
        }
        return result;
    }

    private static Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        raw.forEach((key, entry) -> result.put(String.valueOf(key), entry));
        return result;
    }

    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : -1;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
