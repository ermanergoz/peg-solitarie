---
phase: 13-cloud-save
plan: 06
subsystem: infra
tags: [play-games, saved-games, cloud-save, hilt, seam, archunit, junit4, truth, test-double]

# Dependency graph
requires:
  - phase: 11-play-games-session
    provides: "GamesService / NoOpGamesService / GamesConstants / FakeGamesService — the seam KDoc voice, the failure-as-return-value contract, the scoped-constants shape and the record-and-assert fake style all copied verbatim; plus the KDoc sentence forbidding cloud save on that seam, which this plan honours literally"
  - phase: 09-ads
    provides: "PlatformModule's SDK-containment rule and the empirical finding (plan 09-05) that the Hilt-generated component carries a Provider<T> per binding, so the bound type must be SDK-free wherever it is declared"
  - phase: 13-cloud-save
    provides: "13-01's CloudProgress / ProgressMerge — the pure :domain merge that the mergeRemote lambda will carry across this seam as bytes"
provides:
  - "CloudSaveStore — the ByteArray-in/ByteArray-out cloud seam; no Activity type and no Play Games SDK type in any signature"
  - "CloudSaveDescription — three ints (ladder, totalStars, classicCleared), so data/ never touches a Context and the listing line follows the device locale (D-11)"
  - "CloudSaveConstants — SNAPSHOT_NAME 'progress' / MAX_CONFLICT_RETRIES 10 / COVER_PX 512 / MAX_DESCRIPTION_CHARS 100"
  - "NoOpCloudSaveStore — the silently-degrading stub bound in PlatformModule until plan 13-09"
  - "PlatformModule.bindCloudSaveStore — the temporary @Binds that lets wave 3 compile and run green before any SnapshotsClient exists"
  - "FakeCloudSaveStore — the record-and-assert fake whose save() invokes mergeRemote, the only place in the JVM suite where the conflict merge actually runs"
affects: [13-07, 13-08, 13-09, 13-10, 13-11, cloud-save, saved-games, conflict-resolution]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Sibling platform seam per concern rather than growing an existing one: who-is-the-player and what-bytes-are-theirs are separate interfaces"
    - "Pure merge passed across a platform seam as a (ByteArray, ByteArray) -> ByteArray lambda, so platform/ never learns the payload's shape"
    - "Listing metadata crosses the seam as raw numbers, never a formatted string, keeping strings.xml the only place wording lives"
    - "A deliberately temporary @Binds, KDoc'd as scaffolding with the plan that replaces it named, so an intermediate commit cannot be misread as a shipped decision"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/platform/games/save/CloudSaveStore.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/save/CloudSaveConstants.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/save/NoOpCloudSaveStore.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/save/FakeCloudSaveStore.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/save/FakeCloudSaveStoreTest.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/save/NoOpCloudSaveStoreTest.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/save/CloudSaveConstantsTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt

key-decisions:
  - "Cloud save is a sibling CloudSaveStore in platform/games/save/, never a saveToCloud() on GamesService — that seam's own KDoc forbids it, and the two answer different questions"
  - "mergeRemote is a parameter on BOTH load and save: an open can come back conflicted on a read, and resolving a read-conflict with anything other than the real merge would discard a device's progress during a pull"
  - "Snapshot identity is one fixed name ('progress') with createIfNotFound = true; 'multiple snapshots exist' is not handled, because the requirements reserve exactly one cloud-save dialog and it is the profile swap"
  - "CloudSaveDescription carries three ints rather than a formatted string, so data/ never touches a Context and the description follows the device locale at commit time (D-11)"
  - "The RESEARCH sketch's load() signature was superseded by the plan's load(mergeRemote) — the read-conflict case is real and the sketch did not cover it"
  - "FakeCloudSaveStore.save invokes mergeRemote deliberately; a fake that ignored the lambda would leave the conflict merge unexercised and let a broken merge pass every test in the project"
  - "FakeCloudSaveStore.remote advances only on a successful save, so a failed push leaves the server where it was and the next pass merges against the same bytes"
  - "Pitfall 4 (offline commitAndClose reports success) is recorded in the seam KDoc rather than worked around — GMS owns that retry queue and exposes no API to observe it"

patterns-established:
  - "Grep-gate hygiene extends to prose: no gms package name appears even in a KDoc inside platform/games/save/, so the SDK-containment grep stays a clean zero"
  - "A test double that carries behaviour (not just recording) gets its own contract test, because downstream suites assert through it"

requirements-completed: []

# Metrics
duration: 18min
completed: 2026-09-15
---

# Phase 13 Plan 06: Cloud Save Seam Summary

**A `CloudSaveStore` seam that carries bytes in and bytes out — with the pure merge passed across it as a lambda — plus its scoped constants, a silently-degrading stub bound in Hilt, and a recording fake that runs the conflict merge on the JVM without the SDK.**

## Performance

- **Duration:** ~18 min
- **Started:** 2026-09-15T20:07Z
- **Completed:** 2026-09-15T20:25Z
- **Tasks:** 2 of 2
- **Files modified:** 8 (7 created, 1 modified)

## Accomplishments

- **The seam names only Kotlin/JVM types.** `load`, `save`, `CloudSaveDescription` — `ByteArray`, `Long`, `Boolean`, three `Int`s. No `Snapshot`, no `SnapshotsClient`, no `Activity`. `PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks` reads bytecode including the Hilt-generated `DaggerApp_HiltComponents_SingletonC`, so keeping the bound type SDK-free is what makes T-13-20 structural rather than a habit.
- **Failure is a return value on both halves.** `null` from `load` means *stay local* — explicitly not "the player has no progress", the misreading that would push an empty device's payload over a real save. `false` from `save` means *nothing reached the cloud* and is not an error: the caller sets the pending marker and retries next pass (CLOUD-05, T-13-21).
- **The conflict merge is testable on the JVM.** `FakeCloudSaveStore.save` invokes the caller's `mergeRemote` against its current `remote`, which is the only place in the whole suite where that lambda ever runs — in production it fires inside the SDK's resolution path, which no offline test can reach.
- **Wave 3 is unblocked.** `bindCloudSaveStore` resolves to the stub, so `ProgressSubmitter`'s cloud pass can be written, compiled and run green against the real Hilt graph before any Saved Games plumbing exists — the same order Phases 11 and 12 built their seams in, and the reason each SDK swap was later a one-line change.

## Task Commits

1. **Task 1: The seam, its constants, the NoOp stub and the Hilt binding** — `8d894e1` (feat)
2. **Task 2 (TDD RED): FakeCloudSaveStore / stub / constants tests, failing** — `11b5599` (test)
3. **Task 2 (TDD GREEN): FakeCloudSaveStore implemented** — `02ad3c9` (feat)

No REFACTOR commit — nothing needed cleaning after GREEN.

## Files Created/Modified

- `platform/games/save/CloudSaveStore.kt` — the interface and `CloudSaveDescription`; KDoc carries the SDK-containment rule, the failure-as-return-value contract, why `mergeRemote` is on both methods, why the description is three ints, and the Pitfall-4 note about offline commits reporting success.
- `platform/games/save/CloudSaveConstants.kt` — `SNAPSHOT_NAME` / `MAX_CONFLICT_RETRIES` / `COVER_PX` / `MAX_DESCRIPTION_CHARS`, each KDoc'd with why that value and where its number comes from.
- `platform/games/save/NoOpCloudSaveStore.kt` — `null` / `false`, neither override touching `mergeRemote`.
- `di/PlatformModule.kt` — `bindCloudSaveStore`, plus a class-KDoc bullet introducing the third *kind* of binding this module now holds: the deliberately temporary one.
- `test/…/save/FakeCloudSaveStore.kt` — four behaviour knobs (`remote`, `saveResult`, `throwOnSave`, `throwOnLoad`), five `private set` recorders, no MockK.
- `test/…/save/FakeCloudSaveStoreTest.kt` — 9 tests pinning the fake's own contract.
- `test/…/save/NoOpCloudSaveStoreTest.kt` — 4 tests, including the merge-never-invoked flag assertions.
- `test/…/save/CloudSaveConstantsTest.kt` — 4 tests; `SNAPSHOT_NAME` matched against `^[A-Za-z0-9\-._~]{1,100}$`.

## Decisions Made

Beyond the plan's pre-resolved discretion (sibling seam, one fixed snapshot name, temporary stub binding), two judgements were made during execution:

1. **`load` takes `mergeRemote`, superseding the RESEARCH sketch's `load()`.** The plan's `<interfaces>` block is right and the sketch was not: `open` can return a conflict on a *read*, and resolving it writes merged bytes back to the server. The KDoc now says so explicitly, because a future reader looking at a read-only method holding a merge lambda would otherwise be tempted to drop it.
2. **`FakeCloudSaveStore` counts the call before honouring `throwOnSave`/`throwOnLoad`.** A throwing store still received a call, and plan 13-08's Pitfall-8 assertion is "**zero** store calls" — an uncounted throwing call would make that assertion pass over a store that had in fact been reached.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical test coverage] Added `FakeCloudSaveStoreTest.kt`**
- **Found during:** Task 2
- **Issue:** The plan's `<files>` listed three test files, but its acceptance criteria include a concrete behavioural assertion about the fake itself — that `FakeCloudSaveStore(remote = "a").save(bytes, desc, 0L) { l, r -> l + r }` records `"a" + bytes` — and no file in the list was a home for it. Left unwritten, the fake's `mergeRemote` invocation (the one behaviour the plan calls load-bearing) would have had no test until plan 13-08 asserted through it, where a regression would surface as someone else's failing test.
- **Fix:** Added a fourth test file in the same new package, `FakeCloudSaveStoreTest.kt`, with 9 tests covering the merge invocation, the verbatim first-push case, the metadata recording, both call counters, `remote` advancing only on success, and both throwing knobs.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/platform/games/save/FakeCloudSaveStoreTest.kt`
- **Verification:** `./gradlew :app:testDebugUnitTest --tests "*FakeCloudSaveStoreTest*"` — 9 tests, 0 failures.
- **Committed in:** `11b5599` (RED) and `02ad3c9` (GREEN)

**2. [Rule 1 - Gate violation] Removed the gms package name from a KDoc inside the new package**
- **Found during:** Task 1
- **Issue:** `CloudSaveStore`'s KDoc originally quoted the SDK package root verbatim while explaining the containment rule, which made the plan's own acceptance grep (`grep -rn "com.google.android.gms\|android.app.Activity" platform/games/save/` returning 0) report a hit. Harmless to the compiler, but it would have made a grep-based gate that a later plan re-runs report a false positive forever.
- **Fix:** Reworded to "the Play/AdMob/UMP SDK package roots"; the rule is still named precisely, the literal string is gone.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/platform/games/save/CloudSaveStore.kt`
- **Verification:** the grep now returns 0; `ktlintMainSourceSetCheck`, `detekt` and `assembleDebug` re-run green.
- **Committed in:** `8d894e1`

**3. [Rule 1 - Bug] Un-nested `runTest` in the two throwing-knob tests**
- **Found during:** Task 2 (before GREEN)
- **Issue:** The two `assertThrows` tests wrapped a `runTest` inside an outer `runTest`, which is not a supported nesting.
- **Fix:** Made both test functions plain (non-`runTest`) bodies with the single `runTest` inside the `assertThrows` block.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/platform/games/save/FakeCloudSaveStoreTest.kt`
- **Verification:** both tests pass.
- **Committed in:** `02ad3c9`

---

**Total deviations:** 3 auto-fixed (1× Rule 2, 2× Rule 1)
**Impact on plan:** No scope change. One extra test file inside the plan's own new package; two corrections to code this plan wrote. Nothing outside `files_modified` was touched, and no file belonging to the parallel plans 13-04 or 13-05 was read or written.

## Issues Encountered

None. The RED phase failed as intended (unresolved `FakeCloudSaveStore`, i.e. a compilation failure across the package's shared test compilation unit) and GREEN passed on the first run after the nesting fix.

## Verification Results

All commands run from the worktree, results as observed:

| Command | Result |
|---|---|
| `:app:testDebugUnitTest --tests "*PlatformBoundaryTest*" --tests "*PlatformBoundaryGateTest*" --tests "*DomainPurityTest*"` | BUILD SUCCESSFUL |
| `:app:testDebugUnitTest --tests "*PlatformBoundary*" --tests "*DomainPurityTest*" --tests "*CloudSave*"` | BUILD SUCCESSFUL |
| `:app:testDebugUnitTest` (full app suite) | BUILD SUCCESSFUL |
| `:app:assembleDebug` | BUILD SUCCESSFUL — the Hilt graph resolves `CloudSaveStore` |
| `:app:detekt` `:app:ktlintCheck` | BUILD SUCCESSFUL |
| `grep -rn "com.google.android.gms\|android.app.Activity" platform/games/save/` | 0 hits |
| `grep -c "bindCloudSaveStore" di/PlatformModule.kt` | 1 |
| `grep -rn "fun load\|fun save" platform/games/GamesService.kt` | 0 hits — cloud save was NOT added to the existing seam |
| `grep -rn "Log\.\|println\|Timber" platform/games/` | 0 hits (T-11-04 extended to `save/` from its first commit) |
| Cloud-save test counts (JUnit XML) | `CloudSaveConstantsTest` 4, `NoOpCloudSaveStoreTest` 4, `FakeCloudSaveStoreTest` 9 — 17 tests, 0 failures, 0 errors |

**TDD gate compliance:** `test(13-06)` at `11b5599` (RED, compilation failure observed) → `feat(13-06)` at `02ad3c9` (GREEN). Both gates present, in order.

## Requirements

`requirements-completed` is deliberately empty. This plan's frontmatter names CLOUD-01 and CLOUD-05, but it delivers only the seam and a stub that returns `null`/`false` — no player's progress reaches or returns from the cloud until plan 13-09 binds `PlayGamesCloudSaveStore` and 13-08 drives the pass. Six other plans in this phase carry the same two IDs; marking them here would report the phase's headline requirement complete at the point where it is provably not. `REQUIREMENTS.md` was therefore left untouched (also avoiding a merge conflict with the parallel wave-2 worktrees, which name the same IDs).

## Threat Flags

None. The new package introduces no network endpoint, no auth path, no file access and no schema change; it declares a seam whose only implementation in this commit returns constants. The threat register's four entries (T-13-20/21/22/23) are all `mitigate` and all verified above.

## Next Phase Readiness

Ready. Plans 13-07 (description formatter + cover image), 13-08 (`ProgressSubmitter` push/pull pass) and 13-09 (`PlayGamesCloudSaveStore`) can all be written against these exact signatures:

```kotlin
suspend fun load(mergeRemote: (ByteArray, ByteArray) -> ByteArray): ByteArray?
suspend fun save(
    bytes: ByteArray,
    description: CloudSaveDescription,
    playedTimeMs: Long,
    mergeRemote: (ByteArray, ByteArray) -> ByteArray,
): Boolean
```

Two things plan 13-09 must not forget, both already KDoc'd at the seam: the binding in `PlatformModule` is temporary and must be repointed (the class KDoc and the binding KDoc both name 13-09 by number), and the real implementation must hold its `SnapshotsClient` internally and expose none, the way `PlayGamesService` and `AdMobAdGateway` already do — otherwise the boundary gate fails naming the generated Hilt component rather than the offending file.

## Self-Check: PASSED

All 7 created files verified present on disk; all 3 task commits (`8d894e1`, `11b5599`, `02ad3c9`) verified present in `git log`. No tracked file was deleted by any commit in this plan. `STATE.md` and `ROADMAP.md` were not modified — the orchestrator owns those after the wave merges.

---
*Phase: 13-cloud-save*
*Completed: 2026-09-15*
