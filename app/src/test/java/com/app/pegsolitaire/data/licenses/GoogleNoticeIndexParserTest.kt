package com.app.pegsolitaire.data.licenses

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * `parseGoogleNoticeIndex` (tech design §13.1 LN-4; spec LS-AC12 (b)): the build's
 * `licenses/google/index.tsv`, one `key\tname\tstem\tsources` line per (name, text) pair, parses
 * whole or not at all. Any malformed line makes the whole index `null` — the screens' error state —
 * because a half-read index would show the player a list that silently lost notices.
 */
class GoogleNoticeIndexParserTest {
    @Test
    fun aWellFormedIndexParsesIntoOneNoticePerLineInOrder() {
        val raw = "$KEY_ONE\tKotlin\t$STEM_ONE\tads-mobile-sdk,hsdp\n$KEY_TWO\tOkHttp\t$STEM_TWO\tads-mobile-sdk\n"

        assertThat(parseGoogleNoticeIndex(raw))
            .containsExactly(
                GoogleNotice(KEY_ONE, "Kotlin", listOf("ads-mobile-sdk", "hsdp"), STEM_ONE),
                GoogleNotice(KEY_TWO, "OkHttp", listOf("ads-mobile-sdk"), STEM_TWO),
            ).inOrder()
    }

    @Test
    fun aNameIsKeptAsSuppliedSpacesAndAllButNoTab() {
        val notice = parseGoogleNoticeIndex("$KEY_ONE\t Android SDK Platforms \t$STEM_ONE\thsdp\n")?.single()

        assertThat(notice?.name).isEqualTo(" Android SDK Platforms ")
    }

    @Test
    fun anEmptyIndexIsAnEmptyList() {
        assertThat(parseGoogleNoticeIndex("")).isEmpty()
    }

    @Test
    fun theFinalNewlineIsOptional() {
        assertThat(parseGoogleNoticeIndex("$KEY_ONE\tKotlin\t$STEM_ONE\thsdp")).hasSize(1)
    }

    @Test
    fun aLineWithThreeFieldsIsNull() {
        assertNull("$KEY_ONE\tKotlin\t$STEM_ONE\n")
    }

    @Test
    fun aLineWithFiveFieldsIsNull() {
        assertNull("$KEY_ONE\tKotlin\t$STEM_ONE\thsdp\textra\n")
    }

    @Test
    fun anEmptyFieldIsNull() {
        assertNull("$KEY_ONE\t\t$STEM_ONE\thsdp\n")
        assertNull("$KEY_ONE\tKotlin\t$STEM_ONE\t\n")
    }

    @Test
    fun aStemThatIsNotSixteenHexDigitsIsNull() {
        assertNull("$KEY_ONE\tKotlin\t0123456789ABCDEF\thsdp\n")
        assertNull("$KEY_ONE\tKotlin\t0123456789abcde\thsdp\n")
        assertNull("$KEY_ONE\tKotlin\t../../../../etc0\thsdp\n")
    }

    @Test
    fun aKeyThatIsNotSixteenHexDigitsIsNull() {
        assertNull("not-a-key-at-all\tKotlin\t$STEM_ONE\thsdp\n")
    }

    @Test
    fun anEmptySourceIdIsNull() {
        assertNull("$KEY_ONE\tKotlin\t$STEM_ONE\thsdp,,ads-mobile-sdk\n")
        assertNull("$KEY_ONE\tKotlin\t$STEM_ONE\thsdp,\n")
    }

    @Test
    fun aDuplicateKeyIsNull() {
        assertNull("$KEY_ONE\tKotlin\t$STEM_ONE\thsdp\n$KEY_ONE\tOkHttp\t$STEM_TWO\thsdp\n")
    }

    @Test
    fun aBlankLineAnywhereButTheEndIsNull() {
        assertNull("$KEY_ONE\tKotlin\t$STEM_ONE\thsdp\n\n$KEY_TWO\tOkHttp\t$STEM_TWO\thsdp\n")
        assertNull("$KEY_ONE\tKotlin\t$STEM_ONE\thsdp\n\n")
    }

    /** The build writes `\n` only; a `\r` means the file is not the one the build wrote. */
    @Test
    fun aCarriageReturnIsNull() {
        assertNull("$KEY_ONE\tKotlin\t$STEM_ONE\thsdp\r\n")
    }

    private fun assertNull(raw: String) {
        assertWithMessage(raw).that(parseGoogleNoticeIndex(raw)).isNull()
    }

    private companion object {
        const val KEY_ONE = "0123456789abcdef"
        const val KEY_TWO = "fedcba9876543210"
        const val STEM_ONE = "00112233445566ff"
        const val STEM_TWO = "aabbccddeeff0011"
    }
}
