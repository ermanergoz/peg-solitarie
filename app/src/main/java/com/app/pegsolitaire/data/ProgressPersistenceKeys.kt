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

    /** Shared discriminator column: which game mode a row belongs to. */
    const val COLUMN_MODE = "mode"

    /** Catalog id (Classic) or level number as text (Challenge); half of the results PK. */
    const val COLUMN_LEVEL_ID = "levelId"

    /** Mode literal for the hand-defined Classic boards. */
    const val MODE_CLASSIC = "classic"

    /** Mode literal for the procedurally generated Challenge ladder. */
    const val MODE_CHALLENGE = "challenge"
}
