# QIVRA agent skills

QIVRA keeps project-scoped AI instructions in \`.agents/skills/<skill-name>/SKILL.md\`. Skills describe repeatable engineering workflows; they are not evidence that the corresponding product runtime feature exists.

The format follows the portable Agent Skills pattern: a small YAML front matter with \`name\` and \`description\`, followed by instructions. Supporting references and scripts may be added when needed. OpenAI documents this structure for reusable agent workflows, and Anthropic's public skills repository provides examples of technical and enterprise skill packaging.

## Skill inventory

| Skill | Use when | Primary output |
|---|---|---|
| \`qivra-using-agent-skills\` | Starting QIVRA engineering work | Selects the minimum applicable skill set |
| \`qivra-repository-intelligence\` | Mapping a repository or its modules | Deterministic inventory and evidence paths |
| \`qivra-build-model-analysis\` | Inspecting Maven/Gradle metadata | Static build-model observations and unresolved references |
| \`qivra-graph-evidence\` | Mapping module and dependency relationships | Provenance-labelled graph edges |
| \`qivra-modernization-assessment\` | Assessing a target upgrade such as Java 17 | Evidence-backed findings and ordered plan |
| \`qivra-safe-transformation-review\` | Reviewing or applying migration recipes | Approved diff, validation, and audit record |
| \`qivra-ai-provider-safety\` | Adding AI providers or generated explanations | Provider-neutral, opt-in, data-minimized AI design |
| \`qivra-test-and-release\` | Testing, CI, changelog, or release work | Verifiable CI and release evidence |

The six existing Anti Slop skills remain installed and authoritative for UI, accessibility, mobile layout, copywriting, and code-comment hygiene.

## Selection principles

- Use the smallest set of skills that covers the task.
- Load the router first, then the task-specific skill.
- Prefer executable tests and source evidence over generic instructions.
- Treat skill text as agent guidance, not a security boundary. Runtime authorization and validation must be enforced in code.
- Review skill changes like code: check scope, examples, links, referenced files, and verification steps.

## External references

Workflow structure is informed by [addyosmani/agent-skills](https://github.com/addyosmani/agent-skills) (MIT), skill packaging examples in [anthropics/skills](https://github.com/anthropics/skills), and the extracted-versus-inferred relationship model described by [Graphify](https://github.com/Graphify-Labs/graphify). QIVRA's skill files are newly authored; external skill files or implementation code are not copied.

See [Repository reuse audit](REPOSITORY_REUSE_AUDIT.md) for project scan results and reuse decisions.
