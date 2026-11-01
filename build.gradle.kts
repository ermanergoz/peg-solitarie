// Root build script.
//
// Almost intentionally empty: shared configuration lives in the `build-logic/` convention
// plugins (kotlin-jvm, android-app, standards) and per-module build files. All
// versions flow from `gradle/libs.versions.toml`. Do not declare plugins or
// dependencies here — with ONE deliberate exception, below.
//
// The exception (PLAT-06): `build-logic` is an *included build*, so its own `:check`
// — and therefore `AdIdResolutionTest`, the only place the release-ID failure contract
// can be proven (AGP 9.1.0 gives `:app` no `testReleaseUnitTest`) — is not reached by
// `./gradlew check` unless it is wired in explicitly. A gate that runs only as a
// separate command is a gate that gets skipped. The `base` plugin exists here solely to
// give the root project the `check` lifecycle task to hang that dependency on.
plugins {
    base
}

tasks.named("check") {
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
