# Studio rules (apply to every product and every role)

## Mission
ergoz-soft builds small, polished software products and ships them:
1. Puzzle games: invented in-house, or new games that evolve a mechanic
   found in a classic DOS game or a free-licensed open-source game. Faithful
   remakes only when rules/ip-policy.md gives a clean verdict (original
   unprotected, name not a live trademark); otherwise inspiration only.
2. Android apps, iOS apps, Kotlin Multiplatform apps.
3. Web apps and websites.

## Product baseline
Every product works without an account unless the product is about
accounts, launches in under 2 seconds on a mid-range phone (or has LCP ≤ 2.5 s
on the web), has deterministic and testable core logic, meets the
accessibility rules of its stack, and passes the engineering, testing, and
release rules before it ships.

## Three standing rules
1. No role approves its own work. Reviewers receive the requirements and the
   diff, never the author's reasoning or chat, or the studio log for the task
   under review.
2. Every role writes a report (rules/reporting.md) before saying "done".
3. Only the producer talks to the founder, and only at the two gates or when
   an escalation cannot be resolved by staff.

## The founder never blocks the studio
Founder questions go to `.planning/questions/FOUNDER.md` (format in
rules/reporting.md). Work that does not depend on the answer continues.
Only two things wait: building beyond a prototype waits for
`.planning/approvals/concept.approved`; upload or production deploy waits for
`.planning/approvals/publish-<VERSION>.approved` (VERSION = root `VERSION`).
Merges to develop and main, and non-copyleft dependency additions, are
autonomous.

## Parallel work
Independent work runs concurrently: one git worktree per concurrent feature
branch, parallel agent dispatch, several products at once. Merges to develop
stay serial and reviewed.

## Fresh context per step
Every pipeline step is executed by newly spawned agents. An agent receives
its inputs as files (docs, diffs, reports, the task text) and never the
transcript or reasoning of an earlier step or of another role. When a step is
complete — outputs written, report written, STATUS.md updated — the agents
that did it are discarded; the next step spawns fresh ones. An agent is
resumed only inside its own step's review loop (the same task, the same
report file). Nothing an agent "remembers" is authoritative: only files in
the repo are.

## Studio log
Hooks write a local record of every dispatch, reply, command and file write
to `.planning/log/` (rules/logging.md). It is a record, not an input: no role
reads it to learn what another role did; the founder watches it live and the
retrospective reads it for a closed version. It is git-ignored, never committed.

## Definition of done for any task
- The repo's Check command (CLAUDE.md) passes locally.
- Tests exist for new logic; no test was disabled or ignored.
- No new `@Suppress`, baseline, `@Ignore`, `eslint-disable`, `// swiftlint:disable`,
  or skipped test without an "approved by code-auditor" line in the PR text.
- Work is on a feature branch, reviewed by a different role, report written.
- "AI-generated code is never accepted because it compiles."

## Agent definition skeleton
Every agent file has exactly these H2 sections in order: ROLE, AUTHORITY,
INPUTS, REQUIRED SKILLS, PROCEDURE, REQUIRED OUTPUTS, QUALITY GATES,
ESCALATION, PROHIBITED, DEFINITION OF DONE. No personas.

## Hiring
A new field (e.g. backend-dev, flutter-dev) is: a new agent file under the
skeleton, entries in required-skills.json, a README staff row, a line in
tests/test_agents.sh, and a stack row in rules/stacks.md if it brings a
stack. Proposed by the research-analyst or tech-lead when a product needs
it; done on a plugin feature branch.

## Question routing
gameplay/scope (games) → game-designer; scope/requirements (apps, web) →
product-manager; screens/UI/accessibility → ux-designer; architecture/stack/
code → tech-lead; tests/acceptance → qa-engineer; versioning/branching/store/
deploy → release-manager; licensing/prior art/competitors/skills →
research-analyst; the log and the studio memory → producer; taste, money, or
a gate → founder (queued, never blocking).
