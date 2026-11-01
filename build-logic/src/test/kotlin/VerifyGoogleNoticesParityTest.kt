import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Brief A1.3: "the debug index the JVM tests read equals the release index the AAB carries" is a
 * check, not an assumption. `verifyGoogleNoticesParity`, in `:app:check`, compares every
 * variant's generated notices tree through [GoogleNoticesParity]; these fixture directories prove
 * the comparison passes only on byte-identical trees and that each failure names the file.
 */
class VerifyGoogleNoticesParityTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun equalTreesPass() {
        val debug = tree("debug", INDEX to "a\tb\tc\td\n", STEM_FILE to "text", NESTED_FILE to "nested")
        val release = tree("release", INDEX to "a\tb\tc\td\n", STEM_FILE to "text", NESTED_FILE to "nested")

        assertThat(GoogleNoticesParity.mismatch(mapOf(DEBUG to debug, RELEASE to release))).isNull()
    }

    @Test
    fun aDifferingByteFailsNamingTheFile() {
        val debug = tree("debug", INDEX to "same", STEM_FILE to "text one")
        val release = tree("release", INDEX to "same", STEM_FILE to "text two")

        val mismatch = GoogleNoticesParity.mismatch(mapOf(DEBUG to debug, RELEASE to release))

        assertThat(mismatch).contains(STEM_FILE)
        assertThat(mismatch).doesNotContain(INDEX)
    }

    @Test
    fun aFileMissingFromOneTreeFailsNamingTheFile() {
        val debug = tree("debug", INDEX to "same", STEM_FILE to "text")
        val release = tree("release", INDEX to "same")

        val mismatch = GoogleNoticesParity.mismatch(mapOf(DEBUG to debug, RELEASE to release))

        assertThat(mismatch).contains(STEM_FILE)
    }

    @Test
    fun anExtraFileInTheSecondTreeFailsNamingTheFile() {
        val debug = tree("debug", INDEX to "same")
        val release = tree("release", INDEX to "same", NESTED_FILE to "only in release")

        val mismatch = GoogleNoticesParity.mismatch(mapOf(DEBUG to debug, RELEASE to release))

        assertThat(mismatch).contains(NESTED_FILE)
    }

    @Test
    fun aSingleVariantFails() {
        val debug = tree("debug", INDEX to "same")

        val mismatch = GoogleNoticesParity.mismatch(mapOf(DEBUG to debug))

        assertThat(mismatch).isNotNull()
        assertThat(mismatch).contains(DEBUG)
    }

    @Test
    fun noVariantFails() {
        assertThat(GoogleNoticesParity.mismatch(emptyMap())).isNotNull()
    }

    /** A directory under the temp root holding [files] at their relative paths. */
    private fun tree(
        name: String,
        vararg files: Pair<String, String>,
    ): File {
        val root = temp.newFolder(name)
        files.forEach { (path, content) ->
            File(root, path).apply { parentFile.mkdirs() }.writeText(content)
        }
        return root
    }

    private companion object {
        const val DEBUG = "debug"
        const val RELEASE = "release"
        const val INDEX = "licenses/google/index.tsv"
        const val STEM_FILE = "licenses/google/0123456789abcdef.txt"
        const val NESTED_FILE = "licenses/google/deeper/extra.txt"
    }
}
