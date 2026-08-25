package com.app.pegsolitaire.domain.generator

import com.app.pegsolitaire.domain.golden.BoardCodec
import com.app.pegsolitaire.domain.golden.GoldenMasterAssert
import org.junit.Test

/**
 * The determinism LOCK for the Challenge generator (CHAL-03, QUAL-04) — the phase's
 * #1, ship-blocking deliverable (roadmap Risk note).
 *
 * For each sampled level it snapshots the finalized generator output (starting
 * board + recorded winning line + `generatorVersion`) into the committed
 * `challenge_level_<n>.txt` fixtures, using the Phase-1 [BoardCodec] +
 * [GoldenMasterAssert] harness VERBATIM — no new serializer or assert helper (only
 * the inputs change). From the commit that lands these fixtures, v1 output is
 * FROZEN: any generator change that alters a fixture requires a deliberate
 * `GeneratorConstants.GENERATOR_VERSION` bump + intentional regen with
 * `UPDATE_GOLDEN=true` (Pitfall 2).
 *
 * The sampled levels span the curve — a level-1 on-ramp board, an adjacent level 2,
 * a low canonical level 10, a mid-curve level 100, and a deep freeform level 1000 —
 * so the lock covers canonical footprints, freeform footprints, and the bounded
 * deterministic fallback alike.
 */
class ChallengeGeneratorGoldenTest {
    @Test
    fun sampledChallengeLevelsAreByteIdenticalToTheirGoldenFixtures() {
        SAMPLE_LEVELS.forEach { levelNumber ->
            val level = LevelGenerator.generate(levelNumber)

            val encoded =
                buildString {
                    append("level=").append(levelNumber).append('\n')
                    append("generatorVersion=").append(level.generatorVersion).append('\n')
                    append(BoardCodec.encodeBoard(level.startingBoard))
                    append("\nsolution:\n")
                    append(BoardCodec.encodeMoves(level.solution))
                }

            GoldenMasterAssert.assertMatches(
                fixtureName = "challenge_level_$levelNumber.txt",
                actual = encoded,
            )
        }
    }

    private companion object {
        /** The frozen determinism-lock sample (canonical + freeform + fallback coverage). */
        val SAMPLE_LEVELS = listOf(1, 2, 10, 100, 1000)
    }
}
