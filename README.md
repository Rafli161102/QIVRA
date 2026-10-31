# QIVRA

**Intelligent Software Modernization Platform**  
*Transform Legacy. Engineer the Future.*

QIVRA is an engineering platform for understanding, planning, and executing software modernization. It helps teams inspect legacy codebases, identify technical risk, apply controlled transformations, and verify results with reproducible validation.

> **Project status:** Pre-alpha. P0.1 foundation and product design direction are documented; no production migration engine or runnable application has been released.

## Product capabilities (target)

- **Codebase intelligence** — map project structure, languages, frameworks, dependencies, and build systems.
- **Reverse engineering** — produce explainable inventories and dependency relationships.
- **Modernization planning** — identify upgrade paths, compatibility concerns, and migration risks.
- **Automated transformation** — apply deterministic, reviewable migration recipes.
- **Validation** — run builds and tests before and after transformations.
- **Auditability** — retain diffs, execution metadata, validation results, and reports.

## First vertical slice

The first implementation targets Java application modernization:

1. Discover Maven and Gradle projects.
2. Inventory Java version, dependencies, plugins, and framework signals.
3. Generate a migration assessment and proposed plan.
4. Preview transformations as a diff before applying them.
5. Run validation in an isolated workspace and report failures clearly.

Java 8/11 to Java 17 is an initial use case, not a permanent product limitation. Other languages and migration paths follow after this workflow is reliable.

## Engineering principles

- **Review before mutation:** never silently rewrite a customer's source tree.
- **Deterministic first:** prefer explicit, testable recipes; use AI assistance only where it adds measurable value.
- **Validation is part of migration:** a transformation is not successful merely because files changed.
- **Secure by default:** treat repositories as untrusted input and execute builds in isolated environments.
- **Transparent licensing:** audit each dependency, plugin, and recipe independently before distribution.
- **Provider-neutral architecture:** keep orchestration separate from transformation engines.

## Product design direction

QIVRA's approved visual direction is **Engineering Atlas**: an industrial-editorial engineering publication language built around precise typography, paper-led surfaces, annotated technical diagrams, and varied editorial composition.

Its signature motif, **Migration Trace**, explains the path from legacy source through inspection, transformation, and validation. The direction uses a restrained palette and calm motion. It does not rely on generic SaaS templates or fabricated product evidence.

Read [DESIGN.md](DESIGN.md) for the authoritative visual direction and [docs/WEBSITE-DESIGN-BRIEF.md](docs/WEBSITE-DESIGN-BRIEF.md) for the recorded product-owner decision. The direction is approved, but no website, logo, design asset, or UI component has been implemented.

## Planned architecture

| Layer | Responsibility | Initial direction |
|---|---|---|
| Dashboard | Projects, assessments, plans, diffs, reports | Next.js + TypeScript |
| API / Orchestrator | Jobs, policies, workspace lifecycle, results | TypeScript |
| Modernization engine | Project analysis and transformations | Java 21 |
| Transformation adapters | Recipe and engine integration | OpenRewrite adapter, subject to license review |
| Execution workers | Isolated build and test execution | Docker-compatible workers |
| Persistence | Users, projects, jobs, audit records | PostgreSQL |
| Queue / coordination | Asynchronous job dispatch | Redis-compatible queue |

This is a target architecture, not a claim that these components have been implemented.

## Repository layout

The repository will evolve toward a monorepo. Directories will be added with working code instead of empty placeholders.

```text
apps/
  api/
  dashboard/
engine/
  analyzer/
  migration/
  recipes/
  validation/
packages/
  contracts/
  sdk/
examples/
docs/
infrastructure/
```

## Development status

See [CHANGELOG.md](CHANGELOG.md) for recorded changes and [docs/ROADMAP.md](docs/ROADMAP.md) for milestone sequencing.

| Milestone | Objective | Status |
|---|---|---|
| P0.1 | Product, architecture, security, and licensing foundation | Complete |
| P0.2 | Repository and build-system analyzer | Next |
| P0.3 | Modernization assessment and migration plan | Planned |
| P0.4 | Transformation engine integration | Planned |
| P0.5 | Isolated validation pipeline | Planned |
| P0.6 | Diff, audit trail, and reports | Planned |
| P0.7 | Dashboard and project workflows | Planned |
| P0.8 | Git provider integration | Planned |

## AI-assisted development quality

QIVRA vendors the [Anti Slop agent skills](https://github.com/miqdadbadjuber/anti-slop) for UI, accessibility, responsive layout, copywriting, and code-comment hygiene. The project snapshot and update procedure are documented in [docs/ANTISLOP.md](docs/ANTISLOP.md). Read [AGENTS.md](AGENTS.md) before AI-assisted development. All UI work must follow the product-owner-approved [design direction](DESIGN.md) and pass the Anti Slop Delivery Gate.

## Getting started

There is no runnable application yet. The analyzer implementation and runtime setup will be documented in P0.2.

## Security

Treat customer repositories as confidential and untrusted input. Read [SECURITY.md](SECURITY.md) before using or contributing to future execution features.

## Licensing

QIVRA's original source is licensed under Apache-2.0. Third-party engines, plugins, recipes, and transitive dependencies remain subject to their own licenses. See [docs/LICENSING.md](docs/LICENSING.md); do not assume that every component in an upstream ecosystem has the same license.

## Contributing

Read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request. Architecture and security decisions should be discussed before introducing a new runtime, migration engine, or external service.

---

QIVRA is under active development. Features, architecture, and compatibility promises are not production guarantees until implemented and covered by tests.
