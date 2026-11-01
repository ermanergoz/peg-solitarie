package com.app.pegsolitaire.data.licenses

/**
 * The generated Google notices index (tech design §13.1 LN-4). Main-safe: an implementation reads
 * and parses off the caller's thread.
 */
interface GoogleNoticeSource {
    /** Every notice in index order; `null` when the index is missing, unreadable or malformed. */
    suspend fun index(): List<GoogleNotice>?
}
