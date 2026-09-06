package com.app.pegsolitaire.platform.games

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Proves the Slice-1 [NoOpGamesService] stub reports "not signed in" and that every one
 * of its submission and stock-UI hooks is a safe, silent no-op.
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
    fun submitScoreDoesNotThrow() {
        service.submitScore(leaderboardId = "challenge_high_score", score = 1_200L)
    }

    @Test
    fun unlockAchievementDoesNotThrow() {
        service.unlockAchievement(achievementId = "first_solve")
    }

    @Test
    fun incrementAchievementDoesNotThrow() {
        service.incrementAchievement(achievementId = "hundred_solves", steps = 1)
    }

    @Test
    fun showLeaderboardDoesNotThrow() =
        runTest {
            service.showLeaderboard(leaderboardId = "challenge_high_score")
        }

    @Test
    fun showAchievementsDoesNotThrow() =
        runTest {
            service.showAchievements()
        }
}
