# Phase 12: Leaderboards & Achievements — Plan Outline

**Phase goal:** Player's progress becomes competitive and recognized — ranked on leaderboards, rewarded with achievements.
**Plans:** 8 · **Waves:** 5

**Ordering rationale (encoded in the waves below):**
- Wave 1 is the four independent, zero-overlap foundations: the Room v1→v2 migration (PGSA-06, must precede all achievement wiring per the ROADMAP note), the pure `:domain` achievement kernel, the `GamesService` seam reshape + stock UI + Settings rows, and the 27-key build-config ID plumbing.
- The telemetry → repository → submitter chain is genuinely serial: each link edits a file the previous link introduced (`Results` nav args → `ResultsViewModel` → `recordResult` signature → submitter hook). No stub/default telemetry is used to fake parallelism.
- Operator/doc work lands last, alone, because it is the only plan whose output is human-executed.

| Plan ID | Objective | Wave | Depends On | Requirements | Key files |
|---------|-----------|------|------------|--------------|-----------|
| 12-01 | Room v1→v2 migration: add the `play_session` telemetry table as a pure `CREATE TABLE` (entity + DAO insert only, `DB_VERSION = 2`, registered `MIGRATION_1_2`, committed `2.json`), proven progress-preserving and proven *registered* by two `MigrationTest` cases (D-06). | 1 | — | PGSA-06, PGSA-02 | `app/…/data/db/PlaySessionEntity.kt`, `app/…/data/db/PlaySessionDao.kt`, `app/…/data/db/Migrations.kt`, `app/…/data/db/PegSolitaireDatabase.kt`, `app/…/di/DatabaseModule.kt`, `app/…/data/db/ProgressPersistenceKeys.kt`, `app/schemas/…/2.json`, `app/src/test/…/data/db/MigrationTest.kt` |
| 12-02 | Pure-Kotlin achievement kernel in `:domain`: `SolveTelemetry`, the 20-value `AchievementId` enum, `AchievementThresholds` (10/50/100, 25/100/250, 60 s, 5 boards, 3 stars), `ProgressAggregate`, and `AchievementRules.evaluate` returning unlocks + absolute steps with zero-valued steps omitted. Zero Android, zero PGS strings. | 1 | — | PGSA-01, PGSA-02, PGSA-03 | `domain/…/telemetry/SolveTelemetry.kt`, `domain/…/achievement/AchievementId.kt`, `domain/…/achievement/AchievementThresholds.kt`, `domain/…/achievement/ProgressAggregate.kt`, `domain/…/achievement/AchievementRules.kt`, `domain/src/test/…/achievement/AchievementRulesTest.kt` |
| 12-03 | Reshape the `GamesService` seam for observable failure (`suspend … : Boolean` submissions, `+ setAchievementSteps`, `− incrementAchievement`, no-arg `showLeaderboards()`), give `PlayGamesService` real `*Immediate`/`Task.await()` bodies plus the two stock-UI Intents via `startActivityForResult`, and surface the two signed-in-only Settings rows (D-14/D-15). No popup API exists in v2, so D-16 is zero code. | 1 | — | PGSL-04, PGSL-06, PGSA-04, PGSA-05 | `app/…/platform/games/GamesService.kt`, `PlayGamesService.kt`, `NoOpGamesService.kt`, `GamesConstants.kt`, `app/src/test/…/platform/games/FakeGamesService.kt`, `app/…/ui/settings/SettingsViewModel.kt`, `SettingsScreen.kt`, `app/src/main/res/values/strings.xml`, `app/src/test/…/ui/settings/SettingsPlayGamesTest.kt` |
| 12-04 | Build-config plumbing for the 7 leaderboard + 20 achievement IDs (D-17): sibling `GamesIdResolution.kt` in `build-logic` (env → `local.properties` → `UNCONFIGURED-<KEY>` on debug, fail-fast on release), 27 looped `buildConfigField`s, `verifyReleaseGamesIds`, documented template keys, and `GamesIds.Leaderboards` accessors + `forClassic()`. | 2 | 12-03 | PGSL-01, PGSL-02, PGSL-03, PGSL-06, PGSA-05 | `build-logic/src/main/kotlin/GamesIdResolution.kt`, `build-logic/src/test/kotlin/GamesIdResolutionTest.kt`, `app/build.gradle.kts`, `local.properties.template`, `app/…/platform/games/GamesIds.kt`, `app/src/test/…/BuildConfigIdsTest.kt` |
| 12-05 | Capture per-session telemetry in gameplay (D-09): count every hint and undo on the assist funnel's success path, reset on restart/new level, persist the counters as nullable snapshot fields defaulting to 0, compute `lastPegOnTarget` from the finishing board against the spec target, and carry all three to Results as defaulted, clamped nav args. | 2 | 12-02 | PGSA-02 | `app/…/ui/game/GameViewModel.kt`, `app/…/data/resume/ResumeSnapshotDto.kt`, `app/…/data/resume/SnapshotMapper.kt`, `app/…/navigation/Routes.kt`, `app/src/test/…/ui/game/GameViewModelTest.kt`, `app/src/test/…/data/resume/SnapshotMapperTest.kt` |
| 12-06 | Persist and aggregate: `recordResult` takes `SolveTelemetry` and writes `level_result` + `play_session` in one transaction (D-08), and two SQL projection queries produce `ProgressAggregate` (ladder `MAX(CAST(...))`, `SUM(bestScore)` as `Long?`, star total, Grand tour / Perfectionist counts, telemetry `EXISTS` predicates). `ResultsViewModel` passes telemetry from its args. | 3 | 12-01, 12-02, 12-05 | PGSL-01, PGSL-02, PGSL-03, PGSA-02, PGSA-03 | `app/…/data/ProgressRepository.kt`, `app/…/data/db/LevelResultDao.kt`, `app/…/data/db/PlaySessionDao.kt`, `app/…/ui/results/ResultsViewModel.kt`, `app/src/test/…/data/ProgressAggregateTest.kt`, `app/src/test/…/data/ProgressRepositoryTest.kt` |
| 12-07 | `ProgressSubmitter` `@Singleton` (D-13): dirty-flag store, mutex-serialised pass that reads aggregates → `AchievementRules` → `GamesService`, flag set on signed-out or any failure and cleared only on all-success (D-10/D-11), session observer started explicitly from `MainActivity`, `GamesIds.of(AchievementId)` mapping, and the fire-and-forget hook inside `recordSolvedPlayOnce()`. | 4 | 12-02, 12-03, 12-04, 12-06 | PGSL-01, PGSL-02, PGSL-03, PGSL-04, PGSL-05, PGSA-02, PGSA-03 | `app/…/data/ProgressSubmitter.kt`, `app/…/data/PlayGamesPreferences.kt`, `app/…/data/PlayGamesOptInRepository.kt`, `app/…/platform/games/GamesIds.kt`, `app/…/ui/results/ResultsViewModel.kt`, `app/…/MainActivity.kt`, `app/src/test/…/data/ProgressSubmitterTest.kt`, `app/src/test/…/ui/results/ResultsViewModelTest.kt` |
| 12-08 | Operator-facing closure: ROADMAP External Prerequisites icon row 10–12 → 20 (and the Phase 12 plan list), a Play Console checklist naming all 7 leaderboards (Numeric, 0 decimals, larger-is-better — immutable after publication) and all 20 achievements with names, descriptions and point values summing ≤ 2,000 in multiples of 5, ID-to-CI-secret recording, and the `11-HUMAN-UAT.md` device items for popup / stock UI / real submissions / minified-release R8 check. | 5 | 12-01, 12-02, 12-03, 12-04, 12-05, 12-06, 12-07 | PGSA-01, PGSA-04, PGSL-06, PGSA-05 | `.planning/ROADMAP.md`, `.planning/phases/12-leaderboards-achievements/12-OPERATOR-CHECKLIST.md`, `.planning/phases/11-play-games-session/11-HUMAN-UAT.md` |

## Decision coverage

Every locked decision is cited in the `must_haves` of at least one plan.

- **D-01** (hint-free / undo-free in either mode; center finish = English-33 `(3,3)` only) → 12-02 (rules + thresholds), 12-05 (`lastPegOnTarget` against the spec target), 12-06 (SQL predicates)
- **D-02** (four first-hour early wins: first solve, first 3-star, Challenge 5, first per-board unlock) → 12-02, 12-08
- **D-03** (six tiered incremental achievements, absolute steps, SQL-derived) → 12-02, 12-06, 12-07
- **D-04** (Grand tour, Perfectionist, five per-board unlocks, Speed solve < 60 s) → 12-02, 12-06
- **D-05** (20 visible achievements ⇒ 20 icons; ROADMAP row updated) → 12-02 (20-value enum), 12-08 (icon row, names/descriptions/points)
- **D-06** (`play_session` table; pure `CREATE TABLE` migration; `MigrationTest` + exported `2.json`) → 12-01
- **D-07** (cumulative Challenge score = `SUM(bestScore)` where `mode = challenge`) → 12-06, 12-07
- **D-08** (`SolveTelemetry` rides alongside `ScoreResult`; `recordResult` writes both tables in one transaction) → 12-02, 12-05, 12-06
- **D-09** (every hint and undo counts; counters persist in the resume snapshot; restart/new level resets) → 12-05
- **D-10** (dirty-marker sync: flag on signed-out or failed submission, resubmit on `SignedIn`, cleared only on success) → 12-07
- **D-11** (backfill covers leaderboards *and* achievements from one `AchievementRules` pass) → 12-02, 12-07
- **D-12** (`setAchievementSteps` on the seam, absolute; `incrementAchievement` removed) → 12-03, 12-07
- **D-13** (one data-layer `ProgressSubmitter` `@Singleton`; nothing in `ui/` names an ID; fire-and-forget) → 12-07
- **D-14** (two Settings rows only — "Leaderboards" opens the all-leaderboards picker, "Achievements") → 12-03
- **D-15** (both rows hidden while signed out) → 12-03
- **D-16** (stock unlock popup untouched, default position — v2 exposes no popup API, so zero code) → 12-03, 12-08 (device verification)
- **D-17** (27 IDs from `local.properties`/CI secrets → `BuildConfig` → `GamesIds`, release fails fast) → 12-04, 12-07 (`GamesIds.of`)

## OUTLINE COMPLETE
