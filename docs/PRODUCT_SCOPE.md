# Product Scope

## Product definition

QIVRA is a software modernization platform. It helps engineering teams understand existing software, evaluate modernization options, apply controlled code transformations, and verify resulting behavior.

QIVRA is not positioned as a generic code-generation chatbot, a one-click black-box converter, or a replacement for engineering review.

## Users and jobs to be done

| User | Primary job |
|---|---|
| Individual developer | Understand an unfamiliar codebase and perform a safe migration |
| Software agency | Assess client systems, estimate modernization work, and produce evidence-based reports |
| Engineering team | Coordinate repeatable migrations across repositories |
| Enterprise platform team | Govern modernization policies, execution, access, and audit records |

## Initial problem and wedge

Start with Java applications built using Maven or Gradle. The initial workflow is intentionally narrow:

1. Inspect a local or Git-hosted project.
2. Detect modules, build files, configured Java versions, dependency declarations, and framework indicators.
3. Produce a machine-readable inventory with source locations and confidence levels.
4. Compare the inventory with a selected target profile.
5. Produce an explainable assessment and ordered migration plan.
6. Preview each proposed change and require explicit approval before application.
7. Run configured validation and report both successes and failures.

Initial target profile: Java 17 modernization from Java 8 or Java 11. Actual compatibility recommendations must be evidence-based and must not be inferred from a version string alone.

## Product pillars

### 1. Understand

- Project and module discovery.
- Build-tool and runtime detection.
- Dependency and plugin inventory.
- Framework and API usage signals.
- Architecture and dependency relationship summaries.

### 2. Plan

- Source-backed findings.
- Compatibility and upgrade checks.
- Risk classification with rationale.
- Ordered migration steps and prerequisites.
- Human-editable plan and policy.

### 3. Transform

- Explicit, versioned recipes.
- Previewable unified diffs.
- Per-recipe configuration and scope.
- Idempotency checks where applicable.
- No automatic apply without user approval.

### 4. Verify

- Before/after build and test execution.
- Structured diagnostics.
- Validation status distinct from transformation status.
- Reproducible execution metadata.

### 5. Govern

- Project and job permissions.
- Audit events and artifact history.
- Dependency and recipe provenance.
- Retention and deletion policies.
- Exportable assessment and migration reports.

## Non-goals for the first release

- Supporting every programming language at launch.
- Blindly translating an entire codebase between unrelated languages.
- Guaranteeing semantic equivalence for arbitrary transformations.
- Automatically deploying migrated applications to production.
- Executing customer build scripts on shared application hosts.
- Sending customer source code to an external AI provider by default.
- Claiming compatibility based only on a superficial text scan.

## Success measures

The first useful release should be measured by:

- precision and recall of Maven/Gradle project discovery;
- correctness of Java and dependency inventory against fixture repositories;
- proportion of findings with source evidence;
- migration plans that are actionable and reviewable;
- transformation idempotency and diff correctness;
- percentage of jobs with complete, reproducible validation results;
- time to produce an assessment compared with a manual baseline.

Numeric targets should be set after a representative fixture corpus exists. Do not invent performance or accuracy claims before measurement.

## Commercial model hypotheses

Possible offers include a free local assessment, paid team workflows, enterprise deployment and policy controls, and professional migration services. Pricing, packaging, and willingness to pay remain hypotheses until customer discovery and paid pilots validate them.
