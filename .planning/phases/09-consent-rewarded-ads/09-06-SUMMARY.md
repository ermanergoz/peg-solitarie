---
phase: 09-consent-rewarded-ads
plan: 06
subsystem: gameplay-viewmodel
tags: [ads-06, assist-funnel, ordering, rewarded-ads, ui-state, tdd]

# Dependency graph
requires:
  - phase: 09-consent-rewarded-ads (plan 09-02)
    provides: widened AdGateway seam (rewardedOfferable, preloadRewarded) the funnel is built against
  - phase: 09-consent-rewarded-ads (plan 09-04)
    provides: freezeClockForAd()/unfreezeClockAfterAd() bracket the reordered requestRewarded stays inside
provides:
  - GameUiState.pendingAdOffer / adOfferLoading / adMessage / adMessageTick — the opt-in dialog and snackbar surface
  - adMessageTick carried forward at all four board-install sites; pendingAdOffer/adOfferLoading cleared there
  - GameViewModel.pendingHint — the found move stashed across an ad round trip
  - GameViewModel.revealPendingHint(boardAtRequest) — the paid-for reveal, board re-checked
  - onHintRequested inverted to search-first/charge-second (ADS-06)
  - onUndo empty-history guard hoisted above the gateway
  - GameViewModelAssistOrderingTest — ADS-06 pinned on "the gateway was never consulted"
affects: [09-08, 09-09, gameplay-viewmodel, assist-funnel]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Search first, charge second: the gateway is consulted only once a deliverable assist exists (RESEARCH Pattern 5)"
    - "Stash-then-charge: a found assist waits in a private field so it survives the ad round trip"
    - "Costs-nothing assertions target gateway.lastReason == null, not board equality — a decline also leaves the board unchanged"

key-files:
  created:
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAssistOrderingTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAdClockTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt

key-decisions:
  - "pendingHint is cleared inside cancelHintSearch() rather than at each of the four board-install sites — every one of them already calls it, so this preserves the single-cancellation-idiom property the KDoc depends on"
  - "solverThinking is cleared BEFORE the gateway is consulted, so the Hint control is never left spinning behind an ad; the Declined arm's now-redundant clear was therefore dropped"
  - "The CR-01 stale-board guard moved ahead of the gateway too, so a hint discarded for a superseded board is not charged for either"
  - "revealPendingHint re-checks the board against the request-time board even though the offer is modal — the ad's return is SDK-timed and CR-01 discipline is not relaxed for a dialog"
  - "Two ADSF-01 clock specs now inject a hint use case that returns a move: they drove the ad bracket via a hint on a board with no one-peg line, which the reorder correctly stops charging for"

requirements-completed: [ADS-06]

# Metrics
duration: ~50 min
completed: 2026-08-31
---

# Phase 9 Plan 06: Assist Ordering Inversion Summary

**Nothing is charged for an assist the player never receives: `onHintRequested` now runs the solver before it consults the gateway, so a "no winning move" answer costs neither an allowance unit nor a thirty-second ad, and `onUndo`'s empty-history guard sits above the gateway for the same reason (ADS-06) — with the four `GameUiState` fields the opt-in dialog and snackbar will render off.**

## What shipped

**Task 1 — `GameUiState` funnel surface (`feecc4f`).** Four fields with `@property` KDoc: `pendingAdOffer: AdReason?` (non-null means the dialog is up, and it carries the reason so the dialog picks hint-vs-undo copy — the `confirmRestart` nullable-flag precedent), `adOfferLoading` (the D-10 in-dialog bounded wait), `adMessage: Int?` (`@StringRes`, D-11 snackbar) and `adMessageTick` (the monotonic one-shot ordinal, `> 0` consumer guard documented as `undoTick` documents its own). All four are enumerated explicitly in `initial()` and `loading()`. `adMessageTick` is **carried forward** at all four board-install sites — `resetToInitial`, `loadChallengeBoardIfNeeded`, `installClassicBoard`, `applyRestored` — because a fresh `0` is a *changed* key for an effect sitting higher, which fires a snackbar nobody triggered. `pendingAdOffer`/`adOfferLoading` are the deliberate opposite: the `GameUiState.initial()` rebuild clears them, so a dialog cannot outlive its board. No allowance count entered the UiState (D-05), and a comment at the new fields says so, so a later reader does not "complete" the state with a counter.

**Task 2 — the inversion, TDD (`66d91aa` RED → `45021a3` GREEN).** `onHintRequested` is now: `isLoading` guard → `cancelHintSearch(); solverThinking = true` → main-thread CR-01 capture (verbatim) → `withContext(defaultDispatcher) { getHintUseCase(...) }` → stale-board guard → `move == null` sets `noWinningMove` and **returns without touching the gateway** → `move != null` stashes `pendingHint`, clears `solverThinking`, and only then runs the freeze / `requestRewarded(HINT)` / unfreeze block, revealing the stashed move on `Granted` and `Unavailable`. `onUndo`'s `if (moveHistory.isEmpty()) return@launch` moved above the same bracket. Both `requestRewarded` calls remain inside plan 09-04's `try/finally`, as that plan's hand-off note requires.

`GameViewModelAssistOrderingTest` (7 specs) pins it. Every "costs nothing" assertion is on `lastReason == null` — *the gateway was never consulted* — which is a strictly stronger claim than "the board did not change", since a declined grant also leaves the board untouched.

## Verification

- `./gradlew check --offline` — **BUILD SUCCESSFUL** (all JVM tests, detekt, ktlint, `dpSpLiteralCheck`, `adIdLiteralCheck`, `:domain` suite).
- Task-2 gate — `GameViewModelAssistOrderingTest` + `GameViewModelHintStalenessTest` + `GameViewModelTest` + `GameViewModelAdClockTest`: 67 tests, all green.
- Ordering proven by line number in `GameViewModel.kt`: `moveHistory.isEmpty()` at 945 precedes `requestRewarded(UNDO)` at 954; `withContext(defaultDispatcher)` at 1082 precedes `requestRewarded(HINT)` at 1121; `pendingHint = move` at 1111 on the non-null branch.
- RED was genuine: exactly the four ADS-06 specs failed before the implementation. The three control specs (a found hint is charged for, an undo with history is charged for, a decline reveals nothing) passed at RED by design — they are regression guards for behaviour the reorder had to preserve, not new claims.
- UI-SPEC negative contract holds: `git diff --name-only` shows no change to `HintControl.kt`, `UndoControl.kt`, `HintToggleControl.kt` or `NoWinBanner.kt`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Two ADSF-01 clock specs were invalidated by the mandated reorder**

- **Found during:** Task 2 GREEN
- **Issue:** `GameViewModelAdClockTest.freezingAClockThatNeverStartedIsInertAndTheTimerStillStartsOnTheFirstJump` and `anAdThatReturnsAfterTheGameIsOverDoesNotResurrectTheClock` reach the ad bracket by calling `onHintRequested()` on `twoMoveStuckLine()` — a 1x5 strip whose minimum is provably two pegs, so it has no one-peg line and the real Solver returns `null`. Under the new ordering the gateway is correctly never consulted there, so `gateway.started` stayed 0 and both specs failed. The plan did not anticipate this (its acceptance criteria expected `GameViewModelAdClockTest` to pass untouched).
- **Fix:** added an optional `getHintUseCase` parameter to that file's `viewModelFor` and an `alwaysFindsAMove()` mockk returning `RulesEngine.legalMoves(board).firstOrNull()`, passed by those two specs only. The spec's subject is the clock across an ad presentation, not the Solver, so the stub supplies the "a move was found" precondition. **No clock assertion changed**; the other three ADSF-01 specs drive the bracket through `onUndo()` after a real move and were untouched.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAdClockTest.kt`
- **Commit:** `45021a3`

**2. [Rule 1 - Stale documentation] A `GameViewModelTest` comment asserted the old ordering**

- **Found during:** Task 2 review
- **Issue:** `hintDeclinedLeavesTheControlIdleOnABoardThatHasAWinningMove` explained itself with "a missing hint can only mean the search never ran — proving the abort happened before the Solver call". That was true only under charge-first; after the reorder the search *does* run and the found move waits in `pendingHint`. The assertions still pass, but the stated reasoning had become false and would mislead the next reader into thinking the decline short-circuits the Solver.
- **Fix:** rewrote the three comment lines to state what the test now proves (the decline suppressed the reveal), citing plan 09-06. Every assertion left byte-identical.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt`
- **Commit:** `5ef7940`

### Instructed consequences worth flagging

- **The `Declined` arm of `onHintRequested` lost its `_uiState.update { it.copy(solverThinking = false) }`.** The plan directed clearing `solverThinking` *before* the freeze/`requestRewarded` block, which makes that write redundant. Keeping a dead idempotent write would imply the flag might still be true. The arm is now a bare `return@launch` with a comment saying why. This is the only edit inside any `Granted`/`Unavailable`/`Declined` body beyond revealing the stashed hint.
- **Acceptance-criterion conflict (benign).** The plan asks that `grep -inE "…|allowance" GameUiState.kt` return nothing, while its own action text requires a comment at the new fields stating that no allowance count belongs there. The grep now matches three *comment* lines and no field. The substantive criterion — no allowance-count field in the UiState — holds; the comment the plan mandated is what the grep sees.

## Known Stubs

None in the behavioural sense. The four new `GameUiState` fields are **declared contracts with no consumer yet**, which the plan states explicitly (`<interfaces>`: "Plans 09-08 and 09-09 write to and render these"):

| Field | Written by | Rendered by |
|-------|-----------|-------------|
| `pendingAdOffer` | 09-08 (funnel) | 09-09 (`AdOfferDialog`) |
| `adOfferLoading` | 09-08 | 09-09 |
| `adMessage` / `adMessageTick` | 09-08 | 09-09 (snackbar) |

`pendingHint` is written and read within this plan (stash → `revealPendingHint`); 09-08 takes over the reveal when it moves it to the dialog's accept path. No UI renders an empty or placeholder value as a result of this plan — the controls are byte-identical to v1.0, which is the D-05 requirement.

## Threat Flags

None. This plan adds no network endpoint, auth path, file access or schema change. The three threats it was scoped to mitigate — T-09-23 (hint ordering), T-09-24 (undo guard ordering), T-09-25 (`pendingHint` staleness) — are each pinned by a test, and T-09-26 (`adMessageTick` carry-forward) by the four-site grep.

## Self-Check: PASSED

- `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAssistOrderingTest.kt` — FOUND
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt` — FOUND
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` — FOUND
- Commits `feecc4f`, `66d91aa`, `45021a3`, `5ef7940` — all FOUND in `git log`

## TDD Gate Compliance

Task 2 followed RED → GREEN with gate commits in order: `test(09-06)` (`66d91aa`, four specs failing) then `feat(09-06)` (`45021a3`, all green). No REFACTOR commit — the implementation needed no cleanup pass beyond extracting `revealPendingHint`, which landed as part of GREEN.

## Note for downstream plans

- **09-08 (allowance funnel):** the charge point is now unambiguous — it is the line that calls `adGateway.requestRewarded`, and everything above it is free. Spend the allowance unit there, not at the top of the handler, or ADS-06 regresses silently. `pendingHint` already holds the move; move the `revealPendingHint(boardForHint)` call to the accept path and keep its board re-check.
- **09-09 (dialog + snackbar):** `adMessageTick` must be consumed with a `> 0` guard, as `undoTick` is. The carry-forward half of that fix is already in place at all four install sites.
- **Do not add an allowance count to `GameUiState`** (D-05). The comment at the new fields says so; the counters stay private to the ViewModel.
