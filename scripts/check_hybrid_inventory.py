import json
import sys
from pathlib import Path


def main():
    data = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    summary = data["summary"]

    assert data["schema_version"] == "1.4.0", "unexpected hybrid inventory schema"
    assert summary["java_files"] >= 3, "Java fixture files were not discovered"
    assert summary["parsed_java_files"] >= 2, "Java fixtures did not parse"
    assert summary["java_type_declarations"] >= 3, "Java type declarations were not extracted"
    assert summary["gradle_files"] >= 1, "Gradle fixture was not discovered"
    assert summary["parsed_gradle_files"] >= 1, "Gradle syntax parser did not accept a fixture"
    assert summary["gradle_version_catalogs"] >= 1, "Gradle version catalog was not discovered"
    assert summary["parsed_gradle_version_catalogs"] >= 1, "Gradle version catalog did not parse"
    assert summary["gradle_catalog_libraries"] >= 2, "Gradle catalog library aliases were not extracted"
    assert summary["maven_files"] >= 2, "Maven parent/child fixtures were not discovered"
    assert summary["parsed_maven_files"] >= 2, "Maven XML parser did not accept parent/child fixtures"
    assert summary["maven_parent_resolved"] >= 1, "workspace-relative Maven parent was not resolved"
    assert summary["maven_dependency_declarations"] >= 1, "Maven dependency declaration was not extracted"
    assert summary["maven_bom_imports"] >= 1, "Maven BOM import was not extracted"

    paths = {item["path"] for item in data["java_sources"]}
    assert any(path.endswith("LegacyService.java") for path in paths), "legacy Java fixture missing"
    assert any(path.endswith("FeatureRecord.java") for path in paths), "record Java fixture missing"

    catalog = next(item for item in data["build_files"] if item["build_system"] == "gradle_version_catalog")
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

    print("Hybrid analyzer output validation passed.")


if __name__ == "__main__":
    main()
