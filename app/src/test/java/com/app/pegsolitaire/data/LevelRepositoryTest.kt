package com.app.pegsolitaire.data

import com.app.pegsolitaire.domain.generator.GeneratorConstants
import com.app.pegsolitaire.domain.generator.LevelGenerator
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * JVM (Robolectric) coverage of the [LevelRepository] seam — the single source of
 * truth for level data (CLAS-01/CLAS-02).
 *
 * Robolectric supplies a real application [android.content.Context] so the data
 * layer can resolve board display names from `R.string.board_*` (proving those
 * resources are live, not dead). The repository is constructed directly over its
 * real Classic source and the throwing Challenge stub — no Hilt graph needed.
 *
 * Security control under test: an unknown/forged level id must fail fast
 * (T-02-04-ID) rather than silently returning a wrong board.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class LevelRepositoryTest {
    private val context get() = RuntimeEnvironment.getApplication()

    private fun repository(): LevelRepository =
        DefaultLevelRepository(
            classicSource = DefaultClassicLevelSource(context),
            challengeSource = GeneratedChallengeLevelSource(LevelGenerator),
        )

    @Test
    fun classicCatalogServesFiveBoardsInAscendingHoleOrder() {
        val catalog = repository().classicCatalog()

        assertThat(catalog).hasSize(EXPECTED_CATALOG_SIZE)
        assertThat(catalog.map { it.id })
            .containsExactly(
                ClassicLevelIds.ENGLISH33,
                ClassicLevelIds.EUROPEAN37,
                ClassicLevelIds.ASYMMETRIC39,
                ClassicLevelIds.DIAMOND41,
                ClassicLevelIds.WIEGLEB45,
            ).inOrder()
        assertThat(catalog.map { it.holeCount }).containsExactly(33, 37, 39, 41, 45).inOrder()
    }

    @Test
    fun everyCatalogDescriptorHasAResolvedDisplayName() {
        // Every name resolved from a board_* string resource — catches a regression
        // to a hardcoded literal or a missing/renamed resource.
        assertThat(repository().classicCatalog().all { it.displayName.isNotBlank() }).isTrue()
    }

    @Test
    fun loadClassicReturnsTheMatchingInitialBoardAndHoleCount() {
        val level = repository().loadClassic(ClassicLevelIds.ENGLISH33)

        assertThat(level.holeCount).isEqualTo(ENGLISH33_HOLE_COUNT)
        assertThat(level.initialBoard).isEqualTo(Boards.english33Initial)
    }

    @Test
    fun loadClassicWithUnknownIdFailsFast() {
        assertThrows(IllegalArgumentException::class.java) {
            repository().loadClassic("nope")
        }
    }

    @Test
    fun loadChallengeReturnsASolvableLevelCarryingSolutionAndGeneratorVersion() {
        val level = repository().loadChallenge(1)

        // The generator's version stamp is carried through the data Level (T-03-08).
        assertThat(level.generatorVersion).isEqualTo(GeneratorConstants.GENERATOR_VERSION)

        // Solvable by construction: the recorded winning line replays legally through
        // the unchanged RulesEngine and folds the starting board down to a single peg.
        val solution = requireNotNull(level.solution) { "Challenge level must carry a solution" }
        val finalBoard =
            solution.fold(level.initialBoard) { board, move ->
                assertThat(move in RulesEngine.legalMoves(board)).isTrue()
                RulesEngine.applyMove(board, move)
            }
        assertThat(finalBoard.pegCount).isEqualTo(SOLVED_PEG_COUNT)
    }

    @Test
    fun loadChallengeClampsNonPositiveLevelToLevelOne() {
        // A level number below 1 must not crash — it clamps to level 1 at the data
        // boundary (T-03-07 / V5) and returns the same board as loadChallenge(1).
        val clamped = repository().loadChallenge(0)
        val levelOne = repository().loadChallenge(1)

        assertThat(clamped.initialBoard).isEqualTo(levelOne.initialBoard)
        assertThat(clamped.solution).isEqualTo(levelOne.solution)
    }

    private companion object {
        const val EXPECTED_CATALOG_SIZE = 5
        const val ENGLISH33_HOLE_COUNT = 33
        const val SOLVED_PEG_COUNT = 1
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34
