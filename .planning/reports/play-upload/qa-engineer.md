# qa-engineer report — play-upload, loop 1 re-read (review mode, no device session)
Date: 2026-10-24
Branch: feature/play-upload (6a0b032..6a0b032; report only, committed by the producer). Reviewed: the fix `git diff 8240177..6a0b032` (`scripts/play-upload.sh`, 131 lines, mode 100755, sha256 `31873d87faf0fd7be213141884ae6239e06a3ada7001431434763bdb2cc20747`), at head `9292193`. Read in full: `android-game-dev.md` (loop 1). My loop-0 report is `qa-engineer-loop0.md`
Skills loaded: ergoz-soft:standards (role qa-engineer). The snapshot is 0.6.2 and the plugin 1.0.0, so I read the plugin copies of studio.md, reporting.md, gitflow.md, skills-policy.md, logging.md, memory.md, puzzle-design.md, play-store.md, kotlin-android.md, compose.md and testing.md in full in this agent's loop-0 pass, plus `.claude/rules/lessons.md`. Also android-testing and android-emulator-skill, loaded because the role requires them. No device was driven
Ran in parallel with: the code-auditor's loop-1 re-read of feature/play-upload
Ran on: claude-opus-5-5 at effort high. **Deviation:** my agent file pins fable at high. Founder answer Q-019 (option 3, re-confirmed 2026-10-24) puts every fable-pinned role on opus until 3.0.0 is published. The author also wrote on opus, so this review is not on a model above the author's
Fan-out: none
Loops: 1
Lessons applied: none. `.claude/rules/lessons.md` lists 0 active lessons, and `.planning/recall/RECALL.md` does not exist

VERDICT: PASS

## Summary
- Row PU's acceptance is still met by the fixed script:
  - `shellcheck` is clean;
  - `bash -n`, `zsh -n` and `dash -n` all pass when the file is fed on stdin;
  - every behaviour the row names is where the source says it is.
- The fix adds four refusals and changes no loop-0 behaviour. I checked each one against fastlane 2.233.0's source. The four: the script must run under bash, the key may not sit inside the checkout, no fastlane config file may sit at the root, and the script stops if `shasum` or `compgen` fails. It now also prints the bundle's sha256 and turns off fastlane's analytics and update check.
- Gate-2 probe list: P1–P6 still hold. I add P0 (not bash), P4a (key inside the checkout), an optional P5a (a config file at the root) and P7, a pre-flight that prints the script's own hash with a dummy key. P7 lets the release-manager compare the hash with checklist 4.6 before the real upload. All the new forms are ones the gate sees. Four refusals cannot be reached by an honest command and are confirmed by reading only.
- **New: the plugin's publish gate does not see a shell named by its path.** In the offline probe, `/bin/bash ./scripts/play-upload.sh` and `/bin/sh ./scripts/play-upload.sh` pass the gate (exit 0), while bare `bash …` and `sh …` are blocked. Under `/bin/bash` or `/bin/sh` (bash 3.2 on macOS) the script runs in full. That is a way around the gate. The defect is in the plugin's `hooks/lib-cmdpos.sh`, not on this branch: row PU makes the hook the only gate. It is high severity for the plugin and is escalated below (E-1).
- T-1 to T-10 still stand. T-1, T-2, T-3, T-6 and T-10 change in wording, and there is one new item, T-11.

## Row PU on the new script, criterion by criterion

| # | Criterion | Met? | Evidence (`6a0b032`) |
|---|---|---|---|
| 1 | Runs `fastlane supply` with `PLAY_SERVICE_ACCOUNT_JSON` | yes | `:69` reads it, and `:120-131` pass it as `--json_key`, a key-file **path** (supply `options.rb:92-104`). |
| 2 | Uploads the signed AAB of `:app:bundleRelease` | yes | `:41` is unchanged. `:107` and `:119` now print the bundle's sha256 before fastlane starts. The script still uploads whatever sits at `:41`, so T-3 stays: the hash is compared **before** the real run. |
| 3 | `internal` track | yes | `:39`, `:122`. |
| 4 | Checks no approval itself | yes | No `VERSION` or `.planning/approvals/` read. The `cd` at `:63` is now preceded by `unset CDPATH` (`:60`), which closes the auditor's F-1. The commands before the `cd` (`:38-60`) neither read nor change the working directory. |
| 5 | A message when the variable or the AAB is missing | yes | `:70-73` and `:94-97` are unchanged, to stderr, exit 78. Loop 1 adds the refusals at `:52-54`, `:85-93`, `:98-103`, `:107` and `:112`, all through `fail` (exit 78, before fastlane). |
| 6 | Echoes no secret | yes | No message interpolates `$key`. The new key-inside-checkout message (`:90-91`) names no path. The new stdout line (`:119`) prints only constants and the bundle's hash. fastlane's summary table still prints the key's path (T-7). |
| 7 | `shellcheck` clean | yes | Re-run: no output, exit 0. |
| 8 | No session runs it before Gate 2, and nothing goes around the hook | the branch, yes | `.planning/approvals/` holds `concept.approved` only, and no `app/build/outputs/bundle/release/` exists here. The developer reports that only throwaway copies of single constructs were run. **But see E-1:** the hook itself can be gone around by a path-named shell. Row PU's "nothing routes around the hook" is therefore a rule of conduct that the hook does not fully enforce. |

The refusals I checked in fastlane's source:
- **The config-file refusal (`:98-103`).** fastlane loads `.env` files only when a `fastlane/` or `.fastlane/` folder exists (`fastlane/lib/fastlane/helper/dotenv_helper.rb:5-22`, `fastlane_core/lib/fastlane_core/fastlane_folder.rb:8-15`), which agrees with the developer's reading. None of the four names is at the root today (`ls -a`).
- **The pinned opt-outs.** `FASTLANE_OPT_OUT_USAGE=1` and `FASTLANE_SKIP_UPDATE_CHECK=1` are scoped to the fastlane process (`:120`).
- **The bash refusal (`:52-54`).** It runs before any bash-only line. `zsh -n` and `dash -n` parse the whole file, so both shells reach the check.
- **One information point, not a defect.** If `dirname` is missing from `PATH`, the substitution at `:63` comes back empty and `cd -- "/.."` moves to `/`. The script then fails closed at the bundle check (`:94`), because there is no `/app/build/...`. `set -e` does not fire on a failed substitution inside an argument, so this is the path it takes.

## Gate-2 probe list, updated (replaces loop 0's P1–P6)

Run these from the root of the `feature/publish-<X.Y.Z>-done` checkout after the approval merge. Invoke the script only as `./scripts/play-upload.sh` (T-11). Run each in the foreground with an explicit `timeout`, ending `; echo "exit=$?"`, and record the stderr line and the exit status in `release-manager-upload.md`.

Each probe either gives no key or gives a readable file that is not a key: `/etc/hosts`, or a file inside the checkout. If fastlane is ever reached with such a file, its `json_key` check rejects it when its configuration is built (`supply/lib/supply/commands_generator.rb:37`, which runs before `Uploader` at `:40`; `options.rb:101-103`; `fastlane_core/helper.rb:423-429` parses the file as JSON). That happens before any Google call. So no probe can send anything.

| Probe | Command | Expected (stderr, exit) | State |
|---|---|---|---|
| P0 *(new)* | `env -u PLAY_SERVICE_ACCOUNT_JSON zsh ./scripts/play-upload.sh` | "run it with bash, as ./scripts/play-upload.sh", 78 | none. Use bare `zsh`, never `/bin/zsh` (E-1). |
| P1 | `env -u PLAY_SERVICE_ACCOUNT_JSON ./scripts/play-upload.sh probe` | "takes no arguments…", 78 | none |
| P2 | `env -u PLAY_SERVICE_ACCOUNT_JSON ./scripts/play-upload.sh` | "PLAY_SERVICE_ACCOUNT_JSON is not set…", 78. This also shows the gate allowing the command. | none |
| P3 | `PLAY_SERVICE_ACCOUNT_JSON=play-key.json ./scripts/play-upload.sh` | "…is not an absolute path…", 78 | none |
| P4 | `PLAY_SERVICE_ACCOUNT_JSON=/nonexistent/play-key.json ./scripts/play-upload.sh` | "…does not name a readable file.", 78 | none |
| P4a *(new)* | `PLAY_SERVICE_ACCOUNT_JSON=<checkout absolute path>/README.md ./scripts/play-upload.sh` | "PLAY_SERVICE_ACCOUNT_JSON names a file inside this checkout. Keep the key outside the repository.", 78 | before the bundle is placed, so P5's check backs this one up |
| P5 | `PLAY_SERVICE_ACCOUNT_JSON=/etc/hosts ./scripts/play-upload.sh` | "no bundle at app/build/outputs/bundle/release/app-release.aab…", 78 | before the bundle is placed. If it is already there, skip P5 and record why; never move the release bundle aside. |
| — | place the bundle, then `shasum -a 256 app/build/outputs/bundle/release/app-release.aab` | equals checklist 4.6, **or stop** (T-3) | — |
| P5a *(new, optional)* | `touch Appfile`, as its own call. Then `PLAY_SERVICE_ACCOUNT_JSON=/etc/hosts ./scripts/play-upload.sh`, then `rm Appfile` and `git status --porcelain` | "Appfile exists at the repository root, and fastlane would read options from it…", 78; then an empty status | bundle in place. If the clean-up is forgotten, the real run refuses, so a lapse fails closed. |
| P6 | `env PATH=/usr/bin:/bin PLAY_SERVICE_ACCOUNT_JSON=/etc/hosts ./scripts/play-upload.sh` | "fastlane is not on PATH…", 78 | bundle in place. fastlane is only at `/opt/homebrew/bin`. `dirname` and `shasum` are in `/usr/bin`, and bash is at `/bin`. |
| P7 *(new pre-flight)* | `PLAY_SERVICE_ACCOUNT_JSON=/etc/hosts ./scripts/play-upload.sh` | stdout "play-upload.sh: uploading … (sha256 <hash>) to the internal track of com.erman.pegsolitarie", then fastlane's own error "'/etc/hosts' doesn't seem to be a JSON file" and fastlane's non-zero exit (not 78) | bundle in place, fastlane on `PATH`. **The printed hash must equal 4.6.** This is the first point where the script's own reading of the bundle can be compared before anything is sent. It runs fastlane, which is authorised once the approval exists, but fastlane cannot authenticate. |
| Real | `PLAY_SERVICE_ACCOUNT_JSON=<absolute key path> ./scripts/play-upload.sh` | the same hash line as P7, then fastlane's success, exit 0 | after P7. Bash `timeout` 600000, foreground. If the timeout cuts it off, the result is **unknown**: check the Console before any re-run. A second run of the same versionCode is refused by Play. |

**Confirmed by reading only, not observable at Gate 2:**
- the `cd` failure (`:63`);
- a key folder that cannot be resolved (`:85-86`). A readable regular file's folder always resolves, since `-f` needs search permission on that folder;
- `shasum` failure (`:107`). Forcing it would mean hiding `/usr/bin`, which also hides `dirname`, so the run would land on the information point above;
- `compgen` failure (`:112`). `compgen` is a bash builtin, and P0 covers the shells that lack it.

I would not force any of these. Each one fails closed.

## E-1 — the publish gate does not see a shell named by its path (plugin, high severity)

I fed eight interpreter forms to `hooks/publish-gate.sh` offline (plugin 1.0.0, sha256 `895a0ef7…`), with `cwd` = this worktree and no approval. Exit 2 means blocked.

| Form | Gate |
|---|---|
| `dash ./scripts/play-upload.sh` | 2 |
| `/bin/dash ./scripts/play-upload.sh` | **0** |
| `/bin/bash ./scripts/play-upload.sh` | **0** |
| `/bin/zsh ./scripts/play-upload.sh` | **0** |
| `sh ./scripts/play-upload.sh` | 2 |
| `/bin/sh ./scripts/play-upload.sh` | **0** |
| `ksh ./scripts/play-upload.sh` | 2 |
| `env -u PLAY_SERVICE_ACCOUNT_JSON dash ./scripts/play-upload.sh` | 2 |

In `hooks/lib-cmdpos.sh:62`, `ERGOZ_CMD_POS` lists `sh|bash|zsh|dash|ksh` with no path prefix in front of them. The path prefix (`ERGOZ_CMD_PATH`) applies only to the publish word. The hook's header (`publish-gate.sh:44-49`) says a wrapper is caught "through an optional path". So the code does not do what its header says.

What it means for this script:
- Under `/bin/zsh` and `/bin/dash`, the new bash refusal stops the run with 78. Loop 1 makes those two forms fail closed.
- Under `/bin/bash`, and `/bin/sh` (which is bash 3.2 on macOS), the script runs in full. With a key and a bundle in place, it would upload without the approval file.

Before Gate 2, two things stand in the way today: no key is provisioned (T-1), and no bundle is in the checkout.

This is not a defect of this branch. Row PU puts the gate in the hook on purpose (plugin rules/stacks.md, contract 9), and the fix belongs on a plugin feature branch. It does break the row's assumption that nothing can go around the hook. ESCALATION: the producer routes it to the plugin's tech-lead. Until the plugin is fixed, the release procedure carries T-11.

## T-1 to T-10: what changes, and the new T-11

- **T-1** (provision the key) — **stands, amended.** The new step must tell the founder to keep the key **outside** the checkout, for example under `~/.config/`. The script now refuses a key whose physical folder lies inside the checkout it runs from (`:85-93`). A key in the main checkout or in another worktree is not refused, so the step should say "outside every repository".
- **T-2** (stale 1.7 and the §4 note) — **stands, amended.** The contract text gains:
  - bash only;
  - the key outside the checkout;
  - a refusal while `fastlane`, `.fastlane`, `Supplyfile` or `Appfile` is at the root;
  - the bundle's sha256 printed before the upload;
  - fastlane's analytics and update check off.

  The syntax check is still `/bin/bash -n < scripts/play-upload.sh`.
- **T-3** (pin the bundle to 4.6) — **stands, amended.** R-1 prints the hash, but only right before fastlane starts, on the real run too. So the comparison must still happen before the real run: either `shasum` after placing the bundle, or P7's printed line. The real run's line then goes into the upload report as the record of what was sent.
- **T-4** (4.3 builds no bundle as written) — unchanged.
- **T-5** (4.7 against the upload path) — unchanged.
- **T-6** (4.8's exact command) — **stands, amended.** The command is `PLAY_SERVICE_ACCOUNT_JSON=<absolute key path> ./scripts/play-upload.sh`, from the `-done` checkout's root, after P0–P7 as listed above, in exactly that form (T-11).
- **T-7** (strip the `json_key` row) — unchanged.
- **T-8** (mapping) — unchanged.
- **T-9** (release plan item 2) — unchanged.
- **T-10** (`docs/store/README.md`) — **stands, amended.** The README says the texts "follow `fastlane supply`'s metadata layout, so the files can be uploaded as they are". The script now refuses to run while a root `fastlane/` folder exists, which is that layout's home. So the README should say that no studio script uploads them, and that copying them into `./fastlane/metadata/android` blocks the upload script.
- **T-11** *(new)* — **the only allowed invocation is `./scripts/play-upload.sh`**, with optional `VAR=value` or `env …` prefixes. Never `/bin/bash …`, `/bin/sh …` or any interpreter named by its path: the gate does not see those (E-1). This goes in checklist 1.7, the §4 note, and the upload report's command line. It is a stopgap until the plugin is fixed.

## Outputs
- `.planning/reports/play-upload/qa-engineer.md` — this report.

## Artifacts (`.planning/qa/play-upload/`, git-ignored, never committed)
- `.planning/qa/play-upload/gate2-forms.jsonl` (1,479 B) and `gate2-forms.out` (1,312 B) — loop 0's eight payloads and verdicts, all exit 2. They still apply: the forms are unchanged.
- `.planning/qa/play-upload/gate2-forms-loop1.jsonl` (990 B) and `gate2-forms-loop1.out` (590 B) — the forms for P0, P4a, P7 and P6, plus `/bin/dash`. The lines that matter:

      row 1 gate=2 ergoz-soft publish gate: publishing 3.0.0 requires founder approval. Missing: /Users/ermanergoz/Desktop/PegSolitarie-pu/.planning/   (P0, bare zsh)
      row 2 gate=0                                                                                                    (/bin/dash: not seen, E-1)
      row 3 gate=2 … (P4a)
      row 4 gate=2 … (P7, with a pipe and PIPESTATUS)
      row 5 gate=2 … (P6)

- `.planning/qa/play-upload/gate2-shells.jsonl` (1,071 B) and `gate2-shells.out` (352 B) — the eight interpreter forms in E-1's table, with the verdicts as tabled there.

All payloads were written with the Write tool and fed to the hook's stdin with `ERGOZ_LOG=0`. No command ran the script.

## Checks
- `shellcheck scripts/play-upload.sh` (0.11.0) — pass: no output, exit 0.
- `/bin/bash -n < scripts/play-upload.sh` (3.2.57) — pass: exit 0.
- `/bin/zsh -n < scripts/play-upload.sh` — pass: exit 0.
- `/bin/dash -n < scripts/play-upload.sh` — pass: exit 0.
- `git show 6a0b032:scripts/play-upload.sh | shasum -a 256` and `shasum -a 256 < scripts/play-upload.sh` — both `31873d87…bdb2cc20747`. `git ls-files -s` gives `100755`.
- `git diff --name-status 8240177..HEAD` — `M scripts/play-upload.sh`, plus the developer's report and my loop-0 rename. The worktree is clean. No Gradle or CI input reads `scripts/` (loop 0's grep). The developer's green Check on `6a0b032` stands: BUILD SUCCESSFUL, 1,576 tests, 0 failed or skipped, fuzz 10,000 / 0. I did not re-run it: the dispatch limits me to static checks.
- Offline gate probes — loop-1 forms: 4 blocked and 1 seen-through. Interpreter forms: 4 blocked and 4 seen-through (E-1).
- fastlane 2.233.0 source, read only:
  - `supply/lib/supply/commands_generator.rb:37-40` builds and checks the configuration before `Uploader`;
  - `fastlane_core/lib/fastlane_core/helper.rb:423-429` (`json_file?` parses the file as JSON);
  - `supply/lib/supply/options.rb:101-103`;
  - `dotenv_helper.rb` and `fastlane_folder.rb` as cited.
- `ls -a` of the root: no `fastlane`, `.fastlane`, `Supplyfile` or `Appfile`. `command -v fastlane` gives `/opt/homebrew/bin/fastlane`. Neither `dirname` nor `shasum` is in `/opt/homebrew/bin`.
- Not run, as dispatched: the script and fastlane, by any route.

## Open issues / risks
1. **E-1 (high, plugin):** a path-named shell goes around the publish gate. The stopgap is T-11. The fix belongs to the plugin's tech-lead.
2. **T-1 and T-3 still block the script route at Gate 2.** Without them, checklist 1.7 can only be met by the Console route.
3. The developer's open issues 1 (the plugin's "first `cd`" wording) and 2 (`scripts/check.sh:38`'s `cd`) are the tech-lead's. I have nothing to add from QA's side.
4. Host: three new small files under `.planning/qa/play-upload/`. Nothing else changed, and no device was used.

## Handoff
Next:
- **tech-lead:** this PASS goes with the code-auditor's loop-1 verdict into the sign-off.
- **producer:** route E-1 to the plugin's tech-lead as a plugin feature branch (`hooks/lib-cmdpos.sh` and its tests). Route T-1 to T-11 to the release-manager. Delete `.planning/qa/play-upload/` after the Done line.
- **release-manager (through the producer):** at Gate 2, run P0–P7 and then the real form, in that order, in the `./scripts/play-upload.sh` form only, and record each result.

VERDICT: PASS
