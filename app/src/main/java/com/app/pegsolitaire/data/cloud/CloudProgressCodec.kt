package com.app.pegsolitaire.data.cloud

import com.app.pegsolitaire.domain.achievement.AchievementThresholds
import com.app.pegsolitaire.domain.generator.ChallengeLevelBounds
import com.app.pegsolitaire.domain.progress.CloudProgress
import com.app.pegsolitaire.domain.progress.LevelBest
import com.app.pegsolitaire.domain.progress.LevelKey
import com.app.pegsolitaire.domain.progress.ProgressMerge
import com.app.pegsolitaire.domain.progress.ProgressMergeLimits
import com.app.pegsolitaire.domain.progress.ProgressMode
import kotlinx.serialization.json.Json

/**
 * The cloud payload's codec, and the phase's **trust boundary** (CLOUD-01 / CLOUD-05).
 *
 * `SnapshotMapper`'s role, one layer out: the pure `:domain` model on one side, bytes on
 * the other, and every untrusted field checked in between. The difference is where the
 * bytes come from — a resume blob is app-written and merely corruptible, whereas a saved
 * game arrives **over the network**, written by another device, and is fully
 * attacker-influenceable by anyone with access to the player's account (ASVS V5).
 *
 * ## Drop the row, never the payload
 *
 * [decode] validates every row and **drops** the invalid ones rather than refusing the
 * whole payload, because one bad row must never cost a player their other 9,999
 * (CLOUD-05). Only two things refuse outright: bytes that are not a readable payload at
 * all, and a payload declaring a schema this build cannot claim to understand. Both return
 * `null`, which every caller reads as "stay local" — the same fail-open posture
 * `StorageFailSafe` applies to every local read.
 *
 * Refusing is the *wrong* default here in a way worth stating: an attacker who can make
 * the decoder reject a payload can delete a player's cross-device progress with a one-byte
 * edit. Dropping a row costs them one level's best; refusing the payload costs them all of
 * them.
 *
 * ## Never throws, never logs
 *
 * [decode] is total: every malformed-input failure kotlinx.serialization raises is an
 * `IllegalArgumentException` (`SerializationException` extends it), so a single catch
 * covers them all — the same single catch `SnapshotMapper.decode` uses. And nothing in
 * this package writes to logcat: a payload is player data, and player figures have no
 * business in a log buffer (T-13-14, extending Phase 11's T-11-04).
 */
@Suppress("TooManyFunctions") // one private validator per untrusted field; merging any two would hide a rejection rule
object CloudProgressCodec {
    /** Unknown keys are ignored so a payload from a newer build still decodes (D-07). */
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * The smallest `bestTimeMs` a real solve can carry, and therefore the decode-time
     * floor: anything below it drops the row.
     *
     * Named rather than written as a bare `> 0` because the zero is a *policy*, not an
     * arithmetic bound. `bestTimeMs` merges by `min`, for which `0L` is the **absorbing**
     * element, not the identity (RESEARCH Pitfall 10): a single row carrying zero — from a
     * defaulting writer, a corruption, or a tamperer — would pin that level's best time at
     * zero on this device, then propagate it to the cloud, then to every other device the
     * player signs into, permanently, because a monotone merge can never walk a value back
     * up. `ProgressMergeLimits.NO_TIME_RECORDED` is the matching identity on the merge
     * side; this is the gate that stops the absorbing value ever reaching it.
     */
    private const val MIN_RECORDED_BEST_TIME_MS = 1L

    /**
     * Total ordering of payload rows: Classic before Challenge, Challenge numerically, and
     * anything that will not parse as a number by its text.
     *
     * Its purpose is **stability**, not presentation. The same progress must always produce
     * the same bytes, so that re-encoding an unchanged payload is a no-op the conflict path
     * can recognise, and so that the round-trip and idempotence tests assert something real
     * rather than an accident of `HashMap` iteration order.
     */
    private val wireOrder =
        compareBy<Map.Entry<LevelKey, LevelBest>>(
            { it.key.mode.ordinal },
            { it.key.levelId.toIntOrNull() ?: Int.MAX_VALUE },
            { it.key.levelId },
        )

    /**
     * [progress] as the bytes written to the saved game — stable, UTF-8, unbounded.
     *
     * Use [encodeWithinCap] for anything that will actually be uploaded; this is the raw
     * encoding the trim loop measures.
     */
    fun encode(progress: CloudProgress): ByteArray = json.encodeToString(progress.toDto()).toByteArray(Charsets.UTF_8)

    /**
     * [progress] encoded to fit inside [maxBytes], dropping the **lowest Challenge levels**
     * — and only those — until it does (D-05).
     *
     * A safety valve that is expected never to fire: at roughly 40 bytes per level, the
     * full 10,000-level ladder is ~400 KB against a [CloudPayloadLimits.TRIM_TARGET_BYTES]
     * of ~2.75 MB. It exists because a payload the server rejects for size can never be
     * pushed *at all*, which would silently freeze a player's cloud save forever.
     *
     * ## Why it terminates
     *
     * This is the only loop in the phase whose exit condition depends on data, so the
     * argument is worth stating rather than trusting:
     *  - Every iteration drops **at least one** Challenge row (the drop count is floored at
     *    1), and `ProgressMerge.dropLowestChallenge` never removes a Classic row, the
     *    ladder or the played time.
     *  - So the Challenge row count strictly decreases every pass, and the loop runs at
     *    most `challengeRowCount` times.
     *  - When it reaches zero, the `challengeRows == 0` branch returns unconditionally.
     * The drop count is proportional to the overshoot rather than fixed at one, so a wildly
     * oversized payload converges in a handful of encodes instead of thousands.
     *
     * ## The honest failure
     *
     * If the Classic rows plus the two scalars alone exceed [maxBytes], the encoding is
     * returned **over the cap** rather than trimmed further. That case is unreachable in
     * practice — five rows — and if it ever were reachable, losing a player's hand-played
     * Classic bests to satisfy a byte budget would be the wrong trade. A push that fails is
     * recoverable; deleted progress is not.
     */
    fun encodeWithinCap(
        progress: CloudProgress,
        maxBytes: Int = CloudPayloadLimits.TRIM_TARGET_BYTES,
    ): ByteArray {
        var current = progress
        var encoded = encode(current)
        while (encoded.size > maxBytes) {
            val challengeRows = current.bests.count { it.key.mode == ProgressMode.CHALLENGE }
            if (challengeRows == 0) return encoded
            current = ProgressMerge.dropLowestChallenge(current, dropCount(challengeRows, encoded.size, maxBytes))
            encoded = encode(current)
        }
        return encoded
    }

    /**
     * How many Challenge rows to drop on this pass: the share of the payload that overshoots
     * [maxBytes], plus one, and never fewer than one.
     *
     * The `+ 1` and the floor are both load-bearing. Integer division rounds toward zero, so
     * a small overshoot would otherwise compute zero rows and the loop would never make
     * progress — the one way this could spin.
     */
    private fun dropCount(
        challengeRows: Int,
        encodedSize: Int,
        maxBytes: Int,
    ): Int = maxOf(1, challengeRows * (encodedSize - maxBytes) / encodedSize + 1)

    /**
     * The progress [bytes] describe, or `null` when they are not a payload this build can
     * read — which every caller treats as "stay local, lose nothing" (CLOUD-05).
     *
     * `null` means exactly two things:
     *  - the bytes are not decodable JSON at all (corrupt, truncated, empty, not UTF-8), or
     *  - the payload declares a [CloudProgressDto.schemaVersion] **newer** than this build
     *    writes. Reading a newer payload on its subset of known fields would look like
     *    success while silently discarding whatever the newer build added — and because the
     *    merge is monotone, the next push would write that lossy subset back over the
     *    richer original. A failed load leaves the cloud untouched; a partial one corrupts
     *    it (D-07 / D-11).
     *
     * Everything else is repaired rather than refused. `stars` is clamped, the ladder is
     * clamped through [ChallengeLevelBounds.clamp] (whose own "fail-safe, not fail-closed"
     * rule this follows), a negative played time floors to its identity, and an individual
     * row that fails validation is dropped while every other row survives.
     */
    fun decode(bytes: ByteArray): CloudProgress? {
        val dto = parseOrNull(bytes) ?: return null
        return if (dto.schemaVersion > CloudPayloadKeys.SCHEMA_VERSION) null else dto.toCloudProgress()
    }

    /**
     * The union of the payloads [left] and [right] describe, re-encoded — the exact
     * byte-array-in, byte-array-out lambda the Saved Games conflict loop needs.
     *
     * It lives here rather than in `platform/` for one reason: `platform/` is handed two
     * opaque snapshots and must produce a third without ever learning what a
     * [CloudProgress] is. This is the only shape that lets it.
     *
     * An undecodable side contributes [CloudProgress.EMPTY] — the merge's identity — rather
     * than discarding the other side. That is the fail-open rule again, and it matters
     * most here: a conflict whose remote half is corrupt must still preserve the local
     * half, not resolve to nothing.
     *
     * The result goes through [encodeWithinCap], not [encode]: a merge is a *union* and is
     * therefore the one operation that can grow a payload, so resolving a conflict must
     * never produce bytes a plain push would have refused.
     */
    fun mergeBytes(
        left: ByteArray,
        right: ByteArray,
    ): ByteArray {
        val merged = ProgressMerge.merge(left.decodeOrEmpty(), right.decodeOrEmpty())
        return encodeWithinCap(merged)
    }

    /** [decode], with the merge identity standing in for bytes that cannot be read. */
    private fun ByteArray.decodeOrEmpty(): CloudProgress = decode(this) ?: CloudProgress.EMPTY

    /**
     * The parsed payload, or `null` for any malformed input.
     *
     * `SerializationException` is an `IllegalArgumentException`, so this single catch covers
     * every malformed-input failure the library raises — the same catch `SnapshotMapper`
     * uses. Bytes that are not valid UTF-8 need no separate branch: decoding substitutes
     * replacement characters, and the result is not parseable JSON.
     */
    private fun parseOrNull(bytes: ByteArray): CloudProgressDto? =
        try {
            json.decodeFromString<CloudProgressDto>(bytes.toString(Charsets.UTF_8))
        } catch (ignored: IllegalArgumentException) {
            null
        }

    private fun CloudProgress.toDto(): CloudProgressDto =
        CloudProgressDto(
            schemaVersion = CloudPayloadKeys.SCHEMA_VERSION,
            ladder = ladder,
            playedTimeMs = playedTimeMs,
            bests = bests.entries.sortedWith(wireOrder).map { it.toDto() },
        )

    private fun Map.Entry<LevelKey, LevelBest>.toDto(): LevelBestDto =
        LevelBestDto(
            mode = key.mode.toWireToken(),
            levelId = key.levelId,
            stars = value.stars,
            bestTimeMs = value.bestTimeMs,
            bestScore = value.bestScore,
        )

    /** The wire token for this mode — the one place the enum becomes a string. */
    private fun ProgressMode.toWireToken(): String =
        when (this) {
            ProgressMode.CLASSIC -> CloudPayloadKeys.MODE_CLASSIC
            ProgressMode.CHALLENGE -> CloudPayloadKeys.MODE_CHALLENGE
        }

    private fun CloudProgressDto.toCloudProgress(): CloudProgress =
        CloudProgress(
            ladder = ChallengeLevelBounds.clamp(ladder),
            playedTimeMs = playedTimeMs.coerceAtLeast(ProgressMergeLimits.NO_PLAYED_TIME),
            bests = bests.toValidatedBests(),
        )

    /**
     * Every well-formed row, keyed and merged.
     *
     * Two rows claiming the same level are joined with [ProgressMerge.merge] rather than
     * letting the later one win: a duplicate key is either a writer bug or a deliberate
     * attempt to overwrite a better result with a worse one, and the monotone merge is
     * immune to both.
     */
    private fun List<LevelBestDto>.toValidatedBests(): Map<LevelKey, LevelBest> {
        val validated = mutableMapOf<LevelKey, LevelBest>()
        forEach { row ->
            val entry = row.toEntryOrNull() ?: return@forEach
            val existing = validated[entry.first]
            validated[entry.first] = if (existing == null) entry.second else ProgressMerge.merge(existing, entry.second)
        }
        return validated
    }

    /** This row as a validated map entry, or `null` to drop it. */
    private fun LevelBestDto.toEntryOrNull(): Pair<LevelKey, LevelBest>? {
        val parsedMode = mode.toProgressModeOrNull() ?: return null
        return if (isWellFormed(parsedMode)) LevelKey(mode = parsedMode, levelId = levelId) to toLevelBest() else null
    }

    /**
     * The mode this wire token denotes, or `null` for a token no version of this codec ever
     * wrote — which drops the row rather than guessing a mode for it.
     */
    private fun String.toProgressModeOrNull(): ProgressMode? =
        when (this) {
            CloudPayloadKeys.MODE_CLASSIC -> ProgressMode.CLASSIC
            CloudPayloadKeys.MODE_CHALLENGE -> ProgressMode.CHALLENGE
            else -> null
        }

    /**
     * Whether this row carries values a real play could have produced.
     *
     * The three universal checks are a blank id (no level is nameless), a non-positive best
     * time (see [MIN_RECORDED_BEST_TIME_MS] — the absorbing-element gate) and a negative
     * score. `stars` is deliberately absent: it is clamped rather than rejected, because an
     * out-of-range star count is the one field where the *rest* of the row is still
     * valuable and the repair is unambiguous.
     */
    private fun LevelBestDto.isWellFormed(parsedMode: ProgressMode): Boolean =
        levelId.isNotBlank() &&
            bestTimeMs >= MIN_RECORDED_BEST_TIME_MS &&
            bestScore >= ProgressMergeLimits.NO_SCORE &&
            (parsedMode != ProgressMode.CHALLENGE || hasLegalChallengeLevelId())

    /**
     * Whether this row's id is a Challenge level number inside
     * `ChallengeLevelBounds.MIN..MAX`.
     *
     * Asked **only** of Challenge rows. A Classic id is catalog text (`"english-33"`) and is
     * never number-parsed — doing so would drop every Classic row in the payload.
     */
    private fun LevelBestDto.hasLegalChallengeLevelId(): Boolean =
        levelId.toIntOrNull()?.let { it in ChallengeLevelBounds.MIN..ChallengeLevelBounds.MAX } == true

    /** This row's bests, with `stars` clamped into the only range a play can earn. */
    private fun LevelBestDto.toLevelBest(): LevelBest =
        LevelBest(
            stars = stars.coerceIn(ProgressMergeLimits.NO_STARS, AchievementThresholds.PERFECT_STARS),
            bestTimeMs = bestTimeMs,
            bestScore = bestScore,
        )
}
