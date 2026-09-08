package com.app.pegsolitaire.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.di.DatabaseModule
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Migration-safety gate (Success Criterion 5) for [PegSolitaireDatabase].
 *
 * The database ships at **v2**, and the two failure modes a migration can have are
 * different failures that need different assertions, so there is one case for each:
 *
 * - [version1ProgressSurvivesTheMigrationToVersion2] catches **silent data loss**. It
 *   seeds a real v1 file with a `level_result` row and a `resume_snapshot` row, runs
 *   the migration through `runMigrationsAndValidate` (which also diffs the result
 *   against the committed `2.json`), and asserts the stored values are still exactly
 *   what went in. `validateDroppedTables = true` is what would catch an accidental
 *   `DROP`.
 * - [migrationIsRegisteredOnTheProductionDatabaseBuilder] catches a **forgotten
 *   `addMigrations` crashing an upgrading device**. The case above constructs
 *   [MIGRATION_1_2] by hand and so stays green even if nothing registers it; this one
 *   opens a seeded v1 file through `DatabaseModule.providePegSolitaireDatabase` — the
 *   real builder — where a missing registration is an `IllegalStateException`, because
 *   the destructive fallback is deliberately absent.
 * - [exportedVersion1SchemaOpens] still guards the *harness* itself: that
 *   `exportSchema = true` plus the Room Gradle plugin's `schemaDirectory` really wrote
 *   `<db-class>/1.json` and that it is reachable from the test.
 *
 * Runs JVM-side under Robolectric: the project has no `androidTest` source set, so
 * `InstrumentationRegistry.getInstrumentation()` is served by Robolectric's monitor
 * shadow. [MigrationTestHelper] loads schemas from *assets* on Android (all of its
 * constructors do — the `File` one takes a database file, not a schema directory), and
 * Robolectric resolves assets from the merged **debug** assets, which is why
 * `app/build.gradle.kts` puts `schemas/` on the debug source set rather than `test`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class MigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            PegSolitaireDatabase::class.java,
        )

    @Test
    fun exportedVersion1SchemaOpens() {
        // Throws FileNotFoundException if the schema was never exported or is not
        // reachable — i.e. this asserts the v1 migration contract exists and loads.
        helper.createDatabase(TEST_DB, SCHEMA_VERSION_1).close()
    }

    @Test
    fun version1ProgressSurvivesTheMigrationToVersion2() {
        helper.createDatabase(TEST_DB, SCHEMA_VERSION_1).apply {
            execSQL(SEED_LEVEL_RESULT_SQL)
            execSQL(SEED_RESUME_SNAPSHOT_SQL)
            // MUST be closed before migrating: runMigrationsAndValidate on an open
            // handle is the classic failure here.
            close()
        }

        val migrated =
            helper.runMigrationsAndValidate(
                TEST_DB,
                SCHEMA_VERSION_2,
                VALIDATE_DROPPED_TABLES,
                MIGRATION_1_2,
            )

        migrated.query(SELECT_SEEDED_LEVEL_RESULT_SQL).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getInt(COLUMN_INDEX_FIRST)).isEqualTo(SEEDED_STARS)
            assertThat(cursor.getLong(COLUMN_INDEX_SECOND)).isEqualTo(SEEDED_BEST_TIME_MS)
            assertThat(cursor.getInt(COLUMN_INDEX_THIRD)).isEqualTo(SEEDED_BEST_SCORE)
        }
        migrated.query(SELECT_SEEDED_RESUME_SNAPSHOT_SQL).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getString(COLUMN_INDEX_FIRST)).isEqualTo(SEEDED_JSON_BLOB)
        }
        // The new table exists and is empty: purely additive, nothing back-filled.
        migrated.query(SELECT_PLAY_SESSION_COUNT_SQL).use { cursor ->
            assertThat(cursor.moveToFirst()).isTrue()
            assertThat(cursor.getInt(COLUMN_INDEX_FIRST)).isEqualTo(EMPTY_ROW_COUNT)
        }
        migrated.close()
    }

    @Test
    fun migrationIsRegisteredOnTheProductionDatabaseBuilder() =
        runTest {
            // Seed at the PRODUCTION database name so the file lands on the path the
            // production builder will open.
            helper.createDatabase(ProgressPersistenceKeys.DB_NAME, SCHEMA_VERSION_1).apply {
                execSQL(SEED_LEVEL_RESULT_SQL)
                close()
            }

            // The real Hilt provider, not a hand-rolled Room.databaseBuilder: a copy of
            // the configuration would prove nothing about DatabaseModule.
            val database = DatabaseModule.providePegSolitaireDatabase(RuntimeEnvironment.getApplication())

            try {
                val stored =
                    requireNotNull(
                        database.levelResultDao().find(ProgressPersistenceKeys.MODE_CLASSIC, SEEDED_LEVEL_ID),
                    )
                assertThat(stored.stars).isEqualTo(SEEDED_STARS)
                assertThat(stored.bestScore).isEqualTo(SEEDED_BEST_SCORE)
            } finally {
                // Do not leak the connection into the next Robolectric test.
                database.close()
            }
        }
}

private const val ROBOLECTRIC_SDK = 34
private const val TEST_DB = "migration-test.db"
private const val SCHEMA_VERSION_1 = 1
private const val SCHEMA_VERSION_2 = 2

/** Third argument of `runMigrationsAndValidate` — the check that catches a stray `DROP`. */
private const val VALIDATE_DROPPED_TABLES = true

private const val SEEDED_LEVEL_ID = ClassicLevelIds.ENGLISH33
private const val SEEDED_STARS = 3
private const val SEEDED_BEST_TIME_MS = 42_000L
private const val SEEDED_BEST_SCORE = 9001
private const val SEEDED_COMPLETED_AT = 1_700_000_000_000L
private const val SEEDED_JSON_BLOB = "{}"
private const val SEEDED_UPDATED_AT = 1_700_000_000_000L
private const val EMPTY_ROW_COUNT = 0

private const val COLUMN_INDEX_FIRST = 0
private const val COLUMN_INDEX_SECOND = 1
private const val COLUMN_INDEX_THIRD = 2

// Explicit column lists, matching the committed 1.json exactly.
private const val SEED_LEVEL_RESULT_SQL =
    "INSERT INTO level_result (mode, levelId, stars, bestTimeMs, bestScore, completedAt) " +
        "VALUES ('${ProgressPersistenceKeys.MODE_CLASSIC}', '$SEEDED_LEVEL_ID', $SEEDED_STARS, " +
        "$SEEDED_BEST_TIME_MS, $SEEDED_BEST_SCORE, $SEEDED_COMPLETED_AT)"

private const val SEED_RESUME_SNAPSHOT_SQL =
    "INSERT INTO resume_snapshot (mode, jsonBlob, updatedAt) " +
        "VALUES ('${ProgressPersistenceKeys.MODE_CHALLENGE}', '$SEEDED_JSON_BLOB', $SEEDED_UPDATED_AT)"

private const val SELECT_SEEDED_LEVEL_RESULT_SQL =
    "SELECT stars, bestTimeMs, bestScore FROM level_result WHERE levelId = '$SEEDED_LEVEL_ID'"

private const val SELECT_SEEDED_RESUME_SNAPSHOT_SQL =
    "SELECT jsonBlob FROM resume_snapshot WHERE mode = '${ProgressPersistenceKeys.MODE_CHALLENGE}'"

private const val SELECT_PLAY_SESSION_COUNT_SQL = "SELECT COUNT(*) FROM play_session"
