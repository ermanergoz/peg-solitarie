# android-game-dev report — candy-backdrop: the develop merge-in, and the code-auditor's G1
Date: 2026-11-01
Branch: feature/candy-backdrop (8fac490..7ce3314, on 23bf220; 8fac490 is the `--no-ff` merge of `develop` at f1c399a)
Skills loaded: ergoz-soft:standards (role android-game-dev). The worktree snapshot is v0.6.2 and the plugin is v1.0.0, so I read these plugin rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing. I also read the product's `.claude/rules/lessons.md`. Other skills: superpowers:test-driven-development, gitflow, compose-skill, compose-ui, android-testing
Ran in parallel with: none named by the dispatch (other worktrees on the host were live; `feature/licences-screen` landed on `develop` as bc95b2a while this ran, and was not merged in, as the producer directed)
Ran on: claude-opus-5-5 at effort max (the agent file's pin; the harness names no effort)
Fan-out: none
Loops: 2
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **Merged `develop` at f1c399a** as 8fac490 (`--no-ff`): bugfix/home-chip-arrival (305f741), feature/colour-vision (1bc442f) and develop's STATUS update. bc95b2a (licences) was **not** merged. Seven files were hand-resolved; every hunk is listed below.
- **The merged 15m oracle:** develop's M3d text check runs ahead of the pixel read, and develop's full-pill glyph read now reads frame 2 (M1). The stop-rule numbers are 0 and 1. Every red was re-shown on 8fac490, in light and dark.
- **G1 is fixed** in 7ce3314. colour-vision's §10.3 duty (BD-17 once per palette) is 9d6bbeb, a test with its guard watched red.
- **The Check is red on 2 of 1,388 app tests:** colour-vision's `ResultsStarPaletteTest`, in both themes. This is a spec conflict with BD-14's transparent Results `Scaffold`. **I did not edit the test** (Open issue 1). Every other gate is green.
- **`HomeChipArrivalTest` stays green unedited,** but one of its tests no longer discriminates on this branch (Open issue 2). **`GameStatusStripTest` is green and still bites.**

## Outputs
Commits (the trailer is `Co-Authored-By: Claude Fable 5.1`, per plugin rules/gitflow.md v1.0.0; see Open issue 5):

| Commit | Kind | What |
|---|---|---|
| 8fac490 | merge | `develop` f1c399a in, with the resolutions below |
| 9d6bbeb | test | BD-17's slab read runs once per board-mark palette (colour-vision brief §10.3, spec §3.7) |
| 7ce3314 | fix | G1: the four gameplay previews go through `BackdropPreviewHost` |

Files I wrote or resolved (paths under `app/src/`):
- `main/java/com/app/pegsolitaire/ui/theme/Theme.kt`, `ui/game/GameScreen.kt`, `ui/game/GameStatusStrip.kt`, `ui/home/HomeStage.kt`, `ui/home/HomeScreen.kt`: hand-resolved merge hunks.
- `test/java/com/app/pegsolitaire/ui/home/HomeLayoutTest.kt`: the merged 15m oracle (M1).
- `test/java/com/app/pegsolitaire/ui/backdrop/TransparentContainersTest.kt`: a semantic merge fix. It passes develop's new `onToggleColorblindPalette` callback; without it the test sources did not compile.
- `test/java/com/app/pegsolitaire/ui/backdrop/BackdropPixelTests.kt`: `BoardInteriorIdentityTest` per palette.
- `main/java/com/app/pegsolitaire/ui/game/GameScreenPreviews.kt`: G1.
- `docs/ux/colour-vision.md`: resolved to `develop`'s copy, unchanged.

### The hand-resolved hunks (8fac490)
1. **`Theme.kt`:** one `CompositionLocalProvider` holds `LocalClayColors`, this branch's `LocalBackdropColors` and `LocalFinishColors`, and develop's `LocalBoardMarks`. Develop's `remember(boardMarkPalette, darkTheme)` is kept. Its diff against `develop` is exactly the branch's two provider lines.
2. **`GameScreen.kt`:** imports only. The slab keeps `finishGlow` and `glossFinish(BoardSlab)` and gains develop's `BoardMarkPaletteKey`. The `clayShadow` import is dropped because nothing in the file uses it. Its diff against `develop` equals the branch's delta from before the merge.
3. **`GameStatusStrip.kt`:** develop's `elapsed_time_spoken` description, `clearAndSetSemantics` and `spokenDuration`/`minutesSaid` sit beside the timer pill's `val pill` and its finish. Both KDocs are kept: develop's for `spokenDuration` and the branch's BD-25 preview KDoc. Its diff against `develop` equals the branch's delta.
4. **`HomeStage.kt`:** develop's fade layer (`.then(graphicsLayer { alpha … })` while a `StageArtFade` runs) stays first. Then `finishGlow` replaces develop's `clayShadow`, then `.background(…)`, then `glossFinish(StageArt)`. Develop moved `clayEntrance` to the stage itself, and that is kept. The art's finish therefore fades with it, exactly as its shadow did on `develop`.
5. **`HomeScreen.kt`:**
   - The class KDoc takes develop's "one fifth of that slack" and the branch's transparent-`Scaffold` sentence. The blobs are gone.
   - Develop's `FIRST_CHIP_ENTRANCE_ORDER` and `LATE_CHIP_ENTRANCE_ORDER` sit above the branch's BD-25 previews.
   - The auto-merged body keeps the transparent `Scaffold`, `HomeBlobs()` removed, the gel tiles, and the Continue chips' finish.
6. **`HomeLayoutTest.kt`:** the merged oracle, described in the next section.
7. **`TransparentContainersTest.kt`:** one added argument, `onToggleColorblindPalette = {}`.
8. **`docs/ux/colour-vision.md`** (add/add): the branch's copy is byte-identical to develop's 8f67d74, and develop's copy descends from it, so develop's copy is taken.

These files auto-merged and carry exactly the branch's delta over `develop`: `TutorialScreen.kt`, `ResultsScreen.kt`, `SettingsScreen.kt`, `Clay.kt` (develop's `gradientFill` extraction beside the branch's `titleGradientBrush`), `Color.kt`, `dimens.xml` and `HomeProgressRibbon.kt` (the pills' finish over develop's three `RibbonShape`s). The check: for each file, the sorted `+`/`-` lines of `git diff f918793 23bf220` equal those of `git diff develop 7ce3314`.

The `clayShadow` call sites on the merged tree are the same files and counts as on 23bf220.

### The merged 15m oracle (M1), at 8fac490
- **`assertEachPillPaintsOnlyItsSurfaceAndItsTexts(darkTheme)`**, in this order:
  - **G1's pill count:** `ribbonPills()`, now under the message "G1: the ribbon's pills".
  - **Develop's M3d:** each pill's `textsWithin()` is exactly `[label, value]`, in order.
  - **`inkBesideTheTextsOfEachPill(darkTheme)`**, which holds G1's whole-pixel edges, frame 1, frame 2, G2, G3 and G4.
  - **G5:** all three pills are collected before anything is asserted.
- **`inkBesideTheTextsOfEachPill(darkTheme)`** keeps develop's name. It now returns, for each pill, the pixels where frame 1 differs from frame 2's reference finish by more than `PAINT_TOLERANCE` (1). It no longer reads the flat `LightColors.surfaceContainerHigh`.
- **Develop's `onAWidePhoneEveryFullPillPaintsItsGlyphBesideItsTexts`** now renders under the reference finishes and reads that helper. Two changes:
  - It collects every pill before it asserts, so a red names all three.
  - It has a dark sibling, `onAWidePhoneEveryFullPillPaintsItsGlyphInTheDarkThemeToo`, so M5c can be shown in both themes, as the auditor's loop 0 asked.
- **What is unchanged:**
  - `GLYPH_INK_FLOOR_PX` stays 20 and `PAINT_TOLERANCE` stays 1.
  - The guards G1–G5 have the same bodies, messages and constants as e07b105, and `ReferenceFinishes` is unchanged.
  - `HomeLayoutTest` went from 17 tests to 19: develop's one plus the dark sibling.
- **The stop rule on 8fac490.** I added a temporary `println` probe (uncommitted, then reverted) after frame 2:

| Window | Theme | Pill 0 | Pill 1 | Pill 2 |
|---|---|---|---|---|
| `w360dp-h640dp`, compact pills (rect 40 px tall) | light | maxDiff 0, G3 spread 1, ink 0 | 0 / 1 / 0 | 0 / 1 / 0 |
| same | dark | 0 / 1 / 0 | 0 / 1 / 0 | 0 / 1 / 0 |
| `w412dp-h915dp`, full pills (rect 84 px tall) | light | ink 131 px (G3 spread 1) | 196 px | 85 px |
| same | dark | 131 px | 196 px | 85 px |

  The full pills' glyph ink under the frame-2 read equals the H-1 probe's 131 / 196 / 85 px, which the KDoc already cites. The flat read had counted 1,062–1,278 px with the glyph deleted (the auditor's M1 probe). The frame-2 read counts **0 px** with it deleted (M5c below).

## Reds on the merged oracle (8fac490)
Each red is a temporary, uncommitted mutation. Each was run as `./gradlew --max-workers=4 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.home.HomeLayoutTest'`, then reverted with `git checkout -- app`, and `git status --porcelain` was empty after every one.
- **Unmutated:** 19 of 19 pass.
- **The compact mutations (M3b, M3d, 4, 4w):** in each, the two full-pill tests stay green.
- **The full-pill mutations (M5c a and b):** in each, the two compact tests stay green.

The mutations, as diffs against 8fac490. The paths are `app/src/main/java/com/app/pegsolitaire/ui/home/HomeProgressRibbon.kt` and `HomeBandSet.kt`, and `app/src/test/java/com/app/pegsolitaire/ui/home/HomeLayoutTest.kt`.

Develop's 05d5bd7 made `CompactPillContent` a custom `Layout` with exactly two measurables. So M3b, 4 and 4w are now a third measurable that is placed inside that layout, not an `Icon` before `PillLabel` in a `Row`:
```diff
# M3b (HomeProgressRibbon.kt)
-            RibbonShape.Compact -> CompactPillContent(label = label, value = value)
+            RibbonShape.Compact -> CompactPillContent(label = label, value = value, glyph = glyph, gilded = gilded)
+    glyph: ImageVector,
+    gilded: Boolean,                                             // CompactPillContent's parameters
+            Icon(glyph, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.then(if (gilded) Modifier.goldGradientFill() else Modifier).size(dimensionResource(R.dimen.home_stat_pill_glyph_size)))   // third child, after PillValue
-        val (labelPlaceable, valuePlaceable) = measurables.map { it.measure(Constraints()) }
+        val (labelPlaceable, valuePlaceable, glyphPlaceable) = measurables.map { it.measure(Constraints()) }
+                glyphPlaceable.placeRelative(0, Alignment.CenterVertically.align(glyphPlaceable.height, height))  // first line of the side-by-side layout { }
# 4: as M3b with .size(10.dp) (plus `import androidx.compose.ui.unit.dp`) and placeRelative(0, 0)
# 4w: as 4 with tint = androidx.compose.ui.graphics.Color.White
# M3d (HomeProgressRibbon.kt): a glyph drawn as text, first in the layout
+            Text("★")                                       // before PillLabel(label)
-        val (labelPlaceable, valuePlaceable) = measurables.map { it.measure(Constraints()) }
+        val (starPlaceable, labelPlaceable, valuePlaceable) = measurables.map { it.measure(Constraints()) }
+                starPlaceable.placeRelative(0, 0)
# M5c (a), the dispatch's form (HomeProgressRibbon.kt, FullPillContent): delete the Icon(…) block before PillLabel(label)
# M5c (b), develop's form (HomeBandSet.kt)
-        !bandSet.compactPills -> RibbonShape.Full
+        !bandSet.compactPills -> RibbonShape.Compact
# G-red (HomeLayoutTest.kt, ReferenceFinishes)
-    references.forEachIndexed { index, rect ->
+    references.take(0).forEachIndexed { index, rect ->
# draw-less G-red (HomeLayoutTest.kt, ReferenceFinishes)
-                    }.finishGlow(glow, shape)
-                    .glossFinish(FinishedContainer.RibbonPill, shape, fill = fill)
-                    .testTag(REFERENCE_FINISH_TAG_PREFIX + index),
+                    }.testTag(REFERENCE_FINISH_TAG_PREFIX + index),
```

| Red | Light | Dark |
|---|---|---|
| **M5c (a), full pill's glyph deleted** | `onAWidePhoneEveryFullPillPaintsItsGlyphBesideItsTexts`: "pills painting under 20 px besides their finished surface and their texts: pill 0 0 px, none; pill 1 0 px, none; pill 2 0 px, none / expected to be empty / but was: [0, 1, 2]" | `…InTheDarkThemeToo`: the same message, `[0, 1, 2]` |
| **M5c (b), compact shape on every window** | the same message, `[0, 1, 2]` | the same message, `[0, 1, 2]` |
| **M3b** | `theNarrowClassPill…ComposesNoGlyph`: "G5: … pill 0 131 px, x 17..34, y 151..166 (px), rows 12..27 below its top; pill 1 84 px, x 136..141, y 151..166 (px), rows 12..27 below its top; pill 2 85 px, x 255..266, y 151..167 (px), rows 12..28 below its top / keys with wrong values / for key : 0 / expected value: 0". G1–G4 passed | `…InTheDarkThemeEither`: the same counts (131 / 84 / 85) and spans |
| **M3d** | "pill 0: the texts it lays out, in order / unexpected (1): ★ / --- / expected : [Stars, 7]". This fails ahead of every pixel guard | the same |
| **4, glyph inside the gloss band** | "G5: … pill 0 44 px, x 16..25, y 148..155 (px), rows 9..16 below its top; pill 1 57 px, x 135..142, …, rows 9..16 below its top; pill 2 25 px, x 253..258, …, rows 9..16 below its top". The band is `[8, 18.4)` px of a 40 px pill | the same: 44 / 57 / 25, rows 9..16 |
| **4w, white glyph in the band** | "G5: … pill 0 44 px, …, rows 9..16; pill 1 56 px, …; pill 2 24 px, …" | 44 / 57 / 25, rows 9..16 |
| **G-red, `references.take(0)`** | all four oracle tests fail with "G2: frame 2 composes reference 0 once / value of: iterable.size() / expected: 1 / but was: 0", at G2 as the auditor's loop-0 ruling places it | the same, both dark tests |
| **G-red, draw-less reference** | G3: "frame 2 must show pill 0's finish alone, flat along every row, but row y 154 (px) spreads 193 levels in one channel …" (compact); "row y 310 (px) spreads 206 levels" (full) | G3: "row y 156 … spreads 194 levels" (compact); "row y 310 … spreads 203 levels" (full) |

## M3: develop's other pixel reads under this branch
- **`GameStatusStripTest`: unedited, 10 of 10, and it still bites.** `dockSlabPaintBottomDp()` walks down while each pixel equals the slab colour. The dock slab's finish adds a rim and a lip but no gloss. Below the slab the transparent screen leaves nothing behind, so the walk stops at the slab's edge. That keeps 576 dp apart from the 528 dp a doubled inset would give.
- **`HomeChipArrivalTest`: unedited, 13 of 13.**
  - **The stage-art reads still bite.** The settled art keeps exact `surfaceContainerHigh` pixels inside its rim and lip, and a half-faded art over the bare window has none.
  - **`aChipThatArrivesAfterFirstPaintFadesInWithItsRoomWithNoStaggerDelay` no longer discriminates** (Open issue 2). `inkBetween` counts the pixels that are not exactly `LightColors.background`. On this branch `HomeScreen` is transparent and `renderHome` draws no host `Surface`, so the bare window behind it reads `#00000000`.
    - The probe (uncommitted, reverted) reads **6,080 px of "ink" between the ribbon and the first tile with no chip at all**: the whole 16 dp × 380 px gap.
    - Under the home-chip auditor's P9, where the late chip takes the cascade's stagger (`entranceOrder = FIRST_CHIP_ENTRANCE_ORDER` in `HomeScreen.kt`), every frame reads the same counts: 6080, 6080, 14060, 19760 and so on. So **P9 survives**, and the test passes.
- **`HomeRibbonShapeTest` (15w): 12 of 12.** It has no pixel read, so `RibbonShape.Stacked`'s gloss cannot reach it.

## colour-vision and home-chip-arrival duties on this merge
- **colour-vision brief §10.3, "Candy-backdrop after this branch":**
  - Both providers are in one `CompositionLocalProvider`. Both `Clay.kt` hunks and both `dimens.xml` blocks are kept.
  - `BoardInteriorIdentityTest` now runs once per palette (9d6bbeb): Standard and Colorblind, each in light and dark. A guard reads `BoardMarkPaletteKey` on the board-state node, so a pass that did not switch palette fails. **Red:** with `boardMarkPalette = palette` removed from the test's `AppTheme`, it fails with "the board draws its marks in the palette under test, palette = Colorblind, dark = false / expected: Colorblind / but was : Standard". **Green:** 2 of 2.
  - `TransparentContainersTest` 2 of 2 and `HomePaletteIdentityTest` 2 of 2.
- **colour-vision sign-off's named tests,** from this Check's XML (tests/failures): `BoardCanvasFirstFrameTest` 4/0, `BoardCanvasPaletteTest` 3/0, `BoardMarkMotionTest` 2/0, `ColorblindPaletteLiveSwitchTest` 1/0, `HomePaletteIdentityTest` 2/0, `HomeChipArrivalTest` 13/0, `BoardCanvasWrongTapReducedMotionTest` 6/0, `BoardCanvasWrongTapLiveMotionTest` 2/0, `BoardCanvasTest` 17/0, `StarGlyphTest` 4/0, **`ResultsStarPaletteTest` 2/2**, `CatalogStarPipPaletteTest` 2/0, `TutorialPipPaletteTest` 3/0, `SettingsScreenTest` 6/0, `AppThemeRootTest` 3/0, `ColorblindPaletteContrastTest` 3/0, `GameScreenCueRotationTest` 4/0, `GameScreenSoundTest` 13/0, `GameFlowTest` 8/0, `GameScreenHintNoticeTest` 20/0, `AnimationsEnabledGateTest` 2/0, `AnimationsEnabledGateWithoutScaleTest` 1/0, `ResultsStarsLiveMotionTest` 1/0, `ResultsCelebrationSoundTest` 7/0, `TutorialHapticsTest` 4/0, `TutorialSoundTest` 5/0, `TutorialCompletionStarLiveMotionTest` 1/0.
- **home-chip-arrival C11 ruling, Open issue 2:** `git grep -n 'ANIMATOR_DURATION_SCALE' HEAD -- app/src/test` prints only `SystemMotionScale.kt:16`, a KDoc line, and develop's own `AnimationsEnabledGateWithoutScaleTest.kt:29`. Every test this branch added drives reduced motion through `SystemMotionScale`.

## G1 (7ce3314)
`GameScreenPreview` and `GameScreenUndecidedNoticePreview` each keep their light and dark `@Preview`s on the stated 360×640 dp baseline window. Each now renders `AppTheme { BackdropPreviewHost(BackdropDensity.Quiet) { GameScreen(…) } }`, the same form F2 gave `GameStatusStrip`. The file's KDoc says why. Every screen-level preview now goes through the host: `HomeScreen.kt` 3, `HomeHero.kt` 1, `GameStatusStrip.kt` 1, `TutorialScreen.kt` 2 and `GameScreenPreviews.kt` 2. The remaining previews without the host are components: the board, stars, dock controls, the notice, `TimerDigits`, `GelFootprintArt` and `HomeStage`. A preview has no test, so G1 is Review-only (BD-25).

## Checks
- `uptime` before every Gradle run. A script waited for a 1-minute load below 12; the first wait started at 24.4. The Checks started at 4.22 and 6.65.
- `git merge --no-ff develop` (develop f1c399a) — 7 conflicts (`GameScreen.kt`, `GameStatusStrip.kt`, `HomeScreen.kt`, `HomeStage.kt`, `Theme.kt`, `HomeLayoutTest.kt`, `docs/ux/colour-vision.md`), resolved as above.
- `./gradlew --max-workers=4 :app:compileDebugUnitTestKotlin` — first **fail** (`TransparentContainersTest.kt:165:25 No value passed for parameter 'onToggleColorblindPalette'`), then pass after hunk 7.
- `./gradlew --max-workers=4 :app:testDebugUnitTest --tests '…HomeLayoutTest' --tests '…HomeChipArrivalTest' --tests '…GameStatusStripTest' --tests '…HomeRibbonShapeTest' --tests '…HomePaletteIdentityTest' --tests '…TransparentContainersTest' --tests '…BoardInteriorIdentityTest' --tests '…HomeStageTest'` — pass. The counts are 19, 13, 10, 12, 2, 2, 2 and 10, with 0 failures.
- `./gradlew --max-workers=4 --continue :app:ktlintCheck :app:detekt` on the resolved tree — pass.
- The reds above — each **fails, as intended**. Unmutated, it passes 19 of 19.
- `./gradlew --max-workers=4 :app:testDebugUnitTest --tests '…BoardInteriorIdentityTest'` — palette guard red as quoted, then pass, 2 of 2.
- `./gradlew --max-workers=4 --continue :app:compileDebugKotlin :app:ktlintMainSourceSetCheck :app:detekt` after G1 — pass.
- **`./scripts/check.sh --wait --max-workers=4` on 7ce3314 — FAIL.** "BUILD FAILED in 11m 7s", "86 actionable tasks: 20 executed, 66 up-to-date". The failure is in `:app:testDebugUnitTest`: 1,388 tests, 2 failures, 0 skipped. Both are `ResultsStarPaletteTest`:
  ```
  theStarRowDrawsTheActivePaletteInTheLightTheme / …InTheDarkTheme:
  Colorblind: the unearned star's centre shows the background beneath it
  expected to be true
      at com.app.pegsolitaire.ui.results.ResultsStarPaletteTest.assertTheStars(ResultsStarPaletteTest.kt:140)
  ```
- **`./scripts/check.sh --wait --max-workers=4 --continue` on 7ce3314 — FAIL, the same two tests only,** so every other gate could run. "BUILD FAILED in 11m 9s", "104 actionable tasks: 15 executed, 6 from cache, 83 up-to-date".
  - `:app:testDebugUnitTest` executed: 161 suites, 1,388 tests, 2 failures (the two above), 0 errors, 0 skipped. The XMLs are stamped 03:22–03:24Z.
  - Green: ktlint, detekt, `dpSpLiteralCheck` (both modules), `:app:lint`, `:domain:koverVerify`, and `:domain:depthSweep` (run this time). Also `:domain:test` 294/0, `:domain:fuzz` 1/0 and `:build-logic:test` 55/0, all up to date.
  - The log has 0 `w:` lines from `src/main`.
- **Lint report** `app/build/reports/lint-results-debug.xml` — 0 errors, 62 warnings. None is in a file this round touched. They are in `gradle/`, `libs.versions.toml`, `App.kt`, `AudioRouteClassifier.kt` and `strings.xml`. Three are `dimens.xml` lines 16, 21 and 23 (`spacing_xl`, `board_min_touch_target`, `dialog_padding`), the same three as loop 1. This round adds no warning.
- **The `ResultsStarPaletteTest` probe** (uncommitted, reverted; one `println` of the unearned star's centre). Each was run as `./gradlew --max-workers=4 :app:testDebugUnitTest --tests '…ResultsStarPaletteTest'`:
  - **as the test stands:** fail. The centre is `#00000000`; the background is `#fff4f1fa` in light and `#ff1b1626` in dark.
  - **with `ResultsScreen` inside a `Surface(colorScheme.background)`,** the F-32 host surface: pass, with the centre equal to the background in all four cases.
  - **with `ResultsScreen` inside `BackdropTestHost(BackdropDensity.Sparse)`,** the production host's shape at Results' density: pass. The centre is the Sparse wash, `#fff6f0fa` in light and `#ff1b132c` in dark, within `isColour`'s tolerance. **The unedited assertions pass under the app's real host.**
- **Suppressions:** `git diff develop...HEAD -- app/src domain | grep -nE '^\+.*(@Suppress|@Ignore|!!|@OptIn|ktlint-disable|detekt:|tools:ignore|noinspection)'` — no match (exit 1). `git diff --name-status develop...HEAD | grep -iE 'baseline|detekt|lint'` — nothing (exit 1).
- **BD-19:** `git diff --name-status develop...HEAD -- app/src/test | grep -v '^A'` lists only `HomeLayoutTest.kt` and `fixture/HomeTestFixture.kt`, which is O3's scope. `HomeChipArrivalTest`, `GameStatusStripTest`, `HomeRibbonShapeTest`, `HomePaletteIdentityTest` and `ResultsStarPaletteTest` have no diff against `develop`.
- **BD-01:** the amended commands 1, 2 and 3 from the repository root each print zero lines (exit 1).
- **BD-04:** `git diff --stat develop...HEAD` over `res/drawable*`, `res/mipmap*`, `res/raw`, `assets/`, `libs.versions.toml` and `*.gradle.kts` prints nothing. No dependency was added, so no `THIRD_PARTY.md` row is owed.
- **BD-05:**
  - `git diff 8fac490..HEAD -- app/src/main | grep -E '^\+' | grep -E '0x|[0-9]\.dp|[0-9]\.sp|Color\('` matches one KDoc line ("360x640dp window").
  - The branch's whole delta over `develop`, outside `Color.kt` and comments, matches nothing (exit 1).
- **The start-path files the merge brought in:** `git diff --name-only 23bf220 8fac490` lists `MainActivity.kt`, `ui/home/` (`HomeBandColumn.kt`, `HomeBandSet.kt`, `HomeProgressRibbon.kt`, `HomeScreen.kt`, `HomeStage.kt`), `ui/theme/` (`AppThemeRoot.kt`, `BoardMarkPaletteState.kt`, `BoardMarks.kt`, `Clay.kt`, `Color.kt`, `Theme.kt`), and the `values-v35` and `values-night-v35` `themes.xml`. `git merge-base HEAD develop` gives f1c399a. See Open issue 3.

## Open issues / risks
1. **A spec conflict reddens the Check: colour-vision's `ResultsStarPaletteTest` against BD-14 (R1).**
   - **The conflict.** CV-13a's oracle reads the unearned star's centre as `colorScheme.background`. The test composes `ResultsScreen` bare, which held while Results' `Scaffold` painted the background. On this branch that `Scaffold` is transparent by design, and `TransparentContainersTest` pins it. The background is painted by the app host's F-32 `Surface` and the Sparse wash. Bare, the centre reads `#00000000`.
   - **Production is not at fault.** Under `BackdropTestHost(Sparse)` the unedited assertions pass (Checks).
   - **What I did:** under A7.5 and the dispatch's "unedited or reported, never retuned", I did not edit the test.
   - **Options for the owners:**
     - (a) The test composes `ResultsScreen` in `BackdropTestHost(BackdropDensity.Sparse)`, the production shape. Both probes are green.
     - (b) The test composes it in a `Surface(colorScheme.background)`. This keeps the exact-background oracle.
     - (c) The oracle becomes "the centre shows what lies beneath it", read against a frame without the star.
   - **Who decides:** the ux-designer (CV-AC05/CV-13a's wording) and the tech-lead (a carve-out like B-19a), with the code-auditor's approval.
   - **Note:** the auditor's loop-0 M3 list predates colour-vision's landing, so it does not name this test.
2. **`HomeChipArrivalTest.aChipThatArrivesAfterFirstPaintFadesInWithItsRoomWithNoStaggerDelay` is green but vacuous on this branch** (M3).
   - **Cause:** `HomeScreen` is transparent, and `renderHome` (`fixture/HomeTestFixture.kt`) draws no host `Surface`. So "not exactly `LightColors.background`" is true of every empty pixel: 6,080 px with no chip. P9 survives.
   - **Not fixed here.** A fix touches either the shared Home fixture, and so every Home suite's tree (BD-19), or that test.
   - **Options for the owners:**
     - (a) `renderHome` composes Home on a `Surface(colorScheme.background)`, the F-32 host. The defaults keep every other read as it is, but every Home suite's tree changes.
     - (b) The test counts ink against its own no-chip frame.
   - **Who decides:** the same owners as item 1.
3. **15e (M4).** The merge brought the start-path files listed in Checks, `MainActivity.kt` and `ui/theme/` included. Candy's sitting therefore needs arm A at the new merge-base, f1c399a. Licences (bc95b2a) is now on `develop` but not merged here, so the next merge-in (licences + talkback) will move arm A again. The tech-lead owns those implications.
4. **The merged oracle needs the auditor's fresh "amended, not weakened" line** (loop 0, "Limit of this line"). The evidence is above. The scope still stays within O3's two files. `HomeTestFixture.kt` was not touched in this round.
5. **The commit trailer.** These commits carry plugin rules/gitflow.md v1.0.0's single `Claude Fable 5.1` trailer on Opus-written code, as in loops 0 and 1. The producer still has to settle the form (the auditor's loop-0 Open issue 3).
6. **Nothing reads as hidden.**
   - I amended two unpublished commits before anything was reviewed:
     - the merge, 29ecb5e → 8fac490, to make the full-pill read collect every pill before asserting;
     - the BD-17 test, 77a52cd → 9d6bbeb, after ktlint's import order failed on it.
   - Every red above was run on 8fac490.
   - The first Check's log names `:app:testDebugUnitTest` FROM-CACHE but then fails that task. Its result XMLs are stamped inside that run, and the `--continue` run shows the task executed, with the same two failures.
7. **Device rows stay with QA:** F1 ON→OFF with the app resident, B-22a after Home's settle, A10's Device rows, BD-02's captures, and 15e (item 3).
8. **The deprecated `createComposeRule` (v1) is unchanged** in test sources, `develop`'s suites and this branch's alike. Warnings in test sources are not gated.

## Handoff
Next: producer —
1. **ux-designer and tech-lead:** rule on Open issues 1 and 2, the two develop tests this branch's transparency reaches. The Check cannot go green on this branch without a ruling on item 1. Patches for each option are ready for the developer once ruled.
2. **code-auditor (loop 2):**
   - the hand-resolved hunks of 8fac490;
   - the merged 15m oracle and its reds (re-apply at least M5c, M3d and 4w), then the "amended, not weakened" line or a refusal;
   - 9d6bbeb (BD-17 per palette);
   - G1 (7ce3314).
3. **tech-lead:** 15e's arm A after this merge-in (Open issue 3), and the sign-off after the auditor and QA.
4. **qa-engineer:** the device rows in Open issue 7.

Merge commit: 8fac490 (`develop` f1c399a; bc95b2a not merged). Final code HEAD: 7ce3314. Check: **FAIL**, with 2 of 1,388 app tests red, both `ResultsStarPaletteTest` (Open issue 1). Every other gate is green.
