package com.app.pegsolitaire.data

import androidx.room.Room
import app.cash.turbine.test
import com.app.pegsolitaire.data.db.PegSolitaireDatabase
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Coverage of [DefaultProgressRepository]'s per-level best merge (PERS-01) over a real
 * in-memory Room database (Robolectric — the project has no `androidTest` source set).
 *
 * The contract under test is D-07 + D-09: the three bests are tracked **independently**
 * (max stars, min time, max score), so a replay may only ever improve a field and never
 * regress one — and `recordResult` must *report* which fields improved, because that is
 * what drives the "New best!" cue on the Results screen.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [PROGRESS_ROBOLECTRIC_SDK])
class ProgressRepositoryTest {
    private lateinit var database: PegSolitaireDatabase
    private lateinit var repository: ProgressRepository

    @Before
    fun openInMemoryDatabase() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    PegSolitaireDatabase::class.java,
                ).build()
        repository = DefaultProgressRepository(database, database.levelResultDao(), database.resumeDao())
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun theFirstResultForALevelIsStoredVerbatimAndIsNewOnEveryField() =
        runTest {
            val bests = repository.recordResult(MODE, LEVEL_ID, scoreResult(), NOW_MS)

            assertThat(bests).isEqualTo(NewBests(stars = true, time = true, score = true))
            val stored = storedResults().single()
            assertThat(stored.stars).isEqualTo(GOOD_STARS)
            assertThat(stored.bestScore).isEqualTo(GOOD_SCORE)
            assertThat(stored.bestTimeMs).isEqualTo(GOOD_SECONDS * MILLIS_IN_SECOND)
            assertThat(stored.completedAt).isEqualTo(NOW_MS)
        }

    @Test
    fun aWorseReplayRegressesNothingAndReportsNoNewBests() =
        runTest {
            repository.recordResult(MODE, LEVEL_ID, scoreResult(), NOW_MS)

            val bests =
                repository.recordResult(
                    MODE,
                    LEVEL_ID,
                    scoreResult(stars = POOR_STARS, score = POOR_SCORE, elapsedSeconds = SLOW_SECONDS),
                    LATER_MS,
                )

            assertThat(bests).isEqualTo(NewBests(stars = false, time = false, score = false))
            val stored = storedResults().single()
            assertThat(stored.stars).isEqualTo(GOOD_STARS)
            assertThat(stored.bestScore).isEqualTo(GOOD_SCORE)
            assertThat(stored.bestTimeMs).isEqualTo(GOOD_SECONDS * MILLIS_IN_SECOND)
            // Nothing improved, so the "last improved" stamp must not move (D-09).
            assertThat(stored.completedAt).isEqualTo(NOW_MS)
        }

    @Test
    fun aSlowerRunWithABetterScoreImprovesOnlyTheScore() =
        runTest {
            repository.recordResult(MODE, LEVEL_ID, scoreResult(), NOW_MS)

            val bests =
                repository.recordResult(
                    MODE,
                    LEVEL_ID,
                    scoreResult(stars = POOR_STARS, score = GREAT_SCORE, elapsedSeconds = SLOW_SECONDS),
                    LATER_MS,
                )

            assertThat(bests).isEqualTo(NewBests(stars = false, time = false, score = true))
            val stored = storedResults().single()
            assertThat(stored.bestScore).isEqualTo(GREAT_SCORE)
            // The fast sloppy run keeps its time; the slow perfect run keeps its score (D-07).
            assertThat(stored.bestTimeMs).isEqualTo(GOOD_SECONDS * MILLIS_IN_SECOND)
            assertThat(stored.stars).isEqualTo(GOOD_STARS)
            assertThat(stored.completedAt).isEqualTo(LATER_MS)
        }

    @Test
    fun aFasterRunWithAWorseScoreImprovesOnlyTheTime() =
        runTest {
            repository.recordResult(MODE, LEVEL_ID, scoreResult(), NOW_MS)

            val bests =
                repository.recordResult(
                    MODE,
                    LEVEL_ID,
                    scoreResult(stars = GOOD_STARS, score = POOR_SCORE, elapsedSeconds = FAST_SECONDS),
                    LATER_MS,
                )

            assertThat(bests).isEqualTo(NewBests(stars = false, time = true, score = false))
            val stored = storedResults().single()
            assertThat(stored.bestTimeMs).isEqualTo(FAST_SECONDS * MILLIS_IN_SECOND)
            assertThat(stored.bestScore).isEqualTo(GOOD_SCORE)
        }

    @Test
    fun aBetterStarRatingImprovesOnlyTheStars() =
        runTest {
            repository.recordResult(
                MODE,
                LEVEL_ID,
                scoreResult(stars = POOR_STARS, score = GOOD_SCORE, elapsedSeconds = GOOD_SECONDS),
                NOW_MS,
            )

            val bests = repository.recordResult(MODE, LEVEL_ID, scoreResult(), LATER_MS)

            assertThat(bests).isEqualTo(NewBests(stars = true, time = false, score = false))
            assertThat(storedResults().single().stars).isEqualTo(GOOD_STARS)
        }

    @Test
    fun theSameLevelIdInEachModeIsADistinctRow() =
        runTest {
            repository.recordResult(ProgressPersistenceKeys.MODE_CLASSIC, SHARED_ID, scoreResult(), NOW_MS)
            repository.recordResult(
                ProgressPersistenceKeys.MODE_CHALLENGE,
                SHARED_ID,
                scoreResult(stars = POOR_STARS),
                NOW_MS,
            )

            val classic = storedResults(ProgressPersistenceKeys.MODE_CLASSIC)
            val challenge = storedResults(ProgressPersistenceKeys.MODE_CHALLENGE)
            assertThat(classic.single().stars).isEqualTo(GOOD_STARS)
            assertThat(challenge.single().stars).isEqualTo(POOR_STARS)
            assertThat(classic.single().mode).isEqualTo(ProgressPersistenceKeys.MODE_CLASSIC)
        }

    @Test
    fun observeResultsReEmitsWhenAResultLands() =
        runTest {
            repository.observeResults(MODE).test {
                assertThat(awaitItem()).isEmpty()

                repository.recordResult(MODE, LEVEL_ID, scoreResult(), NOW_MS)

                // D-10: the Classic catalog re-renders its star pips off this emission.
                assertThat(awaitItem().single().levelId).isEqualTo(LEVEL_ID)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private suspend fun storedResults(mode: String = MODE): List<LevelResult> = repository.observeResults(mode).first()

    private fun scoreResult(
        stars: Int = GOOD_STARS,
        score: Int = GOOD_SCORE,
        elapsedSeconds: Int = GOOD_SECONDS,
    ) = ScoreResult(
        stars = stars,
        score = score,
        pegsRemaining = PEGS_REMAINING,
        pegsCleared = PEGS_CLEARED,
        elapsedSeconds = elapsedSeconds,
    )

    private companion object {
        const val MODE = ProgressPersistenceKeys.MODE_CLASSIC
        const val LEVEL_ID = "cross-33"
        const val SHARED_ID = "33"
        const val MILLIS_IN_SECOND = 1_000L

        const val GOOD_STARS = 3
        const val POOR_STARS = 1
        const val GOOD_SCORE = 1_200
        const val GREAT_SCORE = 1_800
        const val POOR_SCORE = 400
        const val GOOD_SECONDS = 90
        const val FAST_SECONDS = 45
        const val SLOW_SECONDS = 300
        const val PEGS_REMAINING = 1
        const val PEGS_CLEARED = 31

        const val NOW_MS = 1_700_000_000_000L
        const val LATER_MS = 1_700_000_900_000L
    }
}

private const val PROGRESS_ROBOLECTRIC_SDK = 34
