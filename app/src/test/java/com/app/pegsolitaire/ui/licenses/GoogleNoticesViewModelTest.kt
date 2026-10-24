package com.app.pegsolitaire.ui.licenses

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
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

/**
 * `GoogleNoticesViewModel` (spec §4A, LS-09, LS-12; brief §0.3, A1.5): the generated index
 * becomes the list's rows once, in index order — the sources joined for the "Found in" line and
 * an ordinal on every repeated name — so composition formats and nothing more. A missing,
 * malformed or empty index is Failed: a build that produced no notices is a defect, and an empty
 * list would hide it.
 */
@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle virtual-time control
class GoogleNoticesViewModelTest {
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
    fun loadingThenAnIndexBecomesRowsWithJoinedSources() =
        runTest(testDispatcher) {
            val source =
                FakeGoogleNoticeSource(
                    listOf(
                        fixtureNotice(KEY_A, "Android SDK Platforms", STEM_A, "hsdp", "user-messaging-platform"),
                        fixtureNotice(KEY_B, "OkHttp", STEM_B, "ads-mobile-sdk"),
                    ),
                )

            GoogleNoticesViewModel(source).uiState.test {
                assertThat(awaitItem()).isEqualTo(GoogleNoticesUiState.Loading)
                assertThat(awaitItem())
                    .isEqualTo(
                        GoogleNoticesUiState.Loaded(
                            listOf(
                                GoogleNoticeRow(
                                    KEY_A,
                                    "Android SDK Platforms",
                                    "hsdp, user-messaging-platform",
                                    ordinal = null,
                                ),
                                GoogleNoticeRow(KEY_B, "OkHttp", "ads-mobile-sdk", ordinal = null),
                            ),
                        ),
                    )
            }
            assertThat(source.reads).isEqualTo(1)
        }

    @Test
    fun rowsSharingANameCarryTheirOrdinals() =
        runTest(testDispatcher) {
            val source =
                FakeGoogleNoticeSource(
                    listOf(
                        fixtureNotice(KEY_A, "Kotlin", STEM_A, "ads-mobile-sdk"),
                        fixtureNotice(KEY_B, "Kotlin", STEM_B, "ads-mobile-sdk"),
                    ),
                )
            val viewModel = GoogleNoticesViewModel(source)

            advanceUntilIdle()

            val rows = (viewModel.uiState.value as GoogleNoticesUiState.Loaded).rows
            assertThat(rows.map { it.ordinal }).containsExactly(NoticeOrdinal(1, 2), NoticeOrdinal(2, 2)).inOrder()
        }

    @Test
    fun anEmptyIndexIsFailed() =
        runTest(testDispatcher) {
            val viewModel = GoogleNoticesViewModel(FakeGoogleNoticeSource(emptyList()))

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(GoogleNoticesUiState.Failed)
        }

    @Test
    fun aNullIndexIsFailed() =
        runTest(testDispatcher) {
            val viewModel = GoogleNoticesViewModel(FakeGoogleNoticeSource(null))

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isEqualTo(GoogleNoticesUiState.Failed)
        }

    private companion object {
        const val KEY_A = "0123456789abcdef"
        const val KEY_B = "fedcba9876543210"
        const val STEM_A = "00112233445566ff"
        const val STEM_B = "aabbccddeeff0011"
    }
}
