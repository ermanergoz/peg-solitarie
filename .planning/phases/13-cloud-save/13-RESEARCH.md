# Phase 13: Cloud Save - Research

**Researched:** 2026-09-15
**Domain:** Play Games Services v2 Saved Games (`SnapshotsClient`) + a monotonic join-semilattice merge in `:domain`
**Confidence:** HIGH (API surface and size caps verified byte-for-byte against the pinned 22.0.0 artifact; codebase read in full)

## Summary

Every open question the ROADMAP flagged is now closed at HIGH confidence, and closed from the
**artifact itself** rather than from a web page. The `play-services-games-v2:22.0.0` AAR in this
machine's Gradle cache ships a javadoc jar; its `SnapshotsClient` page states that
`getMaxDataSize()` is *"Guaranteed to be at least 3 MB"* and `getMaxCoverImageSize()` is
*"Guaranteed to be at least 800 KB"*, both returned as `Task<Integer>` **in bytes**. D-05's
assumed 3 MB / 800 KB numbers are therefore correct, and they are a documented **floor**, not a
ceiling — the SDK additionally exposes both as runtime queries, which is exactly the "confirm it
at runtime" the ROADMAP asked for. `RESOLUTION_POLICY_MANUAL` is `-1` and is documented as *"This
policy ensures that no user changes to the state of the save game will ever be lost"*; the default
`RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED` is `3` and is documented as *"it is possible that a
player's changes may get lost"* — the ROADMAP's warning is Google's own wording.

The integration shape is almost entirely dictated by code that already exists. `ProgressSubmitter`
is the single orchestration point with one session collector, one pass mutex and one persisted
dirty flag; the cloud push and pull join that object rather than adding a second collector.
`DefaultProgressRepository.recordResultInTransaction` already contains the merge (max stars / min
time / max score) inline at three lines — extracting it to `:domain/progress/ProgressMerge.kt`
is a behaviour-preserving refactor that can and should land, fully tested, before any
`SnapshotsClient` code is written. `PlayGamesService` already owns the `ForegroundActivityProvider`
access, the `Task.await()` bridge (`kotlinx-coroutines-play-services` is **already** on the graph)
and the never-throw/never-log discipline that the snapshot calls must inherit. No new Gradle
artifact, no manifest change and **no Room schema bump** are required.

The two things the existing code does *not* give you for free are (a) a `SUM(elapsedMs)` query over
`play_session` for D-10's played time and a bulk read/write over `level_result` for the import, and
(b) a way to await the ladder write — `ChallengeProgress.advancePast` is fire-and-forget. Both are
small, additive and named precisely below.

**Primary recommendation:** Land `ProgressMerge` in `:domain` first (pure, exhaustively tested:
commutative, associative, idempotent, trim-safe), then a `ByteArray`-in/`ByteArray`-out
`CloudSaveStore` seam in `platform/games/save/` that hides every gms type, then wire the push/pull
into `ProgressSubmitter`'s existing pass. Use the **four-argument** `resolveConflict` overload with
`getResolutionSnapshotContents()` — the two-argument form can only pick one of the two existing
snapshots verbatim and cannot express a merge.

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Sync timing and restore feedback
- **D-01:** **Push after every solved level.** The cloud write piggybacks on the existing `ProgressSubmitter` pass that runs after `recordResult` returns (Phase 12 D-13's single orchestration point). No background-flush trigger. A failed push sets a persisted "cloud push pending" marker and is retried on the next pass, in the shape of Phase 12 D-10's dirty flag (exact home is discretion, see below).
- **D-02:** **Pull once per process, when `session` resolves to `SignedIn`** — the silent launch check or the first explicit sign-in (Phase 11 D-02 already limits sign-in to once per process). No re-pull on foreground return, no connectivity listener. A player opening the app on a new device gets their progress within seconds of the stock "Welcome back" popup.
- **D-03:** **Restore is fully silent.** No toast, no dialog, no Settings status row. Catalog star pips and the Challenge tile update live through the existing Room/DataStore flows; the stock "Welcome back" popup remains the only launch-time signal (parity with Phase 11 D-05). A cloud import must never fire the "New best!" flag or the Results celebration — those are per-play, not per-merge.
- **D-04:** **A stale in-progress game is kept; the ladder jumps underneath it.** If this device has a Challenge game in progress at level 10 and the merged ladder is 50, the Continue tile still resumes the level-10 game. Solving it calls `advancePast(10) = max(50, 11) = 50`, so the ladder stays at 50 and "Next level" serves level 50. Nothing on the resume path is touched by a cloud event (CLOUD-04/05).

#### Payload shape and size cap
- **D-05:** **Lossless by default; trim only at the cap.** The payload carries every Classic and Challenge best (`mode`, `levelId`, `stars`, `bestTimeMs`, `bestScore`) plus the ladder. At roughly 40 bytes per level in JSON, 10,000 Challenge levels is ~400 KB against a 3 MB cap. If a serialized payload ever approaches the **confirmed** cap (planner verifies the limit against current docs/SDK — research says 3 MB at HIGH confidence, ROADMAP flags it unverified), **drop the lowest Challenge levels first** until it fits. The trim is a tested safety valve that is expected never to fire in practice; it must never drop Classic rows or the ladder field.
- **D-06:** **`play_session` telemetry stays local.** Achievements already live on Play Games' servers, so a new device needs no history to keep them. The payload stays a pure join-semilattice of bests. (Consequence: on a fresh device the Phase 12 `play_session` existence flags read false; that is correct and does not un-earn anything.)
- **D-07:** **Encoding is kotlinx-serialization JSON with a top-level `schemaVersion` field**, the same library and shape as the existing resume-snapshot blob. Unknown fields are ignored on read so an older build can read a newer payload's known fields; a payload with a `schemaVersion` above what the build understands is treated as a failed load (fail open to local, D-11).
- **D-08:** **The ladder field is the persisted `ChallengeProgress.currentLevel`** (the next level to play), carried as its own field and **merged by max** on both sides. It is exactly what the Challenge tile shows, so a new device lands on the same "Level N" tile. Per-level bests merge independently. No UI calls `ChallengeProgress.reset()`, so the ladder is purely monotonic today; the merge relies on that.

#### Play Games listing metadata (CLOUD-03)
- **D-09:** **Cover image is bundled board art rasterized from the launcher icon vectors** (`ic_launcher_foreground` / `ic_launcher_background`, the approved English-33 design from quick task 260914-x4a) into a PNG well under the 800 KB cover limit. Static and deterministic; attached on every commit. No runtime board rendering.
- **D-10:** **Played time = `SUM(elapsedMs)` over `play_session`** (solved plays only), carried as a `playedTimeMs` field in the payload and **merged by max** so the metadata reflects the larger of the two devices' totals rather than double-counting. The snapshot's `playedTimeMillis` metadata is set from the merged value.
- **D-11:** **Description is one line: "Challenge level N · S stars · K/5 Classic boards"**, built from the Phase 12 SQL aggregates (`highestChallengeLevel`/ladder, `totalStars`, `clearedClassicBoards.size`). It goes through `strings.xml` and therefore **follows the device locale at commit time**; a player who changes language sees the new language after their next solve. Exact wording and number formatting are discretion.

#### Account switch (the one dialog) and verification
- **D-12:** **The app remembers the last player id its local progress was merged with** (a DataStore key beside the opt-in flags). When `session` resolves to `SignedIn` with a **different** player id than the stored one, the app shows **a one-time dialog** — the single case the v1.1 requirements reserved a dialog for. A fresh install or a first-ever sign-in (no stored id) never shows it; it merges silently per D-02.
- **D-13:** The dialog offers two choices: **"Keep this device's progress"** (merge local into the new account's cloud save exactly like a first sign-in) and **"Use [account]'s cloud progress"** (replace). **Back, tap-outside or any dismissal counts as "Keep this device's progress"** — the safe, non-destructive default.
- **D-14:** **The replace path resets everything to the new account:** wipe `level_result`, `play_session`, the Challenge ladder (back to level 1) and **both** resume snapshots, then import the cloud save. The device becomes that account's device outright; the previous account's data already lives in its own cloud save from earlier pushes. The dialog copy must state exactly what is replaced. If the new account has no cloud save yet, replace means a clean slate.
- **D-15:** **The dialog appears on Home only**, modeled on the Phase 11 opt-in prompt (`PlayGamesPromptDialog`, `HomeUiState` flag owned by `HomeViewModel`): never over gameplay, Results or Settings, and never stacked with the first-run onboarding overlay or the opt-in prompt. **No push or pull runs until it is answered.** After either answer the stored player id is updated to the signed-in one.
- **D-16:** **Verification is two emulators on the one existing tester account** (the `Pixel_9a` and `Pixel_9` AVDs): solve different levels on each, relaunch, assert the union on both, then kill the network on one and confirm local-only play. This covers CLOUD-01/02/05 honestly with what exists today. **The profile-swap dialog (D-12 to D-15) needs a second Google account and is recorded as a UAT gap in `11-HUMAN-UAT.md`**, exactly like its test 2. The Play Console "Enable Saved Games" toggle is a new operator-checklist item that blocks every device test.

### Claude's Discretion
- **Where the merge lives and its exact shape** in `:domain` (working name `ProgressMerge`): one pure function used by local `recordResult`, the cloud reconcile, and `SnapshotsClient` conflict resolution. `max` stars / `min` time / `max` score per `(mode, levelId)`, `max` ladder, `max` playedTime. Must be proven commutative, associative and idempotent by JVM tests before any SDK code is written (ROADMAP note).
- **Where the "cloud push pending" marker lives** — extending `SubmissionSyncStore` with a second key, or a sibling store; and whether one pass drains both the Phase 12 submissions and the cloud push, or the cloud push is its own serialized step inside the same `ProgressSubmitter` pass.
- **Snapshot identity**: one fixed snapshot name per player (e.g. `progress`), `createIfMissing = true`. Whether to handle "multiple snapshots exist" at all beyond picking the fixed name.
- **`SnapshotsClient` plumbing** inside `PlayGamesService` (or a sibling `platform/games/save/` class behind a small seam such as `CloudSaveStore` / `CloudSaveDataSource`): open with `RESOLUTION_POLICY_MANUAL`, handle `DataOrConflict.isConflict()` by merging both sides and resolving, `commitAndClose` with metadata change. All Activity access stays through `ForegroundActivityProvider`; no SDK type crosses into `data/` or `ui/`; `NoOpGamesService` and the test fake degrade silently.
- **Import mechanics**: how a merged payload lands in Room and DataStore (per-row upsert through the existing transactional merge vs a bulk transaction), and how "New best!" / celebration paths are guaranteed not to fire on import.
- **Whether a cloud import re-fires the Phase 12 submission pass** (setting the dirty flag after a merge that improved aggregates, so leaderboards and tiered achievements catch up). Recommended: yes, it is one flag set and idempotent.
- **Payload field names, the trim algorithm's exact threshold** (some margin below the confirmed cap), and the JVM test that proves the trim keeps Classic rows and the ladder.
- **Cover PNG generation**: rasterize at build time into `res/drawable` versus at first use into cache; size and DPI.
- **Dialog copy** (title, body naming what is replaced, the two button labels), string/dimen resources, and the exact `HomeUiState` flag shape for the profile-swap dialog.
- **Threading and timeouts** for `SnapshotsClient` calls (reuse `GamesConstants.ACTIVITY_WAIT_MS`-style bounds), and how a load timeout is treated (as a failed load, fail open).
- **Test doubles**: a fake cloud store for `ProgressSubmitter` / repository tests; the two-emulator UAT script's exact steps in `11-HUMAN-UAT.md`.

### Deferred Ideas (OUT OF SCOPE)
- Cloud-syncing the in-progress resume snapshot — explicitly deferred to v1.2 by the milestone requirements; only if players report losing mid-game state across devices.
- A Settings "Cloud save: last synced …" status row or a "Progress restored" snackbar — considered under D-03 and rejected for this phase; revisit if support requests show players cannot tell the sync ran.
- A rendered per-player board cover image (current Challenge level drawn to a bitmap) — considered under D-09; a personalization touch for a later polish pass.
- Re-pulling on every foreground return for same-account cross-device play mid-session — considered under D-02; only if real usage shows two devices active at once.
- Syncing the derived skill-achievement flags — considered under D-06; not needed while achievements live server-side.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CLOUD-01 | Player's per-level bests and Challenge ladder position are saved to the cloud and restored on a new device | § Standard Stack (`SnapshotsClient` verified surface), § Pattern 1 (seam shape), § Pattern 3 (import), § Code Examples 1–3 |
| CLOUD-02 | Progress from two devices merges without data loss and without asking the player to choose a save | § Pattern 2 (`ProgressMerge` in `:domain`), § Pattern 4 (bounded conflict loop with `RESOLUTION_POLICY_MANUAL` — verified to never auto-delete), § Validation Architecture (algebraic-law tests) |
| CLOUD-03 | The cloud save carries a description, played time, and cover image so it is legible in the Play Games UI | § Standard Stack (`SnapshotMetadataChange.Builder` verified), § Pattern 5 (cover rasterization), § Pitfall 7 (description locale + length), new `PlaySessionDao.totalElapsedMs()` |
| CLOUD-04 | The in-progress board snapshot stays local and is never synced | § Pattern 3 (import writes `level_result` + ladder only; never `resume_snapshot`, never `play_session`), § Validation Architecture (negative test) |
| CLOUD-05 | A cloud save or load failure degrades to local-only play without data loss or an error wall | § Don't Hand-Roll (reuse `storageFailSafe` / `submitting` discipline), § Pitfalls 1–4, § Environment Availability (network-kill UAT step) |
</phase_requirements>

---

## Project Constraints (from CLAUDE.md)

| Directive | What it means for this phase |
|-----------|------------------------------|
| Clean Architecture; `:domain` pure Kotlin, zero Android deps | `ProgressMerge` + the payload model go in `:domain` with **no** annotations. `:domain`'s only deps today are `junit` + `truth` (test-only) — see § Pitfall 9: do **not** add kotlinx-serialization to `:domain`. |
| Ads and Play Games behind interfaces | New `CloudSaveStore` interface in `platform/games/save/`, `@Binds`-ed in `di/PlatformModule.kt`. No gms type in any signature reaching `data/` or `ui/`. |
| No magic numbers or strings; scoped constants objects | Snapshot name, cap floor, cap margin, cover pixel size, retry bound each get a scoped constants object (`CloudSaveConstants` in `platform/games/save/`, payload keys in `data/cloud/`). |
| Compose state hoisting; dimens resource file | Profile-swap dialog is stateless, driven by a `HomeUiState` flag; all spacing via `dimensionResource`. Model on `ui/home/PlayGamesPromptDialog.kt`. |
| detekt + ktlint passing as a build gate | `MagicNumber` is active over `src/main` with only `-1/0/1/2` exempt. `RESOLUTION_POLICY_MANUAL` is a *named SDK constant*, not a literal, so it is fine; the retry bound and byte sizes are not. |
| Gitflow strictly | `feature/*` off `develop` (config: `gsd/phase-13-cloud-save`); no direct commits to `develop`/`main`. |
| KSP not KAPT; Compose BOM governs Compose versions | No new artifacts needed at all — see § Standard Stack. |

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Monotonic merge of two progress states | `:domain` (pure Kotlin) | — | Algebraic, Android-free, and used by three callers (local record, cloud import, conflict resolution). ROADMAP mandates it here and demands it be proven before SDK code exists. |
| Payload trim to fit a byte cap | `:domain` (row-selection policy) | `data/cloud` (encode → measure → shrink loop) | "Drop lowest Challenge first, never Classic, never the ladder" is a pure policy; "how many bytes did that produce" needs the serializer, which lives in `app`. |
| JSON encode/decode + `schemaVersion` gate | `data/cloud` (app layer) | — | Exact precedent: `data/resume/SnapshotMapper.kt` owns `@Serializable` and `Json { ignoreUnknownKeys = true }` because `:domain` carries no serialization annotations. |
| Room/DataStore write of the merged result | `data/ProgressRepository` + `data/ChallengeProgress` | — | Repository is the single source of truth (ARCHITECTURE Anti-Pattern 2). The cloud layer never touches a DAO. |
| Snapshot open / conflict resolve / commit | `platform/games/save/` | — | Every gms type and the `Activity` must stay inside `..platform..` (`PlatformBoundaryRules`). |
| Cover bitmap rasterization | `platform/games/save/` | — | Needs `android.graphics.Bitmap` and a `Context`; keeping it beside the only consumer avoids an Android graphics type in `data/`. |
| Description string composition | `platform/games/save/` (from `@ApplicationContext`) | `data/` supplies the three numbers | Keeps `data/` Context-free while honouring "all strings through `strings.xml`" and D-11's locale-at-commit-time behaviour. |
| Orchestration (when to pull, when to push, retry marker) | `data/ProgressSubmitter` | `data/SubmissionSyncStore` (or sibling) | D-01/D-02 explicitly join the existing single pass; a second session collector is forbidden by the WR-01 race note. |
| Profile-swap decision surface | `ui/home` | `data/PlayGamesOptInRepository` (stored player id) | D-15: Home only, state-hoisted, modeled on `PlayGamesPromptDialog`. |

---

## Standard Stack

### Core — nothing new is needed

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.google.android.gms:play-services-games-v2` | **22.0.0** (already pinned) | Ships `SnapshotsClient`, `Snapshot`, `SnapshotContents`, `SnapshotMetadata`, `SnapshotMetadataChange` | `[VERIFIED: local AAR]` — `PlayGames.getSnapshotsClient(Activity)` and all snapshot classes are present in `classes.jar` of the cached 22.0.0 AAR. No Drive artifact, no extra scope. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | **1.11.0** (already on the graph) | `Task<T>.await()` | `[VERIFIED: gradle/libs.versions.toml + app/build.gradle.kts:441]` — declared `version.ref = "coroutines"`, already used by `PlayGamesService`. |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | **1.11.0** (already on the graph) | Payload encode/decode in `data/cloud` | `[VERIFIED: app/build.gradle.kts:407]` — same library and `Json { ignoreUnknownKeys = true }` shape as `SnapshotMapper`. |
| `androidx.core:core-ktx` | **1.18.0** (already on the graph) | `Drawable.toBitmap(w, h, config)` for the cover image | `[VERIFIED: local AAR]` — `androidx/core/graphics/drawable/DrawableKt.toBitmap(Drawable, int, int, Bitmap$Config)` confirmed present by `javap`. |

**Installation:** none. `./gradlew :app:dependencies` will not change. This phase adds **zero** new coordinates, which is why there is no Package Legitimacy Audit section below.

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| 4-arg `resolveConflict(conflictId, snapshotId, metadataChange, contents)` | 2-arg `resolveConflict(conflictId, snapshot)` | The 2-arg form can only push **one of the two existing snapshots verbatim**. It cannot express a merge, so it collapses to a policy choice — exactly what D-02/CLOUD-02 forbid. Use the 4-arg form. |
| `RESOLUTION_POLICY_MANUAL` (`-1`) | `RESOLUTION_POLICY_HIGHEST_PROGRESS` (`4`) with `setProgressValue(ladder)` | Tempting, because the ladder *is* a monotonic progress value. Rejected: it still discards the losing device's per-level bests and star counts wholesale. Google's own doc for MANUAL says it *"ensures that no user changes … will ever be lost"*. |
| A single fixed snapshot name | Multiple snapshots + `getSelectSnapshotIntent` | Out of scope — the requirements reserve exactly one dialog, and that one is the profile swap. One name, `createIfNotFound = true`. |
| Runtime vector→bitmap cover | Build-time PNG committed to `res/drawable-nodpi/` | See § Pattern 5 — runtime wins here because the repo has **no PNG rasterizer in its authoring pipeline** and a committed PNG would be a second copy of the art that silently drifts from the vectors. |
| Adding a property-testing library (kotest/jqwik) | Plain JUnit4 loops with a seeded `Random` | `:domain`'s entire dependency set is `junit` + `truth`. Adding a generator framework for four algebraic laws is a poor trade; § Validation Architecture gives the loop shapes instead. |

---

## Verified SDK Surface (`play-services-games-v2:22.0.0`)

All of the following was read from `javap` output over `classes.jar` and from the javadoc HTML
bundled in `play-services-games-v2-22.0.0-javadoc.jar`, both taken from this machine's Gradle
cache. This is the **exact artifact the build resolves**, so it supersedes any web page.
`[VERIFIED: local AAR + bundled javadoc]`

### Entry point

```
public final class com.google.android.gms.games.PlayGames {
  public static SnapshotsClient getSnapshotsClient(android.app.Activity);   // Activity, not Context
}
```

### `SnapshotsClient`

```
public interface com.google.android.gms.games.SnapshotsClient {
  public static final int RESOLUTION_POLICY_MANUAL                 = -1;
  public static final int RESOLUTION_POLICY_LONGEST_PLAYTIME       =  1;
  public static final int RESOLUTION_POLICY_LAST_KNOWN_GOOD        =  2;
  public static final int RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED =  3;
  public static final int RESOLUTION_POLICY_HIGHEST_PROGRESS       =  4;
  public static final int DISPLAY_LIMIT_NONE                       = -1;

  Task<Integer>                     getMaxDataSize();
  Task<Integer>                     getMaxCoverImageSize();
  Task<DataOrConflict<Snapshot>>    open(String fileName, boolean createIfNotFound);
  Task<DataOrConflict<Snapshot>>    open(String fileName, boolean createIfNotFound, int conflictPolicy);
  Task<DataOrConflict<Snapshot>>    open(SnapshotMetadata metadata);
  Task<DataOrConflict<Snapshot>>    open(SnapshotMetadata metadata, int conflictPolicy);
  Task<SnapshotMetadata>            commitAndClose(Snapshot, SnapshotMetadataChange);
  Task<Void>                        discardAndClose(Snapshot);
  Task<DataOrConflict<Snapshot>>    resolveConflict(String conflictId, Snapshot);
  Task<DataOrConflict<Snapshot>>    resolveConflict(String conflictId, String snapshotId,
                                                    SnapshotMetadataChange, SnapshotContents);
  Task<String>                      delete(SnapshotMetadata);
  Task<AnnotatedData<SnapshotMetadataBuffer>> load(boolean forceReload);
  Task<Intent>                      getSelectSnapshotIntent(String, boolean, boolean, int);
}
```

Javadoc facts that change the design:

| Method / constant | Documented behaviour (quoted) | Design consequence |
|---|---|---|
| `getMaxDataSize()` | *"loads the maximum data size per snapshot in bytes. **Guaranteed to be at least 3 MB.** May increase in the future."* Task *"can fail with a `RemoteException`."* | D-05's 3 MB is a documented floor. Query it; fall back to the 3 MB constant when the Task fails. |
| `getMaxCoverImageSize()` | *"maximum data size per snapshot cover image in bytes. **Guaranteed to be at least 800 KB.**"* | D-09's 800 KB is confirmed. |
| `RESOLUTION_POLICY_MANUAL` | *"the result will be returned to the app for resolution. No automatic resolution will be performed. This policy ensures that **no user changes to the state of the save game will ever be lost.**"* Value `-1`. | Answers Q3's "guarantee that MANUAL never auto-deletes" — it is Google's own wording. |
| `RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED` | *"blindly chooses the most recent data, it is possible that **a player's changes may get lost**."* | The ROADMAP's warning is documented, not folklore. |
| `open(String, boolean)` (2-arg) | *"This will open the snapshot using `RESOLUTION_POLICY_MANUAL` as a conflict policy."* | The 2-arg overload already defaults to MANUAL. Still pass the constant explicitly — it makes the intent unmissable in review. |
| `open(String fileName, …)` | *"Must be between **1 and 100 non-URL-reserved characters** (a-z, A-Z, 0-9, or the symbols `-`, `.`, `_`, or `~`)."* | `"progress"` is valid. Put it in a scoped constant and add a unit test asserting the name matches `^[A-Za-z0-9\-._~]{1,100}$`. |
| `commitAndClose` | *"fails a `Task` with an exception when called with a snapshot that was **not opened or has already been committed/discarded**."* Also: *"the total size of the contents … may not exceed the size provided by `getMaxDataSize()`."* | Every opened snapshot must reach exactly one of `commitAndClose` / `discardAndClose`, in a `finally`. |
| `resolveConflict(…)` (both forms) | *"it is possible for this operation to **result in a conflict itself, in which case resolution should be repeated**."* 4-arg form: *"This will replace the data on the server with the specified `SnapshotMetadataChange` and `SnapshotContents`."* *"Values which are not included in the metadata change will be resolved to the version currently on the server."* | Bounded retry loop required. The 4-arg form is the merge-capable one. |
| `open(…)` / `resolveConflict(…)` failure | *"If the snapshot's contents are unavailable, the Task will fail with `SnapshotsClient.SnapshotContentUnavailableApiException`."* | A named exception type inside `platform/` — classify it as "failed load, fail open" (D-11 / CLOUD-05), never surface it. |

### `DataOrConflict` / `SnapshotConflict`

```
public final class SnapshotsClient$DataOrConflict<T> {
  public boolean isConflict();
  public T getData();
  public SnapshotsClient$SnapshotConflict getConflict();
}

public final class SnapshotsClient$SnapshotConflict {
  public Snapshot        getSnapshot();                    // "the server's version of the Snapshot that was opened"
  public Snapshot        getConflictingSnapshot();         // "the modified version of the Snapshot"
  public String          getConflictId();
  public SnapshotContents getResolutionSnapshotContents(); // "used to update the data in case of a conflict"
}
```

### `SnapshotContents`, `Snapshot`, `SnapshotMetadataChange`

```
public interface SnapshotContents extends Parcelable {
  byte[]  readFully() throws IOException;
  boolean writeBytes(byte[] content);                       // FULLY overwrites
  boolean modifyBytes(int dstOffset, byte[] content, int srcOffset, int count);  // partial; excess REMAINS
  boolean isClosed();
  ParcelFileDescriptor getParcelFileDescriptor();
}

public interface Snapshot extends Freezable<Snapshot>, Parcelable {
  SnapshotMetadata  getMetadata();
  SnapshotContents  getSnapshotContents();
}

public static final class SnapshotMetadataChange.Builder {
  Builder setDescription(String);
  Builder setPlayedTimeMillis(long);
  Builder setCoverImage(android.graphics.Bitmap);
  Builder setProgressValue(long);
  Builder fromMetadata(SnapshotMetadata);
  SnapshotMetadataChange build();
}
// SnapshotMetadataChange.EMPTY_CHANGE exists — "preserve the existing metadata".
// SnapshotMetadata.getSnapshotId() / getUniqueName() / getDescription() / getPlayedTime() /
// getLastModifiedTimestamp() / getCoverImageAspectRatio() are read-back accessors.
```

`SnapshotContents.writeBytes` is documented: *"this form of the API will fully overwrite the
contents of the file. No previous data will be retained."* `modifyBytes` is documented: *"If you
are writing less data than was previously stored in the snapshot, **the excess data will
remain**."* → **always `writeBytes`** (§ Pitfall 5).

### Manifest and Play Console

`[VERIFIED: local AAR AndroidManifest.xml]` The AAR's own manifest declares `PlayGamesInitProvider`
(already removed by `tools:node="remove"` in this app), `GamesResolutionActivity`,
`PlayGamesAppShortcutsActivity` and the `com.google.android.gms.games.version` meta-data. It
declares **nothing snapshot-specific**. `[VERIFIED: app/src/main/AndroidManifest.xml:81-83]` The app
already declares `com.google.android.gms.games.APP_ID` pointing at
`@string/game_services_project_id`. **No manifest change is needed for this phase.**

`[CITED: developer.android.com/games/pgs/android/saved-games]` *"You must Enable saved games
support for your game in Google Play Console before implementation."* This is a Play Console
toggle (Play Games Services → your game → Configuration/Features), it is **off by default**, and
without it every `SnapshotsClient` call fails. It is already listed in the ROADMAP's External
Prerequisites (`| Play Console: Saved Games enabled on the PGS project | Phase 13 | Minutes |`) —
the plan must surface it as a blocking operator-checklist step, because it blocks **100%** of device
verification and produces a silent failure rather than a loud one.

---

## Architecture Patterns

### System Architecture Diagram

```
 MainActivity.onCreate ──► progressSubmitter.start()      [existing, once per process]
                                    │
                                    ▼
                    GamesService.session : StateFlow<GamesSignInState>
                                    │  (Unknown → SignedOut | SignedIn(playerId, name))
                                    ▼
 ┌──────────────────────── data/ProgressSubmitter  (@Singleton, ONE collector, ONE passMutex) ──┐
 │                                                                                              │
 │   trigger A: session == SignedIn        trigger B: ResultsViewModel.onResultRecorded()       │
 │            │                                          │                                      │
 │            └──────────────┬───────────────────────────┘                                      │
 │                           ▼                                                                  │
 │                  passMutex.withLock {                                                        │
 │                     [0] session gate  (not SignedIn → mark BOTH pending, return)             │
 │                     [1] PROFILE GATE  — storedPlayerId vs session.playerId                   │
 │                            mismatch ─────► raise HomeUiState flag, return (NO push/pull)     │
 │                     [2] PULL (once per process, AtomicBoolean)                               │
 │                            │                                                                 │
 │                     [3] existing Phase-12 leaderboard/achievement submission pass            │
 │                     [4] PUSH (if dirty or just pulled)                                       │
 │                  }                                                                           │
 └──────────────────────────┬───────────────────────────────────────────────────────────────────┘
                            │ ByteArray in / ByteArray out — no gms type crosses this line
                            ▼
 ┌──── platform/games/save/CloudSaveStore  (interface, @Binds in di/PlatformModule) ────────────┐
 │  PlayGamesCloudSaveStore                       NoOpCloudSaveStore / FakeCloudSaveStore       │
 │        │                                                                                     │
 │        ├─ ForegroundActivityProvider.awaitActivity()  [bounded by ACTIVITY_WAIT_MS]          │
 │        ├─ PlayGames.getSnapshotsClient(activity)                                             │
 │        ├─ open("progress", createIfNotFound = true, RESOLUTION_POLICY_MANUAL).await()        │
 │        │      └─ isConflict? ──► mergeBytes(server, conflicting) ──► resolveConflict(4-arg)  │
 │        │                            └──► still conflict? repeat, bounded (≤ 10)              │
 │        ├─ snapshot.snapshotContents.readFully()          → ByteArray  (PULL)                 │
 │        ├─ snapshot.snapshotContents.writeBytes(bytes)                 (PUSH)                 │
 │        └─ commitAndClose(snapshot, metadataChange)  ── or discardAndClose in finally          │
 │               metadataChange = description(strings.xml) + playedTimeMillis + coverImage      │
 └──────────────────────────┬───────────────────────────────────────────────────────────────────┘
                            │ merged ByteArray
                            ▼
 ┌──── data/cloud/CloudProgressCodec  (@Serializable DTO, Json{ignoreUnknownKeys}) ─────────────┐
 │   decode → schemaVersion gate → CloudProgress (pure :domain model)                           │
 │   encode ← trim loop:  encode → measure → ProgressMerge.dropLowestChallenge(n) → repeat      │
 └──────────────────────────┬───────────────────────────────────────────────────────────────────┘
                            ▼
         :domain/progress/ProgressMerge   (PURE — no android.*, no androidx.*, no @Serializable)
              merge(a, b) : max stars · min time · max score · max ladder · max playedTime
                            │
                            ▼
 ┌──── data/ProgressRepository.importProgress(CloudProgress)  [ONE database.withTransaction ] ──┐
 │   levelResultDao.all() ──► ProgressMerge ──► levelResultDao.upsertAll(merged)                │
 │   NEVER touches resume_snapshot (CLOUD-04)   NEVER inserts play_session (D-06)               │
 │   returns Unit / ImportOutcome — NEVER NewBests  (so "New best!" structurally cannot fire)   │
 └──────────────────────────┬───────────────────────────────────────────────────────────────────┘
                            ├──► ChallengeProgress.mergeLadder(level)   [DataStore, max, awaitable]
                            ▼
      existing Flows re-emit ──► Home Challenge tile + Classic catalog star pips update in place
```

### Recommended Project Structure

```
domain/src/main/kotlin/com/app/pegsolitaire/domain/progress/
├── ProgressMerge.kt          # NEW — the one pure merge + the trim policy
├── CloudProgress.kt          # NEW — pure payload model (no annotations)
└── ProgressMergeLimits.kt    # NEW — scoped constants (or fold into ProgressMerge)

app/src/main/java/com/app/pegsolitaire/data/cloud/
├── CloudProgressDto.kt       # NEW — @Serializable mirror + CloudPayloadKeys constants
└── CloudProgressCodec.kt     # NEW — encode/decode/trim loop (SnapshotMapper's role)

app/src/main/java/com/app/pegsolitaire/platform/games/save/
├── CloudSaveStore.kt         # NEW — the seam. ByteArray in/out. No gms types.
├── PlayGamesCloudSaveStore.kt# NEW — SnapshotsClient plumbing + conflict loop
├── NoOpCloudSaveStore.kt     # NEW — degrades silently
├── CloudSaveConstants.kt     # NEW — snapshot name, cap floor/margin, retry bound, cover px
└── CoverImageSource.kt       # NEW — vector → Bitmap, cached, off the main thread

app/src/main/res/drawable/
└── cloud_save_cover.xml      # NEW — layer-list: ic_launcher_background + ic_launcher_foreground
```

### Pattern 1 — The seam carries bytes, not snapshots

**What:** `CloudSaveStore` exposes only Kotlin/JVM types. Everything `Snapshot`-shaped stays inside
`PlayGamesCloudSaveStore`.

**Why:** `PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks` is bytecode-based and matches
`com.google.android.gms..` *anywhere outside `..platform..`* — including the Hilt-generated
`DaggerApp_HiltComponents_SingletonC`, which carries a `Provider<T>` field per binding. A
`@Provides SnapshotsClient` anywhere would fail the gate. `[VERIFIED: app/src/test/.../PlatformBoundaryRules.kt]`

```kotlin
// platform/games/save/CloudSaveStore.kt
interface CloudSaveStore {
    /**
     * The player's cloud payload bytes, or null when there is none / it could not be read.
     * NEVER throws: every failure — offline, no Activity, Play Console misconfigured,
     * contents unavailable — is `null`, which the caller reads as "stay local" (CLOUD-05).
     */
    suspend fun load(): ByteArray?

    /**
     * Writes [bytes] with the listing metadata (CLOUD-03), reporting acceptance.
     * `false` means "nothing reached the cloud"; the caller sets the pending marker (D-01).
     * [mergeRemote] resolves a conflict: given the two conflicting payloads' bytes it
     * returns the merged bytes. Called on the caller's dispatcher, synchronously pure.
     */
    suspend fun save(
        bytes: ByteArray,
        description: CloudSaveDescription,   // three ints, NOT a formatted string
        playedTimeMs: Long,
        mergeRemote: (ByteArray, ByteArray) -> ByteArray,
    ): Boolean
}
```

Passing `mergeRemote` as a lambda is what lets the *conflict* path reuse the exact same merge as
the local path without the platform layer knowing what a `CloudProgress` is. `CloudSaveDescription`
is a plain `data class(ladder: Int, totalStars: Int, classicCleared: Int)`; the platform layer
turns it into a localized `strings.xml` string (§ Pitfall 7).

**Anti-pattern this replaces:** adding `saveToCloud()` to `GamesService`. The seam's own KDoc says
*"Cloud save (Saved Games) is explicitly out of scope here — it arrives as a second data source
behind `ProgressRepository`, not as a method on this seam."* `[VERIFIED: platform/games/GamesService.kt]`
Honour that; add a sibling interface.

### Pattern 2 — Extract the merge that already exists, first, as its own plan

`DefaultProgressRepository.recordResultInTransaction` already holds the merge:

```kotlin
// app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt  (current)
levelResultDao.upsert(
    LevelResultEntity(
        mode = mode, levelId = levelId,
        stars      = maxOf(result.stars, previous?.stars ?: ProgressConstants.NO_STARS),
        bestTimeMs = minOf(timeMs,       previous?.bestTimeMs ?: Long.MAX_VALUE),
        bestScore  = maxOf(result.score, previous?.bestScore ?: ProgressConstants.NO_SCORE),
        completedAt = if (newBests.any) nowMs else previous?.completedAt ?: nowMs,
    ),
)
```

`ChallengeProgress.advancePast` already holds the ladder half (`maxOf(current, solvedLevel + 1)`,
computed inside the `edit` transaction). Both are already commutative/associative/idempotent; the
phase's job is to *name* them, not to invent them.

**Sequencing (ROADMAP mandate):** the extraction plan must land and be green **before any
`SnapshotsClient` code is written**. It is a zero-behaviour-change refactor whose regression net is
the existing `ProgressRepositoryTest` / `ChallengeProgressTest` plus the new algebraic-law tests.

```kotlin
// domain/src/main/kotlin/com/app/pegsolitaire/domain/progress/CloudProgress.kt  — PURE
data class LevelBest(
    val mode: ProgressMode,          // enum CLASSIC / CHALLENGE — NOT the raw on-disk string
    val levelId: String,
    val stars: Int,
    val bestTimeMs: Long,
    val bestScore: Int,
)

data class CloudProgress(
    val ladder: Int,                 // ChallengeProgress.currentLevel (next level to play)
    val playedTimeMs: Long,
    val bests: Map<LevelKey, LevelBest>,
)

// domain/src/main/kotlin/com/app/pegsolitaire/domain/progress/ProgressMerge.kt  — PURE
object ProgressMerge {
    fun merge(a: LevelBest, b: LevelBest): LevelBest = LevelBest(
        mode = a.mode, levelId = a.levelId,
        stars      = maxOf(a.stars, b.stars),
        bestTimeMs = minOf(a.bestTimeMs, b.bestTimeMs),
        bestScore  = maxOf(a.bestScore, b.bestScore),
    )

    fun merge(a: CloudProgress, b: CloudProgress): CloudProgress = CloudProgress(
        ladder       = maxOf(a.ladder, b.ladder),
        playedTimeMs = maxOf(a.playedTimeMs, b.playedTimeMs),
        bests        = (a.bests.keys + b.bests.keys).associateWith { key ->
            val left = a.bests[key]; val right = b.bests[key]
            when { left == null -> right!!; right == null -> left; else -> merge(left, right) }
        },
    )

    /** D-05's trim: never a Classic row, never the ladder, lowest Challenge levels first. */
    fun dropLowestChallenge(progress: CloudProgress, count: Int): CloudProgress = ...
}
```

Note `bestTimeMs`'s identity element is `Long.MAX_VALUE`, not `0` — the min-merge's absorbing
value. A row with `bestTimeMs == 0` would poison every merge it touches. Make that explicit in a
named constant and cover it with a test.

`completedAt` is deliberately **absent from the payload**. It means "last improved on this device"
and has no cross-device meaning; syncing it would make the merge non-idempotent (the value that
changes is not the value that determines whether anything changed). Keep it local: on import,
preserve the existing local `completedAt` for an existing row and stamp `nowMs` for a newly
arriving row.

### Pattern 3 — Import is one transaction and one bulk write

**What:** `ProgressRepository.importProgress(remote: CloudProgress)` reads all rows once, merges in
Kotlin, writes back once, inside one `database.withTransaction { }`.

**Why not per-row read-modify-write:** a 10,000-row Challenge ladder means 10,000 point queries
inside one transaction. The lost-update concern that justifies per-row RMW in `recordResult` does
not apply here, because the entire read+merge+write is already inside one transaction.

**Why not a naked `upsertAll(remoteRows)`:** that is ARCHITECTURE Anti-Pattern 2 in its most direct
form — a stale cloud row would clobber a better local best. The merge must happen between the read
and the write.

New DAO members required (all additive, **no schema change, no `DB_VERSION` bump**):

```kotlin
// data/db/LevelResultDao.kt
@Query("SELECT * FROM $TABLE_LEVEL_RESULT")
suspend fun all(): List<LevelResultEntity>

@Upsert
suspend fun upsertAll(entities: List<LevelResultEntity>)

// data/db/PlaySessionDao.kt   — D-10's played time
@Query("SELECT COALESCE(SUM($COLUMN_ELAPSED_MS), 0) FROM $TABLE_PLAY_SESSION")
suspend fun totalElapsedMs(): Long
```

```kotlin
// data/ProgressRepository.kt
override suspend fun importProgress(remote: CloudProgress) {
    storageFailSafe(fallback = Unit) {                       // CLOUD-05: fail open to local
        database.withTransaction {
            val local = levelResultDao.all()
            val merged = ProgressMerge.merge(local.toCloudBests(), remote.bests)
            levelResultDao.upsertAll(merged.toEntities(preservingCompletedAtFrom = local, nowMs))
            // resume_snapshot: untouched (CLOUD-04).  play_session: untouched (D-06).
        }
    }
}
```

`importProgress` returns `Unit`. **That is the CLOUD-04/D-03 guarantee, and it is structural rather
than a convention:** `NewBests` is constructed in exactly one place (`recordResultInTransaction`)
and consumed in exactly one place (`ResultsViewModel.recordSolvedPlayOnce()` →
`_uiState.update { it.copy(newBests = …) }`), and the star-pop celebration is claimed from
`SavedStateHandle[CELEBRATION_KEY]` on Results *entry*, not from any data change.
`[VERIFIED: ui/results/ResultsViewModel.kt:413-437, 453; ui/results/ResultsScreen.kt:137]`
An import path that never calls `recordResult` and never returns a `NewBests` cannot reach either.
Add one negative test asserting `importProgress` leaves `play_session` row count and both
`resume_snapshot` rows byte-identical.

**Ladder:** `ChallengeProgress.advancePast(remote.ladder - 1)` computes `max(current, remote.ladder)`
— exactly D-08 — but it is `fun`, not `suspend`, and fire-and-forgets onto the app scope.
`[VERIFIED: data/ChallengeProgress.kt]` The push that follows a pull must read a settled value, so
add an awaitable sibling:

```kotlin
suspend fun mergeLadder(level: Int) = storageFailSafe(fallback = Unit) {
    dataStore.edit {
        val current = it[CURRENT_LEVEL_KEY] ?: DEFAULT_CURRENT_LEVEL
        it[CURRENT_LEVEL_KEY] = maxOf(current, level)
    }
}
```

`advancePast` should then be a one-line `scope.launch { mergeLadder(solvedLevel + 1) }`, so there is
one max-merge, not two. The ladder lives in DataStore and the bests live in Room, so the two writes
are **not** atomic with each other. That is acceptable and should be stated in the plan: both are
idempotent and monotonic, so a crash between them only leaves work for the next pass, never a
regression.

### Pattern 4 — The bounded conflict loop

`resolveConflict` is documented to be able to produce a conflict itself. Google's own sample caps
retries at `MAX_SNAPSHOT_RESOLVE_RETRIES = 10`. `[CITED: developer.android.com/games/pgs/android/saved-games]`

```kotlin
// platform/games/save/PlayGamesCloudSaveStore.kt  (sketch — full example in § Code Examples)
private suspend fun resolveToOpenSnapshot(
    client: SnapshotsClient,
    initial: SnapshotsClient.DataOrConflict<Snapshot>,
    mergeRemote: (ByteArray, ByteArray) -> ByteArray,
): Snapshot? {
    var result = initial
    repeat(CloudSaveConstants.MAX_CONFLICT_RETRIES) {
        if (!result.isConflict) return result.data
        val conflict = result.conflict
        val serverBytes = conflict.snapshot.snapshotContents.readFully()
        val mineBytes = conflict.conflictingSnapshot.snapshotContents.readFully()
        val merged = mergeRemote(serverBytes, mineBytes)

        val contents = conflict.resolutionSnapshotContents
        contents.writeBytes(merged)                 // writeBytes, never modifyBytes
        result = client.resolveConflict(
            conflict.conflictId,
            conflict.snapshot.metadata.snapshotId,
            SnapshotMetadataChange.EMPTY_CHANGE,    // metadata applied once, at commit
            contents,
        ).await()
    }
    return null   // exhausted retries → treated as a failed pass, pending marker stays set
}
```

Three things the plan must not get wrong:

1. **The loop's exit value is an *open* snapshot.** `resolveConflict` already wrote the merged bytes
   to the server, but the returned `DataOrConflict.getData()` is an open `Snapshot` that still must
   reach `commitAndClose` or `discardAndClose`. Leaving it open blocks the next `open`.
2. **`EMPTY_CHANGE` during resolution, real metadata at the final commit.** The javadoc says
   *"Values which are not included in the metadata change will be resolved to the version currently
   on the server"* — supplying a half-built metadata change mid-loop would fight the final commit.
3. **`getSnapshot()` is the server's version; `getConflictingSnapshot()` is the modified version.**
   Because the merge is commutative, getting the two the wrong way round is *harmless here* — which
   is precisely why a test must pin the orientation anyway, so a future non-commutative field cannot
   be added silently.

### Pattern 5 — Cover image: rasterize the existing vectors at first use

`[VERIFIED: app/src/main/res/drawable/]` The repo contains `ic_launcher_background.xml` (1,956 B,
full-bleed 512-unit canvas), `ic_launcher_foreground.xml` (34,443 B, the English-33 board at 0.55
scale about centre), `ic_launcher_background_legacy.xml`, `ic_launcher_monochrome.xml`. There is
deliberately **no PNG anywhere in the tree** — the `mipmap-anydpi/ic_launcher.xml` comment states
*"There are no PNG mipmaps anywhere in the tree by design."*

**Recommendation: runtime rasterization, not a build-time PNG.**

| | Runtime (recommended) | Build-time PNG in `res/drawable-nodpi/` |
|---|---|---|
| Art drift | Impossible — one source of truth | A second copy that silently diverges from the vectors |
| New binary in repo | None | ~50–150 KB PNG, un-reviewable in a diff |
| Authoring pipeline | None needed | Needs a rasterizer the repo does not have |
| Runtime cost | One inflate + one 512×512 draw, once per process, off the main thread | Zero |
| Failure mode | Cover omitted; commit still succeeds | None |

Do **not** rasterize `R.mipmap.ic_launcher` — on API 26+ that resolves to the `<adaptive-icon>`,
whose `AdaptiveIconDrawable.draw()` applies its own mask, so the cover would come out circle-cropped
and launcher-dependent. Add a dedicated, unmasked, full-bleed composite instead:

```xml
<!-- app/src/main/res/drawable/cloud_save_cover.xml -->
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:drawable="@drawable/ic_launcher_background" />
    <item android:drawable="@drawable/ic_launcher_foreground" />
</layer-list>
```

```kotlin
// platform/games/save/CoverImageSource.kt
@Singleton
class CoverImageSource @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {
    @Volatile private var cached: Bitmap? = null

    /** Null on any failure — the commit then omits setCoverImage rather than failing. */
    suspend fun coverOrNull(): Bitmap? = cached ?: withContext(Dispatchers.Default) {
        runCatching {
            ContextCompat.getDrawable(appContext, R.drawable.cloud_save_cover)
                ?.toBitmap(
                    CloudSaveConstants.COVER_PX,
                    CloudSaveConstants.COVER_PX,
                    Bitmap.Config.ARGB_8888,
                )
        }.getOrNull()?.also { cached = it }
    }
}
```

- **No Activity is needed** — `@ApplicationContext` suffices; only `getSnapshotsClient` needs the
  Activity.
- **Off the main thread:** `Resources.getDrawable` is not *documented* thread-safe but the platform
  `Resources` synchronizes internally, and this runs once per process on a non-UI path where a
  failure degrades to "no cover". `[ASSUMED]` — if the planner wants zero risk, hop to
  `Dispatchers.Main` for the inflate only and keep the `toBitmap` draw on `Default`.
- **Size:** `COVER_PX = 512` matches the art's native 512-unit design canvas exactly, so there is no
  resampling. A 512×512 ARGB_8888 bitmap is 1 MB *in memory*, but `setCoverImage` transports it via
  `BitmapTeleporter` (a PNG-compressed pipe), and this art — flat gradients plus 33 circles —
  compresses to well under 100 KB. The 800 KB ceiling applies to the transported bytes.
  `[ASSUMED — the 800 KB accounting basis is not documented]`. Mitigation, cheap and worth doing:
  in the first device UAT run, call `getMaxCoverImageSize()` and compare against
  `bitmap.compress(PNG, 100, …).size` written to a scratch stream; record the number in
  `11-HUMAN-UAT.md`. If it ever looks close, drop `COVER_PX` to 384 or 256 — one constant.
- **Aspect ratio:** no Play Games requirement is documented; `SnapshotMetadata.getCoverImageAspectRatio()`
  is a read-back, not a constraint. Square matches the icon art. `[ASSUMED]`

### Anti-Patterns to Avoid

- **`RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED`** — Google's own javadoc: *"a player's changes may
  get lost."* This is the phase's single most damaging possible line of code.
- **`modifyBytes` instead of `writeBytes`** — leaves stale trailing bytes when the new payload is
  shorter than the old one, producing a JSON blob with garbage appended that decodes fine on some
  parsers and throws on others. Always `writeBytes`.
- **A second `session` collector** — `ProgressSubmitter`'s class KDoc spells out the WR-01 race in
  full. The pull and the profile-id check run inside the *existing* collector's critical section.
- **Cloud code touching a DAO or DataStore directly** — ARCHITECTURE Anti-Pattern 2; two write
  paths, one of which bypasses the merge.
- **Awaiting the push from `viewModelScope`** — `ProgressSubmitter.onResultRecorded()` is
  deliberately non-`suspend` and launches on the application scope. A back-navigation mid-push
  would otherwise cancel the very pending-marker write that makes the failure recoverable.
- **Inserting `play_session` rows on import** — would fabricate `anyHintFreeSolve` / `anySpeedSolve`
  / `anyCenterFinish` flags for plays that never happened on this device, silently granting skill
  achievements. D-06 forbids syncing telemetry; the import must forbid writing it.
- **Blocking Home on the pull** — ARCHITECTURE is explicit: *"sync has no ordering requirement
  relative to UI startup"*; `ChallengeProgress.currentLevel` is `SharingStarted.Eagerly`, so a merge
  landing 800 ms after Home renders simply updates the tile.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| `Task<T>` → `suspend` | `suspendCancellableCoroutine` wrappers | `kotlinx-coroutines-play-services` `Task.await()` | Already on the graph, already the idiom in `PlayGamesService`. Hand-rolling re-implements cancellation and exception mapping per call site. |
| Conflict resolution | Timestamp comparison, "newest wins", a user prompt | `RESOLUTION_POLICY_MANUAL` + `ProgressMerge` | The data model is already a join-semilattice. Google documents MANUAL as never losing changes. |
| Swallowing storage failures | Ad-hoc `try/catch` per call | `data/StorageFailSafe.storageFailSafe(fallback) { }` | Established project-wide; rethrows `CancellationException` correctly, which hand-written catches routinely get wrong. |
| Swallowing SDK failures | Ad-hoc `try/catch` | The `PlayGamesService.submitting` / `attempt` shape (`runCatching` + rethrow `CancellationException` + `getOrDefault(false)`) | Same reason; and it is what makes "failure is a return value, not an exception" (PGSL-04) uniform. |
| Bounded Activity waits | A raw `awaitActivity()` | `withTimeoutOrNull(GamesConstants.ACTIVITY_WAIT_MS) { activityProvider.awaitActivity() }` | An unbounded await inside a process-lifetime singleton hangs it for the life of the process (T-11-06). |
| Vector → bitmap | Manual `Bitmap.createBitmap` + `Canvas` + `setBounds` | `androidx.core.graphics.drawable.toBitmap(w, h, config)` | Verified present in core-ktx 1.18.0; handles bounds save/restore correctly. |
| Exactly-once per process | A boolean field | `AtomicBoolean.compareAndSet` | The idiom already used by `PlayGamesService.bootstrapped` / `sdkInitialized` and `ProgressSubmitter.started`. |
| Payload versioning | Silent best-effort decoding | Explicit `schemaVersion` gate + `Json { ignoreUnknownKeys = true }` | `SnapshotMapper`'s exact precedent; D-07 requires it. |

**Key insight:** this phase's single highest-value "don't hand-roll" is the merge itself — and the
right move is not to write one but to *move* the one that has been in `ProgressRepository` since
v1.0 and has been exercised by every solve the app has ever recorded.

---

## Common Pitfalls

### Pitfall 1: A snapshot left open blocks every later open
**What goes wrong:** an exception between `open` and `commitAndClose` leaks the open snapshot; the
next `open` on the same name fails, so cloud save dies for the rest of the process.
**Why it happens:** `commitAndClose` is on the happy path and the failure path just returns `false`.
**How to avoid:** a `try/finally` around every opened snapshot where the `finally` calls
`discardAndClose` unless a commit already succeeded. Track with a local `var committed = false`.
**Warning signs:** cloud save works once per app launch and silently stops.

### Pitfall 2: `commitAndClose` on an already-closed snapshot fails the Task
**What goes wrong:** the conflict loop's intermediate snapshots, or a double-commit after a retry,
throw. Javadoc: *"fails a Task with an exception when called with a snapshot that was not opened or
has already been committed/discarded."*
**How to avoid:** exactly one terminal call per opened `Snapshot` instance. The loop's intermediate
`resolveConflict` results replace the handle each iteration; only the final one is committed.
**Warning signs:** `IllegalStateException` / `ApiException` on the second solve of a session.

### Pitfall 3: The Activity is gone by the time the pull runs
**What goes wrong:** `PlayGames.getSnapshotsClient(Activity)` needs an Activity. `PlayGamesService`'s
`submitting` helper deliberately reads `activityProvider.current` and gives up **immediately** with
no wait — correct for a fire-and-forget leaderboard submission, wrong for the once-per-process pull.
**How to avoid:** the cloud store uses the *awaiting* variant
(`withTimeoutOrNull(ACTIVITY_WAIT_MS) { awaitActivity() }`) — the same helper
`launchStockUi`/`bootstrapIfOptedIn` use. A timeout is a failed pass: leave the pending marker set,
publish nothing.
**Warning signs:** the pull never runs on a cold start where Home takes a moment to attach.

### Pitfall 4: `commitAndClose` "succeeds" offline
**What goes wrong:** `[CITED: developer.android.com/games/pgs/android/saved-games]` *"If a device
loses network connectivity when calling `commitAndClose()`, Play Games Services caches changes
locally and syncs upon reconnection."* So an offline push can report success and clear the pending
marker before anything reaches the server.
**Why it is fine, and must be documented rather than "fixed":** the data is not lost — GMS owns the
retry. Adding an app-level "did it really upload" check would duplicate a queue Google already runs,
and there is no API to observe it. Note it in the KDoc so a future reader does not "fix" it.
**Warning signs:** none at runtime. A reviewer asking "why don't we verify the upload" is the signal.

### Pitfall 5: `modifyBytes` leaves trailing garbage
**What goes wrong:** when the new payload is shorter than the old one (after a trim, or after the
ladder field shrinks the JSON), `modifyBytes` leaves the excess. The blob becomes
`{...valid json...}xyz` and decoding becomes parser-dependent.
**How to avoid:** `writeBytes` only. Add a ktlint-invisible but review-visible comment naming the
reason, and a unit test on the fake store asserting full replacement.

### Pitfall 6: The once-per-process pull fires twice, or never
**What goes wrong:** `GamesService.session` is a `StateFlow`; `collect` replays the current value to
each collector, and the session can legitimately emit `SignedIn` twice — once from
`bootstrapIfOptedIn` and once from an explicit `signIn()` on the same launch (Phase 11's
`sdkMutex`/`bootstrapped` interplay). Conversely, `ProgressSubmitter.start()` is guarded by
`started: AtomicBoolean` and *only ever runs the pass when `onlyIfPending`*; a player with a clean
dirty flag would never pull at all.
**How to avoid:** a **dedicated** `pulled: AtomicBoolean` consumed inside the pass, and a pull that
is **not** gated on the submissions dirty flag. Restructure `submitAll` so the ordering is:
session gate → profile gate → pull (own guard) → `onlyIfPending` check → Phase-12 submissions →
push. The existing `onlyIfPending` early-return currently sits above everything and would skip the
pull; moving it is the single most likely place to introduce a silent regression.
**Warning signs:** a fresh install signs in and gets nothing back until it happens to solve a level.

### Pitfall 7: The description, its locale, and its length
- **Locale (D-11, by design):** composing it from `strings.xml` via `@ApplicationContext` means the
  description is written in whatever locale the process has at commit time. That is D-11's stated
  behaviour, not a bug. Compose it in `platform/games/save/` from a three-int
  `CloudSaveDescription`, so `data/` never touches a `Context` and the string stays in `strings.xml`.
- **Length:** **no maximum is documented anywhere** — not in the 22.0.0 javadoc for
  `SnapshotMetadataChange.Builder.setDescription`, not on the saved-games guide, not via search.
  `[ASSUMED]` Defensively cap it (a scoped constant, ~100 chars) and truncate rather than risk a
  server-side rejection of the whole commit. The D-11 one-liner is ~45 chars, so the cap will never
  bite in practice — it is a guard, not a feature.
- **Never log it.** It contains progress figures tied to a signed-in player (T-11-04 / ASVS V7).

### Pitfall 8: The profile gate must run *before* the pull, not after
**What goes wrong:** D-15 says *"No push or pull runs until it is answered."* If the pull runs first
and the stored id then mismatches, the new account's data has already been merged into the old
account's local store — unrecoverable, and exactly the data loss the dialog exists to prevent.
**How to avoid:** the profile comparison is step [1] of the pass, before step [2]'s pull, and
returns without doing anything else on a mismatch. Pin it with a test that asserts the fake cloud
store received **zero** calls when the stored id differs.

### Pitfall 9: Do not add kotlinx-serialization to `:domain`
**What goes wrong:** the obvious move is `@Serializable data class CloudProgress` in `:domain`.
`DomainPurityTest` would still pass (it only bans `android..`/`androidx..`), so nothing fails —
which is what makes it a trap.
**Why it is wrong anyway:** `domain/build.gradle.kts` declares only `junit` and `truth`, both
test-only; `:domain` currently has **zero** production dependencies. `ResumeSnapshotDto`'s KDoc
states the rule explicitly: *"`@Serializable` lives here and nowhere else."*
`[VERIFIED: data/resume/ResumeSnapshotDto.kt]`
**How to avoid:** pure model in `:domain`, `@Serializable` DTO + mapper in `data/cloud/`, exactly
mirroring `ResumeSnapshotDto` / `SnapshotMapper`.

### Pitfall 10: `bestTimeMs` identity is `Long.MAX_VALUE`
**What goes wrong:** a default of `0L` on a min-merged field is an absorbing element — one
malformed or defaulted row permanently pins the level's best time at zero across every device.
**How to avoid:** a named constant (`NO_TIME = Long.MAX_VALUE`), explicit non-null in the DTO, and a
decode-time rejection of `bestTimeMs <= 0` (drop the row, keep the rest — fail open per CLOUD-05).
Add a law test: `merge(x, identity) == x` for the identity `CloudProgress`.

### Pitfall 11: The payload's mode token is a wire contract
**What goes wrong:** reusing `ProgressPersistenceKeys.MODE_CLASSIC` / `MODE_CHALLENGE` directly in
the JSON couples the on-the-wire format to the on-disk column value. Renaming a DB constant would
then silently orphan every existing cloud save.
**How to avoid:** a `ProgressMode` enum in `:domain` and an explicit two-way mapping in `data/`,
exactly the way `ClassicBoardIds` translates level ids to `ClassicBoard`. Pin the wire strings in
a codec test.

### Pitfall 12: detekt `MagicNumber` over the new constants
`MagicNumber` is active on `src/main` with only `-1`, `0`, `1`, `2` exempt, and
`ignoreConstantDeclaration: true`. `[VERIFIED: config/detekt/detekt.yml]` So `3 * 1024 * 1024`,
`800 * 1024`, `10` (retry bound), `512` (cover px) and the trim margin must all be `const val`s in
a scoped object — which CLAUDE.md demands anyway. `RESOLUTION_POLICY_MANUAL` is an SDK constant
reference, so it is not a magic number.

### Pitfall 13: R8 keep rules for the snapshot classes
Phase 12 UAT test 9 established that `play-services-games-v2:22.0.0` **ships no `proguard.txt` of its
own**, and that R8 full mode produced no `missing_rules.txt` for the Leaderboards/Achievements call
sites. `[VERIFIED: 11-HUMAN-UAT.md test 9]` The snapshot classes are a *new* reflection surface
(`SnapshotEntity` / `SnapshotMetadataEntity` / `SnapshotContentsEntity` are SafeParcelables, and
`BitmapTeleporter` crosses a Binder). Re-run `:app:assembleRelease` and re-inspect
`missing_rules.txt` once the snapshot calls exist. Expected: still clean, covered transitively by
`play-services-basement`. `[ASSUMED]` — cheap to verify, and it is already a UAT-test-9 style check.

---

## Code Examples

### 1. Pull — open, resolve, read (all inside `platform/games/save/`)

```kotlin
// Source: signatures verified from play-services-games-v2:22.0.0 AAR + bundled javadoc.
override suspend fun load(): ByteArray? {
    val activity = withTimeoutOrNull(GamesConstants.ACTIVITY_WAIT_MS) {
        activityProvider.awaitActivity()
    } ?: return null                                   // CLOUD-05: fail open, silently

    return runCatching {
        val client = PlayGames.getSnapshotsClient(activity)
        val opened = client
            .open(
                CloudSaveConstants.SNAPSHOT_NAME,       // "progress" — 1..100 [A-Za-z0-9-._~]
                /* createIfNotFound = */ true,
                SnapshotsClient.RESOLUTION_POLICY_MANUAL,
            ).await()

        val snapshot = resolveToOpenSnapshot(client, opened, ::mergeBytes) ?: return@runCatching null
        var committed = false
        try {
            val bytes = snapshot.snapshotContents.readFully()
            // Nothing to write on a pure read — discard rather than commit, so the
            // server's lastModified is not bumped by a load (Pitfall 1 / Pitfall 2).
            bytes.takeIf { it.isNotEmpty() }
        } finally {
            if (!committed) runCatching { client.discardAndClose(snapshot).await() }
        }
    }.onFailure { if (it is CancellationException) throw it }   // WR-04
        .getOrNull()
}
```

Note `discardAndClose` on the read path: `commitAndClose` would rewrite the snapshot and move
`lastModifiedTimestamp` for a pull that changed nothing.

### 2. Push — merge-on-conflict, write, commit with metadata (CLOUD-03)

```kotlin
// Source: SnapshotMetadataChange.Builder signatures verified from the 22.0.0 AAR.
override suspend fun save(
    bytes: ByteArray,
    description: CloudSaveDescription,
    playedTimeMs: Long,
    mergeRemote: (ByteArray, ByteArray) -> ByteArray,
): Boolean {
    val activity = withTimeoutOrNull(GamesConstants.ACTIVITY_WAIT_MS) {
        activityProvider.awaitActivity()
    } ?: return false

    return runCatching {
        val client = PlayGames.getSnapshotsClient(activity)
        val opened = client
            .open(CloudSaveConstants.SNAPSHOT_NAME, true, SnapshotsClient.RESOLUTION_POLICY_MANUAL)
            .await()
        val snapshot = resolveToOpenSnapshot(client, opened, mergeRemote) ?: return@runCatching false

        var committed = false
        try {
            // Any bytes that arrived during conflict resolution are already merged into the
            // server copy; fold them into what we are about to write so the commit is a
            // superset of everything seen.
            val existing = snapshot.snapshotContents.readFully()
            val finalBytes = if (existing.isEmpty()) bytes else mergeRemote(existing, bytes)

            snapshot.snapshotContents.writeBytes(finalBytes)   // FULL overwrite — Pitfall 5

            val change = SnapshotMetadataChange.Builder()
                .setDescription(descriptionFormatter.format(description))  // strings.xml, capped
                .setPlayedTimeMillis(playedTimeMs)                          // D-10
                .apply { coverImageSource.coverOrNull()?.let { setCoverImage(it) } }  // D-09
                .build()

            client.commitAndClose(snapshot, change).await()
            committed = true
            true
        } finally {
            if (!committed) runCatching { client.discardAndClose(snapshot).await() }
        }
    }.onFailure { if (it is CancellationException) throw it }
        .getOrDefault(false)
}
```

### 3. Runtime cap confirmation, used to size the trim

```kotlin
// Source: getMaxDataSize()/getMaxCoverImageSize() verified present in 22.0.0; javadoc states
// the returned value is in BYTES and is guaranteed to be at least 3 MB / 800 KB.
private suspend fun effectiveMaxDataSize(client: SnapshotsClient): Int =
    runCatching { client.getMaxDataSize().await() }
        .getOrNull()
        ?.takeIf { it >= CloudSaveConstants.MAX_DATA_SIZE_FLOOR_BYTES }
        ?: CloudSaveConstants.MAX_DATA_SIZE_FLOOR_BYTES

// CloudSaveConstants
const val MAX_DATA_SIZE_FLOOR_BYTES = 3 * 1024 * 1024        // documented guaranteed minimum
const val MAX_COVER_IMAGE_FLOOR_BYTES = 800 * 1024           // documented guaranteed minimum
const val PAYLOAD_SAFETY_MARGIN_BYTES = 256 * 1024           // trim target = cap - margin
const val MAX_CONFLICT_RETRIES = 10                          // Google's sample bound
const val SNAPSHOT_NAME = "progress"
const val COVER_PX = 512
const val MAX_DESCRIPTION_CHARS = 100
```

The `takeIf { it >= floor }` is deliberate: a server answer *below* the documented guarantee would
be a contract violation, and honouring it would let a bad response shrink the payload.

### 4. The trim loop (`data/cloud/CloudProgressCodec`)

```kotlin
/**
 * Encodes [progress], dropping the lowest Challenge levels until it fits [maxBytes].
 * NEVER drops a Classic row and NEVER drops the ladder or playedTime fields (D-05).
 * Bounded: each iteration removes at least one row, and Classic rows are never candidates,
 * so it terminates in at most `challengeRowCount` iterations.
 */
fun encodeWithinCap(progress: CloudProgress, maxBytes: Int): ByteArray {
    var current = progress
    var encoded = encode(current)
    while (encoded.size > maxBytes) {
        val challengeRows = current.bests.count { it.value.mode == ProgressMode.CHALLENGE }
        if (challengeRows == 0) return encoded     // Classic + ladder only: emit as-is, never lose
        // Halve the overshoot rather than dropping one at a time — bounded log(n) encodes.
        val drop = maxOf(1, challengeRows * (encoded.size - maxBytes) / encoded.size + 1)
        current = ProgressMerge.dropLowestChallenge(current, drop)
        encoded = encode(current)
    }
    return encoded
}
```

The `challengeRows == 0` branch is the honest failure: if Classic rows plus the ladder alone
exceeded the cap (impossible — five rows), the code emits rather than losing the player's Classic
progress. Cover it with a test using an absurdly small `maxBytes`.

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Saved Games via `com.google.android.gms.games.snapshot.Snapshots` + a Drive scope | `SnapshotsClient` in `play-services-games-v2`; Drive dependency and Google Sign-In removed | PGS v2 21.0.0 | No Drive scope, no `GoogleSignInClient`, no consent screen entry for Drive. `[CITED: developers.google.com/android/guides/releases]` |
| `GoogleApiClient` + `PendingResult` callbacks | `PlayGames.getSnapshotsClient(activity)` + `Task<T>` + `await()` | PGS v2 | Coroutine-native; the project already bridges with `kotlinx-coroutines-play-services`. |
| v1 popup/gravity APIs around saved games | None exposed in v2 | PGS v2 | Consistent with Phase 11 D-05 / Phase 12 D-16 — the app does not and cannot position SDK surfaces. |

**Deprecated/outdated:**
- `com.google.android.gms:play-services-games` (v1) and the `Snapshots`/`Snapshots.OpenSnapshotResult`
  API — v1 APIs are being removed from the SDK. Any v1 sample found while implementing is wrong.
- `com.google.android.gms.drive.*` — the classes are still *present* inside the 22.0.0 jar (they
  back `SnapshotContents.zza()`), but no Drive artifact, permission or OAuth scope is required.

---

## Runtime State Inventory

> This is not a rename/refactor phase, but the phase *does* introduce persistent state outside the
> repo, so the equivalent audit is worth having.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data (new) | One Play Games snapshot per player, unique name `"progress"`, holding the JSON payload | None at write time; note that once players have snapshots, the payload schema is a **migration problem** — hence D-07's `schemaVersion` |
| Stored data (existing, read) | `level_result` (all rows), `play_session` (`SUM(elapsedMs)` only), `challenge_current_level` DataStore int | Two new read-only DAO queries; no schema change |
| Stored data (existing, written) | `level_result` (bulk merged upsert), `challenge_current_level` (max) | **`resume_snapshot` MUST NOT be written** (CLOUD-04); **`play_session` MUST NOT be written** (D-06) |
| New device-local state | `play_games_last_merged_player_id` (String key in the shared `app_prefs` DataStore, beside the opt-in booleans) and a `play_games_cloud_push_pending` boolean | Both keys declared in `PlayGamesPreferences`, per its existing "every `play_games_*` name in one object" rule |
| Live service config | Play Console: **Saved Games must be enabled** on the PGS project. Off by default. Not in git. | Operator checklist item; blocks every device test — none of the 27 existing IDs help |
| Secrets / env vars | None new. The 27 leaderboard/achievement IDs and `game_services_project_id` already resolve from untracked `local.properties`. | None |
| OS-registered state | None | None — verified: no new manifest component, no ContentProvider, no scheduled work |
| Build artifacts | None new. No schema JSON (`app/schemas/.../2.json` unchanged), no new resource beyond `cloud_save_cover.xml`. | None |

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| JDK 17 | build | ✓ | 17.0.15 | — |
| Gradle wrapper | build | ✓ | 9.5.1 | — |
| `play-services-games-v2` AAR (cached) | compile + this research | ✓ | 22.0.0 | — |
| `adb` | two-emulator UAT (D-16) | ✓ | 1.0.41 (`/opt/homebrew/bin/adb`) | — |
| Android emulator | D-16 | ✓ | `~/Library/Android/sdk/emulator` | — |
| AVD `Pixel_9a` | D-16 device A | ✓ | present | — |
| AVD `Pixel_9` | D-16 device B | ✓ | present | — |
| Play Console: **Saved Games enabled** | every CLOUD-0x device test | **✗ — unverified** | — | **None. Hard blocker for all device UAT.** JVM tests against the fake are unaffected. |
| A **second** Google account on the tester list | D-12..D-15 profile-swap dialog | ✗ | — | Record as a UAT gap in `11-HUMAN-UAT.md`, exactly like Phase 11 test 2 (same root cause: one account, already authorised) |

**Missing dependencies with no fallback:**
- Play Console "Enable Saved Games" toggle — must be step 0 of the operator checklist, executed and
  confirmed *before* any device UAT is attempted. Without it every `SnapshotsClient` call fails, and
  it fails **silently** into the same `false`/`null` the offline path produces, so a developer will
  chase code for hours. Make the checklist item name that symptom.

**Missing dependencies with fallback:**
- Second Google account — D-16 already accepts this as a recorded gap. Cover D-12..D-15 with JVM
  tests over `FakeGamesService.emitSession(SignedIn(playerId = "other"))` instead.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit4 4.13.2 + Truth 1.4.4 + Turbine 1.2.1 + MockK 1.14.2 + kotlinx-coroutines-test 1.11.0; Robolectric 4.16 for Room/Android-runtime JVM tests; ArchUnit 1.3.0 for the boundary gates |
| Config file | none for JUnit4; detekt at `config/detekt/detekt.yml` |
| Modules | `:domain` (pure JVM, `:domain:test`) and `:app` (`:app:testDebugUnitTest`; **no `androidTest` source set**) |
| Quick run command | `./gradlew :domain:test --tests "*ProgressMergeTest*"` / `./gradlew :app:testDebugUnitTest --tests "*CloudProgressCodecTest*"` |
| Full suite command | `./gradlew :domain:test :app:testDebugUnitTest` |
| Phase gate | `./gradlew check` (adds detekt, ktlintCheck, `dpSpLiteralCheck`, `adIdLiteralCheck`, `gamesIdLiteralCheck`, `audioFocusLiteralCheck`, `:domain:depthSweep` ≈ 11 min) plus one `./gradlew :app:assembleRelease` and an inspection of `app/build/outputs/mapping/release/missing_rules.txt` (Pitfall 13) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|--------------|
| CLOUD-02 | `merge` is **commutative**: `merge(a,b) == merge(b,a)` over 1,000 seeded random pairs | unit (`:domain`) | `./gradlew :domain:test --tests "*ProgressMergeTest*"` | ❌ Wave 0 |
| CLOUD-02 | `merge` is **associative**: `merge(merge(a,b),c) == merge(a,merge(b,c))` over 1,000 seeded triples | unit (`:domain`) | same | ❌ Wave 0 |
| CLOUD-02 | `merge` is **idempotent**: `merge(a,a) == a`, and `merge(merge(a,b),b) == merge(a,b)` | unit (`:domain`) | same | ❌ Wave 0 |
| CLOUD-02 | **identity**: `merge(a, EMPTY) == a`; `bestTimeMs` identity is `Long.MAX_VALUE`, never `0` | unit (`:domain`) | same | ❌ Wave 0 |
| CLOUD-02 | `merge` never regresses a field: `merged.stars >= max`, `merged.bestTimeMs <= min`, `merged.bestScore >= max`, `merged.ladder >= max` | unit (`:domain`) | same | ❌ Wave 0 |
| CLOUD-02 | Local `recordResult` behaviour is **byte-identical** after the extraction | unit (Robolectric) | `./gradlew :app:testDebugUnitTest --tests "*ProgressRepositoryTest*"` | ✅ exists |
| CLOUD-01 | Round trip: `encode` → `decode` yields an equal `CloudProgress` | unit (`:app`) | `./gradlew :app:testDebugUnitTest --tests "*CloudProgressCodecTest*"` | ❌ Wave 0 |
| CLOUD-01 | `schemaVersion` above the build's own → decode returns `null` (fail open, D-07/D-11) | unit (`:app`) | same | ❌ Wave 0 |
| CLOUD-01 | Unknown fields in a newer payload are ignored, known fields survive (D-07) | unit (`:app`) | same | ❌ Wave 0 |
| CLOUD-01 | Wire mode tokens are pinned (`"classic"` / `"challenge"`), decoupled from DB constants | unit (`:app`) | same | ❌ Wave 0 |
| CLOUD-01 | Snapshot name matches `^[A-Za-z0-9\-._~]{1,100}$` | unit (`:app`) | `--tests "*CloudSaveConstantsTest*"` | ❌ Wave 0 |
| CLOUD-01 | Import of a remote payload lands every best and the ladder; local Flows re-emit | unit (Robolectric + Turbine) | `./gradlew :app:testDebugUnitTest --tests "*ProgressImportTest*"` | ❌ Wave 0 |
| CLOUD-02 | Import never regresses a better local best (stale cloud row) | unit (Robolectric) | same | ❌ Wave 0 |
| CLOUD-02 | Import is idempotent — running it twice changes nothing on the second pass | unit (Robolectric) | same | ❌ Wave 0 |
| CLOUD-03 | `save` builds a `SnapshotMetadataChange` with a non-blank description, a `playedTimeMillis` and a cover; description is capped and locale-sourced | unit (`:app`, fake store + formatter test) | `--tests "*CloudSaveDescriptionFormatterTest*"` | ❌ Wave 0 |
| CLOUD-03 | `SUM(elapsedMs)` over `play_session` is the played time; empty table → 0, not null | unit (Robolectric) | `--tests "*PlaySessionDaoTest*"` or `*ProgressAggregateTest*` | ❌ Wave 0 |
| CLOUD-04 | Import leaves both `resume_snapshot` rows byte-identical | unit (Robolectric, **negative**) | `--tests "*ProgressImportTest*"` | ❌ Wave 0 |
| CLOUD-04 | Import inserts **zero** `play_session` rows (D-06) | unit (Robolectric, **negative**) | same | ❌ Wave 0 |
| CLOUD-04 | The payload DTO has no board/move/resume field — asserted structurally over the serial descriptor | unit (`:app`) | `--tests "*CloudProgressCodecTest*"` | ❌ Wave 0 |
| CLOUD-05 | `load()` returning `null` leaves local progress untouched and sets nothing pending beyond the marker | unit (`:app`, fake store) | `./gradlew :app:testDebugUnitTest --tests "*ProgressSubmitterCloudTest*"` | ❌ Wave 0 |
| CLOUD-05 | `save()` returning `false` sets the cloud-push-pending marker; the next pass retries | unit (`:app`) | same | ❌ Wave 0 |
| CLOUD-05 | A throwing fake store is indistinguishable from a `false` return (never propagates) | unit (`:app`) | same | ❌ Wave 0 |
| CLOUD-05 | A `CancellationException` from the store is **rethrown**, not classified as a failure (WR-04) | unit (`:app`) | same | ❌ Wave 0 |
| D-05 | Trim drops lowest Challenge rows only; Classic rows and the ladder always survive | unit (`:domain` + `:app`) | `--tests "*ProgressMergeTest*" --tests "*CloudProgressCodecTest*"` | ❌ Wave 0 |
| D-05 | `encodeWithinCap` terminates and fits, for a 10,000-row payload at a 64 KB artificial cap | unit (`:app`) | `--tests "*CloudProgressCodecTest*"` | ❌ Wave 0 |
| D-02/Pitfall 6 | The pull runs **exactly once** per process even across two `SignedIn` emissions, and runs even when the submissions dirty flag is clear | unit (`:app`) | `--tests "*ProgressSubmitterCloudTest*"` | ❌ Wave 0 |
| D-12/Pitfall 8 | A mismatched stored player id raises the Home flag and performs **zero** store calls | unit (`:app`) | same | ❌ Wave 0 |
| D-13/D-14 | "Keep" merges; "Replace" wipes `level_result`, `play_session`, ladder and **both** resume snapshots, then imports; dismissal == "Keep" | unit (Robolectric + `HomeViewModel`) | `--tests "*HomeViewModelCloudTest*" --tests "*ProfileSwapTest*"` | ❌ Wave 0 |
| D-15 | The profile-swap dialog is Home-only, never stacked with onboarding or the opt-in prompt | unit (Compose UI test) | `--tests "*HomeScreenTest*"` | ✅ file exists, cases ❌ |
| PLAT-04 | No gms type escapes `..platform..`; `:domain` stays Android-free | arch | `./gradlew :app:testDebugUnitTest --tests "*PlatformBoundaryTest*" --tests "*PlatformBoundaryGateTest*" --tests "*DomainPurityTest*"` | ✅ exists |
| T-11-04 | No logging call names a player id, snapshot id, description or SDK message | grep gate (plan task acceptance) | `grep -rn "Log\.\|println\|Timber" app/src/main/java/com/app/pegsolitaire/platform/games/ app/src/main/java/com/app/pegsolitaire/data/cloud/` → 0 hits | n/a |
| Pitfall 13 | Minified release needs no new keep rules for the snapshot classes | build | `./gradlew :app:assembleRelease` then inspect `app/build/outputs/mapping/release/missing_rules.txt` for `com.google.android.gms.games` | n/a |

**Algebraic-law test shape (no property library — none is on the graph):**

```kotlin
// domain/src/test/kotlin/.../progress/ProgressMergeTest.kt
private fun randomProgress(rnd: Random): CloudProgress = CloudProgress(
    ladder = rnd.nextInt(1, 10_000),
    playedTimeMs = rnd.nextLong(0, 1_000_000_000),
    bests = (0 until rnd.nextInt(0, 40)).associate { ... },
)

@Test fun mergeIsCommutative() {
    val rnd = Random(SEED)                      // fixed seed: reproducible, not flaky
    repeat(LAW_ITERATIONS) {
        val a = randomProgress(rnd); val b = randomProgress(rnd)
        assertThat(ProgressMerge.merge(a, b)).isEqualTo(ProgressMerge.merge(b, a))
    }
}
```

`SEED` and `LAW_ITERATIONS` are `private const val`s in the test file (detekt's `MagicNumber`
excludes `**/test/**`, so this is stylistic, but the project's convention is constants anyway).

**Fake cloud store:**

```kotlin
// app/src/test/java/.../platform/games/save/FakeCloudSaveStore.kt
class FakeCloudSaveStore(
    var remote: ByteArray? = null,
    var saveResult: Boolean = true,
    var throwOnSave: Throwable? = null,
) : CloudSaveStore {
    var loadCallCount = 0; private set
    var savedPayloads: List<ByteArray> = emptyList(); private set
    var lastDescription: CloudSaveDescription? = null; private set
    var lastPlayedTimeMs: Long? = null; private set

    override suspend fun load(): ByteArray? { loadCallCount++; return remote }

    override suspend fun save(
        bytes: ByteArray, description: CloudSaveDescription,
        playedTimeMs: Long, mergeRemote: (ByteArray, ByteArray) -> ByteArray,
    ): Boolean {
        throwOnSave?.let { throw it }
        val merged = remote?.let { mergeRemote(it, bytes) } ?: bytes   // exercises the lambda
        savedPayloads = savedPayloads + merged
        lastDescription = description; lastPlayedTimeMs = playedTimeMs
        if (saveResult) remote = merged
        return saveResult
    }
}
```

Modelled on the shipped `FakeGamesService` (record-and-assert, no MockK) so
`ProgressSubmitterTest`'s existing style carries straight over. The `mergeRemote` invocation is
deliberate — it is the only way a JVM test can exercise the conflict merge without the SDK.

### Sampling Rate

- **Per task commit:** the narrowest `--tests` filter for the class touched.
- **Per wave merge:** `./gradlew :domain:test :app:testDebugUnitTest`.
- **Phase gate:** `./gradlew check` green (accept the ~11-minute `depthSweep`) **plus**
  `./gradlew :app:assembleRelease` with no `com.google.android.gms.games` entries in
  `missing_rules.txt`, **plus** the two-emulator UAT below.

### Wave 0 Gaps

- [ ] `domain/src/test/kotlin/.../progress/ProgressMergeTest.kt` — CLOUD-02 laws + trim policy
- [ ] `app/src/test/.../data/cloud/CloudProgressCodecTest.kt` — CLOUD-01/D-05/D-07 round trip, cap, wire tokens
- [ ] `app/src/test/.../data/ProgressImportTest.kt` — CLOUD-01/02/04 import behaviour (Robolectric + in-memory Room)
- [ ] `app/src/test/.../data/ProgressSubmitterCloudTest.kt` — CLOUD-05 + D-01/D-02/D-12 orchestration
- [ ] `app/src/test/.../platform/games/save/FakeCloudSaveStore.kt` — shared fake
- [ ] `app/src/test/.../platform/games/save/CloudSaveDescriptionFormatterTest.kt` — CLOUD-03
- [ ] `app/src/test/.../ui/home/HomeViewModelCloudTest.kt` — D-12..D-15 dialog state
- [ ] New cases in the existing `HomeScreenTest.kt` (dialog never stacks) and `MigrationTest` is **not** touched (no schema change)
- No framework install needed — the full stack is already wired.

### Manual UAT (append to `11-HUMAN-UAT.md` as tests 10–13)

| # | Test | Requirement | Steps | Expected |
|---|------|-------------|-------|----------|
| 10 | Restore on a new device | CLOUD-01 | Prerequisite: Saved Games enabled in Play Console. `~/Library/Android/sdk/emulator/emulator -avd Pixel_9a`, solve English-33 + Challenge levels 1–3, force-stop. Wipe/fresh-install on `Pixel_9`, sign in as the same tester. | Within seconds of the "Welcome back" popup, the Classic catalog shows English-33's stars and the Challenge tile reads the same level — with **no toast, no dialog, no Settings row** (D-03). |
| 11 | Two-device union | CLOUD-02 | On `Pixel_9a` solve Challenge 1–5 and English-33 at 3 stars. On `Pixel_9` (same account) solve European-37 and Challenge 6–8. Relaunch **both**. | Both devices show Challenge tile = 9, English-33 3 stars, European-37 cleared. No prompt, nothing lost, on either device. |
| 12 | Legible listing entry | CLOUD-03 | Open the Play Games app → this game → Saved games. | One entry named/described "Challenge level N · S stars · K/5 Classic boards", a played-time value, and the board cover art. |
| 13 | Network kill degrades silently | CLOUD-05 | On `Pixel_9`: `adb shell svc wifi disable && adb shell svc data disable` (or `adb emu gsm data off`). Solve two levels, relaunch, solve another, then re-enable the network and relaunch. | Every solve records locally, Results and the catalog behave exactly as offline v1.0 did, **no error wall and no snackbar**. After reconnect + relaunch, the two devices reconverge to the union. |
| — | Gap | D-12..D-15 | Profile-swap dialog | **Recorded as a gap**, same root cause and wording as Phase 11 test 2: needs a second Google account that has never authorised the game. |

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|------------------|
| V2 Authentication | yes (inherited) | Play Games v2 handles auth entirely; the app never sees a token. Phase 11's opt-in gate remains the outermost condition — a non-opted-in player's SDK is never touched, so cloud save never runs for them. |
| V3 Session Management | yes (inherited) | `GamesSignInState` is the only session concept; no app-managed session, no sign-out API in v2. |
| V4 Access Control | yes | The snapshot is scoped by Play Games to the signed-in player. **The one app-side control is D-12's stored-player-id check** — without it, device A's progress merges into player B's account on an account switch. Pin it with the Pitfall-8 negative test. |
| V5 Input Validation | **yes — critical** | The cloud payload is **untrusted input from the network**. Apply `ResumeSnapshotDto`'s exact discipline: `Json { ignoreUnknownKeys = true }`, `schemaVersion` gate, and per-row validation (`stars ∈ 0..3`, `bestTimeMs > 0`, `bestScore >= 0`, `levelId` non-blank, Challenge `levelId` parses to an int within `ChallengeLevelBounds`, `ladder` within bounds). An invalid row is **dropped**, never fatal — the WARN-02 precedent (`06-02-PLAN`: bound coordinates at the decode boundary). |
| V6 Cryptography | no | No app-managed crypto. Transport is Google's. Do not invent a checksum or signature — the threat model is not a malicious server. |
| V7 Error handling & Logging | **yes** | T-11-04 extends verbatim: no player id, snapshot id, description, payload byte or SDK exception message reaches logcat. There is no logging call anywhere in `app/src/main` today; keep it that way. |
| V8 Data Protection | yes | CLOUD-04 is a data-protection control as much as a UX one: the in-progress board (and, with it, the resume blob's move history) never leaves the device. |

### Known Threat Patterns for Play Games Saved Games

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Account switch merges A's progress into B's cloud save | Tampering / Elevation | D-12's stored-player-id gate **before** any pull or push (Pitfall 8) |
| Malformed/hostile payload crashes the app on decode | Denial of Service | `runCatching` decode + per-field validation + drop-the-row; fail open to local (CLOUD-05) |
| Absurd values (negative time, stars = 999) poison the monotonic merge permanently | Tampering | Clamp/reject at the decode boundary — `stars.coerceIn(0, 3)`, reject `bestTimeMs <= 0` (Pitfall 10) |
| Oversized payload rejected by the server, push silently never succeeds | DoS | Trim against `getMaxDataSize()` minus margin, with a tested bound (D-05) |
| Progress figures / player id leaked to logcat | Information Disclosure | T-11-04 grep gate over `platform/games/` **and** the new `data/cloud/` |
| Unbounded conflict loop spins on the SDK | DoS | `MAX_CONFLICT_RETRIES = 10`, then a clean `false` and a retained pending marker |
| Leaked open snapshot disables cloud save for the process | DoS | `try/finally` with `discardAndClose` (Pitfall 1) |

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | **FALSIFIED BY MEASUREMENT (UAT test 12, 2026-09-20).** ~~`setCoverImage` accounts a 512×512 ARGB_8888 bitmap against the 800 KB cap by its *PNG-compressed* transported size, not its 1 MB in-memory size~~ — GMS accounts the **raw** ARGB_8888 size. Measured log line, every commit, both devices: `W/PlayGamesServices[GamesService]: Snapshot cover image is too large. Currently at 1048576 bytes; max is 819200. Image will be scaled`. Cap = 819,200 bytes; the 512×512 cover was accounted at 1,048,576. | Pattern 5 | GMS scaled the cover and the commit succeeded, so no save was lost. Remedy applied: `COVER_PX` 512 → 384 (384² × 4 = 589,824 ≤ 819,200), pinned by `CoverImageSourceTest.theRawArgbCoverFitsThePlayGamesCoverCeiling`. Source: `.planning/reports/phase-13/qa-engineer-device-pass-rerun.md` (test 12). |
| A2 | No documented maximum length exists for `setDescription` | Pitfall 7 | A long description could be rejected server-side and fail the whole commit. Mitigated unconditionally by capping at 100 chars, which the D-11 one-liner never approaches. |
| A3 | `Resources.getDrawable` / vector inflation is safe on `Dispatchers.Default` | Pattern 5 | A rare inflation failure → `coverOrNull()` returns null → cover omitted. Already the designed degradation. |
| A4 | No Play Games guidance exists on cover image aspect ratio; square is fine | Pattern 5 | Cosmetic only — the listing entry could be letterboxed in the Play Games saves UI. |
| A5 | The 22.0.0 snapshot classes need no new R8 keep rules, by transitive coverage from `play-services-basement` | Pitfall 13 | A minified release could crash on a snapshot call. Cheap to falsify: one `assembleRelease` + `missing_rules.txt` inspection, already the Phase-12 UAT-test-9 procedure. |
| A6 | The Play Console PGS project's Saved Games toggle is currently **off** | Environment Availability | If it is already on, the operator step is a no-op confirmation. If off and missed, 100% of device UAT fails silently. |
| A7 | `ProgressSubmitter`'s `onlyIfPending` early return can be reordered below the pull without regressing PGSL-05's "clean launches make no calls" | Pitfall 6 | A clean launch would make one *cloud* call it previously did not. That is intended (D-02's once-per-process pull), but it must be an explicit, reviewed change to a documented invariant, not a side effect. |

---

## Open Questions (RESOLVED — each recommendation below was adopted by the plans: Q1 → 13-02, Q2 → 13-08, Q3 → 13-05, Q4 → 13-05/13-08, Q5 → 13-09)

1. **Does the cloud-push-pending marker share `SubmissionSyncStore` or get a sibling?**
   - What we know: `SubmissionSyncStore`'s KDoc argues at length for *not* folding the submissions
     flag into `PlayGamesOptInRepository`, on the grounds that the opt-in pair has an atomicity
     relationship the third boolean does not share. That argument applies with equal force to a
     fourth boolean.
   - Recommendation: **a sibling `CloudSyncStore`** with its own key
     (`play_games_cloud_push_pending`) declared in `PlayGamesPreferences` alongside the others, and
     the same `open class` / `open val` shape so `ProgressSubmitterTest` can subclass it. Keeping
     the *keys* together and the *accessors* apart is exactly the existing pattern.

2. **Does a successful import re-fire the Phase-12 submission pass?**
   - CONTEXT recommends yes. The implementation is one `syncStore.setPending(true)` after an import
     that changed anything — but note the ordering: set it **before** the Phase-12 submission step
     in the same pass, so the catch-up happens immediately rather than on the next solve. Otherwise
     a restored player's leaderboards lag until they solve something.

3. **How is "the import changed something" detected, given `upsertAll` reports nothing?**
   - Recommendation: compare the merged map to the local map inside the transaction (`merged !=
     localMap`) and return a small `ImportOutcome(changed: Boolean, rowsWritten: Int)`. It is
     **not** `NewBests` and is never routed to `ResultsUiState` — keep the types distinct so the
     structural guarantee in Pattern 3 stays structural.

4. **Should the pull await the ladder write before the push?**
   - Yes. That is the whole reason to add `ChallengeProgress.mergeLadder` as a `suspend` function
     rather than reusing the fire-and-forget `advancePast`. Without it the push in the same pass can
     read a pre-merge ladder and write a *lower* value than it just imported — harmless (the next
     pull maxes it back) but it makes the "one pass converges" property untrue and untestable.

5. **Which `SnapshotContents` does the 4-arg `resolveConflict` want?**
   - `conflict.getResolutionSnapshotContents()` — the javadoc calls it *"the `SnapshotContents`
     object used to update the data in case of a conflict"*. Do **not** reuse
     `conflict.getSnapshot().getSnapshotContents()`; that is a read handle on the server copy.

---

## Sources

### Primary (HIGH confidence)
- `~/.gradle/caches/modules-2/files-2.1/com.google.android.gms/play-services-games-v2/22.0.0/**/play-services-games-v2-22.0.0.aar` — `javap -p -constants` over `classes.jar` for `SnapshotsClient`, `SnapshotsClient$DataOrConflict`, `SnapshotsClient$SnapshotConflict`, `Snapshot`, `SnapshotContents`, `SnapshotMetadata`, `SnapshotMetadataChange`, `SnapshotMetadataChange$Builder`, `PlayGames`; and the AAR's own `AndroidManifest.xml`. **The exact artifact this build resolves.**
- `…/play-services-games-v2-22.0.0-javadoc.jar` → `com/google/android/gms/games/SnapshotsClient.html` and the `snapshot/*.html` pages — the 3 MB / 800 KB guarantees, the `RESOLUTION_POLICY_*` semantics, the filename charset/length rule, `commitAndClose`/`resolveConflict` failure conditions, `writeBytes` vs `modifyBytes`.
- `~/.gradle/caches/.../androidx.core/core-ktx/1.18.0/core-ktx-1.18.0.aar` — `javap` confirming `DrawableKt.toBitmap(Drawable, int, int, Bitmap$Config)`.
- https://developer.android.com/games/pgs/android/saved-games — Play Console "Enable saved games" prerequisite, the `MAX_SNAPSHOT_RESOLVE_RETRIES = 10` retry pattern, offline `commitAndClose` caching behaviour.
- https://developer.android.com/games/pgs/savedgames — "each saved game file has a maximum size of 3 MB"; "3 MB and 800 KB respectively"; "A game can write any number of Saved Games for a single player".
- Codebase, read in full this session: `data/ProgressRepository.kt`, `data/ProgressSubmitter.kt`, `data/SubmissionSyncStore.kt`, `data/ChallengeProgress.kt`, `data/PlayGamesOptInRepository.kt`, `data/PlayGamesPreferences.kt`, `data/ProgressPersistenceKeys.kt`, `data/ClassicBoardIds.kt`, `data/db/{LevelResultDao,LevelResultEntity,PlaySessionDao,ResumeDao,PegSolitaireDatabase}.kt`, `data/resume/{ResumeSnapshotDto,SnapshotMapper}.kt`, `platform/games/{GamesService,PlayGamesService,NoOpGamesService,GamesConstants}.kt`, `di/PlatformModule.kt`, `ui/home/{HomeUiState,HomeViewModel,PlayGamesPromptDialog}.kt`, `ui/results/ResultsViewModel.kt`, `app/src/test/.../{PlatformBoundaryRules,DomainPurityTest,ProgressSubmitterTest,ProgressRepositoryTest,MigrationTest}.kt`, `app/src/test/.../platform/games/FakeGamesService.kt`, `domain/build.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `build-logic/src/main/kotlin/standards.gradle.kts`, `config/detekt/detekt.yml`, `app/src/main/AndroidManifest.xml`, `app/src/main/res/drawable/ic_launcher_*.xml`, `app/src/main/res/mipmap-anydpi*/ic_launcher.xml`.
- Local environment probes: `adb version`, `emulator -list-avds`, `java -version`, `gradle-wrapper.properties`.

### Secondary (MEDIUM confidence)
- `.planning/research/{ARCHITECTURE,STACK,FEATURES}.md` (v1.1, 2026-08-27) — Pattern 4, Anti-Patterns 2/3, the `CloudSaveDataSource`/`CloudProgressSync`/`ProgressMerge` component table. Its flagged unknowns (size cap, snapshot count) are now closed by the primary sources above.
- `.planning/phases/11-play-games-session/11-HUMAN-UAT.md` — test 9's empirical finding that 22.0.0 ships no `proguard.txt` and needed no new keep rules for the Leaderboards/Achievements surface.

### Tertiary (LOW confidence — flagged, not relied on)
- WebSearch for a `setDescription` length limit and a cover-image aspect-ratio recommendation: **no authoritative result exists.** Both are recorded in the Assumptions Log (A2, A4) rather than stated as fact.

---

## Metadata

**Confidence breakdown:**
- SDK surface and size caps: **HIGH** — read from the resolved artifact's own bytecode and javadoc, not from a web page.
- Codebase integration shape: **HIGH** — every named file, signature and line reference was read this session.
- Merge algebra and test strategy: **HIGH** — the merge already exists in production code and has been exercised by every solve since v1.0.
- Cover-image sizing and description length: **MEDIUM** — mechanism verified, two numeric limits undocumented (A1, A2), both with cheap in-UAT falsification and safe degradation.
- R8 keep-rule cleanliness for the snapshot classes: **MEDIUM** — strong prior from Phase 12 UAT test 9, but the snapshot reflection surface is genuinely new (A5).

**Research date:** 2026-09-15
**Valid until:** 2026-10-15 (30 days — `play-services-games-v2` is a slow-moving, additively-versioned artifact and 22.0.0 is pinned)
