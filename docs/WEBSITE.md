# QIVRA Website

## Current scope

The repository contains the first public website foundation at `apps/website/`.

- Next.js App Router and TypeScript.
- Server-rendered page content.
- Engineering Atlas design tokens and responsive CSS.
- Semantic navigation, skip link, focus styles, and reduced-motion handling.
- Migration Trace workflow explanation.
- Honest pre-alpha status and Java modernization target scope.

The website is a product overview. It is not the QIVRA analyzer, dashboard, transformation engine, or a working migration service.

## Local development

Requirements: Node.js 20.9 or newer and npm.

From the repository root:

```bash
npm install
npm run dev
```

Quality commands:

```bash
npm run typecheck
npm run build
npm run check
npx playwright install chromium
npm run test:e2e
```

## Implementation notes

- The root package uses npm workspaces and delegates commands to `apps/website`.
- Direct framework dependencies are pinned in the website package manifest.
- No external font, icon, animation, or component package is required by the production website. Playwright is a development-only test dependency.
- No logo, custom illustration, customer imagery, or fabricated product screenshot is included.
- The CSS uses the derived muted text shade `#555A52` for readable secondary copy. Machine Grey is reserved for non-text annotation.
- The migration sequence is explicitly presented as an intended workflow, not a live scan.

## Initial color contrast checks

The following pairings were calculated using the WCAG relative-luminance contrast formula:

| Foreground | Background | Ratio | Use |
|---|---|---:|---|
| `#555A52` | `#EAE6DC` | 5.67:1 | Secondary text on Archive Paper |
| `#555A52` | `#DED9CE` | 5.02:1 | Secondary text on the workflow surface |
| `#98452D` | `#EAE6DC` | 5.23:1 | Accent-colored text on Archive Paper |
| `#98452D` | `#DED9CE` | 4.63:1 | Accent-colored text on the workflow surface |
| `#FFFFFF` | `#B85C3B` | 4.53:1 | Primary action text on Oxide Signal |
| `#EAE6DC` | `#202522` | 12.49:1 | Main text on Carbon Ink |

Machine Grey is not used for small text because it does not reach normal-text contrast on Archive Paper. Its role is limited to non-text annotation. Focus indicators and meaningful UI boundaries still require validation against their actual adjacent surfaces.

## Validation limits

The website-check GitHub Actions workflow installs dependencies, runs TypeScript checks, creates a production build, starts that build, and executes the Chromium end-to-end suite.

The dependency manifests pin direct package versions. The npm lockfile must still be generated and committed from a network-enabled checkout before treating the complete dependency graph as reproducible. CI currently installs using npm without a committed lockfile.
