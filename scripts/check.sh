#!/usr/bin/env bash
# The Check command (CLAUDE.md, AGENTS.md): `./gradlew check`, run while holding an exclusive
# per-worktree lock (docs/tech-design.md §9.6, F2).
#
# Why: two Gradle builds in one worktree race on the same outputs. A second build's delete of a
# test task's binary result store unlinked the first build's in-progress store and turned
# `:domain:depthSweep` red after 11 minutes (M1, .planning/reports/phase-15/tech-lead-depthsweep.md).
# The same race reaches merged resources, KSP output, lint models and the build cache. Gradle has
# no guard against it, and "one worktree per concurrently working agent" (rules/gitflow.md) failed
# twice as a convention, so the guard is mechanical and lives here.
#
# The lock is an atomic `mkdir` of .gradle/ergoz-build.lock with the holder's PID inside. `flock`
# does not exist on macOS and `lockf`/`shlock` are BSD-only; `mkdir` is atomic on both. A holder
# killed with `kill -9` never runs its trap, so a lock whose PID is not alive is stale and is
# reclaimed.
#
# Usage: ./scripts/check.sh [--wait] [gradle arguments...]
#   --wait  queue behind a running build, retrying every 5 s, instead of failing with exit 75.
#           Consumed here; never forwarded to Gradle.
#   Every other argument goes to `./gradlew check` verbatim, e.g. `./scripts/check.sh --offline`.
# Exit status: Gradle's; 75 (EX_TEMPFAIL) when a live build already holds the lock; 129, 130 or
# 143 when HUP, INT or TERM stopped the run.
#
# Scope: the lock covers the Check command, not every hand-run `./gradlew <task>`. The result-store
# self-heal in build-logic/src/main/kotlin/standards.gradle.kts (F1) is what makes that acceptable:
# a collision there costs one retry, never a wedged store. CI runs one build per runner and calls
# `./gradlew check` directly.
#
# Signals: Gradle runs as a plain child, not through `exec`, so the traps survive it. Ctrl-C
# reaches Gradle and this script together (one process group). A TERM sent to this script alone
# is acted on when Gradle returns, because bash defers a trapped signal while a foreground child
# runs, so the lock is never released while the build it guards is still running. `kill -9`
# skips every trap: the lock stays behind with a dead PID and the next run reclaims it.
#
# Bash 3.2 compatible: macOS ships 3.2, CI is Linux.
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
# Build the worktree the lock guards, whatever directory the caller is in.
cd "$root"
lock="$root/.gradle/ergoz-build.lock"
pid_file="$lock/pid"

readonly POLL_SECONDS=5      # --wait: how often a queued run retries the lock
readonly PID_GRACE_SECONDS=1 # a just-created lock may lack its PID file for this long
readonly EXIT_LOCK_HELD=75   # EX_TEMPFAIL (sysexits.h): try again later

wait_for_lock=0
gradle_args=()
for arg in "$@"; do
  if [ "$arg" = "--wait" ]; then
    wait_for_lock=1
  else
    gradle_args+=("$arg")
  fi
done

mkdir -p "$root/.gradle"

# The PID the holder recorded, or nothing. A holder writes its PID just after its mkdir, so an
# empty read gets one short grace before the lock is judged PID-less.
holder_pid() {
  local pid
  pid="$(cat "$pid_file" 2>/dev/null || true)"
  if [ -z "$pid" ]; then
    sleep "$PID_GRACE_SECONDS"
    pid="$(cat "$pid_file" 2>/dev/null || true)"
  fi
  echo "$pid"
}

is_alive() {
  [ -n "$1" ] && kill -0 "$1" 2>/dev/null
}

# Takes the lock or returns 1. The traps are set the moment the directory is ours, and never
# before: a queued run that is interrupted must not delete the holder's lock. Each signal trap
# exits (128 + the signal's number), and the exit releases the lock through the EXIT trap. A trap
# that only deleted the lock would let the script carry on and start Gradle unlocked if the
# signal landed before Gradle did.
acquire() {
  mkdir "$lock" 2>/dev/null || return 1
  trap 'rm -rf "$lock"' EXIT
  trap 'exit 129' HUP
  trap 'exit 130' INT
  trap 'exit 143' TERM
  echo "$$" >"$pid_file"
}

reclaimed=0
until acquire; do
  holder="$(holder_pid)"
  if ! is_alive "$holder" && { [ "$reclaimed" -eq 0 ] || [ "$wait_for_lock" -eq 1 ]; }; then
    # Stale: its holder died without running its trap. Reclaim and retry, once for a direct
    # run; a queued run re-checks on every poll, so a holder that dies while it waits cannot
    # strand it.
    rm -rf "$lock"
    reclaimed=1
    continue
  fi
  if [ "$wait_for_lock" -eq 1 ]; then
    sleep "$POLL_SECONDS"
    continue
  fi
  echo "check.sh: another build (pid ${holder:-unknown}) holds $lock in $root; wait for it, pass --wait, or if pid ${holder:-unknown} is dead: rm -rf $lock" >&2
  exit "$EXIT_LOCK_HELD"
done

# `${array[@]+...}`: bash 3.2 treats an empty array as unset under `set -u`.
"$root/gradlew" check ${gradle_args[@]+"${gradle_args[@]}"}
