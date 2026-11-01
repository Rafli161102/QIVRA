# Third-party Licensing and Provenance

## Policy

QIVRA's original source is licensed under Apache-2.0. This does not relicense third-party software or grant rights to third-party names, trademarks, recipes, plugins, examples, or datasets.

The website workspace uses Next.js and React at runtime. Playwright is included only as development tooling for browser verification. Other target architecture dependencies remain candidates and are not approved for integration.

## Required review for every dependency

Before adding a dependency, record exact coordinates and version, source release and artifact repository, license file and NOTICE obligations, direct and transitive dependencies, commercial/hosted-service permission, required attribution/patent/source obligations, security advisories/provenance, and review decision.

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
- Artifacts: org.openrewrite:rewrite-gradle:8.90.4, org.openrewrite:rewrite-xml:8.90.4, org.openrewrite:rewrite-toml:8.90.4.
- Purpose: Gradle Groovy/Kotlin syntax, Maven POM XML syntax, and Gradle version-catalog TOML syntax.
- License: Apache-2.0 for the selected parser modules; preserve LICENSES/OPENREWRITE-APACHE-2.0.txt.
- The broader OpenRewrite ecosystem includes other modules, recipe packs, plugins, and commercial offerings. They must not be assumed to share one license.
- No recipes, plugins, OpenRewrite Maven model resolver, or source files are included in this integration.

The selected parser modules are pinned in engine/analyzer-java/pom.xml. Their transitive dependencies still require package-level license and security review before production release. The integration is a pre-alpha static parser prototype, not a released migration engine.

## Vendored agent guidance: Anti Slop

- Upstream: https://github.com/miqdadbadjuber/anti-slop
- Snapshot: v3.2.24; upstream Git tree 3005235d030b4793146ad9afe8d01045efd90e1d (retrieved 2026-10-31).
- Scope: project-local agent instructions under .agents/skills/. This is development tooling/documentation, not a website runtime dependency.
- License: MIT. Preserve LICENSES/ANTI-SLOP-MIT.txt.
- The antislop-ui and antislop-human skill bundles include material from web-quality-skills; its separate MIT notice is preserved in LICENSES/WEB-QUALITY-SKILLS-MIT.txt and in the respective skill directories.

## Vendored agent workflow skills: Superpowers

- Upstream: https://github.com/obra/superpowers
- Snapshot commit: 2ce8aa15d05bd57213e6f746f953aafb7ded7a8b.
- Selected files: systematic-debugging, test-driven-development, and verification-before-completion.
- License: MIT. Preserve LICENSES/SUPERPOWERS-MIT.txt.
- Scope: project-local development instructions only. No upstream runtime, plugin, sub-agent runner, or host integration is included.
- Only these three skills are vendored; QIVRA does not redistribute the complete Superpowers framework.

## Reviewed / deferred projects

| Project | License / provenance | Intended role | Decision |
|---|---|---|---|
| Apache Maven Resolver | Apache-2.0 upstream | Remote or cached dependency resolution | Deferred: needs explicit egress allow-list, offline policy, credentials isolation, cache controls, and audit trail |
| Anchore Syft | Apache-2.0 upstream | SBOM generation for source/filesystem/container releases | Candidate for release engineering; not part of static P0.2 parser |
| Anthropic Agent Skills | Public skills repository; review each individual skill license | Additional task skills | Reviewed, not imported wholesale; integrate only a specific, necessary skill after provenance review |
| GitHub Awesome Copilot | MIT repository, community Copilot-focused catalog | Copilot-specific prompts/agents | Reviewed, not imported; broad and overlaps existing QIVRA workflow skills |

## Dependency register

| Component | Version | Intended use | License | Status |
|---|---|---|---|---|
| JavaParser core | 3.28.2 | Java AST inventory | Apache-2.0 option selected (upstream also offers LGPL-3.0) | Integrated |
| OpenRewrite Gradle parser | 8.90.4 | Gradle Groovy/Kotlin syntax | Apache-2.0 | Integrated |
| OpenRewrite XML parser | 8.90.4 | Maven POM XML syntax | Apache-2.0 | Integrated |
| OpenRewrite TOML parser | 8.90.4 | Gradle version catalog syntax | Apache-2.0 | Integrated |
| OpenRewrite recipes / plugins | Not selected | Future transformations | Review each artifact independently | Not integrated |
| Maven Resolver | Not selected | Future dependency resolution | Apache-2.0 upstream; artifact/transitive review pending | Deferred |
| Syft | Not selected | Release SBOM generation | Apache-2.0 upstream; binary distribution review pending | Candidate only |
| Anti Slop | v3.2.24 | AI development quality instructions | MIT | Vendored |
| Superpowers selected skills | Commit 2ce8aa15d05bd57213e6f746f953aafb7ded7a8b | TDD, systematic debugging, verification | MIT | Vendored |
| Next.js | 16.4.3 | Website runtime | MIT | Integrated; transitive license review pending |
| React / React DOM | 19.3.1 | Website runtime | MIT | Integrated; transitive license review pending |
| @playwright/test | 1.64.1 | Development-only browser checks | Apache-2.0 | Integrated; notice retained |
| TypeScript | 5.9.3 | Website type checking | Apache-2.0 | Integrated |

The root package-lock.json records the resolved direct and transitive npm dependency graph for reproducible installation. It does not replace package-level license review, security review, or an SBOM. Complete transitive dependency and SBOM review before the first production release.

## Release gate

A release must not ship with an unreviewed dependency, missing license notice, prohibited recipe, or unresolved license conflict. Keep dependency review separate from security vulnerability review; passing one does not imply passing the other.

This document is an engineering control, not legal advice. Obtain qualified legal review before commercial distribution or enterprise contracting.
