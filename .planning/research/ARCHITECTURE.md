# Architecture Research — v1.1 "Slice 2" SDK Integration

**Domain:** Integrating Play Games Services v2, AdMob + UMP consent, and SFX audio into an existing Clean Architecture / MVVM Android game
**Researched:** 2026-08-27
**Confidence:** HIGH on SDK API surface and Activity requirements (official Google docs); HIGH on the local codebase facts (read directly); MEDIUM on the opinionated integration shapes (judgement calls, no single authoritative source); LOW on Saved-Games size caps (not stated in the docs read — verify at implementation).

> Supersedes the v1.0 domain architecture research (recoverable at commit `712aec1`). That document described how to *build* the game; this one describes how to *bolt SDKs onto the seams it left behind*. Everything in v1.0 still holds — nothing here restructures the existing layering.

---

## Starting Point (verified against the codebase, not assumed)

| Fact | Evidence |
|------|----------|
| `AdGateway` = `suspend requestRewarded(AdReason): Boolean` + `fun showInterstitial()` | `app/.../platform/ads/AdGateway.kt` |
| `AdReason` = `HINT`, `UNDO` only; KDoc reserves `RETRY` for interstitials | `app/.../platform/ads/AdReason.kt` |
| `GamesService` = `suspend signIn(): Boolean` + `fun submitScore(id, score)`; KDoc explicitly says **cloud save is NOT a method on this seam** — "it arrives as a second data source behind `ProgressRepository`" | `app/.../platform/games/GamesService.kt` |
| Both stubs bound `@Binds @Singleton` in one module | `app/.../di/PlatformModule.kt` |
| `AdGateway` is consumed at exactly **two** call sites, both in `GameViewModel` (`onUndo`, `onHintRequested`), both `if (!adGateway.requestRewarded(...)) return@launch` | `GameViewModel.kt:675, 739` |
| `GamesService` is consumed at **zero** call sites today | grep — no production reference outside the stub + its test |
| `ResultsViewModel.recordSolvedPlayOnce()` already owns an exactly-once, process-death-safe write guard via `SavedStateHandle[RECORDED_KEY]` | `ResultsViewModel.kt:90-113` |
| `DefaultProgressRepository.recordResultInTransaction` merges each best **independently and monotonically** (`max` stars, `min` time, `max` score) inside `withTransaction` | `ProgressRepository.kt:155-180` |
| `ChallengeProgress.advancePast()` writes `max(current, solvedLevel + 1)` **inside** the DataStore `edit` transaction | `ChallengeProgress.kt:83-101` |
| Haptics precedent: persisted flag collected into `GameUiState.hapticsOn`; the *emission* lives in a UI-layer `JumpHaptic` composable keyed on `state.animating?.id` | `GameScreen.kt:118, 206-230` |
| `soundOn` already persisted, fail-open `true`, with zero consumers | `SettingsRepository.kt:29, 85-89` |
| Single Activity (`MainActivity`, `@AndroidEntryPoint`), hosts `AppNavHost`, adds no logic | `MainActivity.kt` |
| `App : Application` is `@HiltAndroidApp` and has an **empty body** — no `onCreate` override yet | `App.kt` |
| `@ApplicationScope CoroutineScope` (SupervisorJob + Dispatchers.Default) exists for fire-and-forget singleton writes | `di/DataModule.kt:78-91` |
| `DomainPurityTest` (ArchUnit) bans `android.*`/`androidx.*` only inside `com.app.pegsolitaire.domain` — `platform/` and `data/` may freely use Android types | `app/src/test/.../DomainPurityTest.kt` |
| `minSdk 24`, `targetSdk 36`, `compileSdk 36` | `build-logic/src/main/kotlin/android-app.gradle.kts:26-30` |

**The single most important discovery:** *every* Play Games v2 client getter and *every* full-screen ad show and *every* UMP call takes an **`Activity`**, not a `Context`. `AdGateway` and `GamesService` are `@Singleton`s injected into `@HiltViewModel`s. That impedance mismatch — Activity-scoped SDK vs. singleton seam — is the one genuinely new structural problem this milestone has to solve, and it is shared by all three SDK integrations.

---

## Standard Architecture

### System Overview (new components in **bold**)

```
┌──────────────────────────────────────────────────────────────────────────┐
│  UI LAYER (Compose, stateless screens + stateful Routes)                  │
│                                                                            │
│  HomeScreen   ClassicCatalog   GameScreen   ResultsScreen   SettingsScreen │
│      │             │              │  ▲            │  ▲            │        │
│      │             │              │  └ LocalSoundPlayer (CompositionLocal) │
│      │             │              │               │                        │
│      │             │        JumpHaptic       StarPopSound       **Privacy  │
│      │             │        **JumpSound**    (celebration)       options   │
│      │             │                                              row**    │
│      │             │                                       **PGS sign-in   │
│      │             │                                          row**        │
│  ────┴─────────────┴──────────────┴───────────────┴────────────┴────────  │
│  Routes own one-shot plumbing: navigation, ActivityResultLauncher for      │
│  **leaderboard / achievements Intents**, lifecycle observers               │
├──────────────────────────────────────────────────────────────────────────┤
│  VIEWMODEL LAYER (StateFlow<UiState>, unidirectional)                      │
│                                                                            │
│  HomeVM      ClassicVM      GameViewModel      ResultsVM      SettingsVM   │
│                              │                  │                          │
│                              │ requestRewarded  │ recordResult (existing)  │
│                              │ **+ ad offer     │ **+ submitScore**        │
│                              │    two-step**    │ **+ achievement events** │
│                              │ **+ SharedFlow<Sfx>**                       │
├──────────────────────────────────────────────────────────────────────────┤
│  PLATFORM SEAMS (com.app.pegsolitaire.platform.*)                          │
│                                                                            │
│  ┌────────────────┐  ┌────────────────┐  ┌──────────────────────────────┐ │
│  │ AdGateway      │  │ GamesService   │  │ **SoundPlayer**              │ │
│  │ →**AdMobAd     │  │ →**PlayGames   │  │ →**SoundPoolPlayer**         │ │
│  │   Gateway**    │  │   Service**    │  │ (NoOp default for previews)  │ │
│  └───────┬────────┘  └───────┬────────┘  └──────────────────────────────┘ │
│          │                   │                                             │
│  ┌───────▼────────┐  ┌───────▼─────────┐    ┌──────────────────────────┐  │
│  │ **RewardedAd   │  │ **GamesSession**│    │ **CloudSaveDataSource**  │  │
│  │   Cache**      │  │ (StateFlow of   │    │ (SnapshotsClient)        │  │
│  │ **Interstitial │  │  sign-in state) │    │        │                 │  │
│  │   Cadence**    │  └─────────────────┘    └────────┼─────────────────┘  │
│  │ **ConsentGate**│                                   │                    │
│  └───────┬────────┘                                   │                    │
│          │                                            │                    │
│  ┌───────▼────────────────────────────────────────────┼─────────────────┐ │
│  │  **ForegroundActivityProvider** (@Singleton,        │                 │ │
│  │  WeakReference, fed by ActivityLifecycleCallbacks)  │                 │ │
│  │  THE single Activity source for ads + PGS + UMP     │                 │ │
│  └────────────────────────────────────────────────────┼─────────────────┘ │
├───────────────────────────────────────────────────────┼───────────────────┤
│  DATA LAYER (repositories = single source of truth)    │                   │
│                                                        ▼                   │
│  LevelRepository    ProgressRepository ◄───── **CloudProgressSync**       │
│                     ChallengeProgress  ◄─────┘  (merge in, export out)    │
│                     SettingsRepository                                     │
│                     OnboardingRepository                                   │
├──────────────────────────────────────────────────────────────────────────┤
│  STORES:   Room (bests, resume)      DataStore (settings, ladder)          │
├──────────────────────────────────────────────────────────────────────────┤
│  :domain (pure JVM, ArchUnit-gated)                                        │
│  RulesEngine · Solver · LevelGenerator · ScoreCalculator                   │
│  **+ ProgressMerge (extracted)**  **+ AchievementRules**                   │
└──────────────────────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | New / Modified | Responsibility | Implementation |
|-----------|----------------|----------------|----------------|
| `ForegroundActivityProvider` | **NEW** `platform/activity/` | Hands out the currently-resumed `Activity` to singletons that need one | `@Singleton`; `WeakReference<Activity>` + `MutableStateFlow<Activity?>`; fed by `Application.registerActivityLifecycleCallbacks`; `suspend fun awaitActivity(): Activity` = `stateFlow.filterNotNull().first()` |
| `App.onCreate()` | **MODIFIED** | SDK bootstrap root | Registers the activity provider callbacks; calls `PlayGamesSdk.initialize(this)` (PGS auto-attempts sign-in from here). **Does not** touch AdMob — that waits on consent |
| `ConsentGateway` | **NEW** `platform/ads/consent/` | UMP consent + `MobileAds` init; publishes `StateFlow<AdsReadiness>` | `requestConsentInfoUpdate` → `loadAndShowConsentFormIfRequired` → if `canRequestAds()` init `MobileAds` **on a background dispatcher**; also exposes `isPrivacyOptionsRequired` + `showPrivacyOptionsForm()` for the Settings row |
| `AdMobAdGateway` | **NEW** impl of existing `AdGateway` | Real rewarded + interstitial behind the unchanged (mostly) seam | `@Singleton`; awaits `AdsReadiness`, awaits Activity, bridges callbacks → coroutines with `suspendCancellableCoroutine` |
| `RewardedAdCache` | **NEW** `platform/ads/` | Keeps one preloaded `RewardedAd` warm; reloads after each show | Plain class; a `RewardedAd` object is single-show, so the reload-on-dismiss loop is mandatory |
| `InterstitialCadencePolicy` | **NEW** `platform/ads/` | Decides *whether* an interstitial may show right now | **Pure Kotlin, no Android** — `shouldShow(nowMs, levelsSinceLast, lastShownMs, sessionLevelCount): Boolean`. JVM-unit-testable, which is the whole point |
| `PlayGamesService` | **NEW** impl of existing `GamesService` | Sign-in, leaderboards, achievements, PGS UI Intents | `@Singleton`; every SDK call goes `activityProvider.awaitActivity()` → `PlayGames.getXClient(activity)` → `Task.await()` |
| `GamesSession` | **NEW** (or a field on `PlayGamesService`) | The observable sign-in state consumed by ViewModels | `StateFlow<GamesSignInState>` — `Unknown` / `SignedOut` / `SignedIn(playerId)` |
| `CloudSaveDataSource` | **NEW** `platform/games/save/` | Raw Saved-Games read/write of one JSON snapshot | `SnapshotsClient.open(name, createIfMissing = true, RESOLUTION_POLICY_MANUAL)` + `commitAndClose` |
| `CloudProgressSync` | **NEW** `data/cloud/` | Orchestrates pull → merge → push; **never writes Room directly** | Calls `ProgressRepository.exportProgress()` / `importProgress()` and `ChallengeProgress` |
| `ProgressRepository` | **MODIFIED** | Gains `exportProgress()`, `importProgress(payload)`, `aggregate(mode)` | Import is a single `withTransaction` monotonic merge — reuses the merge already written for `recordResult` |
| `ProgressMerge` | **NEW**, moved into `:domain` | The one monotonic merge function used by local record, cloud reconcile, **and** snapshot conflict resolution | Pure Kotlin: `max` stars / `min` time / `max` score / `max` level. Currently inlined at `ProgressRepository.kt:161-179` |
| `AchievementRules` | **NEW**, in `:domain` | `(ScoreResult, ProgressAggregate) -> List<AchievementEvent>` | Pure Kotlin, zero Android — the highest-value JVM-testable slice of this milestone |
| `SoundPlayer` | **NEW** `platform/audio/` | Fire-and-forget SFX, gate included | Interface + `SoundPoolSoundPlayer` + `NoOpSoundPlayer`; owns the `soundOn` gate internally |
| `LocalSoundPlayer` | **NEW** `ui/` CompositionLocal | How stateless composables reach the player without Hilt | `staticCompositionLocalOf { NoOpSoundPlayer }`, provided in `MainActivity` |

---

## Recommended Project Structure

```
app/src/main/java/com/app/pegsolitaire/
├── App.kt                              # MODIFIED: registers activity callbacks + PlayGamesSdk.initialize
├── MainActivity.kt                     # MODIFIED: kicks off ConsentGateway; provides LocalSoundPlayer
├── di/
│   ├── PlatformModule.kt               # MODIFIED: 2 binding swaps + 1 new SoundPlayer binding
│   └── AdsModule.kt                    # NEW: ad-unit-id qualifiers, cadence config
├── platform/
│   ├── activity/
│   │   └── ForegroundActivityProvider.kt   # NEW — shared by ads + PGS + UMP
│   ├── ads/
│   │   ├── AdGateway.kt                # MODIFIED: + rewardedAvailability StateFlow
│   │   ├── AdReason.kt                 # MODIFIED: + RETRY (the KDoc already reserves it)
│   │   ├── AdPlacement.kt              # NEW: interstitial placement key
│   │   ├── AdMobAdGateway.kt           # NEW
│   │   ├── RewardedAdCache.kt          # NEW
│   │   ├── InterstitialCadencePolicy.kt# NEW (pure)
│   │   ├── AdConstants.kt              # NEW (QUAL-03: unit ids, timeouts, cadence numbers)
│   │   ├── NoOpAdGateway.kt            # KEPT — debug/test binding + previews
│   │   └── consent/ConsentGateway.kt   # NEW
│   ├── games/
│   │   ├── GamesService.kt             # MODIFIED: + session state, achievements, UI intents
│   │   ├── PlayGamesService.kt         # NEW
│   │   ├── GamesSignInState.kt         # NEW
│   │   ├── GamesIds.kt                 # NEW (QUAL-03: leaderboard/achievement id → R.string map)
│   │   ├── NoOpGamesService.kt         # KEPT
│   │   └── save/CloudSaveDataSource.kt # NEW
│   └── audio/
│       ├── SoundPlayer.kt              # NEW (interface + Sfx enum)
│       ├── SoundPoolSoundPlayer.kt     # NEW
│       └── NoOpSoundPlayer.kt          # NEW
├── data/
│   ├── ProgressRepository.kt           # MODIFIED: export/import/aggregate
│   └── cloud/CloudProgressSync.kt      # NEW
├── ui/
│   ├── LocalSoundPlayer.kt             # NEW CompositionLocal
│   ├── game/  GameViewModel.kt         # MODIFIED: ad-offer two-step, Sfx SharedFlow
│   │          GameUiState.kt           # MODIFIED: pendingAdOffer, rewardedRequired, freeAssistsLeft
│   │          GameScreen.kt            # MODIFIED: JumpSound, AdOfferDialog
│   ├── results/ ResultsViewModel.kt    # MODIFIED: leaderboard + achievement submission
│   └── settings/ SettingsViewModel.kt  # MODIFIED: PGS row + privacy options row
└── res/raw/                            # NEW: jump.ogg, select.ogg, invalid.ogg, win.ogg, star.ogg

domain/src/main/kotlin/com/app/pegsolitaire/domain/
├── progress/ProgressMerge.kt           # NEW (extracted from ProgressRepository)
└── achievement/AchievementRules.kt     # NEW
```

### Structure Rationale

- **`platform/activity/` is a new sibling of `ads/` and `games/`, not a child of either.** It is shared infrastructure; nesting it under `ads/` would misrepresent the dependency and make the PGS integration look like it depends on ads.
- **Cadence and achievement *rules* are pure Kotlin.** Anything that is a policy decision (may an interstitial show? which achievements did this solve unlock?) belongs where it can be JVM-unit-tested without an emulator. `InterstitialCadencePolicy` stays in `platform/ads` (it is app policy, not game rules) but has no Android imports; `AchievementRules` goes into `:domain` because it is a pure function of domain types.
- **`CloudProgressSync` lives in `data/`, not `platform/games/`.** It is a data-layer orchestrator that happens to call a platform data source. Putting it under `platform/games` would invite it to write Room directly and break the single-source-of-truth rule.
- **The No-Op implementations stay.** They are the debug/preview/Roborazzi bindings and the only way to run the whole app without hitting real ad and PGS backends. Bind them behind a build-variant or a debug Hilt module rather than deleting them.

---

## Architectural Patterns

### Pattern 1: `ForegroundActivityProvider` — the singleton-to-Activity bridge

**What:** A process-lifetime `@Singleton` that publishes the currently-resumed `Activity` as a `StateFlow<Activity?>`, backed by a `WeakReference` and fed by `Application.registerActivityLifecycleCallbacks`.

**Why this and not the alternatives:**

| Alternative | Why rejected |
|-------------|--------------|
| Inject `Activity` into `GameViewModel` | Guaranteed leak across configuration change; ViewModels outlive Activities by design |
| `LocalActivity.current` (activity-compose 1.13) passed down to `onUndo(activity)` | Forces an Android type through every stateless composable's lambda signature — a direct violation of the project's state-hoisting mandate, and makes previews/Roborazzi need a real Activity |
| Bind `AdGateway` in `ActivityComponent` instead of `SingletonComponent` | `ActivityComponent` cannot be injected into a `@HiltViewModel`; also destroys the preloaded-ad cache on every rotation, which is exactly what you must not do |
| Hilt `@ActivityContext Context` then cast to Activity | Same scoping problem, plus an unchecked cast |

**Trade-offs:** One new component and one `Application.onCreate` line. It is inherently a little "global", but the app is single-Activity, so the provider has exactly one possible value at any time — the risk that makes this pattern smelly in multi-Activity apps does not exist here.

```kotlin
@Singleton
class ForegroundActivityProvider @Inject constructor() : Application.ActivityLifecycleCallbacks {
    private val _current = MutableStateFlow<WeakReference<Activity>?>(null)

    /** The resumed Activity, or null while the app is backgrounded. */
    val current: StateFlow<Activity?> =
        _current.map { it?.get() }.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * Suspends until an Activity is resumed. Callers MUST apply their own timeout —
     * a backgrounded app may never resume, and an ad request that waits forever
     * leaves the hint button spinning (see Anti-Pattern 4).
     */
    suspend fun awaitActivity(): Activity = current.filterNotNull().first()

    override fun onActivityResumed(activity: Activity) { _current.value = WeakReference(activity) }
    override fun onActivityPaused(activity: Activity) {
        if (_current.value?.get() === activity) _current.value = null
    }
    // remaining callbacks = no-op
}
```

`App.onCreate()` becomes:

```kotlin
@HiltAndroidApp
class App : Application() {
    @Inject lateinit var activityProvider: ForegroundActivityProvider

    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(activityProvider)
        // PGS v2 auto-attempts sign-in from here. Safe pre-consent: PGS is not an ad SDK.
        PlayGamesSdk.initialize(this)
    }
}
```

---

### Pattern 2: Callback-to-coroutine bridge with an explicit fail-open policy

**What:** `requestRewarded` stays `suspend fun (AdReason): Boolean`. Inside, the whole load → show → reward → dismiss dance collapses into one `suspendCancellableCoroutine`.

**The decision that matters is not the bridge, it is what `false` means.** Today the stub always returns `true`, and `GameViewModel` treats `false` as "silently skip the assist". If the real gateway returns `false` whenever an ad fails to load, then **hint and undo break entirely offline** — which would regress shipped v1.0 behaviour and make the game feel broken on a plane.

**Recommendation — grant on failure, decline only on an explicit user dismiss:**

| Situation | Return | Rationale |
|-----------|--------|-----------|
| Consent not yet resolved / `canRequestAds() == false` | `true` | Assist stays free; never punish the player for a consent state they cannot see |
| No fill / load error / load timeout | `true` | Offline play must keep working |
| No Activity within timeout (app backgrounded) | `true` | The player is not looking at the screen anyway |
| Ad shown, user closed it before earning | `false` | The only honest decline |
| Ad shown, reward earned | `true` | — |

This must be written into the `AdGateway` KDoc, because the contract is the whole point of the seam.

```kotlin
override suspend fun requestRewarded(reason: AdReason): Boolean {
    if (!consentGateway.readiness.value.canRequestAds) return GRANT_FREE
    val ad = rewardedCache.take(reason) ?: return GRANT_FREE          // load failure/no-fill
    val activity = withTimeoutOrNull(AdConstants.ACTIVITY_WAIT_MS) {
        activityProvider.awaitActivity()
    } ?: return GRANT_FREE

    return suspendCancellableCoroutine { cont ->
        var earned = false
        ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                rewardedCache.preload(reason)                          // single-show object
                if (cont.isActive) cont.resume(earned)
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedCache.preload(reason)
                if (cont.isActive) cont.resume(GRANT_FREE)
            }
        }
        // show() requires an Activity and (legacy SDK) the main thread.
        ad.show(activity) { earned = true }
        cont.invokeOnCancellation { ad.adEventCallback = null }
    }
}
```

**Trade-offs:** Fail-open costs revenue on flaky connections. That is the correct trade for a puzzle game whose core value proposition is "always solvable and fair"; a hint button that does nothing offline is a 1-star review.

**Additive seam change:** add `val rewardedAvailability: StateFlow<Boolean>` to `AdGateway` so the UI can label the control honestly ("Undo" vs "Undo · Watch ad") instead of surprising the player with a video. Keep the `Boolean` return — widening it to a sealed result would ripple into both call sites and their tests for no gain, since the fail-open decision belongs *inside* the gateway.

---

### Pattern 3: Two-step rewarded opt-in (a real UX change, not just plumbing)

**What:** AdMob's rewarded-ad policy requires an explicit opt-in that states the reward *before* the ad plays. Today `onUndo()` calls `requestRewarded` immediately on tap. Swapping in a real gateway without this change means an undo tap fires a fullscreen video with no warning — a disallowed implementation and a churn driver.

**Shape:**

```
tap Undo
  └─> GameViewModel.onUndo()
        ├─ freeAssistsLeft > 0  → perform the undo immediately (no ad, no dialog)
        └─ otherwise            → _uiState.update { copy(pendingAdOffer = AdReason.UNDO) }
                                     └─> GameScreen renders AdOfferDialog
                                           ├─ dismiss  → onDismissAdOffer()
                                           └─ accept   → onAcceptAdOffer()
                                                            └─ adGateway.requestRewarded(UNDO)
                                                                 └─ true → perform the undo
```

`GameUiState` additions: `pendingAdOffer: AdReason?`, `rewardedRequired: Boolean` (mirrors `adGateway.rewardedAvailability`), `freeAssistsLeft: Int`. This follows the existing `confirmRestart: Boolean` precedent exactly — the ViewModel raises a flag, the screen renders a dialog, the screen calls back. No new pattern is invented.

**Free-first economy (recommended):** grant *N* free hints + undos per level before the first offer. Without it, the very first hint on level 1 shows a video, which is the fastest way to lose a new player. `N` lives in `AdConstants`, counter resets in `resetToInitial()` and on each fresh level load.

**Trade-offs:** Three new `GameUiState` fields and one dialog. Unavoidable — the alternative is an AdMob policy strike.

---

### Pattern 4: Cloud save as a merge, not a copy — and the merge already exists

**What syncs (and, just as important, what does not):**

| Data | Store today | Sync? | Why |
|------|-------------|-------|-----|
| `ChallengeProgress.currentLevel` | DataStore int | **YES** | The long climb — the single thing a player would rage-quit over losing |
| `LevelResult` bests (`stars`, `bestTimeMs`, `bestScore`) | Room, keyed `(mode, levelId)` | **YES** | Cheap, monotonic, and what the Classic catalog pips render |
| Resume snapshot (`InProgressGame`) | Room, 1 row/mode | **NO** | Device-local and mid-flight. Syncing it means picking up a half-played board on another device and having your phone's Continue tile silently mutate. Zero upside, real confusion |
| `Settings` (`soundOn`/`hapticsOn`/`hintsEnabled`) | DataStore | **NO** | Device attributes, not player progress. A tablet on mute should stay on mute |
| `onboardingSeen` | DataStore | **NO** | Per-install |

**Conflict strategy — `RESOLUTION_POLICY_MANUAL` + monotonic field-wise merge. Do not use `RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED`.**

Reasoning: this project's progress model is *already* a join-semilattice. `ProgressRepository` merges with `max(stars)` / `min(time)` / `max(score)`; `ChallengeProgress` merges with `max(current, solved + 1)`. Those operations are commutative, associative, and idempotent — which means two divergent devices can be merged deterministically, in either order, with **zero data loss and zero user-facing "which save do you want?" dialog**. Last-writer-wins would throw away the other device's three-star runs for no reason.

So the correct move is to **extract the merge that already exists** into `:domain/progress/ProgressMerge.kt` and use the same function in three places:

1. `DefaultProgressRepository.recordResultInTransaction` (local record — behaviour must stay byte-identical)
2. `ProgressRepository.importProgress()` (cloud pull)
3. `CloudSaveDataSource` conflict resolution (`SnapshotsClient.resolveConflict`)

**Data flow — the rule is one-directional into the repository:**

```
SnapshotsClient  ◄──►  CloudSaveDataSource  ◄──►  CloudProgressSync
                                                        │
                            exportProgress() ───────────┤
                            importProgress(payload) ────┤
                                                        ▼
                                        ProgressRepository + ChallengeProgress
                                                        │
                                                        ▼
                                                  Room / DataStore
                                                        │
                                                        ▼  (existing Flows re-emit)
                                                   Home / Catalog UI
```

The cloud layer **never** touches Room or DataStore directly. It hands a payload to the repository, the repository merges it inside its existing `withTransaction`, and the already-collected Flows re-emit. **Consequence worth stating explicitly: sync has no ordering requirement relative to UI startup.** `ChallengeProgress.currentLevel` is an eagerly-started `StateFlow`; a merge landing 800 ms after Home renders simply updates the tile. No splash-screen blocking, no "syncing…" gate.

**Payload:**

```kotlin
@Serializable
data class CloudProgressPayload(
    val schemaVersion: Int,          // for forward compatibility
    val generatorVersion: Int,       // GeneratorConstants.GENERATOR_VERSION — see below
    val challengeLevel: Int,
    val results: List<CloudLevelResult>,
)
```

Carrying `generatorVersion` matters: Challenge results are keyed by level number, and level numbers only mean the same board while the generator is frozen. If a future generator bump changes level 500, importing a foreign device's "level 500 = 3 stars" would be a lie. Rule: **import Challenge results only when `generatorVersion` matches; always import `challengeLevel` (a `max`, harmless) and Classic results (generator-independent).**

**Sync triggers:** on sign-in success (pull → merge → push-if-ahead); on `ON_STOP` and after a solve is recorded (push, debounced). Never on every write.

**Size:** a Challenge ladder can reach thousands of solved levels. Cap the payload — store Classic results in full plus the last *K* Challenge levels and an aggregate — and decide the cap against the actual Saved-Games per-snapshot limit. ⚠️ **The size cap was not stated in the docs read for this research — verify `SnapshotsClient` max data size and max snapshots per player before designing the payload.**

**Play Console prerequisite:** Saved Games must be explicitly enabled for the game in Play Console before `SnapshotsClient` works at all. That is a release-configuration task, not a code task, and it blocks all cloud-save testing.

---

### Pattern 5: Leaderboard + achievement submission hooks into `ResultsViewModel`, not `GameViewModel`

**What:** Add the PGS submissions inside `ResultsViewModel.recordSolvedPlayOnce()`, immediately after `progressRepository.recordResult(...)` returns.

**Why there and nowhere else:**

| Candidate site | Verdict |
|----------------|---------|
| `GameViewModel.performJump()` at `status == Won` | ✗ `finishedEmitted` is deliberately *reset* by `onUndo()` so a rescued board can re-finish (`GameViewModel.kt:681`). That is correct for gameplay and catastrophic for submissions — undo/re-jump would submit repeatedly and burn PGS quota |
| A `LaunchedEffect` in `ResultsRoute` | ✗ Survives neither process death nor config change without inventing a second guard |
| `ResultsViewModel.recordSolvedPlayOnce()` | ✓ Already guarded exactly-once via `SavedStateHandle[RECORDED_KEY]` — survives config change *and* process death; already has `mode`, `levelId`, `ScoreResult`; already returns `NewBests` |

```kotlin
viewModelScope.launch {
    val newBests = progressRepository.recordResult(mode, levelId, result, now)
    _uiState.update { it.copy(newBests = newBests) }

    // PGS is strictly downstream of the local write: local persistence must never
    // depend on, or be delayed by, a network round trip. Fire-and-forget by contract
    // (GamesService.submitScore KDoc) — a failed submission cannot fail a solved level.
    if (newBests.any) {                       // quota hygiene: only submit improvements
        GamesIds.leaderboardFor(mode, levelId)?.let { gamesService.submitScore(it, score) }
    }
    val aggregate = progressRepository.aggregate(mode)
    AchievementRules.evaluate(result, aggregate).forEach(gamesService::report)
}
```

Notes:
- PGS leaderboards keep the player's **best**, so submission is naturally idempotent — but gating on `newBests.any` respects the documented quota guidance.
- Challenge's "highest level" leaderboard takes the level number as its score. That is the leaderboard the deterministic ladder makes meaningful (level N is the same board for everyone), and it is the one that actually justifies determinism as a v1.0 feature.
- `submitScore` must silently no-op when signed out. Do **not** build an app-side offline queue — PGS queues submissions itself once authenticated.
- `AchievementRules` needs aggregates (total stars, perfect streak). Add `suspend fun aggregate(mode): ProgressAggregate` to `ProgressRepository` backed by SQL `COUNT`/`SUM` — do **not** map `observeResults()`'s full row list, which is unbounded in Challenge mode.

**PGS UI Intents belong in the UI layer.** `getLeaderboardIntent()` returns a `Task<Intent>` that must be launched with `startActivityForResult` semantics. Model it as `suspend fun leaderboardIntent(id: String): Intent?` on `GamesService`, and have the Route launch it via `rememberLauncherForActivityResult(StartActivityForResult())`. Activity-result plumbing is a UI concern; keeping it there avoids the provider having to launch activities on behalf of a ViewModel. (`Intent` in the `GamesService` signature is fine — `platform/` is outside the ArchUnit purity boundary, which guards only `..domain..`.)

---

### Pattern 6: Sound — state-derived in the UI, gate owned by the player

**Where triggering belongs — the answer is "both, split by nature of the cue":**

| Cue | Source | Mechanism |
|-----|--------|-----------|
| Jump | Has a state correlate: `GameUiState.animating.id` already increments per move | **UI-layer observation** — a `JumpSound(animatingMoveId, …)` composable sitting directly beside the existing `JumpHaptic` (`GameScreen.kt:118`) |
| Star pop / win fanfare | Driven by the existing sequential star-pop animation in `ResultsCelebration.kt` | **UI-layer**, inside the animation |
| Select / deselect / illegal tap / undo / hint-found / no-winning-move | **No faithful state correlate.** `selected` changing cannot distinguish select from deselect; an illegal tap changes nothing at all (`onCellTap`'s `else -> Unit`) | **ViewModel event** — a `SharedFlow<Sfx>` collected in `GameRoute`. Precedent already exists: `finished` is a `Channel(CONFLATED)` for exactly this reason |

Do **not** put event-shaped fields in `UiState`. `GameUiState` is an immutable snapshot; a `lastSfx` field would replay on every recomposition and on process restore — the "Pitfall 4" one-shot problem the codebase already documents and solves with a Channel.

**Where the `soundOn` gate lives — inside `SoundPlayer`, and this is a deliberate divergence from the haptics precedent.**

`JumpHaptic` reads `GameUiState.hapticsOn` because haptics fire on exactly one screen. Sound fires on Game *and* Results *and* (optionally) button taps. Threading `soundOn` into three UiStates duplicates one flag three ways and creates three places for it to drift. Instead:

```kotlin
@Singleton
class SoundPoolSoundPlayer @Inject constructor(
    @ApplicationContext context: Context,
    settingsRepository: SettingsRepository,
    @ApplicationScope scope: CoroutineScope,
) : SoundPlayer {
    // The gate is the repository Flow — same single source of truth, one subscription.
    private val enabled: StateFlow<Boolean> =
        settingsRepository.soundOn.stateIn(scope, SharingStarted.Eagerly, DEFAULT_SOUND_ON)

    override fun play(sfx: Sfx) {
        if (!enabled.value) return
        val sampleId = loaded[sfx] ?: return   // still loading → skip, never block
        soundPool.play(sampleId, /* volumes, priority, loop, rate */ …)
    }
}
```

State hoisting is untouched: it governs *composables*, and the player is a platform-layer singleton, not state. Composables stay stateless and just call `play(Sfx.JUMP)`.

**How composables reach it:** a `staticCompositionLocalOf { NoOpSoundPlayer }` provided once in `MainActivity` (which is `@AndroidEntryPoint` and can field-inject). This mirrors `LocalHapticFeedback`, keeps every `@Preview` and Roborazzi test working with no Hilt graph, and avoids threading an `onSfx: (Sfx) -> Unit` lambda through every screen signature.

**`SoundPool` specifics:** `maxStreams = 4`; `AudioAttributes` with `USAGE_GAME` + `CONTENT_TYPE_SONIFICATION`; load all samples from `res/raw` at construction and track readiness via `setOnLoadCompleteListener` (a jump on a not-yet-loaded sample must be *skipped*, not awaited — silence beats a stutter); short `.ogg` files only. `SoundPool` remains the right tool over Media3/ExoPlayer, which buffers >250 ms and is explicitly not designed for low-latency SFX.

---

## Data Flow

### Rewarded-ad assist flow

```
[tap Undo]
   │
GameScreen ──onUndo()──► GameViewModel
                              │ freeAssistsLeft > 0 ? ──yes──► apply undo (replay fold)
                              │ no
                              ▼
                     uiState.pendingAdOffer = UNDO
                              │
GameScreen renders AdOfferDialog ──accept──► onAcceptAdOffer()
                                                   │
                                          AdGateway.requestRewarded(UNDO)
                                                   │
                              ConsentGateway.readiness ──not ready──► true (free)
                                                   │ ready
                                          RewardedAdCache.take() ──null──► true (free)
                                                   │ ad
                                    ForegroundActivityProvider.awaitActivity()
                                                   │  (timeout ──► true, free)
                                            ad.show(activity) { earned = true }
                                                   │
                                     onAdDismissedFullScreenContent
                                                   │
                                          resume(earned)  +  preload next
                                                   │
                              true ──► apply undo   false ──► no-op
```

### Cloud reconcile flow

```
PlayGamesService.signInState → SignedIn
        │
CloudProgressSync.reconcile()
        │
        ├─ CloudSaveDataSource.open("progress", createIfMissing = true, MANUAL)
        │        └─ isConflict? ──► ProgressMerge.merge(server, local) ──► resolveConflict()
        │
        ├─ remote payload ──► ProgressRepository.importProgress(payload)   [ withTransaction ]
        │                     ChallengeProgress.advancePast(remote.level - 1)  [ max, idempotent ]
        │
        └─ ProgressRepository.exportProgress() ──► commitAndClose()
                 │
                 ▼
    Room / DataStore Flows re-emit ──► Home tile + Classic catalog pips update in place
```

### Consent → ads-readiness flow (once per app session)

```
MainActivity.onCreate
     └─ ConsentGateway.ensure(activity)                 [ Activity required ]
            ├─ ConsentInformation.requestConsentInfoUpdate(activity, params, ok, err)
            ├─ UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity)
            ├─ canRequestAds() ?
            │      └─ yes ──► withContext(Dispatchers.IO) { MobileAds.initialize(...) }
            │                        (MUST be off the main thread — ANR otherwise)
            └─ readiness: StateFlow<AdsReadiness>  ──consumed by──►  AdMobAdGateway
                                                                     RewardedAdCache.preload()
     Settings screen ◄── isPrivacyOptionsRequired ── "Privacy options" row
```

Consent is **never** allowed to block the game. The player reaches the board while this runs; only ad *loading* waits on it.

---

## Anti-Patterns

### Anti-Pattern 1: Injecting or storing an `Activity` in a `@Singleton` or ViewModel
**What people do:** `class AdMobAdGateway @Inject constructor(private val activity: Activity)`, or a `var currentActivity: Activity?` field.
**Why it's wrong:** The gateway and the ViewModel both outlive the Activity. A strong reference survives rotation → guaranteed leak; LeakCanary will find it on the first config change.
**Do this instead:** `WeakReference` behind `ForegroundActivityProvider`, cleared in `onActivityPaused`, always accessed with a timeout.

### Anti-Pattern 2: Cloud save writing straight into Room/DataStore
**What people do:** `CloudProgressSync` injects the DAO and upserts the remote rows.
**Why it's wrong:** Two write paths into the same store means two sources of truth. The transactional monotonic merge in `recordResultInTransaction` gets bypassed, and a stale cloud pull can silently overwrite a better local best.
**Do this instead:** `ProgressRepository.importProgress(payload)` — one entry point, one `withTransaction`, one merge function shared with the local path.

### Anti-Pattern 3: `RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED` for progress data
**What people do:** Take the default "just pick the newest" policy because it needs no conflict code.
**Why it's wrong:** It discards the entire losing device's data. A player who three-starred English-33 on a tablet and then opened their phone loses those stars forever.
**Do this instead:** `RESOLUTION_POLICY_MANUAL` + `ProgressMerge`. The merge is already commutative/idempotent, so this costs one function call and loses nothing.

### Anti-Pattern 4: Blocking gameplay on an ad load or a sign-in round trip
**What people do:** `val granted = adGateway.requestRewarded(HINT)` with no timeout, spinner up, offline device.
**Why it's wrong:** `solverThinking` (or its ad equivalent) never clears; the hint button is permanently dead. This is a regression against a shipped v1.0 behaviour.
**Do this instead:** Timeout every await (`withTimeoutOrNull`), fail open to `true`, and never let a network state change what the board can do.

### Anti-Pattern 5: Firing a rewarded ad with no opt-in prompt
**What people do:** Wire `AdMobAdGateway` straight into today's `onUndo()` and ship.
**Why it's wrong:** AdMob's rewarded policy requires an explicit opt-in stating the reward before playback. It is a disallowed implementation, and it is also just bad — an icon-only undo button that launches a 30-second video.
**Do this instead:** Pattern 3's two-step offer, plus a free-first quota so early levels never show one.

### Anti-Pattern 6: Interstitials on app open, on level start, or after every level
**What people do:** `showInterstitial()` in `GameViewModel.init` or unconditionally on every Results → next.
**Why it's wrong:** Policy strikes / ad-serving limits, plus it is the single biggest churn driver in casual puzzle.
**Do this instead:** `InterstitialCadencePolicy` — enforce a minimum level gap, a minimum wall-clock gap, a session cap, no interstitial in the first N levels of a new install, and never within X seconds of a rewarded ad. Keep it pure and unit-test the cadence table.

### Anti-Pattern 7: Submitting leaderboard scores from `GameViewModel`
**What people do:** Hook `status == Won` in `performJump`.
**Why it's wrong:** `finishedEmitted` is intentionally reset by `onUndo()` so a rescued board can finish again. Every undo/re-jump cycle would resubmit.
**Do this instead:** `ResultsViewModel.recordSolvedPlayOnce()`, whose `SavedStateHandle` guard already survives config change and process death.

### Anti-Pattern 8: Holding `SoundPool` (or `Context`) in a ViewModel
**What people do:** `GameViewModel` owns the pool and calls `play()` from `performJump`.
**Why it's wrong:** Leaks `Context`, makes the ViewModel un-unit-testable on the JVM, and drags an Android dependency into the layer that is supposed to be the pure decision-maker.
**Do this instead:** UI-layer observation for state-derived cues, `SharedFlow<Sfx>` for eventful ones, and a platform-layer singleton that owns the pool.

### Anti-Pattern 9: `MobileAds.initialize()` on the main thread
**What people do:** Call it from `Application.onCreate` or `Activity.onCreate` directly.
**Why it's wrong:** Google's own docs warn this may cause an ANR.
**Do this instead:** `withContext(Dispatchers.IO) { MobileAds.initialize(...) }`, after consent resolves.

### Anti-Pattern 10: Assuming ad SDK callbacks arrive on the main thread
**What people do:** Update Compose state or call `Toast` straight from `onAdLoaded`.
**Why it's wrong:** The GMA **Next-Gen** SDK explicitly runs ad-load and event callbacks on a **background** thread (the legacy SDK is the opposite: all calls must be on the main thread). Getting this backwards produces either a wrong-thread crash or a silent no-update.
**Do this instead:** Resolve the coroutine from the callback and do all state work in the collecting coroutine's own context; wrap `show()` in `withContext(Dispatchers.Main.immediate)` if you are on the legacy SDK.

### Anti-Pattern 11: Gating `soundOn` in two places
**What people do:** Check `state.soundOn` in the composable *and* check `enabled.value` in the player.
**Why it's wrong:** Two gates, two subscription points, one inevitable drift bug where mute half-works.
**Do this instead:** One gate. Recommended: inside `SoundPlayer`.

### Anti-Pattern 12: Deleting `NoOpAdGateway` / `NoOpGamesService` once the real ones land
**What people do:** Treat the stubs as scaffolding.
**Why it's wrong:** They are the only way to run previews, Roborazzi screenshots, and JVM ViewModel tests without a real ad/PGS backend — and the only way to keep debug builds from generating invalid traffic.
**Do this instead:** Keep them and bind by variant/debug module. Use AdMob **test ad unit IDs** in debug regardless.

---

## Scaling Considerations

Reframed for this project — the axis that actually scales is *stored results in an infinite Challenge ladder*, not concurrent users.

| Scale | Architecture adjustments |
|-------|--------------------------|
| ~50 solved levels (typical) | Everything above works unchanged. Full result set fits comfortably in one snapshot |
| ~1,000 solved levels | `observeResults(MODE_CHALLENGE)` now maps 1,000 rows on every emission — do **not** use it for achievement aggregates. Add the SQL `aggregate(mode)` query. Cloud payload needs a cap |
| ~10,000 solved levels (`MAX_CHALLENGE_LEVEL` territory) | Snapshot payload must become lossy by design: full Classic results + `challengeLevel` + an aggregate (`totalStars`, `perfectCount`) + only the most recent *K* Challenge rows. Accept that ancient per-level Challenge bests are device-local |

### Scaling Priorities

1. **First bottleneck: the cloud payload.** Solve it at design time with a versioned, capped payload — retrofitting a size cap after players have snapshots is a migration problem.
2. **Second bottleneck: achievement aggregates.** SQL aggregation from day one; never fold a `List<LevelResult>` in Kotlin for a counter.
3. **Non-bottleneck (do not optimise):** ad preloading. One warm rewarded ad and one warm interstitial is the whole cache.

---

## Integration Points

### External Services

| Service | Artifact | Integration pattern | Gotchas |
|---------|----------|---------------------|---------|
| Play Games Services v2 | `com.google.android.gms:play-services-games-v2` (**~21.0.0**, verify at pin time) | `PlayGamesSdk.initialize(application)` in `App.onCreate`; then `PlayGames.getXClient(activity)` per call | **Every** client getter takes an `Activity`. v1 (`play-services-games`) is end-of-life — deprecated APIs removed from the SDK starting May 2026. Sign-in is auto-attempted at init; `signIn()` is only for an explicit user tap after `isAuthenticated()` returns false |
| PGS Saved Games | same artifact | `PlayGames.getSnapshotsClient(activity).open(name, createIfMissing, MANUAL)` → `commitAndClose(snapshot, metadataChange)` | Must be **enabled in Play Console** first or every call fails. `DataOrConflict.isConflict()` must be handled. Per-snapshot size cap unverified — check before designing the payload |
| Google Mobile Ads | ⚠️ **decision required** — see below | `AdGateway` impl; `show(activity, …)` | Full-screen ads require an `Activity`. A `RewardedAd`/`InterstitialAd` object is **single-show** — reload after every dismiss |
| UMP consent | `com.google.android.ump:user-messaging-platform:4.0.0` | `getConsentInformation(context)` → `requestConsentInfoUpdate(activity, …)` → `loadAndShowConsentFormIfRequired(activity)` → `canRequestAds()` | Requires an `Activity` for both calls. `canRequestAds()` always returns `false` until `requestConsentInfoUpdate` has run. Must run **every** app launch. The AdMob app ID must be in `AndroidManifest.xml` — UMP requires it too |
| Play Services Tasks ↔ coroutines | `org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0` | `Task<T>.await()` | Cancellable. Version must track the existing coroutines pin (1.11.0) — already aligned |
| `SoundPool` | platform (no dependency) | `platform/audio/SoundPoolSoundPlayer` | Async sample loading; skip un-loaded samples rather than blocking |

> ⚠️ **Ad SDK decision this milestone must make.** `CLAUDE.md` and the v1.0 `STACK.md` pin `play-services-ads 23.6.0` and say "a Next-Gen GMA SDK was announced Jan 2026 — evaluate at Slice 2". That evaluation is now due and the landscape moved: the **GMA Next-Gen SDK** (`com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk`, ~1.4.0, minSdk 24 / compileSdk 35+ — both satisfied here) went GA and was designated the **preferred SDK for Android** on 2026-07-06, with the legacy `play-services-ads` line on a sunset clock to 2028-06-30. The architectural difference that matters is the **threading model**: Next-Gen delivers load and event callbacks on a *background* thread and requires explicit main-thread dispatch for UI work, whereas the legacy SDK requires *all* calls on the main thread. Whichever is chosen, the choice is fully contained inside `AdMobAdGateway` — the seam does not change. This is a STACK-doc decision; flagging it here because it changes the coroutine-context code inside the gateway.

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| ViewModel ↔ `AdGateway` | `suspend` call + `StateFlow<Boolean>` availability | Unchanged return type (`Boolean`); the fail-open policy is a gateway-internal concern documented in the KDoc |
| ViewModel ↔ `GamesService` | `StateFlow<GamesSignInState>` collected into UiState; fire-and-forget submissions | Same collection shape as the existing `settingsRepository.hintsEnabled` pattern |
| Route ↔ `GamesService` | `suspend leaderboardIntent(): Intent?` + `rememberLauncherForActivityResult` | Activity-result plumbing is a UI concern; keeps it out of the singleton |
| `CloudProgressSync` ↔ `ProgressRepository` | `exportProgress()` / `importProgress()` — the **only** cloud write path | Non-negotiable: repository stays the single source of truth |
| Composable ↔ `SoundPlayer` | `LocalSoundPlayer` CompositionLocal, `NoOpSoundPlayer` default | Preserves preview/Roborazzi with no Hilt graph |
| Everything ↔ `ForegroundActivityProvider` | `suspend awaitActivity()` with caller-side timeout | Shared by ads, UMP and PGS — build it once, first |
| `:app` ↔ `:domain` | `ProgressMerge`, `AchievementRules` added to the pure module | ArchUnit gate applies: both must stay free of `android.*`/`androidx.*` |

---

## Suggested Build Order

Ordered by hard dependency, then by risk.

| # | Slice | Depends on | Rationale |
|---|-------|------------|-----------|
| **1** | **Sound** — `SoundPlayer` + `LocalSoundPlayer` + `JumpSound` + `Sfx` SharedFlow + Results star pops | *nothing* | Zero SDK, zero Activity, zero network, zero consent. Consumes a flag that already persists (`soundOn`). Independently shippable and the cheapest way to land visible value while the SDK plumbing is still being designed. Order-flexible — it can slot anywhere, but there is no reason to make it wait |
| **2** | **Activity + consent foundation** — `ForegroundActivityProvider`, `App.onCreate` bootstrap, `ConsentGateway`, `AdsReadiness`, `MobileAds` init, Settings "Privacy options" row, ad-SDK version decision | 1 (optional) | **The dependency root for everything else in the milestone.** Ads, UMP *and* Play Games all need the Activity provider. Consent is also a hard release gate — nothing can serve ads without it. No gameplay change; verifiable by "consent form appears, `canRequestAds()` flips true" |
| **3** | **Rewarded ads** — `RewardedAdCache`, `AdMobAdGateway.requestRewarded`, fail-open policy, two-step opt-in dialog, free-assist quota, `GameUiState` additions | 2 | The first behaviour-visible ad slice, and the one with the largest UX surface (`GameUiState`, `GameScreen`, `GameViewModel`). Do it before interstitials so the assist economy is settled before adding forced ads on top |
| **4** | **Interstitials** — `AdPlacement`, `InterstitialCadencePolicy` (pure + unit tests), Results→next-level hook, `AdReason.RETRY` | 2, 3 | Small once the gateway exists. Policy-sensitive, so it wants the cadence rules tested in isolation before wiring |
| **5** | **Play Games session** — `PlayGamesService`, `GamesSignInState` StateFlow, Settings sign-in row, Home badge | 2 | Needs only the Activity provider — **can run in parallel with 3–4** if the roadmap wants two tracks. Low risk, no gameplay impact, fully playable signed-out |
| **6** | **Leaderboards + achievements** — `GamesIds`, `ProgressRepository.aggregate()`, `AchievementRules` (`:domain`), `ResultsViewModel` hook, PGS UI Intent launching | 5 | Needs a signed-in session to verify. `AchievementRules` is pure and can be TDD'd ahead of the SDK wiring |
| **7** | **Cloud save** — extract `ProgressMerge` to `:domain`, `exportProgress`/`importProgress`, `CloudSaveDataSource`, `CloudProgressSync`, conflict resolution, payload versioning/capping | 5, 6 | The hardest and least reversible slice: it touches the persistence contract, needs two devices (or two accounts) to test, needs Play Console configuration, and has an unverified size cap. Extracting `ProgressMerge` first is a safe, test-covered refactor that can be pulled forward into slice 6 to de-risk |
| **8** | Carried tech debt (solver deadline envelope, Diamond-41 fixture, Nyquist gaps, WARN-02/03) | *nothing* | Independent of all of the above; sequence by the roadmap's own risk appetite |

**Ordering rationale in one line:** slice 2 is the only true prerequisite — build the Activity bridge and consent once, and slices 3–7 become independent leaves that can be reordered or parallelised freely.

**Research flags for the roadmap:**
- Slice 2 needs a **stack decision** (Next-Gen vs legacy GMA) before implementation — surface it as a decision, not a research task.
- Slice 7 needs **verified Saved-Games size limits** and Play Console configuration before design; flag it for phase-level research.
- Slices 1, 4, 6 are standard patterns with pure-Kotlin cores — unlikely to need further research.

---

## Confidence & Gaps

| Claim | Confidence | Basis |
|-------|------------|-------|
| All PGS v2 client getters require `Activity` | HIGH | Official `developer.android.com/games/pgs/android/*` sign-in, leaderboards and saved-games guides all pass `activity` |
| UMP requires `Activity`; `canRequestAds()` false until `requestConsentInfoUpdate` | HIGH | Official AdMob privacy guide; UMP 4.0.0 |
| GMA Next-Gen is GA + preferred for Android (2026-07-06); legacy sunset 2028-06-30 | MEDIUM-HIGH | Google Ads Developer Blog announcement + trade coverage; blog body not directly retrievable |
| Next-Gen callbacks run on a background thread | HIGH | Official "Handle callbacks from background thread" migration page |
| Legacy GMA requires all calls on the main thread | HIGH | Official rewarded-ads guide |
| `MobileAds.initialize` must be off the main thread (ANR risk) | HIGH | Official Next-Gen quick-start |
| `Task.await()` is cancellable, ships in coroutines 1.11.0 | HIGH | kotlinx.coroutines API docs |
| `SoundPool` is still correct for short game SFX vs Media3 | MEDIUM-HIGH | ExoPlayer maintainers state low-latency playback is an explicit non-goal (>250 ms buffer); consistent secondary sources |
| Existing merge functions are commutative/idempotent → manual conflict resolution is safe | HIGH | Read directly from `ProgressRepository.kt` and `ChallengeProgress.kt` |
| Saved-Games per-snapshot size cap and per-player snapshot count | **LOW — unverified** | Not stated in the docs retrieved. Must be confirmed before payload design |
| `play-services-games-v2` current version ~21.0.0 | MEDIUM | Third-party binding metadata (Jan 2026); pin against Google Maven at integration |

**Open questions for phase-level research:**
1. Saved Games hard limits (bytes per snapshot, snapshots per player) and behaviour on exceeding them.
2. Whether the free-assist quota should be per level, per session, or per day — a design/monetisation call, not an architecture one.
3. Leaderboard taxonomy: one "highest Challenge level" board plus one per Classic board, versus a single aggregate score board. Affects `GamesIds` shape only.
4. Whether debug builds should bind the No-Op gateways by build type or by a Hilt test module — a small DI decision to settle in slice 2.

## Sources

- developer.android.com/games/pgs/android/android-signin — PGS v2 sign-in, `PlayGamesSdk.initialize`, `PlayGames.getGamesSignInClient(Activity)` (HIGH)
- developer.android.com/games/pgs/android/saved-games — `SnapshotsClient`, `open(name, createIfMissing, policy)`, `commitAndClose`, Play Console prerequisite (HIGH)
- developer.android.com/games/pgs/android/leaderboards — `submitScore`, `getLeaderboardIntent` + `startActivityForResult` requirement, quota guidance (HIGH)
- developer.android.com/games/pgs/android/migrate-to-v2 — v1 API removal timeline (HIGH)
- developers.google.com/android/reference/com/google/android/gms/games/SnapshotsClient(.ResolutionPolicy|.DataOrConflict) — conflict model (MEDIUM, reference index)
- developers.google.com/admob/android/privacy — UMP 4.0.0, `requestConsentInfoUpdate`, `loadAndShowConsentFormIfRequired`, `canRequestAds` (HIGH)
- developers.google.com/admob/android/rewarded — legacy rewarded API, single-show objects, main-thread requirement, test ad units (HIGH)
- developers.google.com/admob/android/next-gen/quick-start — Next-Gen artifact/version, minSdk 24 / compileSdk 35, background-thread init (HIGH)
- developers.google.com/admob/android/next-gen/rewarded — Next-Gen rewarded API, `show(activity)`, `RewardedAdEventCallback` (HIGH)
- developers.google.com/admob/android/next-gen/migration/handle-callbacks — background-thread callback model (HIGH)
- ads-developers.googleblog.com/2026/07/announcing-google-mobile-ads-next-gen.html — Next-Gen as preferred Android SDK, 2026-07-06 (MEDIUM, body not retrievable)
- kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-play-services — `Task.await()` cancellable bridge, 1.11.0 (HIGH)
- github.com/google/ExoPlayer issues 8970 / 8722 — low-latency audio is an explicit non-goal for ExoPlayer/Media3 (MEDIUM)
- Local codebase read directly: `AdGateway.kt`, `AdReason.kt`, `GamesService.kt`, `PlatformModule.kt`, `GameViewModel.kt`, `GameRoute.kt`, `GameScreen.kt`, `ResultsViewModel.kt`, `ProgressRepository.kt`, `ChallengeProgress.kt`, `SettingsRepository.kt`, `DataModule.kt`, `App.kt`, `MainActivity.kt`, `DomainPurityTest.kt`, `build-logic/src/main/kotlin/android-app.gradle.kts`, `gradle/libs.versions.toml` (HIGH)

---
*Architecture research for: v1.1 "Slice 2" — Play Games v2, AdMob + UMP, and SFX integration*
*Researched: 2026-08-27*
