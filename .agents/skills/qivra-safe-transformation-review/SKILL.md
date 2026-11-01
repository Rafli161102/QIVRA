---
name: qivra-safe-transformation-review
description: Plan, review, test, and approve a source transformation or modernization recipe with preview diffs, bounded workspaces, and rollback evidence. Use before any customer source mutation.
---

# Safe transformation review

## Process

1. Start from an immutable repository revision and a versioned assessment.
2. Select an explicitly pinned transformation engine and recipe artifact.
3. Review exact artifact coordinates, licenses, transitive dependencies, provenance, advisories, and recipe behavior.
4. Define the allowed workspace paths and expected file-change set.
5. Run transformations only in a disposable isolated worker, never in the API/control plane.
6. Produce a dry-run diff and changed-file manifest before applying.
7. Require explicit human approval of the exact diff and plan.
8. Apply only the approved plan; reject path traversal, symlink escape, unexpected files, and unbounded output.
9. Validate idempotency, formatting, compilation, tests, and postconditions in a separately isolated validation phase.
10. Preserve before/after digests, engine/recipe versions, approvals, diagnostics, and rollback artifacts.

## Stop conditions

Stop on missing evidence, license uncertainty, unexpected file changes, unbounded output, worker escape, failed postconditions, or a changed source revision.

## Guardrails

- No automatic mutation from an AI-generated suggestion.
- No recipe pack is trusted because its parent repository is permissively licensed.
- No production release before transitive dependency and security review.
- Never overwrite the source branch silently.

## Verification

A transformation is complete only when the approved diff matches the applied diff, all required validation passes, and the audit record is reproducible.
