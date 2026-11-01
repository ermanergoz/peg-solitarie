---
phase: 13
slug: cloud-save
status: complete
nyquist_compliant: true
wave_0_complete: true
created: 2026-09-15
updated: 2026-09-20
---

# Phase 13 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.
> **Reconciled on 2026-09-20 by plan 13-11 task 3** against the work that actually shipped. Where an
> earlier revision of this document said something now known to be false, it has been corrected and
> the correction is named.

---

## What actually shipped

**Eleven plans across five waves**, exactly as planned:

| Wave | Plans |
|------|-------|
| 1 | 13-01 (pure `:domain` merge), 13-02 (DataStore markers), 13-03 (operator checklist + UAT script) |
| 2 | 13-04 (codec), 13-05 (repository import/export), 13-06 (platform seam) |
| 3 | 13-07 (listing metadata), 13-08 (`ProgressSubmitter` orchestration) |
| 4 | 13-09 (`SnapshotsClient` plumbing), 13-10 (profile-swap dialog) |
| 5 | 13-11 (build gate + two-device pass) |

**Plus a twelfth body of work that was never planned.** It is committed under the scope tag `13-12`
and it has **no `13-12-PLAN.md`** — it does not exist as a plan file and this document does not pretend
it does. It was opened in response to a defect the device pass found: 13-11 task 2's first attempt
recorded three blocked tests and one partial, and the follow-up diagnosis found that **every** cloud
push and pull was dying on the SDK's no-conflict path
(`.planning/reports/phase-13/qa-engineer-save-reason.md`). What followed was dispatched from tech-lead
reports rather than from a plan, in three review loops:

1. the cloud-save **outcome / reason code** (`CloudSaveOutcome` stage + status code persisted to
   DataStore), specified by `tech-lead-save-path-observability.md` § B;
2. the **`getConflict()` fix** — `DataOrConflict.getConflict()` throws when `isConflict()` is false, so
   the store's `result.conflict ?: break` killed every pass;
3. the **convergence build** — the 384 px cover, the post-pull push guard, the pull probe, the
   debug-only snapshot picker, and mutation-pinning of auditor findings F-1 and F-2.

13-12's rows are kept in their own table below rather than mixed into the Per-Task Verification Map,
because the map's contract is that every Task ID resolves to a real plan file and every Automated
Command is copied verbatim from that task's own `<verify>` block. 13-12 has neither; its commands are
quoted from the reports that ran them, and the table says so.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 4.13.2 + Truth 1.4.4 + Turbine 1.2.1 + MockK 1.14.2 + kotlinx-coroutines-test 1.11.0; Robolectric 4.16 for Room/Android-runtime JVM tests; ArchUnit 1.3.0 for the boundary gates |
| **Config file** | none for JUnit4; detekt at `config/detekt/detekt.yml` |
| **Quick run command** | `./gradlew :domain:test --tests "*ProgressMergeTest*"` (domain) / `./gradlew :app:testDebugUnitTest --tests "*<ClassName>*"` (app) |
| **Full suite command** | `./gradlew :domain:test :app:testDebugUnitTest` |
| **Measured runtime** | full JVM suite ≈ 36 s–90 s; `./gradlew check` **11m07s** as run by the producer at `9211416` |
| **Suite size at phase end** | **1194 tests, 0 failures, 0 errors, 0 skipped** at `f30de3d` (`.planning/reports/phase-13/android-game-dev-convergence-build.md`) |

---

## Sampling Rate

- **After every task commit:** Run the narrowest `--tests` filter for the class touched (see quick run command)
- **After every plan wave:** Run `./gradlew :domain:test :app:testDebugUnitTest`
- **Before `/gsd:verify-work`:** Full suite green, plus `./gradlew check` and `./gradlew :app:assembleRelease` with no `com.google.android.gms.games` entries in `app/build/outputs/mapping/release/missing_rules.txt`
- **Max feedback latency:** 120 seconds

---

## Per-Task Verification Map

One row per task of the eleven shipped plans. Every **Automated Command** is copied verbatim from that
task's own `<verify><automated>` block in its plan file; nothing here was invented. Every **Task ID**
resolves to a `.planning/phases/13-cloud-save/13-NN-PLAN.md` that exists.

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 13-01-01 | 01 | 1 | CLOUD-02 | T-13-01, T-13-02 | merge is commutative, associative, idempotent, identity-respecting, never regresses; trim keeps Classic rows + ladder | unit (`:domain`) | `./gradlew :domain:test --tests "*ProgressMergeTest*"` | ✅ | ✅ green |
| 13-01-02 | 01 | 1 | CLOUD-02 | T-13-03 | local `recordResult` behaviour unchanged after the extraction; regression net unedited | unit (Robolectric) | `./gradlew :app:testDebugUnitTest --tests "*ProgressRepositoryTest*" --tests "*ProgressRepositoryFailureTest*"` | ✅ | ✅ green |
| 13-02-01 | 02 | 1 | CLOUD-05, CLOUD-02 | T-13-05 | new `play_games_*` keys declared once, fail-open directions argued | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*PlayGamesOptInRepositoryTest*" --tests "*SubmissionSyncStoreTest*"` | ✅ | ✅ green |
| 13-02-02 | 02 | 1 | CLOUD-05, CLOUD-02 | T-13-04, T-13-06 | push-pending marker persists; unreadable stored player id reads as "no stored id" (merge silently), never as "show the destructive dialog" | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*CloudSyncStoreTest*" --tests "*CloudProfileStoreTest*"` | ✅ | ✅ green |
| 13-03-01 | 03 | 1 | CLOUD-01..05 | T-13-07 | operator checklist names the Saved Games toggle, its default-off state and its silent-failure symptom | CLI (doc gate) | `grep -c "Enable Saved Games" .planning/phases/13-cloud-save/13-OPERATOR-CHECKLIST.md; grep -c "x\]" .planning/phases/13-cloud-save/13-OPERATOR-CHECKLIST.md` | ✅ | ✅ green (2 / 2 — authored unticked, ticked by the 13-11 task 2 operator run) |
| 13-03-02 | 03 | 1 | CLOUD-01..05 | T-13-08, T-13-09 | tests 10-13 authored unrun; no existing passing result rewritten; profile-swap gap recorded | CLI (doc gate) | `grep -c "^### 1[0-3]\." .planning/phases/11-play-games-session/11-HUMAN-UAT.md; grep -c "^total: 13" .planning/phases/11-play-games-session/11-HUMAN-UAT.md; grep -c "profile-swap" .planning/phases/11-play-games-session/11-HUMAN-UAT.md` | ✅ | ✅ green (4 / 1 / 1) |
| 13-04-01 | 04 | 2 | CLOUD-01, CLOUD-04, CLOUD-05 | T-13-10, T-13-11, T-13-12, T-13-14 | round trip; unknown fields ignored; higher schemaVersion decodes to null; hostile rows dropped/clamped at the decode boundary; DTO descriptor carries no board/move/resume field | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*CloudProgressCodecTest*"` | ✅ | ✅ green |
| 13-04-02 | 04 | 2 | CLOUD-01, CLOUD-05 | T-13-13 | encode-measure-trim terminates and fits; Classic rows and the ladder always survive | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*CloudProgressCodecTest*"` | ✅ | ✅ green |
| 13-05-01 | 05 | 2 | CLOUD-01, CLOUD-03 | — (13-05's register maps T-13-15..19 to task 2) | additive DAO members, `SUM(elapsedMs)` non-null on an empty table, awaitable `mergeLadder`/`settledLevel`, no Room schema bump | unit (Robolectric) | `./gradlew :app:testDebugUnitTest --tests "*ChallengeProgressTest*" --tests "*MigrationTest*" --tests "*PegSolitaireDatabaseTest*"` | ✅ | ✅ green |
| 13-05-02 | 05 | 2 | CLOUD-01, CLOUD-02, CLOUD-04 | T-13-15, T-13-16, T-13-17, T-13-18, T-13-19 | import lands bests + ladder, never regresses, idempotent; zero `play_session` rows written; both resume rows byte-identical; returns `ImportOutcome`, never `NewBests` | unit (Robolectric + Turbine) | `./gradlew :app:testDebugUnitTest --tests "*ProgressImportTest*" --tests "*ProgressRepositoryTest*" --tests "*ProgressRepositoryFailureTest*"` | ✅ | ✅ green |
| 13-06-01 | 06 | 2 | CLOUD-01, CLOUD-05 | T-13-20, T-13-21, T-13-23 | no gms type or Activity in any cloud-save signature; `@Binds` binds the seam; failure is a return value | arch | `./gradlew :app:testDebugUnitTest --tests "*PlatformBoundaryTest*" --tests "*PlatformBoundaryGateTest*" --tests "*DomainPurityTest*"` | ✅ | ✅ green |
| 13-06-02 | 06 | 2 | CLOUD-01, CLOUD-05 | T-13-22 | snapshot name matches the SDK filename charset; retry bound positive; stub degrades silently; fake invokes `mergeRemote` | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*NoOpCloudSaveStoreTest*" --tests "*CloudSaveConstantsTest*"` | ✅ | ✅ green |
| 13-07-01 | 07 | 3 | CLOUD-03 | T-13-24, T-13-25 | description non-blank, single-line, locale-sourced, unconditionally capped; denominator derived from `ClassicBoard.entries.size` | unit (Robolectric) | `./gradlew :app:testDebugUnitTest --tests "*CloudSaveDescriptionFormatterTest*"` | ✅ | ✅ green |
| 13-07-02 | 07 | 3 | CLOUD-03 | T-13-26, T-13-27 | cover rasterizes to COVER_PX square, caches, is non-blank, compresses well under 800 KB, and degrades to null | unit (Robolectric) | `./gradlew :app:testDebugUnitTest --tests "*CoverImageSourceTest*"` | ✅ | ✅ green (`COVER_PX` later corrected 512 → 384 by 13-12; see the 13-12 table) |
| 13-08-01 | 08 | 3 | CLOUD-01, CLOUD-02, CLOUD-05 | T-13-28, T-13-29, T-13-33 | profile gate precedes the pull; pull runs exactly once per process and is not gated on the dirty flag; mismatched player id performs zero store calls | unit (`:app`, virtual time + fakes) | `./gradlew :app:testDebugUnitTest --tests "*ProgressSubmitterCloudTest*" --tests "*ProgressSubmitterTest*"` | ✅ | ✅ green |
| 13-08-02 | 08 | 3 | CLOUD-01, CLOUD-03, CLOUD-05 | T-13-30 | push carries description + playedTime; `save` false or throwing sets the pending marker; the conflict lambda is `CloudProgressCodec::mergeBytes`; the push reads a settled ladder | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*ProgressSubmitterCloudTest*"` | ✅ | ✅ green (`save`'s return type later widened Boolean → `CloudSaveOutcome` by 13-12) |
| 13-08-03 | 08 | 3 | CLOUD-02 (D-13/D-14) | T-13-31, T-13-32 | Keep merges; Use-cloud wipes `level_result`, `play_session`, the ladder and BOTH resume snapshots then imports; either answer stores the new player id | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*ProgressSubmitterCloudTest*"` | ✅ | ✅ green |
| 13-09-01 | 09 | 4 | CLOUD-01, CLOUD-02, CLOUD-05 | T-13-34, T-13-35, T-13-36, T-13-37, T-13-38, T-13-40 | opens with RESOLUTION_POLICY_MANUAL only; bounded conflict loop with pinned orientation; `writeBytes` never `modifyBytes`; read path discards, never commits | unit (`:app`, MockK over the SDK) | `./gradlew :app:testDebugUnitTest --tests "*PlayGamesCloudSaveStoreTest*"` | ✅ | ✅ green (shipped with the `getConflict()` defect; fixed under 13-12) |
| 13-09-02 | 09 | 4 | CLOUD-03, CLOUD-05 | T-13-39, T-13-41 | commit carries description + playedTime + cover; exactly one terminal call per opened snapshot; runtime `getMaxDataSize()` guard; binding flipped to the real store | unit (`:app`) + arch | `./gradlew :app:testDebugUnitTest --tests "*PlayGamesCloudSaveStoreTest*" --tests "*PlatformBoundaryTest*" --tests "*PlatformBoundaryGateTest*"` | ✅ | ✅ green |
| 13-10-01 | 10 | 4 | CLOUD-02 (D-13) | T-13-42, T-13-43, T-13-44 | dismissal equals Keep through one assignment; gradient CTA on the safe choice; body enumerates what is replaced | Compose UI test | `./gradlew :app:testDebugUnitTest --tests "*ProfileSwapDialogTest*"` | ✅ | ✅ green |
| 13-10-02 | 10 | 4 | CLOUD-02 (D-12/D-15) | T-13-45, T-13-46 | dialog visibility is a pure function of three sequenced inputs, never stacked; neither handler uses `viewModelScope` | unit (ViewModel + Turbine) | `./gradlew :app:testDebugUnitTest --tests "*HomeViewModelCloudTest*" --tests "*HomeViewModelTest*"` | ✅ | ✅ green |
| 13-10-03 | 10 | 4 | CLOUD-02 (D-15) | T-13-45 | Home hosts the dialog as a third sibling conditional and never renders two dialogs together | Compose UI test | `./gradlew :app:testDebugUnitTest --tests "*HomeScreenTest*"` | ✅ | ✅ green |
| 13-11-01 | 11 | 5 | CLOUD-01..05 | T-13-47, T-13-SC, T-13-49 | full gate green; minified release needs no new snapshot keep rules; zero new dependency coordinates; no Room bump | build + CLI | `./gradlew :domain:test :app:testDebugUnitTest && ./gradlew check` | n/a | ✅ green (producer-run at `9211416`, 11m07s) |
| 13-11-02 | 11 | 5 | CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-05 | T-13-48 | two-emulator device pass executed and recorded honestly (blocking human checkpoint) | manual + CLI | `grep -c "result: pending" .planning/phases/11-play-games-session/11-HUMAN-UAT.md` | ✅ | ✅ green (returns 0; see Manual-Only Verifications) |
| 13-11-03 | 11 | 5 | CLOUD-01..05 | T-13-48 | this document reconciled to the shipped layout; no placeholder task ids remain | CLI (doc gate) | `grep -c "provisional" .planning/phases/13-cloud-save/13-VALIDATION.md; grep -c "13-11-" .planning/phases/13-cloud-save/13-VALIDATION.md` | ✅ | ⚠️ 1 / 5 — see the note below |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

**What "✅ green" rests on.** Each row's `--tests` filter selects a subset of
`./gradlew :domain:test :app:testDebugUnitTest`, which ran **green at `f30de3d`: 1194 tests, 0 failures,
0 errors, 0 skipped** (`.planning/reports/phase-13/android-game-dev-convergence-build.md`). The rows
were not each re-invoked individually at that commit; they were each run green by the task that owns
them, and the full suite that contains all of them is green on the phase's final tree. The two doc-gate
rows and 13-11-02 were re-run on 2026-09-20 during this reconcile and their outputs are quoted in the
Status cell. `./gradlew check` in full is the producer's run at `9211416` — see the Approval line for
what that does and does not cover.

**Corrections to the previous revision of this table.**
- The footnote that called the Task IDs above only tentative is deleted; it is no longer true.
- The catch-all row `13-0X-XX | any | any | PLAT-04` is deleted. Its Task ID resolved to no plan file
  and no `<verify>` block, so it could not satisfy the map's own contract. PLAT-04 is covered by
  13-06-01 and 13-09-02, which run exactly the arch commands that row carried, and by the phase-wide
  assertions below.
- The File Exists column no longer says "❌ created by task" anywhere: every file it referred to now
  exists on disk (checked file by file on 2026-09-20).
- **Two of 13-11 task 3's own acceptance criteria cannot both be satisfied, and this is the resolution.**
  The plan requires every Automated Command to be copied verbatim, *and* requires row 13-11-03's own
  first grep to return 0 — but that grep searches this file for the very word it is written with, so
  copying it verbatim guarantees one hit. The verbatim rule wins: the count is **1**, and the single hit
  is that self-reference inside the command cell. Every other use of the word, including the footnote
  the plan asked to delete, is gone. The command's second half, `grep -c "13-11-"`, returns 5.

---

## Unplanned work: 13-12 (no plan file)

Added during the phase in response to a device-found defect. There is no `13-12-PLAN.md`, therefore no
`<verify>` block and no threat register of its own. Commands in this table are **quoted verbatim from
the report that ran them**, named in the last column, not from a plan.

| Task ID | Source of the work | Requirement | Carried threats / findings | Secure Behavior | Test Type | Automated Command | Status | Command source |
|---------|--------------------|-------------|---------------------------|-----------------|-----------|-------------------|--------|----------------|
| 13-12 (a) | `tech-lead-save-path-observability.md` § B | CLOUD-05 | T-13-30, T-13-33, T-13-40 | `save` reports an SDK-free `CloudSaveOutcome(stage, statusCode)` instead of a bare Boolean; the stage and status are persisted in one DataStore edit; no log line, no UI, no reader | unit (`:app`) | `./gradlew :domain:test :app:testDebugUnitTest` | ✅ green (873 app + 268 domain, 0 failures) | `android-game-dev-cloud-save-outcome.md` |
| 13-12 (b) | `qa-engineer-save-reason.md` (root cause) | CLOUD-01, CLOUD-02, CLOUD-05 | T-13-34, T-13-36, T-13-37 | `resolveToOpenSnapshot` discriminates with `isConflict()` before reading either side; fixtures are real `DataOrConflict` / `SnapshotConflict` instances so the spec cannot disagree with the pinned SDK again | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*PlayGamesCloudSaveStoreTest*" --tests "*CloudSaveOutcomeTest*"` | ✅ green at `12514cb` (51 tests); **red at `77817e7`: 29 of 47 failed** | `android-game-dev-getconflict-fix.md` |
| 13-12 (c) | `tech-lead-non-convergence-triage.md` (dev task) | CLOUD-03 | T-13-26, T-13-27 | the cover's **raw ARGB** size, not its PNG size, is what GMS accounts against the 819,200-byte cap; `COVER_PX` 384 → 589,824 bytes, pinned by a test | unit (Robolectric) | `./gradlew :app:testDebugUnitTest --max-workers=2 --tests "*CloudSave*" --tests "*ProgressSubmitter*" --tests "*CloudPull*" --tests "*SnapshotPicker*" --tests "*CoverImage*" --tests "*CloudSyncStore*"` | ✅ green (181 tests, 0 failures, 0 skipped) | `code-auditor-convergence-build.md` |
| 13-12 (d) | `tech-lead-non-convergence-triage.md` (dev task) | CLOUD-02, CLOUD-05 | T-13-30 | a pass that pulled pushes only if `ProgressMerge.merge(remote, localExport) != remote`; `land()` no longer arms the push marker; every failed push still arms the retry marker | unit (`:app`) | (as 13-12-c) | ✅ green; mutation round 2 killed the guard's 5 gates | `code-auditor-convergence-build.md` |
| 13-12 (e) | `tech-lead-non-convergence-triage.md` (dev task) | CLOUD-05 | T-13-40 | `load` returns `CloudPull(bytes, probe)`; the probe holds Boolean/Int/Long only and is persisted write-only in one edit — no PII, nothing user-visible | unit (`:app`) | (as 13-12-c) | ✅ green | `code-auditor-convergence-build.md` |
| 13-12 (f) | `tech-lead-non-convergence-triage.md` (dev task) | CLOUD-03 | T-13-41 | the debug snapshot picker is gated by `BuildConfig.DEBUG`; a non-debug build never even reads the intent extra; the picker runs inside `platform/` and the gms boundary still holds | unit (`:app`) + arch | `./gradlew :app:testDebugUnitTest --max-workers=2 --tests "*PlatformBoundary*"` | ✅ green (6 tests) | `code-auditor-convergence-build.md` |
| 13-12 (g) | `code-auditor-getconflict-fix.md` F-1, F-2 | CLOUD-01, CLOUD-05 | F-1, F-2 | the `NonCancellable` cleanup and metadata-before-open are pinned by tests that fail when the behaviour is removed | unit (`:app`) + mutation | `./gradlew --max-workers=2 :domain:test :app:testDebugUnitTest` | ✅ green (**1194 tests**); mutation round 1 killed exactly the 2 intended tests | `android-game-dev-convergence-build.md`, `code-auditor-convergence-build.md` |

**Review verdicts on 13-12.** code-auditor **PASS** on the outcome diff
(`code-auditor-cloud-save-outcome.md`), **PASS** on the `getConflict` fix
(`code-auditor-getconflict-fix.md`, seven Low/Info), **PASS** on the convergence build
(`code-auditor-convergence-build.md`, four Low/Info, none gating). tech-lead **PASS** with sign-off on
the convergence build (`tech-lead-conflict-wedge.md` § D), conditional on the producer's full
`./gradlew check`. No suppression, no baseline, no `@Ignore`, no skipped test and no new dependency
coordinate was added anywhere in 13-12.

**What 13-12 did not close.** Open follow-ups are listed in `13-12-SUMMARY.md` § Follow-ups; the
load-bearing ones are auditor F-3 (superseded conflict pairs — the tech-lead specified the fix and
deliberately held it until after experiment E3), F-4 (the give-up path closes one of two snapshots, left
by design and pinned by test), L-1/L-2/L-3, and the tech-lead's A(iii) note (the push path never lands
what it saw).

---

## Phase-wide source assertions

Run verbatim as 13-11 plan task 1 § 4 specifies them, re-run on 2026-09-20 during this reconcile.
**Two of the plan's expected counts were wrong as written.** The properties they were checking do hold;
the greps do not express them.

| Assertion | Command (verbatim from 13-11-PLAN.md task 1) | Expected by the plan | Actual | Verdict |
|---|---|---|---|---|
| T-11-04 no logging in the new packages | `grep -rn "Log\.\|println\|Timber" app/src/main/java/com/app/pegsolitaire/platform/games/ app/src/main/java/com/app/pegsolitaire/data/cloud/ app/src/main/java/com/app/pegsolitaire/data/` | 0 | 0 | ✅ |
| PLAT-04 no gms outside `platform/` | `grep -rn "com.google.android.gms" app/src/main/java \| grep -v "com/app/pegsolitaire/platform/"` | 0 | **5** | ⚠️ **plan wrong, property holds** |
| ROADMAP mandate: manual resolution only | `grep -rn "RESOLUTION_POLICY_" app/src/main/java \| grep -v "RESOLUTION_POLICY_MANUAL"` | 0 | 0 | ✅ |
| Pitfall 5: never `modifyBytes` | `grep -rn "modifyBytes" app/src/main/java` | 0 | 0 | ✅ |
| D-14 wipe containment | `grep -rn "resetAllProgress()" app/src/main/java` | exactly 2 | **4** | ⚠️ **plan wrong, property holds** |
| No Room schema bump | `git diff --stat develop -- app/schemas` | empty | empty (0 lines) | ✅ |
| T-13-SC: zero new dependency coordinates | `git diff develop -- gradle/libs.versions.toml app/build.gradle.kts` | empty | empty (0 lines) | ✅ |

- **The PLAT-04 grep returns 5, not 0, and always did** — it returned 5 at base commit `659edf6`, before
  any phase-13 code existed (`android-game-dev-cloud-save-outcome.md`). The five hits are KDoc *prose*
  (`App.kt:44`, `di/PlatformModule.kt:119`) and the downloadable-fonts provider constants
  (`ui/theme/Type.kt:53,54,60`). None is an import. The property PLAT-04 actually asserts is that no gms
  **type** escapes `platform/`, and the grep that expresses it —
  `grep -rn '^import com.google.android.gms' app/src/main/java | grep -v 'com/app/pegsolitaire/platform/'`
  — returns **0**, corroborated by `PlatformBoundaryTest` / `PlatformBoundaryGateTest` over bytecode.
- **The `resetAllProgress()` grep returns 4, not 2.** The four hits are the interface declaration
  (`ProgressRepository.kt:237`), its override (`:508`), a KDoc mention (`ChallengeProgress.kt:140`), and
  the one call site (`ProgressSubmitter.kt:331`). The plan's own wording ("the declaration … and the
  single call") never accounted for the override or the KDoc. D-14 containment holds exactly as
  specified: there is **one** call to `resetAllProgress()` in the app and it is in `ProgressSubmitter`,
  on the answered-dialog path.

**R8 / assumption A5.** `app/proguard-rules.pro` is byte-identical to `develop`
(`git log develop..HEAD -- app/proguard-rules.pro` is empty): **no new keep rule was needed for the
snapshot classes**, which is the outcome assumption A5 predicted. This matches Phase 12's finding
recorded at `11-HUMAN-UAT.md` test 9 — `play-services-games-v2:22.0.0` ships no `proguard.txt` of its
own and R8 wrote no `missing_rules.txt` at all. The phase-13 `:app:assembleRelease` run belongs to the
producer's 13-11 task 1 pass; no report under `.planning/reports/phase-13/` records its console output,
so the *unchanged rules file* is the evidence quoted here rather than a fresh `missing_rules.txt`
inspection.

---

## Wave 0 Requirements

Every missing test file was created by the task that owns its behaviour, in the same commit as the
production code it verifies — so there was no separate Wave 0 and no task whose `<automated>` command
referenced a file another task was supposed to create first. All of them now exist on disk (checked
individually on 2026-09-20):

- [x] `domain/src/test/kotlin/.../progress/ProgressMergeTest.kt` — CLOUD-02 laws + D-05 trim policy — created by **13-01 task 1**
- [x] `app/src/test/.../data/CloudSyncStoreTest.kt` and `CloudProfileStoreTest.kt` — D-01 marker, D-12 stored id — created by **13-02 task 2**
- [x] `app/src/test/.../data/cloud/CloudProgressCodecTest.kt` — CLOUD-01 / CLOUD-04 / D-05 / D-07 — created by **13-04 tasks 1 and 2**
- [x] `app/src/test/.../data/ProgressImportTest.kt` — CLOUD-01 / CLOUD-02 / CLOUD-04 import behaviour — created by **13-05 task 2**
- [x] `app/src/test/.../platform/games/save/FakeCloudSaveStore.kt` — shared record-and-assert fake — created by **13-06 task 2**
- [x] `app/src/test/.../platform/games/save/NoOpCloudSaveStoreTest.kt` and `CloudSaveConstantsTest.kt` — seam contract + snapshot-name charset — created by **13-06 task 2**
- [x] `app/src/test/.../platform/games/save/CloudSaveDescriptionFormatterTest.kt` — CLOUD-03 description — created by **13-07 task 1**
- [x] `app/src/test/.../platform/games/save/CoverImageSourceTest.kt` — CLOUD-03 cover — created by **13-07 task 2**
- [x] `app/src/test/.../data/ProgressSubmitterCloudTest.kt` — CLOUD-05 + D-01 / D-02 / D-12 / D-14 orchestration — created by **13-08 tasks 1-3**
- [x] `app/src/test/.../platform/games/save/PlayGamesCloudSaveStoreTest.kt` — MockK over the SDK: open / conflict / commit / close — created by **13-09 tasks 1 and 2**
- [x] `app/src/test/.../ui/home/ProfileSwapDialogTest.kt` — D-13 dismissal and emphasis — created by **13-10 task 1**
- [x] `app/src/test/.../ui/home/HomeViewModelCloudTest.kt` — D-12..D-15 dialog state — created by **13-10 task 2**
- [x] New cases in the existing `HomeScreenTest.kt` (dialog never stacks) — added by **13-10 task 3**; `MigrationTest` was **not** touched (no schema change)
- [x] No framework install needed — the full stack was already wired; the phase added **zero** dependency coordinates.

Created later, by the unplanned 13-12 and outside the Wave 0 list:
`CloudSaveOutcomeTest.kt`, `CloudPullProbeTest.kt`, `PlayGamesCloudSaveStorePullProbeTest.kt`,
`DebugSnapshotPickerEntryTest.kt`, `PlayGamesSnapshotPickerTest.kt`.

---

## Manual-Only Verifications

Outcomes below are the **final** run recorded in `.planning/reports/phase-13/qa-engineer-uat-final.md`
and transcribed into `11-HUMAN-UAT.md` (2026-09-20, `Pixel_9_clean` + `Pixel_9b_clean`, debug build
`88c2fd7`, APK sha256 `1c480a55cc614da0`, GMS 26.34.36 on both). Earlier attempts of tests 10-13 are
recorded as **superseded** on the same result lines and are not repeated here; they ran on two emulators
whose Play services had stopped uploading — an environment fault, not an app defect.

| Behavior | Requirement | Why Manual | Test Instructions | Recorded outcome |
|----------|-------------|------------|-------------------|------------------|
| Restore on a new device | CLOUD-01 | Needs a real Play Games account and the Play Console "Enable Saved Games" toggle | `11-HUMAN-UAT.md` test 10 | **PASS.** `Pixel_9b_clean`, app-wiped and re-signed-in, restored Challenge level 11 and English-33 3/3 — content it had never held — from `Pixel_9_clean`'s confirmed 17:24:42.612 commit. All D-03 negatives held: no app toast, dialog, snackbar or Settings sync row; the only launch-time signal was the stock Play Games greeting. |
| Two-device union | CLOUD-02 | Needs two signed-in devices | `11-HUMAN-UAT.md` test 11 | **PASS; D-04 sub-check PARTIAL.** Full union in **both** directions on the first relaunch of each device, nothing lost, no prompt, zero conflicts. D-04: the in-progress board survived the merge exactly as left, the Continue tile resumed it, the ladder did not regress — but "Next level" then served level N+1 rather than the merged head. New **low-severity** defect; the earlier run could not see it because its ladder gap was exactly 1. |
| Legible saves-list entry | CLOUD-03 | Rendered by the Play Games app, not ours | `11-HUMAN-UAT.md` test 12 | **PASS, by substitute.** The Play Games app no longer has a Saved games screen, so the debug-only snapshot picker was used. Both devices list the **same single entry**: `Challenge level 13 · 48 stars · 4/5 Classic boards`, same played time, same last-modified, same unbroken cover. Every number cross-checks against the app. |
| Network kill degrades silently | CLOUD-05 | Needs real SDK failure path | `11-HUMAN-UAT.md` test 13 | **PASS.** Offline half clean — three offline solves and an offline relaunch, no error wall, snackbar, dialog or retry prompt, 0 FATAL. Reconvergence half passes in both directions, through two genuine Play Games conflicts that the app resolved without loss. |
| Profile-swap dialog (D-12..D-15) | CLOUD-02 | Needs a second Google account that has never authorised the game | Recorded as a UAT gap beside Phase 11 test 2 | **GAP — never exercised, not closed.** No second Google account exists on this setup, so neither the chooser path nor the alternate-player path can be reached; same root cause as Phase 11 test 2. Covered on the JVM only, by 13-08 and 13-10 over `FakeGamesService.emitSession(SignedIn(playerId = "other"))`. Still recorded in `11-HUMAN-UAT.md` § Gaps, verbatim as plan 13-03 wrote it. |

**Assumption A1, corrected on a device and then re-opened.** The earlier run measured GMS accounting the
512 px cover at its **raw ARGB** size (1,048,576 bytes against an 819,200-byte cap) and logging
`Snapshot cover image is too large` on every commit — A1 was therefore **falsified as written**, and
13-12 shipped `COVER_PX` = 384 (589,824 bytes). The final run on GMS 26.34.36 logged that warning
**zero** times across nine commits. `getMaxCoverImageSize()` was still not obtained, so the cap itself
remains unmeasured; the qa-engineer reports this as an observation, not a conclusion.

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies — every row of the Per-Task Verification Map carries a command copied from its own plan; no Wave 0 dependency remains open
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references — every listed file exists on disk
- [x] No watch-mode flags
- [x] Feedback latency < 120s — full JVM suite 36-90 s
- [x] `nyquist_compliant: true` set in frontmatter
- [ ] Every manual verification carried out — **not ticked**: the profile-swap dialog (D-12..D-15) was never exercisable and stays a recorded gap
- [ ] `./gradlew check` green on the phase's final tree — **not ticked**: see the Approval line

**Approval:** **APPROVED with two stated limits**, 2026-09-20, plan 13-11 task 3.

1. `nyquist_compliant` is `true`: every row of the map carries a real automated command taken verbatim
   from its plan, and the phase gate ran green — `./gradlew check`, producer-run, **11m07s at commit
   `9211416`**.
2. **The full `check` has not been re-run over the 13-12 commits.** `9211416` predates all of them. On
   the final tree (`f30de3d` and later) what is green is `./gradlew :domain:test :app:testDebugUnitTest`
   (1194 tests, 0 failures, 0 skipped) plus `detekt` and `ktlintCheck`; `lint`, `koverVerify`,
   `:domain:depthSweep` and `:app:assembleRelease` have **not** been run since `9211416`. The
   code-auditor's and the tech-lead's PASS verdicts on 13-12 are each explicitly conditional on that
   run. Anyone merging this branch runs `./gradlew check` first.
3. The profile-swap dialog remains an open manual gap, by absence of a second Google account. It is not
   claimed as covered.
