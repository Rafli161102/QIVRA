# QIVRA static and hybrid repository analyzer

P0.2 now has two complementary local analysis paths:

- Python scanner: lightweight Maven/Gradle metadata inventory, including local Maven parent inheritance and static profile/BOM declarations.
- Java hybrid parser: Java 21 module using JavaParser for Java AST inventory and OpenRewrite for Gradle Groovy/Kotlin, Maven POM XML syntax, and Gradle version-catalog TOML.

Neither path executes customer build scripts or application code.

## Python metadata scanner

~~~bash
python -m engine.analyzer.qivra_analyzer ./repository --pretty
python -m engine.analyzer.qivra_analyzer ./repository --output inventory.json
cd engine/analyzer && python -m unittest discover -s tests -v
~~~

The Python output uses schema version 1.1.0 at engine/analyzer/schema/inventory.schema.json. It discovers project metadata and direct dependency/plugin declarations. For Maven it additionally reads local parent relationships, local parent properties, dependency-management/BOM declarations, plugin management, and profile activation predicates.

## Java hybrid parser

See engine/analyzer-java/README.md for build and run instructions.

The hybrid output uses schema version 1.2.0 at engine/analyzer-java/schema/hybrid-inventory.schema.json. It adds:
- Java package and import declarations;
- class, interface, enum, annotation, and record declarations;
- method declarations, method-call-name counts, and source line evidence;
- initial language-feature signals;
- syntax parsing for Gradle Groovy/Kotlin scripts and Maven POM XML;
- gradle/libs.versions.toml version, library, and plugin alias declarations.

## Maven interpretation boundary

The Python scanner resolves only parent POMs that exist at the declared local relativePath (or Maven's default ../pom.xml). It does not fetch remote parents. It resolves properties from those local parent files for basic inherited metadata and records inherited dependency management separately.

BOM imports are listed as declarations; BOMs are not downloaded or merged. Maven profiles are recorded with their activation conditions, but are not activated or merged into the base dependency inventory. This is intentionally not a complete effective Maven model.

## Gradle interpretation boundary

Gradle version catalogs are parsed as TOML data. Alias declarations are inventoried with their sections and literal values. The analyzer does not evaluate arbitrary Gradle script expressions, resolve provider accessors, convention plugins, included builds, computed coordinates, or dependency versions from external catalogs.

## Evidence and interpretation

Every Java source record includes a repository-relative path and line numbers when present in its AST. Build-file and version-catalog records include repository-relative evidence paths. Parse errors identify the source file.

An empty Java settings object is not a particular Java runtime version. Parser success proves syntax acceptance, not successful compilation, dependency resolution, build configuration, API compatibility, runtime behavior, or migration safety.

## Safety boundary

- Build files and source files are untrusted input.
- No Maven or Gradle executable, wrapper, plugin, lifecycle goal, build task, customer test, or application code is invoked.
- No remote Maven parent, BOM, Gradle catalog, dependency registry, or plugin repository is accessed.
- Local Maven parent traversal is restricted to the repository root and local files.
- Maven profiles are catalogued, not activated.
- The Java hybrid scan has file-count and per-file size limits and skips symlinks and common generated/dependency directories.

## Known limitations

- Gradle expressions, version-catalog accessor usage, variables, convention plugins, included builds, and computed dependency coordinates are not resolved.
- Maven inheritance is limited to locally present parent POMs. Remote parents and property indirection beyond the available local properties remain unresolved.
- BOM declarations are identified but not expanded. Maven profile activation is not evaluated.
- Dependency mediation, full effective POM behavior, transitive dependency graphs, and plugin resolution are not calculated.
- Java method-call names are syntax signals, not resolved API identities.
- Framework, runtime reachability, test coverage, and semantic compatibility are not inferred.
- Inventory results are not modernization recommendations; P0.3 adds target-profile assessment and explicit uncertainty handling.

## Validation

CI runs Python tests and fixture inventory checks, then compiles the Java hybrid module, parses Java/Gradle/POM/TOML fixtures, and validates the combined JSON summary. P0.2 remains in progress until representative repository coverage, deeper Gradle project mapping, and resolved Java API evidence are verified.
