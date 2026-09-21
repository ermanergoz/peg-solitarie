package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.ClassicLevelIds
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Render proof that Home actually *shows and uses* what its state carries — the half a
 * ViewModel assertion cannot cover.
 *
 * Without this, `HomeUiState.challengeCurrentLevel` and the PERS-03 resume flags could be
 * collected correctly and still never reach the screen (the "invisible feature" trap): the
 * Challenge tile would read "Level 1" forever, and a saved game would offer no way back
 * into it however faithfully the snapshot was stored.
 *
 * The same trap applies to the Play Games opt-in surfaces (PGS-01, D-15/D-18): the
 * ViewModel could sequence the prompt perfectly and bump the error tick faithfully while
 * the screen hosted neither, so the player is never asked and never told sign-in failed.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HOME_ROBOLECTRIC_SDK])
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun challengeTileShowsThePersistedLevelAndEntersAtIt() {
        var enteredLevel: Int? = null
        renderHome(
            state = HomeUiState(challengeCurrentLevel = PERSISTED_LEVEL),
            onPlayChallenge = { level -> enteredLevel = level },
        )

        // The tile is labelled with the persisted level, verbatim from strings.xml.
        composeRule.onNodeWithText(CONTINUE_LABEL).assertExists()

        // ...and tapping it enters Challenge at that same level, not a hardcoded 1.
        composeRule.onNodeWithText(CONTINUE_LABEL).performClick()
        composeRule.waitForIdle()
        assertThat(enteredLevel).isEqualTo(PERSISTED_LEVEL)
    }

    @Test
    fun noContinueAffordanceRendersWhenNothingIsSaved() {
        renderHome(state = HomeUiState(challengeCurrentLevel = PERSISTED_LEVEL))

        composeRule.onNodeWithText(RESUME_CLASSIC_LABEL).assertDoesNotExist()
        composeRule.onNodeWithText(RESUME_CHALLENGE_LABEL).assertDoesNotExist()
    }

    @Test
    fun aSavedClassicGameOffersAContinueThatResumesItsOwnBoard() {
        var resumedBoard: String? = null
        renderHome(
            state = HomeUiState(classicResumeLevelId = RESUMABLE_LEVEL_ID),
            onResumeClassic = { levelId -> resumedBoard = levelId },
        )

        composeRule.onNodeWithText(RESUME_CLASSIC_LABEL).performClick()
        composeRule.waitForIdle()

        assertThat(resumedBoard).isEqualTo(RESUMABLE_LEVEL_ID)
    }

    @Test
    fun bothContinueAffordancesRenderTogetherAndResumeTheirOwnMode() {
        // D-03: a Classic and a Challenge game coexist, so Home shows both entries.
        // The Challenge Continue deliberately diverges from the ladder level here: the
        // tile must label and resume at the SNAPSHOT's stamped level (WR-03), never at
        // challengeCurrentLevel (which can lag at a cold start).
        var resumedLevel: Int? = null
        renderHome(
            state =
                HomeUiState(
                    challengeCurrentLevel = PERSISTED_LEVEL,
                    classicResumeLevelId = RESUMABLE_LEVEL_ID,
                    challengeResumeLevel = STAMPED_RESUME_LEVEL,
                ),
            onResumeChallenge = { level -> resumedLevel = level },
        )

        composeRule.onNodeWithText(RESUME_CLASSIC_LABEL).assertExists()
        composeRule.onNodeWithText(RESUME_CHALLENGE_LABEL).assertExists()
        composeRule.onNodeWithText(STAMPED_RESUME_SUPPORTING).assertExists()

        composeRule.onNodeWithText(RESUME_CHALLENGE_LABEL).performClick()
        composeRule.waitForIdle()
        assertThat(resumedLevel).isEqualTo(STAMPED_RESUME_LEVEL)
    }

    // ---- Play Games opt-in surfaces (PGS-01, D-15/D-18) ----------------------

    @Test
    fun theOptInPromptIsHostedWhenTheStateRaisesIt() {
        renderHome(state = HomeUiState(playGamesPromptVisible = true))

        composeRule.onNodeWithTag(PLAY_GAMES_PROMPT_DIALOG_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun noOptInPromptIsHostedWhenTheQuestionIsNotBeingAsked() {
        // The overwhelmingly common case: already answered, or onboarding still up.
        renderHome(state = HomeUiState())

        composeRule.onNodeWithTag(PLAY_GAMES_PROMPT_DIALOG_TEST_TAG).assertDoesNotExist()
    }

    // ---- The account-switch dialog (CLOUD-02, D-12 to D-15) ------------------

    @Test
    fun theProfileSwapDialogIsHostedWhenTheStateRaisesIt() {
        renderHome(state = HomeUiState(profileSwapVisible = true, profileSwapAccountName = ARRIVING_ACCOUNT_NAME))

        composeRule.onNodeWithTag(PROFILE_SWAP_DIALOG_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun theHostedDialogCarriesTheAccountNameTheStateHandsIt() {
        // The screen never invents the name and never guesses it (the classicResumeLevelId
        // convention): what the state carries is what the player reads.
        renderHome(state = HomeUiState(profileSwapVisible = true, profileSwapAccountName = ARRIVING_ACCOUNT_NAME))

        val body = RuntimeEnvironment.getApplication().getString(R.string.profile_swap_body, ARRIVING_ACCOUNT_NAME)
        composeRule.onNodeWithText(body).assertIsDisplayed()
    }

    @Test
    fun noProfileSwapDialogIsHostedWhenNoAccountSwitchIsPending() {
        // The overwhelmingly common case: one account, nothing to ask.
        renderHome(state = HomeUiState())

        composeRule.onNodeWithTag(PROFILE_SWAP_DIALOG_TEST_TAG).assertDoesNotExist()
    }

    /**
     * The retarget of the never-stacked test that used to locate the retired first-run sheet by
     * its "Got it" button copy (ux-spec §13; the old name is in this commit's diff).
     *
     * D-15's rule is unchanged — the wipe question may be **withheld**, never raised early —
     * but the surface it is sequenced behind is now a destination rather than an overlay Home
     * hosts, so the screen has nothing of the tutorial's to find. The pending state is read
     * from the **state field** [HomeUiState.tutorialPending], and the dialog from its
     * `PROFILE_SWAP_DIALOG_TEST_TAG`: the old test located the tutorial by the literal
     * "Got it", which is exactly the find-a-screen-by-its-copy defect this retarget removes
     * rather than preserves.
     */
    @Test
    fun theProfileSwapDialogIsNeverRaisedWhileTheTutorialIsStillPending() {
        renderHome(state = HomeUiState(tutorialPending = true, profileSwapVisible = true))

        composeRule.onNodeWithTag(PROFILE_SWAP_DIALOG_TEST_TAG).assertDoesNotExist()
    }

    /** The other side of the same guard: once the tutorial is behind the player, it is raised. */
    @Test
    fun theProfileSwapDialogIsRaisedOnceTheTutorialIsNoLongerPending() {
        renderHome(
            state =
                HomeUiState(
                    tutorialPending = false,
                    profileSwapVisible = true,
                    profileSwapAccountName = ARRIVING_ACCOUNT_NAME,
                ),
        )

        composeRule.onNodeWithTag(PROFILE_SWAP_DIALOG_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun theProfileSwapDialogIsNeverStackedOnTheOptInPrompt() {
        // D-15 again, and the more dangerous pairing: two dialogs at once, the lower of
        // which wipes the device. The opt-in prompt wins — it is the question that decides
        // whether Play Games is involved at all.
        renderHome(state = HomeUiState(playGamesPromptVisible = true, profileSwapVisible = true))

        composeRule.onNodeWithTag(PLAY_GAMES_PROMPT_DIALOG_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PROFILE_SWAP_DIALOG_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun keepingThisDevicesProgressReachesTheHostedCallback() {
        // The invisible-feature trap once more: the dialog could render perfectly and
        // forward its answer nowhere, leaving the pass blocked on a question the player
        // believes they have answered (D-15 — no push and no pull until it is answered).
        var keepCount = 0
        renderHome(
            state = HomeUiState(profileSwapVisible = true, profileSwapAccountName = ARRIVING_ACCOUNT_NAME),
            onKeepDeviceProgress = { keepCount++ },
        )

        composeRule.onNodeWithText(keepLabel).performClick()
        composeRule.waitForIdle()

        assertThat(keepCount).isEqualTo(1)
    }

    @Test
    fun usingTheCloudProgressReachesItsOwnHostedCallback() {
        // The two callbacks must not be crossed: this one wipes the device.
        var keepCount = 0
        var useCloudCount = 0
        renderHome(
            state = HomeUiState(profileSwapVisible = true, profileSwapAccountName = ARRIVING_ACCOUNT_NAME),
            onKeepDeviceProgress = { keepCount++ },
            onUseCloudProgress = { useCloudCount++ },
        )

        composeRule.onNodeWithText(useCloudLabel).performClick()
        composeRule.waitForIdle()

        assertThat(useCloudCount).isEqualTo(1)
        assertThat(keepCount).isEqualTo(0)
    }

    @Test
    fun aSignInErrorTickRaisedWhileOnHomeShowsTheSnackbar() {
        // D-18: the error the ViewModel records must actually reach the player. Driven
        // through a real recomposition (0 -> 1) rather than an already-raised initial
        // state, because that is the live sequence — the player taps "Sign in", the SDK
        // fails, and the tick moves under a screen that is already composed.
        val state = mutableStateOf(HomeUiState())
        composeRule.setContent {
            HomeScreen(
                state = state.value,
                onPlayClassic = {},
                onPlayChallenge = {},
                onResumeClassic = {},
                onResumeChallenge = {},
                onAcceptPlayGamesPrompt = {},
                onDeclinePlayGamesPrompt = {},
                onKeepDeviceProgress = {},
                onUseCloudProgress = {},
                onPlayGamesSignInErrorShown = {},
                onOpenSettings = {},
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(signInErrorMessage).assertDoesNotExist()

        state.value = state.value.copy(playGamesSignInErrorTick = FIRST_ERROR_TICK)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(signInErrorMessage).assertIsDisplayed()
    }

    @Test
    fun aSignInErrorTickOfZeroIsNeverShown() {
        // The phantom-cue guard (the T-09-40 shape, on Home this time): a freshly composed
        // Home sits at tick 0 — and, once consumed, RETURNS to 0 (CR-01). Show on 0 and every
        // visit to Home opens by apologising for a sign-in the player never attempted.
        renderHome(state = HomeUiState(playGamesSignInErrorTick = 0))

        composeRule.onNodeWithText(signInErrorMessage).assertDoesNotExist()
    }

    @Test
    fun aShownSignInErrorIsConsumedAndDoesNotReplayWhenHomeReEntersComposition() {
        // CR-01: HomeViewModel outlives every visit to Home, so the screen must hand the
        // ordinal back the moment it shows it. The callback here plays the ViewModel's part
        // (resetting the tick); leaving and re-entering composition plays a forward
        // navigation and its pop. The second visit must be silent.
        val state = mutableStateOf(HomeUiState(playGamesSignInErrorTick = FIRST_ERROR_TICK))
        val hosted = mutableStateOf(true)
        var shownCount = 0
        composeRule.setContent {
            if (hosted.value) {
                HomeScreen(
                    state = state.value,
                    onPlayClassic = {},
                    onPlayChallenge = {},
                    onResumeClassic = {},
                    onResumeChallenge = {},
                    onAcceptPlayGamesPrompt = {},
                    onDeclinePlayGamesPrompt = {},
                    onKeepDeviceProgress = {},
                    onUseCloudProgress = {},
                    onPlayGamesSignInErrorShown = {
                        shownCount++
                        state.value = state.value.copy(playGamesSignInErrorTick = 0)
                    },
                    onOpenSettings = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(signInErrorMessage).assertIsDisplayed()
        assertThat(shownCount).isEqualTo(1)

        hosted.value = false
        composeRule.waitForIdle()
        hosted.value = true
        composeRule.waitForIdle()

        composeRule.onNodeWithText(signInErrorMessage).assertDoesNotExist()
        assertThat(shownCount).isEqualTo(1)
    }

    private fun renderHome(
        state: HomeUiState,
        onPlayChallenge: (Int) -> Unit = {},
        onResumeClassic: (String) -> Unit = {},
        onResumeChallenge: (Int) -> Unit = {},
        onKeepDeviceProgress: () -> Unit = {},
        onUseCloudProgress: () -> Unit = {},
    ) {
        composeRule.setContent {
            HomeScreen(
                state = state,
                onPlayClassic = {},
                onPlayChallenge = onPlayChallenge,
                onResumeClassic = onResumeClassic,
                onResumeChallenge = onResumeChallenge,
                onAcceptPlayGamesPrompt = {},
                onDeclinePlayGamesPrompt = {},
                onKeepDeviceProgress = onKeepDeviceProgress,
                onUseCloudProgress = onUseCloudProgress,
                onPlayGamesSignInErrorShown = {},
                onOpenSettings = {},
            )
        }
        composeRule.waitForIdle()
    }

    /**
     * The D-18 failure copy, read from `strings.xml` rather than retyped: this test's claim
     * is that the message REACHES the player, not what it says (the wording itself is
     * D-10/D-18's, shared with the Settings row, and is asserted where it is authored).
     */
    private val signInErrorMessage: String
        get() = RuntimeEnvironment.getApplication().getString(R.string.play_games_signin_error)

    /** The two answer labels, read from `strings.xml` — the copy is asserted where it is authored. */
    private val keepLabel: String
        get() = RuntimeEnvironment.getApplication().getString(R.string.profile_swap_keep)

    private val useCloudLabel: String
        get() = RuntimeEnvironment.getApplication().getString(R.string.profile_swap_use_cloud)
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val HOME_ROBOLECTRIC_SDK = 34

/** A persisted level past the on-ramp, so a hardcoded 1 cannot pass by accident. */
private const val PERSISTED_LEVEL = 7

/** The rendered tile copy for [PERSISTED_LEVEL] (verbatim from `strings.xml`). */
private const val CONTINUE_LABEL = "Continue — Level 7"

/** The Classic board a saved game sits on — carried into the resume by the tile. */
private val RESUMABLE_LEVEL_ID = ClassicLevelIds.EUROPEAN37

/** The rendered Continue-affordance titles (verbatim from `strings.xml`). */
private const val RESUME_CLASSIC_LABEL = "Continue Classic"
private const val RESUME_CHALLENGE_LABEL = "Continue Challenge"

/** The snapshot's stamped level — deliberately NOT the ladder level (WR-03). */
private const val STAMPED_RESUME_LEVEL = 9

/** The rendered supporting copy for [STAMPED_RESUME_LEVEL] (verbatim from `strings.xml`). */
private const val STAMPED_RESUME_SUPPORTING = "Pick up Level 9 where you left off"

/** The first genuine sign-in error of a session — the tick's first meaningful value. */
private const val FIRST_ERROR_TICK = 1

/** The arriving account's display name for the swap dialog (D-12). */
private const val ARRIVING_ACCOUNT_NAME = "PegChampion"
