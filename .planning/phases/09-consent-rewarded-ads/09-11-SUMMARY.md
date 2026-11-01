---
phase: 09-consent-rewarded-ads
plan: 11
subsystem: verification
tags: [device-verification, ump, consent, rewarded-ads, admob, evidence, emulator]
status: complete

# Dependency graph
requires:
  - phase: 09-consent-rewarded-ads (plan 09-05)
    provides: UmpConsentGateway — the CONS-01/02/04 sequence and the debug DEBUG_GEOGRAPHY_EEA hook
  - phase: 09-consent-rewarded-ads (plan 09-07)
    provides: the two Settings privacy rows (CONS-03, REL-05) and the live policy URL
  - phase: 09-consent-rewarded-ads (plan 09-08)
    provides: the assist funnel, the free allowance and the opt-in dialog callbacks
  - phase: 09-consent-rewarded-ads (plan 09-10)
    provides: AdMobAdGateway + RewardedAdSlot — the real rewarded path under test
provides:
  - "09-DEVICE-EVIDENCE.md — fourteen itemised device results with named gaps"
  - "A traced FAIL on item 4 (decline ⇒ ads still offerable) routed to gap closure"
  - "A NOT-EXERCISED record for ADS-05's Declined half, with the test-inventory reason"
  - "ADSF-01 confirmed on real ad presentations by two independent wall-clock measurements"
affects: [09-verification, 10-interstitials, 14-release-readiness]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Device verification driven entirely over adb (install, uiautomator, input, screencap, logcat) rather than by hand"
    - "Wall-clock differencing against the AdActivity window lifecycle as the ADSF-01 measurement, instead of eyeballing two timer readings"
    - "Probe the consequence, not the callback: Granted vs Declined distinguished by whether the NEXT assist was free"

key-files:
  created:
    - .planning/phases/09-consent-rewarded-ads/09-DEVICE-EVIDENCE.md
  modified: []

key-decisions:
  - "Item 4 is recorded as a FAIL against the plan's expectation and NOT fixed — suppressing offers for a player who declined TCF consent while Google's canRequestAds() permits limited ads is a Rule-4 product decision, not a bug fix"
  - "RESEARCH A5 is recorded as PARTIALLY closed, not closed: a debug build resolves Google's test AdMob app ID, so the form observed is the test publisher's message and the developer's own message has never rendered"
  - "Item 2's consentInformation.reset() was substituted with `pm clear` — a strictly stronger reset — and the substitution is recorded rather than glossed"
  - "Item 6 required a temporary uncommitted DEBUG_GEOGRAPHY_OTHER patch; it was reverted and the revert re-verified by rebuilding and re-observing the EEA form before anything was committed"
  - "Item 14 is recorded as structurally passing with the dashboard reading explicitly outstanding, rather than as a plain pass"

patterns-established:
  - "Re-measure before reporting a failure: item 12 looked like a clean ADSF-01 failure on a first mis-anchored reading and is in fact a pass — the instrumented re-run is what separated them"
  - "An evidence artifact names whose message/inventory it actually exercised, not just that something rendered"

requirements-verified: [CONS-01, CONS-02, CONS-03, ADS-01, ADS-02, ADS-03, ADS-04, ADS-06, ADSF-01, REL-05]
requirements-partially-verified: [ADS-05, CONS-04]

# Metrics
duration: ~55 min
completed: 2026-09-02
---

# Phase 9 Plan 11: On-Device Consent & Rewarded-Ad Verification Summary

**Fourteen checks driven over adb against a debug build on `emulator-5554`: eleven pass outright, one passes with its gap named, one fails against the plan's expectation for a traced and non-obvious reason, and one could not be produced on Google's test inventory at all — all four outcomes recorded per item in `09-DEVICE-EVIDENCE.md` rather than collapsed into a global approval.**

## Performance

- **Duration:** ~55 min
- **Tasks:** 3 of 3 complete
- **Files created:** 1 (`09-DEVICE-EVIDENCE.md`)
- **Files modified:** 0 — no production code was changed by this plan
- **Gate:** `./gradlew check --offline` **green, 11m 8s, 431 tests / 0 failures**; `:app:assembleDebug --offline` green

## Task 1 — AdMob console prerequisites (D-18 / RESEARCH A5)

Recorded verbatim, per the task's `<action>`:

| Prerequisite | State |
|---|---|
| GDPR consent message published for `com.erman.pegsolitarie` | **Reported published** by the developer (relayed to this run at start: "D-18 is RESOLVED"). Not independently observable — see below. |
| Rewarded ad unit exists | **Not established.** Not reported, and not observable from a debug build, which resolves Google's test rewarded unit regardless. |
| App registered in AdMob at all | **Not established**, same reason. |

**RESEARCH Assumption A5 is marked PARTIALLY CLOSED, not closed**, and this is the single most important thing in this summary that a later reader must not skim:

UMP fetches its message using the AdMob **application ID in the manifest**, and a debug build resolves that to Google's demo publisher `ca-app-pub-3940256099942544~3347511713` (Phase 6 D-05/D-06). The form that rendered in items 1, 2 and 5 is titled *"Welcome to Publisher Test Ads"* — **Google's test-publisher message, not this app's**. Confirmed in the SDK's own config request: `…&client=ca-app-pub-3940256099942544&admob_appcc=3347511713`. Nor could the real one be substituted: `local.properties` on this machine holds **no** release ad IDs at all, so the production application ID is not available here in any form.

So: the app's consent *sequence* is fully exercised and correct, and the developer's own consent *message* has never rendered anywhere. D-18 closes on report; the production message is first exercised in a release-signed build, which is **REL-02, Phase 14**.

**Task 2 was nonetheless safe to start.** The gate's purpose is to prevent a CONS-01 pass that a missing message would have faked. A real server-fetched form *did* render on a fresh install before Home, so CONS-01 was genuinely exercised rather than silently no-op'd — T-09-52's failure mode did not occur. What narrowed is only *whose* message it was, and that narrowing is recorded in both artifacts.

## Task 2 — the fourteen device checks

Device: `emulator-5554` / `sdk_gphone16k_arm64`, **Android 16 (API 36)**, build `BE4B.251210.005`, **debug** build of `com.erman.pegsolitarie` 3.0.0. **Emulator, not physical hardware** — no physical device was available; a user-approved deviation, stated in the evidence file's preamble and repeated in its gaps section, exactly as STATE.md's Phase-7 lesson requires.

| # | Req | Result |
|---|---|---|
| 1 fresh install, consent first | CONS-01 | **PASS** |
| 2 refreshed every launch; reset replays | CONS-01 | **PASS** |
| 3 no ad request before consent | CONS-02 | **PASS** |
| 4 decline ⇒ free, no dialog, no snackbar | CONS-04 / D-12 | **FAIL** |
| 5 both privacy rows in a consent region | CONS-03 | **PASS** |
| 6 policy row only outside one | CONS-03 | **PASS** |
| 7 the policy opens | REL-05 | **PASS** |
| 8 allowance then offer; "No thanks" inert | ADS-01/02 | **PASS** |
| 9a watch to completion ⇒ granted | ADS-05 | **PASS** |
| 9b early dismiss ⇒ nothing granted | ADS-05 | **NOT EXERCISED** |
| 10 rescue banner undo | ADS-04 | **PASS** |
| 11 truthful "no winning move" is free | ADS-06 | **PASS** |
| 12 clock frozen across a full ad | ADSF-01 | **PASS** |
| 13 airplane-mode fail-open | ADS-03 | **PASS** |
| 14 zero impressions | invalid traffic | **PASS structurally; dashboard NOT READ** |

Full observations, log excerpts and measurements are in `09-DEVICE-EVIDENCE.md`. The four that need naming here:

**Item 4 — FAIL, and it is a real finding.** After tapping **"Do not consent"** (UMP recorded `IABTCF_PurposeConsents=00000000000` — all eleven denied), the ads SDK initialized anyway, and the fourth hint raised the opt-in dialog. Traced rather than guessed: `initializeAdsIfPermitted()` gates on `consentInformation.canRequestAds()`, which returned `true` because Google's framework permits **limited / non-personalised** serving on the legitimate-interest bases the same form recorded (`IABTCF_PurposeLegitimateInterests=01000011111`). Both halves of the acceptance criterion were checked explicitly: **the dialog did appear; no snackbar appeared.** See Deviations for why this was reported rather than fixed.

**Item 9b — NOT EXERCISED, with the reason.** `RewardOutcome.Declined` could not be produced: the test creative fires `onUserEarnedReward` within ~2 s and exposes no close control before its post-reward end card. Dismissal was attempted at **t+2 s, t+8 s and t+33 s**; each was *probed* rather than assumed — the next hint request was free every time, meaning the allowance had refilled, meaning the outcome was `Granted`. This is a property of Google's test inventory, not of the app.

**Item 12 — PASS, with both readings, and it nearly went the other way.** Two independent measurements against device wall-clock: (i) anchor `04:59` at 03:05:48, ad on screen 03:06:18.49→03:06:58.05 (**39.6 s** from the `AdActivity` window lifecycle), reading `05:42` at 03:07:09 — predicted-if-frozen **341.4 s**, predicted-if-running 381 s, observed **342 s**; (ii) `08:30` immediately before accept, `08:31` after a ~30 s presentation — **+1 s across the whole ad**. It resumed and kept counting afterwards in both cases.

**Item 14 — the AdMob impression count was not read**, because that needs the developer's console. What replaces it is stronger than a glance at a dashboard: this build resolves **only** Google's demo-publisher IDs, every ad carried the **"Test Ad"** watermark, and the SDK's config request names `client=ca-app-pub-3940256099942544`. No request in the session reached the developer's account, so it cannot have recorded an impression. Recorded as *structurally passing, dashboard outstanding* rather than as a plain pass.

## Task 3 — the evidence artifact and the gate

`.planning/phases/09-consent-rewarded-ads/09-DEVICE-EVIDENCE.md`, in the shape of Phase 8's `08-BOOK-EVIDENCE.md`: preamble with the hardware caveat and full device table, the Task 1 prerequisite results, the fourteen-row table (each row naming its requirement ID and carrying a real observation), two written-up findings, a method note on the item-6 patch, a seven-point **"What this run did not prove"**, an `[ASSUMED]`-items table, and the gate output.

| Gate | Result |
|---|---|
| `./gradlew check --offline` | **BUILD SUCCESSFUL in 11m 8s** |
| `:app:testDebugUnitTest` | **431 tests, 0 failures, 0 skipped** |
| `./gradlew :app:assembleDebug --offline` | **BUILD SUCCESSFUL** (up-to-date against the exact APK that was driven) |
| `git status` on `UmpConsentGateway.kt` | clean — the item-6 patch reverted and uncommitted |

## Task Commits

| Task | Name | Commit |
|------|------|--------|
| 1 | AdMob console prerequisites | no file change by design — recorded in this SUMMARY and in the evidence file |
| 2 | On-device verification | no file change by design — recorded in this SUMMARY and in the evidence file |
| 3 | Commit the evidence and run the final gate | `bfb0c6e` (docs) |

## Deviations from Plan

### 1. [Reported, not worked around] Item 4 contradicts the plan's must-have truth #2

- **Found during:** Task 2, item 4
- **Issue:** the plan asserts *"Declining consent leaves every assist free with no dialog and no snackbar, exactly like v1.0"*. On the device, declining leaves ads offerable and the dialog does appear.
- **Action taken:** none in code. Recorded as a FAIL with the mechanism traced to `canRequestAds()`'s documented semantics, and routed to phase verification / gap closure — which is what the plan's own `<action>` demands ("Report failures rather than working around them: a failed item routes to gap closure, which is the intended outcome, not a setback").
- **Why not auto-fixed under Rule 1 or 2:** the code does exactly what its KDoc and D-12 say — it defers to the SDK's authoritative "may we request ads?" answer and never infers consent client-side. REQUIREMENTS.md's CONS-04 is about the consent flow **failing**, not about a decline, and it is not contradicted. Changing `rewardedOfferable` to additionally require TCF Purpose-1 consent trades ad revenue for a stricter reading of the player's intent — a **Rule 4** product decision, not a defect repair. There is no GDPR exposure either way: the decline was recorded correctly and completely, and ads served in that state are non-personalised by construction.
- **The decision needed is one sentence:** does `rewardedOfferable` gate on `canRequestAds()` (today) or additionally on Purpose-1 consent? If the latter, `AdMobAdGateway.rewardedOfferable` is the single place it changes; if the former, the plan's item 4 and must-have truth #2 should be corrected to match UMP's semantics.

### 2. [Method substitution, recorded] `consentInformation.reset()` → `pm clear`

- **Found during:** Task 2, item 2
- **Issue:** the plan's item 2 says to invoke `consentInformation.reset()` "via the debug path". No such debug path exists in the app, and adding one would have been production code written solely to test itself.
- **Action taken:** used `adb shell pm clear`, which clears the entire TCF key set plus all app storage — a strictly stronger first-install replay than `reset()`. Performed twice (run start, and again after the item-6 rebuild); the form rendered both times.
- **Recorded in:** the evidence file's item 2 row, explicitly as a substitution rather than silently.

### 3. [Temporary, reverted, uncommitted] The item-6 geography patch

- **Found during:** Task 2, item 6
- **Issue:** item 6 needs a build that is **not** in a simulated consent region, and the debug build hard-codes `DEBUG_GEOGRAPHY_EEA` (plan 09-05).
- **Action taken:** a one-line local edit to `DEBUG_GEOGRAPHY_OTHER`, rebuild, install, `pm clear`, observe, then `git checkout --` to revert. The revert was verified two ways — `git status` clean for that file, and a fresh EEA build reinstalled and cold-launched to confirm the form renders again (which doubles as item 2b's second replay).
- **Files modified in any commit:** **none.** The shipped source is byte-identical to what plan 09-05 committed.

### 4. [Self-correction] Item 12 was initially mis-read as a failure

- **Found during:** Task 2, item 12
- **Issue:** the first measurement compared a timer reading taken ~25 s before the accept tap against one taken after the ad, which made the clock look like it had run for the ad's whole duration. That reading would have recorded a false ADSF-01 failure.
- **Action taken:** re-measured with device wall-clock stamps around every step and the `AdActivity` window lifecycle as the ad's true span, then repeated it on a second ad. Both re-measurements show the freeze working to within a second.
- **Why it is written down:** a false FAIL in an evidence file is exactly as damaging as a false PASS (T-09-53), and the failure mode here — anchoring on a stale reading — is easy to repeat.

---

**Total deviations:** 4 — one reported-not-fixed finding, one recorded method substitution, one temporary reverted patch, one self-corrected measurement. No Rule 1/2/3 auto-fixes were needed, and no production code was touched.

## Issues Encountered

- **The first back-press into a rewarded ad is swallowed.** A `KEYCODE_BACK` sent within ~0.5 s of `AdActivity` becoming resumed does nothing; the second lands. Worth knowing for anyone scripting ad interactions over adb.
- **A cached rewarded ad plays fine in airplane mode.** Item 13 needed two attempts: the first accept after enabling airplane mode showed the *already warm* ad from the slot. The genuine fail-open only appears once the slot is cold — which is itself a small confirmation that `RewardedAdSlot` really is holding and re-warming an ad.
- **The clock does not tick on a resumed game until the first move.** `startTimerIfNeeded` marks on the first jump, so a game resumed from a snapshot displays its restored elapsed value as a constant until the player moves — and `freezeClockForAd()` correctly no-ops in that window (`startMark == null`). This is documented behaviour, but it briefly confounded an item-12 reading.
- **Tapping the hint control while the opt-in dialog is up hits the scrim and dismisses it.** Expected (D-06), but it silently consumes a step in a scripted sequence.

## Known Stubs

None introduced. `AdMobAdGateway.showInterstitial()` remains the deliberate Phase-10 no-op that plan 09-10 documented; it has no caller and was correctly not exercised here.

## Threat Flags

None. This plan created one Markdown file and changed no code, so it introduced no network endpoint, auth path, file access pattern or schema change. On the register the plan itself carries: **T-09-51** (invalid traffic) is addressed structurally and recorded with its gap; **T-09-52** (a CONS-01 pass without a published message) did not occur — a real form rendered — but the related risk that the *wrong* message rendered is now recorded explicitly; **T-09-53** (overstated evidence) is addressed by the per-item format, the two non-passing rows and the seven-point gaps section; **T-09-54** (decline path) is the one that produced Finding 1; **T-09-55** (early dismiss) is the one recorded as NOT EXERCISED with its reason.

## Concerns for the Phase Verifier

1. **Item 4 needs a decision before the phase closes.** It is the only item that contradicts a stated must-have truth, and the fix — if one is wanted — is a single condition in `AdMobAdGateway.rewardedOfferable`. Read Finding 1 in the evidence file before deciding; the answer is not obvious, and the plan's expectation may be the thing that is wrong.
2. **ADS-05 is half-verified on real inventory.** The positive half (earned ⇒ granted ⇒ refilled) is confirmed four times over; the negative half is proven only at the seam (09-08's fakes) and structurally (09-10's single `earned = true` grep). Marking ADS-05 fully complete on this evidence would overstate it.
3. **RESEARCH A5 must not be recorded as closed.** The consent-message half rests on the developer's report, and the production message will not render until Phase 14's release-signed build.
4. **Everything here is emulator-grade.** This is the same limitation STATE.md already tracks for Phase 7 audio; the two items where it matters most are 12 (clock freeze under real-device scheduling) and 13 (the `ACCEPT_WAIT_MS` bound against a *slow* network rather than an instantly-failing one).
5. **RESEARCH A1 (the ~1 h rewarded-ad expiry) is still assumed.** No ad in this session sat unshown for anywhere near `REWARDED_AD_MAX_AGE_MS`, so neither the figure nor the discard path was exercised.

## Next Phase Readiness

- **Phase 10 (interstitials)** inherits a rewarded path confirmed working end to end on real test inventory, a consent bootstrap confirmed to gate SDK initialization, and a clock-freeze bracket confirmed to hold across a real fullscreen presentation — the three things an interstitial will reuse.
- **Phase 14 (release readiness)** inherits four concrete obligations from this file: render the **developer's own** consent message for the first time, confirm the rewarded ad unit exists, read the AdMob dashboard for impressions, and exercise a release-signed build (REL-02). All four are named in the evidence file's "What this run did not prove".
- **Phase verification** inherits one open decision (item 4) and one requirement that should not be ticked off as fully verified (ADS-05).

## Self-Check: PASSED

- `.planning/phases/09-consent-rewarded-ads/09-DEVICE-EVIDENCE.md` — **FOUND**
- Commit `bfb0c6e` — **FOUND** in `git log`
- `git diff` against production source — **empty**; the item-6 patch is reverted and in no commit
- `STATE.md` and `ROADMAP.md` — **untouched**; the orchestrator owns those

---
*Phase: 09-consent-rewarded-ads*
*Plan: 11 — complete*
*Date: 2026-09-02*
