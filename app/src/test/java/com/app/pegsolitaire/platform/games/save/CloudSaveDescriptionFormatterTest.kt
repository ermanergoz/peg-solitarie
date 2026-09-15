package com.app.pegsolitaire.platform.games.save

import com.app.pegsolitaire.domain.achievement.ClassicBoard
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * The listing line the player reads in the Play Games saves UI (CLOUD-03, D-11).
 *
 * Robolectric rather than a plain JVM test because the whole point of the class under test
 * is that the wording lives in `strings.xml` and is resolved through a real [android.content.Context]
 * at commit time — a test that stubbed `getString` would assert the stub's wording and would
 * stay green with the string resource deleted.
 *
 * The denominator is asserted through `ClassicBoard.entries.size` and never as a literal `5`:
 * adding a sixth Classic board must make the description follow the roster, and a test that
 * hardcoded the old count would be the thing standing in the way.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [FORMATTER_ROBOLECTRIC_SDK])
class CloudSaveDescriptionFormatterTest {
    private val formatter = CloudSaveDescriptionFormatter(RuntimeEnvironment.getApplication())

    @Test
    fun theLineCarriesThePlayersOwnFigures() {
        val line = formatter.format(SAMPLE)

        assertThat(line).isNotEmpty()
        assertThat(line.isBlank()).isFalse()
        assertThat(line).contains(SAMPLE_LADDER.toString())
        assertThat(line).contains(SAMPLE_TOTAL_STARS.toString())
        assertThat(line).contains(SAMPLE_CLASSIC_CLEARED.toString())
    }

    @Test
    fun theLineIsASingleLine() {
        assertThat(formatter.format(SAMPLE)).doesNotContain("\n")
    }

    @Test
    fun theLineFitsTheDescriptionCap() {
        assertThat(formatter.format(SAMPLE).length)
            .isAtMost(CloudSaveConstants.MAX_DESCRIPTION_CHARS)
    }

    @Test
    fun anAbsurdInputIsStillCapped() {
        val absurd =
            CloudSaveDescription(
                ladder = Int.MAX_VALUE,
                totalStars = Int.MAX_VALUE,
                classicCleared = Int.MAX_VALUE,
            )

        assertThat(formatter.format(absurd).length)
            .isAtMost(CloudSaveConstants.MAX_DESCRIPTION_CHARS)
    }

    @Test
    fun theDenominatorIsTheClassicRosterSize() {
        val line = formatter.format(SAMPLE)

        assertThat(line).contains("$SAMPLE_CLASSIC_CLEARED/${ClassicBoard.entries.size}")
    }
}

private const val FORMATTER_ROBOLECTRIC_SDK = 34

private const val SAMPLE_LADDER = 42
private const val SAMPLE_TOTAL_STARS = 137
private const val SAMPLE_CLASSIC_CLEARED = 3

private val SAMPLE =
    CloudSaveDescription(
        ladder = SAMPLE_LADDER,
        totalStars = SAMPLE_TOTAL_STARS,
        classicCleared = SAMPLE_CLASSIC_CLEARED,
    )
