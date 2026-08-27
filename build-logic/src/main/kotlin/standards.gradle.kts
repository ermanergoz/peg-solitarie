import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.GradleException

/**
 * Code-standards gate (QUAL-02 / QUAL-03), applied by BOTH `:domain` and `:app`.
 *
 * Wires four checks into the `check` lifecycle so the build FAILS on any
 * violation, from commit 0:
 *  - detekt (`dev.detekt` 2.0) with `MagicNumber` enabled — no magic numbers.
 *  - ktlint (`org.jlleitschuh.gradle.ktlint`) — format/lint.
 *  - a custom `dpSpLiteralCheck` — no raw numeric `.dp`/`.sp` literals in UI code
 *    (no built-in detekt/ktlint rule covers this; QUAL-03).
 *  - a custom `adIdLiteralCheck` — no ad-unit-ID literal anywhere in `src/main`
 *    (PLAT-06; ad IDs are `buildConfigField`/`resValue` only).
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

// ---- PLAT-06: custom "no ad-unit-ID literal in source" gate ------------------
// Ad IDs are per-build-type values (Google's test units in debug, real untracked ones
// in release), so they can only exist as `buildConfigField` / `resValue`. This gate is
// what makes that a rule rather than a convention: it fails the build on any
// `ca-app-pub-` substring in shipped source or resources. It scans `src/main` only —
// `src/test` legitimately asserts on the Google test-publisher prefix.
val adIdSourceRoots = listOf("src/main/java", "src/main/kotlin", "src/main/res")
    .map { layout.projectDirectory.dir(it).asFile }

// The AdMob ID publisher prefix, shared by app IDs (`…~…`) and unit IDs (`…/…`).
val adIdMarker = "ca-app-pub-"

// Comment prefixes across the scanned file types (Kotlin, KDoc continuation, XML,
// properties/shell). A comment explaining *why* IDs are not in source must not itself
// trip the gate.
val commentPrefixes = listOf("//", "*", "<!--", "#")

val adIdLiteralCheck =
    tasks.register("adIdLiteralCheck") {
        group = "verification"
        description =
            "Fails on ad-unit-ID literals in src/main (use buildConfigField/resValue)."
        doLast {
            val offenders = mutableListOf<String>()
            adIdSourceRoots.filter { it.exists() }.forEach { root ->
                root.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        file.readLines().forEachIndexed { index, line ->
                            val trimmed = line.trimStart()
                            if (commentPrefixes.any { trimmed.startsWith(it) }) {
                                return@forEachIndexed
                            }
                            if (line.contains(adIdMarker)) {
                                val where = file.relativeTo(moduleDir).invariantSeparatorsPath
                                offenders += "$where:${index + 1}: $trimmed"
                            }
                        }
                    }
            }
            if (offenders.isNotEmpty()) {
                throw GradleException(
                    "Ad-unit-ID literals are forbidden in src/main (PLAT-06 / D-05). " +
                        "IDs differ per build type — declare them as buildConfigField/resValue " +
                        "in app/build.gradle.kts and read them via platform/ads/AdIds.kt:\n  " +
                        offenders.joinToString("\n  "),
                )
            }
        }
    }

tasks.named("check").configure {
    dependsOn("detekt", "ktlintCheck", dpSpLiteralCheck, adIdLiteralCheck)
}
