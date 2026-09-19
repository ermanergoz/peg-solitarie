# Founder questions (newest first)

## Q-001 — Confirm Saved Games is enabled and the Play Games configuration is published   (answered)
Date: 2026-09-20    Asked by: producer
Context: QA's two-emulator pass found cloud save doing nothing: signed in and online, leaderboard/achievement
submissions drain but every Saved Games write is refused, so nothing restores (UAT 10-12 blocked, 13 partial).
That is how 13-OPERATOR-CHECKLIST.md Step 0 says an unset toggle presents; only the founder has Play Console access.
Options: 1) Step 0 was not done — enable Saved Games, publish the PGS configuration, answer "done" 2) Step 0 was already done and published — answer "already on"; the evidence then becomes an app defect in PlayGamesCloudSaveStore.save
Recommendation: producer — check the Console first (about a minute); either answer unblocks the next step.
Blocks: phase-13 close-out (13-11 tasks 2-3), phase-13 review and merge
Answer: 2 — already on (2026-09-20). Founder read the Play Console properties: Published; Saved games enabled; Firebase not linked. The toggle is not the cause; the refused save goes to the developer, starting with the tech-lead's CloudSaveOutcome reason code.

## Q-CONCEPT — Adopt the shipped Peg Solitaire concept as Gate 1   (answered)
Date: 2026-09-20    Asked by: producer
Context: This repo predates the studio: v1.0 shipped and v1.1 phases 6-12 were built under gsd alone, so
`.planning/approvals/concept.approved` was never written. /ergoz-soft:build requires it.
Options: 1) Approve the concept as already built (PROJECT.md / REQUIREMENTS.md are the concept of record) 2) Re-run /ergoz-soft:pitch and /ergoz-soft:design first
Recommendation: producer picks 1 — the game is published (versionCode 2) and 7 of 9 v1.1 phases are complete; a re-pitch would produce nothing the roadmap does not already hold.
Blocks: phase-13 close-out via /ergoz-soft:build, phase-14
Answer: 1 — approved as already built (2026-09-20, /ergoz-soft:approve concept 1)
