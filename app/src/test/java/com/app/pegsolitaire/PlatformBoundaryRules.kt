package com.app.pegsolitaire

import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses

/**
 * The single definition of the three platform-boundary rules (PLAT-04, PLAT-01).
 *
 * **These definitions are a contract shared by two tests.** [PlatformBoundaryTest] runs
 * them against production bytecode as the build gate; plan 06-09's negative gate runs the
 * *same objects* against a deliberately-violating fixture package to prove they actually
 * fail when violated. `ArchRule`s are ordinary values, so one definition can serve both
 * import scopes — and it must. Two copies of a rule is how a gate quietly stops matching
 * what it claims to check: the production copy drifts, the negative copy keeps passing
 * against the old shape, and nothing reports that the gate has gone blind. Change a rule
 * here and both callers change with it.
 *
 * Every rule matches by **name**, never by assignability. `android.app.Activity` and the
 * Play SDK types live outside the `@AnalyzeClasses` import scope, so ArchUnit resolves
 * them to stubs and cannot walk their hierarchies — an assignability predicate would
 * silently match nothing. Name matching is resolution-independent and therefore the only
 * reliable form here.
 */
internal object PlatformBoundaryRules {
    /**
     * The Play/AdMob/UMP SDKs stay inside `..platform..`.
     *
     * Bytecode-based, so it also covers the Hilt/KSP-generated component: an SDK-typed
     * `@Provides` outside `..platform..` would surface here as a violation naming
     * `DaggerApp_HiltComponents_SingletonC`. Expected to pass **vacuously** until plan
     * 06-09 puts the SDK artifacts on the graph.
     */
    val onlyPlatformDependsOnPlaySdks: ArchRule =
        noClasses()
            .that()
            .resideOutsideOfPackage("..platform..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "com.google.android.gms..",
                "com.google.android.ump..",
                "com.google.android.libraries.ads..",
            )

    /**
     * The foreground-Activity seam stays inside `..platform..`.
     *
     * Three exemptions, and they are all the *same* exemption seen from three angles —
     * the one sanctioned registration path. Nothing else outside `..platform..` may name
     * this type; in particular no ViewModel, no composable and no `:domain` class:
     *
     * 1. `.*\.App` — [App] lives in the root package and must depend on the interface to
     *    call `attachTo`. Documented at both ends; see [App]'s KDoc.
     * 2. `.*\.App_MembersInjector` — Dagger's generated field-injector for that exact
     *    `@Inject` field. It is codegen for exemption 1, not a second decision, and it
     *    appears only because this gate reads bytecode rather than source.
     * 3. `.*\.PlatformModule` — the `@Binds` declaration must name its own return type,
     *    and `di/` sits outside `..platform..`. Without this, the binding that *creates*
     *    the seam would be the thing that fails the seam's own rule.
     *
     * Each is matched by exact name, never by package, so the holes stay the size of the
     * three classes named here. Plan 06-09's violating fixture is caught by this rule
     * precisely because it matches none of them.
     */
    val onlyPlatformReachesTheForegroundActivity: ArchRule =
        noClasses()
            .that()
            .resideOutsideOfPackage("..platform..")
            .and()
            .haveNameNotMatching(".*\\.App")
            .and()
            .haveNameNotMatching(".*\\.App_MembersInjector")
            .and()
            .haveNameNotMatching(".*\\.PlatformModule")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName(
                "com.app.pegsolitaire.platform.activity.ForegroundActivityProvider",
            )

    /**
     * No UI, domain or data class touches an `Activity` at all — the backstop that
     * catches an Activity arriving by some route other than the seam above, such as a
     * composable reaching for `LocalActivity`.
     */
    val uiAndDomainNeverTouchAnActivity: ArchRule =
        noClasses()
            .that()
            .resideInAnyPackage("..ui..", "..domain..", "..data..")
            .should()
            .dependOnClassesThat()
            .haveFullyQualifiedName("android.app.Activity")
}
