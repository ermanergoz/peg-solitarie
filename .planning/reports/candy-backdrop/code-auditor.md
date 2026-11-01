# code-auditor report — candy-backdrop (direction B build and the 15m oracle), loop 0
Date: 2026-11-01
Branch: feature/candy-backdrop (4803cf0..5543313; reviewed as `git diff develop...HEAD`, merge base f918793)
Skills loaded: ergoz-soft:standards (role code-auditor. The worktree snapshot is v0.6.2 and the plugin is v1.0.0, so these plugin rule files were read in full: studio, reporting, gitflow, skills-policy, memory, kotlin-android, compose, testing. logging, puzzle-design and play-store are identical to the snapshot. Also the product's `.claude/rules/lessons.md`.), code-reviewer, security-reviewer, compose-skill, compose-performance-audit
Ran in parallel with: talkback merge-in, colour-vision merge-in, licences R2 fix
Ran on: claude-opus-5-5 at effort xhigh. **Staffing deviation:** `agents/code-auditor.md` pins fable, and rule 1 says code is reviewed on a model above its author's. The developer wrote this code on Opus, so this review ran on the same model as the author. That is allowed by the founder's Q-019 answer (2026-10-17: "Opus for every Fable-pinned role until 3.0.0 is published, each report recording the deviation"). As Q-019's recommendation asks, I ran the Check and the tests myself, uncached, and ran my own mutations rather than relying on the developer's recorded results.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- **The build is mostly sound:** the architecture follows A0–A11, every A8 constraint holds, and every Review row except BD-25 holds. The Check is green, and I re-ran all 1,294 app tests uncached: all pass.
- **One defect must be fixed (F1).** If "Remove animations" is turned on while the backdrop is running, the clock's time becomes infinite. After animations are turned back on, the scene stays frozen, its semantics still report Running, and it keeps requesting a frame on every vsync. I reproduced this on the real host.
- **Two smaller fixes are needed:** BD-25's screen previews (F2) and BD-05's scoped constants (F3).
- **15m oracle:** I re-ran reds 2/3, 4w, G-red, the draw-less variant and my own fill-token mutation. Each failed exactly as reported. **BD-19's "amended, not weakened" line is given** below, for e07b105 only.
- **Merging `develop` in will make one develop test pass without checking anything (M1).** Develop's new full-pill glyph read still passes with the glyph deleted on this branch, so the merge-in must move it onto the reference-finish oracle.

## Outputs
- /Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/code-auditor.md — this report.
- No source file was edited. Every mutation and probe ran in a throwaway `git worktree add --detach` of 5543313 under the session scratchpad. That worktree was removed afterwards (`git worktree list` no longer shows it).

## Checks
- `./scripts/check.sh --wait` (= `./gradlew check` under the worktree lock) at 5543313 — **pass**: "BUILD SUCCESSFUL in 11m 9s", "104 actionable tasks: 11 executed, 4 from cache, 89 up-to-date". In that run `:app:testDebugUnitTest` came FROM-CACHE, so I re-ran it with the next command.
- `./scripts/check.sh --wait --max-workers=4 :app:testDebugUnitTest --rerun` — **pass**: "BUILD SUCCESSFUL in 11m 11s". The result XMLs give 1,294 tests, 0 failures, 0 errors and 0 skipped. `HomeLayoutTest` ran 17 tests with 0 failures.
- Lint report `app/build/reports/lint-results-debug.xml` — pass with warnings. The only warning this branch adds is `ModifierParameter` at `testing/BackdropTestHost.kt:124` (F4). Every other warning comes from `develop`.
- Suppressions: `git diff develop...HEAD -- app/src | grep -nE '^\+.*(@Suppress|@Ignore|!!|@OptIn|ExperimentalMaterial|baseline)'` matched only three KDoc lines that say "baseline window". There is no new suppression, opt-in, `!!` or `@Ignore`, and no lint or detekt baseline file. **There is nothing to approve or reject.**
- BD-04: `git diff --stat develop...HEAD` restricted to `res/drawable*`, `res/mipmap*`, `res/raw`, `assets/`, `libs.versions.toml`, `*.gradle.kts` and image files prints nothing — **pass**. There is no new dependency, so no `THIRD_PARTY.md` row is owed.
- BD-05: a grep of the added and modified main `.kt` files (except `Color.kt`) for `0x`, `.dp`, `.sp` and `Color(` finds only lines that were already on `develop` and KDoc text. Hexes appear only in `Color.kt`, and `dpSpLiteralCheck` passes. The scoped-constant drift is F3.
- BD-01, the spec's three greps from the repository root:
  - (1) prints only `docs/store/README.md` lines 57–131. That is `develop`'s file, which this diff does not touch; the lines are meta-text naming this branch and quoting the word rules. With README.md excluded, `docs/store/` prints nothing, and so do `strings.xml` and `assets/`.
  - (2) prints nothing.
  - (3) prints nothing.
  - `grep -rniwE 'gel|jelly|soda|sugar|sweet'` over `strings.xml` and `assets/` prints nothing.
  - The diff adds no string. See F6 and F7.
- BD-19: `git diff --name-status develop...HEAD -- app/src/test | grep -v '^A'` lists only `HomeLayoutTest.kt` and `fixture/HomeTestFixture.kt`, which is exactly O3's scope. No other test that existed before this branch was edited.
- 15m reds were re-applied in the throwaway worktree. Each ran as `./gradlew --max-workers=2 :app:testDebugUnitTest --tests 'com.app.pegsolitaire.ui.home.HomeLayoutTest.theNarrow*' [-i]` and was reverted with `git checkout -- app`, which left `git status` clean:
  - **unmutated**: 2 of 2 pass, both themes.
  - **red 2/3 (M3b, light and dark)**: both fail at G5 with `{0=131, 1=196, 2=85}`.
  - **red 4w (white 10dp glyph in the gloss band)**: G5 fails in light with `{0=44, 1=63, 2=24}` and in dark with `{0=44, 1=64, 2=25}`. Every stray lies on rows 9..16 below the pill's top, which is inside the band (it ends at 18.4 px).
  - **G-red as specified (`references.take(0)`)**: both themes fail at `HomeLayoutTest.kt:758`, "G2: frame 2 composes reference 0 once".
  - **G-red, draw-less variant** (the reference is placed and tagged, but its `finishGlow` and `glossFinish` are removed): both themes fail at `HomeLayoutTest.kt:785`, which is G3.
  - **My own mutation**: the production `StatPill` fill changed to `surfaceContainerHighest` (`HomeProgressRibbon.kt:179`). Both themes fail at G5 with `{0=1196, 1=568, 2=1132}`, which is every read pixel. This confirms the KDoc's claim that a change to the pill's token fails the read.
- My probe for F1, an uncommitted `AuditProbeScaleZeroTest` in the throwaway worktree — **fails, so the defect is confirmed**:
  - On the real `AppNavHost`, with `SystemMotionScale` turned ON, then OFF for two frames, then ON again: `PROBE host before=1952 off=9223372036854775807 after=9223372036854775807 static=false motion=Running`.
  - On `LiveBackdropClock.run` alone, with one frame at scale 0 out of 20: `timeMs = 9223372036854775807`.
- My probe for M1, uncommitted, at `w412dp-h915dp`: `develop`'s `inkBesideTheTextsOfEachPill()` flat read, transcribed onto this branch.
  - With the glyph: "ink" counts 2,659 / 2,584 / 2,626.
  - **With the full pill's `Icon` deleted: 1,278 / 1,062 / 1,224, still ≥ `GLYPH_INK_FLOOR_PX` (20), so the test passes.**
- `git merge-tree --write-tree --name-only HEAD develop` reports content conflicts in `GameStatusStrip.kt`, `HomeScreen.kt`, `HomeStage.kt` and `HomeLayoutTest.kt`. `HomeProgressRibbon.kt` auto-merges.

## Findings

### Must fix (send back to the developer)

1. **HIGH — the backdrop clock never recovers from one frame at animator scale 0.** Location: `app/src/main/java/com/app/pegsolitaire/ui/backdrop/BackdropClock.kt:190-192`.
   - **The defect.** The loop reads `coroutineContext[MotionDurationScale]?.scaleFactor` on every frame and computes `timeExact += dtMs * rate / scale`, with no guard for a scale of 0.
     - When the user turns "Remove animations" on, the next frame's callbacks run before the recomposition that cancels the effect (`runs` is read in composition at :114). So one or more frames run at scale 0.
     - `timeExact` then becomes `+∞`, and `toLong()` gives `Long.MAX_VALUE`. If `dtMs * rate` is 0 on that frame, it becomes `NaN` instead, and `toLong()` gives 0. Either way the value sticks, because `∞ + x = ∞` and `NaN + x = NaN`.
   - **Shown by:** my probe above, on the real host in the Compose harness. After animations are turned back on, `timeMs` stays at 9223372036854775807, `isStatic` is false and `motion` is Running.
   - **The effects:**
     - Waves, lights and the gel sheen are frozen until the process dies, against B-05a and §6.1. The semantics hook says Running, so BD-16 is wrong.
     - The loop keeps awaiting a frame on every vsync (up to 120 Hz on the Pixel 9) with nothing moving. That lasts until the 60 s settle, and every tap after that wakes it for another 60 s. This works against B-22's battery ruling.
   - **Not covered by tests.** `BackdropReducedMotionTest` only goes OFF→ON, never ON→OFF→ON, the order a resident player takes.
   - **What the fix must give:** the loop never advances time with a non-positive or non-finite scale (that frame is treated as the rest path), plus a test of ON→OFF→ON on the host that asserts the time is finite and advances again. I do not prescribe the code.
2. **MEDIUM — BD-25's screen previews do not go through `BackdropPreviewHost`.**
   - **Where:** `ui/home/HomeScreen.kt:719,740`, `ui/home/HomeHero.kt:130`, `ui/game/GameStatusStrip.kt:241`, `ui/onboarding/TutorialScreen.kt:394,408`. `BackdropPreviewHost` is used only by `SceneBackdrop.kt:323,335,347`.
   - **Why it matters:** every screen container is now transparent. So these previews draw over no background, and the dark ones put `onBackground` text on the IDE canvas.
   - **The rule:** BD-25 and A10 require "Screen previews go through `BackdropPreviewHost` with the clock fixed", and the gel tiles and glossed pills and chips must be previewed in light and dark. Today they appear only inside those screen previews.
3. **LOW — BD-05's scoping: three new constants sit outside the holders BD-05 names.**
   - `ui/backdrop/BackdropGeometry.kt:205` `FRONT_WAVE_ALPHA = 1f` is a ratio, and its partner `WAVE_BACK_ALPHA` is in `BackdropMetrics`.
   - `ui/home/GelTile.kt:248` `SHEEN_CENTRE = 0.5f` is a ratio.
   - `ui/home/HomeScreen.kt:685` `CLASSIC_SHEEN_DELAY_MS = 0L` is a duration, and its partner `GEL_SHEEN_CHALLENGE_DELAY_MS` is in `BackdropMotion`.
   - BD-05 says every ratio goes in `BackdropMetrics` and every duration in `ClayAnim` or `BackdropMotion`. Each one is named, not magic, so this is low. It is still the stated pass line.
4. **LOW — a new lint warning in test code.** `app/src/test/java/com/app/pegsolitaire/testing/BackdropTestHost.kt:124`: `StandIn(modifier: Modifier = Modifier.fillMaxSize())` raises `ModifierParameter` ("Optional Modifier parameter should have a default value of `Modifier`"). It is test-only and a warning only, but it is the one lint warning this branch adds.

### Information (no change asked of this branch)

5. **The sheen can freeze mid-pass when the clock settles.** `ui/home/GelTile.kt:66-69` hides the sheen only when `clock.isStatic`. A settled clock is not static: §6.3 step 2 says its time stops "where it is". About 30% of settles on Home stop inside a tile's 2.5 s pass (or the Challenge tile's, which runs 0.6 s later), which leaves a still glint on the lip indefinitely. That is spec-conformant to §6.3, but it is at odds with B-05a rule 9 ("≤ 2.5 s long") and §6.1's "no glint" rest picture. **Route to the ux-designer** to rule whether a settled sheen hides.
6. **`UserFacingTextHasNoProtectedNamesTest.kt:58-77` stores the word list reversed**, so that BD-01's grep 3 prints zero lines over this diff. **Accepted.** It is not a suppression: the KDoc says why, the words are test data and not identifiers, and the first test proves the scanner finds every word in either case. A clearer long-term form is for grep 3 to exclude this one test file. That is the ux-designer's wording, not this branch's code.
7. **BD-01 grep 1's pass line cannot reach zero lines while `develop`'s `docs/store/README.md` quotes the rules** (lines 57–131). In substance BD-01's Auditor half is met: the store texts, `strings.xml` and `assets/` are clean. The grep's target needs amending, which is for the release-manager and ux-designer. This repeats the developer's Open issue 2.

### Review rows ticked
- **A8 / BD-28:**
  - **C1:** `SceneBackdrop.kt:256-280` draws each light as one radial disc (core, core stop, rim, transparent), with no stroke, ring or dot. Lights rise from below their rest centre with envelope 0 at the wrap (`BackdropPoses.kt:64-69`). The core-against-rim alpha clause is tested in `BackdropColorsTest`.
  - **C2:** `buildSceneModel` (`BackdropGeometry.kt:85-91`) takes no game input. The poses change only translation and alpha. The waves are composed behind the opaque slab at `zIndex(-1f)`.
  - **C3:** tokens only; the hue arc is tested.
  - **C4:** exactly two gel families (`FinishColors.kt:56,59`). `BoardCanvas.kt` and `BoardGeometry.kt` are not in the diff.
  - **C5:** opaque `shapes.large` tiles; the sheen is clipped to the lip (`GelTile.kt:111`, E9's one `clipPath`); `GEL_SHEEN_PERIOD_MS` = 10 000.
  - **C6:** the recipe gives `BoardSlab` no gloss (`FinishRecipe.kt:39,49`).
  - **C7:** tested by BD-08.
  - **C8:** no string added; no "jelly" in any added line; `Gel*` names only as allowed by B-10a.
  - **C9:** no asset.
  - BD-28 is met as a document (`research-analyst.md` line 336, "VERDICT: clean").
- **BD-03:**
  - The scene holds exactly the sky, two waves (each a fill, a lip and a clipped sheen) and 9/5/0 lights.
  - The recipe matches §3.5.1: rim, lip and glow on the slab, stage art and dock; all four parts on the five chips and pills; rim and lip on the banner, which keeps `clayShadow`.
  - The dock's discs and every Sparse container keep `clayShadow`.
  - The gel tiles have one family each, no texture and no wobble beyond `claySquish`.
- **BD-18:** the slab is filled with `surfaceContainerHigh` at full alpha by `glossFinish(fill = slab)`, with no gloss.
- **TG-AC01, TG-AC05:**
  - The title edit is exactly colour-vision §4.3's four edits: `Color.kt:115-120` matches lines 477-478; `Clay.kt` adds `titleGradient`, `titleGradientBrush()` and two stops; `HeroTitle` switches.
  - `ctaGradientBrush()` is unchanged, and `GameScreen` still uses it.
- **Architecture and Compose:**
  - The pure model and poses import no `android.*` and no Compose graphics.
  - There is one clock, read only in draw (`SceneBackdrop.kt:172-173`), and the sky sits in its own node.
  - The scene node has its own `graphicsLayer`, and all paths and brushes are built in `drawWithCache`. The draw lambdas allocate nothing: the `indices` loops are intrinsified and `translate` is inline.
  - The sheen goes through `derivedStateOf`, so a tile is invalidated only during a pass.
  - The scene-ready gate holds the scene, the clock's start and the battery-saver read.
  - The battery-saver receiver is `RECEIVER_NOT_EXPORTED` and unregistered on dispose.
  - The input observer reads at `Initial`, never consumes, and returns false for keys.
  - `rememberAnimationsEnabled` gates motion, with the F1 exception.
  - No network, no secrets, no new permission: the security pass is clean.

## BD-19 — the 15m carve-out (B-19a)

**APPROVED: criterion 15m's pixel oracle (`assertEachPillPaintsOnlyItsSurfaceAndItsTexts`, `PAINT_TOLERANCE`, the theme parameter, and `renderHome`'s two defaulted parameters) at e07b105 is amended, not weakened.** The reasons:
- **Tolerance:** it stays 1. Unmutated, both themes are green.
- **Reds:** 2 and 3 (M3b, light and dark) and 4 and 4w (inside the gloss band) fail G5 on every pill in both themes.
- **The anchor still bites:** a change to the pill's fill token fails every read pixel.
- **Guards:** G3 is shown live by the draw-less variant, and G4 still anchors to the theme's `surfaceContainerHigh` below the band.
- **Scope:** the diff stays within O3's list.
- **Strengthened:** the read now runs in dark too, and it reports every pill at once.
- **What it no longer catches:** a mark that is uniform along whole rows inside the gloss band, drawn by `glossFinish` itself, because the reference draws it too. That is not a glyph. G3 and G4 bound it, and `FinishModifierTest` pins the finish's drawing.

**G-red ruling:** G-red counts at **G2**. O1 orders G2 before G3, so a reference that emits nothing is caught where it should be: by placement. O4's "must show G3" assumed the opposite order. The **draw-less variant is the G3 red**. The two together are the evidence O4 asked for, and both were observed.

**Limit of this line:** it covers e07b105's oracle only. The `develop` merge-in edits the same function (M1), so the merged oracle needs a fresh auditor line.

## What the `develop` merge-in must reconcile
- **M1 — `HomeLayoutTest`:**
  - **Keep both hardenings:** `develop`'s `textsWithin()` must equal exactly `[label, value]` in order (M3d), checked ahead of G1–G5.
  - **Move develop's full-pill read onto frame 2.** `develop`'s new `onAWidePhoneEveryFullPillPaintsItsGlyphBesideItsTexts`, through `inkBesideTheTextsOfEachPill()`, reads the flat `LightColors.surfaceContainerHigh`. Under B-19's gloss that read passes even with the glyph deleted (probe: 1,062–1,278 px against a floor of 20). It must move onto the reference-finish frame 2: ink is a pixel that differs from the pill's own finish.
  - **Re-show the reds on the merged oracle:** M5c (full-pill glyph removed, which must fail), M3b, M3d, 4, 4w, G-red and the draw-less variant, in light and dark.
  - Only then can the merged oracle get the auditor's line.
- **M2 — content conflicts** in `GameStatusStrip.kt` (D-05e's `clearAndSetSemantics` timer against the pill's finish), `HomeScreen.kt` and `HomeStage.kt` (`develop`'s `HomeBandColumn` and stage rework against the transparent `Scaffold`, the gel tiles and the art's finish). `HomeProgressRibbon.kt` auto-merges, and `develop`'s third shape, `RibbonShape.Stacked`, also takes the gloss, so 15w's reads must be checked against it.
- **M3 — other `develop` pixel reads that this branch's transparency or finish reaches:**
  - `HomeChipArrivalTest`'s count of pixels that are not exactly `LightColors.background`, now that `HomeScreen` is transparent;
  - its count of stage-art pixels exactly equal to the slab, now that the art has a rim and lip;
  - `GameStatusStripTest`'s walk down the rows while each pixel equals the slab.

  Each stays green unedited, or is reported as a spec conflict under A7.5. None is retuned silently.
- **M4:** the full Check after the merge, and the tech-lead's 15e implications, because the merge brings start-path files.

## Open issues / risks
1. **Model deviation (Q-019):** reviewed on the author's model. Mitigated by an uncached re-run and my own probes.
2. **BD-02's auditor line is left open,** as dispatched. It follows QA's captures.
3. **Commit trailer.** Plugin `rules/gitflow.md` v1.0.0 asks for a single `Claude Fable 5.1` trailer. This review ran on Opus, so the commit carries the harness's attribution lines, as the producer's own last commit (5543313) does. The producer may settle which form the studio wants.
4. **Device rows are untested here:** BD-02, 09, 12, 13, 16 (frame counts), 21, 22, 24 and 27, and TG-AC03/04 are QA's. F1 also affects BD-16's device run: QA should repeat "Remove animations" ON→OFF with the app resident once F1 is fixed.

## Handoff
Next: producer —
1. **android-game-dev:** fix F1 (with a test of ON→OFF→ON), F2, F3 and F4, then re-run the Check. Do the merge-in under M1–M4. Bring the merged 15m oracle back with its reds for a fresh auditor line.
2. **ux-designer** (non-blocking): F5, whether a settled sheen hides. F6 and F7, BD-01's grep wording.
3. **qa-engineer:** A10's Device rows, BD-02's captures (then this role's BD-02 line), and the ON→OFF→ON check after F1.
4. **tech-lead:** sign-off waits on loop 1 of this review and QA's report.

VERDICT: FAIL
