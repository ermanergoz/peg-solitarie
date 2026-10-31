# android-game-dev report — candy-backdrop (direction B "Deep gloss")
Date: 2026-10-31
Branch: feature/candy-backdrop (3ca373b..8613efb; 3ca373b is the merge of develop)
Skills loaded: compose-skill, compose-ui, android-testing, ergoz-soft:standards (role android-game-dev; plugin rules v1.0.0 read in full because the worktree snapshot is v0.6.2: studio, reporting, gitflow, skills-policy, logging, memory, testing, puzzle-design, play-store, kotlin-android, compose; product `.claude/rules/lessons.md`), superpowers:test-driven-development, gitflow
Ran in parallel with: none. A 15e sitting held the host until `host-free` appeared; no Gradle ran before it did.
Ran on: claude-opus-5-5 at effort max
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons)

## Summary
- Direction B is built to the tech-lead's A0–A11/E1–E12, in six test-first units: tokens, model and poses; the host on one clock; transparent containers; the gloss finish and the title gradient; the gel tiles; same-run pixel comparisons. 1,293 JVM/Robolectric tests run.
- **The Check is not green, for one reason I must not work around** (brief §6, A7.5): phase 15's `HomeLayoutTest` (criterion 15m) requires every ribbon-pill pixel outside its texts to be flat `surfaceContainerHigh`, and B-19 puts the white gloss there. No test was edited and no value retuned. The producer routes it to the ux-designer (Open issue 1).
- `develop` merged in with no conflict. No candy test drove reduced motion through `Settings.Global`; every new one uses `SystemMotionScale`.
- Tripwire: `navigation/`, `ui/home/`, `ui/theme/` changed. **A 15e sitting is owed** on this branch (BD-23 (b)); its validity clause holds (no schema, no DataStore key).

## Outputs
New, `app/src/main/java/com/app/pegsolitaire/`:
- `ui/backdrop/BackdropDensity.kt`, `BackdropMetrics.kt` (spec §4.3), `BackdropMotion.kt` (§4.4), `BackdropDimens.kt`: the constants and the px holder.
- `ui/backdrop/BackdropGeometry.kt`, `BackdropPoses.kt`: the pure model and pose functions (A1). They use `Long` time and allocate nothing.
- `ui/backdrop/BackdropClock.kt` (A2, A5), `BackdropSceneReady.kt` (R3, A6), `BackdropInput.kt` (A5.3), `PowerSaveMode.kt` (A5.4): the clock, the frame-1 gate, the input observer and battery saver.
- `ui/backdrop/SceneBackdrop.kt` (two-slot cross-fade, sky node, one scene node, semantics hook, previews) and `BackdropPreviewHost.kt`.
- `navigation/BackdropDestinations.kt` (density per destination, `NavBackdrop`).
- `ui/theme/BackdropColors.kt`, `FinishColors.kt` (with `GelColors`), `FinishRecipe.kt`, `Finish.kt` (`finishGlow`, `glossFinish`).
- `ui/home/GelTile.kt` (`gelSurface`, `GelTileContent`, `GelRipple`) and `GelFootprintArt.kt`.

Edited:
- `navigation/AppNavHost.kt`: the F-32 host `Surface`, the providers, the driver, the observer `Box`, the explicit fades and `NavBackdrop` after the `NavHost`, plus a `backdropFrames` test seam.
- Theme: `Color.kt` (every hex), `Clay.kt` (title fix only: `titleGradient`, `titleGradientBrush()`, two stop constants), `Theme.kt` (two providers), `ClayAnim.kt` (`NAV_TRANSITION_MS` added; `BLOB_*` removed).
- Screens: `HomeScreen.kt`, `HomeHero.kt`, `HomeProgressRibbon.kt`, `HomeStage.kt`, `GameScreen.kt`, `GameStatusStrip.kt`, `GameActionDock.kt`, `NoWinBanner.kt`, `ClassicCatalogScreen.kt`, `ResultsScreen.kt`, `SettingsScreen.kt` and `TutorialScreen.kt`.
- `res/values/dimens.xml`: one `<!-- Backdrop -->` block of 24 tokens; `blob_size` and `blob_drift_range` removed.
- Deleted: `ui/home/HomeBlobs.kt`.

**`FinishedContainer` call sites** (brief §7):

| Container | File |
|---|---|
| `BoardSlab` | `ui/game/GameScreen.kt` |
| `StageArt` | `ui/home/HomeStage.kt` |
| `DockSlab` | `ui/game/GameActionDock.kt` |
| `BackChip`, `TimerPill` | `ui/game/GameStatusStrip.kt` |
| `GearChip` | `ui/home/HomeHero.kt` |
| `RibbonPill` | `ui/home/HomeProgressRibbon.kt` |
| `ContinueChip` | `ui/home/HomeScreen.kt` (`ResumeTile`) |
| `RescueBanner` | `ui/game/NoWinBanner.kt` (rim and lip; its `clayShadow` kept) |

The dock's discs, dialogs, snackbars and every Sparse screen's containers keep `clayShadow`.

**Opt-ins:** none. `LocalRippleConfiguration`/`RippleConfiguration` carry no experimental marker in material3 1.4.0, which is what BOM 2026.06.00 resolves to. `withInfiniteAnimationFrameNanos` (animation-core 1.11.3), `MotionDurationScale`, `repeatOnLifecycle` and `ContextCompat.registerReceiver` exist as the brief names them. I read the cached source jars; no Gradle was involved.

New tests, under `app/src/test/java/com/app/pegsolitaire/`:
- **Pure logic:** `ui/theme/BackdropColorsTest` (BD-08, C1, C3), `ui/theme/TitleGradientContrastTest` (TG-AC02, TG-AC05 pin), `ui/theme/FinishRecipeTest` (BD-03/18), `UserFacingTextHasNoProtectedNamesTest` (BD-01), `ui/backdrop/BackdropMetricsTest` (BD-10), `BackdropPoseTest` (BD-11 g, BD-16 b), `BackdropGeometryTest` (BD-11 a–f, h, i, at the four windows).
- **Host:** `BackdropDensityTest` (BD-06 with the restore case, BD-07), `BackdropSemanticsTest` (BD-15), `BackdropFirstFrameTest` (BD-23 a), `BackdropReducedMotionTest` (BD-16 a, live toggle), `BackdropSettleTest` (BD-16 c: settle, tap reaches the board, navigation, resume, battery saver), `SkyContinuityTest` (BD-21).
- **Screens and finish:** `TransparentContainersTest` (BD-14 with the F-32 probe), `ui/theme/FinishModifierTest`, `ui/home/GelTileTest` (BD-27 layout half, pressed darkens, announcement unchanged).
- **Pixel comparisons:** `BackdropPixelTests.kt` holds `BoardInteriorIdentityTest` (BD-17/18), `BackdropFontScaleTest` (BD-20), `BackdropMirrorTest` (BD-22) and `BackdropStatusBandTest` (BD-09).
- **Helpers:** `testing/Wcag.kt`, `testing/BackdropTestHost.kt`.
- No existing test was edited.

## Commits (red, then green)
| Red (test) | Green (feat) | Unit |
|---|---|---|
| 6f142f0 | 4406a92 | tokens, model, poses, recipe (red commit carries stubs) |
| 6fe4fdb | — | test-only fix: wrap tolerance, hue asserts (Reds, note 2) |
| 4040968 | af16daf | the host (red commit's `AppNavHost` hosts no backdrop) |
| d634599 | 722c65c | transparent containers (red against `develop`'s screens; no stub) |
| 2e485fa | e7ccd2f | the finish and the title (red commit's `Finish.kt` draws nothing) |
| 73e3cb3 | 6fa2eef | the gel tiles (red against the clay tiles; no stub) |
| c9fa9e3 | — | pixel comparisons (reds by mutation, below) |
| — | 8613efb | `ktlintFormat`; BD-01's diff grep cleared |

Every commit ends with one trailer, `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, as rules/gitflow.md (plugin v1.0.0) and the dispatch require. No `prepare-commit-msg` hook is installed, so it was written by hand; the harness reminder's other lines were not added. `Ran on:` records the true model.

## Reds (each watched failing before green)
| Test class | Red run against | Result |
|---|---|---|
| Unit 1's seven classes | stubs restored from 6f142f0; BD-01's scanner stubbed to `emptyList()` | 23 of 42 failed, all assertions (`u1-red.log`) |
| Unit 2's six classes | 4040968's `AppNavHost` (no backdrop hosted) | 12 of 35 failed: every host test except `SkyContinuityTest` |
| `SkyContinuityTest` | a dipping cross-fade (outgoing slot faded out) | failed: "Home to the board frame 8 point 2" (note 3) |
| `TransparentContainersTest` | `develop`'s screens | failed: Home paints `#F4F1FA` over the sentinel |
| `FinishModifierTest` | 2e485fa's no-op `Finish.kt` | failed: "BoardSlab … the fill beneath the gloss" |
| `GelTileTest` | the clay tiles | 2 of 5 failed (deep gel; pressing darkens) |
| `BoardInteriorIdentityTest` | mutation: slab fill at alpha 0.5 | failed: 62,227 differing pixels |
| `BackdropMirrorTest` | carries its own red: the same comparison on an unmirrored scene must exceed the tolerance | asserted on every run |

Notes, so nothing reads as hidden:
1. **Order.** For units 1, 2 and 4 the implementation was drafted before its test file, so their red was proven against stubs or a no-backdrop host rather than first-written code. Units 3 and 5 were test-first against `develop`'s code.
2. **Tests corrected before going green, none weakened against the spec.**
   - `BackdropPoseTest`: the wrap check's alpha bound became 1e-3 at ±1 ms. The 1e-4 bound was tighter than the ramp the spec defines.
   - Truth's `isIn` needs a Guava `Range`, so those asserts became explicit bounds.
   - `BoardInteriorIdentityTest`'s mask now follows the rounded outline inset 2dp. A deflated rectangle had wrongly counted the corners outside the shape.
3. **`SkyContinuityTest` gained a Home↔board leg.** On the spec's path one end of every transition is the Sparse wash, which is nearly the background. A dip toward the background there still lands on the segment, so the dip stub passed at first. Home and the board share their sky, so the added leg is the pair on which a dip shows.
4. **No observed red:** `BackdropFontScaleTest` and `BackdropStatusBandTest` passed as properties of the built code. The F-32 half of `TransparentContainersTest` already held from unit 2's host `Surface`.

## Merge of develop
- `git merge --no-ff develop` → 3ca373b: **no conflict**, so no hunk to report (404 files, +62,727/−1,660).
- `grep -rn ANIMATOR_DURATION_SCALE app/src/test`: two hits, neither a candy test. One is `testing/SystemMotionScale.kt`'s KDoc; the other is `ui/theme/AnimationsEnabledGateWithoutScaleTest.kt`, a `develop` test of the gate, left as is. Nothing to retarget, since the branch had no tests before this build.
- **colour-vision is not on `develop` yet**, nor are talkback-board and the licences screen. Its §10.3 duties apply at the next merge-in (Handoff).

## Checks
- `cd ../PegSolitarie-candy-base && ./scripts/check.sh --max-workers=4` (baseline, merged base 3ca373b): pass (BUILD SUCCESSFUL in 11m 16s).
- `./gradlew --max-workers=4 :app:testDebugUnitTest --tests …` per unit (logs `u1-red`, `u1-green`, `u2-red`, `u2-red-sky`, `u2-green-sky`, `u3-red`, `u3-green`, `u4-red`, `u4-green`, `u5-red`, `u5-green`, `u6`, `u6-red` in the session scratchpad): as in Reds, then green per unit.
- `./scripts/check.sh --max-workers=4 --continue` on c9fa9e3: fail.
  - ktlint formatting in new files (fixed by `./gradlew :app:ktlintFormat`, commit 8613efb).
  - 1293 tests, 1 failed: `HomeLayoutTest.theNarrowClassPillPutsItsLabelBesideItsValueAndComposesNoGlyph` ("pill 0 paints 444 px besides its surface and its texts").
  - detekt and lint passed. Lint's only warnings are `develop`'s `InlinedApi` in `audio/AudioRouteClassifier.kt`.
- `./scripts/check.sh --max-workers=4 --continue` on 8613efb (the final code commit): fail, on exactly one test (BUILD FAILED in 11m 12s). ktlint, detekt, `dpSpLiteralCheck` and lint pass. 1,293 tests ran, 1 failed: `HomeLayoutTest.theNarrowClassPillPutsItsLabelBesideItsValueAndComposesNoGlyph`, the 15m conflict of Open issue 1. All 1,292 others pass, every phase-15 suite included, none edited.
- BD-01 greps from the repo root:
  - (1) prints lines only from `docs/store/README.md` (Open issue 2);
  - (2) prints nothing;
  - (3) `git diff develop...HEAD -- app/src | grep -niE '^\+.*(candy|sweet|jelly)'` prints nothing.
- BD-04: `git diff --stat develop...HEAD` shows nothing under `res/drawable*`, `res/mipmap*`, `res/raw` or `assets/`. It also shows no image, no `libs.versions.toml` entry and no build-file change.
- BD-05: hexes only in `Color.kt`; dp only in `dimens.xml`. No new `@Suppress`, baseline, `@Ignore` or `!!` (`git diff develop...HEAD -- app/src | grep -E '^\+.*(@Suppress|@Ignore|!!)'` prints nothing).
- **Start-path tripwire.** `git diff --name-only develop...HEAD | grep -E '^app/src/main/(java/com/app/pegsolitaire/(App\.kt|MainActivity\.kt|navigation/|ui/home/|ui/theme/|di/)|AndroidManifest\.xml|res/(font/|values[^/]*/themes\.xml))'` prints:
  - `navigation/`: `AppNavHost.kt`, `BackdropDestinations.kt`;
  - `ui/home/`: `GelFootprintArt.kt`, `GelTile.kt`, `HomeBlobs.kt` (deleted), `HomeHero.kt`, `HomeProgressRibbon.kt`, `HomeScreen.kt`, `HomeStage.kt`;
  - `ui/theme/`: `BackdropColors.kt`, `Clay.kt`, `ClayAnim.kt`, `Color.kt`, `Finish.kt`, `FinishColors.kt`, `FinishRecipe.kt`, `Theme.kt`;
  - nothing in `App.kt`, `MainActivity.kt`, `di/`, the manifest, `res/font/` or `themes.xml`.

  **Verdict: a 15e sitting is owed** (BD-23 (b), then the separate animations-on jank block, BD-23 (c)). Validity: `git diff develop...HEAD -- app/schemas` is empty, and the diff adds no DataStore key.

## Open issues / risks
1. **Spec conflict that blocks a green Check (A7.5, brief §6). The ux-designer's call, through the producer.**
   - **The conflict:** B-19 and §3.5.1 give the ribbon pills a white gloss from the top edge to 46% of their height. 15m's "no decorative glyph" oracle reads every pixel of the narrow-class pill, inside `spacing_sm` and outside its texts, as `surfaceContainerHigh`.
   - **Why both cannot hold:** the gloss lies inside that read area, so no implementation satisfies both.
   - **The choices:** (a) drop the gloss on `RibbonPill` (a one-line recipe change); or (b) the owner of 15m amends that oracle to admit the finish's gloss and lip.
   - I did neither. `HomeLayoutTest` is unedited and the recipe follows the spec.
2. **BD-01 grep 1 cannot print zero lines while `docs/store/README.md` exists.** That file belongs to the release-manager and is on `develop`. It names this branch and quotes the word rules themselves. Either the grep targets the store texts (`docs/store/*/`) or the README stops quoting them. Release-manager and ux-designer, not this branch.
3. **colour-vision's §10.3 duties are still owed** at the merge-in after it lands:
   - keep both providers in `Theme.kt`'s one `CompositionLocalProvider`, both `Clay.kt` hunks and both blocks;
   - run `BoardInteriorIdentityTest` with the palette on and off, `TransparentContainersTest` and its `HomePaletteIdentityTest`.

   E7 applies to the licences routes whichever branch lands second.
4. **Device-only rows are untested here.** BD-02, 09, 12, 13, 16 (frame counts), 19 (scrim and handle), 21 (recording), 22 (`ar-XB`), 24 and 27, and TG-AC03/04 belong to the qa-engineer.
5. **Not built, as ruled:** the frame-rate hint (A5, E10) and per-element layers (E11). Both are audit-time options.
6. **Housekeeping.**
   - `local.properties` was copied from the main checkout into this worktree to build; it is untracked and git-ignored.
   - The baseline worktree `../PegSolitarie-candy-base` was removed after use.

## Handoff
Next: producer —
1. Route Open issue 1 to the ux-designer (with the tech-lead if 15m's oracle is to change). Re-dispatch me for the one-line follow-up, then re-run the Check.
2. Open issue 2 goes to the release-manager.
3. When colour-vision is on `develop`, dispatch me to merge it in under its §10.3 (Open issue 3).
4. code-auditor: review against A10's Review rows, A8's C1–C9 and BD-01/03/04/05/18. BD-02's auditor line follows QA's captures.
5. qa-engineer: A10's Device rows, then the 15e sitting on `Pixel_9a_bench` (owed, per the tripwire above), then the BD-23 (c) jank block.
6. tech-lead: sign-off after both reports.
