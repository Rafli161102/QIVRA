---
name: qivra-build-model-analysis
description: Inspect Maven and Gradle build metadata, Maven parent and dependency-management declarations, Gradle version catalogs, plugins, and profiles without executing build logic. Use for P0.2 analyzer work.
---

# Build model analysis

## Process

1. Discover POMs, Gradle Groovy/Kotlin settings/build scripts, and TOML version catalogs.
2. Parse syntax only. Never invoke Maven/Gradle wrappers, Tooling API, plugins, lifecycle goals, or project tasks.
3. For Maven, resolve only a safe workspace-relative \`parent.relativePath\`. Check path containment, symlink boundaries, parent coordinates, cycles, and recursion depth.
4. Merge only the statically visible parent properties and dependency-management declarations. Preserve provenance for inherited versions.
5. Record Maven profiles and their activation rules, but do not activate profiles or claim an effective POM.
6. Detect BOM imports. Do not resolve remote BOMs or transitive dependencies in the static pass.
7. For Gradle version catalogs, inventory versions, library aliases, plugin aliases, bundles, and literal \`version.ref\` references.
8. Leave computed Gradle expressions, provider values, convention plugins, init scripts, and dynamic catalog construction unresolved.
9. Report unresolved property/catalog references explicitly.

## Maven model boundaries

The result is a partial static model, not Maven's effective model. It does not implement parent repositories, profile activation, environment/system property interpolation, remote BOM imports, dependency mediation, plugin resolution, or transitive dependency resolution.

## Gradle boundaries

A parsed build script or catalog does not prove Gradle configuration succeeds. Literal alias resolution does not imply that an alias is used by a build script.

## Security

- Disable external XML entities, DTDs, schemas, and XInclude.
- Resolve local parents only within the inspected workspace.
- Never access network repositories during static discovery.
- Avoid serializing arbitrary POM properties because they may contain credentials or internal values. Report property names and only the specific resolved values needed for dependency evidence.

## Verification

Test:
- standard and custom version catalog filenames;
- version strings, rich versions, references, plugins, and bundles;
- malformed TOML;
- Maven inherited group/version/properties;
- dependencyManagement inheritance and BOM imports;
- profile declarations without activation;
- missing, mismatched, cyclic, too-deep, oversized, and out-of-workspace parents;
- XML external-entity payloads.
