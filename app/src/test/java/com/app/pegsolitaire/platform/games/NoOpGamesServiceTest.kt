package com.app.pegsolitaire.platform.games

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Proves the Slice-1 [NoOpGamesService] stub reports "not signed in" and that its
 * score-submission hook is a safe no-op.
 *
 * Note the deliberate contrast with `NoOpAdGatewayTest`: the ad stub *grants*
 * (`true`) so hint/undo stay free without an SDK, whereas this stub *declines*
 * (`false`) — Play Games really is unavailable in Slice 1, so any future
 * leaderboard/achievement UI must treat sign-in as failed and hide itself rather than
 * pretend the player is connected. Slice 2 swaps the `PlatformModule` binding, not
 * this behaviour.
 */
class NoOpGamesServiceTest {
    private val service = NoOpGamesService()

    @Test
    fun signInReportsNotSignedIn() =
        runTest {
            assertThat(service.signIn()).isFalse()
        }

    @Test
    fun submitScoreDoesNotThrow() {
        service.submitScore(leaderboardId = "challenge_high_score", score = 1_200L)
    }
}
