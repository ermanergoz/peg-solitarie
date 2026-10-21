package com.app.pegsolitaire.di

import com.app.pegsolitaire.audio.AndroidSystemAudioState
import com.app.pegsolitaire.audio.SoundPlayer
import com.app.pegsolitaire.audio.SoundPoolPlayer
import com.app.pegsolitaire.audio.SystemAudioState
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt bindings for the audio engine. Mirrors [DataModule]'s
 * `@Module @InstallIn(SingletonComponent::class) abstract class` + `@Binds` shell.
 *
 * A single binding, in its own module rather than an addition to [PlatformModule], and
 * that separation is deliberate. `..platform..` is defined — by
 * `PlatformBoundaryRules` and the three ArchUnit rules built on it — as the
 * Play/AdMob/UMP SDK and foreground-Activity seam. `SoundPool` is a framework API, needs
 * no Activity and is not a Slice-2 swap point, so folding it into that package would
 * widen the definition those rules depend on while adding nothing.
 *
 * `@Singleton` appears here **and** on [SoundPoolPlayer]. It is not redundant: this
 * annotation scopes the binding of the [SoundPlayer] *interface*, so an injection of the
 * concrete type would otherwise get a second, unscoped player owning a second `SoundPool`
 * (T-07-05). The class-level annotation is what closes that hole; see the impl's KDoc.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AudioModule {
    @Binds
    @Singleton
    abstract fun bindSoundPlayer(impl: SoundPoolPlayer): SoundPlayer

    /**
     * The ringer and output-route reader behind the Silent/Vibrate speaker gate (Q-010 option 2,
     * `docs/tech/audio-silent-mode.md` §1.5). In `audio/`, not `platform/`, for the reason this
     * module's KDoc gives: `AudioManager` is a framework API with no SDK and no Activity.
     */
    @Binds
    @Singleton
    abstract fun bindSystemAudioState(impl: AndroidSystemAudioState): SystemAudioState
}
