package com.app.pegsolitaire.domain.golden.solutions

import com.app.pegsolitaire.domain.TestFlags
import org.junit.Test
import java.io.File

/**
 * Authoring-only regeneration of the committed opening book (DEBT-01).
 *
 * A no-op unless `UPDATE_GOLDEN=true` is set, mirroring
 * `SolutionTest.regenerateSolutionFixturesWhenUpdateFlagSet` exactly — so a normal CI
 * run performs **zero** search and leaves `domain/src/main/resources` byte-identical.
 * The generation is a deliberate multi-minute run at the 8 GB authoring heap
 * (`domain/build.gradle.kts` sizes it inside the same conditional); it is not a CI step,
 * and Pitfall 5's "`:domain:test` stays seconds-scale per commit" contract depends on
 * that separation holding.
 *
 * The CI-time integrity gate over the emitted book — every entry legal, every mask
 * in-footprint, the loader fail-closed on a corrupt file — is plan 08-09's
 * `OpeningBookTest`, not this class.
 */
class OpeningBookAuthoringTest {
    @Test
    fun regenerateOpeningBookWhenUpdateFlagSet() {
        if (!isUpdateEnabled()) return
        val report = OpeningBookAuthoring.writeBook(File(OpeningBookAuthoring.DIRECTORY))
        println(report.render())
    }

    private fun isUpdateEnabled(): Boolean = TestFlags.isEnabled(UPDATE_GOLDEN_FLAG)

    private companion object {
        const val UPDATE_GOLDEN_FLAG = TestFlags.UPDATE_GOLDEN
    }
}
