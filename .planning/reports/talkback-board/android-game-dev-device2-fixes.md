# android-game-dev report — talkback-board, device-2 fixes (F-A, then F-B1)
Date: 2026-11-01
Branch: feature/talkback-board (ee5cf08..fc9b6c3). My two commits are `ee5cf08` and `fc9b6c3`. The range `1269990..fc9b6c3` also holds the ux-designer's `abc9b4a` (D-TBe, docs only), which landed between them.
Skills loaded: ergoz-soft:standards (role android-game-dev; the snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies of studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose and testing in full; logging, puzzle-design and play-store are identical to the snapshot; plus the product's `.claude/rules/lessons.md`), superpowers:test-driven-development, gitflow, compose-skill, compose-ui, android-testing, android-coroutines
Ran in parallel with: the ux-designer's D-TBe amendment in this worktree (`abc9b4a`; I staged only my own paths), and QA's 15e cold-start sitting on `Pixel_9a_bench` (after the producer's message, I ran no Gradle until that AVD was gone)
Ran on: claude-opus-5-5 at effort max (the agent file's pin; the harness names no effort)
Fan-out: none
Loops: 5
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- **F-A** (`ee5cf08`, `fix(talkback): speak the first step's caption alone on opening`). On opening, the tutorial now speaks only "Tap a peg, then tap the empty hole two cells away." Steps 2 and 3 keep the full step line. No string or `BoardSpeechFormat` change.
- **F-B1** (`fc9b6c3`). Applies only while touch exploration is on:
  - The jump that clears step 1 or step 2 puts no line.
  - On steps 2 and 3, focus moves to the new board stop first. The step line follows `TutorialAnim.STEP_LINE_AFTER_FOCUS_MS = 500L` later.
  - With touch exploration off, nothing changes.
- Only `TutorialAnnouncements.kt` and `TutorialAnim.kt` change under `app/src/main`, so the fallback (§6) did not trigger.
- Every new or retargeted test was red first, for the expected reason. The auditor's probes P1–P4, plus two more of mine (P5, P6), each turn at least one case red.
- **Check: green** at `fc9b6c3` (`./scripts/check.sh --wait --max-workers=4`, `BUILD SUCCESSFUL in 11m 7s`, `CHECK_EXIT=0`).

## Outputs
- `app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialAnnouncements.kt`:
  - F-A: `StepAnnouncements`'s first-composition branch puts `stringResource(state.captionRes)`, resolved in composition.
  - F-B1, new `TutorialJumpAnnouncements`: the tutorial's own jump source. It skips the line when touch exploration is on and the jump clears a step that is not the last (`clearsAStepBeforeTheLast`: one peg left and `stepNumber < STEP_COUNT`, which is the ruling's `stepIndex < STEP_COUNT - 1`).
  - F-B1, `StepAnnouncements` on a replacing step: `moveInputFocusTo`, then `delay(STEP_LINE_AFTER_FOCUS_MS)`, then `announce`.
  - The shared `JumpAnnouncements` (`ui/components/BoardAnnouncement.kt`) is untouched, and gameplay still uses it.
- `app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialAnim.kt` — `STEP_LINE_AFTER_FOCUS_MS = 500L`. Its KDoc cites the ruling, ui 1.11.3 `:275`, and QA's ~170 ms round trip (`speech-raw.log` 04:00:38.224).
- `app/src/test/java/com/app/pegsolitaire/ui/onboarding/TutorialTalkBackTest.kt`:
  - two new cases, (a) and (b);
  - the assertion added for (c);
  - six retargets (listed under Loop 5);
  - helpers `advanceToTheNextStepAndItsLine()` and `advanceAFrameAndTheLooper()`;
  - line constants `T1_JUMP_LINE`, `T3_FIRST_JUMP_LINE` and `T3_COMPLETING_JUMP_LINE`.
- `app/src/test/java/com/app/pegsolitaire/testing/AccessibilityServices.kt` — three test helpers: `eventsSent()`, `AccessibilityEvent.isFocusOf(node)` and `AccessibilityEvent.isDescriptionChangeTo(line)`.
  - `isFocusOf` reads the virtual node from Robolectric's public `ShadowAccessibilityRecord.getVirtualDescendantId()`, with no reflection. The host view's own `VIEW_FOCUSED` is never matched.
  - No existing helper changed.
- `.planning/reports/talkback-board/android-game-dev-device2-fixes.md` — this report, committed alone.

## Loop 5 — every new and retargeted test (all in `TutorialTalkBackTest`)
**New:**
- **(a) `afterAStepSwapTheBoardStopsFocusIsSentBeforeTheStepLine`**, run as the ruling sets out. It turns a service on and marks the sent-event count after T1's jump. On a paused clock it then idles past the beat, and steps 58 frames, idling the main looper per frame. The 58 frames cover the focus gap, `STEP_LINE_AFTER_FOCUS_MS` and two service batches. Finally it asserts that the board stop's `TYPE_VIEW_FOCUSED` exists, the `STEP_TWO_LINE` description change exists, and the focus comes first.
- **(b) `underTouchExplorationAStepsClearingJumpPutsNoLineOfItsOwn`:**
  - after T1's and T2's clearing jumps and their swaps, `descriptionChangesSent()` holds neither `T1_JUMP_LINE` nor `T2_JUMP_LINE`;
  - T3's first jump (4 pegs left) and its completing jump are both sent.

**(c), changed:**
- `withTouchExplorationOffAStepChangeMovesNoFocus` gained one line, `assertChannel(T1_JUMP_LINE)`, after the jump. The rest is as it was.

**Retargeted:** F-A's three, the ruling's two, and two that F-B1's later line forced.
1. `theFirstStepsLineIsSpokenOnOpeningAndFocusIsNotMoved` (F-A): the expected line is now the caption alone.
2. `STEP_ONE_LINE` (F-A) is now the caption alone. This retargets two cases with it:
   - `theFirstStepsLineIsHeldUntilTheChannelIsArmedAndIsThenSent`;
   - `whileTheChannelHoldsTheFirstStepsLineItIsTheLastStopInTheReadingOrder`.
3. `clearingAStepSpeaksTheNextStepsLineAndMovesFocusToItsBoardStop` (ruling): it settles the focus move and asserts the board stop is focused. It then asserts the channel does **not** yet hold `STEP_TWO_LINE`. Finally it advances `STEP_LINE_AFTER_FOCUS_MS + FRAME_MS` and asserts the channel holds it.
4. `everyLineAfterTheFirstStepSwapIsSentToTheService` (ruling): `T2_JUMP_LINE` is replaced by `T3_FIRST_JUMP_LINE` in the in-order list. One `playTheOnlyMove` + `letTheServiceHear()` was added for T3's first jump, so that line is sent before the next jump replaces it.
5. `aJumpActionClearsTheFirstStepAndAdvancesItAsTheTwoTapsDo` (forced): `idlePastTheStepBeat(); waitForIdle()` became `advanceToTheNextStepAndItsLine()`. The step-2 line now comes about 700 ms after the swap, so asserting it at the beat failed. That run is the first post-F-B1 run under Checks.
6. `onStepTwoAServiceIsHandedTheChannelAsAPoliteLeafWithTheStepLine` (forced): the same change, for the same reason.

The other cases are unchanged.

## Checks
All commands ran in `/Users/ermanergoz/Desktop/PegSolitarie-tb`. Results come from the JUnit XML, with the test-results directory cleared before each run.

**Host.**
- Before every Gradle run I checked `uptime`: the 1-minute load was between 1.75 and 5.94, always under 12.
- After the producer's message I also checked `adb devices` and `adb -s <serial> emu avd name`. `Pixel_9a_bench` (emulator-5584) was running from 05:23 to 05:29. I polled every 120 s and ran no Gradle until it was gone (05:31:46).

**F-A, red.** Test retargets only, then `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.onboarding.TutorialTalkBackTest' --max-workers=4 --console=plain -q` — **fail, as intended** (14 tests, 3 failed):

    theFirstStepsLineIsSpokenOnOpeningAndFocusIsNotMoved
      expected: Tap a peg, then tap the empty hole two cells away.
      but was : Step 1 of 3. Tap a peg, then tap the empty hole two cells away.
    theFirstStepsLineIsHeldUntilTheChannelIsArmedAndIsThenSent       (same values, on channelText())
    whileTheChannelHoldsTheFirstStepsLineItIsTheLastStopInTheReadingOrder   (same values)

**F-A, green.**
- `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.onboarding.*' --max-workers=4 --console=plain -q` — **pass** (8 classes, 53 tests, 0 failures).
- `./gradlew :app:ktlintCheck :app:detekt --max-workers=4 --console=plain -q` — **pass** (exit 0).
- Committed as `ee5cf08`.

**F-B1, red.** The tests above, plus the constant `STEP_LINE_AFTER_FOCUS_MS` declared first in `TutorialAnim` so that the reds compile. Nothing read the constant yet. Same `TutorialTalkBackTest` command — **fail, as intended** (16 tests, 3 failed):

    afterAStepSwapTheBoardStopsFocusIsSentBeforeTheStepLine                 (a)
      the board stop's focus, before the step line
      expected to be less than: 15
      but was                 : 19
    underTouchExplorationAStepsClearingJumpPutsNoLineOfItsOwn               (b)
      expected not to contain: Row 1, column 1 to row 1, column 3. Removed row 1, column 2. 1 peg left.
      but was: [… Row 1, column 1 to row 1, column 3. Removed row 1, column 2. 1 peg left., Board, 1 peg left, … Step 2 of 3. Jumps work up, …, Board, 2 pegs left]
    clearingAStepSpeaksTheNextStepsLineAndMovesFocusToItsBoardStop          (retarget 3)
      value of          : channelText()
      expected not to be: Step 2 of 3. Jumps work up, down, left and right — never diagonally.

In the same run:
- (c)'s new line passed, as it must: its jump line is present today.
- Retarget 4 passed with `T3_FIRST_JUMP_LINE`, which confirms the computed T3 line.

**F-B1, first green attempt.** The onboarding command above — **fail** (55 tests, 2 failed). This was the timing of retargets 5 and 6, not a defect:

    aJumpActionClearsTheFirstStepAndAdvancesItAsTheTwoTapsDo
      expected: Step 2 of 3. Jumps work up, down, left and right — never diagonally.
      but was : Tap a peg, then tap the empty hole two cells away.
    onStepTwoAServiceIsHandedTheChannelAsAPoliteLeafWithTheStepLine
      expected: Step 2 of 3. Jumps work up, down, left and right — never diagonally.
      but was : null

After retargets 5 and 6, the same command — **pass** (8 classes, 55 tests, 0 failures).

**A candidate test, dropped.** I wrote `aStepLineStillWaitingWhenTheTutorialCompletesIsNeverPutOnAfterTheDoneLine`. It asked whether a step-3 line still waiting at completion could land after the done line.
- It failed (`expected: That's the whole game. …`, `but was: null`), but only because the done line's 1 s hold had expired.
- A temporary clock trace, never committed, showed why. Every T3 move under the test clock's auto-advance costs about 1,072 ms (`t=3824 → 4896 → 5968 → 7040 → 8112`), so step 3's line is long out before completion.
- So the harness cannot reach that edge, and a player cannot either. No guard was added, because no test failed for it. The test and its trace lines were removed. See Open issues.

**After that.** `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.onboarding.*' :app:ktlintCheck :app:detekt --max-workers=4 --console=plain -q` — **pass** (55 tests, 0 failures; ktlint and detekt clean).

**Probes on the uncommitted F-B1.** Each mutant was applied to `TutorialAnnouncements.kt` by a scratchpad script, which then ran the `TutorialTalkBackTest` command and copied the file back. Afterwards, `cmp` against a saved copy showed the file identical.

| Probe | Mutant | Result |
|---|---|---|
| P1 | line before the move | **red**, 4 failed: (a) `expected to be less than: 15 / but was: 19`; retarget 3 (`expected not to be: Step 2 of 3. …`); retargets 5 and 6 (`but was: null`, the early line had expired) |
| P2 | clearing jump's line restored (`if (now != null)`) | **red**, 1 failed: (b) `expected not to contain: Row 1, column 1 to row 1, column 3. Removed row 1, column 2. 1 peg left.` |
| P3 | F-A reverted (opening speaks `format.stepLine(…)`) | **red**, 3 failed: the three opening cases, `but was: Step 1 of 3. Tap a peg, …` |
| P4 | `delay(0L)` | **red**, 4 failed: (a) `expected to be less than: 19 / but was: 20`; retarget 3; retargets 5 and 6 |
| P5 | jump skip without the touch-exploration gate | **red**, 1 failed: (c) `expected: Row 1, column 1 to row 1, column 3. … / but was: Selected row 1, column 1, 1 jump.` |
| P6 | jump skip without `stepNumber < STEP_COUNT` | **red**, 1 failed: (b) `expected to contain: Row 3, column 3 to row 1, column 3. Removed row 2, column 3. 1 peg left.` |

Under P4, (a) catches the mutant by one event: with no delay, the line's change is sent in the same batch, just ahead of the focus (19 against 20). Retarget 3 catches P4 on time alone, since the line must not be on the channel when focus lands.

**Commit.** F-B1 committed as `fc9b6c3`, four files.

**Gates the ruling and the brief name.**
- `git diff --name-only 6a98510..HEAD -- app/src/main | sort` lists only `ui/onboarding/TutorialAnim.kt` and `ui/onboarding/TutorialAnnouncements.kt`.
- Brief §12 tripwire line 1 printed nothing (grep exit 1).
- Tripwire line 2 printed only the existing `+            BoardCellOverlay(` line. `git diff --name-only 1269990..HEAD -- …/BoardCanvas.kt` is empty, so this round did not touch that file.
- `git diff 1269990..HEAD -- app | grep -nE '^\+.*(@Suppress|baseline|@Ignore|!!)'` printed nothing (exit 1). The same grep over the whole range hits only the ux-designer's `docs/ux-spec.md` text ("`@Ignore`d").

**The Check.** `./scripts/check.sh --wait --max-workers=4 --console=plain`, at `fc9b6c3` on a clean tree, logged to a scratchpad file. The dispatch's command; `--console=plain` only makes the log readable. Run with no emulator up and a 1-minute load of 5.55. **Pass.**
- It ran from 05:34:03 to 05:45:10 and printed `BUILD SUCCESSFUL in 11m 7s`, `104 actionable tasks: 22 executed, 7 from cache, 75 up-to-date` and `CHECK_EXIT=0`, with 0 `w:` lines.
- From the JUnit XML:
  - `:app` ran 1,366 tests in 136 classes, with 0 failures, 0 errors and 0 skipped. `TutorialTalkBackTest` ran 16 of 16 green.
  - `:domain`: 296 tests in 35 classes, 0 failures. Its `test` and `fuzz` came from the cache, since `:domain` is unchanged.
- `:app:testDebugUnitTest`, `:app:lint` and `:domain:koverVerify` ran. `:app:ktlintCheck` and `:app:detekt` were up to date from the standalone run on the same files.

**Not run:** a device, an emulator, `.planning/log/`.

## Open issues / risks
- **A step line still waiting at completion is not guarded.** Step 3's effect is keyed on `stepIndex`, which completion does not change. If T3 were finished within about 700 ms of step 3's install (plus any wait for window focus), its board-stop move and its line could follow the done line.
  - A player needs four moves, eight activations, inside that time, so it cannot happen. The JVM harness cannot reach it either (see Checks).
  - If the tech-lead wants it closed anyway, the fix is `LaunchedEffect(state.stepIndex, state.completed)`, inside `TutorialAnnouncements.kt`. A test would have to drive T3 on a paused clock.
- **P4's margin in (a) is one event.** It depends on ui 1.11.3 sending the channel's description change ahead of the focus in a shared batch. Retarget 3 covers P4 independently.
- **On the device, from the ruling, for QA's device-3 run:**
  - The 500 ms margin is about three times QA's measured round trip.
  - At a very slow speech rate, the coach's hint (4 s after the swap) can replace a step line that is still waiting.
  - On the menu route, the jump's own line is no longer heard (D-TBe accepts this).
- **Commit trailer.** Both commits carry the one trailer that the plugin's `rules/gitflow.md` (1.0.0) prescribes, `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`. That rule overrides the harness reminder's Opus and session lines.

## Handoff
Next: code-auditor — loop 5 on `6a98510..fc9b6c3`, with the device-2 ruling as the requirement.
- The code commits are `ee5cf08` (F-A) and `fc9b6c3` (F-B1). `abc9b4a` is the ux-designer's docs only.
- Re-run P1–P4. My results, with P5 and P6, are under Checks.
- Rule on retargets 5 and 6, which F-B1's later line forced and the ruling does not name.

Then: qa-engineer — `qa-engineer-device3.md` (ruling §5). Build the debug APK from **`fc9b6c3`**. The tip after this report's commit has the same code, since the report commit changes no code. D-TBe has landed (`abc9b4a`).
