---
name: qivra-using-agent-skills
description: Select and coordinate QIVRA's project skills for repository analysis, build-model discovery, modernization planning, AI safety, transformations, verification, and releases. Use at the start of QIVRA engineering tasks.
---

# QIVRA skill router

## Purpose

Choose the smallest set of project skills needed for the task. Skills are workflows, not product capabilities. Do not claim a workflow is implemented in the QIVRA runtime merely because a skill documents it.

## Routing

| Task | Required skill |
|---|---|
| Understand an unfamiliar repository or cross-repository relationship | \`qivra-repository-intelligence\` |
| Inspect Maven POMs, Gradle scripts, or version catalogs | \`qivra-build-model-analysis\` |
| Build a code/build/module relationship graph | \`qivra-graph-evidence\` |
| Assess compatibility against a modernization target | \`qivra-modernization-assessment\` |
| Propose or apply source transformations | \`qivra-safe-transformation-review\` |
| Add an AI provider, model, prompt, or generated explanation | \`qivra-ai-provider-safety\` |
| Add tests, update CI, version, or prepare a release | \`qivra-test-and-release\` |
| Change product UI or user-facing copy | Existing Anti Slop core and relevant supplemental skills |

## Operating rules

1. Read the selected skill before planning the task.
2. Prefer source evidence and deterministic analysis before model-generated interpretation.
3. Keep extracted facts separate from resolved relationships and inferred hypotheses.
4. Keep every finding traceable to a repository-relative file and, when available, a line or syntax location.
5. Never execute an untrusted build script during static discovery.
6. Do not mutate customer source without an explicit reviewed plan and approval.
7. Report what was actually tested. A passing parser test is not a successful build or migration.
8. If skills overlap, follow the stricter safety boundary and avoid duplicating the same checklist.

## Exit criteria

The task is complete only when its relevant skill verification steps pass, documentation matches actual behavior, and unresolved limitations are stated.
