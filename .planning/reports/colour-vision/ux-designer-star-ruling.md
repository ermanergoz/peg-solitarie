# ux-designer report — colour-vision (D-1's UI half: the stars, the hollow glyph, the StarBorder wording)
Date: 2026-10-24
Branch: feature/colour-vision (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer). The product snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, dispatch; also the product's `.claude/rules/lessons.md`. Then ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: home-chip-arrival (tech-lead ruling), talkback-board (qa-engineer), licences-screen (code-auditor, ux-designer ruling), reduced-motion (ux-designer ruling), colour-vision (code-auditor; tech-lead ruling on D-1/D-2/D-6)
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** the ux-designer is pinned to fable. This ran on Opus because the Fable limit is reached, under the founder's Q-019 answer (option 3: every Fable-pinned role on Opus until 3.0.0 is published). Mitigation: no ratio here is new (§12.7), and the visual requirement is checked by CV-AC05, CV-AC08 and CV-AC11 as amended, by the auditor and on the device.
Fan-out: none. One haiku Explore helper ran read-only greps and one class listing, because this role has no Grep, Glob or Bash. It ran no git command and wrote nothing. No studio role was dispatched.
Loops: 0
Lessons applied: none (the 0.6.2 snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Ruling 1: not acceptable.** In `material-icons-core`, `Icons.Outlined.Star` is a filled star, so today's standard earned and unearned stars share one silhouette and one fill and differ by colour alone. No visible text repeats the count. rules/compose.md's "never colour alone" has no default-palette exemption. **Required, in both palettes:** the unearned star is the hollow star; the earned star stays filled; no standard colour changes (CV-13a, §12.2).
- **Ruling 2: accepted.** `StarOutline`, as built from `Icons.TwoTone.Star`'s opaque layer, is Material's `star_border` artwork: the filled star's silhouette with an open interior, at the icon's own band weight. §12.3 gives the visual requirement it meets. The construction is the tech-lead's.
- **Ruling 3: the wording is corrected.** The spec now names the shape ("the hollow star", `star_border`) instead of an icon constant. `StarBorder` is not in `material-icons-core` 1.7.8, so the tech-lead's proposed name was not bindable either (CV-10a, §12.4).
- **Founder question.** The standard-palette question drafted on 2026-10-17 was never queued. It is re-drafted below, with the stars as their own option. Recommendation: option 2. The build proceeds on option 1 plus CV-13a.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-cv/docs/ux/colour-vision.md`, amended in place in the house style, with superseded text kept and dated pointers added:
  - the front-matter `revised:` line;
  - **CV-10a** (after CV-10) and **CV-13a** (after CV-13);
  - pointers in §0, in §2's star row and its conclusion, in §3.4's two star rows and in §3.7;
  - **CV-AC05, CV-AC08, CV-AC11, CV-AC12** amended in their cells;
  - §7's F-CV4 and the founder-question paragraph;
  - a new §9 asset row;
  - a new **§12**, "Amendments for deviation D-1: the hollow star": 12.1 the findings, 12.2–12.4 the three rulings, 12.5 the criteria changes, 12.6 F-CV4 and the re-drafted question, 12.7 what does not change.
- `/Users/ermanergoz/Desktop/PegSolitarie-cv/.planning/reports/colour-vision/ux-designer-star-ruling.md`: this report.

## Checks
- `Read` of `docs/ux/colour-vision.md` (786 lines, two reads), `.planning/reports/colour-vision/android-game-dev.md` and `.planning/reports/colour-vision/tech-lead-brief.md` (526 lines, two reads): pass, read in full.
- `Read` of `ui/components/StarGlyph.kt`, `ui/theme/BoardMarks.kt`, `ui/game/BoardMarkRecipe.kt` and `ui/theme/Color.kt` in the worktree (dispatch: head `a950417`, code `32b2600`): pass.
  - The standard unearned star is `Icons.Outlined.Star` tinted `outlineVariant` (`#DCD3EC` light, `#3A3350` dark).
  - The colour-blind unearned star is `StarOutline`; `unearnedStarHollow` is `false` for Standard.
- Helper, `grep -rn` for `Icons.*.Star*`, `StarOutline` and `StarGlyph(` under `app/src/main`: pass.
  - Earned and unearned stars sit together only in `ResultsScreen.kt` `StarRow` (48dp) and `BoardPreview.kt` `EarnedStarPips` (16dp).
  - `TutorialScreen.kt`'s completion star is always earned. `HomeProgressRibbon.kt:90` is a single decorative `Icons.Filled.Star`.
- Helper, the class list of `material-icons-core-release.aar` 1.7.8 from the Gradle cache (`classes.jar`, 311 entries), read in memory: pass. The star classes are `filled/`, `outlined/`, `rounded/`, `sharp/` and `twotone/StarKt`. **No `StarBorder` and no `StarOutline`.** This confirms D-1's premise. The helper did not confirm how the BOM maps to 1.7.8; the developer's report names 1.7.8.
- Helper, `ResultsScreen.kt:195-300` and `BoardPreview.kt:85-109` printed: pass. The star count reaches TalkBack only (`EarnedStarCountKey`; the catalogue row's `clearAndSetSemantics`). No visible text repeats it.
- Helper, `grep -n` for unearned, hollow, `Outlined.Star`, `StarBorder` and `star_border` in `docs/ux-spec.md` and `docs/ui/DESIGN-SYSTEM.md`: no match. No higher document specified the unearned glyph.
- Helper, `grep -n` for the colour-blind and standard-palette terms in `.planning/questions/FOUNDER.md`: Q-026 only, answered "1 — keep it in 3.0.0" (2026-10-24). The question drafted on 2026-10-17 has no entry.
- Helper, `grep -rn` for `unearnedStarHollow` and `StarOutline` under `app/src/test`: `BoardMarkRecipeTest.kt:32` pins `standard.unearnedStarHollow` to `false`, which CV-13a makes stale. `BoardMarksTest` pins the colours, which are unchanged. `StarGlyphTest` samples the outlines.
- **Path agreement, not verified on the compiled vector.** Two claims rest on Material Icons' published artwork: that `TwoTone.Star`'s opaque layer is `star_border`, and that `star_border`'s outer edge matches `Icons.Filled.Star` within 0.01 units. The developer's KDoc says the same. `StarGlyphTest` and QA's CV-AC11 graded item are the checks.
- `./gradlew check`: not run. No code or resource changed, and this role has no Bash.
- `python3 …/ui-ux-pro-max/scripts/search.py`: not run (no Bash). I applied by reading: Quick Reference §1 `color-not-only`, §6 `color-not-decorative-only`, §10 `pattern-texture`, the "Filled vs Outline Discipline" row, and the pre-delivery item "Color is not the only indicator". android-accessibility §4 (state descriptions) applies: the stars stay decorative, and the rows keep speaking their count.

## Open issues / risks
1. **The standard unearned star gets fainter.** It is hollow in `outlineVariant` at 1.20–1.50:1 (F-CV4), so at 16dp its band is about 1.1dp and close to invisible. The earned count still reads by which stars are filled. The fix is a colour change in the approved look, so it is option 2 of the drafted question below. It is a one-value change.
2. **Tests that pin the old standard star now disagree with the spec.** They are `BoardMarkRecipeTest.kt:32`; any standard-palette case in `StarGlyphTest`; the off-palette identity claims of CV-AC05; and the tech-lead brief §11's "the standard look" freeze. The tech-lead rules the mechanism and the developer edits. Every existing-test edit needs the code-auditor's approval.
3. **For the tech-lead's parallel ruling.** The brief's §4.1 calls `Icons.Outlined.StarBorder` "the hollow star the app already draws for unearned stars". The app drew a filled star, and `StarBorder` is not in `material-icons-core` 1.7.8. §12.3 states the visual requirement for any construction. A copied path needs a THIRD_PARTY.md row; `material-icons-extended` would be a new dependency.
4. **Not ruled here, outside this dispatch:**
   - the tech-lead brief's Open issues 2 (the wrong-tap mark's row in CV-AC08);
   - D-6 (the Results-sky floors; the tech-lead's);
   - spec §10 item 6's folds. When they land, `docs/ux-spec.md` §11's "State, not just colour" table should record the stars as filled against hollow.
5. **Parallel edits on one branch.** The code-auditor and the tech-lead work on `feature/colour-vision` at the same time. This role edited only `docs/ux/colour-vision.md` and wrote this report. If the tech-lead's ruling also edits the spec, the producer reconciles the two before committing.
6. **Staffing deviation (Q-019).** The ruling ran on Opus. The mitigation is in the `Ran on:` line.

## Handoff
Next: producer —
1. Commit `docs/ux/colour-vision.md` and this report on `feature/colour-vision`, for example `docs(colour-vision): rule on the hollow star (D-1)`.
2. Queue the founder question below as the next free `Q-<NNN>`, non-blocking. The build proceeds on option 1 plus CV-13a.
3. Give the tech-lead's D-1/D-2 ruling spec §12. The visual requirement is in §12.2 and §12.3. CV-AC08 now covers the star pair in both recipes (§12.5).
4. Once the tech-lead has ruled, re-dispatch the android-game-dev:
   - draw the standard unearned star hollow, by the tech-lead's mechanism, and update the pins of Open issue 2;
   - then the code-auditor on CV-AC05 and CV-AC08 as amended;
   - then QA on CV-AC11's graded star item and CV-AC12's 16dp note.
5. Later, non-blocking, dispatch the ux-designer for Open issue 4's items.

Next: tech-lead — the construction of the hollow star in both palettes, against §12.3's visual requirement, and D-2's test mechanism against CV-AC08 as amended.

### Drafted founder question (FOUNDER.md format; the producer assigns the id)

    ## Q-NNN — Should the standard colours get the stronger marks and stars too?   (open)
    Date: 2026-10-24    Asked by: ux-designer
    Context: The colour-blind palette (a Settings switch, off by default) draws the board's marks and the stars so that any colour vision can read them. In your standard colours they are faint by the studio's contrast rule, though normal colour vision tells them apart. Empty stars are now hollow in both palettes, a rule fix, so in your colours an empty star is a thin pale outline that is easy to miss at the catalogue's small size. This is taste: it changes the look.
    Options: 1) Keep the standard colours exactly as they are (default, being built)  2) Stars only: the empty star's outline takes the colour-blind palette's deeper grey; the board is unchanged  3) Everything: the standard colours also get the solid move dots, the darker amber hints in light and the colour-blind palette's stars (the halo and the dashed hint rings stay colour-blind only)
    Recommendation: ux-designer picks 2 for 3.0.0. Empty stars stay readable as outlines, the board you approved is untouched, and it is one value. 3 is worth a look after 3.0.0 ships, once you have seen the colour-blind palette on a device.
    Blocks: nothing (option 1 is specified and being built; 2 and 3 are constant changes, docs/ux/colour-vision.md §3.4, §7 and §12.6)
    Answer:
