import json
import sys
from pathlib import Path


def main():
    data = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    summary = data["summary"]

    assert data["schema_version"] == "1.6.0", "unexpected hybrid inventory schema"
    assert summary["java_files"] >= 3, "Java fixture files were not discovered"
    assert summary["parsed_java_files"] >= 2, "Java fixtures did not parse"
    assert summary["java_type_declarations"] >= 3, "Java type declarations were not extracted"
    assert summary["gradle_files"] >= 1, "Gradle fixture was not discovered"
    assert summary["parsed_gradle_files"] >= 1, "Gradle syntax parser did not accept a fixture"
    assert summary["gradle_version_catalogs"] >= 1, "Gradle version catalog was not discovered"
    assert summary["parsed_gradle_version_catalogs"] >= 1, "Gradle version catalog did not parse"
    assert summary["gradle_catalog_libraries"] >= 2, "Gradle catalog library aliases were not extracted"
    assert summary["gradle_projects_declared"] >= 2, "literal Gradle project includes were not extracted"
    assert summary["gradle_dependencies"] >= 1, "literal Gradle dependencies were not extracted"
    assert summary["gradle_unresolved_declarations"] >= 0, "Gradle unresolved expression count missing"
    gradle_settings = next(item for item in data["build_files"]
                           if item.get("build_system") == "gradle"
                           and item["path"].endswith("gradle-kotlin/settings.gradle.kts"))
    assert gradle_settings["static_model"]["root_project_name"] == "gradle-kotlin", "Gradle root project name missing"
    assert len(gradle_settings["static_model"]["included_projects"]) >= 2, "Gradle included project paths missing"
    gradle_build = next(item for item in data["build_files"]
                        if item.get("build_system") == "gradle"
                        and item["path"].endswith("gradle-kotlin/build.gradle.kts"))
    gradle_deps = gradle_build["static_model"]["dependencies"]
    assert any(item.get("coordinate") == "org.slf4j:slf4j-api:2.0.9" for item in gradle_deps), "Gradle literal dependency coordinate missing"
    assert any(item.get("resolution") == "version_catalog_alias_resolved" and item.get("version") == "2.0.21" for item in gradle_deps), "Gradle library catalog alias was not linked"
    assert any(item.get("resolution") == "literal_project_dependency" and item.get("project_path") == ":shared" for item in gradle_deps), "Gradle project dependency missing"
    assert summary["gradle_project_dir_mappings"] >= 1, "Gradle projectDir mapping was not extracted"
    assert summary["gradle_convention_plugin_candidates"] >= 1, "Gradle convention plugin candidate was not discovered"
    convention = next(item for item in data["build_files"] if item["path"].endswith("com/acme/java-conventions.gradle.kts"))
    candidate = convention["static_model"]["convention_plugin_candidates"][0]
    assert candidate["identity_status"] == "inferred_from_path_only", "convention plugin identity must remain explicitly inferred"

    assert summary["maven_files"] >= 2, "Maven parent/child fixtures were not discovered"
    assert summary["parsed_maven_files"] >= 2, "Maven XML parser did not accept parent/child fixtures"
    assert summary["maven_parent_resolved"] >= 1, "workspace-relative Maven parent was not resolved"
    assert summary["maven_dependency_declarations"] >= 1, "Maven dependency declaration was not extracted"
    assert summary["maven_bom_imports"] >= 1, "Maven BOM import was not extracted"

    paths = {item["path"] for item in data["java_sources"]}
    assert any(path.endswith("LegacyService.java") for path in paths), "legacy Java fixture missing"
    assert any(path.endswith("FeatureRecord.java") for path in paths), "record Java fixture missing"

    catalog = next(item for item in data["build_files"]
                   if item["build_system"] == "gradle_version_catalog"
                   and item["path"].endswith("gradle-kotlin/gradle/libs.versions.toml"))
    assert any(item["alias"] == "junit-jupiter" and item["version"] == "5.11.4" for item in catalog["libraries"]), \
        "Gradle version reference was not resolved"

    child = next(item for item in data["build_files"] if item["path"].endswith("maven-parent/service/pom.xml"))
    model = child["static_model"]
    assert model["parent"]["resolution"] == "workspace_resolved", "Maven local parent was not resolved"
    assert model["coordinates"]["group_id"] == "org.qivra.fixture", "Maven groupId was not inherited"
    assert model["coordinates"]["version"] == "1.4.0", "Maven version was not inherited"
    assert model["dependencies"][0]["version"] == "5.11.4", "Maven dependencyManagement version was not inherited"
    assert model["dependencies"][0]["version_source"] == "workspace_dependency_management", \
        "Maven inherited dependency version provenance missing"
    assert model["profiles"][0]["activated"] is False, "Maven profile must not be activated during static analysis"

    assert summary["java_api_references"] >= 1, "Java API references were not extracted"
    assert summary["java_api_resolved_references"] >= 1, "Java API references were not resolved"
    assert summary["java_api_workspace_source_roots"] >= 1, "Java workspace source roots were not configured"
    api_records = [item for source in data["java_sources"] for item in source.get("api_references", [])]
    assert api_records, "Java API evidence records missing"
    assert any(item["resolution"] == "resolved" and item.get("qualified_signature") for item in api_records), \
        "resolved Java API identity missing"
    assert all(item["evidence"]["file"] and item["evidence"]["line"] > 0 for item in api_records), \
        "Java API references must retain file and line evidence"

    graph = data["dependency_graph"]
    graph_summary = graph["summary"]
    assert graph["schema_version"] == "1.0.0", "unexpected dependency graph schema"
    assert graph_summary["node_count"] == len(graph["nodes"]), "dependency graph node count mismatch"
    assert graph_summary["edge_count"] == len(graph["edges"]), "dependency graph edge count mismatch"
    assert graph_summary["java_import_edges"] >= 1, "Java import graph edges were not generated"
    assert graph_summary["maven_dependency_edges"] >= 1, "Maven dependency graph edges were not generated"
    node_ids = {node["id"] for node in graph["nodes"]}
    edge_ids = [edge["id"] for edge in graph["edges"]]
    assert len(node_ids) == len(graph["nodes"]), "dependency graph node IDs must be unique"
    assert len(set(edge_ids)) == len(edge_ids), "dependency graph edge IDs must be unique"
    for edge in graph["edges"]:
        assert edge["from"] in node_ids and edge["to"] in node_ids, "dependency graph edge references a missing node"
        assert edge["evidence"]["file"], "dependency graph edge is missing source-file evidence"
    assert summary["dependency_graph_nodes"] == graph_summary["node_count"], "summary graph node count mismatch"
    assert summary["dependency_graph_edges"] == graph_summary["edge_count"], "summary graph edge count mismatch"

    # Pinned external repository snapshots. These are selected inputs, not full upstream clones.
    builds = {item["path"]: item for item in data["build_files"]}
    sources = {item["path"]: item for item in data["java_sources"]}

    spring_settings = next(item for path, item in builds.items()
                           if path.endswith("external-validation/spring-boot/settings.gradle"))
    assert spring_settings["parsed"] is True, "Spring Boot Groovy settings did not parse"
    assert spring_settings["static_model"]["root_project_name"] == "spring-boot-build", \
        "Spring Boot root project name was not extracted"
    assert spring_settings["static_model"]["counts"]["included_builds"] >= 1, \
        "Spring Boot included build was not extracted"
    assert len(spring_settings["static_model"]["included_projects"]) >= 2, \
        "Spring Boot literal project includes were not extracted"

    spring_build = next(item for path, item in builds.items()
                        if path.endswith("external-validation/spring-boot/build.gradle"))
    assert spring_build["parsed"] is True, "Spring Boot Groovy build script did not parse"
    assert any(plugin.get("plugin_id") == "base" for plugin in spring_build["static_model"]["plugins"]), \
        "Groovy no-parentheses plugin ID from Spring Boot was not extracted"

    gradle_root_settings = next(item for path, item in builds.items()
                                if path.endswith("external-validation/gradle/gradle-root/settings.gradle.kts"))
    assert gradle_root_settings["parsed"] is True, "Gradle repository Kotlin settings did not parse"
    assert gradle_root_settings["static_model"]["counts"]["included_builds"] >= 1, \
        "Gradle repository included build was not extracted"

    gradle_root_build = next(item for path, item in builds.items()
                             if path.endswith("external-validation/gradle/gradle-root/build.gradle.kts"))
    assert gradle_root_build["parsed"] is True, "Gradle repository root build script did not parse"
    assert any(plugin.get("plugin_id") == "gradlebuild.root-build"
               for plugin in gradle_root_build["static_model"]["plugins"]), \
        "Gradle repository root plugin declaration was not extracted"

    convention_script = next(item for path, item in builds.items()
                             if path.endswith("gradlebuild.repositories.gradle.kts"))
    candidates = convention_script["static_model"]["convention_plugin_candidates"]
    assert candidates and candidates[0]["plugin_id_candidate"] == "gradlebuild.repositories", \
        "build-logic-commons convention plugin candidate was not recognized"

    catalog = next(item for path, item in builds.items()
                   if path.endswith("gradle/catalog-sample/common/gradle/libs.versions.toml"))
    assert any(item.get("alias") == "groovy-core" for item in catalog["libraries"]), \
        "Gradle upstream version-catalog library was not parsed"
    assert any(item.get("alias") == "versions" and item.get("id") == "com.github.ben-manes.versions"
               for item in catalog["plugins"]), "Gradle upstream version-catalog plugin was not parsed"

    kotlin_sample = next(item for path, item in builds.items()
                         if path.endswith("gradle/catalog-sample/kotlin/build.gradle.kts"))
    assert kotlin_sample["parsed"] is True, "Gradle upstream Kotlin catalog sample did not parse"
    assert any(plugin.get("resolution") == "version_catalog_alias"
               and plugin.get("plugin_id") == "com.github.ben-manes.versions"
               for plugin in kotlin_sample["static_model"]["plugins"]), \
        "Gradle Kotlin plugin alias did not link to upstream catalog"
    assert any(dependency.get("resolution") == "version_catalog_bundle_resolved"
               and dependency.get("catalog_alias") == "groovy"
               and len(dependency.get("bundle_members", [])) == 3
               for dependency in kotlin_sample["static_model"]["dependencies"]), \
        "Gradle Kotlin bundle alias did not link to its three upstream members"

    groovy_sample = next(item for path, item in builds.items()
                         if path.endswith("gradle/catalog-sample/groovy/build.gradle"))
    assert groovy_sample["parsed"] is True, "Gradle upstream Groovy catalog sample did not parse"
    assert any(plugin.get("plugin_id") == "java-library"
               for plugin in groovy_sample["static_model"]["plugins"]), \
        "Groovy plugin shorthand did not resolve"
    assert any(dependency.get("resolution") == "version_catalog_bundle_resolved"
               for dependency in groovy_sample["static_model"]["dependencies"]), \
        "Groovy unparenthesized catalog bundle declaration was not extracted"

    maven_root = next(item for path, item in builds.items() if path.endswith("external-validation/apache-maven/pom.xml"))["static_model"]
    assert maven_root["coordinates"]["artifact_id"] == "maven", \
        "Apache Maven root coordinates were not extracted"
    assert maven_root["parent"]["resolution"] == "external_unresolved", \
        "Apache Maven's deliberately external root parent should remain unresolved"
    maven_api = next(item for path, item in builds.items() if path.endswith("external-validation/apache-maven/api/pom.xml"))["static_model"]
    assert maven_api["parent"]["resolution"] == "workspace_resolved", \
        "Apache Maven API POM should resolve its local root parent"
    maven_api_core = next(item for path, item in builds.items() if path.endswith("external-validation/apache-maven/api/maven-api-core/pom.xml"))["static_model"]
    assert maven_api_core["parent"]["resolution"] == "workspace_resolved", \
        "Apache Maven API core POM should resolve its local API parent"
    maven_impl_core = next(item for path, item in builds.items() if path.endswith("external-validation/apache-maven/impl/maven-core/pom.xml"))["static_model"]
    assert maven_impl_core["parent"]["resolution"] == "workspace_resolved", \
        "Apache Maven core POM should resolve its local implementation parent"

    commons = next(item for path, item in builds.items() if path.endswith("external-validation/commons-lang/pom.xml"))["static_model"]
    assert commons["coordinates"]["artifact_id"] == "commons-lang3", \
        "Commons Lang POM coordinates were not extracted"
    assert commons["parent"]["resolution"] == "external_unresolved", \
        "Commons Lang's remote parent should remain unresolved without remote lookup"

    external_java_paths = [
        path for path in sources
        if path.endswith("spring-boot/SpringApplication.java")
        or path.endswith("apache-maven/MavenProject.java")
        or path.endswith("commons-lang/StringUtils.java")
    ]
    assert len(external_java_paths) == 3, "selected external Java source files were not all discovered"
    assert all(sources[path]["parse_status"] == "valid" for path in external_java_paths), \
        "one or more external Java source snapshots failed AST parsing"
    external_api_records = [
        item for path in external_java_paths for item in sources[path].get("api_references", [])
    ]
    assert external_api_records, "external Java source API references were not inventoried"
    assert all(item["evidence"]["file"] and item["evidence"]["line"] > 0
               for item in external_api_records), "external Java API references lost source evidence"

    print("Hybrid analyzer output validation passed.")


if __name__ == "__main__":
    main()
