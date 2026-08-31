---
phase: 09-consent-rewarded-ads
plan: 08
subsystem: gameplay-viewmodel
tags: [assist-allowance, rewarded-ads, opt-in-dialog, persistence, ads-economy]

# Dependency graph
requires:
  - phase: 09-consent-rewarded-ads (plan 09-01)
    provides: AssistAllowance / AssistAllowanceLimits / AssistKind, RestoredGame.allowance, toDto's two counter parameters
  - phase: 09-consent-rewarded-ads (plan 09-02)
    provides: AdGateway.rewardedOfferable + preloadRewarded, the offerable NoOp stub, AdConstants
  - phase: 09-consent-rewarded-ads (plan 09-04)
    provides: freezeClockForAd()/unfreezeClockAfterAd(), the try/finally ad bracket
  - phase: 09-consent-rewarded-ads (plan 09-06)
    provides: GameUiState.pendingAdOffer/adOfferLoading/adMessage/adMessageTick, pendingHint, the search-first ordering
provides:
  - GameViewModel.enterAssistFunnel — the single gate both assists pass through (free / silently free / offered)
  - GameViewModel.onAcceptAdOffer + onDismissAdOffer — the public surface plan 09-09 wires to the screen
  - GameViewModel.performUndo — the extracted undo body, the one place the move history is shortened
  - GameViewModel.emitAdMessage — the D-11 snackbar tick
  - Allowance persistence: toDto now carries both counters, applyRestored takes restored.allowance
  - GameViewModelAllowanceTest — 13 specs covering ADS-01..ADS-05, CONS-04, D-01/D-03/D-04/D-09/D-12
affects: [09-09, 09-10, gameplay-viewmodel, assist-funnel, resume]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Single assist funnel: one private gate answers free / silently-free / offered for every assist"
    - "Stash-with-its-board: pendingHint travels with pendingHintBoard so staleness survives an ad round trip"
    - "Private economy, observable proof: the counters never enter UiState, and tests assert consequences instead"

key-files:
  created:
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAllowanceTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAdClockTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAssistOrderingTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelSoundTest.kt

key-decisions:
  - "Tasks 1 and 2 landed in one commit: Task 1's own acceptance criterion counts onAcceptAdOffer as the sole requestRewarded call site, so a funnel without its callbacks raises a dialog nothing can answer"
  - "pendingHintBoard added beside pendingHint — the reveal now has two callers an entire ad apart, and a board captured in one is not in scope in the other"
  - "performUndo repeats the empty-history guard: the accept path returns from an SDK-timed callback, and a restart taken during the ad clears the history underneath it"
  - "No ViewModel-level timeout around requestRewarded — ACCEPT_WAIT_MS belongs to the gateway (09-10 step 2), and wrapping the whole call would abort an ad the player is legitimately watching"
  - "LargeClass added to the existing class-level @Suppress with a documented justification; the alternative is a second state owner for the allowance, which is how it would drift from the board and the clock"

patterns-established:
  - "Charge only for a delivered assist, and only after an explicit opt-in: requestRewarded has exactly one call site and it sits behind a dialog tap"
  - "Refill-then-spend as the ordering that makes reward copy literally true"
  - "Test the private economy through its observable consequences rather than adding a test-only accessor"

requirements-completed: [ADS-01, ADS-02, ADS-03, ADS-04, ADS-05, CONS-04]

# Metrics
duration: 105 min
completed: 2026-08-31
---

# Phase 9 Plan 08: The Assist Funnel Summary

**The auto-grant seam is now a real economy: three free hints and five free undos per level spent independently, an explicit opt-in dialog once a counter empties, a fail-open free grant when no ad can be served, total silence when ads are knowably unservable, and counters that survive a retry and process death — all built and proven against the `NoOpAdGateway` stub before any ad SDK exists.**

## Performance

- **Duration:** ~105 min
- **Completed:** 2026-08-31
- **Tasks:** 3 (3 commits)
- **Files:** 6 (1 created, 5 modified)

## Accomplishments

- **One funnel, two entry points.** `enterAssistFunnel(kind, reason)` answers free (ADS-02) / silently free (CONS-04, D-12) / offered (ADS-01) in that fixed order, and both `onUndo` and `onHintRequested` call it at the exact position plan 09-06 left the gateway call. Neither touches `adGateway.requestRewarded` any more: the file has **exactly one** call site, inside `onAcceptAdOffer`, reachable only from a tap on a dialog that already named the reward (T-09-32).
- **D-09 preloading is genuinely stingy.** `preloadRewarded` fires only on the spend that takes a counter to zero — asserted both ways, so the "no ad requests while allowance remains" half is pinned as hard as the "warm one at exhaustion" half. Most levels will issue no ad request at all.
- **`rewardedOfferable` is read only after the allowance check**, so a player with free assists left has behaviour that does not depend on consent state at all.
- **Accept freezes the clock at accept, never at offer** (ADSF-01, Pitfall 9), keeping plan 09-04's `try/finally` bracket intact — and the ad-clock spec now asserts that raising the offer starts *no* request, which makes the dialog-parking anti-exploit an assertion rather than a comment.
- **Refill then spend** on `Granted` and `Unavailable`, in that order, so the dialog's "watch a short ad for 3 more?" is literally true; `Unavailable` additionally lands the assist and *then* explains itself via the D-11 snackbar. `Declined` touches neither counter (ADS-05/T-09-34).
- **Declining is inert.** `onDismissAdOffer` mentions `allowance` nowhere; the spec asserts board, history, both counters, snackbar and loading flag all unchanged.
- **ADS-04 needed no banner code.** `NoWinBanner` routes into `onUndo`, so the rescue undo spends a normal undo while any remain and raises the same dialog at zero — `git diff` shows the banner untouched.
- **Persistence closes 09-01's loose end.** `onLeaveScreen`'s `toDto` now passes both counters, `applyRestored` takes `restored.allowance`, and `resetToInitial` deliberately does not reset it — with a comment naming D-04, explaining why the field must live in the ViewModel instance (`clearSnapshot()` runs one line below), and documenting the one accepted D-17 hole.

## Task Commits

1. **Tasks 1 + 2: the funnel, the callbacks and persistence** — `d5900c7`
2. **The pre-existing specs, adapted to the new economy** — `60e5349`
3. **Task 3: `GameViewModelAllowanceTest`** — `8685891`

## Decisions Made

- **Tasks 1 and 2 are one commit.** Task 1's own acceptance criterion (`grep -c adGateway.requestRewarded` is 1, "the only remaining call site is `onAcceptAdOffer` (added in Task 2)") is unsatisfiable without Task 2, and its `<verify>` gate runs test specs that must call `onAcceptAdOffer`. A Task-1-only commit would ship a dialog with no way to answer it. Recorded as a deviation below rather than faked with an artificial split.
- **`pendingHintBoard` beside `pendingHint`.** `revealPendingHint` lost its board parameter and reads the stashed board instead. The reveal now has two callers — the inline free path and the accept path, an entire ad presentation apart — and the request-time board simply is not in scope in the second. Both fields are cleared together by `clearPendingHint()`, which `cancelHintSearch()` calls, so the "one cancellation idiom" property plan 09-06 established is preserved.
- **`performUndo` repeats the empty-history guard.** The accept path arrives from an SDK-timed callback; a restart taken while the ad was up clears `moveHistory` underneath it, and `removeAt(lastIndex)` on an empty list is a crash on the return from an ad. This is a Rule 2 correctness addition, commented as such.
- **No ViewModel-level `withTimeout` around `requestRewarded`.** Plan 09-03's hand-off note asked the caller to enforce `AdConstants.ACCEPT_WAIT_MS`, but plan 09-10 step 2 places that bound inside `AdMobAdGateway`, around the ad **load** only, and explicitly warns "do not bury a second timeout here, or the D-10 bound becomes two bounds that can disagree". Wrapping the whole call from the ViewModel would also span the ad the player is legitimately watching and would abort it at five seconds, granting free while the video played on. The dialog's D-10 lock is therefore bounded by the gateway, which is the only layer that can tell "still loading" from "on screen". **This is the resolution of 09-03's open note; 09-10 must not drop that `withTimeoutOrNull`.**
- **`LargeClass` suppression.** Added to the existing class-level `@Suppress` with a "Size suppressions" section in the class KDoc explaining it. Splitting the allowance and the offer into a second state owner would separate the counters from the board they are spent on and the clock the ad freezes — three things whose whole correctness is that they move together.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Twelve pre-existing specs were invalidated by the mandated economy**

- **Found during:** Task 1/2 verification (the plan's own gate, `--tests '*GameViewModel*'`)
- **Issue:** The plan's acceptance criteria predicted the existing suites would pass unchanged ("the NoOp stub grants and is offerable, so existing behaviour is preserved"). That is true for the board-level assertions but false for every spec that asserts on the *gateway*: with a free allowance in front of it, the first `FREE_HINTS` hints and `FREE_UNDOS` undos never consult it, so `lastReason` is `null` and `started` is `0`. Twelve specs across four files failed — and `GameViewModelSoundTest.aDeclinedUndoGrantDoesNotTick` did not merely fail, it **hung**: the undo now proceeded, the board stayed `Playing`, the assertion failed before its `cancelTickers()`, and `runTest`'s end-of-test drain span on virtual time forever. This is the same class of breakage plan 09-06 documented for `GameViewModelAdClockTest`.
- **Fix:** each affected spec now exhausts the relevant counter (jump-and-undo cycles for undos, repeated requests for hints — every count expressed as `AssistAllowanceLimits`), asserts the offer is raised **without** an ad request, and only then accepts. No assertion was weakened. Several were strengthened: the two ADS-03 specs now also assert the D-11 snackbar (previously unreachable), every "costs nothing" ordering spec now also asserts `pendingAdOffer == null` so a funnel that wrongly raised a dialog can no longer pass, and the hung spec gained a `try/finally` around `cancelTickers()` so a future regression reports instead of hanging.
- **Files modified:** `GameViewModelTest.kt`, `GameViewModelAdClockTest.kt`, `GameViewModelAssistOrderingTest.kt`, `GameViewModelSoundTest.kt`
- **Commit:** `60e5349`

**2. [Rule 3 - Blocking] `aSecondHintRequestCancelsThePriorSearchSoOnlyOneProceeds` lost its park**

- **Found during:** the same gate
- **Issue:** that D-10 spec used a gated `AdGateway` as the place a hint search suspends, which worked only because `requestRewarded` sat inside `hintJob`. This plan moves it out of `hintJob` entirely and behind an accepted offer, so gating the gateway now holds nothing — and once the allowance is exhausted the second request raises a dialog rather than reaching the gateway at all. The spec was unsalvageable in its original shape.
- **Fix:** it now parks at the **background dispatcher**, where the search genuinely suspends, and counts searches via a recording `GetHintUseCase` — the exact idiom `GameViewModelHintStalenessTest` and `GameViewModelAssistOrderingTest` already use. The claim is unchanged ("exactly one search proceeded; the prior one was cancelled"). `GatedAdGateway` became unused and was replaced by `ParkingDispatcher`; `viewModelFor` gained `defaultDispatcher` and `getHintUseCase` parameters.
- **Files modified:** `GameViewModelTest.kt`
- **Commit:** `60e5349`

**3. [Rule 2 - Missing critical functionality] Empty-history guard in `performUndo`**

- **Found during:** Task 2
- **Issue:** the plan factors the undo body out for both callers but does not guard it. The accept path reaches it after an ad, and `resetToInitial()` (restart) clears `moveHistory` — a restart taken while the ad was up would land `moveHistory.removeAt(moveHistory.lastIndex)` on an empty list.
- **Fix:** `if (moveHistory.isEmpty()) return` as the first line, with a comment naming the scenario. Costs nothing on the free path, where `onUndo`'s own guard already ran.
- **Files modified:** `GameViewModel.kt`
- **Commit:** `d5900c7`

**4. [Rule 3 - Blocking] `LargeClass` detekt failure**

- **Found during:** Task 1/2 lint gate
- **Issue:** the funnel pushed `GameViewModel` over detekt's `LargeClass` threshold; `./gradlew :app:detekt` failed.
- **Fix:** `LargeClass` added to the existing class-level `@Suppress("TooManyFunctions", "LongParameterList")` with a "Size suppressions" section in the class KDoc giving the reason — matching the codebase's own documented-suppression convention (`GameScreen`, `BoardCanvas`, `ModeGlyphs`). The detekt **baseline was not touched** and no `MagicNumber` suppression was added, which is what the plan's verification block actually pins.
- **Files modified:** `GameViewModel.kt`
- **Commit:** `d5900c7`

**5. [Rule 3 - Blocking] Recreated the gitignored `local.properties`**

- **Found during:** the first Gradle invocation
- **Issue:** `local.properties` is gitignored, so a fresh worktree has no `sdk.dir` and every `:app` task fails with "SDK location not found".
- **Fix:** copied from the main checkout. Environment-only; the file stays untracked and is in no commit.
- **Files modified:** none tracked
- **Commit:** n/a

---

**Total deviations:** 5 auto-fixed (4 blocking, 1 missing-functionality). No Rule 4 architectural questions arose.
**Impact on plan:** no scope creep in production code — the only production change beyond the plan's text is the `performUndo` guard and the `LargeClass` suppression. The test churn was mandatory: the plan's economy cannot coexist with specs written against a seam that charged for everything.

## Verification Results

| Check | Result |
|-------|--------|
| `./gradlew check --offline` (all suites, detekt, ktlint, dp/sp + ad-id literal gates, `:domain` depth sweep) | **PASS** in 11m 8s |
| `:app:testDebugUnitTest` — 47 classes, **412 tests, 0 failures** | PASS |
| `GameViewModelAllowanceTest` — 13 tests | PASS |
| `GameViewModelTest` (49) / `AdClockTest` (5) / `AssistOrderingTest` (7) / `SoundTest` (15) | PASS |
| `grep -c "adGateway.requestRewarded" GameViewModel.kt` | **1** (only `onAcceptAdOffer`) |
| `grep -c "moveHistory.removeAt\|removeLast" GameViewModel.kt` | **1** (only `performUndo`) |
| `resetToInitial()` contains an assignment to `allowance` | **0** — the D-04 mechanism, commented |
| `applyRestored` contains `restored.allowance` / `toDto` passes `freeHintsRemaining` | present |
| Allowance count leaked into `GameUiState.kt` | none — the three matches are the D-05 comment plan 09-06 mandated |
| UI-SPEC negative contract: `HintControl` / `UndoControl` / `HintToggleControl` / `NoWinBanner` / `GameUiState` changed | **none of the five** |
| Test-only accessor added to `GameViewModel` for Task 3 | none |
| Bare `3`/`5` used as an allowance limit in the new spec | none — prose and named board-geometry constants only |
| detekt baseline entries added | none |

## Known Stubs

None. `onAcceptAdOffer` and `onDismissAdOffer` have no screen caller yet — plan 09-09 wires them to `AdOfferDialog`, exactly as this plan's `<interfaces>` states — but they are fully implemented and fully exercised by the specs, so no surface renders an empty or placeholder value as a result of this plan. The controls remain byte-identical to v1.0, which is the D-05 requirement.

## Threat Flags

None. This plan adds no network endpoint, auth path, file access or schema change. Every threat in its register is mitigated in code and pinned by a test: T-09-32 by the single `requestRewarded` call site behind an accept tap, T-09-33 by the dismiss spec asserting board/history/both counters unchanged, T-09-34 by the Declined spec refilling nothing, T-09-35 by `applyRestored` reading only `restored.allowance` (never the raw DTO ints), T-09-36 by the restart spec, T-09-37 by the Unavailable spec. T-09-38 (D-12 silence) is the accepted disposition and is asserted as the *absence* of a snackbar.

## Next Phase Readiness

- **09-09 (dialog + snackbar wiring)** can call `onAcceptAdOffer()` / `onDismissAdOffer()` directly. `adMessageTick` must be consumed with the `> 0` guard (09-06's note still stands); `pendingAdOffer` is the render flag and `adOfferLoading` the D-10 lock, and both are already cleared at every board-install site.
- **09-10 (real gateway)** must keep `withTimeoutOrNull(AdConstants.ACCEPT_WAIT_MS)` around the ad **load** inside `requestRewarded`. That is the only bound on the dialog's D-10 lock — the ViewModel deliberately applies none, because from there "still loading" and "on screen" are indistinguishable and a blanket cap would abort a watched ad. It must also keep `earned = true` confined to the `OnUserEarnedRewardListener`: `grantReward`'s `Declined` branch refills nothing, and that is what makes an unearned early dismiss cost the advertiser nothing.
- 09-01's one live loose end is closed: `GameViewModel`'s `toDto` call now supplies the real counters, so a save no longer silently writes a full allowance.

## Self-Check: PASSED

- `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` — FOUND
- `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAllowanceTest.kt` — FOUND
- Commits `d5900c7`, `60e5349`, `8685891` — all FOUND in `git log`

---
*Phase: 09-consent-rewarded-ads*
*Completed: 2026-08-31*
