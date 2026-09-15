# Roadmap: Peg Solitaire (Android)

## Milestones

- ✅ **v1.0 Slice 1 — Playable Core** — Phases 1-5 (shipped 2026-08-26)
- 🚧 **v1.1 Slice 2 — Play Games, AdMob & Polish** — Phases 6-14 (in progress)

## Phases

<details>
<summary>✅ v1.0 Slice 1 — Playable Core (Phases 1-5) — SHIPPED 2026-08-26</summary>

- [x] Phase 1: Domain Foundation & Playable Board (5/5 plans) — completed 2026-06-28
- [x] Phase 2: Classic Mode, Scoring & Navigation (8/8 plans) — completed 2026-06-29
- [x] Phase 3: Challenge Generation & Difficulty Curve (5/5 plans) — completed 2026-07-01
- [x] Phase 4: Assist — Hint & Undo (6/6 plans) — completed 2026-07-12
- [x] Phase 5: Persistence, Resume & Settings (7/7 plans) — completed 2026-08-25

Full details: [milestones/v1.0-ROADMAP.md](milestones/v1.0-ROADMAP.md) · Audit: [milestones/v1.0-MILESTONE-AUDIT.md](milestones/v1.0-MILESTONE-AUDIT.md)

</details>

### 🚧 v1.1 Slice 2 — Play Games, AdMob & Polish (In Progress)

**Milestone Goal:** Turn the shipped offline game into a release-ready product by wiring real Play Games and AdMob SDKs behind the existing seams, adding sound, and paying down carried tech debt.

- [x] **Phase 6: Platform Foundation & Build Hardening** - Activity bridge, extended seams, R8, build-config IDs, purity/dependency gates, security advisories closed — zero player-visible change (completed 2026-08-27)
- [x] **Phase 7: Sound** - The 12-event SFX set consuming the already-persisted `soundOn` flag (completed 2026-08-28; planned as 11 events, grown to 12 by post-phase quick task 260828-vrr, which split peg pick-up and put-down into separate cues)
- [x] **Phase 8: Assist & Generator Quality Debt** - Hints tell the truth on hard early positions; every Classic board's solvability is proven (completed 2026-08-29)
- [x] **Phase 9: Consent & Rewarded Ads** - UMP consent, opt-in-first rewarded assists, fail-open on no-fill, clock frozen across every ad (completed 2026-09-02)
- [x] **Phase 10: Interstitials** - Frequency-capped, win-only interstitials at the Results continue action (completed 2026-09-02)
- [x] **Phase 11: Play Games Session** - One-time opt-in prompt, silent sign-in for opted-in players, full signed-out play, manual sign-in fallback (completed 2026-09-03)
- [x] **Phase 12: Leaderboards & Achievements** - 7 leaderboards, 10+ achievements, per-session telemetry migration (completed 2026-09-13)
- [ ] **Phase 13: Cloud Save** - Bests + ladder synced via Saved Games with deterministic monotonic merge
- [ ] **Phase 14: Release Readiness** - Play Console declarations, signed-track verification, `app-ads.txt`, cold-start budget

## External Prerequisites (start Day 1, not at release)

These have no code equivalent and multi-hour-to-multi-day latency. Start them in parallel with Phase 6 — deferring them to Phase 14 is the milestone's single biggest schedule risk.

| Item | Blocks | Lead time |
|------|--------|-----------|
| Play Console PGS project + published OAuth consent screen + credentials for **both** debug-keystore SHA-1 and Play App Signing SHA-1 + tester list | Phases 11, 12, 13, 14 | Hours to days (tester propagation) |
| AdMob account + rewarded/interstitial ad units created | Phases 9, 10, 14 | Hours |
| A **developer website** exists and is reachable over HTTPS | Phases 9 (REL-05), 14 (REL-03, REL-04) | **Longest lead item — nothing else here can start until it does.** Two separate release requirements now hang off it: `app-ads.txt` and the privacy policy. Does not exist yet. |
| `app-ads.txt` published on that website and linked in the store listing | Phase 14 (REL-03) | Up to 24h crawl after the site is live |
| Privacy policy authored and published on that website, disclosing advertising-ID collection and every third-party SDK receiving player data | Phases 9 (REL-05), 14 (REL-01, REL-04) | Hours to author; Play Console requires the URL for **every** app, and the Data-safety form (REL-01) must agree with what it says |
| UMP consent message configured in the AdMob UI | Phase 9 (CONS-01) | Minutes, but UMP renders nothing without it |
| 20 unique 512×512 achievement icons (one per achievement — see 12-OPERATOR-CHECKLIST.md) | Phase 12 (PGSA-01/04) | Art turnaround; popups silently fail to render without them |
| Play Console: Saved Games enabled on the PGS project | Phase 13 | Minutes |

## Phase Details

### Phase 6: Platform Foundation & Build Hardening

**Goal**: The app has the platform plumbing, build safety, and security posture every SDK integration depends on — with zero player-visible change.
**Depends on**: Nothing (builds on shipped v1.0 Phase 5)
**Requirements**: PLAT-01, PLAT-02, PLAT-03, PLAT-04, PLAT-05, PLAT-06, PLAT-07, DEBT-04
**Success Criteria** (what must be TRUE):

  1. The foreground Activity is reachable from platform-layer code and from nowhere else — no ViewModel and no `:domain` class can obtain one — so ads, consent, and Play Games all have the Activity they require. (PLAT-01)
  2. Player-facing behavior is byte-identical to v1.0: the extended `AdGateway` and `GamesService` seams still auto-grant and degrade silently, so hint, undo, and every existing surface behave exactly as they did. (PLAT-02, PLAT-03)
  3. `./gradlew check` runs green and offline with the new SDK artifacts on the graph — no auto-init ContentProvider executes in tests, and the purity gate fails the build on any `com.google.android.gms`/`com.google.android.ump` import outside the platform layer. (PLAT-04, PLAT-07)
  4. A release build is minified and resource-shrunk by R8 and still launches and plays, while every ad-unit and Play-Games ID resolves from a build-config value — test IDs in debug, real IDs in release, no ID literal anywhere in source. (PLAT-05, PLAT-06)
  5. Security advisories WARN-02 and WARN-03 from the v1.0 audit are closed with evidence committed to the repo. (DEBT-04)

**Plans**: 10 plans in 4 waves

Plans:

- [x] 06-01-PLAN.md — PLAT-02: sealed `RewardOutcome` seam, fail-open `Unavailable` call sites (wave 1)
- [x] 06-02-PLAN.md — DEBT-04/WARN-02: bound resume-snapshot coordinates at the decode boundary (wave 1)
- [x] 06-03-PLAN.md — PLAT-05: R8 full mode + `shrinkResources`, one keep rule, pre-SDK APK ledger (wave 1)
- [x] 06-04-PLAN.md — PLAT-03: `GamesService` session/achievements/UI-intent extension (wave 2)
- [x] 06-05-PLAN.md — PLAT-01/PLAT-04: `ForegroundActivityProvider` bridge + `PlatformBoundaryTest` gate (wave 2)
- [x] 06-06-PLAN.md — DEBT-04/WARN-03 + D-15: `ChallengeLevelBounds` consolidation and both clamp boundaries (wave 2)
- [x] 06-07-PLAN.md — PLAT-06: build-config ad/PGS IDs, unit-tested release-fail gate, no ID literals (wave 2)
- [x] 06-08-PLAN.md — PLAT-05/D-12: pre-SDK cold-start baseline + External Prerequisites kickoff (wave 2, manual)
- [x] 06-09-PLAN.md — PLAT-07/PLAT-04: SDK artifacts on the graph, manifest surface gate, negative boundary gate (wave 3)
- [x] 06-10-PLAN.md — PLAT-05/PLAT-07: `check --offline`, debug-signed release smoke test, doc corrections (wave 4, manual)

**Notes**: This is the dependency root of the milestone — `ForegroundActivityProvider` is required by ads, consent, *and* Play Games. Also capture the pre-SDK cold-start baseline here; Phase 14 measures against it (PLAT-08). Kick off every External Prerequisite above on day 1 of this phase.

### Phase 7: Sound

**Goal**: The game has a voice — every meaningful action makes a sound that respects the player's setting and the player's own audio.
**Depends on**: Nothing (SDK-free, Activity-free — runs in parallel with Phase 6)
**Requirements**: SOUND-01, SOUND-02, SOUND-03, SOUND-04, SOUND-05, SOUND-06
**Success Criteria** (what must be TRUE):

  1. Player hears a pitch-varied jump on every peg jump plus distinct feedback for peg removal, selection, undo, a quiet non-punishing invalid tap, and primary button presses — and the first tap of a session is never silent. (SOUND-01, SOUND-02, SOUND-04)
  2. Player hears the win sound and a rising-pitch star-pop sequence land in step with the existing Results celebration animation. (SOUND-03)
  3. Turning the existing `soundOn` setting off silences the game immediately, including one-shots already queued. (SOUND-05)
  4. Sound plays at system media volume, and a player listening to music or a podcast keeps hearing it — unducked, uninterrupted. (SOUND-06)

**Plans**: 6 plans

Plans:
**Wave 1**

- [x] 07-01-PLAN.md — Curate 11 CC0 candy-clay SFX into `res/raw`, record licence provenance, add the `audioFocusLiteralCheck` build gate

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 07-02-PLAN.md — `SoundPlayer` seam, pitch-ladder constants, `@Singleton SoundPoolPlayer` (media volume, no audio focus, in-player mute gate), Hilt binding, `LocalSoundPlayer`, startup preload

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 07-03-PLAN.md — Gameplay cues: derived same-peg run index, the three cue ordinals, and six `LaunchedEffect` cue composables on `GameScreen`
- [x] 07-04-PLAN.md — Results celebration: play-once latch plus the rising star-pop sequence in step with the existing stagger

**Wave 4** *(blocked on Wave 3 completion)*

- [x] 07-05-PLAN.md — Forward/dismiss button cues on navigational CTAs only, and the D-12 sound-enabled confirmation blip

**Wave 5** *(blocked on Wave 4 completion)*

- [x] 07-06-PLAN.md — Release-shrink retention proof and the blocking phase audit (podcast test, first-tap, aesthetic, licence sign-off)

### Phase 8: Assist & Generator Quality Debt

**Goal**: The hint system tells the truth on every shipped board, and every Classic board's solvability is proven rather than asserted.
**Depends on**: Nothing (pure `:domain` — runs in parallel with Phases 6-7)
**Requirements**: DEBT-01, DEBT-02, DEBT-03
**Success Criteria** (what must be TRUE):

  1. Player asking for a hint from a hard early position on European-37, Asymmetric-39, or Wiegleb-45 gets a real winning move within the unchanged 1.5s deadline instead of folding to the "no winning move" rescue banner — delivered by **a committed depth-4 opening book plus a bitboard solver with a working restart policy**, so the guarantee is a lookup over the first four moves and a budgeted search beyond it. The book's authoritative "no winning move" is a proof; a book miss falls through to the solver. (DEBT-01)
  2. Diamond-41's shipped **centre** start is proven to reach its fewest-pegs floor of **2** — by a committed 38-move line replayed through the unchanged `RulesEngine` in CI — **and** proven unable to reach 1, by the GF(4) colour invariant checked exhaustively over all 41 single-peg positions. "Solvable to one peg" was never achievable for this board; the earlier wording of this criterion predated the centre-start reality and is corrected here per D-06. (DEBT-02)
  3. The Nyquist validation coverage gaps carried from v1.0 phases 3 and 4 are backfilled and green in `check`. (DEBT-03)

**Plans**: 12 plans in 8 waves

Plans:
**Wave 1**

- [x] 08-01-PLAN.md — `BoardSpec.minimumPegs` + structural `footprint` key, `Boards.CLASSIC_SPECS`, Diamond-41 target 2 (DEBT-01/02)
- [x] 08-02-PLAN.md — Consolidate the three `symmetryPermutations` copies into `BoardCanonical`, add the chunked fast path + `CanonicalTest` property net (DEBT-01, Pitfall 4)
- [x] 08-03-PLAN.md — DEBT-03: audit and sign off the phase 3/4 validation contracts, cover duplicate-board rejection, update the milestone audit

**Wave 2** *(blocked on Wave 1)*

- [x] 08-04-PLAN.md — `SolverTables` precomputed bitboard tables + target-aware `Pagoda` (DEBT-01/02, Pitfall 1)
- [x] 08-06-PLAN.md — **A1 gate:** beam-search authoring, committed Diamond-41 floor line replayed to 2 pegs, exhaustive GF(4) optimality proof (DEBT-02, D-05)

**Wave 3** *(blocked on Wave 2)*

- [x] 08-05-PLAN.md — `Solver` bitboard rewrite, per-attempt node cap so restarts happen, re-anchored budget contracts (DEBT-01, D-07)
- [x] 08-07-PLAN.md — Per-board star bands derived from the board target + resume-path target preservation (DEBT-02, D-03/D-04, Pitfall 3)

**Wave 4** *(blocked on Wave 3)*

- [x] 08-08-PLAN.md — Opening-book format, depth-4 authoring run, five committed book resources + blocking generation review (DEBT-01, D-08)

**Wave 5** *(blocked on Wave 4)*

- [x] 08-09-PLAN.md — `OpeningBook` classpath lookup with miss / authoritative / hit separation, fail-closed parse, integrity gate (DEBT-01/02)

**Wave 6** *(blocked on Wave 5)*

- [x] 08-10-PLAN.md — Book-first hint pipeline behind one legality guard + gated `DepthSweepTest` asserting the D-08 universal claim (DEBT-01/02)

**Wave 7** *(blocked on Wave 6)*

- [x] 08-11-PLAN.md — Release-APK book retention proof, D-06 documentation trail, blocking on-device phase audit (DEBT-01/02/03)

**Wave 8** *(gap closure — blocked on Wave 7)*

- [x] 08-12-PLAN.md — Close CR-01: cancel the in-flight hint search on jump/undo, capture search inputs on the main thread, discard a result computed for a superseded board (DEBT-01)

**Notes**: DEBT-01 and DEBT-02 are one unit of work — the per-board target that makes the Diamond proof reachable is the same field the solver's goal test reads. Deliberately sequenced early so it absorbs the Play Console / AdMob propagation wait rather than being squeezed at release.

**Escalation resolved at planning time (2026-08-29).** Research measured that a strengthened solver alone cannot meet D-08 for Wiegleb-45: 0 of 76 depth-≤4 positions resolve inside the fixed 1.5s deadline even at ~15M nodes/s, a ~4-order-of-magnitude gap. The developer chose **"opening book + stronger solver"** — a committed depth-4 opening book (~621 canonical positions, ~100 KB) alongside the bitboard solver rewrite — rather than narrowing DEBT-01 to two boards. D-07's intent ("never make the player wait longer") is satisfied by book and solver together.

**Also settled by research:** Diamond-41's fewest-pegs floor is **F = 2**, not the "~5" carried in three in-code KDocs — and because F−1 = 1 is exactly what the GF(4) colour invariant excludes, D-05's optimality half needs no new search. Plan 08-06 is a hard gate: nothing anchors on F = 2 until a line regenerated through this project's own unchanged `RulesEngine` is observed to terminate at 2 pegs.

### Phase 9: Consent & Rewarded Ads

**Goal**: Player can consent to ads and *choose* to watch one for extra assists — never surprised, never dead-ended, never charged time.
**Depends on**: Phase 6
**Requirements**: CONS-01, CONS-02, CONS-03, CONS-04, ADS-01, ADS-02, ADS-03, ADS-04, ADS-05, ADS-06, ADSF-01, REL-05
**Success Criteria** (what must be TRUE):

  1. On a fresh install in a consent region, player sees the consent form before any ad is ever requested, the ads SDK initializes only after consent resolves, and player can reopen their privacy choices from Settings whenever consent rules require it. (CONS-01, CONS-02, CONS-03)
  1a. Player can open the privacy policy from Settings on **every** install, not only in consent regions — a separate, always-present entry from CONS-03's region-gated one, since Play requires the policy to be reachable regardless of where the player is. (REL-05)

  2. Player gets a free allowance of hints and undos per level; only once it is spent does an explicit opt-in dialog naming the reward appear, and declining leaves the game exactly as it was — an ad never plays on its own. (ADS-01, ADS-02)
  3. Player can accept a rewarded offer straight from the "no winning move" rescue banner, and the reward lands only when the ad reports it earned. (ADS-04, ADS-05)
  4. When an ad cannot load or fill, or the consent flow fails, player still gets the assist with a brief explanation and keeps playing — no error wall, no dead end. (ADS-03, CONS-04)
  5. The elapsed game clock is frozen for the full duration of every ad presentation and resumes on dismiss, so watching an ad can never cost score; and a hint that honestly reports "no winning move" costs neither an allowance nor an ad. (ADSF-01, ADS-06)

**Plans**: 11 plans in 7 waves
**UI hint**: yes

Plans:
**Wave 1**

- [x] 09-01-PLAN.md — `AssistAllowance` pure `:domain` policy (3 hints / 5 undos, refill-to-full) + resume-snapshot counters with fail-open decode (ADS-02, D-01/02/03/04/17)
- [x] 09-02-PLAN.md — Interface-first seam contracts: `AdGateway.rewardedOfferable`/`preloadRewarded`, `NoOpAdGateway` flipped to offerable (Pitfall 6), `AdConstants`, `ConsentGateway` + `AdsReadiness`

**Wave 2** *(blocked on Wave 1)*

- [x] 09-03-PLAN.md — `AdOfferDialog` themed opt-in dialog, `ClaySnackbarHost` (the app's first), and every Phase-9 string + the three new dimen tokens (ADS-01, ADS-03)
- [x] 09-04-PLAN.md — ADSF-01 clock freeze by ticker rebase, `resumeTickerIfNeeded` guard, virtual-time test class
- [x] 09-05-PLAN.md — `UmpConsentGateway` (every-launch consent info, one off-main `MobileAds.initialize`), `AdsModule` inside `..platform..`, `MainActivity` bootstrap, AdMob `APPLICATION_ID` manifest tag (CONS-01/02/03/04)

**Wave 3** *(blocked on Wave 2)*

- [x] 09-06-PLAN.md — ADS-06 inversion: hints search first and charge second, undo's empty-history guard hoisted, `GameUiState` offer/snackbar fields carried at all four install sites
- [x] 09-07-PLAN.md — Settings Privacy section: always-present policy row (REL-05) + consent-gated manage-choices row (CONS-03), `PrivacyLinks` constant. **Blocking human gate: the live policy URL (D-19)**

**Wave 4** *(blocked on Wave 3)*

- [x] 09-08-PLAN.md — The single assist funnel: free allowance, preload at exhaustion, opt-in offer, refill-then-spend on Granted/Unavailable, silent free grant when unservable, retry-proof persistence (ADS-01/02/03/04/05, CONS-04)

**Wave 5** *(blocked on Wave 4)*

- [x] 09-09-PLAN.md — `GameScreen`/`GameRoute` wiring: dialog gating, tick-driven clay snackbar, and the "no second offer surface" negative-contract test

**Wave 6** *(blocked on Wave 5 — the stub-first ordering gate)*

- [x] 09-10-PLAN.md — `RewardedAdSlot` + `AdMobAdGateway` (GMA Next-Gen suspend load, `earned` only from `OnUserEarnedRewardListener`) and the `PlatformModule` binding swap (ADS-01/03/05, CONS-02/04)

**Wave 7** *(blocked on Wave 6)*

- [x] 09-11-PLAN.md — Blocking AdMob-console gate (D-18) + itemised on-device verification of consent, both privacy rows, a real test rewarded ad and the clock freeze, committed as `09-DEVICE-EVIDENCE.md`

**Notes**: Internal ordering is non-negotiable — build and test the opt-in dialog against the still-in-place `NoOpAdGateway` stub **before** the real ad SDK is wired in. The stub's silent `true` auto-grant is exactly what currently hides the missing opt-in UI, and swapping in a real implementation as-is ships an AdMob policy violation. ADSF-01 (clock pause) is owned here and re-asserted as an acceptance criterion in Phase 10. REL-05 (the always-present privacy-policy entry) is placed here rather than in Phase 14 because it shares the Settings surface CONS-03 builds; it needs the policy URL to exist, so the developer website is a hard prerequisite for this phase, not just for release.

### Phase 10: Interstitials

**Goal**: Between-level interstitials a player barely notices and never resents.
**Depends on**: Phase 9
**Requirements**: INT-01, INT-02, INT-03, INT-04
**Success Criteria** (what must be TRUE):

  1. Player sees an interstitial only after tapping the Results continue action — never during the win celebration, never at level start. (INT-01)
  2. Player who loses, abandons, or restarts a level never sees an interstitial. (INT-03)
  3. Interstitials are frequency-capped and fully suppressed through the first session and the first levels of a new install, and the cap survives the app being killed. (INT-02)
  4. A brief quiescence pause precedes every interstitial so a tap aimed at the board or a Results button cannot land on the ad. (INT-04)
  5. The game clock stays paused for the whole interstitial presentation. *(carried acceptance criterion — ADSF-01, owned by Phase 9)*

**Plans**: 6 plans in 5 waves

Plans:
**Wave 1**

- [x] 10-01-PLAN.md — Pure-Kotlin cadence policy in `:domain`, TDD'd as a truth table (D-01..D-05, D-15)
- [x] 10-03-PLAN.md — `InterstitialAdSlot` + the evolved `AdGateway` seam + the real gateway and stub parity

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 10-02-PLAN.md — DataStore cadence persistence + `ProcessLifecycleOwner` session boundary (D-13, D-16)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 10-04-PLAN.md — Results eligibility (D-14), cadence decision, quiescence + ad-then-navigate sequencing

**Wave 4** *(blocked on Wave 3 completion)*

- [x] 10-05-PLAN.md — The INT-04 quiescence scrim and the deferred one-shot navigation in `ResultsRoute`

**Wave 5** *(blocked on Wave 4 completion)*

- [x] 10-06-PLAN.md — Full gate sweep, cross-cutting invariants, and the itemised device UAT checkpoint

**Notes**: The cadence policy is pure Kotlin and should be TDD'd as logic (min levels since last, min wall-clock gap, session cap, first-session/first-N suppression, persisted across process death) before it is wired to a live trigger. Retry stays free — `AdReason.RETRY` was dropped at requirements time.

### Phase 11: Play Games Session

**Goal**: Player is asked once whether to sign in to Play Games, opted-in players are signed in silently on every later launch, and everything works if they say no.
**Depends on**: Phase 6
**Requirements**: PGS-01, PGS-02, PGS-03
**Success Criteria** (what must be TRUE):

  1. Player launching the app for the first time sees a single dismissible "Sign in to Play Games?" prompt on Home; a player who accepts is signed in silently in the background on every later launch — no wall, no pause before they can start a level. (PGS-01)
  2. Player who declines, or whose device has no Play Games, can play every mode, every board, and every feature — social surfaces are hidden, never broken. (PGS-02)
  3. Player who was not signed in automatically can sign in from a Settings row that appears only when they are signed out. (PGS-03)

**Plans**: 4 plans in 3 waves

Plans:
**Wave 1**

- [x] 11-01-PLAN.md — Seam contracts (bootstrapIfOptedIn, displayName), D-17 opt-in persistence, shared FakeGamesService

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 11-02-PLAN.md — PlayGamesService real implementation, manifest APP_ID, PlatformModule/MainActivity wiring
- [x] 11-03-PLAN.md — Home opt-in prompt (D-15/D-16/D-18): dialog, sequencing, snackbar

**Wave 3** *(blocked on Wave 1 + Wave 2's 11-03 — shared strings.xml edits)*

- [x] 11-04-PLAN.md — Settings Play Games section (D-06/D-08/D-09/D-11/D-19): three-state row, manual sign-in

**Notes**: Can run in parallel with Phases 9-10 — it depends only on the Phase 6 Activity provider. PGS v2 has no sign-out and no sign-in wall; do not port v1 patterns. Sign-in is explicit opt-in (planning decision 2026-09-03): the Play Games SDK is never initialized until the player accepts the one-time prompt or taps the Settings row. 11-02 and 11-03 run in parallel (no file overlap); 11-04 is sequenced after 11-03 only because both edit strings.xml, not because of a semantic dependency. Emulator sign-in verification (D-04) has no Play Console project yet and is recorded as pre-release checklist debt, not a plan blocker.

### Phase 12: Leaderboards & Achievements

**Goal**: Player's progress becomes competitive and recognized — ranked on leaderboards, rewarded with achievements.
**Depends on**: Phase 11
**Requirements**: PGSL-01, PGSL-02, PGSL-03, PGSL-04, PGSL-05, PGSL-06, PGSA-01, PGSA-02, PGSA-03, PGSA-04, PGSA-05, PGSA-06
**Success Criteria** (what must be TRUE):

  1. Player completing a level has their highest Challenge level, cumulative Challenge score, and per-Classic-board best submitted to the right leaderboard, and can open the Play Games leaderboard UI from inside the game. (PGSL-01, PGSL-02, PGSL-03, PGSL-06)
  2. Player who played offline and then signs in has their existing bests submitted once, and a failed submission never blocks, fails, or alters a solved level. (PGSL-04, PGSL-05)
  3. Player can earn at least ten visible achievements, at least four of them reachable in the first hour, and sees the in-app unlock popup the moment one is earned. (PGSA-01, PGSA-04)
  4. Player earns skill achievements for solving hint-free, solving undo-free, and finishing with the last peg in the center — and every existing saved best, star total, and ladder position survives the schema migration that makes those achievements possible. (PGSA-02, PGSA-06)
  5. Player's growing ladder position and star total tick incremental achievements forward, and they can open the Play Games achievements UI from inside the game. (PGSA-03, PGSA-05)

**Plans**: 8 plans in 5 waves

Plans:
**Wave 1**

- [x] 12-01-PLAN.md — Room v1→v2 migration (D-06): `play_session` telemetry table, `MIGRATION_1_2`, exported `2.json`, progress-preserving `MigrationTest`
- [x] 12-02-PLAN.md — Pure `:domain` achievement kernel (D-01..D-05, D-11): `SolveTelemetry`, 20-value `AchievementId`, `AchievementThresholds`, `ProgressAggregate`, `AchievementRules`
- [x] 12-03-PLAN.md — `GamesService` seam reshape to Boolean submissions + `setAchievementSteps` (D-12), real `PlayGamesService` bodies, stock-UI Intents, two signed-in-only Settings rows (D-14/D-15)

**Wave 2** *(12-04 blocked on 12-03 — both edit `GamesConstants.kt`; 12-05 blocked on 12-02)*

- [x] 12-04-PLAN.md — 27-key build-config ID plumbing (D-17): `GamesIdResolution`, `verifyReleaseGamesIds`, `gamesIdLiteralCheck`, `GamesIds`
- [x] 12-05-PLAN.md — Per-session telemetry in `GameViewModel` (D-09): assist counters on the funnel success path, `lastPegOnTarget`, persisted snapshot fields, Results nav args

**Wave 3** *(blocked on 12-01, 12-02, 12-05)*

- [x] 12-06-PLAN.md — `recordResult` takes `SolveTelemetry` and writes both tables in one transaction (D-08); SQL aggregate projections build `ProgressAggregate` (D-03/D-04/D-07)

**Wave 4** *(blocked on 12-02, 12-03, 12-04, 12-06)*

- [x] 12-07-PLAN.md — `ProgressSubmitter` `@Singleton` (D-10/D-11/D-13): dirty-flag backfill, mutex-serialised pass, `GamesIds.of`, the `recordSolvedPlayOnce()` hook, `MainActivity.start()`

**Wave 5** *(blocked on all seven)*

- [x] 12-08-PLAN.md — Operator closure: Play Console checklist (7 leaderboards, 20 achievements ≤ 2,000 pts), Phase 12 device UAT items, ROADMAP icon row 10–12 → 20 (D-05)

**Notes**: PGSA-06 (per-session telemetry Room migration) must land **before or with** PGSA-02 — the skill achievements are unearnable without it, and the migration must be proven progress-preserving with a `MigrationTestHelper` test before any achievement wiring. Submission hooks into `ResultsViewModel.recordSolvedPlayOnce()` (which already has the process-death-safe exactly-once guard), **not** `GameViewModel` — `finishedEmitted` is reset by `onUndo()` and would double-submit. Aggregates must be SQL-backed, not an in-memory fold: Challenge reaches thousands of rows. 512×512 achievement icons must already be uploaded (see External Prerequisites).

### Phase 13: Cloud Save

**Goal**: Player's progress follows their account to a new device without ever asking them to choose a save.
**Depends on**: Phase 12
**Requirements**: CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-04, CLOUD-05
**Success Criteria** (what must be TRUE):

  1. Player signing in on a new device gets their per-level bests and Challenge ladder position back. (CLOUD-01)
  2. Player who played on two devices ends up with the union of both — highest ladder, best stars, best scores, fastest times — with no prompt and no loss. (CLOUD-02)
  3. Player browsing saves in the Play Games UI sees a legible entry carrying a description, played time, and cover image. (CLOUD-03)
  4. The in-progress board snapshot never leaves the device, and a cloud save or load failure quietly degrades to local-only play with no data loss and no error wall. (CLOUD-04, CLOUD-05)

**Plans**: 11 plans in 5 waves

Plans:
**Wave 1**

- [x] 13-01-PLAN.md — CLOUD-02: extract the monotonic merge into pure `:domain` `ProgressMerge` + its algebraic-law tests (the ROADMAP sequencing mandate)
- [x] 13-02-PLAN.md — CLOUD-05/D-12: `CloudSyncStore` push-pending marker and `CloudProfileStore` last-merged player id, with argued fail-open defaults
- [x] 13-03-PLAN.md — D-16: `13-OPERATOR-CHECKLIST.md` (Enable Saved Games) and 11-HUMAN-UAT tests 10-13 + the profile-swap gap

**Wave 2** *(blocked on Wave 1)*

- [x] 13-04-PLAN.md — CLOUD-01/04, D-05/D-07: `@Serializable` payload DTO, validating decode boundary, encode-measure-trim loop
- [x] 13-05-PLAN.md — CLOUD-01/02/04: additive DAO members, awaitable ladder merge, transactional `importProgress`/`exportProgress`/`resetAllProgress`
- [x] 13-06-PLAN.md — CLOUD-01/05: `CloudSaveStore` seam (bytes in/out), scoped constants, NoOp stub, recording fake

**Wave 3** *(blocked on Wave 2)*

- [ ] 13-07-PLAN.md — CLOUD-03, D-09/D-11: localized capped description and the cover rasterized from the launcher vectors
- [ ] 13-08-PLAN.md — CLOUD-01/02/03/05, D-01/D-02/D-12/D-14: `ProgressSubmitter` pass — profile gate, pull-once, Phase-12 catch-up, push, swap resolution

**Wave 4** *(blocked on Wave 3)*

- [ ] 13-09-PLAN.md — CLOUD-01/02/03/05: `SnapshotsClient` plumbing under `RESOLUTION_POLICY_MANUAL`, bounded conflict merge, metadata commit, binding flip
- [ ] 13-10-PLAN.md — CLOUD-02, D-13/D-14/D-15: Home-only profile-swap dialog, `HomeUiState` peers, never-stacked sequencing

**Wave 5** *(blocked on Wave 4)*

- [ ] 13-11-PLAN.md — phase gate (`check`, minified release + `missing_rules.txt`, containment greps) and the two-emulator device pass (manual)

**Notes**: The least reversible slice — it touches the persistence contract and needs two devices/accounts to test honestly. Use `RESOLUTION_POLICY_MANUAL` with the monotonic merge extracted into `:domain` and exhaustively unit-tested before any `SnapshotsClient` code is written; the default most-recently-modified policy silently deletes progress. **Research gap to close at planning time:** the Saved Games per-snapshot size cap is unverified — confirm it at runtime and design the payload capped/lossy-by-default rather than assuming an unbounded dump is safe.

### Phase 14: Release Readiness

**Goal**: The build a tester downloads from Play behaves exactly like the one on the developer's machine.
**Depends on**: Phases 6, 9, 10, 11, 12, 13
**Requirements**: PLAT-08, REL-01, REL-02, REL-03, REL-04
**Success Criteria** (what must be TRUE):

  1. Play Games sign-in, leaderboards, achievements, cloud save, and both ad formats are all verified working from a signed internal-testing-track **download** — not an `adb install` debug build. (REL-02)
  2. `app-ads.txt` is published on the developer website, linked from the store listing, and shows verified in AdMob. (REL-03)
  3. Play Console declares ads, data safety, and target-audience information that matches what the app actually does. (REL-01)
  3a. A privacy policy is live at a stable HTTPS URL and linked in the store listing, it names advertising-ID collection and every third-party SDK that receives player data, and its claims agree with the Data-safety answers given for REL-01 — the two are checked against each other, not written independently. (REL-04, REL-01)

  4. Cold-start time on the SDK-integrated build is measured against the pre-SDK baseline captured in Phase 6 and is within the agreed budget. (PLAT-08)

**Plans**: TBD
**Notes**: Two PGS OAuth credentials must both be verified — the debug/upload keystore SHA-1 **and** the Play App Signing SHA-1. They are different certificates, and only the second one is exercised by a real track download; this is the classic "works on my machine, fails from Play" failure. REL-04's policy text and REL-01's Data-safety answers are a single source of truth split across two systems: the ads SDK pulls in the `com.google.android.gms.permission.AD_ID` permission (recorded in Phase 6's build evidence), so advertising-ID collection must be declared in both and worded consistently. Write the Data-safety answers *from* the policy, not alongside it.

## Progress

**Execution Order:**
Phases execute in numeric order: 6 → 7 → 8 → 9 → 10 → 11 → 12 → 13 → 14

Parallelizable tracks (parallelization is enabled in config): Phases 7 and 8 have no dependency on Phase 6 and can run alongside it; Phase 11 depends only on Phase 6 and can run alongside Phases 9-10.

| Phase | Milestone | Plans Complete | Status | Completed |
| ----- | --------- | -------------- | ------ | --------- |
| 1. Domain Foundation & Playable Board | v1.0 | 5/5 | Complete | 2026-06-28 |
| 2. Classic Mode, Scoring & Navigation | v1.0 | 8/8 | Complete | 2026-06-29 |
| 3. Challenge Generation & Difficulty Curve | v1.0 | 5/5 | Complete | 2026-07-01 |
| 4. Assist — Hint & Undo | v1.0 | 6/6 | Complete | 2026-07-12 |
| 5. Persistence, Resume & Settings | v1.0 | 7/7 | Complete | 2026-08-25 |
| 6. Platform Foundation & Build Hardening | v1.1 | 10/10 | Complete   | 2026-08-27 |
| 7. Sound | v1.1 | 6/6 | Complete   | 2026-08-28 |
| 8. Assist & Generator Quality Debt | v1.1 | 12/12 | Complete   | 2026-08-30 |
| 9. Consent & Rewarded Ads | v1.1 | 11/11 | Complete   | 2026-09-02 |
| 10. Interstitials | v1.1 | 6/6 | Complete    | 2026-09-03 |
| 11. Play Games Session | v1.1 | 4/4 | Complete   | 2026-09-03 |
| 12. Leaderboards & Achievements | v1.1 | 8/8 | Complete   | 2026-09-13 |
| 13. Cloud Save | v1.1 | 6/11 | In Progress|  |
| 14. Release Readiness | v1.1 | 0/TBD | Not started | - |
