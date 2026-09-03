package com.app.pegsolitaire.ui.game

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.app.pegsolitaire.R
import com.app.pegsolitaire.platform.ads.AdReason
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * What the *screen* does with the assist funnel's two surfaces (ADS-01/ADS-03/ADS-04),
 * asserted JVM-side via Robolectric on the `NoWinBannerTest`/`GameScreenSoundTest` harness.
 *
 * It drives the real [GameScreen] from hand-constructed [GameUiState] values and deliberately
 * never builds the screen's ViewModel: the funnel's *behaviour* — who is charged, what an
 * accept costs, what a decline leaves behind — belongs to the allowance spec that plan 09-08
 * added beside it. This class asserts only the two claims that live in the composition: the
 * dialog is raised exactly when
 * the hoisted state says so and forwards its taps unchanged, and the D-11 message reaches the
 * player exactly once per emission and never on screen entry.
 *
 * [AdOfferDialogTest] covers the dialog leaf's own copy, loading lock and action hierarchy;
 * nothing here re-asserts those.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [AD_OFFER_SCREEN_ROBOLECTRIC_SDK])
class GameScreenAdOfferTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()

    private var acceptCount = 0
    private var dismissCount = 0
    private var undoCount = 0

    // ---- The dialog is gated purely on pendingAdOffer ------------------------

    @Test
    fun noOfferPendingRendersNoDialogAtAll() {
        setScreen(GameUiState.initial())

        composeRule.onNodeWithTag(AD_OFFER_DIALOG_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun aPendingHintOfferRaisesTheDialogWithTheHintTitle() {
        setScreen(GameUiState.initial().copy(pendingAdOffer = AdReason.HINT))

        composeRule.onNodeWithTag(AD_OFFER_DIALOG_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.ad_offer_hint_title)).assertIsDisplayed()
    }

    @Test
    fun aPendingUndoOfferRaisesTheDialogWithTheUndoTitle() {
        setScreen(GameUiState.initial().copy(pendingAdOffer = AdReason.UNDO))

        composeRule.onNodeWithTag(AD_OFFER_DIALOG_TEST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(app.getString(R.string.ad_offer_undo_title)).assertIsDisplayed()
    }

    // ---- …and forwards both answers, unchanged ------------------------------

    @Test
    fun acceptingTheOfferReachesTheScreensAcceptHandlerExactlyOnce() {
        setScreen(GameUiState.initial().copy(pendingAdOffer = AdReason.HINT))

        composeRule.onNodeWithText(app.getString(R.string.ad_offer_watch)).performClick()

        assertThat(acceptCount).isEqualTo(1)
        assertThat(dismissCount).isEqualTo(0)
    }

    @Test
    fun decliningTheOfferReachesTheScreensDismissHandlerExactlyOnce() {
        setScreen(GameUiState.initial().copy(pendingAdOffer = AdReason.UNDO))

        composeRule.onNodeWithText(app.getString(R.string.ad_offer_decline)).performClick()

        assertThat(dismissCount).isEqualTo(1)
        assertThat(acceptCount).isEqualTo(0)
    }

    // ---- The D-11 snackbar, and the phantom cue it must not fire ------------

    @Test
    fun aRaisedAdMessageIsShownToThePlayer() {
        setScreen(
            GameUiState.initial().copy(
                adMessage = R.string.ad_unavailable_hints,
                adMessageTick = FIRST_MESSAGE_TICK,
            ),
        )

        composeRule
            .onNodeWithText(app.getString(R.string.ad_unavailable_hints))
            .assertIsDisplayed()
    }

    /**
     * The phantom-cue regression test (T-09-40), and the reason `adMessage` is a *pair*.
     *
     * A state carrying a message id at tick 0 is the screen-entry case: a resumed game whose
     * state was rebuilt, or a first composition. Nothing was raised, so nothing may be shown.
     *
     * This fails if either half of the fix is removed — the `> 0` guard in `GameScreen`'s
     * snackbar effect, or plan 09-06's carry-forward of the ordinal at the four board-install
     * sites in the ViewModel (without the carry-forward a board install resets the tick to 0
     * and the *next* real message re-uses a key the effect has already seen). A plain nullable
     * message field could not express this state at all: it has no way to say "this id was
     * shown already", so the same message twice in a row would be indistinguishable from a
     * message that was never consumed.
     */
    @Test
    fun anAdMessageAtTickZeroIsNeverShown() {
        setScreen(
            GameUiState.initial().copy(
                adMessage = R.string.ad_unavailable_hints,
                adMessageTick = 0,
            ),
        )

        composeRule
            .onNodeWithText(app.getString(R.string.ad_unavailable_hints))
            .assertDoesNotExist()
    }

    // ---- The negative contract: no second offer surface ---------------------

    /**
     * UI-SPEC §4 / §Deliberate Non-UI, asserted as a test rather than left to review.
     *
     * The rescue banner keeps its current appearance, copy and handler: its Undo reaches the
     * same `onUndo` funnel that raises the dialog once the allowance is spent (ADS-04, D-08).
     * That is the whole mechanism — **one allowance, one dialog, no second offer surface** — so
     * an ad affordance, badge or alternate label on the banner is out of contract, not an
     * improvement. This test asserts both halves: the funnel is still reached, and no ad string
     * appears anywhere inside the banner. A future change that adds a second offer surface
     * fails here, next to the sentence explaining why it must not exist.
     */
    @Test
    fun theRescueBannerReachesTheUndoFunnelAndCarriesNoAdAffordance() {
        setScreen(GameUiState.initial().copy(noWinningMove = true))

        composeRule.onNodeWithTag(NOWIN_BANNER_TEST_TAG).assertIsDisplayed()

        val adStrings =
            listOf(
                R.string.ad_offer_watch,
                R.string.ad_offer_decline,
                R.string.ad_offer_hint_title,
                R.string.ad_offer_undo_title,
                R.string.ad_unavailable_undos,
            )
        adStrings.forEach { adString ->
            composeRule
                .onNode(
                    hasText(app.getString(adString)) and
                        hasAnyAncestor(hasTestTag(NOWIN_BANNER_TEST_TAG)),
                ).assertDoesNotExist()
        }
        // No offer is pending, so the banner is also the only surface on screen.
        composeRule.onNodeWithTag(AD_OFFER_DIALOG_TEST_TAG).assertDoesNotExist()

        composeRule.onNodeWithText(app.getString(R.string.undo_action)).performClick()

        assertThat(undoCount).isEqualTo(1)
        assertThat(acceptCount).isEqualTo(0)
    }

    // ---- Harness -------------------------------------------------------------

    /** Renders the real screen over [state], recording every handler this class asserts on. */
    private fun setScreen(state: GameUiState) {
        composeRule.setContent {
            AppTheme {
                GameScreen(
                    state = state,
                    onCellTap = {},
                    onRestart = {},
                    onConfirmRestart = {},
                    onDismissRestart = {},
                    onAnimationFinished = {},
                    onToggleHints = {},
                    onUndo = { undoCount++ },
                    onHint = {},
                    onAcceptAdOffer = { acceptCount++ },
                    onDismissAdOffer = { dismissCount++ },
                    onHome = {},
                    onNavigateBack = {},
                )
            }
        }
        composeRule.waitForIdle()
    }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val AD_OFFER_SCREEN_ROBOLECTRIC_SDK = 34

/** The first ordinal a real emission can carry — the tick is monotonic from 0. */
private const val FIRST_MESSAGE_TICK = 1
