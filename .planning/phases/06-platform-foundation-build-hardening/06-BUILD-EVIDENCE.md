# Phase 6 — Build Evidence

**Purpose:** the committed home for D-12's baseline numbers and the proof that R8 /
`shrinkResources` did not break anything player-visible. Later plans in this phase append
to the `TBD` sections below rather than starting new files.

**Environment for every measurement here:** AGP 9.1.0, Gradle 9.5.1 wrapper, Kotlin 2.3.20,
JDK 17, `compileSdk` 36, macOS (Darwin 24.6.0). Release variant, unsigned (D-16 — no signing
config is committed; real signing is Phase 14).

**Offline caveat (D-18).** The enforced offline invariant is `./gradlew check --offline`;
`check` does not build the release variant, so it is unaffected by anything on this page.
`assembleRelease` is **not** offline-safe: enabling `isMinifyEnabled` activates AGP 9.1.0's
`produceReleaseComposeMapping`, which resolves
`org.jetbrains.kotlin:compose-group-mapping:2.3.20` — an implicit, uncatalogued artifact that
is absent from a fresh Gradle cache. Every machine and CI runner needs **one warm online
`assembleRelease`**; after that it is cached. This build's `produceReleaseComposeMapping` and
`mergeReleaseComposeMapping` both ran green, so the cache on this machine is warm.

---

## APK size ledger

D-12 baselines APK size; it deliberately does **not** gate on it while the SDKs are still
landing. D-14 requires three numbers so that "pre-SDK" survives plan 06-09 putting the GMA /
PGS / UMP artifacts on the dependency graph.

| # | Configuration | Release APK (bytes) | MiB | Δ vs row 1 |
|---|---------------|--------------------:|----:|-----------|
| 1 | Pre-R8, pre-SDK — `main`/`develop` as committed, no `buildTypes` block at all | **9,890,332** | 9.43 | baseline |
| 2 | Post-R8, pre-SDK — `isMinifyEnabled` + `isShrinkResources` + `proguard-android-optimize.txt` + this repo's one keep rule | **1,518,323** | 1.45 | **−8,372,009 B (−84.65 %)** |
| 3 | Post-R8, with SDK artifacts on the graph but unreferenced by any code | **TBD — plan 06-09** | — | — |

**Row 1** was measured on 2026-08-27 during phase research (`06-RESEARCH.md § Measurements`),
before this plan existed, via an `--init-script` overlay that modified no repository file.
It is the "pre-SDK" figure Phase 14 compares against.

**Row 2** was measured by this plan (06-03) on 2026-08-27:

```
$ ./gradlew clean :app:assembleRelease
BUILD SUCCESSFUL
$ stat -f %z app/build/outputs/apk/release/app-release-unsigned.apk
1518323
```

**Deviation from the research figure: none.** Row 2 reproduced 1,518,323 B *exactly* — a 0.00 %
difference, well inside the 5 % tolerance the plan allowed. The research measurement and the
committed configuration are byte-identical in effect, which is the strongest available evidence
that the `buildTypes` block landed here matches what was actually probed.

Row 2, not row 1, is the more useful comparand for Phase 14: Phase 14's build will also be
minified, so comparing a minified build against an unminified baseline would attribute R8's
84.65 % win to whatever Phase 14 happened to change.

---

## shrinkResources — downloadable font certs

The one genuinely player-visible thing resource shrinking could break in this app is
`app/src/main/res/values/font_certs.xml`. Its `com_google_android_gms_fonts_certs` array is an
array **of array references** (`@array/..._dev`, `@array/..._prod`) and is reached only
indirectly, as `R.array.com_google_android_gms_fonts_certs` from
`app/src/main/java/com/app/pegsolitaire/ui/theme/Type.kt`. Had the shrinker failed to follow
resource→resource references, the nested cert arrays would have been dropped and downloadable
Fredoka/Nunito would have silently fallen back to the platform default font — a visible
regression in a phase whose success criterion is invisibility.

Verified against the **shrunk** APK:

```
$ $(grep '^sdk.dir' local.properties | cut -d= -f2)/build-tools/36.0.0/aapt2 \
    dump resources app/build/outputs/apk/release/app-release-unsigned.apk | grep fonts_certs

    resource 0x7f030000 array/com_google_android_gms_fonts_certs
      () (array) size=2
        [@array/com_google_android_gms_fonts_certs_dev, @array/com_google_android_gms_fonts_certs_prod]
    resource 0x7f030001 array/com_google_android_gms_fonts_certs_dev
      () (array) size=1
        ["MIIEqDCCA5CgAwIBAgIJANWFuGx90071MA0GCSqGSIb3DQEBBAUAMIGUMQswCQYDVQQGEwJVUzET…"]
    resource 0x7f030002 array/com_google_android_gms_fonts_certs_prod
      () (array) size=1
        ["MIIEQzCCAyugAwIBAgIJAMLgh0ZkSjCNMA0GCSqGSIb3DQEBBAUAMHQxCzAJBgNVBAYTAlVTMRMw…"]
```

All **three** entries survive, and the two leaf arrays still carry their base64 cert payloads
(truncated above for readability; both were present in full in the raw dump). The shrinker
follows resource→resource references correctly.

Corroborating counts: **236** resource entries remain in the shrunk APK — identical to the
research measurement. Resource shrinking demonstrably ran: `shrunk_resources_proto_format` and
`shrunk_resources_binary_format` are both present under `app/build/intermediates/`.

`lintVitalRelease` also passed under minification; enabling R8 surfaced no new lint-vital
errors on this codebase.

---

## R8 keep-rule surface

D-11 requires that any hand-written keep rule be commented with why it exists. The correct
outcome for PLAT-05 was one rule and an explanation, not a file full of defensive keeps — Room,
Hilt/Dagger, kotlinx-serialization, Navigation and DataStore all publish consumer keep rules,
and this app reflects at exactly one site (`PegSolitaireDatabase::class.java` in
`DatabaseModule.kt`), which Room's own consumer rule covers.

- **`app/build/outputs/mapping/release/missing_rules.txt` is ABSENT** after a clean, successful
  `assembleRelease`. AGP writes that file only when R8 detects missing classes it cannot
  resolve; its absence means R8 needed nothing beyond what is already committed.
- **`app/proguard-rules.pro` contains exactly one directive.** Verified mechanically:

  ```
  $ grep -v '^[[:space:]]*#' app/proguard-rules.pro | grep -c '^-'
  1
  ```

  The directive is `-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue`. It exists
  because Hilt 2.60's generated `ActivityRetainedComponentBuilder` references
  `@CanIgnoreReturnValue` on `savedStateHandleHolder`, while `hilt-android` ships
  `error_prone_annotations` only as `compileOnly`. Without the rule the build fails with
  `Missing class com.google.errorprone.annotations.CanIgnoreReturnValue`.
- **No `-keep` rule is committed** (`! grep -q '^-keep' app/proguard-rules.pro`). This matters
  under AGP 9.1.0, where `android.r8.strictFullModeForKeepRules` defaults to `true`: `-keep class A`
  no longer implicitly keeps `A.<init>()`, so any future keep for a reflectively-instantiated
  class must spell out `{ <init>(); }`. That warning is recorded in the file's header for the
  next author, since the failure mode is a runtime `NoSuchMethodException` in a build that
  compiled and shrank cleanly.
- **No `gradle.properties` R8 entry was added.** `android.enableR8.fullMode` and
  `android.r8.strictFullModeForKeepRules` both already default to `true` in AGP 9.1.0 (read out
  of `BooleanOption.kt`), so D-11's "R8 full mode" needs no opt-in; writing `=false` would be a
  regression.

---

## Cold-start baseline

**TBD — plan 06-08.** D-12 requires the pre-SDK cold-start figure to be captured in this phase
so PLAT-08 in Phase 14 has something to measure against.

---

## Release smoke test

**TBD — plan 06-10.** Phase 6 success criterion 4 ("a release build still launches and plays")
cannot be automated here: `assembleRelease` emits `app-release-unsigned.apk` and no signing
config is committed (D-16). Plan 06-10 debug-signs the APK locally for the smoke test.
