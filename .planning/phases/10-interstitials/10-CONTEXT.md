# Phase 10: Interstitials - Context

**Gathered:** 2026-09-02
**Status:** Ready for planning

<domain>
## Phase Boundary

Between-level interstitials a player barely notices and never resents. Requirements: INT-01..04, plus the carried ADSF-01 acceptance criterion (clock stays paused for the whole presentation — the freeze mechanism was built in Phase 9; this phase re-asserts it for interstitials).

The phase delivers: a pure-Kotlin cadence policy in `:domain` (TDD'd as logic before any wiring, per ROADMAP note), the real `showInterstitial` path in `AdMobAdGateway` (currently a deliberate no-op), the trigger wiring at the Results continue actions, the INT-04 quiescence pause, and DataStore persistence so the cap survives process death.

**Locked upstream (do not re-litigate):**
- Trigger is the Results continue tap only — never during win celebration, never at level start (INT-01).
- Losses, abandons, and restarts never see an interstitial (INT-03). Retry stays free — `AdReason.RETRY` was dropped at requirements time.
- Rewarded ads, consent, allowance economy are Phase 9 (complete). Play Games is Phases 11–13.

</domain>

<decisions>
## Implementation Decisions

### Cadence policy values (INT-02)
- **D-01:** Level gate: an interstitial is eligible only after **every 3rd win** (3 wins since the last interstitial shown).
- **D-02:** Wall-clock gate: **minimum 90 seconds** since the last interstitial. Both D-01 AND D-02 must hold — protects against speed-cleared easy levels triggering back-to-back ads.
- **D-03:** New-install suppression: **the entire first session AND the first 5 lifetime completed levels** are interstitial-free — suppression ends only when both have passed (whichever ends later).
- **D-04:** Session cap: **max 4 interstitials per session**. A session ends after the app has been **backgrounded for 30 continuous minutes** (standard analytics convention); the boundary is computed from a persisted last-foreground timestamp so it survives process death.
- **D-05:** All cadence values are named, scoped constants (no magic numbers, per CLAUDE.md).

### Trigger surface (INT-01, INT-03)
- **D-06:** Ad-eligible actions are **forward-moving win actions only**: Challenge "Next level" and Classic "Catalog" after a win. Excluded: Classic "Replay" (a retry — retry stays free), "Home" (an exit), and Challenge-stuck "Retry" (a loss, INT-03).
- **D-07:** **One shared cadence counter across Classic and Challenge** — the player experiences one app; mode-switching cannot dodge the cap. Single policy object, single state.

### Quiescence pause (INT-04)
- **D-08:** On an ad-eligible tap with an ad ready: Results **dims with a scrim and all input is swallowed for 500 ms**, then the ad presents. The dim is the "something is coming" signal — no text, no spinner. 500 ms exceeds typical double-tap intervals, so a stray second tap lands on the scrim, never the ad.
- **D-09:** **Ad first, then navigate**: the ad presents over the (dimmed) Results screen; navigation to the next level / catalog proceeds on ad dismiss. If the ad fails to show, navigation proceeds immediately. The tap's intent is honored exactly once either way, and a new board never sits underneath an ad (spirit of INT-01's "never at level start").
- **D-10:** The pause/dim runs **only when an ad will actually show**. When cadence says no, or no ad is loaded, the tap navigates instantly with zero added latency — the pause is part of ad presentation, not of navigation.

### No-fill & edge behavior
- **D-11:** No-fill at the trigger moment: **skip silently; counters keep running** (not reset). The very next eligible win can show the ad once one loads. Cadence counters/timestamps update only when an ad actually shows.
- **D-12:** Preload timing: **at eligibility, on win** — the load request fires the moment a win makes the next continue-tap ad-eligible. Mirrors Phase 9 D-09's "earliest point an offer can occur": no ad requests while first-session/first-5 suppression, the session cap, or the level/time gates make an ad impossible.
- **D-13:** Persistence split (INT-02 "survives being killed"): **DataStore-persisted** — wins-since-last-interstitial, last-interstitial wall-clock timestamp, lifetime completed-level count (for first-5 suppression), first-session-complete flag, last-foreground timestamp (for the 30-min session boundary). **In-memory** — the per-session interstitial count (it derives from the session boundary; a kill → relaunch after 30 min is a new session anyway).

### Planning-time decisions (resolved with user during plan-phase, 2026-09-02)
- **D-14:** Interstitial-eligible "win" is **any solved finish (2 or 3 stars)** — not only the strict `GameStatus.Won` / 3-star condition. Losses, abandons, and restarts remain excluded (INT-03 unchanged). Caution: this needs its own eligibility predicate — do not reuse `stars == THREE_STARS` (that is the strict-win test) and do not copy `ResultsUiState`'s `PEGS_REMAINING_SOLVED = 1` into a Classic gate (breaks Diamond-41, whose proven floor is 2 pegs).
- **D-15:** D-02's wall-clock gate is **widened to "last fullscreen ad shown at"** — a rewarded ad also resets the 90-second cooldown, so a rewarded ad can never be immediately followed by an interstitial (closes the back-to-back fullscreen-ad adjacency flagged by AdMob policy and ARCHITECTURE.md). The 90s value itself is unchanged.
- **D-16:** The D-04 session boundary source is **`ProcessLifecycleOwner`** via `androidx.lifecycle:lifecycle-process`, added at the pinned atomic-group version (`version.ref = "androidxLifecycle"`, 2.9.4 — 2.11.0 requires an uninstallable compileSdk 37). Do **not** derive it from `DefaultForegroundActivityProvider`: showing a fullscreen ad pauses the host Activity, which would forge a "backgrounded" event on every interstitial and prevent the session cap from ever binding.

### Claude's Discretion
- Exact DataStore keys/schema for the persisted cadence state (Preferences DataStore per the stack table — scalar values, no queries).
- Show-failure mid-presentation handling (map to silent skip + immediate navigation, consistent with D-11's fail-open direction) and reload cadence after a failed load.
- Scrim styling within the Candy Clay theme (color/alpha/animation of the dim).
- Where the policy object boundary sits vs. the gateway (policy decides *whether*, gateway handles *how* — model on the Phase 9 `AssistAllowance` / `AdMobAdGateway` split).
- Whether "eligibility close" (D-12) warrants preloading one win early (e.g. at 2-of-3 wins) if the ad-ready-by-tap rate needs it; keep it simple first.
- How the win counter increments relative to eligible-action taps (recommended simple reading: every win increments; the ad fires on the next eligible tap where all gates pass).
- Debug/test affordances for forcing or fast-forwarding the cadence (if needed for device verification).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & scope
- `.planning/REQUIREMENTS.md` — INT-01..04, ADSF-01, and the Milestone Decisions table (GMA Next-Gen `ads-mobile-sdk` 1.4.0; retry stays free).
- `.planning/ROADMAP.md` — Phase 10 goal, success criteria (including the carried ADSF-01 criterion), and the Notes line mandating the cadence policy be pure Kotlin and TDD'd before live wiring; External Prerequisites table (AdMob interstitial ad unit must exist).
- `.planning/PROJECT.md` — constraints, Key Decisions.

### Prior phase decisions that bind this phase
- `.planning/phases/09-consent-rewarded-ads/09-CONTEXT.md` — D-12 (knowably-unservable fail-open), D-16 (consent resolves at launch, so this phase may assume consent state is settled), D-20 (`canRequestAds()` is the single serving gate, owned by `AdMobAdGateway.rewardedOfferable`), and the ADSF-01 clock-freeze ownership.
- `.planning/phases/06-platform-foundation-build-hardening/06-CONTEXT.md` — D-04 (policy objects live in pure `:domain`), D-05/D-08/D-09 (test ad IDs in debug, real IDs untracked, release fail-fast), PLAT-01 Activity provider.

### Milestone research
- `.planning/research/ARCHITECTURE.md` — `ForegroundActivityProvider` pattern; ads integration points.
- `.planning/research/PITFALLS.md` — ads pitfalls (interstitial-specific entries).
- `.planning/research/STACK.md` — verified GMA Next-Gen dependency graph.

### Existing code this phase modifies or consumes
- `app/src/main/java/com/app/pegsolitaire/platform/ads/AdGateway.kt` — `showInterstitial()` is already declared on the seam; its KDoc anticipates this phase. The signature may need evolving (e.g. show-with-callback or suspend) to support D-09's navigate-on-dismiss — that is planner's call.
- `app/src/main/java/com/app/pegsolitaire/platform/ads/AdMobAdGateway.kt` — line ~134: the deliberate interstitial no-op this phase replaces; rewarded load/show/readiness patterns to mirror.
- `app/src/main/java/com/app/pegsolitaire/platform/ads/AdIds.kt` — `interstitialAdUnitId` build plumbing already exists (Phase 6).
- `app/src/main/java/com/app/pegsolitaire/platform/ads/NoOpAdGateway.kt` — must stay a no-op for interstitials (stub parity).
- `app/src/main/java/com/app/pegsolitaire/ui/results/ResultsActions.kt`, `ResultsViewModel.kt`, `ResultsUiState.kt`, `ResultsRoute.kt` — the trigger surface: "Next level" / "Catalog" tap paths where D-06 gating and the D-08 scrim land.
- `app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt` / `platform/ads/di/` — DI wiring for the cadence policy's persistence.
- `app/src/main/java/com/app/pegsolitaire/data/ChallengeProgress.kt` and `SettingsViewModel.kt` DataStore usage — established Preferences DataStore patterns to follow for D-13.
- `domain/` module (`scoring/`, `assist/`) — `ScoreCalculator` / `AssistAllowance`-style pure policy objects the cadence policy is modeled on.

### Project standards
- `CLAUDE.md` — no magic numbers (all D-01..D-04 values become scoped constants), dimens file for the scrim/UI, state hoisting, detekt/ktlint gates, Gitflow.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`AdGateway.showInterstitial()` seam + `AdMobAdGateway`** — the interface hook and the real gateway (with load/show/full-screen-callback patterns from the rewarded path) already exist; this phase fills the no-op.
- **`AdIds.interstitialAdUnitId`** — build-config plumbing (test IDs in debug, real in release) shipped in Phase 6.
- **ADSF-01 clock freeze** — the ticker pause across ad presentation shipped in Phase 9; interstitials fire from Results (no running clock), but the acceptance criterion must be re-verified for the resumed-level path.
- **Preferences DataStore patterns** — `ChallengeProgress` / Settings show the established read/write/fail-open conventions for the persisted cadence state.
- **Pure `:domain` policy-object pattern** — `ScoreCalculator` / `AssistAllowance` model the cadence policy: constructor-injected clock/state, JVM-testable, zero Android deps.

### Established Patterns
- **Purity gates are CI-enforced** — `gms` imports compile only inside `platform/`; the cadence policy is pure Kotlin.
- **Fail-open convention** — everything degrades toward "the player keeps playing": no ad → instant navigation, never a blocked tap.
- **State hoisting / UDF** — scrim visibility belongs in `ResultsUiState` (or an ads-facing slice), rendered statelessly.
- **One-accent Results discipline (UI-SPEC S3)** — the scrim must not fight the single CtaGradient accent; it dims the whole screen uniformly.

### Integration Points
- `ResultsViewModel` → cadence policy → gateway: the eligible tap asks the policy (whether), then the gateway (how), then navigates on dismiss/failure.
- `AdMobAdGateway` — interstitial load on the preload signal (D-12), show over the `ForegroundActivityProvider` Activity, full-screen callbacks driving navigate-on-dismiss.
- DataStore — new cadence-state preferences alongside existing stores.
- App foreground/background lifecycle — last-foreground timestamp for the 30-min session boundary (D-04, D-13).

</code_context>

<specifics>
## Specific Ideas

- The governing sentence from ROADMAP: *"a player barely notices and never resents"* — every cadence number above was chosen against the "resents" half.
- The dim-scrim IS the INT-04 mechanism: a stray tap lands on the scrim, never the ad — no text needed, the visual change alone communicates intent.
- Phase 9's v1.0-parity principle extends here: a player with no consent, no fill, or no SDK never sees a pause, a scrim, or a delay — interstitials degrade to absence, never to friction.

</specifics>

<deferred>
## Deferred Ideas

### Reviewed Todos (not folded)
- **Decide whether Stuck finishes should reach Results (1–2 star bands unreachable)** — reviewed again (it touches the Results trigger surface) and deliberately kept separate, consistent with Phase 9's call. Phase 10 gates interstitials on "won" (D-06), which is robust regardless of how the Stuck→Results decision lands later. Planner should avoid structural Results rework that would prejudge it.
- **Confirm audio cues on physical hardware** — Phase 7 follow-up, unrelated to ads; stays a pending todo.
- **Upgrade Wiegleb-45 opening book to depth 4** — Phase 8 follow-up compute task; stays a pending todo.

</deferred>

---

*Phase: 10-Interstitials*
*Context gathered: 2026-09-02*
