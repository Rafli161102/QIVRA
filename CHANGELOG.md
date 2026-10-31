# Changelog

All notable changes to QIVRA are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project will use [Semantic Versioning](https://semver.org/spec/v2.0.0.html) once versioned software releases begin.

## [Unreleased]

### Added
- Established the QIVRA product overview and initial Java modernization vertical slice.
- Documented product boundaries, target architecture, security model, licensing review process, and milestone roadmap.
- Added contribution guidance, repository hygiene files, and pull request template.

### Notes
- This foundation milestone does not represent a runnable product release.
- No transformation engine or third-party runtime dependency has been integrated yet.

## Release policy

- Keep active work under `[Unreleased]`.
- When publishing a release, move the relevant entries into a versioned heading using the exact tag and release date.
- Do not create a version tag until the corresponding code, tests, and release notes are verified.
- Use pre-release identifiers (for example, `0.1.0-alpha.1`) while the public API and behavior are unstable.
