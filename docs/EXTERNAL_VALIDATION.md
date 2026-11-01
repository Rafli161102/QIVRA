# P0.2 external repository validation

**Validation mode:** pinned, selective, read-only snapshots  
**Upstream revisions:** immutable Git commit SHAs recorded in [the fixture manifest](../engine/analyzer/fixtures/external-validation/README.md)

## Coverage

| Repository | Analyzer path | Representative inputs |
|---|---|---|
| Spring Boot | Gradle Groovy + Java source | `settings.gradle`, `build.gradle`, `SpringApplication.java` |
| Gradle | Gradle Kotlin/Groovy + version catalogs | Root settings/build logic, precompiled-script candidate, official Groovy/Kotlin catalog snippets |
| Apache Maven | Maven multi-module + Java source | Root/API/implementation POMs and `MavenProject.java` |
| Apache Commons Lang | Maven + Java source | Root POM and `StringUtils.java` |

## CI assertions

The external snapshot portion of `scripts/check_hybrid_inventory.py` verifies:

- Spring Boot's Groovy settings/build files parse, the literal root project name and includeBuild/project declarations are inventoried, and the Java source is parsed.
- Gradle's Kotlin settings/build files and build-logic-commons script are discovered; path-derived convention plugin identity is marked as inferred.
- Gradle's official Groovy/Kotlin catalog samples parse, plugin/library aliases link to the same pinned catalog snapshot, and `libs.bundles.groovy` retains its three literal member aliases.
- Apache Maven's selected child POMs resolve their local parent chain from the checked-in snapshot. The external Maven parent of the root project remains unresolved, as no network lookup is attempted.
- Commons Lang's POM and Java source are parsed, while its external parent remains outside the workspace model.
- Java method calls keep source file/line evidence; JDK/workspace-resolved identities and unresolved external references remain distinct.

## Interpretation and limits

This is a repeatable compatibility sample, not a full-repository certification. The snapshots intentionally omit most modules and source files from the upstream repositories. Therefore:

- Counts and graph edges describe only the selected snapshot files.
- The analyzer does not reproduce the upstream Gradle/Maven effective model.
- Missing external parents, dependency JARs, plugins, and Java types are expected to remain unresolved.
- No build success, test success, runtime compatibility, or migration safety conclusion is inferred from static parsing.
- Repository commits and license notices are pinned for reproducibility; refresh the snapshots and their hashes deliberately when updating this validation corpus.
