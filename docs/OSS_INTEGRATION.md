# Open-source parser integration

## Integration objective

QIVRA combines complementary open-source parsers behind a small analyzer boundary. The integration uses version-pinned Maven artifacts instead of copying upstream repositories or source trees into QIVRA. This keeps upstream provenance explicit and avoids maintaining private parser forks.

## Selected upstream projects

| Upstream | Release | Selected artifacts | Analyzer role | License selection |
|---|---|---|---|---|
| JavaParser | javaparser-parent-3.28.2 | com.github.javaparser:javaparser-core:3.28.2 and com.github.javaparser:javaparser-symbol-solver-core:3.28.2 | Java AST inventory, source locations, and workspace/JDK method identity | Apache-2.0 option; upstream also offers LGPL-3.0 |
| OpenRewrite | v8.90.4 | org.openrewrite:rewrite-gradle:8.90.4 | Gradle Groovy/Kotlin syntax parsing | Apache-2.0 |
| OpenRewrite | v8.90.4 | org.openrewrite:rewrite-xml:8.90.4 | Maven POM XML syntax parsing | Apache-2.0 |
| Tomlj | 1.1.1 | org.tomlj:tomlj:1.1.1 | Gradle version-catalog TOML parsing | Apache-2.0 |

Exact coordinates are pinned in engine/analyzer-java/pom.xml. OpenRewrite's v8.90.4 source tag is used because the matching rewrite-gradle and rewrite-xml artifacts are published to Maven Central. Newer OpenRewrite source releases such as v8.96.3 are distributed through the Code Genome Project artifact repository, which requires authentication; this prototype deliberately avoids adding that credentialed repository to public CI. Upstream Apache license texts are preserved in LICENSES and referenced from THIRD-PARTY-NOTICES.md.

## Combined analysis path

1. The Python scanner discovers Maven and Gradle metadata and extracts literal declarations without running build tools.
2. JavaParser reads Java files and emits package, import, type, method, method-call-name, language-feature, and source-line signals.
3. JavaParser Symbol Solver resolves method calls/references against scanned workspace source roots and the JDK where possible. External dependency JARs are not fetched.
4. OpenRewrite's Gradle parser validates Groovy and Kotlin DSL syntax; its XML parser validates POM XML syntax.
5. Tomlj parses literal Gradle version-catalog versions, libraries, plugins, bundles, and version references.
6. A hardened JDK XML pass resolves only safe workspace-relative Maven parents and statically merges visible properties and dependency-management declarations; remote parents and BOMs are not resolved.
7. The hybrid Java CLI emits one versioned JSON inventory with parser identities, counts, source paths, a stable input digest, and diagnostics.

The Python and Java runtimes remain separate modules. Each can be updated independently; both use repository-relative evidence paths.

## What is deliberately not integrated

- Gradle Tooling API and Gradle model execution: not used during static discovery because a full model can require evaluating customer build logic.
- OpenRewrite Maven effective-model/dependency resolution: not used. The selected artifact is rewrite-xml, not a Maven resolver. QIVRA's own XML model pass resolves only workspace-relative parents.
- Maven Resolver and remote parent/BOM/dependency resolution: deferred until a separately approved dependency-resolution phase with repository allow-lists, offline controls, cache policy, and explicit network permission.
- OpenRewrite recipes: not bundled in this milestone. Recipe packs and plugins must be assessed per artifact before any P0.4 integration.
- Automatic source mutation: neither parser changes customer code or build files.

## Security boundaries

- Treat every repository file as untrusted input.
- Hybrid parsing has file-count and per-file size limits.
- No Maven/Gradle project scripts, plugins, lifecycle goals, tests, or project code are invoked.
- The scan itself does not contact package registries or artifact repositories. Symbol resolution does not resolve remote or external dependency JARs.
- Findings are syntax/declaration observations, not proof of successful builds, runtime compatibility, or safe migration.

## Next technical step

Use parsed ASTs and build syntax trees to enrich inventory evidence, then implement a separately versioned analyzer adapter contract. Dependency resolution and transformation recipes remain separate, approval-gated increments.
