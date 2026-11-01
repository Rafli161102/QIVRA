# Open-source parser and engineering-skill integration

## Integration objective

QIVRA combines complementary open-source parsers behind small analyzer boundaries and selected agent skills behind project-local instructions. Pinned Maven artifacts are used instead of copying complete upstream parser repositories or maintaining private forks.

## Selected parser projects

| Upstream | Release | Selected artifacts | Analyzer role | License |
|---|---|---|---|---|
| JavaParser | javaparser-parent-3.28.2 | com.github.javaparser:javaparser-core:3.28.2 | Java AST inventory and source line locations | Apache-2.0 option |
| OpenRewrite | v8.90.4 | org.openrewrite:rewrite-gradle:8.90.4 | Gradle Groovy/Kotlin script syntax | Apache-2.0 |
| OpenRewrite | v8.90.4 | org.openrewrite:rewrite-xml:8.90.4 | Maven POM XML syntax | Apache-2.0 |
| OpenRewrite | v8.90.4 | org.openrewrite:rewrite-toml:8.90.4 | Gradle version-catalog TOML syntax | Apache-2.0 |

Exact coordinates are pinned in engine/analyzer-java/pom.xml. OpenRewrite v8.90.4 source is used because these artifacts are available from Maven Central. Newer source releases may require authenticated artifact repositories; public CI deliberately does not add credentials for those repositories.

## Analysis flow

1. Python discovers Maven and Gradle files and extracts static declarations.
2. Python resolves Maven parent metadata only when the declared/default relativePath points to a POM within the scanned repository. It inherits local properties and identifies dependency-management/BOM declarations, profiles, and plugin management without evaluating profile activation or downloading anything.
3. JavaParser reads .java files and emits package, import, type, method, method-call-name, language-feature, and source-line signals.
4. OpenRewrite parses Gradle Groovy/Kotlin scripts, Maven POM XML, and Gradle version-catalog TOML.
5. The local Java CLI emits a versioned JSON inventory with parser identities, evidence paths, snapshot digest, counts, and diagnostics.

Python and Java remain separate local modules. They can be updated independently and use repository-relative evidence paths.

## Maven parser decision

The OpenRewrite Maven parser is not used for static P0.2 discovery. In the v8.90.4 implementation, MavenParser resolves a Pom model using a MavenPomDownloader while processing parsed POMs; skipDependencyResolution suppresses dependency resolution but does not turn the parser into a pure XML-only local parser. It could attempt to access parent POMs or repositories.

QIVRA therefore uses the Python XML scanner for explicitly local-only parent traversal and OpenRewrite XML parsing for syntax only. A future Maven Resolver integration requires a separate policy-controlled dependency-resolution milestone.

## What is deliberately not integrated

- Gradle Tooling API / effective Gradle model execution: can evaluate untrusted build logic.
- Maven Resolver: deferred until allow-listed egress, offline mode, cache governance, credential isolation, and audit are implemented.
- OpenRewrite recipes or recipe packs: not bundled; review each artifact and transitive dependency before P0.4.
- Syft: candidate for generating a release SBOM, not a source-analysis dependency.
- Automatic source mutation: neither parser changes customer files.

## Selected AI workflow skills

QIVRA combines Anti Slop with three selected Superpowers skills:
- Anti Slop: design, copy, accessibility, mobile layout, and code-comment quality.
- Superpowers TDD: tests before production changes.
- Superpowers systematic debugging: evidence and root-cause analysis before fixes.
- Superpowers verification: fresh full-run evidence before completion claims.

The skill snapshot details and reviewed but uninstalled repositories are documented in docs/AI_SKILLS.md. These are project instructions only; no AI framework is installed in the runtime.

## Security boundaries

- Treat every repository file as untrusted input.
- Hybrid parsing has file-count and per-file size limits.
- No Maven/Gradle project scripts, plugins, lifecycle goals, tests, application code, or shell scripts are invoked.
- The scanner never contacts Maven, Gradle, or third-party registries.
- Findings are syntax/declaration observations, not proof of build success, runtime compatibility, or migration safety.
