# tech-lead report — talkback-board (ruling: device re-run 2, B-1 residual and A2.4)
Date: 2026-11-01
Branch: feature/talkback-board (report only; head `6a98510`, last code commit `402e5af`)
Skills loaded: ergoz-soft:standards (role tech-lead; the product snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose, stacks, testing, dispatch; logging, puzzle-design and play-store are identical to the snapshot; the product's `.claude/rules/lessons.md`), android-architecture, android-gradle-logic, android-accessibility
Ran in parallel with: ux-designer B-8 amendment (landed as `6a98510`, D-TBd), licences auditor, colour-vision QA
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** `agents/tech-lead.md` pins fable; founder answer Q-019 (option 3) keeps Fable-pinned roles on Opus until 3.0.0 is published. Mitigation: the cause rests on QA's raw TalkBack log and on a JVM probe whose output is quoted under Checks.
Fan-out: none
Loops: 0
Read in full: `.planning/reports/talkback-board/qa-engineer-device2.md`, `tech-lead-defect-ruling.md`, `ux-designer-b8.md` (summary and outputs). In part: `ux-designer-b5.md`, `ux-designer-b6.md`, `android-game-dev-loop4.md`, `code-auditor-merge-home.md` (summaries); `tech-lead-brief.md` A2.4 and §5's `StepAnnouncements` row; `docs/ux-spec.md` D-TBb (`:1686-1720`) and §11.1's focus table, "Two routes to a jump action" and "Pass and fail" (`:5440-5535`)
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **B-1's cause.** At a step swap the app sends the step line first and moves focus to the board stop about 200 ms later. TalkBack drops the step line if it is still waiting in TalkBack's queue when the swap's events arrive. That happens whenever something else is still being spoken, and the clearing jump's own line, about 7 s on this emulator, is the usual case. If the step line has already started, it survives.
- **The fix, F-B1, one commit, test-first.** On steps 2 and 3, with touch exploration on: the clearing jump puts no line of its own; focus moves first; the step line goes on the channel 500 ms after the move. Nothing the app sends afterwards can drop it. The heard order becomes board stop, then step line. That reverses the spec's order, so a one-paragraph ux-designer amendment (D-TBe) is needed before QA.
- **A2.4 is confirmed as F-A.** Commit `fix(talkback): speak the first step's caption alone on opening`. The first-composition branch speaks `stringResource(captionRes)` only. No string changes.
- **Device re-check:** emulator only. Three openings, three step lines by double-tap, three through TalkBack's menu, completion as a by-product. Every one must pass (6 of 6 step lines). B-8 needs no new read.
- **Fallback:** if F-B1 misses this one round, revert it, keep F-A, and record B-1's residual as a 3.0.0 known limitation. The ux-designer amends D-TBb item 3, and the producer adds a line to the Q-PUBLISH-3.0.0 summary. No further code loop.

## 1. B-1 residual: the cause

**What the device shows** (QA's `session3/speech-raw.log`; TalkBack's behaviour is inferred, because its source is not on this machine):

| Case | What TalkBack is saying when the step line arrives | Result |
|---|---|---|
| Menu, T1 (04:04:58) | the jump line, 58.9 → 05.7 s | Step line queued at about 59.6. The board stop's focus event comes at 59.80. Spoken afterwards: the window, then "Board, 2 pegs left", then the hint. **The step line is lost.** |
| Double-tap, run 2 T2 (04:00:37) | the jump line, 37.18 → 44.16 s | Queued: 38.004 and 38.224/38.264; the focus event time is 1690759, 1.02 s after the jump. Spoken afterwards: "Board, 5 pegs left", then the hint. **The step line is lost.** |
| Double-tap, passing (03:50:56, 03:51:58, 03:59:42, 04:08:51) | TalkBack's own re-read of the destination ("Row 1, column 3, peg, no jumps"), which can be interrupted | The step line **starts** (57.484) before the board stop's focus. Spoken: step line, then board stop. The jump line is never spoken. |
| Completion via the menu (04:07:40) | the jump line | The done line was queued 219 ms before Got it's focus, and was spoken after the jump line. **It survives.** |

**What the app sends** (JVM probe in a scratch worktree at `6a98510`, event order through Robolectric's sent events, one frame at a time after the step beat):
```
frame1  node=17 CONTENT_CHANGED  "Board, 2 pegs left"            (the board stop's text)
frame1  node=22 CONTENT_CHANGED  "Row 1, column 2, peg, 1 jump"   (cells of the new board)
frame1  node=6  CONTENT_CHANGED  "Step 2 of 3"                    (pips)
frame1  node=23 CONTENT_CHANGED  "Row 1, column 3, empty"         (the cell TalkBack is on after a double-tap)
frame2  node=29 CONTENT_CHANGED  "Step 2 of 3. Jumps work …"      (the channel)
frame14 node=17 VIEW_FOCUSED                                      (the board stop, +208 ms)
```
Earlier in the same probe, the T1-clearing jump's line ("Row 1, column 1 to row 1, column 3. … 1 peg left.") is sent at the jump.

**Cause.**
1. The step line arrives in the same burst as the board swap: the focused cell's new text, the board stop's new text, and 200 ms later the board stop's focus.
2. Something in that burst discards a live-region line that TalkBack is holding in its queue. A line TalkBack has already started is never cut. A line queued later, such as the hint 4 s on, is spoken.
3. So the step line survives only if it has started before the burst. It does start before the burst when TalkBack is only re-reading a cell, which it interrupts; the passing cases above are each won by a few tens of milliseconds. It does not when a live-region line is playing, which is the jump line. Through TalkBack's menu the jump line always plays, because no cell is re-read there, so the menu route loses the step line every time.
4. The completion case shows that a focus move alone does not discard a queued line. The discarding event is in the swap's burst. Which event it is cannot be read without TalkBack's source. The fix does not depend on knowing it.

**Why the JVM was green:** the tests assert that each line is sent, and it is. Only TalkBack's queue loses it.

## 2. B-1: the fix (F-B1), one developer round

**Mechanism.** Everything below holds only while touch exploration is on. With it off, nothing changes (TB-11, the W5 classes).
1. **The clearing jump of a step that is not the last speaks no line.** The tutorial gets its own jump source in `TutorialAnnouncements.kt`, which skips the line when the post-jump board has `TutorialUiState.STEP_COMPLETE_PEGS` pegs and `stepIndex < STEP_COUNT - 1`. The shared `JumpAnnouncements`, and everything gameplay uses, stay untouched. Every other tutorial jump keeps its line, including T3's jumps and its completing jump.
   - Why: the spec already allows this ("a line that a newer one replaced before it was spoken is not owed"; my ruling §11.3: "or with the step-2 line in its place").
   - Without this, the step line would wait behind a 4–7 s jump line until the coach's hint, 4 s after the swap (`TutorialAnim.NUDGE_DELAY_MS`), could replace it in TalkBack's queue.
2. **On a step that replaced another, focus first, then the line.** In `StepAnnouncements`: `moveInputFocusTo(boardStopFocus, window)`, then `delay(STEP_LINE_AFTER_FOCUS_MS)`, then `announcer.announce(stepLine)`.
   - Add the constant `STEP_LINE_AFTER_FOCUS_MS = 500L` to `TutorialAnim`.
   - Its KDoc: past Compose's 100 ms event batch (ui 1.11.3, `:275`) and TalkBack's round trip from `TYPE_VIEW_FOCUSED` to the spoken focus. That round trip was at most ~170 ms on a heavily loaded emulator (`speech-raw.log` 04:00:38.224; focus event 1.02 s after the jump, with the move requested at about 0.85 s). Cite this ruling.
   - The line goes on after the swap's whole burst, so nothing the app sends later can drop it. The next things TalkBack hears from the app are the player's own taps and the coach's hint, both queued after the line.
3. **Unchanged:**
   - the first step: no move, the line on first composition (as F-A sets it);
   - completion: done line, then the Got-it move;
   - the arm, the hold, `REPEAT_MARK`, `moveInputFocusTo` and its window-focus wait;
   - gameplay;
   - strings.
4. **Files:** `ui/onboarding/TutorialAnnouncements.kt` and `ui/onboarding/TutorialAnim.kt` only. No start-path file, so no 15e sitting. Brief §12's tripwire line 1 must still print nothing.

**What the player hears** (double-tap route, emulator timing):
1. TalkBack's re-read of the destination;
2. "Board, 2 pegs left" (focus);
3. "Step 2 of 3. Jumps work …";
4. the hint, if the player waits 4 s.

On the menu route the first item is "Peg Solitaire" (the window), which the spec does not judge.

**Tests, red first.** Each goes in `TutorialTalkBackTest`, with `turnOnATouchExplorationService()`, in touch mode as now.
- **(a) `afterAStepSwapTheBoardStopsFocusIsSentBeforeTheStepLine`.** Mark the sent-event count after T1's jump. Idle past the beat, the focus gap, `STEP_LINE_AFTER_FOCUS_MS` and two service batches, idling the main looper per frame as `letTheServiceHear` does. Among the events after the mark, the first `TYPE_VIEW_FOCUSED` comes before the description change carrying `STEP_TWO_LINE`, and both exist. **Red today:** the probe shows the line at frame 2 and the focus at frame 14.
- **(b) `underTouchExplorationAStepsClearingJumpPutsNoLineOfItsOwn`.** After T1's jump and its swap, `descriptionChangesSent()` does not contain the T1 jump line. After T2's clearing jump, it does not contain `T2_JUMP_LINE`. A non-clearing T3 jump's line is still sent, as is the completing jump's. **Red today:** the probe shows the T1 jump line sent.
- **(c) Touch exploration off.** `withTouchExplorationOffAStepChangeMovesNoFocus` stays as it is: the step line is on the channel at the beat, with no move. Add the jump line's presence there.
- **Retargets** (this branch's own tests; the report names each one):
  - `clearingAStepSpeaksTheNextStepsLineAndMovesFocusToItsBoardStop`: assert focus first, then advance `STEP_LINE_AFTER_FOCUS_MS` and assert the channel.
  - `everyLineAfterTheFirstStepSwapIsSentToTheService`: `T2_JUMP_LINE` is a clearing jump, so replace it with a non-clearing T3 jump's line in the in-order list.

## 3. A2.4: the "Step 1 of 3 heard twice" fallback (F-A), confirmed

**Triggered:** QA heard it 3 of 3 (`op1..3-*-open-12s`).

**The commit, exactly as the brief's A2.4 pre-ruled it:**
- `fix(talkback): speak the first step's caption alone on opening`, one commit in `ui/onboarding/TutorialAnnouncements.kt`.
- `StepAnnouncements`'s first-composition branch (`!replacedAStep`) announces the caption alone, `stringResource(state.captionRes)` resolved in composition: "Tap a peg, then tap the empty hole two cells away."
- Steps 2 and 3 keep `format.stepLine(…)`.
- No `strings.xml` or `BoardSpeechFormat` change.

**Red first:** in `TutorialTalkBackTest`, change `STEP_ONE_LINE` and `theFirstStepsLineIsSpokenOnOpeningAndFocusIsNotMoved`'s expected line to the caption alone. That retargets the arm case and the traversal case with it.

**Order:** F-A first, then F-B1. Both edit `StepAnnouncements`.

## 4. The spec amendment F-B1 needs (ux-designer, D-TBe)

F-B1 reverses an order the spec states twice:
- §11.1: "When focus moves and an announcement is due, both are spoken: the announcement first, then the newly focused stop";
- "What is spoken … in this order", together with D-TBb item 3's "the step's line … spoken once and in full before it".

**Why the spec's order cannot be delivered reliably.** That order needs the step line to have started speaking before the swap's events reach TalkBack. Whether it has depends on TalkBack's speech queue, which the app cannot observe. Section 1's passing cases each won by tens of milliseconds.

**The amendment** (one paragraph, steps 2 and 3, touch exploration on):
- the step's clearing jump speaks no line of its own; the step line stands for it;
- the new board stop is spoken first, then the step's line, once and in full;
- D-TBb's floor reads "after it" for the step line;
- the completion row stays as it is (its order passes 3 of 3, and F-B1 does not touch it).

**This is a requirement, not a mechanism, so it is the ux-designer's to accept.** It must land before QA's re-check. The developer does not wait for it. If the ux-designer declines, §6 applies.

## 5. The device re-check (QA, `qa-engineer-device3.md`)

**Founder directive:** reduced QA, emulator only. QA's proposal is accepted, with an exact pass line.

**Setup:**
- `ergoz_studio`, cold boot, `-no-snapshot-load -no-snapshot-save`;
- the new tip's debug APK, checked by sha256;
- brief §10.2's TTS precondition;
- start only once the host load has fallen, as QA did after its instrument fault (1-minute average under 14); record it.

**The list:**
1. **Opening ×3** (Settings › How to play). The caption is heard alone, then "Step 1 of 3" once, from the pips' first focus. Pass 3 of 3.
2. **Step lines: 3 by double-tap and 3 through TalkBack's menu.** Suggested runs:
   - run 1: double-tap T1 and T2;
   - run 2: menu T1 and T2;
   - run 3: double-tap T1, menu T2.

   Each clearing jump must give:
   - the new board stop, then the step line once and in full (D-TBe's order);
   - focus resting on the board stop, or on menu-route jumps any stop on the new board (D-TBb);
   - no line cut or repeated.

   The clearing jump's own line is not expected. **Pass 6 of 6, zero lost.**
3. **Completion, as a by-product** of each run reaching T3: T3's jump lines, the done line, then "Got it". Pass 3 of 3.
4. **Smoke, inside the same runs:** one rejected tap and one coach hint on T2, each spoken.
5. **Not re-run:**
   - B-8: D-TBd judges session 3's evidence, "Board restarted. 4 pegs left." then "Navigate back", 3 of 3;
   - gameplay: F-A and F-B1 touch no gameplay file;
   - every PASS row of `qa-engineer-device2.md`.

## 6. The fallback if B-1 is not fixed in this round

**Triggers.** Any one of these:
- the developer cannot make (a) and (b) green without touching a file outside §2's list;
- the code-auditor fails F-B1;
- the ux-designer declines D-TBe;
- QA loses a step line on either route (fewer than 6 of 6).

**Then:**
1. **The code.** F-B1 is reverted on the branch with `git revert`, never a rewrite. F-A stays, since it stands alone.
2. **The spec.** The ux-designer amends D-TBb item 3 and TB-9: for 3.0.0, a step's line may be lost when the clearing jump's line is speaking. That is always on TalkBack's menu route, and about 1 in 6 by double-tap on the emulator.
   - What remains: the visible caption is a stop of its own; the pips stop says "Step N of 3"; focus still reaches the new board.
   - It is a 3.0.1 item.
   - B-1 then does not hold the merge. Note that "recorded against the D-TBb floor" is not enough as the floor stands: the floor itself requires the step line, so the amendment is needed.
3. **The founder.** The producer carries it in the Q-PUBLISH-3.0.0 summary as a known accessibility limitation, beside B-10. No new founder question and no new gate: the founder decides at the publish gate.
4. **The loop count.** This branch is past three review loops (my ESCALATION). This is its last code round for B-1 under the founder's ship-this-week directive. A miss stops code work and records, as above.
- **3.0.1 candidates:**
  - the step line carried in the board stop's own text on a fresh step (one focus utterance, the spec's order kept);
  - the same move-then-speak order for the completion row, which passes today but rests on the same queue.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-tb/.planning/reports/talkback-board/tech-lead-device2-ruling.md` — this ruling, the only file committed. No product code, no device, no `.planning/log/`.

## Checks
All run in `/Users/ermanergoz/Desktop/PegSolitarie-tb` unless noted.
- `cat .claude/rules/VERSION` (0.6.2) against the plugin's `plugin.json` (1.0.0), plus `diff -q` of each rule file. The snapshot is older, so the plugin copies were read in full; logging, puzzle-design and play-store are identical. `.planning/recall/` is absent.
- `git log --oneline -15`, `git log e7f7926..HEAD` — head `6a98510` (the ux-designer's D-TBd, docs only), last code `402e5af`, clean.
- Read in full at `6a98510`: `ui/components/BoardAnnouncement.kt`, `ui/onboarding/TutorialAnnouncements.kt`, `ui/onboarding/TutorialScreen.kt`, `ui/components/TouchExploration.kt`, `TutorialTalkBackTest.kt` and the `testing/` accessibility helpers. In part: `TutorialViewModel.kt:230-375` (the jump → `advanceAfterABeat`, `STEP_ADVANCE_DELAY_MS` 650, `NUDGE_DELAY_MS` 4000), `TutorialUiState.kt:60-69`, `BoardSpeechFormat.kt:127-136`, `strings.xml` (the three tutorial strings).
- ui 1.11.3 sources (`ui-android-1.11.3-sources.jar`): `AndroidComposeViewAccessibilityDelegateCompat.android.kt:1345-1368` (`createEvent`), `:2853-2870` (`Focused` → `TYPE_VIEW_FOCUSED` in the batched pass).
- QA artifacts, read for the record and not as a gate:
  - `session3/speech-raw.log`, the queue and event times at 03:50:56–03:51:06, 03:54:05–03:54:13, 04:00:36–04:00:58, 04:04:51–04:05:09 and 04:07:38–04:07:51;
  - `session3/speech-log.txt`, every step, done and "Step 1 of 3" line.
- `git worktree add --detach <scratchpad>/tl-probe 6a98510`, with a throwaway `TlB1ProbeTest.kt` there only (copy kept at `<scratchpad>/TlB1ProbeTest.kt.txt`, never committed). Then the worktree was removed with `git worktree remove --force` and `git worktree prune`.
- `./gradlew :app:testDebugUnitTest --tests '…TlB1ProbeTest' --tests '…TutorialTalkBackTest' --max-workers=4 --console=plain -i` (in the probe worktree) — pass (exit 0, BUILD SUCCESSFUL in 58s). `TutorialTalkBackTest` is green at `6a98510`; the probe printed the jump line sent at the jump.
- The same command with the probe stepping frame by frame, run twice — pass (exit 0). The second run printed the event order quoted in §1 (the step line at frame 2, the board stop's `VIEW_FOCUSED` at frame 14). In the first run, without the looper idled per frame, the delegate sent no batch, only a host-view `VIEW_FOCUSED`, which is why (a) must idle the looper per frame.
- Not run: the Check, `adb`, an emulator. This is a ruling, and the tech-lead re-runs no gate.

## Open issues / risks
1. **Inferred, not read.** TalkBack's queue rules come from its log; its source is not on this machine. F-B1 rests on two facts that every one of the 12 tutorial samples in session 3 shows:
   - a line TalkBack has started is never cut by the app's later events;
   - a line queued after the swap's burst is spoken.

   It does not rest on which event in the burst discards the line.
2. **The 500 ms margin** is about 3 times the round trip measured under a host load of 47. On a slower instrument the line could again arrive before the focus is spoken, and then it is lost as today, no worse. QA records the host load.
3. **The coach's hint can still replace the line** at a very slow speech rate: if TalkBack's re-read plus the board stop take longer than about 3.2 s, the hint arrives while the step line still waits. The spec does not owe a replaced line. The emulator margin is about 1.5 s. Not tested at non-default rates.
4. **The menu route loses the jump's own confirmation.** The player hears the window title, the board stop, then the step line, and no "Row 1, column 1 to …". The step line and "Board, N pegs left" say that the step was cleared. The ux-designer weighs this in D-TBe.
5. **Staffing:** this ruling, the developer, and every reviewer so far ran on Opus (Q-019).

## Handoff
Next: producer —
1. Commit this ruling alone: `docs(15): record the tech-lead device-2 ruling`. Done by the tech-lead.
2. **In parallel:**
   - **ux-designer:** D-TBe (§4), one paragraph. Report `ux-designer-b1.md`.
   - **android-game-dev** (opus, `PegSolitarie-tb`): F-A (§3), then F-B1 (§2), each red first and green on `./scripts/check.sh`. Report: a "Loop 5" section naming every retargeted test. The output of `git diff --name-only 6a98510..HEAD -- app/src/main | sort` must list only `ui/onboarding/TutorialAnnouncements.kt` and `ui/onboarding/TutorialAnim.kt`.
3. **code-auditor**, loop 5 (on opus under Q-019).
   - **Scope:** `6a98510..<tip>`, with this ruling as the requirement.
   - **Probes, each expected red:**
     - P1: the step line announced before the move → test (a);
     - P2: the clearing jump's line restored → test (b);
     - P3: F-A reverted → the opening case;
     - P4: the delay set to 0 → test (a).
4. **QA**, `qa-engineer-device3.md` (§5), once D-TBe has landed.
5. **Then my sign-off** at `tech-lead.md`. It reads every code-auditor report on this branch, both QA device reports and `qa-engineer-device3.md`, the ux-designer's B-5, B-6, B-8 and B-1 rulings, the developer's reports and this ruling.
6. **On any §6 trigger:** run §6 instead of another loop.
