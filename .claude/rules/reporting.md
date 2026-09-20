# Reporting rules

Every role appends a report before saying "done":
`.planning/reports/<task-slug>/<role>.md`, committed with the work.

    # <role> report — <task>
    Date: YYYY-MM-DD
    Branch: <branch> (<first-commit>..<last-commit>)
    Skills loaded: <comma-separated>
    Ran in parallel with: <task slugs or "none">
    Loops: <N>   (producer reports of build and review steps: developer re-dispatches after the first review round; 0 = first pass)
    Lessons applied: <L-NNNN, L-NNNN> | none

    ## Summary
    ≤ 5 lines.
    ## Outputs
    - <path> — what it is
    ## Checks
    - `<exact command>` — pass|fail (<one-line result>)
    ## Open issues / risks
    ## Handoff
    Next: <role> — <what they need>

A report with no Checks section or no Lessons applied line is invalid.

The producer keeps `.planning/reports/STATUS.md`: one line per finished task
(`date | task | roles | verdict | report paths`), a "Waiting on founder"
section listing open question ids, and an "In flight" section listing
parallel tasks with their worktrees.

Founder questions live in `.planning/questions/FOUNDER.md`, newest first:

    ## Q-<NNN> — <short title>   (open | answered)
    Date: YYYY-MM-DD    Asked by: <role>
    Context: ≤ 3 lines
    Options: 1) … 2) … 3) …
    Recommendation: <role's pick and why>
    Blocks: <task slugs that cannot proceed> | nothing
    Answer: <empty until the founder answers>

Gate questions use ids `Q-CONCEPT` and `Q-PUBLISH-<X.Y.Z>`.

Every founder question text, in FOUNDER.md and in an AskUserQuestion, starts
with its id (Q-CONCEPT, Q-PUBLISH-<X.Y.Z>, Q-<NNN>); log-stats.sh counts
founder asks outside the gates from that prefix.

## Founder-facing updates

Every message the studio prints for the founder in chat — a status screen, a
gate question, a phase summary, the roster, any producer message at all —
opens with a title line `## <role> — <subject>`:

    ## producer — phase-01 done
    ## producer — Q-CONCEPT
    ## producer — staff

The body follows the title line, and two blank lines separate one update from
the next. Together the two rules let the founder see at a glance who is
speaking, what about, and where one update ends and the next begins — in a
scrollback that also carries tool output and agent chatter.

The live feed follows the same rule: `scripts/log-view.sh --follow` prints one
empty line before every entry, whose header line already names from → to, the
step and the kind, and so is that entry's title.

Before every AskUserQuestion to the founder — the two gates, an escalation,
any `Q-<NNN>` — the producer first sends one push notification with the
session's `PushNotification` tool, when that tool is available. The founder
follows sessions from a phone through Claude Code Remote Control: the push
arrives there while Remote Control is connected, and is silently skipped when
the founder is at the terminal. The message is one line, at most 200
characters, no markdown, and starts with the question id, then what is
waiting:

    Q-CONCEPT: pick concept 1 or 2 for Pipe Dreams
    Q-PUBLISH-0.1.0: approve the Play upload

Then the question is asked. Never more than one notification per question, and
never one for routine progress: a push means the studio is waiting for the
founder.
