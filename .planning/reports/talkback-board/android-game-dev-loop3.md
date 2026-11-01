# android-game-dev report — talkback-board, loop 3 (device defects B-1 to B-9)
Date: 2026-10-31
Branch: feature/talkback-board (b84853e..2236207; 7742a21 inside the range is the producer's B-6 docs commit)
Skills loaded: ergoz-soft:standards (the snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; plus the product's lessons.md), superpowers:test-driven-development, gitflow, compose-skill, compose-ui, android-testing
Ran in parallel with: home-chip-arrival merge-in, licences-screen QA re-run, colour-vision QA, play-upload fix; the ux-designer's B-6 ruling, written in this same worktree
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 3
Lessons applied: none (the snapshot holds 0 active lessons)

> Producer note: returned as text (the role declined to write report files); written here verbatim by the producer. A weekly usage limit cut this loop on 2026-10-24 during F1's Check; the same agent resumed on 2026-10-31 after the founder's "limits are reset. Continue".

## Summary
- **F0** merged develop with no code conflict. The one docs conflict was kept on both sides.
- **F1–F5** each started with a test that failed, and each is green on the full Check. I took one deliberate deviation in F1, and changed the test harness for the accessibility-layer cases. Both are explained below and need the auditor's and tech-lead's agreement.
- **F6** and **B-6**: no code, as the rulings say.
- Every commit carries exactly one `Co-Authored-By: Claude Fable 5.1` trailer. No suppression, baseline, `@Ignore` or `!!` was added.

## Commits
| # | Commit | What it does |
|---|---|---|
| F0 | b84853e chore(talkback): merge develop | Merges develop at 3ab1cae (f7dd1fe plus one STATUS docs commit) |
| F1 | 83c494e fix(talkback): put the spoken channel on an uncovered leaf | `AnnouncementRegion`, the arm, `AnnouncementTraversal`; `BoardCanvas` loses its `announcement` parameter (interactive branch only); the three hosts compose the region |
| F2 | 5e34bed fix(talkback): give the board stop a leaf in the reading order | `BoardStop`, `BoardTraversal.STOP_BEFORE_CELLS = -1f`; `boardStop` no longer sets the traversal group; each slab is an explicit group with the stop as its first child |
| F3 | d647d9d fix(talkback): let Got it take focus in touch mode | Got it's `focusProperties` gated on touch exploration; the touch-mode harness |
| F4 | 385e96f fix(talkback): move focus only once the window has focus | `moveInputFocusTo(target, window)` waits for window focus; all four callers pass `LocalWindowInfo.current` |
| F5 | 2236207 fix(talkback): drop the selection line under touch exploration | `SelectionAnnouncements` skips the selection line while touch exploration is on |

## F0's conflict hunk
There was one conflict, in `docs/ux-spec.md`'s revision log, after the "For the merge" paragraph:
- this branch added "Eleventh revision, build loop — 2026-10-24 — AMENDMENT (D-TBb …)";
- develop added "Twelfth revision (the reduced-motion live gate) — 2026-10-24 — AMENDMENT".

I kept both, in that order, separated by a blank quote line. Develop's own entry says a parallel entry with the same number is kept beside it. `GameScreen.kt` and `TutorialScreen.kt` merged automatically, with both sides kept.

The worktree also held the ux-designer's uncommitted D-TBc edit to `docs/ux-spec.md`, which git would not merge over. I did not stash it or overwrite it:
- I built the merge in a temporary worktree;
- I fast-forwarded the branch to it;
- I re-applied the ux-designer's edit with a 3-way file merge, in one guarded command that would abort if the file had changed since my snapshot.

Their only overlap was that their D-TBc paragraph closes the Eleventh entry, right where develop's Twelfth entry begins; I kept both. Every line of their edit was carried through unchanged. The producer later committed it as 7742a21.

## Red tests, quoted (each failed before its fix)
**F1**
- `TutorialTalkBackTest.onStepTwoAServiceIsHandedTheChannelAsAPoliteLeafWithTheStepLine`: "IllegalStateException: the channel is pruned on T2"
- `TutorialTalkBackTest.everyLineAfterTheFirstStepSwapIsSentToTheService`: "missing (5) | #1 : Step 2 of 3. Jumps work up, down, left and right — never diagonally. | #2 : No peg here."
- `TutorialTalkBackTest.theFirstStepsLineIsHeldUntilTheChannelIsArmedAndIsThenSent`: "value of: channelText() | expected: null | but was : Step 1 of 3. Tap a peg, then tap the empty hole two cells away."
- `GameScreenTalkBackTest.withTheBannerUpOverAHeightLimitedBoardAServiceIsStillHandedTheBoardsChannel` (B-9): "value of : iterable.size() | expected : 2 | but was : 1"
- `ResultsScreenTalkBackTest.aServiceIsHandedOneLiveRegionOnResultsAndItIsALeaf`: "value of: getChildCount() | expected: 0 | but was : 11"
- `ResultsScreenTalkBackTest.theSummaryWaitsForTheArmAndIsSentOnceThroughTheCountUpAndTheHoldsEnd`: "expected to be empty | but was: [Solved! — 2 of 3 stars, 2 pegs left.]"
- Two new cases were already green before the fix and stay as guards:
  - `GameScreenTalkBackTest.aServiceIsHandedTheBoardsChannelAsTheWindowsOnlyLiveRegionAndALeaf`: in portrait, English-33's blocked corners leave the canvas uncovered;
  - `ResultsScreenTalkBackTest.aRestoredResultsSendsTheServiceNoSecondSummary`: loop 1's saved flag already stops a second summary.

**F2**
- `BoardSemanticsTest.aServiceIsHandedTheBoardStopAsAScreenReaderFocusableLeaf`: "expected to be true" (`isScreenReaderFocusable`)
- `GameScreenTalkBackTest.theBoardStopFollowsTheTitleAndPrecedesTheFirstCell`: "value of: traversalBeforeOf(...) | expected: 70 | but was : 74"
- `TutorialTalkBackTest.theBoardStopIsReadAfterSkipAndBeforeTheFirstCell`: "value of: traversalBeforeOf(...) | expected: 138 | but was : 142"

**F3**
- `TutorialTalkBackTest.completingTheTutorialSpeaksTheDoneLineAndMovesFocusToGotIt`, in touch mode: "Failed to assert the following: (Focused = 'true') | Semantics of the node: | Node #3518 …"
- The other 53 cases in the three focus classes stay green in touch mode.

**F4**
- `GameScreenTalkBackTest.aBannerRestartConfirmedInItsDialogMovesFocusOnlyOnceTheWindowHasFocusAgain`: "value of: boardStopFocused() | expected to be false"

**F5**
- `GameScreenTalkBackTest.underTouchExplorationASelectionPutsNoLineAndItsPutDownStillDoes`: "value of: channel() | expected: null | but was : Selected row 2, column 4, 1 jump."
- `TutorialTalkBackTest.underTouchExplorationASelectionPutsNoLineAndItsPutDownStillDoes`: "value of: channelText() | expected: null | but was : Selected row 1, column 1, 1 jump."

## Deviations and fallbacks, with reasons
1. **F1: the region is drawn last, at one pixel, not first at full size (deviation).**
   - The ruled form (first child of each root, full size) was pruned on gameplay and Results. It worked only in the tutorial.
   - Cause: Material 3 1.4.0's `Surface` sets `isContainer = true` (`Surface.kt:121`), and `IsContainer` is an accessibility key (ui 1.11.3, `SemanticsProperties.kt:94`). So the Scaffold's Surface is an "important" node that fills the window, and it covers anything drawn under it.
   - A probe confirmed this: on Results, the Scaffold Surface's node was handed out at full-window bounds with keys IsContainer+Shape, while the first-child region was handed out nothing.
   - The fix: `AnnouncementRegion` is now each root's last child, at the top-start corner, `ANNOUNCEMENT_REGION_FOOTPRINT_PX = 1`. Drawn last, nothing can cover it; at one pixel, it covers no stop.
   - Everything else is as ruled: a leaf, no click, polite live region, `traversalIndex = AnnouncementTraversal.LAST`, armed on placement.
   - The auditor's check "drawn first" therefore reads "drawn last, one pixel".
   - `GameScreen`'s `LongMethod` list gains one line for the region. I also dropped "and its spoken channel" from the board-panel line, because the canvas no longer carries it. That is two lines touched, and there is still only one suppression.
2. **F1's accessibility-layer tests run on native graphics.**
   - Compose's pruning uses `android.graphics.Region`, which does nothing under Robolectric's default graphics.
   - With a service on, the provider handed out no node at all. With no service, Compose returns empty node infos (`emptyNodeInfoOrNull`), so the reads mean nothing.
   - So these cases turn on a service and use `@GraphicsMode(NATIVE)`, as `TimerDigitsTest` already does.
3. **§2's harness: neither the primary form nor the ruled fallback worked; the classes now run on native graphics.**
   - The primary form, `setInTouchMode(true)` before composing, does not reach Robolectric's already-created window: 46 of 54 cases failed the harness's own `isInTouchMode` check.
   - The ruled fallback, an `InputMode.Touch` `LocalInputModeManager`, puts only the composition in touch mode while the View stays in keyboard mode. The View then re-focuses after `clearFocus()` and hands focus straight back to the board stop.
   - That made two false failures, `aSecondBannerUndoMovesFocusToTheBoardStopAgain` and `aStepAfterADoubleTappedStepMovesFocusToItsBoardStopAgain`, whose focus traces stayed true throughout. A phone in touch mode would not show this.
   - Final harness: class-level `@GraphicsMode(NATIVE)` on `TutorialTalkBackTest`, `GameScreenTalkBackTest` and `BoardSemanticsTest`. Robolectric 4.16 starts the window in touch mode with native graphics (the ruling's own javap reading). `assertInTouchMode()` checks the host view's `isInTouchMode`, as §2 asks.
   - With this harness, only the ruled red failed.
4. **§2's `focusable` fallback was not needed:** the gated `focusProperties { if (touchExploration) canFocus = true }` works.
5. **§3's provider-only fallback was not needed:** Robolectric computes the traversal hints. On gameplay the board stop's predecessor is the title ("Peg Solitaire"), because home-chip-arrival is not on develop yet.
6. **F4 addition:** the jump action's move runs from `scope.launch`, not a keyed effect. A small `LatestFocusMove` holder lets a newer move cancel one still waiting, so the ruling's "a newer move cancels a waiting one" also holds there.

## Existing tests edited, with reasons
- **GameScreenWrongTapTest** (audio's file): the `boardAnnouncement()` and `assertAnnounced()` helpers, and the region read in `theSentenceIsWithdrawnAfterItsHoldSoTheBoardIsNoFocusStopAfterwards`, now use `ANNOUNCEMENT_REGION_TEST_TAG` (§1 retarget). `aSelectionAndAPutDownEachAnnounceTheirOwnLine` is unchanged and green, because touch exploration is off there.
- **TutorialWrongTapTest** (audio's file): the `channel()` helper now uses the region's tag (§1 retarget).
- **TutorialTalkBackTest:**
  - `assertChannel` now uses the region's tag;
  - `theFirstStepsLineIsSpokenOnOpeningAndFocusIsNotMoved` waits out the arm first, because the line is held until then;
  - native graphics on the class, and the touch-mode check in `open()`.
- **GameScreenTalkBackTest:**
  - `channel()` and `assertChannel()` now use the region's tag;
  - `aRevealedHintIsSpokenAsItsLine` waits out the arm;
  - every TB-1 and focus read now uses `BOARD_STOP_TEST_TAG`, except A2.1's slab-order case, which stays on the slab;
  - `theSlabIsTheBoardStopAndActivatingItChangesNothing` is renamed `theBoardStopIsItsOwnStopAndActivatingItChangesNothing`, and `slabFocused()` is renamed `boardStopFocused()`;
  - native graphics on the class, and the touch-mode check in `startGame` and `showState`;
  - `startGame` and `noWinningMoveBannerUp` take an optional `windowFocused`. Its default leaves every existing case unchanged.
- **ResultsScreenTalkBackTest:** `theSummaryIsSpokenOnTheShowingTheCelebrationLatchMarks` and `reEnteringCompositionOverTheRetainedLatchSpeaksTheSummaryNoSecondTime` advance past the arm before their 3 settle frames (48 ms is shorter than the 200 ms arm).
- **BoardSemanticsTest:**
  - the harness composes the slab as the hosts now do: an explicit group, with `BoardStop` as its first child;
  - the `slab()` helper becomes `boardStop()`, reading `BOARD_STOP_TEST_TAG`;
  - in `theBoardStopSpeaksTheBoardAndItsPegsLeftWithNoRole`, the traversal-group check moves to the slab, and the stop's index is pinned;
  - native graphics on the class, and the touch-mode check in `showLive`.

## Outputs
- Production: `ui/components/BoardAnnouncement.kt`, `BoardCanvas.kt`, `BoardCellOverlay.kt`, `TouchExploration.kt`; `ui/game/GameScreen.kt`, `BannerAction.kt`; `ui/onboarding/TutorialScreen.kt`, `TutorialAnnouncements.kt`; `ui/results/ResultsScreen.kt`.
- New test helpers: `testing/AccessibilityServices.kt` and `testing/TouchMode.kt`.
- New test cases: the 15 named under the red tests above, plus `ResultsScreenTalkBackTest.aRestoredResultsSendsTheServiceNoSecondSummary` and `GameScreenTalkBackTest.aServiceIsHandedTheBoardsChannelAsTheWindowsOnlyLiveRegionAndALeaf`, which were green before the fix and stay as guards.

## Checks
- `./scripts/check.sh --max-workers=4 --console=plain`:
  - F0 at b84853e — pass (BUILD SUCCESSFUL in 11m 13s)
  - F1, first run — fail (detekt MaxLineLength: a 129-character KDoc line in `BoardAnnouncement.kt:59`); I fixed the line
  - F1, re-run — pass (11m 20s)
  - F2 — pass (11m 8s)
  - F3 — pass (11m 25s)
  - F4 — pass (11m 9s)
  - F5 at 2236207 — pass (11m 11s; `:app` 1,325 tests, 0 failures, 0 skipped; 0 `w:` lines)
- §12 tripwire line 1 (`git diff --name-only develop...HEAD | grep -E '<start-path files>'`) — prints nothing.
- §12 tripwire line 2 (`git diff develop...HEAD -- …/BoardCanvas.kt | grep -E '^\+.*(Layout|focusable|semantics|boardStop|BoardCellOverlay)' | grep -v -E '^\+\s*//'`) — prints only `+            BoardCellOverlay(`.
- `git diff --name-only 75a03be..HEAD -- app/src/main | sort`:
  ```
  app/src/main/java/com/app/pegsolitaire/data/SettingsRepository.kt
  app/src/main/java/com/app/pegsolitaire/ui/components/BoardAnnouncement.kt
  app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt
  app/src/main/java/com/app/pegsolitaire/ui/components/BoardCellOverlay.kt
  app/src/main/java/com/app/pegsolitaire/ui/components/TouchExploration.kt
  app/src/main/java/com/app/pegsolitaire/ui/game/BannerAction.kt
  app/src/main/java/com/app/pegsolitaire/ui/game/GameScreen.kt
  app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialAnnouncements.kt
  app/src/main/java/com/app/pegsolitaire/ui/onboarding/TutorialScreen.kt
  app/src/main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt
  app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsScreen.kt
  app/src/main/java/com/app/pegsolitaire/ui/settings/SettingsUiState.kt
  app/src/main/java/com/app/pegsolitaire/ui/theme/Clay.kt
  ```
  `SettingsRepository.kt`, `SettingsScreen.kt`, `SettingsUiState.kt` and `Clay.kt` come only from F0's merge of develop (reduced-motion-live-gate). Every other file is in the ruling's table.
- `git diff --name-only b84853e..HEAD` on the ruling's not-edited list (`GameStatusStrip`, `TimerDigits`, `NoWinBanner`, the three ViewModels, `navigation/`, `di/`, `ui/home/`, `ui/theme/`, `:domain`, `strings.xml`, `dimens.xml`) — empty.
- `BoardCanvas`'s read-only branch is unchanged; F1 removed only the parameter, its KDoc and the interactive chain's `.boardAnnouncement(…)` call.
- Every commit from F0 to F5 has exactly one `Co-Authored-By:` line. The working tree is clean.

## Open issues / risks
- The F1 deviation (a one-pixel region drawn last) needs the auditor's and tech-lead's acceptance. Whether TalkBack speaks a one-pixel polite region is for the device to show (QA's TB-9 and TB-8).
- `bugfix/home-chip-arrival` is still not on develop (now at 8047549). When it lands, the ruling's second `git merge develop` comes before QA, and `theBoardStopFollowsTheTitleAndPrecedesTheFirstCell` will need its predecessor changed from the title to the timer.
- Native graphics makes the three focus test classes somewhat slower. The full Check still runs in about 11 minutes.

## Handoff
Next:
- **producer** — write this report to `.planning/reports/talkback-board/android-game-dev-loop3.md` and commit it alone; schedule the second develop merge once home-chip-arrival lands.
- **code-auditor** — loop 3 per §11.2 on `75a03be..2236207`, plus F0's conflict hunk. P1 and P2 now mean moving the channel back onto the canvas or the column. Deviations 1–3 and 6 need its reading.
- **qa-engineer** — `qa-engineer-device2.md` per §11.3.
- **tech-lead** — sign-off per §11.5, including the F1 deviation.
