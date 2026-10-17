# THIRD_PARTY.md — Peg Solitaire

Every third-party idea, asset, code snippet and dependency that reaches a player's device, with
its licence. Required by `rules/ip-policy.md`; the studio's release gate **blocks a release when a
shipped row has no licence**, so an honest "unverified" row is a blocker by design, not an
oversight to be papered over.

Created on `feature/phase-14-release-docs` for v1.1 / product version **3.0.0** (release blocker
R-5, `.planning/reports/phase-14/tech-lead-signoff-r3-signing.md` §G). Re-checked at every release
branch cut — `docs/release-checklist.md` step 6.

**Rows describe what 3.0.0 will ship, not what `develop` holds today** (updated 2026-10-17). Two
asset changes are still on unmerged branches, and each affected row names its branch:

| Change | Branch today | What `develop` (3f2af90) still holds |
|---|---|---|
| Twelve sound cues as WAV; the wrong-tap and hint cues now rendered in-house instead of taken from the Kenney pack | `feature/audio-review` (d1c6e9b, af2b554; verdicts 3a1fce7) | the twelve-file `.ogg` set described by the earlier revision of this file |
| Fredoka and Nunito **bundled** as five static font files plus their OFL texts; downloadable-font provider, `font_certs.xml` and `ui-text-google-fonts` removed | `feature/phase-15-ux-refresh` (472835f, b5836ac) | the downloadable-font path, nothing bundled |
| A glossy backdrop behind the Home and gameplay screens, drawn in-repo: direction B "Deep gloss" (Q-021, 2026-10-18; §1 idea row) | `feature/candy-backdrop` (**not yet built**: verdict, its direction-B addendum and the UX spec only) | no backdrop |
| An in-app Licenses screen (Settings › About) and its new `assets/licenses/Apache-2.0.txt` (§2 row) | `feature/candy-backdrop` (**not yet built**: `docs/ux/licences-screen.md` only) | no licences screen, no Apache text |

If any of these branches does not reach `release/3.0.0`, its rows below are wrong for the release
and are re-cut at checklist step 1.4.

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
| `[plugins]` block + `build-logic/` classpath — AGP, Kotlin, KSP, Hilt, Room, detekt, ktlint Gradle plugins; kover (pending feature/phase-14-check-gates) | no | build tooling; runs on the build machine |
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
| Candy-themed backdrop for the Home (menu) and gameplay screens. **Ships from `feature/candy-backdrop` (not yet built)**; if it does not reach `release/3.0.0`, delete this row at checklist 1.4 | The candy-themed casual-game genre; the founder's named reference was *Candy Crush Saga* by King.com Limited — https://www.candycrush.com/ , https://play.google.com/store/apps/details?id=com.king.candycrushsaga | Not protected — a visual theme is an idea, not an expression (`rules/ip-policy.md`). King's names, logos, characters, candy designs and artwork are protected, and **none was taken** — verdict *inspiration only*, `.planning/reports/candy-backdrop/research-analyst.md` | The idea of a lively, glossy backdrop behind the menu and the board, and nothing else. The look is direction B "Deep gloss" (Q-021): a deep violet gradient, two glossy colour waves low on the screen, soft out-of-focus lights that rise and fade, a gloss highlight on panels, and gel-finish Home mode cards with a passing sheen. These are generic glossy-UI conventions (the gel controls of Mac OS X Aqua, 2000; the gloss and bokeh of 2004–2013 interface design), and no candy, sweet, character or other King element is drawn. Every shape, colour stop, motion and composition is authored in this repo (`ui/theme/Color.kt` tokens, Compose Canvas) | — deliberately not credited in the app: naming King in the app or the listing would imply a relationship Play's Impersonation policy forbids; this row stays out of any in-app licences screen |

## 2. Assets shipped in the artifact

The only binary assets in the app are twelve sound files in `app/src/main/res/raw/`, all mono
16-bit 44.1 kHz WAV — ten sourced under CC0 (seven from Kenney *Interface Sounds* 1.0, three from
two freesound sounds) and two rendered in-house from committed recipes — **and five font files** in
`app/src/main/res/font/` with their two licence texts in `app/src/main/assets/licenses/`, **and, from `feature/candy-backdrop`, a third licence text** there (`Apache-2.0.txt`, for the Licenses screen). There is
**no raster image anywhere in `app/src/main/res`** — every drawable and the launcher icon are vector
XML authored in this repo (`app/src/main/res/drawable/`, `mipmap-anydpi-v26/`).

**Audio rows: on `feature/audio-review` today** (WAV set d1c6e9b, in-house renders af2b554,
verdicts `docs/audio/licence-verdicts.md` 3a1fce7, research-analyst verdict 2026-09-29).
**Font rows: on `feature/phase-15-ux-refresh` today** (472835f; tech-lead ruling
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
Conditions, all met by the files on `feature/phase-15-ux-refresh` (checked 2026-10-18):
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
**Apache-2.0 text row: on `feature/candy-backdrop`, not yet built** (`docs/ux/licences-screen.md` §7).

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
| Nunito Regular, SemiBold, Bold — `res/font/nunito_regular.ttf`, `nunito_semibold.ttf`, `nunito_bold.ttf`, **bundled in the artifact** | Google Fonts, Nunito **v32** static instances, unmodified (renamed only): https://fonts.google.com/download/list?family=Nunito, entries `static/Nunito-Regular.ttf`, `static/Nunito-SemiBold.ttf`, `static/Nunito-Bold.ttf` (hashes in `tech-lead-fonts.md` D1). Upstream https://github.com/googlefonts/nunito. © 2014 The Nunito Project Authors | **OFL-1.1**, no Reserved Font Name; stays under OFL-1.1 (§5) | The `OFL.txt` from the same family download; `name` table as for Fredoka. Research-analyst OFL-1.1 verdict: **CLEAN** (2026-10-18, note above) | **OFL §2 met the same way:** `assets/licenses/Nunito-OFL.txt` in the APK/AAB. Checked at the release branch that the AAB keeps all five fonts and both texts (`tech-lead-fonts.md` AC-F5; release-checklist 1.4) |
| `assets/licenses/Apache-2.0.txt`: the Apache License 2.0 text, verbatim and unedited, opened from the Licenses screen's open-source entries. **Ships from `feature/candy-backdrop` (not yet built)**; if it does not reach `release/3.0.0`, delete this row at checklist 1.4 | The Apache Software Foundation, https://www.apache.org/licenses/LICENSE-2.0.txt | The licence document's own terms. It is the text that Apache-2.0 §4(a) asks to be given to recipients of the AndroidX, Kotlin, kotlinx and Dagger/Hilt artifacts in §4 and §5b | Not yet: the file does not exist. At the build, its sha256 is compared with the file at the URL (`cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30` on 2026-10-18). **Research-analyst, 2026-10-18: CLEAN, the text may ship verbatim.** The ASF's licence FAQ answers "May I re-use (and modify) the Apache License 2.0 itself?" with "You may re-use our license unchanged" (https://www.apache.org/foundation/license-faq.html); only an altered text loses the Apache name, and re-flowing lines on screen (`licences-screen.md` LS-07) is one of the FAQ's own cosmetic examples ("changing the font or line spacing"). **NOTICE files: one must be reproduced.** `jakarta.inject:jakarta.inject-api:2.0.1` (on the release graph; the Mobile Ads SDK lists it among its components) carries `META-INF/NOTICE.md`, and Apache-2.0 §4(d) asks that its attribution notices travel with the app: in a NOTICE file shipped with it, in its documentation, or in its notices display. Today the file survives packaging intact (sha256 `992ed62d…512214df540`, the same as in the jar), but only because AGP 9.1.0's default excludes strip `META-INF/NOTICE` and `NOTICE.txt` and not `.md`. So the Licenses screen bundles it verbatim as `assets/licenses/Jakarta-Inject-NOTICE.md` and opens it from its own entry (`licences-screen.md` §9 item 4). No other Apache artifact in the last release build's graph carries a NOTICE file: 141 artifacts scanned for 192 coordinates; the 51 not in the Gradle cache (AndroidX, Compose, kotlinx metadata, Okio) are checked at checklist step 6. OkHttp's `okhttp3/internal/publicsuffix/NOTICE` is an MPL-2.0 notice for bundled data, not an Apache NOTICE (`.planning/reports/phase-14/research-analyst-font-licences.md`, open issue 1) | Not a credit itself: it is the licence text behind the Licenses screen's "Apache License 2.0" entries (`licences-screen.md` §3.2) |
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

| Item | Source | Licence | How verified | Credit |
|---|---|---|---|---|
| `app/src/main/res/values/font_certs.xml` — the dev and prod certificate arrays for the Google Play services font provider. **Does not ship in 3.0.0:** deleted on `feature/phase-15-ux-refresh` (b5836ac) when the fonts were bundled; the row stays only while `develop` still carries the file | The standard certificate block published in the Android downloadable-fonts documentation (the file's own header records this) | **Apache-2.0** — developer.android.com sample code is published under Apache-2.0 | Project's published terms (Android developer site content licence). The certificates themselves are public key material, not a copyrightable work | This row |

## 4. Direct shipped dependencies

One row per coordinate declared at `implementation` in `app/build.gradle.kts`. Versions come from
`gradle/libs.versions.toml`; a version is not repeated here when the Compose BOM governs it.

| Coordinate | Version | Licence | How verified |
|---|---|---|---|
| `androidx.compose.material3:material3` | BOM 2026.06.00 | Apache-2.0 | POM `<licenses>` in the local Gradle cache |
| `androidx.compose.ui:ui` | BOM 2026.06.00 | Apache-2.0 | POM `<licenses>` |
| `androidx.compose.ui:ui-tooling-preview` | BOM 2026.06.00 | Apache-2.0 | AndroidX published terms (family row §5b) |
| `androidx.compose.material:material-icons-core` | BOM 2026.06.00 | Apache-2.0 | AndroidX published terms |
| `androidx.compose.ui:ui-text-google-fonts` — **does not ship in 3.0.0**: removed on `feature/phase-15-ux-refresh` (b5836ac); listed while `develop` still declares it | BOM 2026.06.00 | Apache-2.0 | POM `<licenses>` |
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
| U-2 | **The 20 achievement icons** (512×512, uploaded to the Play Console in phase 12) | They exist only in the Play Console. Nothing in this repository records their origin — there is no raster image in `app/src/main/res` at all | **Closed 2026-10-17** — founder answer to Q-017: studio-produced; own-work rows with hashes in §7 |
| U-3 | **The listing 512×512 icon** | Same — Console only | **Closed 2026-10-17** — same answer; §7 |
| U-4 | **The 1024×500 feature graphic** | Same — Console only | **Closed 2026-10-17** — same answer; §7 |
| U-5 | **Any store screenshot that is not a plain capture of this app** | Same — Console only | **Closed 2026-10-17** — the listing screenshots are plain captures the studio recaptures after the candy backdrop lands (Q-016); §7 |
| U-6 | Per-coordinate enumeration of §5b | The resolved release runtime classpath has not been listed (Gradle deliberately not run on this branch) | `./gradlew :app:dependencies --configuration releaseRuntimeClasspath`, on the release branch, output recorded in the release report |

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
  work, and every dependency is Apache-2.0 or a permitted Google SDK. The two bundled font
  families carry OFL-1.1's notice condition; it is met by the stand-alone licence files in the
  artifact (§2), so **nothing shipped carries an unmet attribution obligation** and this is not a
  legal blocker for 3.0.0 — it is a studio-rule gap. **Routed, not fixed here:** either a Settings
  licences row (a UI change: a developer with a ux-designer review) or a licences *page* on the
  developer website linked from the listing (the rule says "screen or page"; website work is
  founder-only); the producer decides which, and whether it is pulled into 3.0.0 or left for
  3.0.1. **Update 2026-10-18:** a Settings › About › Licenses screen is specified on
  `feature/candy-backdrop` (`docs/ux/licences-screen.md`) and is planned for 3.0.0, not built yet.
  This bullet is closed when that branch merges; if it does not reach `release/3.0.0`, the gap stays
  open as written above.
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
