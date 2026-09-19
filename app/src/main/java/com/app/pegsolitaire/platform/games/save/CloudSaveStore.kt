package com.app.pegsolitaire.platform.games.save

/**
 * The platform seam through which the game reads and writes its Play Games **Saved Games**
 * payload (CLOUD-01, CLOUD-05, PLAT-04).
 *
 * A **sibling** of [com.app.pegsolitaire.platform.games.GamesService], deliberately, and not a
 * pair of methods on it. That interface's own KDoc states that cloud save "is explicitly out of
 * scope here — it arrives as a second data source behind `ProgressRepository`, not as a method on
 * this seam", and this file is what honouring that sentence looks like. The two seams answer
 * different questions — *who is the player* versus *what bytes are theirs* — and a `saveToCloud()`
 * on the games seam would put a storage concern behind a sign-in interface that Phase 11 spent an
 * entire phase keeping minimal.
 *
 * **Bytes in, bytes out.** Every signature below names only Kotlin/JVM types: `ByteArray`, `Long`,
 * and two local `data class`es — three `Int`s going in, an enum and a nullable `Int` coming out.
 * Nothing `Snapshot`-shaped, no
 * `SnapshotsClient`, and — exactly as on the games seam — **no Activity type and no Play Games SDK
 * type in any signature**, so no ViewModel and no repository can obtain one through it. That is not
 * a convention: `PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks` matches the Play/AdMob/UMP SDK
 * package roots anywhere outside `..platform..` at the **bytecode** level, which
 * includes the Hilt-generated `DaggerApp_HiltComponents_SingletonC` and its per-binding
 * `Provider<T>` field. A `Provider<SnapshotsClient>` reachable from the graph would fail the gate no
 * matter which module declared it (proven empirically in plan 09-05). Keeping the seam SDK-free is
 * what makes the containment structural rather than a habit.
 *
 * **Failure is a return value, not an exception (CLOUD-05, PGSL-04)** — the same contract
 * [com.app.pegsolitaire.platform.games.GamesService.submitScore] carries, for the same reason: the
 * caller must be able to *observe* a failure and record it, and an exception thrown into the
 * application-scoped coroutine that drives the push would take the process with it rather than the
 * sync pass.
 *
 * ## Why [mergeRemote] is a parameter on **both** methods
 *
 * The merge is `ProgressMerge`, a pure `:domain` function over `CloudProgress`, and the platform
 * layer must never learn what a `CloudProgress` is — the payload is opaque bytes here and is
 * decoded only in `data/cloud`. Passing the merge in as a pure, synchronous
 * `(ByteArray, ByteArray) -> ByteArray` is the only way the **conflict** path can reuse the exact
 * merge the local path runs, without moving serialization into `platform/`. The first argument is
 * the payload already on the server, the second the payload being written.
 *
 * [load] needs it too, and that is the non-obvious half. An `open` can come back **conflicted on a
 * read**: two devices wrote while this one was offline, and the SDK hands back both versions rather
 * than picking one. Resolving that conflict writes merged bytes back to the server, so resolving it
 * with anything other than the real merge — "take the newest", "take mine" — would discard one
 * device's progress during a *pull*, which is precisely the data loss CLOUD-02 forbids.
 *
 * ## Offline commits report success, and that is correct (RESEARCH Pitfall 4)
 *
 * Play Games Services caches a `commitAndClose` locally when the device has no connectivity and
 * syncs it on reconnection, so [save] can legitimately report a commit before anything has reached
 * Google's servers. The data is not lost — GMS owns that retry queue, and the SDK exposes no API
 * through which an app could observe its state. An app-level "did it really upload" check would
 * therefore duplicate a queue Google already runs while being unable to see the thing it claims to
 * verify. This paragraph exists so a future reader does not "fix" it.
 */
interface CloudSaveStore {
    /**
     * Reads the player's cloud payload, or `null` when there is none or it could not be read.
     *
     * **`null` means "stay local" (CLOUD-05), not "the player has no progress."** The two are
     * indistinguishable from here and must therefore be treated as the same instruction: leave the
     * local store exactly as it is. A caller that read `null` as "the cloud is empty" and pushed a
     * fresh device's empty payload over it would erase a real save — the single most damaging
     * misreading of this signature.
     *
     * **Never throws.** Offline, opted out, signed out, no foreground Activity, a Play Console
     * project with Saved Games disabled, and the SDK's own contents-unavailable failure all collapse
     * to `null`. An implementation classifies inside `platform/` and reports nothing further from
     * a read; the one thing the seam does report is [save]'s [CloudSaveOutcome]. `open` is shared by
     * both methods, so a push outcome diagnoses a failing pull as well.
     *
     * @param mergeRemote resolves a conflict encountered while opening: given the two conflicting
     *   payloads' bytes it returns the merged bytes, which are written back to the server as part of
     *   resolving. See the class KDoc for why a read needs a merge at all.
     * @return the payload bytes, or `null`.
     */
    suspend fun load(mergeRemote: (ByteArray, ByteArray) -> ByteArray): ByteArray?

    /**
     * Writes [bytes] as the player's cloud payload, along with the listing metadata that makes the
     * save legible in the Play Games saves UI (CLOUD-03), reporting whether the SDK accepted it and,
     * when it did not, which stage refused.
     *
     * **An uncommitted outcome is not an error.** It means "nothing reached the cloud", and the
     * caller's answer is to set the persisted cloud-push-pending marker and retry on the next pass
     * (D-01) — never a
     * snackbar, never a blocked level, exactly as
     * [com.app.pegsolitaire.platform.games.GamesService.submitScore]'s KDoc describes for
     * submissions. The player is never shown a cloud failure.
     *
     * @param description the three numbers the listing line is built from; see [CloudSaveDescription].
     * @param playedTimeMs total played time recorded on the snapshot, which is also what the SDK's
     *   longest-playtime resolution policy would order by.
     * @param mergeRemote resolves a conflict: given the server's payload and [bytes], returns the
     *   merged payload. See the class KDoc.
     * @return an outcome whose [CloudSaveOutcome.committed] is `true` when the SDK accepted the
     *   commit — which offline means "queued locally and will sync", per the class KDoc — and
     *   otherwise names the stage that ended the push plus the SDK's status integer where one
     *   existed. Never an exception message, an id, the description or the payload.
     */
    suspend fun save(
        bytes: ByteArray,
        description: CloudSaveDescription,
        playedTimeMs: Long,
        mergeRemote: (ByteArray, ByteArray) -> ByteArray,
    ): CloudSaveOutcome
}

/**
 * The three numbers the cloud save's one-line description is built from (D-11): the Challenge
 * ladder position, the total stars earned, and how many Classic boards are cleared.
 *
 * **Three `Int`s and not a formatted `String`, deliberately.** Formatting the line requires a
 * `Context` to reach `strings.xml`, and `data/` — which owns the aggregates these numbers come from
 * — must never touch one. Carrying the raw numbers across the seam keeps the wording in
 * `strings.xml` where a translator can reach it, and means the description **follows the device
 * locale at commit time**: a player who changes language sees the new language on their next solve,
 * rather than a line frozen in whatever locale was active when the save was first written.
 *
 * Its only consumer is plan 13-07's formatter inside `platform/`, which turns it into the localized
 * string handed to `SnapshotMetadataChange`.
 *
 * @property ladder the highest Challenge level reached.
 * @property totalStars the sum of stars across every solved level.
 * @property classicCleared how many Classic boards have been cleared.
 */
data class CloudSaveDescription(
    val ladder: Int,
    val totalStars: Int,
    val classicCleared: Int,
)
