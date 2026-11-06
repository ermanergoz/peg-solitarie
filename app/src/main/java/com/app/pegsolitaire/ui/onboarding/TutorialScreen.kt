package com.app.pegsolitaire.ui.onboarding

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.JumpSoundEffect
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.audio.withClickSound
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.backdrop.BACKDROP_PREVIEW_HEIGHT_DP
import com.app.pegsolitaire.ui.backdrop.BACKDROP_PREVIEW_WIDTH_DP
import com.app.pegsolitaire.ui.backdrop.BackdropDensity
import com.app.pegsolitaire.ui.backdrop.BackdropPreviewHost
import com.app.pegsolitaire.ui.components.AnnouncementRegion
import com.app.pegsolitaire.ui.components.BoardCanvas
import com.app.pegsolitaire.ui.components.BoardStop
import com.app.pegsolitaire.ui.components.StarGlyph
import com.app.pegsolitaire.ui.components.boardStopLabel
import com.app.pegsolitaire.ui.components.rememberBoardAnnouncer
import com.app.pegsolitaire.ui.components.rememberTouchExplorationEnabled
import com.app.pegsolitaire.ui.game.JumpHapticEffect
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.app.pegsolitaire.ui.theme.LocalBoardMarks
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.theme.ctaGradientBrush
import com.app.pegsolitaire.ui.theme.gradientFill
import com.app.pegsolitaire.ui.theme.rememberAnimationsEnabled
import com.app.pegsolitaire.ui.window.stableSystem

/** Semantics tag the tests use to locate the tutorial screen — never its button copy. */
const val TUTORIAL_SCREEN_TEST_TAG = "tutorial_screen"

/** Semantics tag on the header row — the step pips and Skip — for the layout tests. */
const val TUTORIAL_HEADER_TEST_TAG = "tutorial_header"

/** Semantics tag on the body — the board slab, the caption and the footer — for the layout tests. */
const val TUTORIAL_BODY_TEST_TAG = "tutorial_body"

/**
 * The interactive tutorial (ONB-01, D-01/D-01a-i) — stateless: it renders [state] and emits
 * taps upward, and decides nothing.
 *
 * Four bands in a `verticalScroll` column (§4.1): Header (pips · Skip), Board, Caption,
 * Footer. The board is the **real `BoardCanvas` in `interactive = true` mode** driven by a real
 * `GameUiState` and the real rules engine, so what the player learns is literally the mechanic
 * they will use — it is not an illustration. The footer is empty until the third step is
 * cleared, which keeps the board optically centred during play.
 *
 * The header is pinned to the top; the board, caption and footer are one body, centred in the
 * height below the header (the founder, 2026-11-06): on a tall phone the gap from the header down
 * to the body equals the gap from the body down to the navigation bar, on every step
 * (`HeaderPinnedBodyCentred`). The footer keeps its full height from the first step, invisible
 * until the third step is cleared, so the body is the same height throughout and the board does not
 * move when the star and the CTA appear (the founder, loop 1).
 *
 * Scrolling is for the overflow case only: at default font scale all four bands fit on a
 * 360x640dp phone, and the board keeps its square `tutorial_board_size` at every font scale
 * rather than shrinking, because compressing the one interactive element to win vertical space
 * is the wrong trade. What yields at 200% is the caption's growth pushing the empty footer
 * below the fold, which costs nothing.
 *
 * **Window insets are this screen's own job.** It is a full-window destination with no
 * `Scaffold`, so nothing subtracts the system bars for it; the column pads itself off the bars'
 * stable insets and the cutout, `WindowInsets.stableSystem`, the one source every screen reads
 * (D-05g; tech design §3.8 (d)). See the comment at the root for why it sits where it does in
 * the chain, and `TutorialScreenTest.skipsWholeTouchTargetClearsTheSystemBarsOnEveryStep`
 * for the geometry it buys.
 *
 * **Audio (D-01i).** A *completed* jump plays the game's own pair via the shared
 * [JumpSoundEffect] — lifted, never re-implemented, so the removal layering and the pitch
 * ladder cannot drift from the real game's. The two exits speak the app's press vocabulary.
 * Everything else on this screen is silent: no select, deselect, invalid-tap, move-rewind,
 * hint, star-pop or win cue, at any point, on any step. The asymmetry is the whole audio design —
 * the one sound on the screen marks the one thing the player got right. The same jump is also
 * felt (D-01j): the game's own `ContextClick`, through the shared [JumpHapticEffect], under the
 * persisted Haptics switch the board state carries — and nothing else on this screen buzzes.
 *
 * **TalkBack (D-TB as amended by D-TBa).** The board is the same stops as gameplay's — the slab is
 * the board stop, each cell a stop — and speaks through the board's spoken channel, the shared board
 * announcer: the selection, put-down, jump and wrong-tap lines, each step's line, the coach's hint and
 * the done line ([TutorialBoardAnnouncements]). Words, not cues: nothing here plays or buzzes for them.
 *
 * @param state the immutable tutorial state (single source of truth).
 * @param onCellTap emits the tapped board [Position] up to the ViewModel, which owns every rule.
 * @param onAnimationFinished invoked once a jump slide completes.
 * @param onSkip leaves the tutorial from any step (D-01d).
 * @param onDone leaves after the final step, from the completion CTA.
 * @param modifier layout modifier for the screen root.
 */
@Composable
fun TutorialScreen(
    state: TutorialUiState,
    onCellTap: (Position) -> Unit,
    onAnimationFinished: () -> Unit,
    onSkip: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // The one cue the board makes, and it is the SHARED effect (D-01i): re-implementing it
    // here would duplicate the removal layering and the pitch ladder and let them drift.
    JumpSoundEffect(transition = state.board.animating)
    JumpHapticEffect(animatingMoveId = state.board.animating?.id, hapticsOn = state.board.hapticsOn)
    // D-TB: the board's spoken channel — the shared board announcer — and every line it speaks here.
    val announcer = rememberBoardAnnouncer()
    val boardStopFocus = remember { FocusRequester() }
    val gotItFocus = remember { FocusRequester() }
    val touchExploration = rememberTouchExplorationEnabled()
    TutorialBoardAnnouncements(state, announcer, boardStopFocus, gotItFocus, touchExploration)

    Surface(
        modifier = modifier.fillMaxSize(),
        // Transparent over the app-wide backdrop (backdrop spec B-09), with the content colour the
        // background's own so unlabelled text keeps `onBackground` (F-32).
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
    ) {
        // The surface keeps filling the window — the background belongs edge to edge — and
        // this Box is what stops doing so for the content. Two things need it:
        //
        //  * The inset padding sits on the COLUMN and outside `verticalScroll`, so the
        //    scrolling viewport itself is inset and no band can be scrolled under a system
        //    bar. The window is edge to edge on every API level (D-05g), and this screen has
        //    no `Scaffold` to subtract the insets for it the way Home does. Without it the
        //    header lands under the status bar and the visible "Skip" becomes untappable — the
        //    tutorial's one promise (D-01d: "a tutorial the player cannot leave is worse than
        //    no tutorial") resting on a control that does not answer a press. It reads the
        //    bars' STABLE insets and the cutout (`WindowInsets.stableSystem`), as every screen
        //    does, never a visible inset such as `safeDrawing`: on the way back from gameplay
        //    the bars are still hidden for a moment, and a visible read would lay the column
        //    out at zero and drop it by the bar's height as the bar slid in (D-05g part 3).
        //  * `Surface` is a `Box(propagateMinConstraints = true)`, so it hands the column a
        //    minimum width equal to the window's; `widthIn(max = content_max_width)` under
        //    that minimum is a no-op, which is how the 480dp cap came to do nothing here.
        //    This Box does not propagate its minimum, so the cap binds and `align` centres
        //    what it caps (§12 "capped at content_max_width and centred").
        //
        // The column's HEIGHT is the other way round: `fillMaxHeight()` between the insets and
        // the scroll makes the viewport the whole inset height, and `verticalScroll` passes that
        // on as the content's minimum height. So on a tall phone the column is at least its
        // viewport's height and `HeaderPinnedBodyCentred` has the slack to place: the header at
        // the top, the body centred below it. It touches height only, so the width cap above
        // still binds; and once the bands overflow, the column is as tall as they are and
        // scrolls exactly as before.
        Box(modifier = Modifier.fillMaxSize()) {
            val bandSpacing = dimensionResource(R.dimen.spacing_lg)
            val columnPadding = dimensionResource(R.dimen.spacing_md)
            val arrangement =
                remember(bandSpacing, columnPadding) {
                    HeaderPinnedBodyCentred(spacing = bandSpacing, bottomPadding = columnPadding)
                }
            Column(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .windowInsetsPadding(WindowInsets.stableSystem)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .widthIn(max = dimensionResource(R.dimen.content_max_width))
                        .padding(columnPadding)
                        .testTag(TUTORIAL_SCREEN_TEST_TAG),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = arrangement,
            ) {
                TutorialHeader(state = state, onSkip = onSkip)
                Column(
                    modifier = Modifier.testTag(TUTORIAL_BODY_TEST_TAG),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(bandSpacing),
                ) {
                    TutorialBoardSlab(
                        state = state,
                        onCellTap = onCellTap,
                        onAnimationFinished = onAnimationFinished,
                        boardStopFocus = boardStopFocus,
                        touchExploration = touchExploration,
                    )
                    TutorialCaption(captionRes = state.captionRes)
                    TutorialFooter(
                        completed = state.completed,
                        onDone = onDone,
                        gotItFocus = gotItFocus,
                        touchExploration = touchExploration,
                    )
                }
            }
            // D-TB: the screen's spoken channel, drawn last, above everything (defect ruling §1).
            AnnouncementRegion(announcer)
        }
    }
}

/** Pips (leading) and `Skip` (trailing). There is no title bar — the pips are the only progress signal. */
@Composable
private fun TutorialHeader(
    state: TutorialUiState,
    onSkip: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().testTag(TUTORIAL_HEADER_TEST_TAG),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepPips(stepNumber = state.stepNumber)
        // D-01d: reachable on EVERY step. A tutorial the player cannot leave is worse than
        // no tutorial. It is a dismissal, so it speaks the dismiss cue (D-01i, D-11).
        TextButton(onClick = withClickSound(SoundCue.ButtonDismiss, onSkip)) {
            Text(text = stringResource(R.string.onboarding_skip))
        }
    }
}

/**
 * The three progress pips: done = solid gold disc, current = ringed, upcoming = outlined.
 *
 * Filled-vs-outlined carries the state, so colour is never the only channel. The individual
 * pips are decorative, so the row speaks **once** via `clearAndSetSemantics` ("Step 2 of 3")
 * instead of announcing three anonymous dots — the same idiom `EarnedStarPips` already uses.
 */
@Composable
private fun StepPips(stepNumber: Int) {
    val description = stringResource(R.string.tutorial_step_progress, stepNumber, TutorialUiState.STEP_COUNT)
    Row(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_sm)),
        modifier = Modifier.clearAndSetSemantics { contentDescription = description },
    ) {
        repeat(TutorialUiState.STEP_COUNT) { index ->
            val step = index + 1
            Box(
                modifier =
                    Modifier
                        .size(dimensionResource(R.dimen.tutorial_step_pip_size))
                        .then(
                            if (step < stepNumber) {
                                Modifier.gradientFill(Brush.linearGradient(LocalBoardMarks.current.donePipFill))
                            } else {
                                Modifier
                            },
                        ),
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (step < stepNumber) Color.Black else MaterialTheme.colorScheme.surface,
                    border =
                        BorderStroke(
                            width = PIP_BORDER_WIDTH.dp,
                            color =
                                when {
                                    step == stepNumber -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                },
                        ),
                    content = {},
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

/**
 * The live board on a clay slab, plus the single coach pulse (§4.4 class 3, §10).
 *
 * The pulse is one `TutorialAnim.COACH_PULSE_MS` scale beat per escalation, keyed on the
 * state's coach ordinal and skipped entirely when OS animations are off. The escalation's own
 * highlight — the source ring and the brightened destination dot — is the **board's own**
 * hint rendering rather than a second highlight vocabulary.
 */
@Composable
private fun TutorialBoardSlab(
    state: TutorialUiState,
    onCellTap: (Position) -> Unit,
    onAnimationFinished: () -> Unit,
    boardStopFocus: FocusRequester,
    touchExploration: Boolean,
) {
    val animationsEnabled = rememberAnimationsEnabled()
    val pulse = remember { Animatable(SETTLED_SCALE) }
    LaunchedEffect(state.coachTick) {
        if (state.coachTick > NO_COACH_YET && animationsEnabled) {
            pulse.animateTo(COACH_PULSE_SCALE, tween(TutorialAnim.COACH_PULSE_MS / PULSE_HALVES))
            pulse.animateTo(SETTLED_SCALE, tween(TutorialAnim.COACH_PULSE_MS / PULSE_HALVES))
        }
    }

    Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier =
            Modifier
                .size(dimensionResource(R.dimen.tutorial_board_size))
                .scale(pulse.value)
                .clayShadow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.extraLarge)
                // D-TB: the slab is the cells' group, its first child the board stop (ruling §3).
                .semantics { isTraversalGroup = true },
    ) {
        BoardStop(
            label = boardStopLabel(state.board.board.pegCount),
            focusRequester = boardStopFocus,
            focusEnabled = touchExploration,
            modifier = Modifier.fillMaxSize(),
        )
        BoardCanvas(
            state = state.board,
            onCellTap = onCellTap,
            onAnimationFinished = onAnimationFinished,
            modifier =
                Modifier
                    .aspectRatio(SQUARE)
                    .padding(dimensionResource(R.dimen.spacing_md)),
            interactive = true,
        )
    }
}

/**
 * One caption line at a reserved height, cross-faded between steps.
 *
 * The height is reserved rather than wrapped so a 1↔2-line swap never reflows the board the
 * player may be mid-tap on.
 */
@Composable
private fun TutorialCaption(captionRes: Int) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.tutorial_caption_min_height)),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = captionRes, label = "tutorialCaption") { res ->
            Text(
                text = stringResource(res),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Empty until the last step is cleared, then the gold star and the completion CTA.
 *
 * Empty is invisible, not absent (the founder, loop 1): the footer keeps its full height from the
 * first step, so the centred body is the same height throughout and the board does not move when
 * the star and the CTA appear. Until then the star's room is an empty box of the star's size — the
 * star itself is first composed at completion, so its pop starts there — and the CTA is laid out
 * but transparent, disabled, unable to take focus and cleared from semantics: nothing to see,
 * press, focus or announce.
 *
 * **The star pops silently** (D-01h, upheld by D-01i): neither the win nor the star-pop cue
 * plays, because a tutorial is not a victory — `ClayAnim.STAR_POP_MS` is borrowed as a
 * *duration*, not as a cue. Nor is anything banked behind it: no star, no cleared resume
 * snapshot, no ladder advance (§5.1). The CTA plays the same forward press cue every other
 * primary button in the app plays (SOUND-04, D-11).
 */
@Composable
private fun TutorialFooter(
    completed: Boolean,
    onDone: () -> Unit,
    gotItFocus: FocusRequester,
    touchExploration: Boolean,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (completed) Modifier else Modifier.alpha(HIDDEN_ALPHA).clearAndSetSemantics {}),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
    ) {
        Box(
            modifier = Modifier.size(dimensionResource(R.dimen.results_star_size)),
            contentAlignment = Alignment.Center,
        ) {
            if (completed) CompletionStar()
        }
        Button(
            onClick = withClickSound(SoundCue.ButtonForward, onDone),
            enabled = completed,
            shape = MaterialTheme.shapes.large,
            // The house CTA treatment: a transparent M3 button over the shared CtaGradient,
            // so this button is the same object the player has already pressed on Home.
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .focusRequester(gotItFocus)
                    .focusProperties {
                        when {
                            // Waiting, it takes no focus at all, a hardware keyboard's included.
                            !completed -> canFocus = false
                            // A clickable cannot take focus in touch mode, a phone's; completion moves
                            // focus here under touch exploration only (defect ruling §2, B-2; TB-11
                            // untouched).
                            touchExploration -> canFocus = true
                        }
                    }.claySquish()
                    .clayShadow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.large)
                    .background(brush = ctaGradientBrush(), shape = MaterialTheme.shapes.large),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = stringResource(R.string.onboarding_got_it))
            }
        }
    }
}

/**
 * One earned star in the active palette, popped in with the Results star vocabulary. Decorative: it
 * announces nothing.
 *
 * Reduced motion is the clock's, as in `rememberPopIn`: the spring always runs, and its effect is never
 * keyed on the gate. Composed with animations off, the star starts settled; turned off mid-pop, the spring
 * takes the new scale from the composition's clock and ends at full size within two frames.
 */
@Composable
private fun CompletionStar() {
    val animationsEnabled = rememberAnimationsEnabled()
    val scale = remember { Animatable(if (animationsEnabled) STAR_POP_START else SETTLED_SCALE) }
    LaunchedEffect(Unit) {
        scale.animateTo(SETTLED_SCALE, ClayAnim.overshootSpring())
    }
    StarGlyph(
        earned = true,
        size = dimensionResource(R.dimen.results_star_size),
        modifier = Modifier.scale(scale.value),
    )
}

/**
 * The tutorial column's arrangement: the header pinned at the top, the body centred in the height
 * below it (the founder, 2026-11-06).
 *
 * The column is at least its viewport's height, so on a phone with room to spare [arrange] is
 * handed the whole viewport. The header, the first child, stays at the top. The body, the second,
 * is centred between the header's bottom edge and the viewport's: the column's [bottomPadding]
 * lies below `totalSize` and is counted in, so the gap above the body equals the gap below it down
 * to the navigation bar. The body never comes nearer the header than [spacing], the gap
 * `spacedBy` gave it; and once the two overflow the viewport, the column is exactly their height
 * plus that gap, so they sit as `spacedBy(spacing)` would put them and the column scrolls.
 */
private class HeaderPinnedBodyCentred(
    override val spacing: Dp,
    private val bottomPadding: Dp,
) : Arrangement.Vertical {
    override fun Density.arrange(
        totalSize: Int,
        sizes: IntArray,
        outPositions: IntArray,
    ) {
        val headerHeight = sizes[HEADER_INDEX]
        val bodyHeight = sizes[BODY_INDEX]
        val nearest = headerHeight + spacing.roundToPx()
        val slack = totalSize + bottomPadding.roundToPx() - headerHeight - bodyHeight
        val centred = headerHeight + slack / SLACK_HALVES
        outPositions[HEADER_INDEX] = PINNED_TOP
        outPositions[BODY_INDEX] = centred.coerceIn(nearest, maxOf(nearest, totalSize - bodyHeight))
    }
}

/** The tutorial column's children, in order: the header, then the body. */
private const val HEADER_INDEX = 0
private const val BODY_INDEX = 1

/** The header sits at the very top of the column's content. */
private const val PINNED_TOP = 0

/** The slack below the header is split into two equal gaps, above the body and below it. */
private const val SLACK_HALVES = 2

/** The board slab and the canvas inside it are square. */
private const val SQUARE = 1f

/** Resting scale for the coach pulse and the completion star. */
private const val SETTLED_SCALE = 1f

/** The coach pulse's peak — the same gentle overshoot the board's own selection pulse uses. */
private const val COACH_PULSE_SCALE = 1.06f

/** The star starts collapsed and springs out. */
private const val STAR_POP_START = 0f

/** The footer before completion: laid out at its full size, drawn not at all. */
private const val HIDDEN_ALPHA = 0f

/** A pulse is out-and-back, so each leg takes half the total. */
private const val PULSE_HALVES = 2

/** `coachTick` starts here, which the screen reads as "nothing to pulse". */
private const val NO_COACH_YET = 0

/** Pip outline width. */
private const val PIP_BORDER_WIDTH = 2

/** The tutorial's first step on the design baseline's window, over the Sparse wash as the host draws it (BD-25). */
@Preview(name = "Tutorial · light", widthDp = BACKDROP_PREVIEW_WIDTH_DP, heightDp = BACKDROP_PREVIEW_HEIGHT_DP)
@Composable
private fun TutorialScreenLightPreview() {
    AppTheme(darkTheme = false) {
        BackdropPreviewHost(BackdropDensity.Sparse) {
            TutorialScreen(
                state = TutorialUiState.initial(),
                onCellTap = {},
                onAnimationFinished = {},
                onSkip = {},
                onDone = {},
            )
        }
    }
}

/** The tutorial completed, in dark, over the Sparse wash as the host draws it (BD-25). */
@Preview(name = "Tutorial · dark", widthDp = BACKDROP_PREVIEW_WIDTH_DP, heightDp = BACKDROP_PREVIEW_HEIGHT_DP)
@Composable
private fun TutorialScreenDarkPreview() {
    AppTheme(darkTheme = true) {
        BackdropPreviewHost(BackdropDensity.Sparse) {
            TutorialScreen(
                state = TutorialUiState.initial().completed(),
                onCellTap = {},
                onAnimationFinished = {},
                onSkip = {},
                onDone = {},
            )
        }
    }
}
