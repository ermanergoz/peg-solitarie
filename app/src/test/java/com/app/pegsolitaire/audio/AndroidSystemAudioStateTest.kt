package com.app.pegsolitaire.audio

import android.app.Application
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Looper
import app.cash.turbine.test
import com.google.common.collect.ImmutableList
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.AudioDeviceInfoBuilder
import org.robolectric.shadows.ShadowAudioManager
import org.robolectric.util.ReflectionHelpers

/**
 * The platform reader behind the speaker gate (`docs/tech/audio-silent-mode.md` §1.2–§1.4):
 * it reads the ringer and the route once on collection, re-reads on the ringer broadcast, on
 * `ACTION_AUDIO_BECOMING_NOISY` and on an output-device change, and nothing else — no polling.
 *
 * Robolectric SDK 34, the only `android-all` image cached offline (§1.10). The shadow's route
 * query answers with an empty list unless a test maps the game's attributes, so the tests with
 * no mapping also exercise the connected-device inference. The pre-33 branch is one `if`; the
 * classifier tests and device check D9 cover it.
 *
 * It runs on a plain [Application] rather than the app's Hilt `App`, whose own sound player
 * would otherwise register a second receiver and device callback against the same shadows.
 */
@OptIn(ExperimentalCoroutinesApi::class) // runCurrent + UnconfinedTestDispatcher for the cancellation test
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [SYSTEM_AUDIO_ROBOLECTRIC_SDK], application = Application::class)
class AndroidSystemAudioStateTest {
    private val context: Application = RuntimeEnvironment.getApplication()

    private val audioManager: AudioManager = context.getSystemService(AudioManager::class.java)

    private val shadowAudio: ShadowAudioManager = shadowOf(audioManager)

    private val reader = AndroidSystemAudioState(context)

    @Test
    fun theFirstReadingReportsTheCurrentRingerAndRoute() =
        runTest {
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            shadowAudio.addOutputDevice(device(AudioDeviceInfo.TYPE_WIRED_HEADPHONES), false)

            reader.snapshots.test {
                assertThat(awaitItem()).isEqualTo(SystemAudioSnapshot(RingerMode.Vibrate, OutputRoute.External))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun aRingerModeBroadcastProducesANewReading() =
        runTest {
            reader.snapshots.test {
                assertThat(awaitItem()).isEqualTo(SystemAudioSnapshot(RingerMode.Normal, OutputRoute.BuiltIn))

                // The mode changes, but nothing is re-read until the platform says so: no polling.
                audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                expectNoEvents()

                context.sendBroadcast(Intent(AudioManager.RINGER_MODE_CHANGED_ACTION))
                shadowOf(Looper.getMainLooper()).idle()

                assertThat(awaitItem()).isEqualTo(SystemAudioSnapshot(RingerMode.Silent, OutputRoute.BuiltIn))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun anAddedWiredHeadsetMovesTheRouteOffThePhone() =
        runTest {
            reader.snapshots.test {
                assertThat(awaitItem().route).isEqualTo(OutputRoute.BuiltIn)

                shadowAudio.addOutputDevice(device(AudioDeviceInfo.TYPE_WIRED_HEADSET), true)

                assertThat(awaitItem().route).isEqualTo(OutputRoute.External)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun thePlatformRouteWinsOverConnectedDevicesOnApi33() =
        runTest {
            // A2DP is connected, but media has been moved back to the phone: presence alone
            // would say "external", and the platform's answer for the pool's attributes says not.
            shadowAudio.addOutputDevice(device(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP), false)
            shadowAudio.setAudioDevicesForAttributes(
                GameAudioAttributes.build(),
                ImmutableList.of(device(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)),
            )

            reader.snapshots.test {
                assertThat(awaitItem().route).isEqualTo(OutputRoute.BuiltIn)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun theRouteQueryAsksAboutTheAttributesThePoolPlaysWith() =
        runTest {
            // Only the game's own attributes are mapped. A query made with any other attributes
            // gets the empty answer, falls through to the speaker-only connected set, and reads
            // as the phone.
            shadowAudio.setAudioDevicesForAttributes(
                GameAudioAttributes.build(),
                ImmutableList.of(device(AudioDeviceInfo.TYPE_WIRED_HEADPHONES)),
            )

            reader.snapshots.test {
                assertThat(awaitItem().route).isEqualTo(OutputRoute.External)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun becomingNoisyReportsThePhoneAtOnce() =
        runTest {
            audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            shadowAudio.addOutputDevice(device(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP), false)

            reader.snapshots.test {
                assertThat(awaitItem()).isEqualTo(SystemAudioSnapshot(RingerMode.Vibrate, OutputRoute.External))

                // The platform sends this as headphones are pulled, BEFORE it re-routes; the
                // device is still listed, and the reading must not wait for it to go.
                context.sendBroadcast(Intent(AudioManager.ACTION_AUDIO_BECOMING_NOISY))
                shadowOf(Looper.getMainLooper()).idle()

                assertThat(awaitItem()).isEqualTo(SystemAudioSnapshot(RingerMode.Vibrate, OutputRoute.BuiltIn))
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun cancellingTheCollectionUnregistersBothListeners() =
        runTest {
            val collection =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { reader.snapshots.collect {} }
            runCurrent()
            assertThat(hasReceiverFor(AudioManager.RINGER_MODE_CHANGED_ACTION)).isTrue()
            assertThat(hasReceiverFor(AudioManager.ACTION_AUDIO_BECOMING_NOISY)).isTrue()
            assertThat(registeredDeviceCallbacks()).hasSize(1)

            collection.cancel()
            runCurrent()

            assertThat(hasReceiverFor(AudioManager.RINGER_MODE_CHANGED_ACTION)).isFalse()
            assertThat(hasReceiverFor(AudioManager.ACTION_AUDIO_BECOMING_NOISY)).isFalse()
            assertThat(registeredDeviceCallbacks()).isEmpty()
        }

    private fun device(type: Int): AudioDeviceInfo = AudioDeviceInfoBuilder.newBuilder().setType(type).build()

    private fun hasReceiverFor(action: String): Boolean =
        shadowOf(context).registeredReceivers.any { it.intentFilter.hasAction(action) }

    /**
     * The shadow keeps its device callbacks in a private set and offers no accessor, so this
     * reads the field — the same `ReflectionHelpers` idiom `SoundPoolPlayerTest` uses for the
     * pool's attributes. Removing a device notifies nobody once a callback is gone, so there is
     * no behavioural signal to read instead.
     */
    private fun registeredDeviceCallbacks(): Set<*> = ReflectionHelpers.getField(shadowAudio, "audioDeviceCallbacks")
}

/** The only `android-all` image cached offline (`docs/tech/audio-silent-mode.md` §1.10). */
private const val SYSTEM_AUDIO_ROBOLECTRIC_SDK = 34
