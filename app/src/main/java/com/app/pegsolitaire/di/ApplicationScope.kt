package com.app.pegsolitaire.di

import javax.inject.Qualifier

/**
 * Marks the application-lifetime [kotlinx.coroutines.CoroutineScope].
 *
 * A qualifier is required because `CoroutineScope` is far too generic a type to bind
 * unqualified — anything injecting a bare scope would silently receive this one. Use it
 * for work that must outlive any single screen/ViewModel (e.g. a fire-and-forget
 * DataStore write from a `@Singleton` holder such as
 * [com.app.pegsolitaire.data.ChallengeProgress]).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
