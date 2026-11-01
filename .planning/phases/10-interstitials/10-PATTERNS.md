# Phase 10: Interstitials - Pattern Map

**Mapped:** 2026-09-02
**Files analyzed:** 24 (10 new, 14 modified)
**Analogs found:** 24 / 24 (14 exact, 8 self-analog/in-file, 2 role-match)

Every new file in this phase has a shipped in-repo twin. This is a **clone-and-delta phase**, not a
greenfield one — the single largest risk named in RESEARCH.md ("a second, subtly different ad-holding
class gets written and drifts from the first") is a *pattern* risk, so the assignments below are
deliberately prescriptive about which file to copy from.

---

## File Classification

### New files

| New file | Role | Data Flow | Closest Analog | Match |
|----------|------|-----------|----------------|-------|
| `domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceState.kt` | model + policy (pure) | transform (pure decision over immutable value) | `domain/src/main/kotlin/com/app/pegsolitaire/domain/assist/AssistAllowance.kt` | exact |
| `domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceParams.kt` | config (scoped constants) | n/a (static) | `AssistAllowance.kt:34-44` (`AssistAllowanceLimits`) + `domain/.../scoring/ScoreParams.kt` | exact |
| `app/src/main/java/com/app/pegsolitaire/data/InterstitialCadenceStore.kt` | repository / store | CRUD over persisted scalars + reactive read | `app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt` | exact |
| `app/src/main/java/com/app/pegsolitaire/platform/ads/InterstitialAdSlot.kt` | service (SDK holder) | request-response (async load, single-show) | `app/src/main/java/com/app/pegsolitaire/platform/ads/RewardedAdSlot.kt` | exact |
| `app/src/main/java/com/app/pegsolitaire/platform/lifecycle/AppForegroundTracker.kt` | platform observer | event-driven | `app/src/main/java/com/app/pegsolitaire/platform/activity/DefaultForegroundActivityProvider.kt` | role-match |
| `domain/src/test/kotlin/com/app/pegsolitaire/domain/ads/InterstitialCadenceStateTest.kt` | test (pure JVM) | transform | `domain/src/test/kotlin/com/app/pegsolitaire/domain/assist/AssistAllowanceTest.kt` | exact |
| `app/src/test/java/com/app/pegsolitaire/data/InterstitialCadenceStoreTest.kt` | test (JVM + temp DataStore) | CRUD | `app/src/test/java/com/app/pegsolitaire/data/ChallengeProgressTest.kt` | exact |
| `app/src/test/java/com/app/pegsolitaire/ui/results/FakeAdGateway.kt` | test double | request-response | `app/src/test/java/com/app/pegsolitaire/ui/settings/FakeConsentGateway.kt` + `GameViewModelAdClockTest.kt:464-489` | exact |

### Modified files

| Modified file | Role | Data Flow | Closest Analog | Match |
|---------------|------|-----------|----------------|-------|
| `app/.../platform/ads/AdGateway.kt` | interface / seam | request-response | itself — the rewarded triple at `AdGateway.kt:40-63` | self-analog |
| `app/.../platform/ads/AdMobAdGateway.kt` | service (gateway impl) | request-response + callback→coroutine bridge | itself — `requestRewarded` `:111-131`, `showForOutcome` `:156-189`, `recycleSlot` `:204-207` | self-analog |
| `app/.../platform/ads/NoOpAdGateway.kt` | stub | request-response | itself `:37-49` | self-analog |
| `app/.../platform/ads/AdConstants.kt` | config (timing tokens) | n/a | itself `:52-69` (`REWARDED_AD_MAX_AGE_MS`) | self-analog |
| `app/.../ui/results/ResultsUiState.kt` | UI state model | n/a (immutable state) | itself `:50` (`playCelebration`) + `:58-59` (`showNextLevel` derived) | self-analog |
| `app/.../ui/results/ResultsViewModel.kt` | ViewModel | event-driven + request-response sequencing | `app/.../ui/game/GameViewModel.kt:430-445` + `:1294-1328` (ad funnel + accept sequencing) | role-match |
| `app/.../ui/results/ResultsRoute.kt` | route (stateful wrapper) | event-driven (one-shot nav) | `app/.../ui/game/GameRoute.kt:60-73` (one-shot nav effect) | role-match |
| `app/.../ui/results/ResultsActions.kt` | composable leaf | UI events | itself `:37-66` | self-analog |
| `app/.../ui/results/ResultsScreen.kt` | stateless screen (+ scrim overlay) | UI render | `app/.../ui/onboarding/OnboardingOverlay.kt:61-68` (scrim Box) + `AdOfferDialog.kt:98-113` (input lock while in-flight) | role-match |
| `app/.../di/PlatformModule.kt` (or `platform/ads/di/AdsModule.kt`) | DI config | n/a | `PlatformModule.kt:81-91` | self-analog |
| `app/.../App.kt` | app entry | event-driven registration | `App.kt:58-62` (`activityProvider.attachTo(this)`) | self-analog |
| `gradle/libs.versions.toml` + `app/build.gradle.kts` | build config | n/a | `libs.versions.toml:48-52,151-156` | self-analog |
| `app/src/main/res/values/dimens.xml` | resource config | n/a | `dimens.xml:112-126` (Phase-9 scoped-token comments) | self-analog |
| `app/src/test/.../ui/results/ResultsViewModelTest.kt`, `ResultsScreenTest.kt`, `platform/ads/NoOpAdGatewayTest.kt` | tests | — | themselves + `GameViewModelAdClockTest.kt` | self-analog |

---

## Pattern Assignments

### `domain/.../domain/ads/InterstitialCadenceState.kt` + `InterstitialCadenceParams.kt` (model+config, transform)

**Analog:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/assist/AssistAllowance.kt`

**Constants-object pattern** (`AssistAllowance.kt:23-44`) — one KDoc'd `const val` per decision, class
KDoc naming the no-magic-number rule and the "written exactly once" contract:

```kotlin
/**
 * Scoped tunable constants for the per-level free-assist allowance (ADS-02).
 *
 * Split by scope per QUAL-03's no-magic-number rule, in the same shape as `ScoreParams`:
 * each value is a KDoc'd `const val` naming the decision that fixes it. **These two
 * numbers are written exactly once in the whole codebase** (D-02) ...
 */
object AssistAllowanceLimits {
    /** Free hints granted per level before an ad is offered (D-02). */
    const val FREE_HINTS = 3

    /**
     * Free undos granted per level before an ad is offered (D-02). Higher than
     * [FREE_HINTS] because undo is a correction of the player's own mistake rather than
     * a shortcut past the puzzle, so gating it early would read as a punishment.
     */
    const val FREE_UNDOS = 5
}
```

Note the *derived-constant* discipline in the sibling `ScoreParams.kt:40-50`: `TWO_STAR_MAX_PEGS =
THREE_STAR_MAX_PEGS + TWO_STAR_BAND_WIDTH` rather than a standalone literal, "so the boundary and its
anchor cannot drift apart". Apply the same if any cadence value is a function of another.

**Immutable-value + mutators-return-new pattern** (`AssistAllowance.kt:59-101`):

```kotlin
data class AssistAllowance(
    val hintsRemaining: Int = AssistAllowanceLimits.FREE_HINTS,
    val undosRemaining: Int = AssistAllowanceLimits.FREE_UNDOS,
) {
    /** Whether [kind] can be used for free right now — i.e. without offering an ad. */
    fun hasFree(kind: AssistKind): Boolean = remaining(kind) > 0

    fun spend(kind: AssistKind): AssistAllowance =
        when (kind) {
            AssistKind.HINT -> copy(hintsRemaining = (hintsRemaining - 1).coerceAtLeast(0))
            AssistKind.UNDO -> copy(undosRemaining = (undosRemaining - 1).coerceAtLeast(0))
        }
}
```

`mayShow(nowMs)` / `recordShown(nowMs)` / `recordWin()` / `startNewSession()` are exactly this shape.
The `coerceAtLeast(0)` saturation at `:87-88` is the precedent for the security-domain requirement
that `lifetimeCompletedLevels` must not overflow (ASVS V5, RESEARCH § Security).

**Validate-on-read / fail-safe-direction pattern** (`AssistAllowance.kt:103-140`) — the model for
reading tampered or absent persisted cadence counters. Note the *direction* differs: `AssistAllowance`
fails **open to full**, cadence must fail **toward fewer ads**:

```kotlin
companion object {
    fun fromStoredOrFull(hints: Int?, undos: Int?): AssistAllowance =
        AssistAllowance(
            hintsRemaining = hints.orFullIfOutOfRange(AssistAllowanceLimits.FREE_HINTS),
            undosRemaining = undos.orFullIfOutOfRange(AssistAllowanceLimits.FREE_UNDOS),
        )

    /** This stored counter if it is a value [limit] could have produced, else [limit]. */
    private fun Int?.orFullIfOutOfRange(limit: Int): Int =
        if (this != null && this in 0..limit) this else limit
}
```

**Duplication-over-inverted-dependency KDoc** (`AssistAllowance.kt:3-14`) — reproduce this argument
verbatim in shape if the cadence policy needs any app-side concept:

```kotlin
/**
 * ## Why this is not `AdReason`
 *
 * `AdReason` (`app`'s `platform/ads/`) names the same two things from the *ads* side, and
 * the duplication is deliberate rather than an oversight to be tidied away. `:domain` is
 * pure Kotlin with zero dependencies on app code — `DomainPurityTest` enforces exactly
 * that — so reaching for `AdReason` here would invert the module graph ...
 */
```

**Purity gate:** `app/src/test/java/com/app/pegsolitaire/DomainPurityTest.kt:23-31` fails on any
`android..`/`androidx..` dependency from `..domain..`. This is what mechanically forbids `SystemClock`
in the policy — which is why `nowMs` must be a **parameter**, per `ProgressRepository.recordResult`'s
existing `nowMs = System.currentTimeMillis()` caller-supplied shape (`ResultsViewModel.kt:137`).

---

### `app/.../data/InterstitialCadenceStore.kt` (repository, CRUD over persisted scalars)

**Analog:** `app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt` — copy the whole shape.

**Imports pattern** (`ChallengeProgress.kt:1-17`):

```kotlin
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.app.pegsolitaire.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
```

**Scoped-keys object** (`ChallengeProgress.kt:19-26`):

```kotlin
/** Scoped constants for the Challenge-progress Preferences keys (QUAL-03 — no inline literals). */
internal object ChallengeProgressPreferences {
    /** The int key holding the player's current Challenge level (1-based). */
    val CURRENT_LEVEL_KEY = intPreferencesKey("challenge_current_level")

    /** Fail-open default: a fresh install — or a corrupt/IO read — starts at level 1. */
    const val DEFAULT_CURRENT_LEVEL = 1
}
```

**`@Singleton`-on-the-class + injected shared store + app scope** (`ChallengeProgress.kt:43-49`).
The `@Singleton` on the *class* is load-bearing — `DefaultForegroundActivityProvider.kt:21-31` explains
why at length (a `@Binds @Singleton` scopes the interface, not the impl).

```kotlin
@Singleton
class ChallengeProgress
    @Inject
    constructor(
        private val dataStore: DataStore<Preferences>,
        @ApplicationScope private val scope: CoroutineScope,
    ) {
```

**Fail-open reactive read** (`ChallengeProgress.kt:58-68`) — `catch { emit(emptyPreferences()) }` then
`stateIn(Eagerly, DEFAULT)` for a synchronously readable `.value`:

```kotlin
val currentLevel: StateFlow<Int> =
    dataStore.data
        .catch { emit(emptyPreferences()) }
        .map {
            it[ChallengeProgressPreferences.CURRENT_LEVEL_KEY]
                ?: ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL,
        )
```

**Best-effort write, merged INSIDE the transaction** (`ChallengeProgress.kt:80-95`) — the
`storageFailSafe` wrapper is mandatory, not decorative:

```kotlin
fun advancePast(solvedLevel: Int) {
    // Best-effort (T-05-13): the launch runs on the app-lifetime scope, whose
    // SupervisorJob isolates a failed child but does NOT swallow the exception —
    // an unguarded IO failure here would still crash the app.
    scope.launch {
        storageFailSafe(fallback = Unit) {
            dataStore.edit { preferences ->
                val current =
                    preferences[ChallengeProgressPreferences.CURRENT_LEVEL_KEY]
                        ?: ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL
                preferences[ChallengeProgressPreferences.CURRENT_LEVEL_KEY] =
                    maxOf(current, solvedLevel + 1)
            }
        }
    }
}
```

**Hard constraint — one delegate only.** `app/.../data/AppPreferences.kt:8-24` is the single
`preferencesDataStore(name = "app_prefs")` delegate; it is Hilt-provided as
`DataStore<Preferences>` in `di/DataModule.kt:71-75`. Inject that type; never declare a second
delegate ("There are multiple DataStores active for the same file" crashes at runtime).

---

### `app/.../platform/ads/InterstitialAdSlot.kt` (service, request-response)

**Analog:** `app/src/main/java/com/app/pegsolitaire/platform/ads/RewardedAdSlot.kt` — a near-verbatim
clone. Delta = the SDK type and the dropped reward listener; **everything else transfers unchanged**.

**Imports pattern** (`RewardedAdSlot.kt:1-16`) — swap only the ad type:

```kotlin
import android.os.SystemClock
import com.app.pegsolitaire.di.ApplicationScope
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd   // → interstitial.InterstitialAd
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import javax.inject.Inject
import javax.inject.Singleton
```

**Class shell + the three confined fields** (`RewardedAdSlot.kt:54-71`):

```kotlin
@Singleton
class RewardedAdSlot
    @Inject
    constructor(
        @param:ApplicationScope private val scope: CoroutineScope,
    ) {
        /** The one ad, or `null` when the slot is empty. */
        private var held: RewardedAd? = null

        /**
         * When [held] was loaded, on the monotonic
         * [SystemClock.elapsedRealtime] timebase — deliberately not wall-clock, which a
         * timezone change or an NTP correction can move backwards under a long session.
         */
        private var loadedAtMs: Long = 0L

        /** The load currently in flight, so two callers share one request rather than racing. */
        private var inFlight: Deferred<RewardedAd?>? = null
```

**Core lifecycle: preload / await / invalidate / freshOrDiscard** (`RewardedAdSlot.kt:88-136`):

```kotlin
fun preload() {
    if (freshOrDiscard() == null) startLoad()
}

suspend fun await(): RewardedAd? = freshOrDiscard() ?: startLoad().await()

fun invalidate() {
    held?.destroy()
    held = null
    loadedAtMs = 0L
}

private fun freshOrDiscard(): RewardedAd? {
    val age = SystemClock.elapsedRealtime() - loadedAtMs
    if (held != null && age >= AdConstants.REWARDED_AD_MAX_AGE_MS) invalidate()
    return held
}
```

**App-scope load + one bounded retry** (`RewardedAdSlot.kt:145-179`):

```kotlin
private fun startLoad(): Deferred<RewardedAd?> {
    val running = inFlight
    if (running != null && running.isActive) return running
    // Dispatchers.Main because every GMA call other than MobileAds.initialize must
    // be made from the main thread, and because it is the confinement that keeps
    // the three fields above lock-free. See the class KDoc.
    return scope.async(Dispatchers.Main) { loadIntoSlot() }.also { inFlight = it }
}

private suspend fun loadIntoSlot(): RewardedAd? {
    val ad =
        loadOnce() ?: run {
            delay(AdConstants.PRELOAD_RETRY_DELAY_MS)
            loadOnce()
        }
    if (ad != null) {
        held = ad
        loadedAtMs = SystemClock.elapsedRealtime()
    }
    inFlight = null
    return ad
}
```

**Error handling: no-fill is not an error path** (`RewardedAdSlot.kt:198-206`) — the exact function to
retype for interstitials:

```kotlin
private suspend fun loadOnce(): RewardedAd? {
    val outcome = runCatching { RewardedAd.load(AdRequest.Builder(AdIds.rewardedAdUnitId).build()) }
    currentCoroutineContext().ensureActive()
    return when (val result = outcome.getOrNull()) {
        is AdLoadResult.Success -> result.ad
        is AdLoadResult.Failure -> null
        null -> null
    }
}
```

`AdIds.interstitialAdUnitId` already exists — `platform/ads/AdIds.kt:41-42`. No ad-ID literal may
appear under `src/main` (`adIdLiteralCheck`); `AdIds.kt:20-28` documents why.

**Boundary constraint:** the `InterstitialAd` must never appear in any constructor parameter or member
signature. `platform/ads/di/AdsModule.kt:20-52` and `di/PlatformModule.kt:44-63` both carry the full
empirical write-up of why (Hilt's generated `SingletonC` lives in the root package and fails
`PlatformBoundaryTest` regardless of which module declares the binding). Read one of them before
touching DI.

---

### `app/.../platform/ads/AdGateway.kt` + `AdMobAdGateway.kt` + `NoOpAdGateway.kt` (seam + impls)

**Analog:** the rewarded triple already on the same three files — the new interstitial members are the
same three shapes.

**Seam shape to mirror** (`AdGateway.kt:40`, `:53`, `:63`):

```kotlin
val rewardedOfferable: StateFlow<Boolean>
fun preloadRewarded(reason: AdReason)
suspend fun requestRewarded(reason: AdReason): RewardOutcome
```

The KDoc at `AdGateway.kt:17-39` is the model for documenting `interstitialReady`: it explains why the
signal is *observable* rather than a plain getter ("consent resolves asynchronously after launch, so a
surface must react to readiness arriving rather than hold a stale answer") and why it is a separate
member rather than a fourth outcome. The stale line to replace is `AdGateway.kt:65-69`.

**Derived readiness flow** (`AdMobAdGateway.kt:80-83`) — derive, never mirror into a second mutable flow:

```kotlin
override val rewardedOfferable: StateFlow<Boolean> =
    consentGateway.readiness
        .map { it == AdsReadiness.Ready }
        .stateIn(scope, SharingStarted.Eagerly, false)
```

**Consent-gated preload** (`AdMobAdGateway.kt:94-96`) — the exact precedent for D-12's double gate
(consent AND cadence):

```kotlin
override fun preloadRewarded(reason: AdReason) {
    if (rewardedOfferable.value) slot.preload()
}
```

**Show path: capture the Activity BEFORE show(), bounded waits** (`AdMobAdGateway.kt:111-131`):

```kotlin
override suspend fun requestRewarded(reason: AdReason): RewardOutcome {
    if (consentGateway.readiness.value != AdsReadiness.Ready) return RewardOutcome.Unavailable

    // D-10's bounded wait — this is the spinner the dialog is showing.
    val ad = withTimeoutOrNull(AdConstants.ACCEPT_WAIT_MS) { slot.await() }

    // Captured BEFORE show(), and never re-awaited inside a callback: showing a
    // fullscreen ad pauses the host, so ForegroundActivityProvider.current is
    // deliberately null for the whole presentation and the failure mode of asking
    // again from a callback is a hang, not a crash (Pitfall 13, T-09-45).
    val activity =
        ad?.let {
            withTimeoutOrNull(AdConstants.ACTIVITY_WAIT_MS) { activityProvider.awaitActivity() }
        }

    return if (ad != null && activity != null) {
        showForOutcome(ad, activity, reason)
    } else {
        RewardOutcome.Unavailable
    }
}
```

**Callback → coroutine bridge** (`AdMobAdGateway.kt:156-189`) — copy this verbatim minus the reward
listener; the interstitial outcome is binary (`onAdDismissedFullScreenContent` → `true`,
`onAdFailedToShowFullScreenContent` → `false`), and `onAdShowedFullScreenContent` is the scrim-lift signal:

```kotlin
private suspend fun showForOutcome(
    ad: RewardedAd,
    activity: Activity,
    reason: AdReason,
): RewardOutcome =
    suspendCancellableCoroutine { continuation ->
        var earned = false

        ad.adEventCallback =
            object : RewardedAdEventCallback {
                override fun onAdDismissedFullScreenContent() {
                    recycleSlot(reason)
                    if (continuation.isActive) {
                        continuation.resume(
                            if (earned) RewardOutcome.Granted else RewardOutcome.Declined,
                        )
                    }
                }

                override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                    recycleSlot(reason)
                    if (continuation.isActive) continuation.resume(RewardOutcome.Unavailable)
                }
            }

        ad.show(activity) { _: RewardItem -> earned = true }   // interstitial: ad.show(activity)

        continuation.invokeOnCancellation { ad.adEventCallback = null }
    }
```

**Recycle-then-rewarm** (`AdMobAdGateway.kt:204-207`) — destroy is mandatory (single-show), re-warm is
one bounded request, not a buffer:

```kotlin
private fun recycleSlot(reason: AdReason) {
    slot.invalidate()
    preloadRewarded(reason)
}
```

**The line this phase replaces** (`AdMobAdGateway.kt:133-138`):

```kotlin
/**
 * Deliberately still a no-op. Interstitials between levels are Phase 10's work,
 * and adding one here would put an unrequested fullscreen ad in a phase whose
 * contract is "the player is always asked first".
 */
override fun showInterstitial() = Unit
```

**Stub parity** (`NoOpAdGateway.kt:37-49`) — note the deliberate polarity asymmetry documented at
`:24-35`: `rewardedOfferable = true` because the dialog must be reachable against the stub, whereas the
interstitial stub must stay **false / never shows** (v1.0 had no interstitials):

```kotlin
class NoOpAdGateway
    @Inject
    constructor() : AdGateway {
        private val offerable = MutableStateFlow(true)

        override val rewardedOfferable: StateFlow<Boolean> = offerable.asStateFlow()

        override fun preloadRewarded(reason: AdReason) = Unit

        override suspend fun requestRewarded(reason: AdReason): RewardOutcome = RewardOutcome.Granted

        override fun showInterstitial() = Unit
    }
```

**Constants extension** (`AdConstants.kt:52-69`) — the `REWARDED_AD_MAX_AGE_MS` KDoc is the template for
an `INTERSTITIAL_AD_MAX_AGE_MS`, including the explicit `[ASSUMED]` labelling of the expiry figure and
the "correctness does not rest on it being right" paragraph. `AdConstants.kt:3-15` explicitly limits
this object's remit to *ad timing only* — the D-01..D-04 cadence values must **not** land here.

---

### `app/.../platform/lifecycle/AppForegroundTracker.kt` (platform observer, event-driven)

**Analog:** `app/src/main/java/com/app/pegsolitaire/platform/activity/DefaultForegroundActivityProvider.kt`
(role-match — same layer, same `@Singleton`-holding-state shape, different lifecycle API).

**`@Singleton`-on-the-class rationale to reproduce** (`DefaultForegroundActivityProvider.kt:21-36`):

```kotlin
/**
 * **This class carries `@Singleton`, and that is not a stylistic slip.**
 * `@Binds @Singleton fun bind(impl: X): Y` scopes the binding of *`Y`*, not of `X` — so
 * if anything injected the concrete type it would get a **second, unscoped instance**
 * that is never fed a lifecycle event, and `awaitActivity()` would hang forever with no
 * exception (T-06-15). [com.app.pegsolitaire.App] therefore injects the *interface* and
 * never names this class.
 */
@Singleton
class DefaultForegroundActivityProvider
    @Inject
    constructor() : ForegroundActivityProvider, Application.ActivityLifecycleCallbacks {
```

**Registration hook** (`App.kt:58-62`) — `attach()` goes on the line after `attachTo`:

```kotlin
override fun onCreate() {
    super.onCreate()
    activityProvider.attachTo(this)
    appScope.launch { soundPlayer.preload() }
}
```

`App.kt:32-42` records two constraints that apply to the new tracker: **declare the interface, never the
implementation** (a concrete type yields a second unscoped instance), and `App` is exempted by name in
`PlatformBoundaryTest` via `haveNameNotMatching(".*\\.App")` — if a new platform type is injected here,
confirm the exemption still covers it.

**Anti-pattern (explicit):** do NOT source the session boundary from `DefaultForegroundActivityProvider`.
Its `onActivityPaused` clear at `:56-58` fires on every fullscreen ad presentation:

```kotlin
override fun onActivityPaused(activity: Activity) {
    if (current === activity) resumed.value = null
}
```

**Build wiring** (`gradle/libs.versions.toml:48-52`, `:151-156`) — the atomic-group pin and the existing
declaration style to copy:

```toml
# androidx.lifecycle is an ATOMIC GROUP — every lifecycle-* artifact must share
# one version. 2.11.0's `-compose` variants require compileSdk 37 (uninstallable
# here); 2.9.4 is the highest line whose `-compose` variants keep minCompileSdk 36
# and is exactly what the Compose BOM 2026.06.00 already resolves transitively.
androidxLifecycle = "2.9.4"

androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "androidxLifecycle" }
```

New entry uses `version.ref = "androidxLifecycle"` — **no new version key**. Consumed in
`app/build.gradle.kts` beside `implementation(libs.androidx.lifecycle.runtime.compose)` (line 296).

---

### `app/.../ui/results/ResultsViewModel.kt` (ViewModel, event-driven + sequencing)

**Analogs:** itself (the `SavedStateHandle` one-shot latch) and `app/.../ui/game/GameViewModel.kt`
(the ad funnel + presentation sequencing).

**One-shot latch idiom, already used twice in this file** (`ResultsViewModel.kt:118-121`, `:157-161`) —
this is the D-09 "navigate exactly once" mechanism, and it survives both a config change and process death:

```kotlin
private fun claimCelebrationPlayOnce() {
    if (savedStateHandle.get<Boolean>(CELEBRATED_KEY) == true) return
    savedStateHandle[CELEBRATED_KEY] = true
    _uiState.update { it.copy(playCelebration = true) }
}
```

with the keys scoped in the private companion (`ResultsViewModel.kt:178-189`):

```kotlin
private companion object {
    /** SavedStateHandle flag marking this play as already merged into the stored bests. */
    const val RECORDED_KEY = "results_recorded"

    /** SavedStateHandle flag marking this result's celebration as already claimed (D-16). */
    const val CELEBRATED_KEY = "results_celebrated"
}
```

**Decision-gate pattern for the D-06 eligibility test** (`GameViewModel.kt:430-445`) — a single small
`when` whose *order is the design*, with a KDoc enumerating each branch (`:402-429`):

```kotlin
private fun enterAssistFunnel(
    kind: AssistKind,
    reason: AdReason,
): Boolean =
    when {
        allowance.hasFree(kind) -> {
            allowance = allowance.spend(kind)
            if (!allowance.hasFree(kind)) adGateway.preloadRewarded(reason)
            true
        }
        !adGateway.rewardedOfferable.value -> true
        else -> {
            _uiState.update { it.copy(pendingAdOffer = reason) }
            false
        }
    }
```

**Presentation sequencing with a documented order** (`GameViewModel.kt:1294-1313`) — state up *first*,
then the suspending gateway call, then the resolution. The `try/finally` around the gateway call is the
pattern for guaranteeing the scrim comes down on every exit including cancellation:

```kotlin
fun onAcceptAdOffer() {
    val reason = _uiState.value.pendingAdOffer ?: return
    val kind = reason.toAssistKind()
    viewModelScope.launch {
        _uiState.update { it.copy(adOfferLoading = true) }
        freezeClockForAd()
        val outcome =
            try {
                adGateway.requestRewarded(reason)
            } finally {
                unfreezeClockAfterAd()
            }
        _uiState.update { it.copy(pendingAdOffer = null, adOfferLoading = false) }
        if (!grantReward(outcome, kind)) return@launch
        when (kind) {
            AssistKind.HINT -> revealPendingHint()
            AssistKind.UNDO -> performUndo()
        }
    }
}
```

**The WARN-03 boundary-clamp pattern to preserve** (`ResultsViewModel.kt:82`) — every level-keyed read in
the file goes through this one value; the new ad path must too:

```kotlin
private val challengeLevel: Int? = args.challengeLevel?.let(ChallengeLevelBounds::clamp)
```

**The trap this file already contains** (`ResultsViewModel.kt:119`, `:179-180` and
`ResultsUiState.kt:58-63`) — the `PEGS_REMAINING_SOLVED = 1` constant. It is correct where it is used
(Challenge footprints all have `minimumPegs == 1`) and is exactly the line a developer would copy into
the Classic win gate, breaking Diamond-41 (CONTEXT D-14, RESEARCH Pitfall 1):

```kotlin
/** A solved board leaves exactly one peg; a Challenge board that did not is stuck. */
const val PEGS_REMAINING_SOLVED = 1
```

The tier mapping at `ResultsViewModel.kt:201-212` is the existing outcome-classification function and
shows the shape the eligibility predicate should take (a small `when` with a KDoc'd rationale), while
also being the place where "SOLVED" copy and the D-14 gate use different words for the same result:

```kotlin
fun tierFor(stars: Int, pegsRemaining: Int, challengeLevel: Int?): ResultsHeadlineTier =
    when {
        challengeLevel != null && pegsRemaining != PEGS_REMAINING_SOLVED -> ResultsHeadlineTier.STUCK
        stars >= ScoreParams.THREE_STARS -> ResultsHeadlineTier.PERFECT
        stars == ScoreParams.TWO_STARS -> ResultsHeadlineTier.SOLVED
        else -> ResultsHeadlineTier.STUCK
    }
```

**Detekt note:** `GameViewModel.kt:943-946` records a function restructured specifically to stay inside
detekt's `ReturnCount` limit. Keep the new gate functions small.

---

### `app/.../ui/results/ResultsUiState.kt` (UI state model)

**Analog:** itself. Two existing fields are the templates for `adScrimVisible` and `pendingNavigation`.

**One-shot cue as state, KDoc'd for its reach** (`ResultsUiState.kt:50` + the property KDoc at `:32-38`):

```kotlin
val playCelebration: Boolean = false,
```

> `@property playCelebration` whether THIS entry into Results should sound the star-pop
> celebration (D-16). Like [newBests] it is a one-shot cue a recomposition cannot re-fire, but its
> scope is wider than one composition: it is `true` only on the first construction for a given
> `SavedStateHandle` ...

**Derived boolean over stored state** (`ResultsUiState.kt:58-59`):

```kotlin
val showNextLevel: Boolean
    get() = challengeLevel != null && pegsRemaining == PEGS_REMAINING_SOLVED
```

The `@Immutable` annotation + all-`val` discipline at `:40-41` is required for Compose skipping.

**RESEARCH Pitfall 6 constraint:** the navigation effect must be a `StateFlow` **field** on this state
(`pendingNavigation: NavTarget?`), consumed once and cleared through the VM — not a `Channel`/`SharedFlow`
emission, which `collectAsStateWithLifecycle` may drop while the host is STOPPED during an ad.

---

### `app/.../ui/results/ResultsRoute.kt` + `ResultsActions.kt` + `ResultsScreen.kt` (route + leaves)

**Analog for the one-shot effect:** `app/.../ui/game/GameRoute.kt:60-73` — the existing "consume once,
navigate once" idiom (note: it uses a Channel-backed flow, which Pitfall 6 says *not* to copy for the
ad path; copy the `LaunchedEffect`-keyed *shape*, back it with a state field):

```kotlin
LaunchedEffect(Unit) {
    viewModel.finished.collect { result ->
        onFinished(
            Results(
                levelId = viewModel.levelId,
                stars = result.stars,
                ...
            ),
        )
    }
}
```

**The current pass-through to replace** (`ResultsRoute.kt:38-50`) — `ResultsViewModel` injects no gateway
and owns no navigation timing at all today:

```kotlin
val viewModel: ResultsViewModel = hiltViewModel()
val state by viewModel.uiState.collectAsStateWithLifecycle()

ResultsScreen(
    state = state,
    onReplay = onReplay,
    onCatalog = onCatalog,
    onHome = onHome,
    onNextLevel = onNextLevel,
    onRetry = onRetry,
    onNavigateBack = onNavigateBack,
    modifier = modifier,
)
```

**The synchronous `onClick` invocations to defer** (`ResultsActions.kt:53-63`, `:78`, `:89-90`) — these
are the two D-06 ad-eligible surfaces (Challenge "Next level", Classic "Catalog"):

```kotlin
} else if (state.showNextLevel) {
    PrimaryAction(
        textRes = R.string.action_next_level,
        onClick = { onNextLevel(challengeLevel + 1) },
    )
}
...
OutlinedButton(
    onClick = withClickSound(SoundCue.ButtonForward, onCatalog),
```

`ResultsActions.kt:79-88` carries the standing instruction about the click cue: it is attached
per-callsite and **never** to `Modifier.claySquish()`, which is shared with the top-bar back buttons,
the hint toggle and the Settings switches (RESEARCH Pitfall 7 — do not "fix" the early-cue perception by
moving it).

**Scrim rendering pattern** (`app/.../ui/onboarding/OnboardingOverlay.kt:61-68`) — the repo's only
full-screen dim, using the M3 `colorScheme.scrim` token (no custom color needed, and it keeps the
UI-SPEC S3 one-accent discipline intact):

```kotlin
Box(
    modifier =
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.scrim)
            .padding(dimensionResource(R.dimen.spacing_lg)),
    contentAlignment = Alignment.Center,
) {
```

**Input-lock-during-in-flight pattern** (`AdOfferDialog.kt:98-113`) — the precedent that a lock is a
*correctness* device, not a UX nicety:

```kotlin
Dialog(
    onDismissRequest = onDismiss,
    properties =
        if (loading) {
            // D-10 / T-09-11: the request is already in flight, so no player input may
            // tear the dialog down underneath a returning Granted. Bounded by
            // AdConstants.ACCEPT_WAIT_MS, so the lock can never dead-end.
            DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
            )
        } else {
            DialogProperties(usePlatformDefaultWidth = false)
        },
)
```

Note: `BackHandler` and `awaitPointerEvent` consumption appear **nowhere** in `src/main` today — the
back-swallow half of INT-04 has no in-repo analog and follows RESEARCH § Pattern 5.

**Test-tag + semantics convention for the new scrim** (`ResultsScreen.kt:47-60`):

```kotlin
/** Test tag of the star row; carries the [EarnedStarCountKey] semantics for the screen test. */
const val RESULTS_STAR_ROW_TEST_TAG = "results_star_row"

val EarnedStarCountKey = SemanticsPropertyKey<Int>("EarnedStarCount")
```

**Dimens token pattern** (`app/src/main/res/values/dimens.xml:112-126`) — every new token carries a
comment naming its owner and explaining why it is not an existing same-valued token:

```xml
<!--
  Phase-9 token: the gold reward glyph at the top of the rewarded-ad opt-in dialog
  (AdOfferDialog, ADS-01). NOT results_star_size, which is also 48dp: that token
  means "a Results star" and must stay free to move without dragging the ad dialog
  with it. Same value today, different owner.
-->
<dimen name="ad_offer_glyph_size">48dp</dimen>
```

---

### DI wiring (`di/PlatformModule.kt` / `platform/ads/di/AdsModule.kt`)

**Analog:** `di/PlatformModule.kt:81-91`.

```kotlin
@Binds
@Singleton
abstract fun bindAdGateway(impl: AdMobAdGateway): AdGateway
```

Nothing new is likely to be needed: `RewardedAdSlot` required **no** module entry because it takes only
an application `CoroutineScope` and exposes no SDK type (`AdsModule.kt:43-52`). `InterstitialAdSlot`
and `InterstitialCadenceStore` follow the same `@Inject constructor` + `@Singleton`-on-the-class route.
`DataModule.kt:64-89` is the precedent if an instance `@Provides` is genuinely needed (it must live on a
companion object of the abstract module).

---

### Tests

**`domain/src/test/.../ads/InterstitialCadenceStateTest.kt`** — analog
`domain/src/test/kotlin/com/app/pegsolitaire/domain/assist/AssistAllowanceTest.kt`.

Class-KDoc convention (`AssistAllowanceTest.kt:6-15`) — assert against the *constants*, never bare
literals, because a test literal is a second definition that can drift:

```kotlin
/**
 * Every expectation about a *limit* is written as [AssistAllowanceLimits.FREE_HINTS] or
 * [AssistAllowanceLimits.FREE_UNDOS], never as a bare `3`/`5` ... Bare literals
 * used as arbitrary *stored* values (an in-range `4`, a forged `9999`) are fine: they are
 * fixtures, not restatements of the policy.
 */
```

Test shape — plain JUnit4 + Truth, no runner, no scaffolding (`AssistAllowanceTest.kt:41-48`):

```kotlin
@Test
fun spendingAHintLeavesTheUndoCounterUntouched() {
    // D-01: the two counters are independent, not one shared pool.
    val spent = AssistAllowance().spend(AssistKind.HINT)

    assertThat(spent.hintsRemaining).isEqualTo(AssistAllowanceLimits.FREE_HINTS - 1)
    assertThat(spent.undosRemaining).isEqualTo(AssistAllowanceLimits.FREE_UNDOS)
}
```

Tamper/edge cases (`AssistAllowanceTest.kt:104-139`) — `absentStoredCountersFallOpen…`,
`storedZeroesArePreserved…`, `outOfRangeStoredCounterIsRejected…`, `negativeStoredCounters…` are the
exact four cases the cadence store's read path needs.

**`app/src/test/.../data/InterstitialCadenceStoreTest.kt`** — analog
`app/src/test/java/com/app/pegsolitaire/data/ChallengeProgressTest.kt`.

Harness (`ChallengeProgressTest.kt:32-33`, `:148-168`) — a real temp-file store plus two hostile doubles:

```kotlin
@get:Rule
val tempFolder = TemporaryFolder()

private fun realDataStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        produceFile = { tempFolder.newFile(TEST_STORE_FILE) },
    )

private fun throwingDataStore(): DataStore<Preferences> =
    object : DataStore<Preferences> {
        override val data: Flow<Preferences> = flow { throw IOException("corrupt store") }

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            error("not used in the read path")
    }

/** Reads work (and are empty); every write fails like a full disk (T-05-13). */
private fun throwingWriteDataStore(): DataStore<Preferences> =
    object : DataStore<Preferences> {
        override val data: Flow<Preferences> = flowOf(emptyPreferences())

        override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
            throw IOException("disk full")
    }
```

"Survives process death" is simulated by constructing a **second holder over the same store**
(`ChallengeProgressTest.kt:46-57`):

```kotlin
// "Restart": a brand-new holder over the same store must see the persisted level.
val restarted = ChallengeProgress(store, backgroundScope)
assertThat(restarted.currentLevel.first { it == SECOND_LEVEL }).isEqualTo(SECOND_LEVEL)
```

Fail-open + dropped-write cases at `:114-146` map one-to-one onto the cadence store's INT-02 tests.

**`app/src/test/.../ui/results/FakeAdGateway.kt`** (new) — analogs
`app/src/test/java/com/app/pegsolitaire/ui/settings/FakeConsentGateway.kt` (the shared-fake convention +
counter properties with `private set`) and `GameViewModelAdClockTest.kt:464-489` (the gated-suspend
`AdGateway` fake). The latter is the closest structural match:

```kotlin
private class AdClockGatedAdGateway : AdGateway {
    private val gate = CompletableDeferred<Unit>()

    var started = 0
        private set

    var proceeded = 0
        private set

    override val rewardedOfferable: StateFlow<Boolean> = MutableStateFlow(true).asStateFlow()

    override fun preloadRewarded(reason: AdReason) = Unit

    fun release() {
        gate.complete(Unit)
    }

    override suspend fun requestRewarded(reason: AdReason): RewardOutcome {
        started++
        gate.await()
        proceeded++
        return RewardOutcome.Granted
    }

    override fun showInterstitial() = Unit
}
```

`FakeConsentGateway.kt:9-27` documents when a fake is promoted to a shared top-level file rather than
staying `private` in one test class — the new fake is needed by `ResultsViewModelTest` and
(potentially) `ResultsScreenTest`, so it qualifies.

**`ResultsViewModelTest.kt` (extend)** — the existing harness at `:45-59` and the factory at `:466-509`:

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
@OptIn(ExperimentalCoroutinesApi::class) // runCurrent virtual-time control
class ResultsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
```

```kotlin
private fun viewModelFor(
    levelId: String,
    stars: Int,
    pegsRemaining: Int = SAMPLE_PEGS_REMAINING,
    ...
    repository: ProgressRepository = FakeProgressRepository(),
): ResultsViewModel =
    ResultsViewModel(
        savedStateHandle = savedStateHandleFor(...),
        progressRepository = repository,
    )
```

Every new constructor dependency (cadence policy/store, gateway) needs a default here so existing tests
keep compiling.

**`ResultsScreenTest.kt` (extend)** — harness at `:36-42`, assertion style at `:44-84` (Robolectric +
`createComposeRule`, semantics/text assertions, never pixels).

**`NoOpAdGatewayTest.kt` (extend)** — `:39-48`. The `rewardedOfferableIsTrue…` test's KDoc (`:29-38`)
is the template for pinning the *opposite* polarity on the interstitial members ("if a future change
'corrects' this, this test is the thing that explains why it must not").

---

## Shared Patterns

### Scoped constants (QUAL-03, no magic numbers)

**Source:** `domain/.../assist/AssistAllowance.kt:23-44`, `domain/.../scoring/ScoreParams.kt:1-11`,
`platform/ads/AdConstants.kt:1-16`, `data/ProgressPersistenceKeys.kt:1-18`
**Apply to:** `InterstitialCadenceParams`, the new DataStore keys object, any new dimens token

Each object's class KDoc states its *remit* and names the sibling it sits beside. The remit statement is
load-bearing here: `AdConstants.kt:13-15` limits that object to ad timing, which is why the D-01..D-04
cadence values may not go in it.

```kotlin
/**
 * Scoped, not global: this object owns ad timing and only ad timing, as
 * [com.app.pegsolitaire.audio.SoundParams] does for audio.
 */
```

### Fail-open / fail-safe storage

**Source:** `data/StorageFailSafe.kt:22-32`
**Apply to:** every write in `InterstitialCadenceStore`

```kotlin
internal suspend fun <T> storageFailSafe(
    fallback: T,
    block: suspend () -> T,
): T =
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (ignored: Exception) {
        fallback
    }
```

`CancellationException` is deliberately rethrown. Note the direction inversion for this phase: the
default cadence state (`firstSessionComplete = false`) means a corrupt read yields **no interstitial** —
a corrupt read costs revenue, never goodwill.

### SDK containment (`PlatformBoundaryTest`)

**Source:** `platform/ads/di/AdsModule.kt:20-52` and `di/PlatformModule.kt:44-63` (the same warning,
written twice, with the empirical Phase-9 verification)
**Apply to:** `InterstitialAdSlot`, `AdMobAdGateway`, every DI edit

```kotlin
/**
 * `PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks` reads **bytecode**, and the class
 * it catches is Hilt's generated `DaggerApp_HiltComponents_SingletonC`, which lives in the
 * **root** package `com.app.pegsolitaire` — outside `..platform..`. Declaring an SDK-typed
 * binding anywhere puts a `Provider<ThatSdkType>` field on the generated component ...
 *
 * The fix is to keep SDK types out of the graph: a platform class constructs its own SDK
 * handle behind its own seam, and only the seam interface is bound.
 */
```

### Domain purity

**Source:** `app/src/test/java/com/app/pegsolitaire/DomainPurityTest.kt:23-31`
**Apply to:** everything under `domain/.../ads/`

```kotlin
@ArchTest
val domainDoesNotDependOnAndroid: ArchRule =
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("android..", "androidx..")
```

This is what turns "the cadence policy must be pure Kotlin" (ROADMAP) into a build gate, and it is why
`nowMs` is a parameter rather than a read of `SystemClock`.

### Build gates that fail late

**Source:** `build-logic/src/main/kotlin/standards.gradle.kts:11-17`, `:40-46`, `:176-178`
**Apply to:** the scrim composable (dp literals), any ad-ID handling

```kotlin
// Matches a numeric literal immediately converted to dp/sp (e.g. 12.dp, 8.sp,
// 4f.dp). Legitimate sizing flows through dimensionResource(...) / a Dimens
// accessor, which never produces a `<number>.dp` token.
val dpSpRegex = Regex("""(?<![\w.])\d+(?:\.\d+)?[fF]?\.(?:dp|sp)\b""")
```

```kotlin
tasks.named("check").configure {
    dependsOn("detekt", "ktlintCheck", dpSpLiteralCheck, adIdLiteralCheck, audioFocusLiteralCheck)
}
```

The `dpSpLiteralCheck` scan is restricted to files whose path contains `/ui/`, so the scrim composable is
in scope; alpha `Float`s are not (`ResultsScreen.kt:372` keeps `STAR_GLOW_ALPHA = 0.35f` as a private
const — that is the sanctioned shape for a scrim alpha).

### No logging in the ad path

**Source:** `AdMobAdGateway.kt:51-55` (and `UmpConsentGateway`'s equivalent)
**Apply to:** `InterstitialAdSlot`, the interstitial show path, the cadence store

> **Nothing here logs.** Ad response info, consent status and the TCF string are all
> personal-data-adjacent (ASVS V7), so no diagnostic call may name them — which is why this
> paragraph ... avoids spelling any logging API out.

---

## No Analog Found

| File / concern | Role | Data Flow | Reason |
|----------------|------|-----------|--------|
| Back-press swallowing on the scrim (`BackHandler`) | UI leaf | event-driven | `BackHandler` appears **nowhere** under `app/src/main`. Use RESEARCH § Pattern 5. |
| Pointer-event consumption on a full-size overlay (`awaitPointerEvent(PointerEventPass.Initial)`) | UI leaf | event-driven | No in-repo precedent — `pointerInput` is used only for tap/drag detection (`BoardCanvas.kt:206`, `Clay.kt:164`), never for swallowing. Use RESEARCH § Pattern 5. |
| `ProcessLifecycleOwner` observation | platform observer | event-driven | The repo's only app-lifecycle hook is Activity-callback based (`DefaultForegroundActivityProvider`), which RESEARCH Pitfall 3 rules out for this use. The `@Singleton`/`attach()` *shell* transfers; the lifecycle API does not. |

---

## Metadata

**Analog search scope:** `domain/src/main`, `domain/src/test`, `app/src/main/java`, `app/src/test/java`,
`app/src/main/res/values`, `gradle/`, `build-logic/`
**Files read in full:** 28 (source), 3 (partial, targeted ranges: `GameViewModel.kt`,
`ResultsViewModelTest.kt`, `GameViewModelAdClockTest.kt`)
**Pattern extraction date:** 2026-09-02
