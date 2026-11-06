package com.app.pegsolitaire.ui.results

import androidx.compose.runtime.Immutable
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** A confetti piece's shape (ux-spec §4.7, "Shape"): by index mod 3, thirty of each in a burst. */
internal enum class ConfettiShape { Strip, Square, Disc }

/** The dp tokens of §6.1, in pixels for the window the burst falls in. */
@Immutable
internal data class ConfettiSpan(
    val sizeMinPx: Float,
    val sizeMaxPx: Float,
    val swayMinPx: Float,
    val swayMaxPx: Float,
)

/** Where a piece is and how it looks at one instant. Tests read it; the draw reads the piece's own functions. */
@Immutable
internal data class ConfettiPose(
    val x: Float,
    val y: Float,
    val rotationDegrees: Float,
    val scaleX: Float,
    val alpha: Float,
)

/**
 * One piece of the burst, every value drawn once from the seed when the layer composes (D-RC-5);
 * nothing here reads a clock or a random source. Times are ms after the fall's start; the spawn's
 * x and y are unit draws placed on the window at draw time, so a resize keeps the burst.
 */
@Immutable
internal data class ConfettiPiece(
    val shape: ConfettiShape,
    val colourIndex: Int,
    val sizePx: Float,
    val spawnMs: Float,
    val fallMs: Float,
    val spawnXUnit: Float,
    val spawnYUnit: Float,
    val swayPx: Float,
    val swayPeriodMs: Float,
    val swayPhase: Float,
    val rotationStartDegrees: Float,
    val degreesPerMs: Float,
    val flipPeriodMs: Float,
    val flipPhase: Float,
) {
    /** Whether the piece is on its fall at [elapsedMs]: spawned and not yet past its end. */
    fun isFallingAt(elapsedMs: Float): Boolean {
        val local = elapsedMs - spawnMs
        return local >= 0f && local <= fallMs
    }

    /** The piece's centre x at [elapsedMs]: its spawn x across `[−s, W + s]` plus the sway. */
    fun xAt(
        elapsedMs: Float,
        widthPx: Float,
    ): Float {
        val local = elapsedMs - spawnMs
        val base = -sizePx + spawnXUnit * (widthPx + 2 * sizePx)
        return base + swayPx * sin(TWO_PI * local / swayPeriodMs + swayPhase)
    }

    /** The piece's centre y at [elapsedMs]: linear from its spawn above the top edge to `H + 2s`. */
    fun yAt(
        elapsedMs: Float,
        heightPx: Float,
    ): Float {
        val start = spawnY(heightPx)
        val progress = ((elapsedMs - spawnMs) / fallMs).coerceIn(0f, 1f)
        return start + (heightPx + 2 * sizePx - start) * progress
    }

    /** The tumble at [elapsedMs]. */
    fun rotationAt(elapsedMs: Float): Float = rotationStartDegrees + degreesPerMs * (elapsedMs - spawnMs)

    /** The flip, a cheap tumble in depth: the x scale's `cos`. */
    fun scaleXAt(elapsedMs: Float): Float = cos(TWO_PI * (elapsedMs - spawnMs) / flipPeriodMs + flipPhase)

    /** [CelebrationFx.CONFETTI_ALPHA] until the last fifth of the fall, then linear to 0. */
    fun alphaAt(elapsedMs: Float): Float {
        val progress = ((elapsedMs - spawnMs) / fallMs).coerceIn(0f, 1f)
        val fadeFrom = 1f - CelebrationFx.CONFETTI_FADE_FRACTION
        return if (progress <= fadeFrom) {
            CelebrationFx.CONFETTI_ALPHA
        } else {
            CelebrationFx.CONFETTI_ALPHA * (1f - progress) / CelebrationFx.CONFETTI_FADE_FRACTION
        }
    }

    /** The whole pose at [elapsedMs], or `null` before the spawn and after the end. */
    fun poseAt(
        elapsedMs: Float,
        widthPx: Float,
        heightPx: Float,
    ): ConfettiPose? =
        if (isFallingAt(elapsedMs)) {
            ConfettiPose(
                x = xAt(elapsedMs, widthPx),
                y = yAt(elapsedMs, heightPx),
                rotationDegrees = rotationAt(elapsedMs),
                scaleX = scaleXAt(elapsedMs),
                alpha = alphaAt(elapsedMs),
            )
        } else {
            null
        }

    private fun spawnY(heightPx: Float): Float =
        -sizePx - spawnYUnit * CelebrationFx.CONFETTI_SPAWN_BAND_FRACTION * heightPx

    private companion object {
        const val TWO_PI = (2 * PI).toFloat()
    }
}

/**
 * The seeded confetti burst (D-RC-5): `ConfettiBurst.create(seed, count, span)` is a pure function,
 * so the same result replays the same burst and a test pins one.
 */
internal object ConfettiBurst {
    private const val TWO_PI = (2 * PI).toFloat()
    private const val MS_PER_SECOND = 1_000f
    private val shapes = ConfettiShape.entries

    /** [count] pieces drawn from `Random(seed)`, once, into an immutable list. */
    fun create(
        seed: Int,
        count: Int,
        span: ConfettiSpan,
    ): List<ConfettiPiece> {
        val random = Random(seed)
        val maxDegreesPerMs = CelebrationFx.CONFETTI_TUMBLE_MAX_RPS * CelebrationFx.FULL_TURN_DEGREES / MS_PER_SECOND
        return List(count) { index ->
            ConfettiPiece(
                shape = shapes[index % shapes.size],
                colourIndex = random.nextInt(CelebrationFx.CONFETTI_COLOUR_COUNT),
                sizePx = random.between(span.sizeMinPx, span.sizeMaxPx),
                spawnMs = random.between(0f, CelebrationFx.CONFETTI_SPAWN_MS.toFloat()),
                fallMs =
                    random.between(
                        CelebrationFx.CONFETTI_FALL_MIN_MS.toFloat(),
                        CelebrationFx.CONFETTI_FALL_MAX_MS.toFloat(),
                    ),
                spawnXUnit = random.nextFloat(),
                spawnYUnit = random.nextFloat(),
                swayPx = random.between(span.swayMinPx, span.swayMaxPx),
                swayPeriodMs =
                    random.between(
                        CelebrationFx.CONFETTI_SWAY_PERIOD_MIN_MS.toFloat(),
                        CelebrationFx.CONFETTI_SWAY_PERIOD_MAX_MS.toFloat(),
                    ),
                swayPhase = random.between(0f, TWO_PI),
                rotationStartDegrees = random.between(0f, CelebrationFx.FULL_TURN_DEGREES),
                degreesPerMs = random.between(-maxDegreesPerMs, maxDegreesPerMs),
                flipPeriodMs =
                    random.between(
                        CelebrationFx.CONFETTI_FLIP_MIN_MS.toFloat(),
                        CelebrationFx.CONFETTI_FLIP_MAX_MS.toFloat(),
                    ),
                flipPhase = random.between(0f, TWO_PI),
            )
        }
    }

    private fun Random.between(
        from: Float,
        to: Float,
    ): Float = from + nextFloat() * (to - from)
}

/** The burst's seed, a pure function of the result (D-RC-5): `replayId.hashCode() × 31 + (challengeLevel ?: 0)`. */
internal fun confettiSeed(
    replayId: String,
    challengeLevel: Int?,
): Int = replayId.hashCode() * CelebrationFx.CONFETTI_SEED_MULTIPLIER + (challengeLevel ?: 0)
