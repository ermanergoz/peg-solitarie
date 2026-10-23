package com.app.pegsolitaire.ui.onboarding

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

/**
 * The tutorial's half of the wrong tap as a property of the build, not of a reviewer's memory
 * (D-01j(2); WT-16's grep, `docs/ux/invalid-tap-feedback.md` §9; the tech-lead's review greps,
 * 2026-10-18 report item 4). Runner-free.
 *
 * No source under `ui/onboarding/` may name a wrong-tap correction channel — the reject haptic,
 * the gameplay sentences, the board's correction flag, its live region — nor read any setting
 * but the read-only Haptics seam. The tutorial reaches the jump haptic only through the shared
 * `JumpHapticEffect`, so it names no haptic API at all. Comments count too: a KDoc that names a
 * channel is where the next edit starts. `app/build.gradle.kts` declares this directory an input
 * of the unit-test tasks, so a comment-only edit re-runs the guard.
 */
class TutorialSourceGuardTest {
    @Test
    fun theTutorialSourcesNameNoWrongTapChannelAndNoSettingButTheHapticsSeam() {
        val sources = onboardingSources()
        assertThat(sources).isNotEmpty()

        val offences =
            sources.flatMap { file ->
                val text = file.readText()
                FORBIDDEN.filter { it in text }.map { "${file.name} names $it" }
            }

        assertThat(offences).isEmpty()
    }

    private fun onboardingSources(): List<File> {
        val root =
            generateSequence(File(".").absoluteFile) { it.parentFile }
                .firstOrNull { File(it, ROOT_MARKER).isFile }
                ?: error("No ancestor of ${File(".").absolutePath} contains $ROOT_MARKER")
        return File(root, ONBOARDING_SOURCES)
            .walkTopDown()
            .filter { it.isFile && it.extension == KOTLIN }
            .sortedBy { it.name }
            .toList()
    }

    private companion object {
        const val ROOT_MARKER = "settings.gradle.kts"
        const val ONBOARDING_SOURCES = "app/src/main/java/com/app/pegsolitaire/ui/onboarding"
        const val KOTLIN = "kt"

        val FORBIDDEN =
            listOf(
                // The wrong tap's correction channels, all private to the gameplay screen.
                "wrong_tap_",
                "HapticFeedbackType",
                "performHapticFeedback",
                "LocalHapticFeedback",
                "WrongTapHaptic",
                "wrongTapAnnouncement",
                "wrongTapCorrection",
                "liveRegion",
                // Settings the tutorial must not read: it reads Haptics through HapticsSetting only.
                "SettingsRepository",
                "hintsEnabled",
                "soundOn",
            )
    }
}
