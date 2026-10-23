# release-manager report — phase-14 THIRD_PARTY rows, founder-items list, Step 0 item 4
Date: 2026-10-17
Branch: feature/phase-14-release-docs (committed by the producer: `THIRD_PARTY.md`, `.planning/reports/phase-14/release-manager-founder-items.md`, this report); feature/phase-14-release-plan (0ad2ad6, one commit: `14-OPERATOR-CHECKLIST.md`)
Skills loaded: ergoz-soft:standards (role: release-manager). Rule files: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md and memory.md read as the plugin 1.0.0 copies, because the snapshot is 0.5.1. puzzle-design.md, play-store.md and ip-policy.md read from the snapshot, which is byte-identical to the plugin. kotlin-android.md and compose.md from the 0.5.1 snapshot; they are not used by this docs task. Also gitflow, used under the studio's overrides: conventional commits, `.planning/` committed. gsd-complete-milestone was **not invoked**: no milestone is closing, and invoking it would start the archive workflow.
Ran in parallel with: phase-15 code-auditor and qa-engineer loop 3, candy-backdrop IP verdict, phase-14 tech design
Ran on: claude-opus-5-5 at effort medium
Fan-out: none
Loops: 0
Lessons applied: none (`.claude/rules/lessons.md`: 0 active lessons; no `.planning/recall/RECALL.md`)

## Summary
(A) `THIRD_PARTY.md` now describes what 3.0.0 ships. It has twelve per-file WAV rows (seven
Kenney, three freesound, two in-house) and two bundled-font rows (Fredoka v17, Nunito v32,
OFL-1.1). A table at the top names the branch that holds each change today. The OFL §2 notice
condition is met in the artifact by the two stand-alone `assets/licenses/*-OFL.txt` files on
`feature/phase-15-ux-refresh`, so it is **not** a release blocker. The licences-screen gap is a
studio-rule gap, routed below. (B) One founder list: 14 items with state and evidence, Q-F1/F2/F5
marked settled, and two decision questions plus the Gate 2 skeleton drafted. (C) Step 0 item 4
corrected and committed (0ad2ad6).

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-r5/THIRD_PARTY.md` — uncommitted, for the producer. Changes:
  - a header table naming the branch of each unmerged asset change;
  - §2: the opening paragraph rewritten, the 4 audio rows replaced by 12 per-file rows from the
    research-analyst's delta, the font row replaced by two bundled-font rows, the CC0 paragraph
    replaced, and an OFL-1.1 obligations paragraph added (§1–§5, how each is met);
  - §3 `font_certs.xml` and the §4 `ui-text-google-fonts` row marked "does not ship in 3.0.0"
    (deleted in b5836ac);
  - §6 a new U-7 row for the candy backdrop (verdict pending);
  - §7 the licences-screen bullet reworded per the research-analyst's and tech-lead's text, with a
    website-page option added.
- `/Users/ermanergoz/Desktop/PegSolitarie-r5/.planning/reports/phase-14/release-manager-founder-items.md` — uncommitted. The consolidated founder list F-1..F-14, the Q-F1..Q-F5 settlement table, the drafted questions, and the studio-only blockers.
- `/Users/ermanergoz/Desktop/PegSolitarie/.planning/phases/14-release-readiness/14-OPERATOR-CHECKLIST.md` — Step 0 item 4 corrected, committed 0ad2ad6 on feature/phase-14-release-plan. It is the diagnosis's proposed diff, updated to match the diagnosis's own follow-up: the field is now set to `ermanergoz.github.io`, so the item says "leave it alone" and keeps "switch to the apex" only as the 48 h fallback.
- This report.

## Checks
- `git -C …/PegSolitarie-r5 diff --check` — pass (no whitespace errors). `git status --short` showed only `THIRD_PARTY.md` modified before the two new report files.
- `shasum -a 256 app/src/main/res/font/*.ttf` (p15) — pass (all five equal `tech-lead-fonts.md` D1)
- `ls app/src/main/assets/licenses/` + `head` (p15) — pass (`Fredoka-OFL.txt` 4,482 B, `Nunito-OFL.txt` 4,479 B; each has its copyright line and the full OFL 1.1 text; §1/§2/§5 read from it)
- `unzip -l app/build/outputs/apk/debug/app-debug.apk | grep -E 'res/font/|assets/licenses/'` on the debug APK already on disk (p15; nothing built) — pass (5 fonts + 2 licences packaged). The AAB check (AC-F5) stays a release-branch item.
- Python `struct` read of the `name` table, `fredoka_medium.ttf` and `nunito_regular.ttf` — pass (nameID 0 = the copyright line, 13 = OFL 1.1 statement, 14 = OFL URL)
- `git grep -E 'googlefonts|GoogleFont|font_certs|ui-text-google-fonts' -- app gradle` (p15) — pass (one hit, the Nunito upstream URL inside the OFL text; `font_certs.xml` absent)
- `ls app/src/main/res/raw/` (audio worktree) — pass (12 `.wav`, names match the delta rows)
- `git log --oneline develop -40` — read; 08b1b27/2397732 (versionCode 30000) and 3f2af90 (rules 0.6.2) are the newest; no kover/fuzz commit anywhere (`git log --all | grep -iE 'kover|fuzz'`, 0 relevant hits)
- `git ls-tree -r --name-only develop | grep -E '^scripts/|^docs/store|CHANGELOG|THIRD_PARTY|release-checklist'` — 0 hits (none of the release files is on develop yet)
- `curl https://ermanergoz.com/policies/peg-solitarie-privacy-policy.html` — pass (`200 0`; "Last updated: September 21, 2026"; names AAID, GMA/AdMob, UMP, Play Games, saved games in Drive, the deletion route, under-13 statement)
- `curl https://ermanergoz.com/app-ads.txt` / `-L https://ermanergoz.github.io/app-ads.txt` — pass (`200 0` / `200 1 → canonical`)
- `curl -L 'https://play.google.com/store/apps/details?id=com.erman.pegsolitarie&hl=en&gl=US'` — pass (200; Website row `https://ermanergoz.github.io` still live)
- `awk` over `local.properties` key names and value *shapes* only (no value printed) — pass (4 ad/PGS keys set, none `REPLACE_ME`, none with Google's test publisher, 28 PGS keys)
- `grep '^## Q-' .planning/questions/FOUNDER.md` in 4 checkouts — pass (Q-F1..Q-F5 never filed)
- `git log -1` on feature/phase-14-release-plan — pass (0ad2ad6, one file)
- Not run, as the task requires: no Gradle task, no build, no adb, no upload, and no approval file.

## Open issues / risks
- **Licences surface — studio-rule gap, not a release blocker (routed to the producer).** OFL-1.1
  §2 asks that each copy *contain* the notice and licence, as stand-alone files, headers or
  metadata. It does not ask that they be displayed, and the two `assets/licenses/*-OFL.txt` files
  meet it. So something in the app does meet the obligation, once `feature/phase-15-ux-refresh`
  merges. Two caveats:
  - If that branch does not reach `release/3.0.0`, the fonts are not bundled and no obligation
    exists.
  - If anyone drops `assets/licenses/` while keeping `res/font/`, the obligation goes **unmet**.
    That would be a blocker; `tech-lead-fonts.md` AC-F5 checks it on the AAB.

  What remains unmet is `rules/ip-policy.md`'s "attribution … in the product's licenses screen or
  page". The producer chooses between a Settings licences row (developer + ux-designer) and a
  licences page on the developer website (founder-only website work), and decides whether it is
  in 3.0.0.
- **Gradle-free claims to verify at the cut:**
  - the AAB keeps the five fonts and both licence texts (AC-F5, `bundletool dump resources`);
  - U-1 (UMP licence) and U-6 (`releaseRuntimeClasspath` listing) are still open;
  - U-7: the candy backdrop rows wait for its IP verdict.
- **Branch dependency of the new rows.** The audio and font rows are correct only if both
  `feature/audio-review` and `feature/phase-15-ux-refresh` reach develop before the cut. The audio
  branch's `check` is red until `AudioCreditsTest` is updated. The header table says which rows
  to re-cut if either branch slips.
- **Stale text on feature/phase-14-release-plan, not edited** (the task allowed one item in one
  file):
  - operator checklist Step 7 says "No `signingConfig` exists"; it exists on develop (9ca3829);
  - Step 9 says "the fonts are Google Fonts downloadable families";
  - `14-RELEASE-PLAN.md` T-11 and §6 still describe downloadable fonts and the 9/3 OGG split;
  - §2c says to run the upload from develop, while the plugin procedure now says
    `feature/publish-<X.Y.Z>-done`.

  These need one docs pass when that branch is next touched.
- **Privacy-policy wording:** the policy says "Manage ad consent", but the app row is "Manage
  privacy choices". This is founder item F-6, a website edit.
- **Commit trailer:** 0ad2ad6 carries `Co-Authored-By: Claude Opus 5.5`, as the harness asked. The
  0.5.1 snapshot names no trailer. The plugin 1.0.0 gitflow rule names a Fable trailer, which its
  prepare-commit-msg hook would add, but this repo's hook did not rewrite it. Producer: amend it if
  the plugin rule should govern.

## Handoff
Next: producer — commit `THIRD_PARTY.md` and the two report files on
`feature/phase-14-release-docs`. Put F-1 (overdue), F-2 and F-3 in front of the founder first,
because they have waits that cannot be shortened. Number and file the two drafted questions. Rule
on the licences surface.
Next: research-analyst — the OFL-1.1 verdict for the two font families, if the producer wants it
on record beyond this reading. Add OFL-1.1 to the ip-policy matrix as a plugin retro item, as
tech-lead-fonts.md proposes.
Next: release-manager (at the cut) — AC-F5 AAB check, U-1/U-6/U-7, and a docs pass on the stale
release-plan text listed above.
