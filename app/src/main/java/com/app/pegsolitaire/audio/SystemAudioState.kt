package com.app.pegsolitaire.audio

import kotlinx.coroutines.flow.Flow

/**
 * The phone's ringer mode as the speaker gate reads it (`docs/tech/audio-silent-mode.md` §1.2).
 * Any platform value other than vibrate or silent reads as [Normal] (fail-open).
 */
enum class RingerMode { Normal, Vibrate, Silent }

/**
 * Where game sound would come out (§1.3): [BuiltIn] is the phone itself — its speaker or
 * earpiece — and [External] is anything else that carries media: wired, USB or Bluetooth
 * headphones, a Bluetooth speaker, a car.
 */
enum class OutputRoute { BuiltIn, External }

/**
 * One reading of the two system facts the speaker gate needs.
 *
 * Plain Kotlin on purpose, with no `android.*` import: the gate's whole decision is
 * [mutesGameSound], and a JVM test can pin it row by row.
 */
data class SystemAudioSnapshot(
    val ringer: RingerMode,
    val route: OutputRoute,
) {
    /** Q-010 option 2: quiet only on Silent/Vibrate AND when the sound would leave the phone itself. */
    val mutesGameSound: Boolean
        get() = ringer != RingerMode.Normal && route == OutputRoute.BuiltIn

    companion object {
        /** The fail-open reading: today's behaviour, used before the first reading and after a failure. */
        val Unmuted = SystemAudioSnapshot(RingerMode.Normal, OutputRoute.BuiltIn)
    }
}

/**
 * The read-only seam through which [SoundPoolPlayer] learns the ringer mode and the output
 * route (Q-010 option 2, `docs/tech/audio-silent-mode.md` §1.5).
 *
 * It lives in `audio/`, not `platform/`: that package is the Play/AdMob/UMP and
 * foreground-Activity seam the `PlatformBoundaryRules` ArchUnit rules define, and `AudioManager`
 * is a framework API with no SDK and no Activity. The gate it feeds sits inside the player, never
 * in a `UiState`, for the reason [SoundPlayer] gives: only the owner of the stream ids can stop
 * what is already playing.
 */
interface SystemAudioState {
    /**
     * One reading on collection, then one after every ringer or output change, de-duplicated.
     * Cold: each collector registers its own listeners and unregisters them on cancellation.
     * Never completes.
     */
    val snapshots: Flow<SystemAudioSnapshot>
}
