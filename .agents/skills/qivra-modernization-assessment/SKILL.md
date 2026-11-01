---
name: qivra-modernization-assessment
description: Assess repository compatibility and migration risks against an explicit QIVRA target profile, initially Java 17. Use after static repository and build-model discovery.
---

# Modernization assessment

## Preconditions

- A pinned repository snapshot and analyzer version exist.
- The relevant source/build inventory is complete enough to assess.
- A target profile is explicitly selected. Do not assume Java 17 if the user or project configuration says otherwise.

## Process

1. Load the target profile and its versioned compatibility rules.
2. Match findings against direct evidence: source AST nodes, dependency coordinates, plugin versions, build settings, framework declarations, and test/build configuration.
3. Classify each finding as blocker, risk, prerequisite, opportunity, or informational.
4. Record source evidence, rule identifier/version, affected module, observed value, target requirement, and suggested next action.
5. Distinguish confirmed incompatibilities from unknowns and heuristics.
6. Do not infer runtime usage from an import or a method-call name alone.
7. Estimate impact only when there is an explicit, explainable basis.
8. Produce an ordered plan with dependencies and human review points.

## Finding contract

A finding should contain:
- stable ID;
- rule ID and rule-set version;
- severity and confidence;
- observed and expected values;
- evidence file and line/AST location when available;
- explanation and known limitations;
- whether the result is extracted, resolved, or inferred.

## Guardrails

- Never invent compatibility issues, statistics, test results, or migration effort.
- Do not present a parser success as a successful compile.
- Do not present an import as proof of a reachable call path.
- Keep uncertain results explicitly marked.
- AI may summarize evidence but must not create or upgrade a finding without a deterministic rule or human confirmation.

## Verification

Test positive, negative, ambiguous, and unsupported cases against versioned fixtures. Ensure changing a rule-set version changes the assessment provenance.
