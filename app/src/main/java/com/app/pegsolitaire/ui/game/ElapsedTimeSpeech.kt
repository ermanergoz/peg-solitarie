package com.app.pegsolitaire.ui.game

/**
 * What the timer's description says of the elapsed time (ux-spec §4.3.2, D-05e, with its
 * whole-minute fallback; tech-lead defect ruling §2, B-2): whole minutes, or "under a minute". The
 * words are the copy's — `duration_minutes` and `duration_under_a_minute`, resolved beside the
 * clock in `GameStatusStrip` — and only the arithmetic lives here, so a JVM table can read it
 * without resources.
 *
 * - Minutes are floored, never rounded up: 0 to 59 s are "under a minute", 60 to 119 s "1 minute".
 * - Minutes are never rolled into hours, just as the clock never rolls them ([GameTimerFormat]):
 *   the pill shows "100:00" and TalkBack says "100 minutes".
 *
 * Why no seconds: on the device TalkBack spoke the focused timer again every 2.5 to 3 s while a
 * description that carried the seconds changed every second (criterion 18g, B-2). In whole minutes
 * the description changes at most once a minute. The pill still shows the seconds.
 */
internal object ElapsedTimeSpeech {
    /** Whole seconds in one minute — the same split the clock makes. */
    private const val SECONDS_PER_MINUTE = 60

    /** What is said for [totalSeconds] of elapsed time. */
    fun parts(totalSeconds: Int): SpokenDuration {
        val minutes = totalSeconds / SECONDS_PER_MINUTE
        return if (minutes == 0) SpokenDuration.UnderAMinute else SpokenDuration.Minutes(minutes = minutes)
    }
}

/** An elapsed time as TalkBack says it: whole minutes, or under a minute. */
internal sealed interface SpokenDuration {
    /** One minute or more, floored, e.g. "5 minutes" at 05:12. */
    data class Minutes(
        val minutes: Int,
    ) : SpokenDuration

    /** Under a minute, zero included: "under a minute". */
    data object UnderAMinute : SpokenDuration
}
