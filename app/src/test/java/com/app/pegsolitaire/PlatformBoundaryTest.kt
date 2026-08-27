package com.app.pegsolitaire

import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.junit.ArchUnitRunner
import com.tngtech.archunit.lang.ArchRule
import org.junit.runner.RunWith

/**
 * Platform-boundary gate (PLAT-04, PLAT-01) — the seam counterpart to
 * [DomainPurityTest]. [DomainPurityTest] proves `:domain` is Android-free; this proves
 * the Play/AdMob SDKs and the foreground Activity are confined to
 * `com.app.pegsolitaire.platform..`.
 *
 * It is a **separate class on purpose**. ArchUnit's import scope is per-class, and
 * [DomainPurityTest] is scoped to `com.app.pegsolitaire.domain` — folding a "nothing
 * outside platform" rule into it would make that rule vacuous, since nothing outside the
 * domain package would ever be imported to violate it. These rules need the whole app.
 *
 * Bytecode-based (like [DomainPurityTest]), so it also covers Hilt/KSP-generated classes
 * — the generated component references every binding's implementation type, which is
 * exactly where an SDK type would otherwise leak out of `..platform..` unnoticed.
 * Imported with [DoNotIncludeAndroidTestOutput], so plan 06-09's deliberately-violating
 * `archfixture` package stays invisible to this production gate while the negative gate
 * runs the same rules against it.
 *
 * **That import option is a hand-written replacement for `ImportOption.DoNotIncludeTests`,
 * which this class originally used and which does nothing in an Android build.** The
 * substitution was forced by evidence, not preference: adding the `archfixture` classes
 * turned this test red on all three rules, because AGP writes unit-test classes to
 * `…/debugUnitTest/…` and ArchUnit's built-in heuristic looks for a `/test/` path segment
 * that never appears. See [DoNotIncludeAndroidTestOutput] for the paths. Until 06-09 there
 * was no test class violating any rule, so the broken exclusion had no observable effect —
 * which is precisely why it survived to be found here.
 *
 * Runs under `:app:testDebugUnitTest`, which is wired into `check`.
 *
 * The rule bodies live in [PlatformBoundaryRules] and are referenced, never copied — see
 * that object's KDoc. [PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks] is expected
 * to pass **vacuously** until plan 06-09 puts the SDK artifacts on the graph; that plan
 * is what gives it teeth.
 */
@RunWith(ArchUnitRunner::class)
@AnalyzeClasses(
    packages = ["com.app.pegsolitaire"],
    importOptions = [DoNotIncludeAndroidTestOutput::class],
)
class PlatformBoundaryTest {
    @ArchTest
    val onlyPlatformDependsOnPlaySdks: ArchRule = PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks

    @ArchTest
    val onlyPlatformReachesTheForegroundActivity: ArchRule =
        PlatformBoundaryRules.onlyPlatformReachesTheForegroundActivity

    @ArchTest
    val uiAndDomainNeverTouchAnActivity: ArchRule = PlatformBoundaryRules.uiAndDomainNeverTouchAnActivity
}
