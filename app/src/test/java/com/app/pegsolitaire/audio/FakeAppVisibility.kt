package com.app.pegsolitaire.audio

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * An [AppVisibility] a test can drive: set [foreground] as the process would move between
 * STARTED and CREATED. It starts in the foreground, where the game is when a cue fires.
 * Test-source-set only.
 */
internal class FakeAppVisibility(
    initial: Boolean = true,
) : AppVisibility {
    val foreground = MutableStateFlow(initial)

    override val inForeground = foreground
}
