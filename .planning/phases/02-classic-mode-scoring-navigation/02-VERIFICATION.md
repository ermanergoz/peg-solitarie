---
phase: 02-classic-mode-scoring-navigation
verified: 2026-06-30T00:00:00Z
status: gaps_found
score: 5/6 must-have truths verified
overrides_applied: 0
gaps:
  - truth: "Every non-trivial board has a committed recorded winning move-list that replays through RulesEngine down to exactly one peg (D-19 — proves 3-star is reachable without shipping a Solver)."
    status: failed
    reason: >
      Diamond-41 ships in the playable Classic catalog (ClassicLevelSource.kt lines
      64–67 include ClassicLevelIds.DIAMOND41) but has no committed solvability fixture
      and no SolutionTest replay. SolutionFixtures.ALL explicitly omits Diamond-41.
      Critically, Diamond41Layout's KDoc in Boards.kt (line 131) falsely claims
      "validated by the authoring solver and locked with a recorded-solution fixture
      (D-19)" — an active false statement in production code. Only 4 of 5 non-trivial
      boards have proven recorded solutions (english33, european37, asymmetric39,
      wiegleb45). The fifth board (Diamond-41) is shipped as a player-selectable level
      without any computational proof that start position (4,2) reduces to one peg.
    artifacts:
      - path: "domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Boards.kt"
        issue: "Diamond41Layout KDoc (line 131) falsely claims validated fixture exists"
      - path: "domain/src/test/resources/golden/solutions/"
        issue: "Missing diamond41.txt — only english33.txt, european37.txt, asymmetric39.txt, wiegleb45.txt committed"
      - path: "domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/SolutionTest.kt"
        issue: "No replay test for Diamond-41; footprint/peg-count sanity only (not solvability)"
    missing:
      - "Generate and commit domain/src/test/resources/golden/solutions/diamond41.txt using a pagoda-pruned AuthoringSolver"
      - "Add diamond41RecordedSolutionReducesToOnePeg replay test to SolutionTest.kt"
      - "Add Diamond-41 Fixture to SolutionFixtures.ALL in AuthoringSolver.kt"
      - "Correct Diamond41Layout KDoc in Boards.kt (line 131) to truthfully state the proof is deferred"
      - "Alternative: remove Diamond-41 from DefaultClassicLevelSource catalog until proof exists"
human_verification:
  - test: "Launch the app on a fresh install and verify the onboarding overlay appears over the Home screen with three illustrated jump-rule steps (step_1/2/3 captions) and 'Got it' / 'Skip tutorial' buttons. Dismiss with 'Got it'. Re-launch and verify it does NOT re-appear."
    expected: "First launch shows the overlay. Subsequent launches skip it. The How-to-play (Info icon) re-opens it any time."
    why_human: "DataStore persistence and the conditional overlay display require a device/emulator; no Robolectric test exercises the full first-launch + persist + re-launch sequence end-to-end."
  - test: "From Home, tap Classic, select Diamond-41 from the catalog, and play the board to a terminal state (win or stuck). Verify the game reaches a terminal state and navigates to Results."
    expected: "Game runs correctly on the 41-hole diamond footprint. The board renders without clipping. Pegs jump orthogonally. A terminal state (1 peg or stuck) navigates to Results with correct star/score values."
    why_human: "Diamond-41's solvability from start (4,2) is unproven computationally. Manual play is the only current way to spot-check whether the board can reach 1 peg. If the board gets stuck with many pegs remaining every attempt, the start position may be unsolvable."
  - test: "Navigate the full spine: Home → Classic catalog → select any board → play at least one move → verify mm:ss timer ticks → play to completion → verify Results screen shows correct stars, score, pegs remaining, time."
    expected: "Timer starts at first move and increments once per second. Results show: headline (Perfect!/Solved!/Out of moves based on stars), star row (filled/outline), SCORE/TIME/PEGS LEFT metrics, four action buttons."
    why_human: "Visual verification of timer animation, star row rendering, and metric layout requires a running app."
  - test: "On the Home screen verify the Challenge tile has a 'Coming soon' badge, is visually disabled (greyed/desaturated), and does not navigate anywhere when tapped."
    expected: "Tapping Challenge tile does nothing. The badge reads 'Coming soon' on a secondaryContainer background."
    why_human: "The disabled Card with enabled=false is a UI behavior check — automated tests only assert the catalog list order; the disabled-tap no-op requires UI interaction."
---

# Phase 02: Classic Mode + Scoring + Navigation Verification Report

**Phase Goal:** Players can launch the app to a Home screen, choose one of five classic boards, play it to completion with correct rules, and land on a results screen showing stars and a ranking score — establishing the complete five-screen navigation flow and the LevelRepository seam (with the Challenge source stubbed) so the generator slots in behind it next phase.

**Verified:** 2026-06-30
**Status:** gaps_found
**Re-verification:** No — initial verification

---

## Goal Achievement

### Observable Truths (Roadmap Success Criteria + Plan Must-Haves)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| SC-1 | Player can browse a catalog of all five classic boards and select one to play | ✓ VERIFIED | `DefaultClassicLevelSource` maps all 5 boards in D-18 order; `ClassicCatalogScreen` renders a `LazyVerticalGrid` with 5 tappable cards; `LevelRepositoryTest` asserts catalog size == 5 in correct id order. |
| SC-2 | Player can play any selected classic board to completion with correct orthogonal jump rules | ✓ VERIFIED (mechanics) | `RulesEngine` (unchanged from Phase 1) enforces orthogonal-only jumps on all board specs; `BoardGeometry` derives per-board grid dimension (7/8/9) from spec bounds; `GameViewModel` loads each board by `levelId`. 4 of 5 boards are proven solvable to 1 peg via committed fixtures; Diamond-41 is mechanically playable but solvability from start (4,2) is unproven — see gap below. |
| SC-3 | Results screen shows stars, pegs remaining, elapsed time, and a numeric score | ✓ VERIFIED | `ResultsScreen.kt`: tiered headline (`displaySmall`), 3-star row (filled/outline), SCORE/TIME/PEGS LEFT metric block; all values read from route args computed by `ScoreCalculator.evaluate()`; `ResultsScreenTest` (Robolectric `createComposeRule`) asserts all four metrics are displayed. |
| SC-4 | Player can navigate between Home, mode selection, gameplay, and results screens | ✓ VERIFIED | `AppNavHost` in `MainActivity` wires 5 `@Serializable` destinations: `composable<Home>`, `<ClassicCatalog>`, `<Gameplay>`, `<Results>`. `NavFlowTest` (Robolectric, real `NavController`) drives the full Home → Catalog → Gameplay → Results spine and asserts each destination reached. |
| SC-5 | A first-time player sees a brief, skippable how-to-play explanation of the jump rule | ✓ VERIFIED | `OnboardingOverlay` shows 3 illustrated steps (step_1/2/3 captions), "Got it" primary Button, "Skip tutorial" TextButton; gated by `HomeViewModel` collecting `onboardingSeen: Flow<Boolean>` from DataStore; `HomeViewModelTest` asserts first-launch visible, dismiss persists. |
| P-M1 | Every non-trivial board has a committed recorded winning move-list proving solvability to one peg (D-19) | ✗ FAILED | **4 of 5 boards proven** (english33.txt, european37.txt, asymmetric39.txt, wiegleb45.txt in `golden/solutions/`). **No `diamond41.txt` exists.** `SolutionTest` has no Diamond-41 replay. `SolutionFixtures.ALL` explicitly excludes Diamond-41. `Diamond41Layout` KDoc (Boards.kt line 131) falsely claims the fixture exists — an active false statement in shipped code. Diamond-41 IS in the catalog (`ClassicLevelSource.kt` lines 64-67). |

**Score:** 5/6 truths verified (SC-1 through SC-5 all pass; plan must-have P-M1 FAILED)

---

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculator.kt` | Pure-domain stars() + score() + evaluate() | ✓ VERIFIED | `object ScoreCalculator` with all three functions; references only `ScoreParams.*`; zero Android imports. |
| `domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreParams.kt` | Scoped scoring constants | ✓ VERIFIED | `object ScoreParams` with `THREE_STAR_MAX_PEGS=1`, `TWO_STAR_MAX_PEGS=3`, `MIN_STARS=1`, `PEG_VALUE=100`, `TIME_BONUS_CAP=5000`, `DECAY_PER_SECOND=10`, `THREE_STARS=3`, `TWO_STARS=2`. |
| `domain/src/test/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculatorTest.kt` | SCOR-01/02 coverage | ✓ VERIFIED | 6 behavior assertions covering star bands, time-decay, floor, and full-bonus score. |
| `domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Boards.kt` | 4 new board specs + initials | ✓ VERIFIED | `European37`, `Asymmetric39`, `Diamond41`, `Wiegleb45` with correct hole counts (37/39/41/45) and start vacancies. |
| `domain/src/test/resources/golden/solutions/` | One fixture per non-trivial board | ✗ PARTIAL | `english33.txt`, `european37.txt`, `asymmetric39.txt`, `wiegleb45.txt` present. **`diamond41.txt` is missing.** |
| `domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/SolutionTest.kt` | Replay test per board proving solvability | ✗ PARTIAL | Replay tests for 4 boards. **No Diamond-41 replay test.** Footprint/peg-count sanity covers all 5. |
| `app/src/main/java/com/app/pegsolitaire/data/LevelRepository.kt` | LevelRepository interface + DefaultLevelRepository | ✓ VERIFIED | `interface LevelRepository` with `classicCatalog()`, `loadClassic()`, `loadChallenge()`; `DefaultLevelRepository` delegates. |
| `app/src/main/java/com/app/pegsolitaire/data/ClassicLevelSource.kt` | Real Classic source, fail-fast load | ✓ VERIFIED | `DefaultClassicLevelSource` with 5 boards in D-18 order; `requireNotNull` fail-fast on unknown id; display names from `ctx.getString(R.string.board_*)`. |
| `app/src/main/java/com/app/pegsolitaire/di/DataModule.kt` | Hilt @Binds wiring | ✓ VERIFIED | Abstract `@Module @InstallIn(SingletonComponent)` with `@Binds` for LevelRepository, ClassicLevelSource, ChallengeLevelSource. |
| `app/src/main/java/com/app/pegsolitaire/navigation/Routes.kt` | 5 @Serializable type-safe routes | ✓ VERIFIED | `data object Home`, `data object ClassicCatalog`, `data class Gameplay(levelId)`, `data class Results(levelId,stars,pegsRemaining,elapsedSeconds,score)` — all `@Serializable`. |
| `app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt` | Type-safe NavHost wiring all 5 destinations | ✓ VERIFIED | `NavHost(startDestination = Home)` with `composable<Home/ClassicCatalog/Gameplay/Results>`; correct back-stack and Results actions. |
| `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` | levelId-driven board load + timer + one-shot finish | ✓ VERIFIED | `savedStateHandle.toRoute<Gameplay>().levelId`; `levelRepository.loadClassic(levelId)`; `TimeSource` ticker; `Channel(CONFLATED)` + `finishedEmitted` guard. `GameOverDialog.kt` deleted. |
| `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt` | Full results: headline, star row, metrics, 4 actions | ✓ VERIFIED | `displaySmall` headline, `StarRow` (filled/outline), SCORE/TIME/PEGS LEFT, Next/Replay/Catalog/Home buttons; all values from route args. |
| `app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt` | Home with Classic/Challenge tiles + onboarding | ✓ VERIFIED | Enabled Classic `Card` with `onClick = onPlayClassic`; disabled Challenge `Card` (enabled=false) with `ComingSoonBadge`; `OnboardingOverlay` hosted when `onboardingVisible`. |
| `app/src/main/java/com/app/pegsolitaire/data/OnboardingRepository.kt` | DataStore-backed onboardingSeen + fail-open | ✓ VERIFIED | `booleanPreferencesKey("onboarding_seen")`; `.catch { emit(emptyPreferences()) }` fail-open; `markSeen()` persists. |
| `app/src/main/res/values/dimens.xml` | 6 Phase-2 dimens tokens | ✓ VERIFIED | `content_max_width`, `mode_tile_min_height`, `catalog_grid_min_cell_width`, `catalog_preview_size`, `results_star_size`, `onboarding_illustration_size` — all multiples of 4. |
| `app/src/main/res/values/strings.xml` | All Phase-2 strings copy | ✓ VERIFIED | `board_*` names, `coming_soon`, `how_to_play`, `results_headline_*`, `results_label_*`, `action_*`, `onboarding_got_it`, `onboarding_skip`, `onboarding_step_1/2/3`, `holes_count` plural — all confirmed present. |

---

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `GameViewModel` | `LevelRepository.loadClassic` | `savedStateHandle.toRoute<Gameplay>().levelId` | ✓ WIRED | Line 67-69 in GameViewModel.kt: `levelId = savedStateHandle.toRoute<Gameplay>().levelId`; `initialBoard = levelRepository.loadClassic(levelId).initialBoard` |
| `GameRoute` | `Results(...)` one-shot navigation | `LaunchedEffect(Unit)` collecting `viewModel.finished` | ✓ WIRED | GameRoute.kt lines 38-50: `LaunchedEffect(Unit) { viewModel.finished.collect { result -> onFinished(Results(...)) } }` |
| `SolutionTest` | `RulesEngine` via Diamond-41 replay | fold committed moves through `RulesEngine.applyMove` | ✗ NOT WIRED | No Diamond-41 replay test exists. `SolutionFixtures.ALL` = [english33, european37, asymmetric39, wiegleb45] — Diamond-41 absent. |
| `Diamond41Layout` KDoc | claimed solvability fixture | "validated by the authoring solver and locked with a recorded-solution fixture (D-19)" | ✗ FALSE | Line 131 in Boards.kt falsely claims fixture exists. No fixture. No authoring solver support. |
| `ClassicLevelSource` | `ClassicLevelIds.DIAMOND41` | catalog map include | ✓ WIRED | Lines 64-67 confirm Diamond-41 IS shipped to players. |
| `AppNavHost` | All 5 destinations | `composable<Route>` entries | ✓ WIRED | AppNavHost.kt: `composable<Home>`, `<ClassicCatalog>`, `<Gameplay>`, `<Results>` all present with correct nav lambdas. |
| `HomeViewModel` | `OnboardingRepository.onboardingSeen` | `viewModelScope.launch { collect { seen -> ... } }` | ✓ WIRED | HomeViewModel.kt lines 39-42: collects `onboardingSeen` flow, updates `onboardingVisible = !seen`. |

---

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|-------------------|--------|
| `ResultsScreen` | `state.stars`, `state.score`, `state.pegsRemaining`, `state.elapsedSeconds` | `Results` typed route args → `ResultsViewModel.toRoute<Results>()` | `ScoreCalculator.evaluate()` in `GameViewModel.performJump()` | ✓ FLOWING |
| `ClassicCatalogScreen` | `state.boards` | `ClassicCatalogViewModel` → `levelRepository.classicCatalog()` | `DefaultClassicLevelSource` maps `Boards.*` specs | ✓ FLOWING |
| `HomeScreen` | `state.onboardingVisible` | `HomeViewModel` → `onboardingRepository.onboardingSeen` | DataStore `booleanPreferencesKey("onboarding_seen")` | ✓ FLOWING |
| `GameScreen` timer | `state.elapsedSeconds` | `GameViewModel` `viewModelScope` ticker | `timeSource.markNow().elapsedNow().inWholeSeconds` | ✓ FLOWING |

---

### Behavioral Spot-Checks

| Behavior | Check | Result | Status |
|----------|-------|--------|--------|
| All domain tests pass (scoring + solution replay) | `./gradlew :domain:test` | BUILD SUCCESSFUL, 34 tests, 0 failures | ✓ PASS |
| All app unit tests pass | `./gradlew :app:testDebugUnitTest` | BUILD SUCCESSFUL, 53 tests, 0 failures | ✓ PASS |
| Domain purity gate | `./gradlew :app:testDebugUnitTest --tests *DomainPurityTest*` | BUILD SUCCESSFUL | ✓ PASS |
| Debug APK builds | `./gradlew :app:assembleDebug` | BUILD SUCCESSFUL | ✓ PASS |
| detekt + ktlint | `./gradlew detekt ktlintCheck` | BUILD SUCCESSFUL | ✓ PASS |
| No `diamond41.txt` fixture | `ls golden/solutions/` | english33, european37, asymmetric39, wiegleb45 only | ✗ GAP CONFIRMED |
| False KDoc in Boards.kt | `grep "validated by the authoring solver" Boards.kt` | Line 131 found | ✗ GAP CONFIRMED |
| Diamond-41 IS in shipped catalog | `grep DIAMOND41 ClassicLevelSource.kt` | Lines 64-67 confirmed | ✗ GAP CONFIRMED (unproven board ships) |

---

### Probe Execution

Step 7c: SKIPPED — no `scripts/*/tests/probe-*.sh` files exist in this project; this is a Gradle/Kotlin project without standalone probe scripts.

---

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|------------|-------------|--------|---------|
| CLAS-01 | 02-04, 02-07 | Browse and select from catalog of 5 classic boards | ✓ SATISFIED | `DefaultClassicLevelSource` 5-board catalog; `ClassicCatalogScreen` LazyVerticalGrid; `LevelRepositoryTest` covers all 5 ids in order. |
| CLAS-02 | 02-02, 02-04, 02-05 | Play any classic board to completion with correct rules | ✓ SATISFIED (mechanics) | `RulesEngine` unchanged; `BoardGeometry` derives dims 7/8/9; `GameViewModel` loads by `levelId`. Caveat: Diamond-41 solvability to 1 peg unproven (see P-M1 gap). |
| SCOR-01 | 02-01, 02-06 | 1–3 stars at level end based on pegs remaining | ✓ SATISFIED | `ScoreCalculator.stars()` with D-01 bands; `ResultsScreen` renders `state.stars` filled icons. |
| SCOR-02 | 02-01, 02-05 | Elapsed time contributes to a separate numeric score | ✓ SATISFIED | `ScoreCalculator.score(pegsCleared, elapsedSeconds)` with time-decay formula; `GameViewModel` timer feeds `ScoreCalculator.evaluate()`. |
| SCOR-03 | 02-06 | Results screen shows stars, pegs remaining, time, and score | ✓ SATISFIED | `ResultsScreen` has tiered headline + StarRow + SCORE/TIME/PEGS LEFT metric block. |
| NAV-01 | 02-08 | Home screen with entry to Classic and Challenge modes | ✓ SATISFIED | `HomeScreen` renders ClassicTile (enabled) + ChallengeTile (disabled, "Coming soon"); Classic tile navigates to catalog. |
| NAV-02 | 02-03, 02-05, 02-08 | Navigate between Home, mode selection, gameplay, and results | ✓ SATISFIED | `AppNavHost` wires all 5 destinations; `NavFlowTest` drives and asserts the full spine. |
| ONB-01 | 02-08 | First-time player sees brief, skippable how-to-play | ✓ SATISFIED | `OnboardingOverlay` with 3 steps + Got it/Skip; `HomeViewModel` gates on DataStore `onboardingSeen`; fail-open on read error (D-16). |

---

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Boards.kt` | 131–132 | `Diamond41Layout` KDoc falsely claims "validated by the authoring solver and locked with a recorded-solution fixture (D-19)" | BLOCKER | Active false statement in production code. No fixture exists. Misleads every future maintainer into thinking Diamond-41 is proven solvable from (4,2). Flagged as CR-01 in the code review. |
| `domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculator.kt` | 39–40 | `elapsedSeconds * ScoreParams.DECAY_PER_SECOND` computes in `Int`; overflows at ~214M seconds, bypassing the `maxOf(0,…)` floor | WARNING | `ScoreCalculatorTest.scoreNeverDropsBelowThePegBaseRegardlessOfElapsedSeconds` passes `Int.MAX_VALUE` and reports PASS for the wrong reason (overflow wraps negative, making the bonus appear positive). The floor is correct for all real-play elapsed times (≤ seconds per game). Flagged as WR-01 in the code review. |
| `app/src/main/java/com/app/pegsolitaire/data/OnboardingRepository.kt` | 61–62 | `.catch { emit(emptyPreferences()) }` swallows all `Throwable`s, not just `IOException` | WARNING | Genuine programming errors (NullPointerException, ClassCastException) are silently degraded to "show onboarding again" rather than surfacing. Flagged as WR-02 in the code review. |
| `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt` | 81 | `order[(indexOf(currentId) + NEXT_OFFSET) % order.size]` has no empty-list guard | WARNING | `ArithmeticException` if `order` is empty. Unreachable today (catalog always 5 items) but unguarded. Flagged as WR-03 in the code review. |

---

### Human Verification Required

#### 1. First-run onboarding overlay end-to-end

**Test:** Launch the app on a fresh install (or clear app data). Verify the onboarding overlay appears on the Home screen showing three illustrated steps (onboarding_step_1/2/3 captions). Tap "Got it" to dismiss. Fully kill and re-launch the app. Verify the overlay does NOT appear. Then tap the Info icon in the top bar; verify the overlay re-appears and can be dismissed again.

**Expected:** Overlay shows on first launch. DataStore persists. Second launch skips overlay. Info icon re-opens overlay on demand.

**Why human:** DataStore persistence and the first-launch gate require a real device/emulator; no Robolectric test exercises the full persist + kill + re-launch sequence.

---

#### 2. Diamond-41 playability manual spot-check

**Test:** From the Classic catalog, select "Diamond-41". Observe the board renders correctly (41-hole diamond, no clipping). Play several moves. Observe that orthogonal jumps function correctly. Attempt to play to completion (1 peg or stuck). Note whether 1 peg is reachable.

**Expected:** Board renders and plays correctly. Reaching 1 peg would informally support that (4,2) is solvable. If consistently stuck with many pegs, it may indicate the start is not solvable.

**Why human:** This is the only currently available check for Diamond-41 solvability given no computational proof exists. Note: this does NOT substitute for the committed recorded-solution fixture required by D-19.

---

#### 3. Full spine visual walkthrough

**Test:** Launch → Home → tap Classic → catalog shows 5 boards in ascending hole-count order with mini BoardCanvas previews → select English-33 → play at least 3 moves → confirm mm:ss timer is ticking → complete the board → Results screen shows correct headline (based on star count), filled/outline star row, SCORE / TIME / PEGS LEFT values, and four action buttons → tap "Next board" and verify navigation to European-37 Gameplay.

**Expected:** All screens render correctly with M3 styling. Timer increments. Results show accurate data. Navigation works.

**Why human:** Visual rendering, timer animation, and Results layout correctness require a running app.

---

#### 4. Challenge tile disabled verification

**Test:** On the Home screen, tap the "Challenge" tile.

**Expected:** Nothing happens (no navigation, no error). The tile is visually greyed out with a "Coming soon" secondaryContainer badge.

**Why human:** The `enabled = false` Card property prevents navigation; requires UI interaction to confirm.

---

## Gaps Summary

**1 BLOCKER gap preventing full goal achievement:**

Diamond-41 ships in the playable Classic catalog with no solvability proof (no recorded-solution fixture, no SolutionTest replay), while the plan's D-19 must-have explicitly required every non-trivial board to have a committed winning move-list replayed through `RulesEngine` to one peg. This directly violates the plan's stated requirement and the project's core value ("puzzles must be solvable"). Compounding the issue, `Diamond41Layout`'s KDoc in `Boards.kt` (line 131) falsely claims the proof exists — an active false statement in production code that will mislead future maintainers.

The other 4 boards (English-33, European-37, Asymmetric-39, Wiegleb-45) have fully committed fixtures and passing SolutionTest replay proofs. The complete five-screen navigation flow, scoring, UI, and navigation are all correctly implemented and all 87 unit tests pass. The gap is narrowly scoped to Diamond-41's solvability certificate and the false KDoc.

**Fix options (in order of preference):**
1. Implement pagoda-function pruning in `AuthoringSolver`, run it to generate `diamond41.txt`, add Diamond-41 to `SolutionFixtures.ALL`, add the `diamond41RecordedSolutionReducesToOnePeg` replay test to `SolutionTest`, and correct the KDoc.
2. Remove Diamond-41 from `DefaultClassicLevelSource` (making it a 4-board Classic catalog) and correct the KDoc to accurately state the proof is pending.

**3 WARNING issues (not blocking, but should be tracked):**
- WR-01: Integer overflow in `ScoreCalculator.score()` at extreme elapsed times; misleading test.
- WR-02: `OnboardingRepository` swallows all Throwables (should only catch `IOException`).
- WR-03: `ResultsViewModel.nextIdFrom` has latent divide-by-zero on an empty catalog.

---

_Verified: 2026-06-30_
_Verifier: Claude (gsd-verifier)_
