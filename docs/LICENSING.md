# Third-party Licensing and Provenance

## Policy

QIVRA's original source is licensed under Apache-2.0. This does not relicense third-party software or grant rights to third-party names, trademarks, recipes, plugins, examples, or datasets.

The website workspace uses Next.js and React at runtime. Playwright is included only as development tooling for browser verification. Other target architecture dependencies remain candidates and are not approved for integration.

## Required review for every dependency

Before adding a dependency, record:

1. Exact package coordinates, version, repository, and artifact source.
2. License declared by the exact source release and published artifact.
3. License files, copyright notices, and NOTICE obligations.
4. Direct and transitive dependency licenses.
5. Whether the license permits the intended hosted service, redistribution, modification, and commercial use.
6. Required source disclosure, attribution, patent, trademark, or additional terms.
7. Security advisories and provenance/signature information.
8. Reviewer, review date, and approval decision.

Do not infer the license of a Maven artifact from the license of a similarly named repository or neighboring module. Review the exact artifact and all bundled components.

## Integrated parser components

### JavaParser

- Upstream: https://github.com/javaparser/javaparser
- Release: javaparser-parent-3.28.2
- Artifact: com.github.javaparser:javaparser-core:3.28.2
- Purpose: Java AST inventory and source line evidence.
- The upstream project offers Apache-2.0 and LGPL-3.0 licensing options. QIVRA explicitly selects its Apache-2.0 option; preserve LICENSES/JAVAPARSER-APACHE-2.0.txt.
- No JavaParser source files are copied into QIVRA.

### OpenRewrite parser modules

- Upstream: https://github.com/openrewrite/rewrite
- Release: v8.90.4
- Artifacts: org.openrewrite:rewrite-gradle:8.90.4 and org.openrewrite:rewrite-xml:8.90.4.
- Purpose: Gradle Groovy/Kotlin syntax parsing and Maven POM XML syntax parsing.
- License: Apache-2.0 for the selected parser modules; preserve LICENSES/OPENREWRITE-APACHE-2.0.txt.
- The broader OpenRewrite ecosystem includes other modules, recipe packs, plugins, and commercial offerings. They must not be assumed to share one license.
- No recipes, plugins, OpenRewrite Maven model resolver, or source files are included in this integration.

### Tomlj

- Upstream: https://github.com/tomlj/tomlj
- Release: 1.1.1
- Artifact: org.tomlj:tomlj:1.1.1
- Purpose: parse Gradle version-catalog TOML.
- License: Apache-2.0; preserve LICENSES/TOMLJ-APACHE-2.0.txt.
- No Tomlj source files are copied into QIVRA.

The selected parser modules are pinned in engine/analyzer-java/pom.xml. Their transitive dependencies still require package-level license and security review before production release. The integration is a pre-alpha static parser prototype, not a released migration engine.

## Vendored agent guidance: Anti Slop

- Upstream: https://github.com/miqdadbadjuber/anti-slop
- Snapshot: v3.2.24; upstream Git tree `3005235d030b4793146ad9afe8d01045efd90e1d` (retrieved 2026-10-31).
- Scope: project-local agent instructions under `.agents/skills/`. This is development tooling/documentation, not a website runtime dependency.
- License: MIT. Preserve the upstream notice at `LICENSES/ANTI-SLOP-MIT.txt`.
- The `antislop-ui` and `antislop-human` skill bundles include material from web-quality-skills; its separate MIT notice is preserved at `LICENSES/WEB-QUALITY-SKILLS-MIT.txt` and in the respective skill directories.
- QIVRA's Apache-2.0 license does not replace these upstream MIT terms. The vendored files remain subject to their applicable upstream notices.
- Update the snapshot using the documented upstream installer/update procedure in [ANTISLOP.md](ANTISLOP.md), then review the diff and license changes before merging.

## Dependency register

| Component | Candidate version | Intended use | License | Status |
|---|---|---|---|---|
| JavaParser core | 3.28.2 | Java AST inventory | Apache-2.0 option selected (upstream also offers LGPL-3.0) | Integrated in local hybrid analyzer |
| OpenRewrite Gradle parser | 8.90.4 | Gradle Groovy/Kotlin syntax parsing | Apache-2.0 | Integrated in local hybrid analyzer |
| OpenRewrite XML parser | 8.90.4 | Maven POM XML syntax parsing | Apache-2.0 | Integrated in local hybrid analyzer |
| OpenRewrite recipes / plugins | Not selected | Future transformations | Review each artifact independently | Not integrated |
| Maven Resolver | Not selected | Future dependency resolution | Apache-2.0 upstream; artifact/transitive review pending | Deferred |
| Next.js | 16.4.3 | Website runtime | MIT | Integrated; direct and transitive versions recorded in package-lock.json; license review pending |
| React / React DOM | 19.3.1 | Website runtime | MIT | Integrated; direct and transitive versions recorded in package-lock.json; license review pending |
| `@playwright/test` | 1.64.1 | Development-only browser checks | Apache-2.0 | Integrated; notice retained; lockfile committed |
| TypeScript | 5.9.3 | Website type checking | Apache-2.0 | Integrated in website workspace; lockfile committed |
| `@types/node`, `@types/react`, `@types/react-dom` | Pinned in package manifest | Development typings | MIT | Integrated in website workspace; lockfile committed |
| Container runtime | Deployment-dependent | Worker isolation | Review distribution and hosting terms | Not integrated |

The root `package-lock.json` records the resolved direct and transitive npm dependency graph for reproducible installation. This does not replace the required package-by-package license review, security review, or generated SBOM. Update this table whenever a dependency is proposed, added, upgraded, or removed. Complete the transitive license review and generate an SBOM before the first production release.

## Release gate

A release must not ship with an unreviewed dependency, missing license notice, prohibited recipe, or unresolved license conflict. Keep dependency review separate from security vulnerability review; passing one does not imply passing the other.

This document is an engineering control, not legal advice. Obtain qualified legal review before commercial distribution or enterprise contracting.
