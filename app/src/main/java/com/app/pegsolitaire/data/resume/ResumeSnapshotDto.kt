package com.app.pegsolitaire.data.resume

import com.app.pegsolitaire.domain.model.Move
import com.app.pegsolitaire.domain.model.Position
import kotlinx.serialization.Serializable

/**
 * The serialized shape of an in-progress game (PERS-03) — the payload stored in
 * `ResumeSnapshotEntity.jsonBlob`.
 *
 * It deliberately stores the **initial** board plus the **full move history**, never
 * the current board: replaying the history rebuilds the exact position *and* leaves
 * undo working after a resume (D-06). It equally never stores "regenerate level N" —
 * [challengeLevel]/[generatorVersion] are provenance stamps, so a future generator
 * change is detectable but is never an input to the rebuild.
 *
 * `@Serializable` lives **here and nowhere else**: the `:domain` module is pure Kotlin
 * with zero Android/serialization annotations (QUAL-01, enforced by `DomainPurityTest`),
 * and `BoardSpec` is not even a data class, so the footprint ([inPlay], [targetRow],
 * [targetCol]) and the peg bitmask ([initialPegs]) are carried explicitly and mapped
 * back to domain types by [SnapshotMapper].
 *
 * Coordinates travel as fixed-width `IntArray` tuples — `[row, col]` for [inPlay],
 * `[fromRow, fromCol, overRow, overCol, toRow, toCol]` for [moves] — which keeps the
 * JSON compact and free of per-position objects. See [ResumeSnapshotFormat] for the
 * tuple indices; nothing else may index these arrays with literals.
 *
 * **Note:** because two of its properties are arrays, this data class's generated
 * `equals`/`hashCode` compare array *identity*. Compare snapshots via
 * [SnapshotMapper.encode] (their on-disk form), never with `==`.
 *
 * @property mode `"classic"` or `"challenge"` — see `ProgressPersistenceKeys.MODE_*`.
 * @property levelId Classic catalog id, or the Challenge level number as text.
 * @property challengeLevel Provenance: the Challenge level this board came from.
 * @property generatorVersion Provenance: the generator contract that produced it.
 * @property inPlay The initial board's footprint as `[row, col]` pairs.
 * @property targetRow Row of the optional target hole, or `null`.
 * @property targetCol Column of the optional target hole, or `null`.
 * @property initialPegs The peg bitmask of the **initial** board (pre-history).
 * @property moves The complete move history, oldest first.
 * @property elapsedMs Wall-clock milliseconds already spent on this game.
 */
@Serializable
data class ResumeSnapshotDto(
    val mode: String,
    val levelId: String,
    val challengeLevel: Int? = null,
    val generatorVersion: Int? = null,
    val inPlay: List<IntArray>,
    val targetRow: Int? = null,
    val targetCol: Int? = null,
    val initialPegs: Long,
    val moves: List<IntArray>,
    val elapsedMs: Long,
)

/**
 * Scoped constants for the snapshot's on-disk tuple encoding (QUAL-03 — no inline
 * magic numbers). Joins `ProgressPersistenceKeys` (table/column names) and
 * `SettingsPreferences` (DataStore keys): one named object per persistence scope.
 *
 * **These indices are an on-disk contract.** Changing one changes how every stored
 * blob is read; a real format change needs a new field rather than a re-shuffle.
 */
internal object ResumeSnapshotFormat {
    /** An `inPlay` entry is exactly `[row, col]`. */
    const val POSITION_TUPLE_SIZE = 2

    /** Index of the row in a position tuple. */
    const val POSITION_ROW = 0

    /** Index of the column in a position tuple. */
    const val POSITION_COL = 1

    /** A `moves` entry is exactly `[fromRow, fromCol, overRow, overCol, toRow, toCol]`. */
    const val MOVE_TUPLE_SIZE = 6

    /** Index of the jumping peg's row. */
    const val MOVE_FROM_ROW = 0

    /** Index of the jumping peg's column. */
    const val MOVE_FROM_COL = 1

    /** Index of the jumped-over peg's row. */
    const val MOVE_OVER_ROW = 2

    /** Index of the jumped-over peg's column. */
    const val MOVE_OVER_COL = 3

    /** Index of the landing hole's row. */
    const val MOVE_TO_ROW = 4

    /** Index of the landing hole's column. */
    const val MOVE_TO_COL = 5

    /**
     * A `Board`'s peg mask is a single `Long`, so a footprint can hold at most this
     * many holes — a stored footprint longer than this is corrupt, not a big board.
     */
    const val MAX_BOARD_POSITIONS = Long.SIZE_BITS
}

/** The `[row, col]` tuple form of a position. */
internal fun Position.toTuple(): IntArray = intArrayOf(row, col)

/** The six-int tuple form of a move: `[fromRow, fromCol, overRow, overCol, toRow, toCol]`. */
internal fun Move.toTuple(): IntArray = intArrayOf(from.row, from.col, over.row, over.col, to.row, to.col)

/** The position a `[row, col]` tuple denotes, or `null` if it is not a well-formed pair. */
internal fun IntArray.toPositionOrNull(): Position? =
    with(ResumeSnapshotFormat) {
        if (size == POSITION_TUPLE_SIZE) {
            Position(row = this@toPositionOrNull[POSITION_ROW], col = this@toPositionOrNull[POSITION_COL])
        } else {
            null
        }
    }

/**
 * The move a six-int tuple denotes, or `null` if it is not a well-formed six-tuple.
 * A well-formed tuple is **not** a legal move — legality is re-checked against
 * `RulesEngine` during the replay (T-05-04).
 */
internal fun IntArray.toMoveOrNull(): Move? {
    val tuple = this
    return with(ResumeSnapshotFormat) {
        if (tuple.size == MOVE_TUPLE_SIZE) {
            Move(
                from = Position(row = tuple[MOVE_FROM_ROW], col = tuple[MOVE_FROM_COL]),
                over = Position(row = tuple[MOVE_OVER_ROW], col = tuple[MOVE_OVER_COL]),
                to = Position(row = tuple[MOVE_TO_ROW], col = tuple[MOVE_TO_COL]),
            )
        } else {
            null
        }
    }
}
