# ux-designer report — talkback-board, B-6 ruling (the winning jump's line)
Date: 2026-10-24
Branch: feature/talkback-board (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer; the product snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin 1.0.0 copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, dispatch; and the product's `.claude/rules/lessons.md`), ui-ux-pro-max, android-accessibility, compose-skill. All were loaded in this same step for the B-5 ruling.
Ran in parallel with: none (a second ruling in the B-5 step, after the tech-lead's defect ruling `507b392`)
Ran on: claude-opus-5-5 at effort xhigh. Staffing deviation: agents/ux-designer.md pins fable at xhigh. The Fable limit is reached, and the founder's answer to Q-019 (option 3, re-confirmed 2026-10-24) puts every Fable-pinned role on Opus until 3.0.0 is published.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- B-6 is ruled as **D-TBc**, the tech-lead's option (c), for 3.0.0 and after. A winning jump speaks Results' summary, once and in full, and its own line is not owed.
- **For 3.0.1, nothing.** Options (b) (carrying the move into Results) and (a) (a delay before Results only under TalkBack) are declined, not deferred. The ruling reopens only if the win itself changes so the board stays on screen long enough to show the last jump, for every player.
- The summary is now a win's only spoken record, so TB-8's "once and in full" becomes a hard condition. The tech-lead's B-4 fix (F1's leaf and its arm) is what delivers it.
- In the same step I read the tech-lead's §5 against D-TBb (B-5):
  - option A is D-TBb's floor;
  - option B is not wanted;
  - option C's likely "source, then destination" sequence is now judged with a half-second bound, so F4 (which B-8 needs) cannot fail B-5 just by correcting late-but-promptly.
- No string, token or colour changes. No new test is owed, and the developer has no work for B-6.

## Rulings
1. **The user-facing requirement for 3.0.0 — option (c).**
   - The winning jump may come from a double-tap or from an action on either route, on a Classic board or a Challenge level.
   - After it, the first line the app speaks is Results' summary, once and in full ("Perfect! — 3 of 3 stars, 1 peg left."). Focus follows Results' own order (TB-8; the focus table's "A game won" row).
   - The winning jump's own line is not owed. Any part of it that a device lets through is not judged, if it comes before the summary and does not cut it.

   Reasons:
   - **Every player gets the same moment.** The board leaves within a frame or two of the winning tap (tech-lead ruling §6), so a sighted player never sees the last jump either.
   - **The summary says what matters.** It carries the pegs left that the jump's line ended with, plus the outcome and the stars. The player chose the last move a moment earlier by its label ("Jump left to row 4, column 2", or "jump here").
   - **The line would break §11.1's own rule.** Every announcement says only what the cells then say. The winning jump's line would describe cells that have already left the screen.
2. **What I want for 3.0.1: nothing.**
   - **(b)** would roughly double what Results speaks, say "1 peg left" twice, and cost a 15e sitting through `navigation/`. It is declined on its value, not only on its cost.
   - **(a)** would make the game's flow depend on whether a screen reader is running, and it carries the lost-finish risk the tech-lead does not rule in. It is declined.
   - The only reopening condition is a win flow that keeps the board visible to show the winning jump for every player. The line would then return through the board's own channel, followed by the summary.
3. **B-5, read against the tech-lead's §5.** This is an addition to D-TBb, not a reversal.
   - **Option A** = D-TBb's floor (no code).
   - **Option B** (no move on either route) is not wanted. It gives up the reading control's working destination (33 of 33) only to make the routes identical.
   - **Option C** rides F4's window-focus wait. If the menu route then ends on the destination, the requirement is met and the floor is not used. The likely sequence is TalkBack's return to the source, then the app's move, judged this way:
     - the destination cutting the source's read passes, as before;
     - a source read in full then passes only if the destination's read starts within half a second of its end;
     - later than that fails;
     - destination, then source, always fails.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-tb/docs/ux-spec.md` — every edit dated 2026-10-24:
  - the revision log gains a "Same day — D-TBc" entry under the build-loop revision, including the D-TBb reading of §5;
  - §3 gains **D-TBc** (the requirement, the hard condition on TB-8, "3.0.1: nothing", and four reasons), plus a dated line on D-TBb (options A, B and C);
  - in §11.1, the sentence "When a jump wins, the jump's line is followed by Results' summary when Results opens." is withdrawn in place and kept as the record;
  - §11.1's announcement-table row "A jump, by tap or by action" gains "not the jump that wins", the focus-table row "A game won" gains a pointer, and the "Results" paragraph gains one;
  - in §11.1's "Pass and fail" list, the "spoken in full and then left" line is bounded, and "The window-focus note" is added (options A, B and C, with the half-second bound);
  - §13 gains a pointer: no new assertion;
  - §14 amends TB-6 (the winning jump is judged by TB-8) and TB-8 (the summary is first, once and in full, and is a win's only record);
  - §16's ids pointer now covers D-TBb and D-TBc.
- `/Users/ermanergoz/Desktop/PegSolitarie-tb/.planning/reports/talkback-board/ux-designer-b6.md` — this report.

## Checks
I have no Bash and ran no Gradle. The change is docs-only.
- `Read .planning/reports/talkback-board/tech-lead-defect-ruling.md` in full (454 lines) — pass. §5 (B-5 options A to E) and §6 (B-6 options (a) to (c), and the cause: the finish navigates within a frame or two, the delegate batches every 100 ms, and the line leaves with the screen) were read in full, as asked. §4 (B-4's arm, which TB-8 now depends on) and §11.3 item 4 (QA judges B-6 by this ruling) were read too.
- `Read .planning/reports/talkback-board/qa-engineer.md` in full (in this step, for B-5) — pass (B-6: 0 of 2; `tb10-e33-m31.png`, `tb10-c1-win.png`).
- `Read docs/ux-spec.md` — pass. I read §11.1's stranding/win bullet, its announcement and focus tables, "Results", and TB-6 and TB-8 in full before editing (in this step).
- Consistency of D-TBc against the existing contract — pass:
  - the focus table's "A game won" row already moved focus to Results' order;
  - TB-8's latch (D-TBa item 7) is unchanged;
  - TB-10's "plays from the first move to Results" needs no edit;
  - the tutorial's completion is not a win (it has its own done line, under D-TBb) and is untouched.
- Consistency of the D-TBb addition against the tech-lead's F4 (§8) — pass. The half-second bound keeps F4's general wait from failing B-5's menu route by a prompt correction. "Destination, then source" still fails.

## Open issues / risks
- **B-6 passes only if B-4 is fixed.** Under D-TBc the summary is a win's only spoken record. QA's run 1 saw it missing on Challenge 1 (B-4's race), so QA judges B-6 and TB-8 together after F1.
- **The half-second bound is new.** It is a pass line for QA's speech-log timestamps, not a mechanism. If the device shows F4's move starting more than half a second after the source's read ends on the menu route, that run fails: the source was heard as the resting stop and then left.
  - The tech-lead then chooses between making the move land sooner (the requirement) and not making it after TalkBack's return (the floor).
  - Either outcome meets the spec as written, and no further spec change is needed.
- **My B-5 report** (`ux-designer-b5.md`, committed in `b25c472`) does not mention the §5 reconciliation, because §5 did not exist when I wrote it. This report and the spec's dated lines are the record.
- **Check of this amendment.** D-TBc and the D-TBb addition are spec changes written on Opus. A fresh checker is the producer's to dispatch if the loop rules call for one.
- No founder question. D-TBc ships no floor that needs a Q-PUBLISH line, because the summary gives the same moment a sighted player gets.

## Handoff
Next: producer — commit `docs/ux-spec.md` and this report on `feature/talkback-board`. Then route:
- **android-game-dev:** no work for B-6 (the tech-lead's F-table, "— B-6: nothing under (c)"). For B-5, no F6 commit: option A/C, not B.
- **qa-engineer** (`qa-engineer-device2.md`, the tech-lead's §11.3):
  - **item 4, B-6:** one Classic win and one Challenge win, by a double-tap and by an action. Pass when the summary is the first line, once and in full. Any fragment of the jump's line before it is not judged.
  - **item 8, B-5:** judge by §11.1's "Pass and fail" and "The window-focus note". Record the timestamps of the source's read end and the destination's read start when both are heard.
- **tech-lead:** sign-off reads D-TBb (with its §5 line) and D-TBc. Option (b) is off the 3.0.1 list.
