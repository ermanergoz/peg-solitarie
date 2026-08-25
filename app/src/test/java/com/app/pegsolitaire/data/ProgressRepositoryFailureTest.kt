package com.app.pegsolitaire.data

import android.database.sqlite.SQLiteException
import androidx.room.Room
import com.app.pegsolitaire.data.db.LevelResultDao
import com.app.pegsolitaire.data.db.LevelResultEntity
import com.app.pegsolitaire.data.db.PegSolitaireDatabase
import com.app.pegsolitaire.data.db.ResumeDao
import com.app.pegsolitaire.data.db.ResumeSnapshotEntity
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
 * Coverage of [DefaultProgressRepository]'s behaviour when the database itself fails
 * (T-05-10): a corrupt SQLite file or a disk-I/O error raises [SQLiteException] from the
 * query, *before* any blob guard can run.
 *
 * The contract locked here is "failure is indistinguishable from absence": a throwing
 * read degrades to exactly what a missing row yields — an empty catalog, no Continue
 * tiles, a `null` snapshot — never a crash. The fakes delegate to a real in-memory Room
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

    // --- helpers -------------------------------------------------------------

    private fun repositoryWith(
        levelResultDao: LevelResultDao = database.levelResultDao(),
        resumeDao: ResumeDao = database.resumeDao(),
    ): ProgressRepository = DefaultProgressRepository(database, levelResultDao, resumeDao)

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

    private companion object {
        const val DISK_FAILURE = "disk I/O error"
        const val CORRUPT_BLOB = "}{ not json"
        const val NOW_MS = 1_700_000_000_000L
    }
}

private const val FAILURE_ROBOLECTRIC_SDK = 34
