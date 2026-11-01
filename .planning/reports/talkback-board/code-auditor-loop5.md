# code-auditor report — talkback-board, loop 5 (F-A and F-B1, device-2 ruling)
Date: 2026-11-01
Branch: feature/talkback-board (6a98510..17d6b37; code commits `ee5cf08` F-A and `fc9b6c3` F-B1; `abc9b4a` is the ux-designer's D-TBe, docs only; `17d6b37` is the developer's report)
Skills loaded: ergoz-soft:standards (role code-auditor; the snapshot is 0.6.2 and the plugin 1.0.0, so the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, testing, puzzle-design, play-store, kotlin-android and compose were read in full, plus the product's `.claude/rules/lessons.md`), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: none of my own; QA's emulator `ergoz_studio` was up, and I did not touch it
Ran on: claude-opus-5-5 at effort high. **Staffing deviation:** `agents/code-auditor.md` pins fable at xhigh. Founder answer Q-019 (option 3) puts Fable-pinned roles on Opus until 3.0.0 is published, and the founder directive of 2026-10-31 (STATUS.md) sets this role one effort step lower. The author was also on Opus, so rule 1 does not hold. Each ruling below rests on a test run or a mutation probe that I ran myself.
Fan-out: none
Loops: 5
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- F-A and F-B1 match the device-2 ruling §2–§3 and D-TBe. Only `TutorialAnnouncements.kt` and `TutorialAnim.kt` change under `app/src/main`. Gameplay's shared `JumpAnnouncements` is untouched. With touch exploration off, nothing changes.
- I re-ran the ruling's probes P1–P4, both commits' reds by reverting their production code, and two probes of my own (P7, P8). All eight went red, each for the expected reason. The unmutated baseline is 16 of 16 green.
- The Check is green at `17d6b37`, with `:app` tests forced to re-run: 1,366 app tests and 294 domain tests, 0 failures.
- I approve both retargets that the ruling did not name (lines below). No suppression was added.
- No must-fix findings. Four Info items, including my view on the ~700 ms step-3 gap: it is not reachable, so it does not block. **VERDICT: PASS.**

## Requirements traced
| Requirement | Where it is met | Evidence |
|---|---|---|
| Ruling §3 / D-TBe item 3 (F-A): opening speaks the caption alone | `TutorialAnnouncements.kt:126-129` | P3 red, 3 opening cases |
| §2.1 / TB-6: under touch exploration, step 1's and step 2's clearing jumps put no line; step 3's jumps keep theirs | `TutorialAnnouncements.kt:60-81` | P2 red (b); the developer's P5 and P6 cover both gates |
| §2.2 / D-TBe item 1: focus first, then the line after `STEP_LINE_AFTER_FOCUS_MS = 500L` | `TutorialAnnouncements.kt:130-135`, `TutorialAnim.kt:46-55` | P1, P4 and P8 red |
| §2.3 / D-TBe item 2: touch exploration off — no move, the line at the beat, the jump line kept | `:131` gate | P7 red on (c) |
| Completion, arm, hold and gameplay unchanged | `git diff --name-only 6a98510..HEAD -- app/src/main` | only the two files |

## Review
- **Correctness.** `TutorialJumpAnnouncements` reads `state` through `ChangeEffect`'s `rememberUpdatedState` (`BoardAnnouncement.kt:305`). So it judges the post-jump board, one peg left, while `stepNumber` is still the old step: the swap comes 650 ms later. Test (b) shows this. Because steps 1 and 2 each have exactly one move, the `pegCount == STEP_COMPLETE_PEGS` half of `clearsAStepBeforeTheLast` is equivalent to "any jump on steps 1 and 2". A mutant that drops it would be equivalent, so I did not run it.
- **Structured concurrency.** The `delay` runs inside the `LaunchedEffect(state.stepIndex)` that already existed. A step change, Skip, or leaving the screen cancels it. There is no new scope or dispatcher.
- **Compose.** `stringResource(state.captionRes)` is read in composition, the same pattern as `CompletionAnnouncement`'s done line. It is cheap, and the screen recomposes per state change, not per frame. There is no new state and no allocation in a draw path.
- **Tests.**
  - All tests are deterministic: the clock is paused, there is no sleep, the board is seeded, and the frame counts are derived from named constants (`FRAMES_PAST_THE_STEP_LINE`).
  - Each name reads as a sentence.
  - The new helpers (`eventsSent`, `isFocusOf`, `isDescriptionChangeTo`) use Robolectric's public shadow API, with no reflection.
  - Under P4, (a) catches the mutant by only one event (19 against 20). Retarget 3 catches P4 on timing alone, so the pair is sufficient.
- **Security.** No network, no secrets, no input from outside the app, no new dependency (no `*.toml`, `*.gradle.kts` or `THIRD_PARTY.md` change).
- **Suppressions.** `git diff 6a98510..HEAD -- app | grep -nE '^\+.*(@Suppress|baseline|@Ignore|!!|GlobalScope|Thread\.sleep)'` printed nothing (exit 1).

APPROVED SUPPRESSION: none (none added).

## Rulings on the two unnamed retargets
Both tests still assert what they asserted before: `stepIndex == 1` and the step-2 line on the channel, or the channel handed out as a polite leaf carrying that line. The only change is that they wait for the line until F-B1 sends it: the beat, the focus gap and `STEP_LINE_AFTER_FOCUS_MS`. No assertion was removed or loosened.

They also gained strength. Under P1, P4 and R-FB1 both go red with "but was: null", so they now also catch a line sent too early, which has expired by the time they look.

- approved by code-auditor: `TutorialTalkBackTest.aJumpActionClearsTheFirstStepAndAdvancesItAsTheTwoTapsDo` retarget (`idlePastTheStepBeat(); waitForIdle()` → `advanceToTheNextStepAndItsLine()`). F-B1's required delay forces it. GD-TB-5's claim is unchanged.
- approved by code-auditor: `TutorialTalkBackTest.onStepTwoAServiceIsHandedTheChannelAsAPoliteLeafWithTheStepLine` retarget (the same change). F-B1's required delay forces it. The B-1 leaf/polite assertions are unchanged.

I also checked the two retargets the ruling named, and the F-A constant retargets. They are as the ruling prescribes: retarget 3 asserts focus, then that the line is absent, then the line; retarget 4 uses `T3_FIRST_JUMP_LINE`.

## Findings
No Critical, Major or Minor findings.

**Info**
1. **The ~700 ms step-3 gap: not reachable, so it does not block. A 3.0.1 item, or ride-along on any further code round.** (`TutorialAnnouncements.kt:122`)
   - **The gap.** Step 3's effect is keyed on `stepIndex`, and completion does not change `stepIndex`. If completion lands during step 3's focus gap plus the 500 ms wait, the effect still runs to its end. It moves focus to the board stop, then puts "Step 3 of 3 …" on the polite channel after the done line, where it can replace the done line before TalkBack speaks it.
   - **Why it cannot happen today.** Step 3 needs four jumps, and under touch exploration each jump is two focused activations or a menu action. No player completes that in under a second. While a TalkBack menu holds window focus, the effect is parked in `moveInputFocusTo`'s window-focus wait, and the player cannot play until the menu closes. The developer's clock trace (about 1,072 ms per T3 move in the harness) agrees.
   - **The fix.** The developer's `LaunchedEffect(state.stepIndex, state.completed)` is correct. On completion it cancels the pending run, and the re-run returns at `:125`. A test needs T3 driven on a paused clock.
2. **The same window applies to the player's own lines on steps 2 and 3.** A selection or rejected tap made within about 700 ms of a swap would be replaced on the channel by the later step line. This is unreachable for the same reason as item 1: TalkBack must first move to a cell. The fix in item 1 does not cover it, but the coach's 3.0.1 candidate in D-TBe ("never takes an unspoken step line's place") is the same family. Recorded only.
3. **The step line now waits behind `moveInputFocusTo`'s unbounded window-focus wait** (`TouchExploration.kt:73`). Before F-B1, the line went out before that wait. If the window never regains focus while the tutorial is up, the step line is never sent. Every focus loss I can name ends with focus back (the TalkBack menu, a notification shade), and D-TBe's order requires the move first. This extends loop 3's open item on that wait (QA's). It is not a new defect.
4. **On a recreation at step 2 or 3** (rotation keeps the step, `docs/ux-spec.md:699`), the first-composition branch now speaks that step's caption without "Step N of 3". This follows the ruling's letter ("the first-composition branch speaks `stringResource(captionRes)` only"). TB-6 and TB-8's "a recreation … speaks nothing" (`ux-spec.md:6760`) was not met before this diff either, so this is pre-existing. For the ux-designer to weigh if QA ever judges a rotation mid-tutorial.

**Process (not code; producer).** `abc9b4a` carries `Co-Authored-By: Claude Opus 5.5` and a `Claude-Session:` line. The plugin's `rules/gitflow.md` (1.0.0) allows only the Fable trailer. The developer's commits comply. This is the repo's known missing hook (STATUS.md, play-upload row). I ask for no history rewrite.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-tb/.planning/reports/talkback-board/code-auditor-loop5.md` — this report, committed alone.

## Checks
All commands ran in `/Users/ermanergoz/Desktop/PegSolitarie-tb` unless noted.

**Host.** Before the Gradle runs:
- `uptime` showed a load of 3.22 (05:49);
- `adb devices` listed only `emulator-5580`;
- `adb -s emulator-5580 emu avd name` returned `ergoz_studio`, QA's, so no `Pixel_9a_bench` was up.

The 1-minute load rose to 7.80 during the runs. No emulator was touched, and `./gradlew --stop` was not run.

**The Check.**
- `./scripts/check.sh --wait --max-workers=4 --console=plain :app:testDebugUnitTest --rerun` at `17d6b37` on a clean tree, with the app test-results directory cleared first, about 05:49–06:00:41 — **pass**.
- It printed `BUILD SUCCESSFUL in 11m 8s`, `104 actionable tasks: 12 executed, 92 up-to-date` and `CHECK_EXIT=0`, with 0 `w:` lines.
- From the JUnit XML:
  - `:app` ran 1,366 tests in 136 classes, with 0 failures, 0 errors and 0 skipped;
  - `TutorialTalkBackTest` ran 16 of 16 green;
  - `:domain` ran 294 tests in 33 classes, 0 failures.
- `:app:detekt`, `:app:ktlintCheck` and lint analysis were up to date on these exact inputs, and `:app:lint` ran.

**Gates.**
- `git diff --name-only 6a98510..HEAD -- app/src/main | sort` lists only `ui/onboarding/TutorialAnim.kt` and `ui/onboarding/TutorialAnnouncements.kt` — pass.
- The suppression grep above printed nothing (exit 1) — pass.

**Probes.** In a throwaway `git worktree add --detach <scratchpad>/probe5 17d6b37`, outside the repo. A script mutated only `TutorialAnnouncements.kt` there, ran `./gradlew :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.onboarding.TutorialTalkBackTest' --max-workers=3 --console=plain -q`, read the JUnit XML, and restored the file. Results:

| Probe | Mutant | Result |
|---|---|---|
| baseline | none | 16/16 green |
| P1 | step line announced before the move | **red**, 4: (a) `expected to be less than: 15 / but was: 19`; retarget 3; retargets 5 and 6 (`but was: null`) |
| P2 | clearing jump's line restored (`if (now != null)`) | **red**, 1: (b) `expected not to contain: Row 1, column 1 to row 1, column 3. … 1 peg left.` |
| P3 | F-A reverted (opening speaks `format.stepLine(…)`) | **red**, 3: the three opening cases, `but was: Step 1 of 3. Tap a peg, …` |
| P4 | `delay(0L)` | **red**, 4: (a) `expected to be less than: 19 / but was: 20`; retarget 3; retargets 5 and 6 |
| P7 (mine) | the wait not gated on touch exploration | **red**, 2: (c) `but was: Row 1, column 1 to row 1, column 3. …`; `threeNonProgressingTapsBringTheCoachWhoseHintIsSpoken` (`but was: Step 2 of 3. …`) |
| P8 (mine) | focus move dropped, wait kept | **red**, 3: retarget 3 and `aStepAfterADoubleTappedStepMovesFocusToItsBoardStopAgain` (board stop not focused); (a) `the board stop's focus event … but was: -1` |
| R-FB1 | F-B1's production reverted (file as at `ee5cf08`), tests at the tip | **red**, 5: (a), (b), retarget 3, retargets 5 and 6 |
| R-FA | F-A and F-B1 reverted (file as at `6a98510`), tests at the tip | **red**, 8: the three opening cases plus R-FB1's five |

After the runs:
- `cmp` showed the file identical to the tip, and the probe worktree's `git status --short` was clean;
- `git worktree remove --force` and `git worktree prune` were run, and `git worktree list` shows no `probe5`;
- the repo's `git status --short` was clean before this report.

Not run: a device, an emulator, `.planning/log/`.

## Open issues / risks
- Info items 1 to 3 are 3.0.1 candidates for the tech-lead. Item 4 is for the ux-designer, only if a mid-tutorial rotation is ever judged.
- The device result is QA's (`qa-engineer-device3.md`, ruling §5). The JVM proves only the order and timing the app sends, not TalkBack's queue. The 500 ms margin and slow speech rates stay recorded in D-TBe.
- Staffing: this review, like the code, ran on Opus (Q-019), one effort step below the pin.

## Handoff
Next: producer —
- QA's device-3 re-check (ruling §5) on `fc9b6c3`'s code, which is identical to `17d6b37`.
- Then the tech-lead's sign-off, which reads this report with the earlier code-auditor reports on this branch.
- No §6 fallback trigger came from this review.

VERDICT: PASS
