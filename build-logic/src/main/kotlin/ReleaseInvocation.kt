/**
 * Decides whether the task names the user actually requested mean "a release artifact is about
 * to be PACKAGED" (D-08 / D-09 / D-16 / D-17).
 *
 * This is the configuration-time half of three gates in `app/build.gradle.kts` — the ad IDs
 * (PLAT-06 / D-05), the Play Games IDs (PLAT-06 / D-17) and the upload signing credentials
 * (PLAT-08 / D-16). AGP configures EVERY variant on EVERY invocation, so a check inside
 * `release { }` would fire during `assembleDebug` and during IDE sync; `gradle.startParameter`
 * `.taskNames` is the one signal that distinguishes what was asked for, and it is EMPTY during
 * IDE sync, which is exactly the behaviour these gates want.
 *
 * **Why this is a verb-prefix test and not `contains("elease")`.** The substring test also
 * matched `lintRelease`, `lintVitalRelease`, `detektRelease`, `compileReleaseKotlin` and
 * `connectedReleaseAndroidTest` — analysis tasks that package nothing — so running any of them
 * demanded two keystore passwords and four ID values, which pressures an operator into exporting
 * cryptographic secrets in an everyday shell. Every task that *produces or installs* a shippable
 * release artifact starts with one of [PACKAGING_VERBS]. The match is not exact in either
 * direction, and both gaps are harmless: some internal AGP tasks share the prefix
 * (`packageReleaseResources`, `bundleReleaseClassesToCompileJar`) and are caught although they
 * package nothing shippable, which costs only a demand for credentials; and `signReleaseBundle`,
 * which signs the AAB, does not match, but it depends on `packageReleaseBundle`, which carries the
 * execution-time verify tasks described below. This is deliberately **not** an exhaustive list of
 * task names: a forgotten entry in a name list is a silent hole.
 *
 * **Why narrowing is safe.** It is a heuristic over a user-supplied string and it cannot be proven
 * exhaustive over every invocation form, so it is not the only protection: `verifyReleaseAdIds`,
 * `verifyReleaseGamesIds` and `verifyReleaseSigning` re-run the identical resolutions at execution
 * time as hard dependencies of `packageRelease` / `packageReleaseBundle`. This predicate buys
 * *latency* — a two-second failure before compilation instead of a three-minute one after R8 —
 * and the execution-time tasks buy *coverage*.
 *
 * **Why it lives in `build-logic` rather than inline in `app/build.gradle.kts`.** Same reason as
 * [AdIdResolution] and [SigningResolution]: AGP 9.1.0 creates a unit-test component only for the
 * tested build type, so there is no `testReleaseUnitTest` and a `:app` test of release-gate
 * behaviour would silently never run. A Gradle-free predicate can be proven in
 * [ReleaseInvocationTest] instead.
 */
object ReleaseInvocation {
    /**
     * Task names that build BOTH variants, so a release is packaged even though the name carries
     * no variant. Matched whole, after the project path is stripped.
     */
    val WHOLE_PROJECT_TASK_NAMES = setOf("assemble", "build", "bundle")

    /**
     * The AGP verbs that begin the name of a task which produces or installs a packaged artifact:
     * `assembleRelease`, `bundleRelease`, `packageRelease`, `packageReleaseBundle`,
     * `installRelease`, `publishReleaseBundle`.
     */
    val PACKAGING_VERBS = setOf("assemble", "bundle", "package", "install", "publish")

    /**
     * The variant fragment, spelled without its first letter so it matches both the capitalised
     * infix (`assembleRelease`) and any lower-case spelling (`release`).
     */
    const val RELEASE_FRAGMENT = "elease"

    /** The project-path separator: `:app:assembleRelease` is requested as often as the bare name. */
    private const val TASK_PATH_SEPARATOR = ':'

    /** True when ANY requested name would package a release. Empty (IDE sync) is false. */
    fun isReleaseInvocation(requestedTaskNames: Iterable<String>): Boolean =
        requestedTaskNames.any(::isReleasePackagingTask)

    /** True when this one requested name would package a release, with or without a project path. */
    fun isReleasePackagingTask(requestedTaskName: String): Boolean {
        val name = requestedTaskName.substringAfterLast(TASK_PATH_SEPARATOR)
        if (name in WHOLE_PROJECT_TASK_NAMES) {
            return true
        }
        return name.contains(RELEASE_FRAGMENT) && PACKAGING_VERBS.any { name.startsWith(it) }
    }
}
