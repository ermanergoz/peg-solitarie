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

    /**
     * Drops BOTH modes' snapshots.
     *
     * **D-14's profile-replace path only — and nothing else, ever.** The in-progress board
     * never leaves this device and is never touched by a cloud event (CLOUD-04/CLOUD-05):
     * no cloud read, no import and no conflict resolution may call this, which is why a
     * cloud import is written so that it cannot even reach this DAO. The one legitimate
     * caller is the player answering "use the other account's progress", which replaces
     * the whole device state on purpose.
     *
     * D-04 is the case this exists to *contrast* with: a merged ladder jumping from 10 to
     * 50 leaves a level-10 game in progress perfectly resumable, because an import is not
     * a replace.
     */
    @Query("DELETE FROM $TABLE_RESUME_SNAPSHOT")
    suspend fun clearAll()
}
