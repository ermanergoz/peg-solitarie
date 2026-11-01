# ux-designer report — talkback-board, B-8 ruling (banner Restart confirmed in its dialog)
Date: 2026-11-01
Branch: feature/talkback-board (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer; the product snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin 1.0.0 copies were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, dispatch; and the product's `.claude/rules/lessons.md`), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: tech-lead B-1 ruling (talkback-board, same worktree, report file only)
Ran on: claude-opus-5-5 at effort xhigh. Staffing deviation: agents/ux-designer.md pins fable. Founder answer Q-019 (option 3) keeps Fable-pinned roles on Opus until 3.0.0 is published.
Fan-out: none. This role has no grep, so one haiku Explore helper listed the line numbers of TB-7, the D-TB ids and the revision log. Every passage I edited I read myself.
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- B-8 is ruled as **D-TBd** in `docs/ux-spec.md`. This is the no-code fallback the tech-lead pre-ruled in `tech-lead-defect-ruling.md` §8.
- **Scope.** Only the rescue banner's Restart, confirmed in its dialog. After it, focus rests where TalkBack puts it as the dialog window closes: the screen's first stop, "Navigate back", which is what the build does 3 of 3. The board stop also passes.
- **Still owed:** the restart line, once and in full, and no focus move after focus rests (the half-second note from D-TBb applies).
- **Unchanged:** the banner's Undo, a banner Restart made without the dialog, the dock's Restart and a declined dialog stay as written.
- TB-7 gains a testable pass line for QA. No code, string, token or colour changes, and no new assertion is owed.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-tb/docs/ux-spec.md` — the ruling. Every edit is dated 2026-11-01 and marked D-TBd:
  - the revision log gains a D-TBd entry under the build-loop revision, after D-TBc;
  - §3 gains **D-TBd**: scope, what is expected, no code, four reasons, and "Recorded, not hidden";
  - §11.1's focus-table row "Undo or Restart from the rescue banner" gains the dialog-confirmed and declined cases;
  - §11.1's "How focus moves" gains a pointer. The fallback stays untriggered, because TalkBack follows the same move after the banner's Undo;
  - §13 gains a pointer: no new assertion, and the tech-lead's window-focus test stands;
  - §14 TB-7 gains a dated amendment with the new pass line;
  - §16's ids pointer gains D-TBd.
- `/Users/ermanergoz/Desktop/PegSolitarie-tb/.planning/reports/talkback-board/ux-designer-b8.md` — this report.

## Checks
I have no Bash, so I ran no Gradle and no Check. The change is docs-only.
- `Read .claude/rules/VERSION` (0.6.2) against `ergoz-soft/.claude-plugin/plugin.json` (1.0.0) — pass. The snapshot is older, so I read the plugin copies in full.
- `Read .planning/reports/talkback-board/qa-engineer-device2.md` in full — pass. B-8: "Board restarted. 4 pegs left." → "Peg Solitaire" (window) → focus "Navigate back | Button", 0 of 3 on the board stop, and no later move within 6 s. Declined: focus back on the banner's Restart (PASS). O-3: the landing moved from the timer to "Navigate back" once B-3 put back first.
- `Read .planning/reports/talkback-board/tech-lead-defect-ruling.md` in full — pass. §8's pre-ruled fallback: "TB-7's row for a dialog-confirmed banner Restart falls to TalkBack's own placement. The ux-designer amends the row … and no further code is written."
- `Read` of my `ux-designer-b5.md` and `ux-designer-b6.md` — pass. D-TBd uses D-TBb's half-second note and its "focus that has rested is never moved again" rule, and contradicts neither ruling.
- `Read docs/ux-spec.md` before editing — pass. I read the revision log (:440-534), D-TB to D-TBc (:1449-1792), §11.1's focus table, "How focus moves", "Two routes" and "Pass and fail" (:5425-5564), the dock's Restart row (:3636-3664), §13's D-TB pointers (:5818-5857), §14 TB-1 to TB-11 (:6790-6964) and §16's ids (:7135-7179).
- Proofread of D-TBd and TB-7's new block — pass after one fix. TB-7 had said the previous bullet "holds only for a Restart made without the dialog", which no device run shows. It now says only that the bullet "does not cover a Restart confirmed in its dialog".
- Quality gates:
  - the focus table and TB-7 agree: pass;
  - accessibility rules of rules/compose.md unchanged: pass;
  - no string, token or asset added: pass.

## Open issues / risks
- **A dialog-free banner Restart was not judged in session 3.** D-TBd keeps the board stop as its target. The Restart row says "the ViewModel decides confirm or immediate". If QA never sees a banner Restart without the dialog, that row stays unverified on the device. It is not a B-8 item.
- **Input focus and TalkBack's focus differ after the confirm.** The app still gives the board stop input focus after the window regains focus (F4), while TalkBack rests on "Navigate back". Input focus shows nothing under touch exploration, and TB-11 gates it, so I see no player-facing effect. The code-auditor may want to confirm that the request leaves no visible focus indication.
- **Timing.** The 5 s quiet window and the half-second bound are pass lines read from speech-log timestamps, not mechanisms. A phone may differ from the emulator. That belongs to the release's phone sitting, as the tech-lead's ruling already plans.
- **Check of this amendment.** D-TBd is a spec change written on Opus under Q-019. A fresh checker is the producer's to dispatch if the loop rules call for one.
- No founder question. This is not an art-direction choice between equals or a gate, and it is carried as a line in the Q-PUBLISH-3.0.0 summary.

## Handoff
Next: producer —
- commit `docs/ux-spec.md` and this report on `feature/talkback-board`;
- name D-TBd beside D-TBb's floor in the Q-PUBLISH-3.0.0 summary's TalkBack lines.

Then route:
- **android-game-dev:** no work for B-8.
- **qa-engineer:** at the narrow re-check, read B-8 against TB-7's new pass line: three confirms and one decline.
- **tech-lead:** sign-off reads D-TBd as B-8's closure under §8's fallback.

New TB-7 pass line, verbatim:

**Pass line (TB-7, a banner Restart confirmed in its dialog).** Raise the rescue banner (`stuck_message` or `nowin_message`), double-tap its Restart, then double-tap the dialog's confirm button. The run passes when, in this order:
1. "Board restarted. *N* pegs left." is spoken once and in full, with *N* the new board's count;
2. focus then rests on the gameplay screen's first stop, "Navigate back", or on the board stop, "Board, *N* pegs left", read once with its current text;
3. no further focus move follows in the 5 s after that read, unless the player makes it.

TalkBack's window name between them ("Peg Solitaire") is not judged. The run fails if:
- the restart line is missing, repeated or cut off;
- focus rests on any other stop: the title, the timer, a cell or a dock control;
- a stop is read in full and then left by a move the player did not make. "Navigate back" followed by the board stop passes only if the board stop's read starts no later than half a second after "Navigate back" ends.

Declining the dialog passes when focus returns to the banner's Restart and nothing moves. The device pass is three confirms and one decline, read from the speech log.
