package com.app.pegsolitaire.platform.games.save

/**
 * A settable in-memory [CloudSaveStore] for the Phase 13 JVM suite — the test seam that makes
 * CLOUD-01/02/05 verifiable without a Play Console project, a signed-in device or an Activity.
 *
 * It lives beside the interface it fakes, exactly as
 * [com.app.pegsolitaire.platform.games.FakeGamesService] does and for the same reason: two unrelated
 * packages need it. Plan 13-08's `ProgressSubmitter` tests drive the push and pull passes against it,
 * and plan 13-10's end-to-end round-trip test drives the whole payload path through it. Record and
 * assert, with no MockK — matching the shipped `FakeGamesService` so the existing test style carries
 * straight over.
 *
 * ## Why `save` deliberately invokes [CloudSaveStore.save]'s `mergeRemote`
 *
 * This is the one place in the entire JVM suite where the **conflict merge actually runs**. In
 * production that lambda is invoked from inside the SDK's conflict-resolution path, which no offline
 * test can reach, so a fake that accepted the lambda and ignored it would leave the conflict path
 * completely unexercised — and every test in the project would stay green over a merge that
 * discarded a device's progress. Invoking it here is what turns "the conflict path is covered" from
 * a claim into a fact.
 *
 * [remote] therefore behaves like the server's copy: [save] merges the incoming payload against it,
 * and — crucially — advances it **only when [saveOutcome] is committed**. A caller's retry bookkeeping is
 * written against precisely that, since a failed push must leave the server where it was so the next
 * pass merges against the same bytes rather than against its own failed write.
 *
 * ## The throwing knobs
 *
 * [throwOnLoad] and [throwOnSave] exist so a caller test can prove the seam's fail-quiet contract
 * end to end: a throwing store must be **indistinguishable** from one that returns `null` or an
 * uncommitted outcome (CLOUD-05, T-13-21). The real implementation never throws, but the caller must not depend on that
 * — an SDK exception escaping into the application-scoped coroutine that drives the sync pass would
 * take the process rather than the pass.
 *
 * Every recording property is exposed read-only with a private setter, so a test observes them and
 * never seeds them. [loadCallCount] and [saveCallCount] are both counted, and both are needed: the
 * profile-gate assertion (RESEARCH Pitfall 8) is "**zero** store calls", which is a statement about
 * the pair, not about either one alone.
 *
 * @param remote the payload the "server" currently holds; `null` for a player with no cloud save.
 * @param saveOutcome what [save] reports — stage and status code, verbatim — and therefore whether
 *   [remote] advances.
 * @param throwOnSave when set, thrown out of [save] instead of returning.
 * @param throwOnLoad when set, thrown out of [load] instead of returning.
 */
class FakeCloudSaveStore(
    var remote: ByteArray? = null,
    var saveOutcome: CloudSaveOutcome = CloudSaveOutcome(CloudSaveStage.COMMITTED),
    var throwOnSave: Throwable? = null,
    var throwOnLoad: Throwable? = null,
) : CloudSaveStore {
    /** How many times [load] was called — proves a pull reached the seam, or proves it did not. */
    var loadCallCount = 0
        private set

    /** How many times [save] was called, including calls that reported an uncommitted outcome. */
    var saveCallCount = 0
        private set

    /** Every payload [save] committed, in order, **after** the merge — never the raw input. */
    var savedPayloads: List<ByteArray> = emptyList()
        private set

    /** The listing metadata from the most recent [save] (CLOUD-03, D-11). */
    var lastDescription: CloudSaveDescription? = null
        private set

    /** The played time from the most recent [save]. */
    var lastPlayedTimeMs: Long? = null
        private set

    /**
     * Returns whatever [remote] currently holds, after [throwOnLoad].
     *
     * The `mergeRemote` lambda is not invoked: a conflict on a *read* is an SDK-side event this fake
     * has no way to simulate, and the merge itself is already exercised through [save].
     */
    override suspend fun load(mergeRemote: (ByteArray, ByteArray) -> ByteArray): ByteArray? {
        loadCallCount++
        throwOnLoad?.let { throw it }
        return remote
    }

    /**
     * Merges [bytes] against [remote] through the caller's own `mergeRemote`, records the merged
     * payload and the listing metadata, and advances [remote] only when [saveOutcome] is committed.
     *
     * With no [remote] the payload is recorded verbatim — there is nothing to merge against, which is
     * the first-push case.
     */
    override suspend fun save(
        bytes: ByteArray,
        description: CloudSaveDescription,
        playedTimeMs: Long,
        mergeRemote: (ByteArray, ByteArray) -> ByteArray,
    ): CloudSaveOutcome {
        saveCallCount++
        throwOnSave?.let { throw it }
        val merged = remote?.let { mergeRemote(it, bytes) } ?: bytes
        savedPayloads = savedPayloads + merged
        lastDescription = description
        lastPlayedTimeMs = playedTimeMs
        if (saveOutcome.committed) {
            remote = merged
        }
        return saveOutcome
    }
}
