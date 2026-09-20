package com.app.pegsolitaire.data.cloud

import kotlinx.serialization.Serializable

/**
 * The serialized shape of the cloud payload (CLOUD-01) — the exact bytes written to the
 * Play Games saved game, and the exact bytes read back from another device.
 *
 * `@Serializable` lives **here and nowhere else**: `:domain` is pure Kotlin with zero
 * production dependencies (QUAL-01, RESEARCH Pitfall 9), so
 * [com.app.pegsolitaire.domain.progress.CloudProgress] carries no annotations and this
 * class mirrors it, exactly as `ResumeSnapshotDto` mirrors the resume model.
 *
 * **There is no board field, no move history and no resume field, and there never will be
 * (CLOUD-04).** That is not a convention to be remembered — `CloudProgressCodecTest`
 * asserts set equality over this type's serial descriptor, so adding one fails the build.
 * A payload type that cannot carry an in-progress game cannot leak one.
 *
 * ## The compatibility contract (D-07)
 *
 * Two directions, covered by two different mechanisms:
 *  - **Forward** — a payload from a *newer* build carrying fields this build has never
 *    heard of decodes anyway, because [CloudProgressCodec]'s `Json` sets
 *    `ignoreUnknownKeys`.
 *  - **Backward** — a payload from an *older* build missing a field this build expects
 *    decodes anyway, because **every field added after v1 must be nullable with a
 *    default**. Same rule, same reason, as `ResumeSnapshotDto`: without it, shipping one
 *    new field would orphan every cloud save already written.
 *
 * A payload declaring a [schemaVersion] *greater* than [CloudPayloadKeys.SCHEMA_VERSION]
 * is the one case neither mechanism covers, and it is refused outright rather than
 * partially read — see [CloudProgressCodec.decode].
 *
 * @property schemaVersion The writer's payload schema. Defaults to
 *   [CloudPayloadKeys.SCHEMA_VERSION_UNKNOWN] so a payload written before this field
 *   existed reads as "older", never as "newer".
 * @property ladder The player's next Challenge level (D-08).
 * @property playedTimeMs Total solved-play time in millis (D-10).
 * @property bests Every level's stored bests, one entry per `(mode, levelId)`.
 */
@Serializable
internal data class CloudProgressDto(
    val schemaVersion: Int = CloudPayloadKeys.SCHEMA_VERSION_UNKNOWN,
    val ladder: Int,
    val playedTimeMs: Long,
    val bests: List<LevelBestDto> = emptyList(),
)

/**
 * One level's bests on the wire.
 *
 * Flattened into a list rather than a JSON object keyed by a composed `"mode:levelId"`
 * string: a composed key would be a second, ambiguous encoding of
 * [com.app.pegsolitaire.domain.progress.LevelKey], and a level id containing the separator
 * would silently split. Mode and id travel as their own fields and are re-paired at decode.
 *
 * Every field is non-null and un-defaulted **on purpose**. These are v1 fields, so absence
 * is corruption rather than an older writer, and a defaulted [bestTimeMs] would be exactly
 * the `0L` that RESEARCH Pitfall 10 warns is the min-merge's absorbing element. A field
 * added *later* must follow [CloudProgressDto]'s nullable-with-a-default rule.
 *
 * @property mode [CloudPayloadKeys.MODE_CLASSIC] or [CloudPayloadKeys.MODE_CHALLENGE];
 *   any other token drops the row.
 * @property levelId The Classic catalog id, or the Challenge level number as decimal text.
 * @property stars Stars earned, clamped into range at decode.
 * @property bestTimeMs Fastest solve in millis; a value at or below zero drops the row.
 * @property bestScore Highest score; a negative value drops the row.
 */
@Serializable
internal data class LevelBestDto(
    val mode: String,
    val levelId: String,
    val stars: Int,
    val bestTimeMs: Long,
    val bestScore: Int,
)

/**
 * The payload's **wire contract**: the schema version and the two mode tokens.
 *
 * In the voice of `ResumeSnapshotFormat`'s "these indices are an on-disk contract":
 * **these strings are an on-the-wire contract.** Changing one changes how every saved game
 * already sitting in every player's Play Games account is read.
 *
 * They are deliberately **decoupled from the Room mode literals** in the `data` package's
 * persistence-keys object, which happen to spell the same two words today (RESEARCH
 * Pitfall 11). Those are SQLite *column values*; these are network bytes. Importing the
 * database constants here would couple two formats that must be free to change apart —
 * renaming a column value is a local schema migration, whereas renaming a wire token would
 * silently orphan every existing cloud save, on devices this build will never run on. The
 * duplication is the point: a build gate greps this package for that object's name, and
 * `CloudProgressCodecTest` pins both tokens by their literal text, so a well-meaning
 * de-duplication fails the build twice over.
 *
 * `:domain`'s `ProgressMode` enum is a third, independent thing again: it is what the pure
 * layer speaks, and it never learns a string.
 */
internal object CloudPayloadKeys {
    /** The payload schema this build writes. Bump only for a genuinely breaking change. */
    const val SCHEMA_VERSION = 1

    /**
     * The version a payload written before the field existed reads as.
     *
     * Zero rather than one, so "absent" and "explicitly v1" stay distinguishable, and so
     * the `schemaVersion > SCHEMA_VERSION` refusal can never fire on an older writer.
     */
    const val SCHEMA_VERSION_UNKNOWN = 0

    /** Wire token for [com.app.pegsolitaire.domain.progress.ProgressMode.CLASSIC]. */
    const val MODE_CLASSIC = "classic"

    /** Wire token for [com.app.pegsolitaire.domain.progress.ProgressMode.CHALLENGE]. */
    const val MODE_CHALLENGE = "challenge"
}
