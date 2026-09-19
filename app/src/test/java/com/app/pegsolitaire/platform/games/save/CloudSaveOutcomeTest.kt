package com.app.pegsolitaire.platform.games.save

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Behaviour spec for [CloudSaveOutcome] — the one thing the cloud seam reports about a push
 * beyond "it did not land".
 *
 * Two properties are pinned here because both are silent when broken. The stage **names** are
 * what gets persisted and what QA reads back off a device, so renaming one changes the meaning
 * of a diagnostic already written down in a report. And the **shape** is the privacy argument:
 * an outcome that can only hold an enum and an integer cannot carry an SDK message, a snapshot
 * id, a description or a payload, no matter what a future implementation tries to put in it
 * (T-11-04 / T-13-40).
 */
class CloudSaveOutcomeTest {
    @Test
    fun onlyTheCommittedStageCountsAsCommitted() {
        val committed = CloudSaveStage.entries.filter { CloudSaveOutcome(it).committed }

        assertThat(committed).containsExactly(CloudSaveStage.COMMITTED)
    }

    @Test
    fun theStatusCodeIsAbsentUnlessOneIsGiven() {
        assertThat(CloudSaveOutcome(CloudSaveStage.OPEN_FAILED).statusCode).isNull()
    }

    @Test
    fun theStageNamesArePinnedBecauseTheyAreWhatIsPersisted() {
        assertThat(CloudSaveStage.entries.map { it.name })
            .containsExactly(
                "COMMITTED",
                "NO_ACTIVITY",
                "OPEN_FAILED",
                "CONFLICT_UNRESOLVED",
                "OVER_CAP",
                "COMMIT_FAILED",
                "FAILED_OTHER",
            ).inOrder()
    }

    @Test
    fun anOutcomeCanHoldAStageAndAnIntegerAndNothingElse() {
        val fieldTypes =
            CloudSaveOutcome::class.java.declaredFields
                .filterNot { it.isSynthetic }
                .map { it.type }

        // No String-typed slot exists, so no SDK message, id, description or payload can ride
        // along on an outcome — the guarantee is the type, not the implementation's restraint.
        assertThat(fieldTypes).containsExactly(CloudSaveStage::class.java, Int::class.javaObjectType)
    }
}
