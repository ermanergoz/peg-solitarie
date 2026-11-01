package com.app.pegsolitaire.data.resume

import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.domain.assist.AssistAllowance
import com.app.pegsolitaire.domain.assist.AssistAllowanceLimits
import com.app.pegsolitaire.domain.generator.GeneratorConstants
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardConstants
import com.app.pegsolitaire.domain.model.BoardSpec
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

    // --- Per-board target across resume (DEBT-02, Pitfall 3) -----------------

    @Test
    fun aDiamondSnapshotWritesItsProvenTargetIntoTheBlob() {
        assertThat(snapshotOf(Boards.diamond41Initial).minimumPegs)
            .isEqualTo(BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR)
    }

    @Test
    fun aResumedDiamondGameKeepsItsTwoPegTarget() {
        // Pitfall 3: `toBoardSpecOrNull` rebuilds the spec STRUCTURALLY rather than
        // re-resolving it from the catalog, so without an explicit carry a resumed
        // Diamond-41 game would silently revert to a one-peg target — the hint would
        // instantly report "no winning move" and stars would band on the wrong anchor.
        val restored = restore(snapshotOf(Boards.diamond41Initial))

        assertThat(restored).isNotNull()
        assertThat(restored!!.initialBoard.spec.minimumPegs)
            .isEqualTo(BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR)
    }

    @Test
    fun aResumedEnglishGameKeepsItsOnePegTarget() {
        val restored = restore(snapshotOf(Boards.english33Initial))

        assertThat(restored).isNotNull()
        assertThat(restored!!.initialBoard.spec.minimumPegs)
            .isEqualTo(BoardConstants.MINIMUM_PEGS_DEFAULT)
    }

    @Test
    fun aLegacyDiamondBlobResolvesItsTargetFromTheBoardCatalog() {
        // A blob written BEFORE per-board targets existed carries no `minimumPegs` key
        // at all. Defaulting it to 1 would be the Pitfall 3 defect; the footprint is
        // matched against `Boards.CLASSIC_SPECS` instead.
        val restored =
            SnapshotMapper
                .decode(legacyBlobFor(Boards.diamond41Initial))
                ?.let { SnapshotMapper.fromDto(it, RulesEngine) }

        assertThat(restored).isNotNull()
        assertThat(restored!!.initialBoard.spec.minimumPegs)
            .isEqualTo(BoardConstants.DIAMOND41_FEWEST_PEGS_FLOOR)
    }

    @Test
    fun aLegacyFreeformBlobFallsBackToTheDefaultTarget() {
        // A generated Challenge footprint matches no catalog entry. It is built by a
        // reverse walk from a single peg, so one peg is the correct fallback.
        val restored =
            SnapshotMapper
                .decode(legacyBlobFor(freeformBoard()))
                ?.let { SnapshotMapper.fromDto(it, RulesEngine) }

        assertThat(restored).isNotNull()
        assertThat(restored!!.initialBoard.spec.minimumPegs)
            .isEqualTo(BoardConstants.MINIMUM_PEGS_DEFAULT)
    }

    @Test
    fun aSnapshotStampedWithAnotherGeneratorVersionPassesTheStampThroughUntouched() {
        // The mapper never reads the stamp (GDD §12.3(3), tech-lead F-5): an older contract's
        // stamp, or one this build has never heard of, comes back exactly as written, with the
        // stored board. What a stamp means for a restart is the ViewModel's business.
        listOf(OLDER_GENERATOR_VERSION, UNKNOWN_GENERATOR_VERSION).forEach { version ->
            assertThat(version).isNotEqualTo(GeneratorConstants.GENERATOR_VERSION)
            val played = play(MOVES_PLAYED)
            val stamped =
                SnapshotMapper.toDto(
                    mode = ProgressPersistenceKeys.MODE_CHALLENGE,
                    levelId = STAMPED_LEVEL.toString(),
                    initialBoard = Boards.english33Initial,
                    moveHistory = played.history,
                    elapsedMs = ELAPSED_MS,
                    challenge = ChallengeStamp(level = STAMPED_LEVEL, generatorVersion = version),
                )

            val restored = restore(stamped)

            assertThat(restored).isNotNull()
            assertThat(restored!!.challenge)
                .isEqualTo(ChallengeStamp(level = STAMPED_LEVEL, generatorVersion = version))
            assertBoardMatches(actual = restored.currentBoard, expected = played.currentBoard)
            assertThat(restored.moveHistory).isEqualTo(played.history)
        }
    }

    @Test
    fun aForgedNonPositiveTargetYieldsNoResume() {
        // T-08-21: a `<= 0` target would make the solver's `pegCount <= minimumPegs`
        // goal test trivially true, so every hint would return `null` forever. The
        // whole snapshot is refused rather than clamped — a value outside the legal
        // range is evidence of tampering, not something to repair.
        val valid = snapshotOf(Boards.english33Initial)

        assertThat(restore(valid.copy(minimumPegs = FORGED_ZERO_TARGET))).isNull()
        assertThat(restore(valid.copy(minimumPegs = FORGED_NEGATIVE_TARGET))).isNull()
    }

    @Test
    fun aForgedTargetLargerThanTheFootprintYieldsNoResume() {
        val valid = snapshotOf(Boards.english33Initial)
        val oversized = Boards.English33.size + 1

        assertThat(restore(valid.copy(minimumPegs = oversized))).isNull()
    }

    @Test
    fun aTargetAtEitherEndOfTheLegalRangeStillDecodes() {
        // The boundary is inclusive on both sides: 1 and `size` are legal targets, so
        // the rejection above is a genuine range check rather than an off-by-one.
        val valid = snapshotOf(Boards.english33Initial)

        assertThat(restore(valid.copy(minimumPegs = 1))).isNotNull()
        assertThat(restore(valid.copy(minimumPegs = Boards.English33.size))).isNotNull()
    }

    @Test
    fun anAbsentFreeAllowanceRestoresAFullOne() {
        // D-04/D-17: `null` means "written before the allowance existed", not "none
        // left". A saved game from an older build must resume with a fresh allowance.
        val restored = restore(snapshotOf(Boards.english33Initial))

        assertThat(restored).isNotNull()
        assertThat(restored!!.allowance).isEqualTo(AssistAllowance())
    }

    @Test
    fun aStoredFreeAllowanceRoundTripsThroughTheBlob() {
        // Both counters survive encode → decode → rebuild, including a genuine zero:
        // an exhausted allowance must not be refilled by backgrounding the app.
        val dto =
            SnapshotMapper.toDto(
                mode = ProgressPersistenceKeys.MODE_CLASSIC,
                levelId = CLASSIC_LEVEL_ID,
                initialBoard = Boards.english33Initial,
                moveHistory = emptyList(),
                elapsedMs = ELAPSED_MS,
                freeHintsRemaining = STORED_HINTS,
                freeUndosRemaining = STORED_UNDOS,
            )

        val restored = restore(dto)

        assertThat(restored).isNotNull()
        assertThat(restored!!.allowance.hintsRemaining).isEqualTo(STORED_HINTS)
        assertThat(restored.allowance.undosRemaining).isEqualTo(STORED_UNDOS)
    }

    @Test
    fun aForgedAllowanceFallsOpenToTheFullLimitWithoutRefusingTheResume() {
        // T-09-01/T-09-02, and the deliberate asymmetry with `minimumPegs`: a forged
        // target kills the whole snapshot, a forged counter must NOT. Rejecting here
        // would turn a tamper attempt — or an unrelated corruption bug — into progress
        // loss for an honest player, which is a worse posture, not a better one. Both
        // halves are asserted together because either alone would pass a wrong impl.
        val valid = snapshotOf(Boards.english33Initial)

        val restored = restore(valid.copy(freeHintsRemaining = FORGED_HINT_COUNTER))

        assertThat(restored).isNotNull()
        assertThat(restored!!.allowance.hintsRemaining).isEqualTo(AssistAllowanceLimits.FREE_HINTS)
    }

    @Test
    fun aLegacyBlobWithNoAllowanceKeysStillDecodesToAFullAllowance() {
        // The backward-compatibility direction `ignoreUnknownKeys` does NOT cover: a
        // blob written before these fields existed carries neither key at all.
        val blob = legacyBlobFor(Boards.english33Initial)
        val restored = SnapshotMapper.decode(blob)?.let { SnapshotMapper.fromDto(it, RulesEngine) }

        assertThat(blob).doesNotContain("freeHintsRemaining")
        assertThat(restored).isNotNull()
        assertThat(restored!!.allowance).isEqualTo(AssistAllowance())
    }

    // --- assist counters (D-09) ---------------------------------------------

    @Test
    fun aStoredAssistCountRoundTripsThroughTheBlobIncludingAGenuineZero() {
        // D-09: the counters ride the snapshot so process death cannot launder an assist
        // away. A stored 0 is a real value — "this play took no undos" — and must come
        // back as 0 rather than being re-defaulted by a resolver that cannot tell the
        // difference between an absent key and an honest zero.
        val dto =
            SnapshotMapper.toDto(
                mode = ProgressPersistenceKeys.MODE_CLASSIC,
                levelId = CLASSIC_LEVEL_ID,
                initialBoard = Boards.english33Initial,
                moveHistory = emptyList(),
                elapsedMs = ELAPSED_MS,
                hintsUsed = STORED_HINTS_USED,
                undosUsed = STORED_UNDOS_USED,
            )

        val restored = restore(dto)

        assertThat(restored).isNotNull()
        assertThat(restored!!.hintsUsed).isEqualTo(STORED_HINTS_USED)
        assertThat(restored.undosUsed).isEqualTo(STORED_UNDOS_USED)
    }

    @Test
    fun aLegacyBlobWithNoAssistCounterKeysRestoresToZeroAssists() {
        // The fail-CLOSED direction, deliberately opposite to the allowance above: a
        // missing allowance is a generosity, so it guesses full; a missing assist counter
        // is a CLAIM OF PURITY, so it guesses zero — the value that concedes nothing.
        // The doesNotContain assertions are belt and braces: without them this would
        // silently degrade into a re-serialised-modern-payload test if legacyBlobFor drifts.
        val blob = legacyBlobFor(Boards.english33Initial)
        val restored = SnapshotMapper.decode(blob)?.let { SnapshotMapper.fromDto(it, RulesEngine) }

        assertThat(blob).doesNotContain("hintsUsed")
        assertThat(blob).doesNotContain("undosUsed")
        assertThat(restored).isNotNull()
        assertThat(restored!!.hintsUsed).isEqualTo(NO_ASSISTS)
        assertThat(restored.undosUsed).isEqualTo(NO_ASSISTS)
    }

    @Test
    fun aForgedNegativeAssistCounterRefusesTheResumeLikeAnyOtherForgedField() {
        // WR-03 / T-12-21: a negative count is not a legacy state and never a real one —
        // no version of this codec can write it — so it is tampering, and the mapper
        // already refuses a resume for a forged move, footprint or minimum-peg target.
        // Resolving it to zero instead would hand the tamperer exactly the value the
        // Hint-free / Undo-free predicates reward. Refusing costs an honest player
        // nothing, because ABSENT counters are a separate case handled by the `null`
        // branch and never reach this rule.
        val valid = snapshotOf(Boards.english33Initial)

        assertThat(restore(valid.copy(hintsUsed = FORGED_NEGATIVE_ASSIST_COUNT))).isNull()
        assertThat(restore(valid.copy(undosUsed = FORGED_NEGATIVE_ASSIST_COUNT))).isNull()
        // Both at once, and the untouched control: only the forgery is refused.
        assertThat(
            restore(
                valid.copy(
                    hintsUsed = FORGED_NEGATIVE_ASSIST_COUNT,
                    undosUsed = FORGED_NEGATIVE_ASSIST_COUNT,
                ),
            ),
        ).isNull()
        assertThat(restore(valid)).isNotNull()
    }

    @Test
    fun aForgedHugeAssistCounterIsPreservedRatherThanCapped() {
        // Nothing is capped at the top. A forged large count costs the tamperer their own
        // hint-free badge and nothing else, whereas capping would invent a ceiling no
        // honest play can be checked against (T-12-21 / T-12-24 residual).
        val valid = snapshotOf(Boards.english33Initial)

        val restored = restore(valid.copy(hintsUsed = FORGED_HUGE_ASSIST_COUNT))

        assertThat(restored).isNotNull()
        assertThat(restored!!.hintsUsed).isEqualTo(FORGED_HUGE_ASSIST_COUNT)
    }

    @Test
    fun toDtoWithoutTheAssistCountersWritesNeitherValue() {
        // Every pre-existing caller keeps writing today's shape: the two params are
        // defaulted, so "not tracked by this caller" and "written by an older build" are
        // the same on-disk value, and both resolve to zero on the way back in.
        val dto = snapshotOf(Boards.english33Initial)

        assertThat(dto.hintsUsed).isNull()
        assertThat(dto.undosUsed).isNull()
    }

    // --- helpers -------------------------------------------------------------

    /**
     * The on-disk blob a PRE-DEBT-02 build would have written for [board]: the exact
     * envelope `ResumeSnapshotDto` serialized then, with **no `minimumPegs` key at
     * all**, so the legacy-resolution path is genuinely exercised rather than being
     * handed a re-serialised modern payload.
     *
     * The footprint is derived from the real board rather than transcribed, because
     * these tests turn on the footprint matching `Boards.CLASSIC_SPECS` exactly — a
     * typo would silently convert a catalog-resolution test into a fallback test.
     */
    private fun legacyBlobFor(board: Board): String {
        val footprint = board.spec.inPlay.joinToString(",") { "[${it.row},${it.col}]" }
        val target = board.spec.target
        return """
            {"mode":"${ProgressPersistenceKeys.MODE_CLASSIC}","levelId":"$CLASSIC_LEVEL_ID",
            "inPlay":[$footprint],"targetRow":${target?.row},"targetCol":${target?.col},
            "initialPegs":${board.pegs},"moves":[],"elapsedMs":$ELAPSED_MS}
            """.trimIndent().replace("\n", "")
    }

    /** A footprint no Classic catalog entry carries — the Challenge freeform case. */
    private fun freeformBoard(): Board =
        Board(
            spec =
                BoardSpec(
                    inPlay = (0 until FREEFORM_LENGTH).map { Position(row = 0, col = it) },
                    target = null,
                ),
            pegs = 0L,
        ).withPeg(Position(row = 0, col = 0))

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

    /** An untouched snapshot of [initialBoard] — the per-board-target cases play no moves. */
    private fun snapshotOf(initialBoard: Board): ResumeSnapshotDto =
        SnapshotMapper.toDto(
            mode = ProgressPersistenceKeys.MODE_CLASSIC,
            levelId = CLASSIC_LEVEL_ID,
            initialBoard = initialBoard,
            moveHistory = emptyList(),
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

        /** The contract before 3.0.0's, and one no build has shipped: neither is the current one. */
        const val OLDER_GENERATOR_VERSION = 1
        const val UNKNOWN_GENERATOR_VERSION = 99
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

        /**
         * T-08-21 forgery: a zero target. This is the dangerous one — the solver's goal
         * test is `pegCount <= minimumPegs`, which a zero can never satisfy, so every
         * search would run to its deadline and every hint would come back `null`.
         */
        const val FORGED_ZERO_TARGET = 0

        /** T-08-21 forgery: a negative target, which no footprint can ever admit. */
        const val FORGED_NEGATIVE_TARGET = -1

        /** Cell count of the freeform stand-in footprint (a short 1xN line). */
        const val FREEFORM_LENGTH = 3

        /**
         * T-09-01 forgery: an allowance counter far outside `0..FREE_HINTS`. Unlike the
         * forged target above it must NOT refuse the resume — it degrades to the full
         * default allowance and the game still restores (T-09-02).
         */
        const val FORGED_HINT_COUNTER = 9_999

        /** A partly-spent hint counter, stored to prove the round trip preserves it. */
        const val STORED_HINTS = 1

        /** A genuinely exhausted undo counter: `0` is in range, so it must survive. */
        const val STORED_UNDOS = 0

        /**
         * The value an ABSENT assist counter resolves to (D-09).
         *
         * Absent only: a negative counter refuses the resume outright (WR-03). Note the
         * direction this default points — zero is the purest count, so it fails OPEN
         * toward the Hint-free / Undo-free achievements. D-09 locks that, and the exposure
         * is one self-awarded badge on a local-only store.
         */
        const val NO_ASSISTS = 0

        /** A play that took two hints — the non-zero half of the round-trip case. */
        const val STORED_HINTS_USED = 2

        /**
         * A play that took no undos. `0` here is a CLAIM (an undo-free run), not an
         * absence, so the round trip must preserve it rather than re-default it.
         */
        const val STORED_UNDOS_USED = 0

        /**
         * T-12-21 forgery: a negative assist counter. REFUSES the resume (WR-03), unlike
         * the forged-huge counter below, which is preserved verbatim — a high count only
         * ever denies the tamperer their own badge, whereas a negative one was edited in
         * to claim purity.
         */
        const val FORGED_NEGATIVE_ASSIST_COUNT = -7

        /**
         * T-12-21 forgery: an absurdly large assist counter. Preserved verbatim, because
         * the resolver bounds the bottom only and a forged-high count only denies the
         * tamperer their own badge.
         */
        const val FORGED_HUGE_ASSIST_COUNT = 9_999
    }
}
