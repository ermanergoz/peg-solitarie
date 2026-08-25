package com.app.pegsolitaire.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.app.pegsolitaire.data.ProgressPersistenceKeys.COLUMN_MODE
import com.app.pegsolitaire.data.ProgressPersistenceKeys.TABLE_RESUME_SNAPSHOT
import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes the at-most-two in-progress snapshots (PERS-03, D-03).
 *
 * [observeModes] exists so Home can decide which "Continue" affordances to show
 * (D-01/D-11) without deserializing either blob — the mode list is the cheap
 * question, the snapshot itself is only loaded when the player actually taps
 * Continue. [clear] is the D-05 hook: solving a level or starting a new game in a
 * mode removes that mode's snapshot while leaving the other mode resumable.
 *
 * All access is `suspend`/`Flow` (no main-thread Room calls) and every statement
 * binds `:mode` as a parameter rather than concatenating it (T-05-05).
 */
@Dao
interface ResumeDao {
    /** The saved snapshot for [mode], or `null` when nothing is resumable there. */
    @Query("SELECT * FROM $TABLE_RESUME_SNAPSHOT WHERE $COLUMN_MODE = :mode")
    suspend fun get(mode: String): ResumeSnapshotEntity?

    /** The modes currently holding a snapshot — drives the Home Continue tiles. */
    @Query("SELECT $COLUMN_MODE FROM $TABLE_RESUME_SNAPSHOT")
    fun observeModes(): Flow<List<String>>

    /** Saves [entity], replacing any snapshot already stored for its mode. */
    @Upsert
    suspend fun upsert(entity: ResumeSnapshotEntity)

    /** Drops [mode]'s snapshot (solved, restarted, or new game — D-05). */
    @Query("DELETE FROM $TABLE_RESUME_SNAPSHOT WHERE $COLUMN_MODE = :mode")
    suspend fun clear(mode: String)
}
