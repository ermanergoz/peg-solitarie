package com.app.pegsolitaire.ui.licenses

import com.app.pegsolitaire.data.licenses.GoogleNotice

/**
 * The ordinal of every notice whose name occurs more than once in this index, keyed by the
 * notice's key; a name that occurs once has no entry (brief A1.5).
 *
 * Numbered in index order, and grouped by the **exact** name: LN-3's case-insensitive sort is an
 * ordering, while "the same name" for the player is the same string. Computed once per index load
 * by both notices ViewModels, never in composition.
 */
fun List<GoogleNotice>.noticeOrdinals(): Map<String, NoticeOrdinal> =
    groupBy { it.name }
        .values
        .filter { it.size > 1 }
        .flatMap { sameName ->
            sameName.mapIndexed { position, notice -> notice.key to NoticeOrdinal(position + 1, sameName.size) }
        }.toMap()
