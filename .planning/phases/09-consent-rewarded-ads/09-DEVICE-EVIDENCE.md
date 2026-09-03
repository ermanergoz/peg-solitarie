# Phase 9 — Consent & Rewarded-Ad Device-Verification Evidence

**Purpose:** record, per item, what was actually observed on a running Android instance for the
behaviours Phase 9 cannot assert from the JVM suite — the UMP consent flow (CONS-01, CONS-02), the
two Settings privacy surfaces (CONS-03, REL-05), a real Google **test** rewarded ad end to end
(ADS-01..ADS-06), and the clock freeze across a full ad watch (ADSF-01).

**Why this file exists in this shape.** STATE.md records Phase 7's manual audit as a real weakness:
it was emulator-only and **globally approved** rather than itemised. This run is the correction. Each
of the fourteen checklist items below carries its own result and its own observation, two items are
recorded as **not passing**, and § *What this run did not prove* names every gap plainly rather than
omitting it. A green evidence file over a partly-red device run would be worse than no file at all
(T-09-53).

---

## Preamble — the physical-hardware caveat

**Every observation below was made on an emulator, not on physical hardware.** No physical device
was available for this run; running against `emulator-5554` was a deliberate, user-approved
deviation. This is the *same* limitation STATE.md flags for Phase 7 and for the pending
`2026-08-28-confirm-audio-cues-on-physical-hardware` todo, and it is not silently repeated here: it
is stated once, up front, and repeated in § *What this run did not prove*.

What the emulator does **not** weaken for this phase: UMP, the GMA Next-Gen SDK, WebView-rendered
consent forms and rewarded-video presentation are the same code paths on both, and an emulator is an
AdMob **test device by default**, which is exactly the property the invalid-traffic guard (item 14)
depends on. What it does weaken: real-network timing, real-fill behaviour, and anything sensitive to
hardware audio/graphics. The two items where that matters are called out individually.

| Field | Value |
|---|---|
| Device | `emulator-5554` — `sdk_gphone16k_arm64` (Google Play services system image, arm64) |
| Physical or emulator | **Emulator** (no physical hardware available — user-approved) |
| Android version | **16** (API level 36), build `BE4B.251210.005` |
| Build type | **debug**, `com.erman.pegsolitarie` versionName `3.0.0` (versionCode 3) |
| Ad IDs in the build | Google's public test IDs — app `ca-app-pub-3940256099942544~3347511713`, rewarded `ca-app-pub-3940256099942544/5224354917` |
| Consent geography | `ConsentDebugSettings` + `DEBUG_GEOGRAPHY_EEA` (debug-only, already present from plan 09-05) |
| Date | **2026-09-02** |
| Branch | `feature/phase-09-consent-rewarded-ads` |
| Plan | 09-11, wave 7 |

---

## Task 1 — AdMob console prerequisites (D-18 / RESEARCH A5)

| Prerequisite | State | How established |
|---|---|---|
| GDPR consent message published for `com.erman.pegsolitarie` | **Reported published** by the developer | Developer's report, relayed to this run at start ("D-18 is RESOLVED — the GDPR consent message is published"). **Not independently observable from this build — see below.** |
| Rewarded ad unit exists for the app | **Not established** | Not reported and not observable from a debug build, which resolves Google's test rewarded unit regardless. Release-readiness item, not a blocker for Task 2. |
| App registered in AdMob at all | **Not established** | Same reason. Phase 6's build plumbing resolves debug IDs to Google's test values, so the app builds and runs green whether or not it was ever registered. |

### RESEARCH Assumption A5: **PARTIALLY CLOSED — the consent-message half rests on report, not observation**

The honest statement, which the plan's own threat T-09-52 exists to force:

**A debug build cannot exercise the developer's own consent message.** UMP fetches its message using
the AdMob **application ID in the manifest meta-data**, and this build resolves
`@string/admob_app_id` to Google's demo publisher `ca-app-pub-3940256099942544~3347511713` (Phase 6
D-05/D-06). The form that rendered in items 1, 2 and 5 below is therefore titled *"Welcome to
Publisher Test Ads"* — it is **Google's test-publisher message, not this app's**. Confirmed in the
`getconfig/pubsetting` request logged by the SDK:
`…?app_name=com.erman.pegsolitarie&…&client=ca-app-pub-3940256099942544&admob_appcc=3347511713`.

Nor could the real message be exercised by substituting the production ID: `local.properties` on this
machine contains **no** `ADMOB_APP_ID` / `ADMOB_REWARDED_AD_UNIT_ID` / `ADMOB_INTERSTITIAL_AD_UNIT_ID`
entries, so the production application ID is not available here at all.

**What this run therefore does and does not establish about D-18:**

- **Established (observed):** the app's *own* consent sequence is correct and complete — it refreshes
  consent unconditionally at cold start, renders a real server-fetched UMP form before any ad
  request, records the player's choice in the IAB TCF keys, publishes the privacy-options
  requirement, and re-opens the form on demand from Settings. That is the entire app-side surface of
  CONS-01/02/03.
- **Not established (not observable here):** that the *developer's own* published message exists and
  is published rather than draft. Task 1's gate is therefore closed **on the developer's report**,
  and the first render of the production message will happen in a release-signed build — which is
  REL-02, owned by Phase 14. Anyone reading this file later should treat "the production consent
  message renders" as **unverified**, and verify it in Phase 14.

**Task 2 was not started against an unpublished message.** The gate's substantive purpose — never
record a CONS-01 pass that a missing message would have faked — is satisfied more strongly than the
gate asked: a form *did* render, on a fresh install, before Home, so CONS-01 was genuinely exercised
rather than silently no-op'd. What is scoped down is only *whose* message rendered.

---

## Task 2 — the fourteen device checks

Legend: **PASS** = observed. **FAIL** = observed, and the observation contradicts the plan's stated
expectation. **NOT EXERCISED** = could not be produced, with the reason given.

| # | Check | Req | Result | Observation |
|---|---|---|---|---|
| 1 | Fresh install, consent first | CONS-01 | **PASS** | `pm clear` + cold launch: the UMP form rendered over the tutorial surface within ~2 s and before any ad request. The app process' own logcat for that launch contains **zero** `GoogleMobileAds*` lines up to the moment of the form — the ads SDK had not been touched. UMP wrote `IABTCF_gdprApplies=1` and reported `load_complete {"status":"ok"}`. |
| 2 | Refreshed every launch; reset replays a first install | CONS-01 | **PASS** | (a) With consent already obtained, `am force-stop` + relaunch showed **no** re-prompt and went straight to Home, while UMP still ran (47 `UserMessagingPlatform` lines, one `load_complete`) and the ads SDK still initialized (33 `GoogleMobileAds*` lines) — refreshed, not skipped, and ads still work. (b) Replay: `pm clear` + relaunch rendered the form again, twice (at run start and again after the item-6 rebuild). **Substitution recorded:** the app exposes no debug `consentInformation.reset()` hook, so `pm clear` was used — a strictly stronger reset (it clears the whole TCF key set, which is what `reset()` does plus the rest of app storage). |
| 3 | No ad request before consent | CONS-02 | **PASS** | While the form was on screen, `logcat --pid=<app>` contained no ad-request, no `MobileAds` initialization and no ad UI anywhere. The first `GoogleMobileAds` line in the whole process appears **after** the form was answered. This is the ordering `UmpConsentGateway` documents as load-bearing, observed rather than inferred. |
| 4 | Decline consent ⇒ every assist free, no dialog, no snackbar | CONS-04 / D-12 | **FAIL** (against the plan's expectation; see § *Finding 1*) | Tapped **"Do not consent"**; UMP recorded `IABTCF_PurposeConsents=00000000000` (all eleven purposes denied) — the decline was genuinely registered. The ads SDK then **initialized anyway**, because UMP's `canRequestAds()` returned `true` (legitimate-interest bases remained: `IABTCF_PurposeLegitimateInterests=01000011111`). Playing on: the first three hints were free, and the **fourth raised the opt-in dialog** — *"Out of free hints … Watch a short ad for 3 more?"*. The plan requires no ad-related UI at all on this path. **Both halves explicitly checked: the dialog DID appear; no snackbar appeared.** |
| 5 | Settings in a consent region shows both rows; manage opens the form in-app | CONS-03 | **PASS** | Under `DEBUG_GEOGRAPHY_EEA`, Settings → Privacy showed **both** "Privacy policy" and "Manage privacy choices". Tapping the latter rendered the UMP privacy-options form, and `topResumedActivity` stayed `com.app.pegsolitaire.MainActivity` throughout — **in-app**, no Activity hand-off. Answering it wrote the new choice (`IABTCF_PurposeConsents` flipped `000…`→`111…` when Consent was chosen). |
| 6 | Settings outside a consent region shows only the policy row | CONS-03 | **PASS** | Rebuilt with `DEBUG_GEOGRAPHY_OTHER` (temporary, uncommitted — see § *Method note*), `pm clear`, cold launch: **no** consent form appeared and UMP wrote `IABTCF_gdprApplies=0`. Settings → Privacy showed **only** "Privacy policy"; "Manage privacy choices" was absent. The patch was reverted (`git checkout --`) and the EEA build restored and re-verified before anything was committed. |
| 7 | The privacy policy opens | REL-05 | **PASS** | "Privacy policy" launched Chrome (`topResumedActivity` = `com.android.chrome/…ChromeTabbedActivity`) at `ermanergoz.com/policies/…`; the page rendered, headed **"Privacy Policy — Last updated: September 2, 2026"**, i.e. the revision plan 09-07 required to clear T-09-31. |
| 8 | Three free hints, then the offer; "No thanks" changes nothing | ADS-01 / ADS-02 | **PASS** | Hints 1–3 each revealed a move with **no** dialog. Hint 4 raised the themed dialog naming both the count and the reward: *"Out of free hints — You've used your 3 free hints for this level. Watch a short ad for 3 more?"* with **Watch ad** / **No thanks**. "No thanks" left the board pixel-identical to the pre-dialog capture, the timer unchanged and no snackbar. The undo variant reads *"Out of free undos … your 5 free undos … Watch a short ad for 5 more?"* — both counts match `AssistAllowanceLimits`. |
| 9a | Watch to completion ⇒ reward granted and hints free again | ADS-05 | **PASS** | Accepted; a real Google **test** rewarded video played fullscreen (`…ads.mobile.sdk.common.AdActivity`, "Test Ad" watermark, Google Ads creative). On return the pending hint was revealed and the next hint request was **free** — i.e. the allowance had been refilled. Reproduced on four separate ads across the session. |
| 9b | Early dismiss ⇒ nothing granted, nothing refilled | ADS-05 | **NOT EXERCISED** (see § *Finding 2*) | Could not be produced on Google's test inventory. Dismissal was attempted at **t+2 s**, **t+8 s** and **t+33 s** after presentation; every one returned **Granted** (hint revealed, allowance refilled — probed by requesting the next hint and observing no dialog). The test creative fires `onUserEarnedReward` within ~2 s and exposes no close affordance until its post-reward end card, so a pre-reward dismissal is not reachable. **This is a limitation of the test inventory, not an observation about the app.** |
| 10 | Rescue-banner undo: same dialog when exhausted, instant and free when not; no ad affordance on the banner | ADS-04 | **PASS** | Reached a genuine dead position (3 pegs, no legal move). Banner: *"No moves left — undo or restart"* with **Undo / Restart / Home** and **no ad affordance of any kind**. With undos remaining, its Undo restored the board instantly, with no dialog and **zero** `AdActivity` lines in logcat. After draining all five free undos (four move-undo cycles plus the banner undo), the same banner Undo raised **the same dialog**, in its undo wording. |
| 11 | A truthful "no winning move" is free | ADS-06 | **PASS** | From a dead position with the hint allowance at zero, requesting a hint changed the banner from *"No moves left"* to *"No winning move — undo or restart"* and raised **no dialog**; logcat recorded **zero** `AdActivity` lines. Search first, charge second — observed. |
| 12 | The clock is frozen across a full ad watch | ADSF-01 | **PASS** | Measured against device wall-clock, twice. **(i)** Anchor 03:05:48 → timer `04:59` (299 s). Ad on screen 03:06:18.49 → 03:06:58.05 (**39.6 s**, from the `AdActivity` window lifecycle). At 03:07:09 the timer read **`05:42` (342 s)**. Predicted-if-frozen 299 + 81 − 39.6 = **341.4**; predicted-if-running = 381. Observed 342. **(ii)** Timer `08:30` immediately before accept at 03:10:36; ad on screen ~30 s; timer `08:31` at 03:11:07 — **+1 s across a 30 s presentation**. It resumed and kept counting normally afterwards in both cases. |
| 13 | Fail-open with no network | ADS-03 | **PASS** | Airplane mode on, allowance exhausted, ad slot cold, accepted at 03:20:41.4. At **t+2 s** the dialog was locked with a spinner in place of "Watch ad" and "No thanks" dimmed (D-10). By **t+~5 s** — matching `ACCEPT_WAIT_MS = 5000` — the dialog was gone, the hint was revealed, and the snackbar read exactly **"Ad unavailable — hints refilled for free"**. No error wall, no dead end. |
| 14 | Zero impressions from the development device | invalid-traffic guard | **PASS structurally; dashboard NOT READ** | The dashboard itself was not read — that requires the developer's AdMob console. What was verified instead is the stronger structural fact that makes the dashboard reading a formality: this build's `BuildConfig` resolves **only** Google's demo-publisher IDs (`ca-app-pub-3940256099942544~3347511713`, `…/5224354917`); every ad rendered carried the **"Test Ad"** watermark; and the SDK's own config request names `client=ca-app-pub-3940256099942544`. **No request in this session reached the developer's AdMob account**, so it cannot have recorded an impression from it. The emulator is additionally a test device by default. |

**Score: 11 PASS, 1 PASS-with-a-named-gap (14), 1 FAIL (4), 1 NOT EXERCISED (9b).**

---

## Finding 1 (item 4) — declining consent does not suppress the ad offer

**Observed:** after **"Do not consent"** — with all eleven TCF purpose consents recorded as `0` — the
app still initialized the ads SDK, still marked rewarded offers as available, and still raised the
opt-in dialog once the free allowance ran out.

**Mechanism, traced rather than guessed:** `UmpConsentGateway.initializeAdsIfPermitted()` gates
strictly on `consentInformation.canRequestAds()`, and that call returned `true`. UMP returns `true`
here because Google's consent framework permits **limited / non-personalised** ad serving on the
legitimate-interest bases the same form recorded
(`IABTCF_PurposeLegitimateInterests=01000011111`), even though consent was denied. The app is doing
exactly what its own KDoc says it does — deferring to the SDK's authoritative "may we request ads?"
answer, and never inferring consent client-side.

**Why this is being reported rather than fixed here.**

1. **It is not a defect against CONS-04 as written.** REQUIREMENTS.md defines CONS-04 as *"When the
   consent flow **fails**, the app falls back to the previous session's status and simply serves no
   ads rather than blocking play."* That is the failure path, and it is not what was exercised here.
   The plan's checklist item 4 additionally asserts a *decline ⇒ no ad UI* rule, and it is that
   stated expectation the observation contradicts.
2. **The correct behaviour is a product decision, not a bug fix.** Suppressing all ad offers for a
   player who declined TCF consent — even though Google says limited ads may be served — trades
   revenue for a stricter reading of the player's intent. Choosing that is a Rule-4 architectural /
   product call, and this plan's own instruction is to *"report failures rather than working around
   them"*.
3. **Nothing here is a GDPR exposure.** The decline was recorded correctly and completely; ads served
   in that state are non-personalised by construction. What is at stake is the *player-perceived*
   meaning of "Do not consent", not lawfulness.

**Routes to:** phase verification / gap closure. The decision needed is one sentence: *does
`rewardedOfferable` gate on `canRequestAds()` (today) or additionally on TCF Purpose-1 consent?* If
the latter, `AdMobAdGateway.rewardedOfferable` is the single place it changes, and the plan's
must-have truth #2 becomes true; if the former, the plan's item 4 and must-have truth #2 should be
corrected to match UMP's semantics.

---

## Finding 2 (item 9b) — the early-dismiss path is not reachable on test inventory

`RewardOutcome.Declined` — the "closed the ad early, so nothing is granted" branch — could not be
produced on the device. Google's test rewarded creative fires `onUserEarnedReward` within roughly two
seconds of presentation and shows no close control until its post-reward end card, so every
dismissal attempted (t+2 s, t+8 s, t+33 s) had already earned the reward. Each was probed rather than
assumed: the next hint request was free in every case, which means the allowance had been refilled,
which means the outcome was `Granted`.

**What remains proving ADS-05's negative half, and how strong it is:**

- **Behavioural, at the seam:** `GameViewModelAllowanceTest` (plan 09-08) asserts against a fake
  gateway that `Declined` refills nothing, spends nothing and leaves board, history, both counters,
  snackbar and loading flag untouched.
- **Structural, at the SDK edge:** plan 09-10 pins by grep that `earned = true` appears **exactly
  once** in `AdMobAdGateway`, on the `ad.show(` line, inside the `OnUserEarnedRewardListener` — and
  never in a dismissal callback. `onAdDismissedFullScreenContent` resumes with
  `if (earned) Granted else Declined`.

So the *wiring* is proven and the *consequences* are proven; what is unproven is the two of them
composed on real inventory. Item 9a — that a genuinely earned reward grants and refills — **is**
proven on real inventory, four times over.

---

## Method note — the item-6 geography patch

Item 6 needs a build that is **not** in a simulated consent region, and the debug build hard-codes
`DEBUG_GEOGRAPHY_EEA`. A one-line local edit changed that constant to `DEBUG_GEOGRAPHY_OTHER`, the
APK was rebuilt and installed, the check was observed, and the edit was then reverted with
`git checkout --` before any commit. The reverted state was re-verified two ways: `git status` clean
for that file, and a fresh EEA build reinstalled and cold-launched to confirm the form renders again
(item 2b's second replay). **No part of that patch is in any commit**, and the shipped source is
byte-identical to what plan 09-05 committed.

---

## What this run did **not** prove

Stated plainly, because a later phase will build on this file's claims.

1. **Nothing here ran on physical hardware.** Every one of the fourteen items is emulator-grade
   (`sdk_gphone16k_arm64`, API 36). The items where that matters most are **12** (clock freeze —
   verified against emulator wall-clock, and real-device scheduling under a fullscreen video could
   differ) and **13** (fail-open — `ACCEPT_WAIT_MS` was measured against an instantly-failing
   airplane-mode load, not a slow real network, which is the case that actually tests the bound).
2. **The developer's own UMP consent message has never been rendered.** Every form observed was
   Google's test-publisher message, because debug resolves the test AdMob application ID. Item 1's
   pass is a pass for *the app's consent sequence*, not for *the account's message configuration*.
   D-18 is closed on report; **RESEARCH A5 is only partially closed** (see Task 1).
3. **No release-signed build was exercised.** This is entirely a `debug` build with test ad units and
   no minification path involved. Release signing, real ad units against real inventory, and the
   R8/`isShrinkResources` release surface are **REL-02, owned by Phase 14** — and Phase 14 is
   therefore the first point at which the production consent message and the real rewarded unit are
   exercised at all.
4. **The AdMob dashboard was not read** (item 14). The structural argument is strong — only Google's
   demo publisher IDs were ever used, every ad was watermarked "Test Ad" — but the dashboard reading
   itself needs the developer's console and remains outstanding.
5. **`RewardOutcome.Declined` was never produced on real inventory** (item 9b / Finding 2).
6. **The rewarded ad unit's existence in the AdMob console is unconfirmed**, as is whether the app
   has been registered in AdMob at all. Neither blocks anything before Phase 14, and neither is
   observable from a debug build.
7. **Interstitials were not exercised**, correctly: `AdMobAdGateway.showInterstitial()` is a
   documented no-op owned by Phase 10, and it has no caller.

### `[ASSUMED]` research items touched by this run

| Item | Claim | Status after this run |
|---|---|---|
| **A1** | Rewarded ads expire ~1 h after load (undocumented for GMA Next-Gen); `REWARDED_AD_MAX_AGE_MS` = 45 min is a defensive bound under it | **Still assumed — not exercised.** No ad in this session sat unshown for anywhere near 45 minutes; the longest gap between a preload and its presentation was a few minutes. `REWARDED_AD_MAX_AGE_MS` was never reached, so neither the figure nor the discard path was tested. Correctness does not rest on it (a stale ad lands in `onAdFailedToShowFullScreenContent` → `Unavailable` → free assist, which item 13 *did* exercise via a different trigger). |
| **A5** | The AdMob rewarded ad unit and the UMP consent message are already configured | **Partially closed.** Consent message: reported published, not observable here. Rewarded unit: still unconfirmed. See Task 1. |
| **A7** | The ads/UMP manifest meta-data does not change the merged permission set | **Already settled in plan 09-05** by `ManifestSurfaceTest`'s set-equality assertion, which is green in this run's gate below. Nothing on the device contradicted it. |

---

## Task 3 — the full offline gate

Run on the same commit as this file, on `feature/phase-09-consent-rewarded-ads`, macOS (Darwin
24.6.0), AGP 9.1.0 / Gradle 9.5.1 wrapper / Kotlin 2.3.20 / JDK 17.

| Gate | Result |
|---|---|
| `./gradlew check --offline` | **GREEN** — see § below |
| `./gradlew :app:assembleDebug --offline` | **GREEN** |
| `git status` for `UmpConsentGateway.kt` | clean — the item-6 patch is reverted and uncommitted |

### `./gradlew check --offline`

```
BUILD SUCCESSFUL in 11m 8s
90 actionable tasks: 14 executed, 12 from cache, 64 up-to-date
```

`:app:testDebugUnitTest` — **431 tests, 0 failures, 0 skipped**. The suite includes the Phase-6
structural gates this phase must not have weakened (`PlatformBoundaryTest`, `ManifestSurfaceTest`,
`BuildConfigIdsTest`, `adIdLiteralCheck`, `dpSpLiteralCheck`), plus `detekt` and `ktlint`. The
`--offline` flag holds, so Phase 6's D-18 offline invariant is intact with both the UMP and GMA
SDKs on the graph.

### `./gradlew :app:assembleDebug --offline`

```
BUILD SUCCESSFUL in 1s
55 actionable tasks: 55 up-to-date
```

Up-to-date against the APK that was actually installed and driven for every item above — i.e. the
gate ran on the same build the device evidence describes, with the item-6 patch already reverted.

---

*Phase: 09-consent-rewarded-ads · Plan 09-11 · Recorded 2026-09-02*
