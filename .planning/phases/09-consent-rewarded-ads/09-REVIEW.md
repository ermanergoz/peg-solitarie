---
phase: 09-consent-rewarded-ads
reviewed: 2026-09-02T01:02:32Z
depth: standard
files_reviewed: 49
files_reviewed_list:
  - app/src/main/AndroidManifest.xml
  - app/src/main/java/com/app/pegsolitaire/MainActivity.kt
  - app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt
  - app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt
  - app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt
  - app/src/main/java/com/app/pegsolitaire/platform/ads/AdConstants.kt
  - app/src/main/java/com/app/pegsolitaire/platform/ads/AdGateway.kt
  - app/src/main/java/com/app/pegsolitaire/platform/ads/AdIds.kt
  - app/src/main/java/com/app/pegsolitaire/platform/ads/AdMobAdGateway.kt
  - app/src/main/java/com/app/pegsolitaire/platform/ads/NoOpAdGateway.kt
  - app/src/main/java/com/app/pegsolitaire/platform/ads/RewardedAdSlot.kt
  - app/src/main/java/com/app/pegsolitaire/platform/ads/consent/AdsReadiness.kt
  - app/src/main/java/com/app/pegsolitaire/platform/ads/consent/ConsentGateway.kt
  - app/src/main/java/com/app/pegsolitaire/platform/ads/consent/UmpConsentGateway.kt
  - app/src/main/java/com/app/pegsolitaire/platform/ads/di/AdsModule.kt
  - app/src/main/java/com/app/pegsolitaire/ui/components/ClaySnackbarHost.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/AdOfferDialog.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
  - app/src/main/java/com/app/pegsolitaire/ui/settings/PrivacyLinks.kt
  - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsRoute.kt
  - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt
  - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsUiState.kt
  - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsViewModel.kt
  - app/src/main/res/values/dimens.xml
  - app/src/main/res/values/strings.xml
  - app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt
  - app/src/test/java/com/app/pegsolitaire/platform/ads/NoOpAdGatewayTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/AdOfferDialogTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenAdOfferTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenSoundTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAdClockTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAllowanceTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAssistOrderingTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelChallengeAdvanceTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelHintStalenessTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelResumeTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelSoundTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/settings/FakeConsentGateway.kt
  - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsPrivacyTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelSoundTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelTest.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/assist/AssistAllowance.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/assist/AssistAllowanceTest.kt
findings:
  critical: 0
  warning: 5
  info: 5
  total: 10
status: issues_found
---

# Phase 9: Code Review Report

**Reviewed:** 2026-09-02T01:02:32Z
**Depth:** standard
**Files Reviewed:** 49
**Status:** issues_found

## Summary

Reviewed the full Phase 9 surface: the pure-domain `AssistAllowance` policy, the resume-snapshot allowance fields and their fail-open codec, the assist funnel and ad-clock freeze in `GameViewModel`, the opt-in dialog and snackbar surfaces, the Settings privacy section, the UMP consent gateway, and the real GMA Next-Gen gateway/slot. The known deliberate decisions (D-20, in-class SDK handles, the single `ACCEPT_WAIT_MS` timeout, the post-grant re-warm) were treated as ground truth and are not reported.

The domain policy, snapshot validation asymmetry (refuse a forged `minimumPegs`, fall open on a forged counter), and the funnel ordering (search first / guard first, charge second) are correct and thoroughly pinned by tests — the untrusted-input handling in `SnapshotMapper`/`AssistAllowance` is genuinely strong. The defects found cluster on the two seams no automated test exercises: the accept path's re-entrancy in `GameViewModel`, and the real SDK-facing classes (`AdMobAdGateway`, `RewardedAdSlot`, `UmpConsentGateway`), which have zero unit coverage and several places where the code's own documented invariants ("hand it over exactly once", "every failure degrades to Unavailable", "idempotent per Activity creation") are not actually enforced by the implementation.

No hardcoded secrets, no ad-ID literals, no logging of consent/ad data, no injection surfaces (the privacy-policy `ACTION_VIEW` URI is a compile-time constant). No Critical findings.

## Narrative Findings (AI reviewer)

## Warnings

### WR-01: `onAcceptAdOffer` is not re-entrancy guarded — a double-tap double-freezes the clock, requests two ads, and performs the assist twice

**File:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:1294-1313` (with `freezeClockForAd` at 977-983)
**Issue:** The accept button is disabled via `enabled = !loading`, but that disable only lands at the *next* recomposition. Two clicks delivered in the same frame (the standard Compose multi-click race) both invoke `onAcceptAdOffer()`, and nothing in the ViewModel rejects the second call: `pendingAdOffer` is still non-null (it is cleared only after the outcome) and there is no `adOfferLoading` check. Consequences, traced:

1. `freezeClockForAd()` runs twice. It folds `mark.elapsedNow()` into `elapsedBaseSeconds` but never nulls `startMark`, so the second call folds the *same* elapsed span a second time — e.g. 60 s of play becomes 120 s of charged time. This permanently inflates the elapsed clock the score decays against, which is precisely what ADSF-01 promises cannot happen. (The unfreeze is guarded by `frozenForAd`, so only the freeze double-counts.)
2. Two concurrent `requestRewarded` calls reach the gateway. `RewardedAdSlot.await()` hands the *same* held `RewardedAd` instance to both (see WR-03); after the first presentation's `recycleSlot()` destroys it, the second caller can attempt `show()` on a destroyed, already-shown ad (see WR-02).
3. Both coroutines run `grantReward(...)` on return — for `AssistKind.UNDO` that is two `performUndo()` calls: a second move popped that the player never asked for.

Relatedly, `onDismissAdOffer` and `onCellTap` also rely purely on the dialog's UI-level modality with no ViewModel-level guard while `adOfferLoading` is set; the same one-line guard covers that as defense-in-depth.
**Fix:**
```kotlin
fun onAcceptAdOffer() {
    val state = _uiState.value
    if (state.adOfferLoading) return // a request is already in flight
    val reason = state.pendingAdOffer ?: return
    ...
}
```

### WR-02: `AdMobAdGateway.showForOutcome` has no exception containment — a synchronous SDK throw escapes as an app crash instead of degrading to `Unavailable`

**File:** `app/src/main/java/com/app/pegsolitaire/platform/ads/AdMobAdGateway.kt:156-189` (caller: `GameViewModel.kt:1300-1305`)
**Issue:** The class KDoc states "Every failure degrades to `RewardOutcome.Unavailable`", and `RewardedAdSlot.loadOnce` honours that with a `runCatching` — its own comment notes GMA Next-Gen *throws* on ad calls in bad states. But `showForOutcome` attaches `ad.adEventCallback` and calls `ad.show(activity) { ... }` with no containment at all. A synchronous throw from `show()` (a destroyed or already-shown instance — reachable via WR-01/WR-03 — or any SDK-internal state error) propagates out of `suspendCancellableCoroutine`, through `requestRewarded`, into `onAcceptAdOffer`'s coroutine. The `try/finally` there unfreezes the clock but catches nothing, so the exception is uncaught in `viewModelScope` → app crash mid-gameplay. The one place the fail-open stance matters most — the presentation itself — is the one place it is not applied.
**Fix:** Contain the show call and resume with the fail-open outcome, preserving cancellation:
```kotlin
try {
    ad.show(activity) { _: RewardItem -> earned = true }
} catch (e: Exception) {
    if (e is CancellationException) throw e
    recycleSlot(reason)
    if (continuation.isActive) continuation.resume(RewardOutcome.Unavailable)
}
```

### WR-03: `RewardedAdSlot` does not enforce its "hand it over exactly once" invariant — `await()` leaves the ad in the slot, and cancellation never recycles it

**File:** `app/src/main/java/com/app/pegsolitaire/platform/ads/RewardedAdSlot.kt:107` and `AdMobAdGateway.kt:188`
**Issue:** The class KDoc promises "hand it over exactly once, then throw it away", but `await()` (`freshOrDiscard() ?: startLoad().await()`) returns the held ad *without clearing `held`*. The slot is only emptied by the gateway's terminal show callbacks (`recycleSlot`). Two consequences:

1. **Concurrent/second `await()` returns the same instance** — the WR-01 race gets the identical `RewardedAd` for both requests, and a rewarded ad may be shown exactly once (Pitfall 11, the slot's own reason for existing).
2. **Cancellation leaks a spent ad into the slot.** `showForOutcome`'s `invokeOnCancellation` only detaches `ad.adEventCallback`; it never invalidates the slot. If the accepting coroutine is cancelled during presentation (ViewModel cleared), the shown-once ad stays in `held` looking fresh, and the *next* offer — possibly a new level's ViewModel — receives it, guaranteeing a failed show and a fall-open free grant.
3. A related edge: because the being-shown ad remains `held`, any `preload()`/`await()` whose `freshOrDiscard()` finds it over-age calls `invalidate()` → `destroy()` on an ad that is *currently presenting*.
**Fix:** Give `await()` take-semantics — clear `held`/`loadedAtMs` when handing the ad over (the gateway already owns discarding it via the terminal callbacks) — and add `slot.invalidate()` to the gateway's `invokeOnCancellation` block. `recycleSlot`'s `invalidate()` then becomes a harmless no-op for the handed-out instance.

### WR-04: `UmpConsentGateway.bootstrap` has no in-flight or once-per-Activity guard, contradicting its documented idempotency contract

**File:** `app/src/main/java/com/app/pegsolitaire/platform/ads/consent/UmpConsentGateway.kt:108-163`
**Issue:** `ConsentGateway.bootstrap`'s KDoc requires: "Idempotent, and called once per Activity creation — a configuration change must not re-run the form or double-initialize the SDK." The `initialized` AtomicBoolean guards only SDK initialization. The rest of the sequence — `awaitActivity`, `requestConsentInfoUpdate`, `loadAndShowConsentFormIfRequired` — re-runs unguarded on every `MainActivity.onCreate`, i.e. every rotation. Two concrete failure modes:

1. A rotation while the consent form is on screen (the highest-probability moment for a rotation, since the form blocks play) creates a new Activity, whose bootstrap awaits it and calls `loadAndShowConsentFormIfRequired` again — a second form load stacked on a consent state still mid-flow.
2. A rotation during the up-to-10 s `ACTIVITY_WAIT_MS` window leaves two full bootstrap coroutines running concurrently against the same `ConsentInformation`, issuing parallel `requestConsentInfoUpdate` calls whose callbacks interleave into `_readiness`/`_privacyOptionsRequired` in unspecified order.

The per-*launch* refresh is deliberate (D-16); the per-*rotation* unbounded re-entry is not, and the interface KDoc says so explicitly.
**Fix:** Track the in-flight sequence and make re-entry a no-op:
```kotlin
private var bootstrapJob: Job? = null

override fun bootstrap() {
    if (bootstrapJob?.isActive == true) return
    bootstrapJob = scope.launch(Dispatchers.Main) { ... }
}
```
(If a genuinely fresh sequence per cold launch is wanted, this still permits it — only a concurrent duplicate is suppressed.)

### WR-05: a game restored with an exhausted counter never triggers the D-09 preload, so the first post-resume offer always cold-loads

**File:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:430-445` (funnel), `608-649` (`applyRestored`)
**Issue:** `enterAssistFunnel` warms an ad only on "the spend that takes a free counter to zero". `applyRestored` can install an allowance that is *already* zero (a stored `0` is deliberately preserved — D-04/D-17), and no path preloads in that case. The player's very next assist raises the offer against a completely cold slot: accepting starts the load from scratch under the 5 s `ACCEPT_WAIT_MS` cap, so on any slow network the request falls open to a free grant plus the "ad unavailable" snackbar — for an ad that was never actually unavailable, merely never requested. This undercuts D-09's own stated purpose ("so the next offer has something to show") on exactly the sessions most likely to hit the offer (a resumed, part-spent game). The same gap applies to a restored counter of 0 for either kind independently.
**Fix:** In `applyRestored`, after `allowance = restored.allowance`, warm each exhausted kind:
```kotlin
if (!allowance.hasFree(AssistKind.HINT)) adGateway.preloadRewarded(AdReason.HINT)
if (!allowance.hasFree(AssistKind.UNDO)) adGateway.preloadRewarded(AdReason.UNDO)
```
(The gateway's readiness gate already suppresses the request when consent forbids it, and the slot is idempotent, so the two calls collapse to at most one load.)

## Info

### IN-01: the dialog's documented 5 s loading bound is actually 15 s pre-show

**File:** `app/src/main/java/com/app/pegsolitaire/platform/ads/AdMobAdGateway.kt:111-131`; docs in `AdOfferDialog.kt:75`, `AdConstants.kt:25`, `GameUiState.kt:90-93`
**Issue:** `requestRewarded` waits `ACCEPT_WAIT_MS` (5 s) for the load *and then* up to `ACTIVITY_WAIT_MS` (10 s) for a foreground Activity, sequentially. The dialog's locked loading state (back and scrim disabled) is therefore bounded at 15 s before an ad shows, while at least four KDocs state the wait "is hard-bounded by `AdConstants.ACCEPT_WAIT_MS`". Behaviour is still bounded, so this is a documentation-accuracy issue with a real player-visible consequence in the worst case.
**Fix:** Either budget the activity wait inside the accept window (`withTimeoutOrNull(ACCEPT_WAIT_MS) { ad + activity }`) or correct the four KDocs to name the true 15 s bound.

### IN-02: `UmpConsentGateway.showPrivacyOptions` does not implement its documented "silent no-op when `privacyOptionsRequired` is false"

**File:** `app/src/main/java/com/app/pegsolitaire/platform/ads/consent/UmpConsentGateway.kt:165-183`; contract in `ConsentGateway.kt:60-62`
**Issue:** The interface promises a silent no-op when the requirement is false; the implementation unconditionally awaits an Activity and calls `UserMessagingPlatform.showPrivacyOptionsForm`. Today the UI hides the row when not required, so the path is mostly unreachable — but the seam's contract and its only implementation disagree, and a future caller trusting the KDoc would put a UMP error round-trip (discarded) where it expected nothing.
**Fix:** `if (!privacyOptionsRequired.value) return` at the top, or amend the interface KDoc.

### IN-03: readiness is sampled from `MobileAds.isInitialized` immediately after `initialize(...)` returns — verify the call is synchronous, or the latch has no in-session recovery

**File:** `app/src/main/java/com/app/pegsolitaire/platform/ads/consent/UmpConsentGateway.kt:214-224`
**Issue:** The first (winning) caller sets `_readiness` from `MobileAds.isInitialized` on the line after `MobileAds.initialize(appContext, config)` returns. If Next-Gen initialization completes asynchronously after that call returns (the `@WorkerThread`/ANR warning suggests it blocks, but this is not pinned anywhere), readiness latches `Unavailable` for the session — nothing re-evaluates it except the second-caller branch, which only runs on a configuration-change bootstrap. Rewarded offers would then silently fall open to free grants all session.
**Fix:** Confirm the synchronous contract of `MobileAds.initialize(Context, InitializationConfig)` against ads-mobile-sdk 1.4.0 and record it in the comment; if it is not synchronous, publish `Ready` from the SDK's completion signal instead of sampling.

### IN-04: the three SDK-facing classes — including the ADS-05 reward line — have zero automated coverage

**File:** `app/src/main/java/com/app/pegsolitaire/platform/ads/AdMobAdGateway.kt`, `RewardedAdSlot.kt`, `consent/UmpConsentGateway.kt`
**Issue:** Every behaviour above the seam is exhaustively spec'd, but nothing executable pins the gateway's own logic: the reward-only-from-earned-callback rule its KDoc calls "the single most consequential line in this file" (ADS-05/T-09-43), the double-resume guards, the slot's single-ad/age-out lifecycle, or the consent state machine. WR-01/WR-02/WR-03 all live exactly in this untested band. The SDK types are constructible obstacles, but the state machines around them (slot bookkeeping, readiness transitions, `earned` flag plumbing) are extractable and testable without a device.
**Fix:** Extract the callback-to-outcome mapping and slot bookkeeping behind thin SDK-free seams (e.g. the slot parameterised over a `suspend () -> RewardedAd?` loader) and pin at minimum: dismissal-without-reward → `Declined`, failed show → `Unavailable`, one hand-out per loaded ad, and the readiness transitions of `initializeAdsIfPermitted`.

### IN-05: `GameRoute` reads the non-snapshot `viewModel.challengeLevel` in composition

**File:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt:90` (source: `GameViewModel.kt:155-181`)
**Issue:** `challengeLevel` delegates to plain `var stampedChallengeLevel`, which is not Compose snapshot state. `GameScreen(challengeLevel = viewModel.challengeLevel)` therefore only picks up the WR-03-mandated restored stamp because `applyRestored` happens to update `_uiState` immediately afterwards, forcing a recomposition that re-reads the getter. The coupling is correct today but is a silent ordering dependency: any future path that sets the stamp without touching `_uiState` leaves the top-bar "Level N" title showing the stale route value.
**Fix:** Carry the effective challenge level in `GameUiState` (or document the ordering dependency at both sites) so the title reads snapshot-backed state like everything else on the screen.

---

_Reviewed: 2026-09-02T01:02:32Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
