package com.app.pegsolitaire.ui.home

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import com.app.pegsolitaire.R
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.domain.model.Boards
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
        composeRule.onNodeWithText(CONTINUE_LABEL).performScrollTo().performClick()
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
        renderHome(state = HomeUiState())

        assertThat(renderedValues()).contains(unknownValue)
        assertThat(pillDescriptions(STARS_PILL)).containsExactly(announcement(starsLabel, unknownDescription))
        assertThat(pillDescriptions(BOARDS_PILL)).containsExactly(announcement(boardsLabel, unknownDescription))
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
        renderHome(state = state)

        assertThat(renderedValues()).contains(unknownValue)
        assertThat(renderedValues()).doesNotContain(ZERO_VALUE)

        // A brand-new player genuinely has zero, and that IS a number, spoken as one.
        state.value = HomeUiState(totalStars = 0, classicBoardsCleared = 0)
        composeRule.waitForIdle()

        assertThat(renderedValues()).contains(ZERO_VALUE)
        assertThat(renderedValues()).doesNotContain(unknownValue)
        assertThat(pillDescriptions(STARS_PILL)).containsExactly(announcement(starsLabel, ZERO_VALUE))
    }

    @Test
    fun theBoardsPillDenominatorIsTheClassicCatalogueSize() {
        // Criterion 13b, and prohibition 5's Home half: the denominator is a compile-time
        // fact, so no stored row — and no tutorial board joining CLASSIC_SPECS — can change
        // a shipped number. The expected text is built from the catalogue itself, so this
        // test tracks a sixth board instead of pinning a stale 5.
        renderHome(state = HomeUiState(classicBoardsCleared = CLEARED_BOARDS))

        val expected =
            RuntimeEnvironment.getApplication().getString(
                R.string.home_stat_boards_value,
                CLEARED_BOARDS,
                Boards.CLASSIC_SPECS.size,
            )
        assertThat(renderedValues()).contains(expected)
        assertThat(pillDescriptions(BOARDS_PILL)).containsExactly(announcement(boardsLabel, expected))
        assertThat(Boards.CLASSIC_SPECS).hasSize(CLASSIC_CATALOGUE_SIZE)
    }

    @Test
    fun theLevelPillAndTheChallengeTileShowTheSameNumber() {
        // Criteria 12 / 15b. Both read `challengeCurrentLevel`, so they cannot diverge by
        // construction; what this pins is that both are WIRED to it. The tile chip is the
        // cloud-save oracle and the pill is additive — if they ever disagree the tile is right.
        renderHome(state = HomeUiState(challengeCurrentLevel = PERSISTED_LEVEL))

        assertThat(renderedValues()).contains(PERSISTED_LEVEL.toString())
        assertThat(pillDescriptions(LEVEL_PILL))
            .containsExactly(announcement(levelLabel, PERSISTED_LEVEL.toString()))
        composeRule.onNodeWithText(CONTINUE_LABEL).assertExists()
    }

    @Test
    fun eachRibbonPillSpeaksAsOneNodeSayingItsLabelThenItsValue() {
        // "Stars, 7", never a bare number: each pill announces once, label first, and the
        // ribbon is three nodes rather than nine. Asserted as the exact spoken string, so a
        // future refactor that merged descendants instead — and thereby dropped the label from
        // TalkBack's reading — fails here rather than only on a device.
        renderHome(
            state =
                HomeUiState(
                    totalStars = EARNED_STARS,
                    classicBoardsCleared = CLEARED_BOARDS,
                    challengeCurrentLevel = PERSISTED_LEVEL,
                ),
        )

        assertThat(ribbonPillCount()).isEqualTo(RIBBON_PILLS)
        assertThat(pillDescriptions(STARS_PILL))
            .containsExactly(announcement(starsLabel, EARNED_STARS.toString()))
        assertThat(pillDescriptions(LEVEL_PILL))
            .containsExactly(announcement(levelLabel, PERSISTED_LEVEL.toString()))
    }

    @Test
    fun theProgressPillsAreNotTappable() {
        // §5's negative contract. The pills are status, not navigation: three more competing
        // targets is the opposite of what the founder asked for, and a tappable pill would
        // also make the ribbon a fourth thing to reason about on the busiest screen.
        renderHome(state = HomeUiState(totalStars = EARNED_STARS))

        repeat(RIBBON_PILLS) { index ->
            assertThat(pillClickAction(index)).isNull()
        }
    }

    @Test
    fun bothContinueChipsAndBothModeTilesRenderWhenBothResumesExist() {
        // The D-03f oracle-3 guard at unit level: the busiest legitimate Home — two Continue
        // chips AND two mode tiles — renders all four. Whether all four are within the first
        // viewport at default font scale on a 360x640dp phone is criterion 15a, a DEVICE
        // check: this rule's viewport is 320x470px, so it can prove presence and not the fold.
        renderHome(
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

    // ---- Home's vertical slack (D-03i, §4.2.2, criteria 15g-15j) -------------

    /**
     * Criterion 15g, the fresh-install state — the player the refresh is for, and the state
     * the device session failed on.
     *
     * With a fixed `Spacer` in a top-aligned scroll column all the slack falls to the *bottom*:
     * QA measured the Classic tile's centre at 1173 px against a 1212 px midline with the
     * bottom 34% of the screen empty, so criterion 15 held only for a player who happened to
     * have both Continue chips. The weighted pair puts the slack above band 3 instead, and the
     * assertion is on the tile's **top** edge, because a tile whose top is below the midpoint
     * is wholly in the lower half however tall it grows.
     *
     * The window is the reference 6.7-inch phone. Robolectric reports zero window insets, so
     * the root's height *is* the available content height the `Scaffold` hands `HomeBands` —
     * which is what "half the Home content height" means in §4.2.2's owed tests.
     */
    @Test
    @Config(qualifiers = TALL_PHONE_QUALIFIER)
    fun theFirstModeTileStartsBelowTheMidpointOnAFreshInstall() {
        renderHome(state = HomeUiState(challengeCurrentLevel = PERSISTED_LEVEL))

        assertThat(topOf(classicTileTitle)).isAtLeast(midpointOfContent())
    }

    /** Criterion 15g's other half: the busiest legitimate Home must hold the same line. */
    @Test
    @Config(qualifiers = TALL_PHONE_QUALIFIER)
    fun theFirstModeTileStartsBelowTheMidpointWithBothContinueChips() {
        renderHome(state = bothResumesState())

        assertThat(topOf(classicTileTitle)).isAtLeast(midpointOfContent())
    }

    /**
     * Criterion 15h — the compact band set, on the small baseline with both chips.
     *
     * 15a failed on this window before the refresh and after it: the full set needs ~716dp
     * against ~640dp, so no value of a spacer could fit it and the band set itself has to
     * change (§4.2.2). Below `home_compact_threshold_height` the hero drops its art and
     * becomes one row, and the spacing drops to `spacing_sm`.
     *
     * "Fully inside the viewport" is asserted as the bottom edge of the **last** band, since
     * the four are laid out in order: if the Challenge tile's bottom is inside, everything
     * above it is too, and each node is asserted displayed as well so nothing is merely
     * *measured* inside.
     */
    @Test
    @Config(qualifiers = SHORT_PHONE_QUALIFIER)
    fun bothChipsAndBothTilesAreInsideTheViewportOnAShortPhoneWithNoHeroArt() {
        renderHome(state = bothResumesState())

        composeRule.onNodeWithTag(HOME_HERO_ART_TEST_TAG).assertDoesNotExist()
        listOf(RESUME_CLASSIC_LABEL, RESUME_CHALLENGE_LABEL, classicTileTitle, challengeTileTitle)
            .forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
        assertThat(bottomOf(challengeTileTitle)).isAtMost(contentBottom())
    }

    /** The full set keeps its art on a window with room for it — the other side of 15h. */
    @Test
    @Config(qualifiers = TALL_PHONE_QUALIFIER)
    fun theHeroKeepsItsArtOnAPhoneAboveTheCompactThreshold() {
        renderHome(state = bothResumesState())

        composeRule.onNodeWithTag(HOME_HERO_ART_TEST_TAG).assertExists()
    }

    /**
     * Criterion 15i (with 14) — at 200% font scale the **slack** is what disappears, not the
     * content: every band is still composed and still reachable, and the column scrolls.
     *
     * This is the case §4.2.2 clause 4 forbids solving with a weighted content band: a
     * weighted child with `fill = true` is measured at exactly its share, so a weighted hero
     * would measure **0** the moment the fixed bands exceed the viewport — clipped to nothing
     * at exactly this font scale.
     */
    @Test
    @Config(qualifiers = SHORT_PHONE_QUALIFIER)
    fun everyBandSurvivesAndStaysReachableAtDoubleFontScale() {
        renderHome(state = bothResumesState(), fontScale = DOUBLE_FONT_SCALE)

        listOf(
            appName,
            RESUME_CLASSIC_LABEL,
            RESUME_CHALLENGE_LABEL,
            classicTileTitle,
            challengeTileTitle,
        ).forEach { composeRule.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
        composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG).performScrollTo().assertIsDisplayed()
    }

    /**
     * Criterion 15j, first half: the slack is empty space and not a control.
     *
     * The two `Spacer`s carry no semantics at all, so there is no node to interrogate — the
     * honest form of "neither spacer is clickable or focusable" is therefore an exhaustive
     * count of what *is* clickable on the busiest Home. Five, and these five: the Settings
     * gear, two Continue chips, two mode tiles. A control added into the slack fails here.
     */
    @Test
    @Config(qualifiers = TALL_PHONE_QUALIFIER)
    fun theOnlyClickableThingsOnHomeAreTheGearTheTwoChipsAndTheTwoTiles() {
        renderHome(state = bothResumesState())

        assertThat(composeRule.onAllNodes(hasClickAction()).fetchSemanticsNodes()).hasSize(HOME_CONTROL_COUNT)
        listOf(RESUME_CLASSIC_LABEL, RESUME_CHALLENGE_LABEL, classicTileTitle, challengeTileTitle)
            .forEach { composeRule.onNodeWithText(it).assertHasClickAction() }
        composeRule.onNodeWithContentDescription(settingsLabel).assertHasClickAction()
    }

    /**
     * Criterion 15j, second half: on a window wider than `content_max_width` the content
     * column is still capped **and centred**, not pushed against one edge.
     *
     * §12 has claimed this since Phase 2 and it stopped being true when `fillMaxSize()` went
     * ahead of `widthIn(max = …)` in the column's chain: ahead of `verticalScroll` it fixes
     * the content's width to the window's, which makes the cap that follows it a no-op
     * (§4.2.2 clause 2).
     */
    @Test
    @Config(qualifiers = WIDE_WINDOW_QUALIFIER)
    fun theContentColumnIsCappedAndCentredOnAWindowWiderThanTheCap() {
        renderHome(state = bothResumesState())

        val root = composeRule.onRoot().getBoundsInRoot()
        val tile = composeRule.onNodeWithText(classicTileTitle).getBoundsInRoot()
        val cap = with(composeRule.density) { contentMaxWidthPx.toDp() }

        assertThat(tile.right - tile.left).isAtMost(cap)
        // Centred: the two margins agree. A column hugging the leading edge fails the first
        // assertion of the pair, whatever the second says.
        assertThat((tile.left - root.left).value).isWithin(CENTRING_TOLERANCE_DP).of((root.right - tile.right).value)
        assertThat(tile.left).isGreaterThan(root.left)
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
        fontScale: Float = DEFAULT_FONT_SCALE,
    ) = renderHome(
        state = mutableStateOf(state),
        onPlayChallenge = onPlayChallenge,
        onResumeClassic = onResumeClassic,
        onResumeChallenge = onResumeChallenge,
        onKeepDeviceProgress = onKeepDeviceProgress,
        onUseCloudProgress = onUseCloudProgress,
        fontScale = fontScale,
    )

    // ---- Layout helpers (D-03i) ----------------------------------------------

    /** The busiest legitimate Home: a filled ribbon, both Continue chips, both mode tiles. */
    private fun bothResumesState(): HomeUiState =
        HomeUiState(
            totalStars = EARNED_STARS,
            classicBoardsCleared = CLEARED_BOARDS,
            challengeCurrentLevel = PERSISTED_LEVEL,
            classicResumeLevelId = RESUMABLE_LEVEL_ID,
            classicResumeBoard = Boards.european37Initial,
            challengeResumeLevel = STAMPED_RESUME_LEVEL,
        )

    private fun topOf(text: String): Dp = composeRule.onNodeWithText(text).getBoundsInRoot().top

    private fun bottomOf(text: String): Dp = composeRule.onNodeWithText(text).getBoundsInRoot().bottom

    /**
     * Half the height of Home's content area.
     *
     * Robolectric delivers no window insets, so the `Scaffold`'s content padding is zero and
     * the root's height is exactly the available height `HomeBands` measures its slack
     * against — which is what §4.2.2 means by "the available height". On a device the insets
     * are subtracted first, by the `Scaffold`, before the threshold or the weights see it.
     */
    private fun midpointOfContent(): Dp {
        val root = composeRule.onRoot().getBoundsInRoot()
        return (root.bottom - root.top) / HALVES
    }

    private fun contentBottom(): Dp = composeRule.onRoot().getBoundsInRoot().bottom

    private val contentMaxWidthPx: Int
        get() = RuntimeEnvironment.getApplication().resources.getDimensionPixelSize(R.dimen.content_max_width)

    /**
     * The settable overload, for the tests that need a live recomposition — Home fills the
     * ribbon *after* first paint, so "shows the placeholder, then the value" is only honest
     * when it is asserted across one state change rather than across two compositions.
     */
    private fun renderHome(
        state: MutableState<HomeUiState>,
        onPlayChallenge: (Int) -> Unit = {},
        onResumeClassic: (String) -> Unit = {},
        onResumeChallenge: (Int) -> Unit = {},
        onKeepDeviceProgress: () -> Unit = {},
        onUseCloudProgress: () -> Unit = {},
        fontScale: Float = DEFAULT_FONT_SCALE,
    ) {
        composeRule.setContent {
            // The font scale is provided rather than set through a resource qualifier because
            // there is no qualifier for it: it is a property of `Density`, which is where
            // every `sp` on the screen reads it from.
            CompositionLocalProvider(
                LocalDensity provides Density(density = LocalDensity.current.density, fontScale = fontScale),
            ) {
                HomeScreen(
                    state = state.value,
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
        }
        composeRule.waitForIdle()
    }

    // ---- Ribbon helpers ------------------------------------------------------

    /**
     * The three pills are the ribbon node's children, located through
     * [HOME_PROGRESS_RIBBON_TEST_TAG] rather than by their copy — the rule §13 sets after the
     * retired overlay was found by its "Got it" label.
     */
    private fun pill(index: Int) = ribbon().onChildren()[index]

    private fun ribbonPillCount(): Int = ribbon().fetchSemanticsNode().children.size

    private fun ribbon() = composeRule.onNodeWithTag(HOME_PROGRESS_RIBBON_TEST_TAG)

    /**
     * Every value string the ribbon actually **renders**, read from the unmerged tree.
     *
     * The pills clear and set one description each, so their child `Text`s are invisible to the
     * merged tree by design — which is the point of the announcement assertions. What a player
     * *sees* therefore has to be read past that, and this is where "—" versus "0" is checked.
     */
    private fun renderedValues(): List<String> =
        composeRule
            .onNode(hasTestTag(HOME_PROGRESS_RIBBON_TEST_TAG), useUnmergedTree = true)
            .fetchSemanticsNode()
            .textsIn()

    private fun androidx.compose.ui.semantics.SemanticsNode.textsIn(): List<String> =
        config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            children.flatMap { it.textsIn() }

    /** The exact string a pill announces — "Stars, 7" — built from the shipped format. */
    private fun announcement(
        label: String,
        value: String,
    ): String = RuntimeEnvironment.getApplication().getString(R.string.home_stat_pill_desc, label, value)

    /** Every `contentDescription` on a pill's node — the whole announcement, and only one. */
    private fun pillDescriptions(index: Int): List<String> =
        pill(index)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.ContentDescription)
            .orEmpty()

    /** A pill's click action, which must be absent: the pills are status, not navigation. */
    private fun pillClickAction(index: Int) =
        pill(index).fetchSemanticsNode().config.getOrNull(SemanticsActions.OnClick)

    private val starsLabel: String
        get() = RuntimeEnvironment.getApplication().getString(R.string.home_stat_stars_label)

    private val boardsLabel: String
        get() = RuntimeEnvironment.getApplication().getString(R.string.home_stat_boards_label)

    private val levelLabel: String
        get() = RuntimeEnvironment.getApplication().getString(R.string.home_stat_level_label)

    private val unknownValue: String
        get() = RuntimeEnvironment.getApplication().getString(R.string.stat_value_unknown)

    private val unknownDescription: String
        get() = RuntimeEnvironment.getApplication().getString(R.string.stat_value_unknown_desc)

    private val classicTileTitle: String
        get() = RuntimeEnvironment.getApplication().getString(R.string.classic_tile_title)

    private val challengeTileTitle: String
        get() = RuntimeEnvironment.getApplication().getString(R.string.challenge_tile_title)

    /** The hero's gradient title — band 1's own content, and the compact hero's whole width. */
    private val appName: String
        get() = RuntimeEnvironment.getApplication().getString(R.string.app_name)

    /** The gear's unchanged description (D-05b): `ButtonSoundTest` locates it by this string. */
    private val settingsLabel: String
        get() = RuntimeEnvironment.getApplication().getString(R.string.settings_action)

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

/** Ribbon pill positions, left to right (§4.2 band 2). */
private const val STARS_PILL = 0
private const val BOARDS_PILL = 1
private const val LEVEL_PILL = 2

/** Three pills, and exactly three: the ribbon is not a place to add a fourth thing. */
private const val RIBBON_PILLS = 3

/** A stars total past zero, so a placeholder cannot pass for a real value. */
private const val EARNED_STARS = 7

/** Cleared Classic boards — deliberately neither 0 nor the catalogue size. */
private const val CLEARED_BOARDS = 3

/** The shipped Classic catalogue size, asserted against `Boards.CLASSIC_SPECS` itself. */
private const val CLASSIC_CATALOGUE_SIZE = 5

/** What a genuinely-zero pill renders — a real answer, and never the placeholder. */
private const val ZERO_VALUE = "0"

/**
 * The reference 6.7-inch phone (criterion 15/15g), and the small baseline the first-paint
 * rule is written against (criteria 15a/15h). The short one is BELOW
 * `home_compact_threshold_height`, so it takes the compact band set; the tall one is above it.
 */
private const val TALL_PHONE_QUALIFIER = "w411dp-h915dp"
private const val SHORT_PHONE_QUALIFIER = "w360dp-h640dp"

/** Wider than `content_max_width` 480dp, which is the only window where the cap is visible. */
private const val WIDE_WINDOW_QUALIFIER = "w800dp-h915dp"

/** The system font-size setting at its maximum (criteria 14, 15i). */
private const val DOUBLE_FONT_SCALE = 2f

/** The system font-size setting as shipped. */
private const val DEFAULT_FONT_SCALE = 1f

/** Two halves of a screen — the midpoint criterion 15g is asserted against. */
private const val HALVES = 2f

/**
 * Everything on the busiest Home a player can press: the Settings gear, two Continue chips,
 * two mode tiles. The ribbon's three pills are deliberately not among them (§5), and neither
 * spacer may ever be (criterion 15j).
 */
private const val HOME_CONTROL_COUNT = 5

/**
 * Slack the centring assertion allows, in dp: a cap of an odd number of pixels cannot be
 * centred exactly, and rounding may put one extra pixel on either side.
 */
private const val CENTRING_TOLERANCE_DP = 1f
