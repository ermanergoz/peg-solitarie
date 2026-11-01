# code-auditor report — talkback-board, loop 4 (re-review of loop 3's findings 1 and 2)
Date: 2026-10-31
Branch: feature/talkback-board (92fd8a0..92fd8a0; reviewed `git diff 7c16094..e40fcbb`: the two test files and the developer's report. The Check ran at `d174323`, this role's rename of the loop-3 report, whose code is identical to `e40fcbb`)
Skills loaded: ergoz-soft:standards (role code-auditor; the plugin copies that loop 3 read in full, re-used in this same step), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: none of my own; other agents' builds were running on this machine
Ran on: claude-opus-5-5 at effort high — **staffing deviation:** `agents/code-auditor.md` pins fable at xhigh. Q-019 puts Fable roles on Opus, and the founder's directive of 2026-10-31 sets every role one effort step lower. The author was also on Opus, so rule 1 does not hold; each ruling below rests on a probe run.
Fan-out: none
Loops: 4
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- Both loop-3 findings are closed. Each new case fails under its mutant in my own worktree and passes without it.
- I accept Finding 2's changed design. My own probe repeats the developer's result: in the shape I prescribed, the frame traces are identical with and without the cancel, so that shape cannot observe the cancel.
- Finding 1's single tutorial case is enough for the finding as raised.
- The change is test-only and adds no suppression. The Check is green: `:app` 1,327 tests, `:domain` 294, 0 failures.
- **VERDICT: PASS.**

## Loop-3 findings
1. **Finding 1 — the channel last in the reading order: closed.**
   - `TutorialTalkBackTest.whileTheChannelHoldsTheFirstStepsLineItIsTheLastStopInTheReadingOrder` checks that one stop's traversal-before successor is the channel, and that the channel has none.
   - **Re-run of P9** (`BoardAnnouncement.kt:179` deleted), on the four TalkBack classes at `e40fcbb`: 66 tests, 1 failure, the new case ("value of: iterable.size() / expected: 1 / but was: 0"). Every other case is green.
   - **Is one tutorial case enough? Yes, for this finding.** The gap was the unguarded line `:179`. All three hosts compose the same `AnnouncementRegion`, so that one line is now guarded for every host.
   - The developer is right that both halves are needed. "No successor" alone would also pass for a channel that is out of the order altogether.
   - **Residual risk, not a finding.** A host that later composes the region inside a traversal group of its own would not be caught. Today all three hosts compose it as their root's last child (`GameScreen.kt:333`, `TutorialScreen.kt:189`, `ResultsScreen.kt:174`), and loop 3 read that. The device covers the rest: QA's TB-9 records whether a screen's first focus lands on the channel, and TB-8 records Results' order.
2. **Finding 2 — `LatestFocusMove`'s cancel: closed, with the changed design accepted.**
   - `GameScreenTalkBackTest.aJumpActionTakenWhileAnEarlierOnesFocusMoveWaitsCancelsItSoOnlyItsOwnDestinationTakesFocus` takes the second jump action inside the first move's focus gap. It then asserts frame by frame that the first destination is never focused and the second ends focused.
   - **Re-run of P10** (`job?.cancel()` removed): 66 tests in the four classes, 1 failure, the new case. The first destination is focused for 7 frames (frames 6–12) before the second move takes it. Without P10 it passes.
   - **The prescribed shape, probed by me.**
     - I wrote my own candidate in the throwaway worktree. It takes both actions while `FocusControlledWindow` reports no window focus, with 100 ms between them so the second cell has its action, and then gives focus back.
     - Under P10 and without it, the candidate passed, and it printed the same trace both times: `before=(false, false)`; after focus returns, 13 frames of `(false, false)`, then `(false, true)` to the end.
     - Both waits resume in the same snapshot apply, and the two equal gaps end in the same frame. So the older request is overtaken inside one frame and never shows as `Focused`. The delegate batches what TalkBack is sent at 100 ms, so TalkBack would not see it either.
     - So with both moves waiting on window focus, the cancel changes nothing observable, and a test of that path would be vacuous.
   - **The ruling.** The gap path is the one wait where the cancel changes what TalkBack is sent, and the new case pins it. It is still §8's rule, "a newer move cancels a waiting one" (`TouchExploration.kt:67`). I accept the design and need no further test.

## Review of the two cases
- The tests are deterministic: the clock is paused, there is no sleep, the board is seeded, and the positions are named constants with KDoc (`OPENED_HOLE`, `LEFT_OF_THE_OPENED_HOLE`).
- Each name reads as a sentence.
- The helpers are small: `everyNode()` and `cellFocused()`.
- No production line changed: `git diff --stat 7c16094..92fd8a0` shows the two test files, 58 insertions.
- `git diff 7c16094..92fd8a0 | grep -nE '^\+.*(@Suppress|baseline|@Ignore|!!)'` finds nothing. The commit carries one `Co-Authored-By` line.

APPROVED SUPPRESSION: none (none added).

## Outputs
- `.planning/reports/talkback-board/code-auditor-loop3-record.md` — loop 3's report, renamed with `git mv`, commit `d174323`.
- `.planning/reports/talkback-board/code-auditor-loop4.md` — this report, committed alone.

## Checks
- `./scripts/check.sh --max-workers=4 --console=plain :app:testDebugUnitTest --rerun` at `d174323`, 22:59:39–23:10:49, logged to a file only this audit writes: **pass**.
  - It printed `BUILD SUCCESSFUL in 11m 10s`, `104 actionable tasks: 12 executed, 92 up-to-date` and `CHECK_EXIT=0`, with 0 `w:` lines.
  - From the JUnit XML: `:app` ran 1,327 tests in 131 classes and `:domain` 294 in 33, with 0 failures, 0 errors and 0 skipped. Both new cases are in the results.
- P9, in a throwaway `git worktree add --detach` of `e40fcbb`: `./gradlew :app:testDebugUnitTest --tests '*GameScreenTalkBackTest' --tests '*TutorialTalkBackTest' --tests '*BoardSemanticsTest' --tests '*ResultsScreenTalkBackTest' --max-workers=3 --console=plain -q`. **Fail, as intended:** 66 tests, 1 failed (Finding 1's case).
- P10 plus my both-waiting candidate, the same four classes: **fail, as intended.** 67 tests; the only failure is Finding 2's case. My first try at the candidate failed only because I took the second action before the board updated; I added the 100 ms and re-ran.
- The candidate alone, `--tests '*GameScreenTalkBackTest'`, under P10 and then with production restored: green both times, with identical traces; Finding 2's case failed under P10 and passed without it.
- `git worktree remove --force` and `git worktree prune`: the probe worktree is gone (`git worktree list` shows no `probe4`). No probe file reached the repo (`git status --short` was clean).
- Not run: a device, an emulator, `.planning/log/`.

## Open issues / risks
- **Finding 1's residual risk** (a host-level misplacement) is the device's: TB-8 and TB-9.
- **Loop 3's open items stand.** Two Info items: the Results rotation inside the arm (tech-lead) and the unbounded window-focus wait (QA). One device item: whether TalkBack speaks a one-pixel polite region (TB-8, TB-9).
- **Staffing:** this review, like the code, ran on Opus (Q-019), one effort step below the pin.

## Handoff
Next: producer.
- QA's device re-run (`qa-engineer-device2.md`, ruling §11.3) on the branch's current code.
- Then the tech-lead's sign-off (§11.5), which reads this report with `code-auditor.md` and `code-auditor-loop3-record.md`.

VERDICT: PASS
