# QIVRA AI Skills and Engineering Workflow Inventory

## Purpose

This document records the project-scoped AI instructions committed to QIVRA, the upstream sources reviewed, and the boundaries of the combinations we actually support. Skills are instructions for coding agents. They are not runtime dependencies, application features, or proof that a particular AI host supports every workflow.

## Installed: Anti Slop

| Field | Value |
|---|---|
| Upstream | https://github.com/miqdadbadjuber/anti-slop |
| Snapshot | v3.2.24 |
| Upstream Git tree | 3005235d030b4793146ad9afe8d01045efd90e1d |
| License | MIT |
| Local path | .agents/skills/ |
| Project role | Design and implementation quality filter |

Installed coverage:
- antislop: global quality filter and usage-mode rules.
- antislop-ui: visual purpose, layout, craft, and UI delivery gate.
- antislop-copywriting: product and interface copy.
- antislop-human: accessibility, contrast, keyboard, and human factors.
- antislop-layoutmobile: responsive and mobile layout behavior.
- antislop-code: code-comment hygiene.

The product-owner-approved DESIGN.md remains the source of visual direction. Anti Slop is a quality filter, not a replacement design system.

## Installed: selected Superpowers workflow skills

| Field | Value |
|---|---|
| Upstream | https://github.com/obra/superpowers |
| Snapshot commit | 2ce8aa15d05bd57213e6f746f953aafb7ded7a8b |
| License | MIT |
| Local path | .agents/skills/ |
| Project role | Test-first implementation and evidence-led engineering |

Selected skills:
- test-driven-development: RED-GREEN-REFACTOR for features and fixes. New behavior gets a failing test before implementation.
- systematic-debugging: investigate evidence and root cause before suggesting or applying fixes.
- verification-before-completion: run fresh, complete verification and inspect its result before making success claims or opening a merge-ready PR.

This is a deliberate partial integration, not a wholesale import of Superpowers. The skills are standalone Markdown guidance and do not introduce an application dependency.

## How the skill sets combine

| Work type | Primary skill | Supporting skill | QIVRA project rule |
|---|---|---|---|
| Analyzer or backend feature | Test-driven-development | Verification-before-completion | Add valid, malformed, and boundary fixtures |
| Failed test or integration | Systematic-debugging | Test-driven-development | Read CI logs and isolate root cause before edits |
| Build, parser, or dependency change | Test-driven-development | Systematic-debugging + verification | Pin versions, inspect artifact availability, update license register |
| Website or dashboard change | Anti Slop core + relevant UI skill | Human + layoutmobile + verification | Read DESIGN.md; pass Delivery Gate and browser checks |
| Product copy or documentation claims | Anti Slop copywriting | Verification-before-completion | No fabricated features, evidence, benchmarks, or customer proof |

Suggested sequence for a backend change:

1. Read relevant QIVRA architecture, security, and licensing docs.
2. Write the smallest behavior test and verify it fails for the intended reason.
3. Implement only enough behavior to satisfy that test.
4. Run the full relevant unit/build/fixture suite.
5. Inspect CI logs and diff; report remaining limitations explicitly.

## Upstream projects reviewed but not installed

| Project | License observed | Decision | Reason |
|---|---|---|---|
| https://github.com/anthropics/skills | Public Agent Skills repository; per-skill license review still required | Reviewed, not vendored | Broad catalog; no single narrow skill was required for the current analyzer work. Revisit individual skills with exact license/provenance review when a concrete need appears. |
| https://github.com/github/awesome-copilot | MIT repository | Reviewed, not vendored | Community catalogue is Copilot-oriented and heterogeneous; importing it wholesale would duplicate instructions and add maintenance noise. |
| https://github.com/obra/superpowers | MIT | Partially integrated | Only the three directly applicable workflow skills are copied. Host-specific or sub-agent-dependent capabilities are intentionally excluded. |

## Host capability boundary

The repository does not assume that every coding environment supports sub-agent dispatch, parallel agents, hidden task runners, or external tool calls. Skills that require these capabilities are not installed as active QIVRA workflows. Use the selected local guidance only through capabilities actually available in the current host.

## Maintenance and licensing

- Keep the upstream snapshot commit and full license notice alongside vendored skill files.
- Do not copy an entire skill marketplace or an upstream repository without selecting concrete files and verifying exact licensing.
- Review skill changes as code: inspect the diff, identify changed instructions, and retain attribution.
- Skills do not grant permission to run customer code, access secrets, or perform unapproved repository mutations.
- Anti Slop MIT and Superpowers MIT remain separate from QIVRA's Apache-2.0 source license.

## Related project documentation

- Anti Slop integration: ANTISLOP.md
- Third-party licensing: LICENSING.md
- Security policy: ../SECURITY.md
- Architecture: ARCHITECTURE.md
