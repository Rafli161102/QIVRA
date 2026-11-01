# Hybrid source analyzer

The hybrid analyzer combines QIVRA's Python build-metadata inventory with a Java 21 source-analysis module.

## Upstream parser components

| Component | Pinned artifact | Role | License selection |
|---|---|---|---|
| JavaParser | com.github.javaparser:javaparser-core:3.28.2 | Java AST, package/import/type/method declarations, and source line evidence | Apache-2.0 option; upstream also offers LGPL-3.0 |
| OpenRewrite Gradle parser | org.openrewrite:rewrite-gradle:8.96.3 | Static Gradle Groovy and Kotlin syntax parsing | Apache-2.0 |
| OpenRewrite XML parser | org.openrewrite:rewrite-xml:8.96.3 | Static XML parsing for Maven POM files | Apache-2.0 |

The integration uses pinned Maven artifacts and does not copy upstream parser source files into this repository. See docs/OSS_INTEGRATION.md and docs/LICENSING.md for provenance, scope, and notices.

## Build and run

Requirements: JDK 21 and Maven 3.9+.

From the repository root:

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

- Java package and import declarations, type declarations, methods, method-call name counts, and source line locations.
- Initial language feature signals for records, text blocks, lambdas, switch expressions, and module declarations.
- Groovy/Kotlin Gradle build and settings syntax parsing through OpenRewrite.
- Maven POM XML syntax parsing through OpenRewrite XML.
- Deterministic source snapshot digest, file-count and file-size limits, and evidence-backed parse diagnostics.

## Safety boundary

The parser reads files as text and builds syntax trees. It does not invoke the repository's Maven or Gradle executable, evaluate Gradle scripts, resolve project dependencies, execute plugins, compile customer source, or run tests.

The POM pass uses OpenRewrite's XML parser only, not its Maven model resolver. The Gradle pass uses the parser module only, not the Gradle Tooling API.

## Known limitations

- Java method-call names are syntax signals, not resolved API identities.
- Import and declaration inventories do not prove runtime reachability or compatibility.
- Groovy/Kotlin parse success does not mean a Gradle script would configure or build successfully.
- XML parse success does not mean a POM has a valid effective model.
- Gradle version catalogs, Maven parent inheritance, profiles, BOM resolution, and transitive dependency graphs remain separate work.
- Static results must not be presented as migration recommendations until P0.3 assessment rules are implemented.
