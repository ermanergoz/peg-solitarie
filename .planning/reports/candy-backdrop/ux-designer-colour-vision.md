# ux-designer report — candy-backdrop (colour vision: the colour-blind palette and the Home title)
Date: 2026-10-17
Branch: feature/candy-backdrop (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer; the plugin's rules v1.0.0 read in full because the product snapshot is 0.6.2: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, dispatch; product `.claude/rules/lessons.md`), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: phase-15 qa-engineer loop 3, candy-backdrop tech-lead ruling
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** the ux-designer is pinned to fable. This ran on Opus under the founder's answer to Q-019 (2026-10-17: Opus for every Fable-pinned role until 3.0.0 is published), recorded in `PegSolitarie-p15/.planning/questions/FOUNDER.md`. Mitigation: every number in the spec is re-proved by a JVM test (CV-AC07, TG-AC02) and by the qa-engineer's own device and screenshot checks (CV-AC11–14, TG-AC03–04), not trusted from this document.
Fan-out: none. Four haiku helpers did work this role's tools cannot (it has no Glob, Grep or Bash): one read-only Explore listed files and grepped the p15 worktree; three general-purpose helpers each ran one read-only Python script from the session scratchpad. No studio role was dispatched.
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
Ran on Opus, not the pinned Fable, by Q-019. Wrote `docs/ux/colour-vision.md`, which covers both fixes routed from backdrop §14.
**Colour-blind palette:**
- It is a "Colorblind palette" switch in Settings › General after Haptics, off by default, kept in DataStore.
- Pegs, holes, the dock, the backdrop and Home are unchanged. The audit found that their shape and luminance already carry every distinction.
- It adds only what the audit found missing: a halo inside the selection ring, opaque move dots, dashed deep-amber hint rings, and stars and pips with a visible fill and outline.
- Every "≥ 3:1" pair holds under normal vision, protanopia, deuteranopia, tritanopia and monochromacy.

**Home title:**
- It is large-scale text, so the bar is 3:1. Today it fails that at large font scale.
- It gets its own violet gradient: `#7C3AED → #5B21B6` in light, `#C4B5FD → #A78BFA` in dark. That reads ≥ 5.1:1 and ≥ 6.5:1 at rest.

**Other findings:**
- The backdrop spec's dark frosting lip sits under a wrapped title at 1.1–1.6:1. Routed to the backdrop's owner.
- One founder question is drafted below.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/docs/ux/colour-vision.md`: the spec. It contains:
  - decisions CV-01–CV-13 and TG-01–TG-02;
  - the board audit (§2);
  - the palette: setting, persistence, plumbing, marks, deficiencies served with the computed table, motion (§3);
  - the title fix and its reach over the backdrop (§4);
  - acceptance criteria CV-AC01–CV-AC15 and TG-AC01–TG-AC05 (§5) and tests owed (§6);
  - standard-palette findings F-CV1–F-CV6 (§7), rejected alternatives (§8), assets (§9) and open items (§10).
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/ux-designer-colour-vision.md`: this report.
- Working scripts, not committed, kept in the session scratchpad only:
  - `/private/tmp/claude-501/-Users-ermanergoz-Desktop-PegSolitarie/b4ba853a-5267-41d9-906d-30baa9c15e39/scratchpad/cvd/cvd_check.py`
  - `/private/tmp/claude-501/-Users-ermanergoz-Desktop-PegSolitarie/b4ba853a-5267-41d9-906d-30baa9c15e39/scratchpad/cvd/cvd_check2.py`
  - `/private/tmp/claude-501/-Users-ermanergoz-Desktop-PegSolitarie/b4ba853a-5267-41d9-906d-30baa9c15e39/scratchpad/cvd/cvd_check3.py`

  The permanent form of the same computation is CV-AC07's JVM test.

## Checks
- `./gradlew check`: not run. No code or resource changed, and this role has no Bash.
- `python3 -I …/scratchpad/cvd/cvd_check.py`, run by a haiku helper: exit 0. It computes WCAG luminance and contrast, Machado 2009 severity-1.0 protanopia, deuteranopia and tritanopia on linear sRGB, luminance-only monochromacy, and CIEDE2000.
  - Proposed palette: **pass**. Ring 4.17–5.34; ring against halo 4.76–7.05; legal-target mark 4.17–5.34; hint rings 3.82–9.01.
  - Standard palette: **six shortfalls recorded** against `docs/ux-spec.md` §11's 3:1 for glyphs and boundaries. Legal-target mark 1.67–2.04; light hint rings 1.61–1.98; ring against peg 1.02; unearned outline 1.20–1.50; light gold fill 1.09–2.22; dark hint against selection in greyscale ΔE00 7.9.
  - The proposed hint and the selection reach 1.13:1 in light greyscale, which is why the hint rings are dashed.
- `python3 -I …/scratchpad/cvd/cvd_check2.py`: exit 0. It samples the peg's drawn radial-gradient edge every 15°.
  - Standard ring against the edge: 1.02–1.18 in every vision.
  - Halo against the edge (information): shaded half ≥ 3.36 light / ≥ 2.85 dark; lit side ≥ 2.33 light.
  - Star fill: amber-600 `#D97706` failed (2.33 under deuteranopia on the Results sky), so it was replaced.
- `python3 -I …/scratchpad/cvd/cvd_check3.py`: exit 0. Of five candidate light stops for the star fill, `#C05A08` was chosen: its worst case is 3.22 across the background, the Results sky and the card, in every vision.
- Title, from the same run:
  - today, light: stop 2.44, glyph band ≈ 2.6–3.2; dark: deep stop 4.17;
  - proposed: **pass**, light 5.10–8.05 and dark 6.49–9.56 on the background, ≥ 4.55 over the zone-T tokens and the pink orb, and both monotone;
  - over the dark backdrop frosting lip: **fail**, 1.08–1.59, routed (§10 item 1).
- Words check for the two new strings ("Colorblind palette", "Clearer moves, hints and stars") against the backdrop's BD-01 list, by reading: pass.
- The Settings entrance orders were cross-checked against `docs/ux/licences-screen.md` §2: both numberings are specified, and they are consistent.
- `python3 …/ui-ux-pro-max/scripts/search.py`: not run (no Bash). The skill's Quick Reference §1, §2, §6 and §7 and its pre-delivery checklist were applied by reading.
- The board-drawing code (`BoardCanvas.kt`, `BoardGeometry.kt`, `BoardMetrics.kt`), the dock (`DockLook.kt`, `GameActionDock.kt`), the star and pip renderers, Settings and its repository, the theme, `HomeHero.kt` and `MainActivity.kt` were all read directly on `PegSolitarie-p15`. The helper's greps ran only on that worktree.

## Open issues / risks
1. **The backdrop spec's dark claim is wrong for one case.** It says "dark: ≥ 4.2:1 everywhere" (`docs/ux/candy-backdrop.md` §5.1). But a title wrapped to three lines (200% font on a 360dp phone) puts its last line over the far mounds' dark frosting lip (`#A98BC2`, L 0.308), at 1.08–1.59:1. Today's title does no better. No title colour can fix it. The proposed rule for the backdrop: in dark Landscape, no colour brighter than L 0.0786 above about 0.47 H. TG-AC04 fails until that document is amended. I could not edit it under this dispatch.
2. **Device readings govern.** Android's "Simulate color space" offers only the anomalous forms, and `screencap` may capture the unsimulated frame. CV-AC11 therefore asks QA to record which it saw, and CV-AC12 gives the screenshot path with severity-1.0 transforms.
3. **The halo against the lit side of the peg** reads ≥ 2.33:1 in light (under deuteranopia). This is recorded as information: what WCAG asks of the indicator (the ring against both of its neighbours) is ≥ 4.17:1. If a reviewer reads the halo against the peg as a requirement too, a uniform band cannot reach 3:1 all round on this gradient (spec §3.5 row 3).
4. **The standard palette sits below §11's 3:1** in six places (§7). This is pre-existing and not introduced here. A founder question is drafted below; the default keeps today's look for 3.0.0.
5. **Settings renumbering** interacts with the licences screen's About section. Whichever lands second renumbers on top of the first (§3.1).
6. **Build order.** Both fixes build on phase 15's Settings and Home, so they come after `feature/phase-15-ux-refresh` merges. Neither depends on the backdrop, except TG-AC04.
7. **Docs owed after the phase-15 merge** (this role's): `docs/ux-spec.md` §4.5, §6.2 and §11, and `docs/ui/DESIGN-SYSTEM.md`'s Gradients and a colour-blind section (§10 item 6).
8. **Staffing deviation (Q-019):** the spec was written on the developer's model. The mitigation is the reviewers' own computation and device checks (CV-AC07, CV-AC11–14).

## Handoff
Next: producer —
1. Commit `docs/ux/colour-vision.md` and this report on `feature/candy-backdrop`.
2. Queue the founder question below as the next free `Q-<NNN>`, non-blocking. The build proceeds on option 1.
3. Route open issue 1 (the dark frosting lip under a wrapped title) to the ux-designer as an amendment to `docs/ux/candy-backdrop.md`, once the tech-lead's reading of it is done.
4. Dispatch the tech-lead on §3.3: how the setting reaches `AppTheme` once at the root, meeting its three properties.
5. Once phase 15 has merged, dispatch the android-game-dev to build §3 and §4.
6. Give the code-auditor and the qa-engineer CV-AC01–CV-AC15 and TG-AC01–TG-AC05.
7. Decide whether both fixes go into 3.0.0. They close a rules/compose.md requirement and a §11 gap, and nothing in them waits on the founder.

### Drafted founder question (FOUNDER.md format; the producer assigns the id)

    ## Q-NNN — Should the standard colours get the stronger board marks too?   (open)
    Date: 2026-10-17    Asked by: ux-designer
    Context: The new colour-blind palette (a switch in Settings, off by default) makes the board's marks easy to see for everyone: solid move dots, darker amber hint rings in the light theme, outlined stars. In the standard colours you approved, those marks are faint by the studio's own contrast rule, though normal colour vision tells them apart. This is taste: it changes how the board looks.
    Options: 1) Keep the standard colours exactly as they are; the stronger marks stay inside the colour-blind palette (default, being built)  2) The standard colours also get the solid move dots, the darker amber hints in light and the outlined stars (the halo around the selected peg and the dashed hint rings stay colour-blind only)
    Recommendation: ux-designer picks 1 for 3.0.0. It keeps the board you approved, and anyone who needs it gets a fully readable board one switch away. 2 is worth a look after 3.0.0 ships, once you have seen the colour-blind palette on a device.
    Blocks: nothing (option 1 is specified and buildable; option 2 is a constant change, docs/ux/colour-vision.md §3.4 and §7)
    Answer:
