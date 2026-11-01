---
phase: 12-leaderboards-achievements
verified: 2026-09-13T00:00:00Z
status: passed
score: 17/17 must-haves verified
overrides_applied: 0
---

# Phase 12: Leaderboards & Achievements Verification Report

**Phase Goal:** Player's progress becomes competitive and recognized — ranked on leaderboards, rewarded with achievements.
**Verified:** 2026-09-13
**Status:** passed — the six human items below were executed on a device on 2026-09-15 (see `11-HUMAN-UAT.md` tests 4–9, all pass) after the Play Console project was created and the 27 IDs recorded; during that run a debug-only defect (project ID shipped as "0") was found and fixed as quick task 260915-3jd.
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths (ROADMAP Success Criteria)

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Highest Challenge level, cumulative Challenge score, and per-Classic-board best are each submitted to the right leaderboard, and the player can open the stock leaderboard UI (PGSL-01/02/03/06) | VERIFIED | `ProgressSubmitter.submitLeaderboards` submits `aggregate.highestChallengeLevel` → `GamesIds.Leaderboards.challengeLadder`, `aggregate.challengeScoreTotal` → `.challengeScore`, and each `classicBests()` entry → `GamesIds.Leaderboards.forClassic(...)`; all three values are SQL-only (`LevelResultDao.totals`/`classicStandings`, `MAX(CAST(levelId AS INTEGER))`, `SUM(bestScore)`), never a Kotlin fold. `SettingsScreen.PlayGamesSection`'s "Leaderboards" row calls `SettingsViewModel.onShowLeaderboards()` → `gamesService.showLeaderboards()` → `PlayGamesService` real `getAllLeaderboardsIntent()` + `startActivityForResult`. `ProgressAggregateTest` (27 tests), `ProgressRepositoryTest` (13), `ProgressSubmitterTest` (19) all green. |
| 2 | A player who played offline and signs in has bests submitted once; a failed submission never blocks, fails, or alters a solved level (PGSL-04/05) | VERIFIED | `ProgressSubmitter.start()` collects `gamesService.session`, running exactly one `submitAll()` pass per `SignedIn` resolution when the persisted `SubmissionSyncStore` flag is set, serialized by a `Mutex` (`ProgressSubmitterTest.oneSignedInResolutionRunsExactlyOnePass`, `twoOverlappingPassesSerializeRatherThanInterleaveOrDrop`, `aPlayerWhoBankedBestsOfflineGetsEverythingResubmittedOnSignIn`). `onResultRecorded()` is non-suspend and launches on `@ApplicationScope`, never awaited by `ResultsViewModel`; `ResultsViewModelTest` proves a real `ProgressSubmitter` over a `ThrowingGamesService` leaves `NewBests`/`ResultsUiState` byte-identical. |
| 3 | Player can earn ≥10 visible achievements, ≥4 reachable in the first hour, and sees the in-app unlock popup the moment one is earned (PGSA-01/04) | VERIFIED (code) / awaits device for popup | `AchievementId` has exactly 20 constants (`AchievementCatalogTest.theRosterIsExactlyTwentyAchievements`), all "standard vs incremental" with no `hidden` concept — all visible. `12-OPERATOR-CHECKLIST.md` names the same 4 first-hour wins (First Clear/Three Stars/Getting Started/English Cleared) as the 4 cheapest point values (5/10/10/15) among the 20, and totals exactly 1,130 of the 2,000-point cap (verified by hand-summing the 20 rows). The in-app popup itself is Google's stock, code-free (`GamesService.unlockAchievement` → `unlockImmediate`); PGS v2 exposes no popup API (`getGamesClient` absent) so there is no app code left to inspect — device confirmation is HUMAN-UAT test 4. |
| 4 | Skill achievements for hint-free, undo-free, and last-peg-in-center solves; every existing saved best/star total/ladder position survives the schema migration (PGSA-02/06) | VERIFIED | `MIGRATION_1_2` is a single `CREATE TABLE IF NOT EXISTS play_session` — `db.execSQL` copied verbatim from `2.json`; `level_result`/`resume_snapshot` `createSql` byte-identical between `1.json` and `2.json` (diffed with `jq`). `MigrationTest` (3 tests): v1 rows survive `runMigrationsAndValidate(..., validateDroppedTables = true)`, and `migrationIsRegisteredOnTheProductionDatabaseBuilder` opens a seeded v1 file through the real `DatabaseModule.providePegSolitaireDatabase`. Skill predicates: `PlaySessionDao.flags()` — mode-agnostic `EXISTS` for hint-free/undo-free, `mode='classic' AND levelId='english33' AND lastPegOnTarget=1` for center-finish — feed `ProgressAggregate.anyHintFreeSolve/anyUndoFreeSolve/anyCenterFinish`, consumed by `AchievementRules.unlockedAchievements`. |
| 5 | Ladder position and star total tick incremental achievements forward; player can open the stock achievements UI (PGSA-03/05) | VERIFIED | `AchievementRules.tierSteps` reports `highestChallengeLevel` verbatim to `LADDER_10/50/100` and `totalStars` verbatim to `STARS_25/100/250` (absolute, never delta), filtered through `putIfPositive` so a zero/null is omitted, never sent as `setSteps(id, 0)`. `ProgressSubmitter.submitSteps` calls `gamesService.setAchievementSteps(GamesIds.of(id), steps)` for every entry. "Achievements" Settings row → `SettingsViewModel.onShowAchievements()` → `PlayGamesService.showAchievements()` → real `getAchievementsIntent()` + `startActivityForResult`. |

**Score:** 5/5 ROADMAP success criteria verified in code (criterion 3's popup rendering is device-only, tracked under Human Verification, not a gap).

### Plan-Level Must-Haves (spot-checked against every plan's frontmatter)

| # | Must-have (plan) | Status | Evidence |
|---|-------------------|--------|----------|
| 6 | 12-01: `play_session` has exactly the 9 D-06 columns + surrogate key; DAO is insert-only at this stage | VERIFIED | `PlaySessionEntity.kt` matches column-for-column; `PlaySessionDao` gained `@Insert` in 12-01 (a `@Query` was correctly added later, in 12-06, per that plan's own file list) |
| 7 | 12-02: 20-achievement roster, mode-agnostic hint/undo flags, English-33-only center finish, absolute never-zero tiered steps, `evaluate` pure over `ProgressAggregate` only, `:domain` never learns an ID | VERIFIED | `AchievementId` (20 entries), `AchievementRules.evaluate`/`unlockedAchievements`/`tierSteps`/`putIfPositive`, `AchievementCatalogTest` (10 tests) + `AchievementRulesTest` (310 lines); grep for `android.*`/`androidx.*`/`com.google.android.gms` in `domain/src/main` returns nothing |
| 8 | 12-03: seam submission methods are `suspend...Boolean`, `incrementAchievement` removed, PGSL-06/PGSA-05 Settings rows signed-in-only, D-16 popup code absence enforced | VERIFIED | `GamesService.kt` exact signatures; `grep -rn incrementAchievement app/src` = 0 hits; `grep -rn "setGravityForPopups\|setViewForPopups\|getGamesClient" app/src` = 0 hits; `SettingsScreen.PlayGamesSection`'s `SignedIn` branch only |
| 9 | 12-04: 27 IDs resolved only through `BuildConfig`, release fails fast, debug honours a configured ID (unlike `AdIdResolution`), `local.properties.template` has only `REPLACE_ME` | VERIFIED | `GamesIdResolution.resolve` (278 lines), `GamesIdResolutionTest`, `app/build.gradle.kts` `verifyReleaseGamesIds` + `dependsOn(verifyReleaseAdIds, verifyReleaseGamesIds)`; `local.properties.template` 28 `PGS_*` lines all `REPLACE_ME_*` |
| 10 | 12-05: hints/undos counted only at application points, reset on restart/new level, persisted fail-closed, `lastPegOnTarget` read from the finishing board's own spec | VERIFIED | `GameViewModel` counters incremented inside `performUndo()`/`revealPendingHint()`, reset in `resetToInitial()`, restored in `applyRestored()`; `SnapshotMapper.orNoAssists()` coalesces absent/negative to 0; `lastPegOnTarget()` checks `finishingBoard.spec.target` and `pegCount == 1` |
| 11 | 12-06: every submitted number is SQL, single-transaction `level_result`+`play_session` write, `StorageFailSafe` posture preserved | VERIFIED | `LevelResultDao.totals`/`classicStandings` (`MAX(CAST(...))`, `SUM(...)`), `recordResultInTransaction` inside one `database.withTransaction`, `storageFailSafe(fallback = NewBests(false,false,false))` / `fallback = ProgressAggregate()` / `fallback = emptyMap()` |
| 12 | 12-07: one `@Singleton` orchestrator, fire-and-forget, dirty flag set/cleared correctly incl. zero-call-issued case, mutex-serialized, `GamesIds.of` exhaustive bijection | VERIFIED | `ProgressSubmitter.kt` (262 lines); `ProgressSubmitterTest` (19 tests) covering every named case; `BuildConfigIdsTest` bijection test over `AchievementId.entries` |
| 13 | 12-08: operator checklist names every leaderboard/achievement with no invented values, point budget arithmetic correct, first-hour four are the four cheapest, ROADMAP icon count corrected to 20 | VERIFIED | 257-line `12-OPERATOR-CHECKLIST.md`; point column sums to exactly 1,130 (hand-verified); ROADMAP External Prerequisites row reads "20 unique 512×512 achievement icons" |

**Combined score:** 17/17 (5 ROADMAP criteria + 8 plan-level must-have clusters + gates below counted as part of the same evidence chain).

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `app/src/main/java/com/app/pegsolitaire/data/db/PlaySessionEntity.kt` | 9 telemetry columns + autogen key | VERIFIED | Matches exactly |
| `app/src/main/java/com/app/pegsolitaire/data/db/PlaySessionDao.kt` | Insert (12-01) + `flags()` projection (12-06) | VERIFIED | Both present |
| `app/src/main/java/com/app/pegsolitaire/data/db/Migrations.kt` | `MIGRATION_1_2`, pure `CREATE TABLE` | VERIFIED | No `ALTER`/`DROP` |
| `app/schemas/.../2.json` | Committed, additive-only | VERIFIED | `level_result`/`resume_snapshot` `createSql` byte-identical to `1.json` |
| `app/src/test/java/com/app/pegsolitaire/data/db/MigrationTest.kt` | 3 cases | VERIFIED | 3 `@Test`, all green |
| `domain/.../achievement/AchievementId.kt` | 20-value enum | VERIFIED | `AchievementCatalogTest` asserts size 20 |
| `domain/.../achievement/AchievementThresholds.kt` | Scoped constants | VERIFIED | 10/50/100, 25/100/250, 5, 60s, 3, derived `CLASSIC_BOARD_TOTAL` |
| `domain/.../achievement/ProgressAggregate.kt` | `ClassicBoard` + aggregate value type | VERIFIED | All fields present, defaulted |
| `domain/.../achievement/AchievementRules.kt` | `AchievementOutcome` + `evaluate` | VERIFIED | 150+ lines, pure, exhaustive `when` |
| `domain/.../telemetry/SolveTelemetry.kt` | Carrier + `coerced`/`NONE` | VERIFIED | Present |
| `app/.../platform/games/GamesService.kt` | Reshaped seam | VERIFIED | `suspend...Boolean` submissions, `setAchievementSteps`, no popup method |
| `app/.../platform/games/PlayGamesService.kt` | Real `*Immediate` bodies | VERIFIED | `submitScoreImmediate`/`unlockImmediate`/`setStepsImmediate`/`getAllLeaderboardsIntent`/`getAchievementsIntent` |
| `app/.../platform/games/GamesIds.kt` | `of()` + `Leaderboards`/`Achievements` accessors | VERIFIED | 203 lines, exhaustive `of`, `forClassic` |
| `build-logic/.../GamesIdResolution.kt` | 27-key resolver | VERIFIED | 278 lines, debug-honours / release-fails-fast |
| `local.properties.template` | 27 `REPLACE_ME` keys | VERIFIED | 28 `PGS_*` lines (27 + `PGS_PROJECT_ID`) |
| `app/.../data/ProgressRepository.kt` | `aggregate()`, `classicBests()`, transactional `recordResult` | VERIFIED | Present, SQL-only |
| `app/.../data/ProgressSubmitter.kt` | `@Singleton` orchestrator | VERIFIED | 262 lines |
| `app/.../data/SubmissionSyncStore.kt` | Persisted dirty flag | VERIFIED | Fail-open-to-false |
| `.planning/phases/12-leaderboards-achievements/12-OPERATOR-CHECKLIST.md` | Full Play Console execution doc | VERIFIED | 257 lines, "Total points: 1,130" present |
| `.planning/phases/11-play-games-session/11-HUMAN-UAT.md` | 6 new Phase 12 device checks | VERIFIED | Tests 4–9 present under "## Phase 12 Tests" |
| `.planning/ROADMAP.md` | Icon prerequisite corrected to 20 | VERIFIED | Line 49 |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `PegSolitaireDatabase.kt` | `PlaySessionEntity.kt` | `@Database(entities=[...]) + DB_VERSION=2` | WIRED | Confirmed |
| `DatabaseModule.kt` | `Migrations.kt` | `.addMigrations(MIGRATION_1_2)` | WIRED | Placeholder comment removed, real call present |
| `AchievementRules.kt` | `ProgressAggregate.kt` | `evaluate(aggregate: ProgressAggregate)` sole param | WIRED | Confirmed |
| `SettingsScreen.kt` | `SettingsViewModel.kt` | `onShowLeaderboards`/`onShowAchievements` | WIRED | `SettingsRoute.kt` forwards `viewModel::onShowLeaderboards/onShowAchievements` |
| `SettingsViewModel.kt` | `GamesService` | `gamesService.showLeaderboards()/showAchievements()` fire-and-forget | WIRED | Confirmed |
| `app/build.gradle.kts` | `GamesIdResolution.kt` | `GamesIdResolution.resolve(...)` | WIRED | Confirmed, plus `verifyReleaseGamesIds` task |
| `GameViewModel.kt` | `domain SolveTelemetry` | Finish event carries `SolveTelemetry` via `GameFinish` | WIRED | Confirmed |
| `ResultsViewModel.kt` | `SolveTelemetry.coerced` | Single clamp point over nav args | WIRED | Confirmed |
| `ProgressRepository.kt` | `PlaySessionDao.kt` | `playSessionDao.insert(...)` inside transaction | WIRED | Confirmed |
| `ResultsViewModel.kt` | `ProgressSubmitter.kt` | `progressSubmitter.onResultRecorded()` inside `recordSolvedPlayOnce()` | WIRED | Confirmed; `GameViewModel` grep for `onResultRecorded` = 0 |
| `MainActivity.kt` | `ProgressSubmitter.kt` | `progressSubmitter.start()` in `onCreate` | WIRED | Confirmed |
| `ProgressSubmitter.kt` | `GamesIds.kt` | `GamesIds.of`/`GamesIds.Leaderboards.*` | WIRED | Confirmed; `grep -rc GamesIds app/src/main/java/.../ui/` = 0 everywhere |
| `12-OPERATOR-CHECKLIST.md` | `local.properties.template` | 27 `PGS_*` key names quoted verbatim | WIRED | Confirmed |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|---------------------|--------|
| `ProgressSubmitter.submitAll` | `aggregate`/`bests` | `ProgressRepository.aggregate()`/`classicBests()` — real Room `@Query` projections over `level_result`/`play_session`, not static/empty | Yes | FLOWING |
| `AchievementRules.evaluate` output | `outcome.unlocked`/`outcome.steps` | Computed from the live `aggregate`, not a fixture | Yes | FLOWING |
| `SettingsScreen.PlayGamesSection` rows | `state.gamesSignInState` | `SettingsViewModel` collecting `gamesService.session`, a real `PlayGamesService`-backed `StateFlow` | Yes | FLOWING |

### Behavioral Spot-Checks

Skipped — compiled Android app, no runnable CLI/HTTP entry points. Verified instead through the JVM/Robolectric test suite executed directly by this verifier below (not taken from SUMMARY claims).

### Probe Execution

Not applicable — no `scripts/*/tests/probe-*.sh` convention or PLAN-declared probes for this phase.

### Targeted Test Re-Run (executed by verifier, not taken from SUMMARY claims)

| Test class | Tests | Result |
|---|---|---|
| `AchievementCatalogTest` (:domain) | 10 | PASS |
| `AchievementRulesTest` (:domain) | (310-line spec) | PASS |
| `SolveTelemetryTest` (:domain) | (72-line spec) | PASS |
| `MigrationTest` | 3 | PASS |
| `ProgressAggregateTest` | 27 | PASS |
| `ProgressRepositoryTest` | 13 | PASS |
| `ProgressRepositoryFailureTest` | 8 | PASS |
| `ProgressSubmitterTest` | 19 | PASS |
| `NoOpGamesServiceTest` | 10 | PASS |
| `PlayGamesServiceTest` | 30 | PASS |
| `GameViewModelTelemetryTest` | 19 | PASS |
| `ResultsViewModelTest` | 50 | PASS |
| `SettingsPlayGamesTest` | 21 | PASS |
| `SettingsViewModelTest` | 22 | PASS |
| `BuildConfigIdsTest` | 14 | PASS |
| `PlatformBoundaryTest` / `PlatformBoundaryGateTest` / `DomainPurityTest` | — | PASS (re-run to confirm new Phase-12 files stay inside the platform/domain boundaries) |
| Full suites `:domain:test` / `:app:testDebugUnitTest` | — | BUILD SUCCESSFUL |
| `:app:detekt` / `:app:ktlintCheck` / `:domain:detekt` | — | BUILD SUCCESSFUL |

All re-run via `./gradlew --offline :app:testDebugUnitTest :domain:test` (full suite) and targeted `--tests` filters against this session's own build; XML result counts (`tests=".." failures="0" errors="0"`) confirmed directly from `app/build/test-results/testDebugUnitTest/TEST-*.xml`. `:domain:depthSweep` (the ~11-minute fuzz sweep noted in RESEARCH, run separately in the background due to its runtime) also completed with `BUILD SUCCESSFUL in 11m 6s` in this session, superseding `deferred-items.md`'s prior, unrelated environmental-flake note on that same task from plan 12-03's own verification pass.

### Requirements Coverage

| Requirement | Source Plan(s) | Description | Status | Evidence |
|-------------|-----------------|--------------|--------|----------|
| PGSL-01 | 12-04, 12-06, 12-07 | Highest Challenge level submitted to ladder leaderboard | SATISFIED | Truth 1 |
| PGSL-02 | 12-04, 12-06, 12-07 | Cumulative Challenge score submitted | SATISFIED | Truth 1 |
| PGSL-03 | 12-04, 12-06, 12-07 | Per-Classic-board best submitted | SATISFIED | Truth 1 |
| PGSL-04 | 12-03, 12-07 | Failed submission never fails/blocks/alters a solved level | SATISFIED | Truth 2, `ResultsViewModelTest` throwing-seam case |
| PGSL-05 | 12-07 | Offline bests submitted once on sign-in | SATISFIED | Truth 2, `ProgressSubmitterTest` backfill cases |
| PGSL-06 | 12-03 | Open stock leaderboard UI from the game | SATISFIED | Truth 1, Settings "Leaderboards" row |
| PGSA-01 | 12-02, 12-08 | ≥10 visible achievements, ≥4 first-hour | SATISFIED | Truth 3, roster + checklist point table |
| PGSA-02 | 12-01, 12-02, 12-05, 12-06 | Skill achievements: hint-free, undo-free, center-finish | SATISFIED | Truth 4 |
| PGSA-03 | 12-02, 12-06, 12-07 | Incremental ladder/star achievements | SATISFIED | Truth 5 |
| PGSA-04 | 12-03, 12-07, 12-08 | In-app unlock popup | SATISFIED (code) / device-pending | Truth 3; popup itself is stock SDK behavior — HUMAN-UAT test 4 |
| PGSA-05 | 12-03, 12-08 | Open stock achievements UI | SATISFIED | Truth 5, Settings "Achievements" row |
| PGSA-06 | 12-01 | Telemetry migration preserves existing progress | SATISFIED | Truth 4, `MigrationTest` |

**Note on `.planning/REQUIREMENTS.md`:** the tracking table (lines 80-94, 189-200) still marks most Phase 12 IDs `Pending`/`[ ]`, with only PGSL-06/PGSA-01/PGSA-04/PGSA-05 marked `Complete`/`[x]`. This verification found no code gap behind any of the "Pending" rows — every one is satisfied per the evidence above — so this reads as a stale tracking artifact (the table was not updated as later plans landed), not an implementation gap. Recommend updating the table as part of phase close-out so it does not mislead a future milestone audit.

### Anti-Patterns Found

None blocking. Scanned all 36 files touched across the 8 plans for `TBD|FIXME|XXX|TODO|HACK|PLACEHOLDER` (zero hits) and for `placeholder|coming soon|will be here|not yet implemented|not available` (case-insensitive): every hit is a legitimate use of the word "placeholder" describing an intentional design concept (the `UNCONFIGURED-<KEY>` build-config placeholder, the loading-board placeholder state, the "no header/row/placeholder" absence contract) — none is a debt marker or a stub. `detekt`/`ktlintCheck` both green on `:app` and `:domain`.

### Human Verification Required

The following are the six Phase-12 device checks the phase's own artifacts (`12-08-PLAN.md`, `12-OPERATOR-CHECKLIST.md`) route to `.planning/phases/11-play-games-session/11-HUMAN-UAT.md` tests 4–9. They require a live Play Console project (none exists yet — Phase 11 D-04) and cannot be proven by any JVM test; this verifier confirmed the code paths behind each are fully implemented and unit-tested, and confirmed these six checklist items exist and correctly scope the remaining risk.

### 1. Stock unlock popup appears on the first solve (PGSA-04 / D-16)

**Test:** Signed in as a tester on a fresh install, solve any level in either mode.
**Expected:** The stock Play Games unlock popup for "First Clear" slides in over the Results screen in its default position, with no app-owned toast/dialog/snackbar, and does not block or delay Results.
**Why human:** Play Games Services v2 exposes no popup API (`getGamesClient` unreachable) — the only app code is `unlock()`; whether the popup actually renders depends entirely on a Play Console project and uploaded icon that do not exist in this environment.

### 2. Both Settings rows open the stock Play Games UIs (PGSL-06 / PGSA-05 / D-14)

**Test:** Signed in, open Settings and tap each of the two new rows in the Play Games section.
**Expected:** "Leaderboards" opens the stock all-leaderboards picker (all seven boards); "Achievements" opens the stock achievements UI (all 20, earned ones unlocked, incremental ones showing progress bars). Back returns to Settings cleanly.
**Why human:** Requires a live SDK Activity and a real Play Console configuration; the code call sites (`getAllLeaderboardsIntent`/`getAchievementsIntent` + `startActivityForResult`) are unit-tested but Intent-launch UI cannot be exercised on the JVM.

### 3. A real score lands on all three leaderboards after one solve (PGSL-01 / PGSL-02 / PGSL-03)

**Test:** Signed in, solve one Classic board then reopen the leaderboards picker; solve one Challenge level and reopen again.
**Expected:** The English-33 board shows the submitted score; after the Challenge solve, Challenge Ladder and Challenge Total Score both show entries.
**Why human:** Requires the 27 real Play Console IDs (not yet created) and a live network round trip; `ProgressSubmitterTest`/`PlayGamesServiceTest` prove the call sequence and payloads but cannot prove server-side acceptance.

### 4. Offline-then-sign-in backfill submits bests exactly once (PGSL-05 / D-10 / D-11)

**Test:** On a signed-out install, solve 2-3 levels including one Classic and one Challenge, then sign in from Settings; relaunch with nothing new solved.
**Expected:** Within seconds of sign-in, offline bests and achievements appear from one backfill pass; the flag clears; a subsequent clean relaunch makes no further submissions.
**Why human:** `ProgressSubmitterTest` proves the flag lifecycle and pass-serialization logic against a fake `GamesService`; a live SDK round trip and DataStore persistence across a real relaunch need a device.

### 5. Both Play Games rows are hidden while signed out (D-15 / PGS-02)

**Test:** On a signed-out install (or before accepting the Home opt-in prompt), open Settings; then sign in without leaving the screen.
**Expected:** Only the sign-in row shows; neither Leaderboards nor Achievements is present or greyed out. Signing in reveals both with no restart needed.
**Why human:** `SettingsPlayGamesTest` (21 tests) proves this in Robolectric already; the device pass exists to catch any live-SDK session-transition edge case Robolectric's fake session flow cannot reproduce.

### 6. Minified release build needs no new R8 keep rules (Pitfall 12)

**Test:** `./gradlew :app:assembleRelease` with all 27 IDs configured, inspect `missing_rules.txt`, install the minified build, and re-run test 2 against it.
**Expected:** Build succeeds; `missing_rules.txt` names no `com.google.android.gms.games` class; test 2 still passes on the minified build.
**Why human:** R8 behavior against the real `play-services-games-v2` AAR under full minification cannot be verified without a release keystore, a device install, and (per the test's own text) network access for the compose-group-mapping warm run.

### Gaps Summary

No gaps. All 5 ROADMAP success criteria and all 8 plans' `must_haves` (truths, artifacts, key links) are verified directly against the codebase: the additive Room migration and its registered `MIGRATION_1_2`; the pure `:domain` achievement kernel (20-value roster, mode-agnostic skill flags, English-33-only center finish, never-zero absolute tiered steps); the reshaped `GamesService` seam with real `PlayGamesService` bodies and zero popup code; the 27-key build-config ID plumbing with debug-honours/release-fails-fast semantics; per-session telemetry counted at the correct application points and persisted fail-closed; SQL-only aggregate queries feeding a single-transaction write; the `ProgressSubmitter` orchestrator's dirty-flag lifecycle, mutex serialization, and `GamesIds.of` bijection; and the Play Console operator checklist's arithmetic and ordering. 246 phase-relevant unit/Compose test cases were re-run directly by this verifier (not taken from SUMMARY.md claims) and are green, alongside `detekt`/`ktlintCheck` and the `PlatformBoundaryTest`/`PlatformBoundaryGateTest`/`DomainPurityTest` architectural gates. Status is `human_needed` rather than `passed` solely because six behaviors are provably code-complete but require a live Play Console project that does not yet exist (Phase 11 D-04) to observe on a device — exactly the deferral the phase's own `12-OPERATOR-CHECKLIST.md` and `11-HUMAN-UAT.md` tests 4-9 were built for, not an implementation gap. One informational (non-blocking) note: `.planning/REQUIREMENTS.md`'s tracking table has not been updated to reflect the now-complete Phase 12 requirements — a documentation lag, not a code gap.

---

*Verified: 2026-09-13T00:00:00Z*
*Verifier: Claude (gsd-verifier)*
