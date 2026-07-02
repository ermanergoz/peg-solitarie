package com.app.pegsolitaire.domain.generator

import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.random.Random

/**
 * Validity spec for freeform footprints (CHAL-01, D-08/D-09).
 *
 * The genuinely-new checks (no in-repo analog): an INDEPENDENT orthogonal
 * flood-fill connectivity check (deliberately re-implemented here, not the
 * production one, so the test is a true cross-check) and the `size <= 64` Long
 * bitmask cap. Plus intro-level gating and the on-grid / bounding-box cap, and that
 * freeform levels stay reverse-generable-or-reseed (never crash).
 */
class FreeformValidityTest {
    private val canonicalSpecs =
        setOf(
            Boards.English33,
            Boards.European37,
            Boards.Asymmetric39,
            Boards.Diamond41,
            Boards.Wiegleb45,
        )

    private val boundingDim = GeneratorConstants.FREEFORM_BOUNDING_DIM

    @Test
    fun everyFreeformFootprintIsOrthogonallyConnected() {
        val tier = DifficultyTiers.forLevel(FREEFORM_SAMPLE_LEVEL)
        var produced = 0
        for (seed in 0 until FREEFORM_SEED_SAMPLES) {
            val spec = FreeformFootprints.build(Random(seed.toLong()), tier) ?: continue
            produced++
            assertThat(floodFillReachesEveryCell(spec.inPlay)).isTrue()
        }
        // The builder must actually produce shapes (connectivity holds by construction).
        assertThat(produced).isGreaterThan(0)
    }

    @Test
    fun everyFreeformFootprintFitsTheLongBitmaskCap() {
        val tier = DifficultyTiers.forLevel(FREEFORM_SAMPLE_LEVEL)
        for (seed in 0 until FREEFORM_SEED_SAMPLES) {
            val spec = FreeformFootprints.build(Random(seed.toLong()), tier) ?: continue
            assertThat(spec.size).isAtMost(GeneratorConstants.MAX_IN_PLAY_CELLS)
        }
    }

    @Test
    fun everyFreeformCellLiesOnTheSquareGridWithinTheBoundingBoxCap() {
        val tier = DifficultyTiers.forLevel(FREEFORM_SAMPLE_LEVEL)
        for (seed in 0 until FREEFORM_SEED_SAMPLES) {
            val spec = FreeformFootprints.build(Random(seed.toLong()), tier) ?: continue
            spec.inPlay.forEach { position ->
                assertThat(position.row).isIn(0 until boundingDim)
                assertThat(position.col).isIn(0 until boundingDim)
            }
        }
    }

    @Test
    fun footprintsAreCanonicalBelowTheIntroLevelAndFreeformAtIt() {
        val introLevel = DifficultyTiers.forLevel(1).freeformIntroLevel

        for (n in 1 until introLevel) {
            assertThat(LevelGenerator.generate(n).startingBoard.spec in canonicalSpecs).isTrue()
        }

        // At the intro level freeform footprints become eligible and are used.
        assertThat(LevelGenerator.generate(introLevel).startingBoard.spec in canonicalSpecs)
            .isFalse()
    }

    @Test
    fun freeformLevelsStayReverseGenerableOrReseedWithoutCrashing() {
        val introLevel = DifficultyTiers.forLevel(1).freeformIntroLevel
        listOf(introLevel, introLevel + 1, 100, 500, 1000).forEach { n ->
            val level = LevelGenerator.generate(n)

            // A freeform footprint that admits no reverse walk simply reseeds; whatever
            // is finally accepted must still fold to one peg (solvable by construction).
            val finalBoard =
                level.solution.fold(level.startingBoard) { board, move ->
                    RulesEngine.applyMove(board, move)
                }
            assertThat(finalBoard.pegCount).isEqualTo(1)
        }
    }

    private fun floodFillReachesEveryCell(positions: List<Position>): Boolean {
        val cells = positions.toHashSet()
        if (cells.isEmpty()) return false
        val neighbors = listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0)
        val seen = HashSet<Position>()
        val queue = ArrayDeque<Position>()
        val start = positions.first()
        seen += start
        queue += start
        while (queue.isNotEmpty()) {
            val cell = queue.removeFirst()
            for ((rowDelta, colDelta) in neighbors) {
                val next = Position(row = cell.row + rowDelta, col = cell.col + colDelta)
                if (next in cells && seen.add(next)) queue += next
            }
        }
        return seen.size == cells.size
    }

    private companion object {
        /** A high level (freeform-eligible) whose tier yields a sizeable freeform shape. */
        const val FREEFORM_SAMPLE_LEVEL = 1000

        /** Distinct RNG seeds sampled to exercise the builder. */
        const val FREEFORM_SEED_SAMPLES = 40
    }
}
