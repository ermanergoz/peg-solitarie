# Phase 9: Consent & Rewarded Ads - Context

**Gathered:** 2026-08-30
**Status:** Ready for planning

<domain>
## Phase Boundary

Player can consent to ads (UMP) and *choose* to watch a rewarded ad for extra assists once a free per-level allowance is spent — never surprised, never dead-ended, never charged time. Requirements: CONS-01, CONS-02, CONS-03, CONS-04, ADS-01..06, ADSF-01, REL-05.

The phase turns the auto-grant `NoOpAdGateway` behavior into a real economy: a `:domain` allowance policy, an opt-in dialog, the GMA Next-Gen rewarded-ad path behind the existing `RewardOutcome` seam, the UMP consent flow gating SDK init, and two privacy entries in Settings. Interstitials are Phase 10 (though ADSF-01 clock-freeze is owned here and re-asserted there). Play Games is Phases 11–13.

**Internal ordering is locked by ROADMAP:** the opt-in dialog and allowance are built and tested against the still-in-place `NoOpAdGateway` stub **before** the real ad SDK is wired in. The stub's silent auto-grant is what currently hides the missing opt-in UI; swapping in a real implementation without the dialog ships an AdMob policy violation.

</domain>

<decisions>
## Implementation Decisions

### Allowance policy (ADS-02)
- **D-01:** The free allowance is **separate counters per assist type** — free hints AND free undos tracked independently. Protects undo (the habit-formed v1.0 feature) from being drained by hint use. Both counters live in one pure `:domain` policy object per Phase 6 D-04 (`AssistAllowance`-style, modeled on `ScoreCalculator`) — not in the gateway, not in `GameViewModel`.
- **D-02:** Allowance size is **3 hints / 5 undos per level**. Named, scoped constants (no magic numbers).
- **D-03:** A watched rewarded ad **refills the spent counter back to its full allowance** (e.g. 3 more hints). One ad per counter-exhaustion in practice — no per-assist nagging.
- **D-04:** The allowance **persists across retries and process death — it survives everything until the level is won**. Retrying does NOT refill it; counters are carried in the resume snapshot; reset happens only on winning the level (moving to a fresh level) or a rewarded refill. Closes the "retry to farm free assists" loophole. Same 3/5 numbers in Classic and Challenge.
- **Requirement reminder (ADS-06, locked upstream):** a hint that honestly reports "no winning move" is never charged against the allowance or an ad.

### Opt-in offer UX (ADS-01, ADS-04)
- **D-05:** Hint/undo buttons **never change appearance** — no counter badge, no ad icon. The opt-in dialog is the only signal the allowance system exists. (User explicitly chose this over the recommended always-visible counter badge.)
- **D-06:** The offer is a **themed Compose dialog in the game's (Candy Clay) styling** — not a stock M3 AlertDialog, not a bottom sheet. It names the reward explicitly (ADS-01) with a Watch ad / No thanks pair; declining leaves the game exactly as it was.
- **D-07:** Because there is no button-state signal (D-05), **the dialog copy carries the first-encounter explanation**: e.g. "You've used your 3 free hints for this level — watch a short ad for 3 more?". No one-time intro snackbar, no update-day changelog dialog.
- **D-08:** Rescue-banner integration (ADS-04): the banner's rescue-undo **spends a normal undo — free and instant while undos remain; at 0 the banner action opens the same themed opt-in dialog** (reward: undo refill). One consistent allowance, one dialog, no second offer surface.

### Ad loading & fail-open (ADS-03, ADS-05, CONS-04)
- **D-09:** Rewarded ad **preloads the moment any counter hits 0** — the earliest point an offer can occur. No ad requests while allowance remains (most levels never request at all).
- **D-10:** If the player accepts before the ad is ready: **in-dialog loading state on the button with a ~5s cap**, then fall open — grant the assist free with the explanation. Bounded wait, single surface, no dead end.
- **D-11:** The ADS-03 "brief explanation" on a free grant (`RewardOutcome.Unavailable`) is a **transient snackbar** (e.g. "Ad unavailable — hints refilled for free") while the assist lands immediately. Not a blocking confirmation, not silent.
- **D-12:** When ads are **knowably unservable** (consent flow failed → CONS-04 fallback, SDK never initialized), **skip the opt-in dialog entirely — assists silently stay free past the allowance**, exactly like v1.0 NoOp behavior. No "watch an ad?" → instant-free-grant absurdity, no snackbar spam.
- **Seam contract reminder (locked, Phase 6 D-01):** `Granted` and `Unavailable` both proceed with the assist; only `Declined` skips. Reward granted only when the ad reports it earned (ADS-05); an unearned early dismiss is `Declined`.

### Privacy & consent surfaces (CONS-01..03, REL-05)
- **D-13:** The **privacy policy URL is already live** and reachable over HTTPS on the developer website — no authoring/hosting work in this phase; wire it into Settings. (Planner: obtain the exact URL from the user and store it as a build-config value or scoped constant, consistent with PLAT-06 conventions.)
- **D-14:** Settings gains a **"Privacy" section at the bottom**, under the existing Sound/Haptics switches: a "Privacy policy" row always present (REL-05), and a "Manage privacy choices" row beneath it appearing **only when UMP reports a required consent form** (CONS-03).
- **D-15:** The privacy policy opens via **plain external browser intent (`ACTION_VIEW`)** — no Custom Tab dependency, no in-app WebView.
- **D-16:** The UMP consent form shows **at app launch** — gather + show on the first Activity at cold start, before Home. SDK initialization proceeds immediately after consent resolves, off the main thread (CONS-02). Every later surface may assume consent state is resolved.

### Post-research resolutions (2026-08-31, user-confirmed)
- **D-17 (resolves RESEARCH.md open question 1):** Allowance persistence uses **Option A** — counters live in-memory across level restarts (so `resetToInitial()`'s `clearSnapshot()` cannot refill them) and ride the resume snapshot for process death. The one documented hole (kill-app-then-restart refills counters) is accepted. This refines D-04; DataStore (Option B) is rejected.
- **D-18 (resolves RESEARCH.md open question 3):** The UMP consent message is **not yet configured** in the AdMob console. The plan must include a `checkpoint:human` before any on-device consent verification (CONS-01) — UMP renders nothing without the published message, and its absence is invisible from a non-EEA test location. The user will configure it in AdMob → Privacy & messaging in parallel.
- **D-19 (partially resolves RESEARCH.md open question 2):** The user will supply the exact live HTTPS privacy-policy URL before/at execution; it is wired as a build-config value or scoped constant per D-13. If the URL is not yet recorded here when the Settings row task runs, execution pauses at a `checkpoint:human` to collect it. <!-- URL pending: replace this comment with the URL when provided -->

### Claude's Discretion
- Exact dialog/snackbar/Settings-row wording and the dialog's visual composition (within the Candy Clay theme).
- Clock-freeze mechanics for ADSF-01 (how the ticker pauses across ad presentation; the rescue-undo ticker resume pattern in `GameViewModel` is prior art).
- The resume-snapshot field shape for the persisted counters (D-04) and its backward-compatibility handling for existing snapshots (missing field → treat as fresh full allowance is the obvious fail-open default, consistent with existing decode behavior).
- Ad reload/retry cadence after a failed preload, and mid-show error handling (map to `Unavailable`).
- How "knowably unservable" (D-12) is represented on the gateway (e.g. a queryable readiness/availability signal vs. a fourth outcome) — must not break the sealed three-way `RewardOutcome` contract call sites already handle.
- Decline re-offer behavior (tapping the button again after "No thanks" simply re-opens the dialog — no cooldown was discussed; keep it simple).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requirements & scope
- `.planning/REQUIREMENTS.md` — CONS-01..04, ADS-01..06, ADSF-01, REL-05 + the Milestone Decisions table (GMA Next-Gen `ads-mobile-sdk` 1.4.0, per-level allowance, retry stays free).
- `.planning/ROADMAP.md` — Phase 9 goal, success criteria, the stub-first ordering note, and the External Prerequisites table (AdMob rewarded ad units; UMP consent message configured in the AdMob UI — renders nothing without it).
- `.planning/PROJECT.md` — constraints, Key Decisions.

### Prior phase decisions that bind this phase
- `.planning/phases/06-platform-foundation-build-hardening/06-CONTEXT.md` — D-01 (`RewardOutcome` semantics), D-04 (allowance lives in pure `:domain`), D-05/D-08/D-09 (test IDs in debug, real IDs untracked, release fail-fast), D-07/D-17 (auto-init provider removal; explicit init in code — required by CONS-02), D-20..D-23 (published-app identity `com.erman.pegsolitarie`).

### Milestone research
- `.planning/research/ARCHITECTURE.md` — `ForegroundActivityProvider` pattern; ads/consent integration points.
- `.planning/research/PITFALLS.md` — consent/ads pitfalls (several are Phase 9-specific).
- `.planning/research/STACK.md` — verified GMA Next-Gen / UMP dependency graph.

### Existing code this phase modifies or consumes
- `app/src/main/java/com/app/pegsolitaire/platform/ads/AdGateway.kt`, `RewardOutcome.kt`, `AdReason.kt`, `NoOpAdGateway.kt` — the seam; its KDoc already anticipates this phase's dialog-in-front-of-ads work.
- `app/src/main/java/com/app/pegsolitaire/platform/ads/AdIds.kt` — build-config ad-unit plumbing from Phase 6.
- `app/src/main/java/com/app/pegsolitaire/ui/game/GameViewModel.kt` — the two `requestRewarded` call sites (`onUndo`, hint request), the `noWinningMove` rescue-banner state, and the ticker pause/resume prior art for ADSF-01.
- `app/src/main/java/com/app/pegsolitaire/ui/settings/` — `SettingsScreen.kt` / `SettingsViewModel.kt` / `SettingsUiState.kt` — where the Privacy section lands.
- `app/src/main/java/com/app/pegsolitaire/di/PlatformModule.kt` — the `@Binds` swap point for the real gateway.
- `app/src/main/java/com/app/pegsolitaire/platform/activity/` — the `ForegroundActivityProvider` bridge (PLAT-01) that UMP and ad show-calls require.

### Project standards
- `CLAUDE.md` — no magic numbers (allowance sizes, timeout become scoped constants), dimens file, state hoisting per `compose-skill`, detekt/ktlint gates, Gitflow.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- **`RewardOutcome` sealed seam + both `GameViewModel` call sites** — the three-way branch (Granted/Unavailable proceed, Declined aborts) already exists and is exactly the contract the real gateway must fulfill; the dialog slots in front of `requestRewarded`, not inside the ViewModel branches.
- **Rescue-banner + rescue-undo flow** — `noWinningMove` state, banner UI, and the ticker-resume-after-rescue pattern in `GameViewModel` are the surfaces D-08 and ADSF-01 build on.
- **Resume snapshot codec** — replay-validated, fail-safe decode; the persisted allowance counters (D-04) extend it with the established fail-open convention for missing/invalid fields.
- **Pure `:domain` policy-object pattern** — `ScoreCalculator`/`DifficultyTiers`/`ScoreParams` model the `AssistAllowance` object (Phase 6 D-04 mandates this placement).
- **Phase 6 build plumbing** — test-vs-real ad-unit IDs via `buildConfigField`, provider auto-init already stripped, `INTERNET` permission present, R8 keep-rule surface prepared.

### Established Patterns
- **State hoisting / UDF** — dialog visibility and snackbar messages belong in `GameUiState` (or a small ads-facing UiState slice), rendered statelessly; Settings rows follow `SettingsUiState`.
- **Purity gates are CI-enforced** — `gms`/`ump` imports compile only inside `platform/`; the allowance policy is pure JVM-testable Kotlin.
- **Fail-open convention** — DataStore reads, snapshot decode, and now ad-unavailability all degrade toward "the player keeps playing".

### Integration Points
- `PlatformModule` — swap `NoOpAdGateway` binding for the real implementation (last step, after dialog + allowance are proven against the stub).
- `App` (`@HiltAndroidApp`) + first Activity — UMP gather/show at launch (D-16), then explicit off-main-thread SDK init (CONS-02).
- `GameViewModel` ↔ allowance policy ↔ dialog — decrement on granted assists, gate `requestRewarded` behind counter exhaustion, freeze the elapsed-time ticker for the full ad presentation (ADSF-01).
- Settings screen — Privacy section (D-14) with UMP `privacyOptionsRequirementStatus` driving the CONS-03 row's visibility.

</code_context>

<specifics>
## Specific Ideas

- The dialog copy pattern that carries the whole system's legibility (D-05/D-07): **"You've used your 3 free hints for this level — watch a short ad for 3 more?"** — count, scope, and reward in one sentence.
- The governing product sentence from ROADMAP: *never surprised, never dead-ended, never charged time* — every decision above maps onto one of those three.
- v1.0 parity when ads can't exist: a player with no consent, no fill, or no SDK sees the game behave exactly as it did in v1.0 (free assists, no dialogs) — monetization degrades to absence, never to friction.

</specifics>

<deferred>
## Deferred Ideas

### Reviewed Todos (not folded)
- **Confirm audio cues on physical hardware** — Phase 7 follow-up, unrelated to ads; stays a pending todo.
- **Upgrade Wiegleb-45 opening book to depth 4** — Phase 8 follow-up compute task; stays a pending todo.
- **Decide whether Stuck finishes should reach Results (1–2 star bands unreachable)** — scoring/flow decision independent of ads; touches the same rescue-banner surface but was deliberately kept separate. Planner should avoid structural rework of the banner that would prejudge it.

</deferred>

---

*Phase: 9-Consent & Rewarded Ads*
*Context gathered: 2026-08-30*
