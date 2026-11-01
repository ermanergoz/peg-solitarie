# tech-lead report — candy-backdrop: the 15m surface oracle (B-19a, handoff item 2)
Date: 2026-11-01
Branch: feature/candy-backdrop (report only)
Skills loaded: ergoz-soft:standards (role tech-lead; the worktree snapshot is v0.6.2 and the plugin is v1.0.0, so the plugin's studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose, stacks and testing were read in full, logging, puzzle-design and play-store are identical to the snapshot, and dispatch was read from the plugin; product `.claude/rules/lessons.md`), android-architecture, android-gradle-logic
Ran in parallel with: home sign-off, licences F-4 ruling
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** `agents/tech-lead.md` pins fable; this ran on Opus, as the harness names it, the same deviation `tech-lead-direction-b.md` records under Q-019. The harness does not name the effort, so the effort is the agent file's pin.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Oracle:** neither (a) as offered nor (b). The test uses a same-run reference, **drawn at each pill's own window pixels**. It is a second frame of the same composition: a reference box per pill, carrying the pill's finish modifiers exactly, is laid over Home at that pill's bounds. Frame 1 (Home) and frame 2 (the finish alone) are compared pixel by pixel over the unchanged read area. Skia's ordered dither and the edge anti-aliasing then fall in the same phase in both frames. An empty box drawn elsewhere (option (a)) would not get that, and neither would a formula (option (b)). No golden is stored (D4).
- **Tolerance:** `PAINT_TOLERANCE` stays **1** level a channel. It is not widened, because the same-pixels reference needs no slack. Only its KDoc changes.
- **Themes:** both. The light test keeps every assertion it has. A new dark test runs the same pixel oracle. `renderHome` gains `darkTheme = false` and an empty-by-default `overlay` slot, so no other caller's tree changes.
- **Reds**, against the final oracle commit: M3b in light (2), M3b in dark (3), and a glyph inside the gloss band in both themes (4). I add two: a white glyph in the band in light (4w), and the reference guard (G-red).

## Rulings

### O1 — How "the pill's finished surface at that pixel" is computed

**The reference is the finish, drawn by production code, at the same window pixels, in the same composition.** It goes in a second frame.

1. **Frame 1, Home.** `renderHome(state = bothResumesState(), darkTheme = …, overlay = { ReferenceFinishes(references) })`, where `references` is the test's `mutableStateOf(emptyList<IntRect>())`. With the list empty the overlay emits nothing, so frame 1 is the screen the test reads today. Take `home = renderedWindow()`.
2. **Frame 2, the finish alone.** Set `references` to the three pills' `IntRect`s from frame 1's `boundsInRoot`, `waitForIdle()`, then take `finish = renderedWindow()`.
3. **`ReferenceFinishes`** is a private composable in `HomeLayoutTest.kt`. For each rect it emits one `Box`:
   - placed at exactly `rect.topLeft` in px: `Modifier.offset { rect.topLeft }`;
   - sized at exactly `rect.width × rect.height` px: a `layout {}` that measures with `Constraints.fixed(...)`, not a px→dp→px round trip;
   - then the pill's own draw chain, in `StatPill`'s order and with its arguments: `.finishGlow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.medium).glossFinish(FinishedContainer.RibbonPill, MaterialTheme.shapes.medium, fill = MaterialTheme.colorScheme.surfaceContainerHigh)`;
   - then a `testTag(REFERENCE_FINISH_TAG_PREFIX + index)`;
   - and nothing else: no content, no text, no glyph.

   It is emitted inside `AppTheme(darkTheme)`, so it reads the same `LocalFinishColors`, shapes, scheme and density as the pills. The recipe comes from `FinishedContainer.RibbonPill.recipe`, so a recipe change reaches the reference by itself. A change to the pill's fill token or modifier chain does not reach it, and fails the test. That is wanted: §4.2.3 names `surfaceContainerHigh`, and anything new the pill paints is what 15m exists to catch.
4. **The read area does not change:** `boundsInRoot.deflate(spacing_sm)` minus each text's bounds inflated by `TEXT_INK_MARGIN_DP`, with the texts found through frame 1's semantics. A read pixel is a stray when any RGB channel of `home` differs from `finish` by more than `PAINT_TOLERANCE`.

**The guards inside the oracle, in this order.** Each one has its own message.

- **G1, preconditions.** The ribbon has exactly `RIBBON_PILLS` children (as today). Every edge of each pill's `boundsInRoot` lies within 0.01 px of a whole pixel, so the reference can sit on the same pixels. A fractional edge fails as a precondition. It is never rounded silently.
- **G2, placement.** In frame 2, each `REFERENCE_FINISH_TAG_PREFIX + i` node's `boundsInRoot` equals pill *i*'s rect exactly.
- **G3, the reference is a pure finish.** In `finish`, scan each pill's whole deflated rect, **texts included**. On every row, each channel's spread (max − min) must be ≤ `PAINT_TOLERANCE`. The finish there is a flat fill under a vertical gradient, so a row is uniform up to the dither's one level. This one guard proves three things: the overlay was drawn, it covers the texts (a frame 2 that still showed them would break the text rows), and the reference itself carries no glyph. Without G3 a frame 2 that silently failed to draw would equal frame 1, and every glyph would pass.
- **G4, the token anchor.** In `finish`, every deflated-rect row whose pixel centre lies at or below `top + BackdropMetrics.FINISH_GLOSS_FADE × height` equals the theme's `surfaceContainerHigh` (`LightColors` or `DarkColors`) within `PAINT_TOLERANCE`. Below the gloss band the amended read therefore checks against the same absolute token as today's.
- **G5, the comparison.** Collect the strays for **all three pills** before asserting anything, then assert that the map `pill index → stray count` equals `{0=0, 1=0, 2=0}`. The failure message gives each pill's count and span (`spanOf`), plus the span's rows relative to the pill's top in px. Today's loop stops at pill 0, so a red can never show "every pill" (B-19a item 2). Collecting first is what lets it.

**Why not (a) as offered**, an empty box of each pill's size somewhere else: this render dithers its gradients. Skia's ordered dither is keyed to device pixels; `WrongTapBoards.kt` already places its control board a multiple of 16 px away "so Skia's ordered dither of the pegs' gradients falls on both boards in the same phase". A box elsewhere would differ from the pill by a level here and there, and that would force slack the property does not need. It would also need a second `setContent`, which `createComposeRule` refuses.

**Why not (b)**, an analytic per-row composite: the formula would have to restate the renderer. That means how the gradient is sampled at pixel centres, the interpolation toward a stop of transparent black (`Color.Transparent` is `0x00000000`), the dither, and float-to-byte rounding. That is a model of Skia, not of the spec. It needs a tolerance of at least 2, and it goes stale whenever the finish's drawing changes, for reasons that have nothing to do with a glyph. The reference costs one frame and models nothing.

**Stop rule (feasibility I could not run without writing test code).** On the unmutated build, the developer records the largest RGB channel difference between `home` and `finish` over the read area, for each pill and each theme. It is expected to be **0**. Frame 1 draws the pill's fill and gloss through the same two modifiers, at the same size and pixels. The entrance layer ends at alpha 1 with no translation, which the old flat-fill read already proved by passing before B-19. The largest G3 spread is expected to be ≤ 1. If either number is larger, the developer does not widen anything: stop, and report the values and where they occur to the producer for this role.

### O2 — The tolerance constant

`PAINT_TOLERANCE = 1` is kept, with the same name and the same value. B-19a item 5 allows widening only by "what the gloss gradient's own rendering needs". With the reference on the same pixels that is nothing, and today's one level already covers the dither's ±1 at G3. Its KDoc becomes: "One level a channel: the reference is the pill's own finish drawn by the same modifiers on the same pixels, dither phase included, so it renders exact; the level is the dither's, and every glyph red fails with it." G3, G4 and G5 all use it. No second constant is introduced.

### O3 — Both themes

**Yes: the test runs in light and in dark.**

- `HomeTestFixture.kt`: both `renderHome` overloads gain `darkTheme: Boolean = false`, passed to `AppTheme(darkTheme = darkTheme)`, and `overlay: @Composable () -> Unit = {}`, emitted inside `AppTheme` right after `HomeScreen`. The comment "Light, stated rather than read from the configuration" becomes "Light unless a test states dark; never read from the configuration". With the defaults, every existing caller composes exactly the tree it composes today.
- `theNarrowClassPillPutsItsLabelBesideItsValueAndComposesNoGlyph` keeps its name, its window and every assertion. It now passes `overlay` and calls the amended oracle, in light.
- New: `theNarrowClassPillComposesNoGlyphInTheDarkThemeEither()`, on `SHORT_PHONE_QUALIFIER`, `renderHome(state = bothResumesState(), darkTheme = true, overlay = …)`, then the same oracle. The one-line and ≤ 56dp asserts are not repeated; they do not depend on the theme.
- The oracle's KDoc is rewritten to say what it now reads. Its name stays `assertEachPillPaintsOnlyItsSurfaceAndItsTexts`, because "surface" now means the finished surface (15m's pointer, 2026-10-31).
- `LightColors.surfaceContainerHigh` is no longer the expected colour of every read pixel; it survives only in G4, chosen by theme.

**Scope of the BD-19 carve-out, for the auditor.** The diff touches only `HomeLayoutTest.kt` (the oracle, its KDoc, `PAINT_TOLERANCE`'s KDoc, one new test method, `ReferenceFinishes`, one tag-prefix constant) and `HomeTestFixture.kt` (the two defaulted parameters). The fixture slot is how the oracle is computed. Its defaults are the reason no other phase-15 test changes. No production file changes.

### O4 — The reds, shown before the auditor's approval

Every red is a temporary, uncommitted mutation, run against **the final oracle commit**, whose hash goes in the developer's report. For each one the developer quotes the mutation as a diff of ≤ 10 lines, so the auditor can re-apply it with `git apply`. The developer also quotes the test's failure message verbatim. Then the mutation is restored and the test is green again.

| Red | Mutation (in `CompactPillContent`, before `PillLabel` in its `Row`, unless stated) | Run | Must show |
|---|---|---|---|
| **2**, light (B-19a item 2) | The H-1 audit's M3b: the full pill's `Icon(glyph, contentDescription = null, tint = onSurfaceVariant)`, 20dp (`home_stat_pill_glyph_size`), gold fill when gilded. This means threading `glyph`/`gilded` into `CompactPillContent`, as M3b did (`android-game-dev-h1-fix.md`, F-2) | light test | G5 fails with a count > 0 on **each** of pills 0, 1 and 2; G1–G4 pass |
| **3**, dark (item 3) | the same M3b | dark test | the same, in dark |
| **4**, inside the gloss band (item 4) | M3b's glyph with `Modifier.align(Alignment.Top).size(10.dp)` instead of 20dp. At mdpi the compact pill is 40 px tall and the gloss ends at 0.46 × 40 = 18.4 px, so the glyph's rows `top+8 … top+17` lie wholly inside the band | light **and** dark tests | G5 fails on every pill in both themes. Every reported stray row lies in `[top + spacing_sm, top + 0.46·h)` |
| **4w**, a white glyph in the band (mine) | Red 4's glyph with `tint = Color.White`: the colour nearest the light gloss | light test | G5 fails on every pill. This shows a level-1 tolerance is tight inside the band: by my arithmetic, the light gloss at the band's first read row is still about 9–13 levels below white in R and G |
| **G-red**, the reference is real (mine) | **test-side:** `ReferenceFinishes` emits nothing (body commented out) | light test | **G3** fails ("frame 2 still shows its texts"), so a frame 2 that does not draw cannot pass |

Also: **unmutated, both themes green**, with the stop rule's two numbers recorded. Then the full Check (`./scripts/check.sh` or `./gradlew check`), all 1,294 tests green, with `HomeLayoutTest` at 17 of 17.

## Outputs
- /Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/tech-lead-15m-oracle.md: this ruling and report. No product code, test code or doc was written.

## Checks
- `./gradlew --max-workers=2 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.home.HomeLayoutTest'` on `9a5550f`: fail, as expected (16 tests, 1 failed: "pill 0 paints 444 px besides its surface and its texts, x 16..109, y 176..184 (px) / expected: 0 / but was: 444"). This confirms the baseline red. The strays lie on 9 rows starting 8 px below the pill's top, at mdpi: inside the top 46% of a 40 px pill, and nowhere near the 1 px rim or the 2 px lip. So the gloss is the only part of the finish the read area meets, as B-19a says. The other 15 pass.
- Read `HomeLayoutTest.kt` lines 1–130, 400–500 and 610–838; `HomeTestFixture.kt` `renderHome`; `testing/RenderedWindow.kt`: pass. The oracle reads one light `surface` with `PAINT_TOLERANCE` 1; `renderHome` hard-codes `AppTheme(darkTheme = false)`; `renderedWindow` is the software path.
- Read `ui/theme/Finish.kt`, `FinishRecipe.kt` and `ui/home/HomeProgressRibbon.kt`; grepped `Color.kt` for the panel tokens: pass.
  - `StatPill` = `finishGlow(elevation_card, shapes.medium)` then `glossFinish(RibbonPill, shapes.medium, fill = surfaceContainerHigh)` on a transparent `Surface`.
  - The gloss is `verticalGradient(panelGloss → Color.Transparent at FINISH_GLOSS_FADE = 0.46)`.
  - The gloss is `#B3FFFFFF` in light and `#0FFFFFFF` in dark, over `#F1EAFB` and `#2E2740`.
  - `shapes.medium` = 20dp, rim 1dp, lip 2dp. At an 8dp inset the lip's corner sliver ends by 5.7dp, outside the read.
- Read `ui/theme/FinishModifierTest.kt`, `ui/backdrop/BackdropPixelTests.kt` (D4's two-frame idiom, `MIRROR_TOLERANCE` 3) and `WrongTapBoards.kt:440` (the ordered-dither note): pass. That note is the evidence behind O1's choice.
- Read `docs/ux/candy-backdrop.md` B-19, B-19a and BD-19; `docs/ux-spec.md` 15m and its 2026-10-31 pointer; `ux-designer-15m.md`; `android-game-dev.md`; `15-founder-feedback/tech-lead-signoff-close.md` items 1 and 8; `android-game-dev-h1-fix.md` F-2: pass. Item 8 of my earlier sign-off already ruled that this oracle changes with B and is watched red against M3b again. O4 does that.
- No mutation or probe was run: each needs test or product code, which this dispatch forbids. The stop rule in O1 hands that measurement to the developer.

## Open issues / risks
1. **The equality is argued, not yet measured.** The stop rule covers the case where it fails: no widening, and the developer reports back.
2. **A merge interplay with `bugfix/home-chip-arrival`.** Item 1 of `tech-lead-signoff-close.md` hardens the same oracle there: `textsWithin()` must be exactly `[label, value]`, watched red against M3d. The two edits are orthogonal, but they touch the same function. Whichever lands second keeps both, re-runs M3b and M3d on the merged oracle, and keeps G1–G5.
3. **The tech design is not synced.** §8.5's pointer to same-run pixel comparisons does not yet name this two-frame, same-pixels form. It is owed at the next tech-design sync, not on this branch.
4. **Density.** The reds' arithmetic assumes Robolectric's default mdpi on `w360dp-h640dp`, which the baseline run confirms (`x 16` = 8dp + 8dp). If the qualifier ever gains a density, red 4's glyph must be resized so it still lies inside the band. The test's own "rows inside the band" report shows it either way.

## Handoff
Next: producer.
1. **android-game-dev:** build O1–O3 on `feature/candy-backdrop` (test-only: `HomeLayoutTest.kt` and `HomeTestFixture.kt`). Record the stop rule's two numbers, show reds 2, 3, 4, 4w and G-red against the final oracle commit as O4 sets out, and re-run the full Check.
2. **code-auditor:** review that diff against B-19a and this report. The diff must stay within O3's scope list. Re-apply at least red 4w and G-red. Write the "amended, not weakened" line BD-19's carve-out requires, or refuse it.
3. **tech-lead:** the branch sign-off stays owed after the auditor's and QA's reports.
