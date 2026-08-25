package com.app.pegsolitaire.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.app.pegsolitaire.data.ProgressPersistenceKeys.COLUMN_LEVEL_ID
import com.app.pegsolitaire.data.ProgressPersistenceKeys.COLUMN_MODE
import com.app.pegsolitaire.data.ProgressPersistenceKeys.TABLE_LEVEL_RESULT
import kotlinx.coroutines.flow.Flow

/**
 * Reads and writes per-level best results (PERS-01).
 *
 * Every query binds its inputs as `:params` — Room compiles the SQL and forbids
 * string-concatenated queries, so a forged level id can never alter the statement
 * (T-05-05). Access is `suspend` or `Flow` only: Room refuses blocking DAO calls on
 * the main thread, and the `Flow` query is what lets the Classic catalog re-render
 * its star pips the moment a result lands (D-10).
 *
 * There is deliberately **no** "insert if better" SQL here. The per-field best merge
 * (max stars, min time, max score — D-07) needs to *report* which fields improved so
 * Results can show "New best!" (D-09), so it lives in the repository as a
 * read-modify-write inside a transaction, and this DAO stays a dumb store.
 */
@Dao
interface LevelResultDao {
    /** The stored bests for one level, or `null` if it has never been solved. */
    @Query("SELECT * FROM $TABLE_LEVEL_RESULT WHERE $COLUMN_MODE = :mode AND $COLUMN_LEVEL_ID = :levelId")
    suspend fun find(
        mode: String,
        levelId: String,
    ): LevelResultEntity?

    /** Every stored result for [mode], re-emitted on any change (catalog star pips, D-10). */
    @Query("SELECT * FROM $TABLE_LEVEL_RESULT WHERE $COLUMN_MODE = :mode")
    fun observe(mode: String): Flow<List<LevelResultEntity>>

    /** Inserts, or replaces the row sharing the `(mode, levelId)` primary key. */
    @Upsert
    suspend fun upsert(entity: LevelResultEntity)
}
