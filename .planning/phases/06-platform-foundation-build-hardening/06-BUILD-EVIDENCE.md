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
| 3 | Post-R8, with SDK artifacts on the graph but unreferenced by any code | **1,999,795** | 1.91 | **−7,890,537 B (−79.78 %)** |

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

**Row 3** was measured by plan 06-09 on 2026-08-27, immediately after the three SDK artifacts
landed on the graph (D-14) and while **no code references any of them**:

```
$ ./gradlew clean :app:assembleRelease
BUILD SUCCESSFUL
$ stat -f %z app/build/outputs/apk/release/app-release-unsigned.apk
1999795
```

**Interpretation against row 2 — and a correction to the expectation.** The plan predicted
row 3 ≈ row 2 on the reasoning that R8 strips code nothing calls. It is **+481,472 B
(+31.7 %)** instead, and the gap is worth understanding now rather than at Phase 14, because
it is the part that will *not* shrink further.

R8 did strip the SDK code itself, almost completely. Of 4,469 classes surviving into
`mapping.txt`, only **9** are `com.google.android.libraries.ads..`, **45** are
`com.google.android.gms.games..`, and **0** are `com.google.android.ump..` — against roughly
6.6 MB of input AAR. Nothing calls those SDKs, so nearly all of them are gone. That much
matched the prediction.

What the prediction missed is that dependencies do not enter an APK only through code
reachability:

- **Manifest-declared components are roots R8 cannot remove.** The merge brings in
  `AdActivity`, `GamesResolutionActivity`, `PlayGamesAppShortcutsActivity`,
  `GoogleApiActivity` and `HsdpShimActivity`, and every class reachable from them is live by
  definition. That is why 296 `com.google.android.gms..` classes and 93
  `com.google.android.play..` / `protobuf` / `datatransport` classes survive.
- **The ads SDK drags in whole androidx libraries that self-register.** `androidx.work`
  (134 retained classes) declares its own services and an `androidx.startup` initializer;
  `emoji2`, `browser`, `webkit` and `play-services-cronet` arrive the same way. These are
  kept alive by their own manifests, not by this app.
- **Non-code payload is never shrunk at all.** `okhttp3/internal/publicsuffix/publicsuffixes.gz`
  (41,394 B), the `google/protobuf/*.proto` descriptors, and eight 10,175 B androidx
  `LICENSE.txt` files are packaged verbatim regardless of what R8 concludes about code.

**Consequence for Phase 14.** Row 2 remains the meaningful comparand for judging *this
project's* growth, exactly as stated above — but the ~480 KB in row 3 is the fixed entry price
of having the SDKs on the graph, and it is already paid. Phase 14 should expect the increment
from real integration to be measured against **row 3**, not row 2: wiring `MobileAds.initialize`
and the PGS calls will move far less than another 480 KB, because the expensive part
(manifest roots and transitive androidx libraries) is already resident.

**How row 3 was invoked, stated plainly.** Plan 06-07's D-09 gate now refuses to *configure* a
release invocation without real ad/PGS IDs, and the real IDs still do not exist (see § External
prerequisites, items 1–2, "not started"). The gate fired correctly and blocked the first
attempt. Row 3 was therefore measured with **throwaway, all-zero, non-functional IDs**
(`ca-app-pub-0000000000000000~0000000000` and friends) placed in the untracked, gitignored
`local.properties` — never committed, incapable of serving an ad or reaching a PGS project.
This does not weaken the number: the IDs appear in the APK only as four short strings in
`BuildConfig` and `resources.arsc`, so their contents cannot move a byte count meaningfully.
It is recorded here because a reader comparing row 3 to a future Phase-14 measurement taken
with *real* IDs should know the difference exists and is negligible — and because "the release
gate fired and was satisfied with placeholders for a local measurement" is exactly the kind of
step that must not be invisible.

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

**Pre-SDK figure (D-12).** Captured 2026-08-27, *before* plan 06-09 adds any SDK artifact to the
dependency graph — this is the before-picture PLAT-08 measures against in Phase 14.

| Field | Value |
|-------|-------|
| Device | AVD `Pixel_9a` (`emulator-5554`) |
| API level | 36 (`ro.build.version.sdk`) |
| Variant | **debug** (`:app:installDebug`) — release is unsigned until 06-10; a consistent variant matters more than absolute realism for a before/after comparison |
| Command | `adb shell am force-stop com.app.pegsolitaire`, ~2 s pause, `adb shell am start -W -n com.app.pegsolitaire/.MainActivity`, ×10 |
| Samples | 10 taken, first discarded as warm-up, **9 recorded** |
| TotalTime (ms) | min **603** · median **633** · max **649** |
| Raw samples (ms) | 649, 633, 629, 633, 649, 622, 609, 639, 603 (discarded warm-up: 721) |

**Precision caveat:** `am start -W` displayed-time is coarse. It is affected by host load, emulator
thermal/background state, and is **not comparable across devices**. Phase 14 must re-measure on the
**same AVD (`Pixel_9a`, API 36) on the same host** for the PLAT-08 comparison to mean anything.
This is an honest smoke-level baseline, not a benchmark-grade number.

---

## Release smoke test

**TBD — plan 06-10.** Phase 6 success criterion 4 ("a release build still launches and plays")
cannot be automated here: `assembleRelease` emits `app-release-unsigned.apk` and no signing
config is committed (D-16). Plan 06-10 debug-signs the APK locally for the smoke test.

---

## External prerequisites

Recorded 2026-08-27 at the 06-08 Task 3 checkpoint. **Discovery: this milestone updates the
already-published listing `com.erman.pegsolitarie`** (see CONTEXT D-20..D-23), which removes the
two worst lead-time items (developer account, website hosting).

| # | Item | Status | Date | Notes |
|---|------|--------|------|-------|
| 1 | Play Console PGS project + OAuth consent + credentials + testers | not started — **unblocked** | 2026-08-27 | Developer account EXISTS (app published). Create the PGS project on the existing `com.erman.pegsolitarie` app entry. Credentials needed: original release-keystore SHA-1 + debug SHA-1 (no Play App Signing yet — D-21; enroll at next upload). |
| 2 | AdMob ad units (rewarded + interstitial) | not started — **unblocked** | 2026-08-27 | AdMob account EXISTS. Add app via "Yes, it's listed on Google Play" → `com.erman.pegsolitarie`, then create the two units. |
| 3 | `app-ads.txt` on developer website | not started — **unblocked** | 2026-08-27 | **A developer website EXISTS and the user controls it** — the answer to the milestone's most schedule-relevant question. Remaining work: upload the one-line file, confirm the listing's Website field, wait ≤24 h crawl. |
| 4 | UMP consent message in AdMob UI | not started | 2026-08-27 | Minutes of work once item 2's app link exists. Blocks Phase 9 (CONS-01). |
| 5 | 10–12 achievement icons (512×512) | not started | 2026-08-27 | Art production; needed before Phase 12 console config. Popups silently fail without them. |
| 6 | Saved Games enabled on PGS project | blocked on item 1 | 2026-08-27 | One toggle once the PGS project exists. Blocks Phase 13. |

Lead-time reassessment: with the account and website existing, the longest remaining lead is PGS
tester propagation (hours–days) and the app-ads.txt crawl (≤24 h) — both comfortably absorbed by
Phases 7–8 as sequenced. The genuinely user-gated items (1–4, 6) total roughly an hour of console
work; item 5 is the only open-ended one.
