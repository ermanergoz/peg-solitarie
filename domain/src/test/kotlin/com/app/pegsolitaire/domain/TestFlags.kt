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
 *
 * A flag that carries a value rather than a switch ([FUZZ_SEEDS]) is read through [value],
 * from the same two channels, and parsed strictly by its consumer.
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

    /**
     * Runs the fuzz harness body (`FuzzHarnessTest`, docs/tech-design.md §7.3).
     *
     * Set by the dedicated `:domain:fuzz` Gradle task, which `check` depends on. Unset during a
     * normal `:domain:test`, where the harness is a no-op.
     */
    const val RUN_FUZZ = "RUN_FUZZ"

    /**
     * How many Challenge levels the fuzz harness walks, from level 1 (rules/puzzle-design.md
     * invariant 8). A count, not a switch: read it with [value]. `:domain:fuzz` forwards the
     * `FUZZ_SEEDS` environment variable, or the build's local default, as a system property.
     */
    const val FUZZ_SEEDS = "FUZZ_SEEDS"

    /** Whether [flag] is set to `"true"` in the environment or as a system property. */
    fun isEnabled(flag: String): Boolean = isTrue(System.getenv(flag)) || isTrue(System.getProperty(flag))

    /** The raw value of [flag]: the environment variable first, then the system property; null if neither is set. */
    fun value(flag: String): String? = System.getenv(flag) ?: System.getProperty(flag)

    private fun isTrue(value: String?): Boolean = value?.equals("true", ignoreCase = true) == true
}
