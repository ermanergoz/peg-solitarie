---
role: game-designer
status: of record
created: 2026-11-01
revised: 2026-11-01 — Q-040 re-scope (founder): LEVEL_OFFSET 30; G6, G7, the pool copies and GD-SB-05 deferred to 3.0.1
branch: feature/challenge-star-bands
supersedes: nothing — this is the product's first `docs/gdd.md` (tech-design §16 row 5)
amends: `.planning/milestones/v1.0-phases/02-…/02-CONTEXT.md` D-01; `03-…/03-CONTEXT.md` D-02, D-05, D-08, D-10, D-11; `.planning/phases/12-…/12-CONTEXT.md` D-01, D-04
---

# Peg Solitaire — Game Design Document

> The game-designer's spec of record for every gameplay rule (rules/studio.md: the game-designer
> owns rules, state model, progression, difficulty, scoring, hints, onboarding). Until this file
> existed the concept of record was `.planning/PROJECT.md`, `.planning/REQUIREMENTS.md`, the
> 2026-06-28 design spec and the phase CONTEXT decisions (`docs/concept-brief.md`). Those stay
> authoritative for everything this file does not restate; where this file and one of them
> disagree, this file wins and the disagreement is a defect here for the game-designer to fix.
>
> **Why it exists now.** The founder's 2026-11-01 directive for 3.0.0 changes the Challenge
> clear rule and shifts the difficulty curve past the opening levels. Both reach the generator contract, the Results screen,
> persistence, cloud save, achievements and leaderboards, so the ruling needs one durable home
> rather than a fifth report. Sections 1–12 transcribe the shipped rules as verified in the code
> on `feature/challenge-star-bands` (cut from `develop` 37d3052) and mark every change with
> **[3.0.0 change]**; §13 is the decisions log; §14 maps every invariant to a named test; §15 holds
> the acceptance criteria the developer builds against and QA judges by; §16 the copy contract;
> §17 what the phase delivers and what it does not.
>
> UI layout, colour and motion are the ux-designer's (`docs/ux-spec.md`); architecture and module
> boundaries are the tech-lead's (`docs/tech-design.md`). This file names surfaces and
> information rules only.

## 1. The founder's directive, and how it is read

Verbatim, 2026-11-01, for 3.0.0:

> "also on challenge, player needs to leave exactly one peg if they dont, cant move to the next
> step. But it should be like this; if the user leaves 3 pegs, gets 1 star, if the user leaves 2
> pegs gets 2 stars, if the user leaves 1 peg, gets 3 star. Also you can remove the first levels."

**The first sentence describes the game as shipped, not a request.** On `develop` a Challenge
board advances only when it reaches `GameStatus.Won`, and `Won` is `pegCount <= spec.minimumPegs`
— one peg on every generated footprint (`GameViewModel.statusFor`, `:1698-1703`;
`freezeAndFinishIfTerminal`, `:967-983`). A board with no legal move and more pegs is `Stuck`: it
never reaches Results, never records a result, never advances; it stays on the board under the
rescue banner "No moves left — undo or restart" (`:945`, `strings.xml:174`). So "needs to leave
exactly one peg, if they don't, can't move to the next step" is exactly today's rule, and the
"But it should be like this" that follows is the change: a finish with two or three pegs must
count — with two or one stars — and must advance. The Results screen's two lower bands and its
"Out of moves"/Retry branch exist in code (`ResultsViewModel.tierFor`, `:575-585`) but were never
reachable in play; the pending todo of 2026-08-30 records that gap. This directive closes it for
Challenge, in the founder's words.

**What is not said, and how it is decided here.** The directive does not say what a finish with
four or more pegs is beyond "can't move to the next step": §3 R6 keeps it as today's stuck board,
now with no star and a message that names the threshold. It does not name Classic — it says "on
challenge" — so §3 R8 leaves Classic exactly as it is in 3.0.0; the non-blocking founder question
**Q-037** (queued by the producer from the game-designer's report) asks only whether Classic
adopts the bands in 3.1. It did not say how many levels "the first levels" are; §7's first edition fixed
the cut at fifty on measurable grounds as a decision the founder could move by one constant, and
the founder moved it — **Q-040** (2026-11-01, verbatim: "let the player get 3 stars. remove first
like 30 levels", clarified the same day: "i remove i meant shift") sets a **shift of thirty
levels** — level 1 plays like the old level 31, level numbers are unchanged, nothing is deleted
or renumbered — and defers the rest of the generator work to 3.0.1.

**Scope of this ruling.** Gameplay and scope decisions are the game-designer's
(rules/studio.md, Question routing). Everything below is either a transcription of a shipped rule
with its code reference, or a decision in §13 marked as such. Nothing here is a founder approval
of anything; the founder sees the result at Gate 2.

**Re-scope, Q-040 (2026-11-01).** Kept for 3.0.0: the clear rule and its bands (R5–R7), every
surface and string (§16, GD-SB-19, GD-SB-20), `LEVEL_OFFSET = 30`, `GENERATOR_VERSION = 2`, and
the resume and restart rules (GD-SB-13). Deferred to 3.0.1 by the founder: the levels 1–4
fail-proof gate (G6, GD-SB-10), the freeform intro at 10 with the even draw (G7, GD-SB-11), the
pool changes — the one-peg footprint copies, the Diamond-41 entry, the widening (GD-SB-15) — and
the three-star requirement on the skill achievements (GD-SB-05). What the deferral costs against
rules/puzzle-design.md's onboarding rule is recorded in §7.4 and §10 as a deviation the founder
approved. Every section below marks a deferred item **[3.0.1]**.

## 2. Concept and player promise

Peg Solitaire, native Android, two modes. **Classic**: five hand-defined boards (English-33,
European-37, Asymmetric-39, Diamond-41, Wiegleb-45), each with a proven fewest-peg finish.
**Challenge**: a ladder of sequential, procedurally generated levels, each unique within its
neighbourhood, each solvable to one peg by construction, growing harder as the number rises.
The core value (`.planning/PROJECT.md`): infinite, never-repeating, always-solvable Challenge
levels that feel fair and satisfying.

What the player observes: a grid of holes, some holding pegs, one or more empty. What they can
do: tap a peg, tap an empty hole two cells away in a straight orthogonal line with a peg between;
the jumping peg lands, the jumped peg leaves. What progress means: fewer pegs; in Challenge, a
cleared level and a higher ladder number; in both modes, stars banked per level and a numeric
score for ranking. Why mastery is possible: every board is deterministic, every move is visible,
every Challenge level has a recorded winning line and hints that tell the truth about it
(§9), and undo is unlimited in count. How difficulty scales: §7 — the length of the winning
line first, then footprint size and shape.

The promise the 3.0.0 change makes explicit: **in Challenge, a finish is graded by how many pegs
are left, one star per peg fewer, and any finish down to three pegs is a pass.** Perfection
(one peg) is still the ideal the tutorial teaches and the three stars reward; it is no longer the
only way forward.

## 3. Rules

The rules are stated once, numbered, and referenced by number everywhere else in this file.
Code references are to `develop` at 37d3052 unless marked **[3.0.0 change]**.

- **R1 — Board.** A board is a `BoardSpec` footprint (the set of in-play `Position`s on a square
  grid, at most 64 cells, a `Long` bitmask) and a peg set. Every in-play cell is `Peg` or `Empty`;
  cells outside the footprint are not part of the board. (`domain/.../model/BoardSpec.kt`,
  `Board.kt`.)
- **R2 — Move.** A move is a jump: a peg at `from` moves to an empty in-play cell `to` two cells
  away along a row or a column, over an adjacent peg at `over`, which is removed. Jumps are
  orthogonal only, in both modes, forever (`.planning/PROJECT.md`, Constraints;
  `RulesEngine.legalMoves`, `applyMove`). Diagonal jumps are out of scope.
- **R3 — Terminal board.** A board with no legal move is terminal (`RulesEngine.isGameOver`,
  `:79`). A board with one peg is terminal by R2. Play ends only at a terminal board; the player
  may not "bank" a finish while a legal move remains.
- **R4 — Classic win.** In Classic a terminal board whose peg count is at or below the board's
  own perfect finish `BoardSpec.minimumPegs` is won; otherwise it is stuck. `minimumPegs` is 1 on
  four boards and a proven 2 on Diamond-41 (`BoardConstants.kt:23, :42`; GF(4) proof, phase 8).
  Unchanged by 3.0.0.
- **R5 — Challenge clear [3.0.0 change].** In Challenge a terminal board with at most
  **`CLEAR_MAX_PEGS = 3`** pegs is *cleared*, and earns **`CLEAR_MAX_PEGS + 1 − pegsRemaining`**
  stars: one peg → 3, two → 2, three → 1. The count is absolute: it does not read
  `minimumPegs`, the footprint, the level number or the solution length. Every generated level
  reaches one peg by construction (§8 I1), so the three-star finish is always reachable.
- **R6 — Challenge stuck [3.0.0 change in copy only].** In Challenge a terminal board with four
  or more pegs is *stuck*: no stars, no result recorded, no ladder advance, no Results screen.
  The board stays on screen under the rescue banner with Undo, Restart and Home, as today
  (`GameViewModel.kt:942-946`; `NoWinBanner.kt`), and the banner's message names the peg count
  that remained and that three or fewer clears the level (§16).
- **R7 — Advancement [3.0.0 change].** A cleared Challenge level N (any star count) advances
  the ladder past N: the stored ladder becomes `max(current, N + 1)`
  (`ChallengeProgress.advancePast`, `mergeLadder`). Results offers Next level, which serves
  `max(ladder head, N + 1)` clamped to the level bounds (`ResultsViewModel.onNextLevelTapped`).
  A stuck board never advances. Replaces phase-3 D-02 "only a 1-peg solve unlocks level N+1".
- **R8 — Classic unchanged.** Classic keeps R4, the per-board star bands of §6.2, the Results
  flow, records, achievements and leaderboards exactly as shipped. The founder's directive
  names Challenge only; the alignment question is drafted for the founder, non-blocking.
- **R9 — Undo and Restart.** Undo rewinds the last move (history replay, never a stored board);
  Restart reloads the level's starting board. Both are available from a stuck board and from
  play; Restart is never ad-gated (v1.1 decision "Retry stays free"). Undo draws on the per-level
  free allowance, then the rewarded offer (ADS-02). Unchanged.
- **R10 — Hint.** A hint reveals one winning move from the current position, or reports a
  proven dead end, or reports that it could not decide in time (GD-HT-1, §9). A hint that
  reveals no move is never charged (ADS-06, GD-HT-5). Unchanged.
- **R11 — Classic star bands.** Classic stars: at or below `minimumPegs` → 3; within
  `TWO_STAR_BAND_WIDTH = 2` pegs above it → 2; otherwise 1 (`ScoreCalculator.stars`,
  `ScoreParams`). Reachable in play only at 3, since Classic Results opens only for a won board
  (R4). Unchanged; recorded so the two band shapes are both visible in one place.

## 4. State model

Immutable values, pure reducers, seeded randomness only (rules/kotlin-android.md). The names
below are the shipped ones; the tech-lead names any type this change adds.

| State | Where | Contents | Change in 3.0.0 |
|---|---|---|---|
| `Board` | `:domain` | `spec: BoardSpec`, `pegs: Long`; `pegCount` | none |
| `BoardSpec` | `:domain` | `inPlay`, `target?`, `minimumPegs` (1 default; 2 on Diamond-41) | generated specs carry 1 in 3.0.0 by the pool's reach, asserted on every level by the fuzz harness (§8 I5); the by-construction copies are [3.0.1] |
| `GameStatus` | `:domain` | `Playing` / `Won` / `Stuck` | the status of a Challenge board is computed by R5/R6, a Classic board by R4; `Won` reads "cleared" in Challenge. Its KDoc ("a single peg remains") is stale today and is corrected with this change |
| `GeneratedLevel` | `:domain` | `levelNumber`, `startingBoard`, `solution`, `generatorVersion` | `generatorVersion` becomes 2 (§8 I7) |
| `DifficultyTier` | `:domain` | `solutionLength`, `minSolutionLength`, `footprintPool`, `pegRange`, `minOpeningMoves`, `minSpread`, `freeformIntroLevel`, `scoreMultiplier` | computed with the level offset (§7). `failProof` (G6) and the freeform share constant `GeneratorConstants.FREEFORM_SHARE_DENOMINATOR` (G7; a contract constant, not a tier field — tech-lead ruling) are [3.0.1] |
| `ScoreResult` | `:domain` | `stars`, `score`, `pegsRemaining`, `pegsCleared`, `elapsedSeconds` | `stars` for Challenge come from R5, not from `ScoreCalculator.stars(…, minimumPegs)` |
| `GameFinish` | `:app` | `ScoreResult`, `SolveTelemetry`, `minimumPegs` | emitted for every Challenge clear (1–3 stars), not only for `pegCount <= minimumPegs` |
| `GameUiState.stuck` | `:app` | the rescue-banner flag | set for a Challenge terminal board with 4+ pegs; never for a cleared one |
| `ResultsUiState` | `:app` | `stars`, `pegsRemaining`, `headlineTier`, `challengeLevel?`, `minimumPegs`, `isSolved`, `showNextLevel` | for Challenge the single cleared test is `pegsRemaining <= CLEAR_MAX_PEGS`; for Classic it stays `pegsRemaining <= minimumPegs`. One test, read by the record gate, the headline and `showNextLevel` alike (the CR-01 lesson) |
| `ResultsHeadlineTier` | `:app` | `PERFECT` / `SOLVED` / `STUCK` | gains `CLEARED` (1 star); `STUCK` stays as the defensive branch for a route reached with an uncleared peg count, which play never produces |
| Ladder | DataStore `challenge_current_level` | the next level to play, 1-based, monotonic by `max` | numbers kept as they are (§12) |
| `LevelResult` | Room `level_result` | `(mode, levelId)`, `stars`, `bestTimeMs`, `bestScore`, `completedAt` | 1- and 2-star Challenge rows now exist |
| `play_session` | Room | one row per recorded play: `stars`, `score`, `elapsedMs`, `hintsUsed`, `undosUsed`, `lastPegOnTarget` | one row per Challenge clear; the `stars = 3` filter on the skill predicates is [3.0.1] (§11) |
| Resume snapshot | Room `resume_snapshot` blob | initial board, move history, `challengeLevel`, `generatorVersion`, `minimumPegs`, allowances, counters | old-version snapshots resume on their stored board (§12) |
| Cloud payload | Saved Games, `schemaVersion = 1` | `ladder`, `playedTimeMs`, `bests[(mode, levelId) → stars, time, score]` | unchanged; schema version not bumped |

The one structural rule this change adds: **the Challenge cleared test exists in exactly one
`:domain` function, and `GameViewModel`, `ResultsViewModel` and `ResultsUiState` all read it.**
Ruled by the tech-lead (`.planning/reports/challenge-star-bands/tech-lead-brief.md` §1(b)):
`scoring/ChallengeClearRule` with `ChallengeFinish` (`Cleared(stars)` / `NotCleared`),
`CLEAR_MAX_PEGS` and `PERFECT_STARS`; the mode branch lives at exactly two `:app` sites,
`GameViewModel.statusFor` and `ResultsUiState.isSolved`, and `ResultsViewModel` reads the state's
property; `GameStatus.Won` keeps its name.
Three separate `pegsRemaining == 1` comparisons across two files are how the Diamond-41 record gap
survived to phase 10 (CR-01); the 3.0.0 change must not reintroduce that shape with a `<= 3`.

## 5. Modes and the Challenge ladder

**Classic.** The five-board catalogue in ascending hole count (`Boards.CLASSIC_SPECS`). Each
board plays from its locked start to a terminal board (R3); a won board (R4) goes to Results
with Replay, Catalog and Home; a stuck board stays under the rescue banner. Per-board bests and
the catalogue's star pips read `level_result` rows keyed `("classic", boardId)`. Unchanged.

**Challenge.** Levels are the integers `1..MAX_CHALLENGE_LEVEL` (10,000;
`GeneratorConstants.kt:42`); the number is the seed and the difficulty input (§7). The player
sees one number at a time:

- **Continue** (Home's Challenge tile, the Level pill) serves the ladder head —
  `challenge_current_level`, the next level to play, 1 on a fresh install.
- **Next level** after a clear serves `max(ladder head, N + 1)` (R7), so a replay of an old
  level behind a merged cloud head never sends the player backwards.
- **Restart** on a level reloads its starting board; **Retry** on Results is the same action
  reached from the defensive stuck branch, which play never shows (§4).
- A stuck board (R6) offers Undo, Restart and Home and nothing else. There is no skip, no
  level select, no way past a level but clearing it. That is deliberate and unchanged: the
  levels are solvable by construction, so the ladder is fair; what 3.0.0 changes is how good a
  finish has to be to count (R5).

**Numbering after the shift [3.0.0 change].** It is a shift, not a removal (the founder's
clarification to Q-040: "i remove i meant shift"). The player-visible numbers are unchanged —
level 1 is still the first level, the ceiling is still 10,000, the Level pill, the ladder
leaderboard and the ladder achievements still count the same integers. What changes is the
content behind each number: level N deals what the previous curve dealt at N + 30 (§7). Nothing
is deleted, nobody is renumbered and no stored number moves (§12). The ladder is infinite in practice either way: at a level a minute,
the ceiling is a week of continuous play.

**What a level teaches about the rule.** A new player's first Results screens show one, two
or three stars with a line that says what the next star needs (§16), and that is how the band is
learned — by result, not by text. The tutorial keeps teaching the ideal, one peg (§10). In 3.0.0
the first levels *can* be finished uncleared — the fail-proof gate is [3.0.1] (Q-040); on a
five-peg level the only such finish is a one-jump dead end, and the banner, Undo and Restart are
the way back (§7.4, §10).

## 6. Scoring: stars, score, bests

### 6.1 Challenge stars [3.0.0 change]

| Pegs left at the terminal board | Outcome | Stars | Results | Recorded | Ladder |
|---|---|---|---|---|---|
| 1 | cleared, perfect | 3 | "Perfect!" + Next level | yes | advances |
| 2 | cleared | 2 | "Solved!" + Next level + next-star line | yes | advances |
| 3 | cleared | 1 | "Cleared!" + Next level + next-star line | yes | advances |
| 4 or more | stuck | none | no — stays on the board under the banner | no | no |

`stars = CLEAR_MAX_PEGS + 1 − pegsRemaining` for `pegsRemaining <= CLEAR_MAX_PEGS`, else the
board is not cleared. Two named constants: `CLEAR_MAX_PEGS = 3` and the star count 3; everything
else derives. The band is the same on a 6-peg level 1 and a 31-peg level 300: the score multiplier
(§6.3) is what rewards the climb, stars grade the finish.

Why absolute and not relative to `minimumPegs`: every generated level is built backwards from a
single peg, so one peg is reachable on every level (§8 I1); a per-board anchor would only ever
differ on the Diamond-41 footprint, where it would be wrong — it would hand three stars to a
two-peg finish on a board that reaches one. The founder's bands are absolute; so is the rule.

### 6.2 Classic stars (unchanged)

`ScoreCalculator.stars(pegsRemaining, minimumPegs)`: at or below the board's floor → 3; within
two pegs above it → 2; otherwise 1 (R11). Classic Results opens only for a won board (R4), so in
play a Classic result is always three stars; the lower bands remain defensive. The founder's
directive says "on challenge", so Classic is unchanged in 3.0.0; Q-037 (non-blocking) asks only
about 3.1.

### 6.3 Score (unchanged)

`score = (pegsCleared × 100 + max(0, 5000 − elapsedSeconds × 10)) × levelMultiplier`
(`ScoreCalculator.score`, `ScoreParams`). `levelMultiplier` is `DifficultyTier.scoreMultiplier`
in Challenge (§7) and 1 in Classic. Stars and score are independent axes (phase-2 D-03): a slow
perfect clear is three stars and a modest score; a fast one-star clear can out-score it. The
clock freezes for every ad (ADSF-01) and at the terminal board.

### 6.4 Bests (unchanged merge, newly exercised)

`level_result` keeps one row per `(mode, levelId)`; a recorded play merges in by
`ProgressMerge.merge`: `stars = max`, `bestTimeMs = min`, `bestScore = max`
(`ProgressRepository.recordResult`, `:412-452`; `ProgressMerge.kt:42-50`). Consequences the
change makes visible for the first time:

- a 1-star clear later improved to 3 stars reads 3; a later 1-star clear leaves a 3-star best
  untouched; "New best!" fires only on an improved field;
- a fast 1-star clear can set the time best while a slower 3-star clear holds the star best —
  the row is a join of bests, not one play, and the Results screen reports per field;
- a stuck board writes nothing, so a level's row exists only once it has been cleared, which is
  what the Home ribbon, the ladder leaderboard and the achievements count on (§11).

The cloud merge is the same function and the same laws (commutative, associative, idempotent),
so two devices converge on the union of their bests whichever one records the 1-star clear.

## 7. Difficulty model

Difficulty is a function of the level number only (`DifficultyTiers.forLevel`), continuous, with
every knob a named constant in `DifficultyTiers.Curve` (phase-3 D-05/D-06/D-07). Every lever is
numeric and recorded per level by the tier the generator used.

### 7.1 The levers (shipped values, then the 3.0.0 change)

| Lever | Shipped formula (`fromStart = level − 1`) | Role |
|---|---|---|
| Solution length `S` | `min(30, 3 + fromStart / 25)` | primary: the length of the recorded winning line; the start deals `S + 1` pegs |
| Minimum solution length | `max(3, S − 1)` | gate floor (G1) |
| Footprint pool | first `min(5, 1 + fromStart / 50)` of English-33, European-37, Asymmetric-39, Diamond-41, Wiegleb-45 | board size, later and slower than `S` |
| Peg range | `(S) .. (S + 3)` pegs on the starting board | density gate (G2) |
| Opening branching floor | 2 legal moves | gate (G3) |
| Spread floor | bounding-box width + height ≥ 2 | gate (G4) |
| Freeform intro | level 40 | shape variety |
| Score multiplier | `1 + fromStart / 20` | reward for the climb (§6.3) |
| Dedup window | 8 levels | uniqueness in the player's sequence (G5) |

**[3.0.0 change] — the shift.** `fromStart = (level − 1) + LEVEL_OFFSET`, **`LEVEL_OFFSET = 30`**
(Q-040; the first edition said 50, §7.2). The curve's origin moves thirty levels along; the
level numbers, the seeds and every stored number stay where they are. Every lever above except the freeform intro reads the
offset value, so level N deals exactly what the previous curve dealt at N + 30; the shape of the
curve, its steps and its cap are untouched. The freeform intro stays at 40 against the visible
level, as shipped (7.3 is [3.0.1]). Worked values:

| Visible level | `S` (old → new) | Starting pegs | Footprint pool (as shipped) | Multiplier (old → new) |
|---|---|---|---|---|
| 1 | 3 → **4** | 4 → **5** | 1 (English-33) | 1 → 2 |
| 10 | 3 → 4 | 4 → 5 | 1 (English-33) | 1 → 2 |
| 21 | 3 → 5 | 4 → 6 | 1 → 2 (+ European-37) | 2 → 3 |
| 40 | 4 → 5 | 5 → 6 | freeform from here, as shipped (7.3) | 2 → 4 |
| 51 | 5 → 6 | 6 → 7 | (pool 2 — unreachable: freeform) | 3 → 5 |
| 101 | 7 → 8 | 8 → 9 | (pool 3 — unreachable) | 6 → 7 |
| 151 | 9 → 10 | 10 → 11 | (pool 4 — unreachable) | 8 → 10 |
| 646 | 28 → 30 (cap) | 29 → 31 | — | 33 → 34 |
| 10,000 | 30 | 31 | — | 500 → 502 |

### 7.2 Why thirty

Thirty is the founder's number (Q-040: "remove first like 30 levels" — "i remove i meant
shift"). What the shift does, measured by R5: a level with solution length `S` deals `S + 1`
pegs, so reaching three pegs takes `S − 2` jumps. Under the old curve levels 1–25 had `S = 3` —
**any single legal jump cleared the level**, a one-star pass with no decision in it — and levels
26–50 had `S = 4`, two jumps. Shifting the curve by thirty puts the whole single-jump band and
five levels of the two-jump band behind the start — nothing is deleted or renumbered; the ladder
simply begins where the old level 31 was: **level 1 is English-33, 5 pegs, a 4-jump line**; a
one-star clear takes two jumps, a two-star three, a three-star four. Levels 1–20 stay in that
`S = 4` band; `S = 5` (6 pegs) arrives at level 21 with the second footprint, `S = 6` at 46.

The first edition chose fifty, so that a one-star clear would take three jumps from level 1 and
the pool would hold two shapes from the first screen (its reasoning is kept in §13's rejected
list, under the 25-level row). The founder's thirty keeps twenty levels where a one-star pass is
two jumps; that is the founder's call on taste and pace, recorded, not re-argued. It is one
constant, and the generator contract is bumped once for it (§8 I7).

### 7.3 Freeform intro and share [3.0.1 — deferred by Q-040]

Shipped: at level ≥ 40 the generator tries a freeform footprint first and falls back to the
canonical pool only when none is produced — and `FreeformFootprints.build` always produces one
(`:38-59`). So from level 40 every level is freeform, and the pool widening at 51, 101, 151 and
201 is unreachable: European-37, Asymmetric-39, Diamond-41 and Wiegleb-45 have never appeared in
Challenge. **In 3.0.0 this stays as it is** (Q-040): the intro is 40 against the visible level
and the pool is as shipped, so levels 1–20 deal English-33, levels 21–39 English-33 or
European-37, and every level from 40 is freeform; Asymmetric-39, Diamond-41 and Wiegleb-45 remain
unreachable in Challenge, as today. For 3.0.1 the intro is re-stated as phase-3 D-08 meant it —
"freeform shapes introduced at higher levels as an *added* variety lever" — and that change
re-rolls every level, so it is a second contract bump (version 3):

- `FREEFORM_INTRO_LEVEL = 10`, compared against the **visible** level, so a new player's first
  nine levels are the English and European crosses they met on Home and in the catalogue;
- from level 10 each generation attempt draws **freeform or canonical with equal probability**
  from the level's own PRNG (`FREEFORM_SHARE_DENOMINATOR = 2`; a draw of 0 means freeform), so
  the canonical shapes keep appearing and the pool widening means what the table says.

Both are designer decisions (GD-SB-11). `FREEFORM_SHARE_DENOMINATOR` lives in
`GeneratorConstants` as a contract constant — it changes every level. The two measurables — over
levels 10..1,000 the *accepted* freeform share lies in `[0.35, 0.65]`, and all five canonical
footprints occur by level 300 — are **tuning targets, not mechanism guarantees**: the draw is per
attempt, and the accepted share depends on the two kinds' gate pass rates (tech-lead F-4). The
developer measures them first (the brief's M3) and asserts them only once they hold; a miss comes
back to the game-designer, who moves the bound or the denominator — never to the developer's
judgement.

### 7.4 Fail-proof opening [3.0.1 — deferred by Q-040], and the recorded deviation

The design: `FAIL_PROOF_LEVELS = 4`; levels 1–4 reject any candidate board from which some legal
line ends with more than `CLEAR_MAX_PEGS` pegs (gate G6, §8), so a player cannot finish those
four levels uncleared whatever they tap, and the band teaches itself from one-, two- and
three-star Results screens (§10). It would make rules/puzzle-design.md's "first failure
impossible before puzzle 5" true of Challenge for the first time; the phase-15 tutorial ruling
named it as the open gap (its risk 5). The founder deferred it to 3.0.1 (Q-040).

**Recorded deviation (3.0.0), approved by the founder's Q-040.** In 3.0.0 Challenge levels 1–4
can be finished uncleared. The numbers: new level 1 is the old level 31 — English-33, **5 pegs,
a 4-jump recorded line** (`S = 4`); a clear needs the board down to 3 pegs, so the *only*
uncleared finish on a five-peg level is a **one-jump dead end**: the first jump leaves four pegs
with no legal move. After two jumps the board holds three pegs and any terminal state is a clear.
Levels 1–20 are all five-peg boards, so the exposure is the same on each. Mitigations already in
the build: the opening always offers at least two legal moves (G3), so the dead end is a choice,
not a rail; the stuck banner names the count and the threshold (R6, §16); Undo (five free per
level) and Restart are on the dock and on the banner; the tutorial's three boards remain
dead-end-free, so the rule holds for the first three puzzles the player meets. What is not
mitigated: a player who takes the one wrong jump on level 1 meets a stuck board before puzzle 5.
The gate that removes that outcome ships in 3.0.1 with the next contract bump.

### 7.5 What is not changed

The step sizes (25, 50, 20 levels), the solution-length cap of 30, the density and spread
floors, the dedup window, the level bounds, the seed mix, the freeform intro (40) and the
canonical pool are as shipped. The shift moves the curve's origin; it does not re-tune it.
Re-tuning is a playtest loop (D-07) and a later release; 7.3 and 7.4 are 3.0.1.

## 8. Generation: invariants and quality gates

Reverse generation (`LevelGenerator`): start from one peg on a footprint, un-jump `S` times
under a PRNG seeded from the level number and a rejection salt, record the walk; reversed, it is
the level's winning line. Each level is generated in a chain from level 1 so the dedup window is
exact. Each invariant below is proven by the test named in §14.

**Invariants (hold for every level, every version).**

- **I1 — Solvable to one peg.** The recorded solution replays legally through `RulesEngine`
  and ends with `pegCount == 1`. This is why R5's three stars are always reachable.
- **I2 — Seed-reproducible.** `generate(N)` is a pure function of `N`: same board, same
  solution, on every run and every device; golden fixtures pin levels 1, 2, 10, 100 and 1,000.
- **I3 — Unique in the neighbourhood.** No two levels within a window of 8 share a
  symmetry-canonical peg key. Global uniqueness is not claimed and not required.
- **I4 — Bounded.** Any level number is clamped to `1..10,000`; generation terminates within
  `MAX_ATTEMPTS = 1,000` attempts per level with a deterministic fallback.
- **I5 — One-peg target.** Every generated `BoardSpec` carries `minimumPegs == 1`, whatever
  its footprint. **In 3.0.0 it holds by the pool's reach, and the fuzz harness asserts it on
  every level** (G8): the canonical pool is drawn only below the freeform intro (visible level
  40), where with the shift it holds at most English-33 and European-37, both floor 1; the
  Diamond-41 entry (`DifficultyTiers.kt:70`, floor 2) sits at pool position 4, first reached at
  visible level 121, where the freeform takeover has long since made the pool unreachable
  (§7.3). **[3.0.1]** makes it true by construction when the mix makes the pool reachable again
  (Q-040 deferred the pool changes): the tech-lead's ruling (brief §1(c)) — every canonical pool
  entry a footprint copy built uniformly from `Boards.CLASSIC_SPECS` with `minimumPegs = 1` and
  its `target` kept, never a Diamond-41 special case; identity comparisons in tests become
  footprint comparisons — and the resume-path floor by mode (`SnapshotMapper` case 2, the
  brief's C6; no such blob can exist in 3.0.0).
- **I6 — Versioned.** Every level carries `generatorVersion`; the fuzz harness and the fixtures
  assert the current value.
- **I7 — Contract bump [3.0.0 change].** The shift changes the boards every level deals, so
  `GENERATOR_VERSION` becomes **2**, the five golden fixtures are regenerated under
  `UPDATE_GOLDEN=true` with the bump, and the fuzz harness runs green at `FUZZ_SEEDS=10000` in
  CI and `FUZZ_SEEDS=100000` (the whole space) on the release branch. The 3.0.1 items (G6, G7,
  the pool copies) re-roll every level again and bump the version to 3 then — one bump per
  release, never a partial contract. No parallel v1 generator is kept: the store has never seen a build of this codebase
  (`.planning/phases/14-release-readiness/14-RELEASE-PLAN.md` §1), so there is no live player
  whose levels v1 must keep serving; the version stamp is what lets stored data say which
  contract made it (§12).
- **I8 — Difficulty measurable.** The tier that generated a level is a pure function of its
  number (§7), and the solution length is recorded on the level itself.

**Quality gates (a candidate is rejected and re-seeded unless all pass).**

| Gate | Rule | Status |
|---|---|---|
| G1 | `solution.size >= tier.minSolutionLength` — no trivial lines | shipped |
| G2 | `startingBoard.pegCount in tier.pegRange` — density band | shipped |
| G3 | `legalMoves(start).size >= 2` — no forced opening | shipped |
| G4 | bounding-box spread ≥ 2 — no clump | shipped |
| G5 | canonical key not in the previous 8 levels' keys | shipped |
| G6 | **[3.0.1]** (deferred by Q-040) for levels `1..FAIL_PROOF_LEVELS`: every reachable terminal position has `pegCount <= CLEAR_MAX_PEGS` (enumerate every position reachable by legal jumps from the start; at 5 or 6 pegs this is at most a few hundred positions) | 3.0.1 |
| G7 | **[3.0.1]** (deferred by Q-040) footprint choice: below `FREEFORM_INTRO_LEVEL` canonical only; at or above it, freeform or canonical by an even PRNG draw (§7.3). In 3.0.0 the shipped choice stands: freeform first from visible level 40 | 3.0.1 |
| G8 | **[3.0.0]** `startingBoard.spec.minimumPegs == 1` (I5), asserted on every level by the fuzz harness rather than gated — in 3.0.0 it holds by the pool's reach, from 3.0.1 by construction | kept |

The fallback after `MAX_ATTEMPTS` keeps today's rule (the best-scoring non-colliding candidate).
When G6 lands (3.0.1) the fallback is not acceptable for it: the fail-proof property is asserted
by test on levels 1–4 of that contract (§14 row G6), so if a level ever had to fall back past G6
the test fails and the developer re-tunes rather than ships a failable opening.

**What the generator does not do.** It does not target a peg count other than one and does not
grade a finish. In 3.0.0 it reads nothing of the clear rule; when G6 lands (3.0.1) it reads the
clear threshold, `ChallengeClearRule.CLEAR_MAX_PEGS`, and nothing else of the rule — one
constant, read rather than copied, because a copy would drift (tech-lead F-2). It does not know
about modes or stars. R5 is applied to the terminal board at play time; the generator's promises
to R5 are I1 and I5, and G6 from 3.0.1.

## 9. Hints and undo

Transcribed from `.planning/reports/phase-14/game-designer-hint-timeout.md` (GD-HT-1…6, in force
and built on `bugfix/hint-undecided`, merged as `b1a6cf9`) and the phase-9 assist decisions.

- A hint request has three outcomes the player sees: **Found** (a book hit, the recorded line,
  or a move on a line the solver completed — shown as the gold glow), **Proven dead** (the
  book's dead entry, an exhausted search or a Pagoda reject — the rescue banner with
  `nowin_message`), **Undecided** (deadline, node ceiling or attempt limit — the neutral notice,
  never the banner). A cancelled search is silent. The 1.5 s deadline stands (D-07).
- The hint's target is the board's perfect finish: `minimumPegs`, one peg on every Challenge
  board (§8 I5). The recorded line is used while the player is on it, and never after a resume — a
  restored game loads no recorded line (`solution == null`, `onRecordedLine = false` in
  `applyRestored`; tech-lead F-1); off it, the opening book (Classic) then the solver.
- A hint that reveals no move is never charged (ADS-06, GD-HT-5); a revealed hint spends one
  unit of the per-level free allowance (3 hints, 5 undos; `AssistAllowance`), after which the
  opt-in rewarded offer applies (ADS-01/02). Restart and a new level refill the allowance and
  reset the counters (phase-12 D-09).
- Undo rewinds one move by replaying history; the per-level undo allowance and offer are as
  above. A stuck board (R6) is rescuable by Undo or Restart; the banner's Undo is the ADS-04
  path.

**Interaction the new rule creates [3.0.0 change] — GD-SB-18.** "Proven dead" means *one peg
is unreachable from here*. Under R4 that was the whole story; under R5 a Challenge board that
cannot reach one peg may still end at two or three and be cleared for fewer stars. So in
Challenge the proven-dead banner must say that one peg is out of reach and that the level may
still be cleared with two or three pegs; it must never say the level is lost, and it keeps Undo,
Restart and Home. Phase 8 already made the identical ruling for Diamond-41's two-peg floor
(the hint-timeout report's §2 table, "the board can still be finished for fewer stars"); this
extends it to every Challenge board. Classic keeps today's `nowin_message`. The hint does not
search for a two- or three-peg line in 3.0.0; a tiered hint ("no line to one peg; here is a line
to two") is a 3.1 candidate recorded in §17, not a promise.

**The banner can be put away — ux-spec D-SB-4, acknowledged (GD-SB-19).** The ux-designer made
the Challenge proven-dead banner dismissable by a tap anywhere on it but its three buttons
(`docs/ux-spec.md` §4.4.3, b8e4c8c), because at 200 % text scale it covers nearly the whole board
and its own sentence invites the player to keep playing. That is consistent with the rules here,
and the boundary it keeps is the one that matters: a dismissal clears the proof *flag* and nothing
else — it is not a hint, not a move, not an undo; it is never charged, never counted and plays no
cue (GD-HT-5, D-09) — and the proof itself stands, so a Hint tapped on the same position returns
Proven dead again, free, and raises the banner again. The **stuck** banner (R6) is not
dismissable in either mode: there is nothing to play beneath it, and Undo, Restart and Home are
the only ways on. Classic's proven-dead banner stays as shipped (R8; 3.1 alignment with Q-037).

## 10. Onboarding

**The tutorial (phase 15, in 3.0.0).** Three bespoke miniatures, each a live board the player
must actually solve to one peg, proven dead-end-free by exhaustive enumeration
(`.planning/reports/phase-15/game-designer-tutorial-boards.md`; `TutorialBoards`): T1 teaches
the jump (one horizontal move), T2 the other axis and "never diagonally" (one vertical move on a
full 3×3), T3 the goal — keep jumping until one peg is left (five pegs, three openings, every
line wins in four moves). No button advances a step; a wrong tap costs nothing; three taps
without progress escalate the help; Skip is on every step. It runs once on first launch and can
be replayed from Settings. It never banks a result, never plays the win cue, never navigates to
Results. Its copy is unchanged by 3.0.0: "Clear the board down to one peg" is still the ideal
and still the three-star finish.

**The first Challenge levels [3.0.0 change].** A non-perfect Results screen says in one line
what the next star needs (§16). That line, read after a real finish, is the whole onboarding of
the band — there is no text about stars in the tutorial and none is added. In 3.0.0 the first
levels can also be finished uncleared: level 1 is a five-peg English-33 board (old level 31) whose
only uncleared finish is a one-jump dead end, caught by the banner that names the count and the
threshold (R6), with Undo and Restart beside it (§7.4). The fail-proof gate that makes levels 1–4
unfailable is **[3.0.1]** (G6, Q-040).

**Against the studio rule** (rules/puzzle-design.md, Onboarding): the first three puzzles teach
without text — T1–T3 are layouts with one or three legal moves and a one-line caption each; the
layout does the teaching and the caption names it. First failure impossible before puzzle 5 —
true of the tutorial since phase 15; **not guaranteed of Challenge levels 1–4 in 3.0.0**, a
recorded deviation approved by the founder's Q-040 (§7.4, GD-SB-21) and closed by G6 in 3.0.1.
Classic is not an onboarding surface: its boards are hard by design and its catalogue says so
with hole counts and star pips.

**What is deliberately not done.** No coach mark on the first Challenge level, no modal that
explains the bands, no star forecast in the gameplay status strip. The ux-designer may propose
a forecast ("3 pegs = 1 star") as a later refinement; it is not required for the rule to be
learnable, and the gameplay chrome was just rebuilt (phase 15, D-04/D-05).

## 11. Progression systems the clear rule feeds

Every system below reads `level_result`, `play_session` or the ladder; none reads the star rule
directly. The change reaches them only through the rows a 1- or 2-star clear now writes and
the rows a stuck board still does not. "Unchanged" means no code change is needed; "query
change" means a SQL predicate changes and no schema does (tech-design §4: a new SELECT over
existing columns is not a schema change).

### 11.1 Home ribbon (unchanged)

Stars = `SUM(stars)` over every `level_result` row, both modes; Boards = Classic rows; Level =
the ladder head (`LevelResultDao.observeHomeProgress`; ux-spec §4.2). A 1-star clear adds one
star; a stuck board adds nothing. Unknown reads as "—", never 0.

### 11.2 Achievements

| Achievement | Rule today | After 3.0.0 | Change |
|---|---|---|---|
| FIRST_SOLVE "First Clear" | any `level_result` row | any clear (1–3 stars) or Classic win | none — the name already says "clear" |
| FIRST_THREE_STAR | any row with 3 stars | same | none |
| CHALLENGE_LEVEL_5 | highest Challenge row ≥ 5 | any clear counts | none |
| LADDER_10/50/100 | highest Challenge row (absolute steps) | any clear counts — the ladder is the ladder | none |
| STARS_25/100/250 | `SUM(stars)` both modes | 1- and 2-star rows add their stars | none |
| HINT_FREE "No Hints Needed" | EXISTS `play_session` with `hintsUsed = 0` | 3.0.0: unchanged — any recorded play counts, so a 1-star hint-free clear unlocks it. **[3.0.1]**: requires `stars = 3` (GD-SB-05) | none in 3.0.0; query change in 3.0.1 |
| UNDO_FREE "No Take-Backs" | EXISTS `undosUsed = 0` | 3.0.0: unchanged. **[3.0.1]**: requires `stars = 3` | none in 3.0.0; query change in 3.0.1 |
| SPEED_SOLVE "Quick Hands" | EXISTS `elapsedMs < 60000` | 3.0.0: unchanged — two quick jumps to a 1-star clear on level 1 unlock it. **[3.0.1]**: requires `stars = 3` | none in 3.0.0; query change in 3.0.1 |
| CENTER_FINISH "Dead Center" | English-33, `lastPegOnTarget` | unchanged (a one-peg finish by definition) | none |
| CLEARED_* / GRAND_TOUR / PERFECTIONIST | Classic rows | unchanged (R8) | none |

Why the three skill awards should need a perfect clear: their names promise *solving* without
help or fast; a 1-star clear is a pass, not a solve, and the predicates are satisfiable on level 1
by any two jumps. The founder deferred the change to 3.0.1 (Q-040), so **in 3.0.0 the predicates
are as shipped and the consequence is accepted and recorded**: a one-star clear of level 1 in
under sixty seconds, with no hint and no undo, unlocks Quick Hands, No Hints Needed and No
Take-Backs in the player's first minute; "first-hour" becomes "first level". When it lands it
amends phase-12 D-01 and D-04 for Challenge only; in Classic a recorded play is always a won
board (R4), so the extra predicate changes nothing there. Play Games achievements already earned
are server-side and immutable; nothing is revoked then either.

### 11.3 Leaderboards (unchanged)

Challenge Ladder = `MAX(levelId)` over Challenge rows — any clear counts, as the ladder does.
Cumulative Challenge Score = `SUM(bestScore)` over Challenge rows — a 1-star clear contributes
its best; improving it later raises the sum. Classic per-board score = the stored `bestScore`.
All three are "larger is better" and resubmitted from aggregates (`ProgressSubmitter`), so a
number stored on Play Games never moves down whatever the device later computes.

### 11.4 Interstitial cadence (unchanged)

An ad may follow only a Results continue action (INT-01) of an eligible finish; eligibility is
"this Results entry is a win", and after 3.0.0 every Results entry Challenge can produce is a
clear (1–3 stars), so eligibility is unchanged in meaning (phase-10 D-14 already defined a win as
any solved finish, two or three stars included). A stuck board never reaches Results, so INT-03
(never after a loss) holds by construction. The "first five levels" suppression counts lifetime
completed plays, i.e. clears. The win counter is banked once per Results entry (WIN_COUNTED_KEY).

### 11.5 Cloud save (unchanged)

The payload (`schemaVersion = 1`) carries the ladder and the bests; 1 and 2 are valid star
values on the wire already. The merge is the §6.4 function. Two devices on different app builds
during the pre-release window merge by the same laws; §12 rules what the numbers mean.

### 11.6 Telemetry (unchanged schema)

`play_session` gains one row per recorded Challenge clear with its `stars`; the skill
predicates read that column from 3.0.1 (11.2). The in-progress snapshot keeps carrying the assist counters
so a killed process cannot launder a hint (D-09).

### 11.7 Rewarded assists (unchanged)

The stuck banner's Undo is the ADS-04 path; the free allowance and the opt-in offer are as
shipped. Retry/Restart is never ad-gated.

## 12. Persistence and the 3.0.0 migration

### 12.1 What is stored (shipped)

| Store | Key or table | Content |
|---|---|---|
| DataStore `app_prefs` | `challenge_current_level` | the ladder head (next level), default 1; there is no separate "highest level reached" key — phase-5 D-13 persisted the current level only, and the highest *solved* level is `MAX(levelId)` over Challenge rows |
| Room v2 | `level_result (mode, levelId)` | stars, bestTimeMs, bestScore, completedAt |
| Room v2 | `play_session` | one row per recorded play (11.6) |
| Room v2 | `resume_snapshot (mode)` | the in-progress game as a JSON blob: initial board, full move history, `challengeLevel`, `generatorVersion`, `minimumPegs`, allowances, counters, elapsed |
| Saved Games | one snapshot, `schemaVersion = 1` | ladder, played time, bests (11.5) |
| Play Games | leaderboards, achievements | server-side, monotonic |

### 12.2 Who holds data when 3.0.0 ships

The Play listing's existing users run the founder's **earlier, different application**
(versionName 2.0, versionCode 2); milestone v1.0 of this codebase was tagged and never uploaded
(`14-RELEASE-PLAN.md` §1). So the only devices holding *this* codebase's data are the founder's
and the testers' pre-release builds. The migration below is specified so that none of them
loses anything, and so that it is correct for any future contract change too. The old 2.0
application's on-disk files were the tech-lead's question and are ruled (brief §1(a),
tech-design §7.4): it persisted only to Realm (`files/pegsolitarie.realm`); this app opens Room
`peg_solitaire.db` and DataStore `app_prefs`; no name collides, so a store update meets no foreign
schema and nothing is migrated, deleted or read. Importing its old bests is the tech-lead's own
non-blocking founder question (recommended no).

### 12.3 Rules — GD-SB-13

1. **Level numbers are kept.** The ladder head, every `level_result` row's `levelId`, the cloud
   ladder and bests, the ladder leaderboard and the ladder achievements keep their numbers. No
   subtraction, no renumbering, no deletion. The content behind a number changes (§7); the
   number does not. Reason: every one of those stores is monotonic by `max` and some are
   server-side; a local subtraction would be undone by the next cloud merge or leave the device
   and the server disagreeing forever. The one thing not preserved is difficulty continuity — a
   tester at old level 30 continues at new level 30 and meets what old level 80 dealt. Accepted:
   it affects only pre-release testers, once.
2. **Stored results are not re-graded.** A `level_result` row written under the old rule
   (necessarily 3 stars, since only a one-peg finish recorded) stays. Nothing is downgraded; the
   merge could not express it anyway.
3. **An in-progress Challenge snapshot resumes on its stored board.** The blob stores the
   initial board and the move history, never "regenerate level N" (phase-5 D-06;
   `ResumeSnapshotDto` KDoc), and `SnapshotMapper` never reads the stamp, so a snapshot stamped
   `generatorVersion = 1` continues exactly the board the player left, with undo and elapsed time
   intact (tech-lead confirmed, brief §1(b)). Its hint goes to the solver directly: a restored
   game loads no recorded line (`solution == null`, `onRecordedLine = false` in
   `applyRestored`), so there is no line to reject — an earlier edition of this item named a
   legality-guard rejection, which was the wrong mechanism for the same outcome (F-1). Its finish
   is graded by R5/R6 and recorded under its level number. **Restart regenerates when the stamp
   is old** — a tech-lead ruling built in the brief's C3, not true of the code before this
   change: the ViewModel keeps the restored stamp, and a Challenge restart whose stamp is not the
   current `GENERATOR_VERSION` does the reset bookkeeping, clears the snapshot and takes the
   level-load path, which serves level N's current board and its recorded line; a current-stamp
   or fresh game keeps today's regeneration-free restart (D-06). New snapshots are stamped 2.
   Discarding old snapshots is rejected: it is the one path that loses a player's game, and it
   buys nothing the stamp does not already give.
4. **The cloud payload is not re-versioned.** `schemaVersion` stays 1: no field changes, and 1
   and 2 were always valid star values. The decoder's refusal of a *newer* version is untouched.
5. **Play Games is not touched.** Achievements already unlocked stay unlocked; ladder
   submissions are max-based on the server and are resubmitted from the same aggregates.
6. **Room schema is not bumped.** 3.0.0 changes no query; the 3.0.1 skill-predicate change
   (11.2) is a SELECT over an existing column; `DB_VERSION` stays 2 and `MIGRATION_1_2` is the
   only migration. If the tech-lead
   finds a schema change necessary, tech-design §4.4's constraints apply and a
   progress-preserving `MigrationTest` ships with it.

### 12.4 Reads that must fail open (unchanged)

A damaged ladder reads as 1; a snapshot that fails validation is "no resume", never a crash;
a forged Results route arg is clamped at the nav boundary, and the Challenge cleared test there
(`pegsRemaining <= CLEAR_MAX_PEGS`) is read from the same single function the game used, so a
forged peg count cannot record a level that was never cleared.

## 13. Designer decisions

Each entry is a game-designer decision taken on 2026-11-01 unless it names an earlier ruling.
Entries are never deleted; a superseded one gets a "superseded by" line.

| Id | Decision | Supersedes or amends |
|---|---|---|
| GD-SB-01 | The directive's first sentence describes the shipped rule (one peg to advance); the change is the band and that every band advances (§1). | — |
| GD-SB-02 | Challenge clear rule R5: a terminal board with `CLEAR_MAX_PEGS = 3` pegs or fewer is cleared and earns `4 − pegsRemaining` stars; the count is absolute. | phase-2 D-01 (the 2–3 → 2-star band) for Challenge; phase-3 D-11 ("a stuck finish shows fewer stars") |
| GD-SB-03 | A terminal Challenge board with four or more pegs is stuck and stays on the board under the rescue banner (R6); no Results variant, no "accept this finish" action. The 2026-08-30 todo's option 1 is taken for 2–3 pegs and option 2's "keep stuck on the board" for 4+. | closes `.planning/todos/pending/2026-08-30-decide-whether-stuck-finishes-reach-results-1-2-stars.md` for Challenge |
| GD-SB-04 | Every clear advances the ladder past its level (R7). | phase-3 D-02 "only a 1-peg solve unlocks level N+1"; phase-3 D-10 for 2–3 pegs |
| GD-SB-05 | HINT_FREE, UNDO_FREE and SPEED_SOLVE require a 3-star clear; CENTER_FINISH already does; first-clear, level-5, ladder and star tiers count any clear (§11.2). **Deferred to 3.0.1 by Q-040**; in 3.0.0 the predicates are as shipped and the consequence is recorded in §11.2. | phase-12 D-01, D-04 for Challenge (from 3.0.1) |
| GD-SB-06 | Classic is unchanged in 3.0.0 (R8): the directive says "on challenge". Q-037 (non-blocking, queued by the producer) asks only whether Classic adopts the bands relative to each board's floor in 3.1. | — |
| GD-SB-07 | Results headline tiers for Challenge: 3 stars "Perfect!", 2 "Solved!", 1 "Cleared!"; the STUCK tier remains only as the defensive branch for a route reached with an uncleared peg count. | phase-2 D-08 (names the 1-star tier) |
| GD-SB-08 | Information rules for the two surfaces: a 1- or 2-star Challenge Results screen states how many pegs the next star needs; the Challenge stuck banner states the pegs that remained and that three or fewer clears the level (§16). Wording and layout are the ux-designer's. | — |
| GD-SB-09 | The ladder shift: `LEVEL_OFFSET = 30` (Q-040; the first edition's 50 is superseded). Level N deals what the previous curve dealt at N + 30; level 1 plays like the old level 31; level numbers unchanged, nothing deleted or renumbered ("i remove i meant shift"). Seeds stay keyed on the visible level (§7.1, §7.2). | phase-3 D-05's "levels ~1–10 are easy/tutorial-ish" (the tutorial now carries that job); this entry's first edition |
| GD-SB-10 | Levels 1–4 are fail-proof (G6, `FAIL_PROOF_LEVELS = 4`). **[3.0.1]** — deferred by Q-040; the 3.0.0 deviation is recorded in §7.4 and GD-SB-21. | — (makes rules/puzzle-design.md's onboarding rule true of Challenge when it lands) |
| GD-SB-11 | Freeform intro at visible level 10; from it, freeform and canonical are drawn with equal probability per attempt (G7). **[3.0.1]** — deferred by Q-040; 3.0.0 keeps the shipped intro at 40 and the freeform takeover. | phase-3 D-08 as built (freeform took over entirely from level 40) |
| GD-SB-12 | `GENERATOR_VERSION` becomes 2 for the shift; golden fixtures regenerated; fuzz at 100,000 on the release branch; no parallel v1 generator kept (§8 I7). The 3.0.1 items bump it to 3. | the `GeneratorConstants` KDoc's "a bump ships as a parallel generator" — moot while no store build exists |
| GD-SB-13 | Migration: numbers kept, results not re-graded, old snapshots resume on their stored board and an old-stamp restart regenerates (tech-lead ruling, brief §1(b)), cloud schema unchanged, Play Games untouched, Room not bumped (§12.3). | — |
| GD-SB-14 | The Challenge cleared test is one `:domain` function read by `GameViewModel`, `ResultsViewModel` and `ResultsUiState` (§4). | extends the CR-01 ruling of phase 10 |
| GD-SB-15 | Every generated `BoardSpec` carries `minimumPegs == 1` (I5, G8); the Diamond-41 footprint never carries the Classic floor into Challenge. In 3.0.0 this holds by the pool's reach and is asserted by the fuzz harness; the by-construction copies and the resume-path floor by mode are **[3.0.1]** (Q-040). | — |
| GD-SB-16 | Interstitial eligibility keeps its meaning: every Results entry is a win; a stuck board never reaches Results (§11.4). | — (confirms phase-10 D-14) |
| GD-SB-17 | The tutorial and its copy are unchanged; no text about stars is added anywhere before the first graded Results screen (§10). | — |
| GD-SB-18 | In Challenge the proven-dead banner says one peg is out of reach and that the level may still be cleared with two or three pegs; it never says the level is lost (§9). | extends phase 8's Diamond-41 ruling; phase-8 D-02's banner meaning for Challenge |
| GD-SB-19 | Ux-spec D-SB-4 acknowledged: the Challenge proven-dead banner may be dismissed by a tap outside its buttons. Rule boundary: a dismissal clears only the proof flag, is never charged, counted or undoable and plays no cue; the proof stands and a Hint on the same position raises the banner again, free; the stuck banner (R6) is never dismissable in either mode; Classic's proven-dead banner is unchanged (§9). | — |
| GD-SB-20 | Ux-spec D-SB-2 acknowledged: the Results next-star line names the next band up only ("Leave 2 pegs for 2 stars." at one star, "Leave 1 peg for 3 stars." at two) and both numbers come from the clear function through `ResultsUiState`. This supersedes §16's earlier two-band proposal at one star; GD-SB-08's information rule (the peg count of the next band up) is met, and the three star slots show the whole scale. | §16 (first edition) |
| GD-SB-21 | The founder's Q-040 re-scope (2026-11-01: "let the player get 3 stars. remove first like 30 levels"; "i remove i meant shift") is recorded as the founder's decision: a difficulty shift of thirty levels with numbers unchanged, and G6, G7, the pool copies and GD-SB-05 deferred to 3.0.1. **Recorded deviation from rules/puzzle-design.md ("first failure impossible before puzzle 5"), approved by Q-040:** in 3.0.0 Challenge levels 1–4 — five-peg, four-jump English-33 boards (old level 31's difficulty) — can be finished uncleared by a one-jump dead end; mitigations and the exact exposure are in §7.4. The deviation closes with G6 in 3.0.1. | GD-SB-09 (50 → 30), GD-SB-10, GD-SB-11, GD-SB-05 and GD-SB-15 as dated above |

**Rulings recorded elsewhere and in force.** GD-HT-1…6, the hint outcomes
(`.planning/reports/phase-14/game-designer-hint-timeout.md`); the tutorial boards T1–T3
(`.planning/reports/phase-15/game-designer-tutorial-boards.md`); the TalkBack board
(`.planning/reports/ux-spec-folds/game-designer-talkback-board.md`, TB-1…12). When this file is
next revised they are transcribed here in full; until then the reports are the text of record
for those three.

**Considered and rejected.**

| Candidate | Why rejected |
|---|---|
| Bands relative to `minimumPegs` for Challenge (floor → 3, floor+1 → 2, floor+2 → 1) | Every generated level reaches one peg; the only footprint with a floor above 1 would be graded wrongly (§6.1). |
| A Results variant for the stuck board ("Out of moves", Retry) | Loses the board state the player needs to Undo; adds a surface the rescue banner already is; would make a loss reach the screen that banks wins and counts them for ads. |
| Advancing on 3 stars only, recording 1–2 as bests | Makes the bands decorative and the ladder as unforgiving as today; contradicts "if the user leaves 3 pegs, gets 1 star" read as a pass. |
| Shifting by 25 levels (level 1 = old 26, `S = 4`) — the first edition's rejection | A one-star clear would take only two jumps and the pool would hold one footprint; the first edition chose 50 for that reason. The founder's Q-040 shift of 30 sits in the same `S = 4` band for levels 1–20: the designer's reservation stands recorded here, and the founder's call governs (GD-SB-09, GD-SB-21). |
| Shifting by re-tuning the curve's base constants instead of an offset | Same boards, more constants touched, and the "shift, don't re-tune" intent (§7.5) lost in the diff. |
| Renumbering stored progress by the offset | Undone by the next cloud merge; permanent disagreement with the ladder leaderboard; a visible demotion — and the founder's own word is "shift", not "remove". |
| Discarding old-version resume snapshots | The one option that loses a game; the stored board already carries everything needed to continue. |
| A tiered hint that finds a two- or three-peg line when one peg is proven dead | Right idea, new search target, new book semantics; 3.1 (§17). |
| Keeping freeform-first from level 10 (no mix) | Canonical shapes would vanish after nine levels; the pool widening would stay dead code. |

## 14. Invariants and their tests

Every invariant has a named test; names read as sentences (rules/testing.md). "Existing" means
the test is on `develop` at 37d3052 and must stay green (renamed where its sentence would
otherwise be false); "new" means the developer writes it, test-first. Domain tests are pure JVM
with no wall clock; the fuzz harness honours `FUZZ_SEEDS` and writes failing seeds to
`domain/build/fuzz-failures.txt`.

| # | Invariant | Test | Status |
|---|---|---|---|
| P1 | Solvable: every generated level's recorded line ends at one peg | `FuzzHarnessTest` (per level, `pegCount == 1`); `ChallengeSolvabilityTest.everyGeneratedSolutionReplaysLegallyAndFoldsToOnePeg` | existing, fixtures regenerated |
| P2 | Unique in the window of 8 | `QualityGateTest.noTwoBoardsInASlidingWindowShareASymmetryCanonicalKey` | existing |
| P3 | Seed-reproducible | `ChallengeGeneratorGoldenTest` (levels 1, 2, 10, 100, 1000 at version 2); `QualityGateTest.theDedupGateIsDeterministicAcrossRegeneration`; the fuzz harness's second-ladder equality | existing, fixtures regenerated |
| P4 | No unrecoverable state: a stuck board is rescuable by Undo or Restart | `GameViewModelTest.stuckThenUndoThenReSolveToWonEmitsExactlyOneFinishedEvent`; new `GameViewModelTest.undoFromAStuckChallengeBoardReturnsItToPlayingAndClearsTheBanner` | existing + new |
| P5 | Hints progress and tell the truth | `GetHintUseCaseTest` AC-1…AC-8 of the hint ruling | existing |
| P6 | Difficulty measurable and monotone | `DifficultyTierTest` (monotone, bounded deltas); new `DifficultyTierTest.levelOneDealsTheTierTheOldCurveDealtAtLevelThirtyOne`; new `DifficultyTierTest.everyLevelDealsTheTierTheOldCurveDealtThirtyLevelsLater` (property, levels 1..2000, `forLevel(n) == forLevel(n + LEVEL_OFFSET, levelOffset = 0)`); new `DifficultyTierTest.theSolutionLengthCapIsReachedAtLevelSixHundredAndFortySix` | existing + new |
| P7 | No trivial states | `QualityGateTest.everyGeneratedSolutionMeetsTheMinimumSolutionLengthFloor`; new `QualityGateTest.levelOneDealsFivePegsAndAFourJumpLine` | existing + new |
| P8 | Fuzz over the space | `FuzzHarnessTest` at `FUZZ_SEEDS=10000` (CI) and `100000` (release) | existing, assertions extended (I5, I7) |
| I5 | Every generated spec carries a one-peg target | `FuzzHarnessTest` asserts `startingBoard.spec.minimumPegs == 1` on every level; `BoardSpecTest.everyGeneratedChallengeFootprintKeepsTheDefaultOnePegTarget` | existing (sampled) + extended (every level) |
| I7 | Contract version 2 | `FuzzHarnessTest` and `ChallengeSolvabilityTest.everyGeneratedLevelStampsTheGeneratorVersion` assert 2 | existing, value changed |
| G6 | Levels 1–4 are fail-proof | **[3.0.1]** `QualityGateTest.levelsOneToFourCannotEndWithMoreThanThreePegs` (enumerate every reachable position; every terminal one has `pegCount <= 3`). Not written in 3.0.0 (Q-040); the deviation is recorded, not tested | 3.0.1 |
| G7 | Freeform mix | **[3.0.1]** `FreeformValidityTest.footprintsAreCanonicalBelowLevelTenAndMixedFromIt`, `theFreeformShareOverLevelsTenToOneThousandLiesBetweenThirtyFiveAndSixtyFivePercent`, `QualityGateTest.allFiveCanonicalFootprintsAppearByLevelThreeHundred`. In 3.0.0 the existing `FreeformValidityTest.footprintsAreCanonicalBelowTheIntroLevelAndFreeformAtIt` stays green unedited | 3.0.1; existing kept |
| R5 | Challenge stars by absolute peg count | new `ChallengeClearRuleTest.onePegLeftIsClearedWithThreeStars`, `twoPegsLeftAreClearedWithTwoStars`, `threePegsLeftAreClearedWithOneStar`, `fourOrMorePegsLeftAreNotCleared` (table-driven, 4..64), `aBoardWithALegalMoveIsStillPlayingWhateverItsPegCount`, `theClearRuleIgnoresTheFootprintsMinimumPegs` (a spec with floor 2 at two pegs reads two stars) | new |
| R8 / R11 | Classic bands and win test unchanged | `ScoreCalculatorTest.theDefaultTargetReproducesTheGlobalBandsAcrossTheWholePegDomain`; `GameViewModelTest.aDiamondFinishAtFivePegsIsOnlyOneStarAndNeverReachesResults`; new `GameViewModelTest.aClassicBoardEndingAboveItsFloorIsStillStuckUnderTheChallengeRule` | existing + new |
| R6 / R7 | Clears advance, stuck boards do not | new `GameViewModelTest.aChallengeBoardEndingWithTwoPegsEmitsAFinishWithTwoStarsAndAdvancesTheLadder`, `…WithThreePegsEmitsAFinishWithOneStarAndAdvancesTheLadder`, `aChallengeBoardEndingWithFourPegsIsStuckEmitsNoFinishAndDoesNotAdvance` | new |
| GD-SB-14 | One cleared test | new `ResultsViewModelTest.aOneStarChallengeClearIsRecordedSubmittedAndOffersNextLevel`; new `ResultsViewModelTest.aChallengeRouteWithFourPegsRecordsNothingAndShowsRetry`; `ResultsViewModelTest.aStuckTwoPegFinishOnAOnePegBoardIsStillNotRecorded` is **retired** (its sentence is false under R5) and replaced by the first | new |
| §6.4 | Bests merge | `ProgressMergeTest` (laws); new `ProgressRepositoryTest.aLaterThreeStarClearUpgradesAOneStarBest`, `aLaterOneStarClearLeavesAThreeStarBestUntouched` | existing + new |
| GD-SB-05 | Skill awards need 3 stars | **[3.0.1]** `PlaySessionDaoTest.aOneStarHintFreeClearDoesNotCountAsAHintFreeSolve`, `aThreeStarHintFreeClearCountsAsAHintFreeSolve`, `aOneStarClearUnderSixtySecondsDoesNotCountAsASpeedSolve`. In 3.0.0 `PlaySessionDao` and its tests are untouched | 3.0.1 |
| GD-SB-16 | Eligibility | new `ResultsViewModelTest.aOneStarChallengeClearIsInterstitialEligible`; `InterstitialCadenceStateTest` unchanged | new |
| GD-SB-07/08 | Copy | new `ResultsScreenTest.aOneStarChallengeResultShowsTheClearedHeadlineAndTheNextStarLine`, `aTwoStarChallengeResultShowsTheNextStarLine`, `aPerfectChallengeResultShowsNoNextStarLine`; new `aStuckChallengeBoardShowsTheChallengeStuckMessageWithItsPegCount` in the game screen's existing test files or a new `GameScreenRescueBannerTest` (there is no `GameScreenTest`; ux-spec §4.4.3); existing `ResultsScreenTest.solvedChallengeShowsExactlyOneNextLevelAndNoReplayOrRetry` | new + existing |
| GD-SB-13 | Old snapshots resume; an old-stamp restart regenerates | new `GameViewModelResumeTest.aSnapshotStampedWithGeneratorVersionOneRestoresOnItsStoredBoard` (the stamp is a ViewModel property — the mapper never reads it, tech-lead F-5), `restartOfAResumedOldStampLevelServesTheLevelsCurrentBoardAndItsRecordedLine`, `restartOfAResumedCurrentStampLevelKeepsItsStoredBoardWithoutRegenerating`; new `SnapshotMapperTest.aSnapshotStampedWithAnotherGeneratorVersionPassesTheStampThroughUntouched`; `MigrationTest` (v1 → v2 rows survive). **[3.0.1]** with the pool copies: `SnapshotMapperTest.aChallengeBlobWithoutAStoredTargetOnTheDiamondFootprintResolvesToOnePeg`, `aClassicBlobWithoutAStoredTargetOnTheDiamondFootprintKeepsItsFloorOfTwo` | new + existing; two in 3.0.1 |
| GD-SB-18 | Banner copy | new `aProvenDeadChallengeBoardSaysOnePegIsOutOfReachNotThatTheLevelIsLost` (same placement as the GD-SB-07/08 row) | new |
| GD-SB-19 | Dismissal clears only the proof flag; the stuck banner never dismisses | new `GameViewModelTest.dismissingTheProvenDeadBannerClearsOnlyTheProofFlagAndChargesNothing` (flag false; `stuck`, allowances, counters, clock, board and selection unchanged; a second Hint on the same position returns Proven dead again, free); new `NoWinBannerTest.onlyTheChallengeProvenDeadBannerCarriesTheDismissAction` (the stuck banner in both modes and Classic's proven-dead banner carry none) | new |

Coverage: `:domain` line coverage ≥ 90 % (kover, tech-design §8.4) must hold after the change.

## 15. Acceptance criteria

Given/When/Then, one per observable behaviour, each mapped to a §14 test or a QA device step.
Ids are `SB-<layer><n>` so they cannot collide with the hint ruling's AC-n or the ux-spec's
criteria. The developer builds test-first against D, V, U and P; QA judges Q on a device or the
emulator and records an observed `result:` line per item, never a pre-ticked box.

### Domain (`:domain`, pure JVM)

- **SB-D1** Given a Challenge board with no legal move and 1, 2 or 3 pegs, when the clear rule
  is applied, then it is cleared with 3, 2 or 1 stars respectively. (R5 tests, §14)
- **SB-D2** Given a Challenge board with no legal move and 4..64 pegs, when the clear rule is
  applied, then it is not cleared and reports no star count. (`fourOrMorePegsLeftAreNotCleared`)
- **SB-D3** Given a Challenge board with at least one legal move and any peg count, when the
  rule is applied, then it is still playing. (`aBoardWithALegalMoveIsStillPlaying…`)
- **SB-D4** Given a generated board whose footprint spec declares `minimumPegs = 2`, when it
  ends with 2 pegs, then it is cleared with 2 stars, not 3. (`theClearRuleIgnoresTheFootprints…`)
- **SB-D5** Given any level 1..2000, when its tier is computed, then every field equals what
  the pre-offset formula gives for level + 30; the freeform intro stays 40 against the visible
  level, as shipped. (`everyLevelDealsTheTierTheOldCurveDealtThirtyLevelsLater`)
- **SB-D6** Given level 1, when generated, then its starting board holds 5 pegs, its solution
  is 4 moves, and its footprint is English-33. (`levelOneDealsFivePegsAndAFourJumpLine`)
- **SB-D7** **[3.0.1]** Given levels 1–4, when every position reachable by legal jumps from the
  start is enumerated, then every position with no legal move holds at most 3 pegs. Not tested
  in 3.0.0 (Q-040; deviation §7.4). (`levelsOneToFourCannotEndWithMoreThanThreePegs`)
- **SB-D8** Given levels 1–39, when generated, then every footprint is canonical — English-33
  at 1–20, English-33 or European-37 at 21–39 — and from level 40 every footprint is freeform,
  as shipped (the existing `footprintsAreCanonicalBelowTheIntroLevelAndFreeformAtIt`). **[3.0.1]**
  the intro at 10, the accepted share in `[0.35, 0.65]` over 10..1000 and all five canonical
  footprints by level 300 (G7 tests).
- **SB-D9** Given the whole level space at `FUZZ_SEEDS=100000`, when each level is generated,
  validated and its line replayed, then every line ends at one peg, every spec carries
  `minimumPegs == 1`, every level is stamped version 2, and a second ladder is identical.
  (`FuzzHarnessTest`)
- **SB-D10** Given the five golden fixtures regenerated at version 2, when the generator runs,
  then levels 1, 2, 10, 100 and 1000 match them byte for byte. (`ChallengeGeneratorGoldenTest`)
- **SB-D11** Given a stored best of 1 star and a new play of 3, when merged, then stars read 3;
  given a stored 3 and a new 1, then stars read 3, and only the fields that improved report
  "new best". (`aLaterThreeStarClearUpgrades…`, `aLaterOneStarClearLeaves…`)
- **SB-D12** Given Classic, when `ScoreCalculator.stars` is evaluated across the whole peg
  domain for every shipped `minimumPegs`, then the table is byte-identical to today's. (R11 test)

### ViewModel (`:app` JVM, `runTest` and Turbine)

- **SB-V1** Given a Challenge level and a terminal board with 2 pegs, when the last jump lands,
  then one finish is emitted with 2 stars, the snapshot is cleared, and `advancePast(N)` is
  called once; the same with 3 pegs yields 1 star. (R6/R7 tests)
- **SB-V2** Given a Challenge level and a terminal board with 4 pegs, when the last jump lands,
  then no finish is emitted, `stuck` is true, the ladder is not advanced, the timer is frozen,
  and Undo returns the board to playing with the banner cleared. (R6 and P4 tests)
- **SB-V3** Given a Classic board that ends above its floor, when the last jump lands, then it
  is stuck exactly as today — no finish, no record. (`aClassicBoardEndingAboveItsFloor…`)
- **SB-V4** Given a Results entry for a Challenge level with 3 pegs, when constructed, then the
  result is recorded once with 1 star, the submitter is called once, the headline is CLEARED,
  Next level is offered, and the entry is interstitial-eligible. (`aOneStarChallengeClear…`)
- **SB-V5** Given a Results route forged with a Challenge level and 4 pegs, when constructed,
  then nothing is recorded or submitted, the headline is STUCK and Retry is offered.
  (`aChallengeRouteWithFourPegsRecordsNothing…`)
- **SB-V6** **[3.0.1]** Given a `play_session` with a hint-free, undo-free, 20-second play at
  1 star, when the skill flags are read, then none of HINT_FREE, UNDO_FREE or SPEED_SOLVE is
  earned; given the same at 3 stars, then all three are. In 3.0.0 the flags are as shipped and
  such a play earns all three (§11.2, recorded). (GD-SB-05 tests)
- **SB-V7** Given a resume snapshot stamped `generatorVersion = 1` with a valid history, when
  restored, then the stored initial board replayed by that history is on screen with undo
  available and the elapsed time carried; the level number is the stamped one; a Hint request
  goes to the solver (no recorded line is loaded on a resume).
  (`GameViewModelResumeTest.aSnapshotStampedWithGeneratorVersionOneRestoresOnItsStoredBoard`)
- **SB-V8** Given a resumed Challenge level whose stamp is not the current generator version,
  when the player restarts, then the level's current board and its recorded line are served
  through the level-load path; given a current-stamp resume or a fresh game, then Restart keeps
  the stored board without regenerating.
  (`restartOfAResumedOldStampLevelServesTheLevelsCurrentBoardAndItsRecordedLine`,
  `restartOfAResumedCurrentStampLevelKeepsItsStoredBoardWithoutRegenerating`)
- **SB-V9** Given a Challenge board under the proven-dead banner, when the player dismisses the
  banner, then only the proof flag clears — `stuck`, the allowances, the assist counters, the
  clock, the board and the selection are unchanged, nothing is charged or counted — and a Hint
  tapped on the same position returns Proven dead again at no charge.
  (`dismissingTheProvenDeadBannerClearsOnlyTheProofFlagAndChargesNothing`)

### UI (Compose UI tests)

- **SB-U1** Given a 1-star Challenge result, when rendered, then the headline reads
  `results_headline_cleared`, one filled star shows, and the next-star line is present; given
  2 stars, the next-star line is present; given 3, it is absent. (§14 copy tests)
- **SB-U2** Given a stuck Challenge board with 5 pegs, when rendered, then the banner shows the
  Challenge stuck message with "5" in it and Undo, Restart and Home; `stuck_message` (Classic)
  does not show. (`aStuckChallengeBoardShowsTheChallengeStuckMessage…`)
- **SB-U3** Given a proven-dead hint on a Challenge board, when rendered, then the banner text is
  the Challenge proven-dead message and contains no word meaning "lost".
  (`aProvenDeadChallengeBoardSaysOnePegIsOutOfReach…`)
- **SB-U4** Given TalkBack, when a 1-star Results opens, then the summary announced once is
  "Cleared! — 1 of 3 stars, 3 pegs left." in the existing `results_a11y_summary` shape, and the
  next-star line is a stop. (ux-designer's TalkBack script; QA SB-Q7)
- **SB-U5** Given the Challenge proven-dead banner, when the player taps its message or padding,
  then the banner leaves and the board plays; given the stuck banner in either mode or Classic's
  proven-dead banner, then no such tap removes it. (`onlyTheChallengeProvenDeadBannerCarriesTheDismissAction`)

### Persistence (instrumented or Robolectric where the DAO is exercised)

- **SB-P1** Given a v1 database seeded with Challenge rows at 3 stars and a ladder of 30, when
  the app updates to the 3.0.0 build, then every row and the ladder read unchanged.
  (`MigrationTest`; no new migration)
- **SB-P2** Given a cloud payload at `schemaVersion = 1` carrying a 1-star Challenge best, when
  pulled and merged, then the local row reads the max of the two and nothing is dropped.
  (`ProgressMergeTest`, codec round-trip test)

### Device (qa-engineer; emulator per the founder's 2026-10-31 directive, a phone if attached)

- **SB-Q1** Fresh install, play Challenge level 1 to a 3-peg terminal board: Results shows
  "Cleared!", one star, the next-star line, Next level; Home's Stars pill shows 1 and the Level
  pill 2. Record the board shape (the English cross) and the peg count 5.
- **SB-Q2** On level 2, undo twice from a 2-peg board and finish at 1 peg: "Perfect!", three
  stars, no next-star line; level_result for level 2 reads 3 stars.
- **SB-Q3** Replay level 1 (Home → Continue serves the head; reach level 1 by finishing a run or
  by the debug picker if it ships): finish at 1 peg; Results reports "New best!" on stars; the
  Stars pill rises by 2.
- **SB-Q4** On level 5 or later, reach a board with no move and 4 or more pegs: the banner
  shows the Challenge stuck message with the exact peg count; no Results; the Level pill is
  unchanged; Undo clears the banner; Restart reloads the level; the clock is frozen while the
  banner is up.
- **SB-Q5** 3.0.0 — informational, no pass line: on level 1, look for a one-jump dead end (a
  first jump after which no move is legal); if the board has one, make it and record the banner
  text (the count 4 and the threshold 3) and that Undo clears it. The deviation is recorded
  (§7.4); the fail-proof check ("never above 3 on levels 1–4") is **[3.0.1]**.
- **SB-Q6** Tap Hint on a Challenge position proven dead (the developer supplies a sequence):
  the banner says one peg is out of reach and that the level may still be cleared; tap its
  message — it leaves, no cue plays, the hint allowance is unchanged; tap Hint again — it
  returns at once; then finish the board to 2 or 3 pegs and confirm Results opens with 2 or 1
  stars. On a stuck board (SB-Q4) a tap on the banner's message does nothing.
- **SB-Q7** TalkBack on: SB-U4's announcement on a 1-star Results; the stuck banner's message is
  read when it appears.
- **SB-Q8** Signed in: after a 1-star clear of level N, the Challenge Ladder leaderboard shows
  N (server-side view). In 3.0.0 a 1-star hint-free play *does* unlock "No Hints Needed" (the
  recorded consequence of Q-040, §11.2) — observe and record it; the 3-star gate and its
  "no unlock at 1 star" check are **[3.0.1]**.
- **SB-Q9** Upgrade path: install the pre-change build, play to level 12 with a half-finished
  level 12 and a cloud save, install the 3.0.0 build over it: Home shows level 12, Continue
  resumes the half-finished board with its moves and time, Hint on it answers (Found, Undecided
  or Proven dead — never a crash), Restart serves a different board; the Stars pill is unchanged;
  the cloud save merges with no prompt.
- **SB-Q10** Classic: finish English-33 at 2 pegs — the banner, not Results, exactly as before
  the change; finish Diamond-41 at 2 pegs — "Perfect!", three stars, as before.
- **SB-Q11** Interstitial: with the cadence past its first-session and first-five suppression,
  Continue after a 1-star clear can present an interstitial; a stuck board can never lead to
  one (there is no Continue).

## 16. Copy contract

Information rules are the game-designer's; the final wording, its tr-TR translation, TalkBack
announcements and layout are the ux-designer's, within these rules — and are now decided:
`docs/ux-spec.md` §3 D-SB-1…6, §4.4.3 and §4.6 (b8e4c8c). The en-US column below is the decided
copy; where it differs from this file's first edition the ux-designer's wording stands, and the
two differences are recorded as GD-SB-19 and GD-SB-20 (§13). Every string lands in
`app/src/main/res/values/strings.xml`; the app has no `values-tr` today, and the Turkish copy of
record for the four new strings is the ux-spec's §8 D-SB table (D-SB-6), which
`feature/turkish-locale` carries into `values-tr`. This table is the complete list of strings the
change adds, changes or leaves alone.

| Resource | Status | en-US (decided, ux-spec D-SB-1…3) | Rule it must satisfy |
|---|---|---|---|
| `results_headline_cleared` | **new** | "Cleared!" | names a pass that is not a solve; one word plus the house exclamation mark, like its two siblings (D-SB-1) |
| `results_next_star_hint` | **new**, one plural resource | "Leave 2 pegs for 2 stars." after a one-star clear; "Leave 1 peg for 3 stars." after a two-star one — the next band up only; both numbers from the clear function through `ResultsUiState`, never typed (D-SB-2, GD-SB-20) | states the peg count of the next band up; no "try again", no "almost", no time or score advice |
| `stuck_message_challenge` | **new**, plurals | "No moves left with %1$d pegs — leave %2$d or fewer to clear the level. Undo or restart." (`%2$d` = `CLEAR_MAX_PEGS`; shown with 5 and 3) (D-SB-3) | names the exact count and the threshold; keeps the banner's "problem — what to do" shape; never "lost", "failed", "game over" |
| `nowin_message_challenge` | **new**, plurals | "One peg is out of reach from here — you may still clear the level with %1$d pegs or fewer, or undo or restart." (`%1$d` = `CLEAR_MAX_PEGS`) (D-SB-3) | GD-SB-18: one peg out of reach *from here*; a clear *may* still be possible (no promise); never "lost". Dismissable by a tap outside its buttons (D-SB-4, GD-SB-19) |
| `results_headline_perfect` / `_solved` | unchanged | "Perfect!" / "Solved!" | now reachable in Challenge at 3 and 2 stars |
| `results_headline_stuck` | unchanged | "Out of moves" | defensive branch only; never shown in play |
| `action_next_level`, `action_retry`, `action_home` | unchanged | | Next level on every clear; Retry on the defensive branch only |
| `stuck_message`, `nowin_message` | unchanged | | Classic only after this change |
| `catalog_stars_earned` ("%1$d of %2$d stars") | unchanged | | the Results star row's TalkBack stop, now "1 of 3 stars" and "2 of 3 stars" occur |
| `results_a11y_summary` | unchanged | "%1$s — %2$s, %3$s." | "Cleared! — 1 of 3 stars, 3 pegs left." |
| `results_label_pegs_left`, `results_new_best` | unchanged | | "New best!" now fires on star upgrades |
| `tutorial_step_3_caption`, `tutorial_done_caption` | unchanged | "Keep jumping. Clear the board down to one peg." / "That's the whole game. Go clear a board." | GD-SB-17: the ideal is still one peg |
| `home_stat_stars_label` and the ribbon | unchanged | | |
| `game_over_perfect_headline`, `_body`, `_near_headline`, `_stuck_headline` | orphaned today | | not this change's to delete; noted for the ux-spec's Deleted list |

**Store and changelog (release-manager owns the text; these are the claims the rule allows).**

- `docs/store/*/full_description.txt` line 19 "Finish a level with one to three stars" becomes
  true in Challenge for the first time; line 7 "a ladder of 10,000 generated levels" stays true
  (the ceiling is unchanged); line 1 "ideally just one" stays. No store sentence needs to change;
  one may be added under "Two ways to play": "Clear a Challenge level with three pegs or fewer —
  one star for each peg fewer." and its tr-TR mirror.
- `docs/store/*/changelogs/30000.txt` (500 characters, Play): one clause may be added if room
  remains: "Challenge: finish with three pegs or fewer to clear a level". Not required.
- `CHANGELOG.md` 3.0.0, under *Changed*: "**Challenge grades a finish by the pegs you leave.**
  One peg is three stars, two pegs two, three pegs one, and any of them clears the level and
  unlocks the next; four or more is still a stuck board you can undo or restart. The opening
  levels, where any single jump was already a pass, are gone: level 1 is now a six-peg puzzle."
- Not allowed: "every level has a solution" claims stay as they are (true); no copy may say a
  stuck board is "lost" or that hints find a two-peg line.

**TalkBack (ux-designer's script; rules only).** The stuck and proven-dead banners are polite
live regions already; their new messages are read as they appear. The Results summary keeps its
one-announcement latch. The next-star line is a focusable stop after the star row.

## 17. Roadmap constraints

**What ships, and in which release.** The founder named 3.0.0, and `release/3.0.0` has been
cut (`CHANGELOG.md`, `STATUS.md` 2026-11-01). The change is one feature branch
(`feature/challenge-star-bands`, cut from `develop` 37d3052) that gsd plans as one phase —
**Phase 16 (proposed), "Challenge star bands and ladder shift"** — carrying, for 3.0.0,
CSB-01…05, CSB-07, CSB-08 and CLC-01, CLC-04, CLC-05, CLC-06 (`.planning/REQUIREMENTS.md`).
CSB-06, CLC-02 and CLC-03 are 3.0.1 (Q-040), a proposed Phase 17. Whether it merges into `release/3.0.0` or re-cuts the
release is the producer's and release-manager's call (Q-030 set the precedent for a founder
feature landing after the cut); this file only says what the phase delivers.

**Order inside the phase (each step green before the next).**

1. `:domain`: the clear rule and its tests (SB-D1…D4); `GameStatus` KDoc corrected.
2. `:domain`: `LEVEL_OFFSET = 30` and `GENERATOR_VERSION = 2`; fixtures regenerated with
   `UPDATE_GOLDEN=true`; `FuzzHarnessTest` extended with the `minimumPegs == 1` line and green
   at 10,000 (SB-D5, D6, D9, D10). G6, G7 and the pool copies are 3.0.1 — not in this commit.
3. `:app`: `GameViewModel` (mode-aware status, finish on every clear, stuck at 4+, old-stamp
   restart regenerates), `ResultsViewModel`/`ResultsUiState` (one cleared test, CLEARED tier)
   (SB-V1…V5, V7…V9). `PlaySessionDao` is untouched in 3.0.0.
4. `:app`: strings and the two banners, the next-star line (SB-U1…U4), with the ux-designer's
   wording.
5. `./gradlew check` green; code-auditor review; QA device pass SB-Q1…Q11; tech-lead
   sign-off; `FUZZ_SEEDS=100000` on the release branch (rules/testing.md).

**Owners.** game-designer: this file, the rule, the ACs' intent. ux-designer: ruled
(ux-spec b8e4c8c, D-SB-1…6, §4.4.3, §4.6, §8) — wording, tr-TR, TalkBack, the next-star line,
the proven-dead banner's dismissal; acknowledged here as GD-SB-19 and GD-SB-20. tech-lead: ruled on
2026-11-01 (`.planning/reports/challenge-star-bands/tech-lead-brief.md` §1; tech-design §7.1,
§7.4) — the cleared function's home and shape, the pool-spec copies, the resume and restart
mechanism, the old application's files; the sign-off transcribes M1–M5 into tech-design §7.3. android-game-dev: builds test-first. qa-engineer: judges.
release-manager: changelog and store lines (§16), the fuzz run at release settings. producer:
Phase 16 in `ROADMAP.md`, the todo closure, the founder question, `PROJECT.md` Key Decisions row.

**Deferred to 3.0.1 by the founder (Q-040), as one bundle and one contract bump (version 3).**

- The levels 1–4 fail-proof gate (G6, GD-SB-10) — closes the recorded onboarding deviation
  (§7.4, GD-SB-21).
- The freeform intro at 10 with the even draw (G7, GD-SB-11), which also revives the canonical
  pool widening.
- The pool copies with `minimumPegs = 1`, the Diamond-41 entry handled by construction, and the
  resume-path floor by mode (GD-SB-15; the tech-lead's §1(c) and C6).
- The three-star requirement on HINT_FREE, UNDO_FREE and SPEED_SOLVE (GD-SB-05).

**Deferred, named so nobody assumes it (3.1 candidates).**

- A tiered hint that, when one peg is proven unreachable, searches for a two- or three-peg
  line (§9). Needs a new solver target and book semantics; it also raises Undecided rates.
- Classic alignment with the band model — only if the founder answers Q-037 that way, and 3.1
  at the earliest; 3.0.0 ships Classic unchanged.
- A star forecast in the gameplay status strip (§10).
- Curve re-tuning by playtest (D-07) — the cut shifts the curve, it does not re-tune it.
- A per-level best of "fewest pegs left" alongside stars — redundant with stars under R5;
  revisit only if the bands ever stop being one-to-one with peg counts.

**Out of scope, unchanged.** Diagonal jumps; a level select or skip; lives or energy; any rule
that reads the clock into stars (phase-2 D-03's independence stands); Classic's rules (R8).

**Documents this change makes stale, and who fixes them.** `GameStatus.kt` KDoc (developer,
with the change); `GeneratorConstants.kt` KDoc on parallel generators (developer, with the
bump); `ResultsViewModel`/`ResultsUiState` KDocs that say "every generated footprint has
`minimumPegs == 1`" become true by I5 and should cite it (developer); `.planning/PROJECT.md`
Key Decisions "Stars from pegs remaining" row (producer, one line); phase-3 D-02/D-10/D-11 and
phase-12 D-01/D-04 are superseded as §13 says (no edit to archived CONTEXT files — this file is
the record); `docs/ux-spec.md` §2 row 6 "Results | Unchanged" and §4.4's control-state table
(ux-designer); `docs/tech-design.md` §7.1 and §7.3 (tech-lead).
