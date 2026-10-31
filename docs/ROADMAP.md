# QIVRA Roadmap

## Delivery rules

- Complete a milestone with working behavior and tests, not only directories or diagrams.
- Keep README and CHANGELOG aligned with actual implementation.
- Do not publish version tags for documentation-only or incomplete software milestones.
- Each milestone should state deliverables, acceptance criteria, and known limitations.

## P0 — Foundation and first vertical slice

### P0.1 — Product and engineering foundation
**Status: Complete**

- Product definition and first Java modernization use case.
- Architecture and engine adapter boundary.
- Security requirements for repository ingestion and worker execution.
- Third-party license review process.
- Branching, contribution, and changelog policies.

### P0.2 — Repository and build-system analyzer
**Status: In progress**

Deliverables:
- [x] Analyzer module and documented local runtime (Python standard library, CLI).
- [x] Maven POM discovery and declared multi-module inventory.
- [x] Gradle build/settings discovery for Groovy and Kotlin DSL.
- [x] Initial Java source/target/release/toolchain declaration extraction.
- [x] Literal dependency and plugin declaration inventory.
- [x] Malformed Maven POM diagnostics with file evidence.
- [x] Versioned JSON inventory schema and deterministic snapshot digest.
- [x] Initial fixture corpus for Maven, Gradle, nested, mixed, and malformed projects.
- [ ] Improve Gradle project-directory mapping, version catalogs, and computed declarations.
- [ ] Add Maven parent inheritance, profile-aware parsing, and richer diagnostics.
- [ ] Add Java source-level language/API signals with file and line evidence.
- [ ] Expand fixture coverage and validate against representative repositories.

Acceptance criteria:
- Analyzer never executes build scripts during static discovery.
- Every finding includes evidence or a clear reason why evidence is unavailable.
- Tests prove behavior for Maven, Gradle, mixed, nested, and malformed fixtures.
- Output is deterministic for the same input snapshot and analyzer version.

The first implementation is a declaration-level inventory only. It does not resolve dependencies, calculate effective Maven/Gradle models, parse Java source, or produce migration recommendations. P0.2 remains open until the remaining items and acceptance criteria are verified.

### P0.3 — Modernization assessment and plan
- Introduce target profiles, initially Java 17.
- Compare detected settings and APIs against target requirements.
- Generate explainable findings, risks, prerequisites, and ordered plan steps.
- Keep uncertain detections explicit; do not fabricate a migration recommendation.

### P0.4 — Transformation engine integration
- Define versioned engine adapter contract.
- Complete artifact-by-artifact license and security review.
- Integrate a minimal, pinned recipe set.
- Produce preview diffs; require approval before apply.
- Test idempotency and ensure transformations do not escape the workspace.

### P0.5 — Isolated validation
- Disposable worker boundary.
- Resource and time limits.
- Controlled dependency network access.
- Before/after build and test execution.
- Explicit cancellation, timeout, and failure state handling.

### P0.6 — Diff, audit, and reporting
- Changed-file manifest and unified diff.
- Job, engine, recipe, policy, and source revision metadata.
- Structured diagnostics and downloadable assessment report.
- Auditable approval and execution events.

### P0.7 — Dashboard
- Project registration and job history.
- Inventory and findings views.
- Migration plan review and approval.
- Diff review and validation report.
- Clear states for incomplete, failed, cancelled, and successful work.

### P0.8 — Git provider integration
- Least-privilege repository access.
- Immutable source revision per job.
- Branch-based output or patch export.
- No automatic commit or push without explicit user authorization.

## P1 — Team and commercial readiness

- Multi-user workspaces, roles, and policy controls.
- Repeatable project and organization-level reporting.
- More Java framework and build-tool migration paths.
- CLI and SDK.
- Enterprise deployment option and operational observability.
- Billing and usage metering only after customer discovery validates packaging.
- Additional languages only after a tested adapter and fixture strategy exist.

## Release gates

### Alpha
- Core analyzer and at least one end-to-end local workflow.
- Known limitations published.
- Automated tests and dependency/license inventory.

### Beta
- Secure worker execution, repeatable validation, usable review flow, and documented data handling.
- Representative real-world pilot repositories with permission.

### 1.0
- Stable API and migration behavior for documented scope.
- Security review and recovery/rollback strategy.
- Support, retention, privacy, licensing, and upgrade policies.
- Reproducible builds, release automation, and operational monitoring.
