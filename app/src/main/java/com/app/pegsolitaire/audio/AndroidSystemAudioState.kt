package com.app.pegsolitaire.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The platform reader behind the speaker gate (Q-010 option 2, `docs/tech/audio-silent-mode.md`
 * §1.2–§1.4, §1.8): the ringer mode from `AudioManager.getRingerMode()`, and the output route.
 *
 * **Event-driven, never polled.** Collection registers a receiver for
 * `RINGER_MODE_CHANGED_ACTION` and `ACTION_AUDIO_BECOMING_NOISY` and an [AudioDeviceCallback],
 * then reads once; each of those events triggers one full re-read. The listeners only `trySend`
 * a trigger on the main looper, and every platform read runs in the collector's coroutine — on
 * the application scope's `Dispatchers.Default` in production — so `SoundPoolPlayer.play()` pays
 * one volatile read and no IPC. Listeners are registered **before** the first read, so a change
 * between the two cannot be lost.
 *
 * **`BECOMING_NOISY` reads as the phone at once.** The platform sends it as wired headphones are
 * pulled or a Bluetooth sink drops, *before* it re-routes to the speaker, so the reading for that
 * trigger forces [OutputRoute.BuiltIn]; the device callback that follows re-reads the real state.
 * If a second headset is still connected, that mutes for a moment and then restores — erring
 * toward quiet.
 *
 * **Reads only.** `AudioManager` is fetched lazily, so the constructor does no IPC and app start
 * costs nothing; no stream volume is changed, no audio focus is requested
 * (`audioFocusLiteralCheck` still proves it), and only each device's type is read — never its
 * address or product name, so no Bluetooth permission is needed. The receiver is not exported:
 * system broadcasts still reach it, and `ContextCompat` drops the flag below API 33.
 *
 * Accepted limitation (O1): a route change with no device change — the output switcher moving
 * media to the phone while headphones stay connected — is seen only at the next ringer or device
 * event. `SoundPool` has no `AudioRouting` listener to see it sooner.
 */
@Singleton
class AndroidSystemAudioState
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) : SystemAudioState {
        private val audioManager: AudioManager by lazy {
            checkNotNull(context.getSystemService(AudioManager::class.java)) { "No AudioManager service" }
        }

        /** The attributes the route query asks about: the pool's own, so the answer is for game sound. */
        private val gameAttributes: AudioAttributes by lazy { GameAudioAttributes.build() }

        override val snapshots: Flow<SystemAudioSnapshot> =
            callbackFlow {
                val receiver =
                    object : BroadcastReceiver() {
                        override fun onReceive(
                            context: Context,
                            intent: Intent,
                        ) {
                            when (intent.action) {
                                AudioManager.RINGER_MODE_CHANGED_ACTION -> trySend(ReadTrigger.RingerChanged)
                                AudioManager.ACTION_AUDIO_BECOMING_NOISY -> trySend(ReadTrigger.BecomingNoisy)
                            }
                        }
                    }
                val deviceCallback =
                    object : AudioDeviceCallback() {
                        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                            trySend(ReadTrigger.DevicesChanged)
                        }

                        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                            trySend(ReadTrigger.DevicesChanged)
                        }
                    }
                val triggers =
                    IntentFilter().apply {
                        addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                        addAction(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                    }
                ContextCompat.registerReceiver(context, receiver, triggers, ContextCompat.RECEIVER_NOT_EXPORTED)
                audioManager.registerAudioDeviceCallback(deviceCallback, Handler(Looper.getMainLooper()))
                send(ReadTrigger.Initial)
                awaitClose {
                    context.unregisterReceiver(receiver)
                    audioManager.unregisterAudioDeviceCallback(deviceCallback)
                }
            }.map(::read)
                .distinctUntilChanged()

        /** One full reading of the ringer and the route, made in the collector's coroutine. */
        private fun read(trigger: ReadTrigger): SystemAudioSnapshot =
            SystemAudioSnapshot(
                ringer = AudioRouteClassifier.ringerModeOf(audioManager.ringerMode),
                route = if (trigger == ReadTrigger.BecomingNoisy) OutputRoute.BuiltIn else currentRoute(),
            )

        private fun currentRoute(): OutputRoute =
            AudioRouteClassifier.classify(
                routedTypes = routedTypes(),
                connectedOutputTypes =
                    audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).mapTo(HashSet()) { it.type },
            )

        /**
         * Where the platform's routing policy would send the pool's streams (API 33+), or `null`
         * below 33, where the route is inferred from the connected outputs instead. The level is
         * checked right here, so lint's `NewApi` sees the guard.
         */
        private fun routedTypes(): List<Int>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                audioManager.getAudioDevicesForAttributes(gameAttributes).map { it.type }
            } else {
                null
            }

        /** Why a reading was taken; only [BecomingNoisy] changes what the reading reports. */
        private enum class ReadTrigger { Initial, RingerChanged, DevicesChanged, BecomingNoisy }
    }
