---
phase: 7
slug: sound
status: planned
nyquist_compliant: true
wave_0_complete: false
created: 2026-08-28
updated: 2026-08-28
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 4.13.2 + Truth 1.4.4 + Turbine 1.2.1 + MockK 1.14.2 + Robolectric 4.16 + Compose UI Test (BOM 2026.06.00) — all already on `testImplementation`, no install needed |
| **Config file** | `app/build.gradle.kts` (deps) + `build-logic/src/main/kotlin/android-app.gradle.kts` (`testOptions.unitTests.isIncludeAndroidResources = true`) |
| **Quick run command** | `./gradlew :app:testDebugUnitTest --tests "*Sound*" --offline` |
| **Full suite command** | `./gradlew check --offline` |
| **Estimated runtime** | ~90-120 seconds for full `check` (detekt, ktlint, 3 file-scan gates, ArchUnit, ~299+ JVM tests) |

Robolectric has no android-all jar for SDK 36; every Robolectric test in this phase pins
`@Config(sdk = [34])`, matching the existing `GameFlowTest` convention. There is no `androidTest`
source set and this phase does not add one.

---

## Sampling Rate

- **After every task commit:** `./gradlew :app:testDebugUnitTest --tests "*Sound*" --offline` plus the task's own named test
- **After every plan wave:** `./gradlew check --offline`
- **Before `/gsd:verify-work`:** full suite green + plan 07-06's release-shrink check + the manual audit
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 01-T1 | 07-01 | 1 | SOUND-01, SOUND-02, SOUND-03, SOUND-04 | T-07-01 | HTTPS download, archive-content inspection before extraction, `ffprobe` type confirmation per committed file | asset scan + build | `for f in app/src/main/res/raw/*.ogg; do ffprobe -v error -select_streams a:0 -show_entries stream=codec_name -of csv=p=0 "$f"; done` then `./gradlew :app:assembleDebug --offline` | n/a (CLI) | ⬜ pending |
| 01-T2 | 07-01 | 1 | SOUND-01..04 (D-01) | T-07-02 | Per-file source URL + licence recorded in-repo and enforced | unit (JVM file scan) | `./gradlew :app:testDebugUnitTest --tests "*AudioCreditsTest" --offline` | ❌ created in task | ⬜ pending |
| 01-T3 | 07-01 | 1 | SOUND-06 | T-07-04 | No `requestAudioFocus`/`abandonAudioFocusRequest` anywhere in `src/main` | build gate | `./gradlew :app:audioFocusLiteralCheck :domain:audioFocusLiteralCheck --offline` | ❌ created in task | ⬜ pending |
| 02-T1 | 07-02 | 2 | SOUND-01, SOUND-02 | T-07-02 | Playback rate `coerceIn(MIN_RATE, MAX_RATE)` — total for negative and absurd indices (ASVS V5) | unit (pure JVM) | `./gradlew :app:testDebugUnitTest --tests "*SoundParamsTest" --offline` | ❌ created in task | ⬜ pending |
| 02-T2 | 07-02 | 2 | SOUND-01, SOUND-05, SOUND-06 | T-07-03, T-07-04, T-07-05 | `USAGE_GAME`+`CONTENT_TYPE_SONIFICATION`, no focus request, bounded stream buffer, class-level `@Singleton` | unit (Robolectric `ShadowSoundPool`) | `./gradlew :app:testDebugUnitTest --tests "*SoundPoolPlayerTest" --offline` | ❌ created in task | ⬜ pending |
| 02-T3 | 07-02 | 2 | SOUND-01 | T-07-05 | Interface-typed injection in `App`/`MainActivity` so no second unscoped pool exists | integration (Hilt graph + full check) | `./gradlew :app:check --offline` | ✅ existing suite | ⬜ pending |
| 03-T1 | 07-03 | 3 | SOUND-01, SOUND-02 | T-07-08, T-07-09 | Ordinals carried forward at all four rebuild sites; run index derived, never stored; nothing enters `:domain` | unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*GameViewModelSoundTest" --offline` | ❌ created in task | ⬜ pending |
| 03-T2 | 07-03 | 3 | SOUND-01, SOUND-02, SOUND-03 | T-07-06 | Every cue fires from a `LaunchedEffect` bound to the composition; fresh state fires nothing | integration (Robolectric + fake `LocalSoundPlayer`) | `./gradlew :app:testDebugUnitTest --tests "*GameScreenSoundTest" --offline` | ❌ created in task | ⬜ pending |
| 04-T1 | 07-04 | 3 | SOUND-03 | T-07-10 | Play-once latch written to `SavedStateHandle` synchronously before state update | unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*ResultsViewModelTest" --offline` | ✅ extend existing | ⬜ pending |
| 04-T2 | 07-04 | 3 | SOUND-03 | T-07-02, T-07-11 | Pop sequence bounded by `earned`, cancelled with the composition, rates clamped | integration (Robolectric + fake) | `./gradlew :app:testDebugUnitTest --tests "*ResultsCelebrationSoundTest" --offline` | ❌ created in task | ⬜ pending |
| 05-T1 | 07-05 | 4 | SOUND-04 | T-07-13, T-07-14 | Wrapper never swallows or reorders the original callback; excluded controls proven silent | integration (Robolectric + fake) | `./gradlew :app:testDebugUnitTest --tests "*ButtonSoundTest" --offline` | ❌ created in task | ⬜ pending |
| 05-T2 | 07-05 | 4 | SOUND-04 (D-12) | T-07-12 | Exactly one gate-bypass call site, on the user-initiated enable only | unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "com.app.pegsolitaire.ui.settings.*" --offline` | ❌ created in task | ⬜ pending |
| 06-T1 | 07-06 | 5 | SOUND-01, SOUND-02, SOUND-03, SOUND-04 | T-07-15 | All 11 `raw/sfx_` resources survive R8 + `isShrinkResources` | build + CLI assertion | `./gradlew :app:assembleRelease --offline` then `aapt2 dump resources <apk> \| grep -c 'raw/sfx_'` | n/a (CLI) | ⬜ pending |
| 06-T2 | 07-06 | 5 | SOUND-01, SOUND-05, SOUND-06 | T-07-02, T-07-04, T-07-16 | Real audio-policy coexistence, real cold-start latency, human licence sign-off | manual UAT (+ regression) | `./gradlew :app:check --offline` (regression half only) | manual-only | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

**Sampling continuity:** every task carries an `<automated>` command. There is no run of three
consecutive tasks without automated feedback. The only partly-manual task (06-T2) still runs the
full `check` regression as its automated half.

---

## Wave 0 Requirements

No separate Wave 0 plan exists. Every test file is created **inside the task that implements the
behaviour**, with the expectations written first as a `<behavior>` block (`tdd="true"`). No
framework install is required — every library is already on `testImplementation`.

Files created during execution:

- [ ] `app/src/test/java/com/app/pegsolitaire/audio/AudioCreditsTest.kt` — 01-T2 (D-01 provenance)
- [ ] `build-logic/src/main/kotlin/standards.gradle.kts` → `audioFocusLiteralCheck` — 01-T3 (SOUND-06)
- [ ] `app/src/test/java/com/app/pegsolitaire/audio/SoundParamsTest.kt` — 02-T1 (SOUND-01, SOUND-02)
- [ ] `app/src/test/java/com/app/pegsolitaire/audio/SoundPoolPlayerTest.kt` — 02-T2 (SOUND-01 preload, SOUND-05, SOUND-06)
- [ ] `app/src/test/java/com/app/pegsolitaire/audio/FakeSoundPlayer.kt` — 02-T3 (recording fake for `LocalSoundPlayer`)
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelSoundTest.kt` — 03-T1 (D-06 ladder, D-08 undo symmetry, D-05a, ordinal carry-forward)
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenSoundTest.kt` — 03-T2 (SOUND-02 cue set, D-14 win at final jump)
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt` (extend) — 04-T1 (D-16 play-once)
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsCelebrationSoundTest.kt` — 04-T2 (D-13, D-15)
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt` — 05-T1 (D-09 both directions, D-11)
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelSoundTest.kt` — 05-T2 (D-12)
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelTest.kt` (update constructor) — 05-T2

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| A podcast is never ducked during a full level | SOUND-06 | Ducking is arbitrated by the platform audio policy service; Robolectric shadows do not model focus arbitration, and emulator audio routing is the least faithful part of the emulator | Plan 07-06 Task 2 step 2 — physical device preferred, hardware recorded in the response |
| First tap of a cold session is audible | SOUND-01 | Real decode latency on real hardware; the JVM test proves the load-completion guard exists, not that the timing holds | Plan 07-06 Task 2 step 1 — three consecutive cold starts |
| Immediate silence including mid-playback one-shots | SOUND-05 | The JVM test proves `stop()` reaches tracked stream ids; only a human ear confirms the perceptual cut-out | Plan 07-06 Task 2 step 3 |
| The set reads as candy clay and nothing is a dud | SOUND-01..04 (D-02, D-03) | Aesthetic judgement, deferred here by explicit user decision D-04 | Plan 07-06 Task 2 step 4 — name duds, they are swapped before the phase closes |
| No doubled click with OS touch sounds on | SOUND-04 (D-10) | Confirms a research finding (Compose emits no platform tick) against a real OS setting | Plan 07-06 Task 2 step 5 |
| Every shipped licence is acceptable | D-01 (T-07-02) | Legal acceptance is a human decision | Plan 07-06 Task 2 step 6 — blocking sign-off on the CREDITS table |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or an in-task test-creation step
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 gaps are each assigned to the task that creates them
- [x] No watch-mode flags
- [x] Feedback latency < 120s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** planner-approved 2026-08-28; execution pending.
