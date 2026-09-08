---
phase: 12-leaderboards-achievements
plan: 02
subsystem: domain
tags: [kotlin, achievements, play-games, pure-domain, junit4, truth, tdd]

# Dependency graph
requires:
  - phase: 02-scoring
    provides: "ScoreParams.THREE_STARS and ScoreCalculator's per-board 3-star banding, which is what lets Perfectionist reduce to a star comparison instead of a per-board peg table"
  - phase: 09-ads-assists
    provides: "AssistAllowance.fromStoredOrFull — the clamp-don't-refuse posture SolveTelemetry.coerced copies"
provides:
  - "AchievementId — the fixed 20-constant roster with per-tier stepTotal and an isIncremental marker"
  - "AchievementThresholds — the 12 named constants (ladder 10/50/100, stars 25/100/250, early level 5, 60s + derived ms, 3 perfect stars, derived CLASSIC_BOARD_TOTAL)"
  - "ClassicBoard — the domain-side identity of the five shipped Classic boards, in ascending hole-count order"
  - "ProgressAggregate — the SQL-shaped snapshot that is the SOLE input to the rules; 12-06's projection contract"
  - "AchievementRules.evaluate + AchievementOutcome — one pure pass serving both live solve and offline backfill"
  - "SolveTelemetry — the per-solve carrier (hints, undos, lastPegOnTarget) with NONE and a clamping coerced factory"
affects: [12-05 telemetry plumbing, 12-06 SQL aggregates, 12-07 submission loop, 12-08 Play Console checklist]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Scoped constants object with DERIVED values (SPEED_SOLVE_MAX_ELAPSED_MS from seconds x MILLIS_PER_SECOND), extending the ScoreParams / InterstitialCadenceParams house style"
    - "Enum constant carrying its own policy threshold as a constructor arg, so the tier and its step total are named once"
    - "Rules expressed as conditional additions inside buildSet/buildMap rather than early-return chains, to stay under detekt ReturnCount/CyclomaticComplexMethod"
    - "Exhaustive when over a domain enum with NO else branch, so adding a board fails the build rather than shipping a silent gap"

key-files:
  created:
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/telemetry/SolveTelemetry.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/achievement/AchievementId.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/achievement/AchievementThresholds.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/achievement/ProgressAggregate.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/achievement/AchievementRules.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/telemetry/SolveTelemetryTest.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/achievement/AchievementCatalogTest.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/achievement/AchievementRulesTest.kt
  modified: []

key-decisions:
  - "clearedClassicBoards/perfectClassicBoards are per-board Sets, not the two bare counts RESEARCH sketched — the five CLEARED_* achievements need per-board identity anyway, and deriving Grand tour/Perfectionist from the same sets makes a count-vs-set disagreement unrepresentable"
  - "AchievementRules.kt names no per-solve telemetry TYPE at all (the plan's own acceptance gate greps for zero occurrences); the KDoc states the exclusion in prose instead"
  - "PGSA-01/02/03 were NOT marked complete in REQUIREMENTS.md — this plan delivers only the :domain kernel of each, and 12-05/06/07/08 claim the same IDs"

patterns-established:
  - "AchievementOutcome invariant pair: steps never carries a value <= 0, and unlocked/steps are disjoint — both asserted, both consumed by 12-07's submission loop"
  - "Absolute-not-delta step reporting: every tier on an axis receives the same unclamped aggregate value, which is what makes a resubmitted backfill idempotent"
  - "Untrusted-boundary clamping factory (SolveTelemetry.coerced) that bounds without policing — negatives clamped, large forged values preserved"

requirements-completed: []

# Metrics
duration: 26 min
completed: 2026-09-08
---

# Phase 12 Plan 02: Achievement Kernel Summary

**The pure-Kotlin achievement contract for Phase 12: a 20-constant `AchievementId` roster with six incremental tiers, a 12-constant `AchievementThresholds` object, the SQL-shaped `ProgressAggregate`, a clamping `SolveTelemetry` carrier, and a single `AchievementRules.evaluate` pass that yields absolute tier steps and never emits a zero.**

## Performance

- **Duration:** 26 min
- **Started:** 2026-09-08T21:32:41Z
- **Completed:** 2026-09-08T21:58:52Z
- **Tasks:** 2 (Task 2 executed as RED → GREEN)
- **Files created:** 8

## Accomplishments

- **The roster is now structurally fixed at 20.** `AchievementId` carries exactly 20 constants, six of them incremental with their `AchievementThresholds` tier as `stepTotal`, and `AchievementCatalogTest` fails the build if a constant is silently dropped or added — the desync that would otherwise only surface as an unearnable Play Console entry on a device.
- **`AchievementRules.evaluate` is one total pure function of `ProgressAggregate`.** The same call serves a live solve and the offline backfill (D-11); there is no single-solve overload and no second rule set. 27 JVM tests cover every one of the 20 achievements, including a maximal-aggregate test proving `unlocked + steps.keys == AchievementId.entries.toSet()`.
- **The two `AchievementOutcome` invariants are enforced, not just documented.** `steps` never carries a value `<= 0` (T-12-05 — a `setSteps(id, 0)` would be rejected and, under D-10, leave every fresh install with an unclearable pending flag), and `unlocked`/`steps` are disjoint (no tiered id is ever unlocked directly).
- **The 60-second speed bound and the five-board denominator each exist exactly once.** `SPEED_SOLVE_MAX_ELAPSED_MS` is derived from the seconds constant, and `CLASSIC_BOARD_TOTAL` is derived from `ClassicBoard.entries.size` — so a sixth board moves the Grand tour denominator automatically instead of leaving a stale literal that hands the achievement out one board early.
- **`:domain` stays provably Android-free and ID-free.** No `android.*`/`androidx.*` import, no `CgkI` Play Games ID literal, no `:app` level-id string, and no logging call reached the new packages; the ArchUnit bytecode gate confirms it.

## Task Commits

1. **Task 1: Define the achievement value types — roster, thresholds, aggregate, telemetry** — `eb2231c` (feat)
2. **Task 2 (RED): failing spec for AchievementRules** — `81fa550` (test) — 27 tests, 13 failing against the stub
3. **Task 2 (GREEN): implement the pure achievement rules kernel** — `2830b35` (feat)

No REFACTOR commit: the GREEN implementation was already in its final decomposed shape (`evaluate` as a two-line composition over `unlockedAchievements` / `tierSteps` / `clearedAchievement`), so there was nothing to clean up without churn.

**Branch:** `worktree-agent-a45cbbd835cc08524` (parallel wave-1 worktree)

## Files Created

- `domain/.../telemetry/SolveTelemetry.kt` — `SolveTelemetry(hintsUsed, undosUsed, lastPegOnTarget)` plus `NONE` and the clamping `coerced` factory. Rides alongside `ScoreResult`; deliberately not an input to the rules.
- `domain/.../achievement/AchievementThresholds.kt` — the 12 named constants, each KDoc'd to its decision.
- `domain/.../achievement/AchievementId.kt` — the 20-constant roster, `stepTotal` per tier, `isIncremental` marker.
- `domain/.../achievement/ProgressAggregate.kt` — `ClassicBoard` enum + the fully-defaulted aggregate value type.
- `domain/.../achievement/AchievementRules.kt` — `AchievementOutcome` + the single pure `evaluate` pass (153 lines).
- `domain/src/test/.../telemetry/SolveTelemetryTest.kt` — 7 tests over the clamp posture.
- `domain/src/test/.../achievement/AchievementCatalogTest.kt` — 10 shape/drift guards over the roster and thresholds.
- `domain/src/test/.../achievement/AchievementRulesTest.kt` — 27 tests, the full behaviour spec (310 lines).

## Contracts Recorded for Downstream Plans

### `ProgressAggregate` — 12-06's SQL projection contract

| Property | Type | Source |
|---|---|---|
| `highestChallengeLevel` | `Int? = null` | `MAX(CAST(levelId AS INTEGER))` over `mode='challenge'`; NULL when none |
| `challengeScoreTotal` | `Long? = null` | `SUM(bestScore)` over `mode='challenge'` — **`Long`**, Pitfall 6. Leaderboard-only; the rules ignore it |
| `totalStars` | `Int = 0` | `SUM(stars)` both modes, coalesced at the DAO |
| `solvedLevelCount` | `Int = 0` | `COUNT(*)` both modes |
| `threeStarCount` | `Int = 0` | `COUNT(*) WHERE stars >= 3` |
| `clearedClassicBoards` | `Set<ClassicBoard> = emptySet()` | Classic ids with any stored result |
| `perfectClassicBoards` | `Set<ClassicBoard> = emptySet()` | Classic ids whose best is `stars >= PERFECT_STARS` |
| `anyHintFreeSolve` | `Boolean = false` | `EXISTS(... hintsUsed = 0)` — mode-agnostic |
| `anyUndoFreeSolve` | `Boolean = false` | `EXISTS(... undosUsed = 0)` — mode-agnostic |
| `anyCenterFinish` | `Boolean = false` | `EXISTS(... mode='classic' AND levelId=<English-33> AND lastPegOnTarget=1)` — that board only |
| `anySpeedSolve` | `Boolean = false` | `EXISTS(... elapsedMs < AchievementThresholds.SPEED_SOLVE_MAX_ELAPSED_MS)` |

Every property is defaulted, so a fresh player is exactly `ProgressAggregate()`.

**The `String` ↔ `ClassicBoard` mapping is 12-06's to own**, at the data boundary. `:domain` has no level-id string and must not gain one.

### `AchievementOutcome` — 12-07's submission loop contract

`AchievementOutcome(unlocked: Set<AchievementId>, steps: Map<AchievementId, Int>)`.
- `unlocked` → `GamesService.unlockAchievement`; one-shots only, **never** a tiered id.
- `steps` → `GamesService.setAchievementSteps` with the value verbatim; **absolute, unclamped**, and the map never contains a value `<= 0`.
- The two collections are **disjoint** by construction, so a submitter can iterate both without a de-dup check.

### Still owed by plan 12-08

Display names, descriptions, point values and 512×512 icons for **all 20** `AchievementId` constants, plus the ROADMAP External Prerequisites row (budgeted 10–12 icons, actually needs 20). None of these may enter `:domain`; the `AchievementId → String` ID map is 12-04/12-07's `platform/games/GamesIds.kt`.

## Decisions Made

1. **Per-board `Set`s, not counts.** 12-RESEARCH sketched `classicBoardsSolved`/`classicBoardsPerfect` as `Int` counts. The five `CLEARED_*` achievements need per-board identity regardless, so carrying two counts *and* per-board identity would be two representations of one fact. Deriving Grand tour and Perfectionist from `containsAll(ClassicBoard.entries)` over the same sets makes a count-disagrees-with-its-set bug unrepresentable.
2. **`AchievementRules.kt` names no telemetry type.** The plan's action text asked the KDoc to say "`SolveTelemetry` is NOT a parameter", but the plan's own acceptance gate requires `grep -c "SolveTelemetry" AchievementRules.kt` to return `0`. The gate won: the KDoc states the exclusion in prose ("per-solve facts … are not a parameter here"), which satisfies both the intent and the gate.
3. **No `REFACTOR` commit.** The GREEN implementation already had the decomposition the plan mandated; a refactor commit would have been churn with no diff worth reading.
4. **`AchievementCatalogTest` also asserts `EARLY_CHALLENGE_LEVEL < LADDER_TIER_1`.** Not in the plan, but it pins D-02's actual intent — the one-shot must be reachable before the incremental ladder shows meaningful progress — which a bare "5" assertion would not.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Missing `local.properties` blocked the `:app` ArchUnit gate**
- **Found during:** Task 2 (plan-level verification)
- **Issue:** `./gradlew :app:testDebugUnitTest --tests "*DomainPurityTest*"` failed with `SDK location not found` — `local.properties` is gitignored, so it lives in the main checkout and was absent from this parallel worktree. The `:domain` module is pure JVM and was unaffected; only the Android-module gate could not configure.
- **Fix:** Copied the main checkout's `local.properties` into the worktree root. It is gitignored, so nothing entered any commit.
- **Files modified:** none tracked (`local.properties` is gitignored)
- **Verification:** `./gradlew :app:testDebugUnitTest --tests "*DomainPurityTest*" --offline` → BUILD SUCCESSFUL
- **Committed in:** n/a — build-environment fix, no tracked file changed

**2. [Rule 3 - Blocking] ktlint `class-signature` rejected the single-line enum header**
- **Found during:** Task 1
- **Issue:** `enum class AchievementId(val stepTotal: Int? = null) {` tripped "Newline expected after opening parenthesis" — the `ktlint_official` style forces a multiline class signature at one or more constructor parameters, regardless of line length.
- **Fix:** Wrapped the constructor parameter onto its own line.
- **Files modified:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/achievement/AchievementId.kt`
- **Verification:** `./gradlew :domain:ktlintCheck --offline` → BUILD SUCCESSFUL; the 20-constant grep gate still returns 20 (the wrapped `val` line is lowercase and does not match the constant pattern)
- **Committed in:** `eb2231c` (Task 1 commit)

### Acceptance criteria that could not be met as literally written

**`grep -rn "ClassicLevelIds\|\"english33\"\|..." domain/src | wc -l` returns 0** — this gate is unachievable at repository scope and always was: 15 pre-existing `:domain` files (including `BoardConstants.kt`, `Boards.kt`, `OpeningBook.kt` and the golden-solution test fixtures) mention those names in **KDoc prose and test fixture keys**, producing a baseline of 61 matches before this plan ran. Fixing them is out of scope (they belong to Phases 2–8 and are not this task's changes).

The meaningful form of the gate was met, and verified two ways:
- `grep -rn "ClassicLevelIds\|\"english33\"\|..." <the two new packages> | wc -l` → **0** (this plan contributes nothing to the baseline; the one prose mention drafted into `ProgressAggregate`'s KDoc was removed)
- `grep -rn "\"english33\"\|..." domain/src/main | grep -v '^\s*\*'` → **0** — no id-string *literal* exists anywhere in `:domain` production code, only in comments

Logged rather than silently skipped, per the deviation protocol.

---

**Total deviations:** 2 auto-fixed (2 blocking), 1 acceptance criterion met in spirit with the divergence documented.
**Impact on plan:** No scope change. Both fixes were environmental/stylistic; neither altered the contracts this plan exists to settle.

## Issues Encountered

**Shared-file writes deliberately skipped.** `STATE.md` and `ROADMAP.md` were not touched (orchestrator-owned in parallel mode). `REQUIREMENTS.md` was also **not** updated: PGSA-01, PGSA-02 and PGSA-03 are each claimed by later plans too (PGSA-01 by 12-08; PGSA-02 by 12-01/05/06/07; PGSA-03 by 12-06/07), and this plan delivers only their `:domain` kernel — marking them complete now would both close them prematurely and race sibling agents 12-01 and 12-03 on the same file. `requirements-completed` is therefore empty by intent, and the orchestrator or the closing plans should mark them.

## Verification Results

| Gate | Result |
|---|---|
| `./gradlew :domain:test --tests "*SolveTelemetryTest*" --tests "*AchievementCatalogTest*" --tests "*AchievementRulesTest*"` | PASS |
| `./gradlew :domain:test` (full module, no regression) | PASS |
| `./gradlew :app:testDebugUnitTest --tests "*DomainPurityTest*"` (ArchUnit bytecode gate) | PASS |
| `./gradlew detekt ktlintCheck` (all modules) | PASS |
| `grep -c "^    [A-Z0-9_]*[(,]" AchievementId.kt` == 20 | PASS |
| `grep -rEn "^import (android\|androidx)\." <new packages>` == 0 | PASS |
| `grep -rn "CgkI" domain/src` == 0 | PASS |
| `grep -n "challengeScoreTotal: Long?" ProgressAggregate.kt` | PASS |
| `AchievementThresholds.kt` constant-declaration count == 12 | PASS |
| `grep -c "SolveTelemetry" AchievementRules.kt` == 0 | PASS |
| `grep -c "else ->" AchievementRules.kt` == 0 | PASS |
| `evaluate` takes `ProgressAggregate` as its only parameter | PASS |
| TDD gate sequence: `test(12-02)` → `feat(12-02)` commits present | PASS |

`:domain:check` was deliberately NOT run — it pulls in the ~11-minute `depthSweep`, and the plan names `:domain:test` as this plan's gate.

## Known Stubs

None. The `evaluate` stub committed at the RED gate (`81fa550`) was replaced by the real implementation at GREEN (`2830b35`); no placeholder, TODO or hardcoded empty value remains in the five production files.

## Threat Flags

None. This plan adds no network endpoint, no auth path, no file access and no schema — it is five pure-JVM value/rule files with no I/O of any kind. All four threats in the plan's register (T-12-04 tampering, T-12-05 DoS via zero steps, T-12-06 telemetry tampering, T-12-07 ID disclosure) were mitigated as specified, with T-12-04's client-side-forgery residual explicitly accepted per PITFALLS.md's standing answer for a local-first single-player game.

## User Setup Required

None — no external service configuration, no dependency, no version-catalog change. Play Console setup for the 20 achievements is plan 12-08's checklist.

## Next Phase Readiness

- **Ready for 12-05** (`SolveTelemetry` plumbing through the finish event and Results nav args — the type and its `coerced` factory exist).
- **Ready for 12-06** (SQL aggregates — the `ProgressAggregate` projection contract is fixed and tabulated above; note the `String` ↔ `ClassicBoard` mapping is 12-06's to own).
- **Ready for 12-07** (submission loop — `AchievementOutcome`'s disjointness and never-zero-steps invariants are enforced and tested, so the submitter needs no defensive filtering).
- **Blocker for 12-08, unchanged:** names, descriptions, point values and 20 icons are still owed, and the ROADMAP External Prerequisites row still budgets 10–12 icons against an actual roster of 20.

## Self-Check: PASSED

All 8 created files verified present on disk; all 3 commit hashes (`eb2231c`, `81fa550`, `2830b35`) verified present in `git log`; all task acceptance criteria and all 6 plan-level verification commands re-run green.

---
*Phase: 12-leaderboards-achievements*
*Completed: 2026-09-08*
