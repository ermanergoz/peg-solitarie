---
phase: quick-260828-tnw
plan: 01
type: execute
wave: 1
depends_on: []
files_modified:
  - app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt
  - app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt
autonomous: true
requirements: [NAV-02]
must_haves:
  truths:
    - "After finishing a Classic board launched from the catalog, system Back on Results lands on ClassicCatalog, not the completed board"
    - "After finishing a Challenge level, system Back on Results lands on Home, not the completed board"
    - "After finishing a Classic board resumed from Home Continue (no catalog ever visited), system Back on Results lands on Home, not the completed board"
    - "The completed Gameplay entry is absent from the back stack once Results is showing"
    - "The Results action buttons (Replay, Catalog, Home, Next level, Retry) keep their existing destinations and stack behaviour"
  artifacts:
    - path: "app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt"
      provides: "private NavHostController.toResults helper wired into the composable<Gameplay> win hop"
      contains: "private fun NavHostController.toResults"
    - path: "app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt"
      provides: "Robolectric back-stack assertions for the three finish paths"
  key_links:
    - from: "AppNavHost composable<Gameplay> onFinished lambda"
      to: "NavHostController.toResults"
      via: "direct call replacing the bare navigate(results)"
      pattern: "navController\\.toResults\\(results\\)"
---

<objective>
Fix the Results back-navigation defect: pressing system Back on the Results screen
currently returns the player to the completed (won) Game board instead of to the place
they launched the level from.

The win hop in `AppNavHost.kt` (`composable<Gameplay>`, line ~126) is the lone navigation
in the file that does not manage the back stack — `{ results -> navController.navigate(results) }`
pushes `Results` on top of a still-live `Gameplay` entry. Every sibling hop
(`toBoardFromCatalog`, `toChallengeBoard`) already uses `popUpTo`. This plan adds the third
helper in the same style.

Purpose: the player never sees the board they just finished again when backing out.
Output: one private `NavHostController.toResults` helper + three Robolectric back-stack tests.

**Scope guard (locked user decisions):**
- Use `popUpTo` on the win navigation. Do NOT add a `BackHandler` to Results.
- Branch by mode: Classic (`Results.challengeLevel == null`) → `popUpTo<ClassicCatalog>()`;
  Challenge → `popUpTo<Home>()`.
- Do NOT touch undo behaviour. No status guard in `GameViewModel.onUndo()`, no disabling
  `UndoControl` on a won board. Navigation only — do not open those files.

**Discretionary detail the planner is adding on top of the locked decisions (flagged for
visibility, strike it if unwanted):** the Classic branch also checks that `ClassicCatalog`
is actually on the back stack. The Home Continue affordance (PERS-03) navigates
`Home → Gameplay(levelId, resume = true)` *directly*, so a resumed Classic game has no
catalog entry beneath it. `popUpTo` against a route absent from the back stack is a
documented no-op in Navigation (the pop is ignored, the navigate still happens) — so
without this check the reported bug survives untouched on the Continue path. Decision 2's
stated intent is "land in the catalog the player launched from"; when the player did not
launch from the catalog there is no such catalog, and `Home` is the only destination
actually beneath them.
</objective>

<execution_context>
@$HOME/.claude/get-shit-done/workflows/execute-plan.md
@$HOME/.claude/get-shit-done/templates/summary.md
</execution_context>

<context>
@.planning/STATE.md
@CLAUDE.md

@app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt
@app/src/main/java/com/app/pegsolitaire/navigation/Routes.kt
@app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt

<interfaces>
<!-- Verified against navigation-runtime/common 2.9.8 sources in the Gradle cache. -->
<!-- Both APIs below exist on the pinned version — no exploration needed. -->

androidx.navigation.NavController:
  public val currentBackStack: StateFlow<List<NavBackStackEntry>>
  public open val previousBackStackEntry: NavBackStackEntry?
  public open val currentBackStackEntry: NavBackStackEntry?

androidx.navigation.NavDestination.Companion:
  @JvmStatic public inline fun <reified T : Any> NavDestination.hasRoute(): Boolean
  // import androidx.navigation.NavDestination.Companion.hasRoute

Route discriminator (Routes.kt):
  data class Results(..., val challengeLevel: Int? = null)   // null => Classic
</interfaces>

Existing back-stack entry paths that the fix must handle correctly:

| Entry path | Stack when the game finishes | Correct landing on Back |
|---|---|---|
| Home → Catalog → Gameplay(levelId) | Home, ClassicCatalog, Gameplay | ClassicCatalog |
| Home → Continue Classic (PERS-03) | Home, Gameplay | Home |
| Home → Challenge tile (`toChallengeBoard`) | Home, Gameplay | Home |
| Results → Replay (`toBoardFromCatalog`) | Home, ClassicCatalog, Gameplay | ClassicCatalog |
| Results → Next/Retry (`toChallengeBoard`) | Home, Gameplay | Home |
</context>

<tasks>

<task type="auto" tdd="true">
  <name>Task 1: RED — assert the post-finish back stack in NavFlowTest</name>
  <files>app/src/test/java/com/app/pegsolitaire/navigation/NavFlowTest.kt</files>
  <behavior>
    Three new `@Test` methods, each hosting `AppNavHost` over the captured
    `navController` with `StubDestination` bodies (follow the existing file's pattern
    exactly — same tags, same `waitForIdle()` cadence, same `currentRoute()` helper):

    - `classicWinPopsTheCompletedBoardAndBacksOutToCatalog`
      Drive Home → ClassicCatalog → Gameplay, then fire `onFinished(sampleResults())`
      (`sampleResults()` already exists and has `challengeLevel` defaulted to null = Classic).
      Assert: `currentRoute()` contains `RESULTS_ROUTE`; `previousBackStackEntry`'s route
      contains `CATALOG_ROUTE`; and no entry in `navController.currentBackStack.value`
      has a route containing `GAMEPLAY_ROUTE`.

    - `challengeWinPopsTheCompletedBoardAndBacksOutToHome`
      Drive Home (challenge tile → `onPlayChallenge(PERSISTED_CHALLENGE_LEVEL)`) → Gameplay,
      then fire `onFinished(stuckChallengeResults())` (already exists; `challengeLevel = 7`).
      Assert: `previousBackStackEntry`'s route contains `HOME_ROUTE`; no `GAMEPLAY_ROUTE`
      entry remains in `currentBackStack.value`.

    - `resumedClassicWinBacksOutToHomeWhenNoCatalogWasVisited`
      Drive Home (`onResumeClassic(SELECTED_LEVEL_ID)`) → Gameplay — deliberately never
      visiting the catalog — then fire `onFinished(sampleResults())`.
      Assert: `previousBackStackEntry`'s route contains `HOME_ROUTE`; no `GAMEPLAY_ROUTE`
      entry remains in `currentBackStack.value`.

    All three MUST fail on the current unmodified `AppNavHost` (the completed `Gameplay`
    entry is still on the stack and is `previousBackStackEntry`). Run them and confirm
    the failure before moving to Task 2 — a test that passes here is not testing the bug.
  </behavior>
  <action>
Add the three tests to the existing `NavFlowTest` class. Reuse the file's existing
private helpers and top-level constants (`StubDestination`, `currentRoute()`,
`sampleResults()`, `stuckChallengeResults()`, `HOME_TAG`/`CATALOG_TAG`/`GAMEPLAY_TAG`/`RESULTS_TAG`,
`HOME_ROUTE`/`CATALOG_ROUTE`/`GAMEPLAY_ROUTE`/`RESULTS_ROUTE`, `SELECTED_LEVEL_ID`,
`PERSISTED_CHALLENGE_LEVEL`) — do not introduce parallel copies and do not introduce
raw string or numeric literals; CLAUDE.md's no-magic-values rule applies, and every value
these tests need already has a named constant in this file.

Factor the repeated stack assertion into one private helper on the class, e.g.
`private fun assertCompletedBoardIsOffTheBackStack()` reading
`navController.currentBackStack.value` and asserting via Truth that no
`entry.destination.route.orEmpty()` contains `GAMEPLAY_ROUTE`. Give it a one-line KDoc
saying it proves the finished board cannot be reached by Back.

Import `androidx.navigation.NavDestination.Companion.hasRoute` ONLY if you actually use
it here; the route-substring style already used in this file is sufficient for the test
assertions, so plain `.destination.route` comparisons are preferred for consistency.

Extend the `NavFlowTest` class KDoc with one sentence noting it now also pins the
post-finish back stack (the finished board is popped, so Back leaves Results for the
launching destination) — the file's doc block is the reader's map and must stay true.

Do NOT modify `AppNavHost.kt` in this task. Do NOT modify the five existing tests.
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --offline --tests "com.app.pegsolitaire.navigation.NavFlowTest" 2>&1 | tail -40; echo "EXPECT: the three new tests FAIL (RED)"</automated>
  </verify>
  <done>The three new tests exist, compile, and FAIL against the unmodified `AppNavHost` — each failing because the completed `Gameplay` entry is still on the back stack. The five pre-existing `NavFlowTest` tests still pass.</done>
</task>

<task type="auto" tdd="true">
  <name>Task 2: GREEN — add the toResults helper and wire the win hop</name>
  <files>app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt</files>
  <behavior>
    With the helper in place, all three Task-1 tests pass and all five pre-existing
    `NavFlowTest` tests continue to pass — in particular
    `drivesHomeToCatalogToGameplayToResultsReachingEveryRoute` (Results is still reached
    and still carries the played `levelId`) and
    `challengeRetryReloadsSameBoardWithoutGrowingBackStack` (Retry still lands on
    Gameplay with Home directly beneath it).
  </behavior>
  <action>
Add a third private extension helper at the bottom of `AppNavHost.kt`, alongside and in
the same style as `toBoardFromCatalog` and `toChallengeBoard`:

    private fun NavHostController.toResults(results: Results)

Name it `toResults`, not `toResultsFromWin` — `GameViewModel.finished` also emits for a
stuck (unsolvable) board, and the Challenge Retry flow depends on that path, so the helper
must not claim to be win-only.

Body: compute a local `launchedFromCatalog` = `results.challengeLevel == null` AND
`currentBackStack.value.any { it.destination.hasRoute<ClassicCatalog>() }`. When true,
`navigate(results) { popUpTo<ClassicCatalog>() }`; otherwise
`navigate(results) { popUpTo<Home>() }`. Both `popUpTo` calls are non-inclusive (matching
the two existing helpers), so the landing destination survives and only the finished
`Gameplay` entry above it is popped. Do not add `inclusive` or `launchSingleTop`.

Add the required import `androidx.navigation.NavDestination.Companion.hasRoute`
(verified present in navigation 2.9.8; `currentBackStack` is a `StateFlow` on
`NavController`, read `.value` directly — this runs inside a nav callback, not composition).

Write a KDoc block on the helper in the same voice as its two neighbours (which cite
their driving requirement/threat ids). It must state: (a) that it pops the finished board
so system Back from Results returns to the launching destination rather than re-showing
the completed game; (b) that Classic mirrors `toBoardFromCatalog` by popping up to
`ClassicCatalog` and Challenge mirrors `toChallengeBoard` by popping up to `Home`;
(c) that a Classic game resumed via the Home Continue affordance (PERS-03) never visited
the catalog, so `popUpTo<ClassicCatalog>` would silently no-op there and those finishes
fall back to `Home`, the only destination actually beneath them.

Then rewire the win hop inside `composable<Gameplay>`: replace
`{ results -> navController.navigate(results) }` with
`{ results -> navController.toResults(results) }`. Change nothing else in that block —
`onNavigateBack` stays `navController.popBackStack()`.

Finally, update the `AppNavHost` function KDoc: the sentence listing back-arrow pops
currently reads "... Results→Catalog ...". Amend that paragraph so it also states that
finishing a board navigates to Results *through* `toResults`, popping the completed
Gameplay entry, so Back from Results cannot return to the finished board.

Out of scope, do not touch: the `Results` destination's six action lambdas (Replay,
Catalog, Home, Next level, Retry, back) — they already collapse the stack correctly and
must keep working unchanged; `toBoardFromCatalog`; `toChallengeBoard`; `Routes.kt`;
`GameRoute.kt`; anything under `ui/game/`.
  </action>
  <verify>
    <automated>./gradlew :app:testDebugUnitTest --offline --tests "com.app.pegsolitaire.navigation.NavFlowTest" && ./gradlew :app:check --offline</automated>
  </verify>
  <done>
All eight `NavFlowTest` tests pass (three new + five pre-existing) and
`./gradlew :app:check --offline` passes, detekt and ktlint included.

Structural acceptance checks — each verified against the code this task asks for:

    F=app/src/main/java/com/app/pegsolitaire/navigation/AppNavHost.kt
    # the helper exists (exactly one declaration)
    [ "$(grep -c 'private fun NavHostController.toResults' $F)" = "1" ]
    # the win hop goes through it
    [ "$(grep -c 'navController.toResults(results)' $F)" = "1" ]
    # the bare, stack-unmanaged win hop is gone. NOTE: the helper body legitimately
    # contains the substring "navigate(results)"; this grep is anchored on the
    # navController receiver, which the helper does NOT use (implicit receiver), so
    # the expected count is 0 and does not contradict the helper's own code.
    [ "$(grep -c 'navController\.navigate(results)' $F)" = "0" ]
    # both pop targets now appear twice each: once in the pre-existing helper,
    # once in the new one (each was 1 before this change).
    [ "$(grep -c 'popUpTo<ClassicCatalog>()' $F)" = "2" ]
    [ "$(grep -c 'popUpTo<Home>()' $F)" = "2" ]
    # locked decision 3: no BackHandler introduced anywhere
    [ "$(grep -rc 'BackHandler' app/src/main/java | grep -v ':0' | wc -l | tr -d ' ')" = "0" ]
  </done>
</task>

</tasks>

<threat_model>
## Trust Boundaries

| Boundary | Description |
|----------|-------------|
| (none new) | This change moves no data across a trust boundary — it only removes an entry from the in-process navigation back stack. `Results` args are unchanged and still passed by value from in-game computation. |

## STRIDE Threat Register

| Threat ID | Category | Component | Disposition | Mitigation Plan |
|-----------|----------|-----------|-------------|-----------------|
| T-TNW-01 | Elevation of Privilege | `toResults` popUpTo targets | accept | `popUpTo` is non-inclusive against two routes that are compile-time `@Serializable` objects; it can only shrink the stack toward an already-visited destination, never fabricate a destination or reach a board the plain route could not. |
| T-TNW-02 | Denial of Service | `popUpTo` against an absent route | mitigate | The absent-`ClassicCatalog` case (Home Continue path) is explicitly branched to `popUpTo<Home>()`; `Home` is the `startDestination` and is therefore always on the stack, so the pop can never no-op into an unbounded stack. Covered by `resumedClassicWinBacksOutToHomeWhenNoCatalogWasVisited`. |
| T-TNW-SC | Tampering | package installs | n/a | No new dependency. `hasRoute` and `currentBackStack` are already-resolved APIs of the pinned `navigation-compose 2.9.8`; `gradle/libs.versions.toml` is untouched. |
</threat_model>

<verification>
Test-feasibility note (required by the plan constraints): back-stack assertions ARE
practical in this project's existing setup and no invented proxy test is needed.
`NavFlowTest` already hosts the real `AppNavHost` over a captured `NavHostController`
under Robolectric, and the existing `challengeRetryReloadsSameBoardWithoutGrowingBackStack`
test already asserts stack shape via `navController.previousBackStackEntry`. The three
new tests use the same real controller plus `navController.currentBackStack.value`, so
they assert the actual production back stack — not a stand-in for it.

1. `./gradlew :app:testDebugUnitTest --offline --tests "com.app.pegsolitaire.navigation.NavFlowTest"` — 8 tests pass.
2. `./gradlew :app:check --offline` — passes, including detekt and ktlint.
3. Manual smoke (optional, not gating): Home → Classic → pick a board → win → Back lands on the catalog. Home → Challenge → win → Back lands on Home. Home → Continue a Classic game → win → Back lands on Home.
</verification>

<success_criteria>
- Backing out of Results never shows the completed board, on all three finish paths.
- Classic boards launched from the catalog back out to the catalog; Challenge levels and Continue-resumed Classic boards back out to Home.
- The win hop is the third `popUpTo` helper in `AppNavHost.kt`, stylistically consistent with `toBoardFromCatalog` and `toChallengeBoard`.
- The Results action buttons behave exactly as before (proven by the five untouched `NavFlowTest` tests).
- No `BackHandler` added; `GameViewModel.onUndo()` and `UndoControl` untouched.
- `./gradlew :app:check --offline` passes.
</success_criteria>

<output>
Create `.planning/quick/260828-tnw-fix-results-back-navigation-with-popupto/260828-tnw-SUMMARY.md` when done.
</output>
</content>
</invoke>
