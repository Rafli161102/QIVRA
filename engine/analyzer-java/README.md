# Hybrid source analyzer

The hybrid analyzer combines QIVRA's Python build-metadata inventory with a Java 21 source-analysis module.

## Upstream parser components

| Component | Pinned artifact | Role | License |
|---|---|---|---|
| JavaParser | com.github.javaparser:javaparser-core:3.28.2 | Java AST and source locations | Apache-2.0 option selected |
| JavaParser Symbol Solver | com.github.javaparser:javaparser-symbol-solver-core:3.28.2 | Workspace/JDK method identity resolution | Apache-2.0 option selected |
| OpenRewrite Gradle | org.openrewrite:rewrite-gradle:8.90.4 | Gradle Groovy/Kotlin syntax | Apache-2.0 |
| OpenRewrite XML | org.openrewrite:rewrite-xml:8.90.4 | Maven POM XML syntax | Apache-2.0 |
| Tomlj | org.tomlj:tomlj:1.1.1 | Gradle version-catalog TOML | Apache-2.0 |

No upstream parser source trees are copied into QIVRA. See docs/OSS_INTEGRATION.md, docs/LICENSING.md, and THIRD-PARTY-NOTICES.md for provenance.

## Build and run

Requirements: JDK 21 and Maven 3.9+.

~~~sh
mvn -B -ntp -f engine/analyzer-java/pom.xml verify
mvn -q -f engine/analyzer-java/pom.xml org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  -Dexec.mainClass=dev.qivra.analyzer.HybridAnalyzer \
  -Dexec.args="./path/to/repository --pretty"
~~~

Write JSON to a file:

~~~sh
mvn -q -f engine/analyzer-java/pom.xml org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
  -Dexec.mainClass=dev.qivra.analyzer.HybridAnalyzer \
  -Dexec.args="./path/to/repository --output inventory-source.json"
~~~

## Inventory coverage

- Java package/import/type/method declarations, method-call-name counts, language-feature signals, and source line evidence.
- Best-effort method-call and method-reference identities for workspace source and JDK APIs, with explicit unresolved states and source locations.
- Groovy/Kotlin Gradle build and settings syntax parsing with literal static-model extraction for included projects, includeBuild paths, safe projectDir mappings, plugins, dependencies, and convention-plugin candidates.
- Gradle version catalogs: literal versions, library/plugin aliases, bundles, and version references; aliases are linked to build dependency/plugin declarations when the accessor is statically recognizable.
- Maven POM XML syntax parsing plus partial static model: local relative parent, inherited properties and dependencyManagement, dependency version provenance, BOM import inventory, plugins, modules, and profile activation metadata.
- Deterministic snapshot digest, file-count and per-file-size limits, and evidence-backed diagnostics.
- Provenance-labelled graph nodes and edges for Java type declarations/imports and Maven parent, dependency, and aggregator-module relationships. Unresolved and ambiguous targets stay explicit.

## Safety boundary

The parser reads files as text and builds syntax/model structures. Java symbol resolution uses scanned workspace source roots and the analyzer's JDK only; it does not download dependency JARs or evaluate customer build configuration. It never invokes Maven/Gradle, evaluates Gradle scripts, executes plugins, compiles customer source, or runs tests. Maven parent lookup is limited to safe workspace-relative files. Remote parent, BOM, dependency, and plugin resolution are not performed. Maven XML external entities, DTDs, and external schemas are disabled.

## Known limitations

- Maven output is not an effective POM. Remote parent inheritance, BOM expansion, profile activation, dependency mediation, and transitive resolution are not implemented.
- Gradle output is not an effective Gradle model. Literal `include`, `includeBuild`, `projectDir`, plugin, and dependency declarations are inventoried without evaluating the DSL. Computed expressions and settings evaluation remain unsupported; precompiled convention plugin IDs are path-derived candidates, not verified applied plugin identities.
- Java API resolution is best-effort against workspace sources and the JDK. External library methods remain unresolved unless their source is present in a scanned source root.
- Static inventory is not a migration recommendation or proof of successful build.
