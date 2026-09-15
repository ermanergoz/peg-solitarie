---
phase: 13
slug: cloud-save
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-09-15
---

# Phase 13 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 4.13.2 + Truth 1.4.4 + Turbine 1.2.1 + MockK 1.14.2 + kotlinx-coroutines-test 1.11.0; Robolectric 4.16 for Room/Android-runtime JVM tests; ArchUnit 1.3.0 for the boundary gates |
| **Config file** | none for JUnit4; detekt at `config/detekt/detekt.yml` |
| **Quick run command** | `./gradlew :domain:test --tests "*ProgressMergeTest*"` (domain) / `./gradlew :app:testDebugUnitTest --tests "*<ClassName>*"` (app) |
| **Full suite command** | `./gradlew :domain:test :app:testDebugUnitTest` |
| **Estimated runtime** | ~90 seconds (full JVM suite); `./gradlew check` ≈ 11 minutes (phase gate only) |

---

## Sampling Rate

- **After every task commit:** Run the narrowest `--tests` filter for the class touched (see quick run command)
- **After every plan wave:** Run `./gradlew :domain:test :app:testDebugUnitTest`
- **Before `/gsd:verify-work`:** Full suite must be green, plus `./gradlew check` and `./gradlew :app:assembleRelease` with no `com.google.android.gms.games` entries in `app/build/outputs/mapping/release/missing_rules.txt`
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 13-01-01 | 01 | 1 | CLOUD-02 | — | merge is commutative, associative, idempotent; never regresses a field | unit (`:domain`) | `./gradlew :domain:test --tests "*ProgressMergeTest*"` | ❌ W0 | ⬜ pending |
| 13-01-02 | 01 | 1 | CLOUD-02 | — | local `recordResult` byte-identical after extraction | unit (Robolectric) | `./gradlew :app:testDebugUnitTest --tests "*ProgressRepositoryTest*"` | ✅ | ⬜ pending |
| 13-02-01 | 02 | 1 | CLOUD-01, CLOUD-04 | V5 | codec round trip; unknown fields ignored; higher schemaVersion → null; no board/move field in DTO; trim keeps Classic + ladder | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*CloudProgressCodecTest*"` | ❌ W0 | ⬜ pending |
| 13-02-02 | 02 | 1 | CLOUD-01, CLOUD-02, CLOUD-04 | V5 | import lands bests + ladder, never regresses, idempotent, zero `play_session` rows, resume rows untouched | unit (Robolectric + Turbine) | `./gradlew :app:testDebugUnitTest --tests "*ProgressImportTest*"` | ❌ W0 | ⬜ pending |
| 13-03-01 | 03 | 2 | CLOUD-05 | V7 | load null / save false / throwing store → local untouched, pending marker set, CancellationException rethrown | unit (`:app`, fake store) | `./gradlew :app:testDebugUnitTest --tests "*ProgressSubmitterCloudTest*"` | ❌ W0 | ⬜ pending |
| 13-03-02 | 03 | 2 | CLOUD-03 | — | description non-blank, capped, locale-sourced; playedTime = SUM(elapsedMs) | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*CloudSaveDescriptionFormatterTest*"` | ❌ W0 | ⬜ pending |
| 13-04-01 | 04 | 2 | CLOUD-02 (D-12..D-15) | V4 | mismatched player id → Home flag, zero store calls; Replace wipes level_result/play_session/ladder/both resume rows; dismiss == Keep | unit (Robolectric + ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*HomeViewModelCloudTest*"` | ❌ W0 | ⬜ pending |
| 13-0X-XX | any | any | PLAT-04 | — | no gms type escapes `..platform..`; `:domain` Android-free | arch | `./gradlew :app:testDebugUnitTest --tests "*PlatformBoundaryTest*" --tests "*PlatformBoundaryGateTest*" --tests "*DomainPurityTest*"` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*
*Task IDs above are provisional until PLAN.md files exist; the planner's plan/wave layout is authoritative.*

---

## Wave 0 Requirements

- [ ] `domain/src/test/kotlin/.../progress/ProgressMergeTest.kt` — stubs for CLOUD-02 laws + D-05 trim policy
- [ ] `app/src/test/.../data/cloud/CloudProgressCodecTest.kt` — stubs for CLOUD-01 / CLOUD-04 / D-05 / D-07
- [ ] `app/src/test/.../data/ProgressImportTest.kt` — stubs for CLOUD-01 / CLOUD-02 / CLOUD-04 import behaviour
- [ ] `app/src/test/.../data/ProgressSubmitterCloudTest.kt` — stubs for CLOUD-05 + D-01 / D-02 / D-12 orchestration
- [ ] `app/src/test/.../platform/games/save/FakeCloudSaveStore.kt` — shared record-and-assert fake (modelled on `FakeGamesService`)
- [ ] `app/src/test/.../platform/games/save/CloudSaveDescriptionFormatterTest.kt` — stubs for CLOUD-03
- [ ] `app/src/test/.../ui/home/HomeViewModelCloudTest.kt` — stubs for D-12..D-15 dialog state
- No framework install needed — the full stack is already wired.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Restore on a new device | CLOUD-01 | Needs a real Play Games account and the Play Console "Enable Saved Games" toggle | `11-HUMAN-UAT.md` test 10: solve on `Pixel_9a`, fresh-install on `Pixel_9`, same tester; catalog stars and Challenge tile restore silently |
| Two-device union | CLOUD-02 | Needs two signed-in devices | `11-HUMAN-UAT.md` test 11: disjoint solves on both AVDs, relaunch both, assert union on both |
| Legible saves-list entry | CLOUD-03 | Rendered by the Play Games app, not ours | `11-HUMAN-UAT.md` test 12: Play Games app → this game → Saved games shows description, played time, cover |
| Network kill degrades silently | CLOUD-05 | Needs real SDK failure path | `11-HUMAN-UAT.md` test 13: `adb shell svc wifi disable && adb shell svc data disable`, solve, relaunch, reconnect, reconverge |
| Profile-swap dialog (D-12..D-15) | CLOUD-02 | Needs a second Google account that has never authorised the game | Recorded as a UAT gap beside Phase 11 test 2 |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 120s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
