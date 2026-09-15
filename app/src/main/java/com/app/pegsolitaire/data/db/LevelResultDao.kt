package com.app.pegsolitaire.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.app.pegsolitaire.data.ProgressPersistenceKeys.COLUMN_BEST_SCORE
import com.app.pegsolitaire.data.ProgressPersistenceKeys.COLUMN_LEVEL_ID
import com.app.pegsolitaire.data.ProgressPersistenceKeys.COLUMN_MODE
import com.app.pegsolitaire.data.ProgressPersistenceKeys.COLUMN_STARS
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

    /**
     * The scalar progress totals in ONE round trip (PGSL-01, PGSL-02, D-02, D-03).
     *
     * A `FROM`-less `SELECT` of correlated scalar subqueries, so it always returns exactly
     * one row and the return type is non-null. Only the star `SUM` is coalesced: `COUNT`
     * never yields `NULL`, and the two Challenge scalars stay nullable on purpose, because
     * "no Challenge level banked" must not read as "level 0 / score 0".
     *
     * `CAST(levelId AS INTEGER)` is total and safe here precisely because the query is
     * mode-filtered: `ResultsViewModel` writes `challengeLevel.toString()` as the id for
     * every `challenge` row, so the column is always decimal text. Without the cast a
     * `MAX` over TEXT would order lexically and answer `"9"` over `"17"`.
     *
     * The star sum spans BOTH modes deliberately — the D-03 star tiers count stars
     * everywhere — which is why it carries no `WHERE`.
     *
     * @param challengeMode bound `ProgressPersistenceKeys.MODE_CHALLENGE`.
     * @param threeStars bound `AchievementThresholds.PERFECT_STARS`.
     */
    @Query(
        """
        SELECT
          (SELECT MAX(CAST($COLUMN_LEVEL_ID AS INTEGER)) FROM $TABLE_LEVEL_RESULT
             WHERE $COLUMN_MODE = :challengeMode) AS highestChallengeLevel,
          (SELECT SUM($COLUMN_BEST_SCORE) FROM $TABLE_LEVEL_RESULT
             WHERE $COLUMN_MODE = :challengeMode) AS challengeScoreTotal,
          (SELECT COALESCE(SUM($COLUMN_STARS), 0) FROM $TABLE_LEVEL_RESULT) AS totalStars,
          (SELECT COUNT(*) FROM $TABLE_LEVEL_RESULT) AS solvedLevelCount,
          (SELECT COUNT(*) FROM $TABLE_LEVEL_RESULT
             WHERE $COLUMN_STARS >= :threeStars) AS threeStarCount
        """,
    )
    suspend fun totals(
        challengeMode: String,
        threeStars: Int,
    ): LevelResultTotals

    /**
     * Every stored Classic board's standing — the ONE read behind THREE answers: the
     * cleared set, the perfect set, and the per-board best scores of PGSL-03.
     *
     * Deriving all three from a single result set is what makes it structurally impossible
     * for a Grand tour / Perfectionist count to disagree with the per-board set it was
     * counted from (D-04). `(mode, levelId)` is the primary key, so each board appears at
     * most once and the list needs no `GROUP BY` and no de-duplication.
     *
     * @param classicMode bound `ProgressPersistenceKeys.MODE_CLASSIC`.
     */
    @Query(
        """
        SELECT $COLUMN_LEVEL_ID AS levelId, $COLUMN_STARS AS stars, $COLUMN_BEST_SCORE AS bestScore
        FROM $TABLE_LEVEL_RESULT
        WHERE $COLUMN_MODE = :classicMode
        """,
    )
    suspend fun classicStandings(classicMode: String): List<ClassicStanding>

    /** Inserts, or replaces the row sharing the `(mode, levelId)` primary key. */
    @Upsert
    suspend fun upsert(entity: LevelResultEntity)

    /**
     * Every stored row across BOTH modes — the cloud export's single read, and the
     * local side of the cloud import's merge (CLOUD-01 / CLOUD-02).
     *
     * Deliberately unfiltered and unordered: the caller keys the rows by `(mode, levelId)`
     * into a map, so a `WHERE` would only force a second query and an `ORDER BY` would buy
     * nothing. The cost is bounded by the same row count `totals` already scans.
     */
    @Query("SELECT * FROM $TABLE_LEVEL_RESULT")
    suspend fun all(): List<LevelResultEntity>

    /**
     * Writes every entity in [entities], replacing the rows sharing their `(mode, levelId)`
     * primary keys — the cloud import's single write.
     *
     * It is a bulk verb rather than a loop over [upsert] because the import has already
     * merged each row against its stored counterpart *inside the same transaction*, so
     * there is no per-row read-modify-write left to serialize. Room issues one statement
     * set; ten thousand point upserts would issue ten thousand.
     */
    @Upsert
    suspend fun upsertAll(entities: List<LevelResultEntity>)

    /**
     * Empties the table.
     *
     * **D-14's profile-replace path only.** The player answered a dialog saying this
     * device now belongs to another account; nothing else in the app may call it, and in
     * particular no cloud read or import path ever does — an import merges, it never
     * wipes (CLOUD-02).
     */
    @Query("DELETE FROM $TABLE_LEVEL_RESULT")
    suspend fun deleteAll()
}
