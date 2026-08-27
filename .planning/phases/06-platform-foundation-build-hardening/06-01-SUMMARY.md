---
phase: 06-platform-foundation-build-hardening
plan: 01
subsystem: platform/ads
tags: [PLAT-02, ADS-03, D-01, D-02, D-03, seam, sealed-type]
requires: []
provides:
  - "RewardOutcome — the three-way rewarded-grant result crossing the platform seam"
  - "AdGateway.requestRewarded(reason): RewardOutcome — the seam contract Phase 9's real gateway implements"
  - "The ADS-03 fail-open call-site mapping at both GameViewModel assist entry points"
affects:
  - "Phase 9 (real rewarded ads) — implements this seam and is now forced by the compiler to map Unavailable"
  - "Phase 10 (interstitials) — inherits the corrected AdReason/showInterstitial split"
tech-stack:
  added: []
  patterns:
    - "First sealed interface in the repo; member-by-member KDoc register copied from the GameStatus enum"
    - "Exhaustive when over a sealed outcome instead of a negated Boolean guard"
key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/platform/ads/RewardOutcome.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/platform/ads/AdGateway.kt
    - app/src/main/java/com/app/pegsolitaire/platform/ads/AdReason.kt
    - app/src/main/java/com/app/pegsolitaire/platform/ads/NoOpAdGateway.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
    - app/src/test/java/com/app/pegsolitaire/platform/ads/NoOpAdGatewayTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelChallengeAdvanceTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelResumeTest.kt
decisions:
  - "D-01 honoured: requestRewarded returns a sealed RewardOutcome, not a Boolean"
  - "D-02 honoured: NoOpAdGateway grants unconditionally — no configurable stub"
  - "D-03 honoured: no AdReason.RETRY; the stale reservation KDoc was struck"
  - "Unavailable proceeds at BOTH call sites (ADS-03); only Declined aborts"
metrics:
  duration: "~50 min"
  completed: 2026-08-27
  tasks: 3
  commits: 4
  tests: "303 JVM tests green (299 baseline + 4 new), 0 failures"
---

# Phase 6 Plan 01: AdGateway RewardOutcome Seam Summary

Replaced the `AdGateway.requestRewarded` `Boolean` with a sealed `RewardOutcome` (`Granted`/`Declined`/`Unavailable`) and made both `GameViewModel` assist call sites branch on it exhaustively, so a grant that could not be paid for still grants the assist (ADS-03) — with zero player-visible change, proven by three untouched Robolectric UI tests.

## What Was Built

**`RewardOutcome`** (new, `platform/ads/`) — the repo's first `sealed interface`. Three `data object` members. The type KDoc records D-01's reasoning: a `Boolean` cannot distinguish "the player said no" from "we had nothing to show them", yet ADS-03 requires those two to behave in *opposite* ways. `Unavailable`'s KDoc states the caller obligation in the imperative — callers MUST proceed with the assist anyway. The member-by-member KDoc register and the closing "adjacent concern lives next door" sentence were copied from `GameStatus`, the nearest analog.

**The seam** — `requestRewarded` now returns `RewardOutcome`. Two stale doc claims were corrected:
- The interface KDoc previously promised "Slice 2 swaps only the Hilt binding … no gameplay or UI change". That is false: every real SDK call needs a foreground Activity (which only platform-layer code may hold) and Phase 9 puts an opt-in dialog in front of any ad that plays. The KDoc now says the binding swap is necessary but not sufficient, and names both reasons.
- The method KDoc's "treat a `false` return as 'not granted' and skip" became the asymmetric three-way contract: `Granted` and `Unavailable` proceed, only `Declined` skips.

**`AdReason`** — the KDoc reserving a future `RETRY` member for interstitials was struck per D-03. Retry is free permanently, so no `RETRY` member will ever exist, and the sentence pointing readers at `showInterstitial` for retry was wrong about the mechanism. No enum member was added or removed.

**`NoOpAdGateway`** — returns `RewardOutcome.Granted` unconditionally. The KDoc now states *why* it is not configurable (D-02): a stub that could decline would change debug behavior inside a phase whose entire contract is that nothing a player can see changes. The `@Inject constructor` 8-space body indent was preserved untouched.

**Both call sites** — `onUndo` and `onHintRequested` now use `when (adGateway.requestRewarded(...))` with `RewardOutcome.Granted, RewardOutcome.Unavailable -> Unit` and `RewardOutcome.Declined -> return@launch`. The hint decline branch still clears `solverThinking` before returning (T-06-01 — dropping it would leave the hint control spinning forever). The `onUndo` proceed branch carries a comment recording the ADS-03 rule and the concrete regression it prevents: mapping `Unavailable` to "skip" would silently break offline hint/undo the moment Phase 9 lands a real gateway, and would not surface in Phase 6 because the stub never returns it.

**Test doubles and new coverage** — `FakeAdGateway(grant: Boolean)` became `FakeAdGateway(outcome: RewardOutcome = RewardOutcome.Granted)`; `GatedAdGateway`, `GrantingAdGateway` and `GrantingAds` return `Granted`. The two pre-existing `grant = false` call sites became `outcome = RewardOutcome.Declined`, so the old decline coverage was preserved rather than lost. Four new tests were added.

## Key Decisions

**The `Unavailable` branch had no prior art anywhere in the codebase.** Because the stub has always granted, *every* non-grant branch in the repo was dead code that had never once executed. The four new tests are the first execution of those paths in the project's history, which is why they were worth more than the mechanical conversion around them.

**RED was proven by inverting the mapping, not by trusting a green run.** New tests that pass on first run prove nothing about whether they bite. `Unavailable -> return@launch` was temporarily substituted at both call sites and the test task failed; the inversion was then reverted with a file-scoped `git checkout --`. Worth recording for the next author: with the wrong mapping the undo test does not fail cleanly, it *hangs* — a failing assertion skips the test's `cancelTickers()` call, the per-second ticker loops forever and `runTest` never goes idle. The hang is the failure signal.

**`GetHintUseCase` is constructed inline in the test factory, not injected.** So "the `Declined` hint never runs the solver" could not be asserted with a call-counting double. It is asserted indirectly instead: the board used (`wonInOneJumpBoard`) demonstrably has a winning move, so the absence of a hint can only mean the search never ran.

**The two new `Declined` tests assert `lastReason` as well as the aborted state.** The pre-existing decline tests only checked the outcome state. Asserting `lastReason` pins that the abort happens *downstream* of the seam — the gateway is genuinely asked and only then is the assist skipped — which a state-only assertion would not catch if a future refactor short-circuited before the request.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Missing `local.properties` in the fresh worktree**
- **Found during:** Task 1, first `./gradlew` invocation
- **Issue:** Gradle failed with "SDK location not found" — `local.properties` is gitignored, so a fresh worktree has none and no `ANDROID_HOME` was set in the environment.
- **Fix:** Wrote `local.properties` with `sdk.dir=/Users/ermanergoz/Library/Android/sdk` (the SDK's actual location on this machine).
- **Files modified:** `local.properties` (gitignored — deliberately **not** committed)
- **Commit:** none (untracked by design)

### Task-sequencing deviation (no functional impact)

The plan gives Task 1 the automated verification `:app:testDebugUnitTest --tests "*NoOpAdGatewayTest"`, but a return-type change to an interface ripples across the whole compile unit: the app module cannot compile until Task 2's call sites *and* Task 3's test doubles also speak `RewardOutcome`. Task 1's verification was therefore run after those edits were in the working tree, and commits `26aee9e` and `f4fa981` are intermediate states in which the module does not compile on its own. Per-task commit boundaries were kept as the plan requires; the alternative (one giant commit) would have lost the task-level history. Every plan-listed acceptance criterion was still checked at its own task boundary, and the final `./gradlew check` is green.

### RED-gate note for Task 1

The RED commit (`d50cd67`) includes `RewardOutcome.kt` alongside the failing test, because the test cannot reference the type otherwise and a commit with an unresolved reference would be worse than one that compiles and fails. The test failed as a genuine assertion failure (`ComparisonFailureWithFacts` — the stub returned `true`, not `RewardOutcome.Granted`), not as a compile error.

## Verification

| Check | Result |
|-------|--------|
| `grep -c "data object" RewardOutcome.kt` | 3 |
| `grep "sealed interface RewardOutcome"` | present |
| `grep "requestRewarded(reason: AdReason): RewardOutcome" AdGateway.kt` | present |
| `grep "Boolean" NoOpAdGateway.kt` | 0 matches |
| `RETRY` outside comments in `AdReason.kt` | 0 matches |
| `grep -c "RewardOutcome.Granted, RewardOutcome.Unavailable" GameViewModel.kt` | 2 |
| `grep -c "!adGateway.requestRewarded" GameViewModel.kt` | 0 |
| `requestRewarded(...): Boolean` anywhere in `app/src` | 0 matches |
| `grep -c "grant = false" GameViewModelTest.kt` | 0 |
| `RewardOutcome.Unavailable` / `.Declined` in `GameViewModelTest.kt` | 3 / 5 |
| `git diff --stat 5ab69ad -- HintControlTest UndoControlTest GameFlowTest` | **zero changed lines** |
| `./gradlew check` (detekt, ktlint, dp/sp gate, ArchUnit, all tests) | BUILD SUCCESSFUL |
| JVM test total (`:domain:test` + `:app:testDebugUnitTest`) | **303 tests, 0 failures, 0 errors, 0 skipped** (299 baseline + 4 new) |
| Files deleted by this plan's commits | none |

## Known Stubs

`NoOpAdGateway` remains a stub returning `RewardOutcome.Granted` unconditionally — this is **intentional and locked by D-02**, not incidental debt. It is the mechanism by which this phase delivers zero player-visible change. Phase 9 replaces it with the real rewarded-ad implementation; `RewardOutcome.Declined` and `RewardOutcome.Unavailable` are unreachable in the shipped build until then, which is exactly why the four new tests drive them through a fake.

## For Future Phases

- **Phase 9 authors:** the compiler now forces you to handle every `RewardOutcome` member at both call sites. Do not "simplify" `Granted, Unavailable -> Unit` into an equality check against `Declined` — the exhaustive form is the whole point of D-01, and it is what will flag any member you add.
- **`AssistAllowance` (D-04) still belongs in `:domain`,** not in the gateway and not in `GameViewModel`. Nothing in this plan created a home for it.
- The `AdGateway` KDoc now states that a real implementation needs a foreground Activity. The `ForegroundActivityProvider` that supplies one is a later plan in this phase.

## Self-Check: PASSED

- `app/src/main/java/com/app/pegsolitaire/platform/ads/RewardOutcome.kt` — FOUND
- Commit `d50cd67` — FOUND
- Commit `26aee9e` — FOUND
- Commit `f4fa981` — FOUND
- Commit `4a873c5` — FOUND
- `HintControlTest.kt` / `UndoControlTest.kt` / `GameFlowTest.kt` unmodified vs `5ab69ad` — CONFIRMED
