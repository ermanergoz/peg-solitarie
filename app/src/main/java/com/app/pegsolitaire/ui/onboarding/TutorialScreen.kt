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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.JumpSoundEffect
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.audio.withClickSound
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.components.BoardCanvas
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.theme.ctaGradientBrush
import com.app.pegsolitaire.ui.theme.goldGradientFill
import com.app.pegsolitaire.ui.theme.rememberAnimationsEnabled

/** Semantics tag the tests use to locate the tutorial screen — never its button copy. */
const val TUTORIAL_SCREEN_TEST_TAG = "tutorial_screen"

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
 * Scrolling is for the overflow case only: at default font scale all four bands fit on a
 * 360x640dp phone, and the board keeps its square `tutorial_board_size` at every font scale
 * rather than shrinking, because compressing the one interactive element to win vertical space
 * is the wrong trade. What yields at 200% is the caption's growth pushing the empty footer
 * below the fold, which costs nothing.
 *
 * **Audio (D-01i).** A *completed* jump plays the game's own pair via the shared
 * [JumpSoundEffect] — lifted, never re-implemented, so the removal layering and the pitch
 * ladder cannot drift from the real game's. The two exits speak the app's press vocabulary.
 * Everything else on this screen is silent: no select, deselect, invalid-tap, move-rewind,
 * hint, star-pop or win cue, at any point, on any step. The asymmetry is the whole audio design —
 * the one sound on the screen marks the one thing the player got right.
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

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .widthIn(max = dimensionResource(R.dimen.content_max_width))
                    .padding(dimensionResource(R.dimen.spacing_md))
                    .testTag(TUTORIAL_SCREEN_TEST_TAG),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_lg)),
        ) {
            TutorialHeader(state = state, onSkip = onSkip)
            TutorialBoardSlab(
                state = state,
                onCellTap = onCellTap,
                onAnimationFinished = onAnimationFinished,
            )
            TutorialCaption(captionRes = state.captionRes)
            TutorialFooter(completed = state.completed, onDone = onDone)
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
        modifier = Modifier.fillMaxWidth(),
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
                        .then(if (step < stepNumber) Modifier.goldGradientFill() else Modifier),
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
                .clayShadow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.extraLarge),
    ) {
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
) {
    if (!completed) {
        Spacer(modifier = Modifier.fillMaxWidth())
        return
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
    ) {
        CompletionStar()
        Button(
            onClick = withClickSound(SoundCue.ButtonForward, onDone),
            shape = MaterialTheme.shapes.large,
            // The house CTA treatment: a transparent M3 button over the shared CtaGradient,
            // so this button is the same object the player has already pressed on Home.
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .claySquish()
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

/** One gold star, popped in with the Results star vocabulary. Decorative: it announces nothing. */
@Composable
private fun CompletionStar() {
    val animationsEnabled = rememberAnimationsEnabled()
    val scale = remember { Animatable(if (animationsEnabled) STAR_POP_START else SETTLED_SCALE) }
    LaunchedEffect(animationsEnabled) {
        if (animationsEnabled) scale.animateTo(SETTLED_SCALE, ClayAnim.overshootSpring())
    }
    Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = null,
        modifier =
            Modifier
                .size(dimensionResource(R.dimen.results_star_size))
                .scale(scale.value)
                .goldGradientFill(),
    )
}

/** The board slab and the canvas inside it are square. */
private const val SQUARE = 1f

/** Resting scale for the coach pulse and the completion star. */
private const val SETTLED_SCALE = 1f

/** The coach pulse's peak — the same gentle overshoot the board's own selection pulse uses. */
private const val COACH_PULSE_SCALE = 1.06f

/** The star starts collapsed and springs out. */
private const val STAR_POP_START = 0f

/** A pulse is out-and-back, so each leg takes half the total. */
private const val PULSE_HALVES = 2

/** `coachTick` starts here, which the screen reads as "nothing to pulse". */
private const val NO_COACH_YET = 0

/** Pip outline width. */
private const val PIP_BORDER_WIDTH = 2

@Preview(name = "Tutorial · light")
@Composable
private fun TutorialScreenLightPreview() {
    AppTheme(darkTheme = false) {
        TutorialScreen(
            state = TutorialUiState.initial(),
            onCellTap = {},
            onAnimationFinished = {},
            onSkip = {},
            onDone = {},
        )
    }
}

@Preview(name = "Tutorial · dark")
@Composable
private fun TutorialScreenDarkPreview() {
    AppTheme(darkTheme = true) {
        TutorialScreen(
            state = TutorialUiState.initial().completed(),
            onCellTap = {},
            onAnimationFinished = {},
            onSkip = {},
            onDone = {},
        )
    }
}
