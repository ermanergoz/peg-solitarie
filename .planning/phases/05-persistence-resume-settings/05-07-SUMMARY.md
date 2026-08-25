---
phase: 05-persistence-resume-settings
plan: 07
subsystem: ui
tags: [resume, lifecycle, navigation, mvvm, state-hoisting, persistence, tdd]

# Dependency graph
requires:
  - phase: 05-persistence-resume-settings
    plan: 03
    provides: "ProgressRepository save/load/clearSnapshot + observeResumeModes; SnapshotMapper.toDto/fromDto (fail-safe rebuild-by-replay)"
  - phase: 05-persistence-resume-settings
    plan: 06
    provides: "Won-latch advance site in GameViewModel; 3-arg AppNavHost homeContent slot; HomeUiState.challengeCurrentLevel"
  - phase: 04-assist-hint-undo
    provides: "the onUndo fold over moveHistory — the model the restore seeds"
provides:
  - "`Routes.Gameplay.resume: Boolean` — the Continue entry flag"
  - "`GameViewModel.onLeaveScreen()` — Playing-only snapshot write (D-04)"
  - "Regeneration-free resume: initialBoard + validated history + elapsed seeded from the snapshot"
  - "`clearSnapshot(mode)` on solve and on restart (D-05)"
  - "`HomeUiState.classicResumeLevelId` / `challengeResumeAvailable` (+ derived `classicResumeAvailable`)"
  - "Home `ResumeTile` — up to two Continue affordances, one per mode"
  - "`AppNavHost` HomeContent typealias: 5-slot Home content with the two resume entries"
affects: [05-08-phase-verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Lifecycle-driven persistence: DisposableEffect + LifecycleEventObserver on ON_STOP in the Route, never onCleared()"
    - "Restore-first init: the resume branch tries the snapshot and only falls through to the normal load when there is nothing usable — a successful Continue never touches the generator"
    - "An elapsed-time BASE offset added to the monotonic mark, so a restored timer resumes rather than restarts"
    - "A hoisted content typealias (HomeContent) once a NavHost slot passes ~4 lambdas"

key-files:
  created:
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelResumeTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt
    - app/src/main/java/com/app/pegsolitaire/navigation/Routes.kt
    - app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeRoute.kt
    - app/src/main/res/values/dimens.xml
    - app/src/main/res/values/strings.xml
    - app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/home/HomeScreenTest.kt
    - app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelChallengeAdvanceTest.kt

key-decisions:
  - "The resume branch runs the restore FIRST and calls the normal load only on failure, so a successful Continue provably never reaches loadChallenge (asserted by a counting LevelRepository) while a corrupt blob still lands on a playable board"
  - "A new `elapsedBaseSeconds` offset carries the restored time into the ticker — without it the first post-resume move would reset the visible timer to 0 and silently discard the restored elapsed time"
  - "clearSnapshot fires for BOTH modes on a solve (the plan named Challenge); D-05 is mode-agnostic and a stale Classic Continue is the same bug"
  - "Home resumes are two dedicated `Continue Classic` / `Continue Challenge` tiles above the mode tiles, not a state change on the existing tiles: entering the ladder and resuming a saved board are different intents (D-01 — Continue is explicit), and the 05-06 tile copy stays untouched"
  - "`HomeUiState` carries `classicResumeLevelId` (the board to resume) with `classicResumeAvailable` derived from it — the screen hands the id to the click lambda, so `AppNavHost` never has to guess which Classic board a snapshot belongs to"
  - "The restored Challenge game does not restore a recorded solution line (it is not stored, and inventing one means regenerating) — hints fall back to the Solver, the same path a deviated line already takes"

patterns-established:
  - "A persisted seam is only 'wired' when the write site, the read site and the render/nav consumer each have their own failing-first test — extended here to lifecycle writes"
  - "Fail-safe reads are asserted from the consumer's side too: an unreadable blob must make the affordance *disappear*, not throw"

requirements-completed: [PERS-03]

# Metrics
duration: 38min
completed: 2026-08-25
---

# Phase 05 Plan 07: In-Progress Resume Summary

**Backgrounding a game in progress now writes a per-mode snapshot on `ON_STOP`, Home offers a Continue affordance for each mode holding one, and tapping it rebuilds the exact board by replaying the stored history — undo intact, elapsed time carried over, the generator never called, and the snapshot dropped the moment the level is solved or restarted.**

## Performance

- **Duration:** ~38 min
- **Tasks:** 2 automatable (Task 1 TDD: RED → GREEN) + 1 blocking human-verify checkpoint (NOT executed — awaiting the user)
- **Files modified:** 17 (1 created, 16 modified)
- **Tests added:** 16 (9 resume VM + 5 Home VM + 3 Home render + 3 nav, minus the 4 pre-existing cases they extend)

## Accomplishments

- **Continue restores, never regenerates (D-06).** `GameViewModel`'s resume branch loads the snapshot, rebuilds via `SnapshotMapper.fromDto`, and only calls the normal load path when there is nothing usable. A counting `LevelRepository` proves `loadChallenge` is called exactly once — for the original game — and not again on the resume.
- **Undo survives the round trip.** The restored `moveHistory` is real history: `undoStillWorksImmediatelyAfterAResume` plays two moves, saves, rebuilds in a fresh ViewModel, and asserts one `onUndo()` lands on the board as it stood after the first move with a second undo still available.
- **The clock resumes instead of restarting.** The snapshot's elapsed time seeds a new `elapsedBaseSeconds` offset that the per-second ticker adds to its monotonic measurement, so 30 s left is 30 s restored *and* the next move continues from 30 rather than resetting to 0 (SC3). Without the offset the restored value would have survived exactly until the first tap.
- **No stale Continue can outlive a game.** `onLeaveScreen()` returns immediately unless the status is `Playing` (Pitfall 4), and `clearSnapshot(mode)` fires inside the Won latch and in `resetToInitial` (D-05). Four tests pin the truth table: leaving to Home saves and does not clear; a Won board neither saves nor survives; a restart clears.
- **Untrusted state fails safe all the way to the UI.** A forged move history and an absent snapshot both degrade to the normal load without throwing (T-05-03/T-05-04), and on Home an unreadable Classic blob simply makes the Continue affordance *disappear* — asserted from the consumer side, not just inside the repository.
- **Home shows up to two Continue entries (D-01/D-03).** `observeResumeModes()` drives them; the Classic one additionally carries the board id its snapshot was taken on, so `AppNavHost` routes to that exact board with `resume = true`. `HomeScreenTest` proves both tiles render together and each resumes its own mode; `NavFlowTest` proves the route args (`levelId` + `resume`, `challengeLevel` + `resume`) and that the plain Challenge tile still navigates with `resume = false`.

## Task Commits

1. **Task 1: Snapshot save/restore/clear + the `Gameplay(resume)` flag** (TDD)
   - `f75bda3` (test) — RED: compile-failed on the missing `onLeaveScreen()` / `progressRepository` seam
   - `a6fbef2` (feat) — GREEN: 9 tests, 0 failures
2. **Task 2: `ON_STOP` save + Home Continue tiles + Continue routing** — `4aa33bb`

No REFACTOR commit: both implementations landed at their final shape (the ktlint/import fixes were made before their commits).

## Files Created/Modified

**Created**
- `ui/game/GameViewModelResumeTest.kt` — 9 Robolectric cases over a `RecordingProgressRepository` (stores real DTOs, so both halves run the production encode → rebuild path) and a `CountingLevelRepository` (the no-regeneration proof).

**Modified (main)**
- `ui/game/GameViewModel.kt` — `progressRepository` dependency; `resume` + `mode` route-derived properties; `onLeaveScreen()`; `applyRestored()`; `loadChallengeBoardIfNeeded()` (the old inline init branch, extracted so the resume path can fall back to it); `clearSnapshot()` called from the Won latch and `resetToInitial`; `elapsedBaseSeconds` + the ticker offset; a `MILLIS_IN_SECOND` constant.
- `ui/game/GameRoute.kt` — `DisposableEffect(lifecycleOwner)` registering a `LifecycleEventObserver` that calls `onLeaveScreen()` on `Lifecycle.Event.ON_STOP`, removed in `onDispose`.
- `navigation/Routes.kt` — `Gameplay.resume: Boolean = false`.
- `navigation/AppNavHost.kt` — new `HomeContent` typealias (5 slots); the two Continue routes; `toChallengeBoard(level, resume)`.
- `ui/home/HomeViewModel.kt` — `progressRepository` dependency + the `observeResumeModes()` collect (opening the Classic blob only to learn its board id).
- `ui/home/HomeUiState.kt` — `classicResumeLevelId` + `challengeResumeAvailable`, with `classicResumeAvailable` derived.
- `ui/home/HomeScreen.kt` — `onResumeClassic`/`onResumeChallenge` params and a `ResumeTile` (secondaryContainer `Card`) rendered per available mode above the mode tiles.
- `ui/home/HomeRoute.kt` — forwards the two new hoisted lambdas.
- `res/values/strings.xml`, `res/values/dimens.xml` — four Continue strings + `resume_tile_min_height`.

**Modified (tests)**
- `ui/home/HomeViewModelTest.kt` — +5 cases and a `FakeResumeProgressRepository`.
- `ui/home/HomeScreenTest.kt` — +3 render cases behind a shared `renderHome` helper.
- `navigation/NavFlowTest.kt` — +3 cases; the four `homeContent` call sites widened to 5 params.
- `ui/game/GameViewModelTest.kt`, `GameFlowTest.kt`, `GameViewModelChallengeAdvanceTest.kt` — absorb the new `GameViewModel` dependency with a relaxed stand-in.

## Decisions Made

See `key-decisions` in the frontmatter. The load-bearing one is **restore-first ordering**: because the snapshot is tried before the load and the load only runs on failure, "a resume never regenerates" is a structural property of the code rather than a rule someone must remember — and the counting-repository assertion can actually fail if that ordering is ever inverted.

The second is the **`elapsedBaseSeconds` offset**. The plan asked for `elapsedSeconds` to be restored; restoring only the state field would have passed a naive assertion while silently resetting the visible timer on the very next move, because the ticker writes `mark.elapsedNow()` from a mark created at that move. Treating the restored value as a base the ticker adds to is what makes "elapsed time exactly restored" survive contact with play.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Recreated the gitignored `local.properties` in the worktree**
- **Found during:** setup (first Gradle invocation)
- **Issue:** `local.properties` is gitignored, so a fresh worktree fails every Gradle command with "SDK location not found" (the same blocker 05-02/05-03/05-04/05-06 hit).
- **Fix:** Wrote `sdk.dir=/Users/ermanergoz/Library/Android/sdk`.
- **Files modified:** `local.properties` (gitignored — intentionally **not** committed)
- **Committed in:** n/a

**2. [Rule 3 - Blocking] Updated the three existing `GameViewModel` test harnesses**
- **Found during:** Task 1 (GREEN)
- **Issue:** Adding the `progressRepository` constructor parameter is source-breaking for `GameViewModelTest.viewModelFor`, `GameFlowTest.startGame` and `GameViewModelChallengeAdvanceTest.viewModelFor`.
- **Fix:** Each passes `mockk(relaxed = true)` with a comment pointing at the dedicated resume spec; no behaviour changed.
- **Verification:** full `:app:testDebugUnitTest` green.
- **Committed in:** `a6fbef2`

**3. [Rule 2 - Missing Critical] `elapsedBaseSeconds` — the restored timer must keep counting**
- **Found during:** Task 1 (implementing the restore)
- **Issue:** Seeding `GameUiState.elapsedSeconds` alone satisfies the letter of "restore elapsed" but the ticker overwrites it with `mark.elapsedNow()` on the first post-resume move, throwing the restored time away. SC3 ("elapsed time exactly restored") would have been true for one frame.
- **Fix:** The restored time becomes a base the ticker adds to; `resetToInitial` clears it.
- **Files modified:** `ui/game/GameViewModel.kt`
- **Committed in:** `f75bda3` (the test asserts the restored value) / `a6fbef2` (impl)

**4. [Rule 2 - Missing Critical] `clearSnapshot` on a Classic solve too**
- **Found during:** Task 1
- **Issue:** The plan's behaviour line says "a Challenge Won calls clearSnapshot(mode)", but D-05 ("cleared on solve") is mode-agnostic — a Classic solve leaving its snapshot behind is exactly the stale-Continue bug T-05-12 describes, pointing at a board that is already over.
- **Fix:** The clear sits in the mode-agnostic part of the Won latch (the Challenge-only `advanceToNext()` stays gated beside it). `solvingClearsTheModesSnapshot` runs on a **Classic** game.
- **Files modified:** `ui/game/GameViewModel.kt`
- **Committed in:** `a6fbef2`

**5. [Rule 2 - Missing Critical] Render + consumer-side fail-safe tests the plan did not list**
- **Found during:** Task 2
- **Issue:** The plan's Task-2 verification (`HomeViewModelTest` + `NavFlowTest`) would pass even if `HomeScreen` never rendered a Continue tile — `NavFlowTest` replaces Home with a stub destination, and the VM test only proves the state fields. The same "invisible feature" trap 05-06 hit.
- **Fix:** 3 `HomeScreenTest` render cases (nothing saved → no tile; a Classic save → a tile carrying its own board id; both modes → both tiles, each resuming its own), plus a `NavFlowTest` case proving the plain Challenge tile still navigates with `resume = false` (entering the ladder must not silently resurrect a saved game) and a `HomeViewModelTest` case proving an unreadable blob hides the affordance rather than throwing.
- **Committed in:** `4aa33bb`

**6. [Trivial] `HomeContent` typealias**
- The `homeContent` slot reached five lambdas; it now uses a private typealias mirroring the existing `ResultsContent` precedent in the same file.

**7. [Trivial] Continue affordances are their own tiles**
- The plan suggested "mark [the Challenge tile] resumable". Two dedicated tiles were used instead (see key-decisions): the Challenge tile's "Continue — Level N" copy and its 05-06 test stay untouched, and the two intents stay separable — which the new `playingChallengeFreshNeverAsksForAResume` case now enforces.

---

**Total deviations:** 5 auto-fixed (2 blocking, 3 missing-critical) + 2 trivial
**Impact on plan:** No scope creep — no data-layer, settings or results code was touched. Deviations 3–5 close gaps that would have shipped as "green but broken"; the plan's own acceptance criteria are all met as written.

## Verification

| Gate | Result |
|------|--------|
| `./gradlew :app:testDebugUnitTest --tests "*GameViewModelResumeTest"` | PASS (9 tests, 0 failures) |
| `./gradlew :app:testDebugUnitTest --tests "*HomeViewModelTest"` | PASS (12 tests) |
| `./gradlew :app:testDebugUnitTest --tests "*HomeScreenTest"` | PASS (4 tests) |
| `./gradlew :app:testDebugUnitTest --tests "*NavFlowTest"` | PASS (8 tests) |
| `./gradlew check` (both modules: all unit tests + detekt + ktlint + dpSpLiteralCheck + lint) | BUILD SUCCESSFUL |
| `./gradlew :app:assembleDebug` (Hilt graph resolves both new dependencies) | BUILD SUCCESSFUL |
| `grep -n "ON_STOP" ui/game/GameRoute.kt` | present (must-have artifact) |
| `grep -n "resume" navigation/Routes.kt` | present (must-have artifact) |

## Issues Encountered

- Two style-gate stumbles, both caught before their commits: ktlint's "multiline expression should start on a new line" on the restore `let`, and import ordering (`kotlin.time.Duration.Companion.seconds` sorts before `TestTimeSource`). Running `:app:ktlintCheck :app:detekt` before every commit remains the cheap habit this project rewards.
- `assertDoesNotExist()` is a `SemanticsNodeInteraction` member, not an importable top-level assertion — the stray import broke the test compile.
- Virtual-time note for future timer tests: `advanceTimeBy(1_000)` does **not** fire a `delay(1_000)` scheduled at that instant (the boundary is exclusive). The fixture advances 1_100 ms per intended tick.

## Known Stubs

None. Every seam this plan introduced has a production caller and a test: `onLeaveScreen()` is called by `GameRoute`'s `ON_STOP` observer, `Gameplay.resume` is set by the two Home Continue routes, and both `HomeUiState` resume fields are rendered by `HomeScreen`.

The restored Challenge game deliberately has no recorded solution line (see key-decisions) — a documented behaviour, not a stub: hints still work via the Solver.

## Threat Flags

None — no new network, auth, file-access or schema surface. The plan's register is fully addressed:

| Threat ID | Disposition | Status |
|-----------|-------------|--------|
| T-05-03 (corrupt blob crashes Continue) | mitigate | Done — absent and forged snapshots both fall back to the normal load; on Home an unreadable blob hides the affordance. 3 tests. |
| T-05-04 (forged moves applied on restore) | mitigate | Done — `fromDto` re-validates every move (05-03); the ViewModel restores only what it returns, and rejects the whole history otherwise. |
| T-05-12 (stale Continue survives a solve) | mitigate | Done — Playing-only saves + `clearSnapshot` on solve/restart, asserted for both modes; Home drops the affordance when the mode leaves the list. |
| T-05-07 (PII in snapshot) | accept | Unchanged — board geometry, moves and elapsed only. |
| T-05-11 (tampered stored Challenge level) | mitigate | Unchanged — `clampedChallengeLevel` still clamps at the ViewModel boundary; the resume path does not use the level to rebuild anything. |

## User Setup Required

None — no external service configuration required.

## Checkpoint Outcome

**Task 3 (`checkpoint:human-verify`, gate="blocking") was NOT executed — it is awaiting the user.**

Both automatable tasks are complete and committed. The checkpoint covers the two Phase-5 behaviours that cannot be asserted JVM-side (physical haptic actuation, and a real process-death cold start), plus the end-to-end phase sweep. No verification result is recorded here because none has been obtained; the phase must not be marked verified until the user reports back.

One wording note for whoever runs it: the plan's script says "tap the Continue affordance". On Home the affordances are titled **"Continue Classic"** and **"Continue Challenge"** (tiles above the two mode tiles). The Challenge *mode* tile still reads "Continue — Level N" and starts that level fresh — the two are deliberately distinct.

## Next Phase Readiness

- **05-08 (phase verification)** can run the full suite as-is; every PERS-03 seam is JVM-covered except the two manual-only behaviours above.
- A future Continue surface anywhere else needs only the `HomeUiState` pattern: collect `observeResumeModes()`, carry whatever the resume needs in the state, hand it to the click lambda.
- **Carry forward:** `GameViewModel` now has three route-derived properties (`levelId`, `challengeLevel`, `resume`) and two persistence dependencies; the next plan touching its `init` should preserve the restore-first ordering, since the no-regeneration guarantee rides on it.

**Blockers:** None (the checkpoint is a gate, not a blocker).

## Self-Check: PASSED

- Created file verified present and tracked: `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelResumeTest.kt`.
- All 3 task commits verified in `git log`: `f75bda3`, `a6fbef2`, `4aa33bb`.
- `git diff --diff-filter=D HEAD~3 HEAD` reports no deleted files; working tree clean apart from the intentionally-untracked `local.properties`.

---
*Phase: 05-persistence-resume-settings*
*Completed: 2026-08-25*
