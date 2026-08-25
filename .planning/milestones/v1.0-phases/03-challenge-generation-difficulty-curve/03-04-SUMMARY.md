---
phase: 03-challenge-generation-difficulty-curve
plan: 04
subsystem: ui
tags: [navigation, viewmodel, compose, challenge, scoring, state-hoisting]

requires:
  - phase: 03-03
    provides: "LevelRepository.loadChallenge(n) → solvable Level; ScoreCalculator.evaluate(..., levelMultiplier); DifficultyTiers.forLevel(n).scoreMultiplier"
  - phase: 02
    provides: "Type-safe @Serializable routes (Gameplay/Results); the single GameViewModel + GameRoute/GameScreen Route/Screen/Leaf split; GameTopBar title slot"
provides:
  - "Unified LevelRef: Gameplay/Results routes carry an optional challengeLevel (one route arg drives both mode paths, Open-Q2)"
  - "GameViewModel loads either mode from the route arg alone (loadChallenge when set, else loadClassic) — no if(challenge) in board/game logic"
  - "challengeLevel < 1 clamped at the VM boundary (T-03-09); D-11 level multiplier applied at finish"
  - "Context-aware 'Level N' gameplay top-bar title (D-03/S2), neutral onSurface; Classic keeps the app-name title"
  - "challenge_level_indicator / _desc string resources"
affects:
  - "03-05 (Results reads Results.challengeLevel for the mode-aware Next-level/Retry actions; Home enables the Challenge tile → Gameplay(challengeLevel = n))"

tech-stack:
  added: []
  patterns:
    - "Unified LevelRef via one nullable route arg — the lone GameViewModel branches on challengeLevel only; mode never leaks into board/selection/jump logic (Anti-Pattern 2)"
    - "Nav args carried as nullable fields with defaults — Classic call sites stay valid unchanged (smallest-diff, mirrors 03-03 Level carry-through)"
    - "Level multiplier read from pure :domain DifficultyTiers in the VM and passed into ScoreCalculator as a value — no calculator ↔ level coupling"
    - "Context-aware title extracted to a stateless GameTopBarTitle composable (state hoisting) — presentation-only mode delta"

key-files:
  created: []
  modified:
    - app/src/main/java/com/app/pegsolitaire/navigation/Routes.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
    - app/src/main/res/values/strings.xml
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt

key-decisions:
  - "Unified LevelRef as one Gameplay route with a nullable challengeLevel (Open-Q2 planner's call) — no separate Challenge VM/route; the single arg drives both the load path and the title slot"
  - "VM reads DifficultyTiers.forLevel(n).scoreMultiplier directly (pure :domain object) rather than injecting a new dependency — keeps ScoreCalculator uncoupled from level state"
  - "Clamp challengeLevel < 1 to 1 at the VM boundary (T-03-09) as a second guard, even though the data source already clamps — untrusted nav arg must never crash the load"

requirements-completed: [CHAL-01, CHAL-04]

duration: 4min
completed: 2026-07-01
---

# Phase 3 Plan 04: Challenge Playable Through the Reused Board Summary

**Challenge levels now load and play through the exact Phase-1/2 board via a unified `LevelRef` — one optional `challengeLevel` route arg drives both the `loadChallenge` load path and a context-aware "Level N" gameplay title, with a clamped-untrusted-arg boundary and the D-11 level-scaled score applied at finish — while Classic stays byte-identical and no `if (challenge)` reaches the board/game logic.**

## Performance

- **Duration:** ~4 min
- **Started:** 2026-07-01T19:17:52Z
- **Completed:** 2026-07-01T19:21:54Z
- **Tasks:** 2
- **Files created/modified:** 6 (0 created, 6 modified)

## Accomplishments

- **Unified `LevelRef` (Open-Q2)** — `Gameplay` gained `challengeLevel: Int? = null` (and `levelId` a `""` default) so Classic navigates `Gameplay(levelId = id)` unchanged and Challenge navigates `Gameplay(challengeLevel = n)`; `Results` gained `challengeLevel: Int? = null` for the 03-05 mode-aware actions. Field names stay the binding contract read via `toRoute<...>()`.
- **One `GameViewModel`, route-arg-driven** — exposes `challengeLevel`, computes a `clampedChallengeLevel` (`coerceAtLeast(1)`, T-03-09), and picks `loadChallenge(clamped)` vs `loadClassic(levelId)` on that single arg. No separate ViewModel; the board/selection/jump logic is untouched.
- **D-11 level-scaled finish** — the VM reads `DifficultyTiers.forLevel(clamped).scoreMultiplier` (pure `:domain`, no new injection) and passes it as `levelMultiplier` into `scoreCalculator.evaluate(...)`; Classic passes the `ScoreParams.DEFAULT_LEVEL_MULTIPLIER` (1) so its score is byte-identical.
- **Context-aware "Level N" title (D-03/S2)** — extracted a stateless `GameTopBarTitle`: a non-null `challengeLevel` renders "Level N" (`titleLarge`, neutral `onSurface` per S3 accent discipline) with a `challenge_level_indicator_desc` content description; Classic renders the app-name title. `GameRoute` threads `viewModel.challengeLevel` into both `GameScreen` and the `Results` route.
- **Strings + test** — added `challenge_level_indicator` = "Level %1$d" and `challenge_level_indicator_desc` = "Challenge level %1$d". Extended `GameViewModelTest` (Robolectric/Turbine harness): `Gameplay(challengeLevel = 1)` loads a generated, non-empty, still-Playing board and exposes `challengeLevel`; a forged `challengeLevel = 0` clamps without crashing. The test's `FakeLevelRepository.loadChallenge` now routes through the real pure-domain `LevelGenerator` (as production does).

## Task Commits

Each task committed atomically:

1. **Task 1: Carry Challenge level via unified route + level-scaled load** — `ddc4b22` (feat)
2. **Task 2: Context-aware Level N title + strings + challenge-load test** — `0aae6e9` (feat)

## Files Created/Modified

- `navigation/Routes.kt` — `Gameplay` carries `challengeLevel: Int?` (+ defaulted `levelId`); `Results` carries `challengeLevel: Int?`.
- `ui/game/GameViewModel.kt` — `challengeLevel` exposed; `clampedChallengeLevel` boundary clamp; branch load path; `levelMultiplier` from `DifficultyTiers`; passed into `evaluate`.
- `ui/game/GameRoute.kt` — threads `challengeLevel` into `GameScreen` and the built `Results` route.
- `ui/game/GameScreen.kt` — new stateless `GameTopBarTitle`; `GameScreen`/`GameTopBar` take an optional `challengeLevel` (presentation-only).
- `res/values/strings.xml` — `challenge_level_indicator` + `challenge_level_indicator_desc`.
- `ui/game/GameViewModelTest.kt` — challenge-load + clamp tests; `viewModelFor` accepts `challengeLevel`; fake routes through the real generator.

## Decisions Made

All three plan-anticipated decisions were followed as specified: unified single-route `LevelRef`, VM references the pure `DifficultyTiers` object directly (no new injection), and a VM-boundary clamp as a defense-in-depth second guard.

## Deviations from Plan

None - plan executed exactly as written.

The only note (carried from 03-03): the plan's verify used `:app:test --tests ...`, but the `:app:test` aggregate rejects `--tests`; the equivalent `:app:testDebugUnitTest --tests '*GameViewModelTest*'` was run for the targeted step, and plain `:app:test` for the wave gate. Same tests, correct task names.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- **03-05 unblocked:** `Results.challengeLevel` is populated on every finish, ready for the mode-aware Next-level (solved) / Retry (stuck) actions; the Home Challenge tile can navigate `Gameplay(challengeLevel = ChallengeProgress.currentLevel)` and the reused board plays it. `ChallengeProgress.advanceToNext()` (03-03) drives the Next-level action.
- **Board logic clean:** the mode delta lives only in the route arg → title slot and the VM load branch; `BoardCanvas`/selection/jump remain mode-agnostic.

## Known Stubs

None. Classic's `levelId=""` default is only ever taken when `challengeLevel` is set (the Challenge path never reads `levelId`), so it is not a stub — it is the unused half of the unified ref.

## Threat Flags

None — no new network endpoints, auth paths, file access, or schema changes; the one untrusted surface (`challengeLevel` nav arg) is the planned T-03-09 clamp, mitigated at the VM boundary.

## Self-Check: PASSED

- All 6 modified files verified present on disk.
- Both task commits (`ddc4b22`, `0aae6e9`) verified in git log.
- Full wave gate `./gradlew ktlintCheck detekt :domain:test :app:test assembleDebug` — BUILD SUCCESSFUL.
- Targeted `:app:testDebugUnitTest --tests '*GameViewModelTest*'` — green (challenge-load + clamp).

---
*Phase: 03-challenge-generation-difficulty-curve*
*Completed: 2026-07-01*
