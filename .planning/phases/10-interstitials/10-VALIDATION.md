---
phase: 10
slug: interstitials
status: approved
nyquist_compliant: true
wave_0_complete: false
created: 2026-09-02
---

# Phase 10 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> Rendered from `10-RESEARCH.md` § Validation Architecture; task map aligned to plans `10-01`..`10-06`.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 4.13.2 + Truth 1.4.4 + Turbine 1.2.1 + MockK 1.14.2 + kotlinx-coroutines-test 1.11.0; Robolectric 4.16 for JVM-side Android tests |
| **Config file** | `gradle/libs.versions.toml` (versions), `app/build.gradle.kts`, `domain/build.gradle.kts` |
| **Quick run command** | `./gradlew :domain:test` (pure cadence logic — seconds, no Android) |
| **Full suite command** | `./gradlew check` (includes `detekt`, `ktlintCheck`, `dpSpLiteralCheck`, `adIdLiteralCheck`, `audioFocusLiteralCheck`) |
| **Estimated runtime** | quick: ~10 s · full `check`: several minutes (phase-gate only) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :domain:test` (plus the task's own `<automated>` command)
- **After every plan wave:** Run `./gradlew check`
- **Before `/gsd:verify-work`:** Full suite must be green + device UAT run against the Google test interstitial unit
- **Max feedback latency:** < 60 s per task (full `check` reserved for wave merges and the 10-06 phase gate)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 10-01-01 | 01 | 1 | INT-02 | tamper/overflow | Validate-on-read fails toward fewer ads; saturating lifetime counter; backwards clock never locks gate | unit (pure, TDD RED) | `./gradlew :domain:test --tests '*InterstitialCadenceStateTest'` (expect FAIL pre-impl) | ❌ W0 | ⬜ pending |
| 10-01-02 | 01 | 1 | INT-02 | tamper/overflow | Same truth-table invariants GREEN | unit (pure) | `./gradlew :domain:test --tests '*InterstitialCadenceStateTest' && ./gradlew :domain:detekt :domain:ktlintCheck` | ❌ W0 | ⬜ pending |
| 10-02-01 | 02 | 2 | INT-02 | tamper | Corrupt store reads back as defaults (⇒ no ad); state survives process death | unit (Robolectric + test DataStore) | `./gradlew :app:testDebugUnitTest --tests '*InterstitialCadenceStoreTest'` | ❌ W0 | ⬜ pending |
| 10-02-02 | 02 | 2 | INT-02 | — | D-16 boundary: no forged background event from fullscreen ads | unit + arch | `./gradlew :app:testDebugUnitTest --tests '*AppForegroundTrackerTest' --tests '*PlatformBoundaryTest' --tests '*PlatformBoundaryGateTest'` | ❌ W0 | ⬜ pending |
| 10-03-01 | 03 | 1 | INT-01, INT-02 | no-logging, consent | No ad-path logging; no ad-ID literals | compile + arch gates | `./gradlew :app:compileDebugKotlin :app:detekt :app:ktlintCheck :app:adIdLiteralCheck` | ✅ | ⬜ pending |
| 10-03-02 | 03 | 1 | INT-01 | consent | Consent gates interstitial load exactly as rewarded load | compile + lint | `./gradlew :app:compileDebugKotlin :app:detekt :app:ktlintCheck` | ✅ | ⬜ pending |
| 10-03-03 | 03 | 1 | INT-01 | — | `NoOpAdGateway` never shows an interstitial (stub parity) | unit | `./gradlew :app:testDebugUnitTest --tests '*NoOpAdGatewayTest' --tests 'com.app.pegsolitaire.ui.game.*'` | ✅ extend | ⬜ pending |
| 10-04-01 | 04 | 3 | INT-01..04 | — | N/A | compile + lint | `./gradlew :app:compileDebugKotlin :app:detekt :app:ktlintCheck` | ✅ | ⬜ pending |
| 10-04-02 | 04 | 3 | INT-01..04 | boundary | No GMA type escapes `..platform..` | arch | `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest --tests '*PlatformBoundaryTest' --tests '*PlatformBoundaryGateTest' :app:detekt :app:ktlintCheck` | ✅ | ⬜ pending |
| 10-04-03 | 04 | 3 | INT-01, INT-03, INT-04, ADSF-01, D-09, D-12, D-14, D-15 | accidental-click, consent | Eligibility (2-star no / Diamond-41 3-star yes / stuck no), navigate-exactly-once, no preload while impossible, clock invariant | unit (VM) | `./gradlew :app:testDebugUnitTest --tests '*ResultsViewModelTest'` | ⚠️ extend, cases ❌ W0 | ⬜ pending |
| 10-05-01 | 05 | 4 | INT-04 | accidental-click | Scrim swallows pointer + back; dimens via `dimens.xml` | compile + `dpSpLiteralCheck` | `./gradlew :app:compileDebugKotlin :app:dpSpLiteralCheck :app:detekt :app:ktlintCheck` | ✅ | ⬜ pending |
| 10-05-02 | 05 | 4 | INT-01, INT-04 | accidental-click | Scrim renders on ad path, absent on D-10 fast path | unit (Compose/Robolectric) | `./gradlew :app:testDebugUnitTest --tests '*ResultsScreenTest' --tests '*NavFlowTest' --tests '*ButtonSoundTest' --tests '*ResultsCelebrationSoundTest'` | ⚠️ extend | ⬜ pending |
| 10-06-01 | 06 | 5 | INT-01..04 | all | Full gate sweep + domain-purity grep of `domain/ads/` | full suite + grep | `./gradlew check && ./gradlew :app:assembleDebug && grep -rn 'import android\|import androidx' domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/ \| wc -l \| grep -qx 0 && echo SWEEP-OK` | ✅ | ⬜ pending |
| 10-06-02 | 06 | 5 | INT-01..04, ADSF-01 | accidental-click | Device UAT (checkpoint, blocking) | manual | — (see Manual-Only Verifications) | — | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `domain/src/test/.../ads/InterstitialCadenceStateTest.kt` — INT-02 truth table (the ROADMAP-mandated TDD-before-wiring artifact; plan 10-01 task 1 writes it RED)
- [ ] `app/src/test/.../data/InterstitialCadenceStoreTest.kt` — INT-02 persistence; model on existing `ChallengeProgressTest`
- [ ] Extend `app/src/test/.../ui/results/ResultsViewModelTest.kt` — INT-01, INT-03, INT-04 sequencing, D-09, D-12, ADSF-01
- [ ] Extend `app/src/test/.../ui/results/ResultsScreenTest.kt` — INT-04 rendering + input swallowing
- [ ] Extend `app/src/test/.../platform/ads/NoOpAdGatewayTest.kt` — stub parity for new seam members
- [ ] `FakeAdGateway` test double (ready/not-ready, dismissed/failed-to-show) — no such fake exists yet
- [ ] Framework install: **none needed** — every library is already declared

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Actual ad rendering + dismissal | INT-01, INT-04 | Requires live Google test interstitial unit on device | Plan 10-06 task 2 itemised device UAT (~40 min, device clock — no debug fast-forward surface exists by design) |
| Perceptual quality of the 500 ms dim | INT-04 (D-08) | Perceptual judgement | 10-06 UAT step covering scrim feel |
| "Does this feel resented?" | Phase goal | The ROADMAP's governing sentence is a human judgement | 10-06 UAT step 12; physical device (Phase 7 audit was emulator-only caution in STATE.md) |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 60s per task (full `check` reserved for wave merges / phase gate)
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved 2026-09-02 (plan-checker Nyquist dimensions 8a–8d passed)
