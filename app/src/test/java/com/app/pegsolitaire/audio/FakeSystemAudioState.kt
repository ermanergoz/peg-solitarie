package com.app.pegsolitaire.audio

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * A [SystemAudioState] a test can drive: set [snapshot] and every collector sees the new
 * reading, as a ringer broadcast or a device callback would deliver it.
 *
 * It starts at [SystemAudioSnapshot.Unmuted], the reading a normal-ringer phone gives on its own
 * speaker. Test-source-set only.
 */
internal class FakeSystemAudioState(
    initial: SystemAudioSnapshot = SystemAudioSnapshot.Unmuted,
) : SystemAudioState {
    val snapshot = MutableStateFlow(initial)

    override val snapshots = snapshot
}
