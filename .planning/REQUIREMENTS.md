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
| Challenge clear rule (added 2026-11-01) | **Absolute peg bands: 1 peg = 3 stars, 2 = 2, 3 = 1; 4 or more = not cleared.** Every clear advances the ladder; a stuck board at 4+ pegs records nothing and stays on the board under the rescue banner. Classic unchanged. | Founder directive 2026-11-01 for 3.0.0. Before it, only a one-peg finish reached Results and advanced (phase-3 D-02/D-10), so the 2- and 1-star bands were unreachable in play (`.planning/todos/pending/2026-08-30-decide-whether-stuck-finishes-reach-results-1-2-stars.md`). Rules, knock-ons and migration: `docs/gdd.md` §3, §6, §11–§13 (GD-SB-01…GD-SB-17). |
| Challenge ladder shift (added 2026-11-01; re-scoped by Q-040 the same day) | **A difficulty shift of 30 levels: level 1 plays like the old level 31** (`LEVEL_OFFSET = 30`) — a 5-peg, 4-jump puzzle on the English cross; level numbers unchanged, nothing deleted or renumbered. Generator contract bumped to version 2; stored level numbers and results kept. **Deferred to 3.0.1 (Q-040):** the levels 1–4 fail-proof gate, the freeform intro at 10 with the even draw, the pool changes (one-peg copies, Diamond-41, widening) and the 3-star requirement on the skill achievements. | Founder: "remove first like 30 levels" — "i remove i meant shift" (Q-040, 2026-11-01). The old levels 1–25 were puzzles where, under the new rule, any single legal jump cleared the level. Recorded deviation from rules/puzzle-design.md ("first failure impossible before puzzle 5") for 3.0.0, approved by Q-040: `docs/gdd.md` §7.4, GD-SB-21. `docs/gdd.md` §7, §8, §12. |

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
- [x] **SOUND-03**: Player hears a win sound and star-pop sounds sequenced in rising pitch to the existing Results celebration animation
- [x] **SOUND-04**: Player hears button-press feedback on primary UI actions
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

- [x] **INT-01**: Player sees an interstitial only after tapping the Results continue action, never during the win celebration or at level start
- [x] **INT-02**: Interstitials are frequency-capped and suppressed during the first session and first levels of an install
- [x] **INT-03**: Interstitials never follow a lost or abandoned level
- [x] **INT-04**: A brief pause precedes an interstitial so a tap on the board cannot land on the ad

### Ad-Safe Gameplay

- [ ] **ADSF-01**: The game clock pauses for the full duration of any ad presentation and resumes afterwards, so an ad can never damage the player's time-decay score

### Play Games — Session

- [ ] **PGS-01**: Player is asked once, on first launch, whether to sign in to Play Games; a player who accepts is signed in automatically on every later launch, and gameplay is never blocked or interrupted by sign-in
- [ ] **PGS-02**: Every feature of the game works fully when sign-in is unavailable or declined, with social surfaces hidden rather than erroring
- [ ] **PGS-03**: Player can sign in manually from Settings when automatic sign-in did not happen

### Play Games — Leaderboards

- [ ] **PGSL-01**: Player's highest Challenge level reached is submitted to a ladder leaderboard on level completion
- [ ] **PGSL-02**: Player's cumulative Challenge score is submitted to its own leaderboard
- [ ] **PGSL-03**: Player's best score on each of the five Classic boards is submitted to that board's leaderboard
- [ ] **PGSL-04**: A failed submission never fails, blocks, or alters a solved level
- [ ] **PGSL-05**: Player's existing bests are submitted once when they sign in after having played offline
- [x] **PGSL-06**: Player can open the Play Games leaderboard UI from the game

### Play Games — Achievements

- [x] **PGSA-01**: Player can earn at least ten visible achievements, at least four of which are reachable within the first hour
- [ ] **PGSA-02**: Player earns skill achievements for solving without hints, solving without undo, and finishing with the last peg in the center
- [ ] **PGSA-03**: Player earns incremental progression achievements as their Challenge ladder and star totals grow
- [x] **PGSA-04**: Player sees the in-app unlock popup when an achievement is earned
- [x] **PGSA-05**: Player can open the Play Games achievements UI from the game
- [ ] **PGSA-06**: Per-session play telemetry needed for skill achievements is recorded through a schema migration that preserves all existing saved progress

### Cloud Save

- [ ] **CLOUD-01**: Player's per-level bests and Challenge ladder position are saved to the cloud and restored on a new device
- [ ] **CLOUD-02**: Progress from two devices merges without data loss and without asking the player to choose a save
- [ ] **CLOUD-03**: The cloud save carries a description, played time, and cover image so it is legible in the Play Games UI
- [ ] **CLOUD-04**: The in-progress board snapshot stays local and is never synced
- [ ] **CLOUD-05**: A cloud save or load failure degrades to local-only play without data loss or an error wall

### Tech Debt

- [x] **DEBT-01**: The hint solver returns a winning move within its deadline for hard early positions on the large Classic boards, instead of folding to "no winning move"
- [x] **DEBT-02**: Diamond-41's shipped starting position is proven solvable by a committed recorded-solution fixture replayed through the rules engine
- [x] **DEBT-03**: The validation coverage gaps carried from phases 3 and 4 are backfilled
- [ ] **DEBT-04**: Security advisories WARN-02 and WARN-03 from the v1.0 audit are closed

### Release Readiness

- [ ] **REL-01**: The app declares ads, data safety, and target-audience information correctly in Play Console
- [ ] **REL-02**: Play Games and AdMob are verified working from a real signed track build, not just a debug build
- [ ] **REL-03**: `app-ads.txt` is published and verified for the AdMob account
- [ ] **REL-04**: A privacy policy is published at a stable HTTPS URL, disclosing advertising-ID collection and every third-party SDK that receives player data, and is linked from the Play Console store listing
- [ ] **REL-05**: Player can open the privacy policy from inside the app, from a Settings entry that is always present (independent of CONS-03's consent-region-gated entry)

### Challenge star bands (founder directive 2026-11-01; ships in 3.0.0; spec `docs/gdd.md` §3, §6, §11)

- [ ] **CSB-01**: A Challenge board that ends with no legal move earns 3 stars with 1 peg left, 2 stars with 2 pegs, 1 star with 3 pegs — absolute peg counts, the same on every footprint
- [ ] **CSB-02**: A Challenge board that ends with no legal move and 4 or more pegs is not cleared: it earns no stars, records no result, does not advance the ladder, and stays on the board under the rescue banner with Undo, Restart and Home
- [ ] **CSB-03**: Every cleared Challenge level (1–3 stars) goes to Results, records its result, advances the ladder past it and offers Next level
- [ ] **CSB-04**: A level's stored best keeps the best of each field across plays — stars never decrease, time never rises, score never falls — so a 1-star clear improved later to 3 stars reads 3, and a later 1-star clear leaves a 3-star best untouched
- [ ] **CSB-05**: Player is told what the next star needs: Results names the peg count of the next band after a 1- or 2-star clear, and the Challenge stuck banner names the pegs that remained and that 3 or fewer clears the level
- [ ] **CSB-06** *[3.0.1 — deferred by Q-040]*: Skill achievements (hint-free, undo-free, speed) require a 3-star clear; first-clear, Challenge-level-5, ladder-tier and star-tier achievements and the ladder and score leaderboards count any clear. In 3.0.0 the predicates are as shipped (a 1-star clear counts), recorded in `docs/gdd.md` §11.2
- [ ] **CSB-07**: Classic is unchanged — its win test, stars, Results, records and achievements behave exactly as before this change
- [ ] **CSB-08**: Every generated Challenge board carries a one-peg perfect target whatever footprint it uses — in 3.0.0 this holds because the canonical pool is drawn only below the freeform intro, where it never reaches the Diamond-41 footprint, and the fuzz harness asserts it on every level; the by-construction pool copies are 3.0.1 (Q-040)

### Challenge ladder shift (founder directive 2026-11-01, re-scoped by Q-040; ships in 3.0.0; spec `docs/gdd.md` §7, §8, §12)

- [ ] **CLC-01**: The Challenge difficulty curve is shifted by 30 levels: level 1 deals what the previous curve dealt at level 31 (a 4-jump, 5-peg puzzle on the English cross) and every level N deals what the previous curve dealt at N + 30; level numbers, the ceiling and the solution-length cap are unchanged; nothing is deleted or renumbered
- [ ] **CLC-02** *[3.0.1 — deferred by Q-040]*: Levels 1–4 are fail-proof: every legal line from the start ends with 3 or fewer pegs, so a first-time player cannot finish uncleared before level 5. In 3.0.0 this is a recorded deviation from rules/puzzle-design.md approved by Q-040 (`docs/gdd.md` §7.4)
- [ ] **CLC-03** *[3.0.1 — deferred by Q-040]*: Freeform footprints become eligible at level 10 and from then on share the draw with the canonical pool, so the five canonical shapes keep appearing as the pool widens. In 3.0.0 the shipped intro (40) and the freeform takeover stand
- [ ] **CLC-04**: The generator contract version is bumped, the golden fixtures are regenerated under it, and the fuzz harness is green over the whole level space at the release seed count
- [ ] **CLC-05**: Stored progress is kept as it is — the ladder, per-level results, the cloud payload, achievements and leaderboards are never renumbered, lowered or deleted by the cut
- [ ] **CLC-06**: An in-progress Challenge snapshot stamped with the previous generator version resumes on its stored board with undo and elapsed time intact, is graded by the new clear rule, and Restart serves the level's new board

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

**Coverage:** 56/56 original v1.1 requirements mapped to exactly one phase — no orphans, no duplicates. The 14 added on 2026-11-01 (CSB-01…08, CLC-01…06) are mapped to a proposed Phase 16 (3.0.0) that the producer adds to `ROADMAP.md`, except CSB-06, CLC-02 and CLC-03, deferred to 3.0.1 by the founder's Q-040 and mapped to a proposed Phase 17 (gsd owns the phases; the game-designer owns the rule).

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
| SOUND-03 | Phase 7 | Complete |
| SOUND-04 | Phase 7 | Complete |
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
| INT-01 | Phase 10 | Complete |
| INT-02 | Phase 10 | Complete |
| INT-03 | Phase 10 | Complete |
| INT-04 | Phase 10 | Complete |
| ADSF-01 | Phase 9 | Pending |
| PGS-01 | Phase 11 | Pending |
| PGS-02 | Phase 11 | Pending |
| PGS-03 | Phase 11 | Pending |
| PGSL-01 | Phase 12 | Pending |
| PGSL-02 | Phase 12 | Pending |
| PGSL-03 | Phase 12 | Pending |
| PGSL-04 | Phase 12 | Pending |
| PGSL-05 | Phase 12 | Pending |
| PGSL-06 | Phase 12 | Complete |
| PGSA-01 | Phase 12 | Complete |
| PGSA-02 | Phase 12 | Pending |
| PGSA-03 | Phase 12 | Pending |
| PGSA-04 | Phase 12 | Complete |
| PGSA-05 | Phase 12 | Complete |
| PGSA-06 | Phase 12 | Pending |
| CLOUD-01 | Phase 13 | Pending |
| CLOUD-02 | Phase 13 | Pending |
| CLOUD-03 | Phase 13 | Pending |
| CLOUD-04 | Phase 13 | Pending |
| CLOUD-05 | Phase 13 | Pending |
| DEBT-01 | Phase 8 | Complete (2026-08-30) |
| DEBT-02 | Phase 8 | Complete (2026-08-30) |
| DEBT-03 | Phase 8 | Complete (2026-08-30) |
| DEBT-04 | Phase 6 | Pending |
| REL-01 | Phase 14 | Pending |
| REL-02 | Phase 14 | Pending |
| REL-03 | Phase 14 | Pending |
| REL-04 | Phase 14 | Pending |
| REL-05 | Phase 9 | Pending |
| CSB-01 | Phase 16 (proposed) | Pending |
| CSB-02 | Phase 16 (proposed) | Pending |
| CSB-03 | Phase 16 (proposed) | Pending |
| CSB-04 | Phase 16 (proposed) | Pending |
| CSB-05 | Phase 16 (proposed) | Pending |
| CSB-06 | Phase 17 (proposed, 3.0.1 — Q-040) | Deferred |
| CSB-07 | Phase 16 (proposed) | Pending |
| CSB-08 | Phase 16 (proposed) | Pending |
| CLC-01 | Phase 16 (proposed) | Pending |
| CLC-02 | Phase 17 (proposed, 3.0.1 — Q-040) | Deferred |
| CLC-03 | Phase 17 (proposed, 3.0.1 — Q-040) | Deferred |
| CLC-04 | Phase 16 (proposed) | Pending |
| CLC-05 | Phase 16 (proposed) | Pending |
| CLC-06 | Phase 16 (proposed) | Pending |
