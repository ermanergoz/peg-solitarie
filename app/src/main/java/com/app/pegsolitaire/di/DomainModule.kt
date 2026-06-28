package com.app.pegsolitaire.di

import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.rules.RulesEngine
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for the pure-domain dependencies the UI layer consumes.
 *
 * The domain is plain Kotlin with no Hilt annotations (it must stay
 * Android-free — QUAL-01), so the seam is provided here in `:app`: the stateless
 * [RulesEngine] object is exposed as a singleton, and the canonical English-33
 * start (D-01) is provided as the initial [Board] that `GameViewModel` plays and
 * restarts back to.
 */
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    /** The stateless rules authority; a singleton since it holds no per-game state. */
    @Provides
    @Singleton
    fun provideRulesEngine(): RulesEngine = RulesEngine

    /** The English-33 starting board (center-empty, 32 pegs) — D-01. */
    @Provides
    fun provideInitialBoard(): Board = Boards.english33Initial
}
