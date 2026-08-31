---
phase: 09-consent-rewarded-ads
plan: 04
subsystem: gameplay-viewmodel
tags: [ad-clock, elapsed-time, rewarded-ads, virtual-time-testing, coroutines]

# Dependency graph
requires:
  - phase: 09-consent-rewarded-ads (plan 09-02)
    provides: widened AdGateway seam whose requestRewarded call sites the freeze wraps
provides:
  - frozenForAd flag plus freezeClockForAd()/unfreezeClockAfterAd() in GameViewModel
  - resumeTickerIfNeeded() early-return guard so lifecycle resume cannot restart a frozen clock (Pitfall 10)
  - try/finally wrapping of both adGateway.requestRewarded call sites (onUndo, onHintRequested) so unfreeze runs on every exit path, cancellation included
  - startMark rebase on unfreeze so a terminal freeze never refunds time the board sat stuck (D-07)
  - GameViewModelAdClockTest — ADSF-01 pinned in virtual time (clock frozen across request, resumes after, rescue-undo does not re-add ad duration, never-started clock freeze is inert, finished game does not resurrect its ticker)
affects: [09-06, 09-08, 09-09, scoring, gameplay-viewmodel]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "finally-cancelling test wrapper (adClockTest) mirroring hintTest so a red clock spec goes red instead of hanging runTest's virtual-time drain"

key-files:
  created:
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAdClockTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt

key-decisions:
  - "Unfreeze rebases from startMark, not displayed elapsedSeconds, so a freeze that outlives the game cannot refund stuck-board time (D-07)"
  - "frozenForAd cleared in resetToInitial() so a retry never inherits a frozen clock"
  - "Both requestRewarded call sites wrapped in try/finally — unfreeze is unconditional on cancellation and exception paths"

patterns-established:
  - "Ad-presentation clock freeze: freeze immediately before adGateway.requestRewarded, unfreeze in finally, guard lifecycle resume with frozenForAd"

requirements-completed: [ADSF-01]

# Metrics
duration: ~35 min (agent stalled at closeout; summary authored by orchestrator from commits)
completed: 2026-08-30
---

# Phase 9 Plan 04: Ad-Clock Freeze Summary

**The elapsed-time ticker freezes for the entire rewarded-ad presentation and resumes exactly where it stopped — enforced with a `frozenForAd` flag, try/finally unfreeze around both `requestRewarded` call sites, and a lifecycle-resume guard — so watching an ad can never cost the player time-based score (ADSF-01).**

## What shipped

- `GameViewModel.kt` (+109/-3): `frozenForAd` flag beside the ticker, `freezeClockForAd()` / `unfreezeClockAfterAd()`, `if (frozenForAd) return` as the first line of `resumeTickerIfNeeded()` (Pitfall 10), both `adGateway.requestRewarded` call sites (`onUndo`, `onHintRequested`) wrapped in try/finally so the unfreeze runs on every exit including cancellation, rebase from `startMark` on unfreeze (D-07), and `frozenForAd` cleared in `resetToInitial()`.
- `GameViewModelAdClockTest.kt` (488 lines, then hardened +28/-17): pins ADSF-01 in virtual time — clock does not advance across a gated rewarded request and resumes after; rescue-banner undo after an ad does not re-add the ad's duration; freezing a never-started clock is inert; a game that finishes while the ad is up does not resurrect its ticker. Specs route through a finally-cancelling `adClockTest` wrapper (same idiom as `hintTest`) so a failing assertion reports instead of hanging `runTest`'s end-of-test drain.

## Verification

- Executor's last report before closeout: all 94 GameViewModel tests green.
- Orchestrator spot-check on the merged tree: all plan `key_links` patterns present in `GameViewModel.kt` (`frozenForAd` declared line 267, guard at 772, freeze at 809, unfreeze at 831–832, reset at 1098; `freezeClockForAd()` at both call sites).
- Post-merge `:app:testDebugUnitTest` + `:domain:test` gate run by the orchestrator after the wave-2 merge (see wave record).

## Self-Check

Closed out by the orchestrator: the executor agent completed all implementation commits (`c70b3f9`, `1236c54`, `e90c5c5`) with a clean working tree but stalled twice (stream watchdog) before writing this summary. No code was authored outside the executor's commits; this file records them.

## Note for downstream plans

`freezeClockForAd()`/`unfreezeClockAfterAd()` are the only sanctioned way to bracket an ad presentation. Plan 09-08's allowance-charging flow must keep the charge inside the same try/finally bracket established here, and 09-06's ordering inversion must not move `requestRewarded` outside it.
