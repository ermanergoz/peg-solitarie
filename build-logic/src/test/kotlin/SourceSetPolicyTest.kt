import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test

/**
 * Proves the scope of the three literal gates (PLAT-06 / D-05, PLAT-06 / D-17, SOUND-06) and the
 * tripwire that keeps that scope honest (F-8 / F-9).
 *
 * Two halves, and the second is as deliberate as the first:
 *  - every SHIPPED source set is scanned whole — every file under `src/main`, `src/release` and
 *    `src/debug`, the manifest, `assets/` and Java `resources/` included. A real ad unit id pasted
 *    into `src/release` reaches the store; one pasted into `src/debug` cannot reach the store but
 *    serves real ads to a developer's own build, which is AdMob invalid traffic.
 *  - every TEST source set is excluded — `src/test` legitimately asserts on Google's test-publisher
 *    prefix, spells Play Games key names and may name the audio-focus API in an assertion, and
 *    `src/testDebug` is simply a second spelling of the same exemption.
 *
 * The tripwire exists because "the KDoc tells the next reader to extend the list" is exactly the
 * protection that failed in F-1: a new build type or product flavour creates a source set nobody
 * adds to these lists, and a gate that silently scans nothing reports success.
 */
class SourceSetPolicyTest {
    @Test
    fun everyShippedSourceSetIsScannedAsAWholeDirectory() {
        val roots = SourceSetPolicy.shippedSourceRoots()

        assertThat(roots).containsExactly("src/main", "src/release", "src/debug")
    }

    @Test
    fun everyFileOfAShippedSourceSetIsUnderAScannedRoot() {
        val roots = SourceSetPolicy.shippedSourceRoots()
        val shippedFiles =
            listOf(
                "src/main/AndroidManifest.xml",
                "src/main/assets/licenses/x.txt",
                "src/main/resources/golden/book/x.txt",
                "src/debug/AndroidManifest.xml",
            )

        shippedFiles.forEach { file ->
            assertWithMessage("%s must lie under one of %s", file, roots)
                .that(roots.any { root -> file.startsWith("$root/") })
                .isTrue()
        }
    }

    @Test
    fun noTestSourceSetIsScannedByTheLiteralGates() {
        val roots = SourceSetPolicy.shippedSourceRoots()

        assertThat(roots.none { it.startsWith("src/test") }).isTrue()
        assertThat(roots.none { it.startsWith("src/androidTest") }).isTrue()
    }

    @Test
    fun theDetektWideningCoversTheBuildTypeSourceSetsThatItsOwnDefaultsMiss() {
        val roots = SourceSetPolicy.variantSourceRoots()

        assertThat(roots).containsExactly(
            "src/debug/java",
            "src/debug/kotlin",
            "src/release/java",
            "src/release/kotlin",
            "src/testDebug/java",
            "src/testDebug/kotlin",
        )
    }

    @Test
    fun theSourceSetsThisRepositoryActuallyHasAreAllKnown() {
        val appSourceSets = listOf("debug", "main", "release", "test", "testDebug")
        val domainSourceSets = listOf("main", "test")

        assertThat(SourceSetPolicy.unknownSourceSetNames(appSourceSets)).isEmpty()
        assertThat(SourceSetPolicy.unknownSourceSetNames(domainSourceSets)).isEmpty()
    }

    @Test
    fun aNewBuildTypeIsUnknownAndTripsTheWire() {
        assertThat(SourceSetPolicy.unknownSourceSetNames(listOf("main", "staging")))
            .containsExactly("staging")
    }

    @Test
    fun aProductFlavourSourceSetIsUnknownAndTripsTheWire() {
        assertThat(SourceSetPolicy.unknownSourceSetNames(listOf("main", "free", "freeDebug")))
            .containsExactly("free", "freeDebug")
    }

    @Test
    fun aTestDimensionNobodyHasClassifiedTripsTheWire() {
        val unclassified =
            listOf(
                "androidTest",
                "androidTestDebug",
                "androidTestRelease",
                "screenshotTest",
                "screenshotTestDebug",
                "testRelease",
            )

        assertThat(SourceSetPolicy.unknownSourceSetNames(listOf("main", "test") + unclassified))
            .containsExactlyElementsIn(unclassified)
            .inOrder()
    }

    @Test
    fun theTripwireMessageNamesTheOffenderAndBothListsThatMustBeExtended() {
        val message = SourceSetPolicy.unknownSourceSetMessage(":app", listOf("staging"))

        assertThat(message).contains("staging")
        assertThat(message).contains(":app")
        assertThat(message).contains("shippedSourceRoots")
        assertThat(message).contains("variantSourceRoots")
    }

    @Test
    fun theGateFailureMessagesCanNameTheRootsTheyActuallyScanned() {
        val description = SourceSetPolicy.SHIPPED_SOURCE_ROOTS_DESCRIPTION

        assertThat(description).contains("every file under")
        assertThat(description).contains("src/main")
        assertThat(description).contains("src/release")
        assertThat(description).contains("src/debug")
        assertThat(description).doesNotContain("src/test")
        assertThat(description).doesNotContain("java")
    }
}
