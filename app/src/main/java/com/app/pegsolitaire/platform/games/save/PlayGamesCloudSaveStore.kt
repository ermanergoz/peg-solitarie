package com.app.pegsolitaire.platform.games.save

import android.app.Activity
import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
import com.app.pegsolitaire.platform.games.GamesConstants
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The real, SDK-backed [CloudSaveStore] — Play Games **Saved Games** behind the cloud seam
 * (CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-05).
 *
 * Every Saved Games type this game ever names lives in this one file. The seam above it carries
 * `ByteArray`s, so nothing outside `platform/` can hold a snapshot, a snapshot client or the
 * `Activity` one is built from. The client itself is constructed **per call** and stored in no
 * field, which is what keeps `PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks` green: that
 * rule reads bytecode, including the generated Hilt component's `Provider<T>` field per binding,
 * so an SDK type reachable from the graph fails the gate no matter which module declared it
 * (proved empirically in plan 09-05). [com.app.pegsolitaire.platform.games.PlayGamesService] and
 * [com.app.pegsolitaire.platform.ads.AdMobAdGateway] hold their SDK handles the same way.
 *
 * ## The conflict policy is the single most consequential line here
 *
 * The snapshot is opened with `RESOLUTION_POLICY_MANUAL`, and no other resolution policy may ever
 * appear in this file. Google's javadoc says of MANUAL that it *"ensures that no user changes to
 * the state of the save game will ever be lost"*, and of the most-recently-modified policy that it
 * *"blindly chooses the most recent data, [so] it is possible that a player's changes may get
 * lost"*. Those two sentences are the ROADMAP's mandate verbatim. An automatic policy would make
 * this class shorter and would delete a player's progress the first time two devices disagreed —
 * silently, on a device no developer is holding (T-13-34).
 *
 * ## Every opened handle reaches exactly one terminal call
 *
 * `commitAndClose` is documented to fail *"when called with a snapshot that was not opened or has
 * already been committed/discarded"*, and an open handle that is never closed makes the **next**
 * open fail for the rest of the process. The failure mode is therefore "cloud save works once per
 * app launch and then silently stops" — which looks like flakiness, not like a bug. Both public
 * methods run their handle through a `try`/`finally` with a local latch so that every path,
 * including a thrown SDK call and an early return, closes exactly once and never twice
 * (T-13-36, RESEARCH Pitfalls 1 and 2).
 *
 * ## An offline commit reports success, and that is correct
 *
 * Play Games caches a `commitAndClose` locally when the device has no connectivity and syncs it on
 * reconnection, so [save] can legitimately return `true` before anything has reached Google's
 * servers. The data is not lost — GMS owns that retry queue and exposes no API through which an
 * app could observe its state. An app-level "did it really upload" check would therefore duplicate
 * a queue Google already runs while being unable to see the thing it claims to verify. A reviewer
 * asking why the upload is not verified is the signal this paragraph exists for (RESEARCH
 * Pitfall 4).
 *
 * ## Nothing here logs
 *
 * The payload, the description and any SDK exception message are all tied to a signed-in player
 * (T-13-40 / T-11-04, ASVS V7). Every failure becomes `null` or `false` and nothing else, matching
 * [com.app.pegsolitaire.platform.games.PlayGamesService] and
 * [com.app.pegsolitaire.platform.ads.RewardedAdSlot]. The absence is enforced by a grep gate
 * rather than by review, which is why this paragraph names no logging API.
 */
@Singleton
class PlayGamesCloudSaveStore
    @Inject
    constructor(
        private val activityProvider: ForegroundActivityProvider,
        private val coverImageSource: CoverImageSource,
        private val descriptionFormatter: CloudSaveDescriptionFormatter,
    ) : CloudSaveStore {
        /**
         * Reads the player's cloud payload, or `null` for every "nothing was read" cause.
         *
         * **The read path discards, never commits.** A pull that changed nothing must not rewrite
         * the snapshot and move the server's `lastModifiedTimestamp` — doing so would make a
         * device that only ever *reads* look like the most recently written one.
         *
         * **Empty contents are reported as `null`, not as an empty array.** "There is no payload"
         * and "the payload is empty" must be indistinguishable to the caller, because both mean
         * the same thing: stay local. A caller handed an empty array could reasonably try to
         * decode it, fail, and treat a brand-new account as a corrupt cloud save.
         *
         * **Why a read takes a merge at all:** an `open` can come back **conflicted**, and
         * resolving that conflict writes merged bytes back to the server. Resolving it with
         * anything other than the caller's real merge — "take the newest", "take mine" — would
         * discard one device's progress during what the player experiences as a restore, which is
         * precisely the data loss CLOUD-02 forbids.
         */
        override suspend fun load(mergeRemote: (ByteArray, ByteArray) -> ByteArray): ByteArray? {
            val activity = awaitForegroundActivity() ?: return null

            return runCatching {
                val client = PlayGames.getSnapshotsClient(activity)
                val snapshot = openResolved(client, mergeRemote) ?: return@runCatching null

                var closed = false
                try {
                    val bytes = snapshot.snapshotContents.readFully()
                    discardQuietly(client, snapshot)
                    closed = true
                    bytes.takeIf { it.isNotEmpty() }
                } finally {
                    // The latch is what makes this exactly-once rather than at-least-once: a
                    // second terminal call on a closed handle fails its Task (Pitfall 2).
                    if (!closed) discardQuietly(client, snapshot)
                }
            }.onFailure { if (it is CancellationException) throw it }
                .getOrNull()
        }

        /**
         * Writes [bytes] as the player's cloud payload together with the listing metadata that
         * makes the save legible in the Play Games saves UI (CLOUD-03), reporting whether the SDK
         * accepted the commit.
         *
         * **The commit is a superset of everything this pass has seen.** Bytes that arrived during
         * conflict resolution are already in the server copy by the time the handle comes back
         * open, so they are read off the snapshot and folded into the payload through the same
         * merge. Writing [bytes] alone would undo, at commit time, the resolution that had just
         * succeeded.
         *
         * See the class KDoc for why `true` offline is a truthful answer, and why the handle is
         * closed in a `finally` rather than on the happy path alone.
         */
        override suspend fun save(
            bytes: ByteArray,
            description: CloudSaveDescription,
            playedTimeMs: Long,
            mergeRemote: (ByteArray, ByteArray) -> ByteArray,
        ): Boolean {
            val activity = awaitForegroundActivity() ?: return false

            return runCatching {
                val client = PlayGames.getSnapshotsClient(activity)
                val snapshot = openResolved(client, mergeRemote) ?: return@runCatching false

                var committed = false
                try {
                    val existing = snapshot.snapshotContents.readFully()
                    val finalBytes = if (existing.isEmpty()) bytes else mergeRemote(existing, bytes)

                    // A FULL overwrite, and that is the point (RESEARCH Pitfall 5). The partial
                    // write sibling on SnapshotContents is documented to leave the excess in place
                    // whenever the new payload is shorter than the old one — after a trim, or once
                    // a JSON field shrinks — producing {...valid json...}xyz, a blob that decodes
                    // on some parsers and throws on others. Do not switch to it.
                    snapshot.snapshotContents.writeBytes(finalBytes)

                    client.commitAndClose(snapshot, metadataChangeFor(description, playedTimeMs)).await()
                    committed = true
                    true
                } finally {
                    if (!committed) discardQuietly(client, snapshot)
                }
            }.onFailure { if (it is CancellationException) throw it }
                .getOrDefault(false)
        }

        /**
         * The foreground [Activity], or `null` if none arrives within
         * [GamesConstants.ACTIVITY_WAIT_MS].
         *
         * **This is the *awaiting* accessor, deliberately unlike
         * [com.app.pegsolitaire.platform.games.PlayGamesService]'s `submitting` helper**, which
         * reads the foreground Activity without waiting and gives up immediately. That is right
         * for a fire-and-forget leaderboard submission and wrong here: the pull fires once per
         * process, immediately after sign-in resolves, on a cold start where Home may not have
         * attached yet — and a no-wait read would simply skip it forever (RESEARCH Pitfall 3). A
         * submission can afford to be skipped because the caller's dirty flag guarantees a retry;
         * the once-per-process pull cannot.
         *
         * The bound is not optional. [ForegroundActivityProvider.awaitActivity]'s own KDoc makes
         * the timeout the caller's responsibility: a backgrounded app may never resume, and an
         * unbounded await inside a process-lifetime singleton hangs it for the life of the process
         * (T-11-06, T-13-38). A timeout is a **failed pass** — `null`/`false`, publish nothing,
         * leave the caller's pending marker set — never an error the player sees.
         */
        private suspend fun awaitForegroundActivity(): Activity? =
            withTimeoutOrNull(GamesConstants.ACTIVITY_WAIT_MS) { activityProvider.awaitActivity() }

        /**
         * Opens the one snapshot under the one policy and resolves any conflict, yielding an
         * **open** handle the caller must close, or `null` when resolution never converged.
         *
         * Shared by [load] and [save] so the name/createIfNotFound/policy triple is written once:
         * two call sites would be two chances for one of them to drift onto an automatic policy.
         */
        private suspend fun openResolved(
            client: SnapshotsClient,
            mergeRemote: (ByteArray, ByteArray) -> ByteArray,
        ): Snapshot? {
            val opened =
                client
                    .open(
                        CloudSaveConstants.SNAPSHOT_NAME,
                        // createIfNotFound: a first-ever push must not have to create the snapshot
                        // in a separate step, and a first-ever pull on a fresh account reads the
                        // empty one it creates as "no payload".
                        true,
                        SnapshotsClient.RESOLUTION_POLICY_MANUAL,
                    ).await()

            return resolveToOpenSnapshot(client, opened, mergeRemote)
        }

        /**
         * The bounded conflict loop: merges both sides of every conflict through [mergeRemote]
         * until the SDK hands back data, or gives up after [CloudSaveConstants.MAX_CONFLICT_RETRIES]
         * attempts (CLOUD-02).
         *
         * Three things that are silent when got wrong:
         *
         * 1. **The loop's exit value is an *open* snapshot.** `resolveConflict` has already written
         *    the merged bytes server-side, but the handle it returns still must reach a terminal
         *    call; leaving it open blocks the next open for the rest of the process (Pitfall 1).
         *    Both callers therefore take ownership of it immediately.
         * 2. **`EMPTY_CHANGE` during resolution, real metadata only at the final commit.** The
         *    javadoc says values absent from a metadata change *"will be resolved to the version
         *    currently on the server"*, so a half-built change mid-loop would fight the commit that
         *    follows it.
         * 3. **`getSnapshot()` is the server's version; `getConflictingSnapshot()` is the modified
         *    one.** The merge is commutative, so swapping them is harmless **today** — which is
         *    exactly why the test pins the orientation, so a future non-commutative field cannot be
         *    added silently.
         *
         * A bound is required rather than defensive: `resolveConflict` is documented to be able to
         * *"result in a conflict itself, in which case resolution should be repeated"*, so this is
         * genuinely a loop against the SDK and an unbounded one would spin for the life of the
         * process (T-13-37). Ten is Google's own sample bound. Exhaustion is a clean failure — the
         * handle is discarded, `null` is returned, and the caller retries on the next pass.
         */
        private suspend fun resolveToOpenSnapshot(
            client: SnapshotsClient,
            initial: SnapshotsClient.DataOrConflict<Snapshot>,
            mergeRemote: (ByteArray, ByteArray) -> ByteArray,
        ): Snapshot? {
            var result = initial
            var attempts = 0

            while (attempts < CloudSaveConstants.MAX_CONFLICT_RETRIES) {
                // `getConflict()` is the SDK's own nullable discriminator — null exactly when
                // `isConflict()` is false — so reading it is both the test and the value.
                val conflict = result.conflict ?: break
                attempts++
                val serverBytes = conflict.snapshot.snapshotContents.readFully()
                val modifiedBytes = conflict.conflictingSnapshot.snapshotContents.readFully()

                val resolution = conflict.resolutionSnapshotContents
                resolution.writeBytes(mergeRemote(serverBytes, modifiedBytes))

                result =
                    client
                        .resolveConflict(
                            conflict.conflictId,
                            conflict.snapshot.metadata.snapshotId,
                            SnapshotMetadataChange.EMPTY_CHANGE,
                            resolution,
                        ).await()
            }

            val unresolved = result.conflict ?: return result.data

            // Giving up is not a licence to leak: the unresolved handle is closed here so the next
            // pass can still open the snapshot at all.
            discardQuietly(client, unresolved.snapshot)
            return null
        }

        /**
         * The listing metadata attached to every commit (CLOUD-03): the localized one-liner, the
         * player's total played time, and the cover art.
         *
         * A `null` cover is the designed degradation, not an error — [CoverImageSource] reports it
         * for a stripped resource or a failed raster, and a save without decoration is worth
         * immeasurably more than no save at all, so the cover is simply left off and the commit
         * proceeds.
         */
        private suspend fun metadataChangeFor(
            description: CloudSaveDescription,
            playedTimeMs: Long,
        ): SnapshotMetadataChange =
            SnapshotMetadataChange
                .Builder()
                .setDescription(descriptionFormatter.format(description))
                .setPlayedTimeMillis(playedTimeMs)
                .apply { coverImageSource.coverOrNull()?.let { setCoverImage(it) } }
                .build()

        /**
         * Closes an opened [snapshot] without writing to it, swallowing whatever the SDK says.
         *
         * Used on the read path and on every failure path. The `runCatching` is not laziness: this
         * runs from a `finally`, frequently while another failure is already propagating, and a
         * throw from a cleanup call would replace the real cause with the noise from tidying up
         * after it.
         */
        private suspend fun discardQuietly(
            client: SnapshotsClient,
            snapshot: Snapshot,
        ) {
            runCatching { client.discardAndClose(snapshot).await() }
        }
    }
