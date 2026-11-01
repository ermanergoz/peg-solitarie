# ux-designer report — licences-screen (ruling on QA's R4 observations O-R4-1 and O-R4-2)
Date: 2026-11-01
Branch: feature/licences-screen (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer). The snapshot is 0.6.2 and the plugin 1.0.0. I read the plugin copies of reporting, compose and dispatch in full; the other snapshot rules are in context. `.claude/rules/lessons.md` lists 0 active lessons, and `.planning/recall/RECALL.md` does not exist. Also ui-ux-pro-max and android-accessibility. I did not invoke compose-skill: it runs only on an explicit request, and this ruling writes no Compose UI.
Ran in parallel with: licences code-auditor merge review
Ran on: claude-opus-5-5. The role pins fable; under Q-019 it runs on Opus, as in `ux-designer-ls15.md`. The harness named no effort.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **O-R4-2 (backward linear swipes skip four entries) is the platform's. It is recorded, not graded, and is not a 3.0.0 defect.**
  - It has the mechanism R1 verified. The skip comes only across TalkBack's two backward page scrolls (`linear-back-timeline.txt`).
  - The stops with no scroll between them came in exact reverse order: OkHttp, then Dagger, then Kotlin.
  - Forward swipes reach every entry (`linear-fwd-timeline.txt`), so nothing is unreachable.
- **O-R4-1 (a heading swipe from the pinned title that finds nothing) touches the spec in one place only.** Pass line 4 (a) counts the headings spoken and their order, not swipes. The lost swipe is recorded, not graded.
- Both are added to LS-15's recorded list as points 6 and 7, with matching pointers in pass lines 1 and 4, two rows in §8.2 item 2's table, and §10 row 47. No grade in R4 changes, and nothing in the build changes.

## The ruling, and why
**O-R4-2: recorded, not a finding.**
- **The cause is the one already ruled.**
  - Compose gives TalkBack only the nodes on screen (R1, Layer 1), and TalkBack's backward scroll moves a full viewport (Layer 2).
  - After the first scroll, the pivot (Chromium) left the exposed tree. TalkBack then chose where focus landed, scrolled a second time, and landed on OkHttp.
  - LS-15 already records this mechanism for going up by headings. Linear backward is the same mechanism in another TalkBack mode.
- **No property the app owns failed:**
  - forward linear order is complete (pass line 1);
  - with no scroll between two stops, the backward order is the exact reverse of the table;
  - every card read was whole, title first (pass line 2, which R2 delivers).
- **The harm is the same class LS-15 accepted.**
  - A player swiping back hears four fewer entries, but nothing is hidden.
  - The screen opens at the top, a forward pass reads every entry, and TalkBack's scroll actions still work.
- **The only lever is R1's option B**, a shorter accessibility page. The tech-lead rejected it for 3.0.0 because it changes every TalkBack scroll and is unproven on the device. It would need its own device spike, in the release week, for a gesture that no pass line grades.
- **What still fails going backward stays testable** (LS-15 point 6; pass line 1's pointer):
  - a card that is not read whole, title first;
  - a skipped or out-of-order stop with no TalkBack page scroll between the two stops;
  - any stop that §5.1's table does not list.
- **Scope.** The rule covers all four screens, because the notices list and the notice text scroll in the same way. Backward swipes are not owed as a run.

**O-R4-1: recorded, not a finding.**
- The swipe found nothing (`success=false`), spoke nothing, and left focus in place, and the next swipe reached Fonts. No heading was skipped or repeated, so run 1 passes pass line 4 (a) as QA graded it.
- The spec now says it outright. A swipe that finds nothing is neither a stop nor a skip, QA swipes again, and (a) is graded on the headings spoken (LS-15 point 7; pass line 4's pointer).
- It fits R1's inference that the title sits outside the scroll container. The mechanism is the tech-lead's, for information only.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/docs/ux/licences-screen.md` — the spec. Nothing was deleted; every change is a dated pointer:
  - frontmatter: the status and revised lines;
  - LS-15: new points 6 and 7 after the 2026-11-01 ruling, plus the option-B note;
  - pass line 1: backward swipes are recorded, not graded, for order;
  - pass line 4: the lost swipe;
  - §8.2 item 2: two rows in the grading table;
  - §10: row 47.
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/ux-designer-r4-observations.md` — this report.

## Checks
This role has no Bash, so every check is a read.
- `Read .planning/reports/licences-screen/qa-engineer-r4.md` (whole) — pass. Read the observations O-R4-1 and O-R4-2, run 1's excerpt, and the forward-swipe evidence.
- `Read .planning/reports/licences-screen/ux-designer-ls15.md` and `tech-lead-f4-ruling.md` (whole) — pass. Read R1 Layers 1–2, option B's rejection, and R2.
- `Read docs/ux/licences-screen.md`: lines 1–517 and 900 to the end — pass. Read LS-15, §5.1, pass lines 1–7, §8.2, §9 and §10 before editing.
- Consistency of the edits — pass. LS-15 points 6 and 7, the pointers in pass lines 1 and 4, the two §8.2 rows and §10 row 47 say the same thing, and no R4 grade changes.
- I did not open the QA timelines (`.planning/qa/licences-screen/r4/*.txt`). The ruling rests on QA's quoted sequence.
- `./gradlew check` — not run. I have no Bash, and the change is to docs only.
- `git add` / `git commit` — not run, for the same reason. No commit hash exists from this role.

## Open issues / risks
1. **O-R4-2 was seen once and not repeated**, per QA's no-repeat rule. The ruling does not depend on how often it happens: only a skip with no scroll before it would make it the app's.
2. **The backward under-report is a known, accepted gap.** If players report it, the place to start is R1's option B as a device spike, which is the tech-lead's. It is not scheduled.
3. **The code-auditor is reviewing in this worktree at the same time.** My only file changes are the two paths above, and neither is code.

## Handoff
Next: producer —
1. **Commit my files** on `feature/licences-screen`, staging only `docs/ux/licences-screen.md` and this report: `docs(licences): record R4's backward-swipe and lost-swipe observations`.
2. **tech-lead:** for information, O-R4-1's mechanism. Nothing is asked, and R5's merge condition is unchanged.
3. **qa-engineer:** nothing is owed. Backward swipes are not a run.

Verdicts:
- O-R4-1 (a heading swipe from the pinned title that finds nothing): recorded, not a finding.
- O-R4-2 (backward linear swipes skip entries across a TalkBack page scroll): recorded, not a finding.
