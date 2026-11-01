import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The file side of `GenerateGoogleNoticesTask`: real AAR-shaped zip files in, the
 * `licenses/google/` tree out (tech design §13.1 LN-2, LN-3). The merge rules themselves are
 * `GoogleNoticeMergeTest`'s; this proves which zip entries are read, where the output goes, that
 * nothing from an earlier run survives, and that every failure names the AAR file.
 */
class GoogleNoticeOutputTest {
    @get:Rule
    val temp = TemporaryFolder()

    @Test
    fun writesTheIndexAndOneFilePerStemUnderLicensesGoogle() {
        val aar = aarFile("ads-mobile-sdk-1.4.0.aar", rootEntries = notices(ALPHA to TEXT_ALPHA))
        val output = temp.newFolder("assets")

        GoogleNoticeOutput.generate(listOf(aar), mapOf(aar.name to ADS), output)

        val google = File(output, GOOGLE_DIR)
        assertThat(google.list()?.toList()).containsExactly(INDEX_FILE, "$STEM_ALPHA.txt")
        assertThat(File(google, INDEX_FILE).readText()).isEqualTo("$KEY_ALPHA\t$ALPHA\t$STEM_ALPHA\t$ADS\n")
        assertThat(File(google, "$STEM_ALPHA.txt").readText()).isEqualTo(TEXT_ALPHA)
    }

    @Test
    fun nothingAnEarlierRunWroteSurvives() {
        val output = temp.newFolder("assets")
        val stale = File(output, "$GOOGLE_DIR/0000000000000000.txt").apply { parentFile.mkdirs() }
        stale.writeText("a text an older SDK carried")
        val strayRootFile = File(output, "left-over.txt").apply { writeText("stray") }
        val aar = aarFile("ads-mobile-sdk-1.4.0.aar", rootEntries = notices(ALPHA to TEXT_ALPHA))

        GoogleNoticeOutput.generate(listOf(aar), mapOf(aar.name to ADS), output)

        assertThat(stale.exists()).isFalse()
        assertThat(strayRootFile.exists()).isFalse()
        assertThat(File(output, "$GOOGLE_DIR/$INDEX_FILE").exists()).isTrue()
    }

    @Test
    fun onlyTheRootNoticeEntriesOfAnAarAreRead() {
        val nested = aarFile("nested-1.0.aar", rootEntries = emptyMap(), nestedEntries = notices(ALPHA to TEXT_ALPHA))
        val root = aarFile("ads-mobile-sdk-1.4.0.aar", rootEntries = notices(BETA to TEXT_BETA))
        val output = temp.newFolder("assets")

        GoogleNoticeOutput.generate(listOf(nested, root), mapOf(nested.name to "nested", root.name to ADS), output)

        val index = File(output, "$GOOGLE_DIR/$INDEX_FILE").readText()
        assertThat(index).doesNotContain(ALPHA)
        assertThat(index).contains("\t$BETA\t")
    }

    @Test
    fun noAarWithNoticesStillWritesAnEmptyIndex() {
        val plain = aarFile("androidx-core-1.18.0.aar", rootEntries = emptyMap())
        val output = temp.newFolder("assets")

        GoogleNoticeOutput.generate(listOf(plain), mapOf(plain.name to "core"), output)

        assertThat(File(output, "$GOOGLE_DIR/$INDEX_FILE").readBytes()).isEmpty()
    }

    @Test
    fun anAarWithNoArtifactIdFailsNamingTheFile() {
        val aar = aarFile("mystery-2.0.aar", rootEntries = notices(ALPHA to TEXT_ALPHA))

        val failure =
            assertThrows(GoogleNoticeOutputException::class.java) {
                GoogleNoticeOutput.generate(listOf(aar), emptyMap(), temp.newFolder("assets"))
            }

        assertThat(failure).hasMessageThat().contains("mystery-2.0.aar")
    }

    @Test
    fun aMergeFailureNamesTheAarFile() {
        val broken =
            aarFile(
                "user-messaging-platform-4.0.0.aar",
                rootEntries = mapOf(JSON_ENTRY to """{"Alpha": {"start": 0, "length": 999}}""", TXT_ENTRY to "short"),
            )

        val failure =
            assertThrows(GoogleNoticeOutputException::class.java) {
                GoogleNoticeOutput.generate(listOf(broken), mapOf(broken.name to UMP), temp.newFolder("assets"))
            }

        assertThat(failure).hasMessageThat().contains("user-messaging-platform-4.0.0.aar")
    }

    /** A zip at [name] holding [rootEntries] at its root and [nestedEntries] under `res/raw/`. */
    private fun aarFile(
        name: String,
        rootEntries: Map<String, String>,
        nestedEntries: Map<String, String> = emptyMap(),
    ): File {
        val file = File(temp.root, name)
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("classes.jar"))
            zip.closeEntry()
            (rootEntries + nestedEntries.mapKeys { (entry, _) -> "res/raw/$entry" }).forEach { (entry, content) ->
                zip.putNextEntry(ZipEntry(entry))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return file
    }

    /** The two notice entries for one component in Google's layout. */
    private fun notices(entry: Pair<String, String>): Map<String, String> {
        val (name, text) = entry
        val header = "\n\n$name:\n\n"
        val start = header.toByteArray(Charsets.UTF_8).size
        val length = text.toByteArray(Charsets.UTF_8).size
        return mapOf(
            JSON_ENTRY to """{"$name": {"start": $start, "length": $length}}""",
            TXT_ENTRY to header + text,
        )
    }

    private companion object {
        const val GOOGLE_DIR = "licenses/google"
        const val INDEX_FILE = "index.tsv"
        const val JSON_ENTRY = "third_party_licenses.json"
        const val TXT_ENTRY = "third_party_licenses.txt"

        const val ADS = "ads-mobile-sdk"
        const val UMP = "user-messaging-platform"
        const val ALPHA = "Alpha"
        const val BETA = "Beta"
        const val TEXT_ALPHA = "Alpha licence text.\n"
        const val TEXT_BETA = "Beta licence text.\n"

        /** `printf 'Alpha licence text.\n' | shasum -a 256 | cut -c1-16` */
        const val STEM_ALPHA = "677b56244522f04a"

        /** `printf 'Alpha\0677b56244522f04a' | shasum -a 256 | cut -c1-16` */
        const val KEY_ALPHA = "a4625c230ea75f8a"
    }
}
