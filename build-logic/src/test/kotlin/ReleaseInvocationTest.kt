import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Proves the configuration-time release gate (D-08 / D-09 / D-16 / D-17): the three fail-fast
 * gates in `app/build.gradle.kts` fire for every task name that PACKAGES a release, and for no
 * task name that merely analyses or compiles one.
 *
 * The second half is the point of this class. Under the previous `contains("elease")` test,
 * `./gradlew :app:lintRelease` and `./gradlew :app:detektRelease` demanded the two upload-keystore
 * passwords — a build gate that pressures an operator into exporting cryptographic secrets to run
 * a linter is a gate that teaches people to export them everywhere.
 *
 * **Why this test lives in `build-logic` and not in `:app`.** Same reason as [AdIdResolutionTest]
 * and [SigningResolutionTest]: AGP 9.1.0 creates a unit-test component only for the tested build
 * type, so `testReleaseUnitTest` does not exist and a release-gate assertion in `:app` would
 * silently never run.
 */
class ReleaseInvocationTest {
    @Test
    fun theTwoTasksThatPackageAReleaseArtifactAreReleaseInvocations() {
        assertThat(ReleaseInvocation.isReleasePackagingTask("assembleRelease")).isTrue()
        assertThat(ReleaseInvocation.isReleasePackagingTask("bundleRelease")).isTrue()
    }

    @Test
    fun theAgpPackageTasksThemselvesAreReleaseInvocations() {
        assertThat(ReleaseInvocation.isReleasePackagingTask("packageRelease")).isTrue()
        assertThat(ReleaseInvocation.isReleasePackagingTask("packageReleaseBundle")).isTrue()
    }

    @Test
    fun installAndPublishShapedNamesAreReleaseInvocations() {
        assertThat(ReleaseInvocation.isReleasePackagingTask("installRelease")).isTrue()
        assertThat(ReleaseInvocation.isReleasePackagingTask("publishReleaseBundle")).isTrue()
        assertThat(ReleaseInvocation.isReleasePackagingTask("publishReleasePublicationToMavenLocal"))
            .isTrue()
    }

    @Test
    fun aProjectQualifiedNameIsMatchedOnItsLastSegment() {
        assertThat(ReleaseInvocation.isReleasePackagingTask(":app:assembleRelease")).isTrue()
        assertThat(ReleaseInvocation.isReleasePackagingTask(":app:bundleRelease")).isTrue()
        assertThat(ReleaseInvocation.isReleasePackagingTask(":app:lintRelease")).isFalse()
    }

    @Test
    fun theThreeWholeProjectNamesAreReleaseInvocationsBecauseTheyBuildBothVariants() {
        ReleaseInvocation.WHOLE_PROJECT_TASK_NAMES.forEach { name ->
            assertThat(ReleaseInvocation.isReleasePackagingTask(name)).isTrue()
            assertThat(ReleaseInvocation.isReleasePackagingTask(":app:$name")).isTrue()
        }
    }

    @Test
    fun analysisTasksNamedAfterTheReleaseVariantPackageNothingAndAreNotReleaseInvocations() {
        listOf(
            "lintRelease",
            "lintVitalRelease",
            "detektRelease",
            "compileReleaseKotlin",
            "connectedReleaseAndroidTest",
            "mergeReleaseResources",
            "testReleaseUnitTest",
        ).forEach { name ->
            assertThat(ReleaseInvocation.isReleasePackagingTask(name)).isFalse()
        }
    }

    @Test
    fun aPackagingVerbOnTheDebugVariantIsNotAReleaseInvocation() {
        assertThat(ReleaseInvocation.isReleasePackagingTask("assembleDebug")).isFalse()
        assertThat(ReleaseInvocation.isReleasePackagingTask("packageDebug")).isFalse()
        assertThat(ReleaseInvocation.isReleasePackagingTask("installDebug")).isFalse()
    }

    @Test
    fun theEverydayVerificationTasksAreNotReleaseInvocations() {
        listOf("check", "test", "ktlintCheck", "detekt", "clean", "tasks").forEach { name ->
            assertThat(ReleaseInvocation.isReleasePackagingTask(name)).isFalse()
        }
    }

    @Test
    fun anIdeSyncRequestsNoTasksAtAllAndIsNotAReleaseInvocation() {
        assertThat(ReleaseInvocation.isReleaseInvocation(emptyList())).isFalse()
    }

    @Test
    fun oneReleasePackagingNameAmongOthersIsEnoughToMakeTheInvocationARelease() {
        assertThat(ReleaseInvocation.isReleaseInvocation(listOf("clean", "assembleRelease")))
            .isTrue()
        assertThat(ReleaseInvocation.isReleaseInvocation(listOf("clean", "lintRelease", "detekt")))
            .isFalse()
    }
}
