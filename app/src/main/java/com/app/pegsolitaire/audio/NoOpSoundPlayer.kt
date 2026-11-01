package com.app.pegsolitaire.audio

import javax.inject.Inject

/**
 * The silent [SoundPlayer]: every call is a no-op.
 *
 * It is the default value of [LocalSoundPlayer] rather than an `error("not provided")`,
 * so a `@Preview`, a screenshot test or any composable rendered outside `MainActivity`
 * renders **silently instead of crashing**. That is the same fail-open posture the rest of
 * the app already takes with player-facing state (see
 * [com.app.pegsolitaire.data.SettingsRepository]'s defaults, D-17): a missing helper
 * degrades to "no help", never to a broken screen.
 *
 * Mirrors [com.app.pegsolitaire.platform.ads.NoOpAdGateway]'s `@Inject constructor` shape
 * so it can also be bound by Hilt if a build variant ever wants a mute app; it is not
 * bound today.
 */
class NoOpSoundPlayer
    @Inject
    constructor() : SoundPlayer {
        override fun preload() = Unit

        override fun play(cue: SoundCue) = Unit

        override fun playJump(runIndex: Int) = Unit

        override fun playStarPop(step: Int) = Unit

        override fun playSoundEnabledConfirmation() = Unit
    }
