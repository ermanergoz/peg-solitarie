# THIRD_PARTY.md — Peg Solitaire

Every third-party idea, asset, code snippet and dependency that reaches a player's device, with
its licence. Required by `rules/ip-policy.md`; the studio's release gate **blocks a release when a
shipped row has no licence**, so an honest "unverified" row is a blocker by design, not an
oversight to be papered over.

Created on `feature/phase-14-release-docs` for v1.1 / product version **3.0.0** (release blocker
R-5, `.planning/reports/phase-14/tech-lead-signoff-r3-signing.md` §G). Re-checked at every release
branch cut — `docs/release-checklist.md` items 3.5 (the dependency listing) and 3.5a (the NOTICE scan).

**Rows describe what 3.0.0 will ship, not what `develop` holds today** (updated 2026-10-23). Three
changes are still on unmerged branches, and each affected row names its branch. The bundled
fonts were the third: `feature/phase-15-ux-refresh` merged into `develop` at c9228a9, so `develop`
now carries the five font files and their two OFL texts, and no longer carries `font_certs.xml` or
`ui-text-google-fonts` (checked 2026-10-21 against `develop` 0582e3f).

| Change | Branch today | What `develop` (0582e3f) still holds |
|---|---|---|
| Twelve sound cues as WAV; the wrong-tap and hint cues now rendered in-house instead of taken from the Kenney pack | `feature/audio-review` (d1c6e9b, af2b554; verdicts 3a1fce7) | the twelve-file `.ogg` set described by the earlier revision of this file |
| A glossy backdrop behind the Home and gameplay screens, drawn in-repo: direction B "Deep gloss" (Q-021, 2026-10-18; §1 idea row) | `feature/candy-backdrop` (**not yet built**: verdict, its direction-B addendum and the UX spec only) | no backdrop |
| An in-app Licenses screen (Settings › About) and its new `assets/licenses/Apache-2.0.txt` (§2 row), with the notices of §5b rows F to J, the Jakarta NOTICE and the Google SDK notices (LN-1 to LN-8) | `feature/licences-screen`, off `develop` (**not yet built**; `docs/tech-design.md` §3.6 and §14.1 LN. It left `feature/candy-backdrop` on 2026-10-18, `candy-backdrop/tech-lead-direction-b.md` A9, and brings `docs/ux/licences-screen.md` over in its first commit) | no licences screen, no Apache text |

If any of these branches does not reach `release/3.0.0`, its rows below are wrong for the release
and are re-cut at checklist item 2.4a.

---

## 0. How the shipped set was drawn

`gradle/libs.versions.toml` declares far more than this app ships: build plugins, annotation
processors, lint tooling and ten test libraries all live in the same catalogue. Transcribing the
catalogue would produce rows for software that never reaches a device, and — worse — would hide
the transitive artifacts that do. The line drawn here is **Gradle configuration**, read out of the
two module build files:

| Configuration | Ships? | Why |
|---|---|---|
| `implementation(...)` in `app/build.gradle.kts:478-547` (`develop` 0582e3f) | **yes** | on the release runtime classpath, packaged into the AAB |
| `implementation(project(":domain"))` | **yes** (own code) | `:domain` declares **no** non-test dependency at all (`domain/build.gradle.kts:7-10`) |
| `ksp(...)` — `hilt-compiler`, `room-compiler` | no | annotation processors; run at compile time, emit code, are not packaged |
| `compileOnly(...)` — `error_prone_annotations` | no | CLASS-retention annotations; the declaration's own comment records "nothing is added to the APK" (`app/build.gradle.kts:500-505`, `develop` 0582e3f) |
| `testImplementation(...)` — JUnit, Truth, Turbine, MockK, coroutines-test, ArchUnit, Robolectric, Room testing, Compose ui-test-junit4 | no | JVM unit-test classpath only; never in a variant's runtime classpath |
| `debugImplementation(...)` — `ui-tooling`, `ui-test-manifest` | no | debug variant only; the release variant's source sets and classpath are disjoint from debug's (proved at class-output and minified-`classes.dex` level for the debug snapshot picker, `.planning/reports/phase-14/code-auditor-prerelease-fixes.md`) |
| `[plugins]` block + `build-logic/` classpath — AGP, Kotlin, KSP, Hilt, Room, detekt, ktlint Gradle plugins; kover | no | build tooling; runs on the build machine |
| ↳ kover: Gradle plugin `org.jetbrains.kotlinx.kover` 0.9.11 (resolves `org.jetbrains.kotlinx:kover-gradle-plugin`, `kover-features-jvm`, `kover-jvm-agent` 0.9.11), JetBrains, https://github.com/Kotlin/kotlinx-kover | no | build-only: measures and verifies `:domain` line coverage (`koverVerify`, applied in `domain/build.gradle.kts`); its JVM agent instruments the unit-test JVM only, and nothing of it is on `releaseRuntimeClasspath` or in the AAB. **Licence: Apache-2.0** — POM `<licenses>` "The Apache Software License, Version 2.0" in all four 0.9.11 POMs in the local Gradle cache, and in https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kover-gradle-plugin/0.9.11/kover-gradle-plugin-0.9.11.pom (fetched 2026-10-24); the repository's own `LICENSE.TXT` at tag `v0.9.11` is the Apache License 2.0 text (https://github.com/Kotlin/kotlinx-kover/blob/v0.9.11/LICENSE.TXT). **Credit:** this row; not distributed, so no NOTICE or Licenses-screen entry is owed |
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
"avoid Gradle" constraint). `docs/release-checklist.md` item 3.5 runs it on the release branch and
requires that every coordinate it prints falls under a family row below. **§5b is complete by
licence, not yet by coordinate (2026-10-21).** The research-analyst resolved the POM of every one
of the 192 coordinates in the 2026-09-15 release dependency report, following parent chains: none
has an unknown licence and none is copyleft (`.planning/reports/phase-14/research-analyst-release-docs-licences.md`).
The families that §5b used to miss (BSD-3-Clause, MIT, MPL-2.0 and the Google SDK `hsdp`) now have
rows with that verdict. Two things stay open: the per-coordinate listing on the release branch
(U-6, §6), and the notice duties those rows carry, open item §16 #20 of `docs/tech-design.md`.

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
| Rules and board fidelity reference | The founder's own earlier peg-solitaire repository — https://github.com/ermanergoz/peg-solitarie (recorded in `.planning/phases/06-platform-foundation-build-hardening/06-CONTEXT.md`) | Own work | Rule behaviour and board fidelity checked against it | — |
| Candy-themed backdrop for the Home (menu) and gameplay screens. **Ships from `feature/candy-backdrop` (not yet built)**; if it does not reach `release/3.0.0`, delete this row at checklist item 2.4a | The candy-themed casual-game genre; the founder's named reference was *Candy Crush Saga* by King.com Limited — https://www.candycrush.com/ , https://play.google.com/store/apps/details?id=com.king.candycrushsaga | Not protected — a visual theme is an idea, not an expression (`rules/ip-policy.md`). King's names, logos, characters, candy designs and artwork are protected, and **none was taken** — verdict *inspiration only*, `.planning/reports/candy-backdrop/research-analyst.md` | The idea of a lively, glossy backdrop behind the menu and the board, and nothing else. The look is direction B "Deep gloss" (Q-021): a deep violet gradient, two glossy colour waves low on the screen, soft out-of-focus lights that rise and fade, a gloss highlight on panels, and gel-finish Home mode cards with a passing sheen. These are generic glossy-UI conventions (the gel controls of Mac OS X Aqua, 2000; the gloss and bokeh of 2004–2013 interface design), and no candy, sweet, character or other King element is drawn. Every shape, colour stop, motion and composition is authored in this repo (`ui/theme/Color.kt` tokens, Compose Canvas) | — deliberately not credited in the app: naming King in the app or the listing would imply a relationship Play's Impersonation policy forbids; this row stays out of any in-app licences screen |

## 2. Assets shipped in the artifact

The only binary assets in the app are twelve sound files in `app/src/main/res/raw/`, all mono
16-bit 44.1 kHz WAV — ten sourced under CC0 (seven from Kenney *Interface Sounds* 1.0, three from
two freesound sounds) and two rendered in-house from committed recipes — **and five font files** in
`app/src/main/res/font/` with their two licence texts in `app/src/main/assets/licenses/`, **and, from `feature/licences-screen`, a third licence text** there (`Apache-2.0.txt`, for the Licenses screen). There is
**no raster image anywhere in `app/src/main/res`** — every drawable and the launcher icon are vector
XML authored in this repo (`app/src/main/res/drawable/`, `mipmap-anydpi-v26/`).

**Audio rows: on `feature/audio-review` today** (WAV set d1c6e9b, in-house renders af2b554,
verdicts `docs/audio/licence-verdicts.md` 3a1fce7, research-analyst verdict 2026-09-29).
**Font rows: on `develop` since c9228a9** (merge of `feature/phase-15-ux-refresh`, 472835f and b5836ac; tech-lead ruling
`.planning/reports/15-founder-feedback/tech-lead-fonts.md` D1–D2). Re-checked against that ruling's
replacement text on 2026-10-18: same items, sources, hashes, upstreams, copyright lines and licence,
with more evidence in How verified and Credit.
**OFL-1.1 verdict (research-analyst, 2026-10-18): CLEAN. Fredoka v17 and Nunito v32 may ship bundled,
unmodified, in a store product. This is not a copyleft escalation.** OFL-1.1 has no row in
`rules/ip-policy.md`'s matrix. In the matrix's terms it sits beside the MPL-2.0 row ("yes (file-level
copyleft)"), with the copyleft at the level of the font files. Its share-alike clause (§5) binds only
the Font Software and never the program it ships in. OFL-FAQ 1.3: "Only the portions based on the Font
Software are required to be released under the OFL. The intent of the license is to allow aggregation
or bundling with software under restricted licensing as well." OFL-FAQ 1.4 names "games and
entertainment software, mobile device applications" among the permitted bundles, sale included.
Conditions, all met by the files on `feature/phase-15-ux-refresh` (checked 2026-10-18; the same blobs are on `develop` since c9228a9):
(1) **§1, no sale alone:** the fonts are never sold or offered by themselves, only inside the app.
(2) **§2, notice and licence in every copy:** `assets/licenses/Fredoka-OFL.txt` (sha256 `782c3137…cf31f7e0`)
and `Nunito-OFL.txt` (`66db16f4…829734a2`) ship in the APK/AAB. Each is the family download's `OFL.txt`,
and its licence body matches https://openfontlicense.org/documents/OFL.txt word for word (whitespace
normalised). OFL-FAQ 1.20 (mobile apps) asks for "the copyright statement, the license notice and the
license text". It calls an About-box mention "good practice", not a duty; the planned Licenses screen
provides it.
(3) **§3, Reserved Font Names:** neither copyright line declares an RFN, and none is reserved by
default (OFL-FAQ 5.7). All five files match `tech-lead-fonts.md` D1's sha256 and are renamed only. A file
left intact inside a wrapper needs no name change (OFL-FAQ 5.9). Any later subsetting, instancing or
table edit would make a Modified Version, which stays under OFL-1.1.
(4) **§4:** the authors' names are not used to promote the app. Acknowledging them on the Licenses
screen is the use §4 allows.
(5) **§5:** the fonts stay under OFL-1.1, never under the app's terms. Images rendered with the fonts
are outside it ("does not apply to any document created using the Font Software").
No founder question. Sources: the OFL-1.1 text, https://openfontlicense.org/open-font-license-official-text/ ;
OFL-FAQ 1.1-update7, https://openfontlicense.org/ofl-faq/ . Report:
`.planning/reports/phase-14/research-analyst-font-licences.md`.
**Apache-2.0 text row: on `feature/licences-screen`, not yet built** (`docs/ux/licences-screen.md` §7; `docs/tech-design.md` §3.6 and §14.1 LN).

| Item | Source | Licence | How verified | Credit |
|---|---|---|---|---|
| `sfx_jump.wav` | Kenney, *Interface Sounds* 1.0, `Audio/drop_003.ogg` — https://kenney.nl/assets/interface-sounds | **CC0 1.0** | Asset page and the pack's `License.txt`; archive and source-file sha256 pinned; research-analyst verdict **CLEAN** (`docs/audio/licence-verdicts.md` row 1, 2026-09-29) | `docs/audio/CREDITS.md` |
| `sfx_peg_remove.wav` | Kenney, *Interface Sounds* 1.0, `Audio/drop_002.ogg` — https://kenney.nl/assets/interface-sounds | **CC0 1.0** | same; verdict **CLEAN** (row 2) | same |
| `sfx_select.wav` | freesound sound 506546 "Pop 01" by LilMati — https://freesound.org/people/LilMati/sounds/506546/ | **CC0 1.0** | Licence on the sound's page (live and archived); master md5 pinned; verdict **CLEAN** (row 9) | same |
| `sfx_deselect.wav` | Derived in-repo from freesound sound 506546 (reversed, a whole tone lower) — https://freesound.org/people/LilMati/sounds/506546/ | **CC0 1.0** (derivative of a CC0 work) | Parent's licence; verdict **CLEAN** (row 10) | same |
| `sfx_undo.wav` | Kenney, *Interface Sounds* 1.0, `Audio/minimize_008.ogg` — https://kenney.nl/assets/interface-sounds | **CC0 1.0** | as `sfx_jump.wav`; verdict **CLEAN** (row 3) | same |
| `sfx_invalid.wav` | Rendered in-house from `audio/recipes/invalid.json` with the studio's `sfx-synth.sh` (oscillators and seeded noise; no sampled material) | Own work | Not a borrowed asset, no verdict needed (`docs/audio/licence-verdicts.md`, In-house cues); re-renders byte-identically | — |
| `sfx_hint.wav` | Rendered in-house from `audio/recipes/hint.json` with the studio's `sfx-synth.sh` (oscillators only; no sampled material) | Own work | same | — |
| `sfx_win.wav` | Kenney, *Interface Sounds* 1.0, `Audio/confirmation_002.ogg` — https://kenney.nl/assets/interface-sounds | **CC0 1.0** | as `sfx_jump.wav`; verdict **CLEAN** (row 4) | `docs/audio/CREDITS.md` |
| `sfx_star_pop.wav` | freesound sound 531512 "Positive Blip Effect" by CogFireStudios — https://freesound.org/people/CogFireStudios/sounds/531512/ | **CC0 1.0** | Licence on the sound's page (live and archived); master md5 pinned; verdict **CLEAN** (row 8) | same |
| `sfx_btn_forward.wav` | Kenney, *Interface Sounds* 1.0, `Audio/click_001.ogg` — https://kenney.nl/assets/interface-sounds | **CC0 1.0** | as `sfx_jump.wav`; verdict **CLEAN** (row 5) | same |
| `sfx_btn_dismiss.wav` | Kenney, *Interface Sounds* 1.0, `Audio/back_004.ogg` — https://kenney.nl/assets/interface-sounds | **CC0 1.0** | as `sfx_jump.wav`; verdict **CLEAN** (row 6) | same |
| `sfx_sound_enabled.wav` | Kenney, *Interface Sounds* 1.0, `Audio/confirmation_001.ogg` — https://kenney.nl/assets/interface-sounds | **CC0 1.0** | as `sfx_jump.wav`; verdict **CLEAN** (row 7) | same |
| Fredoka Medium, Fredoka SemiBold — `res/font/fredoka_medium.ttf`, `fredoka_semibold.ttf`, **bundled in the artifact** | Google Fonts, Fredoka **v17** static instances, unmodified (renamed only): https://fonts.google.com/download/list?family=Fredoka, entries `static/Fredoka-Medium.ttf` (sha256 `8143bd54…2304ae6fa`) and `static/Fredoka-SemiBold.ttf` (sha256 `021e30b2…d7688e43a8`; full hashes in `tech-lead-fonts.md` D1). Upstream https://github.com/hafontia/Fredoka-One. © 2016 The Fredoka Project Authors | **OFL-1.1**, no Reserved Font Name; the font stays under OFL-1.1, never under the app's terms (OFL §5) | The `OFL.txt` from the same family download (copyright line + full OFL 1.1 text); the files' own `name` table carries the same copyright (nameID 0) and an OFL-1.1 licence pointer (nameIDs 13/14). Research-analyst OFL-1.1 verdict: **CLEAN** (2026-10-18, note above) | **OFL §2 condition met by stand-alone text files in every copy:** `assets/licenses/Fredoka-OFL.txt` ships in the APK/AAB beside the fonts. Not shown on any screen — OFL does not require display; the studio's licences-screen rule is a separate gap, §7 |
| Nunito Regular, SemiBold, Bold — `res/font/nunito_regular.ttf`, `nunito_semibold.ttf`, `nunito_bold.ttf`, **bundled in the artifact** | Google Fonts, Nunito **v32** static instances, unmodified (renamed only): https://fonts.google.com/download/list?family=Nunito, entries `static/Nunito-Regular.ttf`, `static/Nunito-SemiBold.ttf`, `static/Nunito-Bold.ttf` (hashes in `tech-lead-fonts.md` D1). Upstream https://github.com/googlefonts/nunito. © 2014 The Nunito Project Authors | **OFL-1.1**, no Reserved Font Name; stays under OFL-1.1 (§5) | The `OFL.txt` from the same family download; `name` table as for Fredoka. Research-analyst OFL-1.1 verdict: **CLEAN** (2026-10-18, note above) | **OFL §2 met the same way:** `assets/licenses/Nunito-OFL.txt` in the APK/AAB. Checked at the release branch that the AAB keeps all five fonts and both texts (`tech-lead-fonts.md` AC-F5; release-checklist item 4.6a) |
| `assets/licenses/Apache-2.0.txt`: the Apache License 2.0 text, verbatim and unedited, opened from the Licenses screen's open-source entries. **Ships from `feature/licences-screen` (not yet built)**; if it does not reach `release/3.0.0`, delete this row at checklist item 2.4a | The Apache Software Foundation, https://www.apache.org/licenses/LICENSE-2.0.txt | The licence document's own terms. It is the text that Apache-2.0 §4(a) asks to be given to recipients of the AndroidX, Kotlin, kotlinx and Dagger/Hilt artifacts in §4 and §5b | Not yet: the file does not exist. At the build, its sha256 is compared with the file at the URL (`cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30` on 2026-10-18). **Research-analyst, 2026-10-18: CLEAN, the text may ship verbatim.** The ASF's licence FAQ answers "May I re-use (and modify) the Apache License 2.0 itself?" with "You may re-use our license unchanged" (https://www.apache.org/foundation/license-faq.html); only an altered text loses the Apache name, and re-flowing lines on screen (`licences-screen.md` LS-07) is one of the FAQ's own cosmetic examples ("changing the font or line spacing"). **NOTICE files: one must be reproduced.** `jakarta.inject:jakarta.inject-api:2.0.1` (on the release graph; the Mobile Ads SDK lists it among its components) carries `META-INF/NOTICE.md`, and Apache-2.0 §4(d) asks that its attribution notices travel with the app: in a NOTICE file shipped with it, in its documentation, or in its notices display. Today the file survives packaging intact (sha256 `992ed62d…512214df540`, the same as in the jar), but only because AGP 9.1.0's default excludes strip `META-INF/NOTICE` and `NOTICE.txt` and not `.md`. So the Licenses screen bundles it verbatim as `assets/licenses/Jakarta-Inject-NOTICE.md` and opens it from its own entry (`licences-screen.md` §9 item 4). No other Apache artifact in the last release build's graph carries a NOTICE file: 141 artifacts scanned for 192 coordinates; the 51 not in the Gradle cache (AndroidX, Compose, kotlinx metadata, Okio) are checked at checklist item 3.5a. OkHttp's `okhttp3/internal/publicsuffix/NOTICE` is an MPL-2.0 notice for bundled data, not an Apache NOTICE (`.planning/reports/phase-14/research-analyst-font-licences.md`, open issue 1) | Not a credit itself: it is the licence text behind the Licenses screen's "Apache License 2.0" entries (`licences-screen.md` §3.2) |
| Launcher icon, monochrome icon, cloud-save cover | Drawn in this repo as vector XML | Own work | — | — |
| Opening books (`domain/src/main/resources/golden/book/*.txt`), recorded solution fixtures, the Diamond-41 floor line | Authored in-house in phase 8 by beam search | Own work | `.planning/phases/08-assist-generator-quality-debt/08-BOOK-EVIDENCE.md` | — |

CC0 waives copyright and imposes no attribution condition, so the audio credits above are a
courtesy and an in-repo provenance record. The audio rows are per file because `rules/ip-policy.md`
asks for one row per borrowed asset and `rules/audio.md` asks every sourced sound's row to carry
the research-analyst's verdict. `docs/audio/CREDITS.md` stays the per-file processing record, and
`docs/audio/licence-verdicts.md` holds the verdict evidence, the archive and file pins, and the
commands that re-verify them at a release.

**What OFL-1.1 asks of an app that bundles a font, and how 3.0.0 meets it** (read from the OFL
text shipped in `assets/licenses/`): §1 — the font may not be sold by itself (it is not; it ships
inside the app). §2 — each copy bundled with software must contain the copyright notice and the
licence, "either as stand-alone text files, human-readable headers or in the appropriate
machine-readable metadata fields"; met by the two stand-alone `*-OFL.txt` files packaged in the
artifact. §3 — Reserved Font Names may not be used by a modified version; neither family declares
one, and the files are unmodified. §4 — the authors' names may not be used to promote the app;
they appear only in the notices. §5 — the font stays under OFL; recorded in the rows above. Nothing
in OFL requires the notice to be displayed in the UI.

## 3. Code snippets copied into this repo

None. The one row this section held, the downloadable-fonts certificate file, was deleted on
2026-10-21: the file left `develop` with the bundled fonts at c9228a9
(`.planning/reports/15-founder-feedback/tech-lead-fonts.md` D5).

## 4. Direct shipped dependencies

One row per coordinate declared at `implementation` in `app/build.gradle.kts`. Versions come from
`gradle/libs.versions.toml`; a version is not repeated here when the Compose BOM governs it.

| Coordinate | Version | Licence | How verified |
|---|---|---|---|
| `androidx.compose.material3:material3` | BOM 2026.06.00 | Apache-2.0 | POM `<licenses>` in the local Gradle cache |
| `androidx.compose.ui:ui` | BOM 2026.06.00 | Apache-2.0 | POM `<licenses>` |
| `androidx.compose.ui:ui-tooling-preview` | BOM 2026.06.00 | Apache-2.0 | AndroidX published terms (family row §5b) |
| `androidx.compose.material:material-icons-core` | BOM 2026.06.00 | Apache-2.0 | AndroidX published terms |
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
| `com.google.android.gms:play-services-games-v2` | 22.0.0 | **Android Software Development Kit License Agreement** (declared in the POM) — proprietary Google SDK, not open source; licensed for building Android apps and published by Google on its Maven repository for inclusion in them; shipped unmodified | POM `<licenses>`: *Android Software Development Kit License*, https://developer.android.com/studio/terms.html. POM fetched again 2026-10-21 from https://dl.google.com/dl/android/maven2/com/google/android/gms/play-services-games-v2/22.0.0/play-services-games-v2-22.0.0.pom (row C; I-1 closed, `.planning/reports/phase-14/research-analyst-release-docs-licences.md`) |
| `com.google.android.libraries.ads.mobile.sdk:ads-mobile-sdk` (Google Mobile Ads, Next-Gen) | 1.4.0 | **Android Software Development Kit License Agreement** (declared in the POM) — proprietary Google SDK, not open source; licensed for building Android apps and published by Google on its Maven repository for inclusion in them; shipped unmodified | POM `<licenses>`, same text as above. POM fetched again 2026-10-21 from https://dl.google.com/dl/android/maven2/com/google/android/libraries/ads/mobile/sdk/ads-mobile-sdk/1.4.0/ads-mobile-sdk-1.4.0.pom (row D; I-1 closed). 98 bundled components in its `third_party_licenses.txt` |
| `com.google.android.ump:user-messaging-platform` | 4.0.0 | **Android Software Development Kit License Agreement** (declared in the POM) — proprietary Google SDK, not open source; licensed for building Android apps and published by Google on its Maven repository for inclusion in them; shipped unmodified | POM `<licenses>`: *Android Software Development Kit License*, fetched 2026-10-21 from https://dl.google.com/dl/android/maven2/com/google/android/ump/user-messaging-platform/4.0.0/user-messaging-platform-4.0.0.pom (row B). **U-1 closed** (§6). Its AAR's `third_party_licenses.txt` (59 bundled components) covers its dependencies |

**Not Apache-2.0, and that matters.** The three Google SDK coordinates above are *not* open source.
Recording them as Apache-2.0 because the rest of the table is Apache-2.0 would be a false row in
the exact document the release gate reads. The Android SDK License Agreement grants a licence "to
use the SDK solely to develop applications for compatible implementations of Android" (§3.1) and
bars redistributing "the SDK or any part of the SDK" except as third-party licences require
(§3.4). No clause expressly says "you may ship these libraries inside your app". The permission to
ship rests on Google publishing these artifacts on its Maven repository as app dependencies, with
integration guides that tell developers to include them; §3.8 asks that Google's notices stay
intact. The research-analyst's verdict is **CLEAN** (known licence from the rights holder, not
copyleft, shipped unmodified), not an escalation (`.planning/reports/phase-14/research-analyst-release-docs-licences.md`, verdicts #1 to #3).

## 5. Transitive dependencies

### 5a. Method

Every artifact above pulls in more. Listed by publisher family; each family row states the licence
its members are published under and the evidence for it.

### 5b. Families on the release runtime classpath

| Family | Examples on this graph | Licence | How verified |
|---|---|---|---|
| `androidx.*` (Compose runtime/foundation/animation/graphics, annotation, collection, arch.core, savedstate, lifecycle-*, sqlite, startup, profileinstaller, emoji2, tracing, versionedparcelable …) | pulled by Compose, Room, DataStore, Navigation, Activity | Apache-2.0, **except** `androidx.datastore:datastore-preferences-external-protobuf` 1.2.1, which is **BSD-3-Clause** (row G below) | AndroidX is published under Apache-2.0; six member POMs read directly from the local cache all declare "The Apache Software License, Version 2.0", and `room-runtime` ships the full text inside its AAR. The exception's own POM declares BSD-3-Clause ("Repackaged proto-lite dependency for use by datastore preferences"), and its in-APK `LICENSE.txt` has no copyright line (row G) |
| `org.jetbrains.kotlin:*` (kotlin-stdlib and friends) | required by every Kotlin artifact | Apache-2.0 | POM `<licenses>`: "Apache-2.0" |
| `org.jetbrains.kotlinx:*` (coroutines-core-jvm, coroutines-android, serialization-core-jvm, atomicfu) | coroutines and serialization runtimes | Apache-2.0 | `kotlinx-coroutines-play-services` POM declares Apache-2.0; the rest are siblings from the same publisher under the same published terms |
| `com.google.dagger:*` (dagger, dagger-lint-aar) | Hilt runtime | Apache-2.0 | POM `<licenses>` |
| `com.google.android.gms:*` (play-services-basement, -base, -tasks, -ads-identifier, -appset, -cronet; no `-auth-*` artifact is on this graph) | Play Games v2, Mobile Ads, UMP, hsdp | Android Software Development Kit License Agreement (declared in each POM) — proprietary Google SDK, not open source; shipped unmodified | Each member's POM read 2026-10-21; row E below |
| `com.google.guava:*`, `com.google.errorprone:*`, `javax.inject:*`, and the other Apache-2.0 libraries in the third table below | small annotation/utility libraries | Apache-2.0 | Each POM read 2026-10-21 (third table below). `org.checkerframework:checker-qual` is **MIT**, row I |

**Families verified on 2026-10-21** (research-analyst, `.planning/reports/phase-14/research-analyst-release-docs-licences.md`, "Table for THIRD_PARTY.md", transcribed as given). Rows B, C and D of that table are the three Google SDK rows in §4. "Licenses screen" means the Settings › About screen specified in `docs/ux/licences-screen.md` and built on `feature/licences-screen` (`docs/tech-design.md` §3.6 and §14.1 LN). It is not built yet, and its entries for rows F to J need the ux-designer's amendment (§16 #20).

| Name | Coordinates@version | Source URL | Licence | What is used | How credited |
|---|---|---|---|---|---|
| A. Google Play HSDP (Play inline-install "half sheet details page" library) | `com.google.android.play:hsdp@2.0.1` | https://dl.google.com/dl/android/maven2/com/google/android/play/hsdp/2.0.1/hsdp-2.0.1.pom | Android Software Development Kit License Agreement, https://developer.android.com/studio/terms (proprietary Google SDK; not open source). It belongs to the Play Core set, whose *Play Core SDK Terms of Service* expressly allow copying and distributing Redistributable Code "solely for inclusion as part of your API Client" | The library, unmodified. It comes in transitively through `ads-mobile-sdk` 1.4.0, and no app code calls it | The SDK licence requires no credit, only that Google's notices stay intact (§3.8). Its bundled `third_party_licenses.txt` (70 components) falls under the Google-SDK notices ruling (font report, open issue 2; `docs/tech-design.md` §16 #20) |
| E. Google Play services siblings (family) | `com.google.android.gms:` `play-services-basement@18.9.0`, `-base@18.5.0`, `-tasks@18.2.0`, `-ads-identifier@18.0.0`, `-appset@16.0.1`, `-cronet@18.0.1` | https://dl.google.com/dl/android/maven2/com/google/android/gms/ (each `<artifact>/<version>/<artifact>-<version>.pom`) | as A; each POM was read | Transitive, unmodified (Games v2, Mobile Ads, UMP, hsdp) | As A |
| F. Protocol Buffers (Java Lite runtime) | `com.google.protobuf:protobuf-javalite@3.22.3` | https://github.com/protocolbuffers/protobuf/blob/v22.3/LICENSE ; POM https://repo1.maven.org/maven2/com/google/protobuf/protobuf-parent/3.22.3/protobuf-parent-3.22.3.pom | BSD-3-Clause | The runtime library, unmodified. It is pulled at runtime scope by `org.chromium.net:cronet-shared` 143.7445.0 (73 classes after R8) | Licenses screen entry "Protocol Buffers": "Copyright 2008 Google Inc. All rights reserved.", the three conditions and the disclaimer. Text: the v22.3 LICENSE verbatim (1,732 B, sha256 `6e5e117324afd944…`) |
| G. Protocol Buffers, repackaged by AndroidX DataStore | `androidx.datastore:datastore-preferences-external-protobuf@1.2.1` | https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences-external-protobuf/1.2.1/datastore-preferences-external-protobuf-1.2.1.pom | BSD-3-Clause (the exception in the `androidx.*` row above) | protobuf-lite repackaged as `androidx.datastore.preferences.protobuf`, unmodified, through `datastore-preferences` 1.2.1 (79 classes after R8). Its own `LICENSE.txt` ships in the APK but carries no copyright line | The same "Protocol Buffers" entry as F, which names both copies. The copyright line that its bundled text lacks comes from F |
| H. Chromium Cronet (Java API, fallback and HttpEngine provider) | `org.chromium.net:cronet-api`, `cronet-shared`, `cronet-common`, `cronet-fallback`, `httpengine-native-provider`, all `@143.7445.0` | https://github.com/chromium/chromium/blob/143.0.7445.0/LICENSE ; the POMs' licence file: https://storage.googleapis.com/chromium-cronet/android/143.0.7445.0/Release/cronet/LICENSE | BSD-3-Clause (Chromium) | The Java-only Cronet classes, unmodified, through `ads-mobile-sdk` 1.4.0 (151 classes after R8). No native library ships | Licenses screen entry "Chromium (Cronet)": "Copyright 2015 The Chromium Authors", the three conditions and the disclaimer. Text: Chromium's LICENSE at 143.0.7445.0 verbatim (1,536 B, sha256 `368cca1106be99d3…`) |
| I. Checker Framework qualifiers | `org.checkerframework:checker-qual@3.33.0` | https://github.com/typetools/checker-framework/blob/checker-framework-3.33.0/checker-qual/LICENSE.txt ; POM https://repo1.maven.org/maven2/org/checkerframework/checker-qual/3.33.0/checker-qual-3.33.0.pom | MIT | Annotations, on the graph through Guava 32.0.0. **Nothing ships:** the 2026-09-21 release kept 0 classes, and AGP excludes its `META-INF/LICENSE.txt` | No duty while nothing ships. Recommended: a Licenses screen entry "Checker Framework qualifiers" with its MIT text verbatim (1,126 B, sha256 `e90eaa08dea089df…`), so no per-release R8 check is needed. Until that entry exists, checklist item 3.5a greps the release mapping for `org.checkerframework.` |
| J. Public Suffix List (data compiled into OkHttp) | `com.squareup.okhttp3:okhttp@4.11.0`, file `okhttp3/internal/publicsuffix/publicsuffixes.gz` (41,394 B) with its `NOTICE` (218 B) | https://publicsuffix.org/list/public_suffix_list.dat ; https://github.com/square/okhttp/blob/parent-4.11.0/okhttp/src/main/resources/okhttp3/internal/publicsuffix/NOTICE | MPL-2.0 (this data file only; OkHttp's code is Apache-2.0). The file ships unmodified, so the file-level copyleft binds only that file and never the app (MPL-2.0 §3.3) | The compiled suffix list, unmodified, inside OkHttp, through `ads-mobile-sdk` 1.4.0. Both files ship as Java resources in the release APK | Licenses screen entry "Public Suffix List": "Mozilla Public License 2.0", OkHttp's NOTICE verbatim (sha256 `8a9c58fbded5ea34…`), which names the source and https://mozilla.org/MPL/2.0/ (the source pointer MPL-2.0 §3.2(a) asks for). Optionally the MPL-2.0 text (16,726 B, sha256 `3f3d9e0024b1921b…`) |

**Apache-2.0 libraries on the graph** (same report; every POM below was read and declares Apache-2.0). Their credit is the Apache-2.0 text the Licenses screen already plans (§2), plus the Jakarta NOTICE (§2, the `Apache-2.0.txt` row).

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
| `jakarta.inject:jakarta.inject-api@2.0.1` (NOTICE.md ships; §2) | The Apache Software License, Version 2.0 |
| `javax.inject:javax.inject@1` | The Apache Software License, Version 2.0 |

**Open item: the licences are settled; the listing and the notices are not.** Every coordinate in the 2026-09-15 release dependency report (192 of 192) now falls under a row with a known licence: 174 Apache-2.0, 10 Android SDK License, 5 Chromium BSD-3, 2 BSD-3-Clause, 1 MIT; none unknown, none copyleft. Two things remain, and both block the release:
- **U-6 (§6):** the per-coordinate listing of `releaseRuntimeClasspath` on `release/3.0.0` (checklist item 3.5). Any coordinate it prints that is not among those 192 needs a verdict before the release. It also confirms that Cronet stays Java-only: a native `cronet-embedded` would bring in the native sections of Chromium's 326 KB LICENSE (BoringSSL, zlib and more).
- **The notice duties**, `docs/tech-design.md` §16 #20, due before `release/3.0.0` is cut: the Licenses screen entries for rows F to J (ux-designer amendment, then the developer bundles the texts at the hashes given), and the tech-lead's ruling on the Google SDKs' `third_party_licenses.txt` notices, which the build does not package (font report, open issue 2). That ruling now covers hsdp (70 components) and UMP (59) as well as Mobile Ads and Games v2. BSD-3 and MPL-2.0 make the notice a condition of redistribution.

`docs/release-checklist.md` items 3.5, 3.5a and 3.5b re-check the result on the release branch.

## 6. Unverified — the release gate's open rows

| # | Item | Why unverified | What would settle it |
|---|---|---|---|
| U-1 | `com.google.android.ump:user-messaging-platform:4.0.0` | No POM in the local Gradle module cache (the artifact resolves through Gradle module metadata); the AAR's `third_party_licenses.txt` covers its dependencies, not itself | **Closed 2026-10-21** — the research-analyst fetched `user-messaging-platform-4.0.0.pom` from `dl.google.com`: it declares the *Android Software Development Kit License* (§4 row; row B, `.planning/reports/phase-14/research-analyst-release-docs-licences.md`). Verdict CLEAN, no escalation |
| U-2 | **The 20 achievement icons** (512×512, uploaded to the Play Console in phase 12) | They exist only in the Play Console. Nothing in this repository records their origin — there is no raster image in `app/src/main/res` at all | **Closed 2026-10-17** — founder answer to Q-017: studio-produced; own-work rows with hashes in §7 |
| U-3 | **The listing 512×512 icon** | Same — Console only | **Closed 2026-10-17** — same answer; §7 |
| U-4 | **The 1024×500 feature graphic** | Same — Console only | **Closed 2026-10-17** — same answer; §7 |
| U-5 | **Any store screenshot that is not a plain capture of this app** | Same — Console only | **Closed 2026-10-17** — the listing screenshots are plain captures the studio recaptures after the candy backdrop lands (Q-016); §7 |
| U-6 | Per-coordinate enumeration of §5b | The resolved release runtime classpath has not been listed (Gradle deliberately not run on this branch). Every family in the 2026-09-15 report (192 coordinates) now has a row with a known licence, `hsdp` included (row A), but that report predates the release branch | `./gradlew :app:dependencies --configuration releaseRuntimeClasspath`, on the release branch, output recorded in the release report; any coordinate outside the 192 gets a verdict before the release. **Still open** |

## 7. Store artwork provenance (Play Console assets)

These assets are not in the artifact. They live in the Play Console (listing, and Play Games
Services achievements and leaderboards), so `rules/ip-policy.md` and the Play IP policy still apply
to them. The rows waited on the founder (Q-F3) until the founder answered **Q-017** on
2026-10-17: *"you did it"*: the studio produced them in its own sessions. The founder keeps the
files on their Desktop, dated 2026-09-14/15. **They are not copied into this repo**; the sha256
below identifies each file, so a later upload can be checked against this record.

| Item | Source | Licence | Credit |
|---|---|---|---|
| 20 achievement icons (512×512), `PegSolitaire Achievement Icons/a01…a20` | Produced by the studio in its own sessions, 2026-09-15 (founder answer, Q-017). No source file is kept; per-file hashes below | **Own work** | — |
| 7 leaderboard icons (512×512), `PegSolitaire Leaderboard Icons/` | Same | **Own work** | — |
| Listing icon (512×512), `peg-solitaire-icon-512.png` | Exported from the studio's approved launcher-icon design canvas, 2026-09-14. That canvas is the same design the in-app launcher vectors reproduce (`.planning/quick/260914-x4a-add-adaptive-launcher-icon-from-the-appr/260914-x4a-PLAN.md`, commit 5e99af1: "The Play Store 512×512 PNG is NOT this task's deliverable; the user exports that from the design canvas separately") | **Own work** | — |
| Feature graphic (1024×500), `peg-solitaire-feature-1024x500.png` | Produced by the studio, 2026-09-14 (Q-017). The file's XMP records a 1024×1024 original, so it was cropped to 1024×500 on this Mac (the file's quarantine tag names macOS Preview). The title and tagline are set in rounded sans faces that look like the app's own Fredoka and Nunito (OFL-1.1, §2) | **Own work** (text rendered in OFL-1.1 fonts: OFL covers the font software, not images made with it, so no notice attaches) | — |
| Store screenshots | Plain captures of this app, recaptured by the studio after the candy backdrop lands (Q-016; candy-backdrop verdict §3d: no added art or framing) | **Own work** | — |

**How each file was checked (2026-10-17).** Each image was opened and inspected; hashes from
`shasum -a 256`, sizes from `sips`. Each file's PNG chunks were read: no `tEXt`/`iTXt` names a
generator or a source, and Spotlight records no download URL (`kMDItemWhereFroms` empty) for any
file. The achievement and leaderboard PNGs have only `IHDR`/`IDAT`/`IEND` chunks and a macOS
quarantine tag naming Chrome, so they were saved from a browser page. That fits a studio canvas
rendered in a browser, the same way as the launcher design. **What is in the art:** peg-board
footprints of the five Classic boards, pink pegs, gold stars, a stopwatch, a struck-through bulb
(no hints), a struck-through undo arrow, a bar ladder, number pills (5, 10, 25, 50, 100, 250), a
dotted constellation, and violet, pink and gold rims. No logo, no character, no photograph, no
stock texture, no watermark, and no third-party mark. The only text is the feature graphic's
"Peg Solitaire" and "Classic boards, endless fun!" plus the digits in the pills. The pictograms
are generic. If any one of them was drawn after an icon set, the likely sets (Material Symbols)
are Apache-2.0, so no licence gap opens either way.

**Achievement icons** (Console names in
`.planning/phases/12-leaderboards-achievements/12-OPERATOR-CHECKLIST.md`):

| # | File | Size | sha256 |
|---|---|---|---|
| A01 | `a01-first-clear.png` | 512×512 | `dd3117880734c1c25775f1cccc0181c4f1880089fb36f974eab6fe009a7625ba` |
| A02 | `a02-three-stars.png` | 512×512 | `48ec47bb312fc67ef5929ec5b9f525a1737ff7e8bf314afe62d0698606c2269c` |
| A03 | `a03-getting-started.png` | 512×512 | `1d8814f550a435215662f09bc538179399b5e238bf6747151dd9b41d57b43b24` |
| A04 | `a04-quick-hands.png` | 512×512 | `f7079525148552bd9fd2514e5bfa170d68146e69e2ea1cbb9b081c89cf01e033` |
| A05 | `a05-no-hints-needed.png` | 512×512 | `7c518147352cdd7854555910016b7d383f4d56cf54d87c81a10f683a5325deb2` |
| A06 | `a06-no-take-backs.png` | 512×512 | `7fef8faccaf755c578af77c2707bc5a862e1d154ffafdc9bca49b4388c6e4f71` |
| A07 | `a07-dead-center.png` | 512×512 | `d65a40db523eb6ebcef57e0c785dee376660fed822cd6311a330f13d5662069c` |
| A08 | `a08-english-cleared.png` | 512×512 | `b283f4980e869f54b02e9193e5c545675ea19e0cdb6b091c28a4ea5e11b6f547` |
| A09 | `a09-european-cleared.png` | 512×512 | `d98dead11b97af79fd01ead4b273bc30af716a3e4b142f440b15334db7f49f47` |
| A10 | `a10-asymmetric-cleared.png` | 512×512 | `48bde989a8f4cb4b7a3b549d721c562b9411170910e34905e259dcaaa27dce49` |
| A11 | `a11-diamond-cleared.png` | 512×512 | `8024b89a6814c0d29c98ea4db15361954e0b43f0a7baf6fdb2bd76f504784f86` |
| A12 | `a12-wiegleb-cleared.png` | 512×512 | `2ae1b6fcb2b07721baf6103fa8db00becb0b23678c905410bc41e7bf55c3e5fe` |
| A13 | `a13-climbing.png` | 512×512 | `cdb8b2caeab195e1c2fa8052c64477c299f05b6c6fda9e06f8c38061c4b4e013` |
| A14 | `a14-ascending.png` | 512×512 | `fbaf04eef36738b0efa2a9311def18bd24184c4d6c487df3ee266a3e07add9ff` |
| A15 | `a15-century-climb.png` | 512×512 | `19ac6be3eba2f1be61ec06b4824f17e1d1b6ec15f646e415c2d997e9cb4eec76` |
| A16 | `a16-star-collector.png` | 512×512 | `35a450a74fe01aafd83894d8309925b1237bf7dee3d01f11a179be7eb1774406` |
| A17 | `a17-star-hoarder.png` | 512×512 | `ffca30cc80d4e0a77cd1348cc028dca8a8776956bc2d677a69031c56b38f3d46` |
| A18 | `a18-constellation.png` | 512×512 | `65e993fcc479f280cd9e61f8064245cd69072f5acdd097c473bc052121c06864` |
| A19 | `a19-grand-tour.png` | 512×512 | `5754a68e5e778d549d0b3755f0e89413c70c03aff6d9fe303ea0e9e69556ff73` |
| A20 | `a20-perfectionist.png` | 512×512 | `8fb1db6ef0ee70463bda3ccc7f3d97d67b3926ac5fcf7c43beb9ffe1c421ee8d` |

**Leaderboard icons:**

| File | Size | sha256 |
|---|---|---|
| `asymmetric-39-best-score.png` | 512×512 | `7eb0815630ae9efaa0d91f8d2be57e0cab378432bb774a924997ed4fcd6c6534` |
| `challenge-ladder.png` | 512×512 | `ec8df014d0c6f1195e165ab8a4ad1d8969e965928bad541cf3963aa8ddee19d8` |
| `challenge-total-score.png` | 512×512 | `8f1aa8faca4ec0e7b535c421122159c064a1dcd03d11e68dc9d090eb02c50a5e` |
| `diamond-41-best-score.png` | 512×512 | `82c755555ff07aa787821f64a6f3263d44d43553be6ffc54d888756f9757b646` |
| `english-33-best-score.png` | 512×512 | `45a2b59aee584d05ca893302b338d7353750645b7457b4053267b35cc74229a7` |
| `european-37-best-score.png` | 512×512 | `6acadbe5a833e6ce8edf6f56fbb4b689ca929096dbdb6538cfdcf1ea9fa5d3f2` |
| `wiegleb-45-best-score.png` | 512×512 | `b9956a28b857e8044f24000e40804b032c78e7e45770d205642c2c4ceae257bf` |

**Listing assets:**

| File | Size | sha256 |
|---|---|---|
| `peg-solitaire-icon-512.png` | 512×512 | `d6cf915da10c78a752504484cc789514e652a96bd94ed15556926703c6134335` |
| `peg-solitaire-feature-1024x500.png` | 1024×500 | `bfaa1ba1cbf79a5a112b3bb0a4a3e30c341d7ae91cd119cdede864078f1b03e4` |

**Limits of this record.** No session record of how the achievement, leaderboard and feature
graphic files were drawn is in this repo; the founder's answer is the provenance. For the
listing icon, the design is recorded only by the launcher task's plan; the canvas file
itself was session scratch and no longer exists. Art an agent produced may carry no
copyright of its own (US Copyright Office, Part 2 report; *Thaler v. Perlmutter*; cited in the
candy-backdrop verdict §4c). That means others might copy these images. It does not create an
infringement risk for this app, and it does not leave a licence gap.

Two related items, recorded here so they are not rediscovered:

- **No in-app licences screen exists.** `rules/ip-policy.md` asks for attribution "in
  `THIRD_PARTY.md` **and** in the product's licences screen or page". Settings today has the two
  privacy rows (REL-05 / CONS-03) and no licences row. Every shipped asset is CC0, OFL or own
  work, and the two bundled font families' OFL-1.1 notice condition is met by the stand-alone
  licence files in the artifact (§2). **The dependencies' notices are not all met.** Their licences
  are now all known (§5b, 2026-10-21), and the BSD-3-Clause (Protocol Buffers, Chromium Cronet) and
  MPL-2.0 (Public Suffix List) rows carry notice duties that no screen meets yet; the Google SDKs'
  own `third_party_licenses.txt` notices are not packaged (§5b open item). These attribution
  obligations are unmet today, and are `docs/tech-design.md` §16 #20 (owners there;
  deadline before `release/3.0.0` is cut). Until it closes, this record does not claim that nothing
  shipped carries an unmet obligation. **Routed, not fixed here:** either a Settings
  licences row (a UI change: a developer with a ux-designer review) or a licences *page* on the
  developer website linked from the listing (the rule says "screen or page"; website work is
  founder-only); the producer decides which, and whether it is pulled into 3.0.0 or left for
  3.0.1. **Update 2026-10-18:** a Settings › About › Licenses screen is specified
  (`docs/ux/licences-screen.md`) and is planned for 3.0.0, not built yet. **Update 2026-10-23:** it is
  built on its own branch, `feature/licences-screen` (`docs/tech-design.md` §3.6 and §14.1 LN), and LN is a
  branch-cut blocker in §14.1, so the "or left for 3.0.1" choice above no longer applies.
  This bullet is closed when that branch merges; if it does not reach `release/3.0.0`, the release
  is not cut (`docs/release-checklist.md` item 1.1).
- A **`SKILLS.md`** is named by `rules/skills-policy.md` and does not exist in this repo. No
  external skill has been installed into `.claude/skills/`, so there is nothing to record; noted
  so the absence is a fact rather than an omission.

## 8. Not shipped — recorded so the gate is not asked about them

For the avoidance of doubt, and so a reader does not re-derive §0: JUnit 4 (`junit:junit:4.13.2`,
**EPL-1.0**), Truth, Turbine, MockK, ArchUnit, Robolectric, `kotlinx-coroutines-test`,
`androidx.room:room-testing`, `androidx.compose.ui:ui-test-junit4`,
`androidx.compose.ui:ui-test-manifest`, `androidx.compose.ui:ui-tooling`,
`com.google.errorprone:error_prone_annotations` (compileOnly), the Hilt and Room KSP processors,
and every Gradle plugin in `[plugins]` (kover 0.9.11, Apache-2.0, included; §0 row) are **test-, debug-, compile- or build-only**. None is in a
release artifact and none is distributed, so no notice or source obligation attaches to anything
that ships — including JUnit's EPL, which `rules/ip-policy.md` does not list and which needs no
escalation for exactly that reason.
