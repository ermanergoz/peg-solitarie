---
phase: 10-interstitials
reviewed: 2026-09-03T13:36:48Z
depth: standard
files_reviewed: 26
files_reviewed_list:
  - app/build.gradle.kts
  - app/src/main/java/com/app/pegsolitaire/App.kt
  - app/src/main/java/com/app/pegsolitaire/data/InterstitialCadenceStore.kt
  - app/src/main/java/com/app/pegsolitaire/platform/ads/AdConstants.kt
  - app/src/main/java/com/app/pegsolitaire/platform/ads/AdGateway.kt
  - app/src/main/java/com/app/pegsolitaire/platform/ads/AdMobAdGateway.kt
  - app/src/main/java/com/app/pegsolitaire/platform/ads/InterstitialAdSlot.kt
  - app/src/main/java/com/app/pegsolitaire/platform/ads/NoOpAdGateway.kt
  - app/src/main/java/com/app/pegsolitaire/platform/lifecycle/AppForegroundTracker.kt
  - app/src/main/java/com/app/pegsolitaire/ui/results/AdQuiescenceScrim.kt
  - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsRoute.kt
  - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt
  - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsUiState.kt
  - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt
  - app/src/main/res/values/strings.xml
  - app/src/test/java/com/app/pegsolitaire/data/InterstitialCadenceStoreTest.kt
  - app/src/test/java/com/app/pegsolitaire/platform/ads/NoOpAdGatewayTest.kt
  - app/src/test/java/com/app/pegsolitaire/platform/lifecycle/AppForegroundTrackerTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/results/FakeAdGateway.kt
  - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsScreenTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceParams.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceState.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceStateTest.kt
  - gradle/libs.versions.toml
findings:
  critical: 1
  warning: 3
  info: 3
  total: 7
status: issues_found
---

# Phase 10: Code Review Report

**Reviewed:** 2026-09-03T13:36:48Z
**Depth:** standard
**Files Reviewed:** 26
**Status:** issues_found

## Summary

Phase 10 (between-level interstitials) was reviewed at standard depth: the pure-domain cadence policy (`InterstitialCadenceState`/`Params`), its DataStore persistence (`InterstitialCadenceStore`), the session-boundary tracker, the interstitial slot and gateway halves, the Results-screen trigger surface (ViewModel, route, screen, scrim), and all associated tests.

The domain policy is in strong shape: the four-gate conjunction is exhaustively tested (one baseline, one flip per gate, boundary values on all four), stored-value validation clamps toward fewer ads, and sentinel/overflow edges (`NEVER_SHOWN`, `Long.MAX_VALUE`, negative deltas) are each pinned with a test that explains its direction. The trigger surface's one-shot latches (`WIN_COUNTED_KEY`, `RECORDED_KEY`, `CELEBRATED_KEY`) follow guard-before-effect consistently, and `pendingNavigation`-as-state correctly survives the stopped-collector window an ad presentation creates.

Four substantive issues were found. One is Critical: the solved-play record guard in `ResultsViewModel` uses the exact `pegsRemaining == 1` comparison that this phase's own KDoc (a few lines below it, in `isInterstitialEligible`) documents as the Diamond-41 trap — a flawless Diamond-41 solve is never recorded, so its catalog stars are permanently unearnable. The three Warnings concern the platform ad path: a cancellation window that strands an already-shown ad in the slot, a threading-contract assertion that matches the legacy GMA SDK rather than the Next-Gen SDK actually in use, and a load retry that escapes the consent gate the code claims every request passes through.

## Critical Issues

### CR-01: Flawless Diamond-41 solve is never recorded — the record guard uses the exact `pegsRemaining == 1` comparison the same file documents as a trap

**File:** `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt:312`
**Issue:** `recordSolvedPlayOnce()` gates the bests write on:

```kotlin
if (args.pegsRemaining != PEGS_REMAINING_SOLVED) return   // PEGS_REMAINING_SOLVED = 1
```

Diamond-41's proven solvability floor is **two** pegs (`BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR = 2`, GF(4) colour-invariant proof, Phase 8), and the domain win test is `board.pegCount <= board.spec.minimumPegs` (`GameViewModel.kt:1516`). A flawless Diamond-41 play therefore arrives at Results as a genuine win — `pegsRemaining = 2`, `stars = 3`, headline PERFECT — and this guard silently skips `progressRepository.recordResult`. Consequences, all permanent for that board: no `LevelResult` row is ever written, the catalog's earned-star pips for Diamond-41 can never light up, and the "New best!" cue can never fire for it.

The irony is that this phase fixed the identical bug in the *ad-eligibility* gate and documented it in this same file: `isInterstitialEligible`'s KDoc (lines 429–435) says a `pegsRemaining == 1` comparison "would misclassify a **perfect Diamond-41 finish as a loss**… That constant is correct where it is already used… which is exactly why it is the line a developer would copy." The record guard three functions up *is* that line, still live. This phase's own test fixture (`DIAMOND_41_PERFECT_PEGS_REMAINING = 2` in `ResultsViewModelTest.kt:1263`) proves the state is reachable and asserts it is PERFECT and ad-eligible — but no test asserts it is recorded, which is how the gap survives.

The guard predates the Phase 10 diff (it is Phase 5's D-08 "only a solved play is recorded"), but Phase 10 both re-documented the trap and added fixtures proving the failing state, so it must not ship unfixed again.

**Fix:** the route args do not carry the finishing board's `minimumPegs`, so the ViewModel cannot rederive "solved" correctly. Carry the solve outcome across the boundary instead — add a `solved: Boolean` (computed from `GameStatus.Won`, which is already minimum-aware) to the typed `Results` route in `GameViewModel`'s navigation, and gate on it:

```kotlin
// Results route gains: val solved: Boolean  (from status == GameStatus.Won)
private fun recordSolvedPlayOnce() {
    if (!args.solved) return
    ...
}
```

Note the trust consequence: `Results` is independently reachable (T-10-06), so a forged `solved = true` could write a stored best for an unplayed level — the same accepted risk class as the existing forged-`stars` arg; `challengeLevel` stays clamped so no forged key is reachable. Add a regression test: a 3-star, 2-peg Diamond-41 result records exactly once. Leave `ResultsUiState.showNextLevel`'s `pegsRemaining == 1` untouched — it is Challenge-only and every Challenge footprint has `minimumPegs == 1`.

## Warnings

### WR-01: Cancellation mid-presentation strands an already-shown ad in the slot with `ready == true`, and leaks it

**File:** `app/src/main/java/com/app/pegsolitaire/platform/ads/AdMobAdGateway.kt:379` (interstitial; same pattern at `:317` for rewarded)
**Issue:** `showToDismissal` calls `ad.show(activity)` and registers `continuation.invokeOnCancellation { ad.adEventCallback = null }`. The terminal callbacks are the **only** path to `recycleInterstitial()` → `interstitialSlot.invalidate()`. If the calling coroutine is cancelled after `show()` (the `viewModelScope` dying while an ad is up — e.g. the back-stack entry being cleared during presentation, or process teardown ordering), the callback is detached and `invalidate()` never runs: the slot keeps holding an ad the SDK forbids showing twice (the code's own Pitfall 9), `readyState` stays `true` so `interstitialReady` advertises an ad that cannot present, and `destroy()` is never called on the instance, leaking its SDK-side resources. The failure degrades (a re-show attempt lands in `onAdFailedToShowFullScreenContent` → silent skip), but the invariant "the shown instance is destroyed on every terminal callback" — stated in the class KDoc — does not hold on the cancellation path. The rewarded path (`showForOutcome`) has the identical gap with `slot.invalidate()`.
**Fix:** invalidate in the cancellation handler as well:

```kotlin
continuation.invokeOnCancellation {
    ad.adEventCallback = null
    recycleInterstitial() // discard the possibly-shown instance; preload re-gates on consent
}
```

(If WR-02's threading verification concludes callbacks/cancellation may run off-main, post this to the main dispatcher to preserve the slot's confinement.)

### WR-02: The lock-free slot design rests on a threading contract quoted from the *legacy* GMA SDK, not the Next-Gen SDK in use

**File:** `app/src/main/java/com/app/pegsolitaire/platform/ads/InterstitialAdSlot.kt:52-58, 74-87` and `app/src/main/java/com/app/pegsolitaire/platform/ads/AdMobAdGateway.kt:55-61, 287-317`
**Issue:** Both KDocs assert "every Google Mobile Ads call except `MobileAds.initialize` must be made from the main thread" and treat the SDK's event callbacks as main-thread, and this claim is load-bearing: it is the entire justification for `held`/`loadedAtMs`/`inFlight` being plain unsynchronized `var`s mutated from `recycleInterstitial()` (invoked inside SDK callbacks) while `heldFresh()`/`preload()` read them from the main thread, and for `var earned` in `showForOutcome` being written by the reward listener and read in `onAdDismissedFullScreenContent` with no happens-before edge. That contract is the **legacy** `play-services-ads` contract. A headline change of the GMA **Next-Gen** SDK (`ads-mobile-sdk`, the artifact this project pins, per its own typosquat warning in `libs.versions.toml`) is precisely the *removal* of main-thread restrictions, and Next-Gen does not document event callbacks as main-thread-guaranteed. If callbacks arrive on a background executor, every one of those fields is a data race: a torn `held`/`readyState` pair, a lost `inFlight` reset, or an `earned = true` that the dismissal read never observes (a player who earned a reward resolved as `Declined` — the exact ADS-05 outcome the code works hardest to prevent).
**Fix:** verify the Next-Gen callback-threading contract against the SDK docs and record the citation in the KDoc. If callbacks are not main-guaranteed, hop explicitly: wrap the callback bodies in `scope.launch(Dispatchers.Main) { ... }` (or make the slot fields `AtomicReference`/`MutableStateFlow` and `earned` an `AtomicBoolean`). Do not leave the confinement claim as an unsourced assertion — everything in these two files leans on it.

### WR-03: The slot's 30-second load retry escapes the consent gate — a request can be issued after consent is revoked

**File:** `app/src/main/java/com/app/pegsolitaire/platform/ads/InterstitialAdSlot.kt:203-216`
**Issue:** `AdMobAdGateway.preloadInterstitial()` checks `consentGateway.readiness == Ready` before calling `interstitialSlot.preload()`, and `recycleInterstitial()`'s KDoc promises "a request is never issued against a consent state that forbids it." But the slot's `loadIntoSlot()` retry breaks that promise: after a failed first attempt it waits `PRELOAD_RETRY_DELAY_MS` (30 s) and calls `loadOnce()` again with **no consent re-check**. The player can revoke consent in Settings ("Manage privacy choices") inside that window — reachable in exactly the no-network scenario that makes the first attempt fail — and the delayed second request fires anyway. By the project's own threat model, "issuing the request is itself the GDPR exposure" (T-09-44), so this is a stated compliance invariant that does not hold, not a style nit. The same structure exists in `RewardedAdSlot` (out of scope this phase) and should be fixed in both to preserve the file's deliberate symmetry.
**Fix:** give the slot a consent predicate and gate every attempt, e.g. inject `canRequest: () -> Boolean` (bound by the gateway to `consentGateway.readiness.value == AdsReadiness.Ready`) and:

```kotlin
private suspend fun loadIntoSlot(): InterstitialAd? {
    val ad = loadOnce() ?: run {
        delay(AdConstants.PRELOAD_RETRY_DELAY_MS)
        if (canRequest()) loadOnce() else null
    }
    ...
}
```

## Info

### IN-01: Physical-device confirmation of assumption A1 (session boundary vs. ad Activity) is still open

**File:** `app/src/main/java/com/app/pegsolitaire/platform/lifecycle/AppForegroundTracker.kt:29-37`
**Issue:** The KDoc honestly records that the "a same-process ad Activity does not forge a background" observation was made only on the `sdk_gphone16k_arm64` emulator, and that plan 10-06 task 2 step 8 must be re-run on real hardware before release. If the observation fails on a device, this class is the wrong session-boundary source and the D-04 cap silently stops binding (T-10-11).
**Fix:** carry this forward as an explicit release-checklist item in the phase VERIFICATION artifact so it cannot be lost in a KDoc; it is currently discoverable only by reading this file.

### IN-02: Rewarded and interstitial cooldown stamps are taken at asymmetric instants

**File:** `app/src/main/java/com/app/pegsolitaire/platform/ads/AdMobAdGateway.kt:292` vs `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt:262`
**Issue:** The interstitial stamp is deliberately taken *after* dismissal ("strictly more conservative", ResultsViewModel KDoc), but the rewarded stamp is written in `onAdShowedFullScreenContent` — at presentation *start*. A 30-second rewarded ad therefore shortens the effective D-15 gap to the next interstitial by its own duration: the 90 s are measured from the moment the rewarded ad appeared, not from when the player got their screen back.
**Fix:** consider also stamping the rewarded path on dismissal (or both callbacks — the later write wins harmlessly). Low stakes since D-01's three-win gate almost always dominates, but the asymmetry contradicts the "later instant is more conservative" reasoning applied one file over.

### IN-03: The Robolectric SDK pin and sample fixtures are re-declared per test file

**File:** `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsScreenTest.kt:255` and `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt:1233`
**Issue:** `ROBOLECTRIC_SDK = 34` (with its "no android-all jar for SDK 36 yet" justification) plus `SAMPLE_CHALLENGE_LEVEL`, `SAMPLE_ELAPSED_SECONDS` etc. are duplicated as file-private constants in the same package (and the SDK pin exists in further test files besides). File-private top-levels compile fine, but when the SDK 36 jar lands, every copy must be found and moved in lockstep — the exact drift the codebase's "named once" discipline exists to prevent.
**Fix:** hoist `ROBOLECTRIC_SDK` into one shared test-fixture object (e.g. `testFixtures` or a shared `RobolectricConfig` constant) referenced by all Robolectric-annotated tests.

---

_Reviewed: 2026-09-03T13:36:48Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
