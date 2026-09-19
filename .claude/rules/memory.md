# Memory rules (studio memory: lessons, product records, metrics)

## Where memory lives and who writes it
The plugin repo's `memory/`: `LESSONS.md`, one `products/<slug>.md` per shipped
product, the generated `products/INDEX.md`, `metrics.jsonl`. A product repo holds a
snapshot (`.claude/rules/lessons.md`) and `.planning/recall/RECALL.md`, never the
memory itself; the retro's `thread.md` log excerpt is local and never committed
(rules/logging.md). Only `scripts/memory.sh` writes `memory/`, only from the retro or
a reviewed plugin branch; nobody edits it by hand and `check-memory.sh` must pass.

## Lesson format and statuses
    ## L-NNNN — <title>
    Status: active|applied|retired
    Added: YYYY-MM-DD   From: <slug>:<X.Y.Z>   Reviewed by: code-auditor
    Applies: roles=<r,...|all>; types=<t,...|all>; stacks=<s,...|all>
    Do: <≤ 400 chars>
    Because: <one sentence>
    Evidence: <≥ 1 F-nn id and ≥ 1 .planning/ path>
    Applied to: none | <path>; <path>
    Recurred: none | <slug>:<F-nn>; ...
`active`: in the snapshot, roles apply its `Do:` line. `applied`: its text now lives in
an agent, rule, skill step or template (`Applied on:` set; every `Applied to:` path
carries `<!-- L-NNNN -->`). `retired` adds a `Retired:` line; nothing is ever deleted.

## Evidence and promotion
A lesson cites ≥ 1 finding id (`F-nn` from the retro's findings.md) and ≥ 1 `.planning/`
path; a proposal lacking either is rejected. A promotion is a lesson too: filed with its
own `L-NNNN` and a `Promote to:` target, it also becomes one line in an agent, rule,
skill step or template ending with `<!-- L-NNNN -->`; never a hook or a gate step.

## Governance
The research-analyst establishes the facts (findings) and, like every other role,
proposes only in its own domain; the code-auditor accepts, merges or rejects on a
different model and proposes nothing; the tech-lead applies promotions and the
code-auditor reviews that diff. No role reviews its own proposal, substitutes
included: the qa-engineer stands in on the auditor's own file, a third role that
authored neither stands in for it, and the recusal goes in the report. The producer
merges to the plugin's develop and queues a non-blocking `Q-<NNN>` (`Blocks: nothing`)
for each accepted item touching a gate step, money or taste; the founder releases.

## Snapshot, reports, triggers
`scripts/sync-rules.sh` writes `.claude/rules/lessons.md` (active lessons for the product
type and stack, cap 40 lessons / 12 KB). Every report carries `Lessons applied: L-NNNN, …
| none`. A retro runs after `/ergoz-soft:publish`, on `/ergoz-soft:approve reject concept`
(`retro pitch`), on demand and as `/ergoz-soft:retro --post-launch <X.Y.Z>`.
