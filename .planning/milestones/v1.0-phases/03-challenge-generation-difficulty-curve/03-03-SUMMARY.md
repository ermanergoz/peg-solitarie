---
phase: 03-challenge-generation-difficulty-curve
plan: 03
subsystem: data
tags: [repository, hilt, generator-wiring, scoring, challenge-progress, pure-kotlin]

requires:
  - phase: 03-01
    provides: "LevelGenerator.generate(levelNumber) → GeneratedLevel (startingBoard/solution/generatorVersion); GeneratorConstants.GENERATOR_VERSION"
  - phase: 03-02
    provides: "Frozen v1 generator (quality gates + freeform + golden lock); generate(n) is O(n) rolling dedup from level 1"
  - phase: 02
    provides: "LevelRepository/ChallengeLevelSource seam + StubChallengeLevelSource; ScoreCalculator/ScoreParams; OnboardingRepository holder-seam shape"
provides:
  - "LevelRepository.loadChallenge(n) returns a solvable data Level carrying solution + generatorVersion (routing inside the repository, no UI branch)"
  - "GeneratedChallengeLevelSource wrapping LevelGenerator behind the existing seam (replaces the throwing stub); clamps levelNumber<1 to 1"
  - "data Level extended with nullable solution/generatorVersion (Classic stays null, unaffected)"
  - "ScoreCalculator.score/evaluate levelMultiplier pure param (D-11 level-scaled numeric score; stars unchanged)"
  - "ChallengeProgress in-memory @Singleton holder (StateFlow<Int> from 1, advanceToNext/reset) — Phase-5-persistable seam"
affects:
  - "03-04 (GameViewModel loads Challenge via loadChallenge; passes levelMultiplier; Level N string resource)"
  - "03-05 (Results Next-level advance drives ChallengeProgress.advanceToNext)"
  - "Phase 4 (hints consume Level.solution); Phase 5 (resume consumes generatorVersion; ChallengeProgress gains DataStore backing)"

tech-stack:
  added: []
  patterns:
    - "Generator routed behind the repository seam — Classic-vs-Challenge routing stays inside LevelRepository (Anti-Pattern 2)"
    - "Downstream seams carried as nullable data-model fields (solution/generatorVersion) — smallest diff, Classic untouched"
    - "Difficulty multiplier as a pure ScoreCalculator param supplied by the caller — no ScoreCalculator↔level coupling"
    - "In-memory StateFlow holder shaped as the Phase-5 persistence seam (mirrors OnboardingRepository)"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt
    - app/src/test/java/com/app/pegsolitaire/data/ChallengeProgressTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/data/LevelRepository.kt
    - app/src/main/java/com/app/pegsolitaire/data/ChallengeLevelSource.kt
    - app/src/main/java/com/app/pegsolitaire/di/DataModule.kt
    - app/src/main/java/com/app/pegsolitaire/di/DomainModule.kt
    - app/src/test/java/com/app/pegsolitaire/data/LevelRepositoryTest.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculator.kt
    - domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreParams.kt
    - domain/src/test/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculatorTest.kt

key-decisions:
  - "Carry solution + generatorVersion as nullable Level fields defaulting null (Open-Q1) — Classic levels stay null and the Classic path is byte-unchanged"
  - "GeneratedChallengeLevelSource clamps levelNumber<1 to 1 at the data boundary (T-03-07) before the untrusted number reaches the generator"
  - "displayName is a stable non-UI id (challenge-N); the visible 'Level N' copy is a UI string resource deferred to 03-04 (no Android resource lookup in the data layer)"
  - "levelMultiplier scales only the whole numeric score base; stars() is never re-based (D-11 — a 1-peg solve stays 3 stars)"
  - "ChallengeProgress is in-memory only this phase (D-04), shaped as the Phase-5 DataStore seam"

patterns-established:
  - "Repository-internal routing: loadChallenge maps level# → generator → data Level; UI never branches on mode"
  - "TDD RED→GREEN for the score multiplier + progress holder (failing test commit, then implementation commit)"

requirements-completed: [CHAL-01, CHAL-03, CHAL-04]

duration: 4min
completed: 2026-07-01
---

# Phase 3 Plan 03: Generator Behind the Repository Seam Summary

**The frozen v1 generator now flows through `LevelRepository.loadChallenge(n)` as a solvable data `Level` carrying its recorded `solution` + `generatorVersion`, plus a pure D-11 level-scaled score and an in-memory `ChallengeProgress` holder — all routing kept inside the repository so no UI branches on Classic-vs-Challenge.**

## Performance

- **Duration:** ~4 min
- **Started:** 2026-07-01T19:09:15Z
- **Completed:** 2026-07-01T19:14:00Z (approx)
- **Tasks:** 2 (Task 2 via TDD)
- **Files created/modified:** 10 (2 created, 8 modified)

## Accomplishments

- **Generator routed behind the seam** — replaced the throwing `StubChallengeLevelSource` with `GeneratedChallengeLevelSource`, which injects the pure-domain `LevelGenerator`, clamps `levelNumber < 1` to 1 (T-03-07), and maps `GeneratedLevel → Level` carrying `solution` + `generatorVersion` (T-03-08). `LevelRepository.loadChallenge` now returns a real, solvable board through the exact path Classic uses.
- **`Level` extended non-invasively** — two nullable fields (`solution: List<Move>? = null`, `generatorVersion: Int? = null`) so Classic levels stay `null` and the Classic path is untouched (Open-Q1 smallest-diff decision).
- **DI rebind** — `DataModule` binds `GeneratedChallengeLevelSource`; `DomainModule` provides the annotation-free `LevelGenerator` object as an `:app` seam so constructor injection resolves. Hilt graph builds; `StubChallengeLevelSource` fully removed (`grep` clean).
- **D-11 level-scaled score** — `ScoreCalculator.score`/`evaluate` gained a pure `levelMultiplier` param (default `DEFAULT_LEVEL_MULTIPLIER = 1`, a named `ScoreParams` const) that scales only the whole numeric base. Default is byte-identical to Phase-2 (Classic unchanged); `> 1` is strictly larger; `stars()` is never re-based.
- **`ChallengeProgress` holder** — an in-memory `@Singleton` exposing `currentLevel: StateFlow<Int>` starting at 1 with `advanceToNext()`/`reset()` (D-04), shaped as the Phase-5 persistence seam (mirrors `OnboardingRepository`). Zero Android deps → JVM-unit-tested.

## Task Commits

Each task committed atomically:

1. **Task 1: Level solution/version carry-through + GeneratedChallengeLevelSource + DI rebind** — `dee2869` (feat)
2. **Task 2: Level-scaled score (D-11) + ChallengeProgress holder** — `b120937` (test RED) → `7df9a62` (feat GREEN)

_No REFACTOR commit — GREEN was already clean (ktlint/detekt green)._

## Files Created/Modified

- `data/ChallengeLevelSource.kt` — `GeneratedChallengeLevelSource` wrapping `LevelGenerator`; clamp + `GeneratedLevel→Level` mapping.
- `data/LevelRepository.kt` — `Level` extended with nullable `solution`/`generatorVersion`; interface/impl doc reflects real routing.
- `data/ChallengeProgress.kt` — in-memory current-level holder (`StateFlow<Int>`, advance/reset).
- `di/DataModule.kt` — `@Binds` rebound to `GeneratedChallengeLevelSource`; stub import removed.
- `di/DomainModule.kt` — `@Provides` `LevelGenerator` seam.
- `scoring/ScoreCalculator.kt` / `ScoreParams.kt` — pure `levelMultiplier` param + `DEFAULT_LEVEL_MULTIPLIER` const.
- Tests: `LevelRepositoryTest.kt` (solvable Level + version + clamp), `ScoreCalculatorTest.kt` (three D-11 behaviors), `ChallengeProgressTest.kt` (start/advance/reset).

## Decisions Made

None beyond the plan — all four planner decisions (nullable carry-through, boundary clamp, non-UI displayName, pure multiplier) were followed as specified.

## Deviations from Plan

None - plan executed exactly as written.

The only note: the plan's verify commands used `:app:test --tests ...`, but the `:app:test` aggregate task does not accept `--tests`; the equivalent `:app:testDebugUnitTest --tests ...` (and `:domain:test` for the pure module) were run instead. No behavior change — same tests, correct task names.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- **03-04/03-05 unblocked:** Challenge levels are reachable through `LevelRepository.loadChallenge(n)` exactly like Classic — the gameplay/results/home wiring is a thin diff (load path generalization, `levelMultiplier` pass-through, `ChallengeProgress.advanceToNext` on the Next-level action).
- **Performance reminder (from 03-02):** `generate(n)` is O(n) (rolling dedup from level 1). The 03-04 wiring should advance/cache the sequence incrementally via `ChallengeProgress` rather than call `generate(highLevel)` cold each load.
- **Seams carried:** `Level.solution` is ready for Phase 4 hints; `Level.generatorVersion` and the `ChallengeProgress` seam are ready for Phase 5 resume/persistence.

## Known Stubs

None. `ChallengeProgress` being in-memory (not DataStore-backed) is the intentional D-04 scope for this phase — Phase 5 (CHAL-05/PERS-02) backs it with persistence behind the same interface. `displayName = challenge-N` is a deliberate non-UI label; the human-facing "Level N" string is added in 03-04 per plan.

## Self-Check: PASSED

- All 6 spot-checked created/modified files verified present on disk.
- All 3 task commits (`dee2869`, `b120937`, `7df9a62`) verified in git log.
- Full wave gate `./gradlew ktlintCheck detekt :domain:test :app:test assembleDebug` — BUILD SUCCESSFUL.
- `DomainPurityTest` green (`ScoreCalculator` change is pure; `ChallengeProgress` lives in `:app`).
- `grep -rn "StubChallengeLevelSource" app/src` returns nothing.

---
*Phase: 03-challenge-generation-difficulty-curve*
*Completed: 2026-07-01*
