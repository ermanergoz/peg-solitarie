package com.app.pegsolitaire.ui.settings

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.NoOpSoundPlayer
import com.app.pegsolitaire.data.FakePlayGamesOptInRepository
import com.app.pegsolitaire.data.SettingsRepository
import com.app.pegsolitaire.platform.games.FakeGamesService
import com.app.pegsolitaire.ui.theme.AppTheme
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Robolectric + Compose UI coverage of the Settings Privacy section (CONS-03 conditional
 * row, REL-05 always-present row and its browser intent; D-14/D-15).
 *
 * The screen is a stateless render of [SettingsUiState] plus hoisted handlers, so every
 * assertion here is deterministic: rows are located by their string resources and the
 * launched intent is read back off Robolectric's shared instrumentation rather than being
 * inferred from "nothing threw".
 *
 * The two claims that actually carry compliance weight are the asymmetry ones — the policy
 * row must survive `privacyOptionsRequired = false` (REL-05 is unconditional) while the
 * manage-choices row must not (CONS-03 hides what UMP says is not required) — so both
 * states are asserted rather than only the populated one.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class SettingsPrivacyTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val app get() = RuntimeEnvironment.getApplication()

    private val policyLabel get() = app.getString(R.string.settings_privacy_policy_label)
    private val choicesLabel get() = app.getString(R.string.settings_privacy_choices_label)
    private val generalHeader get() = app.getString(R.string.settings_section_general)
    private val privacyHeader get() = app.getString(R.string.settings_section_privacy)

    /** Renders the screen over the given CONS-03 flag; handlers default to no-ops. */
    private fun setContent(
        privacyOptionsRequired: Boolean,
        onManagePrivacyChoices: () -> Unit = {},
    ) {
        composeRule.setContent {
            AppTheme {
                SettingsScreen(
                    state = SettingsUiState(privacyOptionsRequired = privacyOptionsRequired),
                    onToggleSound = {},
                    onToggleHaptics = {},
                    onSignInToPlayGames = {},
                    onManagePrivacyChoices = onManagePrivacyChoices,
                    onBack = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun thePolicyRowShowsAndTheManageChoicesRowIsAbsentWhenNotRequired() {
        setContent(privacyOptionsRequired = false)

        composeRule.onNodeWithText(policyLabel).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(choicesLabel).assertDoesNotExist()
    }

    @Test
    fun bothRowsShowWhenThePrivacyOptionsFormIsRequired() {
        setContent(privacyOptionsRequired = true)

        composeRule.onNodeWithText(policyLabel).performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(choicesLabel).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun bothSectionHeadersShowInEitherState() {
        setContent(privacyOptionsRequired = false)

        composeRule.onNodeWithText(generalHeader).assertIsDisplayed()
        composeRule.onNodeWithText(privacyHeader).assertIsDisplayed()
    }

    @Test
    fun bothSectionHeadersShowWhenTheManageChoicesRowIsPresentToo() {
        setContent(privacyOptionsRequired = true)

        composeRule.onNodeWithText(generalHeader).assertIsDisplayed()
        composeRule.onNodeWithText(privacyHeader).assertIsDisplayed()
    }

    @Test
    fun tappingThePolicyRowLaunchesAViewIntentForThePolicyUrl() {
        setContent(privacyOptionsRequired = false)

        composeRule.onNodeWithText(policyLabel).performScrollTo().performClick()
        composeRule.waitForIdle()

        val launched = shadowOf(app).nextStartedActivity
        assertThat(launched).isNotNull()
        assertThat(launched.action).isEqualTo(Intent.ACTION_VIEW)
        assertThat(launched.data.toString()).isEqualTo(PrivacyLinks.POLICY_URL)
    }

    @Test
    fun tappingTheManageChoicesRowInvokesItsHandlerExactlyOnce() {
        var manageCount = 0
        setContent(privacyOptionsRequired = true, onManagePrivacyChoices = { manageCount++ })

        composeRule.onNodeWithText(choicesLabel).performScrollTo().performClick()
        composeRule.waitForIdle()

        assertThat(manageCount).isEqualTo(1)
    }

    /**
     * The T-09-28 dead-end case: `checkActivities(true)` makes Robolectric's `startActivity`
     * throw [android.content.ActivityNotFoundException] when nothing resolves the intent —
     * which is exactly a device with no browser. The `runCatching` in `SettingsScreen` must
     * turn that into the snackbar rather than a crash, because a tap that silently does
     * nothing is the dead end this phase forbids.
     */
    @Test
    fun aMissingBrowserShowsTheErrorSnackbarInsteadOfCrashing() {
        shadowOf(app).checkActivities(true)
        val errorMessage = app.getString(R.string.settings_privacy_policy_error)
        setContent(privacyOptionsRequired = false)

        composeRule.onNodeWithText(policyLabel).performScrollTo().performClick()

        composeRule.waitUntil(timeoutMillis = SNACKBAR_TIMEOUT_MS) {
            composeRule.onAllNodesWithText(errorMessage).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(errorMessage).assertIsDisplayed()
    }

    @Test
    fun theSoundAndHapticsRowsStillRender() {
        setContent(privacyOptionsRequired = true)

        composeRule
            .onNodeWithText(app.getString(R.string.settings_sound_label))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(app.getString(R.string.settings_haptics_label))
            .performScrollTo()
            .assertIsDisplayed()
    }
}

/**
 * The ViewModel half of CONS-03: the consent seam — not the screen, and not any client-side
 * inference — is what decides whether the manage-choices row exists (T-09-30).
 *
 * Separated from [SettingsPrivacyTest] rather than merged into it because
 * `Dispatchers.setMain(StandardTestDispatcher())` and Compose's UI-test infrastructure both
 * want the main dispatcher; keeping the coroutine harness in its own class leaves the
 * Robolectric tests above on a real main looper. This is the [SettingsViewModelTest] harness.
 */
class SettingsPrivacyViewModelTest {
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
    fun theFlagIsFailClosedBeforeTheConsentSeamAnswers() =
        runTest(testDispatcher) {
            val viewModel = viewModelWith(FakeConsentGateway(privacyOptionsRequired = false))
            runCurrent()

            assertThat(viewModel.uiState.value.privacyOptionsRequired).isFalse()
        }

    @Test
    fun aRequiredPrivacyOptionsFormPropagatesIntoTheUiState() =
        runTest(testDispatcher) {
            val gateway = FakeConsentGateway(privacyOptionsRequired = false)
            val viewModel = viewModelWith(gateway)
            runCurrent()

            gateway.emitPrivacyOptionsRequired(true)
            runCurrent()

            assertThat(viewModel.uiState.value.privacyOptionsRequired).isTrue()
        }

    @Test
    fun onManagePrivacyChoicesOpensTheFormThroughTheSeam() =
        runTest(testDispatcher) {
            val gateway = FakeConsentGateway(privacyOptionsRequired = true)
            val viewModel = viewModelWith(gateway)
            runCurrent()

            viewModel.onManagePrivacyChoices()
            runCurrent()

            assertThat(gateway.showPrivacyOptionsCount).isEqualTo(1)
        }

    private fun viewModelWith(gateway: FakeConsentGateway) =
        SettingsViewModel(
            StubSettingsRepository,
            NoOpSoundPlayer(),
            gateway,
            FakeGamesService(),
            FakePlayGamesOptInRepository(),
        )
}

/** A fixed settings store: this file asserts consent behaviour, not persistence. */
private object StubSettingsRepository : SettingsRepository {
    override val hintsEnabled: Flow<Boolean> = flowOf(true)

    override val soundOn: Flow<Boolean> = flowOf(true)

    override val hapticsOn: Flow<Boolean> = flowOf(true)

    override suspend fun setHintsEnabled(enabled: Boolean) = Unit

    override suspend fun setSoundOn(on: Boolean) = Unit

    override suspend fun setHapticsOn(on: Boolean) = Unit
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34

/** Generous upper bound for the snackbar to reach composition; it normally lands immediately. */
private const val SNACKBAR_TIMEOUT_MS = 5_000L
