# tech-lead report — phase 13 conflict wedge
Date: 2026-09-20
Branch: feature/phase-13-cloud-save (aff014c..aff014c — nothing committed, no app source edited, no adb, no emulator touched)
Skills loaded: ergoz-soft:standards (role tech-lead). The repo rules snapshot is 0.1.0 and the plugin is 0.1.1, so I read the plugin copies: studio.md, reporting.md, memory.md, stacks.md. I did not re-read gitflow.md, logging.md, skills-policy.md, testing.md, kotlin-android.md, compose.md, play-store.md or puzzle-design.md — this is a question-mode analysis with no commit, no test authored and no stack or release decision; my predecessor's report records them for the same task. `android-architecture` and `android-gradle-logic` were not invoked: no module boundary and no build-logic change is proposed.
Ran in parallel with: none
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **A: no — and the SDK is silent where the hypothesis needs it to speak.** Nothing in the pinned 22.0.0 artifact requires an app to close `getConflictingSnapshot()`, states any consequence of leaving it open, or lets a client observe GMS's handle bookkeeping at all. The app's conflict path ran on exactly one of the two wedged devices, so it cannot be their common cause.
- **B: the predecessor's F-4 concern is real, but only on the give-up branch.** On the superseded-pair branch (F-3) the modified bytes are provably already inside the resolution that `resolveConflict` wrote to the server, so closing those handles is safe for CLOUD-02; on the give-up branch they are not, so leaving `conflictingSnapshot` alone stays correct.
- **C: there is an app-side recovery worth trying and it does not need a Play services clear** — open → `discardAndClose` → `load(forceReload = true)`, debug-only. `SnapshotsClient.delete` must never be used: it deletes server-side.
- **D: CLOUD-01 I sign off today. CLOUD-02 I hold**, and it needs no fix from B first — it needs one 10-minute write on the clean AVD (E3), which decides between an app/project cause and a device cause.
- One new finding independent of the wedge: English-33 3/3 was **lost from the server copy** during the 03:12–03:14 window. The union + push-guard design self-heals that; the wedge is what turned it into loss.

---

## A. Can the app's conflict handling wedge GMS's uploader?

**Answer: no, on the balance of the evidence (about 85% that the app is not the cause). The specific SDK question — is an app required to close `getConflictingSnapshot()` — is UNPROVEN: the pinned artifact and its bundled javadoc are silent, and I will not guess.**

### A.1 What the SDK actually says (documented fact)

Read from the exact artifact the build resolves — `play-services-games-v2:22.0.0` — `classes.jar` from the Gradle transform cache and `SnapshotsClient.html` / `SnapshotsClient.SnapshotConflict.html` / `SnapshotMetadata.html` from the bundled javadoc jar. Commands in Checks.

| Member | Documented text (verbatim, bundled javadoc) | Consequence |
|---|---|---|
| `SnapshotConflict.getSnapshot()` | *"Returns the server's version of the Snapshot that was opened."* | The app's orientation is right. |
| `SnapshotConflict.getConflictingSnapshot()` | *"Returns the modified version of the Snapshot in the case of a conflict."* | **That is the whole entry.** No ownership claim, no closing obligation, no consequence of leaving it open. |
| `SnapshotConflict.getResolutionSnapshotContents()` | *"the SnapshotContents object used to update the data in case of a conflict. Pass this to `resolveConflict(String, String, SnapshotMetadataChange, SnapshotContents)`"* | The 4-arg form is the merge-capable one, as used. |
| `discardAndClose(Snapshot)` | *"asynchronously discards the contents of the Snapshot and closes it. This will discard all changes made to the file, and close the snapshot to future changes until it is re-opened. **The file will not be modified on the server.** … fails a Task with an exception when called with a snapshot that was not opened or has already been committed/discarded."* | A discard is local-only and never a server write. Calling it on an already-committed handle *fails the Task*, it does not corrupt anything. |
| `commitAndClose(...)` | *"complete once the changes are synced locally and the background sync request for this data has been requested."* | `COMMITTED` ≠ uploaded. QA's reading is the documented one. |
| `resolveConflict(String, String, SnapshotMetadataChange, SnapshotContents)` | *"This will replace the data on the server with the specified SnapshotMetadataChange and SnapshotContents. Note that it is possible for this operation to result in a conflict itself, in which case resolution should be repeated. … If a conflict occurred, the result's `isConflict()` will return true, and the conflict will need to be resolved again"* | A repeated loop is the documented protocol. **Nothing is said about the handles of the conflict that was just resolved.** |
| `SnapshotMetadata.hasChangePending()` | *"Indicates whether or not this snapshot has any changes pending that have not been uploaded to the server. **Once all changes have been flushed to the server, this will return false.**"* | `change_pending = true` on all 13 E2 reads is therefore, by documentation, "the flush never happened". |
| `load(boolean forceReload)` | *"If `true`, this call will clear any locally cached data and attempt to fetch the latest data from the server."* | The only documented cache-clearing lever. See C. |
| `delete(SnapshotMetadata)` | *"This will delete the data of the snapshot **locally and on the server**."* | Destructive. Never a recovery tool. |

I scanned the **entire** bundled javadoc tree for `already open`, `currently open`, `unclosed`, `not closed`, `leak`, `close the snapshot`, `both snapshots`, `conflicting snapshot`, `pending`, `stale`. The only hit that bears on handle lifetime is the `discardAndClose` sentence above. **The javadoc nowhere states that an unclosed snapshot blocks a subsequent `open`** — that claim, which the store's own KDoc at `:44-53` presents as SDK behaviour, comes from the developer guide / community folklore, not from this artifact. It may still be true; it is not documented here.

### A.2 Why the artifact cannot settle it (documented fact + inference)

`javap -c` on the implementation shows every entry point is a thin IPC shim:

```
public final class com.google.android.gms.internal.games_v2.zzfr implements SnapshotsClient
  discardAndClose(Snapshot) -> new zzfl(snapshot) -> zzaq.zzb(zzap) -> Task
  zzfl.zza(GoogleApi)       -> TaskApiCall.builder().run(new zzfa(snapshot))
                               .setMethodKey(6723).build() -> GoogleApi.doWrite(...)
```

Every call builds a `TaskApiCall` and hands it to `GoogleApi.doWrite` — i.e. across the binder to Google Play services. `PlayGamesContentProvider` and `ExtendedSnapshotsHelper`, the two classes in the wedge's log lines, **are not in the client AAR at all** (grep over the extracted `classes.jar`: zero hits). All "is this snapshot open", "is there an outstanding conflict record" and "is an upload queued" state lives in the GMS process, behind an obfuscated IPC surface, with no read accessor on the client side. **Therefore no amount of reading the pinned SDK can answer "does abandoning a conflicting snapshot wedge GMS's uploader". That is the honest limit, and it is why I will not convert this hypothesis into a finding.**

### A.3 The four specific questions

**(i) Is an app REQUIRED to close `getConflictingSnapshot()`? What happens if it is left open?**
UNPROVEN — the SDK is silent on both halves. What *is* documented is a hazard in the other direction: `discardAndClose` "will discard all changes made to the file", and that file is the device's own modified version. So the SDK documents a cost for closing it and no cost for not closing it. `javap` confirms `SnapshotConflict` is a `final` class holding four fields (`Snapshot zza`, `String zzb`, `Snapshot zzc`, `SnapshotContents zzd`) with plain accessors — the getters do no bookkeeping.

**(ii) Does each `resolveConflict` return a new `DataOrConflict` with handles that also need closing? Does the app close them all?**
Documented: yes, each call returns a **new** `Task<DataOrConflict<Snapshot>>`, and each new conflict is a new `SnapshotConflict` with its own `conflictId`, its own server-version `Snapshot`, its own modified-version `Snapshot` and its own resolution contents. Whether they "need closing" is, again, not documented.

What the app actually does, read at `PlayGamesCloudSaveStore.kt:335-363`:

| Branch | `conflict.snapshot` (server version) | `conflict.conflictingSnapshot` (modified version) |
|---|---|---|
| `resolveOnce` throws / refused / cancelled (`:355-357`) | `discardQuietly` | never closed |
| `resolveOnce` returns **another conflict** (`resolved = true`) | **never closed** (F-3) | never closed |
| `resolveOnce` returns data → loop exits | **never closed** | never closed |
| `attempts == MAX` give-up (`:339-344`) | `discardQuietly` | **never closed** (F-4) |

So across *N* conflict iterations the app makes at most **one** terminal call in total — on the final `result.data` handle, by the caller — and leaves *N* server-version and *N* modified-version handles without one. F-3 is therefore wider than "the previous pair": the **last** resolved pair is abandoned too, on the success path. That is a fact about the code, not a defect I can prove.

**(iii) Can the give-up path leave a dangling conflict record?**
By reading: the give-up branch discards the server version and returns `null` **without ever calling `resolveConflict` for that `conflictId`**. So yes — that conflict is left unresolved. But that is the SDK's own designed state: `open` is documented to return a conflict "which will need to be resolved", and the next pass re-opens and resolves again. Not closing `conflictingSnapshot` adds no documented record on top of that. Note also that this branch cannot have fired on any wedged device in the observed sessions: it requires eleven consecutive conflicts, and the maximum ever logged is two in one launch.

**(iv) Does any of this match `Inconsistent DB:null/4` in `PlayGamesContentProvider.delete`?**
**No link can be made, and I judge the match unlikely (about 20%).** Reasons: (a) `PlayGamesContentProvider` and `ExtendedSnapshotsHelper` are GMS-internal and absent from the client AAR, so the `delete` in that stack is a `ContentProvider.delete`, not `SnapshotsClient.delete` — and the app never calls `SnapshotsClient.delete` at all (grep: zero hits in `app/src/main/java`); (b) the line is logged ~1 s **after** `Found conflict`, i.e. during GMS's own conflict bookkeeping, before the app has resolved anything; (c) it was logged **only** on Pixel_9a, yet the old Pixel_9 is equally wedged and never logged it.

### A.4 The argument that decides it

1. `Found conflict` appears exactly twice in the whole session, both on Pixel_9a at 03:12 (`qa-engineer-device-pass-rerun.md:58-61`). The old Pixel_9 **never logged a conflict** (`:49`, `:52`). E1 then recorded 0 conflicts across 5 Pixel_9a launches and 4 Pixel_9 launches; E2 recorded 0 across 13 reads.
2. Therefore `resolveOnce`, `discardQuietly(conflict.snapshot)`, the superseded-pair branch and the give-up branch **never executed on the old Pixel_9**. A per-device wedge caused by the app's conflict handling cannot explain a device on which that code never ran.
3. For the app to be the *common* cause, the dangling state would have to be account- or server-scoped. The clean AVD kills that: on the same account, the same project and the same snapshot record, `Pixel_9_clean` opened with `conflicts_resolved = 0`, `change_pending = false`, and was **served the server's copy**. An account-scoped outstanding conflict would have surfaced on that open.
4. The clean AVD also disposes of the configuration hypothesis QA raised. Data can only have reached a brand-new `userdata-qemu.img` from the server, so Saved Games is enabled for the project, the account is right, and reads work. Uploads worked too, up to 03:14:22.

**Residual probabilities for the wedge:** GMS-side fault on those two device instances / a GMS 26.x Saved Games uploader regression ~55%; emulator/device state (Pixel_9a's disk rollback, whatever the old Pixel_9 acquired) ~25%; app defect invisible from the SDK ~15%; project/server configuration ~5%.

One pattern nobody has named yet, and it is the cheapest thing in the file: **both wedged devices run GMS 26.x (26.15.33 and 26.08.34); the one device that works runs 25.07.34.** Same APK hash, same signing certificate, same `APP_ID`, same account (`qa-engineer-e2-probe.md:116-123`). That is an alternative hypothesis no app change can fix, and E3 tests it for free.

Aggravator worth recording, not a cause: the pre-guard build committed on **every launch** with a 1 MB raw cover that GMS re-scaled each time — 11 commits in 40 minutes, 9 of them carrying nothing new (`qa-engineer-e1-convergence.md:166`). If the uploader is already unhappy, that is a queue of pending changes being added to on every launch. Already removed by the push guard.

---

## B. The fix, and whether F-4's data-loss concern is real

### B.1 The concern is real — but only on one of the two branches

`resolveOnce` (`:366-386`) reads **both** sides before it resolves:

```kotlin
val serverBytes   = conflict.snapshot.snapshotContents.readFully()
val modifiedBytes = conflict.conflictingSnapshot.snapshotContents.readFully()
resolution.writeBytes(mergeRemote(serverBytes, modifiedBytes))
// -> resolveConflict(conflictId, serverSnapshotId, EMPTY_CHANGE, resolution)
```

and `resolveConflict` is documented to *"replace the data on the server with the specified … SnapshotContents"*.

- **Superseded pairs (F-3).** By the time the loop moves on, the modified version's bytes are already inside the resolution that was written to the server. `discardAndClose` is documented to be local-only — *"The file will not be modified on the server"* — so it cannot un-write that resolution. **Closing a superseded pair therefore cannot lose data. The predecessor's concern does not apply here.** (Inference from two documented sentences; high confidence.)
- **The give-up branch (F-4).** `resolveOnce` was **not** called for that final conflict, so `conflictingSnapshot`'s bytes were never read into anything. This is precisely where an un-uploaded offline commit lives, and `discardAndClose` discards "all changes made to the file". **The concern is real here, and leaving it alone was the right call.** The auditor's asymmetry argument (a visible, recoverable leaked handle versus a silent loss) holds, and the pinning test that fails if either close is added or removed is the correct instrument.

### B.2 The smallest correct fix

**Tech-lead decision.** Two changes, both small, neither shipped as a fix for the wedge:

1. **F-3 — close the superseded pair.** In `resolveToOpenSnapshot`, when `resolveOnce` returns another conflict, `discardQuietly` both `conflict.snapshot` and `conflict.conflictingSnapshot` before the next iteration; do the same for the pair whose resolution produced the final data, before returning. Safe for CLOUD-02 by B.1. Cost: if GMS considers a resolved handle already committed, the Task fails and `discardQuietly` swallows it — the documented failure mode of a redundant close is a failed Task, not corruption. Pinned by a unit test counting the discards per iteration, and by the existing mutation discipline.
2. **F-4 — leave it exactly as it is**, and keep `aPushThatGivesUpOnAConflictReportsTheConflictStageAndClosesTheServerVersion` as the pin. Add one sentence to its comment recording *why* the two branches differ: on the give-up branch the modified bytes have not been folded into any server write, on the superseded branch they have.

Also: **F-4-as-numbered-in-the-getconflict-report** (`attempts == MAX` rather than `>=`) — take the `>=`. It is free and removes a class of unbounded loop.

**Does B need a device test?** No, and it must not be given one. A unit test plus mutation proves the closes happen and that nothing else changes; no device test can prove the fix *works*, because the wedge is not reproducible on demand and, per A, is probably not app-caused. **Sequencing decision: do not ship B before E3.** Changing the conflict path now would destroy the only clean attribution we will ever get — if the clean device wedges after E3 with the current code, the app's conflict path is exonerated for a third time; if we have already edited it, nothing is exonerated. B belongs on the existing snapshot-handle-lifetime task, after E3.

### B.3 A separate finding: progress was lost from the server, and the design self-heals it

Reconstructing the 03:12–03:14 window from `qa-engineer-device-pass-rerun.md:34-52` and the clean device's restore:

- At 03:12 Pixel_9a resolved two conflicts and displayed the union, English-33 3/3 included. `resolveConflict` had therefore written a payload containing English-33 to the server (or tried to).
- The server's copy today is Pixel_9's 03:14:22 commit — level 9 + European-37, **English-33 absent**. E2 established that `lastModifiedTimestamp` is stamped by the writing device (`qa-engineer-e2-probe.md:61`), and 03:14:22 is Pixel_9's clock at its 03:14 relaunch.
- Pixel_9 **never** logged a conflict. So its open returned a cached `existing` that lacked English-33, `commitOrDiscard` merged stale-existing + local, `writeBytes` fully overwrote, and the commit was accepted with no conflict raised.

So either Pixel_9a's resolution never left the device, or it reached the server and was then clobbered by a full overwrite against a stale cache. I cannot distinguish them without seeing the server. What matters architecturally: the store's KDoc claim that "if GMS hands the app a stale `existing`, the upload conflicts later and the next open resolves it" (my predecessor's A(ii)) is **not documented anywhere in the artifact** and, in this window, demonstrably did not happen.

The reassuring half: the app **self-heals** this. Every device pushes its own complete union, and the push guard re-pushes whenever `merge(remote, local) != remote` (`ProgressSubmitter.kt:511-512`). Pixel_9a would have re-pushed English-33 on its next launch — if it could upload. **So a clobber is recoverable by design and is not a CLOUD-02 defect; the wedge is what turns it into loss.** Two consequences to record rather than code:

- One sentence in `commitOrDiscard`'s KDoc replacing the undocumented "it will conflict later" reassurance with "a stale `existing` can produce a full-overwrite that drops another device's keys; the guard's re-push is what recovers it".
- The wedge has a second, quieter effect nobody has stated: on a wedged device the pull returns the device's **own** cached copy, so `merge(remote, local) == remote`, so the guard declines and **the app never attempts another commit**. The guard is right; but it means a wedged device goes permanently silent rather than retrying. That is the correct trade (the alternative is the every-launch churn we just removed), and it is why recovery in C has to be an explicit action.

---

## C. Recovering a wedged device

**There is an app-side route worth trying, and clearing Play services data is not required.** Ranked, with the documented basis:

1. **Open → `discardAndClose` → then a normal pass.** Documented: a discard "will discard all changes made to the file, and close the snapshot to future changes until it is re-opened. The file will not be modified on the server." If the wedge is an outstanding local modified file that the uploader will not flush, discarding it is the cheapest thing that could clear it. **Safe for this app specifically**, and this is the load-bearing point: Room is the local source of truth, the snapshot is derived, and `importProgress` is a union that never deletes. Worst case the device then pulls the stale server copy, imports nothing new, and the push guard sees local ⊃ remote and re-pushes everything. Nothing the app owns can be lost. Mechanism confidence ~45%.
2. **`load(forceReload = true)` on metadata.** Documented to "clear any locally cached data and attempt to fetch the latest data from the server". My predecessor called this risky with pending local changes — correct in general, **wrong for this app**, for the same reason as (1): the pending change is a copy of Room, not the original. Run it *after* (1), so a discard has already released the file. Mechanism confidence ~35%.
3. **Both, as one debug-only action.** Add nothing player-facing: extend the existing `BuildConfig.DEBUG`-gated `DebugSnapshotPickerEntry` seam with a second intent extra, `…debug.RESET_CLOUD_CACHE`, that runs discard-then-forceReload-then a normal pass, under the same release-inertness test that already pins the picker (`aNonDebugBuildIgnoresTheExtraAndNeverEvenReadsIt`). Keep it on `lifecycleScope` rather than `@ApplicationScope` (the auditor's L-2), and delete it with the picker when the investigation closes (L-3).
4. **The picker's Refresh button** — QA left it untried and asked for a ruling. **Approved**, but only on a device we are willing to lose, and only *after* E3. It forces Google's own UI to reload from the server, so a refreshed picker that still shows only the device's own entry is direct evidence about the server. Do it on the old, already-account-damaged `Pixel_9` AVD first.

**Forbidden, explicitly:**
- `SnapshotsClient.delete(metadata)` — documented to "delete the data of the snapshot locally **and on the server**". It would destroy the only copy of the player's cloud save. It must never appear in this codebase; the grep gate that keeps `RESOLUTION_POLICY` honest should grow a companion for `.delete(`.
- `pm clear com.google.android.gms` — it cost us the old Pixel_9's Google account (`UserRecoverableAuthException: BadAuthentication`, account unrecoverable). No recovery plan may require it again, and no founder question should offer it as an option.

If none of (1)–(3) clears it, the wedged AVDs stay what they now are: forensic assets. A new AVD costs the founder one sign-in and costs the studio nothing — that is the recovery route of record.

---

## D. What this means for phase 13

### What I sign off today

**VERDICT: PASS** on the convergence-build diff (`git diff c2c1006..f30de3d -- app domain`), on the technical grounds in `.planning/reports/phase-13/code-auditor-convergence-build.md` (PASS, four Low/Info, none gating) and `.planning/reports/phase-13/code-auditor-getconflict-fix.md` (PASS, seven Low/Info), plus my own runs below. Boundaries hold (no gms import outside `platform/`, `PlatformBoundaryTest` green), the dependency rules are untouched (zero new coordinates), the tech design is honoured, and the two gates that mattered — F-1 and F-2 — are mutation-pinned rather than asserted. Conditional, as the auditor's was, on the producer running the full `./gradlew check` green; I am barred from the full run while the emulators are in use.

**CLOUD-01 — PASS.** A brand-new AVD with a fresh `userdata-qemu.img`, no app data and no tutorial restored Challenge level 9 and European-37 3/3 from the cloud on first launch (`producer-clean-device-finding.md:20-31`). That exercises the whole chain — server → `open` → `readFully` → codec decode → `importProgress` in one transaction → `mergeLadder` → the existing Flows repainting the catalog — with nothing local to fall back on. It is the honest new-device test that test 10 was not (test 10 only `pm clear`ed the app, and GMS's cache survived that). Caveat to record with the sign-off: the snapshot it restored was written before the wedge, so CLOUD-01 is proven for *read and restore*, not for *write then restore*.

**CLOUD-03 — PASS by substitute.** The debug picker showed one entry per device with an accurate description, correct played time and an unbroken cover (`qa-engineer-e2-probe.md:99-107`). The requirement's wording ("legible in the Play Games UI") is stale — that app no longer has a saves screen — and is with the game-designer.

**CLOUD-04, CLOUD-05 — unchanged, PASS.** D-04's resume behaviour was verified on a device (`qa-engineer-device-pass-rerun.md:51`), the offline half of test 13 passed with 0 FATAL lines, and the negative tests are in the suite.

### What I hold

**CLOUD-02 — HOLD.** It needs one device to upload and another to receive it, and no device has uploaded since 03:14:22. This is a verification blocker, not a code blocker: the merge, the codec, the import and the guard are all proven by tests and by mutation, and the one field observation we have of them (B.3) shows the design self-healing rather than losing.

**B's fix — HOLD until after E3**, per B.2. Shipping it now buys nothing measurable and costs us the attribution.

### Can UAT complete with one healthy device plus one fresh AVD?

Yes — arithmetically two devices is exactly what D-16 asks for — **but not yet, because we do not know that the clean device can upload.** `Pixel_9_clean` has written nothing; `change_pending = false` means "nothing pending", not "uploads work". Spending a founder sign-in on a second clean AVD before testing that would be spending it blind.

**E3 — the next experiment. QA, one device, ~10 minutes, current build, no code, no credentials, no Play Console.**
1. On `Pixel_9_clean` only, solve one Classic board that is 0/3 everywhere (Diamond-41 is 0/3 on all three devices).
2. Leave the app in the foreground 5 minutes, then force-stop and relaunch, and read `play_games_cloud_last_pull_change_pending` and `…_last_modified_at`.
3. Relaunch again at +15 minutes and read the same two keys.

| Outcome | Meaning | Next |
|---|---|---|
| `change_pending` clears to `false` | Uploads work on a clean device. The wedge is device state (and the GMS 26.x pattern is live). The app and the project are exonerated a third time. | Ask for **one more** clean AVD + founder sign-in; run CLOUD-02 on the two clean devices; retire both wedged AVDs to forensics; then land B as hygiene. |
| `change_pending` stays `true` | A third device, a fresh GMS cache and a clean account state all wedge on first write. That is an app or project cause, and it is now reproducible from zero. | B moves to first suspect and ships immediately; raise a founder question for a Play Console Saved Games / tester-permission check; run C's recovery action on the old Pixel_9. |

E3 costs one Classic board on the clean device and no founder action. It is worth more than every remaining diagnostic on the list, because it is the only experiment whose two outcomes point in opposite directions.

I am not raising a founder question for a second clean AVD yet, and I am not asking for a Play Console check yet: the clean device's restore already shows Saved Games is enabled and serving.

## Outputs
- `.planning/reports/phase-13/tech-lead-conflict-wedge.md` — this report (uncommitted; no app source, doc or test was edited)

## Checks
- `ls ~/.gradle/caches/modules-2/files-2.1/com.google.android.gms/play-services-games-v2/` + `find ~/.gradle/caches -path '*play-services-games-v2*' -name '*.jar'` — pass (22.0.0 only; `classes.jar` under transform `4d846fcb…`, `play-services-games-v2-22.0.0-javadoc.jar` under `modules-2`; the artifact the build resolves)
- `javap -p -classpath classes.jar 'com.google.android.gms.games.SnapshotsClient$SnapshotConflict' 'com.google.android.gms.games.SnapshotsClient'` — pass (`SnapshotConflict` is `final`, four fields `Snapshot zza / String zzb / Snapshot zzc / SnapshotContents zzd`, public constructor, four plain getters; `SnapshotsClient` carries both `resolveConflict` overloads, `commitAndClose`, `discardAndClose`, `delete`, `load(boolean)`, `getSelectSnapshotIntent`)
- `javap -p -c com/google/android/gms/internal/games_v2/zzfr.class` and `…/zzfl.class` — pass (every entry point wraps a `zzap` op and calls `zzaq.zzb(...)`; `zzfl.zza` builds `TaskApiCall.builder().run(new zzfa(snapshot)).setMethodKey(6723)` → `GoogleApi.doWrite` — the client is an IPC shim and holds no handle state)
- `unzip play-services-games-v2-22.0.0-javadoc.jar` then HTML→text over `SnapshotsClient.html`, `SnapshotsClient.SnapshotConflict.html`, `SnapshotMetadata.html` — pass (every quotation in A.1 is verbatim; `getConflictingSnapshot()`'s entire documentation is one sentence with no ownership claim)
- Keyword scan of the whole bundled javadoc tree (`already open|currently open|unclosed|not closed|leak|close the snapshot|both snapshots|conflicting snapshot|pending|stale`) — pass (only the `discardAndClose` sentence bears on handle lifetime; **no** statement that an unclosed snapshot blocks a later `open`)
- `grep -rla "PlayGamesContentProvider" .` and `grep -rla "ExtendedSnapshotsHelper" .` over the extracted `classes.jar` — pass (0 hits each: both classes in the wedge's log lines are GMS-internal and absent from the client AAR)
- `grep -rn --include='*.kt' "conflictingSnapshot|\.delete\(|\.load\(true\)|forceReload|getSelectSnapshotIntent" app/src/main/java` — pass (2 hits: `conflictingSnapshot` read once at `PlayGamesCloudSaveStore.kt:372`, `getSelectSnapshotIntent` in the debug picker. The app never calls `SnapshotsClient.delete` and never calls `load(forceReload)`)
- Read of `PlayGamesCloudSaveStore.kt:335-386` (the loop, the give-up branch, `resolveOnce`), `:117-140` (`load`), `:200-226` (`commitOrDiscard`), `:453-460` (`discardQuietly`) — pass (the close matrix in A.3(ii) is read off these lines)
- Read of `ProgressSubmitter.kt:405-544` and `CloudSyncStore.kt:97-163` — pass (the push guard reconstructs the obligation from `merge(remote, local) != remote`; `recordPushOutcome` clears the marker on `COMMITTED`, which by the javadoc is local-only — the basis for B.3's "a wedged device goes silent")
- `./gradlew :app:testDebugUnitTest --max-workers=2 --tests "*PlayGamesCloudSaveStore*" --tests "*CloudPull*" --tests "*PlatformBoundary*"` — pass (`BUILD SUCCESSFUL in 27s`; 5 result files, **64 tests, 0 failures, 0 errors, 0 skipped**)
- `./gradlew :app:detekt :domain:detekt :app:ktlintCheck :domain:ktlintCheck --max-workers=2` — pass (`BUILD SUCCESSFUL`, 25 tasks up to date against the current files)
- `git status --short` / `git log --oneline -1` — pass (clean; head `aff014c`; I edited nothing but this report)
- `./gradlew check` in full — **not run**, forbidden by the brief. My sign-off is conditional on the producer's run.
- adb, emulators, Play Console, network — **not touched** (QA experiment in progress).
- Not verified, and not verifiable from the client artifact: GMS's handle bookkeeping; whether an abandoned `conflictingSnapshot` affects it; whether a redundant `discardAndClose` on a resolved handle is a no-op or a failed Task; whether `discardAndClose` or `load(forceReload)` clears a stuck upload; whether the server holds English-33 in any form.

## Open issues / risks
- **A is a negative established by elimination, not by observing GMS.** If E3 wedges the clean device, A's conclusion is wrong and B ships at once. The argument is written so that one observation can overturn it.
- **B.3's reconstruction of 03:12–03:14 is inference (medium, ~60%).** "Clobbered by a stale-cache overwrite" and "Pixel_9a's resolution never left the device" both fit the evidence, and the server is not observable. What does not depend on the choice: English-33 is absent from the server copy, and the guard's re-push is what would have recovered it.
- **`docs/tech-design.md` still does not exist.** Every tech-lead decision in phase 13 — my predecessor's C/D/E, and B.2, C.3 and the E3 sequencing here — lives only in `.planning/reports/`. I deliberately did not create a stub: a tech design with unfilled sections fails my own quality gate. **Escalation to the producer:** schedule a documentation task that writes it from the phase-13 record before the phase closes. This is the third report to say so.
- **The store KDoc carries two statements the artifact does not support:** `:44-53` ("an open handle that is never closed makes the next open fail for the rest of the process") and the stale-`existing` reassurance quoted in B.3. Both should be reworded to name them as guide/folklore or as reasoning rather than as SDK contract. Cheap, and it stops the next reader building on sand — the same class of error as RESEARCH Pitfall 4, already corrected.
- **A grep gate for `.delete(` on the SDK path** should join the existing `RESOLUTION_POLICY` and no-logging gates. One destructive call, one line of protection.
- **The auditor's L-1 bites here.** A device stuck in the conflict loop records `opened=false` and *removes* the conflict count — indistinguishable on disk from a refused open. If E3's reads ever come back `opened=false`, that is the case to reinterpret before reading it as a store bug.
- **L-3 (the picker and its title ship in release behind a runtime boolean).** My ruling: acceptable while the investigation runs; the entry, the picker class and any C.3 reset action are **deleted together** when phase 13 closes, or moved behind a `src/debug` source set if any of them outlives it. That is a condition of my sign-off, recorded here so it cannot drift.
- **I-2** (`prefs.py` committed under `.planning/reports/`, against rules/testing.md) stays with the producer.

## Handoff
Next: producer.
1. **Dispatch E3 to qa-engineer** — one device (`Pixel_9_clean`), one solve, two reads, ~10 minutes, no code, no credentials, no founder action. It is the only thing that should happen next.
2. **Sign-off is given** on the convergence build; merge to `develop` once the full `./gradlew check` is green. Nothing is required from android-game-dev right now.
3. **Do not ask the founder for a second clean AVD yet**, and do not ask for a Play Console check: the clean device's restore already shows Saved Games enabled and serving. Both questions become worthwhile, and differently worded, depending on E3's row.
4. **Carry onto the existing snapshot-handle-lifetime task, after E3:** F-3 with both handles of the superseded pair (B.2 item 1), `>=` for the retry bound, the two KDoc corrections in Open issues, the `.delete(` grep gate, and L-1's KDoc note. F-4 stays open and unfixed **by decision**, with its pin and one added sentence.
5. **QA may tap the picker's Refresh** — on the old, already-damaged `Pixel_9` AVD only, and only after E3.
6. CLOUD-03's wording stays with game-designer. A documentation task for `docs/tech-design.md` needs scheduling before the phase closes.
