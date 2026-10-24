package com.app.pegsolitaire.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.ui.components.BoardCanvas
import com.app.pegsolitaire.ui.game.GameUiState
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.clayEntrance
import com.app.pegsolitaire.ui.theme.clayShadow

/** Semantics tag of the stage — its only semantics, which TalkBack does not read (§4.2.4). */
const val HOME_STAGE_TEST_TAG = "home_stage"

/** Semantics tag of the stage art, present only when the stage is tall enough to draw it. */
const val HOME_STAGE_ART_TEST_TAG = "home_stage_art"

/**
 * Home's **stage** (ux-spec §4.2.4, D-03l): the slot directly beneath the hero row that holds the
 * larger share of the screen's slack — four fifths of the slack the window leaves with no Continue
 * chip, less the chips' room (§4.2.5, D-03n) — and draws the game's own board into it.
 *
 * ## Why the slack holds a board now
 *
 * D-03i kept the mode tiles low by parking three quarters of the slack **as nothing** between the
 * ribbon and the Continue chips; on the founder's phone that nothing measured ≈172dp between two
 * islands of content. Tiles in the thumb zone and no hole in the middle are only both achievable
 * if what sits above the tiles *grows into* the space, and a content band must never do that — a
 * band handed a share of the slack is handed 0 the moment the column overflows. A decoration may,
 * because 0 is its correct size on a short screen: this is the **one** band on Home that takes slack
 * and holds anything, and what it holds is decorative, non-interactive, non-focusable and silent
 * (§4.2.2 clause 4's single exception). It is also where a Continue chip's room comes from when the
 * chip arrives after first paint, so the bands below it hold still (§4.2.5).
 *
 * ## How big the board is
 *
 * The art is a square of edge `min(stage height, content width, home_stage_art_max_size)`,
 * centred. It is composed **only** when that edge reaches `home_stage_art_min_size`; below it
 * nothing is composed and the stage is plain slack — which bounds the largest blank Home can show
 * at under 48dp plus the two band gaps around the stage (criterion 15p). It yields by
 * construction: it only ever gets what the hero, the ribbon, the chips and the tiles leave, so no
 * threshold decides it and nothing ever yields to it.
 *
 * ## When the chips take it below its floor (§4.2.5, "The stage art at the end of its range")
 *
 * A Continue chip's room comes out of the stage, so a transition can end below
 * `home_stage_art_min_size` — two chips at 360x640dp take the stage from 186dp to 10dp. The art
 * does not shrink to the floor and vanish there: under [StageArtFade.Out] it stays drawn while the
 * room opens, shrinking with the stage, and fades out over the same `ClayAnim.ENTRANCE_MS`, gone
 * once the room is open; a chip leaving fades it back in ([StageArtFade.In]). Settled, or with
 * animations off, it is simply drawn or not.
 *
 * ## What it costs the first frame (D-03g)
 *
 * One `BoardCanvas` draw of a constant position — the withdrawn peg cluster's one draw, replaced —
 * and a `BoxWithConstraints` to read the slot it was given. No read, no decode, no asset, no SDK
 * call, and nothing that depends on `HomeUiState`: it renders the same before and after the first
 * progress emission (criterion 15r).
 *
 * @param modifier layout modifier. `HomeBandColumn` measures the stage at an exact height and
 *   width, the slot's; the stage fills it.
 * @param artFade whether a chip's room is taking the art across its floor, decided by the band
 *   column from the motion's two ends.
 * @param artFadeProgress how far that motion has come, 0 to 1, read only while the art is drawn.
 */
@Composable
internal fun HomeStage(
    modifier: Modifier = Modifier,
    artFade: StageArtFade = StageArtFade.None,
    artFadeProgress: () -> Float = { MOTION_DONE },
) {
    BoxWithConstraints(
        // The stage enters with the hero, once, with Home. The entrance is the stage's own and not
        // the art's, so art drawn later — faded back in once a chip has left — does not replay it:
        // the cascade is never restarted by a chip's room (§4.2.5, §10).
        modifier = modifier.fillMaxWidth().clayEntrance(HERO_ENTRANCE_ORDER).testTag(HOME_STAGE_TEST_TAG),
        contentAlignment = Alignment.Center,
    ) {
        val edge = minOf(maxHeight, maxWidth, dimensionResource(R.dimen.home_stage_art_max_size))
        if (edge >= dimensionResource(R.dimen.home_stage_art_min_size) || artFade != StageArtFade.None) {
            HomeStageArt(edge = edge, fade = artFade, fadeProgress = artFadeProgress)
        }
    }
}

/**
 * How the stage art crosses `home_stage_art_min_size` while a Continue chip's room opens or closes
 * (ux-spec §4.2.5, "The stage art at the end of its range"): the band column decides it from the
 * stage's height at the motion's two ends, and the art takes its alpha from it.
 */
internal enum class StageArtFade {
    /** The art is drawn at both ends of the motion, or at neither, so it only follows the stage's size. */
    None,

    /** The motion ends below the floor: the art fades out as it shrinks and is gone once the room is open. */
    Out,

    /** The motion starts below the floor: the art fades in as it grows, from the room's first frame. */
    In,
    ;

    /** The art's alpha at [progress] of the motion, from 0 to 1. */
    fun alphaAt(progress: Float): Float =
        when (this) {
            None -> MOTION_DONE
            Out -> MOTION_DONE - progress
            In -> progress
        }
}

/** A motion that has run its course, which is also the alpha of art that is not fading. */
private const val MOTION_DONE = 1f

/**
 * The stage art: the gameplay board slab in miniature — `surfaceContainerHigh`,
 * `shapes.extraLarge`, `clayShadow(elevation_card)` — holding a read-only [BoardCanvas] of
 * [HomeStageBoardState], inset by [HomeStageMetrics.SLAB_PADDING_FRACTION] of its edge.
 *
 * Not through `BoardPreview`, which forces `catalog_preview_size` whatever size it is asked for.
 * Decorative: no description and no semantics of its own beyond a test tag. It enters with the
 * stage around it, which takes the hero's order (`HERO_ENTRANCE_ORDER`) rather than one of its own,
 * so §10's cascade and every chip's and tile's counted order are unchanged; with animations off it
 * is simply there.
 */
@Composable
private fun HomeStageArt(
    edge: Dp,
    fade: StageArtFade,
    fadeProgress: () -> Float,
) {
    val shape = MaterialTheme.shapes.extraLarge
    Box(
        modifier =
            Modifier
                .size(edge)
                // The fade's own layer exists only while a fade runs, so the art Home's first frame
                // draws is the one it always drew. Its alpha is read in the draw phase, so the fade
                // redraws the art each frame without recomposing it.
                .then(
                    if (fade == StageArtFade.None) {
                        Modifier
                    } else {
                        Modifier.graphicsLayer { alpha = fade.alphaAt(fadeProgress()) }
                    },
                ).clayShadow(dimensionResource(R.dimen.elevation_card), shape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, shape)
                .testTag(HOME_STAGE_ART_TEST_TAG),
    ) {
        BoardCanvas(
            state = HomeStageBoardState,
            onCellTap = {},
            onAnimationFinished = {},
            modifier = Modifier.padding(edge * HomeStageMetrics.SLAB_PADDING_FRACTION),
            interactive = false,
        )
    }
}

/** Unitless proportions of the stage art — ratios, so a scoped object rather than `dimens.xml`. */
internal object HomeStageMetrics {
    /** The board's inset inside the stage slab, as a share of the slab's edge. */
    const val SLAB_PADDING_FRACTION = 0.06f
}

/**
 * The board state the stage draws: English-33 in mid-game, 14 pegs scattered over all four arms.
 *
 * It is a **constant**, never the player's saved board or anything read at start-up, so it cannot
 * contradict a readout and costs Home's first frame nothing but one board draw (D-03g). It is
 * reachable from the English start by legal play — the first 18 jumps of a winning line — and
 * `HomeStagePositionTest` proves that by replay; nothing replays it here.
 *
 * `GameUiState.initial` gives it no selected peg, no legal targets, no hint and no jump in flight,
 * so nothing on it pulses, pops or slides on any frame (criterion 15q, §10).
 */
internal val HomeStageBoardState: GameUiState by lazy {
    GameUiState.initial(
        board =
            STAGE_PEGS.fold(Board(spec = Boards.English33, pegs = 0L)) { board, position ->
                board.withPeg(position)
            },
    )
}

/** The stage position's pegs, row by row. */
private val STAGE_PEGS: List<Position> =
    listOf(
        Position(row = 0, col = 4),
        Position(row = 1, col = 2),
        Position(row = 1, col = 3),
        Position(row = 2, col = 0),
        Position(row = 2, col = 1),
        Position(row = 2, col = 4),
        Position(row = 2, col = 5),
        Position(row = 2, col = 6),
        Position(row = 3, col = 6),
        Position(row = 4, col = 1),
        Position(row = 4, col = 2),
        Position(row = 4, col = 4),
        Position(row = 5, col = 4),
        Position(row = 6, col = 2),
    )

/** The stage in a slot of the founder's one-chip share, on the app background, in both themes. */
@Preview(name = "Home stage · light", widthDp = PREVIEW_STAGE_WIDTH_DP, heightDp = PREVIEW_STAGE_HEIGHT_DP)
@Preview(
    name = "Home stage · dark",
    widthDp = PREVIEW_STAGE_WIDTH_DP,
    heightDp = PREVIEW_STAGE_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeStagePreview() {
    AppTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            HomeStage()
        }
    }
}

/** The 6.7-inch phone's column width, and a stage about as tall as the one-chip state gives it. */
private const val PREVIEW_STAGE_WIDTH_DP = 380
private const val PREVIEW_STAGE_HEIGHT_DP = 200
