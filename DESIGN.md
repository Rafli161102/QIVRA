# QIVRA Design Direction

> Status: Product-owner approved direction, locked 2026-11-01.
> Direction name: Engineering Atlas.
> Scope: public website direction and the visual foundation for future QIVRA product surfaces. This document defines design intent, not implemented functionality.

## Design Read

Reading this as: B2B software-modernization platform website for platform engineers, software architects, technical leads, and engineering decision-makers, in an industrial-editorial engineering-publication style, dial ENERGY 2 / RHYTHM 3 / MOTION 1.

## Product identity

QIVRA helps engineering teams inspect legacy software, understand its structure and risks, plan modernization, review controlled transformations, and validate the result. The initial implementation focus is Java projects using Maven and Gradle. The website must distinguish implemented capabilities from planned product direction at every point.

**Positioning:** a disciplined engineering instrument for software modernization.

**Current tagline:** Transform Legacy. Engineer the Future.

### Brand personality

- Investigative: traces how a system is assembled before proposing change.
- Precise: favors evidence, explicit scope, and clear technical language.
- Industrial: feels engineered and durable rather than decorative.
- Editorial: presents complex information with a strong typographic hierarchy.
- Accountable: makes review, validation, and limitations visible.

## Audience and primary task

### Primary audience

- Platform engineers and software architects responsible for legacy systems.
- Technical leads evaluating migration paths and engineering risk.

### Secondary audience

- Engineering managers and CTOs assessing modernization scope, governance, and delivery risk.

### First website task

Help a technical visitor understand what QIVRA analyzes, how a modernization workflow is controlled, what the initial Java/Maven/Gradle scope covers, and what is still planned. The website must not imply that planned features are already available.

## Visual language: Engineering Atlas

Combine the visual logic of technical field manuals, architectural drawings, industrial product documentation, and editorial engineering publications. Use these as broad visual references, not templates to reproduce.

The result should feel like a carefully typeset engineering atlas: a reader can inspect the system, follow its dependencies, and understand the proposed route from an existing codebase to a validated target.

Do not imitate the interface of a named SaaS competitor. Avoid familiar startup landing-page patterns when they do not explain QIVRA's actual work.

## Color system

The proposed palette is deliberately paper-led rather than dark-first. It separates editorial reading surfaces from technical evidence and uses one restrained signal accent.

| Token | Hex | Role |
|---|---|---|
| Archive Paper | `#EAE6DC` | Main page canvas and editorial surfaces |
| Carbon Ink | `#202522` | Primary text, strong rules, selected dark surfaces |
| Machine Grey | `#85877E` | Secondary information, diagram lines, metadata |
| Oxide Signal | `#B85C3B` | The single accent for important changes, warnings, and transformation milestones |

### Color rules

- Treat Archive Paper and Carbon Ink as the core pair.
- Use Machine Grey for secondary hierarchy, not for small low-contrast body text.
- Reserve Oxide Signal for moments that need attention. Do not turn every link, icon, border, and button into an accent.
- Do not introduce extra brand colors without a documented semantic role.
- Check every text and non-text pairing against WCAG AA before implementation. Do not claim compliance based on visual inspection.
- Do not force dark mode on the marketing website. A future engineering workspace may justify a separate dark theme based on actual usage needs.

## Typography

Use a strong editorial grotesk or humanist sans for display and reading text, paired with a legible technical monospace for identifiers, version labels, file paths, code references, and diagram annotations.

Typography should create hierarchy through scale, weight, measure, and spacing. Monospace is metadata, not the default face for paragraphs or oversized headlines.

Candidate typefaces must be checked for readability, language coverage, loading performance, and license before they are committed. No specific font family is locked by this document.

## Layout and composition

- Use asymmetric editorial compositions with a clear reading path.
- Let content determine section structure. Do not repeat the same centered heading and card grid throughout the page.
- Use whitespace as a structural tool and keep one clear focal point per screen.
- Technical annotations, indexes, rules, coordinates, and captions may support the engineering-publication identity when they clarify the content.
- Use dense layouts only for information that benefits from comparison or inspection. Keep explanatory prose comfortable to read.
- Avoid default bento mosaics, fake terminal windows, generic feature-card grids, fake customer-logo bars, and generic pricing templates.
- Every navigation item and control must lead somewhere real or be explicitly marked as unavailable.

## Signature motif: Migration Trace

Migration Trace is QIVRA's recurring visual identity motif. It represents the evidence-backed path through a modernization workflow:

`LEGACY SOURCE / INSPECTION / TRANSFORMATION / VALIDATION / TARGET`

Use a connected trace, dependency path, or annotated sequence to explain actual product concepts. The motif must communicate relationships, state, or sequence. It must not become an abstract decorative network.

When the analyzer exists, diagrams should be derived from real project metadata or clearly labeled examples. Before implementation exists, do not present invented scan results, codebase maps, benchmark figures, or screenshots as real product output.

## Motion and interaction

Motion dial is calm. Prefer clear state transitions, hover/focus feedback, and restrained changes that explain progress or state.

- No parallax, scroll-jacking, auto-advancing content, or motion used only to decorate empty space.
- Respect `prefers-reduced-motion`.
- Any persistent movement requires a pause control.
- Motion must never hide content or delay access to essential information.

## Accessibility and responsive behavior

- Meet WCAG 2.2 AA as the implementation target.
- Verify normal text contrast at 4.5:1 and large text at 3:1. Verify non-text controls and meaningful boundaries at 3:1.
- Keep semantic landmarks, one page-level H1, logical heading order, visible focus, and keyboard-operable controls.
- Give every control an accessible name and every form field a visible label.
- Support zoom to 200% without clipped content or horizontal page scrolling.
- Design mobile layouts intentionally. No horizontal overflow; controls should meet at least 44px touch targets.
- Test narrow mobile, tablet, and desktop widths, keyboard-only operation, reduced motion, and relevant loading, empty, and error states.

## Content integrity

- State clearly which capabilities are implemented, in progress, or planned.
- Do not invent customer names, testimonials, logos, adoption figures, performance claims, benchmark results, security certifications, or product screenshots.
- Do not describe a planned workflow as a current feature.
- Use real technical examples only when their source and status are clear. Label illustrative examples as examples.
- Prefer concrete descriptions of analysis, review, transformation, and validation over generic AI or innovation claims.

## Asset direction

The identity should rely first on typography, composition, and meaningful technical diagrams. Do not add stock 3D blobs, generic robot illustrations, generic abstract network art, or decorative imagery without a direct product purpose.

Creating a logo, illustration, icon family, or other bespoke visual asset requires a separate explicit product-owner request. No logo or mascot is defined by this document.

## Design dials

| Dial | Value | Interpretation |
|---|---:|---|
| ENERGY | 2 | Balanced presence. Strong editorial hierarchy without shouting. |
| RHYTHM | 3 | Deliberately varied compositions shaped by each content type. |
| MOTION | 1 | Calm interaction, with movement limited to useful feedback. |

## Design decision test

Before keeping a visual technique, state what it helps the visitor understand, compare, navigate, or decide. If it has no clear purpose, remove it.

The design should remain recognizably QIVRA even if the wordmark is temporarily hidden. Migration Trace, editorial typography, paper-led surfaces, and engineering annotations must work together to create that identity.

## Implementation status

This direction is approved. It does not mean that the logo, website, dashboard, design tokens, or UI components have been implemented. Each implementation must pass the Anti Slop Delivery Gate and the relevant UI, accessibility, responsive, and copywriting checklists.
