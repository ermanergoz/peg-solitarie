package com.app.pegsolitaire.platform.games.save

/**
 * What [CloudSaveStore.save] reports about one push: the stage it ended at and, where the SDK
 * supplied one, its status integer.
 *
 * It exists because a bare `false` could not be diagnosed. On two signed-in, online devices every
 * push was refused, and the only evidence was a pending marker that stayed set — a disabled
 * feature, a refused `open`, a rejected commit and a missing Activity were all the same `false`,
 * and logging, the obvious way to tell them apart, is exactly what this package forbids.
 *
 * ## Why this is not a leak (T-11-04 / T-13-40)
 *
 * Those threats protect the player id, the display name, the save's description, its payload and
 * the text of SDK exceptions. An outcome can hold none of them, and that is a property of its
 * **type** rather than of any implementation's restraint: the only slots are an enum of this
 * game's own stage names and one nullable `Int`. There is no `String` to put a message, an id or a
 * description in. `CloudSaveOutcomeTest` asserts the field types so that stays true.
 *
 * Kotlin/JVM types only, like everything else on the seam: [statusCode] is the *value* of
 * `ApiException.statusCode`, copied out inside `platform/`, never the exception itself — so
 * `PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks` has nothing to find here.
 *
 * Nothing in the app reads it back. CLOUD-05 and D-03 are untouched: a failed push is still
 * silent, still a return value, still a retry on the next pass.
 *
 * @property stage where the push ended.
 * @property statusCode the SDK's status integer when the failure carried one, else `null`. It is
 *   the datum that actually discriminates — [CloudSaveStage.OPEN_FAILED] alone cannot separate a
 *   disabled feature from a network or an auth refusal.
 */
data class CloudSaveOutcome(
    val stage: CloudSaveStage,
    val statusCode: Int? = null,
) {
    /** Whether the SDK accepted the commit — the one question the retry bookkeeping asks. */
    val committed: Boolean get() = stage == CloudSaveStage.COMMITTED
}

/**
 * The stages a push can end at, in the order the push passes through them.
 *
 * **The names are persisted** (as the last push's stage, for a device diagnostic) and quoted in
 * QA reports, so renaming one changes the meaning of evidence already written down.
 * `CloudSaveOutcomeTest` pins them.
 */
enum class CloudSaveStage {
    /** The SDK accepted the commit — which offline means "queued locally and will sync". */
    COMMITTED,

    /** No foreground Activity arrived within the bound, so the SDK was never reached. */
    NO_ACTIVITY,

    /** The SDK refused or failed the snapshot `open`. */
    OPEN_FAILED,

    /** A conflict never converged within the retry bound, or resolving it failed. */
    CONFLICT_UNRESOLVED,

    /** The payload exceeds the size this server reported it will accept; nothing was committed. */
    OVER_CAP,

    /** The SDK refused or failed the commit. */
    COMMIT_FAILED,

    /** Anything else: a failure outside the three SDK awaits, a throwing store, or a stub. */
    FAILED_OTHER,
}
