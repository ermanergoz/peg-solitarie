# tech-lead report — candy-backdrop (direction B addendum to R1–R6)
Date: 2026-10-18
Branch: feature/candy-backdrop (report only)
Skills loaded: ergoz-soft:standards (role tech-lead; plugin rules v1.0.0 read in full because the worktree snapshot is v0.6.2: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch; product `.claude/rules/lessons.md`), android-architecture, android-gradle-logic
Ran in parallel with: phase-15 H-3 cold-start sitting (qa-engineer), tutorial haptics ruling, store-copy drafts
Ran on: claude-opus-5-5 at effort xhigh — **staffing deviation:** the tech-lead is pinned to fable (`agents/tech-lead.md`: `model: fable`, `effort: xhigh`); this ran on Opus under the founder's Q-019 answer (2026-10-17), recorded in `/Users/ermanergoz/Desktop/PegSolitarie-p15/.planning/questions/FOUNDER.md`
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
Ran on Opus, not the pinned Fable, under Q-019. This addendum rules R1–R6 for direction B and closes tech design §16 #16 (fit: FIT, under A7's conditions).
Void: R2 whole; in R1 the orbs, `HomeBlobs`' move, the wing builders and the `HomeStage` wing edit; in R5 the wing clauses. New: a pure wave/light/pose model, one clock per host (a `CompositionLocal` to the gel tiles), the finish in new files (`Clay.kt` only for the title fix), the gel tile as a transparent `Card` with a darkening ripple.
B-22's settle default is **accepted** with four amendments (A5), battery saver included. R3's gate holds back waves, lights, clock and battery read. The research-analyst's A.3/A.4 are build constraints (A8).
The Home title fix ships here; the colour-blind palette and the licences screen ship on their own branches (A9). The developer brief is the last ruling section.

## Rulings

### A0 — What stands, what is void, what changes

`tech-lead-architecture.md` (2026-10-17) still governs where this addendum is silent. Where the two differ, this addendum governs, and both govern over `docs/ux/candy-backdrop.md` (third edition).

| Ruling | Under direction B |
|---|---|
| R1 | **Stands:** one backdrop in `AppNavHost` behind the `NavHost`; the density as a pure function of the committed destination (`else` → Sparse); `NavBackdrop` composed after the `NavHost` and drawn behind it at `zIndex(−1f)`; the two-slot cross-fade on `ClayAnim.NAV_TRANSITION_MS`; the F-32 host `Surface`, transparent `Scaffold`s, `TopAppBar`s and tutorial root; insets untouched; `MainActivity` untouched; the semantics hook; `BackdropColors` / `LocalBackdropColors`; draw lambdas allocate nothing. **Void:** `BackdropOrbs.kt`, the move of `HomeBlobs`, `BLOB_PEEK_FRACTION`, `ORB_SECONDARY_Y`, `buildStageWings`, `StageWingsModel` and the `HomeStage.kt` wing edit. **Changed:** the model (A1), the clock (A2), `backdropLayers` = {Sky, Waves, Lights}, a new `backdropMotion` key (A2), the token count (16 per theme, spec §4.1), and the file table (brief §3). `HomeBlobs.kt` is deleted, not moved; `ClayAnim.BLOB_DRIFT_MS` and `BLOB_ALPHA` go with it |
| R2 | **Void, whole** (B-12a). D3's reasoning survives: nothing adds a semantics property to the stage subtree (A7) |
| R3 | **Stands**, with A6: the gate now holds back the waves, the lights, the clock and the battery-saver read. The 15e sitting runs on this branch on `Pixel_9a_bench` (tech design §10, which superseded R3's "Pixel_9a"), with §10's start-path list |
| R4 | **Stands, on its own branch** (A9). Nothing in R4 changes |
| R5 | **Stands:** no stored goldens, same-run pixel comparisons, the test kinds, the phase-15 suites unedited. **Void:** BD-07's wings clause, BD-11's wing and sprinkle rules, the stage's custom key. **Rewritten:** the split in A10 |
| R6 | **Amended** (A11): the producer's merge order puts phase 15, check-gates, the audio branch and F-1 on `develop` before this branch is built, so the four shared files become a merge-in, not a parallel collision |

### A1 — The pure model: waves, lights and poses

**Files.** `ui/backdrop/BackdropGeometry.kt` builds the scene; `ui/backdrop/BackdropPoses.kt` holds the pose functions. Neither imports `android.*`, a Compose `Path`, `Brush`, `Color` or `DrawScope`. Compose geometry value types (`Offset`, `Size`) are allowed, as `BoardGeometry` uses them. Both are tested on the JVM or Robolectric without composing anything.

**The builder.** `buildSceneModel(density, widthPx, heightPx, rtl, dimens: BackdropDimens): SceneModel`, an `@Immutable` tree of primitives in px:
- `SkyModel`: the density's stops (fraction, colour role), from `BackdropMetrics`.
- `WaveModel` × 2, back then front: crest point, ellipse semi-axes, bottom edge (window bottom plus the wave's vertical bob), lip depth, sheen centre and radii, the three fill-stop y values in the wave's own coordinates, bob amplitudes and direction signs, half-cycle in ms, the back wave's per-draw alpha.
- `LightModel` × 9 / 5 / 0: rest centre, diameter, kind, period in ms, `φ0`, opacity, set.
- The density's calm end and calm-fade distance in px.

Sparse returns the sky and empty lists. RTL mirrors every x and every x-direction sign inside the builder, so the draw layer never branches on direction. The draw layer turns a model into `Path`s and `Brush`es once, in its cache block.

**The poses.** Pure functions of the model element and the clock time `t`:
- `waveOffsetX(wave, t)`, `waveOffsetY(wave, t)`: `p(t) = −cos(π · t / T)` times the bob (spec §3.4);
- `lightDy(light, t)`, `lightAlpha(light, t, model)`: the rise, the envelope and the calm factor;
- `gelSheenProgress(t, delayMs)`: the travel fraction in the last quarter of the loop, or the constant `SHEEN_HIDDEN` outside it;
- a rest variant for each (`waveRestOffset = 0`, `lightRestDy = 0`, the rest alpha `opacity × calm` at the rest centre, the sheen hidden).

Rules for every pose function:
- **Time is `Long` milliseconds of backdrop time.** Each function reduces `t mod period` in `Long` before any floating-point step, so a session of any length is exact.
- **It returns a primitive (`Float`) and allocates nothing**, because the draw lambda calls it per element per frame. No pose object, no pair, no list.
- **Rest is its own path, never a magic time.** The rest pose is not the pose at any single `t`, because each light rests at its own phase.

**What it replaces.** The second edition's mounds, motifs, sprinkles and wings. `LIGHT_REST_PHASE` is computed from the two rise tokens (spec §4.3), so it reaches the model through `BackdropDimens`, never as a literal.

### A2 — One clock per host, and its route to the gel tiles

**The clock.** `ui/backdrop/BackdropClock.kt` holds a `@Stable class BackdropClock` with:
- `timeMs`: a `mutableLongStateOf`, the backdrop time;
- `isStatic`: true when the rest pose is drawn (animator scale 0, battery saver, or before the clock first starts);
- `motion`: `BackdropMotionState` ∈ {Running, Settling, AtRest};
- `onInput()`: a non-suspending, non-snapshot signal (`Channel<Unit>(CONFLATED).trySend`), safe to call from a pointer handler.

`rememberBackdropClock(density, sceneReady, frames)` creates it in `AppNavHost`'s host, once. One `LaunchedEffect` runs its loop. That loop is the only frame source of the scene and the sheen (B-24); the cross-fade's `Animatable` is navigation (B-13a), not scene motion.

**The loop.**
- It starts only after the scene-ready flag flips (A6), only when `rememberAnimationsEnabled()` is true, and only while the power-save state is false (A5).
- It runs inside `LocalLifecycleOwner.current.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED)`. Pausing cancels it and keeps `timeMs`; resuming continues from that time, with the first frame's delta reset to zero, so nothing jumps.
- It waits for frames through a seam, `fun interface BackdropFrames { suspend fun awaitFrameNanos(): Long }`. The production value is `{ withInfiniteAnimationFrameNanos { it } }`, the API `rememberInfiniteTransition` uses. The Compose test harness's `InfiniteAnimationPolicy` therefore treats the backdrop exactly as it treats today's `HomeBlobs`, and every phase-15 suite that composes `AppNavHost` keeps idling unedited. Tests that need the clock to run inject frames from the test clock (A10).
- Each frame: `timeMs += (Δreal × rate / scale)`, where `scale` is `coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f`, the animator duration scale Compose's own animations follow (B-24).
- The idle timer, the settle ease and the wake ease run on **unscaled** frame time. Only the backdrop time is scaled.
- On Sparse, once the cross-fade has finished, the loop has nothing to move. It settles and stops requesting frames until a navigation to a moving density wakes it.

**Reading it.**
- Only in the draw phase: inside `onDrawBehind`, never in a `drawWithCache` cache block (that would rebuild the cache every frame) and never in composition.
- The scene's L2/L3 node reads `timeMs` once per frame, so only that node's draw is invalidated. The sky node never reads it.

**The route to the gel tiles: a `CompositionLocal` from the host.**
- `LocalBackdropClock = staticCompositionLocalOf<BackdropClock> { StaticBackdropClock }`, provided by the host beside `LocalBackdropSceneReady`.
- It is static because the object never changes; the time is read through its snapshot state in draw.
- The default, `StaticBackdropClock`, is always static. A tile outside the host (a preview, a component test) draws no sheen. `FixedBackdropClock(timeMs)` sets a pose for previews and pixel tests.
- **The tile's draw reads a `derivedStateOf { gelSheenProgress(clock.timeMs, delay) }`**, created in composition and read in draw. Outside the 2.5 s pass its value stays `SHEEN_HIDDEN`, so the tile is not invalidated for 7.5 s of every 10 s loop.

**The semantics hook gains `backdropMotion`** (the spec's §12 proposal, adopted), with values {Running, Settling, AtRest}, in the `PegCountKey` idiom on the backdrop root only. AtRest covers animator scale 0, battery saver, a settled clock and Sparse. It makes BD-16 assertable without counting frames. TalkBack reads none of it.

### A3 — The finish: where the glow, gloss, rim and lip live, and `Clay.kt`

**Decision: new files, and `Clay.kt`'s `clayShadow` is not touched.**
- `ui/theme/FinishColors.kt`:
  - `@Immutable data class FinishColors(panelRim, panelLip, panelGloss, panelGlow)`, its light and dark instances, and `LocalFinishColors`, provided by `AppTheme` beside `LocalClayColors` and `LocalBackdropColors`;
  - `@Immutable data class GelColors`, one theme-independent instance per tile (`GelColors.Violet`, `GelColors.Pink`: top, mid, bottom, glow), plus the shared gel tokens (on-gel, badge, chip, chip text, art peg, art hole, rim, lip, sheen). These need no `CompositionLocal`, because they do not vary with the theme.
- `ui/theme/FinishRecipe.kt`: a pure table. `enum class FinishedContainer { BoardSlab, StageArt, DockSlab, BackChip, TimerPill, GearChip, RibbonPill, ContinueChip, RescueBanner }` maps to `FinishRecipe(rim, lip, gloss, glow)` exactly as spec §3.5.1's table. `FinishRecipeTest` asserts it in the `DockLookRecipeTest` idiom.
- `ui/theme/Finish.kt`, the two modifiers:
  - `Modifier.finishGlow(elevation, shape, tint = LocalFinishColors.current.panelGlow)`: `shadow(elevation, shape, ambientColor = tint, spotColor = tint)`, the same geometry as `clayShadow` with the tint swapped (spec §3.5.1 Glow). It replaces `clayShadow` at exactly the call sites the recipe marks for a glow, and nowhere else.
  - `Modifier.glossFinish(container, shape, fill: Color? = null)`: one `drawWithCache` that builds the gloss brush, the rim stroke and the lip band from the shape's outline once per size, layout direction and theme. Its `onDrawBehind` draws fill (when given), gloss (when the recipe has one), rim, lip. It adds no semantics, no size, no padding, no target and no click.

**Placement, the one binding rule: the finish draws after the container's fill and before its content.**
- **A `Box` with `.background(fill, shape)`:** keep that `.background` exactly where it is, and put `.glossFinish(container, shape)` immediately after it in the chain. A draw modifier after `background` draws after it and before the children. Keeping `.background` keeps the node's `Shape` semantics, which `HomeStageTest` reads on the stage art.
- **An M3 `Surface` or `Card`:** these draw their fill inside, after the passed modifier. Pass `Color.Transparent` as the container colour and give the finish the same token, `.glossFinish(container, shape, fill = token)`. The pixels are the same outline fill, drawn one step earlier.
- **Never** draw the gloss over content, and never as `drawWithContent` after the children. The gloss lies under the chips' text.

**Who keeps `clayShadow`:** the rescue banner (its shadow is unchanged), the dock's discs, every Sparse screen's containers, the tutorial slab, every dialog and snackbar. Nothing else in `Clay.kt` changes for the finish.

**The collision question.** The producer's order merges the audio branch, wrong-tap included, before this branch is built, and this branch merges `develop` in first. `Clay.kt` is therefore no longer a parallel collision; its wrong-tap edits are already in the base.
- The finish would stay out of `Clay.kt` anyway. It is a two-screen concept, and `clayShadow`'s other callers (16 files on phase 15) must not change.
- **The one `Clay.kt` edit on this branch is the Home title fix** (A9): a `titleGradient` field in `ClayColors` with its two instances, and `titleGradientBrush()` beside `ctaGradientBrush()` with its own two stop constants (colour-vision.md TG-01, TG-AC01). It is purely additive.
- **If the wrong-tap half is split off and has not merged when this branch starts,** `Clay.kt` becomes R6's fifth shared file. Keep the title edits additive, in one place beside `ctaGradientBrush()`; whichever branch merges second resolves and re-runs the full Check.

### A4 — The gel tile's container, its pressed state, and the flat footprint

**The container stays an M3 `Card(onClick)`.** Everything spec §3.5.2 does not name stays as built: `shapes.large`, `mode_tile_min_height`, the 16dp row padding, `clayEntrance`, `claySquish`, `withClickSound(ButtonForward)`, the semantics and the announcement.
- `colors = CardDefaults.cardColors(containerColor = Color.Transparent, contentColor = GelColors.onGel)`.
- `clayShadow(elevation_card)` becomes `finishGlow(elevation_card, shapes.large, tint = gel.glow)`.
- **The gel is drawn by `Modifier.gelSurface(gel, shape)`** (`ui/home/GelTile.kt`), on the `Card`'s modifier after the glow. One `drawWithCache` builds, once per size and direction:
  - the fill's vertical gradient (top, mid at `gel_top_band`, bottom);
  - the rim, from the shape's outline inset by half `gel_rim_width`, so its outer edge is the tile's edge;
  - the gel lip's rounded band and its fading brush;
  - the sheen band's path and brush, with the lip path kept for its clip.

  Its draw block draws fill, rim, lip, then, only when the derived sheen value is not `SHEEN_HIDDEN`, the sheen translated by the pose and clipped to the cached lip path. That `clipPath` is the one clip allowed in a draw lambda on this branch (E9): two small regions, for 2.5 s of every 10 s. Because the `Card` paints nothing, all of it lies under the content.
- **The pressed and focused state layer darkens:** wrap each tile's `Card` in `CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(color = gel.bottom))`. The `Card`'s own ripple then uses `GelBottom` at M3's default pressed and focus alphas, and pressing never lightens the gel.
  - If the pinned material3 marks that API experimental, its opt-in is named in the developer's report for the code-auditor to approve or reject. It is never a `@Suppress`.
  - Replacing `Card(onClick)` with a `clickable` is not allowed: it would change the role and the announcement that BD-15 pins.
- **The content:** title and supporting line in `onGel`; the badge `Surface` filled `GelBadge` with its glyph in `onGel`; the level chip `GelChip` / `GelChipText`; the Challenge bolt tinted `GelArtPeg`.
- **New composables go in new files.** `HomeScreen.kt` already hit detekt's per-file function count once (`CONTENT_WEIGHT`'s KDoc).

**The flat footprint.** `GelFootprintArt(board, modifier)` in `ui/home/GelFootprintArt.kt` replaces `StaticBoardArt` in the Classic tile only.
- It is one `Canvas` at `home_tile_art_size`. It uses `BoardGeometry` read-only for the cell centres and `BoardMetrics`' peg and hole radius ratios, and draws flat discs: pegs `GelArtPeg`, holes `GelArtHole`, with no shading, specular or dimple.
- `BoardCanvas.kt` and `BoardGeometry.kt` are not edited. BD-17 needs the board untouched, and both files belong to the wrong-tap work and to the colour-blind palette.
- `StaticBoardArt`'s node carries no semantics today (`BoardCanvas` sets none), so `GelFootprintArt` carries none either.
- `StaticBoardArt` stays for the stage art and the catalogue previews.

### A5 — The idle-frame and battery ruling (B-22): accepted, with amendments

**Accepted as the default:** the lifecycle rule, a settle after idle (60 s on Home, 20 s on the game screen, a 2 s `FastOutSlowIn` ease), a wake on input, a wake on navigation, and battery saver as the rest pose with no clock. Those numbers are motion-feel values the ux-designer set. Technically they are sound: the screen's own timeout and `onPause` stop the clock in any case, and the settle bounds an idle game screen to the timer's ticks.

**Amendments.**
1. **RESUMED alone.** Step 1's "and its window is visible" adds no signal: on API 29+ every visible activity is resumed in multi-window, and a covered one is paused. The clock follows `repeatOnLifecycle(RESUMED)` only (A2).
2. **A resume counts as input.** Returning to the app wakes the clock with the 2 s ease and restarts the idle timer, so a player coming back never finds a frozen screen that a tap would then start.
3. **Where the observer lives:** on the host `Box` in `AppNavHost` that is the parent of the `NavHost` and `NavBackdrop`, as `Modifier.backdropInputObserver(clock)` (`ui/backdrop/BackdropInput.kt`):
   - `pointerInput(clock) { awaitPointerEventScope { while (true) { if (awaitPointerEvent(PointerEventPass.Initial).type == PointerEventType.Press) clock.onInput() } } }`. It reads at the `Initial` pass and never consumes.
   - `onPreviewKeyEvent { if (it.type == KeyEventType.KeyDown) clock.onInput(); false }`. It always returns false.
   - A dialog's own window does not reach it, which the spec accepts. TalkBack's accessibility actions do not either, so for a TalkBack user the backdrop settles, the calmer state (accepted as the spec reasons).
4. **Battery saver mechanism.** `rememberPowerSaveMode(): State<Boolean>` (`ui/backdrop/PowerSaveMode.kt`):
   - it reads `PowerManager.isPowerSaveMode` once;
   - it follows `ACTION_POWER_SAVE_MODE_CHANGED` through `ContextCompat.registerReceiver(…, RECEIVER_NOT_EXPORTED)`, registered and unregistered in a `DisposableEffect`;
   - it is composed only inside the scene-ready-gated composition, so the read happens from frame 2, never on frame 1 (spec §7);
   - while it is true the clock does not run and the rest pose is drawn, exactly the animator-scale-0 path. Turning saver on while the scene moves jumps it to the rest pose once. This is accepted: it is rare (the shade closing), and one static path is simpler to prove than a second settled one.

**The frame-rate cap: not built.**
- A hand-rolled frame skip is rejected: it judders and still wakes on every vsync.
- If the pinned Compose UI offers a stable per-node frame-rate hint, the `compose-performance-audit` may apply the normal (≈ 60 Hz) category to the scene node, as a one-line change backed by its numbers. Nothing is built in advance.

**What it costs while moving** (spec §6.3, confirmed): one scene node re-records its display list each frame. That is 6 wave draws and up to 9 discs on Landscape, from cached paths and brushes, with no recomposition and no allocation. The sky's own node is not re-recorded.
- One node for L2 and L3, not two (the spec left it to this role): both read the same clock on the same frames, and a second node doubles the invalidations for nothing.
- Promoting each element to its own layer is an audit-time option backed by measurement, not an up-front design.

**BD-16 (c)'s pass line under this ruling.** After the idle delay plus 2 s with no input, `backdropMotion` is AtRest. Over the next 10 s, the `dumpsys gfxinfo` frame count is no higher than `develop`'s for the same screen and state: gameplay, the timer's ticks only; Home, expected near zero against `develop`'s ≈ 600 from the orbs. One tap restarts the motion within 2 s and reaches its target unchanged. In battery saver, `backdropMotion` is AtRest, the rest pose is drawn, and the 10 s frame count is the same as at scale 0.

### A6 — The first-frame gate under B

**R3's mechanism stands unchanged:** one flag, `withFrameNanos { }` and then `ready = true` in a `LaunchedEffect` in the host, provided as a `State<Boolean>` through `LocalBackdropSceneReady`.

**What it holds back under B:**
- the L2/L3 scene node: the waves and lights are composed, built and drawn from frame 2, or frame 3 at the latest;
- the clock's start: no time, no sheen, and `backdropMotion` AtRest on frame 1;
- `rememberPowerSaveMode()`: no system service and no receiver on frame 1.

**Frame 1 holds:** the window background, the host `Surface`, the L1 sky gradient, and Home's content with its containers' finishes (gel fills, rims, lips, glosses, glow tints) and the new title gradient.
- The finishes are on frame 1 by design (B-11a): a gel tile that turned from flat to deep on frame 2 would pop. Each finish is a gradient brush, a 1dp stroke and a small path, built once in a cache block: the class of cost the bar allows.
- The title gradient changes colours, not cost: it uses the same brush geometry as today.

**BD-23 (a), made exact under B:** after the host's first frame, `backdropLayers` = {Sky}, `backdropMotion` = AtRest, and the clock has requested no frame. Within two `advanceTimeByFrame()` calls, `backdropLayers` = {Sky, Waves, Lights}.

**The degradation path, if 15e fails:** spec §7's steps 1–3, then §6.5's steps 1–4, re-measuring the whole sitting after each step and stopping at the first PASS. Each step is a one-line change on the flag; none is built in advance. A failure that survives step 3 is not the backdrop's, and this role rules on it with the numbers before any art is removed.

**15e on this branch:** R3's procedure, with tech design §10's current instrument, `Pixel_9a_bench`, and its start-path list.
- **Arm A** = `git merge-base develop feature/candy-backdrop` at the sitting. It contains phase 15, check-gates, the audio branch and F-1.
- **Arm B** = the branch head.
- **Validity:** the sitting is valid only while both arms share the Room schema and the DataStore keys. This branch adds neither, which is one reason the colour-blind palette is not on it (A9).

### A7 — Fit with D-03l clause 4 and the phase-15 suites (tech design §16 #16)

**Verdict: FIT, under the conditions below.** Direction B adds no child, band, target, readout or semantics node anywhere on Home. It changes fills, draws and shadow tints only.

1. **The stage (clause 4).** The stage's one weighted child is unchanged: its weight, its edge formula and its art (D-03g's one `BoardCanvas` draw).
   - The art gains `finishGlow` in place of `clayShadow`, and `glossFinish(StageArt, shape)` right after its existing `.background(fill, shape)`: a rim and a lip, no gloss.
   - Both are draw-only, so 15r's guard (`theStageAndItsArtCarryNothingButTestTags`: every key minus `Shape` is exactly `TestTag`) holds unedited, and the art keeps its `Shape` key.
   - Nothing stands on or beside the stage (B-12a). No `testTag` for wings exists anywhere.
2. **The gel tiles.** They are fixed bands, not the stage, so clause 4 does not reach them. The gel changes the fill, content colours and art, never the size, padding, shape, entrance or semantics.
   - 15p's gap readings between named elements do not move.
   - The tile subtree's semantics are unchanged: `GelFootprintArt` carries none, as `StaticBoardArt` carried none.
   - The Card's role and click label are unchanged (A4).
3. **The chips and pills.** Their finish is draw-only, in place, with no layout change.
4. **The game screen.** The board slab keeps `surfaceContainerHigh`; its rim and lip lie inside BD-17's 2dp margin. The dock slab keeps its flat fill around the discs, and the discs and on mark are untouched, so 21a–21g hold.
5. **Every phase-15 suite runs unedited.** A failure is a defect in this branch. If a suite samples pixels within 2dp of a finished container's edge, and so reads the rim or the lip, that is a conflict between two specs: the developer stops and reports it to the producer for the ux-designer. The developer never edits the test or retunes a value.

### A8 — The research-analyst's conditions as build constraints

The direction-B addendum (`research-analyst.md` A.1–A.8) is **VERDICT: clean** on A.3 and A.4. That is the one-line confirmation BD-28 asks for: waves, lights and gel cards are each "Clean" in A.1, on those conditions. BD-28 is therefore met as a document. What remains is that the build honours every line below, which the code-auditor checks against the diff and QA's captures.

| # | Constraint (A.3, A.4) | How the build holds it | Checked by |
|---|---|---|---|
| C1 | Lights are bokeh, not bubbles: filled discs fading at the edge, centre alpha ≥ half the rim alpha; no ring, stroke, dot, crescent, fizz, pop or burst; never emerging from a crest | The light is one radial-gradient disc (core, rim, transparent) with no stroke; lights rise from below their rest centres with envelope 0 at the wrap, and the model has no crest-attached spawn | `BackdropColorsTest` (each kind's core alpha ≥ ½ rim alpha, both themes; every token passes today, e.g. light Pink 0.30 ≥ 0.275), Review |
| C2 | Waves are light, not liquid: one lip and one sheen per wave, a bob only (translate), no drips, droplets, splash, foam or wobble; never driven by game state; never in, over or through the slab; nothing in the app is shown as liquid in a bottle, tube or meter | `buildSceneModel` takes no game input, so no game state can reach a wave; poses are translation only; the slab is opaque and above every layer | Review (signature and draw code), BD-16 (b), BD-17 |
| C3 | No pink jelly band: in light, the wave body stays lilac to violet below its pink crest; no opaque hot-pink band with streaks at the foot; no text on a wave; violet leads; the violet Classic card stays above the pink Challenge card | Tokens only; the tile order is phase 15's | `BackdropColorsTest`: light `WaveBody` and `WaveDeep` hues within 260°–300° (294° and 267° today); Review; QA captures (BD-02) |
| C4 | Gel stays on UI containers: never on pegs, holes, dots, stars or any small token; exactly two gel colours from `Color.kt`; the board and pegs unchanged | `GelColors` has two tile instances; `BoardCanvas.kt` is not edited; the flat footprint is the tile's art, not a token | BD-03, BD-08 (hue arc), BD-17, Review |
| C5 | Gel cards stay cards: opaque rounded rectangles; no twisted ends, stripes, sprinkles, drips, wavy edge or see-through body; no squash-and-wobble beyond `claySquish`; the sheen at most once per 10 s per card | The outline is `shapes.large`; the gel is opaque at every stop; `GEL_SHEEN_PERIOD_MS` = 10 000 | `BackdropMetricsTest`, Review, BD-03 |
| C6 | No translucent coloured layer on board cells; the slab's gloss is at most one lip along its top edge | The slab has no gloss at all (recipe), only a rim and a lip | `FinishRecipeTest`, BD-18 |
| C7 | No multi-hue colour field behind the board | Every new hue in 250°–336° | BD-08 |
| C8 | Words: never user-facing `jelly`, `soda`, `sugar`, `sweet` or the verdict's list; `gel` never as the name of a mode, theme, setting, feature or achievement, and never next to a sweet word; no "jelly" in any identifier, KDoc, test tag or string; "Solitaire" never next to a sweet word or a "twist" phrase | This branch adds no user-facing string. `UserFacingTextHasNoProtectedNamesTest` scans `strings.xml` and `assets/` on every Check for the verdict list plus A.4's four words; the diff grep keeps "jelly" out of the code; store copy and Play Console names stay the release-manager's | BD-01 (Test, Auditor, QA) |
| C9 | §3 still applies: no tile grid, candy lettering, character, or King image in the repository or a design file | No asset is added (BD-04) | BD-03, BD-04, Review |

### A9 — Which branch ships what

| Work | Branch | Why |
|---|---|---|
| **The Home title fix** (colour-vision.md TG-01, TG-02, TG-AC01–TG-AC05) | **this branch** | B-21's lane bounds and TG-AC04's target were derived for the *new* title stops (colour-vision §11.1, §11.3). B without the fix would put the old title over lights it was never checked against. The fix is four additive edits (`Color.kt`, `Clay.kt`, `HeroTitle`), it changes Home's first frame like B does, and one 15e sitting then covers both |
| **The colour-blind palette** (CV-01–CV-13, CV-AC01–CV-AC15) | **its own branch**, `feature/colour-vision` | (1) It adds a DataStore key, `colorblind_palette_on`, and 15e is valid only while both arms share the DataStore keys, so on this branch it would void the sitting. (2) It redraws the board's marks in `BoardCanvas.kt`, which BD-17 requires untouched here and which the wrong-tap work also edits. (3) It changes Settings, the repository and the app root (the §3.3 collection), none of which the backdrop touches. (4) It is a separate review domain (board accessibility, simulated vision) with its own QA matrix (CV-AC11–CV-AC14). It does not depend on the backdrop (colour-vision §10 item 5) |
| **The licences screen** (R4, LS-AC01–LS-AC11) | **its own branch**, `feature/licences-screen` | rules/ip-policy.md asks for attribution "in the product's licenses screen or page", so it is a release need whether or not B reaches 3.0.0 (THIRD_PARTY.md's own row says the backdrop row is deleted if this branch misses the release). On its own branch it is not held by the backdrop's gates (15e, BD-02's capture review, BD-28's conditions). It can be built as soon as phase 15 has merged. It shares no logic with the backdrop, only `AppNavHost` and Settings, and those conflicts are mechanical |

**Consequences on this branch.**
- **BD-04's carve-out moves to the licences branch.** Here, no file under `assets/` at all (E6).
- **LS-AC08, and the licence rows of BD-06 and BD-14, belong to whichever branch lands second** (E7). If the licences branch is already on `develop` when this branch merges it in, this branch makes the two licence `Scaffold`s and `TopAppBar`s transparent and adds `Licenses` and `LicenseText` to `BackdropDensityTest` and `TransparentContainersTest`. If not, the licences branch does so when it merges `develop` in.
- **Settings' entrance orders:** this branch changes no order. The licences and colour-vision branches renumber; whichever lands second renumbers on top of the first (colour-vision §3.1).

**Documents.** `docs/ux/licences-screen.md` and `docs/ux/colour-vision.md` live on this branch today.
- When the producer cuts either branch from `develop`, its first commit brings its own spec and this addendum over with `git checkout feature/candy-backdrop -- <paths>`. Identical additions merge cleanly when this branch lands.
- The licences branch also brings `tech-lead-architecture.md`, whose R4 and R5 rows it builds to.
- The colour-blind palette needs a short brief from this role before its dispatch: colour-vision §10 item 3, how the setting reaches `AppTheme` once at the root, is still owed. That is the only design question its spec leaves.

### A10 — The test split

R5's rules hold for every row: the Compose test clock (`mainClock.autoAdvance = false`, `advanceTimeByFrame()`), insets from `testing/SystemBarInsets.kt`, `renderedWindow()` under `@GraphicsMode(NATIVE)` on Robolectric 4.16 at SDK 34, names that read as sentences, each test watched failing once, and no stored goldens. The kinds: **JVM**, **Robo**, **Compose**, **Pixel** (two renders compared inside one run), **Device** (the qa-engineer), **Review** (the code-auditor).

**Clock seams for tests.** A test that needs a fixed pose provides `FixedBackdropClock(t)` (pixel tests, previews). A test that needs the loop to run builds the host with an injected `BackdropFrames` driven by the test clock, and counts its frame awaits, so "requests no frames" is an exact assertion. Battery saver goes through Robolectric's `ShadowPowerManager` and a sent `ACTION_POWER_SAVE_MODE_CHANGED`.

**The backdrop, this branch**

| # | Kind | Test or check | Notes |
|---|---|---|---|
| BD-01 | JVM + Review + Device | `UserFacingTextHasNoProtectedNamesTest` over `src/main/res/values*/strings.xml` and `src/main/assets/**`: the verdict's list plus A.4's `jelly`, `soda`, `sugar`, `sweet`, whole words; the three spec greps (Auditor, QA) | `gel` as a name and `docs/store` are Review and release-manager matters |
| BD-02 | Device + Review | the two questions on QA's captures | the auditor's line follows QA's capture session |
| BD-03 | Review + JVM | §10 and A8's table on the code and captures; `FinishRecipeTest` as the auditor's aid | — |
| BD-04 | Review | `git diff --stat develop...HEAD`: nothing under `res/drawable*`, `res/mipmap*`, `res/raw`, `assets/`; no image; no catalogue or build-file entry | no carve-out on this branch (E6) |
| BD-05 | Check + Review | `dpSpLiteralCheck`, detekt, ktlint; grep of new files for `0x`, `.dp`, `.sp` | durations in `ClayAnim` or `BackdropMotion` |
| BD-06 | Compose | `BackdropDensityTest`: the real `AppNavHost` with `NavFlowTest`'s stand-ins through every destination, reading `backdropDensity`; R5's restore case (top `Gameplay` → Quiet on frame 1) | licence rows per E7 |
| BD-07 | Compose + Robo + Review | `backdropLayers` per density on the three reference windows and Home's three chip states; light counts 9/5/0 from the model; a grep that no wing tag or stage-wing node exists | — |
| BD-08 | JVM | `BackdropColorsTest` over `Color.kt`: every §4.1 constraint, plus C1's alpha clause and C3's hue clause | — |
| BD-09 | Pixel + Device | the backdrop alone, insets supplied: every pixel of `[0, statusInset)` equals `background` | ±1 per channel only as a named constant, if the native canvas dithers |
| BD-10 | JVM | `BackdropMetricsTest`: §3.3, §4.3, §4.4, list lengths, every loop ≥ `MIN_LOOP_MS` | — |
| BD-11 | Robo + JVM | `BackdropGeometryTest` on the builder with `BackdropDimens` from real `Resources` at the four windows ((a)–(f), (h): `gel_top_band` ≤ `spacing_md`, (i)); `BackdropPoseTest` on the pose functions ((g): rest poses, wrap at envelope 0, speeds ≤ 8dp/s, `Long` reduction exact at t = 10⁹ ms) | (a)–(c) proved exactly, since lights move only vertically |
| BD-12 | Device | §5.1's texts at rest and in motion | the computed half is BD-08 |
| BD-13 | Device | edges against §5.2's floors | the computed floors are BD-08 |
| BD-14 | Pixel + Compose | `TransparentContainersTest`: each stateless screen in `BackdropTestHost` at its density, light and dark; an empty region equals the backdrop's own pixel; a probe in `AppNavHost`'s slots asserts `LocalContentColor` = `onBackground` (F-32) | licence rows per E7 |
| BD-15 | Compose + Device | `BackdropSemanticsTest`: the root carries tag, `backdropDensity`, `backdropLayers`, `backdropMotion` and nothing actionable; the gel tiles' semantics equal the clay tiles' (the expected config written in the test from `develop`); the existing traversal tests run unchanged; QA's TalkBack pass | — |
| BD-16 (a) | Compose + Device | `BackdropReducedMotionTest`: animator scale 0 → `isStatic`, rest pose, `backdropMotion` AtRest, zero frame awaits; QA: `gfxinfo` 5 s idle ≤ `develop` on Home, gameplay and a Sparse screen | — |
| BD-16 (b) | JVM + Review + Device | `BackdropPoseTest`: poses vary translation and alpha only; one frame source (Review: in `ui/backdrop/` and `ui/home/Gel*`, the only frame awaiters are the clock's loop and the scene-ready gate); nothing reads scroll state (Review) | QA's in-motion captures |
| BD-16 (c) | Compose + Device | `BackdropSettleTest`, injected frames: AtRest after delay + 2 s and no further awaits; a tap at `GameScreen`'s board (a stand-in `onCellTap` recorder) wakes it to Running and still reaches the board; navigation wakes; resume wakes; battery saver → AtRest and the rest pose; QA: A5's pass line | — |
| BD-17 | Pixel | `BoardInteriorIdentityTest`: `GameScreen` mid-game (selection, dots, hint) with and without the backdrop, the slab's interior inset 2dp identical; the same for the stage art, the gel tiles (`FixedBackdropClock` outside the sheen pass) and the Continue chips | — |
| BD-18 | Pixel + Review + JVM | a slab-interior pixel equals `surfaceContainerHigh`; `FinishRecipeTest`: no gloss on the slab | — |
| BD-19 | existing suites + Device | 15g, 15j, 15k, 15n–15r, 18a–18e, 21a–21g green, **none edited**; QA: 18b's insets, the three-button scrim, the gesture handle | A7.5 |
| BD-20 | Pixel | `BackdropFontScaleTest` at 1.0 and 2.0, same window, `FixedBackdropClock` | — |
| BD-21 | Pixel + Device | `SkyContinuityTest`, R5's convex-blend form, `FixedBackdropClock`, sampled in the calm band and between the band and the back crest | QA's recording |
| BD-22 | Pixel + Device | `BackdropMirrorTest`: LTR against mirrored RTL at a fixed time under a named tolerance that fails on a deliberately unmirrored scene; QA in `ar-XB` | — |
| BD-23 (a) | Compose | `BackdropFirstFrameTest` on the real host: A6's exact form | — |
| BD-23 (b), (c) | Device | 15e on `Pixel_9a_bench` (A6); then the separate janky-frames block, animations on | R3, D8 |
| BD-24 | Device | greyscale captures | — |
| BD-25 | Review | light and dark `@Preview`s on every new public composable; screen previews through `BackdropPreviewHost` with `FixedBackdropClock` | — |
| BD-26 | Review + release-manager | on `release/3.0.0` | A.5's row text |
| BD-27 | Compose + Pixel + Device | `GelTileTest`: at font scale 2.0 the title's top bound ≥ `gel_top_band` below the tile's top (semantics bounds); pressed: a tile pixel's luminance falls against its unpressed render, in-run; QA at rest, mid-sheen, pressed | — |
| BD-28 | Review | met as a document by the addendum's VERDICT; the auditor checks A8's C1–C9 on the build | A8 |

**The Home title, this branch.** `TitleGradientContrastTest` lives here and reads `BackdropColors`.

| # | Kind | Test or check | Notes |
|---|---|---|---|
| TG-AC01 | Review | the diff shows exactly colour-vision §4.3's four edits | — |
| TG-AC02 | JVM | `TitleGradientContrastTest`: both stops ≥ 4.5:1 on `background` in both themes; monotone channels; B's zone-T sky tokens; light ≥ 3:1 against every lane light's rim composite at 0.47 H | as amended in §11.3 |
| TG-AC03 | Device | glyph sampling at 1.0 and 2.0 | — |
| TG-AC04 | JVM + Device | BD-08 and BD-11 prove it by construction; QA samples the wrapped dark title at 2.0 on 360×640dp | — |
| TG-AC05 | JVM + Review + Device | the goldens are struck (D4): a JVM test pins `ctaGradientBrush()`'s colours and stops to the values written in the test from `develop`; Review: only `HeroTitle` switched; QA compares the CTA buttons against `develop` | "identical with the palette on and off" holds structurally: the title reads no palette |

**The colour-blind palette, its own branch.** D4 applies there too: no Roborazzi, no stored goldens.

| # | Kind | Notes |
|---|---|---|
| CV-AC01 | Compose + Device | `SettingsScreenTest`'s rendered-top order; TalkBack; `ButtonSoundTest` idiom for no cue |
| CV-AC02 | JVM + Device | `SettingsRepositoryTest` with in-memory and throwing stores |
| CV-AC03 | Compose + Device | the board's palette semantics property |
| CV-AC04 | Review + Check | — |
| CV-AC05 | JVM + Pixel + Device | goldens struck: a JVM test pins the Standard palette's values to today's constants (written in the test); in-run Pixel, on against off differ only inside the slab; QA compares off against `develop` |
| CV-AC06 | Pixel + Review | Home with the palette on and off, in-run, identical; Review for no first-frame read |
| CV-AC07, CV-AC08 | JVM | the title clause of CV-AC07 lives in `TitleGradientContrastTest` on this branch |
| CV-AC09 | Compose + Device | — |
| CV-AC10 | existing suites + Compose + Device | — |
| CV-AC11–CV-AC14 | Device | — |
| CV-AC15 | Review | — |

**The licences screen, its own branch.** R5's LS-AC01–LS-AC11 rows stand verbatim, with R4's design. LS-AC08 is owned per E7. BD-04's Apache-2.0 carve-out (sha256 `cfc7749b…3d30`) applies there.

### A11 — Sequencing under the producer's merge order (R6 amended)

- **This branch builds after phase 15, check-gates, the audio branch and F-1 are on `develop`.** The developer merges `develop` in before its first commit (brief §1). The branch carries docs only, so nothing conflicts.
- **R6's parallel conditions 1–4 lapse for the audio work**, which merges first. Its edits to `GameScreen.kt`, `TutorialScreen.kt`, `dimens.xml`, `strings.xml` and `Clay.kt` are in the base. Their start-path work lands in arm A, so 15e measures the backdrop alone (R6 condition 3's reasoning).
- **check-gates C7 is in the base.** `:app`'s main compilations are warning-free and gated, so a deprecation or an unchecked opt-in is a Check failure, never a `@Suppress`.
- **The licences branch should land before this branch's 15e sitting.** It edits `navigation/`, a start-path directory, so merging it in after the sitting forces a re-sitting (tech design §10).
- **The colour-blind palette** edits `ui/theme/`, also on the start-path list. It lands either before this branch's sitting or after this branch merges, never in between. It also needs its own 15e sitting, since it adds a DataStore key and a read started at the root. The producer schedules.
- **The host stays quiet:** no Gradle build anywhere during any 15e sitting, this addendum's own dispatch included. The qa-engineer's H-3 sitting was running when this was written.

### Where this addendum departs from the third edition (E1–E12)

The developer builds to this addendum where the two differ. The ux-designer folds these into the spec, non-blocking.

| # | Spec text | This addendum | Why |
|---|---|---|---|
| E1 | B-24: "how the clock reaches the tiles … is the tech-lead's call" | `LocalBackdropClock`, a static `CompositionLocal` from the host; the tile reads a `derivedStateOf` sheen value in draw (A2) | the tile is invalidated only during the 2.5 s pass |
| E2 | §6.3 step 1: "RESUMED and its window is visible" | RESUMED alone (A5.1) | visibility adds no signal on API 29+ |
| E3 | §6.3: input wakes the clock | A resume wakes it too (A5.2) | no frozen screen on return |
| E4 | B-24: "time scaled by the animator duration scale" | Only the backdrop time scales, through `MotionDurationScale`; idle, settle and wake run on unscaled frame time (A2) | delays the ux-designer set in seconds stay seconds |
| E5 | §12: `backdropMotion` "proposed for the tech-lead" | Adopted: {Running, Settling, AtRest}, on the backdrop root only (A2) | BD-16 without frame counts |
| E6 | BD-04: the Apache-2.0 carve-out | No file under `assets/` on this branch; the carve-out travels with the licences branch (A9) | the licences screen moves |
| E7 | BD-06, BD-14: the licence routes | Owned by whichever of the two branches lands second (A9) | either may land first |
| E8 | §3.5.1, §14 item 3: the glow "a parameter, a variant or a provided palette" | `finishGlow` and `glossFinish` in new files, with the recipe table; `Clay.kt` edited only by the title fix (A3) | `clayShadow`'s other callers stay untouched |
| E9 | R1: no `clipPath` in a draw lambda | Except the gel sheen's clip to its cached lip path (A4) | two small regions, 2.5 s in 10 s |
| E10 | §6.3 step 6: a frame-rate cap "may" be applied | Not built; a stable per-node hint only at the audit, with numbers; no frame skip (A5) | a skip judders and still wakes every vsync |
| E11 | L2/L3 "one node or two" | One scene node; per-element layers only at the audit (A5) | one clock read, one invalidation |
| E12 | §4.1 "the gel tokens … one value each" through "immutable palette holders provided by `AppTheme`" | Themed finish tokens through `LocalFinishColors`; the theme-independent gel tokens as a plain `GelColors` holder with no `CompositionLocal` (A3) | a local for constants buys nothing |

## The developer brief — `feature/candy-backdrop`, direction B

For: the android-game-dev, dispatched by the producer, in the worktree `/Users/ermanergoz/Desktop/PegSolitarie-candy`. Paths are under `app/src/main/java/com/app/pegsolitaire/` unless named.

### 1. Before the first commit
1. Confirm the base. Each of these exits 0: `git -C <worktree> merge-base --is-ancestor <merge commit> develop`, for phase 15, check-gates, the audio branch and F-1 (the producer names the four commits in the dispatch). If one is missing, stop and report.
2. Merge `develop` into `feature/candy-backdrop` with `--no-ff`. The branch holds docs only, so expect no conflict.
3. Load your required skills: `compose-skill` first (rules/compose.md), then `compose-ui`, `android-viewmodel`, `android-coroutines`, `android-testing`, and `superpowers:test-driven-development`.
4. Run the Check once on the merged base, and record the result as your baseline. Ask the producer before any Gradle run while a 15e sitting may be on the host.

### 2. What governs, in this order
1. This report's A0–A11 and E1–E12.
2. `tech-lead-architecture.md` R1, R3, R5, R6 and D1–D11, as A0 amends them. R2 is void; R4 is not this branch's.
3. `docs/ux/candy-backdrop.md`, third edition: every value, token, table, pose formula and criterion.
4. `docs/ux/colour-vision.md` §4.3, §5.2 and §11, for the Home title fix only.
5. `research-analyst.md` A.3 and A.4, as A8's C1–C9.

**Not this branch:** the colour-blind palette (CV-*), the licences screen (R4, LS-*), `THIRD_PARTY.md`, `docs/ux-spec.md`, `docs/ui/DESIGN-SYSTEM.md`, `MainActivity.kt`, `BoardCanvas.kt`, `BoardGeometry.kt`, `strings.xml` (B adds no string), `gradle/libs.versions.toml` and every build file.

### 3. Files

| File | New / edited | Holds |
|---|---|---|
| `ui/backdrop/BackdropDensity.kt` | new | `enum class BackdropDensity { Landscape, Quiet, Sparse }`, and whether a density moves |
| `ui/backdrop/BackdropMetrics.kt` | new | spec §4.3 verbatim (sky stops, calm ends, `TITLE_REACH`, spinner band, wave shape and placement, light tables as `@Immutable` entries, finish and gel ratios), `BACKDROP_Z_INDEX` |
| `ui/backdrop/BackdropMotion.kt` | new | spec §4.4's durations, idle and settle constants, `MIN_LOOP_MS` |
| `ui/backdrop/BackdropDimens.kt` | new | `@Immutable` px holder of the backdrop's §4.2 tokens, and `rememberBackdropDimens()` |
| `ui/backdrop/BackdropGeometry.kt` | new | `buildSceneModel(…)` and the model types (A1); no `android.*`, no Compose graphics |
| `ui/backdrop/BackdropPoses.kt` | new | the pure pose functions (A1) |
| `ui/backdrop/BackdropClock.kt` | new | `BackdropClock`, `BackdropMotionState`, `BackdropFrames`, `rememberBackdropClock`, `LocalBackdropClock`, `StaticBackdropClock`, `FixedBackdropClock` (A2) |
| `ui/backdrop/BackdropInput.kt` | new | `Modifier.backdropInputObserver(clock)` (A5.3) |
| `ui/backdrop/PowerSaveMode.kt` | new | `rememberPowerSaveMode()` (A5.4) |
| `ui/backdrop/BackdropSceneReady.kt` | new | R3's flag, unchanged |
| `ui/backdrop/SceneBackdrop.kt` | new | the stateless renderer: R1's two slots, the L1 sky node, one L2/L3 scene node reading the clock in draw, the semantics hook; `@Preview`s per density, light and dark |
| `ui/backdrop/BackdropPreviewHost.kt` | new | `internal` preview host: the `Surface`, `SceneBackdrop`, scene ready, `FixedBackdropClock` |
| `navigation/BackdropDestinations.kt` | new | `NavDestination?.backdropDensity()` and `NavBackdrop(navController, modifier)` (R1) |
| `ui/theme/BackdropColors.kt` | new | 16 tokens per theme, `LocalBackdropColors` |
| `ui/theme/FinishColors.kt` | new | `FinishColors`, `LocalFinishColors`, `GelColors` (A3) |
| `ui/theme/FinishRecipe.kt` | new | `FinishedContainer` → `FinishRecipe` (A3) |
| `ui/theme/Finish.kt` | new | `finishGlow`, `glossFinish` (A3) |
| `ui/home/GelTile.kt` | new | `Modifier.gelSurface(gel, shape)` and the tile's ripple provider (A4) |
| `ui/home/GelFootprintArt.kt` | new | the flat footprint (A4) |
| `ui/theme/Color.kt` | edited | the backdrop, finish, gel and title-gradient values; the only place a hex appears |
| `ui/theme/Theme.kt` | edited | provides `LocalBackdropColors` and `LocalFinishColors` |
| `ui/theme/Clay.kt` | edited | the title fix only: `titleGradient` in `ClayColors`, `titleGradientBrush()` and its two stop constants (A3) |
| `ui/theme/ClayAnim.kt` | edited | `NAV_TRANSITION_MS = 700`; `BLOB_DRIFT_MS` and `BLOB_ALPHA` removed |
| `navigation/AppNavHost.kt` | edited | host `Surface(background, onBackground)` → providers (scene-ready, clock) → `Box(Modifier.backdropInputObserver(clock)) { NavHost(…the four explicit fades on NAV_TRANSITION_MS…); NavBackdrop(…, Modifier.matchParentSize().zIndex(BACKDROP_Z_INDEX)) }`, with R1's KDoc on why the order matters |
| `ui/home/HomeBlobs.kt` | deleted | — |
| `ui/home/HomeScreen.kt` | edited | `HomeBlobs()` removed; transparent `Scaffold`; `ModeTile` takes the gel (A4); the Continue chips' finish |
| `ui/home/HomeHero.kt` | edited | the gear chip's finish; `HeroTitle` uses `titleGradientBrush()` |
| `ui/home/HomeProgressRibbon.kt` | edited | the pills' finish |
| `ui/home/HomeStage.kt` | edited | the art: `finishGlow` and `glossFinish(StageArt, …)` after its `.background` (A7) |
| `ui/game/GameScreen.kt` | edited | transparent `Scaffold`; the slab's rim, lip and glow |
| `ui/game/GameStatusStrip.kt` | edited | the back chip and timer pill: full finish |
| `ui/game/GameActionDock.kt` | edited | the dock slab's rim, lip and glow; the discs untouched |
| `ui/game/NoWinBanner.kt` | edited | rim and lip; its `clayShadow` kept |
| `ui/classic/ClassicCatalogScreen.kt`, `ui/results/ResultsScreen.kt`, `ui/settings/SettingsScreen.kt`, `ui/onboarding/TutorialScreen.kt` (and the licence screens if they are on `develop`, E7) | edited | R1's container colours only |
| `res/values/dimens.xml` | edited | one `<!-- Backdrop -->` block, spec §4.2's 24 tokens; the three sub-4dp ones join the documented stroke exception |
| `app/src/test/…/testing/BackdropTestHost.kt` | new | the production host's shape for tests: the `Surface`, `SceneBackdrop` at a density, scene-ready controllable, a clock (`FixedBackdropClock` or injected frames) |

### 4. Binding rules
- **Determinism and cost.** Nothing in the backdrop reads `UiState`, a random source, the wall clock, a resource file, a font or the network. Every path and brush is built in a `drawWithCache` cache block, keyed on size, theme, density and direction. Draw lambdas allocate nothing; R1 lists the usual misses, and E9 is the one `clipPath`.
- **The clock** (A2): a single loop is the only frame source; it uses `withInfiniteAnimationFrameNanos` through `BackdropFrames`; `MotionDurationScale` scales time; `repeatOnLifecycle(RESUMED)`; idle, settle and wake run on unscaled frame time; at scale 0 or in battery saver it is static at the rest pose; on Sparse it settles once the cross-fade ends. Time is read only in `onDrawBehind`.
- **Settle and wake** (A5): 60 s on Home and 20 s on gameplay, then a 2 s ease to rate 0, after which no frame is requested; a wake on a pointer-down (Initial pass, never consumed), a key-down, a navigation or a resume, eased over 2 s.
- **The first frame** (A6): the scene node, the clock's start and `rememberPowerSaveMode()` all sit behind the scene-ready flag. The finishes and the title gradient are on frame 1.
- **The finish** (A3): `finishGlow` replaces `clayShadow` only where the recipe marks a glow. The finish draws after the fill and before the content; keep a `Box`'s `.background`, and make a `Surface` or `Card` transparent with the fill handed to the finish. No size, padding, shape, target, semantics or sound changes.
- **The gel tile** (A4): a transparent `Card(onClick)`, `gelSurface`, the `GelBottom` ripple through `LocalRippleConfiguration`, white content, and `GelFootprintArt` for Classic. No white text above `gel_top_band`; the 16dp row padding is a contract (BD-11 (h)).
- **A8's C1–C9 hold everywhere,** including no "jelly" in any identifier, KDoc, test tag or string.
- **Tokens only:** hexes only in `Color.kt`; dp only in `dimens.xml`; ratios in `BackdropMetrics`; durations in `ClayAnim` or `BackdropMotion`. No `@Suppress`, baseline, `@Ignore` or `!!`. check-gates C7 makes any warning a failure. An opt-in API is named in your report for the code-auditor.
- **New composables go in new files** (detekt's per-file function count). Every new public composable gets light and dark `@Preview`s.

### 5. Order of work, test-first
1. **Tokens and pure logic:** `BackdropColorsTest` (BD-08, with C1 and C3), `TitleGradientContrastTest` (TG-AC02), `BackdropMetricsTest` (BD-10), `BackdropGeometryTest` (BD-11), `BackdropPoseTest` (BD-11 (g), BD-16 (b)), `FinishRecipeTest` (BD-03, BD-18), `UserFacingTextHasNoProtectedNamesTest` (BD-01). Then `Color.kt`, the holders, metrics, motion, dimens, geometry, poses and the recipe.
2. **The host:** the F-32 `Surface`, the density mapping, the two-slot cross-fade, the scene-ready gate, the clock, the input observer and power save. Tests: `BackdropDensityTest` with the restore case (BD-06), layers (BD-07), `BackdropSemanticsTest` (BD-15), `BackdropReducedMotionTest` and `BackdropSettleTest` (BD-16), `BackdropFirstFrameTest` (BD-23 (a)), `SkyContinuityTest` (BD-21). After this step, confirm that an unedited phase-15 suite composing `AppNavHost` (`NavFlowTest`) still idles. If it does not, stop and report (§6).
3. **Transparent containers:** `TransparentContainersTest` (BD-14). Delete `HomeBlobs.kt` and its constants.
4. **The finish and the title:** `finishGlow` and `glossFinish` on spec §3.5.1's containers; `titleGradientBrush()` in `HeroTitle`; the TG-AC05 pin test.
5. **The gel tiles:** `gelSurface`, the ripple, the content colours, `GelFootprintArt`, the sheen on the shared clock; `GelTileTest` (BD-27).
6. **Pixel comparisons:** `BoardInteriorIdentityTest` (BD-17, BD-18), `BackdropFontScaleTest` (BD-20), `BackdropMirrorTest` (BD-22), BD-09's band test.

Run the phase-15 suites unchanged after every step (BD-19).

### 6. Stop and report, do not work around
- A phase-15 test fails, or would need an edit to pass: a defect here, or, if it samples a finished edge, a spec conflict (A7.5).
- A spec ratio, bound or count cannot be met by the tokens as given: report the number. Never retune a value; the ux-designer owns them.
- The base lacks one of the four merges, or merging `develop` conflicts outside `docs/` and `.planning/`.
- An API the brief names does not exist, or is experimental, in the pinned Compose BOM 2026.06.00 (`withInfiniteAnimationFrameNanos`, `MotionDurationScale`, `LocalRippleConfiguration`, `repeatOnLifecycle`, `ContextCompat.registerReceiver`).
- Anything needs a new dependency, an asset, a string or a `MainActivity` edit.

### 7. Done
- The Check is green.
- Every automated row of A10 for this branch exists and was watched failing once; no test is disabled or edited to pass.
- Write your report at `.planning/reports/candy-backdrop/android-game-dev.md` (rules/reporting.md), listing every `FinishedContainer` call site and every opt-in, if any.
- Hand off to the producer for the code-auditor (BD-02 after QA's captures), the qa-engineer (A10's Device rows, then 15e and the jank block, R3) and this role's sign-off.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/tech-lead-direction-b.md` — this report: the direction-B addendum to R1–R6 (A0–A11), the departures E1–E12, the test split, the branch split, and the android-game-dev's brief. It closes tech design §16 #16 (A7: FIT).

## Checks
Read for this addendum: spec §1.2, §2–§4, §6–§7 and §11–§16; §5 and §8–§10 only where cited.
- `sed -n` / `awk` over `docs/ux/candy-backdrop.md`, `colour-vision.md`, `licences-screen.md` (headings and criteria), `ux-designer-direction-b.md` (open issues), `research-analyst.md` (A.1–A.8) and `tech-lead-architecture.md` (in full): pass (the inputs this report rules on).
- `sed -n '1069,1124p;271,300p' docs/tech-design.md` on `PegSolitarie-r5` (`feature/phase-14-release-docs`), and a grep of §10: pass. §16 #16 is open and owed before this dispatch; 15e is "valid only while both arms share the Room schema and DataStore keys"; the instrument is `Pixel_9a_bench`. These are the basis of A6 and A9.
- `awk '/fun Modifier.clayShadow/,/^}/' ui/theme/Clay.kt` on `PegSolitarie-p15` (`feature/phase-15-ux-refresh`): pass (`shadow(elevation, shape, ambientColor = tint, spotColor = tint)` with `LocalClayColors.current.shadowTint`; the basis of `finishGlow`).
- `grep -rn 'clayShadow('` over the main sources on `PegSolitarie-p15`: pass (25 lines in 16 files, dialogs, snackbar host and Sparse screens included; the basis of A3's "only where the recipe marks a glow").
- `sed -n '640,740p' ui/home/HomeScreen.kt` on `PegSolitarie-p15`: pass (`ModeTile` is `Card(onClick = withClickSound(…), colors = cardColors(containerColor = container))` with `clayShadow(elevation_card)` and a `spacing_md` row padding; the basis of A4 and BD-11 (h)).
- `grep -n 'semantics\|clearAndSet' ui/components/BoardCanvas.kt` and `sed -n '40,75p' ui/home/HomeBoardArt.kt` on `PegSolitarie-p15`: pass (`BoardCanvas` sets no semantics; `StaticBoardArt` wraps it with `interactive = false`; the Classic art is `Boards.english33Initial`).
- `grep -n` over `HomeStageTest.kt` and `HomeHeroRowTest.kt` on `PegSolitarie-p15`: pass (15r's guard is `keys − Shape` `containsExactly(TestTag)`; `HomeHeroRowTest` names `HomeBlobs` only in KDoc, lines 144 and 310, so deleting it edits no test).
- `grep -n 'LocalRippleConfiguration\|material3\|composeBom' gradle/libs.versions.toml` and the sources: not verified. The BOM is 2026.06.00; that the ripple API is stable there is not proven without a build, so the brief makes it a stop condition (§6).
- `ls "$(git rev-parse --git-common-dir)/hooks"` and `git config core.hooksPath`: pass (samples only, no hooks path; the trailer is written by hand).
- `git -C /Users/ermanergoz/Desktop/PegSolitarie-candy commit` of this report by explicit path: see the commit `docs(candy): rule direction B …` on `feature/candy-backdrop`.
- `./gradlew check`: not run. No code changed, and the host must stay quiet for the qa-engineer's H-3 sitting.

## Open issues / risks
1. **Staffing deviation (Q-019).** These rulings were written on Opus, the developer's model, not on Fable. Mitigation: the code-auditor reviews the build against this report on its own pin. A ruling it finds wrong comes back to this role through the producer.
2. **Three API facts are unverified on the pinned BOM**, because no build was allowed: `LocalRippleConfiguration`'s stability; the test harness cancelling `withInfiniteAnimationFrameNanos` as it does `rememberInfiniteTransition`; per-node frame-rate hints. The brief makes the first two stop conditions (§5 step 2, §6). The third is only an audit option.
3. **120 Hz.** Until the audit rules on a hint, the clock runs at the display's rate while it moves, up to 120 Hz on the Pixel 9. The settle bounds how long it does so.
4. **Battery saver toggled while the scene moves** jumps it to the rest pose once (A5.4). Accepted.
5. **15e re-sittings.** A merge-in of the licences or colour-vision branch after this branch's sitting forces a re-sitting, because both touch start-path directories (A11). The producer orders them around it.
6. **Owed by this role:** the colour-blind palette's brief (colour-vision §10 item 3: the root collection); the tutorial `hapticsOn` ruling (tech design §16 #17, separate); and recording this addendum in `docs/tech-design.md` on `PegSolitarie-r5`: §3.5's B status, §16 #16 closed, §14.1 rows for `feature/licences-screen` and `feature/colour-vision`, and §10's Idle Home row, where O-B is superseded by B-22 and A5. This dispatch was scoped to this report.
7. **Owed by the ux-designer, non-blocking:** folding E1–E12 into `candy-backdrop.md`, and D10/D11 into `licences-screen.md` (still outstanding from the first ruling).
8. **Commit trailer.** The commit carries rules/gitflow.md's single trailer, `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, written by hand, because no `prepare-commit-msg` hook is installed. That rule overrides the harness reminder. `Ran on:` above records the true model.
9. **Q-021** is still not in either FOUNDER.md (the ux-designer's open issue 6). The producer records it.

## Handoff
Next: producer —
1. When phase 15, check-gates, the audio branch and F-1 are on `develop`, dispatch the android-game-dev with "The developer brief" above and the four merge commits. The worktree is `PegSolitarie-candy`.
2. Cut `feature/licences-screen` from `develop` once phase 15 has merged. Its first commit copies `docs/ux/licences-screen.md`, `tech-lead-architecture.md` and this report from `feature/candy-backdrop` (A9). Dispatch the android-game-dev to build R4 with R5's LS rows. Land it before this branch's 15e sitting (A11).
3. Schedule `feature/colour-vision`, and dispatch this role first for its root-collection brief. It needs its own 15e sitting.
4. ux-designer, non-blocking: fold E1–E12 and D10/D11.
5. code-auditor and qa-engineer, at review: A10's split; A8's C1–C9 as acceptance items; BD-02's auditor line after QA's captures.
6. The 15e sitting on this branch (A6), last before the merge, on `Pixel_9a_bench` with the host quiet, then the separate jank block.
7. Tech-lead: record this addendum in `docs/tech-design.md` (open issue 6) when scheduled, and sign off the build after the auditor's and QA's reports.

Next: android-game-dev — build to "The developer brief", in its §5 order.
