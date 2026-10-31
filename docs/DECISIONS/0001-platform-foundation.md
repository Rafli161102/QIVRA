# ADR 0001: QIVRA platform foundation

- Status: Accepted for initial implementation; revisitable with evidence.
- Date: 2026-10-31

## Context

QIVRA needs to prove a narrow modernization workflow without prematurely coupling its product APIs, execution model, or commercial model to a single external tool.

## Decisions

1. Start with Java modernization for Maven and Gradle projects.
2. Use a monorepo while API, dashboard, contracts, and engine evolve together.
3. Keep the control plane separate from the code-execution plane.
4. Use TypeScript for initial product orchestration and Java 21 for the JVM-oriented analysis/transformation engine.
5. Treat OpenRewrite as a candidate adapter, not a foundational dependency, until exact artifacts and recipes pass licensing and technical review.
6. Make analysis explainable and transformations previewable; require explicit approval before applying changes.
7. Prefer a local analyzer vertical slice before hosted services and UI expansion.

## Consequences

- The initial scope is narrower than the long-term platform vision.
- The team must maintain contracts across TypeScript and Java.
- Worker isolation is a release blocker for hosted execution of untrusted repositories.
- Third-party license review becomes part of dependency onboarding.
- Multi-language support is deferred until the first adapter workflow is proven.
