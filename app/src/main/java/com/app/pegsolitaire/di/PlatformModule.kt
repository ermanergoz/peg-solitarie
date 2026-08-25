package com.app.pegsolitaire.di

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
 * `@InstallIn(SingletonComponent::class)` + `@Binds @Singleton` shape: it maps
 * [AdGateway] to the Slice-1 auto-grant [NoOpAdGateway] stub and [GamesService] to
 * the [NoOpGamesService] stub. These two bindings are the *only* Slice-2 swap points
 * — replacing them with a real AdMob-backed gateway and a
 * `play-services-games-v2`-backed service introduces rewarded ads and leaderboards
 * with no gameplay or UI change (D-11).
 *
 * The two stubs answer in opposite directions on purpose: the ad stub grants (`true`)
 * so assists stay free, while the games stub declines sign-in (`false`) so nothing
 * renders as though Play Games were connected.
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
}
