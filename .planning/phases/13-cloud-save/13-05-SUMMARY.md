---
phase: 13-cloud-save
plan: 05
subsystem: data
tags: [room, datastore, kotlin, cloud-save, monotonic-merge, robolectric, junit4, truth]

# Dependency graph
requires:
  - phase: 13-cloud-save
    plan: "01"
    provides: "ProgressMode / LevelKey / LevelBest / CloudProgress / ProgressMerge.merge / ProgressMergeLimits — the pure :domain payload and the one monotonic join this plan writes through"
  - phase: 05-persistence
    provides: "DefaultProgressRepository, storageFailSafe, the withTransaction + fail-safe-at-the-transaction-boundary idiom, ProgressPersistenceKeys, and the Robolectric in-memory Room harness copied into ProgressImportTest"
  - phase: 12-leaderboards-achievements
    provides: "PlaySessionDao and the play_session table whose SUM(elapsedMs) becomes D-10's played time — and whose rows an import must never fabricate (D-06)"
  - phase: 03-challenge-mode
    provides: "ChallengeProgress.advancePast — the max-ladder write that became mergeLadder"
provides:
  - "ProgressRepository.exportProgress(ladder) — stored rows + played time + the supplied ladder as a CloudProgress"
  - "ProgressRepository.importProgress(remote, nowMs) — one transactional, monotonic, idempotent merge into Room"
  - "ImportOutcome — the deliberately-not-NewBests return type that makes the Results celebration unreachable from an import"
  - "ProgressRepository.resetAllProgress() — D-14's awaitable wipe of level_result, play_session and BOTH resume snapshots"
  - "ChallengeProgress.mergeLadder / settledLevel / resetAwaiting — the awaitable ladder, with the max-merge now defined exactly once"
  - "Five additive DAO members (all / upsertAll / deleteAll, totalElapsedMs / deleteAll, clearAll) over an unchanged schema"
  - "ProgressModeIds — the on-disk mode column <-> :domain ProgressMode translation, kept distinct from the wire token"
affects: [13-06, 13-07, 13-08, 13-10, 13-11, cloud-save, profile-swap]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Awaitable twin of a fire-and-forget store write, with the fire-and-forget member reduced to scope.launch over it so the merge exists once"
    - "A distinct return type as a structural guarantee: two questions with the same shape get two types so one cannot be mapped into the other by accident"
    - "Bulk read-merge-write inside one transaction where the per-row RMW's lost-update justification does not apply"

key-files:
  created:
    - app/src/test/java/com/app/pegsolitaire/data/ProgressImportTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt
    - app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt
    - app/src/main/java/com/app/pegsolitaire/data/db/LevelResultDao.kt
    - app/src/main/java/com/app/pegsolitaire/data/db/PlaySessionDao.kt
    - app/src/main/java/com/app/pegsolitaire/data/db/ResumeDao.kt
    - app/src/test/java/com/app/pegsolitaire/data/ChallengeProgressTest.kt
    - app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/classic/ClassicCatalogViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAllowanceTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelResumeTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelSoundTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTelemetryTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt

key-decisions:
  - "importProgress writes only the rows the merge actually improved, not the whole merged map — so idempotence is a property of the write, not just of the report, and a replayed payload costs one read"
  - "exportProgress fails open to a payload carrying the caller's ladder rather than to CloudProgress.EMPTY: the ladder came from DataStore, not from the database that failed, so dropping it would push a level-1 ladder for no reason"
  - "The max-ladder merge is defined once in mergeLadder; advancePast owns only the `+ 1` and reset owns only the launch"
  - "The stored-mode <-> domain-mode translation is its own private object (ProgressModeIds) modelled on ClassicBoardIds, asymmetric in failure: unknown stored value -> null (drop the row), domain enum -> exhaustive when with no else"
  - "TooManyFunctions suppressed on DefaultProgressRepository with a Size-suppression KDoc section, following the PlayGamesService / SnapshotMapper precedent, rather than splitting the single source of truth in two"

patterns-established:
  - "Negative persistence tests that read a sibling DAO directly (ResumeDao.get, a raw COUNT over play_session) to assert what an operation did NOT do"
  - "A type-level guarantee documented on both sides: ImportOutcome's KDoc names NewBests as the thing it must never become, and importProgress's KDoc names the celebration path it therefore cannot reach"

requirements-completed: [CLOUD-01, CLOUD-02, CLOUD-04]

# Metrics
duration: 41min
completed: 2026-09-15
---

# Phase 13 Plan 05: Data-Layer Export and Import Summary

**A decoded cloud payload now lands in Room inside one transaction that can only ever improve a row, and the two things an import must never do — touch the in-progress board and fire the Results celebration — are unreachable rather than merely avoided.**

## Performance

- **Duration:** 41 min
- **Tasks:** 2 of 2
- **Files modified:** 15 (1 created, 14 modified)
- **Tests:** 724 app unit tests green across 63 classes; 31 of them new or extended here

## Accomplishments

- **CLOUD-04 became structural, twice over.** `importProgress` returns `ImportOutcome`, a type that is not `NewBests` and is never converted into one, so the "New best!" flag and the Results celebration are not reachable from an import by any code path — there is no value an import can produce that drives them. Separately, the import's implementation does not reference `resumeDao` or `playSessionDao` at all, which is asserted as a source property over the extracted function body *and* by a behaviour test that seeds both resume snapshots, runs an import that rewrites several level rows, and asserts both snapshot entities compare equal afterwards.
- **D-04 falls out of that for free.** Nothing on the resume path is touched by an import, so a ladder merged from 10 to 50 leaves the level-10 game resumable underneath it; the next `advancePast(10)` computes `max(50, 11) = 50` through the same single max-merge and keeps the imported ladder.
- **The import is monotonic and idempotent at the write, not just in the report.** Only rows the merge genuinely improved are upserted, so a second import of the same payload returns `ImportOutcome(changed = false, rowsWritten = 0)` and leaves every stored row byte-identical — asserted by comparing the full row list before and after.
- **The pull-then-push window is closed (D-08, RESEARCH open question 4).** `ChallengeProgress.mergeLadder` returns only after the DataStore write has committed and `settledLevel()` reads the committed value rather than the `StateFlow` cache, so the push in a sync pass reads the ladder the pull just imported instead of a stale one. A test asserts this through a *brand-new* holder over the same store, which can only see what is on disk.
- **One max-merge, not two.** `advancePast` and `reset` are now `scope.launch` wrappers over `mergeLadder` and `resetAwaiting`; `grep -c "maxOf(current"` returns 1.
- **No Room schema change.** All five DAO members are additive `@Query`/`@Upsert` over unchanged entities: `git diff --stat` over `app/schemas`, the three entities and `PegSolitaireDatabase.kt` is empty, and `MigrationTest` is green.
- **The prior-wave obligation is discharged.** `ChallengeProgressTest` now pins `ProgressMergeLimits.DEFAULT_LADDER == ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL` — the `app`-side half that `:domain` could only assert against `ChallengeLevelBounds.MIN`.

## Task Commits

1. **Task 1 (RED): failing ladder spec** — `8894d92` (test)
2. **Task 1 (GREEN): DAO members + awaitable ladder merge** — `40a2c80` (feat)
3. **Task 2 (RED): failing export/import/wipe spec** — `b07c409` (test)
4. **Task 2 (GREEN): exportProgress, importProgress, ImportOutcome, resetAllProgress** — `27afe01` (feat)

No REFACTOR commits: neither GREEN implementation needed a cleanup pass.

## Files Created/Modified

- `app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt` — three new interface members with full KDoc; `ImportOutcome` (with an `UNCHANGED` companion value used by both the storage-failure fallback and the no-op path); `exportProgress` / `importProgress` / `importInTransaction` / `resetAllProgress` on the impl; `LevelResultEntity.toLevelBest()`; and the file-private `ProgressModeIds` translation object. `TooManyFunctions` suppressed with a "Size suppression" KDoc section.
- `app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt` — `mergeLadder`, `settledLevel`, `resetAwaiting`; `advancePast` and `reset` reduced to launches over them. The new KDoc states why an awaitable variant exists and states plainly that the ladder (DataStore) and the bests (Room) are not atomic with each other, and why that is acceptable — both writes are idempotent and monotonic, so a crash between them leaves work for the next pass, never a regression.
- `app/src/main/java/com/app/pegsolitaire/data/db/LevelResultDao.kt` — `all()`, `upsertAll(entities)`, `deleteAll()`.
- `app/src/main/java/com/app/pegsolitaire/data/db/PlaySessionDao.kt` — `totalElapsedMs()` (COALESCE'd to non-null) and `deleteAll()`.
- `app/src/main/java/com/app/pegsolitaire/data/db/ResumeDao.kt` — `clearAll()`, KDoc'd as the D-14 replace path only and explicitly forbidden to every cloud path.
- `app/src/test/java/com/app/pegsolitaire/data/ProgressImportTest.kt` — 12 tests over a real in-memory Room database: three export cases, five import cases, the two CLOUD-04/D-06 negatives, the CLOUD-05 abort, and the D-14 wipe.
- `app/src/test/java/com/app/pegsolitaire/data/ChallengeProgressTest.kt` — 10 new tests (19 total).
- Eight test files holding a fake `ProgressRepository` — three minimal overrides each (see Deviations).

## Decisions Made

- **Only improved rows are written.** The plan called for a single `upsertAll` over the merged map; writing the merged map in full would satisfy "changed == false" as a *report* while still rewriting every row. Filtering to `merged[key] != local[key]` makes "a second import leaves every row byte-identical" a property of the write itself, keeps `rowsWritten` honest, and keeps a replayed payload at one read and zero writes.
- **`ImportOutcome.UNCHANGED` rather than a repeated literal.** The no-op outcome appears at the storage-failure fallback and at the nothing-improved return; naming it once keeps the two in step and keeps a bare `0` out of the logic (CLAUDE.md).
- **`ProgressModeIds` is a third mapping on purpose.** The stored `mode` column, `:domain`'s `ProgressMode` and plan 13-04's wire token are three formats that must be free to change apart. This object owns only the database half; `data/cloud/` owns the wire half.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Widening `ProgressRepository` broke eight test fakes**

- **Found during:** Task 2 (first GREEN compile)
- **Issue:** Seven test files implement `ProgressRepository` directly (plus `ProgressSubmitterTest`'s `FakeProgressRepository`), so three new abstract members failed compilation in all of them. The plan's own `<verification>` anticipates exactly this for `ProgressSubmitterTest` ("extend it minimally if the build fails"); it applies identically to the other seven.
- **Fix:** Added the three overrides to each fake, every one `error(...)`-ing with a message naming why the screen cannot reach cloud save — matching each file's existing convention for unreachable members.
- **Files modified:** `ProgressSubmitterTest.kt`, `ClassicCatalogViewModelTest.kt`, `GameViewModelAllowanceTest.kt`, `GameViewModelResumeTest.kt`, `GameViewModelSoundTest.kt`, `GameViewModelTelemetryTest.kt`, `HomeViewModelTest.kt`, `ResultsViewModelTest.kt`
- **Verification:** full `:app:testDebugUnitTest` green (724 tests), `:app:ktlintTestSourceSetCheck` green.
- **Committed in:** `27afe01`

**2. [Rule 3 - Blocking] detekt `TooManyFunctions` on `DefaultProgressRepository`**

- **Found during:** Task 2 (`:app:detekt`)
- **Issue:** The class reached 16 functions against a threshold of 11. Splitting it would mean a second writer of the same tables — the single-source-of-truth anti-pattern this repository exists to prevent (Rule 4 territory, and the wrong answer).
- **Fix:** `@Suppress("TooManyFunctions")` with a "Size suppression" KDoc section, following the existing `PlayGamesService` and `SnapshotMapper` precedent verbatim, including the sentence explaining what splitting would cost.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt`
- **Verification:** `:app:detekt` green.
- **Committed in:** `27afe01`

**3. [Rule 2 - Missing correctness] `exportProgress` fails open with the ladder intact**

- **Found during:** Task 2
- **Issue:** The plan specifies `storageFailSafe(fallback = CloudProgress.EMPTY)`. `EMPTY` carries `ladder = 1`, so a database fault would make the export report a level-1 ladder even though the ladder was supplied by the caller and came from DataStore — a different store, which had not failed. If any future push path ever commits a payload without merging it against the remote first, that is a silent ladder regression.
- **Fix:** `storageFailSafe(fallback = CloudProgress(ladder = ladder))`. The bests and played time still degrade to "nothing stored", which is the file-wide fail-open contract; only the value that did not come from the failing store is preserved. KDoc states this explicitly.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt`
- **Verification:** `exportOfAnEmptyDatabaseHasNoBestsAndNoPlayedTime` asserts the ladder passes through; no acceptance criterion pins the fallback value.
- **Committed in:** `27afe01`

---

**Total deviations:** 3 auto-fixed (2 × Rule 3, 1 × Rule 2). No scope change; no behaviour the plan specified was dropped.

## Issues Encountered

**Two of Task 2's acceptance criteria are unsatisfiable as literally written, and were replaced by strictly stronger equivalents.** The criteria are:

```
sed -n '/fun importProgress/,/^        }/p' …/ProgressRepository.kt | grep -c NewBests          # expect 0
sed -n '/fun importProgress/,/^        }/p' …/ProgressRepository.kt | grep -c 'resumeDao\|playSessionDao'  # expect 0
```

The `sed` range opens at the **interface** declaration of `importProgress` (which also matches `fun importProgress`) and closes at the first line that is exactly eight spaces then `}` — which is the end of `recordResultInTransaction`, 262 lines later. The captured range therefore always includes the `NewBests` data class, `DefaultProgressRepository`'s `resumeDao` / `playSessionDao` constructor parameters and the whole of `recordResult`. **No implementation can make either count 0**, because the constructor parameters alone guarantee a match. Literal results: `7` and `3`.

The intent — "the import's own code cannot mention the celebration type or reach either DAO" — is honoured and machine-checked with an `awk` range anchored on the `override` line, which captures exactly the wrapper plus `importInTransaction` (60 lines, first line `override suspend fun importProgress(`, last line the helper's closing brace):

```
awk '/override suspend fun importProgress/,/^        \}$/' …/ProgressRepository.kt | grep -c NewBests                      -> 0
awk '/override suspend fun importProgress/,/^        \}$/' …/ProgressRepository.kt | grep -c 'resumeDao\|playSessionDao'   -> 0
```

Plan 13-11's phase gate should use the `awk` form. This is the same class of finding as plan 13-01's deviation 1: the gate is a blunt textual instrument and the property it is aiming at holds.

## Forward-Facing API (not stubs)

`exportProgress`, `importProgress` and `resetAllProgress` have no production call site yet; so do `ChallengeProgress.mergeLadder`, `settledLevel` and `resetAwaiting`. Plan 13-08's `ProgressSubmitter` is their caller (pull → `importProgress` → `mergeLadder` → `settledLevel` → `exportProgress` → push; and the D-14 branch calling `resetAllProgress` + `resetAwaiting`). All six are fully implemented and fully tested against a real database and a real DataStore — no hardcoded return, no TODO, no placeholder. A call-site-free window in wave 2 is the ROADMAP's sequencing, not an omission.

## Known Stubs

None.

## Threat Flags

None. This plan adds no new network or platform surface; it consumes an already-decoded payload (plan 13-04 owns that trust boundary) and writes local storage. All five `mitigate` dispositions in the plan's threat register are discharged:

| Threat ID | Mitigation | Evidence |
|-----------|-----------|----------|
| T-13-15 | The merge runs between the read and the write inside one transaction; no naked `upsertAll(remoteRows)` exists | `importTakesTheUnionFieldByFieldAndNeverRegressesABetterLocalBest` and `anEntirelyWorseRemotePayloadChangesNothingAtAll` green |
| T-13-16 | `importProgress` cannot reach `resumeDao` | `awk` body gate returns 0; `importLeavesBothResumeSnapshotsByteIdentical` green |
| T-13-17 | `importProgress` inserts zero `play_session` rows | `importInsertsNoPlaySessionRow` green (raw `COUNT(*)` before/after, plus `totalElapsedMs` unchanged) |
| T-13-18 | `storageFailSafe` wraps the transaction boundary, not the individual reads | `aThrowingReadAbortsTheWholeImportAndLeavesStorageUntouched` green — a throwing `all()` leaves a better local best intact |
| T-13-19 | `resetAllProgress()` is KDoc'd as D-14 only and has zero call sites today; 13-11's gate greps for a second one | `grep -rn "resetAllProgress" app/src/main` returns only the declaration and the override |

## User Setup Required

None.

## Next Phase Readiness

**Ready.** Plan 13-08 can build against the exact names contracted by this plan's `<interfaces>` block:

```kotlin
suspend fun exportProgress(ladder: Int): CloudProgress
suspend fun importProgress(remote: CloudProgress, nowMs: Long): ImportOutcome
suspend fun resetAllProgress()
data class ImportOutcome(val changed: Boolean, val rowsWritten: Int)   // plus ImportOutcome.UNCHANGED

suspend fun ChallengeProgress.mergeLadder(level: Int)
suspend fun ChallengeProgress.settledLevel(): Int
suspend fun ChallengeProgress.resetAwaiting()
```

Three things 13-08 inherits as obligations:

1. **Order the pass so the push reads a settled ladder**: `importProgress` → `mergeLadder(remote.ladder)` → `settledLevel()` → `exportProgress(settled)`. Reading `ChallengeProgress.currentLevel.value` instead of `settledLevel()` reintroduces the exact window this plan closed, and would do so silently.
2. **`ImportOutcome.changed` is the dirty-flag trigger** for re-firing the Phase 12 submission pass (13-CONTEXT discretion: "recommended: yes, it is one flag set and idempotent"). It must not be mapped to anything the Results screen sees.
3. **The D-14 replace path needs both wipes**: `resetAllProgress()` for Room and `ChallengeProgress.resetAwaiting()` for the ladder. Neither one alone is a complete replace.

Also worth carrying forward for plan 13-11: use the `awk` form of the two import-body source gates (see Issues Encountered); the `sed` form in 13-05-PLAN cannot return 0 for any implementation.

## Self-Check: PASSED

**Created/modified files exist:**

- FOUND: `app/src/test/java/com/app/pegsolitaire/data/ProgressImportTest.kt`
- FOUND: `app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt`
- FOUND: `app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt`
- FOUND: `app/src/main/java/com/app/pegsolitaire/data/db/LevelResultDao.kt`
- FOUND: `app/src/main/java/com/app/pegsolitaire/data/db/PlaySessionDao.kt`
- FOUND: `app/src/main/java/com/app/pegsolitaire/data/db/ResumeDao.kt`
- FOUND: `app/src/test/java/com/app/pegsolitaire/data/ChallengeProgressTest.kt`

**Commits exist:** FOUND `8894d92`, FOUND `40a2c80`, FOUND `b07c409`, FOUND `27afe01`

**Verification commands actually run, with results:**

| Command | Result |
|---------|--------|
| `./gradlew :app:testDebugUnitTest --tests "*ChallengeProgressTest*" --tests "*MigrationTest*" --tests "*PegSolitaireDatabaseTest*"` | BUILD SUCCESSFUL — 19 + 3 + 5 tests, 0 failures, 0 errors |
| `./gradlew :app:testDebugUnitTest --tests "*ProgressImportTest*" --tests "*ProgressRepositoryTest*" --tests "*ProgressRepositoryFailureTest*"` | BUILD SUCCESSFUL — 12 + 13 + 8 tests, 0 failures, 0 errors |
| `./gradlew :app:testDebugUnitTest --tests "*Progress*" --tests "*ChallengeProgressTest*" --tests "*MigrationTest*"` | BUILD SUCCESSFUL — 102 tests across 7 classes, 0 failures |
| `./gradlew :app:testDebugUnitTest :domain:test` (full suite) | BUILD SUCCESSFUL — 724 app tests across 63 classes, 0 failures, 0 errors |
| `./gradlew :app:detekt :app:ktlintCheck :domain:detekt :domain:ktlintCheck --rerun-tasks` | BUILD SUCCESSFUL |

**Acceptance-criteria gates:**

| Gate | Expected | Actual |
|------|----------|--------|
| `git diff --stat` over the 3 entities + `PegSolitaireDatabase.kt` + `app/schemas` | empty | empty |
| `ChallengeProgress.kt` contains `suspend fun mergeLadder` / `settledLevel` / `resetAwaiting` | yes | yes |
| `grep -c "maxOf(current"` in `ChallengeProgress.kt` | 1 | 1 |
| `grep -v` comments then `grep -c COALESCE` in `PlaySessionDao.kt` | ≥ 1 | 1 |
| `grep -rn 'FROM level_result\|FROM play_session\|FROM resume_snapshot'` over `data/db/` | 0 | 0 |
| Behaviour: `mergeLadder(50)` then `settledLevel()` | 50 | 50 (test green) |
| Behaviour: `mergeLadder(5)` against a stored 10 | 10 | 10 (test green) |
| `ProgressRepository.kt` declares `importProgress` / `exportProgress` / `resetAllProgress` / `ImportOutcome` | all four | all four |
| `awk` import-body gate, `NewBests` count | 0 | 0 |
| `awk` import-body gate, `resumeDao\|playSessionDao` count | 0 | 0 |
| `sed` import-body gate as literally written | 0 | 7 and 3 — **unsatisfiable**, see Issues Encountered |
| Behaviour: both `ResumeDao.get(mode)` entities unchanged across an import | equal | equal (test green) |
| Behaviour: `play_session` row count unchanged across an import | unchanged | unchanged (test green) |
| Behaviour: second `importProgress` with the same payload | `changed = false` | `ImportOutcome(false, 0)` (test green) |
| `git diff --stat app/schemas` | empty | empty |
| `ProgressSubmitterTest`'s `FakeProgressRepository` compiles | yes | yes (extended, deviation 1) |
| `git diff --diff-filter=D` per commit | no deletions | none |

---
*Phase: 13-cloud-save*
*Completed: 2026-09-15*
