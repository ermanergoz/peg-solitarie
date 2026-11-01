import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Fails `:app:check` when the variants' generated Google notices are not byte-identical (brief
 * A1.3), naming the first differing file. The `:app` JVM tests can read only the debug merged
 * assets (AGP 9.1.0 creates no `testReleaseUnitTest`), so this is what makes "the debug index
 * stands for the release one" a check rather than an assumption. It generates the release
 * notices too, which resolves `releaseRuntimeClasspath` from the cache; that is not a release
 * invocation (no packaging verb), so no `PLAY_UPLOAD_*` variable or real ad id is needed.
 *
 * [trees] is what Gradle fingerprints and what carries the dependency on every
 * `generate<Variant>GoogleNotices`; [treesByVariant] only names the same directories for the
 * message. A marker file is the task's one output, so an unchanged pair of trees is UP-TO-DATE.
 */
abstract class VerifyGoogleNoticesParityTask : DefaultTask() {
    /** Every variant's generated notices tree, keyed by variant name; registered per variant. */
    @get:Internal
    abstract val treesByVariant: MapProperty<String, Directory>

    /** The same directories as a file collection: the task's fingerprinted input. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val trees: ConfigurableFileCollection

    /** Written on success; records which variants were compared. */
    @get:OutputFile
    abstract val marker: RegularFileProperty

    @TaskAction
    fun verify() {
        val byVariant = treesByVariant.get().mapValues { (_, directory) -> directory.asFile }
        GoogleNoticesParity.mismatch(byVariant)?.let { reason ->
            throw GradleException("The Google open-source notices differ between variants: $reason")
        }
        marker.get().asFile.writeText("identical: ${byVariant.keys.sorted().joinToString()}\n")
    }
}
