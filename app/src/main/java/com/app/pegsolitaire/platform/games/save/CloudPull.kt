package com.app.pegsolitaire.platform.games.save

/**
 * What [CloudSaveStore.load] hands back: the payload, and what the opened snapshot said about
 * itself.
 *
 * The two are independent. [bytes] is `null` for every "nothing was read" cause (CLOUD-05); [probe]
 * is `null` only when the snapshot never opened at all. A brand-new account therefore has no bytes
 * and *does* have a probe — "there is nothing here" is a fact about an opened snapshot, and it is
 * a different fact from "the open was refused".
 *
 * A plain class rather than a `data class`: a `ByteArray` property would give it identity-based
 * `equals` while looking like it had structural equality, which is the worse of the two.
 *
 * @property bytes the payload, or `null` — read it as "stay local", never as "the cloud is empty".
 * @property probe the pull diagnostic, or `null` when no snapshot was opened. **Nothing may branch
 *   on it**: it exists to be persisted for a developer and for nothing else.
 */
class CloudPull(
    val bytes: ByteArray?,
    val probe: CloudPullProbe?,
) {
    companion object {
        /** Nothing was read and nothing was opened: every failed pull, and every stub. */
        val NOTHING = CloudPull(bytes = null, probe = null)
    }
}

/**
 * What one pull observed about the snapshot it opened — the read path's counterpart to
 * [CloudSaveOutcome], and a device diagnostic in exactly the same sense.
 *
 * It exists because `COMMITTED` is a **local** fact. `commitAndClose` completes once the change is
 * stored on the device and a background sync has been *requested*; two devices both reported
 * `COMMITTED` throughout a pass in which one's progress never reached the other. The SDK does
 * expose the other half — `SnapshotMetadata.hasChangePending()`, *"changes pending that have not
 * been uploaded to the server"* — and the next pull is where it can be read.
 *
 * ## What [changePending] does NOT mean (the negative three reports got wrong)
 *
 * `changePending = true` is **not** "this device cannot upload", and it is a far weaker oracle than
 * the investigation that asked for it assumed. It was measured `true` across three launches on a
 * device whose payload had demonstrably already reached the other device — because each launch's
 * push guard produced a *fresh* local commit, so the flag was true about a newer commit than the one
 * whose content had landed. It is a fact about the snapshot at the moment of the open, not a verdict
 * about the device.
 *
 * The usable signal is the **conjunction**: `changePending = false` **and** [lastModifiedAt] equal
 * to this device's own last push. Every conclusion the phase records rests on that pair; nothing may
 * be concluded from the flag alone, in either direction.
 *
 * ## Why this is not a leak (T-11-04 / T-13-40)
 *
 * The same argument as [CloudSaveOutcome], and again a property of the **type**: a boolean, an
 * `Int` and a `Long`. There is no `String`, so no device name, snapshot id, description, player or
 * payload can ride along. `CloudPullProbeTest` asserts the field types so that stays true.
 *
 * ## Why there is no "written by this device" flag
 *
 * One was specified, as `SnapshotMetadata.getDeviceName() == Build.MODEL`, and was dropped rather
 * than guessed at. The 22.0.0 javadoc says only *"the name of the device that wrote this snapshot,
 * if known … or null if not known"*: the format is undocumented, it is not stated to be
 * `Build.MODEL`, and it may be absent. Worse, equality could not discriminate where it matters —
 * two emulators of the same system image share one `Build.MODEL`, so on the very devices the
 * diagnostic was wanted for it would read `true` on both. A boolean that is confidently wrong is
 * worse than a missing one. [lastModifiedAt] carries the usable part of the signal: compared with
 * the time of this device's own last push, it says whether somebody else wrote since.
 *
 * @property changePending whether the opened snapshot still held changes that had not been uploaded
 *   — a fact about that snapshot, never about the device. See the negative above before reading it.
 * @property conflictsResolved how many conflicts this pull resolved before the snapshot opened.
 * @property lastModifiedAt the opened snapshot's last-modified time, in epoch millis.
 */
data class CloudPullProbe(
    val changePending: Boolean,
    val conflictsResolved: Int,
    val lastModifiedAt: Long,
)
