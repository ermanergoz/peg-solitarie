---
phase: 08-assist-generator-quality-debt
reviewed: 2026-08-30T00:00:00Z
depth: standard
files_reviewed: 43
files_reviewed_list:
  - app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt
  - app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
  - app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt
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
findings:
  critical: 1
  warning: 2
  info: 5
  total: 8
status: issues_found
---

# Phase 8: Code Review Report

**Reviewed:** 2026-08-30
**Depth:** standard
**Files Reviewed:** 43
**Status:** issues_found

## Summary

Reviewed the assist/generator quality-debt slice: the per-board `minimumPegs` target
(BoardSpec/Boards/ScoreCalculator/SnapshotMapper), the generalized Pagoda prune, the
bitboard Solver with deterministic restarts, the opening book (format, loader, authoring),
the hint use case, the GameViewModel hint/undo/resume wiring, and the accompanying test
suites and Gradle wiring.

The domain layer is in strong shape. The fail-closed contracts I attacked hold under
scrutiny: `SnapshotMapper` rejects forged coordinates, out-of-footprint peg masks,
duplicate footprints, half-specified targets, and non-positive `minimumPegs` (refusing
rather than clamping, which correctly protects the solver's `pegCount <= minimumPegs`
goal test); the `size == 64` edge of `holdsOnly` correctly special-cases Kotlin's
mod-64 shift semantics; `OpeningBookFormat.decodeEntry` is genuinely throw-free and
canonical; `OpeningBook` fails closed on one malformed line and memoises failures so a
missing resource costs one read; the three-outcome Hit/NoWinningMove/miss distinction is
enforced at the type level and re-verified end-to-end by `OpeningBookTest` and
`DepthSweepTest`; `Solver`'s restart schedule, per-attempt memory bound, and
proven-unsolvable early exit are all internally consistent (`hitBudget`/`cancelled`
latching makes the `provenUnsolvable` inference sound); `BoardCanonical`'s chunked path
handles the bit-40+ Pitfall-4 cases, and both canonical paths use the same (signed)
ordering so keys agree.

The defects found are concentrated at the `:app` concurrency seams: an in-flight hint
search whose result is applied to a board that changed underneath it (a user-visible
violation of the "a shown hint is legal on the shown board" contract this very phase
exists to establish), and lifecycle-cancellable persistence writes for the resume
snapshot. Known, user-approved decisions (Wiegleb K=3, Diamond-41's 440-position gap,
english33's four proven-dead markers, the gated depth sweep, the `<= minimumPegs` win
test) were treated as ground truth and are not flagged.

## Critical Issues

### CR-01: In-flight hint result is applied to a board that changed underneath it (stale/illegal hint highlight)

**File:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:839-876` (with `performJump` at 592-637 and `onUndo` at 772-819)
**Issue:** `onHintRequested` launches a search that can legitimately run for the full
1.5 s deadline (an off-book Diamond-41 position, whose Pagoda prune is provably vacuous,
is the documented worst case). While `solverThinking` is raised, the board remains fully
tappable (`GameScreen` disables only the Hint control). If the player performs a jump or
undo during the search:

1. `performJump`/`onUndo` clear `hintSource`/`hintTarget`/`noWinningMove` but do **not**
   cancel `hintJob` — only a *new* hint request or `resetToInitial` cancels it.
2. When the stale search resolves, its unconditional
   `_uiState.update { it.copy(hintSource = move.from, hintTarget = move.to, ...) }`
   writes a move computed for the *old* board onto the *new* board. The glow can point at
   an empty cell or an illegal move — exactly the "highlighted move gameplay then
   rejects" failure that `GetHintUseCase`'s KDoc names as a visible correctness failure,
   and that every book/solver test asserts can never happen. A stale `null` result
   equally raises the `noWinningMove` rescue banner on a position that was never
   searched. Once ads are real (Slice 2), this is a paid hint that is wrong.

   Additionally, the `withContext(defaultDispatcher)` block reads
   `_uiState.value.board`, `moveHistory.size`, and `onRecordedLine` **on the background
   thread** while the main thread may concurrently mutate `moveHistory` (a plain
   `ArrayList`) and `onRecordedLine` — an unsynchronized cross-thread read with no
   happens-before edge after dispatch, so the three inputs can be mutually inconsistent
   (board from before a jump, `movesPlayed` from after it).

The RulesEngine tap path still rejects any illegal move the player then taps, so this
cannot corrupt game state — but it is deterministic, user-visible incorrect assist
output on the exact contract (T-04-CT / D-01) this phase hardens everywhere else.

**Fix:**
```kotlin
// 1. Capture inputs on the main thread, before crossing dispatchers:
val boardForHint = _uiState.value.board
val movesPlayedForHint = moveHistory.size
val solutionForHint = solution.takeIf { onRecordedLine }
val move = withContext(defaultDispatcher) {
    getHintUseCase(
        board = boardForHint,
        solution = solutionForHint,
        movesPlayed = movesPlayedForHint,
        shouldCancel = { !isActive || deadline.hasPassedNow() },
    )
}
// 2. Discard a result the board has outgrown:
_uiState.update {
    if (it.board != boardForHint) it.copy(solverThinking = false)
    else if (move != null) it.copy(hintSource = move.from, hintTarget = move.to, solverThinking = false)
    else it.copy(noWinningMove = true, solverThinking = false)
}
```
And cancel the pending search at the two mutation sites (`performJump`, `onUndo`):
```kotlin
hintJob?.cancel()
hintJob = null
```
(`Board` is a data class over `spec`+`pegs`, but `BoardSpec` has identity equality — the
spec instance does not change between a hint request and a jump on the same game, so the
`it.board != boardForHint` guard is sound; the `hintJob?.cancel()` at both mutation
sites is the primary fix and makes the guard a belt-and-braces check.)

## Warnings

### WR-01: Resume-snapshot writes ride `viewModelScope` and race ViewModel clearance — save and clear can both be silently cancelled

**File:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:476, 480-482`
**Issue:** `onLeaveScreen` persists the resume snapshot with
`viewModelScope.launch { progressRepository.saveSnapshot(dto) }`, and `clearSnapshot()`
does the same for the delete. `GameRoute` invokes `onLeaveScreen` on `ON_STOP`
(GameRoute.kt:54). On back-navigation the NavBackStackEntry is destroyed immediately
after `ON_STOP`, clearing the ViewModel and cancelling `viewModelScope` — the launched
coroutine starts on `Main.immediate` but suspends at the repository's first suspension
point, and the cancellation can abort the write. Consequences:

- Leaving mid-game via back/Home can silently fail to save, so Continue resumes an
  older position (from a previous `ON_STOP`) or does not appear at all — PERS-03 fails
  on precisely the navigate-away case it names.
- The Won-path `clearSnapshot()` (line 649) can be cancelled if the Gameplay entry is
  popped promptly after the finish event, leaving a stale Continue pointing at a
  finished game — the exact Pitfall-4 resurrection the surrounding comment says this
  call prevents.

**Fix:** Move persistence off the ViewModel's lifetime. Either inject an
application-scoped `CoroutineScope` for these two writes
(`externalScope.launch { progressRepository.saveSnapshot(dto) }`), or wrap them in
`withContext(NonCancellable)` inside the existing launch:
```kotlin
viewModelScope.launch {
    withContext(NonCancellable) { progressRepository.saveSnapshot(dto) }
}
```
(An injected application scope is the cleaner fix; `NonCancellable` still fails if the
process dies, but survives ViewModel clearance.)

### WR-02: Six-field cue/settings carry-forward is copy-pasted at four state-rebuild sites — a forgotten field fails silently

**File:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:342-353, 389-400, 427-441, 915-925`
**Issue:** `loadChallengeBoardIfNeeded`, `installClassicBoard`, `applyRestored`, and
`resetToInitial` each hand-copy the same six fields (`showHints`, `hapticsOn`,
`selectionTick`, `deselectionTick`, `undoTick`, `rejectedTapTick`) onto a fresh
`GameUiState.initial(board)`. The `resetToInitial` comment itself documents the failure
mode: any site (or any future fifth site — a seventh cue ordinal is plausible given the
sound work) that forgets one ordinal resets it to 0, silently firing a phantom cue or
losing a persisted setting until the next flow emission, and the `= 0` defaults are
exactly what hides the omission. Four divergent copies of a correctness-bearing list is
the drift this codebase's own conventions (single source of truth, no duplicated
contracts) forbid elsewhere.
**Fix:** Extract one helper and route all four sites through it:
```kotlin
private fun rebuiltState(board: Board): GameUiState {
    val current = _uiState.value
    return GameUiState.initial(board).copy(
        showHints = current.showHints,
        hapticsOn = current.hapticsOn,
        selectionTick = current.selectionTick,
        deselectionTick = current.deselectionTick,
        undoTick = current.undoTick,
        rejectedTapTick = current.rejectedTapTick,
    )
}
```
`applyRestored` then layers its extras (`status`, `canUndo`, `elapsedSeconds`) on the
helper's result with one further `.copy`.

## Info

### IN-01: GoldenMasterAssert re-declares the UPDATE_GOLDEN flag literal that TestFlags exists to centralise

**File:** `domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/GoldenMasterAssert.kt:19`
**Issue:** `private const val UPDATE_GOLDEN_FLAG = "UPDATE_GOLDEN"` duplicates the
string that `TestFlags.UPDATE_GOLDEN` was introduced (this phase) to be the single home
of — `SolutionTest` and `OpeningBookAuthoringTest` both reference
`TestFlags.UPDATE_GOLDEN`; this file only reuses `TestFlags.isEnabled`. A rename on
either side drifts silently.
**Fix:** `private const val UPDATE_GOLDEN_FLAG = TestFlags.UPDATE_GOLDEN` (or call
`TestFlags.isEnabled(TestFlags.UPDATE_GOLDEN)` directly and drop the local constant).

### IN-02: OpeningBook.parseBook silently accepts duplicate-mask entries (last wins)

**File:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/assist/OpeningBook.kt:238-246`
**Issue:** The loader's stance is "one malformed line makes the whole book
unavailable", but a corrupt or tampered book containing the same mask twice — possibly
with conflicting moves, or a move on one line and the authoritative `-` marker on
another — parses successfully with the later line silently overwriting the earlier one.
That is a structurally invalid book being repaired rather than refused, which is the
behaviour the class KDoc rules out for every other corruption shape. (The committed
artifacts have no duplicates; this is a defensive gap, not a live defect.)
**Fix:** Treat a duplicate key as malformed:
```kotlin
if (entries.put(pegs, move) != null && pegs in entries) return null
// or explicitly: if (pegs in entries) return null before the put
```

### IN-03: Mutable unsynchronized diagnostics globals (`Solver.lastVisited`, `OpeningBook.resourceReads`)

**File:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/solver/Solver.kt:109-110`; `domain/src/main/kotlin/com/app/pegsolitaire/domain/assist/OpeningBook.kt:169`
**Issue:** `Solver.lastVisited` is a non-volatile `Long` written from whatever worker
thread runs a search. Because hint cancellation is cooperative (up to 4096 nodes of
lag), a cancelled search and its replacement can briefly run concurrently and interleave
writes; a non-volatile `long` write is also not guaranteed atomic on the JVM.
Diagnostic-only in production, but `SolverBudgetTest` reads it as "the deciding
attempt's count", which assumes single-threaded use. `resourceReads` is only mutated
under `bookFor`'s lock in production but is unguarded when tests call `readBook`
directly.
**Fix:** Mark both `@Volatile` (or return the visited count from the search result
instead of parking it in object state) and note the single-threaded assumption in the
KDoc.

### IN-04: DepthSweepTest infers "authoritative dead" from `polls == 0`, conflating it with a solver fast-reject

**File:** `domain/src/test/kotlin/com/app/pegsolitaire/domain/solver/DepthSweepTest.kt:207-220`
**Issue:** The classification `polls > 0 → deferred; move != null → served; else →
authoritative` assumes every solver entry polls `shouldCancel` at least once. A
book-missed position that `Solver.firstWinningMove` rejects *before* any expansion
(`pegCount <= minimumPegs`, Pagoda reject, or a moveless root) returns `null` with zero
polls and would be counted as a book-proven death. Today the equality assertions against
each header's `proven-dead` count would fail loudly if this ever happened (no shipped
gap position hits the early return), so this cannot pass silently — but the heuristic's
soundness rests on that indirect equality rather than on the classification itself.
**Fix:** Classify from ground truth instead of the poll count — e.g., consult
`OpeningBook.lookup(board)` directly (`Hit`/`NoWinningMove`/`null`) to bucket the
position, and keep the poll spy solely as the "book paths never enter the solver"
assertion.

### IN-05: `GameUiState.loading()` duplicates the `initial()` constructor field-for-field

**File:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt:151-173`
**Issue:** `loading()` restates all 19 defaults of `initial()` and differs only in the
placeholder board and `isLoading = true`. A future field added to `initial()` must be
mirrored by hand here, and a divergence would surface only as a subtle loading-state
inconsistency.
**Fix:**
```kotlin
fun loading(): GameUiState =
    initial(Board(spec = PLACEHOLDER_SPEC, pegs = 0L)).copy(isLoading = true)
```

---

_Reviewed: 2026-08-30_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
