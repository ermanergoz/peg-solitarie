# code-auditor report — play-upload
Date: 2026-10-24
Branch: feature/play-upload (report only)
Skills loaded: ergoz-soft:standards (role code-auditor; the snapshot is 0.6.2 and the plugin 1.0.0, so the plugin copies of studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, testing and stacks, read in full, and `.claude/rules/lessons.md`), code-reviewer, security-reviewer
Ran in parallel with: the qa-engineer's review-mode pass on feature/play-upload, and other branches' work
Ran on: claude-opus-5-5 at effort xhigh. This deviates from the pin: `agents/code-auditor.md` pins fable, and founder answer Q-019 (option 3, re-confirmed 2026-10-24) puts every fable-pinned role on opus until 3.0.0 is published. So this review ran on the developer's own model, and rule 1's "reviewed on a superior model" does not hold for it. Mitigation: every claim below is re-derived from the script, the gate's source, fastlane 2.233.0's installed sources and throwaway probes of my own. None is taken from the developer's report.
Fan-out: none
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- Reviewed `git diff develop...9005fe3`: one commit (`f7dd1fe..9005fe3`) adding `scripts/play-upload.sh` (93 lines, mode 100755). It meets row PU's letter. shellcheck 0.11.0 and `/bin/bash -n` (3.2.57) are clean.
- **FAIL, on four must-fix items of a few lines each:**
  - F-1, MAJOR, probed: the `cd` the gate relies on follows an exported `CDPATH`.
  - F-2: the header's isolation claim is incomplete. fastlane still reads a Supplyfile, an Appfile, dotenv files and `ACK_BUNDLE_INSTALLATION_WARNING` after the unset loop.
  - F-3, probed: the unset loop does nothing under zsh.
  - F-4: fastlane makes an analytics call and an update check that the script neither explains nor switches off.
- All the open choices are APPROVED: `completed`, the five skip flags, the `SUPPLY_*` unset as a mechanism, the absolute-path rule, exit 78, and the no-argument rule.
- Nothing was executed: not the script, not fastlane, not a gate probe. The live gate blocked one of my read-only commands (Open issues 5).

## Findings

### Must fix (VERDICT FAIL while any is open)

**F-1 — MAJOR — `scripts/play-upload.sh:32`: the `cd` the gate relies on follows an exported `CDPATH`, so the script can upload another tree's bundle under this repository's approval.**
- **What the gate judges.** A script named by its path is judged in `dirname(normpath(join(cwd, path)))`, at that directory's `git rev-parse --show-toplevel` (`hooks/lib-cmdpos.sh:1372-1379`, `hooks/publish-gate.sh:119-129`).
  - `CDPATH` enters the resolver only for a literal `cd` in the command line (`lib-cmdpos.sh:1177`, `:1406`).
  - For that case the gate's own contract is to block: "a relative cd while CDPATH is set" fails closed (`lib-cmdpos.sh:150-151`).
- **What the script does.** `cd "$(dirname "$0")/.."` is a relative cd whenever `$0` is relative and does not start with `./` or `../`. That is true for `scripts/play-upload.sh` and `bash scripts/play-upload.sh`, rows 2 and 3 of the developer's gate probe, and the gate judges both forms by this repository. bash searches `CDPATH` before the current directory for such a name.
- **Shown.** I made a throwaway `repoA/scripts/probe.sh` holding the same `cd` line and ran it on `/bin/bash` 3.2.57 from `repoA`, with `CDPATH=<scratch>/other` exported and `<scratch>/other/scripts` present.
  - `scripts/probe.sh` and `bash scripts/probe.sh` both landed in `other`, and `cd` printed the `CDPATH` hit.
  - `./scripts/probe.sh` and the absolute path both landed in `repoA`.
  - `cd -P` does not help: it also landed in `other`.
- **Effect.** Once the approval exists here, an operator's `scripts/play-upload.sh` passes the gate on this repository's `publish-<VERSION>.approved`. It then uploads `<other>/app/build/outputs/bundle/release/app-release.aab` to `com.erman.pegsolitarie`'s internal track, at status completed. That bundle could be another worktree's, a develop build or a stale one.
  - The header's claim at `:30-31` ("uploads that repository's bundle wherever the caller stands") then does not hold.
  - Likelihood is low: `CDPATH` is not in this session's environment (`printenv CDPATH` prints nothing), and it would have to be exported with an entry that holds a `scripts` directory. Even so, the gate's guarantee should not depend on the operator's shell.
- **Fix.** Either form below was probed on 3.2.57 and lands in `repoA`; both are shellcheck-clean.
  - `unset CDPATH` on its own line before the `cd`.
  - `CDPATH='' cd -- "$(dirname -- "$0")/.."`. The bare `CDPATH= cd` trips SC1007. The `--` also stops a `$0` that begins with `-` from being read as an option.

**F-2 — MINOR — `scripts/play-upload.sh:23-24`, `:73-75`: the isolation the header states is not what fastlane 2.233.0 does, and its one example is wrong.**
The header says "no supply option comes from the caller's shell" and "Only the options below decide what is sent".
- **(a) The example.** `:74-75` says "a SUPPLY_ROLLOUT would stage it". supply's `rollout` has no `env_name` (`supply/lib/supply/options.rb:52-60`), and fastlane derives none: `env_names = [env_name].compact + (env_names || [])` (`fastlane_core/lib/fastlane_core/configuration/config_item.rb:152`). No `SUPPLY_ROLLOUT` is ever read.
- **(b) Env the loop leaves in place.**
  - `ack_bundle_installation_warning` reads `ACK_BUNDLE_INSTALLATION_WARNING` (`options.rb:338-343`), which has no `SUPPLY_` prefix.
  - The two OBB sizes read `SUPPLY_OBB_MAIN_FILE SIZE` and `SUPPLY_OBB_PATCH_FILE SIZE` (`:324`, `:334`). These names contain a space, so `compgen -e` cannot list them.
  - Probed on bash 3.2.57 with the loop copied verbatim: `SUPPLY_ROOT_URL` and `SUPPLY_TRACK` were removed, but `ACK_BUNDLE_INSTALLATION_WARNING=true` and `SUPPLY_OBB_MAIN_FILE SIZE=9` were still in the child's environment.
  - The OBB pair is inert here: OBBs are written only on the APK path (`supply/lib/supply/uploader.rb:391-402`), which `--skip_upload_apk true` skips.
- **(c) Files fastlane reads from the root the script moves to.**
  - A `Supplyfile` from `./fastlane/`, `./.fastlane/` or `./` (`supply/lib/supply/commands_generator.rb:65`, `fastlane_core/.../configuration.rb:196-203`).
  - An `Appfile` from the same three places (`credentials_manager/.../appfile_config.rb:19`).
  - Whenever `./fastlane/` or `./.fastlane/` exists, `.env` and `.env.default` from that folder or from the root (`fastlane/lib/fastlane/helper/dotenv_helper.rb:14-32`, `fastlane_core/.../fastlane_folder.rb:9-10`). fastlane loads these when it starts (`cli_tools_distributor.rb:51`), after the script's loop has run, so they put back any `SUPPLY_*` value the loop removed.
- **Why it matters.** Precedence is CLI, then env, then config file, then default (`configuration.rb:220-228`). Every option the script does not pass on the command line stays open to those files.
  - A Supplyfile `rollout(0.1)` turns the pinned `completed` into a staged `inProgress` release (`uploader.rb:433-438`).
  - A Supplyfile `root_url`, or a dotenv `SUPPLY_ROOT_URL`, sends the edit and the service account's bearer token to another host. That is the redirect `:73-74` names as the threat.
  - None of these files exists today: the worktree root has no `fastlane`, `.fastlane`, `Supplyfile`, `Appfile` or `metadata`. The hole is latent.
- **Fix: make the guarantee true.**
  - Before the `fastlane` call, stop with exit 78 if any of `fastlane`, `.fastlane`, `Supplyfile` or `Appfile` exists at the root: one `[ ! -e "$f" ] || fail …` over the four names.
  - Unset `ACK_BUNDLE_INSTALLATION_WARNING` beside the loop, or pass `--ack_bundle_installation_warning false`.
  - Correct `:23-24` and `:73-75`: drop `SUPPLY_ROLLOUT`, name the four files and the `ACK_` variable, and say that the OBB pair cannot be unset by name and is inert for an AAB upload.

**F-3 — MINOR — `scripts/play-upload.sh:76-80`: under zsh the unset loop does nothing, and the script carries on.**
- The header promises bash 3.2 (`:27`), but nothing refuses `zsh scripts/play-upload.sh`. The gate matches that form and judges it like the others, because `zsh` is a wrapper in `ERGOZ_CMD_POS` (`lib-cmdpos.sh:62`). zsh is also the Claude Code Bash tool's shell on this Mac.
- **Shown.** I ran the loop, in a throwaway file, under `zsh` 5.9 with `SUPPLY_ROOT_URL` exported.
  - zsh printed `command not found: compgen`, and `SUPPLY_ROOT_URL=https://example.invalid/` was still in the environment.
  - The run exited 0: `set -e` does not fire on a failed substitution in a `for` word list.
  - `/bin/sh` here is bash (`/private/var/select/sh -> /bin/bash`), and under it the loop cleared the variable.
- **Fix.** Refuse any shell but bash before the first bash-only line, for example `[ -n "${BASH_VERSION:-}" ] || { echo "play-upload.sh: run it with bash, as ./scripts/play-upload.sh" >&2; exit 78; }`. zsh parses that line.

**F-4 — MINOR — `scripts/play-upload.sh:83`: the fastlane run makes two network calls that the script neither explains nor switches off.**
- **Analytics.** fastlane 2.233.0 posts a launch event to `https://www.google-analytics.com/collect` for every tool run, unless `FASTLANE_OPT_OUT_USAGE` is truthy.
  - Code: `fastlane_core/.../ui/fastlane_runner.rb:115`, `analytics/analytics_session.rb:21-43`, `analytics/analytics_ingester_client.rb:9` and `:17-24`.
  - The event carries a salted SHA-256 of `android_project_com.erman.pegsolitarie`, which fastlane guesses from `--package_name` (`analytics/app_identifier_guesser.rb:26-59`).
- **Update check.** fastlane asks for its latest version (`cli_tools_distributor.rb:78`, `update_checker/update_checker.rb:14`) unless `FASTLANE_SKIP_UPDATE_CHECK` is truthy.
- The script exists to call the Play API. These two calls are the "unexplained network" the auditor's security check names, on the one command that holds the Play credential.
- **Fix.** Run `FASTLANE_OPT_OUT_USAGE=1 FASTLANE_SKIP_UPDATE_CHECK=1 fastlane supply …` (`Env.truthy?` accepts `1`, `fastlane_core/.../env.rb:3-7`), and add one header line that says so.

### Non-blocking recommendations
- **R-1 — `scripts/play-upload.sh:82`.** Print `shasum -a 256 "$AAB"` with the upload line, so the upload report can be compared with checklist 4.6's recorded artifact identity. The script uploads whatever bundle sits in the checkout (the developer's open issue 3).
- **R-2 — `:62-64`.** The header says the key is "kept outside the repository", but nothing checks it. An optional `case "$key" in "$(pwd -P)"/*) fail …` would refuse a key that sits inside the checkout, where a careless `git add -A` could commit it.

## Rulings on the choices the row leaves open
1. **`--release_status completed` (`:36`, `:86`): APPROVED.**
   - It is supply's default (`options.rb:38`), pinned so that a later change to the default cannot move it.
   - The listing is already published (tech design §9.5: versionCode 30000 is "above the published 2"), so a completed release on the internal track is accepted.
   - Internal testers can install it at once, which checklist §5's track download needs.
   - Only a Supplyfile `rollout` can override it (F-2).
2. **The five `--skip_upload_*` flags (`:89-93`): APPROVED.**
   - `apk`: the `apk` option defaults to `Dir["*.apk"].last` (`options.rb:127`). With `aab` set, `verify_config!` then stops the upload with "Cannot provide both apk(s) and aab" (`uploader.rb:167-172`). So a stray APK would fail the upload, not "join" it as the developer's report says (its line 134). The flag is right either way.
   - `metadata`, `changelogs`, `images` and `screenshots`: `metadata_path` defaults to `./fastlane/metadata/android` or `./metadata` (`options.rb:66`). Row PU uploads the bundle only, and the founder enters the listing and the release notes.
   - `skip_upload_aab` rightly stays at its default, false.
3. **Unsetting every `SUPPLY_*` (`:76-80`): APPROVED as the mechanism.**
   - Probed on bash 3.2.57: it removes `SUPPLY_ROOT_URL` and `SUPPLY_TRACK`.
   - The threat it names is real: `SUPPLY_ROOT_URL` does replace the API host (`options.rb:261-267`).
   - F-2 and F-3 close what it misses.
4. **An absolute key path (`:55-61`): APPROVED.** The script moves to the root before fastlane resolves the path, so a relative path would resolve against the wrong directory. Refusing it fails closed, with a message. A quoted `~/…` fails the same check instead of being misread.
5. **Exit 78 (`:38`): APPROVED.** Strictly, sysexits gives a bad argument EX_USAGE (64). But one documented code meaning "stopped before anything was sent" is the simpler contract for the operator and for the upload report.
6. **No arguments (`:46-48`): APPROVED.** This choice is not in the dispatch's list. Nothing a caller types can change the track, the package or the bundle, and the publish procedure passes no arguments (plugin `skills/publish` step 3; `agents/release-manager.md` step 8).

## Row PU, requirement by requirement
| Requirement | Verdict | Evidence |
|---|---|---|
| `fastlane supply` against `PLAY_SERVICE_ACCOUNT_JSON` | met | `:50`, `:83-88`. The variable is read as a key-file path, as the plugin's original template read it: `json_key_file(ENV["PLAY_SERVICE_ACCOUNT_JSON"])`, plugin `docs/superpowers/plans/2026-09-16-02-template-android-compose-game.md:2126` |
| The signed AAB of `:app:bundleRelease` | met | `:37`, `:87`: AGP's default path. The app has no flavours, no `archivesName` and no `applicationIdSuffix`, and release plan 4b uses the same path. It is signed because `verifyReleaseSigning` is a `dependsOn` of `packageReleaseBundle` (`app/build.gradle.kts:226-242`, `:255-257`) and a release invocation throws without the four variables (`:172-187`). `PACKAGE_NAME` equals `applicationId` (`:290`) |
| The `internal` track | met | `:35`, `:85`. The flag is load-bearing: supply's default track is production (`supply/lib/supply.rb:37`) |
| Checks no approval itself | met | It reads neither `VERSION` nor `.planning/approvals/`, and the header names the hook as the gate (`:5-9`). This matches plugin `rules/stacks.md` item 9 |
| A message when the variable or the AAB is missing | met | `:50-54`, `:65-68`, and four more checks (the failure paths, below) |
| Echoes no secret | met | Secret handling, below |
| `shellcheck` clean | met | Checks |
| Nothing routes around the hook | **not met while F-1 is open** | The gate's judgement and the upload's target can diverge (F-1). F-3 is a hole in the script's own defence, under a form the gate passes |

## The six failure paths, read from the source
- The developer's table matches the source.
- All six checks (`:46-71`) run before the unset loop (`:76`) and before fastlane (`:83`).
- Each `fail` prints `play-upload.sh: <message>` to stderr and exits 78. `$*` joins the two message arguments with one space.
- No message interpolates `$key`. Path 5 interpolates only the constant `$AAB`.
- `${…:-}` at `:50` keeps `set -u` quiet.
- `-f`, `-r` and `-s` are stat checks, so the script never opens the key file. A directory, a FIFO or a dangling link fails `-f` and takes path 4.
- A failing `cd` at `:32` exits 1 under `set -e`, before any check.
- I executed none of the paths either: the gate blocks every run form, and the dispatch's hard rule forbids any other route.

## Secret handling
- No line prints `$key`, and there is no `set -x`. The one stdout line (`:82`) names the bundle, the track and the package.
- The script only stat-tests the key file. fastlane is what opens it (`supply/lib/supply/client.rb:37-38`).
- fastlane's summary table prints the `json_key` path. It masks only options marked `sensitive` plus its `mask_keys` (`fastlane_core/lib/fastlane_core/print_table.rb:31-36`; `uploader.rb:9` passes `[:json_key_data]`), and `json_key` is not marked. That is the path, not the key. I concur with the developer's open issue 5: the committed upload report should quote the table without that row.
- supply's error messages name the path, never the content (`options.rb:102-103`; `client.rb:30`, `:55`, `:65`).
- If the operator's shell exports the `PLAY_UPLOAD_*` signing secrets, fastlane's process inherits them, but supply reads none of them. Not a finding.

## Bash 3.2
- `/bin/bash -n` (3.2.57) and shellcheck are both clean.
- Every construct is available in bash 3.2: `set -o pipefail` (3.0 and later), `compgen -e` (probed non-interactively on 3.2.57), `unset -v`, `readonly`, `${v:-}`, `case`, `[ ]` and `command -v`. There are no arrays, `[[ ]]`, `mapfile`, `${v,,}` or `local -n`.
- `.editorconfig` holds: 4-space indent, LF line endings, a final newline, no trailing whitespace and no tabs. No line exceeds 100 columns.
- "Bash 3.2 compatible" does not cover a caller who names another shell (F-3).

## The `cd` the gate relies on (`:32`)
- **Where it agrees with the gate.** For every form that names the script by a path starting with `/`, `./` or `../`, from any directory: the gate takes the git toplevel of the script's directory, the script takes the parent of `scripts/`, and the two are the same directory while the script sits at `<root>/scripts/`.
- **If the script moves deeper,** its `..` misses the root, it finds no bundle, and it stops at path 5. That fails closed.
- **Where it disagrees with the gate:** under an exported `CDPATH`, for `scripts/…` and `bash scripts/…` (F-1).
- **A bare `play-upload.sh` found through `PATH`:** the resolver judges the caller's directory, because it needs a `/` in the word (`lib-cmdpos.sh:1372`). bash, however, hands the script an absolute `$0`, so the script moves to its own repository. This is gate-side and cannot be fixed in this script (Open issues 2).

## Routes around the gate
- The diff adds one file and wires it nowhere. No Gradle file, CI step or script names it: `.github/workflows/ci.yml` runs `./gradlew …` only, and `scripts/check.sh` runs `./gradlew check`.
- The repository root has no fastlane folder, Fastfile or Gemfile, so no lane exists that could run supply.
- The script takes no arguments, so no flag can redirect it.
- A renamed copy is invisible to the gate, as is any other script that calls fastlane. This is the gate's documented known limit (`publish-gate.sh:9-13`). A copy placed outside the repository moves to its own `..`, finds no bundle and stops at path 5.
- The leftover state the script means to neutralise gets past it as F-2 and F-3 describe.

## Outputs
- `.planning/reports/play-upload/code-auditor.md` — this report.

## Checks
- `shellcheck --version` — 0.11.0.
- `shellcheck scripts/play-upload.sh` — pass (no output, exit 0).
- `shellcheck --shell=bash --severity=style scripts/play-upload.sh` — pass (exit 0).
- `/bin/bash -n < scripts/play-upload.sh` (GNU bash 3.2.57) — pass (exit 0).
- `git show 9005fe3:scripts/play-upload.sh | shasum -a 256` — pass: `35323283ffe33d7afeee5071d93c8d0c94b97a061200c177b55b377dcddf9abd`, the same as the working copy. `git ls-tree 9005fe3 scripts/` shows mode 100755.
- `git diff --stat develop...9005fe3` — 1 file, 93 insertions.
- `shasum -a 256` of the two hooks — the plugin repository (`43fc0b9`, 1.0.0) and the 1.0.0 cache are identical: `publish-gate.sh` `895a0ef7c76335b1e2902e18e1bfe737323e344e41e6d6601eaf3fc1677ef5c7`, `lib-cmdpos.sh` `8a0a8f3702013966bdcdc46a796da56883737362f256247c51e5a776bc02821c`.
- CDPATH probe (a throwaway `probe.sh` carrying the script's `cd` line, `/bin/bash` 3.2.57, in the session scratchpad) — fail on the script's form:
  - `scripts/probe.sh` and `bash scripts/probe.sh` landed in `other`.
  - `./scripts/probe.sh` and the absolute path landed in `repoA`.
  - `cd -P` landed in `other`.
  - `unset CDPATH` and `CDPATH='' cd --` landed in `repoA`. The quoted form is shellcheck-clean; `CDPATH= cd` gives SC1007.
- Unset-loop probe (a throwaway file with the script's loop verbatim):
  - bash 3.2.57: `SUPPLY_ROOT_URL` and `SUPPLY_TRACK` removed; `ACK_BUNDLE_INSTALLATION_WARNING` and `SUPPLY_OBB_MAIN_FILE SIZE` left in place.
  - zsh 5.9: `command not found: compgen`, `SUPPLY_ROOT_URL` left in place, exit 0.
  - `/bin/sh` (bash): the variable was removed.
- `printenv CDPATH` — not set in this session.
- `ls -la` of the worktree root, plus `git ls-files` — no `fastlane`, `.fastlane`, `Supplyfile`, `Appfile`, `Gemfile` or `metadata`.
- `git grep -n -i scripts -- '*.gradle.kts' build-logic/src/main 'gradle/*.toml' gradle.properties '.github/workflows/*.yml'` — only comments: `ci.yml:5` and `libs.versions.toml:114`. `scripts/check.sh` runs `./gradlew check` and reads no other script.
- Gate source read: `hooks/publish-gate.sh` in full. `hooks/lib-cmdpos.sh` lines 1-230 (the documented contract and the patterns) and 1330-1479 (`judge`, `targets`, `main`); the rest was searched for `self` and `CDPATH`.
- fastlane 2.233.0 sources read under `/opt/homebrew/Cellar/fastlane/2.233.0/libexec/gems/fastlane-2.233.0`; no fastlane command was run.
  - supply: `options.rb` in full, plus `uploader.rb`, `client.rb`, `commands_generator.rb` and `supply.rb`.
  - fastlane_core: `configuration.rb`, `config_item.rb`, `print_table.rb`, `env.rb`, `fastlane_folder.rb`, `update_checker.rb`, `ui/fastlane_runner.rb` and `analytics/*`.
  - fastlane: `helper/dotenv_helper.rb` and `cli_tools_distributor.rb`.
- `./scripts/check.sh` — not run. The dispatch scoped this review to static checks. The diff adds a file that no Gradle input, CI step or `check.sh` reads (the grep above), so it cannot change the Check's result. The developer's green Check on `9005fe3` (1,576 tests, 0 failed; fuzz 10,000 / 0) is the developer's evidence, not mine.

## Open issues / risks
1. **This review ran on the developer's own model** (Ran on). The founder's Q-019 answer covers that. The fable review that rule 1 asks for is still owed for this script once fable is available, if the producer and the founder want it before Gate 2.
2. **Gate-side, for the plugin (producer to route).** The resolver judges a bare-name `self` match (`play-upload.sh`, no slash, found through `PATH`) by the caller's directory (`lib-cmdpos.sh:1372`), while the script moves to its own repository. Blocking such a match as `U`, or resolving it through `command -v`, would close it. Nothing in this product puts `scripts/` on `PATH`.
3. **`scripts/check.sh:38`** has the same idiom, `root="$(cd "$(dirname "$0")/.." && pwd)"`. Under an exported `CDPATH`, invoked as `scripts/check.sh`, the `cd` prints the directory, `root` holds two lines, and `cd "$root"` (`:40`) fails. Probed: exit 1. It fails closed, no gate depends on it, and it is outside this diff. The tech-lead decides whether to fold in the same `unset CDPATH`.
4. **The developer's open issues.**
   - 1 (a shellcheck gate): the tech-lead's call. shellcheck is a GPL-3.0 host tool, neither linked nor shipped, so no copyleft escalation arises either way.
   - 2 (unexecuted failure paths), 3 (bundle provenance; R-1 makes the comparison cheap), 5 (the key path in fastlane's table) and 6 (the R8 mapping in Console): I concur, and they are the release-manager's.
   - 4 (stale texts): the release-manager's and the tech-lead's.
   - 7 (host changes): the producer's record.
5. **One `gate.block` entry in today's log is mine, from a read-only command.** It was an `awk 'length > 100 {print …}' scripts/play-upload.sh`, whose `}` before the script's path the gate reads as a group that runs it: the documented over-block of quoted text, the same one the developer hit. Nothing ran. I re-ran the check through `git show 9005fe3:scripts/…`. Recorded so that the retro does not read it as an upload attempt.

## Handoff
Next: android-game-dev (fix mode, through the producer). Fix F-1 to F-4 on `feature/play-upload`, keep `shellcheck scripts/play-upload.sh` and `/bin/bash -n < scripts/play-upload.sh` clean, and rule R-1 and R-2 in or out in the fix report. Then the code-auditor re-reads the fix diff. After a PASS: the tech-lead's sign-off. The producer routes Open issues 1-4.

VERDICT: FAIL
