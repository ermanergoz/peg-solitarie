# Project Research Summary

**Project:** Peg Solitaire (Android)
**Milestone:** v1.1 "Slice 2 — Play Games, AdMob & Polish"
**Domain:** Bolting platform services (Play Games Services v2), monetization (AdMob + UMP consent), and game audio (SoundPool SFX) onto an already-shipped Kotlin/Compose Clean-Architecture game
**Researched:** 2026-08-27
**Confidence:** HIGH

## Executive Summary

v1.1 is not a from-scratch build — it is four SDK integrations (PGS v2, GMA ads, UMP consent, SoundPool) grafted onto a shipped, fully-offline, 293-test-green MVVM/Clean-Architecture game. All four research files converge on the same core insight: the v1.0 `AdGateway`/`GamesService` seams were designed correctly in *shape* (suspend functions, `@Singleton` Hilt bindings, repository-owns-truth) but were designed blind to one hard fact — every real Play Games and AdMob surface (`PlayGames.get*Client()`, `RewardedAd.show()`, UMP's consent calls) requires an `android.app.Activity`, and none of these seams carry one. Introducing a `ForegroundActivityProvider` (a `@Singleton` `WeakReference<Activity>` fed by `Application.ActivityLifecycleCallbacks`) is therefore the single dependency root for the entire milestone — Architecture and Pitfalls research independently arrive at the identical component and flag it as the highest-leverage, must-do-first decision.

The second load-bearing finding is that CLAUDE.md's ad-stack pin is stale and must be corrected as part of this work: `play-services-ads 23.6.0` is already deprecated (2026-02-17, sunsets 2027-06-30), and Google designated the **GMA Next-Gen SDK** (`ads-mobile-sdk`, currently 1.4.0) the preferred Android ads SDK on 2026-07-06. Research recommends adopting Next-Gen now, behind the existing `AdGateway` seam, with legacy `play-services-ads 25.4.0` retained as a documented one-class fallback if Next-Gen proves unstable or third-party mediation is ever needed. `play-services-games-v2` should be pinned to 22.0.0 (not the CLAUDE.md-guessed ~21.x). compileSdk 36 is verified safe for the entire new dependency graph (max floor is `minCompileSdk=34`) — no `resolutionStrategy` changes are forced today, but the graph must be *guarded* (a dependency-fixture diff in CI) because a routine SDK bump could introduce a compileSdk-37 requirement invisibly.

The third and most consequential shared risk is policy, not code: `NoOpAdGateway.requestRewarded()` currently auto-grants `true` with zero UI, so today's hint/undo controls fire with no opt-in prompt. Swapping in a real AdMob implementation *as-is* would silently ship an AdMob rewarded-ads policy violation (no affirmative opt-in) — flagged independently in FEATURES, ARCHITECTURE, and PITFALLS. The mitigation all three agree on is sequencing: build and test the opt-in dialog (Compose + ViewModel, testable against the stub) **before** any real ad SDK is wired in, and treat the game-clock pause across every ad presentation as a testable acceptance criterion, not an implementation detail — an unpaused clock silently corrupts the time-decay score that leaderboards depend on. Two Play Console/AdMob external dependencies have multi-day-to-24-hour latency and no code equivalent (PGS project + OAuth + tester publishing; AdMob `app-ads.txt` verification, which needs a developer website) — these should start on day one, in parallel with Phase 1 engineering, not be deferred to release.

## Key Findings

### Recommended Stack

The stack additions are narrow and layer cleanly onto the existing `libs.versions.toml`-governed, Compose BOM-pinned toolchain — no v1.0 pins change. Four new artifacts are added, all resolving from the already-declared `google()` repository, with **zero** new Gradle plugins or repositories required.

**Core technologies:**
- **GMA Next-Gen SDK** (`ads-mobile-sdk` 1.4.0) — rewarded + interstitial ads behind `AdGateway` — Google's designated preferred Android ads SDK since 2026-07-06; the legacy line is in maintenance mode; built-in ad preloaders fit the "hint must be instantly ready" requirement directly.
- **User Messaging Platform (UMP)** 4.0.0 — GDPR/consent gathering before any ad request — mandatory in consent regions; declare explicitly even though Next-Gen pulls it transitively, so the version stays catalog-governed.
- **Play Games Services v2** (`play-services-games-v2` 22.0.0) — sign-in, leaderboards, achievements, Saved Games (cloud save) behind `GamesService` — the only supported PGS line; pulls **zero** androidx, so it cannot disturb the project's carefully-pinned androidx graph.
- **kotlinx-coroutines-play-services** 1.11.0 — bridges GMS `Task<T>` → `suspend` — version-locked to the project's existing coroutines pin (1.11.0), so it can never drift.
- **`android.media.SoundPool`** (platform API, no new dependency) — short SFX (jump, peg pop, win, star pops, invalid tap) — purpose-built for pre-decoded, low-latency, overlapping one-shots; Media3/ExoPlayer is explicitly the wrong tool (>250ms buffering, streaming-oriented).

**Two blockers exist outside SDK choice and must be fixed regardless:** the app currently declares **no `INTERNET` permission** (it's fully offline) and has **no R8/minification configured at all** — both must be addressed this milestone, since an unminified release build with a ~5.5MB ads SDK plus its transitive graph (guava, tink, okhttp, cronet, work-runtime) is the single biggest avoidable APK-size risk.

**Stale pins to correct in CLAUDE.md:** `play-services-ads 23.6.0` → replace with the Next-Gen decision (see Roadmap Implications); `play-services-games ~21.x` → `play-services-games-v2 22.0.0`.

### Expected Features

**Must have (table stakes) — all P1 for v1.1 launch:**
- Silent, automatic, non-blocking PGS v2 sign-in with graceful signed-out degradation (never a sign-in wall)
- Extended `GamesService` seam (today only `signIn()`/`submitScore()`) to support achievement unlock/increment and leaderboard/achievement UI intents
- 7 curated leaderboards (1 Challenge ladder, 1 cumulative score, 5 per-Classic-board) — Challenge is infinite so per-level boards are a non-starter (PGS caps at 70)
- 10–12 achievements meeting PGS's hard minimum (≥10 visible, ≥4 reachable in the first hour); 3 of the proposed 12 need new per-session telemetry (hints used, undos used, final peg cell) — a domain + Room schema decision, not just a Console task
- Cloud save of ladder position + per-level bests via Saved Games, with **deterministic monotonic merge** (progress is a join-semilattice: `max` stars/score/level, `min` time) — explicitly NOT the in-progress board snapshot, which stays device-local
- UMP consent gathered every launch before any ad request, plus a persistent Settings privacy-options entry point
- Rewarded ads that are strictly opt-in with the reward named up front, preloaded, with graceful no-fill handling (**never dead-end the player** — grant the assist free on load failure)
- Frequency-capped interstitials only after wins, never after failures/restarts, with a tap-quiescence delay before showing
- Game-clock pause across every ad presentation (protects the time-decay score feeding leaderboards)
- An 11-event SFX set gated by the already-persisted (currently unconsumed) `soundOn` flag, with no audio-focus theft

**Should have (differentiators):** the deterministic-level-seeding-enabled ladder leaderboard is the product's core value made competitive; free-fallback rewarded gating (never a takeaway from what v1.0 shipped free); pitch-varied jump SFX (highest juice-per-line-of-code item); interstitial suppression in session 1 to protect D1 retention.

**Defer (v2+):** social leaderboard rank-context UI, cloud-syncing the in-progress snapshot (only if users actually report loss), background music, remove-ads IAP, custom Candy-Clay-styled PGS UI (stock Google UI is free/compliant/maintained — a v2+ nice-to-have, not a v1.1 gap).

**Explicit anti-features to avoid:** banner ads (steals board real estate), app-open ads (policy-banned FTUE killer), interstitials after a loss (top churn driver, collides with the rescue-banner rewarded offer), hard-gating all hints/undo behind ads (a takeaway from shipped v1.0 behavior), forced/repeated sign-in prompts (PGS v2 has no sign-out; auto sign-in is the whole point).

### Architecture Approach

The v1.0 layering (UI → ViewModel → platform seams → repositories → Room/DataStore, with a pure-JVM `:domain`) is preserved unchanged; this milestone adds implementations behind existing seams plus a small set of new shared infrastructure. The defining architectural move is the **`ForegroundActivityProvider`**, injected only into platform-layer implementations (never into ViewModels, never stored as a strong reference), which resolves the Activity-scoping mismatch shared by ads, PGS, and UMP. Pure-Kotlin policy logic (`InterstitialCadencePolicy`, `ProgressMerge`, `AchievementRules`) is deliberately kept out of Android-dependent code so it stays JVM-unit-testable — `ProgressMerge` in particular is an *extraction* of logic that already exists and is byte-identical-tested in `ProgressRepository.recordResultInTransaction`, reused for local writes, cloud pull, and Snapshot conflict resolution.

**Major components:**
1. `ForegroundActivityProvider` (`platform/activity/`) — shared Activity source for `AdMobAdGateway`, `PlayGamesService`, and `ConsentGateway`; the dependency root of the milestone.
2. `ConsentGateway` + `AdMobAdGateway` + `RewardedAdCache` + `InterstitialCadencePolicy` (`platform/ads/`) — UMP consent → `MobileAds` init → rewarded/interstitial behind the unchanged `AdGateway` interface, with an explicit fail-open policy (grant the assist free on no-fill/timeout/no-Activity, decline only on an honest user dismiss).
3. `PlayGamesService` + `GamesSession` + `CloudSaveDataSource` (`platform/games/`) — sign-in state, leaderboard/achievement submission, and Saved Games, feeding a new `CloudProgressSync` orchestrator in `data/` that is the **only** write path into `ProgressRepository` for cloud data (never writes Room/DataStore directly).
4. `SoundPlayer` (`platform/audio/`) + `LocalSoundPlayer` CompositionLocal — a single `@Singleton` SoundPool wrapper that owns the `soundOn` gate internally (deliberately not threaded through 3 separate UiStates), exposed to stateless composables the same way `LocalHapticFeedback` is.
5. `:domain/progress/ProgressMerge.kt` + `:domain/achievement/AchievementRules.kt` — pure-Kotlin, ArchUnit-gated policy functions, the highest-value JVM-testable slice of the milestone.

Leaderboard/achievement submission is deliberately hooked into `ResultsViewModel.recordSolvedPlayOnce()` (which already has a process-death-safe, exactly-once `SavedStateHandle` guard) rather than `GameViewModel`, because `finishedEmitted` is intentionally reset by `onUndo()` to allow a rescued board to re-finish — hooking submission there would double-submit on every undo/re-jump cycle.

### Critical Pitfalls

1. **The seams cannot host real SDKs without an Activity** — every PGS v2 client getter and every fullscreen ad show requires `Activity`, verified by direct `javap` inspection; no `Context` overload exists. Avoid by building `ForegroundActivityProvider` first, before any SDK code, as a `WeakReference`-backed singleton — never a stored strong `Activity` reference.
2. **`NoOpAdGateway`'s auto-grant hides a policy violation** — swapping in a real AdMob implementation with no opt-in dialog is an immediate AdMob "Rewards implementation / Full disclosure" violation. Build and test the opt-in dialog **against the stub**, before the real SDK lands, with a free-assist quota so early levels never show a video.
3. **Wrong ad unit IDs per build type risks non-appealable account suspension** — real IDs in debug/CI generate invalid traffic; Google explicitly states such suspensions are non-appealable. Use `buildConfigField` per build type (never a shared constants object) plus a release-blocking test asserting debug uses Google's test IDs.
4. **`SnapshotsClient`'s default conflict policy silently deletes progress** — `RESOLUTION_POLICY_MOST_RECENTLY_MODIFIED` can wipe a player's stars on a second-device sign-in. Use `RESOLUTION_POLICY_MANUAL` with the same monotonic merge function already proven in `ProgressRepository`, moved into `:domain` and exhaustively unit-tested before any Snapshots code is written.
5. **Auto-init ContentProviders (`MobileAdsInitProvider`, `PlayGamesInitProvider`) break both the 293-test suite and cold start** — they run before `App.onCreate()` completes, causing Robolectric flakiness/network calls in CI and measurable ANR risk in production. Remove both via `tools:node="remove"` in the debug manifest, initialize explicitly on `Dispatchers.IO`, and prove `./gradlew check` stays green offline in the same commit that adds the dependencies.

## Implications for Roadmap

The two independent phase-ordering proposals (Architecture's 8 slices, Pitfalls' 6 lettered phases) agree on substance and differ only in granularity. Reconciled below into one build order.

### Phase 1: Foundation — Activity bridge, build baseline, ad SDK decision
**Rationale:** This is the true dependency root. `ForegroundActivityProvider` is required by ads, PGS, *and* UMP; the dependency-graph gate and extended `DomainPurityTest` must exist before any SDK artifact lands so drift is caught structurally rather than by code review; the ad-SDK version decision (Next-Gen vs legacy) determines the threading model of everything downstream and should be recorded explicitly, not assumed.
**Delivers:** `ForegroundActivityProvider`; `App.onCreate()` bootstrap; `ads-mobile-sdk`/`play-services-games-v2`/UMP added to the version catalog with `INTERNET`/`ACCESS_NETWORK_STATE` permissions; R8/minification enabled (`isMinifyEnabled`, `isShrinkResources`); `DomainPurityTest` extended to forbid `com.google.android.gms..`/`com.google.android.ump..` outside `platform/`; a committed, CI-diffed resolved-dependency fixture guarding compileSdk 36; `buildConfigField`-based ad-unit-ID plumbing (values wired later, structure now); a written decision record for GMA Next-Gen 1.4.0 vs legacy 25.4.0.
**Addresses:** the "Play Console / AdMob configuration" external blocker (start in parallel — see External Lead-Time Items below); no player-facing feature yet.
**Avoids:** Pitfalls 1 (Activity-less seams), 2 (auto-init providers breaking tests/cold start), 12 (silent compileSdk regression), 14 (quality-gate erosion).

### Phase 2: Sound
**Rationale:** Zero SDK dependency, zero Activity requirement, zero network — genuinely independent of Phase 1's SDK work (only shares the "new platform-layer singleton" pattern, which it can pioneer at low stakes). Consumes a flag (`soundOn`) that already persists but is currently unconsumed. Highest player-visible payoff for the lowest risk in the milestone.
**Delivers:** `SoundPlayer`/`SoundPoolSoundPlayer`/`NoOpSoundPlayer`, `LocalSoundPlayer` CompositionLocal, the 11-event SFX set (pitch-varied jump, star-pop sequence, quiet non-punishing invalid-tap sound), pre-warmed on Home so the first gameplay tap is never silent.
**Uses:** `android.media.SoundPool` (platform, no dependency); wires into the existing `JumpHaptic`/haptics precedent in `GameScreen.kt`.
**Implements:** the `SoundPlayer` platform singleton pattern that owns its own `soundOn` gate (deliberately not threaded through 3 UiStates) — establishes the template Phase 3's ad-availability state and Phase 5's sign-in state both reuse.

### Phase 3: Consent + Rewarded ads (opt-in-first)
**Rationale:** Must follow Phase 1 (needs the Activity provider and the ad-SDK decision). Sequenced to build the opt-in dialog **against the still-in-place `NoOpAdGateway` stub first** — this is not optional ordering, it's the only way to avoid ever shipping a policy-violating rewarded implementation, since the stub's silent `true` auto-grant is what currently hides the missing UI.
**Delivers:** `ConsentGateway` (UMP sequencing: `requestConsentInfoUpdate()` every launch → `loadAndShowConsentFormIfRequired()` → `canRequestAds()` gate → `MobileAds.initialize()` on a background thread, `AtomicBoolean`-guarded); Settings privacy-options row; the two-step rewarded opt-in dialog (`pendingAdOffer`/`onAcceptAdOffer` on `GameUiState`, mirroring the existing `confirmRestart` pattern) with a free-assist quota; `RewardedAdCache` + fail-open policy (grant free on no-fill/timeout/no-Activity, decline only on honest dismiss); `AdMobAdGateway` real implementation.
**Implements:** Architecture Pattern 2 (callback-to-coroutine bridge, fail-open) and Pattern 3 (two-step opt-in) exactly as specified.
**Testable acceptance criterion (cross-cutting, called out explicitly per FEATURES/PITFALLS):** the elapsed game-clock **must pause** across every ad presentation (rewarded or interstitial) and resume on dismiss — write this as a test, not a manual check, since it silently corrupts the leaderboard-feeding time-decay score if missed.

### Phase 4: Interstitials
**Rationale:** Small once Phase 3's gateway exists; kept separate because the cadence policy is genuinely policy-sensitive (AdMob explicitly disallows >1 interstitial per 2 user actions, back-to-back interstitials, and ads on app open/exit) and should be unit-tested as pure logic before it's wired to a live trigger.
**Delivers:** `InterstitialCadencePolicy` (pure Kotlin, persisted state across process death via DataStore — min levels since last, min wall-clock gap, session cap, first-session/first-N-level suppression), `AdReason.RETRY` (resolving the doc-comment vs. PROJECT.md discrepancy — see Gaps), the Results-CTA trigger point with a ~300–600ms tap-quiescence delay.
**Addresses:** FEATURES' concrete ad frequency policy (every 3rd win, ≥90s gap, 3/session cap, never after a loss).

### Phase 5: Play Games session
**Rationale:** Needs only the Activity provider from Phase 1 — can run in parallel with Phases 3–4 if the roadmap wants two tracks. Low risk, no gameplay impact, fully playable signed-out.
**Delivers:** `PlayGamesService`, `GamesSignInState` StateFlow, silent auto sign-in at launch (`PlayGamesSdk.initialize()` in `App.onCreate`), manual sign-in fallback row in Settings (shown only when `isAuthenticated() == false`), extended `GamesService` seam (achievements + UI intents — today's seam has only `signIn()`/`submitScore()`).
**Avoids:** Pitfall 9 (writing v2 as if it were v1 — no sign-out control, no forced sign-in wall).

### Phase 6: Leaderboards + Achievements
**Rationale:** Needs a signed-in session (Phase 5) to verify end-to-end, but `AchievementRules` is pure and can be TDD'd ahead of the SDK wiring.
**Delivers:** `GamesIds` constants, `ProgressRepository.aggregate()` (SQL-backed, not an in-memory fold — Challenge can reach thousands of rows), `AchievementRules` in `:domain`, the `ResultsViewModel.recordSolvedPlayOnce()` submission hook, PGS leaderboard/achievement UI intent launching via `rememberLauncherForActivityResult`.
**Addresses:** the 7-leaderboard and 10–12-achievement configurations from FEATURES. **Decision needed:** whether to build the 3 skill-gated achievements requiring new per-session telemetry (no-hint, no-undo, center-peg) — this is a domain + Room schema migration, not a Console task; flag for planning if the migration cost isn't justified, with progression-tier achievements as the fallback to stay ≥10.

### Phase 7: Cloud save
**Rationale:** The hardest, least reversible slice — touches the persistence contract, needs two devices/accounts to test properly, needs Play Console's Saved Games explicitly enabled first, and has an unverified per-snapshot size cap. Sequenced last so `ProgressMerge` (extracted in Phase 6, reused here) is already proven.
**Delivers:** `CloudSaveDataSource` (`SnapshotsClient`, `RESOLUTION_POLICY_MANUAL`), `CloudProgressSync` orchestrator (the only write path into `ProgressRepository`), `exportProgress()`/`importProgress()`, a versioned+capped payload carrying `generatorVersion` (Challenge results only merge when it matches; Classic results and `challengeLevel` always merge, since they're generator-independent).
**Explicitly out of scope:** syncing the in-progress board snapshot (device-local by design).
**Avoids:** Pitfall 10 (default-policy data loss) via the monotonic merge; the decision to exclude the resume snapshot is deliberate, not an oversight.

### Phase 8: Release readiness
**Rationale:** Cross-cutting checklist that only makes sense once every SDK is integrated — Data safety and Contains-ads declarations, Families/target-audience decision, SHA-1 credential verification from an actual internal-testing track download (not `adb install`), manifest-merger diff review, cold-start macrobenchmark comparison against a pre-SDK baseline.
**Delivers:** Play Console declarations updated; two PGS OAuth credentials verified (upload/debug **and** Play App Signing key — a documented, easy-to-miss distinction); `app-ads.txt` verification confirmed; cold-start regression checked against a captured baseline.
**Avoids:** Pitfall 8 (SHA-1 mismatch surfacing only in production) and Pitfall 13 (Play Console declaration drift).

### Phase Ordering Rationale

- **Phase 1 is non-negotiable as the first phase.** It delivers zero user-visible feature, but 6 of Pitfalls' 15 catalogued pitfalls are prevented there, and three of them (seam shape, provider isolation, gate extension) become progressively more expensive to fix the more subsequent phases are built on the wrong foundation.
- **Phase 2 (Sound) is independent and can be pulled forward or run in parallel** with Phase 1 if the roadmap wants a visible-progress phase while the SDK plumbing is still being designed — there is no hard dependency either direction.
- **Phase 3 must build the opt-in UI before the real ad SDK lands**, even though both land in the "same" phase conceptually — this internal ordering is the mechanism that prevents ever shipping the policy-violating auto-grant state, and both FEATURES and PITFALLS treat it as non-negotiable.
- **Phases 4 and 5 can run in parallel** after Phase 1 — interstitials depend on the Phase 3 gateway existing, but PGS sign-in depends only on the Activity provider, so a two-track roadmap is viable here if velocity matters more than sequencing simplicity.
- **Phase 7 (cloud save) is deliberately last among the SDK phases** because it is the least reversible and needs the merge function already proven by Phase 6's usage.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 3 (Consent + Rewarded ads):** ad policy interpretation is nuanced (rewarded opt-in wording, disallowed-implementation edge cases) and the Next-Gen vs. legacy landscape may have moved further by execution time — re-verify the GMA SDK decision at phase-plan time, not just at this research snapshot.
- **Phase 7 (Cloud save):** Saved Games per-snapshot size cap and per-player snapshot count were **not verified** in any of the four research files (explicitly flagged LOW confidence in Architecture) — this must be confirmed before the payload schema is finalized, and the merge semantics need to be specified against the actual current Room schema at planning time.

Phases with standard, well-documented patterns (research-phase optional):
- **Phase 1 (Foundation):** the `ForegroundActivityProvider` pattern and dependency-graph gating are standard, well-precedented Android patterns with HIGH-confidence official-doc backing.
- **Phase 2 (Sound):** `SoundPool` is a stable, decade-old platform API; the failure modes are well catalogued.
- **Phase 5 (Play Games session), Phase 6 (Leaderboards + Achievements):** official PGS v2 docs are current and directly verified against the AAR (`javap`); pure-Kotlin cores (`AchievementRules`) are standard TDD candidates.

## External Lead-Time Items (start Day 1, not at release)

These have no code equivalent and multi-hour-to-multi-day latency; sequencing them alongside Phase 1 engineering (rather than deferring to release) is the single biggest schedule risk mitigation in this milestone:

1. **Play Console PGS setup** — create the Games Services project, publish the OAuth consent screen in Google Cloud (public audience is blocked until this is published), create OAuth credentials for **both** the debug keystore SHA-1 and the Play App Signing SHA-1 (two different certificates — a documented, frequent source of "works on my machine, fails from Play"), add yourself and testers (propagation takes hours), publish the PGS configuration **before** publishing the app.
2. **AdMob account + `app-ads.txt`** — register the app, create rewarded + interstitial ad units. `app-ads.txt` verification (mandatory since January 2025) requires a **developer website** with the file hosted and the URL entered in the Play Console store listing; crawl/verification takes up to 24 hours. **If no developer website exists yet, this is the single longest-lead item in the entire milestone.**
3. **Achievement icons** — 10–12 unique 512×512 icons required in Play Console; achievement popups silently fail to render without them (a common, hard-to-diagnose bug report).
4. **UMP consent message configuration** in the AdMob UI — the UMP SDK renders nothing if no message is configured there.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Verified by direct artifact inspection (POMs, `aar-metadata.properties`, `javap`) on Google Maven, not prose summaries; official deprecation/sunset dates cited directly. Only the Next-Gen "preferred SDK" announcement itself is MEDIUM-HIGH (blog body not directly retrievable, corroborated by the official deprecation page and trade press). |
| Features | HIGH for platform/policy behavior (official Google docs — PGS quality checklist, AdMob disallowed-implementations policy); MEDIUM for UX convention numbers (interstitial cadence, fill-rate expectations) drawn from industry practice rather than a single authoritative source. |
| Architecture | HIGH on SDK API surface and Activity requirements (official docs) and on local codebase facts (read directly from source); MEDIUM on the opinionated integration shapes (judgment calls with no single authoritative source, though internally consistent with Pitfalls' independent analysis); LOW specifically on Saved-Games size caps. |
| Pitfalls | HIGH for SDK mechanics, dependency-graph facts, and Play/AdMob policy (verified against official docs plus direct AAR inspection); MEDIUM where a mitigation is a codebase-specific judgment call (variant isolation strategy, exact Robolectric 4.16 ContentProvider behavior — flagged as "verify empirically, don't assume"). |

**Overall confidence:** HIGH

### Gaps to Address

- **Saved Games per-snapshot size limit is unverified** — not stated in any doc retrieved by any of the four researchers. Must be confirmed (`SnapshotsClient.getMaxDataSize()`, checked at runtime) before Phase 7's payload schema is finalized; design the payload as capped/lossy-by-default (full Classic results + aggregate + most-recent-K Challenge rows) rather than assuming an unbounded dump is safe.
- **`AdReason.RETRY` is unresolved between two project documents** — the existing seam's KDoc says retry flows through `showInterstitial()`, but PROJECT.md lists "rewarded ads for hint/undo/**retry**." Resolve explicitly during Phase 4 planning; this is a five-minute decision now and a confusing bug report if left ambiguous.
- **Achievements #10–12 (no-hint, no-undo, center-peg) require new per-session telemetry** not currently captured — a domain + Room schema migration. Decide during Phase 6 planning whether the migration is worth it, or substitute progression-tier achievements (e.g., "1,000 total jumps") to stay at the ≥10-visible minimum without a schema change.
- **Whether the free-assist quota is per-level, per-session, or per-day** is explicitly called out in Architecture as a design/monetization decision, not an architecture one — needs a product call before Phase 3 implementation, not during it.
- **Whether debug/test builds bind the No-Op gateways by build-flavor or by a Hilt test module** — Architecture flags this as a small but consequential DI decision to settle in Phase 1, since it determines whether the SDKs are even present on the test classpath (Pitfall 2's flavor-isolation mitigation vs. the tactical `tools:node="remove"` minimum).
- **Exact Robolectric 4.16 ContentProvider auto-init behavior** — Pitfalls flags this as needing empirical verification in the first Phase 1 spike rather than assumption either way, since Robolectric's provider handling has historically varied across versions.

## Sources

### Primary (HIGH confidence)
- Direct artifact inspection of `dl.google.com/dl/android/maven2` (POMs, `aar-metadata.properties`, AAR manifests, `javap` on published classes) — 2026-08-27, the empirical basis for all compileSdk-36 safety and dependency-graph claims across all four research files
- developers.google.com/admob/android/deprecation, /next-gen/quick-start, /next-gen/rel-notes, /next-gen/migration, /rewarded, /privacy, /quick-start, /app-ads
- developer.android.com/games/pgs/android/android-signin, /saved-games, /leaderboards, /migrate-to-v2, /achievements, console/setup, /troubleshooting
- developer.android.com/games/pgs/quality (10-achievement minimum, 4-in-first-hour, sign-in-blocking prohibition)
- support.google.com/admob/answer/6201350 (interstitial implementation), /9857753 (disruptive ads policy), /7313578 (rewarded policy), /6213019 + /3342099 (invalid traffic/suspension)
- developer.android.com/reference/android/media/SoundPool; developer.android.com/media/optimize/audio-focus

### Secondary (MEDIUM confidence)
- ads-developers.googleblog.com 2026-07-06 Next-Gen "preferred SDK" announcement (dates corroborated officially; performance percentages not independently verified)
- AdMob interstitial-frequency and rewarded-fill-rate industry-practice sources (monetizationguy.com, gamebizconsulting.com)
- github.com/robolectric/robolectric issue tracker (ContentProvider handling — flagged for empirical verification, not assumed)

### Project-internal (HIGH — authoritative for scope)
- `.planning/PROJECT.md`, `CLAUDE.md`
- Direct codebase reads: `AdGateway.kt`, `AdReason.kt`, `GamesService.kt`, `PlatformModule.kt`, `GameViewModel.kt`, `ResultsViewModel.kt`, `ProgressRepository.kt`, `ChallengeProgress.kt`, `SettingsRepository.kt`, `DataModule.kt`, `App.kt`, `MainActivity.kt`, `DomainPurityTest.kt`, `build-logic/src/main/kotlin/android-app.gradle.kts`, `gradle/libs.versions.toml`

---
*Research completed: 2026-08-27*
*Ready for roadmap: yes*
