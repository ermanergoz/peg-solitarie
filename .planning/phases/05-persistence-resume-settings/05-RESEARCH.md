# Phase 5: Persistence, Resume & Settings - Research

**Researched:** 2026-07-13
**Domain:** Android local persistence (Room + DataStore) over a Clean-Architecture / Hilt / Compose codebase
**Confidence:** HIGH (stack + codebase grounding), MEDIUM (Room migration-test-on-Robolectric mechanics)

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**In-progress resume UX & lifecycle**
- **D-01: Home "Continue" entry, not auto-resume.** On relaunch the player lands on Home; a Continue affordance drops them back into the saved board. No forced dialog, no skipping Home.
- **D-02: Resume applies to BOTH Classic and Challenge.** Snapshot carries `mode` + `levelId` either way.
- **D-03: One in-progress snapshot PER MODE (not a single global slot).** A Classic snapshot and a Challenge snapshot may coexist. **This intentionally overrides design spec §10's single `InProgressGame` shape** — the resume store is keyed by `mode` (max two live rows).
- **D-04: Snapshot written on leave, not per move.** Persist on `onStop` / app background. Guard against process kill before `onStop` only if cheap; per-move writes are explicitly NOT required.
- **D-05: Snapshot cleared on solve or explicit exit.** Clear on solve (reaches Results) or restart / new-game-in-mode. Leaving to Home keeps it resumable.
- **D-06: Stored board restored, never regenerated** (Success Criterion 3). Resume replays from stored cells + move history so undo stays intact; `generatorVersion` stored so a future generator change can be detected.

**"Best result" semantics**
- **D-07: Per-field bests, tracked independently.** Best stars, best (lowest) time, best (highest) score kept separately — upsert-on-better-per-field.
- **D-08: Only solved plays record a best.** Stuck/abandoned attempts write nothing.
- **D-09: "New best!" cue on Results** when a play beats a stored best.
- **D-10: Earned stars shown on Classic catalog cards** (0–3 star pips from stored `LevelResult`).

**Continue & replay Challenge**
- **D-11: Home Challenge tile shows "Continue — Level N".** One-tap resumable.
- **D-12: Frontier-only progression (+ retry current).** No level picker this phase.
- **D-13: Persist `currentLevel` only.** Back the existing in-memory `ChallengeProgress` `StateFlow<Int>` with DataStore. `currentLevel` == highest reached (linear no-skip ladder), so persisting it alone satisfies both CHAL-05 and PERS-02. `highestLevelReached` deferred.

**Settings**
- **D-14: Dedicated Settings screen.** Gear icon on Home opens a Settings destination.
- **D-15: Haptics wired for real.** Light haptic on peg jump/removal, gated by the haptics toggle (Compose `LocalHapticFeedback`).
- **D-16: Sound persists a FLAG ONLY.** No audio engine / assets now.
- **D-17: Fresh-install defaults = both ON** (fail-open, mirroring the existing `hintsEnabled` default `true`).

### Claude's Discretion
- Exact Room schema/entity shapes, DAO query surface, the `TypeConverter` serialization format for the board + move-history blob, and the concrete initial `Migration` (v1 is initial — the migration test may be a forward-migration scaffold proving the harness works).
- Whether `ProgressRepository` is one repository (per-level results + resume snapshot) or split; spec names a single `ProgressRepository`.
- Exact `GamesService` interface method set and the no-op stub (bind alongside `AdGateway` in `PlatformModule`). Stub only.
- Whether the in-game "Show possible moves" (`hintsEnabled`) toggle is ALSO mirrored into the new Settings screen. Default: stays in-game; mirroring optional.
- Precise Home layout for the Continue affordance(s) and Settings gear placement; all new dp/sp → `dimens.xml`, all strings → resources, all keys/constants scoped (QUAL-03).
- DataStore key naming/placement for `currentLevel`, `soundOn`, `hapticsOn` within the shared `app_prefs` store.

### Deferred Ideas (OUT OF SCOPE)
- Real sound effects / audio engine (assets + playback) — later polish; SET-01 flag ships ready.
- Challenge level picker / replaying arbitrary passed levels — needs `highestLevelReached` + a new screen.
- `highestLevelReached` persistence + leaderboards / achievements / cloud save — Slice 2 (behind `ProgressRepository` / `GamesService`).
- Attempt / loss statistics for unsolved plays — only solved plays record (D-08).
- Mirroring the in-game hints toggle into Settings — optional, planner's call.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PERS-01 | Per-level results (stars, best time, best score) persist across restarts | Room `LevelResult` entity + `@Upsert`/read-modify-write per-field-best DAO; `ProgressRepository`; hooked at the Won → Results finalize point in `GameViewModel.performJump` / `ResultsViewModel`. See Architecture Patterns §2, §5. |
| PERS-02 | Highest Challenge level persists (continue after close/reopen) | DataStore-back `ChallengeProgress.currentLevel` (D-13). **Gap found: the seam is currently unwired** — `advanceToNext()` is never called and the Home tile hardcodes level 1. Phase 5 must both persist AND wire it. See Runtime State Inventory + Pitfall 7. |
| PERS-03 | Resume in-progress game: board + full move history (undo intact) + elapsed time; stored board restored not regenerated | Room single-row-per-mode resume table; kotlinx.serialization JSON blob of initial footprint + full `moveHistory` + `elapsedMs` + `generatorVersion` via a `TypeConverter`; restore = `fold(applyMove)` in `GameViewModel`. See Architecture Patterns §3, §4. |
| SET-01 | Toggle sound (persist flag; no audio yet, D-16) | Extend `SettingsRepository` with `soundOn` (DataStore boolean, default `true`). |
| SET-02 | Toggle haptics; persists AND fires on jump (D-15) | `SettingsRepository.hapticsOn` (default `true`) + `LocalHapticFeedback` in the Compose jump path, gated by the flag. |
| CHAL-05 | Continue Challenge from highest reached level | Same DataStore-backed `currentLevel` as PERS-02; read at the Home Challenge tile (replace hardcoded `CHALLENGE_ENTRY_LEVEL = 1`) and advance on Challenge solve. |
</phase_requirements>

## Summary

Phase 5 is the project's **first use of Room**. Everything it needs is already pinned in `CLAUDE.md` and verified current: Room **2.8.4** (all artifacts — runtime, ktx, compiler, testing, gradle-plugin — confirmed present on Google Maven this session), DataStore Preferences **1.2.1** (already wired via the shared `appDataStore`), and kotlinx-serialization-json **1.11.0** (plugin already applied to `:app` for nav routes). No new library *discovery* is required — the work is (a) adding the Room dependency block + schema-export config to the existing convention-plugin/KSP build, and (b) writing entities, DAOs, a `TypeConverter`, repositories, and a migration-test harness that fit the codebase's established seams.

The codebase is unusually well-prepared for this phase. `ChallengeProgress` was purpose-built as the persistence seam (Phase 3 D-04); `SettingsRepository` + `SettingsPreferences` give an exact fail-open DataStore pattern to copy for `soundOn`/`hapticsOn`; `AdGateway`/`NoOpAdGateway`/`PlatformModule` give the exact interface-plus-no-op-stub-plus-`@Binds` pattern to mirror for `GamesService`; and `GameViewModel` already keeps the game as `initialBoard` + `moveHistory` (current board = `fold(applyMove)`), which is *precisely* the serialization shape PERS-03 needs so undo survives.

**Two findings the planner must act on.** (1) The resume snapshot must be **regeneration-free** (D-06 / Success Criterion 3): store the initial board footprint (in-play `Position`s + target + initial `pegs` Long) plus the full `moveHistory`, and rebuild by replay — do *not* rebuild a Challenge board by calling `loadChallenge(n)`, because that is exactly what a `generatorVersion` bump would silently break. (2) **`ChallengeProgress` is currently defined but unwired** — `advanceToNext()` has zero callers and `AppNavHost` hardcodes Challenge entry to level 1. Persisting it (D-13) is necessary but not sufficient; Phase 5 must also *connect* it (advance on Challenge solve; read it at the Home tile).

**Primary recommendation:** Add Room 2.8.4 via the `androidx.room` Gradle plugin (variant-aware schema export to `$projectDir/schemas`, committed to VCS), one `PegSolitaireDatabase` (`exportSchema = true`, version 1) with two entities (`LevelResultEntity` keyed by `(mode, levelId)`; `ResumeSnapshotEntity` keyed by `mode`, one JSON-blob column). Serialize the resume blob with app-layer `@Serializable` DTOs (keep `:domain` annotation-free). Split responsibilities into a `ProgressRepository` (results + resume) as single source of truth, extend `SettingsRepository`, and DataStore-back `ChallengeProgress`. All tests run JVM-side under Robolectric (the project has **no `androidTest` source set**) — including the Room migration test, which needs a small assets/schema wiring step (documented Pitfall 8).

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Per-level best results (stars/time/score) | Data (Room `LevelResult` + `ProgressRepository`) | UI (Results "New best!", catalog star pips) | Relational, queried per `(mode, levelId)` and aggregated ("total stars") — Room's job; repository is single source of truth. |
| In-progress resume snapshot | Data (Room single-row-per-mode + `ProgressRepository`) | UI/VM (`GameViewModel` save/restore; Home Continue) | Transactional local store; one persistence mechanism for results + resume avoids a second Proto schema (CLAUDE.md). |
| Challenge current level | Data (DataStore-backed `ChallengeProgress`) | Navigation (Home tile entry level), UI (Results advance) | Two scalar ints, read on start, no queries → a key-value tuple, not a table (CLAUDE.md). |
| Settings (sound / haptics) | Data (DataStore `SettingsRepository`) | UI (Settings screen), UI (haptic on jump) | Flow-based async KV; canonical DataStore use case. Haptic *emission* is a Compose UI concern (`LocalHapticFeedback`), gated by the persisted flag. |
| Snapshot serialization (board + history) | Data (`TypeConverter` + `@Serializable` DTOs in `:app`) | Domain (pure `Board`/`Move`/`Position` types, mapped to/from DTOs) | Keeps `:domain` Android- and framework-annotation-free (QUAL-01); the codec/DTO lives in the app data layer, mirroring the existing `BoardCodec` pattern. |
| `GamesService` no-op stub | Platform (`platform/games` interface + `NoOpGamesService`) | DI (`PlatformModule` `@Binds`) | Slice-2 swap point; mirrors `AdGateway`. No behaviour this phase. |
| Lifecycle-driven snapshot save | UI (Compose `LifecycleEventObserver` in `GameRoute`) → VM | Data (`ProgressRepository.saveSnapshot`) | `onStop` is a UI-lifecycle event; the composable observes it and calls a VM method (D-04). |

## Standard Stack

### Core (to add this phase)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `androidx.room:room-runtime` | **2.8.4** | Room DB runtime | CLAUDE.md-pinned; latest 2.8.x stable, verified present on Google Maven 2026-07-13. `[VERIFIED: Google Maven]` |
| `androidx.room:room-ktx` | **2.8.4** | Coroutine/Flow DAOs, `withTransaction` | Flow-returning DAOs feed `StateFlow` UiState; suspend DAO functions keep Room off the main thread. `[VERIFIED: Google Maven]` |
| `androidx.room:room-compiler` | **2.8.4** | Annotation processor (via **KSP**, `ksp(...)`) | KSP not KAPT (CLAUDE.md "What NOT to use"). Project already applies the KSP plugin for Hilt. `[VERIFIED: Google Maven]` |
| `androidx.room:room-testing` | **2.8.4** | `MigrationTestHelper` for the migration test | Success Criterion 5 requires a migration test. `[VERIFIED: Google Maven]` |
| `androidx.room:room-gradle-plugin` (id `androidx.room`) | **2.8.4** | Variant-aware schema export (`room { schemaDirectory(...) }`) | Recommended 2.6+ way to declare the exported-schema location as a proper Gradle input/output (fixes caching + KSP apoption-format pitfalls). `[VERIFIED: Google Maven]` |

### Already present (reuse, do NOT re-add)
| Library | Version | Purpose | Notes |
|---------|---------|---------|-------|
| `androidx.datastore:datastore-preferences` | 1.2.1 | Settings + `currentLevel` scalars | Shared `appDataStore` delegate already provided in `DataModule`. **Never create a second `preferencesDataStore` delegate — it crashes at runtime** (see `AppPreferences.kt` docstring). `[VERIFIED: libs.versions.toml]` |
| `kotlinx-serialization-json` | 1.11.0 | Resume-blob JSON | Runtime present; `org.jetbrains.kotlin.plugin.serialization` already applied to `:app`. `[VERIFIED: libs.versions.toml + app/build.gradle.kts]` |
| `app.cash.turbine` | 1.2.1 | Flow/StateFlow test assertions | For repository/VM Flow tests. `[VERIFIED: libs.versions.toml]` |
| `com.google.truth` | 1.4.4 | Fluent assertions | `[VERIFIED: libs.versions.toml]` |
| `io.mockk` | 1.14.2 | Mocking repos/gateways | `[VERIFIED: libs.versions.toml]` |
| `org.robolectric:robolectric` | 4.16 | JVM Android runtime for all instrumented-style tests | Room DAO + migration tests run here (no `androidTest` source set exists). `[VERIFIED: libs.versions.toml]` |
| `kotlinx-coroutines-test` | 1.11.0 | `runTest`, dispatchers | `[VERIFIED: libs.versions.toml]` |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Room Gradle plugin schema export | `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` | Lighter (no new plugin) but historically bites on the KSP apoption `\S+=\S+` format (nowinandroid#604) and isn't variant-aware. Acceptable fallback if adding a plugin to build-logic is undesirable. `[CITED: github.com/android/nowinandroid/issues/604]` |
| `@Serializable` DTOs in `:app` | Annotate domain `Board`/`Move`/`Position` with `@Serializable` | Rejected: couples `:domain` to the serialization framework and would require applying the serialization plugin to `:domain` (currently `:app`-only). App-layer DTOs preserve domain purity (QUAL-01). |
| Room single-row-per-mode resume table | Proto DataStore for the snapshot | Rejected by CLAUDE.md: keep results + resume in one transactional store; avoids a second schema. |
| Storing initial footprint + moveHistory | Regenerating the Challenge board from `loadChallenge(n)` on resume | Rejected: violates D-06 / Success Criterion 3 ("restored, never regenerated") and defeats the whole point of storing `generatorVersion`. |

**Installation (deltas only):**

`gradle/libs.versions.toml` — add:
```toml
[versions]
room = "2.8.4"

[libraries]
androidx-room-runtime  = { group = "androidx.room", name = "room-runtime",  version.ref = "room" }
androidx-room-ktx      = { group = "androidx.room", name = "room-ktx",      version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing  = { group = "androidx.room", name = "room-testing",  version.ref = "room" }

[plugins]
room = { id = "androidx.room", version.ref = "room" }
```

`app/build.gradle.kts` — apply the plugin, add the schema block, wire deps:
```kotlin
plugins {
    // ...existing (android-app, standards, ksp, hilt, kotlin.serialization)...
    alias(libs.plugins.room)
}

room {
    schemaDirectory("$projectDir/schemas")   // committed to VCS (Success Criterion 5)
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)          // KSP, never KAPT
    testImplementation(libs.androidx.room.testing)
    // Robolectric already present; needed to run the DAO + migration tests JVM-side.
}
```

**Note on version-catalog literals (QUAL-03):** the `room` version is declared once in `libs.versions.toml`; no module hand-pins it (matches the existing catalog rule at the top of that file).

## Package Legitimacy Audit

Ecosystem: **Gradle / Google Maven (AndroidX)** — not npm/PyPI/crates. slopcheck targets those registries and was unavailable; all packages here are **first-party Google/AndroidX artifacts** whose presence was verified directly against the official Google Maven repository this session (the authoritative source).

| Package | Registry | Verified | Source Repo | Disposition |
|---------|----------|----------|-------------|-------------|
| androidx.room:room-runtime 2.8.4 | Google Maven | ✓ present | github.com/androidx/androidx (official) | Approved |
| androidx.room:room-ktx 2.8.4 | Google Maven | ✓ present | github.com/androidx/androidx | Approved |
| androidx.room:room-compiler 2.8.4 | Google Maven | ✓ present | github.com/androidx/androidx | Approved |
| androidx.room:room-testing 2.8.4 | Google Maven | ✓ present | github.com/androidx/androidx | Approved |
| androidx.room:room-gradle-plugin 2.8.4 (id `androidx.room`) | Google Maven | ✓ present | github.com/androidx/androidx | Approved |

**Packages removed due to slop verdict:** none.
**Packages flagged suspicious:** none. All are official AndroidX, matching the `CLAUDE.md` pins. No `postinstall`-equivalent risk (Gradle plugins are declared, not auto-executed from an untrusted registry).

## Architecture Patterns

### System Architecture Diagram

```
                         ┌──────────────────────────── UI / Compose ────────────────────────────┐
  App launch ──▶ Home ──▶│  HomeViewModel: reads ProgressRepository.resumeEntries (Flow)          │
                         │                 reads ChallengeProgress.currentLevel (Flow, persisted) │
                         │   ├─ "Continue — Classic" tile  (if classic snapshot exists)  ─┐        │
                         │   ├─ "Continue — Level N" tile  (challenge snapshot / level)   ─┤        │
                         │   └─ Settings gear ─▶ SettingsScreen (SettingsRepository Flows) │        │
                         └──────────────────────────────────────────────────────────────┼────────┘
                                                                                          │
                     tap Continue → Gameplay(levelId | challengeLevel, resume=true)       │
                                                                                          ▼
        ┌──────────────────────────────── GameViewModel ─────────────────────────────────────────┐
        │  init: if resume ⇒ ProgressRepository.loadSnapshot(mode) → rebuild:                     │
        │           initialBoard(from DTO footprint+pegs) + fold(applyMove, moveHistory) + elapsed │
        │        else ⇒ existing load path (loadClassic / loadChallenge)                           │
        │  onStop (via GameRoute LifecycleEventObserver) ⇒ ProgressRepository.saveSnapshot(...)    │
        │  jump ⇒ (UI) LocalHapticFeedback if hapticsOn                                            │
        │  Won  ⇒ ScoreResult ─┐  (also) advance ChallengeProgress if Challenge                    │
        └──────────────────────┼──────────────────────────────────────────────────────────────────┘
                               ▼
        Results ─▶ ResultsViewModel: ProgressRepository.recordResult(mode, levelId, ScoreResult)
                                     returns per-field "new best?" flags ⇒ "New best!" cue (D-09)
                               │ clears snapshot on solve (D-05)
                               ▼
   ┌──────────────────────────── Data layer (single source of truth) ───────────────────────────┐
   │  ProgressRepository (Hilt @Binds)                                                             │
   │    ├─ LevelResultDao  ── LevelResultEntity  PK(mode, levelId)  [Room]                         │
   │    └─ ResumeDao       ── ResumeSnapshotEntity PK(mode)  jsonBlob:String  [Room]               │
   │         jsonBlob ⇄ @Serializable ResumeSnapshotDto  (TypeConverter, kotlinx.serialization)    │
   │  SettingsRepository (DataStore)  soundOn / hapticsOn / hintsEnabled                            │
   │  ChallengeProgress  (DataStore-backed)  currentLevel                                           │
   │       all over ── PegSolitaireDatabase (exportSchema=true, v1) ── + shared appDataStore        │
   └───────────────────────────────────────────────────────────────────────────────────────────────┘
```

### Recommended Structure (additive, follows existing package layout)
```
app/src/main/java/com/app/pegsolitaire/
├── data/
│   ├── db/
│   │   ├── PegSolitaireDatabase.kt        # @Database(v1, exportSchema=true), @TypeConverters
│   │   ├── LevelResultEntity.kt           # @Entity PK(mode, levelId)
│   │   ├── LevelResultDao.kt              # upsert-on-better + Flow queries
│   │   ├── ResumeSnapshotEntity.kt        # @Entity PK(mode), jsonBlob:String
│   │   ├── ResumeDao.kt                   # upsert/get/delete by mode
│   │   └── SnapshotConverters.kt          # TypeConverter: String ⇄ (kotlinx.serialization already handles DTO⇄String)
│   ├── resume/
│   │   ├── ResumeSnapshotDto.kt           # @Serializable app-layer DTO (footprint, pegs, moves, elapsedMs, generatorVersion)
│   │   └── SnapshotMapper.kt              # DTO ⇄ domain (Board/BoardSpec/Move/Position)
│   ├── ProgressRepository.kt              # interface + Default impl (results + resume)
│   ├── ProgressPersistenceKeys.kt         # scoped DB/table/column name constants (QUAL-03)
│   ├── ChallengeProgress.kt               # (EDIT) DataStore-backed
│   └── SettingsRepository.kt              # (EDIT) + soundOn / hapticsOn
├── platform/games/
│   ├── GamesService.kt                    # interface (Slice-2 seam)
│   └── NoOpGamesService.kt                # @Inject stub
├── di/
│   ├── DatabaseModule.kt                  # provide PegSolitaireDatabase + DAOs (@Provides)
│   ├── DataModule.kt                      # (EDIT) @Binds ProgressRepository
│   └── PlatformModule.kt                  # (EDIT) @Binds GamesService → NoOpGamesService
├── ui/settings/                           # new destination (D-14)
└── navigation/
    ├── Routes.kt                          # (EDIT) + Settings route; Gameplay + resume flag
    └── AppNavHost.kt                      # (EDIT) Settings dest; Home Continue routing; read persisted currentLevel
```

### Pattern 1: Composite-key results entity + per-field upsert-on-better
**What:** `LevelResult` is keyed by `(mode, levelId)` and every replay may only *improve* each field (D-07).
**When:** on every solved play (D-08), at the Won → Results finalize point.
```kotlin
// Source: Room docs (composite PK) + CONTEXT D-07. [CITED: developer.android.com/training/data-storage/room]
@Entity(tableName = "level_result", primaryKey handled below)
data class LevelResultEntity(
    val mode: String,          // "classic" | "challenge"
    val levelId: String,       // catalog id, or challenge level number as string
    val stars: Int,            // best (max)
    val bestTimeMs: Long,      // best (min)
    val bestScore: Int,        // best (max)
    val completedAt: Long,     // epoch millis of the record that set the latest best
)
// Composite key: @Entity(primaryKeys = ["mode", "levelId"]) on the class.

@Dao
interface LevelResultDao {
    @Query("SELECT * FROM level_result WHERE mode = :mode AND levelId = :levelId")
    suspend fun find(mode: String, levelId: String): LevelResultEntity?

    @Query("SELECT * FROM level_result WHERE mode = :mode")
    fun observe(mode: String): Flow<List<LevelResultEntity>>   // catalog star pips (D-10)

    @Upsert
    suspend fun upsert(entity: LevelResultEntity)
}
```
Per-field merge lives in the repository inside a transaction (read-modify-write), returning "new best?" flags for the D-09 cue:
```kotlin
// Repository — pure read/merge/write; @Upsert replaces the whole row with per-field maxima/minima.
suspend fun recordResult(mode: String, levelId: String, r: ScoreResult, nowMs: Long): NewBests =
    db.withTransaction {
        val prev = dao.find(mode, levelId)
        val newTimeMs = r.elapsedSeconds * 1000L
        val bests = NewBests(
            stars = prev == null || r.stars > prev.stars,
            time  = prev == null || newTimeMs < prev.bestTimeMs,
            score = prev == null || r.score > prev.bestScore,
        )
        dao.upsert(
            LevelResultEntity(
                mode, levelId,
                stars      = maxOf(r.stars, prev?.stars ?: 0),
                bestTimeMs = minOf(newTimeMs, prev?.bestTimeMs ?: Long.MAX_VALUE),
                bestScore  = maxOf(r.score, prev?.bestScore ?: 0),
                completedAt = nowMs,
            )
        )
        bests
    }
```
> `@Upsert` is stable in Room 2.8.x. `withTransaction` is the `room-ktx` coroutine transaction wrapper. `[CITED: developer.android.com/jetpack/androidx/releases/room]`

### Pattern 2: Regeneration-free resume snapshot (the load-bearing PERS-03 decision)
**What:** persist enough to rebuild the *exact* board by replay, never by regeneration (D-06).
**When:** on `onStop` (D-04).
```kotlin
// App-layer DTO — @Serializable lives here, NOT on domain types (preserves :domain purity, QUAL-01).
@Serializable
data class ResumeSnapshotDto(
    val mode: String,                       // "classic" | "challenge"
    val levelId: String,                    // classic id  (challenge: "" or the number)
    val challengeLevel: Int? = null,
    val generatorVersion: Int? = null,      // stored so a generator change is detectable (D-06)
    val inPlay: List<IntArray>,             // footprint: [row,col] pairs  → rebuild BoardSpec
    val targetRow: Int? = null,             // BoardSpec.target
    val targetCol: Int? = null,
    val initialPegs: Long,                  // Board.pegs of the INITIAL board
    val moves: List<IntArray>,              // full history: [fromR,fromC, overR,overC, toR,toC]
    val elapsedMs: Long,
)
```
Restore (in `GameViewModel`, mirrors the existing `initialBoard` + `fold(applyMove)` model):
```kotlin
val spec = BoardSpec(dto.inPlay.map { Position(it[0], it[1]) },
                     dto.targetRow?.let { Position(it, dto.targetCol!!) })
val initial = Board(spec, dto.initialPegs)
val history = dto.moves.map { Move(Position(it[0],it[1]), Position(it[2],it[3]), Position(it[4],it[5])) }
val current = history.fold(initial) { b, m -> rulesEngine.applyMove(b, m) }
// moveHistory := history  ⇒ undo works immediately (undo = pure replay, Phase 4)
```
**Why this shape:** `Board` = `BoardSpec` (in-play `Position`s + optional target) + a `pegs: Long`. `BoardSpec` is *not* a data class and has no serializer, so store its `inPlay`/`target` explicitly. Storing `initialPegs` + full `moves` (rather than the current board) is what keeps **undo intact** — the whole point of PERS-03. The JSON string is the single `jsonBlob` column; a Room `TypeConverter` (or a plain `String` column the repository fills via `Json.encodeToString`) bridges it.

### Pattern 3: Single-row-per-mode resume table (D-03)
```kotlin
@Entity(tableName = "resume_snapshot")
data class ResumeSnapshotEntity(
    @PrimaryKey val mode: String,           // "classic" | "challenge"  → max two rows
    val jsonBlob: String,                   // ResumeSnapshotDto serialized
    val updatedAt: Long,
)

@Dao
interface ResumeDao {
    @Query("SELECT * FROM resume_snapshot WHERE mode = :mode")
    suspend fun get(mode: String): ResumeSnapshotEntity?
    @Query("SELECT mode FROM resume_snapshot")               // Home: which Continue tiles to show
    fun observeModes(): Flow<List<String>>
    @Upsert suspend fun upsert(entity: ResumeSnapshotEntity)
    @Query("DELETE FROM resume_snapshot WHERE mode = :mode")
    suspend fun clear(mode: String)                          // D-05 solve/restart/new-game
}
```

### Pattern 4: DataStore-backed `ChallengeProgress` (keep the interface, swap the backing)
The current `ChallengeProgress` is a `@Singleton` holding a `MutableStateFlow<Int>`. Back it with the shared `appDataStore` while keeping the *same* public surface (`currentLevel: StateFlow<Int>` / `advanceToNext()` / `reset()`), so no consumer signature changes:
```kotlin
// Reuse the shared store (NEVER a new delegate). Expose currentLevel as a Flow.map over dataStore.data,
// or as a StateFlow via stateIn(scope). Writes go through dataStore.edit { it[KEY] = next }.
// Keys live in a scoped constants object (QUAL-03), added alongside SettingsPreferences.
val CURRENT_LEVEL_KEY = intPreferencesKey("challenge_current_level")
const val DEFAULT_CURRENT_LEVEL = 1   // fresh install starts at level 1 (fail-open)
```
> **Wiring gap (must fix):** currently nothing calls `advanceToNext()` and `AppNavHost` hardcodes `CHALLENGE_ENTRY_LEVEL = 1`. Phase 5 must (a) advance on a Challenge Won (in `GameViewModel`/Results finalize), and (b) source the Home tile's entry level from the persisted `currentLevel`. See Pitfall 7.

### Pattern 5: `GamesService` no-op stub (mirror `AdGateway` exactly)
```kotlin
// platform/games/GamesService.kt — the Slice-2 seam; no behaviour in Slice 1.
interface GamesService {
    suspend fun signIn(): Boolean            // stub → false (not signed in)
    fun submitScore(leaderboardId: String, score: Long)   // stub → Unit
    // Keep minimal; exact method set is Claude's discretion (spec §8.4). Bind alongside AdGateway.
}
// platform/games/NoOpGamesService.kt
class NoOpGamesService @Inject constructor() : GamesService { /* false / Unit */ }
// di/PlatformModule.kt  (EDIT — add next to bindAdGateway)
@Binds @Singleton abstract fun bindGamesService(impl: NoOpGamesService): GamesService
```

### Pattern 6: Lifecycle-driven snapshot save (D-04)
Save on `onStop` from the composable, not from `ViewModel.onCleared` (which does not map to `onStop`). Observe the lifecycle in `GameRoute`:
```kotlin
// GameRoute — mirror the existing collectAsStateWithLifecycle style.
val lifecycleOwner = LocalLifecycleOwner.current
DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_STOP) viewModel.onLeaveScreen()   // → ProgressRepository.saveSnapshot
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
}
```
`viewModel.onLeaveScreen()` builds the `ResumeSnapshotDto` from `initialBoard` + `moveHistory` + `elapsedSeconds` and persists it — but only while `status == Playing` (never persist a solved/won board; D-05).

### Pattern 7: Haptic on jump gated by the persisted flag (SET-02/D-15)
`LocalHapticFeedback` is a Compose UI concern. Collect `hapticsOn` into `GameUiState` (exactly as `showHints` is collected today), and fire in the UI when a jump animation begins:
```kotlin
// In GameScreen / BoardCanvas jump path:
val haptics = LocalHapticFeedback.current
LaunchedEffect(state.animating?.id) {
    if (state.animating != null && state.hapticsOn) {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)  // or a light type available in the pinned Compose BOM
    }
}
```
> Verify the exact `HapticFeedbackType` constants available in Compose BOM 2026.06.00 at implementation; `LongPress`/`TextHandleMove` are long-standing. `[ASSUMED]`

### Anti-Patterns to Avoid
- **Regenerating a resume board** via `loadChallenge(n)` — violates D-06 and defeats `generatorVersion`. Rebuild by replay from stored cells.
- **A second `preferencesDataStore` delegate** — runtime crash ("multiple DataStores active for the same file"). Reuse `appDataStore` (see `AppPreferences.kt`).
- **`@Serializable` on domain types** — couples `:domain` to the serialization framework. Use app-layer DTOs.
- **`fallbackToDestructiveMigration()` in release** — explicitly forbidden (Success Criterion 5). Never call it in the DB builder; if used at all, guard behind a debug-only flag (and prefer not at all).
- **Main-thread Room access** — never call blocking (non-`suspend`, non-`Flow`) DAO methods on the main thread; Room throws by default. Use `suspend`/`Flow` DAOs (`room-ktx`).
- **Saving a snapshot for a solved game** — clear on solve (D-05); only persist a `Playing` board.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Local relational storage + queries | A hand-rolled SQLite `SQLiteOpenHelper` / JSON-file store | Room 2.8.4 entities + DAOs | Type-safe DAOs, compile-time SQL verification, migration tooling, `Flow` observation. |
| Schema versioning / migration harness | Custom version-int + hand-written ALTERs without validation | Room `Migration` + `MigrationTestHelper` + `exportSchema` JSON | Success Criterion 5 requires exactly this; the exported schema is the diffable contract. |
| Board/move (de)serialization | A bespoke binary format | kotlinx.serialization JSON over app DTOs | Already applied to `:app`; versionable, debuggable, human-diffable. |
| Settings/scalar persistence | `SharedPreferences` | DataStore Preferences (shared `appDataStore`) | Async, Flow-based, transactional; SharedPreferences is forbidden by CLAUDE.md. |
| Per-field "best" merge | Ad-hoc if/else scattered in the VM | A single repository `recordResult` transaction | One source of truth; testable; returns the D-09 cue flags. |
| Ads/Games platform seams | Real SDK now | `NoOpAdGateway` / `NoOpGamesService` behind interfaces | Slice-1 stubs; Slice-2 swaps only the Hilt binding. |

**Key insight:** every capability this phase needs already has an official, pinned library and an *in-repo pattern to copy* — the value is in wiring correctly (regeneration-free resume; actually connecting `ChallengeProgress`), not in inventing storage.

## Runtime State Inventory

> This is a **greenfield persistence** phase (adding the storage layer), not a rename/migration. There is no prior on-device schema to migrate — the DB ships at **version 1**. Categories below are answered explicitly.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data (existing on-device) | **None** — this phase introduces the first Room DB. Only the existing `app_prefs` DataStore holds one boolean (`onboarding_seen`) + the hint flag. No prior `LevelResult`/resume data exists to migrate. | Ship DB at v1, `exportSchema=true`. No data migration. |
| Live service config | **None** — no external services with embedded config (no cloud save until Slice 2). | None. |
| OS-registered state | **None** — no Task Scheduler / launchd / background services registered by this app. | None. |
| Secrets / env vars | **None** — local-only storage; no keys, no encrypted store this phase. | None. |
| Build artifacts / installed packages | **New:** the `androidx.room` KSP processor generates `_Impl` classes; the exported schema JSON at `$projectDir/schemas/<db>/1.json` is a **committed build artifact** (Success Criterion 5). | Add `schemas/` to VCS; ensure it is NOT bundled into the APK (schemas are dev/test only). |
| **Code seam already present but UNWIRED** | `ChallengeProgress.advanceToNext()` has **zero callers**; `AppNavHost.CHALLENGE_ENTRY_LEVEL` is hardcoded to `1`. Challenge progression currently flows only through nav args (`Results → toChallengeBoard(N+1)`) and is lost on process death. | Persist via DataStore **and** wire: advance on Challenge solve; read `currentLevel` at the Home tile. |

## Common Pitfalls

### Pitfall 1: Room migration test can't find the schema under Robolectric
**What goes wrong:** `MigrationTestHelper` throws `FileNotFoundException: Cannot find the schema file in the assets folder` when run as a JVM/Robolectric unit test (this project has **no `androidTest` source set** — everything runs in `test/` via Robolectric).
**Why:** `MigrationTestHelper` reads exported schema JSON from the *test* classpath/assets; the exported `schemas/` dir is not automatically on the unit-test asset path.
**How to avoid:** either (a) add the schema dir to the unit-test assets — `android.sourceSets.test.assets.srcDirs += "$projectDir/schemas"` (or `getByName("test").assets.srcDir(...)`), or (b) use the newer Room 2.6+ `MigrationTestHelper(instrumentation, databaseClass, ...)` / file-directory constructor. Run under `@RunWith(RobolectricTestRunner::class)` and obtain the instrumentation via `InstrumentationRegistry.getInstrumentation()` (Robolectric provides it). **Confidence: MEDIUM — verify the exact working constructor + srcDir wiring at implementation.**
`[CITED: github.com/robolectric/robolectric/issues/6650]` `[CITED: blog.keithyokoma.dev/entry/2026/05/19/151226]`

### Pitfall 2: KSP `room.schemaLocation` apoption format / missing schema export
**What goes wrong:** schema JSON never gets written (so no migration test possible), or KSP rejects the option with `apoption does not match \S+=\S+`.
**Why:** the raw `ksp { arg("room.schemaLocation", ...) }` path is finicky; forgetting `exportSchema=true` silently skips export.
**How to avoid:** prefer the **`androidx.room` Gradle plugin** with `room { schemaDirectory("$projectDir/schemas") }` (variant-aware, no apoption-format issue) AND set `exportSchema = true` on `@Database`. Verify a `schemas/<db>/1.json` file appears after a build. `[CITED: github.com/android/nowinandroid/issues/604]`

### Pitfall 3: `ChallengeProgress` DataStore backing breaks its `StateFlow` synchronicity
**What goes wrong:** consumers read `currentLevel.value` synchronously today (a hot `StateFlow`); a naive `dataStore.data.map { }` is a cold `Flow` with no `.value`.
**Why:** DataStore reads are async/cold.
**How to avoid:** expose `currentLevel` as a `StateFlow<Int>` via `stateIn(scope, SharingStarted.Eagerly, DEFAULT_CURRENT_LEVEL)` inside the `@Singleton`, or refactor consumers to collect the Flow. Keep `advanceToNext()`/`reset()` as `suspend` (or fire-and-forget in an injected scope) that write through `dataStore.edit`. Preserve the public shape so `AppNavHost`/Results wiring changes minimally.

### Pitfall 4: Persisting a snapshot on `onStop` after a solve
**What goes wrong:** a stale "Continue" survives a completed game because `onStop` fires after Results.
**Why:** `onStop` fires on every backgrounding, including post-solve.
**How to avoid:** save only while `status == Playing` (D-05); clear the snapshot at the Won → Results finalize point and on restart/new-game.

### Pitfall 5: Domain purity gate (ArchUnit) rejects serialization annotations in `:domain`
**What goes wrong:** adding `@Serializable`/Room annotations to domain types trips the Phase-1 `android.*`/purity boundary test or forces the serialization plugin into `:domain`.
**Why:** `:domain` is a pure-Kotlin module by design (QUAL-01).
**How to avoid:** keep all Room entities, DAOs, `@Serializable` DTOs, and mappers in `:app`'s data layer. Domain types stay annotation-free; map at the boundary (`SnapshotMapper`).

### Pitfall 6: Coroutine-test flakiness on DataStore/Room Flows
**What goes wrong:** Turbine tests hang or miss emissions.
**Why:** DataStore/Room `Flow`s + `runTest` virtual time interplay.
**How to avoid:** copy the existing `SettingsRepositoryTest` pattern (real `PreferenceDataStoreFactory` over a `TemporaryFolder`, `runTest`, `.first()`); for Room use an in-memory DB (`Room.inMemoryDatabaseBuilder`) under Robolectric with `runTest` + Turbine on `Flow` DAOs.

### Pitfall 7: Persisting `currentLevel` without wiring it (the "invisible feature" trap)
**What goes wrong:** DataStore stores `currentLevel` but Challenge still enters at level 1 and never advances, because the seam was never connected.
**Why:** `advanceToNext()` has no callers today; the Home tile hardcodes `CHALLENGE_ENTRY_LEVEL = 1`; progression currently rides nav args only.
**How to avoid:** in the same phase, (a) call `advanceToNext()` (or `setCurrentLevel(n+1)`) when a Challenge level is solved, and (b) replace the hardcoded entry level in `AppNavHost` with the persisted `currentLevel`. Add a test asserting a solve → persisted increment → Home tile reads level N.

### Pitfall 8: `TypeConverter` not registered / blob schema drift
**What goes wrong:** Room fails to compile ("Cannot figure out how to save this field") or a schema change goes untracked.
**Why:** forgetting `@TypeConverters(...)` on the `@Database`, or changing entity columns without bumping `version`.
**How to avoid:** if you store the DTO via a converter, register it on the `@Database`; simplest is a plain `String` column filled by the repository (`Json.encodeToString`) needing no converter. Any entity-column change ⇒ bump DB `version` + add a `Migration` + regenerate the exported schema (which the migration test then validates).

## Code Examples

### Database declaration (v1, schema exported)
```kotlin
// Source: Room docs. [CITED: developer.android.com/training/data-storage/room/migrating-db-versions]
@Database(
    entities = [LevelResultEntity::class, ResumeSnapshotEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class PegSolitaireDatabase : RoomDatabase() {
    abstract fun levelResultDao(): LevelResultDao
    abstract fun resumeDao(): ResumeDao
}
```

### Hilt provision (new `DatabaseModule`)
```kotlin
@Module @InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): PegSolitaireDatabase =
        Room.databaseBuilder(ctx, PegSolitaireDatabase::class.java, PersistenceKeys.DB_NAME)
            // NO fallbackToDestructiveMigration() — release-safe (Success Criterion 5).
            // .addMigrations(MIGRATION_1_2)  // added when v2 lands
            .build()

    @Provides fun provideLevelResultDao(db: PegSolitaireDatabase) = db.levelResultDao()
    @Provides fun provideResumeDao(db: PegSolitaireDatabase) = db.resumeDao()
}
```
> `DB_NAME` and all table/column names belong in a scoped constants object (QUAL-03), matching `SettingsPreferences`/`AppPreferences`.

### Migration-test scaffold (v1 initial — proves the harness)
```kotlin
// Runs JVM-side under Robolectric (no androidTest source set). See Pitfall 1 for schema/assets wiring.
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        PegSolitaireDatabase::class.java,
    )
    @Test fun v1SchemaOpens() {
        helper.createDatabase(TEST_DB, 1).apply { close() }   // proves export + harness wired
    }
    // When v2 ships: helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
}
```

### DataStore settings extension (copy the fail-open pattern)
```kotlin
// Add to SettingsRepository / SettingsPreferences, mirroring hintsEnabled exactly.
val SOUND_KEY = booleanPreferencesKey("sound_on");   const val DEFAULT_SOUND = true
val HAPTICS_KEY = booleanPreferencesKey("haptics_on"); const val DEFAULT_HAPTICS = true

override val soundOn: Flow<Boolean> =
    dataStore.data.catch { emit(emptyPreferences()) }
        .map { it[SettingsPreferences.SOUND_KEY] ?: SettingsPreferences.DEFAULT_SOUND }
override suspend fun setSoundOn(on: Boolean) { dataStore.edit { it[SettingsPreferences.SOUND_KEY] = on } }
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `ksp { arg("room.schemaLocation", …) }` | `androidx.room` Gradle plugin `room { schemaDirectory(…) }` | Room 2.6.0 (2023) | Variant-aware schema export as a proper Gradle input/output; avoids KSP apoption pitfalls. `[CITED: developer.android.com/jetpack/androidx/releases/room]` |
| `@Insert(onConflict=REPLACE)` + separate `@Update` | `@Upsert` | Room 2.5.0 | One annotation for insert-or-update. |
| KAPT for Room | KSP (`ksp(room-compiler)`) | KSP GA; Room KSP-first | Faster builds; CLAUDE.md-mandated. |
| Migration tests only as instrumented (`androidTest`) | `MigrationTestHelper` runnable under Robolectric in `test/` | Room 2.4+ / Robolectric fixes | Fits this project's JVM-only test strategy (with the assets caveat, Pitfall 1). `[CITED: blog.keithyokoma.dev]` |

**Deprecated/outdated (do NOT use — reaffirms CLAUDE.md):** Room 3.0 (`androidx.room3`, alpha/KMP), KAPT, `SharedPreferences`, `fallbackToDestructiveMigration` in release.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `MigrationTestHelper` runs cleanly under Robolectric in the `test/` source set with `schemas/` added to test assets (or the 2.6+ file constructor) | Pitfall 1, Code Examples | MEDIUM — may need to verify the exact constructor/srcDir combo; worst case a tiny `androidTest` set is added just for the migration test. Flag at implementation. |
| A2 | Compose BOM 2026.06.00 exposes `HapticFeedbackType.LongPress` (and light equivalents) via `LocalHapticFeedback` | Pattern 7 | LOW — API is long-standing; confirm exact constant names against the pinned BOM. |
| A3 | Storing `initialPegs` + `inPlay` footprint + `moveHistory` fully reconstructs any Classic and Challenge board via `RulesEngine.applyMove` | Pattern 2 | LOW — verified against `Board`/`BoardSpec`/`Move` source and the existing `GameViewModel` fold model; this is exactly how undo already works. |
| A4 | `@Upsert` + `withTransaction` are stable in Room 2.8.4 | Pattern 1 | LOW — both GA since 2.5/2.2 respectively. |
| A5 | Exposing `ChallengeProgress.currentLevel` as `stateIn`-backed `StateFlow` keeps all current consumers compiling | Pitfall 3 | LOW — only `AppNavHost` references it (grep-confirmed); consumer surface is tiny. |

## Open Questions

1. **Migration-test placement under Robolectric (A1).**
   - Known: project runs all tests JVM-side; no `androidTest` set; `room-testing` `MigrationTestHelper` needs schema JSON reachable + an `Instrumentation`.
   - Unclear: whether the assets-srcDir approach or the newer file-directory constructor is the clean path in Room 2.8.4 + Robolectric 4.16.
   - Recommendation: spike the v1 `createDatabase` test first (Code Examples); if Robolectric can't resolve assets, add `schemas/` to `test` assets srcDirs; only if that fails, add a minimal `androidTest` set for this one test.

2. **Challenge `levelId` in `LevelResult` (D-07 keying).**
   - Known: Classic uses catalog string ids; Challenge is an infinite integer ladder.
   - Unclear: whether to store per-Challenge-level results at all (D-08 records solved plays; the catalog star pips D-10 are Classic-only).
   - Recommendation: key Challenge results by `mode="challenge", levelId=<level number as string>` for uniformity; the "New best!" cue (D-09) then works for Challenge replays too. Confirm with planner whether Challenge per-level bests are surfaced anywhere this phase (likely only the cue).

3. **Does the Settings screen mirror the in-game `hintsEnabled` toggle?** Claude's discretion (default: no). Recommendation: leave it in-game; add only `soundOn`/`hapticsOn` to Settings to keep scope tight.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Google Maven (androidx.room 2.8.4) | Room stack | ✓ verified | 2.8.4 (runtime/ktx/compiler/testing/gradle-plugin) | — |
| KSP plugin | room-compiler | ✓ applied | 2.3.9 | — |
| kotlin.serialization plugin | resume DTO | ✓ applied to `:app` | tracks Kotlin 2.3.20 | — |
| Robolectric | JVM Room/migration tests | ✓ present | 4.16 | (only) add minimal `androidTest` set if migration test can't run JVM-side |
| DataStore Preferences | settings/currentLevel | ✓ present | 1.2.1 | — |

**Missing dependencies with no fallback:** none.
**Missing with fallback:** Robolectric migration-test support — fallback is a single-purpose `androidTest` migration test (A1/Open-Q1).

## Validation Architecture

`.planning/config.json` was not found at research time; per policy (key absent ⇒ enabled) this section is included.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit4 4.13.2 + Robolectric 4.16 (JVM), Turbine 1.2.1, Truth 1.4.4, MockK 1.14.2, kotlinx-coroutines-test 1.11.0 |
| Config file | `app/build.gradle.kts` `testOptions.unitTests.isIncludeAndroidResources = true` (in `android-app` convention plugin) |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "*Progress*" --tests "*Resume*" --tests "*Settings*"` |
| Full suite command | `./gradlew check` (runs `:domain` + `:app` unit tests + detekt + ktlint + `dpSpLiteralCheck`) |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PERS-01 | per-field best upsert (max stars / min time / max score) | unit (in-memory Room, Robolectric) | `./gradlew :app:testDebugUnitTest --tests "*ProgressRepositoryTest*"` | ❌ Wave 0 |
| PERS-01/D-09 | `recordResult` returns correct "new best" flags | unit | same class | ❌ Wave 0 |
| PERS-03 | snapshot round-trips: save → load → rebuilt board equals current, moveHistory intact (undo works) | unit | `--tests "*ResumeRepositoryTest*"` / `--tests "*SnapshotMapperTest*"` | ❌ Wave 0 |
| PERS-03/D-06 | rebuild uses stored cells, NOT `loadChallenge` (assert no generator call) | unit (MockK spy on repository) | `--tests "*ResumeRepositoryTest*"` | ❌ Wave 0 |
| PERS-02/CHAL-05 | solve advances persisted `currentLevel`; Home reads it | unit | `--tests "*ChallengeProgressTest*"` (extend existing) | ⚠️ extend existing |
| SET-01/SET-02 | `soundOn`/`hapticsOn` default true, round-trip, fail-open | unit (temp-file DataStore) | `--tests "*SettingsRepositoryTest*"` (extend existing) | ⚠️ extend existing |
| SC-5 | exported v1 schema opens via `MigrationTestHelper` | unit (Robolectric) | `--tests "*MigrationTest*"` | ❌ Wave 0 (A1 caveat) |
| SET-02/D-15 | haptic fires on jump only when `hapticsOn` | Compose UI test (Robolectric) | extend `GameFlowTest` | ⚠️ extend existing |

### Sampling Rate
- **Per task commit:** the targeted `--tests` command for the touched area (< 30s).
- **Per wave merge:** `./gradlew :app:testDebugUnitTest`.
- **Phase gate:** `./gradlew check` green (incl. detekt/ktlint/dp-sp gate) before `/gsd:verify-work`.

### Wave 0 Gaps
- [ ] `data/db/…` entities/DAOs + `PegSolitaireDatabase` — needed before any persistence test.
- [ ] `ProgressRepositoryTest` (in-memory Room) — covers PERS-01, D-09.
- [ ] `ResumeRepositoryTest` / `SnapshotMapperTest` — covers PERS-03, D-06.
- [ ] `MigrationTest` (Robolectric) + `schemas/` on test assets — covers SC-5 (A1).
- [ ] Extend `ChallengeProgressTest` (DataStore-backed + advance wiring) — PERS-02/CHAL-05.
- [ ] Extend `SettingsRepositoryTest` (sound/haptics) — SET-01/SET-02.
- [ ] Room test deps: `testImplementation(libs.androidx.room.testing)` (Robolectric already present).

## Security Domain

`security_enforcement` config absent ⇒ treated as enabled. This phase is **local-only device storage** (no network, no auth, no secrets, no PII), so most ASVS categories are N/A.

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | No auth this phase (`GamesService` sign-in is a Slice-2 no-op stub). |
| V3 Session Management | no | — |
| V4 Access Control | no | Single local user; app-private storage. |
| V5 Input Validation / Deserialization | **yes** | The resume JSON blob is app-written but could be corrupted on disk. Deserialize defensively: wrap `Json.decodeFromString` in try/catch → treat a corrupt/invalid snapshot as "no resume" (fail-safe: drop the Continue tile), and validate reconstructed `Move`s through `RulesEngine.legalMoves` before trusting them (mirrors the existing "never trust serialized state" T-04-01-U rule). Use `Json { ignoreUnknownKeys = true }` for forward-compat. |
| V6 Cryptography | no | No sensitive data; no encryption required (do NOT hand-roll any). |

### Known Threat Patterns for local Room/DataStore
| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Corrupt/tampered resume blob crashes restore | Denial of Service | try/catch decode → fail-safe to "no snapshot"; validate moves via `RulesEngine` before applying. |
| Corrupt DataStore read hides settings / progress | Denial of Service | Existing fail-open pattern (`catch → emptyPreferences → default`); apply to `soundOn`/`hapticsOn`/`currentLevel`. |
| Destructive migration silently wipes results | Tampering / data loss | No `fallbackToDestructiveMigration` in release; explicit `Migration` + migration test (Success Criterion 5). |
| SQL injection | Tampering | N/A — Room DAOs use bound `:params`, never string-concatenated SQL. |

## Sources

### Primary (HIGH confidence)
- Google Maven repository — androidx.room 2.8.4 (room-runtime, room-ktx, room-compiler, room-testing, room-gradle-plugin) all confirmed present (`dl.google.com/dl/android/maven2/androidx/room/...`), 2026-07-13.
- Repository source (read this session): `SettingsRepository.kt`, `AppPreferences.kt`, `ChallengeProgress.kt`, `DataModule.kt`, `PlatformModule.kt`, `AdGateway.kt`/`NoOpAdGateway.kt`/`AdReason.kt`, `GameViewModel.kt`, `GameRoute.kt`, `AppNavHost.kt`, `Routes.kt`, `HomeViewModel.kt`, `LevelRepository.kt`, `Board.kt`/`BoardSpec.kt`/`Move.kt`/`Position.kt`/`Cell.kt`/`GameStatus.kt`, `GeneratedLevel.kt`, `ScoreResult.kt`, `BoardCodec.kt`, `SettingsRepositoryTest.kt`, `libs.versions.toml`, `app/build.gradle.kts`, `android-app.gradle.kts`, `standards.gradle.kts`.
- `CLAUDE.md` Technology Stack (Room/DataStore/serialization pins, migration-safety criteria).
- developer.android.com/training/data-storage/room + /jetpack/androidx/releases/room — Room APIs, migration, schema export.

### Secondary (MEDIUM confidence)
- github.com/android/nowinandroid/issues/604 — `room.schemaLocation` KSP apoption format pitfall.
- github.com/robolectric/robolectric/issues/6650 — `MigrationTestHelper` "schema file in assets" under Robolectric.
- blog.keithyokoma.dev/entry/2026/05/19/151226 — testing Room migrations with Robolectric.

### Tertiary (LOW confidence — flagged for validation)
- Exact `HapticFeedbackType` constant names in Compose BOM 2026.06.00 (A2).
- Precise `MigrationTestHelper` constructor that works JVM-side in Room 2.8.4 + Robolectric 4.16 (A1) — spike first.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all pins verified on Google Maven; deps already in CLAUDE.md.
- Architecture: HIGH — grounded in read-through of every seam the phase extends; the resume shape follows the existing `initialBoard + moveHistory` model verbatim.
- Migration-test mechanics: MEDIUM — JVM/Robolectric path has a documented assets caveat; spike recommended (A1/Open-Q1).
- Pitfalls: HIGH — the two most consequential (regeneration-free resume; unwired `ChallengeProgress`) are confirmed against source, not assumed.

**Research date:** 2026-07-13
**Valid until:** 2026-08-13 (stable AndroidX pins; re-verify Room only if the CLAUDE.md pin changes).
