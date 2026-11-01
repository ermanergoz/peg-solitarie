package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.ui.home.fixture.BOARDS_PILL
import com.app.pegsolitaire.ui.home.fixture.CLEARED_BOARDS
import com.app.pegsolitaire.ui.home.fixture.CONTINUE_LABEL
import com.app.pegsolitaire.ui.home.fixture.EARNED_STARS
import com.app.pegsolitaire.ui.home.fixture.HOME_ROBOLECTRIC_SDK
import com.app.pegsolitaire.ui.home.fixture.LEVEL_PILL
import com.app.pegsolitaire.ui.home.fixture.PERSISTED_LEVEL
import com.app.pegsolitaire.ui.home.fixture.RESUMABLE_LEVEL_ID
import com.app.pegsolitaire.ui.home.fixture.RESUME_CHALLENGE_LABEL
import com.app.pegsolitaire.ui.home.fixture.RESUME_CLASSIC_LABEL
import com.app.pegsolitaire.ui.home.fixture.RIBBON_PILLS
import com.app.pegsolitaire.ui.home.fixture.STAMPED_RESUME_LEVEL
import com.app.pegsolitaire.ui.home.fixture.STARS_PILL
import com.app.pegsolitaire.ui.home.fixture.announcement
import com.app.pegsolitaire.ui.home.fixture.boardsLabel
import com.app.pegsolitaire.ui.home.fixture.boardsValue
import com.app.pegsolitaire.ui.home.fixture.challengeTileTitle
import com.app.pegsolitaire.ui.home.fixture.classicTileTitle
import com.app.pegsolitaire.ui.home.fixture.levelLabel
import com.app.pegsolitaire.ui.home.fixture.pillClickAction
import com.app.pegsolitaire.ui.home.fixture.pillDescriptions
import com.app.pegsolitaire.ui.home.fixture.renderHome
import com.app.pegsolitaire.ui.home.fixture.renderedValues
import com.app.pegsolitaire.ui.home.fixture.ribbonPillCount
import com.app.pegsolitaire.ui.home.fixture.starsLabel
import com.app.pegsolitaire.ui.home.fixture.string
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
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
 *
 * Home's **geometry** — the vertical slack and the two window classes, criteria 15g-15m — is
 * [HomeLayoutTest]. Both classes render through the same [renderHome] fixture in
 * `HomeTestSupport.kt`, so they cannot disagree about the screen they describe.
 *
 * These tests run on Robolectric's default 320x470dp window, which is **narrow and short**
 * (§4.2.3), so the ribbon assertions below read the compact pill. That is deliberate and it is
 * half of criterion 15m's evidence: the same three announcements are asserted here on the compact
 * pill and in [HomeLayoutTest.theFullPillSpeaksTheSameSentenceTheCompactPillDoes] on the full one,
 * from the same `announcement()` oracle.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [HOME_ROBOLECTRIC_SDK])
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun challengeTileShowsThePersistedLevelAndEntersAtIt() {
        var enteredLevel: Int? = null
        composeRule.renderHome(
            state = HomeUiState(challengeCurrentLevel = PERSISTED_LEVEL),
            onPlayChallenge = { level -> enteredLevel = level },
        )

        // The tile is labelled with the persisted level, verbatim from strings.xml.
        composeRule.onNodeWithText(CONTINUE_LABEL).assertExists()

        // ...and tapping it enters Challenge at that same level, not a hardcoded 1.
        composeRule.onNodeWithText(CONTINUE_LABEL).performScrollTo().performClick()
        composeRule.waitForIdle()
        assertThat(enteredLevel).isEqualTo(PERSISTED_LEVEL)
    }

    @Test
    fun noContinueAffordanceRendersWhenNothingIsSaved() {
        composeRule.renderHome(state = HomeUiState(challengeCurrentLevel = PERSISTED_LEVEL))

        composeRule.onNodeWithText(RESUME_CLASSIC_LABEL).assertDoesNotExist()
        composeRule.onNodeWithText(RESUME_CHALLENGE_LABEL).assertDoesNotExist()
    }

    @Test
    fun aSavedClassicGameOffersAContinueThatResumesItsOwnBoard() {
        var resumedBoard: String? = null
        composeRule.renderHome(
            state = HomeUiState(classicResumeLevelId = RESUMABLE_LEVEL_ID),
            onResumeClassic = { levelId -> resumedBoard = levelId },
        )

        composeRule.onNodeWithText(RESUME_CLASSIC_LABEL).performScrollTo().performClick()
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
        composeRule.renderHome(
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

        composeRule.onNodeWithText(RESUME_CHALLENGE_LABEL).performScrollTo().performClick()
        composeRule.waitForIdle()
        assertThat(resumedLevel).isEqualTo(STAMPED_RESUME_LEVEL)
    }

    // ---- The progress ribbon (D-03c/D-03h, §4.2 band 2) ----------------------

    @Test
    fun aRibbonPillWithNoValueYetShowsTheUnknownPlaceholder() {
        // Criterion 13, both halves: Home paints before any read lands, so the ribbon's first
        // frame shows "—" — never 0, never blank, never a spinner — and announces it as "not
        // yet available" rather than reading an em dash out as punctuation.
        composeRule.renderHome(state = HomeUiState())

        assertThat(composeRule.renderedValues()).contains(unknownValue)
        assertThat(
            composeRule.pillDescriptions(STARS_PILL),
        ).containsExactly(announcement(starsLabel, unknownDescription))
        assertThat(
            composeRule.pillDescriptions(BOARDS_PILL),
        ).containsExactly(announcement(boardsLabel, unknownDescription))
    }

    @Test
    fun aRibbonPillWhoseReadFailedAlsoShowsThePlaceholderAndNeverZero() {
        // Criterion 13a, and the half that carries the weight: a player with stars already
        // earned must never be told their total is zero because a read threw. The ViewModel
        // maps a failed read to the same `null` "not yet" carries, so the two are
        // indistinguishable on screen BY CONSTRUCTION — which is only safe because zero and
        // unknown stay distinguishable from each other, asserted here in both directions and
        // across one live recomposition, which is how the real fill arrives.
        val state = mutableStateOf(HomeUiState())
        composeRule.renderHome(state = state)

        assertThat(composeRule.renderedValues()).contains(unknownValue)
        assertThat(composeRule.renderedValues()).doesNotContain(ZERO_VALUE)

        // A brand-new player genuinely has zero, and that IS a number, spoken as one.
        state.value = HomeUiState(totalStars = 0, classicBoardsCleared = 0)
        composeRule.waitForIdle()

        assertThat(composeRule.renderedValues()).contains(ZERO_VALUE)
        assertThat(composeRule.renderedValues()).doesNotContain(unknownValue)
        assertThat(composeRule.pillDescriptions(STARS_PILL)).containsExactly(announcement(starsLabel, ZERO_VALUE))
    }

    @Test
    fun theBoardsPillDenominatorIsTheClassicCatalogueSize() {
        // Criterion 13b, and prohibition 5's Home half: the denominator is a compile-time
        // fact, so no stored row — and no tutorial board joining CLASSIC_SPECS — can change
        // a shipped number. The expected text is built from the catalogue itself, so this
        // test tracks a sixth board instead of pinning a stale 5.
        composeRule.renderHome(state = HomeUiState(classicBoardsCleared = CLEARED_BOARDS))

        assertThat(composeRule.renderedValues()).contains(boardsValue)
        assertThat(composeRule.pillDescriptions(BOARDS_PILL))
            .containsExactly(announcement(boardsLabel, boardsValue))
        assertThat(Boards.CLASSIC_SPECS).hasSize(CLASSIC_CATALOGUE_SIZE)
    }

    @Test
    fun theLevelPillAndTheChallengeTileShowTheSameNumber() {
        // Criteria 12 / 15b. Both read `challengeCurrentLevel`, so they cannot diverge by
        // construction; what this pins is that both are WIRED to it. The tile chip is the
        // cloud-save oracle and the pill is additive — if they ever disagree the tile is right.
        composeRule.renderHome(state = HomeUiState(challengeCurrentLevel = PERSISTED_LEVEL))

        assertThat(composeRule.renderedValues()).contains(PERSISTED_LEVEL.toString())
        assertThat(composeRule.pillDescriptions(LEVEL_PILL))
            .containsExactly(announcement(levelLabel, PERSISTED_LEVEL.toString()))
        composeRule.onNodeWithText(CONTINUE_LABEL).assertExists()
    }

    @Test
    fun eachRibbonPillSpeaksAsOneNodeSayingItsLabelThenItsValue() {
        // "Stars, 7", never a bare number: each pill announces once, label first, and the
        // ribbon is three nodes rather than nine. Asserted as the exact spoken string, so a
        // future refactor that merged descendants instead — and thereby dropped the label from
        // TalkBack's reading — fails here rather than only on a device.
        composeRule.renderHome(
            state =
                HomeUiState(
                    totalStars = EARNED_STARS,
                    classicBoardsCleared = CLEARED_BOARDS,
                    challengeCurrentLevel = PERSISTED_LEVEL,
                ),
        )

        assertThat(composeRule.ribbonPillCount()).isEqualTo(RIBBON_PILLS)
        assertThat(composeRule.pillDescriptions(STARS_PILL))
            .containsExactly(announcement(starsLabel, EARNED_STARS.toString()))
        assertThat(composeRule.pillDescriptions(LEVEL_PILL))
            .containsExactly(announcement(levelLabel, PERSISTED_LEVEL.toString()))
    }

    @Test
    fun theProgressPillsAreNotTappable() {
        // §5's negative contract. The pills are status, not navigation: three more competing
        // targets is the opposite of what the founder asked for, and a tappable pill would
        // also make the ribbon a fourth thing to reason about on the busiest screen.
        composeRule.renderHome(state = HomeUiState(totalStars = EARNED_STARS))

        repeat(RIBBON_PILLS) { index ->
            assertThat(composeRule.pillClickAction(index)).isNull()
        }
    }

    @Test
    fun bothContinueChipsAndBothModeTilesRenderWhenBothResumesExist() {
        // The D-03f oracle-3 guard at unit level: the busiest legitimate Home — two Continue
        // chips AND two mode tiles — renders all four. Whether all four are within the first
        // viewport at default font scale on a 360x640dp phone is criterion 15a, a DEVICE
        // check: this rule's viewport is 320x470px, so it can prove presence and not the fold.
        composeRule.renderHome(
            state =
                HomeUiState(
                    challengeCurrentLevel = PERSISTED_LEVEL,
                    classicResumeLevelId = RESUMABLE_LEVEL_ID,
                    classicResumeBoard = Boards.european37Initial,
                    challengeResumeLevel = STAMPED_RESUME_LEVEL,
                ),
        )

        composeRule.onNodeWithText(RESUME_CLASSIC_LABEL).assertExists()
        composeRule.onNodeWithText(RESUME_CHALLENGE_LABEL).assertExists()
        composeRule.onNodeWithText(classicTileTitle).assertExists()
        composeRule.onNodeWithText(challengeTileTitle).assertExists()
    }

    // ---- Play Games opt-in surfaces (PGS-01, D-15/D-18) ----------------------

    @Test
    fun theOptInPromptIsHostedWhenTheStateRaisesIt() {
        composeRule.renderHome(state = HomeUiState(playGamesPromptVisible = true))

        composeRule.onNodeWithTag(PLAY_GAMES_PROMPT_DIALOG_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun noOptInPromptIsHostedWhenTheQuestionIsNotBeingAsked() {
        // The overwhelmingly common case: already answered, or onboarding still up.
        composeRule.renderHome(state = HomeUiState())

        composeRule.onNodeWithTag(PLAY_GAMES_PROMPT_DIALOG_TEST_TAG).assertDoesNotExist()
    }

    // ---- The account-switch dialog (CLOUD-02, D-12 to D-15) ------------------

    @Test
    fun theProfileSwapDialogIsHostedWhenTheStateRaisesIt() {
        composeRule.renderHome(
            state = HomeUiState(profileSwapVisible = true, profileSwapAccountName = ARRIVING_ACCOUNT_NAME),
        )

        composeRule.onNodeWithTag(PROFILE_SWAP_DIALOG_TEST_TAG).assertIsDisplayed()
    }

    @Test
    fun theHostedDialogCarriesTheAccountNameTheStateHandsIt() {
        // The screen never invents the name and never guesses it (the classicResumeLevelId
        // convention): what the state carries is what the player reads.
        composeRule.renderHome(
            state = HomeUiState(profileSwapVisible = true, profileSwapAccountName = ARRIVING_ACCOUNT_NAME),
        )

        val body = string(R.string.profile_swap_body, ARRIVING_ACCOUNT_NAME)
        composeRule.onNodeWithText(body).assertIsDisplayed()
    }

    @Test
    fun noProfileSwapDialogIsHostedWhenNoAccountSwitchIsPending() {
        // The overwhelmingly common case: one account, nothing to ask.
        composeRule.renderHome(state = HomeUiState())

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
        composeRule.renderHome(state = HomeUiState(tutorialPending = true, profileSwapVisible = true))

        composeRule.onNodeWithTag(PROFILE_SWAP_DIALOG_TEST_TAG).assertDoesNotExist()
    }

    /** The other side of the same guard: once the tutorial is behind the player, it is raised. */
    @Test
    fun theProfileSwapDialogIsRaisedOnceTheTutorialIsNoLongerPending() {
        composeRule.renderHome(
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
        composeRule.renderHome(state = HomeUiState(playGamesPromptVisible = true, profileSwapVisible = true))

        composeRule.onNodeWithTag(PLAY_GAMES_PROMPT_DIALOG_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PROFILE_SWAP_DIALOG_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun keepingThisDevicesProgressReachesTheHostedCallback() {
        // The invisible-feature trap once more: the dialog could render perfectly and
        // forward its answer nowhere, leaving the pass blocked on a question the player
        // believes they have answered (D-15 — no push and no pull until it is answered).
        var keepCount = 0
        composeRule.renderHome(
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
        composeRule.renderHome(
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
        composeRule.renderHome(state = HomeUiState(playGamesSignInErrorTick = 0))

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
}

/** The first genuine sign-in error of a session — the tick's first meaningful value. */
private const val FIRST_ERROR_TICK = 1

/** The arriving account's display name for the swap dialog (D-12). */
private const val ARRIVING_ACCOUNT_NAME = "PegChampion"

/** The rendered supporting copy for [STAMPED_RESUME_LEVEL] (verbatim from `strings.xml`). */
private const val STAMPED_RESUME_SUPPORTING = "Pick up Level 9 where you left off"

/** The shipped Classic catalogue size, asserted against `Boards.CLASSIC_SPECS` itself. */
private const val CLASSIC_CATALOGUE_SIZE = 5

/** What a genuinely-zero pill renders — a real answer, and never the placeholder. */
private const val ZERO_VALUE = "0"

private val unknownValue: String get() = string(R.string.stat_value_unknown)

private val unknownDescription: String get() = string(R.string.stat_value_unknown_desc)

/**
 * The D-18 failure copy, read from `strings.xml` rather than retyped: this test's claim is that
 * the message REACHES the player, not what it says (the wording itself is D-10/D-18's, shared
 * with the Settings row, and is asserted where it is authored).
 */
private val signInErrorMessage: String get() = string(R.string.play_games_signin_error)

/** The two answer labels, read from `strings.xml` — the copy is asserted where it is authored. */
private val keepLabel: String get() = string(R.string.profile_swap_keep)

private val useCloudLabel: String get() = string(R.string.profile_swap_use_cloud)
