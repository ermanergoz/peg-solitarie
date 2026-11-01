package com.app.pegsolitaire.platform.games.save

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Pins [CloudSaveConstants] against the rules the Saved Games SDK actually enforces, so a rename or
 * a "tidier" value cannot produce a call the SDK rejects on a device no developer is holding.
 *
 * The snapshot name is the case worth a test at all: a bad filename does not fail at build time, it
 * fails inside a `Task` on a real signed-in device, in a code path that by design swallows every
 * failure into `null`/`false` (CLOUD-05). Left unchecked, a typo'd rename would present exactly as
 * "cloud save silently stopped working", which is the hardest possible thing to notice.
 */
class CloudSaveConstantsTest {
    /**
     * Encodes the javadoc for `SnapshotsClient.open(String, boolean)`: the filename *"must be
     * between 1 and 100 non-URL-reserved characters (a-z, A-Z, 0-9, or the symbols `-`, `.`, `_`,
     * or `~`)."*
     */
    @Test
    fun snapshotNameMatchesTheSdksDocumentedFilenameCharset() {
        assertThat(CloudSaveConstants.SNAPSHOT_NAME).matches(SNAPSHOT_NAME_PATTERN)
    }

    @Test
    fun conflictRetriesAreBoundedAboveZeroSoResolutionIsAttemptedAndCannotSpin() {
        // Zero would mean a conflict is never resolved at all; the bound's whole job is to be a
        // finite number greater than zero (T-13-22).
        assertThat(CloudSaveConstants.MAX_CONFLICT_RETRIES).isGreaterThan(0)
    }

    @Test
    fun coverSizeIsPositive() {
        assertThat(CloudSaveConstants.COVER_PX).isGreaterThan(0)
    }

    @Test
    fun descriptionTruncationGuardIsPositive() {
        // A guard of zero would truncate every description to the empty string, making the save
        // illegible in the Play Games saves UI (CLOUD-03) rather than protecting the commit.
        assertThat(CloudSaveConstants.MAX_DESCRIPTION_CHARS).isGreaterThan(0)
    }

    private companion object {
        const val SNAPSHOT_NAME_PATTERN = "^[A-Za-z0-9\\-._~]{1,100}$"
    }
}
