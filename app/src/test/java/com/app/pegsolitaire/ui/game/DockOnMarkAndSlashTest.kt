package com.app.pegsolitaire.ui.game

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.StringRes
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.app.pegsolitaire.R
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
import kotlin.math.sqrt

/**
 * Criterion 21e — **only the toggle is ever on or off** (ux-spec §4.4.1, §5, D-04e).
 *
 * Show-moves On draws the on mark and the plain [ShowMovesIcon]; Off draws [ShowMovesOffIcon] and
 * no mark; no action, in any look, draws the mark, the slashed glyph, a `primary` fill or a ring.
 *
 * Two halves, because "draws" has two halves. Which looks a control can **reach** is a semantics
 * fact, asserted over every state the dock can be put in. What a look **draws** is a pixel fact:
 * the on mark is drawn rather than composed (it is not a node, §4.4.1), so no semantics assertion
 * can see it, and this class reads it off the rendered dock instead.
 *
 * ## Why NATIVE graphics, and why not `captureToImage`
 *
 * Robolectric's default LEGACY graphics mode stubs every `Canvas` call, so a LEGACY render is blank
 * and a pixel assertion against it can only lie. NATIVE runs the real Skia pipeline. The Compose
 * host view is drawn into a bitmap directly rather than through `captureToImage()`, whose
 * `PixelCopy` path waits on a redraw Robolectric's paused looper never delivers.
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

    /** The rendered window, drawn by Skia into a bitmap (see the class KDoc for why not PixelCopy). */
    private fun renderedWindow(): Bitmap {
        val view = (composeRule.onRoot().fetchSemanticsNode().root as ViewRootForTest).view
        return Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888).also { view.draw(Canvas(it)) }
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
        val bounds =
            composeRule
                .onNode(hasContentDescription(string(control)) and hasLook())
                .fetchSemanticsNode()
                .boundsInRoot
        val offset = probe(bounds.width / 2)
        return renderedWindow().getPixel((bounds.center.x + offset.x).toInt(), (bounds.center.y + offset.y).toInt())
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

    /** A pixel matches a theme colour when every channel is within [CHANNEL_TOLERANCE]. */
    private fun assertNear(
        what: String,
        actual: Int,
        expected: Color,
    ) {
        val wanted = expected.toArgb()
        val distance =
            COLOUR_CHANNELS.maxOf { shift ->
                abs((actual shr shift and CHANNEL) - (wanted shr shift and CHANNEL))
            }
        assertWithMessage("$what: expected #%08X, read #%08X".format(wanted, actual))
            .that(distance)
            .isAtMost(CHANNEL_TOLERANCE)
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

/** A window wide enough that the four discs sit apart, at a density fine enough to read a 2dp stroke. */
private const val MARK_WINDOW_QUALIFIER = "w400dp-h200dp-xxhdpi"

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val MARK_ROBOLECTRIC_SDK = 34
