---
phase: 03-challenge-generation-difficulty-curve
plan: 05
subsystem: ui
tags: [compose, navigation-compose, mvvm, challenge-mode, results, home]

# Dependency graph
requires:
  - phase: 03-04
    provides: Unified LevelRef — one Gameplay route with nullable challengeLevel; Results(..., challengeLevel) route arg
  - phase: 03-03
    provides: ChallengeProgress in-memory StateFlow<Int> (starts at 1); LevelRepository.loadChallenge; level-scaled score
  - phase: 02-08
    provides: AppNavHost 5-screen spine with default-valued @Composable slots; NavFlowTest reachability pattern
provides:
  - Mode-aware Results trio — Challenge solved shows one filled primary "Next level" (→ N+1); Challenge stuck shows one filled primary "Retry" (same deterministic board); Classic Results unchanged
  - Enabled Home Challenge tile (neutral peer of Classic) entering Challenge at level 1
  - AppNavHost ladder board-hops with popUpTo(Home) so the infinite ladder never grows the back stack
  - New strings: action_next_level, action_retry, challenge_load_error, challenge_tile_supporting
affects: [phase-04, phase-05, challenge-progress-persistence, difficulty-tuning]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Mode selection kept in the nav host (lambda choice), not via if(challenge) inside screens"
    - "Infinite-ladder back-stack bounded via popUpTo(Home) on every Challenge board-hop"
    - "One filled primary Button per Results state (M3 accent discipline)"

key-files:
  created:
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsScreenTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsRoute.kt
    - app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeRoute.kt
    - app/src/main/res/values/strings.xml
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt

key-decisions:
  - "showNextLevel = challengeLevel != null && pegsRemaining == 1 (solved ⇔ one peg by construction); stuck (>=2 pegs) yields Retry only"
  - "Challenge board-hops use popUpTo(Home) mirroring the toBoardFromCatalog precedent so the endless ladder does not grow the back stack (T-03-11)"
  - "Home Challenge tile enabled as a neutral surfaceContainerHigh peer of Classic (NOT accent) per UI-SPEC S1; enters Challenge at level 1 (D-04)"

patterns-established:
  - "Pattern: Nav host owns mode routing — Results invokes onNextLevel/onRetry lambdas; the graph selects the destination, screens stay mode-agnostic"
  - "Pattern: Bounded infinite ladder — every Challenge navigate uses popUpTo(Home)"

requirements-completed: [CHAL-01]

# Metrics
duration: 6min
completed: 2026-07-01
---

# Phase 3 Plan 5: Playable Challenge Slice (Mode-Aware Results + Home Tile) Summary

**Mode-aware Results (Next level → N+1 when solved, Retry same deterministic board when stuck), an enabled Home Challenge tile entering at level 1, and popUpTo(Home) ladder-hops that keep the infinite Challenge back stack bounded**

## Performance

- **Duration:** ~6 min (continuation close-out; implementation committed 2026-07-01 22:30–22:34)
- **Started:** 2026-07-01T22:30:05+03:00 (Task 1 commit)
- **Completed:** 2026-07-01T19:30:00Z (plan close-out)
- **Tasks:** 3 (2 auto + 1 human-verify checkpoint)
- **Files modified:** 11 (1 created, 10 modified)

## Accomplishments
- Results is Challenge-aware with strict one-filled-primary accent discipline: solved → "Next level" (→ N+1), stuck → "Retry" (identical deterministic board), Classic trio unchanged.
- Home Challenge tile is a live neutral peer of Classic ("Endless levels"), entering Challenge at level 1 — the "Coming soon" badge is gone.
- AppNavHost hops the ladder with popUpTo(Home) so the endless Challenge stream never grows the back stack (T-03-11 mitigation).
- On-device end-to-end playability checkpoint (Task 3) was verified and APPROVED by the user: Home tile → Level 1 → solve → Next level → Level 2; stuck → Retry (same board); determinism confirmed on re-entry.

## Task Commits

Each task was committed atomically:

1. **Task 1: Mode-aware Results (Next level / Retry) + NavHost ladder hops (popUpTo Home)** - `3890629` (feat)
2. **Task 2: Enable Home Challenge tile entering Challenge at level 1** - `cf31a2d` (feat)
3. **Task 3: On-device playability checkpoint** - human-verify (no code) — **APPROVED by user**

**Plan metadata:** this commit (docs: complete plan)

## Files Created/Modified
- `app/.../ui/results/ResultsUiState.kt` - Added challengeLevel + derived showNextLevel
- `app/.../ui/results/ResultsViewModel.kt` - Reads Results.challengeLevel; computes showNextLevel
- `app/.../ui/results/ResultsScreen.kt` - Branch: Classic / Challenge-solved (Next level) / Challenge-stuck (Retry), one filled primary each
- `app/.../ui/results/ResultsRoute.kt` - Threads onNextLevel/onRetry lambdas
- `app/.../navigation/AppNavHost.kt` - Wires Next level → Gameplay(challengeLevel=n+1) popUpTo(Home); Retry → same level; homeContent onPlayChallenge → level 1
- `app/.../ui/home/HomeScreen.kt` - ChallengeTile rewritten as enabled neutral peer, ComingSoonBadge removed
- `app/.../ui/home/HomeRoute.kt` - Threads onPlayChallenge
- `app/src/main/res/values/strings.xml` - action_next_level, action_retry, challenge_load_error, challenge_tile_supporting
- `app/.../ui/results/ResultsScreenTest.kt` - New: asserts one-primary discipline per Results state
- `app/.../ui/results/ResultsViewModelTest.kt` - showNextLevel truth table (solved/stuck/Classic)
- `app/.../navigation/NavFlowTest.kt` - Home→Challenge reachability + stuck→Retry (not Next level)

## Decisions Made
- `showNextLevel == (challengeLevel != null && pegsRemaining == 1)` — solved is one-peg by construction; stuck (>=2 pegs) surfaces Retry only.
- Challenge board-hops use `popUpTo(Home)` (mirroring toBoardFromCatalog) so the infinite ladder stays bounded.
- Home Challenge tile is enabled but stays neutral (surfaceContainerHigh, not accent) per UI-SPEC S1.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None.

## User Setup Required
None - no external service configuration required.

## Checkpoint Verification
Task 3 was a `checkpoint:human-verify` (gate="blocking") on-device playability check. The user APPROVED it: the full Challenge loop (Home tile → Level 1 → solve → Next level → Level 2; stuck → Retry same deterministic board; re-entry determinism) was confirmed playable end-to-end. Difficulty-feel knob tuning (D-05/D-07) was explicitly out of scope for this checkpoint and remains a deferred playtest loop.

## Next Phase Readiness
- The playable Challenge slice is complete end-to-end; phase 03 implementation is done (plan 5 of 5).
- Carried-forward concerns (unchanged): Phase 3 difficulty-curve knob values still require a dedicated playtest-iteration loop before the generatorVersion contract is frozen; Diamond-41 D-19 proof still deferred.
- ChallengeProgress remains in-memory (D-04); Phase 5 persistence seam is already shaped.

---
*Phase: 03-challenge-generation-difficulty-curve*
*Completed: 2026-07-01*
