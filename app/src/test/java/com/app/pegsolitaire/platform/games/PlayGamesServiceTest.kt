package com.app.pegsolitaire.platform.games

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.app.pegsolitaire.data.PlayGamesOptInRepository
import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.games.AchievementsClient
import com.google.android.gms.games.AuthenticationResult
import com.google.android.gms.games.GamesSignInClient
import com.google.android.gms.games.LeaderboardsClient
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import com.google.android.gms.games.Player
import com.google.android.gms.games.PlayersClient
import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
    private val leaderboardsClient = mockk<LeaderboardsClient>()
    private val achievementsClient = mockk<AchievementsClient>()

    /** The SDK-authored stock-UI Intent. Only ever passed through, never read. */
    private val stockUiIntent = mockk<Intent>()

    /** The application context the process-start initialize is handed. Never read. */
    private val appContext = mockk<Context>()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        mockkStatic(PlayGamesSdk::class, PlayGames::class)
        every { PlayGamesSdk.initialize(any()) } just runs
        every { PlayGames.getGamesSignInClient(any()) } returns signInClient
        every { PlayGames.getPlayersClient(any()) } returns playersClient
        every { PlayGames.getLeaderboardsClient(any()) } returns leaderboardsClient
        every { PlayGames.getAchievementsClient(any()) } returns achievementsClient
        every { activity.applicationContext } returns mockk<Context>()
        every { activity.startActivityForResult(any(), any()) } just runs
        every { leaderboardsClient.submitScoreImmediate(any(), any()) } returns Tasks.forResult(mockk())
        every { achievementsClient.unlockImmediate(any()) } returns Tasks.forResult(null)
        every { achievementsClient.setStepsImmediate(any(), any()) } returns Tasks.forResult(true)
        every { leaderboardsClient.getAllLeaderboardsIntent() } returns Tasks.forResult(stockUiIntent)
        every { achievementsClient.getAchievementsIntent() } returns Tasks.forResult(stockUiIntent)
        coEvery { activityProvider.awaitActivity() } returns activity
        // Nothing resumed after the await unless a test says otherwise, so the post-await
        // lookups fall back to the captured instance (WR-01's fallback branch).
        every { activityProvider.current } returns null
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

    // ---- initializeIfOptedIn: the process-start gate -------------------------------

    // Like the bootstrap section below, this is the only place the gate is proven
    // *negatively* — that a player who never opted in is not reached by the SDK at all.
    // [FakeGamesService] counts the call and re-implements nothing, so if these cases go
    // the whole guarantee goes with them.

    @Test
    fun initializeIfOptedInForANonOptedInPlayerNeverTouchesTheSdk() =
        runTest(testDispatcher) {
            val service = serviceFor(optedIn = false)

            service.initializeIfOptedIn()

            verify(exactly = 0) { PlayGamesSdk.initialize(any()) }
            // Not even the Activity: the process-start hook has no Activity to await, and
            // awaiting one here would block Application.onCreate for the ten-second bound.
            coVerify(exactly = 0) { activityProvider.awaitActivity() }
        }

    @Test
    fun initializeIfOptedInForAnOptedInPlayerInitializesTheSdk() =
        runTest(testDispatcher) {
            val service = serviceFor(optedIn = true)

            service.initializeIfOptedIn()

            // No advanceUntilIdle(): initialization must have happened by the time the call
            // returns. Anything deferred to a coroutine lands after the first Activity is
            // created, which is the Phase 11 UAT test 1 failure this fix exists to undo.
            verify(exactly = 1) { PlayGamesSdk.initialize(any()) }
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

    @Test
    fun aBootstrapOverlappingAnExplicitSignInNeverRegressesTheSignedInAnswer() =
        runTest(testDispatcher) {
            // WR-02: a first-ever opt-in from the Home prompt runs signIn with the chooser up;
            // a rotation while it is up recreates MainActivity, whose onCreate fires the
            // bootstrap against a now-true flag. The bootstrap's own check answering "not
            // authenticated" must never overwrite the SignedIn the chooser produced.
            val resumed = CompletableDeferred<Activity>()
            coEvery { activityProvider.awaitActivity() } coAnswers { resumed.await() }
            every { signInClient.isAuthenticated() } returns Tasks.forResult(authentication(false))
            val service = serviceFor(optedIn = true)

            val signIn = async { service.signIn() }
            runCurrent() // signIn holds the SDK lock, waiting for an Activity
            service.bootstrapIfOptedIn()
            runCurrent() // the bootstrap has passed its gate and is queued behind the lock
            resumed.complete(activity)
            advanceUntilIdle()

            assertThat(signIn.await()).isTrue()
            assertThat(service.session.value)
                .isEqualTo(GamesSignInState.SignedIn(PLAYER_ID, DISPLAY_NAME))
            verify(exactly = 0) { signInClient.isAuthenticated() }
            verify(exactly = 1) { PlayGamesSdk.initialize(any()) }
        }

    @Test
    fun aSuccessfulSignInSatisfiesTheOncePerProcessSilentCheck() =
        runTest(testDispatcher) {
            // WR-02's other half: once the chooser has answered, a later bootstrap (the next
            // rotation) has nothing to learn and must not replay the SDK's welcome popup.
            val service = serviceFor(optedIn = true)

            service.signIn()
            service.bootstrapIfOptedIn()
            advanceUntilIdle()

            verify(exactly = 1) { PlayGamesSdk.initialize(any()) }
            verify(exactly = 0) { signInClient.isAuthenticated() }
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
    fun signInSwallowsTheSignInSpecificCancelCodeAsADeclineToo() =
        runTest(testDispatcher) {
            // WR-08: backing out of the account chooser reports SIGN_IN_CANCELLED (12501),
            // not the generic CANCELED (16). D-10 names over-alerting on a cancel as the worse
            // misclassification, so this code must be just as silent.
            every { signInClient.signIn() } returns
                Tasks.forException(ApiException(Status(GamesConstants.SIGN_IN_CANCELLED_STATUS_CODE)))
            val service = serviceFor(optedIn = false)

            val signedIn = service.signIn()

            assertThat(signedIn).isFalse()
            assertThat(service.session.value).isEqualTo(GamesSignInState.SignedOut)
        }

    @Test
    fun aCancelledSignInIsRethrownUntouchedAndNeverWritesTheSession() =
        runTest(testDispatcher) {
            // WR-04: cancellation is the caller's structured-concurrency signal, not an SDK
            // outcome. It must propagate as itself — not be classified, and not mutate the
            // shared session on its way out.
            every { signInClient.signIn() } returns Tasks.forCanceled()
            val service = serviceFor(optedIn = false)

            val thrown = runCatching { service.signIn() }.exceptionOrNull()

            assertThat(thrown).isInstanceOf(CancellationException::class.java)
            assertThat(service.session.value).isEqualTo(GamesSignInState.Unknown)
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
    fun signInResolvesThePlayerAgainstTheActivityResumedAfterTheChooser() =
        runTest(testDispatcher) {
            // WR-01: the chooser is its own Activity, and a rotation while it is up destroys
            // the instance captured before the await. The player lookups must use whatever is
            // resumed once the round trip is back, or a SUCCESSFUL sign-in reads as a decline.
            val recreated = mockk<Activity>()
            every { activityProvider.current } returns recreated
            val service = serviceFor(optedIn = false)

            val signedIn = service.signIn()

            assertThat(signedIn).isTrue()
            verify(exactly = 1) { PlayGames.getGamesSignInClient(activity) }
            verify(exactly = 2) { PlayGames.getPlayersClient(recreated) }
            verify(exactly = 0) { PlayGames.getPlayersClient(activity) }
        }

    @Test
    fun signInFallsBackToTheCapturedActivityWhileNothingNewerIsResumed() =
        runTest(testDispatcher) {
            // The provider publishes null between a pause and the next resume; in that window
            // the captured instance is still the right (and only) one to use.
            every { activityProvider.current } returns null
            val service = serviceFor(optedIn = false)

            val signedIn = service.signIn()

            assertThat(signedIn).isTrue()
            verify(exactly = 2) { PlayGames.getPlayersClient(activity) }
        }

    @Test
    fun bootstrapResolvesThePlayerAgainstTheActivityResumedAfterTheCheck() =
        runTest(testDispatcher) {
            // The isAuthenticated Task can outlive a rotation just as the chooser can.
            val recreated = mockk<Activity>()
            every { activityProvider.current } returns recreated
            val service = serviceFor(optedIn = true)

            service.bootstrapIfOptedIn()
            advanceUntilIdle()

            assertThat(service.session.value)
                .isEqualTo(GamesSignInState.SignedIn(PLAYER_ID, DISPLAY_NAME))
            verify(exactly = 2) { PlayGames.getPlayersClient(recreated) }
            verify(exactly = 0) { PlayGames.getPlayersClient(activity) }
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

    // ---- submissions: acceptance is a return value, never an exception (PGSL-04) ------

    @Test
    fun submitScoreForASignedInPlayerSendsTheIdAndScoreAndReportsAcceptance() =
        runTest(testDispatcher) {
            val service = signedInServiceInForeground()

            val accepted = service.submitScore(leaderboardId = LEADERBOARD_ID, score = SCORE)

            assertThat(accepted).isTrue()
            verify(exactly = 1) { leaderboardsClient.submitScoreImmediate(LEADERBOARD_ID, SCORE) }
        }

    @Test
    fun aFailedScoreSubmissionIsReportedAsFalseRatherThanThrown() =
        runTest(testDispatcher) {
            every { leaderboardsClient.submitScoreImmediate(any(), any()) } returns
                Tasks.forException(RuntimeException(SDK_FAILURE))
            val service = signedInServiceInForeground()

            // The caller's answer to false is the D-10 pending flag; an exception here would
            // instead propagate into a solved level's completion path (PGSL-04).
            assertThat(service.submitScore(leaderboardId = LEADERBOARD_ID, score = SCORE)).isFalse()
        }

    @Test
    fun aSubmissionWhileSignedOutIsFalseAndNeverReachesTheSdk() =
        runTest(testDispatcher) {
            val service = serviceFor(optedIn = false)
            service.bootstrapIfOptedIn()
            advanceUntilIdle()
            assertThat(service.session.value).isEqualTo(GamesSignInState.SignedOut)

            assertThat(service.submitScore(leaderboardId = LEADERBOARD_ID, score = SCORE)).isFalse()

            verify(exactly = 0) { PlayGames.getLeaderboardsClient(any()) }
        }

    @Test
    fun aSubmissionWhileTheSessionIsStillUnknownIsFalseAndNeverReachesTheSdk() =
        runTest(testDispatcher) {
            // Never bootstrapped, so the first check has not answered. The SDK drops calls
            // made in this window, so optimistically submitting would report a success for a
            // submission that was silently discarded.
            val service = serviceFor(optedIn = true)
            assertThat(service.session.value).isEqualTo(GamesSignInState.Unknown)

            assertThat(service.submitScore(leaderboardId = LEADERBOARD_ID, score = SCORE)).isFalse()

            verify(exactly = 0) { PlayGames.getLeaderboardsClient(any()) }
        }

    @Test
    fun unlockAchievementForASignedInPlayerSendsTheIdAndReportsAcceptance() =
        runTest(testDispatcher) {
            val service = signedInServiceInForeground()

            val accepted = service.unlockAchievement(achievementId = ACHIEVEMENT_ID)

            assertThat(accepted).isTrue()
            verify(exactly = 1) { achievementsClient.unlockImmediate(ACHIEVEMENT_ID) }
        }

    @Test
    fun setAchievementStepsSendsTheAbsoluteValueAndReportsAcceptanceNotTheUnlockedPayload() =
        runTest(testDispatcher) {
            // The SDK's Task<Boolean> payload means "the achievement is now unlocked", not
            // "the submission was accepted". Returning the payload would report failure for
            // every step short of the last one — so a false payload must still yield true.
            every { achievementsClient.setStepsImmediate(any(), any()) } returns Tasks.forResult(false)
            val service = signedInServiceInForeground()

            val accepted = service.setAchievementSteps(achievementId = ACHIEVEMENT_ID, steps = STEPS)

            assertThat(accepted).isTrue()
            verify(exactly = 1) { achievementsClient.setStepsImmediate(ACHIEVEMENT_ID, STEPS) }
        }

    @Test
    fun aSubmissionWithNoForegroundActivityFailsAtOnceRatherThanWaitingForOne() =
        runTest(testDispatcher) {
            // WR-02: `onActivityPaused` nulls the reference, so the ordinary "finish the
            // level, switch apps" pattern leaves every call in a pass with no Activity. This
            // path must NOT await one: at up to ~29 calls per pass, a 10-second wait each
            // would hold `ProgressSubmitter`'s pass mutex for minutes and still produce only
            // `false`s, queueing every later solve behind it. The dirty flag already
            // guarantees the retry, so failing immediately loses nothing.
            val service = signedInService()
            every { activityProvider.current } returns null
            clearMocks(activityProvider, answers = false, recordedCalls = true)
            val startedAt = testScheduler.currentTime

            assertThat(service.submitScore(leaderboardId = LEADERBOARD_ID, score = SCORE)).isFalse()
            assertThat(service.unlockAchievement(achievementId = ACHIEVEMENT_ID)).isFalse()
            assertThat(service.setAchievementSteps(achievementId = ACHIEVEMENT_ID, steps = STEPS)).isFalse()

            // Not one virtual millisecond spent, and the awaiting path never entered.
            assertThat(testScheduler.currentTime).isEqualTo(startedAt)
            coVerify(exactly = 0) { activityProvider.awaitActivity() }
            verify(exactly = 0) { leaderboardsClient.submitScoreImmediate(any(), any()) }
            verify(exactly = 0) { achievementsClient.unlockImmediate(any()) }
            verify(exactly = 0) { achievementsClient.setStepsImmediate(any(), any()) }
        }

    // ---- stock UI: the Intent is obtained and started inside platform/ (PLAT-03) -----

    @Test
    fun showLeaderboardsStartsTheAllLeaderboardsIntentForResult() =
        runTest(testDispatcher) {
            val service = signedInService()

            service.showLeaderboards()

            // startActivityForResult, not startActivity: the SDK requires it so the calling
            // package's identity can be established, and launches the plain way come up broken.
            verify(exactly = 1) { leaderboardsClient.getAllLeaderboardsIntent() }
            verify(exactly = 1) {
                activity.startActivityForResult(stockUiIntent, GamesConstants.LEADERBOARDS_REQUEST_CODE)
            }
        }

    @Test
    fun showAchievementsStartsTheAchievementsIntentForResult() =
        runTest(testDispatcher) {
            val service = signedInService()

            service.showAchievements()

            verify(exactly = 1) { achievementsClient.getAchievementsIntent() }
            verify(exactly = 1) {
                activity.startActivityForResult(stockUiIntent, GamesConstants.ACHIEVEMENTS_REQUEST_CODE)
            }
        }

    @Test
    fun aFailedStockUiIntentLookupIsSilentAndStartsNothing() =
        runTest(testDispatcher) {
            // These getters are documented to fail with a RemoteException. An unreachable
            // cosmetic screen is not something to apologise to the player for.
            every { achievementsClient.getAchievementsIntent() } returns
                Tasks.forException(RuntimeException(SDK_FAILURE))
            val service = signedInService()

            service.showAchievements()

            verify(exactly = 0) { activity.startActivityForResult(any(), any()) }
        }

    @Test
    fun showLeaderboardsStartsNothingWhenNoForegroundActivityEverArrives() =
        runTest(testDispatcher) {
            val service = signedInService()
            coEvery { activityProvider.awaitActivity() } coAnswers { awaitCancellation() }

            // Bounded by ACTIVITY_WAIT_MS, which virtual time elapses instantly: a
            // backgrounded tap gives up rather than hanging the process-lifetime singleton.
            service.showLeaderboards()

            verify(exactly = 0) { activity.startActivityForResult(any(), any()) }
        }

    /**
     * A service whose [GamesService.session] has resolved to [GamesSignInState.SignedIn] —
     * the precondition every submission is gated on. Driven through the real bootstrap
     * rather than injected, so the tests exercise the same path production takes.
     */
    private fun TestScope.signedInService(): PlayGamesService {
        val service = serviceFor(optedIn = true)
        service.bootstrapIfOptedIn()
        advanceUntilIdle()
        return service
    }

    /**
     * A [signedInService] with an Activity actually resumed in front of the player.
     *
     * Submissions read [ForegroundActivityProvider.current] and never await one (WR-02), so
     * this — rather than [signedInService] alone — is the precondition for a submission that
     * reaches the SDK. The class-wide default leaves `current` null, which is exactly the
     * backgrounded case the fast-fail test below pins.
     */
    private fun TestScope.signedInServiceInForeground(): PlayGamesService {
        val service = signedInService()
        every { activityProvider.current } returns activity
        return service
    }

    private fun serviceFor(optedIn: Boolean) =
        PlayGamesService(
            appContext = appContext,
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
 * Obviously-fake ids and values. No real Play Console id belongs in source: this seam takes
 * ids as opaque parameters, and the real ones are resolved at build time (D-13).
 */
private const val LEADERBOARD_ID = "placeholder-leaderboard-id"
private const val ACHIEVEMENT_ID = "placeholder-achievement-id"
private const val SCORE = 1_200L
private const val STEPS = 7

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
