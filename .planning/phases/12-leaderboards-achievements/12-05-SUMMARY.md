---
phase: 12-leaderboards-achievements
plan: 05
subsystem: ui
tags: [kotlin, android, viewmodel, navigation, kotlinx-serialization, resume-snapshot, telemetry, robolectric, turbine, tdd]

# Dependency graph
requires:
  - phase: 12-leaderboards-achievements
    provides: "SolveTelemetry (plan 12-02) — the pure-domain carrier, its NONE value and its clamping `coerced` factory"
  - phase: 09-consent-rewarded-ads
    provides: "the assist funnel (enterAssistFunnel / onAcceptAdOffer / onDismissAdOffer) whose success path is instrumented here, and AssistAllowance.fromStoredOrFull's clamp-don't-refuse posture"
  - phase: 05-persistence-resume
    provides: "ResumeSnapshotDto + SnapshotMapper + RestoredGame — the nullable-with-default on-disk contract the two new counters copy"
provides:
  - "ResumeSnapshotDto.hintsUsed / undosUsed — two nullable Int on-disk fields with a fail-CLOSED contract"
  - "SnapshotMapper.toDto(..., hintsUsed, undosUsed) + RestoredGame.hintsUsed / undosUsed (non-null, codec-resolved)"
  - "GameFinish(result, telemetry) — the finish-event payload; GameViewModel.finished is now Flow<GameFinish>"
  - "GameViewModel.countAssist(kind) — the single counter mutation point, at the two application sites"
  - "GameViewModel.lastPegOnTarget(finishingBoard) — the D-01 target test, read from the finishing board's own spec"
  - "Results.hintsUsed / undosUsed / lastPegOnTarget — three defaulted route args"
  - "ResultsViewModel.solveTelemetry — the internal, already-coerced SolveTelemetry that 12-06 consumes"
affects: [12-06 play_session write and recordResult signature, 12-07 submission loop]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Fail-CLOSED nullable persistence field sitting deliberately adjacent to a fail-OPEN one, with the asymmetry argued in both KDocs"
    - "Counting at the APPLICATION point rather than the permission point, so 'a declined offer costs nothing' is structural instead of a remembered rule"
    - "One private resolver (orNoAssists) owning what an absent/forged stored counter means, so no consumer of a restore sees a raw nullable"
    - "Defaulted flat scalar nav args as the compatibility mechanism for extending a type-safe route, with the defaults documented as load-bearing"
    - "Asserting private ViewModel state exclusively through its legitimate observables (the finish event, the saved snapshot) rather than a test-only accessor"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameFinish.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTelemetryTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt
    - app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt
    - app/src/main/java/com/app/pegsolitaire/navigation/Routes.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt
    - app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt

key-decisions:
  - "The two counter writes live at the application points (performUndo, revealPendingHint), never in enterAssistFunnel — which is what makes a dismissed offer, a Declined reward, an empty-history undo, a null hint search and a still-loading hint request all cost nothing with no second rule"
  - "Absent assist counters resolve to 0 (fail-CLOSED), deliberately the opposite direction to freeHintsRemaining's fail-open-to-full: a missing allowance is a generosity, a missing assist counter is a claim of purity"
  - "resetToInitial zeroes the counters — the mirror image of the allowance it deliberately does not reset — so a restart cannot launder a hint-free badge; clearSnapshot makes the reset durable"
  - "lastPegOnTarget is read from the FINISHING board's own spec.target, never a catalog lookup by levelId, so a resumed game (whose spec is rebuilt structurally from the blob) is judged correctly"
  - "The general lastPegOnTarget fact is recorded for every board; the D-01 narrowing to English-33 stays a 12-06 SQL predicate, making a future per-board variant a query change rather than a migration"
  - "Three flat defaulted scalars on Results rather than a nested payload — a nested @Serializable type in a Navigation 2.9 type-safe route needs a hand-written custom NavType for no benefit"
  - "SnapshotMapper's fromDto does NOT build a SolveTelemetry: a restore has no finishing board and therefore no honest lastPegOnTarget, and manufacturing a false there would put a scoring claim into a type with no business making one"
  - "detekt's TooManyFunctions was suppressed at `object SnapshotMapper` with a reason rather than relaxed in config (deviation Rule 3)"

patterns-established:
  - "Application-point counting: an assist counter is advanced beside the UI cue that already means 'this really happened' (undoTick), so counter and cue share one condition and cannot drift"
  - "Adjacent-opposite persistence contracts: two nullable fields of the same shape resolving in opposite directions, each KDoc naming the other and arguing the asymmetry"
  - "Extending a type-safe nav route by defaulted primitives only, so every existing construction stays valid and an older back stack restores as the zero value"

requirements-completed: [PGSA-02]

# Metrics
duration: 34 min
completed: 2026-09-08
---

# Phase 12 Plan 05: Per-Session Assist Telemetry Summary

**Every solve now knows how many hints the player actually received, how many undos actually rewound the board, and whether the last peg landed on the board's own target hole — counted at the assist application point, persisted through the resume snapshot, and delivered to `ResultsViewModel` as one clamped `SolveTelemetry`.**

## Performance

- **Duration:** 34 min
- **Started:** 2026-09-08T22:23:00Z
- **Completed:** 2026-09-08T22:57:16Z
- **Tasks:** 3 (each executed RED → GREEN, 6 commits)
- **Files created:** 2
- **Files modified:** 10

## Accomplishments

- **The counting rule is structural, not remembered.** `countAssist(kind)` is the only place either counter moves, and it is called from exactly two application points — `performUndo`, past its empty-history re-guard and the history pop, and `revealPendingHint`, past its null-stash and staleness returns. Six dedicated tests pin the paths that must cost nothing: `onDismissAdOffer`, `RewardOutcome.Declined`, an undo on an empty history, a hint search returning `null`, a hint discarded when the board moved on, and a hint requested while the board is still loading. `Granted` and `Unavailable` both count one, because in both the assist was actually delivered.
- **Process death can no longer launder an assist away.** The counters ride the resume blob as `hintsUsed` / `undosUsed`, written by `onLeaveScreen` and read back by `applyRestored`. A blob written before the fields existed still decodes (T-12-22 — the nullable defaults are what stop every installed player's in-progress game vanishing on upgrade) and reads as **zero**: fail-CLOSED, deliberately the opposite direction to `freeHintsRemaining`'s fail-open-to-full.
- **A restart resets the counters and still does not refill the allowance.** The two now sit adjacent in `resetToInitial` with the asymmetry argued in place: a retry may not hand back free assists (D-04), but neither may it let a player take a hint, restart, and bank a hint-free badge on the second attempt (D-09). Because `resetToInitial` also clears the snapshot, that reset is durable — unlike the allowance's accepted D-17 Option A hole.
- **`lastPegOnTarget` is judged on the board that was actually played.** The target comes from `finishingBoard.spec.target`, never a catalog lookup by `levelId`, which is the only version that is correct for a resumed game whose spec is rebuilt structurally from the blob. A board with no target (every freeform Challenge footprint) and a multi-peg Won finish (Diamond-41's proven two-peg floor) are both false — four tests, one per case.
- **`ScoreResult` gained nothing.** The telemetry rides alongside it in a new `GameFinish` two-field envelope, so a consumer cannot receive a score without the telemetry of the very play that produced it, and the scoring type stays a scoring type. `GameViewModel.finished` is now `Flow<GameFinish>`; the six existing consumers were migrated mechanically to `.result` with no assertion or value change.
- **Both untrusted boundaries clamp rather than refuse.** A forged negative counter in the blob resolves to 0 through `SnapshotMapper.orNoAssists()` and the resume still succeeds (T-12-21); a forged negative nav arg is bounded by `SolveTelemetry.coerced` at the single `ResultsViewModel` clamp point (T-12-20), and a test proves the recorded `RecordCall` and the whole `ResultsUiState` are byte-identical with and without a forged telemetry. A large forged value passes through in both places — only the negative case can corrupt a downstream `> 0` predicate.

## Task Commits

Each task was executed RED → GREEN and committed atomically:

1. **Task 1: Persist the assist counters in the resume snapshot** — `e4b7577` (test) → `d33335c` (feat)
2. **Task 2: Count applied assists and compute lastPegOnTarget in GameViewModel** — `b9c22ee` (test) → `57fbcd6` (feat)
3. **Task 3: Carry the telemetry to Results as clamped nav args** — `6c7ae4c` (test) → `6f83ba2` (feat)

## Files Created/Modified

- `app/src/main/java/com/app/pegsolitaire/ui/game/GameFinish.kt` — **created.** The `ScoreResult` + `SolveTelemetry` finish-event envelope; a UI-layer transport type, deliberately not in `:domain` and deliberately not a `ScoreResult` field.
- `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTelemetryTest.kt` — **created.** 19 Robolectric tests (876 lines) asserting the counting rule, the non-counting paths, restart reset, snapshot round trip and the four `lastPegOnTarget` cases, entirely through the finish event or the saved snapshot.
- `app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt` — two nullable `Int` fields with the fail-closed on-disk contract and its contrast with `freeHintsRemaining` documented.
- `app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt` — two trailing defaulted `toDto` params, `RestoredGame`'s two non-null counters, `NO_ASSISTS_RECORDED`, and the single `orNoAssists()` resolver.
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` — the two private counters, `countAssist`, the two call sites, the reset, the snapshot write/read, `telemetryFor` and `lastPegOnTarget`, and the `GameFinish` channel.
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt` — collects `GameFinish` and is the only producer of the three telemetry route args.
- `app/src/main/java/com/app/pegsolitaire/navigation/Routes.kt` — three defaulted telemetry properties on `Results`, with the "flat scalars, not a nested payload" rationale recorded.
- `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt` — `internal val solveTelemetry`, coerced once beside the `challengeLevel` clamp.
- `app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt` — five assist-counter cases including the legacy-blob and forged-counter paths.
- `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt` — five telemetry cases plus three defaulted harness params that omit each arg unless it differs from the route default.
- `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt`, `GameFlowTest.kt` — mechanical `.result` migration of the six `finished` consumers.

## Symbol Contract for Plan 12-06

12-06 consumes these exact names; do not invent variants.

| Symbol | Location | Shape |
|--------|----------|-------|
| `ResumeSnapshotDto.hintsUsed` | `app/.../data/resume/ResumeSnapshotDto.kt` | `Int? = null` — absent means 0 |
| `ResumeSnapshotDto.undosUsed` | `app/.../data/resume/ResumeSnapshotDto.kt` | `Int? = null` — absent means 0 |
| `SnapshotMapper.toDto(…, hintsUsed, undosUsed)` | `app/.../data/resume/SnapshotMapper.kt` | two trailing `Int? = null` params |
| `SnapshotMapper.NO_ASSISTS_RECORDED` | `app/.../data/resume/SnapshotMapper.kt` | `private const val = 0` |
| `RestoredGame.hintsUsed` / `RestoredGame.undosUsed` | `app/.../data/resume/SnapshotMapper.kt` | non-null `Int`, already resolved |
| `GameFinish(result, telemetry)` | `app/.../ui/game/GameFinish.kt` | `ScoreResult` + `SolveTelemetry`; `GameViewModel.finished` is now `Flow<GameFinish>` |
| `GameViewModel.countAssist(kind)` | `app/.../ui/game/GameViewModel.kt` | the only counter mutation point |
| `Results.hintsUsed` / `Results.undosUsed` / `Results.lastPegOnTarget` | `app/.../navigation/Routes.kt` | `Int = 0`, `Int = 0`, `Boolean = false` |
| **`ResultsViewModel.solveTelemetry`** | `app/.../ui/results/ResultsViewModel.kt` | `internal val` of type `SolveTelemetry`, already `coerced` — **this is the value 12-06 passes to `ProgressRepository.recordResult`** |

Also on the record:

- **`ProgressRepository.recordResult` was deliberately NOT changed** and `play_session` was not written — that signature change and the SQL are 12-06's. `grep -c 'solveTelemetry\|SolveTelemetry' app/.../data/ProgressRepository.kt` returns `0`.
- **The D-01 narrowing of `lastPegOnTarget` to English-33 is still owed by 12-06's SQL predicate** (`mode = 'classic' AND levelId = 'english33'`). `GameViewModel` records the general fact for every board, on purpose.

## Decisions Made

See `key-decisions` in the frontmatter. The two worth restating in prose:

- **Counting at the application point is the whole security design of T-12-23.** Placing the two writes past every guard is what makes "a declined or failed offer counts nothing" a property of the code's shape rather than a rule that has to be re-derived each time the funnel changes. Nothing was added to `enterAssistFunnel` except a KDoc paragraph pointing at the two sites and saying why.
- **The fail-closed direction was chosen field by field, not by house style.** `freeHintsRemaining` fails open because a missing allowance is a generosity worth at most a few free hints; `hintsUsed` fails closed because a missing assist counter is a *claim of purity* that would hand out an achievement nobody earned. Both KDocs name the other and argue the asymmetry, so neither reads as a copy-paste slip.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] detekt `TooManyFunctions` on `object SnapshotMapper`**
- **Found during:** Task 1 (snapshot persistence)
- **Issue:** Adding the `orNoAssists()` resolver took the object to 12 functions against a threshold of 11, failing `:app:detekt`. The plan anticipated a `LongParameterList` suppression on `toDto` but not this rule.
- **Fix:** `@Suppress("TooManyFunctions")` at the object with a one-line reason ("one private validator per stored field; merging any two would hide a rejection rule"), matching the plan's instruction to suppress at the declaration and never in config. Inlining the resolver at both call sites was rejected — the plan explicitly wants one place that owns what an absent counter means.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt`
- **Verification:** `./gradlew detekt ktlintCheck` green.
- **Committed in:** `d33335c`

**2. [Rule 3 - Blocking] `MaxLineLength` on the anticipated `LongParameterList` suppression**
- **Found during:** Task 1
- **Issue:** The plan predicted `toDto` might exceed detekt's `LongParameterList` budget at 10 params and told me to suppress at the function. I added it pre-emptively; the comment line breached `MaxLineLength`, and on removing it detekt confirmed the rule does not fire at all on this detekt 2.0 configuration (defaulted parameters are not counted).
- **Fix:** Removed the unnecessary `@Suppress("LongParameterList")` entirely rather than reflowing it — a suppression for a rule that does not fire is worse than none.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt`
- **Verification:** `./gradlew detekt ktlintCheck` green with no suppression present.
- **Committed in:** `d33335c`

---

**Total deviations:** 2 auto-fixed (2 blocking). No architectural change, no dependency change, no config change.
**Impact on plan:** None on scope or behaviour — both were lint-gate mechanics inside Task 1's own files.

## Issues Encountered

- **The "hint discarded as stale" branch is not reachable in a test, and the spec says so.** `revealPendingHint`'s board re-check requires `pendingHint != null` *and* a changed board, but every board mutation calls `cancelHintSearch()` → `clearPendingHint()`, so the stash is always already gone. The test drives the observable behaviour (request a hint past exhaustion, play a jump, then let the grant return — nothing is counted) and the comment states plainly that the discard happens at the stash-drop, upstream of the staleness re-check, and that both return before the counter. The re-check remains correct defence in depth for an SDK callback whose timing the app does not control.
- **Two "counts nothing" cases cannot use the finish event**, because a board with no winning line can never reach `Won`. Those assert through the other legitimate observable — the counters as written into the resume snapshot by `onLeaveScreen` — rather than through a test-only accessor, which the spec's class KDoc explains.

## User Setup Required

None — no external service configuration, no new dependency, no version-catalog change. RESEARCH's Package Legitimacy Audit found no new package anywhere in Phase 12, and this plan installs nothing (T-12-SC: N/A).

## Threat Flags

None. Every trust boundary this plan touches was already in the plan's own `<threat_model>` (T-12-20 through T-12-24). No network endpoint, no auth path, no file access, no schema or SQL change was introduced; the furthest the telemetry travels is a `ResultsViewModel` field.

## Verification Results

| Check | Result |
|-------|--------|
| `./gradlew :app:testDebugUnitTest --tests "*SnapshotMapper*"` | PASS |
| `./gradlew :app:testDebugUnitTest --tests "*GameViewModel*" --tests "*GameFlowTest*"` | PASS (19/19 new telemetry tests; no existing spec regressed) |
| `./gradlew :app:testDebugUnitTest --tests "*ResultsViewModelTest*"` | PASS |
| `./gradlew :app:testDebugUnitTest --rerun-tasks` (whole `:app` JVM suite) | PASS |
| `./gradlew :app:compileDebugKotlin` | PASS |
| `./gradlew detekt ktlintCheck` | PASS |
| `grep -vE "^\s*(\*\|//)" GameViewModel.kt \| grep -c "countAssist(AssistKind"` | `2` |
| `grep -c "solveTelemetry\|SolveTelemetry" data/ProgressRepository.kt` | `0` |
| `grep -vE "^\s*(\*\|//)" ResultsViewModel.kt \| grep -c "SolveTelemetry.coerced("` | `1` |
| `grep -vE "^\s*(\*\|//)" ResultsViewModel.kt \| grep -cE "args\.(hintsUsed\|undosUsed\|lastPegOnTarget)"` | `3` |

Note: the phase gate `./gradlew check` (including the ~11-minute `:domain:depthSweep`) is the orchestrator's to run post-merge; `:domain` was not touched by this plan.

## Next Phase Readiness

- **12-06 is unblocked.** `ResultsViewModel.solveTelemetry` is a resolved, clamped `SolveTelemetry` sitting on the ViewModel that already owns the single `recordResult` call site — 12-06 changes that signature, threads this value into the `play_session` write, and adds the `mode = 'classic' AND levelId = 'english33'` predicate that narrows `lastPegOnTarget` to the Center-finish achievement.
- **Executed in worktree branch `worktree-agent-a4bc912db2880f428`** (base `025fa79`). No `.planning/STATE.md` or `.planning/ROADMAP.md` write was made; the orchestrator owns those.
- **Sibling-plan isolation held:** nothing under `app/build.gradle.kts`, `build-logic/` or `platform/games/` was touched, so the concurrent 12-04 worktree cannot conflict with this one.

## Self-Check: PASSED

- `app/src/main/java/com/app/pegsolitaire/ui/game/GameFinish.kt` — FOUND
- `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTelemetryTest.kt` — FOUND (876 lines, above the plan's 200-line floor)
- Commits `e4b7577`, `d33335c`, `b9c22ee`, `57fbcd6`, `6c7ae4c`, `6f83ba2` — all FOUND in `git log`
- All three tasks' `<acceptance_criteria>` re-run and passing; plan-level `<verification>` re-run and passing
- No stub markers (`TODO` / `FIXME` / placeholder / "not available") in any file this plan created or modified

---
*Phase: 12-leaderboards-achievements*
*Completed: 2026-09-08*
