# tech-lead report — tech-design-rm-close, loop 1 (the code-auditor's text findings)
Date: 2026-10-24
Branch: feature/tech-design-rm-close (bd3218c..bd3218c; read at `ed15e8b`; loop 0 was `300198e`, recorded in `tech-lead-loop0.md`; worktree `/Users/ermanergoz/Desktop/PegSolitarie-rmc`)
Skills loaded: ergoz-soft:standards (role tech-lead; game; android-compose-game). The worktree's snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin's rule files were read in full in this session: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing and dispatch. Also the snapshot's `.claude/rules/lessons.md`, android-architecture and android-gradle-logic. This is the same session as loop 0, resumed inside its own review loop
Ran in parallel with: home-chip-arrival, colour-vision, licences-screen and talkback-board, each in its own worktree
Ran on: claude-opus-5-5 at effort xhigh (policy). **Staffing deviation, recorded:** the agent file pins fable at xhigh. The Fable limit is reached, and the founder's answer to Q-019 (option 3, re-confirmed 2026-10-24) puts every Fable-pinned role on Opus until 3.0.0 is published. The author, the code-auditor and the coming sign-off are all on Opus.
Fan-out: none
Loops: 1
Read in full: `.planning/reports/tech-design-rm-close/code-auditor-loop0.md`
Lessons applied: none (the snapshot holds 0 active lessons for game / android-compose-game; `.planning/recall/RECALL.md` does not exist)

## Summary
- The code-auditor's four text findings are fixed in `docs/tech-design.md`, in one commit: `bd3218c`, +11 / −7 lines, that file only. Each fix uses the finding's suggested wording, or text equivalent to it.
- The four flagged additions of loop 0 stay, as the auditor ruled. Finding 4's precision is now in the #28 sentence. The flagged §17 row also takes the auditor's optional note, which cites its `6aa7624` compile.
- **Loop 0's open issue 1 is closed by the auditor's compile counts** (`code-auditor-loop0.md`, Checks). The one-module compile on `6aa7624` prints 35 `w:`: 34 `createComposeRule` deprecations from 33 files, with `WrongTapBoards.kt:495` twice, and 1 reflect line. On `053e9cc` it prints 40: 39 deprecations from 38 files, and 1 reflect line. So both 39 and the 33 → 34 correction hold by the compiler, and RM's delta is +5.
- No figure, sha or placement changed. The edits touch wording only, apart from one citation path, which follows the rename of the loop-0 report.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-rmc/docs/tech-design.md`: the fixes below, committed alone as `bd3218c` (`docs(14): RM-close loop 1, the auditor's text findings`).
- `/Users/ermanergoz/Desktop/PegSolitarie-rmc/.planning/reports/tech-design-rm-close/tech-lead.md`: this report, committed alone after it. Loop 0's report is `tech-lead-loop0.md`, unchanged.

### The findings, each with its new line (on `bd3218c`)
1. **F1 (medium): §17's ruling-A row** (`:1880`).
   - It said "and the seven had gone vacuous". It now reads: "and seven of their tests, in five of the files, had gone vacuous (ruling §1; `GameScreenHintNoticeTest`'s reduced-motion test went red instead, and `BoardCanvasWrongTapReducedMotionTest`'s rule already carried the element)".
   - The next sentence now says "The seven files now drive …", so "they" no longer reads as the tests.
   - The wording was checked against `reduced-motion-live-gate/tech-lead-ruling.md`: `:25` ("7 tests in 5 files are now vacuous"), `:31` (the board test's rule carries the element) and `:32` (the notice test ran 19/20, its reduced-motion test red).
2. **F2 (low): §9.8 rule 2's parenthetical** (`:1174-1175`). "(33 on `6aa7624`, 2026-10-24; …)" now reads "(33 files on `6aa7624`, 32 suites and the helper, 2026-10-24; 37 suites and one helper on `053e9cc`, the RM close)". It is the auditor's wording. On `6aa7624`, 32 of the 33 importing files hold a `@Test`.
3. **F3 (low): the header's sync sentence** (`:46-56`).
   - "and a §17 row for the test ruling" now reads "two §17 rows (the test ruling; the 33-file count of v1 users on `6aa7624`, which held 34 calls) and merge-order step 5's note".
   - Following r1-close's header, the sentence now also records this loop. "Its review loop 1 (`code-auditor-loop0.md`)" corrected F1–F4, and both figures stand by the compiler on both trees (`:53-56`).
   - The sentence's report citation is now `tech-lead-loop0.md`, with "its loop 1 in `tech-lead.md`" (`:47`), because the loop-0 report was renamed.
4. **F4 (low): #28's enforcement sentence** (`:1806`).
   - It now reads: "The no-gate-keyed-effect rule is held by review. A `git grep` … finds neither on `053e9cc`, but it matches only those two spellings: an effect keyed on a gate-derived local (`GameActionDock.kt:147` and `:174`'s `animate`), a second key or a named key escapes it."
   - It adds the auditor's census: "On `053e9cc` no `LaunchedEffect` or `DisposableEffect` in the gate's nine files is keyed on the gate or on a value derived from it (`code-auditor-loop0.md`, finding 4)".
   - The star-test sentence follows, unchanged.
   - `GameActionDock.kt:147` and `:174` were checked on `053e9cc`: both read `val animate = rememberAnimationsEnabled() && look != DockLook.Working`.

### The flagged additions (kept)
- **§17's figure row** (`:1881`) keeps its text and adds the auditor's optional note: "The same one-module compile on `6aa7624` prints 34 such lines from those 33 files (`tech-design-rm-close/code-auditor-loop0.md`)". Its Where cell now names `tech-lead-loop0.md` and `code-auditor-loop0.md`, because the loop-0 report was renamed.
- §9.8's other mentions, §8.1's count and merge-order step 5's note are unchanged. #28's sentence is unchanged apart from F4.

## Checks
All in `/Users/ermanergoz/Desktop/PegSolitarie-rmc`. A text-only fix, so no Gradle was run: the figures rest on the auditor's two compiles, cited above.
- `git log --oneline -5` at the start: head `ed15e8b`; the auditor's report is `code-auditor-loop0.md`, and mine is `tech-lead-loop0.md` — read.
- `grep -n` in `reduced-motion-live-gate/tech-lead-ruling.md`: `:25`, `:31` and `:32`, as quoted under F1 — pass.
- `git grep -n 'val animate = rememberAnimationsEnabled()' 053e9cc -- app/src/main`: `GameActionDock.kt:147` and `:174` — pass (F4).
- `@Test` among the 33 v1-importing files on `6aa7624`: 32, and `WrongTapBoards.kt` holds none — pass (F2).
- `python3 -I …/scratchpad/rmc/tablecheck.py docs/tech-design.md`: 20 tables, 0 mismatches — pass.
- `awk` over §16: 1 … 32, each once — pass.
- `git diff --check` (before the commit): clean. `git show --stat bd3218c`: `docs/tech-design.md` only, +11 / −7. Each of the 7 removed lines is a line rewritten by the fixes above — pass.
- `grep -nF` proofs on the committed file:
  - "seven of their tests, in five of the files, had gone vacuous" → 1880;
  - "(33 files on `6aa7624`, 32 suites and the helper, 2026-10-24;" → 1174;
  - "two §17 rows (the test ruling; the 33-file count" → 52;
  - "Its review loop 1" → 43 (r1-close's) and 53 (this one's);
  - "The no-gate-keyed-effect rule is held by review. A `git grep`" → 1806;
  - "The same one-module compile on `6aa7624` prints 34" → 1881;
  - "its loop 1 in `tech-lead.md`" → 47
  — pass.
- Commit `bd3218c` ends with exactly one trailer, `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, written by hand (this repo has no git hooks) — pass.

## Open issues / risks
1. **Q-019:** the author, the auditor and the coming sign-off are all on Opus. The mitigation is recorded: the auditor compiled both trees, and the figures do not rest on any report.
2. **The deadline:** row RM must say Done on `develop` before release checklist 1.1 runs (sign-off §4(c), "When").
3. **#32's number** can move if a parallel branch merges a §16 row first. Its merge-in renumbers under §17's merge-in rule, hunk (3).
4. Loop 0's open issue 5 stands: the kotlin-reflect row keeps `e1875cb`'s `:300` by the table's caption, and the auditor's I-2 agrees.

## Handoff
Next: producer — commit this report alone. Then resume the code-auditor in this step's loop to re-read `git diff ed15e8b..bd3218c -- docs/tech-design.md`. Then a fresh tech-lead's sign-off, which can cite `code-auditor-loop0.md`'s compile counts for 39 and 34; QA does not apply. Merge before release checklist 1.1 runs.
Next: code-auditor — the re-read of F1–F4 at the lines above, and the flagged row's new citation.
Next: tech-lead (fresh) — the sign-off.
