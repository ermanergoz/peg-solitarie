package com.app.pegsolitaire.platform.games

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Proves the Slice-1 [NoOpGamesService] stub reports "not signed in", that every
 * submission reports `false`, and that both stock-UI hooks are safe, silent no-ops.
 *
 * The submission cases assert `false` rather than merely "does not throw", and the
 * distinction is the point: `false` means *nothing was submitted*, which is what keeps a
 * caller's retry bookkeeping honest. A stub that reported success would have the caller
 * clear its pending-submissions flag (D-10) for a submission that never left the device,
 * losing it silently — the one failure mode a stub is supposed to make impossible.
 *
 * Note the deliberate contrast with `NoOpAdGatewayTest`: the ad stub *grants*
 * ([com.app.pegsolitaire.platform.ads.RewardOutcome.Granted]) so hint/undo stay free
 * without an SDK, whereas this stub *declines* — Play Games really is unavailable in
 * Slice 1, so any future leaderboard/achievement UI must treat sign-in as failed and
 * hide itself rather than pretend the player is connected. There is no games-side
 * equivalent of the ad seam's fail-open [com.app.pegsolitaire.platform.ads.RewardOutcome.Unavailable]
 * branch: an unavailable leaderboard costs the player nothing, so declining is always
 * the safe answer. Phase 11 swaps the `PlatformModule` binding, not this behaviour.
 */
class NoOpGamesServiceTest {
    private val service = NoOpGamesService()

    @Test
    fun signInReportsNotSignedIn() =
        runTest {
            assertThat(service.signIn()).isFalse()
        }

    @Test
    fun sessionReportsSignedOut() {
        assertThat(service.session.value).isEqualTo(GamesSignInState.SignedOut)
    }

    @Test
    fun sessionIsNeverUnknownSoNoSurfaceSpinsForever() {
        assertThat(service.session.value).isNotEqualTo(GamesSignInState.Unknown)
    }

    @Test
    fun sessionNeverReportsAConnectedPlayer() {
        assertThat(service.session.value).isNotInstanceOf(GamesSignInState.SignedIn::class.java)
    }

    @Test
    fun bootstrapIfOptedInDoesNotThrow() {
        // D-01: the stub has no persisted opt-in flag to read and no SDK to initialize, so the
        // launch-time bootstrap must be a silent no-op rather than an error path.
        service.bootstrapIfOptedIn()
    }

    @Test
    fun submitScoreReportsNothingWasSubmitted() =
        runTest {
            assertThat(service.submitScore(leaderboardId = PLACEHOLDER_LEADERBOARD_ID, score = 1_200L)).isFalse()
        }

    @Test
    fun unlockAchievementReportsNothingWasSubmitted() =
        runTest {
            assertThat(service.unlockAchievement(achievementId = PLACEHOLDER_ACHIEVEMENT_ID)).isFalse()
        }

    @Test
    fun setAchievementStepsReportsNothingWasSubmitted() =
        runTest {
            assertThat(service.setAchievementSteps(achievementId = PLACEHOLDER_ACHIEVEMENT_ID, steps = 1)).isFalse()
        }

    @Test
    fun showLeaderboardsDoesNotThrow() =
        runTest {
            service.showLeaderboards()
        }

    @Test
    fun showAchievementsDoesNotThrow() =
        runTest {
            service.showAchievements()
        }

    private companion object {
        /**
         * Obviously-fake ids. The stub never reads them, and no real Play Console id belongs
         * in source — real ids are resolved at build time, and this seam takes them as opaque
         * parameters (D-13).
         */
        const val PLACEHOLDER_LEADERBOARD_ID = "placeholder-leaderboard-id"
        const val PLACEHOLDER_ACHIEVEMENT_ID = "placeholder-achievement-id"
    }
}
