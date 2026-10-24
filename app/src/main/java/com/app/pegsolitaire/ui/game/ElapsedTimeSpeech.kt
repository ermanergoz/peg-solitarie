package com.app.pegsolitaire.ui.game

/**
 * Which parts of the elapsed time the timer's description says, and their values (ux-spec
 * §4.3.2, D-05e). The words are the copy's — `duration_minutes`, `duration_seconds` and the
 * format strings that join them, resolved beside the clock in `GameStatusStrip` — and only the
 * arithmetic lives here, so a JVM table can read it without resources.
 *
 * - Minutes are said when there is at least one.
 * - Seconds are said when there is at least one, or when the whole time is zero: "0 seconds".
 * - Minutes are never rolled into hours, just as the clock never rolls them
 *   ([GameTimerFormat]): the pill shows "100:00" and TalkBack says "100 minutes".
 */
internal object ElapsedTimeSpeech {
    /** Whole seconds in one minute — the same split the clock makes. */
    private const val SECONDS_PER_MINUTE = 60

    /** The parts said for [totalSeconds] of elapsed time. */
    fun parts(totalSeconds: Int): SpokenDuration {
        val minutes = totalSeconds / SECONDS_PER_MINUTE
        val seconds = totalSeconds % SECONDS_PER_MINUTE
        return when {
            minutes == 0 -> SpokenDuration.Seconds(seconds = seconds)
            seconds == 0 -> SpokenDuration.Minutes(minutes = minutes)
            else -> SpokenDuration.MinutesAndSeconds(minutes = minutes, seconds = seconds)
        }
    }
}

/** An elapsed time as TalkBack says it: one part, or both. */
internal sealed interface SpokenDuration {
    /** Whole minutes and no second, e.g. "12 minutes". */
    data class Minutes(
        val minutes: Int,
    ) : SpokenDuration

    /** Under a minute, zero included, e.g. "45 seconds" or "0 seconds". */
    data class Seconds(
        val seconds: Int,
    ) : SpokenDuration

    /** Both parts, minutes first, e.g. "5 minutes, 12 seconds". */
    data class MinutesAndSeconds(
        val minutes: Int,
        val seconds: Int,
    ) : SpokenDuration
}
