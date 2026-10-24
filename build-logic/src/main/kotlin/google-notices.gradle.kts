import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE

/**
 * Generates the open-source notices that Google's AARs carry and AGP never packages, into every
 * variant's assets (tech design §13.1 LN-1 to LN-3). Applied by `:app` alone, as one line in its
 * `plugins {}`, so it shares no file with the other convention plugins.
 *
 * For **every** variant it registers `generate<Variant>GoogleNotices` over the variant's own
 * runtime classpath, read through an artifact view at Gradle's public `artifactType` attribute,
 * value `aar` — the raw archives, whose root holds `third_party_licenses.json` and `.txt`. A JAR or
 * the `:domain` project resolves to no `aar` artifact and is not in the view, which is right: only
 * an AAR carries these notices. The task's output joins the variant's assets through
 * `addGeneratedSourceDirectory`, so `merge<Variant>Assets` packages `licenses/google/` — the debug
 * merge for the `:app` JVM tests, the release merge for the AAB.
 *
 * Public API only (`Component.runtimeConfiguration`, `SourceDirectories.addGeneratedSourceDirectory`,
 * `ArtifactTypeDefinition`, `ModuleComponentIdentifier`): an AGP-internal class on this path is a
 * stop-and-report (LN-2). `withPlugin` keeps the plugin independent of the order `plugins {}`
 * lists it in.
 *
 * It also registers `verifyGoogleNoticesParity` on `:app`'s own `check` (brief A1.3; tech design
 * §9.1's "owning module's `check` dependency"): every variant's generated tree must be
 * byte-identical, which is what lets the JVM tests' debug assets stand for the release AAB.
 */
pluginManager.withPlugin("com.android.application") {
    val parity =
        tasks.register("verifyGoogleNoticesParity", VerifyGoogleNoticesParityTask::class.java) {
            group = "verification"
            description = "Fails when the variants' generated Google open-source notices are not byte-identical."
            marker.set(layout.buildDirectory.file("intermediates/google-notices/parity.txt"))
        }
    tasks.named("check").configure { dependsOn(parity) }

    extensions.getByType(ApplicationAndroidComponentsExtension::class.java).onVariants { variant ->
        val aarView =
            variant.runtimeConfiguration.incoming.artifactView {
                attributes.attribute(ARTIFACT_TYPE_ATTRIBUTE, GenerateGoogleNoticesTask.AAR_ARTIFACT_TYPE)
            }
        val generate =
            tasks.register(
                "generate${variant.name.replaceFirstChar { it.uppercaseChar() }}GoogleNotices",
                GenerateGoogleNoticesTask::class.java,
            ) {
                group = "build"
                description = "Generates the ${variant.name} variant's Google open-source notices into its assets."
                aars.from(aarView.files)
                artifactIdsByFileName.set(
                    aarView.artifacts.resolvedArtifacts.map { artifacts ->
                        artifacts.associate { it.file.name to GenerateGoogleNoticesTask.artifactIdOf(it) }
                    },
                )
            }
        variant.sources.assets?.addGeneratedSourceDirectory(generate, GenerateGoogleNoticesTask::outputDir)
        val tree = generate.flatMap { it.outputDir }
        parity.configure {
            treesByVariant.put(variant.name, tree)
            trees.from(tree)
        }
    }
}
