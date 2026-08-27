package com.app.pegsolitaire

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.core.importer.Location
import java.util.regex.Pattern

/**
 * Excludes AGP's unit-test and instrumentation-test class output from an ArchUnit import.
 *
 * **This exists because [ImportOption.DoNotIncludeTests] does nothing in an Android
 * Gradle build.** That was assumed to work when [PlatformBoundaryTest] was written (plan
 * 06-05, whose KDoc promised the `archfixture` package would stay invisible to it) and
 * again when plan 06-09 was planned — 06-RESEARCH.md states the heuristic "keys on the
 * classpath location (`.../test/...`), which holds for `app/src/test`". **It does not
 * hold**, and it was caught the only way an assumption like this ever is: by adding a
 * violating class and watching the production gate go red.
 *
 * ArchUnit's built-in heuristic looks for JVM-ecosystem conventions — Maven's
 * `target/test-classes/`, Gradle's `build/classes/java/test/` and
 * `build/classes/kotlin/test/`. AGP writes somewhere else entirely, keying on the
 * *variant* name rather than on a `test` path segment:
 *
 * ```
 * app/build/intermediates/built_in_kotlinc/debugUnitTest/compileDebugUnitTestKotlin/classes/…
 * app/build/intermediates/classes/debugUnitTest/transformDebugUnitTestClassesWithAsm/dirs/…
 * ```
 *
 * versus production classes at `…/intermediates/classes/debug/transformDebugClassesWithAsm/…`.
 * There is no `/test/` segment anywhere in the first two, so `DoNotIncludeTests` matched
 * nothing and silently included every test class in the import. Note the shape of that
 * failure: it is invisible while no test class violates a rule, which is exactly how long
 * it stayed invisible here.
 *
 * The discriminator that does work is the `<variant>UnitTest` / `<variant>AndroidTest`
 * directory AGP names the output after.
 *
 * Not a Kotlin `object`: ArchUnit instantiates `importOptions` reflectively via a no-arg
 * constructor, so this has to be an ordinary class.
 */
internal class DoNotIncludeAndroidTestOutput : ImportOption {
    override fun includes(location: Location): Boolean = !location.matches(ANDROID_TEST_CLASS_OUTPUT)

    private companion object {
        /**
         * Matches AGP's per-variant test class output directories — `debugUnitTest`,
         * `releaseUnitTest`, `debugAndroidTest`, and any flavoured equivalent.
         *
         * Anchored on a full path segment (`/…UnitTest/`) rather than a bare `contains`,
         * so a production class in some future package or directory that merely has the
         * letters "UnitTest" in its name is not silently dropped from the gate's scope.
         * Dropping production classes would weaken the gate in the direction that fails
         * open, which is the one that does not announce itself.
         */
        private val ANDROID_TEST_CLASS_OUTPUT: Pattern =
            Pattern.compile(".*/[A-Za-z0-9]*(UnitTest|AndroidTest)/.*")
    }
}
