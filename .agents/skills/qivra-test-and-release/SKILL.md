---
name: qivra-test-and-release
description: Verify QIVRA changes with reproducible tests, CI evidence, documentation updates, dependency/license checks, and disciplined release versioning.
---

# Test and release workflow

## Process

1. Identify the smallest behavior slice and its acceptance criteria.
2. Add or update fixtures before implementation where practical.
3. Run focused unit/contract tests, then the full relevant suite.
4. Run typecheck, static analysis, build, and browser checks when affected.
5. Inspect CI logs; a queued or running workflow is not a pass.
6. Update README, architecture docs, roadmap, and CHANGELOG when behavior or project status changes.
7. For each dependency, record exact coordinates, source, license, notices, transitive review status, and security review.
8. Use a feature branch and PR into develop. Keep main stable and promote only through an intentional release flow.
9. Do not create a release tag for an incomplete milestone or docs-only change.
10. Report merge status, exact commit, checks, known limitations, and next acceptance criteria.

## Verification

Never say "passed" without a completed passing check. Never suppress, skip, or weaken a failing test to make CI green. Preserve failing logs as evidence and fix the underlying issue.
