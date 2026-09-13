---
phase: 12
plan: 07
subsystem: data
tags: [play-games, leaderboards, achievements, datastore, coroutines, mutex, backfill, hilt]
requires:
  - "12-02: AchievementId, AchievementRules.evaluate, ProgressAggregate, AchievementOutcome (:domain)"
  - "12-03: the Boolean-returning GamesService seam (submitScore / unlockAchievement / setAchievementSteps) + FakeGamesService"
  - "12-04: GamesIds.Leaderboards (7 accessors + forClassic) and GamesIds.Achievements (20 accessors)"
  - "12-06: ProgressRepository.aggregate() / .classicBests(), ClassicBoardIds.levelIdFor"
provides:
  - "data/ProgressSubmitter — the one D-13 @Singleton: start(), onResultRecorded(), a mutex-serialised pass"
  - "data/SubmissionSyncStore — the persisted D-10 'submissions pending' boolean, fail-open to false"
  - "PlayGamesPreferences.SUBMISSIONS_PENDING_KEY / DEFAULT_SUBMISSIONS_PENDING"
  - "GamesIds.of(AchievementId) — the 20-constant compile-time-exhaustive enum-to-ID map (D-17)"
  - "ResultsViewModel's one fire-and-forget submission hook, inside the RECORDED_KEY latch"
  - "MainActivity.progressSubmitter.start() — the cold-start session collector (Pitfall 5)"
affects:
  - "12-08: the Play Console checklist and 11-HUMAN-UAT.md device steps for real submissions, the unlock popup and the R8 release check"
  - "13 (cloud save): ProgressSubmitter is the precedent for a second data source behind ProgressRepository"
tech-stack:
  added: []
  patterns:
    - "Fire-and-forget @Singleton orchestrator: non-suspend public methods whose whole body is scope.launch on @ApplicationScope"
    - "Dirty-marker sync: a persisted boolean + a session collector, with the pass recomputed from SQL every time"
    - "Mutex.withLock (wait, never tryLock) to serialise idempotent passes rather than drop the newest"
    - "Per-call runCatching with CancellationException rethrown, results collected into a List<Boolean> and folded with all { } — never a short-circuiting &&"
    - "Delegating GamesService decorators (`GamesService by delegate`) to script one call in a sequence to fail or throw"
key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/data/ProgressSubmitter.kt
    - app/src/main/java/com/app/pegsolitaire/data/SubmissionSyncStore.kt
    - app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterTest.kt
    - app/src/test/java/com/app/pegsolitaire/data/SubmissionSyncStoreTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/data/PlayGamesPreferences.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/GamesIds.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/MainActivity.kt
    - app/src/test/java/com/app/pegsolitaire/BuildConfigIdsTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt
key-decisions:
  - "The pending flag's ACCESSOR lives in its own SubmissionSyncStore, not PlayGamesOptInRepository, because that class's KDoc claims a single-source-of-truth and move-together-or-not-at-all contract a third unrelated boolean would falsify; the KEY still sits beside the opt-in keys in PlayGamesPreferences"
  - "The flag records only THAT something is pending, never WHAT — the pass recomputes the whole submission set from SQL, so a record of what was pending could only go stale against the database it would be checked against"
  - "A pass that issued ZERO SDK calls leaves the flag untouched — an addition to D-10's literal text (T-12-75), because a storage fault and a brand-new player produce identical empty reads"
  - "RESEARCH Pitfall 3's process-lifetime de-dup cache was deliberately NOT implemented; it is the documented first lever if Play Console reports quota pressure"
  - "GamesIds.of carries a documented @Suppress(\"CyclomaticComplexMethod\") rather than the plan's ofOneShot/ofTiered split, because splitting a total map over a 20-constant enum cannot be done without reintroducing the `else` D-17 forbids"
  - "PGSL-04 is proven against the REAL ProgressSubmitter over a throwing GamesService, not a fake that throws out of onResultRecorded(); the latter would require the defensive runCatching at the call site the plan's own grep gate forbids"
requirements-completed: [PGSL-01, PGSL-02, PGSL-03, PGSL-04, PGSL-05, PGSA-02, PGSA-03]
duration: ~55 min
completed: 2026-09-13
---

# Phase 12 Plan 07: The Progress Submitter and Its Two Triggers Summary

**Stored progress now reaches Play Games: one `@Singleton` reads the SQL aggregates, applies one `AchievementRules` pass, and submits seven leaderboards' worth of scores plus twenty achievements' worth of unlocks and absolute steps — and a persisted dirty flag plus a cold-start session collector make the same pass replay for a player who banked everything offline.**

**Executed on branch:** `worktree-agent-a326eb8ad7dea2af0` (worktree; base `f4ebd41`)

## What Was Built

### Task 1 — the pending flag and the enum-to-ID map (`f9c26c2` RED → `6a54c9b` GREEN)

`PlayGamesPreferences` gained `SUBMISSIONS_PENDING_KEY = booleanPreferencesKey("play_games_submissions_pending")` and `DEFAULT_SUBMISSIONS_PENDING = false`. The literal exists in exactly one file; the KDoc records the fail-open tradeoff in full, including that `true` would be *more* recoverable (every submission is idempotent, an empty database issues no calls) and is rejected anyway because it contradicts D-10's "clean launches make no calls".

`SubmissionSyncStore` is a new `@Singleton open class` over the shared `app_prefs` store with exactly two `open` members — `pending: Flow<Boolean>` (`.catch { emit(emptyPreferences()) }`) and `setPending(Boolean)` (the `editFailOpen` shape, cancellation rethrown). It needs no Hilt module entry.

`GamesIds.of(id: AchievementId): String` is a 20-arm `when` with **no `else`**, ordered to match the enum's declaration order. `BuildConfigIdsTest` gained four cases driven off `AchievementId.entries`: non-blank, 20 distinct, set-equality against the 20 `Achievements` accessor values (derived by filtering the existing `gamesIdsByKey` map, never restated), and two by-name spot checks so a wholesale rotation of the map cannot pass set-equality alone.

### Task 2 — `ProgressSubmitter` (`4bf52c3` RED → `a12edf6` GREEN)

Four constructor params, two non-suspend public methods, one mutex-guarded private pass, four private helpers.

```kotlin
@Singleton
open class ProgressSubmitter @Inject internal constructor(
    private val progressRepository: ProgressRepository,
    private val gamesService: GamesService,
    private val syncStore: SubmissionSyncStore,
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val started = AtomicBoolean(false)
    private val passMutex = Mutex()

    open fun start()                                                      // AtomicBoolean-guarded session collector
    open fun onResultRecorded()                                           // body is exactly `scope.launch { submitAll() }`

    private suspend fun submitAll()                                       // passMutex.withLock { … }
    private suspend fun recordPassOutcome(issued: Boolean, allOk: Boolean)
    private suspend fun submitLeaderboards(aggregate, bests): List<Boolean>
    private suspend fun submitUnlocks(outcome): List<Boolean>
    private suspend fun submitSteps(outcome): List<Boolean>
    private suspend fun attempt(block: suspend () -> Boolean): Boolean
}
```

The helper decomposition is a detekt requirement, not cosmetics — default `LongMethod` 60 / `CyclomaticComplexMethod` 14 would both trip on a monolithic pass — and it survives `detekt` with no suppression on this file.

The pass, in order: session gate (`!is SignedIn` → set flag, return; `Unknown` takes this branch, with an in-source comment saying so); `aggregate()` + `classicBests()` (both already `storageFailSafe`, so neither is re-wrapped); **one** `AchievementRules.evaluate` call; the three helpers' results concatenated into one `List<Boolean>`; then `recordPassOutcome(issued = results.isNotEmpty(), allOk = results.all { it })`.

`attempt` wraps each individual seam call in `runCatching`, rethrows `CancellationException` before classification, and defaults to `false` — so a throw is indistinguishable from a reported failure. Nothing is accumulated with `&&`: every call is issued even after the first failure, asserted by a full-call-count case under `submissionResult = false`.

`ProgressSubmitterTest` is 19 cases over the shipped `FakeGamesService` (never a second recording fake), a settable `FakeProgressRepository` with a `CompletableDeferred` gate and an in-flight high-water counter, a `MutableStateFlow`-backed `FakeSubmissionSyncStore` subclass, and a `ScriptedGamesService` decorator (`GamesService by delegate`) for the two failure shapes one `submissionResult` boolean cannot express. Every expected achievement ID set is computed by calling `AchievementRules.evaluate` inside the test.

### Task 3 — the two triggers (`27608bf` RED → `64486ca` GREEN)

`ResultsViewModel` gained a fifth constructor parameter and exactly one line inside `recordSolvedPlayOnce`'s existing `viewModelScope.launch`, after `_uiState.update { it.copy(newBests = newBests) }`:

```kotlin
progressSubmitter.onResultRecorded()
```

No guard, no `try`, no `runCatching`, no second launch, no branch on `newBests`. The class KDoc's guard list gained a bullet explaining that the submission rides the `RECORDED_KEY` latch rather than adding a fourth — and why the hook is not in `GameViewModel`, whose `finishedEmitted` is reset by `performUndo()` and would resubmit on every undo/re-jump cycle. `GameViewModel` gained no reference to the submitter.

`MainActivity` gained a third injected field and one line between `gamesService.bootstrapIfOptedIn()` and `setContent`:

```kotlin
progressSubmitter.start()
```

Its KDoc covers the one place it departs from the file's stated convention (it names a class because `ProgressSubmitter` has no interface, and `@Singleton` alone already gives the single instance the convention protects), and states that the line has zero JVM-test coverage by construction.

## Task Commits

1. **Task 1: pending flag + AchievementId map** — `f9c26c2` (test) → `6a54c9b` (feat)
2. **Task 2: ProgressSubmitter** — `4bf52c3` (test) → `a12edf6` (feat)
3. **Task 3: the Results hook and the MainActivity start** — `27608bf` (test) → `64486ca` (feat)

No refactor commits were needed; all three GREEN implementations passed `detekt` and `ktlintCheck` as written (after the in-task corrections recorded under Deviations).

## Verification

| Command | Result |
|---------|--------|
| `./gradlew :app:testDebugUnitTest --tests "*BuildConfigIdsTest*"` | green (13 cases) |
| `./gradlew :app:testDebugUnitTest --tests "*SubmissionSyncStoreTest*"` | green (6 cases) |
| `./gradlew :app:testDebugUnitTest --tests "*ProgressSubmitterTest*"` | green (19 cases) |
| `./gradlew :app:testDebugUnitTest --tests "*ResultsViewModelTest*"` | green (50 cases) |
| `./gradlew :app:testDebugUnitTest --tests "*PlatformBoundary*"` | green — `data/` gained a `platform.games` import, no gms/Activity type |
| `./gradlew :app:testDebugUnitTest --tests "*GameViewModel*"` | green — the hook did not land in the wrong ViewModel |
| `./gradlew :app:testDebugUnitTest :domain:test` | green — the whole JVM suite, with `ResultsViewModel`'s changed constructor |
| `./gradlew :app:compileDebugKotlin` | green — the exhaustive `when` is a compile-time proof |
| `./gradlew detekt ktlintCheck` | green |

Grep gates, all satisfied:

- `grep -rc "GamesIds" app/src/main/java/com/app/pegsolitaire/ui/ --include='*.kt' | grep -vc ':0$'` → `0`
- `grep -rc 'AchievementId\|unlockAchievement\|setAchievementSteps\|submitScore' .../ui/ --include='*.kt' | grep -vc ':0$'` → `0`
- `grep -rn "android.util.Log" app/src/main/java | wc -l` → `0` (repo-wide V7 control intact)
- `grep -rn '"PGS_\|CgkI' app/src/main/java/com/app/pegsolitaire/data/ | wc -l` → `0`
- `grep -c 'progressSubmitter.onResultRecorded()' ResultsViewModel.kt` → `1`; `grep -c 'ProgressSubmitter' GameViewModel.kt` → `0`
- `grep -c 'runCatching'` on non-comment `ResultsViewModel.kt` lines → `0`
- `grep -rc 'ProgressSubmitter' app/src/main/java/com/app/pegsolitaire/di/ | grep -vc ':0$'` → `0` — **`di/` needed no change**
- `grep -c 'play_games_submissions_pending'` across `app/src/main` → exactly one file
- `grep -c 'else ->'` on non-comment `GamesIds.kt` lines → `1` (only `forClassic`'s)
- `grep -c 'AchievementId\.'` on non-comment `GamesIds.kt` lines → `20`

**Not run in this plan:** `./gradlew check` (adds the ~11-minute `:domain:depthSweep`) and `:app:assembleRelease` for the R8 `missing_rules.txt` inspection. Both are phase gates the plan assigns to after 12-08.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] The worktree had no `local.properties`, so no Gradle task could resolve the Android SDK**

- **Found during:** Task 1, first test run
- **Issue:** `local.properties` is gitignored, so a fresh worktree fails every AGP task with "SDK location not found"; `ANDROID_HOME` is unset on this machine.
- **Fix:** copied `local.properties` from the main checkout into the worktree. It stays gitignored and is not in any commit.
- **Files modified:** none tracked

**2. [Rule 1 - Bug] `GamesIds.of` tripped detekt's `CyclomaticComplexMethod`, and the plan's suggested remedy would have broken D-17**

- **Found during:** Task 1 verification
- **Issue:** detekt counts every `when` arm as a decision point, so a total map over a 20-constant enum scores 21 against a threshold of 14 — by construction, and the score grows with the roster rather than with any actual complexity. The plan's remedy ("split the map by tier — `ofOneShot` / `ofTiered` — rather than adding an `else`") **cannot be done without reintroducing an `else`**: a `when` over `AchievementId` listing only the fourteen one-shots is not exhaustive, so each half would need a throwing or placeholder `else` — precisely the compile-time guarantee D-17 exists to provide, and a direct violation of this task's own `grep -c 'else ->'` acceptance criterion.
- **Fix:** a documented `@Suppress("CyclomaticComplexMethod")` on `of`, with a KDoc section stating the metric is measuring roster size rather than complexity, that the split was tried and rejected, and why. Follows `PlayGamesService`'s existing documented `TooManyFunctions` precedent.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/platform/games/GamesIds.kt`
- **Commit:** `6a54c9b`

**3. [Rule 2 - Missing coverage] Task 1's `<behavior>` listed four `SubmissionSyncStore` behaviours with no test file to hold them**

- **Found during:** Task 1
- **Issue:** the task's `<files>` and acceptance criteria name only `BuildConfigIdsTest`, but four of the listed behaviours (fresh-store default, round trip, fail-open read, fail-open write) are about the new store and cannot be asserted there.
- **Fix:** added `app/src/test/java/com/app/pegsolitaire/data/SubmissionSyncStoreTest.kt`, six cases, built on `PlayGamesOptInRepositoryTest`'s real-temp-file / throwing-store / cancelling-store harness. The sixth case pins that cancellation still propagates.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/data/SubmissionSyncStoreTest.kt` (new)
- **Commit:** `f9c26c2` / `6a54c9b`

**4. [Rule 1 - Bug] Task 2's "a board with no leaderboard is skipped" case asserts something unreachable through the shipped objects**

- **Found during:** Task 2
- **Issue:** the plan asks for "a `classicBests()` entry whose `forClassic` lookup is `null`". No such entry exists: `ClassicBoardIds.levelIdFor` is exhaustive over the five `ClassicBoard` constants and `GamesIds.Leaderboards.forClassic` maps exactly those five. The only way to produce a `null` would be to stub `GamesIds`, which would assert against the stub rather than the real map.
- **Fix:** the production skip branch is implemented as specified (a `null` contributes no `false`, so it cannot set the dirty flag). The test asserts the **precondition that makes the branch unreachable today** — every `ClassicBoard` resolves to a non-null leaderboard — and its comment records that the branch exists for a sixth board that ships before its Console leaderboard does. The "a skip is not a failure" half is covered by the all-success flag-clearing case.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterTest.kt`
- **Commit:** `a12edf6`

**5. [Rule 1 - Bug] Task 3's PGSL-04 case, as written, could only pass with the `runCatching` its own acceptance criterion forbids**

- **Found during:** Task 3, GREEN run — the first implementation of the case failed with `SubmitterFailure` escaping into `runTest`
- **Issue:** the plan asks for "a submitter whose `onResultRecorded()` throws `RuntimeException`" while simultaneously requiring `grep -c 'runCatching'` on `ResultsViewModel.kt` to print `0` and stating that a defensive wrap "would only disguise a regression in that shape". Those two requirements are in direct tension: production's `onResultRecorded()` is non-suspend with a `scope.launch` body and therefore *cannot* throw at its caller, so a fake that throws synchronously is asserting the ViewModel defends against a violated contract — and the only way to satisfy it is the forbidden wrap.
- **Fix:** the case now drives the **real** `ProgressSubmitter` over a `ThrowingGamesService` (a `GamesService by delegate` decorator whose three submission methods throw) with a non-empty aggregate so calls are actually issued, and a real `SubmissionSyncStore` over a store that also throws — so even the dirty-flag write on the failure path fails. The assertion is unchanged and stronger for being reachable: the whole `ResultsUiState` is byte-identical to a healthy run, `newBests` is exactly what the repository returned, and the recorded calls match. The fire-and-forget *shape* PGSL-04 depends on is pinned separately, by `ProgressSubmitterTest` and by the `suspend fun start|onResultRecorded` → `0` grep.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt`
- **Commit:** `64486ca`

**6. [Rule 3 - Blocking] ktlint and detekt formatting corrections**

- **Found during:** Tasks 2 and 3
- **Issue:** three chained-call newlines, one needless blank line, and one `TooGenericExceptionThrown` on a bare `RuntimeException` in a test fake.
- **Fix:** reformatted the chains; replaced the bare throw with a named `SeamFailure` / `SubmitterFailure` type, which also reads better at the assertion — the claim is that *an unchecked exception the submitter knows nothing about* is classified as a failure, not that one particular stdlib type is.
- **Commits:** `a12edf6`, `64486ca`

**7. [Rule 3 - Blocking] Two pre-existing `viewModelOver(...)` call sites passed the cadence store and ad gateway positionally**

- **Found during:** Task 3 RED compile
- **Issue:** inserting `progressSubmitter` as the third parameter of `viewModelOver` broke four positional call sites in the interstitial-preload case.
- **Fix:** converted those call sites to named arguments (`cadenceStore = …, adGateway = …`). No test meaning changed.
- **Commit:** `27608bf`

### Not deviations, but worth recording

- The plan's `<interfaces>` note that `ResultsViewModel.recordSolvedPlayOnce()` sits at ~line 311 was accurate; the hook landed immediately after the `_uiState.update` inside the existing `viewModelScope.launch`.
- `FakeGamesService`'s shipped recording API matched the plan's sketch exactly; no drift.

## Notes for Plan 12-08 and `/gsd:verify-work`

**The shipped `ProgressSubmitter` surface** is listed in "What Was Built" above. Public API is two non-suspend methods; the four private helpers exist because detekt's default `LongMethod`/`CyclomaticComplexMethod` thresholds would both trip on a monolithic pass, and the file carries no suppression.

**The dirty-flag decision as shipped:**

| Decision | Shipped as | One-line justification |
|----------|-----------|------------------------|
| Key name | `play_games_submissions_pending` in `PlayGamesPreferences` | every `play_games_*` name stays in one scoped constants object (QUAL-03) |
| Accessor | a dedicated `SubmissionSyncStore`, not `PlayGamesOptInRepository` | that class's KDoc claims single-source-of-truth-for-opt-in, move-together-or-not-at-all atomicity, and a "can never fabricate an opt-in" fail-open argument — a third unrelated boolean falsifies all three |
| Payload | boolean only: *that*, never *what* | the pass recomputes the set from SQL every time, so a record of what was pending could only go stale against the database it would be checked against |
| Fail-open | `false` | `true` is more recoverable but contradicts D-10's "clean launches make no calls"; `false` costs at most one solve's delay |

**The zero-call invariant.** A pass that issued **no** SDK calls never clears the flag. This is an addition to D-10's literal text (T-12-75) and must survive any later refactor: `aggregate()` fails safe to `ProgressAggregate()` and `classicBests()` to `emptyMap()`, so a storage fault is byte-identical to a brand-new player, and clearing on that pass would silently discard a backfill no later signed-in solve would re-set. It is asserted by `aPassThatIssuedZeroCallsNeverClearsTheFlag` and stated in `ProgressSubmitter`'s class KDoc and in `recordPassOutcome`'s.

**RESEARCH Pitfall 3's de-dup cache was deliberately NOT implemented**, per CONTEXT's "resubmit everything, it is safe and idempotent". Reasoning is in `ProgressSubmitter`'s class KDoc: passes are human-paced, a fresh player's pass issues 0–3 calls, every call is idempotent by construction, and a cache would be a second source of truth the D-10 backfill must bypass anyway — costing the "every pass is a complete, self-healing resubmission" property that makes PGSL-05 provable. **It is the documented first lever if Play Console ever reports quota pressure.**

**`di/` needed no change** — both new classes are `@Singleton` with an `@Inject` constructor (the `ChallengeProgress` precedent), and `GameViewModel` gained no reference to the submitter. Both are gated by grep in the verification table above.

**The exact `RecordingProgressSubmitter` construction used in `ResultsViewModelTest`**, since later Results work needs the same four arguments:

```kotlin
private fun recordingSubmitter(log: MutableList<String> = mutableListOf()): RecordingProgressSubmitter =
    RecordingProgressSubmitter(
        log = log,
        progressRepository = FakeProgressRepository(),
        gamesService = FakeGamesService(),
        syncStore = SubmissionSyncStore(unusedDataStore()),
        scope = storeScope,
    )
```

`unusedDataStore()` is a file-private `DataStore<Preferences>` whose every member throws; it exists only to satisfy the real `SubmissionSyncStore` constructor, and the subclass overrides both public methods so it is never touched. `FakeProgressRepository` in that file now takes an optional `log` (shared with the submitter fake, so "record before submit" is one ordered comparison) and an optional `aggregate` (an empty one issues no submissions, which would give the PGSL-04 throwing seam nothing to throw from).

**Still owed to the device checklist (12-08's `11-HUMAN-UAT.md` additions)** — none of these is verifiable on the JVM:

- A score actually landing on a real leaderboard (all seven), and an achievement actually unlocking (all twenty) against a live Play Console project.
- The stock "achievement unlocked" popup (PGSA-04) — no v2 API exists to position or observe it.
- The offline→sign-in backfill end to end on a device: solve while signed out, sign in from Settings, confirm the ladder, cumulative score, per-board bests, unlocks and tier progress all arrive.
- The minified release R8 check (`./gradlew :app:assembleRelease`, inspect `missing_rules.txt`) — RESEARCH Pitfall 12.
- `MainActivity.progressSubmitter.start()` itself, which has zero JVM-test coverage by construction (no test references `MainActivity`, no `androidTest` source set). It is verified here only by grep.

## Known Stubs

None. Every file this plan created or modified is fully wired: `ProgressSubmitter` reads real aggregates and calls the real seam, `SubmissionSyncStore` reads and writes the real DataStore, `GamesIds.of` resolves real `BuildConfig` fields, and both triggers are live. The single unreachable code path — the `null`-leaderboard skip in `submitLeaderboards` — is a defensive branch for a sixth Classic board, documented in place and in Deviation 4.

## Threat Flags

None. Every trust boundary this plan crosses is already in the plan's own STRIDE register (T-12-70…T-12-77, T-12-SC). No new network endpoint, auth path, file access pattern or schema change was introduced, and no dependency of any kind was added.

## Self-Check: PASSED

Files verified present:

- `app/src/main/java/com/app/pegsolitaire/data/ProgressSubmitter.kt` — FOUND
- `app/src/main/java/com/app/pegsolitaire/data/SubmissionSyncStore.kt` — FOUND
- `app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterTest.kt` — FOUND
- `app/src/test/java/com/app/pegsolitaire/data/SubmissionSyncStoreTest.kt` — FOUND

Commits verified present: `f9c26c2`, `6a54c9b`, `4bf52c3`, `a12edf6`, `27608bf`, `64486ca`
