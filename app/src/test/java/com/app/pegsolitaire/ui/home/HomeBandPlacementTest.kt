package com.app.pegsolitaire.ui.home

import com.app.pegsolitaire.ui.backdrop.BackdropMetrics
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.math.roundToInt

/**
 * Home's band column arithmetic, with no composition (ux-spec §4.2.7, D-03p, and its title bound,
 * D-03q; the tech-lead's brief, Amendment 1 A1.1–A1.2): how the no-chip slack S₀ splits into the
 * headroom above the hero row and the footer below the tiles, how the chips' room comes out of the
 * headroom, and where the title bound stops the hero row.
 *
 * The windows are §4.2.7's own, read at **10 px per dp**, so every figure the spec's tables give to a
 * tenth of a dp is a whole number of px here and the table is checked to the px: the Pixel_9 (923dp
 * tall, a 54dp status bar and a 24dp gesture bar) and QA's 360×640dp window (52dp and 24dp), where
 * the bound bites, and the design baseline's 360×640dp with a 24dp status bar and a 48dp three-button
 * bar, where it does not. The band heights are the ones those tables were computed from: the 48dp hero
 * row, the 85.4dp full ribbon (40dp compact), the two tiles, and a chip's room of its 80dp height plus
 * its band gap.
 *
 * The bound is handed to [homeBandPlacement] as `HomeBands` hands it, in the viewport's frame: the
 * backdrop's `TITLE_REACH` of the window's height, less `entrance_slide_distance`, less the status
 * inset.
 */
class HomeBandPlacementTest {
    // ---- The Pixel_9: the bound bites (C 299.8dp against ⅘ S₀ 339.1dp) -------------

    @Test
    fun onThePixel9TheBoundHoldsTheNoChipHeadroomAtC() =
        assertPlacement(PIXEL_9, rooms = emptyList(), headroomDp = 299.8f, footerDp = 124.1f, firstTileTopDp = 535.2f)

    @Test
    fun onThePixel9TheFoundersOneChipTakesItsRoomFromTheHeadroom() =
        assertPlacement(
            PIXEL_9,
            rooms = listOf(FULL_CHIP_ROOM),
            headroomDp = 203.8f,
            footerDp = 124.1f,
            firstTileTopDp = 535.2f,
        )

    @Test
    fun onThePixel9BothChipsTakeTheirRoomFromTheHeadroom() =
        assertPlacement(
            PIXEL_9,
            rooms = listOf(FULL_CHIP_ROOM, FULL_CHIP_ROOM),
            headroomDp = 107.8f,
            footerDp = 124.1f,
            firstTileTopDp = 535.2f,
        )

    // ---- QA's 360×640dp window: the bound bites (C 176.8dp against ⅘ S₀ 188.8dp) --------

    @Test
    fun onQas360x640dpWindowTheBoundHoldsTheNoChipHeadroomAtC() =
        assertPlacement(QA_NARROW, rooms = emptyList(), headroomDp = 176.8f, footerDp = 59.2f, firstTileTopDp = 340.8f)

    @Test
    fun onQas360x640dpWindowOneChipTakesItsRoomFromTheHeadroom() =
        assertPlacement(
            QA_NARROW,
            rooms = listOf(COMPACT_CHIP_ROOM),
            headroomDp = 88.8f,
            footerDp = 59.2f,
            firstTileTopDp = 340.8f,
        )

    @Test
    fun onQas360x640dpWindowBothChipsTakeTheirRoomFromTheHeadroom() =
        assertPlacement(
            QA_NARROW,
            rooms = listOf(COMPACT_CHIP_ROOM, COMPACT_CHIP_ROOM),
            headroomDp = 0.8f,
            footerDp = 59.2f,
            firstTileTopDp = 340.8f,
        )

    // ---- The design baseline: the bound does not bite, D-03p's split stands ------------

    /**
     * 360×640dp with a 24dp status bar and a 48dp three-button bar: S₀ is 240dp and C 204.8dp is above
     * ⅘ S₀, so H₀ is ⅘ S₀ = 192dp and the footer a fifth of S₀, 48dp — D-03p's split, untouched.
     */
    @Test
    fun whereTheBoundDoesNotBiteTheHeadroomIsFourFifthsOfTheSlack() {
        assertPlacement(BASELINE_NARROW, rooms = emptyList(), headroomDp = 192f, footerDp = 48f, firstTileTopDp = 328f)
        assertPlacement(
            BASELINE_NARROW,
            rooms = listOf(COMPACT_CHIP_ROOM),
            headroomDp = 104f,
            footerDp = 48f,
            firstTileTopDp = 328f,
        )
    }

    // ---- §4.2.7's eight-window table: one rule on every 15ac window ------------------

    /**
     * §4.2.7's table "The bound on all eight 15ac windows", at ten px per dp and with no chip: on every
     * row the headroom is H₀ = min(⅘ S₀, C), the footer S₀ − H₀ and at most a third of S₀ (15ac part 1),
     * and the first tile's top sits where the formula puts it in the window's frame. The bound bites on
     * five rows, three of them three-button, and not on the other three; the one function decides all
     * eight. Every expected figure is derived from the formula, the tile tops included, rather than
     * copied from the printed table. `w412dp-h780dp` is computed with the full set's content, as the
     * table projects it; on a device that window takes the compact set (708dp usable), which
     * `HomeHeadroomTest` reads measured.
     */
    @Test
    fun onEveryRowOfTheEightWindowTableTheNoChipHeadroomIsTheSmallerOfFourFifthsOfTheSlackAndC() {
        EIGHT_WINDOW_TABLE.forEach { row ->
            assertPlacement(
                row.window,
                rooms = emptyList(),
                headroomDp = row.headroomDp,
                footerDp = row.footerDp,
                firstTileTopDp = row.tileTopDp,
            )
            assertWithMessage("${row.window.name}: three footers against S₀, in px")
                .that(placementOn(row.window, rooms = emptyList()).footer * THIRDS)
                .isAtMost(row.window.inner - row.window.fixedBands)
        }
    }

    // ---- The chips' room past the headroom, and past the slack ----------------------

    /**
     * Where the chips need more room than the headroom holds, the headroom is 0, the footer gives up
     * the rest down to 0, and the tiles fall by exactly R − H₀ (D-03n's shortfall, with H₀): here R is
     * 320dp against the Pixel_9's H₀ of 299.8dp, so the tiles fall 20.2dp and the footer is
     * S₀ − R = 103.9dp; the column is still exactly the viewport.
     */
    @Test
    fun whereTheRoomExceedsTheHeadroomTheTilesFallByTheShortfallAndTheFooterPaysIt() =
        assertPlacement(
            PIXEL_9,
            rooms = listOf(TALL_CHIP_ROOM, TALL_CHIP_ROOM),
            headroomDp = 0f,
            footerDp = 103.9f,
            firstTileTopDp = 555.4f,
        )

    /** Past the whole slack, the headroom and the footer are both 0 and the column overflows by R − S₀. */
    @Test
    fun whereTheRoomExceedsTheSlackTheColumnOverflowsByTheDifference() {
        val rooms = listOf(OVERFLOWING_CHIP_ROOM, OVERFLOWING_CHIP_ROOM)
        val placement = placementOn(PIXEL_9, rooms)

        assertWithMessage("the headroom, in px").that(placement.headroom).isEqualTo(0)
        assertWithMessage("the footer, in px").that(placement.footer).isEqualTo(0)
        assertWithMessage("the column's height over the inner viewport, in px")
            .that(placement.height - PIXEL_9.inner)
            .isEqualTo(rooms.sum() - PIXEL_9_SLACK_PX)
    }

    // ---- C ≤ 0: a hero row too tall for any headroom ---------------------------------

    /**
     * At a font scale at which the hero row alone reaches past the bound, C is 0: the headroom is 0,
     * the row sits at the top padding, and the whole slack lies below the tiles — never between two
     * elements (D-03q part 2's last bullet, the wrapped title's case that B-21 covers).
     */
    @Test
    fun whereTheHeroRowAloneReachesPastTheBoundThereIsNoHeadroomAndTheSlackLiesBelowTheTiles() {
        val wrapped = QA_NARROW.copy(hero = WRAPPED_HERO_PX)
        val placement = placementOn(wrapped, rooms = emptyList())

        assertWithMessage("the headroom, in px").that(placement.headroom).isEqualTo(0)
        assertWithMessage("the footer, in px: the whole slack")
            .that(placement.footer)
            .isEqualTo(wrapped.inner - wrapped.fixedBands)
        assertWithMessage("the column's height, in px").that(placement.height).isEqualTo(wrapped.inner)
    }

    // ---- Assertions --------------------------------------------------------------

    /**
     * [homeBandPlacement] on [window] with the chips' [rooms]: the headroom and the footer the spec's
     * table gives, the first tile's top where the table puts it in the window's frame, and a column
     * exactly the inner viewport tall while it fits.
     */
    private fun assertPlacement(
        window: Window,
        rooms: List<Int>,
        headroomDp: Float,
        footerDp: Float,
        firstTileTopDp: Float,
    ) {
        val placement = placementOn(window, rooms)
        val chips = "${rooms.size} chip(s) on ${window.name}"

        assertWithMessage("$chips: the headroom, in px").that(placement.headroom).isEqualTo(px(headroomDp))
        assertWithMessage("$chips: the footer, in px").that(placement.footer).isEqualTo(px(footerDp))
        assertWithMessage("$chips: the first tile's top in the window's frame, in px")
            .that(window.firstTileTop(placement.headroom, rooms))
            .isEqualTo(px(firstTileTopDp))
        assertWithMessage("$chips: the column's height, in px").that(placement.height).isEqualTo(window.inner)
    }

    /** One row of the eight-window table: its window, and the formula's no-chip figures in dp. */
    private data class TableRow(
        val window: Window,
        val headroomDp: Float,
        val footerDp: Float,
        val tileTopDp: Float,
    )

    private fun placementOn(
        window: Window,
        rooms: List<Int>,
    ): HomeBandPlacement =
        homeBandPlacement(
            inner = window.inner,
            gap = window.gap,
            hero = window.hero,
            ribbon = window.ribbon,
            tiles = window.tiles,
            chipRooms = rooms,
            titleBound = window.titleBound,
        )

    /**
     * One of §4.2.7's windows in px, its bands' heights, and the bound as `HomeBands` computes it in
     * the viewport's frame: `TITLE_REACH` of the window's height, rounded once, less the entrance slide
     * and the status inset.
     */
    private data class Window(
        val name: String,
        val height: Int,
        val statusInset: Int,
        val navigationInset: Int,
        val gap: Int,
        val hero: Int,
        val ribbon: Int,
        val tiles: List<Int>,
    ) {
        /** The viewport less the column's padding above and below. */
        val inner: Int get() = height - statusInset - navigationInset - 2 * gap

        val titleBound: Int
            get() = (BackdropMetrics.TITLE_REACH * height).roundToInt() - ENTRANCE_SLIDE_PX - statusInset

        /** The bands and the four band gaps the column has with no chip. */
        val fixedBands: Int get() = hero + ribbon + tiles.sum() + NO_CHIP_GAPS * gap

        /** The first tile's top in the window's frame: the status inset, the padding, the headroom, the bands above. */
        fun firstTileTop(
            headroom: Int,
            rooms: List<Int>,
        ): Int = statusInset + gap + headroom + hero + gap + ribbon + gap + rooms.sum()
    }

    private companion object {
        /** §4.2.7's figures, read at ten px per dp. */
        const val PX_PER_DP = 10

        fun px(dp: Float): Int = (dp * PX_PER_DP).roundToInt()

        /** `entrance_slide_distance`, 16dp, at ten px per dp. */
        const val ENTRANCE_SLIDE_PX = 160

        /** The column's band gaps with no chip: hero→ribbon, ribbon→tile 1, tile 1→tile 2, the footer's. */
        const val NO_CHIP_GAPS = 4

        /** A chip's room: its 80dp height and its band gap, 16dp in the full set and 8dp in the compact one. */
        const val FULL_CHIP_ROOM = 960
        const val COMPACT_CHIP_ROOM = 880

        /** A chip's room at a font scale where it stands 144dp, so two outgrow the Pixel_9's headroom: R 320dp. */
        const val TALL_CHIP_ROOM = 1600

        /** A chip's room so large that two outgrow the whole slack. */
        const val OVERFLOWING_CHIP_ROOM = 2500

        /** A hero row wrapped so tall at a large font scale that it alone reaches past the bound. */
        const val WRAPPED_HERO_PX = 2400

        /** S₀ on the Pixel_9's window: 423.9dp. */
        const val PIXEL_9_SLACK_PX = 4239

        /** The Pixel_9 (411×923dp), a 54dp status bar and a 24dp gesture bar, the full band set. */
        val PIXEL_9 =
            Window(
                name = "the Pixel_9",
                height = 9230,
                statusInset = 540,
                navigationInset = 240,
                gap = 160,
                hero = 480,
                ribbon = 854,
                tiles = listOf(958, 959),
            )

        /** QA's 360×640dp window, a 52dp status bar and a 24dp gesture bar, the compact band set. */
        val QA_NARROW =
            Window(
                name = "QA's 360x640dp",
                height = 6400,
                statusInset = 520,
                navigationInset = 240,
                gap = 80,
                hero = 480,
                ribbon = 400,
                tiles = listOf(960, 960),
            )

        /** The design baseline, 360×640dp with a 24dp status bar and a 48dp three-button bar. */
        val BASELINE_NARROW =
            QA_NARROW.copy(name = "the 360x640dp design baseline", statusInset = 240, navigationInset = 480)

        /** 15g's bound on the footer: a third of S₀. */
        const val THIRDS = 3

        /** A 412dp-wide window of the full band set, 24dp status bar and 48dp three-button bar. */
        private fun full(
            name: String,
            height: Int,
        ): Window = PIXEL_9.copy(name = name, height = height, statusInset = 240, navigationInset = 480)

        /** A 360dp-wide window of the compact band set, 24dp status bar and 48dp three-button bar. */
        private fun compact(
            name: String,
            height: Int,
        ): Window = BASELINE_NARROW.copy(name = name, height = height)

        /** The 6.7-inch window in gesture navigation: a 24dp status bar and a 24dp gesture bar. */
        val TALL_GESTURE = full("w412dp-h915dp, 24 + 24", 9150).copy(navigationInset = 240)

        /**
         * §4.2.7's eight 15ac windows, with no chip, each figure the formula's: H₀ = min(⅘ S₀, C), the
         * footer S₀ − H₀, the first tile's top at the status inset, the padding, H₀, the hero row, a gap,
         * the ribbon and a gap.
         */
        val EIGHT_WINDOW_TABLE =
            listOf(
                TableRow(compact("w360dp-h640dp", 6400), headroomDp = 192f, footerDp = 48f, tileTopDp = 328f),
                TableRow(compact("w360dp-h720dp", 7200), headroomDp = 242.4f, footerDp = 77.6f, tileTopDp = 378.4f),
                TableRow(compact("w360dp-h800dp", 8000), headroomDp = 280f, footerDp = 120f, tileTopDp = 416f),
                TableRow(full("w412dp-h780dp", 7800), headroomDp = 229.5f, footerDp = 57.4f, tileTopDp = 434.9f),
                TableRow(full("w412dp-h850dp", 8500), headroomDp = 285.5f, footerDp = 71.4f, tileTopDp = 490.9f),
                TableRow(full("w412dp-h915dp", 9150), headroomDp = 326.1f, footerDp = 95.8f, tileTopDp = 531.5f),
                TableRow(TALL_GESTURE, headroomDp = 326.1f, footerDp = 119.8f, tileTopDp = 531.5f),
                TableRow(PIXEL_9, headroomDp = 299.8f, footerDp = 124.1f, tileTopDp = 535.2f),
            )
    }
}
