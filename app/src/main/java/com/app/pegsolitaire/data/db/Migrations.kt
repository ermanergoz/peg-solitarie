package com.app.pegsolitaire.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * The `CREATE TABLE` for `play_session`, **copied verbatim** out of the generated
 * `app/schemas/com.app.pegsolitaire.data.db.PegSolitaireDatabase/2.json` with only
 * `${'$'}{TABLE_NAME}` replaced by the literal table name.
 *
 * This string is a deliberate, documented exception to the no-magic-strings rule
 * (QUAL-03): it is *generated output transcribed*, not authored SQL, and `2.json` is
 * its source of truth. `runMigrationsAndValidate` diffs the migrated database against
 * that exact JSON, so retyping the DDL — a dropped `NOT NULL`, a missing
 * `AUTOINCREMENT`, a `BOOLEAN` where Room expects `INTEGER` — turns into a schema-diff
 * failure here, or an `IllegalStateException` on an upgrading device if the test were
 * skipped. Substituting `ProgressPersistenceKeys.TABLE_PLAY_SESSION` into the middle of
 * the statement would defeat the point of the copy ritual, so the table name is spelled
 * out exactly as the generator emitted it. Line breaks are `+` concatenation only, to
 * respect `max_line_length = 120`; the value stays a compile-time constant.
 */
private const val CREATE_PLAY_SESSION_SQL =
    "CREATE TABLE IF NOT EXISTS `play_session` (" +
        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
        "`mode` TEXT NOT NULL, `levelId` TEXT NOT NULL, `stars` INTEGER NOT NULL, " +
        "`score` INTEGER NOT NULL, `elapsedMs` INTEGER NOT NULL, `hintsUsed` INTEGER NOT NULL, " +
        "`undosUsed` INTEGER NOT NULL, `lastPegOnTarget` INTEGER NOT NULL, `finishedAt` INTEGER NOT NULL)"

/**
 * v1 -> v2: adds the `play_session` solve-telemetry table (PGSA-06 / D-06).
 *
 * **Purely additive by construction.** The body is one `CREATE TABLE` and nothing else
 * — no `ALTER`, no `DROP`, no data copy — so `level_result` and `resume_snapshot` are
 * not read, written or touched. That is the entire progress-preservation argument: an
 * upgrading player keeps every star, best time and in-progress game, and because there
 * is no cloud backup until Phase 13, that guarantee is one-way and unrecoverable if
 * broken. `MigrationTest.version1ProgressSurvivesTheMigrationToVersion2` seeds real v1
 * rows and asserts their exact values afterwards with `validateDroppedTables = true`,
 * which is the check that would catch an accidental drop.
 *
 * Registered on the one production Room builder in `DatabaseModule`; a forgotten
 * registration is covered by `MigrationTest.migrationIsRegisteredOnTheProductionDatabaseBuilder`.
 */
internal val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(CREATE_PLAY_SESSION_SQL)
        }
    }
