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
 * blocking the main thread on a 512-square draw, the background hop is the right trade.
 *
 * ## Why 512 px is safe against the 800 KB ceiling (RESEARCH assumption A1)
 *
 * A 512x512 `ARGB_8888` bitmap is 1 MB **in memory**, which is not the number that matters:
 * `setCoverImage` transports the bitmap PNG-compressed through a `BitmapTeleporter`, and the
 * 800 KB ceiling applies to those transported bytes. This art — two flat gradients plus 33
 * circles — compresses far below it: `CoverImageSourceTest` measured **35,907 bytes**, 4.4% of
 * the ceiling. `[ASSUMED — the accounting basis is undocumented]`. If UAT test 12's runtime
 * `getMaxCoverImageSize()` measurement ever comes close, lower [CloudSaveConstants.COVER_PX];
 * it is one constant and 512 was chosen only because it is the art's native design canvas, so
 * dropping to 384 or 256 costs nothing but sharpness.
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
