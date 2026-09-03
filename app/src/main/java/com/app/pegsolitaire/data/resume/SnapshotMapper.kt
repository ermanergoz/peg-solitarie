package com.app.pegsolitaire.data.resume

import com.app.pegsolitaire.data.resume.ResumeSnapshotFormat.MAX_BOARD_POSITIONS
import com.app.pegsolitaire.domain.assist.AssistAllowance
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.BoardConstants
import com.app.pegsolitaire.domain.model.BoardSpec
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import com.app.pegsolitaire.domain.rules.RulesEngine
import kotlinx.serialization.json.Json

/**
 * The Challenge-only provenance of a snapshot: which generated level it came from and
 * which generator contract produced it.
 *
 * The two always travel together (a level number without its generator version cannot
 * be trusted to mean the same board later), and both are absent for Classic games.
 * Carried through the snapshot **only** so a generator change is detectable — never as
 * an input to rebuilding the board (D-06).
 */
data class ChallengeStamp(
    val level: Int,
    val generatorVersion: Int,
)

/**
 * A game rebuilt from a snapshot: the initial board, the replayed current board, the
 * full move history (so undo keeps working), the elapsed time, the free-assist allowance
 * the player had left, and the Challenge stamp when there is one.
 *
 * [allowance] is non-null by construction: an absent or forged stored counter resolves to
 * the full default rather than to "no allowance", so there is no such thing as a restored
 * game without one (ADS-02, D-04/D-17).
 */
data class RestoredGame(
    val initialBoard: Board,
    val currentBoard: Board,
    val moveHistory: List<Move>,
    val elapsedMs: Long,
    val allowance: AssistAllowance,
    val challenge: ChallengeStamp?,
)

/**
 * Maps between the app-layer [ResumeSnapshotDto] and the pure-domain game state
 * (PERS-03).
 *
 * **Restore is replay, never regeneration (D-06).** [fromDto] folds
 * [RulesEngine.applyMove] over the stored history exactly as `GameViewModel.onUndo`
 * does, so the rebuilt board is bit-identical to the one the player left *and* the
 * restored history keeps undo working. It never calls the level generator: the
 * footprint on disk wins, and [ResumeSnapshotDto.challengeLevel] is provenance only.
 *
 * **Nothing read back from disk is trusted (Security §V5, T-05-03/T-05-04).** A blob is
 * app-written but could be corrupted or tampered with, so [decode] never throws
 * ([Json] failures degrade to `null`) and [fromDto] validates before it rebuilds:
 * well-formed tuples, a footprint that fits the `Long` peg mask with no duplicates, a
 * target hole inside that footprint, a stored perfect-finish peg count inside
 * `1..footprint size`, a peg mask with no bits outside it, and — the important one —
 * **every stored move re-checked against [RulesEngine.legalMoves] before it is
 * applied**. Any failure yields `null`, which callers surface as "no resume" (the
 * Continue affordance simply disappears) rather than as a crash.
 *
 * The peg-count check is worth spelling out (T-08-21): a forged `minimumPegs <= 0` would
 * make the solver's `pegCount <= minimumPegs` goal test unsatisfiable, so every search
 * would burn its full deadline and **every hint would come back `null` forever** — a
 * silently broken game rather than an obvious failure. It is therefore refused, never
 * clamped.
 *
 * **The board's own target survives the round trip (DEBT-02, Pitfall 3).** Because the
 * rebuild is structural rather than a catalog lookup, `minimumPegs` is carried in the
 * blob explicitly; a blob written before that field existed has its target resolved from
 * [Boards.CLASSIC_SPECS] by footprint equality. Without both halves, a resumed Diamond-41
 * game would revert to a one-peg target that its board cannot reach.
 */
object SnapshotMapper {
    /** Unknown keys are ignored so a future field cannot orphan today's saved games. */
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Builds the snapshot for a game in progress from its **initial** board plus the
     * moves played since — deliberately not from the current board, because the history
     * is what makes the restore undoable.
     *
     * [freeHintsRemaining]/[freeUndosRemaining] default to `null` — the same "not
     * written" value an older blob carries — so a caller that does not yet track the
     * allowance produces a snapshot that restores with a full one rather than a locked
     * out one.
     */
    fun toDto(
        mode: String,
        levelId: String,
        initialBoard: Board,
        moveHistory: List<Move>,
        elapsedMs: Long,
        challenge: ChallengeStamp? = null,
        freeHintsRemaining: Int? = null,
        freeUndosRemaining: Int? = null,
    ): ResumeSnapshotDto =
        ResumeSnapshotDto(
            mode = mode,
            levelId = levelId,
            challengeLevel = challenge?.level,
            generatorVersion = challenge?.generatorVersion,
            inPlay = initialBoard.spec.inPlay.map { it.toTuple() },
            targetRow = initialBoard.spec.target?.row,
            targetCol = initialBoard.spec.target?.col,
            initialPegs = initialBoard.pegs,
            moves = moveHistory.map { it.toTuple() },
            elapsedMs = elapsedMs,
            minimumPegs = initialBoard.spec.minimumPegs,
            freeHintsRemaining = freeHintsRemaining,
            freeUndosRemaining = freeUndosRemaining,
        )

    /**
     * Rebuilds the game [dto] describes, or `null` when it is structurally invalid or
     * its move history does not replay legally (fail-safe: the caller treats `null` as
     * "nothing to resume").
     *
     * **The allowance is the one field that never contributes to that `null`.**
     * [resolveMinimumPegsOrNull] refuses the whole snapshot for an out-of-range target,
     * because a wrong target silently breaks scoring and hinting for the rest of the
     * game. The free-assist counters take the opposite path through
     * [AssistAllowance.fromStoredOrFull]: absent or out-of-range degrades to the default
     * allowance and the game still restores. Refusing a resume over a forged counter
     * would be a worse security posture, not a better one — it hands an attacker a way
     * to destroy an honest player's progress with a one-byte edit, in exchange for
     * denying them at most three hints they could get by replaying the level anyway
     * (T-09-01/T-09-02).
     */
    fun fromDto(
        dto: ResumeSnapshotDto,
        rulesEngine: RulesEngine,
    ): RestoredGame? =
        dto.toInitialBoardOrNull()?.let { initialBoard ->
            dto.toMoveHistoryOrNull()?.let { moveHistory ->
                replayOrNull(initialBoard, moveHistory, rulesEngine)?.let { currentBoard ->
                    RestoredGame(
                        initialBoard = initialBoard,
                        currentBoard = currentBoard,
                        moveHistory = moveHistory,
                        elapsedMs = dto.elapsedMs,
                        allowance =
                            AssistAllowance.fromStoredOrFull(
                                hints = dto.freeHintsRemaining,
                                undos = dto.freeUndosRemaining,
                            ),
                        challenge = dto.toChallengeStampOrNull(),
                    )
                }
            }
        }

    /** Serializes [dto] to the string stored in the snapshot row's blob column. */
    fun encode(dto: ResumeSnapshotDto): String = json.encodeToString(dto)

    /**
     * Parses a stored blob, or `null` if it is corrupt, truncated or missing required
     * fields. `SerializationException` is an `IllegalArgumentException`, so this single
     * catch covers every malformed-input failure kotlinx.serialization raises.
     */
    fun decode(blob: String): ResumeSnapshotDto? =
        try {
            json.decodeFromString<ResumeSnapshotDto>(blob)
        } catch (ignored: IllegalArgumentException) {
            null
        }

    /**
     * Replays [moveHistory] from [initialBoard], returning the resulting board — or
     * `null` the moment a stored move is not legal on the board it claims to act upon
     * (a forged history is never applied, T-05-04).
     */
    private fun replayOrNull(
        initialBoard: Board,
        moveHistory: List<Move>,
        rulesEngine: RulesEngine,
    ): Board? =
        moveHistory.fold<Move, Board?>(initialBoard) { board, move ->
            board
                ?.takeIf { move in rulesEngine.legalMoves(it) }
                ?.let { rulesEngine.applyMove(it, move) }
        }

    private fun ResumeSnapshotDto.toInitialBoardOrNull(): Board? {
        val spec = toBoardSpecOrNull() ?: return null
        return if (spec.holdsOnly(initialPegs)) Board(spec = spec, pegs = initialPegs) else null
    }

    private fun ResumeSnapshotDto.toBoardSpecOrNull(): BoardSpec? {
        val positions = inPlay.mapNotNull { it.toPositionOrNull() }
        val row = targetRow
        val col = targetCol
        val target = if (row != null && col != null) Position(row = row, col = col) else null
        val footprintIsWellFormed =
            positions.size == inPlay.size &&
                positions.size in 1..MAX_BOARD_POSITIONS &&
                positions.distinct().size == positions.size
        val targetIsWellFormed = (row == null) == (col == null) && (target == null || target in positions)
        // Deliberately short-circuited: the resolver constructs a BoardSpec to derive the
        // catalog lookup key, which a malformed footprint would make throw.
        val perfectFinish =
            if (footprintIsWellFormed && targetIsWellFormed) resolveMinimumPegsOrNull(positions) else null
        return if (perfectFinish != null) {
            BoardSpec(inPlay = positions, target = target, minimumPegs = perfectFinish)
        } else {
            null
        }
    }

    /**
     * The board's "perfect finish" peg target for a snapshot whose footprint has already
     * been validated as [positions] (DEBT-02) — or `null` when the stored value is not a
     * target any board could carry, which refuses the whole snapshot.
     *
     * Three cases, in order:
     *  1. **Present** — it must satisfy `in 1..positions.size`, the same invariant
     *     [BoardSpec] enforces. Out of range is rejected outright rather than clamped,
     *     exactly as a forged coordinate already is: a bad value here is evidence of
     *     tampering, and clamping would repair an attacker's blob into a playable one.
     *  2. **Absent, footprint in the catalog** — the blob predates this field, so the
     *     target is resolved from [Boards.CLASSIC_SPECS] by footprint equality. This is
     *     the Pitfall 3 case: blind defaulting would demote every stored Diamond-41 game.
     *  3. **Absent, footprint unknown** — a generated Challenge board, built by a reverse
     *     walk from a single peg, so [BoardConstants.MINIMUM_PEGS_DEFAULT] is correct.
     *
     * The lookup key comes from [BoardSpec.footprint] rather than a second sort written
     * here: that property IS the structural identity key, and duplicating its ordering
     * would be a silent determinism hazard.
     */
    private fun ResumeSnapshotDto.resolveMinimumPegsOrNull(positions: List<Position>): Int? {
        val stored = minimumPegs
        if (stored != null) return stored.takeIf { it in 1..positions.size }
        val footprint = BoardSpec(inPlay = positions, target = null).footprint
        return Boards.CLASSIC_SPECS.firstOrNull { it.footprint == footprint }?.minimumPegs
            ?: BoardConstants.MINIMUM_PEGS_DEFAULT
    }

    private fun ResumeSnapshotDto.toMoveHistoryOrNull(): List<Move>? {
        val history = moves.mapNotNull { it.toMoveOrNull() }
        return if (history.size == moves.size) history else null
    }

    private fun ResumeSnapshotDto.toChallengeStampOrNull(): ChallengeStamp? {
        val level = challengeLevel
        val version = generatorVersion
        return if (level != null && version != null) {
            ChallengeStamp(level = level, generatorVersion = version)
        } else {
            null
        }
    }

    /** Whether [pegs] sets no bit outside this spec's `0..size-1` bit indices. */
    private fun BoardSpec.holdsOnly(pegs: Long): Boolean = size == MAX_BOARD_POSITIONS || (pegs ushr size) == 0L
}
