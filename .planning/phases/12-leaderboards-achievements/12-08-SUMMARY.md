---
phase: 12-leaderboards-achievements
plan: 08
subsystem: infra
tags: [play-games-services, play-console, operator-checklist, uat, roadmap, documentation]

# Dependency graph
requires:
  - phase: 12-leaderboards-achievements
    provides: "The 20-constant AchievementId enum and AchievementThresholds (12-02), the 27 PGS_LB_* / PGS_ACH_* local.properties keys and GamesIds accessors (12-04), the ProgressSubmitter that calls them (12-07)"
  - phase: 11-play-games-session
    provides: "11-HUMAN-UAT.md as the project's single accumulating device checklist (D-04), and the stock-popup-untouched posture D-16 mirrors"
provides:
  - "12-OPERATOR-CHECKLIST.md — the one Play Console execution document: ordering, 7 leaderboards, 20 achievements, 20 icons, 27-key ID recording table"
  - "A legal, stated achievement point budget: 1,130 of the 2,000 lifetime maximum, 870 points of headroom"
  - "Six new Phase 12 device tests (4-9) in 11-HUMAN-UAT.md covering every manual-only row in 12-RESEARCH's validation table plus the R8 keep-rule check"
  - "A ROADMAP External Prerequisites row demanding exactly 20 unique 512x512 icons"
affects: [13-cloud-save, 14-release-readiness, play-console-operations]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Operator-facing configuration lives in a phase-scoped checklist that records KEY NAMES and tick boxes, never values"
    - "Runtime claims no JVM test can prove become individually numbered device tests rather than assumed green checks"

key-files:
  created:
    - .planning/phases/12-leaderboards-achievements/12-OPERATOR-CHECKLIST.md
  modified:
    - .planning/phases/11-play-games-session/11-HUMAN-UAT.md
    - .planning/ROADMAP.md

key-decisions:
  - "Phase 12's device tests append to Phase 11's 11-HUMAN-UAT.md rather than opening a 12-HUMAN-UAT.md — Phase 11 D-04 established that file as the project's single accumulating device checklist, and a second one would split the pre-release pass in two"
  - "The achievement point budget is 1,130 of Play Games' 2,000 lifetime maximum, leaving 870 points of headroom for post-v1.1 achievements; every value is a multiple of 5 and none exceeds 200"
  - "The four D-02 first-hour achievements (A01 First Clear 5, A02 Three Stars 10, A03 Getting Started 10, A08 English Cleared 15) carry the four cheapest point values on purpose, so ROADMAP success criterion 3 is readable off the budget"
  - "The CI-secret column in the 27-row recording table is the local.properties key name verbatim, because local.properties.template already establishes the 'SAME-NAMED environment variables in CI' rule — there is nothing to translate"
  - "The 7 completed Phase 12 ROADMAP bullets were NOT reverted to unchecked to satisfy a literal acceptance grep; the tick state is execution truth"

patterns-established:
  - "Immutability warnings are stated twice and in bold (leaderboard sort/format/decimals; achievement type/step total) because Play Games offers no migration for either"
  - "Every committed operator document is grep-gated against leaked IDs with the same 'CgkI' heuristic the gamesIdLiteralCheck build gate scans src/main for"

requirements-completed: [PGSA-01, PGSA-04, PGSL-06, PGSA-05]

# Metrics
duration: 22 min
completed: 2026-09-13
---

# Phase 12 Plan 08: Operator Closure Summary

**A 257-line Play Console execution document (7 leaderboards, 20 achievements totalling 1,130 of 2,000 points, 20 icons, a 27-key ID recording table that holds no ID), six new device tests for every runtime claim the JVM cannot prove, and a ROADMAP that now demands 20 icons rather than 10-12.**

## Performance

- **Duration:** 22 min
- **Started:** 2026-09-13T19:35:00Z
- **Completed:** 2026-09-13T19:57:00Z
- **Tasks:** 3
- **Files modified:** 3 (1 created, 2 modified)

## Accomplishments

- **The 20-achievement roster is now fully specified.** Every `AchievementId` constant has a Console display name, a description, a type, a step total (for the six incremental tiers) and a point value. The six step totals — 10/50/100 and 25/100/250 — are quoted from `AchievementThresholds` rather than restated from memory, and the checklist says explicitly that if the Console value and the app value disagree, the Console wins at runtime and the tier never completes.
- **The point budget is an arithmetic fact, not an aspiration.** All 20 values are multiples of 5, none is below 5 or above 200, and the total is verified by the plan's own awk check at **1,130 of the 2,000 lifetime maximum**, leaving **870 points of headroom** for achievements added after v1.1.
- **The four Play Console ordering traps are ordered and reasoned.** Publish the OAuth consent screen; create the Android credentials *through Play Console* (a Cloud-created client is not associated with this game's leaderboards); register the Play App Signing SHA-1 from App integrity, not the upload certificate; add yourself to the tester list, which does not happen automatically. Each carries the reason, because all four fail identically to a code bug.
- **Nothing that cannot be proven on the JVM is left as an assumed green check.** The unlock popup, both stock UIs opening, a real score landing on three boards, offline-then-sign-in backfill, signed-out row hiding, and the minified-release `missing_rules.txt` inspection are now individually numbered, individually resultable device tests 4 through 9.
- **Zero code touched.** `git diff --name-only eca5c73..HEAD | grep -cE '\.(kt|kts|xml|pro|toml)$'` prints `0`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Write the Play Console operator checklist** — `1905fbb` (docs)
2. **Task 2: Add the six Phase 12 device checks to 11-HUMAN-UAT.md** — `8c96cf5` (docs)
3. **Task 3: Correct the ROADMAP icon prerequisite and finalise the Phase 12 plan list** — `f6d1fe8` (docs)

## Files Created/Modified

- `.planning/phases/12-leaderboards-achievements/12-OPERATOR-CHECKLIST.md` (created, 257 lines) — Step 0 ordering, Step 1 the 7 leaderboards, Step 2 the 20 achievements plus the first-hour and per-row notes, Step 3 the 20 icons, Step 4 the 27-key recording table, Step 5 the hand-off gate.
- `.planning/phases/11-play-games-session/11-HUMAN-UAT.md` (modified, +38/-4) — new `## Phase 12 Tests — Leaderboards & Achievements` section with tests 4-9; frontmatter `source` gains `12-08-PLAN.md` and `updated` refreshed; Summary now `total: 9` / `pending: 9`. Tests 1-3 are byte-identical and unrenumbered.
- `.planning/ROADMAP.md` (modified, +2/-1) — icon prerequisite `10–12` → `20 unique 512×512 achievement icons (one per achievement — see 12-OPERATOR-CHECKLIST.md)`; the missing blank line before Phase 12's `**Notes**:` restored to match Phase 11's format.

## The final point table (for the phase that adds achievement 21)

| # | Console name | Type | Steps | Points |
|---|--------------|------|-------|--------|
| A01 | First Clear | standard | — | 5 |
| A02 | Three Stars | standard | — | 10 |
| A03 | Getting Started | standard | — | 10 |
| A04 | Quick Hands | standard | — | 25 |
| A05 | No Hints Needed | standard | — | 25 |
| A06 | No Take-Backs | standard | — | 25 |
| A07 | Dead Center | standard | — | 50 |
| A08 | English Cleared | standard | — | 15 |
| A09 | European Cleared | standard | — | 20 |
| A10 | Asymmetric Cleared | standard | — | 25 |
| A11 | Diamond Cleared | standard | — | 30 |
| A12 | Wiegleb Cleared | standard | — | 40 |
| A13 | Climbing | incremental | 10 | 25 |
| A14 | Ascending | incremental | 50 | 75 |
| A15 | Century Climb | incremental | 100 | 150 |
| A16 | Star Collector | incremental | 25 | 25 |
| A17 | Star Hoarder | incremental | 100 | 75 |
| A18 | Constellation | incremental | 250 | 150 |
| A19 | Grand Tour | standard | — | 150 |
| A20 | Perfectionist | standard | — | 200 |

**Total: 1,130 of the 2,000 lifetime maximum. Remaining headroom: 870 points.** A 21st achievement must also move `AchievementId` (whose count `AchievementCatalogTest` asserts), `GamesIds.of`'s exhaustive `when`, `local.properties.template`, and the icon count in the ROADMAP's External Prerequisites row.

## Decisions Made

- **Phase 12's device tests append to `11-HUMAN-UAT.md`; no `12-HUMAN-UAT.md` was created.** Phase 11 D-04 established that file as where verification that cannot run on the JVM accumulates, precisely because no Play Console project exists. A second device checklist would split the single pre-release pass in two and make it possible to execute one and forget the other. The file keeps `phase: 11-play-games-session` and `status: partial`; only `source` and `updated` changed.
- **The CI-secret column is the `local.properties` key name verbatim, stated per row rather than inferred.** `local.properties.template`'s preamble already says "set the SAME-NAMED environment variables in CI", so there is no mapping to record — but writing the identical string in its own column removes any chance an operator invents a `PGS_LB_CHALLENGE_LADDER_SECRET`. The checklist also notes that CI needs no new secret today, because the workflow never runs a release invocation.
- **All 20 achievements are visible; none is hidden.** D-05 left this to the planner. PGSA-01 asks for visible achievements, and a hidden achievement cannot serve as a first-hour early win because the player cannot see what to aim at.
- **The recording table's key column carries no backticks**, unlike the roster tables above it. That is what lets `^\| PGS_(LB|ACH)_[A-Z0-9_]+ \|` select exactly the 27 recording rows without colliding with the 27 roster mentions.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Task 3's `- [ ]` acceptance grep contradicted the ROADMAP's true execution state**

- **Found during:** Task 3
- **Issue:** The criterion `grep -cE '^- \[ \] 12-0[1-8]-PLAN\.md — ' .planning/ROADMAP.md` prints `8` was written on the assumption that Task 3 would author the Phase 12 plan list from scratch with every box unchecked. In fact plan-phase wrote the block on 2026-09-09 (the plan's own NOTE says so), and the execute-phase workflow has since ticked `12-01` through `12-07` as each completed. Satisfying the grep literally would require unticking seven genuinely-completed plans — falsifying execution state to make a shape check pass.
- **Fix:** Left the tick state alone. Verified the equivalent state-agnostic property instead: `grep -cE '^- \[[ x]\] 12-0[1-8]-PLAN\.md — '` prints `8`, each of `12-01` … `12-08` appears exactly once, and `12-08` is the single unchecked bullet. Every other Task 3 criterion passes as written.
- **Files modified:** none (the correction was to *not* make an edit)
- **Verification:** `bullets(any state)=8`; per-plan uniqueness loop prints nothing; `^\*\*Plans\*\*: 8 plans in 5 waves$` = 1; `^\*\*Plans\*\*: TBD$` = 2 (Phases 13 and 14).
- **Committed in:** `f6d1fe8` (Task 3 commit)

**2. [Rule 1 - Bug] Task 3's bare-wave-heading grep contradicted the same task's "mirror Phase 11" instruction**

- **Found during:** Task 3
- **Issue:** The criterion `sed -n '/^### Phase 12/,/^### Phase 13/p' | grep -c '^\*\*Wave [1-5]\*\*$'` prints `5` requires all five wave headings to be bare `**Wave N**` lines. The same task's action text says to "Mirror Phase 11's wave-annotation style, **including its parenthetical blocked-on notes**", and Phase 11 (like Phase 10) puts that parenthetical on the heading line itself — `**Wave 2** *(blocked on Wave 1 completion)*`. Only a wave with no blocked-on note can be a bare heading, so the two requirements are mutually exclusive; stripping the parentheticals would delete the dependency information the wave list exists to convey.
- **Fix:** Kept Phase 11's format (Wave 1 bare, Waves 2-5 annotated) and verified the anchorless form: `grep -cE '^\*\*Wave [1-5]\*\*'` over the Phase 12 slice prints `5`.
- **Files modified:** none
- **Verification:** `waves any=5`, `waves bare=1`; `^\*\*Notes\*\*: ` = 1 and the Notes text is byte-unchanged (`PGSA-06 (per-session telemetry Room migration) must land` = 1).
- **Committed in:** `f6d1fe8` (Task 3 commit)

**3. [Rule 1 - Bug] A line wrap hid the tester-list warning from its own acceptance grep**

- **Found during:** Task 1
- **Issue:** Step 0 item 6 originally broke "the account that created the game does not / automatically become a tester" across two lines, so `grep -ci 'not automatically become a tester'` printed `0` — the criterion failed even though the sentence was present and correct. A checked property that a line wrap can silently defeat is not a checked property.
- **Fix:** Rewrapped so the bolded sentence occupies one whole line of its own, which also reads better as the emphasised warning it is.
- **Files modified:** `.planning/phases/12-leaderboards-achievements/12-OPERATOR-CHECKLIST.md`
- **Verification:** `grep -ci 'not automatically become a tester'` now prints `1`; all other Task 1 criteria re-run and still pass.
- **Committed in:** `1905fbb` (Task 1 commit)

**4. [Rule 2 - Missing Critical] Restored the blank line before Phase 12's `**Notes**:` paragraph**

- **Found during:** Task 3
- **Issue:** The `**Notes**:` paragraph sat immediately below the `12-08-PLAN.md` bullet with no blank line, so markdown renders it as a continuation of the list item rather than as the phase-level note it is. Phase 11 (and every other phase entry) has the blank line. The plan's action text says to "keep the blank line before it" — it was not there to keep.
- **Fix:** Inserted the blank line. This is the only drift found between the on-disk plan list and 12-PLAN-OUTLINE.md's wave grouping.
- **Files modified:** `.planning/ROADMAP.md`
- **Verification:** `git diff --stat` shows `2 insertions(+), 1 deletion(-)` across the icon row and this blank line; `grep -c '^-'` over the diff is `2` (one of which is the `---` file header), well inside the "≤ 5 removed lines" criterion.
- **Committed in:** `f6d1fe8` (Task 3 commit)

**5. [Rule 1 - Bug] `state advance-plan` produced "Plan: 2 of 8" from a stale counter**

- **Found during:** state update (post-Task 3)
- **Issue:** STATE.md's Current Position still read `Plan: 1 of 8` — plans 12-01 through 12-07 ran in worktree/parallel mode, where the executor deliberately skips the STATE.md write and the orchestrator owns it centrally, and that central advance never happened for waves 1-4. `gsd-sdk query state.advance-plan` incremented that stale 1 to 2, which would have recorded Phase 12 as one-eighth done at the exact moment its last plan finished.
- **Fix:** Set the position from disk truth instead — 8 PLAN files and 8 SUMMARY files exist, so `Plan: 8 of 8`, phase marked COMPLETE with the outstanding operator work named in the Status line. `state update-progress` had already recomputed the frontmatter counters from disk correctly (`completed_plans: 57`, `completed_phases: 7`, 78%), so only the human-readable position block needed correcting.
- **Files modified:** `.planning/STATE.md`
- **Verification:** `ls .planning/phases/12-leaderboards-achievements/*-PLAN.md | wc -l` = 8 and `*-SUMMARY.md` = 8; `gsd-sdk query roadmap.update-plan-progress 12` independently reports `plan_count: 8, summary_count: 8, status: Complete`.
- **Committed in:** the plan metadata commit

---

**Total deviations:** 5 auto-fixed (4 bugs — three in the plan's own acceptance criteria or source formatting, one in a stale state counter — and 1 missing formatting element).
**Impact on plan:** No scope creep and no code. Deviations 1 and 2 are documented refusals to damage true state in order to satisfy a malformed grep; both were replaced with an equivalent check that actually holds. Deviations 3 and 4 are one-line formatting fixes. Every substantive requirement of the plan is met exactly as written.

## Verification Results

All three plan-level verification blocks pass:

1. **Operator checklist shape and arithmetic** — `^| L[0-9] |` = 7; `^\| A[0-9]{2} \|` = 20; `^\| PGS_(LB|ACH)_[A-Z0-9_]+ \|` = 27, all 27 with an unticked box; point sum = **1130**; illegal point values = 0; `Total points: 1,130 of the 2,000 maximum` = 1; `| incremental |` = 6 and `| standard |` = 14; first-hour rows A01/A02/A03/A08 = `5 10 10 15`; `Larger is better` = 7; `immutab` = 2; `512×512` present; tester / through-Play-Console / App-integrity warnings present; **`CgkI` = 0**; fenced code blocks = 0.
2. **Device checklist extension** — `^## Phase 12` = 1; `^### [1-9]\. ` = 9; `^result: \[pending\]$` = 9; `^expected: ` = 9; `total: 9` / `pending: 9`; Phase 11 tests 1-3 byte-identical (the whole-file diff removes exactly four lines: the two frontmatter lines and the two counters); fenced code blocks = 0.
3. **ROADMAP** — `20 unique 512×512 achievement icons` = 1; `10–12 unique 512×512 achievement icons` = 0; `12-OPERATOR-CHECKLIST.md` = 1; `Phase 12 (PGSA-01/04)` Blocks cell intact; 8 plan bullets, each plan id exactly once.

**Cross-document consistency** — the 20-constant loop checking `PGS_ACH_<AchievementId>` against the checklist prints nothing; every enum constant in `domain/src/main/kotlin/com/app/pegsolitaire/domain/achievement/AchievementId.kt` has a row.

**Documentation-only proof** — `git diff --name-only eca5c73..HEAD` lists exactly the three `files_modified` plus this SUMMARY; `git diff --stat eca5c73..HEAD -- '*.kt' '*.kts' '*.xml'` is empty; the extension filter count is `0`.

## Issues Encountered

None. No Gradle task was run and none was needed — this plan compiles nothing, and the phase's build gates are owned by plans 12-01 through 12-07.

## User Setup Required

**Yes — and it is the entire point of this plan.** `.planning/phases/12-leaderboards-achievements/12-OPERATOR-CHECKLIST.md` is the setup document: create the Google Cloud + Play Games Services projects in the documented order, create 7 leaderboards and 20 achievements, upload 20 unique 512×512 icons, and record 27 generated IDs into an untracked `local.properties`. No `12-USER-SETUP.md` was generated because the plan carries no `user_setup:` frontmatter — the checklist supersedes it and is more specific.

## Unverified Runtime Claims

**PGSA-04, PGSL-06, PGSA-05 and the PGSL-01 / PGSL-02 / PGSL-03 runtime behaviours remain `[UNVERIFIED — D-04]`** until a Play Console PGS project exists and `11-HUMAN-UAT.md` tests **4 through 9** are executed on a device with Play Games and a tester account. Their *code* contributions are complete and JVM-tested against a recording fake; what is unproven is that a Google-rendered popup appears, that a Google-owned Activity opens, and that a score lands on a real board. 12-RESEARCH's "Environment Availability" table marks the entire Play Console surface `✗`, and this plan's answer is to name the gap rather than to produce a green check for it.

Also still open, for the same reason: Pitfall 12's R8 check (test 9). `play-services-games-v2:22.0.0` ships no `proguard.txt`, and this phase added the first `LeaderboardsClient` / `AchievementsClient` / `Intent`-returning calls, so `missing_rules.txt` has never been inspected with them on the graph.

## Next Phase Readiness

- **Phase 12 is code-complete.** All eight plans have summaries; the phase's remaining work is operator work, and it now has exactly one document.
- **Phase 13 (Cloud Save) is unblocked** — it depends on `ProgressRepository` as the single source of truth (12-06) and the `GamesService` seam (12-03), neither of which needs a live Console project to develop against.
- **Blocker carried forward:** the External Prerequisites row now demands **20** unique 512×512 icons with art turnaround as its lead time, up from the 10–12 that was budgeted before the roster was fixed. Until they exist, PGSA-04 cannot be verified at all — a missing icon is a silent no-render, not an error.
- **Recommended next:** `/gsd:verify-work 12`, then Phase 13 planning. Run `./gradlew check` plus one `assembleRelease` for the R8 check before the phase gate, per 12-RESEARCH's sampling rate.

---
*Phase: 12-leaderboards-achievements*
*Completed: 2026-09-13*

## Self-Check: PASSED

All three `files_modified` exist on disk, the created `12-OPERATOR-CHECKLIST.md` exists, and all three task commits (`1905fbb`, `8c96cf5`, `f6d1fe8`) are present in `git log`.
