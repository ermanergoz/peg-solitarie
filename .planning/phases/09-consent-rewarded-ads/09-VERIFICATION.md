---
phase: 09-consent-rewarded-ads
verified: 2026-09-02T00:00:00Z
status: passed
score: 9/9 must-haves verified
overrides_applied: 1
overrides:
  - must_have: "Declining consent leaves every assist free with no dialog and no snackbar, exactly like v1.0"
    reason: "D-20 (user decision, 2026-09-02): `rewardedOfferable` gates on `consentInformation.canRequestAds()`, not additionally on TCF Purpose-1 consent, so declining players still receive non-personalized rewarded offers per Google's limited-ads/legitimate-interest bases. UMP consent governs data processing, not ad existence; every ad remains per-interaction opt-in; gating on Purpose 1 would strip declining players of the ability to earn assists. Device-evidence item 4 (09-DEVICE-EVIDENCE.md) is reclassified pass-by-decision — CONS-04 as written in REQUIREMENTS.md (consent-flow failure falls back to no ads) is unaffected; only plan 09-11's stricter checklist wording was overridden."
    accepted_by: "user (ermanergoz)"
    accepted_at: "2026-09-02T00:00:00Z"
---

# Phase 9: Consent & Rewarded Ads — Verification Report

**Phase Goal:** Consent & Rewarded Ads — UMP consent, opt-in-first rewarded assists, fail-open on no-fill, clock frozen across every ad
**Verified:** 2026-09-02
**Status:** passed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (merged: ROADMAP Success Criteria + PLAN must_haves, D-20 override applied)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Fresh install in a consent region: consent form appears before Home and before any ad request; SDK initializes only after consent resolves; player can reopen privacy choices from Settings (CONS-01/02/03) | VERIFIED | Code: `UmpConsentGateway.bootstrap()` gates `initializeAdsIfPermitted()` behind `requestConsentInfoUpdate`/`loadAndShowConsentFormIfRequired`; `MainActivity` calls `consentGateway.bootstrap()` before `setContent`. Device evidence items 1, 2, 3 all PASS (09-DEVICE-EVIDENCE.md): form rendered before any `GoogleMobileAds*` logcat line, zero ad activity before consent resolved, refresh confirmed on relaunch without re-prompt. |
| 1a | Player can open the privacy policy from Settings on every install (REL-05) | VERIFIED | `PrivacyLinks.POLICY_URL` (compile-time const, `ermanergoz.com/policies/...`) wired via `Intent(ACTION_VIEW, ...)` in `SettingsScreen.kt:247`; row always rendered (not gated). Device evidence item 7 PASS: Chrome opened the live page, headed "Privacy Policy — Last updated: September 2, 2026". |
| 2 | Free allowance of hints/undos per level; opt-in dialog appears only once spent; declining leaves game unchanged; ad never plays automatically (ADS-01, ADS-02) | VERIFIED | `AssistAllowance`/`AssistAllowanceLimits` (domain, 141 lines, 13 passing tests) define 3 hints / 5 undos, independent counters, refill-to-full, spend-floor-at-zero. `GameViewModel.enterAssistFunnel` raises `pendingAdOffer` only after allowance exhausted; `onDismissAdOffer` clears state with no side effect. Device evidence item 8 PASS: hints 1-3 silent, hint 4 raised dialog; "No thanks" left board pixel-identical. |
| 3 | Rescue-banner offer reachable; reward lands only when the ad reports it earned (ADS-04, ADS-05) | VERIFIED | `GameScreen.kt` rescue banner (Undo/Restart/Home, no ad affordance) routes its Undo through the same `onUndo` → `enterAssistFunnel` path as the toolbar button (D-08). `AdMobAdGateway` sets `earned = true` only inside `OnUserEarnedRewardListener`, grep-pinned exactly once (plan 09-10, confirmed by review IN-04's structural note). Device evidence items 9a (4x reproduced, PASS) and 10 (PASS) confirm on-device. Item 9b (early-dismiss ⇒ Declined) could not be produced on Google's test rewarded inventory (fires `onUserEarnedReward` within ~2s, no pre-reward close affordance) — this is a documented limitation of test inventory, not of the app; the negative branch is proven at the unit level (`GameViewModelAllowanceTest`: Declined refills/spends nothing) and structurally (`onAdDismissedFullScreenContent` resolves `if (earned) Granted else Declined`). |
| 4 | When an ad cannot load/fill, or consent flow fails, player still gets the assist with brief explanation, no dead end (ADS-03, CONS-04) | VERIFIED | `GameViewModel` snackbar path (`emitAdMessage`) fires exactly on `RewardOutcome.Unavailable`; `AssistAllowanceLimits`-scoped constants, `ACCEPT_WAIT_MS` 5s cap enforced in `AdMobAdGateway`. Device evidence item 13 PASS: airplane mode, allowance exhausted, dialog locked ~5s then fell open with snackbar "Ad unavailable — hints refilled for free". CONS-04 (consent-flow-fails path, distinct from decline) is coded via `AdsReadiness.Unavailable` fail-open in `UmpConsentGateway` (multiple `_readiness.value = AdsReadiness.Unavailable` failure branches) and unit-tested (`GameViewModelAllowanceTest` "Knowably unservable ads are silent" section). |
| 5 | Elapsed clock frozen for full ad duration, resumes on dismiss; a "no winning move" hint costs neither allowance nor ad (ADSF-01, ADS-06) | VERIFIED | `GameViewModel`: `frozenForAd` flag, `freezeClockForAd()`/`unfreezeClockAfterAd()` wrap `requestRewarded` call sites; `resumeTickerIfNeeded()` refuses while frozen. `onHintRequested` calls the solver before entering the funnel (ADS-06 inversion, plan 09-06). Device evidence item 12 PASS: two independent wall-clock measurements (39.6s and ~30s ad presentations) show near-exact frozen/resumed timer deltas (342s observed vs 341.4s predicted-if-frozen; +1s across a 30s ad). Item 11 PASS: "no winning move" hint raised no dialog, zero `AdActivity` lines. |
| 6 | Declining consent still behaves per D-20 policy (CONS-04 checklist wording override) | PASSED (override) | Device evidence item 4: declining consent (`IABTCF_PurposeConsents=00000000000`) still allows the ads SDK to initialize and the opt-in dialog to appear, because `canRequestAds()` returns true under legitimate-interest bases. Plan 09-11's stricter must-have truth #2 ("no dialog and no snackbar" on decline) is FAILED as literally written, but D-20 (recorded 09-CONTEXT.md, 2026-09-02) is an explicit user product decision accepting this behavior — see override above. |
| 7 | `AssistAllowanceLimits`/timeout constants (`ACCEPT_WAIT_MS` etc.) are scoped, no magic numbers/strings; detekt/ktlint pass (CLAUDE.md gate) | VERIFIED | `./gradlew app:detekt domain:detekt app:ktlintCheck domain:ktlintCheck --offline` all green (this run, current commit `af7f993`). No `TBD`/`FIXME`/`XXX` markers found in Phase 9 files. |
| 8 | Requirements CONS-01..04, ADS-01..06, ADSF-01, REL-05 all satisfied and traceable to plans | VERIFIED | See Requirements Coverage table below — all 12 IDs claimed across the 11 plans, no orphans against REQUIREMENTS.md's Phase 9 mapping. |
| 9 | Full offline build/test gate is green on the verified commit | VERIFIED | `./gradlew :app:testDebugUnitTest :domain:test --offline` — all up-to-date/green, 0 failures across every Phase-9 test file (`AssistAllowanceTest` 13/13, `GameViewModelAllowanceTest` 13/13, `GameViewModelAdClockTest` 5/5, `AdOfferDialogTest` 8/8, `GameScreenAdOfferTest` 8/8, `SettingsPrivacyTest` 8/8 — this verification run, independent of SUMMARY claims). |

**Score:** 9/9 truths verified (8 VERIFIED + 1 PASSED (override))

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `domain/.../assist/AssistAllowance.kt` | Pure `:domain` policy, `AssistAllowanceLimits` constants | VERIFIED | 141 lines, no Android imports, 13 passing unit tests |
| `app/.../platform/ads/AdGateway.kt` | `rewardedOfferable` + `preloadRewarded` seam | VERIFIED | Confirmed present and consumed by `GameViewModel`, `NoOpAdGateway`, `AdMobAdGateway` |
| `app/.../platform/ads/consent/UmpConsentGateway.kt` | UMP implementation of `ConsentGateway` | VERIFIED | 240 lines, `requestConsentInfoUpdate`, fail-open `Unavailable` branches present |
| `app/.../platform/ads/AdMobAdGateway.kt` | Real GMA Next-Gen rewarded gateway | VERIFIED | 208 lines, `OnUserEarnedRewardListener` present, bound in `PlatformModule` (`bindAdGateway(impl: AdMobAdGateway)`) |
| `app/.../platform/ads/RewardedAdSlot.kt` | Single-slot ad cache | VERIFIED (with review caveat) | 207 lines; functionally exercised on-device 4x; review flags WR-03 (take-semantics not enforced on `await()`) — untested edge case, not observed on device |
| `app/.../ui/game/AdOfferDialog.kt` | Themed opt-in dialog | VERIFIED | 278 lines, `AD_OFFER_DIALOG_TEST_TAG` present, wired into `GameScreen.kt` (`state.pendingAdOffer?.let { AdOfferDialog(...) }`) |
| `app/.../ui/settings/PrivacyLinks.kt` | Compile-time policy URL constant | VERIFIED | `const val POLICY_URL = "https://ermanergoz.com/policies/..."`, consumed by `SettingsScreen.kt` |
| `.../09-DEVICE-EVIDENCE.md` | Itemised device-verification results | VERIFIED | 14 checklist items, 11 PASS + 1 PASS-with-named-gap + 1 FAIL (pass-by-decision D-20) + 1 NOT EXERCISED (test-inventory limitation, backfilled by unit+structural evidence) |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `MainActivity` | `ConsentGateway.bootstrap()` | field injection, called before `setContent` | WIRED | Grep-confirmed in code; device evidence items 1-3 confirm behavior |
| `UmpConsentGateway.initializeAdsIfPermitted` | `MobileAds.initialize` | `AtomicBoolean.compareAndSet` guard, background dispatcher | WIRED | Present in code; device evidence item 3 confirms ordering |
| `AdMobAdGateway.requestRewarded` | `RewardOutcome.Granted` | `earned` flag set only in `OnUserEarnedRewardListener` | WIRED | Confirmed by review IN-04 (structural, grep-pinned) and device item 9a (4x) |
| `PlatformModule` | `AdMobAdGateway` | `@Binds bindAdGateway(impl: AdMobAdGateway): AdGateway` | WIRED | Confirmed in `PlatformModule.kt`; `NoOpAdGateway` intentionally left unbound (reference behavior, still tested) |
| `GameScreen.kt` | `GameUiState.pendingAdOffer` | conditional `AdOfferDialog` rendering | WIRED | `state.pendingAdOffer?.let { AdOfferDialog(...) }` confirmed at `GameScreen.kt:257-258` |
| `GameViewModel.onUndo/onHintRequested` | `adGateway.requestRewarded` | `freezeClockForAd()`/`unfreezeClockAfterAd()` wrapping | WIRED | Confirmed at `GameViewModel.kt:1294-1313`; device item 12 confirms behavior |
| `SettingsViewModel` | `ConsentGateway.privacyOptionsRequired` | third collector in init block | WIRED | Confirmed at `SettingsViewModel.kt:70-71`; device item 5/6 confirm region gating |
| `SettingsScreen.kt` | `PrivacyLinks.POLICY_URL` | `Intent(ACTION_VIEW)` wrapped in `runCatching` | WIRED | Confirmed at `SettingsScreen.kt:247`; device item 7 confirms |

### Requirements Coverage

| Requirement | Source Plan(s) | Description | Status | Evidence |
|---|---|---|---|---|
| CONS-01 | 09-05, 09-11 | Consent refreshed each launch, form shown before any ad request | SATISFIED | Device evidence items 1-3 PASS |
| CONS-02 | 09-05, 09-10, 09-11 | SDK initialized only after consent, off main thread, no ad while consent absent | SATISFIED | Device evidence item 3 PASS; code confirms background dispatcher |
| CONS-03 | 09-02, 09-05, 09-07, 09-11 | Settings entry appears only when consent rules require it | SATISFIED | Device evidence items 5, 6 PASS (EEA shows both rows; non-EEA shows only policy row) |
| CONS-04 | 09-02, 09-05, 09-08, 09-10 | Consent-flow failure falls back to previous session status, serves no ads, doesn't block play | SATISFIED | Code: `AdsReadiness.Unavailable` fail-open branches in `UmpConsentGateway`; unit-tested in `GameViewModelAllowanceTest`. Note: decline (not flow-failure) still allows non-personalized offers per D-20 — see override, does not affect this requirement's literal definition. |
| ADS-01 | 09-02, 09-03, 09-06, 09-08, 09-09, 09-10 | Explicit opt-in dialog naming reward, decline possible, ad never auto-plays | SATISFIED | Device evidence item 8 PASS |
| ADS-02 | 09-01, 09-08 | Free allowance of hints/undos per level before any ad offered | SATISFIED | `AssistAllowanceLimits` (3 hints/5 undos), 13 passing domain tests, device item 8 PASS |
| ADS-03 | 09-02, 09-03, 09-06, 09-08, 09-09, 09-10 | Assist granted anyway with brief explanation when ad unloadable | SATISFIED | Device evidence item 13 PASS (airplane mode fail-open) |
| ADS-04 | 09-08, 09-09 | Player can accept rewarded offer from rescue banner | SATISFIED | Device evidence item 10 PASS; code confirms shared handler, no ad affordance on banner |
| ADS-05 | 09-08, 09-10, 09-11 | Reward granted only when SDK reports earned | SATISFIED | Positive path: device item 9a PASS (4x). Negative path (Declined): unit + structural evidence only — Google's test inventory cannot produce an early dismiss (item 9b, documented limitation, not a code gap) |
| ADS-06 | 09-06 | "No winning move" hint never charged | SATISFIED | Device evidence item 11 PASS; `onHintRequested` inversion confirmed in code |
| ADSF-01 | 09-04, 09-11 | Clock frozen for full ad duration, resumes on dismiss | SATISFIED | Device evidence item 12 PASS (two independent wall-clock measurements) |
| REL-05 | 09-07, 09-11 | Privacy policy openable from Settings on every install | SATISFIED | Device evidence item 7 PASS |

No orphaned requirements — all 12 IDs in REQUIREMENTS.md's Phase 9 mapping are claimed by at least one plan.

### Anti-Patterns / Known Issues (advisory, non-blocking)

Per 09-REVIEW.md (0 critical, 5 warnings, 5 info) — all warnings are edge-case defects in code paths not exercised during the extensive device-verification session (14 checklist items, 4 real ad watches, airplane-mode fail-open test), and none contradict a verified must-have truth under tested conditions. Listed here for visibility, not as phase-goal gaps:

| File | Issue | Severity | Impact if triggered |
|------|-------|----------|---------------------|
| `GameViewModel.kt:1294` | `onAcceptAdOffer` not re-entrancy guarded — a same-frame double-tap can double-freeze the clock (permanently inflating elapsed time) and double-grant an assist | WARNING (WR-01) | Would corrupt elapsed-time/score on a specific double-tap race; not observed in 14-item device session |
| `AdMobAdGateway.kt:156-189` | `show()` call has no exception containment — a synchronous SDK throw could crash mid-gameplay instead of degrading to `Unavailable` | WARNING (WR-02) | App crash under an unexercised SDK-internal error state; contradicts the class's own "every failure degrades" KDoc |
| `RewardedAdSlot.kt:107` | `await()` does not clear `held` — a concurrent second caller (enabled by WR-01) could receive the same ad instance | WARNING (WR-03) | Second show() attempt on an already-shown ad; compounds WR-01 |
| `UmpConsentGateway.kt:108-163` | `bootstrap()` has no in-flight/once-per-Activity guard — a rotation during the consent form could re-run the bootstrap sequence | WARNING (WR-04) | Duplicate consent-info requests on rotation during the form; not observed in this session |
| `GameViewModel.kt:430-445` | A game restored with an already-exhausted counter does not trigger the D-09 preload | WARNING (WR-05) | First post-resume offer cold-loads instead of using a preloaded ad; still falls open correctly on slow network |
| Multiple | Zero unit-test coverage on the three real-SDK-facing classes (`AdMobAdGateway`, `RewardedAdSlot`, `UmpConsentGateway`) | INFO (IN-04) | The state machines behind WR-01..03 are extractable/testable but currently untested |

No `TBD`/`FIXME`/`XXX` debt markers found in any Phase 9 file.

### Data-Flow Trace (Level 4)

Not applicable in the traditional sense (no list/dashboard rendering dynamic collection data). The relevant "data flow" for this phase is the consent/allowance state machine, which was traced above (Key Link Verification) and confirmed both statically (code) and dynamically (device evidence, wall-clock measurements, TCF key inspection).

### Behavioral Spot-Checks

Skipped in favor of Step 7c (Probe Execution) — see below. This phase's runnable-code checks are exhaustively covered by 09-DEVICE-EVIDENCE.md's 14-item on-device session plus this verification run's independent re-execution of the offline gate (tests + detekt + ktlint), which is stronger evidence than a scripted spot-check would add.

### Probe Execution

No `scripts/*/tests/probe-*.sh` files exist in this repository, and neither the PLAN files nor SUMMARY files for Phase 9 reference any probe script. Step 7c is SKIPPED (no probes declared or discovered). Independent verification instead re-ran the actual gates this phase defines success by:

| Check | Command | Result | Status |
|-------|---------|--------|--------|
| Unit test suite | `./gradlew :app:testDebugUnitTest :domain:test --offline` | BUILD SUCCESSFUL, all Phase-9 test classes 0 failures (13+13+5+8+8+8 = 55 Phase-9-specific tests, 611 total across both modules) | PASS |
| Static analysis | `./gradlew app:detekt domain:detekt --offline` | BUILD SUCCESSFUL | PASS |
| Lint/format | `./gradlew ktlintCheck --offline` | BUILD SUCCESSFUL | PASS |
| Debt markers | `grep -rn "TBD\|FIXME\|XXX"` over Phase-9 files | No matches | PASS |

This is an independent regression check run during verification (not a re-statement of SUMMARY/DEVICE-EVIDENCE claims) — all commands were executed in this session against the current commit.

### Human Verification Required

None. Every truth requiring human/device observation (consent form rendering, real ad presentation, wall-clock timing, Settings region gating) was already itemised and independently exercised in `09-DEVICE-EVIDENCE.md` on 2026-09-02, on an emulator (user-approved deviation from physical hardware, documented in that file's preamble). No further human action is needed to close Phase 9's goal.

Two items remain explicitly out of this phase's scope, deferred by design (not gaps):
- The AdMob account's own consent message (vs Google's test-publisher message) can only be exercised in a release-signed build — owned by Phase 14 (REL-02), per 09-DEVICE-EVIDENCE.md Task 1.
- `RewardOutcome.Declined` on real ad inventory (item 9b) is not producible on Google's test creative; the negative branch is proven at the unit/structural level only. This is a permanent characteristic of test inventory, not a Phase 9 action item.

### Gaps Summary

No blocking gaps. One plan-level must-have (09-11 truth #2, "decline suppresses all ad offers") failed as literally written against device evidence, but is covered by an explicit user product decision (D-20, 2026-09-02) that the app's actual behavior — declining TCF consent does not suppress non-personalized rewarded offers — is correct and intended. This is recorded as an override above, not a gap. REQUIREMENTS.md's own CONS-04 definition (consent-flow *failure*, not decline) is unaffected and independently satisfied.

Five non-blocking code-review warnings (WR-01..05) document latent defects in untested edge cases (double-tap races, SDK exception containment, rotation re-entrancy). None were triggered during the extensive device-verification session and none contradict a verified must-have truth. They are flagged for future hardening but do not block Phase 9's goal achievement.

---

_Verified: 2026-09-02_
_Verifier: Claude (gsd-verifier)_
