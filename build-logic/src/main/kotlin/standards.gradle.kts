import dev.detekt.gradle.Detekt
import dev.detekt.gradle.extensions.DetektExtension
import org.gradle.api.GradleException

/**
 * Code-standards gate (QUAL-02 / QUAL-03), applied by BOTH `:domain` and `:app`.
 *
 * Wires six checks into the `check` lifecycle so the build FAILS on any
 * violation, from commit 0:
 *  - detekt (`dev.detekt` 2.0) with `MagicNumber` enabled — no magic numbers.
 *  - ktlint (`org.jlleitschuh.gradle.ktlint`) — format/lint.
 *  - a custom `dpSpLiteralCheck` — no raw numeric `.dp`/`.sp` literals in UI code
 *    (no built-in detekt/ktlint rule covers this; QUAL-03).
 *  - a custom `adIdLiteralCheck` — no ad-unit-ID literal anywhere in `src/main`
 *    (PLAT-06; ad IDs are `buildConfigField`/`resValue` only).
 *  - a custom `gamesIdLiteralCheck` — no Play Games leaderboard/achievement ID
 *    literal anywhere in `src/main` (PLAT-06 / D-17; the games half of the same rule).
 *  - a custom `audioFocusLiteralCheck` — no audio-focus request anywhere in `src/main`
 *    (SOUND-06; holding no focus is what stops the game ducking the user's music).
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

// ---- detekt must see the build-type source sets, not just src/main -----------
// The plain `detekt` task is the ONLY detekt task wired into `check` (below), and its
// default source is detekt's own `src/main/{java,kotlin}` + `src/test/{java,kotlin}`.
// Code in a build-type source set (`src/debug`, `src/release`, `src/testDebug`) is
// therefore invisible to the gate — an undeclared blanket suppression over every file
// in it, which is exactly what happened when the debug snapshot picker moved out of
// `src/main`. Verified with a paired probe: an identical `MagicNumber` violation in
// `src/main` failed `:app:detekt`, the same violation in `src/debug` did not.
//
// Why widen this one task instead of `check.dependsOn("detektDebug", "detektRelease")`:
//  - this convention plugin is applied to `:domain` as well, a plain Kotlin JVM module
//    where the variant tasks do not exist at all, so a name-based `dependsOn` would have
//    to become variant-aware — and this plugin deliberately carries no AGP types (see
//    the dp/sp gate below, same reasoning);
//  - `detektDebugSourceSet` reports `NO-SOURCE` under AGP 9, so the per-source-set tasks
//    are not a working substitute either;
//  - `detektDebug`/`detektRelease` each re-analyse `src/main`, so every main-source
//    finding would be reported two or three times, and they add type resolution (and a
//    compile classpath) to what is otherwise a parse-only gate;
//  - `detektRelease`'s NAME matches `app/build.gradle.kts`'s `contains("elease")` ad-ID
//    guard, so it cannot be invoked directly without real AdMob IDs.
// Widening keeps one task, one config, one report, and one name in `check`, and matches
// every other gate in this file: a list of relative source roots filtered to those that
// exist. `src/main` and `src/test` are already covered by detekt's defaults; a new
// build-type or flavour source set must be added to this list.
val variantSourceRoots =
    listOf("debug", "release", "testDebug")
        .flatMap { listOf("src/$it/java", "src/$it/kotlin") }
        .map { layout.projectDirectory.dir(it).asFile }
        .filter { it.exists() }

tasks.named<Detekt>("detekt").configure { source(variantSourceRoots) }

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

// The shared leading fragment of a Play Games leaderboard/achievement ID — they are
// base64url payloads of the form `CgkI…EAIQAQ` and every one observed begins `CgkI`.
//
// This is a HEURISTIC on an ID shape, not a proof: a hand-crafted ID that does not start
// `CgkI` would slip past, and an unrelated base64 blob that does start `CgkI` would trip
// it. It earns its place anyway. PLAT-06 forbids any ID literal under `src/main`, and
// `adIdMarker` (`ca-app-pub-`) matches no Play Games ID at all — so without this gate the
// games half of PLAT-06 is enforced by code review alone, which is exactly the kind of
// rule that decays. The IDs belong in `app/build.gradle.kts`'s `buildConfigField` loop and
// are read through `platform/games/GamesIds.kt`.
val gamesIdMarker = "CgkI"

// The audio-focus APIs SOUND-06 forbids. Requesting focus is what *initiates* ducking,
// so the abandon call is listed too: its presence can only mean a request exists.
val audioFocusMarkers = listOf("requestAudioFocus", "abandonAudioFocusRequest")

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

// ---- PLAT-06 / D-17: custom "no Play Games ID literal in source" gate --------
// The games sibling of `adIdLiteralCheck`, and it exists because that gate cannot cover
// this: it scans for `ca-app-pub-`, which no leaderboard or achievement ID contains.
// Same 27 values, same rule — they are per-build-type `buildConfigField`s resolved from
// an untracked `local.properties`, never source. Reuses `adIdSourceRoots`, so it scans
// `src/main` only: `src/test` legitimately spells key names (see `BuildConfigIdsTest`).
val gamesIdLiteralCheck =
    tasks.register("gamesIdLiteralCheck") {
        group = "verification"
        description =
            "Fails on Play Games leaderboard/achievement ID literals in src/main (use buildConfigField)."
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
                            if (line.contains(gamesIdMarker)) {
                                val where = file.relativeTo(moduleDir).invariantSeparatorsPath
                                offenders += "$where:${index + 1}: $trimmed"
                            }
                        }
                    }
            }
            if (offenders.isNotEmpty()) {
                throw GradleException(
                    "Play Games leaderboard/achievement ID literals are forbidden in src/main " +
                        "(PLAT-06 / D-17). IDs differ per build type — declare them in the " +
                        "buildConfigField loop in app/build.gradle.kts and read them via " +
                        "platform/games/GamesIds.kt:\n  " +
                        offenders.joinToString("\n  "),
                )
            }
        }
    }

// ---- SOUND-06: custom "never request audio focus" gate -----------------------
// SOUND-06's entire implementation is an omission: ducking is *initiated* by a focus
// request, so an app that holds no focus can neither duck nor be ducked. An omission
// with no gate rots the first time someone adds a focus request "to be polite". This
// makes "the player's podcast never dips" a fact CI re-proves on every commit rather
// than a one-off UAT observation. It reuses `adIdSourceRoots`, so it scans `src/main`
// only — `src/test` may still name the API in an assertion.
val audioFocusLiteralCheck =
    tasks.register("audioFocusLiteralCheck") {
        group = "verification"
        description =
            "Fails on audio-focus requests in src/main (SOUND-06: the app holds no focus)."
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
                            if (audioFocusMarkers.any { line.contains(it) }) {
                                val where = file.relativeTo(moduleDir).invariantSeparatorsPath
                                offenders += "$where:${index + 1}: $trimmed"
                            }
                        }
                    }
            }
            if (offenders.isNotEmpty()) {
                throw GradleException(
                    "Audio focus must never be requested (SOUND-06). Ducking is initiated by a " +
                        "focus request — an app that holds no focus cannot duck the player's " +
                        "music or podcast, so the omission IS the implementation. Play through " +
                        "SoundPool with USAGE_GAME attributes and leave AudioManager alone:\n  " +
                        offenders.joinToString("\n  "),
                )
            }
        }
    }

tasks.named("check").configure {
    dependsOn(
        "detekt",
        "ktlintCheck",
        dpSpLiteralCheck,
        adIdLiteralCheck,
        gamesIdLiteralCheck,
        audioFocusLiteralCheck,
    )
}
