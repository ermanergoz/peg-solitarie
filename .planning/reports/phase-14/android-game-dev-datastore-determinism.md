# android-game-dev report — R-3 / F-10: DataStore-backed unit tests made deterministic

Date: 2026-09-21
Branch: feature/phase-14-datastore-determinism (944821f..HEAD; work commits 944821f..fa98d17, then this report)
Skills loaded: `ergoz-soft:standards` (role: android-game-dev) → plugin copies (v0.5.2, newer than this worktree's `.claude/rules/VERSION` 0.5.1) of `studio.md`, `reporting.md`, `gitflow.md`, `testing.md`, `kotlin-android.md`; repo `.claude/rules/lessons.md`. `superpowers:test-driven-development` applied as the procedure (failing test first, watched to fail for the right reason, then implementation). Not loaded: `compose-skill`, `compose-ui` — this task touches **no UI file** and no production source at all; `android-viewmodel` — `ResultsViewModelTest` changed only in how its store is constructed, not in what it asserts.
Ran in parallel with: R-1 and R-2 on `feature/phase-14-prerelease-fixes` (both edit `build-logic/src/main/kotlin/standards.gradle.kts`; this task touches test sources only, so there is no file collision)
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- `InMemoryPreferencesDataStore` replaces the temp-file Preferences store in **all ten** classes, removing the file, the foreign `Dispatchers.IO` scope and the wall clock in one move. No test was disabled, ignored, deleted or reworded into passing; every `@Test` count is identical before and after.
- It carries its own test class, and **each** contract test was watched to fail against a variant lacking the property — atomicity against a non-serialised first cut (1 of 50 increments survived), emission semantics against a conflating `StateFlow` variant. Neither test is vacuous.
- **Ten consecutive full-suite runs, all green**: 1204 tests, 0 failures, 0 errors, 0 skipped, 109 classes, every run.
- The suite got **faster**: mean 59.5 s against a 68.3 s baseline, with 10 more tests. `InterstitialCadenceStoreTest` went from 0.051 s to a 0.024–0.057 s band.
- **I did not reproduce the original 60.041 s failure.** I did not try to; the qa-engineer owns the reproduction. Nothing below should be read as evidence about the mechanism — only as evidence that the pattern that permitted it is gone.

## Outputs
- `app/src/test/java/com/app/pegsolitaire/testing/InMemoryPreferencesDataStore.kt` — the shared double (new). No file IO, no `Dispatchers.IO`, no scope parameter, no `delay`, no timeout; a `Mutex` around `updateData` and a `MutableSharedFlow(replay = 1, extraBufferCapacity = Int.MAX_VALUE - 1)` as the state and change feed.
- `app/src/test/java/com/app/pegsolitaire/testing/InMemoryPreferencesDataStoreTest.kt` — its contract (new, 10 tests): 3 on `updateData` atomicity, 6 on `data` emission semantics, 1 on determinism (zero virtual time elapsed).
- The ten migrated classes: `InterstitialCadenceStoreTest`, `ResultsViewModelTest`, `ChallengeProgressTest`, `SettingsRepositoryTest`, `CloudSyncStoreTest`, `SubmissionSyncStoreTest`, `CloudProfileStoreTest`, `PlayGamesOptInRepositoryTest`, `OnboardingRepositoryTest`, `AppForegroundTrackerTest`.
- `app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterCloudTest.kt` — KDoc only (a stale cross-reference; see "Open issues").
- **No `THIRD_PARTY.md` row is owed and the file was not created.** Zero new dependencies: the double uses `androidx.datastore.core`, `kotlinx.coroutines` and (in its test) `junit`/`truth`, all already declared. `git diff develop...HEAD -- gradle/ '*.gradle.kts' gradle.properties settings.gradle.kts` is **empty**. Creating `THIRD_PARTY.md` is R-5 and belongs to the release-manager.

## What the double guarantees, and why each half is load-bearing

The tech-lead fixed the contract (item C of the sign-off). Both halves are implemented and pinned:

1. **`updateData` atomicity.** `writeLock` is held across the whole read-transform-write, including the caller's suspending `transform`. Callers write read-modify-write transforms; a suspension inside one is the only window in which two updates can interleave on a single-threaded dispatcher, and if they do, one caller's write is lost silently and the store reports a value nobody wrote.
2. **`data` emits the current value on collection, then every subsequent value.** `replay = 1` is the first clause and is exactly what the suite's "killed and relaunched" simulation needs — a second holder constructed over the same store object reads back the first holder's writes. The effectively unbounded buffer is the second clause: an already-collecting collector never misses an intermediate value that a caller's `data.first { … }` predicate is waiting for.

Two divergences from a real store, both documented in the class KDoc:
- **No conflation.** A real store's `data` is `StateFlow`-backed and *can* drop intermediate values. The double drops none. That is strictly weaker as a requirement on the code under test — a predicate satisfied against a real store is still satisfied here — so it cannot turn a passing test red, and it is what makes `data.first { … }` deterministic rather than a race.
- **An unchanged write still emits.** A real store skips the write when `transform` returns an equal value. Invisible here: every consumer reads through a `StateFlow`, which de-duplicates equal values, and no test in the suite asserts an emission count on `data` (checked: no Turbine or list-collection assertion over `dataStore.data` in any of the ten).

It deliberately does **not** simulate failure. The classes needing a hostile store keep their own throwing/cancelling anonymous `DataStore` objects, untouched.

## The ten classes, and the one exception I did not migrate

`grep -rln 'PreferenceDataStoreFactory.create' app/src domain/src` returned exactly the ten classes the task named — I confirmed the list rather than trusting it. **None of them asserts anything about the file on disk**: `TemporaryFolder` appears only as the `produceFile` source in every one, so all ten migrated and no class needed the tech-lead's case-2 fallback (a real store with an explicit scope). The "killed and relaunched" simulation is a second holder over the same store object throughout, which the double reproduces exactly.

One related site is **not** migrated, deliberately: `ProgressSubmitterCloudTest.kt:1344`'s private `inMemoryDataStore()`. It does not match the grep (it never used the factory) and it has **none** of the three defects — no file, no foreign scope, no wall clock. Its author had independently reached the same conclusion, and its KDoc says so: *"that store commits on `Dispatchers.IO`, which `advanceUntilIdle()` cannot drive"*. Migrating it would be a refactor of a deterministic test in a suite the tech-lead has just ruled on (D-04a ladder-read ordering), which is outside this task's remit under `rules/studio.md` standing rule 4. It is a private, untested, non-atomic duplicate of shared infrastructure, so **folding it into the tested double is worth scheduling** — I corrected only its now-false cross-reference to `ChallengeProgressTest` and left a pointer.

## Checks

- `grep -rln 'PreferenceDataStoreFactory.create' app/src domain/src` **before** — **pass** (10 files: the ten classes named in the task, none passing a `scope`).
- `grep -rln 'PreferenceDataStoreFactory.create' app/src domain/src` **after** — **pass** (**1 file**, `app/src/test/java/com/app/pegsolitaire/testing/InMemoryPreferencesDataStore.kt`, and both hits there are KDoc prose explaining what the double replaces — no call site survives anywhere).
- `grep -rn 'TemporaryFolder\|realDataStore\|tempFolder' app/src domain/src` — **pass** (zero hits outside the double's own KDoc).
- **TDD, red before green** — `./gradlew --max-workers=4 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.testing.*'`:
  - contract test with no implementation — **fail** (`Unresolved reference 'InMemoryPreferencesDataStore'`).
  - against a deliberately non-serialised first cut — **fail**, 1 of 10, and for the right reason: `concurrentReadModifyWriteUpdatesAllLandBecauseUpdateDataSerialises`, `expected: 50 but was: 1` (49 increments lost). The atomicity test is not vacuous.
  - against a conflating `MutableStateFlow`-backed variant (mutex present) — **fail**, 2 of 10: `anAlreadyCollectingCollectorSeesEverySubsequentValueInOrder` and `noUpdateIsDroppedWhileTheCollectorIsSuspended`. The emission tests are not vacuous either, and the two properties are pinned independently.
  - against the final implementation — **pass** (10 tests, 0 failures, 0.095 s).
- **Detekt actually reaches the new package** — probe file with two seeded violations in `app/src/test/java/com/app/pegsolitaire/testing/`, `./gradlew --max-workers=4 :app:detekt --rerun-tasks` — **fails as intended** (`Analysis failed with 2 issues`: `SwallowedException`, `FunctionOnlyReturningConstant`). Probe deleted; `git status --porcelain` clean afterwards. The new files are under the gate, not merely assumed to be.
- `./gradlew --max-workers=4 detekt ktlintCheck --rerun-tasks` — **pass** (`BUILD SUCCESSFUL in 9s`, `25 actionable tasks: 25 executed`). Two violations were found and fixed during the work, not suppressed: one `MaxLineLength` in `ResultsViewModelTest` and, from ktlint, three stray blank lines plus two now-unused imports.
- `./gradlew --max-workers=4 :domain:test :app:testDebugUnitTest --rerun-tasks` **× 10 consecutive** — **all ten pass**:

  | run | wall | result | tests | failures | errors | skipped | classes | `InterstitialCadenceStoreTest` | the formerly-flaky test |
  |----|----|----|----|----|----|----|----|----|----|
  | 1 | 52 s | BUILD SUCCESSFUL | 1204 | 0 | 0 | 0 | 109 | 0.024 s | 0.002 s |
  | 2 | 61 s | BUILD SUCCESSFUL | 1204 | 0 | 0 | 0 | 109 | 0.027 s | 0.002 s |
  | 3 | 70 s | BUILD SUCCESSFUL | 1204 | 0 | 0 | 0 | 109 | 0.057 s | 0.005 s |
  | 4 | 54 s | BUILD SUCCESSFUL | 1204 | 0 | 0 | 0 | 109 | 0.024 s | 0.002 s |
  | 5 | 58 s | BUILD SUCCESSFUL | 1204 | 0 | 0 | 0 | 109 | 0.024 s | 0.002 s |
  | 6 | 57 s | BUILD SUCCESSFUL | 1204 | 0 | 0 | 0 | 109 | 0.029 s | 0.002 s |
  | 7 | 60 s | BUILD SUCCESSFUL | 1204 | 0 | 0 | 0 | 109 | 0.027 s | 0.003 s |
  | 8 | 58 s | BUILD SUCCESSFUL | 1204 | 0 | 0 | 0 | 109 | 0.027 s | 0.003 s |
  | 9 | 64 s | BUILD SUCCESSFUL | 1204 | 0 | 0 | 0 | 109 | 0.024 s | 0.002 s |
  | 10 | 61 s | BUILD SUCCESSFUL | 1204 | 0 | 0 | 0 | 109 | 0.031 s | 0.002 s |

  Counts are tallied from the JUnit XML of both test tasks, not from Gradle's console line. 1204 = the 1194 baseline + the double's 10 contract tests; 109 classes = 108 + 1.
- **Timing, before and after** (the tech-lead asked for the cost or the saving — it is a saving):
  - suite, whole run: **68.3 s before** (one baseline run at `develop`, 1194 tests) → **59.5 s mean after** (52–70 s over ten runs, 1204 tests). Roughly **9 s faster despite 10 more tests**, because 10 classes stopped creating and fsyncing ~130 temp preference files.
  - `InterstitialCadenceStoreTest`: **0.051 s before** → **0.024–0.057 s after** (mean 0.029 s).
  - the formerly-flaky `theSessionCountIsInMemoryOnlySoASecondHolderReadsItBackAsZero`: **0.003 s before** → **0.002–0.005 s after**. The reported failure was 60.041 s; the test's *normal* cost was never the problem and is unchanged.
- `git diff develop...HEAD --stat -- gradle/ '*.gradle.kts' gradle.properties settings.gradle.kts` — **empty** (zero build-file change, zero new dependency coordinate, so no licence row and no copyleft question).
- `git diff develop...HEAD | grep -cE '^\+.*(@Suppress|@Ignore|Disabled|baseline)'` — **0**. Nothing suppressed, nothing ignored, no baseline. No production source touched: every changed file is under `app/src/test/`.
- `@Test` count per touched file, `HEAD` vs worktree — **identical in all 11** (19/6/21/13/4/7/39/11/6/6/54). No test lost, renamed away or merged.
- Migration diff is **behaviour-neutral by construction**: every added line across the ten classes is the double's name, a KDoc correction, or one line-wrap — `git diff -U0 | grep '^+' | grep -vE 'InMemoryPreferencesDataStore|^\+ \*|^\+$'` returns **one** line, the wrapped `freshCadenceStore()` signature. Net −127 lines.
- **Not run, by the task's constraint:** the full `./gradlew check` (the producer owns it), `:app:lint`, any release assembly, anything touching adb or an emulator (none available).

## Open issues / risks
- **The reproduction is not mine and I make no claim about it.** I never saw the 60.041 s failure, and ten green runs do not prove a 1-in-N hang is gone — they prove the pattern that permitted it no longer exists in test sources, which is the falsifiable part. The qa-engineer owns the reproduction attempt and the acceptance bar; if it reproduces after this, the mechanism was never the store.
- **The double is now infrastructure under ~200 tests.** That is the risk the tech-lead named, and the mitigation is its own test class — which is only worth what its tests discriminate, so both properties were watched to fail against variants. If a future change to the double is not accompanied by a test that fails without it, that mitigation is gone.
- **`ProgressSubmitterCloudTest`'s private `inMemoryDataStore()` should be folded into the shared double** (`:1344`). Not a determinism defect and deliberately left alone, but it is an untested, non-atomic duplicate of shared test infrastructure — the shape from which the next divergence grows. Small, and for the producer to schedule.
- **`local.properties` had to be copied into this worktree** from the main checkout for the Android SDK path; without it `:app:testDebugUnitTest` cannot even configure (`SDK location not found`). It is git-ignored and was not committed. Worth knowing for any future worktree: a fresh one cannot run the app-module tests until it is copied.
- **The formerly-flaky test's name still says nothing about the store**, which is correct — its subject is D-13's persisted/in-memory split, not the store. Noting it so no one reads the word "InMemory" in the test name as referring to this double.

## Handoff
Next: **code-auditor** — review `develop..HEAD` (944821f..fa98d17 plus this report) on `feature/phase-14-datastore-determinism`. The two things worth their time: (1) whether `InMemoryPreferencesDataStore`'s contract test actually discriminates — the three failure runs above are the evidence, and re-deriving one of them is cheap; (2) whether the migration is truly behaviour-neutral, for which the `git diff -U0` filter and the per-file `@Test` counts above are the fastest route.
Then: **qa-engineer** — owns the R-3 acceptance evidence per the tech-lead's ruling: the bounded reproduction attempt (a thread dump if it hangs), the N ≥ 10 consecutive green full-suite runs at CI's worker count, and the grep proving no `PreferenceDataStoreFactory.create` without an explicit `scope` survives in test sources. My ten runs are at `--max-workers=4` on one machine and are the developer's evidence, not the acceptance evidence.
Also for the **producer**: R-3 is independent of R-1/R-2 and touches no file they touch, so this branch can merge to `develop` in either order.
