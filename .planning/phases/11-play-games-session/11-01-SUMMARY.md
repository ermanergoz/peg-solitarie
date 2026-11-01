---
phase: 11-play-games-session
plan: 01
subsystem: platform
tags: [play-games, datastore, hilt, seam, test-double, kotlin]

# Dependency graph
requires:
  - phase: 06-platform-foundation-build-hardening
    provides: "GamesService / GamesSignInState / NoOpGamesService seam, PlatformBoundaryRules ArchUnit guard, play-services-games-v2 22.0.0 on the graph"
  - phase: 02-onboarding
    provides: "OnboardingRepository — the DataStore boolean-flag repository pattern PlayGamesOptInRepository mirrors"
provides:
  - "GamesService.bootstrapIfOptedIn() — the single documented opt-in gate every implementation must respect (D-01)"
  - "GamesService.signIn() return-vs-throw contract separating a silent decline from a genuine SDK error (D-10)"
  - "GamesSignInState.SignedIn.displayName: String? — the D-07 field with the D-14 degraded-name contract"
  - "PlayGamesOptInRepository + PlayGamesPreferences — atomic two-flag D-17 opt-in persistence"
  - "FakeGamesService — the shared test double plans 11-02/11-03/11-04 consume"
affects: [11-02-play-games-service, 11-03-home-opt-in-prompt, 11-04-settings-play-games-section, 12-leaderboards-achievements]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Opt-in gate declared on the seam interface, not buried in an implementation"
    - "Two-flag atomic DataStore write for a tri-state answer"
    - "Sealed-behavior test double (Success / Decline / Error) for a seam whose failure modes differ in UI consequence"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/data/PlayGamesPreferences.kt
    - app/src/main/java/com/app/pegsolitaire/data/PlayGamesOptInRepository.kt
    - app/src/test/java/com/app/pegsolitaire/data/PlayGamesOptInRepositoryTest.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/FakeGamesService.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/platform/games/GamesService.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/GamesSignInState.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/NoOpGamesService.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/NoOpGamesServiceTest.kt

key-decisions:
  - "PlayGamesOptInRepository is `open` with `open` members, matching OnboardingRepository, so plans 11-03/11-04 can subclass it in JVM tests — the codebase has no Hilt component tests to inject a fake through"
  - "Both D-17 flags are written in a single dataStore.edit block per answer, never two sequential edits, so promptAnswered and optedIn can never observably disagree mid-write"
  - "signIn()'s cancel-vs-error taxonomy is marked [UNVERIFIED against a live Play Console project — D-04] inline in the KDoc, following the AppForegroundTracker A1 precedent, rather than silently assuming a shape"
  - "FakeGamesService lives in platform.games rather than a consumer package because ui.home and ui.settings both need it"
  - "FakeGamesService deliberately does not re-implement the D-01 opt-in gate — tests push session via emitSession() so the gate is proven only where it lives (plan 11-02)"

patterns-established:
  - "Seam-level opt-in gate: an SDK-touching interface declares its own precondition method (bootstrapIfOptedIn) so no implementation can quietly bypass it"
  - "Atomic multi-key preference write: a tri-state answer encoded as two booleans must move together in one edit"
  - "Sealed SignInBehavior test double: outcomes that differ in UI consequence get distinct cases rather than a Boolean flag"

requirements-completed: [PGS-01, PGS-02, PGS-03]

# Metrics
duration: 25min
completed: 2026-09-04
---

# Phase 11 Plan 01: Play Games Seam Contracts and Opt-In Persistence Summary

**The Play Games seam now declares its own opt-in gate and a decline-vs-error sign-in contract, and the two-flag D-17 answer that gate reads is persisted atomically — so plans 11-02/03/04 have contracts, persistence and a test double to build against.**

## Performance

- **Duration:** ~25 min (excluding an 11m full `check` run)
- **Started:** 2026-09-03T20:41:00Z
- **Completed:** 2026-09-04T00:06:00Z
- **Tasks:** 3 of 3
- **Files modified:** 8 (4 created, 4 modified)

## Accomplishments

- **The opt-in gate is now a contract, not a convention.** `GamesService.bootstrapIfOptedIn()` is declared on the interface with KDoc stating that a player who never opted in must see no `PlayGamesSdk.initialize`, no `isAuthenticated` check and no SDK UI — so plan 11-02's implementation cannot quietly bypass D-01, and `NoOpGamesService` already satisfies it as a no-op.
- **A decline and an error are no longer the same outcome.** `signIn()` keeps its `Boolean` signature but its documented contract now splits them: `false` is a plain silent decline, a throw is a genuine SDK error that raises the D-10/D-18 snackbar. Callers in 11-03/11-04 can be written against this without inspecting SDK exception types.
- **D-17 persistence is proven, not just present.** `PlayGamesOptInRepository` encodes the three-state answer in two booleans, writes both atomically per answer, and fails open to "never asked, not opted in" — verified by 5 JVM tests including the D-19 decline-then-opt-in path.
- **`FakeGamesService` unblocks the rest of the phase.** With `SignInBehavior.Success/Decline/Error` and both call counters, the Home prompt and Settings row plans can assert every D-10 branch without a Play Console project (D-04).

## Task Commits

1. **Task 1: Extend GamesSignInState and GamesService with the D-01/D-07/D-10/D-14 contracts** — `cf2a3fc` (test, RED) → `e88f6e0` (feat, GREEN)
2. **Task 2: D-17 opt-in persistence — PlayGamesPreferences + PlayGamesOptInRepository** — `8d1bc70` (test, RED) → `900aefa` (feat, GREEN)
3. **Task 3: Shared FakeGamesService test double** — `dc49ffe` (test)

No REFACTOR commits were needed — each GREEN implementation landed in its final shape.

## Files Created/Modified

- `platform/games/GamesService.kt` — added `bootstrapIfOptedIn()`; rewrote `signIn()`'s KDoc with the return-vs-throw contract and the `[UNVERIFIED … D-04]` marker; interface KDoc now states the opt-in gate is part of the contract.
- `platform/games/GamesSignInState.kt` — `SignedIn` gains `displayName: String? = null`, documented as the D-14 degraded name that is never a sign-in failure and never returns the state to `Unknown`.
- `platform/games/NoOpGamesService.kt` — `bootstrapIfOptedIn()` is a no-op; `signIn()` documented as permanently the silent-decline branch.
- `platform/games/NoOpGamesServiceTest.kt` — added `bootstrapIfOptedInDoesNotThrow` (now 10 tests).
- `data/PlayGamesPreferences.kt` (new) — the two scoped `booleanPreferencesKey`s plus fail-open defaults, mirroring `OnboardingPreferences`.
- `data/PlayGamesOptInRepository.kt` (new) — `promptAnswered`/`optedIn` flows and the two atomic writers.
- `data/PlayGamesOptInRepositoryTest.kt` (new) — 5 tests: defaults, `setNotNow`, `setOptedIn`, decline-then-opt-in (D-19), fail-open read.
- `platform/games/FakeGamesService.kt` (new) — settable session, `SignInBehavior` sealed interface, `signInCallCount`/`bootstrapCallCount`.

## Decisions Made

- **`open` repository over the plan's plain `class`.** The plan's snippet showed a non-`open` class, but the file it says to model on (`OnboardingRepository`) is `open` precisely so `HomeViewModelTest`'s `FakeOnboardingRepository` can subclass it. Since this codebase constructs no Hilt component in tests (RESEARCH A3), a non-`open` repository would leave plans 11-03/11-04 with no way to drive the flags deterministically. Recorded as a deviation below.
- **KDoc-only change to `signIn()`.** D-10 needs a decline/error distinction, and the tempting move is a richer return type. The signature was left alone: the interface has zero production call sites today, so the contract could have been changed freely — but a `Boolean` plus a documented throw expresses the two branches without forcing every future caller through a result type, and matches how the callers in 11-03/11-04 actually branch.
- **The fake does not simulate the opt-in gate.** `bootstrapIfOptedIn()` only counts calls. Re-implementing D-01's branch in the fake would create a second copy of the phase's most important rule, which could drift green while production drifts wrong.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Worktree was missing the gitignored `local.properties`**
- **Found during:** Task 1 (first Gradle invocation)
- **Issue:** `./gradlew` failed with "SDK location not found" — `local.properties` is gitignored and so absent from a fresh worktree, and `ANDROID_HOME` is unset in this environment. No verification command in the plan could run.
- **Fix:** Copied the main checkout's `local.properties` into the worktree root. It stays untracked (line 7 of `.gitignore`) and was never staged.
- **Files modified:** none tracked.
- **Verification:** `./gradlew :app:compileDebugUnitTestKotlin --offline` then reached the Kotlin compiler and produced the expected RED error.
- **Committed in:** n/a — no tracked file changed.

**2. [Rule 2 - Missing critical functionality] `PlayGamesOptInRepository` made `open`**
- **Found during:** Task 2
- **Issue:** The plan specified `@Singleton class PlayGamesOptInRepository @Inject constructor(...)`. Non-`open`, it cannot be subclassed, and this project's ViewModel tests fake repositories exclusively by subclassing (`FakeOnboardingRepository : OnboardingRepository(NoopPreferencesDataStore)`) because no Hilt component is ever constructed in the JVM suite. Plans 11-03 and 11-04 both need to drive `promptAnswered`/`optedIn` deterministically, so shipping it closed would have blocked them.
- **Fix:** Declared the class and its four members `open`, and used the `@Inject internal constructor` form — i.e. mirrored `OnboardingRepository` exactly, which is what the plan's own `<read_first>` and 11-PATTERNS name as the model.
- **Files modified:** `app/src/main/java/com/app/pegsolitaire/data/PlayGamesOptInRepository.kt`
- **Verification:** `PlayGamesOptInRepositoryTest` (5 tests) green; `./gradlew check --offline` green.
- **Committed in:** `900aefa` (part of the task commit)

---

**Total deviations:** 2 auto-fixed (1× Rule 3 blocking, 1× Rule 2 missing critical functionality)
**Impact on plan:** None on scope. Deviation 1 was environmental and left no tracked change; deviation 2 is a one-keyword widening that conforms the new file more closely to the pattern file the plan already told it to mirror, and is required for the two downstream plans in this phase.

## Issues Encountered

- **The plan's Task 1 instructions contained a self-cancelling sentence** about a `sessionNeverCarriesADisplayName` test, resolving itself to "do not duplicate — only add the `bootstrapIfOptedIn` test". Followed as resolved: the existing `sessionNeverReportsAConnectedPlayer` already proves the stub never returns `SignedIn`, so only `bootstrapIfOptedInDoesNotThrow` was added.
- **The `GamesService` KDoc forward-references `PlayGamesOptInRepository`** (created one commit later, in Task 2). This is a KDoc link only — it does not affect compilation, and the reference resolves as of `900aefa`.
- Pre-existing `ExperimentalCoroutinesApi` opt-in warnings in `SettingsViewModelTest` and elsewhere were observed but are out of scope for this plan and were left untouched.

## Known Stubs

None introduced by this plan. `NoOpGamesService` remains the bound Slice-1 stub — that is its existing, intended role, and plan 11-02 replaces the binding.

## Threat Flags

None. The plan's threat register (T-11-01 accept, T-11-02/T-11-03 mitigate) covers everything this plan touches:
- **T-11-02 (information disclosure)** — mitigated as planned: the never-log discipline is stated in `bootstrapIfOptedIn()`'s KDoc, and neither `NoOpGamesService` nor `PlayGamesOptInRepository` logs anything.
- **T-11-03 (elevation of privilege)** — no `com.google.android.gms` import was added; `PlatformBoundaryRules` passed inside the green `./gradlew check --offline`.
No new network endpoint, auth path, file access pattern or schema change at a trust boundary was introduced beyond the two non-identifying booleans the plan already registered.

## Verification

- `./gradlew :app:testDebugUnitTest --tests "*.NoOpGamesServiceTest"` — 10 tests, 0 failures
- `./gradlew :app:testDebugUnitTest --tests "*.PlayGamesOptInRepositoryTest"` — 5 tests, 0 failures
- `./gradlew :app:compileDebugUnitTestKotlin` — exit 0
- `./gradlew :app:detekt ktlintCheck --offline` — green (mandatory CLAUDE.md gate)
- `./gradlew check --offline` — **BUILD SUCCESSFUL in 11m 8s** (full suite, including the ArchUnit `PlatformBoundaryRules` and Roborazzi screenshot tests)

## TDD Gate Compliance

Tasks 1 and 2 were `tdd="true"` and both show the required gate sequence in git log: a `test(...)` commit whose RED state was confirmed by a real compiler failure (`Unresolved reference 'bootstrapIfOptedIn'`, `Unresolved reference 'PlayGamesOptInRepository'`), followed by a `feat(...)` commit that turned it green. No test passed unexpectedly during RED. No REFACTOR gate was needed.

## User Setup Required

None for this plan. The phase-level D-04 debt is unchanged and still owed before release: create the Play Games project on the existing Play Console listing, register OAuth credentials for the debug-keystore and original release-keystore SHA-1s, and add a tester account. Until then, `signIn()`'s cancel-vs-error classification stays `[UNVERIFIED]` — which is now recorded inline in `GamesService.kt` rather than only in planning docs.

## Next Phase Readiness

Wave 1 is complete and the three dependent plans are unblocked:
- **11-02** implements `PlayGamesService` against `bootstrapIfOptedIn()`/`signIn()` and injects `PlayGamesOptInRepository` for the gate; the conservative cancel-vs-error classification it must write is already specified in the interface KDoc.
- **11-03** (Home opt-in prompt) has `promptAnswered`/`setNotNow()`/`setOptedIn()` plus `FakeGamesService` for the D-18 error-snackbar path.
- **11-04** (Settings row) has `SignedIn.displayName` for the D-06/D-14 copy split and `setOptedIn()` for the D-19 write.

No blockers. One thing for the phase verifier to note: `PlayGamesOptInRepository` is `open` rather than the plan's literal `class` — intentional, documented above as Rule 2.

## Self-Check: PASSED

All 8 source/test files claimed above exist on disk, and all 6 commits (`cf2a3fc`, `e88f6e0`, `8d1bc70`, `900aefa`, `dc49ffe`, `4b8ec17`) are present in git history. No missing items.

---
*Phase: 11-play-games-session*
*Completed: 2026-09-04*
