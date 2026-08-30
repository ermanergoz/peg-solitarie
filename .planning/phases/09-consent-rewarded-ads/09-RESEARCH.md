# Phase 9: Consent & Rewarded Ads - Research

**Researched:** 2026-08-30
**Domain:** Android monetization — GMA Next-Gen rewarded ads + UMP consent, integrated behind an existing Clean-Architecture platform seam
**Confidence:** HIGH (SDK API surface, consent ordering, policy) / MEDIUM (preloader semantics, ad expiry, exact `PreloadConfiguration` shape)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Allowance policy (ADS-02)
- **D-01:** The free allowance is **separate counters per assist type** — free hints AND free undos tracked independently. Protects undo (the habit-formed v1.0 feature) from being drained by hint use. Both counters live in one pure `:domain` policy object per Phase 6 D-04 (`AssistAllowance`-style, modeled on `ScoreCalculator`) — not in the gateway, not in `GameViewModel`.
- **D-02:** Allowance size is **3 hints / 5 undos per level**. Named, scoped constants (no magic numbers).
- **D-03:** A watched rewarded ad **refills the spent counter back to its full allowance** (e.g. 3 more hints). One ad per counter-exhaustion in practice — no per-assist nagging.
- **D-04:** The allowance **persists across retries and process death — it survives everything until the level is won**. Retrying does NOT refill it; counters are carried in the resume snapshot; reset happens only on winning the level (moving to a fresh level) or a rewarded refill. Closes the "retry to farm free assists" loophole. Same 3/5 numbers in Classic and Challenge.
- **Requirement reminder (ADS-06, locked upstream):** a hint that honestly reports "no winning move" is never charged against the allowance or an ad.

#### Opt-in offer UX (ADS-01, ADS-04)
- **D-05:** Hint/undo buttons **never change appearance** — no counter badge, no ad icon. The opt-in dialog is the only signal the allowance system exists. (User explicitly chose this over the recommended always-visible counter badge.)
- **D-06:** The offer is a **themed Compose dialog in the game's (Candy Clay) styling** — not a stock M3 AlertDialog, not a bottom sheet. It names the reward explicitly (ADS-01) with a Watch ad / No thanks pair; declining leaves the game exactly as it was.
- **D-07:** Because there is no button-state signal (D-05), **the dialog copy carries the first-encounter explanation**: e.g. "You've used your 3 free hints for this level — watch a short ad for 3 more?". No one-time intro snackbar, no update-day changelog dialog.
- **D-08:** Rescue-banner integration (ADS-04): the banner's rescue-undo **spends a normal undo — free and instant while undos remain; at 0 the banner action opens the same themed opt-in dialog** (reward: undo refill). One consistent allowance, one dialog, no second offer surface.

#### Ad loading & fail-open (ADS-03, ADS-05, CONS-04)
- **D-09:** Rewarded ad **preloads the moment any counter hits 0** — the earliest point an offer can occur. No ad requests while allowance remains (most levels never request at all).
- **D-10:** If the player accepts before the ad is ready: **in-dialog loading state on the button with a ~5s cap**, then fall open — grant the assist free with the explanation. Bounded wait, single surface, no dead end.
- **D-11:** The ADS-03 "brief explanation" on a free grant (`RewardOutcome.Unavailable`) is a **transient snackbar** (e.g. "Ad unavailable — hints refilled for free") while the assist lands immediately. Not a blocking confirmation, not silent.
- **D-12:** When ads are **knowably unservable** (consent flow failed → CONS-04 fallback, SDK never initialized), **skip the opt-in dialog entirely — assists silently stay free past the allowance**, exactly like v1.0 NoOp behavior. No "watch an ad?" → instant-free-grant absurdity, no snackbar spam.
- **Seam contract reminder (locked, Phase 6 D-01):** `Granted` and `Unavailable` both proceed with the assist; only `Declined` skips. Reward granted only when the ad reports it earned (ADS-05); an unearned early dismiss is `Declined`.

#### Privacy & consent surfaces (CONS-01..03, REL-05)
- **D-13:** The **privacy policy URL is already live** and reachable over HTTPS on the developer website — no authoring/hosting work in this phase; wire it into Settings. (Planner: obtain the exact URL from the user and store it as a build-config value or scoped constant, consistent with PLAT-06 conventions.)
- **D-14:** Settings gains a **"Privacy" section at the bottom**, under the existing Sound/Haptics switches: a "Privacy policy" row always present (REL-05), and a "Manage privacy choices" row beneath it appearing **only when UMP reports a required consent form** (CONS-03).
- **D-15:** The privacy policy opens via **plain external browser intent (`ACTION_VIEW`)** — no Custom Tab dependency, no in-app WebView.
- **D-16:** The UMP consent form shows **at app launch** — gather + show on the first Activity at cold start, before Home. SDK initialization proceeds immediately after consent resolves, off the main thread (CONS-02). Every later surface may assume consent state is resolved.

### Claude's Discretion
- Exact dialog/snackbar/Settings-row wording and the dialog's visual composition (within the Candy Clay theme).
- Clock-freeze mechanics for ADSF-01 (how the ticker pauses across ad presentation; the rescue-undo ticker resume pattern in `GameViewModel` is prior art).
- The resume-snapshot field shape for the persisted counters (D-04) and its backward-compatibility handling for existing snapshots (missing field → treat as fresh full allowance is the obvious fail-open default, consistent with existing decode behavior).
- Ad reload/retry cadence after a failed preload, and mid-show error handling (map to `Unavailable`).
- How "knowably unservable" (D-12) is represented on the gateway (e.g. a queryable readiness/availability signal vs. a fourth outcome) — must not break the sealed three-way `RewardOutcome` contract call sites already handle.
- Decline re-offer behavior (tapping the button again after "No thanks" simply re-opens the dialog — no cooldown was discussed; keep it simple).

### Deferred Ideas (OUT OF SCOPE)
- **Confirm audio cues on physical hardware** — Phase 7 follow-up, unrelated to ads; stays a pending todo.
- **Upgrade Wiegleb-45 opening book to depth 4** — Phase 8 follow-up compute task; stays a pending todo.
- **Decide whether Stuck finishes should reach Results (1–2 star bands unreachable)** — scoring/flow decision independent of ads; touches the same rescue-banner surface but was deliberately kept separate. Planner should avoid structural rework of the banner that would prejudge it.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CONS-01 | Consent information is refreshed each launch and any required consent form is shown before any ad is requested | Pattern 1 (consent bootstrap in `MainActivity.onCreate`), Pattern 2 (`ConsentGateway` seam); `requestConsentInfoUpdate` must run every launch [CITED: developers.google.com/admob/android/privacy] |
| CONS-02 | The ads SDK is initialized only after consent resolves, off the main thread, and no ad request occurs while consent is absent | Pattern 2 + Pitfall 4/5; `MobileAds.initialize` is `@WorkerThread` and mandatory in Next-Gen; `AtomicBoolean` guard |
| CONS-03 | Player can change their privacy choices at any time from a Settings entry that appears only when consent rules require it | Pattern 8; `privacyOptionsRequirementStatus == REQUIRED` → `showPrivacyOptionsForm(activity, listener)` |
| CONS-04 | When the consent flow fails, the app falls back to the previous session's status and simply serves no ads rather than blocking play | Pattern 2 `AdsReadiness.Unavailable`; feeds D-12 via `rewardedOfferable == false` |
| ADS-01 | Player is offered a rewarded ad in an explicit opt-in dialog that names the reward, and can decline — an ad never plays automatically | Pattern 4 (two-step opt-in), Pitfall 6 (NoOp stub semantics must change so the dialog is reachable); AdMob rewarded policy quoted in Security Domain |
| ADS-02 | Player gets a free allowance of hints and undos per level before any ad is offered | Pattern 3 (`AssistAllowance` in `:domain`), Pitfall 1 (persistence vs. restart-clears-snapshot) |
| ADS-03 | Player receives the assist anyway, with a brief explanation, when an ad cannot be loaded or filled | Existing `RewardOutcome.Unavailable` branch (already correct in `GameViewModel`); Pattern 9 (snackbar via tick idiom) |
| ADS-04 | Player can accept a rewarded-ad offer from the "no winning move" rescue banner | D-08 routes the banner's Undo action into the same `onUndo()` funnel — no second offer surface; `NoWinBanner.onUndo` already wired to `GameViewModel.onUndo` |
| ADS-05 | The reward is granted only when the ad reports the reward as earned | Pattern 7: `earned` flag set **only** inside `show(activity) { rewardItem -> }`; dismiss-without-earn resumes `Declined` |
| ADS-06 | A hint that reports "no winning move" is never charged against the allowance or an ad | Pattern 5 — **search first, charge second**; the current code charges before searching, which cannot satisfy ADS-06 |
| ADSF-01 | The game clock pauses for the full duration of any ad presentation and resumes afterwards | Pattern 6 — rebase `elapsedBaseSeconds` + fresh `TimeMark`, wrapping `requestRewarded` only (not the dialog) |
| REL-05 | Player can open the privacy policy from inside the app, from an always-present Settings entry | Pattern 8; `Intent(ACTION_VIEW, uri)` from `LocalContext` (a `Context`, not an `Activity` — required by `PlatformBoundaryRules.uiAndDomainNeverTouchAnActivity`) |
</phase_requirements>

---

## Project Constraints (from CLAUDE.md)

| Directive | Consequence for this phase |
|-----------|----------------------------|
| No magic numbers/strings; per-scope constants objects | `AssistAllowance` sizes (3/5), the ~5 s accept timeout, preload/retry cadence → a scoped `AdConstants`/`AssistAllowanceConstants`. **Exception already established:** ad-unit IDs live in `BuildConfig` via `AdIds`, never in a constants object — enforced by the `adIdLiteralCheck` Gradle task which scans `src/main`. |
| Compose state hoisting; stateless composables | The opt-in dialog, the Settings privacy rows and the snackbar are stateless leaves; `pendingAdOffer` lives in `GameUiState`, privacy-row visibility in `SettingsUiState`. |
| Dimens resource file for all dp/sp | The themed dialog must use `dimensionResource(R.dimen.…)`. `dpSpLiteralCheck` fails the build on a raw `.dp`/`.sp` literal in UI code. |
| detekt + ktlint as a build gate | `./gradlew check` runs `detekt` (with `MagicNumber` on), `ktlintCheck`, `dpSpLiteralCheck`, `adIdLiteralCheck`, `audioFocusLiteralCheck`. Do not add `@Suppress` or a detekt baseline (PITFALLS Pitfall 14). |
| Gitflow | Feature branch off `develop`; no direct commits to `main`/`develop`. |
| Clean Architecture; game logic pure Kotlin | `AssistAllowance` goes in `:domain` (Phase 6 D-04, locked). `DomainPurityTest` forbids `android..`/`androidx..` there. |
| Ads/Play Games behind interfaces | `AdGateway` / `GamesService`; **`PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks` forbids `com.google.android.gms..`, `com.google.android.ump..` and `com.google.android.libraries.ads..` anywhere outside `..platform..`** — bytecode-enforced, so it also catches a Hilt-generated component. |
| CLAUDE.md stack table pins GMA Next-Gen 1.4.0; legacy `play-services-ads` must not return | Confirmed correct — see Standard Stack. |

---

## Summary

Phase 9 has **no new dependencies to add and no new build plumbing to invent**. Phase 6 already landed `ads-mobile-sdk 1.4.0`, `user-messaging-platform 4.0.0` and `play-services-games-v2 22.0.0` on the graph (unreferenced), the per-build-type ad-ID `buildConfigField` plumbing behind `AdIds`, the `INTERNET` permission, R8 full mode, `ForegroundActivityProvider`, and three ArchUnit boundary rules. What is left is genuinely all *code*: a pure `:domain` allowance policy, an opt-in dialog, two Settings rows, a `ConsentGateway`, a real `AdGateway` implementation, and one Hilt binding swap.

The **single most consequential technical finding** is that GMA Next-Gen's rewarded API is meaningfully different from what the milestone research assumed. `RewardedAd` exposes a **coroutine-native `suspend fun load(adRequest: AdRequest): AdLoadResult<RewardedAd>`** on its companion (a sealed `Success`/`Failure`) alongside the callback form — so the `suspendCancellableCoroutine` bridge that `.planning/research/ARCHITECTURE.md` Pattern 2 sketches is only needed for the *show* half, not the load half. The ad unit ID moves into `AdRequest.Builder(adUnitId)` rather than a `load()` parameter, the AdMob app ID is supplied programmatically via `InitializationConfig.Builder(appId)` **and** still required as an `AndroidManifest.xml` meta-data tag because UMP reads it from there (the manifest currently has no such tag — that is a real gap this phase must close), and `MobileAds.initialize()` is **mandatory** in Next-Gen rather than optional, throwing `UninitializedPropertyAccessException` if skipped. `MobileAds.isInitialized` is a volatile boolean the gateway can read directly, which is the cleanest available representation of D-12's "knowably unservable".

Three integration problems are latent in the existing code and will not surface from a naive reading of the plan. **First**, `GameViewModel.resetToInitial()` calls `clearSnapshot()` — so carrying the allowance in the resume snapshot (D-04) means a restart wipes it, contradicting D-04's "retrying does NOT refill it". **Second**, `onHintRequested()` currently requests the rewarded grant *before* running the solver, which cannot satisfy ADS-06 ("a hint that reports no winning move is never charged") — the order must invert to search-then-charge. **Third**, `App.onCreate()` executes inside every Robolectric test in the suite (66 test classes call `RuntimeEnvironment.getApplication()` or drive Compose through it), so bootstrapping UMP there would drag the consent SDK into the JVM suite; `MainActivity.onCreate()` is the safe host and is touched by zero tests.

**Primary recommendation:** Build in ROADMAP's locked order — (1) `AssistAllowance` in `:domain` with JVM tests; (2) `GameViewModel` funnel + themed opt-in dialog + snackbar + clock freeze, all proven against a `NoOpAdGateway` whose `rewardedOfferable` now reports `true` so the dialog is actually exercised; (3) `ConsentGateway` + UMP + Settings privacy rows; (4) `AdMobAdGateway` using `AdRequest.Builder(AdIds.rewardedAdUnitId)` + the suspend `load()` + a single-slot cache + `show(activity) { earned = true }`, bound last in `PlatformModule`.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Free-allowance accounting (counts, spend, refill) | `:domain` (pure Kotlin) | — | Phase 6 D-04 mandates it; it is a pure policy function of (assist type, counters) with zero Android surface — the highest-value JVM-testable slice of the phase |
| Allowance persistence | Data layer (`ProgressRepository` + resume snapshot codec) | — | The codec is the established single store for per-game mid-flight state; `:domain` must not know about storage |
| Deciding *whether* to offer an ad | UI layer (`GameViewModel`) | `:domain` (allowance query), platform (`rewardedOfferable`) | The decision composes a domain fact (counters) with a platform fact (readiness); only the ViewModel sees both. It is not a gameplay rule and not an SDK concern |
| Rendering the opt-in dialog / snackbar / privacy rows | UI layer (stateless composables) | — | State hoisting mandate; `GameUiState.pendingAdOffer` mirrors the existing `confirmRestart` precedent exactly |
| Consent gathering + SDK initialization | Platform layer (`platform/ads/consent/`) | Host `Activity` (bootstrap trigger only) | UMP needs an `Activity`; `PlatformBoundaryRules` confines every `com.google.android.ump..` reference to `..platform..` |
| Rewarded load / show / reward callback | Platform layer (`platform/ads/`) | Host `Activity` via `ForegroundActivityProvider` | Same boundary rule; `RewardedAd.show()` takes an `Activity` which no ViewModel may hold |
| Elapsed-clock freeze during an ad | UI layer (`GameViewModel`) | — | The ticker and `elapsedBaseSeconds` already live there; the gateway must not reach into game state |
| Opening the privacy policy URL | UI layer (`SettingsScreen` via `LocalContext`) | — | `ACTION_VIEW` needs only a `Context`; routing it through platform would need an `Activity` for no benefit |
| Showing the UMP privacy options form | Platform layer | UI (trigger only) | Needs an `Activity`; `SettingsViewModel` calls a seam method with no Activity in the signature |

---

## Standard Stack

### Core

**No new artifacts.** Every dependency this phase needs is already on the graph from Phase 6 plan 06-09.

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk` | **1.4.0** (latest; published 2026-08-20) | Rewarded ads | Google's designated preferred Android ads SDK since 2026-07-06; the legacy `play-services-ads` line is in maintenance and sunsets 2027-06-30 [CITED: developers.google.com/admob/android/deprecation]. Already in `gradle/libs.versions.toml` as `adsMobileSdk = "1.4.0"` and on the `:app` graph [VERIFIED: repo inspection + dl.google.com maven-metadata] |
| `com.google.android.ump:user-messaging-platform` | **4.0.0** (latest; published 2025-10-30) | GDPR/consent gathering | Mandatory before serving ads in consent regions; declared explicitly in the catalog rather than inherited transitively [VERIFIED: dl.google.com/dl/android/maven2 metadata + repo inspection] |
| `androidx.compose.*` (BOM 2026.06.00) | via BOM | Themed dialog, snackbar host | Already present |
| `kotlinx-serialization-json` | 1.11.0 | Snapshot codec extension for allowance counters | Already present; `ResumeSnapshotDto` is `@Serializable` |
| `androidx.datastore:datastore-preferences` | 1.2.1 | *(only if the planner adopts the airtight allowance-persistence alternative — see Open Question 1)* | Already present |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `app.cash.turbine` | 1.2.1 | Assert `GameUiState`/`SettingsUiState` emissions around the dialog and privacy-row flows | Already a `testImplementation` |
| `kotlinx-coroutines-test` | 1.11.0 | `runTest` + virtual time for the ~5 s accept cap (D-10) | Already present; the `advanceTimeBy` path is how the cap is tested without a real 5 s wait |
| Robolectric (via Roborazzi/Compose test) | transitive | Drive the themed dialog and the Settings rows | Already the harness for `HintControlTest`, `UndoControlTest`, `NoWinBannerTest` |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| `RewardedAd.load()` (suspend) + a single-slot cache | `RewardedAdPreloader.start/pollAd/isAdAvailable` | The preloader gives managed retry and a maintained buffer, but it **keeps refilling its buffer for the rest of the session** after each `pollAd()` — which fights D-09's "no ad requests while allowance remains" and forces a `destroy(preloadId)` call after every refill to stay compliant with the decision. The manual single-slot form maps 1:1 onto D-09/D-10, is trivially fakeable behind the seam, and the retry cadence is Claude's discretion anyway. **Recommend manual; keep the preloader as fallback if load latency proves bad in device testing.** |
| Programmatic `InitializationConfig.Builder(AdIds.admobAppId)` | Manifest-only app ID (legacy style) | Next-Gen requires the programmatic form; the manifest tag is still needed *for UMP*. Both are required, not either/or. |
| Additive `rewardedOfferable: StateFlow<Boolean>` on `AdGateway` | A fourth `RewardOutcome` member | A fourth member breaks the sealed three-way contract every existing call site handles exhaustively (CONTEXT explicitly forbids this). A separate readiness signal is additive and lets the ViewModel decide *before* showing a dialog — which is what D-12 actually requires. |
| Themed `Dialog { Surface { … } }` with `clayShadow`/`claySquish` | M3 `AlertDialog` (as `RestartConfirmDialog` uses) | D-06 explicitly rules out the stock AlertDialog. `NoWinBanner` is the in-repo precedent for a hand-composed clay surface. |
| `Intent(ACTION_VIEW)` for the privacy policy | `androidx.browser` Custom Tabs | D-15 locked plain intent; Custom Tabs would add a dependency and (per PITFALLS Pitfall 12) `androidx.browser:1.8.0` carries a `minCompileSdk 34` constraint worth not inviting. |

**Installation:** none required — verify with:

```bash
./gradlew :app:dependencies --configuration releaseRuntimeClasspath | grep -E "ads-mobile-sdk|user-messaging-platform"
```

---

## Package Legitimacy Audit

> This phase installs **no new packages**. The two artifacts it begins *using* were added to the dependency graph in Phase 6 plan 06-09 and were verified there by direct AAR/POM inspection against `dl.google.com/dl/android/maven2`.

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.4.0` | Google Maven (`dl.google.com`) | GA line since 1.0.0, 2026-04-14; 1.4.0 published 2026-08-20 | n/a (Google Maven publishes no counts) | First-party Google; docs at developers.google.com/admob/android/next-gen | N/A — registry unreachable | **Approved** — already on graph |
| `com.google.android.ump:user-messaging-platform:4.0.0` | Google Maven (`dl.google.com`) | 4.0.0 published 2025-10-30; line since 1.0.0 | n/a | First-party Google; docs at developers.google.com/admob/android/privacy | N/A — registry unreachable | **Approved** — already on graph |

**Packages removed due to slopcheck `[SLOP]` verdict:** none
**Packages flagged as suspicious `[SUS]`:** none

**slopcheck disposition (honest reporting):** `slopcheck` installed successfully (`python3 -m slopcheck`, maven ecosystem supported) but returned `REGISTRY_ERROR` for both coordinates — it queries `search.maven.org`, and **neither artifact is published to Maven Central**; both live only on Google's Maven repository. slopcheck is therefore structurally inapplicable to this ecosystem, not merely unavailable. The substitute verification is strictly stronger and was performed in this session:

```
curl -s https://dl.google.com/dl/android/maven2/com/google/android/libraries/ads/mobile/sdk/ads-mobile-sdk/maven-metadata.xml
  → <latest>… <version>1.4.0</version>  <lastUpdated>20260820184538</lastUpdated>
curl -s https://dl.google.com/dl/android/maven2/com/google/android/ump/user-messaging-platform/maven-metadata.xml
  → <latest>4.0.0</latest> <release>4.0.0</release> <lastUpdated>20251030222332</lastUpdated>
```

Both coordinates are additionally spelled out verbatim on the official Google setup pages (see Sources), and Phase 6 already builds green with both resolved. `[VERIFIED: Google Maven metadata + official docs]`

**Typosquat note carried forward from `libs.versions.toml`:** `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk` is trivially confused with the legacy `com.google.android.gms:play-services-ads`. Any diff that reintroduces the `gms` coordinate is a regression, not a fix.

---

## Architecture Patterns

### System Architecture Diagram

```
 COLD START                                   PLAYER TAPS HINT / UNDO / BANNER-UNDO
     │                                                        │
     ▼                                                        ▼
 MainActivity.onCreate ──────────┐                    GameViewModel.onUndo()/onHintRequested()
     │  (NOT App.onCreate —      │                            │
     │   Robolectric runs that)  │                    ┌───────┴────────────────────────────┐
     ▼                           │                    │ HINT: run Solver FIRST (ADS-06)    │
 ConsentGateway.bootstrap()      │                    │   null → noWinningMove, CHARGE NONE │
     │ (platform/ads/consent/)   │                    │   move → stash, continue below      │
     ▼                           │                    │ UNDO: empty-history guard FIRST     │
 awaitActivity() ◄───────────────┼── ForegroundActivityProvider (PLAT-01, Phase 6)
     │                           │                    └───────┬────────────────────────────┘
     ▼                           │                            ▼
 UMP.requestConsentInfoUpdate ───┤                    AssistAllowance.remaining(reason)  ◄── :domain (pure)
     │  (EVERY launch, CONS-01)  │                            │
     ▼                           │              ┌─────────────┼──────────────────┐
 loadAndShowConsentFormIfRequired│           > 0│         == 0 │            == 0 │
     │  (main thread, Activity)  │              │              │ offerable=false │ offerable=true
     ▼                           │              ▼              ▼                 ▼
 canRequestAds()?  ── no ──► AdsReadiness   consume,     D-12: proceed      GameUiState.pendingAdOffer
     │ yes            .Unavailable  ▲       proceed       FREE, silent            │
     ▼                    (CONS-04) │                                              ▼
 MobileAds.initialize(ctx,          │                                   AdOfferDialog (themed, D-06)
   InitializationConfig(appId))     │                                     │              │
   @WorkerThread, AtomicBoolean ────┘                              "No thanks"      "Watch ad"
     │                                                                  │              │
     ▼                                                            Declined:      freezeTimer() ADSF-01
 AdsReadiness.Ready ──────────────────────────────────────┐       nothing            │
     │                                                    │       charged            ▼
     └──► AdGateway.rewardedOfferable : StateFlow<Boolean> ┘                 AdGateway.requestRewarded
                                                                                    │  (≤5 s cap, D-10)
 ALLOWANCE HITS 0 ──► AdGateway.preloadRewarded(reason) (D-09)                       ▼
     │                        │                                        ┌── platform/ads/ ──────────┐
     └────────────────────────┴───────────────────────────────────────►│ RewardedAdSlot (1 ad)     │
                                                                       │  suspend RewardedAd.load  │
 SETTINGS SCREEN                                                       │  → AdLoadResult           │
   ├─ "Privacy policy"  (always, REL-05) ─► Intent(ACTION_VIEW, url)   │ awaitActivity() (≤timeout)│
   └─ "Manage privacy choices" (CONS-03) ─► ConsentGateway             │ ad.adEventCallback = …    │
        visible iff privacyOptionsRequirementStatus == REQUIRED        │ ad.show(act){earned=true} │
                          └──► UMP.showPrivacyOptionsForm(activity)    └───────────┬───────────────┘
                                                                                   ▼
                                                        Granted / Unavailable / Declined
                                                                    │
                                    ┌───────────────────────────────┴───────────────┐
                              Granted│                     Unavailable│      Declined│
                                     ▼                                ▼              ▼
                        unfreezeTimer(); refill;         same + transient      unfreezeTimer();
                        consume; perform assist          snackbar (D-11)       nothing changes
```

### Recommended Project Structure

```
domain/src/main/kotlin/com/app/pegsolitaire/domain/assist/
└── AssistAllowance.kt                  # NEW — pure policy + AssistAllowanceConstants (3/5)

app/src/main/java/com/app/pegsolitaire/
├── MainActivity.kt                     # MODIFIED — kicks off ConsentGateway.bootstrap()
├── platform/ads/
│   ├── AdGateway.kt                    # MODIFIED — + rewardedOfferable, + preloadRewarded
│   ├── NoOpAdGateway.kt                # MODIFIED — rewardedOfferable = true (see Pitfall 6)
│   ├── AdConstants.kt                  # NEW — timeouts, retry cadence (NOT ad-unit IDs)
│   ├── AdMobAdGateway.kt               # NEW — real impl
│   ├── RewardedAdSlot.kt               # NEW — single-slot load/hold/consume cache
│   ├── consent/
│   │   ├── ConsentGateway.kt           # NEW — interface, SDK-free signatures
│   │   ├── UmpConsentGateway.kt        # NEW — UMP impl
│   │   └── AdsReadiness.kt             # NEW — Unknown / Ready / Unavailable
│   └── di/AdsModule.kt                 # NEW — SDK-typed @Provides live HERE, not PlatformModule
├── di/PlatformModule.kt                # MODIFIED — AdGateway binding swap (LAST step)
├── data/resume/ResumeSnapshotDto.kt    # MODIFIED — + nullable allowance counters
├── data/resume/SnapshotMapper.kt       # MODIFIED — encode/decode counters, fail-open
├── ui/game/
│   ├── GameUiState.kt                  # MODIFIED — pendingAdOffer, adMessage/adMessageTick
│   ├── GameViewModel.kt                # MODIFIED — funnel, search-first, clock freeze
│   ├── GameScreen.kt                   # MODIFIED — SnackbarHost + AdOfferDialog
│   └── AdOfferDialog.kt                # NEW — themed Candy Clay dialog
├── ui/settings/
│   ├── SettingsUiState.kt              # MODIFIED — + privacyOptionsRequired
│   ├── SettingsViewModel.kt            # MODIFIED — consent readiness collection
│   └── SettingsScreen.kt               # MODIFIED — Privacy section (D-14)
└── res/values/{strings,dimens}.xml     # MODIFIED — dialog/snackbar/row copy + dialog dimens
```

**Structure rationale:** `di/AdsModule.kt` must sit **inside** `..platform..` (`platform/ads/di/`), not in the app's `di/` package. `PlatformModule.kt`'s own KDoc states this explicitly: Hilt's generated `DaggerApp_HiltComponents_SingletonC` references every binding's types, so an SDK-typed `@Provides` in `com.app.pegsolitaire.di` trips `PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks` with a violation naming a generated class rather than the offending file.

---

### Pattern 1: Consent bootstrap lives in `MainActivity`, not `App`

**What:** `MainActivity.onCreate()` fires a single `consentGateway.bootstrap()` call. `App.onCreate()` is untouched.

**When to use:** Always, for this codebase specifically.

**Why:** `App` is the `@HiltAndroidApp` class named in the manifest, so Robolectric instantiates it and runs `onCreate()` for **every** Robolectric test — 66 test classes go through `RuntimeEnvironment.getApplication()` or a Compose rule that stands one up. Putting UMP there drags the consent SDK into the whole JVM suite and makes `./gradlew check --offline` (the Phase 6 D-18 invariant) fragile. **No test in the repo references `MainActivity`, and there is no `androidTest` source set at all** [VERIFIED: repo grep], so `MainActivity.onCreate` has exactly zero JVM-test exposure. D-16's "gather + show on the first Activity at cold start" points here anyway.

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var soundPlayer: SoundPlayer
    @Inject lateinit var consentGateway: ConsentGateway   // interface — no UMP types

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // CONS-01: refreshed on EVERY launch, not just first install.
        consentGateway.bootstrap()
        setContent { /* unchanged */ }
    }
}
```

**Boundary check:** `ConsentGateway` is an interface in `..platform..` whose signatures name no UMP type, so `MainActivity` (root package, outside `..platform..`) may depend on it. `PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks` is satisfied; the `ForegroundActivityProvider` rule is untouched because `MainActivity` never names it.

---

### Pattern 2: `ConsentGateway` — the UMP seam publishing `AdsReadiness`

**What:** A `@Singleton` in `platform/ads/consent/` that owns the entire UMP dance and exposes an SDK-free `StateFlow`.

```kotlin
sealed interface AdsReadiness {
    data object Unknown : AdsReadiness         // pre-bootstrap; the offer is suppressed
    data object Ready : AdsReadiness           // canRequestAds() && MobileAds.isInitialized
    data object Unavailable : AdsReadiness     // consent refused/failed, or init failed (CONS-04, D-12)
}

interface ConsentGateway {
    val readiness: StateFlow<AdsReadiness>
    /** True only while UMP reports PrivacyOptionsRequirementStatus.REQUIRED (CONS-03). */
    val privacyOptionsRequired: StateFlow<Boolean>
    /** Idempotent; called once per Activity creation. Runs the CONS-01 sequence. */
    fun bootstrap()
    /** CONS-03 — resolves the Activity internally; no Activity in the signature. */
    suspend fun showPrivacyOptions()
}
```

**The fixed sequence — every launch, no exceptions** [CITED: developers.google.com/admob/android/privacy]:

```kotlin
override fun bootstrap() {
    scope.launch {
        val activity = withTimeoutOrNull(AdConstants.ACTIVITY_WAIT_MS) {
            activityProvider.awaitActivity()
        } ?: return@launch                       // never resumed: leave readiness Unknown

        val params = ConsentRequestParameters.Builder()
            // Debug-only; see Testing Strategy. Never compiled into release.
            .apply { if (BuildConfig.DEBUG) setConsentDebugSettings(debugSettings(activity)) }
            .build()

        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {   // success — the form (if any) must be shown from the main thread
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) { _readiness.value = AdsReadiness.Unavailable }
                    publishPrivacyOptionsRequirement()
                    initializeAdsIfPermitted()
                }
            },
            { _ ->                                // CONS-04 — fall back, never block play
                publishPrivacyOptionsRequirement()
                initializeAdsIfPermitted()        // uses the PREVIOUS session's cached status
            },
        )
        // Parallel fast path: a returning consented user need not wait for the form call.
        initializeAdsIfPermitted()
    }
}

private val initialized = AtomicBoolean(false)   // Pitfall 5: exactly one initialize()

private fun initializeAdsIfPermitted() {
    if (!consentInformation.canRequestAds()) {
        if (_readiness.value == AdsReadiness.Unknown) _readiness.value = AdsReadiness.Unavailable
        return
    }
    if (!initialized.compareAndSet(false, true)) { _readiness.value = AdsReadiness.Ready; return }
    scope.launch(Dispatchers.IO) {               // @WorkerThread — main thread risks an ANR
        MobileAds.initialize(
            appContext,
            InitializationConfig.Builder(AdIds.admobAppId).build(),
        ) { /* adapter init complete */ }
        _readiness.value =
            if (MobileAds.isInitialized) AdsReadiness.Ready else AdsReadiness.Unavailable
    }
}
```

**Key facts driving this shape:**
- `canRequestAds()` **always returns `false` until `requestConsentInfoUpdate()` has been called** [CITED: developers.google.com/ad-manager/mobile-ads-sdk/android/next-gen/privacy]. Checking it first silently disables ads forever.
- `MobileAds.initialize` is annotated `@WorkerThread` and `@RequiresPermission(INTERNET)`; "This must be called on a background thread, failure to do so may cause an ANR" [CITED: developers.google.com/admob/android/next-gen/quick-start].
- `MobileAds.isInitialized` is a volatile boolean; "It is important to wait until this property is `true` prior to interacting with other methods on `MobileAds`" [CITED: Next-Gen `MobileAds` reference].
- Next-Gen initialization is **mandatory**, unlike legacy — skipping it throws `UninitializedPropertyAccessException` [CITED: developers.google.com/admob/android/next-gen/migration].

---

### Pattern 3: `AssistAllowance` — pure `:domain` policy (Phase 6 D-04, locked)

**What:** A value type plus a constants object, modeled on `ScoreCalculator`/`ScoreParams`.

```kotlin
package com.app.pegsolitaire.domain.assist

/** The free per-level assist allowance (ADS-02, D-01/D-02/D-03). Pure; no Android. */
data class AssistAllowance(
    val hintsRemaining: Int = AssistAllowanceLimits.FREE_HINTS,
    val undosRemaining: Int = AssistAllowanceLimits.FREE_UNDOS,
) {
    fun remaining(kind: AssistKind): Int = when (kind) {
        AssistKind.HINT -> hintsRemaining
        AssistKind.UNDO -> undosRemaining
    }

    fun hasFree(kind: AssistKind): Boolean = remaining(kind) > 0

    /** Spends one unit; a no-op at zero so callers cannot drive a counter negative. */
    fun spend(kind: AssistKind): AssistAllowance = when (kind) {
        AssistKind.HINT -> copy(hintsRemaining = (hintsRemaining - 1).coerceAtLeast(0))
        AssistKind.UNDO -> copy(undosRemaining = (undosRemaining - 1).coerceAtLeast(0))
    }

    /** D-03: a watched ad restores that counter to its full allowance. */
    fun refill(kind: AssistKind): AssistAllowance = when (kind) {
        AssistKind.HINT -> copy(hintsRemaining = AssistAllowanceLimits.FREE_HINTS)
        AssistKind.UNDO -> copy(undosRemaining = AssistAllowanceLimits.FREE_UNDOS)
    }

    companion object {
        /** Fail-open decode: an out-of-range stored value becomes a fresh full allowance. */
        fun fromStoredOrFull(hints: Int?, undos: Int?): AssistAllowance = AssistAllowance(
            hintsRemaining = hints?.takeIf { it in 0..AssistAllowanceLimits.FREE_HINTS }
                ?: AssistAllowanceLimits.FREE_HINTS,
            undosRemaining = undos?.takeIf { it in 0..AssistAllowanceLimits.FREE_UNDOS }
                ?: AssistAllowanceLimits.FREE_UNDOS,
        )
    }
}

/** Scoped constants (QUAL-03) — the 3/5 of D-02 named exactly once. */
object AssistAllowanceLimits {
    const val FREE_HINTS = 3
    const val FREE_UNDOS = 5
}

/** Which assist an allowance unit buys. Deliberately a `:domain` type, NOT `AdReason`. */
enum class AssistKind { HINT, UNDO }
```

**Why `AssistKind` and not `AdReason`:** `AdReason` lives in `platform/ads/`. `DomainPurityTest` forbids `:domain` depending on app code at all, so the domain policy needs its own enum; `GameViewModel` maps between them. Two two-member enums is cheap; a domain→platform dependency is a build failure.

**Refill-then-spend ordering (D-03 + D-07 copy):** on a `Granted` outcome, `refill(kind)` **then** `spend(kind)`. Under D-07's copy — *"watch a short ad for 3 more?"* — the player receives 3 further hint-uses total: this one plus two held. That is the reading that makes the copy literally true.

---

### Pattern 4: Two-step opt-in, following the existing `confirmRestart` precedent

**What:** The ViewModel raises a flag; the screen renders the dialog; the screen calls back. No new pattern is invented — `GameUiState.confirmRestart` + `RestartConfirmDialog` + `onConfirmRestart`/`onDismissRestart` is the exact shape already in the file.

```kotlin
// GameUiState additions
val pendingAdOffer: AdReason? = null,     // non-null ⇒ the themed dialog is on screen
val adOfferLoading: Boolean = false,      // D-10 in-dialog loading state on the button
val adMessage: Int? = null,               // @StringRes for the D-11 snackbar
val adMessageTick: Int = 0,               // follows the undoTick/selectionTick idiom
```

**Do NOT add a free-assists counter to `GameUiState`.** D-05 forbids surfacing counts in the UI, and Phase 7 set the precedent that a value the UI must not act on does not enter `GameUiState` (the `soundOn` gate stayed inside the player). The counters live in the ViewModel's private state, persisted through the codec.

**Why not `suspend` on a `CompletableDeferred` inside `requestRewarded`:** it reads nicely but couples the offer to a live coroutine, which does not survive process death and is awkward to assert with Turbine. The flag-and-callback form is testable with the existing Robolectric harness and matches the file's own idiom.

---

### Pattern 5: Hints search **first**, charge **second** (ADS-06)

**What:** Invert the current order in `onHintRequested()`.

**Why this is mandatory, not stylistic:** `onHintRequested()` today calls `adGateway.requestRewarded(AdReason.HINT)` before it ever runs the solver. Once an allowance and a real ad exist, that order means a player whose position has no winning move pays an allowance unit — or watches a 30-second video — to be told "no winning move". ADS-06 forbids exactly that. An allowance unit could be refunded; **an ad already watched cannot be**, so refund-on-null is not a viable alternative for the ad path, and using different orders for the two paths would be an invitation to drift.

```
onHintRequested()
  ├─ isLoading → return                          (existing WR-01 guard, unchanged)
  ├─ cancelHintSearch(); solverThinking = true    (existing CR-01 idiom, unchanged)
  ├─ capture boardForHint / solutionForHint / movesPlayedForHint on the main thread
  ├─ move = withContext(default) { getHintUseCase(...) }        (unchanged)
  ├─ board changed since capture → clear solverThinking only    (unchanged stale guard)
  ├─ move == null → noWinningMove = true; solverThinking = false; CHARGE NOTHING  ◄── ADS-06
  └─ move != null → pendingHint = move; solverThinking = false; enterAssistFunnel(HINT)
```

**Consequence for the accept path:** the found move must be **stashed** (`private var pendingHint: Move?`) so `onAcceptAdOffer()` can reveal it after the ad. Re-check `_uiState.value.board == boardForHint` before revealing — the dialog is modal so the board should not move, but the existing code already treats a stale result as a hard error condition and that discipline should not be relaxed.

**Consequence for solver-budget misses:** `getHintUseCase` also returns `null` when the budgeted solver misses its deadline (Phase 4 D-04). Under this ordering, a budget miss is likewise free. That is the correct and generous reading of ADS-06 — the player was told "no winning move" and must not be charged for it, regardless of why.

---

### Pattern 6: Clock freeze by rebasing (`ADSF-01`)

**What:** Cancel the ticker, fold the accumulated seconds into `elapsedBaseSeconds`, and take a **fresh** `TimeMark` on resume. This is the same mechanism `applyRestored()` already uses for a resumed snapshot (`elapsedBaseSeconds = restored.elapsedMs / 1000`), so it is prior art rather than a new concept.

```kotlin
private var frozenForAd = false

/** ADSF-01: stop charging wall-clock for the whole ad presentation. */
private fun freezeClockForAd() {
    if (startMark == null) return               // timer never started — nothing to freeze
    frozenForAd = true
    tickerJob?.cancel(); tickerJob = null
    elapsedBaseSeconds = _uiState.value.elapsedSeconds   // absorb time already earned
}

private fun unfreezeClockAfterAd() {
    if (!frozenForAd) return
    frozenForAd = false
    if (_uiState.value.status != GameStatus.Playing) return
    startMark = timeSource.markNow()            // FRESH mark — the stale one counted the ad
    tickerJob = launchTicker(startMark!!)
}
```

**Three hazards the planner must handle:**

1. **`resumeTickerIfNeeded()` must refuse while frozen.** It reuses the *old* `startMark` and only checks `tickerJob != null`. On the D-08 rescue-banner path, `onUndo()` calls `resumeTickerIfNeeded()` after the assist lands — if that runs while `frozenForAd` is still true, or before `unfreezeClockAfterAd()` re-marks, the ad's duration is silently re-added. Add `if (frozenForAd) return` as its first line.
2. **Freeze the `requestRewarded` call, not the dialog.** If the clock stopped while the offer dialog was open, a player could park the dialog indefinitely to stop the score decaying — a real exploit given the time-decay scoring. Freezing from *accept* to *dismiss* is bounded (≤5 s load cap + ad duration) and matches ADSF-01's "duration of any ad presentation" precisely.
3. **Whole-second truncation loses up to 1 s per freeze, in the player's favour.** `elapsedSeconds` is an `Int` of whole seconds, so the sub-second remainder at freeze time is discarded. Acceptable and worth one comment; if the planner wants exactness, carry the base in milliseconds.

**Testability:** `GameViewModel` already takes an injectable `timeSource` (`TimeSource`), and the existing tests drive it. The freeze is therefore fully assertable in a JVM test with a fake gateway that suspends for a known virtual duration — no device needed.

---

### Pattern 7: The real gateway — suspend load + callback-bridged show

**What:** `AdMobAdGateway` uses the coroutine-native load and only bridges the *show* half.

```kotlin
// Load — Next-Gen exposes a suspend companion function returning a sealed result.
// The ad unit ID belongs to the REQUEST in Next-Gen, not to load()'s parameters.
private suspend fun loadRewarded(): RewardedAd? =
    when (val result = RewardedAd.load(AdRequest.Builder(AdIds.rewardedAdUnitId).build())) {
        is AdLoadResult.Success -> result.ad
        is AdLoadResult.Failure -> null          // no fill / error → Unavailable (ADS-03)
    }

override suspend fun requestRewarded(reason: AdReason): RewardOutcome {
    if (consentGateway.readiness.value != AdsReadiness.Ready) return RewardOutcome.Unavailable
    // D-10: bounded wait — the preload may still be in flight when the player accepts.
    val ad = withTimeoutOrNull(AdConstants.ACCEPT_WAIT_MS) { slot.await(reason) }
        ?: return RewardOutcome.Unavailable
    val activity = withTimeoutOrNull(AdConstants.ACTIVITY_WAIT_MS) {
        activityProvider.awaitActivity()
    } ?: return RewardOutcome.Unavailable

    return suspendCancellableCoroutine { cont ->
        var earned = false                       // ADS-05: the ONLY place this becomes true
        ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdDismissedFullScreenContent() {
                slot.invalidate(); slot.preload(reason)      // single-show object
                if (cont.isActive) {
                    cont.resume(
                        if (earned) RewardOutcome.Granted else RewardOutcome.Declined,
                    )
                }
            }
            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                slot.invalidate(); slot.preload(reason)
                if (cont.isActive) cont.resume(RewardOutcome.Unavailable)  // ADS-03
            }
        }
        ad.show(activity) { _: RewardItem -> earned = true } // ADS-05
        cont.invokeOnCancellation { ad.adEventCallback = null }
    }
}
```

**Verified API facts** [CITED: Next-Gen Kotlin reference pages]:
- `RewardedAd` is an **interface** in `com.google.android.libraries.ads.mobile.sdk.rewarded`, with companion `suspend fun load(adRequest: AdRequest): AdLoadResult<RewardedAd>` and `fun load(adRequest, adLoadCallback: AdLoadCallback<RewardedAd>)`.
- `AdLoadResult` is a `sealed interface AdLoadResult<AdT : Ad>` in `…sdk.common` with `Success` and `Failure`.
- `fun show(activity: Activity, onUserEarnedRewardListener: OnUserEarnedRewardListener)`; the SAM is `fun onUserEarnedReward(reward: RewardItem)`.
- `var adEventCallback: RewardedAdEventCallback?`; `RewardedAdEventCallback` implements `AdEventCallback` + `OnAdMetadataChangedListener` and inherits `onAdShowedFullScreenContent()`, `onAdDismissedFullScreenContent()`, `onAdFailedToShowFullScreenContent(FullScreenContentError)`, `onAdImpression()`, `onAdClicked()`, `onAdPaid(AdValue)`.
- `AdRequest.Builder` takes the `adUnitId`; `AdRequest.adUnitId` is a required property.
- `Ad.destroy()` exists and should be called on a slot that is being discarded.

**Threading:** initialize on a background dispatcher; make **all** other GMA calls on the main thread. The gateway's `requestRewarded` is called from `viewModelScope` (main by default) — keep it there and do not add a `withContext(IO)`.

**`ForegroundActivityProvider` returns `null` while a fullscreen ad is showing** — its own KDoc says so, and that is deliberate. Capture the Activity **before** `show()`; never re-await it inside a callback.

---

### Pattern 8: Settings Privacy section (D-14, CONS-03, REL-05)

```kotlin
// SettingsUiState — one new field; the policy row is unconditional so needs no flag.
val privacyOptionsRequired: Boolean = false,

// SettingsViewModel
init {
    viewModelScope.launch {
        consentGateway.privacyOptionsRequired.collect { required ->
            _uiState.update { it.copy(privacyOptionsRequired = required) }
        }
    }
}
fun onManagePrivacyChoices() { viewModelScope.launch { consentGateway.showPrivacyOptions() } }
```

```kotlin
// SettingsScreen — stateless; the policy row uses Context, never Activity.
val context = LocalContext.current
PrivacyRow(
    label = stringResource(R.string.settings_privacy_policy),
    onClick = {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, PrivacyLinks.POLICY_URL.toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    },
)
if (state.privacyOptionsRequired) {
    PrivacyRow(
        label = stringResource(R.string.settings_manage_privacy_choices),
        onClick = onManagePrivacyChoices,
    )
}
```

**Two boundary constraints:**
- `PlatformBoundaryRules.uiAndDomainNeverTouchAnActivity` forbids any `..ui..` class depending on `android.app.Activity`. `LocalContext.current` is a `Context` — fine. Never write `LocalActivity.current` or cast the context. `FLAG_ACTIVITY_NEW_TASK` is needed precisely because the receiver is a `Context`, not an `Activity`.
- Wrap `startActivity` in a `runCatching` — a device with no browser throws `ActivityNotFoundException`, and REL-05's row must not crash Settings.

**Privacy-policy URL placement (D-13):** the existing `AdIdResolution` machinery in `build-logic/` already resolves env-var → `local.properties` → debug default for four keys and fails a release build on a missing/placeholder value. The URL is *not* a secret (it ships in the APK and is public), so a plain scoped constant object (`PrivacyLinks.POLICY_URL`) is the simpler, `adIdLiteralCheck`-safe choice; a `buildConfigField` is only warranted if the URL should differ per build type, which nothing suggests. **Get the exact URL from the user before planning this task** — it is not recorded anywhere in `.planning/`.

---

### Pattern 9: The D-11 snackbar via the established tick idiom

`GameScreen` uses a `Scaffold` but has **no `SnackbarHost` anywhere in the app** [VERIFIED: repo grep]. Add one, and drive it with the same `Int`-tick idiom the file already uses for `undoTick`/`selectionTick`/`rejectedTapTick` — a mechanism this codebase has four working instances of:

```kotlin
val snackbarHostState = remember { SnackbarHostState() }
LaunchedEffect(state.adMessageTick) {
    val res = state.adMessage ?: return@LaunchedEffect
    if (state.adMessageTick == 0) return@LaunchedEffect
    snackbarHostState.showSnackbar(context.getString(res))
}
Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }, /* … */)
```

Why a tick rather than a one-shot `SharedFlow`: the tick survives configuration change without replay bugs, is trivially assertable in a Robolectric test, and does not introduce a second eventing mechanism into a ViewModel that has none.

---

### Anti-Patterns to Avoid

- **Charging the allowance before the solver runs.** Directly violates ADS-06 (Pattern 5).
- **Requesting the grant before the empty-history guard in `onUndo()`.** The current code does `requestRewarded(...)` and *then* `if (moveHistory.isEmpty()) return@launch` — with an allowance, a no-op undo on a fresh board would burn a unit or an ad. Move the emptiness guard to the top.
- **A fourth `RewardOutcome` member for "unservable".** CONTEXT explicitly forbids breaking the sealed three-way contract; use an additive readiness signal.
- **Ad-unit IDs in a constants object.** `adIdLiteralCheck` fails the build; `AdIds` + `BuildConfig` is the only sanctioned route (Phase 6 D-05/D-08).
- **`MobileAds.initialize()` on the main thread, or from `App.onCreate`.** ANR risk plus Robolectric contamination.
- **Granting the reward in `onAdDismissedFullScreenContent`.** ADS-05 and AdMob policy both require the reward come only from the `OnUserEarnedRewardListener`.
- **Reusing a `RewardedAd` instance for a second show.** One show per object; null the slot in the dismiss callback.
- **Adding a `RETRY` member to `AdReason`.** Phase 6 D-03 locked retry as permanently free.
- **`@Suppress` or a detekt baseline to get past `MagicNumber` on the 3/5/5000 constants.** Name them (PITFALLS Pitfall 14).

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Determining whether a consent form is needed and rendering it | A geo-IP check + a custom consent dialog | `UserMessagingPlatform.loadAndShowConsentFormIfRequired` | The form's content, the TCF string encoding, and the regional applicability rules are all server-configured in the AdMob UI; a hand-rolled dialog produces no valid TCF signal and is a policy violation |
| Persisting consent state | Reading TCF strings out of DataStore to infer consent | `requestConsentInfoUpdate()` every launch | Google explicitly warns this "can lead to a TCF 3.3 error if consent is expired"; UMP persists its own state |
| Knowing whether the privacy-options entry point is required | Region detection | `consentInformation.privacyOptionsRequirementStatus == REQUIRED` | Three-state enum (`REQUIRED`/`NOT_REQUIRED`/`UNKNOWN`) whose value depends on server-side message configuration you cannot see from the client |
| Ad retry/backoff after a load failure | A hand-written exponential backoff loop | A bounded, explicit single retry, or `RewardedAdPreloader` | An unbounded retry loop against a no-fill condition burns battery and can look like abusive traffic; if you want managed retry, that is precisely what the SDK preloader provides |
| Reward verification | Client-side trust heuristics | The `OnUserEarnedRewardListener` callback | Server-side verification is explicitly Out of Scope in ROADMAP (no backend); the reward is a hint in a single-player puzzle |
| Test-device / EEA simulation | Shipping a build to an EEA VPN | `ConsentDebugSettings` + `DEBUG_GEOGRAPHY_EEA` + `consentInformation.reset()` | The supported mechanism; emulators are test devices by default |
| Serializing the allowance counters | A bespoke string encoding in the blob | Two nullable `Int` fields on `ResumeSnapshotDto` | `Json { ignoreUnknownKeys = true }` + nullable-with-default is the codec's established backward/forward-compat contract (`minimumPegs: Int? = null` is the working precedent) |

**Key insight:** every consent-related fact in this phase is *server-configured* (the AdMob Privacy & messaging UI) and *regionally variable*. Any client-side inference is both wrong and a compliance exposure. The SDK is the only source of truth; the app's job is sequencing, not judgement.

---

## Common Pitfalls

### Pitfall 1: D-04's "retry does not refill" is contradicted by the code that clears the snapshot

**What goes wrong:** D-04 locks two things — counters live in the resume snapshot, and retrying must not refill them. But `GameViewModel.resetToInitial()` (the restart path) calls `clearSnapshot()`, which deletes the stored row so that "Continue" cannot resurrect an abandoned game. If the counters live only in that row, **restart deletes the allowance**, and the very loophole D-04 exists to close reopens the moment the process is killed after a restart.

**Why it happens:** the two decisions were made against different mental models of where the snapshot lives. Nothing in CONTEXT.md notices that restart is a snapshot-clearing event.

**How to avoid — three options, in order of preference:**

- **Option A (recommended — literal D-04, one documented hole).** Hold the counters in ViewModel memory across `resetToInitial()` (the ViewModel instance survives a restart), and carry them in the snapshot for process death. Reset only on level win / level change / refill. The residual hole is "restart, then force-kill the app before leaving the screen" — high effort, tiny reward, and the snapshot is only written on `ON_STOP` anyway, so an ordinary background-and-return preserves the counters correctly. Cost: ~0 extra code beyond the codec fields.
- **Option B (airtight).** Persist `(mode → hints, undos)` in DataStore Preferences alongside `ChallengeProgress`, written on every spend and cleared on level completion. Fully closes the loophole and never interacts with snapshot clearing. Cost: a new preferences key set + repository method; **deviates from D-04's stated mechanism and should be confirmed with the user.**
- **Option C (rejected).** Make `clearSnapshot()` preserve a counters-only row. This changes what "a stored snapshot exists" means for the Home screen's Continue tile — a real regression risk in a surface that is already replay-validated and security-hardened.

**Warning signs:** a plan task that adds counters to `ResumeSnapshotDto` without a matching task that decides what `resetToInitial()` does with them.

---

### Pitfall 2: `onHintRequested` charges before it searches (ADS-06)

Covered fully in Pattern 5. **Warning sign:** any plan task that says "gate `requestRewarded` behind the allowance" without also saying "and move it after the solver call".

---

### Pitfall 3: `App.onCreate()` runs in every Robolectric test

**What goes wrong:** placing the UMP bootstrap in `App.onCreate` puts the consent SDK on the execution path of the entire 66-class JVM suite. Symptoms are non-obvious: intermittent failures, network attempts under `--offline` (violating Phase 6 D-18), and slow tests.

**Why it happens:** `App` is the natural-looking "app start" hook, and it is where `ForegroundActivityProvider.attachTo` and the sound preload already live — so it reads as the established pattern.

**How to avoid:** bootstrap from `MainActivity.onCreate` (Pattern 1). Zero tests touch `MainActivity`, and there is no `androidTest` source set.

**Warning signs:** `@Inject lateinit var consentGateway` appearing in `App.kt`; a sudden jump in `./gradlew test` wall time.

---

### Pitfall 4: `canRequestAds()` checked before `requestConsentInfoUpdate()`

It returns `false` until `requestConsentInfoUpdate()` has run in the current session [CITED: developers.google.com/ad-manager/mobile-ads-sdk/android/next-gen/privacy]. A check placed at the top of the bootstrap silently disables ads on every device, everywhere — and the usual "fix" is to delete the check, which is a GDPR breach. **Warning sign:** `canRequestAds()` as the first statement in any function.

---

### Pitfall 5: `MobileAds.initialize()` called twice

Both the success callback of `loadAndShowConsentFormIfRequired` and the parallel fast path can reach `initializeAdsIfPermitted()`. Without an `AtomicBoolean` guard the SDK initializes twice and issues duplicate requests. Google's own sample uses exactly this guard. **Warning sign:** two call sites, no `compareAndSet`.

---

### Pitfall 6: The `NoOpAdGateway` stub hides the dialog it is supposed to prove

**What goes wrong:** ROADMAP requires the opt-in dialog to be built and tested *against the stub*. But if the new `rewardedOfferable` signal defaults to `false` on `NoOpAdGateway` (the naïve reading of "no ads available"), D-12 kicks in and the dialog **never renders** — so the stub-first phase proves nothing, and the policy-violating gap survives to the SDK swap.

**How to avoid:** `NoOpAdGateway.rewardedOfferable = MutableStateFlow(true)` and `requestRewarded` keeps returning `Granted`. The stub then models "ads are offerable and instantly succeed": the dialog shows, accept → `Granted`, decline → the ViewModel's own `Declined` path. A separate `FakeAdGateway(offerable = false)` in tests covers D-12, and `FakeAdGateway(outcome = Unavailable)` covers ADS-03/D-11 — the test fakes for all three already exist in `GameViewModelTest.kt`.

**Note for the planner:** this is a deliberate, in-scope change to Phase 6 D-02's "the grant is deliberately not configurable". D-02 scoped that constraint to Phase 6 ("a phase whose contract is *nothing a player can see changes*"). Phase 9 is where it changes; say so in the KDoc.

---

### Pitfall 7: The manifest has no AdMob `APPLICATION_ID` meta-data

**What goes wrong:** `app/src/main/AndroidManifest.xml` declares `INTERNET`, removes `PlayGamesInitProvider`, and nothing else. There is **no `com.google.android.gms.ads.APPLICATION_ID` meta-data tag** [VERIFIED: repo inspection]. The Next-Gen setup page states: *"Google User Messaging Platform (UMP) SDK requires the app ID in your app's `AndroidManifest.xml` file"* [CITED: developers.google.com/admob/android/next-gen/quick-start] — even though the ads SDK itself now takes the ID programmatically via `InitializationConfig`.

**How to avoid:** add the tag referencing the **existing** generated string resource, which Phase 6 already emits per build type via `resValue("string", "admob_app_id", …)`:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="@string/admob_app_id" />
```

Using `@string/admob_app_id` rather than a literal is also what keeps `adIdLiteralCheck` (which scans all of `src/main`) green.

**Warning signs:** a "missing application ID" crash on first UMP call; a plan that adds `InitializationConfig` but never touches the manifest.

---

### Pitfall 8: `ManifestSurfaceTest` asserts permissions by **set equality**

Any manifest or dependency change that alters the merged permission set fails the build with a diff. The current expected set is `INTERNET`, `ACCESS_NETWORK_STATE`, `READ_BASIC_PHONE_STATE`, `com.google.android.gms.permission.AD_ID`, `WAKE_LOCK`, `FOREGROUND_SERVICE`, `${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. Adding the meta-data tag should not change it (the SDKs are already on the graph), **but verify** — and if it does change, the test's own KDoc is explicit that adding an entry is a decision requiring a Play Console Data-safety follow-up, not a chore.

---

### Pitfall 9: Freezing the clock while the dialog is open creates a scoring exploit

Covered in Pattern 6, hazard 2. Freeze from *accept*, not from *offer*. **Warning sign:** `freezeClockForAd()` called where `pendingAdOffer` is set rather than in `onAcceptAdOffer()`.

---

### Pitfall 10: `resumeTickerIfNeeded()` silently re-adds the ad's duration

Covered in Pattern 6, hazard 1. The D-08 rescue-banner path is the one that hits it, because `onUndo()` ends with `resumeTickerIfNeeded()` and the banner's Undo action is the ad-offer trigger. **Warning sign:** no `frozenForAd` guard at the top of `resumeTickerIfNeeded()`.

---

### Pitfall 11: Rewarded ads are single-show and expire

A `RewardedAd` instance can be shown exactly once; null the slot in `onAdDismissedFullScreenContent` and reload. Legacy documentation states rewarded ads expire roughly **one hour** after load — a long peg-solitaire session will hold a stale ad, whose `show()` then fails. Track the load timestamp and treat an over-age slot as empty. `[ASSUMED]` for Next-Gen specifically: the ~1h figure is documented for the legacy SDK and was not restated on any Next-Gen page read in this session. Design defensively — `onAdFailedToShowFullScreenContent` → `Unavailable` covers it regardless.

---

### Pitfall 12: `RewardedAdPreloader` keeps requesting ads after you stop needing them

If the planner chooses the preloader over the manual slot: `pollAd()` "returns the next available ad and loads another ad in the background", and `start()` keeps the buffer full "throughout the session". After a rewarded refill the player has a full allowance again and D-09 says no requests should be in flight — so `destroy(preloadId)` must be called after each successful show, and `start()` again at the next exhaustion. Forgetting this makes the app request ads continuously for the rest of the session, contradicting D-09.

---

### Pitfall 13: `ForegroundActivityProvider.current` is `null` while the ad is on screen

By design — the provider clears on `onActivityPaused`, and a fullscreen ad pauses the host. Capture the `Activity` **before** `show()` and never re-await inside a callback. Documented in the provider's own KDoc; the failure mode is a hang, not a crash.

---

### Pitfall 14: `PlatformBoundaryTest` catches SDK types through Hilt codegen

The rule is bytecode-based, so an SDK-typed `@Provides` in `com.app.pegsolitaire.di` surfaces as a violation naming `DaggerApp_HiltComponents_SingletonC`. Put every ads/UMP-typed provider in `platform/ads/di/`.

---

### Pitfall 15: `GameScreen` has no `SnackbarHost`

D-11 needs one and none exists anywhere in the app. This is a small task in its own right, not a free rider on the dialog work.

---

### Pitfall 16: `AdsReadiness.Unknown` at the moment of the first offer

D-16 says every later surface may assume consent is resolved — but a fast player on a slow network could exhaust an allowance before `bootstrap()` completes. `Unknown` must be treated as **not offerable** (fall through to D-12's free grant), never as "offerable, let's find out". Showing a dialog and then discovering the SDK is not initialized produces exactly the "watch an ad? → instant free grant" absurdity D-12 rules out.

---

## Code Examples

### The single assist funnel in `GameViewModel`

```kotlin
/** Returns true when the assist may proceed. Never called for a null hint (ADS-06). */
private fun enterAssistFunnel(kind: AssistKind, reason: AdReason): Boolean {
    if (allowance.hasFree(kind)) {                    // free path — no ad, no dialog (ADS-02)
        allowance = allowance.spend(kind)
        if (!allowance.hasFree(kind)) adGateway.preloadRewarded(reason)   // D-09
        return true
    }
    if (adGateway.rewardedOfferable.value.not()) return true              // D-12 — silently free
    _uiState.update { it.copy(pendingAdOffer = reason) }                  // ADS-01 — the dialog
    return false                                                          // resolved by callbacks
}

fun onDismissAdOffer() {                                                  // "No thanks"
    pendingHint = null
    _uiState.update { it.copy(pendingAdOffer = null, adOfferLoading = false) }
}

fun onAcceptAdOffer() {
    val reason = _uiState.value.pendingAdOffer ?: return
    val kind = reason.toAssistKind()
    viewModelScope.launch {
        _uiState.update { it.copy(adOfferLoading = true) }                // D-10
        freezeClockForAd()                                                // ADSF-01
        val outcome = adGateway.requestRewarded(reason)
        unfreezeClockAfterAd()
        _uiState.update { it.copy(pendingAdOffer = null, adOfferLoading = false) }
        when (outcome) {
            RewardOutcome.Granted -> { allowance = allowance.refill(kind).spend(kind) }
            RewardOutcome.Unavailable -> {                                // ADS-03
                allowance = allowance.refill(kind).spend(kind)
                emitAdMessage(R.string.ads_unavailable_free_grant)        // D-11 snackbar
            }
            RewardOutcome.Declined -> { pendingHint = null; return@launch }
        }
        when (kind) {
            AssistKind.HINT -> revealStashedHint()
            AssistKind.UNDO -> performUndo()
        }
    }
}
```

### Debug-only consent testing setup

```kotlin
// BuildConfig.DEBUG-guarded. Emulators are test devices by default; a physical device's
// hashed ID is printed by the SDK to logcat on first run.
private fun debugSettings(activity: Activity) = ConsentDebugSettings.Builder(activity)
    .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
    .addTestDeviceHashedId(BuildConfig.UMP_TEST_DEVICE_HASHED_ID)   // optional, may be blank
    .build()

// Replay a first-install consent flow between runs:
//   consentInformation.reset()
```

### Snapshot codec extension (fail-open, matching the `minimumPegs` precedent)

```kotlin
@Serializable
data class ResumeSnapshotDto(
    // … existing fields unchanged …
    val minimumPegs: Int? = null,
    /**
     * Free hints remaining in this level (ADS-02/D-04).
     * `null` means "written before the allowance existed" — decoded as a FULL allowance,
     * the fail-open default, consistent with every other absent field in this blob.
     */
    val freeHintsRemaining: Int? = null,
    /** Free undos remaining in this level (ADS-02/D-04). Same `null` semantics. */
    val freeUndosRemaining: Int? = null,
)
```

Decode through `AssistAllowance.fromStoredOrFull(...)`, which also rejects out-of-range forged values (a stored `999` must not become 999 free hints — the WARN-02 bounding discipline the codec already applies to coordinates).

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `com.google.android.gms:play-services-ads` | `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk` | Next-Gen designated preferred 2026-07-06; legacy v23.x deprecated 2026-02-17, sunsets 2027-06-30 | Package root changes to `com.google.android.libraries.ads.mobile.sdk`; **almost every rewarded-ads tutorial online covers the legacy API** and will not compile |
| `MobileAds.initialize(context)` optional | `MobileAds.initialize(context, InitializationConfig)` **mandatory** | Next-Gen 1.0.0 | Skipping it throws `UninitializedPropertyAccessException` on the first ad call |
| AdMob app ID from manifest meta-data only | App ID passed programmatically via `InitializationConfig.Builder(appId)` **and** still required in the manifest for UMP | Next-Gen 1.0.0 | Both are needed; the manifest tag is a UMP requirement, not a GMA one |
| `RewardedAd.load(context, adUnitId, adRequest, callback)` | `RewardedAd.load(AdRequest.Builder(adUnitId).build())` — with a **suspend** overload returning `AdLoadResult` | Next-Gen 1.0.0 | The ad unit ID moves into the request; a coroutine-native load removes the need for a `suspendCancellableCoroutine` on the load half |
| `FullScreenContentCallback` abstract class + `AdError` | `RewardedAdEventCallback` interface + `FullScreenContentError` | Next-Gen 1.0.0 | Interface, so only the callbacks you need are overridden; assigned to `ad.adEventCallback` rather than passed to a setter |
| `MobileAdsInitProvider` auto-init | **No ContentProvider at all** in Next-Gen | Next-Gen 1.0.0 | Phase 6 D-17 already recorded this from the AAR manifest; only `PlayGamesInitProvider` is removed |
| `AdPreloader.numAdsAvailable()` | `AdPreloader.getNumAdsAvailable()` | Next-Gen 1.0.0 (breaking) | Only relevant if the preloader is adopted |
| `setTagForChildDirectedTreatment` / `setTagForUnderAgeOfConsent` | `RequestConfiguration.setAgeRestrictedTreatment()` | Next-Gen 1.1.0 | Relevant only if the app ever targets a mixed/child audience — currently out of scope |

**Corrections to prior milestone research** (both files should be treated as superseded on these points):

- `.planning/research/STACK.md` says *"Next-Gen's ads API is callback-based, not suspend-based"*. **Incorrect** — `RewardedAd.load` has a `suspend` overload returning `sealed AdLoadResult` [CITED: Next-Gen `RewardedAd` Kotlin reference].
- `.planning/research/ARCHITECTURE.md` Pattern 2 models `requestRewarded` as returning `Boolean` and recommends widening be avoided. **Superseded** — Phase 6 D-01 already shipped the sealed `RewardOutcome`, and the existing `GameViewModel` branches are correct as written.
- `.planning/research/ARCHITECTURE.md` proposes adding `RETRY` to `AdReason`. **Superseded** by Phase 6 D-03 — retry is permanently free.
- `.planning/research/PITFALLS.md` Pitfall 15 warns about `MobileAdsInitProvider`. **Does not exist** in Next-Gen (Phase 6 D-17, AAR-verified).

**Deprecated/outdated:**
- `PreloadConfiguration` in the **legacy** `com.google.android.gms.ads.preload` package is marked deprecated in favour of format-specific preloaders. The Next-Gen `…sdk.rewarded` preloader is the current one; do not follow legacy preload documentation.

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Rewarded ads expire ~1 hour after load in Next-Gen (documented for legacy; not restated on any Next-Gen page read) | Pitfall 11 | A stale slot fails at `show()`; already mapped to `Unavailable`, so the player still gets the assist — low risk, but the "reload on expiry" task may be unnecessary or mis-tuned |
| A2 | `PreloadConfiguration(adRequest)` is the correct Next-Gen constructor form (the docs snippet reads this way; the reference page for the Next-Gen `PreloadConfiguration` was not located) | Alternatives / Pitfall 12 | Only matters if the planner picks the preloader over the manual slot; a compile error at worst |
| A3 | `AdLoadResult.Success` exposes the ad as `.ad` and `Failure` exposes an error property (the reference page confirms the sealed shape but does not print the property names) | Pattern 7 | Compile-time discoverable in 30 seconds; zero design risk |
| A4 | `loadAndShowConsentFormIfRequired` must be called from the main thread (it presents a dialog; the docs do not state this explicitly) | Pattern 2 | Calling it off-main would throw or misbehave; the recommended code already runs it from the UMP success callback, which the SDK invokes on the main thread |
| A5 | The AdMob rewarded ad unit and the UMP consent message are already configured in the AdMob UI (ROADMAP lists both as External Prerequisites started on Phase 6 day 1; no completion record exists in `.planning/`) | Environment Availability | **UMP renders nothing without a configured consent message** — the whole CONS-01 flow silently no-ops and appears to work in a non-EEA test. Must be confirmed before the phase's device-verification task |
| A6 | The privacy policy URL from D-13 is a single static HTTPS URL with no per-build variation | Pattern 8 | If it differs per build type, the constant must become a `buildConfigField` — a small refactor |
| A7 | Adding the `APPLICATION_ID` meta-data tag does not change the merged permission set | Pitfall 8 | `ManifestSurfaceTest` fails with a clear diff; a five-minute fix but it will look alarming |
| A8 | Truncating sub-second remainder on each clock freeze (in the player's favour) is acceptable | Pattern 6 | If a reviewer objects, carry the base in milliseconds — a contained change |

---

## Open Questions

1. **Where do the allowance counters actually live?**
   - *What we know:* D-04 says "carried in the resume snapshot" and "retrying does NOT refill". `resetToInitial()` calls `clearSnapshot()`.
   - *What's unclear:* whether the user considers the "restart then kill the process" hole acceptable.
   - *Recommendation:* Option A (in-memory across restart + snapshot for process death) — the literal D-04 mechanism with the hole documented in a KDoc. Escalate to the user only if the planner wants Option B (DataStore), which deviates from D-04's stated mechanism.

2. **What is the exact privacy-policy URL (D-13)?**
   - *What we know:* it is live on the developer website; nothing in `.planning/` records it.
   - *What's unclear:* the URL itself.
   - *Recommendation:* a `checkpoint:human` task early in the phase; the Settings row cannot be built or tested without it.

3. **Are the AdMob rewarded ad unit and the UMP consent message configured?** (A5)
   - *Recommendation:* a `checkpoint:human-verify` task before any device verification. Rewarded testing works with Google's test unit regardless, but **CONS-01 cannot be verified at all** without a configured consent message.

4. **Preloader vs. manual single-slot.**
   - *What we know:* both work; the preloader gives managed retry but keeps requesting for the session.
   - *Recommendation:* manual single slot (maps to D-09 exactly, trivially fakeable). Revisit only if device testing shows unacceptable load latency at the moment of accept.

5. **Does the rescue banner need a distinct offer copy?**
   - *What we know:* D-08 routes the banner's Undo through the same dialog. D-07 wants the copy to carry the explanation.
   - *What's unclear:* whether "You've used your 5 free undos for this level — watch a short ad for 5 more?" reads correctly when the player arrived from a *stuck* board rather than a normal undo tap.
   - *Recommendation:* one copy string per `AdReason`, not per entry point. Keep it simple; the reward is identical.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| `ads-mobile-sdk` on the dependency graph | All ADS-* | ✓ | 1.4.0 | — |
| `user-messaging-platform` on the graph | All CONS-* | ✓ | 4.0.0 | — |
| Per-build-type ad-unit IDs (`AdIds` / `BuildConfig`) | ADS-01..06 | ✓ | Phase 6 | — |
| `INTERNET` permission | All ads/consent | ✓ | Phase 6 | — |
| `ForegroundActivityProvider` | UMP + `show()` | ✓ | Phase 6 | — |
| `com.google.android.gms.ads.APPLICATION_ID` manifest meta-data | UMP | ✗ | — | **None — must be added this phase** (Pitfall 7) |
| `SnackbarHost` in `GameScreen` | D-11 / ADS-03 | ✗ | — | **None — must be added this phase** |
| AdMob rewarded ad unit (real, release) | Release build config gate | Unverified | — | Debug builds already resolve Google's test unit; release configuration fails fast (Phase 6 D-09) |
| UMP consent message configured in AdMob UI | CONS-01 device verification | Unverified | — | **None** — UMP renders nothing without it |
| Privacy policy URL | REL-05 | Unverified (D-13 says live) | — | None — blocks the Settings row |
| Physical Android device / emulator for on-device verification | CONS-01/03, ADS-01/04/05, ADSF-01 | Assumed available (Phase 8 ran an on-device audit) | — | — |
| Java/Gradle toolchain, offline `check` | All | ✓ | JDK 17, Gradle 9.x, AGP 9.1.0 | — |

**Missing dependencies with no fallback:**
- The manifest AdMob `APPLICATION_ID` meta-data tag.
- A `SnackbarHost` in `GameScreen`.
- A configured UMP consent message (external, human task).
- The privacy policy URL (external, human task).

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit4 4.13.2 + Robolectric (Compose UI Test via BOM) + Truth + Turbine 1.2.1 + kotlinx-coroutines-test 1.11.0 |
| Config file | `app/build.gradle.kts`, `domain/build.gradle.kts`; gates wired in `build-logic/src/main/kotlin/standards.gradle.kts` |
| Quick run command | `./gradlew :domain:test --offline` (pure JVM, seconds) |
| Full suite command | `./gradlew check --offline` (detekt + ktlint + dpSpLiteralCheck + adIdLiteralCheck + audioFocusLiteralCheck + all tests) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| ADS-02 | Allowance spends, refills, clamps at 0, rejects forged stored values | unit (pure JVM) | `./gradlew :domain:test --tests '*AssistAllowanceTest*'` | ❌ Wave 0 |
| ADS-02 | Free hints/undos are granted with no dialog while the allowance holds | unit (Robolectric VM) | `./gradlew :app:testDebugUnitTest --tests '*GameViewModelAllowanceTest*'` | ❌ Wave 0 |
| ADS-01 | At 0, `pendingAdOffer` is raised and no ad is requested until accept | unit (VM) | same class | ❌ Wave 0 |
| ADS-01 | Decline leaves board, history, hint state and allowance untouched | unit (VM) | same class | ❌ Wave 0 |
| ADS-01 | The themed dialog renders the reward text and both actions | Compose (Robolectric) | `./gradlew :app:testDebugUnitTest --tests '*AdOfferDialogTest*'` | ❌ Wave 0 |
| ADS-03 | `Unavailable` grants the assist **and** emits the snackbar message | unit (VM) | `*GameViewModelAllowanceTest*` (fake gateway `outcome = Unavailable`) | ❌ Wave 0 — fake exists in `GameViewModelTest.kt` |
| ADS-04 | Banner Undo at 0 opens the same dialog; at >0 undoes instantly | Compose + VM | `./gradlew :app:testDebugUnitTest --tests '*NoWinBannerTest*' --tests '*GameViewModelAllowanceTest*'` | ⚠️ `NoWinBannerTest.kt` exists; extend |
| ADS-05 | `Declined` (dismiss-unearned) does not refill or spend | unit (VM) | `*GameViewModelAllowanceTest*` | ❌ Wave 0 |
| ADS-06 | A `null` hint charges neither allowance nor ad | unit (VM) | `*GameViewModelAllowanceTest*` — assert allowance unchanged and gateway never called | ❌ Wave 0 |
| ADSF-01 | `elapsedSeconds` does not advance across a gateway call that suspends for N virtual seconds | unit (VM, virtual time) | `./gradlew :app:testDebugUnitTest --tests '*GameViewModelAdClockTest*'` | ❌ Wave 0 |
| ADSF-01 | Rescue-banner undo after an ad does not re-add the ad's duration | unit (VM) | same class | ❌ Wave 0 |
| D-04 | Allowance round-trips through encode/decode; missing fields decode as full; out-of-range rejected | unit (JVM) | `./gradlew :app:testDebugUnitTest --tests '*SnapshotMapper*'` | ⚠️ mapper tests exist; extend |
| D-04 | Restart does not refill (per the chosen Option A/B) | unit (VM) | `*GameViewModelAllowanceTest*` | ❌ Wave 0 |
| CONS-03 | The "Manage privacy choices" row appears only when required | Compose + VM | `./gradlew :app:testDebugUnitTest --tests '*SettingsPrivacyTest*'` | ❌ Wave 0 |
| REL-05 | The "Privacy policy" row is always present and fires an `ACTION_VIEW` intent | Compose (Robolectric `ShadowActivity`/intent capture) | `*SettingsPrivacyTest*` | ❌ Wave 0 |
| CONS-04 / D-12 | With `rewardedOfferable == false`, no dialog appears and assists stay free past the allowance | unit (VM) | `*GameViewModelAllowanceTest*` | ❌ Wave 0 |
| CONS-01 | Consent form shown before any ad request; `requestConsentInfoUpdate` every launch | **manual (device)** | — | Manual-only: UMP has no fakeable seam and needs a real form + `DEBUG_GEOGRAPHY_EEA` |
| CONS-02 | SDK initialized only after consent resolves, off the main thread | **manual (device) + code review** | — | Manual-only for the same reason; the `AtomicBoolean` and dispatcher are review items |
| PLAT-04 (regression) | No `gms`/`ump`/`libraries.ads` type outside `..platform..` | unit (ArchUnit) | `./gradlew :app:testDebugUnitTest --tests '*PlatformBoundaryTest*'` | ✅ exists |
| QUAL (regression) | No new magic numbers, no dp/sp literals, no ad-ID literals | build gate | `./gradlew check --offline` | ✅ exists |

### Sampling Rate

- **Per task commit:** `./gradlew :domain:test --offline` (allowance work) or `./gradlew :app:testDebugUnitTest --tests '<the class touched>'`
- **Per wave merge:** `./gradlew check --offline`
- **Phase gate:** `./gradlew check --offline` green + the manual device checklist (below) before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `domain/src/test/kotlin/com/app/pegsolitaire/domain/assist/AssistAllowanceTest.kt` — ADS-02, D-01/D-02/D-03
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAllowanceTest.kt` — ADS-01/03/04/05/06, D-04, D-12
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/game/GameViewModelAdClockTest.kt` — ADSF-01
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/game/AdOfferDialogTest.kt` — D-06/D-07 rendering + actions
- [ ] `app/src/test/java/com/app/pegsolitaire/ui/settings/SettingsPrivacyTest.kt` — CONS-03, REL-05
- [ ] Extend `SnapshotMapper` tests for the two new nullable fields (fail-open + range rejection)
- [ ] Extend `NoWinBannerTest` for the D-08 at-zero path
- [ ] Framework install: **none needed** — every library is already a `testImplementation`

### Manual (device-only) verification checklist

These cannot be automated in this project and must be a `checkpoint:human-verify` task:

- [ ] Fresh install with `DEBUG_GEOGRAPHY_EEA`: consent form appears **before** Home, and before any ad request
- [ ] `consentInformation.reset()` then relaunch: the form appears again (replayed first-install)
- [ ] Decline consent → assists remain free, no dialog ever appears (D-12 + CONS-04)
- [ ] Settings shows both privacy rows in EEA debug geography; only the policy row outside it
- [ ] "Privacy policy" opens the live URL in the system browser (REL-05)
- [ ] A real Google **test** rewarded ad plays; watching to completion refills; early dismiss does not
- [ ] The elapsed clock reads the same before and after a full ad watch (ADSF-01)
- [ ] AdMob dashboard shows **zero** impressions from the dev device (Pitfall 3 / Phase 6 D-05)

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | No accounts in this phase (Play Games is Phases 11–13) |
| V3 Session Management | no | No sessions |
| V4 Access Control | no | Single-user local app |
| V5 Input Validation | **yes** | The resume snapshot is untrusted input. The new allowance fields must be **range-checked, not clamped-silently** — `AssistAllowance.fromStoredOrFull` rejects anything outside `0..limit` and falls open to a full allowance, matching the WARN-02 discipline already applied to coordinates and `minimumPegs`. A forged `freeHintsRemaining: 9999` must not become 9999 free hints |
| V6 Cryptography | no | No secrets handled. Ad unit IDs are **not** cryptographic secrets — they ship in the APK; they are untracked only to stop a fork serving ads against this AdMob account (Phase 6 D-08) |
| V7 Error Handling / Logging | **yes** | Never log the consent status, TCF string, or ad response info in release. Consent failures degrade to `AdsReadiness.Unavailable`, never to a crash or an error wall (CONS-04) |
| V8 Data Protection / Privacy | **yes** | This phase is the app's first collection of the advertising ID (`com.google.android.gms.permission.AD_ID`, already merged). The Play Console **Data safety** form and the **Contains ads** declaration must agree with what the privacy policy says — Phase 14 owns filing them, but this phase is where the behaviour becomes real |
| V12 Files / Resources | **yes** | `ACTION_VIEW` with a hard-coded HTTPS constant — never a URL from storage or a route argument, which would be an intent-redirection vector |

### Known Threat Patterns for Kotlin/Compose + AdMob/UMP

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Forged resume snapshot inflates the free allowance | Tampering | Range-check on decode; fail open to the *default* allowance, never to the stored value |
| Player games the clock freeze by parking the offer dialog | Tampering (score integrity) | Freeze from accept, not from offer (Pattern 6, hazard 2) |
| Real ad unit IDs reaching a dev/CI build → invalid traffic | Repudiation / account loss | `buildConfigField` per build type + `BuildConfigIdsTest` asserting the debug prefix + `adIdLiteralCheck` scanning `src/main` — **all three already in place from Phase 6; do not weaken any of them** |
| Serving personalised ads before consent in the EEA | Information disclosure / GDPR | `AdsReadiness` gates every `requestRewarded`; `canRequestAds()` gates `MobileAds.initialize` |
| Reward granted without a completed ad view | Elevation of privilege (against the advertiser) | `earned` set only inside `OnUserEarnedRewardListener` |
| Intent redirection via a dynamic privacy URL | Tampering | Compile-time constant only |
| SDK types leaking into `:domain` or `..ui..` | Architecture erosion → untestable, unauditable code | `PlatformBoundaryTest` (bytecode ArchUnit), already green; must remain so |

### AdMob rewarded-inventory policy — the requirements this phase must satisfy

Quoted verbatim [CITED: support.google.com/admob/answer/7313578]:

- *"Rewarded Ads … must only be served after a user affirmatively and unambiguously opts in"* → the D-06 dialog, in front of every `requestRewarded`.
- *"clear, accurate and conspicuous disclosure of the action(s) required and reward(s) offered prior to each instance"* → D-07's copy names the count and the reward, every time.
- *"Rewarded Ads must not oblige users to interact with it (for example, it must be possible to skip or dismiss them)"* → "No thanks" plus the system back gesture.
- *"Skipping the Rewarded Ad or selecting the 'no' or 'don't accept' option must not impede or interfere with the normal usage of the … app"* → the free allowance (ADS-02) and the D-12/ADS-03 fail-open grants make this true *by construction*, not by promise.
- *"cannot include any text or icons, other than to describe the reward(s) offered, to mislead or incentivize users towards a particular choice"* → the two dialog actions must be visually **peers**; do not make "No thanks" a muted micro-link under a large gradient CTA. This is a real constraint on the Candy Clay styling of D-06.

---

## Sources

### Primary (HIGH confidence)
- https://developers.google.com/admob/android/next-gen/reference/kotlin/com/google/android/libraries/ads/mobile/sdk/rewarded/RewardedAd — package, `suspend load` → `AdLoadResult`, `show(Activity, OnUserEarnedRewardListener)`, `adEventCallback`
- https://developers.google.com/admob/android/next-gen/reference/kotlin/com/google/android/libraries/ads/mobile/sdk/rewarded/RewardedAdEventCallback — inherited callback set + `FullScreenContentError`
- https://developers.google.com/admob/android/next-gen/reference/kotlin/com/google/android/libraries/ads/mobile/sdk/rewarded/OnUserEarnedRewardListener — `onUserEarnedReward(reward: RewardItem)`
- https://developers.google.com/admob/android/next-gen/reference/kotlin/com/google/android/libraries/ads/mobile/sdk/rewarded/RewardedAdPreloader — `start`/`pollAd`/`isAdAvailable`/`destroy`/`getNumAdsAvailable`
- https://developers.google.com/admob/android/next-gen/reference/kotlin/com/google/android/libraries/ads/mobile/sdk/common/AdLoadResult — sealed `Success`/`Failure`
- https://developers.google.com/admob/android/next-gen/reference/kotlin/com/google/android/libraries/ads/mobile/sdk/common/AdRequest — `adUnitId` on the request
- https://developers.google.com/admob/android/next-gen/reference/kotlin/com/google/android/libraries/ads/mobile/sdk/MobileAds — `initialize` overloads (`@WorkerThread`, `@RequiresPermission(INTERNET)`), `isInitialized`
- https://developers.google.com/admob/android/next-gen/quick-start — dependency, `InitializationConfig.Builder(appId)`, background-thread requirement, **"UMP SDK requires the app ID in your app's AndroidManifest.xml"**, "ads may be preloaded on initialization"
- https://developers.google.com/admob/android/next-gen/migration — package moves, mandatory initialization, `UninitializedPropertyAccessException`
- https://developers.google.com/admob/android/next-gen/rel-notes — 1.0.0 (2026-04-14) … 1.4.0 (2026-08-20) changelog
- https://developers.google.com/admob/android/next-gen/test-ads — rewarded test unit `ca-app-pub-3940256099942544/5224354917`; `RequestConfiguration.Builder().setTestDeviceIds(...)`; emulators are test devices by default
- https://developers.google.com/admob/android/privacy — full UMP flow, `ConsentDebugSettings`/`DEBUG_GEOGRAPHY_EEA`, `reset()`
- https://developers.google.com/ad-manager/mobile-ads-sdk/android/next-gen/privacy — Next-Gen + UMP ordering; **"canRequestAds() always returns false until you have called requestConsentInfoUpdate()"**
- https://developers.google.com/admob/android/privacy/api/reference/com/google/android/ump/ConsentInformation — method list and signatures
- https://developers.google.com/admob/android/reference/privacy/kotlin/com/google/android/ump/ConsentInformation.PrivacyOptionsRequirementStatus — `REQUIRED` / `NOT_REQUIRED` / `UNKNOWN`
- https://support.google.com/admob/answer/7313578 — rewarded inventory policy, quoted verbatim above
- `https://dl.google.com/dl/android/maven2/.../maven-metadata.xml` — `ads-mobile-sdk` latest 1.4.0 (2026-08-20); `user-messaging-platform` latest 4.0.0 (2025-10-30) [curled this session]
- **In-repo inspection (this session):** `AdGateway.kt`, `RewardOutcome.kt`, `AdReason.kt`, `NoOpAdGateway.kt`, `AdIds.kt`, `ForegroundActivityProvider.kt`, `DefaultForegroundActivityProvider.kt`, `PlatformModule.kt`, `PlatformBoundaryRules.kt`, `ManifestSurfaceTest.kt`, `BuildConfigIdsTest.kt`, `GameViewModel.kt`, `GameUiState.kt`, `GameScreen.kt`, `NoWinBanner.kt`, `SettingsScreen.kt`, `SettingsUiState.kt`, `App.kt`, `MainActivity.kt`, `ResumeSnapshotDto.kt`, `SnapshotMapper.kt`, `AndroidManifest.xml`, `app/build.gradle.kts`, `gradle/libs.versions.toml`, `build-logic/.../standards.gradle.kts`, `build-logic/.../AdIdResolution.kt`, `res/values/dimens.xml`

### Secondary (MEDIUM confidence)
- https://developers.google.com/ad-manager/mobile-ads-sdk/android/next-gen/rewarded — preload snippet (`PreloadConfiguration(adRequest)`), `pollAd` "loads another ad in the background", `destroy()` guidance. Ad Manager flavour of the same guide; AdMob equivalent returned mixed legacy/Next-Gen content
- `.planning/research/STACK.md`, `.planning/research/ARCHITECTURE.md`, `.planning/research/PITFALLS.md` — milestone research; **superseded on four specific points** (see State of the Art)
- WebSearch result set for `privacyOptionsRequirementStatus` — corroborated the `REQUIRED` semantics against the reference page

### Tertiary (LOW confidence)
- The ~1 hour rewarded-ad expiry figure (documented for the legacy SDK; not restated on any Next-Gen page located) — see A1
- `PreloadConfiguration` constructor form — see A2; the Next-Gen reference page for this type was not located, and the same-named legacy type is deprecated

---

## Metadata

**Confidence breakdown:**
- Standard stack: **HIGH** — no new artifacts; both existing coordinates verified against Google Maven metadata and official docs, and already building green
- SDK API surface (rewarded, init, UMP): **HIGH** — every signature taken from the official Next-Gen Kotlin reference pages, not from tutorials or training data
- Preloader semantics: **MEDIUM** — method list verified; `PreloadConfiguration` construction and buffer defaults not fully confirmed (mitigated by recommending the manual slot)
- Architecture / integration points: **HIGH** — derived from direct reading of the actual files, not from the milestone research (which is stale on four points)
- Pitfalls: **HIGH** for the three code-level conflicts (restart/snapshot, hint ordering, `App.onCreate` in Robolectric) — each verified by reading the code and the tests; **HIGH** for the UMP sequencing pitfalls (official docs)
- Policy interpretation: **HIGH** — quoted verbatim from the AdMob rewarded inventory policy
- External prerequisite status: **LOW** — no completion record exists in `.planning/`; flagged as A5 + Open Question 3

**Research date:** 2026-08-30
**Valid until:** 2026-09-29 (30 days). The Next-Gen SDK ships roughly monthly (1.0.0 → 1.4.0 in four months), so re-check `rel-notes` if planning slips past that; the UMP API has been stable since 4.0.0 in Oct 2025.
