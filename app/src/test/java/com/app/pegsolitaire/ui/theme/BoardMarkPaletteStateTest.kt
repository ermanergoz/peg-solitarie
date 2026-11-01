package com.app.pegsolitaire.ui.theme

import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.data.SettingsRepositoryImpl
import com.app.pegsolitaire.testing.InMemoryPreferencesDataStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * The one root collection of the palette setting (docs/ux/colour-vision.md §3.3; the tech-lead's
 * brief §1): a process-wide `StateFlow` that is [BoardMarkPalette.Standard] until the store's
 * first read lands and then follows the store. Pure JVM: the holder is a flow over the
 * repository, on whatever scope it is handed — here the test's background scope.
 */
class BoardMarkPaletteStateTest {
    /** Property 1: before the first read lands, the standard palette is in use. */
    @Test
    fun thePaletteIsStandardBeforeTheStoresFirstEmission() =
        runTest {
            val state = BoardMarkPaletteState(SettingsThatNeverEmit, backgroundScope)
            runCurrent()

            assertThat(state.palette.value).isEqualTo(BoardMarkPalette.Standard)
        }

    /** Property 3's source: every emission of the store reaches the holder, both ways. */
    @Test
    fun thePaletteFollowsTheStore() =
        runTest {
            val settings = PaletteSettings()
            val state = BoardMarkPaletteState(settings, backgroundScope)
            runCurrent()
            assertThat(state.palette.value).isEqualTo(BoardMarkPalette.Standard)

            settings.colorblind.value = true
            runCurrent()
            assertThat(state.palette.value).isEqualTo(BoardMarkPalette.Colorblind)

            settings.colorblind.value = false
            runCurrent()
            assertThat(state.palette.value).isEqualTo(BoardMarkPalette.Standard)
        }

    /** End to end over the real repository and an in-memory store: the switch's write turns the palette on. */
    @Test
    fun thePaletteIsColorblindOnceTheSettingIsWritten() =
        runTest {
            val repository = SettingsRepositoryImpl(InMemoryPreferencesDataStore())
            val state = BoardMarkPaletteState(repository, backgroundScope)

            repository.setColorblindPaletteOn(true)

            assertThat(state.palette.first { it == BoardMarkPalette.Colorblind })
                .isEqualTo(BoardMarkPalette.Colorblind)
        }
}

/** A store whose palette flag is settable; the other flags are inert. */
private class PaletteSettings : SettingsRepository {
    val colorblind = MutableStateFlow(false)

    override val hintsEnabled: Flow<Boolean> = flowOf(true)

    override val soundOn: Flow<Boolean> = flowOf(true)

    override val hapticsOn: Flow<Boolean> = flowOf(true)

    override val colorblindPaletteOn: Flow<Boolean> = colorblind

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit

    override suspend fun setColorblindPaletteOn(on: Boolean) {
        colorblind.value = on
    }
}

/** A store that has not answered yet: a cold start, or a process restored straight into gameplay. */
private object SettingsThatNeverEmit : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> = emptyFlow()

    override val soundOn: Flow<Boolean> = emptyFlow()

    override val hapticsOn: Flow<Boolean> = emptyFlow()

    override val colorblindPaletteOn: Flow<Boolean> = emptyFlow()

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit

    override suspend fun setColorblindPaletteOn(on: Boolean) = Unit
}
