package com.app.pegsolitaire.data

import androidx.room.withTransaction
import com.app.pegsolitaire.data.db.ClassicStanding
import com.app.pegsolitaire.data.db.LevelResultDao
import com.app.pegsolitaire.data.db.LevelResultEntity
import com.app.pegsolitaire.data.db.PegSolitaireDatabase
import com.app.pegsolitaire.data.db.PlaySessionDao
import com.app.pegsolitaire.data.db.PlaySessionEntity
import com.app.pegsolitaire.data.db.ResumeDao
import com.app.pegsolitaire.data.db.ResumeSnapshotEntity
import com.app.pegsolitaire.data.resume.ResumeSnapshotDto
import com.app.pegsolitaire.data.resume.SnapshotMapper
import com.app.pegsolitaire.domain.achievement.AchievementThresholds
import com.app.pegsolitaire.domain.achievement.ClassicBoard
import com.app.pegsolitaire.domain.achievement.ProgressAggregate
import com.app.pegsolitaire.domain.progress.CloudProgress
import com.app.pegsolitaire.domain.progress.LevelBest
import com.app.pegsolitaire.domain.progress.LevelKey
import com.app.pegsolitaire.domain.progress.ProgressMerge
import com.app.pegsolitaire.domain.progress.ProgressMode
import com.app.pegsolitaire.domain.scoring.ScoreResult
import com.app.pegsolitaire.domain.telemetry.SolveTelemetry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The single source of truth for what the player has achieved and what they can come
 * back to: per-level best results (PERS-01) and the at-most-two in-progress snapshots
 * (PERS-03).
 *
 * It hands out app-layer models only ([LevelResult], [NewBests], [ResumeSnapshotDto]) —
 * Room entities never leave the data layer, so a schema change cannot ripple into the
 * UI. Mirrors [SettingsRepository]'s interface + `Impl` + `@Binds` shape.
 *
 * ## Size suppression
 *
 * `TooManyFunctions` is suppressed here for the **same, already-documented reason it is
 * suppressed on [DefaultProgressRepository] below** (see that class's "Size suppression"
 * section): the member set is this store's fixed vocabulary, not accreted responsibility.
 * The impl crossed the threshold first and carried the suppression alone while the
 * interface sat at eleven members; `observeHomeProgress` — the Home ribbon's single
 * aggregate — is the twelfth, so the declaration the impl's justification was already
 * reasoning about now crosses it too. Splitting this interface to satisfy a counter would
 * mean a second declared owner of the same tables, which is the single-source-of-truth
 * property this repository exists to hold (research Anti-Pattern 2).
 */
@Suppress("TooManyFunctions") // see "Size suppression" above
interface ProgressRepository {
    /**
     * Merges a solved play into the stored bests for `(mode, levelId)` and reports which
     * fields improved.
     *
     * Each field is merged independently (D-07): a fast sloppy run keeps its time and a
     * slow perfect run keeps its score, so a replay can only ever improve a level's row.
     * The returned [NewBests] is what the Results screen renders its "New best!" cue
     * from (D-09). Only solved plays are recorded (D-08) — the caller decides that.
     *
     * @param nowMs epoch millis of this play; stored as `completedAt` only when
     *   something actually improved, keeping that column meaningful as "last improved".
     *
     * The write is best-effort (T-05-13): a storage failure drops this play's record —
     * stored bests keep their prior values — and reports "nothing improved" rather than
     * crashing the Results screen.
     *
     * **Both tables are written in ONE transaction (D-08).** The `level_result` upsert and
     * the `play_session` insert land together or not at all, so telemetry can never
     * disagree with the best it belongs to: a half-applied write would otherwise leave a
     * banked best with no telemetry (a permanently unearnable Hint-free) or a telemetry row
     * for a play whose best was never stored. The two tables mean different things —
     * `play_session` is append-only and describes THIS PLAY, while `level_result` holds the
     * merged best — which is why a replay appends a second session row but still updates
     * one result row.
     *
     * @param telemetry this play's per-solve facts (D-08). Deliberately has **no default**:
     *   a default would let a future call site silently drop telemetry, and the compiler
     *   catching every call site is exactly the point.
     */
    suspend fun recordResult(
        mode: String,
        levelId: String,
        result: ScoreResult,
        telemetry: SolveTelemetry,
        nowMs: Long,
    ): NewBests

    /**
     * Every stored result for [mode], re-emitted on change (Classic catalog pips, D-10).
     *
     * A failing database read (corrupt file, disk I/O) degrades to an empty list
     * (T-05-10): the catalog renders as unplayed rather than crashing.
     */
    fun observeResults(mode: String): Flow<List<LevelResult>>

    /**
     * The Home progress ribbon's two values, from ONE SQL aggregate, re-emitted on change
     * (§7 R-1/R-2, D-03c).
     *
     * Both values come from a single row so the two pills structurally cannot disagree, and
     * from SQL rather than a Kotlin fold over [observeResults] because folding
     * `MODE_CHALLENGE` would load the unbounded ladder into memory **on the start
     * destination** — the fold `LevelResultDao.totals`' KDoc already refuses for a
     * background submitter.
     *
     * A failing read degrades to [HomeProgress.UNKNOWN] (T-05-10, D-03h): the pills paint
     * "—", never `0`. That direction is the whole point — a player with stars already
     * earned must never be told their total is zero on the first screen they see.
     */
    fun observeHomeProgress(): Flow<HomeProgress>

    /**
     * The whole stored-progress snapshot the achievement rules consume, computed entirely
     * in SQL.
     *
     * Every field is produced by a `@Query` projection rather than a Kotlin fold over
     * rows, per the ROADMAP's Phase 12 note: the Challenge ladder reaches thousands of
     * rows, and folding them in memory would load the entire history to compute a `MAX`.
     * The only Kotlin work left is mapping a stored `levelId` to a [ClassicBoard].
     *
     * This is the SINGLE input to `AchievementRules.evaluate` (D-11), and it is computed
     * identically for a live solve — where it already reflects the result just recorded —
     * and for the offline backfill. That one shape is what removes the second rule set
     * D-11 forbids.
     *
     * Deliberately takes **no `mode` parameter**: [ProgressAggregate] spans both modes by
     * construction (the D-03 star tiers count stars everywhere), and a mode parameter
     * would invite exactly the per-mode split the achievement rules do not have.
     *
     * A storage failure degrades to `ProgressAggregate()` (T-12-34), which reads as "a
     * brand-new player" and therefore submits nothing rather than submitting something
     * wrong.
     *
     * **The `ChallengeProgress` off-by-one is expected, not a bug.**
     * `highestChallengeLevel` here is the highest level actually BANKED in `level_result`,
     * whereas [ChallengeProgress.currentLevel] stores `max(current, solved + 1)` — the
     * NEXT level to play. The two therefore normally differ by exactly one, and by more if
     * a best-effort [recordResult] write failed while `advancePast` succeeded. **This SQL
     * value is the authoritative one**: the ROADMAP mandates it and it matches what the
     * player actually banked. Do not "reconcile" the difference.
     */
    suspend fun aggregate(): ProgressAggregate

    /**
     * Each stored Classic board's best score, keyed by the domain enum — the input to the
     * five per-board Classic leaderboards (PGSL-03).
     *
     * Keying by [ClassicBoard] rather than by id text is what keeps the on-disk id strings
     * inside the data layer (QUAL-01); the translation happens once, in [ClassicBoardIds].
     * A board that has never been solved is simply **absent** from the map rather than
     * present with a zero, so a caller cannot accidentally submit a score of 0 for a board
     * the player has not played. A storage failure degrades to an empty map.
     */
    suspend fun classicBests(): Map<ClassicBoard, Int>

    /**
     * Saves [dto] as the resume point for its own [ResumeSnapshotDto.mode], replacing any
     * snapshot already stored there (D-03/D-04). Best-effort (T-05-13): a storage failure
     * drops the save — the previously stored snapshot, if any, survives untouched.
     */
    suspend fun saveSnapshot(
        dto: ResumeSnapshotDto,
        nowMs: Long = System.currentTimeMillis(),
    )

    /**
     * The snapshot [mode] can resume into, or `null` when there is none **or the stored
     * blob is unusable** — a corrupt or mismatched payload degrades to "no resume" rather
     * than throwing (Security §V5), and so does a read that fails at the database itself
     * (T-05-10). An unusable row is also **purged** (WR-02), so the
     * Continue affordance genuinely disappears instead of lingering as a phantom tile
     * that can only ever start a fresh board.
     */
    suspend fun loadSnapshot(mode: String): ResumeSnapshotDto?

    /**
     * Drops [mode]'s snapshot — solved, restarted, or new game (D-05). Best-effort
     * (T-05-13): a storage failure is swallowed rather than crashing the caller.
     */
    suspend fun clearSnapshot(mode: String)

    /**
     * The modes currently holding a snapshot; drives the Home "Continue" tiles (D-01/D-11).
     * A failing database read degrades to "no modes" — no tiles, never a crash (T-05-10).
     */
    fun observeResumeModes(): Flow<List<String>>

    /**
     * Everything stored here that follows the player's Play Games account, as the pure
     * `:domain` payload (CLOUD-01): every level's bests, plus [ladder], plus D-10's played
     * time.
     *
     * [ladder] is a **parameter** rather than a read of `ChallengeProgress`, deliberately:
     * it keeps this repository free of the DataStore it does not own, and it leaves the
     * caller (`ProgressSubmitter`) holding the ordering of a sync pass — pull, merge the
     * ladder, then export the settled value.
     *
     * A row whose stored `mode` names neither half of the game is **dropped** from the
     * payload rather than being fatal to it, the same totality argument
     * [ClassicBoardIds.boardFor] makes for an unrecognised level id.
     *
     * A storage failure degrades to a payload with no bests and no played time. The
     * supplied [ladder] survives that degradation because it did not come from the store
     * that failed, and a payload is never a *replacement* for the cloud save — the cloud
     * side merges it — so an empty-bests payload can only fail to add, never subtract.
     */
    suspend fun exportProgress(ladder: Int): CloudProgress

    /**
     * Merges a decoded cloud payload into local storage and reports whether anything
     * actually landed (CLOUD-01 / CLOUD-02).
     *
     * Every level is merged field by field through the same `ProgressMerge` join
     * [recordResult] runs, so the operation is **monotonic** (a remote row that is worse
     * on a field cannot regress that field) and **idempotent** (importing the same payload
     * twice writes nothing the second time). The union of the two sides becomes visible to
     * the UI for free, through the [observeResults] Flow that is already collected.
     *
     * **It returns [ImportOutcome] and never [NewBests] — that is the CLOUD-04/D-03
     * guarantee, expressed as a type.** A cloud import is per-merge, not per-play: it must
     * not fire the "New best!" cue or the Results celebration, and because the only value
     * that can drive those is a [NewBests] this function cannot produce, the celebration
     * path is not merely avoided here, it is unreachable.
     *
     * **Two things it deliberately does not touch, both requirements:**
     *  - the resume snapshot (CLOUD-04) — the in-progress board never leaves the device and
     *    is never destroyed by a cloud event. D-04 is the intended consequence: a merged
     *    ladder may jump from 10 to 50 while a level-10 game stays resumable underneath it.
     *  - `play_session` (D-06) — telemetry describes plays that happened *on this device*.
     *    Fabricating rows for imported progress would silently grant the hint-free, speed
     *    and centre-finish achievements for plays that never happened.
     *
     * It also does not write the Challenge ladder: that lives in DataStore, and the caller
     * owns the ordering between the two stores (see `ChallengeProgress.mergeLadder`).
     *
     * @param nowMs stamped as `completedAt` on a level arriving for the first time. A level
     *   that already had a row keeps its own stamp — `completedAt` means "last improved on
     *   this device", so it is neither merged nor synced.
     *
     * The write is best-effort and guarded at the **transaction** boundary (T-05-13): a
     * failing read aborts the entire import rather than degrading to "no local rows", which
     * would let a stale remote row overwrite better stored bests. Failure reports "nothing
     * changed" and leaves storage exactly as it was (CLOUD-05).
     */
    suspend fun importProgress(
        remote: CloudProgress,
        nowMs: Long,
    ): ImportOutcome

    /**
     * Empties every table this repository owns: the per-level bests, the telemetry history
     * and **both** resume snapshots.
     *
     * **D-14's profile-replace path only.** Its single caller is `ProgressSubmitter`'s
     * profile-swap resolution (plan 13-08), reached only when the player answered the one
     * dialog this milestone reserves with "use the other account's cloud progress". Nothing
     * else may call it — in particular no import, no conflict resolution and no sign-out.
     *
     * It does **not** reset the Challenge ladder, which lives in DataStore rather than in
     * this database; that same caller resets it through `ChallengeProgress.resetAwaiting()`.
     *
     * Best-effort (T-05-13): a storage failure leaves the wipe undone rather than crashing.
     */
    suspend fun resetAllProgress()
}

/**
 * Which fields of a level's stored bests a play just improved — the input to the
 * "New best!" cue (D-09). All three are `true` the first time a level is solved.
 */
data class NewBests(
    val stars: Boolean,
    val time: Boolean,
    val score: Boolean,
) {
    /** Whether anything at all improved (i.e. whether to show the cue). */
    val any: Boolean get() = stars || time || score
}

/**
 * What a cloud import did to local storage: whether anything changed, and how many level
 * rows were written.
 *
 * **This is deliberately not [NewBests], and must never be converted into one or routed to
 * `ResultsUiState`.** [NewBests] answers "did the play the player just finished beat their
 * record" and drives the "New best!" cue and the Results celebration; this answers "did a
 * background merge with another device's progress add anything", which is a silent event by
 * D-03. Two different questions with the same shape would be one careless `map` away from
 * congratulating a player for a level they solved last week on another phone — a separate
 * type is what makes that impossible rather than merely discouraged.
 *
 * @property changed `true` when at least one stored row differs from what it was; the
 *   caller uses it to decide whether the Phase 12 submission pass needs re-firing.
 * @property rowsWritten How many level rows the merge actually wrote — always `0` when
 *   [changed] is `false`, since only genuinely improved rows are upserted.
 */
data class ImportOutcome(
    val changed: Boolean,
    val rowsWritten: Int,
) {
    companion object {
        /** The no-op outcome: a worse-or-equal payload, or a storage failure (CLOUD-05). */
        val UNCHANGED = ImportOutcome(changed = false, rowsWritten = 0)
    }
}

/**
 * A level's stored bests, as the UI layer sees them: a plain read model mapped from
 * [LevelResultEntity] so no Room type reaches a ViewModel.
 */
data class LevelResult(
    val mode: String,
    val levelId: String,
    val stars: Int,
    val bestTimeMs: Long,
    val bestScore: Int,
    val completedAt: Long,
)

/**
 * The Home progress ribbon's two derived values, as the UI layer sees them — a plain read
 * model mapped from `HomeProgressTotals` so no Room type reaches a ViewModel, exactly as
 * [LevelResult] does (§7 R-6).
 *
 * @property totalStars stars earned across BOTH modes — the "Stars" pill is a total.
 * @property classicBoardsCleared how many of the Classic catalogue's boards have been
 *   cleared. The pill's **denominator** is deliberately not here: it is the catalogue size
 *   (`Boards.CLASSIC_SPECS.size`), a compile-time fact, and reading it from storage would
 *   let a stored row change a shipped number.
 */
data class HomeProgress(
    val totalStars: Int,
    val classicBoardsCleared: Int,
) {
    companion object {
        /**
         * "Not yet known" — what a **failed** read degrades to (T-05-10, D-03h), so the
         * pills paint the "—" placeholder.
         *
         * Both fields are `-1` rather than `0` precisely so this value can never be
         * mistaken for a real reading: `0` is a legitimate answer for a brand-new player,
         * and a player with forty stars being told they have none is the failure this
         * exists to prevent. Callers must test against this value, never against the
         * fields — which is why the screen's own placeholder rule is a `null` in
         * `HomeUiState` and this never reaches it verbatim.
         */
        val UNKNOWN = HomeProgress(totalStars = UNKNOWN_VALUE, classicBoardsCleared = UNKNOWN_VALUE)

        /** The out-of-range sentinel behind [UNKNOWN]; no real count can ever be negative. */
        private const val UNKNOWN_VALUE = -1
    }
}

/**
 * Room-backed [ProgressRepository].
 *
 * [recordResult] is a read-modify-write inside `withTransaction`: two plays finishing
 * concurrently (a background save racing a Results write) cannot interleave into a lost
 * update, and the "which fields improved" answer is computed against the same row that
 * is then written. The read-modify-write deliberately happens in Kotlin rather than in
 * SQL because the DAO cannot report *what* improved (see `LevelResultDao`).
 *
 * The merge it applies is [ProgressMerge], the pure `:domain` join — the very same
 * function the cloud import and the Saved Games conflict loop run (CLOUD-02). Recording a
 * solve locally and receiving that solve from another device are therefore the same
 * operation, proven commutative, associative and idempotent by `ProgressMergeTest`.
 *
 * The resume blob is treated as untrusted on the way in: [loadSnapshot] decodes
 * defensively and additionally refuses a payload whose own `mode` is not the key it was
 * filed under, so a tampered row can only ever cost the player a resume.
 *
 * ## Size suppression
 *
 * `TooManyFunctions` is suppressed because the count is [ProgressRepository]'s own member
 * set plus four private helpers, not accreted responsibility: twelve overrides are fixed by
 * the interface, and the helpers ([recordResultInTransaction], [importInTransaction],
 * `List<ClassicStanding>.boards` and the two entity mappers) exist so that a rule — the
 * merge, the transaction boundary, the entity-to-model shape — is stated exactly once
 * rather than repeated per call site. Splitting the class to satisfy the threshold would
 * mean a second writer of the same tables, which is the single-source-of-truth property
 * this repository exists to hold (research Anti-Pattern 2).
 */
@Suppress("TooManyFunctions") // see "Size suppression" above
@Singleton
class DefaultProgressRepository
    @Inject
    constructor(
        private val database: PegSolitaireDatabase,
        private val levelResultDao: LevelResultDao,
        private val resumeDao: ResumeDao,
        private val playSessionDao: PlaySessionDao,
    ) : ProgressRepository {
        override suspend fun recordResult(
            mode: String,
            levelId: String,
            result: ScoreResult,
            telemetry: SolveTelemetry,
            nowMs: Long,
        ): NewBests =
            // Best-effort (T-05-13), guarded at the TRANSACTION boundary deliberately: a
            // throwing `find` must abort the whole merge, because degrading it to "no
            // previous row" inside the transaction would let the upsert overwrite better
            // stored bests. Failure drops the write and reports "nothing improved".
            storageFailSafe(fallback = NewBests(stars = false, time = false, score = false)) {
                database.withTransaction {
                    recordResultInTransaction(mode, levelId, result, telemetry, nowMs)
                }
            }

        private suspend fun recordResultInTransaction(
            mode: String,
            levelId: String,
            result: ScoreResult,
            telemetry: SolveTelemetry,
            nowMs: Long,
        ): NewBests {
            val previous = levelResultDao.find(mode, levelId)
            val timeMs = result.elapsedSeconds * ProgressConstants.MILLIS_IN_SECOND
            val newBests =
                NewBests(
                    stars = previous == null || result.stars > previous.stars,
                    time = previous == null || timeMs < previous.bestTimeMs,
                    score = previous == null || result.score > previous.bestScore,
                )
            val thisPlay =
                LevelBest(
                    stars = result.stars,
                    bestTimeMs = timeMs,
                    bestScore = result.score,
                )
            // The SAME join the cloud import and the Saved Games conflict loop run
            // (CLOUD-02) — one named merge rather than three hand-written copies free to
            // drift apart. A first solve is `thisPlay` verbatim rather than a merge
            // against a fabricated identity row: the two are equal by the identity law,
            // but not inventing a row keeps the "no previous" case obviously lossless.
            //
            // `completedAt` is deliberately NOT merged. It means "last improved on THIS
            // device", so it has no cross-device meaning and stays out of `LevelBest`
            // entirely; its expression below is unchanged.
            val merged =
                previous?.let { row ->
                    ProgressMerge.merge(
                        thisPlay,
                        LevelBest(
                            stars = row.stars,
                            bestTimeMs = row.bestTimeMs,
                            bestScore = row.bestScore,
                        ),
                    )
                } ?: thisPlay
            levelResultDao.upsert(
                LevelResultEntity(
                    mode = mode,
                    levelId = levelId,
                    stars = merged.stars,
                    bestTimeMs = merged.bestTimeMs,
                    bestScore = merged.bestScore,
                    completedAt = if (newBests.any) nowMs else previous?.completedAt ?: nowMs,
                ),
            )
            // Inside the SAME transaction as the upsert above (D-08), and built from THIS
            // PLAY's values rather than the merged bests. Note the deliberate asymmetry of
            // the two timestamps: `completedAt` on the result row means "last improved" and
            // only moves on an improvement, whereas `finishedAt` here means "this play
            // happened" and is stamped every time.
            playSessionDao.insert(
                PlaySessionEntity(
                    mode = mode,
                    levelId = levelId,
                    stars = result.stars,
                    score = result.score,
                    elapsedMs = timeMs,
                    hintsUsed = telemetry.hintsUsed,
                    undosUsed = telemetry.undosUsed,
                    lastPegOnTarget = telemetry.lastPegOnTarget,
                    finishedAt = nowMs,
                ),
            )
            return newBests
        }

        override suspend fun exportProgress(ladder: Int): CloudProgress =
            // Fails open to "nothing stored" (CLOUD-05), keeping the caller-supplied ladder:
            // it came from DataStore, not from the database that just failed, and dropping
            // it would push a level-1 ladder for no reason.
            storageFailSafe(fallback = CloudProgress(ladder = ladder)) {
                // Both reads share ONE transaction so the payload is internally consistent:
                // a solve landing between them cannot produce a payload whose played time
                // knows about a play its bests do not.
                database.withTransaction {
                    CloudProgress(
                        ladder = ladder,
                        playedTimeMs = playSessionDao.totalElapsedMs(),
                        bests =
                            levelResultDao
                                .all()
                                .mapNotNull { row ->
                                    // A row naming neither mode (legacy or forged) is dropped,
                                    // never fatal — the same totality ClassicBoardIds.boardFor has.
                                    ProgressModeIds.domainModeFor(row.mode)?.let { mode ->
                                        LevelKey(mode, row.levelId) to row.toLevelBest()
                                    }
                                }.toMap(),
                    )
                }
            }

        override suspend fun importProgress(
            remote: CloudProgress,
            nowMs: Long,
        ): ImportOutcome =
            // Best-effort (T-05-13), guarded at the TRANSACTION boundary for the same reason
            // recordResult is: a throwing `all()` must abort the whole import, because
            // degrading it to "no local rows" inside the transaction would let every stale
            // remote row clobber a better stored best (T-13-15/T-13-18).
            storageFailSafe(fallback = ImportOutcome.UNCHANGED) {
                database.withTransaction { importInTransaction(remote, nowMs) }
            }

        /**
         * One bulk read-merge-write rather than [recordResult]'s per-row read-modify-write.
         *
         * The lost-update concern that justifies RMW does not arise here, because the read,
         * the merge and the write are already inside a single transaction — there is no
         * window for a concurrent writer to slip between them. What the per-row shape would
         * cost is real: a full Challenge ladder is ten thousand rows, so it would mean ten
         * thousand point queries for one sync pass.
         *
         * Only rows the merge actually improved are written, which is what makes a replayed
         * payload cost one read and nothing else.
         */
        private suspend fun importInTransaction(
            remote: CloudProgress,
            nowMs: Long,
        ): ImportOutcome {
            val storedByKey =
                levelResultDao
                    .all()
                    .mapNotNull { row ->
                        ProgressModeIds.domainModeFor(row.mode)?.let { mode -> LevelKey(mode, row.levelId) to row }
                    }.toMap()
            val local = CloudProgress(bests = storedByKey.mapValues { (_, row) -> row.toLevelBest() })
            // The SAME join `recordResult` runs on a local solve (CLOUD-02): receiving a
            // solve from another device and recording one here are one operation, proven
            // commutative, associative and idempotent by ProgressMergeTest. Only `bests` is
            // taken — the ladder lives in DataStore and is merged by its owner.
            val merged = ProgressMerge.merge(local, remote).bests
            val improved = merged.filter { (key, best) -> local.bests[key] != best }
            if (improved.isEmpty()) {
                return ImportOutcome.UNCHANGED
            }
            levelResultDao.upsertAll(
                improved.map { (key, best) ->
                    LevelResultEntity(
                        mode = ProgressModeIds.storedModeFor(key.mode),
                        levelId = key.levelId,
                        stars = best.stars,
                        bestTimeMs = best.bestTimeMs,
                        bestScore = best.bestScore,
                        // "Last improved on THIS device": an existing row keeps its own
                        // stamp, a newly arriving level is stamped with the import time.
                        completedAt = storedByKey[key]?.completedAt ?: nowMs,
                    )
                },
            )
            return ImportOutcome(changed = true, rowsWritten = improved.size)
        }

        override suspend fun resetAllProgress() {
            // Best-effort (T-05-13). One transaction so the device never ends up as a
            // half-replaced hybrid of two accounts' progress.
            storageFailSafe(fallback = Unit) {
                database.withTransaction {
                    levelResultDao.deleteAll()
                    playSessionDao.deleteAll()
                    resumeDao.clearAll()
                }
            }
        }

        override suspend fun aggregate(): ProgressAggregate =
            // Fails safe to a fresh-player aggregate (T-12-34): a corrupt database submits
            // nothing rather than crashing Results or submitting a half-read number.
            storageFailSafe(fallback = ProgressAggregate()) {
                // All three reads share ONE transaction so the snapshot is internally
                // consistent: a solve landing between the totals query and the flags query
                // cannot produce an aggregate claiming a level the star total does not
                // know about.
                database.withTransaction {
                    val totals =
                        levelResultDao.totals(
                            challengeMode = ProgressPersistenceKeys.MODE_CHALLENGE,
                            threeStars = AchievementThresholds.PERFECT_STARS,
                        )
                    val standings = levelResultDao.classicStandings(ProgressPersistenceKeys.MODE_CLASSIC)
                    val flags =
                        playSessionDao.flags(
                            classicMode = ProgressPersistenceKeys.MODE_CLASSIC,
                            english33Id = ClassicLevelIds.ENGLISH33,
                            speedLimitMs = AchievementThresholds.SPEED_SOLVE_MAX_ELAPSED_MS,
                        )
                    ProgressAggregate(
                        highestChallengeLevel = totals.highestChallengeLevel,
                        challengeScoreTotal = totals.challengeScoreTotal,
                        totalStars = totals.totalStars,
                        solvedLevelCount = totals.solvedLevelCount,
                        threeStarCount = totals.threeStarCount,
                        // Both sets come from the SAME standings read, so a Grand tour or
                        // Perfectionist answer can never disagree with the per-board set it
                        // was counted from (D-04).
                        clearedClassicBoards = standings.boards(),
                        perfectClassicBoards = standings.boards(minStars = AchievementThresholds.PERFECT_STARS),
                        anyHintFreeSolve = flags.anyHintFreeSolve,
                        anyUndoFreeSolve = flags.anyUndoFreeSolve,
                        anyCenterFinish = flags.anyCenterFinish,
                        anySpeedSolve = flags.anySpeedSolve,
                    )
                }
            }

        override suspend fun classicBests(): Map<ClassicBoard, Int> =
            storageFailSafe(fallback = emptyMap()) {
                levelResultDao
                    .classicStandings(ProgressPersistenceKeys.MODE_CLASSIC)
                    .mapNotNull { standing ->
                        // An id naming no shipped board (legacy or forged) is dropped, never
                        // fatal — a leaderboard read must not die on one stale row.
                        ClassicBoardIds.boardFor(standing.levelId)?.let { it to standing.bestScore }
                    }.toMap()
            }

        /**
         * The [ClassicBoard]s these standings represent, optionally narrowed to those at or
         * above [minStars]. An id naming no shipped board is dropped.
         */
        private fun List<ClassicStanding>.boards(minStars: Int = ProgressConstants.ANY_STARS): Set<ClassicBoard> =
            filter { it.stars >= minStars }
                .mapNotNull { ClassicBoardIds.boardFor(it.levelId) }
                .toSet()

        override fun observeResults(mode: String): Flow<List<LevelResult>> =
            levelResultDao
                .observe(mode)
                .map { rows -> rows.map { it.toLevelResult() } }
                // A read that fails at the database (corrupt file, disk I/O) renders as
                // "nothing solved yet" instead of crashing the catalog (T-05-10).
                .catch { emit(emptyList()) }

        override fun observeHomeProgress(): Flow<HomeProgress> =
            levelResultDao
                .observeHomeProgress(
                    classicMode = ProgressPersistenceKeys.MODE_CLASSIC,
                    minStars = ProgressConstants.ANY_STARS,
                ).map { HomeProgress(totalStars = it.totalStars, classicBoardsCleared = it.classicBoardsCleared) }
                // A failing read renders as "not yet known" — the "—" placeholder — and
                // never as `0` (T-05-10, D-03h). Zero is a real answer for a new player,
                // so reporting it for a failed read would lie to an experienced one.
                .catch { emit(HomeProgress.UNKNOWN) }

        override suspend fun saveSnapshot(
            dto: ResumeSnapshotDto,
            nowMs: Long,
        ) {
            // Best-effort (T-05-13): a failed save is dropped — losing one resume point
            // beats crashing the app while the player is backgrounding it.
            storageFailSafe(fallback = Unit) {
                resumeDao.upsert(
                    ResumeSnapshotEntity(
                        mode = dto.mode,
                        jsonBlob = SnapshotMapper.encode(dto),
                        updatedAt = nowMs,
                    ),
                )
            }
        }

        override suspend fun loadSnapshot(mode: String): ResumeSnapshotDto? =
            // A DAO read that throws (corrupt DB, disk I/O) fails safe to the same "no
            // resume" a missing row yields, matching the blob guard below (T-05-10).
            storageFailSafe(fallback = null) {
                val entity = resumeDao.get(mode) ?: return@storageFailSafe null
                val dto =
                    SnapshotMapper
                        .decode(entity.jsonBlob)
                        // A payload claiming a different mode than the row it sits in is tampered
                        // or corrupt state; refuse it rather than resuming the wrong game.
                        ?.takeIf { it.mode == mode }
                // A row that cannot be used is purged, not just skipped (WR-02): leaving it
                // would keep the mode listed in observeResumeModes forever, feeding Home a
                // phantom "Continue" tile that can only ever start a fresh board.
                if (dto == null) resumeDao.clear(mode)
                dto
            }

        override suspend fun clearSnapshot(mode: String) {
            // Best-effort (T-05-13). Worst case a stale snapshot lingers; loadSnapshot's
            // own guards (staleness/purge, T-05-03/WR-02) already cope with that.
            storageFailSafe(fallback = Unit) {
                resumeDao.clear(mode)
            }
        }

        override fun observeResumeModes(): Flow<List<String>> =
            resumeDao
                .observeModes()
                // Same fail-safe as observeResults: a failing read means no Continue
                // tiles on Home, never a crash (T-05-10).
                .catch { emit(emptyList()) }

        private fun LevelResultEntity.toLevelResult(): LevelResult =
            LevelResult(
                mode = mode,
                levelId = levelId,
                stars = stars,
                bestTimeMs = bestTimeMs,
                bestScore = bestScore,
                completedAt = completedAt,
            )

        /**
         * This row's three merged fields as `:domain` states them. `completedAt` is absent
         * from [LevelBest] on purpose — it is device-local and is never synced.
         */
        private fun LevelResultEntity.toLevelBest(): LevelBest =
            LevelBest(stars = stars, bestTimeMs = bestTimeMs, bestScore = bestScore)
    }

/**
 * The single data-boundary translation between the on-disk `mode` column and `:domain`'s
 * [ProgressMode], in the same shape — and for the same reason — as [ClassicBoardIds].
 *
 * It exists because the stored column value, the domain enum and the cloud payload's wire
 * token are three formats that must be free to change apart: renaming a persistence
 * constant must not silently orphan every cloud save already written. The payload's half of
 * that translation lives in `data/cloud/`; this is the database's half.
 *
 * The two directions are deliberately asymmetric in their failure mode, exactly as
 * [ClassicBoardIds]' are.
 */
private object ProgressModeIds {
    /**
     * The [ProgressMode] a stored `mode` column names, or `null` when it names neither half
     * of the game.
     *
     * `null` is the correct answer rather than an error: a legacy or forged value is simply
     * "not a mode this build knows", and an export must drop such a row rather than fail the
     * whole payload over it.
     */
    fun domainModeFor(stored: String): ProgressMode? =
        when (stored) {
            ProgressPersistenceKeys.MODE_CLASSIC -> ProgressMode.CLASSIC
            ProgressPersistenceKeys.MODE_CHALLENGE -> ProgressMode.CHALLENGE
            else -> null
        }

    /**
     * The `mode` column value for [mode].
     *
     * Deliberately has **no** `else` branch: a third game mode must fail the build here
     * rather than quietly import as one of the existing two.
     */
    fun storedModeFor(mode: ProgressMode): String =
        when (mode) {
            ProgressMode.CLASSIC -> ProgressPersistenceKeys.MODE_CLASSIC
            ProgressMode.CHALLENGE -> ProgressPersistenceKeys.MODE_CHALLENGE
        }
}

/**
 * Scoped constants for this repository's unit conversions and query narrowing (QUAL-03 —
 * no inline magic numbers in logic). Kept separate from `ProgressPersistenceKeys`, which
 * owns the on-disk *names*.
 *
 * The merge's own floors are **not** here: `NO_STARS`, `NO_SCORE` and `NO_TIME_RECORDED`
 * moved to `ProgressMergeLimits` in `:domain` when `ProgressMerge` was named, so the
 * identities of the merge are stated exactly once and the cloud path cannot end up using
 * a different zero from the local one.
 */
private object ProgressConstants {
    /** `ScoreResult` reports whole seconds; the stored best time is in milliseconds. */
    const val MILLIS_IN_SECOND = 1_000L

    /**
     * The "do not narrow by stars" floor for the cleared-boards read: a stored row means
     * the board was cleared regardless of how well. The *perfect* narrowing binds
     * `AchievementThresholds.PERFECT_STARS` instead, so the 3 is never restated here.
     */
    const val ANY_STARS = 0
}
