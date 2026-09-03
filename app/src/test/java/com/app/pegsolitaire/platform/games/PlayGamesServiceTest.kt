package com.app.pegsolitaire.platform.games

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.app.pegsolitaire.data.PlayGamesOptInRepository
import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.games.AuthenticationResult
import com.google.android.gms.games.GamesSignInClient
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import com.google.android.gms.games.Player
import com.google.android.gms.games.PlayersClient
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Behaviour spec for [PlayGamesService] — the two rules this phase is actually judged on.
 *
 * **Rule one: a player who never opted in is never reached by the SDK (D-01).** That is the
 * whole premise of the explicit-opt-in design, and it is unfalsifiable by inspection: the
 * violating version of this class looks identical apart from where one `if` sits. So it is
 * pinned negatively — `awaitActivity()` is never awaited and `PlayGamesSdk.initialize` is
 * never called on the not-opted-in path. [FakeGamesService] deliberately does not
 * re-implement this gate, precisely so it is proven here, once, where it lives.
 *
 * **Rule two: [GamesService.session] never stays at [GamesSignInState.Unknown] (D-11).**
 * Every branch below — declined opt-in, no foreground Activity, a thrown SDK call, an
 * unauthenticated player, a failed player-ID lookup — asserts a *resolved* state, because a
 * surface that hides itself while `Unknown` (the Settings section, D-11) would otherwise stay
 * hidden forever on whichever branch was left dangling. Asserting equality with
 * `SignedOut`/`SignedIn` is what makes "never stuck at Unknown" a test rather than a hope.
 *
 * The Play Games statics are mocked with MockK (`mockkStatic`) rather than hidden behind a
 * hand-written client seam. The seam would be the more fashionable design and it would prove
 * less: the thing most likely to go wrong here is the *order* of the real static calls
 * relative to the opt-in gate, and only mocking the real statics can observe that order.
 *
 * `Task`s are built with the SDK's own [Tasks] factory rather than mocked — a completed
 * `Task` is taken by `await()`'s fast path with no listener, executor or `Looper` involved,
 * so this stays a plain JVM test with no Robolectric runtime.
 *
 * Same virtual-time shell as the ViewModel tests: `Dispatchers.setMain(StandardTestDispatcher())`
 * plus a scope on the same scheduler, so the fire-and-forget bootstrap is deterministic and the
 * ten-second Activity bound elapses instantly.
 */
class PlayGamesServiceTest {
    private val testDispatcher = StandardTestDispatcher()
    private val activityProvider = mockk<ForegroundActivityProvider>()
    private val activity = mockk<Activity>()
    private val signInClient = mockk<GamesSignInClient>()
    private val playersClient = mockk<PlayersClient>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(PlayGamesSdk::class, PlayGames::class)
        every { PlayGamesSdk.initialize(any()) } just runs
        every { PlayGames.getGamesSignInClient(any()) } returns signInClient
        every { PlayGames.getPlayersClient(any()) } returns playersClient
        every { activity.applicationContext } returns mockk<Context>()
        coEvery { activityProvider.awaitActivity() } returns activity
        every { playersClient.getCurrentPlayerId() } returns Tasks.forResult(PLAYER_ID)
        every { playersClient.getCurrentPlayer() } returns Tasks.forResult(player())
        every { signInClient.isAuthenticated() } returns Tasks.forResult(authentication(true))
        every { signInClient.signIn() } returns Tasks.forResult(authentication(true))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    // ---- bootstrapIfOptedIn: the D-01 gate -----------------------------------------

    @Test
    fun bootstrapForANonOptedInPlayerResolvesSignedOutWithoutTouchingTheSdk() =
        runTest(testDispatcher) {
            val service = serviceFor(optedIn = false)

            service.bootstrapIfOptedIn()
            advanceUntilIdle()

            // The state resolves — a player who never opted in has nothing to wait on, so
            // leaving them at Unknown would hide the Settings section forever (D-11/D-19).
            assertThat(service.session.value).isEqualTo(GamesSignInState.SignedOut)
            // And it resolves without the SDK, or even the Activity, being reached at all.
            coVerify(exactly = 0) { activityProvider.awaitActivity() }
            verify(exactly = 0) { PlayGamesSdk.initialize(any()) }
            verify(exactly = 0) { PlayGames.getGamesSignInClient(any()) }
        }

    @Test
    fun bootstrapForAnOptedInAuthenticatedPlayerResolvesSignedInWithTheDisplayName() =
        runTest(testDispatcher) {
            val service = serviceFor(optedIn = true)

            service.bootstrapIfOptedIn()
            advanceUntilIdle()

            assertThat(service.session.value)
                .isEqualTo(GamesSignInState.SignedIn(PLAYER_ID, DISPLAY_NAME))
            verify(exactly = 1) { PlayGamesSdk.initialize(any()) }
        }

    @Test
    fun bootstrapForAnOptedInButUnauthenticatedPlayerResolvesSignedOut() =
        runTest(testDispatcher) {
            every { signInClient.isAuthenticated() } returns Tasks.forResult(authentication(false))
            val service = serviceFor(optedIn = true)

            service.bootstrapIfOptedIn()
            advanceUntilIdle()

            assertThat(service.session.value).isEqualTo(GamesSignInState.SignedOut)
        }

    @Test
    fun bootstrapResolvesSignedOutWhenNoForegroundActivityEverArrives() =
        runTest(testDispatcher) {
            // A backgrounded app may never resume. The bound is what stops session sitting
            // at Unknown for the life of the process (T-11-06).
            coEvery { activityProvider.awaitActivity() } coAnswers { awaitCancellation() }
            val service = serviceFor(optedIn = true)

            service.bootstrapIfOptedIn()
            advanceUntilIdle()

            assertThat(service.session.value).isEqualTo(GamesSignInState.SignedOut)
            verify(exactly = 0) { PlayGamesSdk.initialize(any()) }
        }

    @Test
    fun bootstrapResolvesSignedOutWhenTheAuthenticationCheckThrows() =
        runTest(testDispatcher) {
            // D-12: missing Play services, missing Play Games app and an offline device are
            // one state, not three. No GoogleApiAvailability gating, no connectivity retry.
            every { signInClient.isAuthenticated() } returns Tasks.forException(RuntimeException(SDK_FAILURE))
            val service = serviceFor(optedIn = true)

            service.bootstrapIfOptedIn()
            advanceUntilIdle()

            assertThat(service.session.value).isEqualTo(GamesSignInState.SignedOut)
        }

    @Test
    fun bootstrapResolvesSignedOutWhenThePlayerIdLookupFails() =
        runTest(testDispatcher) {
            every { playersClient.getCurrentPlayerId() } returns Tasks.forException(RuntimeException(SDK_FAILURE))
            val service = serviceFor(optedIn = true)

            service.bootstrapIfOptedIn()
            advanceUntilIdle()

            // Unlike a missing display name (below), a missing player ID leaves nothing to
            // identify the player with — SignedIn is not constructible, so D-12's single
            // not-signed-in state is the honest answer.
            assertThat(service.session.value).isEqualTo(GamesSignInState.SignedOut)
        }

    @Test
    fun aFailedDisplayNameLookupDegradesTheNameAndNeverTheSignIn() =
        runTest(testDispatcher) {
            // D-14: the player IS signed in. A name lookup widens what a surface can say
            // about them; it never decides whether they are signed in at all.
            every { playersClient.getCurrentPlayer() } returns Tasks.forException(RuntimeException(SDK_FAILURE))
            val service = serviceFor(optedIn = true)

            service.bootstrapIfOptedIn()
            advanceUntilIdle()

            assertThat(service.session.value)
                .isEqualTo(GamesSignInState.SignedIn(PLAYER_ID, displayName = null))
        }

    @Test
    fun aSecondBootstrapInTheSameProcessDoesNotTouchTheSdkAgain() =
        runTest(testDispatcher) {
            // D-02: the silent check runs once per process. MainActivity.onCreate fires this
            // on every Activity creation, so a configuration change alone reaches it twice.
            val service = serviceFor(optedIn = true)

            service.bootstrapIfOptedIn()
            advanceUntilIdle()
            service.bootstrapIfOptedIn()
            advanceUntilIdle()

            verify(exactly = 1) { PlayGamesSdk.initialize(any()) }
            verify(exactly = 1) { signInClient.isAuthenticated() }
            assertThat(service.session.value)
                .isEqualTo(GamesSignInState.SignedIn(PLAYER_ID, DISPLAY_NAME))
        }

    // ---- signIn: the D-10 decline-versus-error contract -----------------------------

    @Test
    fun signInResolvesSignedInAndReportsSuccess() =
        runTest(testDispatcher) {
            val service = serviceFor(optedIn = false)

            val signedIn = service.signIn()

            // optedIn is false on purpose: the Settings row (D-19) may be a player's very
            // first opt-in, so signIn must initialize the SDK itself rather than assume
            // bootstrapIfOptedIn already did.
            assertThat(signedIn).isTrue()
            assertThat(service.session.value)
                .isEqualTo(GamesSignInState.SignedIn(PLAYER_ID, DISPLAY_NAME))
            verify(exactly = 1) { PlayGamesSdk.initialize(any()) }
        }

    @Test
    fun signInReportsASilentDeclineWhenTheSdkAnswersNotAuthenticated() =
        runTest(testDispatcher) {
            every { signInClient.signIn() } returns Tasks.forResult(authentication(false))
            val service = serviceFor(optedIn = false)

            val signedIn = service.signIn()

            assertThat(signedIn).isFalse()
            assertThat(service.session.value).isEqualTo(GamesSignInState.SignedOut)
        }

    @Test
    fun signInSwallowsAPlainCancellationRatherThanRaisingAnError() =
        runTest(testDispatcher) {
            // D-10: dismissing the account chooser is silent at every call site — no
            // snackbar, no retry prompt, and the Settings row simply remains (D-13).
            every { signInClient.signIn() } returns
                Tasks.forException(ApiException(Status(CommonStatusCodes.CANCELED)))
            val service = serviceFor(optedIn = false)

            val signedIn = service.signIn()

            assertThat(signedIn).isFalse()
            assertThat(service.session.value).isEqualTo(GamesSignInState.SignedOut)
        }

    @Test
    fun signInRethrowsAGenuineSdkErrorSoTheCallerCanSurfaceIt() =
        runTest(testDispatcher) {
            // The other half of D-10: a throw is what raises the snackbar. Swallowing this
            // would leave the player with no explanation and a row that did nothing.
            every { signInClient.signIn() } returns Tasks.forException(RuntimeException(SDK_FAILURE))
            val service = serviceFor(optedIn = false)

            val thrown = runCatching { service.signIn() }.exceptionOrNull()

            assertThat(thrown).isInstanceOf(RuntimeException::class.java)
            assertThat(service.session.value).isEqualTo(GamesSignInState.SignedOut)
        }

    @Test
    fun signInReportsADeclineWhenNoForegroundActivityEverArrives() =
        runTest(testDispatcher) {
            coEvery { activityProvider.awaitActivity() } coAnswers { awaitCancellation() }
            val service = serviceFor(optedIn = false)

            val signedIn = service.signIn()

            // Nothing was touched, so there is nothing to report as an error — a silent
            // false keeps a backgrounded tap from raising a snackbar nobody can see.
            assertThat(signedIn).isFalse()
            verify(exactly = 0) { PlayGamesSdk.initialize(any()) }
        }

    private fun serviceFor(optedIn: Boolean) =
        PlayGamesService(
            activityProvider = activityProvider,
            optInRepository = SettableOptInRepository(optedIn),
            scope = CoroutineScope(testDispatcher),
        )

    private fun authentication(authenticated: Boolean): AuthenticationResult =
        mockk<AuthenticationResult> { every { isAuthenticated } returns authenticated }

    /**
     * The happy-path `Player`. There is deliberately no null-name variant: D-14's degraded
     * name is produced by the *lookup* failing, not by the SDK handing back a nameless
     * player, so faking a null `displayName` here would test a shape the SDK never returns.
     */
    private fun player(): Player =
        mockk<Player> {
            every { playerId } returns PLAYER_ID
            every { displayName } returns DISPLAY_NAME
        }
}

private const val PLAYER_ID = "g-player-1"
private const val DISPLAY_NAME = "Peg Champion"
private const val SDK_FAILURE = "sdk failure"

/**
 * A settable in-memory [PlayGamesOptInRepository]: overrides the DataStore-backed flag so
 * the D-01 gate can be driven directly, with the base store never touched (its constructor
 * takes a no-op, never-emitting [DataStore]). Mirrors `FakeOnboardingRepository`.
 */
private class SettableOptInRepository(
    optedIn: Boolean,
) : PlayGamesOptInRepository(NoopPreferencesDataStore) {
    override val optedIn: Flow<Boolean> = MutableStateFlow(optedIn)
}

/** A never-emitting [DataStore] so the base repository constructor is safe in the fake. */
private object NoopPreferencesDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = emptyFlow()

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        error("not used by the fake")
}
