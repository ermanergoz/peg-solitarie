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

## Four standing rules
1. No role approves its own work. Reviewers receive the requirements and the
   diff, never the author's reasoning or chat, or the studio log for the task
   under review.
2. Every role writes a report (rules/reporting.md) before saying "done".
3. Only the producer talks to the founder, and only at the two gates or when
   an escalation cannot be resolved by staff.
4. No one oversteps. Every role does only the work its ROLE section names and
   hands anything else to the producer through its report. The producer
   dispatches, reads reports and merges; it never implements, tests, runs a
   device or an emulator, or reviews work itself. A reviewer never fixes what
   it reviews; a developer never signs off its own work. The urge to "just do
   it" is the signal to stop and report.

## Hierarchy and communication
- Only the producer addresses the founder: PushNotification, AskUserQuestion
  and FOUNDER.md entries are its alone; a role that needs the founder drafts
  the question in its report and the producer queues it.
- Roles report to the producer through their report files and the files the
  dispatch names, and never message one another: cross-role work is
  dispatched by the producer. Only the producer passes
  `subagent_type: ergoz-soft:<role>`; any other role's Agent calls are helpers
  (Explore, general-purpose) that never name a studio role (rules/dispatch.md
  in the plugin).

## GSD and the studio
GSD is the execution engine. ergoz-soft is the organization and governance
layer. GSD owns decomposition, context management, phase planning mechanics,
execution waves, and phase bookkeeping. ergoz-soft owns product decisions,
domain expertise, technical standards, specialist staffing, independent review,
release policy, institutional memory, and founder gates. ergoz-soft must not
repeat work already completed adequately by GSD unless independent
verification is intentionally required.
- Adequately: GSD's own gate passed — plan-checker PASS for a plan; the Check
  command green and gsd-verifier PASS for executed code.
- Intentionally required, the whole list: the code-auditor's review on a model
  superior to the author's; the qa-engineer's hands-on session on a device; the
  tech-lead's sign-off before a merge to develop; the release and publish
  gates. Nothing else re-checks GSD's work.
- Our specialists staff GSD's waves (rules/stacks.md); an unrouted plan runs
  on gsd-executor staffed by `.planning/config.json`.
- Our standards win on conflict — gitflow, commits and their trailer, the
  Check command, the report rules; GSD's guidance applies inside its own scope.
- Every question GSD asks goes to the role that owns its domain (Question
  routing); only the two gates reach the founder.
- QA checks every step that yields a runnable build — the scaffold, every
  executed plan's acceptance path, every phase before its merge, every
  release — on a real device when one is attached, else the emulator; the
  browser for web.

## Model and effort
Every role pins `model:` and `effort:` in its agent file. Effort is fixed per
role in that file; a dispatch cannot change it. The producer's pin (fable,
high) is advisory: the producer is the founder's own session, which no agent
file can pin. Every other fable role runs at xhigh: at max they spent whole
64k-token responses thinking and wrote nothing; xhigh keeps the depth and
bounds that failure. Developers run on opus at max. The release-manager runs
on opus at medium. A chore runs as a script first, else on a small model.

**Code is reviewed on a model superior to its author's: fable > opus > sonnet >
haiku** (standing rule 1). Code is written only on opus: the developers, and
gsd's executor, code-fixer and debugger. It is reviewed on fable: the
code-auditor, the qa-engineer and gsd's verifier. No product code is written on
fable, because nothing above fable could review it. A fable-written plan, spec
or lesson is checked by a fresh fable agent that did not write it. There is no
fallback. A review that cannot get fable does not happen, and no other fable
step moves to another model either: whether fable is refused before the step
starts or a rate limit cuts it off mid-run, the step fails. The producer reports
it as not done with fable named as the reason, queues a founder question — wait
for fable, or change the policy — and keeps every other task moving.

## The founder never blocks the studio
Founder questions go to `.planning/questions/FOUNDER.md` (format in
rules/reporting.md). Work that does not depend on the answer continues.
Only two things wait: building beyond a prototype waits for
`.planning/approvals/concept.approved`; upload or production deploy waits for
`.planning/approvals/publish-<VERSION>.approved` (VERSION = root `VERSION`);
the stop above ten roles (rules/dispatch.md in the plugin) is an ordinary
question in this queue, not a third gate.
Merges to develop and main, and non-copyleft dependency additions, are
autonomous.

## Parallel work
Independent work runs concurrently: one git worktree per concurrent feature
branch, parallel agent dispatch, several products at once. A product's phases
run one at a time: gsd's executor cannot commit inside a worktree it did not
create. Merges to develop stay serial and reviewed.

Parallel dispatch is several Agent calls in ONE message, each with
`run_in_background: false`, so every report comes back inline in that same
turn. Never set `run_in_background: true` on a dispatch whose report this step
needs: a background launch answers with an acknowledgement, and an
acknowledgement is not a result. A role never ends its turn while a role it
dispatched is still running — in an autonomous session (`claude -p …`) the turn
ending is the session ending, and every agent still running dies with it,
unreported and uncommitted.

If an acknowledgement comes back anyway, wait for it. A completion
notification for every outstanding role ends the wait early; otherwise poll —
never by reading the agent's output file, always by polling the exact report
paths you named as required outputs in each dispatch: with the worktree prefix
when that dispatch gave the role a worktree, and one path per dispatch, since
the same role dispatched twice writes two files (`qa-engineer-fuse.md`,
`qa-engineer-march.md`). Run the poll as one Bash call with an explicit
`timeout` of 600000; at the tool's default the call is moved to the background
after two minutes and dies with the turn like any other background work.

    reports="../repo-p02/.planning/reports/phase-02/qa-engineer.md ..."  # as dispatched
    for i in $(seq 1 45); do                     # 45 checks, 44 sleeps ≈ 7.3 min
      missing=""
      for p in $reports; do
        grep -q '^## Checks' "$p" 2>/dev/null && grep -q 'Lessons applied:' "$p" || missing="$missing $p"
      done
      [ -n "$missing" ] || break
      [ "$i" -lt 45 ] && sleep 10
    done
    [ -z "$missing" ] && echo "all reports in" || echo "still missing:$missing"

A half-written report does not end the wait: the test is a Checks section
and a `Lessons applied:` line. Neither does a report from a previous round —
before re-dispatching a role inside a review loop, rename that round's report
to `<role>-loop<N>.md`, which is that round's record, or give the new dispatch
its own path, so nothing already on disk can satisfy this round's wait. Re-run
the poll while it still prints "still missing", at most eight times (one hour
in all). At that bound the step failed: say so, name the roles that never
reported, and print no Done line and no success for it.

A dispatching role with no Bash tool cannot poll: it dispatches in one message
with `run_in_background: false` and never otherwise, and if an acknowledgement
arrives anyway it re-reads the report paths it named until each one is
complete, or hands the step to the producer — it does not end its turn on an
acknowledgement. Serialising the dispatch is not the fix; waiting is.

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
skeleton with a `model:` and an `effort:` in its frontmatter (scripts/staff.sh
fails a role missing either), entries in required-skills.json, a README staff
row and a row in its Models and effort table, a row in the group table of
`scripts/staff.sh`, the ROLES sets in scripts/memory.sh and
scripts/check-memory.sh, lines in tests/test_agents.sh and
tests/test_staff.sh, a stack row in rules/stacks.md if it brings a stack, and,
if it has the Agent tool, its name on the dispatch line of skills/standards,
which loads rules/dispatch.md. Proposed by the research-analyst or tech-lead
when a product needs it; done on a plugin feature branch.

## Question routing
gameplay/scope (games) → game-designer; scope/requirements (apps, web) →
product-manager; screens/UI/accessibility → ux-designer; architecture/stack/
code → tech-lead; tests/acceptance → qa-engineer; versioning/branching/store/
deploy → release-manager; licensing/prior art/competitors/skills →
research-analyst; sound and music → sound-designer; the log and the studio
memory → producer; taste, money, or a gate → founder (queued, never blocking).
