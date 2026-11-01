# Feature Research — v1.1 "Slice 2" (Play Games · AdMob · Sound)

**Domain:** Casual single-player puzzle game on Android — social/services layer (Play Games v2), ad monetization (AdMob + UMP), and game audio
**Researched:** 2026-08-27
**Confidence:** HIGH for platform/policy behavior (official Google docs), MEDIUM for UX conventions (industry practice + casual-puzzle literature)

> Scope note: this file covers **only the new v1.1 surface**. The v1.0 feature landscape (rules, boards, generator, hints, undo, stars/score, persistence) is in the sections of this repo's earlier research and in `.planning/PROJECT.md` under *Validated*. Everything here is layered onto a **shipped, fully playable offline game** — which changes the calculus: features that would be neutral at launch (ad gating on hint/undo) are now **removals of something players already have**, and are flagged as such.

---

## Existing surfaces this milestone plugs into

| Existing thing (v1.0) | Where | What v1.1 attaches to it |
|---|---|---|
| `GamesService` seam (`signIn(): Boolean`, `submitScore(id, score)`) | `app/.../platform/games/GamesService.kt` | Real PGS v2 impl; seam needs **new methods** for achievements (unlock/increment) and UI intents (show leaderboard/achievements) — current 2-method interface is insufficient |
| `AdGateway` seam (`requestRewarded(AdReason): Boolean`, `showInterstitial()`) | `app/.../platform/ads/AdGateway.kt` | Real AdMob impl; `AdReason` currently has `HINT`, `UNDO` only — `RETRY` deliberately excluded, doc-comment says retry flows through `showInterstitial()` |
| `GameViewModel` — sole call site of both seams | `app/.../ui/game/GameViewModel.kt` | All gating logic already funnels here; ad wiring should not spread |
| `SettingsRepository.soundOn` (persisted, currently unconsumed — "no audio engine yet, D-16") | `app/.../data/SettingsRepository.kt` | The audio engine finally consumes it |
| `SettingsRepository.hapticsOn` (already gates jump haptic) | same | Audio pairs with the existing haptic tick; keep the two flags independent |
| Settings screen | `app/.../ui/settings/` | Hosts the **UMP privacy-options entry point** (required when `getPrivacyOptionsRequirementStatus() == REQUIRED`) and optional PGS sign-in fallback / "view achievements" links |
| `ProgressRepository` (Room bests + DataStore ladder) | data layer | Cloud save becomes a **second data source behind it**, per the seam's own doc-comment |
| Time-decay score + elapsed timer | domain / `GameViewModel` | Feeds leaderboards **and** must be **paused across every ad presentation** (see Dependencies) |
| Results screen (sequential star pops, count-up score, CTA) | `ui/results` | Natural interstitial trigger point (after CTA tap, not during the celebration) + star-pop SFX hooks |
| Rescue banner | gameplay UI | The single most natural rewarded-ad placement in the game |

---

## Feature Landscape

### Table Stakes (Users Expect These)

Missing any of these reads as broken, unpolished, or gets the app rejected/penalized.

| Feature | Why Expected | Complexity | Notes |
|---|---|---|---|
| **Silent automatic PGS sign-in at launch; game never blocks on it** | PGS v2 signs in automatically at game start; there is no sign-out and no required sign-in button. Google's own quality checklist states "don't block gameplay on sign-in" | LOW | Check `GamesSignInClient.isAuthenticated()` after launch. If authenticated → enable social features. If not → hide them silently. `GamesService.signIn()` must only ever be called from an **explicit user action**, never on startup |
| **Graceful signed-out mode (everything still works)** | Sign-in depends on GMS Core + device state and can just fail | LOW | Repository stays the single source of truth; leaderboard/achievement submissions become no-ops. This is already the `NoOpGamesService` contract — keep that behavior as the failure path, not an error dialog |
| **Manual sign-in fallback affordance** | Official guidance: "if automatic sign-in does not work or the user declines, show a manual sign-in button" | LOW | One entry in Settings ("Sign in with Google Play Games"), shown only when `isAuthenticated()` is false. Do **not** put a nag dialog on Home |
| **At least one leaderboard, submitted on level completion** | Score exists and is meaningless without a ranking surface | LOW–MEDIUM | Submit at level end only — PGS quality checklist explicitly warns against per-second submission. Fire-and-forget (the existing seam doc already mandates: a failed submission must never fail a solved level) |
| **≥10 visible achievements, ≥4 reachable in the first hour** | Hard minimum in Google's PGS quality checklist; below it the integration is considered non-compliant and won't qualify for Play's games surfaces | MEDIUM | Complexity is **not** the API (`unlock`/`increment`) — it's that several natural achievements need data the app doesn't currently record (see Dependencies) |
| **Achievement unlock popup rendered in-app** | Players expect the Google toast when they earn something | LOW | Requires `setViewForPopups(...)` / `setGravityForPopups(...)` and **icons uploaded in Play Console** — popups silently don't render if achievement art is missing (common bug report) |
| **Entry points to the leaderboard and achievement UIs** | Unlockables players can't look at don't feel earned | LOW | Launch Google's stock intents from Home and/or Results. Custom UI is out of scope for v1.1 |
| **Cloud save of Challenge progress + per-level bests** | Losing a 300-level climb on device change is the #1 rage-uninstall for ladder games; also a PGS quality-checklist requirement | MEDIUM–HIGH | Saved Games snapshot ≤3 MB binary, cover image ≤800 KB. Must attach **description + played time + cover image** metadata to be checklist-compliant |
| **Non-destructive conflict handling** | Google: "don't automatically overwrite significant player data"; let the player decide when you can't reconcile | MEDIUM | **Opinion: our data is a monotonic lattice, so no dialog is needed** — merge deterministically (see Differentiators). Reserve a dialog for the one unmergeable case |
| **UMP consent gathered before any ad request (EEA/UK/CH)** | Legal/policy gate; `canRequestAds()` returns false until `requestConsentInfoUpdate()` has run | MEDIUM | `requestConsentInfoUpdate()` **every launch** → `loadAndShowConsentFormIfRequired()` → only then initialize GMA / request ads. On failure, fall back to the previous session's consent status and simply serve no ads |
| **Persistent privacy-options entry point in Settings** | Required whenever `getPrivacyOptionsRequirementStatus() == REQUIRED`; users must be able to change consent at any time | LOW | Conditionally-visible Settings row calling `showPrivacyOptionsForm()`. Drops straight into the existing Settings screen |
| **Rewarded ads are strictly opt-in, with the reward named up front** | Play's disruptive-ads policy only exempts ads "explicitly opted-in by users… in exchange for unlocking a specific game feature." Non-opt-in full-screen = policy violation | LOW–MEDIUM | Modal: "Watch a short ad for a hint?" → [Watch ad] / [No thanks]. Never auto-play. Grant **only** in `OnUserEarnedRewardListener` |
| **Rewarded ads preloaded; graceful behavior on no-fill** | Fill rates run ~85–95%; a spinner-then-nothing is the worst outcome | MEDIUM | Preload at level start; ads expire after ~1 hour → refresh. If unavailable, **never dead-end the player** (see Differentiators: free-fallback) |
| **Interstitials only at natural breaks, frequency-capped** | AdMob: place between levels, "explicitly before any Continue or Next level buttons." Play policy bans full-screen ads at level start / before app load / non-dismissible >15s | MEDIUM | Concrete cap recommendation below |
| **A brief delay/beat before an interstitial after tap-heavy play** | AdMob explicitly recommends a loading/please-wait beat to prevent inadvertent clicks in tap-heavy apps — peg solitaire is exactly that | LOW | ~300–600 ms transition; the existing Results CTA gives a natural pause |
| **Game clock pauses across ad presentation** | Otherwise a 30 s rewarded video silently destroys the player's time-decay score | LOW | Cross-cutting; see Dependency Notes — easy to miss, expensive to discover in QA |
| **Core SFX set: jump/land, peg removal, invalid tap, win, star pops, button press** | An unmuted casual puzzle with silent taps feels broken/unfinished; the Candy Clay redesign raises the expectation bar | MEDIUM | Event list and design rules below |
| **`soundOn` actually mutes everything, instantly** | The toggle already exists and currently does nothing | LOW | Read the flow in the audio layer; mute must apply to already-queued one-shots too |
| **SFX respect system media volume; no in-app volume slider** | Platform convention — a custom slider is redundant and users don't look for it | LOW | Play through `AudioAttributes` `USAGE_GAME`; system media volume governs |
| **Never steal audio focus / never kill the player's music** | A large share of casual-puzzle sessions run over music or a podcast. Permanent audio-focus requests are for playback apps; short SFX use transient (or no) focus | LOW | Strong recommendation: **request no focus at all** for sub-second one-shots — they mix over the user's audio. This is the single most-noticed audio mistake in casual games |

### Differentiators (Competitive Advantage)

Where v1.1 can be better than a checkbox integration.

| Feature | Value Proposition | Complexity | Notes |
|---|---|---|---|
| **Ladder leaderboard: "Highest Challenge Level Reached"** | This is the whole product's core value made competitive — and it's only legible *because* levels are deterministic (level N is identical for everyone). No competitor's finite peg catalog can offer this | LOW | Numeric, larger-is-better, tamper protection ON, upper score bound set (e.g. a sane max level) to discard fake submissions |
| **Small, curated leaderboard set instead of one-per-level** | Infinite levels make per-level boards impossible (PGS caps at **70 leaderboards**). A tight set stays meaningful | LOW | **Recommended set (7 total):** 1× Highest Challenge Level · 1× Cumulative Challenge Score (or Total Stars) · 5× per-Classic-board best score (English-33, European-37, Asymmetric-39, Diamond-41, Wiegleb-45). Classic boards are finite and fixed → per-board boards are legitimate |
| **Deterministic monotonic cloud merge (zero conflict dialogs)** | Peg-solitaire progress is a *join-semilattice*: `highestLevel = max`, per-level `stars = max`, `score = max`, `bestTime = min`. Two devices always merge losslessly — no "which save do you want?" dialog, no data loss, no user cognitive load | MEDIUM | Genuinely better than the industry norm of a scary conflict modal. Keep an explicit fallback dialog for the one unmergeable case (signed-in account already has a *different* PGS profile's progress) |
| **Achievements that teach mastery, not grind** | Peg solitaire has real skill texture: leaving one peg, leaving it *in the center*, solving hint-free, solving undo-free | MEDIUM | Proposed set below — designed to hit ≥10 visible, ≥4 within an hour, no frontloading, with incremental tiers |
| **Free-fallback rewarded gating (never dead-end)** | v1.0 shipped hints/undo **free**. Hard-gating them behind ads in v1.1 is a *takeaway* — the classic post-launch review-bomb trigger. A free allowance + ad-for-more preserves goodwill and still monetizes | MEDIUM | Recommended: N free hints/undos per level (or per day), rewarded ad for extras; **and on ad load failure / no-fill, grant the assist anyway** with a quiet "Ads unavailable — here's one on us." The seam's `Boolean` return already supports this |
| **Rescue-banner-anchored rewarded placement** | The rescue banner fires exactly at the moment of maximum player motivation ("I'm stuck") — the highest-converting, least-resented rewarded moment in the game | LOW | Reuses shipped UI; no new surface |
| **Pitch-varied jump SFX** | A single unvaried jump sound becomes fatiguing within ~40 jumps; ±5–8% random playback-rate variation makes a 30-minute session pleasant. SoundPool's `rate` parameter makes this ~free | LOW | Highest juice-per-line-of-code item in the milestone |
| **Star-pop SFX in rising pitch, sequenced to the existing animation** | The Results screen already pops stars sequentially — three ascending tones turn a visual beat into a reward hit | LOW | Pure wiring onto an existing animation |
| **Re-submit bests on (late) sign-in** | Player plays 40 levels offline, signs in on day 3 — their history should appear on the leaderboard, not just future scores | LOW–MEDIUM | On `isAuthenticated()` transitioning true, replay current bests from `ProgressRepository` into PGS once |
| **Interstitials suppressed for the first session / first N levels** | New players who see an ad in their first three minutes churn hardest; deferring ads costs almost nothing in revenue and materially protects D1 | LOW | Gate on `highestLevelReached` or a "levels completed this install" counter |

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why Requested / Surface Appeal | Why Problematic | Alternative |
|---|---|---|---|
| **Banner ad on the gameplay screen** | Easy always-on revenue | Steals board area (the board is the product), fights the Candy Clay layout, low RPM in a long-session puzzle, and increases mis-tap risk next to the board | Rewarded + capped interstitials only. No banners in v1.1 |
| **App-open ad** | High eCPM format | Play's disruptive-ads policy explicitly targets full-screen ads before the app loads; also the worst possible FTUE for a returning daily player | None. First ad opportunity is post-level |
| **Interstitial after a *loss* / dead-end** | "They failed, monetize the reset" | Stacking an ad onto the game's most frustrating moment is the top-cited casual-puzzle churn driver — and it collides with the rescue-banner rewarded offer | Show interstitials only after **wins**, or on exit-to-menu. Keep the loss moment for the (opt-in, helpful) rewarded rescue |
| **Interstitial between the win and the star celebration** | "Catch them at peak attention" | Interrupting the reward animation destroys the payoff the entire scoring system exists to deliver | Fire after the Results CTA tap, before the next level loads |
| **Hard-gating all hints/undo behind ads** | Maximizes rewarded impressions | This is a **removal of a shipped v1.0 feature**; guarantees "they added ads and took away undo" reviews. Also collides with the hint's honest "no winning move" state — charging an ad for a "there is no move" answer is a trust breach | Free allowance + rewarded for extras; never charge for the negative-result hint |
| **Forced sign-in / sign-in wall / repeated sign-in nags** | "Capture the user" | PGS v2 removed sign-out and made auth automatic precisely to end this pattern; the quality checklist bans blocking gameplay on sign-in | Auto sign-in silently; one passive Settings fallback |
| **Conflict-resolution dialog on every cloud sync** | Feels "safe" | Our data merges deterministically; a modal on each launch is pure friction and trains users to tap through data-loss prompts | Auto-merge (monotonic max/min); dialog only in the genuinely unmergeable case |
| **Cloud-saving the in-progress board snapshot** | "Resume on any device" | The snapshot is replay-validated local state; syncing a half-finished board across devices creates real conflicts (both devices mid-game), inflates the payload, and adds a failure mode to the app's most fragile path | Sync **bests + ladder position only** in v1.1. Keep the resume snapshot local. Revisit if users actually ask |
| **Background music** | "Polish" | Large asset, forces audio-focus/ducking policy decisions, and most casual-puzzle sessions are played muted or over the user's own audio — high cost, low realized value | SFX-only in v1.1. If music ever ships, it must be a separate `musicOn` toggle defaulting **off** |
| **A custom in-app volume slider** | "More control" | Duplicates the hardware/system control; extra state, extra persistence, extra QA | System media volume + the existing binary `soundOn` |
| **Loud/harsh "wrong!" buzzer on invalid taps** | Clear feedback | Invalid taps are frequent and often exploratory; a punishing sound makes the game feel hostile and gets people to mute the app entirely (killing all SFX revenue of goodwill) | A quiet, short, low-pitched "thud"/"nope" at reduced volume, paired with the existing haptic. Consider suppressing repeats within ~300 ms |
| **Server-side reward verification (SSV)** | "Prevent reward fraud" | Requires a backend this project doesn't have; the reward is a hint in a single-player puzzle — the fraud surface is a player cheating themselves | Client-side grant in the reward callback. Revisit only if real currency ever exists |
| **>15 achievements at launch / achievement spam in the first 5 minutes** | "More engagement" | The quality checklist warns against frontloading; also every achievement needs a unique 512×512 icon (real art cost) and Console config | 10–14 well-spaced achievements, incremental tiers doing the heavy lifting |
| **Per-level leaderboards for Challenge mode** | "Compare on every level" | PGS hard-caps at 70 leaderboards; Challenge is infinite. Non-starter | Aggregate ladder + cumulative score boards; per-board boards for the 5 finite Classic boards |

---

## Recommended concrete configurations

### Achievements (proposed 12 — meets ≥10 visible, ≥4 within first hour)

| # | Achievement | Type | First hour? | Data needed |
|---|---|---|---|---|
| 1 | **First Jump** — complete any level | Standard | ✅ | Exists |
| 2 | **Perfect Solve** — finish a level with one peg left (★★★) | Standard | ✅ | Exists (stars) |
| 3 | **Classic Initiate** — clear any Classic board | Standard | ✅ | Exists |
| 4 | **Climbing** — reach Challenge level 10 | Standard | ✅ | Exists (ladder) |
| 5 | **Ascendant** — reach Challenge level 50 | Incremental (50 steps) | — | Exists |
| 6 | **Centurion** — reach Challenge level 100 | Incremental (100 steps) | — | Exists |
| 7 | **Grandmaster** — reach Challenge level 500 | Incremental (500 steps) | — | Exists |
| 8 | **Board Collector** — clear all 5 Classic boards | Incremental (5 steps) | — | Exists (per-board results) |
| 9 | **Flawless Ten** — earn ★★★ on 10 levels | Incremental (10 steps) | — | Exists |
| 10 | **Unaided** — win a level using no hints | Standard | ✅ (skill-gated) | ⚠️ **needs per-session hint counter persisted to the result** |
| 11 | **No Take-Backs** — win a level using no undos | Standard | ✅ (skill-gated) | ⚠️ **needs per-session undo counter** |
| 12 | **Bullseye** *(hidden)* — solve English-33 leaving the final peg in the center | Standard (hidden) | — | ⚠️ **needs final-peg position recorded** |

Achievements 10–12 are the ones with real cost: they require the domain/result model to carry a little more per-session telemetry (hints used, undos used, final peg coordinate). That is a **domain + Room schema change**, not a Play Console change. Either accept the migration, or swap them for progress-only achievements (e.g. "1,000 total jumps", also a new counter) — flag this as a planning decision.

### Leaderboards (proposed 7)

| Leaderboard | Order | Format | Submitted when |
|---|---|---|---|
| Highest Challenge Level | larger better | numeric | On Challenge level completion, if new max |
| Cumulative Challenge Score (or Total Stars) | larger better | numeric | On Challenge level completion, if aggregate changed |
| Best Score — English-33 | larger better | numeric | On that board's completion, if new best |
| Best Score — European-37 | larger better | numeric | ″ |
| Best Score — Asymmetric-39 | larger better | numeric | ″ |
| Best Score — Diamond-41 | larger better | numeric | ″ |
| Best Score — Wiegleb-45 | larger better | numeric | ″ |

Submit the **existing time-decay score** (already computed, already stored as the per-level best) — not raw time. Enable tamper protection (on by default for new Android leaderboards; takes up to 24 h to activate) and set upper score bounds. Leaderboard IDs go in a scoped constants object, per the project's no-magic-strings rule.

### Ad frequency policy (recommended starting point)

| Rule | Value | Rationale |
|---|---|---|
| First interstitial | Not before level **4** of the install, and not in the first session | Protects FTUE / D1 |
| Interstitial cadence | Every **3rd** completed level, AND ≥ **90 s** since the last interstitial | AdMob casual-game guidance: 60–90 s minimum interval, 2–3 per session |
| Session cap | **3** interstitials | ″ |
| Trigger point | After the Results CTA tap, before the next level loads, with a ~300–600 ms beat | AdMob: between levels, before "Next"; delay prevents inadvertent clicks |
| After a rewarded ad | Suppress interstitials for ≥ **60 s** | Never double-tax a player who just opted in |
| After a loss / dead-end | **Never** | Frustration stacking |
| Rewarded offers | Unlimited but always opt-in and always dismissible | Opt-in ads are exempt from the 15 s-closeable rule |
| Rewarded no-fill | Grant the assist free, with a quiet notice | Never dead-end |

All numbers belong in a scoped `AdPolicy` constants object so they're tunable without hunting through the ViewModel.

### Sound events (recommended set — 11 one-shots)

| Event | Character | Priority | Notes |
|---|---|---|---|
| Peg select / lift | Soft short "tick" / pluck | P1 | Confirms selection; pairs with existing selection visual |
| Jump + land | The signature sound — short woody/candy "thock" | **P0** | Pitch-varied ±5–8%; this is the game's core sensation |
| Peg removal (pop) | Bright short pop | **P0** | Fires with the existing removal pop animation |
| Invalid tap | Quiet low "nope" thud, at reduced volume, debounced | P1 | Must not be punishing (see Anti-Features) |
| Undo | Reverse whoosh (jump sound reversed works) | P1 | Free asset from the jump SFX |
| Hint reveal | Light sparkle/chime | P1 | Distinguish from reward sounds |
| Level win | Short rising fanfare (<1.5 s) | **P0** | Must not fight the star pops |
| Star pop ×3 | Three ascending tones sequenced to the existing pops | **P0** | Highest emotional payoff per byte |
| Score count-up | Quiet repeating tick, terminating cleanly | P2 | Cap stream count; must stop on skip/CTA |
| No moves left / dead-end | Soft descending tone, not harsh | P1 | Coordinates with the rescue banner |
| Button press (UI) | Neutral click | P1 | Applies to nav/menu/CTA buttons app-wide |

**Implementation conventions:** SoundPool (not MediaPlayer) for all of these — sub-second clips, low latency, memory-cheap; cap `maxStreams` at ~4–6 to bound CPU/mixing cost; mono OGG, ~44.1 kHz, normalized to consistent perceived loudness; `AudioAttributes` `USAGE_GAME` + `CONTENT_TYPE_SONIFICATION`; **request no audio focus** for one-shots (transient-may-duck at most) so the player's music survives; load on first gameplay entry (warm the pool before the first tap to avoid a missed first sound), release on lifecycle teardown; the `soundOn` flow gates playback at the single audio-layer entry point, not at 11 call sites.

### UMP consent flow (recommended sequence)

1. **Every launch:** `requestConsentInfoUpdate()` — this is what makes `canRequestAds()` meaningful (it returns false until then).
2. **Before the first ad-relevant moment** (not blocking the splash → Home path): `loadAndShowConsentFormIfRequired()`. Policy-compliant as long as no ad is requested or loaded first. *Simpler, fully-documented alternative: show it at first launch — Google's samples do this. Trade-off is FTUE friction vs. implementation simplicity; pick one and constant-ize it.* (MEDIUM confidence on the deferral; HIGH on the API ordering.)
3. **Initialize the Mobile Ads SDK** and request ads only once `canRequestAds()` is true.
4. **Failure path:** if the consent update errors, use the previous session's status; if that's unavailable, serve **no ads** and let the player play. The game must never be blocked by the consent layer.
5. **Settings:** show the privacy-options row iff `getPrivacyOptionsRequirementStatus() == REQUIRED`, calling `showPrivacyOptionsForm()`.
6. **Testing:** register test device hash IDs and use `setDebugGeography()` (EEA/UK/CH) + `reset()` to simulate first install.

---

## Feature Dependencies

```
Play Console / AdMob account setup (games services project, OAuth clients per
signing cert, test accounts, achievement & leaderboard config + icons,
AdMob app ID + ad units)
    └──BLOCKS──> everything Play Games          [external, long lead time]
    └──BLOCKS──> everything AdMob

PGS v2 auto sign-in (isAuthenticated)
    ├──required by──> Leaderboard submission
    ├──required by──> Achievement unlock/increment
    └──required by──> Cloud save (Saved Games)
                          └──requires──> ProgressRepository as single source of truth  [exists]
                          └──requires──> serializable, mergeable progress model
                                             └──enabled by──> deterministic level seeding  [exists]

GamesService seam (2 methods today)
    └──must be EXTENDED──> unlock/increment achievements + show leaderboard/achievement UI intents

ScoreCalculator / per-level bests  [exists]
    └──feeds──> Leaderboard submission (submit stored best, on level completion only)
    └──feeds──> "re-submit bests on late sign-in"

Achievements #10–12 (no-hint / no-undo / center-peg)
    └──require──> NEW per-session telemetry (hints used, undos used, final peg cell)
                      └──requires──> domain result model change + Room schema migration   ⚠️ real cost

UMP consent (requestConsentInfoUpdate → form → canRequestAds)
    └──BLOCKS──> Mobile Ads SDK init
                     └──BLOCKS──> Rewarded ads ──gates──> Hint / Undo / (Retry)   [exists, currently free]
                     └──BLOCKS──> Interstitials ──trigger from──> Results CTA      [exists]
    └──requires──> Settings screen (privacy options entry point)   [exists]

Ad presentation (rewarded OR interstitial)
    └──MUST PAUSE──> elapsed game timer ──feeds──> time-decay score ──feeds──> leaderboards
    └──MUST NOT overlap──> Results star-pop celebration
    └──MUST NOT overlap──> rescue banner rewarded offer (mutual exclusion window)

Audio engine (SoundPool wrapper)
    └──consumes──> SettingsRepository.soundOn   [exists, currently unconsumed]
    ├──hooks into──> jump / removal animations  [exists]
    ├──hooks into──> Results star pops + count-up  [exists]
    └──independent of──> hapticsOn (separate flag, separate toggle)  [exists]

Interstitial cadence
    └──reads──> levels-completed counter / ladder position  [ProgressRepository, exists]
```

### Dependency Notes

- **Play Console configuration is the true critical path.** Achievement/leaderboard IDs, 512×512 icons, OAuth clients bound to *each* signing certificate (debug, upload, Play App Signing), and internal-test accounts all have lead time and are the classic cause of "sign-in works for me, not for testers." Sequence this **before** the code phase, not alongside it.
- **The `GamesService` seam as shipped is too small.** It has `signIn()` and `submitScore()` only, and its doc-comment explicitly declares cloud save out of scope for the seam ("it arrives as a second data source behind `ProgressRepository`"). v1.1 must extend it for achievements and UI intents while honoring that cloud-save routing decision.
- **`AdReason` needs `RETRY` — or the doc-comment's decision needs revisiting.** The enum comment says retry flows through `showInterstitial()`, but PROJECT.md lists "rewarded ads for hint/undo/**retry**." One of the two is stale; resolve explicitly during planning.
- **Ads corrupt the score unless the clock is paused.** The time-decay score is computed from elapsed time; a 30-second rewarded video or a mid-flow interstitial silently penalizes the player and pollutes leaderboards. Pause on ad-shown, resume on dismiss — and treat this as a testable requirement, not an implementation detail.
- **Ad gating is a takeaway, not an addition.** v1.0 shipped hints and undo free; `NoOpAdGateway` auto-grants. Any hard gate is a regression for existing players. The free-allowance + free-on-no-fill design is the mitigation.
- **Cloud save merges losslessly *because* levels are deterministic.** Level N is the same board for everyone, so per-level bests from two devices are directly comparable — that's what makes the no-dialog monotonic merge sound rather than lossy.
- **Never charge an ad for the hint's "no winning move" answer.** v1.0's hint honestly reports unsolvable positions; a rewarded ad that returns "there is no move" is the single fastest way to earn a 1-star review.
- **Audio must gate at one place.** With 11 events across gameplay/Results/UI, checking `soundOn` at each call site guarantees a missed one. Gate inside the audio layer.
- **Consent and ads are independent of Play Games.** They can be planned as separate phases; only the *timer-pause* and *Settings screen* concerns touch shared surfaces.

---

## MVP Definition

### Launch With (v1.1)

- [ ] **Play Console + AdMob configuration** — external blocker for everything else
- [ ] **PGS v2 auto sign-in, non-blocking, with signed-out graceful degradation** — the compliance and UX baseline
- [ ] **Extended `GamesService` seam** (achievements + UI intents) with the real `play-services-games-v2` implementation
- [ ] **7 leaderboards, submitted on level completion** — makes the deterministic ladder competitive
- [ ] **10–12 achievements** (defer #10–12 if the telemetry migration isn't worth it; backfill with progression tiers to stay ≥10)
- [ ] **Cloud save of ladder + per-level bests with deterministic monotonic merge** (snapshot metadata: description, played time, cover image)
- [ ] **UMP consent flow + Settings privacy-options entry point** — legally gates everything ad-related
- [ ] **Rewarded ads with free allowance + free-on-no-fill**, anchored on hint/undo and the rescue banner
- [ ] **Frequency-capped interstitials after the Results CTA**, suppressed in session 1 / first 3 levels / after losses
- [ ] **Game-clock pause across all ad presentation**
- [ ] **11-event SFX set on SoundPool, gated by `soundOn`, no audio-focus theft, pitch-varied jump**

### Add After Validation (v1.2)

- [ ] **Social-leaderboard-first UI / rank-context window** (+/- 10 spots) — the quality checklist's recommended engagement pattern; add once there's a population to rank
- [ ] **Additional achievement tiers with new content** — checklist recommends 40+ over a game's life; grow them, don't frontload
- [ ] **Cloud-syncing the in-progress snapshot** — only if users actually report losing mid-game state across devices
- [ ] **Rewarded-ad "double stars"/bonus placements** — test only after the free-allowance model's retention impact is measured
- [ ] **Ad mediation / additional demand sources** — once AdMob-only fill rate and eCPM are measured

### Future Consideration (v2+)

- [ ] **Custom leaderboard/achievement UI in Candy Clay style** — stock Google UI is a visual break from the redesign, but it's free, compliant, and maintained
- [ ] **PGS Friends service integration** — requires profile-comparison UI work and matters only with a social base
- [ ] **Background music with its own `musicOn` toggle (default off)**
- [ ] **Server-side reward verification** — needs a backend; only if real currency appears
- [ ] **Paid "remove ads" IAP** — the standard casual companion to ads, but adds Play Billing, entitlement persistence, and restore flows

---

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---|---|---|---|
| Play Console / AdMob configuration | — (enabler) | MEDIUM (lead time) | P1 |
| PGS auto sign-in + graceful signed-out | MEDIUM | LOW | P1 |
| UMP consent + privacy options entry | LOW (legal) | MEDIUM | P1 |
| Rewarded ads, opt-in, free-fallback | HIGH (revenue) | MEDIUM | P1 |
| Game-clock pause across ads | HIGH (correctness) | LOW | P1 |
| Interstitials with frequency policy | MEDIUM (revenue) | MEDIUM | P1 |
| Core SFX set + `soundOn` wiring | HIGH (feel) | MEDIUM | P1 |
| Leaderboards (7) | HIGH | LOW–MEDIUM | P1 |
| Achievements (10–12) | MEDIUM | MEDIUM (icons + Console + maybe migration) | P1 |
| Cloud save + monotonic merge | HIGH | MEDIUM–HIGH | P1 |
| Pitch-varied jump + star-pop SFX | MEDIUM (polish) | LOW | P1 |
| Re-submit bests on late sign-in | MEDIUM | LOW–MEDIUM | P2 |
| Session-1 / early-level ad suppression | MEDIUM (retention) | LOW | P2 |
| Per-session telemetry for skill achievements | MEDIUM | MEDIUM (schema migration) | P2 |
| Social leaderboard UI / rank window | MEDIUM | MEDIUM | P3 |
| Friends service | LOW | MEDIUM | P3 |
| Custom PGS UI in Candy Clay style | LOW | HIGH | P3 |
| Background music | LOW | MEDIUM | P3 |
| Remove-ads IAP | MEDIUM | HIGH | P3 |

**Priority key:** P1 = v1.1 launch · P2 = fast-follow · P3 = future

---

## Competitor / Convention Analysis

| Concern | Typical casual puzzle app | Typical peg-solitaire app on Play | Our approach |
|---|---|---|---|
| Sign-in | Auto PGS or silent, optional | Often none at all | Auto PGS v2, silent, never blocking; Settings fallback |
| Leaderboards | Score or level-based, often many | Rare | 7 curated boards; the ladder board is the headline |
| Achievements | 20–50, incremental tiers | Common but shallow ("play 100 games") | 10–12 designed around peg-solitaire mastery (perfect solve, hint-free, center peg) |
| Cloud save | Common; usually a conflict dialog | Rare | Deterministic monotonic merge — no dialog, no loss |
| Rewarded ads | Hints/extra moves/continue | Rare (mostly banner-supported) | Hint/undo/rescue, opt-in, free allowance, free-on-no-fill |
| Interstitials | Every 2–3 levels, often aggressive | Banner-heavy | Every 3rd win, 90 s floor, 3/session, none in session 1 |
| Banners | Very common | Very common | **None** — protects the board and the Candy Clay look |
| Sound | Full SFX + music, single mute toggle | Minimal or none | 11-event SFX set, no music, separate sound/haptics toggles, no audio-focus theft |

---

## Sources

**HIGH confidence (official Google documentation):**
- Play Games Services v2 sign-in / automatic authentication — https://developer.android.com/games/pgs/android/android-signin , https://developers.google.com/games/services/android/migrate-to-v2
- PGS quality checklist (10-achievement minimum, 4-in-first-hour, "don't block gameplay on sign-in", leaderboard submission cadence, saved-game metadata) — https://developer.android.com/games/pgs/quality
- Achievements concepts (standard/incremental/hidden, 2,000-point cap, icon requirements) — https://developer.android.com/games/pgs/achievements
- Leaderboards concepts (70-leaderboard cap, ordering fixed after publish, daily/weekly/all-time, tamper protection, score formats) — https://developer.android.com/games/pgs/leaderboards
- Saved Games (3 MB binary / 800 KB cover image, metadata, conflict guidance: "let the player decide", "don't automatically overwrite") — https://developer.android.com/games/pgs/savedgames
- UMP SDK Android setup (requestConsentInfoUpdate every launch, loadAndShowConsentFormIfRequired, canRequestAds, privacy-options entry point, debug geography, reset) — https://developers.google.com/admob/android/privacy
- Rewarded ads on Android (preloading, ~1 h ad expiry, reward only in the earned-reward callback, null on load failure) — https://developers.google.com/admob/android/rewarded
- AdMob recommended interstitial implementations (between levels, before Continue/Next, delay for tap-heavy apps) — https://support.google.com/admob/answer/6201350
- Google Play disruptive-ads policy (no ads at level start / before app load, 15 s closeable rule, explicit opt-in rewarded exemption) — https://support.google.com/googleplay/android-developer/answer/9857753
- Android audio focus / AudioAttributes (transient focus for short sounds; USAGE_GAME) — https://developer.android.com/media/optimize/audio-focus

**MEDIUM confidence (industry practice / secondary sources, cross-checked where possible):**
- Interstitial frequency norms for casual games (60–90 s interval, 2–3/session) — AdMob guidance plus https://monetizationguy.com/articles/mobile-game-ads-first-time-setup
- Rewarded fill-rate expectations (~85–95%) and preload norms — https://www.gamebizconsulting.com/blog/rewarded-video-ads-guide
- SoundPool vs MediaPlayer for short SFX, maxStreams as a CPU cap — Android SoundPool reference + https://www.geeksforgeeks.org/android/soundpool-in-android-with-examples/
- Achievement-popup rendering requires `setViewForPopups`/`setGravityForPopups` and Console-uploaded icons — https://developer.android.com/games/pgs/android/achievements , https://github.com/playgameservices/android-basic-samples/issues/316

**Project-internal (HIGH — authoritative for scope):**
- `.planning/PROJECT.md` (v1.1 milestone definition), `.planning/research/FEATURES.md` (v1.0 baseline, superseded by this file for Slice 2), `app/src/main/java/com/app/pegsolitaire/platform/{ads,games}/*.kt` (seam contracts), `data/SettingsRepository.kt` (`soundOn` unconsumed)

---
*Feature research for: v1.1 Slice 2 — Play Games Services v2, AdMob + UMP, game audio*
*Researched: 2026-08-27*
