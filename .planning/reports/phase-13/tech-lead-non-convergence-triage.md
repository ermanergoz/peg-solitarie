# tech-lead report — phase 13 non-convergence triage (question mode)
Date: 2026-09-20
Branch: feature/phase-13-cloud-save (9211416..9211416 — nothing committed, no app source edited)
Skills loaded: ergoz-soft:standards (role tech-lead). The repo snapshot is 0.1.0 and the plugin is 0.1.1, so I read the plugin copy of the rules: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, stacks.md, testing.md, kotlin-android.md, play-store.md. I also read the repo's `.claude/rules/lessons.md` (not the plugin's memory). android-architecture and android-gradle-logic were not invoked, because no module or build-logic decision is made here.
Ran in parallel with: none
Loops: 0
Lessons applied: none (0 active lessons; `.planning/recall/RECALL.md` does not exist)


> Saved by the producer from the tech-lead's final message: the agent's harness did not let it
> write the report file itself. Content is the agent's, unedited apart from this note.

## Summary
- The code cannot explain the observed non-convergence. The save path never writes a payload that lacks the server's bytes, and the merge, codec and import are true unions.
- Every failed transfer involves Pixel_9a's GMS: its guest disk was rolled back to 2026-09-15, and only it logs `Inconsistent DB:null/4`. That is my primary hypothesis, at about 60% confidence.
- The bundled javadoc confirms that `COMMITTED` is local-only.
- The "no API can observe the upload" claim is false: `SnapshotMetadata.hasChangePending()` exists in 22.0.0.
- Next is the zero-code experiment E1. After that, one small developer build carries the cover fix, the post-pull push guard, a boolean sync diagnostic and a debug snapshot-picker entry.

## A. The sequence, against SDK semantics
Paths are under `app/src/main/java/com/app/pegsolitaire/`.

**Pull, once per process.**
1. `data/ProgressSubmitter.kt:246-248` starts `submitAll` (`:349`). The session gate is `:356-361` and the profile gate is `:365-369`.
2. `pullOnce` (`:372`, `:400-407`) calls `load` (`platform/games/save/PlayGamesCloudSaveStore.kt:108`).
3. `load` opens the snapshot with `open(name, createIfNotFound = true, RESOLUTION_POLICY_MANUAL)` (`:272-280`).
4. While the result is a conflict (`:325`), `resolveOnce` (`:354-374`) reads the server and modified bytes, writes `mergeRemote(server, modified)` into the resolution contents, and calls `resolveConflict(conflictId, serverSnapshotId, EMPTY_CHANGE, resolution)`.
5. `load` then reads the data (`readFully`, `:119`), closes the handle (`discardAndClose`, `:121`) and decodes (`:403`).
6. `land` (`:422-428`) runs a Room import in one transaction (`data/ProgressRepository.kt:447-506`) and `mergeLadder`.

**Push, in the same pass.**
1. `pushIfNeeded` (`:484-509`) runs when the pull brought any decodable payload (`:489`). It calls `save` (`:152`).
2. `save` builds the metadata and cover before the open (`:166`), then calls `openResolved` (`:168`) and `commitOrDiscard` (`:190-216`).
3. `commitOrDiscard` does `existing = readFully()`, then `finalBytes = mergeRemote(existing, bytes)`, then `writeBytes`, then `commitAndClose`.

**(i) Stale read after another device's commit.**
- This is not an app defect. `open` takes no freshness argument, and the 22.0.0 javadoc makes no freshness promise.
- The only freshness lever in the SDK is `load(forceReload = true)` on metadata, which "will clear any locally cached data". It is risky with pending local changes and is not recommended.
- Test 10's restore completed in under 1 s after a `pm clear` of the app only. GMS's snapshot cache survives that wipe, so test 10 was never a new-device test on Pixel_9.

**(ii) Lost update.**
- This is not present. The commit is `merge(server-as-opened, local)` (`:199-200`).
- An undecodable side contributes the merge identity (`data/cloud/CloudProgressCodec.kt:187-196`).
- `ProgressMerge.merge` is a key union with max/min/max per field. `importInTransaction` runs the same join.
- If GMS hands the app a stale `existing`, the upload conflicts later and the next open resolves it through the same merge.
- A lost update would require GMS to break the MANUAL policy.

**(iii) Conflict resolved in the cloud but not landed locally on the push path.**
- This is real by reading. `commitOrDiscard` returns only an outcome, and `pushIfNeeded` never lands `finalBytes`.
- Severity is low. No data is lost, and convergence is delayed by one launch, which D-02 accepts.
- It would explain at most Pixel_9a missing what arrived in its second conflict at 03:12:37. It cannot explain four later relaunches (each one pulls), or Pixel_9 never receiving English-33.
- Decision: backlog note on the existing snapshot-handle-lifetime task. It is not a blocker.

**Match to the observation.** None of (i) to (iii) produces a device-dependent, one-directional failure from identical APKs.

**Documentation errors found.**
- The store KDoc (`:59-61`) and RESEARCH Pitfall 4 ("no API to observe it") are wrong. `hasChangePending()` is documented as "changes pending that have not been uploaded to the server".
- The `ProgressSubmitter` class KDoc (`:91-94`) says a clean launch does not push. Once any cloud payload exists, every launch pushes (`:489`).

## B. Hypotheses
**H3 — Pixel_9a's GMS is damaged (about 60%).**
- Its guest disk, GMS included, was rolled back to 2026-09-15 (`qa-engineer-device-pass.md:75`).
- Only it logs `Inconsistent DB:null/4` from `PlayGamesContentProvider.delete`, about 1 s after each `Found conflict`. That is the cleanup of the conflict record failing.
- A stuck conflict record holding a stale server copy explains four things:
  - its writes never upload, so English-33 never reaches Pixel_9;
  - there are two conflicts in one launch;
  - it received the union exactly once;
  - it has stayed frozen at level 9 since.
- Prediction: Pixel_9a logs `Found conflict` on every open, even when Pixel_9 has written nothing.

**H2 — GMS asynchronous upload (about 25%, as a contributor).**
- `commitAndClose` completes when "synced locally and the background sync request … has been requested".
- Pixel_9's three offline commits may have put its sync into backoff, and QA watched for only 6 minutes after the network returned.
- H2 alone does not explain Pixel_9a still holding unsynced changes 11 minutes after its commit while online.

**H1 — app defect (10–15%).** This is a residual for something invisible by reading, for example F-3's abandoned superseded conflict pairs interacting with GMS bookkeeping.

**Discriminators.**
- Whether `Found conflict` appears on Pixel_9a with no remote write.
- The `hasChangePending` value on the next pull.
- The `dumpsys content` sync and backoff state.
- A clean third reader.

I cannot see the server, so none of this is proven.

## C. Next experiment
**E1 — QA, about 40 minutes, current build, no code and no credentials.** Both devices stay online. Capture `adb logcat -v time | grep -E "PlayGamesServices|Snapshot"` continuously on both.

1. Relaunch Pixel_9a twice while Pixel_9 stays stopped, and record how many `Found conflict` and `Inconsistent DB` lines each launch produces.
2. On Pixel_9a, solve marker A (a Classic board that is 0/3 on both devices). Keep the app in the foreground for 5 minutes with no force-stop. Save `adb shell dumpsys content | grep -i -B2 -A8 games` for pending syncs and backoff, if the authority is listed.
3. Relaunch Pixel_9 at +2, +5 and +15 minutes and read the Classic catalog each time.
4. Swap roles: Pixel_9 solves marker B, and Pixel_9a is relaunched at +2, +5 and +15 minutes.

| Outcome | Meaning | Next |
|---|---|---|
| B arrives, A never arrives, conflicts on every Pixel_9a open | H3 | Pixel_9a is not a valid test device. CLOUD-01 and CLOUD-02 need a clean AVD with a Google sign-in, which is the single founder action. Clearing GMS data on Pixel_9a would very likely also force a re-authentication. |
| Both arrive, after minutes | H2 only | Rescript tests 10–13 with waits of at least the measured lag and no force-stop straight after a solve. No code change. |
| Neither arrives within 15 minutes | H1 or configuration | Build E2 before anything else. |

**E2 — developer, in the same build as D. Tech-lead decision.**
- Extend the persisted non-PII diagnostic to the pull. This supersedes my predecessor's "extend to `load` only if a pull-only failure appears"; that failure has now appeared.
- `load` returns the bytes plus an SDK-free probe with these fields:
  - `changePending: Boolean`, from the opened snapshot's metadata;
  - `conflictsResolved: Int`;
  - `lastModifiedAt: Long`;
  - `writtenByThisDevice: Boolean`, from `getDeviceName() == Build.MODEL` compared inside `platform/`.
- No SDK string is stored.
- The values are written through `CloudSyncStore` as `play_games_cloud_last_pull_*` keys, and `tools/prefs.py` already reads them.
- There is no logging, so the grep gate stays at 0.
- In the same edit, correct the KDoc at `:59-61` and RESEARCH Pitfall 4.

## D. A1 cover
- `COVER_PX` → 384 is correct: 384² × 4 = 589,824 ≤ 819,200, which is the documented 800 KB floor. At 448 the figure is 802,816, which is too tight.
- Also affected:
  - the KDoc at `CoverImageSource.kt:41-46` states the falsified accounting;
  - `CoverImageSourceTest` should gain an assertion of `COVER_PX * COVER_PX * 4 <= 800 * 1024`;
  - RESEARCH A1 should be marked falsified by measurement.
- There is no functional harm today. GMS scales the cover and the commit succeeds.
- **Every-launch push: yes, reconsider.**
  - Today any launch that pulls a decodable payload rewrites the snapshot, cover included.
  - That moves `lastModifiedTimestamp` and creates conflict opportunities for the other device.
  - It contradicts `load`'s own stated principle (`:93-95`).
- Decision: gate the post-pull push on `ProgressMerge.merge(remote, localExport) != remote`. Local then holds something the cloud lacks, which preserves one-round convergence. Solve pushes, swap pushes and the retry marker are unchanged.
- This is invisible to the player, so it is a technical decision, not a product one.

## E. CLOUD-03 verification
- **(a) Primary.**
  - A `BuildConfig.DEBUG`-gated entry, for example a MainActivity intent extra that release builds ignore.
  - It calls `getSelectSnapshotIntent(title, allowAddButton = false, allowDelete = false, 1)` through `startActivityForResult` inside `platform/`.
  - QA reads the cover, description and played time in Google's own snapshot UI.
  - It needs no product decision while it stays debug-only.
  - It does need a test proving a release build ignores the extra.
- **(b) Complement.**
  - A metadata read-back through `SnapshotsClient.load(false)`, persisted as booleans: description equals the last written one, played time matches, and the cover URI is non-null.
  - It proves the round trip but not legibility.
- **Needs a product decision (game-designer, through the producer).** A player-facing Saved games entry would conflict with D-03's silent design. Separately, CLOUD-03's "legible in the Play Games UI" should be reworded now that the Play Games app has no saves screen.

## Outputs
- This report, written to the final message only. My harness forbids writing report files. The intended path is `.planning/reports/phase-13/tech-lead-non-convergence-triage.md`, uncommitted; the producer can save it as was done for the QA report.

## Checks
- `javap -cp classes.jar com.google.android.gms.games.snapshot.SnapshotMetadata com.google.android.gms.games.SnapshotsClient`, on the 22.0.0 AAR from the Gradle cache — pass (`hasChangePending()`, `getLastModifiedTimestamp()`, `getDeviceName()`, `load(boolean)` and `getSelectSnapshotIntent(String,boolean,boolean,int)` are all present).
- Text extraction from `play-services-games-v2-22.0.0-javadoc.jar` for `SnapshotsClient.html` and `SnapshotMetadata.html` — pass (the `commitAndClose` "synced locally … background sync request" quote; the `discardAndClose` "will not be modified on the server" quote; the `hasChangePending` and `forceReload` wording; no freshness statement on `open`).
- `grep -n "COVER_PX\|MAX_CONFLICT\|SNAPSHOT_NAME" …/CloudSaveConstants.kt` — pass (512, 10, `progress`).
- `grep -rn "getSelectSnapshotIntent\|hasChangePending" app/src/main` — pass (0 hits; neither is used today).
- `grep -n -i "roll" .planning/reports/phase-13/qa-engineer-device-pass.md` — pass (the Pixel_9a disk rollback to 2026-09-15 is confirmed at `:12` and `:75`).
- Reading `ProgressMerge.kt`, `CloudProgressCodec.kt` and `ProgressRepository.kt:421-506` — pass (union semantics, identity on an undecodable side, a single-transaction import).
- `ls app/src` — info (`main` and `test` only; there is no debug source set, hence the `BuildConfig.DEBUG` gate in E).
- `git status --short` — pass (clean; nothing edited).
- `./gradlew check` — not run (no source changed). Emulators were not touched.
- Not verified:
  - GMS's internal conflict-record behaviour behind H3;
  - whether `dumpsys content` lists a Play Games sync authority on these images;
  - whether `getDeviceName()` equals `Build.MODEL`.

## Open issues / risks
- H3 is an inference from logs. The server was never observed.
- If E1 points to H3, CLOUD-01 and CLOUD-02 cannot be verified without one founder sign-in on a clean AVD. The producer should queue that as a non-blocking founder question only after E1.
- E2 changes the `CloudSaveStore.load` seam, which has three implementations and their tests. It must go through the code-auditor and QA, and I will not sign off beyond this specification.
- Auditor items F-1 to F-6 stay on the snapshot-handle-lifetime task. Add the A(iii) note ("land what the push saw") to it. If the H3 prediction fails, F-3 (abandoned superseded conflict pairs) moves up to first suspect.
- `docs/tech-design.md` does not exist, so the decisions in C (E2), D and E are recorded only here.

## Handoff
Next: producer — dispatch E1 to qa-engineer on the current build. In parallel, create one android-game-dev task with these items:
- `COVER_PX` 384 with its test and the KDoc and A1 corrections;
- the post-pull push guard;
- the E2 pull probe with the KDoc and Pitfall 4 correction;
- the debug-only snapshot-picker entry.

Review as usual, then QA re-runs tests 10–13 according to E1's outcome. Route the CLOUD-03 wording to game-designer. Raise a founder question for a clean signed-in AVD only if E1 shows H3.
