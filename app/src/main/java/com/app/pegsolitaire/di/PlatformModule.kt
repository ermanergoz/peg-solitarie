package com.app.pegsolitaire.di

import com.app.pegsolitaire.platform.ads.AdGateway
import com.app.pegsolitaire.platform.ads.NoOpAdGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for the platform seam (spec §8.5). Mirrors `DataModule`'s
 * `@InstallIn(SingletonComponent::class)` + `@Binds @Singleton` shape: it maps the
 * [AdGateway] interface to the Slice-1 auto-grant [NoOpAdGateway] stub. This single
 * binding is the sole Slice-2 swap point — replacing it with a real AdMob-backed
 * gateway introduces rewarded ads with no gameplay or UI change (D-11).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlatformModule {
    @Binds
    @Singleton
    abstract fun bindAdGateway(impl: NoOpAdGateway): AdGateway
}
