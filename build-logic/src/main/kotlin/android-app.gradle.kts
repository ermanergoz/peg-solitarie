import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * Convention plugin for the Android application module (`:app`).
 *
 * Centralises the shared Android/Compose/Kotlin configuration (compileSdk,
 * JDK-17 toolchain, Compose build feature) so the module build file only carries
 * its own identity (namespace, applicationId, version). The KSP + Hilt plugins
 * are applied in the module's own build file (after this convention realises the
 * AGP `android{}` extension) — Hilt's Gradle plugin needs the Android extension
 * present at apply time, which a precompiled convention's `plugins {}` block does
 * not guarantee during accessor generation.
 */
plugins {
    // AGP 9 provides built-in Kotlin support, so `org.jetbrains.kotlin.android`
    // is NOT applied (AGP rejects it). The Compose compiler plugin is still
    // applied explicitly and attaches to AGP's built-in Kotlin compilation.
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

extensions.configure<ApplicationExtension> {
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        targetSdk = 36
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Robolectric-backed unit tests (Plan 05 GameFlowTest) need the merged Android
    // resources (strings/dimens/theme) on the JVM test classpath.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

configure<KotlinAndroidProjectExtension> {
    jvmToolchain(17)
}

// ---- Kotlin warnings are errors in shipped code (docs/tech-design.md §9.8) -----------------
// Every Kotlin compilation of :app fails on a warning — debug and release main alike — except
// the unit-test compilations, which are exempt by name, here and nowhere else. Their remaining
// warnings (the `createComposeRule` v2 migration and one kotlin-reflect call) are the design's
// held deviation for 3.0.0, and it ends before the first release/* branch after release/3.0.0
// is cut (§9.8 rule 2, §14.2); until then a diff that adds a unit-test warning in a file it
// touches is a review finding. The unit-test compilations also opt in to
// kotlinx.coroutines.ExperimentalCoroutinesApi, the coroutines test API; a main compilation
// never does, so an experimental coroutines API in shipped code still fails the build.
val unitTestCompilationSuffix = "UnitTestKotlin"
val coroutinesTestApiOptIn = "kotlinx.coroutines.ExperimentalCoroutinesApi"

tasks.withType<KotlinCompile>().configureEach {
    val unitTestCompilation = name.endsWith(unitTestCompilationSuffix)
    compilerOptions {
        allWarningsAsErrors.set(!unitTestCompilation)
        if (unitTestCompilation) {
            optIn.add(coroutinesTestApiOptIn)
        }
    }
}
