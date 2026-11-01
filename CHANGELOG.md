# Changelog

All notable changes to QIVRA are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project will use [Semantic Versioning](https://semver.org/spec/v2.0.0.html) once versioned software releases begin.

## [Unreleased]

### Added
- Added Gradle version-catalog TOML inventory for versions, library aliases, plugin aliases, bundles, and literal version references using Tomlj 1.1.1.
- Added a hardened partial Maven static model for safe workspace-relative parent resolution, inherited properties and dependencyManagement, dependency version provenance, BOM import inventory, plugin declarations, and profile activation metadata.
- Added eight QIVRA-specific Agent Skills for repository intelligence, build-model analysis, evidence graphs, modernization assessment, safe transformations, AI provider safety, and verification/release workflows.
- Added an account-wide repository reuse audit documenting reusable patterns and explicit non-reuse boundaries.
- Added Gradle version-catalog and Maven parent/dependency-management fixtures to the analyzer CI gate.
- Added a hybrid Java source analyzer using JavaParser 3.28.2 for AST inventory and OpenRewrite 8.90.4 parser modules for Gradle Groovy/Kotlin and Maven POM XML syntax.
- Added Java source fixtures for legacy-style classes, records, switch expressions, and malformed source, plus a combined analyzer CI gate.
- Added versioned hybrid inventory schema 1.2.0, pinned upstream provenance documentation, and Apache-2.0 notices for the selected parser components.
- Added the root npm workspace lockfile and deterministic dependency installation with `npm ci`.
- Established the QIVRA product overview and initial Java modernization vertical slice.
- Documented product boundaries, target architecture, security model, licensing review process, and milestone roadmap.
- Added contribution guidance, repository hygiene files, and pull request template.
- Added a GitHub Actions documentation-integrity workflow and a local-link validator.
- Vendored Anti Slop v3.2.24 core and all five supplemental skills for project-scoped AI development, with upstream license notices preserved.
- Added QIVRA agent instructions and Anti Slop installation/update documentation.
- Added the product-owner-approved Engineering Atlas design direction, including the Migration Trace identity motif, conceptual palette, typography direction, layout principles, and design dials.
- Added the first Next.js website workspace with an Engineering Atlas product overview, responsive layout, semantic Migration Trace sequence, and website verification workflow.
- Added website setup and implementation notes in `docs/WEBSITE.md`.
- Added Playwright Chromium end-to-end checks for navigation, link destinations, keyboard skip-link behavior, touch-target sizing, key contrast pairs, reduced motion, and responsive overflow.
- Added production SEO metadata, Open Graph and Twitter summary metadata, canonical-origin handling, production sitemap, preview crawler restrictions, and baseline response security headers.
- Added a Vercel deployment guide with monorepo root settings, environment variables, preview review, and production promotion requirements.

### Changed
- Promoted the website design brief from an unanswered questionnaire to a record of the approved Engineering Atlas direction.
- Updated the README and Anti Slop operating notes to point to the authoritative `DESIGN.md`.
- Updated the README with the website workspace and current implementation status.
- Declared the npm workspace package manager version in the root package manifest.

### Notes
- The public website foundation is separate from the QIVRA modernization engine. The P0.2 analyzer now includes a local Python metadata scanner and a JavaParser/OpenRewrite static source-parser prototype; no hosted migration service, effective build-model resolver, or transformation engine is available.
- The website workflow and Java scope are presented as planned product behavior, not released capabilities.
- No logo, custom illustration, customer imagery, testimonials, benchmark figures, or performance claims were added.
- The website build must pass the website-check workflow before it is considered build-verified.
- Playwright is a development-only dependency and its Apache-2.0 license notice is retained.
- Added the npm workspace lockfile. CI now installs from the committed lockfile using `npm ci`; direct and transitive license review and SBOM generation remain production release requirements.
- No OpenRewrite recipes, transformation engine, effective Maven/Gradle model resolver, or customer build-script execution has been integrated.
- Vercel preview deployment remains pending project connection; no deployment URL or production domain is claimed.

## Release policy

- Keep active work under `[Unreleased]`.
- When publishing a release, move the relevant entries into a versioned heading using the exact tag and release date.
- Do not create a version tag until the corresponding code, tests, and release notes are verified.
- Use pre-release identifiers (for example, `0.1.0-alpha.1`) while the public API and behavior are unstable.
