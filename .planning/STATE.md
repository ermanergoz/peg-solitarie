---
gsd_state_version: 1.0
milestone: v1.1
milestone_name: Slice 2 — Play Games, AdMob & Polish
status: completed
stopped_at: Phase 12 complete and merged; next Phase 11 UAT tests 1-3, then Phase 13
last_updated: "2026-09-15T01:15:00.000Z"
last_activity: 2026-09-15
progress:
  total_phases: 9
  completed_phases: 7
  total_plans: 57
  completed_plans: 57
  percent: 78
---

# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-08-27)

**Core value:** Infinite, never-repeating, always-solvable Challenge levels that feel fair and satisfying.
**Current focus:** Phase 12 — leaderboards-achievements

## Current Position

Phase: 12 (leaderboards-achievements) — COMPLETE (8 of 8 plans)
Plan: 8 of 8
Status: Phase 12 COMPLETE — code, Play Console operator checklist and device UAT 4-9 all done (2026-09-15); merged to develop
Last activity: 2026-09-15 - Completed quick task 260915-3jd: debug builds honour a configured PGS_PROJECT_ID

Progress: [██████████] 100%

## Performance Metrics

**Velocity:**

- Total plans completed: 66 (all v1.0)
- v1.0 shipped: 5 phases, 31 plans, 68 tasks

**By Phase (v1.0):**

| Phase | Plans | Status |
|-------|-------|--------|
| 01 | 5 | Complete |
| 02 | 8 | Complete |
| 03 | 5 | Complete |
| 04 | 6 | Complete |
| 05 | 7 | Complete |

Per-plan durations archived in `.planning/milestones/v1.0-ROADMAP.md`.

**Recent Trend:**

- v1.1 not yet started — no trend data.

**By Plan (v1.1):**

| Plan | Duration | Tasks | Files |
|------|----------|-------|-------|
| Phase 07 P01 | ~35 min | 3 tasks | 13 files |
| Phase 07 P02 | ~50 min | 3 tasks | 9 files |
| Phase 07 P03 | ~45 min | 2 tasks | 8 files |
| Phase 07 P04 | ~35 min | 2 tasks | 6 files |
| Phase 07 P05 | ~40 min | 2 tasks | 8 files |

*Updated after each plan completion*
| Phase 07 P06 | ~15 min | 2 tasks | 4 files |
| Phase 12 P08 | 22 min | 3 tasks | 3 files |

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table. Full v1.0 decision log archived with the milestone.
Recent decisions affecting current work:

- [v1.1 requirements]: Ads SDK is **GMA Next-Gen `ads-mobile-sdk` 1.4.0** — the CLAUDE.md `play-services-ads 23.6.0` pin is deprecated (sunsets 2027) and is superseded. `play-services-games-v2` pins to 22.0.0, not the guessed ~21.x.
- [v1.1 requirements]: Free assist allowance is **per level**; retry stays free (`AdReason.RETRY` dropped) — ad-gating the most frustrating moment is the top churn driver.
- [v1.1 requirements]: Cloud save covers **bests + ladder only**; the replay-validated in-progress snapshot stays device-local. Conflicts resolve by deterministic monotonic merge (join-semilattice), no dialog.
- [v1.1 requirements]: Accept the Room migration for per-session telemetry (PGSA-06) so the three skill achievements ship.
- [v1.1 roadmap]: `ForegroundActivityProvider` (Phase 6) is the dependency root for ads, consent, **and** Play Games — nothing SDK-real lands before it.
- [v1.1 roadmap]: The rewarded opt-in dialog is built and tested against the `NoOpAdGateway` stub **before** the real ad SDK lands — the stub's silent auto-grant is what currently hides the missing opt-in UI, and shipping it as-is is an AdMob policy violation.
- [v1.1 roadmap]: Phases 7 (Sound) and 8 (Assist/Generator debt) are SDK-free and sequenced early to absorb Play Console / AdMob propagation lead time.
- [Phase 07]: The soundOn mute gate lives inside SoundPoolPlayer, not in any UiState — SOUND-05 requires stopping one-shots already in flight, which only the holder of the live stream ids can do
- [Phase 07]: D-07a: the jump pitch ladder is explicitly clamped to SoundPool's 0.5–2.0 range rather than left uncapped, so the one-octave ceiling is a tested contract instead of a silent engine surprise
- [Phase 07]: Audio is bound in its own AudioModule, never in PlatformModule — ..platform.. is defined as the Play/AdMob SDK + foreground-Activity seam and three ArchUnit rules depend on that definition
- [Phase 07]: The jump-pitch ladder step is **derived** from `moveHistory` on every jump and never stored — which makes D-08 (undo steps the ladder back down; replaying reproduces the pitch) true by construction rather than a second invariant to keep in sync
- [Phase 07]: D-05a is implemented by advancing the **same** selection ordinal on deselect, so the invalid-tap cue is structurally unreachable from the deselect branch rather than merely unused there
- [Phase 07]: No audio gate flag enters `GameUiState`/`GameScreen` — silencing stays inside the player (SOUND-05 must stop one-shots already in flight); a UI flag would give the mute two disagreeing sources of truth
- [Phase 07]: An invalid tap is now **observable state** (`rejectedTapTick`), so three `GameViewModelTest` "no-op tap" assertions were tightened from "no emission" to "no *gameplay* change" — the two are no longer the same claim
- [Phase 07]: The Results celebration latch is a SECOND SavedStateHandle key (CELEBRATED_KEY), not a reuse of the bests-record guard — the record guard is set only for a solved play, so sharing it would re-celebrate a stuck one-star finish on every entry
- [Phase 07]: StarPopSounds deliberately breaks ResultsCelebration.kt's animator-duration-scale symmetry (D-13) — audio that collapses its stagger to zero is one muddy chord, not a reduced-motion accommodation; the file header names the exception
- [Phase 07]: Button cues are wrapped at the LEAF composable (ModeTile, PrimaryAction, NeutralAction, BoardCard) but stay per-callsite — never on the shared Modifier.claySquish(), which is also on exactly the back-nav icons, hint toggle and Settings switches D-09 excludes
- [Phase 07]: D-10 requires no implementation — the pinned Compose stack emits no Android platform click tick, so there is no doubled sound to suppress and no soundEffectsEnabled API to disable; recorded as a // note at ResultsActions.kt and deferred to manual UAT in 07-06
- [Phase 07]: The D-12 blip fires on the user event and through the documented gate bypass, because the DataStore write has not round-tripped when the pop is due — a collector-side blip would also greet the player on every Settings open, which D-12 forbids
- [Phase 07]: A surviving resource id is a weaker claim than a surviving sound — the shrink proof follows each R.raw id to its SHORTENED packaged path and byte-compares against source, because isShrinkResources can keep an id while replacing its payload
- [Phase 07]: The audit's 'star pops too high' was fixed by transposing the whole triad down a rung (STAR_POP_BASE_RATE, derived from STAR_POP_STEP_SEMITONES so the two cannot drift), never by narrowing the semitone step — the step is ladder SHAPE, and narrowing it would flatten the rising climb D-15 exists to protect
- [Phase 07]: The Results star stagger stays ONE token (ClayAnim.STAR_STAGGER_MS, 120 -> 200) driving both the visual reveal and the audio pop spacing; the animations-off fallback was scaled by the same 5/3 rather than re-guessed, so the pop lands ON its star rather than near it
- [Phase 07]: Phase 7's manual audit was emulator-only and globally approved rather than itemised — T-07-04 (ducking) and T-07-16 (cold-start latency) have emulator-grade confirmation only, and the shrink-proof APK used placeholder ad IDs so it is a verification artifact, not a shippable build
- [Phase 08]: Diamond-41's fewest-pegs floor is **exactly 2**, proven both ways — a committed 38-move line replays through the unchanged `RulesEngine` to a dead end at (2,2)/(5,5), and all 41 single-peg positions are excluded by the GF(4) colour invariant; F is a fact, not a best-known, which is what lets star bands anchor on it
- [Phase 08]: The per-board target lives on `BoardSpec.minimumPegs`, never on a level id — the solver's goal test and the star bands read the same field, so they cannot drift
- [Phase 08]: The opening book is keyed structurally on `BoardSpec.footprint`, not on a board name or level id, so a resumed game still hits it — the resume boundary was the exact defect 08-07 exists to prevent
- [Phase 08]: A book **miss** falls through to the solver, but a book "no winning move" is **authoritative** and short-circuits it — the two outcomes are different objects, and conflating them is how a timeout gets dressed up as a proof
- [Phase 08]: The `Pagoda` prune is deliberately **weaker** for targets above one peg (provably vacuous at Diamond's two-peg target), and the committed book is the accepted mitigation rather than a stronger prune — Diamond's full start sat right on the 1.5s deadline without it
- [Phase 08]: DEBT-01's literal "every position within four moves" claim was **not** asserted, because it is false of the shipped artifacts (Wiegleb ships K=3, Diamond and Wiegleb carry approved gaps, English-33 ships four correctly-dead entries); what is machine-checked is the truthfulness property — the hint never claims a move, or a dead position, it did not prove
- [Phase 08]: Wiegleb-45's book ships at **K=3** with the file named `wiegleb45-k3.txt` so the artifact cannot misrepresent its own coverage — any check assuming a uniform `-k4` suffix will find four files, not five, and that is the artifacts being honest
- [Phase 08]: The book resources are plain **JAR** resources, not Android `res/` entries — `aapt2` cannot see them, which is precisely why `isShrinkResources` has no remit over them and they survive R8 byte-intact (proven in `08-BOOK-EVIDENCE.md`)
- [Phase 08]: `OpeningBook` fails **closed** — absent resources produce a MISS and a silent degradation to pre-book behaviour with no error at all, which is why release-APK retention is proven rather than assumed
- [Phase 12]: Phase 12's device tests append to Phase 11's 11-HUMAN-UAT.md rather than opening a 12-HUMAN-UAT.md — D-04 made that file the project's single accumulating device checklist, and a second one would split the pre-release pass in two
- [Phase 12]: The 20-achievement point budget is 1,130 of Play Games' 2,000 LIFETIME maximum, leaving 870 points of headroom; the four D-02 first-hour wins carry the four cheapest values (5/10/10/15) on purpose, so ROADMAP criterion 3 is readable off the budget rather than asserted
- [Phase 12]: The 27-row ID recording table holds key names and tick boxes only — never a value; its key column is deliberately un-backticked so the grep that selects the 27 recording rows cannot collide with the 27 roster mentions, and 'CgkI' = 0 is an acceptance criterion
- [Phase 12]: PGSA-04, PGSL-06, PGSA-05 and the PGSL-01/02/03 runtime claims stay [UNVERIFIED — D-04] until a Play Console project exists and 11-HUMAN-UAT tests 4-9 run on a device — the plan names the gap rather than producing a green check for it

### Resolved

- **[resolved by Phase 8 — was carried from v1.0 / 02-02] Diamond-41 D-19 proof** — the board ships the **canonical centre start**, and its solvability claim is now fixture-verified rather than literature-asserted. `domain/src/test/kotlin/com/app/pegsolitaire/domain/golden/solutions/fixtures/diamond41.txt` replays 38 moves through the unchanged `RulesEngine` to a two-peg dead end at (2,2)/(5,5); `DiamondFloorOptimalityTest` excludes all 41 single-peg positions by the GF(4) colour invariant, with positive and negative controls. Closed by plans 08-01 and 08-06. The earlier "off-center start, literature-asserted, one-peg solvable" wording was stale on every count and is corrected per D-06.
- **[resolved by Phase 8 — was carried from v1.0 / 04-02] Solver deadline envelope** — resolved, but **not uniformly**, and the distinction matters:
  - **European-37 and Asymmetric-39 are fixed by the strengthened solver alone.** Both folded to "no winning move" at their full starts before Phase 8; the bitboard rewrite with a geometrically-escalating per-attempt node cap returns a real winning move in **~82–123 ms** and **<1 ms** respectively, against the untouched 1500 ms ceiling (08-05).
  - **Wiegleb-45's early positions remain outside the deadline for any runtime search.** Research measured **0 of 76** depth-≤4 positions resolvable inside 1.5s even at ~15M nodes/s — a ~4-order-of-magnitude gap. The **committed opening book** is what makes those hints truthful, not a faster solver (08-08/08-09/08-10).
  - Post-book, the depth sweep records **zero solver fallbacks** on English-33, European-37 and Asymmetric-39 across their entire depth-4 frontiers, at ≤12 ms per call (08-10).

- **Confirm audio cues on physical hardware** (2026-08-28) — every Phase 7 audio checkpoint was emulator-only. SOUND-06 (no ducking) is structurally enforced by the `audioFocusLiteralCheck` build gate, so a device run is confirmation. SOUND-01 (first tap after cold start) is the real risk: `SoundPoolPlayer.play()` silently drops cues whose decode has not completed. See `.planning/todos/pending/2026-08-28-confirm-audio-cues-on-physical-hardware.md`.

### Blockers/Concerns

- **External lead-time items must start on Phase 6 day 1** — Play Console PGS project + dual-SHA-1 OAuth credentials + tester propagation, AdMob account, and especially `app-ads.txt` (needs a **developer website**; up to 24h crawl). No developer website yet = longest-lead item in the milestone. Achievement icons (10–12 × 512×512) and the UMP consent message also block silently.
- **Residual from DEBT-01 (stated plainly, not a regression)** — 444 positions across Diamond-41 (440 of 785 canonical-covered) and Wiegleb-45 (4 of 101) are outside their committed books, and on every one of them the solver returns `null` after the full 1.5s. Those positions are **player-visible**: a 1.5s spinner ending in the rescue banner. The gaps are header-declared in the artifacts and pinned by equality in `DepthSweepTest`, so any change to them is a visible diff. Closing Diamond's gap is the honest lever (it also removes `check`'s 11-minute cost); a K=4 Wiegleb upgrade is tracked in `.planning/todos/pending/2026-08-29-upgrade-wiegleb45-opening-book-to-depth-4.md`.
- **Saved Games per-snapshot size cap is unverified** (LOW confidence in research) — must be confirmed at runtime before the Phase 13 payload schema is finalized; design the payload capped/lossy-by-default.
- **App declares no `INTERNET` permission and has no R8/minification at all** — both must land in Phase 6 before a ~5.5MB ads SDK and its transitive graph reach a release build.

### Quick Tasks Completed

| # | Description | Date | Commit | Directory |
|---|-------------|------|--------|-----------|
| 260628-n4f | Deselect a peg only when that same peg is re-tapped | 2026-06-28 | 675de73 | [260628-n4f-deselect-only-on-retap](./quick/260628-n4f-deselect-only-on-retap/) |
| 260701-n9t | Phase 2 UI polish: Results/catalog/hint-toggle | 2026-07-01 | cca6cb3 | [260701-n9t-phase-2-ui-polish-remove-next-board-from](./quick/260701-n9t-phase-2-ui-polish-remove-next-board-from/) |
| 260828-tnw | Fix Results back-navigation with popUpTo | 2026-08-28 | 4fa6833 | [260828-tnw-fix-results-back-navigation-with-popupto](./quick/260828-tnw-fix-results-back-navigation-with-popupto/) |
| fast | Slow Results star celebration to zero-overlap pacing (stagger 200→260) | 2026-08-28 | 48bf34f | — (inline /gsd:fast, no plan dir) |
| fast | Drop the star triad below the sample's own pitch (base pow(-4)→pow(-8)) | 2026-08-28 | 724b7ae | — (inline /gsd:fast, no plan dir) |
| 260828-vb3 | Swap star pop for the CC0 freesound blip; stagger 260→500, pop 260→600 | 2026-08-28 | 86b68db | [260828-vb3-swap-star-pop-sound-for-cc0-blip-and-wid](./quick/260828-vb3-swap-star-pop-sound-for-cc0-blip-and-wid/) |
| 260828-vrr | Distinct peg pick-up / put-down cues (twelfth cue `sfx_deselect`) | 2026-08-28 | c25883e | [260828-vrr-distinct-peg-pick-up-and-put-down-sounds](./quick/260828-vrr-distinct-peg-pick-up-and-put-down-sounds/) |
| 260915-3jd | Debug builds honour a configured `PGS_PROJECT_ID` (was shipping `"0"`, so Play Games sign-in silently failed) | 2026-09-15 | f734f44, 8f2cbe3 | [260915-3jd-fix-debug-builds-emitting-play-games-pro](./quick/260915-3jd-fix-debug-builds-emitting-play-games-pro/) |
| fast | Record Phase 12 UAT results (tests 4–8 pass, 9 partial) and tick the operator checklist; fix its package name | 2026-09-15 | 306b71e | — (inline /gsd:fast, no plan dir) |

## Deferred Items

| Category | Item | Status | Deferred At |
|----------|------|--------|-------------|
| Scope | v1.2: social leaderboard rank-context UI, extra achievement tiers, cloud-syncing the in-progress snapshot, rewarded "double stars", ad mediation | Deferred to v1.2 | v1.1 requirements |
| Visual | POLI-01 "visual polish pass" — largely delivered by the Candy Clay redesign (merged to `develop` 2026-08-27, outside GSD planning); remaining polish in v1.1 is audio, not visual | Superseded | v1.1 milestone start |

## Session Continuity

Last session: 2026-09-13T19:51:33.020Z
Stopped at: Phase 12 planned (8 plans, verification passed)
Resume file: None

## Operator Next Steps

- Start the external prerequisites (Play Console PGS, AdMob account, `app-ads.txt` website) **now** — they run in parallel with engineering.
- Plan the first phase with `/gsd:plan-phase 6`.
