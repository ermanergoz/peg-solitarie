import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Generates one variant's Google open-source notices into an assets tree (tech design §13.1 LN-2):
 * `licenses/google/index.tsv` and one `licenses/google/<stem>.txt` per distinct text, merged from
 * the root `third_party_licenses.json`/`.txt` of every AAR on the variant's runtime classpath.
 * Registered per variant by the `google-notices` convention plugin, which wires [outputDir] into
 * the variant's assets.
 *
 * A thin shell over [GoogleNoticeOutput], which `GoogleNoticeOutputTest` proves: the task holds no
 * `Project`, no `Configuration` and no resolved-artifact object, only a file collection and a map
 * set from providers, so it is configuration-cache safe even though the cache is off today
 * (tech design §9.2.7).
 *
 * **Cacheable, honestly.** Sorted index, content-addressed file names, bytes copied rather than
 * decoded, the output cleared before each run, and no timestamp or absolute path in any output:
 * the same classpath restores the same files from the build cache.
 */
@CacheableTask
abstract class GenerateGoogleNoticesTask : DefaultTask() {
    /** The variant's runtime-classpath AAR files. Their names and contents are their identity. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val aars: ConfigurableFileCollection

    /** Each AAR file's name → its artifactId; part of the output, so an input. */
    @get:Input
    abstract val artifactIdsByFileName: MapProperty<String, String>

    /** The root of the generated assets tree; set by AGP through `addGeneratedSourceDirectory`. */
    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        try {
            GoogleNoticeOutput.generate(aars.files, artifactIdsByFileName.get(), outputDir.get().asFile)
        } catch (e: GoogleNoticeOutputException) {
            throw GradleException("Cannot generate the Google open-source notices: ${e.message}", e)
        }
    }

    companion object {
        /** Gradle's `artifactType` value of a raw Android library archive. */
        const val AAR_ARTIFACT_TYPE = "aar"

        /**
         * The artifactId that names [artifact] in the index's `<sources>` column. Only a module
         * from a repository has one; anything else that resolved to an `aar` artifact (a project
         * or a file dependency) is a build failure naming it, never a silent skip.
         */
        fun artifactIdOf(artifact: ResolvedArtifactResult): String {
            val component = artifact.id.componentIdentifier
            return (component as? ModuleComponentIdentifier)?.module
                ?: throw GradleException(
                    "Cannot generate the Google open-source notices: ${artifact.file.name} comes from " +
                        "${component.displayName}, which is not a repository module, so it has no artifactId.",
                )
        }
    }
}
