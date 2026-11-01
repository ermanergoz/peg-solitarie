# release-manager report — release-3.0.0 readiness prep (no cut)
Date: 2026-11-01
Branch: feature/release-3.0.0-prep (d251c3b..7d43d7f; off develop `d251c3b`, one commit shared with the work)
Skills loaded: ergoz-soft:standards (role release-manager; game; android-compose-game — studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, ip-policy, testing from the product snapshot `.claude/rules/`, lessons.md). Not loaded: gitflow and gsd-complete-milestone, because this task cuts no branch and completes no milestone.
Ran in parallel with: candy-backdrop J-4a (other worktree, not touched)
Ran on: claude-opus-5-5 at effort (not named by the harness). Deviation: agents/release-manager.md pins another model; founder answer Q-019 puts Fable-pinned roles on Opus until 3.0.0 is published
Loops: 0
Lessons applied: none (lessons.md has 0 active lessons)

## Summary
I mapped the 3.0.0 readiness without cutting `release/3.0.0`. Every pre-cut blocker row is merged except `feature/candy-backdrop` (HOLD, J-4). The optional `bugfix/solver-deadline-task` still needs a deferral with a named owner.
`VERSION` already reads 3.0.0, and `CHANGELOG.md` already has an unreleased 3.0.0 section. The draft only corrects that section: two stale pending markers, the TalkBack, RM, R1 and home bullets, and a stale "gaps open" bullet.
None of the T-items has been done. The release-checklist 1.7 text still says the upload script does not exist.

## Outputs
- `.planning/release/3.0.0/READINESS.md` — the checklist state item by item, the docs syncs owed, the device-pass list D-1..D-12, and the T-item status
- `.planning/release/3.0.0/CHANGELOG-draft.md` — the edits to apply at the cut
- `.planning/release/3.0.0/Q-PUBLISH-3.0.0-draft.md` — the Gate-2 summary lines, each with its source
- this report

## Checks
- `git log --merges --first-parent develop --grep="Merge branch '<b>'"` for 11 branches — pass (9 merged; candy and solver-deadline-task have none)
- `git diff --name-only d251c3b^1 d251c3b -- app/src/main` — pass (18 files, BoardSpeech.kt among them; 1.1a (2))
- `git ls-tree develop scripts/play-upload.sh` — pass (blob 259c9bb)
- `grep '^## Q-.*(open)' .planning/questions/FOUNDER.md` — only Q-025, which does not block the cut
- `grep -c 'PENDING FOUNDER' THIRD_PARTY.md` — 0
- `git rev-list v1.0..develop --count` — 1260
- `FUZZ_SEEDS=100000 ./scripts/check.sh` — NOT RUN. The tree is not final, and the host must stay quiet for candy's bench visit.

## Open issues / risks
- I did not judge checklist 1.1a, condition (3), against a fresh `git diff --quiet`. I relied on the tech-lead's sign-off; it gets re-run at the cut.
- If candy ships with the +220 ms hitch unfixed, that is a founder Q under J-4c and outside this role.
- E-1 (the plugin publish gate) is still open, so the T-11 and key-ordering stopgaps bind.

## Handoff
Next: producer — owed items in priority order:
1. candy-backdrop: J-4a audit → tech-lead ruling → J-5 (and 15e if the start path changes), or the J-4c founder Q. This is the only cut blocker.
2. Record `bugfix/solver-deadline-task` (row DT) as deferred with a named owner (1.1).
3. Before-cut docs syncs (READINESS §2 items 1–5): the home ux folds and tech-design rows (named by checklist 1.1), and the colour-vision, licences and talkback tech-design rows. Also SN-1 to the ux-designer.
4. Release-manager docs pass on a `feature/*` docs branch: T-2, T-3, T-4 with 2.6's Build command, T-5, T-6, T-7, T-11, TL-1 in the checklist; T-9 and T-10; the store README pending tables and its TalkBack accessibility paragraph.
5. At the cut: 1.2 Check at release settings, 1.3, 1.6 re-count, 2.4/2.4a with the CHANGELOG draft, then §3.
6. Release device pass D-1..D-8 on the final APK; D-9/D-10 only with a phone, otherwise they are Q-PUBLISH lines. D-11 (compose-performance-audit) is part of item 1.
7. Founder F-items (F-1, F-2 24 h lead, F-3, F-5, F-7, F-8, F-13), queued by the producer, non-blocking until Gate 2.
8. PU tech-design close (before 1.7 is ticked).
