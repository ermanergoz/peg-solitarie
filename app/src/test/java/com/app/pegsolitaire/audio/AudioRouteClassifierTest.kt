package com.app.pegsolitaire.audio

import android.media.AudioDeviceInfo
import android.media.AudioManager
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Where game sound would come out, and what the ringer says, decided from plain device-type
 * and ringer ints (`docs/tech/audio-silent-mode.md` §1.2–§1.3).
 *
 * Two paths, one table each:
 * - **API 33+**: the platform says where the pool's own attributes route. Any built-in device
 *   in that list means the phone (a duplicated route counts as the phone); otherwise external.
 * - **API 24–32**, or an empty answer on 33+: the route is inferred from the connected outputs
 *   against an **allowlist** of external media types, so a headset connected for calls only,
 *   screen recording, or a type nobody listed all count as the phone — the direction that keeps
 *   a meeting quiet.
 *
 * Runner-free: the platform constants are compile-time ints, inlined into this test.
 */
class AudioRouteClassifierTest {
    @Test
    fun thePlatformsRoutedDevicesDecideTheRouteOnApi33() {
        val cases =
            listOf(
                Case("routed to the speaker", routed = listOf(SPEAKER), want = OutputRoute.BuiltIn),
                Case("routed to the earpiece", routed = listOf(EARPIECE), want = OutputRoute.BuiltIn),
                Case("routed to wired headphones", routed = listOf(WIRED_HEADPHONES), want = OutputRoute.External),
                Case("routed to Bluetooth A2DP", routed = listOf(A2DP), want = OutputRoute.External),
                Case("routed to a BLE headset", routed = listOf(BLE_HEADSET), want = OutputRoute.External),
                Case("routed to a USB headset", routed = listOf(USB_HEADSET), want = OutputRoute.External),
                Case(
                    "duplicated to the speaker and a wired headset",
                    routed = listOf(SPEAKER, WIRED_HEADSET),
                    want = OutputRoute.BuiltIn,
                ),
                Case(
                    "routed to the speaker while A2DP is connected",
                    routed = listOf(SPEAKER),
                    connected = setOf(SPEAKER, A2DP),
                    want = OutputRoute.BuiltIn,
                ),
            )

        cases.forEach(::assertCase)
    }

    @Test
    fun theConnectedOutputsDecideTheRouteWhenThePlatformGivesNoAnswer() {
        val cases =
            listOf(
                Case("null: speaker and earpiece only", routed = null, connected = setOf(SPEAKER, EARPIECE)),
                Case("empty: speaker and earpiece only", routed = emptyList(), connected = setOf(SPEAKER, EARPIECE)),
                Case(
                    "a wired headset is plugged in",
                    connected = setOf(SPEAKER, WIRED_HEADSET),
                    want = OutputRoute.External,
                ),
                Case("a headset connected for calls only (SCO)", connected = setOf(SPEAKER, BLUETOOTH_SCO)),
                Case("screen recording (remote submix)", connected = setOf(SPEAKER, REMOTE_SUBMIX)),
                Case("a hearing aid", connected = setOf(SPEAKER, HEARING_AID), want = OutputRoute.External),
                Case("a type nobody listed", connected = setOf(SPEAKER, UNKNOWN_TYPE)),
            )

        cases.forEach(::assertCase)
    }

    @Test
    fun ringerModesMapOneToOneAndAnUnknownModeReadsAsNormal() {
        val expected =
            mapOf(
                AudioManager.RINGER_MODE_NORMAL to RingerMode.Normal,
                AudioManager.RINGER_MODE_VIBRATE to RingerMode.Vibrate,
                AudioManager.RINGER_MODE_SILENT to RingerMode.Silent,
                UNKNOWN_RINGER_MODE to RingerMode.Normal,
            )

        expected.forEach { (platform, want) ->
            assertWithMessage("platform ringer mode $platform")
                .that(AudioRouteClassifier.ringerModeOf(platform))
                .isEqualTo(want)
        }
    }

    /** One row: [routed] is the API 33+ answer (null below 33), [connected] every output's type. */
    private data class Case(
        val name: String,
        val routed: List<Int>? = null,
        val connected: Set<Int> = setOf(SPEAKER),
        val want: OutputRoute = OutputRoute.BuiltIn,
    )

    private fun assertCase(case: Case) {
        assertWithMessage(case.name)
            .that(AudioRouteClassifier.classify(routedTypes = case.routed, connectedOutputTypes = case.connected))
            .isEqualTo(case.want)
    }

    private companion object {
        const val SPEAKER = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
        const val EARPIECE = AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
        const val WIRED_HEADSET = AudioDeviceInfo.TYPE_WIRED_HEADSET
        const val WIRED_HEADPHONES = AudioDeviceInfo.TYPE_WIRED_HEADPHONES
        const val A2DP = AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        const val BLUETOOTH_SCO = AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        const val BLE_HEADSET = AudioDeviceInfo.TYPE_BLE_HEADSET
        const val USB_HEADSET = AudioDeviceInfo.TYPE_USB_HEADSET
        const val HEARING_AID = AudioDeviceInfo.TYPE_HEARING_AID
        const val REMOTE_SUBMIX = AudioDeviceInfo.TYPE_REMOTE_SUBMIX

        /** No `AudioDeviceInfo.TYPE_*` constant has this value. */
        const val UNKNOWN_TYPE = 9_999

        /** No `AudioManager.RINGER_MODE_*` constant has this value. */
        const val UNKNOWN_RINGER_MODE = 42
    }
}
