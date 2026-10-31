# code-auditor report — talkback-board, loop 3 (device defects B-1 to B-9)
Date: 2026-10-31
Branch: feature/talkback-board (b84853e..2236207; reviewed `git diff 75a03be..2236207`, F0's merge `b84853e` by `git show --remerge-diff`; head `bdd2784` adds only the developer's report)
Skills loaded: ergoz-soft:standards (role code-auditor; snapshot 0.6.2 is older than the plugin's 1.0.0, so the plugin copies of studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose and testing were read in full; logging, puzzle-design and play-store are identical to the snapshot; `.claude/rules/lessons.md`), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: none of my own; other agents' builds were running on this machine (see Checks)
Ran on: claude-opus-5-5 at effort high — **staffing deviation:** `agents/code-auditor.md` pins fable at xhigh. Founder answer Q-019 puts every Fable-pinned role on Opus, and the founder's directive of 2026-10-31 sets every role one effort step lower (xhigh → high). The code under review was also written on Opus, so rule 1 (a reviewer above the author) does not hold for this loop; every finding below rests on a probe or a source line, not on judgement alone.
Fan-out: none
Loops: 3
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- F0–F5 do what the ruling's §1–§8 ask. Every ruled probe behaves as the ruling expects: P1–P5, P7 and P8 are each killed by the ruled test, and P6 survives outside touch mode. No suppression, baseline, `@Ignore` or `!!` was added. F0's single docs conflict kept both sides.
- Deviations 1–3 and 6 are accepted on their evidence. I checked the sources they cite, and my own P12 shows the one-pixel footprint is guarded.
- Two of the new behaviours have no test, and my own probes P9 and P10 survive every test class. Both gaps are Minor and test-only.
- Check: green on the re-run (Checks).
- **VERDICT: FAIL**, on the two test gaps only. No production line needs to change, so QA's device re-run (§11.3) need not wait for the fix.

## Findings
1. **Minor (test gap) — `app/src/main/java/com/app/pegsolitaire/ui/components/BoardAnnouncement.kt:179` (`traversalIndex = AnnouncementTraversal.LAST`, `:145`): no test pins the channel last in the reading order.**
   - The ruling's §1, mechanism 3 ("Not a stop on opening"), requires this. While it holds a line, the channel is a speaking leaf at the screen's top-left, which is Compose's first stop. Without the index, TalkBack's first focus on a new screen can land on it during the step-1 line or the Results summary. That would make TB-9's "Step 1 heard twice" open item worse.
   - Probe P9 deleted the line. All 64 cases of `GameScreenTalkBackTest`, `TutorialTalkBackTest`, `BoardSemanticsTest` and `ResultsScreenTalkBackTest` stayed green, and no test file references `AnnouncementTraversal`.
   - Fix: one case per host, or at least the tutorial and Results, using the existing `traversalBeforeOf` helper. While the channel holds its line, check one of two things: the channel has no traversal-before successor, or the screen's last ordinary stop names the channel as its successor. Red first under P9's mutant.
2. **Minor (test gap) — `app/src/main/java/com/app/pegsolitaire/ui/components/BoardCellOverlay.kt:258-265` (`LatestFocusMove`, used at `:85-92`): deviation 6's cancel is untested.**
   - The developer added the holder so that "a newer move cancels a waiting one" (ruling §8) also holds for the jump action, which runs from `scope.launch` rather than a keyed effect. That is new logic, and nothing exercises it.
   - Probe P10 replaced `job?.cancel()` with nothing. All 64 cases stayed green (the same run as P9).
   - Fix: a `GameScreenTalkBackTest` case that uses `FocusControlledWindow`.
     - With window focus false, take two jump actions to two different destinations.
     - Give the window focus back, then settle.
     - Assert that the second destination is focused and that the first destination's focus trace never went true.
     - Red first under P10.

No other finding. The cases below were read and are not findings:
- **Info, for the tech-lead.** `ResultsScreen.kt:418-419` sets the saved `spoken` flag when the summary is asked for, but since F1 the line waits up to `ANNOUNCEMENT_ARM_GAP_MS` (200 ms) before it goes on. A recreation inside that window, such as a rotation in the first ~200 ms after Results opens, saves `spoken = true` with no line ever sent. Under D-TBc item 2 the summary is the win's only spoken record. The window is too short for a deliberate action, so I do not hold the merge on it. Setting the flag when the line is shown would close it.
- **Info, for QA.** `TouchExploration.kt:73` waits for window focus with no bound. A move started while another window holds focus fires whenever focus returns. In every path today focus returns at once: the dialog closing, or TalkBack's menu if it takes focus at all. D-TBb's "once focus rests there, nothing moves it again" is the device check.
- **Info.** P11 (Got it's override ungated, `canFocus = true` always) survives every `Tutorial*` class. I verified the gate by reading (`TutorialScreen.kt:376`). TB-11's finger play is QA's regression item.

APPROVED SUPPRESSION: none. No suppression was added in `75a03be..2236207`. `GameScreen.kt:170`'s existing `@Suppress("LongMethod")` keeps its reason. Its KDoc list gains the region's line and drops "and its spoken channel" from the board-panel line: two lines touched, still one suppression. This is accepted as deviation 1's consequence.

## The ruling's §11.2 reads
- **`BoardCanvas` read-only branch** (`BoardCanvas.kt:269-279`): loop 3 touched only the `announcement` parameter, its KDoc and the interactive chain's `.boardAnnouncement(…)`. `git diff 3ab1cae 2236207` shows that nothing below the `Canvas(` calls changed. The read-only `Canvas` carries the same modifiers as develop's, without develop's identity `.then(Modifier)`, which was already reviewed in loop 0. §11.4's tripwire stays clean.
- **`AnnouncementRegion`** (`BoardAnnouncement.kt:162-183`):
  - an empty `Box` with no click, a polite live region, the `LAST` index, and `onPlaced { arm() }`, which is idempotent;
  - it is the last child of each root (`GameScreen.kt:333`, `TutorialScreen.kt:189`, `ResultsScreen.kt:174`), not the first. That is deviation 1, below.
- **The arm's constant and KDoc** (`BoardAnnouncement.kt:41-49`) cite `:275` and `:2572`. In ui 1.11.3, `:275` is `SendRecurringAccessibilityEventsIntervalMillis = 100L` and `:2572` is `previousSemanticsNodes[id] ?: return@forEachKey`. Both are correct.
- **`BoardStop`**:
  - each slab's first child, a childless `Box` at `fillMaxSize()` (`GameScreen.kt:269`, `TutorialScreen.kt:292`);
  - each slab is an explicit group (`GameScreen.kt:265`, `TutorialScreen.kt:290`);
  - A2.1's `GameTraversal.BOARD_BEFORE_BANNER` is still on the gameplay slab (`GameScreen.kt:266`).
- **The four moves** (the jump action, the step swap, completion, and the banner's Undo and Restart) all go through `moveInputFocusTo(target, window)`. `TouchExploration.kt:69-77` is the only `requestFocus()` in `app/src/main`. Each is gated on touch exploration:
  - `BoardCellOverlay.kt:88`;
  - `TutorialAnnouncements.kt:85`, `:103`;
  - `BannerAction.kt:54`, which awaits only `takeIf { touchExploration }`.
- **`focusProperties`** is gated (`TutorialScreen.kt:376`). It sits after `focusRequester`, above the M3 `Button`'s own `clickable`. `claySquish` adds no focus target (`Clay.kt:150-173`: `graphicsLayer` and `pointerInput` only), so the override reaches the button's target.
- **Every existing-test edit is named** in the developer's report. The eight test files changed in `b84853e..2236207` match its list. The audio files `GameScreenWrongTapTest` and `TutorialWrongTapTest` change only the tag the channel is read by.

## Deviations
- **1 — the region drawn last, at one pixel: accepted.**
  - The cause is read: M3 1.4.0's `Surface` sets `isContainer = true` (`Surface.kt:125`; the developer cites `:121`, which is line drift only). `IsContainer` is an `AccessibilityKey` (ui 1.11.3 `SemanticsProperties.kt:94`). So the `Scaffold`'s window-filling `Surface` is important and covers any sibling drawn before it.
  - Drawn last, nothing covers the region. At 1 × 1 px it takes nothing from the stops beneath.
  - My probe P12 gave the last-drawn region full size, and it pruned the board stop's hints on gameplay and the tutorial (`theBoardStopFollowsTheTitleAndPrecedesTheFirstCell`, `theBoardStopIsReadAfterSkipAndBeforeTheFirstCell`: `traversalBeforeOf` null) and the B-9 channel case. The footprint is guarded.
  - Open for the device: whether TalkBack speaks a one-pixel polite region (TB-8, TB-9).
- **2 and 3 — native graphics; the touch-mode harness: accepted.**
  - `assertInTouchMode()` checks the host view's `isInTouchMode`, which is what §2 asks.
  - P5 is killed in this harness. P6 (P5 outside touch mode, with the class's `@GraphicsMode` and the check removed) is green, so the harness is what catches B-2. That is the ruling's own reading of P6.
  - The two fallbacks' failure modes the developer reports are plausible. I did not re-run them.
- **6 — `LatestFocusMove`: accepted in design.** It keeps §8's cancel rule on the one move not run from a keyed effect. It is untested: Finding 2.

## F0's conflict hunk
`git show --remerge-diff b84853e` shows one conflict, in `docs/ux-spec.md`'s revision log. The resolution keeps this branch's Eleventh revision (D-TBb) and develop's Twelfth revision, separated by one `>` line, and drops nothing. `GameScreen.kt` and `TutorialScreen.kt` merged without conflict. The `SelectionSound` and related `NewOrdinalEffect` hunks and `CompletionStar`'s gate come from develop's reduced-motion-live-gate, and loop 3 did not touch them. The ux-designer's D-TBc edit is the separate commit `7742a21`, which is docs only.

## Probes
All probes ran in a throwaway `git worktree add --detach` of `2236207` under this session's scratchpad. The worktree was removed afterwards (`git worktree list` no longer shows it). Each mutant was run against the classes named below, and its failures were read from the JUnit XML.

| # | Mutant | Expected | Result |
|---|---|---|---|
| P1 | The tutorial's channel back on the canvas: a full-size region in `BoardCanvas`'s box, after the `Canvas` and under the cells | red | **killed**: `onStepTwoAServiceIsHandedTheChannelAsAPoliteLeafWithTheStepLine` ("the channel is pruned on T2"), `everyLineAfterTheFirstStepSwapIsSentToTheService` ("missing (5)") |
| P2 | Results' channel back on the content `Column` (tag, arm, live region), the root region removed | red | **killed**: `aServiceIsHandedOneLiveRegionOnResultsAndItIsALeaf` (childCount 11). The other 6 Results cases stay green |
| P3 | The arm removed (`announce` always shows) | red | **killed**: `theFirstStepsLineIsHeldUntilTheChannelIsArmedAndIsThenSent`, `theSummaryWaitsForTheArmAndIsSentOnceThroughTheCountUpAndTheHoldsEnd`. Gameplay's 24 cases are green |
| P4 | `boardStop` back on both slab `Surface`s, the leaf removed | red | **killed**: `theBoardStopFollowsTheTitleAndPrecedesTheFirstCell` (1312 expected, 1316), `theBoardStopIsReadAfterSkipAndBeforeTheFirstCell` (2802, 2806). A2.1's slab case also failed, only because my mutant moved the slab's tag. `BoardSemanticsTest` composes its own slab, so it is reached only through the hosts' cases |
| P5 | Got it's `focusProperties` removed | red | **killed**: `completingTheTutorialSpeaksTheDoneLineAndMovesFocusToGotIt` (Focused ≠ true) |
| P6 | P5, with `TutorialTalkBackTest` outside touch mode | green | **green** (informational, as ruled) |
| P7 | The window-focus wait removed | red | **killed**: `aBannerRestartConfirmedInItsDialogMovesFocusOnlyOnceTheWindowHasFocusAgain`. The other 56 focus cases are green |
| P8 | The selection line restored under touch exploration | red | **killed**: both `underTouchExplorationASelectionPutsNoLineAndItsPutDownStillDoes`. `GameScreenWrongTapTest`'s selection case is green (touch exploration off) |
| P9 | Own: `traversalIndex = AnnouncementTraversal.LAST` removed | — | **survived**, 64 of 64 green: Finding 1 |
| P10 | Own: `LatestFocusMove` stops cancelling | — | **survived** (the same run as P9): Finding 2 |
| P11 | Own: Got it's override ungated | — | **survived**, every `Tutorial*` class green: Info |
| P12 | Own: the region at full size, still drawn last | — | **killed**: both board-stop order cases (successor null), B-9's channel case |
| P13 | Own: the held line keeps the oldest, not the newest | — | **killed**: `anIdleNudgeBringsTheCoachWhoseHintIsSpoken`, `aJumpActionClearsTheFirstStepAndAdvancesItAsTheTwoTapsDo` |

## Security
The change has no network, storage, secret, permission or input-from-outside surface. The new test helpers touch only Robolectric shadows.

## Performance
- `AnnouncementRegion` reads `announcer.current` in its own scope, so a line change recomposes only that leaf.
- `BoardStop` is a static leaf.
- The arm launches one coroutine per screen.
- `LocalWindowInfo.current` is read once per caller, and `snapshotFlow` runs only for the length of a move.
- Nothing is allocated per frame.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-tb/.planning/reports/talkback-board/code-auditor-loop3.md` — this report, the only file written in the repo, committed alone.

## Checks
All commands ran in `/Users/ermanergoz/Desktop/PegSolitarie-tb` at `bdd2784` (code `2236207`), on a clean tree, unless noted.
- `./scripts/check.sh --max-workers=4`, finished by 19:59 (its lint report's time), the dispatch's command: BUILD SUCCESSFUL in 11m 14s. **The record is weak.** I logged it to `check.log` in a scratchpad that another session also writes to. That file later held another worktree's lines (`Wrote HTML report to …/PegSolitarie-lic/…`) and lost my exit line, so I do not cite this run as the gate.
- `./scripts/check.sh --max-workers=4 --console=plain :app:testDebugUnitTest --rerun`, at 22:19–22:30, logged to a file only this audit writes: **pass**.
  - Its output: `BUILD SUCCESSFUL in 11m 12s`, `104 actionable tasks: 12 executed, 92 up-to-date`, `CHECK_EXIT=0`, 0 `w:` lines.
  - The JUnit XML: `:app` 1,325 tests in 131 classes; `:domain` 294 tests in 33 classes. 0 failures, 0 errors, 0 skipped.
  - `:app:detekt`, `:app:ktlintCheck`, `:domain:fuzz`, `:domain:test` and `:domain:koverVerify` were up to date from the first run; `:app:lint` and `:app:testDebugUnitTest` ran.
- `git show --remerge-diff b84853e`: one conflict, `docs/ux-spec.md`, both sides kept.
- `git diff 75a03be..2236207 -- app/src/main | grep -nE '^[+-].*(@Suppress|baseline|@Ignore|!!|disable)'`: nothing.
- `grep -rn 'requestFocus\|moveInputFocusTo\|focusProperties' app/src/main`: one `requestFocus()` (`TouchExploration.kt:76`), and five calls through `moveInputFocusTo`.
- §12 tripwires, as I reconstructed them from the ruling's §11.4 (the brief's exact lines were not re-read):
  - line 1, `git diff --name-only develop...2236207` filtered to the start-path files: prints nothing;
  - line 2, `git diff develop...2236207 -- …/BoardCanvas.kt | grep -E '^\+.*(Layout|focusable|semantics|boardStop|BoardCellOverlay)' | grep -v -E '^\+\s*//'`: prints only `+            BoardCellOverlay(`.
- Sources, from the Gradle cache's source jars: `material3-android-1.4.0` `Surface.kt:122-125` (`isContainer = true`); `ui-android-1.11.3`:
  - `SemanticsProperties.kt:94` (`IsContainer` is an `AccessibilityKey`);
  - `AndroidComposeViewAccessibilityDelegateCompat.android.kt:275`, `:2572`;
  - `WindowInfo.kt:68` and `AndroidWindowInfo.android.kt:47` (`isWindowFocused by mutableStateOf`, so `snapshotFlow` sees it).
- Probes P1–P13, in the throwaway worktree: `./gradlew :app:testDebugUnitTest --tests '<class>' --max-workers=4 --console=plain -q`, each from a clean `test-results` directory. Results are in the Probes table. Then `git worktree remove --force` and `git worktree prune`.
- Not run: a device, an emulator, `.planning/log/`.

## Open issues / risks
- **Staffing:** the author, the tech-lead's ruling and this review all ran on Opus (Q-019), at one effort step below the pin (2026-10-31 directive). Rule 1's "reviewed on a superior model" does not hold. The producer should carry this to the tech-lead's sign-off.
- **For the device (QA, §11.3):**
  - whether TalkBack speaks a one-pixel polite region (TB-8, TB-9);
  - the menu route after §8's wait (D-TBb);
  - TB-11's finger play;
  - L1-2.
- **The open Info items:** the Results rotation inside the arm (tech-lead), and the unbounded window-focus wait (QA).
- **Home's merge:** once `bugfix/home-chip-arrival` lands, `theBoardStopFollowsTheTitleAndPrecedesTheFirstCell` changes its predecessor from the title to the timer, as the developer notes.

## Handoff
Next: android-game-dev (fix mode), through the producer.
- Add Finding 1's and Finding 2's tests, red first under P9 and P10 respectively. No production change is expected.
- Then a fresh code-auditor loop 4 on the new commits.
- QA's device re-run (§11.3) can proceed on `2236207` in parallel, because the fix touches only `app/src/test`.

VERDICT: FAIL
