package com.app.pegsolitaire.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's single relational store: per-level best results (PERS-01) and the
 * in-progress resume snapshots (PERS-03). Scalar preferences (settings, current
 * Challenge level) deliberately stay in DataStore — they are key-value tuples, not
 * tables.
 *
 * `exportSchema = true` writes the full schema of every version to
 * `app/schemas/<this class>/<version>.json`, which is committed to VCS. That file is
 * the diffable migration contract: `MigrationTest` opens it, and once a v2 lands
 * `runMigrationsAndValidate` compares the migrated database against it, so a schema
 * change shipped without a matching `Migration` fails the build rather than
 * destroying a player's stars on their device.
 *
 * **v2** added `play_session` as a purely additive `CREATE TABLE` (PGSA-06 / D-06):
 * `level_result` and `resume_snapshot` are untouched by `MIGRATION_1_2`, which is the
 * whole progress-preservation argument — an upgrading player keeps every star, best
 * time and in-progress game. The bump ritual is unchanged for v3 and beyond: bump
 * [DB_VERSION], add a `Migration`, register it on the builder in `DatabaseModule`, and
 * commit the freshly generated schema JSON.
 */
@Database(
    entities = [LevelResultEntity::class, ResumeSnapshotEntity::class, PlaySessionEntity::class],
    version = PegSolitaireDatabase.DB_VERSION,
    exportSchema = true,
)
abstract class PegSolitaireDatabase : RoomDatabase() {
    /** Per-level best results (stars / time / score). */
    abstract fun levelResultDao(): LevelResultDao

    /** In-progress snapshots, one per mode. */
    abstract fun resumeDao(): ResumeDao

    /** Append-only per-play solve telemetry (PGSA-06). */
    abstract fun playSessionDao(): PlaySessionDao

    companion object {
        /** Current schema version; bump only alongside a `Migration` + a fresh schema JSON. */
        const val DB_VERSION = 2
    }
}
