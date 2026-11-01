package dev.qivra.analyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Dependency-free regression checks for literal Gradle settings/build-model extraction.
 */
public final class GradleStaticModelTest {
    private GradleStaticModelTest() {
    }

    public static void main(String[] args) throws Exception {
        extractsLiteralSettingsAndProjectDirectoryMappings();
        extractsDependenciesPluginsAndVersionCatalogAliases();
        extractsGroovyDslWithoutParentheses();
        discoversPrecompiledConventionPluginCandidates();
        System.out.println("Gradle static model regression checks passed.");
    }

    private static void extractsLiteralSettingsAndProjectDirectoryMappings() throws Exception {
        Path root = Files.createTempDirectory("qivra-gradle-settings-");
        try {
            Files.createDirectories(root.resolve("modules/app"));
            String settings = """
                    // include(":comment-only")
                    rootProject.name = "sample-root"
                    include(":app", ":shared")
                    include ":extra", ":other"
                    include(":dynamic:" + providers.gradleProperty("extra").get())
                    project(":app").projectDir = file("modules/app")
                    project(":generated").projectDir = layout.projectDirectory.dir("generated")
                    includeBuild("build-logic")
                    """;
            Map<String, Object> model = GradleStaticModel.parse(
                    root, "settings.gradle.kts", settings, Map.of()
            );

            check("sample-root".equals(model.get("root_project_name")),
                    "literal rootProject.name should be captured");
            List<Map<String, Object>> projects = maps(model.get("included_projects"));
            check(projects.size() == 4, "Kotlin and Groovy literal include paths should become included projects");
            check(projects.stream().anyMatch(item -> ":app".equals(item.get("project_path"))),
                    "literal project path missing");
            check(projects.stream().anyMatch(item -> ":shared".equals(item.get("project_path"))),
                    "second literal project path missing");
            check(projects.stream().anyMatch(item -> ":extra".equals(item.get("project_path")))
                            && projects.stream().anyMatch(item -> ":other".equals(item.get("project_path"))),
                    "multiple Groovy include arguments should be inventoried");

            List<Map<String, Object>> mappings = maps(model.get("project_directory_mappings"));
            Map<String, Object> appMapping = mappings.stream()
                    .filter(item -> ":app".equals(item.get("project_path"))).findFirst().orElseThrow();
            check("modules/app".equals(appMapping.get("workspace_path")),
                    "safe relative projectDir should map to a workspace-relative path");
            check(Boolean.TRUE.equals(appMapping.get("directory_exists")),
                    "existing projectDir should be marked as present");

            Map<String, Object> computed = mappings.stream()
                    .filter(item -> ":generated".equals(item.get("project_path"))).findFirst().orElseThrow();
            check("computed_or_unsupported".equals(computed.get("resolution")),
                    "computed projectDir must remain unresolved");

            List<Map<String, Object>> includedBuilds = maps(model.get("included_builds"));
            check(includedBuilds.size() == 1
                            && "build-logic".equals(includedBuilds.get(0).get("directory")),
                    "literal includeBuild directory was not extracted");
            check(number(map(model.get("counts")).get("unresolved_expressions")) >= 2,
                    "computed include and projectDir expressions should be preserved as unresolved evidence");
        } finally {
            deleteTree(root);
        }
    }

    private static void extractsDependenciesPluginsAndVersionCatalogAliases() {
        Map<String, Object> catalog = new LinkedHashMap<>();
        catalog.put("build_system", "gradle_version_catalog");
        catalog.put("libraries", List.of(Map.of(
                "alias", "junit-jupiter",
                "group", "org.junit.jupiter",
                "artifact", "junit-jupiter",
                "version", "5.11.4",
                "version_resolved", true
        )));
        catalog.put("plugins", List.of(Map.of(
                "alias", "kotlin-jvm",
                "id", "org.jetbrains.kotlin.jvm",
                "version", "2.0.21"
        )));
        catalog.put("bundles", List.of(Map.of(
                "alias", "test-kit",
                "members", List.of("junit-jupiter", "mockito-core")
        )));
        Map<String, Map<String, Object>> aliases =
                GradleStaticModel.catalogAliasIndex(List.of(catalog));

        String script = """
                plugins {
                    id("java")
                    id("org.example.conventions") version "1.2.3"
                    alias(libs.plugins.kotlin.jvm)
                }
                dependencies {
                    implementation("org.example:core:1.4.2")
                    api(project(":shared"))
                    testImplementation(libs.junit.jupiter)
                    testImplementation(libs.bundles.test.kit)
                    runtimeOnly("org.example:dynamic:VERSION_TOKEN")
                }
                """.replace("VERSION_TOKEN", "$" + "{versions.dynamic}");

        Map<String, Object> model = GradleStaticModel.parse(
                Path.of(".").toAbsolutePath().normalize(),
                "build.gradle.kts",
                script,
                aliases
        );

        List<Map<String, Object>> plugins = maps(model.get("plugins"));
        check(plugins.stream().anyMatch(item -> "java".equals(item.get("plugin_id"))
                        && "literal_plugin_id".equals(item.get("resolution"))),
                "literal plugin ID should be extracted");
        check(plugins.stream().anyMatch(item -> "org.example.conventions".equals(item.get("plugin_id"))
                        && "1.2.3".equals(item.get("version"))),
                "literal plugin version should be extracted");
        check(plugins.stream().anyMatch(item -> "org.jetbrains.kotlin.jvm".equals(item.get("plugin_id"))
                        && "version_catalog_alias".equals(item.get("resolution"))),
                "plugin version-catalog alias should resolve");

        List<Map<String, Object>> dependencies = maps(model.get("dependencies"));
        check(dependencies.size() == 5, "all five dependency declarations should be inventoried");
        check(dependencies.stream().anyMatch(item -> "literal_coordinate".equals(item.get("resolution"))
                        && "org.example".equals(item.get("group"))
                        && "core".equals(item.get("artifact"))
                        && "1.4.2".equals(item.get("version"))),
                "literal external dependency coordinates missing");
        check(dependencies.stream().anyMatch(item -> "literal_project_dependency".equals(item.get("resolution"))
                        && ":shared".equals(item.get("project_path"))),
                "project dependency should be extracted");
        check(dependencies.stream().anyMatch(item -> "version_catalog_alias_resolved".equals(item.get("resolution"))
                        && "junit-jupiter".equals(item.get("catalog_alias"))
                        && "5.11.4".equals(item.get("version"))),
                "library version-catalog alias should resolve");
        check(dependencies.stream().anyMatch(item -> "dynamic_string_interpolation".equals(item.get("resolution"))),
                "dynamic dependency version must remain unresolved");
        check(dependencies.stream().anyMatch(item -> "version_catalog_bundle_resolved".equals(item.get("resolution"))
                        && "test-kit".equals(item.get("catalog_alias"))
                        && item.get("bundle_members") instanceof List<?> members
                        && members.size() == 2),
                "version-catalog bundle aliases should be linked to their literal members");
        check(number(map(model.get("counts")).get("unresolved_expressions")) >= 1,
                "dynamic dependency expression should have unresolved evidence");
    }

    private static void extractsGroovyDslWithoutParentheses() {
        Map<String, Object> catalog = new LinkedHashMap<>();
        catalog.put("build_system", "gradle_version_catalog");
        catalog.put("libraries", List.of(Map.of(
                "alias", "junit-jupiter",
                "group", "org.junit.jupiter",
                "artifact", "junit-jupiter",
                "version", "5.11.4",
                "version_resolved", true
        )));
        Map<String, Map<String, Object>> aliases =
                GradleStaticModel.catalogAliasIndex(List.of(catalog));

        String script = """
                plugins {
                    id "java" version "17.0.1"
                    id "com.acme.build-conventions"
                }
                dependencies {
                    implementation "com.acme:core:1.4.0"
                    api project(":shared")
                    testImplementation libs.junit.jupiter
                }
                """;
        Map<String, Object> model = GradleStaticModel.parse(
                Path.of(".").toAbsolutePath().normalize(), "build.gradle", script, aliases
        );

        List<Map<String, Object>> plugins = maps(model.get("plugins"));
        check(plugins.stream().anyMatch(item -> "java".equals(item.get("plugin_id"))
                        && "17.0.1".equals(item.get("version"))),
                "Groovy plugin ID and literal version should be extracted");
        check(plugins.stream().anyMatch(item -> "com.acme.build-conventions".equals(item.get("plugin_id"))),
                "Groovy plugin ID without a version should be extracted");

        List<Map<String, Object>> dependencies = maps(model.get("dependencies"));
        check(dependencies.stream().anyMatch(item -> "com.acme:core:1.4.0".equals(item.get("coordinate"))),
                "Groovy dependency coordinate without parentheses should be extracted");
        check(dependencies.stream().anyMatch(item -> "literal_project_dependency".equals(item.get("resolution"))
                        && ":shared".equals(item.get("project_path"))),
                "Groovy project dependency without parentheses should be extracted");
        check(dependencies.stream().anyMatch(item -> "version_catalog_alias_resolved".equals(item.get("resolution"))
                        && "junit-jupiter".equals(item.get("catalog_alias"))),
                "Groovy version-catalog dependency without parentheses should resolve");
    }

    private static void discoversPrecompiledConventionPluginCandidates() {
        String path = "build-logic-commons/gradle-plugin/src/main/kotlin/gradlebuild.repositories.gradle.kts";
        check(GradleStaticModel.isConventionPluginFile(path),
                "precompiled convention plugin script should be discovered");
        Map<String, Object> model = GradleStaticModel.parse(
                Path.of(".").toAbsolutePath().normalize(),
                path,
                "plugins { id(\"java\") }",
                Map.of()
        );
        List<Map<String, Object>> candidates = maps(model.get("convention_plugin_candidates"));
        check(candidates.size() == 1, "convention plugin candidate metadata missing");
        check("gradlebuild.repositories".equals(candidates.get(0).get("plugin_id_candidate")),
                "precompiled plugin ID candidate should be inferred from its relative path");
        check("inferred_from_path_only".equals(candidates.get(0).get("identity_status")),
                "path-derived plugin names must not be presented as verified plugin IDs");
    }

    private static List<Map<String, Object>> maps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> output = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> raw) {
                output.add(map(raw));
            }
        }
        return output;
    }

    private static Map<String, Object> map(Object value) {
        if (!(value instanceof Map<?, ?> raw)) {
            return Map.of();
        }
        Map<String, Object> output = new LinkedHashMap<>();
        raw.forEach((key, item) -> output.put(String.valueOf(key), item));
        return output;
    }

    private static int number(Object value) {
        return value instanceof Number number ? number.intValue() : -1;
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
