package com.app.pegsolitaire.platform.games.save

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.app.pegsolitaire.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Produces the square cover bitmap attached to every Play Games cloud commit (CLOUD-03, D-09),
 * once per process, or `null` if it cannot.
 *
 * The art is `R.drawable.cloud_save_cover` — a layer-list over the *existing* launcher vectors,
 * not a committed PNG. Rasterizing at runtime means there is exactly one copy of the art in the
 * tree: the cover cannot drift from the icon, no binary enters a diff nobody can review, and the
 * repo's authoring pipeline still needs no rasterizer. The cost is one inflate plus one
 * [CloudSaveConstants.COVER_PX]-square draw, once, off the main thread.
 *
 * ## No Activity is required
 *
 * `@ApplicationContext` is sufficient — resources and a `Canvas` are all this needs. Only
 * `PlayGames.getSnapshotsClient` demands an `Activity`, and that lives in the store
 * implementation. Holding an Activity here would tie a process-lifetime `@Singleton` cache to a
 * destroyed window for no gain.
 *
 * ## Why `Dispatchers.Default` (RESEARCH assumption A3)
 *
 * Vector inflation on a background dispatcher relies on `Resources`' internal synchronization,
 * which the platform does but does not *document*. `[ASSUMED]`. The exposure is bounded by
 * design: the failure mode of a bad inflate is a `null` cover, and a `null` cover is already the
 * designed degradation — the commit omits `setCoverImage` and still succeeds. Weighed against
 * blocking the main thread on a [CloudSaveConstants.COVER_PX]-square draw, the background hop is the right trade.
 *
 * ## Why the cover is 384 px (RESEARCH assumption A1, falsified by measurement)
 *
 * The 800 KB cover ceiling is charged against the bitmap's **raw** `ARGB_8888` size — width x
 * height x 4 — and not against the PNG bytes that cross the `BitmapTeleporter`. A1 assumed the
 * opposite and UAT test 12 falsified it: for the former 512-square cover GMS logged `Snapshot
 * cover image is too large. Currently at 1048576 bytes; max is 819200. Image will be scaled` on
 * every commit, on both devices. GMS scaled the image and the commit still succeeded, so no save
 * was ever lost to this, but every commit paid for a rescale and shipped a cover this class did
 * not draw. At [CloudSaveConstants.COVER_PX] = 384 the raw size is 589,824 bytes, under the cap.
 * `CoverImageSourceTest` pins that arithmetic, and separately keeps the PNG measurement as a
 * check on the art itself.
 *
 * ## This class never throws
 *
 * Every failure — a stripped resource, an inflate error, an allocation failure — collapses to
 * `null` (T-13-26, CLOUD-05). A cover is decoration on a save; letting it take the save down
 * with it would trade something the player would never miss for something they cannot afford to
 * lose.
 */
@Singleton
class CoverImageSource
    @Inject
    constructor(
        @param:ApplicationContext private val appContext: Context,
    ) {
        /**
         * The raster, held for the life of the process.
         *
         * `@Volatile` rather than a lock: the worst a race can do is rasterize twice and publish
         * one of two equivalent bitmaps, which costs a few milliseconds once. A lock would make
         * the second caller wait on the first's draw for no correctness the callers can observe.
         */
        @Volatile
        private var cached: Bitmap? = null

        /**
         * The cover bitmap, or `null` when it could not be produced.
         *
         * `null` is not an error to report: the caller omits `setCoverImage` and commits anyway.
         */
        suspend fun coverOrNull(): Bitmap? =
            cached ?: withContext(Dispatchers.Default) {
                runCatching {
                    ContextCompat
                        .getDrawable(appContext, R.drawable.cloud_save_cover)
                        ?.toBitmap(
                            width = CloudSaveConstants.COVER_PX,
                            height = CloudSaveConstants.COVER_PX,
                            config = Bitmap.Config.ARGB_8888,
                        )
                }.getOrNull()?.also { cached = it }
            }
    }
