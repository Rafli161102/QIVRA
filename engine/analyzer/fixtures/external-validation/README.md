# External repository validation snapshots

These are deliberately small, read-only source/configuration snapshots pinned to immutable upstream commits. They are analyzer test inputs, not product dependencies or complete repository clones.

No Gradle/Maven wrapper, build, task, plugin, test suite, or application code from these upstream repositories is executed by QIVRA CI. The fixture runner only parses the checked-in text snapshots.

## Sources

| Upstream repository | Pinned commit | License | Selected coverage |
|---|---|---|---|
| [spring-projects/spring-boot](https://github.com/spring-projects/spring-boot) | `bcd4641c9b36fb377712fe26c3f1d3472db41e1e` | Apache-2.0 | Groovy `settings.gradle` and `build.gradle`, included builds/projects, plugin declarations, SpringApplication Java source |
| [gradle/gradle](https://github.com/gradle/gradle) | `252861a9efe84b0e79dd0ffcb72ee6ebd555fa00` | Apache-2.0 | Kotlin settings/build scripts, build-logic-commons precompiled plugin candidate, official Groovy/Kotlin version-catalog examples and bundle usage |
| [apache/maven](https://github.com/apache/maven) | `349d0d0b0a2657d41be5ec5cfa4846850af97962` | Apache-2.0 | Multi-module root/API/implementation POMs, local parent relationships, dependency management, MavenProject Java source |
| [apache/commons-lang](https://github.com/apache/commons-lang) | `b8726053b685e80ea27f10a09fbb53f6022be46e` | Apache-2.0 | Maven POM with a remote parent declaration and StringUtils Java source |

## Snapshot policy

- Source files retain their upstream copyright/license headers.
- The corresponding upstream LICENSE files are stored under `licenses/`; Apache Maven and Commons Lang NOTICE files are retained too.
- Only the listed files are copied. A passing fixture check is not a claim that QIVRA has analyzed every file in an upstream repository.
- The scanner's workspace-only model remains explicit: it does not download remote Maven parents/BOMs, Gradle plugin classpaths, or external dependency JARs.
- Any unresolved API/build reference in these snapshots is a limitation signal, not proof that the upstream project is broken.

See [the external validation report](../../../../docs/EXTERNAL_VALIDATION.md) for test intent and boundaries.
