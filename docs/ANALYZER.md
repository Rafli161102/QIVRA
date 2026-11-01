# QIVRA static and hybrid repository analyzer

P0.2 has two complementary local analysis paths:

- Python scanner: lightweight metadata discovery using Python 3.10+ standard library.
- Java hybrid parser: Java 21 module using JavaParser for Java AST inventory and OpenRewrite parsers for Gradle Groovy/Kotlin and Maven POM XML.

Neither path executes customer build scripts or application code.

## Python metadata scanner

~~~bash
python -m engine.analyzer.qivra_analyzer ./repository --pretty
python -m engine.analyzer.qivra_analyzer ./repository --output inventory.json
cd engine/analyzer && python -m unittest discover -s tests -v
~~~

The Python output uses schema version 1.0.0 at engine/analyzer/schema/inventory.schema.json. It discovers Maven/Gradle build files, modules, literal dependency/plugin declarations, and initial Java build settings.

## Java hybrid parser

See [engine/analyzer-java/README.md](../engine/analyzer-java/README.md) for build and run instructions.

The hybrid output uses schema version 1.1.0 at engine/analyzer-java/schema/hybrid-inventory.schema.json. It adds:
- Java package and import declarations;
- type declarations (classes, interfaces, enums, annotations, records);
- method declarations and method-call-name counts;
- source line evidence and initial language-feature signals;
- syntax parsing for Gradle Groovy/Kotlin files and Maven POM XML.

## Evidence and interpretation

Every Java source record names its repository-relative path and uses source line numbers when the AST provides them. Build parser results also carry repository-relative evidence paths. Parse errors include the source file path.

An empty Java settings object in the Python inventory is not a specific Java runtime version. A Java AST parse success proves syntax acceptance at the configured parser language level, not successful compilation, API compatibility, dependency resolution, runtime behavior, or migration safety.

## Safety boundary

- Build files are treated as untrusted text.
- No Maven or Gradle executable, wrapper, plugin, lifecycle goal, build task, customer test, or customer application code is invoked.
- The hybrid POM pass uses OpenRewrite XML parsing only; it does not resolve Maven parents, profiles, BOMs, or transitive dependencies.
- The hybrid Gradle pass parses script syntax only; it does not create an effective Gradle model.
- The Java parser builds ASTs but does not resolve symbols or compile source files.
- The Java hybrid scan has file-count and per-file size limits and skips symlinks and common generated/dependency directories.

## Known limitations

- Gradle expressions, version catalogs, variables, convention plugins, and computed dependency coordinates are not resolved.
- Maven parent inheritance, active profiles, BOMs, mediation, and effective POM behavior are not calculated.
- Method-call names are not resolved to concrete API symbols.
- Framework, runtime reachability, test coverage, and semantic compatibility are not inferred.
- Inventory results are not modernization recommendations; P0.3 adds target-profile assessment and explicit uncertainty handling.

## Validation

CI runs Python unit tests and fixture inventory checks, then compiles the Java hybrid module, parses representative Java/Gradle/POM fixtures, and validates the combined JSON summary. P0.2 remains in progress until broader fixture coverage and representative repository validation are complete.
