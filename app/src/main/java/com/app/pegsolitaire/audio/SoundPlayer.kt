package com.app.pegsolitaire.audio

/**
 * The seam every sound cue in the app is fired through (SOUND-01..06). Fulfilled by the
 * application-scoped [SoundPoolPlayer]; [NoOpSoundPlayer] is the silent fallback used by
 * `@Preview`s and anything composed outside `MainActivity`.
 *
 * Modelled on [com.app.pegsolitaire.platform.ads.AdGateway]'s interface + `@Inject`
 * constructor impl + `@Binds` shape, but three things about it are deliberate and should
 * not be "tidied" back:
 *
 * 1. **This is not a Slice-2 swap point.** It is bound in its own `AudioModule`, not in
 *    `PlatformModule`. `..platform..` is defined as the Play/AdMob SDK and
 *    foreground-Activity seam, and three ArchUnit rules in `PlatformBoundaryRules` depend
 *    on that definition; widening it to mean "anything with a system dependency" would
 *    make those rules mean less. `SoundPool` is a framework API, not a third-party SDK,
 *    and there is no second implementation coming.
 * 2. **The `soundOn` gate lives in the implementation, not in any `UiState`.** Callers
 *    never pass a `soundOn` flag and never check one. SOUND-05 requires that switching
 *    sound off silences one-shots *already in flight*, and only the object holding the
 *    live stream ids can do that — a UI-state flag can only ever fail to reach them.
 * 3. **No method here is a coroutine.** Playback is fire-and-forget: a cue is a side
 *    effect of something that already happened, there is no result to await, and an
 *    awaitable signature would invite callers to sequence gameplay on audio completion —
 *    unlike `AdGateway.requestRewarded`, which really does await an SDK round trip.
 */
interface SoundPlayer {
    /**
     * Decodes every [SoundCue] sample. Idempotent — calling it twice does not re-decode.
     *
     * Called once from `App.onCreate`. Decoding is asynchronous inside `SoundPool`, so
     * this returning does **not** mean samples are ready; it means the decode has been
     * requested early enough that SOUND-01's "the first tap of a session is audible"
     * holds by the time a board is on screen.
     */
    fun preload()

    /**
     * Plays [cue] at its natural pitch. Fire-and-forget: a no-op when sound is off, and a
     * no-op (silence, never a crash) when the sample has not finished decoding yet.
     */
    fun play(cue: SoundCue)

    /**
     * Plays [SoundCue.Jump] one semitone higher per rung of a same-peg run
     * ([SoundParams.jumpRate]), where [runIndex] is 0 for the first jump of a run.
     */
    fun playJump(runIndex: Int)

    /**
     * Plays [SoundCue.StarPop] for the [step]-th star of the Results celebration, pitched
     * by [SoundParams.starPopRate] so the stars outline a rising triad (D-15).
     */
    fun playStarPop(step: Int)

    /**
     * Plays [SoundCue.SoundEnabled] **ignoring the `soundOn` gate** — the single
     * documented bypass, and the only one that will ever be added.
     *
     * It exists for D-12: switching sound on in Settings answers with one confirmation
     * pop. The gate cannot be used here because the write has not yet round-tripped
     * through DataStore and back out of the collected `soundOn` flow when the pop must
     * sound, so a gated call would simply be swallowed. Call it only from the Settings
     * enable event — never from a flow collector, which would also fire it every time the
     * Settings screen opens, exactly what D-12 forbids.
     */
    fun playSoundEnabledConfirmation()
}
