---
phase: 05-persistence-resume-settings
plan: 06
subsystem: ui
tags: [challenge, navigation, mvvm, datastore, state-hoisting, persistence]

# Dependency graph
requires:
  - phase: 05-persistence-resume-settings
    plan: 02
    provides: DataStore-backed ChallengeProgress (currentLevel StateFlow + advanceToNext)
  - phase: 05-persistence-resume-settings
    plan: 04
    provides: 3-arg AppNavHost homeContent slot + settingsContent slot
  - phase: 03-challenge-mode
    provides: Challenge gameplay path, clamped challengeLevel nav arg, popUpTo(Home) ladder idiom
provides:
  - Advance-on-solve wiring — a Challenge Won calls ChallengeProgress.advanceToNext() (CHAL-05 logic half)
  - HomeUiState.challengeCurrentLevel collected from ChallengeProgress.currentLevel
  - "Home Challenge tile labelled 'Continue — Level N' that enters at the persisted level"
  - "AppNavHost homeContent onPlayChallenge widened to (level: Int) -> Unit; CHALLENGE_ENTRY_LEVEL deleted"
affects: [05-07-resume-snapshot, 05-08-phase-verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Persisted scalar → UiState → hoisted nav lambda: the screen carries the level, the NavHost never invents one"
    - "MockK relaxed stand-in for a final @Singleton holder (ChallengeProgress) in ViewModel specs"
    - "Formatted stringResource(R.string.x, arg) for level-bearing tile copy (QUAL-03)"

key-files:
  created:
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelChallengeAdvanceTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/home/HomeScreenTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeRoute.kt
    - app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt
    - app/src/main/res/values/strings.xml
    - app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt

key-decisions:
  - "advanceToNext() fires inside the existing `status == Won && !finishedEmitted` latch, gated on clampedChallengeLevel != null — exactly-once per solve, never for Classic, never for Stuck (D-02)"
  - "The Home tile entry reuses the existing toChallengeBoard(level) helper (popUpTo<Home>) instead of a bare navigate, so every Challenge hop — entry, Next level, Retry — shares one bounded-back-stack idiom (T-03-11)"
  - "onPlayChallenge became (level: Int) -> Unit end to end (Screen → Route → NavHost slot): the level travels with the click from the UiState, so the NavHost holds no level state and the screen stays stateless"
  - "HomeUiState.challengeCurrentLevel defaults to ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL rather than a new literal 1 — one source of truth for the fail-open level (T-05-02)"
  - "The tile's supporting line replaces 'Endless levels' with 'Continue — Level %1$d' — the persisted progress is now visible, not merely used"
  - "ChallengeProgress is stubbed with MockK (relaxed) rather than made `open`: no production class was widened for testability"

patterns-established:
  - "A persisted scalar is only 'wired' when three things exist: a writer call site, a UiState field, and a render/nav consumer — each covered by its own failing-first test"
  - "Nav-level level arguments are asserted with a non-default value (7) so a hardcoded fallback cannot pass by coincidence"

requirements-completed: [PERS-02, CHAL-05]

# Metrics
duration: 21min
completed: 2026-08-25
---

# Phase 05 Plan 06: Challenge Continue Wiring Summary

**The persisted Challenge level is now load-bearing at both ends: solving level N writes N+1 through the previously-uncalled `ChallengeProgress.advanceToNext()`, and the Home tile reads that stored level back as "Continue — Level N" and enters Challenge there — the hardcoded `CHALLENGE_ENTRY_LEVEL = 1` is gone.**

## Performance

- **Duration:** ~21 min
- **Tasks:** 2 (both TDD: RED → GREEN)
- **Files modified:** 13 (2 created, 11 modified)

## Accomplishments

- **Advance-on-solve (CHAL-05 logic half):** `GameViewModel` injects `ChallengeProgress` and calls `advanceToNext()` inside the existing Won finalize branch, gated on `clampedChallengeLevel != null`. The pre-existing `finishedEmitted` latch makes it exactly-once per solve; a Classic solve and a Stuck Challenge board never advance (Phase-3 D-02: only a solve unlocks N+1).
- **Continue-from-persisted (CHAL-05 UI half):** `HomeViewModel` collects `ChallengeProgress.currentLevel` into a new `HomeUiState.challengeCurrentLevel`; the Challenge tile renders `Continue — Level N` from it and hands that level to a widened `onPlayChallenge(level: Int)` on click.
- **The hardcode is deleted, not shadowed:** `AppNavHost`'s `private const val CHALLENGE_ENTRY_LEVEL = 1` is removed and the Home tile now routes through the existing `toChallengeBoard(level)` helper, so Challenge entry inherits the same `popUpTo<Home>()` bounded-ladder idiom as Next level / Retry (T-03-11). `grep -rn "CHALLENGE_ENTRY_LEVEL" app/src/main` returns nothing.
- **Every link is proven by a test that can actually fail:** `GameViewModelChallengeAdvanceTest` (3 cases) pins the advance truth table with a MockK stand-in; `HomeViewModelTest` (+3 cases, 7 total) proves the UiState mirrors the store, fails open to level 1 before the first emission, and re-emits on an advance; `HomeScreenTest` (new) proves the rendered tile shows *and enters at* level 7; `NavFlowTest` now drives level **7** through the Home tile, so a reintroduced hardcoded 1 fails the assertion instead of matching it.
- **Threat dispositions hold:** T-05-11 (a tampered/corrupt stored level) is still absorbed by `GameViewModel`'s `clampedChallengeLevel` at the VM boundary — documented at the new nav call site; T-05-02 degrades to "Level 1" via the shared `DEFAULT_CURRENT_LEVEL` default, asserted by the before-first-emission case.

## Task Commits

Each task was committed atomically (TDD: test → feat):

1. **Task 1: Advance persisted currentLevel on Challenge solve**
   - `279bdb4` (test) — RED: Challenge Won advances once; Classic Won and Stuck do not
   - `239fa19` (feat) — GREEN: `ChallengeProgress` injection + the gated `advanceToNext()` call
2. **Task 2: Home Challenge tile reads persisted currentLevel; drop the hardcode**
   - `c822cad` (test) — RED: UiState mirroring/fail-open/re-emission, tile label + entry level, persisted level through the NavHost
   - `bd67d9b` (feat) — GREEN: `challengeCurrentLevel`, the collect, the `Continue — Level N` tile, the widened lambda, the deleted constant

No REFACTOR commit was needed — both implementations landed at their final shape.

## Files Created/Modified

- `ui/game/GameViewModel.kt` — new `challengeProgress` constructor dependency; the advance call inside the Won latch (Challenge only).
- `ui/home/HomeViewModel.kt` — new `challengeProgress` dependency and a second `init` collect mirroring `currentLevel` into state.
- `ui/home/HomeUiState.kt` — added `challengeCurrentLevel: Int`, defaulted to `ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL`.
- `ui/home/HomeScreen.kt` — `onPlayChallenge: (level: Int) -> Unit`; `ChallengeTile(currentLevel, onPlayChallenge)` renders the formatted continue line and invokes `onPlayChallenge(currentLevel)`.
- `ui/home/HomeRoute.kt` — forwards the widened lambda (still no ViewModel/controller below the route).
- `navigation/AppNavHost.kt` — `homeContent` slot's `onPlayChallenge` widened to `(level: Int) -> Unit`; Home tile routes via `toChallengeBoard(level)`; `CHALLENGE_ENTRY_LEVEL` deleted and the helper's KDoc updated to cover entry + clamping.
- `res/values/strings.xml` — `challenge_tile_supporting` ("Endless levels") replaced by `challenge_tile_continue` ("Continue — Level %1$d").
- `ui/game/GameViewModelChallengeAdvanceTest.kt` — **created**; 3 cases over a fixed-board `LevelRepository` served through both load paths, so Classic vs Challenge differs only by the route arg.
- `ui/home/HomeScreenTest.kt` — **created**; 1 Robolectric render case (label + entry level).
- `ui/home/HomeViewModelTest.kt` — 3 new cases + a `viewModelFor` helper; class doc widened.
- `navigation/NavFlowTest.kt` — 4 `homeContent` call sites updated for the level-carrying lambda; `CHALLENGE_ENTRY_LEVEL` (1) renamed to `PERSISTED_CHALLENGE_LEVEL` (7).
- `ui/game/GameViewModelTest.kt`, `ui/game/GameFlowTest.kt` — absorb the new `GameViewModel` dependency with a relaxed stand-in.

## Decisions Made

See `key-decisions` in the frontmatter. The load-bearing one is routing Home's Challenge entry through the existing `toChallengeBoard(level)` helper rather than a bare `navigate`. From `Home` the two are behaviourally identical (`popUpTo<Home>` with `inclusive = false` pops nothing when Home is already the top), but funnelling every Challenge hop — entry, Next level, Retry — through one helper means the bounded-back-stack guarantee (T-03-11) has exactly one implementation to keep correct, and the clamp/ladder documentation lives in one place.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Created the worktree's missing `local.properties`**
- **Found during:** Task 1 (first Gradle run)
- **Issue:** `local.properties` is gitignored, so a fresh worktree fails every Gradle invocation with "SDK location not found" (the same blocker 05-02 and 05-04 hit).
- **Fix:** Wrote `sdk.dir=/Users/ermanergoz/Library/Android/sdk` into the worktree root.
- **Files modified:** `local.properties` (gitignored — **not committed**, by design).
- **Committed in:** n/a (intentionally untracked).

**2. [Rule 3 - Blocking] Updated the two existing `GameViewModel` test harnesses**
- **Found during:** Task 1 (GREEN)
- **Issue:** Adding the `challengeProgress` constructor parameter is source-breaking for `GameViewModelTest.viewModelFor` and `GameFlowTest`'s direct construction, so the test source set no longer compiled.
- **Fix:** Both pass `mockk(relaxed = true)` with a comment pointing at the dedicated advance spec — neither file's behaviour changed.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt`, `app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt`
- **Verification:** full `:app:testDebugUnitTest` green.
- **Committed in:** `239fa19`

**3. [Rule 2 - Missing Critical] Added `HomeScreenTest` to prove the tile actually renders and uses the level**
- **Found during:** Task 2 (RED)
- **Issue:** The plan's verification (`HomeViewModelTest` + `NavFlowTest`) would pass even if `HomeScreen` ignored `challengeCurrentLevel` entirely — the VM test only proves the state field, and `NavFlowTest` replaces Home with a stub destination. That is exactly the "invisible feature" trap this plan exists to close: the tile could read "Level 1" forever and still be green.
- **Fix:** A 1-case Robolectric render test asserting the real `HomeScreen` shows `Continue — Level 7` and that clicking it invokes `onPlayChallenge(7)`.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/home/HomeScreenTest.kt` (created)
- **Verification:** `:app:testDebugUnitTest --tests "*HomeScreenTest"` — 1 test, 0 failures.
- **Committed in:** `c822cad` (RED) / `bd67d9b` (GREEN)

**4. [Rule 2 - Missing Critical] `NavFlowTest`'s Challenge level constant changed from 1 to 7**
- **Found during:** Task 2 (RED)
- **Issue:** The test's `CHALLENGE_ENTRY_LEVEL = 1` mirrored the production hardcode, so asserting `challengeLevel == 1` would still pass if the NavHost kept substituting a hardcoded 1 — the assertion could not detect the very regression this plan removes.
- **Fix:** Renamed to `PERSISTED_CHALLENGE_LEVEL = 7` and used it at every Home/Retry call site, so a hardcoded fallback fails loudly.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt`
- **Verification:** `:app:testDebugUnitTest --tests "*NavFlowTest"` — 5 tests, 0 failures.
- **Committed in:** `c822cad` (RED) / `bd67d9b` (GREEN)

---

**Total deviations:** 4 auto-fixed (2 blocking, 2 missing-critical)
**Impact on plan:** All four served the plan's own acceptance criteria (compile, and verifications that can actually fail). No scope creep — the resume snapshot (`data/resume/*`), results recording (`ResultsViewModel`) and `ChallengeProgress.kt` itself were never touched, and no production class was widened for testability.

## Verification

| Check | Result |
|-------|--------|
| `./gradlew :app:testDebugUnitTest --tests "*GameViewModelChallengeAdvanceTest"` | PASS (3 tests) |
| `./gradlew :app:testDebugUnitTest --tests "*HomeViewModelTest"` | PASS (7 tests) |
| `./gradlew :app:testDebugUnitTest --tests "*HomeScreenTest"` | PASS (1 test) |
| `./gradlew :app:testDebugUnitTest --tests "*NavFlowTest"` | PASS (5 tests) |
| `./gradlew :app:testDebugUnitTest` (full suite) | PASS |
| `./gradlew :domain:test` | PASS |
| `./gradlew :app:dpSpLiteralCheck :app:ktlintCheck :app:detekt` | PASS |
| `./gradlew :app:assembleDebug` (Hilt graph resolves both VMs) | PASS |
| `grep -rn "CHALLENGE_ENTRY_LEVEL" app/src/main` | no matches (hardcode removed) |

## Issues Encountered

- None beyond the known fresh-worktree `local.properties` gap (deviation 1) and the expected `homeContent`-slot / `GameViewModel`-constructor compile breaks, both anticipated by the 05-02 and 05-04 handoffs.

## Known Stubs

None introduced by this plan. The two seams 05-02 flagged as unwired are both closed: `advanceToNext()` now has a production caller, and the Home entry level is sourced from persistence. PERS-02 / CHAL-05 are now user-visible, not merely stored.

## Threat Flags

None. No new network, auth, file-access or schema surface was introduced — the only widened surface is an in-process Compose lambda parameter. The persisted level crossing into nav is the register's existing T-05-11, still mitigated by the unchanged `clampedChallengeLevel` clamp at the `GameViewModel` boundary (now documented at the call site).

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- **05-07 (resume snapshot)** re-touches both files this plan edited: `GameViewModel`'s Won finalize branch now contains the Challenge advance (put snapshot-clearing beside it, inside the same `finishedEmitted` latch), and `AppNavHost`'s `homeContent` slot is now `(onPlayClassic, onPlayChallenge: (Int) -> Unit, onOpenSettings)` — a Resume entry point should be appended in the same style, with `NavFlowTest`'s four `homeContent` call sites widened again.
- A Challenge "Continue" surface anywhere else needs only `HomeUiState.challengeCurrentLevel`'s pattern: collect `ChallengeProgress.currentLevel`, hand the value to the click lambda.
- The end-to-end loop (solve → advance → relaunch → continue) is JVM-verified; the phase-verification playtest should still confirm it once on-device across a real process death.

## Self-Check: PASSED

Both created files verified present on disk; all 4 task commits (`279bdb4`, `239fa19`, `c822cad`, `bd67d9b`) verified in `git log`. `git log --diff-filter=D` over the plan's commit range reports no deleted files, and the working tree is clean apart from the intentionally-untracked `local.properties`.

---
*Phase: 05-persistence-resume-settings*
*Completed: 2026-08-25*
