package com.app.pegsolitaire.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.app.pegsolitaire.data.ChallengeLevelSource
import com.app.pegsolitaire.data.ClassicLevelSource
import com.app.pegsolitaire.data.DefaultClassicLevelSource
import com.app.pegsolitaire.data.DefaultLevelRepository
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.data.SettingsRepositoryImpl
import com.app.pegsolitaire.data.StubChallengeLevelSource
import com.app.pegsolitaire.data.appDataStore
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    /**
     * Instance `@Provides` cannot live on an abstract `@Module`, so the single shared
     * `app_prefs` `DataStore<Preferences>` (consumed by every preferences repository) is
     * provided here on the module's companion object.
     */
    companion object {
        @Provides
        @Singleton
        fun provideAppDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = context.appDataStore
    }
}
