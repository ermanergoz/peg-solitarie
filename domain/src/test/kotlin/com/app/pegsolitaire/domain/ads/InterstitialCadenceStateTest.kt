package com.app.pegsolitaire.domain.ads

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Behaviour spec for the pure-domain [InterstitialCadenceState] policy (INT-02).
 *
 * Every expectation about a *threshold* is written as the matching
 * [InterstitialCadenceParams] constant — never as a bare `3`/`90_000`/`5`/`4` — because a test
 * literal is a second definition of the policy that can silently drift from the first. Bare
 * literals used as arbitrary *stored* values (a forged `9999`, a `-1`) are fine: they are
 * fixtures, not restatements of the policy.
 *
 * The core of the file is the D-01 x D-02 x D-03 x D-04 truth table: one baseline where all four
 * gates pass, then one test per gate flipping exactly one field, so every clause of the
 * conjunction is proven independently necessary and cannot be deleted without a red test.
 */
class InterstitialCadenceStateTest {
    @Test
    fun aStateSatisfyingAllFourGatesMayShowAnInterstitial() {
        assertThat(eligible().mayShow(NOW)).isTrue()
    }

    @Test
    fun oneWinShortOfTheLevelGateBlocks() {
        // D-01: wins since the last interstitial.
        val state =
            eligible().copy(
                winsSinceLastInterstitial = InterstitialCadenceParams.WINS_BETWEEN_INTERSTITIALS - 1,
            )

        assertThat(state.mayShow(NOW)).isFalse()
    }

    @Test
    fun oneMillisecondShortOfTheWallClockGapBlocks() {
        // D-02: the guard against speed-cleared levels stacking ads back to back.
        val state =
            eligible().copy(
                lastFullscreenAdAtMs = NOW - (InterstitialCadenceParams.MIN_GAP_MS - 1),
            )

        assertThat(state.mayShow(NOW)).isFalse()
    }

    @Test
    fun anUnfinishedFirstSessionBlocksHoweverManyLevelsAreDone() {
        // D-03, first half: suppression ends only when BOTH halves have passed.
        val state = eligible().copy(firstSessionComplete = false)

        assertThat(state.mayShow(NOW)).isFalse()
    }

    @Test
    fun oneLevelShortOfTheNewInstallThresholdBlocksEvenAfterTheFirstSessionEnded() {
        // D-03, second half — whichever of the two ends later is the one that governs.
        val state =
            eligible().copy(
                lifetimeCompletedLevels = InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS - 1,
            )

        assertThat(state.mayShow(NOW)).isFalse()
    }

    @Test
    fun reachingTheSessionCapBlocks() {
        // D-04.
        val state =
            eligible().copy(
                interstitialsThisSession = InterstitialCadenceParams.MAX_PER_SESSION,
            )

        assertThat(state.mayShow(NOW)).isFalse()
    }

    @Test
    fun exactlyTheRequiredNumberOfWinsPasses() {
        val state =
            eligible().copy(
                winsSinceLastInterstitial = InterstitialCadenceParams.WINS_BETWEEN_INTERSTITIALS,
            )

        assertThat(state.mayShow(NOW)).isTrue()
    }

    @Test
    fun exactlyTheMinimumWallClockGapPasses() {
        val state = eligible().copy(lastFullscreenAdAtMs = NOW - InterstitialCadenceParams.MIN_GAP_MS)

        assertThat(state.mayShow(NOW)).isTrue()
    }

    @Test
    fun exactlyTheNewInstallLevelThresholdPasses() {
        val state =
            eligible().copy(
                lifetimeCompletedLevels = InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS,
            )

        assertThat(state.mayShow(NOW)).isTrue()
    }

    @Test
    fun oneBelowTheSessionCapPasses() {
        val state =
            eligible().copy(
                interstitialsThisSession = InterstitialCadenceParams.MAX_PER_SESSION - 1,
            )

        assertThat(state.mayShow(NOW)).isTrue()
    }

    @Test
    fun aPlayerWhoHasNeverSeenAFullscreenAdPassesTheGapGateAtAnyTime() {
        // NEVER_SHOWN is not a stamp to subtract from — you have trivially waited long enough.
        val state = eligible().copy(lastFullscreenAdAtMs = InterstitialCadenceState.NEVER_SHOWN)

        assertThat(state.mayShow(0L)).isTrue()
        assertThat(state.mayShow(NOW)).isTrue()
    }

    @Test
    fun aBackwardsOrForgedFutureStampPassesTheGapGateRatherThanLockingItForever() {
        // T-10-07: an NTP correction must not lock a player out, and a forged future stamp must
        // not become an ad-suppression exploit. A negative delta OPENS this gate.
        val state = eligible().copy(lastFullscreenAdAtMs = NOW + InterstitialCadenceParams.MIN_GAP_MS)

        assertThat(state.mayShow(NOW)).isTrue()
    }

    @Test
    fun recordWinAdvancesBothSharedCountersAndTouchesNothingElse() {
        // D-07: one counter pair, incremented by a win in either mode.
        val before =
            InterstitialCadenceState(
                winsSinceLastInterstitial = 0,
                lastFullscreenAdAtMs = STORED_STAMP,
                lifetimeCompletedLevels = 0,
                firstSessionComplete = true,
                interstitialsThisSession = 1,
            )

        val after = before.recordWin()

        assertThat(after.winsSinceLastInterstitial).isEqualTo(1)
        assertThat(after.lifetimeCompletedLevels).isEqualTo(1)
        assertThat(after.lastFullscreenAdAtMs).isEqualTo(STORED_STAMP)
        assertThat(after.firstSessionComplete).isTrue()
        assertThat(after.interstitialsThisSession).isEqualTo(1)
    }

    @Test
    fun recordWinSaturatesTheWinCounterAtItsThresholdSoItCanNeverOverflow() {
        // T-10-05 / ASVS V5: the counter is only ever compared with `>=`, so growing past its
        // threshold carries no information — and capping there makes Int overflow unreachable.
        val atThreshold =
            InterstitialCadenceState(
                winsSinceLastInterstitial = InterstitialCadenceParams.WINS_BETWEEN_INTERSTITIALS,
            )

        val twice = atThreshold.recordWin().recordWin()

        assertThat(twice.winsSinceLastInterstitial)
            .isEqualTo(InterstitialCadenceParams.WINS_BETWEEN_INTERSTITIALS)
    }

    @Test
    fun recordWinSaturatesTheLifetimeCounterAtItsThresholdSoItCanNeverOverflow() {
        val atThreshold =
            InterstitialCadenceState(
                lifetimeCompletedLevels = InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS,
            )

        val twice = atThreshold.recordWin().recordWin()

        assertThat(twice.lifetimeCompletedLevels)
            .isEqualTo(InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS)
    }

    @Test
    fun recordInterstitialShownResetsTheWinsStampsTheClockAndCountsTheSession() {
        val after = eligible().recordInterstitialShown(NOW)

        assertThat(after.winsSinceLastInterstitial).isEqualTo(0)
        assertThat(after.lastFullscreenAdAtMs).isEqualTo(NOW)
        assertThat(after.interstitialsThisSession).isEqualTo(1)
        assertThat(after.lifetimeCompletedLevels)
            .isEqualTo(InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS)
        assertThat(after.firstSessionComplete).isTrue()
    }

    @Test
    fun recordInterstitialShownSaturatesTheSessionCounterAtTheCap() {
        val atCap =
            eligible().copy(
                interstitialsThisSession = InterstitialCadenceParams.MAX_PER_SESSION,
            )

        assertThat(atCap.recordInterstitialShown(NOW).interstitialsThisSession)
            .isEqualTo(InterstitialCadenceParams.MAX_PER_SESSION)
    }

    @Test
    fun recordRewardedShownOnlyStampsTheSharedFullscreenClock() {
        // D-15: a rewarded ad is not an interstitial — it must not consume a win or a session slot.
        val before = eligible()

        val after = before.recordRewardedShown(NOW)

        assertThat(after).isEqualTo(before.copy(lastFullscreenAdAtMs = NOW))
    }

    @Test
    fun recordRewardedShownBlocksAnImmediatelyFollowingInterstitialForTheWholeGap() {
        // The D-15 adjacency case: a rewarded ad opens the same 90-second cooldown, closing the
        // back-to-back fullscreen pairing AdMob policy prohibits.
        val before = eligible()
        assertThat(before.mayShow(NOW)).isTrue()

        val after = before.recordRewardedShown(NOW)

        assertThat(after.mayShow(NOW)).isFalse()
        assertThat(after.mayShow(NOW + InterstitialCadenceParams.MIN_GAP_MS - 1)).isFalse()
        assertThat(after.mayShow(NOW + InterstitialCadenceParams.MIN_GAP_MS)).isTrue()
    }

    @Test
    fun startNewSessionZeroesTheSessionCountAndEndsTheFirstSession() {
        val exhausted =
            InterstitialCadenceState(
                firstSessionComplete = false,
                interstitialsThisSession = InterstitialCadenceParams.MAX_PER_SESSION,
            )

        val after = exhausted.startNewSession()

        assertThat(after.interstitialsThisSession).isEqualTo(0)
        assertThat(after.firstSessionComplete).isTrue()
    }

    @Test
    fun aBackgroundGapOfExactlyTheSessionWindowStartsANewSession() {
        val isNew =
            InterstitialCadenceState.isNewSession(
                lastForegroundAtMs = NOW,
                nowMs = NOW + InterstitialCadenceParams.SESSION_GAP_MS,
            )

        assertThat(isNew).isTrue()
    }

    @Test
    fun aBackgroundGapOneMillisecondShortOfTheWindowIsStillTheSameSession() {
        val isNew =
            InterstitialCadenceState.isNewSession(
                lastForegroundAtMs = NOW,
                nowMs = NOW + InterstitialCadenceParams.SESSION_GAP_MS - 1,
            )

        assertThat(isNew).isFalse()
    }

    @Test
    fun aFirstEverLaunchIsNotANewSession() {
        // Load-bearing, not defensive: if launch one reported a new session it would immediately
        // set firstSessionComplete and delete D-03's first-session suppression on install day.
        val isNew =
            InterstitialCadenceState.isNewSession(
                lastForegroundAtMs = InterstitialCadenceState.NEVER_FOREGROUNDED,
                nowMs = NOW,
            )

        assertThat(isNew).isFalse()
    }

    @Test
    fun aBackwardsClockIsNotANewSessionSoTheSessionCapKeepsBinding() {
        // T-10-07 seen from the other side: a negative delta CLOSES this gate.
        val isNew =
            InterstitialCadenceState.isNewSession(
                lastForegroundAtMs = NOW + InterstitialCadenceParams.SESSION_GAP_MS,
                nowMs = NOW,
            )

        assertThat(isNew).isFalse()
    }

    @Test
    fun allNullStoredValuesYieldTheDefaultStateWhichShowsNoInterstitial() {
        // A fresh install or a corrupt store costs revenue, never goodwill (T-10-01).
        val restored =
            InterstitialCadenceState.fromStored(
                winsSinceLastInterstitial = null,
                lastFullscreenAdAtMs = null,
                lifetimeCompletedLevels = null,
                firstSessionComplete = null,
            )

        assertThat(restored).isEqualTo(InterstitialCadenceState())
        assertThat(restored.mayShow(NOW)).isFalse()
        assertThat(restored.mayShow(Long.MAX_VALUE)).isFalse()
    }

    @Test
    fun negativeStoredCountersReadBackAsZero() {
        val restored =
            InterstitialCadenceState.fromStored(
                winsSinceLastInterstitial = -1,
                lastFullscreenAdAtMs = null,
                lifetimeCompletedLevels = -1,
                firstSessionComplete = true,
            )

        assertThat(restored.winsSinceLastInterstitial).isEqualTo(0)
        assertThat(restored.lifetimeCompletedLevels).isEqualTo(0)
    }

    @Test
    fun forgedHighStoredCountersAreClampedDownToTheirThresholds() {
        // Clamped rather than rejected upward: the fail direction is FEWER ads, and a forged
        // counter must never buy more than an honest one could.
        val restored =
            InterstitialCadenceState.fromStored(
                winsSinceLastInterstitial = 9999,
                lastFullscreenAdAtMs = null,
                lifetimeCompletedLevels = 9999,
                firstSessionComplete = true,
            )

        assertThat(restored.winsSinceLastInterstitial)
            .isEqualTo(InterstitialCadenceParams.WINS_BETWEEN_INTERSTITIALS)
        assertThat(restored.lifetimeCompletedLevels)
            .isEqualTo(InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS)
    }

    @Test
    fun anAbsentFirstSessionFlagReadsBackAsFalse() {
        val restored =
            InterstitialCadenceState.fromStored(
                winsSinceLastInterstitial = InterstitialCadenceParams.WINS_BETWEEN_INTERSTITIALS,
                lastFullscreenAdAtMs = InterstitialCadenceState.NEVER_SHOWN,
                lifetimeCompletedLevels = InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS,
                firstSessionComplete = null,
            )

        assertThat(restored.firstSessionComplete).isFalse()
        assertThat(restored.mayShow(NOW)).isFalse()
    }

    @Test
    fun aStoredNeverShownStampIsPreservedRatherThanRewritten() {
        val restored =
            InterstitialCadenceState.fromStored(
                winsSinceLastInterstitial = null,
                lastFullscreenAdAtMs = InterstitialCadenceState.NEVER_SHOWN,
                lifetimeCompletedLevels = null,
                firstSessionComplete = null,
            )

        assertThat(restored.lastFullscreenAdAtMs).isEqualTo(InterstitialCadenceState.NEVER_SHOWN)
    }

    @Test
    fun aNegativeStoredFullscreenStampThatIsNotNeverShownReadsBackAsNeverShown() {
        val restored =
            InterstitialCadenceState.fromStored(
                winsSinceLastInterstitial = null,
                lastFullscreenAdAtMs = -2L,
                lifetimeCompletedLevels = null,
                firstSessionComplete = null,
            )

        assertThat(restored.lastFullscreenAdAtMs).isEqualTo(InterstitialCadenceState.NEVER_SHOWN)
    }

    @Test
    fun aStoredFullscreenStampOfLongMaxValueReadsBackAsNeverShownSoNoDeltaCanOverflow() {
        // T-10-08: otherwise `nowMs - lastFullscreenAdAtMs` would overflow on the next decision.
        val restored =
            InterstitialCadenceState.fromStored(
                winsSinceLastInterstitial = null,
                lastFullscreenAdAtMs = Long.MAX_VALUE,
                lifetimeCompletedLevels = null,
                firstSessionComplete = null,
            )

        assertThat(restored.lastFullscreenAdAtMs).isEqualTo(InterstitialCadenceState.NEVER_SHOWN)
    }

    /**
     * A state in which all four gates pass at [NOW] — the baseline every single-gate test above
     * flips exactly one field of.
     */
    private fun eligible(): InterstitialCadenceState =
        InterstitialCadenceState(
            winsSinceLastInterstitial = InterstitialCadenceParams.WINS_BETWEEN_INTERSTITIALS,
            lastFullscreenAdAtMs = NOW - InterstitialCadenceParams.MIN_GAP_MS,
            lifetimeCompletedLevels = InterstitialCadenceParams.SUPPRESSED_FIRST_LEVELS,
            firstSessionComplete = true,
            interstitialsThisSession = 0,
        )

    private companion object {
        /** An arbitrary "now", far enough from zero that every fixture subtraction stays positive. */
        const val NOW = 10_000_000L

        /** An arbitrary already-persisted stamp, used where the value must be shown untouched. */
        const val STORED_STAMP = 1_234L
    }
}
