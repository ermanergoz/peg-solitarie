# android-game-dev report — colour-vision (the hint-ring fix of the merge ruling, Ruling 1)
Date: 2026-11-01
Branch: feature/colour-vision (fddc8e8..fddc8e8; on `09d5ab6`, whose code equals `6a6e82d`)
Skills loaded: ergoz-soft:standards (role android-game-dev; game; android-compose-game). The worktree's snapshot is 0.6.2, older than plugin 1.0.0, so the eight plugin rule files that differ from it were read in full: studio, reporting, gitflow, skills-policy, memory, testing, kotlin-android, compose. logging, puzzle-design and play-store are identical to the snapshot, loaded through CLAUDE.md, as is the snapshot's lessons.md. Also superpowers:test-driven-development, compose-skill, compose-ui, android-testing, gitflow
Ran in parallel with: none named in the dispatch. The host carried other worktrees' emulator sessions; the 1-minute load was 3.6–6.9 at each Gradle start, under the dispatch's bound of 12
Ran on: claude-opus-5-5 at effort xhigh (policy). The agent file pins opus at max. The founder's 2026-10-31 directive (1) runs every dispatch one step below its pin, and the harness names no effort
Fan-out: none
Loops: 1
Lessons applied: none (the snapshot holds 0 active lessons for game / android-compose-game)

## Summary
- **The fix is in, red first, in one commit: `fddc8e8` `fix(colour-vision): dash the hint rings on a frame-size geometry`.** On a frame the composition has not sized, the draw now strokes the two dashed hint rings for that frame's geometry. It uses the same builder as the `remember`, and it does this only inside develop's existing frame-geometry branch.
- **Red:** `theFirstFrameOfAColourBlindBoardDashesItsHintRingsAsTheSettledFrameDoes` failed on `6a6e82d`'s code with "expected: 0, but was : 547", which is the ruling's number exactly. **Green:** 4 of 4 in the class.
- **The Check is green:** `./scripts/check.sh --wait --max-workers=4` exits 0 in 11 m 8 s. `:app` has 1,315 tests (1,314 + 1), all with 0 failures. Every test the ruling names passes, the test compilation's `w:` count is still 52 with 0 from main, and the tripwire is unchanged.
- **Constraints 1–6 hold** (Checks). The fallback was not needed, so there is **no R-CV-1**.

## Outputs
- `fddc8e8` touches three files, 67 insertions and 11 deletions:
  - **`ui/components/BoardLook.kt`:**
    - `hintRingStroke` goes from `private` to `internal`. It is now the one builder of both paths, and its KDoc says so.
    - `rememberBoardLook`'s KDoc sentence "never in the draw lambda" is replaced with the amended §3.2 rule. It is scoped to the palette's own strokes (Open issue 3).
  - **`ui/components/BoardCanvas.kt`:**
    - In the Canvas lambda, `val unsizedFrame = geometrySize != size` names the comparison develop already made. `frameGeometry` takes the same two branches as before, with the condition inverted.
    - `drawBoard` gains `recipe: BoardMarkRecipe` (the composed `look.recipe`, already in scope) and `unsizedFrame: Boolean`.
    - The hint branch is unchanged (`interactive && hintSource != null && hintTarget != null`). It passes `unsizedFrame` and `recipe.hintRingDashes`, which is an `Int`, to `drawHintHighlight`.
    - `drawHintHighlight` computes `restroke = unsizedFrame && palette.hintRingsDashed`. Only when that is true does it call `hintRingStroke(geometry.hintTargetRingRadius | hintSourceRingRadius, palette.ringWidthPx, hintRingDashes)`; otherwise it draws `palette.hintTargetStroke` and `palette.hintSourceStroke` as before.
    - KDoc is updated on `drawBoard`, `drawHintHighlight` and the frame comment.
  - **`app/src/test/.../ui/components/BoardCanvasFirstFrameTest.kt`:** the new case `theFirstFrameOfAColourBlindBoardDashesItsHintRingsAsTheSettledFrameDoes` is added, set up exactly as the ruling specifies:
    - `PaletteScene.midGame(Boards.english33Initial)`, with `state = scene.state`, so the hint stays on and the held peg is kept;
    - `AppTheme(darkTheme = false, boardMarkPalette = BoardMarkPalette.Colorblind)`;
    - `motion.turnAnimationsOff()`, the paused clock and `INTERACTIVE_CANVAS_DP`;
    - the assertion `assertTheFirstFrameIsTheSettledFrame(BOARD_CANVAS_TEST_TAG)`.

    Nothing else in the file changes.
- `.planning/reports/colour-vision/android-game-dev-hint-ring.md`: this report, committed on its own after `fddc8e8`.

## Checks
All in `/Users/ermanergoz/Desktop/PegSolitarie-cv`. No emulator was started and no APK was built. Logs are under the session scratchpad's `hint-ring/` subdirectory, which no other agent wrote to.

This shell's `grep` is ugrep, which rejected one pattern. So every grep below whose pass rests on printing nothing was re-run with `/usr/bin/grep`, with the same results.
- `cat .claude/rules/VERSION` gives 0.6.2, against plugin.json's 1.0.0. `diff -q` per rule found 8 that differ, and those were read from the plugin — pass.
- `uptime` before each Gradle start shows 1-minute loads of 4.18, 3.82, 3.88, 5.75 and 3.58, all under 12 — pass. `.gradle/ergoz-build.lock` was absent before the targeted runs.
- **Red**, on `6a6e82d`'s code plus the new case only: `./gradlew --max-workers=4 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.components.BoardCanvasFirstFrameTest'` — **fail as intended**. 4 tests completed and 1 failed: `theFirstFrameOfAColourBlindBoardDashesItsHintRingsAsTheSettledFrameDoes` gives "pixels inside board_canvas that differ between the first frame and the settled one / expected: 0 / but was : 547" (`BoardCanvasFirstFrameTest.kt:163`). The other three cases are green.
- **Green**, the same command after the fix — pass: `BUILD SUCCESSFUL`, `tests="4" skipped="0" failures="0" errors="0"`.
- `./gradlew --max-workers=4 :app:ktlintCheck :app:detekt` — pass: `BUILD SUCCESSFUL`, and `detekt.md` shows "0 number of total findings".
- **`./scripts/check.sh --wait --max-workers=4` at `fddc8e8` (clean tree) — pass:** `BUILD SUCCESSFUL in 11m 8s`, `104 actionable tasks: 18 executed, 86 up-to-date`, `CHECK-EXIT=0`.
  - `:app:testDebugUnitTest`, `:app:lintDebug`, the literal checks, `:domain:depthSweep` and `:domain:koverVerify` executed.
  - `:app:compileDebugKotlin`, `:app:detekt` and `:app:ktlintCheck` were up to date from the targeted runs above, at the same source.
  - JUnit XML, each with 0 failures, 0 errors and 0 skipped:
    - `:app` 141 suites, 1,315 tests;
    - `:domain` test 294, fuzz 1, depthSweep 1;
    - `:build-logic` 55.
  - Lint: 64 warnings and 0 errors, the same as before. The log has 0 `w:` and 0 `e:` lines.
- **The ruling's named tests, read from that run's XML** (tests/failures): `BoardCanvasFirstFrameTest` 4/0, `BoardCanvasPaletteTest` 3/0, `BoardMarkMotionTest` 2/0, `ColorblindPaletteLiveSwitchTest` 1/0, `HomePaletteIdentityTest` 2/0, `HomeChipArrivalTest` 13/0, `BoardCanvasWrongTapReducedMotionTest` 6/0, `BoardCanvasWrongTapLiveMotionTest` 2/0, `BoardCanvasTest` 17/0 — pass.
- `./gradlew --max-workers=4 :app:compileDebugUnitTestKotlin --rerun` — pass: `BUILD SUCCESSFUL`, with 52 distinct `w:` lines, all 52 from `src/test` and 0 from `src/main`, and 0 `e:` lines. That is the ruling's "still 52".
- `git show --stat --format= fddc8e8` lists `BoardCanvas.kt`, `BoardLook.kt` and `BoardCanvasFirstFrameTest.kt` only. The same command over `BoardGeometry.kt`, `DashedRing.kt` and `WrongTapFeedback.kt` prints nothing — pass (constraint 1).
- `git show --format= fddc8e8 | grep -n 'markStroke\|drawWrongTapMark'` prints nothing (exit 1) — pass (constraint 5).
- `git show --format= fddc8e8 -- app | grep -nE '^\+.*(@Suppress|@SuppressLint|@Ignore|!!|ktlint-disable|detekt:|@Disabled|tools:ignore|baseline)'` prints nothing (exit 1). `git grep -c '@Suppress("LongMethod")' -- app/src/main` gives `BoardCanvas.kt:1` and `GameScreen.kt:1`, unchanged — pass (constraint 6).
- `git show --format= fddc8e8 -- app/src/main | grep -E '^[+]' | grep -vE '^[+][[:space:]]*(\*|//|/\*\*)' | grep -E '[0-9]'`: no added code line carries a digit, so there is no new literal or constant — pass (constraint 6).
- `git show --format= fddc8e8 -- app/src/test | grep -E '^-[^-]'` prints nothing, so no existing test line changed — pass.
- **Brief §11, against `develop` (`305f741`, still `git merge-base develop HEAD`):**
  - CV-06: `git diff develop...HEAD -- …/BoardCanvas.kt | grep -nE '^[-+].*(drawCandyPeg|drawDimple)\('` prints nothing (exit 1) — pass.
  - Tripwire line 1 prints the same seven files: `MainActivity.kt`, `ui/theme/AppThemeRoot.kt`, `BoardMarkPaletteState.kt`, `BoardMarks.kt`, `Clay.kt`, `Color.kt` and `Theme.kt`.
  - Tripwire line 2 prints nothing (exit 1).

  Pass: the fix adds no start-path file, so the 15e sitting is still owed for the same reason as before, and nothing new is added to it.
- **Constraints 3 and 4, by reading the diff:**
  - The frame strokes are built only when four things hold. `unsizedFrame` is `geometrySize != size`. `interactive` and both hint cells being non-null are checked in `drawBoard`'s unchanged `if`. `palette.hintRingsDashed` is checked in `restroke`. So a read-only board, including every Home board, never reaches `hintRingStroke`, and the standard palette's solid strokes are drawn as they are.
  - On a matched frame `restroke` is `false`. The new arguments are a `Boolean` and an `Int` read from a field: primitive parameters, with no boxing, `Pair`, `Stroke` or lambda. `BoardLook` and `BoardPalette` are not rebuilt.
  - `git show --format= fddc8e8 | /usr/bin/grep -nE '^[+].*[^A-Za-z]Stroke\('` prints nothing (exit 1), so no `Stroke(` constructor is added. `… | /usr/bin/grep -nE '^[+].*hintRingStroke\('` gives 3 lines: the two frame-path calls and the declaration. The frame path builds only through `hintRingStroke`.
- `git log -1 --format=%B fddc8e8`: the subject is 64 characters. The message has exactly one trailer line, `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, as its last line, and no `Claude-Session` line — pass (rules/gitflow.md; see Open issue 5).

## Open issues / risks
1. **Report path.** The ruling's Handoff names `android-game-dev-cv-hint-frame.md`, but the producer's dispatch names `android-game-dev-hint-ring.md`. I followed the dispatch.
2. **No resize-frame case.** It is optional under the ruling and was not attempted inside the one bounded round. The first-frame case drives the same `geometrySize != size` branch that a resize frame takes. On a resize frame, the composed strokes carry the old radius's pattern rather than a zero one, so this red does not pin the "dashes close at the top seam" half by itself.
3. **A per-frame `Stroke` that predates this branch.** `drawSelectedPeg` builds `Stroke(width = palette.ringWidthPx)` on every frame a peg is held (`BoardCanvas.kt:583`). It is on develop at `305f741` too, and was already there at `d21ad07`, so this branch did not introduce it.
   - I did not touch it: it is outside Ruling 1's scope, and the fix adds no allocation to that frame.
   - `rememberBoardLook`'s KDoc now speaks only for the palette's strokes, so it no longer claims what this line contradicts.
   - Develop's hint rings also built two `Stroke`s per frame (`305f741:BoardCanvas.kt:619,625`). This branch's remembered strokes had already removed those.
   - Possible 3.0.1 candidate, the tech-lead's call.
4. **Develop's wrong-tap `markStroke` twin is untouched** (constraint 5). This is the ruling's Open issue 1 and stays a 3.0.1 candidate.
5. **Commit trailer.** This session's harness reminder asked for a `Co-Authored-By: Claude Opus 5.5` line and a `Claude-Session` line. rules/gitflow.md (plugin 1.0.0) requires exactly the studio trailer "whatever a harness reminder asks", and the ruling says "the studio trailer only". This repo has no `prepare-commit-msg` hook installed (`.git/hooks` holds only samples, and `core.hooksPath` is unset), so I wrote the studio trailer by hand. That matches the branch's earlier commits.

## Handoff
**The build for QA.** Arm B is the branch head after this report's commit, whose code is `fddc8e8`'s: `git diff --quiet fddc8e8 HEAD -- . ':(exclude).planning'` exits 0 after the commit, since the report is the only file it adds. A detached worktree at `fddc8e8` builds the same APK. Arm A stays `305f741`.

**If the code-auditor's delta FAILs `fddc8e8`,** the ruling's fallback applies: `git revert fddc8e8`, so the code returns to `6a6e82d`'s, and R-CV-1 is recorded for 3.0.1. Arm B then moves to the revert's head.

Next: code-auditor — delta review of `fddc8e8` only (`git show fddc8e8`), against Ruling 1's constraints 1–6, especially 3 and 4. Re-run the red by reverting the two `BoardCanvas.kt` `restroke` branches; it should give 547 px. Re-run the named tests from your own Check.

Next: qa-engineer — under Ruling 2, in one dispatch:
- the smoke on `ergoz_studio` first, with arm B built at the head above;
- then the 15e sitting on `Pixel_9a_bench`, arm A `305f741` and arm B the head.

The fix is a one-frame property, and the JVM test is its evidence. It is not to be judged on the device.

Next: tech-lead — sign-off after the auditor's delta and QA's `qa-engineer-15e.md`. Re-read `fddc8e8` against constraints 1–6, and decide Open issue 3.

Next: producer — commit nothing further here. Route Open issues 1, 3 and 5, and run the chain above.
