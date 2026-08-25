---
phase: 05-persistence-resume-settings
reviewed: 2026-08-25T12:00:00Z
depth: standard
files_reviewed: 62
files_reviewed_list:
  - app/build.gradle.kts
  - app/schemas/com.app.pegsolitaire.data.db.PegSolitaireDatabase/1.json
  - app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt
  - app/src/main/java/com/app/pegsolitaire/data/ProgressPersistenceKeys.kt
  - app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt
  - app/src/main/java/com/app/pegsolitaire/data/SettingsRepository.kt
  - app/src/main/java/com/app/pegsolitaire/data/db/LevelResultDao.kt
  - app/src/main/java/com/app/pegsolitaire/data/db/LevelResultEntity.kt
  - app/src/main/java/com/app/pegsolitaire/data/db/PegSolitaireDatabase.kt
  - app/src/main/java/com/app/pegsolitaire/data/db/ResumeDao.kt
  - app/src/main/java/com/app/pegsolitaire/data/db/ResumeSnapshotEntity.kt
  - app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt
  - app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt
  - app/src/main/java/com/app/pegsolitaire/di/ApplicationScope.kt
  - app/src/main/java/com/app/pegsolitaire/di/DataModule.kt
  - app/src/main/java/com/app/pegsolitaire/di/DatabaseModule.kt
  - app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt
  - app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt
  - app/src/main/java/com/app/pegsolitaire/navigation/Routes.kt
  - app/src/main/java/com/app/pegsolitaire/platform/games/GamesService.kt
  - app/src/main/java/com/app/pegsolitaire/platform/games/NoOpGamesService.kt
  - app/src/main/java/com/app/pegsolitaire/ui/classic/BoardPreview.kt
  - app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogScreen.kt
  - app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogUiState.kt
  - app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogViewModel.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameUiState.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
  - app/src/main/java/com/app/pegsolitaire/ui/home/HomeRoute.kt
  - app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt
  - app/src/main/java/com/app/pegsolitaire/ui/home/HomeUiState.kt
  - app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt
  - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt
  - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsUiState.kt
  - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt
  - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsRoute.kt
  - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt
  - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsUiState.kt
  - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsViewModel.kt
  - app/src/main/res/values/dimens.xml
  - app/src/main/res/values/strings.xml
  - app/src/test/java/com/app/pegsolitaire/data/ChallengeProgressTest.kt
  - app/src/test/java/com/app/pegsolitaire/data/ProgressRepositoryTest.kt
  - app/src/test/java/com/app/pegsolitaire/data/ResumeRepositoryTest.kt
  - app/src/test/java/com/app/pegsolitaire/data/SettingsRepositoryTest.kt
  - app/src/test/java/com/app/pegsolitaire/data/db/MigrationTest.kt
  - app/src/test/java/com/app/pegsolitaire/data/db/PegSolitaireDatabaseTest.kt
  - app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt
  - app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt
  - app/src/test/java/com/app/pegsolitaire/platform/games/NoOpGamesServiceTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/classic/ClassicCatalogViewModelTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelChallengeAdvanceTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelResumeTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/home/HomeScreenTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsScreenTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelTest.kt
  - gradle/libs.versions.toml
findings:
  critical: 2
  warning: 3
  info: 4
  total: 9
status: issues_found
---

# Phase 5: Code Review Report

**Reviewed:** 2026-08-25T12:00:00Z
**Depth:** standard
**Files Reviewed:** 62
**Status:** issues_found

## Summary

Phase 5 (Room v1 persistence, resume snapshots, persisted settings, Challenge-level wiring) is broadly well built: the snapshot codec (`SnapshotMapper`) validates untrusted blobs thoroughly (tuple shapes, footprint bounds/duplicates, target membership, peg-mask range, per-move legality re-check against `RulesEngine`), results recording is solve-gated and once-only via `SavedStateHandle`, the best-merge runs inside `withTransaction`, and the migration/schema-export harness is real. Test coverage of the happy paths and most fail-safe paths is strong.

However, tracing the resume and ladder-advance flows end to end surfaces two Critical defects: (1) the Classic resume path crashes on a snapshot whose `levelId` is not a known catalog id — the one field of the untrusted blob that is never validated is consumed *synchronously in the ViewModel constructor* by a `requireNotNull`-backed lookup, defeating the phase's own "corrupt data must fail safe to no-resume, never crash" requirement; and (2) the Challenge ladder's `advanceToNext()` is a blind increment latched by a flag that undo deliberately resets, so re-solving the same board (back from Results → undo → re-jump) advances the persisted ladder a second time and silently skips a level. Three Warnings cover a snapshot-overwrite race on the resume path, corrupt snapshot rows that are never purged (leaving a permanent phantom "Continue Challenge" tile), and the restored Challenge game inheriting its level identity from the nav route instead of the snapshot's own provenance stamp.

## Narrative Findings (AI reviewer)

### Critical Issues

### CR-01: Classic "Continue" crashes when the stored snapshot's `levelId` is not a known catalog id

**File:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:153-158` (with `app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt:78-92` and `app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt`)
**Issue:** `SnapshotMapper` validates every structural aspect of the untrusted blob *except* `levelId`. `HomeViewModel` reads the decoded DTO's `levelId` verbatim and hands it to the Classic Continue tile; `AppNavHost` navigates `Gameplay(levelId = <blob value>, resume = true)`; and `GameViewModel` resolves it **synchronously in the constructor**:

```kotlin
private val classicBoard: Board? =
    if (clampedChallengeLevel == null) {
        levelRepository.loadClassic(levelId).initialBoard   // requireNotNull → throws
    } else { null }
```

`DefaultClassicLevelSource.load` is `requireNotNull(levelsById[levelId]) { "Unknown Classic level id: ..." }`, so an unrecognized id throws `IllegalArgumentException` during ViewModel construction — an app crash at the moment the player taps Continue. Because the corrupt row is never cleared (see WR-02), the tile reappears and the crash is reproducible on every tap. This is reachable via DB tampering/corruption (the threat model this phase explicitly defends against — Security §V5, T-05-03) and, more realistically, via a future app update that renames or removes a Classic board while an old snapshot survives on-device. Note `GameViewModelResumeTest` cannot catch this: its `CountingLevelRepository.loadClassic` never throws.
**Fix:** Validate `levelId` against the catalog before it can reach the synchronous load. Either:

```kotlin
// HomeViewModel — only offer Continue for a board that still exists:
val classicLevelId =
    progressRepository.loadSnapshot(MODE_CLASSIC)?.levelId
        ?.takeIf { id -> levelRepository.classicCatalog().any { it.id == id } }
```

and/or make the ViewModel-side load defensive on the resume path (a `loadClassicOrNull` used when `resume == true`, falling back to a safe default board plus snapshot clear). Add a test with a snapshot carrying a bogus `levelId`.

### CR-02: Re-solving the same board advances the Challenge ladder again — persisted level skips levels

**File:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:461-473, 583` (with `app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt:76-85`)
**Issue:** `ChallengeProgress.advanceToNext()` is a blind `current + 1` increment with no knowledge of which level was solved, and the only guard in `GameViewModel` is `finishedEmitted` — which `onUndo` deliberately resets whenever the undone board returns to `Playing` (line 583, needed for the Stuck-rescue flow). These interact into a level skip that needs no tampering:

1. Solve Challenge level N → `advanceToNext()` → persisted level N+1. Navigation to Results does **not** pop the Gameplay entry (`AppNavHost.kt:123-129` navigates without `popUpTo`).
2. Press back on Results (top-bar back / system back) → returns to the solved board, `canUndo == true`.
3. `onUndo()` → status `Playing` → `finishedEmitted = false`.
4. Redo the final jump → `Won` again → `advanceToNext()` fires a second time → persisted level N+2.

The player solved only level N (twice) but the ladder now sits at N+2 — level N+1 is skipped, violating Phase-3 D-02 ("no skipping") and the comment at line 470 claiming "the latch keeps it to once per solve". Each additional back-undo-resolve cycle skips another level. `GameViewModelChallengeAdvanceTest` pins "advances exactly once" only for a single continuous play; the re-solve path is untested.
**Fix:** Make the advance idempotent per played level instead of relative. E.g.:

```kotlin
// ChallengeProgress
fun advancePast(solvedLevel: Int) {
    scope.launch {
        dataStore.edit { prefs ->
            val current = prefs[CURRENT_LEVEL_KEY] ?: DEFAULT_CURRENT_LEVEL
            prefs[CURRENT_LEVEL_KEY] = maxOf(current, solvedLevel + 1)
        }
    }
}
// GameViewModel
if (clampedChallengeLevel != null) challengeProgress.advancePast(clampedChallengeLevel)
```

Alternatively keep a separate `ladderAdvanced` latch in the ViewModel that undo never resets. Add a regression test: Won → undo → re-solve must advance at most once.

### Warnings

### WR-01: `onLeaveScreen` during an in-flight restore can overwrite the saved snapshot with a fresh-board snapshot

**File:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:236-266, 303-320, 332-352` (with `app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt:51-58`)
**Issue:** For a Classic resume, the ViewModel constructor synchronously installs the **fresh** catalog board (`status = Playing`, empty history, fully interactive) while the snapshot load + replay runs asynchronously in `init`. In that window:
- If `ON_STOP` fires (player taps Continue then immediately backgrounds — most likely on the first DB access, when Room opens the connection cold), `onLeaveScreen()` sees a Playing board with a non-null `initialBoard` and saves a snapshot of the *untouched fresh board with empty move history*, racing — and typically overwriting — the very snapshot being restored. The player's saved game is silently destroyed.
- If the player taps quickly, moves made on the fresh board are silently discarded when `applyRestored` replaces the state (and `startMark` set by that early move is never reset, so the restored game's ticker double-counts `elapsedBaseSeconds` plus the pre-restore mark).
**Fix:** Introduce a `restoring` state for the resume path: start Classic resume in `GameUiState.loading()` (or gate `onCellTap`/`onLeaveScreen` on a `restorePending` flag) until the restore coroutine resolves either to `applyRestored(...)` or the fallback. `onLeaveScreen` should early-return while a restore is pending.

### WR-02: Unusable snapshot rows are never cleared — a corrupt Challenge blob leaves a permanent phantom "Continue Challenge" tile

**File:** `app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt:163-169` (with `app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt:78-92`, `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:255-262`)
**Issue:** `loadSnapshot` degrades a corrupt/mismatched blob to `null` but leaves the row in place, and the fallback path in `GameViewModel.init` loads a fresh board without clearing it. Home's Challenge Continue tile is driven purely by row existence (`observeResumeModes`), not decodability — so a corrupt Challenge row produces a "Continue Challenge — pick up Level N" tile that forever silently starts a fresh board, surviving until the player happens to solve or restart in that mode. The KDoc on `ProgressRepository.loadSnapshot` ("the Continue affordance simply disappears") is not what the code does for Challenge. The Classic side hides the tile (Home decodes the blob for `levelId`) but its corrupt row also lingers, re-decoded on every `observeResumeModes` emission.
**Fix:** Delete the row when it proves unusable — either in the repository:

```kotlin
override suspend fun loadSnapshot(mode: String): ResumeSnapshotDto? {
    val entity = resumeDao.get(mode) ?: return null
    val dto = SnapshotMapper.decode(entity.jsonBlob)?.takeIf { it.mode == mode }
    if (dto == null) resumeDao.clear(mode)   // corrupt: purge so Continue disappears
    return dto
}
```

or in `GameViewModel` when the restore fails (`if (restored == null) { progressRepository.clearSnapshot(mode); loadChallengeBoardIfNeeded() }`). Note Home's Challenge tile still needs the blob to be at least decodable before offering the tile, or it will keep showing until a load attempt clears it.

### WR-03: Restored Challenge game takes its level identity from the nav route and ignores the snapshot's `ChallengeStamp` — wrong multiplier/title/result keying possible

**File:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:173-176, 303-320` (with `app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt:111-121`, `app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt:58-68`)
**Issue:** `levelMultiplier`, the "Level N" title, and the `challengeLevel` carried into `Results` (and thus the level the result is recorded under, and the `+1` for "Next level") are all derived from the route arg. `applyRestored` never reads `restored.challenge` — the provenance stamp is dropped at exactly the point it exists to serve. Home's Continue tile supplies `challengeCurrentLevel`, a `stateIn(..., Eagerly, initialValue = 1)` StateFlow: at cold start, before the first DataStore emission lands, a fast tap on "Continue Challenge" navigates with `level = 1` while the restored board is really level N. The player then plays level N's board scored with level 1's multiplier, sees "Level 1" in the title, has the solve recorded under `challenge/1`, and "Next level" offers level 2. The same seam means the corrupt-snapshot fallback (WR-02) generates level 1 instead of level N in that window.
**Fix:** Once a restore succeeds, let the snapshot's stamp win: in `applyRestored`, take `restored.challenge?.level` as the effective challenge level (recompute the multiplier from it, expose it for the title/Results instead of the constructor-time route value). Alternatively/additionally, have Home pass the snapshot's stamped level for the Continue tile rather than `challengeCurrentLevel`.

### Info

### IN-01: Dead code — `ClassicCatalogUiState.Empty` and `ChallengeProgress.reset()` have no callers

**File:** `app/src/main/java/com/app/pegsolitaire/ui/classic/ClassicCatalogUiState.kt:21-25`, `app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt:87-95`
**Issue:** `ClassicCatalogUiState.Empty` is referenced nowhere (the ViewModel seeds with `catalogWith(emptyMap())`), and `ChallengeProgress.reset()` is never called from production code (only exercised by its own test) — it documents a "fresh Challenge run" feature that does not exist in the UI.
**Fix:** Remove `Empty`; either remove `reset()` or annotate it as the intended hook for a future "reset progress" setting.

### IN-02: Stale/unused string resources accumulating in `strings.xml`

**File:** `app/src/main/res/values/strings.xml:16-37, 61, 124`
**Issue:** `play_again`, `view_board`, `game_over_perfect_headline`, `game_over_perfect_body`, `game_over_near_headline`, `game_over_stuck_headline`, `pegs_remaining`, `pegs_remaining_retry` (Phase-1 game-over dialog, replaced by Results), `coming_soon`, and `challenge_load_error` have zero references in source. Dead copy invites drift and confuses the "copy contract lives in one place" claim.
**Fix:** Delete the unreferenced entries (or wire `challenge_load_error` to the load-failure state it was declared for, if that state is still planned).

### IN-03: `SnapshotMapper` does not bound coordinate values in an untrusted footprint

**File:** `app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt:146-161`
**Issue:** `toBoardSpecOrNull` validates tuple arity, footprint size (1..64), duplicates, and target membership, but not the coordinate values themselves: a tampered blob with `[-5, 3]` or `[1073741824, 0]` passes validation (`BoardSpec` imposes no constraints and cannot throw), producing a degenerate board that renders blank/off-canvas and is unplayable — a degraded-but-confusing outcome for the "restore or fail safe" contract. No crash was identified, hence Info.
**Fix:** Add a bounds check to `toPositionOrNull()` or the footprint validation, e.g. `row in 0..MAX_BOARD_DIM && col in 0..MAX_BOARD_DIM` with a small named constant (boards are ≤9 wide today; 63 would be a generous cap).

### IN-04: `GameRoute` puts the raw (unclamped) `challengeLevel` into the Results route

**File:** `app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt:69`
**Issue:** The board is loaded from `clampedChallengeLevel` (T-03-09), but the Results navigation carries `viewModel.challengeLevel` — the raw route value. An out-of-range value (only reachable via a forged/corrupt route today) would be recorded in the DB under the raw level and drive "Next level" as raw+1, while the board actually played was the clamped one. Currently benign because Home always supplies a persisted, in-range level, but it breaks the invariant that the clamp is the single boundary.
**Fix:** Expose and pass the clamped value (e.g. make `clampedChallengeLevel` the value `GameRoute` reads for both the title and the Results route).

---

_Reviewed: 2026-08-25T12:00:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
