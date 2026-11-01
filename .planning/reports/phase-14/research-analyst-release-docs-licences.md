# research-analyst report — release-docs-licences (code-auditor F-2 and I-1)
Date: 2026-10-21
Branch: feature/phase-14-release-docs (committed by the producer)
Skills loaded: ergoz-soft:standards (role research-analyst). The product snapshot is 0.5.1 and the plugin is 1.0.0, so I read the plugin 1.0.0 rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and ip-policy. I also read the product's `.claude/rules/lessons.md`. I loaded research-open-source, research-competitive and market-research because the role lists them. I did not use their AskUserQuestion intake or their `artifacts/research/` output: only the producer talks to the founder, and this dispatch allows one output file.
Ran in parallel with: the F-1 to F-8 text fixes by other roles in this worktree (per the dispatch; slugs not given)
Ran on: claude-opus-5-5 at effort xhigh
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **hsdp 2.0.1 and UMP 4.0.0 have known licences.** Each POM on Google Maven declares the *Android Software Development Kit License*, the same licence as their siblings in §4. U-1 closes and hsdp gets a row. Neither needs escalation.
- **I-1 is closed.** The POMs of `play-services-games-v2` 22.0.0 and `ads-mobile-sdk` 1.4.0 were fetched again from `dl.google.com` and still declare that licence. §4's rows stand.
- **Item 4 families:**
  - Protocol Buffers (BSD-3) needs attribution. It ships twice; the second copy is an androidx artifact that the font report missed.
  - Chromium Cronet (BSD-3) needs attribution.
  - The OkHttp public-suffix data (MPL-2.0) needs its notice and a pointer to the source.
  - checker-qual (MIT) needs no notice while R8 strips it, but it still gets a row.
- **The full graph is now checked.** Every POM on the release graph was resolved (192 of 192): none is unknown and none declares a copyleft licence. No founder question is needed.

## Verdicts

The matrix rows used come from `rules/ip-policy.md`.

| # | Item | Licence (evidence) | Matrix row | Verdict |
|---|---|---|---|---|
| 1 | `com.google.android.play:hsdp` 2.0.1 | Android Software Development Kit License (POM `<licenses>`) | Not in the matrix: a proprietary vendor SDK. It is neither "No license / unknown" nor copyleft | **CLEAN.** The licence is known and the library ships unmodified as Google directs. **Attribution:** the SDK licence needs none. The 70 open-source components bundled with it fall under the shared Google-SDK notice duty (font report, open issue 2) |
| 2 | `com.google.android.ump:user-messaging-platform` 4.0.0 | Android Software Development Kit License (POM `<licenses>`) | as #1 | **CLEAN. U-1 closes.** Attribution as #1 (59 bundled components) |
| 3a | `com.google.android.gms:play-services-games-v2` 22.0.0 | Android Software Development Kit License (POM, fetched again) | as #1 | **CLEAN.** The §4 row is confirmed |
| 3b | `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk` 1.4.0 | Android Software Development Kit License (POM, fetched again) | as #1 | **CLEAN.** The §4 row is confirmed |
| 4a | `com.google.protobuf:protobuf-javalite` 3.22.3 | BSD-3-Clause (POM, via `protobuf-parent`; upstream LICENSE at v22.3) | "MIT, Apache-2.0, BSD-2/3 … — yes — keep notice + attribution" | **ATTRIBUTION REQUIRED.** 73 classes survive R8 |
| 4b | `androidx.datastore:datastore-preferences-external-protobuf` 1.2.1 (**new**: not in the font report's list) | BSD-3-Clause (POM: "Repackaged proto-lite dependency for use by datastore preferences") | BSD-3 row | **ATTRIBUTION REQUIRED.** One "Protocol Buffers" credit covers #4a and #4b. 79 classes survive R8 |
| 4c | Cronet, `org.chromium.net:{cronet-api, cronet-shared, cronet-common, cronet-fallback, httpengine-native-provider}` 143.7445.0 | BSD-3-Clause, Chromium. The POM's licence name is "Chromium and built-in dependencies", and its URL resolves to Chromium's BSD-3 text followed by sections for native dependencies. No native code ships (see Evidence) | BSD-3 row | **ATTRIBUTION REQUIRED.** 151 classes survive R8 |
| 4d | `org.checkerframework:checker-qual` 3.33.0 | MIT (POM; `META-INF/LICENSE.txt` in the jar) | "MIT … yes — keep notice + attribution" | **CLEAN.** No notice duty while nothing ships. The 2026-09-21 release build kept 0 of its classes, and AGP drops its `META-INF/LICENSE.txt`. Recommended anyway: credit it (see Open issues 2) |
| 4e | Public Suffix List data in `com.squareup.okhttp3:okhttp` 4.11.0 (`okhttp3/internal/publicsuffix/publicsuffixes.gz`) | MPL-2.0. OkHttp's own NOTICE and the list's header say so; OkHttp's code is Apache-2.0 | "MPL-2.0 — yes (file-level copyleft) … modified files stay MPL" | **ATTRIBUTION REQUIRED (notice and source pointer).** The file ships unmodified, so the copyleft binds only that file and never the app. MPL-2.0 §3.2(a) requires telling recipients where the source is. The shipped NOTICE does that; the Licenses screen should repeat it |

**The Android SDK Licence verdict (#1 to #3), stated precisely:**
- The agreement grants a licence "to use the SDK solely to develop applications for compatible implementations of Android" (§3.1). §3.4 bars redistributing "the SDK or any part of the SDK" except as third-party licences require.
- No clause expressly says "you may ship these libraries inside your app". The permission to ship rests on Google's own distribution of these artifacts on its Maven repository as app dependencies, and on its integration guides.
- `hsdp` belongs to the Play Core reference set (`com.google.android.play.core.hsdp`). The *Play Core Software Development Kit Terms of Service* do grant that right expressly: "you may copy and distribute Redistributable Code solely for inclusion as part of your API Client".
- This is not an escalation. The licence is known, it comes from the rights holder, it is not copyleft, and the studio already accepts it for two §4 rows. The wording of those rows should match the evidence, though (Open issues 3).

## Table for THIRD_PARTY.md (transcribe as rows)

"Licenses screen" means the screen in Settings › About specified in `docs/ux/licences-screen.md` on `feature/candy-backdrop`. It is not built yet, and its entries for rows F to J need the ux-designer's amendment (Handoff).

| Name | Coordinates@version | Source URL | Licence | What is used | How credited |
|---|---|---|---|---|---|
| A. Google Play HSDP (Play inline-install "half sheet details page" library) | `com.google.android.play:hsdp@2.0.1` | https://dl.google.com/dl/android/maven2/com/google/android/play/hsdp/2.0.1/hsdp-2.0.1.pom | Android Software Development Kit License Agreement, https://developer.android.com/studio/terms (proprietary Google SDK; not open source) | The library, unmodified. It comes in transitively through `ads-mobile-sdk` 1.4.0, and no app code calls it | The SDK licence requires no credit, only that Google's notices stay intact (§3.8). Its bundled `third_party_licenses.txt` (70 components) falls under the Google-SDK notices ruling (font report, open issue 2) |
| B. Google User Messaging Platform | `com.google.android.ump:user-messaging-platform@4.0.0` | https://dl.google.com/dl/android/maven2/com/google/android/ump/user-messaging-platform/4.0.0/user-messaging-platform-4.0.0.pom | as A | The consent SDK, unmodified, as a direct `implementation` dependency | As A (59 bundled components) |
| C. Google Play Games Services v2 | `com.google.android.gms:play-services-games-v2@22.0.0` | https://dl.google.com/dl/android/maven2/com/google/android/gms/play-services-games-v2/22.0.0/play-services-games-v2-22.0.0.pom | as A | The SDK, unmodified, as a direct dependency | As A |
| D. Google Mobile Ads SDK (Next-Gen) | `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk@1.4.0` | https://dl.google.com/dl/android/maven2/com/google/android/libraries/ads/mobile/sdk/ads-mobile-sdk/1.4.0/ads-mobile-sdk-1.4.0.pom | as A | The SDK, unmodified, as a direct dependency | As A (98 bundled components, per the font report) |
| E. Google Play services siblings (family) | `com.google.android.gms:` `play-services-basement@18.9.0`, `-base@18.5.0`, `-tasks@18.2.0`, `-ads-identifier@18.0.0`, `-appset@16.0.1`, `-cronet@18.0.1` | https://dl.google.com/dl/android/maven2/com/google/android/gms/ (each `<artifact>/<version>/<artifact>-<version>.pom`) | as A; each POM was read | Transitive, unmodified (Games v2, Mobile Ads, UMP, hsdp) | As A |
| F. Protocol Buffers (Java Lite runtime) | `com.google.protobuf:protobuf-javalite@3.22.3` | https://github.com/protocolbuffers/protobuf/blob/v22.3/LICENSE ; POM https://repo1.maven.org/maven2/com/google/protobuf/protobuf-parent/3.22.3/protobuf-parent-3.22.3.pom | BSD-3-Clause | The runtime library, unmodified. It is pulled at runtime scope by `org.chromium.net:cronet-shared` 143.7445.0 (73 classes after R8) | Licenses screen entry "Protocol Buffers": "Copyright 2008 Google Inc. All rights reserved.", the three conditions and the disclaimer. Text: the v22.3 LICENSE verbatim (1,732 B, sha256 `6e5e117324afd944…`) |
| G. Protocol Buffers, repackaged by AndroidX DataStore | `androidx.datastore:datastore-preferences-external-protobuf@1.2.1` | https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences-external-protobuf/1.2.1/datastore-preferences-external-protobuf-1.2.1.pom | BSD-3-Clause | protobuf-lite repackaged as `androidx.datastore.preferences.protobuf`, unmodified, through `datastore-preferences` 1.2.1 (79 classes after R8). Its own `LICENSE.txt` ships in the APK but carries no copyright line | The same "Protocol Buffers" entry as F, which names both copies. The copyright line that its bundled text lacks comes from F |
| H. Chromium Cronet (Java API, fallback and HttpEngine provider) | `org.chromium.net:cronet-api`, `cronet-shared`, `cronet-common`, `cronet-fallback`, `httpengine-native-provider`, all `@143.7445.0` | https://github.com/chromium/chromium/blob/143.0.7445.0/LICENSE ; the POMs' licence file: https://storage.googleapis.com/chromium-cronet/android/143.0.7445.0/Release/cronet/LICENSE | BSD-3-Clause (Chromium) | The Java-only Cronet classes, unmodified, through `ads-mobile-sdk` 1.4.0 (151 classes after R8). No native library ships | Licenses screen entry "Chromium (Cronet)": "Copyright 2015 The Chromium Authors", the three conditions and the disclaimer. Text: Chromium's LICENSE at 143.0.7445.0 verbatim (1,536 B, sha256 `368cca1106be99d3…`) |
| I. Checker Framework qualifiers | `org.checkerframework:checker-qual@3.33.0` | https://github.com/typetools/checker-framework/blob/checker-framework-3.33.0/checker-qual/LICENSE.txt ; POM https://repo1.maven.org/maven2/org/checkerframework/checker-qual/3.33.0/checker-qual-3.33.0.pom | MIT | Annotations, on the graph through Guava 32.0.0. **Nothing ships:** the 2026-09-21 release kept 0 classes, and AGP excludes its `META-INF/LICENSE.txt` | No duty while nothing ships. Recommended: a Licenses screen entry "Checker Framework qualifiers" with its MIT text verbatim (1,126 B, sha256 `e90eaa08dea089df…`), so no per-release R8 check is needed |
| J. Public Suffix List (data compiled into OkHttp) | `com.squareup.okhttp3:okhttp@4.11.0`, file `okhttp3/internal/publicsuffix/publicsuffixes.gz` (41,394 B) with its `NOTICE` (218 B) | https://publicsuffix.org/list/public_suffix_list.dat ; https://github.com/square/okhttp/blob/parent-4.11.0/okhttp/src/main/resources/okhttp3/internal/publicsuffix/NOTICE | MPL-2.0 (this data file only; OkHttp's code is Apache-2.0) | The compiled suffix list, unmodified, inside OkHttp, through `ads-mobile-sdk` 1.4.0. Both files ship as Java resources in the release APK | Licenses screen entry "Public Suffix List": "Mozilla Public License 2.0", OkHttp's NOTICE verbatim (sha256 `8a9c58fbded5ea34…`), which names the source and https://mozilla.org/MPL/2.0/. Optionally the MPL-2.0 text (16,726 B, sha256 `3f3d9e0024b1921b…`) |

**Where the rows go in THIRD_PARTY.md:**
- **B:** replaces the "**unverified** — see §6" licence cell in §4. U-1 is marked closed, with this report as evidence.
- **C and D:** their §4 "How verified" cells gain the POM URL and this date.
- **A, E and F to J:** become §5b rows, or one row per family.
- **The §5b androidx row:** add an exception for G, which is BSD-3 inside a family that row calls Apache-2.0.
- **U-6:** stays open for the per-coordinate listing on the release branch (Open issues 1).

**Also for F-2 (the Apache-2.0 families it names):** every POM below was read and declares Apache-2.0. Their credit is the Apache-2.0 text the Licenses screen already plans, plus the Jakarta NOTICE from the font report.

| Coordinates@version | POM licence name |
|---|---|
| `com.squareup.okhttp3:okhttp@4.11.0` | The Apache Software License, Version 2.0 |
| `com.squareup.okio:okio@3.9.1`, `okio-jvm@3.9.1` | The Apache Software License, Version 2.0 |
| `com.google.code.gson:gson@2.13.2` | Apache-2.0 |
| `com.google.crypto.tink:tink-android@1.20.0` | Apache License, Version 2.0 |
| `com.google.guava:guava@32.0.0-jre` (via `guava-parent`), `failureaccess@1.0.1`, `listenablefuture@9999.0-empty-to-avoid-conflict-with-guava` | Apache License, Version 2.0 / The Apache Software License, Version 2.0 |
| `com.google.code.findbugs:jsr305@3.0.2` | The Apache Software License, Version 2.0 |
| `com.google.errorprone:error_prone_annotations@2.41.0` | Apache 2.0 |
| `com.google.j2objc:j2objc-annotations@2.8` | Apache License, Version 2.0 |
| `org.jspecify:jspecify@1.0.0` | The Apache License, Version 2.0 |
| `org.jetbrains:annotations@23.0.0` | The Apache Software License, Version 2.0 |
| `jakarta.inject:jakarta.inject-api@2.0.1` (NOTICE.md ships; font report) | The Apache Software License, Version 2.0 |
| `javax.inject:javax.inject@1` | The Apache Software License, Version 2.0 |

## Evidence

**Versions on `develop`** (read-only in `PegSolitarie-dev` at `0582e3f`):
- `gradle/libs.versions.toml` pins `adsMobileSdk = "1.4.0"`, `userMessagingPlatform = "4.0.0"`, `playServicesGamesV2 = "22.0.0"` and `datastore = "1.2.1"`. They are declared at `implementation` in `app/build.gradle.kts:515,539-541`.
- The transitive versions come from the release dependency report of 2026-09-15: `PegSolitarie/app/build/intermediates/.../collectReleaseDependencies/dependencies.pb`, sha256 `fd6f7941f5c604c9…`, 192 coordinates.
- `git diff f734f44 develop` over the catalog and the `implementation` lines shows one dependency change since that report: `ui-text-google-fonts` was removed. That is an AndroidX artifact outside every family here.
- The POM chain from develop's own pins confirms each version independently:
  - `ads-mobile-sdk` 1.4.0 declares `hsdp` 2.0.1, `user-messaging-platform` 4.0.0, `cronet-api` and `cronet-fallback` 143.7445.0, and `okhttp` 4.11.0.
  - `cronet-shared` 143.7445.0 declares `protobuf-javalite` 3.22.3 at runtime scope.
  - `guava-parent` 32.0.0-jre manages `checker-qual` 3.33.0.
  - DataStore 1.2.1 brings `datastore-preferences-external-protobuf` 1.2.1.
- `hsdp`'s `maven-metadata.xml` lists 2.2.0 as the latest version. Nothing on the graph asks for more than 2.0.1.

**POMs fetched on 2026-10-21** (HTTP 200 each; size and sha256 prefix):
- `ads-mobile-sdk-1.4.0.pom` 5,185 B `40943be80f764280`
- `play-services-games-v2-22.0.0.pom` 1,393 B `1b4c8a2985718ec0`
- `user-messaging-platform-4.0.0.pom` 1,164 B `cfacd698724de2c5`
- `hsdp-2.0.1.pom` 1,135 B `73158570a39d7fd1`

Each of the four carries `<license><name>Android Software Development Kit License</name><url>https://developer.android.com/studio/terms.html</url>`.

Further POMs:
- `protobuf-parent-3.22.3.pom` `399133d7f6f57934` declares BSD-3-Clause.
- `datastore-preferences-external-protobuf-1.2.1.pom` `7f788df98bec7361` declares BSD-3-Clause.
- `checker-qual-3.33.0.pom` `f55a922027a78fdd` declares "The MIT License".
- `okhttp-4.11.0.pom` `7a2d427b38b17e07` declares Apache 2.0.
- `cronet-api-143.7445.0.pom` `9e6dce46bce2a997` names "Chromium and built-in dependencies". All five Cronet POMs carry the same licence block.

**Whole-graph sweep** (`pomlic_all.py`, parent chains followed). All 192 POMs were found:
- 174 Apache-2.0, under five spellings;
- 10 Android Software Development Kit License: the four SDKs above, plus `play-services-basement`, `-base`, `-tasks`, `-ads-identifier`, `-appset` and `-cronet`;
- 5 Chromium;
- 2 BSD-3-Clause;
- 1 MIT;
- 0 with no licence and 0 copyleft.

The `-auth-*` artifacts that §5b names as an example are not on this graph.

**Cronet ships Java only:**
- The five cached AARs hold only `classes.jar`, `R.txt` and `AndroidManifest.xml`: no `jni/` folder and no `.so` file.
- Their classes sit in `org.chromium.*` and `internal.org.chromium.build.*` only.
- All 87 source files in the five `-sources.jar` files open with "Copyright YYYY The Chromium Authors".
- The POM's licence file is 326,255 B, sha256 `be9838eb10695d40`.
  - It begins with Chromium's BSD-3 text. That text is byte-equal to Chromium's own `LICENSE` at tag 143.0.7445.0.
  - Its other sections (BoringSSL, zlib, QUICHE, the Rust crates and more) cover native code that these artifacts do not contain.
  - It names no GPL, LGPL or MPL licence.
  - Its "Protocol Buffers" section is the same BSD-3 text as row F.
- The licence URL's own host, `storage.cloud.google.com`, answers with a 302 sign-in redirect. The same object is public at `storage.googleapis.com`.

**What ships**, from the release build of 2026-09-21 (`app/build/outputs/mapping/release/mapping.txt`, R8 9.1.31, min_api 24):
- Classes kept per family: `com.google.protobuf.` 73, `androidx.datastore.preferences.protobuf.` 79, `org.chromium.net.` 151, `org.checkerframework.` 0, and `okhttp3.internal.publicsuffix.PublicSuffixDatabase` kept.
- The release Java resources (`merged_java_res/release/minifyReleaseWithR8/base.jar`) carry:
  - `okhttp3/internal/publicsuffix/publicsuffixes.gz` and its `NOTICE`;
  - `META-INF/androidx/datastore/datastore-preferences-external-protobuf/LICENSE.txt` (1,434 B, sha256 `a01b712fbb30f80b`). It has BSD-3's conditions but no copyright line;
  - no checker-qual licence.

**Licence texts:**
- The jar copies equal upstream at the tag: checker-qual `LICENSE.txt` is `e90eaa08…` in both, and OkHttp's NOTICE is `8a9c58fb…` in both.
- protobuf's `LICENSE` at v22.3 opens with "Copyright 2008 Google Inc. All rights reserved."
- The Public Suffix List's header reads: "This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0." (MPL Exhibit A).
- Clauses used:
  - MPL-2.0 §3.2(a): inform recipients how to obtain the Source Code Form;
  - MPL-2.0 §3.3: a Larger Work may be under terms of your choice;
  - BSD-3 condition 2: binary redistributions reproduce the notice "in the documentation and/or other materials provided with the distribution";
  - MIT: the notice is included "in all copies or substantial portions of the Software".

**Google terms:**
- **Android SDK License Agreement**, https://developer.android.com/studio/terms (dated April 28, 2026). §3.1 is the use grant quoted above. §3.4 restricts redistribution of the SDK. §3.5 says open-source components are governed by their own licences. §3.8 says not to remove proprietary notices.
- **Play Core SDK Terms of Service**, https://developer.android.com/guide/playcore (terms last modified 2020-09-24): "you may copy and distribute Redistributable Code solely for inclusion as part of your API Client".
- **Mobile Ads SDK terms**, https://developers.google.com/admob/terms (updated 2026-10-20): "Your use of the Google Mobile Ads SDK is subject to the terms governing your use of AdMob."
- **Google's open-source notices guide**, https://developers.google.com/android/guides/opensource (updated 2026-10-20): "you as a developer are responsible for appropriately displaying the notices for the open source libraries that your app uses."
- **hsdp reference**, https://developer.android.com/reference/com/google/android/play/core/hsdp/service/package-summary (updated 2026-08-14): "The interface to start the Google Play inline install flow".

**Bundled notices in hsdp and UMP:** `third_party_licenses.txt` is 814,987 B in hsdp and 788,804 B in UMP. A scan for copyleft names hits only two entries.
- The **J2ObjC** and **Kotlin** entries hit. Each reproduces its whole repository's licence set, compiler and tooling included.
- The artifacts of theirs that ship are Apache-2.0 by their own POMs: `j2objc-annotations` 2.8 and `kotlin-stdlib`.
- No escalation follows.

## Outputs
- `.planning/reports/phase-14/research-analyst-release-docs-licences.md`: this report.
- I edited no other repo file and committed nothing, as dispatched.
- My working files sit in the session scratchpad, outside the repo: the fetched POMs, the licence texts, `licences-all.tsv` and the three helper scripts.

## Checks
- `curl -sS https://dl.google.com/dl/android/maven2/<group>/<artifact>/<version>/<artifact>-<version>.pom` for `ads-mobile-sdk` 1.4.0, `user-messaging-platform` 4.0.0, `play-services-games-v2` 22.0.0, `hsdp` 2.0.1, the five Cronet artifacts, six `play-services-*` siblings and `datastore-preferences-external-protobuf` 1.2.1 — pass (all 200; licences as above).
- `curl -sS https://dl.google.com/dl/android/maven2/com/google/android/play/hsdp/maven-metadata.xml` — pass (1.0.0 to 2.2.0; 2.0.1 present).
- `curl -sS https://repo1.maven.org/maven2/...pom` for protobuf-javalite and protobuf-parent 3.22.3, checker-qual 3.33.0, okhttp 4.11.0, and guava and guava-parent 32.0.0 — pass (all 200).
- `python3 -I pbdeps.py dependencies.pb` — pass (192 coordinates decoded).
- `python3 -I pomlic_all.py <dir> deps-sep15.txt` — pass (192 of 192 POMs; 0 unknown; 0 copyleft).
- `python3 -I pomlic.py` over the 15 Apache-family coordinates — pass (all Apache-2.0).
- `git diff f734f44 develop -- gradle/libs.versions.toml app/build.gradle.kts` (read-only, `PegSolitarie-dev`) — pass (only `ui-text-google-fonts` removed).
- `unzip -l` and `unzip -p <aar> classes.jar | unzip -Z1` over the five Cronet AARs — pass (no `jni/`, no `.so`; `org.chromium.*` only).
- A header tally over the five Cronet `-sources.jar` files — pass (87 of 87 "The Chromium Authors").
- `curl -sS https://storage.googleapis.com/chromium-cronet/android/143.0.7445.0/Release/cronet/LICENSE` — pass (200, 326,255 B; no GPL, LGPL or MPL). The POM's `storage.cloud.google.com` host gave a 302 sign-in redirect.
- `curl -sSL https://raw.githubusercontent.com/chromium/chromium/143.0.7445.0/LICENSE`, then a `diff` against the Cronet LICENSE's first section — pass (identical). `chromium.googlesource.com` returned 503.
- `curl -sSL` of the protobuf v22.3 LICENSE, the checker-qual 3.33.0 LICENSE, the OkHttp parent-4.11.0 NOTICE and the MPL-2.0 text — pass (200). The checker-qual and NOTICE hashes equal the jar copies.
- `curl -sSL https://publicsuffix.org/list/public_suffix_list.dat | head` — pass (MPL-2.0 header).
- `grep -E '^<prefix>[^ ]* -> ' mapping.txt` (release build of 2026-09-21) — pass (counts as above).
- `unzip -l merged_java_res/release/minifyReleaseWithR8/base.jar` — pass (the public-suffix data, its NOTICE and the DataStore BSD text ship; no checker-qual licence).
- WebFetch of developer.android.com/studio/terms, developer.android.com/guide/playcore, developers.google.com/admob/terms and the hsdp reference page — pass (quoted above).
- `curl -sSL https://developers.google.com/android/guides/opensource` — pass (200; quoted above).
- `python3 -I tpl.py` over the `third_party_licenses.json` and `.txt` files of hsdp and UMP — pass (copyleft names only in the J2ObjC and Kotlin whole-repository entries).
- Not run, as dispatched: Gradle (`./gradlew check` and any dependency task), `git commit`, and any edit to `THIRD_PARTY.md`, `docs/**` or `CHANGELOG.md`.

## Open issues / risks
1. **The graph is evidence, not the 3.0.0 listing.**
   - The versions rest on the 2026-09-15 report plus develop's POM chain. That is close, but `releaseRuntimeClasspath` on `release/3.0.0` (checklist 3.5) is still the authority, so U-6 stays open.
   - Any coordinate the listing adds that is not in this report's 192 needs a verdict before the release.
   - The listing should also confirm that Cronet stays Java-only. A native `cronet-embedded` would bring in the native sections of the 326 KB LICENSE (BoringSSL, zlib and more).
2. **checker-qual depends on R8.** Its "no notice duty" rests on the 2026-09-21 mapping, where 0 classes were kept. The cheaper and more robust choice is to credit it (row I). Otherwise checklist 3.5a must grep the release mapping for `org.checkerframework.` every release.
3. **The §4 wording for Google SDKs overstates the evidence.** The cells say "redistribution permitted as part of an application". The Android SDK License has no such clause (§3.1 and §3.4 above). Suggested cell: "Android Software Development Kit License Agreement (declared in the POM) — proprietary Google SDK, not open source; licensed for building Android apps and published by Google on its Maven repository for inclusion in them; shipped unmodified". This is a wording correction, not a change of verdict, and it is the release-manager's edit.
4. **The §5b androidx row is wrong for one member.** `datastore-preferences-external-protobuf` is BSD-3 (row G), and its in-APK licence text lacks the copyright line. The Licenses screen spec's "AndroidX: Apache-2.0" entry, if it reads that way, needs the same exception.
5. **The Google SDK notices are still unruled** (font report, open issue 2; tech-lead). That ruling now covers hsdp (70 components) and UMP (59) as well as Mobile Ads and Games v2.
6. **No founder question.** Nothing here is copyleft, unknown, NC or ND, and no trademark question arises.

## Handoff
Next: producer —
- **release-manager:**
  - Transcribe rows A to J and the Apache table into `THIRD_PARTY.md`.
  - Close U-1 and give `hsdp` its row.
  - Correct the §4 Google-SDK wording (Open issues 3) and add the §5b androidx exception (Open issues 4).
  - Rewrite the §5b and §7 sentences that F-2 flags so they point at the notice duties above (tech design §16 #20).
  - Keep U-6 for checklist 3.5, and add the checker-qual mapping check to 3.5a if row I is not credited.
- **ux-designer:** amend `docs/ux/licences-screen.md` with entries for "Protocol Buffers" (both copies), "Chromium (Cronet)", "Public Suffix List" (MPL-2.0, with the source pointer) and, recommended, "Checker Framework qualifiers".
- **tech-lead:** the Google-SDK notices ruling (font report, open issue 2), now covering hsdp and UMP too.
- **android-game-dev (when the screen is built):** bundle the texts verbatim at the hashes given in rows F, H, I and J.
- **code-auditor:** re-check F-2 and I-1 against the transcribed rows.
- **Founder:** nothing.

## Sources
- https://dl.google.com/dl/android/maven2/com/google/android/play/hsdp/2.0.1/hsdp-2.0.1.pom
- https://dl.google.com/dl/android/maven2/com/google/android/play/hsdp/maven-metadata.xml
- https://dl.google.com/dl/android/maven2/com/google/android/ump/user-messaging-platform/4.0.0/user-messaging-platform-4.0.0.pom
- https://dl.google.com/dl/android/maven2/com/google/android/gms/play-services-games-v2/22.0.0/play-services-games-v2-22.0.0.pom
- https://dl.google.com/dl/android/maven2/com/google/android/libraries/ads/mobile/sdk/ads-mobile-sdk/1.4.0/ads-mobile-sdk-1.4.0.pom
- https://dl.google.com/dl/android/maven2/com/google/android/gms/play-services-cronet/18.0.1/play-services-cronet-18.0.1.pom (and the -base, -basement, -tasks, -appset and -ads-identifier POMs at the versions in row E)
- https://dl.google.com/dl/android/maven2/org/chromium/net/cronet-api/143.7445.0/cronet-api-143.7445.0.pom (and cronet-shared, cronet-common, cronet-fallback and httpengine-native-provider at 143.7445.0)
- https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences-external-protobuf/1.2.1/datastore-preferences-external-protobuf-1.2.1.pom
- https://repo1.maven.org/maven2/com/google/protobuf/protobuf-javalite/3.22.3/protobuf-javalite-3.22.3.pom and https://repo1.maven.org/maven2/com/google/protobuf/protobuf-parent/3.22.3/protobuf-parent-3.22.3.pom
- https://repo1.maven.org/maven2/org/checkerframework/checker-qual/3.33.0/checker-qual-3.33.0.pom
- https://repo1.maven.org/maven2/com/squareup/okhttp3/okhttp/4.11.0/okhttp-4.11.0.pom
- https://repo1.maven.org/maven2/com/google/guava/guava-parent/32.0.0-jre/guava-parent-32.0.0-jre.pom
- https://storage.googleapis.com/chromium-cronet/android/143.0.7445.0/Release/cronet/LICENSE
- https://raw.githubusercontent.com/chromium/chromium/143.0.7445.0/LICENSE
- https://raw.githubusercontent.com/protocolbuffers/protobuf/v22.3/LICENSE
- https://raw.githubusercontent.com/typetools/checker-framework/checker-framework-3.33.0/checker-qual/LICENSE.txt
- https://raw.githubusercontent.com/square/okhttp/parent-4.11.0/okhttp/src/main/resources/okhttp3/internal/publicsuffix/NOTICE
- https://publicsuffix.org/list/public_suffix_list.dat
- https://www.mozilla.org/media/MPL/2.0/index.txt
- https://developer.android.com/studio/terms
- https://developer.android.com/guide/playcore
- https://developers.google.com/admob/terms
- https://developers.google.com/android/guides/opensource
- https://developer.android.com/reference/com/google/android/play/core/hsdp/service/package-summary
- The Apache-family POMs at https://repo1.maven.org/maven2/ for each coordinate in the Apache table.
