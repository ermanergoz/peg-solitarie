# release-manager report — release-3.0.0 docs pass (prep owed item 4)
Date: 2026-11-01
Branch: feature/release-3.0.0-prep (7d43d7f..this commit)
Skills loaded: ergoz-soft:standards (role release-manager; game; android-compose-game — the product snapshot `.claude/rules/`: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, ip-policy, testing; lessons.md). gitflow and gsd-complete-milestone not loaded: this pass cuts no branch and completes no milestone.
Ran in parallel with: candy-backdrop performance measurement (host kept quiet: no Gradle run)
Ran on: claude-opus-5-5 at effort (not named by the harness); on Opus under founder answer Q-019
Loops: 0
Lessons applied: none (lessons.md has 0 active lessons)

## Summary
Docs-only pass. It closes T-2..T-7, T-9, T-10, T-11 and TL-1, writes T-1's text, and makes the 2.6 Build command change. It also records the producer's decision: `bugfix/solver-deadline-task` (row DT) is DEFERRED to 3.0.1, owner tech-lead.
No VERSION change, tag, upload, Gradle run or founder question. T-8 (the mapping check in 4.5, recommended) was not in this pass's list and stays owed.

## Outputs
- `docs/release-checklist.md` — 1.7 evidence: merge `1dd1080`, the reviews, `/bin/bash -n <` syntax check, T-11 invocation rule. 4.3 names `./gradlew :app:bundleRelease` (T-4). 4.7: exactly one AAB with 4.6's hash (T-5). 4.8: the exact script-route command (T-6). The stale §4 note "does not exist yet" is replaced by the as-built contract (T-2) and the script-route rules T-11, key ordering/T-1, T-3, T-6, T-7, TL-1.
- `CLAUDE.md` — Build command `./gradlew :app:bundleRelease` (was `:app:assembleRelease`, an APK). This was checklist 2.6. It lands on develop with this branch, earlier than release plan 4a item 5 placed it.
- `.planning/phases/14-release-readiness/14-RELEASE-PLAN.md` — item 2 rewritten to the merged state (T-9); 4b's "exact upload command" points to T-6.
- `.planning/phases/14-release-readiness/14-OPERATOR-CHECKLIST.md` — Step 8 item 1 forks into the Console and script routes. The script route carries T-1: the service account, and the key placed only after the approval file exists, as a regular file outside every repository.
- `docs/store/README.md` — the README now says no studio script uploads the texts and warns that a root `fastlane/` folder blocks the script (T-10). The pending tables keep only `feature/candy-backdrop`, because colour-vision `1bc442f` and audio `6aa7624` have merged. The Accessibility paragraph is updated: TalkBack merged at `d251c3b`, so a claim is now allowed but the texts do not make it yet. The line is offered at Gate 2.
- `.planning/release/3.0.0/READINESS.md` — T-table states updated; row 1.1 records DT deferred to 3.0.1 (owner tech-lead).

## Checks
- `git ls-tree develop scripts/play-upload.sh` / `git log -1 1dd1080` — pass (merge "feature/play-upload" into develop, as cited)
- `grep -n "does not exist" docs/release-checklist.md docs/store/README.md` — pass (no remaining hit about the upload script; the only hit is 3.1's unrelated `build/` note)
- Edits applied by an exact-match script that asserted each old string occurred once — pass
- No Gradle, no Check command, on purpose (host quiet for the performance measurement)

## Open issues / risks
- E-1 (plugin publish gate misses path-named shells) is still open; T-11 and key ordering bind until the plugin fix is installed. While I worked, the gate also blocked a heredoc that only *mentioned* the script name in text. This is a false positive, a lesser form of the same parser weakness. A note for the plugin's tech-lead through the producer.
- The script's contract text in the §4 note was written from the QA/tech-lead reports, not from a fresh read of `scripts/play-upload.sh` at `6a0b032`. The tech-lead's PU close (READINESS §2 item 6) should confirm it.

## Still owed for the release-branch cut
1. `feature/candy-backdrop` (J-4 HOLD) merged or dropped. If it is dropped, delete its store lines (2.4a) and its THIRD_PARTY row (1.4).
2. Docs syncs by other roles, READINESS §2 items 1–5: ux-designer home folds and SN-1; tech-lead's tech-design rows for home, colour-vision, licences and talkback.
3. 1.2 `FUZZ_SEEDS=100000 ./scripts/check.sh --rerun-tasks` on the final develop; 1.3 suppression grep; 1.6 commit count. All three wait for the final tree and a quiet host to be free.
4. At the cut, on `release/3.0.0`: apply `CHANGELOG-draft.md`, including the TalkBack bullet. Tick 2.6, which this pass satisfied in advance.
5. Before Gate 2, not the cut: PU tech-design close (1.7); T-8; the founder's T-1 act on the script route; a TalkBack store line, if the founder wants one.

## Handoff
Next: producer — merge this branch to develop after review (docs only); route the false-positive gate note with E-1 to the plugin tech-lead; schedule items 1–3 above.

## Loop 1 — code-auditor findings (`code-auditor-docs.md`), fixed in `d54659a`
Must-fix:
- T-11 (`docs/release-checklist.md` 1.7 and §4 note): only a bare `env` prefix (the word `env`, not `/usr/bin/env`), as the tech-lead ruled (`play-upload/tech-lead-signoff.md:106`). `/usr/bin/env`, `/usr/bin/nohup`, `/usr/bin/time`, `exec …` and any wrapper or shell named by its path are now named as forbidden, because the gate lets them through (E-1).
- `Ran on:` line added to this report and to `release-manager-prep.md`. The prep report's Branch line now has its range.

Minor:
- `AGENTS.md:7` now reads `./gradlew :app:bundleRelease`.
- Dated 2026-11-01 notes added at `14-RELEASE-PLAN.md` item 5 and checklist §2: the Build-command change lands with this branch, not on the release branch. Tech-design §9.5 is left to the tech-lead.
- READINESS D-1: install from the uploaded AAB with `bundletool build-apks --connected-device` and `install-apks`, after 4.6's hash matches. Chosen because it tests the bytes and splits Play serves; bundletool still has to be installed on this host.
- READINESS fixes: 1.7 (`:25`), 2.6 (`:37`), 4.3, 4.7 and 4.8 (`:47-49`) and the §4 lead line (`:83`) now match the done T-items. T-7 now points to the checklist §4 note (`:130-132`).
- Row DT: READINESS now cites the producer's decision as recorded in `develop`'s STATUS.md (2026-11-01). That line is not on `develop` at `d251c3b` (`:62` still says "optional pre-cut"), so 1.1 waits for the producer's commit.
- `CHANGELOG-draft.md`: the timer bullet now says that TalkBack reads the value in whole minutes, where before it said only "Elapsed time". B-2 never shipped.

Checks: `git diff --stat d54659a^ d54659a` lists 7 files, all docs or reports. No Gradle run (dispatch).
Open: the producer must commit the DT line to STATUS.md. The commit trailers follow the session's attribution reminder, which conflicts with the auditor's note; the producer decides.
