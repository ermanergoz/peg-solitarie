package com.app.pegsolitaire.platform.games

import android.app.Activity
import android.content.Intent
import com.app.pegsolitaire.data.PlayGamesOptInRepository
import com.app.pegsolitaire.di.ApplicationScope
import com.app.pegsolitaire.platform.activity.ForegroundActivityProvider
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.games.PlayGames
import com.google.android.gms.games.PlayGamesSdk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The real, SDK-backed [GamesService] — Play Games Services v2 sign-in behind the seam
 * (PGS-01, PGS-02, PGS-03).
 *
 * **The opt-in check is the outermost gate, and its position is the whole design.** D-01
 * says a player who never opted in is never reached by the SDK at all: no
 * `PlayGamesSdk.initialize`, no `isAuthenticated`, no account chooser, no "Welcome back"
 * popup. Google's own documented pattern initializes unconditionally in
 * `Application.onCreate`, and copying that shape is how this file would quietly stop
 * honouring the requirement while still looking correct — the violating version differs
 * from this one only in where a single `if` sits. [bootstrapIfOptedIn] therefore reads
 * [PlayGamesOptInRepository.optedIn] **before** it awaits an Activity, before it
 * initializes, and before it constructs any client. `PlayGamesServiceTest` pins that
 * negatively, by asserting the Activity is never even awaited on the declined path.
 *
 * **Every branch resolves [session]; none may return leaving it [GamesSignInState.Unknown].**
 * `Unknown` means "the first check has not answered yet", and D-11 hides the whole Settings
 * Play Games section while it holds — so a branch that returns without publishing an answer
 * hides that section forever. The tempting shape for the declined-opt-in path is a bare
 * early `return`, which is exactly that bug, for the *majority* of players. Each exit below
 * assigns [GamesSignInState.SignedOut] explicitly instead.
 *
 * **`@Singleton` is on the class, not only on the `@Binds`.** `@Binds @Singleton` scopes the
 * binding of the *interface*, so anything injecting the concrete type would receive a
 * second, unscoped instance with its own [_session] and its own [bootstrapped] guard — a
 * session flow nothing ever updates. Same reasoning, and the same annotation, as
 * [com.app.pegsolitaire.platform.ads.consent.UmpConsentGateway] and
 * [com.app.pegsolitaire.platform.activity.DefaultForegroundActivityProvider]: it holds
 * state.
 *
 * **No SDK handle is a field, and no SDK type is a Hilt binding.** `GamesSignInClient` and
 * `PlayersClient` are constructed per call from `PlayGames.get*Client(activity)` — they are
 * built *from* an Activity, unlike `UmpConsentGateway`'s context-scoped
 * `ConsentInformation`, so there is nothing here to hold `by lazy`. Nothing in this file
 * appears in a signature Hilt can see, which is what keeps
 * `PlatformBoundaryRules.onlyPlatformDependsOnPlaySdks` green: the generated
 * `DaggerApp_HiltComponents_SingletonC` lives outside `..platform..` and carries a
 * `Provider<T>` field per binding, so an SDK-typed `@Provides` fails the gate no matter
 * which module declares it (proved empirically in plan 09-05).
 *
 * **Nothing here logs, and the absence is enforced by a grep gate rather than by review** —
 * which is why this paragraph avoids spelling any logging API out. ASVS V7: the player ID,
 * the display name and any SDK exception message are personal-data-adjacent, so no logging
 * call may name them (T-11-04). Every failure degrades instead: a thrown SDK call, an
 * unavailable Play services, an offline device and a missing Play Games app are all
 * [GamesSignInState.SignedOut] — one state for every "not signed in" cause, with no
 * `GoogleApiAvailability` gating and no connectivity retry (D-12).
 *
 * **Two things deliberately absent.** Sign-in never consults
 * [com.app.pegsolitaire.platform.ads.consent.ConsentGateway] or `AdsReadiness`: CONS-02
 * governs the ads SDK only, and the published privacy policy already discloses optional
 * Play Games (D-03). And nothing here touches, suppresses or repositions any SDK popup —
 * neither the "Welcome back" sign-in toast, which is the only launch-time signal that
 * sign-in happened (D-05), nor the "achievement unlocked" toast (D-16). The second is not
 * even a choice: Play Games Services v2 retired the v1 client that owned popup placement,
 * and exposes no replacement, so both toasts appear in the SDK's default position by
 * construction. D-16 costs zero lines here. Like the no-logging rule above, the absence is
 * enforced by a grep gate rather than by review — which is why this paragraph names no
 * popup-positioning API, not even to say it is unavailable.
 *
 * ## The submission contract ([submitScore], [unlockAchievement], [setAchievementSteps])
 *
 * All three run through [submitting], and its three steps are the contract in order:
 * **session-gated** (nothing is attempted unless [session] is
 * [GamesSignInState.SignedIn] — a call made while the SDK has not signed the player in is
 * dropped by the SDK anyway, so reaching for a client first would only cost a round trip to
 * learn what [_session] already knows), **Activity-bounded** (never a wait longer than
 * [GamesConstants.ACTIVITY_WAIT_MS]), and **failure-as-`false`** (an SDK throw, an offline
 * device or a backgrounded app is reported, never propagated). A caller answers `false` with
 * the D-10 pending-submissions flag, never with an error the player sees (PGSL-04).
 *
 * The two stock-UI launches ([showLeaderboards], [showAchievements]) follow the same
 * Activity-bounded, never-throwing shape, and both obtain *and* start their `Intent` inside
 * this class — no `Intent` and no Activity crosses the seam (PLAT-03/PLAT-04).
 *
 * ## Size suppression
 *
 * `TooManyFunctions` is suppressed because the count is [GamesService]'s own method set plus
 * four private helpers, not accreted responsibility: eight overrides are fixed by the
 * interface, and the helpers ([submitting], [launchStockUi], [awaitForegroundActivity],
 * [hostActivityAfterAwait]) each exist to state one rule exactly once rather than repeat it
 * per call site. Splitting the class to satisfy the threshold would mean a second holder of
 * [_session] — the precise bug the `@Singleton`-on-the-class note above exists to prevent.
 */
@Suppress("TooManyFunctions") // see "Size suppression" above
@Singleton
class PlayGamesService
    @Inject
    constructor(
        private val activityProvider: ForegroundActivityProvider,
        private val optInRepository: PlayGamesOptInRepository,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) : GamesService {
        private val _session = MutableStateFlow<GamesSignInState>(GamesSignInState.Unknown)

        override val session: StateFlow<GamesSignInState> = _session.asStateFlow()

        /**
         * Exactly one silent check per process (D-02).
         *
         * `MainActivity.onCreate` fires [bootstrapIfOptedIn] on every Activity creation, so
         * a configuration change alone reaches it a second time; without this guard an
         * opted-in player pays for a redundant `initialize` plus `isAuthenticated` round
         * trip on every rotation. Mirrors `UmpConsentGateway.initialized`.
         *
         * A successful [signIn] sets it too (WR-02): once the chooser has answered, the
         * silent check has nothing left to learn, and running it on the next rotation
         * would only replay the SDK's "Welcome back" popup — the exact cost this guard
         * exists to avoid.
         *
         * It guards only the SDK-touching branch. The declined-opt-in branch runs on every
         * call, which is free — it touches nothing but a DataStore read and a flow write.
         */
        private val bootstrapped = AtomicBoolean(false)

        /**
         * Serializes the two SDK-touching paths, [bootstrapIfOptedIn] and [signIn] (WR-02).
         *
         * Both initialize the SDK, build clients and publish to [_session] with
         * last-writer-wins semantics, and they can overlap: a first-ever opt-in from the Home
         * prompt runs [signIn] with the chooser up, and a rotation while the chooser is up
         * recreates `MainActivity`, whose `onCreate` fires [bootstrapIfOptedIn] against a
         * now-`true` opt-in flag. Unserialized, a slow bootstrap could publish `SignedOut`
         * over the `SignedIn` the chooser had just produced. Under the lock the bootstrap
         * runs second, finds the answer already published, and touches nothing.
         */
        private val sdkMutex = Mutex()

        // RED placeholder: the real, opt-in-gated process-start initialize lands next.
        override fun initializeIfOptedIn() = Unit

        override fun bootstrapIfOptedIn() {
            // Dispatchers.Main because PlayGames.get*Client takes an Activity and the SDK
            // may put its own UI on screen; the injected application scope runs on
            // Dispatchers.Default. Fire-and-forget: MainActivity.onCreate never awaits this,
            // so nothing about sign-in can delay the first frame (PGS-01, Pitfall 3).
            scope.launch(Dispatchers.Main) {
                if (!optInRepository.optedIn.first()) {
                    // D-01's other branch, and the one most players take. The explicit
                    // assignment is the point: a bare `return@launch` here would leave
                    // session at Unknown forever and hide the Settings section for good.
                    _session.value = GamesSignInState.SignedOut
                    return@launch
                }
                if (!bootstrapped.compareAndSet(false, true)) return@launch

                sdkMutex.withLock {
                    // The WR-02 short-circuit: an explicit signIn may have answered while
                    // this waited for the lock, and a published SignedIn is authoritative —
                    // re-checking it could only regress it or replay the SDK popup.
                    if (_session.value is GamesSignInState.SignedIn) return@withLock

                    val activity = awaitForegroundActivity()
                    if (activity == null) {
                        _session.value = GamesSignInState.SignedOut
                        return@withLock
                    }

                    val authenticated =
                        runCatching {
                            PlayGamesSdk.initialize(activity.applicationContext)
                            val signInClient = PlayGames.getGamesSignInClient(activity)
                            signInClient.isAuthenticated().await().isAuthenticated
                        }.getOrDefault(false)

                    _session.value =
                        if (authenticated) {
                            signedInSessionFor(hostActivityAfterAwait(activity))
                        } else {
                            GamesSignInState.SignedOut
                        }
                }
            }
        }

        /**
         * Signs the player in explicitly (D-10/D-18/D-19), returning whether they are signed
         * in afterwards and throwing on a genuine SDK error.
         *
         * `PlayGamesSdk.initialize` runs unconditionally rather than being skipped when
         * [bootstrapIfOptedIn] already ran. It is idempotent, and the case that makes it
         * necessary is D-19: a tap on the Settings row may be this player's first-ever
         * opt-in, so the launch bootstrap will have taken its declined branch and left the
         * SDK untouched.
         *
         * ## Classifying the failure (the one deliberately-conservative decision here)
         *
         * **[UNVERIFIED against a live Play Console project — D-04, RESEARCH Open Question
         * 2].** Google documents no failure taxonomy for `Task<AuthenticationResult>`, and
         * no Play Console project exists yet against which to observe one, so the split
         * below is a conservative guess and is marked as such rather than presented as an
         * SDK contract:
         *
         * - a **successful** Task carrying `isAuthenticated == false` is the expected shape
         *   of a plain decline, and is reported as `false`, silently;
         * - a **failed** Task is reported as an error (thrown) unless it is identifiably an
         *   `ApiException` carrying one of [GamesConstants.CANCEL_STATUS_CODES] — the generic
         *   `CANCELED` or the sign-in-specific `SIGN_IN_CANCELLED` a dismissed chooser reports
         *   (WR-08);
         * - a **cancelled** await — the calling coroutine being cancelled, e.g. a
         *   `SettingsViewModel` cleared while the chooser is up — is neither: it is rethrown
         *   untouched, before any classification and before the session is written, because
         *   `runCatching` would otherwise turn a structured-concurrency cancel into "an SDK
         *   error" (WR-04).
         *
         * The bias is deliberate: under-alerting leaves a player staring at a row that did
         * nothing, while over-alerting shows a snackbar every time someone changes their
         * mind at the account chooser — and D-10 names the second as the worse failure. If
         * the observed taxonomy differs once the Console project exists, only this
         * classification changes; [GamesService.signIn]'s contract and every caller written
         * against it stay as they are.
         *
         * A resolved-but-unidentifiable player (authentication succeeded, the player-ID
         * lookup did not) is reported as `false` rather than thrown: there is no
         * [GamesSignInState.SignedIn] to publish without an ID, and a silent decline is the
         * kinder reading of an outcome the player cannot act on.
         */
        override suspend fun signIn(): Boolean =
            // Under sdkMutex so it never interleaves with the silent launch check (WR-02).
            sdkMutex.withLock {
                // Bounded, and a null answer is not an error: nothing was touched, so there
                // is nothing to surface. A backgrounded tap must not raise a snackbar nobody
                // sees.
                val activity = awaitForegroundActivity() ?: return@withLock false

                val outcome =
                    runCatching {
                        PlayGamesSdk.initialize(activity.applicationContext)
                        val signInClient = PlayGames.getGamesSignInClient(activity)
                        signInClient.signIn().await().isAuthenticated
                    }

                outcome.fold(
                    onSuccess = { authenticated ->
                        val resolved =
                            if (authenticated) {
                                signedInSessionFor(hostActivityAfterAwait(activity))
                            } else {
                                GamesSignInState.SignedOut
                            }
                        _session.value = resolved
                        val signedIn = resolved is GamesSignInState.SignedIn
                        // A signed-in answer satisfies the once-per-process silent check (WR-02).
                        if (signedIn) bootstrapped.set(true)
                        signedIn
                    },
                    onFailure = { failure ->
                        // A cancelled await is the caller's structured-concurrency cancel,
                        // not an SDK outcome: it is neither classified nor allowed to touch
                        // the shared session (WR-04).
                        if (failure is CancellationException) throw failure
                        _session.value = GamesSignInState.SignedOut
                        if ((failure as? ApiException)?.statusCode in GamesConstants.CANCEL_STATUS_CODES) {
                            false
                        } else {
                            throw failure
                        }
                    },
                )
            }

        /**
         * The Activity to hand the player lookups that follow an SDK `await()` (WR-01).
         *
         * [captured] was resolved *before* an await that may span the SDK's own account
         * chooser — a separate Activity on top of `MainActivity` — and a configuration change
         * while the chooser is up destroys the captured instance. `PlayGames.getPlayersClient`
         * on a destroyed Activity fails, silently, inside `runCatching`, and a *successful*
         * sign-in would then be reported as a decline (or as signed-in with no name). So the
         * foreground Activity is re-resolved once the round trip is back, and [captured] is
         * used only while nothing newer is resumed yet — [ForegroundActivityProvider.current]
         * is `null` between a pause and the next resume, and in that window the captured
         * instance is still the best (and possibly only) one there is.
         */
        private fun hostActivityAfterAwait(captured: Activity): Activity = activityProvider.current ?: captured

        /**
         * The session for a player the SDK has just reported as authenticated.
         *
         * Identity is fetched separately from the name, and that separation is what makes
         * D-14 expressible at all: `AuthenticationResult` carries no player ID, and
         * `getCurrentPlayer()` carries both, so resolving through that one call alone would
         * mean a name lookup failure also lost the ID — leaving no way to report the
         * "signed in, name unknown" state D-14 requires. The lighter `getCurrentPlayerId()`
         * therefore establishes identity first; only then is the name attempted.
         *
         * A failed **ID** lookup is different from a failed **name** lookup and is treated
         * as such: with nothing to identify the player by, [GamesSignInState.SignedIn] is
         * not constructible, so this degrades to D-12's single not-signed-in state.
         */
        private suspend fun signedInSessionFor(activity: Activity): GamesSignInState {
            val playerId =
                runCatching { PlayGames.getPlayersClient(activity).getCurrentPlayerId().await() }
                    .getOrNull()

            return if (playerId == null) {
                GamesSignInState.SignedOut
            } else {
                resolveSignedInState(activity, playerId)
            }
        }

        /**
         * Attaches the D-07 display name to an already-established identity, best-effort.
         *
         * D-14 in one expression: a failed or stalled name lookup yields
         * `SignedIn(playerId, displayName = null)`. It is **never** a sign-in failure and
         * **never** a reason to hold [session] at [GamesSignInState.Unknown] — the player is
         * signed in, and the caller renders the neutral fallback copy instead of the named
         * form. The name widens what a surface can say about the player; it never decides
         * whether the player is signed in.
         *
         * The fetched player's own ID wins over [playerId] when both are available: the call
         * that returned the record is the more authoritative source for what that record is.
         */
        private suspend fun resolveSignedInState(
            activity: Activity,
            playerId: String,
        ): GamesSignInState.SignedIn {
            val player =
                runCatching { PlayGames.getPlayersClient(activity).getCurrentPlayer().await() }
                    .getOrNull()

            return GamesSignInState.SignedIn(
                playerId = player?.playerId ?: playerId,
                displayName = player?.displayName,
            )
        }

        /**
         * The foreground Activity, or `null` if none arrives within
         * [GamesConstants.ACTIVITY_WAIT_MS].
         *
         * [ForegroundActivityProvider.awaitActivity]'s KDoc makes the bound the caller's
         * responsibility, and this is the single place this class applies it — a backgrounded
         * app may never resume, and an unbounded await inside a process-lifetime singleton
         * hangs it for the life of the process (T-11-06).
         */
        private suspend fun awaitForegroundActivity(): Activity? =
            withTimeoutOrNull(GamesConstants.ACTIVITY_WAIT_MS) { activityProvider.awaitActivity() }

        override suspend fun submitScore(
            leaderboardId: String,
            score: Long,
        ): Boolean =
            submitting { activity ->
                PlayGames.getLeaderboardsClient(activity).submitScoreImmediate(leaderboardId, score).await()
            }

        override suspend fun unlockAchievement(achievementId: String): Boolean =
            submitting { activity ->
                PlayGames.getAchievementsClient(activity).unlockImmediate(achievementId).await()
            }

        /**
         * Absolute steps (D-12), which is what makes a resend safe: the SDK no-ops when the
         * player's recorded steps already exceed [steps], so a retry can never double-count
         * or move progress backwards.
         *
         * `setStepsImmediate` returns `Task<Boolean>` and that payload is **discarded on
         * purpose** — it reports "the achievement is now unlocked", not "the submission was
         * accepted". Returning it would tell a caller that every step short of the final one
         * had failed.
         */
        override suspend fun setAchievementSteps(
            achievementId: String,
            steps: Int,
        ): Boolean =
            submitting { activity ->
                PlayGames.getAchievementsClient(activity).setStepsImmediate(achievementId, steps).await()
            }

        override suspend fun showLeaderboards() =
            launchStockUi(GamesConstants.LEADERBOARDS_REQUEST_CODE) { activity ->
                PlayGames.getLeaderboardsClient(activity).getAllLeaderboardsIntent().await()
            }

        override suspend fun showAchievements() =
            launchStockUi(GamesConstants.ACHIEVEMENTS_REQUEST_CODE) { activity ->
                PlayGames.getAchievementsClient(activity).getAchievementsIntent().await()
            }

        /**
         * Runs one submission against the SDK and reports whether it was accepted, never
         * throwing for a submission failure (PGSL-04).
         *
         * **The order of the three steps is the whole point.**
         *
         * 1. Not [GamesSignInState.SignedIn] means no submission is attempted at all — and
         *    [GamesSignInState.Unknown] takes the same branch as [GamesSignInState.SignedOut]
         *    deliberately. `Unknown` means "the first check has not answered yet", and the SDK
         *    silently drops calls made before sign-in resolves; treating it as "probably fine"
         *    would convert a droppable submission into a reported success and lose it for good.
         * 2. No foreground Activity means `false` **immediately** — this path reads
         *    [ForegroundActivityProvider.current] and never awaits one (WR-02). A submission
         *    is fire-and-forget and the caller's dirty flag already guarantees a retry, so
         *    waiting buys nothing and costs a great deal: `onActivityPaused` nulls the
         *    reference, so a player who backgrounds the app right after a solve — the normal
         *    "finish level, switch apps" pattern — would make every call in the pass burn the
         *    full [GamesConstants.ACTIVITY_WAIT_MS] and still return `false`. At up to ~29
         *    calls in a fully-unlocked player's pass that is minutes of `ProgressSubmitter`
         *    holding its pass mutex while producing nothing, with every later solve queued
         *    behind it. `showLeaderboards` / `showAchievements` keep the wait, because a
         *    stock UI genuinely cannot start without a foreground Activity.
         * 3. Everything else is classified into `false`, **except** a
         *    `CancellationException`, which is rethrown before classification. A caller's
         *    structured-concurrency cancel is not an SDK failure and must not be reported as
         *    one — the same WR-04 rule [signIn] follows.
         *
         * [sdkMutex] is deliberately **not** taken: it serializes the two session-writing
         * paths, and a submission neither writes [_session] nor initializes the SDK. Holding
         * it here would make an offline submission's round trip block sign-in.
         */
        private suspend fun <T> submitting(block: suspend (Activity) -> T): Boolean {
            // The session check gates the Activity read, not just the submission: a
            // signed-out or still-Unknown player never reaches the SDK at all.
            val activity =
                if (_session.value is GamesSignInState.SignedIn) activityProvider.current else null

            return activity != null &&
                runCatching { block(activity) }
                    .onFailure { if (it is CancellationException) throw it }
                    .isSuccess
        }

        /**
         * Awaits the SDK's own `Intent` for a stock Play Games screen and starts it, silently
         * doing nothing if either step fails.
         *
         * The Activity is re-resolved through [awaitForegroundActivity] on every call and
         * never held in a field — the same lifetime rule the rest of this class follows.
         *
         * These getters are documented to fail with a `RemoteException`, and that must never
         * surface to the player: an unreachable cosmetic screen is not something to apologise
         * for. Cancellation is rethrown before the failure is swallowed (WR-04).
         */
        private suspend fun launchStockUi(
            requestCode: Int,
            intentFor: suspend (Activity) -> Intent,
        ) {
            val activity = awaitForegroundActivity() ?: return
            runCatching {
                val intent = intentFor(activity)
                // startActivityForResult is deprecated on ComponentActivity but not removed,
                // and the SDK javadoc *requires* it for every stock-UI Intent so the calling
                // package's identity can be established. The ActivityResult API cannot
                // substitute: it needs a launcher registered before STARTED on a specific
                // Activity, which this process-lifetime singleton has no handle on, and
                // nothing consumes the result anyway.
                activity.startActivityForResult(intent, requestCode)
            }.onFailure { if (it is CancellationException) throw it }
        }
    }
