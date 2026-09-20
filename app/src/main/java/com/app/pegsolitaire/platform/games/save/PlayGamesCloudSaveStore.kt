package com.app.pegsolitaire.platform.games.save

import android.app.Activity
import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
import com.app.pegsolitaire.platform.games.GamesConstants
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.games.snapshot.Snapshot
import com.google.android.gms.games.snapshot.SnapshotMetadataChange
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
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
 * One sentence of this is SDK contract and the rest is reasoning, and the two are kept apart
 * deliberately — a phase spent arguing from folklore is what taught us to (FU-2).
 *
 * **Documented:** `commitAndClose` fails *"when called with a snapshot that was not opened or has
 * already been committed/discarded"* (22.0.0 javadoc).
 *
 * **Not documented, and asserted here as reasoning only:** that an open handle which is never
 * closed makes the **next** open fail for the rest of the process. That claim comes from the
 * developer guide and community lore, not from the bundled 22.0.0 javadoc, which was searched for
 * it and does not contain it; and this phase's UAT cannot confirm or refute it either, because
 * every abandoned handle in that session was abandoned in a process that then ended. Treat it as
 * a plausible failure mode, not as a guarantee.
 *
 * **The practice is right either way, which is why it is unchanged.** A leaked handle is at best a
 * leak and at worst "cloud save works once per app launch and then silently stops" — a shape that
 * reads as flakiness rather than as a bug — and closing exactly once costs nothing. Ownership
 * therefore never has a gap: while a conflict is being resolved the loop owns the handle and
 * closes it if anything throws, and the moment a resolved handle comes back the public method
 * puts it under a `try`/`finally`. Every path — a thrown SDK call, an early return, a caller's
 * cancel — closes exactly once and never twice (T-13-36, RESEARCH Pitfalls 1 and 2).
 *
 * ## An offline commit reports success, and that is correct
 *
 * Play Games caches a `commitAndClose` locally when the device has no connectivity and syncs it on
 * reconnection, so [save] can legitimately report a commit before anything has reached Google's
 * servers. The data is not lost — GMS owns that retry queue, and an app-level retry would only
 * duplicate it (RESEARCH Pitfall 4).
 *
 * The upload **is** observable, contrary to what this paragraph claimed before the convergence
 * build: `SnapshotMetadata.hasChangePending()` is documented as *"whether or not this snapshot has
 * any changes pending that have not been uploaded to the server"*, and [load] reads it, so the next
 * pull's [CloudPullProbe] carries a flag about the last commit. Read [CloudPullProbe]'s KDoc before
 * drawing a conclusion from it: the field is weaker than it looks in the `true` direction. The
 * upload is still not *waited for* or *retried* here — that remains GMS's queue.
 *
 * ## Nothing here logs
 *
 * The payload, the description and any SDK exception message are all tied to a signed-in player
 * (T-13-40 / T-11-04, ASVS V7). Every failed read becomes [CloudPull.NOTHING] and nothing else, matching
 * [com.app.pegsolitaire.platform.games.PlayGamesService] and
 * [com.app.pegsolitaire.platform.ads.RewardedAdSlot]. The absence is enforced by a grep gate
 * rather than by review, which is why this paragraph names no logging API.
 *
 * ## What a failed push does report
 *
 * A [CloudSaveOutcome]: which of this class's own stages the push ended at, and the SDK's status
 * **integer** when the failure was an `ApiException`. That is the whole of it. The exception's
 * message, the snapshot id, the description and the payload are never copied anywhere — the
 * outcome type has no slot that could hold them. It exists because a bare `false` from two
 * signed-in, online devices could not be told apart from a missing Activity, and the alternative
 * was the logging the paragraph above forbids.
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
         * Reads the player's cloud payload — `null` bytes for every "nothing was read" cause —
         * and, whenever a snapshot opened, the [CloudPullProbe] read off its metadata.
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
         *
         * **The probe is read after the bytes and can never cost them.** It is metadata the SDK
         * already parcelled with the open — no further call, no network — but a diagnostic that
         * could turn a readable save into "stay local" would be a worse bug than the one it helps
         * find, so its failure is its own `null`.
         */
        override suspend fun load(mergeRemote: (ByteArray, ByteArray) -> ByteArray): CloudPull {
            val activity = awaitForegroundActivity() ?: return CloudPull.NOTHING

            return runCatching {
                val client = PlayGames.getSnapshotsClient(activity)
                val opened = openResolved(client, mergeRemote) ?: return@runCatching CloudPull.NOTHING

                // One terminal call, in one place: the read path only ever discards, so the
                // `finally` IS the close and there is no second site to double it (Pitfall 2).
                val pull =
                    try {
                        val bytes = opened.snapshot.snapshotContents.readFully()
                        CloudPull(bytes = bytes.takeIf { it.isNotEmpty() }, probe = probeOf(opened))
                    } finally {
                        discardQuietly(client, opened.snapshot)
                    }

                // The close is non-cancellable on purpose, so a cancel that arrived during it is
                // honoured here instead: a stopped pass must not hand bytes on to its caller.
                currentCoroutineContext().ensureActive()
                pull
            }.onFailure { if (it is CancellationException) throw it }
                .getOrDefault(CloudPull.NOTHING)
        }

        /**
         * Writes [bytes] as the player's cloud payload together with the listing metadata that
         * makes the save legible in the Play Games saves UI (CLOUD-03), reporting whether the SDK
         * accepted the commit and, when it did not, the stage that ended the push.
         *
         * **The commit is a superset of everything this pass has seen.** Bytes that arrived during
         * conflict resolution are already in the server copy by the time the handle comes back
         * open, so they are read off the snapshot and folded into the payload through the same
         * merge. Writing [bytes] alone would undo, at commit time, the resolution that had just
         * succeeded.
         *
         * **Classification is per await, not per push.** `open`, `resolveConflict` and
         * `commitAndClose` each run through [awaitStage], which is what lets the outcome say
         * *which* SDK call refused. The outer `runCatching` stays as the net that keeps the seam
         * from ever throwing; whatever it catches that no stage claimed is
         * [CloudSaveStage.FAILED_OTHER].
         *
         * See the class KDoc for why a commit reported offline is a truthful answer, and why the
         * handle is closed in a `finally` rather than on the happy path alone.
         */
        override suspend fun save(
            bytes: ByteArray,
            description: CloudSaveDescription,
            playedTimeMs: Long,
            mergeRemote: (ByteArray, ByteArray) -> ByteArray,
        ): CloudSaveOutcome {
            val activity = awaitForegroundActivity() ?: return CloudSaveOutcome(CloudSaveStage.NO_ACTIVITY)

            return runCatching {
                val client = PlayGames.getSnapshotsClient(activity)

                // Built BEFORE the open, not between the open and the commit: formatting and the
                // cover raster can both throw, and nothing that can throw may sit between a handle
                // coming back open and [commitOrDiscard] taking ownership of it.
                val change = metadataChangeFor(description, playedTimeMs)
                val opened =
                    openResolved(client, mergeRemote)
                        ?: return@runCatching CloudSaveOutcome(CloudSaveStage.CONFLICT_UNRESOLVED)

                val outcome = commitOrDiscard(client, opened.snapshot, bytes, change, mergeRemote)

                // See [load]: the close cannot be cancelled, so the cancel is honoured after it.
                currentCoroutineContext().ensureActive()
                outcome
            }.getOrElse { failure ->
                if (failure is CancellationException) throw failure
                (failure as? StageFailure)?.outcome
                    ?: CloudSaveOutcome(CloudSaveStage.FAILED_OTHER, statusCodeOf(failure))
            }
        }

        /**
         * Takes ownership of an open [snapshot] and ends it with exactly one terminal call: the
         * commit when everything went through, a discard on every other path.
         *
         * The latch is what makes this exactly-once rather than at-least-once — a second terminal
         * call on a closed handle fails its Task (Pitfall 2).
         */
        private suspend fun commitOrDiscard(
            client: SnapshotsClient,
            snapshot: Snapshot,
            bytes: ByteArray,
            change: SnapshotMetadataChange,
            mergeRemote: (ByteArray, ByteArray) -> ByteArray,
        ): CloudSaveOutcome {
            var committed = false
            try {
                val existing = snapshot.snapshotContents.readFully()
                val finalBytes = if (existing.isEmpty()) bytes else mergeRemote(existing, bytes)
                if (exceedsServerCap(client, finalBytes)) return CloudSaveOutcome(CloudSaveStage.OVER_CAP)

                // A FULL overwrite, and that is the point (RESEARCH Pitfall 5). The partial write
                // sibling on SnapshotContents is documented to leave the excess in place whenever
                // the new payload is shorter than the old one — after a trim, or once a JSON field
                // shrinks — producing {...valid json...}xyz, a blob that decodes on some parsers
                // and throws on others. Do not switch to it.
                snapshot.snapshotContents.writeBytes(finalBytes)

                awaitStage(CloudSaveStage.COMMIT_FAILED) { client.commitAndClose(snapshot, change).await() }
                committed = true
                return CloudSaveOutcome(CloudSaveStage.COMMITTED)
            } finally {
                if (!committed) discardQuietly(client, snapshot)
            }
        }

        /**
         * Runs one SDK await and, if it fails, re-raises the failure as a [StageFailure] naming
         * [stage] and carrying the status integer — and nothing else — from the original.
         *
         * The original throwable is deliberately **not** chained as the cause: its message is
         * tied to a signed-in player, and a `StageFailure` that held it would be one careless
         * `toString()` away from the leak T-11-04 exists to prevent. Cancellation is rethrown
         * untouched before any classification, because it is a caller's structured concurrency
         * and never an SDK failure (WR-04).
         */
        private suspend fun <T> awaitStage(
            stage: CloudSaveStage,
            call: suspend () -> T,
        ): T =
            runCatching { call() }.getOrElse { failure ->
                if (failure is CancellationException) throw failure
                throw StageFailure(CloudSaveOutcome(stage, statusCodeOf(failure)))
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
        ): OpenedSnapshot? {
            val opened =
                awaitStage(CloudSaveStage.OPEN_FAILED) {
                    client
                        .open(
                            CloudSaveConstants.SNAPSHOT_NAME,
                            // createIfNotFound: a first-ever push must not have to create the
                            // snapshot in a separate step, and a first-ever pull on a fresh
                            // account reads the empty one it creates as "no payload".
                            true,
                            SnapshotsClient.RESOLUTION_POLICY_MANUAL,
                        ).await()
                }

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
         *    call; leaving it open leaks it, and plausibly — this is the undocumented half the class
         *    KDoc separates out — blocks the next open for the rest of the process (Pitfall 1).
         *    Both callers therefore take ownership of it immediately, and until they do the loop
         *    itself closes whatever it is holding when anything throws.
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
        ): OpenedSnapshot? {
            var result = initial
            var attempts = 0

            // `isConflict()` is the ONLY discriminator. The two getters are not nullable views of
            // it: the pinned SDK throws IllegalStateException from `getConflict()` when there is
            // no conflict and from `getData()` when there is one, so each is read strictly on its
            // own side of this test. Reading `conflict` first failed every push and every pull.
            while (result.isConflict) {
                // Annotated `@Nullable` in the SDK, which is what invited the `?:` in the first
                // place; it is non-null exactly when `isConflict()` is true (same field).
                val conflict = checkNotNull(result.conflict)
                if (attempts == CloudSaveConstants.MAX_CONFLICT_RETRIES) {
                    // Giving up is not a licence to leak: the unresolved handle is closed here so
                    // the next pass can still open the snapshot at all.
                    discardQuietly(client, conflict.snapshot)
                    return null
                }
                attempts++

                // Nobody above this frame can close a handle they were never given, so until
                // `resolveConflict` hands back the next result the loop is the owner: a read that
                // throws, a merge that throws, a refused resolution and a caller's cancel all
                // close it on the way out.
                var resolved = false
                try {
                    result = resolveOnce(client, conflict, mergeRemote)
                    resolved = true
                } finally {
                    if (!resolved) discardQuietly(client, conflict.snapshot)
                }
            }

            // `attempts` counts resolutions that were *started*, and every one that reaches this
            // line also finished: a resolution that failed threw out of the loop above.
            return result.data?.let { OpenedSnapshot(snapshot = it, conflictsResolved = attempts) }
        }

        /** One pass of the conflict loop: merge both sides and hand the result to the SDK. */
        private suspend fun resolveOnce(
            client: SnapshotsClient,
            conflict: SnapshotsClient.SnapshotConflict,
            mergeRemote: (ByteArray, ByteArray) -> ByteArray,
        ): SnapshotsClient.DataOrConflict<Snapshot> {
            val serverBytes = conflict.snapshot.snapshotContents.readFully()
            val modifiedBytes = conflict.conflictingSnapshot.snapshotContents.readFully()

            val resolution = conflict.resolutionSnapshotContents
            resolution.writeBytes(mergeRemote(serverBytes, modifiedBytes))

            return awaitStage(CloudSaveStage.CONFLICT_UNRESOLVED) {
                client
                    .resolveConflict(
                        conflict.conflictId,
                        conflict.snapshot.metadata.snapshotId,
                        SnapshotMetadataChange.EMPTY_CHANGE,
                        resolution,
                    ).await()
            }
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
         * Whether [payload] exceeds the size this server will accept — the ROADMAP's "confirm it
         * at runtime".
         *
         * **A guard, not a precondition.** The javadoc guarantees the cap is *"at least 3 MB"*, and
         * the payload is trimmed to a target below that before it ever reaches this seam, so this
         * can only fire against a server violating its own documented contract. That is exactly
         * why it is an enforced check rather than an assumption: an oversized payload is rejected
         * server-side *permanently*, so a push that kept retrying it would never succeed and would
         * burn a pass every solve.
         *
         * **A failed query is not a failed push.** `getMaxDataSize` is documented to be able to
         * fail with a `RemoteException`; when it does, the push proceeds unchanged. A guard that
         * cannot answer must never become a reason to drop a save (T-13-39, CLOUD-05).
         */
        private suspend fun exceedsServerCap(
            client: SnapshotsClient,
            payload: ByteArray,
        ): Boolean {
            val cap =
                runCatching { client.getMaxDataSize().await() }
                    // A cancel is not a failed query: absorbing it here would let a stopped pass
                    // go on to commit (WR-04).
                    .onFailure { if (it is CancellationException) throw it }
                    .getOrNull() ?: return false
            return payload.size > cap
        }

        /**
         * Closes an opened [snapshot] without writing to it, swallowing whatever the SDK says.
         *
         * Used on the read path and on every failure path. The `runCatching` is not laziness: this
         * runs from a `finally`, frequently while another failure is already propagating, and a
         * throw from a cleanup call would replace the real cause with the noise from tidying up
         * after it.
         *
         * **`NonCancellable`, so the `runCatching` has no caller's cancel to swallow.** A cancelled
         * pass is exactly when a handle most needs closing, and a cancellable await would abandon
         * the close half-way. The cancel is not lost: it is still pending on the caller's job, a
         * `finally` lets it keep propagating, and the two happy paths re-check it with
         * `ensureActive()` straight after the close.
         *
         * A close the SDK refuses stays silent here, and the handle may then really be open. That
         * is visible one push later as [CloudSaveStage.OPEN_FAILED] with the SDK's status integer.
         */
        private suspend fun discardQuietly(
            client: SnapshotsClient,
            snapshot: Snapshot,
        ) {
            withContext(NonCancellable) {
                runCatching { client.discardAndClose(snapshot).await() }
            }
        }

        /**
         * A failed SDK await, reduced to the [CloudSaveOutcome] it should be reported as.
         *
         * Private to this file and caught only by [save]; on the read path [load]'s own
         * `runCatching` turns it into the same `null` every other read failure becomes. It has no
         * message and no cause on purpose — see [awaitStage].
         */
        private class StageFailure(
            val outcome: CloudSaveOutcome,
        ) : Exception()
    }

/**
 * `ApiException.statusCode` when [failure] is one, else `null`. The only datum ever read off an
 * SDK failure. File-level because it reads no state of the store at all.
 */
private fun statusCodeOf(failure: Throwable): Int? = (failure as? ApiException)?.statusCode

/**
 * An **open** handle plus how many conflicts it took to get it — what `openResolved` yields.
 *
 * File-private, never stored in a field and never returned across the seam: it holds an SDK type,
 * and only the count ever leaves this file, inside a [CloudPullProbe].
 */
private class OpenedSnapshot(
    val snapshot: Snapshot,
    val conflictsResolved: Int,
)

/**
 * What [opened] says about itself, or `null` if the SDK would not say.
 *
 * Three reads and no others. In particular **not** `getDeviceName()`, `getDescription()`,
 * `getUniqueName()`, `getSnapshotId()`, `getOwner()` or the cover URI: each is a string tied to a
 * player or a device, and the probe type has no slot for one (T-13-40). See [CloudPullProbe] for
 * why the device name specifically was specified and then dropped. File-level, like [statusCodeOf],
 * because it reads no state of the store.
 */
private fun probeOf(opened: OpenedSnapshot): CloudPullProbe? =
    runCatching {
        val metadata = opened.snapshot.metadata
        CloudPullProbe(
            changePending = metadata.hasChangePending(),
            conflictsResolved = opened.conflictsResolved,
            lastModifiedAt = metadata.lastModifiedTimestamp,
        )
    }.getOrNull()
