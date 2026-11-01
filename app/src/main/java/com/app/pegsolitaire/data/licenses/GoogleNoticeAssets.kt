package com.app.pegsolitaire.data.licenses

/**
 * Where the build writes the Google notices in the APK's assets (tech design §13.1 LN-3; the
 * `google-notices` build-logic plugin) — the one place these paths are written in the app.
 */
internal object GoogleNoticeAssets {
    /** The generated index: one `key\tname\tstem\tsources` line per (name, text) pair. */
    const val INDEX_PATH = "licenses/google/index.tsv"

    /** How many lowercase hex digits a stem or a key has (the first 16 of a SHA-256). */
    const val HEX_ID_LENGTH = 16

    /** The text a stem names, written by the build as `licenses/google/<stem>.txt`. */
    fun textPath(stem: String): String = "$GOOGLE_DIR/$stem$TEXT_EXTENSION"

    /** Whether [value] is a stem or key: exactly [HEX_ID_LENGTH] lowercase hex digits. */
    fun isHexId(value: String): Boolean =
        value.length == HEX_ID_LENGTH && value.all { it in '0'..'9' || it in 'a'..'f' }

    private const val GOOGLE_DIR = "licenses/google"
    private const val TEXT_EXTENSION = ".txt"
}
