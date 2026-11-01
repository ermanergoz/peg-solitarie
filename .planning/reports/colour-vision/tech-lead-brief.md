# tech-lead report — colour-vision (developer brief: the root collection, the board marks, the sitting)
Date: 2026-10-24
Branch: feature/colour-vision (committed by the producer)
Skills loaded: ergoz-soft:standards (role tech-lead; the product snapshot is 0.5.1 and the plugin is 1.0.0, so the plugin 1.0.0 copies were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, stacks, testing, dispatch; and the product's `.claude/rules/lessons.md`), android-architecture, android-gradle-logic, compose-skill, compose-ui, android-viewmodel, android-coroutines, android-testing
Ran in parallel with: hint-undecided (review), audio-review (review), check-gates (build), ux-spec-folds (check), talkback-board (brief amended today); no Gradle was run here
Ran on: claude-fable-5-1 at effort xhigh
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **The root collection (colour-vision §3.3, the one open question):** a `@Singleton` holder, `BoardMarkPaletteState`, turns `SettingsRepository.colorblindPaletteOn` into a `StateFlow<BoardMarkPalette>` with `stateIn(@ApplicationScope, Eagerly, Standard)`; `MainActivity` injects it and composes `AppThemeRoot(palette) { AppNavHost() }`, which collects it with `collectAsStateWithLifecycle()` and passes the value to a new `AppTheme(boardMarkPalette)` parameter; `AppTheme` provides `LocalBoardMarks`, a dynamic `compositionLocalOf`. The three binding properties fall out of that shape (§1). It is a start-path change, so the branch sits its own 15e (§9).
- **The board (§3):** the marks' geometry and alphas become a pure recipe per palette (`BoardMarkRecipe`, the `DockLook.recipe` idiom), `BoardGeometry` takes it as a defaulted parameter, and `BoardCanvas` draws from the recipe and from `LocalBoardMarks` on its interactive path only. The halo, the opaque dots and the dashed amber rings are exactly colour-vision §3.4; the dashed rings start at the top through `drawArc`.
- **In-flight trees:** this branch builds only after `feature/audio-review` is on `develop` (it edits `BoardCanvas.kt`'s `drawSelectedPeg` and palette, which this branch extends); `bugfix/hint-undecided` shares nothing but `strings.xml`/`dimens.xml`; `feature/talkback-board` restructures `BoardCanvas`'s composable tree while this branch edits its draw functions, and either may merge first under the resolution in §10. Position: 6b stands, after talkback-board and before candy-backdrop's sitting (§10).
- **Settings:** the row after Haptics with `COLORBLIND_ROW_ORDER = 4` and a whole renumbering; the licences branch renumbers again, and whichever lands second renumbers on top (§5).
- **One ruling on §6.5's validity clause** for the DataStore key this branch adds (§9), and three items routed to the ux-designer (Open issues).

## 0. Read first
**For:** the android-game-dev, dispatched by the producer, in the worktree `/Users/ermanergoz/Desktop/PegSolitarie-cv` on `feature/colour-vision` (cut from `develop` `c5f2c54`; its one commit `8f67d74` carries the spec and the three candy reports). Paths are under `app/src/main/java/com/app/pegsolitaire/` unless named.

**Inputs, in precedence order.**
1. This brief, where it decides a mechanism, a file, a test or an order.
2. `docs/ux/colour-vision.md` on this branch: CV-01 to CV-13, §2, §3 (every value of §3.4 is binding), §5.1's CV-AC01 to CV-AC15, §6, §11.2. **Not this branch:** §4, TG-01/TG-02, TG-AC01 to TG-AC05 and the title clause of CV-AC07 — the Home title fix ships on `feature/candy-backdrop` (`candy-backdrop/tech-lead-direction-b.md` A9, A10). Nothing here touches `HomeHero.kt`, `Clay.kt`'s `ctaGradientBrush()` or the CTA colours.
3. `.planning/reports/candy-backdrop/tech-lead-direction-b.md` A9, A10 (the colour-blind rows: no Roborazzi, no stored goldens, same-run pixel comparisons) and A11; `tech-lead-architecture.md` R5's test kinds (JVM, Robo, Compose, Pixel, Device, Review).
4. `docs/tech-design.md` §3.1 (UDF), §8.1–§8.5 (where tests live, the shared DataStore double, no stored goldens), §9.8 (warnings as errors once check-gates lands), §10 (the 15e gate and the start-path list), §14.1 (merge order, row 6b).
5. `docs/ux-spec.md` §6.5 as amended on `feature/ux-spec-folds` (`ef721dd`): the 15e preconditions 1–9, the null control, the verdict table. The qa-engineer reads it for §9.

**What is fixed before the developer starts (B0).** The first act on the branch is `git merge develop` (never a rebase), and `develop` must by then contain **`feature/audio-review`** (merge step 5): its `BoardCanvas.kt` (the `wrongTapCorrection` parameter, `BoardPalette.mark`/`markStroke`, the `shaken` centres, `drawSelectedPeg`'s `nudge`, `wrongTapMarkStroke`), its `SettingsRepository : HapticsSetting`, and its `dimens.xml` block beside `peg_selected_ring_width` are what this branch's hunks are written against. Building on a copy of audio's code is not allowed; if audio-review is not on `develop`, the producer holds the dispatch. Whatever else is on `develop` at B0 (hint-undecided, check-gates, ux-spec-folds, talkback-board, licences-screen, F-1) is merged in and §10's rules apply to the rest.

**What this brief does not decide.** The look (every colour, radius, alpha and dash count is colour-vision §3.4's), the row's words and position (§3.1), the deficiencies served and the numbers (§3.5): the ux-designer's. The scope: rules/compose.md's "colour-blind palette option in games". This brief chooses the mechanism, the files, the tests, the order and the sitting, and names every place where it departs from the spec's illustrative names (§2, §4, Open issues).

## 1. The root collection — how the setting reaches `AppTheme` once
**Ruling.** The spec's three properties (§3.3) are met by one process-wide `StateFlow`, collected once at the Activity's composition root and handed to `AppTheme` as a value:

1. before the first read lands, the standard palette is in use — the flow's initial value is `Standard`;
2. a configuration change never flashes the other palette — the holder is a `@Singleton`, so after the Activity is recreated the flow already holds the persisted value, and `collectAsStateWithLifecycle()` seeds the composition with `StateFlow.value`, not with a default;
3. a change in Settings reaches every consumer without recreating anything — the write lands in DataStore, the repository flow emits, the `StateFlow` updates, `AppThemeRoot` recomposes, `AppTheme` re-provides the local, and only the readers of a **dynamic** local recompose.

**The holder — `ui/theme/BoardMarkPaletteState.kt`.**

    @Singleton
    class BoardMarkPaletteState @Inject constructor(
        settings: SettingsRepository,
        @ApplicationScope scope: CoroutineScope,
    ) {
        /** The palette in use; [BoardMarkPalette.Standard] until the store's first emission. */
        val palette: StateFlow<BoardMarkPalette> =
            settings.colorblindPaletteOn
                .map { on -> if (on) BoardMarkPalette.Colorblind else BoardMarkPalette.Standard }
                .stateIn(scope, SharingStarted.Eagerly, BoardMarkPalette.Standard)
    }

- It is the `ChallengeProgress` idiom (`data/ChallengeProgress.kt:59-72`): `stateIn(@ApplicationScope, Eagerly, DEFAULT)` over a DataStore flow. `@Singleton` on the class with an `@Inject` constructor; no module binding, no interface (it is a holder, like `ProgressSubmitter`), so `di/` is not edited.
- **Where the read starts.** Hilt constructs a singleton on first injection. The only injector is `MainActivity`, so the collection starts in `MainActivity.onCreate` after `super.onCreate()`, on the application scope (`Dispatchers.Default`), and DataStore does the file read on its own dispatcher. Nothing on the main thread waits for it. The JVM suite never constructs it: no test references `MainActivity` (its own KDoc), so the singleton is never built there, and `App.onCreate` — which Robolectric runs for every test — does not touch it. Do **not** move the field to `App`.
- **Why `Eagerly` and not `WhileSubscribed`.** Property 2: the value must be warm before the recreated Activity's first frame. One DataStore subscription for the life of the process costs nothing measurable.
- **Fail-open is the repository's, not the holder's:** a damaged store makes the repository emit `false`, and the holder maps it to `Standard` (CV-04).

**The root composable — `ui/theme/AppThemeRoot.kt`** (`internal`; `MainActivity` is in the same module):

    @Composable
    internal fun AppThemeRoot(palette: StateFlow<BoardMarkPalette>, content: @Composable () -> Unit) {
        val boardMarkPalette by palette.collectAsStateWithLifecycle()
        AppTheme(boardMarkPalette = boardMarkPalette, content = content)
    }

`MainActivity` gains one field and swaps one line (its only two edits):

    @Inject lateinit var boardMarkPalette: BoardMarkPaletteState      // a holder with no interface, like progressSubmitter — the KDoc says so
    …
    setContent {
        CompositionLocalProvider(LocalSoundPlayer provides soundPlayer) {
            AppThemeRoot(palette = boardMarkPalette.palette) {
                AppNavHost(navController = rememberNavController())
            }
        }
    }

**`AppTheme` gains a parameter and provides the local — `ui/theme/Theme.kt`:**

    @Composable
    fun AppTheme(
        darkTheme: Boolean = isSystemInDarkTheme(),
        boardMarkPalette: BoardMarkPalette = BoardMarkPalette.Standard,
        content: @Composable () -> Unit,
    ) {
        val boardMarks = remember(boardMarkPalette, darkTheme) { boardMarksFor(boardMarkPalette, darkTheme) }
        CompositionLocalProvider(
            LocalClayColors provides if (darkTheme) DarkClayColors else LightClayColors,
            LocalBoardMarks provides boardMarks,
        ) { MaterialTheme(…) }
    }

- The default keeps every existing `AppTheme { }` call site (7 in main, 3 test files) and every Robolectric test on the standard palette, unchanged.
- **`LocalBoardMarks` is `compositionLocalOf`, not `staticCompositionLocalOf`.** `LocalClayColors` is static because it changes only with the theme, which recomposes everything anyway. The palette changes at runtime from a switch; a static local would recompose the whole app under `AppTheme` on every flip, a dynamic one recomposes exactly the readers: the interactive board, the star rows and the tutorial pips. Settings itself has no reader, so it shows nothing but the switch moving (§3.1). Default: `boardMarksFor(Standard, darkTheme = false)`, so a composable outside `AppTheme` draws the standard light marks.
- The cost of a flip while a board is on screen: `AppThemeRoot` recomposes, `AppTheme` recomposes, `MaterialTheme` is skipped (same scheme, typography, shapes, memoized content), the board's `BoardPalette` is rebuilt once and the next frame draws in the new palette. No `Animatable`, no cross-fade (§3.6).

**First frame, cold start and the restore case.**
- **Home's first frame reads no palette.** Home composes `BoardCanvas` twice, both `interactive = false` (`HomeStage.kt:103`, `HomeBoardArt.kt:48`), and the read-only path of `BoardCanvas` does not read `LocalBoardMarks` (§3). The ribbon's gilded star and the tiles keep `goldGradientFill()` (§4). So Home's composition is the same with the palette on or off, which `HomePaletteIdentityTest` pins in-run (§7) and the code-auditor reads against D-03g.
- **What the cold start gains:** one `@Singleton` construction in `onCreate`, one flow subscription on the shared `app_prefs` `DataStore<Preferences>` — a store other collectors already read at start (the onboarding flag, `ChallengeProgress`, the opt-in), so it is a second subscriber on one cached read, not a second file read — and one `collectAsStateWithLifecycle` at the root. That is inside what 15e measures, and §9 sits it.
- **A process restored straight into gameplay** (`Gameplay` on top of the restored back stack): the new process's holder starts at `Standard`; the board draws standard for the frames before the read lands, then switches once, with no dialog and no transition — exactly §3.3's accepted behaviour. `BoardMarkPaletteStateTest` pins "Standard before the store's first emission" (§7).

**Rejected.** A `@HiltViewModel` at the root (`hiltViewModel()` in `setContent`): also activity-scoped and config-change-safe, but it would put a navigation-Hilt call in the theme root for one boolean, and the codebase's idiom for "a setting read once at app start" is the eager `stateIn` holder. Reading the repository inside `AppTheme`: couples the theme to Hilt and to `data/`, and breaks every preview. Reading the repository in `GameViewModel`/`ResultsViewModel`: three reads, three flashes, and the tutorial's two-collaborator pin (`TutorialSourceGuardTest`) forbids a fourth.

## 2. The palette's shape: tokens, recipe, colours, geometry
Two halves, split by what can be tested without Robolectric. Names here are binding where the spec's were illustrative (§3.3 "illustratively `LocalBoardMarks`", §3.4 "illustratively `ColorblindBoardMetrics`").

**2.1 The palette enum and the colour holder — `ui/theme/BoardMarks.kt`.**

    enum class BoardMarkPalette { Standard, Colorblind }

    @Immutable
    data class BoardMarks(
        val palette: BoardMarkPalette,
        val selectionRing: Color,            // primary, both palettes
        val selectionHalo: Color?,           // null | surfaceContainerLowest
        val legalTarget: Color,              // primary (the alpha is the recipe's)
        val hintRing: Color,                 // tertiary | LightColorblindHint (light) / tertiary (dark)
        val earnedStarFill: List<Color>,     // goldGradient | LightColorblindStarFillColors (light) / goldGradient (dark)
        val earnedStarOutline: Color?,       // null | onTertiaryContainer
        val unearnedStarOutline: Color,      // outlineVariant | onSurfaceVariant
        val donePipFill: List<Color>,        // goldGradient | LightColorblindStarFillColors (light) / goldGradient (dark)
    )

    val LocalBoardMarks = compositionLocalOf { boardMarksFor(BoardMarkPalette.Standard, darkTheme = false) }

    /** Pure: reads the scheme and clay vals of Color.kt / Clay.kt; no composition, so a JVM test can call it. */
    internal fun boardMarksFor(palette: BoardMarkPalette, darkTheme: Boolean): BoardMarks

    /** Test hook (CV-AC03): the palette the board-state node reports; TalkBack never reads it. */
    val BoardMarkPaletteKey = SemanticsPropertyKey<BoardMarkPalette>("BoardMarkPalette")

The Standard instance is built from exactly today's tokens — `primary`, `tertiary`, `LightGoldGradientColors`/`DarkGoldGradientColors`, `outlineVariant` — so a Standard board is drawn from the same values it is drawn from today. The Colorblind instance is §3.4's table verbatim. The dark Colorblind instance adds no hex (§3.4): `tertiary`, `onTertiaryContainer`, `onSurfaceVariant`, `surfaceContainerLowest`, the dark gold gradient.

**2.2 The two new hexes — `ui/theme/Color.kt`** (the only place a hex appears, CV-AC04):

    /** Colour-blind palette, light theme (docs/ux/colour-vision.md §3.4): amber-700 for the hint rings and connector. */
    internal val LightColorblindHint = Color(0xFFB45309)
    /** Colour-blind palette, light theme: the earned star's fill, `GoldGradient`'s geometry in amber. */
    internal val LightColorblindStarFillColors = listOf(Color(0xFFC05A08), Color(0xFFB45309))

**2.3 The constants — `ui/game/ColorblindBoardMetrics.kt`**, beside `BoardMetrics`, the spec's names verbatim:

    object ColorblindBoardMetrics {
        const val SELECTED_RING_RATIO = 0.42f
        const val HINT_SOURCE_RING_RATIO = 0.44f
        const val HINT_TARGET_RING_RATIO = 0.30f
        const val DESTINATION_DOT_ALPHA = 1f
        const val HINT_HIGHLIGHT_ALPHA = 1f
        const val HINT_ARROW_ALPHA = 0.5f
        const val HINT_RING_DASHES = 8
    }

**2.4 The recipe — `ui/game/BoardMarkRecipe.kt`**, the `DockLook.recipe` idiom (`DockLook.kt:84-100`): the geometry and alpha half of each palette as data, so CV-AC08 is a JVM test over the mapping and not over a frame.

    @Immutable
    data class BoardMarkRecipe(
        val selectedRingRatio: Float,
        val hintSourceRingRatio: Float,
        val hintTargetRingRatio: Float,
        val destinationDotAlpha: Float,
        val hintRingAlpha: Float,
        val hintConnectorAlpha: Float,
        val hintRingDashes: Int,     // 0 = solid
        val halo: Boolean,
    )

    val BoardMarkPalette.recipe: BoardMarkRecipe   // Standard → BoardMetrics' values, 0 dashes, no halo; Colorblind → ColorblindBoardMetrics', 8 dashes, halo

    /** The non-hue channels of every mark that can share a board, for CV-AC08 (§7). */
    enum class BoardMark { SelectionRing, LegalTarget, HintSourceRing, HintTargetRing, WrongTapMark, EarnedStar, UnearnedStar }
    data class MarkLook(val form: MarkForm, val radiusRatio: Float?, val dashes: Int, val halo: Boolean, val filled: Boolean)
    enum class MarkForm { Ring, Disc, Glyph }
    fun BoardMarkRecipe.look(mark: BoardMark): MarkLook

`WrongTapMark` is audio's reduced-motion mark (`BoardMetrics.WRONG_TAP_MARK_RING_RATIO` 0.40, `WRONG_TAP_MARK_DASHES` 12, `onSurfaceVariant`): it is on the same board as the hint rings and is dashed too, so it belongs in the table; its row is the same in both palettes. The spec does not list it (it was written beside, not after, the wrong-tap spec); the ux-designer confirms the row (Open issues 2). `ui/game` already imports `ui/theme` (`ClayAnim`, `LocalClayColors`), so the recipe keying on `BoardMarkPalette` keeps the import direction the codebase has; `ui/theme` imports nothing from `ui/game`.

**2.5 `BoardGeometry` takes the recipe — `ui/components/BoardGeometry.kt`**, one defaulted parameter, nothing else:

    fun forCanvas(spec: BoardSpec, size: Size, recipe: BoardMarkRecipe = BoardMarkPalette.Standard.recipe): BoardGeometry
    val selectedRingRadius: Float get() = recipe.selectedRingRatio * cellSize       // was BoardMetrics.SELECTED_RING_RATIO * cellSize
    val hintSourceRingRadius: Float get() = recipe.hintSourceRingRatio * cellSize
    val hintTargetRingRadius: Float get() = recipe.hintTargetRingRatio * cellSize

The Standard recipe holds `BoardMetrics`' own constants, so the three radii are the same float product they are today, and `center`, `positionAt`, `cellSize`, `pegRadius`, `holeRadius` and `destinationDotRadius` do not read the recipe at all. Every existing caller (`BoardGeometryTest`, `BoardCanvasTest`, talkback-board's overlay) compiles and reads identical values through the default. One geometry object carries the radii the draw uses; no second code path, no dead property.

**2.6 One extraction in `Clay.kt`** so the stars can take a different brush without a copy of the offscreen blend:

    /** Recolours the content with [brush], the content's alpha as the mask; offscreen so nothing bleeds. */
    internal fun Modifier.gradientFill(brush: Brush): Modifier = graphicsLayer(compositingStrategy = Offscreen).drawWithCache { onDrawWithContent { drawContent(); drawRect(brush = brush, blendMode = BlendMode.SrcAtop) } }
    @Composable fun Modifier.goldGradientFill(): Modifier = gradientFill(goldGradientBrush())

`goldGradientFill()`'s callers (`HomeProgressRibbon.kt:211`, `HomeScreen.kt:522`, `AdOfferDialog.kt:161`) are untouched and pixel-identical. This is the branch's only `Clay.kt` hunk, beside `goldGradientFill` and below candy's future `titleGradientBrush()`; candy merges last and keeps both (§10).

**2.7 The halo width — `res/values/dimens.xml`:** `peg_selection_halo_width` = `1.5dp`, placed in the sub-4dp stroke block directly under `peg_selected_ring_width` and audio's `wrong_tap_mark_width`, with a comment naming the exception and the owner ("the band of `surfaceContainerLowest` inside the colour-blind selection ring, docs/ux/colour-vision.md §3.4; one token per owner").

## 3. The board marks in `BoardCanvas.kt`
All of this sits in the file's second half — `BoardPalette`, `drawCell`, `drawSelectedPeg`, `drawHintHighlight` — and in the palette capture at the top of `BoardCanvas(...)`. The composable's tree (the Canvas chain, `pointerInput`, the effects) is not this branch's: talkback-board owns it (§10).

**3.1 Where the palette is read.** In composition, once, on the interactive path only:

    val marks = if (interactive) LocalBoardMarks.current else null         // a read-only board draws pegs and holes, which no palette touches
    val recipe = marks?.palette?.recipe ?: BoardMarkPalette.Standard.recipe
    val geometry = remember(spec, canvasSize, recipe) { BoardGeometry.forCanvas(spec, canvasSize, recipe) }

Once talkback-board's `if (!interactive)` first branch is on the tree, the read moves inside the interactive branch and the `null` case disappears; until then the conditional read is what keeps Home's two read-only boards off the local (CV-AC06). The read-only draw is byte-identical in both states.

**3.2 `BoardPalette` gains the colour-blind fields**, resolved in composition like the existing ones (DrawScope can read neither theme nor resources):

    private class BoardPalette(
        … the existing seven, plus audio's mark and markStroke,
        val selectionHalo: Color?,          // marks?.selectionHalo
        val haloWidthPx: Float,             // dimensionResource(R.dimen.peg_selection_halo_width).toPx()
        val destinationDotAlpha: Float,     // recipe.destinationDotAlpha
        val hintRingAlpha: Float,           // recipe.hintRingAlpha
        val hintConnectorAlpha: Float,      // recipe.hintConnectorAlpha
        val hintSourceStroke: Stroke,       // solid Stroke(ringWidthPx) when dashes == 0, else dashedRingStroke(geometry.hintSourceRingRadius, ringWidthPx, dashes)
        val hintTargetStroke: Stroke,       // the same for the target ring
    )

`accent` stays `primary`; `hint` becomes `marks?.hintRing ?: MaterialTheme.colorScheme.tertiary`. The two dashed strokes are built once per `(geometry, recipe, ringWidthPx)` in a `remember`, exactly as audio builds `markStroke`, never in the draw lambda (rules/compose.md, no allocation in hot paths; `tech-lead-architecture.md` R1's list).

**3.3 The dashed stroke — `ui/components/DashedRing.kt`:**

    /** [dashes] equal dashes at a 50 % duty cycle around a ring of [radius] (the stroke's centreline), butt caps. */
    internal fun dashedRingStroke(radius: Float, widthPx: Float, dashes: Int): Stroke {
        val interval = (FULL_TURN * radius) / (dashes * DASH_AND_GAP)
        return Stroke(width = widthPx, pathEffect = PathEffect.dashPathEffect(floatArrayOf(interval, interval)))
    }

It is audio's `wrongTapMarkStroke` formula with the dash count as a parameter (`WrongTapFeedback.kt:252-258`). Audio's function may delegate to it in the same commit; if it does, `BoardCanvasWrongTapReducedMotionTest` stays green and the interval formula is unchanged. `Stroke`'s default cap is `StrokeCap.Butt`, which is what keeps the gaps open (§3.4 "round caps would close the gaps").

**3.4 The dashes start at the top: `drawArc`, not `drawCircle`.** A circle path begins at the three o'clock point, so a dash effect on `drawCircle` would start there. §3.4 wants the pattern "fixed, starting at the ring's topmost point". The hint rings are therefore drawn as

    drawArc(color, startAngle = RING_START_ANGLE_DEG, sweepAngle = FULL_SWEEP_DEG, useCenter = false,
            topLeft = center - Offset(radius, radius), size = Size(radius * 2, radius * 2), style = stroke)

with `RING_START_ANGLE_DEG = -90f` and `FULL_SWEEP_DEG = 360f` as named privates in `BoardCanvas.kt`. For the Standard palette the stroke is solid and the arc is pixel-identical to today's `drawCircle` (same centre, radius, width, colour, alpha) — the in-run pixel test of §7 proves it on the branch; the Standard recipe's radii are the same products they were. `Offset` and `Size` are value classes, so the two constructions allocate nothing.

**3.5 `drawSelectedPeg`** — the one function whose signature audio changed (`nudge`):

    private fun DrawScope.drawSelectedPeg(center, geometry, palette, pulse: Float, nudge: Float) {
        drawCandyPeg(palette, center, radius = geometry.pegRadius * BoardMetrics.SELECTED_PEG_SCALE * pulse)
        val ringRadius = geometry.selectedRingRadius * pulse * nudge          // 0.40c standard, 0.42c colour-blind, through the recipe
        palette.selectionHalo?.let { halo ->
            drawCircle(color = halo, radius = ringRadius - palette.ringWidthPx / 2 - palette.haloWidthPx / 2, center = center,
                       style = Stroke(width = palette.haloWidthPx))
        }
        drawCircle(color = palette.accent, radius = ringRadius, center = center, style = Stroke(width = palette.ringWidthPx))
    }

- **Halo geometry (§3.4 "so nobody re-derives it"):** the ring is stroked on its centreline, so its inner edge is `ringRadius − w/2`; the halo's outer edge meets it, so the halo's centreline is `ringRadius − w/2 − h/2`. The peg's visible part is then `0.42c − 3dp` (ring half 1.5 + halo 1.5), the spec's figure. Drawn after the sphere and before the ring, so it covers the sphere's outermost band. It scales with `pulse` and with audio's `nudge` because it is derived from the same `ringRadius`, so the two rings never separate.
- The `Stroke(width = haloWidthPx)` constructed here is one small object per selected-peg draw, as the existing `Stroke(width = palette.ringWidthPx)` already is; build both once into `BoardPalette` (`ringStroke`, `haloStroke`) if the code-auditor asks for zero allocation — either is accepted.
- Standard: `selectionHalo == null`, so nothing is drawn that is not drawn today.

**3.6 `drawCell`'s legal-target dot:** `palette.accent.copy(alpha = palette.destinationDotAlpha)` — the recipe's 0.38 or 1.0 — radius and pop unchanged.

**3.7 `drawHintHighlight`:** the target ring (`geometry.hintTargetRingRadius`, `palette.hintTargetStroke`), then the source ring (`geometry.hintSourceRingRadius`, `palette.hintSourceStroke`), both `palette.hint.copy(alpha = palette.hintRingAlpha)` through `drawArc`; then the connector `palette.hint.copy(alpha = palette.hintConnectorAlpha)` with today's width. Draw order unchanged (target → source → line); the hint is still drawn after every cell, so "a hint and a selection on the same peg" reads as hinted-and-selected (§3.4).

**3.8 Nothing else in the draw moves.** `drawCandyPeg`, `drawDimple`, the jumper, the removal fade, the landing squash, audio's `shaken` centres, `drawWrongTapMark` and its grey 12-dash stroke, the dot pop, the pulse: untouched in both palettes. The pulse, the pop and the jump stay behind `rememberAnimationsEnabled()`; the dashes have no phase animation and the halo no transition (§3.6, CV-AC09).

**3.9 The test hook.** `GameScreen.kt`'s board-state node (`GAME_STATE_TEST_TAG`, `GameScreen.kt:201-206`) gains one line in its existing `semantics { }` block: `set(BoardMarkPaletteKey, LocalBoardMarks.current.palette)` (a `boardMarkPaletteValue` delegate beside `pegCountValue`). It is the `PegCountKey` idiom; TalkBack does not read a custom key (CV-AC10). Talkback-board adds `isTraversalGroup = true` to the same block (its A2.1), so the hunk is a keep-both (§10). The tutorial's board carries no such node and needs none; CV-AC03 is read on gameplay.

**3.10 The `@Suppress("LongMethod")` on `BoardCanvas`** stays one annotation; its surface comment gains "the colour-blind marks". Never a second suppression, no baseline (kotlin-android.md; check-gates C7 makes a warning a failure).

**3.11 Previews — new file `ui/components/BoardCanvasPreviews.kt`** (detekt's per-file function count; `GameScreenPreviews.kt` is hint-undecided's to edit): English-33 mid-game with a selection and legal targets, and Wiegleb-45 with a selection and a hint, each × `AppTheme(boardMarkPalette = Standard | Colorblind)` × light and dark (CV-AC15 "the board previews show both palettes").

## 4. Stars and pips
Four consumers (§3.3): Results' `StarRow`/`EarnedStar` (`ResultsScreen.kt:221-290`), the catalogue's `EarnedStarPips` (`BoardPreview.kt:100-124`), the tutorial's `StepPips` done pip (`TutorialScreen.kt:188-215`) and `CompletionStar` (`:343-358`). **Not consumers,** and untouched: `HomeProgressRibbon`'s gilded glyph, `HomeScreen.kt:522`, `AdOfferDialog`'s gold, the Results star glow (`STAR_GLOW_ALPHA`, `tertiary`), the current and upcoming pips.

**4.1 One glyph, one file — `ui/components/StarGlyph.kt`:**

    /** A star in the active palette: the earned fill (and, when the palette has one, an outline over it), or the unearned outline. Decorative: no semantics. */
    @Composable
    fun StarGlyph(earned: Boolean, size: Dp, modifier: Modifier = Modifier) {
        val marks = LocalBoardMarks.current
        Box(modifier.size(size)) {
            if (earned) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary,
                     modifier = Modifier.matchParentSize().gradientFill(Brush.linearGradient(marks.earnedStarFill)))
                marks.earnedStarOutline?.let { Icon(Icons.Outlined.StarBorder, contentDescription = null, tint = it, modifier = Modifier.matchParentSize()) }
            } else {
                Icon(Icons.Outlined.StarBorder, contentDescription = null, tint = marks.unearnedStarOutline, modifier = Modifier.matchParentSize())
            }
        }
    }

- **The outline glyph is `Icons.Outlined.StarBorder`**, the hollow star the app already draws for unearned stars. The spec says "`Icons.Outlined.Star` drawn over it"; in `material-icons-core` `Outlined.Star` is a filled star, and the drawn outline the spec describes is `StarBorder`. Built as `StarBorder`; the ux-designer's wording is corrected non-blocking (Open issues 3). The outline's stroke is the icon's own (the standard 24dp artwork scaled to the size), which is what "the same glyph" means; no `Stroke` is hand-drawn.
- Standard: the earned fill is `goldGradient` through the same `gradientFill`, no outline; the unearned tint is `outlineVariant`. Pixel-identical to today, and the extra `Box` carries no semantics (an `Icon` with `contentDescription = null` adds none), so `EarnedStarCountKey`, the catalogue row's `clearAndSetSemantics`, the pip row's "Step N of 3" and every traversal are unchanged (CV-AC10).
- Light and dark `@Preview`s in the same file, earned and unearned, both palettes.

**4.2 The four call sites.**
- `ResultsScreen.kt`: the unearned branch becomes `StarGlyph(earned = false, size = results_star_size)`; `EarnedStar` keeps its glow `Box` and its `rememberPopIn` layer and replaces its `Icon(...).goldGradientFill()` with `StarGlyph(earned = true, size = results_star_size)`. `StarPopSounds`, the stagger and `EarnedStarCountKey` do not move.
- `BoardPreview.kt`: each pip becomes `StarGlyph(earned = isEarned, size = catalog_star_pip_size)`.
- `TutorialScreen.kt` `CompletionStar`: `StarGlyph(earned = true, size = results_star_size, modifier = Modifier.scale(scale.value))`.
- `TutorialScreen.kt` `StepPips`: the done pip's `goldGradientFill()` becomes `gradientFill(Brush.linearGradient(LocalBoardMarks.current.donePipFill))`; the current and upcoming pips and `PIP_BORDER_WIDTH` are untouched (§3.4 "unchanged").

`TutorialScreen.kt` names `LocalBoardMarks` and `gradientFill` and nothing from `TutorialSourceGuardTest`'s forbidden list (`SettingsRepository`, `hintsEnabled`, `soundOn`, the haptic and wrong-tap tokens): the palette reaches the tutorial through the local, as the board's does.

## 5. The setting: repository, state, ViewModel, screen, entrance orders
**5.1 Repository — `data/SettingsRepository.kt`** (§3.2 verbatim):
- `SettingsRepository` gains `val colorblindPaletteOn: Flow<Boolean>` and `suspend fun setColorblindPaletteOn(on: Boolean)`, after `hapticsOn`/`setHapticsOn`. The interface keeps audio's `: HapticsSetting`.
- `SettingsPreferences` gains `COLORBLIND_PALETTE_KEY = booleanPreferencesKey("colorblind_palette_on")` and `const val DEFAULT_COLORBLIND_PALETTE = false`.
- `SettingsRepositoryImpl`: the read is `dataStore.data.catch { emit(emptyPreferences()) }.map { it[COLORBLIND_PALETTE_KEY] ?: DEFAULT_COLORBLIND_PALETTE }`; the write is `storageFailSafe(fallback = Unit) { dataStore.edit { it[COLORBLIND_PALETTE_KEY] = on } }`. Same shape as the three flags.
- **The KDoc must say why this flag alone fails to `false`,** beside D-17: D-17 protects assists; this is a look; a corrupt store resets every setting and must not show the alternative look to every player whose store failed (§3.2). The class KDoc's "Every read fails open to `true`" becomes "every assist read fails open to `true`; the palette flag fails to its default, `false`".
- **Every test fake must grow the two members.** Sixteen test files define a local `: SettingsRepository` fake (`grep -rln ': SettingsRepository' app/src/test`): `SettingsPrivacyTest`, `SettingsViewModelSoundTest`, `SettingsViewModelTest`, `GameViewModelAdClockTest`, `GameViewModelTelemetryTest`, `GameViewModelTest`, `GameScreenSoundTest`, `GameViewModelAssistOrderingTest`, `GameViewModelHintStalenessTest`, `GameViewModelChallengeAdvanceTest`, `GameViewModelAllowanceTest`, `DockSpentAllowanceTest`, `GameViewModelSoundTest`, `GameViewModelResumeTest`, `GameFlowTest`, `SoundPoolPlayerTest`, plus any audio-review or talkback-board fake on the merged tree. Each addition is two lines (`override val colorblindPaletteOn = flowOf(false)` or a `MutableStateFlow`, and a no-op or recording setter). They are the branch's existing-test edits; the developer lists every file for the code-auditor. No default implementation goes on the interface: a source-of-truth interface with a silent default hides a missing override. (Folding the sixteen copies into one shared fake is R-8's hygiene item, not this branch's.)
- **Not in the cloud payload.** `CloudProfileStore`, `ProgressPersistenceKeys`, the profile-swap replace path and `ChallengeProgress` are not edited (CV-AC02; the key is device-local, CV-04). The code-auditor greps `colorblind` across `data/cloud*`, `platform/` and `ProgressPersistenceKeys.kt`: nothing.

**5.2 State — `ui/settings/SettingsUiState.kt`:** `val colorblindPaletteOn: Boolean = false`, with a KDoc entry. The class KDoc's "Both persisted flags default `true`" becomes "the two assist flags default `true` … the palette flag defaults `false` because its store default is `false` (§3.2): the pre-first-emission value equals the persisted default, so the switch never flickers either way".

**5.3 ViewModel — `ui/settings/SettingsViewModel.kt`:** a fifth collector in `init` (`settingsRepository.colorblindPaletteOn.collect { on -> _uiState.update { it.copy(colorblindPaletteOn = on) } }`) and `fun onToggleColorblindPalette(on: Boolean) { viewModelScope.launch { settingsRepository.setColorblindPaletteOn(on) } }`. **It touches `soundPlayer` in no way** — a toggle takes no cue (D-09; the sound row's confirmation pop is Sound's own case, `onToggleSound`'s KDoc). `SettingsRoute` passes `viewModel::onToggleColorblindPalette`.

**5.4 Screen — `ui/settings/SettingsScreen.kt`:**
- `SettingsScreen` gains `onToggleColorblindPalette: (Boolean) -> Unit`; `GeneralSection` gains `colorblindPaletteOn` and the callback and composes a fourth `SettingsToggleRow` directly after Haptics: `label = settings_colorblind_label`, `supporting = settings_colorblind_supporting`, `checked = colorblindPaletteOn`, `entranceOrder = COLORBLIND_ROW_ORDER`, `onCheckedChange = onToggleColorblindPalette`. The row's switch carries the label as its content description through the row's existing idiom, so TalkBack says "Colorblind palette, switch, off/on" (CV-AC01). No new focus stop, no sound, `settings_row_min_height` like its peers.
- **Entrance orders: a whole renumbering** (`SettingsScreen.kt:701-711`, its block KDoc explains the invariant). Without the licences screen on the tree: `GENERAL_HEADER 0, HOW_TO_PLAY 1, SOUND 2, HAPTICS 3, COLORBLIND_ROW_ORDER 4, PLAY_GAMES_HEADER 5, PLAY_GAMES_ROW 6, LEADERBOARDS 7, ACHIEVEMENTS 8, PRIVACY_HEADER 9, PRIVACY_POLICY 10, PRIVACY_CHOICES 11`. With `feature/licences-screen` already on the tree (`docs/ux/licences-screen.md` §2: About 8, Licenses 9, Privacy 10–12 on its own): `… COLORBLIND 4, PLAY_GAMES 5–8, ABOUT_HEADER 9, LICENSES_ROW 10, PRIVACY 11–13`. Either way Privacy stays last (D-14) and the KDoc's history paragraph gains one sentence for this row. **Whichever of the two branches lands second renumbers on top of the first** — a mechanical conflict in one constant block, resolved by writing the contiguous run above and re-running `SettingsScreenTest` (§10).
- The class KDoc's "two toggle rows that are exact peers: Sound and Haptics" becomes three.

**5.5 Strings — `res/values/strings.xml`,** one comment-headed block `<!-- Colorblind palette (docs/ux/colour-vision.md §3.1). -->` after the Settings strings: `settings_colorblind_label` "Colorblind palette", `settings_colorblind_supporting` "Clearer moves, hints and stars". US English like the file ("Catalog", "Licenses"); neither carries a word from the candy-backdrop BD-01 list, and once that branch's `UserFacingTextHasNoProtectedNamesTest` exists it scans them too. Entries are added, none edited or removed.

**5.6 The device-local rule, stated for review.** Restart, the tutorial, Skip, the profile swap and a cloud pull do not touch the key: none of them writes `SettingsRepository`, and the code-auditor confirms the only writer is `SettingsViewModel.onToggleColorblindPalette`.

## 6. Files
| File | New / edited | Holds |
|---|---|---|
| `ui/theme/Color.kt` | edited | `LightColorblindHint`, `LightColorblindStarFillColors` (§2.2) — the only hexes |
| `ui/theme/BoardMarks.kt` | new | `BoardMarkPalette`, `BoardMarks`, `LocalBoardMarks`, `boardMarksFor`, `BoardMarkPaletteKey` (§2.1) |
| `ui/theme/BoardMarkPaletteState.kt` | new | the `@Singleton` holder (§1) |
| `ui/theme/AppThemeRoot.kt` | new | `internal AppThemeRoot` (§1) |
| `ui/theme/Theme.kt` | edited | `AppTheme(boardMarkPalette)` and the `LocalBoardMarks` provider (§1) |
| `ui/theme/Clay.kt` | edited | `gradientFill(brush)` extracted; `goldGradientFill()` delegates (§2.6) |
| `ui/game/ColorblindBoardMetrics.kt` | new | the seven constants (§2.3) |
| `ui/game/BoardMarkRecipe.kt` | new | `BoardMarkRecipe`, `BoardMarkPalette.recipe`, `BoardMark`, `MarkLook`, `look()` (§2.4) |
| `ui/components/BoardGeometry.kt` | edited | the defaulted `recipe` parameter; three radii read it (§2.5) |
| `ui/components/DashedRing.kt` | new | `dashedRingStroke` (§3.3) |
| `ui/components/BoardCanvas.kt` | edited | the palette capture, `BoardPalette`'s fields, `drawSelectedPeg`'s halo, the `drawArc` rings, the dot alpha, the surface comment (§3) |
| `ui/components/BoardCanvasPreviews.kt` | new | the board previews, both palettes, light and dark (§3.11) |
| `ui/components/StarGlyph.kt` | new | `StarGlyph` and its previews (§4.1) |
| `ui/game/GameScreen.kt` | edited | one line in the board-state node's `semantics { }` (§3.9) |
| `ui/results/ResultsScreen.kt`, `ui/classic/BoardPreview.kt`, `ui/onboarding/TutorialScreen.kt` | edited | the star and pip call sites (§4.2) |
| `data/SettingsRepository.kt` | edited | the flag, key, default, KDoc (§5.1) |
| `ui/settings/SettingsUiState.kt`, `SettingsViewModel.kt`, `SettingsRoute.kt`, `SettingsScreen.kt` | edited | §5.2–§5.4 |
| `MainActivity.kt` | edited | one `@Inject` field, `AppThemeRoot` around `AppNavHost` (§1) |
| `res/values/strings.xml` | edited | the two strings, one block (§5.5) |
| `res/values/dimens.xml` | edited | `peg_selection_halo_width` (§2.7) |
| sixteen test fakes `: SettingsRepository` | edited | the two new members (§5.1) |

**Not edited:** `App.kt`, `di/*`, `navigation/*`, `ui/home/*`, `GameUiState.kt`, `GameViewModel.kt`, `TutorialViewModel.kt`, `TutorialUiState.kt`, `ResultsViewModel.kt`, `NoWinBanner.kt`, `WrongTapFeedback.kt` (unless it delegates to `dashedRingStroke`, §3.3), `BoardMetrics.kt`, `ChallengeProgress.kt`, anything under `data/cloud*`, `platform/`, `audio/`, `:domain`, `gradle/libs.versions.toml` and every build file, `AndroidManifest.xml`, `res/font/`, `res/values*/themes.xml`. No new dependency, no asset, no permission (§9 of the spec).

## 7. Tests: the CV-AC map, the contrast test, and what QA does on a device
Rules for every test (R5, rules/testing.md): deterministic — the Compose test clock (`mainClock.autoAdvance = false`, `advanceTimeByFrame()`) for anything animated, no wall clock, no sleeps; **JVM** means plain JUnit, no Robolectric; **Pixel** means the repo's `renderedWindow()` (`testing/RenderedWindow.kt`) under `@GraphicsMode(NATIVE)` on the suite's SDK, two renders compared inside one run, **no stored goldens** (tech design §8.5; A10 struck CV-AC05/06's "goldens" for this branch). Names read as sentences; each test is watched failing once before it is trusted.

**7.1 The CV-AC map.**

| # | Kind | Test (file under `app/src/test/java/com/app/pegsolitaire/`) | What it asserts |
|---|---|---|---|
| CV-AC01 | Compose + Device | `ui/settings/SettingsScreenTest` (extended, the rendered-top idiom): "the colorblind row sits directly after Haptics and above the Privacy header" — `header.y < howToPlay.y < sound.y < haptics.y < colorblind.y < privacyHeader.y` on the default `SettingsUiState` (Play Games is `Unknown`, so absent); "the colorblind row carries its own supporting line"; "flipping the colorblind switch reports the new value". `ui/settings/SettingsViewModelSoundTest` (extended, the `ButtonSoundTest` idiom): "toggling the colorblind palette plays no cue" — the fake `SoundPlayer` records nothing. QA: TalkBack reads "Colorblind palette, switch, off/on"; 200 % wraps the supporting line, light and dark |
| CV-AC02 | JVM + Device | `data/SettingsRepositoryTest` (extended): "the colorblind palette is off on a fresh store", "a failed read leaves the colorblind palette off" (the `throwingDataStore()` of the file), "setColorblindPaletteOn round-trips", "a failed write drops the colorblind choice without throwing". `ui/settings/SettingsViewModelTest` (extended): "the colorblind switch defaults off before the store emits", "onToggleColorblindPalette persists and the state follows". Review: the key is `colorblind_palette_on`; no cloud or swap code names it. QA: flip, kill (`am force-stop`), relaunch; "Use their cloud progress" leaves it |
| CV-AC03 | Compose + Device | `ui/game/ColorblindPaletteLiveSwitchTest` (new): `AppTheme(boardMarkPalette = state) { GameScreen(midGame …) }` with `state` a `mutableStateOf(Standard)`; the `GAME_STATE_TEST_TAG` node's `BoardMarkPaletteKey` reads `Standard`; set `Colorblind` → it reads `Colorblind` without `setContent` again, and one ring pixel changed (Pixel). `ui/theme/AppThemeRootTest` (new): `AppThemeRoot(MutableStateFlow(Colorblind))` with the clock paused — the **first** frame's reader sees `Colorblind` (property 2, no Standard frame); then a later `flow.value = Standard` reaches the reader (property 3); a probe reading `LocalBoardMarks` outside any board is the reader. QA: flip with a game saved, back, resume |
| CV-AC04 | Review + Check | the Check; `git diff develop...HEAD -- app/src/main | grep -nE '0x[0-9A-Fa-f]{6,8}' | grep -v Color.kt` prints nothing; `grep -nE '[0-9.]+\.(dp|sp)\b'` over the diff's new Kotlin prints nothing; no new `@Suppress`; the local is read only through `LocalBoardMarks.current` (grep for `boardMarksFor(` outside `Theme.kt`, `BoardMarks.kt` and tests: nothing) |
| CV-AC05 | JVM + Pixel + Device | `ui/game/BoardMarkRecipeTest` (new, JVM): "the standard recipe is today's board metrics" — each field equals the `BoardMetrics` constant, `hintRingDashes == 0`, `halo == false`; "the colour-blind recipe is §3.4" — the seven `ColorblindBoardMetrics` values and `halo == true`. `ui/theme/BoardMarksTest` (new, JVM): "the standard marks are today's tokens in both themes" — `primary`, `tertiary`, the gold gradients, `outlineVariant`, no halo, no outline — values written in the test; "the colour-blind marks are §3.4's table in both themes". `ui/components/BoardCanvasPaletteTest` (new, Pixel): `GameScreen` mid-game (English-33, a selection, Show-moves on, a hint) rendered under `Standard` and under `Colorblind` in one run: every pixel outside the slab's bounds (`GAME_BOARD_SLAB_TEST_TAG`) is identical; inside, the pixel at the selection ring's inner-edge radius is `surfaceContainerLowest` only under `Colorblind`; a legal-target pixel is opaque `primary` only under `Colorblind`; the pixel at the top of the hint source ring (−90°) is amber under `Colorblind` and the pixel a quarter-dash later is slab (the gap). The same for Wiegleb-45 at `w360dp-h640dp`. QA compares the off-palette board against `develop` on the device |
| CV-AC06 | Pixel + Review | `ui/home/HomePaletteIdentityTest` (new): `HomeScreen` in each of its three chip states rendered under `Standard` and `Colorblind`, light and dark, pixel-identical. Review: `HomeStage.kt`/`HomeBoardArt.kt` unchanged; `BoardCanvas` reads the local only when `interactive` (§3.1); `MainActivity`'s root collection is the one new start-path read and it blocks nothing (D-03g) |
| CV-AC07 | JVM | `ui/theme/ColorblindPaletteContrastTest` (new): §7.2 below |
| CV-AC08 | JVM | `BoardMarkRecipeTest`: "every pair of marks that can share a board differs in a channel that is not hue" over `BoardMark.entries.pairs()` × both recipes' `look()` (form, radius ratio, dashes, halo, filled; the `DockLookRecipeTest` shape); "a hint ring is larger than the selection ring in both palettes" (`hintSourceRingRatio > selectedRingRatio`); `BoardCanvasTest` (extended, JVM): the existing distinctness cases repeated with `BoardGeometry.forCanvas(spec, size, Colorblind.recipe)` — 0.42/0.44/0.30 × cellSize |
| CV-AC09 | Compose + Device | `ui/components/BoardMarkMotionTest` (new): with the animator scale at 0 (`Settings.Global`, what `rememberAnimationsEnabled()` reads) a `Colorblind` board renders byte-identically on two frames `advanceTimeByFrame()` apart with a selection and a hint (no pulse, no pop, dashes static); at scale 1 the hint-ring pixels are identical across frames while the selection ring's radius changes (the existing pulse only). QA: scale 0 and 1 on the device |
| CV-AC10 | existing suites + Compose + Device | `BoardCanvasTest`, `DockLookRecipeTest`, `GameFlowTest`, `ButtonSoundTest`, the phase-15 suites (15g–15r, 18a–18e, 21a–21g), audio's `BoardCanvasWrongTapTest`/`BoardCanvasWrongTapReducedMotionTest`/`GameScreenWrongTapTest`, talkback-board's `BoardSemanticsTest` if on the tree — all green, none edited (the sixteen fakes excepted, §5.1). `ColorblindPaletteLiveSwitchTest` also asserts `PegCountKey`, `LegalTargetCountKey` and `EarnedStarCountKey` unchanged across a flip. QA: TalkBack traversal on gameplay, tutorial, Results, catalogue; 21a–21g in both palettes |
| CV-AC11–14 | Device | §7.3 |
| CV-AC15 | Review | two strings; `BoardCanvasPreviews.kt`, `StarGlyph.kt` previews light and dark, both palettes; `AppThemeRoot` is `internal` (no preview owed); the BD-01 grep over the two strings prints nothing |

**7.2 `ColorblindPaletteContrastTest` — re-proving §3.5 on every Check.** A plain JVM test over `Color.kt`'s vals and `boardMarksFor(…)` (`androidx.compose.ui.graphics.Color` and material3's `ColorScheme` are plain Kotlin; `BackdropColorsTest` on candy is the same kind). It holds, as named constants with their source in KDoc:
- the sRGB ↔ linear transfer functions and WCAG relative luminance `Y = 0.2126 R + 0.7152 G + 0.0722 B`; contrast `(L1 + 0.05) / (L2 + 0.05)`;
- **alpha blending in gamma-encoded sRGB** over the backing colour (what `drawCircle(alpha = …)` does), applied to the standard palette's translucent marks before any simulation;
- the **Machado, Oliveira and Fernandes (2009) severity-1.0 matrices**, applied to linear RGB, and the luminance-only transform (monochromacy). The published severity-1.0 rows, row-major, are — protanopia `[0.152286, 1.052583, −0.204868; 0.114503, 0.786281, 0.099216; −0.003882, −0.048116, 1.051998]`, deuteranopia `[0.367322, 0.860646, −0.227968; 0.280085, 0.672501, 0.047413; −0.011820, 0.042940, 0.968881]`, tritanopia `[1.255528, −0.076749, −0.178779; −0.078411, 0.930809, 0.147602; 0.004733, 0.691367, 0.303900]`. The developer transcribes them from the paper's severity-1.0 table and treats the agreement with §3.5's figures as the transcription check: each computed ratio must land within **±0.05** of the spec's cell (rows 1, 2, 4, 5, 9, 10, 11, 12, all five columns), and a transcription slip shows as a miss, not as a silent pass;
- **the sampled peg edge (row 3, information):** the peg's radial gradient (`PEG_GRADIENT_OFFSET_RATIO`, `PEG_GRADIENT_RADIUS_RATIO`, `PEG_GRADIENT_MID_STOP`, the three `PegGradient` stops, interpolated in sRGB) evaluated every 15° on the circle of radius `pegRadius`; the test asserts the standard ring against that edge is ≤ 1.2:1 somewhere (the finding) and records the halo's worst against the edge as ≥ the spec's figures.

Its cases: "every colour-blind mark reads 3:1 against its neighbours in every simulated vision" (rows 1, 2, 4, 5, 9, 10, 11, 12 ≥ 3.0 under normal, protanopia, deuteranopia, tritanopia, monochromacy, both themes); "the halo separates the ring from the peg's shaded edge"; "the computed ratios match the spec's table within 0.05". The title clause of CV-AC07 is candy-backdrop's `TitleGradientContrastTest` (A10) and is not written here.

**7.3 What QA does on a device.** The qa-engineer's half, on the Pixel_9a — or the Pixel_9, `emulator-5560`, with the deviation recorded (spec §5) — plus the 360×640dp window (`wm size 1080x1920`, `wm density 480`); light and dark, font scale 1.0 and 2.0, animator scale 1 and 0, TalkBack on for the Settings and board checks. **Never `Pixel_9a_bench`**, which is 15e's instrument and used for nothing else (tech design §10). Artifacts under `.planning/qa/colour-vision/`, never committed; the report lists each by path and size.
- **CV-AC11 — simulated deficiencies on the device.** Palette on. Developer options › Hardware-accelerated rendering › **Simulate color space**: Monochromacy, Deuteranomaly, Protanomaly, Tritanomaly, each in light and dark. The fixed scenario of the spec: English-33 mid-game with a selection and Show-moves on; a revealed hint; Wiegleb-45 on the 360×640 window with a selection and a hint; Results with two of three stars; the catalogue pips; tutorial step 2. Per capture QA states each distinction and the cue it used (halo, opaque dot, dashed ring, outline, fill), and the off-palette baseline is recorded, not graded. QA records whether `screencap` shows the simulation; if not, it judges on the device and leans on CV-AC12.
- **CV-AC12 — from screenshots.** Plain captures (simulation off) transformed offline with the three severity-1.0 matrices and the luminance transform by a helper script under `.planning/qa/colour-vision/` (a Python script is fine; it is never committed); stroke-centre pixels of the ring, the halo, a dot, both hint rings, the star outlines and fill, and the adjacent slab or background; each pair within 0.3 of §3.5 or above, every ≥ 3:1 row ≥ 3:1.
- **CV-AC13 — dashes at the smallest size.** Wiegleb-45, 360×640, palette on, monochromacy: a zoomed capture shows eight separate dashes on each hint ring and a solid ring with a light inner band on the selection.
- **CV-AC14 — with the system's colour correction.** Accessibility › Color and motion › Color correction in each mode, palette on; CV-AC11's distinctions still hold. A failure here is reported to the ux-designer through the producer and does not block the merge (spec).
- **CV-AC01/02/03/09/10's device halves** as in the table, and the Settings row at 200 % in both themes.
- **The 15e sitting** (§9) is a separate session on the bench, after the code-auditor's PASS.

## 8. Commit order, test-first
Each commit is written test-first: the red test is added and seen red on the code before it; then the code lands and the commit is green on the Check — `./scripts/check.sh` once check-gates is on `develop`, else `./gradlew check` (check-gates also makes `:app`'s main warnings errors, C7: fix a warning in the code, never with a suppression or a compiler switch). Conventional subjects; one trailer `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>` (rules/gitflow.md). Load `compose-skill` first (rules/compose.md), then `compose-ui`, `android-viewmodel`, `android-coroutines`, `android-testing`, `superpowers:test-driven-development`.

| # | Commit | Red first | Then |
|---|---|---|---|
| C0 | `chore(colour-vision): merge develop` | — | §0's precondition (`git merge-base --is-ancestor <audio-review merge commit> HEAD` exits 0); the Check green on the merged tree, recorded as the baseline |
| C1 | `feat(colour-vision): palette tokens, marks and the recipe` | `BoardMarkRecipeTest`, `BoardMarksTest`, `ColorblindPaletteContrastTest`, the `BoardCanvasTest` extension | `Color.kt`, `ColorblindBoardMetrics.kt`, `BoardMarkRecipe.kt`, `BoardMarks.kt`, `Theme.kt`, `Clay.kt`'s extraction, `BoardGeometry.kt`, `dimens.xml` |
| C2 | `feat(colour-vision): the colorblind palette setting` | the `SettingsRepositoryTest`, `SettingsViewModelTest`, `SettingsViewModelSoundTest` and `SettingsScreenTest` extensions | `SettingsRepository.kt`, `SettingsUiState.kt`, `SettingsViewModel.kt`, `SettingsRoute.kt`, `SettingsScreen.kt` (row + renumbering), `strings.xml`, the sixteen fakes |
| C3 | `feat(colour-vision): collect the palette once at the root` | `BoardMarkPaletteStateTest` (JVM: `Standard` before the store's first emission — a repository fake whose flow never emits; follows the store; `Colorblind` after `setColorblindPaletteOn(true)` through `SettingsRepositoryImpl` over `InMemoryPreferencesDataStore`, on a test scope), `AppThemeRootTest` | `BoardMarkPaletteState.kt`, `AppThemeRoot.kt`, `MainActivity.kt` |
| C4 | `feat(colour-vision): draw the colour-blind board marks` | `BoardCanvasPaletteTest`, `BoardMarkMotionTest` | `DashedRing.kt`, `BoardCanvas.kt`, `GameScreen.kt`'s key, `BoardCanvasPreviews.kt` |
| C5 | `feat(colour-vision): stars and pips in the active palette` | `StarGlyphTest` (Compose + Pixel: Standard earned = today's gold and no outline node drawn; Colorblind light = amber fill and an outline; unearned tint per palette; `EarnedStarCountKey` and the pip rows' descriptions unchanged) | `StarGlyph.kt`, `ResultsScreen.kt`, `BoardPreview.kt`, `TutorialScreen.kt` |
| C6 | `test(colour-vision): live switch and Home identity` | — | `ColorblindPaletteLiveSwitchTest`, `HomePaletteIdentityTest` (tests only; both red against a deliberately broken local for one run, then green) |
| C7 | `docs(colour-vision): developer report` | — | `.planning/reports/colour-vision/android-game-dev.md` (rules/reporting.md): every file of §6 touched, the sixteen fakes, any `WrongTapFeedback.kt` delegation, the Check output, the §11 tripwire output, the scenarios QA drives |

C1 and C2 are independent; C3 needs C1 and C2; C4 and C5 need C1; C6 needs C3–C5. Run the phase-15 suites and audio's board tests unchanged after C4 and C5.

**Stop and report, do not work around:** a phase-15, audio or talkback test fails or would need an edit to pass (a defect here, or a spec conflict for the producer); a §3.4 value cannot be drawn as specified at some cell size (report the number; never retune — the ux-designer owns it); `drawArc` with a dash effect does not start at the top on the pinned Compose BOM (then `drawCircle` with a phase of a quarter circumference, and say so); the base lacks audio-review; anything needs a new dependency, an asset, a `di/` or `navigation/` edit, or a third string.

## 9. The 15e sitting
**Why this branch sits one.** Tech design §10's trigger is "a later branch that changes Home's first frame", with the start-path list as its proxy; this branch edits `MainActivity.kt` and `ui/theme/` and starts one collection at the Activity's root (§1). Home's composition is unchanged (CV-AC06), so the expected delta is the holder's construction plus one subscription on an already-read store — well inside the threshold — but expectation is not a reading, and the hint and talkback briefs got their "no sitting" by touching no start-path file; this one does. A11 and §14.1 row 6b already say so.

**The sitting, as `docs/ux-spec.md` §6.5 (second edition, as amended on `feature/ux-spec-folds`) rules it**, run by the qa-engineer with "The procedure the qa-engineer runs", `.planning/reports/15-founder-feedback/tech-lead-fonts.md` Ruling 2, as amended by `15-founder-feedback/tech-lead-signoff.md` Decision 1 (P-1 to P-4) and `tech-lead-signoff-close.md` D-1:
- **Instrument:** `Pixel_9a_bench` (precondition 1): the `Pixel_9a`'s profile and image (`android-36.1` `google_apis_playstore_ps16k`, arm64, 16 KB pages, 4 cores, 2 GB RAM, 1080×2424 at 420 dpi) with a 16 GB data partition, no Google account, used for no other testing; its `config.ini` diff against `Pixel_9a.avd` recorded (only `AvdId`, `avd.ini.displayname`, `disk.dataPartition.size` and the `.ini` paths may differ; any extra key makes the sitting VOID). The bench is not rebuilt.
- **Arm A = `git merge-base develop feature/colour-vision` at the sitting; arm B = the branch head.** A must contain phase 15 and audio-review (`git merge-base --is-ancestor <each merge commit> <A>` exits 0). Then `A..B` is exactly this branch's diff, whatever else `develop` has gained.
- **When:** last before the merge: after the code-auditor's PASS on the final diff, after QA's device pass, and after the branch has merged the `develop` of that day. A later `develop` merge-in leaves it standing unless it changes a start-path file (`App.kt`, `MainActivity.kt`, `di/`, `navigation/`, `ui/home/`, `ui/theme/`, `res/font/`, every `res/values*/themes.xml`, `AndroidManifest.xml`); a test-only commit leaves it standing. Note for the producer: the licences branch edits `navigation/` and talkback-board edits none of the list, so a merge-in of licences after the sitting forces a re-sitting and one of talkback does not.
- **Builds:** each arm with `:app:assembleDebug` in its own detached worktree (`git worktree add --detach ../PegSolitarie-armA-cv <A>`, likewise `armB-cv`), daemons stopped before the sitting; each installed only at a block boundary (`adb install -r -d --no-incremental`, `cmd package compile -m speed -f com.erman.pegsolitarie`, `am force-stop`), the installed `base.apk`'s sha256 recorded after every swap (precondition 7).
- **Conditions (preconditions 1–9, each recorded with its value):** animations off; exactly one emulator (the `Pixel_9a` and the `Pixel_9` both shut down); no Gradle build in any worktree and no other session building or measuring (the hint, audio, gates, talkback and candy work pause); `uptime` before and after with the 1-minute load ≤ 5.0; ART normalised on both arms (a debuggable APK stays `[status=verify]`; equal status is the condition); the launch component resolved from the device (`cmd package resolve-activity --brief com.erman.pegsolitarie`); block-boundary swaps; the seeded ~200-level Challenge ladder on both arms; `/data` free ≥ 4 GB at both ends.
- **Protocol:** the null control first (the same build in both positions, `floor = |median difference|`), then A/B/A/B in blocks of five, ≥ 20 kept samples per arm, the first of each block discarded, `am force-stop` plus 2 s between samples.
- **Verdict:** `median_B − median_A ≤ max(2 × floor, 100 ms)` PASS; above it FAIL; `floor > 150 ms` VOID (re-run, never reported as either).
- **Evidence:** `.planning/reports/colour-vision/qa-engineer-15e.md` in §6.5's field shape (one table per arm, the null-control table, the nine preconditions with values, both `uptime`s, each `base.apk` sha256, the `config.ini` diff, both `Data-Free` readings, `floor` and the threshold). A sitting without its null table is VOID.
- **No jank block.** Candy's BD-23 (c) exists because that branch adds animation; this branch adds none (the dashes and the halo are static, §3.6), so no separate `gfxinfo` block is owed.

**Ruling — §6.5 precondition 7's validity clause and the key this branch adds.** The clause reads "valid only while both arms share the Room schema and the DataStore keys". Its purpose is that the two arms start against the same persisted state through the block-boundary swaps (one `applicationId`, one data directory): a schema difference would run or skip a migration on one arm, and a key one arm writes and the other reads would make the arms' start paths differ by persisted data. This branch defines a new key, `colorblind_palette_on`, which arm A does not know. **The clause is met, and the sitting is valid, when the persisted store contains no key either arm does not know** — that is, when `colorblind_palette_on` is never written during the sitting: the scenario never opens Settings, and the switch is at its default, so the key is absent and both arms read byte-identical files. A key defined by one arm but written by neither is shared trivially. The qa-engineer records, at the start and the end of the sitting, that `adb shell run-as com.erman.pegsolitarie sh -c 'strings files/datastore/app_prefs.preferences_pb' | grep -c colorblind_palette_on` prints `0` (the debug build allows `run-as`; if the store's path differs, the report names the one it checked), and that `git diff <A>..<B> -- app/schemas` is empty. A sitting in which the key was found written is VOID. This reading is a tech-lead decision; it is recorded here and owed to tech design §10 as a clarification of the clause (Handoff). Candy's A9 reasoning ("a new key would void the sitting") was written against the literal clause and is superseded by this reading; nothing on candy changes, since candy adds no key.

**If it fails.** There is no art to remove: §6.5's degradation path is Home's, and this branch draws nothing new on Home. A FAIL is first re-sat once under the same protocol (the null control bounds noise, but a second reading is cheap). A confirmed FAIL goes to this role with the numbers before anything is changed. The one pre-ruled one-line change, if the numbers point at the root collection: `SharingStarted.Eagerly` → `SharingStarted.Lazily`, which moves the subscription from `onCreate`'s injection to the root's first `collectAsStateWithLifecycle` and keeps all three properties (the `StateFlow` retains its value across recreation either way). Nothing else is pre-ruled.

## 10. The in-flight branches, the merge order, and what the second branch does
**10.1 What each in-flight tree touches that this branch touches** (read from the worktrees on 2026-10-24: `PegSolitarie-audio` `ae58e5e`, `PegSolitarie-hint` `53bd85f`, `PegSolitarie-tb` `23ca39a`, `PegSolitarie-lic` `0bf578d`, `PegSolitarie-ux` `ef721dd`, `PegSolitarie-gates` `77dcf88`, `PegSolitarie-candy` `fd56cce`).

| Branch | State | Shared with this branch | Kind of overlap, and the resolution |
|---|---|---|---|
| `feature/audio-review` (step 5; auditor PASS `code-auditor-integration.md`, QA/sound/ux reviews pending) | in review | `BoardCanvas.kt` (its `wrongTapCorrection`, `BoardPalette.mark`/`markStroke`, `rememberWrongTapFeedback`, the `shaken` centres, `drawSelectedPeg(…, nudge)`, `drawWrongTapMark` before the hint overlay, the dot-pop key); `SettingsRepository.kt` (`: HapticsSetting`, `override val hapticsOn`); `MainActivity.kt` (V1: one import, one KDoc paragraph, `volumeControlStream = STREAM_MUSIC` at the top of `onCreate`); `dimens.xml` (`wrong_tap_mark_width` in the stroke block); `strings.xml` (three `wrong_tap_*`); `BoardMetrics.kt` (four wrong-tap constants; not edited here); `DataModule.kt` (`bindHapticsSetting`; not edited here) | **A dependency, not a conflict:** §3.5's `nudge`, §3.3's stroke idiom and §2.7's placement are written against audio's tree, so audio-review is on `develop` before C0 (§0). Had audio not merged, this branch's `drawSelectedPeg` would have no `nudge` and its halo would detach when audio landed: hence the hard order |
| `bugfix/hint-undecided` (step 3a; auditor PASS, QA next) | in review | `strings.xml`, `dimens.xml` (blocks), `GameScreen.kt` (its notice slot in the board `Box`; this branch's one semantics line), `GameScreenPreviews.kt` (its; this branch adds none there), `GameUiState.kt` (its `hintUndecided`; not edited here) | Adjacent hunks, keep-both; no behaviour overlap — the notice draws no board mark and the palette changes no state |
| `feature/talkback-board` (step 5a; brief only, builds after audio, hint and ux-spec-folds are on `develop`) | brief | `BoardCanvas.kt`: talkback owns the composable's **tree** — the `if (!interactive)` first branch, the `Box`, the Canvas chain, `.boardAnnouncement(announcement)`, the overlay, `focusable`, the `announcement` parameter — and freezes the file from `drawBoard` down (its §12); this branch owns the **palette capture and the draw functions** (§3) and edits nothing of the tree. `GameScreen.kt`: talkback adds `isTraversalGroup = true` and the slab's `boardStop`/`traversalIndex`; this branch adds one key in the same `semantics { }` block. `ResultsScreen.kt` (its TB-8 summary and star-row description; this branch's `StarGlyph` call sites in `StarRow`/`EarnedStar`), `TutorialScreen.kt` (its three private effects; this branch's pips and star), `strings.xml` | **Different regions of the same three files; either may merge first.** Talkback's 13.2 pins itself before 6b and this brief honours it: this branch merges after talkback-board. If talkback is held (a BLOCKED TalkBack sitting, Q-023's Gate-2 clause) the producer may invert the order, and talkback then does the reverse case below. Both resolutions are written out in 10.3 |
| `feature/licences-screen` (step 6a; docs only, LN build pending) | spec | `SettingsScreen.kt`'s constant block and `GeneralSection`/`PlayGamesSection` boundary (its About section and `onOpenLicenses`; this branch's row and renumbering), `SettingsRoute.kt` (its `onOpenLicenses`; this branch's toggle), `strings.xml`, `navigation/` (its only) | **Whichever lands second renumbers on top:** the contiguous run of §5.4 with both rows present; re-run `SettingsScreenTest` and licences' LS-AC01 case. Keep-both in the route's parameter list |
| `feature/phase-14-check-gates` (step 4; auditor PASS, QA next) | in review | no file. Semantic: C7 (`:app` main warnings as errors), C2 (`./scripts/check.sh`), C6 (kover on `:domain`, untouched here) | This branch's code is warning-free from C1 on; the second to merge re-runs the Check under the other's rule |
| `feature/ux-spec-folds` (`docs/ux-spec.md`, `DESIGN-SYSTEM.md`) | in check (loop 5 closed) | none in code. §6.5 as amended is what QA reads for §9; D-TB is talkback's | The folds land before this branch's sitting (tech design §16 #11). A conflict in `docs/ux-spec.md` is the ux-designer's, never a developer's |
| `bugfix/home-chip-arrival` (step 6; not cut) | pending | `ui/home/**` (not edited here) | none; `HomePaletteIdentityTest` renders whatever `HomeScreen` is on the tree |
| `feature/candy-backdrop` (step 7, last) | spec | `Clay.kt` (its `titleGradient`/`titleGradientBrush()` beside `ctaGradientBrush()`; this branch's `gradientFill` extraction beside `goldGradientFill`), `Theme.kt` (its `LocalBackdropColors`/`LocalFinishColors` providers; this branch's `LocalBoardMarks`), `GameScreen.kt`, `TutorialScreen.kt`, `ResultsScreen.kt`, `SettingsScreen.kt` (its container colours), `dimens.xml`, `strings.xml` | Candy merges last and takes `develop` in: keep both providers in one `CompositionLocalProvider`, both `Clay.kt` hunks, both blocks. BD-17's "board untouched" is candy's own constraint; this branch's board edits are in candy's arm A |
| `feature/play-upload` (step 6c) | pending | nothing | none |

**10.2 Merge order.** Tech design §14.1's list with 6b refined: 3a hint-undecided → 4 check-gates → **5 audio-review (hard: before C0)** → 5a talkback-board → 6 / 6a / 6c free among themselves → **6b this branch** → 7 candy-backdrop. Two rules are hard: **C0 only once audio-review is on `develop`**, and **this branch's merge comes before candy-backdrop's 15e sitting** (A11: "either before candy's sitting or after candy merges, never between"; since candy is last and this branch edits `ui/theme/`, "before the sitting" is the only placement that does not force candy to re-sit). Its place against 6, 6a and 6c is free; against 5a it follows talkback unless the producer inverts (10.1). Building may start the day audio-review merges, in parallel with talkback-board in its own worktree (rules/studio.md, Parallel work: two developer dispatches, two worktrees, not two gsd phases).

**10.3 What the branch merging second does.** For every pair: `git merge develop` (never a rebase), resolve as below, re-run its own Check and the other branch's named tests, and have its tech-lead sign-off re-read the merge commit's conflict hunks.

*This branch after talkback-board (the normal case):*
- `BoardCanvas.kt`: keep talkback's tree exactly — the `if (!interactive)` branch **first**, composing today's Canvas; then the `Box`, the Canvas chain with `.boardAnnouncement(announcement)`, the overlay. Move §3.1's palette read inside the interactive branch and drop the `null` case (`val marks = LocalBoardMarks.current`); the `remember`ed geometry with the recipe, the strokes and `BoardPalette` follow it; §3's draw functions below `drawBoard` are this branch's and talkback has no hunk there. The overlay's own `BoardGeometry.forCanvas(spec, size)` keeps the default recipe: cell centres do not read it.
- `GameScreen.kt`: one `semantics { }` block with `pegCountValue`, `gameStatusValue`, `legalTargetCountValue`, talkback's `isTraversalGroup = true`, and this branch's `boardMarkPaletteValue`.
- `ResultsScreen.kt`: talkback's merged description and summary stay on the row; `StarGlyph` inside. `TutorialScreen.kt`: talkback's effects and `rememberBoardAnnouncer()` beside this branch's pips.
- Then run talkback's `BoardSemanticsTest`, `GameScreenTalkBackTest`, `TutorialTalkBackTest`, `ResultsScreenTalkBackTest`, and this branch's `BoardCanvasPaletteTest`, `ColorblindPaletteLiveSwitchTest`, `StarGlyphTest`, `HomePaletteIdentityTest`.

*Talkback-board after this branch (the inverted case):* talkback places its `if (!interactive)` branch above this branch's `val marks = if (interactive) …` read and simplifies that read to the unconditional form inside its interactive branch; it keeps `BoardGeometry.forCanvas(spec, canvasSize, recipe)` and the strokes where this branch put them; its §12 freeze ("no hunk below `Canvas(`") is read against the merged base, which now contains this branch's draw functions. It re-runs this branch's four tests above.

*This branch after licences-screen, or the reverse:* §5.4's second numbering; both rows; `SettingsRoute`/`SettingsScreen` parameter lists keep both callbacks; `SettingsScreenTest` and licences' rendered-top cases green.

*This branch after check-gates, or the reverse:* `./scripts/check.sh`; a `w:` in this branch's files is fixed in the code (A3's two mechanical kinds their way; anything else, stop and report); C6 is unaffected (no `:domain` change).

*This branch after hint-undecided:* keep-both blocks in `strings.xml`/`dimens.xml`; the notice slot and the semantics line are different hunks of `GameScreen.kt`; run `GameScreenHintNoticeTest` and `NoWinBannerTest`.

*Candy-backdrop after this branch (always):* as 10.1's row; run `BoardInteriorIdentityTest` with the palette **on and off** (BD-17's "runs once per palette", colour-vision §3.7), `TransparentContainersTest`, and this branch's `HomePaletteIdentityTest`; candy's sitting then has this branch in arm A.

## 11. What must not change
**Frozen on this branch** (the code-auditor checks each with the command given):
- **The standard look** (CV-13): with the palette off every screen is pixel-identical to the base. `BoardMarkRecipeTest`'s Standard pin, `BoardMarksTest`'s token pin and `BoardCanvasPaletteTest`'s outside-the-slab identity hold it; QA compares against `develop` on the device. The standard palette's six shortfalls (spec §7) are **not** fixed here: that is the ux-designer's drafted founder question, built on option 1.
- **Pegs, holes, the specular dot, the jumper, the removal fade, the landing squash** (CV-06): `drawCandyPeg` and `drawDimple` have no diff: `git diff develop...HEAD -- app/src/main/java/com/app/pegsolitaire/ui/components/BoardCanvas.kt | grep -nE '^[-+].*(drawCandyPeg|drawDimple)\(' ` shows only call sites whose arguments are unchanged.
- **The dock, the backdrop, Home, the CTA gradient, every text colour, the dialogs, the banner and the notice, the ribbon's gilded star, `AdOfferDialog`'s gold** (CV-11, CV-12a, CV-05, §3.7): `git diff --stat develop...HEAD` shows nothing under `ui/home/`, `ui/backdrop/`, `GameActionDock.kt`, `DockLook.kt`, `NoWinBanner.kt`, `HintUndecidedNotice.kt`, `AdOfferDialog.kt`, `HomeHero.kt`; `Clay.kt`'s diff is the `gradientFill` extraction only; `ctaGradientBrush()` and its callers are untouched.
- **The tap path and the geometry:** `BoardGeometry.center`, `positionAt`, `cellSize`, `pegRadius`, `holeRadius`, `destinationDotRadius` unchanged (the file's only hunk is the recipe parameter and the three ring properties); `pointerInput(geometry) { detectTapGestures … }` unchanged; `GameViewModel.onCellTap` and `TutorialViewModel.onCellTap` no diff.
- **Every ViewModel but Settings, every `UiState` but Settings, `:domain`, the generator, the solver, the books, the assist and ad paths, the timer, the resume codec, the cloud payload and the profile swap:** no diff (`git diff --stat develop...HEAD -- domain app/src/main/java/com/app/pegsolitaire/data/cloud app/src/main/java/com/app/pegsolitaire/platform` is empty; `GameViewModel.kt`, `GameUiState.kt`, `TutorialViewModel.kt`, `ResultsViewModel.kt`, `ChallengeProgress.kt`, `ProgressPersistenceKeys.kt` have no hunk).
- **The read-only board:** `BoardCanvas(interactive = false)` reads no palette and draws today's pixels (§3.1; `HomePaletteIdentityTest`).
- **Audio's wrong-tap feedback:** the shake, the redirect, the nudge, the grey 12-dash mark, the reject haptic and the announcement are unchanged in both palettes; audio's three board tests run unedited.
- **Semantics and targets** (CV-AC10): no new focus stop, no new description, no touch-target change; `PegCountKey`, `LegalTargetCountKey`, `EarnedStarCountKey`, `DockLookKey` and every content description unchanged; the one new key is `BoardMarkPaletteKey`.
- **Motion** (CV-AC09): nothing new animates; `rememberAnimationsEnabled()` gates what it gates today.
- **Audio** (rules/audio.md): no cue added, none changed; the toggle is silent.
- **Tokens and literals** (CV-AC04): hexes only in `Color.kt` (two); dp only in `dimens.xml` (one); ratios, alphas and the dash count in `ColorblindBoardMetrics`; `RING_START_ANGLE_DEG`/`FULL_SWEEP_DEG` as named privates. No `@Suppress`, no baseline, no `@Ignore`, no `!!`, no reflection; the two existing `LongMethod` suppressions keep their one annotation each.
- **Strings and dimens:** entries added, none edited or removed: `git diff develop...HEAD -- app/src/main/res/values/strings.xml app/src/main/res/values/dimens.xml | grep -E '^-[^-]'` prints nothing.
- **No new dependency, asset, permission, manifest entry, `di/` or `navigation/` edit, build-file edit** (spec §9; `git diff --stat develop...HEAD` against §6's list).
- **Existing tests:** none edited except the sixteen fakes' two-member additions and the named extensions of §7 (additions only); the developer's report lists every existing test file touched, and the code-auditor approves the list.

**The tripwire for §9, both lines expected to print** (so the sitting is owed; the report quotes the output): `git diff --name-only develop...HEAD | grep -E '^app/src/main/java/com/app/pegsolitaire/(MainActivity\.kt|ui/theme/)'` prints `MainActivity.kt`, `Theme.kt`, `Clay.kt`, `Color.kt` and the three new theme files and nothing else in the start-path list; `git diff --name-only develop...HEAD | grep -E '^app/src/main/(java/com/app/pegsolitaire/(App\.kt|navigation|ui/home|di)/|AndroidManifest\.xml|res/(font|values[^/]*/themes\.xml))'` prints nothing.

## 12. Merge instructions and done
**Done, for the developer:** the Check green on the merged tree; every automated row of §7.1 exists and was watched failing once; no test disabled or edited to pass; the report at `.planning/reports/colour-vision/android-game-dev.md` (rules/reporting.md) with §6's file list, the fakes, the tripwire output, the §9 scenario QA will drive, and any `WrongTapFeedback.kt` delegation named.

**The review chain** (rules/gitflow.md, rules/studio.md): code-auditor on the diff with CV-AC01 to CV-AC15 as the acceptance list, §11's commands and A10's kinds (the contrast test's matrices read against the paper; the Standard pins; the sixteen fakes approved as the only existing-test edits; the `StarBorder` reading of §4.1) → qa-engineer on the device (§7.3) → the **15e sitting on the bench** (§9), last, after the auditor's PASS and the `develop` merge-in → tech-lead sign-off (`.planning/reports/colour-vision/tech-lead.md`), which re-reads the merge commit's conflict hunks of 10.3 and cites the auditor's, QA's and the sitting's reports by path.

**The merge:** `git merge --no-ff feature/colour-vision` into `develop` by the producer, serial, after the four conditions (Check green, auditor PASS, QA PASS with the sitting PASS, sign-off PASS), in the slot of 10.2; then the branch merging after it does 10.3's duties. No branch-cut marker: this branch has none in §14.1 (the note above its table), and whether `release/3.0.0` waits for it is the producer's schedule in STATUS.md; the brief's recommendation is that it ships in 3.0.0, since it closes a rules/compose.md requirement and waits on nothing.

**Owed after the merge, by owner (not the developer's):**
- ux-designer, non-blocking: `docs/ux-spec.md` §4.5 (the row), §6.2, §11's "State, not just colour" table; `docs/ui/DESIGN-SYSTEM.md`'s colour-blind section (spec §10 item 6); the three corrections of Open issues 2–4.
- tech-lead: `docs/tech-design.md` §3 (the root collection and `AppThemeRoot`, `LocalBoardMarks` as the one dynamic local), §10 (this sitting's record and §9's validity ruling), §14.1 (row 6b closed), §16 (the items this brief opens).
- release-manager: the store copy may name the setting (spec §10 item 7), optional.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-cv/.planning/reports/colour-vision/tech-lead-brief.md` — this brief: the root collection (§1), the palette's shape (§2), the board redraw against the in-flight trees (§3, §10), stars and pips (§4), the setting and its entrance orders (§5), files (§6), the CV-AC test map with the contrast test and QA's device half (§7), the commit order (§8), the 15e sitting with the validity ruling (§9), the merge order and the second-merger duties (§10), the freeze (§11), the merge and the owed docs (§12). It closes colour-vision §10 item 3 and direction-B's open issue 6 (the owed brief). Nothing else was edited; no commit was made (the dispatch's instruction).

## Checks
- Rules: the plugin's `rules/*.md` for the role read in full with one `Read` each (snapshot 0.5.1 < plugin 1.0.0): pass. `.planning/recall/RECALL.md`: absent.
- `docs/ux/colour-vision.md` (786 lines, two reads), `candy-backdrop/tech-lead-direction-b.md` (464, two reads), `candy-backdrop/tech-lead-architecture.md` (513, two reads): read in full. `PegSolitarie-tb/.planning/reports/talkback-board/tech-lead-brief.md`: §Summary, Amendments 1–2, §0–§13 read (lines 1–367 of 419); §14, its Outputs/Checks/Open issues/Handoff not read (the dispatch asked for its brief, which §2, §12 and §13 are). `PegSolitarie-hint/.planning/reports/hint-undecided/tech-lead-brief.md` §9–§10 read; its `code-auditor.md` carries `VERDICT: PASS`.
- `git worktree list` and `git log --oneline -5` in `PegSolitarie-cv`: pass (`8f67d74` on `c5f2c54`, the spec and three reports carried over).
- `git diff --stat <merge-base>..HEAD` on `PegSolitarie-audio` (`0582e3f`; 56 main-source files), `-hint` (`c5f2c54`; no `BoardCanvas.kt`), `-tb` (`0582e3f`; the brief only), `-lic` (`c5f2c54`; docs only), `-ux` (`c9228a9`; docs only): pass — the basis of §10.1.
- `git diff <merge-base>..HEAD` on `PegSolitarie-audio` for `BoardCanvas.kt`, `BoardGeometry.kt` (no main diff), `GameScreen.kt`, `GameUiState.kt`, `BoardMetrics.kt`, `GameAnim.kt`, `MainActivity.kt`, `SettingsRepository.kt`, `DataModule.kt`, `dimens.xml`, `strings.xml`; and `grep -n VERDICT` in `audio-review/code-auditor-integration.md` (`VERDICT: PASS`): pass — the basis of §3.5, §5.1, §10.1 and §0's hard precondition.
- `git diff <merge-base>..HEAD` on `PegSolitarie-hint` for `GameUiState.kt`, `GameAnim.kt`, `GameScreen.kt`, `dimens.xml`, `strings.xml`: pass (adjacent blocks only).
- `cat`/`sed -n` on this branch: `BoardCanvas.kt`, `BoardGeometry.kt`, `BoardMetrics.kt`, `SettingsRepository.kt`, `SettingsScreen.kt`, `SettingsUiState.kt`, `SettingsViewModel.kt`, `SettingsRoute.kt`, `Theme.kt`, `Clay.kt`, `Color.kt`, `MainActivity.kt`, `App.kt`, `AppNavHost.kt`, `DataModule.kt`, `GameScreen.kt:185-232`, `GameScreenPreviews.kt:1-45`, `ResultsScreen.kt:200-295`, `BoardPreview.kt:85-125`, `TutorialScreen.kt:180-215,335-365`, `HomeProgressRibbon.kt:203-215`, `DockLook.kt:80-135`, `dimens.xml` (the stroke block): pass — the basis of §1–§5.
- `grep -rn` on this branch: the `SemanticsPropertyKey` idiom (`GameScreen.kt:61-71`, `ResultsScreen.kt:61`, `DockLook.kt:169`); `goldGradientFill`/`Icons.*Star` call sites (seven, four of them consumers); `stateIn` idiom (`ChallengeProgress.kt:59-72`, `AdMobAdGateway.kt`, `ClassicCatalogViewModel.kt`); `': SettingsRepository'` under `app/src/test` (16 files); `AppTheme(` call sites (7 main, 3 test files); `RunWith|Robolectric` in `ui/theme` tests (`AppTypographyTest` is plain JVM); `BoardCanvas(` in `HomeStage.kt:103` and `HomeBoardArt.kt:48` (both `interactive = false`); `renderedWindow|getPixel` in audio's `BoardCanvasWrongTapTest` (none): pass.
- Tests read: `BoardCanvasTest.kt` (full), `SettingsRepositoryTest.kt:1-80`, `SettingsScreenTest.kt:1-75`, `DockLookRecipeTest.kt:1-60`, `SettingsViewModelTest.kt` (function list), `InMemoryPreferencesDataStore.kt`/`RenderedWindow.kt` (signatures): pass — the basis of §7's idioms.
- `docs/tech-design.md` §2.3, §3.5, §8.2, §10, §14.1, §16 on this branch; `docs/ux-spec.md` §6.5 (lines 3696–4040) on `PegSolitarie-ux` and `git diff c9228a9..HEAD -- docs/ux-spec.md | grep` for its §6.5 hunks; `docs/ux/licences-screen.md` §2 on `PegSolitarie-lic`; `.planning/reports/STATUS.md` "In flight"; `PegSolitarie-gates` log and report list (auditor PASS present): pass — the basis of §9, §10.2 and §5.4.
- `./gradlew check`: **not run** (the dispatch: no Gradle; no code changed).
- **Not verified without a build:** that `drawArc` with a dash `PathEffect` starts its pattern at `startAngle` on Compose BOM 2026.06.00 (§8 names the fallback as a stop-and-report); the Machado severity-1.0 matrix values of §7.2 are written from the published table and not re-read from the paper here (the developer transcribes from the paper; §7.2's ±0.05 agreement with §3.5 is the transcription check).

## Open issues / risks
1. **The §6.5 validity clause** (§9) is read here as "no key either arm does not know is present in the store during the sitting", a tech-lead decision that supersedes candy A9's literal reading for the key case. It is owed to tech design §10 (Handoff). If the producer would rather keep the literal clause, this branch cannot sit a valid 15e at all, and the alternative is to merge it before candy and let candy's sitting measure both — a weaker reading, not recommended.
2. **Routed to the ux-designer (non-blocking):** the wrong-tap mark (audio's grey 12-dash ring at 0.40c) is not in colour-vision §3.4/§3.5 or CV-AC08's pair list; §2.4 adds it as a row of the recipe table because it shares the board with the hint rings and is dashed too. The ux-designer confirms the row, or names a change (its dash count or colour) if the pairing reads as too close under monochromacy.
3. **Routed to the ux-designer (non-blocking):** §3.4 and CV-AC05 say the earned star's outline is "`Icons.Outlined.Star` drawn over it"; the hollow glyph the app draws is `Icons.Outlined.StarBorder` (`material-icons-core`'s `Outlined.Star` is a filled shape). Built as `StarBorder` (§4.1); the spec's wording is corrected.
4. **Routed to the ux-designer (non-blocking, after the merge):** spec §10 item 6's folds (`docs/ux-spec.md` §4.5, §6.2, §11; `DESIGN-SYSTEM.md`).
5. **Sixteen test fakes** each gain two members (§5.1): mechanical, but it is the branch's largest existing-test diff and it is the cost R-8 (fold the duplicate doubles) was recorded for. Not this branch's to fold.
6. **The root collection is one new read on the start path** (non-blocking, off-main). Its measured cost is §9's sitting; if the sitting fails after a re-sit, the pre-ruled change is `Eagerly → Lazily`, and anything beyond that comes back to this role with the numbers.
7. **Two API facts are unverified on the pinned BOM** (no build allowed here): the dash phase origin under `drawArc` (§3.4, fallback named in §8) and nothing else; `compositionLocalOf`, `collectAsStateWithLifecycle` on a `StateFlow`, `PathEffect.dashPathEffect` and `stateIn(Eagerly)` are all in use in the codebase or its pinned libraries today.
8. **Order against talkback-board:** talkback's 13.2 pins itself before this branch; both resolutions are written (§10.3) so a producer inversion costs no new ruling. If talkback is BLOCKED on the emulator's TalkBack driving, this branch should not wait for it.
9. **CV-AC05's "off-palette goldens equal the base's"** has no stored image under A10/D4; the identity rests on the two JVM pins, `BoardCanvasPaletteTest`'s outside-the-slab equality, the read-only path's structural identity and QA's device comparison against `develop`. Recorded so the auditor does not look for a golden.
10. **The drafted founder question** on the standard palette (spec §7, the ux-designer's report) is not in this branch's `FOUNDER.md`; the build proceeds on option 1 whatever its state, and option 2 would be a constant change on `BoardMarkPalette.Standard.recipe` and `boardMarksFor(Standard, …)` only.
11. **Candy A9's sentence** "15e is valid only while both arms share the DataStore keys, so on this branch it would void the sitting" stays true for candy (it adds no key) and is superseded for this branch by issue 1; the tech-design fold records both.

## Handoff
Next: producer —
1. Commit this brief on `feature/colour-vision` in `PegSolitarie-cv` (`docs(colour-vision): the tech-lead's developer brief`, with the studio trailer) and record the task in STATUS.md.
2. When `feature/audio-review` is on `develop`, dispatch the android-game-dev in `PegSolitarie-cv` with this brief and the audio merge commit's hash (§0); it may run in parallel with talkback-board's build in `PegSolitarie-tb` (two worktrees, two dispatches). The developer's report: `.planning/reports/colour-vision/android-game-dev.md`.
3. Dispatch the ux-designer, non-blocking, with Open issues 2 and 3 (the recipe row for the wrong-tap mark; `StarBorder`), and after the merge with issue 4 (spec §10 item 6).
4. At review: the code-auditor with CV-AC01 to CV-AC15, §7.1's kinds and §11's commands; the qa-engineer with §7.3 on the Pixel_9a or Pixel_9, then the 15e sitting of §9 on `Pixel_9a_bench` last, with the host quiet (no Gradle in any worktree; the hint, audio, gates, talkback and candy work paused for its duration; `Pixel_9` and `Pixel_9a` shut down); the sitting's report at `.planning/reports/colour-vision/qa-engineer-15e.md`.
5. Merge in slot 6b (§10.2): after talkback-board unless inverted, and before candy-backdrop's sitting; `git merge --no-ff`; the branch merging second does §10.3.
6. Tech-lead: sign-off after the auditor's, QA's and the sitting's reports; then the tech-design folds of §12 (§3, §10 with Open issue 1's ruling, §14.1 row 6b, §16).

Next: android-game-dev — build to this brief in §8's order, C0 to C7, test-first; stop and report at any line of §8's last paragraph.
