package com.app.pegsolitaire.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.annotation.VisibleForTesting
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.di.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/** Scoped constants for the raw `SoundPool` API values (QUAL-03 — no inline literals). */
private object SoundPoolApi {
    /** `OnLoadCompleteListener` status meaning the sample decoded successfully. */
    const val LOAD_SUCCESS = 0

    /** `SoundPool.play` loop argument: play once, never repeat. */
    const val NO_LOOP = 0

    /** `SoundPool.play` returns this when no stream could be allocated. */
    const val NO_STREAM = 0
}

/**
 * The `SoundPool`-backed [SoundPlayer]: one pool for the whole process, one mute gate, one
 * bounded record of the streams currently in flight.
 *
 * Three design points, each of which pre-empts a plausible "simplification":
 *
 * **`@Singleton` is on the class, not only on the `@Binds`.** `@Binds @Singleton` scopes
 * the binding of the *interface*, so without the class-level annotation any injection of
 * the concrete type would build a second, unscoped instance — and that instance would own
 * a second `SoundPool` holding its own live streams, which the SOUND-05 mute could never
 * reach (T-07-05). This is the same argument
 * [com.app.pegsolitaire.platform.activity.DefaultForegroundActivityProvider] makes for its
 * own scope, and it is not a stylistic slip in either place.
 *
 * **The `soundOn` gate lives here, not in `GameUiState`.** SOUND-05 requires that switching
 * sound off silences one-shots *already playing*, not merely that no new ones start. Only
 * the object holding the stream ids returned by `SoundPool.play` can stop them; a flag in
 * a UI state can gate future calls and nothing else. The gate therefore subscribes to
 * [SettingsRepository.soundOn] directly on the application scope, so it is correct even
 * when no screen is composed.
 *
 * **No audio focus is ever requested.** See the comment at the pool construction site: the
 * absence *is* the implementation of SOUND-06, and it is enforced for good by the
 * `audioFocusLiteralCheck` build gate.
 *
 * Constructor shape mirrors [com.app.pegsolitaire.data.ChallengeProgress], the repo's other
 * `@Singleton` that holds mutable state and subscribes to a DataStore-backed flow on the
 * injected app-lifetime scope.
 */
@Singleton
class SoundPoolPlayer
    @Inject
    constructor(
        // `@param:` is explicit rather than relying on the default use-site target, which
        // Kotlin 2.3 warns is changing (KT-73255). Dagger reads the *parameter* annotation,
        // so pinning it there is both warning-free and the behaviour that must not drift.
        @param:ApplicationContext private val context: Context,
        settingsRepository: SettingsRepository,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) : SoundPlayer {
        // SOUND-06, first half: USAGE_GAME is what routes this pool to STREAM_MUSIC — the
        // system media volume slider — rather than to the notification or alarm stream.
        //
        // SOUND-06, second half, is an omission: no AudioManager is obtained anywhere in this
        // class and no focus request is ever made. Ducking is initiated *by* a focus request,
        // so an app that never holds focus cannot dip the player's podcast. The
        // `audioFocusLiteralCheck` Gradle task (plan 07-01) fails the build if a focus call is
        // ever added under src/main, so the omission cannot be undone by accident.
        //
        // The Builder is mandatory, not stylistic: the deprecated
        // SoundPool(maxStreams, streamType, quality) constructor takes a legacy stream type
        // and has no way to accept AudioAttributes at all.
        @VisibleForTesting
        internal val pool: SoundPool =
            SoundPool
                .Builder()
                .setMaxStreams(SoundParams.MAX_STREAMS)
                .setAudioAttributes(
                    AudioAttributes
                        .Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                ).build()

        /** Cue → the `SoundPool` sound id returned by its load; the presence check makes [preload] idempotent. */
        private val soundIds = ConcurrentHashMap<SoundCue, Int>()

        /** Sound ids whose decode has completed. Playing anything not in here would be silent anyway. */
        private val loaded = ConcurrentHashMap.newKeySet<Int>()

        /**
         * The `soundOn` gate. Fail-open `true` to match [SettingsRepository]'s own defaults
         * (D-17): the first emission of the collected flow arrives asynchronously, and a
         * player that started muted would swallow the cues of a cold-start first move.
         */
        @Volatile
        private var enabled = true

        /**
         * Stream ids currently believed to be in flight, newest last.
         *
         * Bounded to [SoundParams.MAX_STREAMS] and evicting oldest-first, so rapid tapping
         * cannot grow it without limit (T-07-03). `SoundPool` itself voice-steals at the same
         * budget, so an entry evicted here is one the pool has already reclaimed. Guarded by
         * `synchronized` because cues are fired from the main thread while the `soundOn`
         * collector silences from the application scope's dispatcher.
         */
        private val activeStreams = ArrayDeque<Int>(SoundParams.MAX_STREAMS)

        init {
            // Registered before any load() call, so no completion callback can be missed.
            pool.setOnLoadCompleteListener { _, sampleId, status ->
                if (status == SoundPoolApi.LOAD_SUCCESS) {
                    loaded += sampleId
                }
            }
            // `collect`, not `stateIn`: each emission needs a side effect (silencing), not just
            // a readable value.
            scope.launch {
                settingsRepository.soundOn.collect { on ->
                    enabled = on
                    if (!on) silenceNow()
                }
            }
        }

        override fun preload() {
            SoundCue.entries.forEach { cue ->
                if (!soundIds.containsKey(cue)) {
                    soundIds[cue] = pool.load(context, cue.resId, cue.priority)
                }
            }
        }

        override fun play(cue: SoundCue) = play(cue, SoundParams.JUMP_BASE_RATE)

        override fun playJump(runIndex: Int) = play(SoundCue.Jump, SoundParams.jumpRate(runIndex))

        override fun playStarPop(step: Int) = play(SoundCue.StarPop, SoundParams.starPopRate(step))

        /**
         * The one deliberate bypass of the [enabled] gate (D-12).
         *
         * `SettingsViewModel.onToggleSound(true)` writes through DataStore asynchronously, so
         * at the moment the confirmation pop must sound the write has not yet round-tripped
         * back out of the collected `soundOn` flow and [enabled] is still `false` — a gated
         * call would be swallowed entirely (RESEARCH Pitfall 3). Load readiness is still
         * respected; only the gate is skipped.
         */
        override fun playSoundEnabledConfirmation() = playNow(SoundCue.SoundEnabled, SoundParams.JUMP_BASE_RATE)

        /** The gated path every public cue goes through. */
        private fun play(
            cue: SoundCue,
            rate: Float,
        ) {
            if (!enabled) return
            playNow(cue, rate)
        }

        /**
         * Fires [cue] at [rate], skipping the gate. Fails open to silence rather than to a
         * crash: an unknown or still-decoding sample simply does not play, which is the same
         * posture the data layer takes on a bad read.
         */
        private fun playNow(
            cue: SoundCue,
            rate: Float,
        ) {
            val soundId = soundIds[cue] ?: return
            if (soundId !in loaded) return
            val streamId = pool.play(soundId, cue.volume, cue.volume, cue.priority, SoundPoolApi.NO_LOOP, rate)
            if (streamId != SoundPoolApi.NO_STREAM) {
                track(streamId)
            }
        }

        private fun track(streamId: Int) {
            synchronized(activeStreams) {
                if (activeStreams.size >= SoundParams.MAX_STREAMS) {
                    activeStreams.removeFirst()
                }
                activeStreams.addLast(streamId)
            }
        }

        /**
         * Stops everything currently in flight (SOUND-05).
         *
         * `SoundPool` has no `stopAll()`, so each tracked stream id is stopped individually
         * and `autoPause()` is the backstop for anything the tracking missed. Stopping a
         * stream id that has already finished is a documented no-op, so no bookkeeping of
         * completion is needed. The pool is deliberately *not* torn down here: doing so would
         * discard every decoded sample and reintroduce the silent-first-tap failure the
         * startup preload exists to prevent, and the paused streams are never resumed because
         * these are one-shots with nothing to return to.
         */
        private fun silenceNow() {
            synchronized(activeStreams) {
                activeStreams.forEach { pool.stop(it) }
                activeStreams.clear()
            }
            pool.autoPause()
        }

        /** The cue → sound-id map, so a test can assert that every cue was loaded exactly once. */
        @VisibleForTesting
        internal fun loadedSoundIds(): Map<SoundCue, Int> = soundIds.toMap()

        /** The currently tracked in-flight stream ids, oldest first (T-07-03 boundedness). */
        @VisibleForTesting
        internal fun trackedStreamIds(): List<Int> = synchronized(activeStreams) { activeStreams.toList() }
    }
