package com.app.pegsolitaire.domain.generator

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * [LevelGenerator.generateLadder] exposes the chain [LevelGenerator.generate] already walks, so the
 * fuzz harness can visit levels `1..N` in one O(N) pass instead of N calls that cost O(N²) together
 * (docs/tech-design.md §7.3 item 2).
 *
 * The frozen generator contract is about OUTPUT, so these tests pin that the ladder IS that output,
 * level by level. The bytes themselves stay pinned by the golden fixtures
 * (`ChallengeGeneratorGoldenTest`, `SolutionFixtures`), which must stay green unchanged.
 */
class LevelGeneratorLadderTest {
    private val sampledBounds = listOf(1, 2, 3, 10, 100, 1000)

    @Test
    fun theLadderYieldsEachLevelInOrderAndEndsWhereGenerateDoes() {
        sampledBounds.forEach { upTo ->
            val ladder = LevelGenerator.generateLadder(upTo).toList()

            assertWithMessage("levels in generateLadder(%s)", upTo).that(ladder).hasSize(upTo)
            ladder.forEachIndexed { index, level ->
                assertWithMessage("levelNumber at index %s of generateLadder(%s)", index, upTo)
                    .that(level.levelNumber)
                    .isEqualTo(index + 1)
            }
            sampledBounds.filter { it <= upTo }.forEach { k ->
                assertSameLevel(fromLadder = ladder[k - 1], fromGenerate = LevelGenerator.generate(k))
            }
        }
    }

    @Test
    fun theLadderClampsItsBoundLikeGenerate() {
        listOf(0, -5).forEach { upTo ->
            val ladder = LevelGenerator.generateLadder(upTo).toList()

            assertWithMessage("levels in generateLadder(%s)", upTo).that(ladder).hasSize(1)
            assertThat(ladder.single().levelNumber).isEqualTo(ChallengeLevelBounds.MIN)
        }
    }

    /**
     * Content, not identity: a freeform footprint is a fresh `BoardSpec` instance per generation,
     * so the spec is compared through its in-play positions (as `ChallengeSolvabilityTest` does).
     */
    private fun assertSameLevel(
        fromLadder: GeneratedLevel,
        fromGenerate: GeneratedLevel,
    ) {
        val level = fromGenerate.levelNumber
        assertWithMessage("levelNumber of level %s", level).that(fromLadder.levelNumber).isEqualTo(level)
        assertWithMessage("pegs of level %s", level)
            .that(fromLadder.startingBoard.pegs)
            .isEqualTo(fromGenerate.startingBoard.pegs)
        assertWithMessage("footprint of level %s", level)
            .that(fromLadder.startingBoard.spec.inPlay)
            .isEqualTo(fromGenerate.startingBoard.spec.inPlay)
        assertWithMessage("solution of level %s", level).that(fromLadder.solution).isEqualTo(fromGenerate.solution)
        assertWithMessage("generatorVersion of level %s", level)
            .that(fromLadder.generatorVersion)
            .isEqualTo(fromGenerate.generatorVersion)
    }
}
