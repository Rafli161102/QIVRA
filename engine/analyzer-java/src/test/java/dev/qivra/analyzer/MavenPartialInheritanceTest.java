package dev.qivra.analyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;
import java.util.Comparator;

/**
 * Regression checks for partial Maven coordinates inherited from unresolved external parents.
 */
public final class MavenPartialInheritanceTest {
    private MavenPartialInheritanceTest() {
    }

    public static void main(String[] args) throws Exception {
        retainsDeclaredExternalParentCoordinatesForWorkspaceChildren();
        System.out.println("Maven partial parent-inheritance regression checks passed.");
    }

    private static void retainsDeclaredExternalParentCoordinatesForWorkspaceChildren() throws Exception {
        Path root = Files.createTempDirectory("qivra-maven-partial-parent-");
        try {
            String rootPom = """
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.example</groupId>
                        <artifactId>remote-parent</artifactId>
                        <version>7.2.0</version>
                        <relativePath/>
                      </parent>
                      <artifactId>reactor-root</artifactId>
                      <version>1.0.0</version>
                      <packaging>pom</packaging>
                    </project>
                    """;
            String childPom = """
                    <project xmlns="http://maven.apache.org/POM/4.0.0">
                      <modelVersion>4.0.0</modelVersion>
                      <parent>
                        <groupId>org.example</groupId>
                        <artifactId>reactor-root</artifactId>
                        <version>1.0.0</version>
                      </parent>
                      <artifactId>child-module</artifactId>
                    </project>
                    """;
            Path rootFile = root.resolve("pom.xml");
            Path childDir = root.resolve("child");
            Files.createDirectories(childDir);
            Path childFile = childDir.resolve("pom.xml");
            Files.writeString(rootFile, rootPom);
            Files.writeString(childFile, childPom);

            Map<String, Object> rootModel = BuildModelIntelligence.parseMavenPom(
                    root, rootFile, "pom.xml", rootPom, new ArrayList<>()
            );
            Map<String, Object> childModel = BuildModelIntelligence.parseMavenPom(
                    root, childFile, "child/pom.xml", childPom, new ArrayList<>()
            );

            Map<String, Object> rootParent = map(rootModel.get("parent"));
            Map<String, Object> childParent = map(childModel.get("parent"));
            Map<String, Object> childCoordinates = map(childModel.get("coordinates"));

            check("external_unresolved".equals(rootParent.get("resolution")),
                    "the root POM's external parent must remain unresolved");
            check("workspace_resolved".equals(childParent.get("resolution")),
                    "the child POM should match its local parent by inherited declared coordinates");
            check("org.example".equals(rootModel.get("coordinates") instanceof Map<?, ?> coordinates
                            ? coordinates.get("group_id") : null),
                    "root groupId should retain the declared external-parent group as partial identity");
            check("org.example".equals(childCoordinates.get("group_id")),
                    "child groupId should inherit the partial parent coordinate");
            check("1.0.0".equals(childCoordinates.get("version")),
                    "child version should inherit the local parent version");
        } finally {
            deleteTree(root);
        }
    }

    private static Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
        raw.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
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

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
