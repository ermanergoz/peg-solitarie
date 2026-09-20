---
phase: 13-cloud-save
plan: 12
plan-file: none — this work was never planned
subsystem: platform/games/save, data
tags: [cloud-save, play-games, snapshots, diagnostics, defect, unplanned]

# Dependency graph
requires:
  - phase: 13-cloud-save
    plan: "09"
    provides: "PlayGamesCloudSaveStore — the class this work diagnoses, fixes and instruments"
  - phase: 13-cloud-save
    plan: "08"
    provides: "ProgressSubmitter.pushIfNeeded / pullOnce — the pass the outcome, the guard and the probe hang off"
  - phase: 13-cloud-save
    plan: "11"
    provides: "task 2's failing device pass — the defect that opened this work"
provides:
  - "CloudSaveOutcome(stage, statusCode) — a reason code for every refused cloud save, persisted to DataStore"
  - "the getConflict() fix: every push and pull had been dying on the SDK's no-conflict path"
  - "the post-pull push guard: a pass that pulled pushes only when local holds what the cloud lacks"
  - "CloudPull(bytes, probe) — a non-PII probe of every pull, persisted write-only"
  - "COVER_PX 384 — the cover sized against the raw-ARGB accounting GMS actually uses"
  - "a debug-only entry to Google's read-only snapshot picker, which is how CLOUD-03 was finally observed"
affects: [13-11, phase-13-merge, snapshot-handle-lifetime-backlog]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "A refusal is reported as a typed stage plus an optional SDK status code, never as a Boolean and never as a log line — the type has no String slot, so PII cannot be added to it by accident"
    - "SDK fixtures are real final SDK instances built through their public constructors, not mocks that model the SDK — a mock that agreed with a wrong reading is what hid this defect behind 873 green tests"
    - "A diagnostic that must survive a release build is gated by a constructor-injected BuildConfig.DEBUG and pinned by a test that proves the non-debug path never even reads the input"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/platform/games/save/CloudSaveOutcome.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/save/CloudPull.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/save/PlayGamesSnapshotPicker.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/save/DebugSnapshotPickerEntry.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/save/CloudSaveOutcomeTest.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/save/CloudPullProbeTest.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/save/PlayGamesCloudSaveStorePullProbeTest.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/save/DebugSnapshotPickerEntryTest.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/save/PlayGamesSnapshotPickerTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/platform/games/save/PlayGamesCloudSaveStore.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/save/CloudSaveStore.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/save/NoOpCloudSaveStore.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/save/CloudSaveConstants.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/save/CoverImageSource.kt
    - app/src/main/java/com/app/pegsolitaire/data/ProgressSubmitter.kt
    - app/src/main/java/com/app/pegsolitaire/data/CloudSyncStore.kt
    - app/src/main/java/com/app/pegsolitaire/data/PlayGamesPreferences.kt
    - app/src/main/java/com/app/pegsolitaire/MainActivity.kt
    - app/src/main/res/drawable/cloud_save_cover.xml
    - .planning/phases/13-cloud-save/13-RESEARCH.md

key-decisions:
  - "CloudSaveOutcome carries a stage enum and a nullable Int, and nothing else. Only ApiException.statusCode is copied; no message, no cause, no player id, no description. Privacy holds by construction rather than by restraint — javap on the compiled class shows the instance fields are CloudSaveStage and java.lang.Integer only."
  - "The getConflict test fixtures are REAL SnapshotsClient.DataOrConflict / SnapshotConflict instances, built through their public constructors on final classes, not MockK stubs. The old mock stubbed `every { result.conflict } returns null`, which is exactly the wrong reading the production code held — which is why 873 green unit tests never saw the defect."
  - "land() no longer arms the cloud push marker. Without that change the 'cloud ahead only' case pushes anyway and the guard buys nothing. Every FAILED push still arms the retry marker through recordPushOutcome, so no progress can be stranded."
  - "writtenByThisDevice was dropped from the pull probe. The 22.0.0 javadoc promises only 'name of the device that wrote this snapshot, if known … or null'; equality against Build.MODEL is unreliable and could not tell two emulators sharing a system image apart. lastModifiedAt carries the usable part of the signal."
  - "F-4 (the give-up conflict path closes only the server version) was deliberately left unchanged and pinned by a test instead. Discarding getConflictingSnapshot() would risk dropping an un-uploaded offline commit — the data loss CLOUD-02 forbids — and the SDK does not settle whether that handle needs or tolerates a terminal call."

patterns-established:
  - "When a unit test and the SDK disagree, disassemble the SDK: `javap -c -p` on the AAR's classes.jar was the source of truth three separate times in this work"
  - "A device-found defect is diagnosed with a reason code before it is fixed — the reason code is shipped first, read off a device, and the fix follows the evidence"

requirements-completed: [CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-05]

# Metrics
duration: 2026-09-20, three review loops
completed: 2026-09-20
---

# Phase 13, unplanned work `13-12`: Making Cloud Save Actually Work — Summary

**Nothing in this summary was planned.** Plan 13-11's device pass found that no snapshot had ever
committed; the diagnosis found that **every** cloud push and pull in the phase had been dying on the
SDK's normal, no-conflict path, behind 873 green unit tests. What follows is the work that closed it,
dispatched from tech-lead reports in three review loops and committed under the scope tag `13-12`. There
is **no `13-12-PLAN.md`**.

## How this work came to exist

| Step | Evidence |
|---|---|
| The device pass recorded 3 blocked + 1 partial; `play_games_cloud_push_pending` stayed true on both devices while leaderboard submissions drained | `qa-engineer-device-pass.md` |
| That symptom is exactly how an un-enabled Play Console Saved Games toggle presents, so Q-001 went to the founder | `13-OPERATOR-CHECKLIST.md` Step 0, `.planning/questions/FOUNDER.md` |
| The founder answered: Saved Games **already enabled and published**. So the refusal was a defect | STATUS.md, 2026-09-20 |
| The tech-lead read the save path, found no defect by reading, and specified a reason code as the first move | `tech-lead-save-path-observability.md` § B |
| The reason code shipped, was read off two devices, and named the failure | `android-game-dev-cloud-save-outcome.md`, `qa-engineer-save-reason.md` |

---

## 1. The cloud-save outcome / reason code

**What it is.** `CloudSaveStore.save` returns `CloudSaveOutcome(stage: CloudSaveStage, statusCode: Int?)`
instead of a bare `Boolean`. The seven stages — `COMMITTED`, `NO_ACTIVITY`, `OPEN_FAILED`,
`CONFLICT_UNRESOLVED`, `OVER_CAP`, `COMMIT_FAILED`, `FAILED_OTHER` — classify `open`, `resolveConflict`
and `commitAndClose` separately. `ProgressSubmitter` records the outcome through **one**
`CloudSyncStore.recordPushOutcome` edit that writes the pending flag, `play_games_cloud_last_push_stage`
and `play_games_cloud_last_push_status` together.

**What it closed.** It turned "the push was refused" into "the push was refused at this stage with this
code", with no log line, no UI, no reader in the app — CLOUD-05 and D-03 untouched. Privacy holds by
construction: the type has **no `String` slot**, only `ApiException.statusCode` is copied, and `javap -p`
on the compiled class shows instance fields of exactly `CloudSaveStage` and `java.lang.Integer`.

**Verdict: code-auditor PASS** (`code-auditor-cloud-save-outcome.md`). Every requirement of section B
met; the single `dataStore.edit` proven by a `CountingDataStore` rather than by reading. Four items
recorded for planning (F-2, F-3, F-4 — all pre-existing, none a regression from the diff) plus three
informational notes.

**Commits:** `0437a2b` (tests, RED) → `b39e6d6` (feat) → `c25dd7f`, `759f040`.

---

## 2. The `getConflict()` defect and its fix

**The defect.** `SnapshotsClient.DataOrConflict.getConflict()` **throws** `IllegalStateException` when
`isConflict()` is false. The store did `val conflict = result.conflict ?: break` — and line 316 did the
same with `?: return result.data`. Both getters are annotated `@Nullable`, so Kotlin sees
`SnapshotConflict?` and the `?:` compiled without a murmur. The unit-test mock stubbed
`every { result.conflict } returns null`, i.e. it modelled the same wrong reading the production code
held, which is precisely why **873 green unit tests never saw it**.

**QA's evidence, on hardware, not by reading.** With the new reason code shipped, both devices recorded
`stage = FAILED_OTHER` and **no** status key — meaning a non-`ApiException` was thrown outside the three
staged awaits. The qa-engineer attached `jdb` to the debuggable app during a solve on each device and
caught it, identical stack on both (`qa-engineer-save-reason.md`, evidence files 93 and 94):

```
java.lang.IllegalStateException
  [1] com.google.android.gms.games.SnapshotsClient$DataOrConflict.getConflict (play-services-games-v2@@22.0.0:1)
  [2] ...PlayGamesCloudSaveStore.resolveToOpenSnapshot (PlayGamesCloudSaveStore.kt:296)
  [3] ...PlayGamesCloudSaveStore.openResolved (PlayGamesCloudSaveStore.kt:256)
```

and confirmed it against the SDK bytecode:

```
getConflict(): invokevirtual isConflict:()Z ; ifne 17
  new java/lang/IllegalStateException ; ldc "getConflict called when there is no conflict." ; athrow
```

It fired on the **normal** path, so every push and every pull died — which also explains why nothing was
ever restored.

**The fix.** `resolveToOpenSnapshot` now discriminates with `while (result.isConflict)` and reads each
getter strictly on its own side (`checkNotNull(result.conflict)` / `return result.data`); the misleading
"null exactly when" comment is gone. The test fixtures were rebuilt as **real** `DataOrConflict` /
`SnapshotConflict` instances through their public constructors on final classes, so the spec can never
again disagree with the pinned SDK. Handle ownership was closed in the same pass: `resolveOnce` under a
latch and `finally` inside the loop, a new `commitOrDiscard` owner, metadata built **before** the open,
cleanup under `withContext(NonCancellable)` with `ensureActive()` after it.

**Red state, watched:** at `77817e7` (tests only, old store) **29 of 47 store tests failed**. Green at
`12514cb` (51 tests, 0 failures, 0 skipped).

**Verdict: code-auditor PASS** (`code-auditor-getconflict-fix.md`). The auditor re-derived the SDK
contract independently from the AAR bytecode rather than trusting the diff's comments, confirmed exactly
one terminal call on every path with no double close, and confirmed `CancellationException` is never
swallowed. Seven findings, all Low or Info, none blocking; the two real ones (F-1, F-2) were test gaps,
not defects — and were closed in step 7 below.

**Commits:** `77817e7` (tests, RED) → `12514cb` (fix) → `691015f`, `91f9042`.

---

## 3. The 384 px cover

**What it closed.** RESEARCH assumption A1 — the cover-size accounting basis — was **falsified on a
device**: GMS accounts the cover at its **raw ARGB** size, not its PNG size, and logged
`Snapshot cover image is too large. Currently at 1048576 bytes; max is 819200` on every commit with the
512 px cover. `COVER_PX` is now **384**, which is 589,824 raw bytes against the 819,200 cap, pinned by
`theRawArgbCoverFitsThePlayGamesCoverCeiling`. The `CoverImageSource` KDoc is corrected and RESEARCH A1
is marked falsified with the measured GMS log line.

**A later observation that re-opens it.** The final UAT run, on GMS **26.34.36**, logged that warning
**zero** times across nine commits — against "every commit" on GMS 26.15.33. `getMaxCoverImageSize()`
was still not obtainable without a code change or a debugger, so this does **not** prove the cap changed;
the qa-engineer records it as an observation. The 384 px cover is harmless either way and the cover
rendered unscaled-looking in the picker on both devices.

**Commits:** `71a66e0` (test, RED — 1 of 6 failed) → `044c33a` (fix).

---

## 4. The post-pull push guard

**What it is.** A pass that pulled now pushes only if `ProgressMerge.merge(remote, localExport) != remote`
— i.e. only when the device holds something the cloud lacks. `land()` no longer arms the cloud push
marker; it used to, which would have made the "cloud ahead only" case push anyway and the guard
pointless.

**What it closed.** The every-launch rewrite: before this, once any cloud payload existed, every launch
pushed. Four convergence cases plus a played-time case are tested. **No progress can be stranded by the
change**: the push obligation moved from a durable marker to a per-pass reconstruction from the cloud's
own bytes, and every *failed* push still arms the retry marker through `recordPushOutcome` — which the
code-auditor checked specifically.

**Red state, watched:** cases (a) and (c) failed, both "expected 0 but was 1".

**Known caveat, accepted.** The guard compares against the **uncapped** local export while the push
writes `encodeWithinCap`. A player whose payload had been trimmed would push every launch. The auditor
did the arithmetic and called it unreachable: `TRIM_TARGET_BYTES` is 2,883,584 bytes against a full
10,000-level ladder measured at ~400 KB — roughly 7× headroom — and even if it fired the behaviour
degenerates to the *pre-diff* every-launch push, so it is not a regression (finding I-1, Info).

**Commits:** `bc5bddb` (tests, RED) → `9a0afe4` (fix).

---

## 5. The pull probe

**What it is.** `load` returns `CloudPull(bytes, probe)`. `CloudPullProbe` holds `changePending`,
`conflictsResolved` and `lastModifiedAt` — Boolean, Int and Long, nothing else — persisted in one edit as
`play_games_cloud_last_pull_{opened,change_pending,conflicts_resolved,last_modified_at}`. Write-only:
nothing in the app reads them, nothing is user-visible. `prefs.py` prints the new keys.

**What it closed.** It made the non-convergence observable from the outside. Every subsequent
experiment — E1, E2, the upload baseline, the clean-device finding and the final UAT's 22-row probe
table — is read off these four keys. The `change_pending = false` **with `last_modified_at` equal to this
device's own last commit** pair is what finally proved the first real upload.

**A documented deviation.** `writtenByThisDevice` was dropped. The 22.0.0 javadoc promises only "name of
the device that wrote this snapshot, if known … or null"; equality against `Build.MODEL` is unreliable
and could not distinguish two emulators sharing a system image. `lastModifiedAt` carries the usable part
of the signal. The store KDoc and RESEARCH Pitfall 4 — both of which wrongly said no API can observe the
upload — are corrected.

**Commits:** `d845944` (tests, RED — compile errors, the types did not exist) → `94cfe6f` (feat) →
`e532de6` (move the probe read to file level, for detekt's function-count limit).

---

## 6. The debug snapshot picker

**What it is.** `DebugSnapshotPickerEntry`, gated by a constructor-injected `BuildConfig.DEBUG`, launches
`PlayGamesSnapshotPicker` — `getSelectSnapshotIntent(title, false, false, 1)`, run inside `platform/`. It
waits up to 30 s for the session to report signed in. Driven by:

```
adb -s <serial> shell am start -n com.erman.pegsolitarie/com.app.pegsolitaire.MainActivity \
  --ez com.app.pegsolitaire.debug.SHOW_SNAPSHOT_PICKER true
```

**What it closed.** **CLOUD-03.** The Play Games app 2025.09.66390 no longer has a Saved games screen at
all, so UAT test 12 could not be run as written. The picker is how the listing entry was finally
observed: both devices showed the same single entry, the same description
`Challenge level 13 · 48 stars · 4/5 Classic boards`, the same played time, last-modified and cover.
`aNonDebugBuildIgnoresTheExtraAndNeverEvenReadsIt` proves a non-debug configuration never reads the
extra. The exported-Activity entry was security-reviewed clean: the intent supplies only a boolean and no
result is consumed anywhere in the app.

**Commits:** `105025a` (tests, RED) → `aeb861e` (feat).

---

## 7. Pinning auditor F-1 and F-2

The auditor's two real findings on the `getConflict` fix were that the `NonCancellable` cleanup and the
"metadata built before the open" ordering were **unpinned** — the suite stayed green if either was
reverted. Both are now pinned, and each new test was confirmed red against a deliberately broken store
before the store was restored.

The auditor then verified this **by mutation rather than by reading**: in a scratch copy of the tree, with
`withContext(NonCancellable)` removed from `discardQuietly` and `metadataChangeFor` moved after
`openResolved`, the result was `49 tests completed, 2 failed` — exactly
`aCloseStillInFlightWhenThePassIsCancelledIsAwaitedToCompletion` and
`metadataThatCannotBeBuiltFailsThePushBeforeAnyHandleIsOpened`. A second mutation round (guard forced
true, the debug gate deleted, `COVER_PX` back to 512) killed 5 more. Nothing in this work passes
vacuously.

**Commit:** `f30de3d`.

---

## Verdicts and the gate

| Review | Verdict | Source |
|---|---|---|
| code-auditor — outcome / reason code | **PASS** (F-1 Info judgement, F-2..F-4 Low for planning, F-5..F-7 Info) | `code-auditor-cloud-save-outcome.md` |
| code-auditor — `getConflict` fix | **PASS** (seven findings, all Low or Info, none blocking) | `code-auditor-getconflict-fix.md` |
| code-auditor — convergence build | **PASS** (four Low/Info, none gating) | `code-auditor-convergence-build.md` |
| tech-lead — sign-off on the convergence build | **PASS**, conditional on the producer's full `./gradlew check` | `tech-lead-conflict-wedge.md` § D |
| qa-engineer — final UAT 10-13 | **PASS** (test 11's D-04 sub-check partial) | `qa-engineer-uat-final.md` |

**Checks at the final tree `f30de3d`:** `./gradlew --max-workers=2 :domain:test :app:testDebugUnitTest`
— **1194 tests, 0 failures, 0 errors, 0 skipped**; `detekt` and `ktlintCheck` green (the first run
failed on `TooManyFunctions`, two `LargeClass` and two `MaxLineLength` findings, all fixed by
restructuring — **no suppression added**). **Zero** new dependency coordinates, so no `THIRD_PARTY.md`
row is owed. No `@Suppress`, no baseline, no `@Ignore`, no skipped test, no `!!` in production code
anywhere in this work.

**The full `./gradlew check` has not been run on this tree.** The gate on record is the producer's run at
`9211416`, which predates every commit above. Both the auditor's and the tech-lead's PASS verdicts are
explicitly conditional on that run.

---

## Follow-ups

Open items, so they are not lost with the phase. None of them is a blocker for the merge; all of them
belong to the snapshot-handle-lifetime backlog task or to a named role.

1. **code-auditor F-1 and F-2 — now pinned.** The `NonCancellable` cleanup and metadata-before-open are
   mutation-verified (`code-auditor-convergence-build.md`). **Closed**, recorded here so the pins are not
   removed by someone who does not know why they exist.
2. **F-3 — each conflict iteration still abandons the superseded pair.** When `resolveOnce` returns
   another conflict, the superseded `conflict.snapshot` gets no terminal call. The tech-lead **specified
   the fix** (discard both `conflict.snapshot` and `conflict.conflictingSnapshot` before the next
   iteration, and the pair whose resolution produced the final data; pinned by a unit test counting
   discards per iteration) and **deliberately held it** — shipping it before experiment E3 would have
   destroyed the only clean attribution available for the wedge (`tech-lead-conflict-wedge.md` § B.2).
   The final UAT has since exonerated the conflict path by direct observation, so this is now **hygiene**
   and can be sequenced as such.
3. **F-4 — the give-up path closes one of two conflict snapshots. Left by design, pinned by test.**
   Discarding `getConflictingSnapshot()` could drop a device's un-uploaded offline commit — the data loss
   CLOUD-02 forbids — and the SDK does not settle whether that handle needs or tolerates a terminal call.
   Pinned by `aPushThatGivesUpOnAConflictReportsTheConflictStageAndClosesTheServerVersion`, which fails
   if someone adds the second close **or** removes the first, so the decision goes back to a human rather
   than drifting.
4. **L-1 — conflict exhaustion records `opened=false` and drops the conflict count.** When
   `attempts == MAX_CONFLICT_RETRIES`, `recordPullProbe(null)` writes `opened=false` and **removes**
   `play_games_cloud_last_pull_conflicts_resolved` — so a device stuck in the conflict loop is
   indistinguishable on disk from one whose `open` was simply refused, which is exactly the case the
   probe exists to diagnose. Diagnostic only. Fix: carry `conflictsResolved` on the exhaustion path, or
   say in the `CloudPull` KDoc that exhaustion is deliberately folded into "never opened".
5. **L-2 — the picker uses a process-lifetime scope.** `DebugSnapshotPickerEntry` launches on
   `@ApplicationScope`, which outlives the `MainActivity` that read the extra, so the picker can surface
   on a *later* Activity instance and read as flakiness. Both `withTimeoutOrNull` waits also sit outside
   the `runCatching`. Debug-only and bounded; `lifecycleScope` at the call site removes both.
6. **L-3 — the picker is compiled into release behind a runtime flag.** `PlayGamesSnapshotPicker`, the
   extra name and `DEBUG_PICKER_TITLE` ship in the release APK; only `BuildConfig.DEBUG` stands in front
   of them, and Hilt's generated factory keeps the class reachable so R8 will not strip it. The auditor
   judged this acceptable for a diagnostic build that is not being published, with a follow-up: add a
   `src/debug` source set or a build-type-gated `@Binds`, **or delete the entry now that CLOUD-03 is
   verified**. Needs an explicit tech-lead decision on when it goes.
7. **tech-lead A(iii) — the push path never lands what it saw.** `commitOrDiscard` returns only an
   outcome; `pushIfNeeded` never lands `finalBytes`, so a conflict resolved in the cloud is not applied
   locally until the next launch's pull. Real by reading, severity low, no data lost, and D-02 accepts a
   one-launch delay (`tech-lead-non-convergence-triage.md`). Backlog note on the handle-lifetime task.
8. **New low-severity defect — "Next level" after a merged resume serves N+1, not the merged head.**
   Leave a Challenge game in progress at level N, merge in progress from a device that reached N+2 or
   beyond, resume and solve N, tap "Next level": it serves N+1, which the merged progress already records
   as solved 3/3, so the player replays a completed level. The ladder itself does **not** regress and
   `Continue — Level 13` on Home still reaches the head. Never seen before because the earlier run's gap
   was exactly 1. **Routed to the game-designer** for a wording ruling before it becomes a code change —
   the UAT sentence may simply be loose.
9. **QA's caveat: `change_pending = true` is a weaker oracle than the investigation assumed.** One device
   reported `change_pending = true` across three launches over eight minutes while its payload had
   demonstrably already reached the other device — each launch's push guard produced a fresh local commit
   that had not flushed at read time, so the flag was true about a *newer* commit. The honest upload
   signal is `change_pending = false` **with `last_modified_at` equal to this device's own last commit**.
   Any re-reading of `Pixel_9a`'s wedge history that rests on the flag alone should be revisited.
10. **Also recorded, not owned here:** the auditor's F-7 — the repo has no `THIRD_PARTY.md` at all — is a
    standing release-manager item, not a phase-13 blocker (no dependency was added). I-2 — `prefs.py`
    lives under `.planning/reports/` against `rules/testing.md`, which wants QA helpers under
    `.planning/qa/<task-slug>/` — belongs to the producer.
