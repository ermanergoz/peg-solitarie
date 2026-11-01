# tech-lead report — talkback-board (ruling: the device defects B-1 to B-9)
Date: 2026-10-24
Branch: feature/talkback-board (report only; head `907aedb`, last code commit `75a03be`)
Skills loaded: ergoz-soft:standards (role tech-lead; the product snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose, stacks, testing, dispatch; logging, puzzle-design and play-store are identical to the snapshot; the product's `.claude/rules/lessons.md`), android-architecture, android-gradle-logic, android-accessibility
Ran in parallel with: the ux-designer's B-5 ruling on this branch, home-chip-arrival code-auditor loop 2, colour-vision QA, licences-screen QA re-run, play-upload developer
Ran on: claude-opus-5-5 at effort xhigh — **staffing deviation:** `agents/tech-lead.md` pins fable at xhigh; the Fable limit is reached and founder answer Q-019 (option 3, re-confirmed 2026-10-24) puts every Fable-pinned role on Opus until 3.0.0 is published. The code under review was also written on Opus. Mitigation: each cause below cites a line of Compose UI 1.11.3, Compose Foundation 1.11.3 or Robolectric 4.16 source, or a QA artifact, listed under Checks, so a later reader can re-read it.
Fan-out: none
Loops: 0
Read in full: `.planning/reports/talkback-board/qa-engineer.md`, `tech-lead-brief.md` (§0–§14, Amendments 1 and 2), `android-game-dev.md`, `code-auditor.md`; `PegSolitarie-home/.planning/reports/home-chip-arrival/tech-lead-defect-ruling.md`. In part: `docs/ux-spec.md` §11.1 (`:5059-5367`), criteria 23d–29 and TB-1 to TB-12 (`:6257-6634`), §12's Landscape row (`:5506`)
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **B-1, B-4, B-9 share a cause:** Compose drops the live region from the accessibility tree whenever later-drawn stops cover it. On T2/T3 the nine cells tile the canvas, and in landscape the clipped board is covered too. Results' region sits on a container, so TalkBack reads the whole column. **Fix:** the channel becomes an empty leaf at each screen's root, armed once accessibility services know it.
- **B-3:** the board stop sits on the slab, which has children, so Compose leaves it out of its reading order. It is the same mechanism as home's timer. **Fix:** the board stop moves to a leaf of its own inside the slab.
- **B-2:** the button's `clickable` cannot take focus in touch mode, and Robolectric runs outside touch mode, which hid it. My own brief's §6 assumption was wrong. **Fix:** a gated `focusProperties`, and every focus test runs in touch mode.
- **B-8:** focus is moved while the dialog window still has window focus. **Fix:** every focus move waits for window focus. **B-7:** confirmed as pre-ruled.
- **B-5 and B-6** need ux-designer rulings. The options and their costs are in §5 and §6. No 15e sitting is needed. The developer's loop, the auditor's probes and QA's re-run list are in §11.

## 1. B-1 — the tutorial's spoken channel goes silent after the first step swap

**Cause: the live-region node is pruned from the accessibility tree once the cells cover it.**
1. **Compose prunes covered nodes.** The node provider TalkBack reads is built by `SemanticsOwner.getAllUncoveredSemanticsNodesToIntObjectMap` (ui 1.11.3, `SemanticsOwner.kt:157-330`), "a semantics node completely covered by siblings drawn on top of it will be pruned":
   - children are visited last-drawn first;
   - every node that `isImportantForAccessibility()` (`:134-137`; `ContentDescription` and `LiveRegion` are `AccessibilityKey`s, `SemanticsProperties.kt:41`, `:85`, `:508`) has its touch bounds subtracted from the unaccounted space;
   - a node whose bounds no longer intersect that space is never added.
2. **The cells cover the canvas on T2 and T3.**
   - `BoardCanvas`'s interactive branch draws the Canvas, which is the live region, and then `BoardCellOverlay` (`BoardCanvas.kt` at `75a03be`, the `Box` with the Canvas, then the overlay).
   - Every cell stop has a description and a click, so every one is important.
   - T2 and T3 are the full 3 × 3 square, all nine cells in play (`domain/.../tutorial/TutorialBoards.kt`, `TutorialLayout.square`).
   - `BoardGeometry.forCanvas` makes `cellSize = min(w, h) / gridDimension` (`BoardGeometry.kt:107-123`), and the overlay places each stop on its own square.
   - The nine stops therefore tile the canvas: on the 420-dpi device 248 dp = 651 px = 3 × 217 px; at the tests' `hdpi`, 372 px = 3 × 124 px.
   - The canvas leaves the tree.
3. **A node outside the tree sends nothing.**
   - `sendSemanticsPropertyChangeEvents` walks only `currentSemanticsNodes` and skips any id without a previous copy (`AndroidComposeViewAccessibilityDelegateCompat.android.kt:2564-2575`).
   - So from the first step swap on, no line reaches TalkBack: the step lines, the coach, rejected taps, jumps and the done line.
4. **Why step 1 and gameplay speak.** T1 is a 1 × 3 strip in a 3 × 3 grid, so two thirds of the canvas stay uncovered, and step 1 speaks. English-33's blocked corners do the same on gameplay, which is why gameplay passed.
5. **Why the JVM was green.**
   - `createComposeRule`'s finders read the SemanticsOwner tree, which is never pruned.
   - Only the node provider prunes, and no test read it.
6. **"The coach spoken as a cell's description" follows from the same loss.** Inferred: TalkBack's source is not on this machine.
   - With the canvas gone, the only description changes a coach escalation sends are the two hinted cells' (`:2716-2721`).
   - Accessibility focus is then on the board stop, which is the slab and their ancestor.
   - TalkBack speaks a description change inside the focused node's subtree. The log shows this only while the board stop holds focus (`speech-log.txt` 20:39:09, 20:40:00 after a board-stop focus). Step 1's coach, with a cell focused, spoke its "Hint: …" line correctly (20:38:39).
   - §3's fix removes the ancestor relation as well.

**Mechanism: the channel moves to an uncoverable, armed leaf.** This supersedes brief A1.3, which put the region on the Canvas; F-44's condition still holds, because no component chooses its own words.
1. **One composable for every host.** `BoxScope.AnnouncementRegion(announcer: BoardAnnouncer)` goes in `ui/components/BoardAnnouncement.kt`. It is an empty `Box(Modifier.matchParentSize())` carrying:
   - `testTag(ANNOUNCEMENT_REGION_TEST_TAG)`;
   - `onPlaced { announcer.arm() }`;
   - `semantics { liveRegion = Polite; traversalIndex = AnnouncementTraversal.LAST; announcer.current?.let { contentDescription = it } }`.
   It has no children and no click.
2. **It is the first child of each screen's root, drawn under everything:**
   - gameplay: a new `Box(modifier.fillMaxSize())` wrapping the `Scaffold`, which takes `Modifier.fillMaxSize()`;
   - tutorial: the existing inner `Box(Modifier.fillMaxSize())`;
   - Results: the existing root `Box`.
3. **Why it holds.**
   - **Never pruned.** It is pruned only if the screen's important nodes tiled the whole window, and none does: the system-bar insets, the margins and the gaps between controls carry no important node.
   - **It covers nothing.** It is drawn first, so it subtracts its bounds only after everything above it has been visited.
   - **It is a leaf,** so TalkBack has nothing extra to read when it changes (see B-4).
   - **Not a stop on opening.** While it holds a line it would be a speaking leaf at (0, 0), Compose's first stop. `AnnouncementTraversal.LAST = Float.MAX_VALUE`, a named constant in a scoped object, puts it after every other stop in the root group, so TalkBack's first focus on a new screen does not land on it.
4. **The canvas carries nothing again.** `BoardCanvas` loses its `announcement` parameter. The change is in the interactive branch only: the read-only branch and the drawing stay byte-identical. The hosts stop passing it.
5. **The arm: no line before accessibility services know the node.**
   - A property change is sent only for a node already in the previous copy (`:2572`).
   - The delegate refreshes that copy at most every `SendRecurringAccessibilityEventsIntervalMillis` = 100 ms (`:275`, `:2371-2410`).
   - A line set in a new screen's first frames can therefore arrive with the node and never be sent. That race is the step-1 line missing on run 2's opening (`t2-00-open-12s.png`) and B-4's missing summary.
   - So `BoardAnnouncer.arm()`, on the region's first placement, waits `ANNOUNCEMENT_ARM_GAP_MS = 200L`. The constant is named, with that KDoc.
   - A line asked for before the arm is held, the newest replacing an older one (the spec's §11.1 channel rule), and is put on the region when the arm completes. After the arm, `announce` is as today.
6. **Unchanged:** `ANNOUNCEMENT_HOLD_MS`, `REPEAT_MARK`, the generation-guarded clear, every source effect and its order.
7. **Rejected:**
   - **Keep the region on the Canvas and keep the canvas uncovered.** This needs stops smaller than the cell rectangle (the spec's §11.1: "its own rectangle on the board"), or a canvas larger than its drawing (the frozen draw and `BoardGeometry`).
   - **A region on a parent of the cells** (the board's `Box`, the overlay, the slab). A live-region container is re-read whole on any relayout (B-4's mechanism), and that would be 33 cells.
   - **A region leaf inside the slab.** It would cover §3's board-stop leaf, or be covered by it. Both are slab-sized, and only one of two equal rectangles survives the pruning.

**Tests, red first.**
- **The instrument is the accessibility layer.**
  - The node provider: `composeRule.composeHost().accessibilityNodeProvider`, as `TimerDigitsTest`'s N-2 case reads it.
  - With accessibility enabled (`ShadowAccessibilityManager.setEnabled(true)`, one entry in `setEnabledAccessibilityServiceList`, touch exploration on; the delegate's `isEnabled`, `:294-299`), the events Compose sends: `shadowOf(manager).sentAccessibilityEvents`. A description change carries its text in `event.contentDescription` (`:1289-1306`, `:2716-2721`).
  - One shared helper sets all of it up.
- **`TutorialTalkBackTest`:**
  - after step 2 installs, the region's `AccessibilityNodeInfo` is non-null, has `liveRegion == View.ACCESSIBILITY_LIVE_REGION_POLITE` and `childCount == 0`, and its description is the step-2 line. Red today: on T2 the canvas's info is null.
  - a `TYPE_WINDOW_CONTENT_CHANGED` / `CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION` event carrying the step-2 line is sent; so are the coach line, a rejected tap's line and a jump's line on T2, and the done line on T3. Red today.
  - the arm: at the first frame (paused clock) the region carries no line; after `ANNOUNCEMENT_ARM_GAP_MS` plus a frame it carries the step-1 line, and its event has been sent. Red today: the first effect sets the line.
- **`GameScreenTalkBackTest`:** the region is in the provider's tree and is the gameplay window's only live region besides the banner's and the notice's own (23k, 23d), under `TALKBACK_SCREEN_QUALIFIERS` and under `HEIGHT_LIMITED_QUALIFIERS` with the banner raised (B-9).
- **Retargets.** Every assertion that reads the channel off `BOARD_CANVAS_TEST_TAG` moves to `ANNOUNCEMENT_REGION_TEST_TAG`. This includes audio's `GameScreenWrongTapTest` helpers `boardAnnouncement()` / `assertAnnounced()` (`:300-316`, an existing file, already edited on this branch under D-TBa). The developer's report names each edited test; nothing else in them changes.
- **The device** decides whether TalkBack speaks the lines: §11.3.

## 2. B-2 — completion never focuses "Got it"

**Cause: the button cannot take focus in touch mode.**
1. **The request reaches a focus target that refuses it.** `CompletionAnnouncement` requests `gotItFocus` (`TutorialAnnouncements.kt:87-103`). That reaches the M3 `Button`'s focus target, which `clickable` creates with `Focusability.SystemDefined` (foundation 1.11.3, `Clickable.kt:1705-1707`).
2. **`SystemDefined` refuses in touch mode.** It can focus only when `LocalInputModeManager.inputMode != InputMode.Touch` (ui 1.11.3, `Focusability.kt:64-72`). The request fails, and no focus event is sent.
3. **The device is in touch mode.** A phone starts in touch mode. TalkBack's double-tap reaches a node as an accessibility click, not as a touch-down, so nothing takes the window out of it.
4. **Why the other moves work.** The cells and the board stop use `Modifier.focusable()`, which is `Focusability.Always` (`Focusable.kt:152-154`). That is why their moves work on the same device.
5. **Why the JVM was green.**
   - Robolectric 4.16 starts outside touch mode: `ShadowWindowManagerGlobal.WindowSessionDelegate` initialises `inTouchMode` to `ShadowView.useRealGraphics()` (javap), which is false without native graphics.
   - Compose therefore ran in `InputMode.Keyboard` (`AndroidComposeView.android.kt:923-925`), where `SystemDefined` can focus.
6. **The error was mine.** Brief §6 called the button "already focusable: it is clickable". That is true only outside touch mode.

**Mechanism.**
- **The fix.** In `TutorialFooter`, the button's modifier gains `focusProperties { if (touchExploration) canFocus = true }`, ahead of the button's own `clickable`. `FocusTargetNode.fetchFocusProperties` (`:240-246`) starts from the target's focusability, then lets every focus-properties modifier above it, up to the next focus target, override it. `TutorialFooter` takes `touchExploration`.
- **TB-11 is untouched.** The override is gated on touch exploration, so with it off nothing changes.
- **Fallback.** If the override does not take, use `focusable(enabled = touchExploration)` on the same modifier, ahead of `focusRequester`, so the requester targets it. The report says which form ran.

**Tests, red first.**
- **The harness change.** Every focus-move test in `TutorialTalkBackTest`, `GameScreenTalkBackTest` and `BoardSemanticsTest` runs in touch mode, the device's state:
  - one shared helper calls `InstrumentationRegistry.getInstrumentation().setInTouchMode(true)` before `setContent`. Robolectric's `ShadowInstrumentation.setInTouchMode` sets `ShadowWindowManagerGlobal`'s flag (javap);
  - the helper asserts the host view's `isInTouchMode`.
  - Fallback: provide `LocalInputModeManager` with an `InputMode.Touch` manager.
- **Red today:** `completingTheTutorialSpeaksTheDoneLineAndMovesFocusToGotIt` in touch mode.
- **Every other focus case must stay green in touch mode.** One that does not names another target that cannot take focus on a phone, and it is fixed in the same commit.
- **The device:** completion → "Got it", focused and spoken, twice (§11.3).

## 3. B-3 — the board stop is out of the 26b order (this branch's half)

**Two halves.**
- **The timer's half is not this branch's.** It is home-chip-arrival's B-3: the clock's node has hidden children, so Compose leaves it out of its order. That ruling's §3 fixes it with `clearAndSetSemantics` in `TimerPill`, on `bugfix/home-chip-arrival` at `925f36f`. It reaches this branch through `develop`, and this branch's developer does not touch `GameStatusStrip.kt` or `TimerDigits.kt`.
- **The board stop's half is this branch's,** by the same mechanism, and it is the only part of QA's 26b failure in this diff. The cells keep their place; only the board stop moves. That is why the tutorial, which has no timer, fails as well.

**Cause: Compose leaves the board stop out of its reading order.**
1. **Who counts.** Compose writes traversal-before hints only between nodes it counts as screen-reader focusable (`setTraversalValues`, `:3427-3454`). A node counts when it merges its descendants, or when it is an unmerged leaf with something to say (`isScreenReaderFocusable`, `:3456-3468`; a leaf means `replacedChildren.isEmpty()`, `SemanticsNode.kt:100-106`).
2. **The board stop counts as neither.** `Modifier.boardStop` sits on the slab `Surface` (`GameScreen.kt:248-265`, `TutorialScreen.kt:163-176` at `75a03be`). The slab's semantics children are the canvas, the cell layer and the cells, and it does not merge.
3. **So it has no place in the order.**
   - Inside the slab's traversal group, a container that is not screen-reader focusable "is removed" and only its children are listed (`SemanticsSort.kt`, `sortByGeometryGroupings`, the `containerIsScreenReaderFocusable` branch).
   - The board stop gets no hint either way, and reports `isScreenReaderFocusable = false` (`:1145`).
4. **TalkBack still focuses it,** as a visible node with a description and a click, but places the un-hinted node first. That gives QA's gameplay order, timer → board stop → back → title → cells, with two un-hinted nodes at the front; and on the tutorial, board stop → pips → Skip → cells.
5. **QA's lead is answered.** A2.1's `traversalIndex` and the slab's `isTraversalGroup` order nodes already in the order. Neither can bring the board stop in, and both stay.

**Mechanism: the board stop on a leaf of its own, inside the slab.**
1. **The leaf.** A new `BoardStop(label, focusRequester, focusEnabled, modifier)` composable in `BoardCellOverlay.kt` is an empty `Box` (or `Spacer`) carrying:
   - `testTag(BOARD_STOP_TEST_TAG)`;
   - `Modifier.boardStop(…)`, minus `isTraversalGroup`;
   - `semantics { traversalIndex = BoardTraversal.STOP_BEFORE_CELLS }`, a named `-1f` in a scoped object in `ui/components/`. The cells keep their row-major `0f…n-1`, and `UnmergedConfigComparator` sorts the group by index, so the stop is read first.
2. **Each host composes it as the slab's first child,** with `Modifier.fillMaxSize()`, before `BoardCanvas`. The slab's size is set by its own modifiers (gameplay's width, cap and aspect ratio; the tutorial's `size`), and the `Surface`'s content box propagates its minimum constraints. So `BoardCanvas` is measured exactly as today.
3. **The slab keeps** `isTraversalGroup = true`, now set explicitly on the slab by each host (A1.2 item 8's reason), its test tag, and on gameplay A2.1's `GameTraversal.BOARD_BEFORE_BANNER`.
4. **Why it holds.**
   - **It is a leaf with a description,** so Compose puts it in the order: timer, or Skip → board stop → row 1's first cell.
   - **It is never pruned.** `BoardCanvas` sits inside the slab with `padding(spacing_md)` on both hosts, which leaves a 16 dp ring of the leaf around the cells.
     - A gameplay cell below 48 dp has its touch bounds widened to 48 dp (`useMinimumTouchTarget` for nodes with a click, `SemanticsModifierNode.kt:126-145`; `NodeCoordinator.kt:975-1000`, `:1466-1470`). The cell layer does not clip, so the widening can reach into the ring.
     - At most it reaches (48 − 32.9) / 2 ≈ 7.6 dp, on a 9 × 9 board at 360 dp, which is less than the 16 dp ring, so part of the ring always stays uncovered.
   - **It covers no cell,** because it is drawn beneath them.
   - **Explore-by-touch on the rim finds it,** since the cells sit above it only inside the ring.
   - **The four focus moves target its requester,** and the no-op click and the touch-exploration gate are unchanged.
   - **Not a descendant of the cells.** The cells are no longer its descendants, which removes B-1's coach-as-cell path.
5. **Rejected:**
   - **`mergeDescendants = true` on the slab.** It folds the canvas's and the cell layer's semantics into the board stop's merged node. That breaks every finder of `BOARD_CANVAS_TEST_TAG` in the merged tree, including the pre-branch `GameFlowTest.tapCell` and `TutorialScreenTest.tapCell`, and any semantics a later branch adds to the canvas would merge into "Board, N pegs left".
   - **`clearAndSetSemantics`,** home's lever: it removes the cells.
   - **A `traversalIndex`:** see cause 5.

**Tests, red first.**
- **`BoardSemanticsTest`, through the node provider:** the board stop's `AccessibilityNodeInfo.isScreenReaderFocusable` is true and its `childCount` is 0. Red today: the slab's is false.
- **`TutorialTalkBackTest`, with accessibility enabled:** `addExtraDataToAccessibilityNodeInfo(…, ExtraDataTestTraversalBeforeVal, …)` (`:391-396`, `:1162-1180`):
  - on Skip it gives the board stop's id. Red today: it gives a cell's.
  - on the board stop it gives row 1's first cell.
- **`GameScreenTalkBackTest`, the same reads:**
  - the board stop → row 1's first cell;
  - the board stop's predecessor is the strip's last stop: the timer once home has merged, the title before. The developer asserts whichever the tree holds and says which.
- **If Robolectric computes no hints,** the provider assertion carries the JVM half (as in home's ruling, §3), and the device half is 26b.
- **Retargets.** TB-1's existing assertions (the no-op click, no role, the gate, `Focused`) move from the slab node to `BOARD_STOP_TEST_TAG`. These are this branch's own tests.
- **A2.1's case stays as it is:** the region is a group and the slab's index is below the banner's.

## 4. B-4 — Results re-reads its whole column, and the summary is unreliable

**Cause: the region is on a container, and the summary races the screen's first frames.**
1. **The region sits on a container.** Results' channel is on the content `Column` (`ResultsScreen.kt`, `.boardAnnouncement(announcer.current)` on the column at `:133`). Its subtree holds every Results stop and the count-up.
2. **Compose sends the Column events of its own.**
   - **On every relayout.** For any layout node that is laid out again, Compose sends `TYPE_WINDOW_CONTENT_CHANGED` / `CONTENT_CHANGE_TYPE_SUBTREE` for its nearest semantics node (`sendSubtreeChangeAccessibilityEvents`, `:2468-2505`, from `onLayoutChange`, `:2417-2430`). The Column is that node whenever it places its children again: each step of the count-up changes a child's width under `Arrangement.Center`, and a rotation or a theme change rebuilds it.
   - **When the hold ends.** Withdrawing the summary is a deleted property, which sends `CONTENT_CHANGE_TYPE_UNDEFINED` (`:2900-2908`).
3. **TalkBack reads the whole column.** The Column is a live region with no description of its own, so for each such event TalkBack speaks it from its subtree: QA's "Perfect! | 3 of 3 stars | 3100 | SCORE | … | Home | Button".
   - The mid values (3067, 2895) are the count-up's relayouts.
   - The reads twice per rotation are the rebuild, then the count-up's end.
4. **"The summary is unreliable"** is the arm's race of §1 (mechanism 5):
   - the summary is set by a `LaunchedEffect` in the new screen's first frames;
   - a description change is sent only for a node the delegate already holds (`:2572`);
   - on Challenge 1 the line arrived with the node, no change event was sent, and only the whole-column reads followed. English-33 won the race.
   - The auditor's open risk (live region on the count-up column) is confirmed.

**Mechanism.**
- **§1's `AnnouncementRegion`** becomes the first child of Results' root `Box`, and the column loses `.boardAnnouncement`. The leaf has no children, so a relayout or a withdrawal sends TalkBack an empty region, which speaks nothing.
- **The arm** delays the summary until the leaf is known.
- **Unchanged:** the `rememberSaveable` "spoken" flag (loop 1, F-1), the star row's merged stop and TB-8's latch.

**Tests, red first** (`ResultsScreenTalkBackTest`, accessibility enabled, through the provider and the sent events).
- **(a)** Walking the provider's nodes from the root, exactly one node has `liveRegion != NONE`, and it has `childCount == 0`. Red today: the Column.
- **(b)** With `playCelebration = true`:
  - the summary is absent at the first frame and present after the arm;
  - exactly one description-change event carries it, across the count-up (advance the clock through its duration) and the hold's end.
  - Red today for the first-frame half.
- **(c)** After `StateRestorationTester.emulateSavedInstanceStateRestore()`, no event carries the summary.
- **The device:** TB-8 twice (§11.3).

## 5. B-5 — a jump taken through TalkBack's menu leaves focus on the source (options for the ux-designer)

**What happens (QA's evidence; TalkBack's half inferred).**
- The jump action runs while TalkBack's own menu window ("TalkBack menu" → "Actions") holds TalkBack's accessibility focus.
- The overlay's move (`BoardCellOverlay.kt:82-85`: clear, wait 200 ms, request the destination's input focus) does run. TalkBack does not follow it into a window it is not focused in.
- About 7 s later, when the Actions window closes, TalkBack restores accessibility focus to the node it left: the source, which now says "empty".
- Through the Actions reading control there is no second window, and the move works 33 of 33.
- **Dependency:** the user-facing requirement for the menu route is the ux-designer's ruling, running in parallel. The developer's loop does not wait on it except for option B's one commit.

| Option | What ships | Cost | Risk |
|---|---|---|---|
| **A. Accept TalkBack's placement on the menu route** | Reading control: focus on the destination, as today. Menu: focus back on the source, and the jump's line names the destination. TB-7's row and the spec's §11.1 focus table are amended for the menu route only | No code. One spec amendment; QA judges the menu route against it | None |
| **B. Drop the jump-action focus move on both routes** (F-TB-1's fallback, row (i) only) | Both routes: focus stays on the source cell | One commit: remove `onJumpTaken`'s move; retarget the jump-action focus assertions in `BoardSemanticsTest` and `GameScreenTalkBackTest` to "stays on the source" (this branch's own tests) | Loses the move that works on the reading-control route |
| **C. Move focus once the app window has window focus again** | Spec as written, if TalkBack's menu takes window focus from the app | None beyond §8's change: §8 makes every move wait for window focus | Likely no effect: an accessibility overlay usually takes no window focus, so the move runs at 200 ms as today. The device decides |
| D. Re-request focus after a fixed delay | — | Rejected: it guesses TalkBack's ~7 s and could pull focus long after the player moved on | — |
| E. App-driven accessibility focus through the node provider | — | Rejected: it needs the node's virtual id, which production code can only get through test APIs (`RootForTest`), and TalkBack owns accessibility focus | — |

**My technical recommendation: A.** C rides §8 at no cost, so QA records whether the menu route still returns to the source; if it lands on the destination, the spec as written is met and A's amendment is not needed. B only if the ux-designer wants the two routes identical.

## 6. B-6 — the winning jump's line is never spoken

**Cause: the screen is gone before the line can be sent.**
1. **The win navigates at once.** The winning jump's update sets `status = Won` and `animating` together (`GameViewModel.kt:925-946`). `freezeAndFinishIfTerminal` then sends the one-shot finish in the same call (`:948`, `:960-1000`), and `GameRoute` navigates to Results on receipt (`GameRoute.kt:63-79`).
2. **The line never leaves the screen.**
   - `JumpAnnouncements` puts the line on the channel as the animation begins.
   - The gameplay screen leaves composition within a frame or two.
   - The delegate batches property changes every 100 ms (`:275`, `:2410`), and the node is gone from the next copy, so nothing is sent.
3. **The log agrees.** The double-tap is at 21:24:14.150, the first utterance is Results' summary at 21:24:14.984, and nothing comes between (`speech-raw.log:5807-5808`).
4. §1's leaf does not help: it leaves with the screen.

**Every code path that delivers the line costs more than a Minor should in this loop.** So this is a requirement question for the ux-designer, a new dependency the producer adds to the B-5 dispatch.

| Option | Mechanism | Cost and risk |
|---|---|---|
| **(a) Delay the route under touch exploration** | `GameRoute` waits ~200 ms before `onFinished` while touch exploration is on | The finish is a CONFLATED one-shot consumed on receipt. A rotation inside the delay cancels the collector and loses it, which strands a won board, unless the pending `Results` is saved across recreation. Whether TalkBack then speaks the line before Results' summary is unknown. Risky; I do not rule it into this loop |
| **(b) Carry the winning move to Results** | `Results` gains the move; `ResultsViewModel` exposes it; Results' channel speaks "<jump line> <summary>" as one utterance, in the spec's §11.1 order | Robust. But `navigation/` is on tech-design §10's start-path list, so it costs a 15e sitting, and `ResultsViewModel` is frozen for this branch. A 3.0.1 candidate |
| **(c) Amend the spec's §11.1 "When a jump wins…" and TB-6** | A winning jump speaks Results' summary only. The summary already names the headline, the stars and the pegs left | No code |

**Recommendation: (c) for 3.0.0; (b) as the 3.0.1 candidate if the ux-designer wants the line.**
- Red-first test for (b), if it is chosen later: `ResultsScreenTalkBackTest`, "a result reached by a winning jump speaks that jump's line, then the summary, as one line".
- Under (c) there is no developer work, and QA reads TB-8's summary after a win.

## 7. B-7 — the selection spoken twice (pre-ruled; confirmed)

**Confirmed: brief A1.2 item 7 (D-TBa item 11) applies as written.**
- **The cause** is the one the pre-rule anticipated. A selection changes the focused cell's description in the same frame as the line. Compose sends a `CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION` event for that cell (`:2716-2721`), and TalkBack re-reads its focused node. The device answered yes, every time (QA device question 3, `g-11-select-4s.png`).
- **The mechanism, one commit.** `SelectionAnnouncements` drops its selected branch while `rememberTouchExplorationEnabled()` is true. The put-down line, and every other line, stays.
- **Why that covers every selection.** Under touch exploration every board selection is made on the focused cell. A jump action's selection tap is replaced by the jump line in the same frame.

**Test, red first** (`GameScreenTalkBackTest`, `TutorialTalkBackTest`):
- with touch exploration on, a selection by `OnClick` puts no line on the region within the hold;
- the put-down that follows still puts its line.
- **Retarget:** `GameScreenWrongTapTest.aSelectionAndAPutDownEachAnnounceTheirOwnLine` keeps its selection half only where touch exploration is off; the report says which.
- **The device:** one utterance per selection, the cell's re-read.

## 8. B-8 — a banner Restart confirmed in its dialog puts focus on the timer

**Cause: the move is made while the dialog window still holds window focus.** The window model is the platform's; TalkBack's half is inferred from the log.
1. **When the move runs.** `RestartConfirmDialog` is its own window. On confirm, `rememberBannerFocusMove` takes the awaited Restart when `restartTick` rises (`BannerAction.kt:40-42`) and runs `moveInputFocusTo(boardStopFocus)`. That is the same update that closes the dialog.
2. **TalkBack ignores it.** The gameplay window's input focus does change, but TalkBack acts on focus events in the active window, and the dialog is still the active window.
3. **TalkBack places focus itself.** When the dialog window goes, TalkBack places focus in the window it returns to. The node that had focus before the dialog, the banner's Restart, has left with the banner, so TalkBack falls back to its first un-hinted node, the timer (QA: "Board restarted. 4 pegs left.", then "Peg Solitaire" for the window, then "Elapsed time"; `nw-06-confirmed.png`).
4. **Why the dock's Restart passes.** Its button survives, and TalkBack restores focus to it.

**Mechanism: every focus move waits until the app window has window focus.**
- **The change.** `moveInputFocusTo(target, windowInfo)` in `TouchExploration.kt` first awaits `snapshotFlow { windowInfo.isWindowFocused }.first { it }`. Then it clears, waits `FOCUS_MOVE_GAP_MS` and requests, as today.
- **Who passes it.** Each caller (`BoardCellOverlay`, `TutorialAnnouncements`, `BannerAction`) reads `LocalWindowInfo.current` in composition and passes it. `WindowInfo.isWindowFocused` is public and kept current from `onWindowFocusChanged`.
- **What changes and what does not.**
  - A newer move cancels a waiting one, because each move runs in its own keyed effect.
  - A move made with the window focused, which is every move but this one today, behaves as before.
  - The gate on touch exploration is unchanged.
- **The same change is §5's option C.**
- **Fallback, pre-ruled.** If the device still lands elsewhere after this, TB-7's row for a dialog-confirmed banner Restart falls to TalkBack's own placement. The ux-designer amends the row, as under F-TB-1's fallback, and no further code is written.

**Test, red first** (`GameScreenTalkBackTest`, in touch mode).
- **The fake window.** Provide a `WindowInfo` whose `isWindowFocused` the test controls, delegating everything else to the real `LocalWindowInfo.current`, through `CompositionLocalProvider`.
- **The case:**
  - a no-win board, the banner's Restart, the dialog shown;
  - window focus set false; confirm; advance past the gap: the board stop is not `Focused`. Red today: it is.
  - window focus set true; advance past the gap: the board stop goes from not focused to focused.
- **The decline case** stays as it is (no move).
- **The device:** the dialog-confirmed banner Restart lands on the board stop (§11.3).

## 9. B-9 — landscape banner Undo loses its line

**Cause: two things, of which only the first is a defect.**
1. **The lost "Undid …" line is §1's pruning.**
   - In landscape the board is clipped (§10's B-10): rows 3–5 show, and the banner and the dock are drawn over the rest.
   - The visible part of the canvas is covered by the 21 visible cells' widened touch bounds, the banner and the dock, so the region leaves the tree.
   - The evidence: the line is lost only where the visible canvas is covered; portrait is 3 of 3 correct.
2. **The flash is TalkBack's own focus recovery.** The focused banner Undo leaves, TalkBack puts focus on the first stop, and the move arrives 5 ms later: "Peg Solitaire" FOCUSED at 21:44:00.627 is cut by "Board, 27 pegs left" at 21:44:00.632 (`speech-log.txt:664-665`). That is not audible, and the move lands where TB-7 says.

**Mechanism.** §1's leaf, which no layout can cover. No code of its own.

**Test.** §1's region test under `HEIGHT_LIMITED_QUALIFIERS` with the banner raised. The device: three landscape banner Undos, each the undo line, then the board stop (§11.3).

**Pre-ruled.**
- If the line is still lost in landscape after F1, what remains is B-10's: landscape is unsupported in this refresh (§10). It does not hold the merge.
- The flash is recorded and not judged, unless it is heard in full.

## 10. The observations outside the diff

| Observation | Whose | Class | Blocks 3.0.0? |
|---|---|---|---|
| **B-10 — landscape clipping**: rows 3–5 only; TalkBack reaches 21 of 33 cells | Pre-existing gameplay layout. The slab is `widthIn(max).fillMaxWidth().aspectRatio(1f)` (`GameScreen.kt`), unchanged since `e40bea9` (phase 01): with the width fixed, the aspect ratio cannot fit a short height. Orientation is unlocked by the 15-founder-feedback sign-off's O-5 ruling ("landscape layout quality is a post-3.0.0 item for the ux-designer"), and §12 says "Landscape: not a supported orientation change in this refresh" (`docs/ux-spec.md:5506`) | **3.0.1** (ux-designer's layout, then the developer). An upgrade of O-5's wording: this is not layout quality but a board that does not fit, for every player who rotates, sighted or not | **No**, under O-5 and §12. The producer carries it in the Q-PUBLISH-3.0.0 summary as a known limitation, beside the cell-size deviation. TB-2 is read in portrait for 3.0.0, and A2.6's height-limited 26b read uses a wide portrait window instead (§11.3) |
| **The timer speaks no value** | `bugfix/home-chip-arrival` (§4.3.2, D-05e; its ruling §2, `88c52cb` "the timer speaks whole minutes") | Another branch's | Not this branch. It is home's own 18f/18g, and it arrives here through `develop` |
| **The banner re-announced on rotation** | `bugfix/hint-undecided`'s 23k region, on `develop`. A recreated screen composes the raised banner's live region with its text, and TalkBack speaks a region that appears with content | **Backlog / 3.0.1**: the ux-designer rules whether a still-raised banner may speak again after recreation (D-TBa item 7's "not on a rotation" covers Results' summary only). If not, the fix is in `NoWinBanner`, with this branch's first-composition rule | No: a true status said once more |
| **The UMP consent form over tutorial step 1** | The consent flow (v1.1 ads, `AdGateway` + UMP, asked at start) | **3.0.1** for the ux-designer: when consent is asked relative to first-run onboarding. The tutorial shows no ad, so asking after it is the shape to consider; the requirement is the ux-designer's | No. TB-9 was completed through it; QA's re-run handles it (§11.3) |
| **Results' count-up restarts on rotation or theme change** | Pre-existing Results celebration. The D-16 latch stays true across a configuration change (`ResultsViewModel.kt:514-530`); the same family as the auditor's routed `StarPopSounds` replay (`ResultsCelebration.kt:107`) | **Backlog / 3.0.1**, routed with `StarPopSounds` | No. Its TalkBack symptom (the whole-column reads with mid values) goes with B-4's fix |

## 11. What is owed after the fixes

**11.1 The developer — one loop.** android-game-dev, on opus, in `PegSolitarie-tb`. Each code commit is red first and green on `./scripts/check.sh`.

| # | Commit | Closes | Red first | Files |
|---|---|---|---|---|
| F0 | `chore(talkback): merge develop` | — | — | Merge, never rebase. `develop` is now at `f7dd1fe`: it adds reduced-motion-live-gate's hunks in `GameScreen.kt` and `TutorialScreen.kt`, so keep both sides. If `bugfix/home-chip-arrival` has landed by then, its `BoardCanvas.kt` frame-geometry hunk and `GameStatusStrip.kt` come with it: keep them, and do not re-edit the timer. If home lands later, a second `git merge develop` comes before QA's session (§11.3) |
| F1 | `fix(talkback): put the spoken channel on an uncovered leaf` | B-1, B-4, B-9's line, the first-line race | §1's and §4's tests | `BoardAnnouncement.kt` (`AnnouncementRegion`, the arm, `AnnouncementTraversal`); `BoardCanvas.kt` (`announcement` removed, interactive branch only); `GameScreen.kt` (root `Box`, region; the `LongMethod` surface list gains one line for the region); `TutorialScreen.kt`; `ResultsScreen.kt`; the channel tests retargeted to `ANNOUNCEMENT_REGION_TEST_TAG` |
| F2 | `fix(talkback): give the board stop a leaf in the reading order` | B-3 (the board's half) | §3's tests | `BoardCellOverlay.kt` (`BoardStop`, `BoardTraversal`, `boardStop` without the group), `GameScreen.kt`, `TutorialScreen.kt` (slab: explicit group, the leaf first) |
| F3 | `fix(talkback): let Got it take focus in touch mode` | B-2 | The touch-mode harness, then §2's red case | `TutorialScreen.kt` (`TutorialFooter`), the shared test helper |
| F4 | `fix(talkback): move focus only once the window has focus` | B-8 (§5 C) | §8's test | `TouchExploration.kt`, `BoardCellOverlay.kt`, `TutorialAnnouncements.kt`, `BannerAction.kt` |
| F5 | `fix(talkback): drop the selection line under touch exploration` | B-7 | §7's test | `BoardAnnouncement.kt` |
| F6 | Per the ux-designer's B-5 ruling | B-5 | — | A or C: nothing. B: one commit, `fix(talkback): keep focus on the source after a jump action` |
| — | B-6 | — | — | Nothing under (c). (a) is not ruled into this loop; (b) is 3.0.1 (§6) |

- **The report** appends a "Loop 3" section with:
  - every red, quoted;
  - each fallback taken (§2's `focusable` form; §3's provider-only JVM half) and why;
  - the existing tests edited, named, with the reason. Expected: the channel helpers in `GameScreenWrongTapTest` and any other file that reads the channel off `BOARD_CANVAS_TEST_TAG`. Everything else edited is this branch's own test;
  - the §12 tripwire's two lines (§11.4);
  - `git diff --name-only 75a03be..HEAD -- app/src/main | sort`, which must list only the files in the table.
- **Not edited:** `GameStatusStrip.kt`, `TimerDigits.kt`, `NoWinBanner.kt`, `GameViewModel.kt`, `TutorialViewModel.kt`, `ResultsViewModel.kt`, `navigation/`, `di/`, `ui/home/`, `ui/theme/`, `:domain`, `strings.xml`, `dimens.xml`.
- **No new suppression.**

**11.2 The code-auditor — loop 3.** On opus under Q-019; its report at its own path, which the producer names.
- **Scope:** `75a03be..<tip>`, with this ruling as the requirement, plus F0's conflict hunks.
- **Reads:**
  - the read-only branch of `BoardCanvas` is byte-identical to `develop`, and nothing below the `Canvas(` call changed beyond what `develop` brought;
  - `AnnouncementRegion` is a leaf with no click, drawn first, and last in traversal;
  - the arm's constant and KDoc cite `:275` and `:2572`;
  - `BoardStop` is the slab's first child, with no children; the slab is an explicit group; A2.1's index is still on the slab;
  - the four `requestFocus` paths all go through the window-focus wait and the touch-exploration gate;
  - `focusProperties` is gated;
  - the `LongMethod` list is amended by one line and no second suppression exists;
  - every existing-test edit is named.
- **Probes** (P1–P5, P7 and P8 are each expected red; P6 is informational):
  - **P1** the channel back on the Canvas → §1's provider test;
  - **P2** Results' channel back on the Column → §4 (a);
  - **P3** the arm removed → §1's and §4's first-frame cases;
  - **P4** `boardStop` back on the slab `Surface` → §3's `isScreenReaderFocusable` case;
  - **P5** the Got-it `focusProperties` removed → §2's case in touch mode;
  - **P6** P5's mutant run outside touch mode → §2's case is green, which shows the harness is what catches it;
  - **P7** the window-focus wait removed → §8's case;
  - **P8** the selection branch restored under touch exploration → §7's case.
- **The Check:** its own `./scripts/check.sh --max-workers=4 --console=plain` and the `:app:testDebugUnitTest --rerun` run.

**11.3 QA — the re-run.** Report: `qa-engineer-device2.md`.
- **Device.** `ergoz_studio` (it worked):
  - cold boot with `-no-snapshot-load -no-snapshot-save`;
  - brief §10.2's TTS precondition first;
  - the new tip's debug APK, checked by sha256;
  - no `uiautomator` dump (26a);
  - never `Pixel_9a_bench`; the attached phone only if the producer allows it.
- **Precondition.** The build includes `develop` with `bugfix/home-chip-arrival` merged. If it does not, 26b's timer position and the timer's value are home's (its ruling, Open issue 5) and are recorded as such, not judged.
- **The list, in this order:**
  1. **TB-9 by ear, twice (light, dark), from a fresh install.**
     - When the UMP form opens, complete it, and restart that run's timing after it.
     - Record on opening, three openings in all: the step-1 line, and whether "Step 1 of 3" is heard twice now that TalkBack's first focus can land on the pips. If it is, A2.4's pre-ruled one-commit fallback (the first step's opening line speaks the caption alone) goes to the developer. It changes nothing else on this list, so only the opening is read again after it.
     - Then: the step 2 and 3 lines, each followed by the board stop's focus; the coach as "Hint: …"; whether a cell's text is also spoken after it (expected not, since §3 makes the board stop a leaf); rejected taps and jumps on T2 and T3; the done line; focus on "Got it".
     - The T1-clearing jump passes with its own line, or with the step-2 line in its place: the spec's §11.1 rule that a newer line replaces one not yet spoken.
  2. **26b:** gameplay (Classic, Challenge) and the tutorial, light and dark. Then A2.6's height-limited read with the banner raised on a portrait window wider than 512 dp (`wm size`), not in landscape (B-10).
  3. **TB-8 / B-4:** one Challenge level 1 win and one Classic win. The summary is spoken once; no column read; nothing spoken on a rotation or a theme change.
  4. **B-6:** a win, judged by the ux-designer's ruling (under (c): the summary only).
  5. **B-7:** a selection is one utterance (the cell's re-read); the put-down line is still spoken.
  6. **B-8:** the no-win banner's Restart, confirmed, lands on the board stop (×3); declined, moves nothing.
  7. **B-9:** landscape banner Undo ×3: the undo line, then the board stop. Clipping is recorded under B-10, not judged.
  8. **B-5:** the menu route ×3 and the reading-control route ×3, judged by the ux-designer's ruling. Record where the menu route lands now that §8's wait exists.
  9. **The regression sample:**
     - TB-1's no-op double-tap;
     - TB-4's three rejected lines on gameplay;
     - TB-6's stranding pair;
     - TB-7's dock controls and the portrait banner Undo;
     - TB-11's finger play and Tab with touch exploration off;
     - TB-10 by ear on Challenge level 1, plus English-33's first five moves and its win.
  10. **L1-2,** the hardware-keyboard check: one attempt. If it cannot be driven, it is recorded as BLOCKED. It does not hold the merge: the code-auditor accepted it as a platform limit.
- **A phone pass** (B-5, B-9 depend on TalkBack's own placement) belongs to the release's device sitting, not to this merge.

**11.4 15e — no sitting.**
- No fix touches a start-path file: `App.kt`, `MainActivity.kt`, `di/`, `navigation/`, `ui/home/`, `ui/theme/`, `res/font/`, the window theme files and the manifest.
- `BoardCanvas`'s read-only branch, the one Home's first frame composes, stays as `develop` has it. F1 removes a parameter in the interactive branch only.
- The developer re-runs brief §12's two tripwire lines:
  - line 1 prints nothing;
  - line 2 prints at most the existing `BoardCellOverlay(` line, since F1's change there is a removal.
- If line 1 prints anything, the branch needs its own sitting on `Pixel_9a_bench`, and the producer schedules it.
- Option (b) for B-6 would trip line 1, which is one reason it is 3.0.1.

**11.5 Then my sign-off,** at `tech-lead.md`. It reads:
- the code-auditor's report (all loops) and loop 3's;
- `qa-engineer.md` (FAIL, superseded) and `qa-engineer-device2.md`;
- the ux-designer's B-5 and B-6 rulings;
- this ruling and the developer's report;
- F0's merge conflict hunks.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-tb/.planning/reports/talkback-board/tech-lead-defect-ruling.md` — this ruling, the only file written and committed. No code, no Gradle, no device, no `.planning/log/`.

## Checks
All read-only, in `/Users/ermanergoz/Desktop/PegSolitarie-tb` unless noted. Sources unpacked into this session's scratchpad.
- `cat .claude/rules/VERSION` (0.6.2) against `ergoz-soft/.claude-plugin/plugin.json` (1.0.0), plus `diff -q` of each rule file — older, so the plugin copies were read; logging, puzzle-design and play-store are identical. `.planning/recall/` is absent.
- `git log --oneline -15`, `git status --short` — head `907aedb`, last code `75a03be`, clean. `git log develop` — `f7dd1fe`; home-chip-arrival not yet merged. `git diff --name-only 6aa7624 develop` / `develop...bugfix/home-chip-arrival` — the overlaps F0 names (`GameScreen.kt`, `TutorialScreen.kt`; home's `BoardCanvas.kt` frame-geometry hunk, read).
- `git diff --stat 6aa7624...75a03be -- app/src/main` — 17 files. Read at `75a03be`:
  - `BoardCellOverlay.kt`, `TouchExploration.kt`, `BoardAnnouncement.kt` in full;
  - `BoardCanvas.kt:60-285` (the interactive branch);
  - `GameScreen.kt:150-345`, `GameTraversal.kt`, `BannerAction.kt`;
  - `TutorialAnnouncements.kt`, and `TutorialScreen.kt:110-380`;
  - the `ResultsScreen.kt` diff;
  - `GameRoute.kt:40-110`, `GameViewModel.kt:925-950`;
  - `BoardGeometry.kt:107-136`, `domain/.../TutorialBoards.kt`;
  - `TutorialTalkBackTest.kt` (setup and the Got-it case), `TimerDigitsTest.kt:170-214`;
  - the tests' qualifiers (`grep 'QUALIFIERS ='`: hdpi), `dimens.xml`, and the manifest (no `screenOrientation`).
- `unzip` of `ui-android-1.11.3-sources.jar`, `foundation-android-1.11.3-sources.jar` (the BOM 2026.06.00 line) and `material3-android-1.4.0`. Read:
  - `SemanticsOwner.kt:134-146, 157-330`; `SemanticsProperties.kt:38-50, 85, 97, 508-515`; `SemanticsConfiguration.kt:89-90`;
  - `SemanticsNode.kt:100-106, 120-145`; `SemanticsModifierNode.kt:126-145`; `NodeCoordinator.kt:975-1000, 1302-1340, 1466-1470`;
  - `SemanticsSort.kt:59-160`;
  - `AndroidComposeViewAccessibilityDelegateCompat.android.kt:275, 285-310, 391-396, 1289-1306, 2371-2580, 2712-2725, 2895-2915, 3040-3090, 3420-3475`;
  - `AndroidComposeView.android.kt:918-940, 1072`; `Focusability.kt:33-73`; `FocusTargetNode.kt:236-247`;
  - foundation `Focusable.kt:150-240`, `Clickable.kt:1700-1712`.
- `javap` of Robolectric 4.16 `shadows-framework`:
  - `ShadowWindowManagerGlobal$WindowSessionDelegate.<init>` — `inTouchMode = ShadowView.useRealGraphics()`;
  - `ShadowInstrumentation.setInTouchMode` → `ShadowWindowManagerGlobal.setInTouchMode`;
  - `ShadowAccessibilityManager.getSentAccessibilityEvents()`, `setEnabledAccessibilityServiceList`.
- QA artifacts, read for the record and not as a gate:
  - `session2/speech-log.txt` (773 lines: run 1's tutorial 20:35–20:41, banner Undo 21:02, the win 21:23–21:24, landscape 21:42–21:44);
  - `session2/speech-raw.log:5761-5808, 8639-8663` (event types per utterance).
- `grep` of `.planning/reports/15-founder-feedback/tech-lead-signoff.md` and `phase-15/qa-engineer.md` for orientation — O-5, cited in §10.
- Not run: Gradle, the Check, `adb`, an emulator. This is a ruling, and the tech-lead re-runs no gate.

## Open issues / risks
1. **Inferred, not read.** TalkBack's source is not on this machine, so three behaviours are inferred from the speech log:
   - speaking a description change inside the focused node's subtree (B-1's "coach as a cell");
   - placing un-hinted nodes first (B-3, as home's ruling found);
   - restoring its own focus when a window it was in closes (B-5, B-8).
   The B-1 and B-3 fixes do not depend on them: they rest on Compose's pruning and ordering, which are read. B-8's fix does; its fallback is pre-ruled (§8).
2. **The channel leaf can be a stop for one second.** While it holds a line it is a speaking leaf, last in traversal. That is the exposure the canvas had, and the hold bounds it.
3. **The arm's 200 ms** starts at the leaf's placement, after which the delegate's next check (≤ 100 ms, layout-triggered) records it. A slow first frame delays the placement, not the margin.
4. **A testing gap, for the retro** (no lesson filed; that is the retro's): every JVM TalkBack test read the unpruned semantics tree, outside touch mode. §1, §2 and §4's tests read the node provider and the sent events, in touch mode. That is what TalkBack is handed on a phone, and future focus or channel tests should do the same.
5. **My own error.** Brief §6 assumed the button was focusable because it is clickable. That is B-2's cause, recorded in §2.
6. **B-10's severity** goes beyond O-5's "layout quality". The producer should carry it to the founder in the Q-PUBLISH-3.0.0 summary rather than leave it in the backlog unseen.
7. **Two ux-designer rulings are owed:** B-5 (running) and B-6 (new).
8. **Staffing:** this ruling, the developer and every reviewer so far ran on Opus (Q-019).

## Handoff
Next: producer —
1. This ruling is committed alone on `feature/talkback-board`: `docs(15): record the tech-lead talkback-board defect ruling`.
2. Give the ux-designer B-6 (§6) beside the running B-5 dispatch (§5). Both rulings are needed before QA's re-run; only B-5's option B touches the developer (F6).
3. Dispatch the android-game-dev in `PegSolitarie-tb` with §1–§9 and §11.1, F0 first. If `bugfix/home-chip-arrival` lands after F0, have the developer merge `develop` again before QA.
4. Then the code-auditor, loop 3 (§11.2).
5. Then QA on `ergoz_studio`, at `qa-engineer-device2.md` (§11.3). Any FAIL comes back to the tech-lead with its captures.
6. Then the tech-lead sign-off (§11.5).
7. Route §10's rows:
   - B-10 to the ux-designer for 3.0.1, and into the Q-PUBLISH-3.0.0 summary;
   - the banner re-announce and the consent timing to the ux-designer's backlog;
   - the count-up replay with `StarPopSounds`;
   - the timer value stays home's.
