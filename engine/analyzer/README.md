# QIVRA Static Repository Analyzer

The first analyzer slice inventories Maven and Gradle metadata using Python's standard library. It does not resolve dependencies, contact registries, invoke Maven/Gradle, load plugins, or execute project scripts.

## Requirements

- Python 3.10+
- No runtime third-party dependencies

## Run

From the repository root:

```bash
python -m engine.analyzer.qivra_analyzer ./path/to/repository --pretty
python -m engine.analyzer.qivra_analyzer ./path/to/repository --output inventory.json
```

The CLI writes a deterministic JSON inventory. The schema is versioned at [schema/inventory.schema.json](schema/inventory.schema.json).

## Current coverage

- Maven POM discovery, Maven modules, Java compiler properties/plugin configuration, dependency and plugin declarations.
- Gradle Groovy and Kotlin build file discovery, settings include declarations, Java compatibility/toolchain declarations, literal dependency coordinates, and plugin IDs.
- Mixed and nested projects, malformed Maven POM diagnostics, evidence file paths, and a SHA-256 digest of discovered build metadata.
- Deterministic output for an unchanged build-file snapshot and analyzer version.

## Safety boundary

Discovery is static. Build scripts are treated as untrusted text and are never evaluated. The analyzer skips common generated/dependency directories and hidden directories. It reads only recognized build metadata files.

## Known limitations

- Gradle expressions, version catalogs, variables, convention plugins, and computed dependency coordinates are not evaluated.
- Maven parent inheritance, profiles, BOM resolution, dependency mediation, and effective-POM behavior are not calculated.
- The inventory is declaration-level evidence, not a resolved dependency graph or compatibility assessment.
- Java versions are reported only when recognizable literals or Maven properties are present; absence is not proof that a project has no Java target.
- No framework/API usage analysis or Java source parsing is included yet.
