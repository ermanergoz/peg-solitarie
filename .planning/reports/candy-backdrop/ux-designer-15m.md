# ux-designer report — candy-backdrop: the 15m / B-19 spec conflict
Date: 2026-10-31
Branch: feature/candy-backdrop (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer; the snapshot is v0.6.2 and the plugin is v1.0.0, so these plugin rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, dispatch; product `.claude/rules/lessons.md`; `.planning/recall/RECALL.md` does not exist), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort high (Q-019, and the founder's 2026-10-31 directive to run one step lower)
Fan-out: none (one Explore helper located 15m's lines in the 6,901-line ux-spec.md and the test's line range, because this role has no Grep. It ran without `model: "haiku"`, which rules/dispatch.md requires. That deviation is recorded here.)
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **Ruling: option (b).** B-19's gloss stays on every ribbon pill. Criterion 15m's requirement ("no decorative glyph") stands as written. Only its pixel oracle changes: it compares each read pixel with the pill's own finished surface at that pixel, in the same theme and the same run, instead of with flat `surfaceContainerHigh`.
- **Why:** 15m is about a glyph, and the gloss is not one. The flat-fill read stood in for "no glyph" because the pill was flat when phase 15 wrote it. Option (a) removes the gloss from every ribbon pill, because the recipe is per container. The ribbon would then sit flat between the glossed gear chip and the glossed Continue chips, against the founder's pick (Q-021). Accessibility favours neither option (§5.3: ≥ 4.66:1 in dark at the top edge; light only gains).
- **Recorded as** B-19a in `docs/ux/candy-backdrop.md`, with BD-19 amended (one carve-out from "no test edited"), and a dated pointer under 15m in `docs/ux-spec.md`. This is a UX decision, not a founder question.

## Outputs
- /Users/ermanergoz/Desktop/PegSolitarie-candy/docs/ux/candy-backdrop.md — new decision B-19a in §1.2, after B-19: the conflict, the ruling, the five properties the amended read must prove (light and dark pass; three reds; a named tolerance), BD-19's carve-out and the reasons. BD-19 in §11 is marked amended 2026-10-31, and 15m is added to its list. The frontmatter `revised:` line is updated.
- /Users/ermanergoz/Desktop/PegSolitarie-candy/docs/ux-spec.md — criterion 15m gains a pointer dated 2026-10-31 (B-19a), worded in the house style. The criterion's text is not rewritten. The frontmatter `revised:` line is updated.
- /Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/ux-designer-15m.md — this report.

## Checks
- Read `.planning/reports/candy-backdrop/android-game-dev.md` in full — pass (Open issue 1 states the conflict. The failure is "pill 0 paints 444 px besides its surface and its texts", and it is the Check's only failure, 1 of 1,293).
- Read `docs/ux/candy-backdrop.md` in full (1,765 lines, three pages) — pass (B-19 and §3.5.1 give the ribbon pills rim, lip, gloss and glow; §5.3 gives the dark gloss ≥ 4.66:1 on the ribbon labels; BD-19 says "no test edited").
- Read `docs/ux-spec.md` 15m (lines 5992–6002) and §4.2.3 part 2 (lines 2317–2443) — pass (15m's text requires "no decorative glyph". It does not name a flat fill).
- Read `HomeLayoutTest.kt` lines 385–429 and 640–838 — pass (`assertEachPillPaintsOnlyItsSurfaceAndItsTexts` reads `deflate(padding)` minus the texts' ink margin against one `surface` colour, with `PAINT_TOLERANCE` = 1. The read area leaves out the 1dp rim and the 2dp lip, so only the gloss enters it).
- Read `ui/theme/FinishRecipe.kt` and `ui/home/HomeProgressRibbon.kt` — pass (`RibbonPill` → `GLOSSED_CHIP`. One recipe serves both pill shapes, so option (a) would change the full pills too).
- No command was run: this role has no Bash, and the Check is not this role's to run.

## Open issues / risks
1. **The Check stays red until the oracle is amended.** This ruling does not make it green by itself. The test edit is the developer's, and the tech-lead specifies it (Handoff).
2. **The test edit needs the auditor's approval.** Editing a phase-15 oracle is exactly the change that could weaken it silently. B-19a requires reds 2–4 to be shown failing, and the code-auditor writes the line "amended, not weakened". Without that line the edit does not merge.
3. **The ux-spec amendments B-19 owes after the merge** (§14 item 9) are still owed. They include §4.2.3 part 2 item 3's "`surfaceContainerHigh` + `clayShadow(elevation_card)`", which B-19 supersedes for the ribbon. This report does not make them.
4. **Process deviation:** the Explore helper ran on the inherited model, not haiku (rules/dispatch.md). It was read-only, and it reported line numbers that I then read myself.

## Handoff
Next: producer —
1. Commit the two docs and this report on `feature/candy-backdrop`.
2. **tech-lead — the follow-up for the test's check.** Specify how `assertEachPillPaintsOnlyItsSurfaceAndItsTexts` computes "the pill's finished surface at that pixel". Two candidates:
   - a same-run reference render of the finish alone: `glossFinish(RibbonPill, shapes.medium, fill = surfaceContainerHigh)` on an empty box of each pill's size, in the same theme, compared pixel by pixel (the D4 idiom, no stored golden);
   - an analytic composite per row: the fill under `BackdropPanelGloss` at the gradient's alpha for that row.

   The tech-lead also rules on three more things:
   - the tolerance constant;
   - running the test in both themes;
   - the three reds of B-19a items 2–4: the M3b glyph in light, the M3b glyph in dark, and a glyph inside the top 46%.
3. **android-game-dev:** build the tech-lead's specification, then re-run the Check.
4. **code-auditor:** review the oracle diff against B-19a, and approve it with its reds as the BD-19 carve-out requires.
