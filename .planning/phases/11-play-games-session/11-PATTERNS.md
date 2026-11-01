# Phase 11: Play Games Session - Pattern Map

**Mapped:** 2026-09-03
**Files analyzed:** 21
**Analogs found:** 21 / 21

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|--------------------|------|-----------|-----------------|----------------|
| `platform/games/PlayGamesService.kt` | service (platform seam impl) | event-driven (StateFlow session) + request-response (signIn) | `platform/ads/consent/UmpConsentGateway.kt` (bootstrap/lazy-SDK-handle shape) + `platform/ads/AdMobAdGateway.kt` (Task/callback→suspend bridging) | exact (composite) |
| `platform/games/GamesSignInState.kt` (MODIFIED) | model (sealed state) | — | itself (extend in place) | exact |
| `platform/games/GamesService.kt` (MODIFIED — add `bootstrapIfOptedIn()`) | service interface | — | itself + `platform/ads/consent/ConsentGateway.kt` (`bootstrap()` shape) | exact |
| `platform/games/NoOpGamesService.kt` | service (stub) | — | UNCHANGED — reference only | exact (no change) |
| `di/PlatformModule.kt` (MODIFIED — binding swap) | config (Hilt module) | — | itself (`bindAdGateway` line already did this swap) | exact |
| `MainActivity.kt` (MODIFIED — one bootstrap line) | controller (Activity) | — | itself (`consentGateway.bootstrap()` call site) | exact |
| `AndroidManifest.xml` (MODIFIED — APP_ID meta-data) | config | — | itself (`com.google.android.gms.ads.APPLICATION_ID` meta-data block) | exact |
| `data/PlayGamesPreferences.kt` + `data/PlayGamesOptInRepository.kt` (NEW) | repository (DataStore) | CRUD (read/write 2 booleans) | `data/OnboardingRepository.kt` | exact |
| `ui/home/HomeUiState.kt` (MODIFIED — + `playGamesPromptVisible`) | model (UI state) | — | itself (`onboardingVisible` field) | exact |
| `ui/home/HomeViewModel.kt` (MODIFIED — sequencing + handlers) | viewmodel | event-driven (StateFlow collect → UiState) | itself (`onboardingRepository.onboardingSeen` collector + `onShowHowToPlay`/`onDismissOnboarding`) | exact |
| `ui/home/HomeScreen.kt` (MODIFIED — dialog host + snackbar host) | component (Compose screen) | — | itself (`OnboardingOverlay` conditional host) + `ui/settings/SettingsScreen.kt` (`ClaySnackbarHost` wiring) | exact (composite) |
| `ui/home/HomeRoute.kt` (MODIFIED — forward new handlers) | route (Compose) | — | itself + `ui/settings/SettingsRoute.kt` | exact |
| `ui/home/PlayGamesPromptDialog.kt` (NEW) | component (Compose dialog) | — | `ui/game/AdOfferDialog.kt` | exact |
| `ui/settings/SettingsUiState.kt` (MODIFIED — + games sign-in field) | model (UI state) | — | itself (`privacyOptionsRequired` field) | exact |
| `ui/settings/SettingsViewModel.kt` (MODIFIED — collect session, `onSignInToPlayGames`) | viewmodel | event-driven | itself (`consentGateway.privacyOptionsRequired` collector + `onManagePrivacyChoices`) | exact |
| `ui/settings/SettingsScreen.kt` (MODIFIED — new Play Games section) | component (Compose screen) | — | itself (`PrivacySection` composable) | exact |
| `res/values/strings.xml`, `res/values/dimens.xml` (MODIFIED) | config (resources) | — | itself (`settings_section_privacy`/`ad_offer_*` string blocks; existing `spacing_*`/`elevation_*` dimens reused, no new dimens expected) | exact |
| `app/src/test/.../platform/games/PlayGamesServiceTest.kt` (NEW) | test | — | `app/src/test/.../platform/games/NoOpGamesServiceTest.kt` | role-match |
| `app/src/test/.../data/PlayGamesOptInRepositoryTest.kt` (NEW) | test | — | (no `OnboardingRepositoryTest.kt` found in repo — see No Analog Found) | partial |
| `app/src/test/.../ui/home/FakeGamesService.kt` or similar (NEW, test-only fake) | test double | — | `app/src/test/.../ui/settings/FakeConsentGateway.kt` | exact |
| `app/src/test/.../ui/home/HomeViewModelTest.kt` (EXTENDED) | test | — | itself | exact |
| `app/src/test/.../ui/settings/SettingsViewModelTest.kt` (EXTENDED) | test | — | itself | exact |
| `app/src/test/.../ui/home/PlayGamesPromptDialogTest.kt` / Compose section test (NEW) | test (Compose UI) | — | `app/src/test/.../ui/game/GameScreenAdOfferTest.kt` | exact |
| `gradle/libs.versions.toml` + `app/build.gradle.kts` (MODIFIED — new library entry) | config (build) | — | itself (`play-services-games-v2` entry + `implementation(libs.play.services.games.v2)`) | exact |

## Pattern Assignments

### `platform/games/PlayGamesService.kt` (NEW — the real seam implementation)

**Analogs:** `platform/ads/consent/UmpConsentGateway.kt` (class shape, `@Singleton`-on-class discipline, `bootstrap()`/fire-and-forget, lazy SDK handle, never-logs discipline) and `platform/ads/AdMobAdGateway.kt` (bridging an SDK call into a `StateFlow`-observed outcome, Activity resolution via `ForegroundActivityProvider`).

**Imports pattern** (from `UmpConsentGateway.kt` lines 1-28):
```kotlin
package com.app.pegsolitaire.platform.ads.consent

import android.content.Context
import com.app.pegsolitaire.BuildConfig
import com.app.pegsolitaire.di.ApplicationScope
import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
import com.app.pegsolitaire.platform.ads.AdConstants
import com.app.pegsolitaire.platform.ads.AdIds
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
```
For `PlayGamesService.kt`, the equivalent import block (per RESEARCH.md Pattern 2) swaps the UMP/GMA imports for:
```kotlin
import com.google.android.gms.games.GamesSignInClient
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import com.google.android.gms.games.PlayersClient
import kotlinx.coroutines.tasks.await   // kotlinx-coroutines-play-services 1.11.0
```

**`@Singleton`-on-class + lazy SDK handle pattern** (`UmpConsentGateway.kt` lines 58-88):
```kotlin
@Singleton
class UmpConsentGateway
    @Inject
    constructor(
        @param:ApplicationContext private val appContext: Context,
        private val activityProvider: ForegroundActivityProvider,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) : ConsentGateway {
        private val consentInformation: ConsentInformation by lazy {
            UserMessagingPlatform.getConsentInformation(appContext)
        }

        private val _readiness = MutableStateFlow(AdsReadiness.Unknown)
        override val readiness: StateFlow<AdsReadiness> = _readiness.asStateFlow()
```
Copy this exact shape for `PlayGamesService`: `@Singleton` on the class (required — `@Binds @Singleton` in `PlatformModule` scopes only the interface, per `DefaultForegroundActivityProvider`'s documented precedent), constructor-injects `ForegroundActivityProvider` + `@ApplicationScope CoroutineScope` + the new `PlayGamesOptInRepository`, and owns `private val _session = MutableStateFlow<GamesSignInState>(GamesSignInState.Unknown)` exposed as `override val session: StateFlow<GamesSignInState> = _session.asStateFlow()`. Any SDK client (`GamesSignInClient`, `PlayersClient`) is obtained per-call from `PlayGames.get*Client(activity)`, never stored as a field — there is no equivalent of `consentInformation`'s single `by lazy` handle because these clients are constructed from an `Activity` argument, unlike `ConsentInformation` which is context-scoped.

**`bootstrap()`-shaped fire-and-forget entry point** (`UmpConsentGateway.kt` lines 108-120, adapt for `bootstrapIfOptedIn()`):
```kotlin
override fun bootstrap() {
    scope.launch(Dispatchers.Main) {
        val activity =
            withTimeoutOrNull(AdConstants.ACTIVITY_WAIT_MS) {
                activityProvider.awaitActivity()
            } ?: return@launch
        // ... proceed only after the Activity resolves
    }
}
```
`bootstrapIfOptedIn()` copies this shape but adds the opt-in gate as the **outermost** check, before any Activity wait or SDK touch (Pitfall 1 in RESEARCH.md):
```kotlin
override fun bootstrapIfOptedIn() {
    scope.launch(Dispatchers.Main) {
        if (!optInRepository.optedIn.first()) return@launch // D-01: SDK never touched
        val activity = withTimeoutOrNull(GamesConstants.ACTIVITY_WAIT_MS) {
            activityProvider.awaitActivity()
        } ?: return@launch
        PlayGamesSdk.initialize(activity.applicationContext)
        val authenticated = runCatching {
            PlayGames.getGamesSignInClient(activity).isAuthenticated().await().isAuthenticated
        }.getOrDefault(false)
        _session.value =
            if (authenticated) resolveSignedInState(activity, playerId) else GamesSignInState.SignedOut
    }
}
```

**`Task<T>` → suspend bridging** (RESEARCH.md Pattern 2, `kotlinx-coroutines-play-services` 1.11.0):
```kotlin
import kotlinx.coroutines.tasks.await

private suspend fun checkAuthenticated(activity: Activity): Boolean {
    val client = PlayGames.getGamesSignInClient(activity)
    val result = client.isAuthenticated().await()
    return result.isAuthenticated
}
```

**D-07 display-name fetch, degrading on failure to `null` never to a sign-in failure** (RESEARCH.md Code Examples):
```kotlin
private suspend fun resolveSignedInState(activity: Activity, playerId: String): GamesSignInState.SignedIn {
    val displayName = runCatching {
        PlayGames.getPlayersClient(activity).getCurrentPlayer().await().displayName
    }.getOrNull() // D-14: never fails the sign-in, never keeps session at Unknown
    return GamesSignInState.SignedIn(playerId = playerId, displayName = displayName)
}
```

**Never-logs / fail-degrades discipline** — copy the documented convention from `UmpConsentGateway`'s KDoc (lines 51-58) verbatim in spirit: every failure (`isAuthenticated` throwing, `signIn` failing, name lookup stalling) degrades to `SignedOut` / `null`, nothing is logged (ASVS V7, matches RESEARCH.md Security Domain).

---

### `platform/games/GamesService.kt` (MODIFIED — add `bootstrapIfOptedIn()`)

**Analog:** `platform/ads/consent/ConsentGateway.kt` — the `bootstrap()` method's KDoc and signature shape (lines 43-58):
```kotlin
/**
 * Starts the consent sequence: refresh consent information, show any required form,
 * then initialize the ads SDK only if consent permits it (CONS-01/CONS-02).
 *
 * **Idempotent**, and called once per Activity creation ...
 * Non-suspending and fire-and-forget: the caller does not wait ...
 */
fun bootstrap()
```
Add to `GamesService` interface, non-suspend, fire-and-forget, exactly as RESEARCH.md Pattern 1 specifies:
```kotlin
/**
 * Fire-and-forget launch-time bootstrap (D-01). Reads the persisted opt-in flag;
 * if and only if the player previously opted in, runs PlayGamesSdk.initialize
 * plus the silent isAuthenticated check. A no-op for a player who has never
 * opted in — session stays SignedOut without the SDK ever being touched.
 */
fun bootstrapIfOptedIn()
```

---

### `platform/games/GamesSignInState.kt` (MODIFIED — `SignedIn` gains `displayName`)

**Analog:** itself, extend in place (current file at `app/src/main/java/com/app/pegsolitaire/platform/games/GamesSignInState.kt` lines 24-35):
```kotlin
sealed interface GamesSignInState {
    data object Unknown : GamesSignInState
    data object SignedOut : GamesSignInState
    data class SignedIn(
        val playerId: String,
    ) : GamesSignInState
}
```
Becomes:
```kotlin
data class SignedIn(
    val playerId: String,
    val displayName: String? = null,
) : GamesSignInState
```
Update the class KDoc to explain the D-14 null-fallback contract (see RESEARCH.md Code Examples for the exact worded KDoc).

---

### `di/PlatformModule.kt` (MODIFIED — binding swap)

**Analog:** itself; the `bindAdGateway` line already performed exactly this kind of swap (lines 81-91):
```kotlin
@Binds
@Singleton
abstract fun bindAdGateway(impl: AdMobAdGateway): AdGateway

@Binds
@Singleton
abstract fun bindGamesService(impl: NoOpGamesService): GamesService  // ← change to PlayGamesService
```
Change is a one-line parameter-type swap: `bindGamesService(impl: PlayGamesService): GamesService`. Update the import from `NoOpGamesService` to `PlayGamesService`. Per RESEARCH.md's Alternatives Considered / A3, bind unconditionally for every build type — no flavor split, matching how `AdMobAdGateway` was bound with zero per-build-type branching. Update the class KDoc paragraph that currently says "[GamesService] half is still pending; Phase 11 owns it" to reflect the swap landing.

---

### `MainActivity.kt` (MODIFIED — one bootstrap line)

**Analog:** itself; the existing `consentGateway.bootstrap()` call site (lines 54-71):
```kotlin
@Inject
lateinit var consentGateway: ConsentGateway

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    consentGateway.bootstrap()
    setContent { ... }
}
```
Add a second injected field and a second fire-and-forget call, immediately after `consentGateway.bootstrap()` (D-01):
```kotlin
@Inject
lateinit var gamesService: GamesService

override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    consentGateway.bootstrap()
    gamesService.bootstrapIfOptedIn() // D-01: no-op unless the player already opted in
    setContent { ... }
}
```
Keep the KDoc convention explaining *why* this lives in `MainActivity` and not `App` (Robolectric instantiates `App` for the whole JVM suite; bootstrapping an SDK there would drag it onto `./gradlew check --offline`'s execution path) — copy the reasoning already written for `consentGateway` verbatim in spirit for `gamesService`.

---

### `data/PlayGamesPreferences.kt` + `data/PlayGamesOptInRepository.kt` (NEW)

**Analog:** `data/OnboardingRepository.kt` (full file, 56 lines) — the closest possible precedent: one boolean-backed DataStore repository over the shared `app_prefs` store, fail-open reads, single `@Inject internal constructor(DataStore<Preferences>)`.

**Full pattern to copy** (`data/OnboardingRepository.kt` lines 1-56):
```kotlin
package com.app.pegsolitaire.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

internal object OnboardingPreferences {
    val SEEN_KEY = booleanPreferencesKey("onboarding_seen")
    const val DEFAULT_SEEN = false
}

@Singleton
open class OnboardingRepository
    @Inject
    internal constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        open val onboardingSeen: Flow<Boolean> =
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { it[OnboardingPreferences.SEEN_KEY] ?: OnboardingPreferences.DEFAULT_SEEN }

        open suspend fun markSeen() {
            dataStore.edit { it[OnboardingPreferences.SEEN_KEY] = true }
        }
    }
```

**Adapted for D-17's two keys** (per RESEARCH.md Code Examples — `PlayGamesPreferences` constants object):
```kotlin
internal object PlayGamesPreferences {
    val PROMPT_ANSWERED_KEY = booleanPreferencesKey("play_games_prompt_answered")
    const val DEFAULT_PROMPT_ANSWERED = false

    val OPTED_IN_KEY = booleanPreferencesKey("play_games_opted_in")
    const val DEFAULT_OPTED_IN = false
}

@Singleton
class PlayGamesOptInRepository
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
    ) {
        val promptAnswered: Flow<Boolean> =
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { it[PlayGamesPreferences.PROMPT_ANSWERED_KEY] ?: PlayGamesPreferences.DEFAULT_PROMPT_ANSWERED }

        val optedIn: Flow<Boolean> =
            dataStore.data
                .catch { emit(emptyPreferences()) }
                .map { it[PlayGamesPreferences.OPTED_IN_KEY] ?: PlayGamesPreferences.DEFAULT_OPTED_IN }

        suspend fun setNotNow() {
            dataStore.edit {
                it[PlayGamesPreferences.PROMPT_ANSWERED_KEY] = true
                it[PlayGamesPreferences.OPTED_IN_KEY] = false
            }
        }

        suspend fun setOptedIn() {
            dataStore.edit {
                it[PlayGamesPreferences.PROMPT_ANSWERED_KEY] = true
                it[PlayGamesPreferences.OPTED_IN_KEY] = true
            }
        }
    }
```
Note: `SettingsRepositoryImpl` (`data/SettingsRepository.kt` lines 100-116) shows the `storageFailSafe(fallback = Unit) { dataStore.edit { ... } }` variant for writes that must never crash the caller on a disk-full/IO failure — consider that wrapper for `setOptedIn`/`setNotNow` if planner wants write-side fail-safety to match `SettingsRepositoryImpl` rather than `OnboardingRepository` (which does not wrap its `markSeen()` write). Either precedent exists in-repo; `OnboardingRepository`'s bare `dataStore.edit` is the closer analog since it is the file D-17 explicitly names as the model.

**DataStore constant reuse** — `data/AppPreferences.kt` is unchanged; both keys go into the same shared `app_prefs` store (`AppPreferences.STORE_NAME`), same as `OnboardingRepository` and `SettingsRepositoryImpl`.

---

### `ui/home/HomeUiState.kt` (MODIFIED — `+ playGamesPromptVisible: Boolean`)

**Analog:** itself, the `onboardingVisible` field (lines 34-40):
```kotlin
@Immutable
data class HomeUiState(
    val onboardingVisible: Boolean = false,
    val challengeCurrentLevel: Int = ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL,
    val classicResumeLevelId: String? = null,
    val challengeResumeLevel: Int? = null,
)
```
Add `val playGamesPromptVisible: Boolean = false,` as a peer field, with a KDoc `@property` entry following the existing style (D-15: shown once, only after `onboardingVisible == false`).

---

### `ui/home/HomeViewModel.kt` (MODIFIED — sequencing + accept/decline handlers)

**Analog:** itself, the onboarding collector + `onShowHowToPlay`/`onDismissOnboarding` pair (lines 67-74, 141-149):
```kotlin
init {
    viewModelScope.launch {
        onboardingRepository.onboardingSeen.collect { seen ->
            _uiState.update { it.copy(onboardingVisible = !seen) }
        }
    }
}

fun onShowHowToPlay() {
    _uiState.update { it.copy(onboardingVisible = true) }
}

fun onDismissOnboarding() {
    viewModelScope.launch { onboardingRepository.markSeen() }
    _uiState.update { it.copy(onboardingVisible = false) }
}
```
For D-15's sequencing rule (prompt only after onboarding dismissed AND not yet answered), inject `PlayGamesOptInRepository` and `GamesService`, and combine the two flows (`onboardingSeen` + `promptAnswered`) — copy the `combine`-into-`StateFlow` idiom from `AdMobAdGateway.kt`'s `interstitialReady` (lines 160-164):
```kotlin
override val interstitialReady: StateFlow<Boolean> =
    combine(consentGateway.readiness, interstitialSlot.ready) { readiness, held ->
        readiness == AdsReadiness.Ready && held
    }.stateIn(scope, SharingStarted.Eagerly, false)
```
applied as (illustrative, ViewModel-local `viewModelScope.launch { combine(...).collect { ... } }` rather than `stateIn`, matching the existing `HomeViewModel` init-block collector style):
```kotlin
viewModelScope.launch {
    combine(onboardingRepository.onboardingSeen, optInRepository.promptAnswered) { seen, answered ->
        seen && !answered
    }.collect { shouldShow ->
        _uiState.update { it.copy(playGamesPromptVisible = shouldShow) }
    }
}

fun onAcceptPlayGamesPrompt() {
    viewModelScope.launch {
        optInRepository.setOptedIn()
        val signedIn = gamesService.signIn()
        if (!signedIn) _snackbarEvent.emit(...) // D-18 error snackbar via ClaySnackbarHost
    }
    _uiState.update { it.copy(playGamesPromptVisible = false) }
}

fun onDeclinePlayGamesPrompt() {
    viewModelScope.launch { optInRepository.setNotNow() }
    _uiState.update { it.copy(playGamesPromptVisible = false) }
}
```
Home currently has no snackbar/one-shot-event channel — `SettingsScreen`'s `SnackbarHostState` + `rememberCoroutineScope().launch { snackbarHostState.showSnackbar(...) }` pattern (see `SettingsScreen.kt` below) is the model for wiring one in; alternatively a `SharedFlow<Unit>`/event field on `HomeUiState` if the planner prefers ViewModel-driven emission — Claude's Discretion per CONTEXT.md.

---

### `ui/home/HomeScreen.kt` (MODIFIED — dialog host + snackbar host)

**Analog A — conditional dialog host**, itself (lines 134-136):
```kotlin
if (state.onboardingVisible) {
    OnboardingOverlay(onDismiss = onDismissOnboarding)
}
```
Becomes a second, independent conditional host beneath it:
```kotlin
if (state.playGamesPromptVisible) {
    PlayGamesPromptDialog(onAccept = onAcceptPlayGamesPrompt, onDecline = onDeclinePlayGamesPrompt)
}
```

**Analog B — `Scaffold` + `ClaySnackbarHost` wiring**, `ui/settings/SettingsScreen.kt` lines 96-102:
```kotlin
val snackbarHostState = remember { SnackbarHostState() }

Scaffold(
    modifier = modifier.fillMaxSize(),
    snackbarHost = { ClaySnackbarHost(snackbarHostState) },
    topBar = { ... },
) { contentPadding -> ... }
```
`HomeScreen`'s current `Scaffold` (lines 107-129) has no `snackbarHost` parameter — add one identically, using the D-18 error string, triggered from the same collected one-shot event the ViewModel raises on a failed accept.

---

### `ui/home/HomeRoute.kt` (MODIFIED — forward new handlers)

**Analog:** itself (lines 41-51) — the existing `onShowHowToPlay = viewModel::onShowHowToPlay` forwarding idiom:
```kotlin
HomeScreen(
    state = state,
    onPlayClassic = onPlayClassic,
    ...
    onShowHowToPlay = viewModel::onShowHowToPlay,
    onDismissOnboarding = viewModel::onDismissOnboarding,
    onOpenSettings = onOpenSettings,
    modifier = modifier,
)
```
Add `onAcceptPlayGamesPrompt = viewModel::onAcceptPlayGamesPrompt` and `onDeclinePlayGamesPrompt = viewModel::onDeclinePlayGamesPrompt` as two more method-reference parameters, same idiom.

---

### `ui/home/PlayGamesPromptDialog.kt` (NEW)

**Analog:** `ui/game/AdOfferDialog.kt` (full file, 278 lines) — named directly as the model by D-15/D-16 and RESEARCH.md Pattern 4.

**Dialog shell + properties pattern** (`AdOfferDialog.kt` lines 90-135):
```kotlin
@Composable
fun AdOfferDialog(
    reason: AdReason,
    loading: Boolean,
    onAccept: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = /* loading-gated DialogProperties, or plain usePlatformDefaultWidth=false */
    ) {
        val surfaceShape = MaterialTheme.shapes.extraLarge
        Surface(
            shape = surfaceShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier =
                modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.spacing_lg))
                    .widthIn(max = dimensionResource(R.dimen.content_max_width))
                    .clayShadow(dimensionResource(R.dimen.elevation_dialog), surfaceShape)
                    .testTag(AD_OFFER_DIALOG_TEST_TAG),
        ) {
            AdOfferContent(...)
        }
    }
}
```
`PlayGamesPromptDialog` has no `loading` state (D-18 is fire-and-forget, no spinner lock) — use the simple `DialogProperties(usePlatformDefaultWidth = false)` branch only, `onDismissRequest = onDecline` per D-16 (outside tap / back == "Not now"). Add its own test tag constant, e.g. `PLAY_GAMES_PROMPT_DIALOG_TEST_TAG`.

**Stacked full-width two-action pattern** (`AdOfferDialog.kt` lines 179-251, `AdOfferActions`): copy the `Column` of a gradient `Button` (positive, "Sign in") directly above a peer-width `TextButton` (negative, "Not now") — same `Modifier.claySquish()`, same `ctaGradientBrush()` background, same `dimensionResource(R.dimen.spacing_sm)` action spacing, same `withClickSound(SoundCue.ButtonForward/ButtonDismiss, ...)` cue wiring. No loading/spinner branch is needed for this dialog — the accept action is not itself bounded/awaited by the composable (the ViewModel handles `signIn()` fire-and-forget).

**Content column tokens** (`AdOfferDialog.kt` lines 137-177, `AdOfferContent`): title (`headlineSmall`/`onSurface`), body (`bodyMedium`/`onSurfaceVariant`), `spacing_lg` outer padding, `spacing_md` content spacing — reuse verbatim; body copy must name leaderboards/achievements/cloud save per D-16 (no icon/glyph is mandated by D-16, but `AdOfferDialog`'s gold `Icon` slot is available as an option — planner's call).

---

### `ui/settings/SettingsUiState.kt` (MODIFIED — + games sign-in field)

**Analog:** itself, `privacyOptionsRequired` (lines 36-41):
```kotlin
@Immutable
data class SettingsUiState(
    val soundOn: Boolean = true,
    val hapticsOn: Boolean = true,
    val privacyOptionsRequired: Boolean = false,
)
```
Add `val gamesSignInState: GamesSignInState = GamesSignInState.Unknown,` — default `Unknown` mirrors D-11's hidden-by-default guarantee before the first collected emission (parallel to `privacyOptionsRequired`'s fail-closed `false` default and its documented rationale in the class KDoc — copy that reasoning style for the new field).

---

### `ui/settings/SettingsViewModel.kt` (MODIFIED — collect session, `onSignInToPlayGames`)

**Analog:** itself, the `consentGateway.privacyOptionsRequired` collector + `onManagePrivacyChoices` (lines 69-73, 117-119):
```kotlin
init {
    viewModelScope.launch {
        consentGateway.privacyOptionsRequired.collect { required ->
            _uiState.update { it.copy(privacyOptionsRequired = required) }
        }
    }
}

fun onManagePrivacyChoices() {
    viewModelScope.launch { consentGateway.showPrivacyOptions() }
}
```
Add a fourth peer collector and inject `GamesService`:
```kotlin
viewModelScope.launch {
    gamesService.session.collect { session ->
        _uiState.update { it.copy(gamesSignInState = session) }
    }
}

fun onSignInToPlayGames() {
    viewModelScope.launch {
        optInRepository.setOptedIn() // D-19
        val signedIn = gamesService.signIn()
        if (!signedIn) { /* raise D-10 snackbar via a one-shot event, same shape as HomeViewModel's */ }
    }
}
```
`SettingsViewModel` needs `PlayGamesOptInRepository` injected too, for D-19's "manual row also sets both flags" requirement — not just `GamesService`.

---

### `ui/settings/SettingsScreen.kt` (MODIFIED — new "Play Games" section)

**Analog:** itself, `PrivacySection` (lines 181-232) — the closest possible precedent, explicitly named by D-08/RESEARCH.md Pattern 5:
```kotlin
@Composable
private fun PrivacySection(
    privacyOptionsRequired: Boolean,
    snackbarHostState: SnackbarHostState,
    onManagePrivacyChoices: () -> Unit,
) {
    SettingsSectionHeader(
        title = stringResource(R.string.settings_section_privacy),
        entranceOrder = PRIVACY_HEADER_ORDER,
    )
    SettingsLinkRow(
        label = stringResource(R.string.settings_privacy_policy_label),
        supporting = stringResource(R.string.settings_privacy_policy_supporting),
        entranceOrder = PRIVACY_POLICY_ROW_ORDER,
        trailingIcon = { Icon(...) },
        onClick = { ... },
    )
    if (privacyOptionsRequired) {
        SettingsLinkRow(
            label = stringResource(R.string.settings_privacy_choices_label),
            supporting = stringResource(R.string.settings_privacy_choices_supporting),
            entranceOrder = PRIVACY_CHOICES_ROW_ORDER,
            trailingIcon = null,
            onClick = onManagePrivacyChoices,
        )
    }
}
```
New `PlayGamesSection`, inserted in the `Column` in `SettingsScreen` between `GeneralSection(...)` and `PrivacySection(...)` (D-08), exhaustively `when`-matching all three `GamesSignInState` members (Pitfall 4 — no `else`):
```kotlin
@Composable
private fun PlayGamesSection(
    gamesSignInState: GamesSignInState,
    onSignInToPlayGames: () -> Unit,
) {
    if (gamesSignInState == GamesSignInState.Unknown) return // D-11

    SettingsSectionHeader(
        title = stringResource(R.string.settings_section_play_games),
        entranceOrder = PLAY_GAMES_HEADER_ORDER,
    )
    when (gamesSignInState) {
        is GamesSignInState.SignedIn ->
            SettingsLinkRow( // non-interactive: onClick = {}, trailingIcon = null (D-06)
                label = gamesSignInState.displayName
                    ?.let { stringResource(R.string.settings_play_games_signed_in_as, it) }
                    ?: stringResource(R.string.settings_play_games_signed_in_fallback),
                supporting = stringResource(R.string.settings_play_games_signin_supporting),
                entranceOrder = PLAY_GAMES_ROW_ORDER,
                trailingIcon = null,
                onClick = {},
            )
        GamesSignInState.SignedOut ->
            SettingsLinkRow(
                label = stringResource(R.string.settings_play_games_signin_label),
                supporting = stringResource(R.string.settings_play_games_signin_supporting),
                entranceOrder = PLAY_GAMES_ROW_ORDER,
                trailingIcon = null,
                onClick = onSignInToPlayGames,
            )
        GamesSignInState.Unknown -> Unit // unreachable — guarded above
    }
}
```
Call site addition in `SettingsScreen`'s `Column` (between `GeneralSection` and `PrivacySection`, matching current lines 122-135):
```kotlin
GeneralSection(...)
PlayGamesSection(
    gamesSignInState = state.gamesSignInState,
    onSignInToPlayGames = onSignInToPlayGames,
)
PrivacySection(...)
```
Also renumber the private `*_ORDER` constants (`SettingsScreen.kt` lines 437-442) to insert `PLAY_GAMES_HEADER_ORDER`/`PLAY_GAMES_ROW_ORDER` between `HAPTICS_ROW_ORDER = 2` and `PRIVACY_HEADER_ORDER` (which shifts down), preserving the `clayEntrance` top-to-bottom stagger invariant documented on `SettingsSectionHeader`'s KDoc.

D-10's error snackbar reuses the exact `runCatching` → `scope.launch { snackbarHostState.showSnackbar(...) }` idiom already in `PrivacySection` (lines 216-223) for the `settings_privacy_policy_error` message — apply the same shape keyed off a failed `onSignInToPlayGames()` result.

---

### `AndroidManifest.xml` (MODIFIED — `com.google.android.gms.games.APP_ID` meta-data)

**Analog:** itself, the existing `com.google.android.gms.ads.APPLICATION_ID` meta-data block (lines 39-49):
```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="@string/admob_app_id" />
```
Add, following the exact same shape, referencing the already-built `game_services_project_id` resource (`app/build.gradle.kts` lines 195/232):
```xml
<meta-data
    android:name="com.google.android.gms.games.APP_ID"
    android:value="@string/game_services_project_id" />
```
Place it inside `<application>`, immediately above the existing `PlayGamesInitProvider` `tools:node="remove"` `<provider>` block (lines 74-78) — that removal comment already names Phase 11 as "the explicit-init owner," so the two Play-Games-related manifest entries stay adjacent.

---

### `gradle/libs.versions.toml` + `app/build.gradle.kts` (MODIFIED — new library entry)

**Analog:** itself — the existing `play-services-games-v2` entry pattern (`gradle/libs.versions.toml` lines 104, 191; `app/build.gradle.kts` line 345):
```toml
# [versions]
playServicesGamesV2 = "22.0.0"

# [libraries]
play-services-games-v2 = { group = "com.google.android.gms", name = "play-services-games-v2", version.ref = "playServicesGamesV2" }
```
```kotlin
// app/build.gradle.kts
implementation(libs.play.services.games.v2)
```
Add a new library entry, version-ref'd to the **existing** `coroutines = "1.11.0"` catalog key (already used by `kotlinx-coroutines-test`, `gradle/libs.versions.toml` line 201) rather than a new version string:
```toml
# [libraries]
kotlinx-coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "coroutines" }
```
```kotlin
// app/build.gradle.kts, beside the existing play-services-games-v2 implementation line
implementation(libs.kotlinx.coroutines.play.services)
```

---

## Shared Patterns

### Fire-and-forget platform bootstrap
**Source:** `platform/ads/consent/ConsentGateway.kt` (interface contract, lines 43-58) + `platform/ads/consent/UmpConsentGateway.kt` (`bootstrap()` impl, lines 108-163) + `MainActivity.kt` (call site, lines 54-71)
**Apply to:** `GamesService.bootstrapIfOptedIn()` / `PlayGamesService` / `MainActivity`
```kotlin
// Interface: non-suspend, fire-and-forget, called once per Activity creation
fun bootstrap()

// MainActivity: after super.onCreate(), before setContent
consentGateway.bootstrap()
gamesService.bootstrapIfOptedIn()
```

### `@Singleton`-on-class for stateful platform seams
**Source:** `platform/activity/DefaultForegroundActivityProvider.kt` (KDoc lines 21-31) + `platform/ads/consent/UmpConsentGateway.kt` (KDoc lines 43-49)
**Apply to:** `PlayGamesService` (holds `_session` `MutableStateFlow`)
> `@Binds @Singleton fun bind(impl: X): Y` scopes the binding of *Y*, not of *X* — a class holding mutable state (a `StateFlow`, an `AtomicBoolean` guard) MUST also carry `@Singleton` on the class itself, or Hilt constructs a second, unscoped, never-updated instance.

### SDK types never cross the `PlatformBoundaryRules` gate
**Source:** `di/PlatformModule.kt` KDoc (lines 44-63) + `platform/ads/di/AdsModule.kt` KDoc (lines 20-55) + `app/src/test/java/com/app/pegsolitaire/PlatformBoundaryRules.kt`
**Apply to:** `PlayGamesService`, `di/PlatformModule.kt`
> Never add an SDK-typed `@Provides`/`@Binds` anywhere — Hilt's generated `DaggerApp_HiltComponents_SingletonC` lives outside `..platform..` and fails the bytecode gate regardless of which module declares it. Construct `GamesSignInClient`/`PlayersClient` handles as internal per-call `PlayGames.get*Client(activity)` lookups inside `PlayGamesService`; bind only the SDK-free `GamesService` interface in `PlatformModule`.

### Session/readiness state collected into UiState (three parallel `viewModelScope.launch` collectors)
**Source:** `ui/settings/SettingsViewModel.kt` `init` block (lines 58-74)
**Apply to:** `SettingsViewModel` (add a 4th collector for `gamesService.session`), `HomeViewModel` (add a collector combining `onboardingSeen` + `promptAnswered`)
```kotlin
init {
    viewModelScope.launch {
        someFlow.collect { value -> _uiState.update { it.copy(field = value) } }
    }
}
```

### Clay-styled modal dialog, hoisted state, dismiss-on-outside/back
**Source:** `ui/game/AdOfferDialog.kt` (full file)
**Apply to:** `ui/home/PlayGamesPromptDialog.kt`
```kotlin
Dialog(
    onDismissRequest = onDecline,
    properties = DialogProperties(usePlatformDefaultWidth = false),
) {
    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier.fillMaxWidth()
            .padding(horizontal = dimensionResource(R.dimen.spacing_lg))
            .widthIn(max = dimensionResource(R.dimen.content_max_width))
            .clayShadow(dimensionResource(R.dimen.elevation_dialog), shape),
    ) { /* title, body, stacked gradient Button + peer TextButton */ }
}
```

### `ClaySnackbarHost` wiring for a fallible action
**Source:** `ui/settings/SettingsScreen.kt` `PrivacySection` (lines 184-233) + `ui/components/ClaySnackbarHost.kt`
**Apply to:** `SettingsScreen`'s Play Games row (D-10) and `HomeScreen`'s new snackbar host (D-18)
```kotlin
val snackbarHostState = remember { SnackbarHostState() }
Scaffold(snackbarHost = { ClaySnackbarHost(snackbarHostState) }, ...) { ... }

val scope = rememberCoroutineScope()
val errorMessage = stringResource(R.string.settings_play_games_signin_error) // resolved in composition
// on failure:
scope.launch { snackbarHostState.showSnackbar(message = errorMessage, duration = SnackbarDuration.Short) }
```

### DataStore-backed boolean-flag repository over the shared `app_prefs` store
**Source:** `data/OnboardingRepository.kt` (full file) + `data/AppPreferences.kt`
**Apply to:** `data/PlayGamesOptInRepository.kt` / `data/PlayGamesPreferences.kt`
```kotlin
internal object XPreferences {
    val KEY = booleanPreferencesKey("x")
    const val DEFAULT = false
}

@Singleton
class XRepository @Inject constructor(private val dataStore: DataStore<Preferences>) {
    val flag: Flow<Boolean> =
        dataStore.data.catch { emit(emptyPreferences()) }.map { it[XPreferences.KEY] ?: XPreferences.DEFAULT }
    suspend fun setFlag() { dataStore.edit { it[XPreferences.KEY] = true } }
}
```

### Test-double fake for a platform seam (`StateFlow` + call-count tracking)
**Source:** `app/src/test/java/com/app/pegsolitaire/ui/settings/FakeConsentGateway.kt` (full file, 59 lines)
**Apply to:** the new fake `GamesService` for `HomeViewModelTest`/`SettingsViewModelTest`
```kotlin
class FakeGamesService(
    session: GamesSignInState = GamesSignInState.SignedOut,
) : GamesService {
    private val sessionState = MutableStateFlow(session)
    override val session: StateFlow<GamesSignInState> = sessionState.asStateFlow()
    var signInResult: Boolean = true
    var signInCallCount = 0
        private set
    override suspend fun signIn(): Boolean { signInCallCount++; return signInResult }
    override fun bootstrapIfOptedIn() = Unit
    // ...submitScore/unlockAchievement/etc. no-ops, mirroring NoOpGamesService
    fun emitSession(state: GamesSignInState) { sessionState.value = state }
}
```

### ViewModel test harness (`StandardTestDispatcher` + fake repository/gateway)
**Source:** `app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsViewModelTest.kt` (full file)
**Apply to:** `PlayGamesServiceTest.kt`, `PlayGamesOptInRepositoryTest.kt`, extended `HomeViewModelTest.kt`/`SettingsViewModelTest.kt`
```kotlin
class XTest {
    private val testDispatcher = StandardTestDispatcher()
    @Before fun setUp() { Dispatchers.setMain(testDispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun someBehavior() = runTest(testDispatcher) {
        val viewModel = XViewModel(fakeRepo, fakeGateway)
        runCurrent()
        assertThat(viewModel.uiState.value.field).isEqualTo(expected)
    }
}
```

### Compose UI test for a conditional dialog/section (Robolectric + `createComposeRule`)
**Source:** `app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenAdOfferTest.kt` (first ~120 lines shown)
**Apply to:** `PlayGamesPromptDialog` dismiss/accept/decline tests and the Settings Play Games section's three-state rendering test
```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SOME_ROBOLECTRIC_SDK])
class XComposeTest {
    @get:Rule val composeRule = createComposeRule()
    private val app get() = RuntimeEnvironment.getApplication()

    @Test
    fun noStateRendersNoDialog() {
        setScreen(UiState.initial())
        composeRule.onNodeWithTag(SOME_TEST_TAG).assertDoesNotExist()
    }
}
```

---

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `app/src/test/.../data/PlayGamesOptInRepositoryTest.kt` | test | CRUD | No `OnboardingRepositoryTest.kt` exists in the repo (`find app/src/test -iname "*OnboardingRepository*"` returns nothing) despite `data/OnboardingRepository.kt` itself being the production analog. RESEARCH.md's own Wave 0 Gaps table names this same gap ("modeled on any existing `OnboardingRepositoryTest.kt`" — conditional, because none exists). Fall back to the general DataStore-repository test shape used elsewhere in the suite (a real temp-file `DataStore<Preferences>` or a throwing one, per `OnboardingRepository`'s own KDoc: "unit tests construct the repository with a real temp-file store or a throwing one over the same seam") and to `SettingsViewModelTest`'s fake-repository idiom for the *consuming* ViewModel tests. |

## Metadata

**Analog search scope:** `app/src/main/java/com/app/pegsolitaire/platform/`, `app/src/main/java/com/app/pegsolitaire/data/`, `app/src/main/java/com/app/pegsolitaire/ui/home/`, `app/src/main/java/com/app/pegsolitaire/ui/settings/`, `app/src/main/java/com/app/pegsolitaire/ui/game/`, `app/src/main/java/com/app/pegsolitaire/ui/components/`, `app/src/main/java/com/app/pegsolitaire/di/`, `app/src/main/AndroidManifest.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `app/src/test/java/com/app/pegsolitaire/` (platform/games, ui/home, ui/settings, ui/game, root boundary-gate files)
**Files scanned:** 30 production files + 6 test files read in full or targeted excerpt
**Pattern extraction date:** 2026-09-03
