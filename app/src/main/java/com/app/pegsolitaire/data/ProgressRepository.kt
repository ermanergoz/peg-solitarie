package com.app.pegsolitaire.data

import androidx.room.withTransaction
import com.app.pegsolitaire.data.db.ClassicStanding
import com.app.pegsolitaire.data.db.LevelResultDao
import com.app.pegsolitaire.data.db.LevelResultEntity
import com.app.pegsolitaire.data.db.PegSolitaireDatabase
import com.app.pegsolitaire.data.db.PlaySessionDao
import com.app.pegsolitaire.data.db.ResumeDao
import com.app.pegsolitaire.data.db.ResumeSnapshotEntity
import com.app.pegsolitaire.data.resume.ResumeSnapshotDto
import com.app.pegsolitaire.data.resume.SnapshotMapper
import com.app.pegsolitaire.domain.achievement.AchievementThresholds
import com.app.pegsolitaire.domain.achievement.ClassicBoard
import com.app.pegsolitaire.domain.achievement.ProgressAggregate
import com.app.pegsolitaire.domain.scoring.ScoreResult
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
 */
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
     */
    suspend fun recordResult(
        mode: String,
        levelId: String,
        result: ScoreResult,
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
 * Room-backed [ProgressRepository].
 *
 * [recordResult] is a read-modify-write inside `withTransaction`: two plays finishing
 * concurrently (a background save racing a Results write) cannot interleave into a lost
 * update, and the "which fields improved" answer is computed against the same row that
 * is then written. The merge itself deliberately lives here rather than in SQL because
 * the DAO cannot report *what* improved (see `LevelResultDao`).
 *
 * The resume blob is treated as untrusted on the way in: [loadSnapshot] decodes
 * defensively and additionally refuses a payload whose own `mode` is not the key it was
 * filed under, so a tampered row can only ever cost the player a resume.
 */
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
            nowMs: Long,
        ): NewBests =
            // Best-effort (T-05-13), guarded at the TRANSACTION boundary deliberately: a
            // throwing `find` must abort the whole merge, because degrading it to "no
            // previous row" inside the transaction would let the upsert overwrite better
            // stored bests. Failure drops the write and reports "nothing improved".
            storageFailSafe(fallback = NewBests(stars = false, time = false, score = false)) {
                database.withTransaction {
                    recordResultInTransaction(mode, levelId, result, nowMs)
                }
            }

        private suspend fun recordResultInTransaction(
            mode: String,
            levelId: String,
            result: ScoreResult,
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
            levelResultDao.upsert(
                LevelResultEntity(
                    mode = mode,
                    levelId = levelId,
                    stars = maxOf(result.stars, previous?.stars ?: ProgressConstants.NO_STARS),
                    bestTimeMs = minOf(timeMs, previous?.bestTimeMs ?: Long.MAX_VALUE),
                    bestScore = maxOf(result.score, previous?.bestScore ?: ProgressConstants.NO_SCORE),
                    completedAt = if (newBests.any) nowMs else previous?.completedAt ?: nowMs,
                ),
            )
            return newBests
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
    }

/**
 * Scoped constants for the progress merge (QUAL-03 — no inline magic numbers in logic).
 * Kept separate from `ProgressPersistenceKeys`, which owns the on-disk *names*.
 */
private object ProgressConstants {
    /** `ScoreResult` reports whole seconds; the stored best time is in milliseconds. */
    const val MILLIS_IN_SECOND = 1_000L

    /** The "no previous row" floor for the max-stars merge. */
    const val NO_STARS = 0

    /** The "no previous row" floor for the max-score merge. */
    const val NO_SCORE = 0

    /**
     * The "do not narrow by stars" floor for the cleared-boards read: a stored row means
     * the board was cleared regardless of how well. The *perfect* narrowing binds
     * `AchievementThresholds.PERFECT_STARS` instead, so the 3 is never restated here.
     */
    const val ANY_STARS = 0
}
