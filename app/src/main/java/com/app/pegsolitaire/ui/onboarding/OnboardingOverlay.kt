package com.app.pegsolitaire.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.classic.BoardPreview
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.theme.ctaGradientBrush

/** One onboarding step: a board illustration of the jump plus a teaching caption. */
private data class OnboardingStep(
    val board: Board,
    val captionRes: Int,
)

/**
 * The first-run how-to-play overlay teaching the orthogonal jump (ONB-01, D-13/D-14/D-15).
 *
 * A stateless, scrim-dimmed sheet driven entirely by the hoisted [onDismiss] lambda
 * (which `markSeen()`s in the ViewModel, D-13). A `scrim`-filled [Box] backs a centered
 * `surfaceContainerHigh` [Surface] sheet (`shapes.large`, capped at `content_max_width`)
 * holding a few illustrated steps — each a read-only [BoardPreview] drawn with the board
 * roles (pegs `secondary`, recessed hole) plus a one-sentence caption — and two dismiss
 * actions: a primary "Got it" [Button] and a "Skip tutorial" [TextButton], both calling
 * [onDismiss]. Hosted only while `onboardingVisible`.
 *
 * @param onDismiss dismisses the overlay and persists `markSeen()` (D-13).
 * @param modifier layout modifier for the scrim container.
 */
@Composable
fun OnboardingOverlay(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim)
                .padding(dimensionResource(R.dimen.spacing_lg)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier =
                Modifier
                    .clayShadow(
                        dimensionResource(R.dimen.elevation_dialog),
                        MaterialTheme.shapes.extraLarge,
                    ).widthIn(max = dimensionResource(R.dimen.content_max_width)),
        ) {
            OnboardingSheet(onDismiss = onDismiss)
        }
    }
}

/** The sheet body: the illustrated steps stacked over the two dismiss actions. */
@Composable
private fun OnboardingSheet(onDismiss: () -> Unit) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.spacing_lg)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
    ) {
        ONBOARDING_STEPS.forEach { step -> OnboardingStepItem(step = step) }

        val actionShape = MaterialTheme.shapes.medium
        Button(
            onClick = onDismiss,
            shape = actionShape,
            colors =
                ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            modifier =
                Modifier
                    .claySquish()
                    .background(brush = ctaGradientBrush(), shape = actionShape)
                    .fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.onboarding_got_it),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        TextButton(
            onClick = onDismiss,
            shape = actionShape,
            modifier =
                Modifier
                    .claySquish()
                    .fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.onboarding_skip),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** One step: the jump illustration above its `bodyMedium` caption. */
@Composable
private fun OnboardingStepItem(step: OnboardingStep) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_sm)),
    ) {
        BoardPreview(
            board = step.board,
            modifier = Modifier.size(dimensionResource(R.dimen.onboarding_illustration_size)),
        )
        Text(
            text = stringResource(step.captionRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** A 1x3 orthogonal strip the jump is taught on (cols 0,1 pegs; col 2 the empty target). */
private val JUMP_SPEC =
    BoardSpec(
        inPlay =
            listOf(
                Position(row = 0, col = 0),
                Position(row = 0, col = 1),
                Position(row = 0, col = 2),
            ),
        target = null,
    )

/** Before the jump: two adjacent pegs and an empty hole two cells from the first. */
private val JUMP_BEFORE: Board =
    Board(spec = JUMP_SPEC, pegs = 0L)
        .withPeg(Position(row = 0, col = 0))
        .withPeg(Position(row = 0, col = 1))

/** After the jump: the jumped middle peg is removed, leaving a single peg in the target hole. */
private val JUMP_AFTER: Board =
    Board(spec = JUMP_SPEC, pegs = 0L)
        .withPeg(Position(row = 0, col = 2))

/** The three teaching steps: tap-and-target, the orthogonal hop, and the cleared result. */
private val ONBOARDING_STEPS =
    listOf(
        OnboardingStep(board = JUMP_BEFORE, captionRes = R.string.onboarding_step_1),
        OnboardingStep(board = JUMP_BEFORE, captionRes = R.string.onboarding_step_2),
        OnboardingStep(board = JUMP_AFTER, captionRes = R.string.onboarding_step_3),
    )
