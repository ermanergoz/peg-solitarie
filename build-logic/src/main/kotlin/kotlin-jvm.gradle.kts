import org.gradle.api.JavaVersion

/**
 * Convention plugin for PURE Kotlin/JVM modules (e.g. `:domain`).
 *
 * Applies ONLY the Kotlin/JVM plugin — deliberately NO Android plugin — so any
 * `android.*` / `androidx.*` import is structurally un-resolvable on the module's
 * compile classpath. This is the strongest (compiler-enforced) domain-purity
 * guarantee; the ArchUnit `DomainPurityTest` is the belt-and-suspenders CI gate.
 */
plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<Test>().configureEach {
    useJUnit()
}
