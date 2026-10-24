package com.app.pegsolitaire.data.licenses

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * `GoogleNoticeText` (brief §2.1, §3.1): the one `LicenseAsset` besides the curated enum, built
 * only from a stem the parsed index gives. Its constructor accepts exactly sixteen lowercase hex
 * digits, so whatever reaches it can only ever name a file inside `licenses/google/` — never a
 * path, a parent directory or a file the build did not write.
 */
class GoogleNoticeTextTest {
    @Test
    fun aStemOfSixteenLowercaseHexDigitsNamesItsFileUnderLicensesGoogle() {
        assertThat(GoogleNoticeText(VALID_STEM).assetPath).isEqualTo("licenses/google/$VALID_STEM.txt")
    }

    @Test
    fun anythingElseIsRejected() {
        listOf(
            "0123456789ABCDEF",
            "0123456789abcde",
            "0123456789abcdef0",
            "0123456789abcdeg",
            "../../../../etc0",
            "",
        ).forEach { stem ->
            assertThrows(IllegalArgumentException::class.java) { GoogleNoticeText(stem) }
        }
    }

    private companion object {
        const val VALID_STEM = "0123456789abcdef"
    }
}
