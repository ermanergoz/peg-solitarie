# tech-lead report — tech-design-rm-close (the RM close in `docs/tech-design.md`)
Date: 2026-10-24
Branch: feature/tech-design-rm-close (300198e..300198e; cut from `develop` at `053e9cc`, the RM merge; worktree `/Users/ermanergoz/Desktop/PegSolitarie-rmc`)
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game). The worktree's snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin's rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing and dispatch. Also the snapshot's `.claude/rules/lessons.md`, android-architecture and android-gradle-logic
Ran in parallel with: home-chip-arrival (tech-lead ruling), talkback-board (QA), licences-screen (code-auditor), colour-vision (code-auditor)
Ran on: claude-opus-5-5 at effort xhigh (policy). **Staffing deviation, recorded:** the agent file pins fable at xhigh. The Fable limit is reached, and the founder's answer to Q-019 (option 3, re-confirmed 2026-10-24) puts every Fable-pinned role on Opus until 3.0.0 is published. So this text is Opus-written, and its reviewers (the code-auditor's text read, then a fresh tech-lead's sign-off) should re-derive each figure and sha from git, not from this report. The Checks name the commands.
Fan-out: none
Loops: 0
Read in full: `.planning/reports/reduced-motion-live-gate/tech-lead-signoff.md`, `tech-lead-ruling.md` (with the Finding 1 section and the F1 amendment), `ux-designer.md`, `qa-engineer.md`, `code-auditor.md`, `code-auditor-finding1.md`; `docs/tech-design.md` on `develop` at `053e9cc` (1,838 lines, read in consecutive parts to its last line). Read in part: `docs/ux-spec.md` (the twelfth revision entry `:486-499`; §10 `:4801-4869`, with "A switch made while the game is open" at `:4839`; 23d and 23e `:6304-6350`); `reduced-motion-live-gate/producer.md`; `phase-14/tech-lead-tech-design-r1-close.md`, which set the pattern
Lessons applied: none (the snapshot holds 0 active lessons for game / android-compose-game; `.planning/recall/RECALL.md` does not exist)

## Summary
- All nine items of the sign-off's §4(c) are in `docs/tech-design.md`, including §4(b)'s new row, numbered **#32** because `develop`'s highest was #31. One commit: `300198e`, +50 / −11 lines. Every removed line is a rewritten line, and each superseded sentence stays in place, dated.
- **§9.8's figure is 39**, counted from the compiler's own lines on the merged tree with the one-module compile. It comes from 38 files: 37 suites and the `WrongTapBoards.kt` helper, which calls the rule twice. **The recorded 33 for `6aa7624` was a file count.** That tree had 34 calls, so the RM delta is +5, as the sign-off says. Both facts are written into §9.8 and §8.1, and the correction gets a §17 row of its own (flagged below).
- Four small additions go beyond the nine items, each keeping one fact consistent. They are flagged under "Beyond the nine items" for the reviewer to judge, and each reverts cleanly.
- The 3.0.1 notice brief is not written here, as dispatched. #32 only points to the sign-off's §4(a) and says when the brief is due.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-rmc/docs/tech-design.md`: the edits below, committed as `300198e` (`docs(14): close RM in the tech design`).
- `/Users/ermanergoz/Desktop/PegSolitarie-rmc/.planning/reports/tech-design-rm-close/tech-lead.md`: this report, committed alone after it.

### The nine items of §4(c), each with its section and line (on `300198e`)
1. **§14.1 row RM: Done** (`:1593`, the last cell). It opens with "**Done.**", in R1's and R-2's pattern, and keeps the old cell after "*The record:*", with its **Blocks the branch cut** marker. The cell carries:
   - the merge commit `053e9cc` (`--no-ff`, after the branch took `develop` in at `0a059da`) and the code range `7382351..45f84ec`;
   - no Check re-run at the merge, under the sign-off's §3: `git diff --quiet 45f84ec 053e9cc` outside `.planning/`, this file and `docs/ux-spec.md` exits 0, so the merged tree's code is the tree of the last two green Checks (1,576 / 0);
   - the sign-off and the reports by path: the auditor's two PASSes, QA's PASS WITH BLOCKED ITEMS and the ux-designer's rulings;
   - **ruling §9 item 1's replacement**, written into the row's Item cell (`:1593`). The ruling's three sentences replace "stay green unedited, and the developer reports the count", and the old sentence is kept as "*(Until the RM close this read: …)*", with the 14-of-63 probe and the auditor's two 7c-race mutants in the Done cell;
   - **Finding 1's rider**, gating: the house idiom at `45f84ec`, `TutorialCompletionStarLiveMotionTest`'s FA.2 assertion (165 → 639 px) and the auditor's red mutant;
   - the non-gating riders, by commit (`335bc3b`, `35cfd35`, `47e7477`, `873fb9e`);
   - **F1's acceptance**, pointing to ux §10 ("A switch made while the game is open") and its 15 s pass line, with the platform cause (`ContentResolver.java:715-726`, checked in the android-35 sources);
   - **QA's §8 start-path PASS**: 6 starts, 2 rotations, 2 restores, 0 `MotionDurationScale` lines, 0 crashes;
   - what is left open: §6 as #32, the two BLOCKED items as classified in the sign-off's §2, the 23e step closed by other evidence, and the three release/3.0.0 QA steps;
   - the next branch's second-to-merge duties (the sign-off's §3), from "Its slot against 5a" in the record.
2. **§16 #28: closed** (`:1802`).
   - The mechanism paragraph gains **F1.4's sentence with FA.4's clause**, verbatim: "No effect is keyed on the gate. … settle it (a running animation takes a new scale from its next frame, so within two frames of a switch); keying an effect … strands it (`reduced-motion-live-gate` finding 1)."
   - "**Closed 2026-10-24** on that merge, as `053e9cc`" follows #21's pattern, with the reviews by path and the as-built facts (`Clay.kt:219-220`, 14 calls in 9 files, no `Settings.Global`, `contentResolver` or `ContentObserver` call in main). It also records how the no-gate-keyed-effect rule is held, which is review plus a grep: the auditor found that the star test misses a bare key restored without its guard (`code-auditor-finding1.md`).
   - The Item cell's "seven test files drive the gate through `Settings.Global`" gets a dated "Superseded … at the RM close" note, ruling §9 item 1's other half.
   - The Owner cell reads "closed", with the old chain quoted.
3. **§8.1**:
   - **ruling §9 item 2's sentence**, verbatim, as its own paragraph after the Compose-test-rule paragraph (`:845`);
   - the v2 paragraph's "added by the first branch that next touches that file", **marked done** (`:834`): `2b98c5c`, naming `recreateTheScreen` and `deliverTheTapsWrites`, which I checked in the commit's hunk;
   - **the four RM classes on v1** (`:839`): `AnimationsEnabledGateTest`, `AnimationsEnabledGateWithoutScaleTest`, `GameScreenCueRotationTest` and `ResultsStarsLiveMotionTest` (`2b98c5c`), plus the new star suite (`45f84ec`), and why `effectContext` is no reason for v2;
   - the paragraph's count, now on `053e9cc` (`:836`): 38 files, 37 suites and the helper. The old "33 suites … at `6aa7624`" is kept and explained as 33 files.
4. **§9.8: the `createComposeRule` figure** (`:1136`, the table row): **39**, from the merged tree's compiler.
   - `./gradlew --max-workers=2 :app:compileDebugUnitTestKotlin --rerun` prints 40 `w:` lines: 39 of this deprecation, 1 kotlin-reflect, 0 from `src/main`.
   - The 39 come from 38 files, because `WrongTapBoards.kt:496` and `:503` each call the rule.
   - RM's +5 are the four moved classes and the star suite.
   - The old cell is kept as "*Until the RM close this read: …*", with the reason its 33 was a file count: `6aa7624`'s `WrongTapBoards.kt:495` holds two calls, and audio's merge-in report totals 37 = 34 + 1 + 1 + 1.
   - The same figure is updated where §9.8 repeats it: the caption note (`:1128`, the column now sums to 249), the row's Fix column, and rule 2's parenthetical (`:1170`).
5. **§15's RM row: closed** (`:1762`). "*Closed 2026-10-24*" goes in the likelihood cell, as for R-2's three closed rows. The mitigation cell gains the close and its two residuals: (1) F1, accepted under ux §10's 15 s line, with §14.2's aim; (2) §6, a low 3.0.1 defect (#32), named in the Q-PUBLISH-3.0.0 known issues by the producer, as STATUS.md records.
6. **§14.2: two lines** (`:1729`, `:1735`), under "**Added 2026-10-24 (the RM close …):**":
   - F1's backlog aim, under §10's order. Its route and cost come from the sign-off's §2: an app-provided `MotionDurationScale` that observes and re-reads on start, which puts a `Settings` read and a lifecycle hook back on the start path. So it is post-3.0.0 and carries a 15e sitting.
   - Landscape layout quality: O-5 (`15-founder-feedback/tech-lead-signoff.md:280-283`, checked), with QA's observation 2 and the 2.0-against-3.0.0 fact that can make it a regression. The screenshot the sign-off names was deleted with the task's QA artifacts, so I cite the report.
7. **§16: item (b), the new row #32** (`:1806`). It holds the title as §4(b) words it; "**Open, owned. Severity low; 3.0.1; not cut-blocking**"; the chain tech-lead (brief) → android-game-dev → code-auditor + qa-engineer → tech-lead; and the citations to ux 23d (2026-10-24), ruling §6 and `ux-designer.md` §1.
   - The defect's facts are re-read on `053e9cc`: `HintUndecidedNotice.kt:155-189`, the timer at `:163-168` outside the gate's branch, and the only one of the 14 sites that hosts a live region.
   - It says when the brief is due, including the talkback-board condition, which I re-checked: `feature/talkback-board` is still `b8aa8c4` and does not touch the file. It points to the sign-off's §4(a) for the brief's terms and does not restate them.
8. **§17: ruling §9 item 3's row** (`:1876`). It supersedes the audio sign-off's §3(c) premise, quoted from that report's line 84 ("stay green as they are, because Compose's flow reads that same key …"), plus RM's "stay green unedited" and #28's account of the seven, in favour of ruling A.
9. **The header: its dated sync sentence** (`:46`). It comes after r1-close's sentence, so `develop`'s order is kept (§17's merge-in rule, hunk (1)).

### Beyond the nine items (flagged; each keeps one fact consistent, and each reverts cleanly)
- **§17's second new row** (`:1877`): the 33 → 39 correction, explained as a file count against the compiler's lines. The file's own rule lists supersessions in §17, and r1-close gave 29 → 33 a row.
- **§9.8's other mentions of the figure** (the caption `:1128`, the Fix column, rule 2 `:1170`) and **§8.1's count** (`:836`). Item 4 names "the figure", and these cells repeat it.
- **Merge order step 5** (`:1646`): RM merged as `053e9cc` before 5a, so 5a is the second of that pair.
- **#28's enforcement sentence** (`:1802`): the rule is held by review and a grep, not by the star test, per the auditor's Info note.

## Checks
All in `/Users/ermanergoz/Desktop/PegSolitarie-rmc`. The full Check was not run (docs only, as dispatched); one Gradle compile was run, as allowed.
- `cat .claude/rules/VERSION` (0.6.2) against the plugin's `plugin.json` (1.0.0): older, so the plugin rule files were read in full — pass.
- `git worktree add … -b feature/tech-design-rm-close develop`: HEAD `053e9cc`, the RM merge (`git log -1`: parents `29a078d` and `0a059da`) — pass.
- `./gradlew --max-workers=2 :app:compileDebugUnitTestKotlin --rerun` on `053e9cc` (load 4.6 at the start; BUILD SUCCESSFUL in 17 s; 38 actionable tasks, 10 executed, 28 from cache). The log is in the scratchpad (`…/scratchpad/rmc/compile.log`). `grep -c '^w: '` gives 40: 39 `createComposeRule` deprecations and 1 "Call uses reflection API" at `GameScreenSoundTest.kt:304`. 0 lines from `/src/main/`. The 39 come from 38 distinct files, and only `WrongTapBoards.kt` has two (`:496`, `:503`) — pass.
- `git grep -l 'import androidx.compose.ui.test.junit4.createComposeRule'` gives 38 files on `053e9cc` and 33 on `6aa7624`. The 5 new ones are the four moved classes and `TutorialCompletionStarLiveMotionTest`, and none was removed. 37 of the 38 hold `@Test`; `WrongTapBoards.kt` holds none. `git grep -l 'junit4.v2'` finds only `GameScreenHintNoticeTest.kt` — pass.
- `git grep -E 'createComposeRule'` in `WrongTapBoards.kt` on `9f0e67a` and on `6aa7624` shows line 495 with two calls (`createComposeRule() else createComposeRule(effectContext…`).
  - Over `app/src/test*`, `git grep -n -E 'createComposeRule\('` (comments and `v2.`-qualified calls excluded) matches 34 lines on `6aa7624` and 40 on `053e9cc`.
  - Each count includes one v2 call: `GameScreenHintNoticeTest` calls the rule through its `junit4.v2` import (`:89` on `053e9cc`), and no warning comes from that call.
  - Without it, there are 34 v1 calls on `6aa7624`, on 33 lines, `:495` holding two. On `053e9cc` there are 39 v1 calls, one per line, and they match the compiler's 39 line for line (the one static match without a `w:` line is `GameScreenHintNoticeTest.kt:89`).
  - `android-game-dev-merge.md:30` totals 37 `w:`, which is 34 + 1 + 1 + 1
  
  — pass.
- `git show --stat 2b98c5c` lists 11 files, the four classes among them, and its `GameScreenHintNoticeTest.kt` hunk is the §8.1 KDoc sentence (`:76-80`) — pass.
- `git diff --quiet 45f84ec 053e9cc -- . ':(exclude).planning' ':(exclude)docs/tech-design.md' ':(exclude)docs/ux-spec.md'` exits 0 — pass.
- `git grep` on `053e9cc` in `app/src/main`:
  - the gate: 15 lines in 9 files (the definition at `Clay.kt:219-220` plus 14 calls);
  - `LaunchedEffect(animationsEnabled` and `LaunchedEffect(rememberAnimationsEnabled`: none;
  - `Settings.Global` / `contentResolver` / `ContentObserver`: one KDoc line, `Clay.kt:212`
  
  — pass.
- `sed -n 155,192p ui/game/HintUndecidedNotice.kt`: the slot at `:155-189`, the timer at `:163-168`, `key(raiseKey)` at `:171`. `BoardCanvasWrongTapReducedMotionTest.kt:30-32` is the one-frame lag note on `053e9cc` (the sign-off's `:33-35` predates `2b98c5c`), but #32 cites no line of it — read.
- `sed -n 712,727p ~/Library/Android/sdk/sources/android-35/android/content/ContentResolver.java`: `NOTIFY_NO_DELAY`'s KDoc ("not be subject to any delays when dispatching to apps running in the background", `@hide`) at `:715-726` — pass.
- `sed -n 84p .planning/reports/audio-review/tech-lead-signoff.md`: §3(c)'s text as §17 quotes it. `sed -n 278,284p …/15-founder-feedback/tech-lead-signoff.md`: O-5 — pass.
- `git log -1` over `053e9cc 0a059da 7382351 45f84ec 2b98c5c 335bc3b 35cfd35 47e7477 873fb9e b8aa8c4 6aa7624 9f0e67a 13c04c6 bd116a0`: each exists with the subject the text gives. Each cited report path exists, and so does `testing/SystemMotionScale.kt`. `git rev-parse --short feature/talkback-board` gives `b8aa8c4`, and `git diff --name-only develop...feature/talkback-board -- …/HintUndecidedNotice.kt` is empty — pass.
- **Table shape:** `python3 -I …/scratchpad/rmc/tablecheck.py` counts unescaped pipes per row against each header. `develop`'s file: 20 tables, 0 mismatches. The edited file: 20 tables, 0 mismatches. It caught nothing, but one cell first drafted a grep with a raw `|`, rewritten before the check — pass.
- **§16 numbering** (`awk` over §16): 1 … 32, each once — pass.
- `git diff --check` is clean. `git diff -U0 | grep '^-'` gives 11 removed lines, each one rewritten with its superseded text kept where it no longer holds. `git show --stat HEAD`: `docs/tech-design.md` only, +50 / −11 — pass.
- Commit: `300198e docs(14): close RM in the tech design`. This repo has no git hooks (no `core.hooksPath`, no non-sample hook), so the one trailer the project's `.claude/rules/gitflow.md` names was written by hand.

## Open issues / risks
1. **The 33 → 34 reading of `6aa7624`** rests on the static count and on the merge-in report's arithmetic, not on a compile of `6aa7624`. Only the merged tree was compiled, as the dispatch allowed. The auditor's "35 → 39" on RM's test compilation agrees. If the reviewer wants it measured, it is one more compile of `6aa7624`.
2. **#32's numbering:** parallel branches (talkback-board, licences-screen, colour-vision, home-chip-arrival) may add §16 rows. Whichever merges second renumbers its own rows under §17's merge-in rule, hunk (3).
3. **The release checklist:** item 1.1 reads row RM, which now says Done, so this branch should be on `develop` before 1.1 runs (sign-off §4(c) "When"). The auditor's I-2 from r1-close (checklist 1.1 should name RM) is still the release-manager's.
4. **Q-019:** the author is on Opus, and so will the reviewers likely be. The mitigation is reviewers who re-run the Checks' commands.
5. The `kotlin-reflect` row of §9.8's table still cites `GameScreenSoundTest.kt:300`, which is `e1875cb`'s line, and the line is `:304` on `053e9cc`. That row is the `e1875cb` count, by the caption, so I left it.

## Handoff
Next: producer — merge this report's commit after the reviews. Dispatch a code-auditor to read `git diff develop...feature/tech-design-rm-close -- docs/tech-design.md` as text. The most useful checks are the 39 / 34 figures (re-run the one-module compile, or the grep), the §17 rows and the four flagged additions. Then dispatch a fresh tech-lead for the sign-off; QA does not apply. Merge before release checklist 1.1 runs. Then schedule the 3.0.1 notice brief (sign-off §4(a); §16 #32) as a fresh tech-lead dispatch.
Next: code-auditor — the text read above.
Next: tech-lead (fresh) — the sign-off, ruling on the four flagged additions and open issue 1.
