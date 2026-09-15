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
 * Three independently settable knobs, because a ViewModel test needs every half of the
 * seam:
 *  - [session] via [emitSession], for the observed state a surface renders from;
 *  - [signInBehavior], for what an explicit [signIn] call does — including the D-10
 *    distinction a `Boolean` alone cannot express, where a silent decline and a genuine
 *    SDK error are different outcomes with different UI consequences;
 *  - [submissionResult], for what every submission reports.
 *
 * Defaults to [GamesSignInState.SignedOut], [SignInBehavior.Success] and a `true`
 * [submissionResult]: a test that never touches Play Games sees the not-signed-in surface
 * (the majority production case), and one that calls [signIn] or submits without
 * configuring anything gets the happy path.
 *
 * ## Recording, and why it records arguments rather than just counts
 *
 * Every submission is appended to a typed list ([submittedScores], [unlockedAchievements],
 * [achievementSteps]) so a test can assert **which** ids were submitted and **in what
 * order** — the two questions Phase 12's `ProgressSubmitter` (plan 12-07) is defined by,
 * since its whole job is deciding what to send and recording the D-10 pending flag when a
 * `false` comes back. Counts alone would let a submitter that sends the wrong id, or sends
 * one id twice and another never, pass.
 *
 * [achievementSteps] records the **value**, not just the call, because D-12's steps are
 * absolute: a submitter that sends a delta where an absolute count belongs produces exactly
 * the same call count and a silently wrong number, which is invisible to any assertion that
 * does not look at the argument.
 *
 * The lists are exposed read-only with private setters, so a test observes them and never
 * seeds them. **No leaderboard or achievement id is named in this file** — every id arrives
 * as a parameter (D-13).
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
     * What every submission reports. `true` is "the SDK accepted it"; setting it to `false`
     * drives the caller's failure path — for `ProgressSubmitter` (plan 12-07), the D-10
     * pending-submissions flag.
     */
    var submissionResult: Boolean = true

    /** Every [submitScore] call, in order, with its arguments. */
    var submittedScores: List<ScoreSubmission> = emptyList()
        private set

    /** Every [unlockAchievement] call, in order, by achievement id. */
    var unlockedAchievements: List<String> = emptyList()
        private set

    /** Every [setAchievementSteps] call, in order, with its **absolute** step value (D-12). */
    var achievementSteps: List<StepUpdate> = emptyList()
        private set

    /** How many times [showLeaderboards] was called — proves a Settings row tap reached the seam. */
    var showLeaderboardsCallCount = 0
        private set

    /** How many times [showAchievements] was called — the [showLeaderboardsCallCount] counterpart. */
    var showAchievementsCallCount = 0
        private set

    /**
     * One recorded [submitScore]. A named type rather than a `Pair` so a failed assertion
     * reads `leaderboardId=…` instead of `first=…`.
     */
    data class ScoreSubmission(
        val leaderboardId: String,
        val score: Long,
    )

    /**
     * One recorded [setAchievementSteps], named for the same reason as [ScoreSubmission] —
     * and here it also keeps `steps` from being read as a delta.
     */
    data class StepUpdate(
        val achievementId: String,
        val steps: Int,
    )

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

    override fun initializeIfOptedIn() = Unit

    override suspend fun submitScore(
        leaderboardId: String,
        score: Long,
    ): Boolean {
        submittedScores = submittedScores + ScoreSubmission(leaderboardId, score)
        return submissionResult
    }

    override suspend fun unlockAchievement(achievementId: String): Boolean {
        unlockedAchievements = unlockedAchievements + achievementId
        return submissionResult
    }

    override suspend fun setAchievementSteps(
        achievementId: String,
        steps: Int,
    ): Boolean {
        achievementSteps = achievementSteps + StepUpdate(achievementId, steps)
        return submissionResult
    }

    /**
     * Records the call and nothing else — the real one hands back no result, so there is no
     * outcome a test could configure without inventing one production cannot produce.
     */
    override suspend fun showLeaderboards() {
        showLeaderboardsCallCount++
    }

    override suspend fun showAchievements() {
        showAchievementsCallCount++
    }

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
