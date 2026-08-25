---
phase: 03-challenge-generation-difficulty-curve
verified: 2026-07-02T00:00:00Z
status: passed
score: 18/18 must-haves verified
overrides_applied: 0
---

# Phase 3: Challenge Generation & Difficulty Curve Verification Report

**Phase Goal:** Challenge Generation & Difficulty Curve — Infinite, deterministic, guaranteed-solvable generated levels with scaling difficulty (project #1 risk).
**Verified:** 2026-07-02
**Status:** PASSED
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Regenerating Challenge level N twice yields the identical starting board and solution | VERIFIED | `ChallengeSolvabilityTest.regeneratingALevelYieldsTheIdenticalBoardAndSolution` passes for levels {1,2,3,10,100,1000}; `legalReverseMoves` sorts with identical `compareBy` as `RulesEngine`; PRNG index only against sorted `List` (no `buildSet`/`HashSet` feeding `nextInt`) |
| 2 | Every generated level's recorded solution folds its starting board to exactly one peg, each step legal via RulesEngine | VERIFIED | `ChallengeSolvabilityTest.everyGeneratedSolutionReplaysLegallyAndFoldsToOnePeg` asserts each `move in RulesEngine.legalMoves(board)` and `finalBoard.pegCount == 1`; solvable by construction via reverse-generation |
| 3 | Difficulty knobs increase monotonically and continuously with level number | VERIFIED | `DifficultyTierTest.solutionLengthIsMonotonicNonDecreasingAndStrictlyGrowsOverTheRange` covers 1..1000; `consecutiveSolutionLengthDeltasAreBoundedSoThereIsNoHardTierWall` asserts delta in 0..1; no stepped `when` range lookup in `DifficultyTiers.kt` |
| 4 | Trivial/degenerate/duplicate boards are rejected and reseeded deterministically | VERIFIED | `QualityGateTest` covers: min solution length, peg-density, branching floor, spread floor, dedup window — all gates pass for sampled levels; `rejectAndReseedStaysRegenerateIdenticalThroughTheGates` confirms determinism preserved |
| 5 | Freeform footprints are orthogonally connected and have ≤ 64 in-play cells | VERIFIED | `FreeformFootprints.isConnected` uses independent 4-neighbour flood-fill BFS; `size <= GeneratorConstants.MAX_IN_PLAY_CELLS (64)` gate in `FreeformFootprints.build`; `FreeformValidityTest` covers connectivity, size cap, intro-level gating |
| 6 | Challenge levels 1, 2, 10, 100, 1000 are byte-identical to their committed golden-master fixtures | VERIFIED | `ChallengeGeneratorGoldenTest.sampledChallengeLevelsAreByteIdenticalToTheirGoldenFixtures` uses `GoldenMasterAssert.assertMatches` verbatim; all 5 `.txt` fixtures exist at `domain/src/test/resources/golden/`; full gate green with NO `UPDATE_GOLDEN` flag |
| 7 | LevelRepository.loadChallenge(n) returns a solvable Level carrying the recorded solution and generatorVersion stamp | VERIFIED | `GeneratedChallengeLevelSource.load` maps `GeneratedLevel → Level` with `solution = generated.solution` and `generatorVersion = generated.generatorVersion`; `DefaultLevelRepository.loadChallenge` delegates to the source; `LevelRepositoryTest` asserts `generatorVersion == GENERATOR_VERSION` |
| 8 | loadChallenge(0) does not crash (levelNumber clamped to ≥1) | VERIFIED | `GeneratedChallengeLevelSource.load` clamps via `coerceIn(MIN_CHALLENGE_LEVEL, MAX_CHALLENGE_LEVEL)`; `LevelRepositoryTest` covers the clamp; `GameViewModel.clampedChallengeLevel` provides a second guard |
| 9 | A level/difficulty multiplier scales the numeric score without changing stars or the Classic score | VERIFIED | `ScoreCalculator.score/evaluate` accept `levelMultiplier: Int = DEFAULT_LEVEL_MULTIPLIER (1)` pure param; multiplier default keeps Classic byte-identical; `ScoreCalculatorTest` covers multiplier > 1 is strictly larger, stars unchanged |
| 10 | ChallengeProgress starts at level 1 and advances to N+1 on solve | VERIFIED | `ChallengeProgress` starts `MutableStateFlow(FIRST_CHALLENGE_LEVEL = 1)`; `advanceToNext()` increments; `ChallengeProgressTest` covers start/advance/reset (IN-02 note: production wiring to nav is Phase-5 scope; the route arg carries N+1 directly this phase) |
| 11 | A Challenge level loads and plays through the reused board, with a 'Level N' title in the gameplay top bar | VERIFIED | `GameViewModel` loads via `loadChallenge(clampedChallengeLevel)` off main thread; `GameScreen.GameTopBarTitle` renders `stringResource(R.string.challenge_level_indicator, challengeLevel)` when `challengeLevel != null`; `GameViewModelTest` covers challenge-load |
| 12 | Classic gameplay still shows the app-name title (title slot is context-aware, no if(challenge) leaking into board logic) | VERIFIED | `GameTopBarTitle` only branches on `challengeLevel` for the title text; board/selection/jump logic in `GameViewModel` and `GameScreen` is untouched — the load path branch in VM is for loading only, not mechanics |
| 13 | Finishing a Challenge level produces a level-scaled numeric score via the passed-in multiplier | VERIFIED | `GameViewModel.performJump` calls `scoreCalculator.evaluate(..., levelMultiplier = levelMultiplier)` where `levelMultiplier = DifficultyTiers.forLevel(clamped).scoreMultiplier`; Classic passes `DEFAULT_LEVEL_MULTIPLIER (1)` |
| 14 | A challengeLevel < 1 nav arg is clamped at the ViewModel boundary | VERIFIED | `GameViewModel.clampedChallengeLevel = challengeLevel?.coerceIn(MIN_CHALLENGE_LEVEL, MAX_CHALLENGE_LEVEL)`; `GameViewModelTest` asserts `challengeLevel = 0` clamps without crashing |
| 15 | A solved Challenge level shows exactly one filled primary 'Next level' action advancing to N+1 | VERIFIED | `ResultsActions` branches: `if (state.showNextLevel)` → `PrimaryAction(R.string.action_next_level) { onNextLevel(challengeLevel + 1) }`; `showNextLevel = challengeLevel != null && pegsRemaining == 1`; `ResultsViewModelTest` + `ResultsScreenTest` cover this path |
| 16 | A stuck Challenge level shows exactly one filled primary 'Retry' (same deterministic board) and no 'Next level' | VERIFIED | `ResultsActions`: stuck branch → `PrimaryAction(R.string.action_retry) { onRetry(challengeLevel) }` — same `challengeLevel` reloads identical board; `ResultsViewModel.tierFor` returns `STUCK` for `pegsRemaining != 1`; WR-03 fix committed in `487039e` |
| 17 | The Home Challenge tile is enabled and navigates into Challenge at level 1 | VERIFIED | `ChallengeTile` is an enabled `Card(onClick = onPlayChallenge)`; no `enabled = false` or `ComingSoonBadge`; `AppNavHost` wires `onPlayChallenge → Gameplay(challengeLevel = CHALLENGE_ENTRY_LEVEL = 1)`; `NavFlowTest` covers reachability |
| 18 | Classic Results is unchanged (Replay / Catalog / Home) | VERIFIED | `ResultsActions`: `if (challengeLevel == null)` → `ClassicActions(onReplay, onCatalog)` + neutral Home; nothing from Phase 3 modifies the Classic path; `ResultsViewModelTest` Classic branch covers this |

**Score:** 18/18 truths verified

### Post-Review Hardening (CR-01, WR-01, WR-02, WR-03)

All four actionable code-review findings were resolved before verification. Confirmed in code:

| Finding | Fix | Evidence in Code | Commit |
|---------|-----|-----------------|--------|
| CR-01: unbounded main-thread O(n) generation | `MAX_CHALLENGE_LEVEL = 10_000` added; `generate` clamps with `coerceIn`; `GameViewModel` loads Challenge via `withContext(defaultDispatcher)` from injected `CoroutineDispatcher` (not constructor-init) | `GeneratorConstants.kt:42`; `LevelGenerator.kt:63`; `GameViewModel.kt:177-185`; `DomainModule.kt:62` | `5eedff8`, `26d61a4` |
| WR-01: deterministic fallback bypassed dedup gate | `generateForLevel` tracks `dedupBest` (best non-colliding candidate) separately from `best`; returns `dedupBest ?: best` | `LevelGenerator.kt:101-119` | `5eedff8` |
| WR-02: forced-line gate was dead code | `isForcedLine` function and `FORCED_LINE_PREFIX` constant removed entirely; `passesQualityGates` has 5 checks (not 6); tautological test removed from `QualityGateTest` | `LevelGenerator.kt` — grep for `isForcedLine`/`FORCED_LINE` returns nothing | `5eedff8` |
| WR-03: stuck Challenge showed "Solved!" headline | `ResultsViewModel.tierFor` returns `STUCK` when `challengeLevel != null && pegsRemaining != 1`, regardless of star count | `ResultsViewModel.kt:74-81` | `487039e` |

### Determinism Substrate Verification

- `stableSeed(levelNumber, salt)` is pure splitmix64 arithmetic using only `GeneratorConstants` constants — no inline literals.
- `legalReverseMoves` sorts with identical `compareBy({ it.from.row }, { it.from.col }, { it.to.row }, { it.to.col })` comparator as `RulesEngine.legalMoves`.
- PRNG index (`rng.nextInt(candidates.size)`) is drawn only against sorted `List`s — `footprint.inPlay[rng.nextInt(...)]`, `candidates[rng.nextInt(...)]`, `tier.footprintPool[rng.nextInt(...)]`.
- `recentKeys.toHashSet()` used only for O(1) membership tests (`!in forbiddenKeys`), never for PRNG index drawing.

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `domain/…/generator/LevelGenerator.kt` | Pure `object LevelGenerator` with `generate(levelNumber): GeneratedLevel` | VERIFIED | Exists, substantive (297 lines), wired to RulesEngine + DifficultyTiers + BoardCanonical |
| `domain/…/generator/GeneratedLevel.kt` | Value type carrying `levelNumber/startingBoard/solution/generatorVersion` | VERIFIED | `data class GeneratedLevel` with all 4 fields |
| `domain/…/generator/GeneratorConstants.kt` | `GENERATOR_VERSION`, splitmix64 constants, `MAX_ATTEMPTS`, `MAX_CHALLENGE_LEVEL` | VERIFIED | All constants present with KDoc; `GENERATOR_VERSION = 1`, `MAX_CHALLENGE_LEVEL = 10_000` |
| `domain/…/generator/DifficultyTiers.kt` | `fun forLevel(levelNumber: Int): DifficultyTier` — continuous (no `when` range) | VERIFIED | Integer-division growth; no stepped `when`; Curve constants in scoped object |
| `domain/…/generator/DifficultyTier.kt` | Fields-only knob record | VERIFIED | `data class DifficultyTier` with all required fields |
| `domain/…/generator/BoardCanonical.kt` | `symmetryPermutations` + `canonical` lifted from `AuthoringSolver` | VERIFIED | `object BoardCanonical` present; used by `LevelGenerator.canonicalKeyOf` |
| `domain/…/generator/FreeformFootprints.kt` | Connected ≤64-cell freeform builder with flood-fill | VERIFIED | `object FreeformFootprints` with `build` + `isConnected` (independent BFS) |
| `domain/…/generator/ChallengeSolvabilityTest.kt` | Solvability + determinism invariants | VERIFIED | 5 tests covering fold-to-one-peg, regenerate-identical, clamp, in-spec, version stamp |
| `domain/…/generator/DifficultyTierTest.kt` | Monotonicity, continuity, primary-lever, freeform-gate tests | VERIFIED | 6 tests; all pass |
| `domain/…/generator/ChallengeGeneratorGoldenTest.kt` | Golden-master lock reusing `GoldenMasterAssert.assertMatches` | VERIFIED | Uses `GoldenMasterAssert.assertMatches` verbatim; no new codec/assert |
| `domain/src/test/resources/golden/challenge_level_1.txt` | Committed fixture | VERIFIED | Exists; non-empty; `generatorVersion=1` header |
| `domain/src/test/resources/golden/challenge_level_2.txt` | Committed fixture | VERIFIED | Exists |
| `domain/src/test/resources/golden/challenge_level_10.txt` | Committed fixture | VERIFIED | Exists |
| `domain/src/test/resources/golden/challenge_level_100.txt` | Committed fixture | VERIFIED | Exists |
| `domain/src/test/resources/golden/challenge_level_1000.txt` | Committed fixture | VERIFIED | Exists |
| `domain/…/generator/QualityGateTest.kt` | 6 quality gates + determinism-through-rejection + bounded-termination | VERIFIED | 7 tests (forced-line dead gate removed per WR-02) |
| `domain/…/generator/FreeformValidityTest.kt` | Connectivity, ≤64, intro-level gating, on-grid, reseed-or-crash | VERIFIED | Exists and passes |
| `app/…/data/ChallengeLevelSource.kt` | `GeneratedChallengeLevelSource` wrapping `LevelGenerator` | VERIFIED | `StubChallengeLevelSource` removed; `GeneratedChallengeLevelSource` present with clamp + level mapping |
| `app/…/data/ChallengeProgress.kt` | In-memory `StateFlow<Int>` holder starting at 1 | VERIFIED | `@Singleton` with `currentLevel: StateFlow<Int>`, `advanceToNext`, `reset` |
| `app/…/di/DataModule.kt` | `@Binds GeneratedChallengeLevelSource` | VERIFIED | `bindChallengeLevelSource(impl: GeneratedChallengeLevelSource)` present; no stub import |
| `app/…/di/DomainModule.kt` | `@Provides LevelGenerator` | VERIFIED | `provideLevelGenerator(): LevelGenerator = LevelGenerator` present; `provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default` present |
| `app/…/ui/game/GameViewModel.kt` | Challenge load off main thread, clamp, level-scaled score | VERIFIED | `withContext(defaultDispatcher)` for Challenge; `coerceIn(MIN_CHALLENGE_LEVEL, MAX_CHALLENGE_LEVEL)`; `levelMultiplier` from `DifficultyTiers` passed to `scoreCalculator.evaluate` |
| `app/…/navigation/Routes.kt` | `Gameplay(challengeLevel: Int?)`, `Results(challengeLevel: Int?)` | VERIFIED | Both routes carry nullable `challengeLevel` with default null |
| `app/…/navigation/AppNavHost.kt` | `toChallengeBoard(level)` with `popUpTo(Home)` | VERIFIED | `private fun NavHostController.toChallengeBoard(level: Int)` uses `popUpTo<Home>()`; Next-level/Retry wired |
| `app/…/ui/results/ResultsUiState.kt` | `challengeLevel: Int?`, `showNextLevel: Boolean` | VERIFIED | Both fields present; `showNextLevel = challengeLevel != null && pegsRemaining == PEGS_REMAINING_SOLVED` |
| `app/…/ui/results/ResultsViewModel.kt` | `tierFor` returns STUCK for stuck Challenge (WR-03) | VERIFIED | `challengeLevel != null && pegsRemaining != PEGS_REMAINING_SOLVED -> STUCK` |
| `app/…/ui/results/ResultsScreen.kt` | One-accent discipline; Next level / Retry / Classic branches | VERIFIED | `ResultsActions` has exactly 3 branches; only one `Button` (filled primary) per state |
| `app/…/ui/home/HomeScreen.kt` | Enabled `ChallengeTile` with `onPlayChallenge` | VERIFIED | `Card(onClick = onPlayChallenge)`; no `enabled = false`; no `ComingSoonBadge` |
| `app/src/main/res/values/strings.xml` | Required Challenge strings | VERIFIED | `challenge_level_indicator`, `challenge_level_indicator_desc`, `action_next_level`, `action_retry`, `challenge_load_error`, `challenge_tile_supporting` all present |
| `app/…/DomainPurityTest.kt` | ArchUnit gate: no `android.*`/`androidx.*` in domain | VERIFIED | Uses `ArchUnitRunner`; gate is green in full build |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|----|--------|---------|
| `LevelGenerator.kt` | `RulesEngine.kt` | Solution replays via `RulesEngine.applyMove`/`legalMoves` | VERIFIED | `ChallengeSolvabilityTest` fold asserts `move in RulesEngine.legalMoves(board)` each step |
| `LevelGenerator.kt` | `DifficultyTiers.kt` | `generate()` calls `DifficultyTiers.forLevel(level)` | VERIFIED | Line 75: `val tier = DifficultyTiers.forLevel(level)` |
| `LevelGenerator.kt` | `BoardCanonical.kt` | Dedup key via `canonicalKeyOf` | VERIFIED | `canonicalKeyOf` delegates to `BoardCanonical.canonical(..., BoardCanonical.symmetryPermutations(...))` |
| `ChallengeLevelSource.kt` | `LevelGenerator.kt` | `load(levelNumber)` → `generator.generate(clamped)` | VERIFIED | Line 39: `val generated = generator.generate(clamped)` |
| `DataModule.kt` | `ChallengeLevelSource.kt` | `@Binds GeneratedChallengeLevelSource` | VERIFIED | `bindChallengeLevelSource(impl: GeneratedChallengeLevelSource)` |
| `GameViewModel.kt` | `LevelRepository.kt` | `loadChallenge(clampedChallengeLevel)` in `withContext` | VERIFIED | Lines 178-180: `withContext(defaultDispatcher) { levelRepository.loadChallenge(...) }` |
| `ResultsScreen.kt` | `strings.xml` | `action_next_level`, `action_retry` string resources | VERIFIED | `R.string.action_next_level`, `R.string.action_retry` referenced in `ResultsActions` |
| `AppNavHost.kt` | `Routes.kt` | `toChallengeBoard(nextLevel)` → `Gameplay(challengeLevel = level)` | VERIFIED | `navigate(Gameplay(challengeLevel = level)) { popUpTo<Home>() }` |
| `HomeScreen.kt` | `AppNavHost.kt` | `onPlayChallenge` → `Gameplay(challengeLevel = CHALLENGE_ENTRY_LEVEL)` | VERIFIED | `homeContent` slot wires `onPlayChallenge` to nav |

### Requirements Coverage

| Requirement | Source Plan(s) | Description | Status | Evidence |
|-------------|---------------|-------------|--------|----------|
| CHAL-01 | 03-02, 03-03, 03-04, 03-05 | Player can play sequential Challenge levels numbered from 1 | SATISFIED | Home tile → `Gameplay(challengeLevel=1)`; Results "Next level" → N+1; `NavFlowTest` covers reachability |
| CHAL-02 | 03-01, 03-02 | Each Challenge level is procedurally generated and guaranteed solvable | SATISFIED | Reverse-generation by construction; `ChallengeSolvabilityTest` + `QualityGateTest` both green |
| CHAL-03 | 03-01, 03-02 | Challenge level N is deterministic — same board every time (carries generatorVersion) | SATISFIED | Golden-master test green against 5 committed fixtures; `generatorVersion` stamped on every `GeneratedLevel` |
| CHAL-04 | 03-01, 03-03, 03-04 | Challenge difficulty scales with level via board size/shape, peg density, starting pattern | SATISFIED | Continuous `DifficultyTiers.forLevel` growth; `DifficultyTierTest` proves monotonicity and primary-lever ordering; `scoreMultiplier` scales numerically |
| QUAL-04 | 03-01, 03-02 | Domain game logic covered by JVM unit tests including golden-master determinism test | SATISFIED | `ChallengeSolvabilityTest`, `DifficultyTierTest`, `QualityGateTest`, `FreeformValidityTest`, `ChallengeGeneratorGoldenTest` — all JVM, all green |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|--------------|--------|--------------------|--------|
| `GameViewModel` | `initialBoard` (Challenge path) | `levelRepository.loadChallenge(n)` → `GeneratedChallengeLevelSource.load` → `LevelGenerator.generate` → reverse-walk | Real generated board from domain logic | FLOWING |
| `ResultsViewModel` | `challengeLevel`, `showNextLevel` | `savedStateHandle.toRoute<Results>().challengeLevel` (from `GameRoute` → `Results(challengeLevel = viewModel.challengeLevel)`) | Real challenge level from game session | FLOWING |
| `HomeScreen` | `onPlayChallenge` | `AppNavHost` hardcoded `CHALLENGE_ENTRY_LEVEL = 1` (ChallengeProgress.currentLevel is Phase-5 seam, IN-02) | Level 1 always; advancing via nav args in session | STATIC (intentional Phase-5 deferral — IN-02) |

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `domain/…/generator/BoardCanonical.kt` | 71 | `java.lang.Long.numberOfTrailingZeros` (JVM API, not `android.*`) | INFO | IN-01 from code review; not `android.*`, DomainPurityTest passes; Kotlin stdlib `countTrailingZeroBits()` is idiomatic alternative — low-priority, explicitly left as info |

No TBD/FIXME/XXX markers found in any modified files.

### Behavioral Spot-Checks

| Behavior | Evidence | Status |
|----------|----------|--------|
| `generate(1)` folds to 1 peg | `ChallengeSolvabilityTest` asserts `finalBoard.pegCount == 1` for all sample levels including 1; golden fixture `challenge_level_1.txt` contains a 3-move solution | PASS |
| Golden test is byte-stable (no `UPDATE_GOLDEN`) | Full gate `./gradlew ktlintCheck detekt :domain:test :app:testDebugUnitTest assembleDebug` = BUILD SUCCESSFUL; `ChallengeGeneratorGoldenTest` is in-scope and green | PASS |
| DomainPurityTest passes | ArchUnit test in `app:testDebugUnitTest`; full gate green | PASS |
| Full build gate | `./gradlew ktlintCheck detekt :domain:test :app:testDebugUnitTest assembleDebug` = BUILD SUCCESSFUL in 474ms (from cache + up-to-date tasks) | PASS |

### Human Verification Required

*None.* The on-device playability checkpoint (Plan 05, Task 3) was a `checkpoint:human-verify` gate that was approved by the user prior to this verification. The 03-05-SUMMARY records: "On-device end-to-end playability checkpoint (Task 3) was verified and APPROVED by the user." No additional human verification items remain open.

### Gaps Summary

No gaps. All 18 must-haves are verified in the codebase. The full test gate is green. All four code-review hardening fixes (CR-01/WR-01/WR-02/WR-03) are confirmed present in code, not just claimed in the review resolution comment.

**Known deferred items (explicitly NOT counted as gaps):**
- Difficulty-feel knob VALUES (D-05/D-07) are intentionally playtest-deferred; the structure and monotonicity are verified.
- CHAL-05 (continue from highest level / `ChallengeProgress` production wiring) is Phase-5 scope.
- IN-01 (`java.lang.Long.numberOfTrailingZeros`) is an accepted low-priority info note.

---

_Verified: 2026-07-02_
_Verifier: Claude (gsd-verifier)_
