package com.app.pegsolitaire

import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.junit.ArchUnitRunner
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.junit.runner.RunWith

/**
 * Named domain-purity boundary gate (QUAL-01).
 *
 * Belt-and-suspenders to the structural guarantee that `:domain` is a pure-JVM
 * module with no Android plugin on its classpath: this ArchUnit test reads
 * BYTECODE (so it is immune to the Kotlin-2.4 source-parse risk that affects
 * source-parsing tools) and fails if any class in the domain package depends on
 * `android.*` or `androidx.*`. Runs under `:app:testDebugUnitTest`, which is
 * wired into `check`.
 */
@RunWith(ArchUnitRunner::class)
@AnalyzeClasses(packages = ["com.app.pegsolitaire.domain"])
class DomainPurityTest {
    @ArchTest
    val domainDoesNotDependOnAndroid: ArchRule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("android..", "androidx..")
}
