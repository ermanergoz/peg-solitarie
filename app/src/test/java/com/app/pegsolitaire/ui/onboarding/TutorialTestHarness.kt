package com.app.pegsolitaire.ui.onboarding

import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.pegsolitaire.data.OnboardingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.robolectric.Shadows.shadowOf
import java.time.Duration

/*
 * Shared fixtures for the tutorial's screen-level tests.
 *
 * They deliberately host `TutorialScreen` over a REAL `TutorialViewModel` rather than over a
 * hand-built state: what these tests are about is that a real jump on the real rules engine
 * drives the screen, and a stubbed state would prove neither.
 */

/**
 * `TutorialRoute` minus `hiltViewModel()` — the same wiring over an injected holder.
 *
 * A JVM Compose test has no Hilt host, and `TutorialRoute`'s own value is precisely that it
 * resolves the ViewModel from the `NavBackStackEntry`; that scoping is proven by `NavFlowTest`
 * and by the D-01g contract, not here. Everything below the holder is identical.
 */
@Composable
internal fun TutorialRouteUnderTest(viewModel: TutorialViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    TutorialScreen(
        state = state,
        onCellTap = viewModel::onCellTap,
        onAnimationFinished = viewModel::onAnimationFinished,
        onSkip = viewModel::onExit,
        onDone = viewModel::onExit,
    )
}

/**
 * An [OnboardingRepository] whose write always succeeds and is never observed.
 *
 * The exit path's *ordering* is `TutorialViewModelTest`'s subject; the screen tests only need
 * a repository that does not touch a real DataStore.
 */
internal class SilentOnboardingRepository : OnboardingRepository(UnusedPreferencesDataStore) {
    override val onboardingSeen: Flow<Boolean> = flowOf(false)

    override suspend fun markSeen() = Unit
}

/** Stands in for the injected `@ApplicationScope`: a process-lifetime scope, nobody's child. */
internal val TestApplicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

/**
 * Lets the tutorial's functional step beat elapse.
 *
 * It idles the **Robolectric main looper**, not `composeRule.mainClock`: the beat is a `delay`
 * on `viewModelScope`, i.e. on `Dispatchers.Main`, and the Compose test clock drives only the
 * frame clock its own `LaunchedEffect`s run on. Advancing the wrong clock leaves the next
 * footprint uninstalled and shows up as a missing node rather than as a timing failure, which
 * is exactly how long it takes to notice.
 */
internal fun idlePastTheStepBeat() {
    shadowOf(Looper.getMainLooper())
        .idleFor(Duration.ofMillis(TutorialAnim.STEP_ADVANCE_DELAY_MS + STEP_BEAT_SLACK_MS))
}

/** A little past the beat, so the boundary is never the thing under test. */
private const val STEP_BEAT_SLACK_MS = 50L

/** A [DataStore] the base repository is constructed over and never actually reads. */
private object UnusedPreferencesDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = emptyFlow()

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = emptyPreferences()
}
