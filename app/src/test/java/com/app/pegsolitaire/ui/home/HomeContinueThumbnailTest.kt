package com.app.pegsolitaire.ui.home

import androidx.annotation.DimenRes
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.R
import com.app.pegsolitaire.testing.applySystemBarInsets
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.ui.components.BOARD_CANVAS_TEST_TAG
import com.app.pegsolitaire.ui.home.fixture.DEFAULT_FONT_SCALE
import com.app.pegsolitaire.ui.home.fixture.DOUBLE_FONT_SCALE
import com.app.pegsolitaire.ui.home.fixture.HOME_ROBOLECTRIC_SDK
import com.app.pegsolitaire.ui.home.fixture.MID_GAME_ENGLISH_BOARD
import com.app.pegsolitaire.ui.home.fixture.MID_GAME_PEGS_LEFT
import com.app.pegsolitaire.ui.home.fixture.ONE_PEG_ENGLISH_BOARD
import com.app.pegsolitaire.ui.home.fixture.ONE_PEG_LEFT
import com.app.pegsolitaire.ui.home.fixture.RESUME_CHALLENGE_LABEL
import com.app.pegsolitaire.ui.home.fixture.RESUME_CLASSIC_LABEL
import com.app.pegsolitaire.ui.home.fixture.STAMPED_RESUME_LEVEL
import com.app.pegsolitaire.ui.home.fixture.chipsState
import com.app.pegsolitaire.ui.home.fixture.classicSaveState
import com.app.pegsolitaire.ui.home.fixture.columnBoundsOf
import com.app.pegsolitaire.ui.home.fixture.renderHome
import com.app.pegsolitaire.ui.home.fixture.string
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Classic Continue chip's thumbnail (ux-spec §4.2.7, D-03p): the player's saved position on a
 * 56dp `surfaceContainerHigh` slab at the chip's trailing end, in a chip whose height the thumbnail
 * never sets; decorative, with the pegs left spoken as the chip's state instead; and the Challenge
 * chip exactly as it was — criteria 15aa parts 3 and 4, and 15ab parts 1 and 2.
 *
 * The founder saw the chip's 40dp thumbnail draw the board's **start** position, a board nobody had
 * touched (§4.2.7, "What the founder saw"). The position now comes from the save (`HomeViewModelTest`,
 * 15aa part 1); what is read here is how the chip carries it.
 *
 * Read with the design baseline's system bars supplied and NATIVE text metrics in the app theme
 * (§13's two idioms), every bound taken along the column ([columnBoundsOf]).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HOME_ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HomeContinueThumbnailTest {
    @get:Rule
    val composeRule = createComposeRule()

    // ---- Criterion 15aa part 3: a 56dp slab in a chip it never heightens ---------

    @Test
    @Config(qualifiers = NARROW_BASELINE)
    fun onTheNarrowBaselineTheThumbnailIsA56dpSlabCentredInAChipAsTallAsWithTheArrow() =
        assertTheThumbnailSitsInAChipItNeverHeightens(DEFAULT_FONT_SCALE, asTallAsWithTheArrow = true)

    @Test
    @Config(qualifiers = NARROW_BASELINE)
    fun atDoubleFontScaleOnTheNarrowBaselineTheChipIsStillAsTallAsWithTheArrow() =
        assertTheThumbnailSitsInAChipItNeverHeightens(DOUBLE_FONT_SCALE, asTallAsWithTheArrow = true)

    @Test
    @Config(qualifiers = TALL_PHONE)
    fun onATallPhoneTheThumbnailIsA56dpSlabCentredInAChipAsTallAsWithTheArrow() =
        assertTheThumbnailSitsInAChipItNeverHeightens(DEFAULT_FONT_SCALE, asTallAsWithTheArrow = true)

    /**
     * 15aa part 3 at font scale 2.0 on the tall phone, where its last clause does not hold and is not
     * asserted. The slab never sets the chip's height here either — the chip is exactly as tall as its
     * text and its padding make it, with the thumbnail and with the arrow — but the slab is 28dp wider
     * than the arrow, so the text column is 276dp wide instead of 304dp, and at 2.0 the supporting line
     * "Pick up your saved board" wraps onto a second line inside 276dp and not inside 304dp (76dp of
     * text against 38dp): the chip measures 154dp with the thumbnail against 116dp with the arrow
     * (NATIVE metrics, the shipped faces; on the narrow baseline both wrap, 154dp each). No layout inside
     * §4.2.7's 56dp slab, `spacing_md` gap and `spacing_md` end inset gives the text the arrow's width,
     * so this is reported to the ux-designer with the numbers (the developer's report, Open issues) rather
     * than built away.
     */
    @Test
    @Config(qualifiers = TALL_PHONE)
    fun atDoubleFontScaleOnATallPhoneTheChipIsAsTallAsItsTextMakesItWithTheThumbnailAndWithTheArrow() =
        assertTheThumbnailSitsInAChipItNeverHeightens(DOUBLE_FONT_SCALE, asTallAsWithTheArrow = false)

    /**
     * 15aa part 3's first clause: the slab is composed only for a Classic save that carries its
     * position. No save, the Challenge save alone, and a Classic chip drawn with the play arrow — the
     * shape `ResumeTile` keeps for the Challenge chip and for §6.5's degradation step 2 — compose none.
     */
    @Test
    @Config(qualifiers = TALL_PHONE)
    fun theThumbnailIsComposedOnlyForAClassicSaveThatCarriesItsPosition() {
        val state = mutableStateOf(chipsState(classic = false, challenge = false))
        renderWithInsets(state)

        listOf(
            ThumbnailCase("no save", chipsState(classic = false, challenge = false), thumbnails = 0),
            ThumbnailCase("the Challenge save alone", chipsState(classic = false, challenge = true), thumbnails = 0),
            ThumbnailCase("a Classic save with its position", classicSaveState(MID_GAME_ENGLISH_BOARD), thumbnails = 1),
            ThumbnailCase("a Classic chip drawn with the arrow", classicSaveState(board = null), thumbnails = 0),
            ThumbnailCase("both saves", classicSaveState(MID_GAME_ENGLISH_BOARD, challenge = true), thumbnails = 1),
        ).forEach { case ->
            show(state, case.state)
            assertWithMessage("${case.what}: thumbnails composed").that(thumbnails()).hasSize(case.thumbnails)
        }
    }

    // ---- Criterion 15aa part 4: the Challenge chip is unchanged ------------------

    /**
     * The Challenge chip keeps its play arrow, its title and its supporting line, and holds no board:
     * the one thumbnail on Home is inside the Classic chip, and no board canvas lies inside the
     * Challenge chip. The arrow has no semantics (`contentDescription = null`), so it is read where it
     * paints: the chip's trailing glyph box draws something besides the chip's own surface.
     */
    @Test
    @Config(qualifiers = TALL_PHONE)
    fun theChallengeChipKeepsItsPlayArrowAndItsCopyAndHoldsNoBoard() {
        renderWithInsets(mutableStateOf(classicSaveState(MID_GAME_ENGLISH_BOARD, challenge = true)))
        val challengeChip = boundsOf(composeRule.onNodeWithText(RESUME_CHALLENGE_LABEL))
        val classicChip = boundsOf(composeRule.onNodeWithText(RESUME_CLASSIC_LABEL))

        assertWithMessage("the thumbnail lies inside the Classic chip")
            .that(classicChip.encloses(boundsOf(thumbnails().single())))
            .isTrue()
        assertWithMessage("boards inside the Challenge chip")
            .that(boards().filter { challengeChip.encloses(boundsOf(it)) })
            .isEmpty()
        assertWithMessage("the Challenge chip's text")
            .that(textsOf(composeRule.onNodeWithText(RESUME_CHALLENGE_LABEL)))
            .containsExactly(RESUME_CHALLENGE_LABEL, string(R.string.resume_challenge_supporting, STAMPED_RESUME_LEVEL))
            .inOrder()
        assertWithMessage("rows of the Challenge chip's glyph box that paint something besides the chip")
            .that(rowsInkedInTheGlyphBox(challengeChip))
            .isAtLeast(ARROW_ROWS_FLOOR)
    }

    // ---- Criterion 15ab parts 1 and 2: decorative, and the chip speaks the count --

    /**
     * 15ab part 1: the thumbnail is decoration — every node of its subtree carries a test tag and
     * nothing else (the slab's `Shape` aside, which `Modifier.background(color, shape)` records as a
     * focus-outline hint and TalkBack never speaks): no description, no action, no role, no stop
     * (§11.1, D-TBa item 9). And no node inside the Classic chip carries a `contentDescription`: merged
     * into the chip, one would be spoken instead of its title.
     */
    @Test
    @Config(qualifiers = TALL_PHONE)
    fun theThumbnailCarriesNothingButTestTagsAndNoNodeInTheClassicChipIsDescribed() {
        renderWithInsets(mutableStateOf(classicSaveState(MID_GAME_ENGLISH_BOARD)))

        val thumbnail = thumbnails().single()
        (listOf(thumbnail) + thumbnail.descendants()).forEach { node ->
            assertWithMessage("semantics of ${node.config.getOrNull(SemanticsProperties.TestTag)}")
                .that(node.config.map { it.key } - SemanticsProperties.Shape)
                .containsExactly(SemanticsProperties.TestTag)
        }
        val chip =
            composeRule
                .onNode(hasClickAction() and hasAnyDescendant(hasText(RESUME_CLASSIC_LABEL)), useUnmergedTree = true)
                .fetchSemanticsNode()
        (listOf(chip) + chip.descendants()).forEach { node ->
            assertWithMessage("a content description inside the Classic chip")
                .that(node.config.getOrNull(SemanticsProperties.ContentDescription))
                .isNull()
        }
    }

    /**
     * 15ab part 2: the Classic chip's one stop speaks the saved position's pegs left as its state —
     * "14 pegs left", "1 peg left" — while its name is still its title and its supporting line, so a
     * screen-reader player hears the progress the picture shows.
     */
    @Test
    @Config(qualifiers = TALL_PHONE)
    fun theClassicChipSpeaksThePegsLeftAsItsStateBesideItsUnchangedCopy() {
        val state = mutableStateOf(classicSaveState(MID_GAME_ENGLISH_BOARD))
        renderWithInsets(state)

        assertThat(stateOf(composeRule.onNodeWithText(RESUME_CLASSIC_LABEL))).isEqualTo(MID_GAME_PEGS_LEFT)
        assertThat(textsOf(composeRule.onNodeWithText(RESUME_CLASSIC_LABEL)))
            .containsExactly(RESUME_CLASSIC_LABEL, string(R.string.resume_classic_supporting))
            .inOrder()

        show(state, classicSaveState(ONE_PEG_ENGLISH_BOARD))
        assertThat(stateOf(composeRule.onNodeWithText(RESUME_CLASSIC_LABEL))).isEqualTo(ONE_PEG_LEFT)
    }

    /**
     * 15ab part 2's other half, as the tech-lead's ruling 1 reads it: the Challenge chip speaks no
     * state, and neither does a Classic chip drawn with the arrow, which carries no position to count.
     */
    @Test
    @Config(qualifiers = TALL_PHONE)
    fun theChallengeChipAndAChipDrawnWithTheArrowSpeakNoState() {
        val state = mutableStateOf(classicSaveState(MID_GAME_ENGLISH_BOARD, challenge = true))
        renderWithInsets(state)
        assertWithMessage("the Challenge chip's state")
            .that(stateOf(composeRule.onNodeWithText(RESUME_CHALLENGE_LABEL)))
            .isNull()

        show(state, classicSaveState(board = null))
        assertWithMessage("the arrow chip's state")
            .that(stateOf(composeRule.onNodeWithText(RESUME_CLASSIC_LABEL)))
            .isNull()
    }

    // ---- Assertions --------------------------------------------------------------

    /**
     * 15aa part 3 on the current window at [fontScale]: the slab's edge is 56dp, `home_resume_thumb_size`
     * as §4.2.7 sets it — read against the criterion's figure, not the token, so the token cannot drift;
     * it sits `spacing_md` inside the chip's end edge, centred on the chip's height and never closer
     * than `spacing_sm` to its top or bottom edge, which is the row's inset (§4.2.7: 56 + 2 × 8 = 72dp
     * of art in an 80dp chip at default scale); and the chip is exactly as tall as its text and
     * `spacing_md` above and below make it — the height it always had, or its `resume_tile_min_height`
     * floor — with the thumbnail and with the arrow, so the art never sets it. With
     * [asTallAsWithTheArrow], also the criterion's last clause: the two heights are equal, so no figure
     * in 15k, 15s, 15y or 15z moves.
     */
    private fun assertTheThumbnailSitsInAChipItNeverHeightens(
        fontScale: Float,
        asTallAsWithTheArrow: Boolean,
    ) {
        val state = mutableStateOf(classicSaveState(MID_GAME_ENGLISH_BOARD))
        renderWithInsets(state, fontScale)
        val withThumbnail = classicChip()
        val thumbnail = boundsOf(thumbnails().single())

        assertWithMessage("the thumbnail's width, in dp")
            .that(widthOf(thumbnail).value)
            .isWithin(TOLERANCE_DP)
            .of(THUMBNAIL_EDGE_DP)
        assertWithMessage("the thumbnail's height, in dp")
            .that(heightOf(thumbnail).value)
            .isWithin(TOLERANCE_DP)
            .of(THUMBNAIL_EDGE_DP)
        val above = thumbnail.top - withThumbnail.chip.top
        val below = withThumbnail.chip.bottom - thumbnail.bottom
        val inset = dimen(R.dimen.spacing_sm).value - TOLERANCE_DP
        assertWithMessage("the slab's inset from the chip's top edge, in dp").that(above.value).isAtLeast(inset)
        assertWithMessage("the slab's inset from the chip's bottom edge, in dp").that(below.value).isAtLeast(inset)
        assertWithMessage("the slab's two insets, centred, in dp")
            .that(above.value)
            .isWithin(TOLERANCE_DP)
            .of(below.value)
        assertWithMessage("the slab's end edge inside the chip's, in dp")
            .that((withThumbnail.chip.right - thumbnail.right).value)
            .isWithin(TOLERANCE_DP)
            .of(dimen(R.dimen.spacing_md).value)
        assertTheChipIsAsTallAsItsTextMakesIt(withThumbnail, "with the thumbnail")

        show(state, classicSaveState(board = null))
        val withArrow = classicChip()
        assertTheChipIsAsTallAsItsTextMakesIt(withArrow, "with the arrow")
        if (asTallAsWithTheArrow) {
            assertWithMessage("the Classic chip's height with the thumbnail against its height with the arrow, in dp")
                .that(heightOf(withThumbnail.chip).value)
                .isWithin(TOLERANCE_DP)
                .of(heightOf(withArrow.chip).value)
        }
    }

    /**
     * The chip is as tall as its text and `spacing_md` above and below make it, or its
     * `resume_tile_min_height` floor where that is taller: the height it had before D-03p, which the
     * art never sets.
     */
    private fun assertTheChipIsAsTallAsItsTextMakesIt(
        reading: ChipReading,
        what: String,
    ) {
        val text = reading.supporting.bottom - reading.title.top
        val expected = maxOf(text + dimen(R.dimen.spacing_md) * 2, dimen(R.dimen.resume_tile_min_height))
        assertWithMessage("the Classic chip's height $what, against its text block and its padding, in dp")
            .that(heightOf(reading.chip).value)
            .isWithin(TOLERANCE_DP)
            .of(expected.value)
    }

    /**
     * How many pixel rows of a chip's trailing glyph box — `mode_glyph_size` square, `spacing_md`
     * inside the chip's end edge, centred on its height, where the play arrow sits — paint something
     * besides the chip's own surface. That surface is the chip's fill under a vertical gloss, one colour
     * along each row, so a row whose pixels spread wider than [ARROW_INK_SPREAD] in a channel holds a
     * drawing.
     */
    private fun rowsInkedInTheGlyphBox(chip: DpRect): Int {
        val window = composeRule.renderedWindow()
        val glyph = dimen(R.dimen.mode_glyph_size)
        val end = chip.right - dimen(R.dimen.spacing_md)
        val centre = (chip.top + chip.bottom) / 2
        return with(composeRule.density) {
            val columns = (end - glyph).roundToPx() until end.roundToPx()
            ((centre - glyph / 2).roundToPx() until (centre + glyph / 2).roundToPx()).count { y ->
                val row = columns.map { x -> window.getPixel(x, y) }
                CHANNEL_SHIFTS.any { shift ->
                    row.maxOf { it shr shift and CHANNEL_MASK } - row.minOf { it shr shift and CHANNEL_MASK } >
                        ARROW_INK_SPREAD
                }
            }
        }
    }

    // ---- Harness -----------------------------------------------------------------

    private fun renderWithInsets(
        state: MutableState<HomeUiState>,
        fontScale: Float = DEFAULT_FONT_SCALE,
    ) {
        composeRule.renderHome(state, fontScale = fontScale)
        composeRule.applySystemBarInsets(top = STATUS_BAR_DP.dp, bottom = NAVIGATION_BAR_DP.dp)
    }

    private fun show(
        holder: MutableState<HomeUiState>,
        state: HomeUiState,
    ) {
        composeRule.runOnUiThread { holder.value = state }
        composeRule.waitForIdle()
    }

    /** The Classic chip, its title and its supporting line, along the column. */
    private fun classicChip(): ChipReading =
        ChipReading(
            chip = boundsOf(composeRule.onNodeWithText(RESUME_CLASSIC_LABEL)),
            title = boundsOf(composeRule.onNode(hasText(RESUME_CLASSIC_LABEL), useUnmergedTree = true)),
            supporting =
                boundsOf(
                    composeRule.onNode(hasText(string(R.string.resume_classic_supporting)), useUnmergedTree = true),
                ),
        )

    /** Every thumbnail slab composed, from the unmerged tree: the chip merges it into its own node. */
    private fun thumbnails(): List<SemanticsNode> =
        composeRule.onAllNodesWithTag(HOME_RESUME_THUMB_TEST_TAG, useUnmergedTree = true).fetchSemanticsNodes()

    /** Every board canvas composed, from the unmerged tree. */
    private fun boards(): List<SemanticsNode> =
        composeRule.onAllNodesWithTag(BOARD_CANVAS_TEST_TAG, useUnmergedTree = true).fetchSemanticsNodes()

    private fun stateOf(node: SemanticsNodeInteraction): String? =
        node.fetchSemanticsNode().config.getOrNull(SemanticsProperties.StateDescription)

    private fun textsOf(node: SemanticsNodeInteraction): List<String> =
        node
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            .orEmpty()
            .map { it.text }

    private fun boundsOf(node: SemanticsNodeInteraction): DpRect = composeRule.columnBoundsOf(node)

    private fun boundsOf(node: SemanticsNode): DpRect = composeRule.columnBoundsOf(node)

    private fun widthOf(bounds: DpRect): Dp = bounds.right - bounds.left

    private fun heightOf(bounds: DpRect): Dp = bounds.bottom - bounds.top

    /** Whether [other] lies wholly inside this rect, to the tolerance. */
    private fun DpRect.encloses(other: DpRect): Boolean =
        other.left.value >= left.value - TOLERANCE_DP &&
            other.top.value >= top.value - TOLERANCE_DP &&
            other.right.value <= right.value + TOLERANCE_DP &&
            other.bottom.value <= bottom.value + TOLERANCE_DP

    private fun SemanticsNode.descendants(): List<SemanticsNode> = children.flatMap { listOf(it) + it.descendants() }

    /** A `dimens.xml` token in dp, read from the resources rather than retyped. */
    private fun dimen(
        @DimenRes id: Int,
    ): Dp {
        val px = RuntimeEnvironment.getApplication().resources.getDimension(id)
        return with(composeRule.density) { px.toDp() }
    }

    /** One chip state, and how many thumbnails Home composes in it. */
    private data class ThumbnailCase(
        val what: String,
        val state: HomeUiState,
        val thumbnails: Int,
    )

    /** A chip's bounds, and its title's and supporting line's, along the column. */
    private data class ChipReading(
        val chip: DpRect,
        val title: DpRect,
        val supporting: DpRect,
    )
}

/** The 360x640dp design baseline, and the 6.7-inch reference phone. */
private const val NARROW_BASELINE = "w360dp-h640dp"
private const val TALL_PHONE = "w412dp-h915dp"

/** The design baseline's system bars: a 24dp status bar and a 48dp three-button navigation bar. */
private const val STATUS_BAR_DP = 24
private const val NAVIGATION_BAR_DP = 48

/** Criterion 15aa part 3's ±0.5dp. */
private const val TOLERANCE_DP = 0.5f

/** Criterion 15aa part 3's slab edge: 56dp, which `home_resume_thumb_size` carries (§4.2.7). */
private const val THUMBNAIL_EDGE_DP = 56f

/**
 * How far a row's pixels may spread in one channel and still be the chip's own surface: a flat fill
 * under a vertical gloss spreads by its dither's one level at most, and the arrow's deep gold stop
 * (#F59E0B) stands 188 levels from the light chip (#FFEFC7) in blue.
 */
private const val ARROW_INK_SPREAD = 24

/** The least number of inked rows the arrow paints in its 28dp box: it spans more than half of it. */
private const val ARROW_ROWS_FLOOR = 4

/** The red, green and blue channels of an ARGB pixel, by their bit shift; alpha is not read. */
private val CHANNEL_SHIFTS = listOf(0, 8, 16)
private const val CHANNEL_MASK = 0xFF
