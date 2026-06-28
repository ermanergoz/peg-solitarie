package com.app.pegsolitaire.di

import com.app.pegsolitaire.data.ChallengeLevelSource
import com.app.pegsolitaire.data.ClassicLevelSource
import com.app.pegsolitaire.data.DefaultClassicLevelSource
import com.app.pegsolitaire.data.DefaultLevelRepository
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.StubChallengeLevelSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for the data layer (Repository pattern, single source of truth).
 *
 * Uses `@Binds` (the abstract-class form) to map each interface to its
 * implementation: [LevelRepository] to [DefaultLevelRepository], and the two
 * level sources to their real/stub impls. Mirrors `DomainModule`'s
 * `@InstallIn(SingletonComponent::class)` shell; the sources are stateless
 * singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    @Singleton
    abstract fun bindLevelRepository(impl: DefaultLevelRepository): LevelRepository

    @Binds
    @Singleton
    abstract fun bindClassicLevelSource(impl: DefaultClassicLevelSource): ClassicLevelSource

    @Binds
    @Singleton
    abstract fun bindChallengeLevelSource(impl: StubChallengeLevelSource): ChallengeLevelSource
}
