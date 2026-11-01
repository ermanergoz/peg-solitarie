package com.app.pegsolitaire.data

import androidx.room.Room
import app.cash.turbine.test
import com.app.pegsolitaire.data.db.PegSolitaireDatabase
import com.app.pegsolitaire.data.db.ResumeSnapshotEntity
import com.app.pegsolitaire.data.resume.ResumeSnapshotDto
import com.app.pegsolitaire.data.resume.SnapshotMapper
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.rules.RulesEngine
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
 * Coverage of [DefaultProgressRepository]'s resume surface (PERS-03) over a real
 * in-memory Room database (Robolectric).
 *
 * Two things are proven: the snapshot **round-trips** through the blob column so a
 * restored game rebuilds by replay with its history intact (D-06), and a blob that is
 * corrupt or claims a mode it is not stored under **fails safe to "no resume"** rather
 * than throwing (Security §V5 / T-05-03) — a damaged file may cost the player one
 * in-progress game, never a crash loop on launch.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [RESUME_ROBOLECTRIC_SDK])
class ResumeRepositoryTest {
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
    fun aSavedSnapshotLoadsBackAndRebuildsTheBoardTheyLeft() =
        runTest {
            val played = play(MOVES_PLAYED)
            repository.saveSnapshot(snapshotOf(played.history), NOW_MS)

            val loaded = repository.loadSnapshot(ProgressPersistenceKeys.MODE_CLASSIC)

            assertThat(loaded).isNotNull()
            // The DTO holds arrays, so equality is by its on-disk form, not by ==.
            assertThat(SnapshotMapper.encode(loaded!!)).isEqualTo(SnapshotMapper.encode(snapshotOf(played.history)))
            val restored = SnapshotMapper.fromDto(loaded, RulesEngine)
            assertThat(restored).isNotNull()
            assertThat(restored!!.currentBoard.pegs).isEqualTo(played.currentBoard.pegs)
            assertThat(restored.moveHistory).hasSize(MOVES_PLAYED)
        }

    @Test
    fun savingAgainReplacesThatModesSnapshot() =
        runTest {
            repository.saveSnapshot(snapshotOf(play(MOVES_PLAYED).history), NOW_MS)
            repository.saveSnapshot(snapshotOf(play(1).history), LATER_MS)

            val loaded = repository.loadSnapshot(ProgressPersistenceKeys.MODE_CLASSIC)

            // One live row per mode (D-03): the newer snapshot wins, it does not stack.
            assertThat(loaded!!.moves).hasSize(1)
        }

    @Test
    fun clearingOneModeLeavesTheOtherResumable() =
        runTest {
            val history = play(MOVES_PLAYED).history
            repository.saveSnapshot(snapshotOf(history), NOW_MS)
            repository.saveSnapshot(
                snapshotOf(history, mode = ProgressPersistenceKeys.MODE_CHALLENGE),
                NOW_MS,
            )

            repository.clearSnapshot(ProgressPersistenceKeys.MODE_CLASSIC)

            // D-05: solving/restarting Classic must not evict the Challenge game.
            assertThat(repository.loadSnapshot(ProgressPersistenceKeys.MODE_CLASSIC)).isNull()
            assertThat(repository.loadSnapshot(ProgressPersistenceKeys.MODE_CHALLENGE)).isNotNull()
        }

    @Test
    fun observeResumeModesReflectsTheModesHoldingASnapshot() =
        runTest {
            repository.observeResumeModes().test {
                assertThat(awaitItem()).isEmpty()

                repository.saveSnapshot(snapshotOf(play(MOVES_PLAYED).history), NOW_MS)

                // D-01/D-11: this is what decides which Home "Continue" tiles appear.
                assertThat(awaitItem()).containsExactly(ProgressPersistenceKeys.MODE_CLASSIC)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aCorruptStoredBlobLoadsAsNoResumeInsteadOfThrowing() =
        runTest {
            database.resumeDao().upsert(
                ResumeSnapshotEntity(
                    mode = ProgressPersistenceKeys.MODE_CLASSIC,
                    jsonBlob = "}{ not json",
                    updatedAt = NOW_MS,
                ),
            )

            assertThat(repository.loadSnapshot(ProgressPersistenceKeys.MODE_CLASSIC)).isNull()
            // WR-02: the unusable row is PURGED, not just skipped — otherwise the mode
            // stays listed forever and Home shows a phantom Continue tile.
            assertThat(database.resumeDao().get(ProgressPersistenceKeys.MODE_CLASSIC)).isNull()
        }

    @Test
    fun aBlobClaimingAnotherModeIsRejected() =
        runTest {
            // Tampering: a Challenge payload filed under the Classic key.
            database.resumeDao().upsert(
                ResumeSnapshotEntity(
                    mode = ProgressPersistenceKeys.MODE_CLASSIC,
                    jsonBlob =
                        SnapshotMapper.encode(
                            snapshotOf(play(MOVES_PLAYED).history, mode = ProgressPersistenceKeys.MODE_CHALLENGE),
                        ),
                    updatedAt = NOW_MS,
                ),
            )

            assertThat(repository.loadSnapshot(ProgressPersistenceKeys.MODE_CLASSIC)).isNull()
            // The mismatched row is equally unusable — purged the same way (WR-02).
            assertThat(database.resumeDao().get(ProgressPersistenceKeys.MODE_CLASSIC)).isNull()
        }

    @Test
    fun purgingACorruptBlobDropsItsModeFromTheObservedResumeModes() =
        runTest {
            // The full WR-02 loop: a corrupt Challenge row lists its mode (phantom tile)
            // until a load proves it unusable — then the purge re-emits WITHOUT the mode,
            // so the Continue affordance disappears rather than surviving forever.
            database.resumeDao().upsert(
                ResumeSnapshotEntity(
                    mode = ProgressPersistenceKeys.MODE_CHALLENGE,
                    jsonBlob = "}{ not json",
                    updatedAt = NOW_MS,
                ),
            )

            repository.observeResumeModes().test {
                assertThat(awaitItem()).containsExactly(ProgressPersistenceKeys.MODE_CHALLENGE)

                assertThat(repository.loadSnapshot(ProgressPersistenceKeys.MODE_CHALLENGE)).isNull()

                assertThat(awaitItem()).isEmpty()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aHealthySnapshotIsNeverPurgedByLoading() =
        runTest {
            repository.saveSnapshot(snapshotOf(play(MOVES_PLAYED).history), NOW_MS)

            assertThat(repository.loadSnapshot(ProgressPersistenceKeys.MODE_CLASSIC)).isNotNull()
            assertThat(database.resumeDao().get(ProgressPersistenceKeys.MODE_CLASSIC)).isNotNull()
        }

    @Test
    fun loadingAModeWithNoSnapshotIsNull() =
        runTest {
            assertThat(repository.loadSnapshot(ProgressPersistenceKeys.MODE_CHALLENGE)).isNull()
        }

    // --- helpers -------------------------------------------------------------

    private fun snapshotOf(
        history: List<Move>,
        mode: String = ProgressPersistenceKeys.MODE_CLASSIC,
    ): ResumeSnapshotDto =
        SnapshotMapper.toDto(
            mode = mode,
            levelId = LEVEL_ID,
            initialBoard = Boards.english33Initial,
            moveHistory = history,
            elapsedMs = ELAPSED_MS,
        )

    private fun play(moveCount: Int): PlayedGame {
        var board = Boards.english33Initial
        val history = mutableListOf<Move>()
        repeat(moveCount) {
            val move = RulesEngine.legalMoves(board).first()
            history += move
            board = RulesEngine.applyMove(board, move)
        }
        return PlayedGame(currentBoard = board, history = history)
    }

    private data class PlayedGame(
        val currentBoard: Board,
        val history: List<Move>,
    )

    private companion object {
        const val LEVEL_ID = "cross-33"
        const val MOVES_PLAYED = 4
        const val ELAPSED_MS = 91_000L
        const val NOW_MS = 1_700_000_000_000L
        const val LATER_MS = 1_700_000_900_000L
    }
}

private const val RESUME_ROBOLECTRIC_SDK = 34
