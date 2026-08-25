---
phase: 05-persistence-resume-settings
plan: 05
subsystem: ui
tags: [results, catalog, progress, persistence, mvvm, tdd, accessibility]

# Dependency graph
requires:
  - phase: 05-persistence-resume-settings
    plan: 03
    provides: "ProgressRepository.recordResult/observeResults + NewBests/LevelResult read models"
  - phase: 05-persistence-resume-settings
    plan: 01
    provides: "ProgressPersistenceKeys mode literals (classic/challenge)"
  - phase: 02-navigation-onboarding
    provides: "Results route args + display-only ResultsViewModel; Classic catalog screen/VM"
provides:
  - "`ResultsViewModel` records the solved play once per play and surfaces `NewBests` (D-09)"
  - "`ResultsUiState.newBests` + the `results_new_best` cue on `ResultsScreen`"
  - "`ClassicBoardUiState(descriptor, earnedStars)` — the catalog's per-card UI model"
  - "`BoardPreview(board, modifier, earnedStars)` — 0-3 pips from hoisted state (D-10)"
affects: [05-06, 05-07, home-continue-tiles, resume-lifecycle]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Write-once side effect guarded by a SavedStateHandle flag (survives process death, not just config change)"
    - "List UiState items are their own @Immutable model, joining a static descriptor with observed progress"
    - "Repository Flow → `map` → `stateIn(Eagerly)` with the un-joined list as the initial value, so the screen never waits on the DB"

key-files:
  created: []
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/classic/BoardPreview.kt
    - app/src/main/res/values/dimens.xml
    - app/src/main/res/values/strings.xml
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsScreenTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/classic/ClassicCatalogViewModelTest.kt

key-decisions:
  - "Only a solved board (one peg) is recorded — the stuck/Retry Results state must never leave a stored best for an unsolved level (D-08)"
  - "The write-once guard lives in the SavedStateHandle, not a field, so a process-death rebuild cannot double-count the play (T-05-09)"
  - "The catalog's `boards` became a list of `ClassicBoardUiState`, not a parallel id→stars map, so a card can never render a mismatched pip count"
  - "`BoardPreview.earnedStars` is nullable: `null` omits the pip row entirely, keeping the onboarding illustration byte-identical"
  - "`SharingStarted.Eagerly` over `WhileSubscribed`: the catalog is 5 rows, and eager sharing makes the pips deterministic in tests and already resolved at first frame"

patterns-established:
  - "A one-shot UI cue is derived state (`newBests.any`), never an event — recomposition cannot re-fire it"
  - "Decorative icon rows announce their count once via `clearAndSetSemantics { contentDescription = … }` instead of N anonymous icons"

requirements-completed: [PERS-01]

# Metrics
duration: 25min
completed: 2026-08-25
---

# Phase 5 Plan 05: Results Record + Catalog Star Pips Summary

**Every solved play now merges into its level's stored bests exactly once — guarded so neither a rotation nor a process-death rebuild can inflate a record, and refused outright for a board that was never solved — with the repository's per-field verdict surfacing as a "New best!" pill on Results and the stored stars lighting 0-3 pips on each Classic catalog card.**

## Performance

- **Duration:** ~25 min
- **Tasks:** 2 (both TDD: RED → GREEN)
- **Files modified:** 12 (0 created, 12 modified)
- **Tests added:** 7 (5 ResultsViewModel + 2 ResultsScreen) + 4 new ClassicCatalogViewModel cases; the full `check` gate is green

## Accomplishments

- **PERS-01's loop is closed end to end.** A solve now writes through `ProgressRepository.recordResult` and the same play's verdict comes straight back to the player: `NewBests` lands in `ResultsUiState` and the screen shows a "New best!" pill whenever stars, time or score improved. Nothing is recomputed on the way — the route args the screen displays are exactly the numbers the repository merges.
- **A play can be recorded at most once (T-05-09).** The guard flag is written into the `SavedStateHandle` *before* the write is launched, so it survives the two real double-record paths: a configuration change (same ViewModel instance, `init` never re-runs) and a process-death restore (new instance, same handle). `recreatingTheViewModelOverTheSameSavedStateRecordsOnlyOnce` builds two ViewModels over one handle and asserts a single call.
- **An unsolved board writes nothing (D-08).** `ResultsScreen` deliberately supports a stuck Challenge state with a Retry action (D-10), and `NavFlowTest` already drives a stuck result into that destination — so "Results ⇒ solved" is not an invariant that can be assumed. The record is gated on `pegsRemaining == 1`; `anUnsolvedPlayRecordsNothing` pins it.
- **Challenge results are keyed the way the ladder needs.** `mode = challenge`, `levelId = <level number>`, both via `ProgressPersistenceKeys` constants — no inline mode literal anywhere in the UI layer.
- **The catalog shows progress at a glance without the composable touching a repository.** `ClassicCatalogViewModel` joins `observeResults(classic)` onto the D-18-ordered catalog by `levelId`; each card carries its own `earnedStars`, and a board with no row (or an empty results stream) simply shows 0 pips — a missing result costs pips, never the catalog (T-05-10).
- **Pips are legible to a screen reader.** The three icons are decorative; the row announces "N of 3 stars" once, mirroring how the Results star row already exposes its count.

## Task Commits

1. **Task 1: Record the solved play + "New best!" cue** — `cce3562` (test, RED) → `4415671` (feat, GREEN)
2. **Task 2: Earned-star pips on the Classic catalog** — `0bef23c` (test, RED) → `6b4f7ac` (feat, GREEN)

No REFACTOR commits: both implementations landed at their final shape (the ktlint reflow in Task 2 was applied before the GREEN commit).

## Exported Signatures (for 05-06 / 05-07)

```kotlin
// ui/results/ResultsUiState.kt
@Immutable
data class ResultsUiState(
    val stars: Int, val pegsRemaining: Int, val elapsedSeconds: Int, val score: Int,
    val headlineTier: ResultsHeadlineTier, val replayId: String,
    val challengeLevel: Int? = null,
    val newBests: NewBests = NewBests(false, false, false),   // D-09 cue source
)

// ui/results/ResultsScreen.kt
const val RESULTS_NEW_BEST_TEST_TAG = "results_new_best"     // present only when newBests.any

// ui/classic/ClassicCatalogUiState.kt
@Immutable data class ClassicCatalogUiState(val boards: List<ClassicBoardUiState>)
@Immutable data class ClassicBoardUiState(
    val descriptor: ClassicLevelDescriptor,
    val earnedStars: Int = 0,                                 // 0-3, D-10
)

// ui/classic/BoardPreview.kt
@Composable fun BoardPreview(board: Board, modifier: Modifier = Modifier, earnedStars: Int? = null)
// earnedStars == null ⇒ no pip row at all (the onboarding illustration)
```

**Note for 05-07:** the Results record now happens in `ResultsViewModel`, **not** in `GameViewModel`'s Won branch. If 05-07 also wires a record at the Won finalize point, the play would be counted twice under two different guards — `GameViewModel` should own `clearSnapshot` and the Challenge advance, and leave `recordResult` here.

## Files Created/Modified

**Modified (main)**
- `ui/results/ResultsViewModel.kt` — injects `ProgressRepository`; derives `mode`/`levelId` from the route args via `ProgressPersistenceKeys`; `recordSolvedPlayOnce()` (solved-only + SavedStateHandle guard) → `recordResult` → `NewBests` into state; `scoreResultFromArgs()` rebuilds the domain `ScoreResult`
- `ui/results/ResultsUiState.kt` — `newBests: NewBests` with an all-false default
- `ui/results/ResultsScreen.kt` — `NewBestBadge()` (a `tertiaryContainer` pill, deliberately not the one `primary` accent) rendered between the star row and the metric block when `newBests.any`, plus `RESULTS_NEW_BEST_TEST_TAG`
- `ui/classic/ClassicCatalogViewModel.kt` — `observeResults(MODE_CLASSIC)` → `map` → `stateIn(Eagerly)`; `catalogWith(starsByLevelId)` joins by id
- `ui/classic/ClassicCatalogUiState.kt` — `boards` is now `List<ClassicBoardUiState>`; the new per-card model
- `ui/classic/ClassicCatalogScreen.kt` — `BoardCard` takes the card model and forwards `earnedStars`
- `ui/classic/BoardPreview.kt` — split into `BoardFootprint` + `EarnedStarPips`; the public composable gains `earnedStars: Int?`
- `res/values/dimens.xml` — `catalog_star_pip_size` (16dp); `catalog_card_height` 216dp → 248dp
- `res/values/strings.xml` — `results_new_best`, `catalog_stars_earned`

**Modified (tests)**
- `ui/results/ResultsViewModelTest.kt` — `FakeProgressRepository` reproducing the D-07 merge + 5 new cases (first-solve all-true, worse-replay all-false, challenge keying, unsolved records nothing, single record over one handle); Robolectric + `Dispatchers.setMain`
- `ui/results/ResultsScreenTest.kt` — the cue is rendered when a flag is set and absent when none is
- `ui/classic/ClassicCatalogViewModelTest.kt` — reworked over the new card model + a settable `FakeProgressRepository`: unplayed catalog = all zeros, a stored `LevelResult(stars=N)` = N on that card only, a later result updates the pips, and the catalog observes the Classic mode only

## Decisions Made

- **Recording is gated on a solve, not merely on reaching Results.** The plan assumed the destination is solve-only, but the screen itself renders a stuck Challenge state (D-10) and `NavFlowTest` already navigates one there. Without the gate, a stuck 1-star board would create a stored row for a level the player never solved and light a pip on the catalog — the exact thing D-08 forbids. Cost: one `if`; benefit: the must-have truth holds regardless of how nav evolves.
- **The guard lives in `SavedStateHandle`.** A private field would only cover configuration change (which is already covered by the ViewModel surviving). Process-death restore rebuilds the ViewModel over the *same* handle, which is precisely when a naive `init` would re-record.
- **`pegsCleared` is filled with a named zero, not re-derived.** The `Results` route does not carry it and no stored best uses it; re-deriving it in the UI would re-open the T-02-06-ARG "don't recompute scoring in the ViewModel" hole for no gain.
- **The catalog card became its own model rather than a `Map<String, Int>` beside the list.** A parallel map can drift out of sync with the list it indexes; a per-item `earnedStars` cannot.
- **`BoardPreview.earnedStars` is nullable rather than defaulting to 0.** `BoardPreview` has a second caller — the onboarding illustration — which must not sprout an empty three-pip row. `null` means "this is not a progress-bearing card" and renders exactly today's output.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Recreated the gitignored `local.properties` in the worktree**
- **Found during:** setup (first Gradle invocation)
- **Issue:** `local.properties` is gitignored, so a fresh worktree fails every Gradle command with "SDK location not found".
- **Fix:** Wrote `sdk.dir=/Users/ermanergoz/Library/Android/sdk`.
- **Files modified:** `local.properties` (gitignored — intentionally **not** committed)
- **Committed in:** n/a

**2. [Rule 2 - Missing Critical] Only a solved play is recorded**
- **Found during:** Task 1
- **Issue:** The plan recorded unconditionally on the premise that "this destination is only reached on a solve". That premise does not hold: `ResultsUiState.showNextLevel`/`ResultsHeadlineTier.STUCK` exist precisely to render a stuck Challenge result with a Retry action (D-10), and `NavFlowTest.stuckChallengeResultsSurfacesRetryNotNextLevel` drives that state. An unconditional record would store a best (and light a catalog pip) for a level that was never solved — a direct violation of the plan's own D-08 must-have truth.
- **Fix:** `recordSolvedPlayOnce()` returns early unless `pegsRemaining == 1`; covered by `anUnsolvedPlayRecordsNothing`.
- **Files modified:** `ui/results/ResultsViewModel.kt`, `ui/results/ResultsViewModelTest.kt`
- **Committed in:** `cce3562` (test) / `4415671` (impl)

**3. [Rule 2 - Missing Critical] The card height was raised so the pip row cannot clip**
- **Found during:** Task 2
- **Issue:** `catalog_card_height` is a fixed 216dp sized for preview + 2-line name + hole count. Adding a pip row to a card whose name wraps to two lines ("Asymmetric 3-3-2-2") overflows the fixed height and the pips would be clipped — the feature would silently not render on exactly one of the five boards.
- **Fix:** 216dp → 248dp, with the reason recorded in the token's comment.
- **Files modified:** `res/values/dimens.xml`
- **Committed in:** `6b4f7ac`

**4. [Trivial] Three files outside the plan's `files_modified` were touched**
- `ui/classic/ClassicCatalogUiState.kt` and `ui/classic/ClassicCatalogScreen.kt`: unavoidable — the plan requires "each catalog UiState item carries `earnedStars: Int`", which is a change to the UiState type and therefore to its single consumer.
- `ui/results/ResultsScreenTest.kt`: two cases added so the D-09 cue (a must-have truth) is locked by a test rather than only asserted at the ViewModel boundary.

---

**Total deviations:** 3 auto-fixed (1 blocking, 2 missing-critical) + 1 trivial file-scope note
**Impact on plan:** No scope creep — no lifecycle, resume, Home or `GameViewModel` wiring was pulled forward. Deviations 2 and 3 keep the plan's own must-have truths true.

## Issues Encountered

- **ktlint's chained-call rule reformats Truth assertions over property chains.** `assertThat(viewModel.uiState.value.boards.map { … })` is rewritten to a multi-line form; `:app:ktlintFormat` fixes it, but running the standards gate *before* committing (as 05-03 recommended) is what caught it.
- **`ClassicCatalogViewModel` now launches a coroutine, so its test needs a Main dispatcher.** The old test was a plain `runTest` with no `Dispatchers.setMain`; without it `viewModelScope` throws "Module with the Main dispatcher had failed to initialize". `SharingStarted.Eagerly` + `runCurrent()` then makes the joined state readable straight off `uiState.value`, avoiding the StateFlow-conflation race that a `WhileSubscribed` + Turbine assertion on the *initial* item would have.

## TDD Gate Compliance

Both tasks followed RED → GREEN with commits proving the sequence:
- Task 1: `cce3562` (`test`, compile-fails on the missing `newBests` and repository parameter) → `4415671` (`feat`)
- Task 2: `0bef23c` (`test`, compile-fails on the missing `descriptor`/`earnedStars`) → `6b4f7ac` (`feat`)

No test passed unexpectedly during RED. REFACTOR was skipped for both.

## Verification

| Gate | Result |
|------|--------|
| `./gradlew :app:testDebugUnitTest --tests "*ResultsViewModelTest" --tests "*ResultsScreenTest"` | BUILD SUCCESSFUL |
| `./gradlew :app:testDebugUnitTest --tests "*ClassicCatalogViewModelTest"` | BUILD SUCCESSFUL |
| `./gradlew :app:dpSpLiteralCheck :app:ktlintCheck :app:detekt` | BUILD SUCCESSFUL |
| `./gradlew check` (both modules: all unit tests + detekt + ktlint + dpSpLiteralCheck + lint) | BUILD SUCCESSFUL |
| `./gradlew :app:assembleDebug` (Hilt graph resolves the new `ProgressRepository` injections) | BUILD SUCCESSFUL |
| `git diff --diff-filter=D` over the plan's commits | no deletions |

## Known Stubs

None. Every piece of this plan is wired end to end: the record call reaches the real repository through Hilt, and the pips read the real `observeResults` stream.

## Threat Flags

None — no new security-relevant surface. The plan's register is addressed:

| Threat ID | Disposition | Status |
|-----------|-------------|--------|
| T-05-09 (double-record inflates a best) | mitigate | Done — SavedStateHandle write-once guard, asserted by `recreatingTheViewModelOverTheSameSavedStateRecordsOnlyOnce`; the repository's per-field merge keeps a duplicate idempotent anyway |
| T-05-07 (PII in results) | accept | Unchanged — stars/time/score only |
| T-05-10 (missing/corrupt result breaks the catalog) | mitigate | Done — no row ⇒ 0 pips; an empty stream renders the full catalog, asserted by `anUnplayedCatalogShowsNoEarnedStarsAtAll` |

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

**Ready to consume:**
- 05-06's Home Continue tiles are unaffected by this plan; the catalog and Results screens are self-contained.
- 05-07 owns `clearSnapshot` on solve/restart/new-game and the Challenge advance. **It must not add a second `recordResult` call** at the Won finalize point — that write now belongs to `ResultsViewModel` (see "Exported Signatures").

**Carry forward:**
1. The Results cue is one-shot by construction (derived state, lost on process death). If a later plan wants it to survive a restore, the flags would have to be persisted — deliberately not done.
2. `catalog_card_height` is now sized for preview + pips + a 2-line name. Any further card content needs another look at that token.
3. Challenge has no catalog, so Challenge stars are stored but not yet displayed anywhere — the ladder UI is the natural home if that is ever wanted.

**Blockers:** None.

## Self-Check: PASSED

- All 12 modified files verified present and tracked.
- All 4 commit hashes verified present in `git log` (`cce3562`, `4415671`, `0bef23c`, `6b4f7ac`).
- Working tree clean apart from the deliberately gitignored `local.properties`.

---
*Phase: 05-persistence-resume-settings*
*Completed: 2026-08-25*
