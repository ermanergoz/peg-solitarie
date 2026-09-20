package com.app.pegsolitaire.platform.games.save

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.graphics.Bitmap
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

/**
 * The cover art attached to every cloud commit (CLOUD-03, D-09).
 *
 * Robolectric, because what is under test is a real vector inflation followed by a real
 * 512-square draw — the two steps that can actually fail.
 *
 * **[GraphicsMode.Mode.NATIVE] is load-bearing and must not be removed.** Robolectric's default
 * LEGACY mode stubs the whole drawing pipeline: every `Canvas` call is a no-op and
 * `Bitmap.compress` writes the raw ARGB buffer instead of encoding. Under LEGACY this file is
 * worse than no test — it was observed reporting an all-zero (fully transparent) bitmap and a
 * "PNG" of 1,049,700 bytes, i.e. exactly 512x512x4 plus a header, which is how both the
 * not-blank and the under-the-ceiling assertions were caught lying. NATIVE runs the real Skia
 * pipeline, so both now measure the bytes that would actually cross `BitmapTeleporter`.
 *
 * Every assertion here protects a commit that must not fail: an oversized cover, a blank cover
 * and a thrown inflation failure are three different ways to lose a push the player would never
 * see explained (T-13-26, T-13-27).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [COVER_ROBOLECTRIC_SDK])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CoverImageSourceTest {
    private fun sourceFor(context: Context = RuntimeEnvironment.getApplication()) = CoverImageSource(context)

    @Test
    fun theCoverIsASquareAtTheConfiguredRasterSize() =
        runTest {
            val cover = requireNotNull(sourceFor().coverOrNull())

            assertThat(cover.width).isEqualTo(CloudSaveConstants.COVER_PX)
            assertThat(cover.height).isEqualTo(CloudSaveConstants.COVER_PX)
        }

    @Test
    fun theCoverIsRasterizedOnceAndThenCached() =
        runTest {
            val source = sourceFor()

            val first = source.coverOrNull()
            val second = source.coverOrNull()

            assertThat(first).isNotNull()
            assertThat(second).isSameInstanceAs(first)
        }

    @Test
    fun theCoverActuallyDrewSomething() =
        runTest {
            val cover = requireNotNull(sourceFor().coverOrNull())
            val middle = CloudSaveConstants.COVER_PX / 2

            val centre = cover.getPixel(middle, middle)
            val corner = cover.getPixel(0, 0)

            assertWithMessage("the cover is a flat field, so nothing drew")
                .that(centre)
                .isNotEqualTo(corner)
        }

    @Test
    fun theCoverStaysWellUnderThePlayGamesCoverCeiling() =
        runTest {
            val cover = requireNotNull(sourceFor().coverOrNull())
            val sink = ByteArrayOutputStream()
            cover.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY_LOSSLESS, sink)

            assertWithMessage(
                "cover PNG measured ${sink.size()} bytes against a $MAX_COVER_BYTES byte ceiling " +
                    "— lower CloudSaveConstants.COVER_PX",
            ).that(sink.size())
                .isLessThan(MAX_COVER_BYTES)
        }

    /**
     * The accounting Play Games actually applies, measured on two devices in UAT test 12: the cover
     * is charged at its **raw** `ARGB_8888` size, not its PNG size. GMS logged `Snapshot cover image
     * is too large. Currently at 1048576 bytes; max is 819200. Image will be scaled` for the old
     * 512-square cover on every commit. No rasterization is needed to check this, so it is pure
     * arithmetic over the constant: raising [CloudSaveConstants.COVER_PX] past the cap fails here.
     */
    @Test
    fun theRawArgbCoverFitsThePlayGamesCoverCeiling() {
        val rawBytes = CloudSaveConstants.COVER_PX * CloudSaveConstants.COVER_PX * BYTES_PER_ARGB_8888_PIXEL

        assertWithMessage(
            "a ${CloudSaveConstants.COVER_PX}-square ARGB_8888 cover is $rawBytes raw bytes against a " +
                "$MAX_COVER_BYTES byte ceiling — lower CloudSaveConstants.COVER_PX",
        ).that(rawBytes)
            .isAtMost(MAX_COVER_BYTES)
    }

    @Test
    fun anUnresolvableDrawableYieldsNoCoverRatherThanAnException() =
        runTest {
            val broken = BrokenDrawableContext(RuntimeEnvironment.getApplication())

            assertThat(sourceFor(broken).coverOrNull()).isNull()
        }
}

/**
 * A [Context] whose drawable lookup fails the way a stripped or corrupt resource table would.
 *
 * The interception point is [getResources] and not `getDrawable`: `Context.getDrawable` is
 * `final` and simply forwards to `getResources().getDrawable(id, getTheme())`, which is the
 * lowest place a subclass can still reach. `ContextCompat.getDrawable` routes through exactly
 * that call, so this is the real inflation path failing, not a shortcut around it.
 */
private class BrokenDrawableContext(
    base: Context,
) : ContextWrapper(base) {
    private val broken: Resources =
        mockk {
            every { getDrawable(any<Int>(), any()) } throws
                Resources.NotFoundException("cover drawable deliberately unavailable")
        }

    override fun getResources(): Resources = broken
}

private const val COVER_ROBOLECTRIC_SDK = 34

/** Lossless, so the measurement below is the worst case rather than a flattering one. */
private const val PNG_QUALITY_LOSSLESS = 100

/** `Bitmap.Config.ARGB_8888` stores four bytes per pixel, which is the size GMS charges the cover at. */
private const val BYTES_PER_ARGB_8888_PIXEL = 4

/**
 * The Play Games cover ceiling, in bytes. Play Games guarantees at least 800 KB, and UAT test 12
 * measured exactly that (819,200) as the cap on both test devices (RESEARCH A1, falsified).
 *
 * Two budgets are held against it. The binding one is the **raw** `ARGB_8888` size, because that
 * is what GMS accounts. The PNG size is kept as a second, much looser check on the art itself: it
 * was 35,907 bytes at the former `COVER_PX = 512`, so a failure there reads as "the art changed".
 */
private const val MAX_COVER_BYTES = 800 * 1024
