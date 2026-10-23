# release-manager report — release-docs-font-rows
Date: 2026-10-21
Branch: feature/phase-14-release-docs (9c0fae7..9c0fae7)
Skills loaded: ergoz-soft:standards (role release-manager; plugin rules 1.0.0, read because the snapshot is 0.5.1: studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md, ip-policy.md; .claude/rules/lessons.md)
Ran in parallel with: none
Ran on: opus at effort medium
Fan-out: none
Loops: 0
Lessons applied: none

## Summary
Cleared the last "Not met" merge precondition in `tech-lead-design-close.md` for this branch.
Phase 15 merged into `develop` at c9228a9, so `develop` no longer carries `font_certs.xml` or
`ui-text-google-fonts`. I deleted both THIRD_PARTY rows as `tech-lead-fonts.md` (§3/§4 items) specifies.
The file has no convention of keeping deleted rows; to avoid an empty table, §3 now has a dated one-line note saying the section is empty.
I also refreshed the stale "fonts on an unmerged branch" notes in the header table and the §2 font paragraph. Docs only, no Gradle.

## Outputs
- `THIRD_PARTY.md`: header table: the font row is removed; the lead-in now says phase 15 merged at
  c9228a9, and the "develop holds" column now points at 0582e3f (it pointed at 3f2af90). §2 font paragraph: the rows are now on `develop` since
  c9228a9; the OFL conditions line notes that the same blobs are on `develop`. §3: the `font_certs.xml` row is deleted and
  "None." is added with the date and the D5 reference. §4: the `ui-text-google-fonts` row is deleted.
- `.planning/reports/phase-14/release-manager-font-rows.md`: this report.

## Checks
- `awk '/^## 3\./,/^## 5\./' THIRD_PARTY.md | grep -c -E 'font_certs|ui-text-google-fonts'`: pass (output `0`)
- `git -C /Users/ermanergoz/Desktop/PegSolitarie-dev merge-base --is-ancestor c9228a9 develop`: pass (c9228a9 is on develop; develop head 0582e3f)
- `git -C /Users/ermanergoz/Desktop/PegSolitarie-dev ls-files | grep -i -E 'font_certs|fonts'`: pass (only `.planning/reports/15-founder-feedback/tech-lead-fonts.md` and `ui/theme/AppFonts.kt`; no `font_certs.xml`)
- `git -C …/PegSolitarie-dev ls-tree -r --name-only develop | grep -i -E 'font_certs|res/font|licenses'`: pass (the five `res/font/*.ttf` and `assets/licenses/{Fredoka,Nunito}-OFL.txt`; no `font_certs`)
- `git -C …/PegSolitarie-dev grep -n -i 'google-fonts' develop -- gradle/libs.versions.toml app/build.gradle.kts`: pass (exit 1, no hits)
- `git -C …/PegSolitarie-dev diff c9228a9^1 c9228a9 -- app/build.gradle.kts gradle/libs.versions.toml`: pass (the merge's only dependency change is the removal of `ui-text-google-fonts`, so §4 needs no other edit)
- `diff <(git ls-tree -r develop app/src/main/res/font app/src/main/assets/licenses) <(git ls-tree -r b5836ac …)`: pass (identical blobs, so the 2026-10-18 hash and OFL checks still hold on develop)
- `git -C …/PegSolitarie-dev ls-tree -r develop` and `merge-base --is-ancestor d1c6e9b|af2b554|3a1fce7 develop`: pass (the audio commits are not on develop and there is no `feature/candy-backdrop` content, so those two table rows stay as they are)

## Open issues / risks
- `tech-lead-fonts.md` AC-F4 (`git grep … googlefonts … -- app gradle` returns 0 hits) is not literally true on
  develop. `app/src/main/assets/licenses/Nunito-OFL.txt:1` contains the upstream URL
  `https://github.com/googlefonts/nunito`, which the OFL requires. The hit is harmless, but the AC's pattern is
  wrong. This belongs to the tech-lead, not to this task.
- The audio and candy-backdrop rows still describe unmerged branches. Re-cut them at checklist step 1.4 if either branch misses `release/3.0.0`.

## Handoff
Next: producer — this branch's THIRD_PARTY precondition in `tech-lead-design-close.md` is now met (9c0fae7). Route the
AC-F4 pattern note to the tech-lead if you want it fixed.
