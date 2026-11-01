package dev.qivra.analyzer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Dependency-free regression checks executed during Maven's test phase.
 */
public final class DependencyGraphTest {
    private DependencyGraphTest() {
    }

    public static void main(String[] args) {
        javaImportsKeepSourceEvidenceAndResolutionState();
        mavenDependenciesResolveOnlyUniqueWorkspaceCoordinates();
        ambiguousWorkspaceCoordinatesStayAmbiguous();
        graphSerializationIsDeterministic();
        System.out.println("Dependency graph regression checks passed.");
    }

    private static void javaImportsKeepSourceEvidenceAndResolutionState() {
        Map<String, Object> caller = javaSource(
                "src/main/java/org/example/app/Caller.java",
                "org.example.app",
                List.of(Map.of("name", "org.example.api.Service", "static", false, "wildcard", false, "line", 3)),
                List.of(Map.of("name", "Caller", "kind", "class", "line", 5))
        );
        Map<String, Object> service = javaSource(
                "src/main/java/org/example/api/Service.java",
                "org.example.api",
                List.of(),
                List.of(Map.of("name", "Service", "kind", "interface", "line", 2))
        );
        Map<String, Object> externalCaller = javaSource(
                "src/main/java/org/example/app/ExternalCaller.java",
                "org.example.app",
                List.of(Map.of("name", "com.vendor.sdk.Client", "static", false, "wildcard", false, "line", 7)),
                List.of(Map.of("name", "ExternalCaller", "kind", "class", "line", 9))
        );

        Map<String, Object> graph = DependencyGraph.build(List.of(caller, service, externalCaller), List.of());
        Map<String, Object> internalImport = edge(graph, "java_import", "workspace_type_match");
        check(internalImport != null, "workspace Java import should resolve to a scanned type");
        check(number(map(internalImport.get("evidence")).get("line")) == 3,
                "Java import edge must retain its source line");
        check("src/main/java/org/example/app/Caller.java".equals(map(internalImport.get("evidence")).get("file")),
                "Java import edge must retain its source file");

        Map<String, Object> externalImport = edge(graph, "java_import", "not_found_in_scanned_sources");
        check(externalImport != null, "unseen imports must remain explicit rather than being inferred as resolved");
        check(number(map(graph.get("summary")).get("java_import_edges")) == 2,
                "both import declarations should produce graph edges");
    }

    private static void mavenDependenciesResolveOnlyUniqueWorkspaceCoordinates() {
        Map<String, Object> root = mavenProject(
                "pom.xml", "org.example", "application", "1.0.0", List.of("core"),
                List.of(
                        dependency("org.example", "core", "1.0.0"),
                        dependency("com.vendor", "remote-client", "2.4.0")
                )
        );
        Map<String, Object> core = mavenProject(
                "core/pom.xml", "org.example", "core", "1.0.0", List.of(), List.of()
        );

        Map<String, Object> graph = DependencyGraph.build(List.of(), List.of(root, core));
        Map<String, Object> internalDependency = edge(graph, "maven_dependency", "workspace_module_match");
        check(internalDependency != null, "unique matching Maven coordinates should resolve to a workspace module");
        Map<String, Object> internalTarget = node(graph, String.valueOf(internalDependency.get("to")));
        check("core/pom.xml".equals(map(internalTarget.get("data")).get("path")),
                "resolved Maven dependency should point to the child POM path");

        Map<String, Object> aggregate = edge(graph, "maven_aggregates", "workspace_module_match");
        check(aggregate != null, "literal Maven module declarations should map to scanned child POMs");
        Map<String, Object> externalDependency = edge(graph, "maven_dependency", "not_found_in_scanned_workspace");
        check(externalDependency != null, "dependencies absent from the scanned workspace must stay unresolved");
        check("pom.xml".equals(map(externalDependency.get("evidence")).get("file")),
                "Maven dependency edge must retain the declaring POM");
    }

    private static void ambiguousWorkspaceCoordinatesStayAmbiguous() {
        Map<String, Object> root = mavenProject(
                "pom.xml", "org.example", "application", "1.0.0", List.of(),
                List.of(dependency("org.example", "shared", "1.0.0"))
        );
        Map<String, Object> first = mavenProject(
                "one/pom.xml", "org.example", "shared", "1.0.0", List.of(), List.of()
        );
        Map<String, Object> second = mavenProject(
                "two/pom.xml", "org.example", "shared", "1.0.0", List.of(), List.of()
        );
        Map<String, Object> graph = DependencyGraph.build(List.of(), List.of(root, first, second));
        check(edge(graph, "maven_dependency", "ambiguous_workspace_module") != null,
                "duplicate workspace coordinates must not resolve arbitrarily");
    }

    private static void graphSerializationIsDeterministic() {
        Map<String, Object> source = javaSource(
                "A.java", "sample", List.of(),
                List.of(Map.of("name", "A", "kind", "class", "line", 1))
        );
        String first = JsonWriter.write(DependencyGraph.build(List.of(source), List.of()), false);
        String second = JsonWriter.write(DependencyGraph.build(List.of(source), List.of()), false);
        check(first.equals(second), "identical input must produce byte-for-byte stable graph output");
    }

    private static Map<String, Object> javaSource(
            String path,
            String packageName,
            List<Map<String, Object>> imports,
            List<Map<String, Object>> types
    ) {
        return Map.of(
                "path", path,
                "package", packageName,
                "parse_status", "valid",
                "imports", imports,
                "types", types
        );
    }

    private static Map<String, Object> mavenProject(
            String path,
            String group,
            String artifact,
            String version,
            List<String> modules,
            List<Map<String, Object>> dependencies
    ) {
        Map<String, Object> model = Map.of(
                "coordinates", Map.of("group_id", group, "artifact_id", artifact, "version", version),
                "parent", Map.of(),
                "modules", modules,
                "dependencies", dependencies
        );
        return Map.of("path", path, "build_system", "maven", "static_model", model);
    }

    private static Map<String, Object> dependency(String group, String artifact, String version) {
        return Map.of(
                "group_id", group,
                "artifact_id", artifact,
                "version", version,
                "version_source", "declared",
                "scope", "compile",
                "optional", false,
                "type", "jar"
        );
    }

    private static Map<String, Object> edge(Map<String, Object> graph, String kind, String resolution) {
        return maps(graph.get("edges")).stream()
                .filter(item -> kind.equals(item.get("kind")) && resolution.equals(item.get("resolution")))
                .findFirst().orElse(null);
    }

    private static Map<String, Object> node(Map<String, Object> graph, String id) {
        return maps(graph.get("nodes")).stream()
                .filter(item -> id.equals(item.get("id"))).findFirst().orElseThrow();
    }

    private static List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(Map.class::isInstance).map(item -> (Map<String, Object>) item)
                .collect(Collectors.toList());
    }

    private static Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> raw ? (Map<String, Object>) raw : Map.of();
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
