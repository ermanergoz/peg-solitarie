package com.app.pegsolitaire.audio

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * P8's notion of "the background" (`docs/tech/audio-silent-mode.md` §2.1): the **process**
 * lifecycle, at least STARTED while any Activity of the app is visible, and only CREATED once the
 * last one has stopped. A same-process interstitial does not stop the process (D-16), so it
 * is not a background here either.
 *
 * Driven on a [LifecycleRegistry] built with `createUnsafe`, so the test owns every transition and
 * needs no main thread.
 */
class ProcessAppVisibilityTest {
    @Test
    fun startedIsForegroundAndCreatedIsBackground() =
        runTest {
            val registry = LifecycleRegistry.createUnsafe(TestOwner)
            val visibility = ProcessAppVisibility(registry)

            visibility.inForeground.test {
                // At process start the lifecycle is not yet STARTED: no Activity is visible.
                assertThat(awaitItem()).isFalse()

                registry.currentState = Lifecycle.State.STARTED
                assertThat(awaitItem()).isTrue()

                // Resuming changes nothing the gate cares about, so nothing is emitted.
                registry.currentState = Lifecycle.State.RESUMED
                expectNoEvents()

                // ON_STOP of the whole process: back to CREATED.
                registry.currentState = Lifecycle.State.CREATED
                assertThat(awaitItem()).isFalse()
                cancelAndIgnoreRemainingEvents()
            }
        }

    /** The owner the registry reports for; the visibility reader never asks it for anything. */
    private object TestOwner : LifecycleOwner {
        override val lifecycle: Lifecycle get() = error("the registry under test is passed directly")
    }
}
