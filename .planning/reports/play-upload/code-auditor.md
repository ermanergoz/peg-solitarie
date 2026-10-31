# code-auditor report — play-upload (loop 1: re-read of the fix)
Date: 2026-10-24
Branch: feature/play-upload (report only)
Skills loaded: ergoz-soft:standards (role code-auditor; the snapshot is 0.6.2 and the plugin 1.0.0, so the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing and stacks, read in full in this agent's loop-0 pass, and `.claude/rules/lessons.md`), code-reviewer, security-reviewer
Ran in parallel with: other branches' work
Ran on: claude-opus-5-5 at effort xhigh. This deviates from the pin: `agents/code-auditor.md` pins fable, and founder answer Q-019 (option 3, re-confirmed 2026-10-24) puts every fable-pinned role on opus until 3.0.0 is published. So this re-read ran on the developer's own model. Mitigation: each closure below is shown from the committed blob, fastlane 2.233.0's installed sources, or a throwaway probe of the script's own lines (never its fastlane line). None is taken from the developer's report.
Fan-out: none
Loops: 1
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- Re-read the fix `git diff 8240177..6a0b032`: one file, `scripts/play-upload.sh`, now 131 lines, mode 100755, sha256 `31873d87…20747`. The head `9292193` carries the same blob; the commits after the fix add reports only.
- **F-1 to F-4 of `code-auditor-loop0.md` are closed.** Each closure was shown on bash 3.2.57, zsh 5.9 or dash, or from fastlane's source.
- **R-1 and R-2, which the developer ruled in, are sound.** R-2 has one edge, a symlinked key file, noted below and non-blocking.
- **Nothing new opens a route around the gate.** The fix adds refusals and one printed hash. It adds no argument, no invocation path and no other command.
- shellcheck 0.11.0, `/bin/bash -n` and `zsh -n` are all clean. **VERDICT: PASS.**

## Findings

### Loop-0 findings, closed
| Finding | Fix in `6a0b032` | Shown by |
|---|---|---|
| **F-1 MAJOR**: the `cd` followed an exported `CDPATH` | `:60` `unset CDPATH`, then `:63` `cd -- "$(dirname -- "$0")/.."` | **Probe:** the script's own lines 38-63, extracted with `git show` and run from `repoA` as `scripts/headprobe.sh` with `CDPATH=<scratch>/other` exported and `other/scripts` present. It printed `landed=…/repoA`; at loop 0 the same form landed in `other`. macOS `/usr/bin/dirname -- scripts/x.sh` prints `scripts`, so `--` is honoured. The R-2 subshell's `cd` (`:85`) runs after `:60` and takes an absolute folder |
| **F-2 (a)**: the `SUPPLY_ROLLOUT` example | `:109-111`: the example is now `SUPPLY_ROOT_URL` only | `options.rb:261-267` |
| **F-2 (b)**: `ACK_BUNDLE_INSTALLATION_WARNING` | `:126` `--ack_bundle_installation_warning false` on the command line. The header `:29-31` names the OBB pair as not unsettable by name and inert | fastlane's parser takes `--flag false` (`commander_generator.rb:50-70`) and coerces it to `false` (`config_item.rb:290-295`). An explicit `false` is non-nil, so it beats the env value (`configuration.rb:220-222`). OBBs are written only on the APK path (`uploader.rb:391-402`), which `--skip_upload_apk true` skips |
| **F-2 (c)**: Supplyfile, Appfile, dotenv | `:98-103` exits 78 while `fastlane`, `.fastlane`, `Supplyfile` or `Appfile` exists at the root, or is a dangling link (`-e \|\| -L`) | These four cover every Supplyfile and Appfile path (`configuration.rb:196-203`, `appfile_config.rb:19`). Dotenv loading needs a `./fastlane/` or `./.fastlane/` folder (`dotenv_helper.rb:14-22`, `fastlane_folder.rb:9-14`). **Probe** (the script's lines 98-103): a dangling `Supplyfile` link and a `.fastlane` folder were each refused |
| **F-2 (d)**: the header's claim | `:25-33` | The claim is now true. Options the script does not pass reach supply only through `SUPPLY_*` env (unset), `ACK_…` (pinned), config files (refused) or the OBB pair (inert) |
| **F-3**: the unset loop under zsh | `:52-54` checks `BASH_VERSION` before `set` and before every bash-only line. `:112` `exported="$(compgen -e)" \|\| fail …` makes a missing `compgen` fatal | **Probe** (the script's lines 38-63 and 109-117): zsh and dash both exit 78 with "run it with bash". zsh with a forged `BASH_VERSION=5` exported gets past the guard, then exits 78 at "cannot list the environment (compgen)". Under bash, `SUPPLY_ROOT_URL` is cleared. `env IFS=_ /bin/bash` shows bash resets `IFS`, so the unquoted `for name in $exported` splits on whitespace as intended |
| **F-4**: fastlane's analytics and update check | `:120` `FASTLANE_OPT_OUT_USAGE=1 FASTLANE_SKIP_UPDATE_CHECK=1 fastlane supply …`, scoped to the fastlane process; the header `:32-33` says so | `Env.truthy?` treats `1` as true (`env.rb:3-7`). The analytics post stops at `analytics_ingester_client.rb:20` and the update check at `update_checker.rb:14` |

### R-1 and R-2, ruled in by the developer
- **R-1 is sound** (`:107`, `:119`). `shasum -a 256` runs after every precondition and before the unset loop, and a failure exits 78. `${digest%% *}` prints the hash field only; `$AAB` is a constant with no blanks. The hash is not a secret, and the comparison with checklist 4.6 stays the procedure's job.
- **R-2 is sound** (`:84-93`). **Probe** (the script's lines 84-93, keys and a root whose folder name holds glob characters, `re*[po]`):
  - Refused: a key at the root, a key in a subfolder, and a key reached through an outside symlink to a folder inside the root.
  - Accepted: a key outside the root, and a key in a sibling folder whose name shares the root's prefix (`re*[po]2`).
  - The quoted `"$root"` keeps glob characters in the name literal, and the message names no path. The `cd` in its subshell cannot fail in practice: `-f` and `-r` at `:81` already needed search permission on that folder.

### Non-blocking (no change required for this PASS)
- **N-1 — `scripts/play-upload.sh:109`.** "supply reads each option it is not given from its SUPPLY_* variable" is broader than the source: `rollout` and `version_codes_to_retain` have no variable, and `ack_…` reads one without the `SUPPLY_` prefix. The header (`:25-31`) states the exact picture, so this is wording only. A possible rewording: "supply reads an option it is not given from its SUPPLY_* variable, when it has one".
- **N-2 — `:84-93`.** R-2 resolves the key's **folder**, not the key file. **Probe:** an outside symlink `out/linkfile.json` pointing to a key inside the checkout was accepted. If the tech-lead wants that edge closed too, refusing a symlinked key (`[ ! -L "$key" ] || fail …`) is one line. The header's "a key inside it is refused" holds for real paths.

## Routes around the gate
- No new route. The fix adds no argument handling, no second command and no sourced file. It reads no input beyond loop 0's: the environment, the key's folder, the bundle, and four names at the root.
- The script still moves to the repository that holds it before anything that depends on the working directory. The lines above the `cd` — constants, `fail`, the bash test, `set` and `unset CDPATH` — neither read nor change the directory. That keeps the gate's premise (`hooks/lib-cmdpos.sh:116-120`, `hooks/publish-gate.sh:58-61`).
- Loop 0's gate-side note stands, outside this diff: a bare-name match found through `PATH` is judged by the caller's directory.

## Outputs
- `.planning/reports/play-upload/code-auditor.md` — this report. Loop 0's report is kept at `code-auditor-loop0.md`.

## Checks
- `shellcheck scripts/play-upload.sh` (0.11.0) — pass: no output, exit 0.
- `/bin/bash -n < scripts/play-upload.sh` (GNU bash 3.2.57) — pass: exit 0.
- `git show 6a0b032:scripts/play-upload.sh | zsh -n` (zsh 5.9) — pass: exit 0.
- `git show 6a0b032:scripts/play-upload.sh | shasum -a 256` — `31873d87faf0fd7be213141884ae6239e06a3ada7001431434763bdb2cc20747`, equal to the working copy at head `9292193`. `git ls-tree 9292193 scripts/` shows mode 100755. `git diff --stat 8240177..6a0b032` — 1 file, +56/−18.
- The blob through `awk 'length > 100'` and a grep for trailing whitespace or tabs — none, so `.editorconfig` holds.
- The fix commit's message — exactly one `Co-Authored-By` line, `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`, as the last line.
- Probes, all in the session scratchpad, built from `git show 6a0b032:scripts/play-upload.sh | sed -n …` lines, never the fastlane line (`grep -c -i fastlane` on the probe printed 0):
  - The head and the loop under bash with `CDPATH` exported: landed in `repoA`; SUPPLY and ACK entries cleared.
  - Under zsh and under dash: exit 78 at the bash guard.
  - Under zsh with a forged `BASH_VERSION`: exit 78 at the `compgen` check.
  - R-2: five keys and a prefix sibling, with the results above.
  - The config guard: a dangling `Supplyfile` link and a `.fastlane` folder, both refused.
  - `/usr/bin/dirname -- scripts/x.sh` prints `scripts`.
  - `env IFS=_ /bin/bash -c 'printf %q "$IFS"'` prints `$' \t\n'`.
- fastlane 2.233.0 sources read without running fastlane: `commander_generator.rb:36-72`, `config_item.rb:290-295`, plus the loop-0 lines re-cited above.
- `./scripts/check.sh` — not run by me, as at loop 0. The review is static, and no Gradle input, CI step or `check.sh` reads `scripts/` (loop 0's `git grep`). The developer's green Check on `6a0b032` (BUILD SUCCESSFUL, 1,576 tests, 0 failed or skipped; fuzz 10,000 / 0) is the developer's evidence.

## Open issues / risks
1. **This re-read ran on opus, the developer's own model** (Ran on). The fable review rule 1 asks for is owed once fable is available, if the producer and the founder want it before Gate 2.
2. **The developer's open issue 1** asks the plugin text to soften "start with `cd …`" (`lib-cmdpos.sh:118`, `publish-gate.sh:58`) to "move to the repository before anything that depends on the working directory". I concur; it needs no product change, and the producer routes it to the plugin.
3. **Carried from loop 0, unchanged:** `scripts/check.sh:38`'s `CDPATH` idiom, which fails closed (the tech-lead's); the lint-gate question (the tech-lead's); the release-manager's procedure items; the gate's bare-name `PATH` judgement (the plugin's).
4. **N-1 and N-2** are optional. The tech-lead's sign-off may fold them in, or leave them.

## Handoff
Next: tech-lead — sign-off on `feature/play-upload` at `6a0b032` (head `9292193`). Read this report, `code-auditor-loop0.md` and the developer's fix report. N-1 and N-2 are optional. The producer decides whether QA re-reads the fix diff, and routes Open issues 2-3.

VERDICT: PASS
