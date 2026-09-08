package com.app.pegsolitaire.data

import android.database.sqlite.SQLiteException
import androidx.room.Room
import com.app.pegsolitaire.data.db.LevelResultDao
import com.app.pegsolitaire.data.db.LevelResultEntity
import com.app.pegsolitaire.data.db.PegSolitaireDatabase
import com.app.pegsolitaire.data.db.PlaySessionDao
import com.app.pegsolitaire.data.db.ResumeDao
import com.app.pegsolitaire.data.db.ResumeSnapshotEntity
import com.app.pegsolitaire.data.resume.ResumeSnapshotDto
import com.app.pegsolitaire.data.resume.SnapshotMapper
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.domain.telemetry.SolveTelemetry
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Coverage of [DefaultProgressRepository]'s behaviour when the database itself fails:
 * a corrupt SQLite file or a disk-I/O error raises [SQLiteException] from the query or
 * write, *before* any blob guard can run.
 *
 * Two contracts are locked. Reads (T-05-10): "failure is indistinguishable from
 * absence" — a throwing read degrades to exactly what a missing row yields (an empty
 * catalog, no Continue tiles, a `null` snapshot), never a crash. Writes (T-05-13):
 * "best-effort" — a failing write is dropped, reports nothing improved, and leaves the
 * previously stored state fully intact. The fakes delegate to a real in-memory Room
 * database and throw only where the test aims the fault, so everything around the
 * failure keeps its real semantics.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [FAILURE_ROBOLECTRIC_SDK])
class ProgressRepositoryFailureTest {
    private lateinit var database: PegSolitaireDatabase

    @Before
    fun openInMemoryDatabase() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    PegSolitaireDatabase::class.java,
                ).build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun observeResultsDegradesToAnEmptyListWhenTheDatabaseReadThrows() =
        runTest {
            val repository = repositoryWith(levelResultDao = ThrowingReadLevelResultDao(database.levelResultDao()))

            // T-05-10: the Classic catalog must render as "unplayed", not crash.
            assertThat(repository.observeResults(ProgressPersistenceKeys.MODE_CLASSIC).first()).isEmpty()
        }

    @Test
    fun observeResumeModesDegradesToNoModesWhenTheDatabaseReadThrows() =
        runTest {
            val repository = repositoryWith(resumeDao = ThrowingReadResumeDao(database.resumeDao()))

            // No Continue tiles on Home is the fail-safe shape, never a crash.
            assertThat(repository.observeResumeModes().first()).isEmpty()
        }

    @Test
    fun loadSnapshotFailsSafeToNoResumeWhenTheDatabaseReadThrows() =
        runTest {
            val repository = repositoryWith(resumeDao = ThrowingReadResumeDao(database.resumeDao()))

            // A throwing `get` yields the same `null` a missing row does.
            assertThat(repository.loadSnapshot(ProgressPersistenceKeys.MODE_CLASSIC)).isNull()
        }

    @Test
    fun loadSnapshotStillFailsSafeWhenPurgingACorruptRowThrows() =
        runTest {
            // A corrupt blob triggers the WR-02 purge; here the purge itself fails too.
            database.resumeDao().upsert(
                ResumeSnapshotEntity(
                    mode = ProgressPersistenceKeys.MODE_CLASSIC,
                    jsonBlob = CORRUPT_BLOB,
                    updatedAt = NOW_MS,
                ),
            )
            val repository = repositoryWith(resumeDao = ThrowingClearResumeDao(database.resumeDao()))

            assertThat(repository.loadSnapshot(ProgressPersistenceKeys.MODE_CLASSIC)).isNull()
        }

    @Test
    fun recordResultReportsNoNewBestsInsteadOfCrashingWhenTheReadThrows() =
        runTest {
            val repository = repositoryWith(levelResultDao = ThrowingFindLevelResultDao(database.levelResultDao()))

            val bests =
                repository.recordResult(
                    ProgressPersistenceKeys.MODE_CLASSIC,
                    LEVEL_ID,
                    scoreResult(SEEDED_STARS),
                    SolveTelemetry.NONE,
                    NOW_MS,
                )

            // T-05-13: no record, no cue — and crucially no crash on the Results screen.
            assertThat(bests.any).isFalse()
        }

    @Test
    fun aFailedRecordDropsTheWriteAndLeavesTheStoredBestsIntact() =
        runTest {
            val healthy = repositoryWith()
            healthy.recordResult(
                ProgressPersistenceKeys.MODE_CLASSIC,
                LEVEL_ID,
                scoreResult(SEEDED_STARS),
                SolveTelemetry.NONE,
                NOW_MS,
            )
            val failing = repositoryWith(levelResultDao = ThrowingUpsertLevelResultDao(database.levelResultDao()))

            val bests =
                failing.recordResult(
                    ProgressPersistenceKeys.MODE_CLASSIC,
                    LEVEL_ID,
                    scoreResult(IMPROVED_STARS),
                    SolveTelemetry.NONE,
                    LATER_MS,
                )

            // The failed merge reports "nothing improved" and the prior row survives —
            // a storage fault may lose one play, never the stored bests.
            assertThat(bests.any).isFalse()
            val stored = healthy.observeResults(ProgressPersistenceKeys.MODE_CLASSIC).first().single()
            assertThat(stored.stars).isEqualTo(SEEDED_STARS)
            assertThat(stored.completedAt).isEqualTo(NOW_MS)
        }

    @Test
    fun aFailedSaveDropsTheWriteAndLeavesThePriorSnapshotIntact() =
        runTest {
            val healthy = repositoryWith()
            healthy.saveSnapshot(snapshot(elapsedMs = FIRST_ELAPSED_MS), NOW_MS)
            val failing = repositoryWith(resumeDao = ThrowingUpsertResumeDao(database.resumeDao()))

            failing.saveSnapshot(snapshot(elapsedMs = SECOND_ELAPSED_MS), LATER_MS)

            // Backgrounding the game while the disk is full loses the newest save, not
            // the app — and the previously stored resume point still loads.
            val stored = healthy.loadSnapshot(ProgressPersistenceKeys.MODE_CLASSIC)
            assertThat(stored).isNotNull()
            assertThat(stored!!.elapsedMs).isEqualTo(FIRST_ELAPSED_MS)
        }

    @Test
    fun aFailedClearIsSwallowedInsteadOfCrashing() =
        runTest {
            val healthy = repositoryWith()
            healthy.saveSnapshot(snapshot(elapsedMs = FIRST_ELAPSED_MS), NOW_MS)
            val failing = repositoryWith(resumeDao = ThrowingClearResumeDao(database.resumeDao()))

            failing.clearSnapshot(ProgressPersistenceKeys.MODE_CLASSIC)

            // The delete was dropped, not half-applied: the snapshot is still there.
            assertThat(healthy.loadSnapshot(ProgressPersistenceKeys.MODE_CLASSIC)).isNotNull()
        }

    // --- helpers -------------------------------------------------------------

    private fun repositoryWith(
        levelResultDao: LevelResultDao = database.levelResultDao(),
        resumeDao: ResumeDao = database.resumeDao(),
        playSessionDao: PlaySessionDao = database.playSessionDao(),
    ): ProgressRepository = DefaultProgressRepository(database, levelResultDao, resumeDao, playSessionDao)

    private fun scoreResult(stars: Int): ScoreResult =
        ScoreResult(
            stars = stars,
            score = SCORE,
            pegsRemaining = PEGS_REMAINING,
            pegsCleared = PEGS_CLEARED,
            elapsedSeconds = ELAPSED_SECONDS,
        )

    private fun snapshot(elapsedMs: Long): ResumeSnapshotDto =
        SnapshotMapper.toDto(
            mode = ProgressPersistenceKeys.MODE_CLASSIC,
            levelId = LEVEL_ID,
            initialBoard = Boards.english33Initial,
            moveHistory = emptyList(),
            elapsedMs = elapsedMs,
        )

    /** A [LevelResultDao] whose reads fail like a corrupt/unreadable database file. */
    private class ThrowingReadLevelResultDao(
        private val delegate: LevelResultDao,
    ) : LevelResultDao by delegate {
        override fun observe(mode: String): Flow<List<LevelResultEntity>> = flow { throw SQLiteException(DISK_FAILURE) }
    }

    /** A [ResumeDao] whose reads fail like a corrupt/unreadable database file. */
    private class ThrowingReadResumeDao(
        private val delegate: ResumeDao,
    ) : ResumeDao by delegate {
        override suspend fun get(mode: String): ResumeSnapshotEntity? = throw SQLiteException(DISK_FAILURE)

        override fun observeModes(): Flow<List<String>> = flow { throw SQLiteException(DISK_FAILURE) }
    }

    /** A [ResumeDao] that reads fine but fails on the purge/delete path. */
    private class ThrowingClearResumeDao(
        private val delegate: ResumeDao,
    ) : ResumeDao by delegate {
        override suspend fun clear(mode: String) = throw SQLiteException(DISK_FAILURE)
    }

    /** A [LevelResultDao] whose read-inside-the-merge fails; the write path is real. */
    private class ThrowingFindLevelResultDao(
        private val delegate: LevelResultDao,
    ) : LevelResultDao by delegate {
        override suspend fun find(
            mode: String,
            levelId: String,
        ): LevelResultEntity? = throw SQLiteException(DISK_FAILURE)
    }

    /** A [LevelResultDao] that reads fine but fails on the write, like a full disk. */
    private class ThrowingUpsertLevelResultDao(
        private val delegate: LevelResultDao,
    ) : LevelResultDao by delegate {
        override suspend fun upsert(entity: LevelResultEntity) = throw SQLiteException(DISK_FAILURE)
    }

    /** A [ResumeDao] that reads fine but fails on the write, like a full disk. */
    private class ThrowingUpsertResumeDao(
        private val delegate: ResumeDao,
    ) : ResumeDao by delegate {
        override suspend fun upsert(entity: ResumeSnapshotEntity) = throw SQLiteException(DISK_FAILURE)
    }

    private companion object {
        const val DISK_FAILURE = "disk I/O error"
        const val CORRUPT_BLOB = "}{ not json"
        const val LEVEL_ID = "cross-33"
        const val SEEDED_STARS = 2
        const val IMPROVED_STARS = 3
        const val SCORE = 1_200
        const val PEGS_REMAINING = 1
        const val PEGS_CLEARED = 31
        const val ELAPSED_SECONDS = 90
        const val FIRST_ELAPSED_MS = 45_000L
        const val SECOND_ELAPSED_MS = 90_000L
        const val NOW_MS = 1_700_000_000_000L
        const val LATER_MS = 1_700_000_900_000L
    }
}

private const val FAILURE_ROBOLECTRIC_SDK = 34
