package com.app.pegsolitaire.platform.games.save

import javax.inject.Inject

/**
 * The silently-degrading [CloudSaveStore] stub: [load] reports "no payload", [save] reports
 * "nothing was written", and neither ever touches the merge it is handed.
 *
 * **Bound in `PlatformModule` on purpose, and only until plan 13-09.** Cloud save has not shipped
 * disabled — this is the intermediate state that lets wave 3's `ProgressSubmitter` cloud pass be
 * written, compiled and tested green *before* any `SnapshotsClient` plumbing exists, which is the
 * same order Phases 11 and 12 built their seams in and the reason each SDK swap was later a
 * one-line change. Plan 13-09 replaces the binding with `PlayGamesCloudSaveStore`; this class then
 * stays in the tree, unbound, exactly as
 * [com.app.pegsolitaire.platform.games.NoOpGamesService] and
 * [com.app.pegsolitaire.platform.ads.NoOpAdGateway] did, because `NoOpCloudSaveStoreTest` is where
 * the seam's fail-quiet contract stays written down somewhere executable.
 *
 * Its answers are the *pessimistic* ones, matching [com.app.pegsolitaire.platform.games.NoOpGamesService]
 * rather than the fail-open ad stub, and for the same reason the games stub declines: a phantom
 * success would have the caller clear its cloud-push-pending marker for a payload that never left
 * the device, losing the retry rather than the round trip. `null` and `false` are simply the truth
 * for a stub, and they are also the answers that leave the local store untouched and the retry
 * bookkeeping honest (CLOUD-05).
 */
class NoOpCloudSaveStore
    @Inject
    constructor() : CloudSaveStore {
        /**
         * `null`: no payload was read, so the caller should stay local — never read as "the player
         * has no cloud progress". [mergeRemote] is not invoked; a stub reaches no conflict.
         */
        override suspend fun load(mergeRemote: (ByteArray, ByteArray) -> ByteArray): ByteArray? = null

        /**
         * `false`: nothing was written, and a caller's retry bookkeeping should say so.
         * [mergeRemote] is not invoked; there is no remote payload to merge against.
         */
        override suspend fun save(
            bytes: ByteArray,
            description: CloudSaveDescription,
            playedTimeMs: Long,
            mergeRemote: (ByteArray, ByteArray) -> ByteArray,
        ): Boolean = false
    }
