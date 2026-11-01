package com.app.pegsolitaire.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.core.net.toUri
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.audio.withClickSound
import com.app.pegsolitaire.platform.games.GamesSignInState
import com.app.pegsolitaire.ui.components.ClaySnackbarHost
import com.app.pegsolitaire.ui.theme.clayEntrance
import com.app.pegsolitaire.ui.theme.claySquish
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** The capped content column's test tag (the tech-lead's re-run ruling, F-3). */
internal const val SETTINGS_CONTENT_TAG = "settings_content"

/**
 * The stateless Settings screen (SET-01, SET-02, D-14).
 *
 * Renders [state] and forwards events; owns no logic and no ViewModel (state hoisting —
 * the switches' `checked` values come from [state] and every flip leaves through
 * [onToggleSound]/[onToggleHaptics]/[onToggleColorblindPalette]). An M3 [Scaffold] hosts a
 * [TopAppBar] (title + back nav) over a centered column, capped at `content_max_width`, of three
 * toggle rows that are exact peers: Sound, Haptics and the colour-blind palette.
 *
 * All three switches are live. Sound's gate is the sound player's own (SOUND-05): it reads the
 * choice straight from the repository, so switching it off also silences cues already
 * playing. Haptics gates the jump's light tick (D-15) and the wrong tap's reject buzz (D-06b).
 *
 * ## The two sections (D-14, REL-05, CONS-03)
 *
 * The column is grouped under a "General" and a "Privacy" header. **The "General" header is
 * added deliberately**: D-14 asks only for a Privacy *section*, but a section label over one
 * group and not the other is precisely the inconsistency the design contract exists to
 * prevent — both headers or neither, and named groups are the honest choice once the screen
 * holds two unrelated concerns.
 *
 * The privacy-policy row is unconditional (REL-05: every install, every region); the
 * manage-choices row renders only while [SettingsUiState.privacyOptionsRequired] is `true`,
 * because in other regions there is no UMP form to open and an affordance that opens nothing
 * is worse than its absence (CONS-03, T-09-30).
 *
 * Neither privacy row carries an accent gradient. They are plain clay cards, exact peers of
 * Sound and Haptics — a privacy-policy link is not a celebration.
 *
 * ## The third section (D-08, PGS-02/PGS-03)
 *
 * "Play Games" sits **between** General and Privacy, not after it: D-14 pins Privacy to the
 * bottom of this screen, so every later section is inserted above it rather than appended.
 * Phase 12's leaderboard and achievement rows land in this same section.
 *
 * It is the one section that can be wholly absent. While the session is
 * [GamesSignInState.Unknown] there is no header, no row and no placeholder (D-11) — see
 * [PlayGamesSection]. The screen decides none of that itself: like every other conditional
 * here, it renders exactly what [SettingsUiState] says and nothing more.
 *
 * @param state the immutable Settings UI state (the two persisted flags, the CONS-03 flag,
 *   and the Play Games session plus its error ordinal).
 * @param onToggleSound persists the new sound choice (SET-01).
 * @param onToggleHaptics persists the new haptics choice (SET-02).
 * @param onToggleColorblindPalette persists the new colour-blind palette choice
 *   (docs/ux/colour-vision.md CV-03); silent, like every toggle (D-09).
 * @param onShowHowToPlay opens the interactive tutorial (ONB-01, D-02a) — the **one**
 *   permanent entry point to it, since Home's help icon is deleted. A hoisted lambda
 *   supplied by `AppNavHost`; the screen never sees a controller.
 * @param onOpenLicenses opens the Licenses screen from Settings › About (spec LS-01).
 * @param onSignInToPlayGames signs the player in to Play Games and records the opt-in
 *   (PGS-03, D-10/D-19). Reached only from the signed-out row.
 * @param onShowLeaderboards opens the stock all-leaderboards picker (PGSL-06, D-14).
 *   Reached only from the signed-in rows.
 * @param onShowAchievements opens the stock achievements UI (PGSA-05, D-14). Reached only
 *   from the signed-in rows.
 * @param onPlayGamesSignInErrorShown the D-10 error snackbar has been raised for the current
 *   [SettingsUiState.playGamesSignInErrorTick]; the ViewModel consumes the ordinal so a
 *   rotation does not replay it (CR-01).
 * @param onManagePrivacyChoices opens the UMP privacy-options form (CONS-03).
 * @param onBack leaves Settings (top-bar back nav → pops to Home).
 * @param modifier layout modifier for the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleColorblindPalette: (Boolean) -> Unit,
    onShowHowToPlay: () -> Unit,
    onOpenLicenses: () -> Unit,
    onSignInToPlayGames: () -> Unit,
    onShowLeaderboards: () -> Unit,
    onShowAchievements: () -> Unit,
    onPlayGamesSignInErrorShown: () -> Unit,
    onManagePrivacyChoices: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // One host for both failures this screen can surface — the missing-browser message and
    // the D-10 sign-in error. A second SnackbarHostState would let two messages fight for
    // the same corner, and the Scaffold can only host one anyway.
    val snackbarHostState = remember { SnackbarHostState() }
    PlayGamesSignInErrorSnackbarEffect(
        errorTick = state.playGamesSignInErrorTick,
        hostState = snackbarHostState,
        onErrorShown = onPlayGamesSignInErrorShown,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { ClaySnackbarHost(snackbarHostState) },
        topBar = { SettingsTopBar(onBack = onBack) },
    ) { contentPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            // Scrollable because the column now holds six elements rather than two: without
            // it the last row — which is the CONS-03 privacy entry point — falls below the
            // fold on a short screen, in landscape, or at a large font scale, with no way to
            // reach it. A compliance affordance the player cannot scroll to is the same
            // dead end as one that opens nothing.
            Column(
                modifier =
                    Modifier
                        .widthIn(max = dimensionResource(R.dimen.content_max_width))
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(dimensionResource(R.dimen.spacing_md))
                        .testTag(SETTINGS_CONTENT_TAG),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
            ) {
                GeneralSection(
                    soundOn = state.soundOn,
                    hapticsOn = state.hapticsOn,
                    colorblindPaletteOn = state.colorblindPaletteOn,
                    onToggleSound = onToggleSound,
                    onToggleHaptics = onToggleHaptics,
                    onToggleColorblindPalette = onToggleColorblindPalette,
                    onShowHowToPlay = onShowHowToPlay,
                )
                PlayGamesSection(
                    gamesSignInState = state.gamesSignInState,
                    onSignInToPlayGames = onSignInToPlayGames,
                    onShowLeaderboards = onShowLeaderboards,
                    onShowAchievements = onShowAchievements,
                )
                AboutSection(onOpenLicenses = onOpenLicenses)
                PrivacySection(
                    privacyOptionsRequired = state.privacyOptionsRequired,
                    snackbarHostState = snackbarHostState,
                    onManagePrivacyChoices = onManagePrivacyChoices,
                )
            }
        }
    }
}

/**
 * The "General" group: the header, the "How to play" row, and the three toggle rows — Sound,
 * Haptics and the colour-blind palette.
 *
 * Emits its children straight into the caller's [Column], so the screen's
 * `Arrangement.spacedBy` and the [clayEntrance] cascade still apply — extracting the group
 * changes how the file reads, not how the screen lays out.
 *
 * ## The "How to play" row (ONB-01, D-02a, ux-spec §4.5)
 *
 * It opens the group, **above** Sound, because it is the only row here that goes somewhere:
 * the three toggles are settings the player adjusts in place, and a navigational row buried
 * under them reads as an afterthought. It is also the tutorial's one permanent entry point
 * now that Home's help icon is deleted (D-02), which is why it is a `SettingsLinkRow` — the
 * exact peer chrome of the toggles — rather than a bespoke card that would read as a
 * different kind of thing.
 *
 * Unlike every other row on this screen it **speaks**: `SoundCue.ButtonForward`, because
 * D-09 gives a navigational CTA the forward press cue and withholds it from toggles and
 * back-navigation. The cue is attached at this call site through the house `withClickSound`
 * idiom, never to the shared `claySquish()` below it — that modifier is also on the silent
 * back nav and on every switch.
 *
 * ## The colour-blind palette row (docs/ux/colour-vision.md §3.1, CV-03)
 *
 * Directly after Haptics: General already holds what the player tunes about how the game feels,
 * and a section of its own would be a header over one row. It is the exact peer of Sound and
 * Haptics — the switch is the target and carries the label, so TalkBack says "Colorblind palette,
 * switch" with its state — and, like Haptics, it is silent (D-09). Its effect is immediate:
 * Settings itself shows no change but the switch, and the next frame of any board, star row or
 * pip row draws in the chosen palette.
 */
@Composable
private fun GeneralSection(
    soundOn: Boolean,
    hapticsOn: Boolean,
    colorblindPaletteOn: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onToggleColorblindPalette: (Boolean) -> Unit,
    onShowHowToPlay: () -> Unit,
) {
    SettingsSectionHeader(
        title = stringResource(R.string.settings_section_general),
        entranceOrder = GENERAL_HEADER_ORDER,
    )
    SettingsLinkRow(
        label = stringResource(R.string.settings_how_to_play_label),
        supporting = stringResource(R.string.settings_how_to_play_supporting),
        entranceOrder = HOW_TO_PLAY_ROW_ORDER,
        trailingIcon = null,
        onClick = withClickSound(SoundCue.ButtonForward, onShowHowToPlay),
    )
    SettingsToggleRow(
        label = stringResource(R.string.settings_sound_label),
        supporting = stringResource(R.string.settings_sound_supporting),
        checked = soundOn,
        entranceOrder = SOUND_ROW_ORDER,
        onCheckedChange = onToggleSound,
    )
    SettingsToggleRow(
        label = stringResource(R.string.settings_haptics_label),
        supporting = stringResource(R.string.settings_haptics_supporting),
        checked = hapticsOn,
        entranceOrder = HAPTICS_ROW_ORDER,
        onCheckedChange = onToggleHaptics,
    )
    SettingsToggleRow(
        label = stringResource(R.string.settings_colorblind_label),
        supporting = stringResource(R.string.settings_colorblind_supporting),
        checked = colorblindPaletteOn,
        entranceOrder = COLORBLIND_ROW_ORDER,
        onCheckedChange = onToggleColorblindPalette,
    )
}

/**
 * The "Play Games" group (D-08): a header over exactly one row, whose identity — and whose
 * very existence — is a pure function of the observed session.
 *
 * ## Absent, not empty, while Unknown (D-11)
 *
 * The early return is the first statement in the function, before the header, so an
 * unresolved session emits *nothing at all*: no header, no row, no placeholder, and no
 * `Arrangement.spacedBy` gap where a section will later be. An opted-in player's silent
 * launch check can still be pending when Settings opens, and the honest rendering of "we do
 * not know yet" is silence — a sign-in row offered to someone already signed in, or a
 * signed-in row shown to someone who is not, is a claim this screen has no basis for.
 * [SettingsUiState.gamesSignInState] defaults to [GamesSignInState.Unknown] for exactly this
 * reason, so the absent state is also the pre-first-emission state.
 *
 * ## Exhaustive `when`, deliberately without an `else`
 *
 * [GamesSignInState] is a sealed interface, so listing all three members and no `else` makes
 * a future fourth member a **compile** error here rather than a silently-skipped branch that
 * ships a headed section with nothing under it. [GamesSignInState.Unknown] is therefore
 * still listed — unreachable past the guard above, and kept solely to hold that gate open.
 *
 * ## The signed-in row is a statement, not an affordance
 *
 * D-06 asks for a non-tappable row, so its `onClick` is `null` — which strips the click
 * action, the focus stop and the squish from the card (WR-06), leaving it inert to TalkBack
 * and the D-pad rather than a tap that merely does nothing. It stays a [SettingsLinkRow]
 * rather than becoming a bespoke composable because the two states must be exact visual
 * peers: a differently-shaped card would read as a different *kind* of thing appearing when
 * the player signs in, when in truth one row has simply changed what it says. Its label is
 * the player's own Play Games display name, or the neutral D-14 fallback used whole when
 * that lookup failed — a name that never arrived is not a failed sign-in and must not be
 * rendered as one, which is why there is no "Signed in as (unknown)" form. The name is a
 * free-text value from Google's service crossing a trust boundary: it is rendered as plain
 * [Text] through a [stringResource] format (Compose escapes it, and it is never parsed as
 * markup) and is never logged (T-11-11).
 *
 * The row is a genuine re-entry point and never a nag: it renders identically on every
 * visit, carries no dismiss counter and no re-prompt logic, and an explicit decline leaves
 * it exactly where it was (D-13, T-11-12).
 *
 * The two stock-UI rows below the signed-in statement row (Phase 12 D-14/D-15) exist in the
 * `SignedIn` branch **only**, and their absence elsewhere is the design rather than an
 * oversight: signed out they do not exist at all — not disabled, not empty — because the
 * sign-in row directly above already says what to do, and a greyed-out "Leaderboards"
 * explains nothing while still looking like a broken feature. While the session is
 * [GamesSignInState.Unknown] the guard above hides the whole section, these rows included.
 *
 * They render straight off [gamesSignInState], so there is no second copy of "is the player
 * signed in" that could fall out of step with the row above them.
 *
 * @param gamesSignInState the observed Play Games session; the section's sole input.
 * @param onSignInToPlayGames invoked by the signed-out row's tap (D-10/D-19).
 * @param onShowLeaderboards invoked by the signed-in Leaderboards row's tap (PGSL-06).
 * @param onShowAchievements invoked by the signed-in Achievements row's tap (PGSA-05).
 */
@Composable
private fun PlayGamesSection(
    gamesSignInState: GamesSignInState,
    onSignInToPlayGames: () -> Unit,
    onShowLeaderboards: () -> Unit,
    onShowAchievements: () -> Unit,
) {
    if (gamesSignInState == GamesSignInState.Unknown) return

    SettingsSectionHeader(
        title = stringResource(R.string.settings_section_play_games),
        entranceOrder = PLAY_GAMES_HEADER_ORDER,
    )
    when (gamesSignInState) {
        is GamesSignInState.SignedIn -> {
            SettingsLinkRow(
                label =
                    gamesSignInState.displayName
                        ?.let { stringResource(R.string.settings_play_games_signed_in_as, it) }
                        ?: stringResource(R.string.settings_play_games_signed_in_fallback),
                supporting = stringResource(R.string.settings_play_games_signin_supporting),
                entranceOrder = PLAY_GAMES_ROW_ORDER,
                trailingIcon = null,
                // Still a statement, not an offer: the rows below are what the player taps.
                onClick = null,
            )
            SettingsLinkRow(
                label = stringResource(R.string.settings_play_games_leaderboards_label),
                supporting = stringResource(R.string.settings_play_games_leaderboards_supporting),
                entranceOrder = PLAY_GAMES_LEADERBOARDS_ROW_ORDER,
                trailingIcon = null,
                onClick = onShowLeaderboards,
            )
            SettingsLinkRow(
                label = stringResource(R.string.settings_play_games_achievements_label),
                supporting = stringResource(R.string.settings_play_games_achievements_supporting),
                entranceOrder = PLAY_GAMES_ACHIEVEMENTS_ROW_ORDER,
                trailingIcon = null,
                onClick = onShowAchievements,
            )
        }

        GamesSignInState.SignedOut ->
            SettingsLinkRow(
                label = stringResource(R.string.settings_play_games_signin_label),
                supporting = stringResource(R.string.settings_play_games_signin_supporting),
                entranceOrder = PLAY_GAMES_ROW_ORDER,
                trailingIcon = null,
                onClick = onSignInToPlayGames,
            )

        // Unreachable — the guard above already returned. Present only so the `when` stays
        // exhaustive without an `else`, which is what makes a future member a build failure.
        GamesSignInState.Unknown -> Unit
    }
}

/**
 * The "About" group (spec LS-01, §2): a header over the one Licenses row, after Play Games — or
 * after General while Play Games is absent (D-11) — and before Privacy, which stays pinned last.
 *
 * The row is the "How to play" row's exact shape: a `SettingsLinkRow` with no trailing glyph,
 * because it opens in-app (the browser-bound policy row is the one with a glyph), and the forward
 * cue attached at this call site through `withClickSound` (D-09), never to the shared squish.
 */
@Composable
private fun AboutSection(onOpenLicenses: () -> Unit) {
    SettingsSectionHeader(
        title = stringResource(R.string.settings_section_about),
        entranceOrder = ABOUT_HEADER_ORDER,
    )
    SettingsLinkRow(
        label = stringResource(R.string.settings_licenses_label),
        supporting = stringResource(R.string.settings_licenses_supporting),
        entranceOrder = LICENSES_ROW_ORDER,
        trailingIcon = null,
        onClick = withClickSound(SoundCue.ButtonForward, onOpenLicenses),
    )
}

/**
 * Shows the D-10 sign-in error once per raised [errorTick] and hands the ordinal back
 * through [onErrorShown] — the same consumed one-shot-event idiom `HomeScreen`'s
 * `PlayGamesSignInErrorSnackbarEffect` uses, against the same `play_games_signin_error`
 * string, because it is the same event seen from the app's other sign-in entry point. A
 * second string resource for it would be two wordings for one failure the moment either was
 * edited.
 *
 * Consumed rather than left standing (CR-01): a `LaunchedEffect` runs its block whenever it
 * *enters* composition with a given key, so an unconsumed tick would re-announce one failure
 * on every recomposition of this screen — a rotation after a failed tap, here. And the effect
 * is keyed on the host and the text rather than on the tick, because consuming the tick moves
 * it to 0 and an effect keyed on it would be cancelled by that change — which clears the
 * snackbar the frame it appeared. It waits for the next positive ordinal through a
 * [rememberUpdatedState] mirror, consumes it, shows, and waits again; a fresh composition
 * finds a consumed 0 and shows nothing, so no `> 0` opening guard is needed.
 *
 * The text is resolved with [stringResource] **in composition** rather than through
 * `LocalContext.current.getString(...)` inside the effect — `LocalContext` reads are not
 * invalidated by a `Configuration` change, and the pinned Compose BOM fails the build on it
 * (`LocalContextGetResourceValueCall`). Same idiom as the policy-row error below.
 *
 * [SnackbarDuration.Short] and no action: the opt-in already stands, the next launch retries
 * silently, and the row is still right there — there is nothing for the player to do here.
 *
 * @param errorTick the ordinal `SettingsViewModel` raises on a genuine SDK error and resets
 *   on [onErrorShown].
 * @param hostState the snackbar queue hoisted by the screen's `Scaffold`.
 * @param onErrorShown consumes the ordinal — called the moment the message is raised, before
 *   the show suspends, so a recomposition mid-show does not replay it.
 */
@Composable
private fun PlayGamesSignInErrorSnackbarEffect(
    errorTick: Int,
    hostState: SnackbarHostState,
    onErrorShown: () -> Unit,
) {
    val message = stringResource(R.string.play_games_signin_error)
    val latestTick by rememberUpdatedState(errorTick)
    val latestOnErrorShown by rememberUpdatedState(onErrorShown)
    LaunchedEffect(hostState, message) {
        while (true) {
            snapshotFlow { latestTick }.first { it > 0 }
            latestOnErrorShown()
            hostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Short,
            )
        }
    }
}

/**
 * The "Privacy" group: the header, the unconditional policy row (REL-05) and the
 * manage-choices row that exists only while UMP requires it (CONS-03).
 *
 * Owns the browser hand-off because that is the one behaviour on this screen that can fail:
 * `startActivity` is wrapped in `runCatching`, and a device with no browser gets the
 * [ClaySnackbarHost] message rather than a tap that silently does nothing (T-09-28).
 */
@Composable
private fun PrivacySection(
    privacyOptionsRequired: Boolean,
    snackbarHostState: SnackbarHostState,
    onManagePrivacyChoices: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Resolved in composition, never with LocalContext.getString(...) inside the click
    // lambda: LocalContext reads are not invalidated by a Configuration change, so the
    // latter can hand a stale-locale string to the snackbar — and the pinned Compose BOM
    // fails the build on it (`LocalContextGetResourceValueCall`). Same idiom as GameScreen.
    val policyErrorMessage = stringResource(R.string.settings_privacy_policy_error)

    SettingsSectionHeader(
        title = stringResource(R.string.settings_section_privacy),
        entranceOrder = PRIVACY_HEADER_ORDER,
    )
    SettingsLinkRow(
        label = stringResource(R.string.settings_privacy_policy_label),
        supporting = stringResource(R.string.settings_privacy_policy_supporting),
        entranceOrder = PRIVACY_POLICY_ROW_ORDER,
        trailingIcon = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(dimensionResource(R.dimen.settings_row_icon_size)),
            )
        },
        onClick = {
            val opened = runCatching { context.startActivity(privacyPolicyIntent()) }
            if (opened.isFailure) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = policyErrorMessage,
                        duration = SnackbarDuration.Short,
                    )
                }
            }
        },
    )
    if (privacyOptionsRequired) {
        SettingsLinkRow(
            label = stringResource(R.string.settings_privacy_choices_label),
            supporting = stringResource(R.string.settings_privacy_choices_supporting),
            entranceOrder = PRIVACY_CHOICES_ROW_ORDER,
            trailingIcon = null,
            onClick = onManagePrivacyChoices,
        )
    }
}

/**
 * The external-browser intent for the privacy policy (REL-05, D-15, T-09-27).
 *
 * `FLAG_ACTIVITY_NEW_TASK` is required because the receiver is a `Context` rather than an
 * `Activity` — and a `Context` is all this screen may hold, since
 * `PlatformBoundaryRules.uiAndDomainNeverTouchAnActivity` is a bytecode build gate. The URI
 * is [PrivacyLinks.POLICY_URL], a compile-time constant; nothing dynamic may ever reach an
 * implicit `ACTION_VIEW` here.
 */
private fun privacyPolicyIntent(): Intent =
    Intent(Intent.ACTION_VIEW, PrivacyLinks.POLICY_URL.toUri())
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

/** The Settings top bar: the screen title and a back nav that returns to Home. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack, modifier = Modifier.claySquish()) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.navigate_back),
                    modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
    )
}

/**
 * Entrance stagger order of every element in the Settings column, top to bottom (UI-SPEC §3).
 *
 * Named constants rather than literals (no magic numbers), and a contiguous 0-13
 * set including the headers, so the [clayEntrance] cascade reads strictly downward. The
 * conditional elements keep their orders even when they are not rendered — the orders
 * describe position in the layout, not the count of visible children, which is also why the
 * whole Play Games section being absent (D-11), or its two signed-in-only rows being absent
 * (Phase 12 D-15), leaves the Privacy orders untouched.
 *
 * Inserting anything above Privacy is therefore a **renumbering**, not an append. Play Games
 * arriving between General and Privacy shifted the three Privacy constants up by two; the
 * Phase-12 stock-UI rows shifted them by two again, to 7-9; the Phase-15 "How to play" row
 * opening the General group shifted **every** later constant by one, to 2-10; the colour-blind
 * palette row after Haptics (docs/ux/colour-vision.md §3.1) and the About section above Privacy
 * (docs/ux/licences-screen.md LS-01) then renumbered the run whole, the palette row to 4, Play
 * Games to 5-8, About to 9-10 and Privacy to 11-13. Privacy stays
 * pinned last (Phase 9 D-14), so new rows go above it and never between it and the bottom.
 * That is the
 * price of the one invariant this block exists to keep — position in the column and stagger
 * order are the same ordering, so nothing can fade in above something that arrived before it.
 *
 * A **half**-renumbering is the failure mode worth naming: it shows only as two rows entering
 * together, which no cue, callback or copy assertion can see. `SettingsScreenTest` reads the
 * rendered top coordinates instead, so it fails on a half-renumbering and survives a whole one.
 */
private const val GENERAL_HEADER_ORDER = 0
private const val HOW_TO_PLAY_ROW_ORDER = 1
private const val SOUND_ROW_ORDER = 2
private const val HAPTICS_ROW_ORDER = 3
private const val COLORBLIND_ROW_ORDER = 4
private const val PLAY_GAMES_HEADER_ORDER = 5
private const val PLAY_GAMES_ROW_ORDER = 6
private const val PLAY_GAMES_LEADERBOARDS_ROW_ORDER = 7
private const val PLAY_GAMES_ACHIEVEMENTS_ROW_ORDER = 8
private const val ABOUT_HEADER_ORDER = 9
private const val LICENSES_ROW_ORDER = 10
private const val PRIVACY_HEADER_ORDER = 11
private const val PRIVACY_POLICY_ROW_ORDER = 12
private const val PRIVACY_CHOICES_ROW_ORDER = 13
