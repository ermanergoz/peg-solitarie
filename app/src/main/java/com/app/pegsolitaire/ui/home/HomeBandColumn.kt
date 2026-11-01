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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import com.app.pegsolitaire.ui.theme.ClayAnim
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Home's band column (ux-spec §4.2.2, §4.2.5 D-03n, §4.2.7 D-03p and D-03q, §3 D-03r): the hero row,
 * the progress ribbon, up to two Continue chips and the two mode tiles, top to bottom.
 *
 * ## D-03r (founder 2026-11-02, Q-046): only the title moves up
 *
 * The hero row (title and gear) is pinned at the top of the column, right under its top padding. The
 * ribbon, the Continue chips and the mode tiles stay exactly where D-03p/D-03q put them, low in the
 * thumb zone: the space D-03p put **above** the hero row — its "headroom" — now lies **between** the
 * hero row and the ribbon, as the [HomeBandPlacement.middle]. The arithmetic is unchanged, so the
 * lower group's position in every chip state is today's: the no-chip middle is min(⅘ S₀, C), C the
 * former title bound (D-03q), which now only fixes where the lower group sits (its title-contrast
 * reason is gone, the title being back in the calm band at the top). Supersedes D-03p's headroom
 * above the hero and D-03q's limit on the hero row; the founder's Q-033 option 2.
 *
 * ## Why a measure policy, and not weights
 *
 * A weight is a share of the slack the column has **now**, so every chip that arrived after first
 * paint would take its room from both shares and move the mode tiles under a thumb already heading
 * for them (QA's loop-3 F-1). D-03n fixes the footer to a share of the slack the column has with
 * **no** chip, S₀, and takes the chips' room from the middle alone — which no pair of weights can say.
 * The middle gives up exactly what the chips take, so the ribbon rides up toward the hero row and the
 * tiles hold still; past that the tiles fall by the shortfall and no more, the footer paying the
 * rest, down to 0. The hero row never moves. The middle is space with nothing in it: no child, no
 * node, no semantics.
 *
 * ## When a chip arrives, or leaves, while Home is drawn
 *
 * Each chip's room opens from 0 to its full height over `ClayAnim.ENTRANCE_MS`, with
 * `ClayAnim.ENTRANCE_EASING` and no overshoot, and closes the same way when its save goes
 * (§4.2.5, "The motion"). The room includes the chip's band gap, so no gap shows a frame ahead of
 * its chip, and the chip is drawn clipped to its share of the room, so the room grows from the
 * ribbon's edge and nothing overlaps while it opens. In each frame the middle gives up exactly what
 * the room takes, so the tiles hold still in every frame, not only at the end, and the ribbon only
 * ever rides **up** from its no-chip place, and the hero row never moves. With OS animations off
 * the room is open, or shut, in the frame the chip arrives or leaves.
 *
 * ## What it does not change
 *
 * The band order, the bands themselves and their entrance cascade (which moves a band by a drawing
 * offset, never by layout) are untouched; no content band ever takes slack (§4.2.2 clause 4), and
 * nothing sits between the hero row and the ribbon, the ribbon and the chips, or the chips and the
 * tiles (clause 6, 15y part 3). The column reports its own height, so nothing in the modifier chain
 * around it sets one (`HomeBands`).
 *
 * @param availableHeightPx the height `HomeBands`' `BoxWithConstraints` was given, in px — the
 *   viewport the column scrolls in, after the `Scaffold` has taken the system bars. Inside the
 *   scroll the column's own maximum height is infinite, so it is handed the viewport's.
 * @param titleBoundPx the line the hero row's bottom edge may not pass, in px from the viewport's top
 *   edge (D-03q; `HomeBands` translates it from the window's frame, where `TITLE_REACH` is stated).
 * @param bandSpacing the band set's spacing: between two bands, and the column's padding.
 * @param modifier the column's modifier chain, its padding included.
 * @param hero band 1, the hero row.
 * @param ribbon band 2, the progress ribbon.
 * @param chips band 3: the Continue chips, top to bottom — none, one or two — each in its own
 *   [ChipRoomSlot], which tells this policy how far its room is open.
 * @param tiles band 4: the two mode tiles, top to bottom.
 */
@Composable
internal fun HomeBandColumn(
    availableHeightPx: Int,
    titleBoundPx: Int,
    bandSpacing: Dp,
    modifier: Modifier,
    hero: @Composable () -> Unit,
    ribbon: @Composable () -> Unit,
    chips: @Composable () -> Unit,
    tiles: @Composable () -> Unit,
) {
    Layout(
        contents = listOf(hero, ribbon, chips, tiles),
        modifier = modifier,
    ) { bands, constraints ->
        val gap = bandSpacing.roundToPx()
        val width = constraints.maxWidth
        val atColumnWidth = Constraints(maxWidth = width)
        val heroPlaceable = bands[HERO_SLOT].single().measure(atColumnWidth)
        val ribbonPlaceable = bands[RIBBON_SLOT].single().measure(atColumnWidth)
        val chipMeasurables = bands[CHIPS_SLOT]
        val chipPlaceables = chipMeasurables.map { it.measure(atColumnWidth) }
        val tilePlaceables = bands[TILES_SLOT].map { it.measure(atColumnWidth) }
        val chipRooms =
            chipPlaceables.mapIndexed { index, chip ->
                val open = (chipMeasurables[index].layoutId as? ChipRoom)?.fraction ?: ROOM_OPEN
                ((chip.height + gap) * open).roundToInt()
            }
        val placement =
            homeBandPlacement(
                inner = availableHeightPx - 2 * gap,
                gap = gap,
                hero = heroPlaceable.height,
                ribbon = ribbonPlaceable.height,
                tiles = tilePlaceables.map { it.height },
                chipRooms = chipRooms,
                titleBound = titleBoundPx,
            )

        layout(width, placement.height) {
            // D-03r: the hero row is pinned at the top; the middle lies between it and the ribbon.
            heroPlaceable.place(0, 0)
            var y = heroPlaceable.height + gap + placement.middle
            ribbonPlaceable.place(0, y)
            y += ribbonPlaceable.height + gap
            chipPlaceables.forEachIndexed { index, chip ->
                chip.place(0, y)
                y += chipRooms[index]
            }
            tilePlaceables.forEach { tile ->
                tile.place(0, y)
                y += tile.height + gap
            }
        }
    }
}

/**
 * One Continue chip's room in the band column (§4.2.5, "The motion"): how far it is open, from 0 to 1.
 *
 * [fraction] is read in layout and in drawing, never in composition, so the room's motion
 * re-measures the column and redraws the chip each frame without recomposing either; composition
 * reads only [isOpen], which changes twice a transition at most.
 */
@Stable
internal class ChipRoom(
    private val openness: () -> Float,
    private val opened: State<Boolean>,
) {
    /** How far the room is open: 0 shut, 1 open. */
    val fraction: Float get() = openness()

    /** Whether any of the room is open, so its chip must still be drawn. */
    val isOpen: Boolean get() = opened.value
}

/**
 * The room of one Continue chip slot: open from Home's first composition if [present] then, and
 * from then on opening and closing as the chip arrives and leaves, over `ClayAnim.ENTRANCE_MS` with
 * `ClayAnim.ENTRANCE_EASING` and no overshoot.
 *
 * With OS animations off ([animationsEnabled] false) the room is its target at once, read in the
 * composition that carries the change, so the very next frame is the settled layout (§10's
 * reduced-motion bullet) — not a frame later, as a snap from an effect would be.
 *
 * **The effect is keyed on the target alone**, never on the gate (tech design §16 #28; §14.3 item 1,
 * `home-chip-arrival/tech-lead-c11-ruling.md` §3): it runs the tween whenever the room is not where its
 * chip wants it, and the tween takes the composition's own animator scale, so with animations off it
 * ends in its first frame. A switch of Remove animations therefore restarts nothing and strands
 * nothing: switched off mid-opening, the room reads its target in that composition and the tween ends
 * on its next frame; switched back on, the room is already at its target and nothing moves.
 */
@Composable
internal fun rememberChipRoom(
    present: Boolean,
    animationsEnabled: Boolean,
): ChipRoom {
    val target = if (present) ROOM_OPEN else ROOM_SHUT
    val openness = remember { Animatable(target) }
    LaunchedEffect(target) {
        if (openness.value != target) {
            openness.animateTo(target, tween(ClayAnim.ENTRANCE_MS, easing = ClayAnim.ENTRANCE_EASING))
        }
    }
    return remember(present, animationsEnabled) {
        val fraction: () -> Float = if (animationsEnabled) ({ openness.value }) else ({ target })
        ChipRoom(openness = fraction, opened = derivedStateOf { fraction() > ROOM_SHUT })
    }
}

/** Semantics tag of a Continue chip's room in the band column: the only semantics it has once its chip is inert. */
const val CONTINUE_CHIP_ROOM_TEST_TAG = "home_continue_chip_room"

/**
 * One Continue chip in its room: [content] for [value] — what the chip resumes — while the save
 * exists, and while its room is still closing after it has gone, showing the chip it was.
 *
 * **A chip whose save has gone is drawn and nothing else** (code-auditor loop 1, finding 1). For the
 * ≤ `ClayAnim.ENTRANCE_MS` its room takes to close it carries no semantics — no click action, no
 * text, no TalkBack stop — and takes no touch, so neither a tap nor a TalkBack click can resume a
 * save that no longer exists: the phantom Continue WR-02 and CR-01 removed, which in Challenge would
 * play the replaced ladder's level and merge it into the progress the player just chose.
 *
 * The chip is measured at its own height, and drawn clipped to the share of it its room has opened,
 * from its top, so it is revealed downward from the ribbon's edge and hidden upward toward it. Only
 * the bottom edge is clipped, so the chip's clay shadow stays whole at its sides and top while the
 * room moves; settled, nothing is clipped.
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
        Box(
            modifier =
                Modifier
                    .layoutId(room)
                    .testTag(CONTINUE_CHIP_ROOM_TEST_TAG)
                    .then(if (value == null) Modifier.inert() else Modifier)
                    .clipToRoom(room),
        ) { content(shown) }
    }
}

/**
 * Nothing inside answers: its semantics are cleared, so no action, text or stop reaches an
 * accessibility service, and every pointer event is consumed before the content sees it.
 */
private fun Modifier.inert(): Modifier =
    clearAndSetSemantics {}.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
            }
        }
    }

/**
 * Draws only the part of the chip its room has opened, from the top, cutting the bottom edge alone:
 * the clip reaches a chip's size past its sides and top, far beyond its clay shadow, so the shadow is
 * never cut there. Nothing is clipped once open.
 */
private fun Modifier.clipToRoom(room: ChipRoom): Modifier =
    drawWithContent {
        val open = room.fraction
        if (open >= ROOM_OPEN) {
            drawContent()
        } else {
            clipRect(
                left = -size.width,
                top = -size.height,
                right = size.width * 2,
                bottom = size.height * open,
            ) { this@drawWithContent.drawContent() }
        }
    }

/**
 * Where Home's band column puts its slack (ux-spec §4.2.7, D-03p, D-03q; §3 D-03r): [middle] between
 * the hero row and the ribbon (D-03p's headroom, moved below the title by D-03r), [footer] below the
 * second tile, and the column's [height], padding excluded, all in px. Below, "headroom" is [middle].
 */
internal data class HomeBandPlacement(
    val middle: Int,
    val footer: Int,
    val height: Int,
)

/**
 * The band column's arithmetic, in px and with no Compose type, so §4.2.7's tables are checked without
 * composing text (`HomeBandPlacementTest`; the tech-lead's brief, Amendment 1 A1.1). The `Layout` in
 * [HomeBandColumn] measures the bands, calls this, and places them.
 *
 * - **S₀**, the no-chip slack: [inner] less the bands and the **four** band gaps the column has with
 *   no chip — hero→ribbon, ribbon→tile 1, tile 1→tile 2 and the footer's — floored at 0. The headroom
 *   brings no gap of its own (D-03p).
 * - **C**, the headroom at which the hero row's bottom edge meets the title bound:
 *   max(0, [titleBound] − [gap] − [hero]), [titleBound] being the bound in the viewport's frame and
 *   [gap] the column's top padding (D-03q).
 * - **H₀**, the no-chip headroom: min(⅘ S₀, C). ⅘ S₀ is S₀ less its rounded fifth, the footer's share
 *   (D-03n), and the bound can only lower it.
 * - **The headroom** with the chips' rooms R: max(0, H₀ − R) — the room comes out of the headroom, so
 *   the hero row and the ribbon ride up on it and the tiles hold still while R ≤ H₀.
 * - **The footer**: min(S₀ − H₀, max(0, S₀ − R)) — fixed for the window while the headroom can give the
 *   room, a fifth of S₀ plus whatever the bound would not let the headroom hold; past that it pays the
 *   rest, down to 0, and the tiles fall by exactly R − H₀.
 *
 * So the column is exactly [inner] tall while R ≤ S₀, and overflows by R − S₀ past it; the bound's
 * excess lies below the tiles and never between two elements (15y part 3).
 *
 * @param inner the viewport's height less the column's padding above and below.
 * @param gap the band gap, which is also the column's padding.
 * @param tiles both mode tiles' heights.
 * @param chipRooms each Continue chip's room as far as it is open: its height plus its band gap.
 * @param titleBound the line the hero row's bottom edge may not pass, from the viewport's top edge.
 */
internal fun homeBandPlacement(
    inner: Int,
    gap: Int,
    hero: Int,
    ribbon: Int,
    tiles: List<Int>,
    chipRooms: List<Int>,
    titleBound: Int,
): HomeBandPlacement {
    val fixedBands = hero + ribbon + tiles.sum() + HEADROOM_COLUMN_BAND_GAPS * gap
    val slack = max(0, inner - fixedBands)
    val footerWithNoChip = (slack * FOOTER_SHARE_OF_SLACK).roundToInt()
    val boundHeadroom = max(0, titleBound - gap - hero)
    val headroomWithNoChip = min(slack - footerWithNoChip, boundHeadroom)
    val room = chipRooms.sum()
    val headroom = max(0, headroomWithNoChip - room)
    val footer = min(slack - headroomWithNoChip, max(0, slack - room))
    return HomeBandPlacement(middle = headroom, footer = footer, height = fixedBands + headroom + room + footer)
}

/** The band gaps of the column with no chip: hero→ribbon, ribbon→tile 1, tile 1→tile 2, and the footer's. */
private const val HEADROOM_COLUMN_BAND_GAPS = 4

/** A chip's room fully open, and shut. */
private const val ROOM_OPEN = 1f
private const val ROOM_SHUT = 0f

/**
 * The footer's share of the no-chip slack S₀ (ux-spec §4.2.5, D-03n; kept by D-03p): one fifth, fixed
 * for the window, so the headroom holds at most the other four fifths, less the chips' room. §4.2.5
 * gives the reasons it is a fifth and not D-03i's quarter: it pins the mode tiles on every phone window
 * measured, a quarter failing by 5dp at 360x640dp with both chips, and it keeps the founder's one-chip
 * Home where they approved it. The title bound (D-03q) can only add to the footer. A unitless ratio, so a
 * scoped constant rather than a `dimens.xml` token (§6.4); it replaces D-03i's two weights, which are
 * not to be rebuilt (§4.2.4).
 */
private const val FOOTER_SHARE_OF_SLACK = 0.2f

/** Where each band's content sits in the column's slots, top to bottom. */
private const val HERO_SLOT = 0
private const val RIBBON_SLOT = 1
private const val CHIPS_SLOT = 2
private const val TILES_SLOT = 3
