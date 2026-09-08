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
 * @property minimumPegs The board's "perfect finish" peg target (`BoardSpec.minimumPegs`,
 *   DEBT-02) — 1 for the cross boards and every generated footprint, 2 for Diamond-41.
 *
 *   **On-disk contract:** `null` means "written before per-board targets existed", NOT
 *   "the target is 1". The distinction is Pitfall 3: [SnapshotMapper] rebuilds a spec
 *   *structurally* from this blob rather than re-resolving it from the catalog, so
 *   defaulting an absent value would silently demote every already-stored Diamond-41
 *   game to a one-peg target — permanently `null` hints and mis-banded stars.
 *   [SnapshotMapper] resolves absence from `Boards.CLASSIC_SPECS` by footprint instead.
 *   Nullable-with-a-default is also what keeps today's saved games decodable at all:
 *   the writer's `ignoreUnknownKeys` covers the forward direction, this covers the
 *   backward one.
 * @property freeHintsRemaining Free hints left on this level (ADS-02, D-04/D-17).
 *
 *   **On-disk contract:** `null` means "written before the allowance existed", NOT "zero
 *   remaining" — and unlike [minimumPegs] it resolves **fail-open to a full allowance**
 *   via `AssistAllowance.fromStoredOrFull`. The two fields point opposite ways on
 *   purpose: a missing target is a *fact the game needs* to score and hint correctly, so
 *   guessing it would corrupt play; a missing allowance is a *generosity the player
 *   gets*, and the worst case of guessing wrong is a few free hints.
 *
 *   The same asymmetry governs a forged value. An out-of-range [minimumPegs] refuses the
 *   whole snapshot; an out-of-range counter must not, because refusing would convert a
 *   tamper attempt — or an unrelated corruption bug — into progress loss for an honest
 *   player (T-09-01/T-09-02). A stored `0` is in range and is preserved: an exhausted
 *   allowance has to survive process death, or backgrounding the app would be a free
 *   refill.
 * @property freeUndosRemaining Free undos left on this level — same contract as
 *   [freeHintsRemaining], resolved against its own independent limit (D-01).
 * @property hintsUsed Hints the player actually received during this play (D-09) — free,
 *   silently-free and rewarded alike.
 *
 *   **On-disk contract:** `null` means "written before assist counting existed", NOT
 *   "unknown", and it resolves to **0** — fail-CLOSED, deliberately the opposite
 *   direction to the [freeHintsRemaining] field it sits beside. The asymmetry is the
 *   whole point of putting them adjacent: a missing *allowance* is a generosity the
 *   player receives, so guessing generously costs at most a few free hints; a missing
 *   *assist counter* is a **claim of purity**, and guessing generously there would hand
 *   out a Hint-free or Undo-free achievement nobody earned.
 *
 *   The counters ride the snapshot for exactly one reason (D-09's anti-laundering
 *   purpose): the assists are spent in `GameViewModel`, which does not survive process
 *   death, so without this field killing the app mid-level would erase an assist already
 *   taken and turn a hinted solve into a hint-free one. A forged value is CLAMPED at the
 *   bottom rather than refused — see [SnapshotMapper] — because refusing a resume over a
 *   counter would convert a one-byte edit into progress loss for an honest player.
 * @property undosUsed Undos that actually rewound the board during this play — same
 *   fail-closed contract as [hintsUsed], counted independently.
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
    val minimumPegs: Int? = null,
    val freeHintsRemaining: Int? = null,
    val freeUndosRemaining: Int? = null,
    val hintsUsed: Int? = null,
    val undosUsed: Int? = null,
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

/**
 * The position a `[row, col]` tuple denotes, or `null` if it is not a well-formed pair
 * **or** carries a coordinate no legal board could hold (WARN-02).
 *
 * A stored blob is untrusted input, exactly as [SnapshotMapper.decode] documents. A
 * footprint can hold at most [ResumeSnapshotFormat.MAX_BOARD_POSITIONS] cells because a
 * `Board`'s peg mask is a single `Long`, and a 64-cell footprint could in principle be a
 * 1x64 line — so no legal coordinate can fall outside `0 until MAX_BOARD_POSITIONS`.
 * Anything else is corrupt or forged (the advisory's examples are `[-5, 3]` and
 * `[1073741824, 0]`, which would otherwise build a degenerate off-canvas board).
 *
 * Rejecting here is fail-safe, never fail-fast: `SnapshotMapper.toBoardSpecOrNull`'s
 * `positions.size == inPlay.size` check turns one rejected coordinate into a rejection of
 * the whole snapshot, and the caller surfaces that as "no resume" — a fresh board — rather
 * than a crash. The bound is deliberately not tightened to the largest *current* board
 * dimension: that would fail **closed** on a future larger board and silently destroy a
 * player's in-progress game.
 */
internal fun IntArray.toPositionOrNull(): Position? =
    with(ResumeSnapshotFormat) {
        if (size != POSITION_TUPLE_SIZE) return null
        val row = this@toPositionOrNull[POSITION_ROW]
        val col = this@toPositionOrNull[POSITION_COL]
        if (row !in 0 until MAX_BOARD_POSITIONS || col !in 0 until MAX_BOARD_POSITIONS) return null
        Position(row = row, col = col)
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
