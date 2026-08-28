# Requirements: Peg Solitaire (Android) — v1.1 "Slice 2 — Play Games, AdMob & Polish"

**Defined:** 2026-08-27
**Core Value:** Infinite, never-repeating, always-solvable Challenge levels that feel fair and satisfying.

**Milestone goal:** Turn the shipped offline game into a release-ready product by wiring real Play Games and AdMob SDKs behind the existing seams, adding sound, and paying down carried tech debt.

**Baseline:** v1.0 requirements (GAME/CLAS/CHAL/SCOR/ASST/PERS/NAV/SET/ONB/QUAL) are all validated — see `.planning/milestones/v1.0-REQUIREMENTS.md`. This document covers v1.1 only.

**Research basis:** `.planning/research/SUMMARY.md` (+ STACK/FEATURES/ARCHITECTURE/PITFALLS), 2026-08-27.

## Milestone Decisions (locked at definition time)

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Ads SDK | **GMA Next-Gen `ads-mobile-sdk` 1.4.0** | Google's preferred Android ads SDK since 2026-07-06; the CLAUDE.md `play-services-ads 23.6.0` pin is deprecated (sunsets 2027, blocks new releases). Verified compileSdk-36-safe. Only AdMob mediation is supported — the sole limitation, and this project uses no third-party mediation. Supersedes the CLAUDE.md pin. |
| Skill achievements | **Accept the Room migration** | Per-session telemetry (hint-free, undo-free, final peg position) unlocks the achievements that teach mastery rather than grind. Migration infra proven in v1.0. |
| Free assist allowance | **Per level** | N free hints/undos per level, then rewarded-for-more. Resets naturally, easy to reason about, and protects against the "they took away undo" review risk of hard-gating a shipped v1.0 feature. |
| Retry | **Stays free — `AdReason.RETRY` dropped** | Retry was free in v1.0; ad-gating the most frustrating moment is the top-cited churn driver. Resolves the contradiction between the seam doc-comment and PROJECT.md. Interstitial cadence (suppressed after losses) covers monetization instead. |
| Cloud save scope | **Bests + ladder only; resume snapshot stays local** | The in-progress snapshot is replay-validated local state; syncing a half-finished board creates genuine conflicts on the app's most fragile path. |
| Conflict resolution | **Deterministic monotonic merge, no dialog** | Progress is a join-semilattice (`highestLevel`/`stars`/`score` = max, `bestTime` = min) so two devices always merge losslessly. `RESOLUTION_POLICY_MANUAL` + merge in `:domain`. A dialog is reserved for the one genuinely unmergeable case (different PGS profile). |

## v1.1 Requirements

### Platform Foundation

- [ ] **PLAT-01**: An Activity-scoped bridge makes the foreground Activity available to the ad, consent, and Play Games implementations without leaking it into ViewModels or the `:domain` module
- [ ] **PLAT-02**: The `AdGateway` seam is extended to express a rewarded-ad offer, a user's accept/decline, and the distinction between "user declined" and "ad unavailable" — with the No-Op implementation still auto-granting so existing behavior is preserved until real ads land
- [ ] **PLAT-03**: The `GamesService` seam is extended to cover achievements and the stock Play Games UI intents, with the No-Op implementation degrading silently
- [ ] **PLAT-04**: The domain-purity CI gate additionally forbids `com.google.android.gms`/`com.google.android.ump` imports outside the platform layer
- [ ] **PLAT-05**: Release builds are minified with R8 and keep rules verified, so the added SDKs do not bloat the shipped APK
- [ ] **PLAT-06**: Ad unit IDs and Play Games IDs are build-config values, with test IDs in debug and real IDs in release — no ID literals in source
- [ ] **PLAT-07**: The full JVM test suite stays green and SDK-free after the real SDKs are added (auto-init providers isolated from tests)
- [ ] **PLAT-08**: App cold-start time is measured before and after SDK integration and stays within an agreed budget

### Sound

- [x] **SOUND-01**: Player hears a jump sound on every peg jump, pitch-varied so repeated jumps do not fatigue
- [x] **SOUND-02**: Player hears distinct feedback for peg removal, invalid tap (quiet, non-punishing), selection, and undo
- [ ] **SOUND-03**: Player hears a win sound and star-pop sounds sequenced in rising pitch to the existing Results celebration animation
- [ ] **SOUND-04**: Player hears button-press feedback on primary UI actions
- [x] **SOUND-05**: Turning off the existing `soundOn` setting silences all sound immediately, including already-queued one-shots
- [x] **SOUND-06**: Sound plays at system media volume and never interrupts or ducks the player's own music or podcast

### Consent & Privacy

- [ ] **CONS-01**: Consent information is refreshed each launch and any required consent form is shown before any ad is requested
- [ ] **CONS-02**: The ads SDK is initialized only after consent resolves, off the main thread, and no ad request occurs while consent is absent
- [ ] **CONS-03**: Player can change their privacy choices at any time from a Settings entry that appears only when consent rules require it
- [ ] **CONS-04**: When the consent flow fails, the app falls back to the previous session's status and simply serves no ads rather than blocking play

### Rewarded Ads

- [ ] **ADS-01**: Player is offered a rewarded ad in an explicit opt-in dialog that names the reward, and can decline — an ad never plays automatically
- [ ] **ADS-02**: Player gets a free allowance of hints and undos per level before any ad is offered
- [ ] **ADS-03**: Player receives the assist anyway, with a brief explanation, when an ad cannot be loaded or filled — the player is never dead-ended
- [ ] **ADS-04**: Player can accept a rewarded-ad offer from the "no winning move" rescue banner
- [ ] **ADS-05**: The reward is granted only when the ad reports the reward as earned
- [ ] **ADS-06**: A hint that reports "no winning move" is never charged against the allowance or an ad

### Interstitial Ads

- [ ] **INT-01**: Player sees an interstitial only after tapping the Results continue action, never during the win celebration or at level start
- [ ] **INT-02**: Interstitials are frequency-capped and suppressed during the first session and first levels of an install
- [ ] **INT-03**: Interstitials never follow a lost or abandoned level
- [ ] **INT-04**: A brief pause precedes an interstitial so a tap on the board cannot land on the ad

### Ad-Safe Gameplay

- [ ] **ADSF-01**: The game clock pauses for the full duration of any ad presentation and resumes afterwards, so an ad can never damage the player's time-decay score

### Play Games — Session

- [ ] **PGS-01**: Player is signed in to Play Games automatically at launch without gameplay ever being blocked or interrupted
- [ ] **PGS-02**: Every feature of the game works fully when sign-in is unavailable or declined, with social surfaces hidden rather than erroring
- [ ] **PGS-03**: Player can sign in manually from Settings when automatic sign-in did not happen

### Play Games — Leaderboards

- [ ] **PGSL-01**: Player's highest Challenge level reached is submitted to a ladder leaderboard on level completion
- [ ] **PGSL-02**: Player's cumulative Challenge score is submitted to its own leaderboard
- [ ] **PGSL-03**: Player's best score on each of the five Classic boards is submitted to that board's leaderboard
- [ ] **PGSL-04**: A failed submission never fails, blocks, or alters a solved level
- [ ] **PGSL-05**: Player's existing bests are submitted once when they sign in after having played offline
- [ ] **PGSL-06**: Player can open the Play Games leaderboard UI from the game

### Play Games — Achievements

- [ ] **PGSA-01**: Player can earn at least ten visible achievements, at least four of which are reachable within the first hour
- [ ] **PGSA-02**: Player earns skill achievements for solving without hints, solving without undo, and finishing with the last peg in the center
- [ ] **PGSA-03**: Player earns incremental progression achievements as their Challenge ladder and star totals grow
- [ ] **PGSA-04**: Player sees the in-app unlock popup when an achievement is earned
- [ ] **PGSA-05**: Player can open the Play Games achievements UI from the game
- [ ] **PGSA-06**: Per-session play telemetry needed for skill achievements is recorded through a schema migration that preserves all existing saved progress

### Cloud Save

- [ ] **CLOUD-01**: Player's per-level bests and Challenge ladder position are saved to the cloud and restored on a new device
- [ ] **CLOUD-02**: Progress from two devices merges without data loss and without asking the player to choose a save
- [ ] **CLOUD-03**: The cloud save carries a description, played time, and cover image so it is legible in the Play Games UI
- [ ] **CLOUD-04**: The in-progress board snapshot stays local and is never synced
- [ ] **CLOUD-05**: A cloud save or load failure degrades to local-only play without data loss or an error wall

### Tech Debt

- [ ] **DEBT-01**: The hint solver returns a winning move within its deadline for hard early positions on the large Classic boards, instead of folding to "no winning move"
- [ ] **DEBT-02**: Diamond-41's shipped starting position is proven solvable by a committed recorded-solution fixture replayed through the rules engine
- [ ] **DEBT-03**: The validation coverage gaps carried from phases 3 and 4 are backfilled
- [ ] **DEBT-04**: Security advisories WARN-02 and WARN-03 from the v1.0 audit are closed

### Release Readiness

- [ ] **REL-01**: The app declares ads, data safety, and target-audience information correctly in Play Console
- [ ] **REL-02**: Play Games and AdMob are verified working from a real signed track build, not just a debug build
- [ ] **REL-03**: `app-ads.txt` is published and verified for the AdMob account

## v1.2 Requirements

Deferred to a future release. Tracked but not in this roadmap.

- **Social-first leaderboard UI** with a rank-context window — needs a population to rank first
- **Additional achievement tiers with new content** — grow over time rather than frontload
- **Cloud-syncing the in-progress snapshot** — only if players actually report losing mid-game state across devices
- **Rewarded "double stars" / bonus placements** — test only after the free-allowance retention impact is measured
- **Ad mediation / additional demand sources** — once AdMob-only fill rate and eCPM are measured

## Out of Scope

| Feature | Reason |
|---------|--------|
| Banner ads on the gameplay screen | Steals board area (the board is the product), fights the Candy Clay layout, low RPM in long sessions, raises mis-tap risk |
| App-open ads | Play's disruptive-ads policy targets full-screen ads before app load; worst possible experience for a returning daily player |
| Interstitial after a loss or between win and celebration | Stacking an ad onto the most frustrating moment is the top casual-puzzle churn driver; interrupting the reward animation destroys the payoff scoring exists to deliver |
| Hard-gating all hints/undo behind ads | Removes a shipped v1.0 feature; guarantees "they added ads and took away undo" reviews |
| Ad-gating retry | Same takeaway problem, at the moment of maximum frustration |
| Forced sign-in / sign-in walls / repeated nags | PGS v2 removed sign-out and made auth automatic precisely to end this pattern; the quality checklist bans blocking gameplay on sign-in |
| Per-level Challenge leaderboards | Play Games hard-caps at 70 leaderboards; Challenge is infinite |
| Conflict-resolution dialog on every sync | Data merges deterministically; a modal each launch is pure friction and trains players to tap through data-loss prompts |
| Background music | Large asset, forces audio-focus decisions, most casual sessions are muted or played over the user's own audio. If it ever ships it needs its own `musicOn` toggle defaulting off |
| Custom in-app volume slider | Duplicates the system control; extra state, persistence, and QA for no gain |
| Harsh "wrong" buzzer on invalid taps | Invalid taps are frequent and exploratory; a punishing sound makes people mute the app entirely |
| Server-side reward verification | Needs a backend this project doesn't have; the reward is a hint in a single-player puzzle |
| Custom Candy Clay leaderboard/achievement UI | Stock Google UI is a visual break but free, compliant, and maintained — revisit in v2 |
| Paid "remove ads" IAP | Adds Play Billing, entitlement persistence, and restore flows — a separate milestone's worth of work |
| Third-party ad mediation | Unsupported by the chosen Next-Gen SDK and unnecessary at this scale |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

**Coverage:** 56/56 v1.1 requirements mapped to exactly one phase — no orphans, no duplicates.

| Requirement | Phase | Status |
|-------------|-------|--------|
| PLAT-01 | Phase 6 | Pending |
| PLAT-02 | Phase 6 | Pending |
| PLAT-03 | Phase 6 | Pending |
| PLAT-04 | Phase 6 | Pending |
| PLAT-05 | Phase 6 | Pending |
| PLAT-06 | Phase 6 | Pending |
| PLAT-07 | Phase 6 | Pending |
| PLAT-08 | Phase 14 | Pending |
| SOUND-01 | Phase 7 | Complete |
| SOUND-02 | Phase 7 | Complete |
| SOUND-03 | Phase 7 | Pending |
| SOUND-04 | Phase 7 | Pending |
| SOUND-05 | Phase 7 | Complete |
| SOUND-06 | Phase 7 | Complete |
| CONS-01 | Phase 9 | Pending |
| CONS-02 | Phase 9 | Pending |
| CONS-03 | Phase 9 | Pending |
| CONS-04 | Phase 9 | Pending |
| ADS-01 | Phase 9 | Pending |
| ADS-02 | Phase 9 | Pending |
| ADS-03 | Phase 9 | Pending |
| ADS-04 | Phase 9 | Pending |
| ADS-05 | Phase 9 | Pending |
| ADS-06 | Phase 9 | Pending |
| INT-01 | Phase 10 | Pending |
| INT-02 | Phase 10 | Pending |
| INT-03 | Phase 10 | Pending |
| INT-04 | Phase 10 | Pending |
| ADSF-01 | Phase 9 | Pending |
| PGS-01 | Phase 11 | Pending |
| PGS-02 | Phase 11 | Pending |
| PGS-03 | Phase 11 | Pending |
| PGSL-01 | Phase 12 | Pending |
| PGSL-02 | Phase 12 | Pending |
| PGSL-03 | Phase 12 | Pending |
| PGSL-04 | Phase 12 | Pending |
| PGSL-05 | Phase 12 | Pending |
| PGSL-06 | Phase 12 | Pending |
| PGSA-01 | Phase 12 | Pending |
| PGSA-02 | Phase 12 | Pending |
| PGSA-03 | Phase 12 | Pending |
| PGSA-04 | Phase 12 | Pending |
| PGSA-05 | Phase 12 | Pending |
| PGSA-06 | Phase 12 | Pending |
| CLOUD-01 | Phase 13 | Pending |
| CLOUD-02 | Phase 13 | Pending |
| CLOUD-03 | Phase 13 | Pending |
| CLOUD-04 | Phase 13 | Pending |
| CLOUD-05 | Phase 13 | Pending |
| DEBT-01 | Phase 8 | Pending |
| DEBT-02 | Phase 8 | Pending |
| DEBT-03 | Phase 8 | Pending |
| DEBT-04 | Phase 6 | Pending |
| REL-01 | Phase 14 | Pending |
| REL-02 | Phase 14 | Pending |
| REL-03 | Phase 14 | Pending |
