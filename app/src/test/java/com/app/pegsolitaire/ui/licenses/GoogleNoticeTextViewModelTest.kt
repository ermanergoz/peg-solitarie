package com.app.pegsolitaire.ui.licenses

import androidx.lifecycle.SavedStateHandle
import com.app.pegsolitaire.data.licenses.GoogleNoticeText
import com.app.pegsolitaire.testing.RecordingDispatcher
import com.app.pegsolitaire.ui.licenses.LicenseTextHeader.Notice
import com.app.pegsolitaire.ui.licenses.LicenseTextUiState.Failed
import com.app.pegsolitaire.ui.licenses.LicenseTextUiState.Loaded
import com.app.pegsolitaire.ui.licenses.LicenseTextUiState.Loading
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * `GoogleNoticeTextViewModel` (spec §4B; tech design §13.1 LN-4; brief §3.4, A1.5) — the lazy
 * reader for Cronet's 785 KB notice. The route carries a key and nothing else: the key is resolved
 * against the index the APK carries, and only then is the notice's stem read and re-flowed off the
 * main thread. An unknown key is Failed with no header and no text read (LS-AC14).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle virtual-time control
class GoogleNoticeTextViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun resolvesTheKeyThenReadsTheStem() =
        runTest(testDispatcher) {
            val texts = FakeLicenseTextSource(mapOf(GoogleNoticeText(STEM_B) to "OkHttp notice\nwrapped.\n\nSecond.\n"))
            val viewModel = viewModel(KEY_B, texts = texts)

            advanceUntilIdle()

            val part = LoadedTextPart(heading = null, paragraphs = listOf("OkHttp notice wrapped.", "Second."))
            assertThat(
                viewModel.uiState.value,
            ).isEqualTo(Loaded(Notice(OKHTTP, "ads-mobile-sdk", ordinal = null), listOf(part)))
            assertThat(texts.reads).containsExactly(GoogleNoticeText(STEM_B))
        }

    @Test
    fun theHeaderCarriesTheJoinedSourcesAndTheOrdinalOfARepeatedName() =
        runTest(testDispatcher) {
            val texts = FakeLicenseTextSource(mapOf(GoogleNoticeText(STEM_C) to "Second Kotlin text.\n"))
            val viewModel = viewModel(KEY_C, texts = texts)

            advanceUntilIdle()

            assertThat(viewModel.uiState.value.header)
                .isEqualTo(Notice(KOTLIN_TWO, "hsdp, user-messaging-platform", NoticeOrdinal(index = 2, count = 2)))
        }

    @Test
    fun anUnknownKeyIsFailedWithNoHeaderAndReadsNoText() =
        runTest(testDispatcher) {
            val texts = FakeLicenseTextSource()
            val viewModel = viewModel("missing", texts = texts)

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(Failed(header = null))
            assertThat(texts.reads).isEmpty()
        }

    @Test
    fun aNullIndexIsFailedWithNoHeaderAndReadsNoText() =
        runTest(testDispatcher) {
            val texts = FakeLicenseTextSource()
            val viewModel = viewModel(KEY_B, notices = FakeGoogleNoticeSource(null), texts = texts)

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(Failed(header = null))
            assertThat(texts.reads).isEmpty()
        }

    @Test
    fun aNullTextIsFailedWithTheHeader() =
        runTest(testDispatcher) {
            val viewModel = viewModel(KEY_B, texts = FakeLicenseTextSource())

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(Failed(Notice(OKHTTP, "ads-mobile-sdk", ordinal = null)))
        }

    @Test
    fun nothingRunsBeforeTheDispatcherAdvances() =
        runTest(testDispatcher) {
            val notices = FakeGoogleNoticeSource(INDEX)
            val texts = FakeLicenseTextSource(mapOf(GoogleNoticeText(STEM_B) to "Text.\n"))
            val viewModel = viewModel(KEY_B, notices = notices, texts = texts)

            assertThat(viewModel.uiState.value).isEqualTo(Loading(header = null))
            assertThat(notices.reads).isEqualTo(0)
            assertThat(texts.reads).isEmpty()

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isInstanceOf(Loaded::class.java)
        }

    @Test
    fun theReflowRunsOnTheInjectedDispatcher() =
        runTest(testDispatcher) {
            val reflow = RecordingDispatcher(StandardTestDispatcher(testScheduler))
            val texts = FakeLicenseTextSource(mapOf(GoogleNoticeText(STEM_B) to "Text.\n"))
            val viewModel = viewModel(KEY_B, texts = texts, defaultDispatcher = reflow)

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isInstanceOf(Loaded::class.java)
            assertThat(reflow.dispatches).isAtLeast(1)
        }

    private fun viewModel(
        key: String,
        notices: FakeGoogleNoticeSource = FakeGoogleNoticeSource(INDEX),
        texts: FakeLicenseTextSource,
        defaultDispatcher: CoroutineDispatcher = testDispatcher,
    ): GoogleNoticeTextViewModel =
        GoogleNoticeTextViewModel(
            savedStateHandle = SavedStateHandle(mapOf(KEY_ARG to key)),
            noticeSource = notices,
            textSource = texts,
            defaultDispatcher = defaultDispatcher,
        )
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34

/** The `GoogleNoticeText` route's one argument, as `toRoute` reads it back. */
private const val KEY_ARG = "key"

private const val KEY_A = "000000000000000a"
private const val KEY_B = "000000000000000b"
private const val KEY_C = "000000000000000c"
private const val STEM_A = "a000000000000000"
private const val STEM_B = "b000000000000000"
private const val STEM_C = "c000000000000000"

private val KOTLIN_ONE = fixtureNotice(KEY_A, "Kotlin", STEM_A, "ads-mobile-sdk")
private val OKHTTP = fixtureNotice(KEY_B, "OkHttp", STEM_B, "ads-mobile-sdk")
private val KOTLIN_TWO = fixtureNotice(KEY_C, "Kotlin", STEM_C, "hsdp", "user-messaging-platform")
private val INDEX = listOf(KOTLIN_ONE, OKHTTP, KOTLIN_TWO)
