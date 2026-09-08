---
phase: 12
plan: 06
subsystem: data
tags: [room, sql, aggregates, achievements, leaderboards, telemetry, transaction]
requires:
  - "12-01: play_session table + insert-only PlaySessionDao + DatabaseModule provider"
  - "12-02: ProgressAggregate, ClassicBoard, AchievementThresholds, SolveTelemetry (:domain)"
  - "12-05: ResultsViewModel.solveTelemetry (already coerced at the nav boundary)"
provides:
  - "ProgressRepository.aggregate(): ProgressAggregate — the single AchievementRules input"
  - "ProgressRepository.classicBests(): Map<ClassicBoard, Int> — the PGSL-03 leaderboard input"
  - "ProgressRepository.recordResult(mode, levelId, result, telemetry, nowMs) — both tables, one transaction"
  - "ClassicBoardIds.boardFor(levelId) / .levelIdFor(board) — the id-string <-> ClassicBoard map"
  - "LevelResultDao.totals / .classicStandings, PlaySessionDao.flags — the four SQL projections"
affects:
  - "12-07: ProgressSubmitter consumes aggregate() + classicBests(); GamesIds.forClassic() consumes ClassicBoardIds.levelIdFor"
tech-stack:
  added: []
  patterns:
    - "Room @Query projections into unannotated data classes; SQL aliases must equal constructor parameter names"
    - "FROM-less SELECT of correlated scalar subqueries — one round trip, always exactly one row, non-null return"
    - "Multiple DAO calls composed inside database.withTransaction for a consistent-read snapshot"
    - "Delegating throwing-DAO test fakes (`X by delegate`) to aim a fault at one method"
key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/data/ClassicBoardIds.kt
    - app/src/main/java/com/app/pegsolitaire/data/db/ProgressProjections.kt
    - app/src/test/java/com/app/pegsolitaire/data/ProgressAggregateTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/data/ProgressPersistenceKeys.kt
    - app/src/main/java/com/app/pegsolitaire/data/db/LevelResultDao.kt
    - app/src/main/java/com/app/pegsolitaire/data/db/PlaySessionDao.kt
    - app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt
    - app/src/test/java/com/app/pegsolitaire/data/ProgressRepositoryTest.kt
    - app/src/test/java/com/app/pegsolitaire/data/ProgressRepositoryFailureTest.kt
    - app/src/test/java/com/app/pegsolitaire/data/ResumeRepositoryTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/classic/ClassicCatalogViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAllowanceTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelResumeTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelSoundTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTelemetryTest.kt
key-decisions:
  - "aggregate() takes no mode parameter — ProgressAggregate spans both modes by construction (D-03)"
  - "recordResult's telemetry parameter has no default, so the compiler catches every call site (D-08)"
  - "Both Classic board sets are derived from one classicStandings() read, so a Grand tour count cannot disagree with its set (D-04)"
  - "The English-33 center-finish narrowing lives only in PlaySessionDao.flags' SQL (D-01)"
  - "challengeScoreTotal is Long? end to end so the sum cannot wrap into a negative leaderboard score (Pitfall 6)"
requirements-completed: [PGSL-01, PGSL-02, PGSL-03, PGSA-02, PGSA-03]
duration: ~50 min
completed: 2026-09-09
---

# Phase 12 Plan 06: SQL Progress Aggregates and the Atomic Telemetry Write Summary

Every number Phase 12 submits is now produced by SQL: four Room `@Query` projections fill a complete `ProgressAggregate` and a `ClassicBoard`-keyed best-score map, and a solved play writes its merged best and its telemetry row inside one transaction or writes neither.

**Executed on branch:** `worktree-agent-a2ea0194d16e07493` (worktree; base `09bdc2d`)

## What Was Built

**Task 1 — the SQL layer** (`81d21c1`)

Six column constants joined `ProgressPersistenceKeys` (12-01 deliberately deferred them until a `@Query` interpolated one). `ClassicBoardIds` is the single data-boundary translation between `:app`'s id strings and `:domain`'s `ClassicBoard`: `boardFor` has an `else -> null` so a legacy or Challenge id is dropped rather than fatal, and `levelIdFor` has no `else` so a sixth board fails the build. `ProgressProjections.kt` holds the three unannotated row types whose constructor parameter names the SQL aliases match exactly.

Four queries shipped, all with `ProgressPersistenceKeys` names interpolated and every mode / id / threshold bound as a `:param`:

`LevelResultDao.totals(challengeMode, threeStars): LevelResultTotals`
```sql
SELECT
  (SELECT MAX(CAST($COLUMN_LEVEL_ID AS INTEGER)) FROM $TABLE_LEVEL_RESULT
     WHERE $COLUMN_MODE = :challengeMode) AS highestChallengeLevel,
  (SELECT SUM($COLUMN_BEST_SCORE) FROM $TABLE_LEVEL_RESULT
     WHERE $COLUMN_MODE = :challengeMode) AS challengeScoreTotal,
  (SELECT COALESCE(SUM($COLUMN_STARS), 0) FROM $TABLE_LEVEL_RESULT) AS totalStars,
  (SELECT COUNT(*) FROM $TABLE_LEVEL_RESULT) AS solvedLevelCount,
  (SELECT COUNT(*) FROM $TABLE_LEVEL_RESULT
     WHERE $COLUMN_STARS >= :threeStars) AS threeStarCount
```

`LevelResultDao.classicStandings(classicMode): List<ClassicStanding>`
```sql
SELECT $COLUMN_LEVEL_ID AS levelId, $COLUMN_STARS AS stars, $COLUMN_BEST_SCORE AS bestScore
FROM $TABLE_LEVEL_RESULT
WHERE $COLUMN_MODE = :classicMode
```

`PlaySessionDao.flags(classicMode, english33Id, speedLimitMs): PlaySessionFlags`
```sql
SELECT
  EXISTS(SELECT 1 FROM $TABLE_PLAY_SESSION
           WHERE $COLUMN_HINTS_USED = 0) AS anyHintFreeSolve,
  EXISTS(SELECT 1 FROM $TABLE_PLAY_SESSION
           WHERE $COLUMN_UNDOS_USED = 0) AS anyUndoFreeSolve,
  EXISTS(SELECT 1 FROM $TABLE_PLAY_SESSION
           WHERE $COLUMN_MODE = :classicMode AND $COLUMN_LEVEL_ID = :english33Id
             AND $COLUMN_LAST_PEG_ON_TARGET = 1) AS anyCenterFinish,
  EXISTS(SELECT 1 FROM $TABLE_PLAY_SESSION
           WHERE $COLUMN_ELAPSED_MS < :speedLimitMs) AS anySpeedSolve
```

**Task 2 — the read surface** (`a8a4466` RED, `409e91f` GREEN)

`aggregate()` runs its three reads inside one `withTransaction` so a solve landing mid-read cannot yield a snapshot claiming a level the star total does not know about, then assembles `ProgressAggregate` positionally. Both board sets come from the same standings list via a private `List<ClassicStanding>.boards(minStars)` helper. `classicBests()` maps the same standings to `board to bestScore`, dropping unmappable ids. `DefaultProgressRepository` took `playSessionDao` as its fourth constructor parameter.

**Task 3 — the atomic write** (`bd28ff3` RED, `a749672` GREEN)

`recordResult` gained `telemetry: SolveTelemetry` between `result` and `nowMs`, and `recordResultInTransaction` appends a `PlaySessionEntity` built from this play's own values after the existing upsert. `ResultsViewModel.recordSolvedPlayOnce()` passes `telemetry = solveTelemetry` — one named argument.

## Verification Results

| Check | Result |
|-------|--------|
| `:app:testDebugUnitTest` (whole JVM suite) | PASS |
| `ProgressAggregateTest` | PASS — 27 tests |
| `ProgressRepositoryTest` | PASS — 13 tests (was 7; +6) |
| `ResultsViewModelTest` | PASS — 46 tests |
| `MigrationTest` | PASS — 3 tests |
| `:app:compileDebugKotlin` (Room KSP validates the SQL) | PASS |
| `detekt` + `ktlintCheck` (project-wide) | PASS |
| `git diff --stat 09bdc2d -- app/schemas` | EMPTY — schema byte-identical |
| `60_000` / `60000` under `app/.../data/` | 0 — the 60 lives only in `AchievementThresholds` |
| 12-07 code surface under `app/.../data/` | 0 (3 prose-only KDoc mentions of `AchievementRules`) |

**Counts requested by the plan:**
- `storageFailSafe` in `ProgressRepository.kt`: **7 before Task 3, 7 after** — the boundary did not move.
- `database.withTransaction {` call sites: **2** (`recordResult`, `aggregate`). A raw `grep -c withTransaction` returns 4 because the import line and one KDoc line also match.
- **Pitfall 7 KDoc landed on `aggregate()`**: yes — it states that `highestChallengeLevel` is highest BANKED, that `ChallengeProgress.currentLevel` stores `max(current, solved + 1)` i.e. the NEXT level, that the two normally differ by exactly one (and by more if a best-effort write failed while `advancePast` succeeded), and that the SQL value is authoritative and must not be "reconciled".

## Symbol Contract for Plan 12-07

| Symbol | Location | Shape |
|--------|----------|-------|
| `ProgressRepository.aggregate()` | `app/.../data/ProgressRepository.kt` | `suspend fun aggregate(): ProgressAggregate` — no parameters, cross-mode, fails safe to `ProgressAggregate()` |
| `ProgressRepository.classicBests()` | `app/.../data/ProgressRepository.kt` | `suspend fun classicBests(): Map<ClassicBoard, Int>` — absent boards missing, fails safe to `emptyMap()` |
| `ProgressRepository.recordResult(...)` | `app/.../data/ProgressRepository.kt` | `suspend fun recordResult(mode: String, levelId: String, result: ScoreResult, telemetry: SolveTelemetry, nowMs: Long): NewBests` — no default on `telemetry` |
| `ClassicBoardIds.boardFor` / `.levelIdFor` | `app/.../data/ClassicBoardIds.kt` | `internal object`; `fun boardFor(levelId: String): ClassicBoard?` and `fun levelIdFor(board: ClassicBoard): String` |

**Still owed by 12-07:** the `ProgressSubmitter` type, its call inside `recordSolvedPlayOnce()`, the dirty flag, `AchievementRules.evaluate` wiring, and every leaderboard/achievement ID. None of them is referenced anywhere in this plan's code.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocker] A seventh ViewModel-test fake needed the same updates**
- **Found during:** Task 2 (and again in Task 3)
- **Issue:** The plan enumerated six test files declaring a `ProgressRepository` fake. A seventh, `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTelemetryTest.kt`, landed with plan 12-05 *after* 12-06 was written. Without it the whole test source set failed to compile (`Class 'TelemetryProgressRepository' is not abstract and does not implement abstract members`).
- **Fix:** Added the same two `aggregate()` / `classicBests()` overrides and the same `telemetry: SolveTelemetry` parameter, in the file's existing style.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTelemetryTest.kt`
- **Verification:** `:app:testDebugUnitTest` green.
- **Commits:** `409e91f`, `bd28ff3`

**2. [Rule 3 - Blocker] `ProgressRepositoryFailureTest` also calls `recordResult`**
- **Found during:** Task 3
- **Issue:** The plan listed this file only for the `repositoryWith(...)` helper. It also makes three direct `recordResult` calls that stopped compiling once the signature changed.
- **Fix:** Passed `SolveTelemetry.NONE` at all three sites. Its existing fail-safe assertions are untouched, which is the point — the `StorageFailSafe` posture did not move.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/data/ProgressRepositoryFailureTest.kt`
- **Commit:** `a749672`

**3. [Rule 1 - Stale assertion] The 12-05 "byte-identical write path" case was invalidated by design**
- **Found during:** Task 3
- **Issue:** `ResultsViewModelTest` asserted `forgedRepository.recordCalls == plainRepository.recordCalls`, justified by a comment reading "`recordResult` keeps its current signature (12-06)". This plan changes that signature and passes forged-but-clamped telemetry through, so the two call lists now legitimately differ.
- **Fix:** The case now asserts the **scoring** half is identical (`result`, `mode`, `levelId`), that the plain path records `SolveTelemetry.NONE`, and that the forged path's recorded telemetry equals the ViewModel's own resolved `solveTelemetry` — strictly stronger than the original, and honest about the one field that is meant to differ.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt`
- **Commit:** `bd28ff3`

**4. [Rule 1 - Stale doc] `ResultsViewModel.solveTelemetry` KDoc claimed the value was written nowhere**
- **Found during:** Task 3
- **Issue:** The 12-05 KDoc ended "What this plan deliberately does NOT do: `recordResult` keeps its current signature and this value is not yet written anywhere. Plan 12-06 owns that." That is now false in the same commit.
- **Fix:** Rewrote the closing paragraph to state that `recordSolvedPlayOnce` passes it to `recordResult`, which writes it to `play_session` in the same transaction as the best row, and that it is passed through verbatim. Also extended `recordSolvedPlayOnce`'s own KDoc.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt`
- **Commit:** `a749672`

**Total deviations:** 4 auto-fixed (2× Rule 3 blocker, 2× Rule 1 stale artifact). **Impact:** none on the plan's contract — all four are consequences of plan 12-05 landing between this plan being written and being executed. No architectural decision was needed.

### Acceptance-criteria wording resolved against the plan's own stronger rule

Two criteria are written as literal greps that the plan's own instructions make impossible to satisfy literally. Both are met in substance:

- `grep -c "lastPegOnTarget = 1" PlaySessionDao.kt is 1` — the shipped SQL is `$COLUMN_LAST_PEG_ON_TARGET = 1`, because the same task mandates that column NAMES are `ProgressPersistenceKeys` string templates and "never a quoted literal". The constant rule wins; the comparison **is** against `1`, and `grep -c "= true\|= false"` is **0** as required.
- `grep -c "GamesService\|...\|AchievementRules\|AchievementId" ProgressRepository.kt is 0` — the file has exactly **one** match, a prose KDoc line the same task's action text explicitly instructs writing ("KDoc `aggregate()` with ... that it is the SINGLE input to `AchievementRules.evaluate`"). There is no import, no reference and no call; a code-only grep returns 0.

## Threat Model Outcomes

| Threat | Disposition | Realised as |
|--------|-------------|-------------|
| T-12-30 torn state | mitigated | The `play_session` insert sits inside the existing `withTransaction`; two tests force a failure in each direction and assert row counts on **both** tables, not just the return value. |
| T-12-31 integer overflow | mitigated | `challengeScoreTotal` is `Long?` from SQL through the projection into `ProgressAggregate`; a test sums three `Int.MAX_VALUE` bests and asserts the exact positive `Long`. |
| T-12-32 SQL injection | N/A (structural) | All four queries bind `:params`; no concatenation, no quoted literal. Verified by grep. |
| T-12-33 client-trusted score | accepted (residual) | Unchanged posture — no server this milestone. Every number remains deterministic and recomputable from stored rows. |
| T-12-34 storage-fault DoS | mitigated | `aggregate()` → `ProgressAggregate()`, `classicBests()` → `emptyMap()`, `recordResult` keeps `NewBests(false,false,false)`; three throwing-DAO tests. |
| T-12-35 information disclosure | N/A | No logging added; no identity data touched. |
| T-12-SC supply chain | N/A | No dependency change; `gradle/libs.versions.toml` untouched. |

## Known Stubs

None. Every field of `ProgressAggregate` is populated from SQL and asserted; no placeholder, empty-literal or TODO was introduced.

## Threat Flags

None — this plan opened no new network endpoint, auth path, exported component or schema surface. It added four `@Query` methods, one constructor parameter and two repository methods.

## Issues Encountered

None blocking. The known worktree quirk (missing gitignored `local.properties`) was resolved by copying it from the main checkout before the first Gradle invocation; it was never staged.

## Next Phase Readiness

Ready for **12-07**. The two consumed methods and the `ClassicBoardIds` map are stable and committed; no schema change was made, so 12-01's `2.json` and `MIGRATION_1_2` remain valid.

## Self-Check: PASSED

- All 3 created files exist on disk (`ClassicBoardIds.kt`, `ProgressProjections.kt`, `ProgressAggregateTest.kt` at 473 lines, above the 200-line minimum).
- All 5 commits found in `git log --all`: `81d21c1`, `a8a4466`, `409e91f`, `bd28ff3`, `a749672`.
- Every task's `<acceptance_criteria>` re-run and passing (two resolved on wording as documented above).
- Plan-level `<verification>` commands re-run: full `:app` JVM suite, `compileDebugKotlin`, project-wide `detekt` + `ktlintCheck`, and all four greps — green.
- `app/schemas` byte-identical to base commit `09bdc2d`.
- `.planning/STATE.md` and `.planning/ROADMAP.md` were not modified (worktree mode; the orchestrator owns those writes).
