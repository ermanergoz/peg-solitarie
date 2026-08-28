package com.app.pegsolitaire.audio

/**
 * One recorded playback: which cue, at what rate, and whether it took the D-12 gate bypass.
 *
 * Every event carries a rate — unpitched cues record [SoundParams.JUMP_BASE_RATE] — so a
 * test can compare the whole sequence uniformly instead of special-casing jumps and stars.
 */
internal data class PlayedCue(
    val cue: SoundCue,
    val rate: Float,
    val gateBypassed: Boolean = false,
)

/**
 * A recording [SoundPlayer] for screen tests: it plays nothing and remembers everything.
 *
 * The same idea as `GameFlowTest`'s `RecordingHapticFeedback`, installed the same way —
 * `CompositionLocalProvider(LocalSoundPlayer provides FakeSoundPlayer())` — so a test can
 * assert *which* cue fired, *in what order* and *at what pitch*, rather than asserting on
 * a UI flag that merely implies a sound.
 *
 * Test-source-set only: it must never be referenced from `src/main`.
 */
internal class FakeSoundPlayer : SoundPlayer {
    private val recorded = mutableListOf<PlayedCue>()

    /** How many times [preload] was called — plan 07-02 asserts the App-startup preload. */
    var preloadCalls = 0
        private set

    /** Every playback in call order. */
    val events: List<PlayedCue> get() = recorded.toList()

    /** The cues alone, for the common "what fired, in what order" assertion. */
    val cues: List<SoundCue> get() = recorded.map { it.cue }

    override fun preload() {
        preloadCalls++
    }

    override fun play(cue: SoundCue) {
        recorded += PlayedCue(cue, SoundParams.JUMP_BASE_RATE)
    }

    override fun playJump(runIndex: Int) {
        recorded += PlayedCue(SoundCue.Jump, SoundParams.jumpRate(runIndex))
    }

    override fun playStarPop(step: Int) {
        recorded += PlayedCue(SoundCue.StarPop, SoundParams.starPopRate(step))
    }

    override fun playSoundEnabledConfirmation() {
        recorded += PlayedCue(SoundCue.SoundEnabled, SoundParams.JUMP_BASE_RATE, gateBypassed = true)
    }

    /** Forgets every recorded event and the preload count. */
    fun clear() {
        recorded.clear()
        preloadCalls = 0
    }
}
