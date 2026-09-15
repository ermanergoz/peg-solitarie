---
phase: 12-leaderboards-achievements
reviewed: 2026-09-13T20:01:11Z
depth: standard
files_reviewed: 68
files_reviewed_list:
  - app/build.gradle.kts
  - app/schemas/com.app.pegsolitaire.data.db.PegSolitaireDatabase/2.json
  - app/src/main/java/com/app/pegsolitaire/MainActivity.kt
  - app/src/main/java/com/app/pegsolitaire/data/ClassicBoardIds.kt
  - app/src/main/java/com/app/pegsolitaire/data/PlayGamesPreferences.kt
  - app/src/main/java/com/app/pegsolitaire/data/ProgressPersistenceKeys.kt
  - app/src/main/java/com/app/pegsolitaire/data/ProgressRepository.kt
  - app/src/main/java/com/app/pegsolitaire/data/ProgressSubmitter.kt
  - app/src/main/java/com/app/pegsolitaire/data/SubmissionSyncStore.kt
  - app/src/main/java/com/app/pegsolitaire/data/db/LevelResultDao.kt
  - app/src/main/java/com/app/pegsolitaire/data/db/Migrations.kt
  - app/src/main/java/com/app/pegsolitaire/data/db/PegSolitaireDatabase.kt
  - app/src/main/java/com/app/pegsolitaire/data/db/PlaySessionDao.kt
  - app/src/main/java/com/app/pegsolitaire/data/db/PlaySessionEntity.kt
  - app/src/main/java/com/app/pegsolitaire/data/db/ProgressProjections.kt
  - app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt
  - app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt
  - app/src/main/java/com/app/pegsolitaire/di/DatabaseModule.kt
  - app/src/main/java/com/app/pegsolitaire/navigation/Routes.kt
  - app/src/main/java/com/app/pegsolitaire/platform/games/GamesConstants.kt
  - app/src/main/java/com/app/pegsolitaire/platform/games/GamesIds.kt
  - app/src/main/java/com/app/pegsolitaire/platform/games/GamesService.kt
  - app/src/main/java/com/app/pegsolitaire/platform/games/NoOpGamesService.kt
  - app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameFinish.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameRoute.kt
  - app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt
  - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt
  - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsRoute.kt
  - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt
  - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsViewModel.kt
  - app/src/main/res/values/strings.xml
  - build-logic/src/main/kotlin/GamesIdResolution.kt
  - build-logic/src/main/kotlin/standards.gradle.kts
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/achievement/AchievementId.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/achievement/AchievementRules.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/achievement/AchievementThresholds.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/achievement/ProgressAggregate.kt
  - domain/src/main/kotlin/com/app/pegsolitaire/domain/telemetry/SolveTelemetry.kt
  - local.properties.template
  - app/src/test/java/com/app/pegsolitaire/BuildConfigIdsTest.kt
  - app/src/test/java/com/app/pegsolitaire/data/ProgressAggregateTest.kt
  - app/src/test/java/com/app/pegsolitaire/data/ProgressRepositoryFailureTest.kt
  - app/src/test/java/com/app/pegsolitaire/data/ProgressRepositoryTest.kt
  - app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterTest.kt
  - app/src/test/java/com/app/pegsolitaire/data/ResumeRepositoryTest.kt
  - app/src/test/java/com/app/pegsolitaire/data/SubmissionSyncStoreTest.kt
  - app/src/test/java/com/app/pegsolitaire/data/db/MigrationTest.kt
  - app/src/test/java/com/app/pegsolitaire/data/resume/SnapshotMapperTest.kt
  - app/src/test/java/com/app/pegsolitaire/platform/games/FakeGamesService.kt
  - app/src/test/java/com/app/pegsolitaire/platform/games/NoOpGamesServiceTest.kt
  - app/src/test/java/com/app/pegsolitaire/platform/games/PlayGamesServiceTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/classic/ClassicCatalogViewModelTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameFlowTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAllowanceTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelResumeTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelSoundTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTelemetryTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsPlayGamesTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsPrivacyTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelTest.kt
  - build-logic/src/test/kotlin/GamesIdResolutionTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/achievement/AchievementCatalogTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/achievement/AchievementRulesTest.kt
  - domain/src/test/kotlin/com/app/pegsolitaire/domain/telemetry/SolveTelemetryTest.kt
findings:
  critical: 1
  warning: 3
  info: 6
  total: 10
status: issues_found
---

# Phase 12: Code Review Report

**Reviewed:** 2026-09-13T20:01:11Z
**Depth:** standard
**Files Reviewed:** 68 (40 production/config, 28 test)
**Status:** issues_found

## Summary

Phase 12 adds Play Games leaderboards and achievements on top of the Phase 11 session seam: a pure-domain achievement kernel (`AchievementId` / `AchievementRules` / `ProgressAggregate` / `SolveTelemetry`), a Room v1 to v2 migration adding `play_session`, SQL aggregate projections, a `ProgressSubmitter` singleton with a persisted dirty flag, per-session assist telemetry in `GameViewModel`, 27 build-config IDs with a release fail-fast, and two Settings rows for the stock UIs.

What holds up under scrutiny: the migration SQL matches `2.json` byte-for-byte and is validated with `validateDroppedTables = true` against seeded v1 rows; `recordResult` writes both tables in one `withTransaction` and the new tests prove rollback in both directions; the SQL aggregates handle `NULL` correctly (`MAX`/`SUM` stay nullable, `COUNT` and the coalesced star sum do not; `SUM(bestScore)` is a `Long`); every submission is collected before the flag verdict (no short-circuit `&&`); `CancellationException` is rethrown before classification everywhere it is caught; `ApplicationScope` is a `SupervisorJob`; `start()` is `AtomicBoolean`-guarded against the repeat `onCreate`; nothing under `ui/` references `GamesIds`, `AchievementId`, or a `BuildConfig.PGS_*` field; the `:domain` additions import nothing from Android or `:app`.

What does not hold up: **`ResultsViewModel.recordSolvedPlayOnce()` still gates on `pegsRemaining == 1`**, while `GameViewModel.statusFor` awards `Won` at `pegCount <= spec.minimumPegs`. Diamond-41's floor is 2, so every Diamond-41 win is dropped before it reaches `level_result` or `play_session`. The guard predates this phase, but Phase 12 builds three achievements and one leaderboard directly on top of it and its own CONTEXT asserts "Diamond-41's proven minimum of 2 counts as perfect" — an assumption the data layer can never satisfy. That is the one blocker. Beyond it there is a narrow race in the backfill trigger, an unbounded-wait robustness issue in `PlayGamesService.submitting`, and an inverted "fail-closed" label on the assist-counter defaults, plus six quality items.

## Critical Issues

### CR-01: Diamond-41 wins are never recorded, so `CLEARED_DIAMOND41`, `GRAND_TOUR`, `PERFECTIONIST`, and the Diamond-41 leaderboard are unreachable

**File:** `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt:376` (guard), `:439` (constant); contrast `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:1657`
**Issue:** `recordSolvedPlayOnce()` begins with `if (args.pegsRemaining != PEGS_REMAINING_SOLVED) return` where `PEGS_REMAINING_SOLVED = 1`. `GameViewModel.statusFor` (line 1657) declares a board `Won` when `board.pegCount <= board.spec.minimumPegs`, and `Boards.kt:287` sets Diamond-41's `minimumPegs` to its proven floor of 2. A flawless Diamond-41 finish therefore arrives at Results with `pegsRemaining = 2`, is shown as `PERFECT` (the `tierFor` branch and the `aPerfectDiamond41FinishWithTwoPegsRemainingIsAdEligible` test both know this), and is then silently **not recorded**: no `level_result` row, no `play_session` row, no `progressSubmitter.onResultRecorded()` call.

Consequences for this phase specifically:
- `CLEARED_DIAMOND41` can never unlock (`clearedClassicBoards` is derived from `level_result`).
- `GRAND_TOUR` requires all five `ClassicBoard` entries cleared; Diamond-41 is always missing, so it can never unlock.
- `PERFECTIONIST` likewise can never unlock, despite CONTEXT D-04 stating "Diamond-41's proven minimum of 2 counts as perfect".
- `PGS_LB_CLASSIC_DIAMOND41` never receives a score (`classicBests()` has no Diamond row).
- Pre-existing collateral: Diamond-41 never shows star pips in the Classic catalog and never reports "New best!".

The guard was introduced in commit `4415671` (plan 05-05) before Diamond-41's two-peg floor existed (Phase 8, DEBT-02), and the KDoc on `isInterstitialEligible` (lines ~490–500) explicitly warns that copying `PEGS_REMAINING_SOLVED` into a Classic gate "would misclassify a perfect Diamond-41 finish as a loss" — yet that is exactly what the record gate already does. No test in `ResultsViewModelTest` or `ProgressRepositoryTest` records a Diamond-41 result, which is why 28 test files pass with 3 of 20 achievements unreachable.

**Fix:** Carry the finishing board's solve threshold on the route and compare against it, rather than against a hardcoded 1. `GameFinish` is emitted only on `Won`, so `GameRoute` has the spec at hand:

```kotlin
// navigation/Routes.kt — Results
val minimumPegs: Int = 1,   // default keeps pre-Phase-12 call sites and old back stacks valid

// ui/game/GameRoute.kt — inside viewModel.finished.collect { finish -> ... }
minimumPegs = finish.result.minimumPegs,   // or expose nextBoard.spec.minimumPegs on GameFinish

// ui/results/ResultsViewModel.kt
private val isSolved: Boolean get() = args.pegsRemaining <= args.minimumPegs

private fun recordSolvedPlayOnce() {
    if (!isSolved) return
    ...
}
```

Clamp `minimumPegs` at the nav boundary the way `challengeLevel` is clamped (e.g. `coerceIn(1, pegsRemaining.coerceAtLeast(1))` or against `BoardConstants`), since `Results` is an independently reachable typed route. Use the same `isSolved` in `tierFor` / `ResultsUiState.offersNextLevel` so the three solved checks cannot drift (see IN-06). Add a regression test: a Results VM constructed with `levelId = ClassicLevelIds.DIAMOND41, pegsRemaining = 2, stars = 3` must call `recordResult` exactly once with `mode = classic, levelId = diamond41` and then `onResultRecorded()`; and a `ProgressSubmitterTest` / `AchievementRulesTest` fixture that reaches `GRAND_TOUR` through a Diamond-41 row written by `recordResult`, not seeded directly.

## Warnings

### WR-01: Backfill trigger reads the pending flag outside the pass mutex, so a sign-in that lands mid-write loses its backfill until the next solve or launch

**File:** `app/src/main/java/com/app/pegsolitaire/data/ProgressSubmitter.kt:123` (collector) vs `:159` (session gate under lock)
**Issue:** `start()`'s collector does `if (session is SignedIn && syncStore.pending.first()) submitAll()`. The flag read happens **before** `passMutex` is taken, while `submitAll()`'s signed-out branch writes `setPending(true)` **inside** the lock. Interleaving: (1) a solve is recorded while `session` is `Unknown`; `onResultRecorded()` enters `submitAll`, sees `!SignedIn`, and begins the suspending `DataStore.edit`; (2) `bootstrapIfOptedIn` resolves `SignedIn`; the collector runs `pending.first()`, reads the not-yet-committed `false`, and skips; (3) the edit commits `true`. The session `StateFlow` never re-emits `SignedIn`, so the pending work sits until the next signed-in solve (which resubmits everything and clears it) or the next process launch. The CONTEXT's "whenever `session` resolves to `SignedIn`, if the flag is set, everything is resubmitted" is therefore not quite what the code guarantees. Bounded and self-healing, but it is precisely the player the backfill was written for (signs in, then closes the app) who can hit it, and `twoOverlappingPassesSerializeRatherThanInterleaveOrDrop` does not exercise this ordering.
**Fix:** Move the flag read under the lock so the session check and the flag check see a consistent snapshot:

```kotlin
open fun start() {
    if (!started.compareAndSet(false, true)) return
    scope.launch {
        gamesService.session.collect { session ->
            if (session is GamesSignInState.SignedIn) submitAll(onlyIfPending = true)
        }
    }
}

open fun onResultRecorded() { scope.launch { submitAll(onlyIfPending = false) } }

private suspend fun submitAll(onlyIfPending: Boolean) =
    passMutex.withLock {
        if (gamesService.session.value !is GamesSignInState.SignedIn) { syncStore.setPending(true); return@withLock }
        if (onlyIfPending && !syncStore.pending.first()) return@withLock
        ...
    }
```

Add a test that gates the fake store's `setPending` behind a `CompletableDeferred`, flips the session to `SignedIn` while the write is parked, and asserts exactly one pass still runs.

### WR-02: `PlayGamesService.submitting` blocks up to 10 s per call waiting for a foreground Activity; a ~29-call pass can hold `passMutex` for minutes while backgrounded

**File:** `app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt:429-433` (and `:362`)
**Issue:** Each `submitScore` / `unlockAchievement` / `setAchievementSteps` calls `awaitForegroundActivity()`, which is `withTimeoutOrNull(ACTIVITY_WAIT_MS = 10_000)` around `awaitActivity()`. `DefaultForegroundActivityProvider.onActivityPaused` nulls the current activity, so if the player backgrounds the app immediately after a solve (the normal "finish level, switch apps" pattern), every one of the pass's sequential calls waits the full 10 s and returns `false`. A fully-unlocked player's pass is ~29 calls (RESEARCH Pitfall 3), so `ProgressSubmitter.submitAll` holds `passMutex` for up to ~5 minutes producing nothing but `false`s, and any `onResultRecorded()` or backfill pass queues behind it. The `Immediate` SDK calls do not need a *resumed* Activity to complete; the dirty flag already guarantees a retry, so waiting buys nothing.
**Fix:** For submissions, do not wait — use the current activity or fail fast, and let the flag carry the retry:

```kotlin
private suspend fun <T> submitting(block: suspend (Activity) -> T): Boolean {
    if (_session.value !is GamesSignInState.SignedIn) return false
    val activity = activityProvider.current ?: return false   // no wait: the dirty flag retries
    return runCatching { block(activity) }
        .onFailure { if (it is CancellationException) throw it }
        .isSuccess
}
```

Alternatively, if an `Activity` must be awaited for the first call, resolve it once per pass (e.g. a `suspend fun <R> withSubmissionContext(block: suspend (Activity) -> R)` on the seam is more invasive; failing fast is simpler and matches PGSL-04's fire-and-forget intent). Keep `launchStockUi`'s wait as-is — the stock UI genuinely needs a foreground Activity.

### WR-03: Absent or negative assist counters resolve to `0`, which *grants* Hint-free / Undo-free, yet four KDocs label this "fail-CLOSED"

**File:** `app/src/main/java/com/app/pegsolitaire/data/resume/ResumeSnapshotDto.kt:75,89`; `app/src/main/java/com/app/pegsolitaire/data/resume/SnapshotMapper.kt:41,97,200-203`; `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt:694`; `app/src/main/java/com/app/pegsolitaire/navigation/Routes.kt:112-114`; `domain/src/main/kotlin/com/app/pegsolitaire/domain/telemetry/SolveTelemetry.kt` (`coerced`)
**Issue:** For a *purity* claim ("this play used no hints"), the conservative direction is to **deny** purity when the evidence is missing. The code does the opposite at every boundary: `orNoAssists()` maps `null` and negatives to `0`; `Results` defaults `hintsUsed = 0` / `undosUsed = 0`; `SolveTelemetry.coerced` clamps a forged negative to `0`. Each of those makes a tampered or telemetry-less play count as hint-free and undo-free. CONTEXT D-09 locks "absent fields on an old snapshot read as zero", so the *default* is a deliberate product decision and not in dispute — but the documentation repeatedly asserts the opposite security direction ("fail-CLOSED … guessing generously there would hand out a Hint-free … achievement nobody earned"), which is precisely what the code does. Misleading threat-model prose is a maintenance hazard: the next reader will trust it. Separately, a **negative** value is never a legitimate legacy state — it is tampering — and the mapper already refuses resumes for tampered moves, footprints, and targets (`aTamperedIllegalMoveYieldsNoResume` etc.), so clamping it toward the achievement is inconsistent with the codec's own rule.
**Fix:** (a) Reword the four KDocs to state the true direction: "absent resolves to 0 per D-09 — fail-OPEN toward the achievement, accepted because the store is local-only and the exposure is one self-awarded badge". (b) Treat negatives as tampering rather than as zero: in `SnapshotMapper.fromDto` return `null` (refuse the resume, matching the other forged-field rules) or, if refusing is judged too harsh, resolve negatives to a value that *disqualifies* purity (`Int.MAX_VALUE` or `1`) instead of `0`; do the same in `SolveTelemetry.coerced` at the nav boundary. Update `aForgedNegativeAssistCounterClampsToZeroWithoutRefusingTheResume` and `coercedClampsANegativeHintCountToZero` to the chosen behaviour.

## Info

### IN-01: Unreachable `null` branch in `submitLeaderboards`

**File:** `app/src/main/java/com/app/pegsolitaire/data/ProgressSubmitter.kt:221-222`
**Issue:** `GamesIds.Leaderboards.forClassic(ClassicBoardIds.levelIdFor(board))` cannot return `null`: `levelIdFor` is an exhaustive `when` over `ClassicBoard` and `forClassic` covers all five ids. The `if (leaderboardId != null)` branch is dead, and a test (`everyShippedClassicBoardResolvesToALeaderboardSoTheSkipBranchIsUnreachableToday`) exists only to assert that it is dead. The round trip `ClassicBoard -> String -> String?` also couples the submitter to the on-disk id text for no reason.
**Fix:** Key the lookup by the enum with an exhaustive `when` so a sixth board fails the build and the nullable path disappears:

```kotlin
// GamesIds.Leaderboards
fun forClassic(board: ClassicBoard): String = when (board) {
    ClassicBoard.ENGLISH33 -> classicEnglish33
    ...
}
// ProgressSubmitter
bests.forEach { (board, best) -> add(attempt { gamesService.submitScore(GamesIds.Leaderboards.forClassic(board), best.toLong()) }) }
```

Keep a `String`-keyed overload only if something else needs it (nothing in `src/main` does today); drop the "unreachable" test.

### IN-02: `AchievementThresholds.CLASSIC_BOARD_TOTAL` is dead production code

**File:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/achievement/AchievementThresholds.kt:96`
**Issue:** Referenced only by `AchievementCatalogTest` and a KDoc link in `AchievementId`. `AchievementRules` uses `containsAll(ClassicBoard.entries)`, so the denominator is never read in `src/main`. It is also the one non-`const` member of an otherwise `const`-only thresholds object.
**Fix:** Remove it and the test that asserts it equals `ClassicBoard.entries.size` (a tautology); reword the `GRAND_TOUR` KDoc to "every `ClassicBoard`".

### IN-03: Package cycle between `data` and `platform.games`

**File:** `app/src/main/java/com/app/pegsolitaire/platform/games/GamesIds.kt:4` (imports `data.ClassicLevelIds`); `app/src/main/java/com/app/pegsolitaire/data/ProgressSubmitter.kt` (imports `platform.games.GamesIds`, `GamesService`, `GamesSignInState`)
**Issue:** `data -> platform.games -> data`. `PlayGamesService` already imported `data.PlayGamesOptInRepository`, so there is precedent, and `PlatformBoundaryRules` does not forbid it — but Phase 12 doubles the edge count, and the `ProgressSubmitter` KDoc has to spend a paragraph arguing the layering is legal. Cycles like this are what make a later module split painful.
**Fix:** Resolving IN-01 (`forClassic(ClassicBoard)`) removes `GamesIds`'s only `data` import. Longer term, `GamesSignInState`/`GamesService` are the data layer's legitimate dependency; `GamesIds` could live beside the submitter in `data` (it is a BuildConfig accessor, not SDK code) if the one-way rule `data -> platform` is wanted.

### IN-04: Stock-UI handlers have no in-flight guard; a double tap launches two intents

**File:** `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsViewModel.kt:253-266`
**Issue:** `onShowLeaderboards()` / `onShowAchievements()` each `viewModelScope.launch { gamesService.showX() }`. `launchStockUi` awaits a foreground Activity (up to 10 s) and then `startActivityForResult`s. Two quick taps on the row produce two coroutines and, once the Activity is available, two stock-UI launches stacked on the back stack.
**Fix:** Hold a `Job?` and ignore taps while it is active, or use the existing `playGamesSignInInFlight`-style pattern the sign-in row uses:

```kotlin
private var stockUiJob: Job? = null
fun onShowLeaderboards() {
    if (stockUiJob?.isActive == true) return
    stockUiJob = viewModelScope.launch { gamesService.showLeaderboards() }
}
```

### IN-05: Unchecked cast in the build script hides a future debug-resolution failure behind a `ClassCastException`

**File:** `app/build.gradle.kts:129-132`
**Issue:** `(GamesIdResolution.resolve(..., isRelease = false) as GamesIdResolutionResult.Resolved).ids` relies on the comment "debug can never fail". If `GamesIdResolution` ever grows a debug-side failure mode, configuration dies with a bare `ClassCastException` rather than the resolver's message. The release path right beside it already uses `when`.
**Fix:**

```kotlin
val debugGamesIds: Map<String, String> =
    when (val resolved = GamesIdResolution.resolve(gamesIdEnv, gamesIdLocal, isRelease = false)) {
        is GamesIdResolutionResult.Resolved -> resolved.ids
        is GamesIdResolutionResult.Failure -> throw GradleException(resolved.message)
    }
```

### IN-06: Two identically named `PEGS_REMAINING_SOLVED = 1` constants encode the same (incorrect) solved rule in two files

**File:** `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsUiState.kt:121`; `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt:439`
**Issue:** The same name and value are declared privately in both files and consumed by three different "is this solved?" checks (`offersNextLevel`, `tierFor`, `recordSolvedPlayOnce`). Duplication is how CR-01 survived Phase 8: the constant's KDoc was updated to say it is only correct for Challenge, but the record gate that uses it for Classic was not.
**Fix:** Fold into the CR-01 change: a single `isSolved` derived from `pegsRemaining <= minimumPegs` on the route, exposed on `ResultsUiState`, and used by all three sites; delete both constants.

---

_Reviewed: 2026-09-13T20:01:11Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
