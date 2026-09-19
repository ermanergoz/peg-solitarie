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
