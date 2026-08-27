package com.app.pegsolitaire.platform.activity

import android.app.Activity
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.mockk.mockk
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Proves [DefaultForegroundActivityProvider] publishes the resumed Activity and — the
 * part that matters for T-06-14 — releases it again, so a configuration change cannot
 * leak a whole view hierarchy into a process-lifetime singleton.
 *
 * The clear is **identity-checked**: pausing or destroying a *different* Activity must
 * not evict a live one, which is what happens on every A→B transition where B resumes
 * before A pauses. Runner-free — no JUnit runner and no Android runtime — because the
 * lifecycle callbacks are ordinary methods, driven directly with MockK Activity doubles.
 */
class ForegroundActivityProviderTest {
    private val provider = DefaultForegroundActivityProvider()

    @Test
    fun resumePublishesTheActivity() {
        val activity = mockk<Activity>()

        provider.onActivityResumed(activity)

        assertThat(provider.current).isSameInstanceAs(activity)
    }

    @Test
    fun pauseOfTheCurrentActivityClearsIt() {
        val activity = mockk<Activity>()
        provider.onActivityResumed(activity)

        provider.onActivityPaused(activity)

        assertThat(provider.current).isNull()
    }

    @Test
    fun pauseOfADifferentActivityDoesNotClearTheCurrentOne() {
        val current = mockk<Activity>()
        val other = mockk<Activity>()
        provider.onActivityResumed(current)

        provider.onActivityPaused(other)

        assertThat(provider.current).isSameInstanceAs(current)
    }

    @Test
    fun destroyOfTheCurrentActivityClearsIt() {
        val activity = mockk<Activity>()
        provider.onActivityResumed(activity)

        provider.onActivityDestroyed(activity)

        assertThat(provider.current).isNull()
    }

    @Test
    fun awaitActivitySuspendsUntilOneResumes() =
        runTest {
            val activity = mockk<Activity>()

            flow { emit(provider.awaitActivity()) }.test {
                expectNoEvents()

                provider.onActivityResumed(activity)

                assertThat(awaitItem()).isSameInstanceAs(activity)
                awaitComplete()
            }
        }
}
