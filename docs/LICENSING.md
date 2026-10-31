# Third-party Licensing and Provenance

## Policy

QIVRA's original source is licensed under Apache-2.0. This does not relicense third-party software or grant rights to third-party names, trademarks, recipes, plugins, examples, or datasets.

No third-party runtime dependency is currently integrated into the repository. The entries below are candidates and review requirements, not approvals to redistribute an artifact.

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

## Candidate: OpenRewrite

- Upstream project: https://github.com/openrewrite/rewrite
- Upstream project describes its core repository as Apache License 2.0.
- The broader OpenRewrite ecosystem includes separate modules, recipe packs, plugins, and commercial offerings. They must not be assumed to share one license.
- Before integration, inspect the exact Maven coordinates and source tag for each artifact, including recipe modules and transitive dependencies.
- Exclude any component whose terms are unclear or incompatible with QIVRA's distribution and hosted-service model until written legal approval is obtained.
- Do not copy or redistribute upstream recipes merely because the core repository is Apache-2.0.

Status: **Not integrated — per-artifact legal and technical review required.**

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
| OpenRewrite core | Not selected | Java analysis / transformation adapter | Verify exact artifact | Not integrated |
| OpenRewrite recipe modules | Not selected | Migration recipes | Verify each artifact independently | Not integrated |
| TypeScript dependencies | Not selected | API / dashboard | Verify package and transitive licenses | Not integrated |
| Container runtime | Deployment-dependent | Worker isolation | Review distribution and hosting terms | Not integrated |

Update this table whenever a dependency is proposed, added, upgraded, or removed. A lockfile and generated software bill of materials (SBOM) should accompany the first executable milestone.

## Release gate

A release must not ship with an unreviewed dependency, missing license notice, prohibited recipe, or unresolved license conflict. Keep dependency review separate from security vulnerability review; passing one does not imply passing the other.

This document is an engineering control, not legal advice. Obtain qualified legal review before commercial distribution or enterprise contracting.
