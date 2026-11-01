package com.app.pegsolitaire.archfixture.ui

import android.app.Activity

/**
 * **Deliberately broken code. Do not fix it, and do not delete it.**
 *
 * Violates
 * [com.app.pegsolitaire.PlatformBoundaryRules.uiAndDomainNeverTouchAnActivity]: it depends
 * on [Activity] from a package matching `..ui..`.
 *
 * **The `.ui` package segment is load-bearing and is why this file is not next to its two
 * sibling fixtures.** That rule's `that()` clause is
 * `resideInAnyPackage("..ui..", "..domain..", "..data..")`, so a class sitting in
 * `com.app.pegsolitaire.archfixture` alone would not be selected by the rule at all. The
 * rule would then pass against it — vacuously, having examined nothing — and a gate test
 * asserting "this throws" would fail for a reason that has nothing to do with the rule's
 * correctness. Worse, the inverse mistake is silent: if the assertion were "this passes",
 * a fixture outside `..ui..` would make it green forever while proving nothing.
 *
 * Deleting this class silently removes the proof that `uiAndDomainNeverTouchAnActivity`
 * can fail. See [com.app.pegsolitaire.PlatformBoundaryGateTest].
 */
@Suppress("unused")
class ActivityBoundaryViolationFixture {
    /** A field typed with [Activity], so the dependency survives into bytecode. */
    val activity: Activity? = null
}
