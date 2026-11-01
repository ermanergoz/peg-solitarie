package com.app.pegsolitaire.domain.golden

import com.app.pegsolitaire.domain.TestFlags
import com.google.common.truth.Truth.assertWithMessage
import java.io.File

/**
 * Reusable golden-master assertion: compares an encoded "actual" string against a
 * committed fixture under `src/test/resources/golden/`.
 *
 * On mismatch it writes a sibling `<name>.actual` file (for easy diffing) and
 * fails with a readable message. Fixtures are regenerated only deliberately, by
 * opting in with `UPDATE_GOLDEN=true` (env var or `-D` system property) — a normal
 * test run never overwrites a fixture.
 *
 * Phase 3 reuses this class verbatim to lock generator determinism.
 */
object GoldenMasterAssert {
    private const val UPDATE_GOLDEN_FLAG = "UPDATE_GOLDEN"
    private const val GOLDEN_DIR = "src/test/resources/golden"
    private const val ACTUAL_SUFFIX = ".actual"

    /**
     * Asserts [actual] matches the committed fixture named [fixtureName].
     *
     * When the update flag is set, (re)writes the fixture and returns without
     * asserting. Otherwise compares against the on-disk fixture and, on mismatch,
     * persists `<fixtureName>.actual` before failing.
     */
    fun assertMatches(
        fixtureName: String,
        actual: String,
    ) {
        val goldenDir = File(GOLDEN_DIR)
        val fixture = File(goldenDir, fixtureName)

        if (isUpdateEnabled()) {
            goldenDir.mkdirs()
            fixture.writeText(actual)
            return
        }

        val actualFile = File(goldenDir, fixtureName + ACTUAL_SUFFIX)
        if (!fixture.exists()) {
            actualFile.parentFile?.mkdirs()
            actualFile.writeText(actual)
            assertWithMessage(
                "Missing golden fixture '$fixtureName'. Wrote actual to " +
                    "${actualFile.path}. Re-run with UPDATE_GOLDEN=true to create fixtures.",
            ).fail()
            return
        }

        val expected = fixture.readText()
        if (expected != actual) {
            actualFile.writeText(actual)
        }
        assertWithMessage(
            "Golden mismatch for '$fixtureName'. Actual written to ${actualFile.path}. " +
                "If this change is intentional, re-run with UPDATE_GOLDEN=true.",
        ).that(actual).isEqualTo(expected)
    }

    private fun isUpdateEnabled(): Boolean = TestFlags.isEnabled(UPDATE_GOLDEN_FLAG)
}
