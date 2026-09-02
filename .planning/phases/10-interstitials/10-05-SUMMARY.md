---
phase: 10-interstitials
plan: 05
subsystem: ads
tags: [interstitial, results, compose, scrim, pointer-input, backhandler, navigation, robolectric]

# Dependency graph
requires:
  - phase: 10-interstitials
    plan: 04
    provides: "ResultsUiState.adScrimVisible / pendingNavigation, ResultsNavTarget, and the onNextLevelTapped / onCatalogTapped / onNavigationHandled trigger surface this plan finally gives a production caller"
  - phase: 10-interstitials
    plan: 03
    provides: "the AdGateway interstitial slot and AdConstants.INTERSTITIAL_QUIESCENCE_MS the scrim covers"
  - phase: 02-gameplay-scoring
    provides: "ResultsScreen / ResultsActions / ResultsRoute and the six ResultsScreen(...) call sites whose signature this plan deliberately did not widen"
provides:
  - "AdQuiescenceScrim — the INT-04 input block: a full-bleed dim consuming pointers at PointerEventPass.Initial and swallowing back presses"
  - "RESULTS_AD_SCRIM_TEST_TAG — the scrim's presence/absence handle for the screen tests"
  - "results_ad_scrim_description — the scrim's accessibility announcement"
  - "The rewired ResultsRoute: the two D-06 taps enter the ViewModel, the other four stay direct pass-throughs, and pendingNavigation is consumed once per target"
affects: [10-06-device-uat]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "An overlay that must cover a Scaffold's TopAppBar is a SIBLING of the Scaffold, never a child of its content lambda"
    - "Blocking input is pointer consumption at PointerEventPass.Initial plus a BackHandler — an opaque Box on top blocks neither"
    - "A negative interaction assertion (\"the tap did not land\") is only meaningful paired with the positive control that proves the same tap normally does land"
    - "Navigation timing lives in a LaunchedEffect keyed on a StateFlow target field, never on a Channel/SharedFlow, when the host is STOPPED across the event"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/ui/results/AdQuiescenceScrim.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/results/ResultsRoute.kt
    - app/src/main/res/values/strings.xml
    - app/src/test/java/com/app/pegsolitaire/ui/results/ResultsScreenTest.kt

key-decisions:
  - "The scrim renders as a sibling of the Scaffold rather than inside its content lambda — inside, it sits within the content padding and leaves the TopAppBar's back arrow live, which is exactly the gap INT-04 names"
  - "Pointer consumption at PointerEventPass.Initial is load-bearing and was proven so: with it removed, the swallow test FAILS and the tap reaches the button, because a Box carrying only a background registers no pointer-input node and hit testing walks straight past it to the Scaffold"
  - "AD_SCRIM_ALPHA is 0.6f, not opaque: the M3 scrim token is fully opaque black, and a screen that went black for half a second would read as a crash rather than as a pause"
  - "D-08's 'no text, no spinner' governs pixels only — the scrim still carries a contentDescription, because a silent dead zone that swallows a screen-reader user's taps without explanation is not an accessible implementation of the same rule"
  - "The D-06 split is expressed as which lambda goes where in ResultsRoute, not as a condition: onReplay/onHome/onRetry/onNavigateBack never enter the ViewModel, so no code path exists by which they could reach the cadence policy"
  - "ResultsScreen's and ResultsActions' parameter lists were left byte-identical, keeping all six existing call sites compiling and honouring 10-CONTEXT's 'avoid structural Results rework' constraint"

patterns-established:
  - "Input-swallowing overlay: fillMaxSize + Initial-pass consumption + BackHandler + testTag + contentDescription, gated purely on a hoisted state flag"
  - "Paired swallow/control interaction tests, where the control is what stops the negative assertion passing vacuously"

requirements-completed: [INT-01, INT-04]

# Metrics
duration: 24min
completed: 2026-09-03
---

# Phase 10 Plan 05: Quiescence Scrim and Route Navigation Timing Summary

**The interstitial path now reaches a real player: the two D-06 continue taps enter the ViewModel, navigation is consumed once from state, and a full-bleed dim makes the whole Results screen — top bar included — genuinely inert while the ad opens.**

## Performance

- **Duration:** ~24 min
- **Started:** 2026-09-03T00:13:00Z (approx.)
- **Completed:** 2026-09-03T00:37:00Z
- **Tasks:** 2
- **Files modified:** 5 (1 created, 4 modified)

## Accomplishments

- **INT-04 is a real block, and that was proven rather than assumed.** The swallow test was deliberately driven RED before being accepted: with `pointerInput`'s `PointerEventPass.Initial` consumption removed and only the opaque `Box` left in place, `aTapBehindTheQuiescenceScrimNeverReachesThePrimaryAction` **fails** — the tap reaches the "Next level" button. A `Box` carrying only `background()` contributes no pointer-input node, so hit testing walks past it into the `Scaffold`. Covering the screen visually and blocking it are two different things, and only the consumption does the second.
- **The scrim covers the top bar.** It is rendered as a sibling of the `Scaffold` inside a new enclosing `Box`, not inside the `Scaffold`'s content lambda. Inside, it would have been laid out within the content padding and left the `TopAppBar`'s back arrow live above it — a working exit from a screen INT-04 requires to be inert.
- **Both input channels are closed.** Pointer events die at the `Initial` pass; a queued back press is taken by `BackHandler` and dropped. Neither API appeared anywhere under `app/src/main` before this file, and the KDoc says so explicitly so the next reader does not go hunting for a precedent that does not exist.
- **D-09's "navigation is not a button's job" is now structural.** `ResultsRoute` passes `viewModel::onNextLevelTapped` and `viewModel::onCatalogTapped` down, and a `LaunchedEffect(state.pendingNavigation)` consumes the target exactly once before clearing it through `onNavigationHandled()`. RESEARCH Pitfall 5's complaint — that `ResultsActions` invoked the nav lambdas synchronously in `onClick`, leaving nowhere to insert an ad — no longer holds.
- **D-06 is a shape, not a condition.** Four of the six lambdas (`onReplay`, `onHome`, `onRetry`, `onNavigateBack`) are still direct pass-throughs and never enter the ViewModel, so retry, exit and the INT-03 loss are *unable* to acquire an ad. Widening that set would require adding a `ResultsNavTarget` member and re-pointing a lambda in this file — visible and reviewable, not a silently loosened boolean.
- **Nothing downstream had to move.** `ResultsScreen`, `ResultsActions`, `ResultsRoute` and `AppNavHost` all kept their exact parameter lists; `git diff` against the plan's base commit for `AppNavHost.kt` and `ResultsActions.kt` is empty. The suite grew **468 → 472** tests with no pre-existing assertion touched.

## Task Commits

1. **Task 1: the quiescence scrim** — `8ab4ba2` (feat)
2. **Task 2 (RED/spec): the scrim block and its paired control** — `c3a08bb` (test)
3. **Task 2 (GREEN): the two continue taps routed through the ViewModel** — `fa33e47` (feat)

## Files Created/Modified

- `ui/results/AdQuiescenceScrim.kt` **(new)** — `internal @Composable fun AdQuiescenceScrim(modifier)`: `fillMaxSize()`, `MaterialTheme.colorScheme.scrim.copy(alpha = AD_SCRIM_ALPHA)`, the `awaitPointerEventScope` / `awaitPointerEvent(PointerEventPass.Initial)` / `consume()` loop, `BackHandler(enabled = true) { }`, the test tag and a `contentDescription`. The KDoc argues each of the two channels separately, names the file as the repo's first use of both APIs, and states why the block is a correctness device rather than a UX nicety (the `AdOfferDialog` precedent).
- `ui/results/ResultsScreen.kt` — `RESULTS_AD_SCRIM_TEST_TAG` added beside the two existing tag constants; the `Scaffold` wrapped in a `Box(modifier.fillMaxSize())` with the incoming `modifier` moved to that outer `Box`; `if (state.adScrimVisible) { AdQuiescenceScrim() }` as the `Scaffold`'s sibling, carrying a comment on why it is not inside the content lambda; KDoc extended with the overlay's INT-04 purpose. **Signature unchanged** (the same seven parameters).
- `ui/results/ResultsRoute.kt` — the `LaunchedEffect(state.pendingNavigation)` consumer with a comment covering both of its correctness properties (keyed on the target so recomposition cannot re-fire it; read from a `StateFlow` field so a value landing while the host is STOPPED behind the ad survives, RESEARCH Pitfall 6); `onCatalog`/`onNextLevel` re-pointed at the ViewModel; the four pass-throughs left alone; KDoc rewritten to state the D-06 split and why each excluded action is excluded, with `@param` docs noting that the two ad-eligible lambdas are now invoked from the effect rather than from `onClick`. **Parameter list unchanged.**
- `res/values/strings.xml` — `results_ad_scrim_description` ("Preparing"), with a comment explaining why a dim that draws no text still needs one.
- `test/ui/results/ResultsScreenTest.kt` — four cases (`theQuiescenceScrimIsDisplayedWhileAnInterstitialIsComing`, `theFastPathDimsNothing`, `aTapBehindTheQuiescenceScrimNeverReachesThePrimaryAction`, `theSamePrimaryActionTapLandsWhenNoAdIsComing`), an `AD_COMING_STATE` fixture, and a `setResultsContent(...)` helper. The swallow test's KDoc records that the control is what gives it meaning.

## Decisions Made

- **`AD_SCRIM_ALPHA = 0.6f`.** The M3 `scrim` token is fully opaque black (`OnboardingOverlay` uses it at full strength behind a sheet). At full opacity a half-second dim reads as a crash; at 0.6 every control unmistakably reads as unavailable while the celebration the player just earned stays visible behind it. It is an alpha `Float` as a private `const`, the shape `STAR_GLOW_ALPHA` already establishes — not a dimension, so it does not belong in `dimens.xml` and does not trip `dpSpLiteralCheck`.
- **No new `dimens.xml` token was needed.** A full-bleed dim has no dimension: `fillMaxSize()` is its entire geometry. The Phase-9 scoped-token convention was read and found not to apply.
- **The scrim is announced to accessibility services.** D-08 forbids drawn text and a spinner; it says nothing about semantics. A screen reader whose user's taps are being silently swallowed needs to be told the screen is busy, so `contentDescription` carries one neutral word.
- **Task 2 was committed as two gate commits rather than one.** `test(...)` then `feat(...)`, so the RED/GREEN sequence is legible in `git log` even though the plan's frontmatter is `type: execute`.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Recreated the gitignored `local.properties` in the worktree**

- **Found during:** Task 1, first Gradle invocation
- **Issue:** `local.properties` is gitignored, so a fresh worktree has none and every `:app` task fails with "SDK location not found". The same blocker 10-01 through 10-04 each hit; the executor prompt flags it as known.
- **Fix:** Wrote `sdk.dir=/Users/ermanergoz/Library/Android/sdk` into the worktree root.
- **Files modified:** `local.properties` (gitignored — deliberately not committed; committing it would leak a machine-specific absolute path).
- **Verification:** `:app:compileDebugKotlin` then ran normally.
- **Committed in:** Not committed (gitignored by design).

### Deliberate re-indentation in `ResultsScreen.kt`

Wrapping the `Scaffold` in a `Box` shifted the entire content block one level deeper, so `ResultsScreen.kt`'s diff is larger than the change it expresses (78 lines touched for what is structurally an added wrapper, an added `if`, and a comment). No content, ordering, modifier or value inside the block changed; ktlint's formatting rules required the `verticalArrangement = Arrangement.spacedBy(...)` argument to wrap at the new depth. Recorded here so a reviewer reading the diffstat does not go looking for a behavioural change that is not there.

---

**Total deviations:** 1 auto-fixed (1 blocking).
**Impact on plan:** None on behaviour. One worktree environment file. Every acceptance criterion in both tasks was met literally — no criterion needed to be satisfied "by intent" this time.

## Issues Encountered

- **An opaque overlay does not block input in Compose.** This was the plan's central assumption and it is worth stating as a discovered fact rather than a design note: `hitTestChildren` only stops at a sibling that actually registers a hit, and a `Box` whose only modifiers are `background()` and `fillMaxSize()` registers none — it has no pointer-input node. The tap goes straight through to the `Scaffold` beneath. Verified empirically by deleting the `pointerInput` block and watching the swallow test fail. Anyone later tempted to "simplify" the scrim by dropping the consumption loop will silently reopen the exact accidental-click window INT-04 exists to close.
- **`BackHandler` works under `createComposeRule()`.** It resolves `LocalOnBackPressedDispatcherOwner`, which is provided because the rule hosts content in a `ComponentActivity`. No test harness change was needed, and no additional dependency — `androidx.activity:activity-compose` was already an `implementation` dependency of `:app`.

## TDD Gate Compliance

Task 2 carries `tdd="true"` and the gate sequence is present in `git log`: `test(10-05)` (`c3a08bb`) → `feat(10-05)` (`fa33e47`).

One honest qualification. The four tests exercise the **scrim**, which Task 1 had already built, so at `c3a08bb` they pass rather than fail — a literal RED-before-the-test-commit was not available, because the plan assigns the scrim implementation to Task 1 and its tests to Task 2. Rather than let that stand as an untested claim, RED was established **directly against the mechanism**: the `PointerEventPass.Initial` consumption was temporarily removed from `AdQuiescenceScrim.kt`, the suite was re-run, and `aTapBehindTheQuiescenceScrimNeverReachesThePrimaryAction` failed. The file was then restored with `git checkout -- <file>` and the suite re-run green. So the assertion is known to be sensitive to the code it claims to pin, which is what the RED phase exists to establish.

The plan's frontmatter is `type: execute`, so no plan-level gate sequence applies. Flagged for the same reason 10-04 flagged its own conflict: a `tdd="true"` task whose subject was implemented by the previous task cannot have a literal RED, and a future planner should either co-locate the implementation with its tests or drop the attribute.

## Verification

| Gate | Result |
|------|--------|
| `:app:testDebugUnitTest --tests '*ResultsScreenTest'` | PASS — 9/9 (5 pre-existing + 4 new) |
| `:app:testDebugUnitTest --tests '*NavFlowTest' --tests '*ButtonSoundTest' --tests '*ResultsCelebrationSoundTest'` | PASS — the other five `ResultsScreen(...)` call sites still compile and pass |
| `:app:testDebugUnitTest` (whole module) | PASS — **472 tests, 0 failures, 0 errors** across 52 classes (468 before this plan) |
| `:domain:test` | PASS |
| `:app:detekt :app:ktlintCheck` | PASS |
| `:app:dpSpLiteralCheck` | PASS — no dp/sp literal reached the new `/ui/` file |
| `:app:adIdLiteralCheck :app:audioFocusLiteralCheck` | PASS |
| RED check: `pointerInput` removed from the scrim | `aTapBehindTheQuiescenceScrimNeverReachesThePrimaryAction` **FAILED**, as required; file restored and re-run green |
| `grep -n -A 9 '^fun ResultsScreen('` | the same seven parameters as before this plan |
| `grep -c 'onReplay = onReplay'` in `ResultsRoute.kt` | 1 (likewise `onHome = onHome`, `onRetry = onRetry`, `onNavigateBack = onNavigateBack`) |
| `git diff <base> -- AppNavHost.kt ResultsActions.kt` | empty — neither the nav host nor the action leaves needed a change |
| `git diff --diff-filter=D --name-only <base> HEAD` | empty — no file deleted by this plan |
| `grep -ci 'android.util.Log\|Timber\|println('` on both new/rewired files | 0 (T-10-09) |

## Known Stubs

None. This plan closes the cross-plan incompleteness 10-04 recorded: `onNextLevelTapped`, `onCatalogTapped` and `onNavigationHandled` now have a production caller, and `adScrimVisible` is rendered. The Results interstitial path is wired end to end from tap to ad to navigation.

## Threat Flags

None — no security-relevant surface beyond the plan's `<threat_model>`, and every mitigation it assigned is present and tested:

- **T-10-04** (accidental interstitial clicks as invalid traffic) — the scrim consumes at `PointerEventPass.Initial`, swallows back, and covers the `TopAppBar` by being a sibling of the `Scaffold`. Proven by the paired swallow/control tests, and the consumption's necessity proven by the RED check.
- **T-10-17** (double or lost navigation across the ad) — `LaunchedEffect` keyed on a `StateFlow` target field, cleared via `onNavigationHandled()`.
- **T-10-19** (a non-win action acquiring an ad) — structural: four lambdas never enter the ViewModel, confirmed by the pass-through greps.
- **T-10-20** (a permanently dead screen) — the scrim renders only from `state.adScrimVisible`, which 10-04 lowers in a `finally` on every exit path including cancellation and show failure.
- **T-10-09** (information disclosure) — no logging call in the scrim or the route.
- **T-10-SC** (dependency installs) — no dependency added; `androidx.activity:activity-compose` was already declared and on `:app`'s `implementation` configuration.

## User Setup Required

None — no external service configuration required by this plan.

## Next Phase Readiness

- **Ready for plan 10-06 (device UAT).** Two questions are now answerable on a device and only on a device: whether the 0.6-alpha dim reads as "something is coming" rather than as a stall over the `INTERSTITIAL_QUIESCENCE_MS` window, and RESEARCH assumption A1 (that a same-process fullscreen ad Activity does not make `ProcessLifecycleOwner` dispatch `ON_STOP`). Neither is settleable by a JVM test.
- **A third UAT item is worth adding:** confirm on-device that a hardware/gesture back press during the quiescence window is genuinely absorbed. Robolectric exercises the pointer half of INT-04 but not the gesture-navigation back half.
- **No blockers.**

## Self-Check: PASSED

- `app/src/main/java/com/app/pegsolitaire/ui/results/AdQuiescenceScrim.kt` — FOUND
- `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt` — FOUND
- `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsRoute.kt` — FOUND
- `app/src/main/res/values/strings.xml` — FOUND
- `app/src/test/java/com/app/pegsolitaire/ui/results/ResultsScreenTest.kt` — FOUND
- Commits `8ab4ba2`, `c3a08bb`, `fa33e47` — all FOUND on `worktree-agent-a15c6c1f6aeed5a2b`
- No shared orchestrator artifact touched: no `STATE.md` and no `ROADMAP.md` in this plan's diff

---
*Phase: 10-interstitials*
*Completed: 2026-09-03*
