package com.app.pegsolitaire.platform.games

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A settable in-memory [GamesService] for the Phase 11 JVM suite — the test seam that
 * makes PGS-01/02/03 verifiable without a Play Console project (D-04).
 *
 * It lives in `platform.games` rather than beside one consumer (the way
 * [com.app.pegsolitaire.ui.settings.FakeConsentGateway] sits in `ui.settings`) because
 * two unrelated packages need it: `ui.home` drives the opt-in prompt against it
 * (D-15/D-18) and `ui.settings` drives the manual sign-in row (D-08/D-10). Putting it in
 * either consumer's package would make the other import across a feature boundary, so it
 * sits with the interface it fakes, next to [NoOpGamesServiceTest].
 *
 * Two independently settable knobs, because a ViewModel test needs both halves of the
 * seam:
 *  - [session] via [emitSession], for the observed state a surface renders from;
 *  - [signInBehavior], for what an explicit [signIn] call does — including the D-10
 *    distinction a `Boolean` alone cannot express, where a silent decline and a genuine
 *    SDK error are different outcomes with different UI consequences.
 *
 * Defaults to [GamesSignInState.SignedOut] and [SignInBehavior.Success]: a test that
 * never touches Play Games sees the not-signed-in surface (the majority production case),
 * and one that calls [signIn] without configuring anything gets the happy path.
 *
 * @param session the initial observed sign-in state.
 */
class FakeGamesService(
    session: GamesSignInState = GamesSignInState.SignedOut,
) : GamesService {
    private val sessionState = MutableStateFlow(session)

    override val session: StateFlow<GamesSignInState> = sessionState.asStateFlow()

    /** What the next [signIn] call does. Defaults to the happy path. */
    var signInBehavior: SignInBehavior = SignInBehavior.Success()

    /** How many times [signIn] was called — proves a row/dialog action reached the seam. */
    var signInCallCount = 0
        private set

    /** How many times [bootstrapIfOptedIn] was called — the launch hook must run once (D-02). */
    var bootstrapCallCount = 0
        private set

    /**
     * The three outcomes a real `signIn()` can produce, kept distinct so a test can assert
     * that a decline stays silent while an error raises the D-10 snackbar.
     */
    sealed interface SignInBehavior {
        /** Authentication succeeds; the session becomes [GamesSignInState.SignedIn]. */
        data class Success(
            val playerId: String = DEFAULT_PLAYER_ID,
            val displayName: String? = DEFAULT_DISPLAY_NAME,
        ) : SignInBehavior

        /** A plain, player-initiated cancel: `false`, no throw, no session change (D-10). */
        data object Decline : SignInBehavior

        /** A genuine SDK-reported error: throws, which is what raises the snackbar (D-10). */
        data class Error(
            val exception: Exception = RuntimeException(DEFAULT_ERROR_MESSAGE),
        ) : SignInBehavior
    }

    /**
     * Runs the configured [signInBehavior].
     *
     * A [SignInBehavior.Success] also updates [session], mirroring the real implementation
     * where the state flow is the single source of truth the caller then observes — a test
     * asserting a signed-in surface after a successful sign-in needs no second
     * [emitSession] call. [SignInBehavior.Decline] deliberately leaves [session] untouched:
     * a cancel changes nothing, which is precisely why it must stay silent.
     */
    override suspend fun signIn(): Boolean {
        signInCallCount++
        return when (val behavior = signInBehavior) {
            is SignInBehavior.Success -> {
                sessionState.value =
                    GamesSignInState.SignedIn(
                        playerId = behavior.playerId,
                        displayName = behavior.displayName,
                    )
                true
            }

            SignInBehavior.Decline -> false

            is SignInBehavior.Error -> throw behavior.exception
        }
    }

    /**
     * Counts the call only. The fake deliberately does **not** re-implement D-01's opt-in
     * gate: a test that wants the post-bootstrap state pushes it with [emitSession], so
     * the gate itself is proven where it lives (`PlayGamesService`, plan 11-02) rather
     * than duplicated here where a drifting copy could pass while production fails.
     */
    override fun bootstrapIfOptedIn() {
        bootstrapCallCount++
    }

    override fun submitScore(
        leaderboardId: String,
        score: Long,
    ) = Unit

    override fun unlockAchievement(achievementId: String) = Unit

    override fun incrementAchievement(
        achievementId: String,
        steps: Int,
    ) = Unit

    override suspend fun showLeaderboard(leaderboardId: String) = Unit

    override suspend fun showAchievements() = Unit

    /** Simulates the sign-in state resolving after launch, or changing later. */
    fun emitSession(state: GamesSignInState) {
        sessionState.value = state
    }

    private companion object {
        const val DEFAULT_PLAYER_ID = "test-player"
        const val DEFAULT_DISPLAY_NAME = "Test Player"
        const val DEFAULT_ERROR_MESSAGE = "sign-in failed"
    }
}
