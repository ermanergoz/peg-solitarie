package com.app.pegsolitaire.ui.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.app.pegsolitaire.R

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
 * @param state the immutable Settings UI state (the two persisted flags).
 * @param onToggleSound persists the new sound choice (SET-01).
 * @param onToggleHaptics persists the new haptics choice (SET-02).
 * @param onBack leaves Settings (top-bar back nav → pops to Home).
 * @param modifier layout modifier for the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onToggleSound: (Boolean) -> Unit,
    onToggleHaptics: (Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { SettingsTopBar(onBack = onBack) },
    ) { contentPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .widthIn(max = dimensionResource(R.dimen.content_max_width))
                        .padding(dimensionResource(R.dimen.spacing_md)),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
            ) {
                SettingsToggleRow(
                    label = stringResource(R.string.settings_sound_label),
                    supporting = stringResource(R.string.settings_sound_supporting),
                    checked = state.soundOn,
                    onCheckedChange = onToggleSound,
                )
                SettingsToggleRow(
                    label = stringResource(R.string.settings_haptics_label),
                    supporting = stringResource(R.string.settings_haptics_supporting),
                    checked = state.hapticsOn,
                    onCheckedChange = onToggleHaptics,
                )
            }
        }
    }
}

/** The Settings top bar: the screen title and a back nav that returns to Home. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsTopBar(onBack: () -> Unit) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
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
 * One setting row: a `surfaceContainerHigh` [Card] holding the [label] + [supporting]
 * copy beside an M3 [Switch]. Fully hoisted — the row holds no state of its own; the
 * switch renders [checked] and reports every flip through [onCheckedChange]. The switch
 * carries [label] as its content description so screen readers announce which setting it
 * controls rather than a bare "switch".
 */
@Composable
private fun SettingsToggleRow(
    label: String,
    supporting: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        modifier =
            Modifier
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
                modifier = Modifier.semantics { contentDescription = label },
            )
        }
    }
}
