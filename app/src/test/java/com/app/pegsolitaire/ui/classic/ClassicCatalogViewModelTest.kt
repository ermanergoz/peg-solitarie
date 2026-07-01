package com.app.pegsolitaire.ui.classic

import app.cash.turbine.test
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.Level
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Turbine + Truth coverage of `ClassicCatalogViewModel`'s single behavior: it is a
 * pure pass-through of [LevelRepository.classicCatalog] into an immutable
 * `ClassicCatalogUiState`.
 *
 * The repository already enforces the D-18 ascending hole-count order (02-04), so
 * the ViewModel must not reorder, filter or mutate the descriptors — these tests
 * pin that contract: exactly the 5 descriptors the repository returns, in the same
 * id order, with every field intact.
 *
 * No Robolectric: the ViewModel touches no Android framework (no `SavedStateHandle`,
 * no `toRoute`), so a plain JVM `runTest` exercises it directly.
 */
class ClassicCatalogViewModelTest {
    @Test
    fun exposesTheFiveDescriptorsInD18IdOrder() =
        runTest {
            val viewModel = ClassicCatalogViewModel(FakeLevelRepository(D18_CATALOG))

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.boards.map { it.id })
                    .containsExactly(
                        ClassicLevelIds.ENGLISH33,
                        ClassicLevelIds.EUROPEAN37,
                        ClassicLevelIds.ASYMMETRIC39,
                        ClassicLevelIds.DIAMOND41,
                        ClassicLevelIds.WIEGLEB45,
                    ).inOrder()
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun passesEveryDescriptorFieldThroughUnchanged() =
        runTest {
            val viewModel = ClassicCatalogViewModel(FakeLevelRepository(D18_CATALOG))

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.boards).isEqualTo(D18_CATALOG)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private companion object {
        /** The 5 Classic descriptors in the repository's D-18 ascending hole-count order. */
        val D18_CATALOG: List<ClassicLevelDescriptor> =
            listOf(
                descriptor(ClassicLevelIds.ENGLISH33, "English-33", 33, Boards.english33Initial),
                descriptor(ClassicLevelIds.EUROPEAN37, "European-37", 37, Boards.european37Initial),
                descriptor(ClassicLevelIds.ASYMMETRIC39, "Asymmetric 3-3-2-2", 39, Boards.asymmetric39Initial),
                descriptor(ClassicLevelIds.DIAMOND41, "Diamond-41", 41, Boards.diamond41Initial),
                descriptor(ClassicLevelIds.WIEGLEB45, "Wiegleb-45", 45, Boards.wiegleb45Initial),
            )

        fun descriptor(
            id: String,
            name: String,
            holes: Int,
            board: Board,
        ): ClassicLevelDescriptor =
            ClassicLevelDescriptor(
                id = id,
                displayName = name,
                holeCount = holes,
                previewBoard = board,
            )
    }
}

/**
 * Minimal in-memory [LevelRepository] returning a fixed catalog so the ViewModel's
 * pass-through is exercised without the Android-resource-backed real source. The
 * load methods are unused by the catalog ViewModel.
 */
private class FakeLevelRepository(
    private val catalog: List<ClassicLevelDescriptor>,
) : LevelRepository {
    override fun classicCatalog(): List<ClassicLevelDescriptor> = catalog

    override fun loadClassic(levelId: String): Level = error("Catalog ViewModel never loads a level")

    override fun loadChallenge(levelNumber: Int): Level = error("Challenge is a Phase-3 stub")
}
