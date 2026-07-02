package com.app.pegsolitaire.data

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Behaviour spec for the in-memory [ChallengeProgress] holder (D-04).
 *
 * A plain JVM test — [ChallengeProgress] has zero Android dependencies (it is the
 * Phase-5-persistable seam, not yet DataStore-backed). It reads the current level
 * synchronously off the exposed `StateFlow.value`, so no coroutine harness is needed.
 */
class ChallengeProgressTest {
    @Test
    fun challengeProgressStartsAtLevelOne() {
        // D-04: Challenge starts at level 1 each session this phase.
        assertThat(ChallengeProgress().currentLevel.value).isEqualTo(1)
    }

    @Test
    fun advanceToNextIncrementsTheCurrentLevel() {
        val progress = ChallengeProgress()

        progress.advanceToNext()
        assertThat(progress.currentLevel.value).isEqualTo(2)

        progress.advanceToNext()
        assertThat(progress.currentLevel.value).isEqualTo(3)
    }

    @Test
    fun resetReturnsToLevelOne() {
        val progress = ChallengeProgress()
        progress.advanceToNext()
        progress.advanceToNext()

        progress.reset()

        assertThat(progress.currentLevel.value).isEqualTo(1)
    }
}
