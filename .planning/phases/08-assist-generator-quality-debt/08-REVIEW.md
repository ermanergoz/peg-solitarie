---
phase: 08-assist-generator-quality-debt
reviewed: 2026-08-30T00:00:00Z
depth: standard
files_reviewed: 48
files_reviewed_list:
  - app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt
  - app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
  - app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelHintStalenessTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
  - domain/build.gradle.kts
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/assist/OpeningBook.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/assist/OpeningBookFormat.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/generator/BoardCanonical.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/model/BoardConstants.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/model/BoardSpec.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Boards.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculator.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreParams.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Pagoda.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Solver.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/SolverBudget.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/SolverTables.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/usecase/GetHintUseCase.kt
  - domain/src/main/resources/golden/book/asymmetric39-k4.txt
  - domain/src/main/resources/golden/book/diamond41-k4.txt
  - domain/src/main/resources/golden/book/english33-k4.txt
  - domain/src/main/resources/golden/book/european37-k4.txt
  - domain/src/main/resources/golden/book/wiegleb45-k3.txt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/TestFlags.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/assist/OpeningBookFormatTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/assist/OpeningBookTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/generator/QualityGateTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/GoldenMasterAssert.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/AuthoringSolver.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/BeamSearchAuthoring.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/OpeningBookAuthoring.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/OpeningBookAuthoringTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/SolutionTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/model/BoardSpecTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculatorTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/CanonicalTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/DepthSweepTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/DiamondFloorOptimalityTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/PagodaTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/SolverBudgetTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/SolverTablesTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/SolverTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/usecase/GetHintUseCaseTest.kt
  - domain/src/test/resources/golden/solutions/diamond41.txt
findings:
  critical: 0
  warning: 1
  info: 5
  total: 6
status: issues_found
---

# Phase 08: Code Review Report

**Reviewed:** 2026-08-30
**Depth:** standard
**Files Reviewed:** 48
**Status:** issues_found

## Summary

This re-review focused first on verifying the CR-01 gap-closure (plan 08-12, commits 5c299eb / b6e6704) and then reviewed the full phase file set fresh.

**CR-01 verdict: the stale-hint race fix is sound for gameplay mutations.** All three player-driven board-mutation sites — `performJump` (after the legal-move guard, correctly), `onUndo` (after both the declined-grant and empty-history guards, correctly), and `resetToInitial` — call the single `cancelHintSearch()` idiom, and each clears `solverThinking` in the same state update so the Hint control cannot be left disabled. The three search inputs (board, recorded line, move count) are captured on the main thread before the `withContext` hop, closing the cross-thread inconsistency. The stale-result guard (`it.board != boardForHint`) is correct given `Board`'s equality semantics (data class over `pegs` + a referentially-compared `BoardSpec`): within a game the spec instance is stable, so equality means "identical position", and the jump-then-undo-back-to-the-same-position hole is closed by cancellation at both mutation sites, not by the guard. `withContext` rethrows on a cancelled job, so the post-hop `_uiState.update` cannot run after cancellation. `GameViewModelHintStalenessTest` genuinely parks the search at the dispatcher hop (asserting `gate.pending == 1` first) and covers the stale-move, stale-null, control-usability, and captured-inputs cases.

**One gap remains at the edges of the same contract:** the three *async board-install* sites (`loadChallengeBoardIfNeeded`, `installClassicBoard`, `applyRestored`) neither cancel an in-flight search nor is `onHintRequested` gated on `isLoading`, and `GameScreen` renders the Hint control unconditionally in the top bar while the board area shows the loading indicator. That leaves a reachable (transient) untruthful "no winning move" banner during the load window — WR-01 below.

The `:domain` work is of unusually high quality: the opening-book format decoder is genuinely total (cannot throw), the loader fails closed on one malformed line, the miss/authoritative-dead distinction is enforced at the type level and exhaustively tested, the Solver's memoised DFS is sound (no unsound prunes; budget/cancel latches correctly downgrade "no line found" to inconclusive), the `VisitedSet` zero-sentinel argument holds because the goal test precedes the memo insert and `minimumPegs >= 1`, and the resume codec fail-closes on every forgery class including the T-08-21 non-positive `minimumPegs`. Golden book headers were spot-checked against file contents and are consistent (entries + comments = line counts on all five files).

## Warnings

### WR-01: Hint lifecycle does not cover the async board-install window — a transient untruthful "no winning move" banner is reachable during a Challenge load or Continue restore

**File:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:287-290, 355-377, 404-424, 436-465, 905-953`
**Issue:** `cancelHintSearch()`'s own KDoc (lines 274-285) states "Every site that mutates the board MUST call it (CR-01)", but the three sites that *install* a board asynchronously — `loadChallengeBoardIfNeeded` (line 365), `installClassicBoard` (line 412), and `applyRestored` (line 450) — do not call it, and `onHintRequested` has no `isLoading` guard. `GameScreen.kt` renders the top bar (including the Hint control, enabled while `solverThinking` is false) unconditionally; only the board area swaps to `LoadingIndicator()` while `isLoading`. The reachable sequence:

1. Player enters a Challenge level (or Continue). State is `GameUiState.loading()` with the 1-cell placeholder board (`pegs = 0L`).
2. Player taps Hint. The search runs against the placeholder: `pegCount 0 <= minimumPegs 1` makes `Solver.firstWinningMove` return `null` instantly; the book misses (footprint not in catalog).
3. The result lands while the load is still in flight. The stale-result guard *passes* — the placeholder board is unchanged, so `it.board == boardForHint` — and `noWinningMove = true` is written. `showRescueBanner` renders the "no winning move — undo or restart" banner over the loading indicator (the banner branch is outside the `isLoading` conditional).
4. The banner self-clears when the install rebuilds state via `GameUiState.initial(...)`.

This is transient and self-healing, but it is exactly the untruthful banner DEBT-01 exists to remove — raised on a position that was never played — and it violates the documented "every mutating site" invariant, which is the drift the fix's own contract warns against. The symmetric case (hint result landing *after* install) is already safe: the installed board carries a fresh `BoardSpec` instance, so the equality guard discards it.
**Fix:** Two small changes, either of which closes the visible defect; both together restore the documented invariant:
```kotlin
// 1. Guard the request while no real board is installed:
fun onHintRequested() {
    if (_uiState.value.isLoading) return
    cancelHintSearch()
    ...
}

// 2. Cancel at the three install sites (each already rebuilds state via
// GameUiState.initial(), which carries solverThinking = false, so the
// "caller owes the flag" obligation is met by the rebuild itself):
private suspend fun loadChallengeBoardIfNeeded() {
    ...
    initialBoard = level.initialBoard
    cancelHintSearch()   // and likewise in installClassicBoard / applyRestored
    _uiState.update { ... }
}
```

## Info

### IN-01: `Solver.lastVisited` is unsynchronized mutable singleton state written by potentially concurrent searches

**File:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Solver.kt:109-110, 128, 157`
**Issue:** Cancellation is cooperative (polled every 4096 expansions), so a cancelled hint search winding down on `Dispatchers.Default` can overlap a freshly launched one; both write the plain `var lastVisited` (a `Long`, which the JMM does not guarantee atomic without `@Volatile`). Production impact is nil — the field is diagnostic and read only by tests (`SolverBudgetTest`, `SolverTest`), which run single-threaded — but it is shared mutable state on a global object with no synchronization. `OpeningBook.resourceReads` has the same shape but is incremented under the `@Synchronized bookFor` in production.
**Fix:** Mark `lastVisited` `@Volatile`, or better, return visited counts as part of the search result and keep the object stateless.

### IN-02: Verbatim duplicate `VisitedSet` and duplicated centroid/inwardness helpers between main and test sources

**File:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Solver.kt:362-419` and `domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/AuthoringSolver.kt:270-327`
**Issue:** The open-addressing `VisitedSet` (58 lines) exists twice, byte-for-byte identical apart from KDoc; the centroid/inwardness/distanceSquared trio in `SolverTables.kt:193-212` is likewise mirrored in `AuthoringSolver.kt:124-150`. Unlike the deliberately independent BFS re-derivations in the book tests (which exist to cross-check the authoring enumeration), `VisitedSet` is pure mechanism, not an independent check — a fix to one copy (e.g., a probing bug) will not reach the other.
**Fix:** Move `VisitedSet` to an `internal` main-source utility (test sources already depend on main), or add a comment explaining why the test copy must stay independent if that is intentional.

### IN-03: `diamondOneJumpFromAFivePegDeadEnd` KDoc says "four extra" pegs; the code adds three

**File:** `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt:1370-1379`
**Issue:** The doc reads "the same Diamond-41 jump with four extra mutually-unreachable pegs", but the builder adds exactly three (`(8,4)`, `(2,2)`, `(2,6)`) on top of the three-peg floor fixture — 6 pegs, minus one jumped, = the asserted 5. The arithmetic in the test is correct; the comment is not.
**Fix:** Change "four extra" to "three extra".

### IN-04: `toPositionOrNull`'s bound justification is unsound in general, though safe for every current board

**File:** `app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt:122-147`
**Issue:** The comment argues the `0 until 64` coordinate bound cannot fail closed on a legitimate future board because "a 64-cell footprint could in principle be a 1x64 line". That conflates footprint *size* (bounded by the `Long` mask) with coordinate *values* (unbounded by it): a legal 10-cell footprint anchored at row 100 would be rejected by this bound despite fitting the mask. Every current board is origin-anchored on a ≤9-cell grid and generated Challenge footprints are small, so behavior is safe today — but the comment claims a property the code does not have, and a future board authored with offset coordinates would silently destroy stored games exactly as the comment says it cannot.
**Fix:** Correct the comment to state the real invariant ("all shipped and generated boards are origin-anchored, so no legal coordinate exceeds 63; a board that breaks this must also revise this bound"), or bound coordinates independently of the mask-size argument.

### IN-05: `DepthSweepTest`'s bucket classification relies on an incidental property of the solver

**File:** `domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/DepthSweepTest.kt:207-220`
**Issue:** The sweep classifies a position as "reached the solver" via `polls > 0` and as "authoritative dead" via zero polls plus a null answer. But `Solver.firstWinningMove` can return `null` with *zero* polls when a position is pagoda-rejected or already at target — so a gap-board book MISS that the pagoda rejects instantly would be miscounted as an authoritative book answer. The pinned counts (`deferred == DOCUMENTED_GAP`, `authoritative == PROVEN_DEAD`) currently hold because Diamond-41's pagoda is vacuous (all misses launch real searches) and Wiegleb's 4 gap masks all search — but the classification is correct by measurement, not by construction, and a future gap position that happens to be pagoda-rejected would fail the sweep with a misleading "authoritative count mismatch" rather than the real story.
**Fix:** Classify via `OpeningBook.lookup(board)` directly (Hit / NoWinningMove / null-miss) instead of inferring the path from poll counts; keep the poll spy only for the "book-served positions never enter the solver" assertions.

### IN-06: `GameViewModelHintStalenessTest` class KDoc describes the fixed defect in the present tense

**File:** `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelHintStalenessTest.kt:52-59`
**Issue:** The doc states "neither `performJump` nor `onUndo` cancels the search, and the result is applied unconditionally. Worse, the `withContext(defaultDispatcher)` block reads `_uiState.value.board` ... on the BACKGROUND thread" — all of which is now false (both cancel, inputs are captured on the main thread, results are guarded). A reader auditing the ViewModel from this spec would conclude the race still exists.
**Fix:** Rephrase to past tense / "the pre-fix mechanism was:" so the doc describes what the test guards against rather than asserting it about current code.

---

_Reviewed: 2026-08-30_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
