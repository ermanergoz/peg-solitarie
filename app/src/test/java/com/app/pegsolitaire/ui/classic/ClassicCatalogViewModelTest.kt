package com.app.pegsolitaire.ui.classic

import app.cash.turbine.test
import com.app.pegsolitaire.data.ClassicLevelDescriptor
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.data.Level
import com.app.pegsolitaire.data.LevelRepository
import com.app.pegsolitaire.data.LevelResult
import com.app.pegsolitaire.data.NewBests
import com.app.pegsolitaire.data.ProgressPersistenceKeys
import com.app.pegsolitaire.data.ProgressRepository
import com.app.pegsolitaire.data.resume.ResumeSnapshotDto
import com.app.pegsolitaire.domain.achievement.ClassicBoard
import com.app.pegsolitaire.domain.achievement.ProgressAggregate
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Turbine + Truth coverage of `ClassicCatalogViewModel`: it combines
 * [LevelRepository.classicCatalog] with the player's stored Classic results
 * ([ProgressRepository.observeResults]) into an immutable `ClassicCatalogUiState`.
 *
 * The level repository already enforces the D-18 ascending hole-count order (02-04), so
 * the ViewModel must not reorder, filter or mutate the descriptors — these tests pin
 * that contract: exactly the 5 descriptors the repository returns, in the same id order,
 * with every field intact. On top of that each card carries `earnedStars` (D-10): the
 * stored best for that board id, or 0 for a board never solved.
 *
 * No Robolectric: the ViewModel touches no Android framework (no `SavedStateHandle`, no
 * `toRoute`). `Dispatchers.setMain(StandardTestDispatcher())` makes the `viewModelScope`
 * results collector deterministic under `runTest` virtual time.
 */
@OptIn(ExperimentalCoroutinesApi::class) // runCurrent virtual-time control
class ClassicCatalogViewModelTest {
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
    fun exposesTheFiveDescriptorsInD18IdOrder() =
        runTest(testDispatcher) {
            val viewModel = viewModelWith()
            runCurrent()

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.boards.map { it.descriptor.id })
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
        runTest(testDispatcher) {
            val viewModel = viewModelWith()
            runCurrent()

            viewModel.uiState.test {
                val state = awaitItem()
                assertThat(state.boards.map { it.descriptor }).isEqualTo(D18_CATALOG)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun anUnplayedCatalogShowsNoEarnedStarsAtAll() =
        runTest(testDispatcher) {
            val viewModel = viewModelWith()
            runCurrent()

            assertThat(
                viewModel.uiState.value.boards
                    .map { it.earnedStars },
            ).containsExactly(NO_STARS, NO_STARS, NO_STARS, NO_STARS, NO_STARS)
        }

    @Test
    fun aStoredResultSurfacesItsStarsOnThatCardAndLeavesTheOthersAtZero() =
        runTest(testDispatcher) {
            val viewModel =
                viewModelWith(
                    results = listOf(classicResult(ClassicLevelIds.EUROPEAN37, TWO_STARS)),
                )
            runCurrent()

            val byId =
                viewModel.uiState.value.boards
                    .associate { it.descriptor.id to it.earnedStars }
            assertThat(byId[ClassicLevelIds.EUROPEAN37]).isEqualTo(TWO_STARS)
            assertThat(byId[ClassicLevelIds.ENGLISH33]).isEqualTo(NO_STARS)
            assertThat(byId[ClassicLevelIds.WIEGLEB45]).isEqualTo(NO_STARS)
        }

    @Test
    fun aResultRecordedLaterUpdatesThatCardsPips() =
        runTest(testDispatcher) {
            val results = MutableStateFlow(emptyList<LevelResult>())
            val viewModel = ClassicCatalogViewModel(FakeLevelRepository(D18_CATALOG), FakeProgressRepository(results))
            runCurrent()
            assertThat(
                viewModel.uiState.value.boards
                    .first()
                    .earnedStars,
            ).isEqualTo(NO_STARS)

            results.value = listOf(classicResult(ClassicLevelIds.ENGLISH33, THREE_STARS))
            runCurrent()

            assertThat(
                viewModel.uiState.value.boards
                    .first()
                    .earnedStars,
            ).isEqualTo(THREE_STARS)
        }

    @Test
    fun readsTheClassicModeResultsNotTheChallengeLadder() =
        runTest(testDispatcher) {
            val repository = FakeProgressRepository(MutableStateFlow(emptyList()))
            ClassicCatalogViewModel(FakeLevelRepository(D18_CATALOG), repository)
            runCurrent()

            assertThat(repository.observedModes).containsExactly(ProgressPersistenceKeys.MODE_CLASSIC)
        }

    private fun viewModelWith(results: List<LevelResult> = emptyList()): ClassicCatalogViewModel =
        ClassicCatalogViewModel(
            levelRepository = FakeLevelRepository(D18_CATALOG),
            progressRepository = FakeProgressRepository(MutableStateFlow(results)),
        )

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

        /** A stored Classic best whose only interesting field here is its star count. */
        fun classicResult(
            levelId: String,
            stars: Int,
        ): LevelResult =
            LevelResult(
                mode = ProgressPersistenceKeys.MODE_CLASSIC,
                levelId = levelId,
                stars = stars,
                bestTimeMs = SAMPLE_BEST_TIME_MS,
                bestScore = SAMPLE_BEST_SCORE,
                completedAt = SAMPLE_COMPLETED_AT,
            )
    }
}

/** No result stored for a board ⇒ no pips lit. */
private const val NO_STARS = 0
private const val TWO_STARS = 2
private const val THREE_STARS = 3

/** Arbitrary non-star fields of a stored best; the catalog never renders them. */
private const val SAMPLE_BEST_TIME_MS = 61_000L
private const val SAMPLE_BEST_SCORE = 1234
private const val SAMPLE_COMPLETED_AT = 1_700_000_000_000L

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

/**
 * A settable [ProgressRepository] whose [observeResults] replays [results], recording
 * which modes were asked for so the catalog can be proven to read the Classic rows only.
 * The record and resume members are never reached from the catalog.
 */
private class FakeProgressRepository(
    private val results: MutableStateFlow<List<LevelResult>>,
) : ProgressRepository {
    val observedModes = mutableListOf<String>()

    override suspend fun recordResult(
        mode: String,
        levelId: String,
        result: ScoreResult,
        nowMs: Long,
    ): NewBests = error("The catalog never records a result")

    /** Unused by this screen — the catalog never reads the achievement aggregate. */
    override suspend fun aggregate(): ProgressAggregate = ProgressAggregate()

    /** Unused by this screen — no per-board leaderboard read happens here. */
    override suspend fun classicBests(): Map<ClassicBoard, Int> = emptyMap()

    override fun observeResults(mode: String): Flow<List<LevelResult>> {
        observedModes += mode
        return results
    }

    override suspend fun saveSnapshot(
        dto: ResumeSnapshotDto,
        nowMs: Long,
    ) = error("The catalog never saves a resume snapshot")

    override suspend fun loadSnapshot(mode: String): ResumeSnapshotDto? = error("The catalog never loads a snapshot")

    override suspend fun clearSnapshot(mode: String) = error("The catalog never clears a snapshot")

    override fun observeResumeModes(): Flow<List<String>> = flowOf(emptyList())
}
