package com.app.pegsolitaire.ui.game

import androidx.compose.runtime.Immutable
import com.app.pegsolitaire.ui.theme.BoardMarkPalette

/**
 * The non-colour half of a palette's marks as data (docs/ux/colour-vision.md §3.4): ring radii as
 * fractions of the cell edge, the alphas, the hint rings' dash count, the selection halo and the
 * unearned star's form. The board draws its marks from this and from `LocalBoardMarks`' colours,
 * and nothing else, so `BoardMarkRecipeTest` can prove "never colour alone" over the mapping
 * itself (CV-AC08) — the `DockLook.recipe` idiom.
 *
 * @property selectedRingRatio the selection ring's radius (stroke centreline), × cell edge.
 * @property hintSourceRingRatio the hint's ring around the peg to move, × cell edge.
 * @property hintTargetRingRatio the hint's ring around the destination hole, × cell edge.
 * @property destinationDotAlpha the legal-target mark's alpha.
 * @property hintRingAlpha both hint rings' alpha.
 * @property hintConnectorAlpha the hint's source-to-target line's alpha.
 * @property hintRingDashes the dashes on each hint ring; [SOLID_RING] for a solid stroke.
 * @property halo whether a band of `surfaceContainerLowest` runs inside the selection ring (CV-07).
 * @property unearnedStarHollow whether an unearned star is the hollow star, its interior open, rather
 *   than the filled shape drawn before CV-13a (CV-10a); hollow in both palettes, so earned and
 *   unearned stars never differ by colour alone (CV-13a).
 */
@Immutable
data class BoardMarkRecipe(
    val selectedRingRatio: Float,
    val hintSourceRingRatio: Float,
    val hintTargetRingRatio: Float,
    val destinationDotAlpha: Float,
    val hintRingAlpha: Float,
    val hintConnectorAlpha: Float,
    val hintRingDashes: Int,
    val halo: Boolean,
    val unearnedStarHollow: Boolean,
)

/** The recipe of this palette: [BoardMetrics]' values for Standard, [ColorblindBoardMetrics]' for Colorblind. */
val BoardMarkPalette.recipe: BoardMarkRecipe
    get() =
        when (this) {
            BoardMarkPalette.Standard -> STANDARD_RECIPE
            BoardMarkPalette.Colorblind -> COLORBLIND_RECIPE
        }

/** A hint ring with no dash pattern. */
const val SOLID_RING = 0

/**
 * CV-13: exactly the board as it was drawn before the option existed. CV-13a: the unearned star is
 * hollow, its only change of form.
 */
private val STANDARD_RECIPE =
    BoardMarkRecipe(
        selectedRingRatio = BoardMetrics.SELECTED_RING_RATIO,
        hintSourceRingRatio = BoardMetrics.HINT_SOURCE_RING_RATIO,
        hintTargetRingRatio = BoardMetrics.HINT_TARGET_RING_RATIO,
        destinationDotAlpha = BoardMetrics.DESTINATION_DOT_ALPHA,
        hintRingAlpha = BoardMetrics.HINT_HIGHLIGHT_ALPHA,
        hintConnectorAlpha = BoardMetrics.HINT_ARROW_ALPHA,
        hintRingDashes = SOLID_RING,
        halo = false,
        unearnedStarHollow = true,
    )

/** CV-07 to CV-10. */
private val COLORBLIND_RECIPE =
    BoardMarkRecipe(
        selectedRingRatio = ColorblindBoardMetrics.SELECTED_RING_RATIO,
        hintSourceRingRatio = ColorblindBoardMetrics.HINT_SOURCE_RING_RATIO,
        hintTargetRingRatio = ColorblindBoardMetrics.HINT_TARGET_RING_RATIO,
        destinationDotAlpha = ColorblindBoardMetrics.DESTINATION_DOT_ALPHA,
        hintRingAlpha = ColorblindBoardMetrics.HINT_HIGHLIGHT_ALPHA,
        hintConnectorAlpha = ColorblindBoardMetrics.HINT_ARROW_ALPHA,
        hintRingDashes = ColorblindBoardMetrics.HINT_RING_DASHES,
        halo = true,
        unearnedStarHollow = true,
    )

/**
 * Every mark a palette draws: the five that can share a board, and the two stars of the star rows.
 * [WrongTapMark] is audio's reduced-motion mark (`BoardMetrics.WRONG_TAP_MARK_*`): it shares the
 * board with the hint rings and is dashed too, so it belongs in the pairing; it is the same in both
 * palettes.
 */
enum class BoardMark {
    SelectionRing,
    LegalTarget,
    HintSourceRing,
    HintTargetRing,
    WrongTapMark,
    EarnedStar,
    UnearnedStar,
}

/** The shape family of a mark. */
enum class MarkForm { Ring, Disc, Glyph }

/**
 * A mark's channels other than hue: its form, its radius (× cell edge; `null` for a glyph sized by
 * its row), its dash count ([SOLID_RING] for none), whether it carries the halo, and whether it is
 * filled. Two marks that can be seen together must differ in at least one of these (CV-AC08).
 */
data class MarkLook(
    val form: MarkForm,
    val radiusRatio: Float?,
    val dashes: Int,
    val halo: Boolean,
    val filled: Boolean,
)

/** How this recipe draws [mark], in every channel but hue. */
fun BoardMarkRecipe.look(mark: BoardMark): MarkLook =
    when (mark) {
        BoardMark.SelectionRing -> MarkLook(MarkForm.Ring, selectedRingRatio, SOLID_RING, halo, filled = false)
        BoardMark.LegalTarget ->
            MarkLook(MarkForm.Disc, BoardMetrics.DESTINATION_DOT_RATIO, SOLID_RING, halo = false, filled = true)
        BoardMark.HintSourceRing ->
            MarkLook(MarkForm.Ring, hintSourceRingRatio, hintRingDashes, halo = false, filled = false)
        BoardMark.HintTargetRing ->
            MarkLook(MarkForm.Ring, hintTargetRingRatio, hintRingDashes, halo = false, filled = false)
        BoardMark.WrongTapMark ->
            MarkLook(
                MarkForm.Ring,
                BoardMetrics.WRONG_TAP_MARK_RING_RATIO,
                BoardMetrics.WRONG_TAP_MARK_DASHES,
                halo = false,
                filled = false,
            )
        BoardMark.EarnedStar -> MarkLook(MarkForm.Glyph, radiusRatio = null, SOLID_RING, halo = false, filled = true)
        BoardMark.UnearnedStar ->
            MarkLook(MarkForm.Glyph, radiusRatio = null, SOLID_RING, halo = false, filled = !unearnedStarHollow)
    }
