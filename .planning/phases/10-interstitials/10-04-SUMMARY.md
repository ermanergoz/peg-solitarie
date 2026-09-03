---
phase: 10-interstitials
plan: 04
subsystem: ads
tags: [interstitial, cadence, results, mvvm, stateflow, robolectric, datastore, admob]

# Dependency graph
requires:
  - phase: 10-interstitials
    plan: 01
    provides: "InterstitialCadenceState.mayShow / recordWin — the single cadence question and the projection this plan takes once per Results entry"
  - phase: 10-interstitials
    plan: 02
    provides: "InterstitialCadenceStore — the win bank, the interstitial stamp, and the recordRewardedShown entry point this plan finally wires"
  - phase: 10-interstitials
    plan: 03
    provides: "interstitialReady / preloadInterstitial() / suspend showInterstitial(): Boolean and AdConstants.INTERSTITIAL_QUIESCENCE_MS"
  - phase: 02-gameplay-scoring
    provides: "ResultsViewModel's tierFor outcome classification and the SavedStateHandle one-shot latch idiom cloned a third time here"
provides:
  - "ResultsNavTarget — the closed D-06 set of ad-eligible forward-moving win actions"
  - "ResultsUiState.adScrimVisible / pendingNavigation — the INT-04 quiescence signal and the Pitfall-6-safe navigation state"
  - "ResultsViewModel.onNextLevelTapped / onCatalogTapped / onNavigationHandled — the trigger surface plan 10-05 wires the route to"
  - "ResultsViewModel.isInterstitialEligible — the D-14 predicate, derived from the headline tier so gate and copy cannot disagree"
  - "The D-15 rewarded stamp on AdMobAdGateway.onAdShowedFullScreenContent — the cross-phase obligation 10-01 and 10-02 both flagged as unwired"
  - "FakeAdGateway — a shared gated AdGateway double for the Results test surface"
affects: [10-05-route-and-scrim-rendering, 10-06-device-uat]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Decide from a locally projected value, not from a re-read of a fire-and-forget store: the answer must not depend on disk latency"
    - "Scrim teardown and navigation in the SAME finally, so every exit path honours the tap exactly once and can never strand the scrim"
    - "Eligibility derived from the already-computed display tier, so the gate and the copy the player reads are structurally the same decision"
    - "Test sequencing barrier over a field no gate reads (the foreground stamp), so a 'recorded nothing' assertion cannot pass by running early"

key-files:
  created:
    - app/src/test/java/com/app/pegsolitaire/ui/results/FakeAdGateway.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/platform/ads/AdMobAdGateway.kt
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt

key-decisions:
  - "Eligibility is `tier != STUCK`, never a peg comparison and never the strict-win star test — the one shape that admits a two-star Classic solve AND a two-peg perfect Diamond-41 finish while excluding a stuck Challenge board for free"
  - "The tap decides from cadenceForThisResult (the win projected locally at construction), not from a re-read of cadenceStore.state.value, so a fast tap and a slow tap get the same answer and the preload and the show can never disagree"
  - "Both the scrim teardown and pendingNavigation live in presentThenNavigate's finally — one block is what makes D-09's exactly-once true on dismissal, show failure, exception and cancellation alike"
  - "pendingNavigation is STATE on the UiState rather than a Channel/SharedFlow: the host is STOPPED for the whole ad, and a re-delivered event is a Challenge ladder that advances two levels (RESEARCH Pitfall 6)"
  - "claimWinOnce is a single `when` expression rather than guard-clause returns, because detekt's ReturnCount is a build gate here"
  - "continueClaimed is plain instance state, not a SavedStateHandle latch: it guards one screen's lifetime, and a genuine re-entry into Results must be tappable again"
  - "The D-15 rewarded stamp sits on onAdShowedFullScreenContent — the only callback that means an ad actually appeared — with exactly one writer per ad kind, argued out in KDoc"

patterns-established:
  - "Navigation timing owned by the ViewModel and published as state, with the composable reduced to reporting the tap"
  - "A closed sealed nav-target type as the structural boundary of what can ever be ad-gated"
  - "Fixtures seeded through a store's real public transitions rather than by writing preference keys, so the fixture cannot drift from the policy"

requirements-completed: [INT-01, INT-02, INT-03, INT-04]

# Metrics
duration: 41min
completed: 2026-09-03
---

# Phase 10 Plan 04: Results Trigger, Cadence Decision and Ad-then-Navigate Summary

**Every locked decision in the phase now meets on one screen: `ResultsViewModel` owns the D-14 eligibility test, the D-12 preload, the single D-01..D-04 cadence question, the D-08 quiescence pause, D-09's ad-first-then-navigate sequencing and D-11's record-only-on-a-real-presentation rule — and the fast path costs the player nothing.**

## Performance

- **Duration:** ~41 min
- **Started:** 2026-09-03T00:12:00Z
- **Completed:** 2026-09-03T00:53:00Z
- **Tasks:** 3
- **Files modified:** 5 (1 created, 4 modified)

## Accomplishments

- **INT-01 is structural, not conditional.** Nothing in construction can show an ad — the only two entry points that reach `showInterstitial()` are `onNextLevelTapped` and `onCatalogTapped`, the exact pair D-06 admits. `constructingTheViewModelNeverShowsAnInterstitial` asserts it across all three fixtures at once (eligible + ready + passing cadence, non-eligible, and suppressed), because the claim is about the constructor rather than about any one of them.
- **The Diamond-41 trap is closed by shape, not by care.** `isInterstitialEligible(tier) = tier != STUCK` contains no peg comparison and is not the strict-win star test. A perfect Diamond-41 finish (three stars, **two** pegs remaining) is ad-eligible and preloads, pinned by `aPerfectDiamond41FinishWithTwoPegsRemainingIsAdEligible`; a two-star Classic solve is eligible; a stuck Challenge board inherits `tierFor`'s exclusion for free. `grep -rn 'pegsRemaining == 1\|pegsRemaining != 1'` over the ViewModel shows only the two pre-existing uses.
- **D-15 is finally wired — the obligation 10-01 created and 10-02 restated as still-open.** `AdMobAdGateway` now calls `cadenceStore.recordRewardedShown(...)` from `onAdShowedFullScreenContent` on the rewarded presentation, so the 90-second gate genuinely covers both ad kinds and the rewarded-hint → win → "Next" adjacency AdMob prohibits is unreachable. Exactly one writer per path, with the reasoning written into the KDoc so a future editor cannot "symmetrise" it into a double-write.
- **D-09's exactly-once is a single `finally`.** Scrim teardown and `pendingNavigation` are set in the same block, so a dismissal, a show failure, an exception and a cancelled `viewModelScope` all honour the tap once and none of them can strand a scrim over a live screen. `aShowThatNeverPresentedNavigatesAnywayAndRecordsNothing` proves the failure branch navigates *and* leaves `lastFullscreenAdAtMs` at `NEVER_SHOWN` with the win counter untouched.
- **The D-08 boundary is asserted in virtual time, on both sides.** `showInterstitial()` is uncalled after `INTERSTITIAL_QUIESCENCE_MS - 1` and called exactly once after the remaining millisecond.
- **The suite grew 21 → 38 tests in `ResultsViewModelTest` with no pre-existing assertion touched** (the diff's only removals are the eight constructor call sites and one comment). Whole app suite: **468 tests, 0 failures**; `:domain:test`, `detekt`, `ktlintCheck`, `adIdLiteralCheck`, `dpSpLiteralCheck`, `audioFocusLiteralCheck`, `PlatformBoundaryTest`, `PlatformBoundaryGateTest` and `DomainPurityTest` all green.

## Task Commits

1. **Task 1: eligibility, cadence decision and ad-then-navigate sequencing** — `2e61e95` (feat)
2. **Task 2: D-15 — a rewarded ad opens the shared fullscreen cooldown** — `7d8a58c` (feat)
3. **Task 3: FakeAdGateway and the seventeen behaviours** — `bdf1f82` (test)

## Files Created/Modified

- `ui/results/ResultsUiState.kt` — `sealed interface ResultsNavTarget` (`NextLevel(level)`, `Catalog`) KDoc'd as exactly D-06's set with the three excluded actions and *why* each is excluded; `adScrimVisible` and `pendingNavigation` added with their reasons (INT-04's not-a-timer rule; Pitfall 6's stopped-collector argument).
- `ui/results/ResultsViewModel.kt` — `cadenceStore` + `adGateway` constructor params; a shared `headlineTier`; `interstitialEligible`; `cadenceForThisResult` via `claimWinOnce()` behind the new `WIN_COUNTED_KEY` latch; `onNextLevelTapped` / `onCatalogTapped` / `onNavigationHandled`; private `continueWith` / `shouldShowInterstitial` / `presentThenNavigate`; companion `isInterstitialEligible` with the two-trap KDoc and the T-10-06 acceptance recorded.
- `platform/ads/AdMobAdGateway.kt` — `InterstitialCadenceStore` constructor dependency and the `onAdShowedFullScreenContent` override, with a four-paragraph KDoc covering D-15's argument, the choice of callback, the one-writer-per-path invariant and the deliberate `..platform.. → ..data..` layering.
- `test/ui/results/FakeAdGateway.kt` **(new)** — `CompletableDeferred` show gate, `preloadCount` / `showCount` with `private set`, `releaseShow()`, `setInterstitialReady()`, inert rewarded members, and a KDoc naming the four decisions it exists to exercise.
- `test/ui/results/ResultsViewModelTest.kt` — `TemporaryFolder` + `PreferenceDataStoreFactory` harness, `storeScope`, `freshCadenceStore()`, `eligibleCadenceStore()` (seeded through real transitions only), `settledState()` write barrier, `viewModelOver(...)`, a widened `viewModelFor(...)`, and the seventeen new cases.

## Decisions Made

- **The eligibility gate reads the display tier, not the raw args.** Two rules over the same facts can drift; one value cannot. It also means the gate inherits `tierFor`'s Challenge-stuck branch without restating it, so a screen that says "Out of moves" is ad-free *by construction*.
- **`claimWinOnce` returns the projection, and the tap decides from it.** Banking the win is a fire-and-forget DataStore write, so re-reading `cadenceStore.state.value` at tap time would make the decision race a disk write — the same result would show an ad for a slow tapper and not for a fast one. Projecting once also guarantees the D-12 preload and the eventual show agree about eligibility.
- **A fresh `System.currentTimeMillis()` is taken for the recording rather than reusing the decision timestamp.** The ad occupied real time; stamping the later instant makes the next 90-second gate strictly more conservative.
- **A non-eligible finish banks no win at all.** This follows the plan's behaviour spec (`aNonEligibleFinishBanksNoWinAndPreloadsNothing`) and narrows 10-02's broader "call `recordWin()` on every level completion" note: a stuck board is not a win, and counting it would let losses buy ads. Recorded here because it is the one place the two documents differ.
- **`continueClaimed` is instance state, not a `SavedStateHandle` latch.** It guards a screen that is already leaving; a genuine re-entry into Results must be tappable again, so persisting it across construction would be wrong.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Recreated the gitignored `local.properties` in the worktree**

- **Found during:** Task 1, first Gradle invocation
- **Issue:** `local.properties` is gitignored, so a fresh worktree has none and every `:app` task fails with "SDK location not found". The same blocker 10-01, 10-02 and 10-03 each hit.
- **Fix:** Wrote `sdk.dir=/Users/ermanergoz/Library/Android/sdk` into the worktree root.
- **Files modified:** `local.properties` (gitignored — deliberately not committed; committing it would leak a machine-specific absolute path).
- **Verification:** `:app:compileDebugKotlin` then ran normally.
- **Committed in:** Not committed (gitignored by design).

**2. [Rule 3 - Blocking] `detekt`'s `LargeClass` rule failed on the grown test class**

- **Found during:** Task 3, running the style gate
- **Issue:** `:app:detekt` **does** analyse test sources, and adding the seventeen cases pushed `ResultsViewModelTest` past the 600-line `LargeClass` threshold. `detekt` is a mandatory build gate (CLAUDE.md), so this blocked the task.
- **Fix:** Added `@Suppress("LargeClass")` with a justification comment, following the identical in-repo precedent at `GameViewModelTest.kt:77` — "kept together so one file exhaustively documents the VM's contract". Splitting the file was rejected: the plan explicitly assigns these cases to `ResultsViewModelTest.kt` and its acceptance criteria grep that file.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt`
- **Verification:** `:app:detekt :app:ktlintCheck` exit 0; the 38 tests still run and pass.
- **Committed in:** `bdf1f82`

**3. [Rule 3 - Blocking] Task 2's boundary-test verification was deferred to Task 3**

- **Found during:** Task 2
- **Issue:** Task 2's `<verify>` includes `:app:testDebugUnitTest --tests '*PlatformBoundaryTest' ...`, which requires the **test** source set to compile — and it could not, because the widened `ResultsViewModel` constructor was not reconciled with `ResultsViewModelTest`'s eight direct call sites until Task 3. Running it at Task 2 would have failed for a reason unrelated to Task 2's change.
- **Fix:** Task 2 was verified with `:app:compileDebugKotlin :app:detekt :app:ktlintCheck`; the boundary gates were run at Task 3 and are green. Task 2's criterion ("nothing new reaches the generated Hilt component") is therefore satisfied, just one commit later.
- **Files modified:** None — only the verification ordering changed.
- **Verification:** `PlatformBoundaryTest`, `PlatformBoundaryGateTest` and `DomainPurityTest` all pass at `bdf1f82`.
- **Committed in:** n/a (process deviation)

### Acceptance criteria satisfied by intent rather than by literal grep

Three grep-shaped criteria conflict with their own task's `<action>`, which mandates KDoc that names the very identifiers the grep forbids. In each case the criterion's *intent* — no such thing in **code** — is met and was verified more strictly than the grep would have.

| Criterion | Expected | Actual | Why |
|-----------|----------|--------|-----|
| `grep -c 'PEGS_REMAINING_SOLVED'` in `ResultsViewModel.kt` unchanged | 3 (pre-plan) | 4 | The one new occurrence is on a KDoc line the `<action>` requires: "it must never be written as a `pegsRemaining == 1` / `PEGS_REMAINING_SOLVED` comparison". Code occurrences are unchanged at 3 (lines 312, 373, 403). |
| `grep -c 'THREE_STARS'` unchanged | 2 (pre-plan) | 3 | Same cause — the `<action>` requires naming the strict-win test as the trap being avoided. Code occurrences unchanged at 1 (line 405, inside `tierFor`). |
| `grep -c 'recordInterstitialShown'` in `AdMobAdGateway.kt` is 0 | 0 | 1 | The one occurrence is on the KDoc line the `<action>` requires: "the INTERSTITIAL stamp is written by `ResultsViewModel` when `showInterstitial()` answers `true`". No code reference exists; `grep -c 'recordRewardedShown'` is exactly 1, as required. |

This is the same class of conflict 10-02 resolved for `ForegroundActivityProvider`, and it is resolved the same way: the plan's explicit `<action>` instruction wins, because naming the wrong choice is the whole point of the paragraph.

Additionally, the plan quoted the pre-plan `PEGS_REMAINING_SOLVED` count as **2** while the actual pre-edit count was **3** (the declaration plus two uses — `recordSolvedPlayOnce` and `tierFor`). The instruction "record the pre-edit count and assert it did not grow" was followed against the real value.

---

**Total deviations:** 3 auto-fixed (3 blocking) + 3 grep criteria satisfied by intent.
**Impact on plan:** None on behaviour. One worktree environment file, one style-gate exemption with a documented in-repo precedent, one verification-ordering shift, and three documentation-vs-grep conflicts resolved in favour of the plan's own `<action>` text.

## Issues Encountered

- **`advanceTimeBy(n)` does not run a task scheduled at exactly `currentTime + n`.** The D-08 boundary case therefore pairs every `advanceTimeBy` with a `runCurrent()`; without it, the "called after the full pause" half would have read as uncalled and the test would have pinned the wrong boundary.
- **Negative cadence assertions needed a real barrier.** "This recorded nothing" can pass simply by running before the write it rules out. `settledState()` issues a *later* foreground write and awaits that one — DataStore serialises `updateData`, so observing the barrier proves anything launched earlier has already been applied. The foreground stamp was chosen specifically because no cadence gate reads it, so the barrier disturbs no field under assertion (the 10-02 pattern).
- **An eligible cadence store saturates both counters**, so a second win on it is unobservable by construction. The `WIN_COUNTED_KEY` latch case therefore asserts across two stores: a fresh one to prove `lifetimeCompletedLevels` stays at 1, and an eligible one to prove `preloadCount` stays at 1.

## TDD Gate Compliance

Task 1 carries `tdd="true"`, but the plan assigns **all** test files to Task 3 and gives Task 1 a compile-and-lint `<verify>` only. A literal RED-before-Task-1 was not possible: the seventeen cases exercise a constructor, two public tap entry points and two `ResultsUiState` fields that Task 1 itself introduces, so the tests could not link against a real public surface before it existed, and writing them would have required editing files outside Task 1's declared `<files>`.

Resolved in favour of the plan's explicit task decomposition: implementation (`2e61e95`), then the D-15 wiring (`7d8a58c`), then the specification (`bdf1f82`). The plan's frontmatter is `type: execute` (not `type: tdd`), so no plan-level gate sequence applies. Flagged here rather than silently, since the `tdd="true"` attribute and the Task 3 file assignment are in direct conflict and a future planner should drop one or the other.

## Verification

| Gate | Result |
|------|--------|
| `:app:testDebugUnitTest --tests '*ResultsViewModelTest'` | PASS — 38/38 (21 pre-existing + 17 new) |
| `:app:testDebugUnitTest --tests '*ResultsCelebrationSoundTest' --tests '*ResultsScreenTest'` | PASS — celebration and screen behaviour unregressed |
| `:app:testDebugUnitTest --tests '*PlatformBoundaryTest' --tests '*PlatformBoundaryGateTest' --tests '*DomainPurityTest'` | PASS — the gateway's new data-layer dependency reaches no Hilt-visible SDK type |
| `:app:testDebugUnitTest` (whole module) | PASS — 468 tests, 0 failures, 0 errors |
| `:domain:test` | PASS |
| `:app:detekt :app:ktlintCheck` | PASS — `ReturnCount` green on `claimWinOnce` / `continueWith` / `shouldShowInterstitial` |
| `:app:adIdLiteralCheck :app:dpSpLiteralCheck :app:audioFocusLiteralCheck` | PASS |
| `grep -rn 'pegsRemaining == 1\|pegsRemaining != 1' ResultsViewModel.kt` | only the pre-existing `recordSolvedPlayOnce` and `tierFor` uses (plus the trap KDoc); the interstitial gate added none |
| `grep -c 'recordRewardedShown' AdMobAdGateway.kt` | 1 |
| `grep -ci 'android.util.Log\|Timber\|println(' AdMobAdGateway.kt` | 0 |
| `grep -ci 'diamond' ResultsViewModelTest.kt` | 8 (floor: 1) |
| `grep -c 'advanceTimeBy' ResultsViewModelTest.kt` | 11 (floor: 2) |
| `grep -c 'intPreferencesKey\|longPreferencesKey\|booleanPreferencesKey' ResultsViewModelTest.kt` | 0 |
| `git diff` on pre-existing tests | only the eight constructor call sites (+ one comment line); no assertion changed or deleted |

## Known Stubs

None in this plan's own surface. One **cross-plan** incompleteness is expected and by design: `ResultsRoute` / `ResultsActions` still invoke their navigation lambdas synchronously from `onClick`, so `onNextLevelTapped` / `onCatalogTapped` / `onNavigationHandled` currently have **no production caller** and `adScrimVisible` is not rendered. That wiring is plan 10-05's entire scope (the plan's own objective says so: "navigation timing moves into the ViewModel here; plan 10-05 rewires the route to it"). Until 10-05 lands, no interstitial reaches a real player from the Results screen.

## Threat Flags

None — no security-relevant surface beyond the plan's `<threat_model>`. Every mitigation it assigned is present and tested: the scrim is raised before the pause and lowered only in the `finally` plus `continueClaimed` blocks a second tap (T-10-04); `pendingNavigation` is state rather than an event and the route clears it through `onNavigationHandled()` (T-10-17); both teardown and navigation share one `finally` (T-10-18); the ViewModel takes one projected cadence value and never re-derives it (T-10-01); no logging call exists in `ResultsViewModel`, `ResultsUiState` or `FakeAdGateway` (T-10-09); no dependency was added (T-10-SC). T-10-06 (forged route args forcing ad eligibility) is **accepted**, and the acceptance is recorded in `isInterstitialEligible`'s KDoc as the plan required.

## User Setup Required

None — no external service configuration required by this plan. The phase-level prerequisite that a real AdMob interstitial unit must exist for release builds is unchanged and tracked in the ROADMAP's External Prerequisites table; debug builds use Google's test unit.

## Next Phase Readiness

- **Ready for plan 10-05 (route + scrim rendering).** The surface it consumes: `uiState.pendingNavigation` (a `ResultsNavTarget`, consumed once via `LaunchedEffect(target)` then cleared with `onNavigationHandled()`), `uiState.adScrimVisible` (a plain input-swallowing dim, **not** a timed animation), and the two tap entry points `onNextLevelTapped(nextLevel)` / `onCatalogTapped()`. The three non-eligible actions — Classic Replay, Home, Challenge-stuck Retry — must keep calling their nav lambdas directly and must not be routed through the ViewModel.
- **`ResultsScreenTest` can use the shared `FakeAdGateway`** — it was promoted to a top-level test double for exactly that reason.
- **Device UAT still owes two confirmations (10-06):** that the 500 ms dim reads as "something is coming" rather than as a stall, and RESEARCH assumption A1 (a same-process fullscreen ad Activity does not make `ProcessLifecycleOwner` dispatch `ON_STOP`). Neither is settleable by a JVM test.
- **No blockers.** The last outstanding cross-phase wiring obligation from 10-01/10-02 — `recordRewardedShown` — is now closed.

## Self-Check: PASSED

- `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsUiState.kt` — FOUND
- `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsViewModel.kt` — FOUND
- `app/src/main/java/com/app/pegsolitaire/platform/ads/AdMobAdGateway.kt` — FOUND
- `app/src/test/java/com/app/pegsolitaire/ui/results/FakeAdGateway.kt` — FOUND
- `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsViewModelTest.kt` — FOUND
- Commits `2e61e95`, `7d8a58c`, `bdf1f82` — all FOUND on `worktree-agent-a3551b4b2182dc6d1`
- No shared orchestrator artifact touched: `git diff HEAD~3 HEAD --name-only` lists no `STATE.md` and no `ROADMAP.md`

---
*Phase: 10-interstitials*
*Completed: 2026-09-03*
