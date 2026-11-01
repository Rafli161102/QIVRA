---
name: qivra-graph-evidence
description: Build and review a deterministic repository knowledge graph from parsed source, build metadata, modules, and dependency relationships. Use when mapping cross-module or cross-repository architecture.
---

# Evidence-backed repository graph

## Node types

Use explicit typed nodes such as repository revision, module, source file, package, type, method, build target, dependency, plugin, version catalog alias, Maven parent, profile, and target runtime.

## Edge types and provenance

Every edge must carry a provenance class:

- \`EXTRACTED\`: a direct import, dependency declaration, module include, parent coordinate, or source declaration.
- \`RESOLVED\`: a deterministic relationship derived from local declarations, such as a safe relative Maven parent or a dependency version supplied by local dependencyManagement.
- \`INFERRED\`: a heuristic relationship such as likely framework usage or possible runtime call flow.

Each edge records evidence paths, optional line/AST location, resolver rule/version, and confidence only when the edge is inferred.

## Process

1. Build nodes from the pinned inventory.
2. Add extracted edges first.
3. Resolve only relationships covered by explicit rules and local evidence.
4. Detect cycles and produce deterministic topological ordering for acyclic internal module graphs.
5. Preserve unresolved and external edges as unresolved; do not invent a node's implementation.
6. Keep inferred edges visually and structurally distinct from extracted/resolved edges.
7. Make every graph query explain why a connection exists.

## Guardrails

- Do not create edges from names alone.
- Do not claim a method-call string is a resolved API symbol.
- Do not treat BOM import as a resolved dependency graph.
- Do not turn an AI-generated guess into an extracted edge.
- Do not add a vector database merely to make the graph queryable; prefer deterministic indexes and explicit graph traversal first.

## Verification

Test edge provenance, stable graph serialization, cycle reporting, unresolved references, and source-evidence path validity.
