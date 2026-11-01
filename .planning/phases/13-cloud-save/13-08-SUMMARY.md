---
phase: 13-cloud-save
plan: 08
subsystem: data
tags: [kotlin, coroutines, cloud-save, play-games, orchestration, junit4, truth, datastore]

# Dependency graph
requires:
  - phase: 13-cloud-save
    plan: "02"
    provides: "CloudSyncStore.pushPending and CloudProfileStore.mergedPlayerId — the two pieces of device-local cloud state, both open so the pass can be tested over MutableStateFlows"
  - phase: 13-cloud-save
    plan: "04"
    provides: "CloudProgressCodec.encodeWithinCap / decode / mergeBytes — the trust boundary the pass reads and writes bytes through"
  - phase: 13-cloud-save
    plan: "05"
    provides: "ProgressRepository.exportProgress / importProgress / resetAllProgress and ChallengeProgress.mergeLadder / settledLevel / resetAwaiting — the local half of a sync pass"
  - phase: 13-cloud-save
    plan: "06"
    provides: "CloudSaveStore + CloudSaveDescription (the SDK-free seam) and FakeCloudSaveStore (the recording double every case here drives)"
  - phase: 12-leaderboards-achievements
    provides: "ProgressSubmitter itself — the single session collector, the pass mutex, SubmissionSyncStore and the attempt/recordPassOutcome idioms this plan extends"
  - phase: 11-play-games-session
    provides: "GamesService.session and GamesSignInState.SignedIn(playerId, displayName) — the identity the profile gate compares and the trigger the pass hangs off"
provides:
  - "ProgressSubmitter.submitAll as five ordered steps: session gate, profile gate, pull-once, Phase 12 submissions, cloud push"
  - "ProgressSubmitter.profileSwap: StateFlow<ProfileSwapRequest?> — the D-12/D-15 account-switch question, raised before a single byte moves"
  - "ProgressSubmitter.onProfileSwapResolved(keepLocalProgress) — D-13's merge and D-14's full wipe, then a normal pass"
  - "ProfileSwapRequest(playerId, displayName) — the one type plan 13-10's HomeViewModel consumes"
  - "attemptOrNull — the null-shaped sibling of attempt, so a throwing CloudSaveStore is indistinguishable from an empty one"
affects: [13-10, 13-11, cloud-save, profile-swap, home]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "A dedicated once-per-process AtomicBoolean consumed INSIDE the guarded work rather than around the collector, because a StateFlow replays to its one collector"
    - "A per-pass memo object (PassAggregates) so two optional steps read the same storage snapshot at most once and can never describe a different instant than they submitted"
    - "Resolution work under a non-reentrant mutex that RELEASES before triggering the follow-up pass"
    - "Mutation-verified regression test: the settled-ladder case was proven to fail when settledLevel() is swapped for the cached StateFlow read"

key-files:
  created:
    - app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterCloudTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/data/ProgressSubmitter.kt
    - app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt

key-decisions:
  - "pullOnce reports `a decodable payload came back`, not `the pull ran` and not `the import changed something`. `The pull ran` would make every clean launch push, breaking the half of assumption A7 that is NOT relaxed; `changed` would skip the write-back exactly when the CLOUD is the side that is behind."
  - "The A7 change is scoped to step 3 only: `onlyIfPending` no longer gates the pull or the push, and the class KDoc says so in its own section so a later reader cannot read it as an accident."
  - "aggregate()/classicBests() are read through a per-pass memo rather than at two call sites, so a pass reads storage at most once and the listing description always describes the same instant the submissions did — this is also what keeps ProgressSubmitterTest's passCount assertions exactly as they were."
  - "attemptOrNull is a separate three-line function rather than a generic bending of attempt, because the two differ in what 'it failed' IS — collapsing them would put a default at the call site, which is where a `false` would one day be chosen for a ByteArray? and an empty payload pushed over a real cloud save."
  - "onProfileSwapResolved has no notion of 'dismissed': D-13's back/tap-outside → Keep mapping lives once, at the UI layer (plan 13-10)."
  - "TooManyFunctions and LongParameterList suppressed together on ProgressSubmitter with a 'Size suppressions' KDoc section, following the GameViewModel / PlayGamesService / DefaultProgressRepository precedent — splitting it would mean a second object holding part of the ordering, and the ordering IS the specification."

patterns-established:
  - "A build-gate-friendly KDoc: the paragraph explaining why the cached ladder read is wrong deliberately does not spell the token the grep gate looks for, mirroring how the no-logging paragraph avoids naming the logger"
  - "An in-memory DataStore that commits on the calling coroutine, so a DataStore-backed collaborator stays inside the StandardTestDispatcher's virtual time while its stateIn cache remains a separate coroutine — which is what makes the settled-versus-cached distinction observable at all"

requirements-completed: []

# Metrics
duration: 95min
completed: 2026-09-15
---

# Phase 13 Plan 08: Cloud Pull, Push and the Profile Gate Summary

**The sync pass now decides whose device this is before a single byte moves, pulls once per process regardless of any dirty flag, catches the leaderboards up in the same pass, and writes the union back — with every cloud failure recorded as a retry rather than surfaced.**

## Performance

- **Duration:** ~95 min
- **Tasks:** 3 of 3
- **Files modified:** 4 (1 created, 3 modified)
- **Tests:** 790 app unit tests green across 68 classes; 26 of them new here, plus 20 pre-existing `ProgressSubmitterTest` cases unchanged and still green

## Accomplishments

- **The profile gate is step 1, above the pull — the phase's one unrecoverable mistake, closed (T-13-28, Pitfall 8).** A stored player id that disagrees with the signed-in one raises `profileSwap` and returns having made **zero** store calls, asserted over `loadCallCount` *and* `saveCallCount` because "zero store calls" is a claim about the pair. The source-order assertion holds too: inside `submitAll`, `cloudProfileStore.mergedPlayerId` is read at relative line 17 and `cloudSaveStore.load` is not reached until line 54.
- **The pull runs exactly once per process, on its own guard (D-02, Pitfall 6).** `pulled: AtomicBoolean` is consumed *inside* the pass rather than around the collector, because a `StateFlow` replays to its one collector and the real session legitimately emits `SignedIn` twice on a launch. Two emissions produce one `load()`.
- **A clean launch by a signed-in player now pulls — the A7 change, reviewed and documented.** `onlyIfPending` became a guard around the Phase 12 submissions only. The submission half's "clean launches make no calls" property is untouched and still asserted by the pre-existing tests; the cloud half deliberately makes one read. A clean launch still does **not** push: that needed a third test to pin, and it has one.
- **An import that improved anything re-fires the submission pass in the same pass.** The submissions flag is set inside the pull, before step 3 reads it, so a restored player's leaderboards catch up immediately rather than lagging until their next solve (the plan's resolved discretion, RESEARCH open question 2).
- **Every cloud failure shape is the same failure (CLOUD-05, T-13-29).** `null`, undecodable bytes, `false` and a seam that *throws* all leave local play untouched and surface nothing; `attemptOrNull` is what makes the throwing case indistinguishable from the empty one. `CancellationException` is the single exception and is rethrown — pinned by asserting that the write which *would* have followed never lands.
- **One pass converges two devices (D-01).** The push runs after the pull, over `settledLevel()` rather than the eventually-consistent cache, through `CloudProgressCodec::mergeBytes`. **This was verified by mutation:** swapping `settledLevel()` for the cached read makes `aPassThatImportedALadderPushesThatLadderBackRatherThanTheCachedOne` fail and nothing else, so the prior-wave obligation from 13-05 is discharged by a test that genuinely regresses.
- **The conflict lambda is proven to be the codec's own union, not a second implementation.** The server gains a payload *after* this process's single pull, so the union in the recorded bytes cannot have come from the pushed side — it can only have come from `mergeRemote`.
- **D-14's wipe has exactly one call site in `app/src/main`.** `grep -rn "resetAllProgress()" app/src/main/java` returns the interface declaration, the repository implementation and `ProgressSubmitter`'s single call inside `applySwapAnswer` — nothing else can reach the app's only destructive path (T-13-31).
- **No deadlock (T-13-32).** `applySwapAnswer` completes and releases the non-reentrant `passMutex` before `submitAll` is called; the whole suite completing under the virtual-time harness rather than hanging is that claim.
- **Still no logging in `data/`.** `grep -rn "Log\.\|println\|Timber" app/src/main/java/com/app/pegsolitaire/data/` returns 0 hits, which now covers the player id compared in step 1 and the payload figures pushed in step 4 (T-11-04 / T-13-14).

## Task Commits

1. **Task 1: reorder the pass — session gate, profile gate, pull-once** — `b63e44a` (feat)
2. **Task 2: the cloud push, its metadata and its retry marker** — `a2e9005` (test)
3. **Task 3: profile-swap resolution — Keep merges, Use cloud replaces** — `8360e84` (test)

## Files Created/Modified

- `app/src/main/java/com/app/pegsolitaire/data/ProgressSubmitter.kt` — constructor widened with `CloudSyncStore`, `CloudProfileStore`, `CloudSaveStore` and `ChallengeProgress` (`@param:ApplicationScope scope` still last). New: `pulled`, `_profileSwap`/`profileSwap`, `onProfileSwapResolved`, `applySwapAnswer`, `pullOnce`, `land`, `submitPhase12`, `pushIfNeeded`, `attemptOrNull`, plus the top-level `ProfileSwapRequest`, `PassTotals` and `PassAggregates`. `submitAll` is now five numbered, commented steps. The class KDoc gained sections for the five-step order, the A7 change and the size suppressions.
- `app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterCloudTest.kt` — **new**, 26 cases in four groups: the session gate, the profile gate (D-12/D-15), the once-per-process pull and its failure shapes (D-02/CLOUD-05), the push and its marker (D-01/D-11), and both answers to the swap dialog (D-13/D-14). Fakes: `FakeCloudRepository` (a *real* `ProgressMerge` merge, so a payload assertion can tell a union from a half), `FakeSyncStore`, `FakeCloudSyncStore`, `FakeCloudProfileStore`, an in-memory `DataStore` and the shipped `FakeCloudSaveStore` from 13-06.
- `app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterTest.kt` — `submitterFor` supplies the four new collaborators in their quietest form (`InertCloudSyncStore`, `InertCloudProfileStore`, an empty `FakeCloudSaveStore`, a `ChallengeProgress` over the never-readable store); `FakeProgressRepository.exportProgress` returns the fresh-player payload instead of throwing, because a solve-triggered pass now pushes. **No existing assertion changed.**
- `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt` — the same widening for its two `ProgressSubmitter` constructions, and `FakeProgressRepository.exportProgress` made benign for the same reason.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `ResultsViewModelTest` would not compile against the widened constructor**
- **Found during:** Task 1
- **Issue:** `ResultsViewModelTest` constructs `ProgressSubmitter` twice — once directly (`submitterOverAThrowingSeam`) and once through a `RecordingProgressSubmitter` subclass — so widening the constructor broke `compileDebugUnitTestKotlin` for the whole module.
- **Fix:** Both sites take the four new collaborators. The throwing-seam fixture gets real `CloudSyncStore`/`CloudProfileStore` over the same throwing DataStore, keeping that fixture's "every store is hostile" property; the recording subclass mirrors the superclass's parameter list and carries the matching `LongParameterList` suppression.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt`
- **Commit:** `b63e44a`

**2. [Rule 1 - Bug] `exportProgress` throwing in two test doubles would have taken a test scope down**
- **Found during:** Task 1
- **Issue:** Both `FakeProgressRepository`s declared `exportProgress` as `error("the cloud pass lands in plan 13-08")`. The cloud pass *is* plan 13-08, and a solve-triggered pass now pushes — so the throw would land in an application-scoped coroutine with no handler, cancelling the test scope. That is precisely the failure mode CLOUD-05 forbids.
- **Fix:** Both return `CloudProgress(ladder = ladder)`, the fresh-player shape, with KDoc explaining why it is not a throw.
- **Files modified:** `ProgressSubmitterTest.kt`, `ResultsViewModelTest.kt`
- **Commit:** `b63e44a`

### Plan Interpretations

**3. `pushIfNeeded` was implemented in task 1 rather than stubbed**
The plan asked task 1 to leave `pushIfNeeded` a `Unit`-returning stub "so this task's diff stays reviewable", with task 2 filling it in. It was implemented up front instead, because the stub would have made task 1's *own* acceptance criterion — the profile gate yielding `saveCallCount == 0` — vacuous: with no push anywhere, no pass can ever call `save()`. Tasks 2 and 3 therefore land as `test(...)` commits over already-working code rather than `feat(...)` commits. The same reasoning applies to `onProfileSwapResolved`, which references `pulled`, `_profileSwap` and the pass itself and is not separable from the restructure without a stub of its own.

**4. `pushIfNeeded`'s `pulled` parameter is named `pulledPayload` and means "a decodable payload came back"**
The plan's literal wording ("`pulled`") both shadows the `pulled: AtomicBoolean` field and leaves the semantics open. Two readings were rejected in the KDoc rather than silently:
- "the pull step ran" would make **every** clean launch push. That breaks the half of assumption A7 the plan explicitly preserves, and it would have broken `ProgressSubmitterTest.aSignedInResolutionWithTheFlagClearRunsNoPassAtAll` — an existing D-10 assertion the plan told this executor not to change.
- "the import changed something" would skip the write-back whenever the remote was a subset of local, which is exactly the case where the **cloud** is the side that is behind.

**5. `REQUIREMENTS.md` deliberately not updated**
CLOUD-01/02/03/05 are shared with plans 13-07 and 13-09; 13-07 is running in parallel in a sibling worktree, and the requirements are not satisfied end-to-end until 13-09 replaces `NoOpCloudSaveStore` with the real `PlayGamesCloudSaveStore`. Marking them here would both risk a merge conflict and be premature. Left for the orchestrator / a later plan.

### Deferred Issues

None.

## Verification Results

All commands run from the worktree root.

| Command | Result |
|---|---|
| `./gradlew :app:testDebugUnitTest --tests "*ProgressSubmitter*"` | **PASS** — `ProgressSubmitterCloudTest` 26/26, `ProgressSubmitterTest` 20/20, 0 failures, 0 errors |
| `./gradlew :app:testDebugUnitTest --tests "*PlatformBoundaryTest*"` | **PASS** — 3/3; `data/` still names no gms type and no `Activity` |
| `./gradlew :app:testDebugUnitTest` (full suite) | **PASS** — 790 tests across 68 classes, 0 failures, 0 errors |
| `./gradlew :domain:test` | **PASS** |
| `./gradlew :app:detekt :app:ktlintCheck` | **PASS** |
| `./gradlew :domain:detekt :domain:ktlintCheck` | **PASS** |
| `grep -c "gamesService.session.collect" ProgressSubmitter.kt` | **1** — still exactly one session collector (WR-01) |
| `awk '/fun submitAll/,/^        }/' … \| grep -n "mergedPlayerId\|cloudSaveStore.load"` | first hit is `mergedPlayerId` (line 17) before `cloudSaveStore.load` (line 54) |
| `grep -c "currentLevel.value" ProgressSubmitter.kt` | **0** |
| `grep -c "CloudProgressCodec::mergeBytes" ProgressSubmitter.kt` | **2** (the pull's conflict lambda and the push's) |
| `grep -c "challengeProgress.settledLevel()" ProgressSubmitter.kt` | **1** |
| `grep -c "suspend fun onProfileSwapResolved" ProgressSubmitter.kt` | **0** — it is not `suspend` |
| `grep -rn "resetAllProgress()" app/src/main/java` | declaration + implementation + exactly one call site (`ProgressSubmitter.applySwapAnswer`) |
| `grep -rn "Log\.\|println\|Timber" app/src/main/java/com/app/pegsolitaire/data/` | **0 hits** |

**Mutation check.** `settledLevel()` was temporarily replaced with the cached `currentLevel` read and the cloud suite re-run: exactly one case failed (`aPassThatImportedALadderPushesThatLadderBackRatherThanTheCachedOne`). The mutation was reverted and the suite re-run green. The prior-wave ordering obligation from 13-05 is therefore protected by a test that actually regresses, not merely by one that passes.

## Threat Model Coverage

| Threat ID | Disposition | Evidence |
|---|---|---|
| T-13-28 (account switch merges A into B) | mitigated | Source-order assertion (`mergedPlayerId` before `cloudSaveStore.load`) plus `aDifferentStoredPlayerIdRaisesTheSwapAndTouchesNeitherStore` asserting `loadCallCount == 0` and `saveCallCount == 0` |
| T-13-29 (SDK exception in an app-scoped coroutine) | mitigated | `attempt` / `attemptOrNull`; `aThrowingStoreIsIndistinguishableFromAnEmptyOne`, `aThrowingPushIsTreatedExactlyAsAReportedFailure`, `aCancellationFromTheStoreIsRethrownRatherThanClassifiedAsAFailure` |
| T-13-30 (a failed push silently forgotten) | mitigated | `aPushThatDidNotLandIsRememberedForTheNextPass`, `aPushThatLandedClearsTheMarker`, `anOutstandingRetryMarkerPushesOnTheNextStartTriggeredPass` |
| T-13-31 (the D-14 wipe reachable from elsewhere) | mitigated | Single-call-site grep over `app/src/main`; the call is inside `applySwapAnswer`, itself reachable only after the step-1 gate raised the dialog |
| T-13-32 (deadlock of the process-lifetime singleton) | mitigated | The lock is released before the follow-up pass; the cloud suite completes under virtual time rather than hanging |
| T-13-33 (player id or payload bytes in logcat) | mitigated | Zero logging hits under `data/` |

## Known Stubs

None introduced by this plan. `CloudSaveStore` is still bound to `NoOpCloudSaveStore` in `PlatformModule` — that is 13-06's documented mid-phase scaffolding, replaced by plan 13-09, and is not a stub this plan added or is responsible for.

## Threat Flags

None. No new network endpoint, auth path, file access pattern or schema change was introduced; the only new trust-boundary traffic goes through `CloudProgressCodec`, which plan 13-04 already validates.

## For the Next Plan (13-10)

`HomeViewModel` consumes exactly two members and nothing else:

- `ProgressSubmitter.profileSwap: StateFlow<ProfileSwapRequest?>` — `null` means no question; non-`null` carries the **arriving** account's `playerId` and (possibly `null`) `displayName`.
- `ProgressSubmitter.onProfileSwapResolved(keepLocalProgress: Boolean)` — non-`suspend`, returns immediately.

Two things the UI layer owns and this class deliberately does not:

1. **Dismissal.** Back, tap-outside and any other dismissal must map to `onProfileSwapResolved(keepLocalProgress = true)`. `ProgressSubmitter` has no "dismissed" case precisely so the safe default is expressed exactly once.
2. **Placement (D-15).** Home only, and never stacked with the first-run onboarding overlay or the Phase 11 opt-in prompt.

## Self-Check: PASSED

- `app/src/main/java/com/app/pegsolitaire/data/ProgressSubmitter.kt` — FOUND
- `app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterCloudTest.kt` — FOUND
- `app/src/test/java/com/app/pegsolitaire/data/ProgressSubmitterTest.kt` — FOUND
- `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt` — FOUND
- Commit `b63e44a` — FOUND
- Commit `a2e9005` — FOUND
- Commit `8360e84` — FOUND
