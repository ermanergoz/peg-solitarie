package com.app.pegsolitaire.di

import android.content.Context
import androidx.room.Room
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.data.db.LevelResultDao
import com.app.pegsolitaire.data.db.PegSolitaireDatabase
import com.app.pegsolitaire.data.db.ResumeDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for the Room store.
 *
 * A `@Database` and its DAOs are built, not implemented by us, so they cannot be
 * `@Binds`-bound — and instance `@Provides` methods cannot live on an abstract
 * `@Module` (see `DataModule`, which parks its shared-DataStore provider on a
 * companion for the same reason). Hence an `object` module: the database is a
 * `@Singleton` (one connection pool for the process) and each DAO is provided from
 * it so repositories inject the narrow DAO they need rather than the whole database.
 *
 * **The destructive-migration fallback is deliberately absent and must stay absent.**
 * Room's drop-and-recreate escape hatch would make a schema change silently delete
 * every stored star, best time and in-progress game instead of failing loudly; the
 * exported schema plus `MigrationTest` exist precisely so a real `addMigrations(...)`
 * entry gets written instead (Success Criterion 5).
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun providePegSolitaireDatabase(
        @ApplicationContext context: Context,
    ): PegSolitaireDatabase =
        Room
            .databaseBuilder(
                context,
                PegSolitaireDatabase::class.java,
                ProgressPersistenceKeys.DB_NAME,
            )
            // Future schema versions land here as `.addMigrations(MIGRATION_1_2, …)`.
            .build()

    @Provides
    fun provideLevelResultDao(database: PegSolitaireDatabase): LevelResultDao = database.levelResultDao()

    @Provides
    fun provideResumeDao(database: PegSolitaireDatabase): ResumeDao = database.resumeDao()
}
