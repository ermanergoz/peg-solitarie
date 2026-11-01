# ux-designer report — talkback-board, step-order ruling (D-TBe: B-1 residual, A2.4)
Date: 2026-11-01
Branch: feature/talkback-board (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer; the product snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin 1.0.0 copies were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, dispatch; and the product's `.claude/rules/lessons.md`), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: talkback developer fixes (android-game-dev, F-A then F-B1, same worktree; I touched no file of theirs)
Ran on: claude-opus-5-5 at effort xhigh. Staffing deviation: agents/ux-designer.md pins fable. Founder answer Q-019 (option 3) keeps Fable-pinned roles on Opus until 3.0.0 is published.
Fan-out: none. This role has no grep, so one haiku Explore helper listed the spec's line numbers for the D-TB ids, §11.1's phrases, §13, §14 and §16. Every passage I edited I read myself.
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **ACCEPT.** The tech-lead's order is ruled as **D-TBe** in `docs/ux-spec.md`. On steps 2 and 3, while touch exploration is on, the clearing jump speaks no line. Focus moves to the new board stop first, and the step's line follows once and in full. The spec's earlier order, step line then stop with each once and in full, still passes.
- **The first step (A2.4).** F-42's pre-ruled answer is recorded as triggered: the opening line is the caption alone, and "Step 1 of 3" is heard once, from the pips.
- **Unchanged:** completion (done line, then "Got it"), step 3's jumps, gameplay, TalkBack off, and D-TBb's focus floor.
- **Fallback pre-ruled (D-TBe item 4).** If the tech-lead's §6 fallback runs, a lost step line is a recorded 3.0.0 limit and a 3.0.1 item. No further UX dispatch would be needed.
- The device-3 pass lines are in TB-9, quoted verbatim at the end of this report. No string, token, colour or code changes.

## Ruling and reasons
Accepted, for four reasons (written into D-TBe):
1. **A line that arrives beats an order that loses it.** The spec's order depends on TalkBack's queue, which the app cannot observe. The step line is the only spoken instruction for the new step.
2. **"Where you are, then what to do" is D-TB's own principle.** D-TB puts the position first. With the new order, the instruction is the last thing heard before the player acts.
3. **The clearing jump's line describes a board that is leaving.** The board swaps 650 ms after the jump, and the line runs for seconds after that. This is the same reason D-TBc drops the winning jump's line: every announcement says only what the cells then say.
4. **The cost stays small and inside the tutorial.** The tech-lead's open issue 4 is weighed and accepted. The player chose the jump by its label a moment earlier, and "Step 2 of 3" with the new count confirms it worked.

Why the earlier order is kept as a pass: D-TBe changes the order only so that the line arrives. A run that delivers both utterances in the old order, each once and in full, gives the player everything. Failing it would only risk tripping the fallback for no user-facing reason.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-tb/docs/ux-spec.md` — the ruling. Every edit is dated 2026-11-01 and marked D-TBe. First wordings are kept.
  - Revision log: a D-TBe entry after the D-TBd entry.
  - §3: the new decision **D-TBe** (scope, items 1–4, four reasons, "Recorded, not hidden"), plus a dated bullet on D-TBb item 3 ("before it" reads "after it" for the step line).
  - §11.1:
    - announcement table: the jump row (the clearing jumps of steps 1 and 2 owe no line under touch exploration) and the step row (after the board stop on steps 2 and 3; the caption alone on the first step);
    - the exception to "the announcement first, then the newly focused stop";
    - the focus table's step row;
    - a D-TBe order list after "What is spoken … in this order";
    - a D-TBe line in "Pass and fail";
    - "The tutorial": F-42 recorded as triggered.
  - §13: a pointer. The assertions are the tech-lead's tests (a), (b), (c) and the opening retarget. This document adds none. Retargets, never deletions.
  - §14: TB-6 (the two clearing jumps owe no line), and TB-9 (the amendment and the device-3 pass line).
  - §16: the ids pointer.
- `/Users/ermanergoz/Desktop/PegSolitarie-tb/.planning/reports/talkback-board/ux-designer-step-order.md` — this report.

Files changed, for the producer to commit: `docs/ux-spec.md` and this report. Nothing else.

## Checks
I have no Bash, so I ran no Gradle and no Check. The change is docs-only.
- `Read .claude/rules/VERSION` (0.6.2) against `ergoz-soft/.claude-plugin/plugin.json` (1.0.0) — pass. The snapshot is older, so the plugin copies were read in full.
- `Read .planning/reports/talkback-board/tech-lead-device2-ruling.md` in full — pass. §1 cause, §2 F-B1, §3 F-A, §4 the amendment asked for, §5 the device list, §6 the fallback.
- `Read .planning/reports/talkback-board/qa-engineer-device2.md` in full — pass. B-1 residual: double-tap 5 of 6, menu 0 of 2. A2.4: "Step 1 of 3" twice on 3 of 3 openings. Completion 3 of 3.
- `Read` of my `ux-designer-b5.md`, `ux-designer-b6.md` and `ux-designer-b8.md` in full — pass. D-TBe keeps D-TBb's focus floor and its "never moved again" rule, uses D-TBc's "a line about a board that has gone" reason, and leaves D-TBd untouched.
- `Read docs/ux-spec.md` before editing — pass. Passages read:
  - the revision log (:430-552);
  - D-TB (:1467-1498) and D-TBa items 5–6 and F-38 to F-44 (:1566-1700);
  - D-TBb to D-TBd (:1702-1856);
  - §11.1's announcement and focus tables, "How focus moves", "Two routes", "What is spoken", "Pass and fail", the window-focus note, "Results" and "The tutorial" (:5440-5640);
  - §13's D-TB pointers (:5879-5925);
  - §14's TB lead, TB-6 to TB-9 (:6865-6881, :6930-7054);
  - §16's ids (:7243-7266).
- Proofread of every edited passage after writing (`Read` :1877-1950, :5612-5622, :5664-5710, :7128-7230) — pass after three fixes:
  - D-TBe item 4's grammar;
  - the opening pass line no longer contradicts "either order passes";
  - TB-9's sentence that restates D-TBb's floor order is now plain.
- Consistency against existing rows — pass:
  - TB-9's device-3 strings match §11.1's table and QA's speech log ("Board, 2 pegs left", "Board, 5 pegs left", both step lines, the step-1 caption, the done line);
  - the step-2 and step-3 board counts match §4.1's step table (2 and 5 pegs);
  - "Pass and fail"'s "a line repeated, or cut off" is narrowed only for the clearing jump's line, which D-TBe makes not owed.
- Quality gates:
  - the core flow's screens all keep a spec section: pass;
  - the accessibility rules of rules/compose.md are unchanged: pass;
  - no string, token or asset added, so no licence row is owed: pass.

## Open issues / risks
- **The 500 ms margin and slow speech rates** are recorded in D-TBe, not hidden.
  - On a much slower instrument, the step line could again reach TalkBack before the focus is spoken.
  - At a slow speech rate, the coach's first hint, 4 s after the swap, could replace the unspoken step line. The channel's rule allows that, and the hint names the move.
  - QA judges at TalkBack's default rate and records the host load.
  - 3.0.1 candidate for the tech-lead: the first nudge after a step change never takes an unspoken step line's place.
- **The tech-lead's other 3.0.1 candidate** (the step line carried in the board stop's own text on a fresh step) is not ruled here. If it is built, I would read it then. The open question is whether the board stop's later re-reads keep their plain "Board, N pegs left".
- **No fresh checker** has read D-TBe. It is a spec change written on Opus under Q-019. A fresh check is the producer's to dispatch if the loop rules call for one.
- **The phone sitting** before release should repeat the 6 step lines. Timing is TalkBack's and the TTS engine's, as with D-TBb.
- No founder question. This is not an art-direction choice between equals, and it is not a gate. D-TBe adds no Q-PUBLISH line unless item 4's fallback runs.

## Handoff
Next: producer —
- commit `docs/ux-spec.md` and this report on `feature/talkback-board`, alone or beside the developer's loop-5 commits;
- if the tech-lead's §6 fallback triggers, name D-TBe item 4's limit beside D-TBb's floor and B-10 in the Q-PUBLISH-3.0.0 summary. No UX re-dispatch is needed.

Then route:
- **android-game-dev:** nothing new. F-A and F-B1 as the tech-lead ruled them meet D-TBe.
- **code-auditor (loop 5):** read D-TBe and §13's pointer as the requirement beside the tech-lead's ruling.
- **qa-engineer (`qa-engineer-device3.md`):** use TB-9's device-3 pass line below, and record the host load and the speech rate.
- **tech-lead:** sign-off reads D-TBe as B-1's and A2.4's closure if QA passes, and D-TBe item 4 if §6 runs.

**ACCEPT**

Device-3 pass lines, verbatim from TB-9:

**Device-3 pass line (the narrow re-check).** Emulator only, at TalkBack's default speech rate, with the host load recorded. The device pass is 3 openings and 6 step lines, read from the speech log:
1. **Opening ×3** (Settings › How to play, TalkBack on). Each opening passes when the caption, "Tap a peg, then tap the empty hole two cells away.", is spoken once and in full as a line that no longer begins "Step 1 of 3"; "Step 1 of 3" is spoken once, from TalkBack's first focus on the pips; and the app moves no focus. The intended order is the caption first, and either order passes. It fails if "Step 1 of 3" is heard twice, or if the caption is missing, repeated or cut off. **Pass: 3 of 3.**
2. **Step lines ×6:** the clearing jumps of step 1 and step 2, 3 by double-tap and 3 through TalkBack's menu (run 1: double-tap T1 and T2; run 2: menu T1 and T2; run 3: double-tap T1, menu T2). Each passes when, in this order:
   1. TalkBack's own words are not judged: its re-read of the destination after a double-tap, or "Peg Solitaire" as its menu closes. The clearing jump's line is not expected and not judged;
   2. focus rests on the new board's board stop, "Board, 2 pegs left" (step 2) or "Board, 5 pegs left" (step 3), read once with its current text. After a menu jump, any stop on the new board passes (D-TBb's floor). A read cut short by the step's line is tolerated;
   3. the step's line is spoken once and in full: "Step 2 of 3. Jumps work up, down, left and right — never diagonally." or "Step 3 of 3. Keep jumping. Clear the board down to one peg.";
   4. no focus move follows that the player did not make.

   The earlier order (the step's line, then the stop, each once and in full) also passes. A run fails if the step's line is missing, repeated or cut off; if the coach's hint is spoken before it or in its place; if focus rests off the new board (the pips, Skip, the caption) or, on the double-tap route, anywhere but the board stop; or if a stop is read with its text from before the swap. **Pass: 6 of 6, zero lost.**
3. **Completion,** as a by-product of each run reaching step 3: step 3's jump lines, then "That's the whole game. Go clear a board." once and in full, then "Got it". **Pass: 3 of 3.**
