package com.app.pegsolitaire.di

import com.app.pegsolitaire.domain.generator.LevelGenerator
import com.app.pegsolitaire.domain.rules.RulesEngine
import com.app.pegsolitaire.domain.scoring.ScoreCalculator
import com.app.pegsolitaire.domain.usecase.GetHintUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton
import kotlin.time.TimeSource

/**
 * Hilt bindings for the pure-domain dependencies the UI layer consumes.
 *
 * The domain is plain Kotlin with no Hilt annotations (it must stay
 * Android-free — QUAL-01), so the seam is provided here in `:app`: the stateless
 * [RulesEngine] and [ScoreCalculator] objects are exposed for `GameViewModel`, and
 * a monotonic [TimeSource] backs the in-game timer (D-04). The board is no longer
 * injected — it now flows from the selected `levelId` via `SavedStateHandle` +
 * `LevelRepository` (CLAS-02), so the old `provideInitialBoard()` is gone.
 */
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    /** The stateless rules authority; a singleton since it holds no per-game state. */
    @Provides
    @Singleton
    fun provideRulesEngine(): RulesEngine = RulesEngine

    /** The stateless scoring authority (SCOR-01/SCOR-02); a singleton — no per-game state. */
    @Provides
    @Singleton
    fun provideScoreCalculator(): ScoreCalculator = ScoreCalculator

    /**
     * The stateless, deterministic Challenge-level generator (CHAL-01..04). Provided
     * here as an `:app` seam because the domain object carries no Hilt annotations
     * (QUAL-01); `GeneratedChallengeLevelSource` injects it behind the repository.
     */
    @Provides
    @Singleton
    fun provideLevelGenerator(): LevelGenerator = LevelGenerator

    /**
     * The pure-domain hint decision (ASST-01/ASST-03): recorded-line-first, else the
     * budgeted `Solver`. Provided here as an `:app` seam since the use case is plain
     * Kotlin with no Hilt annotations (QUAL-01); `GameViewModel` runs it off-thread.
     */
    @Provides
    @Singleton
    fun provideGetHintUseCase(): GetHintUseCase = GetHintUseCase()

    /**
     * The monotonic clock the in-game timer marks against (D-04). Provided as an
     * injectable seam so tests swap in `kotlin.time.TestTimeSource` for deterministic
     * virtual-time assertions.
     */
    @Provides
    fun provideTimeSource(): TimeSource = TimeSource.Monotonic

    /**
     * The background dispatcher the O(level) Challenge generation runs on, keeping the
     * reverse-walk off the main thread so ViewModel construction never blocks the UI
     * (CR-01, T-03-09). [Dispatchers.Default] is the CPU-bound pool; tests inject a
     * test dispatcher instead for deterministic virtual-time control.
     */
    @Provides
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}
