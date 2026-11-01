package com.app.pegsolitaire.ui.licenses

import com.app.pegsolitaire.data.licenses.GoogleNotice
import com.app.pegsolitaire.data.licenses.GoogleNoticeSource

/**
 * A [GoogleNoticeSource] that serves a fixed [index] — `null` is the real source's missing or
 * malformed index — and counts how often it was read.
 */
internal class FakeGoogleNoticeSource(
    private val index: List<GoogleNotice>?,
) : GoogleNoticeSource {
    /** How many times [index] was called. */
    var reads = 0
        private set

    override suspend fun index(): List<GoogleNotice>? {
        reads++
        return index
    }
}

/** A well-formed fixture notice: sixteen-hex-digit [key] and [stem], as the generated index has. */
internal fun fixtureNotice(
    key: String,
    name: String,
    stem: String,
    vararg sources: String,
): GoogleNotice = GoogleNotice(key = key, name = name, sources = sources.toList(), stem = stem)
