# code-auditor report — release-3.0.0: loop-1 re-check of the three 3.0.0 docs branches
Date: 2026-11-01
Branch: feature/release-3.0.0-prep (report only). Re-checked against `code-auditor-docs.md` (loop 0): `feature/release-3.0.0-prep` `50cd69b..d855c51` (fixes `d54659a`); `feature/tech-design-3.0.0-sync` `5ae8421..3aa7825` (design edits `6b2f0c0`); `feature/ux-3.0.0-folds` `b4fe627..2c8b31b`. All three still branch off `develop` `d251c3b`; `develop` is now `47cb7e0`
Skills loaded: ergoz-soft:standards (role code-auditor; game; android-compose-game). The snapshot is 0.6.2 and the plugin is 1.0.0, so the plugin copies were read in full: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose and testing; also `.claude/rules/lessons.md`. code-reviewer and security-reviewer were loaded too. compose-skill and compose-performance-audit were not: no Compose code in any diff
Ran in parallel with: none known
Ran on: claude-opus-5-5 at effort high (policy). The harness names no effort. Founder directive 2026-10-31 (1) in STATUS.md sets code-auditor dispatches one step below the xhigh pin. **Deviation:** `agents/code-auditor.md` pins fable. Q-019 puts Fable-pinned roles on Opus until 3.0.0 is published, so this review is not on a model above its authors'. Every fact below was re-read in the tree
Fan-out: none
Loops: 1
Lessons applied: none (the snapshot holds 0 active lessons for game / android-compose-game)

## Summary
- **release-prep: both must-fixes are fixed.**
  - T-11, at checklist 1.7 and the §4 note, now matches the tech-lead's ruling ("bare `env …`", `play-upload/tech-lead-signoff.md:106`) and forbids every form the §5 probe table shows passing the gate (rows 3–8).
  - Both release-manager reports now have a `Ran on:` line.
  - Of the 7 minor findings, 7 are addressed. 6 small, non-blocking items remain (below).
- **tech-design sync: all 3 minor findings are fixed**, insertion-only. Its open issue 1 (DT's source not on `develop`) is closed by `47cb7e0`.
- **ux-folds: the report now has a `Ran on:` line**, so it is valid. The line's form is incomplete (minor).
- All six `git merge-tree` pairings are clean (the three branches, and each branch against `develop` `47cb7e0`). The changed-file sets are still disjoint.

## Loop-0 findings, item by item

### `feature/release-3.0.0-prep`
| Loop-0 finding | State | Evidence |
|---|---|---|
| **Must 1 — T-11 weaker than the ruling** | **fixed** | `docs/release-checklist.md:38` and `:115-120` now say "a **bare** `env …` prefix (literally the word `env`, never `/usr/bin/env`)", citing `:106`. They forbid `/usr/bin/env …`, `/usr/bin/nohup …`, `/usr/bin/time …`, `exec …`, `/bin/bash …`, `/bin/sh …` "or any other wrapper or shell named by its path", citing §5 rows 4–8. Re-read against `tech-lead-signoff.md:74-87`: every probed exit-0 form of the script's invocation (rows 3–8) falls under the new text, and the two hand-typed fastlane forms (rows 10–11) fall under TL-1 (`:136-139`). The wording is stricter than the ruling (it adds `exec …` and unprobed path-named wrappers), never looser. That is safe: T-11 already allows only `./scripts/play-upload.sh` |
| **Must 2 — no `Ran on:` line in either report** | **fixed** | `release-manager-prep.md:6` and `release-manager-docs-pass.md:6` now have one, so both reports are valid. The prep report's Branch line now has a range (`d251c3b..7d43d7f`). Form: see minor m1 |
| AGENTS.md:7 | fixed | It reads `Build command: ./gradlew :app:bundleRelease`, the same as `CLAUDE.md:7` |
| Dated Build-command notes | fixed | `14-RELEASE-PLAN.md:311-314` and `docs/release-checklist.md:60`. Each keeps the old sentence and appends a "Note 2026-11-01". Tech-design §9.5 got its note on the tds branch (below). `git grep 'Build command'` finds no remaining text that puts the change on the release branch without a note |
| READINESS self-contradictions | fixed | `READINESS.md:25` (1.7), `:37` (2.6 "done pre-cut"), `:47-49` (4.3, 4.7 and 4.8) and `:83` (the §4 lead line) now agree with the T-item table. T-7 (`:92`) lands in the checklist's §4 note. Its line citation is now 3 lines off (m2) |
| Row DT source | fixed | `READINESS.md:18` cites the producer's decision in `develop`'s STATUS.md and makes the tick wait for it on `develop`. The condition is now met: `47cb7e0` (merge of `4135e2d`) carries "Producer decision 2026-11-01: `bugfix/solver-deadline-task` (optional) is DEFERRED to 3.0.1, owner tech-lead" at STATUS.md `:11` |
| CHANGELOG-draft timer bullet | fixed | `CHANGELOG-draft.md:30-32`: "TalkBack now reads the game timer's value, in whole minutes; before, it said only 'Elapsed time'". It also says that B-2 never shipped. This matches ux-spec 18f, as quoted in loop 0 |
| D-1 install route | fixed, sound with caveats (m3) | `READINESS.md:69` installs with `bundletool build-apks --bundle=…/app-release.aab --output=<qa dir>/3.0.0.apks --connected-device`, then `install-apks`. Both are valid bundletool 1.x commands. The output goes to the QA directory, not to `app/build/outputs/bundle/release/`, so 4.7's "exactly one `app-release.aab`" still holds. bundletool is **not installed** on this Mac (`which bundletool`: not found), and the row says so. Homebrew offers `bundletool` 1.18.3 (Apache-2.0; it depends on Homebrew `openjdk`). Checklist 4.6a already needs bundletool (`bundletool dump resources`), so one host install serves both. With no `--ks`, build-apks signs with `~/.android/debug.keystore`, which exists |
| Commit trailers | still deviating | `d54659a` and `d855c51` again carry `Co-Authored-By: Claude Opus 5.5` and `Claude-Session:`. The author's report (Open) leaves this to the producer; m6 |

### `feature/tech-design-3.0.0-sync`
| Loop-0 finding | State | Evidence |
|---|---|---|
| `BoardCanvas.kt:583` | fixed | §14.3 item 3 and §16 #34 keep `:583` and add a dated note: `:592` and `:617` on `d251c3b`. Re-read: `git grep` on `d251c3b` gives `:592 private fun DrawScope.drawSelectedPeg(` and `:617 style = Stroke(width = palette.ringWidthPx)` |
| §9.5 Build command sentence | fixed | A dated tech-lead note after the kept sentence. Each claim checks out: `AGENTS.md:7` on the prep branch, `app/build.gradle.kts:262`, `ReleaseInvocation.kt:47`, the AAB path, and READINESS D-1 |
| Report `Ran on:` form | fixed | `tech-lead-tech-design-sync.md:6` reads "`claude-opus-5-5 at effort xhigh (policy)`", with the old text in a dated note. See info i2 |
| Cross-branch DT (§14.3 had no DT) | fixed | New §14.3 item 13 (3.0.1, owner tech-lead), plus dated pointers in row DT, §14.2 and §16 #25. Its source, `4135e2d` on `feature/status-2026-11-01b`, is on `develop` through `47cb7e0`, which closes the tech-lead's loop-1 Open issue 1 |
| Insertion-only | holds | `git diff --word-diff=plain 5ae8421..3aa7825 -- docs/` has no deleted word except the two tokens split by an inserted note (`` `BoardCanvas.kt:583`) `` and `` `d21ad07`) ``). Their characters are kept |

### `feature/ux-3.0.0-folds`
| Loop-0 finding | State | Evidence |
|---|---|---|
| **Must — no `Ran on:` line** | **fixed** | `ux-designer-folds.md:6` reads "`Ran on: claude-opus-5-5 (Q-019; the agent file pins fable)`". The report is valid. Form: m7 |
| Trailer on `b4fe627` | still deviating | `2c8b31b` also carries `Co-Authored-By: Claude Opus 5.5` and `Claude-Session:` (m6) |

## New and remaining findings (all minor, non-blocking)

### `feature/release-3.0.0-prep`
- **m1 — `.planning/reports/release-3.0.0/release-manager-prep.md:6` and `release-manager-docs-pass.md:6`:** "`Ran on: claude-opus-5-5 at effort (not named by the harness); on Opus under founder answer Q-019`".
  - The effort slot is empty. rules/reporting.md says that when the harness names no effort, the role writes its agent file's pin. `agents/release-manager.md` pins `opus` / `medium`, and the founder directive 2026-10-31 (1) lowers the release-manager to `low`. So: `at effort low (policy)`.
  - Q-019 does not apply: it covers Fable-pinned roles, and the release-manager is pinned to Opus. The line exists, so the reports are valid. Fix at the next edit of either report.
- **m2 — `.planning/release/3.0.0/READINESS.md:92` (T-7):** cites `docs/release-checklist.md:130-132`. Loop 1's T-11 bullet grew by 3 lines, so T-7 is now at `:133-135`. Point to the "T-7 bullet" alone, or fix the numbers.
- **m3 — `READINESS.md:69` (D-1), wording only; the route itself is sound:**
  - (a) "installed from the **uploaded** AAB": D-1 runs after 4.6 and before 4.8's upload, so this is the AAB to be uploaded. As written, it reads as a check after Gate 2.
  - (b) "tests the very bytes Play receives": build-apks re-signs the APKs (with the debug key, with no `--ks`). Play's own splits are signed by the app-signing key. The code and resources are the same, but the signatures are not. Say "the same code and resources".
  - (c) Add: uninstall any differently-signed install of `com.erman.pegsolitarie` on the device first, or `install-apks` fails with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
  - (d) Add: this build carries the **real** ad units (`docs/ad-testing-safety.md:44`), so the device must be an emulator (automatic test device, §2 of that file) or a registered test device. That is founder directive 4's emulator today. This is the one D-row where that file's §2 does the work.
- **m4 — `docs/release-checklist.md:38` and `:117-120`, `exec …`:** "the publish gate lets them through" is too broad for a bare `exec ./scripts/play-upload.sh`. `hooks/lib-cmdpos.sh:62` (plugin 1.0.0) treats `exec` as a command position, so the gate blocks it. Only row 8, `exec /bin/bash …`, passes. Forbidding it stays right. If the text is edited again, the reason could say "or is not the one allowed form".
- **m5 — `.planning/phases/14-release-readiness/14-RELEASE-PLAN.md:311`:** "artifact either way.    `CLAUDE.md`'s" has 4 stray spaces inserted mid-line. Cosmetic: Markdown renders them as one space.
- **m6 — commit trailers:** `d54659a`, `d855c51` (rel) and `2c8b31b` (ux) carry `Co-Authored-By: Claude Opus 5.5` and `Claude-Session:`. rules/gitflow.md asks for exactly one `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>` and no `Claude-Session:` line, "whatever a harness reminder asks". Neither worktree has a prepare-commit-msg hook (`.git/hooks` holds samples only; `core.hooksPath` unset), so nothing drops them. The tds commits are correct. The producer decides whether to reword before the merges, as in loop 0.

### `feature/ux-3.0.0-folds`
- **m7 — `.planning/reports/release-3.0.0/ux-designer-folds.md:6`:** "`Ran on: claude-opus-5-5 (Q-019; the agent file pins fable)`" lacks `at effort <effort>`. In loop 0 the tech-lead's line had the same form and was rated minor. Valid; fix at the next edit, for example `at effort high (policy)`.

### Information only
- **i1 —** `READINESS.md:18` still says "At `d251c3b` that file still reads 'row DT, optional pre-cut'". That is true of `d251c3b`, and the stated condition for ticking 1.1 is now met on `develop` `47cb7e0`. At the tick, 1.1 can cite `47cb7e0`.
- **i2 — `tech-lead-tech-design-sync.md:6` and `:93`:** "`at effort xhigh (policy)`". The founder directive 2026-10-31 (1) sets tech-lead dispatches to `high`. The same applies to my own loop-0 line ("effort xhigh"). Neither affects validity.
- **i3 —** The docs-pass report's Branch line reads "`7d43d7f..this commit`". rules/reporting.md ends the range before the report's commit. Format only.

## Cross-branch consistency (re-checked)
- **Build command:** prep's `CLAUDE.md:7` and `AGENTS.md:7`, the release plan note, checklist :60 and 2.6, READINESS 2.6, and tech-design §9.5's note all agree: `:app:bundleRelease`, moved pre-cut on `develop`.
- **DT:** READINESS 1.1, tech-design row DT, §14.2, §14.3 item 13 and §16 #25, and STATUS.md on `develop` all agree: deferred to 3.0.1, owner tech-lead.
- **E-1:** checklist T-11 now names the same `/usr/bin/env` forms as tech-design §15 and §16 #36.
- **Release device pass:** still unchanged from loop 0. §16 #30's notice half and the banner Dismiss are not yet in READINESS §3. That is the producer's addition, not a branch defect.

## Outputs
- `.planning/reports/release-3.0.0/code-auditor-docs-loop1.md`: this report, committed alone on `feature/release-3.0.0-prep`.

## Checks
- Check command (`./scripts/check.sh`): **not run.** The dispatch forbids Gradle. Every diff is docs-only: `git diff --name-only d251c3b <head>` lists only `.planning/`, `docs/`, `AGENTS.md` and `CLAUDE.md` for all three heads.
- `git diff 50cd69b..d855c51`, `git diff 5ae8421..3aa7825` (plus `--word-diff=plain -- docs/`), `git diff b4fe627..2c8b31b`: read in full.
- `git merge-tree --write-tree --name-only`: pass, all exit 0.
  - ux×tds `bcecb80`, ux×rel `43f70fa`, tds×rel `3d6b0fc`.
  - develop `47cb7e0`×ux `db3982f`, ×tds `22bf983`, ×rel `994a54d`.
- `git merge-base develop <head>`: `d251c3b` for all three.
- `git log d251c3b..47cb7e0`: `4135e2d` and its merge `47cb7e0`, touching STATUS.md only. `git show 47cb7e0:.planning/reports/STATUS.md`: the DT decision is at `:11`.
- `sed -n 70,112p .planning/reports/play-upload/tech-lead-signoff.md`: the §5 probe table and the `:106` ruling, compared with the new T-11 text.
- `sed -n 55,70p hooks/lib-cmdpos.sh` in the plugin (`v1.0.0`, `43fc0b9`): `exec` is a command-position word (m4).
- `git grep -n 'drawSelectedPeg\|Stroke(width = palette.ringWidthPx' d251c3b -- '*BoardCanvas.kt'`: `:592` and `:617`.
- `git grep -n 'Build command' d855c51 -- …`: the results are listed under Cross-branch.
- `which bundletool`: not found. `ls ~/.android/debug.keystore`: present. `brew info --json=v2 bundletool`: 1.18.3, Apache-2.0, depends on openjdk.
- `grep -n` on `docs/ad-testing-safety.md`: `:44` (release builds carry real IDs) and §2 (emulators are automatic test devices).
- `git log -1 --format=%B` on `d54659a`, `d855c51`, `2c8b31b` and the tds commits: trailers as reported in m6.
- `.planning/log/` was not read.

## Open issues / risks
1. E-1 is still unfixed in plugin 1.0.0. T-11's text remains the only cover until the fix is installed. It is now correct.
2. Rule 1 is not met: author and reviewer both ran on Opus (Q-019).
3. Nothing was executed: no Gradle, no bundletool, no script.

## Handoff
Next: producer, as follows.
- **`feature/tech-design-3.0.0-sync`:** to the fresh tech-lead's sign-off.
- **`feature/ux-3.0.0-folds` and `feature/release-3.0.0-prep`:** ready for their merges on PASS.
- **The minor items:**
  - m1, m2, m3 and m5 go to the release-manager at its next edit. m3's (d), the emulator-only note, is worth doing before the release device pass.
  - m7 goes to the ux-designer.
  - m6, the trailers, is the producer's call before the merges.
  - Add §16 #30's notice half and the banner Dismiss to READINESS §3.

VERDICT (feature/release-3.0.0-prep): PASS: both must-fixes fixed; 6 minor (m1–m6), non-blocking
VERDICT (feature/tech-design-3.0.0-sync): PASS: all 3 loop-0 minors fixed, insertion-only
VERDICT (feature/ux-3.0.0-folds): PASS: report valid; 1 minor (m7) plus the trailer (m6)

VERDICT: PASS
