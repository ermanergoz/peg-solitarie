package com.app.pegsolitaire.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.mutableStateOf
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
import com.app.pegsolitaire.ui.theme.rememberAnimationsEnabled
import kotlinx.coroutines.flow.first

/**
 * The stateless Home mode-select screen (NAV-01, D-09/D-10/D-14/D-15), in the
 * Candy-Clay skin (DESIGN-SYSTEM.md Home).
 *
 * Renders [state] and forwards events; owns no logic (state hoisting). An M3
 * [Scaffold] with **no top bar at all** (Phase 15, D-03/D-05c) hosts the five scrolling
 * bands of [HomeBands], capped at `content_max_width` and centred. The height left over with no
 * Continue chip is split four fifths to the stage beneath the hero and one fifth to the footer
 * below the tiles, and a chip that arrives takes its room from the stage alone (§4.2.2, §4.2.4,
 * §4.2.5, D-03i, D-03l, D-03n): the hero row (the gradient Fredoka title start-aligned beside the
 * Settings gear that opens the Settings destination, Phase-5 D-14), the stage — a clay board
 * showing a game in progress, as large as the slack allows and not drawn when there is none —
 * the read-only progress ribbon, up to two Continue chips, the two chunky clay mode tiles — a
 * violet Classic tile whose whole surface navigates to the catalog ([onPlayClassic]) and a pink
 * Challenge tile (with its persisted-level chip) whose whole surface enters Challenge
 * ([onPlayChallenge]) at the player's persisted [HomeUiState.challengeCurrentLevel] (CHAL-05) —
 * and an empty footer, one fifth of that slack. Two slow-drifting low-alpha
 * blobs float behind the content (skipped when OS animations are off), the bands enter with a
 * stagger, and everything tappable squishes. Below the compact threshold of available height —
 * which reads the available **width** as well, since D-03k — the band spacing tightens, and on
 * the narrow class each progress pill drops its decorative glyph and puts its label beside its
 * value; no readout, control or band is ever dropped.
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
 * Home's five bands (§4.2): hero, progress ribbon, Continue chips, mode tiles, footer — one
 * `verticalScroll` column capped at `content_max_width` and centred, inside a
 * `BoxWithConstraints` that reads the height available to it (§4.2.2, D-03i). The column is
 * [HomeBandColumn], whose measure policy gives every band its height and splits the slack
 * (§4.2.5, D-03n).
 *
 * ## Why it scrolls, and why the scroll is not a place to put things
 *
 * Home did **not** scroll before this refresh, and at 200% font scale with two Continue chips
 * it already overflowed, so the scroll is a fix rather than polish. It exists for the overflow
 * case only: the first-paint rule (D-03f) requires bands 3 and 4 — the Continue chips and both
 * mode tiles — to be fully within the first viewport at default font scale on a 360x640dp
 * phone, and on that window the **compact band set** below is what keeps them there. A
 * Continue chip reachable only by scrolling is a broken device oracle, not a layout compromise.
 *
 * ## The slack: four fifths to the stage, one fifth to the footer — of the slack with no chip
 *
 * The slack is the height the bands leave the column, and D-03n splits the slack the window leaves
 * with **no** Continue chip, S₀, which is fixed for the window: one fifth is the empty footer below
 * the tiles, as band 5, and the rest is the [HomeStage] directly beneath the hero row. A Continue
 * chip that arrives after first paint — every chip does, once `HomeViewModel` has decoded the saves
 * — takes its room from the stage alone. So the ribbon rides up on the stage's bottom edge and the
 * mode tiles, which a thumb may already be heading for, hold still; where the stage cannot give the
 * whole room they move by what it could not give, and no more (§4.2.5, criteria 15s and 15t).
 *
 * So on a tall phone most of the slack still goes *above* the ribbon, the Continue chips and the
 * mode tiles, which stay one contiguous group in the lower thumb half of a 6.7-inch screen (§11
 * "One-handed use") with a visible margin left below them rather than the tiles sitting on the
 * navigation bar — and that share is a clay board showing a game in progress rather than a hole
 * (§4.2.4, D-03l). No slack sits between the ribbon and the chips: that is where the founder's
 * ≈172dp of blank was.
 *
 * **The column reports its own height, so nothing in the chain around it sets one.**
 * `verticalScroll` measures its content with an unbounded maximum height, so [HomeBandColumn] is
 * handed the height this `BoxWithConstraints` measured instead. It is exactly that tall, padding
 * included, while its bands fit, and taller by the overflow once they do not, which is precisely
 * when the column must scroll.
 *
 * The shape before D-03i — one fixed `Spacer(spacing_lg)` — is what put the founder's empty
 * space at the bottom of the screen instead of the middle: a top-aligned column in a
 * `fillMaxSize` scroll container sends every unspent pixel to the footer. Measured on device
 * as the Classic tile's centre at 1173px against a 1212px midline with the bottom 34% bare.
 *
 * **No content band takes slack** (§4.2.2 clause 4). A band handed a share of the slack is handed
 * **0** the moment the fixed bands exceed the viewport, so a hero that took slack would be clipped
 * to nothing at 200% font scale, which criterion 14 forbids. The stage takes it because its one
 * drawing is decoration for which 0 is the right size on a short screen — the single exception
 * clause 4 makes (§4.2.4) — and the footer is space, not a band: neither has an entrance order of
 * its own, a squish, or anything tappable, focusable or announced.
 *
 * ## `fillMaxSize()` belongs on the Box and nowhere else
 *
 * Ahead of `verticalScroll` in the column's own chain it fixes the content's width to the
 * window's, which makes the `widthIn(max = …)` after it a no-op and leaves the column hugging
 * the leading edge on any window wider than 480dp — so §12's "capped and centred" row was not
 * true of the screen. `align(Alignment.TopCenter)` on a column that may now be narrower than
 * its parent is what centres what the cap caps.
 *
 * ## What a short screen gets
 *
 * Below the compact threshold of **available** height — after the `Scaffold` has subtracted the
 * system-bar insets, so a tall navigation bar makes a device behave as the shorter device it
 * effectively is — Home takes the compact band set, all of it or none: the band spacing and the
 * column's padding drop to `spacing_sm`, and on the narrow class each progress pill drops its
 * decorative glyph and puts its label beside its value. (The hero is one row on every window since
 * D-03l, and the stage needs no threshold: it only ever gets the slack, so on a short screen it is
 * simply 0 and draws nothing.) It is read against available height and width
 * **never the font scale**, so a tall phone at 200% keeps the full set and scrolls rather than
 * changing shape (criterion 15i). Nothing else yields: not the gear (the only route to Settings
 * and so to How to play), not a pill, not a label, not a value, not a chip, not a tile, not a
 * touch target.
 *
 * ## Why the threshold reads width as well as height (§4.2.3, D-03k)
 *
 * §4.2.2's first arithmetic summed band **minimums** and put the compact set at ≈544dp; measured
 * at 360dp width the bands stand ≈156dp above their floors, because they **wrap**. The error was
 * wrap at narrow width and not height at all, so a threshold that reads height alone measures the
 * wrong axis. Below `home_compact_threshold_width` the threshold is therefore
 * `home_compact_threshold_height_narrow`, derived exactly as the 720dp one is — the full set's own
 * both-chips requirement **at this width** (≈872dp), rounded onto the grid. At or above 400dp
 * nothing D-03k added changes anything at all.
 *
 * Its consequence is stated rather than discovered: **at 360dp width Home is always compact**, and
 * that is the intent. A 360x800dp phone needs the compact set exactly as much as a 360x640dp one —
 * the full set is ≈872dp against its ~728dp of usable height, while the compact set fits with
 * slack. 400dp is the boundary because 360dp is the narrowest supported window and the next common
 * width is the 412dp class, so no shipping device sits on it.
 *
 * ## What the narrow class still does not fit, and why the criterion says so
 *
 * After every yield that costs no readout the compact-narrow set is ≈640dp against the **568dp** a
 * 360x640dp phone offers (640 less a 24dp status bar and a 48dp three-button navigation bar), so
 * it is ~72dp over and **the screen scrolls**. Criterion 15h asserted a no-scroll fit and is
 * retired as false; 15k requires what is achievable and measurable instead — the hero row, the
 * whole ribbon, both Continue chips and the first mode tile complete in the first viewport, the
 * second mode tile **breaking the fold** so there is no false bottom, and the rest one plain
 * scroll of ≤ 120dp away. Every one of the remaining 72dp is a readout, a target or a route
 * (§4.2.3's table), so there is nothing left to take at a price this screen is willing to pay.
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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // `maxWidth` is a second READ of a value this BoxWithConstraints has already measured,
        // not a second measuring pass, so the width term costs nothing (§4.2.3 part 1).
        val bandSet = homeBandSetFor(availableWidth = maxWidth, availableHeight = maxHeight)
        val bandSpacing =
            dimensionResource(if (bandSet.compact) R.dimen.spacing_sm else R.dimen.spacing_md)
        // The tiles' cascade order follows the chips shown, so the stagger stays contiguous (§10).
        val chipsShown = listOfNotNull(state.classicResumeLevelId, state.challengeResumeLevel).size
        val classicTileOrder = FIRST_CHIP_ENTRANCE_ORDER + chipsShown
        val animationsEnabled = rememberAnimationsEnabled()
        val classicRoom = rememberChipRoom(present = state.classicResumeLevelId != null, animationsEnabled)
        val challengeRoom = rememberChipRoom(present = state.challengeResumeLevel != null, animationsEnabled)
        val stageArtFade = remember { mutableStateOf(StageArtFade.None) }
        HomeBandColumn(
            availableHeightPx = constraints.maxHeight,
            bandSpacing = bandSpacing,
            onStageArtFade = { fade -> if (stageArtFade.value != fade) stageArtFade.value = fade },
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .widthIn(max = dimensionResource(R.dimen.content_max_width))
                    .padding(bandSpacing),
            hero = { HomeHero(onOpenSettings = onOpenSettings) },
            // The stage: the larger share of the slack, directly beneath the hero row (§4.2.4
            // clause 3), so the ribbon, the chips and the tiles form one contiguous lower group in
            // the thumb zone and the slack reads as the game's own board instead of a hole.
            stage = {
                HomeStage(
                    artFade = stageArtFade.value,
                    artFadeProgress = { stageMotionProgress(classicRoom, challengeRoom) },
                )
            },
            ribbon = {
                HomeProgressRibbon(
                    compact = bandSet.compactPills,
                    totalStars = state.totalStars,
                    classicBoardsCleared = state.classicBoardsCleared,
                    challengeCurrentLevel = state.challengeCurrentLevel,
                )
            },
            chips = {
                ContinueChips(
                    state = state,
                    classicRoom = classicRoom,
                    challengeRoom = challengeRoom,
                    onResumeClassic = onResumeClassic,
                    onResumeChallenge = onResumeChallenge,
                )
            },
            tiles = {
                ClassicTile(entranceOrder = classicTileOrder, onPlayClassic = onPlayClassic)
                ChallengeTile(
                    currentLevel = state.challengeCurrentLevel,
                    entranceOrder = classicTileOrder + 1,
                    onPlayChallenge = onPlayChallenge,
                )
            },
        )
    }
}

/**
 * Band 3 — the Continue chips present, Classic over Challenge (§4.2, PERS-03, D-03): none, one or
 * two, each a [ResumeTile] carrying exactly what its resume needs, so the host never guesses, and
 * each in its own room of the band column ([ChipRoomSlot]), which opens as the chip arrives and
 * closes, the chip still shown, as it leaves (§4.2.5).
 *
 * A chip present at Home's first composition takes its place in the entrance cascade, counted on
 * from the ribbon's over the chips shown, so the cascade stays contiguous (§10). A chip that
 * arrives later — the usual case, once `HomeViewModel` has decoded the saves — takes no stagger, so
 * its fade starts in the frame its room starts to open and no empty room shows ahead of it.
 */
@Composable
private fun ContinueChips(
    state: HomeUiState,
    classicRoom: ChipRoom,
    challengeRoom: ChipRoom,
    onResumeClassic: (levelId: String) -> Unit,
    onResumeChallenge: (level: Int) -> Unit,
) {
    val classicAtFirstComposition = remember { state.classicResumeLevelId != null }
    val challengeAtFirstComposition = remember { state.challengeResumeLevel != null }
    ChipRoomSlot(
        value = state.classicResumeLevelId?.let { ClassicResume(levelId = it, board = state.classicResumeBoard) },
        room = classicRoom,
    ) { classic ->
        ResumeTile(
            title = stringResource(R.string.resume_classic_title),
            supporting = stringResource(R.string.resume_classic_supporting),
            entranceOrder = if (classicAtFirstComposition) FIRST_CHIP_ENTRANCE_ORDER else LATE_CHIP_ENTRANCE_ORDER,
            board = classic.board,
            onResume = { onResumeClassic(classic.levelId) },
        )
    }
    // The Challenge Continue carries the SNAPSHOT's stamped level (WR-03),
    // never challengeCurrentLevel — the ladder flow can lag at a cold start
    // while the stamp always names the level the saved board really is.
    ChipRoomSlot(value = state.challengeResumeLevel, room = challengeRoom) { challengeResumeLevel ->
        ResumeTile(
            title = stringResource(R.string.resume_challenge_title),
            supporting =
                stringResource(
                    R.string.resume_challenge_supporting,
                    challengeResumeLevel,
                ),
            entranceOrder =
                when {
                    !challengeAtFirstComposition -> LATE_CHIP_ENTRANCE_ORDER
                    classicAtFirstComposition -> FIRST_CHIP_ENTRANCE_ORDER + 1
                    else -> FIRST_CHIP_ENTRANCE_ORDER
                },
            // A procedural level has no memorable footprint, so this chip keeps the
            // generic play arrow: a thumbnail of a board the player cannot recognise
            // would be decoration pretending to be information.
            board = null,
            onResume = { onResumeChallenge(challengeResumeLevel) },
        )
    }
}

/** What the Classic Continue chip resumes: the saved board's id, and its footprint for the thumbnail. */
private data class ClassicResume(
    val levelId: String,
    val board: Board?,
)

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

/** The first Continue chip's order, straight after the ribbon's; the rest are counted on from it. */
private const val FIRST_CHIP_ENTRANCE_ORDER = RIBBON_ENTRANCE_ORDER + 1

/**
 * A Continue chip arriving after Home's first composition enters with no stagger delay, so its
 * fade starts with its room (§4.2.5, "The motion"); the bands drawn at first paint have already
 * run their cascade, and their counted orders are unchanged.
 */
private const val LATE_CHIP_ENTRANCE_ORDER = 0

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

/**
 * The compact-narrow band set (§4.2.2, §4.2.3), which is otherwise only visible on a short
 * narrow device: the same busiest state as the dark preview, on the design baseline's own
 * 360x640dp window. The hero row should sit directly above the ribbon with no stage between
 * them (the column overflows, so the stage is 0), each progress pill should read "label value"
 * on one line with no glyph, and the Challenge tile should be cut by the bottom edge rather than
 * sitting below it — that fold is criterion 15k part 2, not a clipping bug.
 *
 * In **both** themes, per rules/compose.md: the compact band set is the one a 360dp-wide phone
 * always gets (§4.2.3), so it is not a variant that only needs checking in one of them.
 */
@Preview(
    name = "Home · compact · light",
    widthDp = PREVIEW_COMPACT_WIDTH_DP,
    heightDp = PREVIEW_COMPACT_HEIGHT_DP,
)
@Preview(
    name = "Home · compact · dark",
    widthDp = PREVIEW_COMPACT_WIDTH_DP,
    heightDp = PREVIEW_COMPACT_HEIGHT_DP,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun HomeScreenCompactPreview() {
    // Two annotations on one function rather than two functions: `AppTheme` defaults
    // `darkTheme` to `isSystemInDarkTheme()`, so `uiMode` selects the theme and the two
    // renders cannot drift in their state or their window.
    AppTheme {
        HomeScreen(
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

/** The small baseline the first-paint rule is written against (D-03f), and below the threshold. */
private const val PREVIEW_COMPACT_WIDTH_DP = 360
private const val PREVIEW_COMPACT_HEIGHT_DP = 640
