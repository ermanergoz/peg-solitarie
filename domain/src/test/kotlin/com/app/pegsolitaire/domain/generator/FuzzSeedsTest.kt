package com.app.pegsolitaire.domain.generator

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * `FUZZ_SEEDS` is parsed strictly (rules/stacks.md contract 6): a value that is not a positive
 * integer fails the run and names itself, because a typo such as `1e5` or `10k` that quietly became
 * some other count, or a harness that walked nothing, would read exactly like a pass.
 */
class FuzzSeedsTest {
    @Test
    fun aPositiveIntegerIsTheNumberOfLevelsToWalk() {
        assertThat(FuzzSeeds.parse("1000")).isEqualTo(1_000)
        assertThat(FuzzSeeds.parse("100000")).isEqualTo(100_000)
    }

    @Test
    fun aValueThatIsNotAnIntegerFailsNamingTheValue() {
        listOf(null, "", "abc", "1e3").forEach { raw -> assertRejected(raw) }
    }

    @Test
    fun aZeroOrNegativeCountFailsNamingTheValue() {
        listOf("0", "-7").forEach { raw -> assertRejected(raw) }
    }

    private fun assertRejected(raw: String?) {
        val failure = assertThrows(IllegalArgumentException::class.java) { FuzzSeeds.parse(raw) }

        assertWithMessage("the failure for FUZZ_SEEDS=%s", raw)
            .that(failure)
            .hasMessageThat()
            .isEqualTo("FUZZ_SEEDS must be a positive integer; was '$raw'")
    }
}
