import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.JavaVersion
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

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
}

configure<KotlinAndroidProjectExtension> {
    jvmToolchain(17)
}
