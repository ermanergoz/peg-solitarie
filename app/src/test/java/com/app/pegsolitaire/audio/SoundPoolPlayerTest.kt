package com.app.pegsolitaire.audio

import android.media.AudioAttributes
import android.media.SoundPool
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.SettingsRepository
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowSoundPool
import org.robolectric.util.ReflectionHelpers

/** Robolectric has no android-all jar for SDK 36, so the runtime SDK is pinned like the rest of the suite. */
private const val ROBOLECTRIC_SDK = 34

/** Float comparison tolerance for playback rates. */
private const val RATE_TOLERANCE = 1e-4f

/** Enough taps to overrun the stream budget several times over (T-07-03). */
private const val RAPID_PLAYS = 100

/** A three-jump same-peg run — the longest run in the repo's committed golden solutions. */
private const val RUN_LENGTH = 3

/**
 * Specification for [SoundPoolPlayer]: the media-volume attributes, the decode-before-play
 * guard, the pitch ladder reaching the pool, the SOUND-05 mute and the D-12 bypass.
 *
 * Runs JVM-side under Robolectric because `SoundPool` is a framework class; the shadow
 * records every `play` with its rate, so the ladder is asserted rather than eyeballed.
 * [RecordingSoundPoolShadow] extends the stock shadow to also record `stop`/`autoPause`,
 * which the stock one does not — without it "mute silences streams already in flight"
 * could only be asserted as "mute clears our own bookkeeping", which is not the claim.
 *
 * Note the limit of that: under Robolectric `SoundPool.stop` is a no-op'd native method,
 * so no JVM test can prove the *platform's* behaviour when stopping an already-finished
 * stream. That is a documented platform contract (RESEARCH assumption A1); what
 * [silencingTwiceIsSafe] proves is that this class's own path is re-entrant.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK], shadows = [RecordingSoundPoolShadow::class])
class SoundPoolPlayerTest {
    private val settings = MutableSoundSettingsRepository()

    private val player =
        SoundPoolPlayer(
            context = RuntimeEnvironment.getApplication(),
            settingsRepository = settings,
            scope = CoroutineScope(Dispatchers.Unconfined),
        )

    private val recording: RecordingSoundPoolShadow get() = Shadow.extract(player.pool)

    @Test
    fun theWholePoolIsBuiltForTheMediaVolumeSlider() {
        val attributes = ReflectionHelpers.getField<AudioAttributes>(player.pool, "mAttributes")

        assertThat(attributes.usage).isEqualTo(AudioAttributes.USAGE_GAME)
        assertThat(attributes.contentType).isEqualTo(AudioAttributes.CONTENT_TYPE_SONIFICATION)
    }

    @Test
    fun preloadRequestsOneDecodePerCue() {
        player.preload()

        val ids = player.loadedSoundIds()

        assertThat(ids.keys).containsExactlyElementsIn(SoundCue.entries)
        assertThat(ids.values).containsNoDuplicates()
    }

    @Test
    fun preloadIsIdempotent() {
        player.preload()
        val first = player.loadedSoundIds()

        player.preload()

        assertThat(player.loadedSoundIds()).isEqualTo(first)
    }

    @Test
    fun aCuePlayedBeforeItsDecodeCompletesIsSilent() {
        player.preload()

        player.play(SoundCue.Select)

        assertThat(shadowOf(player.pool).getResourcePlaybacks(R.raw.sfx_select)).isEmpty()
    }

    @Test
    fun aCuePlayedAfterItsDecodeCompletesIsAudible() {
        preloadAndDecode(SoundCue.Select)

        player.play(SoundCue.Select)

        assertThat(shadowOf(player.pool).wasResourcePlayed(R.raw.sfx_select)).isTrue()
    }

    @Test
    fun aSamePegRunClimbsOneSemitonePerJump() {
        preloadAndDecode(SoundCue.Jump)

        repeat(RUN_LENGTH) { player.playJump(runIndex = it) }

        val rates = shadowOf(player.pool).getResourcePlaybacks(R.raw.sfx_jump).map { it.rate }
        assertThat(rates).hasSize(RUN_LENGTH)
        assertThat(rates.first()).isWithin(RATE_TOLERANCE).of(SoundParams.JUMP_BASE_RATE)
        rates.forEachIndexed { index, rate ->
            assertThat(rate).isWithin(RATE_TOLERANCE).of(SoundParams.jumpRate(index))
        }
        assertThat(rates).isInStrictOrder()
    }

    @Test
    fun withSoundOffNothingPlaysAtAll() {
        preloadAndDecode(SoundCue.Jump, SoundCue.Select)
        settings.soundOn.value = false

        player.play(SoundCue.Select)
        player.playJump(runIndex = 0)

        assertThat(shadowOf(player.pool).getResourcePlaybacks(R.raw.sfx_select)).isEmpty()
        assertThat(shadowOf(player.pool).getResourcePlaybacks(R.raw.sfx_jump)).isEmpty()
    }

    @Test
    fun switchingSoundOffStopsTheStreamsAlreadyInFlight() {
        preloadAndDecode(SoundCue.Jump)
        repeat(RUN_LENGTH) { player.playJump(runIndex = it) }
        val inFlight = player.trackedStreamIds()

        settings.soundOn.value = false

        assertThat(inFlight).isNotEmpty()
        assertThat(recording.stoppedStreams).containsExactlyElementsIn(inFlight)
        assertThat(recording.autoPauseCalls).isAtLeast(1)
        assertThat(player.trackedStreamIds()).isEmpty()
    }

    @Test
    fun silencingTwiceIsSafe() {
        preloadAndDecode(SoundCue.Jump)
        player.playJump(runIndex = 0)

        settings.soundOn.value = false
        settings.soundOn.value = true
        settings.soundOn.value = false

        assertThat(player.trackedStreamIds()).isEmpty()
    }

    @Test
    fun theEnableConfirmationPopBypassesTheGate() {
        preloadAndDecode(SoundCue.SoundEnabled)
        settings.soundOn.value = false

        player.playSoundEnabledConfirmation()

        assertThat(shadowOf(player.pool).wasResourcePlayed(R.raw.sfx_sound_enabled)).isTrue()
    }

    @Test
    fun rapidTappingCannotGrowTheTrackedStreamBuffer() {
        preloadAndDecode(SoundCue.Jump)

        repeat(RAPID_PLAYS) { player.playJump(runIndex = 0) }

        assertThat(player.trackedStreamIds()).hasSize(SoundParams.MAX_STREAMS)
    }

    /** Loads every cue, then completes the decode of just [cues] so play-readiness is deterministic. */
    private fun preloadAndDecode(vararg cues: SoundCue) {
        player.preload()
        cues.forEach { shadowOf(player.pool).notifyResourceLoaded(it.resId, true) }
        shadowOf(player.pool).clearPlayed()
    }
}

/**
 * A [SettingsRepository] whose `soundOn` can be flipped mid-test.
 *
 * Only [soundOn] matters here; the other two flags are fixed at their fail-open defaults.
 */
private class MutableSoundSettingsRepository : SettingsRepository {
    override val soundOn = MutableStateFlow(true)

    override val hintsEnabled: Flow<Boolean> = MutableStateFlow(true)

    override val hapticsOn: Flow<Boolean> = MutableStateFlow(true)

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) {
        soundOn.value = on
    }

    override suspend fun setHapticsOn(on: Boolean) = Unit
}

/**
 * The stock [ShadowSoundPool] records `load` and `play` but not `stop`/`autoPause`, so it
 * cannot answer the one question SOUND-05 actually asks. This subclass adds exactly those
 * two recordings and changes nothing else.
 */
@Implements(SoundPool::class)
class RecordingSoundPoolShadow : ShadowSoundPool() {
    /** Stream ids passed to `SoundPool.stop`, in call order. */
    val stoppedStreams = mutableListOf<Int>()

    /** How many times the `autoPause()` backstop was invoked. */
    var autoPauseCalls = 0

    @Implementation
    fun stop(streamId: Int) {
        stoppedStreams += streamId
    }

    @Implementation
    fun autoPause() {
        autoPauseCalls++
    }
}
