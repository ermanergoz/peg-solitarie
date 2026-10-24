package com.app.pegsolitaire.audio

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Q-010 option 2 as a truth table: game sound is quiet only when the ringer is Silent or
 * Vibrate **and** the sound would come out of the phone itself; headphones, a Bluetooth speaker
 * or a car keep it (`docs/tech/audio-silent-mode.md` §1.1).
 *
 * Runner-free: the snapshot is plain Kotlin.
 */
class SystemAudioSnapshotTest {
    @Test
    fun normalRingerOnThePhoneSpeakerIsNotMuted() {
        assertThat(SystemAudioSnapshot(RingerMode.Normal, OutputRoute.BuiltIn).mutesGameSound).isFalse()
    }

    @Test
    fun normalRingerOnHeadphonesIsNotMuted() {
        assertThat(SystemAudioSnapshot(RingerMode.Normal, OutputRoute.External).mutesGameSound).isFalse()
    }

    @Test
    fun vibrateOnThePhoneSpeakerIsMuted() {
        assertThat(SystemAudioSnapshot(RingerMode.Vibrate, OutputRoute.BuiltIn).mutesGameSound).isTrue()
    }

    @Test
    fun silentOnThePhoneSpeakerIsMuted() {
        assertThat(SystemAudioSnapshot(RingerMode.Silent, OutputRoute.BuiltIn).mutesGameSound).isTrue()
    }

    @Test
    fun vibrateOnHeadphonesIsNotMuted() {
        assertThat(SystemAudioSnapshot(RingerMode.Vibrate, OutputRoute.External).mutesGameSound).isFalse()
    }

    @Test
    fun silentOnHeadphonesIsNotMuted() {
        assertThat(SystemAudioSnapshot(RingerMode.Silent, OutputRoute.External).mutesGameSound).isFalse()
    }

    @Test
    fun theFailOpenReadingMutesNothing() {
        // §1.9: before the first reading, and after a reader fails, the game behaves as it
        // did before the gate existed.
        assertThat(SystemAudioSnapshot.Unmuted.mutesGameSound).isFalse()
    }
}
