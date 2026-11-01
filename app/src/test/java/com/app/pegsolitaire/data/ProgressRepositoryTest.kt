package com.app.pegsolitaire.data

import android.database.sqlite.SQLiteException
import androidx.room.Room
import app.cash.turbine.test
import com.app.pegsolitaire.data.db.LevelResultDao
import com.app.pegsolitaire.data.db.LevelResultEntity
import com.app.pegsolitaire.data.db.PegSolitaireDatabase
import com.app.pegsolitaire.data.db.PlaySessionDao
import com.app.pegsolitaire.data.db.PlaySessionEntity
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.domain.telemetry.SolveTelemetry
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
        repository =
            DefaultProgressRepository(
                database,
                database.levelResultDao(),
                database.resumeDao(),
                database.playSessionDao(),
            )
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun theFirstResultForALevelIsStoredVerbatimAndIsNewOnEveryField() =
        runTest {
            val bests = repository.recordResult(MODE, LEVEL_ID, scoreResult(), telemetry(), NOW_MS)

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
            repository.recordResult(MODE, LEVEL_ID, scoreResult(), telemetry(), NOW_MS)

            val bests =
                repository.recordResult(
                    MODE,
                    LEVEL_ID,
                    scoreResult(stars = POOR_STARS, score = POOR_SCORE, elapsedSeconds = SLOW_SECONDS),
                    telemetry(),
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
            repository.recordResult(MODE, LEVEL_ID, scoreResult(), telemetry(), NOW_MS)

            val bests =
                repository.recordResult(
                    MODE,
                    LEVEL_ID,
                    scoreResult(stars = POOR_STARS, score = GREAT_SCORE, elapsedSeconds = SLOW_SECONDS),
                    telemetry(),
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
            repository.recordResult(MODE, LEVEL_ID, scoreResult(), telemetry(), NOW_MS)

            val bests =
                repository.recordResult(
                    MODE,
                    LEVEL_ID,
                    scoreResult(stars = GOOD_STARS, score = POOR_SCORE, elapsedSeconds = FAST_SECONDS),
                    telemetry(),
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
                telemetry(),
                NOW_MS,
            )

            val bests = repository.recordResult(MODE, LEVEL_ID, scoreResult(), telemetry(), LATER_MS)

            assertThat(bests).isEqualTo(NewBests(stars = true, time = false, score = false))
            assertThat(storedResults().single().stars).isEqualTo(GOOD_STARS)
        }

    @Test
    fun theSameLevelIdInEachModeIsADistinctRow() =
        runTest {
            repository.recordResult(
                ProgressPersistenceKeys.MODE_CLASSIC,
                SHARED_ID,
                scoreResult(),
                telemetry(),
                NOW_MS,
            )
            repository.recordResult(
                ProgressPersistenceKeys.MODE_CHALLENGE,
                SHARED_ID,
                scoreResult(stars = POOR_STARS),
                telemetry(),
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

                repository.recordResult(MODE, LEVEL_ID, scoreResult(), telemetry(), NOW_MS)

                // D-10: the Classic catalog re-renders its star pips off this emission.
                assertThat(awaitItem().single().levelId).isEqualTo(LEVEL_ID)
                cancelAndIgnoreRemainingEvents()
            }
        }

    // ---- The play_session write (D-08) -----------------------------------------

    @Test
    fun aSolvedPlayWritesBothTheBestRowAndItsTelemetryRow() =
        runTest {
            repository.recordResult(
                MODE,
                LEVEL_ID,
                scoreResult(),
                telemetry(hintsUsed = SOME_HINTS, undosUsed = SOME_UNDOS, lastPegOnTarget = true),
                NOW_MS,
            )

            val stored = storedResults().single()
            assertThat(stored.stars).isEqualTo(GOOD_STARS)

            val session = storedSessions().single()
            assertThat(session.mode).isEqualTo(MODE)
            assertThat(session.levelId).isEqualTo(LEVEL_ID)
            assertThat(session.stars).isEqualTo(GOOD_STARS)
            assertThat(session.score).isEqualTo(GOOD_SCORE)
            assertThat(session.elapsedMs).isEqualTo(GOOD_SECONDS * MILLIS_IN_SECOND)
            assertThat(session.hintsUsed).isEqualTo(SOME_HINTS)
            assertThat(session.undosUsed).isEqualTo(SOME_UNDOS)
            assertThat(session.lastPegOnTarget).isTrue()
            assertThat(session.finishedAt).isEqualTo(NOW_MS)
        }

    @Test
    fun playSessionIsAppendOnlyWhileTheBestRowIsMerged() =
        runTest {
            repository.recordResult(MODE, LEVEL_ID, scoreResult(), telemetry(), NOW_MS)

            repository.recordResult(
                MODE,
                LEVEL_ID,
                scoreResult(stars = POOR_STARS, score = POOR_SCORE, elapsedSeconds = SLOW_SECONDS),
                telemetry(),
                LATER_MS,
            )

            // One merged best, two plays: the session row describes the PLAY and keeps
            // its own worse numbers, the result row describes the BEST.
            assertThat(storedResults()).hasSize(1)
            val sessions = storedSessions()
            assertThat(sessions).hasSize(2)
            assertThat(sessions.last().stars).isEqualTo(POOR_STARS)
            assertThat(sessions.last().score).isEqualTo(POOR_SCORE)
        }

    @Test
    fun finishedAtIsStampedOnEveryPlayEvenWhenCompletedAtDoesNotMove() =
        runTest {
            repository.recordResult(MODE, LEVEL_ID, scoreResult(), telemetry(), NOW_MS)

            repository.recordResult(
                MODE,
                LEVEL_ID,
                scoreResult(stars = POOR_STARS, score = POOR_SCORE, elapsedSeconds = SLOW_SECONDS),
                telemetry(),
                LATER_MS,
            )

            // completedAt means "last improved" and must not move; finishedAt means
            // "this play happened" and is stamped every time.
            assertThat(storedResults().single().completedAt).isEqualTo(NOW_MS)
            assertThat(storedSessions().map { it.finishedAt }).containsExactly(NOW_MS, LATER_MS).inOrder()
        }

    @Test
    fun aFailingSessionInsertRollsTheBestRowBackSoNeitherTableIsWritten() =
        runTest {
            val failing = repositoryWith(playSessionDao = ThrowingInsertPlaySessionDao(database.playSessionDao()))

            val bests = failing.recordResult(MODE, LEVEL_ID, scoreResult(), telemetry(), NOW_MS)

            // D-08: the upsert already ran inside the transaction and must be rolled
            // back, so telemetry can never disagree with the best it belongs to.
            assertThat(bests).isEqualTo(NewBests(stars = false, time = false, score = false))
            assertThat(storedResults()).isEmpty()
            assertThat(storedSessions()).isEmpty()
        }

    @Test
    fun aFailingSessionInsertLeavesAPreviouslyStoredBestUntouched() =
        runTest {
            repository.recordResult(MODE, LEVEL_ID, scoreResult(), telemetry(), NOW_MS)
            val failing = repositoryWith(playSessionDao = ThrowingInsertPlaySessionDao(database.playSessionDao()))

            failing.recordResult(
                MODE,
                LEVEL_ID,
                scoreResult(stars = POOR_STARS, score = GREAT_SCORE, elapsedSeconds = FAST_SECONDS),
                telemetry(),
                LATER_MS,
            )

            // A storage fault may lose one play, never the stored bests.
            val stored = storedResults().single()
            assertThat(stored.stars).isEqualTo(GOOD_STARS)
            assertThat(stored.bestScore).isEqualTo(GOOD_SCORE)
            assertThat(stored.bestTimeMs).isEqualTo(GOOD_SECONDS * MILLIS_IN_SECOND)
            assertThat(stored.completedAt).isEqualTo(NOW_MS)
            assertThat(storedSessions()).hasSize(1)
        }

    @Test
    fun aFailingBestUpsertLeavesNoSessionRowEither() =
        runTest {
            val failing = repositoryWith(levelResultDao = ThrowingUpsertLevelResultDao(database.levelResultDao()))

            val bests = failing.recordResult(MODE, LEVEL_ID, scoreResult(), telemetry(), NOW_MS)

            // The symmetric direction of D-08, and the StorageFailSafe posture is
            // unchanged: report nothing improved, never propagate (T-05-13).
            assertThat(bests.any).isFalse()
            assertThat(storedResults()).isEmpty()
            assertThat(storedSessions()).isEmpty()
        }

    // ---- The Home progress ribbon's single aggregate (§7 R-1/R-2, D-03c) ----

    @Test
    fun theHomeProgressFlowSumsStarsAcrossBothModesAndCountsOnlyClearedClassicBoards() =
        runTest {
            // Classic AND Challenge rows, so both the absent WHERE (stars) and the
            // present one (boards) are pinned by the same fixture.
            storeResult(ProgressPersistenceKeys.MODE_CLASSIC, ClassicLevelIds.ENGLISH33, stars = GOOD_STARS)
            storeResult(ProgressPersistenceKeys.MODE_CLASSIC, ClassicLevelIds.EUROPEAN37, stars = POOR_STARS)
            // A stored row means the board was CLEARED regardless of how well — the
            // cleared predicate binds ProgressConstants.ANY_STARS (0), not 1. A later
            // reader must not "fix" `>= 0` into `>= 1`: that would silently drop this
            // board from a number the player already reads on Home.
            storeResult(ProgressPersistenceKeys.MODE_CLASSIC, ClassicLevelIds.DIAMOND41, stars = NO_STARS)
            storeResult(ProgressPersistenceKeys.MODE_CHALLENGE, CHALLENGE_LEVEL_ID, stars = CHALLENGE_STARS)

            val progress = repository.observeHomeProgress().first()

            assertThat(progress.totalStars)
                .isEqualTo(GOOD_STARS + POOR_STARS + NO_STARS + CHALLENGE_STARS)
            assertThat(progress.classicBoardsCleared).isEqualTo(CLEARED_CLASSIC_BOARDS)
            // Parity with the existing cleared predicate, asserted rather than assumed:
            // the ribbon must not be able to disagree with the achievement aggregate.
            assertThat(progress.classicBoardsCleared)
                .isEqualTo(repository.aggregate().clearedClassicBoards.size)
        }

    @Test
    fun theHomeProgressFlowReEmitsWhenANewResultIsStored() =
        runTest {
            repository.observeHomeProgress().test {
                assertThat(awaitItem().totalStars).isEqualTo(NO_STARS)

                storeResult(ProgressPersistenceKeys.MODE_CHALLENGE, CHALLENGE_LEVEL_ID, stars = CHALLENGE_STARS)

                // Room invalidates the Flow on any write, so the ribbon cannot go stale
                // after a solve — the property `aggregate()` structurally cannot have.
                assertThat(awaitItem().totalStars).isEqualTo(CHALLENGE_STARS)
                cancelAndIgnoreRemainingEvents()
            }
        }

    /** Stores one banked result directly, so a fixture needs no score/telemetry ceremony. */
    private suspend fun storeResult(
        mode: String,
        levelId: String,
        stars: Int,
    ) = database.levelResultDao().upsert(
        LevelResultEntity(
            mode = mode,
            levelId = levelId,
            stars = stars,
            bestTimeMs = GOOD_SECONDS * MILLIS_IN_SECOND,
            bestScore = GOOD_SCORE,
            completedAt = NOW_MS,
        ),
    )

    // ---- Challenge's lower bands make one- and two-star bests real (GDD §6.4, SB-D11) --------

    @Test
    fun aLaterThreeStarClearUpgradesAOneStarBest() =
        runTest {
            val oneStar = scoreResult(stars = POOR_STARS)
            repository.recordResult(CHALLENGE_MODE, CHALLENGE_LEVEL_ID, oneStar, telemetry(), NOW_MS)

            // Slower and lower-scoring, so only the stars improve — and only they report "new best".
            val bests =
                repository.recordResult(
                    CHALLENGE_MODE,
                    CHALLENGE_LEVEL_ID,
                    scoreResult(stars = GOOD_STARS, score = POOR_SCORE, elapsedSeconds = SLOW_SECONDS),
                    telemetry(),
                    LATER_MS,
                )

            assertThat(bests).isEqualTo(NewBests(stars = true, time = false, score = false))
            val stored = storedResults(CHALLENGE_MODE).single()
            assertThat(stored.stars).isEqualTo(GOOD_STARS)
            assertThat(stored.bestScore).isEqualTo(GOOD_SCORE)
            assertThat(stored.bestTimeMs).isEqualTo(GOOD_SECONDS * MILLIS_IN_SECOND)
        }

    @Test
    fun aLaterOneStarClearLeavesAThreeStarBestUntouched() =
        runTest {
            repository.recordResult(CHALLENGE_MODE, CHALLENGE_LEVEL_ID, scoreResult(), telemetry(), NOW_MS)

            // A fast one-star clear sets the time best while the three-star play keeps the stars
            // and the score: the row is a join of bests, not one play (GDD §6.4).
            val bests =
                repository.recordResult(
                    CHALLENGE_MODE,
                    CHALLENGE_LEVEL_ID,
                    scoreResult(stars = POOR_STARS, score = POOR_SCORE, elapsedSeconds = FAST_SECONDS),
                    telemetry(),
                    LATER_MS,
                )

            assertThat(bests).isEqualTo(NewBests(stars = false, time = true, score = false))
            val stored = storedResults(CHALLENGE_MODE).single()
            assertThat(stored.stars).isEqualTo(GOOD_STARS)
            assertThat(stored.bestScore).isEqualTo(GOOD_SCORE)
            assertThat(stored.bestTimeMs).isEqualTo(FAST_SECONDS * MILLIS_IN_SECOND)
        }

    private suspend fun storedResults(mode: String = MODE): List<LevelResult> = repository.observeResults(mode).first()

    private fun storedSessions(): List<PlaySessionEntity> {
        // 12-01's PlaySessionDao is insert-only by design; widening a production DAO for
        // a test's convenience is not a reason to add a read method to it.
        val rows = mutableListOf<PlaySessionEntity>()
        database.openHelper.readableDatabase
            .query("SELECT * FROM ${ProgressPersistenceKeys.TABLE_PLAY_SESSION} ORDER BY id")
            .use { cursor ->
                while (cursor.moveToNext()) {
                    rows +=
                        PlaySessionEntity(
                            id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                            mode = cursor.getString(cursor.getColumnIndexOrThrow(ProgressPersistenceKeys.COLUMN_MODE)),
                            levelId =
                                cursor.getString(
                                    cursor.getColumnIndexOrThrow(ProgressPersistenceKeys.COLUMN_LEVEL_ID),
                                ),
                            stars = cursor.getInt(cursor.getColumnIndexOrThrow(ProgressPersistenceKeys.COLUMN_STARS)),
                            score = cursor.getInt(cursor.getColumnIndexOrThrow("score")),
                            elapsedMs =
                                cursor.getLong(
                                    cursor.getColumnIndexOrThrow(ProgressPersistenceKeys.COLUMN_ELAPSED_MS),
                                ),
                            hintsUsed =
                                cursor.getInt(
                                    cursor.getColumnIndexOrThrow(ProgressPersistenceKeys.COLUMN_HINTS_USED),
                                ),
                            undosUsed =
                                cursor.getInt(
                                    cursor.getColumnIndexOrThrow(ProgressPersistenceKeys.COLUMN_UNDOS_USED),
                                ),
                            lastPegOnTarget =
                                cursor.getInt(
                                    cursor.getColumnIndexOrThrow(ProgressPersistenceKeys.COLUMN_LAST_PEG_ON_TARGET),
                                ) == TRUE_AS_INTEGER,
                            finishedAt = cursor.getLong(cursor.getColumnIndexOrThrow("finishedAt")),
                        )
                }
            }
        return rows
    }

    private fun repositoryWith(
        levelResultDao: LevelResultDao = database.levelResultDao(),
        playSessionDao: PlaySessionDao = database.playSessionDao(),
    ): ProgressRepository = DefaultProgressRepository(database, levelResultDao, database.resumeDao(), playSessionDao)

    private fun telemetry(
        hintsUsed: Int = NO_ASSISTS,
        undosUsed: Int = NO_ASSISTS,
        lastPegOnTarget: Boolean = false,
    ) = SolveTelemetry(hintsUsed = hintsUsed, undosUsed = undosUsed, lastPegOnTarget = lastPegOnTarget)

    /** A [PlaySessionDao] whose insert fails like a full disk; the merge path is real. */
    private class ThrowingInsertPlaySessionDao(
        private val delegate: PlaySessionDao,
    ) : PlaySessionDao by delegate {
        override suspend fun insert(entity: PlaySessionEntity) = throw SQLiteException(DISK_FAILURE)
    }

    /** A [LevelResultDao] whose best-row upsert fails; the session insert is real. */
    private class ThrowingUpsertLevelResultDao(
        private val delegate: LevelResultDao,
    ) : LevelResultDao by delegate {
        override suspend fun upsert(entity: LevelResultEntity) = throw SQLiteException(DISK_FAILURE)
    }

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
        const val CHALLENGE_MODE = ProgressPersistenceKeys.MODE_CHALLENGE
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

        /** A banked board the player cleared without earning a star. */
        const val NO_STARS = 0

        /** The Challenge row's star rating — distinct from every Classic value above. */
        const val CHALLENGE_STARS = 2

        /** A Challenge level id: the level number as text (`ResultsViewModel`'s own form). */
        const val CHALLENGE_LEVEL_ID = "12"

        /** The three Classic rows stored above — a stored row IS a cleared board. */
        const val CLEARED_CLASSIC_BOARDS = 3

        const val NO_ASSISTS = 0
        const val SOME_HINTS = 2
        const val SOME_UNDOS = 1
        const val DISK_FAILURE = "disk I/O error"

        /** Room stores a Kotlin `Boolean` as `INTEGER`, so a raw cursor read compares to 1. */
        const val TRUE_AS_INTEGER = 1
    }
}

private const val PROGRESS_ROBOLECTRIC_SDK = 34
