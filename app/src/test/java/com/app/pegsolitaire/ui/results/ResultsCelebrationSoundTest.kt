package com.app.pegsolitaire.ui.results

import android.provider.Settings
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import com.app.pegsolitaire.audio.FakeSoundPlayer
import com.app.pegsolitaire.audio.LocalSoundPlayer
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.audio.SoundParams
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * What the Results celebration sounds like — the second beat of D-14, asserted JVM-side.
 *
 * The audio companion to [ResultsScreenTest]: same Robolectric shell, same
 * `createComposeRule`, same `ResultsUiState` fixtures, but with a recording
 * `FakeSoundPlayer` installed through `CompositionLocalProvider(LocalSoundPlayer …)` —
 * the same trick `GameScreenSoundTest` uses for the board.
 *
 * Timing is asserted, not slept through: `mainClock.autoAdvance = false` freezes the
 * celebration and every assertion below advances virtual time to an exact point in it, so
 * the D-14 lead-in gap and the per-pop stagger are deterministic facts rather than races.
 *
 * The three decisions this pins:
 *  - **D-15, the ladder is a prefix.** Pops always start at [SoundParams.starPopRate] step
 *    0 and climb, so a 2-star result plays notes 1-2 and the top resolving note is audibly
 *    reserved for a 3-star finish. The 2-star case asserts that top note is *absent*.
 *  - **D-13, the audio survives reduced motion.** With the OS animator duration scale at 0
 *    the visual stars land instantly, but the pops still sequence — on their own fixed
 *    stagger — instead of collapsing into one simultaneous burst.
 *  - **D-16, once per win.** A `playCelebration = false` state (the re-entry after a back
 *    navigation or a process-death restore) is completely silent at any star count.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [CELEBRATION_SOUND_ROBOLECTRIC_SDK])
class ResultsCelebrationSoundTest {
    @get:Rule
    val composeRule = createComposeRule()

    /** Records every cue the celebration fires instead of playing it. */
    private val sounds = FakeSoundPlayer()

    private var state by mutableStateOf(resultsState(stars = THREE_STARS, play = true))

    // ---- The rising ladder (SOUND-03, D-15) ---------------------------------

    @Test
    fun threeStarsClimbTheWholeLadderInOrder() {
        celebrate(stars = THREE_STARS)

        advance(CELEBRATION_SETTLE_MS)

        assertThat(popRates())
            .containsExactly(popRate(0), popRate(1), popRate(2))
            .inOrder()
    }

    @Test
    fun twoStarsPlayTheLadderPrefixAndNeverTheTopNote() {
        celebrate(stars = TWO_STARS)

        advance(CELEBRATION_SETTLE_MS)

        // D-15: the ladder is a prefix, never centred or rescaled onto the earned count —
        // the resolving third note is the thing a 3-star finish buys.
        assertThat(popRates()).containsExactly(popRate(0), popRate(1)).inOrder()
        assertThat(popRates()).doesNotContain(popRate(TOP_LADDER_STEP))
    }

    @Test
    fun oneStarPlaysASinglePopAtTheBaseNote() {
        celebrate(stars = ONE_STAR)

        advance(CELEBRATION_SETTLE_MS)

        assertThat(popRates()).containsExactly(popRate(0))
    }

    // ---- The two-beat gap after the board's win stinger (D-14) --------------

    @Test
    fun theFirstPopWaitsForTheLeadInGapAfterTheWinStinger() {
        celebrate(stars = THREE_STARS)

        advance(BEFORE_LEAD_IN_MS)

        // The stinger fired on the board (07-03); the ceremony is a separate beat, so
        // nothing has sounded yet this early into the Results screen.
        assertThat(sounds.events).isEmpty()

        advance(CELEBRATION_SETTLE_MS)
        assertThat(popRates()).hasSize(THREE_STARS)
    }

    // ---- Silent re-entry (D-16) ---------------------------------------------

    @Test
    fun aSilentReEntryPlaysNothingAtAnyStarCount() {
        celebrate(stars = THREE_STARS, play = false)

        advance(CELEBRATION_SETTLE_MS)

        assertThat(sounds.events).isEmpty()
    }

    // ---- Reduced motion (D-13) ----------------------------------------------

    @Test
    fun withOsAnimationsOffThePopsStillSequenceOnTheirOwnStagger() {
        disableOsAnimations()
        celebrate(stars = THREE_STARS)

        // Past the lead-in but inside the first fallback stagger: exactly one pop has
        // landed. Were the audio to follow the visual gate it would have burst all three
        // at once here, which is precisely what D-13 refuses.
        advance(JUST_PAST_LEAD_IN_MS)
        assertThat(popRates()).hasSize(1)

        advance(CELEBRATION_SETTLE_MS)
        assertThat(popRates())
            .containsExactly(popRate(0), popRate(1), popRate(2))
            .inOrder()
    }

    // ---- The sequence is keyed, not re-run per composition (T-07-11) --------

    @Test
    fun recomposingWithoutChangingTheCelebrationDoesNotReplayIt() {
        celebrate(stars = THREE_STARS)
        advance(CELEBRATION_SETTLE_MS)

        // A state change that touches neither the star count nor the latch: the effect key
        // is unchanged, so the ladder must not restart.
        state = state.copy(score = ANOTHER_SCORE)
        advance(CELEBRATION_SETTLE_MS)

        assertThat(popRates()).hasSize(THREE_STARS)
    }

    // ---- Helpers -------------------------------------------------------------

    /** Composes the real Results screen over a frozen clock, with the recording player installed. */
    private fun celebrate(
        stars: Int,
        play: Boolean = true,
    ) {
        state = resultsState(stars = stars, play = play)
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalSoundPlayer provides sounds) {
                    ResultsScreen(
                        state = state,
                        onReplay = {},
                        onCatalog = {},
                        onHome = {},
                        onNextLevel = {},
                        onRetry = {},
                        onNavigateBack = {},
                    )
                }
            }
        }
    }

    private fun advance(milliseconds: Long) {
        composeRule.mainClock.advanceTimeBy(milliseconds)
    }

    /** The recorded star-pop rates in fire order; any other cue would fail the size checks. */
    private fun popRates(): List<Float> = sounds.events.filter { it.cue == SoundCue.StarPop }.map { it.rate }

    private fun disableOsAnimations() {
        Settings.Global.putFloat(
            RuntimeEnvironment.getApplication().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            ANIMATIONS_DISABLED_SCALE,
        )
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val CELEBRATION_SOUND_ROBOLECTRIC_SDK = 34

/** Earned-star counts, and the ladder step only a 3-star finish ever reaches (D-15). */
private const val THREE_STARS = 3
private const val TWO_STARS = 2
private const val ONE_STAR = 1
private const val TOP_LADDER_STEP = 2

/** A solved board leaves exactly one peg. */
private const val CELEBRATION_PEGS_REMAINING = 1

/** Arbitrary pass-through metrics; the celebration reads none of them. */
private const val CELEBRATION_ELAPSED_SECONDS = 75
private const val CELEBRATION_SCORE = 1280

/** A different score, used purely to force a recomposition that changes no effect key. */
private const val ANOTHER_SCORE = 1281

/** Comfortably inside the D-14 lead-in gap, so no pop can have sounded yet. */
private const val BEFORE_LEAD_IN_MS = 200L

/** Past the lead-in but inside the first fallback stagger — exactly one pop has landed. */
private const val JUST_PAST_LEAD_IN_MS = 300L

/** Far past the whole three-pop sequence on either stagger. */
private const val CELEBRATION_SETTLE_MS = 1_500L

/** The OS animator duration scale that means "animations off" (D-13). */
private const val ANIMATIONS_DISABLED_SCALE = 0f

/** The rate the [step]-th pop must be played at. */
private fun popRate(step: Int): Float = SoundParams.starPopRate(step)

/** A solved Classic result with [stars] earned, entering Results for the [play] time. */
private fun resultsState(
    stars: Int,
    play: Boolean,
): ResultsUiState =
    ResultsUiState(
        stars = stars,
        pegsRemaining = CELEBRATION_PEGS_REMAINING,
        elapsedSeconds = CELEBRATION_ELAPSED_SECONDS,
        score = CELEBRATION_SCORE,
        headlineTier = ResultsHeadlineTier.PERFECT,
        replayId = ClassicLevelIds.ENGLISH33,
        playCelebration = play,
    )
