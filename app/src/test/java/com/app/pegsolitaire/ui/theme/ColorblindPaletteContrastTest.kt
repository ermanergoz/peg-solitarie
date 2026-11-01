package com.app.pegsolitaire.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.app.pegsolitaire.ui.game.BoardMetrics
import com.app.pegsolitaire.ui.game.recipe
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * §3.5 of docs/ux/colour-vision.md, re-proved on every Check from `Color.kt` (CV-AC07).
 *
 * The spec's numbers came from one script run once; this test is that computation, so a token
 * changed later fails here rather than silently invalidating the table. The method is §0's:
 * - **contrast** is the WCAG 2.x ratio of relative luminances, from the hexes of `Color.kt`;
 * - a **translucent mark** is blended over what it is drawn on in gamma-encoded sRGB, which is
 *   what `drawCircle(color.copy(alpha = …))` does, before anything is simulated;
 * - **protanopia, deuteranopia and tritanopia** are the Machado, Oliveira and Fernandes (2009)
 *   severity-1.0 matrices applied to linear sRGB, clamped to the unit cube; **monochromacy** is
 *   luminance alone, so its ratio is normal vision's.
 *
 * The matrices are transcribed from the paper's severity-1.0 table, and the third case below is
 * the transcription check the tech-lead's brief asks for: every cell of the spec that the tokens
 * on this tree determine must come out within [TOLERANCE] of the spec's own figure, so a slip in a
 * matrix entry shows up as a miss rather than as a silent pass.
 */
class ColorblindPaletteContrastTest {
    /**
     * Rows 1, 2, 4, 5, 9, 10, 11 and 12 hold at 3:1 (`docs/ux-spec.md` §11's bar for glyphs and
     * boundaries) under normal vision, the three dichromacies and monochromacy, in both themes.
     */
    @Test
    fun everyColourBlindMarkReadsThreeToOneAgainstItsNeighboursInEverySimulatedVision() {
        Theme.entries.forEach { theme ->
            theme.colourBlindRows().forEach { row ->
                Vision.entries.forEach { vision ->
                    assertWithMessage("${theme.name}, row ${row.number} (${row.what}), $vision")
                        .that(row.worst(vision))
                        .isAtLeast(GLYPH_CONTRAST)
                }
            }
        }
    }

    /**
     * Row 2's "today" and row 3: the standard ring merges into the peg's shaded edge (≤ 1.2:1
     * somewhere, in every vision and both themes) — the finding the halo exists for — and the
     * colour-blind palette's halo stands apart from that edge by the spec's recorded figures.
     */
    @Test
    fun theHaloSeparatesTheRingFromThePegsShadedEdge() {
        Theme.entries.forEach { theme ->
            val ring = boardMarksFor(BoardMarkPalette.Standard, theme.dark).selectionRing.toSrgb()
            Vision.entries.forEach { vision ->
                assertWithMessage("${theme.name}: the standard ring against the peg's edge, $vision")
                    .that(EVERY_15_DEGREES.minOf { contrast(ring, theme.pegEdgeAt(it), vision) })
                    .isAtMost(RING_MERGES_INTO_PEG)
            }
        }
        assertHaloAgainstTheEdge(Theme.Light, SHADED_HALF, Vision.entries, specFigure = 3.36)
        assertHaloAgainstTheEdge(Theme.Light, LIT_HALF, Vision.entries, specFigure = 2.33)
        assertHaloAgainstTheEdge(Theme.Dark, SHADED_HALF, listOf(Vision.Protanopia), specFigure = 2.85)
        assertHaloAgainstTheEdge(Theme.Dark, SHADED_HALF, Vision.entries - Vision.Protanopia, specFigure = 3.79)
        assertHaloAgainstTheEdge(Theme.Dark, LIT_HALF, Vision.entries, specFigure = 4.67)
    }

    /**
     * The transcription check: rows 1, 2, 4 and 5 cell by cell (normal / protanopia / deuteranopia
     * / tritanopia / monochromacy), row 8's connector, the worst cases of rows 9 to 12 where the
     * tokens on this tree set them, and the standard palette's "today" figures for rows 4, 5 and 8,
     * which are the only ones that exercise the alpha blend.
     *
     * Rows 9, 10 and 11 in light are floors the spec computed against a Results sky that direction B
     * replaced with the Sparse wash (candy-backdrop B-23), which is never darker than the background
     * in light. Here and under B the worst neighbour is the background or the catalogue card, so
     * those three are held at or above the spec's figure, not to it, until the ux-designer restates
     * them (tech-lead D-6).
     */
    @Test
    fun theComputedRatiosMatchTheSpecsTableWithinFiveHundredths() {
        SPEC_CELLS.forEach { (theme, number, columns) ->
            val row = theme.colourBlindRow(number)
            Vision.entries.zip(columns).forEach { (vision, figure) ->
                assertWithMessage("${theme.name}, row $number, $vision")
                    .that(row.worst(vision))
                    .isWithin(TOLERANCE)
                    .of(figure)
            }
        }
        SPEC_FLOORS.forEach { floor ->
            assertWithMessage("${floor.theme.name}, ${floor.row.what}, ${floor.visions}")
                .that(floor.row.worstOf(floor.visions))
                .isWithin(TOLERANCE)
                .of(floor.figure)
        }
        LIGHT_STAR_FLOORS.forEach { (row, figure) ->
            assertWithMessage("Light, ${row.what}").that(row.worstOf(Vision.entries)).isAtLeast(figure - TOLERANCE)
        }
    }

    private fun assertHaloAgainstTheEdge(
        theme: Theme,
        angles: List<Int>,
        visions: List<Vision>,
        specFigure: Double,
    ) {
        val halo = checkNotNull(boardMarksFor(BoardMarkPalette.Colorblind, theme.dark).selectionHalo).toSrgb()
        val worst = visions.minOf { vision -> angles.minOf { contrast(halo, theme.pegEdgeAt(it), vision) } }

        assertWithMessage("${theme.name}: the halo against the peg's edge at $angles under $visions")
            .that(worst)
            .isWithin(TOLERANCE)
            .of(specFigure)
    }
}

// ---- The pairs of §3.5 -----------------------------------------------------------------------

/** One row of §3.5: each of [marks] against each of its [neighbours]; the row reads its worst pair. */
private class MarkPair(
    val number: Int,
    val what: String,
    val marks: List<DoubleArray>,
    val neighbours: List<DoubleArray>,
) {
    fun worst(vision: Vision): Double = marks.minOf { mark -> neighbours.minOf { contrast(mark, it, vision) } }

    fun worstOf(visions: List<Vision>): Double = visions.minOf(::worst)
}

/** A worst case the spec states as one figure, over [visions]. */
private class Floor(
    val theme: Theme,
    val row: MarkPair,
    val figure: Double,
    val visions: List<Vision> = Vision.entries,
)

private enum class Theme(
    val scheme: ColorScheme,
    val pegGradient: List<Color>,
    val dark: Boolean,
) {
    Light(LightColors, LightPegGradientColors, dark = false),
    Dark(DarkColors, DarkPegGradientColors, dark = true),
}

/** The board slab (`GameScreen`'s `surfaceContainerHigh` surface), which every board mark sits on. */
private val Theme.slab: DoubleArray get() = scheme.surfaceContainerHigh.toSrgb()

/** The catalogue card's container (`ClassicCatalogScreen`), behind the catalogue's star pips. */
private val Theme.catalogueCard: DoubleArray get() = scheme.surfaceContainerHigh.toSrgb()

/** The screen background behind the Results star row and the tutorial's step pips and completion star. */
private val Theme.background: DoubleArray get() = scheme.background.toSrgb()

/** Rows 1, 2, 4, 5, 9, 10, 11 and 12 of §3.5 for the colour-blind palette, drawn with its own recipe's alphas. */
private fun Theme.colourBlindRows(): List<MarkPair> {
    val marks = boardMarksFor(BoardMarkPalette.Colorblind, dark)
    val recipe = BoardMarkPalette.Colorblind.recipe
    return listOf(
        MarkPair(1, "selection ring against the slab", listOf(marks.selectionRing.toSrgb()), listOf(slab)),
        MarkPair(
            2,
            "selection ring against the halo",
            listOf(marks.selectionRing.toSrgb()),
            listOf(checkNotNull(marks.selectionHalo).toSrgb()),
        ),
        MarkPair(
            4,
            "legal-target mark against the slab",
            listOf(marks.legalTarget.over(slab, recipe.destinationDotAlpha)),
            listOf(slab),
        ),
        MarkPair(
            5,
            "hint rings against the slab",
            listOf(marks.hintRing.over(slab, recipe.hintRingAlpha)),
            listOf(slab),
        ),
        MarkPair(
            9,
            "earned-star outline against the background",
            listOf(checkNotNull(marks.earnedStarOutline).toSrgb()),
            listOf(background),
        ),
        MarkPair(
            10,
            "earned-star fill against the background and the catalogue card",
            marks.earnedStarFill.map { it.toSrgb() },
            listOf(background, catalogueCard),
        ),
        MarkPair(
            11,
            "unearned-star outline against the background and the catalogue card",
            listOf(marks.unearnedStarOutline.toSrgb()),
            listOf(background, catalogueCard),
        ),
        MarkPair(
            12,
            "done pip against the tutorial background",
            marks.donePipFill.map { it.toSrgb() },
            listOf(background),
        ),
    )
}

private fun Theme.colourBlindRow(number: Int): MarkPair =
    when (number) {
        CONNECTOR_ROW ->
            MarkPair(
                CONNECTOR_ROW,
                "hint connector against the slab",
                listOf(
                    boardMarksFor(BoardMarkPalette.Colorblind, dark)
                        .hintRing
                        .over(slab, BoardMarkPalette.Colorblind.recipe.hintConnectorAlpha),
                ),
                listOf(slab),
            )
        else -> colourBlindRows().single { it.number == number }
    }

/** The standard palette's translucent marks, as §3.5's "Today" column measures them. */
private fun Theme.standardRow(
    number: Int,
    what: String,
    color: Color,
    alpha: Float,
): MarkPair = MarkPair(number, what, listOf(color.over(slab, alpha)), listOf(slab))

/** Row 3's edge: the peg's drawn radial gradient on its own rim, at [degrees] clockwise from three o'clock. */
private fun Theme.pegEdgeAt(degrees: Int): DoubleArray {
    val theta = Math.toRadians(degrees.toDouble())
    val offset = BoardMetrics.PEG_GRADIENT_OFFSET_RATIO.toDouble()
    // The gradient's centre sits [offset] radii up and left of the peg's, and it spans
    // PEG_GRADIENT_RADIUS_RATIO radii; both scale with the peg, so the rim's colours do not
    // depend on the cell size.
    val t = hypot(cos(theta) + offset, sin(theta) + offset) / BoardMetrics.PEG_GRADIENT_RADIUS_RATIO
    val mid = BoardMetrics.PEG_GRADIENT_MID_STOP.toDouble()
    val (highlight, body, rim) = pegGradient.map { it.toSrgb() }
    return if (t <= mid) lerp(highlight, body, t / mid) else lerp(body, rim, min(1.0, (t - mid) / (1.0 - mid)))
}

// ---- Colour arithmetic -----------------------------------------------------------------------

private fun Color.toSrgb(): DoubleArray = doubleArrayOf(red.toDouble(), green.toDouble(), blue.toDouble())

/** This colour at [alpha] over [backing], blended per channel in gamma-encoded sRGB. */
private fun Color.over(
    backing: DoubleArray,
    alpha: Float,
): DoubleArray = lerp(backing, toSrgb(), alpha.toDouble())

private fun lerp(
    from: DoubleArray,
    to: DoubleArray,
    t: Double,
): DoubleArray = DoubleArray(CHANNELS) { from[it] + (to[it] - from[it]) * t }

/** IEC 61966-2-1's sRGB transfer function, encoded → linear. */
private fun toLinear(encoded: Double): Double =
    if (encoded <= SRGB_LINEAR_KNEE) {
        encoded / SRGB_LINEAR_SLOPE
    } else {
        ((encoded + SRGB_OFFSET) / (1.0 + SRGB_OFFSET)).pow(SRGB_GAMMA)
    }

/** WCAG 2.x contrast: `(L1 + 0.05) / (L2 + 0.05)`, the lighter luminance over the darker. */
private fun contrast(
    first: DoubleArray,
    second: DoubleArray,
    vision: Vision,
): Double {
    val a = vision.luminanceOf(first)
    val b = vision.luminanceOf(second)
    return (max(a, b) + WCAG_FLARE) / (min(a, b) + WCAG_FLARE)
}

/**
 * A vision and how it sees a colour. Each dichromacy is a Machado, Oliveira and Fernandes (2009)
 * matrix ("A Physiologically-based Model for Simulation of Color Vision Deficiency", IEEE TVCG
 * 15(6), the precomputed severity-1.0 table), applied to linear sRGB and clamped; monochromacy
 * keeps luminance alone, which is the luminance normal vision reads.
 */
private enum class Vision(
    private val matrix: List<List<Double>>?,
) {
    Normal(null),
    Protanopia(
        listOf(
            listOf(0.152286, 1.052583, -0.204868),
            listOf(0.114503, 0.786281, 0.099216),
            listOf(-0.003882, -0.048116, 1.051998),
        ),
    ),
    Deuteranopia(
        listOf(
            listOf(0.367322, 0.860646, -0.227968),
            listOf(0.280085, 0.672501, 0.047413),
            listOf(-0.011820, 0.042940, 0.968881),
        ),
    ),
    Tritanopia(
        listOf(
            listOf(1.255528, -0.076749, -0.178779),
            listOf(-0.078411, 0.930809, 0.147602),
            listOf(0.004733, 0.691367, 0.303900),
        ),
    ),
    Monochromacy(null),
    ;

    /** WCAG relative luminance, `Y = 0.2126 R + 0.7152 G + 0.0722 B` on linear values, as this vision sees [srgb]. */
    fun luminanceOf(srgb: DoubleArray): Double {
        val linear = srgb.map(::toLinear)
        val seen =
            matrix?.map { row -> row.zip(linear).sumOf { (weight, channel) -> weight * channel }.coerceIn(0.0, 1.0) }
                ?: linear
        return LUMINANCE_WEIGHTS.zip(seen).sumOf { (weight, channel) -> weight * channel }
    }
}

// ---- The spec's figures ------------------------------------------------------------------------

/** Rows whose five columns the spec gives cell by cell: (theme, row, normal / P / D / T / mono). */
private val SPEC_CELLS: List<Triple<Theme, Int, List<Double>>> =
    listOf(
        Triple(Theme.Light, 1, listOf(4.86, 4.17, 4.51, 4.53, 4.86)),
        Triple(Theme.Light, 2, listOf(5.54, 4.76, 5.14, 5.16, 5.54)),
        Triple(Theme.Light, 4, listOf(4.86, 4.17, 4.51, 4.53, 4.86)),
        Triple(Theme.Light, 5, listOf(4.28, 5.25, 3.82, 4.33, 4.28)),
        Triple(Theme.Light, CONNECTOR_ROW, listOf(1.99, 2.15, 1.90, 2.01, 1.99)),
        Triple(Theme.Dark, 1, listOf(5.21, 5.32, 5.17, 5.34, 5.21)),
        Triple(Theme.Dark, 2, listOf(6.85, 7.01, 6.79, 7.05, 6.85)),
        Triple(Theme.Dark, 4, listOf(5.21, 5.32, 5.17, 5.34, 5.21)),
        Triple(Theme.Dark, 5, listOf(8.50, 7.68, 9.01, 7.83, 8.50)),
    )

/**
 * Worst cases the spec states as one figure, each set by a surface on this tree: rows 9 to 12 in
 * dark and row 12 in light; row 8's dark connector and row 9's catalogue-card aside, both given
 * for normal vision; and the standard palette's "Today" figures for the legal-target mark, the
 * hint rings and the connector.
 */
private val SPEC_FLOORS: List<Floor>
    get() =
        listOf(
            Floor(Theme.Dark, Theme.Dark.colourBlindRow(9), 15.0),
            Floor(Theme.Dark, Theme.Dark.colourBlindRow(10), 7.68),
            Floor(Theme.Dark, Theme.Dark.colourBlindRow(11), 5.56),
            Floor(Theme.Dark, Theme.Dark.colourBlindRow(12), 9.58),
            Floor(Theme.Light, Theme.Light.colourBlindRow(12), 3.56),
            Floor(Theme.Dark, Theme.Dark.colourBlindRow(CONNECTOR_ROW), 3.2, listOf(Vision.Normal)),
            Floor(Theme.Light, Theme.Light.cardOutline(), 6.6, listOf(Vision.Normal)),
            Floor(Theme.Light, Theme.Light.todayRow(4), 1.73),
            Floor(Theme.Dark, Theme.Dark.todayRow(4), 1.93),
            Floor(Theme.Light, Theme.Light.todayRow(5), 1.61),
            Floor(Theme.Dark, Theme.Dark.todayRow(5), 6.49),
            Floor(Theme.Light, Theme.Light.todayRow(CONNECTOR_ROW), 1.32),
        )

/**
 * Light rows 9 to 11: the spec's figures, which no surface on this tree or under backdrop direction B
 * reaches (candy-backdrop B-23); this tree's worst case must clear them (tech-lead D-6).
 */
private val LIGHT_STAR_FLOORS: List<Pair<MarkPair, Double>>
    get() {
        val fill = Theme.Light.colourBlindRow(10)
        return listOf(
            Theme.Light.colourBlindRow(9) to 6.02,
            MarkPair(10, "earned-star fill, light stop", fill.marks.take(1), fill.neighbours) to 3.22,
            MarkPair(10, "earned-star fill, deep stop", fill.marks.drop(1), fill.neighbours) to 3.64,
            Theme.Light.colourBlindRow(11) to 5.02,
        )
    }

/** Row 9's aside: the earned-star outline on the catalogue card ("≈ 6.6", normal vision). */
private fun Theme.cardOutline(): MarkPair =
    MarkPair(
        9,
        "earned-star outline on the catalogue card",
        listOf(checkNotNull(boardMarksFor(BoardMarkPalette.Colorblind, dark).earnedStarOutline).toSrgb()),
        listOf(catalogueCard),
    )

/** §3.5's "Today" column: the standard palette's translucent marks against the slab. */
private fun Theme.todayRow(number: Int): MarkPair {
    val marks = boardMarksFor(BoardMarkPalette.Standard, dark)
    val recipe = BoardMarkPalette.Standard.recipe
    return when (number) {
        4 -> standardRow(4, "today's legal-target mark against the slab", marks.legalTarget, recipe.destinationDotAlpha)
        5 -> standardRow(5, "today's hint rings against the slab", marks.hintRing, recipe.hintRingAlpha)
        else ->
            standardRow(number, "today's hint connector against the slab", marks.hintRing, recipe.hintConnectorAlpha)
    }
}

/** §3.5 row 8, the hint connector: recorded, not held to 3:1 (the two rings identify the move). */
private const val CONNECTOR_ROW = 8

/** `docs/ux-spec.md` §11: 3:1 for glyphs and boundaries. */
private const val GLYPH_CONTRAST = 3.0

/** §2's finding: the standard ring and the peg's shaded edge read 1.02 to 1.18:1. */
private const val RING_MERGES_INTO_PEG = 1.2

/** The agreement the brief asks of every computed ratio with the spec's two-decimal figure. */
private const val TOLERANCE = 0.05

/** The rim sampled every 15°, as §2 and row 3 sample it. */
private val EVERY_15_DEGREES: List<Int> = (0 until FULL_TURN_DEGREES step SAMPLE_STEP_DEGREES).toList()

/** The half of the rim facing away from the gradient's highlight (down and right): −45° to 135°. */
private val SHADED_HALF: List<Int> =
    (SHADED_FROM_DEGREES..LIT_FROM_DEGREES step SAMPLE_STEP_DEGREES)
        .map { (it + FULL_TURN_DEGREES) % FULL_TURN_DEGREES }

/** The half facing the highlight (up and left): 135° to 315°. */
private val LIT_HALF: List<Int> = (LIT_FROM_DEGREES..LIT_TO_DEGREES step SAMPLE_STEP_DEGREES).toList()

private const val FULL_TURN_DEGREES = 360
private const val SAMPLE_STEP_DEGREES = 15
private const val SHADED_FROM_DEGREES = -45
private const val LIT_FROM_DEGREES = 135
private const val LIT_TO_DEGREES = 315

private const val CHANNELS = 3
private const val SRGB_LINEAR_KNEE = 0.04045
private const val SRGB_LINEAR_SLOPE = 12.92
private const val SRGB_OFFSET = 0.055
private const val SRGB_GAMMA = 2.4
private const val WCAG_FLARE = 0.05
private val LUMINANCE_WEIGHTS = listOf(0.2126, 0.7152, 0.0722)
