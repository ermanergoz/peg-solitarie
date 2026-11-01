package com.app.pegsolitaire.data.licenses

import androidx.compose.runtime.Immutable

/**
 * One line of the generated Google notices index (tech design §13.1 LN-3, LN-4): a distinct
 * (name, text) pair as Google supplied it.
 *
 * `@Immutable` because it travels inside UI state (the notice text screen's header), and its
 * `List` would otherwise make that state unstable for Compose.
 *
 * @property key the route's handle on this pair: 16 lowercase hex digits, unique in the index.
 * @property name the component name exactly as Google supplied it; shown, never edited.
 * @property sources the artifactIds of the Google AARs that carry this pair, sorted.
 * @property stem the text's 16-hex-digit stem; read only through a `GoogleNoticeText`.
 */
@Immutable
data class GoogleNotice(
    val key: String,
    val name: String,
    val sources: List<String>,
    val stem: String,
)
