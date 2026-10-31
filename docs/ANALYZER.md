# Static repository analyzer

## Scope

P0.2 starts with a local, read-only inventory of Maven and Gradle build metadata. The implementation lives in `engine/analyzer` and uses Python 3.10+ with no runtime third-party dependencies.

The analyzer does not execute build tools, Gradle scripts, Maven plugins, or project code. This is a deliberate trust boundary: build metadata is parsed as data only.

## Output contract

The CLI emits JSON with:
- `schema_version` for consumer compatibility;
- analyzer identity and version;
- a SHA-256 digest of discovered build-file paths and contents;
- project/module declarations, Java settings, dependency/plugin declarations, and evidence file paths;
- diagnostics for malformed or unsupported project layouts.

The JSON Schema is maintained in [engine/analyzer/schema/inventory.schema.json](../engine/analyzer/schema/inventory.schema.json). Breaking output changes require a schema version change and migration notes.

## Run locally

```bash
python -m engine.analyzer.qivra_analyzer ./repository --pretty
python -m engine.analyzer.qivra_analyzer ./repository --output inventory.json
cd engine/analyzer && python -m unittest discover -s tests -v
```

## Evidence and interpretation

Project entries cite the build files from which declarations were read. Diagnostics cite the malformed file when one exists. Missing Java configuration is represented as an empty Java settings object; it must not be interpreted as a specific runtime version.

The output is a static declaration inventory. It is not an effective Maven model, resolved Gradle model, dependency graph, security scan, Java API compatibility assessment, or migration recommendation.

## Validation

Fixtures cover Maven multi-module projects, nested Maven modules, Gradle Kotlin DSL settings and toolchains, and malformed Maven XML. CI runs unit tests and validates that the fixture corpus produces a schema-versioned inventory.

## Next increments

1. Improve Gradle settings/project-directory mapping and version catalog evidence.
2. Add Maven parent/property interpolation and profile-aware diagnostics without resolving dependencies.
3. Add Java source-level language/API signals with file/line evidence.
4. Expand fixtures from representative public projects after license and provenance review.
