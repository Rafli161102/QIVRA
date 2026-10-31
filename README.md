# QIVRA

**Intelligent Software Modernization Platform**  
*Transform Legacy. Engineer the Future.*

QIVRA is an engineering platform for understanding, planning, and executing software modernization. It is designed to help teams inspect legacy codebases, identify technical risk, apply controlled transformations, and verify the result with reproducible validation.

> **Project status:** Pre-alpha / foundation phase. The repository currently contains product and engineering foundations. No production migration engine is available yet.

## What QIVRA is intended to do

- **Codebase intelligence** — map project structure, languages, frameworks, dependencies, and build systems.
- **Reverse engineering** — produce explainable inventories and dependency relationships from existing code.
- **Modernization planning** — identify upgrade paths, compatibility concerns, and migration risks.
- **Automated transformation** — apply deterministic, reviewable migration recipes.
- **Validation** — run builds and tests before and after transformations.
- **Auditability** — retain diffs, execution metadata, validation results, and reports.

## Initial implementation scope

The first vertical slice targets **Java application modernization**:

1. Discover Maven and Gradle projects.
2. Inventory Java version, dependencies, plugins, and framework signals.
3. Generate a migration assessment and proposed plan.
4. Preview transformations as a diff before applying them.
5. Run validation in an isolated workspace and report failures clearly.

Java 8/11 to Java 17 is an initial use case, not a permanent product limitation. Other languages and migration paths will be added only after the first workflow is reliable.

## Engineering principles

- **Review before mutation:** never silently rewrite a customer's source tree.
- **Deterministic first:** prefer explicit, testable recipes; use AI assistance only where it adds measurable value.
- **Validation is part of migration:** a transformation is not successful merely because files changed.
- **Secure by default:** treat repositories as untrusted input and execute builds in isolated environments.
- **Transparent licensing:** audit each dependency, plugin, and recipe independently before distribution.
- **Provider-neutral architecture:** keep the orchestration layer separate from transformation engines.

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

This is a target architecture, not a claim that these components have already been implemented.

## Repository layout

The repository will evolve toward a monorepo:

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

Directories will be introduced with working code, not empty placeholder scaffolding.

## Development status

See [CHANGELOG.md](CHANGELOG.md) for recorded changes and [docs/ROADMAP.md](docs/ROADMAP.md) for milestone sequencing.

| Milestone | Objective | Status |
|---|---|---|
| P0.1 | Product, architecture, security, and licensing foundation | In progress |
| P0.2 | Repository and build-system analyzer | Planned |
| P0.3 | Modernization assessment and migration plan | Planned |
| P0.4 | Transformation engine integration | Planned |
| P0.5 | Isolated validation pipeline | Planned |
| P0.6 | Diff, audit trail, and reports | Planned |
| P0.7 | Dashboard and project workflows | Planned |
| P0.8 | Git provider integration | Planned |

## Getting started

There is no runnable application yet. The analyzer and runtime setup will be documented when P0.2 introduces executable code.

## Security

Do not upload confidential customer repositories or secrets to this project. Review [SECURITY.md](SECURITY.md) before using any future execution feature.

## Licensing

QIVRA's original source is intended to be licensed under Apache-2.0. Third-party engines, plugins, recipes, and transitive dependencies remain subject to their own licenses. See [docs/LICENSING.md](docs/LICENSING.md); do not assume that every component in an upstream ecosystem has the same license.

## Contributing

Contribution guidance will be maintained in [CONTRIBUTING.md](CONTRIBUTING.md). Architecture and security decisions should be discussed before introducing a new runtime, migration engine, or external service.

---

QIVRA is under active development. Features, architecture, and compatibility promises are not production guarantees until implemented and covered by tests.
