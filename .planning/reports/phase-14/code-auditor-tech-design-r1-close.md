# code-auditor report — tech-design-r1-close (text read of `docs/tech-design.md`, plus `d656263`)
Date: 2026-10-24
Branch: feature/tech-design-r1-close (`6aa7624..cf68c27`; one commit, `cf68c27`; this report is uncommitted, as dispatched)
Skills loaded: ergoz-soft:standards (role code-auditor; game; android-compose-game. The worktree snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin's studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing rule files in full, plus the snapshot's `.claude/rules/lessons.md`), code-reviewer, security-reviewer
Ran in parallel with: none named by the dispatch
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation, recorded:** the code-auditor's pin is fable at xhigh. The dispatch reports that the Fable limit is reached, so this read runs on Opus under Q-019 (`.planning/questions/FOUNDER.md`, answer 3: "Opus for every Fable-pinned role until 3.0.0 is published"). The author (tech-lead) also ran on Opus, so here the reviewer shares the author's model. The Q-019 mitigation I applied: I am a fresh agent, and I re-derived every count, sha and file:line below from git and the tree. I took none of them from the author's report.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; I did not read `.planning/log/`)

## Summary
- **FAIL, on one MAJOR finding (F-1).** Row RM and §16 #28 give the reduced-motion defect's scope as "13 call sites in 8 files" and "the six tests". Those counts were measured on audio's pre-merge head. On `6aa7624`, the tree RM is cut from, there are **14 sites in 9 files** and **seven** such test files. R1's Undecided notice is the one both counts miss.
- All 22 owed edits landed as ruled: R1 §4 items 1–9 and audio R.4 items 1–13. QA's 6/6 and 17/17, the book counts (136/378/399), the 33 suites and every sha the text cites are real. §16 runs from 1 to 31 with no gaps or repeats. All 20 tables keep their column counts. Row RM's marker matches checklist 1.1's "last cell" rule.
- The two judgement calls and the four additions hold. `d656263`'s facts are confirmed, apart from two precision points (F-6, F-7).
- I also raise six LOW findings and seven INFO notes. None of the LOW findings blocks the merge alone.

## Severity scale
MAJOR: a wrong fact in text that a gate or a branch reads. It must be fixed before the merge. LOW: imprecise or unruled wording. Fix it in the same pass, or let the signing tech-lead rule it. INFO: no change to this diff is needed.

## Findings

**F-1 — MAJOR — `docs/tech-design.md:1560` (row RM), `:1755` (§16 #28), `:1715` (§15).** RM's scope figures are stale for the tree it is cut from.
- What the text says: "all 13 call sites in 8 files" (`:1560`, `:1755`, with the eight files listed at `:1755`). "the six tests that drive the gate through `Settings.Global` stay green unedited" (`:1560`). "every phase-15 reduced-motion behaviour" (`:1715`).
- What the tree shows: `git grep -n 'rememberAnimationsEnabled()' 6aa7624 -- app/src/main` gives 14 calls in 9 files. The extra one is `ui/game/HintUndecidedNotice.kt:170`, R1's notice, present since `b1a6cf9`. `git grep -l ANIMATOR_DURATION_SCALE 6aa7624 -- app/src/test` gives 7 files. The extra one is `GameScreenHintNoticeTest.kt:354` (`Settings.Global.putFloat(… ANIMATOR_DURATION_SCALE, ANIMATIONS_OFF …)`).
- Where the old counts come from: audio's head `b39e28e` really does have 13 calls and 6 such test files. The ruling (`audio-review/tech-lead-signoff.md` §3) measured there, before R1 merged. The text transcribes those counts unrefreshed into a row that says it is cut from `6aa7624`. Meanwhile this same branch takes the merged tree's own figure for the Compose-rule count (`:1104`, hunk rule (4)'s principle).
- Why it matters: the RM developer must "run them unedited and report the count", and the RM code-auditor and QA scope their work from this row. A seventh test that got edited, or a notice that regressed under a live toggle, would fall outside the stated scope.
- Fix: give the `6aa7624` figures, name `HintUndecidedNotice.kt` and `GameScreenHintNoticeTest.kt`, and make `:1715` read "every phase-15 reduced-motion behaviour, R1's notice and the wrong tap's mark". Whether RM's red-first tests should also flip the scale over the notice is the tech-lead's call (see Handoff).

**F-2 — LOW — `docs/tech-design.md:718-720`.** The text says "The three header figures the hint brief's open bullet compared read 136 = …, 378 = … and 399 = …". The brief's bullet (`hint-undecided/tech-lead-brief.md:567-570`) compares only Wiegleb-45 and Diamond-41 (136 against 97, 378 against 345, and 140/818 against 101/785). The 399 for English-33 comes from §7.2's own table, which is where the sign-off put it (§4 item 5: "the three header figures **there**"). The figures themselves are right: `english33-k4.txt:1` reads `entries=399 proven-dead=4`, `diamond41-k4.txt:1` `entries=378`, `wiegleb45-k3.txt:1` `entries=136`, and 378 = 345 + 33, 136 = 97 + 39, 399 = 373 + 26. Fix: attribute the comparison to the brief for the two gapped boards, and say English-33's figure is §7.2's own.

**F-3 — LOW — `docs/tech-design.md:599`.** The sentence "Both mappings are exhaustive `when`s with no `else`" sits in the `GetHintUseCase` bullet. Read in that bullet, the two mappings are the book→outcome mapping and the solver→outcome mapping. But the book dispatch `when (fromBook)` has an `else` (`GetHintUseCase.kt:121-127`). The two `else`-free mappings the sign-off meant are `toHintOutcome` (`GetHintUseCase.kt:133-138`) and the ViewModel's `when (outcome)` (`GameViewModel.kt:1403-1413`); I read both, and neither has an `else`. Fix: name the two mappings.

**F-4 — LOW — `docs/tech-design.md:1104` against `:1094-1096`.** The table is a single recount at `e1875cb`: "239 `w:` lines", which is 6 + 4 + 198 + 29 + 1 + 1. This row's figure now reads "33 on the merged tree `6aa7624`", and with it the Lines column sums to 243. R.4 item 3 asked for exactly this, and the old 29 is kept with its date. The table is now a mixed-tree table under a single-tree caption. Fix: add a caption note ("one row since re-measured at `6aa7624`"), or keep 29 in the table and give 33 in rule 2 and §8.1, where it already appears. The same cell says "audio's merge-in Check at `13c04c6`". That Check ran on `9f0e67a` (`audio-review/android-game-dev-merge.md:3`; re-read R.1), a commit that changed only a KDoc. The count is unaffected, but the sha is not the tree the Check ran on.

**F-5 — LOW — `docs/tech-design.md:1756` (§16 #29, Owner cell) and `:1560` (row RM, riders).** Two transcriptions widen their rulings.
- (a) #29's owner cell adds "qa-engineer to reproduce". The ruling gives the reproduction to the android-game-dev: "owner android-game-dev, via the producer, as its own task after this merge: reproduce QA's exact sequence … on an Android 8.1 (API 27) emulator image" (`hint-undecided/tech-lead-signoff.md` §1.1; Handoff 5 says the same).
- (b) Row RM says "Riders, non-gating (a rider that costs a loop drops to 3.0.1)". §5 of the audio sign-off gives that drop clause to sound-designer F-1 only ("A rider, non-gating: if it costs a loop, it drops to 3.0.1"). Auditor finding 1 and the F-2 KDoc are assigned to "the bugfix branch" with no drop clause.
- Fix: transcribe each as ruled, or have the signing tech-lead rule the wider text.

**F-6 — LOW — `docs/tech-design.md:1820` (§17, the merge-in rule row, from `d656263`).** The row numbers its hunk rules (1)–(5): header, different rows, numbering, figure, §17. The ruling it transcribes numbers them differently: `phase-14/tech-lead-signoff-tech-design-sync.md` item 4, hunks 1–7, where 2 is the figure, 3 the merge order, 4 the numbering and 6–7 the checklist. The row gives no mapping, and citations by number are already ambiguous. Audio's merge-in cites "tech-design §17's merge-in rule (hunks 2 and 3)" (`audio-review/android-game-dev-merge.md:14`; `13c04c6`'s message). The check-gates re-read cites "Rule 1–7" by item 4's numbers (`tech-lead-merge-read-check-gates.md:23-31`). Fix: add a mapping parenthetical, for example "(item 4's 1; 3 and 6–7; 4; 2; 5)".

**F-7 — LOW — `docs/tech-design.md:805` (§8.1, build-logic Gate cell, from `d656263`).** The cell says "the 55 ran in the merge-in's Check on `ffa49aa`". The developer's own record says "build-logic 55 **(cache)**" (`phase-14/android-game-dev-check-gates-merge.md:34`): the results were restored from the build cache, not executed in that run. The point the cell makes, that the root `check` reaches build-logic, stands (`build.gradle.kts:18-20`; `7b28007`, 2026-08-27, whose comment names PLAT-06). Fix: "and the 55 were part of the merge-in's Check on `ffa49aa`, served from the build cache".

**INFO**
- I-1, `:1553`: row R-2 is marked "**Merged**". R1 and audio use "**Done.**", and the table's reading note (`:1544-1546`) uses "**Done**". The outcome under checklist 1.1 is the same, because the branch is merged (`git merge-base --is-ancestor dbd0a6c develop`). For one vocabulary, mark it **Done.**
- I-2, `docs/release-checklist.md:31`: item 1.1's illustrative list ("R1 … LN, besides R-2 and F-1") does not name RM. 1.1's rule, "every row … whose last cell says **Blocks the branch cut**", does cover it, and RM's last cell carries the marker (`:1560`; an awk pass over the §14.1 rows lists R-2, F-1, R1, TB, RM and LN). This is the release-manager's file, not this diff. Routing: name RM there as R1 and LN are named.
- I-3, `:1757` (§16 #30): the fallback "a founder-run TalkBack pass before release" is transcribed faithfully from `hint-undecided/tech-lead-signoff.md` §1.1. It sits uneasily with the audio sign-off's "Standing rule: the founder never tests by hand" (§4). That tension is between two rulings, not in this text. It is for the signing tech-lead to note.
- I-4, `:34-41`: the header's sync sentence does not mention the four flagged additions (the R-2 row, the step 3a and step 4 notes, row TB, and the #24 note).
- I-5, `cf68c27`: the producer committed the author's report together with the work. rules/reporting.md asks for the report "in its own commit", and the author's Handoff asked the same. This is a process note for the producer. The commit carries the one studio trailer.
- I-6, `:1559`: the N-5 parenthetical is kept with a date rather than dropped as R.4 item 7 said. That is consistent with the file's never-delete rule (`:41-43`), so I accept it.
- I-7: I ran no Gradle (dispatch). `git diff --stat develop HEAD -- app domain build-logic` is empty, so the branch changes no Check input.

## What was verified (requirements, item by item)

**R1, `hint-undecided/tech-lead-signoff.md` §4 (nine items): all landed as ruled.**
1. §7.2 rewrite (`:584-631`). Checked against the code:
   - `SolverOutcome.kt` has `Found(move)`, `ProvenDead` (three provenances) and `Undecided`. `grep Cancelled` over `domain/src/main` finds nothing.
   - `HintOutcome` is the one outcome type `:app` imports.
   - The book's `NoWinningMove` → `ProvenDead` (`GetHintUseCase.kt:123`).
   - `showRescueBanner = stuck || noWinningMove` (`GameUiState.kt:160`).
   - The banner, else `UndecidedNoticeSlot` (`GameScreen.kt:266-281`).
   - `HINT_NOTICE_DISPLAY_MS = 5_000` (`GameAnim.kt:50`).
   - The string (`strings.xml:123`). `liveRegion = Polite` and `dismiss` (`HintUndecidedNotice.kt:98-99`).
   - `shouldCancel = { !isActive || deadline.hasPassedNow() }` (`GameViewModel.kt:1386`). The board guard sits ahead of the `when` (`:1393`).
   - GD-HT-3, AC-8 and AC-11 match the ruling (`game-designer-hint-timeout.md:216-232, 319`).

   The box and the transcription stay, retitled, under "Superseded 2026-10-24" (`:633-638`).
2. §10 Hint-latency row (`:1196`). The golden-line exception stays.
3. §15 (`:1714`), with QA's figures. `hint-undecided/qa-engineer.md:48` reads: "walks A–C **6/6** … Emulator: 17/17 off-book requests Undecided".
4. §16 #21 closed (`:1748`). The cited reports exist: `code-auditor.md` PASS, `qa-engineer.md` PASS WITH BLOCKED ITEMS.
5. The book-count note (`:718-725`); see F-2. `AuthoringSolver.kt:250` is the only `firstWinningMove` hit (`git grep`). Brief A2.3 and its Amendment 2 line (`tech-lead-brief.md:580`) defer it to 3.1 R2/R2b.
6. Row R1 is Done (`:1557`). The shas check out: `df3dc2a` and `67c285b` lead to `b1a6cf9` (first code merge after `c5f2c54`), `ffa49aa` to `dbd0a6c`, and `13c04c6` plus `9f0e67a` to `6aa7624`. The §10.3 named tests are recorded green in `audio-review/android-game-dev-merge.md:31`.
7. §8.1 convention (`:813-819`). `git grep -l 'import androidx.compose.ui.test.junit4.createComposeRule' 6aa7624 -- app/src/test app/src/testDebug` gives 33 files, and `junit4.v2` appears only in `GameScreenHintNoticeTest.kt`. §14.2 schedules the migration (`:1679`).
8. #29, #30 and #31 (`:1756-1758`), as ruled, except F-5 (a).
9. §17 D-04 row (`:1795`). `08-VERIFICATION.md:43` is "## Correction — 2026-10-21: …", and `git log -L43,43` points to `aaaba67`.

**Audio, R.4 (thirteen items): all landed as ruled.**
- Item 1, the header (`:15`; the sync sentence at `:34-41`, placed after `develop`'s, as hunk rule (1) asks). `tech-lead-haptics.md` exists at `6aa7624`.
- Items 2 to 4: §2.4 (`:221-223`); §9.8 rule 2's exception (`:1148-1152`) and the count (`:1104`, see F-4); rule 3's history note (`:1163-1165`).
- Item 5, §12. Every claim matches the tree:
  - the twelve `res/raw/*.wav` and the two `audio/recipes/*.json`;
  - `docs/audio/CREDITS.md:7-13` (7 / 3 / 2);
  - `audio/` holds the five named files, with `SystemAudioSnapshot` at `SystemAudioState.kt:24`;
  - `volumeControlStream` at `MainActivity.kt:142`;
  - `interface SettingsRepository : HapticsSetting` (`:25`), bound at `DataModule.kt:64`, with `TutorialViewModel.kt:83` as the third collaborator;
  - `JumpHapticEffect` called from `GameScreen.kt:182` and `TutorialScreen.kt:116`;
  - `WrongTapHaptic` private, through `NewOrdinalEffect`, gated by `wrongTapHapticFires` = `hapticsOn && sdkInt >= R` (`GameScreen.kt:401-411`, `WrongTap.kt:92-95`);
  - `TutorialScreen.kt` has no `wrongTapCorrection`, `InvalidTapSound` or wrong-tap announcement;
  - `af2b554`, `d1c6e9b` (2026-09-29) and `8f18049` (2026-10-21, "Haptics seam") all exist;
  - Q-025 is open (`FOUNDER.md:3`), so "if the founder answers" is right.
- Item 6, §13 Sounds (`:1359`).
- Items 7 and 8, the audio row (`:1559`) and steps 5 and 5a (`:1611-1612`, `:1621-1623`). Its figures match `android-game-dev-merge.md:16, 27, 29, 30`: 1,565 tests; fuzz 10,000/0; kover 99.02 %; 0 `w:` from main code. `code-auditor-merge.md` PASS and the three integration reports exist with the verdicts cited.
- Item 9, the record-file rule (`:1654-1663`), placed directly below the second-to-merge paragraph. Its (a)–(c) match R.2 verbatim.
- Items 10 and 11, row RM (`:1560`) and §15 (`:1715`). The mechanism, the rejected alternatives, the tests, the chain, no 15e sitting and the 5a slot all match §3, §5 and R.4 item 10, except F-1 and F-5 (b). `git merge-base develop bugfix/reduced-motion-live-gate` gives `6aa7624`, the worktree is `PegSolitarie-rm`, and `Clay.kt:209-218` is the cited helper.
- Item 12, #17 closed (`:1744`) and #28 (`:1755`; see F-1).
- Item 13, §17 (`:1810`, `:1822-1826`). Five new rows and two amended ones, as the author says.

**The two judgement calls: both accepted.**
- (1) §15's re-rating to "high off the book / low" agrees with the game-designer's reading of the residual: "True, and acceptable … only the Undecided rate varies" (`game-designer-hint-timeout.md:344`). The 100 % measured rate makes "low" a judgement worth the signing tech-lead's explicit word.
- (2) The record-file rule lives in §14.1, and §17 records what it supersedes. That follows the file's own convention, and the two texts agree.

**The four additions: accepted, facts real.**
- R-2 is "Merged … `dbd0a6c` … `ffa49aa`" (`:1553`; vocabulary in I-1).
- Steps 3a and 4 are done (`:1600`, `:1610`).
- Row TB (`:1558`) and #24 (`:1751`) cite `4ff4ec6`, `f478ae9`, `b1a6cf9` and `6aa7624`, all ancestors of `develop`.
- 33 replaces 29: the right figure for `6aa7624`, where `dbd0a6c` and `e1875cb` both give 29.

**`d656263` (check-gates' merge-in re-read edits): facts confirmed, except F-6 and F-7.**
- `domain/src/test` holds 41 `.kt` files, 33 of them `*Test.kt`, at both `c632b45` and `ffa49aa`.
- The 294 tests are in `android-game-dev-check-gates-merge.md:34` and `:42`.
- `build.gradle.kts:18-20` has the `includedBuild("build-logic").task(":check")` dependency at HEAD and at `ffa49aa`.
- `7b28007` is dated 2026-08-27 and implements plan 06-07, whose requirement is PLAT-06.
- `ci.yml:48-49` carries the stale "not part of the root `check`" comment, and `:58` carries the "Check command in CLAUDE.md" comment.
- At `ffa49aa`, `git grep` finds no other tracked file citing §16 #23–#27.
- The §17 rule's clauses match item 4 (b)–(d), and the two clauses that are the re-read's own decisions are labelled as such.

**Security pass.** Docs-only diff. A secret and ID scan of the added lines (ad-unit, API-key, PEM, password/token and PGS-ID shapes) found 0 hits. The diff contains no URLs to new services and no credentials.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-r1c/.planning/reports/phase-14/code-auditor-tech-design-r1-close.md`: this report, VERDICT FAIL. I edited no other file and committed nothing. The two worktrees under my scratchpad (`mut/wt`, `probe`) predate this session; I did not touch them.

## Checks
All run in `/Users/ermanergoz/Desktop/PegSolitarie-r1c` and read-only. No Gradle, as dispatched.
- `cat .claude/rules/VERSION` vs the plugin's `plugin.json`: 0.6.2 vs 1.0.0, so I read the plugin rule files — pass.
- `git status`; `git rev-parse develop`; `git merge-base develop HEAD`: clean; `6aa7624`; `6aa7624` — pass. `git log develop..HEAD`: one commit, `cf68c27` — pass.
- `git diff develop...HEAD -- docs/tech-design.md`: 419 lines, read in full (+188/−48). `git show d656263 -- docs/tech-design.md`: read in full (+14/−6).
- `python3 -I <scratchpad>/scripts/tables.py` (unescaped pipes per row against the header; separator rows; pipes inside code spans) over `develop`, HEAD and `d656263^`: 20 tables and 0 mismatches each time — pass.
- `awk` over §16 rows: 1…31, no duplicate (`uniq -d` prints nothing) — pass. `git diff develop...<b> -- docs/tech-design.md` for talkback-board, licences-screen, reduced-motion-live-gate, candy-backdrop, colour-vision and home-chip-arrival: no branch adds a §16 number — pass. `git grep -nE '§16 #(2[89]|3[01])' HEAD` outside the design and reports: none — pass.
- `git log -1` over 20 cited shas (`b1a6cf9 6aa7624 df3dc2a 67c285b ffa49aa dbd0a6c 13c04c6 9f0e67a 8f18049 af2b554 d1c6e9b aaaba67 f478ae9 4ff4ec6 0582e3f e1875cb c5f2c54 7b28007 c632b45 74eec52`): each exists with the cited date and subject, and each is an ancestor of `develop` — pass.
- `git grep -n 'rememberAnimationsEnabled()' {6aa7624,b39e28e,b1a6cf9} -- app/src/main`: 14 at `6aa7624`, 13 at `b39e28e`, and 14 at `b1a6cf9`, where the one more is `HintUndecidedNotice.kt:170` — **fail against `:1560`/`:1755` (F-1)**.
- `git grep -l ANIMATOR_DURATION_SCALE {6aa7624,b39e28e} -- app/src/test`: 7 and 6 — **fail against `:1560` (F-1)**.
- `git grep -l 'import androidx.compose.ui.test.junit4.createComposeRule' <rev> -- app/src/test app/src/testDebug`: 33 at `6aa7624` and `13c04c6`, 29 at `dbd0a6c` and `e1875cb`; `junit4.v2` in one file — pass.
- `head -3` of the five book files: `entries=` 493/378/399/639/136 and `proven-dead=4` on English-33 only — pass (F-2 is wording only).
- `grep` of QA's figures in `hint-undecided/qa-engineer.md:48`: 6/6 and 17/17 — pass.
- `grep` of the merge report figures (`audio-review/android-game-dev-merge.md:3,16,27,29,30,31`; `phase-14/android-game-dev-check-gates-merge.md:17,34,42`) — pass, except "(cache)" (F-7).
- `grep -n` over `docs/release-checklist.md` 1.1, and `awk` over the §14.1 rows for the marker in the last cell: R-2, F-1, R1, TB, RM, LN — pass.
- `git diff --check develop...HEAD`: clean. Conflict markers in the design: none — pass.
- The secret and ID scan of the added lines: 0 hits — pass.
- `./scripts/check.sh` / `./gradlew check`: **not run** (dispatch: no Gradle). `git diff --stat develop HEAD -- app domain build-logic` is empty, so no Check input changed.

## Open issues / risks
- **F-1's source.** The RM brief (`audio-review/tech-lead-signoff.md` §3) carries the same pre-R1 counts. The RM developer reads that brief, so correcting only the design leaves the brief's "run the six unedited and report the count" short by one file.
- **Q-019.** An Opus read of Opus text. Everything above was re-derived from git and the tree. The fresh tech-lead sign-off should re-derive F-1 itself, not take it from here.
- I-2 (checklist 1.1 not naming RM) and I-3 (the founder-run fallback against "the founder never tests") are routing items. This branch cannot fix either.

## Handoff
Next: producer — route the FAIL to the tech-lead (the author's role; a docs-only branch with no developer).
- F-1 is required: `:1560`, `:1755` and `:1715` should give the `6aa7624` figures (14 sites in 9 files; seven test files), with `HintUndecidedNotice.kt:170` and `GameScreenHintNoticeTest.kt:354` named. The tech-lead rules whether RM's red-first tests and QA's repro also cover the notice, and amends the RM brief's counts.
- F-2 to F-7: fix in the same pass, or rule back in the sign-off.
- Then a re-read of the fix hunks by a fresh code-auditor (the same report path, renamed to `-loop1` first), and the fresh tech-lead sign-off, which rules the two judgement calls and I-3.
- Separately, the release-manager for I-2.

VERDICT: FAIL

## Loop 1 — 2026-10-24 (re-read of the fixes, `f4fe205..2d5289e`)
Date: 2026-10-24
Branch: feature/tech-design-r1-close (`f4fe205..2d5289e`; read `git diff f4fe205 HEAD -- docs/tech-design.md`, +29/−18, and the author's "Loop 1" section in `tech-lead-tech-design-r1-close.md`; this section is uncommitted, as dispatched)
Ran on: claude-opus-5-5 at effort xhigh. The same Q-019 staffing deviation as above: the pin is fable, the Fable limit is reported reached, and the author is also on Opus. As before, I re-derived every count from git and the tree, not from the author's section.
Loops: 1
Lessons applied: none

### Summary
- **F-1 is fixed.** Row RM (`:1571`), §15 (`:1726`) and §16 #28 (`:1766`) now give `6aa7624`'s figures: 14 call sites in 9 files and seven test files. They name `HintUndecidedNotice.kt:170` and `GameScreenHintNoticeTest.kt:354`. They also record that the RM brief (`audio-review/tech-lead-signoff.md` §3) still carries the pre-R1 counts, and that this file wins, as its "How to read it" (`:49-52`) says.
- **F-2 to F-7 are fixed** as proposed, each with a dated "until loop 1" note.
- **I-1, I-3 and I-4 are accepted.** I-3 leaves one LOW wording point for the signing tech-lead (L1-1). It does not block the merge.
- **VERDICT: PASS.**

### Verification, finding by finding
- **F-1.** The tree confirms all three figures:
  - `git grep -n 'rememberAnimationsEnabled()' 6aa7624 -- app/src/main` gives 14 calls in 9 files, `HintUndecidedNotice.kt:170` among them.
  - `git grep -l ANIMATOR_DURATION_SCALE 6aa7624 -- app/src/test` gives 7 files: the brief's six, named in row RM, plus `GameScreenHintNoticeTest.kt`. `:354` there is its `putFloat(… ANIMATIONS_OFF)`.
  - #28's file list now has nine entries.

  The author's new decision on the notice: it is in scope by construction, needs no extra red-first test, and adds one QA repro step. It holds. Criterion 23e (`docs/ux-spec.md:6257`) is pinned at both gate values by `theNoticeLeavesAfterItsDisplayTimeAndItsExit` (5 s plus the 160 ms exit) and `withAnimationsOffTheNoticeKeepsItsTimeAndGoesInOneFrame` (5 s, one frame) in `GameScreenHintNoticeTest.kt:311-361`. RM's (a)/(b) flips test the one function every caller reads. I accept it as a tech-lead decision.
- **F-2** (`:725-731`): the bullet is now attributed to the brief's two gapped boards, with 399 as §7.2's own. Accepted.
- **F-3** (`:603-607`): names `toHintOutcome` and the ViewModel's `when (outcome)`, and says `when (fromBook)` keeps its `else`. That matches `GetHintUseCase.kt:121-138` and `GameViewModel.kt:1403-1413`. Accepted.
- **F-4** (`:1105-1107`, `:1115`): the caption note's 243 is right (6 + 4 + 198 + 33 + 1 + 1). The Check sha is now `9f0e67a`, per `android-game-dev-merge.md:3, 30`. Accepted.
- **F-5:**
  - (a) #29's owner cell (`:1767`) gives the reproduction to the android-game-dev, as `hint-undecided/tech-lead-signoff.md` §1.1 and Handoff 5 say. "qa-engineer to reproduce" survives only in the dated note.
  - (b) Row RM's riders: only sound-designer F-1 drops to 3.0.1 (§5), and all three are non-gating (open issue 2).

  Accepted.
- **F-6** (`:1831`): the mapping (1)=1, (2)=3 and 6–7, (3)=4, (4)=2, (5)=5 matches `tech-lead-signoff-tech-design-sync.md:69-74`. Accepted.
- **F-7** (`:814`): "were part of the merge-in's Check … served from the build cache", quoting `android-game-dev-check-gates-merge.md:34`. Accepted.

### The author's additions
- **I-1, accepted.** Row R-2 (`:1564`) now reads "**Done.** Merged …". That is the vocabulary of the table's reading note (`:1555-1557`), of R1 and of audio. Its last cell still carries the marker; checklist 1.1 is satisfied by the merge either way.
- **I-4, accepted.** The header (`:41-45`) names the four facts and loop 1. The cited report path exists (committed in `f4fe205`), and "six precision points (F-2 to F-7)" counts right.
- **I-3, accepted, with L1-1.** §16 #30 (`:1768`) now asks the founder only for hardware set-up, never for a pass the founder runs. That agrees with the audio sign-off's §4 standing rule. It also agrees with the producer's own wording in Q-024 option 1, "the founder never tests by hand" (`FOUNDER.md:21-22`), and with how Q-024 and Q-025 ask for hardware. It overrides the hint sign-off's §1.1 fallback. The override is recorded as a tech-lead decision, keeps the old text dated, and is flagged for the signer. That is within the tech-lead's authority under "How to read it".

### New in loop 1
**L1-1 — LOW — `docs/tech-design.md:1768` (§16 #30).**
- The status cell still opens "**Owed before `release/3.0.0`**".
- The new fallback adds an end state the R1 ruling did not have: "If no set-up makes TalkBack drivable by the cut, the two halves become one line in the Q-PUBLISH-3.0.0 summary". The old founder-run fallback still closed the item before release.
- Disclosure at Gate 2 has precedent: audio's open halves; Q-024 option 2's "recorded as unverified … never silently passed".
- But the cell now says both "owed before the cut" and "may ship disclosed".
- Fix, or rule in the sign-off: "Owed before `release/3.0.0`, or, if no set-up makes TalkBack drivable, disclosed in the Q-PUBLISH-3.0.0 summary". The signer rules it together with I-3. It does not block the merge.

**INFO.**
- L1-I1, `:1571`: row RM says "the producer has told the RM developer". No file shows it: the RM worktree (`012b1d6`) and `.planning/` mention the corrected count only in the two r1-close reports. It is a process fact, not a design fact, and the row's own figures are what the developer reads (this file wins), so nothing depends on it.
- L1-I2: I-2 (checklist 1.1 does not name RM) is still the release-manager's, as the author says.

### Loop 1 checks
All run in `/Users/ermanergoz/Desktop/PegSolitarie-r1c`, read-only; no Gradle.
- `git log --oneline develop..HEAD`: `cf68c27`, `f4fe205` (my report), `2d5289e` (the fixes). `git diff --stat f4fe205 HEAD`: the design, +29/−18, and the author's report — read in full.
- `git diff --stat f4fe205 HEAD -- app domain build-logic`: empty, so no Check input changed. `git diff --check f4fe205 HEAD`: clean — pass.
- `python3 -I <scratchpad>/scripts/tables.py docs/tech-design.md`: 20 tables, 0 mismatches — pass. `awk` over §16: 1…31, each once — pass.
- `git grep` of the 14/9 calls and the 7 test files at `6aa7624` — pass (F-1).
- `sed -n 6257,6262p docs/ux-spec.md` and `sed -n 311,361p GameScreenHintNoticeTest.kt`: 23e and its two gate-value tests — pass.
- `sed -n 49,52p docs/tech-design.md` ("this file wins"); `sed -n 16,26p .planning/questions/FOUNDER.md` (Q-024, "the founder never tests by hand") — pass.
- `grep -rln '14 call sites\|HintUndecidedNotice.kt:170' .planning <rm worktree>/.planning`: only the two r1-close reports (L1-I1).

### Handoff
Next: producer — commit this section. Then a fresh tech-lead sign-off, which rules:
- the loop-0 judgement calls ("low" in §15; the record-file rule in both §14.1 and §17);
- loop 1's notice decision;
- I-3 together with L1-1.

Route I-2 to the release-manager.

VERDICT: PASS
