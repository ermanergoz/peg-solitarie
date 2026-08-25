package com.app.pegsolitaire.platform.ads

import javax.inject.Inject

/**
 * The Slice-1 auto-grant [AdGateway] stub: every rewarded request is granted for free
 * ([requestRewarded] returns `true`) and [showInterstitial] is a no-op (D-11). This is
 * the single behavior Slice 2 replaces — swapping only the `PlatformModule` binding for
 * a real AdMob-backed gateway leaves all gameplay and UI untouched. Mirrors
 * [com.app.pegsolitaire.data.DefaultLevelRepository]'s `@Inject constructor` impl shape.
 */
class NoOpAdGateway
    @Inject
    constructor() : AdGateway {
        override suspend fun requestRewarded(reason: AdReason): Boolean = true

        override fun showInterstitial() = Unit
    }
