package dev.qivra.analyzer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

final class DependencyGraph {
    private DependencyGraph() {
    }

    static Map<String, Object> build(
            List<Map<String, Object>> javaSources,
            List<Map<String, Object>> buildFiles
    ) {
        TreeMap<String, Map<String, Object>> nodes = new TreeMap<>();
        TreeMap<String, Map<String, Object>> edges = new TreeMap<>();
        Map<String, String> javaFileIds = new TreeMap<>();
        Map<String, List<String>> javaTypes = new TreeMap<>();
        Map<String, String> packageIds = new TreeMap<>();

        for (Map<String, Object> source : javaSources) {
            String path = string(source.get("path"));
            if (blank(path)) {
                continue;
            }
            String fileId = stableId("java-file", path);
            javaFileIds.put(path, fileId);
            Map<String, Object> fileData = new LinkedHashMap<>();
            fileData.put("parse_status", string(source.get("parse_status")));
            addNode(nodes, fileId, "java_source", path, fileData);

            String packageName = string(source.get("package"));
            if (!blank(packageName)) {
                String packageId = stableId("java-package", packageName);
                packageIds.put(packageName, packageId);
                addNode(nodes, packageId, "java_package", packageName, Map.of("package", packageName));
            }

            List<Map<String, Object>> types = maps(source.get("types"));
            int ordinal = 0;
            for (Map<String, Object> type : types) {
                String name = string(type.get("name"));
                if (blank(name)) {
                    continue;
                }
                String qualifiedName = blank(packageName) ? name : packageName + "." + name;
                int line = integer(type.get("line"), -1);
                String typeId = stableId("java-type", qualifiedName + "|" + path + "|" + line + "|" + ordinal++);
                Map<String, Object> typeData = new LinkedHashMap<>();
                typeData.put("qualified_name", qualifiedName);
                typeData.put("name", name);
                typeData.put("kind", string(type.get("kind")));
                typeData.put("package", packageName);
                typeData.put("source_file", path);
                typeData.put("line", line);
                addNode(nodes, typeId, "java_type", qualifiedName, typeData);
                javaTypes.computeIfAbsent(qualifiedName, ignored -> new ArrayList<>()).add(typeId);

                addEdge(edges, "declares_type", fileId, typeId, "observed",
                        path, line, Map.of("declaration_kind", string(type.get("kind"))), path + "|" + line);
                if (!blank(packageName)) {
                    String packageId = packageIds.get(packageName);
                    addEdge(edges, "contains_type", packageId, typeId, "observed",
                            path, line, Map.of("qualified_name", qualifiedName), path + "|" + line);
                }
            }
        }

        for (Map.Entry<String, List<String>> entry : javaTypes.entrySet()) {
            entry.getValue().sort(String::compareTo);
        }

        for (Map<String, Object> source : javaSources) {
            String path = string(source.get("path"));
            String fileId = javaFileIds.get(path);
            if (fileId == null) {
                continue;
            }
            for (Map<String, Object> imported : maps(source.get("imports"))) {
                String importName = string(imported.get("name"));
                if (blank(importName)) {
                    continue;
                }
                boolean isStatic = bool(imported.get("static"));
                boolean wildcard = bool(imported.get("wildcard"));
                int line = integer(imported.get("line"), -1);
                String targetName = importTarget(importName, isStatic, wildcard);
                List<String> matches = javaTypes.getOrDefault(targetName, List.of());
                String targetId;
                String resolution;

                if (matches.size() == 1) {
                    targetId = matches.get(0);
                    resolution = "workspace_type_match";
                } else if (matches.size() > 1) {
                    targetId = unresolvedImportNode(nodes, targetName, importName, isStatic, wildcard,
                            "ambiguous_workspace_type");
                    resolution = "ambiguous_workspace_type";
                } else if (wildcard && !isStatic && packageIds.containsKey(targetName)) {
                    targetId = packageIds.get(targetName);
                    resolution = "workspace_package_match";
                } else {
                    targetId = unresolvedImportNode(nodes, targetName, importName, isStatic, wildcard,
                            "not_found_in_scanned_sources");
                    resolution = "not_found_in_scanned_sources";
                }

                Map<String, Object> provenance = new LinkedHashMap<>();
                provenance.put("import_name", importName);
                provenance.put("static", isStatic);
                provenance.put("wildcard", wildcard);
                addEdge(edges, "java_import", fileId, targetId, resolution,
                        path, line, provenance, importName + "|" + line);
            }
        }

        Map<String, String> projectIdsByPath = new TreeMap<>();
        Map<String, Map<String, Object>> modelsByPath = new TreeMap<>();
        Map<String, List<String>> projectIdsByCoordinate = new TreeMap<>();

        for (Map<String, Object> buildFile : buildFiles) {
            if (!"maven".equals(string(buildFile.get("build_system")))) {
                continue;
            }
            String path = string(buildFile.get("path"));
            Map<String, Object> model = map(buildFile.get("static_model"));
            if (blank(path) || model.isEmpty()) {
                continue;
            }
            Map<String, Object> coordinates = map(model.get("coordinates"));
            String group = string(coordinates.get("group_id"));
            String artifact = string(coordinates.get("artifact_id"));
            String version = string(coordinates.get("version"));
            String projectId = stableId("maven-project", path);
            projectIdsByPath.put(path, projectId);
            modelsByPath.put(path, model);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("path", path);
            data.put("group_id", group);
            data.put("artifact_id", artifact);
            data.put("version", version);
            addNode(nodes, projectId, "maven_project", coordinateLabel(group, artifact, version), data);
            if (!blank(group) && !blank(artifact) && !unresolvedCoordinate(group, artifact)) {
                projectIdsByCoordinate.computeIfAbsent(group + ":" + artifact, ignored -> new ArrayList<>())
                        .add(projectId);
            }
        }

        for (List<String> projectIds : projectIdsByCoordinate.values()) {
            projectIds.sort(String::compareTo);
        }

        for (Map.Entry<String, Map<String, Object>> entry : modelsByPath.entrySet()) {
            String path = entry.getKey();
            Map<String, Object> model = entry.getValue();
            String projectId = projectIdsByPath.get(path);

            Map<String, Object> parent = map(model.get("parent"));
            String parentGroup = string(parent.get("group_id"));
            String parentArtifact = string(parent.get("artifact_id"));
            if (!blank(parentGroup) && !blank(parentArtifact)) {
                Target target = mavenTarget(nodes, projectIdsByCoordinate, parentGroup, parentArtifact,
                        string(parent.get("version")));
                Map<String, Object> provenance = new LinkedHashMap<>();
                provenance.put("declared_resolution", string(parent.get("resolution")));
                addEdge(edges, "maven_parent", projectId, target.id(), target.resolution(),
                        path, -1, provenance, "parent");
            }

            int dependencyIndex = 0;
            for (Map<String, Object> dependency : maps(model.get("dependencies"))) {
                String group = string(dependency.get("group_id"));
                String artifact = string(dependency.get("artifact_id"));
                String version = string(dependency.get("version"));
                if (blank(group) || blank(artifact)) {
                    continue;
                }
                Target target = mavenTarget(nodes, projectIdsByCoordinate, group, artifact, version);
                Map<String, Object> provenance = new LinkedHashMap<>();
                provenance.put("version_source", string(dependency.get("version_source")));
                provenance.put("scope", string(dependency.get("scope")));
                provenance.put("optional", bool(dependency.get("optional")));
                provenance.put("type", string(dependency.get("type")));
                provenance.put("classifier", string(dependency.get("classifier")));
                provenance.put("dependency_index", dependencyIndex);
                addEdge(edges, "maven_dependency", projectId, target.id(), target.resolution(),
                        path, -1, provenance, "dependency-" + dependencyIndex++);
            }

            int moduleIndex = 0;
            for (Object moduleValue : list(model.get("modules"))) {
                String module = string(moduleValue);
                if (blank(module)) {
                    continue;
                }
                String childPom = modulePomPath(path, module);
                if (childPom == null || childPom.equals(path)) {
                    continue;
                }
                String childId = projectIdsByPath.get(childPom);
                String resolution = childId == null ? "not_found_in_scanned_workspace" : "workspace_module_match";
                if (childId == null) {
                    childId = stableId("maven-module-path", childPom);
                    addNode(nodes, childId, "unresolved_maven_module", childPom,
                            Map.of("path", childPom, "declared_module", module));
                }
                addEdge(edges, "maven_aggregates", projectId, childId, resolution,
                        path, -1, Map.of("declared_module", module), "module-" + moduleIndex++);
            }
        }

        List<Map<String, Object>> nodeList = new ArrayList<>(nodes.values());
        List<Map<String, Object>> edgeList = new ArrayList<>(edges.values());
        long unresolvedEdges = edgeList.stream()
                .filter(edge -> isUnresolved(string(edge.get("resolution"))))
                .count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("node_count", nodeList.size());
        summary.put("edge_count", edgeList.size());
        summary.put("resolved_edges", edgeList.size() - unresolvedEdges);
        summary.put("unresolved_edges", unresolvedEdges);
        summary.put("java_import_edges", countEdges(edgeList, "java_import"));
        summary.put("maven_dependency_edges", countEdges(edgeList, "maven_dependency"));
        summary.put("maven_parent_edges", countEdges(edgeList, "maven_parent"));
        summary.put("maven_aggregate_edges", countEdges(edgeList, "maven_aggregates"));

        Map<String, Object> graph = new LinkedHashMap<>();
        graph.put("schema_version", "1.0.0");
        graph.put("summary", summary);
        graph.put("nodes", nodeList);
        graph.put("edges", edgeList);
        return graph;
    }

    private static Target mavenTarget(
            Map<String, Map<String, Object>> nodes,
            Map<String, List<String>> projectIdsByCoordinate,
            String group,
            String artifact,
            String version
    ) {
        String key = group + ":" + artifact;
        List<String> projects = projectIdsByCoordinate.getOrDefault(key, List.of());
        if (projects.size() == 1) {
            return new Target(projects.get(0), "workspace_module_match");
        }
        if (projects.size() > 1) {
            String id = stableId("maven-ambiguous-module", key + ":" + nullToEmpty(version));
            List<String> paths = projects.stream()
                    .map(projectId -> string(map(nodes.get(projectId).get("data")).get("path")))
                    .sorted().toList();
            addNode(nodes, id, "ambiguous_maven_module", coordinateLabel(group, artifact, version),
                    Map.of("group_id", group, "artifact_id", artifact, "version", nullToEmpty(version),
                            "candidate_paths", paths));
            return new Target(id, "ambiguous_workspace_module");
        }
        if (unresolvedCoordinate(group, artifact)) {
            String id = stableId("maven-unresolved-coordinate", key + ":" + nullToEmpty(version));
            addNode(nodes, id, "unresolved_maven_coordinate", coordinateLabel(group, artifact, version),
                    Map.of("group_id", group, "artifact_id", artifact, "version", nullToEmpty(version)));
            return new Target(id, "unresolved_coordinate");
        }
        String id = stableId("maven-artifact", key + ":" + nullToEmpty(version));
        addNode(nodes, id, "maven_artifact", coordinateLabel(group, artifact, version),
                Map.of("group_id", group, "artifact_id", artifact, "version", nullToEmpty(version)));
        return new Target(id, "not_found_in_scanned_workspace");
    }

    private static String unresolvedImportNode(
            Map<String, Map<String, Object>> nodes,
            String targetName,
            String importName,
            boolean isStatic,
            boolean wildcard,
            String resolution
    ) {
        String id = stableId("java-import-target", targetName + "|" + resolution);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("target_name", targetName);
        data.put("example_import", importName);
        data.put("static", isStatic);
        data.put("wildcard", wildcard);
        data.put("resolution", resolution);
        addNode(nodes, id, "java_import_target", targetName, data);
        return id;
    }

    private static String importTarget(String importName, boolean isStatic, boolean wildcard) {
        String target = importName;
        if (wildcard && target.endsWith(".*")) {
            target = target.substring(0, target.length() - 2);
        } else if (isStatic && !wildcard) {
            int separator = target.lastIndexOf('.');
            if (separator > 0) {
                target = target.substring(0, separator);
            }
        }
        return target;
    }

    private static String modulePomPath(String pomPath, String module) {
        try {
            Path modulePath = Path.of(module.replace('\\', '/'));
            if (modulePath.isAbsolute()) {
                return null;
            }
            Path pom = Path.of(pomPath);
            Path parent = pom.getParent() == null ? Path.of("") : pom.getParent();
            Path resolved = parent.resolve(modulePath).normalize();
            if (resolved.isAbsolute() || resolved.startsWith("..")) {
                return null;
            }
            String normalized = resolved.toString().replace('\\', '/');
            if (normalized.endsWith("/pom.xml")) {
                return normalized;
            }
            return normalized.equals(".") ? "pom.xml" : normalized + "/pom.xml";
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static void addNode(
            Map<String, Map<String, Object>> nodes,
            String id,
            String kind,
            String label,
            Map<String, Object> data
    ) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("id", id);
        node.put("kind", kind);
        node.put("label", label == null ? "" : label);
        node.put("data", data);
        nodes.putIfAbsent(id, node);
    }

    private static void addEdge(
            Map<String, Map<String, Object>> edges,
            String kind,
            String from,
            String to,
            String resolution,
            String evidenceFile,
            int line,
            Map<String, Object> provenance,
            String discriminator
    ) {
        String edgeKey = kind + "|" + from + "|" + to + "|" + evidenceFile + "|" + line + "|" + discriminator;
        String id = stableId("edge", edgeKey);
        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("file", evidenceFile);
        if (line >= 0) {
            evidence.put("line", line);
        }
        Map<String, Object> edge = new LinkedHashMap<>();
        edge.put("id", id);
        edge.put("kind", kind);
        edge.put("from", from);
        edge.put("to", to);
        edge.put("resolution", resolution);
        edge.put("evidence", evidence);
        edge.put("provenance", provenance);
        edges.putIfAbsent(id, edge);
    }

    private static String stableId(String prefix, String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (int index = 0; index < 12; index++) {
                hex.append(String.format("%02x", digest[index]));
            }
            return prefix + ":" + hex;
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is unavailable.", impossible);
        }
    }

    private static long countEdges(List<Map<String, Object>> edges, String kind) {
        return edges.stream().filter(edge -> kind.equals(edge.get("kind"))).count();
    }

    private static boolean isUnresolved(String resolution) {
        return resolution != null && (resolution.startsWith("not_found")
                || resolution.startsWith("ambiguous")
                || resolution.startsWith("unresolved"));
    }

    private static String coordinateLabel(String group, String artifact, String version) {
        String coordinate = nullToEmpty(group) + ":" + nullToEmpty(artifact);
        return blank(version) ? coordinate : coordinate + ":" + version;
    }

    private static boolean unresolvedCoordinate(String group, String artifact) {
        return blank(group) || blank(artifact) || group.contains("$" + "{") || artifact.contains("$" + "{");
    }

    private static List<Map<String, Object>> maps(Object value) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list(value)) {
            Map<String, Object> mapped = map(item);
            if (!mapped.isEmpty()) {
                result.add(mapped);
            }
        }
        return result;
    }

    private static Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> result ? result : List.of();
    }

    private static String string(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static int integer(Object value, int fallback) {
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static boolean bool(Object value) {
        return Boolean.TRUE.equals(value);
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private record Target(String id, String resolution) {
    }
}
