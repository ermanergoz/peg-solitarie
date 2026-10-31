# ux-designer report — licences-screen (LS-15 ruling, after the tech-lead's F-4 R1)
Date: 2026-11-01
Branch: feature/licences-screen (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer; the snapshot rules in context, plus the plugin copies of reporting and compose read in full, because the plugin is newer than the snapshot; `.claude/rules/lessons.md` lists 0 active lessons; `.planning/recall/RECALL.md` does not exist), ui-ux-pro-max, android-accessibility. compose-skill was not invoked: it activates only on an explicit request, and this ruling writes no Compose UI.
Ran in parallel with: licences R2 developer fix, candy 15m oracle build, home sign-off
Ran on: claude-opus-5-5. This deviates from the role's pin (fable) under Q-019, as in `ux-designer-rerun.md`.
Fan-out: none. One read-only haiku Explore helper searched `docs/ux-spec.md` and `docs/ux/` for any other heading-navigation rule, because this role has no search tool. It wrote nothing.
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
1. **LS-15 is restated for a scrolling list (option F). The redesign is not required.** Going down, every heading is reached in order, from the top and from a mid-list entry. Going up, and from the title with the list scrolled, the behaviour is the platform's: recorded, not graded.
2. **Between two headings going down**, a non-heading stop is accepted only right after one of TalkBack's page scrolls, at most one per scroll. It must be a whole card, title first.
3. **Pass line 2 is unchanged and graded on every card stop in every run.** Its note "LS-15's fix is expected to remove it" is superseded by the tech-lead's R2.
4. **Rejected for 3.0.0:** E, the pinned section index (the only exact fit for the old text), and D, a title that scrolls with the list. This is not a choice between equals, so no founder question is drafted.

## The ruling, and why
- **The premises of 2026-10-24 fell.** LS-15 rested on three reasons, and R1 removes all three:
  - **Being in the tree is not enough.** Compose exposes only on-screen nodes to TalkBack, which was verified in the pinned source.
  - **Settings is not a counter-example.** It passes because it nearly fits on one screen, so it is not proof that the property can be built.
  - **The cost is not small.** The only design that meets the old text is a redesign.
- **Reason 1's harm remains**, but only going up. Up from mid-list, the player hears the title and under-counts the sections.
  - It is accepted because the screen opens at the top, and the downward pass names all five headings in order.
  - Every entry is still reachable by linear swipes and by scrolling.
  - By R1's analysis, the same behaviour follows from the platform on any Compose list that scrolls under a pinned heading title.
- **E is rejected:**
  - it adds a second navigation row and four stops for every player, to serve one TalkBack gesture;
  - no other screen in the product has such a row, and Android does not use one for a list of 23 items;
  - it needs a new design-and-build loop in the release week.
  E is recorded as the shape to reach for if the screen grows past about eight sections.
- **D is rejected:** it changes the top bar that every screen shares, and it is unproven, because TalkBack would still land through its own scroll.
- **A is kept:** the non-lazy list. Its Compose tests are now named "necessary, not sufficient".

## The QA grading for the heading runs (R4)
Every run records every stop and every TalkBack scroll. Settings: light, 100 %, TalkBack 16, reading control on Headings.

| Run | Grade |
|---|---|
| **1. Down from the back button** | **PASS** when Licenses (title), Fonts, Sounds, Open-source software and Google libraries are reached in that order, none skipped or repeated. A non-heading stop between two headings must come right after a TalkBack page scroll, at most one per scroll, and be read whole, title first. **FAIL** on a skipped or repeated heading, a non-heading stop with no scroll before it, or a card read without its title. What follows Google libraries is not graded. |
| **3b. From an entry in the middle, down** | Graded as run 1, for the headings below that entry. |
| **2. Up from the last entry; 3a. from the title with the list mid-scrolled, down; 3b. from a mid entry, up** | **Recorded, not graded** for heading order. Today's outcome ("Google libraries" then the title; the title only) is expected and is **not** a finding. Each card stop in these runs is graded by pass line 2. |
| **4. Linear forward from the title with the list at its end** | Not graded for order (that is the platform's). Each card stop is graded by pass line 2. |
| **5. R2 regression** | As in R4: a whole Licenses card and a whole notices row are each read once, label first, with "Button" and the hint; about 15 linear swipes across one scroll on the notices list read each row once, name first, with its "k of" position. |
| **Every card stop in runs 1–5** | **Pass line 2** (pass line 3 for an inert card): read once, whole, title first; "Button" and the hint only on a tappable card. |

- **200 %** is not owed in this re-check (the founder's reduced scope). If the producer runs it, run 1 is graded the same way.
- **No repeats.** A failing run is reported once, with its transcript, and never repeated in the hope of another result.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/docs/ux/licences-screen.md` — the spec edits. Nothing was deleted; each change is a dated pointer:
  - the frontmatter's status and revised lines;
  - LS-15's ruling block, which covers the facts that changed, the contract, why it is acceptable, and the rejected E and D;
  - §5.1 "Headings are headings";
  - pass line 2's note, now pointing at R2;
  - pass line 4, restated again as (a)–(d);
  - LS-AC06's pointer;
  - §8.2 item 2's grading table;
  - §9 item 18, closed for LS-15;
  - §10 row 46.
- `/Users/ermanergoz/Desktop/PegSolitarie-lic/.planning/reports/licences-screen/ux-designer-ls15.md` — this report.
- `docs/ux-spec.md` is unchanged. The helper found no LS-15, heading-navigation or Licenses-screen rule in it, nor in `docs/ux/invalid-tap-feedback.md`. The nearest line is 4996 (titles carry `heading()` "so a screen-reader user can jump bands"), which does not bear on off-screen headings.

## Checks
This role has no Bash, so every check is a read.
- `Read .planning/reports/licences-screen/tech-lead-f4-ruling.md` (whole) — pass. R1's options A–F, R2's label fix, R4's runs and R5's merge condition were all read.
- `Read .planning/reports/licences-screen/qa-engineer-recheck.md` (whole) — pass. The F-4 transcript excerpt and the linear observation were read.
- `Read .planning/reports/licences-screen/ux-designer-rerun.md` (whole) — pass.
- `Read docs/ux/licences-screen.md` — pass. Read before editing: lines 1–60, 360–420 and 905–1064, and from 1100 to the end.
- Helper search of `docs/ux-spec.md` and `docs/ux/` — pass. It found no conflicting rule.
- Consistency of the edits — pass, by re-reading the LS-15 block after editing. Pass line 4 (a)–(d), LS-15 points 1–5, LS-AC06 and §8.2 item 2 say the same thing, and §10 row 46 points at each.
- `./gradlew check` — not run. This role has no Bash, and the change is to docs only.
- `git add` / `git commit` — not run, for the same reason (no Bash). **No commit hash exists from this role.**

## Open issues / risks
1. **The R2 dependency.** Run 1's "whole card, title first" is met only if R2 lands as ruled. If the device reads a card twice or not at all, that goes to the tech-lead (R2's open issue 2), not to this role.
2. **The upward under-report is a known, accepted gap.** If players report it, E is the recorded remedy.
3. **The Settings inference is untested.** The claim that Settings at 200 % behaves the same is R1's inference and has not been observed. The ruling does not depend on it.
4. **The tech design (§3.6) lacks R2 and T4.** That docs sync is for the producer to schedule (R1's open issue 5).

## Handoff
Next: producer —
1. **Commit my files.** Commit `docs/ux/licences-screen.md` and this report on `feature/licences-screen` with `docs(licences): rule LS-15 for a scrolling list`. Stage only those two paths, because the developer is editing code in the same worktree. I have no Bash, so I could not commit.
2. **qa-engineer:** run R4 and grade it by the table above. Pass line 4 is now gradable in the same session as pass line 2.
3. **tech-lead:** R5 item 1 (this ruling) is met. Sign off once the auditor and QA pass.
