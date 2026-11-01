# android-game-dev report — talkback-board, loop 4 (fix mode: code-auditor loop 3, findings 1 and 2)
Date: 2026-10-31
Branch: feature/talkback-board (92fd8a0..92fd8a0)
Skills loaded: ergoz-soft:standards (role android-game-dev; the snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, kotlin-android, compose, testing, puzzle-design and play-store in full, plus the product's `.claude/rules/lessons.md`), superpowers:test-driven-development (with its writing-good-tests.md), android-testing, gitflow, compose-skill, compose-ui, android-coroutines
Ran in parallel with: none of my own; another worktree's Gradle daemon was running on this machine
Ran on: claude-opus-5-5 at effort max (the agent file's pin; the harness names no effort)
Fan-out: none
Loops: 4
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- Two tests are added. The change is test-only: no production line changed, and no test proved a defect.
- Each test failed under its auditor mutant, run against the committed test files, and passes with the mutant reverted.
- Finding 1: `TutorialTalkBackTest.whileTheChannelHoldsTheFirstStepsLineItIsTheLastStopInTheReadingOrder` fails under P9.
- Finding 2: `GameScreenTalkBackTest.aJumpActionTakenWhileAnEarlierOnesFocusMoveWaitsCancelsItSoOnlyItsOwnDestinationTakesFocus` fails under P10. It departs from the auditor's prescribed shape, because that shape passed under P10 (see Finding 2 below).
- Check: green at `92fd8a0` (`./scripts/check.sh --max-workers=4`, `BUILD SUCCESSFUL in 11m 28s`, exit 0). `:app` ran 1,327 tests, the auditor's 1,325 plus these two, with 0 failures.

## Outputs
- `app/src/test/java/com/app/pegsolitaire/ui/onboarding/TutorialTalkBackTest.kt` — Finding 1's case and an `everyNode()` helper.
- `app/src/test/java/com/app/pegsolitaire/ui/game/GameScreenTalkBackTest.kt` — Finding 2's case, a `cellFocused()` helper and two board positions (`OPENED_HOLE`, `LEFT_OF_THE_OPENED_HOLE`).
- `.planning/reports/talkback-board/android-game-dev-loop4.md` — this report, committed alone.
- Commit `92fd8a0` `test(talkback): pin the channel's traversal and the latest focus move`, which ends with the one trailer line.

## Finding 1 — the channel is last in the reading order (`BoardAnnouncement.kt:179`)
- **What the case does.** It turns on a touch-exploration service, opens the tutorial and waits for the arm. It asserts that the channel holds the step-1 line. Then it reads the traversal hints Compose writes: exactly one node names the channel as its successor (`traversalBeforeOf(node) == channel.id`), and the channel has no successor.
- **Why both halves.** In ui 1.11.3, Compose puts a node in its order only while that node speaks. `isScreenReaderFocusable` (`AndroidComposeViewAccessibilityDelegateCompat.android.kt:3456`) needs a content description on an unmerged leaf. So "no successor" alone would also pass for a channel left out of the order, and "one stop leads to it" rules that out. Without the index, `LtrBoundsComparator` (`SemanticsSort.kt`, which compares left, then top, then bottom) sorts the 1 px region at the root's top-left corner first.
- **Red under P9** (line 179 deleted), from the JUnit XML:

      whileTheChannelHoldsTheFirstStepsLineItIsTheLastStopInTheReadingOrder
      com.google.common.truth.ComparisonFailureWithFacts: value of    : iterable.size()
      expected    : 1
      but was     : 0
      iterable was: []

  Under P9 the four classes ran 66 cases, and only this one failed.
- **One host.** The dispatch asks for one test per finding. The auditor's fix line suggests one case per host, "or at least the tutorial and Results", so Results and gameplay have no case of their own. All three hosts compose the same `AnnouncementRegion`, so P9 is one line for all of them, and this case kills it. See Open issues.

## Finding 2 — `LatestFocusMove`'s cancel (`BoardCellOverlay.kt:258-265`)
- **What the case does.** It runs on gameplay with touch exploration on and the clock paused, in three steps:
  1. It takes the jump action from row 2, column 4 to row 4, column 4.
  2. After half the focus gap (100 ms), it takes the jump action from row 3, column 2 to row 3, column 4, into the hole the first jump opened.
  3. It traces 30 frames, one at a time. The first destination is never focused, and the second destination is focused at the end.
- **Red under P10** (`job?.cancel()` removed), from the JUnit XML:

      aJumpActionTakenWhileAnEarlierOnesFocusMoveWaitsCancelsItSoOnlyItsOwnDestinationTakesFocus
      com.google.common.truth.AssertionErrorWithFacts: expected not to contain: true
      but was                : [false, false, false, false, false, true, true, true, true, true, true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false]

  Without the cancel, the first move runs out its gap. Focus rests on row 4, column 4 for 7 frames before the second move takes it, so TalkBack would be sent that stop. Under P10 the four classes ran 66 cases, and only this one failed.
- **Departure from the auditor's shape.** The auditor prescribed taking both actions while the window lacks focus (`FocusControlledWindow`), then giving focus back.
  - **Run.** I wrote that shape as a candidate case and ran it under P10. It passed. That run had 26 `GameScreenTalkBackTest` cases with the candidate included, and only the gap case failed.
  - **My reading of why.** Both moves wait on the same window flag, so they resume in the same snapshot apply. Each clears focus and starts an equal 200 ms gap, so both requests land in the same frame. The newer request leaves its destination focused before any frame shows the older one's. So with both moves waiting on window focus, nothing a test or TalkBack can observe here depends on the cancel.
  - **Result.** I deleted the candidate before the commit. The committed case takes the second action during the first move's gap, which is the other wait the cancel covers: "a newer move cancels a waiting one" (ruling §8; `TouchExploration.kt:67`, "A caller's newer move cancels one still waiting"). In this harness, that gap wait is the only one where the cancel changes what TalkBack is sent.
- **No defect.** With the cancel present, the case passes. No production change was needed.

## Checks
All commands ran in `/Users/ermanergoz/Desktop/PegSolitarie-tb`. Each mutant was applied to the production file with `sed -i ''`, run from a cleared `app/build/test-results/testDebugUnitTest`, read from the JUnit XML, and reverted with `git checkout -- app/src/main`.
- `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.onboarding.TutorialTalkBackTest' --max-workers=4 --console=plain -q`, P9 applied: **fail, as intended**. 14 tests, 1 failed (the new case).
- `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.game.GameScreenTalkBackTest' --max-workers=4 --console=plain -q`, P10 applied, with the auditor-shape candidate still in the file: **fail, as intended**. 26 tests, 1 failed (the gap case). The candidate passed, so it was deleted.
- `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.game.GameScreenTalkBackTest' --tests 'com.app.pegsolitaire.ui.onboarding.TutorialTalkBackTest' --tests 'com.app.pegsolitaire.ui.components.BoardSemanticsTest' --tests 'com.app.pegsolitaire.ui.results.ResultsScreenTalkBackTest' --max-workers=4 --console=plain -q`, final test files, P9 applied: **fail, as intended**. 66 tests, 1 failed (Finding 1's case).
  - My first attempt at this run passed the `--tests` flags through a zsh variable, which zsh does not word-split. Gradle rejected the option before any task ran ("Unknown command-line option", `BUILD FAILED in 475ms`, no XML). I repeated it with the flags written out, and that repeat is the run above.
- The same four-class command, P10 applied: **fail, as intended**. 66 tests, 1 failed (Finding 2's case).
- The same four-class command with production code as committed (`git diff --quiet -- app/src/main` true): **pass**. 66 tests, 0 failures, 0 errors, 0 skipped.
- `./gradlew :app:ktlintCheck :app:detekt --max-workers=4 --console=plain -q`: **pass** (exit 0).
- `./scripts/check.sh --max-workers=4 --console=plain` at `92fd8a0` on a clean tree, logged to a file only this session writes. This is the dispatch's command; `--console=plain` only makes the log readable, and the script forwards it to Gradle. **Pass.**
  - It ran from 22:46:56 to 22:58:25 and printed `BUILD SUCCESSFUL in 11m 28s`, `104 actionable tasks: 15 executed, 1 from cache, 88 up-to-date` and `CHECK_EXIT=0`, with 0 `w:` lines.
  - From the JUnit XML: `:app` ran 1,327 tests in 131 classes, and `:domain` 294 tests in 33 classes. There were 0 failures, 0 errors and 0 skipped. Both new cases ran and passed.
  - `:app:testDebugUnitTest`, `:app:lint` and `:domain:koverVerify` ran.
  - These tasks were up to date: `:app:ktlintCheck` and `:app:detekt`, from the standalone run above on the same files; `:domain:test`, `:domain:fuzz`, `:domain:detekt` and `:domain:ktlintCheck`, since `:domain` is unchanged.
  - Writing this report during the run changed nothing the Check reads: `check.sh` runs only `./gradlew check`, and the build reads nothing under `.planning/`.
- `git diff --stat 7c16094..92fd8a0`: the two test files, 58 insertions. `git diff 7c16094..92fd8a0 | grep -nE '^\+.*(@Suppress|baseline|@Ignore|!!)'`: nothing.
- Before the commit, `git status --short` listed only the two test files: every mutant had been reverted.
- Not run: a device, an emulator, `.planning/log/`.

## Open issues / risks
- **Finding 1 covers one host.** Gameplay and Results compose the same region code, so P9 is caught. A host-level regression would not be, such as a host that composes the region inside a group of its own. The auditor may still ask for the Results and gameplay cases its fix line names.
- **Finding 2's shape is a departure, for the auditor to rule on.** In this harness, the cancel's effect while both moves wait on window focus cannot be observed (see Finding 2), so no test pins that path.
- **Effort.** The agent file pins opus at max. The code-auditor's loop-3 report cites a founder directive of 2026-10-31 that sets every role one effort step lower. The harness names no effort for this run.
- **Not in this dispatch, untouched:** the auditor's Info items, the Results rotation inside the arm (for the tech-lead) and the unbounded window-focus wait (for QA).

## Handoff
Next: code-auditor, for loop 4 on `7c16094..92fd8a0`, which touches only the two test files. It needs to re-run P9 and P10 against the two cases and rule on Finding 2's departure from its prescribed shape. QA's device re-run (§11.3) is unaffected, because no production line changed.
