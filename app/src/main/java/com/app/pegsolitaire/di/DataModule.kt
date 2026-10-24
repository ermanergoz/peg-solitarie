package com.app.pegsolitaire.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.app.pegsolitaire.data.ChallengeLevelSource
import com.app.pegsolitaire.data.ClassicLevelSource
import com.app.pegsolitaire.data.DefaultClassicLevelSource
import com.app.pegsolitaire.data.DefaultLevelRepository
import com.app.pegsolitaire.data.DefaultProgressRepository
import com.app.pegsolitaire.data.GeneratedChallengeLevelSource
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.data.SettingsRepositoryImpl
import com.app.pegsolitaire.data.appDataStore
import com.app.pegsolitaire.data.licenses.AssetLicenseTextSource
import com.app.pegsolitaire.data.licenses.LicenseTextSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    abstract fun bindChallengeLevelSource(impl: GeneratedChallengeLevelSource): ChallengeLevelSource

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    /**
     * The single source of truth for per-level bests (PERS-01) and resume snapshots
     * (PERS-03). A `@Singleton` because it fronts the process-wide Room connection.
     */
    @Binds
    @Singleton
    abstract fun bindProgressRepository(impl: DefaultProgressRepository): ProgressRepository

    // Licences (feature/licences-screen)

    /**
     * The licences screens' reader of the bundled texts and the generated Google notices (tech
     * design §3.6, §13.1 LN-4). Unscoped: it holds nothing, and only a licences ViewModel asks.
     */
    @Binds
    abstract fun bindLicenseTextSource(impl: AssetLicenseTextSource): LicenseTextSource

    /**
     * Instance `@Provides` cannot live on an abstract `@Module`, so the single shared
     * `app_prefs` `DataStore<Preferences>` (consumed by every preferences repository) and
     * the application-lifetime [CoroutineScope] are provided here on the module's
     * companion object.
     */
    companion object {
        @Provides
        @Singleton
        fun provideAppDataStore(
            @ApplicationContext context: Context,
        ): DataStore<Preferences> = context.appDataStore

        /**
         * The app-lifetime scope used for fire-and-forget persistence writes from
         * `@Singleton` holders (e.g. `ChallengeProgress.advancePast()`), plus the
         * sharing scope for their `stateIn`-backed StateFlows.
         *
         * `SupervisorJob` so one failed write never cancels the sibling writers, and
         * `Dispatchers.Default` because DataStore already moves file IO to its own
         * dispatcher. Never cancelled — it is scoped to the process.
         */
        @Provides
        @Singleton
        @ApplicationScope
        fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }
}
