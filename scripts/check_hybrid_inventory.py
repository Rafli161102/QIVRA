import json
import sys
from pathlib import Path


def main():
    data = json.loads(Path(sys.argv[1]).read_text(encoding="utf-8"))
    summary = data["summary"]

    assert data["schema_version"] == "1.2.0", "unexpected hybrid inventory schema"
    assert summary["java_files"] >= 3, "Java fixture files were not discovered"
    assert summary["parsed_java_files"] >= 2, "Java fixtures did not parse"
    assert summary["java_type_declarations"] >= 3, "Java type declarations were not extracted"
    assert summary["gradle_files"] >= 1, "Gradle fixture was not discovered"
    assert summary["parsed_gradle_files"] >= 1, "Gradle syntax parser did not accept a fixture"
    assert summary["maven_files"] >= 1, "Maven POM fixture was not discovered"
    assert summary["parsed_maven_files"] >= 1, "Maven XML parser did not accept a fixture"

    assert summary["version_catalog_files"] >= 1, "Gradle version catalog was not discovered"
    assert summary["version_catalog_aliases"] >= 3, "version catalog aliases were not extracted"
    catalog_paths = {item["path"] for item in data["version_catalogs"]}
    assert any(path.endswith("libs.versions.toml") for path in catalog_paths), "libs.versions.toml fixture missing"
    aliases = {
        alias["alias"]
        for catalog in data["version_catalogs"]
        for alias in catalog["aliases"]
    }
    assert {"spring-core", "slf4j-api"}.issubset(aliases), "expected catalog aliases missing"

    paths = {item["path"] for item in data["java_sources"]}
    assert any(path.endswith("LegacyService.java") for path in paths), "legacy Java fixture missing"
    assert any(path.endswith("FeatureRecord.java") for path in paths), "record Java fixture missing"
    print("Hybrid analyzer output validation passed.")


if __name__ == "__main__":
    main()
