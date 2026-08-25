package com.app.pegsolitaire.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.app.pegsolitaire.data.ProgressPersistenceKeys

/**
 * The single in-progress game a mode can resume into (PERS-03, D-03).
 *
 * [mode] is the primary key, so the table holds **at most two rows** — one Classic,
 * one Challenge — and they coexist: leaving a half-solved Classic board to play
 * Challenge does not destroy either. Writing a mode's snapshot replaces its previous
 * one (upsert); solving or restarting deletes it (D-05).
 *
 * The board is carried as an opaque [jsonBlob] rather than as columns on purpose: the
 * snapshot stores the *initial* footprint plus the full move history, which rebuilds
 * the exact board by replay and therefore keeps undo intact (D-06 — restored, never
 * regenerated). Its shape is an app-layer serialization concern, so it stays out of
 * the schema; only the blob's encoding version can change without a Room migration.
 */
@Entity(tableName = ProgressPersistenceKeys.TABLE_RESUME_SNAPSHOT)
data class ResumeSnapshotEntity(
    /** `"classic"` or `"challenge"` — one snapshot per mode. */
    @PrimaryKey val mode: String,
    /** Serialized snapshot (initial footprint + move history + elapsed time). */
    val jsonBlob: String,
    /** Epoch millis the snapshot was written (on leaving the board, D-04). */
    val updatedAt: Long,
)
