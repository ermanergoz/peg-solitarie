package com.app.pegsolitaire.platform.games.save

import android.app.Activity
import android.content.Intent
import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
import com.app.pegsolitaire.platform.games.FakeGamesService
import com.app.pegsolitaire.platform.games.GamesSignInState
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.SnapshotsClient
import com.google.android.gms.tasks.Tasks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Behaviour spec for [PlayGamesSnapshotPicker] — Google's own "select snapshot" screen, reachable
 * only through [DebugSnapshotPickerEntry].
 *
 * It is a viewer and must stay one: the add button and the delete affordance are both off, so a QA
 * session can read the cover, the description and the played time (CLOUD-03) and can neither
 * create a second snapshot nor delete the player's only one. Same harness as
 * `PlayGamesCloudSaveStoreTest`.
 *
 * This file lives in `src/testDebug` rather than `src/test` because the class under test lives in
 * `src/debug`: the picker is not compiled into release (tech-lead L-3), so neither is its spec. The
 * app module has only a `debug` unit-test variant today, so `src/test` would still have compiled —
 * but by accident, and it would break the day a release unit-test variant appears.
 */
class PlayGamesSnapshotPickerTest {
    private val testDispatcher = StandardTestDispatcher()
    private val activityProvider = mockk<ForegroundActivityProvider>()
    private val activity = mockk<Activity>()
    private val client = mockk<SnapshotsClient>()
    private val pickerIntent = mockk<Intent>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(PlayGames::class)
        every { PlayGames.getSnapshotsClient(any()) } returns client
        coEvery { activityProvider.awaitActivity() } returns activity
        every { activity.startActivityForResult(any(), any()) } just runs
        every { client.getSelectSnapshotIntent(any(), any(), any(), any()) } returns Tasks.forResult(pickerIntent)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun aSignedInPlayerGetsAReadOnlyPickerOfTheOneSnapshot() =
        runTest(testDispatcher) {
            picker(signedIn()).show()

            // allowAddButton = false, allowDelete = false: asserted literally, because either one
            // flipped turns a viewer into a way to lose the player's only save.
            verify(exactly = 1) {
                client.getSelectSnapshotIntent(
                    DebugPickerConstants.PICKER_TITLE,
                    false,
                    false,
                    DebugPickerConstants.MAX_SNAPSHOTS,
                )
            }
            verify(exactly = 1) {
                activity.startActivityForResult(pickerIntent, DebugPickerConstants.REQUEST_CODE)
            }
        }

    @Test
    fun thePickerWaitsForASessionThatResolvesAfterLaunch() =
        runTest(testDispatcher) {
            // The extra arrives in onCreate, before the sign-in bootstrap has answered.
            val games = FakeGamesService(GamesSignInState.Unknown)
            val picker = PlayGamesSnapshotPicker(activityProvider, games)

            val showing = launch { picker.show() }
            testScheduler.runCurrent()
            verify(exactly = 0) { PlayGames.getSnapshotsClient(any()) }

            games.emitSession(signedIn())
            showing.join()

            verify(exactly = 1) { activity.startActivityForResult(pickerIntent, any()) }
        }

    @Test
    fun aPlayerWhoNeverSignsInNeverReachesTheSdk() =
        runTest(testDispatcher) {
            picker(GamesSignInState.SignedOut).show()

            // Bounded, so a debug launch by a signed-out tester does not park a coroutine forever.
            verify(exactly = 0) { PlayGames.getSnapshotsClient(any()) }
            verify(exactly = 0) { activity.startActivityForResult(any(), any()) }
        }

    @Test
    fun noForegroundActivityMeansNoPickerAndNoSdkCall() =
        runTest(testDispatcher) {
            coEvery { activityProvider.awaitActivity() } coAnswers { awaitCancellation() }

            picker(signedIn()).show()

            verify(exactly = 0) { PlayGames.getSnapshotsClient(any()) }
        }

    @Test
    fun anSdkRefusalIsSwallowedSilently() =
        runTest(testDispatcher) {
            every { client.getSelectSnapshotIntent(any(), any(), any(), any()) } returns
                Tasks.forException(IllegalStateException(SDK_FAILURE))

            picker(signedIn()).show()

            verify(exactly = 0) { activity.startActivityForResult(any(), any()) }
        }

    private fun picker(session: GamesSignInState) = PlayGamesSnapshotPicker(activityProvider, FakeGamesService(session))

    private fun signedIn() = GamesSignInState.SignedIn(playerId = PLAYER_ID, displayName = null)
}

private const val PLAYER_ID = "placeholder-player"
private const val SDK_FAILURE = "sdk failure"
