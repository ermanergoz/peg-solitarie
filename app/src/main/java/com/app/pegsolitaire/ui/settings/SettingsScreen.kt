package com.app.pegsolitaire.ui.settings

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.core.net.toUri
import com.app.pegsolitaire.R
import com.app.pegsolitaire.ui.components.ClaySnackbarHost
import com.app.pegsolitaire.ui.theme.clayEntrance
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish
import kotlinx.coroutines.launch

/**
 * The stateless Settings screen (SET-01, SET-02, D-14).
 *
 * Renders [state] and forwards events; owns no logic and no ViewModel (state hoisting —
 * the switches' `checked` values come from [state] and every flip leaves through
 * [onToggleSound]/[onToggleHaptics]). An M3 [Scaffold] hosts a [TopAppBar] (title + back
 * nav) over a centered column, capped at `content_max_width`, of two toggle rows that are
 * exact peers: Sound and Haptics.
 *
 * Sound persists the flag only this phase — there is no audio engine yet (D-16) — so its
 * supporting line says so rather than implying playback that does not exist; Haptics is
 * live (D-15) and its supporting line describes the real jump feedback.
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
 * @param state the immutable Settings UI state (the two persisted flags plus the CONS-03 flag).
 * @param onToggleSound persists the new sound choice (SET-01).
 * @param onToggleHaptics persists the new haptics choice (SET-02).
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
    onManagePrivacyChoices: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }

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
                        .fillMaxWidth()
                        .widthIn(max = dimensionResource(R.dimen.content_max_width))
                        .verticalScroll(rememberScrollState())
                        .padding(dimensionResource(R.dimen.spacing_md)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
            ) {
                GeneralSection(
                    soundOn = state.soundOn,
                    hapticsOn = state.hapticsOn,
                    onToggleSound = onToggleSound,
                    onToggleHaptics = onToggleHaptics,
                )
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
 * The "General" group: the header over the two existing, unchanged toggle rows.
 *
 * Emits its children straight into the caller's [Column], so the screen's
 * `Arrangement.spacedBy` and the [clayEntrance] cascade still apply — extracting the group
 * changes how the file reads, not how the screen lays out.
 */
@Composable
private fun GeneralSection(
    soundOn: Boolean,
    hapticsOn: Boolean,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
) {
    SettingsSectionHeader(
        title = stringResource(R.string.settings_section_general),
        entranceOrder = GENERAL_HEADER_ORDER,
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
 * One setting row: a soft-shadowed `surfaceContainerHigh` clay [Card] holding the [label]
 * + [supporting] copy beside a playful [Switch] (violet track when on, clay-neutral when
 * off, with the shared squish on the thumb). Fully hoisted — the row holds no state of its
 * own; the switch renders [checked] and reports every flip through [onCheckedChange]. The
 * switch carries [label] as its content description so screen readers announce which
 * setting it controls rather than a bare "switch". Rows enter with the shared clay
 * stagger ([entranceOrder]).
 */
@Composable
private fun SettingsToggleRow(
    label: String,
    supporting: String,
    checked: Boolean,
    entranceOrder: Int,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier =
            Modifier
                .clayEntrance(entranceOrder)
                .clayShadow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.large)
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.settings_row_min_height)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.spacing_md)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors =
                    SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                        uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                modifier =
                    Modifier
                        .claySquish()
                        .semantics { contentDescription = label },
            )
        }
    }
}

/**
 * One section label ("General" / "Privacy"), in the app's shipped caption style
 * (`labelLarge` / `onSurfaceVariant`, as `ResultsScreen`'s metric labels use).
 *
 * It joins the [clayEntrance] cascade rather than sitting outside it, so the stagger still
 * reads strictly top-to-bottom: a header that appeared instantly above rows that fade in
 * would break the one visual rule the ordering exists to keep.
 */
@Composable
private fun SettingsSectionHeader(
    title: String,
    entranceOrder: Int,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier =
            Modifier
                .clayEntrance(entranceOrder)
                .padding(horizontal = dimensionResource(R.dimen.spacing_sm)),
    )
}

/**
 * One link row: the exact peer of [SettingsToggleRow] — same clay [Card] chrome, same inner
 * padding, typography and entrance stagger — differing in exactly one way: an optional
 * [trailingIcon] slot replaces the [Switch].
 *
 * The whole card is the tap target (`clickable` + [claySquish]), so the row clears the 48dp
 * minimum by a wide margin via `settings_row_min_height`; no icon-sized target is
 * introduced. Fully hoisted: the row owns no state and decides nothing — [onClick] is
 * whatever the screen passed.
 *
 * [trailingIcon] is `null` for the manage-choices row on purpose. The UMP form opens
 * in-app while the policy leaves for a browser, so the glyph's presence or absence is what
 * makes the two rows honestly distinguishable at a glance rather than by label alone.
 */
@Composable
private fun SettingsLinkRow(
    label: String,
    supporting: String,
    entranceOrder: Int,
    trailingIcon: (@Composable () -> Unit)?,
    onClick: () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier =
            Modifier
                .clayEntrance(entranceOrder)
                .clayShadow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.large)
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.settings_row_min_height))
                .claySquish()
                .clickable(onClick = onClick),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.spacing_md)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            trailingIcon?.invoke()
        }
    }
}

/**
 * Entrance stagger order of every element in the Settings column, top to bottom (UI-SPEC §3).
 *
 * Named constants rather than literals (CLAUDE.md: no magic numbers), and a contiguous 0-5
 * set including the headers, so the [clayEntrance] cascade reads strictly downward. The
 * manage-choices row keeps order 5 even when it is not rendered — the orders describe
 * position in the layout, not the count of visible children.
 */
private const val GENERAL_HEADER_ORDER = 0
private const val SOUND_ROW_ORDER = 1
private const val HAPTICS_ROW_ORDER = 2
private const val PRIVACY_HEADER_ORDER = 3
private const val PRIVACY_POLICY_ROW_ORDER = 4
private const val PRIVACY_CHOICES_ROW_ORDER = 5
