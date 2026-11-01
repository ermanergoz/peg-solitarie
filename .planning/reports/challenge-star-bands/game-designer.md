# game-designer report — challenge-star-bands
Date: 2026-11-01
Branch: feature/challenge-star-bands (committed by the producer)
Skills loaded: ergoz-soft:standards (role: game-designer), the-fool. The product snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin 1.0.0 rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, ip-policy, dispatch; plus the product's `.claude/rules/lessons.md` (0 active). `.planning/recall/RECALL.md` does not exist.
Ran in parallel with: home-continue-live-boards and the release-3.0.0 work, in other worktrees — not touched.
Ran on: claude-fable-5-1 at effort xhigh
Fan-out: none. Two haiku Explore helpers located files and quoted line numbers (this role has no Glob, Grep or Bash); every site they named that bears on the ruling I then read myself.
Loops: 0
Lessons applied: none

## Summary
- **The directive's first sentence is today's rule, not a request.** On `develop` a Challenge board advances only at `pegCount <= minimumPegs` (one peg); a stuck board never reaches Results and never records (`GameViewModel.kt:945, :967-983`). The change is the band and that every band advances: 1 peg = 3 stars, 2 = 2, 3 = 1, all cleared and advancing; 4+ stays the stuck board under the rescue banner, now with no star and a message that names the threshold. Absolute peg counts, Classic untouched.
- **"Remove the first levels" = cut fifty.** Level 1 now deals what level 51 dealt (6 pegs, a 5-jump line, two canonical footprints); levels 1–4 are fail-proof; freeform shapes start at level 10 and are mixed with the canonical pool (today they take over completely from level 40, so four of the five canonical footprints have never appeared in Challenge). Generator contract bumped to version 2; stored level numbers are kept, never renumbered; old resume snapshots resume on their stored board.
- Wrote the product's first `docs/gdd.md` (tech-design §16 row 5 owed it to this role) with rules R1–R11, state model, numeric difficulty levers, 18 designer decisions, every invariant mapped to a named test, 45 Given/When/Then acceptance criteria and the full copy list; added CSB-01…08 and CLC-01…06 to `.planning/REQUIREMENTS.md`.
- One non-blocking founder question drafted below (Classic alignment); one architecture question for the tech-lead (the old 2.0 app's on-disk data); nothing blocks the build.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-stars/docs/gdd.md` — the GDD of record (new, 896 lines). §1 the reading of the directive; §3 rules; §6 scoring; §7 the cut with worked values; §8 generator invariants and gates; §11 knock-ons (ribbon, achievements, leaderboards, cadence, cloud, telemetry); §12 migration; §13 decisions GD-SB-01…18 and the rejected list; §14 invariant → test; §15 acceptance criteria SB-D1…D12, SB-V1…V7, SB-U1…U4, SB-P1…P2, SB-Q1…Q11; §16 copy contract; §17 roadmap constraints.
- `/Users/ermanergoz/Desktop/PegSolitarie-stars/.planning/REQUIREMENTS.md` — two Milestone Decisions rows; sections "Challenge star bands" (CSB-01…08) and "Challenge ladder cut" (CLC-01…06); traceability rows mapped to a proposed Phase 16; the coverage line corrected.
- `/Users/ermanergoz/Desktop/PegSolitarie-stars/.planning/reports/challenge-star-bands/game-designer.md` — this report.
- No file under `app/`, `domain/`, `docs/ux-spec.md`, `docs/tech-design.md`, `ROADMAP.md`, `FOUNDER.md` or `STATUS.md` was changed. Nothing was committed: this role has no Bash and is prohibited from committing or spawning a helper to commit; the producer commits the three files above with `docs(design): …`.

## Checks
I have no Bash; every check is a read.
- `Read docs/gdd.md` (two pages, lines 1–896, all of it) — pass: no "(filled below)" marker remains; 17 sections, none empty; every invariant row in §14 names a test; every difficulty lever in §7 is a number.
- `Read .planning/REQUIREMENTS.md` after the edits (the four edited regions) — pass: 14 new ids, prefixes CSB/CLC collide with none of the 40-odd families in use (helper 2's inventory: PLAT, SOUND, CONS, ADS, ADSF, INT, PGS, PGSL, PGSA, CLOUD, DEBT, REL, GAME, CLAS, CHAL, SCOR, ASST, PERS, NAV, SET, ONB, QUAL, POLI, WARN, UX, TB, GD-HT, AC, R, Q, D, F, B, LN, LS, CV, C, FU, CR …).
- Arithmetic of the cut, worked by hand against `DifficultyTiers.forLevel` (`DifficultyTiers.kt:25-57`, `Curve` constants `:80-138`): for visible levels 1, 10, 26, 51, 101, 151, 626 and 10,000 the old and new (`fromStart + 50`) solution length, pool size, starting pegs and multiplier match the table in gdd §7.1 — pass.
- Claims established by reading, with the exact reference:
  - `Won` iff `pegCount <= spec.minimumPegs`, `Stuck` otherwise; only `Won` emits the finish, clears the snapshot and calls `advancePast` — `GameViewModel.kt:924-948, :960-1011, :1698-1703`.
  - `advancePast(N)` = `mergeLadder(N + 1)` = `max(current, N + 1)` in one DataStore edit; the only key is `challenge_current_level` — `ChallengeProgress.kt:23, :106-117, :166-171`. There is no `highestLevelReached` key (CLAUDE.md and the 2026-06-28 spec are stale on this).
  - Results: `isSolved = pegsRemaining <= minimumPegs`; record, headline and `showNextLevel` read it; only solved plays record and submit; every Results entry celebrates; eligibility = tier != STUCK — `ResultsViewModel.kt:182, :226-234, :486-511, :575-621`; `ResultsUiState.kt:103-112`; `ResultsActions.kt:56-70`.
  - Bests merge: read-modify-write, `stars = max`, `bestTimeMs = min`, `bestScore = max` — `ProgressRepository.kt:412-452` (helper-quoted) and `ProgressMerge.kt:42-50`; `LevelResultDao.kt` aggregates (`:59-109`).
  - Stars today: `ScoreCalculator.stars` ≤ floor → 3, ≤ floor+2 → 2, else 1 — `ScoreCalculator.kt:45-53`, `ScoreParams.kt:26, :38, :57`.
  - Generator: reverse walk from one peg; `pickFootprint` tries freeform first at `level >= 40` and `FreeformFootprints.build` keeps at least one candidate — `LevelGenerator.kt:207-252`, `FreeformFootprints.kt:38-59`; the canonical pool holds `Boards.Diamond41` with its floor of 2 — `DifficultyTiers.kt:64-72`; `GENERATOR_VERSION = 1`, `MAX_CHALLENGE_LEVEL = 10_000`, `DEDUP_WINDOW = 8`, `MAX_ATTEMPTS = 1_000` — `GeneratorConstants.kt:21, :29, :42, :81`.
  - Achievements: predicates and tiers — `AchievementRules.kt:65-121`; the skill EXISTS flags carry no star filter — `PlaySessionDao.kt:55-73`; the roster and thresholds — `12-OPERATOR-CHECKLIST.md:127-146`, `AchievementThresholds.kt` (helper-quoted).
  - Resume: the blob stores the initial board and history, never "regenerate N"; `generatorVersion` is a provenance stamp — `ResumeSnapshotDto.kt:7-15, :99-115`.
  - Banner and strings — `NoWinBanner.kt` (all), `strings.xml:160-210`; the rescue banner is shared by both modes and both messages.
  - The 2026-08-30 pending todo records the unreachable 1–2 star bands — read in full.
  - Spec of record for stars and the ladder — phase-2 CONTEXT D-01…D-08 and phase-3 CONTEXT D-01…D-11, read in full; phase-12 CONTEXT D-01…D-17 (`:20-59`).
  - Release context: 3.0.0 is the store's first sight of this codebase; the listing's users run a different application — `14-RELEASE-PLAN.md` §1, `CHANGELOG.md:29-35`.
- **Not run:** `./gradlew check`. This task changes no code and no Check input; the three files are docs and planning.

## The-fool pass
Modes chosen by me (no AskUserQuestion in this role): a pre-mortem on the clear rule and an evidence audit of the directive's reading.
- *"You read 'exactly one peg' away."* Evidence audit: the sentence matches the shipped code line for line (`statusFor`, D-02, D-10); the "But it should be like this" that follows would be redundant if it did not change that. Conceded: it is a reading, so it is recorded as GD-SB-01 with the evidence, and the founder sees the result at Gate 2.
- *"Fifty is arbitrary; the founder may have meant five."* Partly conceded: it is one constant and the founder can move it. Not conceded that it is arbitrary — under the new rule any single jump cleared levels 1–25, two jumps 26–50; fifty is the first level where a one-star clear needs three real jumps and the pool holds two shapes (gdd §7.2).
- *"A stuck board at four pegs on level 5 is harsher than today's one-star."* Audit says no: today a four-peg stuck board also never advances and never records (it shows a banner); the only thing it loses under the new rule is a star it was never actually awarded in play. Levels 1–4 cannot be failed at all.
- *"Players will farm one-star clears to climb the ladder leaderboard."* Conceded as a design property, not a defect: the founder's bands make three pegs a pass, the ladder is the ladder, and the skill achievements are gated to three stars so the awards that say "solve" still mean it.
- *"Three generator changes in a release week (offset, fail-proof gate, freeform mix) is scope creep."* The offset alone already re-rolls every board and forces the contract bump, the fixture regeneration and the 100,000-level fuzz; the other two ride that same run. The mix fixes a dead lever the cut would otherwise make worse (four canonical footprints unreachable); the gate makes a studio rule true. Each is a named constant and a named test. Accepted, with the order of work in gdd §17 so step 2 is one commit.
- *"The old 2.0 app's users will update and hit this app's Room schema."* Out of my scope and real; raised to the tech-lead below rather than guessed at.

## Open issues / risks
1. **Proposed founder question, non-blocking — for the producer to queue** (rules/reporting.md format):

   ```
   ## Q-037 — Should Classic use the same star bands as Challenge?   (open)
   Date: 2026-11-01    Asked by: game-designer
   Context: You asked for Challenge to count 3/2/1 pegs as 1/2/3 stars and to clear on any of them. Classic today still
   needs the board's proven floor to count at all (one peg, or two on Diamond-41): a finish at two or three pegs above it
   is a banner, never a result. Aligning Classic would also make its five "Cleared" achievements reachable at one star.
   Options: 1) Classic unchanged in 3.0.0, as directed; revisit in 3.1  2) Classic uses the same bands relative to each
   board's floor: floor = 3 stars, floor+1 = 2, floor+2 = 1, any of them clears and records  3) Classic uses absolute
   bands like Challenge (impossible on Diamond-41, whose floor is two — not recommended)
   Recommendation: game-designer picks 1 for 3.0.0 (your words name Challenge only, and the release is cut) and 2 for 3.1.
   Blocks: nothing
   Answer:
   ```
2. **Tech-lead question (architecture, not decided here):** does 3.0.0 open a Room database or DataStore file whose name the founder's earlier 2.0 application (same `applicationId`) also wrote? If so an update from the store could meet a foreign schema with no migration and no destructive fallback (`DatabaseModule.kt:52` registers only `MIGRATION_1_2`). Outside this ruling, inside "nobody gets a broken resume".
3. **Tech-lead rulings the build needs (gdd §17 Owners):** the shape and home of the single Challenge cleared function (GD-SB-14); how pool entries carry `minimumPegs = 1` for the Diamond-41 footprint (I5); confirmation that an old-version snapshot's hints degrade to the solver through the legality guard as §12.3(3) states; whether `GameStatus.Won` is renamed.
4. **Latent defects found while reading, fixed by this spec, named so the retro sees them:** (a) `Boards.Diamond41` sits in the Challenge pool with its Classic floor of 2 — unreachable only because (b) freeform footprints take over entirely from level 40, which also makes the pool widening at 51/101/151/201 dead code: European-37, Asymmetric-39, Diamond-41 and Wiegleb-45 have never appeared in Challenge. (c) `ResultsViewModelTest.aStuckTwoPegFinishOnAOnePegBoardIsStillNotRecorded` pins behaviour the directive reverses and must be retired, not kept green by accident. (d) `GameStatus.kt` KDoc says "a single peg remains"; the code says `<= minimumPegs`.
5. **The founder may move the cut.** `LEVEL_OFFSET` is one constant; moving it re-rolls every board and re-runs the fuzz, so it should be decided from the device build at Gate 2, not after.
6. **Phase 16 does not exist in `ROADMAP.md`.** The requirement rows say "Phase 16 (proposed)"; the producer or gsd adds the phase. Whether it merges into `release/3.0.0` or the release is re-cut is the producer's and release-manager's call (Q-030 is the precedent).
7. **Pre-release testers lose difficulty continuity once** (a tester at level 30 meets what level 80 dealt). Accepted in GD-SB-13; no store player exists to be affected.
8. **The 2026-08-30 todo** (`decide-whether-stuck-finishes-reach-results-1-2-stars.md`) is answered for Challenge by GD-SB-03; the producer closes or re-scopes it to Classic pending Q-031.

## Handoff
Next: **producer** — commit `docs/gdd.md`, `.planning/REQUIREMENTS.md` and this report on `feature/challenge-star-bands` (`docs(design): …`); queue Q-031; add Phase 16 to `ROADMAP.md` or route it to gsd; route the items below; update `PROJECT.md` Key Decisions ("Stars from pegs remaining" row) with one line pointing at gdd §13.

**android-game-dev builds, test-first, in this order (gdd §17; every test name in gdd §14):**
1. `:domain` Challenge clear rule: `CLEAR_MAX_PEGS = 3`; terminal and ≤ 3 pegs → cleared with `4 − pegs` stars; terminal and ≥ 4 → stuck; a board with a legal move → playing; ignores `minimumPegs` (SB-D1…D4). `GameStatus` KDoc corrected.
2. `:domain` generator: `LEVEL_OFFSET = 50` in `DifficultyTiers.forLevel` (`fromStart = level − 1 + 50`, every lever but the freeform intro); `FREEFORM_INTRO_LEVEL = 10` against the visible level; from it an even PRNG draw between freeform and the canonical pool; `FAIL_PROOF_LEVELS = 4` gate (every reachable terminal position ≤ 3 pegs); every pool spec carries `minimumPegs = 1`; `GENERATOR_VERSION = 2`; golden fixtures regenerated (`UPDATE_GOLDEN=true`); `FuzzHarnessTest` asserts `minimumPegs == 1` and version 2 per level (SB-D5…D10).
3. `:app`: `GameViewModel.statusFor` mode-aware (Classic R4, Challenge R5/R6); finish emitted and `advancePast` called on every clear; `stuck` only at ≥ 4 pegs; `ResultsViewModel`/`ResultsUiState` read one cleared test (`<= 3` for Challenge, `<= minimumPegs` for Classic); `ResultsHeadlineTier.CLEARED`; `PlaySessionDao.flags` adds `AND stars = 3` to the hint-free, undo-free and speed predicates (SB-V1…V7). Retire `aStuckTwoPegFinishOnAOnePegBoardIsStillNotRecorded`.
4. `:app` strings and surfaces per gdd §16 with the ux-designer's wording (SB-U1…U4).
5. `./gradlew check`; then `FUZZ_SEEDS=100000` on the release branch.

**Copy that changes (gdd §16 — the list the Turkish localisation branch needs; the app has no `values-tr`, tr-TR copy of record lives in the ux-spec and `docs/store/tr-TR/`):**
- New: `results_headline_cleared` ("Cleared!"); `results_next_star_hint` (2 stars: "Leave 1 peg for 3 stars." / 1 star: "Leave 2 pegs for 2 stars, 1 for 3."); `stuck_message_challenge` ("No moves left with %1$d pegs — leave 3 or fewer to clear the level. Undo or restart."); `nowin_message_challenge` ("No way down to one peg from here. You may still clear the level with 2 or 3 pegs — or undo or restart.").
- Unchanged but newly reachable: `results_headline_solved`, `catalog_stars_earned` at 1 and 2, `results_a11y_summary` ("Cleared! — 1 of 3 stars, 3 pegs left."), `results_new_best` on star upgrades.
- Unchanged: tutorial captions, `stuck_message`/`nowin_message` (Classic only now), `results_headline_stuck` (defensive), the Results actions.
- Store/changelog (release-manager): no store sentence becomes false; optional added line and the CHANGELOG *Changed* bullet are drafted in gdd §16.

**ux-designer:** final wording and tr-TR for the four new strings within gdd §16's rules; placement of the next-star line (a stop after the star row); TalkBack script; ux-spec §2 row 6 ("Results | Unchanged" is no longer true), §4.4's control-state rows for the Challenge stuck and proven-dead messages, §8 rows.

**tech-lead:** open issues 2 and 3 above; tech-design §7.1 (generator facts: offset, G6, G7, I5, version 2) and §7.3 (fuzz assertions); confirm the resume-path and hint-path claims in gdd §12.3(3).

**qa-engineer:** SB-Q1…Q11 in gdd §15 on the emulator (a phone if attached), observed `result:` lines; SB-Q9 is the upgrade path and needs a pre-change build installed first.

**release-manager:** the `FUZZ_SEEDS=100000` run on the release branch; changelog and store lines per gdd §16; the Phase-16-into-3.0.0 sequencing call with the producer.

**gsd (through the producer):** Phase 16 with CSB-01…08 and CLC-01…06; the `PROJECT.md` row; the todo closure.

## Amendment 1 — 2026-11-01: the tech-lead's F-1…F-5, the ux-designer's D-SB-1…6, Q-037
Only `docs/gdd.md` and this report were edited; `docs/ux-spec.md`, `docs/tech-design.md` and every other file were not touched (a developer and the ux-designer work in this worktree).

**Tech-lead brief (`tech-lead-brief.md`, 80b8e5a; tech-design c31860d) — all five findings taken, none contested:**
- F-1 → gdd §9 (the recorded line is never loaded on a resume: `solution == null`, `onRecordedLine = false` in `applyRestored`, so the hint goes to the solver directly), §12.3(3) rewritten with that mechanism and with the restart ruling (an old-stamp restart regenerates through the level-load path; a current-stamp one keeps the stored board — built in C3, not true before), SB-V7 corrected and SB-V8 added.
- F-2 → gdd §8 closing paragraph: the generator does not grade a finish; it reads `ChallengeClearRule.CLEAR_MAX_PEGS` for G6 and nothing else of the rule.
- F-3 → gdd §4 tier row and §7.3: `failProof` is on the tier; `FREEFORM_SHARE_DENOMINATOR` is a `GeneratorConstants` contract constant.
- F-4 → gdd §7.3: the share bounds and "all five by 300" are tuning targets, measured first (M3), asserted once they hold; a miss comes back to me, never to the developer.
- F-5 → gdd §14 row GD-SB-13 re-pointed to `GameViewModelResumeTest` with the two restart tests; the mapper gets the narrower stamp-pass-through and the C6 one-peg-by-mode tests.
- The three rulings that answered my open issues are folded in: §4 (the function's home and shape, two mode-branch sites, `Won` keeps its name), §8 I5 (uniform footprint copies with `minimumPegs = 1`; `SnapshotMapper` case 2 by mode), §12.2 (the old 2.0 app persisted only to Realm; no file collides; nothing to migrate). Open issues 2 and 3 above are closed by the brief.

**Ux-designer's spec (b8e4c8c, §3 D-SB-1…6, §4.4.3, §4.6) — acknowledged, no objection, no change requested:**
- D-SB-4 (the Challenge proven-dead banner dismissable by a tap outside its buttons): consistent with the rules, recorded as **GD-SB-19** with the boundary that matters — the dismissal clears only the proof flag, is never charged, counted or undoable, plays no cue; the proof stands and a Hint on the same position raises the banner again, free; the stuck banner is never dismissable in either mode; Classic's proven-dead banner unchanged. Tests added in gdd §14 (`dismissingTheProvenDeadBannerClearsOnlyTheProofFlagAndChargesNothing`, `onlyTheChallengeProvenDeadBannerCarriesTheDismissAction`), SB-V9 and SB-U5 in §15, SB-Q6 extended.
- D-SB-2 (next band up only): meets GD-SB-08's rule; my two-band one-star line is superseded, recorded as **GD-SB-20**; gdd §16 now carries the decided copy for all four strings (D-SB-1…3), including the plurals and format arguments, and names the ux-spec §8 D-SB table as the Turkish copy of record (D-SB-6).
- One placement note taken from the spec: `GameScreenTest` does not exist; the two game-screen tests live in the existing game-screen test files or a new `GameScreenRescueBannerTest` (gdd §14).

**Founder question id.** Q-031 was already taken (the ux-designer's Turkish register); the Classic-bands question above is **Q-037**, corrected in place. Classic is unchanged in 3.0.0 per the directive's "on challenge" (gdd §1, §6.2, GD-SB-06, §17); Q-037 asks only about 3.1.

**Checks for this amendment:** `Read .planning/reports/challenge-star-bands/tech-lead-brief.md` (all 178 lines) — pass; `Read docs/ux-spec.md:4345-4450` (§4.4.3, D-SB-3/D-SB-4 as specified) — pass; one haiku Explore helper located the D-SB-1…6 definitions (`docs/ux-spec.md:2022-2072`) and quoted them — pass; the GDD edits re-read in the tool's returned state. Nothing run; nothing committed — the producer commits `docs/gdd.md` and this report.

## Amendment 2 — 2026-11-01: the founder's Q-040 re-scope ("let the player get 3 stars. remove first like 30 levels"; "i remove i meant shift")
Edited: `docs/gdd.md`, `.planning/REQUIREMENTS.md` (the CSB/CLC rows, their traceability and the decisions row) and this report; nothing else.

**Kept for 3.0.0, as the producer listed:** the clear rule and bands (R5–R7: 1/2/3 pegs = 3/2/1 stars, all cleared and advancing; 4+ not cleared), every surface and string including the dismissal (GD-SB-19) and the next-star line (GD-SB-20), `LEVEL_OFFSET = 30`, `GENERATOR_VERSION = 2`, the resume and old-stamp-restart rules (GD-SB-13).

**Worded as a shift, per the clarification.** Level 1 plays like the old level 31; level numbers, the ceiling, the seeds and every stored number are unchanged; nothing is deleted or renumbered (gdd §1, §5, §7.1, §7.2, GD-SB-09, GD-SB-21; REQUIREMENTS "ladder shift", CLC-01). The curve with offset 30, re-derived by hand against `DifficultyTiers.forLevel`: level 1 = `S` 4, 5 pegs, English-33 only, multiplier 2; level 21 = `S` 5, 6 pegs, the second footprint; level 40 = freeform from here as shipped; the cap at level 646; level 10,000 multiplier 502 — the §7.1 table (pass).

**Deferred to 3.0.1 by the founder, marked [3.0.1] everywhere they appear:** the levels 1–4 fail-proof gate (G6, GD-SB-10; CLC-02), the freeform intro at 10 with the even draw (G7, GD-SB-11; CLC-03), the pool changes — one-peg copies, Diamond-41, widening, and the resume-path floor by mode (GD-SB-15; the tech-lead's §1(c) and C6) — and the three-star requirement on the skill achievements (GD-SB-05; CSB-06). They form one 3.0.1 bundle and one further contract bump (version 3), gdd §17. CSB-08 (one-peg target on every generated board) stays in 3.0.0 as a fact proven by the fuzz harness: with the shift the canonical pool is drawn only below the intro (40), where it holds at most English-33 and European-37; the Diamond-41 entry is first reached at visible level 121, long after the freeform takeover makes the pool unreachable.

**Recorded deviation (rules/puzzle-design.md, "first failure impossible before puzzle 5"), approved by Q-040 — GD-SB-21, gdd §7.4, §10:** in 3.0.0 Challenge levels 1–4 can be finished uncleared. The numbers of the old level 31, now level 1: English-33, 5 pegs, a 4-jump recorded line; a clear needs 3 pegs, so the only uncleared finish is a one-jump dead end (first jump → four pegs, no legal move). Levels 1–20 are all five-peg boards. Mitigations in the build: at least two opening moves (G3), the banner naming the count and the threshold, five free undos, Restart, and the dead-end-free tutorial. A second consequence recorded (gdd §11.2): with the skill predicates as shipped, a one-star clear of level 1 in under sixty seconds without a hint or an undo unlocks Quick Hands, No Hints Needed and No Take-Backs in the first minute.

**Criteria and test map updated to what is kept:** §14 — the curve tests renamed for 30 (`…AtLevelThirtyOne`, `…ThirtyLevelsLater`, cap at 646, `levelOneDealsFivePegsAndAFourJumpLine`); the G6, G7, GD-SB-05 rows and the two C6 mapper tests marked 3.0.1; the shipped `footprintsAreCanonicalBelowTheIntroLevelAndFreeformAtIt` stays green unedited. §15 — SB-D5/D6 re-based on 30; SB-D7, SB-D8's mix clause and SB-V6 marked 3.0.1; SB-Q1 (English cross, 5 pegs); SB-Q5 is informational in 3.0.0 (a one-jump dead end on level 1, if the board has one, and what the banner says); SB-Q8 records that a 1-star hint-free play unlocks the award in 3.0.0. §17 — step 2 is the offset and the bump only; `PlaySessionDao` untouched in 3.0.0.

**For the producer and tech-lead:** the tech-lead's brief predates Q-040; of its commits, C2 shrinks to the offset, the version bump, the fixtures and the fuzz `minimumPegs` line (no G6, G7, pool copies or the `DifficultyTier.failProof` field), C5 and C6 move to 3.0.1, and C1, C3, C4 and C7 stand; M2 and M3 have nothing to measure in 3.0.0 (M1, M4, M5 stand). The brief's "Not in scope" line should name Q-037, not Q-031. Re-briefing is the tech-lead's, routed by the producer.

**Checks for this amendment:** every GDD and REQUIREMENTS edit re-read in the tool's returned state; the §7.1 table arithmetic re-derived for offset 30 at levels 1, 10, 21, 40, 51, 101, 151, 646 and 10,000 — pass. Nothing run; nothing committed — the producer commits the three files.
