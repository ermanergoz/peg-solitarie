package com.app.pegsolitaire.data.licenses

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.FileNotFoundException
import java.security.MessageDigest

/**
 * LS-AC12 (b): the Google notices the build generates (tech design §13.1 LN-2, LN-3) are in the
 * app's merged debug assets, read the way the app reads them — through the real `AssetManager`.
 *
 * Robolectric serves the variant's merged assets (`isIncludeAndroidResources`), so this sees
 * exactly what `mergeDebugAssets` packaged; where AGP keeps the generated directory is AGP's
 * business, not this test's. The debug index stands for the release one because
 * `verifyGoogleNoticesParity`, in `:app:check`, fails the build when the two differ (brief A1.3).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [ROBOLECTRIC_SDK])
class GoogleNoticeIndexAssetTest {
    private val assets get() = RuntimeEnvironment.getApplication().assets

    @Test
    fun theIndexHasAtLeastOneLineOfFourNonEmptyFields() {
        val lines = indexLines()

        assertThat(lines).isNotEmpty()
        lines.forEach { fields ->
            assertThat(fields).hasSize(FIELD_COUNT)
            assertThat(fields.none { it.isEmpty() }).isTrue()
        }
    }

    @Test
    fun everyKeyIsUnique() {
        val keys = indexLines().map { it[KEY_FIELD] }

        assertThat(keys).containsNoDuplicates()
    }

    @Test
    fun everyStemNamesAnAssetWhoseSha256StartsWithIt() {
        val stems = indexLines().map { it[STEM_FIELD] }.toSet()

        stems.forEach { stem ->
            val text = readAsset("$GOOGLE_DIR/$stem$TEXT_SUFFIX")
            assertWithMessage("the text for stem $stem is in the merged assets").that(text).isNotNull()
            assertThat(sha256Hex(requireNotNull(text))).startsWith(stem)
        }
    }

    /**
     * Equality, not inclusion (brief A1.3, spec LS-AC12 (b)): a Google AAR that starts or stops
     * carrying notices is exactly the fact checklist 3.5 must hear about, so this list changes
     * only with the research-analyst's verdict and the THIRD_PARTY.md row.
     */
    @Test
    fun theSourcesAreExactlyTheTenMeasuredArtifacts() {
        val sources =
            indexLines()
                .flatMap { it[SOURCES_FIELD].split(SOURCE_SEPARATOR) }
                .toSet()

        assertThat(sources).containsExactlyElementsIn(MEASURED_ARTIFACTS)
    }

    /** The index split into lines and tab-separated fields; fails the test when it is absent. */
    private fun indexLines(): List<List<String>> {
        val raw = readAsset(INDEX_PATH)
        assertWithMessage("$INDEX_PATH is in the merged debug assets").that(raw).isNotNull()
        return String(requireNotNull(raw), Charsets.UTF_8)
            .removeSuffix(LINE_END)
            .split(LINE_END)
            .filter { it.isNotEmpty() }
            .map { it.split(FIELD_SEPARATOR) }
    }

    private fun readAsset(path: String): ByteArray? =
        try {
            assets.open(path).use { it.readBytes() }
        } catch (ignored: FileNotFoundException) {
            null
        }

    private fun sha256Hex(bytes: ByteArray): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(bytes)
            .joinToString(separator = "") { "%02x".format(it) }
}

/** Robolectric has no android-all jar for Android 16 (SDK 36) yet; emulate SDK 34. */
private const val ROBOLECTRIC_SDK = 34

private const val GOOGLE_DIR = "licenses/google"
private const val INDEX_PATH = "$GOOGLE_DIR/index.tsv"
private const val TEXT_SUFFIX = ".txt"
private const val LINE_END = "\n"
private const val FIELD_SEPARATOR = "\t"
private const val SOURCE_SEPARATOR = ","
private const val FIELD_COUNT = 4
private const val KEY_FIELD = 0
private const val STEM_FIELD = 2
private const val SOURCES_FIELD = 3

/** The ten Google AARs tech design §13.1 measured as carrying `third_party_licenses.json`. */
private val MEASURED_ARTIFACTS =
    listOf(
        "ads-mobile-sdk",
        "hsdp",
        "user-messaging-platform",
        "play-services-games-v2",
        "play-services-appset",
        "play-services-basement",
        "play-services-base",
        "play-services-tasks",
        "play-services-ads-identifier",
        "play-services-cronet",
    )
