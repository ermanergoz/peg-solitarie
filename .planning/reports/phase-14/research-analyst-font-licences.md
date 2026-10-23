# research-analyst report — phase-14 font licences (OFL-1.1 verdict; Apache-2.0 text and NOTICE files)
Date: 2026-10-18
Branch: feature/phase-14-release-docs (eadf23c..eadf23c)
Skills loaded: ergoz-soft:standards (role research-analyst). The product snapshot is 0.5.1, so the plugin's 1.0.0 rule files were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and ip-policy, plus the product's `.claude/rules/lessons.md`. Also research-open-source, research-competitive and market-research, which the role's REQUIRED SKILLS list. Their AskUserQuestion intake and `artifacts/research/` output were not used: only the producer addresses the founder, and this report is the record.
Ran in parallel with: phase-15 H-3 cold-start sitting, candy-backdrop tech-lead ruling, tutorial haptics ruling
Ran on: claude-opus-5-5 at effort xhigh
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
- **Item 1, OFL-1.1:** CLEAN. Bundling Fredoka v17 and Nunito v32 unmodified in the APK/AAB is allowed for a store product. It is not a copyleft escalation, and every condition is met. The verdict is in THIRD_PARTY.md §2, and both font rows now read CLEAN.
- **Item 2, Apache-2.0 text:** CLEAN. The licence text may ship verbatim as `assets/licenses/Apache-2.0.txt`. One Apache NOTICE file must also travel with the app: `jakarta.inject-api` 2.0.1's `META-INF/NOTICE.md`. Recorded in THIRD_PARTY.md's Apache-2.0.txt row.
- **New finding (open issue 1):** the last recorded release build's dependency graph has families that are in neither THIRD_PARTY.md §5b nor the Licenses screen spec. Among them are BSD-3, MIT and MPL-2.0 items whose notices have to be shown. None needs a founder question under the matrix.

## Verdicts
VERDICT 1 — OFL-1.1, Fredoka v17 (Medium, SemiBold) and Nunito v32 (Regular, SemiBold, Bold), bundled unmodified in `res/font/`: **CLEAN. Allowed in a store product; not a copyleft escalation.** OFL-1.1 has no row in the matrix of `rules/ip-policy.md`. Its spirit puts it beside the MPL-2.0 row, "yes (file-level copyleft)": the copyleft in §5 binds the font files and never the program around them (OFL-FAQ 1.3). OFL-FAQ 1.4 names "games and entertainment software, mobile device applications" as permitted bundles. The conditions, and how 3.0.0 meets each:
- **§1, no sale of the fonts by themselves:** met. They ship only inside the app.
- **§2, the copyright notice and the licence in every copy:** met by `assets/licenses/Fredoka-OFL.txt` and `Nunito-OFL.txt`, stand-alone files whose licence body equals the official OFL.txt. The files' `name` tables also carry the copyright (nameID 0) and an OFL pointer (nameIDs 13 and 14).
- **§3, Reserved Font Names:** neither family declares one, and the files are byte-identical to the downloads, only renamed.
- **§4, the authors' names are not used to promote the app:** met. The Licenses screen's acknowledgement is the use §4 allows.
- **§5, the fonts stay under OFL-1.1:** met. Images made with the fonts are outside it.

Showing the fonts on a screen is good practice under OFL-FAQ 1.20, not a duty.

VERDICT 2 — the Apache License 2.0 text as `assets/licenses/Apache-2.0.txt`: **CLEAN. It may be redistributed verbatim.** The ASF's FAQ says: "You may re-use our license unchanged". Only an altered text loses the Apache name, and re-flowing its lines on screen (licences-screen.md LS-07) is one of the FAQ's own cosmetic examples. **NOTICE files: yes, one must be reproduced.** `jakarta.inject:jakarta.inject-api:2.0.1` carries `META-INF/NOTICE.md`. Apache-2.0 §4(d) asks that its attribution notices travel with the app in one of three places: a NOTICE file shipped with it, its documentation, or its notices display. Today that copy survives packaging only because AGP's default excludes miss the `.md` extension. Recommended: bundle it verbatim as `assets/licenses/Jakarta-Inject-NOTICE.md` and open it from its own Licenses entry. The scan found no other Apache NOTICE file. OkHttp's `publicsuffix/NOTICE` is an MPL-2.0 data notice, not an Apache one (open issue 1).

## Evidence
**Font files** (on `feature/phase-15-ux-refresh`, HEAD e9c870b):
- All five `res/font/*.ttf` sha256 values equal `tech-lead-fonts.md` D1: `8143bd54…`, `021e30b2…`, `b0885056…`, `c7fd1944…` and `ad443e47…`, with byte sizes as D1.
- `Fredoka-OFL.txt` is 4,482 B, sha256 `782c313774e59f7f0e2face680137ad0b578822ba1582160f30cb333cf31f7e0`.
- `Nunito-OFL.txt` is 4,479 B, sha256 `66db16f4d402431d22c354c3e7c740ab550f1ba1ef7cd7b70ff2ba13829734a2`.
- Each OFL text's first line is its family's copyright: "Copyright 2016 The Fredoka Project Authors (https://github.com/hafontia/Fredoka-One)" and "Copyright 2014 The Nunito Project Authors (https://github.com/googlefonts/nunito)". Neither line has a "with Reserved Font Name" clause.
- Below line 1 the two texts are identical. With whitespace normalised, the licence body equals https://openfontlicense.org/documents/OFL.txt (4,052 characters each).

**`name` tables** (parsed from the files):
- nameID 0 holds the same copyright line.
- nameID 13 reads "This Font Software is licensed under the SIL Open Font License, Version 1.1. …".
- nameID 14 is the OFL URL.
- Versions: Fredoka 2.001, Nunito 3.602.

**OFL primary sources:**
- Text: https://openfontlicense.org/open-font-license-official-text/ (page "Last updated on 12 January 2026").
- FAQ: https://openfontlicense.org/ofl-faq/ (1.1-update7, November 2023).
- Quoted answers: 1.3 (the program need not be OFL), 1.4 (sale within software; mobile apps and games named), 1.6 (why the fonts cannot be sold alone), 1.10 and 1.20 (notice and licence text required; an About mention is "good practice"), 5.7 (no names reserved by default) and 5.9 (a wrapper that leaves the font intact needs no rename).

**Apache primary sources:**
- Licence text: https://www.apache.org/licenses/LICENSE-2.0.txt, 11,358 B, sha256 `cfc7749b96f63bd31c3c42b5c471bf756814053e847c10f3eb003417bc523d30` on 2026-10-18. The same bytes are served at `/licenses/LICENSE-2.0`.
- FAQ: https://www.apache.org/foundation/license-faq.html, "May I re-use (and modify) the Apache License 2.0 itself?".
- ASF guide: https://infra.apache.org/licensing-howto.html: "If the dependency supplies a NOTICE file, its contents must be analyzed and the relevant portions bubbled up into the top-level NOTICE file."
- Clauses used: Apache-2.0 §4(a) (give recipients a copy of the licence) and §4(d) (NOTICE).

**NOTICE scan** (no Gradle run):
- Graph: `/Users/ermanergoz/Desktop/PegSolitarie/app/build/intermediates/metadata_library_dependencies_report/release/collectReleaseDependencies/dependencies.pb`, AGP's release dependency report of 2026-09-15. Decoded, it lists 192 coordinates.
- Artifacts: each coordinate's jar or AAR, and the classes.jar inside each AAR, from `~/.gradle/caches/modules-2/files-2.1`. 141 files were found and scanned. 51 coordinates are no longer in the cache: AndroidX, Compose, DataStore, Room, Navigation, the kotlinx BOM and metadata modules, Okio, and `kotlin-stdlib-common`.
- Found: `jakarta.inject-api-2.0.1.jar!/META-INF/NOTICE.md` (1,074 B, sha256 `992ed62dd3ee…a512214df540`) and `okhttp-4.11.0.jar!/okhttp3/internal/publicsuffix/NOTICE` (218 B). Every Google SDK AAR has `third_party_licenses.txt/.json` at its root (open issue 2).
- In the APKs: both NOTICE files are present, byte-identical, in the main checkout's `app/build/outputs/apk/debug/app-debug.apk` (2026-09-28) and in `merged_java_res/release/minifyReleaseWithR8/base.jar` (2026-09-21).
- AGP 9.1.0's `PackagingOptionsUtilsKt` default excludes are `/META-INF/NOTICE`, `/META-INF/NOTICE.txt`, `/NOTICE`, `/NOTICE.txt`, `/META-INF/LICENSE`, `/META-INF/LICENSE.txt`, `/LICENSE` and `/LICENSE.txt`. That is why the jar's `META-INF/LICENSE.txt` is dropped and its `NOTICE.md` is kept.

## Outputs
- `THIRD_PARTY.md` §2: the OFL-1.1 verdict paragraph replaces the "not recorded yet" note. Both font rows' How-verified cells change from **pending** to **CLEAN**. The Apache-2.0.txt row's How-verified cell gains the text confirmation, the URL's sha256 and the NOTICE answer. Commit eadf23c.
- `.planning/reports/phase-14/research-analyst-font-licences.md`: this report.

## Checks
- `shasum -a 256 …/PegSolitarie-p15/app/src/main/res/font/*.ttf …/assets/licenses/*`: pass (all five fonts equal D1; OFL hashes as above)
- `python3 -I nametable.py …/res/font/*.ttf` (a name-table parser in the session scratchpad): pass (copyright, licence description and URL present; no RFN anywhere)
- `curl -sL https://openfontlicense.org/documents/OFL.txt`, then a whitespace-normalised comparison with both shipped texts: pass (equal: True, True)
- `curl -sL` of the OFL text page, the OFL-FAQ, LICENSE-2.0.txt, the Apache licence FAQ and the ASF licensing how-to: pass (all 200; read; quoted above)
- `python3 -I notice_scan.py deps-sep15.txt` (192 coordinates; 141 artifacts scanned): pass (2 NOTICE files, as listed)
- `unzip -l …/app-debug.apk` and `…/minifyReleaseWithR8/base.jar | grep -iE 'notice|license'`: pass (both NOTICE files ship; BSD-3 protobuf LICENSE.txt present; see open issue 1)
- `unzip -p <jar> META-INF/NOTICE.md | shasum -a 256` against the APK copy: pass (identical)
- `curl -sL https://developers.google.com/android/guides/opensource`: pass (200, "Last updated 2026-10-15"; quoted in open issue 2)
- `git -C /Users/ermanergoz/Desktop/PegSolitarie-r5 commit -F - THIRD_PARTY.md`: pass (eadf23c)
- No Gradle and no build were run, as dispatched.

## Open issues / risks
1. **THIRD_PARTY.md §5b and the Licenses screen spec miss families on the release graph (U-6 is now real).**
   - **Where the list comes from:** the 2026-09-15 release dependency report. It is evidence, not the 3.0.0 graph, which checklist step 6 confirms.
   - **Listed in neither file:**
     - Apache-2.0 per their publishers, not verified here: `com.squareup.okhttp3:okhttp` 4.11.0, `com.squareup.okio` 3.9.1, `com.google.code.gson` 2.13.2, `com.google.crypto.tink:tink-android` 1.20.0, Guava 32.0.0-jre with failureaccess, j2objc-annotations, jsr305, `org.jetbrains:annotations`, `org.jspecify`, `jakarta.inject-api` and `javax.inject`.
     - BSD-3-Clause: `com.google.protobuf:protobuf-javalite` 3.22.3, plus `org.chromium.net` cronet-* 143 (Chromium).
     - MIT: `org.checkerframework:checker-qual` 3.33.0.
     - MPL-2.0 data: OkHttp's `publicsuffixes.gz`, with its NOTICE.
     - Google SDKs: `com.google.android.play:hsdp` 2.0.1 (licence unverified, like U-1), plus play-services-appset, -ads-identifier and -cronet.
   - **Where most come from:** the Mobile Ads SDK's `third_party_licenses.json` lists okhttp4, Okio, Cronet, Gson, Tink, Guava and Jakarta Inject.
   - **Matrix reading:** BSD and MIT are "yes, keep notice". MPL-2.0 is "yes"; the file ships unmodified and its NOTICE names the source. So no escalation is expected.
   - **What goes wrong without a fix:** BSD-3 and MIT require their notices "in the documentation and/or other materials provided with the distribution". The datastore-repackaged protobuf `LICENSE.txt` in the APK has the BSD conditions but no copyright line. The Licenses screen therefore needs entries for these families. THIRD_PARTY.md §7 says "every dependency is Apache-2.0 or a permitted Google SDK", and §5b says "no licence risk hides in the gap". Both are now known to be incomplete about notice duties. Not edited here (outside this dispatch).
   - **Recommendation:** dispatch a research-analyst verdict per family against the release-branch listing (or now, from this report's list), and a ux-designer amendment of `licences-screen.md` §3.2.
2. **Notices bundled inside Google's SDKs.** Each Google AAR ships `third_party_licenses.txt` at its root. AGP never packages these files. The sizes are 1.45 MB for the Mobile Ads SDK (98 components), 789 KB for UMP (59) and 815 KB for hsdp (70). Google's guide (https://developers.google.com/android/guides/opensource) says: "you as a developer are responsible for appropriately displaying the notices for the open source libraries that your app uses". Its plugin's list "includes any open source libraries that are used to create the Google Play services libraries". The spec's inert "Google libraries" entry, under LS-03, does not display these notices. Recommendation: a tech-lead ruling, with a ux-designer amendment, between two options. (a) Bundle those texts and open them from the Google entry. (b) Revisit LS-03 and adopt the oss-licenses data. Risk: medium for the studio rule, low for enforcement.
3. **The Jakarta NOTICE needs a spec change.** `licences-screen.md` §3.2 and §7, and LS-AC11, should name `assets/licenses/Jakarta-Inject-NOTICE.md` and its entry. Suggested entry: "Jakarta Dependency Injection" · "Eclipse Foundation" · "Apache License 2.0", opening the NOTICE and the Apache text. This is the ux-designer's file, on `feature/candy-backdrop`.
4. **Add OFL-1.1 to the matrix.** OFL-1.1 should get its own row in `rules/ip-policy.md`. Proposed: "OFL-1.1 | — | yes (fonts) | font-level copyleft; ship notice + licence; never sold alone; RFN on modified fonts". This goes through a retro item (rules/memory.md), not this branch.
5. **Commit trailer.** `rules/gitflow.md` (plugin 1.0.0) requires exactly `Co-Authored-By: Claude Fable 5.1`, and eadf23c carries it. This work ran on Opus 5.5, so the trailer and `Ran on:` differ by rule. The release-manager noted the same.

## Handoff
Next: producer —
- **release-manager:** THIRD_PARTY.md §2's OFL verdict is in, so the merge blocker in `release-manager-store-copy.md` open issue 1 is closed for this branch. At `release/3.0.0` (checklist steps 1.4 and 6), confirm the AAB carries all five fonts, both OFL texts, `Apache-2.0.txt` and the Jakarta NOTICE asset if adopted. Also scan the release listing's 51 unscanned coordinates for NOTICE files.
- **research-analyst (new dispatch):** verdict rows for the families in open issue 1. Once the listing exists, settle hsdp's licence the same way as U-1.
- **ux-designer:** `licences-screen.md` amendments for open issues 1 and 3.
- **tech-lead:** a ruling on the Google SDK notices (open issue 2).
- **developer (when the screen is built):** add `assets/licenses/Jakarta-Inject-NOTICE.md`, verbatim from the jar, sha256 `992ed62d…a512214df540`.
- **Founder:** nothing.
