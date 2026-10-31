# tech-lead report — tech-design-r1-close (R1's and audio's closes in `docs/tech-design.md`)
Date: 2026-10-24
Branch: feature/tech-design-r1-close (committed by the producer; cut from `develop` at `6aa7624`; worktree `/Users/ermanergoz/Desktop/PegSolitarie-r1c`; nothing committed by me, as dispatched)
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game — the worktree's snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin's studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing and dispatch rule files were read in full, plus the snapshot's `.claude/rules/lessons.md`), android-architecture, android-gradle-logic (these two were invoked after the edits and before this report; nothing in them changed an edit)
Ran in parallel with: none named by the dispatch (other worktrees exist on this host; I ran no Gradle)
Ran on: claude-opus-5-5 at effort xhigh (policy). **Staffing deviation, recorded:** the agent file pins fable at xhigh. The dispatch reports the Fable limit reached, so this run is on Opus under Q-019's answer 3 (`.planning/questions/FOUNDER.md`: "Opus for every Fable-pinned role until 3.0.0 is published", each report recording the deviation). The author of this text is therefore on Opus; its reviewer (the code-auditor's text read, then a fresh tech-lead's sign-off) should be fresh agents that check each claim against git and the code rather than against this report.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons for game / android-compose-game; `.planning/recall/RECALL.md` does not exist)

## Summary
- `docs/tech-design.md` now carries both closes the dispatch named: R1's (`.planning/reports/hint-undecided/tech-lead-signoff.md` §4, all nine items) and audio's (`.planning/reports/audio-review/tech-lead-signoff.md`, "Merge-in re-read" R.4, all thirteen items).
- New §16 numbers continue after `develop`'s #27: **#28** the frozen reduced-motion gate, **#29** the GM6 Diamond-41 jump, **#30** the TalkBack-by-hand residual, **#31** the `GameViewModelResumeTest` drain hang. #17 and #21 are closed.
- Every table keeps its column count (20 tables, 0 mismatches, before and after). Every superseded sentence stays in place as a dated record, and §17 gains five rows (two existing rows amended).
- Four small consistency additions beyond the two lists are flagged under "Beyond the two lists" for the reviewer to judge.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-r1c/docs/tech-design.md` — the edits below (+188 / −48 lines; every removed line is a line rewritten with its old text kept, dated).
- `/Users/ermanergoz/Desktop/PegSolitarie-r1c/.planning/reports/phase-14/tech-lead-tech-design-r1-close.md` — this report.

### R1's close (`hint-undecided/tech-lead-signoff.md` §4), item by item
1. **§7.2 rewrite** — a new subsection, "What 'No winning move' means now" (`:584`), states the three outcomes as built on `develop` (`b1a6cf9`), checked against the code: `Solver.decide` → `SolverOutcome` (ProvenDead only from a board at target, a Pagoda reject, or an exhausted, uncancelled, uncapped attempt); `GetHintUseCase` → `HintOutcome`, the only type `:app` imports, book `NoWinningMove` → ProvenDead with no search, both mappings exhaustive with no `else`; the ViewModel's `noWinningMove` / `hintUndecided`, neither charged; `showRescueBanner` still `stuck || noWinningMove`; the notice in the banner's slot, one occupant at a time (`UndecidedNoticeSlot`); a deadline on an unchanged board is Undecided, a superseded search is never observed (brief §2); where Undecided can appear, with QA's device figures (GM6 6/6, emulator 17/17); what `DepthSweepTest` (AC-8) proves. The 2026-10-21 box and the `0582e3f` transcription stay below it, under a dated "Superseded 2026-10-24 (§17)" line (`:633`) and a retitled heading, "meant until R1 merged" (`:636`).
2. **§10 Hint-latency row** (`:1196`) — a miss reads Undecided, never "No winning move"; the golden-line exception is now the row's text; the old cell is quoted as "Until 2026-10-24 this cell read …".
3. **§15 row** (`:1714`) — reworded to the residual Undecided rate off the book, unmeasured until R5 (3.1, GD-HT-6), with QA's figures as the first data point; likelihood/impact re-rated (my judgement: high off the book / low, since the claim is now truthful); the old risk, rating, mitigation and owners kept as dated quotes.
4. **§16 #21** (`:1748`) — **Closed 2026-10-24** on `b1a6cf9`, citing GD-HT-1 to GD-HT-6, the brief with Amendments 1 and 2, the developer's, auditor's and QA's reports and the sign-off; #22 noted as already closed.
5. **Book-count note** (`:718`) — confirms 136 = 97 + 39 (Wiegleb-45), 378 = 345 + 33 (Diamond-41), 399 = 373 + 26 (English-33, its 4 proven-dead entries included) against the frontiers 101 and 785; closes the brief's open bullet; records the stale `Solver.firstWinningMove` KDoc at `AuthoringSolver.kt:250` (it is in `domain/src/test/.../golden`, frozen by brief A2.3) as owed to the 3.1 R2/R2b item.
6. **§14.1 R1** (`:1557`) — **Done**: signed off, merged `b1a6cf9` (step 3a done, `:1600`); the brief §10.3 second-to-merge duties discharged by check-gates (`ffa49aa` → `dbd0a6c`) and audio (`13c04c6` with the `GameAnim.kt` keep-both, `9f0e67a` for the `LongMethod` list move → `6aa7624`). The old cell stays after "*The record:*".
7. **§8.1 convention line** (`:813`) — the default stays `junit4.createComposeRule` (v1); `junit4.v2` only where a test needs a behaviour v1 lacks, with the reason in the class KDoc; today one class, `GameScreenHintNoticeTest`; its KDoc sentence rides the next branch that touches that file; no other migration until the §9.8/§14.2 one. I give the measured count instead of the sign-off's "29": 33 suites import v1 on `6aa7624`.
8. **Three new §16 rows** — #29 GM6 Diamond-41 (`:1756`; MINOR provisional, MAJOR and release-blocking through the checklist if it reproduces on an API 27 emulator image with the screen on, or by finger); #30 TalkBack by hand (`:1757`; 23j item 5's spoken order and 23k's spoken traversal, carried to row TB's device pass, owed before `release/3.0.0`, the founder-run fallback as a non-blocking question through the producer); #31 the `GameViewModelResumeTest` drain hang (`:1758`; test-only, after the cut's code branches).
9. **§17 check** — the D-04 → GD-HT-1 row's claim holds: `.planning/phases/08-assist-generator-quality-debt/08-VERIFICATION.md:43` is "## Correction — 2026-10-21: override 1's … overturned (GD-HT-1)", committed in `aaaba67`. Recorded in that row's Where cell (`:1795`).

### Audio's close (`audio-review/tech-lead-signoff.md`, "Merge-in re-read" R.4), item by item
1. **Header** (`:15`) — the haptics ruling "on `feature/audio-review` — *on `develop` since `6aa7624`, 2026-10-24*"; and this sync's own dated sentence (`:34`), naming both closes (§17 hunk 1's form).
2. **§2.4** (`:221`) — "on `develop` since `6aa7624`", the N-5 sentence kept as "until then this read …".
3. **§9.8 rule 2** (`:1148`) — the recorded exception: new Compose suites follow the house v1 rule until the §14.2 migration, and the deprecation line such a suite adds is not a no-growth finding; the v2 rule only as §8.1 allows. **The count** (`:1104`): the table row takes the merged tree's own figure, 33 on `6aa7624` — `git grep -l 'import androidx.compose.ui.test.junit4.createComposeRule' 6aa7624 -- app/src/test app/src/testDebug | wc -l` = 33, matching the 33 such `w:` lines of audio's merge-in Check (`audio-review/android-game-dev-merge.md:30`); "29 … until 2026-10-24" kept. Rule 2's reasoning line gets "(33 on `6aa7624`)".
4. **§9.8 rule 3** (`:1163`) — the "if check-gates merges after audio" is history: check-gates first (`dbd0a6c`), audio's merge-in `13c04c6`, audio's main code 0 `w:` in debug and release; the rule stands for later branches.
5. **§12** — current state: `GameAudioAttributes` (`:1268`); the twelve WAVs, 7 Kenney + 3 freesound-derived + 2 in-house from `audio/recipes/` (`:1270`), checked against `res/raw`, `audio/recipes/` and `docs/audio/CREDITS.md`; the `.ogg` bullet as a dated quote with its replacement commits (`af2b554`, `d1c6e9b`); the design file on `develop`, C1 done; where each A1–B5 decision now lives (table unchanged, the locations in the lead-in sentence, so the table keeps two columns); the device checks as proven on the emulator (API 36) and the GM6 (API 27), the two open halves (the speaker branch on an API 24–32 phone; D4, wired) and Q-025 (`:1310`, `:1317`); the haptics as built — `HapticsSetting` (one member, `SettingsRepository`, `DataModule`), `JumpHapticEffect` from both screens, `WrongTapHaptic` private to `GameScreen` through `NewOrdinalEffect` with `wrongTapHapticFires` = `hapticsOn && sdkInt >= R`, the tutorial with no `wrongTapCorrection` (`:1330`), the old paragraph quoted.
6. **§13 Sounds row** (`:1359`) — current, the old cell quoted.
7. **§14.1 audio row** (`:1559`) — **Done**: merge sha `6aa7624`; "no 15e sitting (`audio-review/tech-lead-signoff.md` §2.4)"; the merge-in `13c04c6` with its three conflicts (two per §2.3, FOUNDER.md by its format), the merged Check (1,565 / 0; fuzz 10,000 / 0; kover 99.02 %), `code-auditor-merge.md` and the re-read. The N-5 parenthetical is kept dated rather than dropped, per the dispatch's "each superseded sentence stays".
8. **§14.1 merge order** — step 5 done with its shas (`:1611`); step 5a's precondition met and its "should audio-review slip" clause moot (`:1621`).
9. **Record-file merge rule** (`:1654`) — R.2's (a)–(c) under the second-to-merge paragraph, stating that §17's merge-in rule governs this file only; **and a §17 row** (`:1826`), as the dispatch asked. R.4 item 9 places the rule in §14.1 and the dispatch asks for "a §17 row"; I did both: the rule lives in §14.1, and §17 records what it supersedes (the paragraph's silence on records, §2.2's named-branch measurement, the merge commit's citation of §17 for FOUNDER.md).
10. **§14.1 row RM** (`:1560`), `bugfix/reduced-motion-live-gate`, **Blocks the branch cut**: brief = audio sign-off §3 plus §5's riders; chain android-game-dev → code-auditor + qa-engineer → tech-lead; cut 2026-10-24 at `6aa7624` (`git merge-base bugfix/reduced-motion-live-gate develop` = `6aa7624`, worktree `PegSolitarie-rm`); no 15e sitting with the ruled mechanism; its slot against 5a (both edit `GameScreen.kt`'s effect block; the second to merge carries the merge-in and its re-read).
11. **§15 row** (`:1715`) — the frozen gate: certain on a live toggle / medium (accessibility), its branch and its deadline.
12. **§16** — #17 closed (`:1744`): "built on `feature/audio-review` (`8f18049`, 2026-10-21), merged as `6aa7624`"; **#28** (`:1755`) the gate, with §3's mechanism.
13. **§17** — one row for the branch-conditional audio text (`:1824`), which also corrects the sign-off's §6 line "§17 → nothing superseded"; the N-5 row's "moot at the merge" dated (`:1810`). Plus R1's two rows (`:1822`, `:1823`) and the §9.8 count row (`:1825`).

### Beyond the two lists (flagged; each keeps one fact consistent across the file)
- **§14.1 R-2** (`:1553`): "**Merged** to `develop` 2026-10-24 as `dbd0a6c` … `ffa49aa` …", and step 4 "(Done …)" (`:1610`). The row carries **Blocks the branch cut** and checklist 1.1 reads it; it still read only "Signed off" although `dbd0a6c` is on `develop`.
- **Step 3a** "(Done …)" (`:1600`): R1 item 6 says "step 3a done"; I wrote it at the step as well as in the row.
- **Row TB** and **§16 #24**: one dated note each that their four preconditions are on `develop` (`4ff4ec6`, `f478ae9`, `b1a6cf9`, `6aa7624`) — the same fact R.4 item 8 records at step 5a.
- §8.1 and §9.8 quote the measured 33, not the sign-offs' 29 (R.4 item 3 asks for the merged tree's own figure).

## Checks
All in `/Users/ermanergoz/Desktop/PegSolitarie-r1c`, read-only except the two files above. No Gradle, no commit, as dispatched.
- `cat .claude/rules/VERSION` vs the plugin's `plugin.json` — 0.6.2 vs 1.0.0, so the plugin rule files were read in full — pass.
- `git log -1 --format='%h %ci %s'` over `b1a6cf9 dbd0a6c 13c04c6 9f0e67a 6aa7624 8f18049 ffa49aa f478ae9 4ff4ec6` — every sha cited exists with the date and subject the text gives — pass. `git merge-base bugfix/reduced-motion-live-gate develop` — `6aa7624` (row RM's cut) — pass.
- Code facts behind §7.2 and §12, read on the tree: `SolverOutcome.kt`, `HintOutcome.kt`, `GetHintUseCase.kt:100-140`, `GameViewModel.kt:1376-1420`, `GameScreen.kt:100-110, 268-292, 401-412`, `GameUiState.kt:160` (`showRescueBanner = stuck || noWinningMove`), `GameAnim.kt:50` (5_000 ms), `WrongTap.kt:92-95`, `MainActivity.kt:142`, `DataModule.kt:64`, `SettingsRepository.kt:25`, `ls audio/ res/raw audio/recipes` (12 WAVs, 2 recipes), `docs/audio/CREDITS.md:5-13` (7 / 3 / 2) — consistent with the text — pass.
- `git grep -l 'import androidx.compose.ui.test.junit4.createComposeRule' 6aa7624 -- app/src/test app/src/testDebug | wc -l` — 33; `git grep -l 'junit4.v2' 6aa7624 -- …` — `GameScreenHintNoticeTest.kt` only — pass.
- `grep -n firstWinningMove` over the tree — one hit, `domain/src/test/.../golden/solutions/AuthoringSolver.kt:250` — pass (item 5's note).
- `grep -n 'Correction — 2026-10-21' .planning/phases/08-…/08-VERIFICATION.md` — `:43`; `git log` on the file — `aaaba67` — pass (R1 item 9).
- **Table shape:** `python3 -I …/scratchpad/r1c/tablecheck.py` (counts unescaped pipes per row against each table's header) on `git show develop:docs/tech-design.md` — 20 tables, 0 mismatches; on the edited file — 20 tables, 0 mismatches — pass.
- **§16 numbering:** `awk` over §16's rows — 1 … 31, each once, #28–#31 new — pass.
- `git diff --check` — clean; `git status --short` — ` M docs/tech-design.md` (and this report) only — pass.
- **Greps proving each edit** (`grep -nF -- '<text>' docs/tech-design.md`, line numbers printed):
  - `Synced on 2026-10-24 on the docs-only` → 23 (tech-design-sync's), 34 (this sync's)
  - ``*on `develop` since `6aa7624`, 2026-10-24*`` → 15 (header haptics)
  - ``on `develop` since `6aa7624`,`` → 15, 221 (§2.4), 1270 (§12 assets)
  - `What "No winning move" means now` → 584; `What "No winning move" meant until R1 merged` → 636; `Superseded 2026-10-24 (§17): the box and the transcription` → 633
  - `**Confirmed at R1's close**` → 718; `AuthoringSolver.kt:250` → 723
  - `**The Compose test rule (tech-lead decision, 2026-10-24` → 813
  - ``| same | 33 on the merged tree `6aa7624` `` → 1104; `*One recorded exception (tech-lead decision, 2026-10-24` → 1148; `(2026-10-24: the "if" is history.` → 1163
  - `A miss reads Undecided, never "No winning move": only a proof raises the banner` → 1196
  - ``As built, on `develop` since `6aa7624`, they are twelve mono`` → 1270; `*Device checks, as proven (2026-10-24` → 1310; `Q-025 with option 1` → 1317; ``fires `HapticFeedbackType.Reject` only when `hapticsOn && SDK_INT >= R` `` → 1330
  - ``| Sounds | **For 3.0.0, on `develop` since `6aa7624`**`` → 1359
  - ``**Merged** to `develop` 2026-10-24 as `dbd0a6c` `` → 1553 (R-2)
  - ``| **Done.** Signed off PASS 2026-10-24 (`hint-undecided/tech-lead-signoff.md` `` → 1557 (R1)
  - ``| **Done.** Signed off PASS 2026-10-24 (`audio-review/tech-lead-signoff.md`)`` → 1559 (audio)
  - `| **RM** | **The live reduced-motion gate**` → 1560
  - ``(Done 2026-10-24: merge-in `df3dc2a` `` → 1600 (3a); `` `ffa49aa` `` → 1610 (4); `` `13c04c6` `` → 1611 (5); `(2026-10-24: the precondition is met` → 1621 (5a)
  - `**Record files in a merge-in**` → 1654 (§14.1), 1826 (§17)
  - `| **The residual Undecided rate off the book**` → 1714; `| **The reduced-motion gate is frozen at the first composition**` → 1715
  - ``**Closed 2026-10-24:** built on `feature/audio-review` (`8f18049` `` → 1744 (#17); ``**Closed 2026-10-24** on R1's merge (`b1a6cf9`)`` → 1748 (#21)
  - `| 28 | **` → 1755; `| 29 | **` → 1756; `| 30 | **` → 1757; `| 31 | **` → 1758
  - ``Note confirmed 2026-10-24 at R1's close: `.planning/phases/08`` → 1795; ``moot since 2026-10-24: audio merged as `6aa7624` `` → 1810
  - §17 new rows: `| §7.2's "What 'No winning move' means today"` → 1822; `| §10's Hint-latency row` → 1823; `| The branch-conditional audio text:` → 1824; `| §9.8's table row and rule 2 counting 29` → 1825; `| §14.1's second-to-merge paragraph, "resolves the conflicts"` → 1826
- `./scripts/check.sh` — **not run** (docs only; the dispatch forbids Gradle; this branch changes no Check input).

## Open issues / risks
- **Staffing (Q-019):** author and, likely, the reviewers share Opus. The mitigation is reviewers that re-derive each figure and sha from git and the code (the Checks above name the commands), not from this report.
- **Two of my calls are judgement, not transcription:** §15's re-rating of the R1 row (high off the book / low) and placing the record-file rule in §14.1 *and* §17 (R.4 item 9 vs the dispatch's "as a §17 row"). The reviewing tech-lead may rule either back.
- **The four flagged additions** go beyond the two lists; each restates a fact already true on `develop`. If the reviewer prefers them out, they revert cleanly (each is one dated parenthetical or prefix).
- **The check-gates merge-in re-read's own edits** to this file (`d656263`, its §17 merge-in rule and §8.1 counts) had no reader but their author; that re-read names this branch's code-auditor read as their first text read (`phase-14/tech-lead-merge-read-check-gates.md:124`). The producer should give the auditor that scope as well.
- **The §8.1 KDoc sentence** for `GameScreenHintNoticeTest` and the §9.8 count stay correct only until the next Compose suite lands; the figure is dated so a later reader knows its tree.

## Handoff
Next: producer — commit `docs/tech-design.md` and this report on `feature/tech-design-r1-close` (e.g. `docs(14): close R1 and audio in the tech design` and the report as its own commit); dispatch the code-auditor to read the diff as text (`git diff develop...feature/tech-design-r1-close -- docs/tech-design.md`), including `d656263`'s check-gates edits per that re-read's line 124; then a fresh tech-lead sign-off; QA not applicable. Merge before checklist 1.1 reads §14.1 at the cut: row RM is now a **Blocks the branch cut** row it must see. If another branch numbers §16 first, §17's hunk 3 applies to #28–#31.
Next: code-auditor — the text read above; the five new §17 rows and the four flagged additions are the places most worth checking against git.
Next: tech-lead (fresh) — the sign-off, ruling on the two judgement calls in Open issues.

## Loop 1 — 2026-10-24 (the code-auditor's FAIL, `.planning/reports/phase-14/code-auditor-tech-design-r1-close.md`)
Date: 2026-10-24
Branch: feature/tech-design-r1-close (committed by the producer; read at `f4fe205`, my loop-0 work at `cf68c27`; loop-1 edits uncommitted, as dispatched)
Ran on: claude-opus-5-5 at effort xhigh (policy); the same Q-019 staffing deviation as above (the agent file pins fable)
Loops: 1
Lessons applied: none

### Summary
- **F-1 (MAJOR) fixed.** Row RM, §15 and §16 #28 now give `6aa7624`'s counts: **14 call sites in 9 files** (R1's `HintUndecidedNotice.kt:170` the fourteenth) and **seven** test files that drive the gate through `Settings.Global` (R1's `GameScreenHintNoticeTest.kt:354` the seventh, all seven named in row RM). Both rows record that the audio sign-off's §3 (the RM brief) still carries the pre-R1 counts, measured on `b39e28e`, and that this file wins. The producer has already told the RM developer. I edited no report but my own.
- **R1's notice in RM (tech-lead decision, the auditor's Handoff question).** The notice is in scope by construction, because it reads the one function. No extra red-first test is required: 23e's `withAnimationsOffTheNoticeKeepsItsTimeAndGoesInOneFrame` pins it at each gate value, and the (a)/(b) live flips prove the function. QA's repro gains one step: after a live toggle, an off-book Hint's notice keeps its whole time and leaves in one frame.
- **F-2 to F-7 (LOW) fixed** as the auditor proposed, each with a dated "until loop 1" note.
- **I-1, I-3 and I-4 taken.** I-3 is the one judgement: §16 #30's fallback now asks the founder only for hardware set-up, never for a TalkBack pass the founder runs. If no set-up works by the cut, the halves become a Q-PUBLISH-3.0.0 line. This reconciles the hint sign-off's §1.1 with the audio sign-off's §4, and it is flagged for the signing tech-lead to confirm or rule back.

### Edits, by finding
- **F-1:** row RM `:1571` (count, the stale-brief note, the seven tests named, the notice ruling, the QA step); §16 #28 `:1766` (14 in 9 with the notice listed, seven tests, the stale-brief note); §15 `:1726` ("every phase-15 reduced-motion behaviour, R1's Undecided notice and the wrong tap's mark (14 call sites in 9 files on `6aa7624`)").
- **F-2** `:726`: the brief compared two boards (136 = 97 + 39, 378 = 345 + 33, against 101 and 785). 399 = 373 + 26 is this table's own figure, and all three match the book headers.
- **F-3** `:603`: names `toHintOutcome` and the ViewModel's `when (outcome)` as the two `else`-free `when`s, and says the book dispatch `when (fromBook)` keeps its `else`.
- **F-4** `:1106`: a caption note under the count says one row was re-measured at `6aa7624`, so the column sums to 243 and the 239 is `e1875cb`'s. Cell `:1115`: the Check ran on `9f0e67a` (`android-game-dev-merge.md:3`: `13c04c6..9f0e67a`).
- **F-5:** (a) #29's owner `:1767` gives the reproduction to the android-game-dev, as the ruling's §1.1 and Handoff 5 say; "qa-engineer to reproduce" is kept only as the dated note. (b) Row RM's riders: all three are non-gating (the brief's open issue 2), and only sound-designer F-1 drops to 3.0.1 if it costs a loop (§5).
- **F-6** `:1831` (`d656263`'s §17 merge-in row): the mapping to item 4's numbering. (1) is item 4's 1, (2) is 3 and 6–7, (3) is 4, (4) is 2, and (5) is 5. I checked this against `phase-14/tech-lead-signoff-tech-design-sync.md:69-75`.
- **F-7** `:814` (`d656263`'s §8.1 build-logic cell): "were part of the merge-in's Check … served from the build cache", quoting `android-game-dev-check-gates-merge.md:34` ("build-logic 55 (cache)").
- **I-1** `:1564`: R-2 reads "**Done.** Merged …".
- **I-3** `:1768`: as in the Summary.
- **I-4** `:34-45`: the header's sync sentence names the four facts and this loop.
- **Not taken:** I-2 (checklist 1.1 does not name RM) is the release-manager's file; the producer routes it. I-5 and I-6 need no change. I-7 is the auditor's note.

### Checks
- `git grep -n 'rememberAnimationsEnabled()' 6aa7624 -- app/src/main`: 15 lines, the definition `Clay.kt:209` plus 14 calls in 9 files (`HintUndecidedNotice.kt:170` among them) — pass, as F-1 says.
- `git grep -l ANIMATOR_DURATION_SCALE 6aa7624 -- app/src/test`: seven files, matching the brief's six named in §3(c) plus `GameScreenHintNoticeTest.kt`; `:354` is its `putFloat(… ANIMATOR_DURATION_SCALE, ANIMATIONS_OFF)` inside `withAnimationsOffTheNoticeKeepsItsTimeAndGoesInOneFrame` — pass.
- `sed -n 160-185p HintUndecidedNotice.kt`: the gate chooses `AnimatedVisibility` with a 160 ms `fadeOut` or a plain branch; the display time is a `delay`, independent of the gate — the basis of the notice ruling.
- `sed -n 68-76p phase-14/tech-lead-signoff-tech-design-sync.md`: item 4's hunks 1–7, as mapped (F-6) — pass. `sed -n 34p phase-14/android-game-dev-check-gates-merge.md`: "build-logic 55 (cache)" (F-7) — pass. `sed -n 3p audio-review/android-game-dev-merge.md`: `13c04c6..9f0e67a` (F-4) — pass.
- `python3 -I …/scratchpad/r1c/tablecheck.py docs/tech-design.md`: 20 tables, 0 mismatches — pass. `git diff --check`: clean. `git diff --stat`: `docs/tech-design.md` +29/−18 against `f4fe205`.
- `grep -nF` proofs (line numbers): `all 14 call sites in 9 files on \`6aa7624\`` → 1571, 1766; `The seven test files that drive the gate` → 1571; `**R1's notice (tech-lead decision, loop 1):**` → 1571; `which alone drops to 3.0.1 if it costs a loop` → 1571; `R1's Undecided notice and the wrong tap's mark (14 call sites` → 1726; `` R1's `GameScreenHintNoticeTest.kt:354` the seventh `` → 1766; `open bullet compared two boards` → 726; `` mapped one to one by `toHintOutcome` `` → 603; `the Lines column now sums to 243` → 1106; `` run on `9f0e67a` `` → 1115; `it reproduces QA's exact sequence on the API 27 emulator image` → 1767; `this row's (1) is item 4's 1` → 1831; `served from the build cache` → 814; `**If adb still cannot (tech-lead decision, r1-close loop 1` → 1768; `` | **Done.** Merged to `develop` 2026-10-24 as `dbd0a6c` `` → 1564; `Its review loop 1` → 43.
- `grep -c '13 call sites in 8 files'` → 1, the brief's figure quoted in row RM's stale-brief note; `qa-engineer to reproduce` → only inside #29's dated note — pass.
- No Gradle (docs only; no Check input changed).

### Open issues / risks
- **The RM brief** (`audio-review/tech-lead-signoff.md` §3) still says 13/8 and "the six". This file corrects it, and the producer has told the developer. If a tech-lead amends that report later, it should be a dated note, not an edit of the ruling.
- **The I-3 reconciliation** is my decision, and the signing tech-lead should rule it. So should loop 0's two judgement calls, which the auditor accepted and flagged for the signer's explicit word on "low".
- **I-2** stays with the release-manager: checklist 1.1 should name RM, as it names R1 and LN.

### Handoff
Next: producer — commit the loop-1 edit and this section; rename the auditor's report to `code-auditor-tech-design-r1-close-loop1.md` before re-dispatching, per the auditor's Handoff; a fresh code-auditor re-reads the fix hunks (`git diff f4fe205 -- docs/tech-design.md`); then a fresh tech-lead sign-off ruling F-1's notice decision, I-3, and the loop-0 calls; route I-2 to the release-manager.
