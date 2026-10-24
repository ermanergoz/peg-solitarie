package com.app.pegsolitaire.audio

import android.media.AudioDeviceInfo
import android.media.AudioManager

/**
 * The speaker gate's two pure decisions (`docs/tech/audio-silent-mode.md` §1.2–§1.3): where
 * game sound would come out, and what the ringer says. Inputs are the platform's plain ints, so
 * a JVM test drives every row without a device.
 *
 * It reads device **types** only — never an address or a product name — so it needs no
 * Bluetooth permission on API 31+.
 *
 * Lint reports the API 26, 28, 30 and 31 type constants below as `InlinedApi` warnings. They are
 * compile-time ints, inlined and harmless on older devices: a device on API 24 cannot report a
 * type that was added later, so the sets simply never match there (design O8; no suppression).
 */
internal object AudioRouteClassifier {
    /**
     * Where game sound would come out.
     *
     * @param routedTypes where the platform routes the pool's own attributes (API 33+), or
     *   `null` below 33. Any built-in device among them means the phone — a duplicated route
     *   counts as the phone — and otherwise the route is external. An empty list carries no
     *   answer and falls through to [connectedOutputTypes].
     * @param connectedOutputTypes every connected output's type (all levels). The route is
     *   external if **any** of them is an external media output in the allowlist, and the phone
     *   otherwise. An allowlist, not a denylist: an unrecognised type counts as the phone, the
     *   direction that keeps a meeting quiet.
     */
    fun classify(
        routedTypes: List<Int>?,
        connectedOutputTypes: Set<Int>,
    ): OutputRoute =
        when {
            !routedTypes.isNullOrEmpty() ->
                if (routedTypes.any { it in DeviceTypes.BUILT_IN }) OutputRoute.BuiltIn else OutputRoute.External
            connectedOutputTypes.any { it in DeviceTypes.EXTERNAL_MEDIA } -> OutputRoute.External
            else -> OutputRoute.BuiltIn
        }

    /** `AudioManager.RINGER_MODE_*` → [RingerMode]; any other value reads as [RingerMode.Normal]. */
    fun ringerModeOf(platformRingerMode: Int): RingerMode =
        when (platformRingerMode) {
            AudioManager.RINGER_MODE_VIBRATE -> RingerMode.Vibrate
            AudioManager.RINGER_MODE_SILENT -> RingerMode.Silent
            else -> RingerMode.Normal
        }

    /** The two device-type sets the route decision reads (QUAL-03 scoped constants). */
    private object DeviceTypes {
        /** The phone itself: "the phone" in Q-010's answer. */
        val BUILT_IN: Set<Int> =
            setOf(
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER,
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE,
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER_SAFE,
            )

        /**
         * Outputs that carry media away from the phone. Deliberately absent: Bluetooth SCO (a
         * headset connected for calls only), telephony, remote submix (screen recording and cast
         * capture), FM, tuners, IP and bus outputs.
         */
        val EXTERNAL_MEDIA: Set<Int> =
            setOf(
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_LINE_ANALOG,
                AudioDeviceInfo.TYPE_LINE_DIGITAL,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_HDMI,
                AudioDeviceInfo.TYPE_HDMI_ARC,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_ACCESSORY,
                AudioDeviceInfo.TYPE_DOCK,
                AudioDeviceInfo.TYPE_AUX_LINE,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_HEARING_AID,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                AudioDeviceInfo.TYPE_BLE_SPEAKER,
            )
    }
}
