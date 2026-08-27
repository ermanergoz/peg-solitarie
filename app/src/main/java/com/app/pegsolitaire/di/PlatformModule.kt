package com.app.pegsolitaire.di

import com.app.pegsolitaire.platform.activity.DefaultForegroundActivityProvider
import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
import com.app.pegsolitaire.platform.ads.AdGateway
import com.app.pegsolitaire.platform.ads.NoOpAdGateway
import com.app.pegsolitaire.platform.games.GamesService
import com.app.pegsolitaire.platform.games.NoOpGamesService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for the platform seam (spec §8.4/§8.5). Mirrors `DataModule`'s
 * `@InstallIn(SingletonComponent::class)` + abstract interface-to-impl binding shape.
 *
 * Three bindings, of two different kinds:
 *
 * - [AdGateway] → [NoOpAdGateway] and [GamesService] → [NoOpGamesService] are the
 *   **Slice-2 swap points**: replacing them with a real AdMob-backed gateway and a
 *   `play-services-games-v2`-backed service introduces rewarded ads and leaderboards
 *   with no gameplay or UI change (D-11).
 * - [ForegroundActivityProvider] → [DefaultForegroundActivityProvider] is **not** a swap
 *   point. It is permanent: the Slice-2 implementations are precisely the callers that
 *   need a foreground Activity, so this binding is what makes the swap possible rather
 *   than something the swap replaces.
 *
 * The two stubs answer in opposite directions on purpose: the ad stub returns
 * [com.app.pegsolitaire.platform.ads.RewardOutcome.Granted] so assists stay free, while
 * the games stub declines sign-in (`false`) so nothing renders as though Play Games were
 * connected.
 *
 * **Do not add an SDK-typed `@Provides` to this file.** When Phase 9 needs to provide a
 * `MobileAds`, UMP or Play Games type, that module belongs at `platform/ads/di/` or
 * `platform/games/di/` — i.e. inside `..platform..`. This package is *outside*
 * `..platform..`, and Hilt's generated `DaggerApp_HiltComponents_SingletonC` references
 * every binding's types, so an SDK-typed provider here would trip `PlatformBoundaryTest`'s
 * SDK-containment rule with a confusing violation naming a generated class rather than
 * the file that caused it.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    @Binds
    @Singleton
    abstract fun bindAdGateway(impl: NoOpAdGateway): AdGateway

    @Binds
    @Singleton
    abstract fun bindGamesService(impl: NoOpGamesService): GamesService

    @Binds
    @Singleton
    abstract fun bindForegroundActivityProvider(impl: DefaultForegroundActivityProvider): ForegroundActivityProvider
}
