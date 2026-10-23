# game-designer report — hint-timeout-ruling
Date: 2026-10-21
Branch: feature/phase-14-release-docs (committed by the producer)
Skills loaded: ergoz-soft:standards (role game-designer). The product snapshot is 0.5.1 and the plugin is 1.0.0, so I read the plugin 1.0.0 rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, ip-policy and dispatch. I also read the product's `.claude/rules/lessons.md`. Second skill: the-fool.
Ran in parallel with: none named. Per the dispatch, other roles were editing docs in this worktree.
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** the game-designer is pinned to fable at xhigh (`agents/game-designer.md`). This ruling ran on Opus under the founder's Q-019 (answered 2026-10-17): Opus for every Fable-pinned role until 3.0.0 is published.
Fan-out: none. Two haiku Explore helpers ran one after the other. They located files and returned line numbers, because this role has no Glob, Grep or Bash.
Loops: 0
Lessons applied: none. The snapshot holds 0 active lessons, and the product has no `.planning/recall/RECALL.md`.

## Summary
- **Yes, it is a defect.** The game says "No winning move" about positions it never proved dead. That breaks the spec of record: ASST-03, design spec §6, the precondition Phase 4 attached to D-04, and Phase 8's goal. It also breaks the puzzle-design fairness rule. **Severity MAJOR. It must be fixed before `release/3.0.0` is cut.**
- **Three outcomes, three behaviours.**
  - A proven dead end keeps today's banner.
  - A budget miss is *Undecided*. It gets its own neutral notice, is free, and never raises the rescue banner.
  - A cancelled search is silent.
  - The 1.5 s deadline stays as it is (D-07).
- **Pick: R1, a distinct Undecided outcome.** The domain already tells a proof from a cut-off (`Solver.kt:160`) and then throws the difference away, so R1 is a small change. Deeper books and line continuity are a 3.1 follow-up. A longer or progressive budget is rejected.
- **Copy.** Today the listing may say "a shown hint is a real winning move". It may not say that "no winning move" is proven. After R1 merges, it may say that too.

## Amendment 1 — 2026-10-21: following Hint from the start never leaves the book
**Source.** The tech-lead raised this as §16 #22, in `tech-lead-release-docs-blockers.md` (Open issues) and in `docs/tech-design.md` §7.2, "The book counts, reconciled", committed on this branch.

**What I re-checked against `develop`:**
- `OpeningBook.lookup` has no depth check (`OpeningBook.kt:184-192`).
- The committed Wiegleb-45 line is `domain/src/test/resources/golden/solutions/wiegleb45.txt`: 43 moves, and its first four are (2,4)→(3,4)→(4,4), (0,4)→(1,4)→(2,4), (3,6)→(3,5)→(3,4) and (3,8)→(3,7)→(3,6).

**What is true:**
- **Each book holds more than its depth-≤K frontier.** It also holds every state of its board's committed golden line past depth K, and at every golden-line state the book's answer is that line's own move.
- **So a player who follows Hint from the untouched start never reaches the solver,** on any of the five Classic boards:
  - they stay on the golden line, and every hint along it is an instant book answer;
  - Challenge behaves the same way along its recorded line.
- **The solver, and with it the false banner, is reached only off that line:** past each book's depth, or in a gap.
- **The position counts, corrected:**
  - Wiegleb-45 lacks 4 of its **101** positions at depth ≤ 3, and Diamond-41 lacks 440 of its **785** at depth ≤ 4.
  - The book headers' "4 of 140" and "440 of 818" also count the line's states, so they are wrong as position counts.

**What changes:**
- **The ruling stands:** a defect, MAJOR, and R1 before `release/3.0.0`.
  - The false banner needs a player off the golden line. Most players reach the moment they ask for help that way: they play their own moves and tap Hint when unsure.
  - The players it never reaches are the ones who lean on Hint from the first move.
- **F-1's concrete example does not occur on any board.** That covers both the auditor's "fourth hint" and my first correction's "fifth hint": following Hint never leads off the book. I have marked §1 in place.
- **AC-18 as written cannot pass, because the fifth followed hint is a book answer.** It is replaced below.
  - AC-2 stands, since it asserts the book miss. It now names its positions.
  - AC-2b's boundary sentence is corrected, and AC-2c is new.
- **R2b's value moves.** Players who follow Hint from the start are already covered. R2b helps the player who leaves the line, gets a Found, and then follows Hint from there. The R2b row in §3 and GD-HT-6 are corrected in place.
- **§4.** The claims built on "the first K + 1 hints" are corrected in place, and one new true claim is added.

**Three device walks that genuinely leave the book.**

*How to read them:*
- Coordinates are (row, col), 0-based, with row 0 at the top and column 0 at the left. These are the `Position` values in `Boards.kt` and in the golden file.
- The centre is (4,4).
- Each move reads from → over → to.

*Why they work:* each walk plays the first four golden moves under one symmetry of the board. Its position after the fourth jump therefore:
- **is a guaranteed book miss.** It has 40 pegs, so it is outside the depth-3 frontier. It is also not the golden line's own depth-4 state, whose empty holes are (0,4), (1,4), (3,5), (3,7) and (3,8).
- **is winnable.** The same symmetry applied to golden moves 5 to 43 takes it to one peg.
- **is the hardest kind of position for the solver.** Up to symmetry it is the golden depth-4 state, one of the 76 canonical Wiegleb-45 positions at depth ≤ 4, none of which resolved in 1.5 s.

| Walk | Symmetry | The four jumps | Empty holes afterwards |
|---|---|---|---|
| **A** | left–right mirror, col → 8 − col | (2,4)→(3,4)→(4,4); (0,4)→(1,4)→(2,4); (3,2)→(3,3)→(3,4); (3,0)→(3,1)→(3,2) | (0,4), (1,4), (3,3), (3,1), (3,0) |
| **B** | 180° rotation | (6,4)→(5,4)→(4,4); (8,4)→(7,4)→(6,4); (5,2)→(5,3)→(5,4); (5,0)→(5,1)→(5,2) | (8,4), (7,4), (5,3), (5,1), (5,0) |
| **C** | transpose, (r,c) → (c,r) | (4,2)→(4,3)→(4,4); (4,0)→(4,1)→(4,2); (6,3)→(5,3)→(4,3); (8,3)→(7,3)→(6,3) | (4,0), (4,1), (5,3), (7,3), (8,3) |

**Walk A in words, for the device:**
1. The peg two above the centre jumps down into the centre.
2. The top peg of that arm jumps down two, into the hole the first jump's peg started from.
3. In the row just above the centre, the peg two left of the middle column jumps right, into the middle hole of that row. Hint would show the same target hole approached from the right; the walk takes it from the left.
4. The leftmost peg of that row jumps right two, into the hole the third jump's peg started from.

If the screen's orientation ever differed from the coordinates, at most one of A, B and C could turn into the golden line itself. That is one reason AC-18 runs all three.

## 1. Is it a defect, how severe, and must it block `release/3.0.0`?

**Which documents count as the spec.** This product has no `docs/gdd.md`. Q-CONCEPT adopted PROJECT.md and REQUIREMENTS.md as the concept of record, together with the design spec and the phase CONTEXT decisions. These are the parts of that record that bear on Hint:
- **Design spec §6** (`docs/superpowers/specs/2026-06-28-peg-solitaire-design.md:82`): "if the player has reached an unsolvable state, the hint surfaces 'no winning move — undo or restart'".
- **ASST-03** (`.planning/milestones/v1.0-REQUIREMENTS.md:50`): "a clear 'no winning move' state when the position is unsolvable".
- **Phase 4 D-04** (`04-CONTEXT.md:32`) folded a budget miss into "no winning move", but on a stated condition. The budget "MUST be tuned high enough that genuinely-solvable full-size classic positions (English-33, Wiegleb-45, etc. from arbitrary mid-game states) essentially never hit the budget — otherwise the game would falsely tell a player a solvable board is dead."
  - The measurements falsify that condition. 0 of 76 Wiegleb-45 positions at depth ≤ 4 resolve within 1.5 s (`GetHintUseCase.kt:54-58`; `Solver.kt:60-64`: "roughly four orders of magnitude").
  - **D-04 is therefore void.**
- **Phase 8**:
  - The goal: "the hint system tells the truth on every shipped board" (`08-CONTEXT.md:15`).
  - The banner "means the same thing everywhere — the perfect finish is no longer reachable" (`:112`).
  - DEBT-01 (`.planning/REQUIREMENTS.md:106`): "instead of folding to 'no winning move'".
- **`rules/puzzle-design.md`**:
  - The Fairness rule says: "all needed information visible; no hidden state; no forced guess". The player cannot see whether the search finished, and that hidden fact flips the meaning of the banner. The player is forced to guess whether to believe it.
  - Invariant 4 (no unrecoverable states) holds: Undo and Restart are always reachable.
  - Invariant 5 holds for every hint that reveals a move. Each revealed move is book-proven, the generator's recorded line, or the first move of a complete line the solver found, and each passes the legality guard (`GetHintUseCase.kt:92-97`). The defect sits in the no-move answer, so it falls under Fairness and ASST-03.

**What the code does on `develop`** (I read it in `PegSolitarie-dev`):
1. The Solver computes `provenUnsolvable` apart from `cancelled` and `hitBudget` (`Solver.kt:160`). It then returns a plain `Move?` (`:121-131`, `:165`).
2. `GetHintUseCase` passes that `null` through (`:117-120`). Its own KDoc says: "its `null` is 'no move found in time', not a proof" (`:32-34`).
3. `GameViewModel` maps the `null` to `noWinningMove = true` (`:1384-1385`). The comment on those lines admits the budget-miss case.
4. The banner then shows `nowin_message`, "No winning move — undo or restart" (`strings.xml:108`).

A test pins the defect as intended behaviour: `GameViewModelAssistOrderingTest.aHintWhoseSearchMissesItsBudgetIsAlsoFree` (`:162-183`). It asserts `noWinningMove == true` for a budget miss "on a board that may well be winnable".

**I overrule the Phase 8 override on this point.** `08-VERIFICATION.md:9-12` calls the residual "a documented completeness gap, not an untruthfulness defect". The developer accepted that wording; the designer never did. The solver's `null` is honest. The sentence the banner builds from it is not. A gap counts as a completeness gap only while the game presents it as "I don't know".

**Where a winnable position can get the banner:**

| Where | Extent | Evidence |
|---|---|---|
| Wiegleb-45 | ~~Every hint asked four or more jumps in (the fifth hint onward along any line)~~ *(superseded, Amendment 1)* Every position four or more jumps in that is off the committed golden line, until the solver can decide. How deep this reaches is unmeasured. Also the 4 gap positions within depth 3. | Book depth 3 (`OpeningBook.kt:129,159`; `wiegleb45-k3.txt:1-3`). 0 of 76 positions at depth ≤ 4 resolve within the deadline (`Solver.kt:60-64`). Line states past depth 3 are book entries (`docs/tech-design.md` §7.2, reconciliation). |
| Diamond-41 | ~~440 of the 818 positions within four jumps. Also every position five or more jumps in (the sixth hint onward)~~ *(superseded, Amendment 1)* 440 of the **785** positions within four jumps. Also every position five or more jumps in that is off the golden line, where the book stops and the pagoda prune does nothing. | `diamond41-k4.txt:2` (its "of 818" includes line states); `DepthSweepTest.kt:55`: 440 fallbacks of 785 examined, max 1,511 ms; `GetHintUseCase.kt:59-64` |
| Challenge, after the player leaves the recorded line | Larger footprints late in the ladder. Unmeasured. | No test measures hint latency on Challenge levels (helper grep). |
| English-33, European-37, Asymmetric-39 | 0 fallbacks within depth 4. ~~so the first five hints along any line are book answers. Five or more jumps in, the solver answers.~~ *(superseded, Amendment 1)* Five or more jumps in, off the golden line, the solver answers. Low risk there (the full starts solve in 33–59 ms), but unmeasured. | `DepthSweepTest.kt:52-54`; `SolverBudget.kt:68-71` |

All of these numbers come from the development host. A mid-range phone searches fewer nodes in 1.5 s, so on real devices the problem reaches further.

**A correction to F-1's example.** The tech-lead confirmed it independently against `develop` (`tech-lead-release-docs-f8.md`, Open issues; `docs/tech-design.md` §7.2, "What 'No winning move' means today").
- **How far each book reaches.** A depth-K book holds every position up to K moves from the start (`OpeningBook.kt:121,129`).
  - Depth 4: English-33, European-37, Asymmetric-39 and Diamond-41.
  - Depth 3: Wiegleb-45.
  - Two books have gaps inside their depth: 440 positions missing on Diamond-41 and 4 on Wiegleb-45.
- ~~**How many hints that answers.** Along a line that avoids the gaps, the book answers the first K + 1 hints: five on the depth-4 boards, four on Wiegleb-45.~~ *(Superseded, Amendment 1.)* Along any line that avoids the gaps, the book answers at least the first K + 1 hints. Along the committed golden line it answers every hint.
- ~~**What that changes in F-1's example.** On Wiegleb-45, hints 1 to 4 are asked at depths 0 to 3 and are book answers. The first request off the book is the **fifth** hint, after four jumps, and it goes to a solver that resolved none of Wiegleb-45's 76 positions at depth ≤ 4 in time.~~ *(Superseded, Amendment 1.)* F-1's example does not occur: a player who follows Hint from the start stays on the golden line, where every hint is a book answer.
- **The defect stands.** ~~Only the example's count was off by one. Any position four jumps into Wiegleb-45 qualifies, whether or not the player followed Hint to get there.~~ *(Superseded, Amendment 1.)* Every position four or more jumps into Wiegleb-45 qualifies except the golden line's own. Amendment 1's walks A to C are three of them, each provably winnable.

**Severity: MAJOR.**
- **It is not a mechanics blocker.** It causes no crash, no data loss and no softlock, and every move shown is legal and winning.
- **It is still MAJOR, for four reasons:**
  - (a) The game makes a false statement about the puzzle, in the surface that carries its core value, on an ordinary path: ~~any four jumps on Wiegleb-45~~ *(superseded, Amendment 1)* any four jumps on Wiegleb-45 that leave its committed line. That is how most players reach the moment they ask for help.
  - (b) The false statement steers the player's next action. The banner's actions are Undo, Restart and Home, so it tells a player on a winning line to throw that line away.
  - (c) It touches money. The banner's Undo is the ADS-04 path (`GameViewModel.kt:1197-1200`). Once the free undos are spent, the player is offered a rewarded ad to escape a position the game wrongly called dead.
  - (d) It contradicts the headline claim of the 3.0.0 changelog.

**Release: R1 (§3) must merge before `release/3.0.0` is cut.**
- R1 goes on a bugfix branch off `develop`, through the normal review chain.
- Deeper books do **not** block the release.
- Why this blocks:
  - The product baseline in `rules/studio.md` requires a game to pass its rules before it ships, and the fairness rule and the spec of record are among them.
  - The fix is small.
  - Shipping without it means shipping a false statement, plus an ad served on a false premise.
- **No founder question is needed.** This is a fairness ruling within the game-designer's authority, and a bug fix passes through no gate.

## 2. What the player is told

| Outcome | When (domain definition) | Behaviour | What the player must learn | What it must never say | Charge |
|---|---|---|---|---|---|
| **Found** | A book hit, the recorded line, or a move on a line the solver completed | Unchanged | Unchanged | — | Unchanged |
| **Proven dead** | One of three proofs: the book's `NoWinningMove` (`OpeningBook.kt:189`); a solver attempt that exhausted its subtree without hitting its cap or being cancelled (`Solver.kt:160`); a Pagoda reject (`Solver.kt:126`) | Today's rescue banner, non-blocking, with Undo · Restart · Home. The Hint control stays Available. | No winning line exists from this position, so undo or restart to get back to one. On Diamond-41 this means the two-peg best finish is out of reach; the board can still be finished for fewer stars. | — | Free (ADS-06), as today |
| **Undecided** | The search stopped with no move and no proof: the deadline passed, or the cumulative node ceiling or the attempt limit was reached | **No rescue banner, and `noWinningMove` stays false.** A distinct, neutral, non-blocking notice. Hint returns to Available. The notice clears on the next jump, undo, restart or Hint tap, or when the player leaves the board. It is not persisted for resume. | (a) Hint found no move this time, within its time limit. (b) That says nothing about the position: it may still be winnable, so keep playing. | "No winning move", "stuck", "dead end", "lost", any advice to undo or restart, or "try again" on the same board. The solver is deterministic (`Solver.kt:50-53`), so asking again on the same board gives the same answer. UX may invite a new request *after the next move*, but may not promise that it will work. | **Free.** Not counted as a hint used: D-09 counts a hint only when it is revealed (`GameViewModel.kt:1549-1551`), and that rule is unchanged. |
| **Cancelled** | A jump, undo, restart or new Hint tap supersedes the search, or the player leaves the screen, or the process dies | **Silent.** The spinner clears; no notice, no banner. This is already true: `GameViewModel.kt:1343`, `:1375-1378`. | Nothing | Anything | Free |

**A deadline expiry is never "cancelled".** When the board is unchanged, it is Undecided.
- Inside the solver, the player's cancel and the deadline set the same latch (`Solver.kt:193`; the lambda is at `GameViewModel.kt:1368`).
- Mapping that latch to silence would create a fourth, worse state: the spinner vanishes and nothing is said. AC-11 pins this.

**Owed to the ux-designer:**
- The Undecided notice's text.
- Its form: an inline variant with no rescue actions, or the existing snackbar idiom (`adMessage`/`adMessageTick`, `GameViewModel.kt:1528-1532`). That choice is theirs.
- Its TalkBack announcement.
- New rows in the control-state table, ux-spec §4.4 (`docs/ux-spec.md:2418-2431`), next to the `noWinningMove` row at `:2426`.
- Optionally, a re-check that `nowin_message` reads true for Diamond-41's two-peg floor.

The information rules in the table above bind the wording.

## 3. Remedies, the pick, and acceptance criteria

| # | Remedy | Fixes the false claim? | Raises hint availability? | Cost and risk | Verdict |
|---|---|---|---|---|---|
| R1 | A distinct Undecided outcome carried from the Solver through `GetHintUseCase` and the ViewModel to the UI: a result type of Found(move), ProvenDead or Undecided in place of `Move?` | **Yes, on every board and in Challenge** | No | Small. The split already exists at `Solver.kt:160`. It touches the `:domain` return types, one ViewModel branch, one UiState field, one string and the tests. | **PICK. Blocks the release.** |
| R2b | Line continuity: book entries carry their whole proven line, and the solver's found line is kept, so a player who follows Hint stays on a proven line and every later hint is instant | ~~It ends F-1's "follow the hints" case~~ *(superseded, Amendment 1: from the untouched start that case already never reaches the solver.)* It ends the same case for a player who left the golden line, got a Found, and follows Hint from there. | Yes, exactly where players lean on Hint after a deviation | The book format changes and the books are re-authored. Keeping the solver's line is cheap: it already returns the whole line (`Solver.kt:156-159`). | Follow-up for 3.1, first in line |
| R2 | Deeper books: Wiegleb-45 to K=4 (the todo `08-VERIFICATION.md:10` already records) and Diamond-41's 440-position gap closed | Only within the new depth; a frontier always remains | Yes, near the start | Offline authoring, which has already failed to decide these positions; larger resources; the sweep already takes about 11 minutes (`docs/tech-design.md:538-539`) | Follow-up for 3.1 |
| R3 | A longer deadline | No | Barely | D-07 (`08-CONTEXT.md:33`) freezes 1.5 s as product UX. The Wiegleb gap is about 10⁴, which no tolerable wait closes. | **Rejected.** D-07 is reaffirmed. |
| R4 | A progressive or background budget: show Undecided at 1.5 s, keep searching, and upgrade the answer later | Yes | Somewhat | A late answer reopens the stale-board problem (CR-01). A late find would be chargeable, so it would raise an ad offer the player never asked for (`GameViewModel.kt:1388-1401`). It also costs battery. | Rejected for 3.0.0. May be revisited after R5. |
| R5 | A census (a measurement, not a fix): for each board and depth, the rates of Found, ProvenDead and Undecided | — | — | Uses a node budget equal to 1.5 s on the reference device, never wall clock (`rules/testing.md`) | Follow-up for 3.1. Required before any copy quotes coverage. |

**Pick: R1 now. R2b, R2 and R5 go into 3.1 as one work item.**

**Acceptance criteria for R1.** Test names are suggested and read as sentences. "Undecided" names the outcome; the type name is the developer's choice.

*Domain (`:domain`, pure JVM, deterministic, no wall clock):*
- **AC-1** `aSearchCutShortByCancellationIsUndecidedNotDead` (GetHintUseCaseTest)
  - **Given** an off-book Classic position, from the existing `offBookPositionAtDepth(6)` helper (`GetHintUseCaseTest.kt:356-363`), and a `shouldCancel` that returns true on its first poll
  - **When** the hint use case runs
  - **Then** the outcome is Undecided, never ProvenDead.
- **AC-2** `aWinnableWiegleb45PositionOffTheGoldenLineIsNeverReportedDead`
  - ~~**Given** a position four jumps from Wiegleb-45's start, one ply past its depth-3 book, so this is the fifth hint along a line. Its book miss is asserted as a precondition. Use the same forced cancel.~~ *(Superseded, Amendment 1: following hints never produces this position.)*
  - **Given** each of the positions after Amendment 1's walks A, B and C, with three preconditions asserted:
    - the position is a book miss;
    - it is not the golden line's depth-4 state;
    - it is winnable: the same symmetry applied to golden moves 5 to 43, replayed through `RulesEngine`, ends at one peg.
  - Use the same forced cancel.
  - **When** the hint use case runs
  - **Then** the outcome is Undecided on all three, never ProvenDead.
- **AC-2b** `aWiegleb45PositionThreeJumpsInIsStillABookAnswer` (the frontier control)
  - **Given** a position three jumps from Wiegleb-45's start that is not one of the 4 gap positions, and a cancel spy
  - **When** the hint use case runs
  - **Then** the outcome is Found, with zero solver polls. ~~This pins the boundary at four answers from the book: hints 1 to 4.~~ *(Superseded, Amendment 1.)* This pins the depth-3 frontier as served from the book. Past it, only golden-line states are.
- **AC-2c** `followingHintFromEachClassicStartStaysOnTheBookToTheEnd` (new, Amendment 1)
  - **Given** each of the five untouched Classic starts and a cancel spy
  - **When** the hint use case is asked at every state reached by playing its own previous answer, to the end of the line
  - **Then** every outcome is Found with zero solver polls, and each move equals the committed golden line's move at that depth.
  - This pins §16 #22's finding as intended behaviour, so R1 cannot regress it.
- **AC-3** `aBookProvenDeadPositionIsProvenDeadWithoutTheSolver`
  - **Given** one of English-33's four authoritative dead entries (`proven-dead=4`; `DepthSweepTest.kt:28-30,52`)
  - **When** the hint use case runs
  - **Then** the outcome is ProvenDead, with zero solver polls.
- **AC-4** `anExhaustedSearchIsProvenDead`
  - **Given** a small board with at least one legal move and no line to its target, the default budget and no cancel
  - **When** the hint use case runs
  - **Then** the outcome is ProvenDead.
- **AC-5** `aPagodaRejectIsProvenDead`
  - **Given** a position that `Pagoda.mayBeSolvable` rejects
  - **When** the hint use case runs
  - **Then** the outcome is ProvenDead.
- **AC-6** `aSearchThatSpendsItsNodeCeilingIsUndecided`
  - **Given** a winnable off-book position and a `SolverBudget` too small to finish, for example `maxNodes = 1`, with no cancel
  - **When** the hint use case runs
  - **Then** the outcome is Undecided.
- **AC-7** The existing tests for Found (recorded line, book hit, legality guard) stay green, and their meaning does not change.
- **AC-8** `DepthSweepTest` clause 3 (`:38-39`) changes:
  - **Then** a fallback is Found (and legal) or Undecided; it is ProvenDead only when an exhausted search proved it.
  - The sweep reports the Undecided count next to the fallback count.

*ViewModel (app JVM tests; `runTest`/Turbine; a fake `TimeSource`):*
- **AC-9** Rewrite `aHintWhoseSearchMissesItsBudgetIsAlsoFree` as `anUndecidedHintIsFreeAndNeverRaisesTheNoWinningMoveBanner`.
  - **Given** the use case answers Undecided for the board on screen
  - **When** the player taps Hint
  - **Then** all of the following hold:
    - `noWinningMove` is false and `showRescueBanner` is false;
    - the Undecided notice is up and `solverThinking` is false;
    - no allowance unit is spent, and the gateway receives no request.
- **AC-10** `aProvenDeadHintStillRaisesTheNoWinningMoveBannerForFree`
  - **Given** the use case answers ProvenDead
  - **When** the player taps Hint
  - **Then** the behaviour is exactly today's.
- **AC-11** `theDeadlinePassingOnAnUnchangedBoardShowsTheUndecidedNoticeNotSilence`
  - **Given** the real use case on ~~an off-book Wiegleb-45 position~~ the position after Amendment 1's walk A (its book miss asserted), and the fake `TimeSource` moved past 1.5 s
  - **When** the search ends
  - **Then** the Undecided notice shows.
- **AC-12** `aSupersededSearchShowsNoNoticeAndChargesNothing` (GameViewModelHintStalenessTest)
  - **Given** a search in flight
  - **When** the player jumps, undoes, restarts or taps Hint again
  - **Then** the old search raises neither the notice nor the banner, and nothing is charged.
- **AC-13** `theUndecidedNoticeClearsOnTheNextJumpUndoRestartOrHint`
- **AC-14** `anUndecidedHintDoesNotCountAsAHintUsed`
  - **Given** an Undecided answer
  - **Then** the per-level hint allowance and the hint-free telemetry (PGSA-02) are unchanged.
- **AC-15** `theUndecidedNoticeIsNotRestoredOnResume`
  - **Given** the notice is up when the process dies
  - **When** the game resumes
  - **Then** neither the notice nor the banner shows.

*UI (Compose UI test):*
- **AC-16** `theUndecidedNoticeShowsItsOwnTextAndNoRescueActions`
  - **Given** a state with the Undecided notice
  - **Then** all of the following hold:
    - the text the UX spec specifies shows;
    - `nowin_message` does not show, and neither do the Undo, Restart and Home rescue actions;
    - TalkBack announces the notice as the UX spec specifies.
- **AC-17** `nowinMessageIsBoundOnlyToProvenDead`: no path renders `R.string.nowin_message` from the Undecided outcome.

*Device (qa-engineer, on the emulator or a physical phone):*
- **AC-18**: ~~Given Wiegleb-45, when the player follows Hint for four moves, each hint shows a move at once; when the player taps Hint a fifth time, the Undecided notice appears; repeat on two further positions four jumps in that were reached without following Hint.~~ *(Superseded, Amendment 1. It cannot pass: the fifth followed hint is a book answer, §16 #22.)* Replaced by:
  - **AC-18a, the control.**
    - **Given** Wiegleb-45 from its untouched start
    - **When** the player follows Hint for as many moves as the level's free hints allow
    - **Then** every hint shows a move at once, with no visible wait, and no notice or banner ever appears.
  - **AC-18b, off the book.**
    - **Given** Wiegleb-45 from its untouched start
    - **When** the player makes Amendment 1's walk A by hand, with no Hint until the fourth jump is done, and then taps Hint
    - **Then** the spinner runs for about 1.5 s and the Undecided notice appears. "No winning move" must never appear: the position is winnable by construction.
  - **AC-18c and AC-18d.** The same as AC-18b, with walk B and walk C. Restart the level between walks.
  - **The pass line:**
    - "No winning move" appears on none of the three walks; any appearance fails the AC.
    - The Undecided notice appears on each walk.
    - A walk that shows a move instead does not break the truth rule. QA records the device and the walk. The developer then supplies a replacement walk from the remaining four symmetry images: the top–bottom mirror, the anti-transpose, and the 90° and 270° rotations.
    - If any walk shows a move *at once*, that is the book, which means the screen's orientation differs from the coordinates. QA reports it before continuing.
- **AC-19**
  - **Given** English-33 at one of its four book-proven dead positions (the developer supplies the move sequence)
  - **When** the player taps Hint
  - **Then** "No winning move — undo or restart" appears at once.
- **AC-20**
  - **Given** Diamond-41 at one of its 440 gap positions, which are 440 of the 785 positions within four jumps of the start. The developer supplies the position as a move sequence, from the sweep. *(Count corrected, Amendment 1.)*
  - **When** the player taps Hint
  - **Then** the Undecided notice appears, never "No winning move".
- **AC-21**
  - **Given** the free hints for the level are spent
  - **When** a hint comes back Undecided
  - **Then** no ad offer appears.

## 4. What the Play listing and the changelog may claim

The release-manager owns the final copy. The Turkish texts follow the same rules.

| Claim | Today (`develop`) | After R1 merges |
|---|---|---|
| "Hint shows a real winning move." | **True. Allowed.** Every move shown is proven or found on a complete line, and passes the legality guard. | Allowed |
| "When it says there is no winning move, there is none from that position." (`full_description.txt:15`) | **False. Remove it.** | Allowed |
| "If Hint can't work a position out in time, it tells you so instead of guessing." | False | Allowed |
| "Hints that tell the truth on every board" (`changelogs/30000.txt:3`) | **False. Remove it.** | Literally true, but it reads as a promise that Hint answers on every board. Do not use it. |
| "'No winning move' now means *proven*, not *gave up*" (`CHANGELOG.md:141`, as reworded on this branch) | **False. Remove it.** | Allowed |
| "a built-in opening book for the first four moves (three on Wiegleb-45)" (`CHANGELOG.md:140`, as reworded) | **Not allowed as worded.** The depths are right if read as positions: up to four moves in on English-33, European-37, Asymmetric-39 and Diamond-41, and three on Wiegleb-45. ~~That is five book-answered hints along a line, or four on Wiegleb-45.~~ *(Superseded, Amendment 1: at least that many along any line that avoids the gaps, and every hint along the committed line.)* But the sentence promises the whole range, and Diamond-41 lacks 440 of its 785 positions in it and Wiegleb-45 lacks 4 of its 101. Use "an opening book that covers the first moves of every Classic board". | The same wording |
| `CHANGELOG.md:137-139`: a real winning move "from a difficult early position on … Wiegleb-45", "instead of folding to the 'no winning move' banner" | **Not allowed as worded.** ~~On Wiegleb-45 it holds only for positions up to three moves in, which is the first four hints. From the fifth hint on, a winnable position can still get the banner today.~~ *(Superseded, Amendment 1.)* On Wiegleb-45 it holds for positions up to three moves in, and along the board's committed winning line. Any other position four or more moves in can still get the banner today; walks A to C are three. Use "from the opening positions", and drop "instead of folding to the banner" until R1 merges. | "from the opening positions" stays. The banner clause may return as "and it never shows 'no winning move' unless that is proven". |
| "Faster hints on the big boards" | True: the openings of European-37, Asymmetric-39 and Wiegleb-45 are now answered instantly. ~~, which is the first five hints along a line, four on Wiegleb-45~~ *(superseded, Amendment 1)* Allowed. | Allowed |
| "Follow Hint from the start and every hint comes at once" (new, Amendment 1) | **True on all five Classic boards.** Every hint along the committed line is a book answer (§16 #22; AC-2c pins it). Allowed. It is a claim about following Hint from the start, not about every position. | Allowed |

**Not allowed until R5's census exists:**
- "Hint always finds a winning move", or "a hint for every position".
- "Instant hints" stated as a universal. ~~Off-book positions still wait up to 1.5 s: from the fifth hint on Wiegleb-45, from the sixth on the other boards, and in Diamond-41's 440 gap positions.~~ *(Superseded, Amendment 1.)* Off-book positions still wait up to 1.5 s:
  - positions off the committed line, four or more moves in on Wiegleb-45 and five or more on the other boards;
  - Diamond-41's 440 gap positions and Wiegleb-45's 4.
- Any count or percentage of positions covered.

**Suggested lines:**
- **Listing, today:** "Hint shows a real winning move. Undo takes back your last jump."
- **Listing, after R1:** "Hint shows a real winning move. When it says there is no winning move, there is none from that position, and when it can't work a position out in time, it tells you so."
- **Changelog, today:** "Faster hints on the big boards, answered from a built-in opening book".
- **Changelog, after R1:** "Hints you can trust: 'no winning move' appears only when it is proven".

## Designer decisions (recorded here until routed, because this product has no `docs/gdd.md`)
- **GD-HT-1** A hint search has three outcomes the player sees: Found, ProvenDead and Undecided. Only ProvenDead may raise "No winning move". This supersedes Phase 4 D-04, whose precondition is falsified, and the reasoning of override 1 in `08-VERIFICATION.md`.
- **GD-HT-2** Undecided behaves and informs as §2 rules: a neutral notice, free, no rescue banner, transient.
- **GD-HT-3** A cancelled search is silent. A deadline expiry on an unchanged board is Undecided, never cancelled.
- **GD-HT-4** D-07 is reaffirmed: the 1.5 s deadline stays, with no longer or progressive budget in 3.0.0.
- **GD-HT-5** ADS-06 is extended: a hint that reveals no move, whether proven dead or Undecided, is never charged. D-09 (a hint is counted only when revealed) is unchanged.
- **GD-HT-6** For 3.1, R2b, R2 and R5 form one work item. Its target is that a player who follows Hint never sees Undecided. *(Sharpened, Amendment 1.)* From the untouched start this already holds (§16 #22). The target is that it also holds from any position where Hint gave a Found.

**Requirement text for gsd to record** (the producer routes it to REQUIREMENTS.md and PROJECT.md):
- **New ASST-04:** "A hint that cannot decide a position within its deadline tells the player so; the 'no winning move' state appears only for a position proven to have no winning line."
- **ADS-06, reworded:** "A hint that reveals no move — proven dead or undecided — is never charged against the allowance or an ad."
- **PROJECT.md Key Decisions, the hint row (`:100`):** the residual sentence "…burn the full deadline and show the rescue banner" becomes "…burn the full deadline and report Undecided".

## The-fool pass
**Mode.** This role has no AskUserQuestion, and roles never address the founder. I therefore chose the modes myself:
- a pre-mortem on R1;
- an evidence audit of F-1.

**The opposing position, steelmanned.** "Ship 3.0.0 with honest copy. It is already strictly better than the live 2.x, where even Wiegleb-45's starting position got the banner. Fix the banner in 3.0.1."

**The five strongest challenges, and my answer to each:**
1. **"The problem is pre-existing, and 3.0.0 already improves on it."**
   - Conceded: 3.0.0 is better than what is live.
   - Not conceded: that it ships as is. The fix is one small branch. Honest copy does not remove the harm of a banner on a winning line with an ad behind its Undo.
2. **"Undecided will show often on Wiegleb-45 and Diamond-41, and Hint will look broken."**
   - Conceded: this is a real risk. R2b, R2 and R5 in 3.1 address it.
   - Even so, a confident wrong answer costs the player the winning line, and Undecided costs nothing.
3. **"It depends on the device: the same position can be Found on a fast phone and Undecided on a slow one."**
   - True, and acceptable. A Found move and a ProvenDead result are correct on any device; only the Undecided rate varies.
   - Tests use forced cancels and node budgets, never wall clock.
4. **"Some early blunders on the large boards cannot be proved dead within 1.5 s, so they will show Undecided instead of the rescue banner."**
   - Accepted. Undo and Restart stay in the dock.
   - A neutral notice on a dead position misleads no one. The reverse, a dead-end banner on a live position, misleads.
5. **"There is an implementation trap: the deadline and a player's cancel set one latch."**
   - Real. Mapping that latch to silence would leave a worse fourth state. AC-11 pins it.

**Evidence audit of F-1.**
- I verified the code path by reading it: `Solver.kt:121-165` → `GetHintUseCase.kt:117-120` → `GameViewModel.kt:1384-1385` → `strings.xml:108`.
- ~~The concrete example is off by one: it is the fifth hint, not the fourth. The tech-lead's check against `develop` reached the same count (`tech-lead-release-docs-f8.md`, Open issues).~~ *(Superseded, Amendment 1.)* The concrete example does not occur at all. Following Hint from the start stays on the golden line, and every hint along it is a book answer (§16 #22). My own first audit missed that, because I read the book's depth as its whole reach.
- The defect is reached off the line instead, and walks A to C prove it on positions that are winnable by construction.
- The defect does not depend on how often it happens. One winnable position shown as dead is enough, and the code's own comment admits the case.

**Synthesis.** R1 now, with its scope bounded. Hint availability waits for 3.1.

## Outputs
- `.planning/reports/phase-14/game-designer-hint-timeout.md`: this report.
  - It holds the four rulings, decisions GD-HT-1 to GD-HT-6, and AC-1 to AC-21: AC-2b and AC-2c are added, and AC-18 is split into AC-18a to AC-18d.
  - **Amendment 1** (2026-10-21) carries the golden-line correction (§16 #22) and walks A to C; the superseded lines are struck through in place.
  - I wrote no other file and committed nothing.

## Checks
I have no Bash, so every check here is a full or ranged read of the source on `develop` (in `PegSolitarie-dev`) or of the docs on this branch (in `PegSolitarie-r5`).
- `Read domain/.../usecase/GetHintUseCase.kt` (all 122 lines): pass. The `null` is "not a proof" (`:32-34`); the solver's `null` is passed through (`:117-120`).
- `Read domain/.../solver/Solver.kt:1-200`: pass. The proof and the cut-off are told apart at `:160` and folded together at `:121-131` and `:165`; one latch for cancel and deadline (`:193`); deterministic seeds (`:50-53`).
- `Read app/.../ui/game/GameViewModel.kt`, lines 1188-1217, 1320-1429, 1500-1569 and 1640-1667: pass.
  - `null` becomes `noWinningMove` at `:1384-1385`.
  - The banner's Undo is the ADS-04 path (`:1197-1200`).
  - A hint is counted only on reveal (`:1549-1551`).
- `Read domain/.../assist/OpeningBook.kt` (all): pass. Wiegleb-45's depth is 3 (`:129,159`); the three-outcome contract is at `:184-192`.
- `Read golden/book/wiegleb45-k3.txt:1-6` and `diamond41-k4.txt:1-6`: pass. 4 of 140 and 440 of 818 positions are absent; `proven-dead=0` in both. *(Amendment 1: the headers' denominators include the golden line's states. As position counts they are 4 of 101 and 440 of 785.)*
- `Read domain/.../solver/SolverBudget.kt` (all): pass. D-07 freezes 1.5 s (`:84-95`).
- `Read DepthSweepTest.kt:1-105`: pass. The measured table is at `:50-56`.
- `Read GetHintUseCaseTest.kt` (all) and `GameViewModelAssistOrderingTest.kt:140-189`: fail against the spec. The defect is pinned as intended behaviour at `:162-183`.
- `Read 04-CONTEXT.md` (all): pass. D-04's precondition is at `:32`.
- `Read 08-CONTEXT.md` (all) and `08-VERIFICATION.md:1-70`: pass. D-07 is at `:33`; the override reasoning is at `:9-12`.
- `Read` design spec §6, `v1.0-REQUIREMENTS.md` (ASST-03), `.planning/REQUIREMENTS.md` (ADS-04, ADS-06, DEBT-01) and `PROJECT.md:100`: pass.
- `Read` `docs/store/en-US/full_description.txt`, `changelogs/30000.txt`, `CHANGELOG.md:120-159`, `docs/tech-design.md:520-549` and `:855-889` (this branch), and `docs/ux-spec.md:2418-2431` (`develop`): pass. These are the claims that §4 rules on.
- `Read .planning/reports/phase-14/tech-lead-release-docs-f8.md` (all), `docs/tech-design.md:528-581`, `CHANGELOG.md:132-145`, `docs/store/en-US/full_description.txt:13-15` and `changelogs/30000.txt` (this branch, after the coordinator's correction). Pass:
  - Wiegleb-45's book is depth 3 and the other four boards are depth 4 (`OpeningBook.kt:121,129`).
  - The gaps are 440 on Diamond-41 and 4 on Wiegleb-45.
  - ~~The first hint off the book is the fifth on Wiegleb-45 and the sixth elsewhere.~~ *(Superseded, Amendment 1: that holds only off the committed line.)*
  - The ruling, the reach table, AC-2, AC-2b, AC-18, AC-20 and §4 now use these depths.
- **Amendment 1 checks:**
  - `Read .planning/reports/phase-14/tech-lead-release-docs-blockers.md` (all) and `docs/tech-design.md:540-646` (this branch, committed): pass. §16 #22 says every golden-line state is a book entry and `lookup` has no depth check. The reconciliation table gives 101 and 785 as the frontiers.
  - `Read domain/.../assist/OpeningBook.kt:184-192` (`develop`): pass. `lookup` carries no depth check.
  - `Read domain/src/test/resources/golden/solutions/wiegleb45.txt` (all 43 lines, `develop`): pass. Golden moves 1 to 4 are as quoted in Amendment 1.
  - **Walks A, B and C, worked by hand from that file and the Wiegleb-45 footprint** (cells where row or column is 3 to 5; start hole (4,4)):
    - every jump is legal in order;
    - each walk ends at 40 pegs, with the empty holes listed in Amendment 1;
    - none equals the golden depth-4 state.
  - The android-game-dev re-proves all of this in AC-2's preconditions.
  - `CHANGELOG.md:137-141` was reworded since my first read, and §4 rules on its current text.
- Two Explore helpers on haiku: path location, and a line-number grep over the ux-spec, `DepthSweepTest` and the Challenge latency tests (none exist).
- **Not run:** `./gradlew check`. I have no Bash, and this ruling changes no code and no Check input.

## Open issues / risks
- **The reach beyond each book's depth is unmeasured** on every board and in Challenge, and phones search more slowly than the host. R5 is the fix.
- ~~**Two count mismatches:** the book headers say 140 and 818 positions at depth ≤ K, while `DepthSweepTest` examined 101 and 785. This is probably different counting. It is not a ruling matter, but the tech-lead should reconcile the figures that the docs and the changelog quote.~~ *(Resolved: the tech-lead reconciled them in `docs/tech-design.md` §7.2, "The book counts, reconciled"; see Amendment 1.)*
- ~~**`08-VERIFICATION.md` override 1 needs a correction note**, pointing to GD-HT-1.~~ *(Done by the tech-lead, `tech-lead-release-docs-blockers.md`.)* That note still quotes "440/818"; §7.2 governs.
- **The hint brief on `bugfix/hint-undecided` carries the old premise.** Its §7 Device line includes "two reached without following Hint", per `tech-lead-release-docs-blockers.md`. It needs the same correction as AC-18; that file is outside this dispatch.
- **AC-18b to AC-18d depend on the measured host result,** none of the 76 canonical positions resolving. A phone is slower, so Undecided is the expected result. The pass line still treats a Found as a recorded event, not a failure.
- **The Phase 8 physical-device checklist was never run** (`08-VERIFICATION.md:30-33`). AC-18 to AC-21 overlap with it.
- **Timing:** if the release-manager finalises the copy before R1 merges, use the "Today" column of §4, then switch to the "After R1" column on merge.

## Handoff
Next: producer. Route the following.
- **ux-designer:** the Undecided notice's text, form and TalkBack announcement, and the ux-spec §4.4 rows (wording owed, §2).
- **tech-lead:**
  - Confirm the shape of the outcome type and the list of files it touches.
  - Schedule a bugfix branch off `develop` (suggested name `bugfix/hint-undecided`) before `release/3.0.0`.
  - When R1 merges, update tech-design §7.2's transcription, "What 'No winning move' means today" (`:550-577`), and the "Hint latency" row in §10.
  - Close §16 #21 and the §15 risk row with this ruling, GD-HT-1 to GD-HT-6.
- **android-game-dev:** build R1 test-first against AC-1 to AC-17, including the new AC-2c.
  - Turn Amendment 1's walks A to C into AC-2's fixtures, with the three preconditions asserted.
  - Supply the move sequences for AC-19, AC-20 and any replacement walk.
  - Correct the hint brief's §7 Device line to AC-18a to AC-18d.
- **code-auditor and qa-engineer:** review the branch. QA runs AC-18a to AC-18d and AC-19 to AC-21 on a device. *(Amendment 1: AC-18 is replaced.)*
- **release-manager:** write the copy now from §4's "Today" column, and switch to the "After R1" lines when R1 merges. The reworded `CHANGELOG.md:137-141` still needs the two changes in §4: "first four moves (three on Wiegleb-45)" overstates coverage on the two gap boards, and "now means *proven*" is false until R1 merges.
- **gsd (through the producer):** record ASST-04 and the ADS-06 rewording in REQUIREMENTS.md, update the PROJECT.md Key Decisions row, and add the correction note to `08-VERIFICATION.md`.
- **3.1 backlog:** R2b, R2 and R5 as one item (GD-HT-6).
