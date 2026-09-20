package com.app.pegsolitaire.platform.games.save

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Behaviour spec for [CloudPullProbe] and [CloudPull] — what the cloud seam reports about a read.
 *
 * The property pinned here is the privacy argument, exactly as `CloudSaveOutcomeTest` pins it for
 * a push: a probe that can only hold a boolean, an `Int` and a `Long` cannot carry a device name,
 * a snapshot id, a description, a player or a payload, whatever a future implementation tries to
 * put in it (T-11-04 / T-13-40).
 */
class CloudPullProbeTest {
    @Test
    fun aProbeCanHoldABooleanAnIntegerAndATimestampAndNothingElse() {
        val fieldTypes =
            CloudPullProbe::class.java.declaredFields
                // Excluded by NAME, not by "is static" — see CloudSaveOutcomeTest for why.
                .filterNot { it.isSynthetic || it.name == COMPOSE_STABILITY_FIELD }
                .map { it.type }

        assertThat(fieldTypes)
            .containsExactly(
                Boolean::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Long::class.javaPrimitiveType,
            )
    }

    @Test
    fun nothingReadIsNoBytesAndNoProbe() {
        assertThat(CloudPull.NOTHING.bytes).isNull()
        assertThat(CloudPull.NOTHING.probe).isNull()
    }
}

/** The field `@StabilityInferred` adds to every class the Compose compiler sees. */
private const val COMPOSE_STABILITY_FIELD = "\$stable"
