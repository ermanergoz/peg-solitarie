package com.app.pegsolitaire

import com.app.pegsolitaire.archfixture.GmsBoundaryViolationFixture
import com.app.pegsolitaire.archfixture.ui.ActivityBoundaryViolationFixture
import com.tngtech.archunit.core.importer.ClassFileImporter
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Proves the PLAT-04 / PLAT-01 boundary rules **fail when violated**.
 *
 * A gate that has only ever been observed to pass is indistinguishable from a gate that
 * cannot fail. [PlatformBoundaryTest] runs the same three rules against production
 * bytecode and is green — but green there is also what a typo in a package pattern, a
 * predicate that matches nothing, or a rule accidentally scoped to an empty import would
 * produce. Nothing about a passing ArchUnit run distinguishes "no violations exist" from
 * "no violations are detectable". This test supplies the missing half: violations that do
 * exist, and the assertion that each rule notices.
 *
 * `com.app.pegsolitaire.archfixture` holds one deliberately-broken class per rule.
 * [PlatformBoundaryTest] cannot see them because it imports with
 * `ImportOption.DoNotIncludeTests` and they live under `app/src/test`, so the production
 * gate stays green while this test runs the *same rule objects* against them.
 *
 * **The rules are referenced from [PlatformBoundaryRules], never restated here.** That is
 * the entire reason plan 06-05 extracted them. A copied rule body would make this test
 * assert that *a* rule can fail, not that *the* rule can — the production copy could drift
 * to match nothing at all and this file would keep passing against the old shape, which is
 * precisely the blindness it exists to rule out.
 *
 * Runner-free by design: it constructs its own [ClassFileImporter] rather than using
 * `ArchUnitRunner`, because `@ArchTest` fields are evaluated as assertions that must pass,
 * and here the passing condition is that the rule *throws*.
 */
class PlatformBoundaryGateTest {
    /**
     * The violating classes, scoped by **symbol** rather than by a package-name string.
     *
     * Two benefits, both about the failure modes of the string form. A renamed or moved
     * fixture package breaks compilation here instead of silently narrowing the import to
     * zero classes — and a zero-class import makes every rule below pass vacuously, i.e.
     * turns this test into the very thing it is guarding against. Naming the classes also
     * keeps them genuinely referenced, so neither static analysis nor a future reader
     * mistakes them for dead code.
     *
     * Both packages are named explicitly: `archfixture` for the first two fixtures and
     * `archfixture.ui` for the third, which must sit in a `..ui..` package to be selected
     * by [PlatformBoundaryRules.uiAndDomainNeverTouchAnActivity] at all.
     */
    private val fixtures =
        ClassFileImporter().importPackagesOf(
            GmsBoundaryViolationFixture::class.java,
            ActivityBoundaryViolationFixture::class.java,
        )

    @Test
    fun theGmsContainmentRuleFailsOnAViolatingClass() {
        assertThrows(AssertionError::class.java) {
            PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks.check(fixtures)
        }
    }

    @Test
    fun theForegroundActivityContainmentRuleFailsOnAViolatingClass() {
        assertThrows(AssertionError::class.java) {
            PlatformBoundaryRules.onlyPlatformReachesTheForegroundActivity.check(fixtures)
        }
    }

    @Test
    fun theUiActivityRuleFailsOnAViolatingClass() {
        assertThrows(AssertionError::class.java) {
            PlatformBoundaryRules.uiAndDomainNeverTouchAnActivity.check(fixtures)
        }
    }
}
