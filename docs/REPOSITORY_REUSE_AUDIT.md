# Repository reuse and AI-skill audit

## Scope and method

A GitHub repository search for the linked account returned 20 repository results. We ran targeted repository-tree and code searches for \`SKILL.md\`, \`AGENTS.md\`, AI provider abstractions, prompt generation, compiler/parser pipelines, module graphs, and language identification. Detailed source review focused on projects with direct relevance to QIVRA. This is a targeted reuse audit, not a full security or code-quality audit of every repository.

## Internal projects reviewed

| Repository | Relevant material found | QIVRA decision |
|---|---|---|
| [QIVRA](https://github.com/Rafli161102/QIVRA) | Six existing Anti Slop skills, agent instructions, JavaParser/OpenRewrite analyzer | Keep as the product's quality and static-analysis foundation; add QIVRA-specific engineering skills |
| [CV-MRR](https://github.com/Rafli161102/CV-MRR) | Root \`SKILL.md\` for a personal portfolio redesign, plus Next.js/SEO toolkit code | Keep its skill scoped to CV-MRR. Its portfolio visual direction and Supabase/data-contract restrictions do not apply to QIVRA |
| [community-forge](https://github.com/Rafli161102/community-forge) | AI provider interface/factory, Gemini and OpenAI-compatible adapters, deterministic template fallback, provider fallback memory | Reuse the architecture pattern only. Do not copy provider code unchanged |
| [seira](https://github.com/Rafli161102/seira) | Typed compiler stages, diagnostics, deterministic module graph/topological ordering, backend contract | Use architectural ideas for typed diagnostics and graph ordering; do not couple QIVRA to Seira's language compiler |
| [wibuscript](https://github.com/Rafli161102/wibuscript) | AST/compiler pipeline, interpreter/transpiler separation, test fixtures, CI/release workflows | Reference for parser adapters and fixture-driven validation; not a Java modernization dependency |
| [linguist](https://github.com/Rafli161102/linguist) | Language classification, grammar metadata, and \`AGENTS.md\` | Future polyglot discovery adapter; do not add the Ruby Linguist runtime to the Java-first analyzer |
| [eldaya](https://github.com/Rafli161102/eldaya) | Corporate website and deployment docs | No direct analyzer or agent-skill reuse identified |

Other account repositories included AquaNime website variants, Valve-related prototypes, \`awesome-esolangs\`, \`loker\`, \`Surat-PT-AKI\`, and the profile repository. No direct source-analysis component was selected from them for this milestone.

## AI skill findings

QIVRA already contains the Anti Slop core plus five supplemental skills: UI, copywriting, human/accessibility, mobile layout, and code comments. They remain installed with upstream MIT notices.

CV-MRR's \`SKILL.md\` is a project-specific redesign brief. It is not a reusable general engineering skill and is not copied into QIVRA.

### Community Forge provider pattern

The useful pattern is a provider interface, one factory, isolated provider adapters, and a deterministic template fallback when no key is configured.

The inspected implementation is not copied as-is. It includes behaviors QIVRA must improve before production use: Gemini credentials in a query parameter, raw provider error-body logging, JSON parsing without schema validation, and no visible request timeout. QIVRA therefore requires AI disabled by default, explicit operator opt-in, authorization-header secrets, bounded requests, redaction, schema validation, and no customer source upload by default.

### Seira, WibuScript, and Linguist patterns

- Seira's deterministic module ordering and diagnostic structure inform QIVRA's future graph and finding contracts.
- WibuScript's staged compiler pipeline informs the separation of discovery, parse, analysis, and later transformation adapters.
- Linguist is a possible future language-detection source, but language labels remain hints, not proof of build participation.

## External agent-skill references

| Project | Useful idea | Reuse decision |
|---|---|---|
| [OpenAI Agent Skills documentation](https://developers.openai.com/api/docs/guides/tools-skills) | Portable \`SKILL.md\` directory with optional resources | Follow the format; no code copied |
| [Anthropic skills](https://github.com/anthropics/skills) | Focused, self-contained skills | Reference only; no skill files vendored |
| [Addy Osmani agent-skills](https://github.com/addyosmani/agent-skills) | Define → plan → build → verify → review → ship lifecycle | Use workflow pattern; QIVRA skills are original |
| [Graphify](https://github.com/Graphify-Labs/graphify) | Deterministic AST graph and extracted/inferred edge distinction | Use provenance principle; no implementation copied |

External project pages and licenses must be rechecked before future vendoring. This milestone copies no external AI skill implementation code.

## Combined QIVRA architecture

1. **Discovery:** Python scanner and future language adapters identify files, modules, and build systems.
2. **Parsing:** JavaParser handles Java AST; OpenRewrite parses Gradle/XML syntax; Tomlj parses Gradle version catalogs.
3. **Build-model evidence:** static Maven workspace-parent/property/dependency-management analysis and literal Gradle catalog resolution.
4. **Graph:** Seira-inspired deterministic ordering plus Graphify-inspired provenance, without conflating extracted facts and inferred hypotheses.
5. **Assessment:** QIVRA-specific skills and deterministic rules compare evidence against a selected target profile.
6. **Optional AI assistance:** Community Forge-inspired provider abstraction, disabled by default and restricted to minimized, redacted structured summaries.
7. **Transformation:** separately reviewed recipes, preview diffs, and human approval.
8. **Validation:** isolated build/test workers; static analysis never executes customer build scripts.

## Explicit non-goals

- Do not merge unrelated project source trees into QIVRA.
- Do not import the CV-MRR visual skill into QIVRA.
- Do not add an AI provider to the static analyzer simply because another project has one.
- Do not send source files to an LLM by default.
- Do not treat graph inference or model-generated explanations as source evidence.
- Do not introduce Maven/Gradle execution into static discovery.
