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

> Reconciled against the shipped plan layout: 11 plans across 5 waves. Plan 13-11 task 3 refreshes
> the Status / File Exists columns and the frontmatter once the phase gate and the device pass run.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 13-01-01 | 01 | 1 | CLOUD-02 | T-13-01, T-13-02 | merge is commutative, associative, idempotent, identity-respecting, never regresses; trim keeps Classic rows + ladder | unit (`:domain`) | `./gradlew :domain:test --tests "*ProgressMergeTest*"` | ❌ created by task | ⬜ pending |
| 13-01-02 | 01 | 1 | CLOUD-02 | T-13-03 | local `recordResult` behaviour unchanged after the extraction; regression net unedited | unit (Robolectric) | `./gradlew :app:testDebugUnitTest --tests "*ProgressRepositoryTest*" --tests "*ProgressRepositoryFailureTest*"` | ✅ | ⬜ pending |
| 13-02-01 | 02 | 1 | CLOUD-05, CLOUD-02 | T-13-05 | new `play_games_*` keys declared once, fail-open directions argued | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*PlayGamesOptInRepositoryTest*" --tests "*SubmissionSyncStoreTest*"` | ✅ | ⬜ pending |
| 13-02-02 | 02 | 1 | CLOUD-05, CLOUD-02 | T-13-04, T-13-06 | push-pending marker persists; unreadable stored player id reads as "no stored id" (merge silently), never as "show the destructive dialog" | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*CloudSyncStoreTest*" --tests "*CloudProfileStoreTest*"` | ❌ created by task | ⬜ pending |
| 13-03-01 | 03 | 1 | CLOUD-01..05 | T-13-07 | operator checklist names the Saved Games toggle, its default-off state and its silent-failure symptom; nothing pre-ticked | CLI (doc gate) | `grep -c "Enable Saved Games" .planning/phases/13-cloud-save/13-OPERATOR-CHECKLIST.md; grep -c "x\]" .planning/phases/13-cloud-save/13-OPERATOR-CHECKLIST.md` | ❌ created by task | ⬜ pending |
| 13-03-02 | 03 | 1 | CLOUD-01..05 | T-13-08, T-13-09 | tests 10-13 authored unrun; no existing passing result rewritten; profile-swap gap recorded | CLI (doc gate) | `grep -c "^### 1[0-3]\." .planning/phases/11-play-games-session/11-HUMAN-UAT.md; grep -c "^total: 13" .planning/phases/11-play-games-session/11-HUMAN-UAT.md; grep -c "profile-swap" .planning/phases/11-play-games-session/11-HUMAN-UAT.md` | ✅ | ⬜ pending |
| 13-04-01 | 04 | 2 | CLOUD-01, CLOUD-04, CLOUD-05 | T-13-10, T-13-11, T-13-12, T-13-14 | round trip; unknown fields ignored; higher schemaVersion decodes to null; hostile rows dropped/clamped at the decode boundary; DTO descriptor carries no board/move/resume field | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*CloudProgressCodecTest*"` | ❌ created by task | ⬜ pending |
| 13-04-02 | 04 | 2 | CLOUD-01, CLOUD-05 | T-13-13 | encode-measure-trim terminates and fits; Classic rows and the ladder always survive | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*CloudProgressCodecTest*"` | ❌ created by task | ⬜ pending |
| 13-05-01 | 05 | 2 | CLOUD-01, CLOUD-03 | — | additive DAO members, `SUM(elapsedMs)` non-null on an empty table, awaitable `mergeLadder`/`settledLevel`, no Room schema bump | unit (Robolectric) | `./gradlew :app:testDebugUnitTest --tests "*ChallengeProgressTest*" --tests "*MigrationTest*" --tests "*PegSolitaireDatabaseTest*"` | ✅ | ⬜ pending |
| 13-05-02 | 05 | 2 | CLOUD-01, CLOUD-02, CLOUD-04 | T-13-15, T-13-16, T-13-17, T-13-18, T-13-19 | import lands bests + ladder, never regresses, idempotent; zero `play_session` rows written; both resume rows byte-identical; returns `ImportOutcome`, never `NewBests` | unit (Robolectric + Turbine) | `./gradlew :app:testDebugUnitTest --tests "*ProgressImportTest*" --tests "*ProgressRepositoryTest*" --tests "*ProgressRepositoryFailureTest*"` | ❌ created by task | ⬜ pending |
| 13-06-01 | 06 | 2 | CLOUD-01, CLOUD-05 | T-13-20, T-13-21, T-13-23 | no gms type or Activity in any cloud-save signature; `@Binds` binds the seam; failure is a return value | arch | `./gradlew :app:testDebugUnitTest --tests "*PlatformBoundaryTest*" --tests "*PlatformBoundaryGateTest*" --tests "*DomainPurityTest*"` | ✅ | ⬜ pending |
| 13-06-02 | 06 | 2 | CLOUD-01, CLOUD-05 | T-13-22 | snapshot name matches the SDK filename charset; retry bound positive; stub degrades silently; fake invokes `mergeRemote` | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*NoOpCloudSaveStoreTest*" --tests "*CloudSaveConstantsTest*"` | ❌ created by task | ⬜ pending |
| 13-07-01 | 07 | 3 | CLOUD-03 | T-13-24, T-13-25 | description non-blank, single-line, locale-sourced, unconditionally capped; denominator derived from `ClassicBoard.entries.size` | unit (Robolectric) | `./gradlew :app:testDebugUnitTest --tests "*CloudSaveDescriptionFormatterTest*"` | ❌ created by task | ⬜ pending |
| 13-07-02 | 07 | 3 | CLOUD-03 | T-13-26, T-13-27 | cover rasterizes to COVER_PX square, caches, is non-blank, compresses well under 800 KB, and degrades to null | unit (Robolectric) | `./gradlew :app:testDebugUnitTest --tests "*CoverImageSourceTest*"` | ❌ created by task | ⬜ pending |
| 13-08-01 | 08 | 3 | CLOUD-01, CLOUD-02, CLOUD-05 | T-13-28, T-13-29, T-13-33 | profile gate precedes the pull; pull runs exactly once per process and is not gated on the dirty flag; mismatched player id performs zero store calls | unit (`:app`, virtual time + fakes) | `./gradlew :app:testDebugUnitTest --tests "*ProgressSubmitterCloudTest*" --tests "*ProgressSubmitterTest*"` | ❌ created by task | ⬜ pending |
| 13-08-02 | 08 | 3 | CLOUD-01, CLOUD-03, CLOUD-05 | T-13-30 | push carries description + playedTime; `save` false or throwing sets the pending marker; the conflict lambda is `CloudProgressCodec::mergeBytes`; the push reads a settled ladder | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*ProgressSubmitterCloudTest*"` | ❌ created by task | ⬜ pending |
| 13-08-03 | 08 | 3 | CLOUD-02 (D-13/D-14) | T-13-31, T-13-32 | Keep merges; Use-cloud wipes `level_result`, `play_session`, the ladder and BOTH resume snapshots then imports; either answer stores the new player id | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*ProgressSubmitterCloudTest*"` | ❌ created by task | ⬜ pending |
| 13-09-01 | 09 | 4 | CLOUD-01, CLOUD-02, CLOUD-05 | T-13-34, T-13-35, T-13-36, T-13-37, T-13-38, T-13-40 | opens with RESOLUTION_POLICY_MANUAL only; bounded conflict loop with pinned orientation; `writeBytes` never `modifyBytes`; read path discards, never commits | unit (`:app`, MockK over the SDK) | `./gradlew :app:testDebugUnitTest --tests "*PlayGamesCloudSaveStoreTest*"` | ❌ created by task | ⬜ pending |
| 13-09-02 | 09 | 4 | CLOUD-03, CLOUD-05 | T-13-39, T-13-41 | commit carries description + playedTime + cover; exactly one terminal call per opened snapshot; runtime `getMaxDataSize()` guard; binding flipped to the real store | unit (`:app`) + arch | `./gradlew :app:testDebugUnitTest --tests "*PlayGamesCloudSaveStoreTest*" --tests "*PlatformBoundaryTest*" --tests "*PlatformBoundaryGateTest*"` | ❌ created by task | ⬜ pending |
| 13-10-01 | 10 | 4 | CLOUD-02 (D-13) | T-13-42, T-13-43, T-13-44 | dismissal equals Keep through one assignment; gradient CTA on the safe choice; body enumerates what is replaced | Compose UI test | `./gradlew :app:testDebugUnitTest --tests "*ProfileSwapDialogTest*"` | ❌ created by task | ⬜ pending |
| 13-10-02 | 10 | 4 | CLOUD-02 (D-12/D-15) | T-13-45, T-13-46 | dialog visibility is a pure function of three sequenced inputs, never stacked; neither handler uses `viewModelScope` | unit (ViewModel + Turbine) | `./gradlew :app:testDebugUnitTest --tests "*HomeViewModelCloudTest*" --tests "*HomeViewModelTest*"` | ❌ created by task | ⬜ pending |
| 13-10-03 | 10 | 4 | CLOUD-02 (D-15) | T-13-45 | Home hosts the dialog as a third sibling conditional and never renders two dialogs together | Compose UI test | `./gradlew :app:testDebugUnitTest --tests "*HomeScreenTest*"` | ✅ file exists, cases ❌ | ⬜ pending |
| 13-11-01 | 11 | 5 | CLOUD-01..05 | T-13-47, T-13-SC, T-13-49 | full gate green; minified release needs no new snapshot keep rules; zero new dependency coordinates; no Room bump | build + CLI | `./gradlew :domain:test :app:testDebugUnitTest && ./gradlew check` | n/a | ⬜ pending |
| 13-11-02 | 11 | 5 | CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-05 | T-13-48 | two-emulator device pass executed and recorded honestly (blocking human checkpoint) | manual | — (see Manual-Only Verifications) | n/a | ⬜ pending |
| 13-11-03 | 11 | 5 | CLOUD-01..05 | T-13-48 | this document reconciled to the shipped layout; no provisional task ids remain | CLI (doc gate) | `grep -c "provisional" .planning/phases/13-cloud-save/13-VALIDATION.md; grep -c "13-11-" .planning/phases/13-cloud-save/13-VALIDATION.md` | ✅ | ⬜ pending |
| 13-0X-XX | any | any | PLAT-04 | T-13-20, T-13-41 | no gms type escapes `..platform..`; `:domain` stays Android-free | arch | `./gradlew :app:testDebugUnitTest --tests "*PlatformBoundaryTest*" --tests "*PlatformBoundaryGateTest*" --tests "*DomainPurityTest*"` | ✅ | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*
*"File Exists: ❌ created by task" means the test file does not exist yet and the task that owns the row creates it alongside its production code; the automated command is therefore valid at task completion. No task in this phase depends on a test file another task was supposed to create.*

---

## Wave 0 Requirements

Every missing test file is created by the task that owns its behaviour, in the same commit as the
production code it verifies — so there is no separate Wave 0 and no task whose `<automated>` command
references a file another task was supposed to create first. The owning task is named beside each:

- [ ] `domain/src/test/kotlin/.../progress/ProgressMergeTest.kt` — CLOUD-02 laws + D-05 trim policy (13-01 task 1)
- [ ] `app/src/test/.../data/CloudSyncStoreTest.kt` and `CloudProfileStoreTest.kt` — D-01 marker, D-12 stored id (13-02 task 2)
- [ ] `app/src/test/.../data/cloud/CloudProgressCodecTest.kt` — CLOUD-01 / CLOUD-04 / D-05 / D-07 (13-04 tasks 1 and 2)
- [ ] `app/src/test/.../data/ProgressImportTest.kt` — CLOUD-01 / CLOUD-02 / CLOUD-04 import behaviour (13-05 task 2)
- [ ] `app/src/test/.../platform/games/save/FakeCloudSaveStore.kt` — shared record-and-assert fake, modelled on `FakeGamesService` (13-06 task 2)
- [ ] `app/src/test/.../platform/games/save/NoOpCloudSaveStoreTest.kt` and `CloudSaveConstantsTest.kt` — seam contract + snapshot-name charset (13-06 task 2)
- [ ] `app/src/test/.../platform/games/save/CloudSaveDescriptionFormatterTest.kt` — CLOUD-03 description (13-07 task 1)
- [ ] `app/src/test/.../platform/games/save/CoverImageSourceTest.kt` — CLOUD-03 cover (13-07 task 2)
- [ ] `app/src/test/.../data/ProgressSubmitterCloudTest.kt` — CLOUD-05 + D-01 / D-02 / D-12 / D-14 orchestration (13-08 tasks 1-3)
- [ ] `app/src/test/.../platform/games/save/PlayGamesCloudSaveStoreTest.kt` — MockK over the SDK: open / conflict / commit / close (13-09 tasks 1 and 2)
- [ ] `app/src/test/.../ui/home/ProfileSwapDialogTest.kt` — D-13 dismissal and emphasis (13-10 task 1)
- [ ] `app/src/test/.../ui/home/HomeViewModelCloudTest.kt` — D-12..D-15 dialog state (13-10 task 2)
- [ ] New cases in the existing `HomeScreenTest.kt` (dialog never stacks, 13-10 task 3); `MigrationTest` is **not** touched (no schema change)
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
