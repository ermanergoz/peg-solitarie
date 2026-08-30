---
phase: 08-assist-generator-quality-debt
fixed_at: 2026-08-30T00:00:00Z
review_path: .planning/phases/08-assist-generator-quality-debt/08-REVIEW.md
iteration: 1
findings_in_scope: 1
fixed: 1
skipped: 0
status: all_fixed
---

# Phase 08: Code Review Fix Report

**Fixed at:** 2026-08-30
**Source review:** .planning/phases/08-assist-generator-quality-debt/08-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 1 (fix scope: critical_warning — 0 Critical, 1 Warning; the 5 Info findings are out of scope)
- Fixed: 1
- Skipped: 0

## Fixed Issues

### WR-01: Hint lifecycle does not cover the async board-install window

**Files modified:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt`, `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelHintStalenessTest.kt`
**Commit:** 4c4a24a
**Applied fix:** Both changes the review recommended, restoring the documented "every board-mutating site cancels" invariant (CR-01 contract):

1. `onHintRequested()` now refuses the request outright while `GameUiState.isLoading` (`if (_uiState.value.isLoading) return`), with a KDoc paragraph explaining why the guard must live in the ViewModel (GameScreen renders the Hint control unconditionally, and a search against the loading placeholder would pass the stale-result guard and raise the untruthful "no winning move" rescue banner over the loading indicator).
2. All three async board-install sites — `loadChallengeBoardIfNeeded`, `installClassicBoard`, `applyRestored` — now call `cancelHintSearch()` immediately before their `_uiState.update` rebuild. The "caller owes the flag" obligation is met by the rebuild itself: `GameUiState.initial()` carries `solverThinking = false`, noted in a comment at each site.
3. The `onHintRequested` KDoc's list of cancelling sites was extended to name the three install sites, so the documentation cannot drift from the restored invariant.

**Regression test added:** `aHintDuringTheChallengeLoadWindowIsRefusedNotAnsweredWithTheRescueBanner` in `GameViewModelHintStalenessTest`, following the suite's existing harness pattern (`hintTest { … }` with the load-bearing `finally { cancelTickers() }`, `runCurrent()` only — never `advanceUntilIdle()`, and `gate.pending == 1` asserted so "the load really was in flight" is proven). A new `ChallengeHintLevelRepository` + `challengeViewModelFor` helper parks the Challenge generation at the gated dispatcher, so the test taps Hint while `isLoading` is genuinely true, asserts the request is refused (no spinner, no second parked runnable), then releases the load and asserts the installed board arrives with no rescue banner and that a fresh hint on the real board still resolves to a legal move. The existing `viewModelFor` was refactored to share a `viewModelWith` factory; no existing test behavior changed.

**Verification:** Full `:app:testDebugUnitTest` (all 6 staleness tests including the new one), `ktlintCheck`, and `detekt` all pass.

---

_Fixed: 2026-08-30_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
