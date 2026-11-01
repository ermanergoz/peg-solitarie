# ux-designer report — release-3.0.0 pre-cut spec folds (home-chip-arrival folds, SN-1)
Date: 2026-11-01
Branch: feature/ux-3.0.0-folds (committed by the producer)
Skills loaded: ergoz-soft:standards (role ux-designer; game; android-compose-game: studio, reporting, gitflow, skills-policy, logging, memory, puzzle-design, play-store, kotlin-android, compose, the plugin's dispatch, and `.claude/rules/lessons.md`), ui-ux-pro-max, android-accessibility, compose-skill
Ran in parallel with: none known
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons; `.planning/recall/RECALL.md` was not found)

## Summary
- The home-chip-arrival spec folds that READINESS §2 item 1 and checklist 1.1 name are made in `docs/ux-spec.md`, as dated notes beside the first wordings. Nothing is deleted. Source: `tech-lead-defect-ruling.md` Open issue 3 and the sign-off's Open issue 1.
- The folds cover D-05f as built (the v35 pair replaces the deleted v27 pair; "below API 35", not "API 24–26") and D-05e's whole-minute fallback, which is live: the 18f sentences, the §4.3.2 note and §8's string set.
- SN-1 is folded in `docs/ux/colour-vision.md`. CV-AC11's "Results with two of three stars" cannot be reached in play, so it is corrected by a dated note, and so is §13.5. Whether such a Results should be reachable is routed to the game-designer.
- No fold is owed for LS-g, L-2, D-TBb, D-TBd or D-TBe (see Open issues). No string, token, colour or decision id is added.

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-uxf/docs/ux-spec.md`:
  - frontmatter `revised:` gains 2026-11-01;
  - a "Pre-release folds — 2026-11-01" entry after the twelfth revision;
  - §3 D-05e: a dated pointer;
  - §4.3.2: a dated note with the fallback's table (floored minutes), the node holding still (B-3's clearing modifier) and the accepted cost;
  - §6.2: a dated mechanism note (v35 pair with `navigationBarColor` = `@android:color/transparent`, v27 pair deleted, below API 35 unchanged);
  - §6.5: a dated note on the start-path note (the v35 pair replaces the v27 pair);
  - §8: a dated note (`duration_under_a_minute` shipped; `duration_seconds` and `duration_minutes_seconds` withdrawn);
  - §13: the `WindowThemeTest` row gains a dated note (below-35 cases, `WindowThemeApi35FilesTest`);
  - §14: dated notes on 18f (the nine sentences), 18g (re-read parts a–c) and 29 (1) (v35 placement, below API 35, the below-35 device read).
- `/Users/ermanergoz/Desktop/PegSolitarie-uxf/docs/ux/colour-vision.md`:
  - frontmatter `revised:` gains 2026-11-01;
  - CV-AC11: a dated SN-1 correction in its cell;
  - §13.5: a dated SN-1 note.
- `/Users/ermanergoz/Desktop/PegSolitarie-uxf/.planning/reports/release-3.0.0/ux-designer-folds.md`: this report.

## Checks
I have no Bash, so this list has no shell command. Every edit is an Edit tool replacement on an exact, unique anchor, and each one returned success.
- Read in full: `PegSolitarie-rel/.planning/release/3.0.0/READINESS.md` (§2 items 1 and 5), `home-chip-arrival/tech-lead-signoff.md`, `home-chip-arrival/tech-lead-defect-ruling.md`, `colour-vision/tech-lead-signoff.md`, `licences-screen/tech-lead-signoff.md`, `licences-screen/tech-lead-merge-cv-addendum.md` — pass.
- Read the spec regions each fold touches before editing: `docs/ux-spec.md` lines 1–200, 390–573, 1440–1481, 3590–3671, 4440–4461, 4680–4691, 4936–5047, 5984–5995, 6636–6661 and 6976–7005; `docs/ux/colour-vision.md` lines 1–30, 595 and 1076–1091 — pass.
- Two read-only helper searches (Explore, haiku). They ran because I have no grep. Their results:
  - `docs/ux-spec.md` has 7,458 lines (before these edits). It has the 8 v27/API 24–26/`windowLightNavigationBar` sites, and every §8 duration row.
  - No `CV-AC11` or `SN-1` hit in `docs/ux-spec.md`. CV-AC11 lives only in `docs/ux/colour-vision.md`.
  - D-TBb, D-TBd and D-TBe are already in `docs/ux-spec.md` on develop: the revision log (:486–553), §3 and §11.1.
  - LS-g is only in `licences-screen/tech-lead-merge-cv-addendum.md`. L-2 (Settings) is only in `licences-screen/code-auditor-merge-cv.md:90`. Neither id is in any spec file.
- `.planning/log/` was not read.

## Open issues / risks
1. **SN-1, routed to the game-designer (non-blocking):** whether a one- or two-star Results should be reachable in play. Today a win (pegs ≤ the board's minimum) is the three-star band, and only a win reaches Results. The spec now records that fact and rules nothing about it.
2. **LS-g and L-2: no fold owed before 3.0.0.** The tech-lead's addendum ((2) item 5, Handoff 4) routes the spec-oracle wording to me *when LS-g lands* in 3.0.1. That wording is `colour-vision.md:250` and `:585`, and `licences-screen.md:558`. Until then those oracles overstate `SettingsScreenTest`, by the tech-lead's ruling. L-2 (the palette row above About is unpinned) is a test item in the same 3.0.1 round. Re-dispatch me with LS-g's merge.
3. **D-TBb, D-TBd, D-TBe: no fold owed.** All three are in `docs/ux-spec.md` on develop, with their "Recorded, not hidden" lines. The Q-PUBLISH-3.0.0 TalkBack lines (D-TBb's floor, D-TBd, D-TBe item 4 if it applies) belong to the producer and release-manager, not the spec.
4. **No fresh checker** has read these folds. They are text-only records of rulings already built and reviewed, and they decide nothing new. A check is the producer's call.
5. **Staffing:** written on Opus (Q-019), effort per the founder's 2026-10-31 directive.

## Handoff
Next: producer —
1. Commit the three files above on `feature/ux-3.0.0-folds`, for example `docs(ux): fold home-chip-arrival and SN-1 before release/3.0.0`. Then merge to develop as a docs-only branch, before the cut.
2. Route SN-1's gameplay question to the game-designer (Open issue 1). It is non-blocking.
3. Keep LS-g's spec-oracle wording (Open issue 2) on the 3.0.1 list, against my role.
4. The tech-lead's tech-design mirror rows (READINESS §2 item 2) are separate, and these notes do not depend on them.
