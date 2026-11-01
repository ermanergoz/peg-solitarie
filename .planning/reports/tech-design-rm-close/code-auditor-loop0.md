# code-auditor report — tech-design-rm-close (the RM-close sync of `docs/tech-design.md`, read as text)
Date: 2026-10-24
Branch: feature/tech-design-rm-close (report only; read `git diff develop...feature/tech-design-rm-close -- docs/tech-design.md` at `47767cf`, merge base `053e9cc` = `develop`; the edit is `300198e`, the author's report `47767cf`)
Skills loaded: ergoz-soft:standards (role code-auditor; game; android-compose-game). The worktree's snapshot is 0.6.2, older than plugin 1.0.0, so the plugin rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, kotlin-android, compose, testing; puzzle-design and play-store are identical to the snapshot copies, which were read; also the snapshot's lessons.md. Plus code-reviewer, security-reviewer. No Compose, web or Swift code is in the diff, so the stack review skills do not apply
Ran in parallel with: home-chip-arrival (developer loop 2), colour-vision (developer loop 1), licences-screen (developer and QA), talkback-board (QA)
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation, recorded:** the agent file pins fable at xhigh. The Fable limit is reached, and the founder's answer to Q-019 (option 3, re-confirmed 2026-10-24) puts every Fable-pinned role on Opus until 3.0.0 is published. The author is on Opus too, so this read shares the author's model. Mitigation: every figure, sha and path below was re-derived from git, the compiler and the sources jars; none is taken from the author's report (Checks)
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons for game / android-compose-game)

## Summary
- All nine items of the sign-off's §4(c) are present and placed where it says, §4(b)'s row is #32, and §16 runs 1–32 with each number once. Every table keeps its column count. All 16 cited shas and every cited path exist.
- **The figures hold, by the compiler on both trees.** `053e9cc` prints 39 `createComposeRule` deprecation lines from 38 files, so §9.8's 39 holds. `6aa7624` prints 34 from 33 files, because `WrongTapBoards.kt:495` holds two calls, so the 33 → 34 correction holds too. RM's delta is +5. The author's open issue 1, which rested on a static count, is now measured.
- **VERDICT: FAIL, on text only.** Finding 1 misstates the ruling in a §17 row: it says the seven files went vacuous, but the ruling says 7 tests in 5 files did. Findings 2–4 are low precision points for the same loop. No figure, sha or placement is wrong.
- The four flagged additions are correct and should stay. #28's sentence should take finding 4's precision.

## Findings
Line numbers are `docs/tech-design.md` on `47767cf`, whose copy of the file is `300198e`'s.

1. **Medium (must fix): `docs/tech-design.md:1876` — the new §17 row says "So a `Settings.Global` write reached neither the gate nor the clock, and the seven had gone vacuous."** Here "the seven" is the seven files of the row's first cell ("for the seven (R1's `GameScreenHintNoticeTest` the seventh)"). The ruling says otherwise (`reduced-motion-live-gate/tech-lead-ruling.md` §1):
   - "7 tests in 5 files are now vacuous";
   - `GameScreenHintNoticeTest` "was not vacuous; it caught the change" (it ran 19/20, §1(a));
   - `BoardCanvasWrongTapReducedMotionTest` is "the only one whose rule carries the element", so its six tests failed the pre-ruling probe: they reached the path.

   `2b98c5c`'s own message says "seven reduced-motion guards had gone vacuous", which means tests. Fix: "…and seven of their tests, in five of the files, had gone vacuous (ruling §1; `GameScreenHintNoticeTest`'s reduced-motion test went red instead, and `BoardCanvasWrongTapReducedMotionTest`'s rule already carried the element)".
2. **Low: `docs/tech-design.md:1170-1171` — §9.8 rule 2 now reads "how 29 reviewed Compose suites synchronise (33 on `6aa7624`, 2026-10-24; 37 suites and one helper on `053e9cc`, the RM close)".** The sync edited this parenthetical but left 33 standing as a count of suites. The same sync rules that 33 was a file count, with the helper among them:
   - §8.1 at `:837-838`: "a count of 33 files with that helper among them";
   - the table row at `:1136`;
   - the Fix column: "33 files until the RM close";
   - the new §17 row at `:1877`.

   On `6aa7624`, 32 of the 33 importing files hold a `@Test`, and `WrongTapBoards.kt` holds none. At `e1875cb` the helper did not exist, so its 29 are suites. Fix: "(33 files on `6aa7624`, 32 suites and the helper, 2026-10-24; 37 suites and one helper on `053e9cc`, the RM close)".
3. **Low: `docs/tech-design.md:46-52` — the header's sync sentence ends "and a §17 row for the test ruling", but the branch adds two §17 rows (`:1876`, `:1877`).** It never names the second row, a supersession of a recorded figure (33 files, 34 calls), even though the header says supersessions are listed in §17. Nor does it name merge-order step 5's note (`:1646-1647`). `300198e`'s message has the same gap ("a §17 row for ruling A"), but that is history and needs no change. Fix: "…§16 #32 (…), two §17 rows (the test ruling; the 33-file count of v1 users on `6aa7624`, which held 34 calls) and merge-order step 5's note".
4. **Low: `docs/tech-design.md:1802` — #28's new sentence says the no-gate-keyed-effect rule "is held by review and by a `git grep` in `app/src/main` for `LaunchedEffect(animationsEnabled` and `LaunchedEffect(rememberAnimationsEnabled`".** The facts are right: the grep finds neither on `053e9cc`. But it overstates what the grep holds. It matches two literal spellings only. These escape it:
   - an effect keyed on a gate-derived local, such as `GameActionDock.kt:147` and `:174`'s `val animate = rememberAnimationsEnabled() && …`;
   - a second key, such as `LaunchedEffect(raiseKey, animationsEnabled)`;
   - a named key, such as `key1 = animationsEnabled`.

   I listed every `LaunchedEffect` and `DisposableEffect` in the gate's nine files on `053e9cc` (Checks). None is keyed on the gate or on a value derived from it, so the rule holds today. Fix: "…is held by review. A `git grep` … finds neither on `053e9cc`, but it matches only those two spellings: a gate-derived local (`GameActionDock.kt`'s `animate`) or a second key escapes it. The star test catches …".

**Info (no change required):**
- **I-1, `:1136`'s "and none from `src/main`".** It is true, but the command it cites does not show it, because `:app:compileDebugKotlin` is up to date in that run. I measured it separately: `:app:compileDebugKotlin --rerun` on `053e9cc` prints 0 `w:` and 0 `e:`.
- **I-2, the kotlin-reflect row's `GameScreenSoundTest.kt:300`.** The `createComposeRule` cell now points to "the kotlin-reflect line below", and that row cites `:300`. The compile on `053e9cc` prints `:304`; `:300` is the line on `e1875cb` and on `6aa7624`. The caption makes every other row `e1875cb`'s, so I agree with the author's open issue 5 to leave it.
- **I-3, the author's report.** `.planning/reports/tech-design-rm-close/tech-lead.md:77` and `:85` carry trailing whitespace (`git diff --check`). Its Checks call `git diff --check` clean, which holds for `docs/tech-design.md`, the file the check covered. It is a record, not product text.

Suppressions: none (a docs-only diff, so nothing to approve or reject). THIRD_PARTY.md: no dependency change. Security: the added lines hold no key, token, password, ad unit or Play Games id (scan in Checks).

## The nine items of §4(c), and §4(b)
| # | Item | Where on `47767cf` | Present, accurate, placed |
|---|---|---|---|
| 1 | §14.1 row RM: Done | `:1593`, last cell | Yes. "**Done.**" opens the cell, in R1's and R-2's pattern, and the old cell stays after "*The record:*". It names the merge `053e9cc` (parents `29a078d`, `0a059da`), the code `7382351..45f84ec` (every non-docs commit of the branch lies in it), the sign-off, both auditor PASSes, QA and the ux rulings by path. Ruling §9 item 1's three sentences are in the Item cell, with the old sentence dated. Finding 1's rider (FA.2, 165 → 639 px, the red mutant) and F1's acceptance (ux §10, 15 s, `ContentResolver.java:715-726`) are in. So is QA's §8 PASS (6 starts, 2 rotations, 2 restores, 0 lines, 0 crashes). The kept **Blocks the branch cut** marker is harmless: the table note at `:1577-1579` says "A row marked **Done** … blocks nothing" |
| 2 | §16 #28 closed | `:1802` | Yes. F1.4's sentence with FA.4's clause is verbatim, in the mechanism paragraph. #21's pattern is followed: the close is appended and the Owner cell reads "closed", with the old chain dated. The Item cell's account of the seven gets its dated "Superseded" note. `Clay.kt:219-220` is right, and so are 14 calls in 9 files and no `Settings.Global`, `contentResolver` or `ContentObserver` call (one KDoc line, `Clay.kt:212`). See finding 4 |
| 3 | §8.1 | `:833-848` | Yes. Ruling §9 item 2's sentence is verbatim, in its own paragraph after the Compose-test-rule paragraph. The v2 KDoc is marked done: `2b98c5c`'s hunk names `recreateTheScreen` and `deliverTheTapsWrites`. The four classes are on v1: they were v2 at `7382351`/`335bc3b` and v1 at `2b98c5c`. The star suite is on v1 (`45f84ec`), and `AndroidComposeTestRule.android.kt:66` is right. The count is 38 files = 37 suites + the helper |
| 4 | §9.8 figure | `:1136` | Yes: 39, re-measured (Checks). The old cell stays as a dated "until" note, as §17 merge-in rule (4) asks. One mention at `:1170` is finding 2 |
| 5 | §15 RM row closed | `:1762` | Yes. "*Closed 2026-10-24*" is in the likelihood cell, in R-2's closed-row pattern. Both residuals are there. The producer's known-issue line matches STATUS.md:13 on `053e9cc` |
| 6 | §14.2, two lines | `:1729-1741` | Yes, as two items of the section's running list. The aim, the route and the 15e cost are the sign-off's §2, and `Pixel_9a_bench` matches the RM row's sitting rule. O-5 is at `15-founder-feedback/tech-lead-signoff.md:280-283`. The screenshot really is gone: the `PegSolitarie-rm` worktree is removed, and no copy is on the Desktop |
| 7 | §16 #32 | `:1806` | Yes. #32 follows `develop`'s #31. The title is §4(b)'s, verbatim, and so are the status, severity, version and chain. The citations are 23d, ruling §6 and `ux-designer.md` §1. The facts hold on `053e9cc`: the slot `:155-189`, the timer `:163-168`, `key(raiseKey)` `:171` and `clayEntrance(0)` `:182`. `feature/talkback-board` is at `b8aa8c4`, not on `develop`, and leaves the file untouched |
| 8 | §17, ruling §9 item 3 | `:1876` | Present and placed. The §3(c) quote matches `audio-review/tech-lead-signoff.md:84`. **Not accurate in one clause: finding 1** |
| 9 | Header sync sentence | `:46-52` | Present, after r1-close's, per merge-in rule (1). **Incomplete: finding 3** |

## The four flagged additions
1. **§17's second row (`:1877`), the 33 → 39 correction.** Correct, by the compiler on both trees. Its claim "34 calls" for `6aa7624` was a static count and is now measured: 34 lines. **Keep it.** §17 is where this file lists a superseded figure, and r1-close gave 29 → 33 a row of its own. Optional: its By cell can say that the same compile on `6aa7624` prints 34.
2. **§9.8's other mentions, and §8.1's count.** The caption's 249 is right: 6 + 4 + 0 + 198 + 39 + 1 + 1. The Fix column is right, and so is §8.1's 38 = 37 + 1. Rule 2 is finding 2. **Keep them.** Merge-in rule (4) wants one figure per cell, and leaving the repeats at 33 would give the file two figures for one thing.
3. **Merge order step 5 (`:1646-1647`).** Correct: RM merged as `053e9cc`, and `feature/talkback-board` is not an ancestor of `develop`. **Keep it.** It answers step 5's own "its slot against 5a is set in its row", in the place a reader of the order looks.
4. **#28's enforcement sentence (`:1802`).** Its facts are correct. The auditor's Info confirms that a bare key without its guard keeps the star test green, and the grep finds neither spelling. **Keep it, with finding 4's precision.**

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-rmc/.planning/reports/tech-design-rm-close/code-auditor.md`: this report, committed alone. Nothing else was written in the repo. The scratch files (diff copy, compile logs, the table script, the extracted sources) are in this session's scratchpad. The throwaway worktree of `6aa7624` was removed.

## Checks
All in `/Users/ermanergoz/Desktop/PegSolitarie-rmc` unless named. A docs-only read, so the full Check was not run. Three single-task compiles ran at `--max-workers=2`, as dispatched (host load 4.9 at the start).
- `cat .claude/rules/VERSION` gives 0.6.2, against the plugin's `plugin.json` at 1.0.0. Older, so the plugin rule files were read in full — pass.
- `git merge-base develop feature/tech-design-rm-close` gives `053e9cc`, which is `develop`. `git log develop..feature/tech-design-rm-close` lists `300198e`, which touches `docs/tech-design.md` only (+50 / −11), and `47767cf`, the report only. Each commit carries one trailer, the Fable line — pass.
- `git diff --quiet 053e9cc HEAD -- . ':(exclude).planning' ':(exclude)docs/tech-design.md'` exits 0, so this worktree's code is `053e9cc`'s — pass.
- `./gradlew --max-workers=2 :app:compileDebugUnitTestKotlin --rerun` (BUILD SUCCESSFUL in 5 s) prints 40 `w:`:
  - 39 `createComposeRule` deprecations, from 38 distinct files, the only double being `WrongTapBoards.kt` `:496` and `:503`;
  - 1 kotlin-reflect line, at `GameScreenSoundTest.kt:304`;
  - 0 from `src/main`

  — pass.
- The same command in a throwaway `git worktree add --detach` of `6aa7624` in the scratchpad, removed afterwards (BUILD SUCCESSFUL in 7 s), prints 35 `w:`:
  - 34 deprecations, from 33 files, with `WrongTapBoards.kt:495` twice;
  - 1 reflect line, at `:300`

  — pass: the 33 → 34 correction holds by the compiler.
- `./gradlew --max-workers=2 :app:compileDebugKotlin --rerun` on `053e9cc` prints 0 `w:` and 0 `e:` (I-1) — pass.
- `git grep -l 'import androidx.compose.ui.test.junit4.createComposeRule'` over `app/src/test*` finds:
  - 29 files on `e1875cb`, where `WrongTapBoards.kt` is absent;
  - 33 on `6aa7624`, 32 of them with a `@Test`;
  - 38 on `053e9cc`, 37 of them with a `@Test`. The 5 new files are the four moved classes and `TutorialCompletionStarLiveMotionTest`, and none was removed.

  `git grep -l 'junit4.v2'` finds only `GameScreenHintNoticeTest.kt` on both trees. The non-comment `createComposeRule(` matches number 35 and 40, one of each in the v2 class, so the v1 calls are 34 and 39 — pass.
- `git diff --stat 9f0e67a 6aa7624 -- app/src build-logic app/build.gradle.kts` is empty, so audio's merge-in Check compiled these same tests. Its 37 `w:` is therefore 34 + 1 + 1 + 1. Its own breakdown at `audio-review/android-game-dev-merge.md:30` sums to 36 — pass.
- Table shape: a pipe-count script (`python3 -I`, scratchpad) over `develop`'s and `HEAD`'s file finds 20 tables in each and 0 rows off their header's count — pass.
- §16 numbering: `awk` over `:1769-1807` gives 1 … 32, no duplicates (`develop`: 1 … 31) — pass.
- Shas: `git log -1` on each of the 16 cited shas resolves it. Each subject fits its use: `053e9cc` merge (parents `29a078d`, `0a059da`), `7382351`, `335bc3b`, `35cfd35`, `47e7477`, `2b98c5c`, `873fb9e`, `45f84ec`, `0a059da`, `6aa7624`, `9f0e67a`, `13c04c6`, `b39e28e`, `b8aa8c4`, `d656263`, `e1875cb`. Every non-docs commit of the RM branch lies in `7382351..45f84ec` — pass.
- Paths: every repo path cited in the added lines exists at `HEAD`, the bare file names included (each resolves to one file). `s8-off-3-rotated.png` exists nowhere under `~/Desktop`, and `PegSolitarie-rm` is removed, so "went with the deleted QA artifacts" holds — pass.
- `git diff --quiet 45f84ec 053e9cc -- . ':(exclude).planning' ':(exclude)docs/tech-design.md' ':(exclude)docs/ux-spec.md'` exits 0 — pass.
- `git grep` on `053e9cc` over `app/src/main`:
  - `rememberAnimationsEnabled()`: 14 calls in 9 files, plus the definition at `Clay.kt:219-220`;
  - `LaunchedEffect\((animationsEnabled|rememberAnimationsEnabled)`: exit 1, no match;
  - `Settings\.Global|contentResolver|ContentObserver`: one KDoc line, `Clay.kt:212`;
  - every `LaunchedEffect(` and `DisposableEffect(` in the nine files: keys `transition?.id, geometry, interactive`, `state.selected, interactive, wrongTap.dotRePops`, `raiseKey`, `state.coachTick`, `Unit` ×3, `target` and `play, earned`. None is gate-derived (finding 4)

  — pass.
- `git show 053e9cc:…/HintUndecidedNotice.kt`: the slot at `:155-189`, the timer at `:163-168`, `key(raiseKey)` at `:171` and `clayEntrance(0)` at `:182`. `git rev-parse --short feature/talkback-board` gives `b8aa8c4`. `git merge-base --is-ancestor feature/talkback-board develop` exits 1, so it is not merged. `git diff --name-only develop...feature/talkback-board -- …/HintUndecidedNotice.kt` is empty — pass.
- Sources:
  - `~/Library/Android/sdk/sources/android-35/android/content/ContentResolver.java:715-726`: `NOTIFY_NO_DELAY`, "not be subject to any delays when dispatching to apps running in the background", `@hide`;
  - `ui-test-android-1.11.3-sources.jar`: `ComposeUiTest.android.kt:591-601` builds the recomposer from `effectContext` plus the rule's own elements, and `:764` calls `WindowRecomposerPolicy.withFactory({ recomposer })`;
  - `ui-test-junit4-android-1.11.3-sources.jar`: `AndroidComposeTestRule.android.kt:66` is `createComposeRule(effectContext: CoroutineContext)`

  — pass.
- Quotes and records:
  - `audio-review/tech-lead-signoff.md:84` is §3(c), as quoted;
  - `15-founder-feedback/tech-lead-signoff.md:280-283` is O-5;
  - on `053e9cc`, `docs/ux-spec.md:4839` is "A switch made while the game is open", with its order, its aim and the 15 s line, and `:6311` is 23d's "A change of the animation setting is not a raise";
  - STATUS.md:13 on `053e9cc` records the Q-PUBLISH-3.0.0 known issue;
  - `git show 2b98c5c`: 11 files, and the v2 class's KDoc hunk names both helpers

  — pass.
- `git diff --check develop...feature/tech-design-rm-close`: `docs/tech-design.md` is clean. Two whitespace-only lines are in the author's report (I-3) — info.
- A secrets scan of the added lines (key, token, password, PEM, `ca-app-pub`, `AIza` and long digit runs) finds 0 hits — pass.
- `git status --short` is clean before this report. `git worktree list` no longer lists the throwaway worktree — pass.

## Open issues / risks
1. **Q-019:** this read, the author and the coming sign-off are all on Opus. The mitigation is recorded: two compiles, one per tree, and every claim re-derived. §16 #19's post-3.0.0 question about a Fable re-audit applies here too.
2. **The deadline:** row RM must say Done on `develop` before release checklist 1.1 runs (sign-off §4(c) "When"). This FAIL costs one text loop. The findings touch no figure, so the loop is short.
3. **#32's number** can still move if a parallel branch merges a §16 row first. Its own merge-in renumbers under §17's merge-in rule (3), as the author says.

## Handoff
Next: producer —
- commit this report alone;
- send findings 1–4 to the authoring tech-lead (fix mode, a text-only edit of `docs/tech-design.md` on this branch, in its own commit);
- then resume this code-auditor in the same step's loop for a re-read of the fix commit, after renaming this file to `code-auditor-loop0.md`;
- then a fresh tech-lead's sign-off. QA does not apply.

Next: tech-lead (the author) — findings 1–4 (the suggested wording is in each). Keep the four flagged additions. Open issue 1 of your report is closed by the compiler counts above.

Next: tech-lead (fresh, the sign-off) — after the re-read. It can cite this report's compile counts for 39 and 34.

VERDICT: FAIL
