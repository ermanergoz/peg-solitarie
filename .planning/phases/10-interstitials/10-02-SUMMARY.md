---
phase: 10-interstitials
plan: 02
subsystem: ads
tags: [interstitial, cadence, datastore, persistence, process-lifecycle, tdd, hilt, mockk]

# Dependency graph
requires:
  - phase: 10-interstitials
    plan: 01
    provides: "InterstitialCadenceState — the transitions, isNewSession and fromStored that this store delegates ALL of its arithmetic to"
  - phase: 05-persistence
    provides: "The shared app_prefs DataStore delegate, storageFailSafe, @ApplicationScope, and ChallengeProgress — the exact holder shape cloned here"
  - phase: 06-platform-foundation-build-hardening
    provides: "DefaultForegroundActivityProvider's @Singleton-on-the-class rationale, the PlatformBoundaryRules gates, and App's registration ordering contract"
provides:
  - "InterstitialCadenceStore — app_prefs-backed persistence of the four cadence scalars plus the in-memory session count (D-13)"
  - "InterstitialCadenceStore.state: StateFlow<InterstitialCadenceState> — the synchronously readable value the 10-04 trigger asks mayShow() of"
  - "InterstitialCadenceStore.recordWin / recordInterstitialShown / recordRewardedShown — the three write entry points downstream plans call"
  - "InterstitialCadenceStore.lastForegroundAtMs — the session-boundary stamp, kept off the state value because the cadence decision never reads it"
  - "AppForegroundTracker — ProcessLifecycleOwner ON_START/ON_STOP translated into the session boundary (D-16), registered in App.onCreate"
  - "androidx.lifecycle:lifecycle-process on the graph at the pinned atomic-group version"
affects: [10-03, 10-04, 10-05, 10-06, admob-gateway, results-trigger, rewarded-ad-path]

# Tech tracking
tech-stack:
  added:
    - "androidx.lifecycle:lifecycle-process 2.9.4 (version.ref = androidxLifecycle — no new version key)"
  patterns:
    - "Split-state holder: combine(dataStore.data, MutableStateFlow) so a persisted/in-memory split is invisible to callers"
    - "Store-delegates-arithmetic: every write rebuilds the state from the transaction's own preferences, applies a domain transition, and writes the fields back — the store itself computes nothing"
    - "Sequencing barrier in tests: assert a NEGATIVE by issuing a later write and awaiting that one, since DataStore serialises updateData"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/data/InterstitialCadenceStore.kt
    - app/src/main/java/com/app/pegsolitaire/platform/lifecycle/AppForegroundTracker.kt
    - app/src/test/java/com/app/pegsolitaire/data/InterstitialCadenceStoreTest.kt
    - app/src/test/java/com/app/pegsolitaire/platform/lifecycle/AppForegroundTrackerTest.kt
  modified:
    - gradle/libs.versions.toml
    - app/build.gradle.kts
    - app/src/main/java/com/app/pegsolitaire/App.kt

key-decisions:
  - "applyTransition writes exactly four fields and never interstitialsThisSession, so the D-13 split is enforced by the write path rather than by convention"
  - "recordInterstitialShown raises the in-memory session count SYNCHRONOUSLY, before the write is launched — the ad has already been on screen, so a slow or failed disk write must not let the D-04 cap under-count it"
  - "lastForegroundAtMs is a separate StateFlow, not a sixth state field, because mayShow never reads it"
  - "sessionCount.value = 0 is set inside the edit transform; safe under DataStore's retry semantics because the assignment is idempotent"
  - "AppForegroundTracker is injected into App as the CONCRETE type — it has no interface, so there is no binding to scope and @Singleton on the class is what guarantees one registration"

patterns-established:
  - "Two-halves test design for a lifecycle translator: mocked-store cases pin WHICH store call each event makes (and that it makes no other); real-store cases pin the resulting state"
  - "Wall-clock proved by window assertion: capture the stamp and assert before <= stamp <= after, which an elapsedRealtime value could never satisfy"

requirements-completed: [INT-02]

# Metrics
duration: 14min
completed: 2026-09-02
---

# Phase 10 Plan 02: Cadence Persistence and the Session Boundary Summary

**The interstitial cadence now remembers: four scalars in the shared `app_prefs` store (fail-open reads, best-effort writes, merges computed inside the transaction) plus an in-memory session count per D-13, with the 30-minute session boundary sourced from `ProcessLifecycleOwner` so a fullscreen ad can never forge a background event and disable the session cap.**

## Performance

- **Duration:** 14 min
- **Started:** 2026-09-02T20:53Z
- **Completed:** 2026-09-02T21:07Z
- **Tasks:** 2 (5 commits — RED/GREEN per task, plus a standalone dependency commit)
- **Files modified:** 4 created, 3 modified

## Accomplishments

- **INT-02's "survives being killed" is now an executable claim, not a design intent.** `recordWinPersistsSoASecondHolderOverTheSameStoreReadsItBack` constructs a *second* `InterstitialCadenceStore` over the same `DataStore` and reads back both counters — the only observation that actually proves process-death survival.
- **D-13's split is enforced by the write path, not by discipline.** `applyTransition` writes exactly four keys and structurally cannot write `interstitialsThisSession`, and the test `theSessionCountIsInMemoryOnlySoASecondHolderReadsItBackAsZero` asserts the second holder sees `0` for it while seeing the persisted stamp intact. Both halves of the decision are pinned by the same restart.
- **The store performs no cadence arithmetic whatsoever.** Every read goes through `InterstitialCadenceState.fromStored`; every write rebuilds the state from the transaction's own `preferences`, applies a 10-01 domain transition, and writes the resulting fields back. The only `+ 1` in the file is the in-memory session counter. Saturation, sentinels and clamping therefore have exactly one implementation, and it is the one with the 31-case truth table behind it.
- **T-10-11 is closed structurally.** `DefaultForegroundActivityProvider` clears on `onActivityPaused`, which every fullscreen ad triggers — sourcing the boundary from it would forge a "backgrounded" event on each interstitial and reset the D-04 cap on every show, leaving the cap permanently unable to bind. `ProcessLifecycleOwner` is the only event source used, and the tracker has **zero** bytecode references to the Activity provider (verified by `javap`).
- **Both hostile-store paths are proven, not asserted.** A store whose every read throws yields the default state, whose `mayShow` is false at every instant; a store whose every write throws drops `recordWin` and `recordForegroundLeft` without propagating onto the app-lifetime `SupervisorJob` scope (T-10-01, T-10-10).
- **The new dependency did not fracture the atomic group.** `lifecycle-process` is declared with `version.ref = "androidxLifecycle"` and no new version key, lands inside the existing resolution-strategy pin, and `:app:assembleDebug` is green.

## Task Commits

1. **Task 1 RED — the cadence store spec (13 failing cases)** — `1e5a1e8` (test)
2. **Task 1 GREEN — persistence, fail-open reads, best-effort writes** — `cda3998` (feat)
3. **Task 2 — declare `lifecycle-process` through the pinned atomic group** — `e183602` (chore)
4. **Task 2 RED — the foreground-tracker spec (6 failing cases)** — `aab509a` (test)
5. **Task 2 GREEN — `ProcessLifecycleOwner` boundary + `App` registration** — `e515827` (feat)

No REFACTOR commits: both GREEN implementations landed in the shape the plan specified and passed `detekt`/`ktlint` unmodified, so cleanup would have been churn.

The dependency declaration was split into its own `chore` commit rather than folded into either gate: it is build configuration with its own independent verification (`assembleDebug`), and a `test(...)` commit that also moves the dependency graph is a commit that cannot be reverted cleanly.

## Files Created/Modified

- `app/src/main/java/com/app/pegsolitaire/data/InterstitialCadenceStore.kt` — `InterstitialCadencePreferences` (five KDoc'd keys) and the `@Singleton` holder: `state` via `combine(dataStore.data.catch { … }, sessionCount)`, `lastForegroundAtMs`, and the five fire-and-forget writes, each with its own `storageFailSafe` guard.
- `app/src/main/java/com/app/pegsolitaire/platform/lifecycle/AppForegroundTracker.kt` — the `DefaultLifecycleObserver` that translates `ON_START`/`ON_STOP` into `startSessionIfGapElapsed` / `recordForegroundLeft`, with the D-16 anti-pattern, the wall-clock rationale and the `@Singleton`-on-the-class rationale all stated in KDoc.
- `app/src/test/java/com/app/pegsolitaire/data/InterstitialCadenceStoreTest.kt` — 13 cases over a real temp-file store plus `throwingDataStore()` / `throwingWriteDataStore()`.
- `app/src/test/java/com/app/pegsolitaire/platform/lifecycle/AppForegroundTrackerTest.kt` — 6 cases: two MockK call-translation proofs (including "and makes no other call"), four real-store session-state proofs.
- `gradle/libs.versions.toml` — `androidx-lifecycle-process` entry, `version.ref`, with a comment pointing at the atomic-group note.
- `app/build.gradle.kts` — `implementation(libs.androidx.lifecycle.process)` beside `lifecycle-runtime-compose`.
- `app/src/main/java/com/app/pegsolitaire/App.kt` — `@Inject lateinit var foregroundTracker`, `foregroundTracker.attach()` on the line after `activityProvider.attachTo(this)`, and a KDoc extension explaining why the app now registers two lifecycle observers, why they are not redundant, and why the `PlatformBoundaryTest` exemption is not widened.

## Decisions Made

- **The in-memory session count rises synchronously; only the disk write is deferred.** `recordInterstitialShown` calls `sessionCount.update { … }` before launching the write. An interstitial that reached the screen must count against D-04's cap immediately — deferring it behind IO would let a slow or failed write silently grant extra ads inside the same session, which is exactly the failure the cap exists to prevent.
- **`lastForegroundAtMs` stayed off the state value.** Adding it as a sixth field would have widened `InterstitialCadenceState`'s inputs beyond what `mayShow` consumes. The session transition is the only reader, so it gets its own flow.
- **`sessionCount.value = 0` is assigned inside the `edit` transform.** DataStore may re-run a transform on contention, so a side effect there must be idempotent — this assignment is. The alternative (deciding outside the transaction) would separate the boundary read from the write it authorises, reopening exactly the lost-update hole T-10-12 closes.
- **Negative assertions are sequenced behind a later write.** "This call changed nothing" can pass simply by running before the write it is meant to rule out. Every such case here issues a subsequent `recordWin()` and awaits *that*; since DataStore serialises `updateData`, observing the later write proves the earlier call already ran.
- **The D-13 kill-window residual is documented in the source, by name.** A force-quit and relaunch inside 30 minutes reports zero interstitials for what is still the same session. D-01 and D-02 still bind, so the cost is at most one extra ad per force-quit, and force-quitting in order to see *more* ads is not a threat model. The KDoc says so explicitly, and says not to "fix" it by persisting the count.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Recreated the gitignored `local.properties` in the worktree**

- **Found during:** Task 1 (RED), first `:app` Gradle invocation
- **Issue:** `local.properties` is gitignored, so a fresh worktree has none and every `:app` task fails with "SDK location not found". Same blocker 10-01 hit.
- **Fix:** Wrote `sdk.dir=/Users/ermanergoz/Library/Android/sdk` into the worktree root.
- **Files modified:** `local.properties` (gitignored — deliberately not committed; committing it would leak a machine-specific absolute path)
- **Verification:** `:app:testDebugUnitTest` then ran normally.
- **Committed in:** Not committed (gitignored by design).

**2. [Rule 3 - Blocking] Split the dependency declaration into its own commit**

- **Found during:** Task 2, at the RED gate
- **Issue:** Task 2's `<files>` mixes build configuration with TDD'd source. Folding `libs.versions.toml` + `build.gradle.kts` into the `test(...)` commit would produce a RED commit that also moves the dependency graph.
- **Fix:** Committed the catalog entry and the `implementation(...)` line as `chore(10-02)` (`e183602`) before the RED commit, keeping each commit independently revertible.
- **Files modified:** none beyond the plan's own list; only the commit boundary changed.
- **Verification:** `:app:assembleDebug` green — the artifact resolves under the atomic-group pin.
- **Committed in:** `e183602`

### Acceptance criterion adjusted (not met literally)

**Task 2 criterion: `grep -c 'ForegroundActivityProvider' AppForegroundTracker.kt` is 0 — actual is 3.**

This criterion and the same task's `<action>` are in direct conflict, and the conflict is unresolvable as written: the action **requires** the class KDoc to "carry the D-16 anti-pattern in full", naming `DefaultForegroundActivityProvider` as the wrong choice — and every accurate way of naming that type contains the substring the grep forbids.

The criterion's *intent* — the tracker must not **depend on** the Activity provider — is met and was verified more strictly than the grep would have:

- All three occurrences are on KDoc comment lines (15, 17, 44); there is no import and no code reference.
- `javap -v -p` on the compiled `AppForegroundTracker.class` finds **0** occurrences of the name in the constant pool, so there is no bytecode dependency at all.
- `PlatformBoundaryTest` (bytecode-based, and the actual gate) passes.

The KDoc was kept: naming the wrong choice is the whole point of the paragraph, since the wrong choice is the obvious one and a future reader who does not recognise it by name will make it.

---

**Total deviations:** 2 auto-fixed (2 blocking) + 1 acceptance criterion satisfied by intent rather than by literal grep.
**Impact on plan:** None on behaviour. One worktree environment file, one commit boundary, and one documentation-vs-grep conflict resolved in favour of the plan's own explicit `<action>` instruction.

## Issues Encountered

None beyond the above. Both RED gates behaved as intended — 13/13 and 6/6 failing with `kotlin.NotImplementedError`, never with a compile or syntax error, confirming the tests were linking against a real public surface before any implementation existed. No test passed unexpectedly during RED.

## TDD Gate Compliance

Two full cycles, verified in `git log`:

| Task | RED | GREEN | REFACTOR |
|------|-----|-------|----------|
| 1 — cadence store | `1e5a1e8` `test(10-02)` — 13 tests, 13 failed, all `NotImplementedError` | `cda3998` `feat(10-02)` — 13/13 pass, no test file touched | not required |
| 2 — foreground tracker | `aab509a` `test(10-02)` — 6 tests, 6 failed, all `NotImplementedError` | `e515827` `feat(10-02)` — 6/6 pass, no test file touched | not required |

## Verification

| Gate | Result |
|------|--------|
| `:app:testDebugUnitTest --tests '*InterstitialCadenceStoreTest'` | PASS — 13/13 |
| `:app:testDebugUnitTest --tests '*AppForegroundTrackerTest'` | PASS — 6/6 |
| `:app:testDebugUnitTest --tests '*PlatformBoundaryTest' --tests '*PlatformBoundaryGateTest' --tests '*DomainPurityTest'` | PASS |
| `:app:testDebugUnitTest` (whole module, regression check) | PASS |
| `:app:assembleDebug` | PASS — `lifecycle-process` resolves under the atomic-group pin |
| `:app:detekt :app:ktlintCheck` | PASS |
| `grep -c 'preferencesDataStore(' InterstitialCadenceStore.kt` | 0 (only `AppPreferences.kt` declares the delegate) |
| `grep -c 'storageFailSafe' InterstitialCadenceStore.kt` | 6 (floor: 5) |
| `grep -c 'InterstitialCadenceState' InterstitialCadenceStore.kt` | 17 (floor: 4) |
| `grep -n '+ 1' InterstitialCadenceStore.kt` | one line only — the `sessionCount.update` |
| `grep -c '^androidxLifecycle' libs.versions.toml` | 1 (no new version key) |
| `git diff --stat -- PlatformBoundaryRules.kt` | empty — no exemption widened |
| `javap` constant pool of `AppForegroundTracker.class` for `ForegroundActivityProvider` | 0 |

## Known Stubs

None. Every declared member is implemented; no placeholder value or "coming soon" string was introduced.

## Threat Flags

None. The plan's `<threat_model>` already registers this plan's only trust boundaries (`app_prefs` on disk, the lifecycle event source, the new Maven artifact), and each is closed by a test or by a resolved, cached first-party artifact. No network endpoint, auth path or schema was added.

## User Setup Required

None. No SDK is referenced by either file — the AdMob interstitial unit named in the phase's External Prerequisites is untouched by this plan.

## Next Phase Readiness

Ready. The cross-plan obligations this plan creates or inherits:

- **10-04 (the trigger)** reads `store.state.value.mayShow(nowMs)` and calls `store.recordInterstitialShown(nowMs)` **only if an ad actually presented** (D-11). It must also call `store.recordWin()` on every level completion in both modes — the store is the only thing that banks a win, and nothing else will.
- **The Phase 9 rewarded path** must call `store.recordRewardedShown(nowMs)` for D-15 to bind. This obligation was created by 10-01 and is still **unwired**: the store exposes the method, but nothing calls it yet. If it is missed, the rewarded-then-interstitial adjacency reopens silently with every test in this plan and 10-01 still green.
- **10-06 device UAT** confirms RESEARCH assumption A1: that a same-process fullscreen ad Activity does not cause `ProcessLifecycleOwner` to dispatch `ON_STOP`. It is marked `[ASSUMED]` in `AppForegroundTracker`'s KDoc and cannot be settled by a JVM test.
- **`App` registration is live** — `foregroundTracker.attach()` runs on every cold start, so the session boundary is already recording foreground exits before any interstitial code exists.

No blockers.

## Self-Check: PASSED

All four source files exist on disk at the claimed paths; commits `1e5a1e8`, `cda3998`, `e183602`, `aab509a` and `e515827` are present in `git log`. No claimed artifact is missing.

---
*Phase: 10-interstitials*
*Completed: 2026-09-02*
