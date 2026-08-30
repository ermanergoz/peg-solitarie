# Phase 9: Consent & Rewarded Ads - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-08-30
**Phase:** 9-Consent & Rewarded Ads
**Areas discussed:** Allowance policy, Opt-in offer UX, Ad loading & fail-open UX, Privacy & consent surfaces

---

## Pending-todo cross-reference

Three pending todos keyword-matched the phase. User chose **"None — keep all separate"**: the audio-hardware check (Phase 7 follow-up), the Wiegleb-45 depth-4 book upgrade (Phase 8 follow-up), and the Stuck→Results decision (independent scoring/flow question) all stay pending todos.

---

## Allowance policy

| Option | Description | Selected |
|--------|-------------|----------|
| Separate counters | N free hints AND M free undos per level; protects undo from being drained by hint use | ✓ |
| One shared pool | N assists spent by either; simpler to communicate, hint-heavy play drains undo | |
| Only limit hints | Undo stays unlimited; weakest monetization | |

| Option | Description | Selected |
|--------|-------------|----------|
| 3 hints / 5 undos | Generous undo keeps v1.0 feel; ad offer still matters on hard boards | ✓ |
| 1 hint / 3 undos | Tight; stronger monetization, higher churn risk | |
| 5 hints / 10 undos | Very generous; rewarded revenue near zero | |

| Option | Description | Selected |
|--------|-------------|----------|
| Refill that counter | One ad refills the spent counter to full | ✓ |
| Single assist per ad | One ad = one assist; nagging pattern | |
| Unlimited for this level | One ad unlocks the rest of the level | |

| Option | Description | Selected |
|--------|-------------|----------|
| Reset on retry; same everywhere | Fresh allowance every level start incl. retries | |
| Reset on retry; Challenge tighter | Classic generous, Challenge smaller | |
| Persists across retries | Retry does NOT refill; blocks hint-farming | ✓ |

Follow-up on durability (raised by the persist choice):

| Option | Description | Selected |
|--------|-------------|----------|
| Survives everything until won | Counters live in the resume snapshot; reset only on win or rewarded refill; same 3/5 both modes | ✓ |
| Per-session only | Fresh app launch refills; no snapshot change | |
| Survives until won + tighter Challenge | Same durability, smaller Challenge numbers | |

**User's choice:** Separate counters, 3 hints / 5 undos, ad refills the spent counter, allowance persists across retries and process death until the level is won; identical numbers in Classic and Challenge.
**Notes:** The persist-across-retries pick was the one deviation from the recommended option in this area — user explicitly closed the "retry to farm free assists" loophole even though retry itself stays free.

---

## Opt-in offer UX

| Option | Description | Selected |
|--------|-------------|----------|
| Counter badge always visible | Badge counts down 3→0, then ad icon; limit never a surprise | |
| Ad icon only when spent | v1.0 look until 0, then ad badge | |
| No button change at all | Buttons never change; dialog is the only signal | ✓ |

| Option | Description | Selected |
|--------|-------------|----------|
| Themed dialog, game styling | Candy Clay-styled Compose dialog naming the reward | ✓ |
| Plain Material 3 AlertDialog | Stock system look, fastest | |
| Bottom sheet offer | Modal sheet; new pattern for the app | |

| Option | Description | Selected |
|--------|-------------|----------|
| Free while allowance lasts, then offer | Rescue-undo spends a normal undo; at 0 the banner opens the same dialog | ✓ |
| Banner rescue always free | Being stuck stays cost-free like retry | |
| Banner offers ad directly | Inline offer in the banner; second surface | |

| Option | Description | Selected |
|--------|-------------|----------|
| Dialog copy carries it | First-encounter explanation lives in the dialog text | ✓ |
| One-time intro on first decrement | One-shot snackbar + seen-flag | |
| Update-day changelog note | v1.1 first-launch dialog | |

**User's choice:** No button-state signaling (chosen against the recommendation); themed game-styled dialog whose copy names count, scope, and reward; rescue banner routes through the normal undo allowance into the same dialog.
**Notes:** With no persistent signal, the dialog copy carries all system legibility — e.g. "You've used your 3 free hints for this level — watch a short ad for 3 more?".

---

## Ad loading & fail-open UX

| Option | Description | Selected |
|--------|-------------|----------|
| Preload when a counter empties | Earliest offer point; no requests while allowance remains | ✓ |
| Preload at level start | Always warm; wasted requests on most levels | |
| Load on accept only | Zero waste; several-second spinner every accept | |

| Option | Description | Selected |
|--------|-------------|----------|
| In-dialog spinner, ~5s cap | Button becomes loading state; free grant after ~5s | ✓ |
| Grant free immediately if not ready | Snappiest; loses late-loading impressions | |
| Longer wait, ~10s cap | More impressions on slow networks; long stare | |

| Option | Description | Selected |
|--------|-------------|----------|
| Snackbar | Transient "Ad unavailable — refilled for free"; assist lands immediately | ✓ |
| Dialog morphs into confirmation | Explicit, one extra tap | |
| Silent grant | Violates ADS-03's "brief explanation" | |

| Option | Description | Selected |
|--------|-------------|----------|
| Skip dialog, assists stay free | Knowably-unservable ads → v1.0 NoOp behavior, no dialog | ✓ |
| Show dialog, always fall open | Uniform flow; asks player to watch a nonexistent ad | |
| Skip dialog, one-time snackbar | Free + single per-level explanation | |

**User's choice:** All four recommended options.
**Notes:** None.

---

## Privacy & consent surfaces

| Option | Description | Selected |
|--------|-------------|----------|
| Website up, policy not written yet | Authoring/publishing becomes an early plan step | |
| Policy already live | URL exists, reachable over HTTPS — just wire it in | ✓ |
| No website yet | Blocking external prerequisite | |

| Option | Description | Selected |
|--------|-------------|----------|
| Privacy section at bottom | New section under Sound/Haptics: policy row always, choices row region-gated | ✓ |
| Single 'Privacy' row → sub-screen | Sub-screen for two items | |
| About/footer placement | Lowest weight, less discoverable | |

| Option | Description | Selected |
|--------|-------------|----------|
| Chrome Custom Tab | In-app, one androidx.browser dependency | |
| External browser intent | Plain ACTION_VIEW, zero dependencies | ✓ |
| In-app WebView screen | Heaviest, outdated pattern | |

| Option | Description | Selected |
|--------|-------------|----------|
| At app launch | UMP gather+show at cold start before Home; SDK init right after | ✓ |
| Deferred to first game entry | Form when first entering a level | |
| Deferred until first offer possible | Last legal moment; races consent state | |

**User's choice:** Policy already live; Privacy section at the bottom of Settings; external browser intent (chosen over the recommended Custom Tab); consent form at app launch.
**Notes:** Planner must obtain the exact policy URL from the user.

---

## Claude's Discretion

- Exact dialog/snackbar/Settings wording and dialog visual composition (Candy Clay theme).
- ADSF-01 clock-freeze mechanics (ticker pause/resume across ad presentation).
- Resume-snapshot field shape for persisted counters + backward compatibility (missing field → fresh full allowance).
- Ad reload/retry cadence after failed preload; mid-show error → `Unavailable`.
- How "knowably unservable" is represented on the gateway without breaking the sealed `RewardOutcome` contract.
- Decline re-offer behavior (re-tap simply re-opens the dialog).

## Deferred Ideas

None new — discussion stayed within phase scope. The three reviewed-but-not-folded todos are recorded in CONTEXT.md `<deferred>`.
