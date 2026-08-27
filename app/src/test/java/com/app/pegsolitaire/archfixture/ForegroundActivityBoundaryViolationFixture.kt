package com.app.pegsolitaire.archfixture

import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider

/**
 * **Deliberately broken code. Do not fix it, and do not delete it.**
 *
 * Violates
 * [com.app.pegsolitaire.PlatformBoundaryRules.onlyPlatformReachesTheForegroundActivity]:
 * it reaches for [ForegroundActivityProvider] from `com.app.pegsolitaire.archfixture`,
 * which is outside `..platform..` and matches none of that rule's three exact-name
 * exemptions (`App`, `App_MembersInjector`, `PlatformModule`). It stands in for the thing
 * PLAT-01 actually fears — a ViewModel or composable acquiring an Activity — without this
 * repository having to contain such a ViewModel.
 *
 * The name matters: it must not collide with an exemption. Were this class called `App`,
 * the rule would exempt it, the gate test would see no violation, and the failure would
 * read as "the rule is broken" when in fact the fixture was.
 *
 * Deleting this class silently removes the proof that
 * `onlyPlatformReachesTheForegroundActivity` can fail. See
 * [com.app.pegsolitaire.PlatformBoundaryGateTest].
 */
@Suppress("unused")
class ForegroundActivityBoundaryViolationFixture {
    /** A field typed with the seam, so the dependency survives into bytecode. */
    val provider: ForegroundActivityProvider? = null
}
