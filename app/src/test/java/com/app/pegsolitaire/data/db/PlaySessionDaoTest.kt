package com.app.pegsolitaire.data.db

import androidx.room.Room
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.domain.achievement.AchievementThresholds
import com.app.pegsolitaire.domain.scoring.ChallengeClearRule
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
 * The skill predicates of [PlaySessionDao.flags] over a real in-memory Room database (Robolectric —
 * the project has no `androidTest` source set).
 *
 * Since 3.0.0 a Challenge clear at one or two stars is a recorded play (docs/gdd.md R5), so the
 * three skill one-shots — HINT_FREE, UNDO_FREE and SPEED_SOLVE — require a three-star play, or
 * three quick jumps to a one-star clear of level 1 would unlock them (GD-SB-05, §11.2). Classic
 * rows are always three stars (R4), so Classic is unchanged by construction.
 *
 * Every fixture is the GDD's SB-V6 play: hint-free, undo-free, twenty seconds, varying only its
 * stars, so each test isolates the star requirement of one predicate.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [PLAY_SESSION_ROBOLECTRIC_SDK])
class PlaySessionDaoTest {
    private lateinit var database: PegSolitaireDatabase
    private lateinit var dao: PlaySessionDao

    @Before
    fun openInMemoryDatabase() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    PegSolitaireDatabase::class.java,
                ).build()
        dao = database.playSessionDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun aOneStarHintFreeClearDoesNotCountAsAHintFreeSolve() =
        runTest {
            dao.insert(sb6Play(stars = ONE_STAR))

            assertThat(flags().anyHintFreeSolve).isFalse()
        }

    @Test
    fun aThreeStarHintFreeClearCountsAsAHintFreeSolve() =
        runTest {
            dao.insert(sb6Play(stars = THREE_STARS))

            assertThat(flags().anyHintFreeSolve).isTrue()
        }

    @Test
    fun aOneStarClearUnderSixtySecondsDoesNotCountAsASpeedSolve() =
        runTest {
            dao.insert(sb6Play(stars = ONE_STAR))

            assertThat(flags().anySpeedSolve).isFalse()
        }

    @Test
    fun aOneStarUndoFreeClearDoesNotCountAsAnUndoFreeSolve() =
        runTest {
            dao.insert(sb6Play(stars = ONE_STAR))

            assertThat(flags().anyUndoFreeSolve).isFalse()
        }

    @Test
    fun aThreeStarHintFreeUndoFreeTwentySecondClearEarnsAllThreeSkillFlags() =
        runTest {
            // SB-V6's other half; a two-star play beside it changes nothing in either direction.
            dao.insert(sb6Play(stars = TWO_STARS))
            dao.insert(sb6Play(stars = THREE_STARS))

            val flags = flags()
            assertThat(flags.anyHintFreeSolve).isTrue()
            assertThat(flags.anyUndoFreeSolve).isTrue()
            assertThat(flags.anySpeedSolve).isTrue()
        }

    private suspend fun flags(): PlaySessionFlags =
        dao.flags(
            classicMode = ProgressPersistenceKeys.MODE_CLASSIC,
            english33Id = ClassicLevelIds.ENGLISH33,
            speedLimitMs = AchievementThresholds.SPEED_SOLVE_MAX_ELAPSED_MS,
            perfectStars = ChallengeClearRule.PERFECT_STARS,
        )

    /** A hint-free, undo-free, twenty-second Challenge play at [stars] (GDD SB-V6). */
    private fun sb6Play(stars: Int): PlaySessionEntity =
        PlaySessionEntity(
            mode = ProgressPersistenceKeys.MODE_CHALLENGE,
            levelId = CHALLENGE_LEVEL_ID,
            stars = stars,
            score = SAMPLE_SCORE,
            elapsedMs = TWENTY_SECONDS_MS,
            hintsUsed = NO_ASSISTS,
            undosUsed = NO_ASSISTS,
            lastPegOnTarget = false,
            finishedAt = NOW_MS,
        )

    private companion object {
        const val ONE_STAR = 1
        const val TWO_STARS = 2
        const val THREE_STARS = 3
        const val CHALLENGE_LEVEL_ID = "1"
        const val SAMPLE_SCORE = 3_100
        const val TWENTY_SECONDS_MS = 20_000L
        const val NO_ASSISTS = 0
        const val NOW_MS = 1_700_000_000_000L
    }
}

private const val PLAY_SESSION_ROBOLECTRIC_SDK = 34
