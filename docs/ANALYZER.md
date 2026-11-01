# QIVRA static and hybrid repository analyzer

P0.2 combines three complementary local analysis paths:

- Python scanner: lightweight Maven/Gradle metadata discovery using Python 3.10+ standard library.
- Java hybrid parser: Java 21 module using JavaParser for Java AST inventory and OpenRewrite for Gradle Groovy/Kotlin and Maven POM XML syntax.
- Build-model intelligence: Tomlj parses Gradle version catalogs; a hardened JDK XML parser resolves only safe workspace-relative Maven parents and statically merges visible properties and dependency management.

No path executes customer build scripts or application code.

## Python metadata scanner

~~~bash
python -m engine.analyzer.qivra_analyzer ./repository --pretty
python -m engine.analyzer.qivra_analyzer ./repository --output inventory.json
cd engine/analyzer && python -m unittest discover -s tests -v
~~~

The Python output uses schema version 1.0.0 at engine/analyzer/schema/inventory.schema.json. It discovers Maven/Gradle build files, modules, literal dependency/plugin declarations, and initial Java build settings.

## Java hybrid parser

See [engine/analyzer-java/README.md](../engine/analyzer-java/README.md) for build and run instructions.

The hybrid output uses schema version 1.6.0 at engine/analyzer-java/schema/hybrid-inventory.schema.json. It includes Java AST declarations, Gradle syntax, Gradle version catalogs, and a partial static Maven model.

## Maven model boundaries

The Maven result is a **partial static model**, not Maven's effective model. It resolves only a local \`parent.relativePath\` inside the workspace, with symlink/path containment, cycle, and depth checks. It does not query remote repositories, resolve remote parents/BOMs, activate profiles, calculate full Maven interpolation semantics, mediate dependencies, or resolve transitive dependencies.

POM properties are not dumped as arbitrary values because they may contain credentials or internal configuration. The inventory records property keys and only emits specific resolved coordinate/dependency fields required for evidence.

## Gradle boundaries

The version catalog parser handles literal TOML version strings, library/plugin aliases, bundles, and \`version.ref\` references. It does not evaluate Gradle scripts, determine whether an alias is used, resolve computed catalog expressions, or create an effective Gradle model.

## Evidence and interpretation

Every source/build record names its repository-relative path. Maven parent and dependency-management resolutions include provenance. Parse errors and unresolved relationships are explicit diagnostics.

The hybrid inventory also emits a deterministic dependency graph (`dependency_graph`, graph schema 1.0.0). Nodes represent scanned Java source files, declared Java types/packages, Maven projects, and unresolved dependency/import targets. Edges represent Java type declarations/imports and Maven parent, declared dependency, and literal aggregator-module relationships. Every edge retains a repository-relative evidence file; Java import/declaration edges also retain source line numbers. Maven dependency edges retain declaration metadata such as scope, optionality, and version provenance. Workspace resolution is reported only when a unique scanned coordinate/type match exists; missing or duplicate targets remain marked `not_found_*`, `unresolved_*`, or `ambiguous_*` rather than being silently attached to a guessed node.

A parsed build file does not prove that Maven or Gradle can configure or build the project. A resolved local Maven parent does not imply that external BOMs or transitive dependencies are resolved. A Java AST parse does not prove runtime compatibility or migration safety.

## Safety boundary

- Build files are untrusted input.
- No Maven/Gradle executable, wrapper, plugin, lifecycle goal, build task, customer test, or application code is invoked.
- XML external entities, DTDs, external schemas, and XInclude are disabled for the static Maven model pass.
- Maven parent lookup is local-only and rejects workspace escape and symlink traversal.
- No remote dependency or BOM resolution occurs.
- The Java hybrid scan has file-count and per-file size limits and skips symlinks and common generated/dependency directories.

## Validation

CI runs Python unit tests and fixtures, compiles the Java 21 module, parses Java/Gradle/POM/TOML fixtures, validates version-catalog references and bundles, local Maven parent inheritance, dependency version provenance, BOM declarations, and non-activation of Maven profiles. It also analyzes pinned external configuration/source snapshots from Spring Boot, Gradle, Apache Maven, and Commons Lang; no upstream build is executed.

## Known limitations

- Gradle computed declarations and settings evaluation remain incomplete. Convention plugins are reported as path-derived candidates only; plugin application and effective plugin identity are not inferred.
- Maven remote parents, external BOM imports, profile activation, repositories, effective plugin configuration, and dependency mediation remain incomplete.
- Java method calls and method references are resolved on a best-effort basis through JavaParser Symbol Solver against discovered workspace source roots and the running JDK. Each reference records a qualified method signature, return/parameter types, source path/line, and whether the declaration came from workspace source or the JDK.
- External dependency JARs are not downloaded or added to the type solver. Calls relying on APIs outside workspace sources and JDK remain unresolved; that state is not proof that the API does not exist.
- Symbol resolution is not a full compiler/type-checker and does not establish runtime reachability, semantic compatibility, or Java 17 migration safety.
- Java imports are matched only against types/packages found in the scanned source set; `not_found_in_scanned_sources` does not prove that an import is external.
- Maven workspace dependency matching uses unique groupId/artifactId coordinates from scanned POMs; it does not prove the artifact is selected by Maven mediation or that the project builds.
- Framework/runtime reachability and semantic compatibility are not inferred.
- P0.3 adds target-profile assessment and explicit uncertainty handling.
