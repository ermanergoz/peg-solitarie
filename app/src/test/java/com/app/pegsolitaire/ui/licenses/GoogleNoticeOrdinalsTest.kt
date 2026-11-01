package com.app.pegsolitaire.ui.licenses

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * `noticeOrdinals` (spec LS-12 as amended in loop 1, F-08; brief A1.5): when the index holds the
 * same name more than once, each of those rows is numbered "Notice n of m" in index order, so two
 * rows that read alike by eye and by ear — same name, same sources — are still told apart. A name
 * that occurs once carries no ordinal. "The same name" is the same string, not a case-folded one.
 */
class GoogleNoticeOrdinalsTest {
    @Test
    fun repeatedNamesAreNumberedInIndexOrder() {
        val index =
            listOf(
                fixtureNotice(KEY_A, "Kotlin", STEM_A, "ads-mobile-sdk"),
                fixtureNotice(KEY_B, "Kotlin", STEM_B, "hsdp"),
                fixtureNotice(KEY_C, "OkHttp", STEM_A, "ads-mobile-sdk"),
                fixtureNotice(KEY_D, "Kotlin", STEM_C, "ads-mobile-sdk"),
            )

        val ordinals = index.noticeOrdinals()

        assertThat(ordinals[KEY_A]).isEqualTo(NoticeOrdinal(index = 1, count = 3))
        assertThat(ordinals[KEY_B]).isEqualTo(NoticeOrdinal(index = 2, count = 3))
        assertThat(ordinals[KEY_D]).isEqualTo(NoticeOrdinal(index = 3, count = 3))
    }

    @Test
    fun aNameThatOccursOnceHasNoOrdinal() {
        val index =
            listOf(
                fixtureNotice(KEY_A, "Kotlin", STEM_A, "ads-mobile-sdk"),
                fixtureNotice(KEY_B, "Kotlin", STEM_B, "hsdp"),
                fixtureNotice(KEY_C, "OkHttp", STEM_A, "ads-mobile-sdk"),
            )

        assertThat(index.noticeOrdinals()).doesNotContainKey(KEY_C)
    }

    @Test
    fun namesDifferingOnlyInCaseAreNotTheSameName() {
        val index =
            listOf(
                fixtureNotice(KEY_A, "Guava", STEM_A, "ads-mobile-sdk"),
                fixtureNotice(KEY_B, "guava", STEM_B, "hsdp"),
            )

        assertThat(index.noticeOrdinals()).isEmpty()
    }

    private companion object {
        const val KEY_A = "000000000000000a"
        const val KEY_B = "000000000000000b"
        const val KEY_C = "000000000000000c"
        const val KEY_D = "000000000000000d"
        const val STEM_A = "a000000000000000"
        const val STEM_B = "b000000000000000"
        const val STEM_C = "c000000000000000"
    }
}
