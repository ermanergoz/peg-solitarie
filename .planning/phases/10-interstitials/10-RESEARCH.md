# Phase 10: Interstitials - Research

**Researched:** 2026-09-02
**Domain:** Google Mobile Ads Next-Gen interstitials; pure-Kotlin frequency-cap policy; Preferences DataStore persistence; Compose input-quiescence scrim
**Confidence:** HIGH (SDK API verified from the resolved AAR bytecode; every integration point read in-repo; two genuinely open design questions flagged)

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Cadence policy values (INT-02)
- **D-01:** Level gate: an interstitial is eligible only after **every 3rd win** (3 wins since the last interstitial shown).
- **D-02:** Wall-clock gate: **minimum 90 seconds** since the last interstitial. Both D-01 AND D-02 must hold — protects against speed-cleared easy levels triggering back-to-back ads.
- **D-03:** New-install suppression: **the entire first session AND the first 5 lifetime completed levels** are interstitial-free — suppression ends only when both have passed (whichever ends later).
- **D-04:** Session cap: **max 4 interstitials per session**. A session ends after the app has been **backgrounded for 30 continuous minutes** (standard analytics convention); the boundary is computed from a persisted last-foreground timestamp so it survives process death.
- **D-05:** All cadence values are named, scoped constants (no magic numbers, per CLAUDE.md).

#### Trigger surface (INT-01, INT-03)
- **D-06:** Ad-eligible actions are **forward-moving win actions only**: Challenge "Next level" and Classic "Catalog" after a win. Excluded: Classic "Replay" (a retry — retry stays free), "Home" (an exit), and Challenge-stuck "Retry" (a loss, INT-03).
- **D-07:** **One shared cadence counter across Classic and Challenge** — the player experiences one app; mode-switching cannot dodge the cap. Single policy object, single state.

#### Quiescence pause (INT-04)
- **D-08:** On an ad-eligible tap with an ad ready: Results **dims with a scrim and all input is swallowed for 500 ms**, then the ad presents. The dim is the "something is coming" signal — no text, no spinner. 500 ms exceeds typical double-tap intervals, so a stray second tap lands on the scrim, never the ad.
- **D-09:** **Ad first, then navigate**: the ad presents over the (dimmed) Results screen; navigation to the next level / catalog proceeds on ad dismiss. If the ad fails to show, navigation proceeds immediately. The tap's intent is honored exactly once either way, and a new board never sits underneath an ad (spirit of INT-01's "never at level start").
- **D-10:** The pause/dim runs **only when an ad will actually show**. When cadence says no, or no ad is loaded, the tap navigates instantly with zero added latency — the pause is part of ad presentation, not of navigation.

#### No-fill & edge behavior
- **D-11:** No-fill at the trigger moment: **skip silently; counters keep running** (not reset). The very next eligible win can show the ad once one loads. Cadence counters/timestamps update only when an ad actually shows.
- **D-12:** Preload timing: **at eligibility, on win** — the load request fires the moment a win makes the next continue-tap ad-eligible. Mirrors Phase 9 D-09's "earliest point an offer can occur": no ad requests while first-session/first-5 suppression, the session cap, or the level/time gates make an ad impossible.
- **D-13:** Persistence split (INT-02 "survives being killed"): **DataStore-persisted** — wins-since-last-interstitial, last-interstitial wall-clock timestamp, lifetime completed-level count (for first-5 suppression), first-session-complete flag, last-foreground timestamp (for the 30-min session boundary). **In-memory** — the per-session interstitial count (it derives from the session boundary; a kill → relaunch after 30 min is a new session anyway).

### Claude's Discretion
- Exact DataStore keys/schema for the persisted cadence state (Preferences DataStore per the stack table — scalar values, no queries).
- Show-failure mid-presentation handling (map to silent skip + immediate navigation, consistent with D-11's fail-open direction) and reload cadence after a failed load.
- Scrim styling within the Candy Clay theme (color/alpha/animation of the dim).
- Where the policy object boundary sits vs. the gateway (policy decides *whether*, gateway handles *how* — model on the Phase 9 `AssistAllowance` / `AdMobAdGateway` split).
- Whether "eligibility close" (D-12) warrants preloading one win early (e.g. at 2-of-3 wins) if the ad-ready-by-tap rate needs it; keep it simple first.
- How the win counter increments relative to eligible-action taps (recommended simple reading: every win increments; the ad fires on the next eligible tap where all gates pass).
- Debug/test affordances for forcing or fast-forwarding the cadence (if needed for device verification).

### Deferred Ideas (OUT OF SCOPE)
- **Decide whether Stuck finishes should reach Results (1–2 star bands unreachable)** — reviewed again (it touches the Results trigger surface) and deliberately kept separate, consistent with Phase 9's call. Phase 10 gates interstitials on "won" (D-06), which is robust regardless of how the Stuck→Results decision lands later. Planner should avoid structural Results rework that would prejudge it.
- **Confirm audio cues on physical hardware** — Phase 7 follow-up, unrelated to ads; stays a pending todo.
- **Upgrade Wiegleb-45 opening book to depth 4** — Phase 8 follow-up compute task; stays a pending todo.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| INT-01 | Player sees an interstitial only after tapping the Results continue action, never during the win celebration or at level start | Trigger surface mapped exactly (§ Trigger Surface Anatomy). D-09's ad-first-then-navigate is *structurally* what keeps an ad off a fresh board: the next `GameViewModel` is not constructed until the nav lambda fires on dismiss. The current `ResultsRoute` invokes nav lambdas **synchronously inside `onClick`** — deferring that is the core wiring change (Pitfall 5). |
| INT-02 | Interstitials are frequency-capped and suppressed during the first session and first levels of an install | Pure-Kotlin `InterstitialCadencePolicy` shape (§ Pattern 1), Preferences DataStore persistence following the verified in-repo `ChallengeProgress` pattern (§ Pattern 3), session-boundary sourcing via `ProcessLifecycleOwner` (§ Pattern 4, Pitfall 3). Two persistence holes surfaced: existing-install counter cold start (§ Runtime State Inventory) and the in-memory session-count kill window (Open Question 2). |
| INT-03 | Interstitials never follow a lost or abandoned level | **The single hardest finding in this phase.** "Won" is NOT `pegsRemaining == 1` — Diamond-41's floor is 2. The `Results` route args do not carry `minimumPegs`, but `stars == ScoreParams.THREE_STARS` is a *provably exact* restatement of `GameStatus.Won`, deliberately maintained as such in-source (§ Pattern 2, Pitfall 1). |
| INT-04 | A brief pause precedes an interstitial so a tap on the board cannot land on the ad | Compose input-swallowing scrim (§ Pattern 5) — `pointerInput` with `awaitPointerEvent` consumption plus `BackHandler`, hoisted into `ResultsUiState`. dp values must go through `dimens.xml` (the `dpSpLiteralCheck` build gate fails the build otherwise). |
| ADSF-01 *(carried, owned by Phase 9)* | Game clock pauses for the full duration of any ad presentation | **Structurally already satisfied on this path, and this is worth asserting rather than re-plumbing.** `GameViewModel.freezeAndFinishIfTerminal` cancels `tickerJob` at terminal status *before* Results is ever reached, so no clock is running when an interstitial shows. D-09 then guarantees the next level's `startClock` runs only after dismiss (§ Pattern 6). Verification is a *test that the invariant holds*, not new freeze code. |
</phase_requirements>

---

## Summary

Phase 10 is a **wiring and policy phase, not an SDK-learning phase**. Every hard SDK problem — consent gating, main-thread confinement, foreground-Activity capture, single-show ad lifecycle, fail-open degradation, the Hilt/ArchUnit SDK-containment trap — was already solved and documented in Phase 9's `RewardedAdSlot` / `AdMobAdGateway`. The GMA Next-Gen `InterstitialAd` API is a near-exact structural twin of `RewardedAd` (verified by decompiling the resolved 1.4.0 AAR): the same `Companion.load(AdRequest, Continuation): AdLoadResult<T>` suspend entry point, the same `Ad.destroy()`, and `AdEventCallback`'s `onAdDismissedFullScreenContent` / `onAdFailedToShowFullScreenContent`. It differs in exactly one way: `show(Activity)` takes **no reward listener**. An `InterstitialAdSlot` cloned from `RewardedAdSlot` with the reward plumbing removed is the whole platform-side story.

The genuinely new work is above the seam, and it has three parts. First, a **pure-Kotlin `InterstitialCadencePolicy`** — five gates (D-01 level, D-02 wall-clock, D-03 first-session/first-5, D-04 session cap, plus readiness), constructor-injected state, zero Android deps, TDD'd as a truth table before wiring, per the ROADMAP note. Second, **new Preferences DataStore state** in the existing shared `app_prefs` store, following `ChallengeProgress`'s verified fail-open `catch { emit(emptyPreferences()) }` + `stateIn(Eagerly)` shape. Third — and this is where the surprises live — **the Results trigger surface**, which today is a pure pass-through: `ResultsViewModel` injects no gateway, and `ResultsRoute` hands nav lambdas straight to `ResultsActions`, which invokes them synchronously inside `onClick`. Introducing D-08's 500 ms scrim and D-09's navigate-on-dismiss means those lambdas must become *deferred* — a one-shot effect the route consumes — which is a real structural change to a screen the Deferred Ideas section explicitly asks not to over-refactor.

Two findings deserve the planner's attention before task decomposition. **(1) "Won" is not `pegsRemaining == 1`.** Diamond-41's proven floor is two pegs, `GameStatus.Won` is `pegCount <= spec.minimumPegs`, and the `Results` route args carry no `minimumPegs`. The saving grace is that `ScoreCalculator.stars` uses the *same* `<=` comparison against the *same* target, and `GameViewModel` documents in-source that this is deliberate so "the status can never disagree" — making `stars == ScoreParams.THREE_STARS` an exact, zero-route-change restatement of "won". **(2) The session boundary needs a real foreground signal.** The existing `ForegroundActivityProvider` is unusable for this: it clears on `onActivityPaused`, and showing a fullscreen ad pauses the host, so every ad presentation would forge a "backgrounded" event. `androidx.lifecycle:lifecycle-process` (`ProcessLifecycleOwner`) is the correct source — it debounces same-process Activity transitions, so an ad Activity does not register as a background.

**Primary recommendation:** Clone `RewardedAdSlot` → `InterstitialAdSlot` (drop the reward listener, keep the age bound and single-attempt retry), write `InterstitialCadencePolicy` as a pure `:domain`-style object driven by a `CadenceState` value type and an injected `nowMs`, persist that state in `app_prefs` via a `ChallengeProgress`-shaped repository, gate the trigger on `stars == ScoreParams.THREE_STARS` plus D-06's action identity, and convert `ResultsRoute`'s nav lambdas into a deferred one-shot so the scrim and the ad can sit in front of them.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| "May an interstitial show right now?" (D-01..D-04) | **Pure Kotlin policy** (`:domain` or an Android-free `platform/ads` class) | — | ROADMAP mandates TDD-as-logic before wiring. Zero Android deps ⇒ JVM unit tests, matching `AssistAllowance` / `ScoreCalculator`. |
| Cadence state persistence (D-13) | **Data layer** (`data/`, Preferences DataStore) | — | Scalars, no queries — exactly the CLAUDE.md stack table's DataStore criterion. Must reuse the single `appDataStore` delegate. |
| Session-boundary foreground timestamp (D-04) | **Platform layer** (`ProcessLifecycleOwner` observer) | Data layer (writes the timestamp) | Android lifecycle is inherently platform. Policy consumes only a `Long`. |
| Interstitial load / hold / show (SDK) | **Platform layer** (`platform/ads/InterstitialAdSlot` + `AdMobAdGateway`) | — | ArchUnit `onlyPlatformDependsOnPlaySdks` confines GMA imports to `..platform..`. SDK types must not escape into any signature (Pitfall 4). |
| Trigger decision at the Results tap (D-06) | **UI layer** (`ResultsViewModel`) | Pure policy (the *whether*) | The VM is the only place that knows both the action identity and the outcome; it maps to the policy, mirroring `GameViewModel`'s `AdReason ↔ AssistKind` bridging. |
| Quiescence scrim + input swallow (D-08) | **UI layer** (`ResultsUiState` + stateless `ResultsScreen`) | — | State hoisting per CLAUDE.md; scrim visibility is UI state, rendered statelessly. |
| Navigate-on-dismiss sequencing (D-09) | **UI layer** (`ResultsViewModel` one-shot effect → `ResultsRoute`) | — | Navigation targets are supplied by `AppNavHost`; the *timing* must move into the VM so it can be sequenced behind the ad. |
| Clock stays paused (ADSF-01) | **Already-shipped Phase 9 mechanism** (`GameViewModel` ticker) | — | Terminal status cancels the ticker before Results; nothing to add, only to assert. |

---

## Standard Stack

### Core — already on the graph, no new dependency required

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk` | **1.4.0** | `InterstitialAd` load/show | Already resolved in `gradle/libs.versions.toml` and in the local Gradle cache. `InterstitialAd`, `InterstitialAdEventCallback`, `InterstitialAdPreloader` all present in the AAR. [VERIFIED: AAR bytecode at `~/.gradle/caches/modules-2/files-2.1/com.google.android.libraries.ads.mobile.sdk/ads-mobile-sdk/1.4.0/…/ads-mobile-sdk-1.4.0.aar`] |
| `androidx.datastore:datastore-preferences` | **1.2.1** | Persisted cadence state (D-13) | Already on the graph and already the project's scalar store. Reuse the single `Context.appDataStore` delegate — a second `preferencesDataStore(name = …)` on the same file crashes at runtime. [VERIFIED: `data/AppPreferences.kt`] |
| `org.jetbrains.kotlinx:kotlinx-coroutines-core` | **1.11.0** | `StateFlow` cadence state, `suspendCancellableCoroutine` show bridge | Already on the graph; the Phase 9 gateway's presentation bridge is the exact precedent. [VERIFIED: `gradle/libs.versions.toml`] |

### Supporting — one candidate new artifact

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `androidx.lifecycle:lifecycle-process` | **2.9.4** (must match the atomic group pin) | `ProcessLifecycleOwner` → the D-04 last-foreground timestamp | Needed **only if** the session boundary is sourced from real app foreground/background. Artifact confirmed present on Google Maven (`HTTP 200` for `lifecycle-process-2.9.4.aar`) and already resident in the local Gradle cache. [VERIFIED: `curl` to `dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-process/2.9.4/…` + cache listing] |

⚠️ **`androidx.lifecycle` is an ATOMIC GROUP** — `libs.versions.toml` documents this explicitly and pins `androidxLifecycle = "2.9.4"` because 2.11.0's `-compose` variants require `compileSdk 37`, which is uninstallable in this environment. Adding `lifecycle-process` **must** use `version.ref = "androidxLifecycle"`; a bare `androidx.lifecycle:lifecycle-process:latest` would drag the whole group forward and break the build. [VERIFIED: `gradle/libs.versions.toml` lines 48–52]

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Hand-rolled `InterstitialAdSlot` | SDK `InterstitialAdPreloader` | Rejected for the same reason `RewardedAdSlot`'s KDoc rejects `RewardedAdPreloader`: it maintains a *refilling buffer* for the session, which fights D-12's "no request while an ad is impossible". One slot has that property by construction. **Consistency with the shipped rewarded path is itself the argument.** |
| `ProcessLifecycleOwner` for the session boundary | Reuse `ForegroundActivityProvider` | **Rejected — it is actively wrong here.** It clears on `onActivityPaused`, and a fullscreen ad pauses the host Activity, so every interstitial would forge a background event and could reset the session counter mid-session (Pitfall 3). |
| `ProcessLifecycleOwner` for the session boundary | "Last game activity" timestamp, written on every win | Viable, dependency-free, and 100% JVM-testable. Diverges from D-04's literal "last-foreground timestamp" wording but is behaviourally close for a game whose only meaningful activity is completing levels. **Offer to the user if the planner wants to avoid the new artifact** (Open Question 3). |
| `stars == THREE_STARS` as the win test | Add `minimumPegs` to the `Results` route args | Explicit and self-documenting, but touches a typed nav route and the WARN-03 arg-clamping boundary in `ResultsViewModel` — more surface than D-06 needs, and closer to the "structural Results rework" the Deferred Ideas section warns against. |
| DataStore for the session count | Persist the in-session count too | D-13 locks it in-memory. Persisting would close the kill-window hole (Open Question 2) at the cost of contradicting a locked decision. Surface, do not unilaterally change. |

**Installation:** No `npm install` equivalent. If the session boundary uses `ProcessLifecycleOwner`, add to `gradle/libs.versions.toml`:

```toml
# [libraries]
androidx-lifecycle-process = { group = "androidx.lifecycle", name = "lifecycle-process", version.ref = "androidxLifecycle" }
```

and `implementation(libs.androidx.lifecycle.process)` in `app/build.gradle.kts`. **No new version key** — the atomic-group pin is reused deliberately.

---

## Package Legitimacy Audit

This is a Gradle/Maven Android project; `slopcheck` targets npm/PyPI/crates and has no Maven ecosystem support, so registry-legitimacy verification was performed directly against **Google Maven** and the **local Gradle module cache** instead. No package name in this phase originates from a web search or from training recall — every artifact is either already declared in `gradle/libs.versions.toml` or is a sibling of an already-declared artifact within the same pinned atomic group.

| Package | Registry | Age | Source Repo | Verification | Disposition |
|---------|----------|-----|-------------|--------------|-------------|
| `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.4.0` | Google Maven | GA since Apr 2026 | Google (first-party) | Already on the graph; AAR resolved in local cache; interstitial classes enumerated from bytecode | **Approved — already declared, no new install** |
| `androidx.datastore:datastore-preferences:1.2.1` | Google Maven | Stable | AOSP (first-party) | Already declared and in active use | **Approved — already declared** |
| `androidx.lifecycle:lifecycle-process:2.9.4` | Google Maven | Stable line | AOSP (first-party) | `HTTP 200` on the exact AAR URL; artifact directory present in local Gradle cache; same atomic group as five already-declared `androidx.lifecycle` artifacts | **Approved — conditional on the session-boundary decision** |

**Packages removed due to a `[SLOP]` verdict:** none.
**Packages flagged suspicious:** none.

All three are first-party Google/AOSP artifacts served from `dl.google.com`, resolved through a version catalog that is the project's mandated single source of truth. The slopsquatting threat model that `slopcheck` addresses (a plausibly-named package uploaded to an open registry) does not apply: Google Maven is not open to third-party publication under these group IDs. **No `checkpoint:human-verify` gate is required for any install in this phase.**

---

## Architecture Patterns

### System Architecture Diagram

```
                       ┌──────────────────────────────────────────┐
   player taps         │  ResultsScreen  (stateless)              │
   "Next level" /  ───▶│  ResultsActions → onNextLevel/onCatalog  │
   "Catalog"           └────────────────────┬─────────────────────┘
                                            │  action identity + outcome
                                            ▼
                       ┌──────────────────────────────────────────┐
                       │  ResultsViewModel                        │
                       │  ① D-06 gate: is this a WIN action?      │
                       │     won = stars == THREE_STARS  ─────────┼── INT-03
                       │     action ∈ {NextLevel, Catalog}        │
                       └────────────────────┬─────────────────────┘
                          not a win action  │  win action
                    ┌───────────────────────┴──────────────┐
                    ▼                                      ▼
        ┌────────────────────┐          ┌──────────────────────────────────┐
        │ navigate NOW       │          │ ② InterstitialCadencePolicy      │
        │ (zero latency,     │          │    (PURE KOTLIN, no Android)     │
        │  D-10)             │          │  D-01 wins-since-last  >= 3 ?    │
        └────────────────────┘          │  D-02 now - lastShown  >= 90s ?  │
                    ▲                   │  D-03 firstSessionDone && lvl>5? │
                    │                   │  D-04 sessionShown     <  4 ?    │
                    │                   └──────────┬───────────────────────┘
                    │                    deny      │ allow
                    ├───────────────────────────────┤
                    │                               ▼
                    │                   ┌──────────────────────────────────┐
                    │       no ad held  │ ③ AdGateway.interstitialReady?   │
                    ├───────────────────┤    (InterstitialAdSlot, D-11)    │
                    │  (silent skip,    └──────────┬───────────────────────┘
                    │   counters keep              │ ad held
                    │   running)                   ▼
                    │                   ┌──────────────────────────────────┐
                    │                   │ ④ SCRIM ON — dim + swallow ALL   │
                    │                   │    input for 500 ms  (D-08/INT-04)│
                    │                   └──────────┬───────────────────────┘
                    │                              ▼
                    │                   ┌──────────────────────────────────┐
                    │                   │ ⑤ capture Activity (BEFORE show) │
                    │                   │    InterstitialAd.show(activity) │
                    │                   └──────────┬───────────────────────┘
                    │        onAdFailedToShow      │ onAdDismissed
                    │◀─────────────────────────────┤
                    │                              ▼
                    │                   ┌──────────────────────────────────┐
                    │                   │ ⑥ record shown: lastShownMs=now, │
                    │                   │    winsSinceLast=0, session++    │
                    │                   │    slot.destroy() + reload       │
                    │                   └──────────┬───────────────────────┘
                    │                              ▼
                    └──────────────────▶ ⑦ scrim OFF, emit one-shot nav effect
                                              (D-09 — ad first, THEN navigate)
                                                   │
                                                   ▼
                                        AppNavHost → Game(N+1) / Catalog
                                        (new GameViewModel → fresh clock
                                         starts only NOW — ADSF-01 holds)

   ── side channel ─────────────────────────────────────────────────────────
   on WIN (any mode):  winsSinceLast++ , lifetimeCompleted++   [DataStore]
                       if policy says "next tap would be eligible"
                          → AdGateway.preloadInterstitial()    (D-12)

   ProcessLifecycleOwner ON_STOP → persist lastForegroundMs    [DataStore]
   ProcessLifecycleOwner ON_START → if now - lastForegroundMs >= 30min
                                       → new session: sessionShown = 0
                                       → firstSessionDone = true
```

### Recommended Project Structure

```
domain/src/main/kotlin/com/app/pegsolitaire/domain/ads/     # NEW (see note)
├── InterstitialCadence.kt        # CadenceState value type + decision fn
└── InterstitialCadenceParams.kt  # D-01..D-04 scoped constants (D-05)

app/src/main/java/com/app/pegsolitaire/
├── data/
│   └── InterstitialCadenceStore.kt   # NEW: app_prefs-backed persistence (D-13)
├── platform/ads/
│   ├── InterstitialAdSlot.kt         # NEW: RewardedAdSlot twin, no reward
│   ├── AdConstants.kt                # EXTEND: INTERSTITIAL_AD_MAX_AGE_MS
│   ├── AdGateway.kt                  # EVOLVE: showInterstitial() signature
│   ├── AdMobAdGateway.kt             # REPLACE the ~line-134 no-op
│   └── NoOpAdGateway.kt              # keep interstitial a no-op (stub parity)
├── platform/lifecycle/
│   └── AppForegroundTracker.kt       # NEW (if ProcessLifecycleOwner is used)
└── ui/results/
    ├── ResultsUiState.kt             # EXTEND: adScrimVisible, pendingNav
    ├── ResultsViewModel.kt           # EXTEND: policy + gateway + one-shot nav
    ├── ResultsActions.kt             # EXTEND: pass action identity up
    ├── ResultsScreen.kt              # EXTEND: render scrim
    └── ResultsRoute.kt               # EXTEND: consume the one-shot nav effect
```

**Note on the policy's home.** `.planning/research/ARCHITECTURE.md` places `InterstitialCadencePolicy` in `platform/ads/` ("it is app policy, not game rules") while keeping it Android-free. Phase 6 D-04 says "policy objects live in pure `:domain`". Both readings satisfy the ROADMAP's actual requirement (pure Kotlin, JVM-testable). **Recommendation: `:domain`.** `DomainPurityTest` then *mechanically enforces* the purity the ROADMAP note demands, whereas a `platform/ads` placement relies on reviewer discipline — and `ArchUnit`'s `onlyPlatformDependsOnPlaySdks` rule permits SDK imports in `..platform..`, so the gate would be silent if someone later reached for an Android API there. Precedent is exact: `AssistAllowance` lives in `:domain` and its KDoc explains at length that it duplicates `AdReason` rather than invert the module graph — the same trade, already made once.

### Pattern 1: The cadence policy as a pure decision over an immutable state value

Model directly on `AssistAllowance`: an `@Immutable`-style `data class` of counters, mutators returning new instances, and a decision function taking `nowMs` as a parameter rather than reading a clock.

```kotlin
// Source: pattern extracted from domain/…/assist/AssistAllowance.kt (in-repo, VERIFIED)

/** Scoped tunables for interstitial cadence (D-01..D-05, QUAL-03 no-magic-numbers). */
object InterstitialCadenceParams {
    /** Wins that must accrue since the last interstitial before one is eligible (D-01). */
    const val WINS_BETWEEN_INTERSTITIALS = 3

    /** Minimum wall-clock gap between interstitials (D-02) — guards speed-cleared levels. */
    const val MIN_GAP_MS = 90_000L

    /** Lifetime completed levels that are interstitial-free on a new install (D-03). */
    const val SUPPRESSED_FIRST_LEVELS = 5

    /** Maximum interstitials in one session (D-04). */
    const val MAX_PER_SESSION = 4

    /** Background duration that ends a session (D-04) — the analytics convention. */
    const val SESSION_GAP_MS = 30L * 60L * 1_000L
}

/** Everything the cadence decision needs, as plain data. No Android, no clock, no I/O. */
data class InterstitialCadenceState(
    val winsSinceLastInterstitial: Int = 0,
    val lastInterstitialAtMs: Long = NEVER_SHOWN,
    val lifetimeCompletedLevels: Int = 0,
    val firstSessionComplete: Boolean = false,
    val interstitialsThisSession: Int = 0,
) {
    /**
     * Whether an interstitial may show at [nowMs]. ALL gates must pass (D-01 AND D-02
     * AND D-03 AND D-04) — the conjunction is the decision, and each clause is named
     * so a failing test names the gate it broke.
     */
    fun mayShow(nowMs: Long): Boolean =
        newInstallSuppressionOver() &&
            underSessionCap() &&
            enoughWins() &&
            enoughElapsed(nowMs)

    /** D-03: BOTH conditions must have passed — whichever ends later. */
    private fun newInstallSuppressionOver(): Boolean =
        firstSessionComplete &&
            lifetimeCompletedLevels >= InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS

    private fun underSessionCap(): Boolean =
        interstitialsThisSession < InterstitialCadenceParams.MAX_PER_SESSION

    private fun enoughWins(): Boolean =
        winsSinceLastInterstitial >= InterstitialCadenceParams.WINS_BETWEEN_INTERSTITIALS

    /**
     * D-02. `NEVER_SHOWN` must pass rather than fail: a player who has never seen an
     * interstitial has, trivially, waited long enough. Encoding "never" as 0L and
     * comparing `now - 0 >= 90s` happens to work, but says the ad was shown at the
     * epoch — state the intent instead.
     *
     * Wall-clock is REQUIRED here (not elapsedRealtime) because the value must survive
     * process death (D-13), which no monotonic timebase does. The cost is that a clock
     * change can move it; a negative delta is therefore treated as "gate open", never
     * as a lock-out.
     */
    private fun enoughElapsed(nowMs: Long): Boolean =
        lastInterstitialAtMs == NEVER_SHOWN ||
            nowMs - lastInterstitialAtMs >= InterstitialCadenceParams.MIN_GAP_MS

    /** D-11: counters advance ONLY when an ad actually showed. */
    fun recordShown(nowMs: Long): InterstitialCadenceState =
        copy(
            winsSinceLastInterstitial = 0,
            lastInterstitialAtMs = nowMs,
            interstitialsThisSession = interstitialsThisSession + 1,
        )

    /** Every win advances both counters, in both modes (D-07: one shared counter). */
    fun recordWin(): InterstitialCadenceState =
        copy(
            winsSinceLastInterstitial = winsSinceLastInterstitial + 1,
            lifetimeCompletedLevels = lifetimeCompletedLevels + 1,
        )

    /** D-04: a new session zeroes the in-memory count and closes first-session suppression. */
    fun startNewSession(): InterstitialCadenceState =
        copy(interstitialsThisSession = 0, firstSessionComplete = true)

    companion object {
        /** Sentinel: no interstitial has ever been shown on this install. */
        const val NEVER_SHOWN = -1L
    }
}
```

**Why `nowMs` is a parameter, not an injected `Clock`:** it makes the D-02 and D-04 truth tables testable as pure arithmetic with no fake-clock scaffolding — the same reason `ProgressRepository.recordResult` already takes `nowMs = System.currentTimeMillis()` from its caller. [VERIFIED: `ResultsViewModel.recordSolvedPlayOnce`]

### Pattern 2: Deriving "won" from the Results route args — the exact equivalence

This is the load-bearing correctness argument for INT-03. It should be reproduced in a KDoc, because it is non-obvious and a future reader will "simplify" it back to `pegsRemaining == 1` otherwise.

```kotlin
// Source: app/…/ui/game/GameViewModel.kt statusFor()  (in-repo, VERIFIED)
private fun statusFor(board: Board): GameStatus =
    when {
        !rulesEngine.isGameOver(board) -> GameStatus.Playing
        board.pegCount <= board.spec.minimumPegs -> GameStatus.Won
        else -> GameStatus.Stuck
    }

// Source: domain/…/scoring/ScoreCalculator.kt stars()  (in-repo, VERIFIED)
fun stars(pegsRemaining: Int, minimumPegs: Int = ScoreParams.THREE_STAR_MAX_PEGS): Int =
    when {
        pegsRemaining <= minimumPegs -> ScoreParams.THREE_STARS
        pegsRemaining <= minimumPegs + ScoreParams.TWO_STAR_BAND_WIDTH -> ScoreParams.TWO_STARS
        else -> ScoreParams.MIN_STARS
    }
```

Both test `pegsRemaining <= minimumPegs` against the **same** `BoardSpec.minimumPegs`. `GameViewModel`'s own KDoc states this is deliberate: *"The comparison is `<=` rather than `==` so the status can never disagree with `ScoreCalculator.stars`, whose 3-star band is also `<= minimumPegs`."* Therefore, on the `Results` route:

```
GameStatus.Won  ⟺  stars == ScoreParams.THREE_STARS
```

…with **no need to add `minimumPegs` to the route args.**

**What this rules out.** `pegsRemaining == 1` is wrong: Diamond-41's proven floor is 2 pegs (Phase 8, GF(4) colour invariant), so a *perfect* Diamond finish would be classified as a loss and silently denied its interstitial. Note that `ResultsUiState.showNextLevel` **does** hardcode `PEGS_REMAINING_SOLVED = 1` — that is correct there only because every Challenge footprint has `minimumPegs == 1`, and it is exactly the line a developer would copy into the Classic win test by mistake. [VERIFIED: `ResultsUiState.kt`, `GameViewModel.kt`, `ScoreParams.kt`]

**The honest caveat, which the planner must surface:** for Classic, `tierFor` renders a 2-star finish as `SOLVED` ("Solved!"). Under this test that same finish is *not* a win and gets no interstitial. The behaviours are consistent with INT-03 (the player did not reach the board's perfect finish) and fail in the player's favour, but the copy and the gate use different words for the same result. See Open Question 1.

### Pattern 3: Cadence persistence — the verified in-repo DataStore shape

Follow `ChallengeProgress` exactly. Three properties are non-negotiable and all three are already proven in-repo:

```kotlin
// Source: app/…/data/ChallengeProgress.kt  (in-repo, VERIFIED)

// 1. Scoped key object — no inline literals (QUAL-03).
internal object InterstitialCadencePreferences {
    val WINS_SINCE_LAST_KEY = intPreferencesKey("interstitial_wins_since_last")
    val LAST_SHOWN_AT_KEY = longPreferencesKey("interstitial_last_shown_at")
    val LIFETIME_COMPLETED_KEY = intPreferencesKey("interstitial_lifetime_completed")
    val FIRST_SESSION_DONE_KEY = booleanPreferencesKey("interstitial_first_session_done")
    val LAST_FOREGROUND_AT_KEY = longPreferencesKey("app_last_foreground_at")
}

// 2. Reads FAIL OPEN through the shared store — never a second delegate.
val state: StateFlow<InterstitialCadenceState> =
    dataStore.data
        .catch { emit(emptyPreferences()) }   // corrupt/IO read -> defaults
        .map { it.toCadenceState() }
        .stateIn(scope, SharingStarted.Eagerly, InterstitialCadenceState())

// 3. Writes are best-effort and merge INSIDE the edit transaction (no lost updates).
fun recordShown(nowMs: Long) {
    scope.launch {
        storageFailSafe(fallback = Unit) {
            dataStore.edit { prefs ->
                prefs[LAST_SHOWN_AT_KEY] = nowMs
                prefs[WINS_SINCE_LAST_KEY] = 0
            }
        }
    }
}
```

**`storageFailSafe` is mandatory, not decorative.** `ChallengeProgress.advancePast`'s comment records why: the app-lifetime scope's `SupervisorJob` isolates a failed child but does **not** swallow the exception, so an unguarded DataStore IO failure crashes the app. [VERIFIED: `data/ChallengeProgress.kt`, `data/StorageFailSafe.kt`]

**Fail-open direction for cadence specifically:** a damaged store should read back as *default* state, which — because `firstSessionComplete` defaults to `false` — means **no interstitial**. That is the correct direction: a corrupt read costs revenue, never the player's goodwill.

### Pattern 4: The session boundary via `ProcessLifecycleOwner`

```kotlin
// Source: developer.android.com/topic/libraries/architecture/lifecycle (CITED)
@Singleton
class AppForegroundTracker @Inject constructor(
    private val store: InterstitialCadenceStore,
) : DefaultLifecycleObserver {

    fun attach() = ProcessLifecycleOwner.get().lifecycle.addObserver(this)

    /** Persist the moment the whole app went to background (D-04, D-13). */
    override fun onStop(owner: LifecycleOwner) = store.recordBackgrounded(System.currentTimeMillis())

    /** A gap >= 30 min starts a new session: zero the in-memory count, close first-session. */
    override fun onStart(owner: LifecycleOwner) = store.startSessionIfGapElapsed(System.currentTimeMillis())
}
```

**Why `ProcessLifecycleOwner` and not the existing provider:** `ProcessLifecycleOwner` dispatches `ON_STOP` only after *all* Activities in the process have stopped, and debounces same-process transitions — so a fullscreen ad Activity (same process) does **not** register as a background. `DefaultForegroundActivityProvider` clears on `onActivityPaused`, which fires on every single ad presentation. Using the latter would zero the session counter each time an ad shows, defeating D-04 entirely. [VERIFIED: `platform/activity/DefaultForegroundActivityProvider.kt` + CITED: AndroidX Lifecycle docs]

`attach()` is called from `App.onCreate()`, where `ForegroundActivityProvider.attachTo(application)` already is — one established hook, one more line.

### Pattern 5: The quiescence scrim (INT-04 / D-08)

The scrim must swallow **all** input, not merely cover the buttons. Two channels:

```kotlin
// Pointer input: consume every event in the pass, so nothing reaches what is beneath.
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(scrimColor)               // alpha token, Candy Clay theme
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial)
                        .changes.forEach { it.consume() }
                }
            }
        }
        // Semantics: announce the transient block rather than leaving a silent dead zone.
        .semantics { /* … */ },
)

// Back channel: a queued back press must not escape either.
BackHandler(enabled = state.adScrimVisible) { /* deliberately swallow */ }
```

Two gates apply to this composable:
- **`dpSpLiteralCheck`** fails the build on any `dp`/`sp` literal under `src/main` — every dimension goes through `dimens.xml`. [VERIFIED: `build-logic/src/main/kotlin/standards.gradle.kts:177`]
- **UI-SPEC S3 one-accent discipline** — the scrim dims the whole screen uniformly and must not introduce a second accent against `CtaGradient`.

**The 500 ms is a *minimum*, and the ad may take longer to appear.** Design the state so the scrim stays up from the tap until `onAdShowedFullScreenContent`, rather than being a fixed-duration animation that ends while the ad is still opening — a scrim that lifts before the ad renders reopens the exact accidental-tap window INT-04 exists to close.

### Pattern 6: Why ADSF-01 needs assertion, not implementation

```kotlin
// Source: app/…/ui/game/GameViewModel.kt  (in-repo, VERIFIED)
private fun freezeAndFinishIfTerminal(status: GameStatus, nextBoard: Board) {
    tickerJob?.cancel()
    tickerJob = null
    …
}
```

The ticker is already cancelled at terminal status — *before* Results is ever navigated to. So during an interstitial shown from Results, **no clock is running at all**. And because D-09 shows the ad *before* navigating, the next level's `GameViewModel` (and its `startClock`) is not constructed until dismiss. ADSF-01 therefore holds by construction on this path.

The planner should express this as an **executable invariant**, not as new freeze code: a test asserting that the elapsed seconds carried into Results equal the elapsed seconds carried out of it, and that no `startClock` occurs before the dismiss callback. Adding an `unfreezeClockAfterAd`-style call here would be actively wrong — there is no frozen clock to resume, and the resumed level is a *different* ViewModel instance.

### Anti-Patterns to Avoid

- **`showInterstitial()` in a `LaunchedEffect` on the Results route.** Named in `PITFALLS.md` §6 as the canonical failure. It fires on composition — i.e. during the win celebration — violating INT-01 directly.
- **Treating `pegsRemaining == 1` as "won".** Breaks Diamond-41 (Pattern 2).
- **Navigating first, then showing the ad.** Puts a fullscreen ad over a freshly-started board — the "never at level start" violation D-09 exists to prevent, and it starts the new level's clock under the ad (ADSF-01).
- **Resetting cadence counters on a no-fill.** D-11 is explicit: counters keep running; only an *actual show* advances them.
- **Exposing `InterstitialAd` in any constructor parameter or member signature.** See Pitfall 4 — it breaks `PlatformBoundaryTest` via Hilt's generated component, with a violation message that names the wrong file.
- **Requesting an ad while cadence forbids one.** D-12 mirrors Phase 9's `preloadRewarded` guard (`if (rewardedOfferable.value) slot.preload()`); the interstitial preload must be gated by *both* consent readiness and the cadence policy.
- **A second `preferencesDataStore(name = …)` delegate.** Crashes at runtime with "There are multiple DataStores active for the same file". Reuse `Context.appDataStore`. [VERIFIED: `data/AppPreferences.kt`]

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| App-wide foreground/background detection | An Activity-counting `ActivityLifecycleCallbacks` tracker | `ProcessLifecycleOwner` (`lifecycle-process`) | Correct debouncing of same-process transitions is the entire difficulty; a hand-rolled counter fires spuriously on rotation, on Activity A→B, and on every fullscreen ad. |
| Single-show ad lifecycle, age-out, one-attempt retry | A new bespoke holder | **Clone `RewardedAdSlot`** | It already encodes single-show discipline, `destroy()` on recycle, monotonic age bound, shared in-flight `Deferred`, app-scope load survival, and `runCatching` + `ensureActive`. Every one of those is a bug someone already fixed. |
| Callback → coroutine bridging for the presentation | `suspendCoroutine` + ad-hoc flags | `suspendCancellableCoroutine` with `isActive` guards + `invokeOnCancellation { ad.adEventCallback = null }` | Verbatim the shipped `showForOutcome` shape; prevents double-resume and a dead continuation reachable through a retained SDK object. |
| Persisted scalar state with corruption tolerance | Raw `dataStore.edit` calls | `storageFailSafe(fallback = …)` + `catch { emit(emptyPreferences()) }` | An unguarded IO failure on the app-lifetime scope **crashes the app** — documented in-repo (T-05-13). |
| Frequency-cap arithmetic | Timer/handler-based scheduling | A pure `mayShow(nowMs)` conjunction over an immutable state | The ROADMAP mandates TDD-as-logic. Pure arithmetic makes the D-01×D-02×D-03×D-04 truth table exhaustively testable with zero scaffolding. |
| Input blocking during the scrim | `enabled = false` on each button | `pointerInput` consumption on a full-size overlay **plus** `BackHandler` | Disabling buttons leaves the board, the top bar, and the back gesture live — three routes for a stray tap that INT-04 must close. |

**Key insight:** Phase 9 paid the whole cost of learning this SDK and wrote the reasoning down in KDoc. The dominant risk in Phase 10 is not that something is hard — it is that a second, subtly different ad-holding class gets written and drifts from the first. **Symmetry with `RewardedAdSlot` is a design requirement, not a stylistic preference.**

---

## Common Pitfalls

### Pitfall 1: "Won" is not `pegsRemaining == 1` — Diamond-41 breaks the obvious test
**What goes wrong:** The Classic win gate is written as `pegsRemaining == 1` (copying `ResultsUiState.showNextLevel`'s `PEGS_REMAINING_SOLVED`). Diamond-41's proven minimum is 2 pegs, so a *perfect* Diamond finish is misclassified as a loss and permanently denied an interstitial.
**Why it happens:** `PEGS_REMAINING_SOLVED = 1` is right there in the same file, and it *is* correct for Challenge (every Challenge footprint has `minimumPegs == 1`). The route args carry no `minimumPegs` to contradict it.
**How to avoid:** Use `stars == ScoreParams.THREE_STARS` (Pattern 2) and KDoc the equivalence proof.
**Warning signs:** Any `== 1` or `<= 1` in the interstitial trigger path; a Diamond-41 test case absent from the win-gate test table.

### Pitfall 2: `ForegroundActivityProvider.current` is null for the entire ad presentation
**What goes wrong:** Code awaits the Activity from inside `onAdDismissedFullScreenContent` and hangs forever — no crash, no log.
**Why it happens:** Showing a fullscreen ad pauses the host, and the provider identity-clears on `onActivityPaused`.
**How to avoid:** Capture the Activity **before** `show()`, exactly as `AdMobAdGateway.requestRewarded` does, and never re-await inside a callback. Bound every `awaitActivity()` with `AdConstants.ACTIVITY_WAIT_MS`.
**Warning signs:** `awaitActivity()` inside any `AdEventCallback` override. Already documented in-repo as Pitfall 13 / T-09-45.

### Pitfall 3: Using the Activity provider as the session-boundary signal forges a background on every ad
**What goes wrong:** D-04's session counter resets each time an interstitial shows, because the provider reads the ad's Activity pause as "app backgrounded". The session cap silently never binds.
**Why it happens:** `DefaultForegroundActivityProvider` is the app's only existing lifecycle hook, so it looks like the natural source.
**How to avoid:** `ProcessLifecycleOwner` (Pattern 4) — it does not dispatch `ON_STOP` for a same-process ad Activity.
**Warning signs:** `lastForegroundMs` updating more than once per genuine app switch; four interstitials never being reached in a long session.

### Pitfall 4: A GMA type in any Hilt-visible signature fails `PlatformBoundaryTest` — and blames the wrong file
**What goes wrong:** `@Provides fun provideInterstitialAd(...): InterstitialAd`, or an `InterstitialAd` constructor parameter, makes the ArchUnit rule fail on Hilt's generated `DaggerApp_HiltComponents_SingletonC` — which lives in the root package, outside `..platform..`.
**Why it happens:** The rule reads bytecode, and the generated component carries a `Provider<T>` field per binding regardless of which module declared it.
**How to avoid:** `InterstitialAdSlot` constructs and owns its `InterstitialAd` internally and exposes none; `AdMobAdGateway` names no SDK type in any signature. Verified empirically in Phase 9 (plan 09-05) — moving the provider inside `..platform..` did **not** help.
**Warning signs:** A `PlatformBoundaryTest` failure naming `SingletonCImpl.…Provider`. Both `PlatformModule` and `AdsModule` carry long KDoc warnings about this; read them before touching DI.

### Pitfall 5: Results nav lambdas fire synchronously — there is nowhere to insert the ad
**What goes wrong:** The scrim renders but navigation has already happened, or the ad shows over the next screen.
**Why it happens:** `ResultsRoute` passes `onNextLevel`/`onCatalog` straight through, and `ResultsActions` invokes them inside `onClick` (wrapped in `withClickSound`). `ResultsViewModel` currently injects **no** `AdGateway` and owns no navigation timing at all.
**How to avoid:** Route the tap into the ViewModel, hold the intent, and emit a **one-shot** navigation effect the route consumes on ad dismiss (or immediately, on the D-10 fast path). Use the `SavedStateHandle` one-shot latch idiom the VM already uses twice (`RECORDED_KEY`, `CELEBRATED_KEY`) so a config change cannot double-navigate.
**Warning signs:** Any remaining direct `onNextLevel(challengeLevel + 1)` call inside a composable's `onClick` on the ad-eligible path.

### Pitfall 6: The host Activity is stopped during the ad, so lifecycle-aware collection is suspended
**What goes wrong:** A navigation effect emitted from `onAdDismissedFullScreenContent` is delivered while `collectAsStateWithLifecycle` is not collecting, and is either lost or re-delivered on resume (double navigation).
**Why it happens:** `ResultsRoute` uses `collectAsStateWithLifecycle`, which stops at `STOPPED`. The dismiss callback can land at the boundary of the host resuming.
**How to avoid:** Make the nav effect **state, not an event stream** — a `pendingNavigation: NavTarget?` field on `ResultsUiState`, consumed exactly once via a `LaunchedEffect(target)` that calls back into the VM to clear it. A `StateFlow` field survives a stopped collector; a `Channel`/`SharedFlow` emission may not.
**Warning signs:** Occasional double-advance of the Challenge ladder after an ad; a "Next level" that lands two levels forward.

### Pitfall 7: The click sound has already played by the time the scrim appears
**What goes wrong:** `withClickSound(SoundCue.ButtonForward, …)` fires on tap, then 500 ms of silent dim, then an ad — the cue reads as "the button didn't work".
**Why it happens:** `ResultsActions` wraps *every* action in `withClickSound` at the call site.
**How to avoid:** Accept it (the cue is confirmation of the tap, which is honest), or fold the scrim's fade-in so the visual change begins within the cue's decay. Do **not** move the cue into `Modifier.claySquish()` — the file's comment records that this modifier is shared with top-bar back buttons, the hint toggle, and Settings switches, which D-09 requires to stay silent.
**Warning signs:** A UAT note that the forward click "sounds early".

### Pitfall 8: AdMob policy — ≤1 interstitial per two user actions, never back-to-back
**What goes wrong:** Serving is limited or the account is flagged. `PITFALLS.md` §6 records the specific disallowed implementations: on app load, on app exit, more than one per two user actions (back presses count), and immediately after another interstitial.
**Why it happens:** The cadence is written as "every 3rd win" and nothing checks the interaction *between* an interstitial and the Phase 9 rewarded ad.
**How to avoid:** D-01 (3 wins) and D-02 (90 s) already exceed the policy floor. The uncovered case is **a rewarded ad immediately followed by an interstitial** — a player who watches a rewarded hint on the final move, then wins, then taps Next. `ARCHITECTURE.md`'s anti-pattern 6 explicitly recommends "never within X seconds of a rewarded ad". This is *not* in D-01..D-13. See Open Question 4.
**Warning signs:** An "ad serving limited" notice in AdMob; an implausibly high interstitial CTR.

### Pitfall 9: An interstitial is single-show and must be destroyed
**What goes wrong:** The same `InterstitialAd` instance is shown twice; the second `show()` silently does nothing or reports a failure, and the SDK-side resources leak.
**Why it happens:** The object still exists and looks reusable.
**How to avoid:** `Ad.destroy()` (verified present on the `Ad` supertype) on **both** terminal callbacks, then reload — the `recycleSlot` shape already in `AdMobAdGateway`.
**Warning signs:** An interstitial that shows once per process and then never again.

### Pitfall 10: `elapsedRealtime` vs wall-clock — mixing the two timebases
**What goes wrong:** The D-02 90-second gate is written against `SystemClock.elapsedRealtime()` (copying `RewardedAdSlot`'s age bound) and resets to "gate open" on every reboot — or worse, is persisted and compared against a post-reboot value that is smaller.
**Why it happens:** `RewardedAdSlot` deliberately uses `elapsedRealtime` and its KDoc explains why (timezone/NTP immunity) — the reasoning is persuasive and does not transfer.
**How to avoid:** **Persisted** cadence values (D-02 gap, D-04 session gap) must be wall-clock, because no monotonic clock survives process death or reboot. **Non-persisted** ad-age bounds stay `elapsedRealtime`. Guard against a backwards clock by treating a negative delta as "gate open" rather than as a lock-out.
**Warning signs:** `SystemClock` imported into the cadence policy — which, in `:domain`, would fail `DomainPurityTest` outright (a useful accident).

### Pitfall 11: Preloading while the cadence forbids an ad
**What goes wrong:** Ad requests are issued during first-session suppression, violating D-12 and — before consent resolves — creating the GDPR exposure T-09-44 names.
**Why it happens:** Preload is naturally written as "on every win".
**How to avoid:** Gate the preload on **both** consent readiness (`rewardedOfferable`-equivalent) and a cadence "would the next eligible tap pass?" query. `AdMobAdGateway.preloadRewarded`'s `if (rewardedOfferable.value) slot.preload()` is the exact precedent.
**Warning signs:** Network ad requests observable on a fresh install before level 6.

### Pitfall 12: The build gates fail late and cheaply-avoidably
**What goes wrong:** `./gradlew check` fails on `dpSpLiteralCheck` (a `dp` literal in the scrim), `adIdLiteralCheck` (an ad-unit string), `detekt` (`ReturnCount` — `GameViewModel` has an in-source comment about restructuring specifically to stay inside that limit), or `ktlintCheck`.
**How to avoid:** dimensions → `dimens.xml`; ad units → `AdIds`/`BuildConfig` only; keep new gate functions small. `check` depends on `detekt`, `ktlintCheck`, `dpSpLiteralCheck`, `adIdLiteralCheck`, `audioFocusLiteralCheck`. [VERIFIED: `build-logic/src/main/kotlin/standards.gradle.kts:177`]

---

## Code Examples

### Verified GMA Next-Gen 1.4.0 interstitial API surface

Decompiled from the resolved AAR — this is the authoritative signature list, not documentation recall.

```java
// javap com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
public interface InterstitialAd extends Ad {
  public static void load(AdRequest, AdLoadCallback<InterstitialAd>);
  public abstract void show(android.app.Activity);              // NO reward listener
  public abstract void setAdEventCallback(InterstitialAdEventCallback);
  public abstract InterstitialAdEventCallback getAdEventCallback();
  public abstract void setImmersiveMode(boolean);
}

// javap …interstitial.InterstitialAd$Companion   — the suspend entry point
public final class InterstitialAd$Companion {
  public final Object load(AdRequest, Continuation<? super AdLoadResult<? extends InterstitialAd>>);
  public final void load(AdRequest, AdLoadCallback<InterstitialAd>);
}

// javap …common.AdEventCallback   (InterstitialAdEventCallback extends this)
public interface AdEventCallback {
  public default void onAdShowedFullScreenContent();
  public default void onAdDismissedFullScreenContent();
  public default void onAdFailedToShowFullScreenContent(FullScreenContentError);
  public default void onAdImpression();
  public default void onAdClicked();
  public default void onAdPaid(AdValue);
}

// javap …common.Ad
public interface Ad {
  public abstract void destroy();
  public abstract String getAdUnitId();
  public abstract ResponseInfo getResponseInfo();
  …
}
```

**Three consequences for the plan.** (1) The suspend `Companion.load(AdRequest, Continuation): AdLoadResult<InterstitialAd>` is structurally identical to the `RewardedAd.load` call in `RewardedAdSlot.loadOnce()` — the slot clones with a one-word type change. (2) `show(Activity)` takes no listener, so `showForOutcome`'s `earned` flag and the whole `RewardItem` path simply vanish; the outcome is binary (dismissed / failed-to-show). (3) `onAdShowedFullScreenContent` exists and is the correct signal for lifting the scrim (Pattern 5).

### The interstitial slot — the delta from `RewardedAdSlot`

```kotlin
// Source: adapted from app/…/platform/ads/RewardedAdSlot.kt (in-repo, VERIFIED)
private suspend fun loadOnce(): InterstitialAd? {
    val outcome = runCatching {
        InterstitialAd.load(AdRequest.Builder(AdIds.interstitialAdUnitId).build())
    }
    currentCoroutineContext().ensureActive()   // runCatching must not swallow cancellation
    return when (val result = outcome.getOrNull()) {
        is AdLoadResult.Success -> result.ad
        is AdLoadResult.Failure -> null        // no fill is NOT an error path (D-11)
        null -> null
    }
}
```

Everything else — `@Singleton` on the class, `Dispatchers.Main` confinement, the shared in-flight `Deferred`, app-scope load survival, the age bound, `invalidate()` calling `destroy()` — transfers unchanged. `AdIds.interstitialAdUnitId` already exists from Phase 6. [VERIFIED: `platform/ads/AdIds.kt`]

### Evolving the `AdGateway` seam

The current `fun showInterstitial()` is fire-and-forget and cannot express D-09's navigate-on-dismiss. Recommended shape:

```kotlin
/** Whether an interstitial is loaded and showable right now (D-10, D-11). */
val interstitialReady: StateFlow<Boolean>

/** Warms one interstitial when the next eligible tap could show it (D-12). */
fun preloadInterstitial()

/**
 * Shows the held interstitial, suspending until it is dismissed or fails to show.
 * Returns `true` only if an ad was actually presented — the caller advances cadence
 * counters on `true` and navigates unconditionally either way (D-09, D-11).
 */
suspend fun showInterstitial(): Boolean
```

`suspend` mirrors `requestRewarded`, and a `Boolean` return keeps the seam SDK-free (Pitfall 4). **`NoOpAdGateway` must answer `interstitialReady = false` and `showInterstitial() = false`** — the opposite polarity from its `rewardedOfferable = true`, because the stub's contract is "the player is never worse off than v1.0", and v1.0 had no interstitials. Its class KDoc reasons through exactly this asymmetry for the rewarded case and must be extended, not contradicted.

---

## Runtime State Inventory

Not a rename phase, but this phase **introduces new persisted state into an app that already has installed users**, which raises the same class of question.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | New `app_prefs` Preferences keys (5, per D-13). **Existing v1.0/v1.1 installs read them as absent → defaults.** So an established player with `challenge_current_level = 200` starts with `lifetimeCompletedLevels = 0` and `firstSessionComplete = false`, and gets a fresh interstitial-free first session plus 5 free levels. | **Decide and document.** Fails in the player's favour and arguably *is* the desirable upgrade experience. Optional: seed `lifetimeCompletedLevels` from `ChallengeProgress.currentLevel` on first read. Recommend accepting the default and stating it in a KDoc rather than adding a migration. |
| Live service config | **AdMob interstitial ad unit must exist in the AdMob console** (ROADMAP External Prerequisites). `AdIds.interstitialAdUnitId` is already wired to `BuildConfig.INTERSTITIAL_AD_UNIT_ID`, resolved from an untracked `local.properties` / CI env var in release, Google test units in debug. | Verify the release ID resolves before a release build; `verifyReleaseAdIds` already fails fast if it does not. Debug work needs nothing. |
| OS-registered state | None — no scheduled jobs, no receivers, no `WorkManager` usage introduced. | None. |
| Secrets / env vars | `INTERSTITIAL_AD_UNIT_ID` — already established in Phase 6. No new key. | None. |
| Build artifacts | New `app_prefs` keys need no migration (Preferences DataStore is schemaless). If `lifecycle-process` is added, `libs.versions.toml` + `app/build.gradle.kts` change. | None beyond a normal rebuild. |

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `play-services-ads` `InterstitialAd.load(context, adUnitId, adRequest, callback)` | GMA Next-Gen `InterstitialAd.load(AdRequest, …)` — no `Context` parameter; a **suspend** overload returning `AdLoadResult` | Legacy line deprecated 2026-02-17; Next-Gen preferred since 2026-07-06 | Any StackOverflow/blog snippet passing a `Context` to `load` is legacy and will not compile. Mirror the in-repo `RewardedAdSlot` instead. |
| `FullScreenContentCallback` (abstract class) | `AdEventCallback` (interface, all methods `default`) | GMA Next-Gen | Implement as a Kotlin `object : InterstitialAdEventCallback { … }`, overriding only what is needed. [VERIFIED: AAR bytecode] |
| `AdReason.RETRY` gating retry behind an ad | Retry is free; `AdReason.RETRY` dropped | v1.1 requirements | `AdGateway.showInterstitial`'s original KDoc and `ARCHITECTURE.md`'s phase-4 line both still mention `AdReason.RETRY` — **both are stale**. `AdReason` today has only `HINT` and `UNDO`. Do not reintroduce it. |
| Cadence "in the repository layer" (`PITFALLS.md` §6) | Cadence *decision* pure in `:domain`; cadence *state* in the data layer | ROADMAP Phase 10 note | The two documents agree once split: persistence is repository-layer, the decision is pure. |

**Deprecated / outdated in the project's own docs:**
- `AdGateway.showInterstitial()` KDoc says "A no-op in Slice 1; the real implementation is introduced with the binding swap in Slice 2" — the binding swap already happened in Phase 9; only this method remains a no-op.
- `ARCHITECTURE.md` line 112 sketches `shouldShow(nowMs, levelsSinceLast, lastShownMs, sessionLevelCount)`. That signature predates D-03 (first-session/first-5 suppression) and D-04 (session cap semantics); it is a useful shape, not a specification.

---

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| `ads-mobile-sdk` AAR (with `InterstitialAd`) | The whole phase | ✓ | 1.4.0, in local Gradle cache | — |
| `androidx.lifecycle:lifecycle-process` | D-04 session boundary | ✓ | 2.9.4 on Google Maven (HTTP 200) + in local cache | "Last win" timestamp — no new artifact (Open Question 3) |
| `datastore-preferences` | D-13 persistence | ✓ | 1.2.1, in use | — |
| JVM test stack (JUnit4, Truth, Turbine, MockK, coroutines-test, Robolectric) | Cadence + VM tests | ✓ | 4.13.2 / 1.4.4 / 1.2.1 / 1.14.2 / 1.11.0 / 4.16 | — |
| ArchUnit gates (`DomainPurityTest`, `PlatformBoundaryTest`) | Purity enforcement | ✓ | in `app/src/test` | — |
| AdMob **interstitial ad unit** (production ID) | Release builds only | ✗ *(unverified)* | — | Google test unit `ca-app-pub-3940256099942544/1033173712` in debug; `verifyReleaseAdIds` fails the release build fast if the real ID is absent |

**Missing with no fallback:** none blocking development.
**Missing with fallback:** the production interstitial ad unit — debug work is fully unblocked by the test unit, and the release gate already exists. This remains a ROADMAP External Prerequisite and a **human/console task, not an engineering task**.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit4 4.13.2 + Truth 1.4.4 + Turbine 1.2.1 + MockK 1.14.2 + kotlinx-coroutines-test 1.11.0; Robolectric 4.16 for JVM-side Android tests |
| Config file | `gradle/libs.versions.toml` (versions), `app/build.gradle.kts`, `domain/build.gradle.kts` |
| Quick run command | `./gradlew :domain:test` (pure cadence logic — seconds, no Android) |
| Full suite command | `./gradlew check` (includes `detekt`, `ktlintCheck`, `dpSpLiteralCheck`, `adIdLiteralCheck`, `audioFocusLiteralCheck`) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|--------------|
| INT-02 | Cadence truth table: D-01 × D-02 × D-03 × D-04, each gate independently proven necessary | unit (pure) | `./gradlew :domain:test --tests '*InterstitialCadenceStateTest'` | ❌ Wave 0 |
| INT-02 | `recordShown` zeroes wins, stamps time, increments session; `recordWin` advances both counters in both modes (D-07) | unit (pure) | same | ❌ Wave 0 |
| INT-02 | `NEVER_SHOWN` passes the D-02 gate; a backwards clock does not lock the player out | unit (pure) | same | ❌ Wave 0 |
| INT-02 | State survives process death; a corrupt store reads back as defaults (⇒ no ad) | unit (Robolectric + test DataStore) | `./gradlew :app:testDebugUnitTest --tests '*InterstitialCadenceStoreTest'` | ❌ Wave 0 — model on `ChallengeProgressTest` |
| INT-03 | A 2-star Classic finish and a stuck Challenge finish produce **no** interstitial; a Diamond-41 two-peg (3-star) finish **does** qualify | unit (VM) | `./gradlew :app:testDebugUnitTest --tests '*ResultsViewModelTest'` | ⚠️ file exists, cases ❌ Wave 0 |
| INT-01 / INT-03 | Only "Next level" and Classic-"Catalog"-after-a-win are ad-eligible; Replay / Home / Retry never are (D-06) | unit (VM) | same | ⚠️ file exists, cases ❌ Wave 0 |
| INT-01 | No ad request or show occurs on Results composition — only on an eligible tap | unit (VM, MockK verify) | same | ⚠️ file exists, cases ❌ Wave 0 |
| INT-04 | Scrim renders on the ad path, swallows taps, and is absent on the D-10 fast path | unit (Compose UI test / Robolectric) | `./gradlew :app:testDebugUnitTest --tests '*ResultsScreenTest'` | ⚠️ file exists, cases ❌ Wave 0 |
| INT-04 | Scrim lifts on `onAdShowedFullScreenContent`, not on a fixed timer | unit (VM, fake gateway) | `*ResultsViewModelTest` | ❌ Wave 0 |
| ADSF-01 | Elapsed seconds in == elapsed seconds out; no clock start before dismiss | unit (VM invariant) | `*ResultsViewModelTest` | ❌ Wave 0 |
| D-09 | Navigation fires exactly once — on dismiss, on show-failure, and on the no-ad fast path; never twice | unit (VM) | `*ResultsViewModelTest` | ❌ Wave 0 |
| D-12 | No preload while suppression / cap / gates make an ad impossible | unit (VM + fake gateway, MockK verify) | `*ResultsViewModelTest` | ❌ Wave 0 |
| Purity | The cadence policy imports no Android type | arch | `./gradlew :app:testDebugUnitTest --tests '*DomainPurityTest'` | ✅ exists — extend fixture |
| Boundary | No GMA type escapes `..platform..` (incl. the generated Hilt component) | arch | `./gradlew :app:testDebugUnitTest --tests '*PlatformBoundaryTest'` | ✅ exists |
| Stub parity | `NoOpAdGateway` never shows an interstitial | unit | `./gradlew :app:testDebugUnitTest --tests '*NoOpAdGatewayTest'` | ✅ exists — extend |

**Manual-only (device UAT), justified:** actual ad rendering and dismissal via the Google test unit; the perceptual quality of the 500 ms dim; and the "does this feel resented?" judgement the ROADMAP's governing sentence asks for. None of these are automatable, and all three want a physical device (the phase already inherits a *"Phase 7's audit was emulator-only"* caution in STATE.md).

### Sampling Rate
- **Per task commit:** `./gradlew :domain:test` (pure cadence) — sub-second feedback on the phase's most logic-dense artifact.
- **Per wave merge:** `./gradlew check`.
- **Phase gate:** full `check` green + a device UAT run against the Google test interstitial unit before `/gsd:verify-work`.

### Wave 0 Gaps
- [ ] `domain/src/test/.../ads/InterstitialCadenceStateTest.kt` — covers INT-02 (the truth table; the ROADMAP-mandated TDD-before-wiring artifact)
- [ ] `app/src/test/.../data/InterstitialCadenceStoreTest.kt` — covers INT-02 persistence; model on the existing `ChallengeProgressTest`
- [ ] Extend `app/src/test/.../ui/results/ResultsViewModelTest.kt` — covers INT-01, INT-03, INT-04 sequencing, D-09, D-12, ADSF-01
- [ ] Extend `app/src/test/.../ui/results/ResultsScreenTest.kt` — covers INT-04 rendering + input swallowing
- [ ] Extend `app/src/test/.../platform/ads/NoOpAdGatewayTest.kt` — stub parity for the new seam members
- [ ] A `FakeAdGateway` test double supporting ready/not-ready, dismissed/failed-to-show — no such fake exists yet
- [ ] Framework install: **none needed** — every library is already declared

---

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | no | No auth surface in this phase (Play Games is Phases 11–13). |
| V3 Session Management | no | "Session" here is an analytics window, not a security session. No tokens, no server. |
| V4 Access Control | no | No privileged operation is gated. |
| V5 Input Validation | **yes** | Persisted cadence counters are attacker-writable on a rooted device. Apply the `AssistAllowance.fromStoredOrFull` discipline: validate on read, **fail toward fewer ads**, never crash. A forged `lifetimeCompletedLevels = MAX_VALUE` must not overflow when incremented; a forged `lastInterstitialAtMs` in the future must not lock the gate permanently. |
| V6 Cryptography | no | Nothing secret is stored. Cadence state is not security-relevant — the only "attack" is a player suppressing their own ads, which costs revenue, not safety. Encrypting it would be theatre. |
| V7 Logging | **yes** | **Nothing in the ad path may log.** `AdMobAdGateway` and `UmpConsentGateway` both carry explicit KDoc stating this — ad response info, consent status and the TCF string are personal-data-adjacent. New interstitial code inherits the prohibition. |
| V8 Data Protection | **yes** | Consent (CONS-02 / Phase 9 D-20) must gate the interstitial load exactly as it gates the rewarded load. Requesting an interstitial before consent resolves is the GDPR exposure T-09-44 names. |

### Known Threat Patterns for this stack

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Local tampering of cadence counters to suppress ads | Tampering | Validate-on-read, fail toward *fewer* ads; accept as low-impact (self-harm to revenue only). Do not add integrity checks — cost exceeds value. |
| Accidental interstitial clicks counted as invalid traffic → account suspension | Repudiation / business | INT-04's scrim is the control; 500 ms exceeds double-tap intervals. `PITFALLS.md` names accidental clicks as invalid traffic explicitly. |
| Ad request issued before consent resolves | Information disclosure | Gate `preloadInterstitial()` on consent readiness, mirroring `preloadRewarded`. |
| Diagnostic logging of ad/consent identifiers | Information disclosure | No logging call may name them (ASVS V7, in-repo convention). |
| Integer overflow on a long-lived lifetime counter | Tampering / DoS | Saturating increment, or stop counting once past the D-03 threshold (the counter's only use is `>= 5` — it need not grow unboundedly). |
| Ad presented over a live game board | — (UX/policy) | D-09 ad-first-then-navigate; the ad can only ever sit over Results. |

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `ProcessLifecycleOwner` does **not** dispatch `ON_STOP` when a same-process fullscreen ad Activity is displayed | Pattern 4, Pitfall 3 | The session counter resets on each ad show and D-04's cap never binds. **Cheap to verify on device with a log-free counter assertion during UAT — recommend an explicit UAT step.** |
| A2 | Interstitial ads expire in roughly one hour after load (carried from `AdConstants.REWARDED_AD_MAX_AGE_MS`'s own `[ASSUMED]` note — the figure is legacy-SDK documentation, not restated for Next-Gen) | AdConstants extension | Only affects how often a stale ad is discarded pre-emptively. A stale ad that slips through fails at `show()` → treated as no-fill → navigate immediately (D-11). Correctness does not depend on the number. |
| A3 | Every Challenge footprint has `minimumPegs == 1` (which is why `ResultsUiState.showNextLevel`'s hardcoded `1` is currently correct) | Pattern 2, Pitfall 1 | If a future generated footprint had a higher floor, `showNextLevel` would already be broken independently of this phase. The recommended `stars == THREE_STARS` gate is immune either way. |
| A4 | AdMob's "≤1 interstitial per two user actions" is satisfied by D-01+D-02 | Pitfall 8 | Policy text is sourced from `PITFALLS.md`'s citation of `support.google.com/admob/answer/6201362`, not re-fetched this session. D-01/D-02 are far stricter than the floor, so the margin is large. |
| A5 | The `Results` destination is reached only via a terminal game state (so `stars` always reflects a real finish) | Pattern 2 | `ResultsViewModel`'s WARN-03 comment notes `Results` is independently reachable as a typed route and clamps `challengeLevel` for exactly that reason. A forged `stars = 3` arg could force ad eligibility — harmless (the player would be *choosing* to see an ad) but worth a one-line note. |
| A6 | 500 ms exceeds typical double-tap intervals (D-08's stated rationale) | D-08 | User-locked decision; carried as given. Android's `ViewConfiguration` double-tap timeout is 300 ms, which supports it. |

---

## Open Questions (RESOLVED)

> All five questions were resolved during plan-phase on 2026-09-02: Q1 → CONTEXT **D-14** (user chose the *wider* reading: any solved finish, 2+ stars, is eligible — contra the recommendation below), Q2 → accepted as-is, documented in KDoc (plan 10-01/10-02), Q3 → CONTEXT **D-16** (`ProcessLifecycleOwner` via `lifecycle-process`), Q4 → CONTEXT **D-15** (shared "last fullscreen ad at" timestamp), Q5 → accepted as-is, documented in KDoc (plan 10-02).

1. **Is a 2-star Classic finish a "win" for interstitial purposes?** — *RESOLVED: D-14 — any solve (2+ stars) counts; losses/abandons/restarts still excluded (INT-03 unchanged).*
   - *What we know:* `GameStatus.Won ⟺ stars == 3` is exact and machine-checkable. `tierFor` labels a 2-star Classic result `SOLVED` → the copy reads "Solved!".
   - *What's unclear:* D-06 says "Classic 'Catalog' after a win" without defining "win", and the screen's own copy and the proposed gate would disagree in wording for 2-star Classic finishes.
   - *Recommendation:* Use `stars == THREE_STARS`. It is the exact restatement of the domain's own win test, it is the conservative direction (fewer ads), and it is consistent with INT-03's "never after a lost level" — a board that did not reach its perfect finish was not solved. **Flag for user confirmation during planning**, since it materially affects how often Classic players see ads.

2. **The in-memory session count creates a kill-window cap evasion (D-13).** — *RESOLVED: accepted as-is; reasoning documented in KDoc per plans 10-01/10-02.*
   - *What we know:* D-13 puts `interstitialsThisSession` in memory, reasoning that "a kill → relaunch after 30 min is a new session anyway".
   - *What's unclear:* A kill → relaunch after **2 minutes** is the *same* session by D-04's own definition, but the counter reads 0. A player who force-quits can reset the cap.
   - *Recommendation:* Accept it. D-01 (3 wins) and D-02 (90 s) still bind, so the worst case is one extra ad per force-quit, and force-quitting to see *more* ads is not a threat model. **Document the reasoning in KDoc.** Persisting the count would close it but contradicts a locked decision — do not change unilaterally.

3. **`lifecycle-process` dependency vs. a dependency-free "last win" timestamp for the session boundary.** — *RESOLVED: D-16 — `ProcessLifecycleOwner` via `androidx.lifecycle:lifecycle-process` at the pinned atomic-group version.*
   - *What we know:* Both work. `ProcessLifecycleOwner` matches D-04's literal wording ("last-foreground timestamp"); "last win" needs no new artifact and is 100% pure-Kotlin testable.
   - *What's unclear:* Whether the user's intent in D-04 was the mechanism or the behaviour.
   - *Recommendation:* Use `ProcessLifecycleOwner` — it matches the decision's wording, the artifact is a first-party sibling already in the pinned atomic group, and the "last win" proxy would mis-handle a player who opens the app, plays nothing for an hour, and returns. Mention the alternative in the plan so the cost is visible.

4. **Interstitial immediately following a rewarded ad — an uncovered adjacency.** — *RESOLVED: D-15 — D-02's timestamp widened to shared "last fullscreen ad at"; rewarded ads also reset the 90s cooldown.*
   - *What we know:* `ARCHITECTURE.md`'s anti-pattern 6 recommends "never within X seconds of a rewarded ad", and AdMob policy prohibits an interstitial immediately after another fullscreen ad. D-01..D-13 contain **no** rewarded-interstitial interlock.
   - *What's unclear:* Whether the reachable case (rewarded hint on the last move → win → tap Next) is frequent enough to matter, and whether the user considers this in scope.
   - *Recommendation:* Add the D-02 90-second gate as a **shared** "last fullscreen ad at" timestamp rather than a "last interstitial at" timestamp. This is a one-word change to the state field's meaning, closes the policy gap for free, and stays inside the locked 90-second value. **Surface to the user — it is a genuine extension of D-02's scope, not a pure implementation detail.**

5. **Should `lifetimeCompletedLevels` be seeded for existing installs?** — *RESOLVED: accepted as-is (fresh grace period for upgrading players is a feature); recorded in KDoc per plan 10-02.*
   - *What we know:* Existing players will get a fresh interstitial-free first session plus 5 free levels (§ Runtime State Inventory).
   - *Recommendation:* Do nothing — a gentle re-introduction for upgrading players is a feature, not a bug. Record the decision in KDoc so it does not read as an oversight later.

---

## Project Constraints (from CLAUDE.md)

| Directive | Effect on this phase |
|-----------|---------------------|
| No magic numbers/strings; constants split **by scope** | D-01..D-04 values go in an `InterstitialCadenceParams` object beside `AssistAllowanceLimits` / `ScoreParams` — **not** into `AdConstants`, which owns ad *timing* only and whose KDoc explicitly limits its remit. |
| Compose state hoisting | Scrim visibility and pending navigation are `ResultsUiState` fields; `ResultsScreen` stays stateless. |
| `dimens.xml` for all UI dimensions | Scrim dimensions/tokens go through `dimens.xml`; `dpSpLiteralCheck` fails the build otherwise. |
| detekt + ktlint as a build gate | `./gradlew check` must be green. Watch `ReturnCount` on the new gate functions. |
| Clean Architecture / MVVM, `StateFlow` UiState, UDF | Policy (pure) → repository (state) → ViewModel (decision + sequencing) → stateless screen. |
| Game logic isolation — pure Kotlin, no Android deps | `DomainPurityTest` enforces it if the policy lives in `:domain` (the recommended placement). |
| Ads behind the `AdGateway` interface | The seam evolves; no GMA type may cross it (Pitfall 4). |
| Gitflow strictly | Work on a `feature/*` branch off `develop`. Current branch is `feature/phase-09-consent-rewarded-ads` — **branch anew for Phase 10.** |
| Gradle version catalog mandatory | Any `lifecycle-process` addition is declared in `libs.versions.toml` with `version.ref = "androidxLifecycle"`. |

**Project skills:** No `.claude/skills/`, `.agents/skills/`, `.cursor/skills/`, `.github/skills/` or `.codex/skills/` directory with a `SKILL.md` index was found. CLAUDE.md references a "locally installed `compose-skill`" for state hoisting; treat its guidance as the state-hoisting rule already stated above.

---

## Sources

### Primary (HIGH confidence)
- **`ads-mobile-sdk-1.4.0.aar` bytecode**, decompiled with `javap` from the resolved Gradle cache artifact — `InterstitialAd`, `InterstitialAd$Companion`, `InterstitialAdEventCallback`, `AdEventCallback`, `Ad`. The authoritative API surface for this phase.
- **In-repo source, read in full:** `platform/ads/AdGateway.kt`, `AdMobAdGateway.kt`, `RewardedAdSlot.kt`, `AdConstants.kt`, `AdIds.kt`, `NoOpAdGateway.kt`, `di/AdsModule.kt`, `di/PlatformModule.kt`, `platform/activity/DefaultForegroundActivityProvider.kt`, `data/ChallengeProgress.kt`, `data/AppPreferences.kt`, `ui/results/*.kt`, `ui/game/GameViewModel.kt` (status + clock regions), `domain/assist/AssistAllowance.kt`, `domain/scoring/ScoreParams.kt` + `ScoreCalculator.kt`, `navigation/Routes.kt`, `build-logic/src/main/kotlin/standards.gradle.kts`, `gradle/libs.versions.toml`.
- **Google Maven** — `HTTP 200` on `lifecycle-process-2.9.4.aar`; local Gradle cache listing confirms the artifact directory.
- **Project planning documents** — `.planning/REQUIREMENTS.md` (INT-01..04, ADSF-01, Milestone Decisions), `.planning/ROADMAP.md`, `.planning/STATE.md`, `10-CONTEXT.md`, `09-CONTEXT.md`, `06-CONTEXT.md`.
- **Milestone research** — `.planning/research/ARCHITECTURE.md` (anti-pattern 6, `InterstitialCadencePolicy` sketch), `PITFALLS.md` (§6 interstitial cadence, §disallowed implementations), `STACK.md` (SDK pin, test ad unit IDs).

### Secondary (MEDIUM confidence)
- `developer.android.com/topic/libraries/architecture/lifecycle` — `ProcessLifecycleOwner` semantics and same-process debouncing (behaviour under a fullscreen ad Activity is inferred, → A1).
- `developers.google.com/admob/android/next-gen/interstitial` — cited transitively via `STACK.md`; not re-fetched this session, but every API claim above is independently verified from bytecode.

### Tertiary (LOW confidence)
- `support.google.com/admob/answer/6201362` (disallowed interstitial implementations) — quoted via `PITFALLS.md` rather than re-fetched (→ A4). The locked cadence exceeds the policy floor by a wide margin, so the risk is low.

---

## Metadata

**Confidence breakdown:**
- Standard stack: **HIGH** — no new third-party dependency; the one candidate artifact is a first-party sibling in an already-pinned atomic group, verified on Google Maven and in the local cache.
- SDK API: **HIGH** — read from the resolved AAR's bytecode, not from documentation or recall.
- Architecture / integration points: **HIGH** — every file this phase touches was read in full; the shipped Phase 9 code is a directly transferable template with one documented delta.
- The INT-03 win test: **HIGH** — the `<=`-against-`minimumPegs` equivalence between `GameStatus.Won` and `ScoreCalculator.stars` is asserted in-source by `GameViewModel`'s own KDoc as a deliberate invariant.
- Pitfalls: **HIGH** for the seven derived from in-repo code and shipped Phase 9 findings; **MEDIUM** for the two resting on AdMob policy text quoted at one remove (→ A4).
- Session-boundary mechanism: **MEDIUM** — the recommendation is sound and standard, but A1 (`ProcessLifecycleOwner` behaviour during a fullscreen ad) deserves an explicit device-UAT confirmation rather than being assumed.

**Research date:** 2026-09-02
**Valid until:** 2026-10-02 (30 days). The SDK pin is stable and the findings are overwhelmingly in-repo; only the AdMob *policy* citations are subject to outside change.
