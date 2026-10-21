package com.app.pegsolitaire.ui.game

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.R
import com.app.pegsolitaire.testing.renderedWindow
import com.app.pegsolitaire.testing.renderedWindowWithShadows
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Criterion 21e — **only the toggle is ever on or off** (ux-spec §4.4.1, §5, D-04e) — and the disc
 * each look draws, fill, ring and shadow, as §4.4.1's table of the five looks requires.
 *
 * Show-moves On draws the on mark and the plain [ShowMovesIcon]; Off draws [ShowMovesOffIcon] and
 * no mark; no action, in any look, draws the mark, the slashed glyph, a `primary` fill or a ring.
 *
 * Two halves, because "draws" has two halves. Which looks a control can **reach** is a semantics
 * fact, asserted over every state the dock can be put in. What a look **draws** is a pixel fact —
 * the on mark is drawn rather than composed (it is not a node, §4.4.1), and a disc's fill, ring and
 * shadow are colours no semantics assertion can see — so this class reads them off the rendered
 * dock. The recipe being right is [DockLookRecipeTest]'s to prove; that the dock draws what the
 * table says is this class's, which is why its expected discs are typed in from the table rather
 * than read from [DockLook.recipe].
 *
 * ## Two renders
 *
 * The mark, the slash, the fills and the ring are read from the flat render, where every colour is
 * exact. A shadow exists only in the composited render — see `renderedWindow` and
 * `renderedWindowWithShadows` in the `testing` package for why each is what it is.
 *
 * ## Where the pixels are read
 *
 * Each point sits inside a flat region, away from every edge, so anti-aliasing never decides a
 * result — and each is derived from §4.4.1's geometry, not from the drawing code:
 *
 * - **The on mark.** Its 24dp circle is centred on the disc's rim at the top-end 45° point and
 *   bordered by a 2dp `primary` stroke. The point read is the middle of that stroke, [MARK_PROBE_DP]
 *   out from the mark's centre along the same diagonal: outside the disc, clear of the check, and
 *   `primary` against a slab it cannot be mistaken for. Where there is no mark, it is bare slab.
 * - **The slash.** It runs from the glyph's top-start corner to its bottom-end corner, and
 *   [ShowMovesIcon]'s three circles never reach that corner. The point read is on the slash's centre
 *   line, [SLASH_PROBE_UNITS] into the 24-unit glyph viewport: ink when slashed, and the disc or the
 *   slab showing through when not.
 * - **The fill.** On the start side, midway between the 28dp glyph's edge and the inner edge of
 *   the 2dp ring that sits inside the 56dp disc's rim: always the disc, never the glyph or a ring.
 * - **The ring.** On the start side, in the middle of that 2dp ring: the ring's colour where there
 *   is one, the fill beneath it where there is none.
 * - **The shadow.** The band `elevation_pressed` (2dp) deep straight below the rim, where a disc
 *   raised on that elevation casts its shadow onto the slab and a flat disc leaves bare slab.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [MARK_ROBOLECTRIC_SDK], qualifiers = MARK_WINDOW_QUALIFIER)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DockOnMarkAndSlashTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var scheme: ColorScheme

    /**
     * The reach half: across every combination of the three inputs the dock reads, the actions
     * only ever report action looks and the toggle only ever reports On or Off. Together with the
     * recipes of the action looks — asserted in the same breath — that is the whole of "no action
     * ever draws a mark, a slash, a strong fill or a ring": those four channels are On's and Off's.
     */
    @Test
    fun noActionEverReachesOnOrOffAndTheToggleReachesNothingElse() {
        val flags = mutableStateOf(EVERY_DOCK_STATE.first())
        composeRule.setContent { ThemedDock(flags.value) }

        val reached = mutableMapOf<Int, MutableSet<DockLook>>()
        EVERY_DOCK_STATE.forEach { state ->
            setFlags(flags, state)
            DOCK_CONTROLS.forEach { control ->
                reached.getOrPut(control) { mutableSetOf() } += lookOf(control, state)
            }
        }

        assertThat(reached.getValue(R.string.undo_action)).containsExactly(DockLook.Available, DockLook.Unavailable)
        assertThat(reached.getValue(R.string.hint_action)).containsExactly(DockLook.Available, DockLook.Working)
        assertThat(reached.getValue(R.string.show_moves_on)).containsExactly(DockLook.On, DockLook.Off)
        assertThat(reached.getValue(R.string.restart_board)).containsExactly(DockLook.Available)
        ACTION_LOOKS.forEach { look ->
            val recipe = look.recipe
            assertWithMessage("$look draws the on mark").that(recipe.onMark).isFalse()
            assertWithMessage("$look draws a ring").that(recipe.ring).isFalse()
            assertWithMessage("$look draws a strong fill").that(recipe.fill).isNotEqualTo(DockFill.Strong)
            assertWithMessage("$look draws a slash").that(recipe.glyph).isNotEqualTo(DockGlyph.Slashed)
        }
    }

    /** Show-moves On draws the on mark; the same point beside the Off toggle is bare slab. */
    @Test
    fun onlyShowMovesOnDrawsTheOnMark() {
        val flags = mutableStateOf(DockFlags(showHints = true, canUndo = true, solverThinking = false))
        composeRule.setContent { ThemedDock(flags.value) }

        assertNear("Show moves, on", pixelAt(R.string.show_moves_on, ::markProbe), scheme.primary)

        setFlags(flags, DockFlags(showHints = false, canUndo = true, solverThinking = false))
        assertNear("Show moves, off", pixelAt(R.string.show_moves_on, ::markProbe), scheme.surfaceContainerHigh)
    }

    /** No action draws the on mark, in any of the looks it can take. */
    @Test
    fun noActionInAnyLookDrawsTheOnMark() {
        val flags = mutableStateOf(DockFlags(showHints = true, canUndo = true, solverThinking = false))
        composeRule.setContent { ThemedDock(flags.value) }

        listOf(R.string.undo_action, R.string.hint_action, R.string.restart_board).forEach { action ->
            assertNear(string(action), pixelAt(action, ::markProbe), scheme.surfaceContainerHigh)
        }

        setFlags(flags, DockFlags(showHints = true, canUndo = false, solverThinking = true))
        listOf(R.string.undo_action, R.string.hint_thinking).forEach { action ->
            assertNear(string(action), pixelAt(action, ::markProbe), scheme.surfaceContainerHigh)
        }
    }

    /** Show-moves Off draws the slashed glyph; On draws the plain one, so its disc shows through. */
    @Test
    fun onlyShowMovesOffDrawsTheSlashedGlyph() {
        val flags = mutableStateOf(DockFlags(showHints = false, canUndo = true, solverThinking = false))
        composeRule.setContent { ThemedDock(flags.value) }

        assertNear("Show moves, off", pixelAt(R.string.show_moves_on) { slashProbe() }, scheme.onSurfaceVariant)

        setFlags(flags, DockFlags(showHints = true, canUndo = true, solverThinking = false))
        assertNear("Show moves, on", pixelAt(R.string.show_moves_on) { slashProbe() }, scheme.primary)
    }

    /**
     * The fill half of criterion 21e, drawn: every control, in every look it can reach, fills its
     * disc with its look's tone and no other — `primaryContainer` for Available and Working,
     * `primary` for On alone, and nothing for Unavailable and Off, so the slab shows through.
     *
     * Read with a tolerance of one level, because the pale disc and the slab are ≈1.04:1 apart
     * (§4.4.1) — four to six levels a channel — and a looser read could not tell a pale disc from
     * no disc at all.
     */
    @Test
    fun everyControlFillsItsDiscWithTheToneItsLookRequires() =
        forEveryReachableLook(render = { composeRule.renderedWindow() }) { window, row ->
            assertNear(
                "${row.what}: the fill",
                window.pixelAt(disc(row.control), ::fillProbe),
                fillColour(row.expected.fill),
                EXACT_TOLERANCE,
            )
        }

    /**
     * The ring half of criterion 21e, drawn: Show-moves Off rings its disc in `onSurfaceVariant`,
     * and nothing else ever does — at the same point every other look shows its own fill.
     */
    @Test
    fun onlyShowMovesOffRingsItsDisc() =
        forEveryReachableLook(render = { composeRule.renderedWindow() }) { window, row ->
            val expected = if (row.expected.ring) scheme.onSurfaceVariant else fillColour(row.expected.fill)
            assertNear(
                "${row.what}: the ring",
                window.pixelAt(disc(row.control), ::ringProbe),
                expected,
                EXACT_TOLERANCE,
            )
        }

    /**
     * The shadow channel, drawn: Available and On sit raised on `elevation_pressed` and cast a
     * shadow onto the slab below the rim; Working, Unavailable and Off are flat, and the slab under
     * them is bare. It is one of the two channels that keep Working apart from Available, and
     * Unavailable apart from On (§4.4.1's pair table), so a disc that keeps its shadow in a flat
     * look brings back a pair the founder could not tell apart.
     */
    @Test
    fun onlyTheRaisedLooksCastAShadowOntoTheSlab() =
        forEveryReachableLook(render = { composeRule.renderedWindowWithShadows() }) { window, row ->
            val darkening = window.shadowBelow(disc(row.control))
            if (row.expected.raised) {
                assertWithMessage("${row.what}: the slab under a raised disc is darkened by")
                    .that(darkening)
                    .isAtLeast(RAISED_MIN_DARKENING)
            } else {
                assertWithMessage("${row.what}: the slab under a flat disc is darkened by")
                    .that(darkening)
                    .isAtMost(EXACT_TOLERANCE)
            }
        }

    // ---- Rendering -------------------------------------------------------------

    /** The real dock over [flags], in the light theme, recording the scheme it was drawn with. */
    @Composable
    private fun ThemedDock(flags: DockFlags) {
        AppTheme(darkTheme = false) {
            scheme = MaterialTheme.colorScheme
            GameActionDock(
                showHints = flags.showHints,
                canUndo = flags.canUndo,
                solverThinking = flags.solverThinking,
                onUndo = {},
                onHint = {},
                onToggleHints = {},
                onRestart = {},
            )
        }
    }

    private fun setFlags(
        holder: MutableState<DockFlags>,
        flags: DockFlags,
    ) {
        composeRule.runOnUiThread { holder.value = flags }
        composeRule.waitForIdle()
    }

    /**
     * Puts the dock in each state [DRAWN_ROWS] names, renders it once per state with [render], and
     * hands every row of that state to [check] — every control in every look it can reach.
     */
    private fun forEveryReachableLook(
        render: () -> Bitmap,
        check: (window: Bitmap, row: DrawnRow) -> Unit,
    ) {
        val rowsByState = DRAWN_ROWS.groupBy { it.flags }
        val flags = mutableStateOf(rowsByState.keys.first())
        composeRule.setContent { ThemedDock(flags.value) }

        rowsByState.forEach { (state, rows) ->
            setFlags(flags, state)
            val window = render()
            rows.forEach { row -> check(window, row) }
        }
    }

    /**
     * The colour at [probe]'s offset from the centre of the dock control named [control], read
     * once every animation has settled — a mark still scaling in is not a mark yet.
     *
     * @param probe maps the control's radius in px to the offset to read, in px.
     */
    private fun pixelAt(
        @StringRes control: Int,
        probe: (radiusPx: Float) -> Offset,
    ): Int {
        composeRule.waitForIdle()
        return composeRule.renderedWindow().pixelAt(disc(control), probe)
    }

    /** The disc of the dock control named [control]: its own node, which the disc wraps exactly. */
    private fun disc(
        @StringRes control: Int,
    ): Rect =
        composeRule
            .onNode(hasContentDescription(string(control)) and hasLook())
            .fetchSemanticsNode()
            .boundsInRoot

    /** The colour at [probe]'s offset from the centre of [disc]. */
    private fun Bitmap.pixelAt(
        disc: Rect,
        probe: (radiusPx: Float) -> Offset,
    ): Int {
        val offset = probe(disc.width / 2)
        return getPixel((disc.center.x + offset.x).toInt(), (disc.center.y + offset.y).toInt())
    }

    /**
     * How much darker than the slab the band straight below [disc]'s rim is drawn, summed over its
     * rows and its three colour channels: zero where nothing is cast onto the slab.
     */
    private fun Bitmap.shadowBelow(disc: Rect): Int {
        val slab = scheme.surfaceContainerHigh.toArgb()
        val x = disc.center.x.toInt()
        val rim = ceil(disc.bottom).toInt()
        return (rim until rim + ceil(dpToPx(SHADOW_BAND_DP)).toInt()).sumOf { y ->
            val drawn = getPixel(x, y)
            COLOUR_CHANNELS.sumOf { shift -> max(0, (slab shr shift and CHANNEL) - (drawn shr shift and CHANNEL)) }
        }
    }

    /**
     * On the start side, midway between the glyph's edge and the inner edge of the ring: the disc
     * itself, whatever glyph or ring the look draws.
     */
    private fun fillProbe(radiusPx: Float): Offset {
        val glyphEdge = dpToPx(GLYPH_DP / 2)
        val ringInnerEdge = radiusPx - dpToPx(RING_DP)
        return Offset(-(glyphEdge + ringInnerEdge) / 2, 0f)
    }

    /** On the start side, in the middle of the ring drawn inside the disc's rim. */
    private fun ringProbe(radiusPx: Float): Offset = Offset(-(radiusPx - dpToPx(RING_DP / 2)), 0f)

    /** The colour §4.4.1 fills a disc with — the slab itself where the look has no disc. */
    private fun fillColour(fill: DockFill): Color =
        when (fill) {
            DockFill.None -> scheme.surfaceContainerHigh
            DockFill.Pale -> scheme.primaryContainer
            DockFill.Strong -> scheme.primary
        }

    /** [MARK_PROBE_DP] beyond the rim along the top-end diagonal: on the mark's border, off the disc. */
    private fun markProbe(radiusPx: Float): Offset {
        val reach = radiusPx + dpToPx(MARK_PROBE_DP)
        val diagonal = reach / sqrt(2f)
        return Offset(diagonal, -diagonal)
    }

    /** On the slash's centre line, [SLASH_PROBE_UNITS] into the glyph from its top-start corner. */
    private fun slashProbe(): Offset {
        val fromCentre = dpToPx((SLASH_PROBE_UNITS - GLYPH_VIEWPORT_UNITS / 2) * GLYPH_DP / GLYPH_VIEWPORT_UNITS)
        return Offset(fromCentre, fromCentre)
    }

    private fun dpToPx(dp: Float): Float = with(composeRule.density) { dp.dp.toPx() }

    private fun lookOf(
        @StringRes control: Int,
        flags: DockFlags,
    ): DockLook {
        // Working renames Hint for its duration; it is the same control under a second name.
        val name = if (control == R.string.hint_action && flags.solverThinking) R.string.hint_thinking else control
        return checkNotNull(
            composeRule
                .onNode(hasContentDescription(string(name)) and hasLook())
                .fetchSemanticsNode()
                .config
                .getOrNull(DockLookKey),
        )
    }

    private fun hasLook() = SemanticsMatcher.keyIsDefined(DockLookKey)

    private fun string(
        @StringRes id: Int,
    ): String = RuntimeEnvironment.getApplication().getString(id)

    /** A pixel matches a theme colour when every channel is within [tolerance]. */
    private fun assertNear(
        what: String,
        actual: Int,
        expected: Color,
        tolerance: Int = CHANNEL_TOLERANCE,
    ) {
        val wanted = expected.toArgb()
        val distance =
            COLOUR_CHANNELS.maxOf { shift ->
                abs((actual shr shift and CHANNEL) - (wanted shr shift and CHANNEL))
            }
        assertWithMessage("$what: expected #%08X, read #%08X".format(wanted, actual))
            .that(distance)
            .isAtMost(tolerance)
    }
}

/** The three inputs the dock draws from. */
private data class DockFlags(
    val showHints: Boolean,
    val canUndo: Boolean,
    val solverThinking: Boolean,
)

/** Every state the dock can be put in: each of its three inputs on and off. */
private val EVERY_DOCK_STATE =
    listOf(true, false).flatMap { showHints ->
        listOf(true, false).flatMap { canUndo ->
            listOf(true, false).map { thinking -> DockFlags(showHints, canUndo, thinking) }
        }
    }

/** The four controls, by the name each keeps in its resting state. */
private val DOCK_CONTROLS =
    listOf(R.string.undo_action, R.string.hint_action, R.string.show_moves_on, R.string.restart_board)

/** The looks an action may take (§4.4.1 "Two kinds, two grammars"). */
private val ACTION_LOOKS = listOf(DockLook.Available, DockLook.Working, DockLook.Unavailable)

/**
 * One row of §4.4.1's table of the five looks, as the disc must be drawn: its fill, whether it is
 * ringed, and whether it is raised on a shadow.
 */
private data class SpecDisc(
    val fill: DockFill,
    val ring: Boolean,
    val raised: Boolean,
)

/** §4.4.1's "Disc fill", "Ring" and "Shadow" columns, typed in from the spec — never read from the recipe. */
private val SPEC_DISCS =
    mapOf(
        DockLook.Available to SpecDisc(fill = DockFill.Pale, ring = false, raised = true),
        DockLook.Working to SpecDisc(fill = DockFill.Pale, ring = false, raised = false),
        DockLook.Unavailable to SpecDisc(fill = DockFill.None, ring = false, raised = false),
        DockLook.On to SpecDisc(fill = DockFill.Strong, ring = false, raised = true),
        DockLook.Off to SpecDisc(fill = DockFill.None, ring = true, raised = false),
    )

/**
 * A control, found by the name it has in [flags], and the look §4.4.1's "which control is in which
 * look" rows put it in there.
 */
private data class DrawnRow(
    val flags: DockFlags,
    @param:StringRes val control: Int,
    val look: DockLook,
) {
    val expected: SpecDisc get() = SPEC_DISCS.getValue(look)
    val what: String get() = "${RuntimeEnvironment.getApplication().getString(control)}, $look"
}

/** Mid-game with the dots on: every action can be pressed, and the toggle is on. */
private val MID_GAME_DOTS_ON = DockFlags(showHints = true, canUndo = true, solverThinking = false)

/** A fresh board with the dots off while the Solver searches: nothing to undo, Hint at work, the toggle off. */
private val FRESH_BOARD_SEARCHING_DOTS_OFF = DockFlags(showHints = false, canUndo = false, solverThinking = true)

/**
 * Every control in every look it can reach, across the two states that between them reach all
 * five looks. Working renames Hint to `hint_thinking` for its duration.
 */
private val DRAWN_ROWS =
    listOf(
        DrawnRow(MID_GAME_DOTS_ON, R.string.undo_action, DockLook.Available),
        DrawnRow(MID_GAME_DOTS_ON, R.string.hint_action, DockLook.Available),
        DrawnRow(MID_GAME_DOTS_ON, R.string.show_moves_on, DockLook.On),
        DrawnRow(MID_GAME_DOTS_ON, R.string.restart_board, DockLook.Available),
        DrawnRow(FRESH_BOARD_SEARCHING_DOTS_OFF, R.string.undo_action, DockLook.Unavailable),
        DrawnRow(FRESH_BOARD_SEARCHING_DOTS_OFF, R.string.hint_thinking, DockLook.Working),
        DrawnRow(FRESH_BOARD_SEARCHING_DOTS_OFF, R.string.show_moves_on, DockLook.Off),
        DrawnRow(FRESH_BOARD_SEARCHING_DOTS_OFF, R.string.restart_board, DockLook.Available),
    )

/** How far beyond the disc's rim the on-mark probe reads: the middle of the mark's 10..12dp border. */
private const val MARK_PROBE_DP = 11f

/** §6.1: a 28dp glyph on a 24-unit viewport. */
private const val GLYPH_DP = 28f
private const val GLYPH_VIEWPORT_UNITS = 24f

/** A point on the slash's centre line, near its top-start end and clear of the three circles. */
private const val SLASH_PROBE_UNITS = 4.5f

/** Blue, green and red, by bit offset in an ARGB int. */
private val COLOUR_CHANNELS = listOf(0, 8, 16)
private const val CHANNEL = 0xFF

/** Per-channel slack for a solid region; wide enough for rounding, far below any colour change. */
private const val CHANNEL_TOLERANCE = 12

/**
 * Slack for a read that must tell colours a few levels apart — the pale disc from the slab, bare
 * slab from a shadow: a flat region renders exact, so one level covers rounding and nothing more.
 */
private const val EXACT_TOLERANCE = 1

/** §4.4.1: the ring is `game_action_ring_width`, drawn inside the disc's rim. */
private const val RING_DP = 2f

/** `elevation_pressed`: a disc raised on it casts its shadow within this distance of its rim. */
private const val SHADOW_BAND_DP = 2f

/**
 * The least darkening that counts as a raised disc's shadow. The composited render darkens the band
 * under a disc on `elevation_pressed` by ≈18 levels summed; a third of that leaves room for a
 * renderer's rounding and still sits far above the zero a flat disc leaves.
 */
private const val RAISED_MIN_DARKENING = 6

/** A window wide enough that the four discs sit apart, at a density fine enough to read a 2dp stroke. */
private const val MARK_WINDOW_QUALIFIER = "w400dp-h200dp-xxhdpi"

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val MARK_ROBOLECTRIC_SDK = 34
