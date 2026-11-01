import java.io.File
import java.util.zip.ZipFile

/** The notices could not be generated; [message] names the AAR file and the reason. */
class GoogleNoticeOutputException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

/**
 * The file side of `GenerateGoogleNoticesTask` (tech design §13.1 LN-2, LN-3): read each AAR's
 * two **root** notice entries, merge them with [GoogleNoticeMerge], and write the
 * `licenses/google/` tree under an assets root.
 *
 * Gradle-free so `GoogleNoticeOutputTest` can prove it on real zip files, as `AdIdResolutionTest`
 * proves its sibling; the task only hands it its inputs and wraps a failure in a
 * `GradleException`.
 */
object GoogleNoticeOutput {
    /** The directory, under an assets root, that holds the index and the texts. */
    const val GOOGLE_DIR = "licenses/google"

    /** The index's file name; the app reads it at `licenses/google/index.tsv`. */
    const val INDEX_FILE = "index.tsv"

    /** Each text is written as `<stem>.txt`. */
    const val TEXT_EXTENSION = ".txt"

    /**
     * Clears [assetsRoot] — so a stem an older SDK carried cannot survive an SDK bump — then writes
     * `licenses/google/index.tsv` and one `licenses/google/<stem>.txt` per distinct text, byte for
     * byte. An index is written even when no AAR carries notices (empty), so the asset the app
     * opens always exists and an empty one is the app's error state, never a missing file.
     *
     * @param aars the variant's runtime-classpath AAR files.
     * @param artifactIdsByFileName each AAR file's name → its artifactId, the index's `<sources>`.
     * @throws GoogleNoticeOutputException naming the AAR file, when an AAR has no artifactId or its
     *   notices cannot be merged.
     */
    fun generate(
        aars: Collection<File>,
        artifactIdsByFileName: Map<String, String>,
        assetsRoot: File,
    ) {
        val sorted = aars.sortedBy { it.name }
        val fileNamesByArtifactId = HashMap<String, MutableList<String>>()
        val notices =
            sorted.map { aar ->
                val artifactId =
                    artifactIdsByFileName[aar.name]
                        ?: throw GoogleNoticeOutputException("${aar.name}: no artifactId was resolved for this AAR")
                fileNamesByArtifactId.getOrPut(artifactId) { mutableListOf() } += aar.name
                readRootNotices(aar, artifactId)
            }
        val merged =
            try {
                GoogleNoticeMerge.merge(notices)
            } catch (e: GoogleNoticeMergeException) {
                val files = fileNamesByArtifactId[e.artifactId].orEmpty().joinToString()
                throw GoogleNoticeOutputException("$files: ${e.message}", e)
            }
        write(merged, assetsRoot)
    }

    private fun readRootNotices(
        aar: File,
        artifactId: String,
    ): AarNotices =
        ZipFile(aar).use { zip ->
            fun rootEntry(name: String): ByteArray? = zip.getEntry(name)?.let { entry -> zip.getInputStream(entry).use { it.readBytes() } }
            AarNotices(artifactId, json = rootEntry(JSON_ENTRY), txt = rootEntry(TXT_ENTRY))
        }

    private fun write(
        merged: MergedNotices,
        assetsRoot: File,
    ) {
        assetsRoot.listFiles()?.forEach { it.deleteRecursively() }
        val google = File(assetsRoot, GOOGLE_DIR).apply { mkdirs() }
        File(google, INDEX_FILE).writeBytes(merged.indexTsv())
        merged.texts.forEach { (stem, bytes) -> File(google, stem + TEXT_EXTENSION).writeBytes(bytes) }
    }

    private const val JSON_ENTRY = "third_party_licenses.json"
    private const val TXT_ENTRY = "third_party_licenses.txt"
}
