package com.app.pegsolitaire.audio

import android.media.AudioAttributes

/**
 * The one [AudioAttributes] definition game sound plays with: `USAGE_GAME`, which routes the
 * pool to the media stream and so to the system media volume slider (SOUND-06, first half), and
 * `CONTENT_TYPE_SONIFICATION`.
 *
 * It exists as its own object so that the two places reading it can never drift apart:
 * [SoundPoolPlayer] builds its pool from it, and [AndroidSystemAudioState] asks the platform where
 * these same attributes would route (`docs/tech/audio-silent-mode.md` §1.3). A route query made
 * with different attributes would answer for a stream the game never plays.
 */
internal object GameAudioAttributes {
    fun build(): AudioAttributes =
        AudioAttributes
            .Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
}
