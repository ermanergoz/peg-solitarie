---
phase: 02-classic-mode-scoring-navigation
plan: 01
subsystem: domain
tags: [scoring, kotlin, pure-domain, tdd, stars]

# Dependency graph
requires:
  - phase: 01-foundation
    provides: pure-Kotlin :domain module shape (RulesEngine object, Board.pegCount), scoped-constant + KDoc convention, JUnit4 + Truth test harness, domain-purity gate
provides:
  - ScoreCalculator pure-domain object with stars() + score() + evaluate()
  - ScoreParams scoped scoring constants (D-01 star bands, D-03 score formula)
  - ScoreResult immutable pure-domain scoring outcome model
affects: [02-07-results, game-route, results-screen]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Pure-domain scoring service mirrors RulesEngine: stateless object, plain-Int inputs, zero Android deps"
    - "Scoped scoring constants in ScoreParams (no magic numbers, every const KDoc'd to its decision id)"

key-files:
  created:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreParams.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreResult.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculator.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculatorTest.kt
  modified: []

key-decisions:
  - "Stars derived solely from pegs remaining (D-01/D-02); ScoreCalculator.stars takes no target argument"
  - "Time bonus floored at 0 (maxOf) so score never drops below the peg base — stars and score stay independent axes"
  - "Extracted structural star counts (3/2) into ScoreParams.THREE_STARS/TWO_STARS to satisfy detekt MagicNumber gate"

patterns-established:
  - "ScoreCalculator: stateless pure-domain object with Int-only inputs, no domain-type coupling"
  - "ScoreParams: per-feature scoped-constants object, each const tied to a decision id (QUAL-03)"

requirements-completed: [SCOR-01, SCOR-02]

# Metrics
duration: 6min
completed: 2026-06-28
---

# Phase 2 Plan 01: ScoreCalculator Summary

**Pure-Kotlin ScoreCalculator delivering SCOR-01 star bands (1→3, 2-3→2, >3→1, never 0) and SCOR-02 peg-base + time-decay score (floored at 0), backed by scoped ScoreParams constants and full JVM TDD coverage with zero Android dependencies.**

## Performance

- **Duration:** ~6 min
- **Started:** 2026-06-28T19:38:00Z
- **Completed:** 2026-06-28T19:43:27Z
- **Tasks:** 2 (TDD RED → GREEN)
- **Files modified:** 4 created

## Accomplishments
- `ScoreCalculator.stars(pegsRemaining)` — D-01 bands: `<=1`→3, `2-3`→2, `>3`→`MIN_STARS` (1, never 0)
- `ScoreCalculator.score(pegsCleared, elapsedSeconds)` — D-03: `pegsCleared * PEG_VALUE + maxOf(0, TIME_BONUS_CAP - elapsedSeconds * DECAY_PER_SECOND)`, never below the peg base
- `ScoreCalculator.evaluate(...)` — assembles `ScoreResult` for the 02-07 Results caller
- `ScoreParams` scoped constants (THREE_STAR_MAX_PEGS, TWO_STAR_MAX_PEGS, MIN_STARS, THREE_STARS, TWO_STARS, PEG_VALUE, TIME_BONUS_CAP, DECAY_PER_SECOND), each KDoc'd to its decision id
- `ScoreCalculatorTest` — 6 behavior assertions (boundary stars, 2-3 band, >3 floor, full-bonus score, decayed-to-base score, never-negative time term)
- Domain purity preserved: zero `android.*`/`androidx.*` imports (DomainPurityTest green)

## Task Commits

Each task was committed atomically:

1. **Task 1: RED — ScoreCalculatorTest + ScoreParams + ScoreResult** - `9bb328f` (test)
2. **Task 2: GREEN — ScoreCalculator** - `acffae0` (feat)

_TDD gate sequence verified in git log: test() RED commit precedes feat() GREEN commit._

## Files Created/Modified
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreParams.kt` - Scoped D-01/D-03 scoring constants
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreResult.kt` - Immutable scoring outcome (stars, score, pegsRemaining, pegsCleared, elapsedSeconds)
- `domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculator.kt` - Pure stars()/score()/evaluate() authority
- `domain/src/test/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculatorTest.kt` - SCOR-01/02 behavior spec

## Decisions Made
- Stars depend solely on pegs remaining; the board target/center hole is ignored as a star condition (D-02) — `stars()` takes no target argument
- Time term floored via `maxOf(0, …)` so score is monotonic-safe at and below the peg base regardless of elapsed time

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Extracted structural star counts into ScoreParams to pass the detekt MagicNumber gate**
- **Found during:** Task 2 (GREEN — ScoreCalculator)
- **Issue:** The plan/PATTERNS treated the literal star counts `3`/`2` in the `stars()` `when` as acceptable structural literals, but this project's detekt config flags `3` (its default ignore list covers only 0/1/2), failing the mandatory `detekt` acceptance gate (`ScoreCalculator.kt:24 MagicNumber`).
- **Fix:** Added `THREE_STARS = 3` and `TWO_STARS = 2` to `ScoreParams` (KDoc'd to D-01) and referenced them from `stars()`; updated the class KDoc accordingly. This also tightens compliance with CLAUDE.md's no-magic-numbers standard.
- **Files modified:** ScoreParams.kt, ScoreCalculator.kt
- **Verification:** `./gradlew :domain:test --tests *ScoreCalculatorTest* :app:testDebugUnitTest --tests *DomainPurityTest* ktlintCheck detekt` → BUILD SUCCESSFUL (exit 0)
- **Committed in:** acffae0 (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 blocking gate fix)
**Impact on plan:** Necessary to pass the mandatory detekt gate and the project's no-magic-number standard. No behavioral change, no scope creep — `stars()` returns identical values.

## Issues Encountered
- detekt rejected the structural `3` literal (resolved via the Rule 3 deviation above). `2` was not flagged (in detekt's default ignore set) but was extracted alongside `3` for symmetry and full no-magic-number compliance.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- SCOR-01/SCOR-02 scoring foundation is complete and consumable by the Results destination (02-07): `GameRoute` computes `ScoreCalculator.evaluate(pegsRemaining, pegsCleared, elapsedSeconds)` at game end and routes the `ScoreResult` to Results.
- `ScoreCalculator` takes plain Ints only — no coupling to domain types; the caller supplies values from the validated `Board.pegCount` and the in-VM timer.

## Self-Check: PASSED

- Files: all 4 present (ScoreParams.kt, ScoreResult.kt, ScoreCalculator.kt, ScoreCalculatorTest.kt)
- Commits: 9bb328f (test/RED), acffae0 (feat/GREEN) both present in git log

---
*Phase: 02-classic-mode-scoring-navigation*
*Completed: 2026-06-28*
