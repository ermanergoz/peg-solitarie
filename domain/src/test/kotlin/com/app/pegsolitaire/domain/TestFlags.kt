package com.app.pegsolitaire.domain

/**
 * Opt-in flags that switch a test out of its default fast behaviour.
 *
 * Every such flag in this module is read the same way — an environment variable OR a `-D`
 * system property, compared case-insensitively to `"true"` — because Gradle makes each
 * channel convenient in a different place: an env var suits a developer's shell, a system
 * property suits a `Test` task that sets it in `build.gradle.kts`. Accepting both means a
 * flag works from either without the caller having to know which one a given test happened
 * to implement.
 *
 * This exists because that three-line read had been copied into
 * [com.app.pegsolitaire.domain.golden.GoldenMasterAssert],
 * [com.app.pegsolitaire.domain.golden.solutions.SolutionTest] and
 * `OpeningBookAuthoringTest`, and plan 08-10's depth sweep would have been a fourth. Four
 * copies of a gate is four chances for one of them to read only `getenv` and silently
 * ignore the `-D` the build actually passes — a gated test that quietly never runs is
 * indistinguishable from a passing one.
 */
internal object TestFlags {
    /** Regenerates committed golden fixtures instead of asserting against them. */
    const val UPDATE_GOLDEN = "UPDATE_GOLDEN"

    /**
     * Runs the exhaustive depth sweep body (plan 08-10).
     *
     * Set by the dedicated `:domain:depthSweep` Gradle task, which `check` depends on.
     * Unset during a normal `:domain:test`, where the sweep is a no-op.
     */
    const val RUN_SWEEP = "RUN_SWEEP"

    /** Whether [flag] is set to `"true"` in the environment or as a system property. */
    fun isEnabled(flag: String): Boolean = isTrue(System.getenv(flag)) || isTrue(System.getProperty(flag))

    private fun isTrue(value: String?): Boolean = value?.equals("true", ignoreCase = true) == true
}
