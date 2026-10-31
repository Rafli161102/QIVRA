# QIVRA Website

## Current scope

The repository contains the first public website foundation at `apps/website/`.

- Next.js App Router and TypeScript.
- Server-rendered page content.
- Engineering Atlas design tokens and responsive CSS.
- Semantic navigation, skip link, focus styles, and reduced-motion handling.
- Migration Trace workflow explanation.
- Honest pre-alpha status and Java modernization target scope.
- Production metadata, Open Graph and Twitter card metadata, and origin-based canonical URLs.
- Production-only sitemap and crawler access. Preview, local, and CI builds are configured as non-indexable.
- Baseline security response headers.

The website is a product overview. It is not the QIVRA analyzer, dashboard, transformation engine, or a working migration service.

## Local development

Requirements: Node.js 20.9 or newer and npm 10.9.9.

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

## SEO and crawler behavior

The metadata in `app/layout.tsx` defines the site title, description, Open Graph website metadata, and Twitter summary card. A canonical URL and `metadataBase` are added only for a configured production deployment.

Set `NEXT_PUBLIC_SITE_URL` to the real HTTPS origin in the Vercel Production environment. The value must be an origin only, with no path, query, or fragment. Do not set the production origin as a Preview variable.

The `app/robots.ts` and `app/sitemap.ts` handlers use Vercel's `VERCEL_ENV` and the configured canonical origin:
- Production with a valid origin: crawling is allowed and the sitemap lists the homepage.
- Preview, local, CI, or missing origin: crawling is disallowed and no public sitemap entries are emitted.

The website does not yet include an Open Graph image, favicon, or logo. Bespoke visual assets require separate product-owner approval.

## Security response headers

The website sets the following baseline headers through `next.config.ts`:

- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Referrer-Policy: strict-origin-when-cross-origin`
- `Permissions-Policy: camera=(), microphone=(), geolocation=()`

This is a baseline for the static public marketing surface, not a complete security certification. A Content Security Policy and HSTS policy should be designed when third-party scripts, forms, analytics, or custom-domain production hosting are introduced.

## Vercel deployment

See [DEPLOYMENT.md](DEPLOYMENT.md) for repository import, monorepo root settings, preview/production branch behavior, canonical origin configuration, and the pre-promotion checklist.

The GitHub repository is not yet connected to Vercel. No live preview URL is available.

## Implementation notes

- The root package uses npm workspaces and delegates commands to `apps/website`.
- The root `package.json` declares `npm@10.9.9` as the package manager.
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

## Validation

The website-check GitHub Actions workflow installs dependencies, runs TypeScript checks, creates a production build, starts that build, and executes the Chromium end-to-end suite.

The browser suite checks the rendered page, anchor navigation, CTA links, keyboard skip-link focus, link target sizes, text/action contrast, responsive overflow from 320px to 1440px, reduced motion, SEO metadata, preview crawler rules, and baseline response headers.

The dependency manifests pin direct package versions, and the root package declares `npm@10.9.9`. The npm lockfile must still be generated and committed from a network-enabled checkout before treating the complete dependency graph as reproducible. CI currently uses `npm install`; switch it to `npm ci` after the lockfile is committed.
