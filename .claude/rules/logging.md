# Studio log rules

## What is logged
One JSON line per event, written by the plugin hook `hooks/studio-log.sh`:
`session.start`, `session.end`, `founder.prompt`, `dispatch` (description,
prompt, reply), `agent.start`, `agent.stop` (last message, transcript pointer,
tool-count digest), `skill`, `founder.ask`, `message`, `bash` (command, 1 KB of
output head and tail, exit code), `file` (Write/Edit path, size, hash),
`tool.error`, `gate.block` (a command stopped by publish-gate or branch-guard).

## Where
`.planning/log/<YYYY-MM-DD>.jsonl` in the product repo, one file per UTC day.
It is LOCAL: `.planning/log/*` is git-ignored (only its README is tracked), and so
is the retrospective's `.planning/retro/<X.Y.Z>/thread.md` log excerpt; nothing
commits them, and they never leave the founder's machine. `.planning/log/ERRORS`
lists dropped events: health, not content; empty is healthy.

## Who reads it
The log is a record, not an input. The founder reads it live with the absolute command
that `hooks/session-start.sh` prints at session start, in a second terminal:
`bash <plugin root>/scripts/log-view.sh --follow` (numbers: `scripts/log-stats.sh`).
The retrospective (`/ergoz-soft:retro`) reads it for a closed version. Nobody else:
working roles and reviewers never receive a log path and never read `.planning/log/`
for the task under review (rules/studio.md, standing rule 1); the producer reads only the
ERRORS and entry counts (health) and never passes a log path to a role; no role edits it.

## What is never stored
File contents (path, byte count and sha256 only; never-log paths such as keystores,
`.env*` and `*secret*` record the path only), environment variables, transcripts
(pointer only), reasoning. Every string is redacted for key/value secrets, PEM blocks
and known token shapes; prompts and replies are capped at 8 KB (length and hash kept).

## Step attribution and knobs
Every Agent dispatch `description` starts with `[<step>]`: `[new-product]`,
`[pitch]`, `[design]`, `[phase-<NN>]`, `[review-<branch-slug>]`,
`[release-<X.Y.Z>]`, `[publish-<X.Y.Z>]`, `[approve]`, `[status]`,
`[retro-<X.Y.Z>]`, `[retro-pitch]`, `[retro-post-launch-<X.Y.Z>]`; the logger
reads the step from that prefix. Knobs: `ERGOZ_LOG=0` turns the logger off
(tests/run.sh sets it); `ERGOZ_LOG_ALWAYS=1` logs outside a product repo;
`ERGOZ_LOG_CAP=<bytes>` changes the 8 KB cap.
