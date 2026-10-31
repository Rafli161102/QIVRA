# QIVRA Website

Public website foundation for QIVRA, implemented with Next.js App Router and TypeScript.

## Requirements

- Node.js 20.9 or newer
- npm 10.9.9 (workspace package manager declaration)

## Commands

Run from the repository root:

```bash
npm ci
npm run dev
npm run typecheck
npm run build
npx playwright install chromium
npm run test:e2e
```

Playwright runs the Chromium browser suite against the production build. Build the website before running the end-to-end tests.

## Deployment

See [the Vercel deployment guide](../../docs/DEPLOYMENT.md). The canonical production origin must be provided through `NEXT_PUBLIC_SITE_URL` in the Vercel Production environment. Preview deployments are configured to emit noindex metadata and disallow crawling.

## Design direction

The website follows the root [DESIGN.md](../../DESIGN.md): Engineering Atlas, with paper-led editorial surfaces, Carbon Ink typography, restrained Oxide Signal accents, and the Migration Trace motif.

The current page is a factual pre-alpha product overview. It labels the modernization workflow and Java scope as planned. It does not present an operational analyzer, fabricated scan output, customer evidence, or unimplemented controls.

No bespoke logo, illustration, icon set, or social sharing image is included. The visual system uses typography, CSS layout, rules, and semantic HTML. Playwright is a development-only dependency and is not bundled into the website.
