package com.app.pegsolitaire.data

/**
 * Scoped constants for the Room-backed progress store (QUAL-03 — no inline magic
 * strings). Sits alongside [AppPreferences] (the DataStore-file name) and
 * `SettingsPreferences` (the DataStore keys): every persistence identifier the app
 * writes lives in exactly one named object, per scope.
 *
 * The table/column names are consumed from `@Entity`/`@Query` annotations as folded
 * `const` values, so renaming a column here is a single edit that the Room compiler
 * then re-validates across every DAO query.
 *
 * **These names are an on-disk contract.** Changing any of them changes the SQLite
 * schema, which means bumping `PegSolitaireDatabase`'s version and shipping a
 * `Migration` — the exported schema JSON + `MigrationTest` exist to make that
 * impossible to forget.
 */
internal object ProgressPersistenceKeys {
    /** The SQLite file name backing [com.app.pegsolitaire.data.db.PegSolitaireDatabase]. */
    const val DB_NAME = "peg_solitaire.db"

    /** Per-level best results, one row per (mode, levelId) — PERS-01. */
    const val TABLE_LEVEL_RESULT = "level_result"

    /** In-progress resume snapshots, one row per mode (max two live rows, D-03) — PERS-03. */
    const val TABLE_RESUME_SNAPSHOT = "resume_snapshot"

    /**
     * Per-play solve telemetry, one row per **solved** play — PGSA-06 / D-06. Unlike
     * [TABLE_LEVEL_RESULT]'s one row per `(mode, levelId)` best, a replay appends a
     * second row here, so the achievement aggregates stay derivable from history.
     */
    const val TABLE_PLAY_SESSION = "play_session"

    /** Shared discriminator column: which game mode a row belongs to. */
    const val COLUMN_MODE = "mode"

    /** Catalog id (Classic) or level number as text (Challenge); half of the results PK. */
    const val COLUMN_LEVEL_ID = "levelId"

    /** Star rating column, shared by [TABLE_LEVEL_RESULT] (the best) and [TABLE_PLAY_SESSION] (this play). */
    const val COLUMN_STARS = "stars"

    /** Best (maximum) score column on [TABLE_LEVEL_RESULT]; summed for the cumulative Challenge score. */
    const val COLUMN_BEST_SCORE = "bestScore"

    /** Wall-clock solve time of one play, in millis, on [TABLE_PLAY_SESSION] — the Speed-solve bound. */
    const val COLUMN_ELAPSED_MS = "elapsedMs"

    /** Hints consumed by one play, on [TABLE_PLAY_SESSION] — the Hint-free predicate. */
    const val COLUMN_HINTS_USED = "hintsUsed"

    /** Undos used by one play, on [TABLE_PLAY_SESSION] — the Undo-free predicate. */
    const val COLUMN_UNDOS_USED = "undosUsed"

    /**
     * Whether one play's last peg finished on the board's target hole, on
     * [TABLE_PLAY_SESSION]. Room stores `Boolean` as `INTEGER NOT NULL`, so SQL compares
     * it against `1`/`0` and never `true`/`false`.
     */
    const val COLUMN_LAST_PEG_ON_TARGET = "lastPegOnTarget"

    /** Mode literal for the hand-defined Classic boards. */
    const val MODE_CLASSIC = "classic"

    /** Mode literal for the procedurally generated Challenge ladder. */
    const val MODE_CHALLENGE = "challenge"
}
