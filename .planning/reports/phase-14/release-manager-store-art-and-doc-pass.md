# release-manager report — phase-14 store-art provenance and doc pass
Date: 2026-10-17
Branch: feature/phase-14-release-docs (070d1d2..this commit; the work and this report share one commit, as dispatched); feature/phase-14-release-plan in the primary checkout (c3c8605)
Skills loaded: ergoz-soft:standards (plugin rules v1.0.0 read in full because the snapshot is v0.5.1: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, ip-policy; `.claude/rules/lessons.md`), gitflow
Ran in parallel with: phase-15 qa-engineer loop 3, candy-backdrop ux spec
Ran on: claude-opus-5-5 at effort medium
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot lists 0 active lessons; `.planning/recall/RECALL.md` does not exist)

## Summary
THIRD_PARTY.md §7 now holds own-work rows for the 20 achievement icons, the 7 leaderboard icons, the listing icon, the feature graphic and the screenshots, with each file's sha256 and size. The rows rest on the founder's answer to Q-017. U-2..U-5 are closed. U-7 is replaced by the candy-backdrop idea row in §1, marked as shipping from `feature/candy-backdrop` (not yet built).
Release checklist 1.2, 3.1 and 3.2 now match tech-design §16 item 9. Kover joins the build-tooling row as pending `feature/phase-14-check-gates`.
No achievement or leaderboard name or description hits the §3a words rule.
In the primary checkout, the stale text in the operator checklist and the release plan is corrected (signingConfig, fonts, audio, upload branch).

## Outputs
- `THIRD_PARTY.md` (r5): new §7 with own-work rows and per-file hash tables (27 icons and 2 listing assets). It records how the files were checked and what is in the art. U-2..U-5 marked closed. U-7 removed. Candy-backdrop idea row added to §1 (text from the verdict's §6, plus the branch marker). Candy-backdrop added to the header's unmerged-branches table. "kover (pending feature/phase-14-check-gates)" added to §0's build-tooling row. No literal `PENDING FOUNDER` is left, so checklist 1.4's test can pass.
- `docs/release-checklist.md` (r5): 3.1 → `FUZZ_SEEDS=100000 ./scripts/check.sh` (marked unticked until the check-gates branch lands); 3.2 → `domain/build/fuzz-failures.txt`; 1.2 aligned to the same command, because it runs the same gate on develop. 3.3 already said "record the measured figure".
- `.planning/phases/14-release-readiness/14-OPERATOR-CHECKLIST.md` (primary, c3c8605): the intro and Step 7 now say the signingConfig is on develop (9ca3829, `verifyReleaseSigning` backstop). Step 9 is marked done via Q-017, with the bundled-font, WAV and in-house facts corrected.
- `.planning/phases/14-release-readiness/14-RELEASE-PLAN.md` (primary, c3c8605): T-11 now covers bundled fonts. §6 now has 12 WAVs (7 Kenney + 3 freesound-derived + 2 in-house) and the five bundled OFL files, with a pointer that the store-art rows are closed. §2c now says: Check via `scripts/check.sh`, the branch created alone, versionCode already 30_000, merge and tag **before** Gate 2 (skills/release), and the upload run from `feature/publish-<X.Y.Z>-done` (skills/publish steps 3 and 5). §4a item 1 is marked done (9ca3829).
- This report.

### Store art: what was found
- **Hashes and sizes:** all 20 achievement PNGs and all 7 leaderboard PNGs are 512×512 RGBA. The icon is 512×512 and the feature graphic is 1024×500. The full sha256 list is in THIRD_PARTY.md §7. Icon `d6cf915d…34335`, feature graphic `bfaa1ba1…b03e4`.
- **How they were made (evidence found):** for the listing icon, `.planning/quick/260914-x4a-…/260914-x4a-PLAN.md` (5e99af1, 2026-09-14) names the approved launcher design canvas (`Main.dc.html`, a studio session's scratchpad, since gone). The plan says the Play 512×512 PNG "the user exports … from the design canvas separately"; the icon matches that design (English-33, 32 pink pegs, empty centre, violet gradient). The feature graphic's XMP records a 1024×1024 original, and its quarantine tag names Preview, so it was cropped locally. The achievement and leaderboard PNGs carry bare chunks (IHDR/IDAT/IEND) and a Chrome quarantine tag. No `kMDItemWhereFroms` URL is recorded on any file, so none was downloaded from a web page. No repo or `.planning/` record describes drawing the achievement, leaderboard or feature-graphic art. The founder's answer is the provenance for those.
- **Third-party content check:** each image was inspected (two contact sheets, plus both listing assets at full size). No logo, character, photograph, stock texture, watermark or third-party mark appears. The feature graphic's text and the achievement digit pills use rounded faces that look like Fredoka and Nunito (OFL-1.1). OFL does not reach images rendered with the font. The pictograms (stopwatch, bulb, undo arrow, stars) are generic. If one was modelled on Material Symbols, that set is Apache-2.0.

### §3a words check (achievements and leaderboards)
All 20 achievement names and descriptions (A01 First Clear … A20 Perfectionist) and all 7 leaderboard names and descriptions (L1 Challenge Ladder … L7 Wiegleb-45 Best Score) were checked, from `12-OPERATOR-CHECKLIST.md` lines 88–146. The app holds only their IDs; there are no names in `res/values`. The check was case-insensitive and also matched inside other words: candy, crush, saga, sugar, candyland, candy land, tiffi, toffee, yeti, odus, king, activision, microsoft, sweet, tasty, delicious, divine. **Zero hits.** Neither feature-graphic string ("Peg Solitaire", "Classic boards, endless fun!") is a hit either. `strings.xml`, comments excluded, also has zero hits.

## Checks
- `shasum -a 256` and `sips -g pixelWidth -g pixelHeight -g hasAlpha` over the 29 Desktop files: pass (29 hashes recorded; dimensions as above)
- PNG chunk read (`python3 -I` scratchpad script), `mdls -name kMDItemWhereFroms`, `xattr -p com.apple.quarantine`: pass (no generator or URL metadata; XMP 1024×1024 on the feature graphic; quarantine Chrome on the icon sets, Preview on the feature graphic)
- `ffmpeg … tile=5x4` and `tile=4x2` contact sheets in the scratchpad, then each image viewed: pass (no third-party element seen)
- `git -C …/PegSolitarie log --all --oneline -i --grep=…` and `grep -rn` over `.planning` and `docs`, excluding `.planning/log`: pass (launcher-canvas evidence 5e99af1 and c96d0c0; nothing for the other art)
- `sed -n '88,146p' 12-OPERATOR-CHECKLIST.md | grep -i -o -E '<§3a words>'`: pass (exit 1, no match)
- `git show develop:app/build.gradle.kts | grep -n 'signingConfig\|register("verify'`: pass (signingConfigs block from 9ca3829; `verifyReleaseSigning` registered)
- `git show develop:app/build.gradle.kts | grep versionCode`: pass (`30_000`)
- `grep -c 'PENDING FOUNDER' THIRD_PARTY.md`: pass (0)
- `git diff | grep '^+.*/Users/'` in both checkouts: pass (no absolute paths entered tracked files; checklist 3.6)
- `./gradlew check`: not run (documentation only, as dispatched)

## Open issues / risks
1. **Agent-made art may carry no copyright of its own** (Thaler; USCO Part 2, per the candy-backdrop verdict §4c). Others could copy the icons. This is not an infringement risk and not a licence gap. Recorded in THIRD_PARTY.md §7.
2. **No source files for the store art.** The canvases were session scratch and are gone. If an icon has to be redrawn or resized later, it starts from the PNGs. The founder's Desktop files are the only copies outside the Console, and the hashes identify them.
3. **The screenshots row depends on a future capture.** It holds only if the recapture after the candy backdrop is plain (verdict §3d). Re-check at listing time.
4. **Checklist 1.2/3.1 cannot be ticked until `feature/phase-14-check-gates` lands** (`scripts/` is empty on develop). This is said in the item text.
5. **If a Kenney *Platformer Art Candy* file is used for the backdrop,** the verdict's §6 also requires a §2 asset row. It is not added now, because nothing is built.
6. **The publish gate matches on the script name in command text.** An edit command that only contained the upload script's name in replacement text was blocked. Nothing ran; the edits went through the Edit tool instead. This is gate friction for the retro (F-candidate), not a defect in the gate.
7. **Commit trailer:** written by hand as the single `Co-Authored-By: Claude Fable 5.1` line that `rules/gitflow.md` requires (the harness reminder asked for a different one; the project rule wins).

## Handoff
Next: producer. Merge `feature/phase-14-release-docs` and `feature/phase-14-release-plan` when their steps allow. Checklist 1.4 now depends only on U-1/U-6 (release-branch commands) and the candy-backdrop row's fate. At every listing change, the release-manager re-runs the §3a words check over the listing text.
