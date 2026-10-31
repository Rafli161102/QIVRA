# QIVRA agent instructions

<!-- antislop:start -->
## antislop

For UI, copy, people, mobile layout, or code comments work, read these installed skill files directly (use these paths even if a same-named global skill exists):
- Core filter, always on: `antislop`: `.agents/skills/antislop/SKILL.md`
- UI / visual: `antislop-ui`: `.agents/skills/antislop-ui/SKILL.md`
- Copy & text: `antislop-copywriting`: `.agents/skills/antislop-copywriting/SKILL.md`
- People / accessibility: `antislop-human`: `.agents/skills/antislop-human/SKILL.md`
- Mobile / responsive: `antislop-layoutmobile`: `.agents/skills/antislop-layoutmobile/SKILL.md`
- Code comments: `antislop-code`: `.agents/skills/antislop-code/SKILL.md`

Before starting, follow the core's "Two Usage Modes" section in strict order: explicit session instruction first, then global preference, then ask. A session instruction always wins. For a resolved mode, say `antislop active: <mode> (session override).` or `antislop active: <mode> (global preference).` once before presenting findings or making edits, using the actual mode and source. Only an explicit choice of antislop during or after selects a session mode.

If the mode is unresolved, ask DURING or AFTER and end the response; wait for the answer before any UI review, planning, concept, or edits. For read-only tasks, put the active-mode notice only at the start of the final answer. For editing tasks, announce before the first edit and omit the notice from the final answer. Additional antislop skills reuse the resolved mode without another question or notice.

For UI work, read the product-owner-authored `DESIGN.md` before choosing visual direction. If it does not exist or is incomplete, ask the product owner to supply direction; do not invent brand direction or fill it with generic AI taste. A blank brief lives at `docs/WEBSITE-DESIGN-BRIEF.md`. The core's Delivery Gate is mandatory before UI work is presented as complete.

To update the project-local skills from the official installer, run `npx antislop-ai --update` from the QIVRA repository root (Node.js 18+). Review the resulting changes and license notices before committing. Start a new agent session after installation or update.
<!-- antislop:end -->
