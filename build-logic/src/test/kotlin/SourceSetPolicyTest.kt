import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Proves the scope of the three literal gates (PLAT-06 / D-05, PLAT-06 / D-17, SOUND-06) and the
 * tripwire that keeps that scope honest (F-8 / F-9).
 *
 * Two halves, and the second is as deliberate as the first:
 *  - every SHIPPED source set is scanned — `src/main`, `src/release` and `src/debug`. A real ad
 *    unit id pasted into `src/release` reaches the store; one pasted into `src/debug` cannot reach
 *    the store but serves real ads to a developer's own build, which is AdMob invalid traffic.
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
    fun everyShippedSourceSetIsScannedInAllThreeLanguageDirectories() {
        val roots = SourceSetPolicy.shippedSourceRoots()

        assertThat(roots).containsExactly(
            "src/main/java",
            "src/main/kotlin",
            "src/main/res",
            "src/release/java",
            "src/release/kotlin",
            "src/release/res",
            "src/debug/java",
            "src/debug/kotlin",
            "src/debug/res",
        )
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
    fun theAgpCreatedTestSourceSetsAreKnownSoTheyDoNotFalselyTripTheWire() {
        val agpCreated = listOf("androidTest", "androidTestDebug", "screenshotTest", "testRelease")

        assertThat(SourceSetPolicy.unknownSourceSetNames(agpCreated)).isEmpty()
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

        assertThat(description).contains("src/main")
        assertThat(description).contains("src/release")
        assertThat(description).contains("src/debug")
        assertThat(description).doesNotContain("src/test")
    }
}
