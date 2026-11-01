# android-game-dev report — candy-backdrop: the 15m surface oracle (B-19a)
Date: 2026-11-01
Branch: feature/candy-backdrop (e07b105..e07b105, one work commit on bc7bd6a; the producer commits this report, see Open issue 1)
Skills loaded: ergoz-soft:standards (role android-game-dev; the worktree snapshot is v0.6.2 and the plugin is v1.0.0, so these plugin rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, testing, puzzle-design, play-store, kotlin-android, compose; also the product's `.claude/rules/lessons.md`), superpowers:test-driven-development (with writing-good-tests.md), gitflow, compose-skill, compose-ui, android-testing
Ran in parallel with: home-chip-arrival sign-off, licences F-4 ruling
Ran on: claude-opus-5-5 at effort max (the agent file's pin; the harness names no effort, and STATUS.md's 2026-10-31 directive dispatches developers one step lower, which this agent cannot see)
Fan-out: none
Loops: 1
Lessons applied: none (the snapshot lists 0 active lessons)

Producer note: the harness refused this agent's report-file write; the producer saved its returned text here verbatim.

## Summary
- **What was built:** the tech-lead's O1–O3, test-only, in `e07b105`.
  - 15m's read now compares frame 1 (Home) with frame 2: the pill's own finish modifiers laid over each pill, on that pill's own pixels.
  - Guards G1–G5 run in the spec's order. `PAINT_TOLERANCE` stays 1.
  - `renderHome` gains `darkTheme = false` and an empty `overlay` slot, and there is a new dark test.
- **Stop rule, on the unmutated build:** the largest home-vs-finish difference over the read area is **0** on every pill in both themes. The largest G3 row spread is **1**. No widening.
- **Reds 2, 3, 4 and 4w** fail at G5 on every pill. Red 4's strays all lie on rows 9..16 of a 40 px pill, inside a band that ends at 18.4 px.
- **G-red as specified** fails at **G2**, not G3, because G2 runs first. A variant that places the reference but draws nothing fails at G3. Every mutation is reverted.
- **Full Check:** green, 11m 17s. 1,294 app tests pass, `HomeLayoutTest` 17 of 17.

## Outputs
- `app/src/test/java/com/app/pegsolitaire/ui/home/HomeLayoutTest.kt`:
  - the oracle `assertEachPillPaintsOnlyItsSurfaceAndItsTexts(darkTheme)`, rewritten with G1–G5, and its KDoc;
  - the new test `theNarrowClassPillComposesNoGlyphInTheDarkThemeEither`;
  - the light test keeps its name, window and every assertion, and now renders through the overlay;
  - a private composable `ReferenceFinishes`, `REFERENCE_FINISH_TAG_PREFIX`, `WHOLE_PIXEL_SLACK` (G1's 0.01 px) and a `PillRead` holder;
  - `PAINT_TOLERANCE` is still 1, with the KDoc O2 specifies.
- `app/src/test/java/com/app/pegsolitaire/ui/home/fixture/HomeTestFixture.kt`: both `renderHome` overloads gain `darkTheme: Boolean = false` and `overlay: @Composable () -> Unit = {}`. The overlay is emitted inside `AppTheme`, after `HomeScreen`. The comment now reads "Light unless a test states dark; never read from the configuration".
- No production file changed: `git diff --name-only bc7bd6a..e07b105` lists only those two files.

## Stop rule (O1)

| theme | pill | rect (px, LTRB) | pixels read | max \|home − finish\| | max G3 row spread |
|---|---|---|---|---|---|
| light | 0 | 8,168,118,208 | 1196 | 0 | 1 |
| light | 1 | 126,168,235,208 | 568 | 0 | 1 |
| light | 2 | 243,168,352,208 | 1132 | 0 | 1 |
| dark | 0 | 8,168,118,208 | 1196 | 0 | 1 |
| dark | 1 | 126,168,235,208 | 568 | 0 | 1 |
| dark | 2 | 243,168,352,208 | 1132 | 0 | 1 |

- **How it was measured:** a temporary probe in the oracle, placed after frame 2 and never committed:
  ```kotlin
  reads.forEachIndexed { index, pill ->
      val maxReadDiff = pill.read.maxOf { p -> COLOUR_CHANNEL_SHIFTS.maxOf { s -> abs(home.channel(p, s) - finish.channel(p, s)) } }
      val maxG3Spread = pill.box.wholePixels().groupBy { it.y }.values.maxOf { row -> COLOUR_CHANNEL_SHIFTS.maxOf { s -> row.maxOf { finish.channel(it, s) } - row.minOf { finish.channel(it, s) } } }
      println("STOP-RULE darkTheme=$darkTheme pill=$index rect=${pill.rect} read=${pill.read.size} maxReadDiff=$maxReadDiff maxG3Spread=$maxG3Spread")
  }
  ```
- **Where it ran:** on the pre-commit tree. After that run I only re-wrapped G1's message line, re-ordered one import and reworded two KDocs, none of which changes behaviour.
- **Density:** the pills are 40 px tall, which confirms mdpi (tech-lead Open issue 4).

## Reds against e07b105
Each diff below applies with `git apply` on e07b105; I checked each with `git apply --check`. Every run used `./gradlew --max-workers=4 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.home.HomeLayoutTest'`. In every red run exactly 2 of 17 tests failed (the light and dark 15m tests), and G1–G4 passed wherever G5 was reached.

**Red 2 (light) and red 3 (dark): the M3b glyph.** The light and dark messages are byte-identical.
```diff
@@ -191,3 +191,3 @@ private fun StatPill(
         if (compact) {
-            CompactPillContent(label = label, value = value)
+            CompactPillContent(glyph = glyph, gilded = gilded, label = label, value = value)
         } else {
@@ -238,2 +238,4 @@ private fun FullPillContent(
 private fun CompactPillContent(
+    glyph: ImageVector,
+    gilded: Boolean,
     label: String,
@@ -253,2 +255,3 @@ private fun CompactPillContent(
     ) {
+        Icon(glyph, null, Modifier.then(if (gilded) Modifier.goldGradientFill() else Modifier).size(dimensionResource(R.dimen.home_stat_pill_glyph_size)), MaterialTheme.colorScheme.onSurfaceVariant)
         PillLabel(label, modifier = Modifier.weight(CONTENT_WEIGHT, fill = false))
```
(file `app/src/main/java/com/app/pegsolitaire/ui/home/HomeProgressRibbon.kt`)
```
com.google.common.truth.AssertionErrorWithFacts: G5: pixels each pill paints besides its finished surface and its texts: pill 0 131 px, x 29..46, y 174..189 (px), rows 12..27 below its top; pill 1 196 px, x 136..151, y 178..193 (px), rows 16..31 below its top; pill 2 85 px, x 266..277, y 174..190 (px), rows 12..28 below its top
expected      : {0=0, 1=0, 2=0}
but was       : {0=131, 1=196, 2=85}
```
The dark test really is dark: its G4 passed against `DarkColors.surfaceContainerHigh` (#2E2740), which is more than 100 levels from the light token.

**Red 4: a 10dp glyph inside the gloss band, light and dark.** Byte-identical messages in both themes. Every stray row lies in [top+8, top+18.4).
```diff
@@ -24,2 +24,3 @@ import androidx.compose.ui.res.dimensionResource
 import androidx.compose.ui.res.stringResource
+import androidx.compose.ui.unit.dp
 import androidx.compose.ui.semantics.clearAndSetSemantics
@@ -191,3 +192,3 @@ private fun StatPill(
         if (compact) {
-            CompactPillContent(label = label, value = value)
+            CompactPillContent(glyph = glyph, gilded = gilded, label = label, value = value)
         } else {
@@ -238,2 +239,4 @@ private fun FullPillContent(
 private fun CompactPillContent(
+    glyph: ImageVector,
+    gilded: Boolean,
     label: String,
@@ -253,2 +256,3 @@ private fun CompactPillContent(
     ) {
+        Icon(glyph, null, Modifier.align(Alignment.Top).then(if (gilded) Modifier.goldGradientFill() else Modifier).size(10.dp), MaterialTheme.colorScheme.onSurfaceVariant)
         PillLabel(label, modifier = Modifier.weight(CONTENT_WEIGHT, fill = false))
```
```
com.google.common.truth.AssertionErrorWithFacts: G5: pixels each pill paints besides its finished surface and its texts: pill 0 44 px, x 33..42, y 177..184 (px), rows 9..16 below its top; pill 1 64 px, x 138..145, y 177..184 (px), rows 9..16 below its top; pill 2 25 px, x 269..274, y 177..184 (px), rows 9..16 below its top
expected      : {0=0, 1=0, 2=0}
but was       : {0=44, 1=64, 2=25}
```

**Red 4w: red 4's glyph with `Color.White`.** The diff is red 4 with only the Icon line changed:
```diff
+        Icon(glyph, null, Modifier.align(Alignment.Top).then(if (gilded) Modifier.goldGradientFill() else Modifier).size(10.dp), Color.White)
```
Light: `but was : {0=44, 1=63, 2=24}` (pill 1 63 px, pill 2 24 px, rows 9..16). Dark: the same message as red 4 (44 / 64 / 25).
- **Pill 0 is still gold here.** `goldGradientFill` is SrcAtop, so on the gilded pill it overrides the tint.
- **Extra run, every pill white:** the same diff with `gilded = false` at the call site. In light the counts are 40 / 63 / 24. In dark they are 44 / 64 / 25.
- So a white glyph against the light gloss loses only 1 pixel per pill to the 1-level tolerance.

**G-red (test-side): `ReferenceFinishes` emits nothing.**
```diff
@@ -1046,3 +1046,3 @@ private fun ReferenceFinishes(references: List<IntRect>) {
     val fill = MaterialTheme.colorScheme.surfaceContainerHigh
-    references.forEachIndexed { index, rect ->
+    references.take(0).forEachIndexed { index, rect ->
         Box(
```
(file `HomeLayoutTest.kt`; the commented-out body gave the same message) Light and dark, byte-identical:
```
com.google.common.truth.ComparisonFailureWithFacts: G2: frame 2 composes reference 0 once
value of    : iterable.size()
expected    : 1
but was     : 0
```
**G-red variant: the reference is placed and tagged but draws nothing.** This is the case only G3 can catch.
```diff
@@ -1054,4 +1054,3 @@ private fun ReferenceFinishes(references: List<IntRect>) {
                         layout(placeable.width, placeable.height) { placeable.place(0, 0) }
-                    }.finishGlow(glow, shape)
-                    .glossFinish(FinishedContainer.RibbonPill, shape, fill = fill)
+                    }
                     .testTag(REFERENCE_FINISH_TAG_PREFIX + index),
```
```
com.google.common.truth.AssertionErrorWithFacts: G3: frame 2 must show pill 0's finish alone, flat along every row, but row y 183 (px) spreads 193 levels in one channel: frame 2 still shows its texts, or its reference was not drawn or carries a glyph
expected to be at most: 1
but was               : 193
```
Dark: the same text with `row y 185` and `194`.

## Checks
- `./gradlew --max-workers=4 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.home.HomeLayoutTest'`, with the probe — pass (17 of 17; stop rule 0 and 1, table above).
- `./gradlew --max-workers=4 --continue :app:ktlintCheck :app:detekt :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.home.HomeLayoutTest'` — fail on the first run (import order; one line over 120 at line 816). After fixing both: pass (17 of 17), and pass again on the committed content.
- Each red above, with the same `--tests` command — fail as intended (2 of 17 each), then reverted. `git status --porcelain` was empty after every revert.
- `./gradlew --max-workers=4 :app:testDebugUnitTest --rerun --tests 'com.app.pegsolitaire.ui.home.HomeLayoutTest'` at e07b105, after all reverts — pass (17 of 17, executed).
- `./scripts/check.sh --max-workers=4 --continue` at e07b105 (`./gradlew check` under the worktree lock) — pass (BUILD SUCCESSFUL in 11m 17s). 1,294 app tests, 0 failures; ktlint, detekt, `dpSpLiteralCheck`, lint, `koverVerify`, `:domain:depthSweep` green; `:domain:test` and `:domain:fuzz` UP-TO-DATE.
- `git diff bc7bd6a..e07b105 | grep -cE '^\+.*(@Suppress|@Ignore|!!|baseline)'` — pass (0).

## Open issues / risks
1. **Report file.** The harness refused this agent's report-file write; the producer saved this text.
2. **G-red hit G2, not G3.** O4 predicted G3 for "emits nothing", but O1's order puts G2 first. The draw-less variant shows G3 is live. I kept the spec's order; the code-auditor should rule which run counts as G-red, or accept both.
3. **4w's pill 0 is gold, as specified.** The all-white run covers pill 0 as well.
4. **Merge interplay with `bugfix/home-chip-arrival`** (now on develop). That branch hardens `textsWithin()` in this same oracle. The merge-in keeps both changes, re-runs M3b and M3d on the merged oracle, and keeps G1–G5.
5. **Tech design §8.5** does not yet name this two-frame form. The sync is the tech-lead's.
6. **Commit trailer.** `Co-Authored-By: Claude Fable 5.1` was written per plugin rules/gitflow.md; `Ran on:` records the true model.

## Handoff
Next: producer —
1. Commit this report on `feature/candy-backdrop`.
2. **code-auditor:** review e07b105 against B-19a and `tech-lead-15m-oracle.md` O1–O4. Re-apply at least 4w and G-red, plus the draw-less variant. Write BD-19's "amended, not weakened" line, or refuse it.
3. **qa-engineer:** review the test-only diff. It adds no new device row.
4. **tech-lead:** the branch sign-off, after both reports.
