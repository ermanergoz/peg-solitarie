# game-designer report — talkback-board-scope (the scope ruling F-12 asks for)
Date: 2026-10-21
Branch: feature/ux-spec-folds (committed by the producer)
Skills loaded: ergoz-soft:standards (role game-designer; the product's rules snapshot is older than plugin 1.0.0, so I read the plugin 1.0.0 copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, ip-policy, dispatch; and the product's `.claude/rules/lessons.md`), the-fool (pre-mortem and dialectic modes, chosen by this role: the skill's mode question goes to the user, and a staff dispatch has no user to ask)
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort xhigh (the agent file's effort). **Staffing deviation:** the game-designer is pinned to fable. This ruling ran on Opus under the founder's Q-019 (2026-10-17: "Opus for every Fable-pinned role until 3.0.0 is published"), in `.planning/questions/FOUNDER.md`.
Fan-out: none. Two read-only helpers (Explore, haiku), each told to run no git: one located the board files and grepped `.planning/` and `docs/` on develop; one read the old Play app's public source. See Checks.
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist in either checkout)

## Summary
- **The minimum (GD-TB-1):** every playable cell is its own TalkBack stop that speaks its row, column, content, selection and legal jumps. Double-tap is the existing tap. Each legal jump is also one TalkBack action. Every change and the result are spoken once, and focus stays on the board. That is M1–M8 below. W1–W7 can wait.
- **Baseline:** (c) breaks rules/compose.md and so the studio baseline. (b) meets the rule's words but not its purpose, and fails the spec's own pass line 26a. Both need the founder's explicit acceptance. Only (a) meets the baseline.
- **Recommendation: (a).** One branch, started now and run alongside the release queue, merged before `feature/candy-backdrop`. It is judged by TB-1..TB-12 below. I rule that the tutorial board is inside criterion 26.
- **Regression?** No. This codebase's v1.0 never had board semantics. Also, v1.0 never reached Play: players have the old 2.0 app, a different codebase (Q-015, Q-016), whose board is also a canvas that TalkBack cannot reach. No player loses anything, but the baseline is judged at ship, not against the previous version.
- The founder question draft is at the end. Under my procedure the studio proceeds with (a) while the question is open.

## 1. Ruling: what accessible play of this board minimally needs (GD-TB-1)

**The standard.** rules/puzzle-design.md asks for fairness ("all needed information visible; no hidden state; no forced guess") and rejects difficulty that comes from memorising. For a TalkBack player, "visible" means spoken. So the screen reader must give a blind player the same information a sighted player takes in at a glance, without making them hold a 33-hole board in memory. Peg solitaire's difficulty is planning a sequence of jumps. It is not finding the legal ones, and it is not remembering where the pegs are. "Playable" means that a level can be finished from the first move to the result with TalkBack gestures alone, at a cost in gestures that does not grow with the board's height.

**What a sighted player has today that a TalkBack player has none of** (develop, `PegSolitarie-dev`):
- The board is one `Canvas` with a test tag and a tap detector. It has no label, role, text or action (`BoardCanvas.kt:196-212`). Its parent's three semantics keys are custom and test-only, and TalkBack does not read them (`GameScreen.kt:193-207`). The tutorial board is the same composable with nothing added (`TutorialScreen.kt:245-263`).
- So TalkBack's swipe goes from the timer to Undo and from Skip to the caption. No cell can be heard and no peg can be selected.
- The hint is drawn only: a source glow, a target ring and a line. It "never performs the jump" (`BoardCanvas.kt:402-409`, `:544-580`), and its only other channel is a sound (`GameScreen.kt:170`).
- The Show-moves dots are drawn only (`BoardCanvas.kt:448`).
- On Results the stars are decorative. Only the headline and the metrics speak (`ResultsScreen.kt:56-60`, `:239-243`, `:279-282`).

**The minimum, M1–M8.** Each item names the sighted channel it replaces. Wording, the traversal position and where focus lands are the ux-designer's (screens/UI/accessibility). What must be said and what must be operable is this ruling.

| # | The TalkBack player gets | It replaces |
|---|---|---|
| M1 | **The board announces itself.** On entering the board at its §11 position (after the timer on gameplay, after Skip in the tutorial), the first thing spoken says it is the board and gives the pegs left. Activating that announcement never lands a tap on a cell. | Seeing the board and its peg count |
| M2 | **One stop per playable cell.** Every in-play cell is exactly one focus stop. Blocked cells are never stops. Order: row by row from the top, left to right in a row. Each cell is also reachable by touching its place on the screen (explore by touch). | Seeing the layout |
| M3 | **What a cell says:** its row and column, its content (peg or empty hole), and its state. A selected peg says "selected". Every peg says how many legal jumps it has, "no jumps" included. While a peg is selected, an empty hole that is a legal destination of that peg says so, and no other hole does. | Reading the position, the selection ring, and the legal jumps at a glance |
| M4 | **Activation is the tap.** A double-tap on a cell does exactly what a finger tap on that cell does, through the same path: select, reselect, deselect, jump or rejected tap. A rejected tap says that nothing happened and why. | The finger tap; the wrong-tap feedback |
| M5 | **One action per legal jump.** A peg offers one TalkBack action per legal jump, naming the direction and the destination cell. Taking one gives the same result as selecting the peg and tapping the destination: the same move record, sound, haptic, undo entry and tutorial progress. A peg with no legal jump offers none. | Moving the finger two cells. Without M5 a vertical jump on a 7-wide board costs a row of swipes to reach the destination |
| M6 | **Every change is spoken once.** After a jump: from where, to where, which peg was removed, and the pegs left. After Undo: what was undone and the pegs left. After a hint: the hinted peg, its direction and its destination. After Restart: the pegs left. When the rescue banner rises: its message. Each is spoken politely and once, and the cells always carry the same state, so an announcement is never the only record. | The jump slide, the removal fade, the board stepping back, the hint glow and arrow, the banner |
| M7 | **Focus stays on the board.** After a board action (select, a jump by tap or by action, a rejected tap) TalkBack focus is still on a board cell, not on another control and not at the top of the screen. The ux-designer picks which cell. Undo, Hint and Restart keep focus on their own dock control. | The eye staying on the board |
| M8 | **The result is spoken.** When no jump remains, the pegs left are spoken. Results speaks the star count ("2 of 3 stars") as well as the headline and metrics. When the game ends on the rescue banner instead, M6 covers it. | Seeing the stars fill |

The tutorial gets M1–M8 on its board. Two tutorial-only items come with them: a step's new caption is spoken when the step changes (the caption is the instruction), and a coach escalation, which today draws the source ring and the brightened dot (`TutorialScreen.kt:222-229`), speaks the hinted move as M6 does.

**Designer decisions that come with GD-TB-1** (recorded here: the product has no `docs/gdd.md`, because it predates the studio and its concept of record is `.planning/PROJECT.md`, per `docs/concept-brief.md`):
- **GD-TB-2: the legal-jump count and the "jump target" words are spoken whether Show-moves is on or off.** They stand in for sight, not for the assist. A sighted player with Show-moves off still sees which holes lie two cells from the selected peg, past a peg. "Legal" is not "winning": the solver-backed Hint stays the only thing that ever names a winning move, so the puzzle's difficulty is unchanged.
- **GD-TB-3: no time allowance for TalkBack play.** Stars come only from pegs left (`docs/superpowers/specs/2026-06-28-peg-solitaire-design.md` §7), so a slower TalkBack player loses no star. The time-decay score only ranks players. Adjusting it would split the leaderboards, and nothing in fairness asks for that.
- **GD-TB-4: the tutorial board is inside criterion 26.** It is §4.1's "one interactive element" (`docs/ux-spec.md:1195`) on a screen 3.0.0 made new, and phase 15's own success criterion 10 says every new interactive element is "TalkBack-labelled" (`.planning/ROADMAP.md:416` on develop). The gameplay board predates phase 15, so it is not "new" in 26's words, but the studio baseline applies to it all the same. This ruling covers both boards equally.
- **GD-TB-5: an M5 action is a move, not a new rule.** It must leave no trace that the two taps would not leave. The tutorial counts it as progress, the hint logic treats it as the player's move, and undo reverts it as one step.

**What can wait (3.1 or later, not owed by 3.0.0):**
- W1: row and column navigation gestures beyond TalkBack's own (for example, moving focus straight up a row).
- W2: a "read this row" or "describe the board" action that speaks a whole line at once.
- W3: verbosity choices (short labels, letter-and-number coordinates).
- W4: per-content haptic or audio ticks while exploring by touch.
- W5: Switch Access, keyboard and D-pad play as their own judged paths. They use the same accessibility actions, so M4 and M5 should serve them, but 3.0.0 does not judge them.
- W6: a TalkBack-specific tutorial wording (for example, "double-tap" in place of "tap"). TalkBack users already read "tap" as "activate".
- W7: spoken previews of each Classic board's layout in the catalogue, beyond today's peg count.

## 2. The three options: cost, baseline and regression

### Is it a regression?
- **Within this codebase: no.** On every record I can read, the gap is as old as the board:
  - `.planning/milestones/v1.0-REQUIREMENTS.md` has no accessibility id, and `.planning/milestones/` has no TalkBack mention at all.
  - The board's tap path is v1.0's design: `BoardCanvas.kt`'s doc comment cites phase 4's threats T-04-01/T-04-02.
  - The ux-designer's check found the same (`ux-designer-check-loop2.md`, F-12: "the gap predates phase 15").
  - My tools cannot read the `v1.0` tag itself (no Bash, and helpers may not run git). A one-line confirmation is in the Handoff.
- **But v1.0 was never on Play, so "v1.0 is published" is not right as worded.**
  - The live listing is the old app: `com.erman.pegsolitarie`, versionCode 2, "which had no Challenge ladder, leaderboards, cloud save or sound" (Q-016). This repo's v1.0 had the Challenge ladder.
  - The rewrite's first upload is 3.0.0's: "The first upload of 3.0.0 enrols the app in Play App Signing … keeps every copy of the old app installed today upgradable to 3.0.0" (Q-015).
  - `docs/concept-brief.md`'s "Evidence: v1.0 published" uses gsd's milestone sense. It is the likely source of the premise.
  - So the player-facing question is whether 3.0.0 takes away something the old 2.0 app gave TalkBack users.
- **The old 2.0 app: no regression either. It had the same gap, so no player loses anything.**
  - Its public source (`github.com/ermanergoz/peg-solitarie`, branch `master`) draws the whole board in one custom `View`: `GridView.onDraw` paints one circle per cell, and a raw `onTouchEvent` turns two coordinate taps into a move (`GridView.kt:43`, `:111-150`; `GameBoard.kt:85`).
  - It has no content description, no accessibility delegate, no `ExploreByTouchHelper` and no focusable cell.
  - Only the board picker's images on Home and the text buttons are labelled.
  - This is inferred from the source by a read-only helper, through a summarising fetch for every file but `GameBoard.kt`, and not run on a device.
- **So 3.0.0 does not make anyone worse off.** That settles who is to blame. It does not settle whether 3.0.0 may ship: the baseline is judged at ship, not against the previous version.

### Cost and baseline, option by option
The size below is **my estimate from reading the code, not a tech-lead size** (my agent file lets me ask for one only through the producer). It rests on what already exists to build on:
- one board geometry maps a cell to its screen rectangle for both drawing and tapping (`BoardGeometry.positionAt`, `.center`);
- the rules engine enumerates legal moves;
- the ViewModel already exposes the selected peg's legal targets, the hint's source and target, and the peg count;
- every reaction already has a state tick (`GameScreen.kt:164-171`).

| | (a) Full minimum (M1–M8) in 3.0.0 | (b) Board labelled as a whole; play in 3.1 | (c) Ship as is; defer it all |
|---|---|---|---|
| Work | The ux-designer writes the board's spoken contract in §11, and its criteria, from TB-1..TB-12. One developer branch: a JVM-tested model that turns `GameUiState` into what each cell says and the jumps it offers; a semantics layer over the board's existing geometry, on both boards; a polite announcement for M6; the star count on Results; strings and plurals; Compose semantics tests. Then the usual chain: code-auditor, QA's TalkBack pass on a device, tech-lead sign-off. **My estimate: one branch of a few hundred lines with its tests, and one or two review loops.** | One board-level label with the peg count. It must be checked on a device that a TalkBack double-tap on it never lands as a tap on a cell; the board's tap detector reads raw pointer positions. A spec line, a criterion and a QA check. Hours. | None beyond the record, and §11's recorded gap already exists. |
| What a TalkBack player gets | The whole game: Classic, Challenge and the tutorial, with every move, hint, undo and result spoken. | The board's name and peg count, and still no move. They are in effect told the game is closed to them. | Silence where the board is, and a tutorial they can only Skip. |
| A side effect worth naming | None. | In both (b) and (c), Hint stays reachable and labelled. A TalkBack player can spend the per-level free hints (ADS-02) and then be offered, and accept, a rewarded ad (ADS-01; `AdOfferDialog` is labelled) for a hint they can neither hear nor play. | Same as (b). |
| Release effect | It holds the release only if it is the last item standing. Ahead of `release/3.0.0` the queue already holds `phase-14-check-gates`, the 17-item audio integration, `bugfix/home-chip-arrival`, licences, colour-vision, and candy-backdrop with its own 15e sitting, plus the store copy and the screenshots that wait on the backdrop (`.planning/reports/STATUS.md`, In flight). Run in parallel now, its delay is likely nil. Its merge conflicts with `feature/colour-vision` (the board palette in `BoardCanvas.kt`) and with candy-backdrop (the screens) are ordinary serial merges. | None. | None. |
| Studio baseline | **Meets it.** | **Deviation; founder's explicit acceptance needed.** It satisfies rules/compose.md's words ("contentDescription or semantics role on every interactive element") but not their purpose: the element stays inoperable by the very users the rule is for. It also fails the spec's own pass line for 26: "a control fails … when the label sits on something that cannot be activated" (`docs/ux-spec.md` 26a). Under GD-TB-4 that line covers the tutorial board. | **Breaks it.** An interactive element with no description and no role is exactly what rules/compose.md forbids, and rules/studio.md's baseline says each product "meets the accessibility rules of its stack … before it ships". **Founder's explicit acceptance needed.** |

The baseline cannot be met by a smaller audience or an older gap. It is judged when the product ships, and neither "few users" nor "not a regression" is in its wording. The studio already treats rules/compose.md's accessibility list as binding for 3.0.0 elsewhere:
- colour-vision (the same rule's "colour-blind palette option in games") is specified and queued before the release cut;
- the timer's TalkBack form is "owed before release/3.0.0" (STATUS.md, In flight).

Deferring the board while fixing the timer would apply that list to the chrome and skip the game itself.

## 3. Challenging the options (the-fool: pre-mortem and dialectic)

**The strongest case for (c), steelmanned.** Few blind players pick up a visual board game. The gap is not a regression in this codebase. 3.0.0 is the long-awaited replacement for a stale Play app. Every extra branch is a chance for the release to slip, and 3.1 can carry the fix. I concede the following:
- The audience is small. That is why W1–W7 wait.
- It is not a regression in this codebase.
- The fix belongs in this release only if it does not hold the release hostage.

**The challenges that decide it:**
1. **"It will be fixed in 3.1" is the failure this repo already shows.** Imagine 3.1 has shipped and the board is still silent. The likely story is the one STATUS.md already tells:
   - `docs/audio-spec.md` "has never existed on any branch" although a rule makes it the cue list of record;
   - criterion 26 has stood at "PARTIAL — the TalkBack half is NOT settled" since phase 15, which merged with it so (`.planning/reports/phase-15/qa-engineer.md`);
   - the ≤ 2 s launch bar "has never been measured on a physical phone".
   Each was deferred with a reason, and each is still open. A deferral with no date and no owner joins that list. That is the strongest objection to (b) and (c).
2. **"Blind players don't play spatial puzzles."** A peg-solitaire grid is one of the friendliest shapes a game can have for a screen reader. It is discrete, turn-based, fully observable, and its stars are untimed: the same shape that blind players already play as chess and sudoku. Nothing about the game needs sight. Only the build does.
3. **(a) could turn open-ended**, with focus bugs on a canvas, chatty announcements, and an emulator TTS that crashed once in phase 15 (STATUS.md, phase-15 QA). Three mitigations:
   - the scope is fixed now (M1–M8, TB-1..TB-12), and the W list is excluded by name;
   - most of the proof is JVM and Compose semantics tests, with the device pass only confirming;
   - the branch runs alongside the queue rather than after it.
   If it is still the only item left when everything else for `release/3.0.0` has merged, the producer puts "wait, or ship (b)" into the Q-PUBLISH-3.0.0 summary. Gate 2 is already a founder touchpoint, so this adds no new round trip.
4. **"(b) is honest and cheap."** (b) is strictly better than (c) for the player who opens the app, and it is the right fallback if (a) cannot land. As a plan it is not enough: it tells a blind player "you can't play" and still lets them spend hints and be offered ads for them.

**Synthesis** (temporal, with a stated fallback): build the minimum now, in parallel, and merge it before candy-backdrop. Fall back to (b) only at Gate 2, and only if the board is the last item standing. Do not ship (c). Confidence: **high** on the ruling and the baseline reading; **medium** on the cost, until the tech-lead sizes it.

## 4. Recommendation: (a), with these acceptance criteria

Each criterion names the test that proves it. The ux-designer turns them into §14 criteria with the exact strings. QA judges them. "TalkBack gestures only" means swipes, double-taps and TalkBack's actions menu, with TalkBack's "Display speech output" log as the record.

- **TB-1 (M1): the board announces itself.** Given gameplay (Classic and Challenge) and the tutorial with TalkBack on, when the player swipes forward from the timer (gameplay) or from Skip (tutorial), then the next utterance identifies the board and contains the pegs-left count, and activating whatever spoke it changes no cell. *Compose semantics test on both screens; device pass.*
- **TB-2 (M2): one stop per playable cell.** Given any board, when TalkBack's swipe crosses the board, then every in-play cell is visited exactly once, row by row from the top and left to right within a row, no blocked cell is visited, and each stop speaks its row, its column and "peg" or "empty hole". *JVM model test over all five Classic boards and Challenge levels 1–1,000: the stop count equals `inPlay.size`, in order. Compose test on English-33: the node count and their order.*
- **TB-3 (M3): selection and legal jumps are spoken.** Given a peg with k legal jumps, its stop speaks k ("no jumps" when k = 0). Given a selected peg, its stop speaks "selected", and exactly the empty holes in the ViewModel's `legalTargets` speak the target wording. All of this holds with Show-moves on and with it off. *Table-driven JVM test, plus a property test over 1,000 seeded reachable states: the spoken k equals the rules engine's legal-move count from that peg, and the spoken targets equal `legalTargets`.*
- **TB-4 (M4): a double-tap is a tap.** Given any state and any cell, when TalkBack activates the cell, then the next `GameUiState`, the sound and haptic events, the undo history and the tutorial progress equal those of a finger tap on that cell from the same state. This covers select, reselect, deselect, jump and rejected tap, and a rejected tap is spoken. *Compose test: a scripted sequence run once through the click action and once through pointer taps, compared state by state; device pass.*
- **TB-5 (M5): one action per legal jump.** Given a peg with legal jumps, its stop offers exactly one action per legal jump, each naming the direction and the destination cell. Taking one leaves the state that selecting the peg and tapping that destination leaves. A peg with no legal jump, and every hole, offers no jump action. *Compose test through the custom actions; JVM property test over seeded states.*
- **TB-6 (M6): every change is spoken once.** Given TalkBack on, when a jump completes (by tap or by action), the player hears once: the source, the destination, the removed peg's cell and the pegs left. When Undo completes, they hear what was undone and the pegs left. When a hint is revealed: the hinted peg, its direction and its destination. When Restart completes: the pegs left. When the rescue banner rises: its message. No event is spoken twice, and none is spoken late over the next event. *Compose test on the announcement node's text after each event; device pass from the speech log.*
- **TB-7 (M7): focus stays on the board.** Given TalkBack focus on a board cell, when the player selects, jumps by tap, jumps by action or makes a rejected tap, then focus is on the board cell the spec names, never on another control or at the top of the screen. Undo, Hint and Restart keep focus on themselves. *Device pass.*
- **TB-8 (M8): the result is spoken.** Given a finished game routed to Results, when Results opens, then the star count ("n of 3 stars") and the pegs left are spoken. Given a game that ends on the rescue banner, TB-6's banner line applies. *Compose test on `ResultsScreen`; device pass.*
- **TB-9: the tutorial can be finished by ear.** Given a fresh install with TalkBack on, a tester using TalkBack gestures only completes T1, T2 and T3 and reaches the completion CTA. Each step's new caption is spoken when the step changes, and a coach escalation speaks the hinted move. *Device pass; Compose test for the caption and the coach announcements.*
- **TB-10: a whole game can be finished by ear.** Given English-33 and Challenge level 1, a tester using TalkBack gestures only plays from the first move to Results without looking at the screen. *Device pass; the speech log is the evidence.*
- **TB-11: sighted play is untouched.** Given TalkBack off, then taps, drawing, animations, sounds and haptics behave exactly as before. The layer that carries the cells' semantics takes no pointer input. The existing suite passes with zero diff to existing tests, except 26b's spoken order, which the ux-designer amends to place the board's stop. *Existing suite; a pointer-tap test through the new layer.*
- **TB-12: nothing spoken leaks a winning move (GD-TB-2).** Given any state, every spoken cell text, action and announcement comes only from the board, the selection, `legalTargets`, the move just made and the revealed hint. Nothing comes from the solver except a hint the player asked for, or a tutorial coach escalation. *JVM test on the model's inputs; code-auditor reads the dependency.*

## Founder question draft
(id-less; the producer gives it a Q-NNN)

Context: With TalkBack (Android's screen reader) on, the board can't be heard or played, in the game or in the tutorial. A blind player can open 3.0.0 but never make a move. The studio's rule is that every app meets its platform's accessibility rules before it ships, and this misses that rule. Making the board playable is one small branch that can run alongside the rest of the 3.0.0 work.
Options:
1) Make the board playable with TalkBack in 3.0.0. The release waits for it only if it is the last thing left.
2) Ship 3.0.0 with the board named and its peg count spoken, but not playable; make it playable in 3.1.
3) Ship 3.0.0 as it is; fix it later.
Options 2 and 3 ship below the studio's accessibility rule and need your explicit OK.
Recommendation: game-designer picks 1. It is the only option that meets the rule, it likely costs no release time because it runs in parallel, and in 2 and 3 a blind player can still spend hints, and watch an ad for one, without ever being able to use it. If at Gate 2 the board is the only thing left, you will be offered "wait, or ship option 2" then.
Blocks (suggested): nothing now, since the studio proceeds with 1 by default; `release/3.0.0` reads the answer before its cut.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-ux/.planning/reports/ux-spec-folds/game-designer-talkback-board.md`: this report. It holds the scope ruling (GD-TB-1..5), the option table, TB-1..TB-12 and the founder question draft. Not committed; the producer commits it. I edited no other file.

## Checks
- `Read` of the plugin 1.0.0 rules in full (studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, ip-policy, dispatch), `.claude/rules/lessons.md`, and `agents/game-designer.md`: pass.
- `Read` of `PegSolitarie-ux`, all passing:
  - in full: `.planning/reports/ux-spec-folds/ux-designer-check-loop2.md` (F-12), `.planning/REQUIREMENTS.md`, `.planning/PROJECT.md`, `docs/concept-brief.md`, `docs/superpowers/specs/2026-06-28-peg-solitaire-design.md`, `VERSION` (3.0.0);
  - in part: `docs/ux-spec.md` `:1170-1229` (§4.1), `:3920-4079` (§11, including the recorded-gap pointer `:4048-4060`) and `:4810-4915` (21c–29, 26, 26a, 26b); `.planning/questions/FOUNDER.md` `:1-200` (Q-003 to Q-022); `.planning/reports/STATUS.md` `:1-120`.
  - `docs/gdd.md` does not exist.
- `Read` of code on develop (`PegSolitarie-dev`), all passing: `BoardCanvas.kt` in full; `GameScreen.kt:40-239`; `TutorialScreen.kt:150-279`; `ResultsScreen.kt:40-299`; `.planning/ROADMAP.md:395-434`. The facts are cited where they are used.
- Helper 1 (Explore, haiku; Glob, Grep and Read; no git), pass:
  - It found the three board files' paths.
  - The board's only input path is `BoardCanvas.kt:206-207` (`pointerInput` → `detectTapGestures`). Every other `pointerInput` is in `AdQuiescenceScrim.kt` and `Clay.kt`.
  - The board, game, results and tutorial packages have labelled controls, and the board has none.
  - In `.planning/milestones/` there is no TalkBack mention, and no accessibility id appears in `.planning/REQUIREMENTS.md` or `milestones/v1.0-REQUIREMENTS.md`.
  - `ROADMAP.md:416` is phase 15's success criterion 10.
  - `.planning` and `docs` have no colour-blind hit.
- Helper 2 (Explore, haiku; WebFetch, Glob and Read; no git), pass, with a caveat:
  - The old app's board is one custom `GridView`, drawn in `onDraw` and played through `onTouchEvent`, with no accessibility API.
  - No local checkout of the old app exists.
  - Caveat: every file but `GameBoard.kt` came back through a summarising fetch.
- `Read` of `.planning/recall/RECALL.md` in both checkouts: absent.
- the-fool: a pre-mortem and a dialectic, in §3.
- The Check command was not run. This is a documents-only ruling, and the role has no Bash.
- I did not read `.planning/log/`.

## Open issues / risks
- **The cost is my estimate, not a tech-lead size.** The recommendation does not hinge on the exact size, because the branch runs alongside the queue either way. The fallback in §3 covers the case where it does not land in time.
- **The `v1.0` tag was not read directly.** My role has no Bash, and its helpers may not run git. The "no regression in this codebase" finding rests on the planning record and on today's code. The confirmation is in the Handoff.
- **The old-app finding is inferred from source,** read through a summarising fetch, and was not run on a device. It changes nothing in the ruling: the baseline is judged at ship.
- **Process finding (record only, not mine to act on).** Phase 15 merged to develop on 2026-10-21 in this state:
  - its own success criterion 10 ("Every new interactive element … TalkBack-labelled", `ROADMAP.md:416`) was unmet for the tutorial board;
  - criterion 26 stood at "PARTIAL — the TalkBack half is NOT settled".
  This ruling settles the scope from here on. Whether the merge record needs a note is the producer's call.
- **The Hint and rewarded-ad exposure is live in today's build.** A TalkBack player can spend the free hints and be offered an ad for a hint they cannot use. It lasts until (a) merges, and under (b) or (c) it ships.
- **Device-pass risk.** Phase-15 QA's TalkBack check was cut short when the emulator's TTS engine crashed (STATUS.md). The 15-founder-feedback passes later ran TalkBack on device. QA should confirm a working TTS on its AVD before TB-7, TB-9 and TB-10.
- **This ruling ran on Opus (Q-019), and no fresh agent checked it.** The producer may want one before the ux-designer writes from it.
- **Where the decisions live.** No `docs/gdd.md` exists, so GD-TB-1..5 are recorded only here until the product has a designer-decision log.

## Handoff
Next: producer —
- **The founder:** queue the question above as a Q-NNN, with the suggested `Blocks:` line. Under my procedure the studio proceeds with (a) meanwhile. If the founder picks 2 or 3, the branch is parked and §11 records the founder's acceptance of the deviation.
- **ux-designer** (on `feature/ux-spec-folds`, closing F-12):
  - Write the board's spoken contract into §11: M1–M8's wording, the board stop's form, and M7's focus cell.
  - Turn TB-1..TB-12 into §14 criteria with exact strings.
  - Amend 26b to place the board's stop after the timer and after Skip.
  - Replace the "pending" pointers at §11 (`:4058-4060`), 26 (`:4851-4856`) and 26b (`:4893-4895`) with GD-TB-4 and this report's path.
- **tech-lead:**
  - Size the branch. My estimate: one branch, a few hundred lines with tests, one or two review loops.
  - Name the branch and its merge slot: before `feature/candy-backdrop`, and ordered against `feature/colour-vision`, which also edits `BoardCanvas.kt`'s palette.
- **android-game-dev:** build it test-first in its own worktree, once the ux-designer's criteria exist. Then the code-auditor; then the qa-engineer, whose TalkBack device pass covers TB-7, TB-9 and TB-10 with the speech log; then the tech-lead's sign-off.
- **Any role that may run git:** confirm that the `v1.0` tag's board had no accessibility semantics. Run `git grep -n -i -e semantics -e contentDescription v1.0 -- 'app/src/main/java/com/app/pegsolitaire/ui/'` and check that no hit sits on the board's `Canvas` or on its parent.
- **At Gate 2:** if the board branch is the last item standing, put "wait, or ship (b)" into the Q-PUBLISH-3.0.0 summary rather than raising a new question.
