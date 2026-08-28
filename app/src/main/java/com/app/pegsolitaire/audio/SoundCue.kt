package com.app.pegsolitaire.audio

import androidx.annotation.RawRes
import com.app.pegsolitaire.R

/**
 * The eleven sound effects the game can play, each bound to the `res/raw` asset committed
 * in plan 07-01 together with its mix level and voice-stealing priority.
 *
 * The resource ids are **compile-time constants** (`R.raw.…`), never resolved from a name
 * string at runtime through the `Resources` name-lookup API. The release build runs with
 * `isMinifyEnabled` and `isShrinkResources` both on, and a resource reached only by name
 * has no static reference for the shrinker to trace — it would be stripped from the APK
 * and every sound would go silent in release builds only, the worst possible discovery
 * timing.
 * An enum of `@RawRes` constants makes each asset statically reachable and keeps the whole
 * cue set enumerable, which is what lets [SoundPlayer.preload] decode all of them without
 * a hand-maintained second list.
 *
 * Volumes are relative levels chosen in [SoundParams], not per-file mastering: the assets
 * are already peak-balanced (see `docs/audio/CREDITS.md`), and these values place each cue
 * against the others at playback time.
 */
enum class SoundCue(
    @param:RawRes val resId: Int,
    val volume: Float,
    val priority: Int,
) {
    /** The signature pop of a peg completing a jump (SOUND-01). */
    Jump(R.raw.sfx_jump, SoundParams.VOLUME_JUMP, SoundParams.PRIORITY_DEFAULT),

    /** The jumped peg being removed — layered under [Jump], never played alone. */
    PegRemove(R.raw.sfx_peg_remove, SoundParams.VOLUME_REMOVAL, SoundParams.PRIORITY_DEFAULT),

    /** A peg being selected. */
    Select(R.raw.sfx_select, SoundParams.VOLUME_SELECT, SoundParams.PRIORITY_DEFAULT),

    /** A move being taken back. */
    Undo(R.raw.sfx_undo, SoundParams.VOLUME_UNDO, SoundParams.PRIORITY_DEFAULT),

    /** A rejected tap: deliberately the quietest cue (SOUND-02 — quiet, non-punishing). */
    InvalidTap(R.raw.sfx_invalid, SoundParams.VOLUME_INVALID, SoundParams.PRIORITY_DEFAULT),

    /** A hint being revealed. */
    Hint(R.raw.sfx_hint, SoundParams.VOLUME_HINT, SoundParams.PRIORITY_DEFAULT),

    /** The win stinger — first beat of the D-14 celebration, and the loudest cue. */
    Win(R.raw.sfx_win, SoundParams.VOLUME_WIN, SoundParams.PRIORITY_CELEBRATION),

    /** One star landing on the Results screen; pitched up per star by [SoundParams.starPopRate]. */
    StarPop(R.raw.sfx_star_pop, SoundParams.VOLUME_STAR_POP, SoundParams.PRIORITY_CELEBRATION),

    /** A forward/commit button press (D-11). */
    ButtonForward(R.raw.sfx_btn_forward, SoundParams.VOLUME_BUTTON, SoundParams.PRIORITY_DEFAULT),

    /** A dismiss/back button press (D-11) — softer and darker than [ButtonForward]. */
    ButtonDismiss(R.raw.sfx_btn_dismiss, SoundParams.VOLUME_BUTTON, SoundParams.PRIORITY_DEFAULT),

    /** The confirmation pop played when the player switches sound on (D-12). */
    SoundEnabled(R.raw.sfx_sound_enabled, SoundParams.VOLUME_SOUND_ENABLED, SoundParams.PRIORITY_DEFAULT),
}
