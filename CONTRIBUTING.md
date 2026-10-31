# Contributing to QIVRA

Thank you for helping improve QIVRA. The project is in pre-alpha, so changes to product boundaries, security, licensing, and architecture need careful review.

## Before starting

1. Read [README.md](README.md), [docs/PRODUCT_SCOPE.md](docs/PRODUCT_SCOPE.md), and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
2. For execution, repository ingestion, or dependency changes, read [SECURITY.md](SECURITY.md) and [docs/LICENSING.md](docs/LICENSING.md).
3. Search existing issues and pull requests before creating duplicate work.
4. Open an issue or design discussion for changes that affect public APIs, architecture, supported migration paths, or third-party engines.

## Branch model

- `main` is the stable, release-ready branch.
- `develop` is the integration branch.
- Use short-lived branches from `develop`: `feature/<topic>`, `fix/<topic>`, `docs/<topic>`, or `chore/<topic>`.
- Release preparation uses `release/<version>`; urgent production corrections use `hotfix/<topic>`.
- Do not force-push shared branches. Do not commit directly to `main` or `develop` once branch protections are enabled.

## Commit messages

Use clear, scoped Conventional Commit-style messages:

- `feat(analyzer): discover Maven modules`
- `fix(parser): handle malformed version properties`
- `docs(architecture): clarify worker boundary`
- `chore(deps): update audited parser dependency`

Keep commits focused. Do not bundle unrelated formatting, behavior, and dependency changes.

## Pull request requirements

A pull request should explain:

- the problem and intended behavior;
- design decisions and alternatives considered;
- tests run and their results;
- security implications, especially for untrusted repository input;
- new or changed dependencies and their licenses;
- breaking changes and migration notes.

Include examples or fixtures for parser and transformation behavior. Never include real customer source code, secrets, access tokens, or private repository URLs.

## Testing expectations

Every behavior change must include appropriate automated tests. Parser changes should include valid, malformed, and boundary-case fixtures. Transformation changes must assert exact diffs and prove that validation failures are surfaced rather than hidden.

## Documentation and changelog

Update README, architecture docs, or roadmap when user-visible scope or implementation status changes. Add a concise entry under `[Unreleased]` in [CHANGELOG.md](CHANGELOG.md) for notable changes. Do not claim an unreleased feature is implemented.

## Versioning

QIVRA will use Semantic Versioning for software releases, with pre-release identifiers while behavior is unstable (for example, `0.1.0-alpha.1`). Do not create release tags before tests and release notes are ready. The changelog heading, Git tag, package version, and release metadata must match exactly.

## License

By contributing, you agree that your contributions are submitted under the Apache License 2.0, subject to any separate written agreement. Do not copy code, recipes, fixtures, or documentation from third-party projects unless their licensing and attribution requirements have been reviewed and recorded.
