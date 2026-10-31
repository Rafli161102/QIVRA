# QIVRA Website Deployment

## Current deployment state

The website is build-verified in GitHub Actions, but it has not yet been connected to a Vercel project or deployed to a public URL. No production domain is configured.

Use Vercel Preview Deployments for review. Keep `main` as the Production Branch so pushes to `develop` and feature branches remain previews rather than becoming the public production deployment.

## Before importing into Vercel

Generate and commit the npm lockfile from a network-enabled checkout at the repository root:

```bash
npm install --package-lock-only --ignore-scripts
npm ci
npm run typecheck
npm run build
npm run test:e2e
```

Review the generated `package-lock.json` and commit it with the package manifests. Once the lockfile is committed, change the website-check workflow from `npm install` to `npm ci` so CI and Vercel can use the same dependency graph.

The root `package.json` declares the workspace manager as `npm@10.9.9`. Node.js 22 is the CI baseline.

## Create the Vercel project

1. Connect Vercel to the GitHub account that owns `Rafli161102/QIVRA`, then import that repository.
2. Use the **Next.js** framework preset.
3. Set the **Root Directory** to `apps/website`.
4. Enable **Include source files outside of the Root Directory**. The npm workspace manifest and lockfile live at the repository root, outside the website package directory.
5. Set the **Production Branch** to `main`. Branches such as `develop` and `feature/*` should produce Preview Deployments.
6. Use Node.js 22.x. Keep the default Next.js build output and npm workspace install detection unless the Vercel build log demonstrates a reason to override them.

Vercel's monorepo documentation explains the Root Directory and the option for including source files outside it:
- https://vercel.com/docs/monorepos
- https://vercel.com/docs/monorepos/monorepo-faq

Vercel's Git integration can create a Preview Deployment for pushes to non-production branches and a production deployment from the configured Production Branch:
- https://vercel.com/docs/git
- https://vercel.com/docs/deployments/environments

## Environment variables

Add this variable in **Vercel Project Settings → Environment Variables**:

| Variable | Environment | Value |
|---|---|---|
| `NEXT_PUBLIC_SITE_URL` | Production only | The final canonical origin, for example `https://your-real-domain.example` |

Replace the example with the real HTTPS domain before enabling production indexing. It must be an origin only: no path, query string, or fragment.

Do not set the production canonical origin as a Preview variable. Preview builds are intentionally non-indexable and do not publish a sitemap URL. `NEXT_PUBLIC_SITE_URL` is public configuration, not a secret.

Vercel environment variables are scoped to Local, Preview, and Production environments and take effect on subsequent deployments:
- https://vercel.com/docs/environment-variables

## SEO and crawler behavior

- Production builds become indexable only when `VERCEL_ENV=production` and `NEXT_PUBLIC_SITE_URL` is configured.
- Preview, local, and CI builds emit `noindex, nofollow` metadata and a `robots.txt` rule that disallows crawling.
- Production `robots.txt` allows crawling and points to `/sitemap.xml`.
- The sitemap currently contains only the public homepage. Add future public routes explicitly when they exist.
- A missing or invalid canonical origin prevents production indexing rather than publishing localhost or a fabricated domain.

Next.js documents the Metadata API, `robots.ts`, and `sitemap.ts` conventions:
- https://nextjs.org/docs/app/getting-started/metadata-and-og-images
- https://nextjs.org/docs/app/api-reference/file-conventions/metadata/robots
- https://nextjs.org/docs/app/api-reference/file-conventions/metadata/sitemap

## Preview review checklist

Before promoting a preview to production, verify:

- [ ] The page loads on desktop and mobile with no horizontal overflow.
- [ ] All primary navigation links and CTAs reach their intended sections.
- [ ] Browser console and network contain no unexpected errors.
- [ ] Page title, description, Open Graph, and Twitter card metadata are present.
- [ ] Preview metadata contains `noindex` and `nofollow`.
- [ ] Preview `/robots.txt` disallows crawling and `/sitemap.xml` has no public URL entries.
- [ ] The four baseline response headers are present.
- [ ] The preview copy still distinguishes planned product behavior from implemented software.

Do not promote to production until the real canonical domain is configured and the production robots/sitemap output has been checked.

## Known limitations

- Vercel is not connected to this repository yet, so no preview URL exists.
- The npm lockfile is not committed yet. This is the remaining reproducibility blocker before changing CI to `npm ci`.
- No custom Open Graph image, favicon, or logo is included. Bespoke brand assets remain a separate product-owner decision.
