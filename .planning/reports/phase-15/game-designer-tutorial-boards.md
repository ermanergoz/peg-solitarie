# game-designer report — phase-15 tutorial board content (ux-spec §7 R-4)

Date: 2026-09-21
Branch: feature/phase-15-ux-refresh (5635d8c..this commit — one `docs(15):` commit)
Skills loaded: ergoz-soft:standards (role: game-designer), the-fool
Rules loaded: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md,
puzzle-design.md, play-store.md, kotlin-android.md, compose.md, ip-policy.md, lessons.md
Ran in parallel with: phase-14 (in flight in other worktrees — not touched)
Loops: 0
Lessons applied: none

## Summary
Ruled on the tutorial's three micro-puzzles. T1 is kept as specified; **T2 and T3 are replaced** —
the designer's T2 default is not dead-end-free (it strands the player on two pegs) and lets the
player satisfy the step with a second *horizontal* jump, so acceptance criterion 2 was not
structurally guaranteed. All three chosen boards are proven dead-end-free to one peg by exhaustive
enumeration of every reachable position, not by argument.
Boards are **bespoke miniatures, not Classic-catalogue geometry** — no catalogue board can be
dead-end-free, and the smallest is 33 holes.
One of my own candidates (a 3×5 ring) was killed by the harness; that is recorded below rather
than hidden.

## Outputs
- `.planning/reports/phase-15/game-designer-tutorial-boards.md` — this report: the ruling for
  ux-spec §7 R-4, with the proof, the proposed §4.1/§4.4/§8 diff, and the named permanent test.
- No file under `app/`, `domain/`, `docs/` or `.planning/{ROADMAP,questions}` was changed. The
  proof harness was deliberately deleted after it was run (see Checks).

---

## 1. The ruling — the three boards

Coordinates are `Position(row, col)`, the domain's own type
(`domain/src/main/kotlin/com/app/pegsolitaire/domain/model/Position.kt:8`). "In play" means a cell
present in `BoardSpec.inPlay`; every in-play cell is either a peg or a hole at the start.
All three use `target = null` and the default `minimumPegs = 1`
(`BoardConstants.kt:23`) — these are teaching boards with no designated finishing hole.

### T1 — kept exactly as the ux-spec defaults it

```
P P .          in play: (0,0) (0,1) (0,2)
               pegs:    (0,0) (0,1)
               holes:   (0,2)
```

- **Teaches:** tap a peg, tap the empty hole two cells away, the peg in between disappears.
- **Why the layout teaches it without text:** there is exactly one legal move on the board, so the
  only thing the player *can* do is the thing being taught. With `showHints` forced on (D-01b),
  selecting the left peg draws exactly one destination dot.
- **Target:** one peg. **Intended line:** 1 move. **Alternatives:** none — the board has one
  maximal line.
- **Axis:** horizontal.
- This is the footprint already in the codebase (`OnboardingOverlay.kt:156-171`), so keeping it
  costs nothing and D-01a's claim that T1 is "trivially correct" holds.

### T2 — replaced

```
. P .          in play: all nine cells of the 3×3 grid
. P .          pegs:    (0,1) (1,1)
. . .          holes:   (0,0) (0,2) (1,0) (1,2) (2,0) (2,1) (2,2)
```

- **Teaches:** the same jump works on the **other axis**, and the destination is the hole *two*
  cells beyond a peg — never the hole next door, never a diagonal.
- **Why the layout teaches it without text:** exactly one legal move exists and it is vertical, so
  the player cannot complete the step any other way. The footprint is the **full** 3×3 square, so
  the four diagonal holes and the two horizontally-adjacent holes are all visibly in play and all
  visibly receive **no** destination dot when the top peg is selected. That negative information is
  what earns the spec's caption "Jumps work up, down, left and right — never diagonally"
  (`tutorial_step_2_caption`); a plus-shaped footprint cannot earn it, because there is no diagonal
  cell there to withhold.
- **Target:** one peg. **Intended line:** 1 move. **Alternatives:** none.
- **Axis:** vertical.
- **Why the designer's default was rejected.** The default (5-cell plus, pegs at (0,1),(1,0),(1,1))
  has two legal moves, **one horizontal and one vertical, and both complete the step**. Two
  consequences, both proven by the harness:
  1. It is **not dead-end-free**: `worstTerminalPegs = 2`. Whichever move the player picks, the
     board ends on two pegs with no move left. For a step whose completion condition is "any legal
     jump lands" that is harmless, but it makes T2 the only step that does not end on one peg and
     so it quietly contradicts the goal T3 then has to teach.
  2. **Acceptance criterion 2 was not structural.** T1 is horizontal; if the player also picks the
     horizontal move in T2, they finish the tutorial having performed no jump on the other axis
     unless T3 happens to force one. Criterion 2 says the player must *perform* one of each. With
     T2 forced vertical, T1 + T2 satisfy criterion 2 on their own, and T3 satisfies it again
     independently — the criterion survives any later change to T3.

### T3 — replaced

```
P P .          in play: all nine cells of the 3×3 grid
P P P          pegs:    (0,0) (0,1) (1,0) (1,1) (1,2)
. . .          holes:   (0,2) (2,0) (2,1) (2,2)
```

- **Teaches:** the goal — keep jumping until **one peg** is left.
- **Why the layout teaches it without text:** it is the board T2 just introduced, now filled, so
  no new geometry has to be read; it has **three** opening moves and **all of them win**, so the
  player learns that the game is a sequence of ordinary jumps and that their own choices are safe;
  and it empties visibly, four pegs at a time disappearing until one is left, which is the goal
  stated as a picture rather than a sentence.
- **Target:** one peg, and **every one of its four maximal lines reaches it**.
- **Intended line:** 4 moves. **Alternatives:** there are 4 distinct maximal lines and **every one
  is also 4 moves long** — no line is longer or shorter. This is not luck: every legal jump removes
  exactly one peg, so any line from 5 pegs to 1 peg is exactly 4 moves, and because the board is
  dead-end-free there is no shorter line that stops early. All four lines converge on the **same**
  final cell (`terminalPositions = 1`), so the ending looks designed rather than arbitrary.
- **Axes:** every line contains at least one horizontal and at least one vertical jump
  (`everyLineUsesBothAxes = true`).
- **Reachable positions:** 10 — small enough that the proof is exhaustive by inspection as well as
  by execution.

### Axis coverage (ux-spec §14 criterion 2) — satisfied twice over

| Step | Legal openings | Axis the player must use |
|------|----------------|--------------------------|
| T1 | 1 | horizontal (forced) |
| T2 | 1 | vertical (forced) |
| T3 | 3 | both, on every line |

Criterion 2 ("at least one horizontal jump and at least one jump on the other axis, on a live
board, before the tutorial ends") therefore holds **structurally** — it cannot be defeated by any
sequence of taps, and it does not depend on T3.

### First failure impossible, structurally (D-01c, rules/puzzle-design.md)

For each of the three boards, every position reachable by any legal sequence of jumps was
enumerated and every terminal position (no legal move) was checked. In all three the worst
terminal position holds **exactly one peg**. There is therefore no tap sequence that strands the
player above the target, so "first failure impossible" is a property of the layouts and not of the
UI refusing taps — which is what the task required. Numbers in Checks.

---

## 2. Ruling: bespoke miniatures, not Classic-catalogue geometry

**Bespoke miniatures**, drawn deliberately from the catalogue's visual vocabulary. Four reasons, in
descending weight:

1. **No Classic board can be dead-end-free, so no Classic geometry can satisfy D-01c.** The GDD
   designs for the opposite: "if the player has reached an unsolvable state, the hint surfaces 'no
   winning move — undo or restart'"
   (`docs/superpowers/specs/2026-06-28-peg-solitaire-design.md:82`). Dead ends are a *feature* of
   the real game and the rescue banner exists for them. `Diamond41` cannot even reach one peg — it
   ships with `minimumPegs = DIAMOND41_FEWEST_PEGS_FLOOR = 2` (`Boards.kt:283-294`,
   `BoardConstants.kt:42`). A tutorial on catalogue geometry could strand the player on their
   second tap.
2. **The smallest catalogue board is 33 holes.** `Boards.CLASSIC_SPECS` is
   `English33, European37, Asymmetric39, Diamond41, Wiegleb45` (`Boards.kt:323`). English-33 sits
   in a 7×7 box; at `tutorial_board_size` 280dp (ux-spec §6.1) that is 40dp per cell — below the
   48dp touch-target floor the same spec mandates in §11. It is neither readable nor tappable at
   tutorial size.
3. **Dead-end-freeness only exists at miniature scale.** The exhaustive search below found that
   inside a 3×3 bounding box the largest dead-end-free board has **5 pegs** (32 such boards), and
   inside a 3×4 box **8 pegs** (4 such boards, all with a single forced opening). It is not a
   property you obtain by shrinking a Classic board; it is a property you build a miniature around.
4. **But the miniatures should still look like the game.** T1's 1×3 strip is already in the code;
   the 3×3 square used by T2 and T3 is the centre block of the English cross and the silhouette
   the Classic catalogue's previews and the Home tile art are built from. The player's eye is
   prepared for the catalogue without the tutorial inheriting a board that can strand them.

**Implementation consequence the developer must honour:** the three tutorial specs go in a new
`object TutorialBoards` in `:domain` beside `Boards.kt`, and **must not** be added to
`Boards.CLASSIC_SPECS`. That list is the Classic catalogue and is the denominator of the Home
"Boards" pill (ux-spec §7 R-2: "the 5 `ClassicBoard` enum values"); adding tutorial footprints to
it would change a shipped number and put three teaching boards on the catalogue screen.

---

## 3. Proposed diff to `docs/ux-spec.md` (I did not edit the file)

The ux-designer owns that document; these are the edits my ruling implies, for the producer to
route. Nothing outside §4.1's step table, §4.4's class-3 row and §8's string table is affected —
in particular **no token changes**: a 3×3 board inside `tutorial_board_size` 280dp gives ≈93dp
cells, comfortably above the 48dp floor, so §6.1 stands unchanged.

### §4.1 — replace the three rows of "The three steps"

```diff
-| **T1** | Tap a peg, tap a hole two cells away; the middle peg goes | 1×3 strip: pegs at (0,0),(0,1), hole at (0,2) — exactly one legal move | the jump lands |
-| **T2** | Jumps work on the other axis too; diagonals are never offered | 5-cell plus: in-play (0,1),(1,0),(1,1),(1,2),(2,1); pegs at (0,1),(1,1),(1,0); holes at (2,1),(1,2) — two legal moves, both correct | **any** legal jump lands |
-| **T3** | The goal is to leave one peg, and Undo exists | a small footprint whose every legal line reaches one peg | one peg remains |
+| **T1** | Tap a peg, tap a hole two cells away; the middle peg goes | 1×3 strip: in-play (0,0),(0,1),(0,2); pegs at (0,0),(0,1); hole at (0,2) — exactly one legal move, horizontal | one peg remains (1 move) |
+| **T2** | The same jump works on the other axis, and the destination is two cells away — never the hole next door, never a diagonal | full 3×3: all nine cells in play; pegs at (0,1),(1,1); the other seven are holes — exactly one legal move, vertical | one peg remains (1 move) |
+| **T3** | The goal is to leave one peg | full 3×3: pegs at (0,0),(0,1),(1,0),(1,1),(1,2); holes at (0,2),(2,0),(2,1),(2,2) — three legal openings, every line wins | one peg remains (4 moves) |
```

### §4.1 — replace the block-quote under the table

```diff
-> **Board content is the game-designer's call, not mine.** T1 is safe to fix here because it is the
-> footprint already in the codebase (`OnboardingOverlay.kt:156-165`) and is trivially correct.
-> **T2 and T3's footprints are a game-designer input requirement** (§7, R-4): they must be verified
-> one-peg-reachable-from-every-legal-line by the Solver so D-01c holds structurally rather than by
-> hope. The footprints above are the *defaults* so nothing blocks; a game-designer ruling replaces
-> them without touching anything else in this section.
+> **Board content is the game-designer's ruling** (R-4, closed 2026-09-21 —
+> `.planning/reports/phase-15/game-designer-tutorial-boards.md`). All three footprints are proven
+> dead-end-free to one peg by exhaustive enumeration of every reachable position, so D-01c holds
+> structurally: no sequence of taps can strand the player. T1 is unchanged from
+> `OnboardingOverlay.kt:156-171`. The step-completion predicate is therefore **the same for all
+> three steps — "the board holds one peg"** — so no step needs a bespoke completion rule.
```

### §4.1 — one sentence added to "Advance"

The uniform predicate is worth stating where the developer will read it:

```diff
 **Advance.** On completion the caption swaps, the pip advances, and the next footprint installs
 after one beat (`TutorialAnim.STEP_ADVANCE_DELAY_MS`) so the player sees their own jump land
 before the board changes. No button, no tap-to-continue.
+A step is complete when its board holds exactly one peg — the same test for all three steps.
```

### §4.4 — the T3 escalation row, and §8's `tutorial_undo_nudge`

**There is no Undo control on the tutorial screen.** §4.1's band table is Header / Board / Caption /
Footer; no dock, no Undo. So T3's old lesson "and Undo exists" had no affordance to point at, and
`tutorial_undo_nudge` ("Boxed in? Undo takes your last jump back.") names a control the player
cannot see and a situation — being boxed in — that these boards make **impossible**. Promising a
rescue from a failure that cannot happen teaches the player to expect failure.

My ruling: T3 teaches the goal only; Undo is met in the real game, where it is genuinely useful and
where Restart and the rescue banner already back it up.

```diff
-| Three consecutive non-progressing taps in a step | The coach escalates once: the source ring pulses **and** the destination dot brightens **and** the caption swaps to the step's explicit one-liner. It never escalates past this, and it never blocks. |
+| Three consecutive non-progressing taps in a step | The coach escalates once: the source ring pulses **and** the destination dot brightens **and** the caption re-shows the step's own caption. It never escalates past this, it never blocks, and it never mentions Undo — no tutorial board can box the player in, so there is nothing to be rescued from. |
```

```diff
-| `tutorial_undo_nudge` | `Boxed in? Undo takes your last jump back.` | T3 escalation only |
```

(If the ux-designer would rather keep a distinct escalation line per step, that is their call; the
only thing my ruling requires is that it does not claim the player is stuck.)

---

## 4. Considered and rejected

| Candidate | Why rejected |
|-----------|--------------|
| **T2 = 5-cell plus, 3 pegs** (the spec default) | Not dead-end-free (ends on 2 pegs) and lets the player satisfy the step with a second horizontal jump, so §14 criterion 2 was not structural. Proven: `worstTerminalPegs = 2`. |
| **T2 = 5-cell plus, 2 pegs** | Correct (dead-end-free, one forced vertical move) but it cannot earn the "never diagonally" caption: the plus has no diagonal cell to withhold a dot from. The full 3×3 does. |
| **T3 = 3×3 ring** (centre out of play), 5 pegs | Correct and pretty — a peg orbits the ring, 2 mirror-image lines, 4 moves, both axes. Rejected only because it gives the player **2** openings where the full 3×3 gives **3**, and because reusing T2's footprint means T3 introduces no new geometry to read. Kept on the shelf: it is the drop-in if anyone dislikes the chosen silhouette. |
| **T3 = the ring start on a full 3×3** | Behaviourally identical to the ring (the centre can never be jumped into from inside a 3×3, so an empty centre stays empty forever — verified: identical stats). Rejected because a hole that can never be used is a small lie to the player; if the centre is unusable it should not be drawn. |
| **T3 = 3×5 perimeter ring, 7 pegs** | **My own candidate, killed by the harness.** I reasoned by hand that the peg would orbit the ring in either direction; the enumeration found 6 maximal lines, 5 distinct terminal positions and `worstTerminalPegs = 5`. It can strand the player on five pegs on the second move. This is the entry that justifies proving by execution: the hand argument was confident and wrong. |
| **T3 = the 8-peg boards in a 3×4 box** | Dead-end-free, but all 4 have a **single forced opening** for all 7 moves — a 7-move rail. Longer, less readable (10-cell irregular footprint), and less agency than the 4-move board with 3 openings. |
| **Any Classic-catalogue footprint** | §2 above: none is dead-end-free, and the smallest is 33 holes in a 7×7 box → 40dp cells at `tutorial_board_size`, under the spec's own 48dp floor. |

**The fool's strongest surviving objection**, recorded because it is fair: *T1 and T2 each have
exactly one legal move, so the first two thirds of the tutorial is a rail — the player is not
solving, they are being led.* I accept the description and keep the design: a step whose job is to
teach one mechanic pays nothing for ambiguity and risks everything by it, and T3 restores agency
with three winning openings. The rail is two taps long.

---

## Checks

Every solvability claim below was established by **execution**, not by reading. The harness
enumerated every position reachable by any legal sequence of jumps from the start, using the
product's own `RulesEngine.legalMoves` / `applyMove`
(`domain/src/main/kotlin/com/app/pegsolitaire/domain/rules/RulesEngine.kt:42`, `:69`), and reported
the largest peg count over all terminal positions. `worstTerminalPegs = 1` **is** the proof that
every legal line ends on one peg, because a board is dead-end-free to one peg exactly when no
reachable position has zero legal moves and more than one peg.

- `./gradlew :domain:test --tests "com.app.pegsolitaire.domain.tutorial.TutorialBoardProofTest"`
  (run from `/Users/ermanergoz/Desktop/PegSolitarie-p15`) — **first run: fail** in 14s. One
  assertion failed: `value of: getWorstTerminalPegs() / expected: 1 / but was : 5` — the 3×5
  perimeter-ring candidate I had reasoned out by hand. Compilation was clean; the harness was
  right and my hand analysis was wrong.
- `./gradlew :domain:test --tests "com.app.pegsolitaire.domain.tutorial.TutorialBoardProofTest"`
  — **second run: BUILD SUCCESSFUL in 2s**, after the 3×5 ring was reclassified as rejected and
  the two remaining T3 candidates were added. Every assertion below passed.

Per-board results, quoted from the harness's own output
(`domain/build/tutorial-proof.txt`, written by the test, git-ignored build output):

```
=== T1 — 1x3 strip (ux-spec default, kept) ===
cells=3 startPegs=2 reachablePositions=2 terminalPositions=1 worstTerminalPegs=1
firstMoves=1 (h=1 v=0) maximalLines=1 lineLength=1..1
DEAD-END-FREE-TO-ONE-PEG = true

=== T2 — 5-cell plus, 3 pegs (ux-spec default) ===
cells=5 startPegs=3 reachablePositions=3 terminalPositions=2 worstTerminalPegs=2
firstMoves=2 (h=1 v=1) maximalLines=2 lineLength=1..1
DEAD-END-FREE-TO-ONE-PEG = false

=== T2 — full 3x3, 2 pegs (proposed) ===
cells=9 startPegs=2 reachablePositions=2 terminalPositions=1 worstTerminalPegs=1
firstMoves=1 (h=0 v=1) maximalLines=1 lineLength=1..1
DEAD-END-FREE-TO-ONE-PEG = true

=== T3 candidate C1 — full 3x3, 5-peg clump  [CHOSEN] ===
cells=9 startPegs=5 reachablePositions=10 terminalPositions=1 worstTerminalPegs=1
firstMoves=3 (h=1 v=2) maximalLines=4 lineLength=4..4 everyLineUsesBothAxes=true
DEAD-END-FREE-TO-ONE-PEG = true

=== T3 candidate — 3x3 ring, 5 pegs  [shelved alternative] ===
cells=8 startPegs=5 reachablePositions=8 terminalPositions=1 worstTerminalPegs=1
firstMoves=2 (h=1 v=1) maximalLines=2 lineLength=4..4 everyLineUsesBothAxes=true
DEAD-END-FREE-TO-ONE-PEG = true

=== T3 rejected — 3x5 perimeter ring, 7 pegs ===
cells=12 startPegs=7 reachablePositions=16 terminalPositions=5 worstTerminalPegs=5
firstMoves=2 (h=1 v=1) maximalLines=6 lineLength=2..6 everyLineUsesBothAxes=false
DEAD-END-FREE-TO-ONE-PEG = false
```

Exhaustive search, so the choice of T3 is evidence-backed rather than the first thing that worked.
Every footprint × every starting layout inside a bounding box was tested (3^9 = 19,683 pairs for
3×3; 3^12 = 531,441 for 3×4):

```
=== exhaustive search: 3x3 bounding box (>= 4 pegs) ===
pegs=4 boards=192 maxFirstMoves=4
pegs=5 boards=32  maxFirstMoves=3      <- 5 pegs is the ceiling in a 3x3 box;
                                          3 openings is the most any 5-peg board offers
=== exhaustive search: 3x4 bounding box (>= 5 pegs) ===
pegs=5 boards=1472 maxFirstMoves=4
pegs=6 boards=160  maxFirstMoves=3
pegs=7 boards=84   maxFirstMoves=2
pegs=8 boards=4    maxFirstMoves=1     <- the 8-peg boards are 7-move rails
```

The chosen T3 is one of the 32 five-peg boards and one of those attaining the maximum 3 openings.

Claims established by **reading**, with the exact reference:

- Orthogonal-only jumps, span 2, four directions —
  `domain/src/main/kotlin/com/app/pegsolitaire/domain/rules/RulesEngine.kt:26-35`; GDD
  `docs/superpowers/specs/2026-06-28-peg-solitaire-design.md:37` and `:71`.
- Objective is one peg — GDD `:39`; `BoardConstants.kt:23` `MINIMUM_PEGS_DEFAULT = 1`.
- Real levels are *not* dead-end-free (so the tutorial cannot borrow their geometry) — GDD `:82`
  ("if the player has reached an unsolvable state, the hint surfaces 'no winning move'").
- `Diamond41` cannot reach one peg — `Boards.kt:283-294` with
  `minimumPegs = BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR`, `BoardConstants.kt:42` `= 2`.
- Smallest catalogue board is 33 holes; the catalogue list is five specs — `Boards.kt:238-248`,
  `:323`.
- T1's footprint is already in the code, unchanged — `OnboardingOverlay.kt:155-171`.
- The tutorial screen has no Undo control (so §8's `tutorial_undo_nudge` has no referent) —
  `docs/ux-spec.md:319-325` (the four bands) vs `:690` (the string) and `:338` (the T3 lesson).
- `tutorial_board_size` is 280dp, touch targets must be ≥ 48dp — `docs/ux-spec.md:551`, `:786-792`.
- `showHints` is forced on inside the tutorial, so legal destinations are always visible —
  `docs/ux-spec.md:99-102` (D-01b).
- Boards are built as `Board(spec, pegs = 0L).withPeg(...)`; bit order is `BoardSpec.footprint`
  (row then col) — `Board.kt:13-32`, `BoardSpec.kt:17-38`.
- `./gradlew check` — **not run.** This is a design task; the only file I added to a source set was
  the throwaway proof harness, which I deleted after running it, so the build tree is identical to
  the branch point. The one file this task leaves behind is this report.

**The harness was deliberately not kept.** It was
`domain/src/test/kotlin/com/app/pegsolitaire/domain/tutorial/TutorialBoardProofTest.kt`; it asserted
against boards that do not exist in the product yet and carried a 550k-pair search that has no
place in `check`. It has been deleted.

**The permanent test that *does* earn its place, for the developer to write in this phase** — the
dead-end-freeness of the tutorial boards is the invariant that makes D-01c true, so it must be
guarded by a test, not by this report:

- **`TutorialBoardsTest.everyLegalLineOfEveryTutorialBoardEndsWithExactlyOnePeg`**, in
  `domain/src/test/kotlin/com/app/pegsolitaire/domain/tutorial/`, over `TutorialBoards.ALL`.
  For each board: enumerate every position reachable via `RulesEngine.legalMoves` /
  `RulesEngine.applyMove` (memoise on `Board.pegs`), and assert that every position with no legal
  move has `pegCount == 1`. Ten reachable positions is the largest of the three, so the test is
  effectively free.
- Two cheap companions in the same file, since they are the other two things the design promises:
  `theFirstTutorialStepHasExactlyOneLegalMoveAndItIsHorizontal` and
  `theSecondTutorialStepHasExactlyOneLegalMoveAndItIsVertical` — together these are the unit-level
  guard for ux-spec §14 criterion 2, which otherwise only has a device check.

## Open issues / risks

**No founder-taste call is hiding in here.** Board content is gameplay, which rules/studio.md
routes to this role, and the one aesthetic choice (which 5-peg silhouette T3 uses) is settled above
on measurable grounds — openings, line length, axis coverage — not on taste. I raise no `Q-9xx`.

1. **The tutorial board will reach `GameStatus.Won`, and must not be treated as a win**
   (tech-lead / developer). Every tutorial step ends at one peg, and `minimumPegs = 1`, so whatever
   computes status in `:app` will report `Won` at the end of **each** step. The tutorial must not
   play `SoundCue.Win`, must not navigate to Results, and must not write a `LevelResult` or any
   stars — ux-spec §4.1 already forbids the sound; the other two are mine to flag. A tutorial that
   banks stars would corrupt the Home "Stars" pill (§7 R-1) on first run.
2. **`TutorialBoards` must not join `Boards.CLASSIC_SPECS`** (developer). §2 above: that list is
   the Classic catalogue and the denominator of the Home "Boards" pill. This is the single easiest
   mistake to make with this ruling.
3. **The ux-spec needs the §3 diff routed** (producer → ux-designer). Three edits: §4.1's step
   table and its block-quote, §4.4's class-3 row, and the retirement of `tutorial_undo_nudge` in
   §8. I did not touch `docs/ux-spec.md`. If the string is retired, the §8 count in the copy
   contract drops by one.
4. **Wrong-tap class 1 will fire on T2 and T3, by design** (ux-designer / QA). On T2 the middle peg
   (1,1) has no legal jump; on T3 two of the five pegs have none at the start. Selecting one draws
   zero dots, which is exactly ux-spec §4.4's first wrong-tap class, and the nudge answers it. This
   is unavoidable in any peg-solitaire opening — the jumped-over peg can never itself jump — so it
   is a case QA should expect to see rather than a defect. Criterion 3 is testable on T2 by tapping
   the middle peg.
5. **"First failure impossible before puzzle 5" is only settled for the tutorial** (out of scope,
   named so it is not assumed). Challenge levels 1–4 are solvable **by construction** (GDD §5) but
   are *not* dead-end-free — GDD `:82` designs for the stuck state and the rescue banner exists for
   it. Nothing in this ruling changes that, and nothing in phase 15 should be read as having fixed
   it. If the studio wants the letter of rules/puzzle-design.md for levels 1–4, that is a generator
   change and a separate phase.

## Handoff

Next: **producer** — route §3 (the ux-spec diff) to the ux-designer, and hand §1's three footprints
plus the named permanent test in Checks to the tech-lead for the phase-15 plan. Nothing here blocks:
if the diff is never applied, the ruling in §1 is still the authoritative board content and the
developer should build from this report.
Then: **tech-lead / developer** — `object TutorialBoards` in `:domain` beside `Boards.kt` (not in
`CLASSIC_SPECS`), and `TutorialBoardsTest.everyLegalLineOfEveryTutorialBoardEndsWithExactlyOnePeg`.
Then: **qa-engineer** — risks 1 and 4 are the two device checks this ruling adds.
