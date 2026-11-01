package com.app.pegsolitaire.data.licenses

import com.app.pegsolitaire.testing.RecordingDispatcher
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * `AssetGoogleNoticeSource` (tech design §13.1 LN-4): the generated index, read from the merged
 * debug assets on the injected IO dispatcher and parsed there, so a ViewModel on the main thread
 * gets the notices without reading or splitting anything itself.
 *
 * The oracle is the raw asset split by hand here, not the parser under test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle virtual-time control
class AssetGoogleNoticeSourceTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun readsAndParsesTheGeneratedIndexOneNoticePerLine() =
        runTest {
            val rawLines =
                context.assets
                    .open(GoogleNoticeAssets.INDEX_PATH)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readLines() }
            val source = AssetGoogleNoticeSource(context, StandardTestDispatcher(testScheduler))

            val index = async { source.index() }
            advanceUntilIdle()
            val notices = requireNotNull(index.await())

            assertThat(notices).hasSize(rawLines.size)
            val firstFields = rawLines.first().split('\t')
            assertThat(notices.first().key).isEqualTo(firstFields[KEY_FIELD])
            assertThat(notices.first().name).isEqualTo(firstFields[NAME_FIELD])
            assertThat(notices.first().stem).isEqualTo(firstFields[STEM_FIELD])
        }

    @Test
    fun readsOnTheInjectedDispatcher() =
        runTest {
            val io = RecordingDispatcher(StandardTestDispatcher(testScheduler))
            val source = AssetGoogleNoticeSource(context, io)

            val index = async(start = CoroutineStart.UNDISPATCHED) { source.index() }

            assertThat(index.isCompleted).isFalse()
            advanceUntilIdle()
            assertThat(index.await()).isNotEmpty()
            assertThat(io.dispatches).isAtLeast(1)
        }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34

private const val KEY_FIELD = 0
private const val NAME_FIELD = 1
private const val STEM_FIELD = 2
