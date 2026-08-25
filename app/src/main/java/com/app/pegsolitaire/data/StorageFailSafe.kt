package com.app.pegsolitaire.data

import kotlinx.coroutines.CancellationException

/**
 * Runs [block] and degrades any storage failure to [fallback] instead of letting it
 * crash the app (T-05-10 / T-05-13).
 *
 * "Storage failure" means the local persistence layer itself threw — a corrupt SQLite
 * file, a disk-I/O error, or a full disk surfacing as an `SQLiteException`/`IOException`
 * from Room or DataStore. For this offline single-user game the stored data is a
 * convenience, not an obligation: a failed read degrades to "nothing stored" and a
 * failed write is dropped, both strictly better outcomes than a crash loop on the
 * player's device. Callers pick the fallback that matches their missing-data shape
 * (`null` snapshot, empty list, no-op write), keeping failure indistinguishable from
 * absence — the same contract the blob guard already established (Security §V5).
 *
 * [CancellationException] is deliberately rethrown, never swallowed: cancellation is
 * control flow, not a storage fault, and eating it would break structured concurrency
 * for every caller.
 */
internal suspend fun <T> storageFailSafe(
    fallback: T,
    block: suspend () -> T,
): T =
    try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (ignored: Exception) {
        fallback
    }
