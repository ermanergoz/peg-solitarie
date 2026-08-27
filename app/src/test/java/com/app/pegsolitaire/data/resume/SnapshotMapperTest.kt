package com.app.pegsolitaire.data.resume

import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.google.common.truth.Truth.assertThat
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test

/**
 * Pure-JVM coverage of the resume codec (PERS-03) — the load-bearing correctness
 * surface of Phase 5's resume slice.
 *
 * Three contracts are proven here:
 *  - **Round-trip by replay (D-06).** A snapshot stores the *initial* footprint plus
 *    the full move history, so restoring rebuilds the exact current board **and** the
 *    history — which is what keeps undo working after a resume.
 *  - **Never regenerated.** The rebuild folds `RulesEngine.applyMove` over the stored
 *    moves exactly once per move; the `challengeLevel` stamp is provenance only and is
 *    never used to re-run the generator (a spy proves the call count; a deliberately
 *    mismatched stamp proves the footprint on disk wins).
 *  - **Fail-safe deserialization (Security §V5 / T-05-03 / T-05-04 / WARN-02).** A
 *    corrupt, structurally malformed, move-tampered, or coordinate-forged blob yields
 *    `null` ("no resume") rather than throwing or applying a forged move. The WARN-02
 *    cases close the v1.0 advisory that footprint coordinates were bounded only in
 *    *arity*, never in *value* — and the English-33 case proves the bound did not
 *    fail closed on legitimate stored data.
 */
class SnapshotMapperTest {
    @Test
    fun snapshotRoundTripRebuildsTheCurrentBoardAndTheFullMoveHistory() {
        val played = play(MOVES_PLAYED)

        val restored = restore(snapshotOf(played))

        assertThat(restored).isNotNull()
        assertBoardMatches(actual = restored!!.currentBoard, expected = played.currentBoard)
        assertBoardMatches(actual = restored.initialBoard, expected = Boards.english33Initial)
        assertThat(restored.moveHistory).isEqualTo(played.history)
        assertThat(restored.moveHistory).hasSize(MOVES_PLAYED)
        assertThat(restored.elapsedMs).isEqualTo(ELAPSED_MS)
    }

    @Test
    fun restoredHistoryStillSupportsUndoByReplay() {
        val played = play(MOVES_PLAYED)
        val restored = restore(snapshotOf(played))!!

        // Undo is pure replay (GameViewModel.onUndo): drop the last move, fold the rest.
        val undoneHistory = restored.moveHistory.dropLast(1)
        val undoneBoard =
            undoneHistory.fold(restored.initialBoard) { board, move -> RulesEngine.applyMove(board, move) }

        assertBoardMatches(actual = undoneBoard, expected = play(MOVES_PLAYED - 1).currentBoard)
    }

    @Test
    fun fromDtoReplaysEveryStoredMoveExactlyOnceAndNothingElse() {
        val played = play(MOVES_PLAYED)
        val spiedEngine = spyk(RulesEngine)

        SnapshotMapper.fromDto(snapshotOf(played), spiedEngine)

        // Rebuild-by-replay: one applyMove per stored move — no extra board construction.
        verify(exactly = MOVES_PLAYED) { spiedEngine.applyMove(any(), any()) }
    }

    @Test
    fun challengeStampIsProvenanceOnlyAndNeverRegeneratesTheBoard() {
        val played = play(MOVES_PLAYED)
        // A Challenge stamp pointing at a level whose generated board is nothing like this
        // Classic footprint: if the restore regenerated from the level, the board would differ.
        val stamped =
            SnapshotMapper.toDto(
                mode = ProgressPersistenceKeys.MODE_CHALLENGE,
                levelId = STAMPED_LEVEL.toString(),
                initialBoard = Boards.english33Initial,
                moveHistory = played.history,
                elapsedMs = ELAPSED_MS,
                challenge = ChallengeStamp(level = STAMPED_LEVEL, generatorVersion = GENERATOR_VERSION),
            )

        val restored = restore(stamped)

        assertThat(restored).isNotNull()
        assertBoardMatches(actual = restored!!.currentBoard, expected = played.currentBoard)
        assertThat(restored.challenge).isEqualTo(ChallengeStamp(STAMPED_LEVEL, GENERATOR_VERSION))
    }

    @Test
    fun corruptBlobDecodesToNullInsteadOfThrowing() {
        assertThat(SnapshotMapper.decode("not json at all")).isNull()
        assertThat(SnapshotMapper.decode("")).isNull()
        assertThat(SnapshotMapper.decode("""{"mode":"classic"}""")).isNull()
    }

    @Test
    fun unknownKeysInAStoredBlobAreIgnoredForForwardCompatibility() {
        val encoded = SnapshotMapper.encode(snapshotOf(play(MOVES_PLAYED)))
        val withFutureField = encoded.dropLast(1) + ""","aFutureField":42}"""

        assertThat(SnapshotMapper.decode(withFutureField)).isNotNull()
    }

    @Test
    fun aTamperedIllegalMoveYieldsNoResume() {
        val played = play(MOVES_PLAYED)
        val forged =
            snapshotOf(played).copy(
                // A jump from an in-play hole that holds no peg — legal-looking, never legal.
                moves = listOf(intArrayOf(0, 2, 1, 2, 2, 2)),
            )

        assertThat(SnapshotMapper.fromDto(forged, RulesEngine)).isNull()
    }

    @Test
    fun structurallyMalformedTuplesYieldNoResume() {
        val valid = snapshotOf(play(MOVES_PLAYED))

        assertThat(SnapshotMapper.fromDto(valid.copy(moves = listOf(intArrayOf(0, 2, 1, 2))), RulesEngine)).isNull()
        assertThat(SnapshotMapper.fromDto(valid.copy(inPlay = listOf(intArrayOf(3))), RulesEngine)).isNull()
        assertThat(SnapshotMapper.fromDto(valid.copy(inPlay = emptyList()), RulesEngine)).isNull()
    }

    @Test
    fun aPegMaskReachingOutsideTheFootprintYieldsNoResume() {
        val valid = snapshotOf(play(MOVES_PLAYED))

        // English-33 occupies bits 0..32; a bit past the footprint is tampered state.
        assertThat(SnapshotMapper.fromDto(valid.copy(initialPegs = -1L, moves = emptyList()), RulesEngine)).isNull()
    }

    @Test
    fun aHalfSpecifiedOrOffBoardTargetYieldsNoResume() {
        val valid = snapshotOf(play(MOVES_PLAYED))

        assertThat(SnapshotMapper.fromDto(valid.copy(targetCol = null), RulesEngine)).isNull()
        assertThat(
            SnapshotMapper.fromDto(valid.copy(targetRow = OFF_BOARD_INDEX, targetCol = OFF_BOARD_INDEX), RulesEngine),
        ).isNull()
    }

    @Test
    fun aFootprintWithDuplicatePositionsYieldsNoResume() {
        val valid = snapshotOf(play(MOVES_PLAYED))
        val duplicated = valid.inPlay + intArrayOf(valid.inPlay.first()[0], valid.inPlay.first()[1])

        assertThat(SnapshotMapper.fromDto(valid.copy(inPlay = duplicated), RulesEngine)).isNull()
    }

    @Test
    fun aNegativeFootprintCoordinateYieldsNoResume() {
        assertThat(restore(forgedFootprint(intArrayOf(FORGED_NEGATIVE_ROW, FORGED_COL)))).isNull()
    }

    @Test
    fun anAbsurdlyLargeFootprintCoordinateYieldsNoResume() {
        assertThat(restore(forgedFootprint(intArrayOf(FORGED_HUGE_ROW, ORIGIN_COL)))).isNull()
    }

    @Test
    fun aFootprintCoordinateOnePastTheBoundYieldsNoResume() {
        assertThat(restore(forgedFootprint(intArrayOf(FIRST_REJECTED_COORDINATE, ORIGIN_COL)))).isNull()
    }

    @Test
    fun aFootprintCoordinateAtTheInclusiveBoundStillDecodes() {
        val valid = snapshotOf(play(MOVES_PLAYED))
        // (63, 63) sorts after every English-33 hole, so it takes the last bit index and
        // leaves the stored peg mask (bits 0..32) meaning exactly what it meant before.
        val extended = valid.copy(inPlay = valid.inPlay + intArrayOf(MAX_VALID_COORDINATE, MAX_VALID_COORDINATE))

        val restored = restore(extended)

        assertThat(restored).isNotNull()
        assertThat(restored!!.initialBoard.spec.inPlay)
            .contains(Position(row = MAX_VALID_COORDINATE, col = MAX_VALID_COORDINATE))
        assertThat(restored.moveHistory).hasSize(MOVES_PLAYED)
    }

    @Test
    fun anOutOfRangeTargetYieldsNoResume() {
        val valid = snapshotOf(play(MOVES_PLAYED))
        // targetRow/targetCol are read raw and never flow through the bounded position
        // decoder; the pre-existing `target in positions` guard is what rejects them, so
        // this case verifies that guard rather than adding a second bound (T-06-06).
        val forged = valid.copy(targetRow = FIRST_REJECTED_COORDINATE, targetCol = ORIGIN_COL)

        assertThat(restore(forged)).isNull()
    }

    @Test
    fun aPreviouslyValidEnglish33SnapshotStillDecodesToTheSameBoard() {
        val played = play(MOVES_PLAYED)

        val restored = restore(snapshotOf(played))

        // The non-regression proof: every English-33 coordinate is inside the bound, so
        // adding the bound must not have cost a real player their in-progress game.
        assertThat(restored).isNotNull()
        assertBoardMatches(actual = restored!!.initialBoard, expected = Boards.english33Initial)
        assertBoardMatches(actual = restored.currentBoard, expected = played.currentBoard)
        assertThat(restored.moveHistory).isEqualTo(played.history)
    }

    // --- helpers -------------------------------------------------------------

    /**
     * A valid English-33 snapshot with [extra] appended to its footprint and its move
     * history dropped, so footprint validation is the only thing that can reject the
     * blob — an accidentally-illegal replay must not be what makes a WARN-02 case pass.
     */
    private fun forgedFootprint(extra: IntArray): ResumeSnapshotDto {
        val valid = snapshotOf(play(MOVES_PLAYED))
        return valid.copy(inPlay = valid.inPlay + extra, moves = emptyList())
    }

    private fun snapshotOf(played: PlayedGame): ResumeSnapshotDto =
        SnapshotMapper.toDto(
            mode = ProgressPersistenceKeys.MODE_CLASSIC,
            levelId = CLASSIC_LEVEL_ID,
            initialBoard = Boards.english33Initial,
            moveHistory = played.history,
            elapsedMs = ELAPSED_MS,
        )

    /** Full save path: encode to the on-disk blob, decode it back, then rebuild. */
    private fun restore(dto: ResumeSnapshotDto): RestoredGame? =
        SnapshotMapper.decode(SnapshotMapper.encode(dto))?.let { SnapshotMapper.fromDto(it, RulesEngine) }

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

    /**
     * `BoardSpec` is not a data class, so a rebuilt [Board] can never be `==` to the
     * original (its spec is a different instance). Board *state* equality is therefore
     * asserted by content: same footprint, same target, same peg bitmask — and the
     * bitmask is only comparable because `BoardSpec` assigns bit indices by sorting
     * `(row, col)`, which makes the mask reproducible for an identical footprint.
     */
    private fun assertBoardMatches(
        actual: Board,
        expected: Board,
    ) {
        assertThat(actual.spec.inPlay.toSortedSet()).isEqualTo(expected.spec.inPlay.toSortedSet())
        assertThat(actual.spec.target).isEqualTo(expected.spec.target)
        assertThat(actual.pegs).isEqualTo(expected.pegs)
        assertThat(actual.pegCount).isEqualTo(expected.pegCount)
    }

    private fun List<Position>.toSortedSet(): List<Position> = sortedWith(compareBy({ it.row }, { it.col }))

    private data class PlayedGame(
        val currentBoard: Board,
        val history: List<Move>,
    )

    private companion object {
        const val MOVES_PLAYED = 4
        const val ELAPSED_MS = 91_000L
        const val CLASSIC_LEVEL_ID = "cross-33"
        const val STAMPED_LEVEL = 7
        const val GENERATOR_VERSION = 1
        const val OFF_BOARD_INDEX = 99

        /** WARN-02 forgery: a negative row can never name a bit in the peg mask. */
        const val FORGED_NEGATIVE_ROW = -5

        /** The in-range column the negative-row forgery is paired with. */
        const val FORGED_COL = 3

        /** WARN-02 forgery: `2^30`, the advisory's absurd-coordinate example. */
        const val FORGED_HUGE_ROW = 1_073_741_824

        /** Column 0 — the coordinate under test in a forgery is always the row. */
        const val ORIGIN_COL = 0

        /**
         * The largest coordinate any legal footprint can carry: `Long.SIZE_BITS - 1`,
         * because a 64-cell footprint could in principle be a 1x64 line. Spelled as a
         * literal rather than derived from `ResumeSnapshotFormat.MAX_BOARD_POSITIONS`
         * so that moving the bound has to break this test rather than follow it.
         */
        const val MAX_VALID_COORDINATE = 63

        /** One past [MAX_VALID_COORDINATE] — the boundary that must be rejected. */
        const val FIRST_REJECTED_COORDINATE = 64
    }
}
