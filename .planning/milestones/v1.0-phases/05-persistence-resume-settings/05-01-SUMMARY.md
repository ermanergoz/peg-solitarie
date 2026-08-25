---
phase: 05-persistence-resume-settings
plan: 01
subsystem: database
tags: [room, ksp, sqlite, hilt, robolectric, migration-testing, play-games]

# Dependency graph
requires:
  - phase: 01-foundation
    provides: Hilt SingletonComponent graph, standards gate (detekt/ktlint/dpSp), version catalog as sole version source
  - phase: 02-navigation-onboarding
    provides: shared `app_prefs` DataStore + `DataModule` provider pattern mirrored by `DatabaseModule`
  - phase: 04-assist
    provides: `AdGateway`/`NoOpAdGateway`/`PlatformModule` seam copied for `GamesService`
provides:
  - Room 2.8.4 wired via KSP with variant-aware schema export to a VCS-committed `app/schemas`
  - "`PegSolitaireDatabase` v1 (exportSchema=true, no destructive-migration fallback)"
  - "`LevelResultEntity` + `LevelResultDao` — per-level bests keyed by composite PK (mode, levelId)"
  - "`ResumeSnapshotEntity` + `ResumeDao` — one snapshot row per mode, opaque JSON blob"
  - "`ProgressPersistenceKeys` — scoped DB/table/column/mode constants"
  - "`DatabaseModule` — Hilt `@Provides` for the database and both DAOs"
  - Working JVM-side (Robolectric) `MigrationTestHelper` harness — RESEARCH Open-Q1 resolved
  - "`GamesService` + `NoOpGamesService` bound in `PlatformModule` alongside `AdGateway`"
affects: [05-02, 05-03, 05-04, progress-repository, resume, challenge-progress, slice-2-play-games]

# Tech tracking
tech-stack:
  added: [androidx.room:room-runtime 2.8.4, androidx.room:room-ktx 2.8.4, androidx.room:room-compiler 2.8.4 (KSP), androidx.room:room-testing 2.8.4, androidx.room gradle plugin 2.8.4]
  patterns:
    - Room DAOs are suspend/Flow only — no blocking main-thread access
    - Table/column/mode names are folded `const` values from a scoped keys object, referenced inside `@Entity`/`@Query`
    - "`object` Hilt module for `@Provides` (a `@Database`/DAO cannot be `@Binds`-bound and instance `@Provides` cannot live on an abstract module)"
    - Exported schema JSON is committed and opened by a Robolectric migration test as the migration-safety gate

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/data/ProgressPersistenceKeys.kt
    - app/src/main/java/com/app/pegsolitaire/data/db/PegSolitaireDatabase.kt
    - app/src/main/java/com/app/pegsolitaire/data/db/LevelResultEntity.kt
    - app/src/main/java/com/app/pegsolitaire/data/db/LevelResultDao.kt
    - app/src/main/java/com/app/pegsolitaire/data/db/ResumeSnapshotEntity.kt
    - app/src/main/java/com/app/pegsolitaire/data/db/ResumeDao.kt
    - app/src/main/java/com/app/pegsolitaire/di/DatabaseModule.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/GamesService.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/NoOpGamesService.kt
    - app/schemas/com.app.pegsolitaire.data.db.PegSolitaireDatabase/1.json
    - app/src/test/java/com/app/pegsolitaire/data/db/PegSolitaireDatabaseTest.kt
    - app/src/test/java/com/app/pegsolitaire/data/db/MigrationTest.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/NoOpGamesServiceTest.kt
  modified:
    - gradle/libs.versions.toml
    - app/build.gradle.kts
    - app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt

key-decisions:
  - "Exported schema lives on the DEBUG source set's assets, not the `test` source set: Robolectric resolves assets from the MAIN variant's merged assets, so a test-only asset srcDir is never on the path"
  - "MigrationTestHelper's assets-based constructor is the only workable Android path — the `File` overload's argument is the database file, not a schema directory"
  - "Room table/column/mode names are `const` values in `ProgressPersistenceKeys`, interpolated into `@Entity`/`@Query` (constant folding survives KSP)"
  - "`DatabaseModule` is an `object` module; `PegSolitaireDatabase.DB_VERSION` is a companion const so the version is declared once"
  - "`NoOpGamesService.signIn()` returns `false` (declines) — the deliberate inverse of `NoOpAdGateway`'s auto-grant `true`"
  - "No `ProgressRepository` in this plan — the DAOs are the deliverable; the repository (per-field best merge, defensive blob decode) belongs to the plan that needs it"

patterns-established:
  - "Scoped persistence keys: `ProgressPersistenceKeys` joins `AppPreferences`/`SettingsPreferences` — one named constants object per persistence scope (QUAL-03)"
  - "Migration-safety gate: exportSchema + committed JSON + a Robolectric MigrationTestHelper test; adding a v2 is one `runMigrationsAndValidate` line"
  - "Platform seams are interface + `@Inject` no-op stub + `@Binds @Singleton` in `PlatformModule`; Slice 2 swaps only the binding"

requirements-completed: [PERS-01, PERS-03]

# Metrics
duration: 34min
completed: 2026-08-25
---

# Phase 5 Plan 01: Persistence Foundation Summary

**Room 2.8.4 wired via KSP with a migration-safe v1 database (committed schema JSON + a green Robolectric `MigrationTestHelper` spike), two entities/DAOs for per-level bests and per-mode resume snapshots, a Hilt `DatabaseModule`, and the `GamesService` no-op stub bound alongside `AdGateway`.**

## Performance

- **Duration:** ~34 min
- **Started:** 2026-08-25T18:52:00Z
- **Completed:** 2026-08-25T19:26:00Z
- **Tasks:** 3 (Task 1 pre-completed on the fork base; Tasks 2-3 executed here)
- **Files modified:** 16 (13 created, 3 modified)

## Accomplishments

- **The migration-safety gate is real, not aspirational.** `exportSchema = true` + the Room Gradle plugin write `app/schemas/com.app.pegsolitaire.data.db.PegSolitaireDatabase/1.json`, that file is committed, and `MigrationTest` opens it through `MigrationTestHelper` on the JVM. The day a v2 entity change lands, a forgotten `Migration` fails the build instead of silently wiping a player's stars.
- **RESEARCH Open-Q1 resolved** (see Deviations #1) — the JVM/Robolectric path works; the `androidTest` fallback was **not** needed.
- **Storage shapes that later plans just consume:** `LevelResultEntity` with the composite PK `(mode, levelId)` so Classic `"cross-33"` and Challenge `"33"` can never collide, and `ResumeSnapshotEntity` keyed by `mode` so Classic and Challenge snapshots coexist (D-03).
- **No destructive-migration escape hatch anywhere** — `grep -rn "fallbackToDestructiveMigration" app` returns nothing, including comments.
- **Both Slice-2 platform swap points now exist** (`AdGateway`, `GamesService`) with opposite stub semantics: ads grant, Play Games declines.

## Task Commits

1. **Task 1: Wire Room 2.8.4 deps + Gradle plugin + schema export** - `f391ac6` (feat) — *pre-completed on the fork base; not re-executed*
2. **Task 2: Room database, entities, DAOs, DatabaseModule + migration-test spike** - `52837c3` (test, RED) → `8331a58` (feat, GREEN)
3. **Task 3: GamesService no-op stub bound alongside AdGateway** - `a65f11a` (test, RED) → `eedc98f` (feat, GREEN)

No REFACTOR commits were needed — both GREEN implementations landed at their final shape.

## Files Created/Modified

**Room stack (created)**
- `app/src/main/java/com/app/pegsolitaire/data/ProgressPersistenceKeys.kt` — `DB_NAME`, table names, shared column names, and the `"classic"`/`"challenge"` mode literals (QUAL-03)
- `app/src/main/java/com/app/pegsolitaire/data/db/LevelResultEntity.kt` — per-level bests; composite PK `(mode, levelId)`; max stars / min time / max score are tracked independently (D-07)
- `app/src/main/java/com/app/pegsolitaire/data/db/LevelResultDao.kt` — `find` (suspend), `observe(mode)` (Flow, for the D-10 catalog star pips), `@Upsert`
- `app/src/main/java/com/app/pegsolitaire/data/db/ResumeSnapshotEntity.kt` — `@PrimaryKey mode`, `jsonBlob`, `updatedAt`; max two live rows (D-03)
- `app/src/main/java/com/app/pegsolitaire/data/db/ResumeDao.kt` — `get`, `observeModes` (Home Continue tiles), `@Upsert`, `clear(mode)` (D-05)
- `app/src/main/java/com/app/pegsolitaire/data/db/PegSolitaireDatabase.kt` — `@Database(version = 1, exportSchema = true)`, both DAOs, `DB_VERSION` companion const
- `app/src/main/java/com/app/pegsolitaire/di/DatabaseModule.kt` — `object` module: `@Provides @Singleton` database + `@Provides` DAO accessors; no destructive-migration fallback
- `app/schemas/com.app.pegsolitaire.data.db.PegSolitaireDatabase/1.json` — the committed migration contract

**Platform seam (created)**
- `app/src/main/java/com/app/pegsolitaire/platform/games/GamesService.kt` — `suspend fun signIn(): Boolean`, `fun submitScore(leaderboardId, score)`
- `app/src/main/java/com/app/pegsolitaire/platform/games/NoOpGamesService.kt` — `@Inject` stub returning `false` / `Unit`

**Tests (created)**
- `app/src/test/java/com/app/pegsolitaire/data/db/PegSolitaireDatabaseTest.kt` — 5 tests over an in-memory Room DB (Robolectric + Turbine): result round-trip, composite-PK upsert-not-duplicate, per-mode `observe` isolation, per-mode snapshot clear, `observeModes`
- `app/src/test/java/com/app/pegsolitaire/data/db/MigrationTest.kt` — opens the exported v1 schema via `MigrationTestHelper`
- `app/src/test/java/com/app/pegsolitaire/platform/games/NoOpGamesServiceTest.kt` — `signIn()` is `false`; `submitScore` does not throw

**Modified**
- `app/build.gradle.kts` — schema dir extracted to a `roomSchemaDir` val; `debug` source-set assets srcDir (replacing the non-functional `test` one); `mergeDebugAssets dependsOn copyRoomSchemas`
- `app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt` — added `@Binds @Singleton bindGamesService`
- `gradle/libs.versions.toml` — Room 2.8.4 version, four libraries, plugin entry (Task 1, base commit)

## Decisions Made

- **Schema assets go on the `debug` source set.** This is the narrowest source set Robolectric can actually see. Release APKs never carry the schema JSON; debug APKs carry ~1.5 KB of already-public schema definition.
- **Table/column names are interpolated `const` values** (`"SELECT * FROM $TABLE_LEVEL_RESULT WHERE $COLUMN_MODE = :mode …"`). Kotlin folds these before KSP sees them — verified by the generated `1.json`, which contains the literal `level_result` / `resume_snapshot` names. Renaming a column is now a single edit that Room re-validates across every query.
- **The per-field "best" merge is deliberately *not* in the DAO.** It has to report *which* fields improved to drive the "New best!" cue (D-09), so it belongs in a repository transaction. This plan ships the dumb store; `ProgressRepository` lands with the plan that consumes it.
- **`NoOpGamesService.signIn()` declines.** Auto-granting would let a future leaderboard screen render as though the player were connected to a service that does not exist yet.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Exported schema moved from the `test` source set's assets to `debug` (RESEARCH Open-Q1 / Pitfall 1 / A1)**
- **Found during:** Task 2 (migration-test spike)
- **Issue:** Task 1's wiring followed RESEARCH Pitfall 1's recommendation, `sourceSets.getByName("test").assets.srcDir("$projectDir/schemas")`. It does not work. AGP's generated `com/android/tools/test_config.properties` sets `android_merged_assets=build/intermediates/assets/debug/mergeDebugAssets` — i.e. Robolectric resolves assets from the **main variant's** merged assets, and there is no unit-test asset merge at all. `MigrationTest` failed with `FileNotFoundException: Cannot find the schema file in the assets folder … Missing file: com.app.pegsolitaire.data.db.PegSolitaireDatabase/1.json`.
- **Intermediate attempt (rejected):** Room 2.8.4's `MigrationTestHelper(instrumentation, file, driver, databaseClass, …)` overload looked like a file-directory escape hatch. It is not — decompiling `room-testing` 2.8.4 shows its `file` argument is the *database* file; the stack trace confirmed the same `AndroidMigrationTestHelper.loadSchema` assets path (`SQLiteDriverMigrationTestHelper.createDatabase → loadSchema`). **On Android, every `MigrationTestHelper` constructor loads schemas from assets.** (The `schemaDirectoryPath` constructor is KMP-only.)
- **Fix:** `app/build.gradle.kts` now puts `roomSchemaDir` on the `debug` source set's assets, and wires `mergeDebugAssets dependsOn copyRoomSchemas` so the asset merge cannot race the Room plugin's schema copy. `MigrationTest` uses the standard assets-based constructor.
- **Files modified:** `app/build.gradle.kts`, `app/src/test/java/com/app/pegsolitaire/data/db/MigrationTest.kt`
- **Verification:** `./gradlew clean :app:testDebugUnitTest --tests "*MigrationTest" …` → BUILD SUCCESSFUL from a clean build.
- **Committed in:** `8331a58` (Task 2 GREEN commit)
- **Note:** This supersedes Task 1's acceptance criterion "adds `$projectDir/schemas` to the `test` source set assets". The intent (schema reachable by the JVM migration test) is met; the mechanism the research proposed was wrong. **The `androidTest` fallback (RESEARCH A1) was not needed** — the migration test runs JVM-side as designed.

**2. [Rule 3 - Blocking] Recreated the gitignored `local.properties` inside the execution worktree**
- **Found during:** Task 2 (first Gradle invocation)
- **Issue:** `local.properties` is gitignored, so the fresh worktree had none and every Gradle command failed with "SDK location not found".
- **Fix:** Wrote `local.properties` with `sdk.dir=/Users/ermanergoz/Library/Android/sdk`.
- **Files modified:** `local.properties` (gitignored — intentionally **not** committed)
- **Verification:** Gradle configures and `./gradlew check` runs.
- **Committed in:** n/a (gitignored, worktree-local only)

**3. [Rule 2 - Missing Critical] Reworded the `DatabaseModule` KDoc to drop the literal `fallbackToDestructiveMigration` token**
- **Found during:** Task 2 (verification)
- **Issue:** The KDoc explaining *why* the destructive fallback is absent contained the literal token, which would make the plan's own phase-gate check (`grep -rn "fallbackToDestructiveMigration" app/` returns nothing) report a false positive forever — training the team to ignore that gate.
- **Fix:** Reworded to "the destructive-migration fallback"; the warning is preserved in full, the grep gate stays meaningful.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/di/DatabaseModule.kt`
- **Verification:** `grep -rn "fallbackToDestructiveMigration" app` → exit 1 (no matches).
- **Committed in:** `8331a58` (Task 2 GREEN commit)

---

**Total deviations:** 3 auto-fixed (2 blocking, 1 missing critical)
**Impact on plan:** All three were necessary to make the plan's own success criteria verifiable. No scope creep — no repository, DTO, mapper, or UI work was pulled forward.

## Issues Encountered

- **Robolectric asset resolution** was the only real obstacle; resolved as Deviation #1. Worth recording for future phases: **unit-test asset srcDirs are invisible to Robolectric on this AGP version.** Anything a Robolectric test must read from assets has to be on the `main` or `debug` source set.
- A `mergeDebugAssets` / `copyRoomSchemas` ordering hazard was pre-empted with an explicit `dependsOn`. It does not bite on a fresh clone (the schema JSON is committed), but it would have on the first build after a future schema change.

## TDD Gate Compliance

Both TDD tasks followed RED → GREEN with commits proving the sequence:
- Task 2: `52837c3` (`test`, compile-fails on missing types) → `8331a58` (`feat`)
- Task 3: `a65f11a` (`test`, compile-fails on missing types) → `eedc98f` (`feat`)

No test passed unexpectedly during RED. REFACTOR was skipped for both (no cleanup warranted).

## Verification

| Gate | Result |
|------|--------|
| `./gradlew clean :app:testDebugUnitTest --tests "*PegSolitaireDatabaseTest" --tests "*MigrationTest" --tests "*NoOpGamesServiceTest"` | BUILD SUCCESSFUL (8 tests) |
| `./gradlew check` (all unit tests + detekt + ktlint + dpSpLiteralCheck + lint, both modules) | BUILD SUCCESSFUL |
| `test -f app/schemas/*/1.json` | present and committed |
| `grep -rn "fallbackToDestructiveMigration" app` | no matches |

## Known Stubs

- `NoOpGamesService` — an **intentional** Slice-2 stub, exactly as the roadmap specifies. `signIn()` returns `false` and `submitScore` discards. Nothing in the app calls `GamesService` yet; the real `play-services-games-v2` implementation replaces only the `PlatformModule` binding in Slice 2. Not a gap in this plan's goal.

No unintentional stubs: every entity, DAO, and DI provider in this plan is fully implemented and exercised by tests.

## Threat Flags

None. The plan's threat register is fully addressed and no new security-relevant surface was introduced:

| Threat ID | Disposition | Status |
|-----------|-------------|--------|
| T-05-01 (migration data loss) | mitigate | Done — `exportSchema=true`, committed `1.json`, no destructive fallback, green `MigrationTest` |
| T-05-05 (DAO SQL injection) | mitigate | Done — every query binds `:params`; no string-concatenated SQL (Room verifies at compile time) |
| T-05-SC (Room package legitimacy) | accept | Unchanged — first-party AndroidX on Google Maven |

The database is app-private local storage with no network, auth, or PII surface. The `jsonBlob` column is untrusted-on-read (a corrupt file could yield invalid JSON), but nothing decodes it yet — **the defensive `try/catch` decode → "no resume" fail-safe (Security §V5) is a required control for whichever plan introduces the snapshot codec.**

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

**Ready to consume:**
- `LevelResultDao` / `ResumeDao` are injectable today (`DatabaseModule` provides both) — `ProgressRepository` can be written directly against them.
- `PegSolitaireDatabase` is injectable for `withTransaction { }`, which the per-field best merge (D-07) needs.
- `ProgressPersistenceKeys.MODE_CLASSIC` / `MODE_CHALLENGE` are the canonical mode literals for every later slice.

**Carry forward into later plans:**
1. **Any Room schema change requires** bumping `PegSolitaireDatabase.DB_VERSION`, adding `.addMigrations(...)` in `DatabaseModule`, committing the new `schemas/<db>/<n>.json`, and extending `MigrationTest` with `runMigrationsAndValidate`. The plumbing is done; the discipline is now enforceable.
2. **Defensive blob decode is still owed** — the resume snapshot codec must wrap `Json.decodeFromString` in `try/catch` and fail safe to "no resume" (Security §V5).
3. **Unaddressed by this plan** (unchanged from RESEARCH, still open for 05-02+): `ChallengeProgress` remains in-memory with `advanceToNext()` having zero callers, and `AppNavHost` still hardcodes Challenge entry to level 1 (Pitfall 7). Persisting `currentLevel` is necessary *and* insufficient — the seam must also be wired.

**Blockers:** None.

## Self-Check: PASSED

- All 13 created files verified present and tracked by git (`git ls-files`), including `app/schemas/com.app.pegsolitaire.data.db.PegSolitaireDatabase/1.json`.
- All 5 commit hashes referenced above verified present in `git log` (`52837c3`, `8331a58`, `a65f11a`, `eedc98f`, `d976af5`).
- Working tree clean; the only uncommitted artifact is the deliberately gitignored `local.properties`.

---
*Phase: 05-persistence-resume-settings*
*Completed: 2026-08-25*
