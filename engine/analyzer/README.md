# QIVRA Static Repository Analyzer

The Python scanner inventories Maven and Gradle metadata using the standard library. It complements the Java hybrid parser rather than replacing it.

## Requirements

- Python 3.10+
- No runtime third-party dependencies

## Run

~~~bash
python -m engine.analyzer.qivra_analyzer ./path/to/repository --pretty
python -m engine.analyzer.qivra_analyzer ./path/to/repository --output inventory.json
~~~

The CLI writes schema 1.1.0 JSON at [schema/inventory.schema.json](schema/inventory.schema.json).

## Current coverage

- Maven POM discovery, project modules, local parent-POM inheritance, recursive property interpolation, and compiler settings inherited from local parent POMs.
- Direct dependencies, dependency-management declarations, imported BOM coordinates, plugin and plugin-management declarations.
- Maven profile IDs, activation conditions, profile properties, and profile-scoped dependencies/plugins. Profiles are inventoried but deliberately not activated or evaluated.
- Gradle Groovy and Kotlin build-file discovery, settings include declarations, Java compatibility/toolchain declarations, literal dependency coordinates, and plugin IDs.
- Mixed and nested projects, malformed POM diagnostics, deterministic ordering, evidence paths, and a SHA-256 digest.

## Maven interpretation boundary

The scanner resolves parent metadata only when the parent POM exists at the declared local relativePath (or Maven's default ../pom.xml). It never downloads a missing parent. Missing local parents are reported explicitly; their properties and managed dependencies are not inferred.

Imported BOMs are inventoried as declarations. QIVRA does not download or merge them. Profiles are reported with activation predicates but are not activated; profile-specific declarations are not merged into the base project's dependency inventory. This is not a complete effective Maven model.

## Safety boundary

Discovery is static. Build scripts are treated as untrusted text and are never evaluated. Maven/Gradle, wrappers, plugins, lifecycle tasks, test code, and shell scripts are not executed. The scanner has no network access and reads only recognized build metadata files.

## Known limitations

- Gradle expressions, version catalogs, variables, convention plugins, and computed dependency coordinates are handled by the separate Java hybrid parser only for version-catalog syntax; Gradle expressions remain unresolved.
- Maven property interpolation is limited to local and locally inherited POM properties. It is not full Maven interpolation or profile activation.
- Dependency management, BOMs, parent POMs, and profiles are static declaration evidence, not a resolved dependency graph or effective model.
- Java versions are reported only when recognizable literal properties are present; absence is not proof that a project has no Java target.
- No framework/API usage analysis or Java source parsing is included in this Python module.
