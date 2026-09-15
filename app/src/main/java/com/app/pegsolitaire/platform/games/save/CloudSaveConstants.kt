package com.app.pegsolitaire.platform.games.save

/**
 * The Saved Games tokens and bounds [CloudSaveStore]'s implementations must observe: the snapshot's
 * identity, the conflict-retry ceiling, the cover image's raster size and the description's length
 * guard.
 *
 * Scoped, not global — one object per platform concern, exactly as
 * [com.app.pegsolitaire.platform.games.GamesConstants] is for sign-in and
 * [com.app.pegsolitaire.platform.ads.AdConstants] is for ads. These four live here rather than
 * inline because detekt's `MagicNumber` rule is active over `src/main` with only `-1`, `0`, `1` and
 * `2` exempt (`ignoreConstantDeclaration: true`), and because CLAUDE.md forbids magic numbers
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
     * 512 matches the launcher art's native 512-unit design canvas exactly, so plan 13-07 rasterizes
     * the vectors at 1:1 with no resampling and no filtering artefacts. Play Games guarantees a
     * cover ceiling of at least 800 KB, which a 512-square PNG of flat board art sits far below — if
     * UAT test 12's measured cover size ever comes close to that ceiling, **this is the one constant
     * to lower**, and nothing else in the cover path needs to change.
     */
    const val COVER_PX = 512

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
}
