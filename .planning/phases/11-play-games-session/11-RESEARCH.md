# Phase 11: Play Games Session - Research

**Researched:** 2026-09-03
**Domain:** Play Games Services v2 (`play-services-games-v2` 22.0.0) explicit-opt-in sign-in, bound behind the existing `GamesService` seam on Android/Kotlin/Compose/Hilt
**Confidence:** HIGH on SDK dependency facts, in-repo integration points, and the Hilt/test-boundary strategy (all read directly from source or verified against live Maven metadata); MEDIUM on the precise trigger mechanics of `PlayGamesSdk.initialize()` vs `isAuthenticated()` (official docs describe behavior but do not name the exact trigger with full precision — see Open Questions); LOW on the `signIn()` cancel-vs-error distinction (undocumented by Google as of this research).

## Summary

This phase binds a real `PlayGamesService` behind the already-shaped `GamesService` seam and adds exactly one new UI surface (a one-time Home opt-in dialog) plus one Settings row. The seam, the `ForegroundActivityProvider` Activity bridge, the manifest's `PlayGamesInitProvider` removal, and the `game_services_project_id` build-config resource all already exist from Phase 6 — this phase's job is almost entirely the platform implementation, the opt-in persistence, and the two UI surfaces, not new plumbing.

The central design fact — confirmed against official Android documentation — is that Play Games Services v2 was built around **automatic, launch-time authentication with no sign-out**: "When your game launches, it will always attempt to authenticate the user." That automatic attempt is the standard v2 pattern for *every* game, and it is triggered by the same `PlayGamesSdk.initialize()` + `isAuthenticated()` sequence this project needs — the only degree of freedom this project has is *when* that sequence runs. D-01's design (never call `PlayGamesSdk.initialize` until the player has explicitly opted in) is therefore not a workaround for some special v2 quirk; it is choosing to run the standard v2 auto-auth sequence conditionally, deferred behind a persisted flag, instead of unconditionally in `Application.onCreate` the way Google's own sample does it. This is safe: `initialize()` takes only a `Context`, is documented as an idempotent one-time-per-process setup call, and nothing about the SDK requires it to run at process start — it only needs to run before the first `PlayGames.get*Client(activity)` call in that process.

The second major finding corrects a speculative recommendation in the milestone's own `ARCHITECTURE.md`: that document proposed a `play`/`nosdk` Gradle flavor dimension so JVM tests would never see the SDK. Phases 9 and 10 shipped without that flavor dimension — `AdMobAdGateway` (real GMA Next-Gen implementation) is bound **unconditionally**, for every build type, in `PlatformModule`, with zero flavor split — and PLAT-07's "JVM suite stays SDK-free" holds anyway, structurally: **no test in this codebase constructs the real Hilt `SingletonComponent`.** Every ViewModel/Screen test builds its subject directly with hand-written fakes; `NavFlowTest`'s own KDoc says this explicitly ("without a Hilt host for the production `hiltViewModel()` routes"). `PlayGamesService` should follow the exact same precedent: bind it unconditionally in `PlatformModule`, one line, no flavor dimension, no per-build-type Hilt module.

**Primary recommendation:** Build `PlayGamesService` as a `@Singleton` platform class holding its own `MutableStateFlow<GamesSignInState>`, gated by a small `PlayGamesPreferences`-style repository (modeled on `OnboardingRepository`) over the shared `app_prefs` DataStore; bind it unconditionally in `PlatformModule` (mirroring `AdMobAdGateway`, not the discarded flavor-dimension idea); bridge every `Task<T>` with `kotlinx-coroutines-play-services:1.11.0` (verified on Maven Central, version-locked to the project's coroutines pin); and treat the Home opt-in dialog and the Settings row as two independent callers of the same `signIn()` seam method, distinguished only by which persisted flags they also set (D-19).

## Project Constraints (from CLAUDE.md)

- **No magic numbers/strings** — every new string goes through `strings.xml`, every DataStore key/dimension through a scoped constants object (`PlayGamesPreferences`-style, mirroring `OnboardingPreferences`/`SettingsPreferences`).
- **Compose state hoisting** — `HomeScreen`/`SettingsScreen` stay stateless; the opt-in dialog and Settings row render from `HomeUiState`/`SettingsUiState` fields owned by their ViewModels, exactly like `AdOfferDialog`.
- **`dimens.xml` for all spacing/sizes** — no hardcoded dp/sp in the new dialog or Settings row composables.
- **detekt + ktlint gates** — both run inside `./gradlew check`; new code must pass both, no suppressions without cause.
- **Gitflow** — work happens on a `feature/*` branch off `develop` per the repo's existing branch (`feature/phase-11-play-games-session`).
- **Pure-Kotlin domain, SDK isolation** — `com.google.android.gms.games.*` types may only appear inside `platform/games/`; `PlatformBoundaryRules` (ArchUnit, bytecode-based) fails the build otherwise. `ForegroundActivityProvider` is the only sanctioned Activity source, reachable only from `..platform..` plus the three named exemptions (`App`, `App_MembersInjector`, `PlatformModule`).
- **Architecture** — MVVM with StateFlow UiState, Repository pattern for persistence, Hilt DI, Coroutines/Flow for async — all already established and to be followed exactly (see Architecture Patterns below).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Sign-in timing**
- D-01 (revised): Sign-in is explicit opt-in. Until the player has opted in (D-15/D-19), nothing Play-Games-related runs: no `PlayGamesSdk.initialize`, no `isAuthenticated` check, no SDK UI, and `session` resolves straight to `SignedOut`. On every launch, `MainActivity.onCreate`, immediately after `consentGateway.bootstrap()`, fire-and-forget, hands the bootstrap to the platform implementation, which reads the persisted opt-in flag (D-17) and — if and only if the player previously opted in — runs `PlayGamesSdk.initialize` plus the silent `isAuthenticated` check, the same shape as the consent bootstrap. Every PGS client getter needs an `Activity`; `MainActivity` is the single Activity, and the platform implementation still reaches it only through `ForegroundActivityProvider` (never a stored Activity). Nothing about sign-in may block or delay the first frame or the first tap.
- D-02 (revised): The silent check runs once per process for opted-in players. `session` changes afterwards only through (a) accepting the first-launch prompt (D-18) and (b) the manual `signIn()` path from Settings (D-10). No re-check on foreground return, no connectivity listener.
- D-03: Play Games init is independent of the UMP consent flow. CONS-02 governs the ads SDK only; the published privacy policy (Phase 9 D-19) already discloses optional Play Games. Sign-in never waits on `AdsReadiness`.
- D-04: No Play Games project exists in Play Console yet. The phase is therefore verified by JVM tests against a fake `GamesService` plus the platform implementation's own unit tests; emulator sign-in verification is a pre-release checklist item, recorded as human-needed debt — not a blocker for planning or execution. Operator checklist gains: create the PGS project, register OAuth credentials for the debug-keystore SHA-1 and the original release-keystore SHA-1, add a tester account.

**Signed-in feedback**
- D-05: The stock Play Games "Welcome back" popup stays, in its default position. No gravity change, no suppression attempt.
- D-06: When signed in, Settings shows a non-tappable "Signed in as <display name>" row, replacing the sign-in row.
- D-07: `GamesSignInState.SignedIn` gains `displayName: String?`, fetched once at sign-in (via `PlayersClient`) inside the platform implementation and carried in the state flow. Supersedes the earlier "playerId only" pick.

**Manual sign-in row**
- D-08: Settings gains a new "Play Games" section, between General and Privacy, using the existing `SettingsSectionHeader` + `SettingsLinkRow` shapes. Privacy stays pinned at the bottom (Phase 9 D-14). Phase 12's leaderboard/achievement rows land in this same section.
- D-09: Row copy: label "Sign in to Play Games", supporting "Leaderboards, achievements and cloud save".
- D-10: Tapping the row calls `signIn()`. On an SDK-reported error, a snackbar ("Couldn't sign in to Play Games" — exact wording at planner's discretion) via the existing `ClaySnackbarHost`. A plain dismiss/decline by the player is silent. The row remains in both cases.

**Opt-in prompt (new, 2026-09-03)**
- D-15: A one-time, app-owned dialog asks the player whether to sign in to Play Games. Shown on the Home screen after the first frame, and only once the first-run onboarding overlay has been dismissed (`HomeUiState.onboardingVisible == false`, ONB-01). Material 3 `AlertDialog` in the clay style, modeled on `ui/game/AdOfferDialog.kt`, driven by a `playGamesPromptVisible` flag in `HomeUiState` owned by `HomeViewModel` — state hoisted, no SDK types in `ui/`.
- D-16: The dialog is dismissible and never a wall: two actions — positive "Sign in" and negative "Not now"; tapping outside or pressing back counts as "Not now". Never shown over gameplay, Results, or Settings — Home only. Copy must name leaderboards, achievements, cloud save and make clear the game is fully playable without signing in.
- D-17: The answer is persisted in the shared app DataStore (`AppPreferences` store, `data/AppPreferences.kt`) through a small preferences repository modeled on `data/OnboardingRepository.kt`, with two boolean keys: `play_games_prompt_answered` and `play_games_opted_in`. "Not now" sets answered = true, opted-in = false, prompt never appears again. "Sign in" sets answered = true, opted-in = true. No per-session flags.
- D-18: Accepting the prompt immediately runs `PlayGamesSdk.initialize` + `signIn()` through the seam; the SDK's own account chooser may appear as the Google-owned second step. Later launches take D-01's silent path. If the SDK reports an error on that first sign-in, the D-10 snackbar wording is shown on Home via `ClaySnackbarHost` (Home gains a snackbar host if it has none); opted-in stays `true` so the next launch retries silently, and the Settings row is available meanwhile. A plain dismiss of the SDK's chooser is silent.
- D-19: Tapping the manual Settings row (D-10) also sets `play_games_opted_in = true` and `play_games_prompt_answered = true`. For a player who has not opted in, or who declined, `session` is `SignedOut` from launch with the SDK untouched, and the Settings row shows.

**Unknown and offline states**
- D-11: While `session` is `Unknown`, the Play Games section is hidden entirely. Appears only once resolved to `SignedOut` or `SignedIn`. Players who have not opted in are never `Unknown`.
- D-12: For opted-in players, missing Google Play services / Play Games app, or an offline device at launch, all read as `SignedOut`. No `GoogleApiAvailability` gating, no connectivity retry.
- D-13: After an explicit decline from the manual row, the row stays. No per-session "declined" flag.
- D-14: If authentication succeeds but the display-name lookup fails or stalls, the state is `SignedIn(playerId, displayName = null)` and Settings shows the fallback "Signed in to Play Games". A name lookup failure is never reported as a sign-in failure and never keeps the section in `Unknown`.

### Claude's Discretion
- Exact snackbar wording and the exact supporting text of the signed-in row; all strings through `strings.xml`, spacing through `dimens.xml`.
- Internal structure of the real implementation (`PlayGamesService` naming, whether the session `StateFlow` lives on the service or a small `GamesSession` holder, `Task` → `suspend` bridging), threading/dispatchers, display-name fetch sequencing.
- Whether the debug build type binds the real implementation or keeps `NoOpGamesService` — constrained by D-04 and PLAT-07.
- Test doubles: the shape of the fake `GamesService` used by `SettingsViewModel` tests, and whether `NoOpGamesService` remains the preview/test default.
- Whether the seam's Phase 12 methods get real bodies now or remain no-ops until Phase 12.
- Exact title/body copy of the opt-in dialog (D-16) and whether the opt-in flags live on `SettingsRepository` or a new dedicated repository (D-17) — both go through the shared `AppPreferences` store either way.
- Where the "opted in?" branch of D-01 lives: inside the real `GamesService` implementation (preferred) or a small bootstrap helper.

### Deferred Ideas (OUT OF SCOPE)
- Leaderboard and achievement rows in the Play Games section, and the stock Play Games UI intents — Phase 12.
- Saved Games / cloud save — Phase 13, behind `ProgressRepository`.
- Play Console PGS project creation, OAuth credentials for both SHA-1s, tester list — operator work, tracked in ROADMAP External Prerequisites.
- Emulator sign-in verification of PGS-01/PGS-03 — pre-release checklist item once the Console project exists (D-04).
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| PGS-01 | Player is asked once, on first launch, whether to sign in to Play Games; a player who accepts is signed in automatically on every later launch, and gameplay is never blocked or interrupted by sign-in | Architecture Patterns (bootstrap sequence, opt-in dialog); Code Examples (`PlayGamesService.bootstrapIfOptedIn`, `HomeUiState`/`HomeViewModel` opt-in wiring); Common Pitfalls 1–3 |
| PGS-02 | Every feature of the game works fully when sign-in is unavailable or declined, with social surfaces hidden rather than erroring | Architecture Patterns (`GamesSignInState` hiding rules, D-11/D-12); Common Pitfalls 4, 5 |
| PGS-03 | Player can sign in manually from Settings when automatic sign-in did not happen | Architecture Patterns (Settings Play Games section); Code Examples (`SettingsViewModel.onSignInToPlayGames`); Common Pitfalls 2 |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Opt-in prompt (Home dialog) | UI (Compose, `ui/home/`) | — | Pure presentation + state-hoisted flag; no SDK knowledge |
| Opt-in decision persistence | Data (`data/`, DataStore) | — | Two booleans, no queries — a `data/`-layer preferences repository, not a database concern |
| Sign-in bootstrap decision ("opted in? initialize+check") | Platform (`platform/games/`) | — | Reads the persisted flag and decides whether to touch the SDK at all; must stay behind the seam so `MainActivity` names no SDK type |
| `PlayGamesSdk.initialize` / `isAuthenticated` / `signIn` / `PlayersClient.getCurrentPlayer` | Platform (`platform/games/`) | — | Every one of these calls needs an `Activity` and an SDK type; PlatformBoundaryRules confines both to `..platform..` |
| Foreground Activity resolution | Platform (`platform/activity/`) | — | Already built (Phase 6); `PlayGamesService` is a new *consumer*, not a new implementation |
| Session state observation (`GamesSignInState`) | Platform (source) / UI (consumer) | — | Owned and mutated in `platform/games/`, observed read-only via `StateFlow` by `SettingsViewModel` |
| Settings "Play Games" section rendering | UI (`ui/settings/`) | — | Stateless render of `SettingsUiState`, mirrors the existing Privacy section pattern exactly |
| Manual sign-in trigger from Settings | UI (ViewModel) → Platform (seam) | — | `SettingsViewModel.onSignInToPlayGames()` calls `GamesService.signIn()`; no SDK type crosses the boundary |

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.google.android.gms:play-services-games-v2` | **22.0.0** `[VERIFIED: dl.google.com/android/maven2 maven-metadata.xml, fetched live this session]` | Sign-in (`PlayGamesSdk`, `GamesSignInClient`, `PlayersClient`) | Already pinned in `gradle/libs.versions.toml` (`playServicesGamesV2 = "22.0.0"`) by Phase 6; the only supported PGS line (v1 is EOL). Re-verified current — `dl.google.com`'s `maven-metadata.xml` `<latest>` still reads `22.0.0`, `lastUpdated` 2026-07-29, unchanged since the milestone's original research. |
| `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | **1.11.0** `[VERIFIED: repo1.maven.org/maven2 maven-metadata.xml, fetched live this session]` | `Task<T>.await()` bridge for every PGS call | Confirmed to exist on Maven Central at exactly 1.11.0 (`<latest>1.11.0</latest>`, `<release>1.11.0</release>` in the live metadata) — matches the project's `coroutines = "1.11.0"` pin exactly, so version-ref it to the existing catalog entry rather than hardcoding a literal. **Not yet in `gradle/libs.versions.toml`** — this phase adds the library entry (the `play-services-games-v2` entry already exists from Phase 6, the coroutines-bridge artifact does not). |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `androidx.datastore:datastore-preferences` | 1.2.1 (already in the project) | Persist `play_games_prompt_answered` / `play_games_opted_in` | Already the project's settings/onboarding persistence mechanism; no new dependency needed — extend the existing shared `app_prefs` store (D-17). |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `kotlinx-coroutines-play-services` `.await()` | Hand-rolled `suspendCancellableCoroutine` wrapper around `Task.addOnCompleteListener` | `UmpConsentGateway.showPrivacyOptions()` already hand-rolls this pattern for a UMP callback that has no `Task`-based API. But every PGS v2 call in this phase (`isAuthenticated()`, `signIn()`, `getCurrentPlayer()`) *does* return a `Task<T>`, so the official adapter is strictly less code and version-locked to the existing coroutines pin — no reason to hand-roll here. |
| Single unconditional `PlatformModule` binding for `PlayGamesService` | A `play`/`nosdk` Gradle flavor dimension (as speculatively proposed in milestone `ARCHITECTURE.md`) | The flavor-dimension idea predates Phases 9–10 shipping. `AdMobAdGateway` proved the simpler path works: bind the real implementation unconditionally, and PLAT-07 holds because no JVM test constructs the real Hilt component at all (verified: `grep` for `HiltAndroidTest`/`DaggerApp_HiltComponents` across `app/src/test` returns nothing outside `PlatformBoundaryRules.kt`, which references the class name only for the ArchUnit rule, not to instantiate it). A flavor dimension is real added build-graph complexity for a benefit the codebase already has for free. |
| `PlayersClient.getCurrentPlayer()` for `displayName` | Server-side Play Games API call | The client-side ID lookup is what D-07 asks for and is standard practice for cosmetic display; Google's own docs warn against trusting the *client-side player ID* for server-authoritative purposes (not relevant here — nothing in this app is server-authoritative) but say nothing against using it for a Settings display string. |

**Installation:**
```kotlin
// gradle/libs.versions.toml — new library entry (playServicesGamesV2 = "22.0.0" already exists)
kotlinx-coroutines-play-services = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-play-services", version.ref = "coroutines" }
```
```kotlin
// app/build.gradle.kts
implementation(libs.kotlinx.coroutines.play.services)
// libs.play.services.games.v2 is already an `implementation(...)` dependency from Phase 6
```

**Version verification performed this session:**
```
curl -s https://dl.google.com/android/maven2/com/google/android/gms/play-services-games-v2/maven-metadata.xml
  → <latest>22.0.0</latest>  (unchanged since 2026-07-29)
curl -s https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-play-services/maven-metadata.xml
  → <latest>1.11.0</latest>  <release>1.11.0</release>
```

## Package Legitimacy Audit

`slopcheck` was not available in this environment (`pip install slopcheck` was not attempted because both candidate packages are already first-party/official — Google's own SDK and the official JetBrains coroutines-interop library — and both were independently verified against their authoritative registries by direct `curl` against `dl.google.com` and `repo1.maven.org`, which is a stronger signal than `slopcheck` would add). Both packages are therefore tagged `[VERIFIED]`, not `[ASSUMED]`, per the provenance rule (verified against an authoritative source, not merely "exists on the registry").

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| `com.google.android.gms:play-services-games-v2` | Google Maven (`dl.google.com`) | First published as v2 line 2023; current 22.0.0 released 2026-07-29 | N/A (Google Maven does not publish download counts; already a dependency of this project since Phase 6) | Closed-source, first-party Google | not run — first-party official SDK | Approved (already integrated, no new install) |
| `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | Maven Central | Part of the `kotlinx.coroutines` monorepo since 2018; version 1.11.0 released alongside the rest of the 1.11.0 coroutines line | N/A (official JetBrains library, sibling of `kotlinx-coroutines-android`/`-test` already in this project) | `github.com/Kotlin/kotlinx.coroutines` | not run — first-party official JetBrains library, version-locked to the already-audited `coroutines` pin | Approved |

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```
                     ┌─────────────────────────────────────────────┐
                     │                MainActivity.onCreate         │
                     │  (after super.onCreate(), fire-and-forget)   │
                     └───────────────┬───────────────────────────────┘
                                      │
                      consentGateway.bootstrap()   gamesService.bootstrapIfOptedIn()
                                      │                        │
                                      │                        ▼
                                      │        ┌───────────────────────────────┐
                                      │        │ PlayGamesPreferences (DataStore)│
                                      │        │  play_games_opted_in == true?   │
                                      │        └───────────┬─────────┬─────────┘
                                      │                 false      true
                                      │                    │          │
                                      │            session=SignedOut  ▼
                                      │            (SDK never touched)│
                                      │                          ┌────────────────────┐
                                      │                          │ ForegroundActivity   │
                                      │                          │ Provider.awaitActivity()
                                      │                          └─────────┬───────────┘
                                      │                                    ▼
                                      │                     PlayGamesSdk.initialize(context)
                                      │                                    │
                                      │                     GamesSignInClient(activity)
                                      │                          .isAuthenticated()
                                      │                                    │
                                      │                       ┌────────────┴────────────┐
                                      │                    authenticated            not authenticated
                                      │                       │                          │
                                      │           PlayersClient.getCurrentPlayer()   session=SignedOut
                                      │                       │
                                      │           session=SignedIn(playerId, displayName)
                                      ▼
                     ┌───────────────────────────────┐
                     │           Compose UI            │
                     │  HomeScreen ── playGamesPrompt   │  ← shown once, after onboarding dismissed,
                     │  (D-15/D-16, HomeViewModel)      │    gated on !prompt_answered
                     │                                   │
                     │  SettingsScreen ── Play Games      │  ← collects GamesService.session,
                     │  section (D-08..D-14)              │    hidden while Unknown, shows
                     │                                     │    Sign-in row (SignedOut) or
                     │                                     │    "Signed in as X" row (SignedIn)
                     └───────────────┬─────────────────────┘
                                     │  onAccept() / onSignInRowTap()
                                     ▼
                     gamesService.signIn()  →  sets opted_in=true, prompt_answered=true (D-18/D-19)
                                     │
                                     ▼
                     GamesSignInClient(activity).signIn() → Task<AuthenticationResult>
                     (Google-owned account-chooser UI may appear — expected, not suppressed)
```

### Recommended Project Structure

```
app/src/main/java/com/app/pegsolitaire/
├── platform/games/
│   ├── GamesService.kt              # MODIFIED: no interface shape change needed (signIn() already suspend/Boolean)
│   ├── GamesSignInState.kt          # MODIFIED: SignedIn gains displayName: String? (D-07)
│   ├── PlayGamesService.kt          # NEW — the real implementation
│   ├── NoOpGamesService.kt          # UNCHANGED — stays the reference/no-op behavior
├── data/
│   ├── AppPreferences.kt            # UNCHANGED — shared DataStore delegate
│   ├── PlayGamesPreferences.kt      # NEW — the two boolean keys + scoped constants (mirrors OnboardingPreferences)
│   ├── PlayGamesOptInRepository.kt  # NEW — read/write play_games_prompt_answered / play_games_opted_in (mirrors OnboardingRepository)
├── ui/home/
│   ├── HomeUiState.kt               # MODIFIED: + playGamesPromptVisible: Boolean
│   ├── HomeViewModel.kt             # MODIFIED: sequences prompt after onboardingVisible == false; onAcceptPlayGames/onDeclinePlayGames
│   ├── HomeScreen.kt                # MODIFIED: renders the opt-in dialog + a ClaySnackbarHost for the D-18 error case
│   ├── PlayGamesPromptDialog.kt     # NEW — modeled directly on ui/game/AdOfferDialog.kt
├── ui/settings/
│   ├── SettingsUiState.kt           # MODIFIED: + gamesSignInState: GamesSignInState (or a flattened equivalent)
│   ├── SettingsViewModel.kt         # MODIFIED: collects GamesService.session; onSignInToPlayGames()
│   ├── SettingsScreen.kt            # MODIFIED: new "Play Games" section between General and Privacy (D-08)
├── di/
│   ├── PlatformModule.kt            # MODIFIED: bindGamesService(impl: NoOpGamesService) → bindGamesService(impl: PlayGamesService)
├── AndroidManifest.xml               # MODIFIED: + com.google.android.gms.games.APP_ID meta-data (currently absent)
```

### Pattern 1: The opted-in-gated bootstrap lives inside the platform implementation

**What:** `MainActivity.onCreate` gets exactly one new line, fire-and-forget, calling a single method on the `GamesService` interface — no branching logic in `MainActivity` itself. The interface needs one addition beyond what already exists: a `bootstrap()`-style entry point that internally reads the opt-in flag and decides whether to touch the SDK.

**When to use:** This is the CONTEXT.md discretion point ("Where the 'opted in?' branch of D-01 lives") — research recommends resolving it in favor of the platform implementation, for the same reason `ConsentGateway.bootstrap()` lives entirely inside `UmpConsentGateway`: `MainActivity` should stay a thin, undecorated caller, and every branch that reads persisted state or touches an SDK type belongs behind the seam where `PlatformBoundaryRules` can see it.

**Example:**
```kotlin
// Source: modeled on the shipped ConsentGateway.bootstrap() / MainActivity pattern
// app/src/main/java/com/app/pegsolitaire/platform/games/GamesService.kt (interface addition)
interface GamesService {
    val session: StateFlow<GamesSignInState>
    suspend fun signIn(): Boolean

    /**
     * Fire-and-forget launch-time bootstrap (D-01). Reads the persisted opt-in flag;
     * if and only if the player previously opted in, runs PlayGamesSdk.initialize
     * plus the silent isAuthenticated check. A no-op for a player who has never
     * opted in — session stays SignedOut without the SDK ever being touched.
     */
    fun bootstrapIfOptedIn()

    // ...existing submitScore/unlockAchievement/showLeaderboard/showAchievements...
}

// app/src/main/java/com/app/pegsolitaire/MainActivity.kt (one new line)
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    consentGateway.bootstrap()
    gamesService.bootstrapIfOptedIn() // D-01: no-op unless the player already opted in
    setContent { /* ... */ }
}
```

### Pattern 2: `Task<T>` → `suspend` bridging with `kotlinx-coroutines-play-services`

**What:** Every PGS v2 call (`isAuthenticated()`, `signIn()`, `getCurrentPlayer()`) returns `com.google.android.gms.tasks.Task<T>`. The official adapter's `.await()` extension turns this into a cancellable suspend call.

**When to use:** Always, for any `Task`-returning PGS API. Do not hand-roll `suspendCancellableCoroutine` for these — that pattern is reserved in this codebase for APIs with no `Task`-based surface (see `UmpConsentGateway.showPrivacyOptions()`, which wraps a raw callback because UMP's form-dismiss API has no `Task`).

**Example:**
```kotlin
// Source: kotlinx-coroutines-play-services 1.11.0 (org.jetbrains.kotlinx), verified on Maven Central
import kotlinx.coroutines.tasks.await

private suspend fun checkAuthenticated(activity: Activity): Boolean {
    val client = PlayGames.getGamesSignInClient(activity)
    val result = client.isAuthenticated().await() // suspend, cancellable
    return result.isAuthenticated
}
```

### Pattern 3: Session state collection into UiState (established pattern, reused verbatim)

**What:** `SettingsViewModel` already collects three independent sources (`SettingsRepository.soundOn/hapticsOn`, `ConsentGateway.privacyOptionsRequired`) into one `SettingsUiState` via three parallel `viewModelScope.launch { flow.collect { ... } }` blocks. `GamesService.session` is a fourth peer, collected the same way.

**When to use:** Exactly this phase's Settings wiring — no new pattern needed.

**Example:**
```kotlin
// Source: existing SettingsViewModel.kt pattern, extended
init {
    // ...existing soundOn/hapticsOn/privacyOptionsRequired collectors...
    viewModelScope.launch {
        gamesService.session.collect { session ->
            _uiState.update { it.copy(gamesSignInState = session) }
        }
    }
}

fun onSignInToPlayGames() {
    viewModelScope.launch { gamesService.signIn() }
}
```

### Pattern 4: The Home opt-in dialog, modeled directly on `AdOfferDialog`

**What:** A raw `Dialog` (not the stock M3 `AlertDialog` component — matches D-06's rejection of the stock component for the same reason `AdOfferDialog` rejects it) with a clay `Surface`, two full-width stacked actions (positive gradient button, negative `TextButton` peer beneath it), driven entirely by hoisted state.

**When to use:** This phase's `PlayGamesPromptDialog`. `AdOfferDialog` is the closest possible precedent: same modal-over-scrim shape, same two-action hierarchy, same "declining leaves everything exactly as it was" guarantee.

**Example:**
```kotlin
// Source: modeled on the shipped app/src/main/java/com/app/pegsolitaire/ui/game/AdOfferDialog.kt
@Composable
fun PlayGamesPromptDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDecline, // D-16: outside tap / back press == "Not now"
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // Surface + Column, same token set as AdOfferDialog:
        // shapes.extraLarge, surfaceContainerHigh, clayShadow(elevation_dialog),
        // spacing_lg outer padding, spacing_md content spacing, spacing_sm action spacing
    }
}
```

### Pattern 5: Settings "Play Games" section, modeled directly on the Privacy section

**What:** A new `SettingsSectionHeader` + conditional `SettingsLinkRow`, between the existing General and Privacy sections (D-08), following the exact structure `PrivacySection` already establishes in `SettingsScreen.kt` (a private composable taking the relevant state slice + click handlers, called from the top-level `SettingsScreen`).

**Example:**
```kotlin
// Source: modeled on the shipped PrivacySection composable in SettingsScreen.kt
@Composable
private fun PlayGamesSection(
    gamesSignInState: GamesSignInState,
    onSignInToPlayGames: () -> Unit,
) {
    if (gamesSignInState == GamesSignInState.Unknown) return // D-11: hidden entirely while Unknown

    SettingsSectionHeader(title = stringResource(R.string.settings_section_play_games), entranceOrder = PLAY_GAMES_HEADER_ORDER)
    when (gamesSignInState) {
        is GamesSignInState.SignedIn ->
            // D-06/D-14: non-tappable "Signed in as X" row, falls back to "Signed in to Play Games"
            SettingsStaticRow(/* ... */)
        GamesSignInState.SignedOut ->
            SettingsLinkRow(
                label = stringResource(R.string.settings_play_games_signin_label),
                supporting = stringResource(R.string.settings_play_games_signin_supporting),
                onClick = onSignInToPlayGames,
            )
        GamesSignInState.Unknown -> Unit // unreachable — guarded above
    }
}
```
*(`SettingsStaticRow` does not exist yet — `SettingsLinkRow` with `onClick = {}` and no trailing icon is an acceptable substitute if a new non-interactive row shape is not worth adding; planner's call.)*

### Anti-Patterns to Avoid

- **Calling `PlayGamesSdk.initialize()` unconditionally in `App.onCreate`** (the pattern Google's own sample and most tutorials show): this is exactly what D-01 forbids. `App.onCreate` also runs under Robolectric for the whole JVM suite (per `MainActivity.kt`'s own KDoc, which explains why `ConsentGateway.bootstrap()` deliberately does *not* live in `App`) — initializing an SDK there would risk dragging it onto the JVM test execution path the same way consent bootstrap was kept out of `App` to avoid.
- **A `play`/`nosdk` Gradle flavor dimension for this phase**: superseded by the Phase 9/10 precedent (see Alternatives Considered). Do not introduce new build-graph complexity the codebase has already proven unnecessary.
- **Passing an `Activity` through `GamesService`'s interface signature**: every method on `GamesService` must stay Activity-free, exactly like `AdGateway` and `ConsentGateway`. The implementation resolves its own Activity via `ForegroundActivityProvider.awaitActivity()`.
- **A second Hilt `@Provides` for a PGS SDK type**: `PlatformModule`'s own KDoc documents (with a cited empirical test from Phase 9) that an SDK-typed `@Provides` fails `PlatformBoundaryRules` regardless of which module declares it, because the generated Hilt component itself lives outside `..platform..`. Construct PGS SDK handles (`GamesSignInClient`, `PlayersClient`) as plain internal `by lazy` properties inside `PlayGamesService`, the same way `UmpConsentGateway` does for `ConsentInformation`.
- **Re-checking `isAuthenticated()` on every foreground return**: explicitly rejected by D-02. PGS v2's own design intent (automatic launch-time auth, no sign-out) already covers this — a foreground-return re-check adds nothing and risks a spurious "Welcome back" popup mid-session.
- **Deriving the interstitial-style session boundary (`ProcessLifecycleOwner`) for anything in this phase**: not needed. D-02 already rules out foreground-return re-checks, so there is no cadence state to persist across backgrounding the way `AppForegroundTracker` does for ads.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| `Task<T>` → `suspend` bridging | A `suspendCancellableCoroutine` wrapper per PGS call | `kotlinx-coroutines-play-services`'s `Task<T>.await()` | Official adapter, version-locked to the project's coroutines pin, already the milestone's own recommendation, handles cancellation and exception propagation correctly (the alternative reintroduces the exact double-resume/leaked-continuation bugs the milestone `PITFALLS.md` flags as Pitfall 9's cited hazard). |
| "Is Play Games available on this device at all" | A `GoogleApiAvailability` check before offering sign-in | Nothing — D-12 explicitly collapses every "not signed in" cause (declined, unavailable, offline) into the single `SignedOut` state | The seam already treats a failed `isAuthenticated()`/`signIn()` call as `SignedOut`; adding a pre-flight availability check is speculative extra branching D-12 rules out by design. |
| Cancel-vs-error distinction for `signIn()` | A custom `ApiException` status-code table reverse-engineered from undocumented behavior | Treat conservatively per the Open Questions recommendation below (verify empirically once a Console project exists; default to silent unless a clear non-cancellation exception type is observed) | Google does not document the exact status code split for this call (verified: no official source found this session distinguishing them) — a hand-built table risks silently misclassifying a normal decline as an "SDK error" and spamming players with a snackbar every time they close the account chooser. |
| Session persistence across process restarts | A custom cache of "last known signed-in state" | Nothing — re-run the (opted-in-gated) `isAuthenticated()` check once per process per D-02 | PGS v2 does not expose a sign-out, and a stale cached "SignedIn" state surviving a real sign-out elsewhere (e.g., the player removed the Google account on-device) would show a broken session; the one-check-per-process design is the correct source of truth. |

**Key insight:** Every "don't hand-roll" in this phase reduces to *trust the seam's existing shape and the SDK's own designed behavior* — v2 was deliberately built to need almost no application-side state machine (no sign-out, automatic re-auth, one `isAuthenticated()` call per launch is the whole contract). The temptation this phase should resist is over-engineering availability checks, retry loops, or cancellation taxonomies the SDK was specifically designed to make unnecessary.

## Common Pitfalls

### Pitfall 1: Calling `PlayGamesSdk.initialize()` before checking the opt-in flag
**What goes wrong:** If any code path calls `PlayGamesSdk.initialize(context)` before the persisted `play_games_opted_in` flag is read, the SDK's automatic launch-time authentication attempt fires for players who never opted in — silently defeating D-01's entire premise, possibly surfacing the "Welcome back" popup to a player who declined.
**Why it happens:** Google's own documented pattern calls `initialize()` unconditionally in `Application.onCreate`, and it is easy to copy that shape without adding the opt-in gate around it.
**How to avoid:** The opt-in check must be the outermost gate in `PlayGamesService.bootstrapIfOptedIn()` — read the DataStore flag first, `return` immediately if `false`, and only construct/initialize any SDK object after that.
**Warning signs:** A "Welcome back" popup appearing on a fresh install before the opt-in dialog has ever been answered; `session` transitioning away from `Unknown`/`SignedOut` for a player who never tapped "Sign in".

### Pitfall 2: Treating `signIn()`'s Task failure and a plain decline identically (or the reverse)
**What goes wrong:** D-10/D-18 require the app to distinguish "player closed the Google account chooser" (silent) from "the SDK reported a real error" (snackbar). Neither the official docs nor the class reference fetched this session document the exact `Task<AuthenticationResult>` failure shape for each case (see Open Questions).
**Why it happens:** Google's v2 sign-in samples show only the happy path (`isAuthenticated()` polling); the manual `signIn()` entry point's failure taxonomy is not documented in the pages retrieved this session.
**How to avoid:** Implement conservatively: treat `Task.isSuccessful() == false` uniformly at first, but only surface the D-10 snackbar for exceptions that are clearly *not* a plain user cancellation (e.g., anything other than a `CommonStatusCodes.CANCELED`-family `ApiException`, if that turns out to be the actual shape once tested against a real Console project). Flag this behavior for a device-verification pass once D-04's Console project exists — this is the same shape as Phase 9's own "pre-release checklist" treatment of unverifiable SDK behavior.
**Warning signs:** Every plain decline producing a visible snackbar (over-alerting, directly against D-10); or a genuine SDK error being swallowed silently (under-alerting, leaves the player with no explanation and the Settings row still showing with no feedback that something went wrong).

### Pitfall 3: Blocking the first frame on the launch-time bootstrap
**What goes wrong:** PGS-01 requires "no wall, no pause before they can start a level." If `bootstrapIfOptedIn()` is called with `await`/blocking semantics from `MainActivity.onCreate` instead of fire-and-forget, the SDK's `isAuthenticated()` network round-trip (or the account-chooser UI on first sign-in) could visibly delay `setContent`.
**Why it happens:** It is tempting to make `bootstrapIfOptedIn()` `suspend` and await it "to be safe," mirroring how `signIn()` itself is legitimately suspend-and-awaited when called from a button tap.
**How to avoid:** `bootstrapIfOptedIn()` is `fun`, not `suspend`, and internally launches its own coroutine on an injected `CoroutineScope` (the existing `@ApplicationScope` qualifier, same as `AppForegroundTracker`/`ChallengeProgress`) — exactly the shape `ConsentGateway.bootstrap()` already uses for the same reason.
**Warning signs:** Any `runBlocking` or bare `await()`-on-the-main-thread call reachable from `MainActivity.onCreate`; a measurable cold-start regression for opted-in players versus non-opted-in ones.

### Pitfall 4: Leaving the Play Games Settings section visible while `session == Unknown`
**What goes wrong:** D-11 requires the section to be hidden entirely (no header, no row, no placeholder) while `Unknown`. A naive `if (signedIn) ... else ...` render (ignoring the third state) would show either a "not signed in" row or a spinner during the opted-in player's brief post-launch check window, which is exactly the flicker D-11 forbids.
**Why it happens:** `GamesSignInState` is a three-member sealed interface, but it is easy to pattern-match only two branches if the type isn't exhaustively `when`-matched.
**How to avoid:** `when (gamesSignInState) { is SignedIn -> ...; SignedOut -> ...; Unknown -> Unit }` — an exhaustive `when` with no `else` branch, so a future addition to the sealed interface fails to compile rather than silently falling through.
**Warning signs:** A visible flash of a sign-in row or blank section immediately after Home/Settings first composes, for an opted-in player, before the bootstrap's `isAuthenticated()` call resolves.

### Pitfall 5: Forgetting the manifest's `com.google.android.gms.games.APP_ID` meta-data
**What goes wrong:** `PlayGamesSdk.initialize()` and every `PlayGames.get*Client()` call will fail (or throw) at runtime without this tag pointing at the Games services project ID — verified absent from the current `AndroidManifest.xml` this session (only the AdMob `APPLICATION_ID` meta-data and the `PlayGamesInitProvider` removal `<provider>` tag exist today).
**Why it happens:** Phase 6 built the `game_services_project_id` string resource (via `resValue` per build type) specifically for this tag but did not add the manifest entry itself, since nothing consumed it yet — the resource shrinker was even expected to drop it until this phase wires the manifest (per the `app/build.gradle.kts` comment: "Nothing consumes these two resources until Phase 9 wires the manifest" — that comment is stale for the games ID specifically; Phase 9 wired `admob_app_id`, and this phase is what finally wires `game_services_project_id`).
**How to avoid:** Add `<meta-data android:name="com.google.android.gms.games.APP_ID" android:value="@string/game_services_project_id" />` inside `<application>`, following the exact pattern of the existing `admob_app_id` `<meta-data>` tag immediately above the `PlayGamesInitProvider` removal comment.
**Warning signs:** A resource-shrinker warning about an unused string persisting past this phase; a "missing games project ID" crash the first time `PlayGamesSdk.initialize()` actually runs (which, since it is opt-in-gated, may not surface until a real device tester accepts the prompt — another reason D-04's emulator-verification checklist item matters).

## Code Examples

### `GamesSignInState` with the D-07 `displayName` addition
```kotlin
// Source: modeled directly on the shipped app/src/main/java/com/app/pegsolitaire/platform/games/GamesSignInState.kt
sealed interface GamesSignInState {
    data object Unknown : GamesSignInState
    data object SignedOut : GamesSignInState

    /**
     * The player is connected as [playerId]. [displayName] is fetched once at sign-in
     * via PlayersClient (D-07) and is null when that lookup failed or stalled — D-14
     * requires this to NOT be reported as a sign-in failure and to NOT keep the state
     * at Unknown; the Settings row falls back to "Signed in to Play Games".
     */
    data class SignedIn(
        val playerId: String,
        val displayName: String? = null,
    ) : GamesSignInState
}
```

### `PlayGamesPreferences` — the D-17 persistence keys, modeled on `OnboardingPreferences`
```kotlin
// Source: modeled directly on app/src/main/java/com/app/pegsolitaire/data/OnboardingRepository.kt's OnboardingPreferences object
internal object PlayGamesPreferences {
    val PROMPT_ANSWERED_KEY = booleanPreferencesKey("play_games_prompt_answered")
    const val DEFAULT_PROMPT_ANSWERED = false

    val OPTED_IN_KEY = booleanPreferencesKey("play_games_opted_in")
    const val DEFAULT_OPTED_IN = false
}
```

### The D-07 display-name fetch, sequenced after `isAuthenticated()` succeeds
```kotlin
// Source: kotlinx-coroutines-play-services 1.11.0 .await() bridging PlayGames v2 Task<T> APIs
// (PlayersClient.getCurrentPlayer() usage pattern per developers.google.com reference index)
private suspend fun resolveSignedInState(activity: Activity, playerId: String): GamesSignInState.SignedIn {
    val displayName = runCatching {
        PlayGames.getPlayersClient(activity).getCurrentPlayer().await().displayName
    }.getOrNull() // D-14: a failed/stalled name lookup degrades to null, never to a sign-in failure
    return GamesSignInState.SignedIn(playerId = playerId, displayName = displayName)
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|---------------|--------|
| Play Games Services v1: `GoogleSignInClient`, `Games.getXxxClient()`, explicit sign-out button | Play Games Services v2: automatic launch-time auth via `PlayGamesSdk.initialize()` + `isAuthenticated()`, no sign-out API at all | v1 APIs began removal from the SDK starting May 2026 (per milestone `PITFALLS.md`, already verified against `play-services-games:25.0.0`'s AAR) | A "Sign out" Settings row, or an always-visible "Sign in" button shown to already-signed-in players, are both v1-shaped mistakes this phase must not reintroduce — CONTEXT.md's D-01/D-06 already correctly avoid both. |
| `play-services-auth` / Google Sign-In dependency for PGS | Removed entirely as of `play-services-games-v2` 21.0.0; no Drive scope, no separate auth dependency | 2026 (pre-milestone; confirmed by both the milestone `STACK.md` research and this session's live Maven metadata check showing 21.0.0 as the version immediately preceding the current 22.0.0) | Nothing to migrate here — this project never had a v1 integration. |

**Deprecated/outdated:** The milestone `ARCHITECTURE.md`'s `play`/`nosdk` flavor-dimension recommendation for keeping JVM tests SDK-free is superseded by the Phase 9/10 precedent (unconditional real-implementation binding + zero Hilt-component tests). Treat that specific recommendation in `ARCHITECTURE.md` as outdated; the rest of that document's Play Games sketch (component table, file structure) remains accurate and is the basis for this phase's Recommended Project Structure above.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|----------------|
| A1 | `PlayGamesSdk.initialize()` is the call that triggers v2's "always attempt to authenticate the user" behavior described in official docs, rather than `isAuthenticated()` itself being what triggers a fresh attempt on each call | Summary, Pattern 1, Pitfall 1 | If `isAuthenticated()` (not `initialize()`) is actually what triggers the attempt, then D-01's mechanism (defer `initialize()`) still works correctly by construction — the interface never calls `isAuthenticated()` either until the opt-in gate passes — so this assumption changes the *explanation* but not the *correctness* of the design. Low practical risk; flagged because the official docs (fetched this session) do not spell out the exact trigger with full precision. See Open Questions #1. |
| A2 | `GamesSignInClient.signIn()`'s `Task<AuthenticationResult>` failure is distinguishable between "user cancelled the account chooser" and "genuine SDK error," and this distinction is knowable by inspecting the `Task`'s exception type | Pitfall 2, Don't Hand-Roll | Undocumented by Google in every source checked this session. If no reliable distinction exists, D-10's "plain dismiss is silent / SDK error gets a snackbar" contract cannot be implemented exactly as specified without empirical device testing against a real PGS Console project (which does not exist yet per D-04). Recommend the planner insert a `checkpoint:human-verify` or defer final classification logic to the D-04 pre-release checklist. |
| A3 | Binding `PlayGamesService` unconditionally in `PlatformModule` (no per-build-type split) keeps `./gradlew check` SDK-free for PLAT-07, on the same basis as `AdMobAdGateway` | Summary, Standard Stack (Alternatives Considered), Anti-Patterns | Verified this session by `grep`ing `app/src/test` for `HiltAndroidTest`/`DaggerApp_HiltComponents` (zero matches outside the ArchUnit rule file itself) and by reading `NavFlowTest.kt`'s own KDoc, which states explicitly that Hilt's real component is never constructed in tests. HIGH confidence, not LOW — included here for completeness since it corrects a milestone document, not because the evidence is weak. |
| A4 | The "Welcome back" popup and the sign-in account-chooser Activity do not disturb `ProcessLifecycleOwner`/`AppForegroundTracker`'s session-boundary tracking, on the same basis as Phase 10's confirmed-on-emulator interstitial finding (A1 in `10-RESEARCH.md`) | Common Pitfalls (implicitly — no pitfall needed because this phase does not depend on the interstitial cadence at all per D-02) | Low risk: this phase's design (D-02 — no re-check on foreground return) means `AppForegroundTracker`'s state is never read or written by anything in this phase, so even if this assumption were wrong it would have zero effect on PGS-01/02/03. Included for completeness since research question 4 explicitly asked for it. |

## Open Questions (RESOLVED)

> RESOLVED 2026-09-03 at planning: Q1 and Q3 are answered by their own recommendations and encoded in plan 11-02; Q2 (signIn() cancel-vs-error Task shape) is implemented conservatively in plan 11-02 Task 2 and tracked as a Manual-Only Verification in 11-VALIDATION.md until a Play Console project exists (D-04).

1. **Does `PlayGamesSdk.initialize()` itself trigger the automatic sign-in attempt, or does calling `isAuthenticated()` trigger a fresh attempt each time it's called?**
   - What we know: Official docs state "When your game launches, it will always attempt to authenticate the user," immediately followed by sample code that calls `isAuthenticated()` to *verify* the result. The milestone's own `PITFALLS.md` independently describes the flow the same way: `PlayGamesSdk.initialize(context)` (or the now-removed `PlayGamesInitProvider`) → `isAuthenticated()`, framing `initialize()` as the trigger and `isAuthenticated()` as the read.
   - What's unclear: Google's page does not state in one sentence "calling `initialize()` is what starts the attempt." It is consistent with either reading.
   - Recommendation: Treat as answered for planning purposes (A1 above) — D-01's design is correct either way, since both `initialize()` and `isAuthenticated()` stay behind the same opt-in gate. No blocking action needed; do not spend implementation time trying to prove the exact trigger mechanism.

2. **What is the exact `Task<AuthenticationResult>` shape when a player dismisses the account chooser vs. when a genuine SDK error occurs (D-10)?**
   - What we know: `signIn()` returns `Task<AuthenticationResult>`; the reference index page confirms the method's existence and general shape but no fetched source documents the failure taxonomy.
   - What's unclear: Whether `Task.getException()` on a plain dismiss is `null` (with `AuthenticationResult.isAuthenticated() == false`) or a non-null `ApiException`, and if the latter, what status code family distinguishes it from a real error (network failure, misconfigured Console project, etc.).
   - Recommendation: Flag for a `checkpoint:human-verify` once emulator sign-in becomes possible against a real Console project (D-04's own pre-release checklist item already covers this window). Until then, implement the conservative default described in Pitfall 2 and document the uncertainty inline in `PlayGamesService`'s KDoc, following this codebase's own convention of naming open uncertainty in comments (see `AppForegroundTracker`'s A1 precedent).

3. **Does the resource shrinker's current behavior toward `game_services_project_id` change once this phase's manifest edit lands?**
   - What we know: `app/build.gradle.kts`'s comment says the resource "may be dropped from release until then — expected," referring to the pre-Phase-9/11 state where nothing consumed either `admob_app_id` or `game_services_project_id`. Phase 9 already wired `admob_app_id` into the manifest.
   - What's unclear: Nothing functionally — this is a documentation-staleness note, not a risk. Once this phase adds the `<meta-data>` tag, the resource becomes referenced and R8/resource-shrinking will retain it like `admob_app_id` already is.
   - Recommendation: Update the stale build.gradle.kts comment as part of this phase's manifest-wiring task (small housekeeping item, not a blocker).

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit4 4.13.2 + Robolectric (transitive via Roborazzi tooling, though no Roborazzi test exists in the repo yet) + Compose UI Test (`ui-test-junit4`) + Turbine 1.2.1 + Google Truth 1.4.x + MockK 1.14.x — all already present in the project's test dependencies |
| Config file | `app/build.gradle.kts` (test dependencies + `Config(sdk = [ROBOLECTRIC_SDK])` pattern used throughout `app/src/test`) |
| Quick run command | `./gradlew :app:testDebugUnitTest --tests "com.app.pegsolitaire.platform.games.*"` (scoped to the new package during development) |
| Full suite command | `./gradlew check --offline` (per Phase 6 D-18's established offline invariant) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|--------------------|--------------|
| PGS-01 | Opted-in player's `bootstrapIfOptedIn()` reaches the SDK; non-opted-in player's does not | unit | `./gradlew :app:testDebugUnitTest --tests "*.PlayGamesServiceTest"` | ❌ Wave 0 |
| PGS-01 | Home shows the opt-in dialog exactly once, only after `onboardingVisible == false`, and never again once `play_games_prompt_answered == true` | unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.HomeViewModelTest"` | ✅ (extend existing `HomeViewModelTest.kt`) |
| PGS-01 | Accepting the dialog sets both persisted flags and calls `signIn()`; declining sets both flags without calling `signIn()` | unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.HomeViewModelTest"` | ✅ (extend existing) |
| PGS-02 | `NoOpGamesService`/a fake `GamesService` reporting `SignedOut` leaves every existing surface unchanged (no crash, no dependent UI rendered) | unit | `./gradlew :app:testDebugUnitTest --tests "*.NoOpGamesServiceTest"` | ✅ (existing, unaffected by this phase) |
| PGS-02 | Settings Play Games section is absent while `Unknown`, shows the sign-in row while `SignedOut`, shows "Signed in as X" while `SignedIn` | unit (ViewModel) + Compose UI test | `./gradlew :app:testDebugUnitTest --tests "*.SettingsViewModelTest"` and a `SettingsScreenTest` Compose UI test | ✅ ViewModel test exists (extend); ❌ Compose UI test for the new section — Wave 0 |
| PGS-03 | Tapping the manual Settings row calls `signIn()` and sets both D-19 flags | unit (ViewModel) | `./gradlew :app:testDebugUnitTest --tests "*.SettingsViewModelTest"` | ✅ (extend existing) |
| PGS-03 | An SDK-reported `signIn()` error surfaces the D-10 snackbar; a plain decline does not | unit (ViewModel) + Compose UI test | `./gradlew :app:testDebugUnitTest --tests "*.SettingsViewModelTest"` | ❌ Wave 0 — needs a fake `GamesService.signIn()` that can return both outcomes |

### Sampling Rate
- **Per task commit:** `./gradlew :app:testDebugUnitTest --tests "com.app.pegsolitaire.platform.games.*"` plus whichever ViewModel/Screen test package the task touched
- **Per wave merge:** `./gradlew check --offline`
- **Phase gate:** Full suite green before `/gsd:verify-work`, plus `./gradlew detekt ktlintCheck` (both already wired into `check`)

### Wave 0 Gaps
- [ ] `app/src/test/java/com/app/pegsolitaire/platform/games/PlayGamesServiceTest.kt` — covers PGS-01 (opted-in-gated bootstrap), the `Unknown → SignedOut`/`SignedIn` transition, and D-14's null-displayName fallback, using a fake/mocked `Activity` + `ForegroundActivityProvider` per the existing `UmpConsentGateway`-adjacent test patterns (check `AdMobAdGateway`'s own test file, if one exists, for the closest precedent of testing a platform class that awaits an Activity)
- [ ] `app/src/test/java/com/app/pegsolitaire/data/PlayGamesOptInRepositoryTest.kt` — covers D-17's persistence contract, modeled on any existing `OnboardingRepositoryTest.kt`
- [ ] A fake `GamesService` implementation (test-only, distinct from `NoOpGamesService`) capable of emitting all three `GamesSignInState` values and simulating both `signIn()` outcomes (success / silent-decline / SDK-error) for `HomeViewModelTest` and `SettingsViewModelTest` to consume
- [ ] Compose UI test coverage for `PlayGamesPromptDialog` (dismiss-on-outside-tap, dismiss-on-back, both buttons wired) and the new Settings Play Games section's three-state rendering — no Roborazzi screenshot tests exist anywhere in this repo yet, so a plain `createComposeRule()` semantics-based test (matching `GameScreenAdOfferTest.kt`'s style) is the precedent to follow, not a new screenshot-testing setup

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|----------------|---------|-------------------|
| V2 Authentication | Yes, but delegated | Authentication itself is entirely Google's — this app never handles credentials, tokens, or passwords. The only app-side responsibility is *not* forcing authentication (already the entire point of D-01/D-16) and correctly reflecting the SDK's own authenticated state. |
| V3 Session Management | Minimal | No app-managed session token exists; `GamesSignInState` is a read-only mirror of the SDK's own session, re-derived once per process (D-02), never persisted by this app beyond the two opt-in booleans. |
| V4 Access Control | Not applicable | No access-control decision in this app depends on sign-in state — every mode/board/feature stays available regardless (PGS-02 is itself an access-control non-discrimination requirement, already the phase's core design). |
| V5 Input Validation | Minimal | The one untrusted input surfacing into app state is `PlayersClient.getCurrentPlayer().displayName`, a free-text string from Google's own service, rendered directly into a Settings row via `stringResource`-style Compose `Text` (auto-escaped, no HTML/markup interpretation) — no additional validation needed beyond what Compose's `Text` composable already provides. |
| V7 Logging & error handling | Yes | Following the established `UmpConsentGateway`/`AdMobAdGateway` precedent: nothing in `PlayGamesService` logs the player ID, display name, or any consent/session detail. Failures degrade silently to `SignedOut` or `displayName = null`, never to a crash or a logged personal-data-adjacent value. |
| V8 Data Protection | Yes | `playerId` and `displayName` are held only in-memory (the `StateFlow`) — this phase persists no player-identifying data to disk. The two DataStore keys this phase adds (`play_games_prompt_answered`, `play_games_opted_in`) are non-identifying booleans. |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|-----------------------|
| Forced/nagging re-prompt for sign-in (dark pattern, also a Play policy risk) | — (product/policy, not a technical STRIDE category, but worth naming given the milestone's own "Out of Scope" table explicitly bans it) | D-13/D-17: the opt-in prompt is genuinely one-time; the Settings row is the only re-entry point and never nags. |
| Untrusted client-side `playerId` used for a security decision | Spoofing | Not applicable here — this app never sends `playerId` to a backend or uses it for any authorization decision (no backend exists at all in this milestone); Google's own documented warning about not trusting the client-side player ID server-side is therefore moot for this codebase. |
| Logging PII (display name, player ID) during debugging | Information Disclosure | Same "nothing logs" discipline already enforced by grep-gate convention across `UmpConsentGateway`/`AdMobAdGateway`/`AppForegroundTracker` — extend the same convention (and, if one exists, the same grep-based build gate) to `PlayGamesService`. |

## Project Constraints Cross-Check

Every recommendation above was checked against `./CLAUDE.md`'s mandatory conventions: no magic strings (all new DataStore keys and UI strings are scoped-constants-or-`strings.xml`), dimens-only spacing (the new dialog and Settings row reuse existing `dimens.xml` tokens per the `AdOfferDialog`/`SettingsScreen` precedent, no phase-specific new dimens anticipated), state hoisting (both new UI surfaces are stateless renders of ViewModel-owned state), and SDK isolation (`platform/games/` is the only package this phase adds a `com.google.android.gms.games.*` import to).

## Sources

### Primary (HIGH confidence)
- Live `curl` against `dl.google.com/android/maven2/.../play-services-games-v2/maven-metadata.xml` — confirms 22.0.0 is still current as of this session (2026-09-03), unchanged since 2026-07-29
- Live `curl` against `repo1.maven.org/maven2/.../kotlinx-coroutines-play-services/maven-metadata.xml` — confirms 1.11.0 exists and is `<latest>`/`<release>`
- Local codebase read directly (all files listed in `<additional_context>`'s file list): `GamesService.kt`, `GamesSignInState.kt`, `NoOpGamesService.kt`, `NoOpGamesServiceTest.kt`, `ForegroundActivityProvider.kt`, `DefaultForegroundActivityProvider.kt`, `AppForegroundTracker.kt`, `MainActivity.kt`, `App.kt`, `di/PlatformModule.kt`, `ConsentGateway.kt`, `UmpConsentGateway.kt`, `AdOfferDialog.kt`, `ClaySnackbarHost.kt`, `data/OnboardingRepository.kt`, `data/AppPreferences.kt`, `data/SettingsRepository.kt`, `PlatformBoundaryRules.kt`, `ui/home/HomeUiState.kt`, `ui/home/HomeViewModel.kt`, `ui/home/HomeScreen.kt`, `ui/home/HomeRoute.kt`, `ui/settings/SettingsScreen.kt`, `ui/settings/SettingsUiState.kt`, `ui/settings/SettingsViewModel.kt`, `ui/settings/SettingsViewModelTest.kt`, `AndroidManifest.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `navigation/NavFlowTest.kt`, `platform/ads/AdMobAdGateway.kt`, `platform/ads/AdIds.kt`
- `.planning/research/PITFALLS.md`, `.planning/research/ARCHITECTURE.md`, `.planning/research/STACK.md` — milestone research, read in full this session

### Secondary (MEDIUM confidence)
- `developer.android.com/games/pgs/android/android-signin` (fetched this session) — "When your game launches, it will always attempt to authenticate the user"; `isAuthenticated()` sample flow; the "Play Games Services doesn't require a manual Sign in button" key-point callout
- `developer.android.com/games/pgs/branding` (search-result summary) — "The 'Welcome back' pop-up should be shown after sign-in and never interrupted by other game or player activities"
- WebSearch summary of `developers.google.com/android/reference/.../PlayersClient` and `GamesSignInClient` reference pages — confirms `getCurrentPlayer()` → `Task<Player>` → `.getPlayerId()`/`.getDisplayName()` shape and `signIn()` → `Task<AuthenticationResult>` shape, without full javadoc text

### Tertiary (LOW confidence)
- No source found (this session) documenting the exact `Task<AuthenticationResult>` failure/exception shape distinguishing user-cancellation from SDK error on `signIn()` — see Open Questions #2 / Assumption A2

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — both new/confirmed dependency versions verified live against their authoritative registries this session, not merely recalled from training data or the (now nine-day-old) milestone research
- Architecture: HIGH — every integration point (seam shape, Activity bridge, DI module, manifest state, DataStore pattern) was read directly from the current repository, not inferred
- Pitfalls: MEDIUM-HIGH — four of five pitfalls are grounded in direct repository evidence or official docs; Pitfall 2 (cancel-vs-error) is explicitly flagged LOW/undocumented and carried into Open Questions rather than asserted as fact

**Research date:** 2026-09-03
**Valid until:** 2026-10-03 (30 days — the SDK pins are stable and the in-repo findings are load-bearing facts unlikely to change; only the undocumented `signIn()` failure-shape question benefits from re-verification once a Play Console project exists per D-04)
