---
phase: 12-leaderboards-achievements
plan: 03
subsystem: platform
tags: [play-games-services-v2, leaderboards, achievements, compose, settings, mockk, robolectric]

# Dependency graph
requires:
  - phase: 11-play-games-session
    provides: "The GamesService seam, PlayGamesService's session/sign-in machinery, ForegroundActivityProvider (PLAT-01), GamesConstants, FakeGamesService and the PlatformBoundary bytecode gates"
  - phase: 06-slice-2-sdks
    provides: "play-services-games-v2:22.0.0 pinned on the compile classpath (plan 06-09)"
provides:
  - "A Boolean-returning, Activity-free GamesService seam: submission failure is now an observable event"
  - "suspend fun setAchievementSteps(achievementId, steps): Boolean with absolute-step semantics (incrementAchievement deleted)"
  - "PlayGamesService bodies for all six Phase-12 methods against the real *Immediate / Intent APIs"
  - "GamesConstants.LEADERBOARDS_REQUEST_CODE / ACHIEVEMENTS_REQUEST_CODE"
  - "FakeGamesService as the phase's recording fake (settable submissionResult + typed per-method call logs)"
  - "Signed-in-only Leaderboards and Achievements rows in Settings, reaching the stock Play Games UIs"
  - "D-16 discharged as zero code, with a grep gate keeping it zero"
affects: [12-04, 12-07, 12-08]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Submission seam returns Boolean (acceptance), never throws — failure is data the caller can act on"
    - "submitting {} helper: session gate -> bounded Activity await -> runCatching with CancellationException rethrown before classification"
    - "Stock-UI Intents obtained AND started inside platform/games; startActivityForResult with named request codes"
    - "Recording test fake logs call arguments in typed data classes, not counts or Pairs"

key-files:
  created: []
  modified:
    - app/src/main/java/com/app/pegsolitaire/platform/games/GamesService.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/NoOpGamesService.kt
    - app/src/main/java/com/app/pegsolitaire/platform/games/GamesConstants.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/FakeGamesService.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/NoOpGamesServiceTest.kt
    - app/src/test/java/com/app/pegsolitaire/platform/games/PlayGamesServiceTest.kt
    - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsRoute.kt
    - app/src/main/res/values/strings.xml
    - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsPlayGamesTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsPrivacyTest.kt

key-decisions:
  - "launchStockUi(requestCode, intentFor) WAS extracted — the two stock-UI launches share one helper (the plan left this to executor discretion)"
  - "Boolean means 'the SDK accepted it', explicitly NOT setStepsImmediate's Task<Boolean> payload (which means 'now unlocked')"
  - "Unknown takes the SignedOut branch in submitting {} — the SDK drops calls made before sign-in resolves"
  - "PlayGamesService carries @Suppress(\"TooManyFunctions\") with a why-comment, following the GameViewModel precedent, rather than raising the global detekt threshold"
  - "KDoc prose must not spell popup-API names literally — the D-16 grep gate is a literal grep, so even a 'this does not exist' mention trips it"

patterns-established:
  - "Pattern 1: submission methods report acceptance as Boolean and never throw; the caller answers false with the D-10 pending flag"
  - "Pattern 2: submitting {} orders session gate before Activity await before SDK call, so a signed-out player never waits"
  - "Pattern 3: recording fakes expose read-only lists with private setters and typed nested data classes so assertion failures name the field"
  - "Pattern 4: per-row handler isolation tests assert the OTHER handler stayed at zero, catching swapped lambdas"

requirements-completed: [PGSL-04, PGSL-06, PGSA-04, PGSA-05]

# Metrics
duration: 47 min
completed: 2026-09-09
---

# Phase 12 Plan 03: Reshape the GamesService Seam and Add the Stock-UI Entry Points Summary

**A Boolean-returning, Activity-free Play Games seam with real `*Immediate` + `Task.await()` submission bodies, `startActivityForResult` stock-UI launches, a recording `FakeGamesService`, and two signed-in-only Settings rows.**

## Performance

- **Duration:** 47 min
- **Tasks:** 3 of 3
- **Files modified:** 14 (plus one gitignored build-environment file, see Issues)
- **Worktree branch:** `worktree-agent-ae6ca310ac22f956b`

## Accomplishments

- **Submission failure became an observable event.** `submitScore` / `unlockAchievement` are now
  `suspend … : Boolean`, joined by `setAchievementSteps`. Plan 12-07's `ProgressSubmitter` can
  now define D-10's dirty flag in terms of something that actually happens; against the previous
  `void`-returning shape there was no failure to observe.
- **`PlayGamesService` implements all six methods for real** — session-gated, Activity-bounded,
  failure-as-`false`, cancellation rethrown before classification.
- **Both stock Play Games UIs are reachable** from two signed-in-only Settings rows, with the
  `Intent` obtained *and* started inside `platform/games` so nothing framework-shaped crosses
  into `ui/`.
- **D-16 (PGSA-04) discharged at zero cost**, as the plan predicted: Play Games Services v2 has
  no popup API, so the correct implementation is no code, and a grep gate now keeps it that way.
- **`FakeGamesService` became the phase's recording harness**, so 12-07's tests can be written
  against a real shape rather than a guess.

## Task Commits

1. **Task 1: Reshape the seam and give PlayGamesService real bodies** — `06e5ca4` (feat)
2. **Task 2: Update the test doubles and prove the platform bodies** — `efffa0a` (test)
3. **Task 3: Two signed-in-only Settings rows into the stock Play Games UIs** — `4f2c7a1` (feat)

## For Downstream Plans (12-04 and 12-07 read this)

### Final seam signatures, verbatim

```kotlin
suspend fun submitScore(leaderboardId: String, score: Long): Boolean
suspend fun unlockAchievement(achievementId: String): Boolean
suspend fun setAchievementSteps(achievementId: String, steps: Int): Boolean
suspend fun showLeaderboards()
suspend fun showAchievements()
```

Unchanged: `val session: StateFlow<GamesSignInState>`, `fun bootstrapIfOptedIn()`,
`suspend fun signIn(): Boolean`. `incrementAchievement` and `showLeaderboard(leaderboardId)`
no longer exist anywhere in `app/src`.

**`Boolean` means "the SDK accepted it"** — the round trip completed without throwing. It is
**not** `setStepsImmediate`'s `Task<Boolean>` payload, which means "the achievement is now
unlocked" and is discarded. `false` is normal and expected (offline, backgrounded, signed out,
`Unknown` session, misconfigured project) and is never shown to the player.

**`launchStockUi` WAS extracted.** The plan left this to executor discretion; both stock-UI
methods delegate to
`private suspend fun launchStockUi(requestCode: Int, intentFor: suspend (Activity) -> Intent)`.

### `FakeGamesService` recording API (exact shape)

```kotlin
var submissionResult: Boolean = true                       // what every submission returns
var submittedScores: List<ScoreSubmission>   private set
var unlockedAchievements: List<String>       private set
var achievementSteps: List<StepUpdate>       private set
var showLeaderboardsCallCount: Int           private set
var showAchievementsCallCount: Int           private set

data class ScoreSubmission(val leaderboardId: String, val score: Long)
data class StepUpdate(val achievementId: String, val steps: Int)
```

Nested inside `FakeGamesService`, so reference them as `FakeGamesService.ScoreSubmission` /
`FakeGamesService.StepUpdate`. Existing `signInBehavior`, `signInCallCount`,
`bootstrapCallCount` and `emitSession(...)` are untouched.

### `GamesConstants` request-code constants

`GamesConstants.LEADERBOARDS_REQUEST_CODE = 9004` and
`GamesConstants.ACHIEVEMENTS_REQUEST_CODE = 9003` (Google's own sample values). Referenced by
name only — no `9003`/`9004` literal appears in `PlayGamesService`.

### SDK behaviour vs RESEARCH's `javap` findings

**No discrepancies.** Every signature RESEARCH recorded compiled and mocked exactly as
documented: `submitScoreImmediate(String, long) -> Task<ScoreSubmissionData>`,
`unlockImmediate(String) -> Task<Void>`, `setStepsImmediate(String, int) -> Task<Boolean>`,
`getAllLeaderboardsIntent() -> Task<Intent>`, `getAchievementsIntent() -> Task<Intent>`, and
both `PlayGames.get*Client(Activity)` statics. There is indeed no popup client. `Tasks.forResult`
took `await()`'s fast path with no Looper, keeping `PlayGamesServiceTest` a plain JVM test.

**Two notes for whoever writes the next mock setup:** `unlockImmediate`'s `Task<Void>` needs
`Tasks.forResult(null)`, and `submitScoreImmediate`'s payload type is best left to inference
(`Tasks.forResult(mockk())`) rather than imported.

### Not in this plan

Nothing calls the submission methods yet. `GamesIds`, the ID plumbing, `ProgressSubmitter` and
the `ResultsViewModel` hook remain plans 12-04 / 12-07. **No leaderboard or achievement ID
literal appears in any file this plan touched** — verified by grep over
`app/src/main/java/com/app/pegsolitaire/ui/` (0 hits); test placeholders are named constants
(`placeholder-leaderboard-id`, `placeholder-achievement-id`).

## Decisions Made

- **`@Suppress("TooManyFunctions")` on `PlayGamesService`** rather than raising detekt's global
  threshold. The count is the interface's own method set plus four private helpers; splitting
  the class would mean a second holder of `_session`, which is the exact bug the file's
  `@Singleton`-on-the-class KDoc already warns about. Follows the `GameViewModel` precedent
  (`@Suppress` + a "Size suppression" KDoc section).
- **KDoc must not name the popup API even to say it is unavailable.** The D-16 gate is a literal
  grep, so the first draft's "v2 exposes no `getGamesClient`" sentences tripped it. Reworded to
  describe the absence without the token — the same trick this file already uses for the
  no-logging rule ("this paragraph avoids spelling any logging API out").
- **`SettingsUiState` gained no field.** Both rows render straight off the already-collected
  `gamesSignInState`, so there is no second copy of "is the player signed in" to drift.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocker] Missing `local.properties` in the worktree**

- **Found during:** Task 1 (first `./gradlew :app:compileDebugKotlin`)
- **Issue:** `SDK location not found` — `local.properties` is gitignored, so a fresh worktree
  has none and no Gradle task can run.
- **Fix:** Wrote `sdk.dir=/Users/ermanergoz/Library/Android/sdk` into the worktree's
  `local.properties`, matching the main checkout.
- **Files modified:** `local.properties` (gitignored — not committed, and correctly so)
- **Verification:** `./gradlew :app:compileDebugKotlin --offline` then succeeded.
- **Committed in:** n/a (gitignored build-environment file)

**2. [Rule 3 - Blocker] detekt `ReturnCount` on `submitting`**

- **Found during:** Task 1 (`:app:detekt`)
- **Issue:** The plan's literal three-step body has 3 `return` statements; the project limit is 2.
- **Fix:** Restructured to resolve the Activity conditionally
  (`if (_session.value is SignedIn) awaitForegroundActivity() else null`) and return a single
  `&&` expression. Semantics preserved exactly — the session check still short-circuits *before*
  the Activity is awaited, which is the ordering the plan cares about.
- **Files modified:** `PlayGamesService.kt`
- **Verification:** `:app:detekt` green; the `SignedOut`/`Unknown` short-circuit tests assert
  `getLeaderboardsClient` is never reached.
- **Committed in:** `06e5ca4`

**3. [Rule 3 - Blocker] detekt `TooManyFunctions` on `PlayGamesService`**

- **Found during:** Task 1 (`:app:detekt`)
- **Issue:** 13 functions against a limit of 11, after adding the six bodies and two helpers.
- **Fix:** `@Suppress("TooManyFunctions")` plus a "Size suppression" KDoc section explaining why
  splitting would be worse, following the `GameViewModel` precedent.
- **Files modified:** `PlayGamesService.kt`
- **Verification:** `:app:detekt` green.
- **Committed in:** `06e5ca4`

**4. [Rule 3 - Blocker] D-16 grep gate tripped by the plan's own required KDoc**

- **Found during:** Task 1 (acceptance criteria)
- **Issue:** The plan required KDoc paragraphs stating that v2 exposes no `getGamesClient`, and
  simultaneously required `grep -rEn 'setGravityForPopups|setViewForPopups|getGamesClient' app/src`
  to return 0. Prose satisfying the first broke the second.
- **Fix:** Reworded both paragraphs to describe the absence without naming the API, and said so
  explicitly ("A grep gate enforces the absence, so this paragraph names no such API").
- **Files modified:** `GamesService.kt`, `PlayGamesService.kt`
- **Verification:** grep returns 0; the D-16 rationale is still fully documented.
- **Committed in:** `06e5ca4`

**5. [Rule 3 - Blocker] `SettingsPrivacyTest.kt` not in the plan's `files_modified`**

- **Found during:** Task 3
- **Issue:** `SettingsScreen` gained two required params, and `SettingsPrivacyTest` also calls it
  directly — the test source would not compile.
- **Fix:** Added `onShowLeaderboards = {}` / `onShowAchievements = {}` to its one call site.
  Signature maintenance only; no test case changed.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsPrivacyTest.kt`
- **Verification:** `--tests "*SettingsPrivacyTest*"` green.
- **Committed in:** `4f2c7a1`

### Plan Inconsistency (noted, not a code change)

Task 1's acceptance criteria require `grep -rn "incrementAchievement" app/src` to print `0`,
while the same task explicitly expects `:app:compileDebugUnitTestKotlin` to *fail* until Task 2
updates the test doubles that still declare it. The two cannot both hold at the Task 1 boundary.
Resolved by keeping Task 1 scoped to `src/main` as written; both greps print `0` from Task 2
onward, where Task 2's criteria re-assert them. Final state satisfies every criterion.

---

**Total deviations:** 5 auto-fixed (5x Rule 3 - blockers). Zero Rule 1 or Rule 2 findings; the
plan's threat-model mitigations (T-12-30 no logging, T-12-31 no framework types on the seam,
T-12-32 bounded short circuits, T-12-33 cancellation rethrow, T-12-35 unmodified SDK Intent,
T-12-36 no IDs in `ui/`) were all specified in the plan and implemented as written.
**Impact on plan:** None on scope or design. Four of the five were build-gate mechanics; the
fifth was a one-line signature fix in a file the plan did not list.

## Verification Results

| # | Check | Result |
|---|-------|--------|
| 1 | `./gradlew :app:compileDebugKotlin` | PASS |
| 2 | `:app:testDebugUnitTest --tests "*NoOpGamesService*" "*PlayGamesServiceTest*" "*SettingsPlayGamesTest*" "*SettingsViewModelTest*" "*PlatformBoundary*"` | PASS |
| 3 | `./gradlew detekt ktlintCheck` | PASS |
| 4 | `grep -rn "incrementAchievement" app/src` | 0 (D-12) |
| 5 | `grep -rEn "setGravityForPopups\|setViewForPopups\|getGamesClient" app/src` | 0 (D-16) |
| 6 | `grep -rEn '"[a-z_]*(leaderboard\|achievement)[a-z_]*"' app/src/main/java/.../ui/` | 0 (D-13/PLAT-06) |

All 3 tasks' `<acceptance_criteria>` were re-run individually and pass.

**Not verifiable here, by design:** no real score reaches a real leaderboard and no stock UI
actually opens — there is no Play Console project (Phase 11 D-04). Those are `11-HUMAN-UAT.md`
device items owned by plan 12-08, together with the D-16 popup check and the R8
`missing_rules.txt` inspection (T-12-34).

## Issues Encountered

**`:domain:depthSweep` fails with `java.io.EOFException`, unrelated to this plan.** The full
`./gradlew check --offline` gate fails on this one `:domain` task. It is out of scope per the
scope boundary: `depthSweep` runs `:domain`'s own test classes on `:domain`'s runtime classpath
and cannot see `:app`, and this plan changed **zero** `:domain` files
(`git diff --name-only e881a4b HEAD` lists only `app/**`). Reproduced in isolation with
`./gradlew :domain:depthSweep --offline` — the test-worker JVM dies in ~550ms before any
assertion runs, which points at environment/worker contention (several parallel worktree agents
sharing a daemon set) rather than a code regression. Logged to
`.planning/phases/12-leaderboards-achievements/deferred-items.md`. Everything this plan owns is
green.

## Known Stubs

None. `NoOpGamesService` remains a deliberate stub by design (it is the Slice-1 binding, swapped
out in `PlatformModule` since Phase 11), not an unfinished implementation.

## Threat Flags

None. No new network endpoint, auth path, file access pattern or schema change was introduced
beyond what the plan's `<threat_model>` already registers.

## User Setup Required

None — no external service configuration is introduced by this plan. Play Console setup remains
a Phase-11 D-04 open item covered by `11-HUMAN-UAT.md`.

## Next Phase Readiness

Ready for wave 2. Plans 12-04 (`GamesIds` and ID plumbing) and 12-07 (`ProgressSubmitter`) can
now be written against the final seam signatures and the `FakeGamesService` recording API
documented above, both reproduced verbatim in this summary so neither plan needs to re-read the
source to discover them.

No blockers introduced. One unrelated `:domain` build-gate failure is deferred (see Issues).

## Self-Check: PASSED

- All 9 key files verified present on disk.
- All 3 task commits verified in `git log`: `06e5ca4`, `efffa0a`, `4f2c7a1`.
- All 3 `key_links` patterns verified present (`onShowLeaderboards` in `SettingsScreen.kt`;
  `gamesService.show(Leaderboards|Achievements)()` in `SettingsViewModel.kt`;
  `startActivityForResult` in `PlayGamesService.kt`).
- All plan-level `<verification>` steps re-run: 6 of 6 pass.
- STATE.md and ROADMAP.md deliberately untouched (worktree/parallel mode — orchestrator owns them).

---
*Phase: 12-leaderboards-achievements*
*Completed: 2026-09-09*
