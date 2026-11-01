package com.app.pegsolitaire.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Qualifier

/**
 * Marks the [CoroutineDispatcher] for blocking file reads ([Dispatchers.IO]).
 *
 * Qualified because `DomainModule` already binds the unqualified `CoroutineDispatcher` to
 * `Dispatchers.Default`, and a second unqualified binding would be a Hilt duplicate-binding error
 * (tech design §3.6). Inject it with the `@param:` use-site target on a constructor property, the
 * form tech design §9.8 rules for every qualifier.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class IoDispatcher

/** Provides the [IoDispatcher]-qualified dispatcher; tests construct their sources with a test one. */
@Module
@InstallIn(SingletonComponent::class)
object DispatchersModule {
    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
