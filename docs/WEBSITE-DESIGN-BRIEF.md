# QIVRA website design brief

> Status: direction approved and promoted to the authoritative root `DESIGN.md` on 2026-11-01. This brief records the product-owner decision; implementation details remain subject to testing.

## Product facts

- Brand: QIVRA
- Product category: Intelligent Software Modernization Platform
- Product purpose: analyze, plan, transform, and validate software modernization work.
- Current tagline: Transform Legacy. Engineer the Future.
- Initial implementation focus: Java modernization for Maven and Gradle projects.

## Approved direction

### 1. Brand personality

**Approved:** Investigative, precise, industrial, editorial, and accountable.

### 2. Audience and primary task

**Approved recommendation:** Primary audience is platform engineers, software architects, and technical leads responsible for legacy systems. Secondary audience is engineering managers and CTOs.

The website should help technical visitors understand QIVRA's analysis and modernization workflow, the initial Java/Maven/Gradle scope, and the distinction between current implementation and planned capabilities.

### 3. Visual references

**Approved direction:** Engineering Atlas. Use the visual logic of technical field manuals, architectural drawings, industrial product documentation, and editorial engineering publications. These are broad references, not templates to copy.

### 4. Color

**Approved conceptual palette:**

- Archive Paper: `#EAE6DC`
- Carbon Ink: `#202522`
- Machine Grey: `#85877E`
- Oxide Signal: `#B85C3B`

The palette must pass contrast checks before implementation. Paper-led light surfaces are the marketing-site default; a dark workspace theme is not assumed.

### 5. Typography

**Approved direction:** Editorial grotesk or humanist sans for display and reading, paired with a legible technical monospace for metadata and code references. No exact font family is locked; licensing, language coverage, readability, and performance must be checked.

### 6. Layout and density

**Approved:** Asymmetric editorial composition, content-led section structure, deliberate whitespace, and varied rhythm. Technical annotations and diagrams should explain real system relationships. Avoid generic SaaS templates.

### 7. Motion and interaction

**Approved:** Calm motion, limited to useful feedback and state changes. Respect reduced-motion preferences. No parallax or scroll-jacking.

### 8. Accessibility and responsive constraints

**Approved implementation target:** WCAG 2.2 AA, keyboard operation, visible focus, semantic structure, 200% zoom support, no horizontal overflow, and touch targets of at least 44px. Test contrast rather than relying on visual judgment.

### 9. Required pages and content

**Initial content priorities:** Explain the product, modernization workflow, initial technical scope, architecture approach, and current development status. Final page map and calls to action remain subject to product and implementation readiness. Do not fabricate proof points, customer logos, testimonials, screenshots, or metrics.

## Signature identity

**Migration Trace** is the recurring motif: a connected, annotated path through:

`LEGACY SOURCE / INSPECTION / TRANSFORMATION / VALIDATION / TARGET`

The motif must explain real sequence, relationships, or state. It is not decorative network art. Any illustrative data must be clearly labeled.

## Design dials

- ENERGY: 2 / Balanced
- RHYTHM: 3 / Bold
- MOTION: 1 / Calm

## Source of truth

The authoritative design direction is [`DESIGN.md`](../DESIGN.md). This brief records the approved decision. Production UI must follow `DESIGN.md` and pass the Anti Slop Delivery Gate.
