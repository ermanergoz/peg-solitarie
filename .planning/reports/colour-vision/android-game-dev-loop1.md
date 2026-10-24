# android-game-dev report — colour-vision, fix loop 1 (code-auditor F-1, F-2; tech-lead D-1, D-6; ux-designer CV-13a; founder Q-027)
Date: 2026-10-24
Branch: feature/colour-vision (8600c49..0f1f58f)
Skills loaded: ergoz-soft:standards (role android-game-dev; the worktree's snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin's rules were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing; and `.claude/rules/lessons.md`), superpowers:test-driven-development (with its writing-good-tests.md), gitflow, compose-skill, compose-ui, android-testing
Ran in parallel with: home-chip-arrival developer loop 2, licences-screen developer F1 fix and QA re-run, talkback-board QA, tech-design RM-close audit
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 1
Lessons applied: none (the 0.6.2 snapshot holds 0 active lessons)

## Summary
- CV-13a: the standard recipe's unearned star is hollow (D-1 item 1), so the unearned star is the hollow star in both palettes. Q-027 option 2: the standard palette's unearned outline is the colour-blind palette's `onSurfaceVariant` (D-1 item 2). Both land before the 15e sitting.
- F-2: three new same-run pixel tests cover the Results row, a catalogue card, and the tutorial's done pip and completion star. They prove the amended CV-AC05 clause in both palettes and both themes, and `EarnedStarCountKey` and the pip rows' descriptions across a flip. The auditor's M16 + M17 pair, which passed all 1,258 tests at review, now fails three tests.
- F-1: `StarGlyph` previews, light and dark, earned and unearned, both palettes. D-6: the test-only rename and its two KDocs, verbatim from the ruling. F-4 (optional): the pair test's KDoc now says what the test proves. F-3 was not taken.
- `./scripts/check.sh --max-workers=4` is green on 0f1f58f: 11m 9s; `:app` 1,265 tests, `:domain` 296; 0 failures; exit 0.

## Outputs
Commits, each a conventional subject ending with the one trailer `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`:

| Commit | Subject | Carries |
|---|---|---|
| 8600c49 | feat(colour-vision): draw the unearned star hollow in both palettes | CV-13a: `STANDARD_RECIPE.unearnedStarHollow = true`, with KDocs in `BoardMarkRecipe.kt` and `StarGlyph.kt`. D-1 item 3's pin flips in `BoardMarkRecipeTest` and `StarGlyphTest`. New: `StarPixels.kt` (test helper), `ResultsStarPaletteTest`, `CatalogStarPipPaletteTest` |
| 2fd0baf | feat(colour-vision): give the standard empty star the deeper grey | Q-027: `boardMarksFor(Standard, …).unearnedStarOutline = onSurfaceVariant`, read from one local that both palettes share; `BoardMarksTest`'s two standard pins; `ResultsScreen.kt`'s KDoc (it called the unearned stars `outlineVariant`) |
| 55b9a99 | test(colour-vision): pin the tutorial's done pip and star palette | F-2, tutorial half: `TutorialPipPaletteTest` |
| 5c68458 | test(colour-vision): give the light star floors their true reason | D-6: the rename and both KDocs in `ColorblindPaletteContrastTest`; no assertion changed |
| 3187f42 | feat(colour-vision): preview the star glyph in both palettes | F-1: four previews in `StarGlyph.kt` |
| 0f1f58f | test(colour-vision): say what the board-pair test proves | F-4, optional: a KDoc in `BoardMarkRecipeTest`, each claim checked by mutant |

Files (paths under `app/src/`):
- `main/java/com/app/pegsolitaire/ui/game/BoardMarkRecipe.kt`: the standard recipe's hollow unearned star; its KDocs.
- `main/java/com/app/pegsolitaire/ui/theme/BoardMarks.kt`: Q-027's one value; its KDocs. This is the round's only start-path file.
- `main/java/com/app/pegsolitaire/ui/components/StarGlyph.kt`: KDocs; the light and dark previews (Standard and Colorblind, earned and unearned, at 48dp and 16dp, sizes from `dimens.xml`).
- `main/java/com/app/pegsolitaire/ui/results/ResultsScreen.kt`: KDoc only.
- New tests: `test/.../ui/results/ResultsStarPaletteTest.kt`, `test/.../ui/classic/CatalogStarPipPaletteTest.kt`, `test/.../ui/onboarding/TutorialPipPaletteTest.kt`, and the helper `test/.../ui/components/StarPixels.kt` (star sampling points; a between-the-stops colour check).
- Edited tests: listed below for approval.
- `/Users/ermanergoz/Desktop/PegSolitarie-cv/.planning/reports/colour-vision/android-game-dev-loop1.md`: this report, committed alone after the code.

How the new tests work: one screen is rendered under `Standard`, then flipped to `Colorblind` through `AppTheme(boardMarkPalette = state)` with no new `setContent`, and both frames are read with `renderedWindow()` under `@GraphicsMode(NATIVE)`, animations off, `w400dp-h800dp-xxhdpi`. No golden is stored.
- **Positions.** Each star's box comes from its row's semantics bounds and the dimens read in composition. A self-check fails loudly if the row's measured width stops matching the layout.
- **Colours.** Expected colours come from `boardMarksFor(…)`, which `BoardMarksTest` pins to literal hexes, and the surfaces from the colour schemes. This is the `StarGlyphTest` and `BoardCanvasPaletteTest` idiom.
- **What they assert,** in each palette and theme:
  - Results: an unearned star's centre is `background`; an earned star's centre lies between its fill's two stops; the colour-blind earned star alone carries its outline; the unearned band is the palette's unearned colour; `EarnedStarCountKey` is 2 before and after the flip.
  - Catalogue card: an unearned pip's centre is `surfaceContainerHigh`, the card; an earned pip's centre lies between its fill's stops; the row's description is the same before and after the flip.
  - Tutorial, step 2: the done pip's centre lies between the palette's `donePipFill` stops; "Step 2 of 3" is the same before and after the flip.
  - Tutorial, completed: the completion star is the one region below the board the palette changes, and it must be star-sized. Its centre is gold under Standard and amber under Colorblind.

## Red first (each red quoted)
Each red comes from the JUnit XML. The runs were `./gradlew :app:testDebugUnitTest --tests <classes> --max-workers=4`.
- **RED-1.** The tests were written and the pins flipped, with the code still at 81525cd. Classes: `ResultsStarPaletteTest`, `CatalogStarPipPaletteTest`, `TutorialPipPaletteTest`, `BoardMarkRecipeTest`, `StarGlyphTest`. EXIT=1; 16 tests, 7 failed:
  - `ResultsStarPaletteTest` light and dark: "Standard: the unearned star's centre shows the background beneath it / expected to be true"
  - `CatalogStarPipPaletteTest` light and dark: "Standard: the unearned pip's centre shows the card beneath it / expected to be true"
  - `StarGlyphTest.theStandardEarnedStarIsTodaysPixelsAndItsUnearnedStarIsHollow`: "hollow: the background shows at the unearned star's centre / expected to be true"
  - `BoardMarkRecipeTest.earnedAndUnearnedStarsDifferInAChannelThatIsNotHueInBothPalettes`: "Standard: the unearned star / expected to be false"
  - `BoardMarkRecipeTest.theStandardRecipeIsTodaysBoardMetrics`: "value of: getUnearnedStarHollow() / expected to be true"
  - The colour-blind halves and the key and description checks passed before the standard check was reached. `TutorialPipPaletteTest` was 3/3 green, because that drawing already existed.
- **GREEN-1.** After `unearnedStarHollow = true`: EXIT=0. The run covered the five classes above plus `BoardMarksTest`, `ColorblindPaletteContrastTest`, `ResultsScreenTest`, `HomePaletteIdentityTest`, `BoardCanvasPaletteTest` and `ColorblindPaletteLiveSwitchTest`: 11 suites, 38 tests, 0 failed.
- **RUN-B, the auditor's exact pair.** M16 (the done pip back to `goldGradientFill()`) and M17 (Results' unearned star back to `Icons.Outlined.Star` in `outlineVariant`). A script applied them, asserting each target occurs exactly once. Over the three new classes: EXIT=1; 7 tests, 3 failed:
  - `ResultsStarPaletteTest` light and dark: "Colorblind: the unearned star's centre shows the background beneath it / expected to be true"
  - `TutorialPipPaletteTest.theDonePipDrawsTheActivePaletteInTheLightTheme`: "Colorblind: the done pip's centre shows the palette's done-pip fill / expected to be true"
  - The dark done pip stays green: §3.4's dark column keeps `GoldGradient` in both palettes, so M16 cannot show in dark.
- **RUN-A, every consumer at the base.** `ResultsScreen.kt`, `BoardPreview.kt` and `TutorialScreen.kt` were replaced by `git show 69e1cdd:…`. EXIT=1; 7 tests, 6 failed:
  - Results light: "Colorblind: earned star 0's centre shows its fill / expected to be true". Results dark: "Colorblind: earned star 0 is outlined only in the colour-blind palette / expected: true"
  - Catalogue light: "Colorblind: earned pip 0's centre shows its fill". Catalogue dark: "Colorblind: the unearned pip's centre shows the card beneath it"
  - Tutorial, done pip in light: "Colorblind: the done pip's centre shows the palette's done-pip fill". Completion star: "pixels below the board that the palette changed (the completion star) / expected not to be: null"
  - Both runs were restored with `git checkout --` on the three files, and `git status --short` confirmed it.
- **RED-2.** `BoardMarksTest`'s standard pins moved to `onSurfaceVariant`, with `BoardMarks.kt` unchanged. EXIT=1; 4 tests, 2 failed:
  - light: "expected: …StarOutline=Color(0.3882353, 0.37254903, 0.4117647, 1.0, … but was : …StarOutline=Color(0.8627451, 0.827451, 0.9254902, 1.0, …", that is `#635F69` expected and `#DCD3EC` found.
  - dark: "expected: …StarOutline=Color(0.654902, 0.62352943, 0.70980394, … but was : …StarOutline=Color(0.22745098, 0.2, 0.3137255, …", that is `#A79FB5` expected and `#3A3350` found.
- **GREEN-2.** After the Q-027 value: EXIT=0. `BoardMarksTest`, `StarGlyphTest`, the three new classes, `ColorblindPaletteContrastTest`, `AppThemeRootTest`, `BoardMarkRecipeTest` and `HomePaletteIdentityTest`: 9 suites, 28 tests, 0 failed. `:app:ktlintCheck` and `:app:detekt` were green in the same run.
- **F-4's claims,** one mutant at a time on `BoardMarkRecipe.kt`, each run over `BoardMarkRecipeTest`:
  - K1 (the standard hint source ring at `SELECTED_RING_RATIO`) fails the pair test: "Standard: SelectionRing against HintSourceRing / expected not to be: MarkLook(form=Ring, radiusRatio=0.4, dashes=0, halo=false, filled=false)"
  - K2 (`look(WrongTapMark)` with `SOLID_RING`) fails it: "Standard: SelectionRing against WrongTapMark / expected not to be: MarkLook(…radiusRatio=0.4, dashes=0…)"
  - M20 (colour-blind `hintRingDashes = SOLID_RING`) leaves the pair test green and fails `theColourBlindRecipeIsSection34`: "value of: getHintRingDashes() / expected: 8 / but was : 0"
  - The file was restored after each mutant.
- **No red is possible** for D-6, a rename and two KDocs with no behaviour: `ColorblindPaletteContrastTest` was 3/3 green before and after. Nor for F-1's previews: they compiled with main warnings as errors, and ktlint, detekt, lint and `StarGlyphTest` stayed green.

## Existing-test edits, for the code-auditor's approval
1. `ui/game/BoardMarkRecipeTest.kt`, `theStandardRecipeIsTodaysBoardMetrics`: the `unearnedStarHollow` line flips to `isTrue()` and its comment is replaced. This is D-1 item 3, verbatim in substance.
2. `ui/game/BoardMarkRecipeTest.kt`, the star-pair test (D-1 item 3): it now loops over `BoardMarkPalette.entries` with `assertWithMessage("$palette: …")`. With the loop, it is renamed from `earnedAndUnearnedStarsDifferInAChannelThatIsNotHueInTheColourBlindPalette` to `earnedAndUnearnedStarsDifferInAChannelThatIsNotHueInBothPalettes`, and its KDoc now cites CV-AC08 as amended. The old name and KDoc said "colour-blind only" and called the colour-only standard pair a finding for the ux-designer. Both are false now.
3. `ui/game/BoardMarkRecipeTest.kt`, `everyPairOfMarksThatCanShareABoardDiffersInAChannelThatIsNotHue`: KDoc only (F-4, optional). It no longer claims to catch M20; it names the tests that do, and two mutants it does catch, each checked above.
4. `ui/components/StarGlyphTest.kt`, the standard test (D-1 item 3):
   - It keeps its earned comparison against today's gold star, pixel for pixel.
   - Its unearned half now reads the band (12, 4.5) in the standard unearned colour and the hollow centre (12, 12) as `background`, as the colour-blind case does.
   - It is renamed from `theStandardStarsAreTodaysPixels` to `theStandardEarnedStarIsTodaysPixelsAndItsUnearnedStarIsHollow`, and the class KDoc is updated to match.
   - The `TODAY_UNEARNED` fixture is removed: the filled `Icons.Outlined.Star` in `show()`, its tag constant and its import. Nothing compares against it any more.
5. `ui/theme/BoardMarksTest.kt` (D-1 item 2's "its pin", Q-027): both standard pins' `unearnedStarOutline` changes, `0xFFDCD3EC` to `0xFF635F69` in light and `0xFF3A3350` to `0xFFA79FB5` in dark. The light test's KDoc names Q-027.
6. `ui/theme/ColorblindPaletteContrastTest.kt` (D-6): `LIGHT_FLOORS_SET_BY_THE_RESULTS_SKY` is renamed `LIGHT_STAR_FLOORS`. Its KDoc and the second KDoc paragraph of `theComputedRatiosMatchTheSpecsTableWithinFiveHundredths` are the ruling's text, verbatim; I checked them mechanically after removing line breaks. No assertion changed.

No other existing test file changed: `git diff --stat 81525cd..HEAD -- app/src/test` lists these four and the four new files.

## Checks
- `./scripts/check.sh --max-workers=4` at 0f1f58f, 21:35:03–21:46:13 (670 s): **pass**.
  - It printed "BUILD SUCCESSFUL in 11m 9s", "104 actionable tasks: 21 executed, 3 from cache, 80 up-to-date", `CHECK-EXIT=0`.
  - Executed: `:app:testDebugUnitTest`, `:app:lintDebug` and its models, `:domain:depthSweep`, `:domain:koverVerify`, and the literal-check tasks. ktlintCheck and detekt were up-to-date from green runs on the same sources minutes before.
  - From the JUnit XML: `:app` 130 suites, 1,265 tests, 0 failures, 0 errors, 0 skipped. At the reviewed head it was 127 and 1,258; this round adds 3 suites and 7 tests. `:domain`: 33 suites and 294 tests, plus `depthSweep` 1 and `fuzz` 1, so 296 tests, 0 failures.
  - Main-source `w:`: 0. Test-source `w:` in this round's compiles: 3 new, each the deprecated v1 `createComposeRule` the suite uses throughout (test compilation is exempt from warnings-as-errors).
- Targeted runs, each listed under "Red first": pass or fail exactly as quoted.
- §11 tripwire, on `d21ad07..HEAD` at 0f1f58f. `git merge-base develop HEAD` is `d21ad07`; develop is at `053e9cc`. The report commit adds only `.planning/`.
  - `git diff --name-only d21ad07..HEAD | grep -E '^app/src/main/java/com/app/pegsolitaire/(MainActivity\.kt|ui/theme/)'` printed the following, exit 0:
    ```
    app/src/main/java/com/app/pegsolitaire/MainActivity.kt
    app/src/main/java/com/app/pegsolitaire/ui/theme/AppThemeRoot.kt
    app/src/main/java/com/app/pegsolitaire/ui/theme/BoardMarkPaletteState.kt
    app/src/main/java/com/app/pegsolitaire/ui/theme/BoardMarks.kt
    app/src/main/java/com/app/pegsolitaire/ui/theme/Clay.kt
    app/src/main/java/com/app/pegsolitaire/ui/theme/Color.kt
    app/src/main/java/com/app/pegsolitaire/ui/theme/Theme.kt
    ```
  - `git diff --name-only d21ad07..HEAD | grep -E '^app/src/main/(java/com/app/pegsolitaire/(App\.kt|navigation|ui/home|di)/|AndroidManifest\.xml|res/(font|values[^/]*/themes\.xml))'` printed nothing, exit 1.
  - Same seven files as at 32b2600. The round's one start-path edit, `BoardMarks.kt`, lands before the sitting (D-1 timing). **The 15e sitting stays owed.**
- CV-AC04 greps over `81525cd..HEAD`. All pass:
  - hex literals added in main outside `Color.kt`: none;
  - `[0-9.]+\.(dp|sp)` added in main Kotlin: none;
  - `strings.xml` and `dimens.xml`: untouched.
- Suppressions over `81525cd..HEAD`, main and test: none. The grep was `^\+.*(@Suppress|@SuppressLint|@Ignore|@OptIn|@file:OptIn|!!|ktlint-disable|detekt:disable|baseline)`. Build files, `gradle/`, `build-logic/`, the manifest and `THIRD_PARTY.md`: untouched. No dependency was added.
- BD-01's third grep over `81525cd..HEAD -- app/src` prints two lines. Both are D-6's verbatim test KDoc text naming "candy-backdrop B-23": test-only and not user-facing, the kind of line F-5 already accepted. F-5's own line is gone with the old paragraph.
- Inputs read in full:
  - `code-auditor.md`, `tech-lead-deviation-ruling.md`, `ux-designer-star-ruling.md`, the first-round `android-game-dev.md`;
  - `tech-lead-brief.md` (525 lines, three reads) and `docs/ux/colour-vision.md` (966 lines, four reads);
  - Q-027 in `FOUNDER.md` on `develop` ("Answer: 2 — stars only …"); the worktree's `FOUNDER.md` has no Q-027 entry yet.

## Open issues / risks
1. **The spec's text still says `outlineVariant` for the standard unearned star,** and Q-027 changed it. The places: CV-13a ("the unearned star keeps `outlineVariant`"), §3.4's Standard unearned row, §12.2 ("The unearned star keeps `outlineVariant`"), §12.3's colour line and §12.6 ("Until the founder answers"). Recording the answer is the ux-designer's (non-blocking). The code follows Q-027 as the dispatch rules.
2. **F-3 is not taken (optional).** Making `BoardLook` read `marks.selectionRing` and `marks.legalTarget` means splitting `BoardPalette.accent` into two colours. That edits `drawCell` and `drawSelectedPeg` in `BoardCanvas.kt`, the file talkback-board also merges into. The other fix, dropping the two fields, edits the contrast test and the `BoardMarksTest` pins. The two tokens are still the same `primary`, and `BoardCanvasPaletteTest`'s `isColour(ring, marks.selectionRing)` still ties them.
3. **The completion-star test finds the star as the only region below the board that the palette changes** (§3.3's consumer list). A future palette consumer below the board would trip its size check (≤ the star's edge) loudly rather than pass silently.
4. **For QA (D-1 item 4, the spec's §12.5):**
   - CV-AC11's graded item: in the standard palette at normal vision, unearned stars are hollow on Results and on a catalogue card, in light and dark.
   - CV-AC12's 16dp note.
   - The off-palette comparison against `develop` excepts the unearned stars, whose form (CV-13a) and colour (Q-027) both changed.
5. **The review model.** The first review ran on Opus (Q-019 option 3); the auditor's note on rule 1's superior-model review still applies to this diff.
6. **Merge note.** `TutorialPipPaletteTest` and `ResultsStarPaletteTest` render the whole screen. Talkback-board's merge changes those screens' semantics, not their pixels, so the two should hold. Re-run them in the second merger's §10.3 list, beside `StarGlyphTest`.

## Handoff
Next: code-auditor. Re-review the fix diff `81525cd..0f1f58f`:
- F-1: `StarGlyph.kt`'s previews;
- F-2: the three new test classes, the helper, and the reds above (RUN-B is your M16 + M17);
- CV-AC05 and CV-AC08 as amended;
- the six existing-test edits above, for approval.

Next: qa-engineer. CV-AC11's graded star item and CV-AC12's 16dp note (open issue 4), on the Pixel_9a or the Pixel_9, never the bench. Then the 15e sitting last, on `Pixel_9a_bench` (brief §9): after the auditor's PASS, QA's device pass and the `develop` merge-in of that day.

Next: producer. Route open issue 1, the spec's record of Q-027, to the ux-designer, non-blocking; then the tech-lead's sign-off cites this report beside the auditor's, QA's and the sitting's.
