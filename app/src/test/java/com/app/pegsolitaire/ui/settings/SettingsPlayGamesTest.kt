package com.app.pegsolitaire.ui.settings

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.app.pegsolitaire.R
import com.app.pegsolitaire.platform.games.GamesSignInState
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Robolectric + Compose UI coverage of the Settings "Play Games" section (PGS-02/PGS-03;
 * D-06/D-08/D-09/D-10/D-11/D-13/D-14).
 *
 * Modelled directly on [SettingsPrivacyTest]: the screen is a stateless render of
 * [SettingsUiState] plus hoisted handlers, so every assertion here is a pure function of the
 * session pushed in, and rows are located by their string resources rather than by test tags.
 *
 * The claims that actually carry weight are the *absence* ones. D-11 forbids the section
 * outright while the session is [GamesSignInState.Unknown], and D-06 replaces the sign-in row
 * rather than adding to it — so "the header is gone", "the row is gone" and "the sign-in row
 * is gone once signed in" are asserted as their own tests instead of being inferred from the
 * populated states passing.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [PLAY_GAMES_ROBOLECTRIC_SDK])
class SettingsPlayGamesTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()

    private val sectionHeader get() = app.getString(R.string.settings_section_play_games)
    private val signInLabel get() = app.getString(R.string.settings_play_games_signin_label)
    private val supporting get() = app.getString(R.string.settings_play_games_signin_supporting)
    private val signedInFallback get() = app.getString(R.string.settings_play_games_signed_in_fallback)
    private val signInError get() = app.getString(R.string.play_games_signin_error)

    private fun signedInAs(name: String) = app.getString(R.string.settings_play_games_signed_in_as, name)

    /** Renders the screen over the given session; handlers default to no-ops. */
    private fun setContent(
        gamesSignInState: GamesSignInState,
        onSignInToPlayGames: () -> Unit = {},
    ) {
        composeRule.setContent {
            AppTheme {
                SettingsScreen(
                    state = SettingsUiState(gamesSignInState = gamesSignInState),
                    onToggleSound = {},
                    onToggleHaptics = {},
                    onSignInToPlayGames = onSignInToPlayGames,
                    onPlayGamesSignInErrorShown = {},
                    onManagePrivacyChoices = {},
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    // ---- Unknown: absent entirely (D-11) --------------------------------------

    @Test
    fun theWholeSectionIsAbsentWhileTheSessionIsUnknown() {
        // D-11 in full: not an empty section, not a disabled row, not a spinner — nothing.
        setContent(GamesSignInState.Unknown)

        composeRule.onNodeWithText(sectionHeader).assertDoesNotExist()
        composeRule.onNodeWithText(signInLabel).assertDoesNotExist()
        composeRule.onNodeWithText(supporting).assertDoesNotExist()
    }

    @Test
    fun theOtherTwoSectionsStillRenderWhileThePlayGamesSectionIsHidden() {
        // The absent section must take nothing with it: Settings is fully usable for a
        // player whose sign-in state never resolves (PGS-02 — nothing broken).
        setContent(GamesSignInState.Unknown)

        composeRule.onNodeWithText(app.getString(R.string.settings_section_general)).assertIsDisplayed()
        composeRule
            .onNodeWithText(app.getString(R.string.settings_section_privacy))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(app.getString(R.string.settings_privacy_policy_label))
            .performScrollTo()
            .assertIsDisplayed()
    }

    // ---- SignedOut: the manual sign-in row (D-09/D-10) -------------------------

    @Test
    fun theSignInRowShowsWithItsExactCopyWhileSignedOut() {
        // D-09 fixes both lines verbatim; this is the assertion that catches a reword.
        setContent(GamesSignInState.SignedOut)

        composeRule.onNodeWithText(sectionHeader).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(signInLabel).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(supporting).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun theSignInRowIsAnActionableControl() {
        // The counterpart of the signed-in assertion below: the offer really is a control.
        setContent(GamesSignInState.SignedOut)

        composeRule.onNodeWithText(signInLabel).performScrollTo().assertHasClickAction()
    }

    @Test
    fun tappingTheSignInRowInvokesItsHandlerExactlyOnce() {
        var signInCount = 0
        setContent(GamesSignInState.SignedOut, onSignInToPlayGames = { signInCount++ })

        composeRule.onNodeWithText(signInLabel).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertThat(signInCount).isEqualTo(1)
    }

    @Test
    fun thePlayGamesSectionSitsBetweenGeneralAndPrivacy() {
        // D-08's ordering, asserted through the layout itself rather than the source: the
        // three headers' top coordinates must increase in the documented order, with
        // Privacy still last (Phase-9 D-14 pins it to the bottom).
        setContent(GamesSignInState.SignedOut)

        val generalTop = topOf(app.getString(R.string.settings_section_general))
        val playGamesTop = topOf(sectionHeader)
        val privacyTop = topOf(app.getString(R.string.settings_section_privacy))

        assertThat(generalTop).isLessThan(playGamesTop)
        assertThat(playGamesTop).isLessThan(privacyTop)
    }

    /** The laid-out top edge of the node carrying [text], in root coordinates. */
    private fun topOf(text: String): Float =
        composeRule
            .onNodeWithText(text)
            .fetchSemanticsNode()
            .positionInRoot
            .y

    // ---- SignedIn: identity replaces the offer (D-06/D-14) ---------------------

    @Test
    fun aSignedInPlayerSeesTheirNameInsteadOfTheSignInRow() {
        setContent(GamesSignInState.SignedIn(playerId = PLAYER_ID, displayName = DISPLAY_NAME))

        composeRule.onNodeWithText(signedInAs(DISPLAY_NAME)).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(signInLabel).assertDoesNotExist()
    }

    @Test
    fun aMissingDisplayNameFallsBackToTheNeutralSignedInCopy() {
        // D-14: the name lookup failed, the sign-in did not. The row must still say so.
        setContent(GamesSignInState.SignedIn(playerId = PLAYER_ID, displayName = null))

        composeRule.onNodeWithText(signedInFallback).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(signInLabel).assertDoesNotExist()
    }

    @Test
    fun theSignedInRowIsNotAnOfferToSignInAgain() {
        // D-06's "non-tappable" claim, asserted two ways: the node carries no click action at
        // all (WR-06 — so TalkBack does not announce "double-tap to activate" and it is not a
        // focus stop), and a tap on it reaches no handler regardless.
        var signInCount = 0
        setContent(
            GamesSignInState.SignedIn(playerId = PLAYER_ID, displayName = DISPLAY_NAME),
            onSignInToPlayGames = { signInCount++ },
        )

        val row = composeRule.onNodeWithText(signedInAs(DISPLAY_NAME)).performScrollTo()
        row.assertHasNoClickAction()
        row.performClick()
        composeRule.waitForIdle()

        assertThat(signInCount).isEqualTo(0)
    }

    // ---- The D-10 error snackbar ----------------------------------------------

    @Test
    fun aSignInErrorTickRaisedWhileOnSettingsShowsTheSharedSnackbar() {
        // Driven through a real recomposition (0 -> 1) rather than an already-raised
        // initial state, because that is the live sequence: the player taps the row, the
        // SDK fails, and the tick moves under a screen that is already composed.
        val state = mutableStateOf(SettingsUiState(gamesSignInState = GamesSignInState.SignedOut))
        composeRule.setContent {
            AppTheme {
                SettingsScreen(
                    state = state.value,
                    onToggleSound = {},
                    onToggleHaptics = {},
                    onSignInToPlayGames = {},
                    onPlayGamesSignInErrorShown = {},
                    onManagePrivacyChoices = {},
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(signInError).assertDoesNotExist()

        state.value = state.value.copy(playGamesSignInErrorTick = FIRST_ERROR_TICK)
        composeRule.waitForIdle()

        composeRule.onNodeWithText(signInError).assertIsDisplayed()
    }

    @Test
    fun aSignInErrorTickOfZeroIsNeverShown() {
        // The phantom-cue guard: a freshly composed Settings sits at tick 0 — and, once
        // consumed, returns to 0 (CR-01). Show on 0 and every visit opens by apologising for
        // a sign-in never attempted.
        setContent(GamesSignInState.SignedOut)

        composeRule.onNodeWithText(signInError).assertDoesNotExist()
    }

    @Test
    fun aShownSignInErrorIsConsumedAndDoesNotReplayWhenTheScreenRecomposesFromScratch() {
        // CR-01: the screen hands the ordinal back the moment it shows it. The callback here
        // plays the ViewModel's part (resetting the tick); leaving and re-entering composition
        // plays a configuration change under a ViewModel that survives it. The second
        // composition must be silent.
        val state =
            mutableStateOf(
                SettingsUiState(
                    gamesSignInState = GamesSignInState.SignedOut,
                    playGamesSignInErrorTick = FIRST_ERROR_TICK,
                ),
            )
        val hosted = mutableStateOf(true)
        var shownCount = 0
        composeRule.setContent {
            AppTheme {
                if (hosted.value) {
                    SettingsScreen(
                        state = state.value,
                        onToggleSound = {},
                        onToggleHaptics = {},
                        onSignInToPlayGames = {},
                        onPlayGamesSignInErrorShown = {
                            shownCount++
                            state.value = state.value.copy(playGamesSignInErrorTick = 0)
                        },
                        onManagePrivacyChoices = {},
                        onBack = {},
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(signInError).assertIsDisplayed()
        assertThat(shownCount).isEqualTo(1)

        hosted.value = false
        composeRule.waitForIdle()
        hosted.value = true
        composeRule.waitForIdle()

        composeRule.onNodeWithText(signInError).assertDoesNotExist()
        assertThat(shownCount).isEqualTo(1)
    }

    @Test
    fun theRowStaysAfterAFailedSignInAttempt() {
        // D-13: a decline or an error never retires the row — it is the only way back in.
        val state =
            mutableStateOf(
                SettingsUiState(
                    gamesSignInState = GamesSignInState.SignedOut,
                    playGamesSignInErrorTick = FIRST_ERROR_TICK,
                ),
            )
        composeRule.setContent {
            AppTheme {
                SettingsScreen(
                    state = state.value,
                    onToggleSound = {},
                    onToggleHaptics = {},
                    onSignInToPlayGames = {},
                    onPlayGamesSignInErrorShown = {},
                    onManagePrivacyChoices = {},
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(signInLabel).performScrollTo().assertIsDisplayed()
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val PLAY_GAMES_ROBOLECTRIC_SDK = 34

/** The first error ordinal a real `SettingsViewModel` would publish (0 means "nothing yet"). */
private const val FIRST_ERROR_TICK = 1

/** The Play Games identity the signed-in states carry. */
private const val PLAYER_ID = "player-1"
private const val DISPLAY_NAME = "Erman"
