package com.app.pegsolitaire.data

import androidx.room.withTransaction
import com.app.pegsolitaire.data.db.LevelResultDao
import com.app.pegsolitaire.data.db.LevelResultEntity
import com.app.pegsolitaire.data.db.PegSolitaireDatabase
import com.app.pegsolitaire.data.db.ResumeDao
import com.app.pegsolitaire.data.db.ResumeSnapshotEntity
import com.app.pegsolitaire.data.resume.ResumeSnapshotDto
import com.app.pegsolitaire.data.resume.SnapshotMapper
import com.app.pegsolitaire.domain.scoring.ScoreResult
import kotlinx.coroutines.flow.Flow
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
     */
    suspend fun recordResult(
        mode: String,
        levelId: String,
        result: ScoreResult,
        nowMs: Long,
    ): NewBests

    /** Every stored result for [mode], re-emitted on change (Classic catalog pips, D-10). */
    fun observeResults(mode: String): Flow<List<LevelResult>>

    /**
     * Saves [dto] as the resume point for its own [ResumeSnapshotDto.mode], replacing any
     * snapshot already stored there (D-03/D-04).
     */
    suspend fun saveSnapshot(
        dto: ResumeSnapshotDto,
        nowMs: Long = System.currentTimeMillis(),
    )

    /**
     * The snapshot [mode] can resume into, or `null` when there is none **or the stored
     * blob is unusable** — a corrupt or mismatched payload degrades to "no resume" rather
     * than throwing (Security §V5).
     */
    suspend fun loadSnapshot(mode: String): ResumeSnapshotDto?

    /** Drops [mode]'s snapshot — solved, restarted, or new game (D-05). */
    suspend fun clearSnapshot(mode: String)

    /** The modes currently holding a snapshot; drives the Home "Continue" tiles (D-01/D-11). */
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
    ) : ProgressRepository {
        override suspend fun recordResult(
            mode: String,
            levelId: String,
            result: ScoreResult,
            nowMs: Long,
        ): NewBests =
            database.withTransaction {
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
                newBests
            }

        override fun observeResults(mode: String): Flow<List<LevelResult>> =
            levelResultDao.observe(mode).map { rows -> rows.map { it.toLevelResult() } }

        override suspend fun saveSnapshot(
            dto: ResumeSnapshotDto,
            nowMs: Long,
        ) {
            resumeDao.upsert(
                ResumeSnapshotEntity(
                    mode = dto.mode,
                    jsonBlob = SnapshotMapper.encode(dto),
                    updatedAt = nowMs,
                ),
            )
        }

        override suspend fun loadSnapshot(mode: String): ResumeSnapshotDto? =
            resumeDao
                .get(mode)
                ?.let { SnapshotMapper.decode(it.jsonBlob) }
                // A payload claiming a different mode than the row it sits in is tampered
                // or corrupt state; refuse it rather than resuming the wrong game.
                ?.takeIf { it.mode == mode }

        override suspend fun clearSnapshot(mode: String) {
            resumeDao.clear(mode)
        }

        override fun observeResumeModes(): Flow<List<String>> = resumeDao.observeModes()

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
}
