package com.app.pegsolitaire.data

import com.app.pegsolitaire.data.cloud.CloudProgressCodec
import com.app.pegsolitaire.di.ApplicationScope
import com.app.pegsolitaire.domain.achievement.AchievementOutcome
import com.app.pegsolitaire.domain.achievement.AchievementRules
import com.app.pegsolitaire.domain.achievement.ClassicBoard
import com.app.pegsolitaire.domain.achievement.ProgressAggregate
import com.app.pegsolitaire.domain.progress.CloudProgress
import com.app.pegsolitaire.platform.games.GamesIds
import com.app.pegsolitaire.platform.games.GamesService
import com.app.pegsolitaire.platform.games.GamesSignInState
import com.app.pegsolitaire.platform.games.save.CloudSaveDescription
import com.app.pegsolitaire.platform.games.save.CloudSaveOutcome
import com.app.pegsolitaire.platform.games.save.CloudSaveStage
import com.app.pegsolitaire.platform.games.save.CloudSaveStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single orchestrator that turns stored progress into Play Games submissions (D-13)
 * and into the player's Play Games **cloud save** (CLOUD-01/02/03/05).
 *
 * It owns the persisted "submissions pending" flag ([SubmissionSyncStore]) and its cloud
 * sibling ([CloudSyncStore]), reads the SQL aggregates ([ProgressRepository.aggregate] /
 * [ProgressRepository.classicBests]), applies [AchievementRules] once (D-11), and issues
 * every call through [GamesService] and [CloudSaveStore]. There are exactly two triggers —
 * a solved level ([onResultRecorded], from `ResultsViewModel`) and a session resolving to
 * [GamesSignInState.SignedIn] ([start]'s collector) — plus one resolution
 * ([onProfileSwapResolved]), and they all share one pass, so the offline backfill and the
 * cloud sync have no separate code path (PGSL-05, D-01/D-02).
 *
 * **Nothing in `ui/` names a leaderboard or achievement ID.** [GamesIds] is read here and in
 * `platform/games/` and nowhere else, which is what makes D-13's containment structural
 * rather than a convention. Note the layering this depends on: `data/` → `platform.games`
 * is legal (`PlatformBoundaryRules` forbids only gms/UMP/ads SDK types and `Activity` names
 * outside `..platform..`), and no Play Games SDK type appears in any signature here — which
 * is exactly why [CloudSaveStore] trades in `ByteArray` and a three-`Int`
 * [CloudSaveDescription] rather than in snapshots.
 *
 * **No Hilt module entry exists or is needed.** A `@Singleton` with an `@Inject`
 * constructor is constructible by Hilt on its own — the [ChallengeProgress] precedent — so
 * a reader should not go looking in `di/` for a missing `@Provides`.
 *
 * ## One pass, five numbered steps, and the order is the specification
 *
 * [submitAll] runs, under one mutex, in exactly this order:
 *
 *  0. **Session gate** — anything but [GamesSignInState.SignedIn] (`Unknown` included) sets
 *     BOTH pending markers and returns.
 *  1. **Profile gate** — a stored player id that disagrees with the signed-in one raises
 *     [profileSwap] and returns having performed **zero** store calls (D-12/D-15).
 *  2. **Pull** — once per process, on its own guard, never gated on a dirty flag (D-02).
 *  3. **Phase 12 submissions** — gated on the submissions flag when the trigger asks.
 *  4. **Cloud push** — the merged payload plus its listing metadata (D-01/D-11).
 *
 * Step 1 sits above step 2, and that is the single most damaging thing in this file to get
 * wrong (RESEARCH Pitfall 8). If the pull ran first and the ids then turned out to
 * disagree, the new account's progress would already have been merged into the old
 * account's local store — unrecoverable, and precisely the data loss the D-12 dialog exists
 * to prevent.
 *
 * Step 2 sits above step 3 for a smaller but equally deliberate reason: a cloud import that
 * improved the local aggregates sets the submissions flag **before** step 3 reads it, so a
 * restored player's leaderboards catch up in the same pass rather than lagging until their
 * next solve.
 *
 * ## The A7 change: the cloud half deliberately calls on a clean launch
 *
 * The `onlyIfPending && !pending` early return used to sit above everything, so a signed-in
 * player with a clean flag made no calls at all (PGSL-05 / D-10). It is now a guard around
 * **step 3 only**, because leaving it where it was would skip the once-per-process pull for
 * exactly the new-device player D-02 exists for — they would get nothing back until they
 * happened to solve a level. This is a reviewed change to a documented invariant (RESEARCH
 * assumption A7), not a side effect:
 *  - the **submission** half keeps its "clean launches make no SDK submission calls"
 *    property, unchanged and still asserted by `ProgressSubmitterTest`;
 *  - the **cloud** half does not: a clean launch by a signed-in player now performs one
 *    [CloudSaveStore.load].
 *
 * What a clean launch still does **not** do is push. Step 4's guard requires the trigger to
 * be a solve or a swap resolution, an outstanding retry marker, or a pull that actually
 * brought a payload back — so a signed-in player who opens the app, pulls nothing and
 * closes it again has cost the SDK one read and nothing else.
 *
 * ## The dirty flags, and the one case D-10's text does not cover
 *
 * The submissions flag is set whenever the session is not [GamesSignInState.SignedIn] and
 * whenever any submission in a pass returns `false` or throws; it is cleared only when every
 * call in the pass succeeded. **A pass that issued ZERO calls leaves it untouched**, which
 * is an addition to D-10's literal wording and must survive any later refactor:
 * [ProgressRepository.aggregate] fails safe to [ProgressAggregate] and
 * [ProgressRepository.classicBests] to an empty map, so a storage fault is byte-identical
 * to a brand-new player. Clearing on that pass would silently discard a pending backfill
 * that no later signed-in solve would re-set. Leaving it alone is a no-op for the genuinely
 * empty player — whose flag is already `false` — and preserves the backfill for the fault.
 *
 * [CloudSyncStore.pushPending] is the same idea for the push, with one deliberate
 * difference documented on [pushIfNeeded]: the push is a single call rather than a batch,
 * so it has no "issued nothing" middle case.
 *
 * ## Every pass resubmits everything (RESEARCH Pitfall 3, decided not missed)
 *
 * Pitfall 3 counts up to ~29 SDK calls on a fully-unlocked player's solve and proposes a
 * process-lifetime `Set<AchievementId>` de-dup cache. That cache is deliberately **not**
 * implemented here, per CONTEXT's "resubmit everything, it is safe and idempotent":
 * passes are human-paced (one per solved level, plus one per session resolution), a fresh
 * player's pass issues 0–3 calls, and every call is idempotent by construction — absolute
 * steps (D-12/D-03) and unlocks that no-op on an already-unlocked achievement. A cache
 * would be a second source of truth that the D-10 backfill would have to bypass anyway,
 * buying an optimisation at the cost of the one property — every pass is a complete,
 * self-healing resubmission — that makes PGSL-05 provable. **If Play Console ever reports
 * quota pressure, that de-dup set is the first lever to pull.**
 *
 * ## No logging, anywhere
 *
 * There is no logging call anywhere in `app/src/main` — a build gate greps for the platform
 * logger's fully-qualified name and expects zero hits, which is why this paragraph does not
 * spell it — and that absence is itself the ASVS V7 control: no player id, no achievement id
 * and no SDK message reaches logcat. "Log the failure" is therefore not an available
 * convention here; a failure is swallowed silently, exactly as `InterstitialAdSlot` and
 * `SettingsViewModel` swallow theirs. The cloud half raises the stakes rather than changing
 * the rule: the player id compared in step 1 and the payload figures pushed in step 4 are
 * the two most identifying values this class handles (T-11-04 / T-13-14).
 *
 * ## Size suppressions
 *
 * `TooManyFunctions` and `LongParameterList` are suppressed together for one reason: this is
 * the phase's single orchestration point by decision — Phase 12 D-13 and Phase 13 D-01/D-02
 * all name this object rather than a second collector — so every store it sequences is a
 * constructor parameter and every step of the pass is one small private function. Splitting
 * it would mean a second object holding part of the ordering, and the ordering, as the
 * section above says, *is* the specification.
 */
@Suppress("TooManyFunctions", "LongParameterList") // see "Size suppressions" above
@Singleton
open class ProgressSubmitter
    @Inject
    internal constructor(
        private val progressRepository: ProgressRepository,
        private val gamesService: GamesService,
        private val syncStore: SubmissionSyncStore,
        private val cloudSyncStore: CloudSyncStore,
        private val cloudProfileStore: CloudProfileStore,
        private val cloudSaveStore: CloudSaveStore,
        private val challengeProgress: ChallengeProgress,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) {
        /**
         * One session collector per process.
         *
         * `MainActivity.onCreate` calls [start] on every Activity creation, so a
         * configuration change alone reaches it a second time; a second collector would
         * double every backfill pass for the rest of the process. The idiom is
         * `PlayGamesService.bootstrapped`'s, for the same reason.
         */
        private val started = AtomicBoolean(false)

        /**
         * One cloud pull per process (D-02) — a **dedicated** guard, not a by-product of
         * [started].
         *
         * RESEARCH Pitfall 6: `GamesService.session` is a `StateFlow`, so `collect` replays
         * the current value to each collector and the session can legitimately emit
         * [GamesSignInState.SignedIn] **twice** on one launch — once from
         * `bootstrapIfOptedIn` and once from an explicit `signIn()` (Phase 11's
         * `sdkMutex`/`bootstrapped` interplay). [started] guards the *collector*, which does
         * nothing about a second emission reaching the one collector that exists. Only a
         * guard consumed inside the pass can promise "exactly one [CloudSaveStore.load] per
         * process", which is what D-02 says.
         *
         * It is reset in exactly one place — [onProfileSwapResolved] — so that the account
         * the player just switched to is actually pulled.
         */
        private val pulled = AtomicBoolean(false)

        /**
         * Serializes passes (PGSL-05, T-12-72).
         *
         * [withLock] — wait — rather than `tryLock` — skip — and the choice is deliberate:
         * a skipped pass could discard the newest aggregate, whereas a queued one merely
         * re-reads and resubmits idempotent values. The cost of waiting is a duplicate
         * inert call; the cost of skipping is a lost update.
         *
         * **It is not reentrant.** [onProfileSwapResolved] does its resolution work under
         * this lock and then releases it before triggering a pass; calling [submitAll] from
         * inside the lock would deadlock a process-lifetime `@Singleton` permanently
         * (T-13-32).
         */
        private val passMutex = Mutex()

        private val _profileSwap = MutableStateFlow<ProfileSwapRequest?>(null)

        /**
         * The pending account-switch question, or `null` when there is none (D-12/D-15).
         *
         * Raised by step 1 of the pass when the stored player id disagrees with the
         * signed-in one, and cleared only by [onProfileSwapResolved]. Its one consumer is
         * `HomeViewModel`, which renders the single dialog this milestone reserves — on Home
         * only, never over gameplay, Results or Settings.
         *
         * While it is non-`null` **no push and no pull runs**: every pass returns at step 1
         * having touched neither store. That is not a UI courtesy, it is the mitigation —
         * see the class KDoc for why step 1 must precede step 2.
         */
        open val profileSwap: StateFlow<ProfileSwapRequest?> = _profileSwap.asStateFlow()

        /**
         * Starts the one session collector, which runs a pass whenever the session resolves
         * to [GamesSignInState.SignedIn] — the whole of PGSL-05 and the trigger half of
         * D-02.
         *
         * **Called explicitly from `MainActivity.onCreate`, and that is load-bearing
         * (RESEARCH Pitfall 5).** Hilt constructs a `@Singleton` lazily, on first injection.
         * A submitter injected only by `ResultsViewModel` would have no collector until the
         * player first reached a Results screen — so the backfill would never run for
         * exactly the player it was written for, the one who signed in from Settings and
         * closed the app, while passing every JVM test. That is why this method exists at
         * all, and why [started] guards it.
         *
         * The flag is read INSIDE the pass, under [passMutex], once per emission — never
         * here in the collector and never once outside it (WR-01). Reading it once outside
         * would freeze a launch-time value and miss every solve banked since; reading it
         * here, before the lock, would race the signed-out branch of [submitAll] that writes
         * it. That race loses a backfill: a solve banked while the session was still
         * `Unknown` begins its suspending `setPending(true)`, the session then resolves to
         * `SignedIn`, this collector reads the not-yet-committed `false` and skips, and the
         * write lands a moment later with nobody left to observe it — the session
         * `StateFlow` does not re-emit, so the work waits for the next signed-in solve or
         * the next process launch. Taking the lock FIRST and reading the flag inside is what
         * makes the session check and the flag check one consistent snapshot.
         */
        open fun start() {
            if (!started.compareAndSet(false, true)) return
            scope.launch {
                gamesService.session.collect { session ->
                    if (session is GamesSignInState.SignedIn) submitAll(onlyIfPending = true)
                }
            }
        }

        /**
         * Submits everything the player's stored progress now implies and pushes it to the
         * cloud — fire-and-forget (PGSL-04, D-13, D-01).
         *
         * The caller gets control back immediately: the whole body is a launch on the
         * application-lifetime scope, so the submission survives the Results screen being
         * navigated away from and cannot delay, fail or alter the solved level that
         * triggered it.
         *
         * **Nothing here may be made `suspend`.** Awaiting it from `viewModelScope` would
         * tie submission lifetime to the screen, and a back-navigation mid-submit would
         * cancel the very dirty-flag write that makes the failure recoverable — RESEARCH
         * Pattern 1's named anti-pattern, and the reason `ResultsViewModel` does not wrap
         * this call in anything.
         */
        open fun onResultRecorded() {
            scope.launch { submitAll(onlyIfPending = false) }
        }

        /**
         * Answers the D-12 profile-swap question, then runs a normal pass for the account
         * the device now belongs to.
         *
         * D-13 verbatim, and the two answers are not symmetrical:
         *  - **"Keep this device's progress"** ([keepLocalProgress] `true`) merges local into
         *    the new account's cloud save exactly like a first sign-in — no wipe, nothing
         *    lost on either side, because the merge is a monotonic union (CLOUD-02).
         *  - **"Use [account]'s cloud progress"** ([keepLocalProgress] `false`) replaces
         *    everything. D-14 names exactly four things: `level_result`, `play_session`, the
         *    Challenge ladder **and both** resume snapshots. The device becomes that
         *    account's device outright, which is safe precisely because the previous
         *    account's data already lives in its own cloud save from earlier pushes.
         *
         * **This function has no notion of "dismissed", deliberately.** D-13 makes back,
         * tap-outside and any other dismissal mean "Keep this device's progress"; that
         * mapping lives at the UI layer (plan 13-10) and is therefore expressed exactly
         * once, so the safe default cannot drift between two places that both think they own
         * it.
         *
         * **Nothing here may be made `suspend`**, for [onResultRecorded]'s reason with more
         * at stake: awaiting a wipe-and-import from `viewModelScope` would let a
         * back-navigation mid-write cancel it, leaving the device a half-replaced hybrid of
         * two accounts.
         *
         * @param keepLocalProgress `true` for D-13's merge, `false` for D-14's replace.
         */
        open fun onProfileSwapResolved(keepLocalProgress: Boolean) {
            scope.launch {
                if (!applySwapAnswer(keepLocalProgress)) return@launch
                // The lock is RELEASED before this line, and must be: passMutex is not
                // reentrant, so a pass started from inside it would deadlock the singleton
                // for the life of the process (T-13-32).
                submitAll(onlyIfPending = false)
            }
        }

        /**
         * Applies the answer under [passMutex] and reports whether a pass should follow.
         *
         * The wipe order — Room first, then the DataStore ladder — is deliberate: a failure
         * of the second leaves a ladder ahead of an empty database, which the next pull
         * simply re-fills, whereas the reverse would leave the old account's level rows
         * under the new account's reset ladder. Both calls are best-effort by their own
         * contract, so neither can throw into this application-scoped coroutine.
         *
         * [pulled] is reset here and nowhere else, so the account the player just chose is
         * actually pulled even though a pull already ran earlier in this process.
         */
        private suspend fun applySwapAnswer(keepLocalProgress: Boolean): Boolean =
            passMutex.withLock {
                val session = gamesService.session.value
                if (session !is GamesSignInState.SignedIn) return@withLock false
                if (!keepLocalProgress) {
                    progressRepository.resetAllProgress()
                    challengeProgress.resetAwaiting()
                }
                // D-15: after either answer the stored id becomes the signed-in one, so the
                // dialog cannot re-raise on the next pass.
                cloudProfileStore.setMergedPlayerId(session.playerId)
                _profileSwap.value = null
                pulled.set(false)
                true
            }

        /**
         * One serialized pass, in the five-step order the class KDoc specifies.
         *
         * @param onlyIfPending whether this pass exists solely to drain a pending backfill,
         *   and should therefore skip **step 3** when the flag is clear. `true` for the
         *   [start] collector — a clean launch by a signed-in player with nothing owed makes
         *   no SDK *submission* calls (D-10) — and `false` for [onResultRecorded] and
         *   [onProfileSwapResolved], whose fresh work is always worth submitting. It no
         *   longer gates steps 2 and 4; see the class KDoc's A7 section for why that change
         *   is reviewed rather than incidental. The read happens **inside** [passMutex] so
         *   it cannot observe a `setPending(true)` that a concurrent signed-out pass has
         *   begun but not committed (WR-01).
         */
        private suspend fun submitAll(onlyIfPending: Boolean) =
            passMutex.withLock {
                // [0] Session gate. `Unknown` takes this branch too (RESEARCH Pitfall 8). It
                // means "the first check has not answered", and reading it as "offline" — or
                // as "probably fine" — is the mistake this line exists to prevent: the
                // subsequent SignedIn emission triggers the backfill, so nothing is lost by
                // waiting. BOTH markers are set, because both backends are owed a retry.
                val session = gamesService.session.value
                if (session !is GamesSignInState.SignedIn) {
                    syncStore.setPending(true)
                    cloudSyncStore.setPushPending(true)
                    return@withLock
                }
                // [1] Profile gate, and it must be step 1 — above the pull. See the class
                // KDoc: a pull that ran first would already have merged the new account's
                // data into the old account's local store, unrecoverably.
                val mergedPlayerId = cloudProfileStore.mergedPlayerId.first()
                if (mergedPlayerId != null && mergedPlayerId != session.playerId) {
                    _profileSwap.value = ProfileSwapRequest(session.playerId, session.displayName)
                    return@withLock
                }
                val aggregates = PassAggregates(progressRepository)
                // [2] Pull — own guard, never gated on any dirty flag (D-02).
                val pulledPayload = pullOnce(session)
                // [3] Phase 12 submissions — the one step `onlyIfPending` still gates (A7).
                if (!onlyIfPending || syncStore.pending.first()) submitPhase12(aggregates)
                // [4] Cloud push (D-01).
                pushIfNeeded(onlyIfPending = onlyIfPending, pulledPayload = pulledPayload, aggregates = aggregates)
            }

        /**
         * The once-per-process pull (D-02), reporting whether it brought a payload back.
         *
         * It reports **`true` for any decodable payload, not only one that changed
         * something**, and that is the value step 4 treats as "push back in this pass". Both
         * narrower readings are wrong in a way worth naming. Gating the push on
         * [ImportOutcome.changed] would skip the write-back whenever the remote was a subset
         * of local — exactly the case where the *cloud* is the side that is behind. Gating it
         * on "the pull ran" would make every clean launch push, which is the property the
         * class KDoc's A7 section promises is preserved.
         *
         * A `null` from [CloudSaveStore.load] means "stay local" (CLOUD-05), never "the cloud
         * is empty": the two are indistinguishable from here, and pushing a fresh device's
         * empty payload over a real save on that guess is the single most damaging misreading
         * available. The same is true of bytes the codec refuses.
         *
         * The stored player id is written at the end rather than the beginning, so it records
         * a merge that actually happened. It is what makes the next launch's step 1
         * meaningful, and on a fresh install it is the write that turns "merge silently"
         * (D-02) into "this device belongs to this account".
         */
        private suspend fun pullOnce(session: GamesSignInState.SignedIn): Boolean {
            if (!pulled.compareAndSet(false, true)) return false
            val bytes = attemptOrNull { cloudSaveStore.load(CloudProgressCodec::mergeBytes) }
            val remote = bytes?.let(CloudProgressCodec::decode)
            if (remote != null) land(remote)
            cloudProfileStore.setMergedPlayerId(session.playerId)
            return remote != null
        }

        /**
         * Merges one decoded payload into the two local stores, and re-arms both markers when
         * it changed anything.
         *
         * Two stores, two calls, no transaction spanning them — see
         * [ChallengeProgress.mergeLadder] for why that is accepted rather than overlooked.
         * The ladder is merged even when no level row improved: the ladder and the bests are
         * independent fields of the payload, and a remote that is behind on levels can still
         * be ahead on the ladder.
         *
         * Setting [SubmissionSyncStore] here — before step 3 reads it — is what makes a
         * restored player's leaderboards catch up in this pass instead of the next one.
         */
        private suspend fun land(remote: CloudProgress) {
            val outcome = progressRepository.importProgress(remote, System.currentTimeMillis())
            challengeProgress.mergeLadder(remote.ladder)
            if (!outcome.changed) return
            syncStore.setPending(true)
            cloudSyncStore.setPushPending(true)
        }

        /** Step 3, unchanged from Phase 12 but for reading its aggregates through [aggregates]. */
        private suspend fun submitPhase12(aggregates: PassAggregates) {
            // Both reads already fail safe inside `storageFailSafe` (12-06), so neither
            // needs wrapping here — and note what that means for the flag write below.
            val totals = aggregates.read()
            val outcome = AchievementRules.evaluate(totals.aggregate)

            val results =
                submitLeaderboards(totals.aggregate, totals.classicBests) +
                    submitUnlocks(outcome) +
                    submitSteps(outcome)

            recordPassOutcome(issued = results.isNotEmpty(), allOk = results.all { it })
        }

        /**
         * Step 4: writes the merged payload with the listing metadata that makes the save
         * legible in the Play Games saves UI (D-01/D-11/CLOUD-03).
         *
         * ## The guard
         *
         * Three ways in: the trigger was a solve or a swap resolution ([onlyIfPending] is
         * `false`), a previous push is owed a retry ([CloudSyncStore.pushPending]), or this
         * pass pulled a payload ([pulledPayload]). The third is what makes two devices
         * converge in **one** round rather than two: having imported the remote, this device
         * now holds the union, and writing it back in the same pass means the other device's
         * next pull already sees everything. Without it the union would only reach the cloud
         * on this player's next solve.
         *
         * ## The ladder, and why it is not read from the StateFlow
         *
         * [ChallengeProgress.settledLevel] reads the value committed to disk.
         * [ChallengeProgress.currentLevel] is a `stateIn` cache whose synchronously readable
         * value re-emits **asynchronously** after [ChallengeProgress.mergeLadder] commits, so
         * in this pass — which merged the ladder moments ago in [land] — it can still hold
         * the pre-merge level, and the push would write back a ladder lower than the one it
         * had just imported. That is RESEARCH open question 4, and it is the whole reason
         * `settledLevel` exists. A build gate greps this file for that cached read and
         * expects zero hits, which is why this paragraph does not spell it.
         *
         * ## The marker bookkeeping
         *
         * [recordPassOutcome]'s three-case `when` is the precedent, with one deliberate
         * difference: the push is a **single** call, not a batch, so there is no "issued
         * nothing" middle case to preserve. Either it was attempted and the answer is known —
         * pending is "not committed" — or the guard declined and the flag is not touched at
         * all. An uncommitted outcome is not an error (see [CloudSaveStore.save]); it means
         * "nothing reached the cloud", and the answer is a retry on the next pass, never a
         * snackbar.
         *
         * [CloudSyncStore.recordPushOutcome] moves the marker and, in the same edit, records
         * the stage and status code the seam reported. Those two are a device diagnostic
         * that nothing reads back; the marker's meaning is unchanged by them.
         */
        private suspend fun pushIfNeeded(
            onlyIfPending: Boolean,
            pulledPayload: Boolean,
            aggregates: PassAggregates,
        ) {
            if (onlyIfPending && !pulledPayload && !cloudSyncStore.pushPending.first()) return
            val ladder = challengeProgress.settledLevel()
            val progress = progressRepository.exportProgress(ladder)
            val totals = aggregates.read()
            val description =
                CloudSaveDescription(
                    ladder = ladder,
                    totalStars = totals.aggregate.totalStars,
                    classicCleared = totals.classicBests.size,
                )
            val outcome =
                attemptPush {
                    cloudSaveStore.save(
                        bytes = CloudProgressCodec.encodeWithinCap(progress),
                        description = description,
                        playedTimeMs = progress.playedTimeMs,
                        mergeRemote = CloudProgressCodec::mergeBytes,
                    )
                }
            cloudSyncStore.recordPushOutcome(outcome)
        }

        /**
         * Moves the submissions dirty flag to match a completed pass.
         *
         * Three cases, written as a `when` so the middle one is impossible to read past: a
         * pass that **issued no calls at all** leaves the flag exactly as it was. See the
         * class KDoc — a storage fault and a brand-new player produce identical empty
         * reads, and only one of them has a backfill worth keeping.
         */
        private suspend fun recordPassOutcome(
            issued: Boolean,
            allOk: Boolean,
        ) {
            when {
                !allOk -> syncStore.setPending(true)
                issued -> syncStore.setPending(false)
                else -> Unit
            }
        }

        /**
         * The two Challenge leaderboards plus one per stored Classic best (PGSL-01/02/03).
         *
         * A `null` aggregate value is an OMISSION, not a zero: "no Challenge level has ever
         * been solved" is a different claim from "the highest solved Challenge level is 0",
         * and only one of them belongs on a public leaderboard. `challengeScoreTotal` is
         * already a `Long` because it is the SQL `SUM(bestScore)` (D-07, Pitfall 6) — a
         * few tens of thousands of levels overflow an `Int`, and the wrapped negative would
         * be submitted.
         */
        private suspend fun submitLeaderboards(
            aggregate: ProgressAggregate,
            bests: Map<ClassicBoard, Int>,
        ): List<Boolean> =
            buildList {
                aggregate.highestChallengeLevel?.let {
                    add(attempt { gamesService.submitScore(GamesIds.Leaderboards.challengeLadder, it.toLong()) })
                }
                aggregate.challengeScoreTotal?.let {
                    add(attempt { gamesService.submitScore(GamesIds.Leaderboards.challengeScore, it) })
                }
                bests.forEach { (board, best) ->
                    // A `null` lookup means "this level has no board leaderboard" — the
                    // correct answer, not an error — so it is SKIPPED without contributing a
                    // `false` that would set the dirty flag (PGSL-03, and 12-04's own KDoc).
                    val leaderboardId = GamesIds.Leaderboards.forClassic(ClassicBoardIds.levelIdFor(board))
                    if (leaderboardId != null) {
                        add(attempt { gamesService.submitScore(leaderboardId, best.toLong()) })
                    }
                }
            }

        /** The one-shot unlocks the rules produced. */
        private suspend fun submitUnlocks(outcome: AchievementOutcome): List<Boolean> =
            outcome.unlocked.map { attempt { gamesService.unlockAchievement(GamesIds.of(it)) } }

        /**
         * The tiers' ABSOLUTE step values, passed verbatim (D-03/D-12).
         *
         * There is deliberately **no `steps <= 0` filter here** (Pitfall 4). 12-02's
         * `AchievementOutcome` invariant already guarantees it, and a second filter would be
         * a silent second source of truth for the same rule; the guarantee is asserted by a
         * dedicated `ProgressSubmitterTest` case instead. Nothing is clamped to a tier's own
         * total either — Play Games auto-unlocks once steps reach it and ignores the rest.
         */
        private suspend fun submitSteps(outcome: AchievementOutcome): List<Boolean> =
            outcome.steps.map { (id, steps) -> attempt { gamesService.setAchievementSteps(GamesIds.of(id), steps) } }

        /**
         * Runs one seam call, degrading a throw to the `false` the seam is supposed to have
         * returned.
         *
         * The `InterstitialAdSlot`/`SettingsViewModel` idiom: `runCatching` around the single
         * call it guards, with cancellation rethrown before classification so structured
         * concurrency still works. A thrown failure is therefore indistinguishable from a
         * reported one and sets the dirty flag exactly the same way.
         *
         * **Every call is issued and every result collected — the verdict is `all { it }`
         * over the completed list, never a short-circuiting `&&`.** A short-circuit would
         * skip real submissions after the first failure and leave half the pass unattempted,
         * so the flag would promise a retry for work that had never been tried once.
         */
        private suspend fun attempt(block: suspend () -> Boolean): Boolean =
            runCatching { block() }
                .onFailure { if (it is CancellationException) throw it }
                .getOrDefault(false)

        /**
         * [attempt]'s sibling for the one seam call whose failure value is an uncommitted
         * [CloudSaveOutcome] — [CloudSaveStore.save].
         *
         * A throw is recorded as [CloudSaveStage.FAILED_OTHER] with no status code. Nothing
         * is read off the throwable, not even its type: the seam is where an SDK failure is
         * classified, and anything that escapes it is by definition unclassified.
         */
        private suspend fun attemptPush(block: suspend () -> CloudSaveOutcome): CloudSaveOutcome =
            runCatching { block() }
                .onFailure { if (it is CancellationException) throw it }
                .getOrDefault(CloudSaveOutcome(CloudSaveStage.FAILED_OTHER))

        /**
         * [attempt]'s sibling for the one seam call whose failure value is `null` rather than
         * `false` — [CloudSaveStore.load].
         *
         * A separate three-line function rather than a generic bending of [attempt], because
         * the two differ in the thing that matters: what "it failed" *is*. Collapsing them
         * would mean a caller choosing a default at the call site, which is exactly where a
         * `false` would one day be chosen for a `ByteArray?` and an empty payload pushed over
         * a real cloud save.
         *
         * The contract it buys is T-13-29 / CLOUD-05: a [CloudSaveStore] that **throws** is
         * indistinguishable from one that returns `null`, so an SDK exception can never
         * escape into the application-scoped coroutine that drives this pass and take the
         * process with it. Cancellation is rethrown before classification (WR-04).
         */
        private suspend fun <T> attemptOrNull(block: suspend () -> T?): T? =
            runCatching { block() }
                .onFailure { if (it is CancellationException) throw it }
                .getOrNull()
    }

/**
 * The account-switch question the Home dialog renders (D-12/D-13/D-15).
 *
 * It carries the **signed-in** player's id and display name — the account the device would
 * be moving to — never the stored one, because the dialog's copy names the arriving account
 * ("Use [account]'s cloud progress") and nothing in the UI ever needs the departing id.
 *
 * @property playerId the signed-in Play Games player id, written to [CloudProfileStore]
 *   after either answer.
 * @property displayName that player's display name, or `null` when the SDK's lookup failed
 *   or stalled (Phase 11 D-14) — a degraded name the UI falls back from, never an error.
 */
data class ProfileSwapRequest(
    val playerId: String,
    val displayName: String?,
)

/** The two SQL reads a pass makes, taken together so they describe one instant. */
private data class PassTotals(
    val aggregate: ProgressAggregate,
    val classicBests: Map<ClassicBoard, Int>,
)

/**
 * The pass's aggregates, read **at most once** however many steps ask for them.
 *
 * Steps 3 and 4 both need them — the submissions from the aggregate, the listing description
 * from `totalStars` and `classicBests.size` — and step 3 does not always run. Two independent
 * call sites would mean a pass that reads storage twice and, worse, could describe the save
 * from a different instant than the one it submitted, so this memoises instead. It is
 * constructed per pass and never shared, so there is no cross-pass staleness to reason about:
 * a pass always describes the database as it was when that pass first looked.
 */
private class PassAggregates(
    private val repository: ProgressRepository,
) {
    private var cached: PassTotals? = null

    suspend fun read(): PassTotals =
        cached ?: PassTotals(repository.aggregate(), repository.classicBests()).also { cached = it }
}
