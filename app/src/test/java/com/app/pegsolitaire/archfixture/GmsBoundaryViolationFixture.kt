package com.app.pegsolitaire.archfixture

import com.google.android.gms.games.PlayGamesSdk

/**
 * **Deliberately broken code. Do not fix it, and do not delete it.**
 *
 * Violates [com.app.pegsolitaire.PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks]: it
 * lives in `com.app.pegsolitaire.archfixture`, which is outside `..platform..`, and it
 * names a Play SDK type. That is exactly the shape the production gate is supposed to
 * reject, and its only purpose is to make the gate demonstrate that it does — see
 * [com.app.pegsolitaire.PlatformBoundaryGateTest].
 *
 * Deleting this class does not break a build. It silently removes the proof that
 * `onlyPlatformDependsOnPlaySdks` can fail at all, leaving a rule that has only ever been
 * observed to pass — which is indistinguishable from a rule that matches nothing.
 *
 * [PlayGamesSdk] is a **real** class from `play-services-games-v2 22.0.0`, reachable here
 * only because plan 06-09 put that artifact on the graph (D-14). A hand-written decoy in
 * a `com.google.android.gms.fake` package was the alternative, and it would have proved
 * something weaker: that the rule matches package names this repository invented, rather
 * than the ones Google actually ships.
 */
@Suppress("unused")
class GmsBoundaryViolationFixture {
    /**
     * A field typed with a Play SDK class. A field, not a local variable, because
     * ArchUnit reads bytecode and a local's type is not retained there — the violation
     * has to survive compilation to be visible to the importer.
     */
    val playGamesSdk: PlayGamesSdk? = null
}
