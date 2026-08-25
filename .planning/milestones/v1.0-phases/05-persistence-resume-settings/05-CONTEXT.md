# Phase 5: Persistence, Resume & Settings - Context

**Gathered:** 2026-07-13
**Status:** Ready for planning

<domain>
## Phase Boundary

Make the player's progress survive leaving the app, on a **migration-safe** local storage layer:

- **Per-level results** — stars, best time, best score, saved locally per `(mode, levelId)`.
- **Continue-from-highest Challenge level** — Challenge resumes at the player's saved current level instead of restarting at 1.
- **Full in-progress resume** — the exact board (stored cells, never regenerated) + complete move history (undo still intact) + elapsed time + `generatorVersion`, restorable after the app is closed.
- **Sound & haptics settings** — persisted toggles.
- **`GamesService` no-op stub** — established alongside the existing `AdGateway` stub for Slice 2 (roadmap-mandated; interface + no-op only).

**Requirements:** PERS-01 (per-level results saved), PERS-02 (highest Challenge level saved), PERS-03 (resume in-progress game: board + move history + elapsed time), SET-01 (toggle sound), SET-02 (toggle haptics), CHAL-05 (continue from highest Challenge level).

**Storage technology is LOCKED (not a discussion item)** by the tech stack in `CLAUDE.md`:
- **Room** (2.8.4, KSP) → per-level `LevelResult` rows **and** the in-progress resume snapshot (board + move-history blob serialized via a `TypeConverter` with kotlinx.serialization).
- **DataStore Preferences** (1.2.1, already wired via `appDataStore`) → Challenge progress (`currentLevel`) + settings (`soundOn`, `hapticsOn`), extending the existing shared `app_prefs` store.
- **Migration-safe** is a hard success criterion: `exportSchema = true` with the schema committed to VCS, explicit Room `Migration` objects with a migration test, and **no `fallbackToDestructiveMigration` in release builds** (Success Criterion 5).

**In scope:** adding Room (deps, `RoomDatabase`, DAOs, entities, `TypeConverter`, schema export, migration test); a `ProgressRepository` (per-level results + resume snapshot) as single source of truth; backing the existing in-memory `ChallengeProgress` seam with DataStore; extending `SettingsRepository` with sound/haptics; the resume save/restore wiring in `GameViewModel`/lifecycle; the Home "Continue" affordances; the Results "New best!" cue; earned-stars on Classic catalog cards; a dedicated Settings screen; real haptic feedback on jump; the `GamesService` interface + no-op stub bound in Hilt.

**Out of scope:** cloud save / Play Games Saved Games (Slice 2 — added later behind the same `ProgressRepository`); leaderboards & achievements (Slice 2); real audio engine / sound effect assets (later polish — SET-01 persists the flag only this phase); a Challenge level picker / replaying arbitrary passed levels; attempt/loss statistics for unsolved plays; rewarded-ad gating (Slice 2); diagonal jumps (never).
</domain>

<decisions>
## Implementation Decisions

### In-progress resume UX & lifecycle
- **D-01: Home "Continue" entry, not auto-resume.** On relaunch the player lands on Home as normal; a **Continue** affordance (banner/tile) drops them back into the saved board. No forced dialog, no skipping Home — fits the existing Home-first nav spine and lets the player start something fresh instead.
- **D-02: Resume applies to BOTH Classic and Challenge.** Any mid-game is resumable; the snapshot carries `mode` + `levelId` either way. Success Criterion 3 does not restrict by mode.
- **D-03: One in-progress snapshot PER MODE (not a single global slot).** A Classic snapshot and a Challenge snapshot may coexist — leaving one to play the other keeps both, so Home can show up to two Continue entries. **This intentionally overrides the design spec §10 "single `InProgressGame`" shape**: the resume store is keyed by `mode` (max two live rows), not one row.
- **D-04: Snapshot is written on leave, not per move.** Persist when the game screen is left / the app backgrounds (lifecycle `onStop`) — exactly when data could be lost — so there is no per-tap Room churn. (Planner: guard against the process being killed before `onStop` if cheap, but per-move writes are explicitly not required.)
- **D-05: Snapshot cleared on solve or explicit exit.** Clear when the level is solved (reaches Results) or the player restarts that board / starts a new game in that mode. **Leaving to Home keeps it resumable** (that's the whole point of Continue). A stale Continue never survives a solve or a restart.
- **D-06: The stored board is restored, never regenerated** (Success Criterion 3, carried from roadmap). Resume replays from the stored cells + move history so undo stays intact; `generatorVersion` is stored so a future generator change can be detected.

### "Best result" semantics (per-level results)
- **D-07: Per-field bests, tracked independently.** Keep the best stars, the best (lowest) time, and the best (highest) score **separately** — even across different plays. Every replay can only improve the record. Matches the `LevelResult { stars, bestTimeMs, bestScore }` schema (upsert-on-better-per-field).
- **D-08: Only solved plays record a best.** A per-level result exists once the player actually solves the level (earns stars). Stuck/abandoned attempts write nothing — clean semantics, no attempt/loss tracking this phase.
- **D-09: "New best!" cue on Results.** When a play beats a stored best (new high score, faster time, or more stars), Results shows a small badge/label. Makes persistence visible and satisfying.
- **D-10: Earned stars shown on Classic catalog cards.** Each Classic board card surfaces its earned stars (e.g. 0–3 star pips) from the stored `LevelResult`, so progress is visible at a glance. (Modest UI addition; reuses the read-only catalog card from Phase 2 quick-task 260701-n9t / plan 02-07.)

### Continue & replay Challenge
- **D-11: Home Challenge tile shows "Continue — Level N".** The tile presents the resume level (the player's current level) and is one-tap resumable. Fulfills CHAL-05 visibly.
- **D-12: Frontier-only progression (+ retry current).** Play proceeds at the current level; the player can retry the current level (existing Results "Retry"), but there is **no level picker** to jump back to earlier levels this phase. Keeps the existing linear ladder-of-mastery; deterministic generation means nothing is lost.
- **D-13: Persist `currentLevel` only.** Back the existing in-memory `ChallengeProgress` `StateFlow<Int>` with DataStore Preferences. Because advancing requires a solve and there is no skip (Phase 3 D-02), `currentLevel` **is** the highest reached — so persisting `currentLevel` alone satisfies both CHAL-05 and PERS-02. A separate `highestLevelReached` field is deferred (only a picker/leaderboard would need it).

### Settings
- **D-14: Dedicated Settings screen.** A gear icon on Home opens a Settings destination holding sound + haptics (with room to grow). A new but small screen on the existing type-safe NavHost.
- **D-15: Haptics are wired for real this phase.** Fire a light haptic on peg jump/removal, gated by the haptics toggle (Compose `LocalHapticFeedback`). SET-02 is a felt setting, not a dead switch.
- **D-16: Sound persists a FLAG ONLY this phase.** Store the sound on/off preference; **no audio engine or sound-effect assets are added now** (that's a later polish pass which will consume the flag). SET-01 is satisfied as a persisted, wired-to-UI setting. This keeps Phase 5 focused on the storage layer and avoids pulling audio assets/playback into it.
- **D-17: Fresh-install defaults = both ON** (sound on, haptics on). Fail-open, mirroring the existing `hintsEnabled` DataStore default (`true`). Players expect feedback out of the box.

### Claude's Discretion
- Exact Room schema/entity shapes, DAO query surface, the `TypeConverter` serialization format for the board + move-history blob, and the concrete `Migration` (initial schema is v1 — the migration test may be a forward-migration scaffold proving the harness works).
- Whether `ProgressRepository` is one repository (per-level results + resume snapshot) or split; the spec names a single `ProgressRepository`.
- Exact `GamesService` interface method set and the no-op stub (roadmap/spec §8.4 mandate; bind alongside `AdGateway` in `PlatformModule`). Not a gray area — established as a stub only.
- Whether the existing in-game **"Show possible moves"** (`hintsEnabled`) toggle is *also* mirrored into the new Settings screen. Default: it **stays in-game** (a live gameplay affordance); mirroring is optional and the planner's call.
- Precise Home layout for the Continue affordance(s) and the Settings gear placement, following `compose-skill` state-hoisting + the existing Home/catalog conventions; all new dp/sp → `dimens.xml`, all strings → resources, all keys/constants scoped (QUAL-03).
- DataStore key naming/placement for `currentLevel`, `soundOn`, `hapticsOn` within the shared `app_prefs` store (extend `SettingsPreferences` / add a scoped keys object).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Persistence model & architecture (highest priority)
- `docs/superpowers/specs/2026-06-28-peg-solitaire-design.md` §10 "Persistence Model (Slice 1)" — the `LevelResult { mode, levelId, stars, bestTimeMs, bestScore, completedAt }`, `ChallengeProgress { highestLevelReached, currentLevel }`, `InProgressGame (resume) { mode, levelId, serializedBoard, moveHistory, elapsedMs }`, `Settings { soundOn, hapticsOn }` shapes. Also §8.3 (Repository pattern: `ProgressRepository` per-level results + highest level + in-progress snapshot; `SettingsRepository`; Room for results/progress, DataStore for settings/lightweight state) and §8.4 (`GamesService` no-op stub). **Note the two deliberate deviations captured in this phase:** resume is one-snapshot-**per-mode** (D-03, not the single `InProgressGame` row), and only `currentLevel` is persisted (D-13, `highestLevelReached` deferred).
- `CLAUDE.md` — **the resolved storage-technology decision** (Room vs DataStore table: Room 2.8.4 for `LevelResult` + resume snapshot via KSP + `TypeConverter`; DataStore Preferences 1.2.1 for `ChallengeProgress` + `Settings`; **use Room 2.8.x not Room 3.0**; migration-safety = `exportSchema=true` + committed schema + explicit `Migration` + no `fallbackToDestructiveMigration`; kotlinx-serialization-json 1.11.0 for the blob). Also code standards (no magic numbers/strings → scoped constants, dimens for UI, detekt+ktlint gate, state hoisting via `compose-skill`, Gitflow).

### Requirements & phase goal
- `.planning/ROADMAP.md` §"Phase 5: Persistence, Resume & Settings" — goal (progress survives leaving the app; stored board restored never regenerated; `GamesService` stub alongside `AdGateway`) and the 5 success criteria (esp. #3 restore board+history+elapsed, #5 migration-safety).
- `.planning/REQUIREMENTS.md` — PERS-01, PERS-02, PERS-03, SET-01, SET-02, CHAL-05.

### Existing seams this phase backs / extends (Phase 2–4 output)
- `.planning/phases/03-challenge-generation-difficulty-curve/03-CONTEXT.md` — D-04 (`ChallengeProgress` in-memory seam explicitly shaped for Phase-5 persistence), the `Level.solution` + `generatorVersion` carry-through, `LevelRepository` routing.
- `.planning/phases/04-assist-hint-undo/04-CONTEXT.md` — the in-memory `moveHistory` model (undo = pure replay from `initialBoard`), explicitly deferred "persisting move history / resume with undo intact" to Phase 5 (PERS-03).
- `app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt` — the in-memory `@Singleton StateFlow<Int>` (`currentLevel`, `advanceToNext`, `reset`) to be DataStore-backed (D-13). Its docstring already names this as the Phase-5 seam.
- `app/src/main/java/com/app/pegsolitaire/data/SettingsRepository.kt` — the DataStore-backed settings repo (currently `hintsEnabled`, fail-open `true`); extend with `soundOn`/`hapticsOn` (D-15..D-17) following the same catch→`emptyPreferences` fail-open pattern and `SettingsPreferences` scoped keys.
- `app/src/main/java/com/app/pegsolitaire/data/AppPreferences.kt` + `app/src/main/java/com/app/pegsolitaire/di/DataModule.kt` — the SINGLE shared `app_prefs` `DataStore<Preferences>` (`appDataStore` delegate, Hilt `@Singleton`). All new DataStore keys reuse this one store (do not create a second delegate — it crashes). `DataModule` is where the new Room DB / DAOs / `ProgressRepository` bindings get added.
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` — holds `moveHistory`, `initialBoard`, and the `elapsedSeconds` timer (marked against an injected `TimeSource`) — the source of the resume snapshot (D-04/D-06) and the consumer of restored state; also the jump site for D-15 haptics.
- `app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt` — where the `AdGateway` no-op stub is bound; the new `GamesService` interface + no-op stub bind here alongside it.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`ChallengeProgress` (data)** — in-memory `@Singleton StateFlow<Int> currentLevel` with `advanceToNext()`/`reset()`, purpose-built as the Phase-5 persistence seam (Phase 3 D-04). Back it with DataStore behind the same interface so `AppNavHost`/Results wiring (which already observe it) don't change.
- **`SettingsRepository` + `SettingsRepositoryImpl` + `SettingsPreferences`** — the DataStore fail-open pattern to copy for `soundOn`/`hapticsOn`; already Hilt-bound over the shared store.
- **Shared `appDataStore` (`app_prefs`) + `DataModule.provideAppDataStore`** — the one DataStore delegate every prefs repo shares (a second delegate crashes at runtime). New scalar keys go here.
- **`GameViewModel.moveHistory` / `initialBoard` / `elapsedSeconds` (+ injected `TimeSource`)** — the exact fields the resume snapshot serializes and restores; undo = pure replay so restoring history restores undo (PERS-03 "undo still intact").
- **`BoardCodec` / board bitmask (`Long`) + kotlinx.serialization (already applied to `:app`)** — the encoding substrate for the resume blob `TypeConverter`; `Move` is a domain type already used in golden fixtures.
- **`ScoreCalculator` / `ScoreResult` (stars + score) + `finished` Channel → Results** — the point where a solve is finalized; where per-level upsert (D-07/D-08) and the "New best!" comparison (D-09) hook in.
- **Read-only Classic catalog card (plan 02-07 `BoardPreview`)** — the surface to add earned-star pips (D-10).
- **`AdGateway` / `NoOpAdGateway` + `PlatformModule` `@Binds`** — the exact pattern to mirror for the `GamesService` interface + no-op stub.
- **Type-safe `Routes` + `AppNavHost`** — add a `Settings` destination (D-14) and Home Continue routing (D-01).

### Established Patterns
- Repository pattern = single source of truth; MVVM + `StateFlow<UiState>`, stateless composables (state hoisting via `compose-skill`); one-shot nav events via `Channel`.
- Pure-Kotlin `:domain` stays Android-free — persistence lives in `:app`/data. `LevelResult`/resume snapshot **models** may be plain data classes; Room entities/DAOs live in the `:app` data layer (not `:domain`).
- Data-driven constants split by scope, no magic numbers/strings (QUAL-03) — DataStore keys, DB/table/column names, and defaults in scoped constants objects; all dp/sp → `dimens.xml`; all strings → resources.
- DataStore reads **fail open** (catch → `emptyPreferences` → default); Room reads should degrade gracefully (a missing/corrupt result ⇒ "no best yet", not a crash).

### Integration Points
- **New Room stack** (`RoomDatabase`, `@Entity` for `LevelResult` + resume snapshot, DAOs, `TypeConverter`, `exportSchema=true`, `Migration` + migration test) wired via `DataModule` — the first Room usage in the project (deps + KSP `room-compiler` to add to `libs.versions.toml` / `app/build.gradle.kts`).
- **`ProgressRepository`** (new) — per-level result upsert/query + resume snapshot save/load/clear; Hilt-bound single source of truth; observed by Results (best comparison), the Classic catalog (stars), and Home (Continue entries).
- **`GameViewModel` ↔ lifecycle** — save snapshot on `onStop` (D-04), clear on solve/restart/new (D-05), restore on Continue (D-06); emit haptics on jump gated by the setting (D-15).
- **Settings screen + Home gear** — new destination reading/writing `SettingsRepository`.
- **`GamesService`** — new interface + no-op stub in `PlatformModule` (Slice-2 swap point).

</code_context>

<specifics>
## Specific Ideas

- Resume should feel like a gentle **"Continue"**, not a takeover — land on Home, offer to pick up where they left off (D-01), and keep a Classic and a Challenge game side by side (D-03).
- Persistence should be **visible and rewarding**: a "New best!" nudge on Results (D-09) and earned stars right on the Classic catalog cards (D-10) so the player sees their history, not just an invisible save.
- Challenge continuation should read at a glance — **"Continue — Level N"** on the Home tile (D-11).
- Haptics should be a **real, felt** setting now (D-15); sound is deliberately a persisted flag awaiting a later audio pass (D-16) — the switch is honest (it saves), it just has nothing to play yet.

</specifics>

<deferred>
## Deferred Ideas

- **Real sound effects / audio engine** (SoundPool or similar + assets, gated by the `soundOn` flag) — later polish pass; the SET-01 flag ships ready for it (D-16).
- **Challenge level picker / replaying arbitrary passed levels** — needs `highestLevelReached` persisted + a new level-select screen; deferred (D-12/D-13). Frontier-only this phase.
- **`highestLevelReached` persistence + leaderboards / achievements / cloud save** — Slice 2 (Play Games). Cloud save slots in behind `ProgressRepository` as a second data source; only the `GamesService` no-op stub is built now.
- **Attempt / loss statistics for unsolved plays** — not this phase; only solved plays record (D-08).
- **Mirroring the in-game "Show possible moves" toggle into the Settings screen** — optional, planner's discretion; default keeps it in-game.

None of the above are scope creep into Phase 5 — all are explicitly later-phase, later-slice, or optional.

</deferred>

---

*Phase: 5-persistence-resume-settings*
*Context gathered: 2026-07-13*
