---
phase: 11-play-games-session
plan: 03
subsystem: ui
tags: [play-games, opt-in, compose, dialog, snackbar, mvvm, tdd]

# Dependency graph
requires:
  - phase: 11-play-games-session
    plan: 01
    provides: "PlayGamesOptInRepository (open, D-17 two-flag persistence), GamesService.signIn() return-vs-throw contract, FakeGamesService"
  - phase: 02-onboarding
    provides: "HomeUiState.onboardingVisible / OnboardingRepository.onboardingSeen — the surface the opt-in prompt is sequenced behind"
  - phase: 09-consent-rewarded-ads
    provides: "AdOfferDialog (the clay dialog shell + stacked-action pattern), ClaySnackbarHost, GameScreen's AdMessageSnackbarEffect tick idiom"
provides:
  - "PlayGamesPromptDialog — the one-time D-15/D-16 opt-in prompt leaf, with dismissal wired to decline"
  - "HomeUiState.playGamesPromptVisible — the D-15 sequencing flag (onboarding seen AND question unanswered)"
  - "HomeUiState.playGamesSignInErrorTick — the D-18 one-shot error ordinal"
  - "HomeViewModel.onAcceptPlayGamesPrompt() / onDeclinePlayGamesPrompt() — the two answer handlers"
  - "play_games_signin_error — the single D-10/D-18 sign-in failure string, shared with plan 11-04's Settings row"
  - "Home's first ClaySnackbarHost"
affects: [11-04-settings-play-games-section, 12-leaderboards-achievements]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Two first-run surfaces sequenced by a combine() of two persisted flags rather than by a UI-side guard"
    - "Dialog dismissal as an ANSWER, not a non-answer — the mechanism that makes a one-time prompt genuinely one-time"
    - "Synchronous UI hide before the suspend side effect it triggers, so no surface waits on an SDK round trip"
    - "Back-press dismissal tested through the real ComponentDialog window under Robolectric"

key-files:
  created:
    - app/src/main/java/com/app/pegsolitaire/ui/home/PlayGamesPromptDialog.kt
    - app/src/test/java/com/app/pegsolitaire/ui/home/PlayGamesPromptDialogTest.kt
  modified:
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeUiState.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt
    - app/src/main/java/com/app/pegsolitaire/ui/home/HomeRoute.kt
    - app/src/main/res/values/strings.xml
    - app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/home/HomeScreenTest.kt
    - app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt

key-decisions:
  - "onDismissRequest is onDecline, not a no-op — a dismissal must WRITE the answer, or the prompt returns next launch and becomes the nag D-17 forbids"
  - "The accept handler hides the dialog synchronously BEFORE launching the coroutine, so the ordering that satisfies T-11-09 is structural rather than incidental"
  - "The two conditional hosts on HomeScreen stay independent — D-15's mutual exclusion is asserted once, in the ViewModel, not restated in the UI where the copies could drift"
  - "play_games_signin_error is authored once here and reused verbatim by plan 11-04, per D-10/D-18 naming the same wording"
  - "Dialog dismissal is tested by dispatching back on the real ComponentDialog's own onBackPressedDispatcher — the host Activity's dispatcher would not exercise the path a player takes"

patterns-established:
  - "Sequenced first-run surfaces: visibility as a pure combine() of two persisted flags makes 'shown exactly once, in this order' a property of the data rather than of screen-side ordering luck"
  - "Dismissal-as-answer: any prompt that must never return wires onDismissRequest to its negative handler, so walking away is recorded"
  - "Hide-then-suspend: a handler that triggers an SDK round trip updates UI state synchronously first, leaving no in-flight window for a dismissal to race"

requirements-completed: [PGS-01]

# Metrics
duration: 35min
completed: 2026-09-04
---

# Phase 11 Plan 03: Home Play Games Opt-In Prompt Summary

**Home now asks the single "Sign in to Play Games?" question a player ever sees — sequenced after onboarding, answered permanently by any exit including a dismissal, signing in without ever making the player wait, and apologising only for a real SDK error.**

## Performance

- **Duration:** ~35 min (excluding an 11m 9s full `check` run)
- **Completed:** 2026-09-04
- **Tasks:** 3 of 3
- **Files modified:** 10 (2 created, 8 modified)

## Accomplishments

- **The prompt is one-time by construction, not by convention.** `playGamesPromptVisible` is a `combine(onboardingSeen, promptAnswered) { seen, answered -> seen && !answered }` — so "after onboarding, before any answer, never again" is a property of two persisted flags rather than of screen-side ordering. Once `promptAnswered` flips, the collector can never emit `true` again. That is T-11-08's mitigation, and it is asserted by two dedicated tests, including one that re-opens the onboarding overlay afterwards.
- **Every exit is an answer.** `onDismissRequest` is wired to `onDecline`, so a scrim tap and a back press write the flags exactly as "Not now" does. This is the load-bearing detail: had dismissal been a non-answer, the flags would stay unwritten and the dialog would return on the next launch — the precise nag D-17 exists to forbid. It is proven by dispatching back on the real `ComponentDialog`'s own `onBackPressedDispatcher`, the path a player's back press actually takes.
- **Accepting never makes the player wait.** `onAcceptPlayGamesPrompt` hides the dialog *synchronously, before* the coroutine that persists and signs in is launched. A test asserts this with no `runCurrent()` at all, so the dialog is already gone while `signInCallCount` is still 0 — PGS-01's "no wall, no pause" (T-11-09) enforced structurally.
- **A decline and an error are visibly different outcomes.** Only a throw from `signIn()` bumps `playGamesSignInErrorTick`; a `false` return — the player dismissing Google's own chooser — takes no action at all. Both branches are covered against `FakeGamesService`'s `SignInBehavior`, so plan 11-01's contract is now exercised, not just documented.
- **Home gained its first snackbar host.** `ClaySnackbarHost` plus a `PlayGamesSignInErrorSnackbarEffect` copied from `GameScreen`'s proven shape, including the `> 0` phantom-cue guard — without which every visit to Home would open by apologising for a sign-in nobody attempted.

## Task Commits

1. **Task 1: HomeViewModel sequencing + accept/decline handlers** — `6128dca` (test, RED) → `470f655` (feat, GREEN)
2. **Task 2: PlayGamesPromptDialog leaf + Phase-11 strings** — `e9020a2`
3. **Task 3: HomeScreen/HomeRoute wiring + D-18 snackbar** — `07df59f`

No REFACTOR commit was needed — the GREEN implementation landed in its final shape.

## Files Created/Modified

- `ui/home/PlayGamesPromptDialog.kt` (new, 195 lines) — the D-15/D-16 prompt: `AdOfferDialog`'s clay shell (`extraLarge` / `surfaceContainerHigh` / `clayShadow(elevation_dialog)`, inset `spacing_lg`, capped at `content_max_width`) with a stacked full-width gradient "Sign in" over a peer `TextButton` "Not now", both carrying their `withClickSound` cues. No loading state, no icon. Exports `PLAY_GAMES_PROMPT_DIALOG_TEST_TAG`.
- `ui/home/HomeUiState.kt` — `playGamesPromptVisible` and `playGamesSignInErrorTick` as peer fields with full `@property` KDoc citing D-15 and D-18.
- `ui/home/HomeViewModel.kt` — injects `PlayGamesOptInRepository` + `GamesService`; adds the `combine` collector as a fourth parallel `init` launch and the two handlers.
- `ui/home/HomeScreen.kt` — `SnackbarHostState` + `ClaySnackbarHost`, the two new handler params, the conditional dialog host, and the private `PlayGamesSignInErrorSnackbarEffect`.
- `ui/home/HomeRoute.kt` — forwards both handlers by method reference.
- `res/values/strings.xml` — a Phase-11 block: the four prompt strings plus `play_games_signin_error`.
- `ui/home/HomeViewModelTest.kt` — +10 tests (26 total) and a `FakePlayGamesOptInRepository` subclassing the `open` repository plan 11-01 provided.
- `ui/home/PlayGamesPromptDialogTest.kt` (new) — 5 tests including the dismissal-is-decline proof.
- `ui/home/HomeScreenTest.kt` — +4 tests (8 total): dialog hosting both ways, the 0→1 tick snackbar under a live recomposition, and the tick-0 silence.
- `ui/ButtonSoundTest.kt` — its `renderHome` helper updated for the new signature (deviation 1).

## Decisions Made

- **Dismissal maps to decline rather than to a separate "asked but unanswered" state.** A third state would be defensible in the abstract — the player did not really choose. It was rejected because the only thing that state can do is bring the dialog back, and D-16 explicitly rules a tap-outside to be "Not now". Recording it keeps the prompt honestly one-time; the Settings row (D-19) is the door left open.
- **The two conditional hosts on `HomeScreen` are independent, with no `else`.** Adding a UI-side guard would express D-15's mutual exclusion a second time, in a place that could later disagree with the ViewModel. The invariant is asserted once, where it is produced, by `theOptInPromptStaysHiddenWhileTheOnboardingOverlayIsStillShowing`.
- **`runCatching` around only the `signIn()` call, not the whole handler body.** `setOptedIn()` must have completed before the guarded call, so a sign-in failure can never skip the persistence that makes the next launch retry silently (D-18).
- **One error string, authored here.** D-10 and D-18 name identical wording for the same event seen from Home and from Settings. Two resources would be two wordings the moment either is edited, so plan 11-04 reuses `play_games_signin_error` verbatim.
- **The dismissal test drives the real dialog window.** `ShadowDialog.getLatestDialog()` → `ComponentDialog.onBackPressedDispatcher.onBackPressed()`. A Compose `Dialog` owns its own back dispatcher, so going through the host Activity would have tested nothing and passed anyway.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] `ButtonSoundTest` did not compile after the `HomeScreen` signature change**
- **Found during:** Task 3
- **Issue:** `app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt` has its own `renderHome` helper calling `HomeScreen(...)`. Adding the two required handler parameters broke it — `No value passed for parameter 'onAcceptPlayGamesPrompt'` — so the whole unit-test source set failed to compile and no verification command in the plan could run. The file is not in the plan's `files_modified`, but the breakage is caused directly by this task's change.
- **Fix:** Passed `{}` for both new parameters, with a comment pointing at `PlayGamesPromptDialogTest` as where the prompt's own sound cues are asserted. No behavioural change to that suite.
- **Files modified:** `app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt`
- **Verification:** `./gradlew :app:testDebugUnitTest --tests "com.app.pegsolitaire.ui.home.*"` green; full `./gradlew check --offline` green.
- **Committed in:** `07df59f` (part of the task commit)

**2. [Rule 3 - Blocking] Worktree was missing the gitignored `local.properties`**
- **Found during:** setup, before Task 1
- **Issue:** `local.properties` is gitignored and therefore absent from a fresh worktree, and `ANDROID_HOME` is unset, so `./gradlew` cannot locate the SDK. Same environmental blocker plan 11-01 recorded.
- **Fix:** Copied the main checkout's `local.properties` into the worktree root. It stays untracked and was never staged.
- **Files modified:** none tracked.
- **Committed in:** n/a — no tracked file changed.

---

**Total deviations:** 2 auto-fixed (both Rule 3 blocking)
**Impact on plan:** None on scope. Deviation 1 is a mechanical call-site update forced by an intended signature change; deviation 2 is environmental and left no tracked change.

## Issues Encountered

- **The plan's Task 1 pseudocode had the accept handler hide the dialog *after* launching the coroutine** (`viewModelScope.launch { ... }` then `_uiState.update { ... }`), while its own prose and threat register (T-11-09) require the synchronous hide to come first. The prose was followed; the pseudocode ordering would still have worked in practice (the launch does not run before the next suspension point) but would have made the guarantee incidental rather than structural. `acceptingHidesThePromptBeforeTheSignInRoundTripEvenBegins` now pins it.
- **The plan's pseudocode also raised the error snackbar on `if (!signedIn)`**, which contradicts D-18's explicit "a plain dismiss of the SDK's chooser is silent" and the plan's own behavior list. The behavior list was followed: only a throw raises the tick.
- **A latent, unreachable stacking case exists by design.** `playGamesPromptVisible` combines on `onboardingSeen` (the persisted flag), not on `onboardingVisible` (the transient overlay). If a player could tap How-to-play while the prompt is showing, both would render. They cannot — the prompt is a modal dialog whose scrim covers the top bar — and the plan explicitly rules that no extra guard is wanted here. Noted rather than fixed.
- Pre-existing `ExperimentalCoroutinesApi` opt-in warnings in `SettingsViewModelTest`/`SettingsViewModelSoundTest` were observed again; out of scope, untouched.

## Known Stubs

None. Every surface this plan renders is wired to real state: the dialog's visibility comes from two persisted DataStore flags through `PlayGamesOptInRepository`, and both handlers write that store and reach the `GamesService` seam. The seam's *implementation* is still `NoOpGamesService` in this worktree — that is plan 11-02's binding swap, by design, and is not a stub introduced here.

## Threat Flags

None. This plan introduces no new network endpoint, auth path, file access pattern or schema change beyond the two non-identifying booleans plan 11-01 already registered. Its three registered threats are all mitigated as planned:

- **T-11-08 (dark pattern / Play policy)** — the prompt is permanently retired once `promptAnswered` is true, including on dismissal; asserted by `theOptInPromptNeverShowsAgainOnceAnswered` and `theOptInPromptStaysRetiredEvenWhenTheHelpAffordanceReopensOnboarding`. The two actions are styled peers, not a nudged pair.
- **T-11-09 (denial of service)** — the dialog is hidden before the suspend call is launched; asserted by `acceptingHidesThePromptBeforeTheSignInRoundTripEvenBegins`.
- **T-11-10 (information disclosure)** — neither the dialog nor the ViewModel renders or logs `playerId`/`displayName`; the accept path only calls `signIn()`, which carries no PII across the boundary. Nothing in this plan logs at all.

## Verification

- `./gradlew :app:testDebugUnitTest --tests "*.HomeViewModelTest"` — 26 tests, 0 failures
- `./gradlew :app:testDebugUnitTest --tests "*.PlayGamesPromptDialogTest"` — 5 tests, 0 failures
- `./gradlew :app:testDebugUnitTest --tests "*.HomeScreenTest"` — 8 tests, 0 failures
- `./gradlew :app:testDebugUnitTest --tests "com.app.pegsolitaire.ui.home.*"` — green
- `./gradlew :app:detekt ktlintCheck --offline` — green (mandatory CLAUDE.md gate, run before every task commit)
- `./gradlew check --offline` — **BUILD SUCCESSFUL in 11m 9s** (full suite, including Android Lint, the ArchUnit boundary rules, the dp/sp literal check and the Roborazzi screenshot tests)

## TDD Gate Compliance

Task 1 was `tdd="true"` and shows the required gate sequence in git log: `6128dca` (`test(...)`, RED — confirmed by a real compiler failure, `Unresolved reference 'playGamesPromptVisible'`, `'onDeclinePlayGamesPrompt'`, and `Too many arguments for constructor`) followed by `470f655` (`feat(...)`, GREEN). No test passed unexpectedly during RED. Tasks 2 and 3 were not marked `tdd`, and their tests ship in the same commit as the code they cover, as planned.

## User Setup Required

None for this plan. The phase-level D-04 debt is unchanged: until a Play Console project exists, `signIn()`'s cancel-vs-error classification stays `[UNVERIFIED]` in `GamesService`'s KDoc. Note that this plan's *callers* are unaffected by how that resolves — they branch only on "returned false" vs "threw", so a corrected classification in `PlayGamesService` changes nothing here.

## Next Phase Readiness

Plan 11-04 (Settings Play Games section) is unblocked and inherits two things from this plan:
- **`play_games_signin_error` already exists** — reuse it verbatim for the D-10 row error; do not author a second string.
- **The accept-handler shape is established** — persist via `setOptedIn()`, then `runCatching { signIn() }` with only the failure branch surfacing a snackbar. The Settings row's D-19 write is the same call.

One integration note for the phase verifier: this plan compiles and passes entirely against `NoOpGamesService`, whose `signIn()` always returns `false`. That is the silent-decline branch, so on a merge of wave 2 the prompt will accept, persist the opt-in, and correctly show nothing — until plan 11-02's real `PlayGamesService` binding lands. The two plans touch disjoint files.

## Self-Check: PASSED

All 10 source/test files claimed above exist on disk (verified by `[ -f ]`), and all 4 task commits (`6128dca`, `470f655`, `e9020a2`, `07df59f`) are present in `git log`. `git status --short` is clean apart from the untracked, gitignored `local.properties`. No missing items.

---
*Phase: 11-play-games-session*
*Completed: 2026-09-04*
