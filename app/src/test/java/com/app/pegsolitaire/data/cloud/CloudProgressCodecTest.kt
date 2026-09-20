package com.app.pegsolitaire.data.cloud

import com.app.pegsolitaire.data.ChallengeProgressPreferences
import com.app.pegsolitaire.domain.achievement.AchievementThresholds
import com.app.pegsolitaire.domain.generator.ChallengeLevelBounds
import com.app.pegsolitaire.domain.progress.CloudProgress
import com.app.pegsolitaire.domain.progress.LevelBest
import com.app.pegsolitaire.domain.progress.LevelKey
import com.app.pegsolitaire.domain.progress.ProgressMergeLimits
import com.app.pegsolitaire.domain.progress.ProgressMode
import com.google.common.truth.Truth.assertThat
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.elementNames
import org.junit.Test

/**
 * Pure-JVM coverage of the cloud payload codec (CLOUD-01 / CLOUD-04 / CLOUD-05) — the
 * phase's only trust boundary, and the one place a hostile or corrupt snapshot can be
 * stopped before it reaches the monotonic merge.
 *
 * Four contracts are proven here:
 *  - **Lossless round trip (D-05 / D-08 / D-10).** Every Classic and Challenge best plus
 *    the ladder and the played time survive `encode` → `decode` unchanged.
 *  - **Version tolerance in one direction only (D-07 / D-11).** A payload written by a
 *    newer build decodes on its known fields, but a payload declaring a *newer schema* is
 *    a failed load, never a partial one.
 *  - **Fail-open validation (CLOUD-05, ASVS V5).** Malformed bytes yield `null` and throw
 *    nothing; a single invalid row is dropped while every other row in the same payload
 *    survives. One bad row must never cost a player their other 9,999.
 *  - **The board cannot ride along (CLOUD-04).** Asserted structurally, by set equality
 *    over the serial descriptor's element names — not by convention.
 *
 * Every forward-compatibility fixture is a **raw JSON string carrying extra keys**, never
 * a serialized "future DTO": the point is to exercise the real wire text a newer writer
 * would emit, which a Kotlin type defined here could only approximate.
 */
class CloudProgressCodecTest {
    @Test
    fun encodeThenDecodeRoundTripsEveryField() {
        val progress =
            CloudProgress(
                ladder = LADDER,
                playedTimeMs = PLAYED_TIME_MS,
                bests =
                    mapOf(
                        classicKey(CLASSIC_ID) to LevelBest(stars = 3, bestTimeMs = 9_000L, bestScore = 1_200),
                        challengeKey(7) to LevelBest(stars = 1, bestTimeMs = 44_000L, bestScore = 310),
                    ),
            )

        val decoded = CloudProgressCodec.decode(CloudProgressCodec.encode(progress))

        assertThat(decoded).isEqualTo(progress)
    }

    @Test
    fun encodingIsStableSoTheSameProgressAlwaysProducesTheSameBytes() {
        val progress = progressOf(challengeKey(9), challengeKey(2), classicKey(CLASSIC_ID))
        val reordered = progressOf(classicKey(CLASSIC_ID), challengeKey(2), challengeKey(9))

        val first = CloudProgressCodec.encode(progress).toString(Charsets.UTF_8)
        val second = CloudProgressCodec.encode(reordered).toString(Charsets.UTF_8)

        assertThat(first).isEqualTo(second)
    }

    @Test
    fun unknownFieldsAtEitherLevelStillDecodeEveryKnownField() {
        val decoded = CloudProgressCodec.decode(FORWARD_COMPATIBLE_PAYLOAD.toByteArray(Charsets.UTF_8))

        assertThat(decoded).isNotNull()
        assertThat(decoded!!.ladder).isEqualTo(12)
        assertThat(decoded.playedTimeMs).isEqualTo(5_000L)
        assertThat(decoded.bests[classicKey(CLASSIC_ID)])
            .isEqualTo(LevelBest(stars = 2, bestTimeMs = 8_000L, bestScore = 900))
    }

    @Test
    fun aNewerSchemaVersionIsAFailedLoadNotAPartialOne() {
        val decoded = CloudProgressCodec.decode(NEWER_SCHEMA_PAYLOAD.toByteArray(Charsets.UTF_8))

        assertThat(decoded).isNull()
    }

    @Test
    fun anAbsentSchemaVersionDecodesOnItsKnownFields() {
        val decoded = CloudProgressCodec.decode(NO_SCHEMA_VERSION_PAYLOAD.toByteArray(Charsets.UTF_8))

        assertThat(decoded).isNotNull()
        assertThat(decoded!!.ladder).isEqualTo(4)
        assertThat(decoded.bests).hasSize(1)
    }

    @Test
    fun malformedTruncatedEmptyAndNonUtf8BytesAllDecodeToNull() {
        val malformed = "{ this is not json }".toByteArray(Charsets.UTF_8)
        val truncated = TRUNCATED_PAYLOAD.toByteArray(Charsets.UTF_8)
        val empty = ByteArray(0)
        val nonUtf8 = byteArrayOf(0xC3.toByte(), 0x28, 0xA0.toByte(), 0xA1.toByte())

        assertThat(CloudProgressCodec.decode(malformed)).isNull()
        assertThat(CloudProgressCodec.decode(truncated)).isNull()
        assertThat(CloudProgressCodec.decode(empty)).isNull()
        assertThat(CloudProgressCodec.decode(nonUtf8)).isNull()
    }

    @Test
    fun starsOutsideTheLegalRangeAreClampedRatherThanDroppingTheRow() {
        val decoded = decodeOrFail(OUT_OF_RANGE_STARS_PAYLOAD)

        assertThat(decoded.bests).hasSize(2)
        assertThat(decoded.bests[challengeKey(1)]?.stars).isEqualTo(AchievementThresholds.PERFECT_STARS)
        assertThat(decoded.bests[challengeKey(2)]?.stars).isEqualTo(ProgressMergeLimits.NO_STARS)
    }

    @Test
    fun aRowWithNonPositiveBestTimeIsDroppedAndEveryOtherRowSurvives() {
        val decoded = decodeOrFail(NON_POSITIVE_BEST_TIME_PAYLOAD)

        assertThat(decoded.bests.keys).containsExactly(challengeKey(1))
    }

    @Test
    fun rowsWithANegativeScoreOrABlankLevelIdAreDropped() {
        val decoded = decodeOrFail(NEGATIVE_SCORE_AND_BLANK_ID_PAYLOAD)

        assertThat(decoded.bests.keys).containsExactly(challengeKey(1))
    }

    @Test
    fun aRowWithAnUnknownModeTokenIsDropped() {
        val decoded = decodeOrFail(UNKNOWN_MODE_PAYLOAD)

        assertThat(decoded.bests.keys).containsExactly(challengeKey(1))
    }

    @Test
    fun aChallengeRowWhoseLevelIdIsNotAnIntOrIsOutOfBoundsIsDropped() {
        val decoded = decodeOrFail(challengeIdPayload("abc", (ChallengeLevelBounds.MAX + 1).toString(), "0"))

        assertThat(decoded.bests).isEmpty()
    }

    @Test
    fun aClassicRowsLevelIdIsNeverNumberParsed() {
        val decoded = decodeOrFail(CLASSIC_TEXT_ID_PAYLOAD)

        assertThat(decoded.bests.keys).containsExactly(classicKey(CLASSIC_ID))
    }

    @Test
    fun theLadderIsClampedAndANegativePlayedTimeFloorsToItsIdentity() {
        val tooHigh = decodeOrFail(scalarsPayload(ladder = Int.MAX_VALUE, playedTimeMs = -1L))
        val tooLow = decodeOrFail(scalarsPayload(ladder = -5, playedTimeMs = -1L))

        assertThat(tooHigh.ladder).isEqualTo(ChallengeLevelBounds.MAX)
        assertThat(tooLow.ladder).isEqualTo(ChallengeLevelBounds.MIN)
        assertThat(tooHigh.playedTimeMs).isEqualTo(ProgressMergeLimits.NO_PLAYED_TIME)
    }

    @Test
    fun theWireTokenForEachModeIsPinned() {
        val progress = progressOf(classicKey(CLASSIC_ID), challengeKey(1))

        val text = CloudProgressCodec.encode(progress).toString(Charsets.UTF_8)

        assertThat(text).contains("\"classic\"")
        assertThat(text).contains("\"challenge\"")
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun thePayloadCarriesNoBoardNoMovesAndNoResumeField() {
        val payloadDescriptor = CloudProgressDto.serializer().descriptor
        val rowDescriptor = LevelBestDto.serializer().descriptor
        val payloadFields = payloadDescriptor.elementNames.toSet()
        val rowFields = rowDescriptor.elementNames.toSet()

        assertThat(payloadFields).isEqualTo(setOf("schemaVersion", "ladder", "playedTimeMs", "bests"))
        assertThat(rowFields).isEqualTo(setOf("mode", "levelId", "stars", "bestTimeMs", "bestScore"))
    }

    @Test
    fun mergeBytesJoinsBothSidesFieldByField() {
        val left =
            CloudProgress(
                ladder = 3,
                playedTimeMs = 100L,
                bests = mapOf(challengeKey(1) to LevelBest(stars = 1, bestTimeMs = 50_000L, bestScore = 10)),
            )
        val right =
            CloudProgress(
                ladder = 9,
                playedTimeMs = 40L,
                bests = mapOf(challengeKey(1) to LevelBest(stars = 3, bestTimeMs = 90_000L, bestScore = 80)),
            )

        val merged = mergeAndDecode(left, right)

        assertThat(merged.ladder).isEqualTo(9)
        assertThat(merged.playedTimeMs).isEqualTo(100L)
        assertThat(merged.bests[challengeKey(1)])
            .isEqualTo(LevelBest(stars = 3, bestTimeMs = 50_000L, bestScore = 80))
    }

    @Test
    fun anUndecodableSideContributesTheIdentityRatherThanDiscardingTheOther() {
        val left = progressOf(challengeKey(1), classicKey(CLASSIC_ID))
        val garbage = "not json".toByteArray(Charsets.UTF_8)

        val bytes = CloudProgressCodec.mergeBytes(CloudProgressCodec.encode(left), garbage)

        assertThat(CloudProgressCodec.decode(bytes)).isEqualTo(left)
    }

    @Test
    fun theDomainLadderIdentityMatchesTheAppSideChallengeDefault() {
        assertThat(ProgressMergeLimits.DEFAULT_LADDER)
            .isEqualTo(ChallengeProgressPreferences.DEFAULT_CURRENT_LEVEL)
    }

    @Test
    fun aPayloadAlreadyUnderTheCapIsEncodedWithoutDroppingARow() {
        val progress = progressOf(classicKey(CLASSIC_ID), challengeKey(1), challengeKey(2))

        val capped = CloudProgressCodec.encodeWithinCap(progress)

        assertThat(capped.toList()).isEqualTo(CloudProgressCodec.encode(progress).toList())
        assertThat(CloudProgressCodec.decode(capped)).isEqualTo(progress)
    }

    @Test
    fun tenThousandChallengeRowsAreTrimmedToFitAnArtificialCap() {
        val capped = CloudProgressCodec.encodeWithinCap(largeProgress(), ARTIFICIAL_CAP_BYTES)

        assertThat(capped.size).isAtMost(ARTIFICIAL_CAP_BYTES)
    }

    @Test
    fun theTrimKeepsEveryClassicRowAndBothScalars() {
        val capped = CloudProgressCodec.encodeWithinCap(largeProgress(), ARTIFICIAL_CAP_BYTES)

        val decoded = requireNotNull(CloudProgressCodec.decode(capped))

        assertThat(decoded.bests.keys).containsAtLeastElementsIn(CLASSIC_IDS.map { classicKey(it) })
        assertThat(decoded.ladder).isEqualTo(LADDER)
        assertThat(decoded.playedTimeMs).isEqualTo(PLAYED_TIME_MS)
    }

    @Test
    fun theTrimKeepsTheNumericallyHighestChallengeRows() {
        val capped = CloudProgressCodec.encodeWithinCap(largeProgress(), ARTIFICIAL_CAP_BYTES)

        val decoded = requireNotNull(CloudProgressCodec.decode(capped))
        val survivors = challengeLevelsIn(decoded)
        val dropped = (1..CHALLENGE_ROW_COUNT).toSet() - survivors

        assertThat(survivors).isNotEmpty()
        assertThat(dropped).isNotEmpty()
        assertThat(survivors.min()).isGreaterThan(dropped.max())
        assertThat(survivors.max()).isEqualTo(CHALLENGE_ROW_COUNT)
    }

    @Test
    fun aClassicOnlyPayloadAgainstAnAbsurdCapIsEmittedWholeRatherThanLosingARow() {
        val classicOnly = classicOnlyProgress()

        val capped = CloudProgressCodec.encodeWithinCap(classicOnly, ABSURD_CAP_BYTES)

        assertThat(capped.size).isGreaterThan(ABSURD_CAP_BYTES)
        assertThat(CloudProgressCodec.decode(capped)).isEqualTo(classicOnly)
    }

    private fun challengeLevelsIn(progress: CloudProgress): Set<Int> =
        progress.bests.keys
            .filter { it.mode == ProgressMode.CHALLENGE }
            .mapNotNullTo(mutableSetOf()) { it.levelId.toIntOrNull() }

    private fun decodeOrFail(payload: String): CloudProgress =
        requireNotNull(CloudProgressCodec.decode(payload.toByteArray(Charsets.UTF_8)))

    private fun mergeAndDecode(
        left: CloudProgress,
        right: CloudProgress,
    ): CloudProgress {
        val encodedLeft = CloudProgressCodec.encode(left)
        val encodedRight = CloudProgressCodec.encode(right)
        return requireNotNull(CloudProgressCodec.decode(CloudProgressCodec.mergeBytes(encodedLeft, encodedRight)))
    }

    private companion object {
        const val CLASSIC_ID = "english-33"
        const val LADDER = 42
        const val PLAYED_TIME_MS = 123_456L

        /** The full Challenge ladder, so the trim has a realistic worst case to chew on. */
        const val CHALLENGE_ROW_COUNT = ChallengeLevelBounds.MAX

        /**
         * A deliberately tiny stand-in for `CloudPayloadLimits.TRIM_TARGET_BYTES`.
         *
         * The real target is ~2.75 MB and the realistic payload is ~600 KB, so the trim
         * would never fire against it — and a safety valve that is never exercised is a
         * safety valve that is not known to work. 64 KB forces the loop to run for real.
         */
        const val ARTIFICIAL_CAP_BYTES = 64 * 1024

        /** Smaller than the shortest possible payload: the honest-failure branch. */
        const val ABSURD_CAP_BYTES = 16

        val CLASSIC_IDS =
            listOf("english-33", "european-37", "diamond-41", "triangle-15", "asymmetrical-45")

        fun largeProgress(): CloudProgress {
            val challenge =
                (1..CHALLENGE_ROW_COUNT).associate { level ->
                    challengeKey(level) to LevelBest(stars = 3, bestTimeMs = 30_000L, bestScore = 500)
                }
            return CloudProgress(
                ladder = LADDER,
                playedTimeMs = PLAYED_TIME_MS,
                bests = challenge + classicBests(),
            )
        }

        fun classicOnlyProgress(): CloudProgress =
            CloudProgress(ladder = LADDER, playedTimeMs = PLAYED_TIME_MS, bests = classicBests())

        fun classicBests(): Map<LevelKey, LevelBest> =
            CLASSIC_IDS.associate { id ->
                classicKey(id) to LevelBest(stars = 3, bestTimeMs = 9_000L, bestScore = 1_200)
            }

        fun classicKey(levelId: String) = LevelKey(mode = ProgressMode.CLASSIC, levelId = levelId)

        fun challengeKey(level: Int) = LevelKey(mode = ProgressMode.CHALLENGE, levelId = level.toString())

        fun progressOf(vararg keys: LevelKey) =
            CloudProgress(
                ladder = LADDER,
                playedTimeMs = PLAYED_TIME_MS,
                bests = keys.associateWith { LevelBest(stars = 2, bestTimeMs = 7_000L, bestScore = 500) },
            )

        fun row(
            mode: String,
            levelId: String,
            stars: Int = 2,
            bestTimeMs: Long = 7_000L,
            bestScore: Int = 500,
        ): String {
            val head = "\"mode\":\"$mode\",\"levelId\":\"$levelId\",\"stars\":$stars"
            val tail = "\"bestTimeMs\":$bestTimeMs,\"bestScore\":$bestScore"
            return "{$head,$tail}"
        }

        fun payload(
            rows: String,
            schemaVersion: String = "\"schemaVersion\":1,",
            ladder: Int = 3,
            playedTimeMs: Long = 0L,
        ): String = "{$schemaVersion\"ladder\":$ladder,\"playedTimeMs\":$playedTimeMs,\"bests\":[$rows]}"

        fun payloadOf(vararg rows: String): String = payload(rows.joinToString(","))

        fun challengeIdPayload(vararg levelIds: String): String =
            payloadOf(*levelIds.map { row(mode = "challenge", levelId = it) }.toTypedArray())

        fun scalarsPayload(
            ladder: Int,
            playedTimeMs: Long,
        ): String = payload(rows = "", ladder = ladder, playedTimeMs = playedTimeMs)

        val FORWARD_COMPATIBLE_PAYLOAD =
            """
            {
              "schemaVersion": 1,
              "ladder": 12,
              "playedTimeMs": 5000,
              "futureTopLevelField": "ignored",
              "bests": [
                {
                  "mode": "classic",
                  "levelId": "$CLASSIC_ID",
                  "stars": 2,
                  "bestTimeMs": 8000,
                  "bestScore": 900,
                  "futureRowField": 7
                }
              ]
            }
            """.trimIndent()

        val NEWER_SCHEMA_PAYLOAD = payload(rows = "", schemaVersion = "\"schemaVersion\":99,", ladder = 5)

        val TRUNCATED_PAYLOAD = payload(rows = row(mode = "challenge", levelId = "1")).dropLast(12)

        val NO_SCHEMA_VERSION_PAYLOAD =
            payload(rows = row(mode = "challenge", levelId = "1"), schemaVersion = "", ladder = 4)

        val OUT_OF_RANGE_STARS_PAYLOAD =
            payloadOf(
                row(mode = "challenge", levelId = "1", stars = 999),
                row(mode = "challenge", levelId = "2", stars = -7),
            )

        val NON_POSITIVE_BEST_TIME_PAYLOAD =
            payloadOf(
                row(mode = "challenge", levelId = "1"),
                row(mode = "challenge", levelId = "2", bestTimeMs = 0L),
                row(mode = "challenge", levelId = "3", bestTimeMs = -1L),
            )

        val NEGATIVE_SCORE_AND_BLANK_ID_PAYLOAD =
            payloadOf(
                row(mode = "challenge", levelId = "1"),
                row(mode = "challenge", levelId = "2", bestScore = -1),
                row(mode = "classic", levelId = "  "),
            )

        val UNKNOWN_MODE_PAYLOAD =
            payloadOf(
                row(mode = "challenge", levelId = "1"),
                row(mode = "CLASSIC", levelId = CLASSIC_ID),
                row(mode = "arcade", levelId = "3"),
            )

        val CLASSIC_TEXT_ID_PAYLOAD = payloadOf(row(mode = "classic", levelId = CLASSIC_ID))
    }
}
