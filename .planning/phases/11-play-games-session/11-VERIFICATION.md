---
phase: 11-play-games-session
verified: 2026-09-03T22:16:47Z
status: human_needed
score: 15/15 must-haves verified
overrides_applied: 0
human_verification:
  - test: "Install a debug build on an emulator with Play Games and Google Play services, once a PGS Play Console project + OAuth credentials (both debug and release SHA-1) + tester account exist (D-04). Accept the Home 'Sign in to Play Games?' prompt, confirm sign-in completes, kill the process and relaunch."
    expected: "The stock Play Games 'Welcome back' popup appears at launch with no app-owned dialog, no wall and no pause before a level can start (PGS-01)."
    why_human: "No Play Console project exists yet (D-04); PlayGamesService.bootstrapIfOptedIn()/signIn() can only be proven against JVM MockK fakes of GamesSignInClient/PlayersClient today, not against Google's real Task<AuthenticationResult> behavior."
  - test: "On the same emulator, tap the Settings 'Sign in to Play Games' row and dismiss the account chooser without picking an account; then tap it again and complete sign-in with an account not registered as a tester."
    expected: "Dismissing the chooser is silent (no snackbar, row stays); an unregistered/erroring account shows the 'Couldn't sign in to Play Games' snackbar (PGS-03/D-10)."
    why_human: "Google does not document the Task<AuthenticationResult> failure shape for a plain user cancel versus a genuine SDK error (RESEARCH Open Question 2). PlayGamesService.signIn() implements a conservative CommonStatusCodes.CANCELED-based classification marked '[UNVERIFIED against a live Play Console project — D-04]' in its own KDoc; only a real device run can confirm or correct it."
  - test: "With Phase 10 interstitial foreground-cadence logging on, trigger the manual Settings sign-in row (or the Home prompt's 'Sign in' action) and watch for spurious foreground/background transitions."
    expected: "The Play Games account chooser and the 'Welcome back' popup do not trip AppForegroundTracker's interstitial cadence."
    why_human: "Requires a live SDK Activity in-process; cannot be exercised by a JVM/Robolectric test double."
---

# Phase 11: Play Games Session Verification Report

**Phase Goal:** Player is asked once whether to sign in to Play Games, opted-in players are signed in silently on every later launch, and everything works if they say no.
**Verified:** 2026-09-03T22:16:47Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Non-opted-in players never touch the Play Games SDK — `bootstrapIfOptedIn()` is the single documented gate (D-01) | VERIFIED | `PlayGamesService.bootstrapIfOptedIn()` reads `optInRepository.optedIn.first()` before awaiting an Activity or touching the SDK; `PlayGamesServiceTest.bootstrapForANonOptedInPlayerResolvesSignedOutWithoutTouchingTheSdk` asserts `coVerify(exactly = 0) { activityProvider.awaitActivity() }` and `verify(exactly = 0) { PlayGamesSdk.initialize(any()) }` |
| 2 | A plain decline and a genuine SDK error are distinguishable via `signIn()`'s return-vs-throw contract (D-10) | VERIFIED | `GamesService.signIn()` KDoc documents the contract; `PlayGamesService.signIn()` returns `false`/silent for `CommonStatusCodes.CANCELED`, rethrows otherwise; `HomeViewModel.onAcceptPlayGamesPrompt`/`SettingsViewModel.onSignInToPlayGames` only increment the error tick `.onFailure {}`, never on a `false` return — proven by `HomeViewModelTest`/`SettingsViewModelTest` and `PlayGamesServiceTest.signInSwallowsAPlainCancellationRatherThanRaisingAnError` / `signInRethrowsAGenuineSdkErrorSoTheCallerCanSurfaceIt` |
| 3 | The opt-in prompt's answer persists across process restarts; "Not now" is permanent; Settings is the only later re-entry (D-17) | VERIFIED | `PlayGamesOptInRepository.setNotNow()`/`setOptedIn()` write both DataStore keys atomically; `PlayGamesOptInRepositoryTest` (5 tests) proves default-false, atomic writes, fail-open on corrupt store; `HomeViewModelTest.theOptInPromptNeverShowsAgainOnceAnswered` |
| 4 | A display-name lookup failure is distinct from a sign-in failure (D-14) | VERIFIED | `PlayGamesService.resolveSignedInState` falls back to `SignedIn(playerId, displayName = null)` on a failed `getCurrentPlayer()`; `PlayGamesServiceTest.aFailedDisplayNameLookupDegradesTheNameAndNeverTheSignIn`; `SettingsScreen.PlayGamesSection` renders the D-14 fallback string, proven by `SettingsPlayGamesTest` |
| 5 | Player is asked once, on Home, only after onboarding is dismissed, and never again once answered (D-15) | VERIFIED | `HomeViewModel` combines `onboardingRepository.onboardingSeen` and `optInRepository.promptAnswered` into `playGamesPromptVisible`; `HomeViewModelTest.theOptInPromptStaysHiddenWhileTheOnboardingOverlayIsStillShowing` / `theOptInPromptShowsOnceOnboardingIsSeenAndTheQuestionIsStillUnanswered` / `theOptInPromptStaysRetiredEvenWhenTheHelpAffordanceReopensOnboarding` |
| 6 | Declining leaves the game exactly as it was — no SDK call, only persisted flags change (D-16/D-17) | VERIFIED | `HomeViewModel.onDeclinePlayGamesPrompt()` calls only `optInRepository.setNotNow()`, never `gamesService`; `HomeViewModelTest.decliningHidesThePromptPersistsNotNowAndNeverTouchesTheSdk` |
| 7 | Accepting calls `signIn()` immediately; only a genuine SDK error surfaces a Home snackbar (D-18) | VERIFIED | `HomeViewModel.onAcceptPlayGamesPrompt()` hides dialog synchronously, then `setOptedIn()` + `runCatching { gamesService.signIn() }.onFailure { tick++ }`; `HomeScreen`'s `PlayGamesSignInErrorSnackbarEffect` keyed on `playGamesSignInErrorTick`; `HomeViewModelTest.aGenuineSignInErrorRaisesTheErrorTick`, `HomeScreenTest` |
| 8 | Tapping outside the dialog or pressing back counts as "Not now", never accept (D-16) | VERIFIED | `PlayGamesPromptDialog`'s `Dialog(onDismissRequest = onDecline, ...)`; `PlayGamesPromptDialogTest` (5 tests) covers accept/decline/outside-dismiss |
| 9 | The real `GamesService` binding is live; `MainActivity` fires the bootstrap once per Activity creation, fire-and-forget (PGS-01) | VERIFIED | `PlatformModule.bindGamesService(impl: PlayGamesService): GamesService`; `MainActivity.onCreate()` calls `gamesService.bootstrapIfOptedIn()` immediately after `consentGateway.bootstrap()`, before `setContent {}` |
| 10 | For opted-in players, missing Play services/app or offline all resolve to `SignedOut` — one state, no retry (D-12) | VERIFIED | `PlayGamesService.bootstrapIfOptedIn`/`signIn` wrap every SDK call in `runCatching`, degrading to `SignedOut` on any throw or timeout; `PlayGamesServiceTest.bootstrapResolvesSignedOutWhenTheAuthenticationCheckThrows`, `...WhenNoForegroundActivityEverArrives` |
| 11 | The Play Games Settings section is completely absent while session is `Unknown` (D-11) | VERIFIED | `SettingsScreen.PlayGamesSection` returns before rendering anything `if (gamesSignInState == GamesSignInState.Unknown) return`; `SettingsPlayGamesTest` asserts no header/row while `Unknown` |
| 12 | Signed-in players see a non-tappable "Signed in as <name>" row, or the D-14 fallback (D-06/D-14) | VERIFIED | `PlayGamesSection`'s `is GamesSignInState.SignedIn` branch renders `SettingsLinkRow(onClick = {})` with `displayName`-formatted or fallback label; `SettingsPlayGamesTest` |
| 13 | Tapping the Settings sign-in row persists both D-19 flags before calling `signIn()`; row stays after decline (D-13) | VERIFIED | `SettingsViewModel.onSignInToPlayGames()` awaits `optInRepository.setOptedIn()` before `gamesService.signIn()`; `SettingsViewModelTest` |
| 14 | Settings has a new "Play Games" section between General and Privacy (D-08) | VERIFIED | `SettingsScreen`'s `Column` calls `GeneralSection(...)`, then `PlayGamesSection(...)`, then `PrivacySection(...)`; `PLAY_GAMES_HEADER_ORDER = 3`, `PLAY_GAMES_ROW_ORDER = 4`, `PRIVACY_HEADER_ORDER = 5` |
| 15 | The sign-in row reads exactly "Sign in to Play Games" / "Leaderboards, achievements and cloud save" (D-09) | VERIFIED | `strings.xml`: `settings_play_games_signin_label` = "Sign in to Play Games", `settings_play_games_signin_supporting` = "Leaderboards, achievements and cloud save" |

**Score:** 15/15 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `platform/games/GamesSignInState.kt` | `SignedIn(playerId, displayName)` — D-07/D-14 | VERIFIED | `displayName: String? = null` present, KDoc documents D-14 contract |
| `platform/games/GamesService.kt` | `bootstrapIfOptedIn()` contract — D-01 | VERIFIED | Method present with full D-01/D-02/D-10 KDoc |
| `data/PlayGamesOptInRepository.kt` | D-17 persistence | VERIFIED | `setNotNow()`/`setOptedIn()` atomic two-key writes |
| `platform/games/FakeGamesService.kt` | Shared test double | VERIFIED | `SignInBehavior` sealed interface (Success/Decline/Error), `emitSession`, call counters; consumed by both `ui.home` and `ui.settings` tests |
| `platform/games/PlayGamesService.kt` | Real GamesService impl — PGS-01/02/03 | VERIFIED | 265 lines; `@Singleton`, `AtomicBoolean` guard, explicit `SignedOut` assignment on every exit branch (never a bare early return), no logging call present |
| `platform/games/GamesConstants.kt` | Scoped Activity-wait timeout | VERIFIED | `ACTIVITY_WAIT_MS = 10_000L`, package-scoped per CLAUDE.md constants rule |
| `platform/games/PlayGamesServiceTest.kt` | Opted-in gate + throw/false coverage | VERIFIED | 13 tests, all green, MockK-verified negative assertions (`coVerify(exactly = 0)`) |
| `ui/home/PlayGamesPromptDialog.kt` | D-15/D-16 opt-in dialog | VERIFIED | 187 lines; `onDismissRequest = onDecline`, stacked Sign-in/Not-now actions, `PLAY_GAMES_PROMPT_DIALOG_TEST_TAG` |
| `ui/home/HomeUiState.kt` | `playGamesPromptVisible`/`playGamesSignInErrorTick` | VERIFIED | Both fields present with KDoc |
| `ui/settings/SettingsScreen.kt` | `PlayGamesSection` — PGS-02/03 | VERIFIED | Exhaustive `when` (no `else`), three-state rendering, correctly positioned |
| `ui/settings/SettingsPlayGamesTest.kt` | Three-state Compose coverage | VERIFIED | 11 tests, all green |

### Key Link Verification

| From | To | Via | Status | Details |
|------|-----|-----|--------|---------|
| `PlayGamesOptInRepository.kt` | `AppPreferences.kt` | shared `DataStore<Preferences>` constructor injection | WIRED | `@Inject internal constructor(private val dataStore: DataStore<Preferences>)`, same singleton `DataModule` provider |
| `PlatformModule.kt` | `PlayGamesService.kt` | `@Binds @Singleton bindGamesService` | WIRED | `abstract fun bindGamesService(impl: PlayGamesService): GamesService` |
| `MainActivity.kt` | `PlayGamesService.kt` | `gamesService.bootstrapIfOptedIn()` fire-and-forget | WIRED | Called in `onCreate()` immediately after `consentGateway.bootstrap()`, before `setContent` |
| `HomeScreen.kt` | `PlayGamesPromptDialog.kt` | conditional host on `state.playGamesPromptVisible` | WIRED | `if (state.playGamesPromptVisible) { PlayGamesPromptDialog(...) }` |
| `HomeViewModel.kt` | `PlayGamesOptInRepository.kt` | `setOptedIn()`/`setNotNow()` from accept/decline handlers | WIRED | Both handlers call the matching repository method |
| `SettingsScreen.kt` | `SettingsViewModel.kt` | `state.gamesSignInState` render + `onSignInToPlayGames` forwarded from `SettingsRoute` | WIRED | `SettingsRoute` forwards `onSignInToPlayGames = viewModel::onSignInToPlayGames`; `SettingsScreen` renders `state.gamesSignInState` |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|---------------------|--------|
| `SettingsScreen.PlayGamesSection` | `state.gamesSignInState` | `SettingsViewModel` collects `gamesService.session` (backed by `PlayGamesService._session`, a real `MutableStateFlow` updated from live SDK calls, not a static value) | Yes | FLOWING |
| `HomeScreen` prompt/snackbar | `state.playGamesPromptVisible` / `playGamesSignInErrorTick` | `HomeViewModel` `combine()` of two persisted DataStore flows; error tick incremented only inside `runCatching {...}.onFailure {}` around a real `gamesService.signIn()` call | Yes | FLOWING |

### Behavioral Spot-Checks

Skipped — this is a compiled Android app with no runnable CLI/HTTP entry points; behavior is proven exclusively through the JVM/Robolectric unit + Compose UI test suite (see below), which was executed directly rather than spot-checked.

### Probe Execution

Not applicable — no `scripts/*/tests/probe-*.sh` convention or PLAN-declared probes exist for this phase; verification runs through the standard Gradle test suite instead.

### Targeted Test Re-Run (executed by verifier, not taken from SUMMARY claims)

| Test class | Tests | Result |
|---|---|---|
| `NoOpGamesServiceTest` | 10 | PASS |
| `PlayGamesOptInRepositoryTest` | 5 | PASS |
| `PlayGamesServiceTest` | 13 | PASS |
| `HomeViewModelTest` | 26 | PASS |
| `PlayGamesPromptDialogTest` | 5 | PASS |
| `HomeScreenTest` | 8 | PASS |
| `SettingsViewModelTest` | 16 | PASS |
| `SettingsPlayGamesTest` | 11 | PASS |
| `PlatformBoundaryGateTest` (re-run to confirm the new `PlayGamesService` binding does not break `PLAT-07`) | — | PASS |

All re-run via `./gradlew :app:testDebugUnitTest --offline --tests "com.app.pegsolitaire.platform.games.*" --tests "com.app.pegsolitaire.data.PlayGamesOptInRepositoryTest" --tests "com.app.pegsolitaire.ui.home.*" --tests "com.app.pegsolitaire.ui.settings.*"`; `app/build/test-results/testDebugUnitTest/TEST-*.xml` timestamps and `tests=".." failures="0" errors="0"` counts confirmed directly from the XML reports produced by this run.

### Requirements Coverage

| Requirement | Source Plan(s) | Description | Status | Evidence |
|-------------|-----------------|--------------|--------|----------|
| PGS-01 | 11-01, 11-02, 11-03 | Player asked once, accepted players signed in silently on later launches, gameplay never blocked | SATISFIED | Truths 1, 5, 6, 7, 9 above |
| PGS-02 | 11-01, 11-02, 11-04 | Every feature works fully when sign-in unavailable/declined, social surfaces hidden not broken | SATISFIED | Truths 10, 11 above; `NoOpGamesService`/`PlayGamesService` never block gameplay; `PlayGamesSection` hides while `Unknown`/renders correctly while `SignedOut` |
| PGS-03 | 11-01, 11-02, 11-04 | Player can sign in manually from Settings when automatic sign-in did not happen | SATISFIED | Truths 12, 13, 14, 15 above |

No orphaned requirements: REQUIREMENTS.md lines 186-188 map exactly PGS-01/02/03 to Phase 11, and all three appear in at least one plan's `requirements:` frontmatter.

### Anti-Patterns Found

None. Scanned all 18 files modified across the four plans for `TBD|FIXME|XXX|TODO|HACK|PLACEHOLDER|placeholder|coming soon|not yet implemented|not available` — zero matches. The one intentionally-documented open item (`signIn()`'s cancel-vs-error classification) is explicitly marked `[UNVERIFIED against a live Play Console project — D-04]` inline in both `GamesService.kt` and `PlayGamesService.kt` KDoc, citing the specific decision (D-04) that defers it — this is the codebase's established "named open uncertainty" convention (per `AppForegroundTracker`'s A1 precedent), not an unreferenced debt marker, and is correctly routed to human verification rather than treated as a gap.

### Human Verification Required

### 1. Real silent sign-in + "Welcome back" popup on relaunch

**Test:** Once a Play Console PGS project exists with OAuth credentials for both the debug and release-keystore SHA-1s and a tester account (D-04 checklist): install a debug build on an emulator with Google Play services + Play Games, accept the Home opt-in prompt, sign in, kill the process, relaunch.
**Expected:** The stock Play Games "Welcome back" popup appears with no app-owned dialog, no wall, and no pause before a level can start (PGS-01).
**Why human:** No Play Console project exists yet (D-04) — `isAuthenticated()` answers `false` for every account today, so `PlayGamesService`'s live SDK behavior cannot be exercised end-to-end in this environment; only proven against MockK fakes of `GamesSignInClient`/`PlayersClient`.

### 2. `signIn()` cancel-vs-error taxonomy

**Test:** On the same emulator: tap the Settings sign-in row and dismiss the account chooser without picking an account (expect silence); tap it again and attempt sign-in with an account not registered as a tester (expect the snackbar).
**Expected:** A plain dismissal shows no snackbar and the row remains untouched; a genuine SDK error shows "Couldn't sign in to Play Games" (PGS-03/D-10).
**Why human:** Google does not document the `Task<AuthenticationResult>` failure shape for a plain user cancel versus a genuine error (RESEARCH Open Question 2). `PlayGamesService.signIn()`'s `CommonStatusCodes.CANCELED`-based classification is explicitly marked `[UNVERIFIED against a live Play Console project — D-04]` in its own KDoc pending this confirmation.

### 3. Interstitial foreground-cadence interaction

**Test:** With Phase 10 interstitial foreground-tracking logging enabled, trigger the Play Games account chooser (via the Home prompt or the Settings row) and watch for spurious foreground/background transitions.
**Expected:** No spurious transition is logged; the chooser/welcome popup do not trip `AppForegroundTracker`'s interstitial cadence.
**Why human:** Requires a live SDK Activity in-process; not exercisable through a JVM/Robolectric test double.

### Gaps Summary

No gaps. All 15 derived observable truths (merging the ROADMAP Success Criteria and all four plans' `must_haves.truths`) are verified directly against the codebase: the opt-in gate, the D-10 throw/false contract, D-17 persistence, D-14's degraded-name handling, the Home prompt's sequencing/dismissal/accept/decline behavior, the real `PlayGamesService` implementation and its live wiring into `PlatformModule`/`MainActivity`, and the three-state Settings section are all present, substantive (no stubs), and wired end to end, with 94 phase-specific unit/Compose tests re-run and green in this verification pass (not merely claimed by SUMMARY.md). Status is `human_needed` rather than `passed` solely because three behaviors genuinely require a live Play Console project that does not yet exist (D-04, explicitly tracked as pre-release operator debt in `11-VALIDATION.md`, `ROADMAP.md`'s External Prerequisites, and both 11-02/11-04 SUMMARY.md "User Setup Required" sections) — this is expected, documented deferral, not an implementation gap.

---

*Verified: 2026-09-03T22:16:47Z*
*Verifier: Claude (gsd-verifier)*
