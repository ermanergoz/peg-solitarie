package com.app.pegsolitaire.data.db

import androidx.room.Entity
import com.app.pegsolitaire.data.ProgressPersistenceKeys

/**
 * One player's best result for one level (PERS-01).
 *
 * Keyed by the **composite** primary key `(mode, levelId)` so Classic id `"cross-33"`
 * and Challenge level `"33"` can never collide, and so a replay updates the existing
 * row rather than appending a second one. The three best-fields are tracked
 * independently (D-07): [stars] and [bestScore] keep the maximum ever achieved while
 * [bestTimeMs] keeps the minimum — a fast sloppy run and a slow perfect run each
 * retain the part they won. Only solved plays are recorded (D-08).
 *
 * The per-field merge itself is the repository's job (read-modify-write inside a
 * transaction); this type is the flat storage shape and mirrors the domain
 * `ScoreResult` fields without importing it, keeping `:domain` free of Room
 * annotations (QUAL-01).
 */
@Entity(
    tableName = ProgressPersistenceKeys.TABLE_LEVEL_RESULT,
    primaryKeys = [ProgressPersistenceKeys.COLUMN_MODE, ProgressPersistenceKeys.COLUMN_LEVEL_ID],
)
data class LevelResultEntity(
    /** `"classic"` or `"challenge"` — see `ProgressPersistenceKeys.MODE_*`. */
    val mode: String,
    /** Classic catalog id, or the Challenge level number rendered as text. */
    val levelId: String,
    /** Best (maximum) star rating earned, 0-3. */
    val stars: Int,
    /** Best (minimum) solve time in milliseconds. */
    val bestTimeMs: Long,
    /** Best (maximum) score. */
    val bestScore: Int,
    /** Epoch millis of the play that last improved any best (drives "New best!", D-09). */
    val completedAt: Long,
)
