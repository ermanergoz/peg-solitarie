package com.app.pegsolitaire.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.audio.withClickSound
import com.app.pegsolitaire.ui.onboarding.OnboardingOverlay
import com.app.pegsolitaire.ui.theme.ClayAnim
import com.app.pegsolitaire.ui.theme.clayEntrance
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.theme.ctaGradientBrush
import com.app.pegsolitaire.ui.theme.goldGradientFill
import com.app.pegsolitaire.ui.theme.rememberAnimationsEnabled

/**
 * The stateless Home mode-select screen (NAV-01, D-09/D-10/D-14/D-15), in the
 * Candy-Clay skin (DESIGN-SYSTEM.md Home).
 *
 * Renders [state] and forwards events; owns no logic (state hoisting). An M3
 * [Scaffold] hosts a [TopAppBar] (gradient Fredoka title, a trailing How-to-play
 * affordance — D-15 — and a Settings gear opening the Settings destination, Phase-5
 * D-14) over a centered column, capped at `content_max_width`, of two chunky clay
 * mode tiles: a violet Classic tile whose whole surface navigates to the catalog
 * ([onPlayClassic]) and a pink Challenge tile (with its persisted-level chip) whose
 * whole surface enters Challenge ([onPlayChallenge]) at the player's persisted
 * [HomeUiState.challengeCurrentLevel] (CHAL-05). Two slow-drifting low-alpha blobs
 * float behind the content (skipped when OS animations are off), the tiles enter
 * with a 40ms stagger, and everything tappable squishes. While
 * [HomeUiState.onboardingVisible], the [OnboardingOverlay] is hosted over the body
 * (first-run, D-14, or the help affordance, D-15); dismissing it calls
 * [onDismissOnboarding] (which `markSeen()`s, D-13).
 *
 * Above the mode tiles sit up to two gold "Continue" chips (PERS-03, D-01/D-03) — one per
 * mode holding a saved in-progress game. They are independent: a Classic and a Challenge
 * game can both be waiting, and each carries what its resume needs (the stored board id /
 * the persisted level) so the host never has to guess.
 *
 * @param state the immutable Home UI state (the onboarding gate + the persisted level).
 * @param onPlayClassic navigates to the Classic catalog (the screen's primary affordance).
 * @param onPlayChallenge enters Challenge at the level it is handed — the persisted
 *   [HomeUiState.challengeCurrentLevel] (CHAL-05); the screen never invents the level.
 * @param onResumeClassic continues the saved Classic game on the board id it is handed —
 *   [HomeUiState.classicResumeLevelId], the board the snapshot was taken on (PERS-03).
 * @param onResumeChallenge continues the saved Challenge game at the snapshot's own
 *   stamped level ([HomeUiState.challengeResumeLevel], WR-03) — never the ladder level.
 * @param onShowHowToPlay re-opens the onboarding overlay any time (D-15).
 * @param onDismissOnboarding dismisses + persists the onboarding (D-13).
 * @param onOpenSettings opens the Settings destination from the top-bar gear (Phase-5
 *   D-14); a hoisted lambda supplied by `AppNavHost` — the screen never sees a controller.
 * @param modifier layout modifier for the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onPlayClassic: () -> Unit,
    onPlayChallenge: (level: Int) -> Unit,
    onResumeClassic: (levelId: String) -> Unit,
    onResumeChallenge: (level: Int) -> Unit,
    onShowHowToPlay: () -> Unit,
    onDismissOnboarding: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            HomeTopBar(
                onShowHowToPlay = onShowHowToPlay,
                onOpenSettings = onOpenSettings,
            )
        },
    ) { contentPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            HomeBlobs()
            HomeTiles(
                state = state,
                onPlayClassic = onPlayClassic,
                onPlayChallenge = onPlayChallenge,
                onResumeClassic = onResumeClassic,
                onResumeChallenge = onResumeChallenge,
            )
        }
    }

    if (state.onboardingVisible) {
        OnboardingOverlay(onDismiss = onDismissOnboarding)
    }
}

/**
 * The centered stack of Continue chips + mode tiles. Up to two Continue affordances sit
 * above the mode tiles: whatever the player left unfinished is the first thing they see
 * (D-01/D-03). The entrance stagger order counts only the tiles actually shown.
 */
@Composable
private fun HomeTiles(
    state: HomeUiState,
    onPlayClassic: () -> Unit,
    onPlayChallenge: (level: Int) -> Unit,
    onResumeClassic: (levelId: String) -> Unit,
    onResumeChallenge: (level: Int) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .widthIn(max = dimensionResource(R.dimen.content_max_width))
                .padding(dimensionResource(R.dimen.spacing_md)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
    ) {
        var entranceOrder = 0
        val resumeLevelId = state.classicResumeLevelId
        if (resumeLevelId != null) {
            ResumeTile(
                title = stringResource(R.string.resume_classic_title),
                supporting = stringResource(R.string.resume_classic_supporting),
                entranceOrder = entranceOrder++,
                onResume = { onResumeClassic(resumeLevelId) },
            )
        }
        // The Challenge Continue carries the SNAPSHOT's stamped level (WR-03),
        // never challengeCurrentLevel — the ladder flow can lag at a cold start
        // while the stamp always names the level the saved board really is.
        val challengeResumeLevel = state.challengeResumeLevel
        if (challengeResumeLevel != null) {
            ResumeTile(
                title = stringResource(R.string.resume_challenge_title),
                supporting =
                    stringResource(
                        R.string.resume_challenge_supporting,
                        challengeResumeLevel,
                    ),
                entranceOrder = entranceOrder++,
                onResume = { onResumeChallenge(challengeResumeLevel) },
            )
        }
        ClassicTile(entranceOrder = entranceOrder++, onPlayClassic = onPlayClassic)
        ChallengeTile(
            currentLevel = state.challengeCurrentLevel,
            entranceOrder = entranceOrder,
            onPlayChallenge = onPlayChallenge,
        )
    }
}

/**
 * The Home top bar: the gradient Fredoka app title, the permanent How-to-play
 * affordance (D-15) and the Settings gear (Phase-5 D-14) — both with the clay squish.
 * The gear uses the curated core Material icon set already on the classpath
 * (`Icons.Rounded.Settings`) — no `material-icons-extended` dependency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    onShowHowToPlay: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.titleLarge.merge(TextStyle(brush = ctaGradientBrush())),
            )
        },
        actions = {
            IconButton(onClick = onShowHowToPlay, modifier = Modifier.claySquish()) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = stringResource(R.string.how_to_play),
                    modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
                )
            }
            IconButton(onClick = onOpenSettings, modifier = Modifier.claySquish()) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.settings_action),
                    modifier = Modifier.size(dimensionResource(R.dimen.top_bar_icon_size)),
                )
            }
        },
    )
}

/**
 * Two slow-drifting, low-alpha clay blobs floating behind the Home content
 * (DESIGN-SYSTEM.md Home: 2 blobs, ±16dp drift over ~8s). Purely decorative —
 * no semantics — and skipped entirely when OS animations are off.
 */
@Composable
private fun HomeBlobs() {
    if (!rememberAnimationsEnabled()) return
    val drift = rememberInfiniteTransition(label = "homeBlobDrift")
    val progress by
        drift.animateFloat(
            initialValue = -DRIFT_EXTENT,
            targetValue = DRIFT_EXTENT,
            animationSpec =
                infiniteRepeatable(
                    animation = tween(ClayAnim.BLOB_DRIFT_MS, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
            label = "homeBlobOffset",
        )
    val blobSize = dimensionResource(R.dimen.blob_size)
    val driftRange = dimensionResource(R.dimen.blob_drift_range)
    Box(modifier = Modifier.fillMaxSize()) {
        Blob(
            color = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        x = blobSize * BLOB_PEEK_FRACTION + driftRange * progress,
                        y = -blobSize * BLOB_PEEK_FRACTION - driftRange * progress,
                    ),
        )
        Blob(
            color = MaterialTheme.colorScheme.secondary,
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .offset(
                        x = -blobSize * BLOB_PEEK_FRACTION - driftRange * progress,
                        y = blobSize * BLOB_PEEK_FRACTION + driftRange * progress,
                    ),
        )
    }
}

/** One soft circular background blob at [ClayAnim.BLOB_ALPHA]. */
@Composable
private fun Blob(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(dimensionResource(R.dimen.blob_size))
                .background(color = color.copy(alpha = ClayAnim.BLOB_ALPHA), shape = CircleShape),
    )
}

/**
 * A gold "Continue" clay chip for one mode's saved in-progress game (PERS-03, D-01).
 *
 * A `tertiaryContainer` (reward-gold) [Card] — visually distinct from the mode tiles,
 * because it is an offer to return to something rather than a mode choice — whose whole
 * surface resumes, with a gold-gradient play glyph as the trailing accent. Stateless: the
 * caller decides whether it renders at all and what [onResume] carries.
 */
@Composable
private fun ResumeTile(
    title: String,
    supporting: String,
    entranceOrder: Int,
    onResume: () -> Unit,
) {
    // SOUND-04 / D-09 / D-11: Continue is a navigational CTA, so it clicks. The cue is attached
    // per callsite and NEVER to Modifier.claySquish() below it — that modifier is also on the
    // top-bar How-to-play and Settings icons, which D-09 requires to stay silent (the full
    // exclusion list is in the withClickSound KDoc). Resume does not route through ModeTile,
    // so it carries its own wrap.
    Card(
        onClick = withClickSound(SoundCue.ButtonForward, onResume),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier =
            Modifier
                .clayEntrance(entranceOrder)
                .claySquish()
                .clayShadow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.large)
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.resume_tile_min_height)),
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
                modifier = Modifier.weight(CONTENT_WEIGHT),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null,
                modifier =
                    Modifier
                        .goldGradientFill()
                        .size(dimensionResource(R.dimen.mode_glyph_size)),
            )
        }
    }
}

/**
 * The violet Classic clay tile (D-10): a `primaryContainer` [Card] with a grid glyph
 * badge whose whole surface navigates to the catalog. Title "Classic" + supporting
 * "5 boards". The screen's primary affordance.
 */
@Composable
private fun ClassicTile(
    entranceOrder: Int,
    onPlayClassic: () -> Unit,
) {
    ModeTile(
        container = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        badgeColor = MaterialTheme.colorScheme.primary,
        badgeContentColor = MaterialTheme.colorScheme.onPrimary,
        glyph = rememberClassicGlyph(),
        title = stringResource(R.string.classic_tile_title),
        entranceOrder = entranceOrder,
        onClick = onPlayClassic,
    ) {
        Text(
            text = stringResource(R.string.classic_tile_supporting),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/**
 * The pink Challenge clay tile (D-04): a `secondaryContainer` [Card] — an exact peer of
 * [ClassicTile] — with a lightning glyph badge, whose whole surface enters Challenge at
 * [currentLevel], the player's persisted ladder position (CHAL-05). The persisted level
 * rides a soft level chip ("Continue — Level N"), so the stored progress is visible, not
 * just used. Same `mode_tile_min_height` footprint as Classic.
 */
@Composable
private fun ChallengeTile(
    currentLevel: Int,
    entranceOrder: Int,
    onPlayChallenge: (level: Int) -> Unit,
) {
    ModeTile(
        container = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        badgeColor = MaterialTheme.colorScheme.secondary,
        badgeContentColor = MaterialTheme.colorScheme.onSecondary,
        glyph = rememberChallengeGlyph(),
        title = stringResource(R.string.challenge_tile_title),
        entranceOrder = entranceOrder,
        onClick = { onPlayChallenge(currentLevel) },
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Text(
                text = stringResource(R.string.challenge_tile_continue, currentLevel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier =
                    Modifier.padding(
                        horizontal = dimensionResource(R.dimen.spacing_sm),
                        vertical = dimensionResource(R.dimen.spacing_xs),
                    ),
            )
        }
    }
}

/**
 * One chunky clay mode tile: a soft-shadowed, squishy [Card] in the mode's container
 * color, leading with a rounded glyph badge beside the [title] and the mode-specific
 * [supporting] slot (plain text for Classic, the level chip for Challenge).
 */
@Composable
private fun ModeTile(
    container: Color,
    contentColor: Color,
    badgeColor: Color,
    badgeContentColor: Color,
    glyph: ImageVector,
    title: String,
    entranceOrder: Int,
    onClick: () -> Unit,
    supporting: @Composable () -> Unit,
) {
    // Wrapped at the shared leaf, so Classic and Challenge each inherit the forward click once.
    Card(
        onClick = withClickSound(SoundCue.ButtonForward, onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = container),
        modifier =
            Modifier
                .clayEntrance(entranceOrder)
                .claySquish()
                .clayShadow(dimensionResource(R.dimen.elevation_card), MaterialTheme.shapes.large)
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.mode_tile_min_height)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.spacing_md)),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = badgeColor,
            ) {
                Icon(
                    imageVector = glyph,
                    contentDescription = null,
                    tint = badgeContentColor,
                    modifier =
                        Modifier
                            .padding(dimensionResource(R.dimen.spacing_sm))
                            .size(dimensionResource(R.dimen.mode_glyph_size)),
                )
            }
            Column(
                modifier = Modifier.weight(CONTENT_WEIGHT),
                verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xs)),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                )
                supporting()
            }
        }
    }
}

/** The text column takes all width left of the fixed-size badge/accent. */
private const val CONTENT_WEIGHT = 1f

/** How far a background blob's center peeks past its screen corner. */
private const val BLOB_PEEK_FRACTION = 0.3f

/** Blob drift progress runs −1…+1 of the `blob_drift_range` (±16dp). */
private const val DRIFT_EXTENT = 1f
