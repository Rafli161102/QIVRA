# Third-party notices

QIVRA's original work is licensed under Apache License 2.0. The third-party files below remain under their own licenses; the QIVRA license does not replace those terms.

## Anti Slop

- Project: https://github.com/miqdadbadjuber/anti-slop
- Version snapshot: 3.2.24
- License: MIT
- Full notice: [LICENSES/ANTI-SLOP-MIT.txt](LICENSES/ANTI-SLOP-MIT.txt)
- Installed files: `.agents/skills/antislop*/`

## web-quality-skills material

Some accessibility and web-quality references/scripts are included in Anti Slop's `antislop-ui` and `antislop-human` skill folders.

- License: MIT
- Full notice: [LICENSES/WEB-QUALITY-SKILLS-MIT.txt](LICENSES/WEB-QUALITY-SKILLS-MIT.txt)
- Upstream notices are also retained inside the respective installed skill folders as `LICENSE-web-quality-skills`.

## Next.js and React

The public website uses these runtime packages:

- Next.js 16.4.3, MIT: https://github.com/vercel/next.js
- React 19.3.1 and React DOM 19.3.1, MIT: https://github.com/facebook/react

Consult the package manifests for exact direct versions. The transitive dependency graph remains subject to the generated npm lockfile review.

## Playwright

- Package: `@playwright/test`
- Version: 1.64.1
- Purpose: development-only Chromium end-to-end checks. Playwright is not included in the production website bundle.
- License: Apache-2.0
- Full license text: [LICENSES/PLAYWRIGHT-APACHE-2.0.txt](LICENSES/PLAYWRIGHT-APACHE-2.0.txt)
- Upstream source and license: https://github.com/microsoft/playwright/tree/v1.64.1
- Chromium binaries are installed by CI for test execution and are not redistributed as QIVRA product assets.
