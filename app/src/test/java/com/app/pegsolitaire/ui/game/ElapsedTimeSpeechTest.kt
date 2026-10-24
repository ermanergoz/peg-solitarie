package com.app.pegsolitaire.ui.game

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Which parts of an elapsed time TalkBack says (ux-spec §4.3.2, D-05e; criterion 18f's table).
 *
 * The arithmetic is pure, so it is read here as a table on the JVM, without resources; the composed
 * sentence, with its plurals and its format strings, is `GameStatusStripTest`'s, on the node.
 *
 * The rule: minutes are said when there is at least one; seconds are said when there is at least
 * one, or when the whole time is zero; minutes never roll into hours, just as the clock never rolls
 * them. Every expected part below is written out by hand from §4.3.2's table, never computed.
 */
class ElapsedTimeSpeechTest {
    @Test
    fun eachTimeOnCriterion18fsTableSaysExactlyThePartsTheSpecLists() {
        val table =
            listOf(
                0 to SpokenDuration.Seconds(seconds = 0),
                1 to SpokenDuration.Seconds(seconds = 1),
                45 to SpokenDuration.Seconds(seconds = 45),
                60 to SpokenDuration.Minutes(minutes = 1),
                61 to SpokenDuration.MinutesAndSeconds(minutes = 1, seconds = 1),
                312 to SpokenDuration.MinutesAndSeconds(minutes = 5, seconds = 12),
                720 to SpokenDuration.Minutes(minutes = 12),
                3599 to SpokenDuration.MinutesAndSeconds(minutes = 59, seconds = 59),
                6000 to SpokenDuration.Minutes(minutes = 100),
            )

        table.forEach { (elapsedSeconds, said) ->
            assertWithMessage("what is said for $elapsedSeconds s")
                .that(ElapsedTimeSpeech.parts(elapsedSeconds))
                .isEqualTo(said)
        }
    }
}
