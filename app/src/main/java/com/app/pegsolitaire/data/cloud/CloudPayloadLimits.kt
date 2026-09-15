package com.app.pegsolitaire.data.cloud

/**
 * Scoped constants for the cloud payload's **size** budget (QUAL-03 — no inline magic
 * numbers), in the same shape as `ResumeSnapshotFormat` (the resume blob's on-disk tuple
 * contract) and the `data` package's Room-identifier object: one named constants object per
 * persistence scope, every value KDoc'd with *why that number*.
 *
 * (Those neighbours are described rather than named on purpose — a build gate greps this
 * package for the Room object's name, so the vocabulary stays out, prose included.)
 *
 * detekt's `MagicNumber` exempts constant declarations, which is precisely why this object
 * is the only legal home for these byte counts — a second statement of any of them, in the
 * codec, in the Saved Games store or in a test, is a bug waiting for the copies to drift.
 *
 * ## Remit: bytes, and only bytes
 *
 * The wire *tokens* and the schema version live in `CloudPayloadKeys`; the merge identities
 * live in `:domain`'s `ProgressMergeLimits`. Nothing here validates a field.
 */
object CloudPayloadLimits {
    /**
     * The snapshot data budget this app plans against: 3 MB.
     *
     * `SnapshotsClient.getMaxDataSize()` returns the real per-snapshot limit in **bytes**,
     * and its contract is that the returned value is *"guaranteed to be at least 3 MB"* —
     * a documented **floor**, not the exact figure. Sizing the trim against the floor is
     * therefore safe on any server that honours the contract: a real limit can only be
     * larger, never smaller.
     *
     * Plan 13-09's Saved Games store additionally re-checks the true runtime value before
     * it commits, and deliberately ignores an answer *below* this floor — a response that
     * low is a contract violation, and honouring it would let a bad reply shrink a
     * player's payload.
     */
    const val MAX_DATA_SIZE_FLOOR_BYTES = 3 * 1024 * 1024

    /**
     * Headroom held back from [MAX_DATA_SIZE_FLOOR_BYTES]: 256 KB.
     *
     * The measured size is the payload alone. The snapshot the server actually stores also
     * carries its metadata (description, played time, cover image reference), and a future
     * payload field will grow every row at once. A margin an order of magnitude larger than
     * the whole realistic payload (~400 KB at 10,000 Challenge levels) costs nothing,
     * because the trim is a safety valve that is expected never to fire.
     */
    const val PAYLOAD_SAFETY_MARGIN_BYTES = 256 * 1024

    /**
     * The size `CloudProgressCodec.encodeWithinCap` trims to by default.
     *
     * DERIVED from the floor and the margin rather than restated as a third number, per
     * `AchievementThresholds.SPEED_SOLVE_MAX_ELAPSED_MS`' discipline: the cap, the headroom
     * and the target cannot drift apart because only two of them are ever written down.
     */
    const val TRIM_TARGET_BYTES = MAX_DATA_SIZE_FLOOR_BYTES - PAYLOAD_SAFETY_MARGIN_BYTES
}
