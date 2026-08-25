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
 * The version stays at **1** until an entity's columns actually change; any such
 * change requires bumping [DB_VERSION], adding a `Migration` in `DatabaseModule`, and
 * regenerating the exported schema.
 */
@Database(
    entities = [LevelResultEntity::class, ResumeSnapshotEntity::class],
    version = PegSolitaireDatabase.DB_VERSION,
    exportSchema = true,
)
abstract class PegSolitaireDatabase : RoomDatabase() {
    /** Per-level best results (stars / time / score). */
    abstract fun levelResultDao(): LevelResultDao

    /** In-progress snapshots, one per mode. */
    abstract fun resumeDao(): ResumeDao

    companion object {
        /** Current schema version; bump only alongside a `Migration` + a fresh schema JSON. */
        const val DB_VERSION = 1
    }
}
