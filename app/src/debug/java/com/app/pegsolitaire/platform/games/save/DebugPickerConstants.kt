package com.app.pegsolitaire.platform.games.save

/**
 * The tokens [PlayGamesSnapshotPicker] needs — and nothing else needs — in the **debug** source
 * set, beside the picker they belong to.
 *
 * They used to sit in [CloudSaveConstants] under `src/main`, which meant the QA-only title string
 * and three QA-only numbers were compiled into the release APK for a screen release cannot open.
 * Here they are absent from release by construction, which is the same reason the picker itself
 * moved (tech-lead L-3).
 *
 * [CloudSaveConstants.DEBUG_PICKER_EXTRA] deliberately stays in `src/main`: the gate that reads it,
 * [DebugSnapshotPickerEntry], ships in every build so that the release build's *ignoring* of the
 * extra is a tested behaviour rather than an absence.
 *
 * Scoped, not global — one object per concern, exactly as [CloudSaveConstants] is for the snapshot
 * itself and [com.app.pegsolitaire.platform.games.GamesConstants] is for sign-in. **Nothing
 * identifying may ever be written here**, following the same reservation those two carry.
 */
object DebugPickerConstants {
    /**
     * The title bar of the debug snapshot picker. A constant and not a `strings.xml` entry on
     * purpose: no player ever sees it, and a resource would put a QA-only label in front of every
     * translator. Being debug-only source, it is also not in the release APK to be found.
     */
    const val PICKER_TITLE = "Saved games (debug)"

    /**
     * The game owns exactly one snapshot ([CloudSaveConstants.SNAPSHOT_NAME]), so the picker lists
     * at most one.
     */
    const val MAX_SNAPSHOTS = 1

    /** The picker's `startActivityForResult` code; nothing consumes the result. Beside 9003/9004. */
    const val REQUEST_CODE = 9005

    /**
     * How long the debug picker waits for the session to report signed-in. Longer than
     * [com.app.pegsolitaire.platform.games.GamesConstants.ACTIVITY_WAIT_MS] because a cold-start
     * sign-in is a network round trip; bounded because nothing here may wait forever — and now
     * bounded twice, since the wait runs on the launched Activity's own scope and dies with it.
     */
    const val SIGN_IN_WAIT_MS = 30_000L
}
