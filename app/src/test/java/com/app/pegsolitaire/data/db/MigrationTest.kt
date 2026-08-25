package com.app.pegsolitaire.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Migration-safety gate (Success Criterion 5) for [PegSolitaireDatabase].
 *
 * The database ships at version 1, so there is no migration to run yet — what this
 * test proves is that the *harness* is real: `exportSchema = true` plus the Room
 * Gradle plugin's `schemaDirectory` actually wrote `<db-class>/1.json`, that JSON is
 * on the unit-test asset path, and [MigrationTestHelper] can open a database from it.
 * With that in place, the day a v2 entity change lands, a single
 * `helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)` line turns this
 * into a real migration test — and a forgotten migration fails the build instead of
 * silently wiping player results on device.
 *
 * Runs JVM-side under Robolectric: the project has no `androidTest` source set, so
 * `InstrumentationRegistry.getInstrumentation()` is served by Robolectric's monitor
 * shadow and the exported schema is resolved from `app/schemas` (wired onto the
 * `test` source set's assets in `app/build.gradle.kts`).
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
        // Throws FileNotFoundException if the schema was never exported or is not on
        // the test asset path — i.e. this asserts the migration contract exists.
        helper.createDatabase(TEST_DB, SCHEMA_VERSION_1).close()
    }
}

private const val ROBOLECTRIC_SDK = 34
private const val TEST_DB = "migration-test.db"
private const val SCHEMA_VERSION_1 = 1
