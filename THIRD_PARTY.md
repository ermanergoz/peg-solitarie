# THIRD_PARTY.md — Peg Solitaire

Every third-party idea, asset, code snippet and dependency that reaches a player's device, with
its licence. Required by `rules/ip-policy.md`; the studio's release gate **blocks a release when a
shipped row has no licence**, so an honest "unverified" row is a blocker by design, not an
oversight to be papered over.

Created on `feature/phase-14-release-docs` for v1.1 / product version **3.0.0** (release blocker
R-5, `.planning/reports/phase-14/tech-lead-signoff-r3-signing.md` §G). Re-checked at every release
branch cut — `docs/release-checklist.md` step 6.

---

## 0. How the shipped set was drawn

`gradle/libs.versions.toml` declares far more than this app ships: build plugins, annotation
processors, lint tooling and ten test libraries all live in the same catalogue. Transcribing the
catalogue would produce rows for software that never reaches a device, and — worse — would hide
the transitive artifacts that do. The line drawn here is **Gradle configuration**, read out of the
two module build files:

| Configuration | Ships? | Why |
|---|---|---|
| `implementation(...)` in `app/build.gradle.kts:465-537` | **yes** | on the release runtime classpath, packaged into the AAB |
| `implementation(project(":domain"))` | **yes** (own code) | `:domain` declares **no** non-test dependency at all (`domain/build.gradle.kts:7-10`) |
| `ksp(...)` — `hilt-compiler`, `room-compiler` | no | annotation processors; run at compile time, emit code, are not packaged |
| `compileOnly(...)` — `error_prone_annotations` | no | CLASS-retention annotations; the declaration's own comment records "nothing is added to the APK" (`app/build.gradle.kts:490-495`) |
| `testImplementation(...)` — JUnit, Truth, Turbine, MockK, coroutines-test, ArchUnit, Robolectric, Room testing, Compose ui-test-junit4 | no | JVM unit-test classpath only; never in a variant's runtime classpath |
| `debugImplementation(...)` — `ui-tooling`, `ui-test-manifest` | no | debug variant only; the release variant's source sets and classpath are disjoint from debug's (proved at class-output and minified-`classes.dex` level for the debug snapshot picker, `.planning/reports/phase-14/code-auditor-prerelease-fixes.md`) |
| `[plugins]` block + `build-logic/` classpath — AGP, Kotlin, KSP, Hilt, Room, detekt, ktlint Gradle plugins | no | build tooling; runs on the build machine |
| `platform(libs.androidx.compose.bom)` | no artifact of its own | a BOM is a version constraint; the Compose artifacts it governs are listed below |

**Transitive dependencies ship too**, and there are many more of them than direct ones (the local
Gradle cache holds 186 module groups, most of them transitive or build-only). They are covered
here by **family rows** in §5b rather than one row per coordinate: a family row states the licence
that every member of that publisher's family is published under, verified from POMs of members
actually on this app's graph. The authoritative per-coordinate enumeration is

```sh
./gradlew :app:dependencies --configuration releaseRuntimeClasspath
```

which is **not run in this document** (this branch is a documentation task under an explicit
"avoid Gradle" constraint). `docs/release-checklist.md` step 6 runs it on the release branch and
requires that every coordinate it prints falls under a family row below. Until that run happens,
§5b is licence-complete by publisher and *unenumerated by coordinate* — stated plainly rather than
implied.

**How a licence was established.** In order of strength: (1) the `<licenses>` block of the
artifact's own POM in the local Gradle module cache; (2) a `LICENSE`/`NOTICE` file inside the
artifact itself; (3) the project's own published terms. Each row names which. Where none of the
three could be reached from this machine, the row says **unverified** and names the single thing
that would settle it. No licence is guessed.

---

## 1. Ideas and mechanics

Not protected by copyright — inspiration is always allowed — but `rules/ip-policy.md` requires
them recorded anyway.

| Item | Source | Licence | What was taken | Credit |
|---|---|---|---|---|
| Peg solitaire as a game | Traditional; documented from the 17th century | Not protected — a game mechanic is an idea, not an expression | The rule set: orthogonal jump over an adjacent peg into an empty hole, jumped peg removed | Named as the game |
| The five Classic board footprints — English-33, European-37, Asymmetric-39, Diamond-41, Wiegleb-45 | Historical published board geometries (Wiegleb's 45-hole cross dates from 1779) | Not protected — board geometry is an idea | Hole layouts only; every coordinate table is authored in this repo (`domain/.../model`) | Board names in-game |
| Rules and board fidelity reference | The founder's own earlier peg-solitaire repository | Own work | Rule behaviour and board fidelity checked against it | — |

## 2. Assets shipped in the artifact

The only binary assets in the app are twelve sound files. There is **no raster image anywhere in
`app/src/main/res`** — every drawable and the launcher icon are vector XML authored in this repo
(`app/src/main/res/drawable/`, `mipmap-anydpi-v26/`).

| Item | Source | Licence | How verified | Credit |
|---|---|---|---|---|
| 9 of the 12 `res/raw` sound cues | Kenney, *Interface Sounds* 1.0 — https://kenney.nl/assets/interface-sounds | **CC0 1.0** | Publisher's published terms; per-file table in `docs/audio/CREDITS.md` | `docs/audio/CREDITS.md` |
| `sfx_star_pop.ogg` | freesound sound 531512 by CogFireStudios — https://freesound.org/people/CogFireStudios/sounds/531512/ | **CC0 1.0** | Licence stated on the sound's freesound page; recorded per-file | same |
| `sfx_select.ogg` | freesound sound 506546 by LilMati — https://freesound.org/people/LilMati/sounds/506546/ | **CC0 1.0** | same | same |
| `sfx_deselect.ogg` | Derived in-repo from sound 506546 (reversed, pitched down a whole tone) | **CC0 1.0** (derivative of a CC0 work) | same | same |
| Launcher icon, monochrome icon, cloud-save cover | Drawn in this repo as vector XML | Own work | — | — |
| Opening books (`domain/src/main/resources/golden/book/*.txt`), recorded solution fixtures, the Diamond-41 floor line | Authored in-house in phase 8 by beam search | Own work | `.planning/phases/08-assist-generator-quality-debt/08-BOOK-EVIDENCE.md` | — |
| Fredoka, Nunito | Google Fonts, delivered at runtime by the Google Play services downloadable-font provider — **not bundled in the artifact** | **OFL-1.1** (Google Fonts published terms) | Project's published terms | Listed here; no in-app licences screen exists — see §7 |

CC0 waives copyright and imposes no attribution condition, so the credits above are a courtesy and
an in-repo provenance record. `docs/audio/CREDITS.md` is the per-file authority; this file points
at it rather than duplicating it, because two copies of an asset table drift.

## 3. Code snippets copied into this repo

| Item | Source | Licence | How verified | Credit |
|---|---|---|---|---|
| `app/src/main/res/values/font_certs.xml` — the dev and prod certificate arrays for the Google Play services font provider | The standard certificate block published in the Android downloadable-fonts documentation (the file's own header records this) | **Apache-2.0** — developer.android.com sample code is published under Apache-2.0 | Project's published terms (Android developer site content licence). The certificates themselves are public key material, not a copyrightable work | This row |

## 4. Direct shipped dependencies

One row per coordinate declared at `implementation` in `app/build.gradle.kts`. Versions come from
`gradle/libs.versions.toml`; a version is not repeated here when the Compose BOM governs it.

| Coordinate | Version | Licence | How verified |
|---|---|---|---|
| `androidx.compose.material3:material3` | BOM 2026.06.00 | Apache-2.0 | POM `<licenses>` in the local Gradle cache |
| `androidx.compose.ui:ui` | BOM 2026.06.00 | Apache-2.0 | POM `<licenses>` |
| `androidx.compose.ui:ui-tooling-preview` | BOM 2026.06.00 | Apache-2.0 | AndroidX published terms (family row §5b) |
| `androidx.compose.material:material-icons-core` | BOM 2026.06.00 | Apache-2.0 | AndroidX published terms |
| `androidx.compose.ui:ui-text-google-fonts` | BOM 2026.06.00 | Apache-2.0 | POM `<licenses>` |
| `androidx.activity:activity-compose` | 1.13.0 | Apache-2.0 | POM `<licenses>` |
| `androidx.core:core-ktx` | 1.18.0 (forced) | Apache-2.0 | POM `<licenses>` |
| `androidx.lifecycle:lifecycle-viewmodel` | 2.9.4 | Apache-2.0 | AndroidX published terms |
| `androidx.lifecycle:lifecycle-runtime-compose` | 2.9.4 | Apache-2.0 | AndroidX published terms |
| `androidx.lifecycle:lifecycle-process` | 2.9.4 | Apache-2.0 | POM `<licenses>` |
| `androidx.navigation:navigation-compose` | 2.9.8 | Apache-2.0 | POM `<licenses>` |
| `androidx.datastore:datastore-preferences` | 1.2.1 | Apache-2.0 | AndroidX published terms (no POM for the `-android` variant in the local cache; Gradle module metadata only) |
| `androidx.room:room-runtime` | 2.8.4 | Apache-2.0 | `META-INF/androidx/room/room-runtime/LICENSE.txt` **inside the AAR** — full Apache 2.0 text |
| `androidx.room:room-ktx` | 2.8.4 | Apache-2.0 | AndroidX published terms |
| `androidx.hilt:hilt-navigation-compose` | 1.3.0 | Apache-2.0 | POM `<licenses>` |
| `com.google.dagger:hilt-android` | 2.60 | Apache-2.0 | POM `<licenses>` ("Apache 2.0") |
| `org.jetbrains.kotlinx:kotlinx-serialization-json` | 1.11.0 | Apache-2.0 | JetBrains published terms for kotlinx.serialization; no POM and no bundled licence file for the `-jvm` variant in the local cache |
| `org.jetbrains.kotlinx:kotlinx-coroutines-play-services` | 1.11.0 | Apache-2.0 | POM `<licenses>` ("Apache-2.0") |
| `com.google.android.gms:play-services-games-v2` | 22.0.0 | **Android Software Development Kit License Agreement** — proprietary, redistribution permitted as part of an application | POM `<licenses>`: *Android Software Development Kit License*, https://developer.android.com/studio/terms.html |
| `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk` (Google Mobile Ads, Next-Gen) | 1.4.0 | **Android Software Development Kit License Agreement** — proprietary, redistribution permitted as part of an application | POM `<licenses>`, same text as above |
| `com.google.android.ump:user-messaging-platform` | 4.0.0 | **unverified** — see §6 | No POM in the local Gradle cache; the AAR carries `third_party_licenses.txt` for its *dependencies* but no licence of its own |

**Not Apache-2.0, and that matters.** The three Google SDK coordinates above are *not* open source.
Recording them as Apache-2.0 because the rest of the table is Apache-2.0 would be a false row in
the exact document the release gate reads. They are permitted — their own terms allow
redistribution inside an application — and they are proprietary.

## 5. Transitive dependencies

### 5a. Method

Every artifact above pulls in more. Listed by publisher family; each family row states the licence
its members are published under and the evidence for it.

### 5b. Families on the release runtime classpath

| Family | Examples on this graph | Licence | How verified |
|---|---|---|---|
| `androidx.*` (Compose runtime/foundation/animation/graphics, annotation, collection, arch.core, savedstate, lifecycle-*, sqlite, startup, profileinstaller, emoji2, tracing, versionedparcelable …) | pulled by Compose, Room, DataStore, Navigation, Activity | Apache-2.0 | AndroidX is published under Apache-2.0; six member POMs read directly from the local cache all declare "The Apache Software License, Version 2.0", and `room-runtime` ships the full text inside its AAR |
| `org.jetbrains.kotlin:*` (kotlin-stdlib and friends) | required by every Kotlin artifact | Apache-2.0 | POM `<licenses>`: "Apache-2.0" |
| `org.jetbrains.kotlinx:*` (coroutines-core-jvm, coroutines-android, serialization-core-jvm, atomicfu) | coroutines and serialization runtimes | Apache-2.0 | `kotlinx-coroutines-play-services` POM declares Apache-2.0; the rest are siblings from the same publisher under the same published terms |
| `com.google.dagger:*` (dagger, dagger-lint-aar) | Hilt runtime | Apache-2.0 | POM `<licenses>` |
| `com.google.android.gms:*` (play-services-basement, -base, -tasks, -auth-*, and the Games v2 siblings) | Play Games v2 | Android Software Development Kit License Agreement — proprietary, redistribution permitted | the `play-services-games-v2` POM declares it; every artifact in this family is distributed under the same Android SDK terms |
| `com.google.guava:*`, `com.google.errorprone:*`, `org.checkerframework:*`, `javax.inject:*` (if present via Dagger/Play services) | small annotation/utility libraries | Apache-2.0 (checkerframework annotations: MIT) | Publishers' published terms — **not individually verified here**; the release-branch dependency listing (§0) is what confirms whether they are on the classpath at all |

**Open item, non-blocking for licence completeness, blocking for enumeration:** §5b has not been
checked against a real `releaseRuntimeClasspath` listing. Every family above is permissively or
permittedly licensed, so no *licence* risk hides in the gap; what hides in it is a coordinate from
a family nobody listed. `docs/release-checklist.md` step 6 closes it on the release branch.

## 6. Unverified — the release gate's open rows

| # | Item | Why unverified | What would settle it |
|---|---|---|---|
| U-1 | `com.google.android.ump:user-messaging-platform:4.0.0` | No POM in the local Gradle module cache (the artifact resolves through Gradle module metadata); the AAR's `third_party_licenses.txt` covers its dependencies, not itself. Its two sibling Google SDKs declare the Android SDK Licence, but that is an inference about a different artifact | Read the `<licenses>` block of `user-messaging-platform-4.0.0.pom` from `dl.google.com` / `maven.google.com`, **or** run `./gradlew :app:dependencies` once online and read the resolved module's metadata. One command, and it is on the release-branch checklist |
| U-2 | **The 20 achievement icons** (512×512, uploaded to the Play Console in phase 12) | They exist only in the Play Console. Nothing in this repository records their origin — there is no raster image in `app/src/main/res` at all | Founder recall — operator checklist Step 9. See §7 |
| U-3 | **The listing 512×512 icon** | Same — Console only | Founder recall — Step 9 |
| U-4 | **The 1024×500 feature graphic** | Same — Console only | Founder recall — Step 9 |
| U-5 | **Any store screenshot that is not a plain capture of this app** | Same — Console only | Founder recall — Step 9 |
| U-6 | Per-coordinate enumeration of §5b | The resolved release runtime classpath has not been listed (Gradle deliberately not run on this branch) | `./gradlew :app:dependencies --configuration releaseRuntimeClasspath`, on the release branch, output recorded in the release report |

## 7. PLACEHOLDER — store artwork provenance (needs the founder)

**These rows block the release.** `rules/ip-policy.md`: the release is blocked if a row lacks a
licence, and four of them do. This is operator checklist **Step 9** and founder question **Q-F3**.

| Item | Source | Licence | Credit |
|---|---|---|---|
| 20 achievement icons (512×512) | `PENDING FOUNDER — Q-F3` | `PENDING FOUNDER — Q-F3` | — |
| Listing icon (512×512) | `PENDING FOUNDER — Q-F3` | `PENDING FOUNDER — Q-F3` | — |
| Feature graphic (1024×500) | `PENDING FOUNDER — Q-F3` | `PENDING FOUNDER — Q-F3` | — |
| Store screenshots that are not plain app captures | `PENDING FOUNDER — Q-F3` | `PENDING FOUNDER — Q-F3` | — |

**Exactly what is needed, per group** (the studio writes the rows; do not paste URLs, account
details or Console identifiers into this file):

1. For the **20 achievement icons**, answer once for the set if they share an origin, otherwise
   per icon: which of — (a) *"I drew them"*, (b) *"generated with `<tool name>`"* — name the tool
   and, if the tool's terms turn on a plan or a date, which plan and roughly when, (c) *"bought or
   downloaded from `<source>` under `<licence>`"* — name both, (d) *"taken from `<a URL>`"* — the
   URL, so the studio can read the licence at that URL itself.
2. The same four-way answer for the **listing 512×512 icon**.
3. The same four-way answer for the **1024×500 feature graphic**.
4. Whether **any listing screenshot** contains artwork that is not simply a capture of this app
   (a device frame, a background, a logo, a font in an overlay) — and if so, the same answer for
   that element.
5. If any answer is (c) or (d) and the licence is **CC-BY** or **CC-BY-SA**, the studio also needs
   the author's name as the source requires it to be credited — CC-BY makes the credit an
   obligation, not a courtesy, and CC-BY-SA is an escalation under `rules/ip-policy.md`.

Two related items, also founder-only and recorded here so they are not rediscovered:

- **No in-app licences screen exists.** `rules/ip-policy.md` asks for attribution "in
  `THIRD_PARTY.md` **and** in the product's licences screen or page". Settings today has the two
  privacy rows (REL-05 / CONS-03) and no licences row. Every shipped asset is CC0 or OFL and every
  dependency is Apache-2.0 or a permitted Google SDK, so **nothing shipped carries an attribution
  obligation** and this is not a legal blocker for v1.1 — it is a studio-rule gap. **Routed, not
  fixed here:** a Settings licences row is a UI change and belongs to a developer with a
  ux-designer review; recorded for the v1.1.1 backlog.
- A **`SKILLS.md`** is named by `rules/skills-policy.md` and does not exist in this repo. No
  external skill has been installed into `.claude/skills/`, so there is nothing to record; noted
  so the absence is a fact rather than an omission.

## 8. Not shipped — recorded so the gate is not asked about them

For the avoidance of doubt, and so a reader does not re-derive §0: JUnit 4 (`junit:junit:4.13.2`,
**EPL-1.0**), Truth, Turbine, MockK, ArchUnit, Robolectric, `kotlinx-coroutines-test`,
`androidx.room:room-testing`, `androidx.compose.ui:ui-test-junit4`,
`androidx.compose.ui:ui-test-manifest`, `androidx.compose.ui:ui-tooling`,
`com.google.errorprone:error_prone_annotations` (compileOnly), the Hilt and Room KSP processors,
and every Gradle plugin in `[plugins]` are **test-, debug-, compile- or build-only**. None is in a
release artifact and none is distributed, so no notice or source obligation attaches to anything
that ships — including JUnit's EPL, which `rules/ip-policy.md` does not list and which needs no
escalation for exactly that reason.
