import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.GradleException

/**
 * Code-standards gate (QUAL-02 / QUAL-03), applied by BOTH `:domain` and `:app`.
 *
 * Wires three checks into the `check` lifecycle so the build FAILS on any
 * violation, from commit 0:
 *  - detekt (`dev.detekt` 2.0) with `MagicNumber` enabled — no magic numbers.
 *  - ktlint (`org.jlleitschuh.gradle.ktlint`) — format/lint.
 *  - a custom `dpSpLiteralCheck` — no raw numeric `.dp`/`.sp` literals in UI code
 *    (no built-in detekt/ktlint rule covers this; QUAL-03).
 */
plugins {
    id("dev.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

extensions.configure<DetektExtension> {
    buildUponDefaultConfig.set(true)
    ignoreFailures.set(false)
    parallel.set(true)
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
}

// ---- QUAL-03: custom "no raw dp/sp literal" gate -----------------------------
// Captured at configuration time (the convention plugin has no compose/AGP types,
// so the gate is a pure file scan over the module's ui/ Kotlin sources).
val uiSourceRoots = listOf("src/main/java", "src/main/kotlin")
    .map { layout.projectDirectory.dir(it).asFile }
val moduleDir = projectDir

// Matches a numeric literal immediately converted to dp/sp (e.g. 12.dp, 8.sp,
// 4f.dp). Legitimate sizing flows through dimensionResource(...) / a Dimens
// accessor, which never produces a `<number>.dp` token.
val dpSpRegex = Regex("""(?<![\w.])\d+(?:\.\d+)?[fF]?\.(?:dp|sp)\b""")

val dpSpLiteralCheck =
    tasks.register("dpSpLiteralCheck") {
        group = "verification"
        description =
            "Fails on raw numeric .dp/.sp literals in ui/ sources (use dimensionResource/Dimens)."
        doLast {
            val offenders = mutableListOf<String>()
            uiSourceRoots.filter { it.exists() }.forEach { root ->
                root.walkTopDown()
                    .filter {
                        it.isFile &&
                            it.extension == "kt" &&
                            it.invariantSeparatorsPath.contains("/ui/")
                    }
                    .forEach { file ->
                        file.readLines().forEachIndexed { index, line ->
                            val trimmed = line.trimStart()
                            if (trimmed.startsWith("//") || trimmed.startsWith("*")) {
                                return@forEachIndexed
                            }
                            if (dpSpRegex.containsMatchIn(line)) {
                                val where = file.relativeTo(moduleDir).invariantSeparatorsPath
                                offenders += "$where:${index + 1}: $trimmed"
                            }
                        }
                    }
            }
            if (offenders.isNotEmpty()) {
                throw GradleException(
                    "Raw dp/sp literals are forbidden in UI code (QUAL-03). " +
                        "Use dimensionResource(...) or a Dimens accessor:\n  " +
                        offenders.joinToString("\n  "),
                )
            }
        }
    }

tasks.named("check").configure {
    dependsOn("detekt", "ktlintCheck", dpSpLiteralCheck)
}
