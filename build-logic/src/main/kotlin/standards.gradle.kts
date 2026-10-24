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
 *  - a custom `adIdLiteralCheck` — no ad-unit-ID literal anywhere in shipped source
 *    (PLAT-06; ad IDs are `buildConfigField`/`resValue` only).
 *  - a custom `gamesIdLiteralCheck` — no Play Games leaderboard/achievement ID
 *    literal anywhere in shipped source (PLAT-06 / D-17; the games half of the same rule).
 *  - a custom `audioFocusLiteralCheck` — no audio-focus request anywhere in shipped source
 *    (SOUND-06; holding no focus is what stops the game ducking the user's music).
 *
 * "Shipped source" is `SourceSetPolicy.shippedSourceRoots()` — every file under `src/main`,
 * `src/release` and `src/debug`, test source sets deliberately excluded — and a
 * configuration-time tripwire below fails the build the day a source set appears that
 * neither list classifies.
 *
 * It also makes every `Test` task in both modules self-heal a truncated result store (F1,
 * below), so a clobbered or interrupted run can never wedge a later one.
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

// ---- F1: self-heal a truncated test-result store (docs/tech-design.md §9.6) ----------
// A store truncated by an interrupted or clobbered run makes Gradle's own
// SerializableTestResultStore.hasResults() throw EOFException before any test runs, and it
// does so on every later invocation until the directory is deleted by hand. AbstractTestTask.executeTests() reads
// the previous store — for previously-failed-first ordering — BEFORE it deletes the binary
// directory, so the task cannot recover from a file it is about to discard. `doFirst` runs
// before the task action, which is early enough; `--rerun-tasks` does not help, because the
// read happens inside the action whatever made the task run.
//
// Why here and not per module: this plugin is applied by both `:app` and `:domain`, so one
// block covers `:domain:test`, `:domain:depthSweep`, `:domain:fuzz` and
// `:app:testDebugUnitTest`, and every Test task registered later. Per-module copies are the
// copy-paste a convention plugin exists to prevent. Cost: these tasks forfeit
// previously-failed-first ordering, which is immaterial here. It captures only the task's own
// DirectoryProperty — no Project reference at execution time — so it stays
// configuration-cache safe.
tasks.withType<Test>().configureEach {
    val binaryResults = binaryResultsDirectory
    doFirst { binaryResults.get().asFile.deleteRecursively() }
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
//    compile classpath) to what is otherwise a parse-only gate.
// A fourth reason used to be listed here and is now FALSE, recorded so it is not
// rediscovered as a fact: `detektRelease`'s name once matched `app/build.gradle.kts`'s
// `contains("elease")` release gate, so it could not be invoked without real AdMob IDs.
// That gate is now a packaging-verb test (`ReleaseInvocation`), `detektRelease` is
// directly invocable, and reason 1 above was decisive on its own anyway.
// Widening keeps one task, one config, one report, and one name in `check`, and matches
// every other gate in this file: a list of relative source roots filtered to those that
// exist. `src/main` and `src/test` are already covered by detekt's defaults; a new
// build-type or flavour source set must be added to `SourceSetPolicy` — and the tripwire
// below is what makes that sentence enforceable instead of aspirational.
val variantSourceRoots =
    SourceSetPolicy
        .variantSourceRoots()
        .map { layout.projectDirectory.dir(it).asFile }
        .filter { it.exists() }

tasks.named<Detekt>("detekt").configure { source(variantSourceRoots) }

// ---- F-9: the unknown-source-set tripwire ------------------------------------
// Both root lists in this file are hand-maintained, and "the KDoc tells the next reader to
// extend the list" is exactly the protection that failed in F-1. So the day a `src/staging`
// or a product-flavour source set appears, configuration FAILS here — naming both lists and
// the decision each needs — instead of three literal gates silently scanning a directory
// that no longer holds all the shipped source. Configuration time is deliberate: at
// execution time the gates would have to run to report it, and `check` is precisely the
// command someone adding a build type does not run first.
val unknownSourceSets =
    SourceSetPolicy.unknownSourceSetNames(
        layout.projectDirectory
            .dir(SourceSetPolicy.SOURCE_DIR)
            .asFile
            .listFiles()
            .orEmpty()
            .filter { it.isDirectory }
            .map { it.name }
            .sorted(),
    )

if (unknownSourceSets.isNotEmpty()) {
    throw GradleException(SourceSetPolicy.unknownSourceSetMessage(path, unknownSourceSets))
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
// `ca-app-pub-` substring in shipped source or resources.
//
// It reads every file of every SHIPPED source set — `src/main`, `src/release`, `src/debug` —
// and no test source set. Every file, not a code-and-res subset: the manifest is where
// AdMob's setup puts the app ID, and `assets/` and Java `resources/` ship too, so the
// source-set list is the only list and the tripwire guards it (design §9.3, amended
// 2026-10-18). `src/release` is the half that reaches the store; `src/debug` is not cosmetic
// either, because D-05's rule is "Google's test units in debug, real untracked ones in
// release", so a real unit id there serves real ads to a developer's own build, which is
// AdMob invalid traffic. `src/test` and `src/testDebug` are excluded on purpose: they
// legitimately assert on the Google test-publisher prefix, spell Play Games key names and
// may name the audio-focus API. The full reasoning, and the tripwire that keeps this list
// honest, are on `SourceSetPolicy`.
val shippedSourceRoots =
    SourceSetPolicy.shippedSourceRoots().map { layout.projectDirectory.dir(it).asFile }

// What the three failure messages below say they scanned, derived from the list itself: a
// gate whose failure text misdescribes its own scope is how F-1's false claim happened.
val shippedScopeSentence =
    "Scanned: ${SourceSetPolicy.SHIPPED_SOURCE_ROOTS_DESCRIPTION}. Test source sets are " +
        "excluded on purpose (see SourceSetPolicy)."

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
            "Fails on ad-unit-ID literals in shipped source (use buildConfigField/resValue)."
        doLast {
            val offenders = mutableListOf<String>()
            shippedSourceRoots.filter { it.exists() }.forEach { root ->
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
                    "Ad-unit-ID literals are forbidden in shipped source (PLAT-06 / D-05). " +
                        "IDs differ per build type — declare them as buildConfigField/resValue " +
                        "in app/build.gradle.kts and read them via platform/ads/AdIds.kt. " +
                        "$shippedScopeSentence\n  " +
                        offenders.joinToString("\n  "),
                )
            }
        }
    }

// ---- PLAT-06 / D-17: custom "no Play Games ID literal in source" gate --------
// The games sibling of `adIdLiteralCheck`, and it exists because that gate cannot cover
// this: it scans for `ca-app-pub-`, which no leaderboard or achievement ID contains.
// Same 27 values, same rule — they are per-build-type `buildConfigField`s resolved from
// an untracked `local.properties`, never source. Reuses `shippedSourceRoots`, so it reads
// every file under `src/main`, `src/release` and `src/debug` — the manifest, `assets/` and
// Java `resources/` included, wherever an ID could be pasted — and no test source set:
// `src/test` legitimately spells key names (see `BuildConfigIdsTest`) and `src/testDebug`
// is the same exemption.
val gamesIdLiteralCheck =
    tasks.register("gamesIdLiteralCheck") {
        group = "verification"
        description =
            "Fails on Play Games leaderboard/achievement ID literals in shipped source (use buildConfigField)."
        doLast {
            val offenders = mutableListOf<String>()
            shippedSourceRoots.filter { it.exists() }.forEach { root ->
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
                    "Play Games leaderboard/achievement ID literals are forbidden in shipped " +
                        "source (PLAT-06 / D-17). IDs differ per build type — declare them in the " +
                        "buildConfigField loop in app/build.gradle.kts and read them via " +
                        "platform/games/GamesIds.kt. $shippedScopeSentence\n  " +
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
// than a one-off UAT observation. It reuses `shippedSourceRoots`, so it reads every file under
// `src/main`, `src/release` and `src/debug`, whatever its content type — `src/debug` matters
// most here, because SOUND-06's evidence
// is gathered on debug builds and a focus request added there would silently falsify every
// "the player's podcast never dips" observation. No test source set is scanned: `src/test`
// may still name the API in an assertion.
val audioFocusLiteralCheck =
    tasks.register("audioFocusLiteralCheck") {
        group = "verification"
        description =
            "Fails on audio-focus requests in shipped source (SOUND-06: the app holds no focus)."
        doLast {
            val offenders = mutableListOf<String>()
            shippedSourceRoots.filter { it.exists() }.forEach { root ->
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
                        "SoundPool with USAGE_GAME attributes and never request audio focus from " +
                        "AudioManager (reading the ringer mode and the output devices is fine). " +
                        "$shippedScopeSentence\n  " +
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
