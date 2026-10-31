# Anti Slop integration

## Purpose

QIVRA uses Anti Slop as project-scoped instructions for AI-assisted engineering. It is a filter for design and implementation quality, not a UI framework, website dependency, component library, or replacement for QIVRA's own product design system.

## Upstream snapshot

| Field | Value |
|---|---|
| Repository | https://github.com/miqdadbadjuber/anti-slop |
| Version | 3.2.24 |
| Upstream tree | `3005235d030b4793146ad9afe8d01045efd90e1d` |
| Snapshot date | 2026-10-31 |
| License | MIT |
| Installation scope | Project-local shared agent skills |
| Local path | `.agents/skills/` |

The snapshot contains the core skill and all five supplemental skills: UI, copywriting, accessibility/human, responsive layout, and code-comment hygiene. The upstream human/UI web-quality license files and supporting references/scripts are retained.

## What this changes

- `AGENTS.md` points agents to the project-local core and the relevant supplemental skills.
- `.agents/skills/` stores the exact upstream files from the recorded tree.
- The product-owner-approved visual direction is maintained in the root `DESIGN.md`. Anti Slop remains a quality filter and does not replace that direction.
- These files do not add a dependency to the website bundle, API, or production runtime.

## Install and update

The current snapshot is already committed in this repository. To refresh it from the official npm installer on a local checkout:

```bash
npx antislop-ai --update
```

Run the command from the QIVRA repository root. The installer requires Node.js 18 or newer. Review all changed skill files and license notices, then commit the reviewed update. Start a new agent session so it loads the refreshed files.

To install afresh rather than update:

```bash
npx antislop-ai
```

Choose the project scope and the agent(s) used for QIVRA. The installer is interactive and may update the pointer in `AGENTS.md`; keep the selected skills consistent with this repository's installed set.

Official installation guide: https://github.com/miqdadbadjuber/anti-slop/blob/main/GUIDE.md

## Operating rules for QIVRA

1. Read the core skill before UI, copy, accessibility, responsive-layout, or code-comment work.
2. Load the relevant supplemental skill for the task.
3. Resolve DURING or AFTER mode according to the core's rules before UI review or design work.
4. Follow the product-owner-approved `DESIGN.md` for brand identity, audience, visual language, palette, typography, layout, and motion. Anti Slop is the quality filter layered on top of that direction.
5. Do not fabricate customer logos, product screenshots, benchmark results, performance numbers, testimonials, or launch claims.
6. Use the Delivery Gate and task-specific checklists before considering UI work complete.
7. Review the licensing notices whenever these upstream files are updated or redistributed.

The design direction is documented, but this integration does not claim that QIVRA's website has been designed or implemented.
