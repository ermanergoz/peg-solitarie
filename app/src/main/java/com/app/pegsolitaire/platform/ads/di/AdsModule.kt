package com.app.pegsolitaire.platform.ads.di

import com.app.pegsolitaire.platform.ads.consent.ConsentGateway
import com.app.pegsolitaire.platform.ads.consent.UmpConsentGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for the ads/consent seam.
 *
 * This module is separate from [com.app.pegsolitaire.di.PlatformModule] because that file
 * is the *stub-swap* module for Slice 1's no-op gateways, while this one is where the ads
 * seam's real bindings accumulate. Keeping it at `platform/ads/di/` also follows the
 * instruction in `PlatformModule`'s own KDoc about where ads-related Hilt declarations
 * belong.
 *
 * **There is no SDK-typed `@Provides` here, and there must never be one — in this module
 * or in any other.** The obvious design was a
 * `@Provides fun provideConsentInformation(…): ConsentInformation`, placed here precisely
 * because this package is inside `..platform..` where the SDK-containment rule permits SDK
 * types. That does not work, and the reason is worth knowing before someone tries it again:
 *
 * `PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks` reads **bytecode**, and the class
 * it catches is Hilt's generated `DaggerApp_HiltComponents_SingletonC`, which lives in the
 * **root** package `com.app.pegsolitaire` — outside `..platform..`. Declaring an SDK-typed
 * binding anywhere puts a `Provider<ThatSdkType>` field on the generated component, so the
 * rule fails on the *generated* class regardless of which module the `@Provides` was
 * written in. Verified empirically during Phase 9: the provider was written in this file,
 * inside `..platform..`, and `PlatformBoundaryTest` still went red with
 * `SingletonCImpl.provideConsentInformationProvider has generic type
 * Provider<com.google.android.ump.ConsentInformation>`.
 *
 * The fix is not to exempt the generated component — that would blind the gate to every
 * future SDK-typed binding, which is the one thing it exists to catch. The fix is to keep
 * SDK types out of the graph: a platform class constructs its own SDK handle behind its
 * own seam, and only the seam interface is bound. See
 * [com.app.pegsolitaire.platform.ads.consent.UmpConsentGateway]'s `consentInformation`
 * property for the shape.
 *
 * **That prediction was then tested.** The paragraph above warned that a future
 * rewarded-ad loader would hit the identical wall; plan 09-10 built one and, following
 * this advice, it never touched the wall at all.
 * [com.app.pegsolitaire.platform.ads.RewardedAdSlot] constructs and owns its `RewardedAd`
 * internally and takes only an application `CoroutineScope`, and
 * [com.app.pegsolitaire.platform.ads.AdMobAdGateway] exposes no ads type in any
 * constructor parameter or member signature. So the real ad gateway needed **no** addition
 * to this module: its `@Binds` sits in `PlatformModule` beside the other seam bindings,
 * naming only SDK-free types. The pattern's cost is one lazily-constructed handle per
 * platform class; its payoff is that the boundary gate stays at full strength.
 *
 * What is left here is therefore a single `@Binds` of an interface that names no SDK type —
 * which is exactly what the boundary rules are designed to allow.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AdsModule {
    /**
     * The consent seam (CONS-01..CONS-04), bound to its User Messaging Platform
     * implementation.
     *
     * `@Singleton` here scopes the binding of the *interface*; [UmpConsentGateway] also
     * carries `@Singleton` on the class, and must, because it holds the readiness flow and
     * the initialization guard. See that class's KDoc — the two annotations are not
     * redundant.
     *
     * Unlike `PlatformModule`'s ad and Play Games bindings this is **not** a swap point:
     * there was never a stub consent gateway to replace, because there is nothing to
     * consent to until an ads SDK is actually present.
     */
    @Binds
    @Singleton
    abstract fun bindConsentGateway(impl: UmpConsentGateway): ConsentGateway
}
