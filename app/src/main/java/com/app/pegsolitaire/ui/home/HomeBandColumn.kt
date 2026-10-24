package com.app.pegsolitaire.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.theme.ClayAnim
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Home's band column (ux-spec §4.2.2, §4.2.5 D-03n): the hero row, the stage, the progress ribbon,
 * up to two Continue chips and the two mode tiles, top to bottom, with the footer's slack below
 * them — one measure policy that gives every band its own height and then splits what is left.
 *
 * ## Why a measure policy, and not two weights
 *
 * D-03i split the slack 3 : 1 between the stage and the footer with two `Column` weights. A weight
 * is a share of the slack the column has **now**, so every chip that arrived after first paint
 * took its room from both shares, and the mode tiles under it moved 22–24dp, beneath a thumb that
 * was already heading for them (QA's loop-3 F-1). D-03n fixes the footer to a share of the slack
 * the column has with **no** chip, S₀, and takes the chips' room from the stage alone — which no
 * pair of weights can say, because S₀ is not the slack the column has once a chip is in.
 *
 * ## The quantities, in px, all from this measure pass
 *
 * - Each band is measured at the column's width and its own height: the hero, the ribbon, both
 *   tiles, and each chip present.
 * - **S₀** = max(0, inner − hero − ribbon − both tiles − the five band gaps the column has with no
 *   chip), where `inner` is [availableHeightPx] less the column's padding above and below. It
 *   depends on the window, the font scale and those bands, never on the chips.
 * - **The footer with no chip** is [FOOTER_SHARE_OF_SLACK] of S₀, and **the stage with no chip**
 *   the rest of S₀, so the two add up to S₀ exactly, to the pixel.
 * - **R**, the chips' room: for each chip present, its height plus the band gap it brings, times
 *   how far its room is open ([ChipRoom]) — 1 when it has settled.
 * - **The stage** = max(0, stage with no chip − R); **the footer** = min(footer with no chip,
 *   max(0, S₀ − R)).
 *
 * So the column is exactly `inner` tall while R ≤ S₀, and overflows by R − S₀ once it is not, and
 * the scroll above takes the rest (criteria 14, 15i, 15k). Tile 1's top edge sits at the hero, the
 * ribbon, three gaps and the no-chip stage below the top **whatever R is**, as long as R is no more
 * than the no-chip stage: the stage gives up exactly what the chips take, so the ribbon rides up on
 * its bottom edge and the tiles hold still (criterion 15s). Past that the tiles move by the
 * shortfall and no more (15t), because the stage is 0 and the footer pays the rest, down to 0.
 *
 * ## When a chip arrives, or leaves, while Home is drawn
 *
 * Each chip's room opens from 0 to its full height over `ClayAnim.ENTRANCE_MS`, with
 * `ClayAnim.ENTRANCE_EASING` and no overshoot, and closes the same way when its save goes
 * (§4.2.5, "The motion"). The room includes the chip's band gap, so no gap shows a frame ahead of
 * its chip, and the chip is drawn clipped to its share of the room, so the room grows from the
 * ribbon's edge and nothing overlaps while it opens. In each frame the stage gives up exactly what
 * the room takes, so the tiles hold still in every frame, not only at the end. With OS animations
 * off the room is open, or shut, in the frame the chip arrives or leaves.
 *
 * Only this pass knows where a motion's two ends leave the stage, since that depends on how the
 * bands wrap, so it decides whether the stage art crosses its floor ([StageArtFade]) and hands the
 * verdict to [onStageArtFade] on every pass; the caller keeps it in state and writes it only when it
 * changes, so a steady Home writes nothing.
 *
 * ## What it does not change
 *
 * The band order, the bands themselves and their entrance cascade (which moves a band by a
 * drawing offset, never by layout) are untouched; the stage is still the one child that holds a
 * drawing in space no band claims (§4.2.2 clause 4), no content band ever takes slack, and none
 * sits between the ribbon and the chips (clause 6). The column reports its own height, so nothing
 * in the modifier chain around it sets one (`HomeBands`).
 *
 * @param availableHeightPx the height `HomeBands`' `BoxWithConstraints` was given, in px — the
 *   viewport the column scrolls in, after the `Scaffold` has taken the system bars. Inside the
 *   scroll the column's own maximum height is infinite, so it is handed the viewport's.
 * @param bandSpacing the band set's spacing: between two bands, and the column's padding.
 * @param onStageArtFade receives the stage art's verdict for the motion under way, on every pass.
 * @param modifier the column's modifier chain, its padding included.
 * @param hero band 1, the hero row.
 * @param stage the stage, measured at exactly the height this policy gives it.
 * @param ribbon band 2, the progress ribbon.
 * @param chips band 3: the Continue chips, top to bottom — none, one or two — each in its own
 *   [ChipRoomSlot], which tells this policy how far its room is open.
 * @param tiles band 4: the two mode tiles, top to bottom.
 */
@Composable
internal fun HomeBandColumn(
    availableHeightPx: Int,
    bandSpacing: Dp,
    onStageArtFade: (StageArtFade) -> Unit,
    modifier: Modifier,
    hero: @Composable () -> Unit,
    stage: @Composable () -> Unit,
    ribbon: @Composable () -> Unit,
    chips: @Composable () -> Unit,
    tiles: @Composable () -> Unit,
) {
    val artFloor = dimensionResource(R.dimen.home_stage_art_min_size)
    Layout(
        contents = listOf(hero, stage, ribbon, chips, tiles),
        modifier = modifier,
    ) { bands, constraints ->
        val gap = bandSpacing.roundToPx()
        val width = constraints.maxWidth
        val atColumnWidth = Constraints(maxWidth = width)
        val heroPlaceable = bands[HERO_SLOT].single().measure(atColumnWidth)
        val ribbonPlaceable = bands[RIBBON_SLOT].single().measure(atColumnWidth)
        val chipMeasurables = bands[CHIPS_SLOT]
        val chipPlaceables = chipMeasurables.map { it.measure(atColumnWidth) }
        val (firstTile, secondTile) = bands[TILES_SLOT].map { it.measure(atColumnWidth) }

        val inner = availableHeightPx - 2 * gap
        val fixedBands =
            heroPlaceable.height + ribbonPlaceable.height + firstTile.height + secondTile.height +
                FIXED_BAND_GAPS * gap
        val slack = max(0, inner - fixedBands)
        val footerWithNoChip = (slack * FOOTER_SHARE_OF_SLACK).roundToInt()
        val stageWithNoChip = slack - footerWithNoChip
        val chipRooms =
            chipPlaceables.mapIndexed { index, chip ->
                val open = (chipMeasurables[index].layoutId as? ChipRoom)?.fraction ?: ROOM_OPEN
                ((chip.height + gap) * open).roundToInt()
            }
        val room = chipRooms.sum()
        val stageHeight = max(0, stageWithNoChip - room)
        val footer = min(footerWithNoChip, max(0, slack - room))
        val stagePlaceable = bands[STAGE_SLOT].single().measure(Constraints.fixed(width, stageHeight))
        onStageArtFade(
            stageArtFade(
                stageWithNoChip = stageWithNoChip,
                fullRooms = chipPlaceables.map { it.height + gap },
                rooms = chipMeasurables.map { it.layoutId as? ChipRoom },
                width = width,
                artFloor = artFloor.roundToPx(),
            ),
        )

        layout(width, fixedBands + stageHeight + room + footer) {
            var y = 0
            heroPlaceable.place(0, y)
            y += heroPlaceable.height + gap
            stagePlaceable.place(0, y)
            y += stageHeight + gap
            ribbonPlaceable.place(0, y)
            y += ribbonPlaceable.height + gap
            chipPlaceables.forEachIndexed { index, chip ->
                chip.place(0, y)
                y += chipRooms[index]
            }
            firstTile.place(0, y)
            y += firstTile.height + gap
            secondTile.place(0, y)
        }
    }
}

/**
 * One Continue chip's room in the band column (§4.2.5, "The motion"): whether its chip is wanted —
 * its save exists — and how far the room is open, from 0 to 1.
 *
 * [fraction] is read in layout and in drawing, never in composition, so the room's motion
 * re-measures the column and redraws the chip each frame without recomposing either; composition
 * reads only [isOpen], which changes twice a transition at most.
 */
@Stable
internal class ChipRoom(
    /** Whether this room's chip is wanted: its save exists, so its room is opening or open. */
    val present: Boolean,
    private val openness: () -> Float,
    private val opened: State<Boolean>,
) {
    /** How far the room is open: 0 shut, 1 open. */
    val fraction: Float get() = openness()

    /** Whether any of the room is open, so its chip must still be drawn. */
    val isOpen: Boolean get() = opened.value

    /** Whether the room is still opening or closing. */
    val moving: Boolean get() = fraction != target

    /** How far the room has come toward where it is going, from 0 to 1; 1 once it is there. */
    val progress: Float
        get() {
            val open = fraction
            return when {
                open == target -> ROOM_OPEN
                present -> open
                else -> ROOM_OPEN - open
            }
        }

    private val target: Float get() = if (present) ROOM_OPEN else ROOM_SHUT
}

/**
 * How far the motion under way has come, from 0 to 1, for the stage art's fade: the less advanced of
 * the two rooms, so a room that is not moving counts as done. Two rooms that move together — a cloud
 * swap that replaces both saves — start in one frame and run one curve, so they agree.
 */
internal fun stageMotionProgress(
    classic: ChipRoom,
    challenge: ChipRoom,
): Float = min(classic.progress, challenge.progress)

/**
 * The stage art's verdict for the motion under way (§4.2.5): where the motion's two ends — every room
 * at where it started, then at where it is going — leave the stage against the art's floor.
 *
 * @param fullRooms each chip's room at full opening, its height plus its band gap, in px.
 * @param rooms each chip's room, in the same order.
 * @param width the column's width: the art's edge is the stage's height or this, the smaller.
 * @param artFloor the smallest edge the art is drawn at, `home_stage_art_min_size`, in px.
 */
private fun stageArtFade(
    stageWithNoChip: Int,
    fullRooms: List<Int>,
    rooms: List<ChipRoom?>,
    width: Int,
    artFloor: Int,
): StageArtFade {
    if (rooms.none { it?.moving == true }) return StageArtFade.None
    var fromRoom = 0
    var toRoom = 0
    rooms.forEachIndexed { index, room ->
        val present = room?.present ?: true
        // A moving room started where it is not going; a settled one is where it always was.
        val startedOpen = if (room?.moving == true) !present else present
        if (present) toRoom += fullRooms[index]
        if (startedOpen) fromRoom += fullRooms[index]
    }
    val drawnAtStart = min(stageWithNoChip - fromRoom, width) >= artFloor
    val drawnAtEnd = min(stageWithNoChip - toRoom, width) >= artFloor
    return when {
        drawnAtStart && !drawnAtEnd -> StageArtFade.Out
        !drawnAtStart && drawnAtEnd -> StageArtFade.In
        else -> StageArtFade.None
    }
}

/**
 * The room of one Continue chip slot: open from Home's first composition if [present] then, and
 * from then on opening and closing as the chip arrives and leaves, over `ClayAnim.ENTRANCE_MS` with
 * `ClayAnim.ENTRANCE_EASING` and no overshoot.
 *
 * With OS animations off ([animationsEnabled] false) the room is its target at once, read in the
 * composition that carries the change, so the very next frame is the settled layout (§10's
 * reduced-motion bullet) — not a frame later, as a snap from an effect would be.
 */
@Composable
internal fun rememberChipRoom(
    present: Boolean,
    animationsEnabled: Boolean,
): ChipRoom {
    val target = if (present) ROOM_OPEN else ROOM_SHUT
    val openness = remember { Animatable(target) }
    LaunchedEffect(target, animationsEnabled) {
        if (!animationsEnabled) {
            openness.snapTo(target)
        } else if (openness.value != target) {
            openness.animateTo(target, tween(ClayAnim.ENTRANCE_MS, easing = ClayAnim.ENTRANCE_EASING))
        }
    }
    return remember(present, animationsEnabled) {
        val fraction: () -> Float = if (animationsEnabled) ({ openness.value }) else ({ target })
        ChipRoom(present = present, openness = fraction, opened = derivedStateOf { fraction() > ROOM_SHUT })
    }
}

/**
 * One Continue chip in its room: [content] for [value] — what the chip resumes — while the save
 * exists, and while its room is still closing after it has gone, showing the chip it was.
 *
 * The chip is measured at its own height, and drawn clipped to the share of it its room has opened,
 * from its top, so it is revealed downward from the ribbon's edge and hidden upward toward it.
 * Settled, nothing is clipped, so the chip's clay shadow is whole.
 *
 * @param value what the chip resumes, or `null` when its mode has no save.
 * @param room this chip's room, which the band column reads from the slot.
 */
@Composable
internal fun <T : Any> ChipRoomSlot(
    value: T?,
    room: ChipRoom,
    content: @Composable (T) -> Unit,
) {
    // The last chip this slot showed: a chip whose save has gone still shows itself while its
    // room closes, which is the arrival run in reverse (§4.2.5).
    val latched = remember { mutableStateOf<T?>(null) }
    SideEffect { if (value != null) latched.value = value }
    val shown = value ?: latched.value
    if (shown != null && (value != null || room.isOpen)) {
        Box(modifier = Modifier.layoutId(room).clipToRoom(room)) { content(shown) }
    }
}

/** Draws only the part of the chip its room has opened, from the top; nothing is clipped once open. */
private fun Modifier.clipToRoom(room: ChipRoom): Modifier =
    drawWithContent {
        val open = room.fraction
        if (open >= ROOM_OPEN) {
            drawContent()
        } else {
            clipRect(bottom = size.height * open) { this@drawWithContent.drawContent() }
        }
    }

/** A chip's room fully open, and shut. */
private const val ROOM_OPEN = 1f
private const val ROOM_SHUT = 0f

/**
 * The footer's share of the no-chip slack S₀ (ux-spec §4.2.5, D-03n): one fifth, fixed for the
 * window, so the stage holds the other four fifths less the chips' room. §4.2.5 gives the three
 * reasons it is a fifth and not D-03i's quarter: it pins the mode tiles on every phone window
 * measured, a quarter failing by 5dp at 360x640dp with both chips; it keeps the founder's one-chip
 * Home where they approved it, the board 3.6dp smaller rather than 24dp; and it keeps the one-chip
 * stage a board down to the full set's 720dp threshold. A unitless ratio, so a scoped constant
 * rather than a `dimens.xml` token (§6.4); it replaces D-03i's two weights, which are not to be
 * rebuilt (§4.2.4).
 */
private const val FOOTER_SHARE_OF_SLACK = 0.2f

/**
 * The band gaps of the column with no chip: hero, stage, ribbon, two tiles and the footer are six
 * children, and the stage and the footer bring one gap each at any height, even 0. Each chip
 * present brings one more, which is counted in its room.
 */
private const val FIXED_BAND_GAPS = 5

/** Where each band's content sits in the column's slots, top to bottom. */
private const val HERO_SLOT = 0
private const val STAGE_SLOT = 1
private const val RIBBON_SLOT = 2
private const val CHIPS_SLOT = 3
private const val TILES_SLOT = 4
