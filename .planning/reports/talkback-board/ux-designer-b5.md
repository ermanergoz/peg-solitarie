# ux-designer report — talkback-board, B-5 ruling (jump action through TalkBack's menu)
Date: 2026-10-24
Branch: feature/talkback-board (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer; the product snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin 1.0.0 copies in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, dispatch; and the product's `.claude/rules/lessons.md`), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: the tech-lead's defect ruling on this branch (talkback-board)
Ran on: claude-opus-5-5 at effort xhigh. Staffing deviation: agents/ux-designer.md pins fable at xhigh. The Fable limit is reached, and the founder's answer to Q-019 (option 3, re-confirmed 2026-10-24) puts every Fable-pinned role on Opus until 3.0.0 is published.
Fan-out: none. This role has no grep, so one haiku Explore helper listed the spec's heading line numbers. I read every passage cited below myself, and nothing in this ruling rests on the helper's output.
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- B-5 is ruled as **D-TBb** in `docs/ux-spec.md`. After a jump action, focus must rest on the destination on every TalkBack route. On TalkBack's menu route only, resting on the source is accepted as the floor for 3.0.0, under five conditions.
- The tutorial's step and completion moves get the same requirement and the same floor. B-1 and B-2 are not waived.
- F-TB-1's fallback is **not triggered**, so input focus and the four moves stay. The fallback now applies only to a move that TalkBack follows on no route.
- The ruling changes no string, token or colour, and owes no new JVM or Compose assertion. The pass line is §11.1's new "Pass and fail" list, which QA reads from the speech log.
- On QA's own evidence, the gameplay case QA recorded (`g-12-jump-action.png`) already meets the floor. The tutorial menu case can only pass once B-1's step line is spoken.

## Rulings (the producer's four questions)
1. **After a jump taken through the menu (requirement).** Focus rests on the destination cell, whichever route invoked the action (the Actions reading control or TalkBack's menu). The player hears the jump's line once and in full, then the destination's description once ("Row 4, column 4, peg, …"). After that, nothing moves focus until the player does. The requirement fails if the source is spoken in full and focus then moves, or if the destination is spoken and focus is then taken back. A source fragment cut off by the destination is tolerated. TalkBack's own words as its menu closes ("Out of list") are not judged.
2. **Ending on the source after the menu closes: accepted for 3.0.0, on the menu route only.** The floor passes when all five conditions hold:
   - the jump's line is spoken once and in full before the source is read. It names the destination, so it already tells the player where the peg went, and no string is added;
   - the source is read with its current text, "empty";
   - nothing moves focus again after it rests;
   - the reading control still ends on the destination;
   - focus never rests off the board.

   Reasons:
   - The line and the cells hide nothing and say nothing false.
   - The board stays playable, which is what Q-023 asks. The cost is the walk to the moved peg: two swipes after a sideways jump, and up to about two rows of stops after an up or down one.
   - Returning focus to where the menu was opened is TalkBack's own behaviour when its menu closes, not this app's. Fighting it risks the two failures the spec already rules out: focus that moves after it has rested, and a stop read twice.

   The destination stays the target. Whether a build can meet it on the menu route is the tech-lead's call on mechanism. The floor is recorded in D-TBb, and the producer names it in the Q-PUBLISH-3.0.0 summary's TalkBack lines, beside the cell-size deviation (D-TBa item 10).
3. **The tutorial's step-change focus has the same requirement on the menu path.** Steps 2 and 3 must land on the new board's board stop, and completion on "Got it". On the double-tap and reading-control routes, both stay required as written, so B-2 stands. When the clearing jump or the completing jump is a menu action, the floor is a stop on the new board (or on the finished board), reached after the step's line (or the done line) has been spoken once and in full. Every step line and the done line are owed on every route, so B-1 stands. I added completion to the question as asked, because a completing jump made through the menu meets the same return.
4. **F-TB-1 is amended.** Its fallback was written for "TalkBack does not follow input focus". The device shows TalkBack following on the double-tap route (step change 6 of 6), on the reading-control route (jump 33 of 33) and after the banner's Undo and Restart. Applying the fallback would remove input focus from all four moves and break those working routes. So the fallback is not triggered, and from now on it applies to a move only if TalkBack follows that move on no route. A route where TalkBack places focus itself as its own window closes is governed by D-TBb.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-tb/docs/ux-spec.md` — the ruling, every edit dated 2026-10-24 and marked D-TBb:
  - the revision log gains "Eleventh revision, build loop" (top of file);
  - §3 gains **D-TBb** (the decision, its four items, reasons, and a "Recorded, not hidden" line), plus a pointer on D-TBa item 5's fallback;
  - in §11.1, three focus-table rows gain a dated clause (jump by action, tutorial step installed, tutorial completed);
  - in §11.1's "How focus moves", the fallback bullet gains a pointer and a new paragraph, "Two routes to a jump action", is added. It has the routes, a per-route table (required / floor), what is spoken in order, and the "Pass and fail" list;
  - §13 gains a pointer: no new assertion, and the route split is judged on the device only;
  - §14 gains a pointer in the TB lead (the two routes), a pointer on TB-5, and dated amendments to TB-7 (judged per route; the floor; the fallback not triggered) and TB-9 (per route; the floor; B-1 and B-2 not waived);
  - §16 gains a pointer on the new id.
- `/Users/ermanergoz/Desktop/PegSolitarie-tb/.planning/reports/talkback-board/ux-designer-b5.md` — this report.

## Checks
I have no Bash and ran no Gradle. The change is docs-only, and the producer runs the Check at commit if the branch's rules require it.
- `Read .claude/rules/VERSION` (0.6.2) against `ergoz-soft/.claude-plugin/plugin.json` (1.0.0) — pass (the snapshot is older, so I read the plugin copies in full).
- `Read .planning/reports/talkback-board/qa-engineer.md` in full — pass (B-5, device question 1, and the B-1/B-2/B-3 context).
- `Read .planning/reports/talkback-board/tech-lead-brief.md` in full (two pages, :1-217 and :218-421) — pass (§6 and Amendment 2 read; F-TB-1 at §1).
- `Read docs/ux-spec.md` before editing — pass. Sections read in full: D-TB and D-TBa (:1398-1631), §11.1 (:5059-5368), §13's D-TB rows (:5535-5577), §14 TB-1 to TB-12 (:6480-6636), the revision log (:400-487) and §16/§17 (:6790-6830).
- `Read` of QA captures `session2/g-12-jump-action.png`, `g-26-rc-jump.png` and `t2-03-after-T1-action-9s.png` — pass, consistent with B-5:
  - after the menu route, focus is on (2,4), now empty;
  - after the reading control, focus is on (4,4), a peg;
  - after T1 cleared by a menu action, focus is on step 2's (1,1) cell, and the overlay reads "Row 1, column 1, empty".
- Proofread of §11.1's new block (`Read docs/ux-spec.md` :5330-5404) — pass after two fixes:
  - "off the board" was reworded, because "Got it" is a required target;
  - item 1 now defers to the channel's existing replacement rule (F-43), so this ruling adds no new duty on lines that are replaced before they are spoken.
- Quality gates for this ruling:
  - every route has a row: pass (§11.1's table has the double-tap, reading-control, menu-required and menu-floor rows);
  - the screen-reader rules of rules/compose.md are unchanged: pass;
  - no string, token or asset added: pass.

## Open issues / risks
- **Mechanism.** Whether the destination can be landed on the menu route without changing the other routes or the TalkBack-off state is the tech-lead's ruling, running in parallel. If the tech-lead's option fails §11.1's "Pass and fail" (for example, focus moves after it has rested, or the source is read and then left), the floor ships instead.
- **The floor is not yet proven in the tutorial.** QA's step-2 capture lands on a cell of the new board, which is the floor's focus half. But no step line is spoken there (B-1), and that line is a floor condition. Two more cases are unverified:
  - a step whose new board has no cell at the origin's position, where TalkBack's return could land off the board and fail the floor;
  - completion by a menu action.
- **The delay and the cut.** QA saw the menu's return about 7 s after the line on the emulator. That timing is TalkBack's. If a phone shows the return cutting the jump's line, the floor fails there and a mechanism becomes required. QA's own open issue already asks for a phone sitting for B-5.
- **The tech-lead's brief still states the fallback as "TalkBack not following → remove the four `requestFocus()` and every `focusable()`".** This is in §1 F-TB-1 and in §6's last paragraph. It now needs to be read against D-TBb item 4. That brief is the tech-lead's file, and I did not edit it.
- **Check of this amendment.** D-TBb is a spec change written on Opus. If the producer's loop rules call for a fresh checker on spec amendments (as the ux-spec-folds loops did), that check is the producer's to dispatch.
- No founder question. This is neither an art-direction choice between equals nor a gate, and the floor is carried as a line in the Q-PUBLISH-3.0.0 summary, not as a question.

## Handoff
Next: producer —
- commit `docs/ux-spec.md` and this report on `feature/talkback-board`;
- put the D-TBb floor in the Q-PUBLISH-3.0.0 summary's TalkBack lines, beside the cell-size deviation.

Then route:
- **tech-lead:** set your mechanism options against §11.1's "Two routes to a jump action" and its "Pass and fail" list.
  - Build a menu-route mechanism only if it meets the requirement there while leaving the reading-control route, the double-tap route and the TalkBack-off gate unchanged.
  - Otherwise B-5's gameplay case needs no code: the current build already sits at the floor on QA's evidence.
  - Amend the brief's F-TB-1 and §6 fallback text to D-TBb item 4.
- **android-game-dev:** only what the tech-lead's ruling chooses. B-1 and B-2 stay required fixes either way.
- **qa-engineer:** re-judge B-5 per route against TB-7 and TB-9 as amended, using §11.1's "Pass and fail" list as the pass line.
  - Gameplay: a jump through the menu (3 runs) and through the reading control.
  - Tutorial, after the B-1/B-2 fixes: T1's and T2's clearing jumps and T3's completing jump, each through the menu.
  - For each run, record: the line spoken in full or cut, the stop focus rests on, any second focus move, and the time from the line to the menu's return.
  - Repeat once on the phone sitting before release.
