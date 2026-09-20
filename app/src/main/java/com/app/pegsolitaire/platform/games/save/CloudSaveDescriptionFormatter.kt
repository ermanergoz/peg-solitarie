package com.app.pegsolitaire.platform.games.save

import android.content.Context
import com.app.pegsolitaire.R
import com.app.pegsolitaire.domain.achievement.ClassicBoard
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns the three numbers of a [CloudSaveDescription] into the one localized line the player
 * reads under this game's entry in the Play Games saved-games list (CLOUD-03, D-11).
 *
 * ## Why this lives in `platform/` and not in `data/`
 *
 * Resolving the wording requires a [Context], and `data/` — which owns the SQL aggregates the
 * three numbers come from — must never touch one. CLAUDE.md also requires every piece of copy to
 * come from `strings.xml`, so building the line anywhere a `Context` is unavailable would mean
 * building it out of Kotlin string literals, which is the rule this class exists to avoid
 * breaking. [CloudSaveDescription] therefore crosses the seam as three `Int`s and becomes a
 * `String` here, one layer from the SDK call that consumes it.
 *
 * ## The locale behaviour is intended (D-11)
 *
 * The line is written into the snapshot's metadata in whatever locale the process holds **at
 * commit time**, and the stored description is never re-rendered afterwards. So a player who
 * changes their device language sees the new language after their next solve pushes, not
 * immediately — and the previously-stored line stays in the old language until then. That is the
 * decided behaviour, not a defect: the alternative, storing a locale-independent token and
 * rendering it in the Play Games UI, is not something the Saved Games API offers.
 *
 * ## The length cap is a guard, not a feature (RESEARCH assumption A2)
 *
 * **No maximum length is documented anywhere** for `SnapshotMetadataChange.Builder.setDescription`
 * — not in the SDK javadoc, not in the saved-games guide. The cap at
 * [CloudSaveConstants.MAX_DESCRIPTION_CHARS] exists purely so that an unexpectedly long line — a
 * verbose translation, a player at a six-digit ladder level — can never draw a server-side
 * rejection that fails the **whole commit** and costs the push (T-13-24). It is applied
 * unconditionally rather than behind a length check, so there is no branch a future input can get
 * around. In practice the D-11 line is around 45 characters and the cap never bites.
 *
 * ## The returned string must never be logged
 *
 * It carries progress figures tied to a signed-in player (T-13-25 / T-11-04, ASVS V7). There is no
 * logging anywhere in this package and none may be added — not even at `DEBUG`, since a release
 * build's stripped log call is still a plaintext string built at runtime.
 */
@Singleton
class CloudSaveDescriptionFormatter
    @Inject
    constructor(
        @param:ApplicationContext private val appContext: Context,
    ) {
        /**
         * Renders [description] as the listing line, truncated to
         * [CloudSaveConstants.MAX_DESCRIPTION_CHARS].
         *
         * The denominator is [ClassicBoard]`.entries.size` and never a literal: adding a sixth
         * Classic board must move the line on its own, with no second place to remember.
         */
        fun format(description: CloudSaveDescription): String =
            appContext
                .getString(
                    R.string.cloud_save_description,
                    description.ladder,
                    description.totalStars,
                    description.classicCleared,
                    ClassicBoard.entries.size,
                ).take(CloudSaveConstants.MAX_DESCRIPTION_CHARS)
    }
