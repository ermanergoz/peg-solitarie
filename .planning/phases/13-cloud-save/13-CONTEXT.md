# Phase 13: Cloud Save - Context

**Gathered:** 2026-09-15
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase makes the player's **per-level bests and Challenge ladder position follow their Play Games account** through Play Games Services v2 Saved Games (CLOUD-01). Progress from two devices always merges to the **union** — highest ladder, best stars, best scores, fastest times — by a deterministic monotonic merge with **no prompt and no loss** (CLOUD-02). The cloud save carries a **description, played time, and cover image** so it is legible in the Play Games saves UI (CLOUD-03). The **in-progress board snapshot never leaves the device** (CLOUD-04), and any cloud save or load failure **degrades to local-only play** with no data loss and no error wall (CLOUD-05).

Cloud save is a **second data source behind `ProgressRepository`**, not a method the UI sees. The merge is extracted into `:domain` and exhaustively unit-tested before any `SnapshotsClient` code is written. Conflicts use `RESOLUTION_POLICY_MANUAL` plus that merge — never the default most-recently-modified policy, which silently deletes progress.

**Locked before this discussion (not re-litigated):** scope is bests + ladder only (v1.1 requirements); monotonic merge with no dialog for same-account conflicts (v1.1 requirements + ROADMAP note); nothing Play-Games-related runs unless the player opted in, with the `session` flow as the gate (Phase 11 D-01/D-17); the Saved Games per-snapshot cap must be confirmed at planning time and the payload designed capped-by-default (ROADMAP research gap).

</domain>

<decisions>
## Implementation Decisions

### Sync timing and restore feedback
- **D-01:** **Push after every solved level.** The cloud write piggybacks on the existing `ProgressSubmitter` pass that runs after `recordResult` returns (Phase 12 D-13's single orchestration point). No background-flush trigger. A failed push sets a persisted "cloud push pending" marker and is retried on the next pass, in the shape of Phase 12 D-10's dirty flag (exact home is discretion, see below).
- **D-02:** **Pull once per process, when `session` resolves to `SignedIn`** — the silent launch check or the first explicit sign-in (Phase 11 D-02 already limits sign-in to once per process). No re-pull on foreground return, no connectivity listener. A player opening the app on a new device gets their progress within seconds of the stock "Welcome back" popup.
- **D-03:** **Restore is fully silent.** No toast, no dialog, no Settings status row. Catalog star pips and the Challenge tile update live through the existing Room/DataStore flows; the stock "Welcome back" popup remains the only launch-time signal (parity with Phase 11 D-05). A cloud import must never fire the "New best!" flag or the Results celebration — those are per-play, not per-merge.
- **D-04:** **A stale in-progress game is kept; the ladder jumps underneath it.** If this device has a Challenge game in progress at level 10 and the merged ladder is 50, the Continue tile still resumes the level-10 game. Solving it calls `advancePast(10) = max(50, 11) = 50`, so the ladder stays at 50 and "Next level" serves level 50. Nothing on the resume path is touched by a cloud event (CLOUD-04/05).

### Payload shape and size cap
- **D-05:** **Lossless by default; trim only at the cap.** The payload carries every Classic and Challenge best (`mode`, `levelId`, `stars`, `bestTimeMs`, `bestScore`) plus the ladder. At roughly 40 bytes per level in JSON, 10,000 Challenge levels is ~400 KB against a 3 MB cap. If a serialized payload ever approaches the **confirmed** cap (planner verifies the limit against current docs/SDK — research says 3 MB at HIGH confidence, ROADMAP flags it unverified), **drop the lowest Challenge levels first** until it fits. The trim is a tested safety valve that is expected never to fire in practice; it must never drop Classic rows or the ladder field.
- **D-06:** **`play_session` telemetry stays local.** Achievements already live on Play Games' servers, so a new device needs no history to keep them. The payload stays a pure join-semilattice of bests. (Consequence: on a fresh device the Phase 12 `play_session` existence flags read false; that is correct and does not un-earn anything.)
- **D-07:** **Encoding is kotlinx-serialization JSON with a top-level `schemaVersion` field**, the same library and shape as the existing resume-snapshot blob. Unknown fields are ignored on read so an older build can read a newer payload's known fields; a payload with a `schemaVersion` above what the build understands is treated as a failed load (fail open to local, D-11).
- **D-08:** **The ladder field is the persisted `ChallengeProgress.currentLevel`** (the next level to play), carried as its own field and **merged by max** on both sides. It is exactly what the Challenge tile shows, so a new device lands on the same "Level N" tile. Per-level bests merge independently. No UI calls `ChallengeProgress.reset()`, so the ladder is purely monotonic today; the merge relies on that.

### Play Games listing metadata (CLOUD-03)
- **D-09:** **Cover image is bundled board art rasterized from the launcher icon vectors** (`ic_launcher_foreground` / `ic_launcher_background`, the approved English-33 design from quick task 260914-x4a) into a PNG well under the 800 KB cover limit. Static and deterministic; attached on every commit. No runtime board rendering.
- **D-10:** **Played time = `SUM(elapsedMs)` over `play_session`** (solved plays only), carried as a `playedTimeMs` field in the payload and **merged by max** so the metadata reflects the larger of the two devices' totals rather than double-counting. The snapshot's `playedTimeMillis` metadata is set from the merged value.
- **D-11:** **Description is one line: "Challenge level N · S stars · K/5 Classic boards"**, built from the Phase 12 SQL aggregates (`highestChallengeLevel`/ladder, `totalStars`, `clearedClassicBoards.size`). It goes through `strings.xml` and therefore **follows the device locale at commit time**; a player who changes language sees the new language after their next solve. Exact wording and number formatting are discretion.

### Account switch (the one dialog) and verification
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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements and roadmap
- `.planning/ROADMAP.md` §"Phase 13: Cloud Save" — goal, five success criteria, and the note mandating `RESOLUTION_POLICY_MANUAL`, the `:domain` merge, and the unverified size cap.
- `.planning/REQUIREMENTS.md` §"Milestone Decisions" (Cloud save scope, Conflict resolution rows) and §"Cloud Save" (CLOUD-01..05) — the locked scope and the reserved single-dialog case.
- `.planning/PROJECT.md` — Constraints (Clean Architecture, seams, no magic values, dimens, detekt/ktlint gate, Gitflow) and Key Decisions.

### Research (v1.1, 2026-08-27)
- `.planning/research/ARCHITECTURE.md` §"Pattern 4: Cloud save as a merge, not a copy", §"Anti-Pattern 2/3", the `CloudSaveDataSource` / `CloudProgressSync` / `ProgressMerge` component table, and the scale note on lossy payloads — the integration shape this phase follows (with D-05 replacing the lossy-window sketch).
- `.planning/research/STACK.md` — `play-services-games-v2` 22.0.0 (no Drive scope in v2), `kotlinx-coroutines-play-services` 1.11.0, the 3 MB snapshot cap claim, and the Play Console "Enable Saved Games" step.
- `.planning/research/FEATURES.md` — cloud save feature row (metadata requirements: description + played time + cover image ≤ 800 KB), and the reasoning for keeping the resume snapshot local.

### Prior phase decisions this phase builds on
- `.planning/phases/11-play-games-session/11-CONTEXT.md` — D-01/D-02 (opt-in gate, once-per-process session), D-15..D-19 (the Home opt-in prompt the profile-swap dialog is modeled on), D-11..D-14 (Unknown/offline states).
- `.planning/phases/12-leaderboards-achievements/12-CONTEXT.md` — D-06 (`play_session`), D-07 (cumulative score = SUM of bests), D-10/D-13 (dirty-flag backfill and the `ProgressSubmitter` orchestration point the cloud push joins).
- `.planning/phases/11-play-games-session/11-HUMAN-UAT.md` — the single accumulating device checklist (Phase 12 D-04 rule); Phase 13 tests append here, and the profile-swap gap is recorded beside test 2.

### Design spec
- `docs/superpowers/specs/2026-06-28-peg-solitaire-design.md` §8.4 / §10 — the `GamesService` seam intent and the persistence data model.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt` — `DefaultProgressRepository.recordResultInTransaction` already holds the per-field monotonic merge (max stars / min time / max score, lines ~241–243) inlined; this is what gets extracted to `:domain` as the one merge. `aggregate()` and `classicBests()` supply the description numbers (D-11). `observeResults` / `observeResumeModes` are the flows that make the silent restore (D-03) visible in the UI for free.
- `app/src/main/java/com/app/pegsolitaire/data/ProgressSubmitter.kt` — the `@Singleton` with the one session collector and the serialized `submitAll` pass; the cloud push (D-01) and pull (D-02) join this object's pass and collector rather than adding a second session observer.
- `app/src/main/java/com/app/pegsolitaire/data/SubmissionSyncStore.kt` — the persisted "submissions pending" flag (Phase 12 D-10) and the model for the cloud-push-pending marker.
- `app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt` — `currentLevel` StateFlow and `advancePast` (already `max`-merged inside the `edit` transaction); the ladder field of D-08 reads and writes this key.
- `app/src/main/java/com/app/pegsolitaire/data/PlayGamesOptInRepository.kt` / `PlayGamesPreferences.kt` — DataStore keys for opt-in; the natural home for the "last merged player id" key (D-12).
- `app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt` — owns `ForegroundActivityProvider` access, `Task.await()` bridging, the `submitting` helper (session-gated, Activity-bounded, failure-as-`false`) and the no-logging rule; `SnapshotsClient` calls follow the same shape.
- `app/src/main/java/com/app/pegsolitaire/platform/games/GamesService.kt` + `NoOpGamesService.kt` — the seam and stub; `GamesSignInState.SignedIn.playerId` is the id compared in D-12.
- `app/src/main/java/com/app/pegsolitaire/data/db/LevelResultEntity.kt` / `LevelResultDao.kt` (`upsert`, `totals`, `classicStandings`) and `PlaySessionDao.kt` — the payload's source rows and the `SUM(elapsedMs)` for D-10.
- `app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt` + `SnapshotMapper.kt` — the kotlinx-serialization JSON blob precedent (nullable-with-default fields) for the payload DTO (D-07).
- `app/src/main/java/com/app/pegsolitaire/ui/home/PlayGamesPromptDialog.kt`, `HomeUiState.kt`, `HomeViewModel.kt` — the Home-only, state-hoisted dialog pattern for D-15.
- `app/src/main/res/drawable/ic_launcher_foreground.xml` / `ic_launcher_background.xml` — the vector art rasterized for the cover image (D-09).
- `app/src/main/java/com/app/pegsolitaire/platform/games/GamesConstants.kt` — timing tokens (`ACTIVITY_WAIT_MS`) to reuse for snapshot call bounds.

### Established Patterns
- **Seam + `@Binds` in `di/PlatformModule.kt`; SDK types never leave `..platform..`** — enforced by `PlatformBoundaryGateTest` / `PlatformBoundaryTest`; `:domain` purity by `DomainPurityTest`. The merge must pass both.
- **Best-effort storage with `storageFailSafe`** — every read fails open, every write is best-effort; cloud load/save inherit this (CLOUD-05).
- **Failure is a return value, not an exception** on the games seam (PGSL-04); cloud calls report `false` and set the pending marker.
- **One session collector per process** in `ProgressSubmitter`, gated on `session` then the flag inside one lock (WR-01 race note) — the pull and the profile-id check run inside that same critical section.
- **Room bump ritual** (`DB_VERSION`, `Migration`, `DatabaseModule`, exported schema JSON, `MigrationTest`) — this phase should need **no schema change**; if the planner finds it does, the ritual applies.
- **Scoped constants objects** (`GamesConstants`, `ProgressConstants`, `ChallengeProgressPreferences`) — snapshot name, cap margin and cover size get their own scoped constants.
- **All strings through `strings.xml`, all dimensions through `dimens.xml`**; no logging of player-identifying data (T-11-04 grep gate).

### Integration Points
- `:domain` — new pure merge (`ProgressMerge`) + the payload model if it is kept SDK- and Room-free; exhaustive JVM tests (commutative / associative / idempotent, trim keeps Classic + ladder).
- `data/ProgressRepository` — extract the inlined merge to call the domain function; add export (rows → payload) and import (payload → merged rows + ladder + playedTime) operations.
- `data/ProgressSubmitter` — cloud push after `recordResult`, cloud pull on `SignedIn`, pending marker, profile-id gate (D-12/D-15).
- `platform/games/` — `SnapshotsClient` open/merge-conflict/commit behind a small seam; `NoOpGamesService` / test fake degrade silently.
- `ui/home/` — profile-swap dialog and `HomeUiState` flag; nothing else in `ui/` changes.
- `app/build.gradle.kts` / `libs.versions.toml` — no new SDK artifact expected (`play-services-games-v2` 22.0.0 already ships `SnapshotsClient`).
- `.planning/phases/11-play-games-session/11-HUMAN-UAT.md` — two-emulator tests appended; profile-swap recorded as a second-account gap. ROADMAP External Prerequisites gains "Enable Saved Games in Play Console".

</code_context>

<specifics>
## Specific Ideas

- "Cloud save is a merge, not a copy" (research Pattern 4): the same monotonic function resolves local record, cloud reconcile and snapshot conflict — one implementation, three callers.
- The Play Games saves list entry should look like the app: the launcher icon's English-33 board art as the cover, one readable line under it.
- The profile-swap dialog must read like the Phase 11 opt-in prompt (same clay `AlertDialog`, Home only) and must make "keep this device's progress" the effortless default.

</specifics>

<deferred>
## Deferred Ideas

- Cloud-syncing the in-progress resume snapshot — explicitly deferred to v1.2 by the milestone requirements; only if players report losing mid-game state across devices.
- A Settings "Cloud save: last synced …" status row or a "Progress restored" snackbar — considered under D-03 and rejected for this phase; revisit if support requests show players cannot tell the sync ran.
- A rendered per-player board cover image (current Challenge level drawn to a bitmap) — considered under D-09; a personalization touch for a later polish pass.
- Re-pulling on every foreground return for same-account cross-device play mid-session — considered under D-02; only if real usage shows two devices active at once.
- Syncing the derived skill-achievement flags — considered under D-06; not needed while achievements live server-side.

### Reviewed Todos (not folded)
- **Upgrade Wiegleb-45 opening book from K=3 to K=4** — keyword-only match; Phase 8 solver debt.
- **Confirm audio cues on physical hardware** — keyword-only match; Phase 7 audio debt.
- **Decide whether Stuck finishes should reach Results (1–2 star bands are unreachable)** — keyword-only match; scoring/flow decision independent of cloud save.

</deferred>

---

*Phase: 13-cloud-save*
*Context gathered: 2026-09-15*
