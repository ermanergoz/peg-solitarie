package com.app.pegsolitaire.data.licenses

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.security.MessageDigest
import kotlin.coroutines.CoroutineContext

/**
 * `AssetLicenseTextSource` (tech design §3.6, §13.1 LN-4; spec LS-AC05): every licence text is read
 * through the real `AssetManager`, whole and as UTF-8, on the injected IO dispatcher and never on
 * the caller's; a path that does not exist is `null`, the screens' error state, never a crash.
 *
 * The expected texts are read from the committed files on disk, so the oracle is independent of the
 * code under test: what the `AssetManager` hands back must be exactly what was committed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle virtual-time control
class AssetLicenseTextSourceTest {
    private val context get() = RuntimeEnvironment.getApplication()

    @Test
    fun readsEveryBundledTextThroughTheAssetManager() =
        runTest {
            val source = AssetLicenseTextSource(context, StandardTestDispatcher(testScheduler))

            BundledLicenseText.entries.forEach { text ->
                val expected = File(findAssetsRoot(), text.assetPath).readText(Charsets.UTF_8)
                assertThat(readNow(source, text)).isEqualTo(expected)
            }
        }

    @Test
    fun returnsNullForAStemThatDoesNotExist() =
        runTest {
            val source = AssetLicenseTextSource(context, StandardTestDispatcher(testScheduler))

            assertThat(readNow(source, GoogleNoticeText(MISSING_STEM))).isNull()
        }

    @Test
    fun readsAGeneratedNoticeAtTheStemTheIndexGives() =
        runTest {
            val stem =
                context.assets
                    .open(GoogleNoticeAssets.INDEX_PATH)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readLine() }
                    .split('\t')[STEM_FIELD]
            val source = AssetLicenseTextSource(context, StandardTestDispatcher(testScheduler))

            val text = readNow(source, GoogleNoticeText(stem))

            assertThat(sha256Hex(requireNotNull(text).toByteArray(Charsets.UTF_8))).startsWith(stem)
        }

    /**
     * The read runs on the dispatcher it was given: it is queued there, finishes only once that
     * dispatcher's scheduler is advanced, and the dispatcher records the dispatch — so a source that
     * ignored the injected dispatcher (a hard-coded `Dispatchers.IO`, or none) fails here.
     */
    @Test
    fun readsOnTheInjectedDispatcher() =
        runTest {
            val io = RecordingDispatcher(StandardTestDispatcher(testScheduler))
            val source = AssetLicenseTextSource(context, io)

            val text =
                async(start = CoroutineStart.UNDISPATCHED) {
                    source.read(BundledLicenseText.PublicSuffixListNotice)
                }

            assertThat(text.isCompleted).isFalse()
            advanceUntilIdle()
            assertThat(text.await()).startsWith(PUBLIC_SUFFIX_NOTICE_OPENING)
            assertThat(io.dispatches).isAtLeast(1)
        }

    /** Starts [source]'s read, lets the virtual clock run it to the end, and returns its result. */
    private suspend fun TestScope.readNow(
        source: AssetLicenseTextSource,
        asset: LicenseAsset,
    ): String? {
        val result = async { source.read(asset) }
        advanceUntilIdle()
        return result.await()
    }

    /** `app/src/main/assets`, found by walking up to the directory that holds `settings.gradle.kts`. */
    private fun findAssetsRoot(): File {
        var candidate: File? = File(".").absoluteFile
        while (candidate != null) {
            if (File(candidate, ROOT_MARKER).isFile) return File(candidate, ASSETS_DIR)
            candidate = candidate.parentFile
        }
        error("Could not locate the repository root from ${File(".").absolutePath}.")
    }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(bytes)
            .joinToString(separator = "") { "%02x".format(it) }

    /** Forwards to [delegate] and counts every dispatch it is asked to make. */
    private class RecordingDispatcher(
        private val delegate: CoroutineDispatcher,
    ) : CoroutineDispatcher() {
        var dispatches = 0
            private set

        override fun isDispatchNeeded(context: CoroutineContext): Boolean = delegate.isDispatchNeeded(context)

        override fun dispatch(
            context: CoroutineContext,
            block: Runnable,
        ) {
            dispatches++
            delegate.dispatch(context, block)
        }
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34

private const val ROOT_MARKER = "settings.gradle.kts"
private const val ASSETS_DIR = "app/src/main/assets"

/** Sixteen hex digits that are no SHA-256 prefix of any text the build generated. */
private const val MISSING_STEM = "0000000000000000"
private const val STEM_FIELD = 2

/** The first words of OkHttp's Public Suffix List NOTICE. */
private const val PUBLIC_SUFFIX_NOTICE_OPENING = "Note that publicsuffixes.gz is compiled from The Public Suffix List"
