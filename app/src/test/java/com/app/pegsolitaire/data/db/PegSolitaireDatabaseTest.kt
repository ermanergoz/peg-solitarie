package com.app.pegsolitaire.data.db

import androidx.room.Room
import app.cash.turbine.test
import com.app.pegsolitaire.data.ProgressPersistenceKeys
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
 * JVM (Robolectric) coverage of the Phase-5 Room stack over an in-memory
 * [PegSolitaireDatabase] — the storage substrate every later Phase-5 slice writes
 * through (PERS-01 results, PERS-03 resume).
 *
 * Robolectric supplies a real application `Context` so `Room.inMemoryDatabaseBuilder`
 * can open a throw-away SQLite database on the JVM; the project has no `androidTest`
 * source set, so this is where DAO behaviour is proven. The DAOs are exercised
 * directly (no Hilt graph, no repository) to keep the contract under test narrow:
 * an upserted result round-trips, the `Flow` query observes inserts, and clearing a
 * resume row for one mode really removes it (D-03/D-05).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class PegSolitaireDatabaseTest {
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
    fun upsertedLevelResultIsReadBackByModeAndLevelId() =
        runTest {
            val dao = database.levelResultDao()

            dao.upsert(classicResult(levelId = "cross-33", stars = 3))

            val stored = dao.find(ProgressPersistenceKeys.MODE_CLASSIC, "cross-33")
            assertThat(stored).isNotNull()
            assertThat(stored!!.stars).isEqualTo(3)
            assertThat(stored.bestScore).isEqualTo(1200)
        }

    @Test
    fun upsertReplacesTheRowSharingTheCompositeKey() =
        runTest {
            val dao = database.levelResultDao()

            dao.upsert(classicResult(levelId = "cross-33", stars = 1))
            dao.upsert(classicResult(levelId = "cross-33", stars = 3))

            // Composite PK (mode, levelId) — the second upsert updates, never duplicates.
            val stored = dao.find(ProgressPersistenceKeys.MODE_CLASSIC, "cross-33")
            assertThat(stored!!.stars).isEqualTo(3)
            dao.observe(ProgressPersistenceKeys.MODE_CLASSIC).test {
                assertThat(awaitItem()).hasSize(1)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun observeEmitsTheRowsInsertedForThatMode() =
        runTest {
            val dao = database.levelResultDao()
            dao.upsert(classicResult(levelId = "cross-33", stars = 2))
            // A different mode must never leak into the classic observation (D-03 keying).
            dao.upsert(
                classicResult(levelId = "7", stars = 1)
                    .copy(mode = ProgressPersistenceKeys.MODE_CHALLENGE),
            )

            dao.observe(ProgressPersistenceKeys.MODE_CLASSIC).test {
                val rows = awaitItem()
                assertThat(rows).hasSize(1)
                assertThat(rows.single().levelId).isEqualTo("cross-33")
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun resumeSnapshotRoundTripsAndClearRemovesOnlyThatMode() =
        runTest {
            val dao = database.resumeDao()
            dao.upsert(snapshot(ProgressPersistenceKeys.MODE_CLASSIC))
            dao.upsert(snapshot(ProgressPersistenceKeys.MODE_CHALLENGE))

            assertThat(dao.get(ProgressPersistenceKeys.MODE_CLASSIC)).isNotNull()

            dao.clear(ProgressPersistenceKeys.MODE_CLASSIC)

            // D-05: clearing one mode's snapshot leaves the other mode resumable.
            assertThat(dao.get(ProgressPersistenceKeys.MODE_CLASSIC)).isNull()
            assertThat(dao.get(ProgressPersistenceKeys.MODE_CHALLENGE)).isNotNull()
        }

    @Test
    fun observeModesEmitsEveryModeHoldingASnapshot() =
        runTest {
            val dao = database.resumeDao()
            dao.upsert(snapshot(ProgressPersistenceKeys.MODE_CHALLENGE))

            dao.observeModes().test {
                assertThat(awaitItem()).containsExactly(ProgressPersistenceKeys.MODE_CHALLENGE)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private fun classicResult(
        levelId: String,
        stars: Int,
    ) = LevelResultEntity(
        mode = ProgressPersistenceKeys.MODE_CLASSIC,
        levelId = levelId,
        stars = stars,
        bestTimeMs = 42_000L,
        bestScore = 1200,
        completedAt = 1_700_000_000_000L,
    )

    private fun snapshot(mode: String) =
        ResumeSnapshotEntity(
            mode = mode,
            jsonBlob = """{"mode":"$mode"}""",
            updatedAt = 1_700_000_000_000L,
        )
}

private const val ROBOLECTRIC_SDK = 34
