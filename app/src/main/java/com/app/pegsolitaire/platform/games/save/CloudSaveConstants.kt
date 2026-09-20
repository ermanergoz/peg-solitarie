package com.app.pegsolitaire.platform.games.save

/**
 * The Saved Games tokens and bounds [CloudSaveStore]'s implementations must observe: the snapshot's
 * identity, the conflict-retry ceiling, the cover image's raster size, the description's length
 * guard, and the handful of tokens the debug-only snapshot picker needs.
 *
 * Scoped, not global — one object per platform concern, exactly as
 * [com.app.pegsolitaire.platform.games.GamesConstants] is for sign-in and
 * [com.app.pegsolitaire.platform.ads.AdConstants] is for ads. These four live here rather than
 * inline because detekt's `MagicNumber` rule is active over `src/main` with only `-1`, `0`, `1` and
 * `2` exempt (`ignoreConstantDeclaration: true`), and because the project forbids magic numbers
 * regardless of what the linter would tolerate.
 *
 * **What must never be written here:** anything identifying, and anything the SDK already owns. No
 * player id and no display name, following [com.app.pegsolitaire.platform.games.GamesConstants]'s
 * reservation; and no copy of an SDK constant such as a resolution policy, which is referenced from
 * the SDK by name inside `platform/` and is therefore not a magic number to begin with.
 */
object CloudSaveConstants {
    /**
     * The one snapshot name this game ever opens, for every player, with `createIfNotFound = true`.
     *
     * One fixed name is the whole of the game's snapshot identity: there is no save-slot concept
     * here, so "several snapshots exist" is not a case to be handled beyond always opening this
     * name. The requirements reserve exactly one cloud-save dialog and it is the profile swap, so a
     * snapshot picker would be a second one.
     *
     * Play Games documents the filename rule for `SnapshotsClient.open`: it *"must be between 1 and
     * 100 non-URL-reserved characters (a-z, A-Z, 0-9, or the symbols `-`, `.`, `_`, or `~`)."*
     * `CloudSaveConstantsTest` asserts this value against that charset, so a later rename cannot
     * quietly produce a name the SDK rejects at runtime on a device no developer is holding.
     */
    const val SNAPSHOT_NAME = "progress"

    /**
     * How many times a conflict resolution may be retried before an implementation gives up and
     * reports failure.
     *
     * A bound is required, not defensive: Play Games documents that `resolveConflict` *"can result
     * in a conflict itself, in which case resolution should be repeated"*, so the resolve path is
     * genuinely a loop and an unbounded one would spin against the SDK for the life of the process
     * (T-13-22). Ten is Google's own sample bound. Exhausting it is a **clean failure** — `null`
     * from [CloudSaveStore.load] or `false` from [CloudSaveStore.save], the caller retries next pass
     * — never a crash and never an error shown to the player.
     */
    const val MAX_CONFLICT_RETRIES = 10

    /**
     * The edge length, in pixels, of the square cover image attached to every commit (D-09).
     *
     * 384 is set by measurement, not taste. Play Games charges the cover at its **raw**
     * `ARGB_8888` size — four bytes per pixel — and not at its PNG size: in UAT test 12 GMS logged
     * `Snapshot cover image is too large. Currently at 1048576 bytes; max is 819200. Image will be
     * scaled` for the former 512-square cover, on every commit, on both devices. 384 x 384 x 4 is
     * 589,824 bytes, which fits the documented 800 KB floor (819,200) with room to spare; 448 would
     * be 802,816 and leave under 2%. `CoverImageSourceTest` pins the product against the ceiling, so
     * this is still **the one constant to change** and it cannot be raised past the cap unnoticed.
     * The art is a vector layer-list, so any raster size is drawn natively with no resampling.
     */
    const val COVER_PX = 384

    /**
     * The length at which an implementation truncates the description before handing it to
     * `SnapshotMetadataChange` (D-11).
     *
     * Purely defensive: **no maximum is documented anywhere** for `setDescription` (RESEARCH
     * assumption A2), so this guard exists only so that an unexpectedly long line — a verbose
     * translation, a player at a six-digit ladder level — can never reject the whole commit and cost
     * the push. The D-11 one-liner is around 45 characters, so in practice this never bites.
     */
    const val MAX_DESCRIPTION_CHARS = 100

    /**
     * The launch-intent extra that asks a **debug** build to open Google's snapshot picker. Read
     * only by [DebugSnapshotPickerEntry], which ignores it on any other build. Quoted literally in
     * QA briefs, so `DebugSnapshotPickerEntryTest` pins the name.
     */
    const val DEBUG_PICKER_EXTRA = "com.app.pegsolitaire.debug.SHOW_SNAPSHOT_PICKER"

    /**
     * The title bar of the debug snapshot picker. A constant and not a `strings.xml` entry on
     * purpose: no player ever sees it, and a resource would put a QA-only label in front of every
     * translator.
     */
    const val DEBUG_PICKER_TITLE = "Saved games (debug)"

    /** The game owns exactly one snapshot ([SNAPSHOT_NAME]), so the picker lists at most one. */
    const val DEBUG_PICKER_MAX_SNAPSHOTS = 1

    /** The picker's `startActivityForResult` code; nothing consumes the result. Beside 9003/9004. */
    const val DEBUG_PICKER_REQUEST_CODE = 9005

    /**
     * How long the debug picker waits for the session to report signed-in. Longer than
     * [com.app.pegsolitaire.platform.games.GamesConstants.ACTIVITY_WAIT_MS] because a cold-start
     * sign-in is a network round trip; bounded because nothing here may wait forever.
     */
    const val DEBUG_PICKER_SIGN_IN_WAIT_MS = 30_000L
}
