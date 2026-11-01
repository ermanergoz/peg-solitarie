package com.app.pegsolitaire.ui.licenses

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.licenses.BundledLicenseText
import com.app.pegsolitaire.testing.RecordingDispatcher
import com.app.pegsolitaire.ui.licenses.LicenseTextHeader.Bundled
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
 * `LicenseTextViewModel` (spec §4, LS-09; tech design §3.6; brief §3.4): it resolves the route's
 * entry id against the catalogue, reads the entry's bundled texts in catalogue order through
 * [LicenseTextSource][com.app.pegsolitaire.data.licenses.LicenseTextSource], re-flows them on the
 * injected dispatcher, and exposes Loading → Loaded or Failed. An id the catalogue does not know —
 * or one that opens no bundled text — is Failed with no header and **no read at all** (LS-AC14).
 *
 * Robolectric backs `SavedStateHandle.toRoute<LicenseText>()`; `Dispatchers.setMain` with a
 * `StandardTestDispatcher` makes `viewModelScope` run only when the virtual clock is advanced.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
@OptIn(ExperimentalCoroutinesApi::class) // advanceUntilIdle virtual-time control
class LicenseTextViewModelTest {
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
    fun loadingThenLoadedForAOneTextEntry() =
        runTest(testDispatcher) {
            val raw = "Copyright line\nwraps here.\n\nSecond.\n"
            val source = FakeLicenseTextSource(mapOf(BundledLicenseText.FredokaOfl to raw))
            val fredoka = requireNotNull(LicenseCatalog.entry(FREDOKA))
            val part = LoadedTextPart(heading = null, paragraphs = listOf("Copyright line wraps here.", "Second."))

            viewModel(FREDOKA, source).uiState.test {
                assertThat(awaitItem()).isEqualTo(Loading(Bundled(fredoka)))
                assertThat(awaitItem()).isEqualTo(Loaded(Bundled(fredoka), listOf(part)))
            }
            assertThat(source.reads).containsExactly(BundledLicenseText.FredokaOfl)
        }

    @Test
    fun partsArriveInCatalogueOrderForATwoTextEntry() =
        runTest(testDispatcher) {
            val source =
                FakeLicenseTextSource(
                    mapOf(
                        BundledLicenseText.JakartaInjectNotice to "# Notices\n\nProject home.\n",
                        BundledLicenseText.Apache2 to "Apache License\nVersion 2.0\n",
                    ),
                )
            val viewModel = viewModel(JAKARTA, source)

            advanceUntilIdle()

            val jakarta = requireNotNull(LicenseCatalog.entry(JAKARTA))
            assertThat(viewModel.uiState.value)
                .isEqualTo(
                    Loaded(
                        Bundled(jakarta),
                        listOf(
                            LoadedTextPart(R.string.license_text_part_notice, listOf("# Notices", "Project home.")),
                            LoadedTextPart(R.string.licenses_apache_name, listOf("Apache License Version 2.0")),
                        ),
                    ),
                )
            assertThat(source.reads)
                .containsExactly(BundledLicenseText.JakartaInjectNotice, BundledLicenseText.Apache2)
                .inOrder()
        }

    @Test
    fun anUnknownIdIsFailedAndReadsNoAsset() =
        runTest(testDispatcher) {
            val source = FakeLicenseTextSource()
            val viewModel = viewModel("licenses/Apache-2.0.txt", source)

            assertThat(viewModel.uiState.value).isEqualTo(Failed(header = null))
            advanceUntilIdle()
            assertThat(viewModel.uiState.value).isEqualTo(Failed(header = null))
            assertThat(source.reads).isEmpty()
        }

    @Test
    fun anEntryThatOpensNoBundledTextIsFailedAndReadsNoAsset() =
        runTest(testDispatcher) {
            val source = FakeLicenseTextSource()

            listOf(KENNEY, GOOGLE_NOTICES).forEach { id ->
                val viewModel = viewModel(id, source)
                advanceUntilIdle()
                assertThat(viewModel.uiState.value).isEqualTo(Failed(header = null))
            }
            assertThat(source.reads).isEmpty()
        }

    @Test
    fun aNullReadIsFailedWithTheEntryStillNamed() =
        runTest(testDispatcher) {
            val source = FakeLicenseTextSource(mapOf(BundledLicenseText.JakartaInjectNotice to "# Notices\n"))
            val viewModel = viewModel(JAKARTA, source)

            advanceUntilIdle()

            val jakarta = requireNotNull(LicenseCatalog.entry(JAKARTA))
            assertThat(viewModel.uiState.value).isEqualTo(Failed(Bundled(jakarta)))
        }

    @Test
    fun theReflowRunsOnTheInjectedDispatcher() =
        runTest(testDispatcher) {
            val reflow = RecordingDispatcher(StandardTestDispatcher(testScheduler))
            val source = FakeLicenseTextSource(mapOf(BundledLicenseText.NunitoOfl to "One\n\nTwo\n"))
            val viewModel = viewModel(NUNITO, source, reflow)

            advanceUntilIdle()

            assertThat(viewModel.uiState.value).isInstanceOf(Loaded::class.java)
            assertThat(reflow.dispatches).isAtLeast(1)
        }

    private fun viewModel(
        entryId: String,
        source: FakeLicenseTextSource,
        defaultDispatcher: CoroutineDispatcher = testDispatcher,
    ): LicenseTextViewModel =
        LicenseTextViewModel(
            savedStateHandle = SavedStateHandle(mapOf(ENTRY_ID_ARG to entryId)),
            source = source,
            defaultDispatcher = defaultDispatcher,
        )
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34

/** The `LicenseText` route's one argument, as `toRoute` reads it back. */
private const val ENTRY_ID_ARG = "entryId"

private const val FREDOKA = "fredoka"
private const val NUNITO = "nunito"
private const val JAKARTA = "jakarta-inject"
private const val KENNEY = "kenney-interface-sounds"
private const val GOOGLE_NOTICES = "google-notices"
