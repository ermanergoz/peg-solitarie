# qa-engineer report — play-upload (review mode, no device session)
Date: 2026-10-24
Branch: feature/play-upload (9005fe3..9005fe3; report only, committed by the producer). Reviewed: `develop` `f7dd1fe`...`9005fe3`, one work commit adding `scripts/play-upload.sh` (mode 100755, sha256 `35323283…7abeee5071d93c8d0c94b97a061200c177b55b377dcddf9abd`), and the developer's report `e8448ad`
Skills loaded: ergoz-soft:standards (role qa-engineer). The repo snapshot is 0.6.2, older than plugin 1.0.0, so I read the plugin copies of studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md and testing.md in full, plus the repo's `.claude/rules/lessons.md`. Also android-testing and android-emulator-skill, loaded because the role requires them. No device was driven: the deliverable cannot run before Gate 2, and check-gates C7 is the precedent
Ran in parallel with: the code-auditor's review of feature/play-upload
Ran on: claude-opus-5-5 at effort high. **Deviation:** my agent file pins fable at high. Founder answer Q-019 (option 3, re-confirmed 2026-10-24) puts every fable-pinned role on opus until 3.0.0 is published. The author also wrote on opus, so this review is not on a model above the author's (rules/studio.md, Model and effort). Q-019 is what covers that
Fan-out: none
Loops: 0
Lessons applied: none. `.claude/rules/lessons.md` lists 0 active lessons, and `.planning/recall/RECALL.md` does not exist

VERDICT: PASS

## Summary
- Row PU's acceptance is met. `shellcheck scripts/play-upload.sh` (0.11.0) prints nothing and exits 0, and `/bin/bash -n < scripts/play-upload.sh` exits 0. Every behaviour the row names can be checked by reading the code, and I found each one in the source (table below). The row's other acceptance half, the code-auditor's read, is not mine to give.
- The behaviour cannot be tested before Gate 2. Row PU says so on purpose, so it is a design limit and not a gap in the tests. Any harness that runs the script before Gate 2 would go around the hook: a bats run, a copy, a rename, a sourced function or a forged approval. So no test is owed on this branch.
- All six failure paths **can** be observed at Gate 2 without going around the gate. Each probe is a command the hook sees, and I fed all eight forms to the hook offline: each one is judged by the approval (exit 2 today). Each probe also uses either no key or a file that is not a key, so a probe cannot send anything even if the script were wrong. fastlane's own `json_key` check rejects a file that is not JSON before any Google call.
- The release texts are not yet able to run that check. Nothing asks the founder to create the Play service-account key that `PLAY_SERVICE_ACCOUNT_JSON` must name. Nothing pins the bundle that gets uploaded to the hash recorded in item 4.6. Checklist 4.3 and 4.7 do not yet produce or keep that bundle. Four texts go stale when the branch merges. These are the release-manager's (list below). They gate **using** the script at Gate 2, not this merge.

## Row PU, criterion by criterion (`docs/tech-design.md` §14.1 PU)

| # | Criterion | Met? | Evidence (`scripts/play-upload.sh` at `9005fe3`) | How to test |
|---|---|---|---|---|
| 1 | Runs `fastlane supply` against `PLAY_SERVICE_ACCOUNT_JSON` | yes | `:50` reads the variable; `:83-93` passes it as `--json_key`. fastlane 2.233.0's `supply/options.rb:92-104` describes `json_key` as "The path to a file containing service account JSON". So the variable is a **path**, and the script's absolute-path rule (`:55-61`) matches that. | by reading; at Gate 2 by the real run |
| 2 | Uploads the signed AAB of `:app:bundleRelease` | yes | `:37` is AGP's default output path. `app/build.gradle.kts` sets no `archivesName` or output rename (grep: none). `:255-256` make `packageReleaseBundle` depend on `verifyReleaseSigning`, so a bundle at that path was built with the upload key. **But the script uploads whatever file is at that path.** That it is *the release's* bundle depends on the procedure, not the script (T-3 below). | by reading, plus a hash check at Gate 2 (T-3) |
| 3 | To the `internal` track | yes | `:35`, `:85`. supply's default track is production, so this flag is load-bearing. | by reading; Console after the upload |
| 4 | Checks no approval itself | yes | The script reads neither `VERSION` nor `.planning/approvals/`. Its header (`:5-9`) names the hook as the gate. The `cd "$(dirname "$0")/.."` at `:32` is the convention `hooks/publish-gate.sh:58-61` relies on. | by reading |
| 5 | A message when the variable or the AAB is missing | yes | `:51-54` and `:65-68`, both to stderr, prefixed `play-upload.sh:`, exit 78. Four more paths cover an argument, a relative path, an unreadable key and a missing fastlane. | **only at Gate 2** (probes P1–P6 below) |
| 6 | Echoes no secret | yes, for the script | No message interpolates `$key` (`:42-71`), there is no `set -x`, and the one stdout line (`:82`) holds constants. fastlane's summary table prints the key's **path**, not its content (developer open issue 5). The studio log keeps a 1 KB head and tail of command output (rules/logging.md), so that path will also land in the local, git-ignored log. That is information, not a secret. | by reading; at Gate 2, read the upload report |
| 7 | `shellcheck scripts/play-upload.sh` clean | yes | Re-run here: no output, exit 0 (Checks). | static |
| 8 | No session runs it before Gate 2, and nothing goes around the hook | yes, as far as files show | `.planning/approvals/` holds only `concept.approved`. No `app/build/outputs/bundle/release/` exists in this worktree. The developer's report says the script was never run and its gate probe went to the hook's stdin only. I re-ran nothing but static checks and the same kind of offline hook probe (Artifacts). I cannot check what other sessions did: rules/logging.md forbids me the log. | by attestation |

Gitflow requires a QA PASS before the merge, though row PU's owner chain names none. This report is that PASS, in review mode.

## The six failure paths at Gate 2, without going around the gate (developer open issue 2)

**The answer is yes, with conditions.** Before Gate 2, every form of a run is blocked: the developer's probe rows 1–7, and my eight forms below. Once `.planning/approvals/publish-3.0.0.approved` is on `feature/publish-3.0.0-done`, the hook lets those same visible commands through. So each failure path can be observed by the release-manager's upload step, from that checkout, as a literal command the hook judges. Nothing is renamed, sourced, copied or wrapped in a way the hook cannot see. The approval covers these probes: they are the authorised command, stopped before anything is sent.

**The ordering makes every probe unable to upload, even if the script had a defect.** The checks run in order (`:46`, `:50-64`, `:65`, `:69`). Paths 1–4 exit before the bundle or fastlane is looked at. Paths 5 and 6 need a readable key, so the probe hands them `/etc/hosts`, a readable file that is not JSON. If fastlane were reached anyway, its `json_key` check (`supply/options.rb:101-103`: "doesn't seem to be a JSON file") stops it before `Supply::Client` asks Google for a token (`supply/client.rb:48-70`). I read this in the installed 2.233.0 source and did not run it.

Run from the root of the `feature/publish-<X.Y.Z>-done` checkout, each in the foreground with an explicit `timeout`. Record each command's stderr line and exit status in `release-manager-upload.md`:

| Probe | Command (each ends `; echo "exit=$?"`) | Expected | State needed |
|---|---|---|---|
| P1 | `env -u PLAY_SERVICE_ACCOUNT_JSON ./scripts/play-upload.sh probe` | "takes no arguments…", exit 78 | none |
| P2 | `env -u PLAY_SERVICE_ACCOUNT_JSON ./scripts/play-upload.sh` | "PLAY_SERVICE_ACCOUNT_JSON is not set…", 78. This also shows the gate allowing the command. | none |
| P3 | `PLAY_SERVICE_ACCOUNT_JSON=play-key.json ./scripts/play-upload.sh` | "…is not an absolute path…", 78 | none |
| P4 | `PLAY_SERVICE_ACCOUNT_JSON=/nonexistent/play-key.json ./scripts/play-upload.sh` | "…does not name a readable file.", 78 | none |
| P5 | `PLAY_SERVICE_ACCOUNT_JSON=/etc/hosts ./scripts/play-upload.sh` | "no bundle at app/build/outputs/bundle/release/app-release.aab…", 78 | **before** the bundle is placed in this checkout. If it is already there, skip P5 and record why. Do not move the release bundle aside for it. |
| P6 | `env PATH=/usr/bin:/bin PLAY_SERVICE_ACCOUNT_JSON=/etc/hosts ./scripts/play-upload.sh` | "fastlane is not on PATH…", 78 | bundle in place. fastlane is at `/opt/homebrew/bin/fastlane` on this Mac, so `/usr/bin:/bin` hides it, and `/usr/bin/env`, `/bin/bash` and `dirname` stay visible. |
| Real | `PLAY_SERVICE_ACCOUNT_JSON=<absolute key path> ./scripts/play-upload.sh` | fastlane's success, exit 0 | after T-1 to T-3. Bash `timeout` 600000, foreground. If the timeout cuts the call off, the result is **unknown**, not failed: check the Console before any re-run. A second run of the same versionCode fails with "versionCode already used". |

Two conditions:
- **The gate's block is already observed offline**, by the developer's probe and by mine. Do not repeat it live before Gate 2 to "see" the block: if the hook ever failed open, a live attempt would be a run before Gate 2. If the release-manager wants it live anyway, P2's form (no key) is the only acceptable one.
- **An upload that came from a session the hook does not watch** (the founder's own terminal) is not evidence for the script, and the Console route of item 1.7 does not test it at all. If 3.0.0 goes through the Console, the script stays unverified until the first upload that uses it, and the release report should say so.

## Texts that must change, for the release-manager (developer open issues 3–6)

The bundle and the key are the two things the procedure has to name. Item T-1 is new. T-3 is the check that matters most.

- **T-1 — There is no step that provisions the key. Blocks the script route at Gate 2.** `14-OPERATOR-CHECKLIST.md` has no step to create the Play Console service account, grant it release rights on `com.erman.pegsolitarie`, download its JSON key outside the repository, and export `PLAY_SERVICE_ACCOUNT_JSON` as that file's absolute path. `grep -i 'service\|fastlane\|json'` over the file matches only "Play Games Services", at lines 187, 250, 256, 292 and 498. Without such a step, every run ends at P2. Step 8 item 1 ("upload the artifact the studio hands you") also assumes the Console route. If the script route is chosen, Step 8 needs a fork: the studio uploads by script, and the founder starts at item 2. The key is a secret, so the new step should carry Step 7's rule: no value appears in any file or report.
- **T-2 — Item 1.7 and the note under §4 are stale on merge.** 1.7 says "Today it does not exist". The note says "The gated upload script does not exist yet … Until it merges, the only upload route is the founder's own Console upload". Replace them with:
  - the merge commit;
  - the script's contract: no arguments, `PLAY_SERVICE_ACCOUNT_JSON` = the key file's absolute path, uploads `app/build/outputs/bundle/release/app-release.aab` of the checkout it runs from to `internal`, builds nothing, uploads no listing, exit 78 before anything is sent;
  - the route this release uses, recorded by the producer (1.7's "or").

  1.7's evidence should add this report as the QA PASS. It should give the syntax check as `/bin/bash -n < scripts/play-upload.sh`, because `bash -n scripts/play-upload.sh` is blocked by the gate (developer probe row 7). And it should note that shellcheck is now a host tool on this Mac (0.11.0, installed 2026-10-24).
- **T-3 — Pin the uploaded bundle to item 4.6's hash.** The script uploads whatever sits at its path in the `-done` checkout. That checkout is cut from `develop`, not from the tag, and plugin skills/publish step 1 has the release-manager prepare "the release artifact" on `feature/publish-<X.Y.Z>-gate`, also off `develop`. The paragraph under §4 ("The studio stops here") should state three things:
  - the bundle uploaded is the one 4.3–4.6 built and verified on `release/<X.Y.Z>`;
  - how it reaches the `-done` checkout: a branch switch in the same worktree, or a copy;
  - that right before the real run, `shasum -a 256 app/build/outputs/bundle/release/app-release.aab` must equal 4.6's recorded value, and the run stops on any difference. The hash is the only check the procedure has that the script cannot make.
- **T-4 — Item 4.3 builds no bundle as written.** Its evidence is "the Build command in `CLAUDE.md`", and that command is still `./gradlew :app:assembleRelease`, which makes an APK. The release plan 4a item 5 moves `CLAUDE.md` to `:app:bundleRelease` "on the release branch", and 4b step 2 names `bundleRelease`. Run 4.3 literally before that edit and P5's message is the result at upload. 4.3 should name `./gradlew :app:bundleRelease` itself.
- **T-5 — Item 4.7 conflicts with the upload path.** It lists `app/build/outputs/bundle/release/` to find a leftover artifact, and that is the directory the script uploads from. Its pass condition should read: exactly one `app-release.aab` there, and its hash is 4.6's. That ties 4.7 to T-3.
- **T-6 — Item 4.8's "exact upload command".** Name it as `PLAY_SERVICE_ACCOUNT_JSON=<absolute key path> ./scripts/play-upload.sh`, run from the `-done` checkout's root, with P1–P6 before it. Write the key path as that placeholder in the committed summary, for the same reason as developer open issue 5.
- **T-7 — Upload report hygiene (developer open issue 5).** `release-manager-upload.md` quotes fastlane's summary table without its `json_key` row. I agree with the developer's position: `json_key_data` would be worse.
- **T-8 — Mapping (developer open issue 6) can be checked before the upload.** Add to 4.5's R8 evidence: `unzip -l <aab> | grep 'BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map'` prints one line. That is where AGP's bundle carries R8's mapping. I could not confirm it on this host: there is no release bundle. After the upload, the founder can confirm the deobfuscation file for 30000 in the Console as an [F] item. If the bundle lacks it, adding `--mapping` is a change to the script and goes back to the developer through the producer.
- **T-9 — Release plan item 2** (`14-RELEASE-PLAN.md:278-298`): "The repo has **no `scripts/` directory at all**" is already false (`scripts/check.sh`), and "Until it merges…" goes stale on merge. Rewrite them as T-2 does. Item 4b's closing "the exact upload command" should point to T-6.
- **T-10 — `docs/store/README.md:5-8`** says the script "is planned to upload it", meaning the listing. The script passes `--skip_upload_metadata`, `--skip_upload_changelogs`, `--skip_upload_images` and `--skip_upload_screenshots` (`:90-93`), so the founder pastes the listing and the release notes. The README's "can be uploaded as they are" should say that no studio script uploads them.
- For the tech-lead, not the release-manager: `.planning/phases/15-ux-refresh/15-PLAN.md:24` and `:137` say the script resolves the approval file from `VERSION`. It reads no `VERSION` (criterion 4). That file is a phase-15 record, and row PU supersedes it.

## Outputs
- `.planning/reports/play-upload/qa-engineer.md` — this report.

## Artifacts (`.planning/qa/play-upload/`, git-ignored, never committed)
- `.planning/qa/play-upload/gate2-forms.jsonl` (1,479 B) — eight PreToolUse payloads. P1–P6 and the real form above, with `cwd` = this worktree, plus a `cd <worktree> && ./scripts/play-upload.sh` form with `cwd` = the main checkout. Written with the Write tool, so no live command carried the script's name.
- `.planning/qa/play-upload/gate2-forms.out` (1,312 B) — the hook's verdict on each payload. The lines that matter:

      row 1 gate=2 ergoz-soft publish gate: publishing 3.0.0 requires founder approval. Missing: /Users/ermanergoz/Desktop/PegSolitarie-pu/.planning/approvals/publish-3.
      row 2 gate=2 … (the same)
      row 6 gate=2 … (the same: `env PATH=… VAR=… ./scripts/…` is seen through `env`)
      row 7 gate=2 … (the same: the real form with a pipe and `PIPESTATUS`)
      row 8 gate=2 … (the same: `cd` to the worktree first, judged by the worktree's approval)

  All eight forms read exit 2: every probe proposed above is one the hook sees.

## Checks
- `shellcheck --version` — 0.11.0.
- `shellcheck scripts/play-upload.sh` — pass: no output, exit 0.
- `/bin/bash -n < scripts/play-upload.sh` (GNU bash 3.2.57) — pass: exit 0.
- `git show 9005fe3:scripts/play-upload.sh | shasum -a 256` and `shasum -a 256 < scripts/play-upload.sh` — both `35323283ffe33d7afeee5071d93c8d0c94b97a061200c177b55b377dcddf9abd`. `git ls-files -s` gives `100755`. `git diff --stat 9005fe3 HEAD` touches only the developer's report. The worktree is clean.
- `git diff --name-status develop...9005fe3` — `A scripts/play-upload.sh` only. `git grep -n -i scripts` over `*.gradle.kts`, `build-logic/src/main`, `gradle/*.toml` and `gradle.properties` finds one comment (`libs.versions.toml:114`). `scripts/check.sh` and `.github/workflows/ci.yml` do not read the new file. So no Gradle or CI input changed, and the developer's recorded green Check (`./scripts/check.sh --max-workers=4`, BUILD SUCCESSFUL, 1,576 tests, 0 failed or skipped, fuzz 10,000 / 0) stands for this tree. I did not re-run it: the dispatch limits me to static checks.
- `shasum -a 256` of `hooks/publish-gate.sh` gives `895a0ef7…` in the plugin repository and in the 1.0.0 cache. `hooks/lib-cmdpos.sh` gives `8a0a8f37…`. Both match the developer's report.
- `ERGOZ_LOG=0 bash <plugin>/hooks/publish-gate.sh` on each line of `gate2-forms.jsonl` — pass: 8 of 8 blocked, exit 2, each naming this worktree's missing `publish-3.0.0.approved`. Nothing ran the script.
- fastlane 2.233.0 source, read only (`/opt/homebrew/Cellar/fastlane/2.233.0/libexec/gems/fastlane-2.233.0/supply/lib/supply/`):
  - `options.rb` defines each of the ten options the script passes, once each, and `mapping`.
  - The `release_status` default is `COMPLETED`.
  - `json_key`'s verify block rejects a file that is not JSON (`:101-103`).
  - `client.rb:48-70` parses the key before any token request.
- `command -v fastlane` — `/opt/homebrew/bin/fastlane`. Nothing is at `/usr/bin` or `/bin`, which is what P6 relies on.
- `ls .planning/approvals/` — `concept.approved` only. `cat VERSION` — `3.0.0`. `app/build/outputs/bundle/release/` is absent.
- Not run, as dispatched: the script and any fastlane command, by any route.

## Open issues / risks
1. **T-1 blocks the script route for 3.0.0** until the operator checklist gains a service-account step. Without one, 1.7 can only be met by the Console route, and the script reaches Gate 2 unverified. That is the producer's routing call, to the release-manager.
2. **T-3 is the highest-risk gap.** The script cannot tell the release bundle from any other signed bundle built from `develop`. Only the procedure's hash comparison can.
3. **Developer open issue 1 (lint in the Check or CI)** is the tech-lead's. From QA's side, option (c), review plus 1.7's `shellcheck`, is testable today. It is enough while the repository has two scripts.
4. **The reviewer is not on a model above the author** (Ran on). Q-019 covers it, and the producer may want the code-auditor's read, also under Q-019, noted the same way.
5. Host: I created `.planning/qa/play-upload/` (two small files). I changed nothing else on the host and used no device.

## Handoff
Next:
- **tech-lead:** this PASS joins the code-auditor's verdict for the sign-off. Developer open issue 1 (lint) is yours, and so is the `15-PLAN.md:24`/`:137` note.
- **producer:** route T-1 to T-10 to the release-manager. T-1 and T-3 must land before Gate 2 if 3.0.0 is to use the script, and otherwise the producer records the Console route under 1.7. Delete `.planning/qa/play-upload/` after the Done line.
- **release-manager (through the producer):** at Gate 2, run P1–P6 and then the real form from the `-done` checkout, in that order and under those conditions, and record each stderr line and exit status in `release-manager-upload.md`.

VERDICT: PASS
