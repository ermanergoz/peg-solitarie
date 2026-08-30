# Phase 9: Consent & Rewarded Ads - Pattern Map

**Mapped:** 2026-08-31
**Files analyzed:** 31 (14 new, 17 modified)
**Analogs found:** 29 / 31

Sources: `09-CONTEXT.md` (D-01..D-19), `09-RESEARCH.md` (Recommended Project Structure §, Patterns 1-9), `09-UI-SPEC.md` (§Interaction Contract).

**Reading rule for the planner:** every excerpt below is real, current repo code. Where a pattern conflicts with RESEARCH.md's sketch, the repo wins on *shape* (KDoc density, `@Inject constructor` indentation style, naming) and RESEARCH.md wins on *SDK API surface*.

---

## File Classification

### New files

| New file | Role | Data Flow | Closest Analog | Match Quality |
|----------|------|-----------|----------------|---------------|
| `domain/src/main/kotlin/com/app/pegsolitaire/domain/assist/AssistAllowance.kt` | model + policy (pure) | transform | `domain/scoring/ScoreCalculator.kt` + `ScoreParams.kt` | exact (mandated by Phase 6 D-04) |
| `app/.../platform/ads/consent/AdsReadiness.kt` | model (sealed state) | event-driven | `platform/games/GamesSignInState.kt` | exact |
| `app/.../platform/ads/consent/ConsentGateway.kt` | service seam (interface) | event-driven | `platform/games/GamesService.kt` | exact |
| `app/.../platform/ads/consent/UmpConsentGateway.kt` | service impl (@Singleton) | event-driven | `platform/activity/DefaultForegroundActivityProvider.kt` | exact |
| `app/.../platform/ads/AdMobAdGateway.kt` | service impl | request-response | `platform/games/NoOpGamesService.kt` (shape) + `platform/ads/NoOpAdGateway.kt` (contract) | role-match |
| `app/.../platform/ads/RewardedAdSlot.kt` | service (single-slot cache) | request-response | `platform/activity/DefaultForegroundActivityProvider.kt` (MutableStateFlow + await) | partial |
| `app/.../platform/ads/AdConstants.kt` | config (scoped constants) | — | `audio/SoundParams.kt` | exact |
| `app/.../platform/ads/di/AdsModule.kt` | config (Hilt module) | — | `di/DataModule.kt` (companion `@Provides`) | exact |
| `app/.../ui/game/AdOfferDialog.kt` | component (stateless leaf) | request-response | `ui/onboarding/OnboardingOverlay.kt` (clay surface + stacked actions) + `ui/game/NoWinBanner.kt` (test tag, KDoc) | exact |
| `app/.../ui/components/ClaySnackbarHost.kt` *(name is Claude's discretion)* | component (stateless leaf) | event-driven | `ui/game/NoWinBanner.kt` | role-match |
| `app/.../ui/settings/PrivacyLinks.kt` | config (scoped constants) | — | `audio/SoundParams.kt` / `data/resume/ResumeSnapshotFormat` | exact |
| `domain/src/test/kotlin/.../assist/AssistAllowanceTest.kt` | test (pure JVM) | — | `domain/scoring/ScoreCalculatorTest.kt` | exact |
| `app/src/test/.../ui/game/GameViewModelAllowanceTest.kt` | test (VM, virtual time) | — | `ui/game/GameViewModelTest.kt` (factory + `FakeAdGateway`) | exact |
| `app/src/test/.../ui/game/GameViewModelAdClockTest.kt` | test (VM, virtual time) | — | `ui/game/GameViewModelTest.kt` (`GatedAdGateway` + `TestTimeSource`) | exact |
| `app/src/test/.../ui/game/AdOfferDialogTest.kt` | test (Compose/Robolectric) | — | `ui/game/NoWinBannerTest.kt` | exact |
| `app/src/test/.../ui/settings/SettingsPrivacyTest.kt` | test (Compose + VM) | — | `ui/game/NoWinBannerTest.kt` + `ui/settings/SettingsViewModelTest.kt` | exact |

### Modified files

| Modified file | Role | Data Flow | Pattern source (itself unless noted) | Match Quality |
|---------------|------|-----------|--------------------------------------|---------------|
| `app/.../platform/ads/AdGateway.kt` | service seam | request-response | itself + `platform/games/GamesService.kt` (`val session: StateFlow<…>`) | exact |
| `app/.../platform/ads/NoOpAdGateway.kt` | service stub | request-response | `platform/games/NoOpGamesService.kt` (StateFlow in a stub) | exact |
| `app/.../di/PlatformModule.kt` | config | — | itself (lines 46-56) | exact |
| `app/.../MainActivity.kt` | activity host | event-driven | `App.kt` (inject-the-interface + `onCreate` ordering) | exact |
| `app/src/main/AndroidManifest.xml` | config | — | itself (the `<provider tools:node="remove">` documentation style) | exact |
| `app/.../data/resume/ResumeSnapshotDto.kt` | model (DTO) | file-I/O | itself — the `minimumPegs: Int? = null` precedent | exact |
| `app/.../data/resume/SnapshotMapper.kt` | service (codec) | file-I/O | itself — `resolveMinimumPegsOrNull` | exact |
| `app/.../ui/game/GameUiState.kt` | model (UiState) | — | itself — `confirmRestart` + the four `*Tick` ordinals | exact |
| `app/.../ui/game/GameViewModel.kt` | viewmodel | request-response | itself — `onUndo`/`onHintRequested`/ticker/`resetToInitial` | exact |
| `app/.../ui/game/GameScreen.kt` | screen (stateless) | — | itself — `RestartConfirmDialog` gating + `UndoSound` tick idiom | exact |
| `app/.../ui/game/GameRoute.kt` | route | — | itself (lines 75-84) | exact |
| `app/.../ui/settings/SettingsUiState.kt` | model (UiState) | — | itself | exact |
| `app/.../ui/settings/SettingsViewModel.kt` | viewmodel | event-driven | itself — the `init { viewModelScope.launch { …collect } }` block | exact |
| `app/.../ui/settings/SettingsScreen.kt` | screen (stateless) | — | itself — `SettingsToggleRow` is the literal peer of the new `SettingsLinkRow` | exact |
| `app/.../ui/settings/SettingsRoute.kt` | route | — | itself (lines 21-36) | exact |
| `app/src/main/res/values/{strings,dimens}.xml` | config | — | themselves (grouped, commented blocks) | exact |
| `app/src/test/.../data/resume/SnapshotMapperTest.kt`, `.../platform/ads/NoOpAdGatewayTest.kt` | test | — | themselves | exact |

---

## Pattern Assignments

### `domain/.../domain/assist/AssistAllowance.kt` (model + policy, pure)

**Analog:** `domain/src/main/kotlin/com/app/pegsolitaire/domain/scoring/ScoreCalculator.kt` + `ScoreParams.kt`
**Note:** the package `com.app.pegsolitaire.domain.assist` **already exists** (`OpeningBook.kt`, `OpeningBookFormat.kt`) — this is a new file in an existing package, not a new package.

**Constants-object pattern** — `ScoreParams.kt:11-26, 38-50`. Every value is a KDoc'd `const val` naming the decision that fixes it; derived values are *expressions of other constants*, never re-typed literals:

```kotlin
object ScoreParams {
    /**
     * The DEFAULT "perfect finish" peg target (D-01), used by [ScoreCalculator.stars]
     * ... At or below this count the board scores 3 stars ...
     */
    const val THREE_STAR_MAX_PEGS = 1

    /**
     * DERIVED from [THREE_STAR_MAX_PEGS] + [TWO_STAR_BAND_WIDTH] rather than written
     * as a standalone `3`, so the boundary and its anchor cannot drift apart.
     */
    const val TWO_STAR_MAX_PEGS = THREE_STAR_MAX_PEGS + TWO_STAR_BAND_WIDTH
}
```

→ `AssistAllowanceLimits.FREE_HINTS = 3` / `FREE_UNDOS = 5` (D-02) take exactly this shape. The UI-SPEC formats the dialog copy from these two constants (`%1$d`), so they are named **once** in the whole codebase.

**Pure-policy pattern** — `ScoreCalculator.kt:22-53`. A top-level `object` of total pure functions, zero `android.*`, every tunable read from the constants object:

```kotlin
object ScoreCalculator {
    fun stars(
        pegsRemaining: Int,
        minimumPegs: Int = ScoreParams.THREE_STAR_MAX_PEGS,
    ): Int =
        when {
            pegsRemaining <= minimumPegs -> ScoreParams.THREE_STARS
            pegsRemaining <= minimumPegs + ScoreParams.TWO_STAR_BAND_WIDTH -> ScoreParams.TWO_STARS
            else -> ScoreParams.MIN_STARS
        }
}
```

**Totality / clamping discipline** — `audio/SoundParams.kt:190-197`. The repo's stated rule for a value "derived from gameplay state and not range-checked by its type":

```kotlin
    /**
     * `runIndex` is derived from gameplay state and is not range-checked by its type, so
     * the [coerceIn] is a correctness requirement, not defensive noise (T-07-02 / ASVS
     * V5): it makes the function total for every `Int`.
     */
    fun jumpRate(runIndex: Int): Float = (JUMP_BASE_RATE * SEMITONE_STEP.pow(runIndex)).coerceIn(MIN_RATE, MAX_RATE)
```

→ `spend()` clamps at 0 (`coerceAtLeast(0)`); `fromStoredOrFull()` **rejects** out-of-range instead of clamping (see the codec pattern below — different input, different rule, and the difference is deliberate).

---

### `app/.../platform/ads/consent/AdsReadiness.kt` (model, sealed 3-state)

**Analog:** `app/src/main/java/com/app/pegsolitaire/platform/games/GamesSignInState.kt` — the repo already ships a three-member sealed platform-state type with an `Unknown` "no answer yet" member.

**Core pattern** (`GamesSignInState.kt:3-35`) — note the KDoc bullet list explaining each member's *semantics for observers*, and the explicit "sealed rather than enum, because…" justification the repo requires:

```kotlin
/**
 * The player's Play Games sign-in state, observed through [GamesService.session] (PLAT-03).
 *
 * - [Unknown]: no sign-in check has completed yet. The state *before* the first answer
 *   arrives, not a failed one — a surface seeing it should commit to neither a
 *   signed-in nor a signed-out affordance.
 * ...
 * Sealed rather than an enum because [SignedIn] carries a payload the other two members
 * have no meaning for; [com.app.pegsolitaire.domain.model.GameStatus] stays an `enum`
 * for exactly the opposite reason.
 */
sealed interface GamesSignInState {
    /** No sign-in check has completed yet — the pre-first-check state, not a failure. */
    data object Unknown : GamesSignInState

    /** The player is not connected; hide every Play-Games-dependent affordance. */
    data object SignedOut : GamesSignInState
    ...
}
```

→ `AdsReadiness.{Unknown, Ready, Unavailable}`. **All three members are payload-free**, so if the planner keeps it that way the "sealed rather than enum" justification above does *not* apply — either make it an `enum class` (matching `GameStatus`/`AdReason`) or write a different justification. Do not copy the sentence unchanged. RESEARCH Pitfall 16 requires `Unknown` to be treated as **not offerable**; document that on the member itself, as `GamesSignInState.Unknown` does.

---

### `app/.../platform/ads/consent/ConsentGateway.kt` (service seam, interface)

**Analog:** `app/src/main/java/com/app/pegsolitaire/platform/games/GamesService.kt`

**Seam-interface pattern** (`GamesService.kt:26-45`) — an observable `StateFlow` property plus SDK-free method signatures, with the KDoc stating the boundary guarantee explicitly:

```kotlin
interface GamesService {
    /**
     * The player's current sign-in state, [GamesSignInState.Unknown] until the first
     * check completes. Observable so a surface reacts to sign-in resolving
     * asynchronously rather than holding a stale answer from a single [signIn] call.
     */
    val session: StateFlow<GamesSignInState>

    /**
     * ... What the swap does guarantee is that the dependency stops there — this
     * interface names no Activity type and no Play Games SDK type, so no ViewModel can
     * obtain one through it.
     */
    suspend fun signIn(): Boolean
```

→ `ConsentGateway.readiness: StateFlow<AdsReadiness>`, `privacyOptionsRequired: StateFlow<Boolean>`, `fun bootstrap()`, `suspend fun showPrivacyOptions()`. **No `Activity` in any signature** — `showPrivacyOptions()` resolves it internally via `ForegroundActivityProvider`, exactly as `GamesService.showLeaderboard` (lines 74-81) documents.

**Existing seam KDoc to update, not just extend** — `AdGateway.kt:3-13` already forward-references this phase:

```kotlin
/**
 * ... Today it is fulfilled by an auto-grant
 * [NoOpAdGateway] stub so hint/undo are free. Swapping the Hilt binding
 * (`PlatformModule`) for a real ad-backed implementation is necessary but **not
 * sufficient**: ... Phase 9 additionally puts an opt-in dialog in
 * front of any ad that plays — so the swap does come with UI work.
 */
```

---

### `app/.../platform/ads/consent/UmpConsentGateway.kt` (service impl, @Singleton)

**Analog:** `app/src/main/java/com/app/pegsolitaire/platform/activity/DefaultForegroundActivityProvider.kt` — the repo's only stateful `@Singleton` platform implementation.

**Class shell** (`DefaultForegroundActivityProvider.kt:38-50`) — note the exact `@Inject constructor()` line-break/indent style ktlint enforces here:

```kotlin
@Singleton
class DefaultForegroundActivityProvider
    @Inject
    constructor() :
    ForegroundActivityProvider,
        Application.ActivityLifecycleCallbacks {
        private val resumed = MutableStateFlow<WeakReference<Activity>?>(null)

        override val current: Activity? get() = resumed.value?.get()

        override suspend fun awaitActivity(): Activity = resumed.mapNotNull { it?.get() }.first()
```

**The `@Singleton`-on-the-class rule** (`DefaultForegroundActivityProvider.kt:19-31`) — load-bearing for `UmpConsentGateway`, which holds `_readiness` and the `AtomicBoolean` init guard:

```kotlin
/**
 * **This class carries `@Singleton`, and that is not a stylistic slip.**
 * `@Binds @Singleton fun bind(impl: X): Y` scopes the binding of *`Y`*, not of `X` — so
 * if anything injected the concrete type it would get a **second, unscoped instance**
 * that is never fed a lifecycle event ... `NoOpAdGateway` and
 * `NoOpGamesService` deliberately do **not** carry `@Singleton` on the class, which is
 * correct for them because they are stateless.
 */
```

→ `UmpConsentGateway` and `AdMobAdGateway` are **stateful** → `@Singleton` on the class. Every injection site (`MainActivity`, `SettingsViewModel`) declares the **interface**.

**App-lifetime scope injection** — `di/DataModule.kt:86-89` + `App.kt:54-56`. `UmpConsentGateway`'s `bootstrap()` needs a scope; the repo's is:

```kotlin
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
```

and at the injection site the `@field:` qualifier form is mandatory (`App.kt:51-56`):

```kotlin
    // `@field:` is required, not stylistic: a qualifier with no declared `@Target` lands on
    // the Kotlin *property* by default, where Dagger's field injection cannot see it ...
    @Inject
    @field:ApplicationScope
    lateinit var appScope: CoroutineScope
```

(In a constructor-injected `@Singleton` the form is `@ApplicationScope private val scope: CoroutineScope` — the `@field:` note applies only to `lateinit var` field injection.)

---

### `app/.../platform/ads/AdMobAdGateway.kt` + `RewardedAdSlot.kt` (service impl, request-response)

**Contract analog:** `platform/ads/NoOpAdGateway.kt:14-20` (what it replaces)
**Shape analog:** `platform/games/NoOpGamesService.kt:27-34` (StateFlow-exposing platform impl)

```kotlin
class NoOpGamesService
    @Inject
    constructor() : GamesService {
        private val signedOutSession = MutableStateFlow<GamesSignInState>(GamesSignInState.SignedOut)

        override val session: StateFlow<GamesSignInState> = signedOutSession.asStateFlow()
```

→ `rewardedOfferable: StateFlow<Boolean>` is exposed with `asStateFlow()`; the mutable flow never leaks. Same idiom in `SettingsViewModel.kt:41-44`.

**Activity capture pattern** — `ForegroundActivityProvider.kt:25-42`, whose KDoc is the direct instruction for `show()`:

```kotlin
    /**
     * **Deliberately `null` while a fullscreen ad is on screen.** Showing an ad pauses
     * the host Activity, and the implementation clears the reference on pause — that is
     * the safer default, and callers honour it by capturing the Activity *before*
     * calling `show()`.
     */
    val current: Activity?

    /**
     * Callers MUST apply their own timeout: a backgrounded app may never resume, and an
     * ad request that waits forever leaves the assist control spinning with no way out.
     */
    suspend fun awaitActivity(): Activity
```

→ `withTimeoutOrNull(AdConstants.ACTIVITY_WAIT_MS) { activityProvider.awaitActivity() } ?: return RewardOutcome.Unavailable`. RESEARCH Pattern 7 has the verified GMA Next-Gen call sequence; RESEARCH Pitfall 13 restates this.

**Ad-unit ID access — the only sanctioned route** (`AdIds.kt:20-38`):

```kotlin
 * 3. **Ad IDs are a deliberate, documented exception to the no-magic-strings rule
 *    (QUAL-03).** Where the project's convention would otherwise call for an
 *    `AdConstants` scoped-constants object ... writing one here would be actively wrong ...
 *    The build gate `adIdLiteralCheck` fails the build on any ad-ID
 *    literal under `src/main` precisely to stop that object from ever being written.
object AdIds {
    /** Rewarded ad unit — hint / undo / retry (ADS-03). */
    val rewardedAdUnitId: String get() = BuildConfig.REWARDED_AD_UNIT_ID
}
```

→ `AdRequest.Builder(AdIds.rewardedAdUnitId)`. `AdConstants.kt` holds **timeouts and retry cadence only** — never an ID. Note `AdIds.kt`'s KDoc names `AdConstants` as the thing that must not hold IDs; when `AdConstants.kt` is created, update that sentence so it reads as "AdConstants exists but holds no IDs" rather than "AdConstants must never be written".

**`AdConstants.kt` shape** — `audio/SoundParams.kt:5-20` is the template for a non-dimen scoped constants object:

```kotlin
/**
 * Every value here is unitless — ... None of them is a dp/sp
 * measurement, so `dimens.xml` (which QUAL-03 reserves for UI dimensions, and which
 * `dpSpLiteralCheck` enforces) is the wrong home. They live in this scoped constants
 * object instead, exactly as [com.app.pegsolitaire.ui.game.GameAnim] and
 * [com.app.pegsolitaire.ui.theme.ClayAnim] do for motion timings. Scoped, not global:
 * this object owns audio and only audio.
 */
object SoundParams {
```

→ `AdConstants` owns `ACCEPT_WAIT_MS` (~5 s, D-10), `ACTIVITY_WAIT_MS`, preload retry cadence. Millisecond `Long`s follow `GameViewModel.kt:1052-1057`'s `const val TIMER_TICK_MILLIS = 1_000L` underscore-separated style.

---

### `app/.../platform/ads/NoOpAdGateway.kt` (MODIFIED — service stub)

**Analog:** itself (`NoOpAdGateway.kt:5-19`) + `NoOpGamesService.kt:8-26`

The existing KDoc **explicitly forbids what this phase must do**, and RESEARCH Pitfall 6 flags it. The current text:

```kotlin
/**
 * ... The grant is deliberately not
 * configurable (D-02): this is the behavior that keeps hint and undo identical to v1.0
 * while the seam around it is rebuilt, and a stub that could decline would change debug
 * behavior inside a phase whose contract is "nothing a player can see changes".
 */
```

→ The plan task **must rewrite this paragraph**, scoping D-02 to Phase 6 and stating that Phase 9 is where it changes. The stub gains `rewardedOfferable = MutableStateFlow(true)` (Pitfall 6: a `false` default makes D-12 suppress the dialog and the stub-first ordering proves nothing).

`NoOpGamesService.kt:13-21` is the in-repo model for a stub KDoc that *contrasts itself against the other stub* — extend that comparison rather than replacing it:

```kotlin
 * Unlike [com.app.pegsolitaire.platform.ads.NoOpAdGateway], which auto-grants ...
 * this stub deliberately *declines*. ... The ad seam needs a third, fail-open answer
 * because a missing ad would otherwise cost the player an assist (ADS-03); a missing
 * leaderboard costs them nothing.
```

**Test to extend:** `app/src/test/.../platform/ads/NoOpAdGatewayTest.kt:14-33` — three tiny `runTest` cases; add one asserting `rewardedOfferable.value` is `true`.

---

### `app/.../platform/ads/di/AdsModule.kt` (config, Hilt module) — NEW, inside `..platform..`

**Analog:** `di/DataModule.kt:37-90` (the `abstract class` + `companion object { @Provides }` combination), `di/PlatformModule.kt:43-57` (`@Binds` shape)

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindLevelRepository(impl: DefaultLevelRepository): LevelRepository

    /**
     * Instance `@Provides` cannot live on an abstract `@Module`, so the single shared
     * `app_prefs` `DataStore<Preferences>` ... is provided here on the module's
     * companion object.
     */
    companion object {
        @Provides
        @Singleton
        fun provideAppDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = context.appDataStore
```

**The placement rule is already written down** — `di/PlatformModule.kt:35-41`, and it is a hard build gate:

```kotlin
 * **Do not add an SDK-typed `@Provides` to this file.** When Phase 9 needs to provide a
 * `MobileAds`, UMP or Play Games type, that module belongs at `platform/ads/di/` or
 * `platform/games/di/` — i.e. inside `..platform..`. This package is *outside*
 * `..platform..`, and Hilt's generated `DaggerApp_HiltComponents_SingletonC` references
 * every binding's types, so an SDK-typed provider here would trip `PlatformBoundaryTest`'s
 * SDK-containment rule with a confusing violation naming a generated class rather than
 * the file that caused it.
```

---

### `app/.../di/PlatformModule.kt` (MODIFIED — the binding swap, LAST step)

**Analog:** itself, `PlatformModule.kt:43-57`

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    @Binds
    @Singleton
    abstract fun bindAdGateway(impl: NoOpAdGateway): AdGateway
```

→ `impl: AdMobAdGateway`, plus a new `@Binds @Singleton abstract fun bindConsentGateway(impl: UmpConsentGateway): ConsentGateway`. Both bound types are interfaces declared in `..platform..` and name no SDK type, so `PlatformModule` stays inside the boundary rules. The KDoc's "Slice-2 swap points" paragraph (lines 21-24) needs updating for the half that is no longer pending.

---

### `app/.../MainActivity.kt` (MODIFIED — consent bootstrap host)

**Analog:** `App.kt:43-62` (the injection + `onCreate` ordering discipline) and `MainActivity.kt:32-46` (itself)

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var soundPlayer: SoundPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalSoundPlayer provides soundPlayer) {
```

→ add `@Inject lateinit var consentGateway: ConsentGateway` and call `consentGateway.bootstrap()` **after** `super.onCreate()`, before `setContent`.

**The ordering rule the analog states** (`App.kt:21-25`) applies verbatim:

```kotlin
 * It is also the registrar for [ForegroundActivityProvider] (PLAT-01) ... The ordering in
 * [onCreate] is **not optional**: Hilt performs field injection into an `Application` as
 * part of `super.onCreate()`, so [activityProvider], [soundPlayer] and [appScope] are only
 * non-null once that call has returned — touching any of them before it would throw
 * `UninitializedPropertyAccessException`.
```

**Declare the interface, never the impl** — both `App.kt:32-36` and `MainActivity.kt:29-31` say so:

```kotlin
 * The declared type is the **interface**, for the same reason `App` documents: naming the
 * concrete implementation instead would build a second, unscoped player owning a second
 * `SoundPool`, which the sound-off switch could never silence.
```

**Why not `App.kt`** (RESEARCH Pitfall 3): `App.onCreate()` runs in every Robolectric test. `MainActivity` is referenced by zero tests. `MainActivity` also needs **no** `PlatformBoundaryRules` exemption, because `ConsentGateway` is not `ForegroundActivityProvider` — see `PlatformBoundaryRules.kt:65-79`, whose three name-matched exemptions must **not** be widened.

---

### `app/src/main/AndroidManifest.xml` (MODIFIED — AdMob `APPLICATION_ID` meta-data)

**Analog:** itself. The manifest's house style is a load-bearing comment above every entry explaining the decision (`AndroidManifest.xml:5-12, 31-53`):

```xml
    <!-- D-10: the app was fully offline through v1.0 and declared no permissions.
         Inert until an SDK uses it; declared explicitly so the store permission
         diff is a deliberate commit rather than an implicit manifest-merge result. -->
    <uses-permission android:name="android.permission.INTERNET" />
```

→ the new tag, inside `<application>`, referencing the **generated** string resource (never a literal — `adIdLiteralCheck` scans `src/main`):

```xml
    <meta-data
        android:name="com.google.android.gms.ads.APPLICATION_ID"
        android:value="@string/admob_app_id" />
```

**Guard:** `ManifestSurfaceTest` asserts the merged permission set by **equality** (RESEARCH Pitfall 8) — run it after this change.

---

### `app/.../data/resume/ResumeSnapshotDto.kt` (MODIFIED — allowance counters)

**Analog:** itself — the `minimumPegs: Int? = null` field is the exact precedent, added for the same backward-compat reason.

**Field + on-disk-contract KDoc** (`ResumeSnapshotDto.kt:42-67`):

```kotlin
 * @property minimumPegs The board's "perfect finish" peg target ...
 *
 *   **On-disk contract:** `null` means "written before per-board targets existed", NOT
 *   "the target is 1". ... Nullable-with-a-default is also what keeps today's saved games
 *   decodable at all: the writer's `ignoreUnknownKeys` covers the forward direction, this
 *   covers the backward one.
 */
@Serializable
data class ResumeSnapshotDto(
    ...
    val elapsedMs: Long,
    val minimumPegs: Int? = null,
)
```

→ append `val freeHintsRemaining: Int? = null` and `val freeUndosRemaining: Int? = null` with the same `null` = "predates the field" contract, resolved fail-open to a **full** allowance (which is the *opposite* of `minimumPegs`'s deliberately non-defaulting resolution — say why in the KDoc).

---

### `app/.../data/resume/SnapshotMapper.kt` (MODIFIED — encode/decode, fail-open)

**Analog:** itself, `SnapshotMapper.kt:183-209` — the reject-don't-clamp resolver:

```kotlin
    /**
     * Three cases, in order:
     *  1. **Present** — it must satisfy `in 1..positions.size`, the same invariant
     *     [BoardSpec] enforces. Out of range is rejected outright rather than clamped,
     *     exactly as a forged coordinate already is: a bad value here is evidence of
     *     tampering, and clamping would repair an attacker's blob into a playable one.
     */
    private fun ResumeSnapshotDto.resolveMinimumPegsOrNull(positions: List<Position>): Int? {
        val stored = minimumPegs
        if (stored != null) return stored.takeIf { it in 1..positions.size }
```

**The crucial difference to document:** `minimumPegs` out-of-range **refuses the whole snapshot** (returns `null` → no resume). The allowance must **not** — a forged counter must degrade to a full allowance and still restore the game (`AssistAllowance.fromStoredOrFull`). A rejected allowance that destroyed the resume would be a worse security posture, not a better one. The `decode` fail-safe idiom is `SnapshotMapper.kt:129-139`:

```kotlin
    fun decode(blob: String): ResumeSnapshotDto? =
        try {
            json.decodeFromString<ResumeSnapshotDto>(blob)
        } catch (ignored: IllegalArgumentException) {
            null
        }
```

`RestoredGame` (`SnapshotMapper.kt:32-38`) gains the decoded `AssistAllowance`; `toDto` (`:81-101`) gains the two counters as parameters.

**Tests to extend:** `app/src/test/.../data/resume/SnapshotMapperTest.kt` (447 lines, already covers the `minimumPegs` absent/out-of-range matrix — copy those cases verbatim in shape).

---

### `app/.../ui/game/GameUiState.kt` (MODIFIED — offer + snackbar state)

**Analog:** itself. Two idioms already in the file.

**Nullable-flag-drives-a-dialog** (`GameUiState.kt:26, 94`): `confirmRestart: Boolean` is the exact precedent for `pendingAdOffer: AdReason?`.

**Monotonic tick ordinal for one-shot effects** (`GameUiState.kt:73-81`) — the model for `adMessage: Int?` + `adMessageTick: Int`:

```kotlin
 * @property undoTick a monotonic cue ordinal, incremented once per undo that actually shortened
 *   the move history — never on a declined grant and never on an empty history. An undo's board
 *   update is otherwise indistinguishable from any other, so this is the only trace of it.
 *   Guard on `> 0` at the consumer, as with [selectionTick].
```

**Four update sites, not one** — `GameUiState.kt:113-173` defines `initial()` and `loading()`, both of which enumerate *every* field explicitly. `GameViewModel.resetToInitial()` (`:1013-1023`) plus the three board-install sites carry the ordinals forward. Any new tick must be added at **all** of them; `GameViewModel.kt:1004-1012` explains why a default cannot be relied on:

```kotlin
            // The four cue ordinals ride along for a different reason: they are keys for
            // one-shot effects in the screen. A fresh 0 is a *changed* key for an effect
            // sitting at a higher value, so resetting them here would fire a cue nobody
            // triggered ... The `= 0` defaults on the ordinals cannot be relied on to
            // cover a site that forgets one — a default is exactly what would hide it.
```

**Negative contract (UI-SPEC §Deliberate Non-UI):** no allowance count enters `GameUiState`. The counters stay in the ViewModel's private state.

---

### `app/.../ui/game/GameViewModel.kt` (MODIFIED — funnel, search-first, clock freeze)

**Analog:** itself. Four distinct in-file patterns.

**1. Two-step confirm flag + callbacks** (`GameViewModel.kt:791-808`) — the literal shape of `onAcceptAdOffer` / `onDismissAdOffer`:

```kotlin
        fun onRestart() {
            val state = _uiState.value
            if (state.status == GameStatus.Playing && moveHistory.isNotEmpty()) {
                _uiState.update { it.copy(confirmRestart = true) }
            } else {
                resetToInitial()
            }
        }

        /** Confirms a mid-game restart: resets to the initial board (D-06). */
        fun onConfirmRestart() { resetToInitial() }

        /** Dismisses the restart confirmation, keeping the current game (D-06). */
        fun onDismissRestart() { _uiState.update { it.copy(confirmRestart = false) } }
```

**2. The existing `onUndo` grant branch — guard ordering is the bug to fix** (`GameViewModel.kt:825-840`):

```kotlin
        fun onUndo() {
            viewModelScope.launch {
                when (adGateway.requestRewarded(AdReason.UNDO)) {
                    // ADS-03: an assist that could not be paid for is still granted ...
                    RewardOutcome.Granted, RewardOutcome.Unavailable -> Unit
                    RewardOutcome.Declined -> return@launch
                }
                if (moveHistory.isEmpty()) return@launch
                cancelHintSearch()
```

→ the empty-history guard **moves above** the funnel (RESEARCH Anti-Patterns): a no-op undo on a fresh board must not burn allowance. The three-way `when` branch bodies stay exactly as written.

**3. The existing `onHintRequested` order — the ADS-06 inversion** (`GameViewModel.kt:937-962`):

```kotlin
            hintJob =
                viewModelScope.launch {
                    when (adGateway.requestRewarded(AdReason.HINT)) {
                        RewardOutcome.Granted, RewardOutcome.Unavailable -> Unit
                        RewardOutcome.Declined -> {
                            _uiState.update { it.copy(solverThinking = false) }
                            return@launch
                        }
                    }
                    val deadline = timeSource.markNow() + SolverBudget.DEFAULT.deadline
                    // CR-01: capture every mutable input HERE, on the main thread ...
                    val boardForHint = _uiState.value.board
                    val solutionForHint = solution.takeIf { onRecordedLine }
                    val movesPlayedForHint = moveHistory.size
                    val move = withContext(defaultDispatcher) { getHintUseCase(...) }
```

→ the grant request moves **after** `withContext`, gated on `move != null` (RESEARCH Pattern 5). The stale-board guard (`:963-973`) and the main-thread capture comment are untouched — do not relax either.

**4. Ticker rebase = the ADSF-01 freeze mechanism** (`GameViewModel.kt:743-779`):

```kotlin
        private fun resumeTickerIfNeeded() {
            val mark = startMark ?: return
            if (tickerJob != null) return
            tickerJob = launchTicker(mark)
        }

        private fun launchTicker(mark: TimeMark): Job =
            viewModelScope.launch {
                while (isActive && _uiState.value.status == GameStatus.Playing) {
                    _uiState.update {
                        it.copy(elapsedSeconds = elapsedBaseSeconds + mark.elapsedNow().inWholeSeconds.toInt())
                    }
                    delay(TIMER_TICK_MILLIS)
                }
            }
```

→ `freezeClockForAd()` cancels `tickerJob` and folds into `elapsedBaseSeconds`; `unfreezeClockAfterAd()` takes a **fresh** `timeSource.markNow()`. **`resumeTickerIfNeeded()` needs a `if (frozenForAd) return` first line** (RESEARCH Pitfall 10) — `onUndo` calls it at line 878 and that is exactly the D-08 rescue path.

**5. `resetToInitial()` and the allowance** (`GameViewModel.kt:984-997`) — Pitfall 1's collision point:

```kotlin
        private fun resetToInitial() {
            val board = initialBoard ?: return
            moveHistory.clear()
            ...
            // Starting this board over abandons the game the snapshot describes, so the
            // stored resume point goes with it (D-05) ...
            clearSnapshot()
```

→ per **D-17 (Option A)**, the private allowance field is **not** reset here. Add a comment saying so at this site, since every other piece of per-game state *is* cleared and a reader will otherwise read the omission as a bug.

---

### `app/.../ui/game/AdOfferDialog.kt` (NEW — component, stateless leaf)

**Analogs:** `ui/onboarding/OnboardingOverlay.kt:69-131` (clay surface + stacked CTA/decline), `ui/game/NoWinBanner.kt:21-56` (test tag + stateless-leaf KDoc), `ui/game/GameScreen.kt:530-565` (`withClickSound` per callsite)
**Contract:** UI-SPEC §1 is authoritative on structure and copy; the excerpts below are the code shapes it names.

**Clay surface + stacked actions** (`OnboardingOverlay.kt:69-131`):

```kotlin
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier =
                Modifier
                    .clayShadow(
                        dimensionResource(R.dimen.elevation_dialog),
                        MaterialTheme.shapes.extraLarge,
                    ).widthIn(max = dimensionResource(R.dimen.content_max_width)),
        ) { ... }

    Column(
        modifier = Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.spacing_lg)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
    ) {
        val actionShape = MaterialTheme.shapes.medium
        Button(
            onClick = onDismiss,
            shape = actionShape,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            modifier =
                Modifier
                    .claySquish()
                    .background(brush = ctaGradientBrush(), shape = actionShape)
                    .fillMaxWidth(),
        ) {
            Text(text = stringResource(R.string.onboarding_got_it), style = MaterialTheme.typography.labelLarge)
        }
        TextButton(
            onClick = onDismiss,
            shape = actionShape,
            modifier = Modifier.claySquish().fillMaxWidth(),
        ) { ... }
    }
```

**Difference from the analog:** `OnboardingOverlay` is a `Box` scrim inside the composition; UI-SPEC §1 requires an `androidx.compose.ui.window.Dialog` host with `DialogProperties(usePlatformDefaultWidth = false)`. Copy the *surface and action stack*, not the `Box`/`scrim` wrapper.

**Test tag constant** (`NoWinBanner.kt:21-22`):

```kotlin
/** Test tag of the inline rescue banner so the screen-level Compose test can locate it. */
const val NOWIN_BANNER_TEST_TAG = "nowin_banner"
```

→ `const val AD_OFFER_DIALOG_TEST_TAG = "ad_offer_dialog"`, top-level in the same file, applied with `.testTag(...)` on the `Surface` (`NoWinBanner.kt:65`).

**Both button cues, per callsite** (`GameScreen.kt:532-555`) — the rule and its exclusion list:

```kotlin
            // SOUND-04 / D-09 / D-11: the dialog is the one place both button cues meet — confirm
            // commits (bright forward click), dismiss backs out (the softer, darker one). The cues
            // are attached per callsite and NEVER to Modifier.claySquish() ...
            TextButton(
                onClick = withClickSound(SoundCue.ButtonForward, onConfirm),
                ...
            TextButton(
                onClick = withClickSound(SoundCue.ButtonDismiss, onDismiss),
```

The wrapper itself is `audio/LocalSoundPlayer.kt:35-47`. **Do not** put the cue on `claySquish()` — that KDoc (lines 23-34) explains why.

**Stateless-leaf KDoc shape** (`NoWinBanner.kt:24-49`): the repo's leaf composables carry a KDoc naming the requirement IDs, the state-hoisting guarantee, the exact colour/shape/dimen tokens used, and a `@param` per handler. Match that density.

---

### `app/.../ui/components/ClaySnackbarHost.kt` (NEW — component) + `GameScreen.kt` / `SettingsScreen.kt` (MODIFIED)

**Analog:** `ui/game/NoWinBanner.kt` (a shared, parameterised stateless leaf used by more than one caller) — the same "one component serves both surfaces" reasoning applies here (UI-SPEC §3 requires the host be extracted once, not duplicated).

**No `SnackbarHost` exists anywhere in the app** (RESEARCH Pitfall 15) — the styling is specified in UI-SPEC §2, so this is the one Phase-9 component with **no code analog for its body**. Its *host wiring* does have one:

**Scaffold slot pattern** (`GameScreen.kt:140-156`):

```kotlin
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { GameTopBar(...) },
    ) { contentPadding ->
```

→ add `snackbarHost = { ClaySnackbarHost(snackbarHostState) }`.

**Tick-driven one-shot effect** (`GameScreen.kt:301-307`) — the exact idiom for `adMessage`/`adMessageTick`, including the mandatory `> 0` guard:

```kotlin
@Composable
private fun SelectionSound(selectionTick: Int) {
    val sound = LocalSoundPlayer.current
    LaunchedEffect(selectionTick) {
        if (selectionTick > 0) sound.play(SoundCue.Select)
    }
}
```

and its stated rationale (`GameScreen.kt:296-299`):

```kotlin
 * The `> 0` guard is required, not defensive: it is the second half of the phantom-cue fix
 * whose first half is the ordinal carry-forward in `GameViewModel`.
```

**Conditional overlay gating** (`GameScreen.kt:215-220`) — the shape for rendering `AdOfferDialog`:

```kotlin
    if (state.confirmRestart) {
        RestartConfirmDialog(
            onConfirm = onConfirmRestart,
            onDismiss = onDismissRestart,
        )
    }
```

→ `state.pendingAdOffer?.let { reason -> AdOfferDialog(reason = reason, loading = state.adOfferLoading, …) }`.

**Route wiring** (`GameRoute.kt:75-84`): pure `viewModel::method` references, one per handler — add `onAcceptAdOffer = viewModel::onAcceptAdOffer`, `onDismissAdOffer = viewModel::onDismissAdOffer`.

⚠️ `GameScreen` already carries `@Suppress("LongMethod")` (line 113) with a justification comment listing what is in it. Adding the snackbar host + dialog means **updating that comment**, not silently growing under an existing suppression.

---

### `app/.../ui/settings/*` (MODIFIED — Privacy section)

**`SettingsUiState.kt:22-26`** — one new field, `@Immutable`, fail-open default documented per property:

```kotlin
@Immutable
data class SettingsUiState(
    val soundOn: Boolean = true,
    val hapticsOn: Boolean = true,
)
```

→ `val privacyOptionsRequired: Boolean = false`. Default `false` is the correct fail-*closed* direction here and the KDoc must say so explicitly, because the file's existing KDoc (lines 12-15) justifies `true` defaults as the fail-open convention — a reader will otherwise think the new field breaks the rule. It does not: hiding a row a player does not need is safe; showing one that opens nothing is not.

**`SettingsViewModel.kt:41-57`** — collector-per-source in `init`, mutable flow never leaks:

```kotlin
        private val _uiState = MutableStateFlow(SettingsUiState())

        /** Read-only state stream for the screen; the mutable flow never leaks. */
        val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

        init {
            viewModelScope.launch {
                settingsRepository.soundOn.collect { on ->
                    _uiState.update { it.copy(soundOn = on) }
                }
            }
```

→ a third collector over `consentGateway.privacyOptionsRequired`.

**Event method shape** (`SettingsViewModel.kt:84-87`):

```kotlin
        /** Persists the player's haptics choice (SET-02); the collected flow re-renders the switch. */
        fun onToggleHaptics(on: Boolean) {
            viewModelScope.launch { settingsRepository.setHapticsOn(on) }
        }
```

→ `fun onManagePrivacyChoices() { viewModelScope.launch { consentGateway.showPrivacyOptions() } }`.

**`SettingsScreen.kt:136-195` — `SettingsToggleRow` is the literal peer** the new `SettingsLinkRow` copies (identical `Card` chrome; a trailing icon slot replaces the `Switch`):

```kotlin
@Composable
private fun SettingsToggleRow(
    label: String,
    supporting: String,
    checked: Boolean,
    entranceOrder: Int,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier =
            Modifier
                .clayEntrance(entranceOrder)
                .clayShadow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.large)
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.settings_row_min_height)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.spacing_md)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
            ) {
                Text(text = label, style = MaterialTheme.typography.titleMedium,
                     color = MaterialTheme.colorScheme.onSurface)
                Text(text = supporting, style = MaterialTheme.typography.bodyMedium,
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(...)
        }
    }
}
```

**Entrance-order constants** (`SettingsScreen.kt:197-199`) — private file-level consts, not literals:

```kotlin
/** Entrance stagger order of the two settings rows (Sound above Haptics). */
private const val SOUND_ROW_ORDER = 0
private const val HAPTICS_ROW_ORDER = 1
```

→ renumber to the UI-SPEC §3 0-5 ordering (two headers + four rows) so `clayEntrance` still cascades top-to-bottom.

**`ACTION_VIEW` from `LocalContext`, wrapped** — no in-repo precedent exists (⚠️ first `startActivity` from a composable in this codebase). The binding constraint is `PlatformBoundaryRules.kt:86-92`, which is a build gate:

```kotlin
    val uiAndDomainNeverTouchAnActivity: ArchRule =
        noClasses()
            .that()
            .resideInAnyPackage("..ui..", "..domain..", "..data..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("android.app.Activity")
```

→ `LocalContext.current` only (a `Context`), never `LocalActivity`, never a cast; `FLAG_ACTIVITY_NEW_TASK` is required because the receiver is a `Context`; `runCatching` around `startActivity` per UI-SPEC §Copywriting (ActivityNotFoundException → the Settings snackbar).

**`PrivacyLinks.kt`** — scoped constants object, `SoundParams.kt:5-20` shape. RESEARCH §Pattern 8 note: a plain constant, not a `buildConfigField`, since the URL is public and does not vary per build type. **D-19: the URL is still pending** — the plan must carry a `checkpoint:human` for it.

---

### Test files

**Pure-JVM domain test** — analog `domain/src/test/kotlin/.../scoring/ScoreCalculatorTest.kt` (184 lines, Truth assertions, no Android, no runner annotation).

**ViewModel test** — analog `app/src/test/.../ui/game/GameViewModelTest.kt`.

Construction factory (`GameViewModelTest.kt:1306-1345`) — every new VM test copies this; note `mockk(relaxed = true)` for the collaborators the spec is *not* about, each with a comment naming the spec that does own it:

```kotlin
    private fun viewModelFor(
        board: Board,
        levelId: String = ClassicLevelIds.ENGLISH33,
        challengeLevel: Int? = null,
        timeSource: TimeSource = TestTimeSource(),
        settings: SettingsRepository = FakeSettingsRepository(),
        adGateway: AdGateway = FakeAdGateway(),
        levelRepository: LevelRepository = FakeLevelRepository(boardsById = mapOf(levelId to board)),
    ): GameViewModel {
        ...
                    GameViewModel(
                        rulesEngine = RulesEngine,
                        ...
                        // This spec is about gameplay, not the Challenge ladder; a relaxed
                        // stand-in absorbs the advance-on-solve call ...
                        challengeProgress = mockk(relaxed = true),
                        timeSource = timeSource,
                        defaultDispatcher = testDispatcher,
                        adGateway = adGateway,
```

The fakes to extend with `rewardedOfferable` / `preloadRewarded` (`GameViewModelTest.kt:1592-1634`):

```kotlin
private class FakeAdGateway(
    private val outcome: RewardOutcome = RewardOutcome.Granted,
) : AdGateway {
    var lastReason: AdReason? = null
        private set

    override suspend fun requestRewarded(reason: AdReason): RewardOutcome {
        lastReason = reason
        return outcome
    }

    override fun showInterstitial() = Unit
}

/**
 * An [AdGateway] whose [requestRewarded] suspends on a shared gate until [release],
 * counting how many grants [started] ... and how many [proceeded] past it.
 */
private class GatedAdGateway : AdGateway {
    private val gate = CompletableDeferred<Unit>()
    ...
    override suspend fun requestRewarded(reason: AdReason): RewardOutcome {
        started++
        gate.await()
        proceeded++
        return RewardOutcome.Granted
    }
```

→ `GatedAdGateway` is *already* the ADSF-01 clock-freeze harness: hold the gate across `advanceTimeBy(N)` and assert `elapsedSeconds` did not move. `cancelTickers()` (`GameViewModelTest.kt:1353`) must be called by any test leaving the board mid-game.

Assertion style (`GameViewModelTest.kt:518-557`) — `runTest(testDispatcher)` + `runCurrent()` between every event:

```kotlin
    @Test
    fun undoIsSkippedWhenTheGrantIsDenied() =
        runTest(testDispatcher) {
            val deniedGateway = FakeAdGateway(outcome = RewardOutcome.Declined)
            val viewModel = viewModelFor(Boards.english33Initial, adGateway = deniedGateway)
            runCurrent()
            ...
            // Grant denied → the undo does not proceed; the board is unchanged.
            assertThat(viewModel.uiState.value.board).isEqualTo(afterJump)
            cancelTickers()
        }
```

**Compose leaf test** — analog `app/src/test/.../ui/game/NoWinBannerTest.kt:24-64` (whole file). Robolectric + `createComposeRule`, nodes located by **string resource text**, handler-invocation counters, and the SDK pin at the bottom:

```kotlin
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class NoWinBannerTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()

    @Test
    fun rendersTheMessageAndInvokesEachActionExactlyOnce() {
        var undoCount = 0
        ...
        composeRule.setContent { AppTheme { NoWinBanner(message = message, onUndo = { undoCount++ }, ...) } }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(message).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.undo_action)).performClick()
        assertThat(undoCount).isEqualTo(1)
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34
```

**Settings VM test** — analog `app/src/test/.../ui/settings/SettingsViewModelTest.kt:38-70` (`Dispatchers.setMain(StandardTestDispatcher())` in `@Before`, `resetMain()` in `@After`, fakes backed by settable `MutableStateFlow`s, Turbine for emission ordering).

---

## Shared Patterns

### Architectural boundary (build gate — applies to every new platform file)

**Source:** `app/src/test/java/com/app/pegsolitaire/PlatformBoundaryRules.kt:33-43`
**Apply to:** `AdMobAdGateway`, `RewardedAdSlot`, `UmpConsentGateway`, `AdsModule`, and every Hilt binding touching them

```kotlin
    val onlyPlatformDependsOnPlaySdks: ArchRule =
        noClasses()
            .that()
            .resideOutsideOfPackage("..platform..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.google.android.gms..",
                "com.google.android.ump..",
                "com.google.android.libraries.ads..",
            )
```

This is bytecode-based, so it also catches Hilt codegen. Companion rules at `:65-79` (three name-matched exemptions for `ForegroundActivityProvider` — do not widen) and `:86-92` (no `..ui..`/`..domain..`/`..data..` class may name `android.app.Activity`).

### Interface-to-impl seam (Hilt)

**Source:** `di/PlatformModule.kt:46-56`, `di/DataModule.kt:40-62`
**Apply to:** `ConsentGateway`, `AdGateway`

`@Binds @Singleton abstract fun bindX(impl: Impl): Interface` on an `abstract class @Module @InstallIn(SingletonComponent::class)`. Instance `@Provides` (SDK-typed: `ConsentInformation`, `InitializationConfig`) go on a `companion object` **inside `platform/ads/di/`**.

### Stateless composable + hoisted state (UDF)

**Source:** `ui/settings/SettingsRoute.kt:21-36`, `ui/game/GameRoute.kt:75-84`
**Apply to:** `AdOfferDialog`, `SettingsLinkRow`, `ClaySnackbarHost`, both screens

```kotlin
@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsScreen(
        state = state,
        onToggleSound = viewModel::onToggleSound,
        ...
    )
}
```

Route → Screen → Leaf. No leaf owns state; no screen owns a ViewModel; no composable holds a `NavController`.

### Fail-open convention

**Sources:** `data/resume/SnapshotMapper.kt:50-58`, `ui/settings/SettingsUiState.kt:12-15`, `audio/LocalSoundPlayer.kt:14-19`, `platform/ads/RewardOutcome.kt:22-27`
**Apply to:** allowance decode, `AdsReadiness` failure, ad load failure, browser-intent failure

```kotlin
    /**
     * No ad could be served (no consent yet, no fill, load error, no foreground
     * Activity). Callers MUST proceed with the assist anyway (ADS-03) — the player
     * is never dead-ended by an ad that failed to arrive.
     */
    data object Unavailable : RewardOutcome
```

Every degradation path in this codebase points at "the player keeps playing". The `Granted | Unavailable → proceed`, `Declined → skip` asymmetry is a *contract*, not a convention — the sealed type exists to force it (`RewardOutcome.kt:9-13`).

### No magic numbers / no dp-sp literals / no ad-ID literals

**Sources:** `audio/SoundParams.kt` (unitless constants), `res/values/dimens.xml:1-9` (dp/sp), `platform/ads/AdIds.kt:20-31` (IDs)
**Apply to:** every new file

```xml
<!--
  Every fixed UI dp/sp value lives here (QUAL-03). Composables read these via
  `dimensionResource(...)` — no raw `.dp`/`.sp` literal ever appears in a
  composable (the custom dpSpLiteralCheck gate fails the build on one).
-->
```

New dimen tokens follow the file's commented-block convention (`dimens.xml:53-63`), each with a phase tag and a "why this token and not that one" note:

```xml
    <!--
      Phase-5 token: min height of a Settings toggle row (label + supporting line beside
      an M3 Switch), a multiple of 4 that keeps the row comfortably tap-sized (D-14).
    -->
    <dimen name="settings_row_min_height">72dp</dimen>
```

→ the three new tokens named in UI-SPEC (`ad_offer_glyph_size`, `ad_offer_progress_size`, `settings_row_icon_size`) each need this justification comment, because each one is being added *instead of* reusing a same-valued existing token.

`strings.xml` groups keys under a block comment naming the requirement (see the `nowin_message` group at `strings.xml:97-103`). Allowance counts are `%1$d` placeholders formatted from `AssistAllowanceLimits`, never typed into the string.

**No `@Suppress` and no detekt baseline** to get past `MagicNumber` (RESEARCH Pitfall 14 / CLAUDE.md). The one existing `@Suppress("LongMethod")` in the touched code (`GameScreen.kt:113`) carries an inline justification and must have it updated, not extended silently.

### KDoc density

**Source:** every file read for this map.

This codebase's non-negotiable house style: each public type/function carries a KDoc naming the **requirement ID(s)**, the **decision ID(s)**, the reason the shape is what it is, and — where a reader might "fix" it — an explicit "do not normalise this" paragraph (`DefaultForegroundActivityProvider.kt:19-31`, `AdIds.kt:20-31`, `LocalSoundPlayer.kt:23-34`, `SoundParams.kt:126-158`). A plan task that produces terse code will read as foreign here regardless of correctness.

---

## No Analog Found

| File | Role | Data Flow | Reason |
|------|------|-----------|--------|
| `ui/components/ClaySnackbarHost.kt` (body) | component | event-driven | **No `SnackbarHost` exists anywhere in the app** (RESEARCH Pitfall 15, VERIFIED by grep). Its host wiring copies `GameScreen`'s `Scaffold` slot and its tick driver copies `UndoSound`, but the `Snackbar(...)` styling has no precedent — use UI-SPEC §2's block verbatim. |
| `SettingsScreen`'s `ACTION_VIEW` launch (REL-05) | screen fragment | — | No composable in the repo calls `startActivity`. Constrained (not patterned) by `PlatformBoundaryRules.uiAndDomainNeverTouchAnActivity`; use `LocalContext.current` + `FLAG_ACTIVITY_NEW_TASK` + `runCatching` per RESEARCH Pattern 8. |

Both are covered by RESEARCH.md / UI-SPEC.md code blocks rather than repo analogs.

---

## Planner Notes — analog-derived hazards

1. **`GameScreen.kt:113`** carries `@Suppress("LongMethod")` with a justification listing its contents. Adding a snackbar host + dialog requires updating that comment.
2. **`NoOpAdGateway.kt:8-11`** actively forbids what this phase must do (D-02 "not configurable"). The KDoc rewrite is a required part of the task, not cleanup.
3. **`AdIds.kt:20-31`** says an `AdConstants` object "must never be written". Creating `AdConstants.kt` (timeouts only) requires amending that sentence, or a future reader will read the new file as a violation.
4. **`GameUiState`'s tick ordinals must be carried at four sites** (`resetToInitial`, `loadChallengeBoardIfNeeded`, `installClassicBoard`, `applyRestored`) — see `GameViewModel.kt:1004-1012`. A new `adMessageTick` inherits this.
5. **`resumeTickerIfNeeded()` (`GameViewModel.kt:756-760`) has no frozen guard** and is called on the D-08 rescue path (`:878`). Add `if (frozenForAd) return`.
6. **`resetToInitial()` (`:997`) calls `clearSnapshot()`** — D-17 Option A means the allowance field is *deliberately* not touched there; comment it, since every sibling piece of state is cleared.
7. **`ManifestSurfaceTest` asserts permissions by set equality** — run it immediately after the manifest edit.
8. **`SettingsUiState`'s KDoc justifies `true` defaults as fail-open.** `privacyOptionsRequired = false` is the correct default for the opposite reason; state it, or the file contradicts itself.

---

## Metadata

**Analog search scope:** `app/src/main/java/com/app/pegsolitaire/{platform,di,ui,data,audio}`, `app/src/test/java/com/app/pegsolitaire/**`, `domain/src/{main,test}/kotlin/com/app/pegsolitaire/domain/**`, `app/src/main/res/values/`, `app/src/main/AndroidManifest.xml`
**Files scanned:** 196 Kotlin files enumerated; 31 read in full or by targeted range
**Pattern extraction date:** 2026-08-31
