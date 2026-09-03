# Phase 11: Play Games Session - Context

**Gathered:** 2026-09-03
**Status:** Ready for planning

<domain>
## Phase Boundary

This phase binds a **real `GamesService` implementation** behind the existing seam and delivers the **sign-in session only**: a one-time, dismissible opt-in prompt on Home followed by silent automatic Play Games v2 sign-in on every later launch for players who accepted (PGS-01), a game that is fully playable when sign-in is unavailable or declined with Play-Games-dependent surfaces hidden rather than broken (PGS-02), and a manual sign-in row in Settings that appears only while the player is signed out (PGS-03).

**Design revision (2026-09-03, during planning):** the operator replaced the original "silent sign-in for everyone, never asked" design with **explicit opt-in**. PGS-01, the ROADMAP goal/success criterion 1, and D-01/D-02 below were rewritten accordingly; D-15 to D-19 are new. Everything else stands.

**Not this phase:** leaderboard/achievement submission and the stock Play Games UI intents (Phase 12), Saved Games / cloud save (Phase 13, behind `ProgressRepository`, never on `GamesService`), Play Console declarations and signed-track verification (Phase 14). The seam's `submitScore` / `unlockAchievement` / `incrementAchievement` / `showLeaderboard` / `showAchievements` methods may be implemented here if it is cheaper than leaving stubs, but nothing in `ui/` calls them until Phase 12.

</domain>

<decisions>
## Implementation Decisions

### Sign-in timing
- **D-01 (revised):** Sign-in is **explicit opt-in**. Until the player has opted in (D-15/D-19), **nothing Play-Games-related runs**: no `PlayGamesSdk.initialize`, no `isAuthenticated` check, no SDK UI, and `session` resolves straight to `SignedOut`. On every launch, **`MainActivity.onCreate`, immediately after `consentGateway.bootstrap()`, fire-and-forget**, hands the bootstrap to the platform implementation, which reads the persisted opt-in flag (D-17) and — **if and only if the player previously opted in** — runs `PlayGamesSdk.initialize` plus the silent `isAuthenticated` check, the same shape as the consent bootstrap. Every PGS client getter needs an `Activity`; `MainActivity` is the single Activity, and the platform implementation still reaches it only through `ForegroundActivityProvider` (never a stored Activity). Nothing about sign-in may block or delay the first frame or the first tap. The researcher must confirm whether `PlayGamesSdk.initialize` itself triggers the SDK's automatic sign-in attempt in v2 — if it does, deferring `initialize` until opt-in is what keeps the SDK silent for non-opted-in players.
- **D-02 (revised):** The silent check runs **once per process** for opted-in players. `session` changes afterwards **only** through (a) accepting the first-launch prompt (D-18) and (b) the manual `signIn()` path from Settings (D-10). No re-check on foreground return, no connectivity listener.
- **D-03:** Play Games init is **independent of the UMP consent flow**. CONS-02 governs the ads SDK only; the published privacy policy (Phase 9 D-19) already discloses optional Play Games. Sign-in never waits on `AdsReadiness`.
- **D-04:** **No Play Games project exists in Play Console yet** (no PGS project on the listing, no OAuth credentials, no tester list). The phase is therefore verified by **JVM tests against a fake `GamesService`** plus the platform implementation's own unit tests; **emulator sign-in verification is a pre-release checklist item**, recorded as human-needed debt exactly as Phase 10 recorded its physical-device run — not silently skipped, and not a blocker for planning or execution. The operator checklist gains: create the PGS project on the existing listing, register OAuth credentials for the **debug-keystore SHA-1 and the original release-keystore SHA-1** (Phase 6 D-21 — no Play App Signing key exists yet), add a tester account.

### Signed-in feedback
- **D-05:** The stock Play Games **"Welcome back" popup stays, in its default position**. No gravity change, no suppression attempt. It is the only launch-time signal that sign-in happened, and that is intended.
- **D-06:** When signed in, Settings shows a **non-tappable "Signed in as <display name>" row** in the Play Games section, replacing the sign-in row.
- **D-07:** `GamesSignInState.SignedIn` gains **`displayName: String?`**, fetched **once at sign-in** (via `PlayersClient`) inside the platform implementation and carried in the state flow. This **supersedes** the initial "playerId only" pick made in the same discussion — the Settings choice (D-06) needs the name, and one source of truth in the flow beats a second seam method that Settings would re-query on every open.

### Manual sign-in row
- **D-08:** Settings gains a **new "Play Games" section, between General and Privacy**, using the existing `SettingsSectionHeader` + `SettingsLinkRow` shapes. Privacy stays pinned at the bottom (Phase 9 D-14). Phase 12's leaderboard/achievement rows land in this same section.
- **D-09:** Row copy: label **"Sign in to Play Games"**, supporting **"Leaderboards, achievements and cloud save"**.
- **D-10:** Tapping the row calls `signIn()`. **On an SDK-reported error, a snackbar** ("Couldn't sign in to Play Games" — exact wording at planner's discretion) via the existing `ClaySnackbarHost`. **A plain dismiss/decline by the player is silent.** The row remains in both cases.

### Opt-in prompt (new, 2026-09-03)
- **D-15:** A **one-time, app-owned dialog** asks the player whether to sign in to Play Games. It is shown on the **Home screen** after the first frame, and **only once the first-run onboarding overlay has been dismissed** (`HomeUiState.onboardingVisible == false`, ONB-01) so two first-run surfaces never stack. It is a Material 3 `AlertDialog` in the existing clay style, modeled on `ui/game/AdOfferDialog.kt`, driven by a `playGamesPromptVisible` flag in `HomeUiState` owned by `HomeViewModel` — state hoisted, no SDK types in `ui/`.
- **D-16:** The dialog is **dismissible and never a wall**: two actions — a positive **"Sign in"** and a negative **"Not now"**; tapping outside or pressing back counts as "Not now". It is never shown over gameplay, Results, or Settings — Home only. Title/body copy is at the planner's discretion but must name the same three benefits as the Settings row (leaderboards, achievements, cloud save) and must make clear the game is fully playable without signing in.
- **D-17:** The answer is **persisted in the shared app DataStore** (`AppPreferences` store, `data/AppPreferences.kt`) through a small preferences repository modeled on `data/OnboardingRepository.kt`, with two boolean keys: `play_games_prompt_answered` and `play_games_opted_in`. "Not now" sets answered = true, opted-in = false, and **the prompt never appears again** — the Settings row (D-08 to D-10) is the only later way in. "Sign in" sets answered = true, opted-in = true. No per-session flags; the flags are the single source of truth for D-01's launch branch.
- **D-18:** Accepting the prompt **immediately** runs `PlayGamesSdk.initialize` + `signIn()` through the seam; the SDK's own account chooser may appear as the Google-owned second step, and that is expected. Later launches then take D-01's silent path. If the SDK reports an error on that first sign-in, the D-10 snackbar wording is shown on Home via `ClaySnackbarHost` (Home gains a snackbar host if it has none); opted-in stays `true` so the next launch retries silently, and the Settings row is available meanwhile. A plain dismiss of the SDK's chooser is silent (D-10 parity).
- **D-19:** Tapping the manual Settings row (D-10) **also** sets `play_games_opted_in = true` and `play_games_prompt_answered = true`, so a player who signs in from Settings gets silent sign-in on later launches and never sees the prompt. For a player who has not opted in, or who declined, `session` is `SignedOut` from launch with the SDK untouched, and the Settings row shows (D-08).

### Unknown and offline states
- **D-11:** While `session` is `Unknown` (the opted-in player's first check pending), the **Play Games section is hidden entirely** — no header, no row, no placeholder. It appears only once the state resolves to `SignedOut` or `SignedIn`. Players who have not opted in are never `Unknown` (D-19).
- **D-12:** For opted-in players, missing Google Play services / Play Games app, or an offline device at launch, all read as **`SignedOut`**: the sign-in row is shown, and a tap that cannot succeed surfaces the D-10 snackbar. One state for every "not signed in" cause; no `GoogleApiAvailability` gating, no connectivity retry.
- **D-13:** After an explicit decline from the manual row, **the row stays**. The SDK rate-limits its own prompts; the app keeps no per-session "declined" flag.
- **D-14:** If authentication succeeds but the display-name lookup fails or stalls, the state is **`SignedIn(playerId, displayName = null)`** and Settings shows the fallback **"Signed in to Play Games"**. A name lookup failure is never reported as a sign-in failure and never keeps the section in `Unknown`.

### Claude's Discretion
- Exact snackbar wording and the exact supporting text of the signed-in row; all strings go through `strings.xml`, all spacing through `dimens.xml`, per project standards.
- Internal structure of the real implementation (`PlayGamesService` naming, whether the session `StateFlow` lives on the service or a small `GamesSession` holder, `Task` → `suspend` bridging — research proposed `kotlinx-coroutines-play-services` 1.11.0, version-locked to the coroutines pin), threading/dispatchers for the SDK calls, and how the display-name fetch is sequenced after authentication.
- Whether the debug build type binds the real implementation or keeps `NoOpGamesService` — constrained by D-04 (no credentials exist, so a real debug sign-in cannot succeed today) and by the JVM suite staying SDK-free (PLAT-07).
- Test doubles: the shape of the fake `GamesService` used by `SettingsViewModel` tests, and whether `NoOpGamesService` remains the preview/test default.
- Whether the seam's Phase 12 methods get real bodies now or remain no-ops in the real implementation until Phase 12.
- Exact title/body copy of the opt-in dialog (D-16) and whether the opt-in flags live on `SettingsRepository` or a new dedicated repository (D-17) — both go through the shared `AppPreferences` store either way.
- Where the "opted in?" branch of D-01 lives: inside the real `GamesService` implementation (preferred — keeps `MainActivity` to a one-line bootstrap) or in a small bootstrap helper.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase scope and requirements
- `.planning/ROADMAP.md` — Phase 11 entry: goal, success criteria, and the note "PGS v2 has no sign-out and no sign-in wall; do not port v1 patterns"; External Prerequisites table (PGS project + dual SHA-1 credentials + tester list).
- `.planning/REQUIREMENTS.md` — PGS-01, PGS-02, PGS-03 (the only requirements this phase closes); PLAT-01/PLAT-03/PLAT-07 for the seam and boundary constraints it must preserve.

### Prior decisions this phase builds on
- `.planning/phases/06-platform-foundation-build-hardening/06-CONTEXT.md` — D-07/D-17 (`PlayGamesInitProvider` removed; init is explicit and app-chosen), D-08 (IDs in `local.properties` + CI secret, never committed), D-14 (SDK artifacts already on the graph), D-21 (signing continuity: debug SHA-1 + original keystore SHA-1, no Play App Signing yet), D-22 (existing listing, existing developer account).
- `.planning/phases/09-consent-rewarded-ads/09-CONTEXT.md` — D-14 (Settings Privacy section pinned at the bottom), D-15 (external-intent pattern), D-19 (live privacy policy URL that already discloses optional Play Games).
- `.planning/phases/10-interstitials/10-CONTEXT.md` — `AppForegroundTracker` semantics and RESEARCH assumption A1 (same-process fullscreen SDK Activities and `ProcessLifecycleOwner`); the researcher must state whether the Play Games sign-in prompt or welcome popup can trip the interstitial cadence's foreground tracking.

### Milestone research
- `.planning/research/PITFALLS.md` — Verified Baseline (every `PlayGames.get*Client` takes an `Activity`; `PlayGamesSdk.initialize` takes a `Context`), Pitfall 1 (Activity provider), Pitfalls 7–8 (`isAuthenticated()` false on every account when the Console project/credentials are wrong; SHA-1 is the keystore actually signing the build).
- `.planning/research/ARCHITECTURE.md` — `PlayGamesService` / `GamesSession` component sketch, `App`/`MainActivity` bootstrap roles, the rule that `GamesService` is consumed at zero call sites today.
- `.planning/research/STACK.md` — `play-services-games-v2` 22.0.0 pin and `kotlinx-coroutines-play-services` 1.11.0 bridge.

### Code standards
- `CLAUDE.md` — no magic strings/numbers, dimens resource file, state hoisting, detekt + ktlint gates, Gitflow.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `app/src/main/java/com/app/pegsolitaire/platform/games/GamesService.kt` — the seam: `session: StateFlow<GamesSignInState>`, `suspend signIn(): Boolean`, submission methods, two UI-intent methods. Shape is fixed; D-07 extends `SignedIn` only.
- `app/src/main/java/com/app/pegsolitaire/platform/games/GamesSignInState.kt` — `Unknown` / `SignedOut` / `SignedIn(playerId)`; gains `displayName: String?` (D-07).
- `app/src/main/java/com/app/pegsolitaire/platform/games/NoOpGamesService.kt` + `app/src/test/java/com/app/pegsolitaire/platform/games/NoOpGamesServiceTest.kt` — the stub and its test; the model for the phase's fake.
- `app/src/main/java/com/app/pegsolitaire/platform/activity/ForegroundActivityProvider.kt` (+ `DefaultForegroundActivityProvider.kt`) — `current: Activity?`, `suspend awaitActivity()`; the only sanctioned Activity source for SDK calls.
- `app/src/main/java/com/app/pegsolitaire/platform/lifecycle/AppForegroundTracker.kt` — Phase 10 foreground tracking; check interaction with the sign-in prompt (see canonical refs).
- `app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt` — `SettingsSectionHeader`, `SettingsLinkRow(label, supporting, trailingIcon, onClick)`, `SettingsToggleRow`; the Privacy section (lines ~183–250) is the template for the new Play Games section.
- `app/src/main/java/com/app/pegsolitaire/ui/components/ClaySnackbarHost.kt` — themed snackbar host already used by Settings for the privacy-policy browser error (D-10 reuses it; D-18 reuses it on Home).
- `app/src/main/java/com/app/pegsolitaire/ui/game/AdOfferDialog.kt` — the existing app-owned Material 3 dialog in the clay style; the model for the opt-in prompt (D-15/D-16).
- `app/src/main/java/com/app/pegsolitaire/data/OnboardingRepository.kt` + `data/AppPreferences.kt` — the first-run `onboardingSeen` flag over the shared DataStore; the model for the `play_games_prompt_answered` / `play_games_opted_in` flags (D-17).
- `app/src/main/java/com/app/pegsolitaire/ui/home/HomeUiState.kt` / `HomeViewModel.kt` / `HomeScreen.kt` / `HomeRoute.kt` — `onboardingVisible` first-run overlay sequencing; the prompt is gated behind it (D-15).
- `app/src/main/res/values/strings.xml` (`settings_section_*`, `settings_privacy_*` naming) and `app/src/main/res/values/dimens.xml` — where new strings and spacing go.

### Established Patterns
- **Seam pattern**: `interface` + `@Inject constructor` impl + `@Binds @Singleton` in `di/PlatformModule.kt` (`bindGamesService(impl: NoOpGamesService)` is the line that changes).
- **Fire-and-forget bootstrap from `MainActivity.onCreate` after `super.onCreate()`** — `consentGateway.bootstrap()` is the precedent D-01 copies; the comment there explains why it must follow Hilt field injection.
- **Platform singleton owns its own state flow** (`SoundPoolPlayer` gate, `ConsentGateway.privacyOptionsRequired`) — `SettingsViewModel` collects platform flows into `SettingsUiState` in `init`; the session flow follows the same collect-into-UiState shape.
- **SDK types never reach `ui/`**: `app/src/test/java/com/app/pegsolitaire/PlatformBoundaryRules.kt` + `PlatformBoundaryGateTest.kt` with `GmsBoundaryViolationFixture.kt` / `ForegroundActivityBoundaryViolationFixture.kt` — `com.google.android.gms.games.*` must stay inside `platform/games/`; the fixtures prove the gate bites.
- **Build-type IDs**: `app/build.gradle.kts` already emits `resValue("string", "game_services_project_id", …)` per build type from `local.properties` / CI env (D-08), with release builds failing fast on unresolved IDs. The manifest still needs the `com.google.android.gms.games.APP_ID` meta-data pointing at that resource — check whether it exists before adding.
- **Manifest**: `PlayGamesInitProvider` is removed via `tools:node="remove"`; the removal comment names Phase 11 as the explicit-init owner.

### Integration Points
- `MainActivity.onCreate` — add the sign-in bootstrap call after `consentGateway.bootstrap()` (D-01); the bootstrap itself branches on the persisted opt-in flag (D-17).
- `ui/home/HomeUiState.kt` / `HomeViewModel.kt` / `HomeScreen.kt` — `playGamesPromptVisible` flag, prompt dialog, accept/decline handlers, Home snackbar for the D-18 error case (D-15 to D-18).
- `data/` — new opt-in preference keys over the shared `AppPreferences` DataStore (D-17).
- `di/PlatformModule.kt` — swap `NoOpGamesService` for the real implementation (per-build-type binding at Claude's discretion, see D-04).
- `ui/settings/SettingsUiState.kt` / `SettingsViewModel.kt` / `SettingsScreen.kt` — Play Games section driven by the collected session state (D-06, D-08–D-14).
- `AndroidManifest.xml` — Play Games `APP_ID` meta-data referencing `@string/game_services_project_id`, if not already present.

</code_context>

<specifics>
## Specific Ideas

- Row copy is fixed by D-09: **"Sign in to Play Games"** / **"Leaderboards, achievements and cloud save"**.
- Signed-in row reads **"Signed in as <display name>"**, falling back to **"Signed in to Play Games"** when the name is absent (D-06, D-14).
- The stock "Welcome back" popup is a feature, not a defect — do not spend effort suppressing or relocating it (D-05).
- Opt-in dialog actions are fixed by D-16: positive **"Sign in"**, negative **"Not now"**; "Not now" is permanent (D-17) and the Settings row is the way back in.

</specifics>

<deferred>
## Deferred Ideas

- Leaderboard and achievement rows in the Play Games section, and the stock Play Games UI intents — Phase 12.
- Saved Games / cloud save — Phase 13, behind `ProgressRepository`.
- Play Console PGS project creation, OAuth credentials for both SHA-1s, tester list — operator work that must start now so Phase 12–14 device verification is possible; tracked in ROADMAP External Prerequisites.
- Emulator sign-in verification of PGS-01/PGS-03 — pre-release checklist item once the Console project exists (D-04).

### Reviewed Todos (not folded)
- **Confirm audio cues on physical hardware** — keyword-only match; Phase 7 audio debt, unrelated to sign-in.
- **Upgrade Wiegleb-45 opening book from K=3 to K=4** — keyword-only match; Phase 8 solver debt.
- **Decide whether Stuck finishes should reach Results** — keyword-only match; scoring/flow decision independent of Play Games.

</deferred>

---

*Phase: 11-play-games-session*
*Context gathered: 2026-09-03*
