---
phase: 9
slug: consent-rewarded-ads
status: planned
nyquist_compliant: true
wave_0_complete: false
created: 2026-08-30
updated: 2026-08-31
---

# Phase 9 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 + Robolectric/Roborazzi (JVM), Turbine, MockK, Truth |
| **Config file** | `app/build.gradle.kts`, `domain/build.gradle.kts` (existing test setup) |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --offline --tests "<changed test class>"` (or `./gradlew :domain:test --offline` for the pure-Kotlin allowance work) |
| **Full suite command** | `./gradlew check --offline` (offline-capable invariant must hold — Phase 6 D-18) |
| **Estimated runtime** | ~120 seconds full suite |

---

## Sampling Rate

- **After every task commit:** Run the quick command scoped to the touched test classes
- **After every plan wave:** Run `./gradlew check --offline`
- **Before `/gsd:verify-work`:** Full suite must be green **and** `09-DEVICE-EVIDENCE.md` must exist
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 09-01-T1 | 09-01 | 1 | ADS-02 | T-09-01, T-09-03, T-09-04 | Counters are total for every `Int`; limits named once | unit (pure JVM) | `./gradlew :domain:test --offline --tests '*AssistAllowanceTest*'` | ❌ created by task | ⬜ pending |
| 09-01-T2 | 09-01 | 1 | ADS-02 | T-09-01, T-09-02 | Forged counter falls open to default; never refuses the resume | unit (JVM) | `./gradlew :app:testDebugUnitTest --offline --tests '*SnapshotMapperTest*'` | ⚠️ extend existing | ⬜ pending |
| 09-02-T1 | 09-02 | 1 | ADS-01, ADS-03 | T-09-06, T-09-07 | No ad ID may enter `AdConstants` | build gate | `./gradlew :app:adIdLiteralCheck --offline` | ✅ exists | ⬜ pending |
| 09-02-T2 | 09-02 | 1 | ADS-01 | — | Stub reports offers available so the dialog is exercised (Pitfall 6) | unit | `./gradlew :app:testDebugUnitTest --offline --tests '*NoOpAdGatewayTest*' --tests '*GameViewModel*Test*'` | ⚠️ extend existing | ⬜ pending |
| 09-02-T3 | 09-02 | 1 | CONS-03, CONS-04 | T-09-05, T-09-08 | Consent contracts name no SDK and no Activity type | unit (ArchUnit) | `./gradlew :app:testDebugUnitTest --offline --tests '*PlatformBoundaryTest*'` | ✅ exists | ⬜ pending |
| 09-03-T1 | 09-03 | 2 | ADS-01, ADS-03 | T-09-10 | Allowance counts are placeholders fed from the policy, never typed copy | build | `./gradlew :app:assembleDebug --offline` | ✅ exists | ⬜ pending |
| 09-03-T2 | 09-03 | 2 | ADS-01 | T-09-09, T-09-10, T-09-11 | Peer accept/decline; loading state locked and bounded | Compose (Robolectric) | `./gradlew :app:testDebugUnitTest --offline --tests '*AdOfferDialogTest*'` | ❌ created by task | ⬜ pending |
| 09-03-T3 | 09-03 | 2 | ADS-03 | T-09-12 | Clay snackbar defined once for both screens | build gate | `./gradlew :app:dpSpLiteralCheck --offline` | ✅ exists | ⬜ pending |
| 09-04-T1 | 09-04 | 2 | ADSF-01 | T-09-13, T-09-14, T-09-16 | Freeze wraps the request, not the dialog; fresh mark on resume | unit (VM regression) | `./gradlew :app:testDebugUnitTest --offline --tests '*GameViewModelTest*' --tests '*GameViewModelSoundTest*' --tests '*GameViewModelResumeTest*'` | ✅ exists | ⬜ pending |
| 09-04-T2 | 09-04 | 2 | ADSF-01 | T-09-14, T-09-15, T-09-16 | Rescue path cannot re-add the ad's duration | unit (VM, virtual time) | `./gradlew :app:testDebugUnitTest --offline --tests '*GameViewModelAdClockTest*'` | ❌ created by task | ⬜ pending |
| 09-05-T1 | 09-05 | 2 | CONS-01, CONS-02, CONS-04 | T-09-17, T-09-18, T-09-19, T-09-20 | `requestConsentInfoUpdate` before `canRequestAds`; one off-main init; no logging | compile + static | `./gradlew :app:compileDebugKotlin --offline && ./gradlew :app:detekt --offline` | ✅ exists | ⬜ pending |
| 09-05-T2 | 09-05 | 2 | CONS-01, CONS-02 | T-09-21 | Every SDK-typed provider inside `..platform..` | unit (ArchUnit) + build | `./gradlew :app:testDebugUnitTest --offline --tests '*PlatformBoundaryTest*' && ./gradlew :app:assembleDebug --offline` | ✅ exists | ⬜ pending |
| 09-05-T3 | 09-05 | 2 | CONS-01 | T-09-22 | App ID via generated resource; permission surface unchanged; suite still offline | unit + build gates | `./gradlew :app:testDebugUnitTest --offline --tests '*ManifestSurfaceTest*' && ./gradlew :app:adIdLiteralCheck --offline && ./gradlew check --offline` | ✅ exists | ⬜ pending |
| 09-06-T1 | 09-06 | 3 | ADS-01, ADS-03 | T-09-26 | Tick ordinal carried at all four install sites; no allowance count in UiState | unit (VM regression) | `./gradlew :app:testDebugUnitTest --offline --tests '*GameViewModel*' --tests '*GameScreenSoundTest*' --tests '*GameFlowTest*'` | ✅ exists | ⬜ pending |
| 09-06-T2 | 09-06 | 3 | ADS-06 | T-09-23, T-09-24, T-09-25 | Gateway never consulted for an assist not delivered | unit (VM) | `./gradlew :app:testDebugUnitTest --offline --tests '*GameViewModelAssistOrderingTest*' --tests '*GameViewModelHintStalenessTest*'` | ❌ created by task | ⬜ pending |
| 09-07-T1 | 09-07 | 3 | REL-05 | T-09-27, T-09-31 | Policy URL is a static compile-time value | human gate + grep | `grep -q 'https://' .planning/phases/09-consent-rewarded-ads/09-CONTEXT.md` | n/a | ⬜ pending |
| 09-07-T2 | 09-07 | 3 | CONS-03, REL-05 | T-09-27, T-09-29, T-09-30 | ViewModel never names an Activity or the UMP impl | unit (VM + ArchUnit) | `./gradlew :app:testDebugUnitTest --offline --tests '*SettingsViewModel*' --tests '*PlatformBoundaryTest*'` | ✅ exists | ⬜ pending |
| 09-07-T3 | 09-07 | 3 | CONS-03, REL-05 | T-09-27, T-09-28, T-09-29 | ACTION_VIEW carries the constant URI; no browser is not a dead end | Compose + ArchUnit | `./gradlew :app:testDebugUnitTest --offline --tests '*SettingsPrivacyTest*' --tests '*PlatformBoundaryTest*' && ./gradlew :app:dpSpLiteralCheck --offline` | ❌ created by task | ⬜ pending |
| 09-08-T1 | 09-08 | 4 | ADS-01, ADS-02, ADS-04, CONS-04 | T-09-32, T-09-35, T-09-36 | One request site; readiness read only after exhaustion; retry cannot refill | unit (VM) | `./gradlew :app:testDebugUnitTest --offline --tests '*GameViewModel*' --tests '*GameFlowTest*'` | ✅ exists | ⬜ pending |
| 09-08-T2 | 09-08 | 4 | ADS-03, ADS-05 | T-09-33, T-09-34, T-09-37 | Refill-then-spend on Granted/Unavailable; Declined mutates nothing | unit (VM) | `./gradlew :app:testDebugUnitTest --offline --tests '*GameViewModel*'` | ✅ exists | ⬜ pending |
| 09-08-T3 | 09-08 | 4 | ADS-01/02/03/04/05, CONS-04 | T-09-32..T-09-38 | The whole economy proven against the stub before any real SDK | unit (VM, 13 cases) | `./gradlew :app:testDebugUnitTest --offline --tests '*GameViewModelAllowanceTest*'` | ❌ created by task | ⬜ pending |
| 09-09-T1 | 09-09 | 5 | ADS-01, ADS-03 | T-09-39, T-09-40, T-09-42 | Snackbar styling lives only in the shared host; suppression comment amended | unit + full gate | `./gradlew :app:testDebugUnitTest --offline --tests '*GameScreenSoundTest*' --tests '*GameFlowTest*' && ./gradlew check --offline` | ✅ exists | ⬜ pending |
| 09-09-T2 | 09-09 | 5 | ADS-01, ADS-03, ADS-04 | T-09-40, T-09-41 | No snackbar on entry; no second offer surface on the banner | Compose (Robolectric) | `./gradlew :app:testDebugUnitTest --offline --tests '*GameScreenAdOfferTest*'` | ❌ created by task | ⬜ pending |
| 09-10-T1 | 09-10 | 6 | ADS-03 | T-09-46, T-09-47, T-09-48 | Single bounded retry; no legacy `gms.ads` package; no ID literal | compile + build gate | `./gradlew :app:compileDebugKotlin --offline && ./gradlew :app:adIdLiteralCheck --offline` | ✅ exists | ⬜ pending |
| 09-10-T2 | 09-10 | 6 | ADS-01, ADS-05, CONS-02 | T-09-43, T-09-44, T-09-45, T-09-50 | `earned = true` exists exactly once, inside the SDK callback | compile + static + grep | `./gradlew :app:compileDebugKotlin --offline && ./gradlew :app:detekt --offline` | ✅ exists | ⬜ pending |
| 09-10-T3 | 09-10 | 6 | CONS-02, CONS-04 | T-09-48, T-09-49 | Binding swap disturbs no Phase-6 gate; no expected value edited | unit (3 gates) + full gate | `./gradlew :app:testDebugUnitTest --offline --tests '*PlatformBoundaryTest*' --tests '*ManifestSurfaceTest*' --tests '*BuildConfigIdsTest*' && ./gradlew check --offline` | ✅ exists | ⬜ pending |
| 09-11-T1 | 09-11 | 7 | CONS-01 | T-09-52 | No CONS-01 pass may be recorded without a published consent message | human gate | — (blocking checkpoint) | n/a | ⬜ pending |
| 09-11-T2 | 09-11 | 7 | CONS-01/02/03, ADS-05, ADSF-01, REL-05 | T-09-51, T-09-54, T-09-55 | Per-item results, never a global approval | manual (device) | — (blocking checkpoint) | n/a | ⬜ pending |
| 09-11-T3 | 09-11 | 7 | all | T-09-53 | Evidence names its own gaps | build + artifact | `./gradlew check --offline && ./gradlew :app:assembleDebug --offline && test -f .planning/phases/09-consent-rewarded-ads/09-DEVICE-EVIDENCE.md` | ❌ created by task | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

The seven RESEARCH-identified test files are each created **inside the plan that owns their subject**,
rather than in a separate Wave 0 plan — every one of them tests code that does not exist until that
plan runs, so a pre-created empty file would be a placeholder, not a gate.

- [ ] `domain/src/test/kotlin/com/app/pegsolitaire/domain/assist/AssistAllowanceTest.kt` — plan 09-01 T1
- [ ] `SnapshotMapperTest` allowance cases (extend existing) — plan 09-01 T2
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/game/AdOfferDialogTest.kt` — plan 09-03 T2
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAdClockTest.kt` — plan 09-04 T2
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAssistOrderingTest.kt` — plan 09-06 T2 (ADS-06)
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsPrivacyTest.kt` — plan 09-07 T3
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAllowanceTest.kt` — plan 09-08 T3
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenAdOfferTest.kt` — plan 09-09 T2 (the D-08 at-zero path is asserted at the ViewModel and screen level; `NoWinBanner.kt` and `NoWinBannerTest` stay untouched per UI-SPEC §4)
- [ ] JVM suite must remain offline-capable — no UMP/GMA class reachable from Robolectric-run `App.onCreate()` (plan 09-05 T2/T3 enforce this)

*Existing infrastructure (JUnit4/Robolectric/Turbine/MockK/Truth) covers the frameworks; no new installs.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| AdMob GDPR consent message is **published** | CONS-01 | Server-side configuration invisible from the client; its absence is a silent no-op | Plan 09-11 Task 1 (blocking gate before any device check) |
| Fresh install in EEA debug geography shows the consent form before Home | CONS-01 | UMP has no fakeable seam; needs a real published message | Plan 09-11 Task 2, item 1 |
| `consentInformation.reset()` replays a first-install consent flow | CONS-01 | Same | Plan 09-11 Task 2, item 2 |
| SDK initializes only after consent resolves, off the main thread | CONS-02 | Manual + code review (`AtomicBoolean` + dispatcher are review items) | Plan 09-11 Task 2, item 3; code review in 09-05 |
| Declining consent leaves assists free with **no** dialog and **no** snackbar | CONS-04 / D-12 | Requires a real declined UMP state | Plan 09-11 Task 2, item 4 |
| Both privacy rows in a consent region; only the policy row outside one | CONS-03 | Depends on the server-configured requirement status | Plan 09-11 Task 2, items 5-6 |
| The privacy policy opens the live URL in the system browser | REL-05 | Needs a real browser and a live site | Plan 09-11 Task 2, item 7 |
| A real Google **test** rewarded ad plays; completion refills, early dismiss does not | ADS-05 | Needs real ad inventory and the SDK's earned callback | Plan 09-11 Task 2, item 9 |
| The elapsed clock is unchanged across a full ad watch | ADSF-01 | Asserted in virtual time by 09-04, confirmed end to end on device | Plan 09-11 Task 2, item 12 |
| Airplane-mode fail-open grants the assist with the explanation snackbar | ADS-03 | Needs a real no-fill / no-network condition | Plan 09-11 Task 2, item 13 |
| AdMob dashboard shows **zero** impressions from the dev device | Phase 6 D-05 | Dashboard-only signal | Plan 09-11 Task 2, item 14 |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or an explicit `<human-check>` on a blocking checkpoint
- [x] Sampling continuity: no 3 consecutive tasks without an automated verify
- [x] Wave 0 files are each owned by the plan that creates the code they test
- [x] No watch-mode flags
- [x] Feedback latency < 180s (quick commands are class-scoped; the full gate is ~120s)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** planned 2026-08-31 — statuses flip to green during execution
