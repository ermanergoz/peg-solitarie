# ux-designer report — candy-backdrop: the backdrop spec rewritten for direction B (Q-021)
Date: 2026-10-18
Branch: feature/candy-backdrop (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer; plugin rules v1.0.0 read in full because the product snapshot is v0.6.2: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, dispatch; product `.claude/rules/lessons.md`), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: research-analyst direction-B confirmation, phase-15 H-2 developer fix
Ran on: claude-opus-5-5 at effort xhigh (deviation: the agent file pins fable; Opus by the founder's answer to Q-019, option 3, 2026-10-17, recorded in `PegSolitarie-p15/.planning/questions/FOUNDER.md`)
Fan-out: none. One haiku Explore helper grepped the phase-15 sources and tests, which this role's tools cannot search (rules/dispatch.md)
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Staffing deviation:** this step ran on Opus instead of the pinned Fable, by the founder's Q-019 answer.
- **`docs/ux/candy-backdrop.md`** is now its third edition, written for B. It specifies:
  - a deep sky, two glossy waves, and nine lights on Home and five on the game screen;
  - a gloss finish, gel mode tiles, and a static wash on the Sparse screens;
  - a settle-after-idle battery default, proposed for the tech-lead.
- **The record:** B-01 to B-16 are kept word for word under a status table, and 17 rulings are added.
  The tech-lead's D1–D11 are folded in, with D3 void; D10 and D11 belong to licences-screen.md.
- **`docs/ux/colour-vision.md`** gains §11: TG-AC04's target moves, and B meets it; the gel tiles and
  the dock keep "nothing missing".
- **Design-board changes** were made only where a gate required one; §13 lists them. The dark
  Continue chip's edge is the one measurable cost of the pick.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/docs/ux/candy-backdrop.md`: the third edition (direction B).
  - **Decisions (§1):** a status table for B-01 to B-16, then their text unchanged (§1.1). The new
    rulings (§1.2): B-05a, B-06a, B-09a, B-10a, B-11a, B-12a, B-13a, B-14a, B-16a and B-17 to B-24.
  - **Design (§2–§10):** the densities (§2); the layers, elements, composition, poses and container
    finish (§3); the tokens in light and dark with their constraints (§4); legibility (§5); motion,
    reduced motion and the battery proposal (§6); cold start (§7); the fit with phase 15 (§8); assets
    (§9); the verdict checklist (§10).
  - **Criteria and tests (§11–§12):** BD-01 to BD-28, and the tests owed.
  - **Rejections (§13).**
  - **Open items (§14).**
  - **The fold-in (§15):** D1–D11 and R1–R6 against B.
  - **The superseded record (§16).**
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/docs/ux/colour-vision.md`: amended.
  - Front matter: a `revised` line.
  - CV-12a added.
  - §2's tile and backdrop rows amended.
  - §4.4 headed with an amendment note.
  - TG-AC02's clauses amended; TG-AC04's target struck and replaced.
  - §10 item 1 closed.
  - New §11, "Amendments for backdrop direction B".
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/ux-designer-direction-b.md`: this report.

## UX decisions taken where the board had to change (each recorded in the spec, §13)
1. **The gel's light band.** `GelMid` now sits at 16dp, not at 36% of the tile. The sheen is confined
   to the gel lip, and the pressed state darkens.
   - **Why:** white text would read ≈ 3.8:1 at 200% font, ≈ 3.1:1 under the sheen, and 4.46:1 while
     pressed.
2. **No gloss on four surfaces:** the dock (discs and slab), the board slab, the stage art and the
   rescue banner.
   - **Why:** 21e and 21f measure the discs against the slab, and a glossed On disc reads ≈ 2.4:1;
     BD-17 needs the slab interior identical.
3. **Dark gloss at 6%, not 10%.**
   - **Why:** the ribbon labels would read 4.10:1. At 6% they read ≥ 4.66:1.
4. **A calm fade instead of the board's veil**, with the calm end at 0.14 H.
   - **Why:** no extra fill per frame, and a pose test can check it. The calm end clears the Home hero
     row (≤ 0.128 H) and the game strip at 200% (≤ 0.1375 H).
5. **Home's lights re-laid in lanes**; the game lights kept from the board.
   - **Why:** two overlapping rims break the wrapped-title bounds (L 0.094 in dark, 0.44 in light).
6. **The inner bottom shade dropped.**
   - **Why:** the timer pill has no vertical padding, so the shade would lie under its digits.
7. **Sparse is a static wash**, lighter than the background in light: the new `BackdropSparseLow`,
   `#FBF0FA`.
   - **Why:** a darkening wash crosses the cards' luminance and erases their edges.
8. **The orbs retire.**
   - **Why:** one slow layer, not two.
9. **A rim on every hero container (B-14a)**, with floors that hold against any backdrop colour.
   - The clay family keeps ≥ today's ratio: 1.07 and 1.29, against 1.05 and 1.24.
   - The dark Continue chip falls from 1.90:1 to ≥ 1.25:1, and the dark rescue banner from 1.39:1 to
     ≥ 1.26:1. Both are accepted and recorded.
10. **The hue arc widens by 1°, to 336°**, admitting `#BE185D` (the peg rim) and `#831843` (an existing
    role).
11. **The backdrop's durations live in a scoped `BackdropMotion` object.** BD-05 is worded for it.
12. **Correction:** today's dark slab edge is 1.24:1, not the second edition's 1.26:1.

## Checks
- **Contrast and luminance, by hand.** Every figure was computed with the WCAG 2.x formula from the
  `Color.kt` and board hexes, translucent layers composited in gamma-encoded sRGB. **Pass:**
  - every text pair ≥ 4.5:1: white on the gel ≥ 4.90; the dark ribbon label under the gloss ≥ 4.66;
    the Sparse text ≥ 5.11;
  - every glyph ≥ 3:1: the gel badge ≥ 3.73; the Challenge spinner ≥ 4.0;
  - the wrapped title ≥ 3:1: light 3.11, dark 3.99;
  - TG-AC04 met: max L 0.047 against 0.0786;
  - the rim floors as stated.
- **Lanes and bands, by hand at 360×640dp.** Pass:
  - the Home lane extents are pairwise disjoint, with ≥ 13dp between them;
  - the highest low light's top is at 327dp, against the 301dp line;
  - the Quiet upper lights stay ≤ 0.32 H and the lower ones ≥ 0.60 H;
  - the back crest with its bob is at 0.484 H, against 0.47 H.
- **Haiku helper greps on `PegSolitarie-p15`:** pass.
  - No phase-15 test asserts a mode tile's or Continue chip's colour.
  - `DockOnMarkAndSlashTest` asserts the disc fills (21e).
  - There is no `keepScreenOn`, `isPowerSaveMode` or `preferredFrameRate` anywhere in `app/src/main`.
  - `clayShadow` tints with `ClayColors.shadowTint`, at 21 call sites.
  - The pill, chip and timer paddings were quoted, and §5.3 uses them.
- **The two-question test, this role's pre-check of B:** pass (Q1 no, Q2 no). The binding answer is
  the reviewers' (BD-02).
- **`Write` and `Edit` of the 21 section placeholders in candy-backdrop.md:** pass. Each matched once.
  Ten colour-vision.md amendments: pass, each matched once.
- **`./gradlew check`:** not run. Design documents only, and this role has no Bash.
- **A device or browser render:** not run, for the same reason. The device readings govern (BD-12,
  BD-13, BD-27).

## Open issues / risks
1. **IP confirmation pending.** The research-analyst's one line on the waves, lights and gel cards. The
   spec assumes it. BD-28 makes it a merge condition, and a "not confirmed" returns that element to
   this role.
2. **The tech-lead's addendum to R1–R6 is needed before the build.** The developer builds to that
   report where it differs, and parts of it are now void:
   - R2, whole;
   - in R1: `BackdropOrbs.kt`, the `HomeBlobs` move, the wing builders and the `HomeStage.kt` edit;
   - in R5: the wing clauses.

   The addendum must also rule on:
   - the pure wave and light model and the shared clock;
   - the glow's mechanism. A glow inside `ui/theme/Clay.kt` would make it a fifth file shared with the
     audio branch's wrong-tap work;
   - the gel tile's container;
   - the flat footprint.
3. **The idle-frame ruling.** B-22's settle default is a proposal: 60 s on Home and 20 s in a game,
   then a 2 s ease to a stop, a tap to wake it, and rest in battery saver. BD-16 (c) follows the
   ruling.
4. **Beyond the dispatch's list of criteria to rewrite**, two were changed, because B made them
   unattainable or inconsistent:
   - BD-13's pass line: "≥ develop" is now §5.2's floors;
   - BD-05: it now admits `BackdropMotion`.

   BD-27 (gel and gloss text on the device) and BD-28 (IP) were added. The producer may want a
   reviewer to look at these four first.
5. **A measurable edge drop in dark.** The Continue chips go from 1.90 to ≥ 1.25, and the rescue banner
   from 1.39 to ≥ 1.26. I accepted it as an accessibility judgement: a card outline is not a WCAG
   requirement, the chip is labelled amber on violet, and the floor beats today's 1.02:1 light chip.
6. **Q-021's answer is not in FOUNDER.md.** The newest entry in `PegSolitarie-p15/.planning/questions/FOUNDER.md` is Q-020, and in this worktree's copy Q-007. The producer records it.
7. **The THIRD_PARTY.md §1 row's wording** should describe B. A draft is in spec §9: the
   research-analyst owns the text, the release-manager the file.
8. **Documents this role still owes, each on its own dispatch:** after the merge, `docs/ux-spec.md`
   and `docs/ui/DESIGN-SYSTEM.md` (the gel tiles, the gloss finish, §10's "only drifting elements");
   and `docs/ux/licences-screen.md` (D10, D11).
9. **`docs/ux/backdrop-directions.html` now differs from the spec** in the 12 details above. It was not
   edited, as the dispatch said. The spec governs the build.

## Handoff
Next: producer —
1. Commit `docs/ux/candy-backdrop.md`, `docs/ux/colour-vision.md` and this report on `feature/candy-backdrop`.
2. Record Q-021's answer in FOUNDER.md (open issue 6).
3. Read the research-analyst's confirmation when it lands. A "not confirmed" item comes back to this role.
4. Dispatch the tech-lead for the R1–R6 addendum and the idle-frame ruling (open issues 2 and 3), with spec §6.3, §14 items 2–3 and §15.
5. Then dispatch the android-game-dev with the spec and the tech-lead's addendum.

Next: code-auditor and qa-engineer, at review — BD-01 to BD-28, with the split in spec §11 and the tech-lead's R5, and colour-vision.md's TG-AC02 and TG-AC04 as amended.
