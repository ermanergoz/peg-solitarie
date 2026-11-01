# app/proguard-rules.pro
#
# PLAT-05 / D-11. This file is deliberately almost empty: Room, Hilt/Dagger,
# kotlinx-serialization, Navigation, DataStore and (later) the GMS SDKs all publish
# consumer keep rules, and this app performs reflection at exactly one site
# (PegSolitaireDatabase::class.java in DatabaseModule.kt:43), which Room's own rule
# (`-keep class * extends androidx.room.RoomDatabase { void <init>(); }`) covers.
#
# Every rule here MUST carry a comment explaining why it exists. Do not add
# speculative keeps — add a rule only when R8 emits it into
# app/build/outputs/mapping/release/missing_rules.txt or a release crash demands it.
# An over-broad keep silently defeats the point of minifying and nothing in the build
# will tell you it happened.
#
# NOTE: AGP 9.1.0 runs R8 in STRICT full mode (android.r8.strictFullModeForKeepRules
# defaults to true, as does android.enableR8.fullMode — neither needs a
# gradle.properties entry), so `-keep class A` does NOT implicitly keep A's default
# constructor. Any future -keep for a reflectively-instantiated class must spell out
# `{ <init>(); }`. Forgetting this is the single most likely source of a runtime
# NoSuchMethodException in a build that compiled and shrank cleanly.

# Hilt 2.60's generated ActivityRetainedComponentBuilder references
# @CanIgnoreReturnValue (on savedStateHandleHolder), but hilt-android does not ship
# error_prone_annotations at runtime — it is a compileOnly dependency. The annotation
# is not needed at runtime, so the reference is safe to leave dangling.
# Verified against this exact dependency set on 2026-08-27: this is the ONLY rule R8
# requires. Without it the release build fails with
#   "Missing class com.google.errorprone.annotations.CanIgnoreReturnValue".
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
