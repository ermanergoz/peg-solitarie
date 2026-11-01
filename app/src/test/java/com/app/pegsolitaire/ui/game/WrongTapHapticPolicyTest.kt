package com.app.pegsolitaire.ui.game

import android.os.Build
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * When a wrong tap is felt (D-06b, `docs/ux/invalid-tap-feedback.md` §3.4; tech-lead Ruling 4,
 * test 13). Runner-free and table-driven.
 *
 * The API boundary is load-bearing, not belt and braces: below API 30 the pinned androidx.core
 * rewrites the platform `REJECT` to the heavy `LONG_PRESS`, which §3.4 forbids, so the gate must
 * hold in our own code. Robolectric here runs SDK 34 only, so this pure predicate is the JVM
 * evidence for every phone below 30.
 */
class WrongTapHapticPolicyTest {
    @Test
    fun aWrongTapIsFeltOnlyWithHapticsOnAndFromApi30() {
        val rows =
            listOf(
                Row(hapticsOn = true, sdkInt = 30, fires = true),
                Row(hapticsOn = true, sdkInt = 29, fires = false),
                Row(hapticsOn = false, sdkInt = 30, fires = false),
                Row(hapticsOn = false, sdkInt = 29, fires = false),
                Row(hapticsOn = true, sdkInt = TARGET_SDK, fires = true),
                Row(hapticsOn = false, sdkInt = TARGET_SDK, fires = false),
                Row(hapticsOn = true, sdkInt = MIN_SDK, fires = false),
            )

        rows.forEach { row ->
            assertWithMessage("$row").that(wrongTapHapticFires(row.hapticsOn, row.sdkInt)).isEqualTo(row.fires)
        }
    }

    @Test
    fun theBoundaryIsAndroidR() {
        assertThat(Build.VERSION_CODES.R).isEqualTo(30)
        assertThat(wrongTapHapticFires(hapticsOn = true, sdkInt = Build.VERSION_CODES.R)).isTrue()
        assertThat(wrongTapHapticFires(hapticsOn = true, sdkInt = Build.VERSION_CODES.R - 1)).isFalse()
    }

    private data class Row(
        val hapticsOn: Boolean,
        val sdkInt: Int,
        val fires: Boolean,
    )

    private companion object {
        /** The app's `minSdk` and `targetSdk` (build-logic `android-app.gradle.kts`). */
        const val MIN_SDK = 24
        const val TARGET_SDK = 36
    }
}
