import java.io.File

/**
 * Whether every variant generated the same Google notices (brief A1.3): the `:app` JVM tests read
 * the **debug** merged assets, and this is what lets them stand for the **release** AAB.
 *
 * Gradle-free so `VerifyGoogleNoticesParityTest` proves it on fixture directories;
 * `VerifyGoogleNoticesParityTask` turns a mismatch into a build failure in `:app:check`.
 */
object GoogleNoticesParity {
    /**
     * `null` when every tree in [treesByVariant] holds the same relative file paths with the same
     * bytes; otherwise a reason naming the first difference (the first file, in sorted path order,
     * that is missing from a tree or differs in a byte). Fewer than two variants is a mismatch too:
     * parity that compared nothing would pass the day a variant stops being registered.
     */
    fun mismatch(treesByVariant: Map<String, File>): String? {
        if (treesByVariant.size < MINIMUM_VARIANTS) {
            return "the Google notices parity check needs at least two variants, " +
                "but only ${treesByVariant.keys.sorted()} registered one"
        }
        val variants = treesByVariant.toSortedMap()
        val (referenceVariant, referenceRoot) = variants.entries.first()
        val referenceFiles = relativeFiles(referenceRoot)
        variants.entries.drop(1).forEach { (variant, root) ->
            val files = relativeFiles(root)
            val missing = (referenceFiles - files.toSet()).firstOrNull()
            val extra = (files - referenceFiles.toSet()).firstOrNull()
            val firstAbsent = listOfNotNull(missing, extra).minOrNull()
            if (firstAbsent != null) {
                val holder = if (firstAbsent == missing) referenceVariant else variant
                return "$firstAbsent is in the $holder notices only ($referenceVariant vs $variant)"
            }
            val differing =
                referenceFiles.firstOrNull { path ->
                    !File(referenceRoot, path).readBytes().contentEquals(File(root, path).readBytes())
                }
            if (differing != null) return "$differing differs between the $referenceVariant and $variant notices"
        }
        return null
    }

    /** Every regular file under [root], as `/`-separated paths relative to it, sorted. */
    private fun relativeFiles(root: File): List<String> =
        root
            .walkTopDown()
            .filter { it.isFile }
            .map { it.relativeTo(root).invariantSeparatorsPath }
            .sorted()
            .toList()

    private const val MINIMUM_VARIANTS = 2
}
