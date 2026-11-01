package com.app.pegsolitaire.data

import android.database.sqlite.SQLiteException
import androidx.room.Room
import com.app.pegsolitaire.data.db.ClassicStanding
import com.app.pegsolitaire.data.db.LevelResultDao
import com.app.pegsolitaire.data.db.LevelResultEntity
import com.app.pegsolitaire.data.db.PegSolitaireDatabase
import com.app.pegsolitaire.data.db.PlaySessionDao
import com.app.pegsolitaire.data.db.PlaySessionEntity
import com.app.pegsolitaire.data.db.PlaySessionFlags
import com.app.pegsolitaire.domain.achievement.AchievementThresholds
import com.app.pegsolitaire.domain.achievement.ClassicBoard
import com.app.pegsolitaire.domain.achievement.ProgressAggregate
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Coverage of [DefaultProgressRepository.aggregate] and
 * [DefaultProgressRepository.classicBests] over a real in-memory Room database
 * (Robolectric — the project has no `androidTest` source set).
 *
 * What this locks, field by field: every [ProgressAggregate] property is produced by SQL
 * rather than an in-memory fold (the ROADMAP Phase 12 note — the Challenge ladder reaches
 * thousands of rows); the ladder is a **numeric** `CAST`, so 17 beats 9 rather than losing
 * to it lexically; the cumulative Challenge score is a `Long` that cannot wrap through
 * `Int.MAX_VALUE` into a negative leaderboard submission (Pitfall 6); and "center finish"
 * means English-33 and **nothing else** (D-01), which is the single most likely predicate
 * to be generalised by accident.
 *
 * Rows are seeded through the DAOs directly rather than through `recordResult`: the READ
 * path is under test here and must not be able to pass because the write path happens to
 * agree with it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [AGGREGATE_ROBOLECTRIC_SDK])
class ProgressAggregateTest {
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
        repository = repositoryWith()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    // ---- Empty history ---------------------------------------------------------

    @Test
    fun anEmptyHistoryIsExactlyTheFreshPlayerAggregate() =
        runTest {
            // Both Challenge scalars must be null, not 0: MAX/SUM over an empty set is
            // SQL NULL, and "nothing banked" is not "banked level 0 for 0 points".
            assertThat(repository.aggregate()).isEqualTo(ProgressAggregate())
        }

    @Test
    fun anEmptyHistoryHasNoClassicBests() =
        runTest {
            assertThat(repository.classicBests()).isEmpty()
        }

    // ---- The Challenge ladder (PGSL-01) ----------------------------------------

    @Test
    fun theChallengeLadderIsTheNumericMaximumNotTheLexicalOne() =
        runTest {
            seedResult(mode = CHALLENGE, levelId = LOW_LEVEL_ID)
            seedResult(mode = CHALLENGE, levelId = HIGH_LEVEL_ID)
            seedResult(mode = CHALLENGE, levelId = MID_LEVEL_ID)

            // A MAX over TEXT would answer "9"; CAST(levelId AS INTEGER) answers 17.
            assertThat(repository.aggregate().highestChallengeLevel).isEqualTo(HIGH_LEVEL)
        }

    @Test
    fun aClassicRowCannotChangeTheChallengeLadder() =
        runTest {
            seedResult(mode = CHALLENGE, levelId = LOW_LEVEL_ID)
            // A non-numeric Classic id sitting in the same table must neither raise the
            // ladder nor make the CAST throw.
            seedResult(mode = CLASSIC, levelId = ClassicLevelIds.ENGLISH33)

            assertThat(repository.aggregate().highestChallengeLevel).isEqualTo(LOW_LEVEL)
        }

    // ---- The cumulative Challenge score (PGSL-02, D-07) ------------------------

    @Test
    fun theChallengeScoreTotalSumsOnlyChallengeRows() =
        runTest {
            seedResult(mode = CHALLENGE, levelId = LOW_LEVEL_ID, bestScore = GOOD_SCORE)
            seedResult(mode = CHALLENGE, levelId = MID_LEVEL_ID, bestScore = GREAT_SCORE)
            seedResult(mode = CLASSIC, levelId = ClassicLevelIds.WIEGLEB45, bestScore = HUGE_CLASSIC_SCORE)

            assertThat(repository.aggregate().challengeScoreTotal)
                .isEqualTo((GOOD_SCORE + GREAT_SCORE).toLong())
        }

    @Test
    fun theChallengeScoreTotalIsALongAndNeverWrapsThroughIntMaxValue() =
        runTest {
            // Pitfall 6: three near-Int.MAX_VALUE bests sum past Int.MAX_VALUE. A wrapped
            // negative here would be submitted verbatim to a public leaderboard.
            seedResult(mode = CHALLENGE, levelId = LOW_LEVEL_ID, bestScore = Int.MAX_VALUE)
            seedResult(mode = CHALLENGE, levelId = MID_LEVEL_ID, bestScore = Int.MAX_VALUE)
            seedResult(mode = CHALLENGE, levelId = HIGH_LEVEL_ID, bestScore = Int.MAX_VALUE)

            val total = repository.aggregate().challengeScoreTotal

            assertThat(total).isEqualTo(EXPECTED_OVERFLOWING_TOTAL)
            assertThat(total).isGreaterThan(Int.MAX_VALUE.toLong())
        }

    // ---- Cross-mode counters (D-02, D-03) --------------------------------------

    @Test
    fun totalStarsSumsAcrossBothModes() =
        runTest {
            seedResult(mode = CLASSIC, levelId = ClassicLevelIds.ENGLISH33, stars = PERFECT_STARS)
            seedResult(mode = CHALLENGE, levelId = LOW_LEVEL_ID, stars = TWO_STARS)

            assertThat(repository.aggregate().totalStars).isEqualTo(PERFECT_STARS + TWO_STARS)
        }

    @Test
    fun solvedLevelCountCountsRowsInBothModes() =
        runTest {
            seedResult(mode = CLASSIC, levelId = ClassicLevelIds.ENGLISH33)
            seedResult(mode = CLASSIC, levelId = ClassicLevelIds.EUROPEAN37)
            seedResult(mode = CHALLENGE, levelId = LOW_LEVEL_ID)

            assertThat(repository.aggregate().solvedLevelCount).isEqualTo(THREE_ROWS)
        }

    @Test
    fun threeStarCountCountsOnlyRowsAtOrAbovePerfectStars() =
        runTest {
            seedResult(mode = CLASSIC, levelId = ClassicLevelIds.ENGLISH33, stars = PERFECT_STARS)
            seedResult(mode = CHALLENGE, levelId = LOW_LEVEL_ID, stars = PERFECT_STARS)
            seedResult(mode = CHALLENGE, levelId = MID_LEVEL_ID, stars = TWO_STARS)

            val aggregate = repository.aggregate()

            assertThat(aggregate.threeStarCount).isEqualTo(TWO_ROWS)
            assertThat(aggregate.solvedLevelCount).isEqualTo(THREE_ROWS)
        }

    // ---- The Classic board sets (D-04, PGSL-03) --------------------------------

    @Test
    fun clearedClassicBoardsContainsEveryStoredClassicBoard() =
        runTest {
            ClassicBoard.entries.forEach { board ->
                seedResult(mode = CLASSIC, levelId = ClassicBoardIds.levelIdFor(board))
            }

            assertThat(repository.aggregate().clearedClassicBoards)
                .containsExactlyElementsIn(ClassicBoard.entries)
        }

    @Test
    fun aChallengeRowAddsNoClassicBoard() =
        runTest {
            seedResult(mode = CHALLENGE, levelId = LOW_LEVEL_ID, stars = PERFECT_STARS)

            val aggregate = repository.aggregate()

            assertThat(aggregate.clearedClassicBoards).isEmpty()
            assertThat(aggregate.perfectClassicBoards).isEmpty()
        }

    @Test
    fun aTwoStarBoardIsClearedButNotPerfect() =
        runTest {
            seedResult(mode = CLASSIC, levelId = ClassicLevelIds.EUROPEAN37, stars = TWO_STARS)

            val aggregate = repository.aggregate()

            assertThat(aggregate.clearedClassicBoards).containsExactly(ClassicBoard.EUROPEAN37)
            assertThat(aggregate.perfectClassicBoards).isEmpty()
        }

    @Test
    fun aThreeStarDiamond41IsPerfect() =
        runTest {
            // D-04: ScoreCalculator bands the top star against the FINISHING board's own
            // minimumPegs, so Diamond-41's proven 2-peg floor earns 3 stars and therefore
            // counts as perfect — no per-board peg carve-out is needed.
            seedResult(mode = CLASSIC, levelId = ClassicLevelIds.DIAMOND41, stars = PERFECT_STARS)

            assertThat(repository.aggregate().perfectClassicBoards)
                .containsExactly(ClassicBoard.DIAMOND41)
        }

    @Test
    fun anUnknownClassicLevelIdIsDroppedFromEverySetAndFromTheBests() =
        runTest {
            seedResult(mode = CLASSIC, levelId = LEGACY_LEVEL_ID, stars = PERFECT_STARS)

            val aggregate = repository.aggregate()

            // A legacy or forged id names no shipped board: drop it, never throw.
            assertThat(aggregate.clearedClassicBoards).isEmpty()
            assertThat(aggregate.perfectClassicBoards).isEmpty()
            assertThat(repository.classicBests()).isEmpty()
        }

    @Test
    fun classicBestsKeysEachStoredBoardByItsEnumAndOmitsUnsolvedOnes() =
        runTest {
            seedResult(mode = CLASSIC, levelId = ClassicLevelIds.ENGLISH33, bestScore = GOOD_SCORE)
            seedResult(mode = CLASSIC, levelId = ClassicLevelIds.WIEGLEB45, bestScore = GREAT_SCORE)
            seedResult(mode = CHALLENGE, levelId = LOW_LEVEL_ID, bestScore = HUGE_CLASSIC_SCORE)

            val bests = repository.classicBests()

            assertThat(bests)
                .containsExactly(
                    ClassicBoard.ENGLISH33,
                    GOOD_SCORE,
                    ClassicBoard.WIEGLEB45,
                    GREAT_SCORE,
                )
        }

    // ---- Telemetry flags (D-01, D-04) ------------------------------------------

    @Test
    fun aHintFreeSolveSetsOnlyTheHintFlag() =
        runTest {
            seedSession(hintsUsed = NO_ASSISTS, undosUsed = SOME_ASSISTS)

            val aggregate = repository.aggregate()

            assertThat(aggregate.anyHintFreeSolve).isTrue()
            assertThat(aggregate.anyUndoFreeSolve).isFalse()
        }

    @Test
    fun anUndoFreeSolveSetsOnlyTheUndoFlag() =
        runTest {
            seedSession(hintsUsed = SOME_ASSISTS, undosUsed = NO_ASSISTS)

            val aggregate = repository.aggregate()

            assertThat(aggregate.anyUndoFreeSolve).isTrue()
            assertThat(aggregate.anyHintFreeSolve).isFalse()
        }

    @Test
    fun aHintFreeChallengeSolveCountsBecauseThePredicateIsModeAgnostic() =
        runTest {
            // D-01: a mode filter here would be a silent scope reduction.
            seedSession(mode = CHALLENGE, levelId = LOW_LEVEL_ID, hintsUsed = NO_ASSISTS, undosUsed = NO_ASSISTS)

            val aggregate = repository.aggregate()

            assertThat(aggregate.anyHintFreeSolve).isTrue()
            assertThat(aggregate.anyUndoFreeSolve).isTrue()
        }

    // ---- Center finish: English-33 and nothing else (D-01) ---------------------

    @Test
    fun anEnglish33TargetFinishSetsTheCenterFlag() =
        runTest {
            seedSession(levelId = ClassicLevelIds.ENGLISH33, lastPegOnTarget = true)

            assertThat(repository.aggregate().anyCenterFinish).isTrue()
        }

    @Test
    fun aTargetFinishOnEuropean37DoesNotSetTheCenterFlag() =
        runTest {
            seedSession(levelId = ClassicLevelIds.EUROPEAN37, lastPegOnTarget = true)

            assertThat(repository.aggregate().anyCenterFinish).isFalse()
        }

    @Test
    fun aTargetFinishOnDiamond41DoesNotSetTheCenterFlag() =
        runTest {
            seedSession(levelId = ClassicLevelIds.DIAMOND41, lastPegOnTarget = true)

            assertThat(repository.aggregate().anyCenterFinish).isFalse()
        }

    @Test
    fun aTargetFinishOnAChallengeLevelDoesNotSetTheCenterFlag() =
        runTest {
            seedSession(mode = CHALLENGE, levelId = LOW_LEVEL_ID, lastPegOnTarget = true)

            assertThat(repository.aggregate().anyCenterFinish).isFalse()
        }

    @Test
    fun anEnglish33SolveThatMissedTheTargetDoesNotSetTheCenterFlag() =
        runTest {
            seedSession(levelId = ClassicLevelIds.ENGLISH33, lastPegOnTarget = false)

            assertThat(repository.aggregate().anyCenterFinish).isFalse()
        }

    // ---- Speed solve: a strict bound (D-04) ------------------------------------

    @Test
    fun aSolveJustUnderTheSpeedLimitSetsTheSpeedFlag() =
        runTest {
            seedSession(elapsedMs = AchievementThresholds.SPEED_SOLVE_MAX_ELAPSED_MS - 1)

            assertThat(repository.aggregate().anySpeedSolve).isTrue()
        }

    @Test
    fun aSolveExactlyAtTheSpeedLimitDoesNotSetTheSpeedFlag() =
        runTest {
            // "under 60 seconds" is strict, so exactly the bound does not qualify.
            seedSession(elapsedMs = AchievementThresholds.SPEED_SOLVE_MAX_ELAPSED_MS)

            assertThat(repository.aggregate().anySpeedSolve).isFalse()
        }

    // ---- Fail-safe reads (T-05-10 / T-12-34) -----------------------------------

    @Test
    fun aggregateFailsSafeToAFreshPlayerAggregateWhenTheFlagsQueryThrows() =
        runTest {
            seedResult(mode = CHALLENGE, levelId = HIGH_LEVEL_ID, stars = PERFECT_STARS)
            val failing = repositoryWith(playSessionDao = ThrowingFlagsPlaySessionDao(database.playSessionDao()))

            // A corrupt database submits NOTHING rather than crashing Results or
            // submitting a half-read aggregate.
            assertThat(failing.aggregate()).isEqualTo(ProgressAggregate())
        }

    @Test
    fun classicBestsFailsSafeToAnEmptyMapWhenTheStandingsQueryThrows() =
        runTest {
            val failing =
                DefaultProgressRepository(
                    database,
                    ThrowingStandingsLevelResultDao(database.levelResultDao()),
                    database.resumeDao(),
                    database.playSessionDao(),
                )

            assertThat(failing.classicBests()).isEmpty()
        }

    // ---- Helpers ---------------------------------------------------------------

    private fun repositoryWith(playSessionDao: PlaySessionDao = database.playSessionDao()): ProgressRepository =
        DefaultProgressRepository(database, database.levelResultDao(), database.resumeDao(), playSessionDao)

    private suspend fun seedResult(
        mode: String,
        levelId: String,
        stars: Int = PERFECT_STARS,
        bestScore: Int = GOOD_SCORE,
        bestTimeMs: Long = GOOD_TIME_MS,
        completedAt: Long = NOW_MS,
    ) {
        database.levelResultDao().upsert(
            LevelResultEntity(
                mode = mode,
                levelId = levelId,
                stars = stars,
                bestTimeMs = bestTimeMs,
                bestScore = bestScore,
                completedAt = completedAt,
            ),
        )
    }

    private suspend fun seedSession(
        mode: String = CLASSIC,
        levelId: String = ClassicLevelIds.ENGLISH33,
        stars: Int = PERFECT_STARS,
        score: Int = GOOD_SCORE,
        elapsedMs: Long = GOOD_TIME_MS,
        hintsUsed: Int = SOME_ASSISTS,
        undosUsed: Int = SOME_ASSISTS,
        lastPegOnTarget: Boolean = false,
        finishedAt: Long = NOW_MS,
    ) {
        database.playSessionDao().insert(
            PlaySessionEntity(
                mode = mode,
                levelId = levelId,
                stars = stars,
                score = score,
                elapsedMs = elapsedMs,
                hintsUsed = hintsUsed,
                undosUsed = undosUsed,
                lastPegOnTarget = lastPegOnTarget,
                finishedAt = finishedAt,
            ),
        )
    }

    /** A [PlaySessionDao] whose projection read fails like a corrupt database file. */
    private class ThrowingFlagsPlaySessionDao(
        private val delegate: PlaySessionDao,
    ) : PlaySessionDao by delegate {
        override suspend fun flags(
            classicMode: String,
            english33Id: String,
            speedLimitMs: Long,
        ): PlaySessionFlags = throw SQLiteException(DISK_FAILURE)
    }

    /** A [LevelResultDao] whose per-board standings read fails like a corrupt file. */
    private class ThrowingStandingsLevelResultDao(
        private val delegate: LevelResultDao,
    ) : LevelResultDao by delegate {
        override suspend fun classicStandings(classicMode: String): List<ClassicStanding> =
            throw SQLiteException(DISK_FAILURE)
    }

    private companion object {
        const val CLASSIC = ProgressPersistenceKeys.MODE_CLASSIC
        const val CHALLENGE = ProgressPersistenceKeys.MODE_CHALLENGE
        const val DISK_FAILURE = "disk I/O error"

        /** A legacy/forged Classic id that names no shipped board. */
        const val LEGACY_LEVEL_ID = "cross-33"

        const val LOW_LEVEL = 3
        const val MID_LEVEL = 9
        const val HIGH_LEVEL = 17
        const val LOW_LEVEL_ID = "3"
        const val MID_LEVEL_ID = "9"
        const val HIGH_LEVEL_ID = "17"

        const val PERFECT_STARS = AchievementThresholds.PERFECT_STARS
        const val TWO_STARS = 2
        const val TWO_ROWS = 2
        const val THREE_ROWS = 3

        const val GOOD_SCORE = 1_200
        const val GREAT_SCORE = 1_800
        const val HUGE_CLASSIC_SCORE = 999_999
        const val GOOD_TIME_MS = 90_000L
        const val NOW_MS = 1_700_000_000_000L

        const val NO_ASSISTS = 0
        const val SOME_ASSISTS = 2

        /** Three `Int.MAX_VALUE` bests, which only a `Long` can hold (Pitfall 6). */
        const val OVERFLOWING_ROW_COUNT = 3
        val EXPECTED_OVERFLOWING_TOTAL: Long = Int.MAX_VALUE.toLong() * OVERFLOWING_ROW_COUNT
    }
}

private const val AGGREGATE_ROBOLECTRIC_SDK = 34
