# tech-lead report — candy-backdrop (architecture rulings)
Date: 2026-10-17
Branch: feature/candy-backdrop (report only)
Skills loaded: ergoz-soft:standards (role tech-lead; plugin rules v1.0.0 read in full because the product snapshot is v0.6.2: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch, audio; product `.claude/rules/lessons.md`), android-architecture, android-gradle-logic, compose-ui, compose-performance-audit, compose-navigation
Ran in parallel with: phase-15 qa-engineer loop 3, accessibility spec (ux-designer)
Ran on: claude-opus-5-5 at effort xhigh — **staffing deviation:** the tech-lead is pinned to fable; this ran on Opus under the founder's Q-019 answer (2026-10-17: Opus for every Fable-pinned role until 3.0.0 is published), recorded in `PegSolitarie-p15/.planning/questions/FOUNDER.md`
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
Ran on Opus, not the pinned Fable, under Q-019. Six rulings; where they differ from the two specs, they govern (list in "Where this report departs from the two specs").
Backdrop: one `SceneBackdrop` in `AppNavHost` behind the `NavHost`, its density from the committed back-stack entry, cross-fading on the NavHost's own 700 ms, inside the F-32 theme `Surface` the app lacks today. Wings: they fit D-03l clause 4 as a test-tagged stage child, with no custom semantics key.
Cold start: a one-frame gate keeps the scene off Home's first frame; 15e runs on this branch with arm A = its develop merge-base. Licences: two routes, a static catalogue, an asset reader on an IO dispatcher.
Tests: JVM tests plus pixel comparisons inside one run, no stored goldens. Order: after phase 15 merges; parallel with the audio work in two worktrees, with four shared files.
Q-018 is recorded in `docs/tech-design.md` §1.3 on `feature/phase-14-release-docs` (d11112d).

## Rulings

### R1 — B-13: one backdrop, hosted in `AppNavHost` behind the `NavHost`

**Decision.** There is exactly one backdrop instance. It lives in `AppNavHost`, inside the window-filling theme `Surface`, behind the `NavHost`. Screens draw no backdrop of their own; they only stop painting over it.

**Why not one backdrop per screen.**
- **The NavHost's transition is a cross-fade of both screens.** In navigation-compose 2.9.8 the default `enterTransition` / `exitTransition` are `fadeIn(tween(700))` / `fadeOut(tween(700))` (`NavHost.kt:133,136` in the sources jar), and `AppNavHost` sets no transition of its own. With a backdrop inside each screen, both skies are translucent mid-transition and composite over `background`. That is the dip B-13 forbids: at the midpoint the window shows about 75 % sky. No per-screen variant avoids it without a second, app-wide transition system.
- **Cost.** A per-screen backdrop rebuilds its geometry on every entry into composition, every pop included. One host builds it once per window size, theme and density.
- **Shape.** Seven screens would each carry the host logic. One host leaves each screen exactly one change: transparent containers.

**Where the state lives.**
- **The source of truth is the NavController's back stack.** The density is a pure function of the committed destination, `NavDestination?.backdropDensity()` in `navigation/BackdropDestinations.kt`: `Home` → Landscape, `Gameplay` → Quiet, everything else → Sparse. "Everything else" covers `ClassicCatalog`, `Results`, `Settings`, `Tutorial`, `Licenses` and `LicenseText`, and it is deliberately the `else` branch: a destination added later gets Sparse, which is zone T from edge to edge and so can never put text on a saturated colour. Challenge needs no case, because it is `Gameplay` (B-07).
- **`SceneBackdrop(density, modifier)` in `ui/backdrop/` is stateless with respect to app state.** It knows no route and reads no `UiState`. Its only internal state is UI-element state of the kind `rememberScrollState` holds: the cross-fade's slots and `Animatable`, the orbs' infinite transition, and the scene-ready flag (R3).
- **The back-stack read happens in a thin adapter, `NavBackdrop(navController, modifier)`, also in `navigation/BackdropDestinations.kt`.** A destination change therefore recomposes only the adapter and the backdrop, never `AppNavHost`'s body or the `NavHost` call.
- **The first frame reads the right density, including after a process-death restore.**
  - `NavHost` sets its graph synchronously during its own composition (`navController.graph = graph`, `NavHost.kt:500`), and setting the graph restores a saved back stack synchronously.
  - So `NavBackdrop` is composed *after* `NavHost` in the same `Box` and drawn *behind* it with `Modifier.zIndex(BackdropMetrics.BACKDROP_Z_INDEX)` (−1f).
  - It initialises from the synchronous read: `remember(navController) { navController.currentBackStackEntryFlow }.collectAsState(initial = navController.currentBackStackEntry)`.
  - Composed before the `NavHost`, the initial entry would be `null` on frame 1 on every launch. That would mean either a guessed density, or one frame of the Home sky under a restored game screen.
  - A KDoc on the `Box` says why the order matters. BD-06's restore case (R5) pins it.
- **Predictive back.** `NavHost` drives predictive back with a seekable transition (`NavHost.kt:511–512`), but `currentBackStackEntry` changes only when the gesture commits. During the gesture the sky holds the current density; on commit it cross-fades. The sky never dips, which is the property BD-21 states. QA observes it with gesture navigation.

**The cross-fade.**
- **One duration constant: `ClayAnim.NAV_TRANSITION_MS = 700`.** It is today's NavHost default made explicit. `AppNavHost` passes `enterTransition = { fadeIn(tween(ClayAnim.NAV_TRANSITION_MS)) }` and the matching `exitTransition`, `popEnterTransition` and `popExitTransition`. The backdrop's fade uses the same constant.
  - The spec's separate `BACKDROP_CROSSFADE_MS` (§4.3) is not created: a single constant is how the two "cannot drift".
  - Nothing a player sees in the screens' own transition changes.
- **Two slots, never two translucent ones.**
  - **Below:** the current density, drawn at full alpha.
  - **Above:** present only during a change, drawn with `Modifier.graphicsLayer { alpha = fade.value }`, where `fade` is an `Animatable(0f)` run to 1f over `NAV_TRANSITION_MS`. When it reaches 1, the target becomes Below and Above is dropped.
  - **A change during a fade:** the in-flight Above is promoted to Below at full alpha, as if its fade had completed, and the new target fades in over it. This is `LaunchedEffect(density)`'s cancellation path.
  - The outgoing sky therefore stays opaque beneath until the incoming one is fully in (B-13).
- **Reduced motion.** When `rememberAnimationsEnabled()` is false, the swap is instant: there is no Above slot.

**What happens per frame.**
- **During a cross-fade:** only the Above slot's layer alpha changes. The `alpha` is read inside the `graphicsLayer` lambda, so the work is a draw-phase layer property update: no recomposition and no re-recording of the sky or the scene.
- **Orbs (L2).** Each orb's drift is read in `Modifier.graphicsLayer { translationX = …; translationY = … }`.
  - Today's `HomeBlobs` reads `progress` in composition and feeds it to `Modifier.offset(x = Dp, y = Dp)` (`HomeBlobs.kt:34–69`), so it recomposes and re-lays out on every frame of its 8 s drift.
  - After the move, a frame of drift is two layer property updates.
  - **O-B is not ruled here.** Whether idle Home should animate at all stays with the pre-release `compose-performance-audit` (tech-design §10). This lowers each frame's cost, not the frame count.
- **Sky (L1).** One `drawWithCache` per slot. `Brush.verticalGradient(stops)` is built in the cache block, keyed on size and the density's stops, and the draw block is one `drawRect(brush)`.
- **Scene (L3).**
  - **Own layer:** L3 sits in its own `Modifier.graphicsLayer()`, so the orbs' translation and the slot alpha re-composite it without re-recording its display list.
  - **Built once:** its geometry is built in the `drawWithCache` cache block from the pure model below, and `onDrawBehind` only replays.
  - **When it rebuilds:** on a change of size, density, palette (theme), layout direction or status-bar inset — the inset is read in composition as `WindowInsets.statusBars.getTop(LocalDensity.current)` and passed in as an `Int`.
  - **Offscreen texture:** whether to promote L3 to an offscreen (texture) layer is decided by measurement at the compose-performance-audit, not up front: it would hold a full-window texture in GPU memory.
- **Draw lambdas allocate nothing** (rules/compose.md, Recomposition). Named, because they are the usual misses:
  - no `Path()`, `Stroke(…)`, `Brush.*`, `listOf`/`mutableListOf` or `PathEffect` constructed in `onDrawBehind`;
  - `Stroke` objects and paths are built in the cache block;
  - no `forEach` over a `List` in the draw block (an iterator per call): use an indexed loop, `for (i in 0 until n)`;
  - no per-shape `clipPath`: a frosting lip is cut to its mound once, in the cache block, with `Path.combine(PathOperation.Intersect, …)`.

**The geometry is a pure model.**
- **Builder:** `ui/backdrop/BackdropGeometry.kt` holds `buildSceneModel(density, width, height, statusInsetPx, rtl, dimens): SceneModel` and `buildStageWings(slotWidth, slotHeight, artEdge, rtl, dimens): StageWingsModel`.
- **What they return:** immutable primitives — ellipses, circles, capsules (centre, length, width, angle), the spiral as parameters, a motif's base point and kind, and the list of motifs and sprinkles actually drawn.
- **What they avoid:** no Compose `Path` and no `android.*` import, so BD-10 and BD-11 test them without composing anything. The draw layer turns a model into `Path`s.
- **Units:** every dp input reaches the builder already in px through `BackdropDimens`, an `@Immutable` holder resolved once in composition from `dimensionResource` (`rememberBackdropDimens()`). Every ratio comes from `BackdropMetrics`. No literal appears anywhere.

**Palette.**
- **The hexes** live only in `ui/theme/Color.kt`, as `LightBackdrop<Token>` / `DarkBackdrop<Token>`: 12 tokens per theme, §4.1.
- **The holder:** `ui/theme/BackdropColors.kt` holds `@Immutable data class BackdropColors`, its `LightBackdropColors` / `DarkBackdropColors` instances (built from those vals) and `LocalBackdropColors = staticCompositionLocalOf { LightBackdropColors }`. It mirrors `ClayColors` / `LocalClayColors` in `Clay.kt`.
- **Who provides it:** `AppTheme` (`Theme.kt`), beside `LocalClayColors`.
- **The orbs** keep `colorScheme.primary` / `secondary` at `ClayAnim.BLOB_ALPHA`, as today.

**The theme `Surface`, and a fact the spec got wrong.**
- **There is no window-filling theme `Surface` today.** `AppTheme` wraps `MaterialTheme` only (`Theme.kt:26–38`). Every `Scaffold` paints `background` by default, and the tutorial's root `Surface` paints it explicitly (`TutorialScreen.kt:114–117`).
  - B-09 ("the window-filling theme `Surface` … stays") and §7's fill-count row both assume one exists.
  - Without it, a transparent `Scaffold` passes `contentColorFor(Color.Transparent)`, which falls back to `LocalContentColor.current`. Nothing above provides that, so it is the default black, and dark-mode text that names no colour turns black. That is exactly F-32.
- **Ruling.** `AppNavHost`'s root is `Surface(color = colorScheme.background, contentColor = colorScheme.onBackground, modifier = Modifier.fillMaxSize())`. This is rules/compose.md's F-32 surface.
  - Every transparent `Scaffold` also passes `containerColor = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground` explicitly, so a screen is right in a preview or a test without the host.
  - The four `TopAppBar`s pass `TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent)`. None uses a `scrollBehavior` today; both are set so that adding one later cannot repaint the bar.
  - The tutorial's root becomes `Surface(color = Color.Transparent, contentColor = MaterialTheme.colorScheme.onBackground)`. Its `Box` and the `propagateMinConstraints` reasoning in its comment are unchanged.
- **Fill count, corrected.** Today's frame 1 has two full-window fills: the window background and the `Scaffold`'s `background`. After this branch it has three: the window background, the host `Surface` and the sky.
  - The extra one is a solid fill, it is inside what 15e measures, and it is what makes §7's degradation step 2 ("no backdrop on the first frame") exactly today's picture.
  - Accepted.

**Insets.**
- **The backdrop is outside every inset.** It fills the window under both bars and any cutout and is never padded.
- **The content keeps the one inset application each screen has today.** That is the `Scaffold` content padding on Home, gameplay, the catalogue, Results, Settings and both licence screens, and `safeDrawingPadding()` on the tutorial column. 18b/18c's "each bar applied once" holds unchanged.
- **Known divergence from rules/compose.md F-33.** F-33 says the navigation host pads off the system bars once with `safeDrawingPadding()`. This product predates that line; its intent (each bar applied exactly once) is met and tested by 18b/18c and BD-19.
  - Moving the inset application up into the host would re-lay out every screen and re-open phase 15's 15k/18b/18c geometry. Not on this branch. It is recorded for the tech design (Open issues 4).
- **No scroll container may draw under a transparent top bar.** All four keep `.padding(contentPadding)` outside their scroll container, as today. The Sparse cloud's "no container can reach it" rests on that.
- **The orbs' frame of reference.** It is the window less `ScaffoldDefaults.contentWindowInsets`, the insets Home's `Scaffold` applies today, so the orbs keep their pixel positions (§3.1).
- **`MainActivity` is not touched.** No `enableEdgeToEdge()`, no `isNavigationBarContrastEnforced`: the platform keeps its three-button-navigation scrim (§5.4). Leaving the file alone also keeps the audio branch's V1 line conflict-free (R6).

**Test hook.**
- **The backdrop's root:** one semantics node, `Modifier.clearAndSetSemantics { testTag = BACKDROP_TEST_TAG; backdropDensity = …; backdropLayers = … }`.
  - The two custom keys follow the `PegCountKey` idiom (`GameScreen.kt`). `backdropLayers` is the set drawn, from {Sky, Orbs, Scene}.
  - With no actions, no role and no description, TalkBack never stops on it (BD-15).
- **The stage:** carries no custom key (R2).

**File-level shape** (paths under `app/src/main/java/com/app/pegsolitaire/` unless named):

| File | New / edited | What it holds |
|---|---|---|
| `ui/backdrop/BackdropDensity.kt` | new | `enum class BackdropDensity { Landscape, Quiet, Sparse }` |
| `ui/backdrop/BackdropMetrics.kt` | new | the §4.3 constants verbatim, `BLOB_PEEK_FRACTION` (moved from `HomeBlobs.kt`, value unchanged), `ORB_SECONDARY_Y`, `BACKDROP_Z_INDEX` |
| `ui/backdrop/BackdropDimens.kt` | new | `@Immutable` px holder of the §4.2 tokens + `rememberBackdropDimens()` |
| `ui/backdrop/BackdropGeometry.kt` | new | the pure `SceneModel` / `StageWingsModel` and their two builders; no Compose graphics, no `android.*` |
| `ui/backdrop/SceneBackdrop.kt` | new | stateless `SceneBackdrop(density, modifier)`: two-slot cross-fade, L1 sky, L3 scene, the semantics hook; light and dark `@Preview`s per density |
| `ui/backdrop/BackdropOrbs.kt` | new | the two orbs (L2): `HomeBlobs` re-seated per §3.1/§3.3, drift in `graphicsLayer`, drawn static at rest when animations are off (B-05) |
| `ui/backdrop/BackdropSceneReady.kt` | new | `LocalBackdropSceneReady` and `rememberBackdropSceneReady()` (R3) |
| `ui/backdrop/BackdropPreviewHost.kt` | new | `internal` preview wrapper: the host `Surface` plus `SceneBackdrop(density)` with the scene ready, for every screen preview |
| `ui/theme/BackdropColors.kt` | new | `BackdropColors`, its two instances, `LocalBackdropColors` |
| `ui/theme/Color.kt` | edited | the 24 `LightBackdrop*` / `DarkBackdrop*` vals |
| `ui/theme/Theme.kt` | edited | provides `LocalBackdropColors` beside `LocalClayColors` |
| `ui/theme/ClayAnim.kt` | edited | `NAV_TRANSITION_MS = 700` |
| `navigation/BackdropDestinations.kt` | new | `NavDestination?.backdropDensity()`, `NavBackdrop(navController, modifier)` |
| `navigation/AppNavHost.kt` | edited | host `Surface` → scene-ready provider → `Box { NavHost(…explicit transitions…); NavBackdrop(…, Modifier.matchParentSize().zIndex(…)) }`; the licence routes (R4) |
| `ui/home/HomeBlobs.kt` | deleted | moved to `BackdropOrbs.kt` |
| `ui/home/HomeScreen.kt` | edited | `HomeBlobs()` call removed; transparent `Scaffold`; the KDoc sentence about the blobs points at the backdrop |
| `ui/home/HomeStage.kt` | edited | the wings (R2) |
| `ui/game/GameScreen.kt`, `ui/classic/ClassicCatalogScreen.kt`, `ui/results/ResultsScreen.kt`, `ui/settings/SettingsScreen.kt`, `ui/onboarding/TutorialScreen.kt` | edited | container colours only (above); Settings also gains the About section (R4) |
| `res/values/dimens.xml` | edited | the §4.2 tokens as one contiguous, comment-headed block |
| `app/src/test/…/testing/BackdropTestHost.kt` | new | the production host's shape for tests: the `Surface`, `SceneBackdrop` at a given density, the scene-ready state controllable |

The developer transcribes; nothing here is new product behaviour beyond the two specs.

### R2 — The stage wings in `HomeStage.kt` (B-12): fit with D-03l clause 4 and the phase-15 tests

**Verdict: FIT, under the seven conditions below.**
- Clause 4 (`docs/ux-spec.md` §4.2.4) allows exactly one weighted child on Home to hold anything: the stage. What it holds must be decorative, carry no description, and be non-interactive and non-focusable.
- The wings are a second drawing inside that same child. They add no weighted child, no band, no target and no readout, so the exception stays exactly one child.
- Clause 5 holds too. The wings take no entrance order; they appear without an animation of their own (spec §6), and the art keeps borrowing `HERO_ENTRANCE_ORDER`.

**Conditions.**
1. **Shape.**
   - **Placement:** inside `HomeStage`'s `BoxWithConstraints`, a `StageWings(edge, modifier = Modifier.matchParentSize())` is composed *before* `HomeStageArt(edge)`, so it draws behind the art.
   - **When:** only when the art is composed (`edge ≥ home_stage_art_min_size`), `maxHeight ≥ backdrop_wing_min_height`, and the scene is ready (R3).
   - **Why `matchParentSize()`:** it is measured after the other children and contributes nothing to the `Box`'s size. The stage's weight, its edge formula `min(maxHeight, maxWidth, home_stage_art_max_size)`, its art and its first-frame cost (D-03g: one `BoardCanvas` draw) are untouched.
   - `edge` is passed in, so `e` has one formula in the codebase.
2. **Geometry.**
   - **Source:** from `buildStageWings(slotWidth, slotHeight, artEdge, rtl, dimens)` (R1), drawn through `drawWithCache` on the wings' own node.
   - **The skirt:** the wing hills run on 32dp below the slot, outside the node's bounds. So neither the stage nor the wings may add `clipToBounds()` or `graphicsLayer(clip = true)`.
   - **Drawing order:** the ribbon covers the skirt because the column draws it after the stage. The skirt shows in the band gap above it, as specified.
   - **RTL:** comes from `DrawScope.layoutDirection`.
3. **Semantics: a test tag and nothing else.** `Modifier.testTag(HOME_STAGE_WINGS_TEST_TAG)` on the wings node: no description, no `pointerInput`, no `clickable`, no focus.
   - **The spec's "one [custom semantics property] on the stage, carrying which wing sweets it drew" (§12) is rejected.** `HomeStageTest.theStageAndItsArtCarryNothingButTestTags` (criterion 15r) asserts that every node in the stage subtree carries exactly `TestTag`, plus `Shape` on the art (`HomeStageTest.kt:146–162`).
   - A custom key on the stage or the wings fails that guard. Widening the guard to admit it would weaken 15r for every later change.
   - **Instead:** which wings are drawn is the presence of the wings tag (BD-07), and which sweets and sprinkles are drawn is the `StageWingsModel`'s list, asserted on the pure builder (BD-11).
4. **Inputs.** The wings read the slot size, `edge`, the palette, the layout direction and `LocalBackdropSceneReady`, and nothing else. In particular they read no `HomeUiState`, so 15r's "renders identically before and after the first progress emission" test keeps its meaning.
5. **Phase-15 tests: unchanged and green, none edited to pass.**
   - 15n, 15o and 15q do not touch the wings.
   - 15p measures gaps between named visible elements (title, gear, stage art, pills, chips, tiles). The wings are not among them, so 15p's readings do not move. The spec's "15p can only tighten" is true of the picture, and the tests stay exactly as they are.
   - 15r now also covers the wings automatically, because they are in the stage subtree.
   - A failure in any of these suites is a defect in the wings, not a reason to touch the suite.
6. **Fit rule.** "A sweet is drawn only if its topmost point lies ≥ 8dp below the slot's top edge" (§3.3) lives in the builder, which omits a sweet rather than clipping it. The 160dp gate lives in `HomeStage`, next to the art's own gate.
7. **No entrance animation.** The wings appear on the frame the scene does, behind the art's 240 ms `clayEntrance`.

`HomeStage.kt` is a phase-15 file, so this edit lands only after phase 15 merges (R6).

### R3 — B-11: keeping the scene off Home's first frame, and the 15e sitting on this branch

**The mechanism: one flag, set one frame late.**
- **The flag.** `rememberBackdropSceneReady()` (`ui/backdrop/BackdropSceneReady.kt`) is called once, in `AppNavHost`'s host:
  - `val ready = remember { mutableStateOf(false) }`
  - `LaunchedEffect(Unit) { withFrameNanos { }; ready.value = true }`
- **How it reaches the readers.** The `State<Boolean>` itself — not its value — is provided through `LocalBackdropSceneReady = staticCompositionLocalOf<State<Boolean>> { AlwaysReady }`.
  - Only the two readers that call `.value` subscribe to the flip: `SceneBackdrop`'s L3 slot and `StageWings`. Nothing else recomposes when it flips, the provider included.
  - The default, `AlwaysReady`, is a `State` that is always true. Previews and component tests without the host therefore show the whole scene.
  - BD-23 (a) is asserted on the real host, so a production path that forgot the provider fails there.
- **Why this places the scene off frame 1.**
  - `am start -W`'s `TotalTime` ends when the activity window's first frame is drawn.
  - A `LaunchedEffect` starts only after its first composition is applied, and `withFrameNanos` resumes on the *next* frame.
  - So L3 and the wings are composed and recorded on the second frame (the third at the latest, depending on where the recomposer's own frame awaiter falls), never on the first.
  - Frame 1 is: the window background, the host `Surface`, the L1 gradient, the two orbs, and Home's content.
- **Determinism.** `withFrameNanos` runs on the Compose frame clock, so the test clock steps it exactly (BD-23 (a)).
- **Not used**, and why:
  - a state write from a draw lambda (a backwards write that forces a recomposition from the draw phase);
  - `delay(16)` (wall clock);
  - `View.post` / `Choreographer` (outside Compose's clock, so not testable).
- **Scope.** The flag is per Activity composition. A configuration change (dark mode, font scale) re-runs it, which is harmless.
- **The degradation steps (§7) become one-line changes on this flag.** Step 1 gates L1's gradient on it, drawing `SkyTop` flat on frame 1; step 2 gates the whole `NavBackdrop`. Do not build them as switches in advance: they ship only if a sitting fails.

**The 15e sitting on this branch.** It is `docs/ux-spec.md` §6.5's second edition unchanged, run by the qa-engineer using "The procedure the qa-engineer runs" in `.planning/reports/15-founder-feedback/tech-lead-fonts.md` Ruling 2.

What is specific to this branch:

- **Arm A** is `git merge-base develop feature/candy-backdrop` at the sitting, not the moving `develop`.
  - It must contain phase 15: `git merge-base --is-ancestor <the phase-15 merge commit> <A>` exits 0.
  - **Arm B** is the branch head.
  - Then `A..B` is exactly this branch's diff, whatever else `develop` has gained since.
- **When.** Last, before the merge: after the code-auditor's PASS on the final diff, and after the branch has merged the `develop` of that day.
  - If `develop` is merged in again before the merge, re-run only if that merge brings changes to start-path files: `App.kt`, `MainActivity.kt`, `di/`, `navigation/`, `ui/home/`, `ui/theme/`, `res/font/`. Otherwise the sitting stands.
- **Builds.**
  - Each arm is built with `:app:assembleDebug` in its own clean worktree beforehand, e.g. `git worktree add --detach ../PegSolitarie-armA-candy <A>` for arm A and a fresh `git worktree add --detach ../PegSolitarie-armB-candy <B>` for arm B, so neither arm's APK reflects uncommitted state.
  - All Gradle and Kotlin daemons are stopped before the sitting.
- **Precondition 7's validity check.** `git diff A..B -- app/schemas` is empty, and the diff adds or renames no DataStore key. This branch adds none by design; the code-auditor confirms it in review.
- **Precondition 1 (animations off) changes what arm B draws, deliberately.**
  - With animations off, arm A's `HomeBlobs` returns early and draws no orbs.
  - Arm B draws both orbs, static at rest (B-05).
  - That is part of what ships and is measured as such.
- **A quiet host (preconditions 2–3).** The producer confirms, before the sitting:
  - the founder's `Pixel_9` is shut down;
  - no Gradle build is running in any worktree, `feature/audio-review`'s included;
  - no other agent session is building or measuring.
  - The audio build and its QA sessions pause for the sitting's duration (R6).
- **Evidence.** There is no phase directory for this branch, so it goes in `.planning/reports/candy-backdrop/qa-engineer-15e.md`, in §6.5's field shape:
  - one table per arm, and the null-control table;
  - the eight preconditions, each with its recorded value;
  - both `uptime` readings;
  - each installed `base.apk`'s sha256 after every swap;
  - `floor` and the threshold.
- **Verdict.** PASS / FAIL / VOID exactly as §6.5 rules.
  - **VOID** is re-run, never reported as either.
  - **FAIL:** walk backdrop §7's steps 1–2, then §6.5's steps 1–4, re-measuring the whole sitting, null control included, after each step and stopping at the first PASS. A failure that survives step 2 is not the backdrop's; the tech-lead rules on it with the numbers before any art is removed (§7).
- **BD-23 (c), the jank check, is a separate block after the 15e sitting.** It is not interleaved with 15e and is not part of its verdict.
  - Animations are **on** (scale 1): the frames that could jank are the entrance cascade and the frame-2 build, which 15e's animations-off state does not exercise.
  - At least 10 kept samples per arm, interleaved A/B in blocks of five with the first sample of each block discarded, and arms swapped only at block boundaries as in precondition 7.
  - For each sample, read the "Janky frames" line of `adb shell dumpsys gfxinfo com.erman.pegsolitarie` one second after `am start -W` returns.
  - PASS when `median_B ≤ median_A + 1`.
- **15f** (placeholders, no spinner, no reflow) is re-observed at the same visit; it is unaffected by construction.

### R4 — The licences screen

**Routes** (`navigation/Routes.kt`):
- `@Serializable data object Licenses`
- `@Serializable data class LicenseText(val entryId: String)`

**Navigation** (`AppNavHost`):
- `settingsContent` gains `onOpenLicenses`, wired to `navController.navigate(Licenses)`.
- `composable<Licenses>` gets an `onOpenEntry(entryId)` that navigates to `LicenseText(entryId)` and an `onBack` that pops.
- `composable<LicenseText>` gets an `onBack` that pops.
- Both screens are Sparse by R1's `else` branch.

**The route carries the entry's stable id, never an asset path.**
- An id the catalogue does not know shows the text screen's error state. It is not a crash and not a file lookup.
- Route arguments are restorable, forgeable state, which is the same reasoning as `Gameplay(levelId)` and T-05-08: nothing from a route reaches `AssetManager.open`.

**Where the static list is defined: `ui/licenses/LicenseCatalog.kt`.**
- **Shape:** `internal object LicenseCatalog` with `val sections: List<LicenseSection>` and `fun entry(id: String): LicenseEntry?`. The types are `@Immutable` data classes with read-only `List` fields, the codebase's existing idiom (`ClassicCatalogUiState`, `GameUiState`). Adding `kotlinx.collections.immutable` would be a new dependency, which LS-AC10 and BD-04 forbid.
  - **`LicenseSection`:** `@StringRes title` and `entries`.
  - **`LicenseEntry`:** `id`, `@StringRes title`, its lines, and `text: BundledLicenseText?`.
  - **`enum class BundledLicenseText(val assetPath: String, @StringRes val licenseName: Int)`:**
    - `FredokaOfl("licenses/Fredoka-OFL.txt", …)`
    - `NunitoOfl("licenses/Nunito-OFL.txt", …)`
    - `Apache2("licenses/Apache-2.0.txt", …)`
  - This enum is the only place in the code where an asset path is written. Three software entries share `Apache2`.
- **Strings:** every visible string is a resource (spec §6). Proper names, licence names and addresses carry `translatable="false"`; sound counts use the `licenses_sound_effects` plurals. Naming the User Messaging Platform once U-1 settles is then a string change and nothing more.
- **Why code, and not a generated list or a JSON asset:**
  - LS-03's reasons: names a player recognises, offline, deterministic.
  - A JSON asset would add a read and a parser to a screen with nine static entries.
  - `oss-licenses` and AboutLibraries are new dependencies.

**The two screens.**
- **Licenses (the list).**
  - **Wiring:** a stateless `LicensesScreen(sections, onOpenEntry, onBack, modifier)`. `LicensesRoute` hands it `LicenseCatalog.sections`.
  - **No ViewModel**, and this is a tech-lead exception to tech design §3.1's "every screen has one `@HiltViewModel`": the screen has no data source, no async work and no event beyond navigation, so a ViewModel would hold nothing.
  - **List:** its `LazyColumn` uses `key = { it.id }` per entry and `rememberLazyListState()`. The state is saveable, and the back-stack entry stays alive while a text is open, so the scroll position survives the round trip (LS-AC02).
  - **Sound:** `ButtonForward` comes from `withClickSound` at each tappable entry and at the Settings row (LS-AC09).
- **License text.**
  - **Wiring:** `LicenseTextRoute` → `@HiltViewModel LicenseTextViewModel(savedStateHandle, source: LicenseTextSource, defaultDispatcher: CoroutineDispatcher)`.
  - **What the ViewModel does:** it reads `savedStateHandle.toRoute<LicenseText>().entryId` and resolves the entry through `LicenseCatalog.entry`. In `init` on `viewModelScope` it calls the source once, then re-flows the text on the injected Default dispatcher (the existing unqualified binding, `DomainModule`).
  - **What it exposes:** `StateFlow<LicenseTextUiState>`, a sealed type:
    - `Loading(entry)`;
    - `Loaded(entry, paragraphs: List<String>)`;
    - `Failed(entry?)`, which also covers an unknown id and a failed read.
  - **The 300 ms spinner rule is UI timing.** The screen shows the spinner only if the state is still `Loading` after `LicensesUi.SPINNER_DELAY_MS` (300, a scoped constants object in `ui/licenses/`). It is driven by a `LaunchedEffect` `delay` on the composition's clock, so the Compose test clock controls it.
  - **Body: a plain `Column` in `verticalScroll(rememberScrollState())`, not a `LazyColumn`.**
    - This departs from the spec's §4 wording, deliberately.
    - The longest text is the Apache licence: 11,358 bytes, a few dozen paragraphs, a cheap one-time composition.
    - A `SelectionContainer` around a lazy list can only select what is composed, so LS-AC05's "selectable" would hold only for the paragraphs on screen. A `Column` makes the whole text selectable.
    - It still keeps one `Text` node per paragraph for TalkBack, and its scroll state is saveable.

**How the texts are read, off the main thread.**
- **`data/LicenseTextSource.kt`:** `interface LicenseTextSource { suspend fun read(text: BundledLicenseText): String? }`.
- **`data/AssetLicenseTextSource.kt`:** `class AssetLicenseTextSource @Inject constructor(@ApplicationContext context: Context, @IoDispatcher dispatcher: CoroutineDispatcher)`.
  - `read` runs `withContext(dispatcher) { context.assets.open(text.assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() } }`.
  - It catches `IOException` only and returns `null`; anything else propagates.
- **Binding:** `@Binds` in `di/DataModule.kt`.
- **`@IoDispatcher`** is a new qualifier in a new file, `di/DispatchersModule.kt`. The same file holds `@Module @InstallIn(SingletonComponent::class) object DispatchersModule` with `@Provides @IoDispatcher fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO`.
  - The graph already binds an *unqualified* `CoroutineDispatcher` (Default, `DomainModule.kt:72`), so a second unqualified binding would be a Hilt duplicate-binding error.
  - `data/` may hold a `Context`; `PlatformBoundaryRules` forbids only `Activity` there.
- **The re-flow (LS-07)** is a pure function, `reflowLicenseText(raw: String): List<String>` in `ui/licenses/LicenseTextReflow.kt`:
  1. Normalise `\r\n` to `\n`.
  2. Split on runs of blank or whitespace-only lines.
  3. Trim each line and join a paragraph's lines with single spaces.
  4. Drop empty paragraphs.
  - It changes no character other than whitespace, and the asset file stays untouched.

**`app/src/main/assets/licenses/Apache-2.0.txt`.**
- **Content:** byte-identical to `https://www.apache.org/licenses/LICENSE-2.0.txt`, fetched by this role on 2026-10-17. It is 11,358 bytes, sha256 `cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30`.
- **The developer** downloads it, checks the hash, and commits it unmodified.
- **Formatters leave it alone:** it has no trailing whitespace and ends in one newline, so the repo's `.editorconfig` rules (`trim_trailing_whitespace`, `insert_final_newline`) do not change it.
- **BD-01:** it contains none of BD-01's words (grep in Checks).
- **BD-04 carve-out:** BD-04's "no new file under `assets/`" exempts exactly this file, plus an `assets/licenses/<name>-MIT.txt` only if the release listing proves an MIT artifact ships (LS §7). Nothing else.
- **THIRD_PARTY.md:** its row, and the research-analyst's one-line confirmation of the text's own redistribution terms, belong to the release-manager's file on `feature/phase-14-release-docs`. They are not this branch's (LS §9).

**Settings.**
- The About section and Licenses row sit as LS §2 specifies, inside `SettingsScreen.kt`'s existing `Column`, as a fourth `…Section` composable between `PlayGamesSection` and `PrivacySection`.
- The entrance orders are renumbered as one contiguous run. `SettingsScreen`'s signature gains `onOpenLicenses`, threaded from `SettingsRoute`.

### R5 — Test strategy

**Rules for every test below.**
- **Determinism** (rules/testing.md): transitions run on the Compose test clock (`mainClock.autoAdvance = false`, `advanceTimeByFrame()`). System-bar insets come from `testing/SystemBarInsets.kt`, per the phase-15 spec's §13. Names read as sentences. Each test is watched fail once before it is trusted.
- **"Pixel"** means the repo's own `renderedWindow()` (`testing/RenderedWindow.kt`) under `@GraphicsMode(NATIVE)`, Robolectric 4.16, SDK 34. No `@Config(sdk = …)` for another level (tech-design §8.1).

**Screenshot tests: no stored goldens on this branch.** Tech design §8.5 stands, and BD-25's "Roborazzi goldens" clause is struck for this branch:
- a golden recorded from the diff under review is an image of the implementation, not of the contract;
- CI (`ubuntu-latest`) and development (macOS/arm64) still have no host-parity decision;
- Roborazzi is not in `gradle/libs.versions.toml`, so adding it breaks BD-04's "no new dependency".

**What *is* justified now: pixel comparisons inside one run.**
- BD-17, BD-20 and BD-22 are *equality properties between two renders*: with and without the backdrop; font scale 1.0 and 2.0; LTR and mirrored RTL.
- Both images are rendered in the same JVM, on the same host, in the same test, and compared in memory. Nothing is stored and there is no baseline to drift, so neither objection above applies. The infrastructure already exists.

**What this changes elsewhere.**
- **BD-02:** the auditor's half is answered from the qa-engineer's device captures, listed by path in QA's report, together with the code and the `@Preview`s. The producer sequences that one line of the audit after QA's capture session; nothing else in the audit waits.
- **The golden-image suite** stays the post-v1.1 roadmap item of §8.5.

**Who checks what.** The kinds:
- **JVM** — a plain JUnit test, no Robolectric.
- **Robo** — a Robolectric test, without composition unless it says so.
- **Compose** — a Robolectric Compose UI test.
- **Pixel** — a Compose test that compares pixels inside one run.
- **Device** — the qa-engineer, on the device matrix of spec §11.
- **Review** — the code-auditor, on the diff.

| # | Kind | Test (illustrative name) or check | Notes |
|---|---|---|---|
| BD-01 | JVM + Review | `UserFacingTextHasNoProtectedNamesTest` over `src/main/res/values*/strings.xml` and `src/main/assets/**`; BD-01's third grep (the diff) stays the auditor's; `docs/store` stays the release-manager's | Added so that the verdict's words rule outlives this review: every later string, the wrong-tap strings included, is checked by `check` |
| BD-02 | Device + Review | two-question test on QA's captures | see above |
| BD-03 | Review | §10 checklist on the code and the captures | — |
| BD-04 | Review | `git diff --stat develop...HEAD` | with the Apache-2.0 carve-out (R4) |
| BD-05 | Check + Review | `dpSpLiteralCheck`, detekt, ktlint, the grep | — |
| BD-06 | Compose | `BackdropDensityTest`: real `AppNavHost` with `NavFlowTest`'s stand-in slots, through every destination, reading `backdropDensity`; **plus a restore case**: `StateRestorationTester` restores a back stack whose top is `Gameplay`, and the backdrop's density is Quiet on the first frame | the restore case pins R1's composition order |
| BD-07 | Compose | layers from `backdropLayers`; Home's wings from `HOME_STAGE_WINGS_TEST_TAG`, on the three reference windows × the three chip states | — |
| BD-08 | JVM | `BackdropColorsTest`: luminance and hue from the `Color.kt` vals (`androidx.compose.ui.graphics.Color` is plain Kotlin) | — |
| BD-09 | Pixel + Device | the backdrop alone, insets supplied: every pixel of `[0, statusInset)` equals `background`, Landscape's primary-orb box excluded; QA compares the band against `develop` | — |
| BD-10 | JVM | `BackdropMetricsTest` | — |
| BD-11 | Robo | `BackdropGeometryTest` on the pure builders; `BackdropDimens` resolved from real `Resources` under `@Config(qualifiers = …)` at the four windows, no composition | the 2dp, 4dp and 8dp rules, the omit-not-clip rule, sprinkle rules 1–6 |
| BD-12 | Device | — | the computed half is BD-08 |
| BD-13 | Device | — | ditto |
| BD-14 | Pixel + Compose | `TransparentContainersTest`: each stateless screen inside `BackdropTestHost`, light and dark; a pixel in an empty region equals the backdrop's own pixel there (Quiet's and Sparse's lower window is `SkyMid`/hill, ≠ `background`); and a probe in `AppNavHost`'s slots asserts `LocalContentColor.current == onBackground` in both themes (F-32) | — |
| BD-15 | Compose + Device | the backdrop node's semantics = tag + the two keys; the existing traversal tests run unchanged with the host present; QA's TalkBack pass | — |
| BD-16 | Compose + Device | animator scale 0 via `Settings.Global` (what `rememberAnimationsEnabled()` reads): orbs present, at their rest bounds; the "requests no frame" clause is device-checked (`dumpsys gfxinfo` frame count flat over 5 s idle on Quiet, Sparse, and Home at scale 0) | Compose tests cancel infinite animations, so a JVM frame count would prove nothing |
| BD-17 | Pixel | `BoardInteriorIdentityTest`: `GameScreen` mid-game (selection, dots, hint) in `BackdropTestHost(Quiet)` against a plain `Surface(background)` host; slab interior inset 2dp identical; same for Home's stage art, tiles and chips | in-run comparison, no file |
| BD-18 | Pixel + Review | covered by BD-17, plus a slab-interior pixel equal to `surfaceContainerHigh` | — |
| BD-19 | existing suites + Device | phase-15 suites green, none edited; QA: 18b's insets, the three-button scrim, the gesture handle | see below |
| BD-20 | Pixel | the backdrop alone under `LocalDensity` with `fontScale` 1.0 and 2.0, compared | — |
| BD-21 | Pixel + Device | `SkyContinuityTest` (below); QA's screen recording | — |
| BD-22 | Pixel + Device | the backdrop alone, and `HomeStage` at a fixed slot, under `LocalLayoutDirection` LTR and RTL; one image mirrored and compared under a stated tolerance constant, which must fail on a deliberately unmirrored scene; QA in `ar-XB` | — |
| BD-23 (a) | Compose | `BackdropFirstFrameTest` on the real host: after the first frame `backdropLayers` = {Sky, Orbs} and no wings tag; present within two `advanceTimeByFrame()` | "after one frame" in the spec, made exact |
| BD-23 (b), (c) | Device | R3 | — |
| BD-24 | Device | greyscale captures | — |
| BD-25 | Review | light and dark `@Preview`s on every new public composable (screen previews through `BackdropPreviewHost`) | goldens struck (above) |
| BD-26 | Review + release-manager | on `release/3.0.0` | — |
| LS-AC01 | Compose | `SettingsScreenTest`'s rendered-top order check, extended | — |
| LS-AC02 | Compose + Device | `AppNavHost` with stand-ins: Settings → Licenses → a text → back → back; the list's first visible index survives the round trip | — |
| LS-AC03 | JVM + release-manager | `LicenseCatalogTest` against an expected list written in the test, mirroring THIRD_PARTY.md's shipped rows; also every `BundledLicenseText.assetPath` exists, non-empty, under `src/main/assets/`, and every file in `src/main/assets/licenses/` is referenced exactly once | the THIRD_PARTY.md diff is the release-manager's at checklist steps 1.4 and 6 |
| LS-AC04 | JVM + Review | BD-01's test covers these strings | — |
| LS-AC05 | JVM + Robo + Compose + Device | `reflowLicenseText` property: whitespace-normalised join equals the whitespace-normalised asset, for all three texts; `AssetLicenseTextSourceTest` reads each through the real `AssetManager`; Compose: one node per paragraph, selectable; QA at 2.0 | — |
| LS-AC06 | Compose + Device | traversal, headings, `Role.Button` and the "Read the license" label; inert entries carry no click action | — |
| LS-AC07 | Device + Review | airplane mode on the device; the auditor greps `ui/licenses/` and the source for any network call or outbound `Intent` | — |
| LS-AC08 | Compose | BD-06 and BD-14 cover both routes | — |
| LS-AC09 | Compose | the `ButtonSoundTest` idiom | — |
| LS-AC10 | Review + Check | — | one Compose UI test per screen: Licenses (open, read sections, open Fredoka); License text (load, scroll, the error state through a fake `LicenseTextSource`) |
| LS-AC11 | release-manager | AAB listing at checklist step 1.4 | — |

**`SkyContinuityTest` (BD-21), the property made exact.**
- **Setup:** `AppNavHost` with stand-in slots, the test clock paused. Navigate Home → catalogue → gameplay → Results → Home and render every frame of each transition.
- **Sample points:** pixels where no stand-in draws, and away from the orbs and from every density's clouds.
- **The assertion:** at every frame, each sampled colour lies on the segment between that pixel's colour in the outgoing and the incoming density, within ±2/255 per channel. Only a convex blend is allowed.
- **Why the spec's wording needs this form.** "A pixel in the top band never changes" is not literally true: the clouds and the primary orb sit in the top band and differ between densities. The blend form is what "never lightens toward `background`" means.

**The phase-15 suites (BD-19).** They run unchanged, and none is edited to pass.
- A suite that renders a component alone over a `Surface(background)` (the strip, the dock, the timer) stays as it is: it tests the component, not the backdrop.
- A suite that renders a whole screen and reads pixels next to a container renders that screen in `BackdropTestHost` at the screen's density, so it reads what ships.
- If a ratio assertion then fails, that is a BD-13 finding for the ux-designer through the producer, never a re-baselined number.

### R6 — Sequencing, and parallel work with `feature/audio-review`

**Confirmed: this branch builds only after `feature/phase-15-ux-refresh` merges into `develop`.** Every surface it edits exists only on phase 15:
- `HomeStage.kt` (D-03l) and the one-row hero;
- the strip and dock split (D-05d) that Quiet's geometry is computed against;
- the `Tutorial` destination and its `safeDrawingPadding()` column;
- Settings' sections;
- `assets/licenses/Fredoka-OFL.txt` and `Nunito-OFL.txt` (the bundled fonts);
- the phase-15 test suites BD-19 re-runs.

15e's arm A must also contain phase 15 (R3).

**Two corrections.**
1. **"Builds after" means "merges `develop` in first".** `feature/candy-backdrop` was cut from `develop` 3f2af90, which is before phase 15, and carries docs commits only (4803cf0, 6cec86e, and this report).
   - Once phase 15 has merged, the producer merges `develop` into this branch before the developer's first commit. A docs-only branch has nothing to conflict.
   - Building on the current base would mean writing against files that do not exist there.
2. **No dependency on `feature/phase-14-release-docs`.** THIRD_PARTY.md, its §1 idea row (r5 line 79) and the coming Apache-2.0 row are the release-manager's file, on that branch. This branch neither edits nor reads THIRD_PARTY.md, and LS-AC03's expected list is written in the test. BD-26 and LS-AC03's THIRD_PARTY.md diff are checked on `release/3.0.0`.

**In parallel with the audio integration: yes, with conditions, and not "without file collisions".** That claim holds for one half of the audio work and not the other. The overlap was checked against `git diff develop...feature/audio-review`, `docs/tech/audio-silent-mode.md` §5's file table and `docs/ux/invalid-tap-feedback.md`.

| Audio work | Files it edits (its own design docs) | Overlap with this branch |
|---|---|---|
| The silent-mode gate, P5, P4a, P8, V1 (`docs/tech/audio-silent-mode.md` §5) | `audio/*` (four new files, `SoundPoolPlayer.kt`, `SoundParams.kt`, `SoundCue.kt`), `di/AudioModule.kt`, `App.kt`, `MainActivity.kt` (V1, one line), `build-logic/…/standards.gradle.kts` (failure text), `res/raw/`, `audio/recipes/`, `docs/audio/`, audio tests; its §3 scope also names `app/build.gradle.kts`, `AndroidManifest.xml` and `gradle/libs.versions.toml` | **none, as ruled.** This branch adds no dependency and no manifest entry, so it touches none of those three either. The backdrop host lives in `AppNavHost`, not `MainActivity` (R1). The licence source binds in `di/DataModule.kt`, with the qualifier in a new `di/DispatchersModule.kt`, so `di/AudioModule.kt` is not touched |
| The wrong-tap feedback (`docs/ux/invalid-tap-feedback.md`) | `ui/game/GameScreen.kt`, `GameUiState.kt`, `GameViewModel.kt`, `ui/components/BoardCanvas.kt`, `BoardGeometry.kt`, `ui/theme/Clay.kt`, `ui/onboarding/TutorialScreen.kt`, `TutorialViewModel.kt`, `res/values/dimens.xml` (`wrong_tap_mark_width`), `res/values/strings.xml` (three announcements) | **four files:** `GameScreen.kt` (this branch: the `Scaffold`'s two colour arguments only), `TutorialScreen.kt` (the root `Surface`'s two colour arguments only), `dimens.xml` and `strings.xml` (both branches add a block). This branch touches neither `BoardCanvas.kt` nor `Clay.kt`; BD-17 requires the board untouched |

**Conditions for running them side by side.**
1. **Two worktrees and two developer dispatches, not two gsd phases.** rules/studio.md (Parallel work): "a product's phases run one at a time: gsd's executor cannot commit inside a worktree it did not create". If the producer runs either build through `/ergoz-soft:build`, the two are serial.
2. **This branch keeps its edits to the two shared screens to the lines named above.** It adds its `dimens.xml` and `strings.xml` entries as one comment-headed block each (`<!-- Backdrop -->`, `<!-- Licenses -->`), never interleaved with existing entries. A conflict in those files is then a mechanical "keep both blocks".
3. **Merge order is free.** Whichever branch is green and signed off first merges first. The second merges `develop` in, resolves, re-runs the full Check (both branches' tests included), and goes back through review on the merged diff. For this branch, that is also when the 15e rule in R3 applies.
   - If the audio branch merges first, its start-path work (receivers and collectors started from `App.onCreate`) lands in this branch's arm A. That is the correct comparison: 15e then measures the backdrop alone.
4. **The host is shared.**
   - 15e needs exactly one emulator and no Gradle build anywhere (preconditions 2–3), so the audio build, its Check runs and its QA sessions pause for this branch's sitting.
   - The audio device checks D1–D10 need a physical phone, so they do not compete for the `Pixel_9a`.
   - Two Checks in two worktrees at once are correct (F2's lock is per worktree), but each roughly doubles the other's ~11-minute wall clock on the 10-core host.
5. **The wrong-tap half also waits on a ruling this role owes.** The ux-designer's handoff on `feature/audio-review` asks the tech-lead how the tutorial reads `hapticsOn` without breaking `TutorialViewModel`'s two-collaborator pin. That ruling is separate from this report and is not given here; it gates only the wrong-tap build.

### Where this report departs from the two specs

The developer builds to this report where the two differ. The ux-designer folds these into the specs in their own house style (`B-nna` rulings with pointers). That fold is non-blocking: the build does not wait for it.

| # | Spec text | This report | Why |
|---|---|---|---|
| D1 | B-09, §7: "the window-filling theme `Surface` … stays"; the fill count is unchanged | There is none today; `AppNavHost` adds it, which is one more solid fill on frame 1 (R1) | `Theme.kt` has no `Surface`; without one, transparent containers make dark-mode text black (F-32) |
| D2 | §4.3: a `ClayAnim.BACKDROP_CROSSFADE_MS` "equal to the transition the `NavHost` uses" | One constant, `ClayAnim.NAV_TRANSITION_MS = 700`, used by the `NavHost`'s four transitions (made explicit at today's default) and by the backdrop (R1) | two constants can drift; one cannot |
| D3 | §12: a custom semantics property on the stage naming the wing sweets drawn | No custom key on the stage; a wings test tag plus the pure builder's model (R2) | it would fail 15r's existing guard, `theStageAndItsArtCarryNothingButTestTags` |
| D4 | BD-25, §12: Roborazzi goldens; BD-02's auditor half "on the goldens" | No stored goldens; same-run pixel comparisons for BD-17, BD-20 and BD-22; BD-02 on QA's device captures (R5) | tech design §8.5; BD-04 (Roborazzi would be a new dependency) |
| D5 | BD-04: no new file under `assets/` | Except `assets/licenses/Apache-2.0.txt`, which LS §7 requires, and a conditional MIT text (R4) | the two specs contradicted each other |
| D6 | BD-21: "a pixel in the top band never changes" | The convex-blend property, sampled away from the orbs and the clouds (R5) | the top band holds clouds and the orb, which differ between densities |
| D7 | BD-23 (a): "present after one frame" | Absent on the first frame; present within two frames (R3) | where the recomposer's frame awaiter falls is not the app's to fix |
| D8 | BD-23 (c): inside the 15e sitting | A separate block after it, animations **on** (R3) | 15e runs with animations off, which hides the jank the check is for, and adb reads between samples would perturb 15e |
| D9 | BD-01: auditor and QA greps only | Also a JVM test over strings and assets (R5) | the verdict is permanent; reviews are not |
| D10 | LS §4: the text body is a `LazyColumn` in a `SelectionContainer` | A `Column` in `verticalScroll` (R4) | a lazy list can only select what is composed; the longest text is 11 KB |
| D11 | LS: no state holder named for the list | No ViewModel for the list; one for the text (R4) | the list holds nothing |

### The design-doc update: Q-018 in `docs/tech-design.md` §1.3

The update is committed on `feature/phase-14-release-docs` in `PegSolitarie-r5`: d11112d, `docs(14): record Q-018 - minSdk 24 stands until a technical need`.
- **§1.3 records the founder's answer:** `minSdk` 24 indefinitely, unless a technical need appears. It cites FOUNDER.md Q-018 (committed on `feature/phase-15-ux-refresh`, 98913c8) and states what does and does not re-open it:
  - **Re-opens it:** a required dependency or Play-policy SDK whose minimum leaves 24 with no maintained version at 24; a platform API a feature needs with no compat path on API 24–25; a Play requirement.
  - **Does not:** the ≈ 50 KB of variable fonts, simpler code paths, or rules/kotlin-android.md's scaffold default.
  - **What happens on a need:** the tech-lead records it there with evidence, and the producer informs the founder before the release that carries it. Information, not a new question: the answer already authorises a raise on a technical need.
- **Consistency edits in the same file,** following the file's own never-delete rule:
  - the §1.3 recommendation paragraph gains a superseded pointer;
  - §14.2's "minSdk 26 when the Play statistics permit" is marked superseded;
  - §16 row 1 records the answer;
  - §17 gains the supersession row.

No other file was touched, as dispatched.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/tech-lead-architecture.md` — this report: rulings R1–R6, the departures D1–D11, and the file-level shape the android-game-dev builds.
- `/Users/ermanergoz/Desktop/PegSolitarie-r5/docs/tech-design.md` — §1.3 records Q-018, with consistency edits in §14.2, §16 and §17; committed on `feature/phase-14-release-docs` as d11112d.

## Checks
- `git -C /Users/ermanergoz/Desktop/PegSolitarie-r5 commit -F -` (docs/tech-design.md only): pass (d11112d; `git status` clean afterwards).
- `curl -sS -m 20 -o <scratch>/LICENSE-2.0.txt https://www.apache.org/licenses/LICENSE-2.0.txt && shasum -a 256 <scratch>/LICENSE-2.0.txt`: pass (11,358 bytes, `cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30`).
- `grep -c ' $' <scratch>/LICENSE-2.0.txt` and `tail -c 50 … | od -c`: pass (0 lines with trailing whitespace, ends in one `\n`, so `.editorconfig` leaves it byte-identical).
- `grep -niwE 'candy|candies|crush|saga|candyland|tiffi|toffee|yeti|odus|king|activision|microsoft'` over the Apache text and over `PegSolitarie-p15/app/src/main/assets`: pass (zero lines).
- `unzip -q navigation-compose-android-2.9.8-sources.jar && grep -rn 'tween(700)\|navController.graph = \|PredictiveBack' .`: pass (default `fadeIn`/`fadeOut(tween(700))` at `NavHost.kt:133,136`; the graph is set synchronously in composition at `:500`; predictive back at `:512`).
- `grep -rn 'Surface(\|safeDrawingPadding\|enableEdgeToEdge\|containerColor\|Scaffold(\|TopAppBar(' app/src/main/java` on `PegSolitarie-p15`: pass (no theme `Surface` in `AppTheme`; every `Scaffold` and `TopAppBar` uses default colours; `safeDrawingPadding()` only on the tutorial; no `enableEdgeToEdge`).
- `grep -n` over `HomeStageTest.kt` on `PegSolitarie-p15`: pass (15r's guard is `containsExactly(SemanticsProperties.TestTag)` over the stage subtree, which is the basis of D3).
- `grep -n -i 'roborazzi\|immutable' gradle/libs.versions.toml` on `PegSolitarie-p15`: pass (neither present, which is the basis of D4 and R4's `@Immutable` idiom).
- `git diff --stat develop...feature/audio-review`, plus `git show feature/audio-review:docs/tech/audio-silent-mode.md` and `docs/ux/invalid-tap-feedback.md` file greps: pass (37 files today, all audio assets and docs; the planned code edits are R6's table).
- `./gradlew check`: not run. No code or resource changed; this commit adds a report, and the other commit changes one design doc.

## Open issues / risks
1. **Staffing deviation (Q-019).** These rulings were written on Opus, the developer's model. Mitigation: the code-auditor reviews the build against this report and runs the Check and its own mutation probes; a ruling the auditor finds wrong comes back to the tech-lead through the producer.
2. **The commit trailer.** The report's commit carries rules/gitflow.md's single trailer, `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, written by hand; no `prepare-commit-msg` hook is installed in this repository. That rule (plugin v1.0.0 and the v0.6.2 snapshot imported by CLAUDE.md) overrides the harness reminder. `Ran on:` above records the true model.
3. **rules/compose.md.** This branch *adds* the F-32 theme `Surface`, closing a gap no one had found. It *diverges* from F-33's host-level `safeDrawingPadding()` on purpose (R1, Insets). Both belong in tech design §2/§3 as recorded decisions.
4. **To fold into `docs/tech-design.md`** (tech-lead, later; not done here because the dispatch scoped the doc edit to Q-018):
   - §3: the backdrop host (R1), the licence routes, and the list screen's no-ViewModel exception (R4);
   - §8.5: a pointer allowing same-run pixel comparisons while stored goldens stay out;
   - §10: 15e on this branch, and the Idle Home row's drift moving into `graphicsLayer`;
   - §13: the bundled Apache-2.0 text, with no dependency added;
   - §14.1: a row for this branch after phase 15;
   - §16 #4: the in-app licences surface, now scheduled with this branch rather than in the v1.1.1 backlog.
5. **R1's restore guarantee depends on composition order** (`NavBackdrop` after `NavHost`, drawn behind by `zIndex`). A KDoc and BD-06's restore case pin it. If a later refactor moves the backdrop above the `NavHost`, the restore test fails rather than the sky silently guessing.
6. **O-B remains open** (the orbs' idle frames on Home): R1 cuts each frame's cost and does not rule on the drift itself.
7. **Predictive back:** the sky follows the committed destination, not the gesture. The property holds (no dip); QA observes it with gesture navigation. Accepted.
8. **15e may VOID more than once on a busy host.** Every VOID costs a re-sitting, and the audio work must be idle each time (R6 condition 4).
9. **The wrong-tap build** shares four files with this branch (R6) and waits on a separate tech-lead ruling about the tutorial's `hapticsOn`, which this report does not give.
10. **The drafted Q-NNN (game-screen density) is a constant change either way, confirmed.** The builder reads each density's composition table from `BackdropMetrics`, so option 2 adds four motif placements to Quiet's table and option 3 drops Quiet's mounds, lips, drips and sprinkles. Neither touches the host, the cross-fade or the tests' shape.
11. **BD-09 is an exact-equality test.** If the gradient shader dithers on Robolectric's native canvas, the test states a ±1-per-channel tolerance as a named constant. It must not drop the assertion.
12. **Unaffected and still open elsewhere.** The ux-designer's routed items are not technical and this report does not touch them: the missing colour-blind palette option and the Home title's light stop (backdrop §14 items 2–3). The backdrop shares no token with the pegs, so whatever form the colour-blind option takes needs no backdrop change.

## Handoff
Next: producer —
1. When phase 15 has merged, merge `develop` into `feature/candy-backdrop` (R6), then dispatch the android-game-dev with `docs/ux/candy-backdrop.md`, `docs/ux/licences-screen.md` and this report; R1–R5 and D1–D11 govern where they differ from the specs.
2. Dispatch the ux-designer, non-blocking, to fold D1–D11 into the two specs.
3. Choose the parallel shape with `feature/audio-review` under R6's conditions 1–4, as developer dispatches in two worktrees, not gsd phases. Tell both developers about the four shared files.
4. Give the code-auditor and the qa-engineer BD-01–BD-26 and LS-AC01–LS-AC11 with R5's split. Sequence the auditor's BD-02 line after QA's capture session.
5. Schedule the 15e sitting per R3: last before the merge, Pixel_9 off, no build anywhere, the audio work idle. Then the separate BD-23 (c) block.
6. Release-manager: a THIRD_PARTY.md row for `assets/licenses/Apache-2.0.txt` (sha256 above). Research-analyst: the one-line confirmation of the licence text's own redistribution terms (LS §9).
7. Tech-lead: fold Open issues 4 into the tech design when scheduled, and sign off the build after the auditor's and QA's reports.

Next: android-game-dev — build R1–R5 test-first, in this order:
1. tokens and pure models (BD-08, BD-10, BD-11);
2. the host, the cross-fade and the scene-ready gate (BD-06, BD-07, BD-21, BD-23 (a));
3. the transparent containers (BD-14);
4. the stage wings (R2);
5. the licence screens (LS-AC01–LS-AC10);
6. the pixel comparisons (BD-17, BD-20, BD-22).

Run the phase-15 suites unchanged throughout (BD-19).
