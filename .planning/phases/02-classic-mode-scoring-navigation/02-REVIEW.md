---
phase: 02-classic-mode-scoring-navigation
reviewed: 2026-06-30T00:00:00Z
depth: standard
files_reviewed: 50
findings:
  critical: 1
  warning: 3
  info: 2
  total: 6
status: issues_found
---

# Phase 02: Code Review Report

**Reviewed:** 2026-06-30
**Depth:** standard
**Files Reviewed:** ~50 (source + tests)
**Status:** issues_found

## Summary

The Classic-mode scoring/navigation slice is high quality: domain purity is preserved
(scoring is pure Kotlin, no `android.*`), state hoisting is clean (stateless screens over
`StateFlow` UiState), navigation is fully type-safe, the no-magic-number / dimens conventions
are followed rigorously, and the one-shot finish event is correctly guarded against re-firing.
Test coverage is broad and the legal-move "never trust the tap" control (re-deriving the move
from `RulesEngine.legalMoves`) is sound.

The headline concern is **Diamond-41**: it ships in the playable Classic catalog with no
solvability proof, while its source KDoc falsely claims one exists — a direct conflict with the
project's stated core value ("puzzles must be solvable"). There are also two robustness defects
in scoring/persistence whose tests give false confidence.

## Critical Issues

### CR-01: Diamond-41 ships as a "guaranteed-solvable" Classic board with no solvability proof, and its KDoc falsely claims one exists  — ✅ RESOLVED (commit 6b3d77d)

> **Resolution:** Traced to a faulty requirement, not a code defect. The reference game scores on fewest pegs remaining (no one-peg win), and the GF(4) colour invariant proves the canonical center-start diamond has no single-peg solution. Diamond-41 realigned to its authentic center start; all false D-19 fixture claims removed and replaced with the GF(4) rationale across Boards.kt / SolutionTest.kt / AuthoringSolver.kt. D-19 now correctly scopes to the four cross boards that genuinely reduce to one peg. WR-01/WR-02/WR-03/IN-01/IN-02 remain open as advisory follow-ups.


**File:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Boards.kt` (Diamond41Layout KDoc + spec)
**Also:** `app/.../data/ClassicLevelSource.kt` (ships it in the catalog);
`domain/.../golden/solutions/AuthoringSolver.kt` (`SolutionFixtures.ALL` excludes it);
`domain/.../golden/solutions/SolutionTest.kt` (only checks Diamond-41's hole count / start peg count, never solvability)

**Issue:** `Diamond41Layout`'s KDoc states the off-center start `(4,2)` is *"validated by the
authoring solver and locked with a recorded-solution fixture (D-19)."* This is provably false:
`AuthoringSolver` explicitly says the 41-hole Diamond-41 is NOT handled, and `SolutionFixtures.ALL`
omits it (`english33`, `european37`, `asymmetric39`, `wiegleb45` only). `SolutionTest` has no
Diamond-41 replay. Yet `DefaultClassicLevelSource` ships Diamond-41 as a fully playable catalog
board. The result is a shipped Classic board whose start `(4,2)` has zero automated proof it
reduces to one peg, and a self-contradicting KDoc that misleads the next maintainer.

**Fix:** Either generate and commit a Diamond-41 recorded-solution fixture (needs pagoda-function
pruning the authoring solver lacks) and add the matching `SolutionTest` replay; or, until then,
remove Diamond-41 from the shipped catalog AND correct the false KDoc.

## Warnings

### WR-01: Integer overflow in the time-bonus formula silently bypasses the "floored at 0" contract; the unit test masks it

**File:** `domain/.../scoring/ScoreCalculator.kt` (`score(...)` time-bonus term)
**Issue:** `TIME_BONUS_CAP - elapsedSeconds * DECAY_PER_SECOND` computes `elapsedSeconds * 10` in
`Int` arithmetic. For large `elapsedSeconds` the product overflows and wraps negative, so
`CAP - (negative)` yields a positive bonus instead of the intended 0 floor.
`ScoreCalculatorTest.scoreNeverDropsBelowThePegBaseRegardlessOfElapsedSeconds` passes
`Int.MAX_VALUE`, which wraps and makes `isAtLeast(pegBase)` pass for the wrong reason.
Unreachable in real play (~214M seconds) but a latent bug with a misleading test.
**Fix:** Compute the decay in `Long` (or clamp `elapsedSeconds`), and strengthen the test to
assert the bonus term is exactly 0 at the extreme.

### WR-02: Onboarding read fails open on every Throwable, not just IOException — masks programming errors

**File:** `app/.../data/OnboardingRepository.kt` (`.catch { emit(emptyPreferences()) }`)
**Issue:** Swallows all non-cancellation `Throwable`s, degrading genuine bugs to "not seen." The
D-16 fail-open contract is about corrupt/IO reads only. The test exercises only `IOException`.
**Fix:** `.catch { if (it is IOException) emit(emptyPreferences()) else throw it }`.

### WR-03: `nextIdFrom` divides by `order.size` with no empty-list guard (latent ArithmeticException)

**File:** `app/.../ui/results/ResultsViewModel.kt` (`nextIdFrom`)
**Issue:** `order[(indexOf(currentId) + NEXT_OFFSET) % order.size]` throws `/ by zero` if `order`
is empty. Unreachable today (catalog always 5) but unguarded for a future filtered/empty catalog.
**Fix:** Guard the empty case (return `currentId`) or `require(order.isNotEmpty())`.

## Info

### IN-01: Stale dependency comment claims only one icon is used

**File:** `app/build.gradle.kts` (Material Symbols comment)
**Issue:** Comment says only `Icons.Rounded.Refresh` is used; Phase 2 added Info, ArrowBack, and
Star/Star-outline icons. Update or drop the specific-icon claim.

### IN-02: Results headline shows "Solved!" for an out-of-moves (Stuck) game that left 2–3 pegs

**File:** `app/.../ui/results/ResultsViewModel.kt` + `ResultsScreen.kt`
**Issue:** Headline tier derives purely from star count; a Stuck finish that leaves 2–3 pegs shows
"Solved!". Consistent with the documented D-08 star-bucketing (by-design) but a possible copy
mismatch — confirm with spec owner. If undesired, thread terminal `GameStatus` through the
`Results` route.

---

_Reviewed: 2026-06-30 · Reviewer: gsd-code-reviewer · Depth: standard_
