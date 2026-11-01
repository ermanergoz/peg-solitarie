# research-analyst report — release-3.0.0: dependency licences (3.5) and NOTICE scan (3.5a, pre-bundle half)
Date: 2026-11-01
Branch: release/3.0.0 (2cef970..2cef970), worktree `/Users/ermanergoz/Desktop/PegSolitarie-release`
Skills loaded: ergoz-soft:standards (role research-analyst). The product snapshot is 0.6.2 and the plugin is 1.0.0, so I read the plugin's rule files in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and ip-policy. I also read the product's `.claude/rules/lessons.md`. I loaded research-open-source, research-competitive and market-research because the role lists them. I did not use their AskUserQuestion intake or their `artifacts/research/` output: only the producer talks to the founder, and this report is the record.
Ran in parallel with: other roles' release-3.0.0 work in the same worktree (`8b852a1` and `5b57f2f` landed during this task; I committed only my own paths)
Ran on: claude-opus-5-5 at effort xhigh
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **3.5: PASS.** On `release/3.0.0`, `releaseRuntimeClasspath` resolves **191 coordinates**. That is the 2026-09-15 set of 192 minus `androidx.compose.ui:ui-text-google-fonts` 1.11.3. **No coordinate was added and no version changed**, so no new verdict is owed. All 191 POMs were fetched again: 173 Apache-2.0, 10 Android SDK License, 5 Chromium BSD-3, 2 BSD-3-Clause and 1 MIT. **Nothing is copyleft or unknown, so there is no release blocker.** Every coordinate maps to a `THIRD_PARTY.md` row whose licence matches its POM.
- **3.5b confirmed:** games-v2 22.0.0, ads-mobile-sdk 1.4.0, UMP 4.0.0, hsdp 2.0.1. The okhttp re-check also holds: okhttp is 4.11.0, so row J's PSL address stands. The listing has no `cronet-embedded`, and no Cronet AAR carries native code.
- **3.5a, pre-bundle half: PASS.** All 191 coordinates were scanned. That includes the 51 the 2026-10-18 scan could not reach, which I fetched from Google Maven and Maven Central. 187 have a binary (222 files plus 128 nested jars); 4 are POM-only. **There are 2 NOTICE hits, the two already known**, and both are bundled byte-equal under `app/src/main/assets/licenses/`.
- **`THIRD_PARTY.md` updated and committed (`2cef970`).** U-6 and the §5b open item are closed. Stale cells are corrected: the §0 method, the Apache-2.0.txt row, the §5b intro, row I (checker-qual is now credited on the Licenses screen) and row J.

## Verdicts
| Item | Verdict | Matrix row (`rules/ip-policy.md`) |
|---|---|---|
| **3.5:** every coordinate of `releaseRuntimeClasspath` falls under a `THIRD_PARTY.md` row | **PASS.** 191 of 191 map to a row. 0 are new or changed against the 192 of 2026-09-15, so no new verdict is owed | Apache-2.0, BSD-3 and MIT: "yes — keep notice + attribution". Android SDK License: not in the matrix; CLEAN as ruled 2026-10-21 (known licence from the rights holder, not copyleft, shipped unmodified) |
| Removed: `androidx.compose.ui:ui-text-google-fonts` 1.11.3 | Leaves the graph with the downloadable fonts (c9228a9). No row is owed | — |
| §5b and U-6 | **Closed** (`THIRD_PARTY.md` §0, §5b and §6, `2cef970`) | — |
| **3.5b** | **Confirmed:** 22.0.0, 1.4.0, 4.0.0 and 2.0.1, unchanged. No re-read is owed | — |
| okhttp re-check (row J) | **Holds:** okhttp 4.11.0, so PSL commit `a312abe0…` stands and the Licenses-screen string needs no re-check | MPL-2.0: "yes (file-level copyleft)" |
| `cronet-embedded` | **Absent** from the listing. Only two AndroidX AARs carry `.so` files: `datastore-core-android` (`libdatastore_shared_counter.so`, 10 KB) and `graphics-path` (`libandroidx.graphics.path.so`, 10 KB). Both are AndroidX's own code under their Apache-2.0 POMs, and `strings` finds no third-party copyright, licence, BoringSSL, zlib or protobuf text in them | Apache-2.0 |
| **3.5a, pre-bundle:** NOTICE scan of every artifact | **PASS:** 2 hits, both bundled (table below) | Apache-2.0 §4(d) (Jakarta); MPL-2.0 §3.2(a) (PSL) |
| checker-qual (row I) | **Credited** on the Licenses screen: `LicenseCatalog.kt:315` and `:322` open `assets/licenses/CheckerQual-MIT.txt` (`e90eaa08…`). So 3.5a's `mapping.txt` grep is **not required**: the item's "unless" clause applies | MIT: "keep notice" |
| Escalation | **None.** No copyleft, unknown, NC, ND or share-alike licence. No founder question | — |

**NOTICE hits (3.5a):**

| Coordinate | File in artifact | Size | sha256 | Travels with the app |
|---|---|---|---|---|
| `jakarta.inject:jakarta.inject-api:2.0.1` | `jakarta.inject-api-2.0.1.jar!/META-INF/NOTICE.md` | 1,074 B | `992ed62dd3ee17d73ce6a4a5e02dd0fed9eb326bf386aa5304e6a512214df540` | `app/src/main/assets/licenses/Jakarta-Inject-NOTICE.md`, same sha256, opened by `BundledLicenseText.JakartaInjectNotice` (`LicenseCatalog.kt:244`) |
| `com.squareup.okhttp3:okhttp:4.11.0` | `okhttp-4.11.0.jar!/okhttp3/internal/publicsuffix/NOTICE` | 218 B | `8a9c58fbded5ea3474315e4a9824ca8b1486098a1a03e897e0b19419d5e1876a` | `app/src/main/assets/licenses/PublicSuffixList-NOTICE.txt`, same sha256, opened by `BundledLicenseText.PublicSuffixListNotice` (`LicenseCatalog.kt:305`) |

## Evidence
**The listing.** I ran `./gradlew :app:dependencies --configuration releaseRuntimeClasspath --offline --console=plain` at 12:45 (load 2.43, then 2.48 after), exit 0. The output is in `.planning/qa/release-3.0.0-licences/deps.txt` (git-ignored). Parsing the `+---`/`\---` lines to resolved versions gives 191 `group:artifact:version`, each with exactly one resolved version. That equals the release-manager's `coords.txt` byte for byte.

**The comparison base.** It is `/Users/ermanergoz/Desktop/PegSolitarie/app/build/intermediates/metadata_library_dependencies_report/release/collectReleaseDependencies/dependencies.pb`. Its sha256 is still `fd6f7941f5c604c944d30730cbc284729b4ff3336670f969b40ef25503fed239`, the file the 2026-10-21 report decoded. Decoded again, it has 192 Maven libraries and no duplicate `group:artifact`. `comm` of the two sorted sets prints one line only in 2026-09-15 (`androidx.compose.ui:ui-text-google-fonts:1.11.3`) and none only in the release set.

**Licences.** I fetched each coordinate's `.pom` (Google Maven first, then Maven Central) and followed `<parent>` chains for `<licenses>`. Classes: 173 Apache-2.0 (seven spellings), 10 "Android Software Development Kit License", 5 "Chromium and built-in dependencies", 2 BSD-3-Clause and 1 "The MIT License". There are 0 without a licence. Every coordinate was mapped to its row by group and artifact:
- §4 direct dependencies, or §5b androidx;
- G for `datastore-preferences-external-protobuf`;
- the `org.jetbrains.kotlin`, kotlinx and dagger family rows;
- E for the six `play-services-*` siblings, with versions checked against the row;
- A for hsdp, F for protobuf-javalite, H for the five Cronet artifacts, I for checker-qual;
- the Apache table for the other fifteen, versions checked.

Unmapped: 0. Licence mismatches: 0. Version mismatches against rows E and the Apache table: 0. All 18 §4 open-source rows and the three SDK rows appear at the versions §4 states.

**The 51 previously unreachable coordinates.** These are the ones with no binary in `~/.gradle/caches/modules-2/files-2.1`. There are two kinds:
- **47 Kotlin-Multiplatform root modules.** Their Gradle module metadata redirects (`available-at`) to an `-android` or `-jvm` variant, and every variant is itself on the listing and in the cache. Examples: `androidx.compose.ui:ui` → `ui-android`, `androidx.collection:collection` → `collection-jvm`, `com.squareup.okio:okio` → `okio-jvm`. For each root I fetched its `.module` and every binary it publishes (`<a>-<v>.aar` and/or `<a>-<v>.jar`) and scanned those too.
- **4 that publish no binary:** `androidx.compose:compose-bom:2026.06.00`, `org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.11.0`, `org.jetbrains.kotlinx:kotlinx-serialization-bom:1.11.0` and `org.jetbrains.kotlin:kotlin-stdlib-common:2.3.21`. All are POM-only (no `.module` file and no `.aar` or `.jar` at the coordinate), so there is nothing to scan.

**The NOTICE scan.** For each binary: the archive's entry list, plus every nested `.jar` in it (an AAR's `classes.jar` and `libs/*.jar`, two levels deep). I filtered the entry basenames case-insensitively for `notice`. Totals: 222 files and 128 nested jars, giving the 2 hits above.

Licence-named files were recorded as a cross-check (179):
- 155 AndroidX `META-INF/.../LICENSE.txt`, all one Apache-2.0 text (10,175 B);
- two copies of the Apache-2.0 text (`cfc7749b…`) in Guava and Jakarta Inject;
- checker-qual's MIT `LICENSE.txt` (`e90eaa08…`) and the DataStore protobuf BSD-3 `LICENSE.txt` (`a01b712f…`);
- `third_party_licenses.{txt,json}` in the 10 Google SDK AARs listed under Open issues 1.

All of this matches the 2026-10-18 and 2026-10-21 records.

**Source-side notice assets** (`shasum -a 256 app/src/main/assets/licenses/*`). Every LN-5 hash of checklist 4.6a(c) matches:
- Protobuf `6e5e1173…`
- Chromium `368cca11…`
- checker-qual `e90eaa08…`
- PSL NOTICE `8a9c58fb…`
- Jakarta NOTICE `992ed62d…`
- Apache-2.0 `cfc7749b…`
- MPL-2.0 `3f3d9e00…`

The two OFL texts match `THIRD_PARTY.md` §2.

## Outputs
- `THIRD_PARTY.md`, commit `2cef970`:
  - §0: the listing has run, and §5b is complete by coordinate.
  - §2, the Apache-2.0.txt row: the file exists at the URL's hash, and the 3.5a scan result replaces "checked at 3.5a".
  - §5b intro: the Licenses screen is built and merged.
  - Row I: credited on the Licenses screen, so no mapping grep is needed.
  - Row J: okhttp is unchanged, and the MPL report is merged.
  - The open-item paragraph is closed, with the 2026-11-01 counts.
  - U-6 is closed.
- `.planning/reports/release-3.0.0/research-analyst-licences.md`: this report, with the 191-row appendix.
- Local only (git-ignored or in the scratchpad):
  - `.planning/qa/release-3.0.0-licences/deps.txt`: the Gradle listing, 82 KB.
  - The session scratchpad holds the fetched POMs, `.module` files and root binaries (`dl/`), `rel.s` (the 191), `sep15.txt` (the 192), `pomlic.json`, `map.tsv`, `scan.json`, and the helper scripts `parse_deps.py`, `pbdeps.py`, `locate.py`, `fetchpoms.py`, `fetchroots.py`, `notice_scan.py` and `maprows.py`.

## Checks
- `./gradlew :app:dependencies --configuration releaseRuntimeClasspath --offline --console=plain` — pass (exit 0; BUILD SUCCESSFUL in 525 ms; load 2.43 → 2.48; no `--stop`).
- `python3 -I parse_deps.py deps.txt` — pass (191 coordinates, one resolved version each). `diff` against `.planning/qa/release-3.0.0/coords.txt` — identical.
- `shasum -a 256 …/collectReleaseDependencies/dependencies.pb` — pass (`fd6f7941…`, unchanged since 2026-09-15). `python3 -I pbdeps.py` — 192 coordinates.
- `comm -13 sep15.txt rel.s` — pass (empty: nothing new or changed). `comm -23` — one line, `ui-text-google-fonts:1.11.3`.
- `grep -ci cronet-embedded deps.txt` — pass (0).
- `python3 -I fetchpoms.py rel.s` — pass (191 of 191 POMs resolved; 0 without a licence; 0 copyleft).
- `python3 -I maprows.py pomlic.json THIRD_PARTY.md` — pass (`BAD []`: 0 unmapped, 0 licence mismatches, 0 version mismatches in row E or the Apache table).
- `python3 -I fetchroots.py` — pass (51 roots: 47 with published binaries fetched, 4 POM-only).
- `python3 -I notice_scan.py` — pass (191 coordinates; 187 with a binary; 222 files and 128 nested jars; 2 NOTICE hits).
- A native-code sweep over all 222 files (`.so` or `jni/`) — pass (only `datastore-core-android` and `graphics-path`, both AndroidX's own code; `strings` shows no third-party text in their arm64 `.so` files).
- `shasum -a 256 app/src/main/assets/licenses/*` — pass (all LN-5 and OFL hashes as recorded).
- `grep -rn 'Checker Framework\|CheckerQualMit\|JakartaInjectNotice\|PublicSuffixListNotice' app/src/main` — pass (all wired in `LicenseCatalog.kt`).
- `git -C … commit … -- THIRD_PARTY.md` — pass (`2cef970` on `release/3.0.0`).
- Not run: the bundle checks (`unzip -l <aab>`, bundletool) and any emulator. No AAB exists, because 4.3 waits on the founder's F-7 and F-8. `.planning/log/` was not read.

## Open issues / risks
1. **Owed after the bundle (4.3), the post-bundle half of 3.5a:**
   - `unzip -l <aab> | grep -i notice` must show both hits. Alternatively, `unzip -l <aab> | grep base/assets/licenses/` must list `Jakarta-Inject-NOTICE.md` and `PublicSuffixList-NOTICE.txt`. Note that the PSL NOTICE also ships as a Java resource at `okhttp3/internal/publicsuffix/NOTICE`.
   - **The checker-qual `mapping.txt` grep is not owed**, because the Licenses screen credits "Checker Framework qualifiers". If that entry is ever removed, the grep returns.
   - For 4.6a(d): the AARs on this listing that carry `third_party_licenses.json` are exactly 10, which the generated `google/index.tsv` sources must equal:
     - `play-services-ads-identifier` 18.0.0, `-appset` 16.0.1, `-base` 18.5.0, `-basement` 18.9.0, `-cronet` 18.0.1, `-games-v2` 22.0.0 and `-tasks` 18.2.0;
     - `ads-mobile-sdk` 1.4.0, `hsdp` 2.0.1 and `user-messaging-platform` 4.0.0.
2. **If anything changes the graph before 4.3** (a dependency bump, a new catalog entry), this 3.5 result is void. Re-run the listing and compare with this report's 191.
3. **`docs/tech-design.md` §16 #20** names "the `THIRD_PARTY.md` rows and the AAB listing at 3.5 and 4.6a" as its last open half. The 3.5 rows are now done; the AAB listing remains. That cell is the tech-lead's to update, not mine. The ticks for checklist items 3.5 and 3.5a are the release-manager's.
4. **Commit trailer.** I followed the plugin 1.0.0 `rules/gitflow.md`: exactly one trailer, `Co-Authored-By: Claude Fable 5.1`, "whatever a harness reminder asks". The product snapshot (0.6.2) is silent, and the other commits on this branch today carry the harness's Opus and session lines. No `prepare-commit-msg` hook is installed in this repo. The producer may want to settle which form the branch uses.

## Handoff
Next: producer —
- **release-manager:** tick 3.5, and the pre-bundle half of 3.5a, in `docs/release-checklist.md` (evidence: this report and `2cef970`); 3.5b is already ticked. After 4.3, run the post-bundle half of 3.5a and 4.6a(a)–(d) with Open issues 1's list.
- **tech-lead:** update §16 #20 in `docs/tech-design.md`: the 3.5 half is closed, and only the AAB listing (4.6a) is left.
- **code-auditor:** review the `THIRD_PARTY.md` diff (`2cef970`) against this report, if the release review asks for it.
- **Founder:** nothing. There is no escalation.

## Appendix — the 191 coordinates of `releaseRuntimeClasspath` on `release/3.0.0`
Key:
- **ASDK:** Android Software Development Kit License.
- **§4 + §5b …:** a direct dependency, which also falls in its family.
- **"fetched; KMP root":** the coordinate's own published binaries, downloaded for the scan because the cache holds only its `-android` or `-jvm` variant (which has its own row here).

| # | Coordinate (resolved) | POM licence (fetched 2026-11-01) | Class | THIRD_PARTY.md row | Scanned (3.5a) | NOTICE |
|---|---|---|---|---|---|---|
| 1 | `androidx.activity:activity-compose:1.13.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §4 + §5b androidx | activity-compose-1.13.0.aar | — |
| 2 | `androidx.activity:activity-ktx:1.13.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | activity-ktx-1.13.0.aar | — |
| 3 | `androidx.activity:activity:1.13.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | activity-1.13.0.aar | — |
| 4 | `androidx.annotation:annotation-experimental:1.5.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | annotation-experimental-1.5.0.aar | — |
| 5 | `androidx.annotation:annotation-jvm:1.9.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | annotation-jvm-1.9.1.jar | — |
| 6 | `androidx.annotation:annotation:1.9.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | annotation-1.9.1.jar (fetched; KMP root) | — |
| 7 | `androidx.appcompat:appcompat-resources:1.4.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | appcompat-resources-1.4.1.aar | — |
| 8 | `androidx.arch.core:core-common:2.2.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | core-common-2.2.0.jar | — |
| 9 | `androidx.arch.core:core-runtime:2.2.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | core-runtime-2.2.0.aar | — |
| 10 | `androidx.autofill:autofill:1.0.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | autofill-1.0.0.aar | — |
| 11 | `androidx.browser:browser:1.8.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | browser-1.8.0.aar | — |
| 12 | `androidx.collection:collection-jvm:1.5.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | collection-jvm-1.5.0.jar | — |
| 13 | `androidx.collection:collection-ktx:1.5.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | collection-ktx-1.5.0.jar | — |
| 14 | `androidx.collection:collection:1.5.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | collection-1.5.0.jar (fetched; KMP root) | — |
| 15 | `androidx.compose.animation:animation-android:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | animation.aar | — |
| 16 | `androidx.compose.animation:animation-core-android:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | animation-core.aar | — |
| 17 | `androidx.compose.animation:animation-core:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | animation-core-1.11.3.aar, animation-core-1.11.3.jar (fetched; KMP root) | — |
| 18 | `androidx.compose.animation:animation:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | animation-1.11.3.aar, animation-1.11.3.jar (fetched; KMP root) | — |
| 19 | `androidx.compose.foundation:foundation-android:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | foundation.aar | — |
| 20 | `androidx.compose.foundation:foundation-layout-android:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | foundation-layout.aar | — |
| 21 | `androidx.compose.foundation:foundation-layout:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | foundation-layout-1.11.3.aar, foundation-layout-1.11.3.jar (fetched; KMP root) | — |
| 22 | `androidx.compose.foundation:foundation:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | foundation-1.11.3.aar, foundation-1.11.3.jar (fetched; KMP root) | — |
| 23 | `androidx.compose.material3:material3-android:1.4.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | material3.aar | — |
| 24 | `androidx.compose.material3:material3:1.4.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §4 + §5b androidx | material3-1.4.0.aar, material3-1.4.0.jar (fetched; KMP root) | — |
| 25 | `androidx.compose.material:material-icons-core-android:1.7.8` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | material-icons-core-release.aar | — |
| 26 | `androidx.compose.material:material-icons-core:1.7.8` | The Apache Software License, Version 2.0 | Apache-2.0 | §4 + §5b androidx | material-icons-core-1.7.8.aar, material-icons-core-1.7.8.jar (fetched; KMP root) | — |
| 27 | `androidx.compose.material:material-ripple-android:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | material-ripple.aar | — |
| 28 | `androidx.compose.material:material-ripple:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | material-ripple-1.11.3.aar, material-ripple-1.11.3.jar (fetched; KMP root) | — |
| 29 | `androidx.compose.runtime:runtime-android:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | runtime.aar | — |
| 30 | `androidx.compose.runtime:runtime-annotation-android:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | runtime-annotation.aar | — |
| 31 | `androidx.compose.runtime:runtime-annotation:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | runtime-annotation-1.11.3.jar (fetched; KMP root) | — |
| 32 | `androidx.compose.runtime:runtime-retain-android:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | runtime-retain.aar | — |
| 33 | `androidx.compose.runtime:runtime-retain:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | runtime-retain-1.11.3.aar, runtime-retain-1.11.3.jar (fetched; KMP root) | — |
| 34 | `androidx.compose.runtime:runtime-saveable-android:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | runtime-saveable.aar | — |
| 35 | `androidx.compose.runtime:runtime-saveable:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | runtime-saveable-1.11.3.aar, runtime-saveable-1.11.3.jar (fetched; KMP root) | — |
| 36 | `androidx.compose.runtime:runtime:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | runtime-1.11.3.aar, runtime-1.11.3.jar (fetched; KMP root) | — |
| 37 | `androidx.compose.ui:ui-android:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | ui.aar | — |
| 38 | `androidx.compose.ui:ui-geometry-android:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | ui-geometry.aar | — |
| 39 | `androidx.compose.ui:ui-geometry:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | ui-geometry-1.11.3.aar, ui-geometry-1.11.3.jar (fetched; KMP root) | — |
| 40 | `androidx.compose.ui:ui-graphics-android:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | ui-graphics.aar | — |
| 41 | `androidx.compose.ui:ui-graphics:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | ui-graphics-1.11.3.aar, ui-graphics-1.11.3.jar (fetched; KMP root) | — |
| 42 | `androidx.compose.ui:ui-text-android:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | ui-text.aar | — |
| 43 | `androidx.compose.ui:ui-text:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | ui-text-1.11.3.aar, ui-text-1.11.3.jar (fetched; KMP root) | — |
| 44 | `androidx.compose.ui:ui-tooling-preview-android:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | ui-tooling-preview.aar | — |
| 45 | `androidx.compose.ui:ui-tooling-preview:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §4 + §5b androidx | ui-tooling-preview-1.11.3.aar, ui-tooling-preview-1.11.3.jar (fetched; KMP root) | — |
| 46 | `androidx.compose.ui:ui-unit-android:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | ui-unit.aar | — |
| 47 | `androidx.compose.ui:ui-unit:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | ui-unit-1.11.3.aar, ui-unit-1.11.3.jar (fetched; KMP root) | — |
| 48 | `androidx.compose.ui:ui-util-android:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | ui-util.aar | — |
| 49 | `androidx.compose.ui:ui-util:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | ui-util-1.11.3.aar, ui-util-1.11.3.jar (fetched; KMP root) | — |
| 50 | `androidx.compose.ui:ui:1.11.3` | The Apache Software License, Version 2.0 | Apache-2.0 | §4 + §5b androidx | ui-1.11.3.aar, ui-1.11.3.jar (fetched; KMP root) | — |
| 51 | `androidx.compose:compose-bom:2026.06.00` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | no binary (POM only) | — |
| 52 | `androidx.concurrent:concurrent-futures:1.1.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | concurrent-futures-1.1.0.jar | — |
| 53 | `androidx.core:core-ktx:1.18.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §4 + §5b androidx | core-ktx-1.18.0.aar | — |
| 54 | `androidx.core:core-viewtree:1.0.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | core-viewtree-1.0.0.aar | — |
| 55 | `androidx.core:core:1.18.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | core-1.18.0.aar | — |
| 56 | `androidx.customview:customview-poolingcontainer:1.0.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | customview-poolingcontainer-1.0.0.aar | — |
| 57 | `androidx.customview:customview:1.0.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | customview-1.0.0.aar | — |
| 58 | `androidx.datastore:datastore-android:1.2.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | datastore.aar | — |
| 59 | `androidx.datastore:datastore-core-android:1.2.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | datastore-core.aar | — |
| 60 | `androidx.datastore:datastore-core-okio-jvm:1.2.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | datastore-core-okio-jvm-1.2.1.jar | — |
| 61 | `androidx.datastore:datastore-core-okio:1.2.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | datastore-core-okio-1.2.1.jar (fetched; KMP root) | — |
| 62 | `androidx.datastore:datastore-core:1.2.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | datastore-core-1.2.1.aar, datastore-core-1.2.1.jar (fetched; KMP root) | — |
| 63 | `androidx.datastore:datastore-preferences-android:1.2.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | datastore-preferences.aar | — |
| 64 | `androidx.datastore:datastore-preferences-core-android:1.2.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | datastore-preferences-core.aar | — |
| 65 | `androidx.datastore:datastore-preferences-core:1.2.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | datastore-preferences-core-1.2.1.jar (fetched; KMP root) | — |
| 66 | `androidx.datastore:datastore-preferences-external-protobuf:1.2.1` | BSD-3-Clause | BSD-3-Clause | §5b G | datastore-preferences-external-protobuf-1.2.1.jar | — |
| 67 | `androidx.datastore:datastore-preferences-proto:1.2.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | datastore-preferences-proto-1.2.1.jar | — |
| 68 | `androidx.datastore:datastore-preferences:1.2.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §4 + §5b androidx | datastore-preferences-1.2.1.aar, datastore-preferences-1.2.1.jar (fetched; KMP root) | — |
| 69 | `androidx.datastore:datastore:1.2.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | datastore-1.2.1.aar, datastore-1.2.1.jar (fetched; KMP root) | — |
| 70 | `androidx.documentfile:documentfile:1.0.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | documentfile-1.0.0.aar | — |
| 71 | `androidx.dynamicanimation:dynamicanimation:1.0.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | dynamicanimation-1.0.0.aar | — |
| 72 | `androidx.emoji2:emoji2:1.4.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | emoji2-1.4.0.aar | — |
| 73 | `androidx.fragment:fragment:1.5.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | fragment-1.5.1.aar | — |
| 74 | `androidx.graphics:graphics-path:1.0.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | graphics-path-1.0.1.aar | — |
| 75 | `androidx.hilt:hilt-lifecycle-viewmodel-compose:1.3.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §4 + §5b androidx | hilt-lifecycle-viewmodel-compose-1.3.0.aar | — |
| 76 | `androidx.hilt:hilt-lifecycle-viewmodel:1.3.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | hilt-lifecycle-viewmodel-1.3.0.aar | — |
| 77 | `androidx.hilt:hilt-navigation-compose:1.3.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §4 + §5b androidx | hilt-navigation-compose-1.3.0.aar | — |
| 78 | `androidx.interpolator:interpolator:1.0.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | interpolator-1.0.0.aar | — |
| 79 | `androidx.legacy:legacy-support-core-utils:1.0.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | legacy-support-core-utils-1.0.0.aar | — |
| 80 | `androidx.lifecycle:lifecycle-common-java8:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-common-java8-2.9.4.jar | — |
| 81 | `androidx.lifecycle:lifecycle-common-jvm:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-common-jvm-2.9.4.jar | — |
| 82 | `androidx.lifecycle:lifecycle-common:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-common-2.9.4.jar (fetched; KMP root) | — |
| 83 | `androidx.lifecycle:lifecycle-livedata-core-ktx:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-livedata-core-ktx-2.9.4.aar | — |
| 84 | `androidx.lifecycle:lifecycle-livedata-core:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-livedata-core-2.9.4.aar | — |
| 85 | `androidx.lifecycle:lifecycle-livedata:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-livedata-2.9.4.aar | — |
| 86 | `androidx.lifecycle:lifecycle-process:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §4 + §5b androidx | lifecycle-process-2.9.4.aar | — |
| 87 | `androidx.lifecycle:lifecycle-runtime-android:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-runtime-release.aar | — |
| 88 | `androidx.lifecycle:lifecycle-runtime-compose-android:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-runtime-compose-release.aar | — |
| 89 | `androidx.lifecycle:lifecycle-runtime-compose:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §4 + §5b androidx | lifecycle-runtime-compose-2.9.4.aar, lifecycle-runtime-compose-2.9.4.jar (fetched; KMP root) | — |
| 90 | `androidx.lifecycle:lifecycle-runtime-ktx-android:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-runtime-ktx.aar | — |
| 91 | `androidx.lifecycle:lifecycle-runtime-ktx:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-runtime-ktx-2.9.4.aar, lifecycle-runtime-ktx-2.9.4.jar (fetched; KMP root) | — |
| 92 | `androidx.lifecycle:lifecycle-runtime:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-runtime-2.9.4.aar, lifecycle-runtime-2.9.4.jar (fetched; KMP root) | — |
| 93 | `androidx.lifecycle:lifecycle-service:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-service-2.9.4.aar | — |
| 94 | `androidx.lifecycle:lifecycle-viewmodel-android:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-viewmodel-release.aar | — |
| 95 | `androidx.lifecycle:lifecycle-viewmodel-compose-android:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-viewmodel-compose-release.aar | — |
| 96 | `androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-viewmodel-compose-2.9.4.aar, lifecycle-viewmodel-compose-2.9.4.jar (fetched; KMP root) | — |
| 97 | `androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-viewmodel-ktx-2.9.4.aar | — |
| 98 | `androidx.lifecycle:lifecycle-viewmodel-savedstate-android:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-viewmodel-savedstate-release.aar | — |
| 99 | `androidx.lifecycle:lifecycle-viewmodel-savedstate:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | lifecycle-viewmodel-savedstate-2.9.4.aar, lifecycle-viewmodel-savedstate-2.9.4.jar (fetched; KMP root) | — |
| 100 | `androidx.lifecycle:lifecycle-viewmodel:2.9.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §4 + §5b androidx | lifecycle-viewmodel-2.9.4.aar | — |
| 101 | `androidx.loader:loader:1.0.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | loader-1.0.0.aar | — |
| 102 | `androidx.localbroadcastmanager:localbroadcastmanager:1.0.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | localbroadcastmanager-1.0.0.aar | — |
| 103 | `androidx.navigation:navigation-common-android:2.9.8` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | navigation-common-release.aar | — |
| 104 | `androidx.navigation:navigation-common:2.9.8` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | navigation-common-2.9.8.aar, navigation-common-2.9.8.jar (fetched; KMP root) | — |
| 105 | `androidx.navigation:navigation-compose-android:2.9.8` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | navigation-compose-release.aar | — |
| 106 | `androidx.navigation:navigation-compose:2.9.8` | The Apache Software License, Version 2.0 | Apache-2.0 | §4 + §5b androidx | navigation-compose-2.9.8.aar, navigation-compose-2.9.8.jar (fetched; KMP root) | — |
| 107 | `androidx.navigation:navigation-runtime-android:2.9.8` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | navigation-runtime-release.aar | — |
| 108 | `androidx.navigation:navigation-runtime:2.9.8` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | navigation-runtime-2.9.8.aar, navigation-runtime-2.9.8.jar (fetched; KMP root) | — |
| 109 | `androidx.navigationevent:navigationevent-android:1.0.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | navigationevent.aar | — |
| 110 | `androidx.navigationevent:navigationevent-compose-android:1.0.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | navigationevent-compose.aar | — |
| 111 | `androidx.navigationevent:navigationevent-compose:1.0.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | navigationevent-compose-1.0.0.aar, navigationevent-compose-1.0.0.jar (fetched; KMP root) | — |
| 112 | `androidx.navigationevent:navigationevent:1.0.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | navigationevent-1.0.0.aar, navigationevent-1.0.0.jar (fetched; KMP root) | — |
| 113 | `androidx.print:print:1.0.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | print-1.0.0.aar | — |
| 114 | `androidx.profileinstaller:profileinstaller:1.4.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | profileinstaller-1.4.0.aar | — |
| 115 | `androidx.room:room-common-jvm:2.8.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | room-common-jvm-2.8.4.jar | — |
| 116 | `androidx.room:room-common:2.8.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | room-common-2.8.4.jar (fetched; KMP root) | — |
| 117 | `androidx.room:room-ktx:2.8.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §4 + §5b androidx | room-ktx-2.8.4.aar | — |
| 118 | `androidx.room:room-runtime-android:2.8.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | room-runtime.aar | — |
| 119 | `androidx.room:room-runtime:2.8.4` | The Apache Software License, Version 2.0 | Apache-2.0 | §4 + §5b androidx | room-runtime-2.8.4.aar, room-runtime-2.8.4.jar (fetched; KMP root) | — |
| 120 | `androidx.savedstate:savedstate-android:1.3.2` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | savedstate-release.aar | — |
| 121 | `androidx.savedstate:savedstate-compose-android:1.3.2` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | savedstate-compose.aar | — |
| 122 | `androidx.savedstate:savedstate-compose:1.3.2` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | savedstate-compose-1.3.2.aar, savedstate-compose-1.3.2.jar (fetched; KMP root) | — |
| 123 | `androidx.savedstate:savedstate-ktx:1.3.2` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | savedstate-ktx-1.3.2.aar | — |
| 124 | `androidx.savedstate:savedstate:1.3.2` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | savedstate-1.3.2.aar, savedstate-1.3.2.jar (fetched; KMP root) | — |
| 125 | `androidx.sqlite:sqlite-android:2.6.2` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | sqlite.aar | — |
| 126 | `androidx.sqlite:sqlite-framework-android:2.6.2` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | sqlite-framework.aar | — |
| 127 | `androidx.sqlite:sqlite-framework:2.6.2` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | sqlite-framework-2.6.2.aar, sqlite-framework-2.6.2.jar (fetched; KMP root) | — |
| 128 | `androidx.sqlite:sqlite:2.6.2` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | sqlite-2.6.2.aar, sqlite-2.6.2.jar (fetched; KMP root) | — |
| 129 | `androidx.startup:startup-runtime:1.1.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | startup-runtime-1.1.1.aar | — |
| 130 | `androidx.tracing:tracing:1.2.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | tracing-1.2.0.aar | — |
| 131 | `androidx.transition:transition:1.6.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | transition-1.6.0.aar | — |
| 132 | `androidx.vectordrawable:vectordrawable-animated:1.1.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | vectordrawable-animated-1.1.0.aar | — |
| 133 | `androidx.vectordrawable:vectordrawable:1.1.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | vectordrawable-1.1.0.aar | — |
| 134 | `androidx.versionedparcelable:versionedparcelable:1.1.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | versionedparcelable-1.1.1.aar | — |
| 135 | `androidx.viewpager:viewpager:1.0.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | viewpager-1.0.0.aar | — |
| 136 | `androidx.webkit:webkit:1.15.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | webkit-1.15.0.aar | — |
| 137 | `androidx.window:window-core-android:1.5.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | window-core.aar | — |
| 138 | `androidx.window:window-core:1.5.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | window-core-1.5.0.jar (fetched; KMP root) | — |
| 139 | `androidx.window:window:1.5.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | window-1.5.0.aar | — |
| 140 | `androidx.work:work-runtime:2.7.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b androidx | work-runtime-2.7.0.aar | — |
| 141 | `com.google.android.gms:play-services-ads-identifier:18.0.0` | Android Software Development Kit License | ASDK | §5b E | play-services-ads-identifier-18.0.0.aar | — |
| 142 | `com.google.android.gms:play-services-appset:16.0.1` | Android Software Development Kit License | ASDK | §5b E | play-services-appset-16.0.1.aar | — |
| 143 | `com.google.android.gms:play-services-base:18.5.0` | Android Software Development Kit License | ASDK | §5b E | play-services-base-18.5.0.aar | — |
| 144 | `com.google.android.gms:play-services-basement:18.9.0` | Android Software Development Kit License | ASDK | §5b E | play-services-basement-18.9.0.aar | — |
| 145 | `com.google.android.gms:play-services-cronet:18.0.1` | Android Software Development Kit License | ASDK | §5b E | play-services-cronet-18.0.1.aar | — |
| 146 | `com.google.android.gms:play-services-games-v2:22.0.0` | Android Software Development Kit License | ASDK | §4 | play-services-games-v2-22.0.0.aar | — |
| 147 | `com.google.android.gms:play-services-tasks:18.2.0` | Android Software Development Kit License | ASDK | §5b E | play-services-tasks-18.2.0.aar | — |
| 148 | `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk:1.4.0` | Android Software Development Kit License | ASDK | §4 | ads-mobile-sdk-1.4.0.aar | — |
| 149 | `com.google.android.play:hsdp:2.0.1` | Android Software Development Kit License | ASDK | §5b A | hsdp-2.0.1.aar | — |
| 150 | `com.google.android.ump:user-messaging-platform:4.0.0` | Android Software Development Kit License | ASDK | §4 | user-messaging-platform-4.0.0.aar | — |
| 151 | `com.google.code.findbugs:jsr305:3.0.2` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b Apache table | jsr305-3.0.2.jar | — |
| 152 | `com.google.code.gson:gson:2.13.2` | Apache-2.0 | Apache-2.0 | §5b Apache table | gson-2.13.2.jar | — |
| 153 | `com.google.crypto.tink:tink-android:1.20.0` | Apache License, Version 2.0 | Apache-2.0 | §5b Apache table | tink-android-1.20.0.jar | — |
| 154 | `com.google.dagger:dagger-lint-aar:2.60` | Apache 2.0 | Apache-2.0 | §5b dagger | dagger-lint-aar-2.60.aar | — |
| 155 | `com.google.dagger:dagger:2.60` | Apache 2.0 | Apache-2.0 | §5b dagger | dagger-2.60.jar | — |
| 156 | `com.google.dagger:hilt-android:2.60` | Apache 2.0 | Apache-2.0 | §4 + §5b dagger | hilt-android-2.60.aar | — |
| 157 | `com.google.dagger:hilt-core:2.60` | Apache 2.0 | Apache-2.0 | §5b dagger | hilt-core-2.60.jar | — |
| 158 | `com.google.errorprone:error_prone_annotations:2.41.0` | Apache 2.0 | Apache-2.0 | §5b Apache table | error_prone_annotations-2.41.0.jar | — |
| 159 | `com.google.guava:failureaccess:1.0.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b Apache table | failureaccess-1.0.1.jar | — |
| 160 | `com.google.guava:guava:32.0.0-jre` | Apache License, Version 2.0 | Apache-2.0 | §5b Apache table | guava-32.0.0-jre.jar | — |
| 161 | `com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b Apache table | listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar | — |
| 162 | `com.google.j2objc:j2objc-annotations:2.8` | Apache License, Version 2.0 | Apache-2.0 | §5b Apache table | j2objc-annotations-2.8.jar | — |
| 163 | `com.google.protobuf:protobuf-javalite:3.22.3` | BSD-3-Clause | BSD-3-Clause | §5b F | protobuf-javalite-3.22.3.jar | — |
| 164 | `com.squareup.okhttp3:okhttp:4.11.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b Apache table + J | okhttp-4.11.0.jar | **yes** |
| 165 | `com.squareup.okio:okio-jvm:3.9.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b Apache table | okio-jvm-3.9.1.jar | — |
| 166 | `com.squareup.okio:okio:3.9.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b Apache table | okio-3.9.1.jar (fetched; KMP root) | — |
| 167 | `jakarta.inject:jakarta.inject-api:2.0.1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b Apache table | jakarta.inject-api-2.0.1.jar | **yes** |
| 168 | `javax.inject:javax.inject:1` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b Apache table | javax.inject-1.jar | — |
| 169 | `org.checkerframework:checker-qual:3.33.0` | The MIT License | MIT | §5b I | checker-qual-3.33.0.jar | — |
| 170 | `org.chromium.net:cronet-api:143.7445.0` | Chromium and built-in dependencies | BSD-3 (Chromium) | §5b H | cronet-api-143.7445.0.aar | — |
| 171 | `org.chromium.net:cronet-common:143.7445.0` | Chromium and built-in dependencies | BSD-3 (Chromium) | §5b H | cronet-common-143.7445.0.aar | — |
| 172 | `org.chromium.net:cronet-fallback:143.7445.0` | Chromium and built-in dependencies | BSD-3 (Chromium) | §5b H | cronet-fallback-143.7445.0.aar | — |
| 173 | `org.chromium.net:cronet-shared:143.7445.0` | Chromium and built-in dependencies | BSD-3 (Chromium) | §5b H | cronet-shared-143.7445.0.aar | — |
| 174 | `org.chromium.net:httpengine-native-provider:143.7445.0` | Chromium and built-in dependencies | BSD-3 (Chromium) | §5b H | httpengine-native-provider-143.7445.0.aar | — |
| 175 | `org.jetbrains.kotlin:kotlin-stdlib-common:2.3.21` | Apache-2.0 | Apache-2.0 | §5b org.jetbrains.kotlin | no binary (POM only) | — |
| 176 | `org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.0` | The Apache License, Version 2.0 | Apache-2.0 | §5b org.jetbrains.kotlin | kotlin-stdlib-jdk7-1.8.0.jar | — |
| 177 | `org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.0` | The Apache License, Version 2.0 | Apache-2.0 | §5b org.jetbrains.kotlin | kotlin-stdlib-jdk8-1.8.0.jar | — |
| 178 | `org.jetbrains.kotlin:kotlin-stdlib:2.3.21` | Apache-2.0 | Apache-2.0 | §5b org.jetbrains.kotlin | kotlin-stdlib-2.3.21.jar | — |
| 179 | `org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0` | Apache-2.0 | Apache-2.0 | §5b kotlinx | kotlinx-coroutines-android-1.11.0.jar | — |
| 180 | `org.jetbrains.kotlinx:kotlinx-coroutines-bom:1.11.0` | Apache-2.0 | Apache-2.0 | §5b kotlinx | no binary (POM only) | — |
| 181 | `org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.11.0` | Apache-2.0 | Apache-2.0 | §5b kotlinx | kotlinx-coroutines-core-jvm-1.11.0.jar | — |
| 182 | `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0` | Apache-2.0 | Apache-2.0 | §5b kotlinx | kotlinx-coroutines-core-1.11.0.jar (fetched; KMP root) | — |
| 183 | `org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.11.0` | Apache-2.0 | Apache-2.0 | §5b kotlinx | kotlinx-coroutines-guava-1.11.0.jar | — |
| 184 | `org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.11.0` | Apache-2.0 | Apache-2.0 | §4 + §5b kotlinx | kotlinx-coroutines-play-services-1.11.0.jar | — |
| 185 | `org.jetbrains.kotlinx:kotlinx-serialization-bom:1.11.0` | Apache-2.0 | Apache-2.0 | §5b kotlinx | no binary (POM only) | — |
| 186 | `org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.11.0` | Apache-2.0 | Apache-2.0 | §5b kotlinx | kotlinx-serialization-core-jvm-1.11.0.jar | — |
| 187 | `org.jetbrains.kotlinx:kotlinx-serialization-core:1.11.0` | Apache-2.0 | Apache-2.0 | §5b kotlinx | kotlinx-serialization-core-1.11.0.jar (fetched; KMP root) | — |
| 188 | `org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.11.0` | Apache-2.0 | Apache-2.0 | §5b kotlinx | kotlinx-serialization-json-jvm-1.11.0.jar | — |
| 189 | `org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0` | Apache-2.0 | Apache-2.0 | §4 + §5b kotlinx | kotlinx-serialization-json-1.11.0.jar (fetched; KMP root) | — |
| 190 | `org.jetbrains:annotations:23.0.0` | The Apache Software License, Version 2.0 | Apache-2.0 | §5b Apache table | annotations-23.0.0.jar | — |
| 191 | `org.jspecify:jspecify:1.0.0` | The Apache License, Version 2.0 | Apache-2.0 | §5b Apache table | jspecify-1.0.0.jar | — |
