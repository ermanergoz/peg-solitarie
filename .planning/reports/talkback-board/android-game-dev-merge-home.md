# android-game-dev report — talkback-board, merge-in of develop with home-chip-arrival
Date: 2026-11-01
Branch: feature/talkback-board (a980033..402e5af)
Skills loaded: ergoz-soft:standards (role android-game-dev; the snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing in full, plus the product's `.claude/rules/lessons.md`), gitflow, compose-skill, compose-ui, android-testing, superpowers:test-driven-development (with its writing-good-tests.md). **Order, recorded:** compose-skill and compose-ui were loaded after I had resolved and committed the `BoardCanvas.kt` conflict, not before. I re-read the resolution against both afterwards: it adds no state, modifier, theme read or allocation beyond what each side already had, so nothing changed.
Ran in parallel with: colour-vision merge-in, licences R2 fix, candy 15m oracle
Ran on: claude-opus-5-5 at effort max (the agent file's pin; the harness names no effort)
Fan-out: none
Loops: 4 (this merge-in is no review round; the last fix loop was loop 4)
Lessons applied: none (the snapshot holds 0 active lessons; no `.planning/recall/`)

## Summary
- **Merge.** `git merge --no-ff develop` at `305f741`, the merge of `bugfix/home-chip-arrival`, gives `a980033`. There was one conflict, in `BoardCanvas.kt`. Develop's frame-geometry hunk now sits inside this branch's shared `draw` lambda, which both canvases use. Both sides' changed lines are kept exactly (see The merge).
- **B-3 reconciled.** Home's `clearAndSetSemantics` on `TimerPill` made the timer a stop in Compose's order. Two tests that ordered gameplay's stops then went red on the merged tree: 165 tests ran and 2 failed. `402e5af` retargets both, test files only, to 26b as D-TB amends it: title → timer → board stop → row 1's first cell, and English-33's last cell → Undo. No assertion was dropped and two were added. Two mutation probes go red on the new assertions.
- **C11.** No test on this branch drives reduced motion. The only `Settings.Global` writer is develop's `AnimationsEnabledGateWithoutScaleTest`, which writes it deliberately (C11 ruling §2 (c) 3). Nothing moves to `SystemMotionScale`.
- **Check: green at `402e5af`.** `./scripts/check.sh --max-workers=4 --console=plain`, which runs `./gradlew check` under the worktree lock, printed `BUILD SUCCESSFUL in 11m 9s`, exit 0. `:app` ran 1,364 tests and `:domain` 294, with fuzz, depthSweep and `:build-logic` 55: 0 failures, 0 errors, 0 skipped. Lint: 0 errors.
- **QA's device re-run** builds this branch's tip. The code there is `402e5af`'s, because the report commit after it touches only `.planning/`.

## Outputs
- `a980033` `chore(talkback): merge develop`. Parents: `144153d` (loop 4's tip) and `305f741` (`develop`).
- `402e5af` `test(talkback): retarget gameplay's order to title, timer, board stop`. It touches two test files and no production line.
- `.planning/reports/talkback-board/android-game-dev-merge-home.md`, this report, committed alone.
- Each commit ends with exactly one trailer, the studio's `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, and has no `Claude-Session` line. This checkout has no `prepare-commit-msg` hook (no `core.hooksPath`, no hooks in the common git dir), so I wrote the trailer myself, as rules/gitflow.md asks, and did not use the one the harness reminder offered.

## The merge (ruling §11.1, F0, run again now that home has landed)
- **What develop brought** (`git diff --name-only 3ab1cae develop -- . ':!.planning'`):
  - home's `BoardCanvas.kt`, `ElapsedTimeSpeech.kt`, `GameStatusStrip.kt`, the four `ui/home/` files, `HomeScreen.kt`, the v35 theme pair and `strings.xml`;
  - their 11 test files;
  - `scripts/play-upload.sh`.
- **The one conflict, `BoardCanvas.kt`.**
  - Develop added two things. It captures `val geometrySize = canvasSize` in composition, and inside the Canvas's draw block it adds `val frameGeometry = if (geometrySize == size) geometry else BoardGeometry.forCanvas(spec, size)` with `drawBoard(geometry = frameGeometry, …)`.
  - On this branch that draw block had already moved into `val draw: DrawScope.() -> Unit = { … }`. Both the playable canvas (inside the `Box` with `BoardCellOverlay`) and the read-only canvas use it.
  - **Resolution:** I kept this branch's two-canvas structure and put develop's comment and `frameGeometry` line at the top of `draw`. Both canvases then draw with the frame's own geometry, as develop's single canvas did. The read-only path draws exactly what develop draws.
  - **Verified:**
    - the `+`/`-` lines of `git diff 3ab1cae develop -- BoardCanvas.kt` equal those of `git diff 144153d a980033` on the file;
    - the `+`/`-` lines of `git diff 3ab1cae 144153d` equal those of `git diff develop a980033`.
    - So neither side's changes were altered. Patch-ids differ only because the hunk context is now `val draw` and no longer the `Canvas(` call.
  - `git show --cc a980033` shows this hunk and no other.
- **`strings.xml`** was auto-merged by git, without a conflict. Both sides' changed lines are intact, checked the same way.
- **Start path (ruling §11.4, brief §12's tripwire), after the merge:**
  - line 1 prints nothing;
  - line 2 prints only `+            BoardCellOverlay(`, the existing line the ruling allows.
  - `git diff --name-only develop...HEAD -- app/src/main` lists the same 17 files as before the merge, with no start-path file. Home's start-path files reached this branch through `develop`, where its own 15e sitting covers them. The only `ui/components` change the merge made on this branch's side is the hunk above, which is develop's own code.

## The reconciliation of the timer's order (home's B-3)
Defect ruling §3 expected this: "the board stop's predecessor is the strip's last stop: the timer once home has merged, the title before." The spec's D-TB retarget table (`docs/ux-spec.md` §11.1) rules the case: "Any test that counts or orders the gameplay or tutorial screen's TalkBack stops, 26b's included → **Retarget** to 26b as amended". The amended gameplay line is back → title → timer → **board stop → each in-play cell** → [banner] → Undo → Hint → Show moves → Restart.

| Test | Whose | Red on `a980033` (JUnit XML) | Retargeted to | Why |
|---|---|---|---|---|
| `GameScreenTalkBackTest.theBoardStopFollowsTheTitleAndPrecedesTheFirstCell` → `…FollowsTheTimerAndPrecedesTheFirstCell` | this branch | `value of: traversalBeforeOf(...)` / `expected: 2231` / `but was : 2223` (`:468`, the title's next stop is now the timer) | title → timer, timer → board stop (new), board stop → row 1's first cell (unchanged). A `timerNode()` helper finds the clock as home's chrome test does | The ruling's "whichever the tree holds": the timer |
| `GameScreenChromeTest.theTimerIsAStopInComposesReadingOrderBetweenTheTitleAndUndo` → `…BetweenTheTitleAndTheBoardStop` | home's, now on `develop` | `the stop Compose reads after the timer` / `expected: Undo` / `but was : node 854` (`:132`) | clock screen-reader focusable (unchanged); title → timer (unchanged); timer → **board stop** (was Undo); **English-33's last in-play cell (6, 4) → Undo** (new). The KDoc gives the reason | Written on a tree with no board stops. A plain swap of Undo for the board stop would have left Undo unchecked, so Undo is pinned where 26b now puts it |

- The diff's assertion lines (`git show -U0 402e5af | grep -E '^[-+].*(assert|isEqualTo|…)'`): two renames; `traversalBeforeOf(title)` `stop.id` → `timer.id`; and two added lines, `traversalBeforeOf(timer) == stop.id` and the chrome test's last-cell → `UNDO_STOP`. The chrome test's `isEqualTo(UNDO_STOP)` line stays, now under the last-cell message.
- The chrome test's last cell is a literal `Position(row = 6, col = 4)`, hand-checked against `English33Layout` (row 6 holds columns 2–4) and named `ENGLISH_33_LAST_CELL`. I first wrote it as the row-major maximum of the rendered cells. I replaced that before the commit, because writing-good-tests.md prefers hand-derived expectations.
- **Probes**, none committed, each reverted with `git checkout -- app/src/main` and `git diff --quiet` confirmed:
  - **(a) Home's fix undone:** `GameStatusStrip.kt:251` `clearAndSetSemantics` → `semantics`. Both retargeted cases go red. The chrome case fails on `whether the clock's node is screen-reader focusable` / `expected to be true` (`:141`). This branch's case fails at the title → timer hint, `expected: 1865` / `but was : 1873` (`:472`). So both now depend on the merged timer.
  - **(b) Board region without its group:** `GameScreen.kt:244` `isTraversalGroup = true` commented out. This is red in the chrome case (`after the timer` / `expected: the board stop` / `but was : Undo`), in this branch's case (`:473`), and in the existing `onAWindowThatLimitsTheBoardByHeightTheSlabIsOrderedBeforeTheBanner` (`:420`).
  - **(c) Cells read column by column:** `BoardCellOverlay.kt:219` `traversalIndex = (col * 7 + row)`. The board stop still comes first, and only the new Undo pin catches it: `the stop Compose reads after the board's last cell` / `expected: Undo` / `but was : node 48` (`:157`).
- Home's other tests, run on the merged tree, are green with no edit: `GameStatusStripTest` 10, `TimerDigitsTest` 4, `ElapsedTimeSpeechTest` 1, `GameFlowTest` 8, `BoardCanvasFirstFrameTest` 2, `HomeChipArrivalTest` 13, `HomeLayoutTest` 17, `HomeRibbonShapeTest` 12, `HomeStageTest` 10, `WindowThemeApi35FilesTest` 2, `WindowThemeTest` 4, and the chrome test's other 5. The same holds for this branch's `TutorialTalkBackTest` 14, `BoardSemanticsTest` 20, `ResultsScreenTalkBackTest` 7 and `GameScreenWrongTapTest` 10. The tutorial has no timer, so its Skip → board stop pin is unaffected.

## C11 (home's sign-off Open issue 5; its C11 ruling §5 item 1)
- `grep -rn 'ANIMATOR_DURATION_SCALE\|Settings\.Global\|TRANSITION_ANIMATION_SCALE\|WINDOW_ANIMATION_SCALE' app/src/test domain/src/test` on the merged tree matches:
  - `AnimationsEnabledGateWithoutScaleTest.kt:27-29`, the one deliberate writer (C11 §2 (c) 3);
  - KDoc lines only in `SystemMotionScale.kt:16`, `GameScreenSoundTest.kt:364` and `TutorialSoundTest.kt:215`.
- `git grep` at `144153d`, before the merge, gives the same set.
- None of the 13 test files this branch had added or edited before the merge (`git diff --name-only 3ab1cae 144153d -- app/src/test`) mentions `SystemMotionScale`, `effectContext`, `MotionDurationScale`, `DurationScale`, `ValueAnimator` or `animator`. No test here drives reduced motion, so none is vacuous and none is moved.

## Checks
All in `/Users/ermanergoz/Desktop/PegSolitarie-tb`. Test counts come from the JUnit XML, read after `rm -rf app/build/test-results/testDebugUnitTest`.
- `git merge-tree --write-tree --name-only develop HEAD` at `144153d` — read: one conflict, `BoardCanvas.kt`.
- `git merge --no-ff --no-commit develop`, resolution, `git commit` — pass: `a980033`, parents `144153d 305f741`; no conflict marker left (`grep -c` 0).
- The `+`/`-` line comparisons for `BoardCanvas.kt` and `strings.xml` (The merge) — pass: identical on both sides.
- `./gradlew :app:testDebugUnitTest --max-workers=4 --console=plain -q` with `--tests` for the 17 classes named above, at `a980033` — **fail, as expected:** 165 tests, 2 failed, the two in the table. That is the red before the retarget.
- The same command for `GameScreenChromeTest` and `GameScreenTalkBackTest` at `402e5af`'s files — pass: 6/6 and 25/25.
- Probes (a), (b), (c), the same two-class command (one class for (c)) — **fail, as intended**, as quoted above; `app/src/main` clean after each.
- `./gradlew :app:ktlintCheck :app:detekt --max-workers=4 --console=plain -q` at `402e5af`'s files — pass, exit 0. My first try put `--tests` on the same command line as `detekt`. Gradle refused it before any task ran ("Unknown command-line option '--tests'", `BUILD FAILED in 1s`), and I re-ran the two steps separately.
- Brief §12's two tripwire lines, `git diff --name-only develop...HEAD -- app/src/main` — as quoted under The merge.
- `git show 402e5af | grep -nE '^\+.*(@Suppress|@Ignore|baseline|!!)'` — pass: nothing. No suppression, baseline or `@Ignore` added anywhere.
- **`./scripts/check.sh --max-workers=4 --console=plain` at `402e5af` — pass.** The script runs `"$root/gradlew" check` with these arguments under `.gradle/ergoz-build.lock` (`scripts/check.sh:71`), so this is the CLAUDE.md Check command. Here is the run:
  - **Gate.** It was started through a scratchpad gate that waits for the 1-minute load to fall to 6 on this 10-core host, which other worktrees' builds share. The gate opened after 345 s at 5.85. The first gated start was stopped while still at the gate, before any Gradle run, so that a test edit would not race it.
  - **Run.** It ran 03:18:32–03:29:41 on a clean tree (`git status --porcelain -- . ':!.planning'` empty) and printed `BUILD SUCCESSFUL in 11m 9s`, `104 actionable tasks: 21 executed, 83 up-to-date` and `CHECK_EXIT=0`.
  - **Executed:** `:app:testDebugUnitTest` (a full run: 136 classes, 1,364 tests, XML written 03:21:49), `:app:lint` (`lint-results-debug.xml`: 64 warnings, 0 errors), the literal checks of both modules, `:domain:depthSweep` (1/1), `:domain:koverVerify`, `:app:kspDebugKotlin` and `:app:kspDebugUnitTestKotlin`.
  - **Up to date, by Gradle's input hashes:**
    - `:domain:test` (294) and `:domain:fuzz` (1), because `:domain` has no diff in the merge (`git diff --name-only 3ab1cae develop` lists no `domain/` path);
    - `:build-logic` (55);
    - `:app:ktlintCheck` and `:app:detekt`, from the standalone pass above on the same files;
    - `:app:compileDebugKotlin` and `:app:compileDebugUnitTestKotlin`, from my targeted runs on the same sources.
  - **All XML:** 0 failures, 0 errors, 0 skipped.
  - **Warnings.** The log has 0 `w:` and 0 `e:` lines, because both compilations were up to date. So I claim no test-compilation `w:` figure (Open issue 3). `src/main` compiles with `allWarningsAsErrors` (`build-logic/.../android-app.gradle.kts:71`), so a main-source warning would have failed the targeted runs.
- Not run: a device, an emulator, `.planning/log/`.

## Open issues / risks
1. **A home test is edited on this branch.** `GameScreenChromeTest` came from `develop`. Brief §12 says an existing test changes only when the report names it with the reason and the auditor approves it. The reason is the spec's D-TB retarget table and defect ruling §3. The auditor rules on it. Its assertions are kept or strengthened, and none is dropped.
2. **26b on the device.** The gameplay order to read is now back → title → timer → board stop → cells → Undo, timer included. Defect ruling §11.3's precondition (`develop` with home merged) is met, so QA judges the timer's position and value as this build's, not as home's.
3. **The `w:` count is not measured here.** The Check's compilations were up to date, so its log carries no `w:` line. Under home sign-off §3.1 only a full `compileDebugUnitTestKotlin --rerun` gives that figure. The auditor's own `--rerun-tasks` Check would.
4. **Skills order** (header): compose-skill and compose-ui came after the conflict edit.
5. **Not in this dispatch, untouched:** loop 4's open items for the auditor (Finding 1 covers one host; Finding 2's departed shape), and §10's routed rows.

## Handoff
Next: code-auditor — review `a980033`'s one hand-resolved hunk (`git show --cc a980033`) and `402e5af` (two test files). Rule on the home test edit (Open issue 1), and re-run probes (a)–(c) if wanted.
Then: qa-engineer — defect ruling §11.3's device re-run on the debug APK built at this branch's tip (code = `402e5af`), checked by sha256. 26b's gameplay line now includes the timer before the board stop.
Then: tech-lead — the sign-off (§11.5), which reads this report and `a980033`'s conflict hunk.
