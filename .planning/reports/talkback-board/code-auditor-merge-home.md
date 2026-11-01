# code-auditor report — talkback-board, the merge-in of develop with home-chip-arrival
Date: 2026-11-01
Branch: feature/talkback-board (a980033..402e5af; head at review `84d4a4f`, which adds only the developer's report; the merge's parents are `144153d` (loop 4's tip) and `305f741` (`develop`))
Skills loaded: ergoz-soft:standards (role code-auditor). The product snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies of studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose and testing in full. logging, puzzle-design and play-store are identical to the snapshot and were read there. I also read the product's `.claude/rules/lessons.md`. Other skills: code-reviewer, security-reviewer, compose-skill, compose-performance-audit.
Ran in parallel with: talkback QA device re-run, candy auditor, licences R2 fix
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** `agents/code-auditor.md` pins fable at xhigh. Founder answer Q-019 (option 3, re-confirmed by the 2026-10-24 addendum) puts every Fable-pinned role on Opus until 3.0.0 is published. The author also ran on Opus, so rule 1's "superior model" does not hold. Q-019's mitigations apply: a fresh reviewer, my own Check with `--rerun-tasks`, and my own mutation probes. Every ruling below rests on a command I ran, not on the developer's figures.
Fan-out: none
Loops: 0 (first review of this merge-in; the branch's last review round was loop 4)
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- **The `BoardCanvas.kt` resolution keeps both sides unaltered.** I compared the changed lines (`-U0`, `+`/`-` only) of each side against the merge minus the other parent, and both pairs are byte-identical. `git merge-tree` re-merged the parents, and the result differs from `a980033` only by the conflict markers. So no other file was hand-edited.
- **The read-only path is `develop`'s, and the start path is untouched.** Brief §12's tripwire line 1 prints nothing. Line 2 prints only the allowed `BoardCellOverlay(`, under `if (interactive)`. `drawBoard` through the end of the file hashes identically on `develop` and `HEAD`.
- **The retargets are not weaker.** Four probes in a throwaway worktree each go red where intended. One is caught only by the new Undo pin. In another, home's timer fix is undone: the new title → timer pin goes red, and the old title → board-stop assertion stays green on that same mutant.
- **Home's `GameScreenChromeTest` edit: APPROVED** (ruling below). My Check (`--rerun-tasks`, 104/104 tasks executed) is green.
- **VERDICT: PASS.** No finding to fix. Two Info items.

## Outputs
- `.planning/reports/talkback-board/code-auditor-merge-home.md`: this report, committed alone on the branch.

## Review

### 1. The `BoardCanvas.kt` conflict (`a980033`)
- **Develop's side survives unaltered.** I took the `+`/`-` lines of `git diff -U0 3ab1cae 305f741 -- BoardCanvas.kt` (the merge base to develop). They are identical to those of `git diff -U0 144153d a980033` (the branch parent to the merge). Those lines are:
  - the four-line comment and `val geometrySize = canvasSize`;
  - the four-line comment and `val frameGeometry = if (geometrySize == size) geometry else BoardGeometry.forCanvas(spec, size)`;
  - `geometry = geometry` → `geometry = frameGeometry`.
  - The indentation is unchanged too, because the branch's `draw` lambda body sits at the depth the `Canvas { }` body had.
- **The branch's side survives unaltered.** `git diff -U0 3ab1cae 144153d` and `git diff -U0 305f741 a980033` give identical changed lines on the file.
- **No change outside the hunk.** `git show --cc a980033` shows this one hunk. `git merge-tree --write-tree 144153d 305f741` reports the one conflict, and its tree differs from `a980033` only in `BoardCanvas.kt`, by 21 deleted lines: the markers and the side that was dropped. `strings.xml` auto-merged, and `strings.xml` has no duplicate `name=`.
- **The semantics hold.**
  - `geometrySize` is still read in composition and captured by the lambda, exactly as develop's inline block captured it. The draw-time read of `size` is unchanged.
  - Both canvases call the one `draw`:
    - The read-only canvas's modifier chain is develop's, less a `.then(Modifier)`, which is a no-op.
    - The playable canvas sits in the `Box` with `matchParentSize()` and `onSizeChanged` on the canvas itself, so `canvasSize` is still the drawn size.
  - `pointerInput(geometry)` still keys on the composed geometry, as on develop.
  - With equal sizes nothing is allocated, as develop's comment states.
- **Probe M4** (`geometry = frameGeometry` → `geometry = geometry` in `draw`): develop's `BoardCanvasFirstFrameTest` goes 2/2 red, `peg ink inside board_canvas in the first frame … but was : 0` and the same for `home_stage_art` (`:111`). So the resolved hunk is under test on the path home cares about.
- **§11.4 and brief §12, re-run by me after the merge:**
  - line 1 prints nothing (rc 1);
  - line 1, widened to the `-v35` theme folders, also prints nothing;
  - line 2 prints `+            BoardCellOverlay(` alone, and that line sits under `if (interactive)` (`BoardCanvas.kt:244-280` at `HEAD`);
  - `git diff develop...HEAD -- BoardCanvas.kt` has no hunk below the `Canvas(`/`draw` region. The `drawBoard`-to-end-of-file slice has the same SHA-1 (`7eb6e1eb…`) on `develop` and `HEAD`.
  - `develop` (`305f741`) is an ancestor of `HEAD`.

### 2. The retargeted tests (`402e5af`)
- **The requirement.** The spec's D-TB retarget table (`docs/ux-spec.md:5814`) reads: "Any test that counts or orders the gameplay or tutorial screen's TalkBack stops, 26b's included → Retarget to 26b as amended". 26b as amended (`:6733-6741`, `:5198-5206`) gives back → title → timer → board stop → each in-play cell → [banner or notice] → Undo → … Defect ruling §3 asks for "the timer once home has merged". Both tests now encode exactly that. `GameUiState.initial()` raises no banner, and its board is `Boards.english33Initial`, whose last in-play cell row-major is (6, 4), so Undo follows that cell.
- **The red before the retarget, confirmed.** I ran the `a980033` versions of both test files on the merged code:
  - `GameScreenChromeTest:132`: `the stop Compose reads after the timer / expected: Undo / but was : node 33`;
  - `GameScreenTalkBackTest:468`: `expected: 1410 / but was : 1402`.
  - 2 of 31 failed, as the developer reported.
- **Baseline at `HEAD`:** 6/6 and 25/25 green, with `--rerun`. The first run came from the build cache, so I did not count it.
- **Nothing was dropped.**
  - `GameScreenChromeTest` keeps its three checks: the clock is screen-reader focusable, title → timer, and something → Undo. Undo has moved to its 26b place, after the last cell, and timer → board stop is added.
  - `GameScreenTalkBackTest` keeps board stop → row 1's first cell and adds title → timer and timer → board stop.
  - The diff has no `@Ignore`, `@Suppress`, baseline or `!!`.
- **Probes**, in a throwaway `git worktree add --detach 84d4a4f` under the scratchpad, outside the repo. Each was reverted, with `git diff --quiet HEAD` confirmed clean after it. Each run used `./gradlew :app:testDebugUnitTest --tests …GameScreenChromeTest --tests …GameScreenTalkBackTest`.

| # | Mutation (production only) | Retargeted assertions that go red | Shows |
|---|---|---|---|
| M1 | `GameActionDock.kt`: `UndoControl` and `HintControl` swapped in the dock's `Row` | `GameScreenChromeTest:157` only, `the stop Compose reads after the board's last cell / expected: Undo / but was : node 73`. `GameScreenTalkBackTest` is 25/25 green | **Only the new Undo pin catches it.** The timer pins stay green. |
| M2 | `GameStatusStrip.kt:251`: `clearAndSetSemantics` → `semantics` (home's B-3 fix undone) | `GameScreenChromeTest:148` `whether the clock's node is screen-reader focusable / expected to be true`; `GameScreenTalkBackTest:472` `expected: 1865 / but was : 1873` (title → timer) | **The title → timer → board order.** |
| M2b | M2, with the `a980033` version of `theBoardStopFollowsTheTitleAndPrecedesTheFirstCell` | none: 1/1 **green** | The pre-retarget assertion was blind to the timer, so the retarget strengthens it. |
| M3 | `BoardCellOverlay.kt:142`: `STOP_BEFORE_CELLS = -1f` → `1000f` (board stop read after the cells) | `GameScreenChromeTest:154` `after the timer / expected: the board stop / but was : node 37`; `GameScreenTalkBackTest:473` `expected: 1873 / but was : 1877` | **The timer → board stop step.** |
| M4 | `BoardCanvas.kt` `draw`: `frameGeometry` → `geometry` | `BoardCanvasFirstFrameTest` 2/2 (§1 above) | The resolved hunk is pinned. |

- My M1 differs from the developer's probe (c). M2 repeats their probe (a). M2b, M3 and M4 are mine.
- **C11 (home sign-off Open issue 5).** `git grep 'ANIMATOR_DURATION_SCALE\|Settings\.Global' HEAD -- app/src/test domain/src/test` finds one writer, `AnimationsEnabledGateWithoutScaleTest.kt:27-29`, which is develop's deliberate writer, plus KDoc lines in `SystemMotionScale.kt`, `GameScreenSoundTest.kt` and `TutorialSoundTest.kt`. This matches the developer's claim.

### 3. Ruling on the edit to home's `GameScreenChromeTest`
**APPROVED.** The test came from `develop` and is edited on this branch, so brief §12 ("Existing tests: … if one must change, the report names it with the reason and the auditor approves it") puts the decision with me.
- **Home's intent is kept.** Home's defect ruling §3 asks for two things: (a) the clock's `isScreenReaderFocusable` is true; (b) Compose's hints read title → clock → the next stop. Both assertions stand word for word. On the tree without board stops, the next stop was Undo. D-TB inserts the board stop and the cells there, and the spec's retarget table orders exactly this retarget.
- **It is not a swap that hides Undo.** Undo is pinned at its new 26b place, after English-33's last cell, and M1 shows the pin has teeth. M2 shows the test still fails if home's fix regresses.
- **No assertion was deleted, no test was disabled, and the test was renamed to say what it now proves.** The KDoc records the reason.

approved by code-auditor: `GameScreenChromeTest.theTimerIsAStopInComposesReadingOrderBetweenTheTitleAndTheBoardStop` (home-chip-arrival's test, edited in `402e5af`), retargeted to 26b as D-TB amends it under `docs/ux-spec.md`'s D-TB retarget table and talkback defect ruling §3; no assertion weakened.

### 4. Boundaries, security, performance
- The merge's branch-side change has no production diff beyond the conflict hunk, and `402e5af` is test-only. `:domain` has no diff from either side.
- Security: no secret, no network, no new dependency, no input path. `local.properties` was copied only into the throwaway worktree, which is gone.
- Performance: the `draw` lambda allocates only on a size mismatch, which is develop's own behaviour. The branch's per-composition `draw` lambda predates this merge and was reviewed in loops 3 and 4.
- Commits: `a980033`, `402e5af` and `84d4a4f` each end with exactly one trailer, the studio's `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, and have no `Claude-Session` line.

### Findings
No Critical, Major or Minor finding.
- **I-1 (Info, tech-lead).** Develop's `BoardCanvasFirstFrameTest` composes only read-only boards (`:64`, `:85`). After the merge the gameplay canvas gets the frame-geometry fix only because it shares `draw`. No JVM case composes the interactive canvas's first frame or resize. A later edit that gave the playable canvas its own draw block could drop the fix from gameplay with every test green. This is not a defect of this merge, and nothing is owed here. Routed for the tech-lead to decide whether a gameplay case is owed for 3.0.1.
- **I-2 (Info).** The test compilation now gives 47 `w:` lines from `src/test`: 46 v1 `createComposeRule` deprecations and one reflection-API warning. Home's sign-off §3.1 records 43 on home's tree, and this branch's own test files account for the difference. There are 0 warnings from `src/main`. This is recorded as the baseline this tree has, from a full compile; it is not a defect.

## Checks
- `./scripts/check.sh --wait --rerun-tasks --max-workers=4 --console=plain` at `84d4a4f` (code = `402e5af`), finished 03:55 — **pass:**
  - output: `BUILD SUCCESSFUL in 12m 18s`, `104 actionable tasks: 104 executed`, `CHECK_EXIT=0`;
  - JUnit XML: `:app` 1,364 tests in 136 classes; `:domain` 294 in 33; fuzz 1; depthSweep 1; `:build-logic` 55. 0 failures, 0 errors and 0 skipped in each;
  - lint: 64 warnings, 0 errors;
  - 48 `w:` lines: 47 from `src/test` (I-2) and one Kotlin-daemon notice. None from `src/main`. No `e:` line.
- `./scripts/check.sh --wait` at `84d4a4f`, earlier — pass: `BUILD SUCCESSFUL in 11m 12s`, `104 actionable tasks: 11 executed, 93 up-to-date`, exit 0. Superseded as evidence by the `--rerun-tasks` run above.
- The `-U0` changed-line comparisons for `BoardCanvas.kt`, both directions — pass: `IDENTICAL` twice.
- `git show --cc a980033`, `git merge-tree --write-tree --name-only 144153d 305f741`, `git diff --stat e86e7ac a980033` — pass: one conflict; only `BoardCanvas.kt` differs, by the 21 marker and dropped-side lines.
- Brief §12's tripwire lines 1 and 2, and line 1 widened to `-v35` — pass, as quoted in §1.
- The `drawBoard`-to-end-of-file slice, `develop` and `HEAD` — pass: same SHA-1.
- The `a980033` test files on the merged code — fail, as expected: 2 failed of 31 (§2).
- Probes M1–M4 and M2b — each as stated in §2's table. The throwaway worktree was removed with `git worktree remove --force` and `git worktree prune`. `git status --short` in the branch worktree was empty before this report was written.
- Trailers on `a980033`, `402e5af` and `84d4a4f` — pass (§4).
- Not run: a device, an emulator, `.planning/log/`.

## Open issues / risks
- **26b on the device** is QA's to judge in the parallel re-run (ruling §11.3). The gameplay line now reads back → title → timer → board stop → cells → Undo, and the JVM pins hold that order.
- **I-1** goes to the tech-lead. **I-2** is a baseline figure only.
- **Staffing:** this review, like the code, ran on Opus under Q-019 (see `Ran on:`).

## Handoff
Next: producer.
- The tech-lead's sign-off (§11.5) reads this report together with `code-auditor.md`, `code-auditor-loop3-record.md` and `code-auditor-loop4.md`, the conflict hunk (`git show --cc a980033`), and QA's device re-run once it is in.
- I-1 is the tech-lead's to rule on.

VERDICT: PASS
