package com.app.pegsolitaire.ui.game

import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * What TalkBack says of an elapsed time (ux-spec §4.3.2, D-05e, with its whole-minute fallback;
 * tech-lead defect ruling §2, B-2; criterion 18f's table).
 *
 * The arithmetic is pure, so it is read here as a table on the JVM, without resources; the composed
 * sentence, with its plural and its string, is `GameStatusStripTest`'s, on the node.
 *
 * The rule: whole minutes, floored and never rounded up, from one minute on; "under a minute" below
 * it; minutes never roll into hours, just as the clock never rolls them. Every expected value below
 * is written out by hand from the ruling's table, never computed.
 */
class ElapsedTimeSpeechTest {
    @Test
    fun eachTimeOnCriterion18fsTableSaysExactlyThePartsTheSpecLists() {
        val table =
            listOf(
                0 to SpokenDuration.UnderAMinute,
                1 to SpokenDuration.UnderAMinute,
                45 to SpokenDuration.UnderAMinute,
                60 to SpokenDuration.Minutes(minutes = 1),
                61 to SpokenDuration.Minutes(minutes = 1),
                312 to SpokenDuration.Minutes(minutes = 5),
                720 to SpokenDuration.Minutes(minutes = 12),
                3599 to SpokenDuration.Minutes(minutes = 59),
                6000 to SpokenDuration.Minutes(minutes = 100),
            )

        table.forEach { (elapsedSeconds, said) ->
            assertWithMessage("what is said for $elapsedSeconds s")
                .that(ElapsedTimeSpeech.parts(elapsedSeconds))
                .isEqualTo(said)
        }
    }
}
