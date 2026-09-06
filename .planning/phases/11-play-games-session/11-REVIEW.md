---
phase: 11-play-games-session
reviewed: 2026-09-04T01:30:00Z
depth: standard
files_reviewed: 35
files_reviewed_list:
  - app/build.gradle.kts
  - app/src/main/AndroidManifest.xml
  - app/src/main/java/com/app/pegsolitaire/MainActivity.kt
  - app/src/main/java/com/app/pegsolitaire/data/PlayGamesOptInRepository.kt
  - app/src/main/java/com/app/pegsolitaire/data/PlayGamesPreferences.kt
  - app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt
  - app/src/main/java/com/app/pegsolitaire/platform/games/GamesConstants.kt
  - app/src/main/java/com/app/pegsolitaire/platform/games/GamesService.kt
  - app/src/main/java/com/app/pegsolitaire/platform/games/GamesSignInState.kt
  - app/src/main/java/com/app/pegsolitaire/platform/games/NoOpGamesService.kt
  - app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt
  - app/src/main/java/com/app/pegsolitaire/ui/home/HomeRoute.kt
  - app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt
  - app/src/main/java/com/app/pegsolitaire/ui/home/HomeUiState.kt
  - app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt
  - app/src/main/java/com/app/pegsolitaire/ui/home/PlayGamesPromptDialog.kt
  - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsRoute.kt
  - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt
  - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsUiState.kt
  - app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsViewModel.kt
  - app/src/main/res/values/strings.xml
  - app/src/test/java/com/app/pegsolitaire/data/FakePlayGamesOptInRepository.kt
  - app/src/test/java/com/app/pegsolitaire/data/PlayGamesOptInRepositoryTest.kt
  - app/src/test/java/com/app/pegsolitaire/platform/games/FakeGamesService.kt
  - app/src/test/java/com/app/pegsolitaire/platform/games/NoOpGamesServiceTest.kt
  - app/src/test/java/com/app/pegsolitaire/platform/games/PlayGamesServiceTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/ButtonSoundTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/home/HomeScreenTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/home/PlayGamesPromptDialogTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsPlayGamesTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsPrivacyTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelSoundTest.kt
  - app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelTest.kt
  - gradle/libs.versions.toml
findings:
  critical: 1
  warning: 8
  info: 3
  total: 12
status: issues_found
---

# Phase 11: Code Review Report

**Reviewed:** 2026-09-04T01:30:00Z
**Depth:** standard
**Files Reviewed:** 35
**Status:** issues_found

## Summary

Phase 11 binds the real Play Games v2 `GamesService` (`PlayGamesService`), adds the opt-in
repository over the shared `app_prefs` DataStore, the one-time Home prompt, the Settings
Play Games section, and the `bootstrapIfOptedIn()` launch hook. The opt-in gate itself is
correctly positioned (the DataStore read precedes the Activity await, `PlayGamesSdk.initialize`
and every client getter) and is pinned negatively by `PlayGamesServiceTest`. The seam stays
SDK-free, the manifest `APP_ID` meta-data references the generated resource, strings and dimens
are all resourced, and the D-09 row copy is verbatim.

The defects are concentrated in **event lifecycle and concurrency**, not in the gate:

1. **The D-18 error snackbar on Home is not a one-shot event.** `HomeViewModel` lives for the
   whole process (Home is the start destination), the tick never resets, and
   `LaunchedEffect(errorTick)` re-runs every time `HomeScreen` re-enters composition. After one
   genuine sign-in failure, every return to Home from Settings/Catalog/Gameplay/Results, and every
   rotation, re-shows "Couldn't sign in to Play Games". This is the phantom-cue bug the effect's own
   KDoc says it guards against. (CR-01)
2. `PlayGamesService` has two unserialized writers to `_session` (`bootstrapIfOptedIn` and
   `signIn`), reuses a possibly-destroyed `Activity` after the chooser round trip, and its
   `runCatching` blocks swallow `CancellationException`. (WR-01, WR-02, WR-04)
3. Both ViewModels launch DataStore writes unguarded in `viewModelScope`; an `IOException` from
   `dataStore.edit` crashes the app on "Sign in"/"Not now"/the Settings row. (WR-05)
4. The Settings row has no in-flight guard (double-tap spawns two SDK sign-ins), and the
   signed-in "non-tappable" row is still clickable/focusable for accessibility. (WR-03, WR-06)

No security vulnerabilities, hardcoded secrets, or logging of player data were found. No
`console`/debug artifacts. Performance is out of scope for this review.

## Critical Issues

### CR-01: Home sign-in error snackbar replays on every re-entry to Home and on every rotation

**File:** `app/src/main/java/com/app/pegsolitaire/ui/home/HomeScreen.kt:203-210`
**Also:** `app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt:206-211`, `app/src/main/java/com/app/pegsolitaire/ui/home/HomeUiState.kt:54`, `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt:311-318`

**Issue:** `playGamesSignInErrorTick` is a monotonic counter that is only ever incremented, and the
effect is `LaunchedEffect(errorTick) { if (errorTick > 0) showSnackbar(...) }`. `LaunchedEffect`
runs its block every time it *enters composition* with a given key, not only when the key changes.
`HomeViewModel` is obtained via `hiltViewModel()` on the Home back-stack entry, and Home is the
`NavHost` start destination, so the ViewModel — and the tick — survive for the life of the process.
`HomeScreen` itself leaves composition whenever the user navigates forward (Settings, Catalog,
Gameplay, Results) and re-enters on pop, and is fully recomposed on a configuration change.

Concrete reproduction: accept the prompt, SDK reports a genuine error → tick = 1, snackbar shows
(correct). Open Settings, press back → `HomeScreen` re-enters composition → `LaunchedEffect(1)`
launches again → snackbar shows again. Rotate the device → shows again. This repeats for the rest of
the process on every visit to Home, telling the player sign-in failed for a sign-in they did not
just attempt — exactly the T-09-40 phantom cue the effect's KDoc says the `> 0` guard prevents (it
only prevents the tick-0 case).

The same shape on Settings (`SettingsScreen.kt:311-318`) is less exposed because
`SettingsViewModel` is cleared when the destination is popped, but it still replays on a rotation
while Settings is open after an error. `HomeScreenTest.aSignInErrorTickRaisedWhileOnHomeShowsTheSnackbar`
drives a 0→1 transition under a single composition and cannot catch this.

**Fix:** Make the event consumable. The smallest change that keeps the tick idiom is to have the
screen report the tick it has shown and the ViewModel clear it:

```kotlin
// HomeViewModel
fun onPlayGamesSignInErrorShown() {
    _uiState.update { it.copy(playGamesSignInErrorTick = 0) }
}

// HomeScreen (and SettingsScreen) — pass onErrorShown from the Route
@Composable
private fun PlayGamesSignInErrorSnackbarEffect(
    errorTick: Int,
    hostState: SnackbarHostState,
    onErrorShown: () -> Unit,
) {
    val message = stringResource(R.string.play_games_signin_error)
    LaunchedEffect(errorTick) {
        if (errorTick > 0) {
            onErrorShown()          // consume before suspending so a re-entry mid-show does not replay
            hostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }
}
```

Alternatively keep the counter and remember the last tick actually shown across re-entries and
config changes with `rememberSaveable { mutableIntStateOf(0) }`, showing only when
`errorTick > lastShown`. Either way, add a screen test that composes Home with tick = 1 already set
(the "returned to Home after an error" state) and asserts the snackbar does **not** appear.

## Warnings

### WR-01: `signIn()` reuses the pre-await Activity for the post-chooser player lookups

**File:** `app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt:180-192, 221-258`

**Issue:** `activity` is captured before `signInClient.signIn().await()`. That await spans the
SDK's account chooser, which is a separate Activity on top of `MainActivity`; a rotation or any
configuration change during the chooser destroys the captured `MainActivity` instance. After the
await returns `isAuthenticated == true`, `signedInSessionFor(activity)` calls
`PlayGames.getPlayersClient(activity)` twice on a destroyed Activity. Both calls are wrapped in
`runCatching`, so the failure is silent — but the consequence is that a *successful* sign-in is
reported as `false` / `SignedOut` (ID lookup fails → `SignedOut`), or as `SignedIn(playerId, null)`
(only the name lookup fails). The player is actually signed in, the Settings row keeps offering
sign-in until the next launch, and the caller has no way to distinguish this from a decline.
`ForegroundActivityProvider`'s own KDoc anticipates exactly this ("capture before show") for the
show step only; the post-await step needs a fresh instance.

**Fix:** Re-resolve the foreground Activity after the round trip, falling back to the captured one:

```kotlin
val authenticated = runCatching { ... signInClient.signIn().await().isAuthenticated }
...
onSuccess = { authenticated ->
    val hostActivity = activityProvider.current ?: activity
    val resolved = if (authenticated) signedInSessionFor(hostActivity) else GamesSignInState.SignedOut
    ...
}
```

Do the same in `bootstrapIfOptedIn` before `signedInSessionFor(activity)` (line 138), since the
`isAuthenticated` Task can also outlive a rotation. Add a test where `activityProvider.current`
returns a second mock after the await and assert the second mock is what `getPlayersClient` receives.

### WR-02: `bootstrapIfOptedIn` and `signIn` are unserialized concurrent writers to `_session`, and `signIn` never sets `bootstrapped`

**File:** `app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt:109-140, 177-205`

**Issue:** Both paths call `PlayGamesSdk.initialize`, build clients, and assign `_session.value`
with last-writer-wins semantics and no mutual exclusion. The `bootstrapped` guard only covers
repeated *bootstrap* calls and is never set by a successful `signIn()`. The reachable interleaving:

1. Player accepts the Home prompt (first-ever opt-in): `setOptedIn()` completes, then `signIn()`
   opens the chooser. `MainActivity` is paused.
2. Player rotates while the chooser is up. `MainActivity.onCreate` → `bootstrapIfOptedIn()` →
   `optedIn.first()` is now `true` → `bootstrapped` CAS succeeds → waits for a foreground Activity.
3. Both coroutines now run to completion independently. If the chooser takes longer than
   `ACTIVITY_WAIT_MS` (10 s), bootstrap writes `SignedOut` at t = 10 s; if `signIn()` had already
   published `SignedIn` (chooser finished faster than the resumed Activity was observed, or the
   isAuthenticated Task is slow/flaky and returns `false`), the bootstrap write regresses a signed-in
   session to `SignedOut`.

Even without the regression, every first-opt-in session pays a redundant `initialize` +
`isAuthenticated` + two player lookups on the first rotation after signing in — and PGS may replay
its "Welcome back" popup — because `signIn()` leaves `bootstrapped == false`. The `bootstrapped`
KDoc claims this exact cost is what the guard prevents.

**Fix:** Serialize the SDK-touching sections and let a successful `signIn` satisfy the once-per-process
contract:

```kotlin
private val sdkMutex = Mutex()

override fun bootstrapIfOptedIn() {
    scope.launch(Dispatchers.Main) {
        if (!optInRepository.optedIn.first()) { _session.value = GamesSignInState.SignedOut; return@launch }
        if (!bootstrapped.compareAndSet(false, true)) return@launch
        sdkMutex.withLock {
            if (_session.value is GamesSignInState.SignedIn) return@withLock   // signIn already answered
            ...existing body...
        }
    }
}

override suspend fun signIn(): Boolean = sdkMutex.withLock {
    ...existing body...
    // on success:
    bootstrapped.set(true)
}
```

Add a test that runs `signIn()` and `bootstrapIfOptedIn()` concurrently under the test scheduler with
the bootstrap `isAuthenticated` answering `false` and asserts the final session is `SignedIn`.

### WR-03: Settings sign-in row has no in-flight guard — a double tap launches two concurrent SDK sign-ins

**File:** `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsViewModel.kt:175-185`
**Also:** `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt:269-276`

**Issue:** The row stays rendered and enabled while `signIn()` is suspended (the session is still
`SignedOut` until the round trip resolves), and `onSignInToPlayGames()` unconditionally launches a
new coroutine. Two quick taps (or a tap plus the squish animation replay) produce two overlapping
`signInClient.signIn()` calls — two `PlayGamesSdk.initialize` calls, potentially two account
choosers queued, two writes to `_session`, and, on failure, two error snackbars queued. The Home
path is protected only because the dialog is hidden synchronously; Settings has no equivalent.

**Fix:** Track the in-flight job in the ViewModel and ignore re-entry:

```kotlin
private var signInJob: Job? = null

fun onSignInToPlayGames() {
    if (signInJob?.isActive == true) return
    signInJob = viewModelScope.launch {
        optInRepository.setOptedIn()
        runCatching { gamesService.signIn() }...
    }
}
```

Optionally surface `signInInFlight` in `SettingsUiState` so the row can render disabled. Add a
ViewModel test: two `onSignInToPlayGames()` calls before `runCurrent()` → `signInCallCount == 1`.

### WR-04: `runCatching` around `signIn()` swallows `CancellationException` and acts on it

**File:** `app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt:183-204`
**Also:** `app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt:206-211`, `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsViewModel.kt:180-185`

**Issue:** `runCatching` catches `Throwable`, including `CancellationException`. In
`PlayGamesService.signIn()` a cancelled `await()` lands in `onFailure`, which first writes
`_session.value = SignedOut` and only then rethrows — so cancellation mutates shared singleton state.
In both ViewModels a cancelled `signIn()` (e.g. `SettingsViewModel` cleared by popping Settings
while the chooser is open) lands in `.onFailure { tick + 1 }`, incrementing the error tick as if the
SDK had failed. Today the observable damage is small (the Settings VM is being cleared; the Home VM
is process-lifetime and rarely cancelled), but the pattern converts a structured-concurrency cancel
into "an SDK error" and is the well-known `runCatching`-in-coroutines anti-pattern.

**Fix:** Rethrow cancellation before classifying, in all three places:

```kotlin
.onFailure { failure ->
    if (failure is CancellationException) throw failure
    ...
}
```

Or replace `runCatching` with `try { ... } catch (e: CancellationException) { throw e } catch (e: Exception) { ... }`.

### WR-05: DataStore write failures in the opt-in handlers are uncaught and crash the app

**File:** `app/src/main/java/com/app/pegsolitaire/data/PlayGamesOptInRepository.kt:87-105`
**Also:** `app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt:205, 225`, `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsViewModel.kt:179`

**Issue:** The repository's reads fail open (`catch { emit(emptyPreferences()) }`) and its KDoc
promises a damaged store "can never … crash a launch", but `setNotNow()` / `setOptedIn()` call
`dataStore.edit` unguarded, and `DataStore.edit` throws `IOException` on a write failure (disk
full, corrupt file, I/O error). All three call sites are `viewModelScope.launch { optInRepository.set…() … }`
with no handler, so the exception propagates to the scope's default handler and terminates the
process. The player taps "Not now", "Sign in", or the Settings row and the app crashes. Worse, on
the accept path the crash happens *before* `signIn()`, with the prompt already hidden, so the next
launch re-asks and the same tap crashes again while the disk condition persists.
`OnboardingRepository.markSeen()` has the same exposure, but this phase adds three new taps to it
and explicitly documents a no-crash contract it does not meet.

**Fix:** Apply the fail-open policy to writes as well, in the repository so every caller inherits it:

```kotlin
open suspend fun setOptedIn() {
    runCatching {
        dataStore.edit {
            it[PlayGamesPreferences.PROMPT_ANSWERED_KEY] = true
            it[PlayGamesPreferences.OPTED_IN_KEY] = true
        }
    }.onFailure { if (it is CancellationException) throw it }   // IOException degrades to "not persisted"
}
```

Add a repository test with a `DataStore` whose `updateData` throws `IOException`, asserting neither
setter throws. Consider the same treatment for `OnboardingRepository.markSeen()`.

### WR-06: The signed-in "non-tappable" row is still clickable, focusable and squishes on press

**File:** `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt:297-307, 538-545`

**Issue:** D-06 requires a non-tappable row, and the implementation passes `onClick = {}` to
`SettingsLinkRow`, whose modifier chain unconditionally applies `.claySquish().clickable(onClick)`.
The result is a card that TalkBack announces as actionable ("double-tap to activate"), that
participates in keyboard/D-pad focus traversal, and that plays the squish animation when tapped —
with nothing happening. That is an accessibility defect (an actionable control with no action), and
it contradicts the KDoc on the section ("its `onClick` is empty" is presented as if it made the row
inert). `SettingsPlayGamesTest.theSignedInRowIsNotAnOfferToSignInAgain` only proves the handler is
not called; it does not prove the node is not clickable.

**Fix:** Make the action optional and gate the interaction modifiers on it:

```kotlin
private fun SettingsLinkRow(
    ...,
    onClick: (() -> Unit)?,
) {
    Card(
        modifier = Modifier
            .clayEntrance(entranceOrder)
            .clayShadow(...)
            .fillMaxWidth()
            .heightIn(min = ...)
            .then(if (onClick != null) Modifier.claySquish().clickable(onClick = onClick) else Modifier),
    ) { ... }
}
```

Pass `onClick = null` for the signed-in row and assert `assertHasNoClickAction()` in the test.

### WR-07: Home prompt visibility can transiently re-raise `true` after the synchronous hide

**File:** `app/src/main/java/com/app/pegsolitaire/ui/home/HomeViewModel.kt:97-105`

**Issue:** `playGamesPromptVisible` is driven by `combine(onboardingRepository.onboardingSeen,
optInRepository.promptAnswered)`. Both upstreams are `map`s over the *same* `dataStore.data` flow
(`app_prefs` is also written by `ChallengeProgress`, `InterstitialCadenceStore`, `SettingsRepository`
and `OnboardingRepository`), and neither is `distinctUntilChanged()`, so every store write re-emits
both. After `onAcceptPlayGamesPrompt()` / `onDeclinePlayGamesPrompt()` hide the dialog
synchronously and launch the write, the write's `data` emission is delivered to `combine`'s two
collectors in unspecified order; when `onboardingSeen` (still `true`) arrives before the new
`promptAnswered` (`true`), `combine` emits `true && !false == true` and the collector sets
`playGamesPromptVisible = true` again, followed by `false` a moment later. `StateFlow` conflation
usually hides this from Compose, but it is not guaranteed across a frame boundary, and a `Dialog`
window flashing open/closed is highly visible when it does happen. It is observable today with
Turbine on `uiState`.

**Fix:** Collapse to one source or dedupe each upstream:

```kotlin
combine(
    onboardingRepository.onboardingSeen.distinctUntilChanged(),
    optInRepository.promptAnswered.distinctUntilChanged(),
) { seen, answered -> seen && !answered }
    .distinctUntilChanged()
    .collect { ... }
```

Better still, expose a single `promptPending: Flow<Boolean>` from the repository built from one
`dataStore.data.map { seen && !answered }`, so the two keys are read atomically from the same
`Preferences` snapshot.

### WR-08: Cancel classification recognises only `CommonStatusCodes.CANCELED`; the sign-in-specific cancel code is treated as an error

**File:** `app/src/main/java/com/app/pegsolitaire/platform/games/PlayGamesService.kt:196-203`

**Issue:** A failed `Task` is swallowed only when `(failure as? ApiException)?.statusCode ==
CommonStatusCodes.CANCELED` (16). Google's sign-in flows report a user-dismissed chooser as
`GoogleSignInStatusCodes.SIGN_IN_CANCELLED` (12501), and the underlying `Status` may also carry
`CommonStatusCodes.CANCELED` only for the generic PendingIntent cancel. With the current check a
player who backs out of the account chooser gets the "Couldn't sign in to Play Games" snackbar —
which the KDoc on this very method names as the worse of the two misclassifications (D-10). The
KDoc marks the taxonomy as unverified (D-04), which is honest, but the code currently biases toward
over-alerting for the most common cancel code.

**Fix:** Treat the sign-in cancel family as a decline, without a magic number:

```kotlin
// GamesConstants
/** Status codes a Task<AuthenticationResult> reports for a player-initiated cancel (D-10). */
val CANCEL_STATUS_CODES = setOf(CommonStatusCodes.CANCELED, GoogleSignInStatusCodes.SIGN_IN_CANCELLED)

// PlayGamesService
if ((failure as? ApiException)?.statusCode in GamesConstants.CANCEL_STATUS_CODES) false else throw failure
```

If `GoogleSignInStatusCodes` (from `play-services-auth`) is not on the graph, declare the value as a
named constant with the source cited, and add the D-04 emulator check for both codes to the
pre-release checklist. Add a test case mirroring `signInSwallowsAPlainCancellationRatherThanRaisingAnError`
for the second code.

## Info

### IN-01: Test doubles for the opt-in repository and the no-op DataStore are duplicated three times

**File:** `app/src/test/java/com/app/pegsolitaire/platform/games/PlayGamesServiceTest.kt:320-339`
**Also:** `app/src/test/java/com/app/pegsolitaire/data/FakePlayGamesOptInRepository.kt:60-66`, `app/src/test/java/com/app/pegsolitaire/ui/home/HomeViewModelTest.kt:637-642`

**Issue:** `SettableOptInRepository` re-implements what `FakePlayGamesOptInRepository(initiallyOptedIn = …)`
already provides, and `NoopPreferencesDataStore` is declared as a `private object` in three files.
`FakePlayGamesOptInRepository`'s own KDoc argues for exactly one shared double so the two-flag
contract cannot drift.

**Fix:** Delete `SettableOptInRepository` and construct `FakePlayGamesOptInRepository(initiallyOptedIn = optedIn)`
in `serviceFor`; move `NoopPreferencesDataStore` to a single `internal object` in the test `data`
package and import it from the other two files.

### IN-02: No tests cover the concurrency and lifecycle paths this review flags

**File:** `app/src/test/java/com/app/pegsolitaire/platform/games/PlayGamesServiceTest.kt`, `app/src/test/java/com/app/pegsolitaire/ui/home/HomeScreenTest.kt`, `app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelTest.kt`

**Issue:** The suite pins the opt-in gate and the decline-vs-error split well, but has no case for:
`signIn()` and `bootstrapIfOptedIn()` overlapping (WR-02); the Activity being replaced mid-await
(WR-01); a cancelled `signIn()` (WR-04); a second tap while a sign-in is in flight (WR-03); a screen
composed with an already-raised error tick (CR-01); or a throwing `DataStore.updateData` (WR-05).

**Fix:** Add the tests named under each finding above; they are all expressible with the existing
`StandardTestDispatcher` + MockK + `createComposeRule` harnesses.

### IN-03: Debug builds bind the real `PlayGamesService` against project ID `"0"`

**File:** `app/src/main/AndroidManifest.xml:70-72`
**Also:** `app/build.gradle.kts:198`, `app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt:112-114`

**Issue:** `TEST_GAMES_PROJECT_ID` is `"0"`, so on every debug device an opted-in player's silent
check and every manual sign-in will resolve `SignedOut` or throw (→ error snackbar). This is
consistent with D-04 (no Console project yet) and the `PlatformModule` KDoc's reasoning, but it means
the debug experience of the new Settings row is "always fails with a snackbar" until credentials
exist. Worth a line in the pre-release checklist so the first real-credential run is not mistaken
for a regression.

**Fix:** No code change required; record in the D-04 operator checklist that debug sign-in is
expected to fail until `local.properties` carries the real PGS project ID and OAuth clients for the
debug SHA-1.

---

_Reviewed: 2026-09-04T01:30:00Z_
_Reviewer: Claude (gsd-code-reviewer)_
_Depth: standard_
