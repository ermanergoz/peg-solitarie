package com.app.pegsolitaire.data

import android.database.sqlite.SQLiteException
import androidx.room.Room
import com.app.pegsolitaire.data.db.LevelResultDao
import com.app.pegsolitaire.data.db.LevelResultEntity
import com.app.pegsolitaire.data.db.PegSolitaireDatabase
import com.app.pegsolitaire.data.db.PlaySessionEntity
import com.app.pegsolitaire.data.db.ResumeSnapshotEntity
import com.app.pegsolitaire.domain.progress.CloudProgress
import com.app.pegsolitaire.domain.progress.LevelBest
import com.app.pegsolitaire.domain.progress.LevelKey
import com.app.pegsolitaire.domain.progress.ProgressMergeLimits
import com.app.pegsolitaire.domain.progress.ProgressMode
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
 * Coverage of the cloud-save half of [DefaultProgressRepository]: [ProgressRepository.exportProgress],
 * [ProgressRepository.importProgress] and [ProgressRepository.resetAllProgress] (CLOUD-01, CLOUD-02,
 * CLOUD-04).
 *
 * Runs against a real in-memory Room database under Robolectric, the same harness
 * [ProgressRepositoryTest] uses — the contracts under test are about what actually lands in SQLite,
 * so a mocked DAO would assert nothing worth asserting.
 *
 * Three of these tests are **negative**, and they are the reason this file exists rather than a few
 * extra cases bolted onto [ProgressRepositoryTest]:
 *  - an import leaves BOTH resume snapshots byte-identical (CLOUD-04) — the in-progress board never
 *    leaves the device and is never destroyed by a cloud event;
 *  - an import inserts **zero** `play_session` rows (D-06) — fabricating telemetry for plays that
 *    never happened on this device would silently grant the three skill achievements;
 *  - a throwing DAO read aborts the whole import rather than half-applying it (CLOUD-05), so a
 *    stale remote row can never clobber a better local best on the way through a storage fault.
 *
 * The fourth structural guarantee — that an import can never fire "New best!" or the Results
 * celebration — is carried by the *type*: [ProgressRepository.importProgress] returns
 * [ImportOutcome] and there is no conversion to [NewBests] anywhere, so no test can reach the
 * celebration path because no production call site could either.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [IMPORT_ROBOLECTRIC_SDK])
class ProgressImportTest {
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

    // ---- export (CLOUD-01) ------------------------------------------------------

    @Test
    fun exportCarriesEveryStoredRowTheSuppliedLadderAndThePlayedTime() =
        runTest {
            seedResult(ProgressPersistenceKeys.MODE_CLASSIC, CLASSIC_ID, stars = GOOD_STARS)
            seedResult(ProgressPersistenceKeys.MODE_CHALLENGE, CHALLENGE_ID, stars = POOR_STARS)
            seedPlaySession(elapsedMs = FAST_MS)
            seedPlaySession(elapsedMs = SLOW_MS)

            val exported = repository.exportProgress(LADDER)

            assertThat(exported.ladder).isEqualTo(LADDER)
            // D-10: played time is the SUM over solved plays, which is what the table holds.
            assertThat(exported.playedTimeMs).isEqualTo(FAST_MS + SLOW_MS)
            assertThat(exported.bests).hasSize(TWO_ROWS)
            assertThat(exported.bests[LevelKey(ProgressMode.CLASSIC, CLASSIC_ID)])
                .isEqualTo(LevelBest(stars = GOOD_STARS, bestTimeMs = GOOD_MS, bestScore = GOOD_SCORE))
            assertThat(exported.bests[LevelKey(ProgressMode.CHALLENGE, CHALLENGE_ID)]?.stars)
                .isEqualTo(POOR_STARS)
        }

    @Test
    fun exportOfAnEmptyDatabaseHasNoBestsAndNoPlayedTime() =
        runTest {
            val exported = repository.exportProgress(LADDER)

            assertThat(exported.bests).isEmpty()
            // COALESCE, not null: "never played" is zero, not an absent value.
            assertThat(exported.playedTimeMs).isEqualTo(ProgressMergeLimits.NO_PLAYED_TIME)
            assertThat(exported.ladder).isEqualTo(LADDER)
        }

    @Test
    fun exportDropsARowWhoseStoredModeNamesNeitherHalfOfTheGame() =
        runTest {
            seedResult(ProgressPersistenceKeys.MODE_CLASSIC, CLASSIC_ID, stars = GOOD_STARS)
            seedResult(UNKNOWN_MODE, CLASSIC_ID, stars = GOOD_STARS)

            // The same totality argument ClassicBoardIds.boardFor makes: a legacy or forged
            // row is dropped from the payload, never fatal to the whole export.
            assertThat(repository.exportProgress(LADDER).bests).hasSize(ONE_ROW)
        }

    // ---- import (CLOUD-01 / CLOUD-02) -------------------------------------------

    @Test
    fun importWritesArrivingRowsAndTheCatalogFlowPublishesThemWithNoOtherCall() =
        runTest {
            val outcome = repository.importProgress(remoteWith(CLASSIC_ID to greatBest()), NOW_MS)

            assertThat(outcome.changed).isTrue()
            assertThat(outcome.rowsWritten).isEqualTo(ONE_ROW)
            // D-03: the silent restore is visible because the existing Flow re-emits — no
            // UI code and no second repository call is involved.
            val published = repository.observeResults(ProgressPersistenceKeys.MODE_CLASSIC).first().single()
            assertThat(published.levelId).isEqualTo(CLASSIC_ID)
            assertThat(published.stars).isEqualTo(GREAT_STARS)
            assertThat(published.bestScore).isEqualTo(GREAT_SCORE)
            assertThat(published.bestTimeMs).isEqualTo(FAST_MS)
        }

    @Test
    fun importTakesTheUnionFieldByFieldAndNeverRegressesABetterLocalBest() =
        runTest {
            seedResult(
                ProgressPersistenceKeys.MODE_CLASSIC,
                CLASSIC_ID,
                stars = GREAT_STARS,
                bestTimeMs = FAST_MS,
                bestScore = POOR_SCORE,
            )

            // A remote row that is worse on stars and time but better on score (D-07).
            repository.importProgress(
                remoteWith(
                    CLASSIC_ID to LevelBest(stars = POOR_STARS, bestTimeMs = SLOW_MS, bestScore = GREAT_SCORE),
                ),
                NOW_MS,
            )

            val stored = storedRow(ProgressPersistenceKeys.MODE_CLASSIC, CLASSIC_ID)
            assertThat(stored.stars).isEqualTo(GREAT_STARS)
            assertThat(stored.bestTimeMs).isEqualTo(FAST_MS)
            assertThat(stored.bestScore).isEqualTo(GREAT_SCORE)
        }

    @Test
    fun anEntirelyWorseRemotePayloadChangesNothingAtAll() =
        runTest {
            seedResult(
                ProgressPersistenceKeys.MODE_CLASSIC,
                CLASSIC_ID,
                stars = GREAT_STARS,
                bestTimeMs = FAST_MS,
                bestScore = GREAT_SCORE,
            )
            val before = storedRow(ProgressPersistenceKeys.MODE_CLASSIC, CLASSIC_ID)

            val outcome =
                repository.importProgress(
                    remoteWith(
                        CLASSIC_ID to LevelBest(stars = POOR_STARS, bestTimeMs = SLOW_MS, bestScore = POOR_SCORE),
                    ),
                    LATER_MS,
                )

            assertThat(outcome).isEqualTo(ImportOutcome(changed = false, rowsWritten = NO_ROWS))
            assertThat(storedRow(ProgressPersistenceKeys.MODE_CLASSIC, CLASSIC_ID)).isEqualTo(before)
        }

    @Test
    fun aSecondImportOfTheSamePayloadIsAnIdempotentNoOp() =
        runTest {
            seedResult(ProgressPersistenceKeys.MODE_CLASSIC, CLASSIC_ID, stars = POOR_STARS)
            val remote = remoteWith(CLASSIC_ID to greatBest(), CHALLENGE_ID to greatBest())

            val first = repository.importProgress(remote, NOW_MS)
            val afterFirst = storedRows()

            val second = repository.importProgress(remote, LATER_MS)

            assertThat(first.changed).isTrue()
            // CLOUD-02: the merge is idempotent, so a replayed delivery is free.
            assertThat(second).isEqualTo(ImportOutcome(changed = false, rowsWritten = NO_ROWS))
            assertThat(storedRows()).isEqualTo(afterFirst)
        }

    @Test
    fun importKeepsAnExistingCompletedAtAndStampsNowOnANewlyArrivingRow() =
        runTest {
            seedResult(ProgressPersistenceKeys.MODE_CLASSIC, CLASSIC_ID, stars = POOR_STARS, completedAt = EARLIER_MS)

            repository.importProgress(
                remoteWith(CLASSIC_ID to greatBest(), CHALLENGE_ID to greatBest()),
                NOW_MS,
            )

            // completedAt means "last improved on THIS device"; it is not merged and not
            // synced, so an existing row keeps its own and only a new row is stamped.
            assertThat(storedRow(ProgressPersistenceKeys.MODE_CLASSIC, CLASSIC_ID).completedAt)
                .isEqualTo(EARLIER_MS)
            assertThat(storedRow(ProgressPersistenceKeys.MODE_CHALLENGE, CHALLENGE_ID).completedAt)
                .isEqualTo(NOW_MS)
        }

    // ---- the two things an import must never do (CLOUD-04 / D-06) ----------------

    @Test
    fun importLeavesBothResumeSnapshotsByteIdentical() =
        runTest {
            seedSnapshot(ProgressPersistenceKeys.MODE_CLASSIC, CLASSIC_BLOB)
            seedSnapshot(ProgressPersistenceKeys.MODE_CHALLENGE, CHALLENGE_BLOB)
            val classicBefore = database.resumeDao().get(ProgressPersistenceKeys.MODE_CLASSIC)
            val challengeBefore = database.resumeDao().get(ProgressPersistenceKeys.MODE_CHALLENGE)

            val outcome =
                repository.importProgress(
                    remoteWith(CLASSIC_ID to greatBest(), CHALLENGE_ID to greatBest()),
                    NOW_MS,
                )

            // CLOUD-04 / D-04: several level rows changed underneath a live Continue, and
            // both in-progress games are still exactly where the player left them.
            assertThat(outcome.rowsWritten).isEqualTo(TWO_ROWS)
            assertThat(database.resumeDao().get(ProgressPersistenceKeys.MODE_CLASSIC)).isEqualTo(classicBefore)
            assertThat(database.resumeDao().get(ProgressPersistenceKeys.MODE_CHALLENGE)).isEqualTo(challengeBefore)
        }

    @Test
    fun importInsertsNoPlaySessionRow() =
        runTest {
            seedPlaySession(elapsedMs = FAST_MS)
            val before = playSessionCount()

            repository.importProgress(remoteWith(CLASSIC_ID to greatBest()), NOW_MS)

            // D-06: telemetry is per-play and stays local. Inventing rows here would earn
            // the hint-free / speed / center-finish achievements for plays that never were.
            assertThat(playSessionCount()).isEqualTo(before)
            assertThat(database.playSessionDao().totalElapsedMs()).isEqualTo(FAST_MS)
        }

    @Test
    fun aThrowingReadAbortsTheWholeImportAndLeavesStorageUntouched() =
        runTest {
            seedResult(
                ProgressPersistenceKeys.MODE_CLASSIC,
                CLASSIC_ID,
                stars = GREAT_STARS,
                bestTimeMs = FAST_MS,
                bestScore = GREAT_SCORE,
            )
            val before = storedRows()
            val failing =
                DefaultProgressRepository(
                    database,
                    ThrowingReadLevelResultDao(database.levelResultDao()),
                    database.resumeDao(),
                    database.playSessionDao(),
                )

            val outcome = failing.importProgress(remoteWith(CLASSIC_ID to poorBest()), NOW_MS)

            // CLOUD-05: fail open. Degrading the failed read to "no local rows" inside the
            // transaction would have let this stale remote row clobber a better local best.
            assertThat(outcome).isEqualTo(ImportOutcome(changed = false, rowsWritten = NO_ROWS))
            assertThat(storedRows()).isEqualTo(before)
        }

    // ---- the D-14 replace path ---------------------------------------------------

    @Test
    fun resetAllProgressEmptiesResultsTelemetryAndBothResumeSnapshots() =
        runTest {
            seedResult(ProgressPersistenceKeys.MODE_CLASSIC, CLASSIC_ID, stars = GOOD_STARS)
            seedResult(ProgressPersistenceKeys.MODE_CHALLENGE, CHALLENGE_ID, stars = GOOD_STARS)
            seedPlaySession(elapsedMs = FAST_MS)
            seedSnapshot(ProgressPersistenceKeys.MODE_CLASSIC, CLASSIC_BLOB)
            seedSnapshot(ProgressPersistenceKeys.MODE_CHALLENGE, CHALLENGE_BLOB)

            repository.resetAllProgress()

            assertThat(storedRows()).isEmpty()
            assertThat(playSessionCount()).isEqualTo(NO_ROWS)
            assertThat(database.resumeDao().get(ProgressPersistenceKeys.MODE_CLASSIC)).isNull()
            assertThat(database.resumeDao().get(ProgressPersistenceKeys.MODE_CHALLENGE)).isNull()
        }

    // ---- helpers -----------------------------------------------------------------

    private fun remoteWith(vararg rows: Pair<String, LevelBest>): CloudProgress =
        CloudProgress(
            ladder = LADDER,
            bests =
                rows.associate { (levelId, best) ->
                    val mode = if (levelId == CHALLENGE_ID) ProgressMode.CHALLENGE else ProgressMode.CLASSIC
                    LevelKey(mode, levelId) to best
                },
        )

    private fun greatBest(): LevelBest = LevelBest(stars = GREAT_STARS, bestTimeMs = FAST_MS, bestScore = GREAT_SCORE)

    private fun poorBest(): LevelBest = LevelBest(stars = POOR_STARS, bestTimeMs = SLOW_MS, bestScore = POOR_SCORE)

    private suspend fun seedResult(
        mode: String,
        levelId: String,
        stars: Int,
        bestTimeMs: Long = GOOD_MS,
        bestScore: Int = GOOD_SCORE,
        completedAt: Long = EARLIER_MS,
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

    private suspend fun seedPlaySession(elapsedMs: Long) {
        database.playSessionDao().insert(
            PlaySessionEntity(
                mode = ProgressPersistenceKeys.MODE_CLASSIC,
                levelId = CLASSIC_ID,
                stars = GOOD_STARS,
                score = GOOD_SCORE,
                elapsedMs = elapsedMs,
                hintsUsed = NO_ROWS,
                undosUsed = NO_ROWS,
                lastPegOnTarget = false,
                finishedAt = EARLIER_MS,
            ),
        )
    }

    private suspend fun seedSnapshot(
        mode: String,
        blob: String,
    ) {
        database.resumeDao().upsert(ResumeSnapshotEntity(mode = mode, jsonBlob = blob, updatedAt = EARLIER_MS))
    }

    private suspend fun storedRows(): List<LevelResultEntity> = database.levelResultDao().all().sortedBy { it.levelId }

    private suspend fun storedRow(
        mode: String,
        levelId: String,
    ): LevelResultEntity = checkNotNull(database.levelResultDao().find(mode, levelId))

    /**
     * The `play_session` row count, read through the open helper: 12-01's DAO is
     * insert-only by design and widening a production DAO for a test's convenience is not
     * a reason to add a read method to it (the same argument [ProgressRepositoryTest] makes).
     */
    private fun playSessionCount(): Int {
        database.openHelper.readableDatabase
            .query("SELECT COUNT(*) FROM ${ProgressPersistenceKeys.TABLE_PLAY_SESSION}")
            .use { cursor ->
                cursor.moveToFirst()
                return cursor.getInt(0)
            }
    }

    /** A [LevelResultDao] whose bulk read fails like a corrupt/unreadable database file. */
    private class ThrowingReadLevelResultDao(
        private val delegate: LevelResultDao,
    ) : LevelResultDao by delegate {
        override suspend fun all(): List<LevelResultEntity> = throw SQLiteException(DISK_FAILURE)
    }

    private companion object {
        const val CLASSIC_ID = "cross-33"
        const val CHALLENGE_ID = "42"
        const val UNKNOWN_MODE = "tournament"

        const val LADDER = 50

        const val GREAT_STARS = 3
        const val GOOD_STARS = 2
        const val POOR_STARS = 1

        const val GREAT_SCORE = 1_800
        const val GOOD_SCORE = 1_200
        const val POOR_SCORE = 400

        const val FAST_MS = 45_000L
        const val GOOD_MS = 90_000L
        const val SLOW_MS = 300_000L

        const val EARLIER_MS = 1_600_000_000_000L
        const val NOW_MS = 1_700_000_000_000L
        const val LATER_MS = 1_700_000_900_000L

        const val CLASSIC_BLOB = "{\"mode\":\"classic\",\"board\":\"never leaves the device\"}"
        const val CHALLENGE_BLOB = "{\"mode\":\"challenge\",\"board\":\"never leaves the device\"}"

        const val NO_ROWS = 0
        const val ONE_ROW = 1
        const val TWO_ROWS = 2

        const val DISK_FAILURE = "disk I/O error"
    }
}

private const val IMPORT_ROBOLECTRIC_SDK = 34
