---
phase: 11-play-games-session
fixed_at: 2026-09-06T16:55:02Z
review_path: .planning/phases/11-play-games-session/11-REVIEW.md
iteration: 1
findings_in_scope: 9
fixed: 9
skipped: 0
status: all_fixed
---

# Phase 11: Code Review Fix Report

**Fixed at:** 2026-09-06T16:55:02Z
**Source review:** .planning/phases/11-play-games-session/11-REVIEW.md
**Iteration:** 1
**Fix scope:** critical_warning (CR-01, WR-01 through WR-08; IN-01 to IN-03 out of scope)
**Branch:** `gsd-reviewfix/11-16053` (worktree `/private/tmp/sv-11-reviewfix-2VDbJK`, cut from `feature/phase-11-play-games-session` at `b0e54ca`)

**Summary:**
- Findings in scope: 9
- Fixed: 9 (5 of them logic changes flagged `fixed: requires human verification`)
- Skipped: 0

**Verification (final, on HEAD `c953467`):**
- `./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin ktlintCheck detekt` — PASS (exit 0, no warnings)
- `./gradlew :app:testDebugUnitTest` — PASS: 56 classes, 550 tests, 0 failures, 0 errors, 0 skipped
- Each fix was also compile/lint/detekt-checked and its affected test classes run before its own commit.

## Fixed Issues

### CR-01: Home sign-in error snackbar replays on every re-entry to Home and on every rotation

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt`, `app/src/main/java/com/app/pegsolitaire/ui/home/HomeUiState.kt`, `app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt`, `app/src/main/java/com/app/pegsolitaire/ui/home/HomeRoute.kt`, `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsViewModel.kt`, `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsUiState.kt`, `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt`, `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsRoute.kt`, `app/src/test/java/com/app/pegsolitaire/ui/home/HomeScreenTest.kt`, `app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt`, `app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsPlayGamesTest.kt`, `app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsPrivacyTest.kt`, `app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelTest.kt`, `app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt`
**Commit:** 80c643e
**Applied fix:** Made the tick a consumable event, as the review's primary suggestion: both ViewModels gain `onPlayGamesSignInErrorShown()` (resets the ordinal to 0), both screens gain an `onPlayGamesSignInErrorShown` parameter wired from their Routes, and the effects call it the moment they raise the snackbar (before the show suspends).

**Deliberate deviation from the review's code sketch — please verify on device.** The sketch keeps `LaunchedEffect(errorTick)` and consumes inside it. That is self-defeating: consuming moves the tick to 0, which changes the effect's key, which cancels the effect's coroutine, and `SnackbarHostState.showSnackbar` clears `currentSnackbarData` in its `finally` on cancellation — so the snackbar would vanish the frame it appeared. The effect is instead keyed on `(hostState, message)` and observes the ordinal through a `rememberUpdatedState` mirror in a `while (true)` loop: `snapshotFlow { latestTick }.first { it > 0 }` → consume → show → wait again. A re-entry therefore restarts the effect against a consumed 0 and shows nothing; a second failure raised while the message is up is announced immediately after it. The reviewer's literal "compose with tick = 1 and assert no snackbar" test does not apply to consume semantics (tick = 1 at composition now means "raised, not yet shown", and it correctly shows); the "returned to Home after an error" state is tick = 0, covered by the existing `aSignInErrorTickOfZeroIsNeverShown`.

**Tests added:** `HomeScreenTest.aShownSignInErrorIsConsumedAndDoesNotReplayWhenHomeReEntersComposition` and `SettingsPlayGamesTest.aShownSignInErrorIsConsumedAndDoesNotReplayWhenTheScreenRecomposesFromScratch` (tick raised → shown once → callback resets it → screen leaves and re-enters composition → silent, callback count still 1); `HomeViewModelTest.theScreenConsumesAShownSignInErrorSoItCannotReplay`; `SettingsViewModelTest.theScreenConsumesAShownSignInErrorSoARotationCannotReplayIt`.

### WR-01: `signIn()` reuses the pre-await Activity for the post-chooser player lookups

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt`, `app/src/test/java/com/app/pegsolitaire/platform/games/PlayGamesServiceTest.kt`
**Commit:** 452aba2
**Applied fix:** Added `hostActivityAfterAwait(captured) = activityProvider.current ?: captured` and routed both post-await `signedInSessionFor(...)` calls (in `signIn()` and in the bootstrap) through it, exactly as suggested. The test fixture now stubs `activityProvider.current` to `null` by default so every pre-existing test exercises the fallback branch.
**Tests added:** `signInResolvesThePlayerAgainstTheActivityResumedAfterTheChooser` (a second mock returned by `current` is what `getPlayersClient` receives twice; the captured one never), `signInFallsBackToTheCapturedActivityWhileNothingNewerIsResumed`, `bootstrapResolvesThePlayerAgainstTheActivityResumedAfterTheCheck`.

### WR-02: `bootstrapIfOptedIn` and `signIn` are unserialized concurrent writers to `_session`, and `signIn` never sets `bootstrapped`

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt`, `app/src/test/java/com/app/pegsolitaire/platform/games/PlayGamesServiceTest.kt`
**Commit:** 3a902d0
**Applied fix:** Added a private `sdkMutex`; both SDK-touching sections run under `withLock`. The bootstrap, once it holds the lock, returns immediately if `_session.value is SignedIn`; a signed-in `signIn()` sets `bootstrapped = true`. The locked bodies are inlined with labeled returns rather than split into helper functions, because two extra private functions pushed the class over detekt's `TooManyFunctions` threshold (13 > 11).
**Tests added:** `aBootstrapOverlappingAnExplicitSignInNeverRegressesTheSignedInAnswer` (signIn suspended on the Activity await while holding the lock, bootstrap fired behind it with `isAuthenticated` answering false; asserts final session `SignedIn`, `isAuthenticated` never called, one `initialize`) and `aSuccessfulSignInSatisfiesTheOncePerProcessSilentCheck`. Both were run against the pre-fix `PlayGamesService.kt` and failed there ("2 tests completed, 2 failed"), then pass on the fix.

### WR-03: Settings sign-in row has no in-flight guard — a double tap launches two concurrent SDK sign-ins

**Status:** fixed
**Files modified:** `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsViewModel.kt`, `app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelTest.kt`
**Commit:** 3128806
**Applied fix:** `private var signInJob: Job?`; `onSignInToPlayGames()` returns early while `signInJob?.isActive == true`, otherwise assigns the launched job. The optional `signInInFlight` UiState field was not added (kept the fix to the guard the finding requires).
**Tests added:** `aSecondTapWhileASignInIsInFlightIsIgnored` (two taps before `runCurrent()` → `signInCallCount == 1`, `setOptedInCount == 1`) and `aTapAfterThePreviousSignInHasResolvedStartsANewOne` (the guard is against overlap only, per D-13).

### WR-04: `runCatching` around `signIn()` swallows `CancellationException` and acts on it

**Status:** fixed
**Files modified:** `app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt`, `app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt`, `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsViewModel.kt`, `app/src/test/java/com/app/pegsolitaire/platform/games/PlayGamesServiceTest.kt`, `app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt`, `app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelTest.kt`
**Commit:** 2841ff6
**Applied fix:** `if (failure is CancellationException) throw failure` as the first statement of `onFailure` in all three places — in the service before the `_session` write and the status-code classification, in both ViewModels before the tick increment.
**Tests added:** `PlayGamesServiceTest.aCancelledSignInIsRethrownUntouchedAndNeverWritesTheSession` (`Tasks.forCanceled()` propagates as `CancellationException`, session stays `Unknown`), `HomeViewModelTest.aCancelledSignInRoundTripNeverRaisesTheErrorTick`, `SettingsViewModelTest.aCancelledSignInRoundTripNeverRaisesTheErrorTick`.

### WR-05: DataStore write failures in the opt-in handlers are uncaught and crash the app

**Status:** fixed
**Files modified:** `app/src/main/java/com/app/pegsolitaire/data/PlayGamesOptInRepository.kt`, `app/src/main/java/com/app/pegsolitaire/data/OnboardingRepository.kt`, `app/src/test/java/com/app/pegsolitaire/data/PlayGamesOptInRepositoryTest.kt`, `app/src/test/java/com/app/pegsolitaire/data/OnboardingRepositoryTest.kt`
**Commit:** 246ddca
**Applied fix:** Both setters go through a private `editFailOpen(transform)` that wraps `dataStore.edit` in `runCatching` and rethrows only `CancellationException`, so every caller inherits the policy; the class KDoc's fail-open section now covers writes. `OnboardingRepository.markSeen()` received the same one-line treatment, as the finding suggested.
**Tests added:** `bothWritesFailOpenWhenTheStoreCannotBeWritten` (throwing `updateData` → neither setter throws), `aCancelledWriteStillPropagatesAsCancellation`, `OnboardingRepositoryTest.markSeenFailsOpenWhenTheWriteErrors`. Both test `throwingDataStore()` doubles now throw `IOException` from `updateData` instead of `error(...)`.

### WR-06: The signed-in "non-tappable" row is still clickable, focusable and squishes on press

**Status:** fixed
**Files modified:** `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt`, `app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsPlayGamesTest.kt`
**Commit:** c497e61
**Applied fix:** `SettingsLinkRow.onClick` is now `(() -> Unit)?` and the chain ends in `.then(if (onClick != null) Modifier.claySquish().clickable(onClick = onClick) else Modifier)`; the signed-in row passes `onClick = null`. KDoc on both the row and `PlayGamesSection` updated (the old text presented `onClick = {}` as if it made the row inert).
**Tests:** `theSignedInRowIsNotAnOfferToSignInAgain` now asserts `assertHasNoClickAction()` before the existing no-handler tap check; new `theSignInRowIsAnActionableControl` asserts `assertHasClickAction()` on the signed-out row as the counterpart.

### WR-07: Home prompt visibility can transiently re-raise `true` after the synchronous hide

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt`
**Commit:** 6c1a1c8
**Applied fix:** `distinctUntilChanged()` on each `combine` input and on the combined result, exactly as the review's first suggestion. The "better still" single `promptPending` repository flow was not adopted: it would couple `OnboardingRepository` and `PlayGamesOptInRepository` (different keys, different owners) into one, a larger structural change than the finding needs.
**No new test.** Reproducing the flicker deterministically needs a shared `DataStore` fake feeding real repositories under an unconfined observer (the existing `FakeOnboardingRepository` / `FakePlayGamesOptInRepository` are independent `MutableStateFlow`s and are already distinct, so they cannot exhibit it). Rather than invent brittle coverage, this is flagged for human verification: accept the Home prompt on a device and confirm the dialog does not flash back open.

### WR-08: Cancel classification recognises only `CommonStatusCodes.CANCELED`; the sign-in-specific cancel code is treated as an error

**Status:** fixed: requires human verification
**Files modified:** `app/src/main/java/com/app/pegsolitaire/platform/games/GamesConstants.kt`, `app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt`, `app/src/test/java/com/app/pegsolitaire/platform/games/PlayGamesServiceTest.kt`, `.planning/phases/11-play-games-session/11-HUMAN-UAT.md`
**Commit:** c953467
**Applied fix:** `GoogleSignInStatusCodes` is not on the dependency graph (no `play-services-auth*` artifact), so per the finding's fallback `GamesConstants` declares `SIGN_IN_CANCELLED_STATUS_CODE = 12501` with its source URL cited, and exposes `CANCEL_STATUS_CODES = setOf(CommonStatusCodes.CANCELED, SIGN_IN_CANCELLED_STATUS_CODE)`. `signIn()` classifies with `statusCode in GamesConstants.CANCEL_STATUS_CODES`; the now-unused `CommonStatusCodes` import was dropped from the service and its KDoc updated. The D-04 emulator item (test 2 in `11-HUMAN-UAT.md`) now names both codes and asks the operator to record which one a dismissed chooser really reports.
**Tests added:** `signInSwallowsTheSignInSpecificCancelCodeAsADeclineToo`, mirroring the existing generic-cancel case.

## Skipped Issues

None — all in-scope findings were fixed.

## Notes for the orchestrator

- Nine commits on `gsd-reviewfix/11-16053`, oldest first: `80c643e` (CR-01), `452aba2` (WR-01), `3a902d0` (WR-02), `3128806` (WR-03), `2841ff6` (WR-04), `246ddca` (WR-05), `c497e61` (WR-06), `6c1a1c8` (WR-07), `c953467` (WR-08). Each carries the required `Co-Authored-By` and `Claude-Session` trailers.
- This file is uncommitted, per instruction. The worktree, the temp branch and the recovery sentinel at `.planning/phases/11-play-games-session/.review-fix-recovery-pending.json` were created by the orchestrator and are left in place for it to fast-forward `feature/phase-11-play-games-session`, remove the worktree, delete the branch and drop the sentinel (in that order).
- Human-verification items, in priority order: CR-01 (snackbar shows once and only once across back-navigation and rotation on both Home and Settings), WR-02/WR-01 (rotate during the account chooser after a first-ever opt-in: session must end `SignedIn` with the player's name), WR-07 (no dialog flash after answering the Home prompt), WR-08 (which cancel code the dismissed chooser reports — D-04 checklist).
- IN-01 to IN-03 were out of scope (`critical_warning`) and are untouched. IN-02's requested coverage is now largely present via the tests listed above.

---

_Fixed: 2026-09-06T16:55:02Z_
_Fixer: Claude (gsd-code-fixer)_
_Iteration: 1_
