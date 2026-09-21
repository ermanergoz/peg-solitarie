package com.app.pegsolitaire.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import com.app.pegsolitaire.R
import com.app.pegsolitaire.audio.SoundCue
import com.app.pegsolitaire.audio.withClickSound
import com.app.pegsolitaire.data.ClassicLevelIds
import com.app.pegsolitaire.domain.model.Board
import com.app.pegsolitaire.domain.model.Boards
import com.app.pegsolitaire.ui.components.ClaySnackbarHost
import com.app.pegsolitaire.ui.theme.AppTheme
import com.app.pegsolitaire.ui.theme.clayEntrance
import com.app.pegsolitaire.ui.theme.clayShadow
import com.app.pegsolitaire.ui.theme.claySquish
import com.app.pegsolitaire.ui.theme.goldGradientFill
import kotlinx.coroutines.flow.first

/**
 * The stateless Home mode-select screen (NAV-01, D-09/D-10/D-14/D-15), in the
 * Candy-Clay skin (DESIGN-SYSTEM.md Home).
 *
 * Renders [state] and forwards events; owns no logic (state hoisting). An M3
 * [Scaffold] hosts a [TopAppBar] (gradient Fredoka title and a Settings gear opening
 * the Settings destination, Phase-5 D-14) over a centered column, capped at
 * `content_max_width`, of two chunky clay
 * mode tiles: a violet Classic tile whose whole surface navigates to the catalog
 * ([onPlayClassic]) and a pink Challenge tile (with its persisted-level chip) whose
 * whole surface enters Challenge ([onPlayChallenge]) at the player's persisted
 * [HomeUiState.challengeCurrentLevel] (CHAL-05). Two slow-drifting low-alpha blobs
 * float behind the content (skipped when OS animations are off), the tiles enter
 * with a 40ms stagger, and everything tappable squishes.
 *
 * **Home carries no tutorial chrome at all** (Phase 15, D-02). There is no information or
 * help icon anywhere on this screen, and no overlay: the tutorial is the `Tutorial` nav
 * destination, auto-started once by `HomeRoute` while [HomeUiState.tutorialPending] stands
 * and reachable afterwards only from Settings › How to play (D-02a). The flag reaches this
 * screen for exactly one purpose — withholding the account-switch question below.
 *
 * Above the mode tiles sit up to two gold "Continue" chips (PERS-03, D-01/D-03) — one per
 * mode holding a saved in-progress game. They are independent: a Classic and a Challenge
 * game can both be waiting, and each carries what its resume needs (the stored board id /
 * the persisted level) so the host never has to guess.
 *
 * Once the tutorial is behind the player and the Play Games question is still unanswered,
 * [HomeUiState.playGamesPromptVisible] raises the [PlayGamesPromptDialog] (PGS-01, D-15).
 * The first-run surfaces are sequenced by the ViewModel, not guarded here, so this stays a
 * plain independent conditional. Behind it sits the account-switch
 * [ProfileSwapDialog] (CLOUD-02, D-12 to D-15) — Home is its only host anywhere in the app.
 * Home also owns its first [ClaySnackbarHost] — the D-18 sign-in error is the only message
 * it has ever needed to show.
 *
 * @param state the immutable Home UI state (the standing tutorial flag + the persisted level).
 * @param onPlayClassic navigates to the Classic catalog (the screen's primary affordance).
 * @param onPlayChallenge enters Challenge at the level it is handed — the persisted
 *   [HomeUiState.challengeCurrentLevel] (CHAL-05); the screen never invents the level.
 * @param onResumeClassic continues the saved Classic game on the board id it is handed —
 *   [HomeUiState.classicResumeLevelId], the board the snapshot was taken on (PERS-03).
 * @param onResumeChallenge continues the saved Challenge game at the snapshot's own
 *   stamped level ([HomeUiState.challengeResumeLevel], WR-03) — never the ladder level.
 * @param onAcceptPlayGamesPrompt the player chose to sign in to Play Games (D-18).
 * @param onDeclinePlayGamesPrompt the player chose "Not now", tapped outside the prompt,
 *   or pressed back (D-16) — all one answer, and a permanent one (D-17).
 * @param onKeepDeviceProgress the player kept this device's progress on the account-switch
 *   dialog (D-13) — and also what a tap outside it or a back press means, since
 *   [ProfileSwapDialog] routes its own dismissal here.
 * @param onUseCloudProgress the player chose the arriving account's cloud progress (D-14),
 *   the deliberate, destructive answer.
 * @param onPlayGamesSignInErrorShown the D-18 error snackbar has been raised for the
 *   current [HomeUiState.playGamesSignInErrorTick]; the ViewModel consumes the ordinal so a
 *   later re-entry to Home does not replay it (CR-01).
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
    onAcceptPlayGamesPrompt: () -> Unit,
    onDeclinePlayGamesPrompt: () -> Unit,
    onKeepDeviceProgress: () -> Unit,
    onUseCloudProgress: () -> Unit,
    onPlayGamesSignInErrorShown: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    PlayGamesSignInErrorSnackbarEffect(
        errorTick = state.playGamesSignInErrorTick,
        hostState = snackbarHostState,
        onErrorShown = onPlayGamesSignInErrorShown,
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { ClaySnackbarHost(snackbarHostState) },
    ) { contentPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            contentAlignment = Alignment.TopCenter,
        ) {
            HomeBlobs()
            HomeBands(
                state = state,
                onPlayClassic = onPlayClassic,
                onPlayChallenge = onPlayChallenge,
                onResumeClassic = onResumeClassic,
                onResumeChallenge = onResumeChallenge,
                onOpenSettings = onOpenSettings,
            )
        }
    }

    // An independent conditional rather than an else-branch: HomeViewModel already guarantees
    // the two dialogs are never true at once (D-15), and expressing that guarantee a second
    // time here would create a place for the two copies to disagree.
    if (state.playGamesPromptVisible) {
        PlayGamesPromptDialog(
            onAccept = onAcceptPlayGamesPrompt,
            onDecline = onDeclinePlayGamesPrompt,
        )
    }

    // A sibling conditional, for the reason the one above is — but with a narrower condition,
    // and that difference is deliberate rather than an oversight. HomeViewModel already
    // sequences this behind both surfaces ahead of it (D-15), so the extra clauses can never
    // change the outcome of a state it produced. They exist because this is the only dialog
    // whose lower option is irreversible, so the screen keeps its own one-way safety over it.
    //
    // The guard SURVIVES IN SHAPE across Phase 15 (§7 D-01f): what it reads is now the
    // standing `tutorialPending` flag rather than the retired overlay's visibility. Its
    // second writer — the permanent How-to-play affordance that could force the old flag
    // true at any time — is deleted by D-02b, so the guard is strictly safer than before: it
    // can only ever withhold the wipe question while the tutorial is still owed, never raise
    // one early. That is the safe direction for the two expressions to disagree in, and the
    // screen half of the never-stacked rule the tests assert on both sides.
    if (state.profileSwapVisible && !state.tutorialPending && !state.playGamesPromptVisible) {
        ProfileSwapDialog(
            accountName = state.profileSwapAccountName,
            onKeepLocal = onKeepDeviceProgress,
            onUseCloud = onUseCloudProgress,
        )
    }
}

/**
 * Shows the D-18 sign-in error once per raised [errorTick], handing the ordinal back through
 * [onErrorShown] so it is consumed rather than left standing — the same one-shot-event idiom
 * `GameScreen`'s `AdMessageSnackbarEffect` uses, and for the same reason: the message is
 * identical every time, so a nullable "current message" would leave no distinguishable state
 * trace between two consecutive failures.
 *
 * ## Why the ordinal is consumed (CR-01)
 *
 * `HomeViewModel` is obtained on the `NavHost` start destination and lives for the whole
 * process, while this screen leaves composition on every forward navigation and re-enters on
 * every pop and every configuration change. A `LaunchedEffect` runs its block whenever it
 * *enters* composition with a given key — not only when the key changes — so an unconsumed
 * tick would re-announce one genuine failure on every later visit to Home for the rest of the
 * session: the phantom-cue bug T-09-40 records on the gameplay side. The `> 0` guard only
 * ever covered the never-failed case.
 *
 * ## Why the effect is not keyed on the tick
 *
 * Consuming the tick moves it back to 0, and an effect keyed on the tick would be cancelled
 * by that very change; `SnackbarHostState.showSnackbar` clears its message on cancellation,
 * so the snackbar would vanish the frame it appeared. The effect is keyed on the host and the
 * text instead, and reads the ordinal through a [rememberUpdatedState] mirror: it waits for
 * the next positive value, consumes it, shows, and waits again — so a failure raised while the
 * message is still up is announced right after it, and a re-entry finds a consumed 0 and
 * shows nothing.
 *
 * The text is resolved with [stringResource] **in composition** rather than through
 * `LocalContext.current.getString(...)` inside the effect: `LocalContext` reads are not
 * invalidated by a `Configuration` change, so the latter can hand a stale-locale string to
 * the snackbar, and the pinned Compose BOM fails the build on it
 * (`LocalContextGetResourceValueCall`). Keying the effect on that text means a locale change
 * re-resolves it without re-showing a message already consumed.
 *
 * [SnackbarDuration.Short] and no action: the opt-in stands regardless, the next launch
 * retries silently, and the Settings row is available meanwhile — there is nothing here
 * for the player to do.
 *
 * @param errorTick the ordinal `HomeViewModel` raises on a genuine SDK error and resets on
 *   [onErrorShown].
 * @param hostState the snackbar queue hoisted by the screen's `Scaffold`.
 * @param onErrorShown consumes the ordinal — called the moment the message is raised, before
 *   the show suspends, so a re-entry mid-show does not replay it.
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
 * Home's five bands (§4.2): hero, progress ribbon, Continue chips, mode tiles, empty footer —
 * one `verticalScroll` column capped at `content_max_width`.
 *
 * ## Why it scrolls, and why the scroll is not a place to put things
 *
 * Home did **not** scroll before this refresh, and at 200% font scale with two Continue chips
 * it already overflowed, so the scroll is a fix rather than polish. It exists for the overflow
 * case only: the first-paint rule (D-03f) requires bands 3 and 4 — the Continue chips and both
 * mode tiles — to be fully within the first viewport at default font scale on a 360x640dp
 * phone, and the hero is the band that yields to keep them there (`home_hero_min_height` is a
 * minimum). A Continue chip reachable only by scrolling is a broken device oracle, not a
 * layout compromise.
 *
 * ## The weighted spacer
 *
 * It sits between bands 2 and 3, so on a tall phone the slack goes *above* the Continue chips
 * and the mode tiles stay in the lower thumb half of a 6.7-inch screen (§11 "One-handed use").
 * `verticalScroll` makes the column's height unbounded, so the spacer needs a
 * `heightIn(min = 0)`-style floor rather than a weight to behave — it is expressed as a
 * `Spacer` inside the arrangement instead, and the column's `Arrangement.spacedBy` does the
 * rest.
 *
 * ## The entrance cascade
 *
 * `clayEntrance` order runs strictly downward — hero 0, ribbon 1, Continue chips 2..3, tiles
 * after — so nothing fades in above something that arrived before it (§10). Conditional chips
 * keep the ordering contiguous by counting only what is shown.
 */
@Composable
private fun HomeBands(
    state: HomeUiState,
    onPlayClassic: () -> Unit,
    onPlayChallenge: (level: Int) -> Unit,
    onResumeClassic: (levelId: String) -> Unit,
    onResumeChallenge: (level: Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .widthIn(max = dimensionResource(R.dimen.content_max_width))
                .padding(dimensionResource(R.dimen.spacing_md)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_md)),
    ) {
        HomeHero(onOpenSettings = onOpenSettings)
        HomeProgressRibbon(
            totalStars = state.totalStars,
            classicBoardsCleared = state.classicBoardsCleared,
            challengeCurrentLevel = state.challengeCurrentLevel,
        )
        // Band 2 -> 3: the slack goes here, above the Continue chips, so the two mode tiles
        // stay in the thumb zone on a tall phone rather than floating mid-screen.
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_lg)))

        var entranceOrder = RIBBON_ENTRANCE_ORDER + 1
        val resumeLevelId = state.classicResumeLevelId
        if (resumeLevelId != null) {
            ResumeTile(
                title = stringResource(R.string.resume_classic_title),
                supporting = stringResource(R.string.resume_classic_supporting),
                entranceOrder = entranceOrder++,
                board = state.classicResumeBoard,
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
                // A procedural level has no memorable footprint, so this chip keeps the
                // generic play arrow: a thumbnail of a board the player cannot recognise
                // would be decoration pretending to be information.
                board = null,
                onResume = { onResumeChallenge(challengeResumeLevel) },
            )
        }
        ClassicTile(entranceOrder = entranceOrder++, onPlayClassic = onPlayClassic)
        ChallengeTile(
            currentLevel = state.challengeCurrentLevel,
            entranceOrder = entranceOrder,
            onPlayChallenge = onPlayChallenge,
        )
        // Band 5 — the footer. Empty, and specified as empty (§4.2) so a later phase reads the
        // gap as reserved rather than as an invitation.
    }
}

/**
 * A gold "Continue" clay chip for one mode's saved in-progress game (PERS-03, D-01).
 *
 * A `tertiaryContainer` (reward-gold) [Card] — visually distinct from the mode tiles,
 * because it is an offer to return to something rather than a mode choice — whose whole
 * surface resumes. Stateless: the caller decides whether it renders at all and what
 * [onResume] carries.
 *
 * **Behaviour, ordering and data are unchanged across Phase 15** (§4.2 band 3, and §4.2.1
 * oracle 3 — the chip's presence, its title, its supporting line and its resume target are
 * what the cloud-save device tests read). The one change is cosmetic: a [board] renders the
 * saved board's own footprint in place of the generic play arrow, so a player recognises
 * *which* board is waiting. `null` keeps the arrow, which is what the Challenge chip wants.
 *
 * @param board the footprint to show as the trailing accent, or `null` for the play arrow.
 */
@Composable
private fun ResumeTile(
    title: String,
    supporting: String,
    entranceOrder: Int,
    board: Board?,
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
            if (board == null) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    modifier =
                        Modifier
                            .goldGradientFill()
                            .size(dimensionResource(R.dimen.mode_glyph_size)),
                )
            } else {
                StaticBoardArt(
                    board = board,
                    modifier = Modifier.size(dimensionResource(R.dimen.home_resume_thumb_size)),
                )
            }
        }
    }
}

/**
 * The violet Classic clay tile (D-10): a `primaryContainer` [Card] with a grid glyph
 * badge whose whole surface navigates to the catalog. Title "Classic" + supporting
 * "5 boards". The screen's primary affordance.
 *
 * Phase 15 adds trailing art only: a **real footprint** at `home_tile_art_size`, drawn from
 * board primitives (§4.2 band 4). The container, badge, title, supporting slot, entrance
 * stagger, squish, `ButtonForward` cue and `onClick` are all unchanged — and the supporting
 * line stays `classic_tile_supporting`, not a progress count, because the catalogue one tap
 * away is where per-board progress lives (§4.2.1 oracle 2).
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
        art = {
            StaticBoardArt(
                board = ClassicTileArtBoard,
                modifier = Modifier.size(dimensionResource(R.dimen.home_tile_art_size)),
            )
        },
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
 *
 * **That level chip is a cloud-save device oracle** (§4.2.1 oracle 1) and is untouched by
 * Phase 15: the ribbon's Level pill reads the same `currentLevel` and is *additive*. The tile
 * gains trailing art like Classic's, but the bolt rather than a footprint — a procedurally
 * generated level has no memorable shape to show.
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
        art = {
            Icon(
                imageVector = rememberChallengeGlyph(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(dimensionResource(R.dimen.home_tile_art_size)),
            )
        },
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
 * [supporting] slot (plain text for Classic, the level chip for Challenge), and closing with
 * the mode's decorative [art].
 *
 * @param art the trailing decoration (§4.2 band 4). Purely decorative — every caller passes
 *   `contentDescription = null` content — so it adds nothing to the tile's announcement, which
 *   stays "title, supporting".
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
    art: @Composable () -> Unit,
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
            art()
        }
    }
}

/**
 * One share of a `Row`'s free width: the text column beside a fixed-size badge or accent, and
 * one of the progress ribbon's three equal pills. `internal` because the ribbon lives in its
 * own file — the bands were split out when this one crossed detekt's per-file function count,
 * and a second copy of the same 1f would be a place for the two to disagree.
 */
internal const val CONTENT_WEIGHT = 1f

/**
 * Entrance-cascade order of Home's fixed bands (§10: 40ms stagger, strictly downward).
 *
 * Only the first two are constants: the Continue chips and the mode tiles are conditional, so
 * their orders are counted from the ribbon's at the call site and stay contiguous whether one
 * chip renders, two, or none. A fixed number for a conditional band would leave a gap in the
 * stagger, which reads as a pause in the middle of the screen appearing.
 */
internal const val HERO_ENTRANCE_ORDER = 0
internal const val RIBBON_ENTRANCE_ORDER = 1

@Preview(name = "Home · light")
@Composable
private fun HomeScreenLightPreview() {
    AppTheme(darkTheme = false) {
        HomeScreen(
            // The first-frame state: the ribbon paints its placeholders and nothing waits.
            state = HomeUiState(),
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
}

@Preview(name = "Home · dark")
@Composable
private fun HomeScreenDarkPreview() {
    AppTheme(darkTheme = true) {
        HomeScreen(
            // The busiest legitimate Home: filled ribbon, both Continue chips, both tiles —
            // the layout the first-paint rule (D-03f) is about.
            state =
                HomeUiState(
                    totalStars = PREVIEW_STARS,
                    classicBoardsCleared = PREVIEW_BOARDS_CLEARED,
                    challengeCurrentLevel = PREVIEW_LEVEL,
                    classicResumeLevelId = ClassicLevelIds.EUROPEAN37,
                    classicResumeBoard = Boards.european37Initial,
                    challengeResumeLevel = PREVIEW_RESUME_LEVEL,
                ),
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
}

private const val PREVIEW_STARS = 7
private const val PREVIEW_BOARDS_CLEARED = 3
private const val PREVIEW_LEVEL = 12
private const val PREVIEW_RESUME_LEVEL = 11
