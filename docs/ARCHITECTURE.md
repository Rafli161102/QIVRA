# Architecture

## Status and decision boundary

This document describes the intended architecture. It is not an implementation inventory. Components should be added incrementally when a milestone needs them.

## Design goals

- Separate product control plane from untrusted code execution.
- Keep transformation engines replaceable behind adapters.
- Make every finding traceable to source evidence.
- Keep plans and diffs reviewable before mutation.
- Make jobs reproducible through pinned engine, recipe, and policy versions.
- Avoid coupling product APIs to one transformation provider.
- Support local assessment first, then controlled hosted execution.

## Logical system

```text
[Dashboard / CLI / SDK]
          |
          v
     [Public API]
          |
          v
 [Control Plane / Orchestrator] ---- [PostgreSQL]
          |                               |
          +---- [Job Queue]               +---- users, projects,
                    |                           jobs, policies,
                    v                           audit metadata
            [Worker Manager]
                    |
           short-lived job boundary
                    |
                    v
          [Isolated Worker]
             |          |
             v          v
        [Analyzer]  [Transform Adapter]
             |          |
             +-----+----+
                   |
                   v
             [Validator]
                   |
                   v
          [Artifact / Report Store]
```

The diagram is conceptual. No production deployment topology is committed yet. P0.2 currently has a Python standard-library metadata scanner and a separate Java 21 hybrid parser module: JavaParser supplies Java AST signals, while OpenRewrite's Gradle and XML parsers inspect Gradle scripts and POM syntax. These parsers do not execute customer build scripts or perform dependency resolution; they are not the transformation engine.

## Control plane

The API and orchestrator own authentication, authorization, project metadata, job state, policy evaluation, scheduling, and audit events. They must not execute user-provided build scripts or transformations in-process.

Initial implementation direction: TypeScript. API contracts should be versioned and shared through a small contracts package.

## Execution plane

Workers perform repository checkout, static inspection, transformations, and build/test validation. A worker receives only the job-scoped credentials and inputs it requires.

Initial implementation direction: a Java 21 engine process, with a Docker-compatible isolated worker boundary. The current Java 21 analyzer module is a local prototype only; it does not yet implement transformations or isolated worker execution. The isolation design must be validated on the target deployment platform before accepting untrusted workloads.

## Engine adapter boundary

The analyzer and transformation engine should communicate through explicit request and result contracts. An adapter should expose:

- engine name and version;
- supported languages and project types;
- capabilities and recipe identifiers;
- configuration schema;
- analysis findings with file and location evidence;
- transformation preview and changed-file list;
- diagnostics and structured execution result.

OpenRewrite is a candidate Java transformation engine, not a hard dependency at this stage. Licensing, artifact provenance, API stability, and recipe-level behavior must be reviewed before integration. See [LICENSING.md](LICENSING.md).

## Core data concepts

- **Project:** repository identity, owner, default branch, and configured policy.
- **Snapshot:** immutable source revision inspected by a job.
- **Inventory:** discovered modules, languages, build systems, dependencies, and framework signals.
- **Finding:** a source-backed observation with severity, confidence, and evidence.
- **Assessment:** comparison of an inventory to a target modernization profile.
- **Plan:** ordered steps, prerequisites, risks, and approval state.
- **Recipe:** versioned transformation definition and configuration.
- **Job:** one isolated analysis, transformation, or validation execution.
- **Artifact:** diff, log, report, test result, or generated bundle.
- **Audit event:** append-only record of actor, action, timestamp, and target.

## Job lifecycle

```text
QUEUED -> PREPARING -> ANALYZING -> ASSESSING
                         |
                         v
                  PLAN_READY
                         |
                  user approval
                         v
                    APPLYING
                         |
                         v
                   VALIDATING
                    /       \
              SUCCEEDED   FAILED
```

Cancellation, timeout, policy rejection, and infrastructure failure must be modeled as explicit terminal or recoverable states. Transformation success and validation success are separate fields.

## Persistence and coordination

- PostgreSQL is the initial source of truth for users, projects, policies, job state, and audit metadata.
- A queue such as Redis-backed workers may coordinate asynchronous jobs; queue contents are not the durable audit record.
- Large source snapshots and artifacts belong in object storage, not relational database rows.
- Retention, encryption, tenant isolation, and deletion semantics must be specified before customer source is stored.

## Initial technology choices

| Concern | Candidate | Reason / constraint |
|---|---|---|
| Dashboard | Next.js + TypeScript | Mature web application ecosystem |
| API / orchestration | TypeScript | Shared contracts and fast product iteration |
| Engine | Java 21 | JVM ecosystem for Java parsing and migration tools |
| Database | PostgreSQL | Relational job and project metadata |
| Queue | Redis-compatible | Asynchronous job coordination |
| Worker isolation | Container runtime | Disposable execution boundary; hardening required |
| Static Java AST | JavaParser 3.28.2 | Integrated parser dependency; Apache-2.0 option selected |
| Build syntax parser | OpenRewrite Gradle/XML 8.96.3 | Integrated parser dependencies; Apache-2.0 selected modules only |
| Transformation | OpenRewrite recipe adapter | Deferred; recipe and dependency license review required |

Technology choices may change through documented architecture decisions.

## Local-first development sequence

1. Implement analyzer contracts and fixtures without network or hosted services.
2. Build a deterministic Maven/Gradle inventory engine.
3. Add assessment and plan generation from explicit target profiles.
4. Integrate one licensed and pinned transformation path.
5. Add a local disposable worker runner and validation.
6. Add API orchestration and persistence only after the engine workflow is testable.
7. Add dashboard and Git provider integration once contracts stabilize.
