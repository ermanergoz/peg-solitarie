# qa-engineer report — candy-backdrop: BD-23 (d) desk check under B-11d (item (4) from T, the battery-saver count)
Date: 2026-11-01
Branch: feature/candy-backdrop (report only; reviewed at a155ca5)
Skills loaded: ergoz-soft:standards (role qa-engineer; the rules CLAUDE.md imports from `.claude/rules/`, snapshot 0.6.2, plus the plugin 1.0.0 copies of reporting.md for this report's format)
Ran in parallel with: none
Ran on: claude-opus-5-5 at effort low (the harness's setting for this dispatch; the agent file pins fable, and Q-019 puts Fable-pinned roles on Opus)
Loops: 0
Lessons applied: none (the snapshot holds 0 active lessons)

## Summary
- I re-derived all 14 values of "at 1 after T" from the absolute presentation times in my read files, as first-at-1 minus T. This avoids subtracting two already-rounded differences.
- **Item (4) PASS, 14/14.** The largest is `bs-dark-2` at **1.288 s**, against ≤ 1.56 s. `bs-light-2` is at 1.075 s.
- The ux-designer's table is confirmed. Two rows differ by 1 ms from rounding (`light-2` 0.849, `darkL-3` 1.261), which does not change the verdict.
- **T is the script's registered T**, not a new reading. **The battery-saver count as restated is met.**
- **BD-23 (d) overall: PASS.**

## Outputs
- `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/reports/candy-backdrop/qa-engineer-b11d-desk.md` — this report.

The inputs below are under `/Users/ermanergoz/Desktop/PegSolitarie-candy/.planning/qa/candy-backdrop/`. They are read-only here, and nothing new was written.

| Path | Size | What it shows |
|---|---|---|
| `j5/bd23d/read-light.txt` | 3,123 | J-5's light-1…5 reads: H, T, first-at-1 (absolute times) |
| `j5/bd23d/read-dark.txt` | 2,978 | J-5's darkL-1…5 reads |
| `bd23d-close/read-bs.txt` | 4,244 | The battery-saver reads: bs-light-1/2 and bs-dark-1/2, including D and the rest pose |
| `j5/bd23d/bd23d.py`, `bd23d-close/bd23d.py` | 4,245 each | The read script. The two copies are byte-identical (`diff` is empty) |

## T: the one the script defined
- The script's header (line 4) defines T before any value is read: "Tile settle T: first frame from which 4 interior Challenge-tile probes (top and bottom rows) equal their last-frame values (+-3) to the end."
- The code computes it once (`T = next(i for i in range(H, n) if all(...close(fr[j][1][k], last[1][k])...))`). Every `read-*.txt` line `T(tile settled)=f..@..s` is that value.
- J-5's report (`qa-engineer-j5.md`, line 163) records the same definition. B-11d's (4) names the same moment: "the first recorded frame from which the Challenge tile holds its final alpha and position (item (1)'s moment)".
- **Registration:**
  - For the battery-saver runs, the script copy (12:16) predates all four recordings (12:17–12:19).
  - For J-5, the script was fixed when the reads were taken. T was then item (1)'s anchor, and (4) was counted from H, so T was not chosen with (4) in view.
  - Using T as (4)'s anchor was chosen after the data (the ux-designer's Open issue 1). That choice belongs to the spec owner and the code-auditor's weighing, not to this desk check.

## Item (4), "at 1 after T" = t(first-at-1) − t(T), absolute times from the reads

| Run | T (s) | first-at-1 (s) | **at 1 after T** | ux-designer | ≤ 1.56 s |
|---|---|---|---|---|---|
| light-1 | 3.246 | 3.959 | 0.713 | 0.713 | ok |
| light-2 | 2.598 | 3.447 | 0.849 | 0.850 | ok |
| light-3 | 2.605 | 3.323 | 0.718 | 0.718 | ok |
| light-4 | 3.030 | 3.762 | 0.732 | 0.732 | ok |
| light-5 | 3.433 | 4.150 | 0.717 | 0.717 | ok |
| darkL-1 | 4.007 | 5.080 | 1.073 | 1.073 | ok |
| darkL-2 | 4.277 | 5.200 | 0.923 | 0.923 | ok |
| darkL-3 | 4.241 | 5.502 | 1.261 | 1.260 | ok |
| darkL-4 | 4.033 | 4.871 | 0.838 | 0.838 | ok |
| darkL-5 | 4.108 | 4.860 | 0.752 | 0.752 | ok |
| bs-light-1 (rendered dark) | 6.066 | 7.081 | 1.015 | 1.015 | ok |
| bs-light-2 (rendered dark) | 4.929 | 6.004 | 1.075 | 1.075 | ok |
| bs-dark-1 | 3.812 | 4.784 | 0.972 | 0.972 | ok |
| bs-dark-2 | 3.704 | 4.992 | 1.288 | 1.288 | ok |

- **"At 1"** is the script's first frame with f ≥ 1 − ε, which is "within 2 code values of C_full".
- The overshoots at 1.03 in `darkL-1` and `darkL-3` are +2 code values on a delta of 75, so they are still at 1.
- The largest value is 1.288 s, a margin of 0.272 s. The smallest is 0.713 s.

## The battery-saver count, as restated
**The line:** four cold starts, two with the system theme setting light and two with it dark, with the rendered theme recorded. Where the platform forces dark, no light run is owed. (1)–(4) hold at the rest pose.

It is met:
- **The four runs:**
  - `bs-light-1` and `bs-light-2` ran with the setting light (`bench-state-start.txt`: "Night mode: no").
  - `bs-dark-1` and `bs-dark-2` ran with the setting dark.
- **Rendered theme recorded:** dark in all four. C_sky is (53, 13, 70), the dark sky (`qa-engineer-bd23d-close.md`, lines 32 and 91–92; `dark-theme-forced-by-saver.png`).
- **(1)–(4), from `read-bs.txt`:**
  - (1) the max fraction before T is +0.000 in 4/4;
  - (2)(a) max back is 0 cv, and (2)(b) D is 629 / 823 / 879 / 743 ms, all ≥ 350 ms;
  - (3) the max step is 0.364, against ≤ 0.50;
  - (4) as in the table above.
- **Rest pose:** it holds. At most 47 of 66,600 samples differ by more than 6 cv, which is codec noise at the wave edges.

Excerpt (`read-bs.txt`):
```
bs-light-2: frames=67 H=f46@3.958s T(tile settled)=f57@4.929s (0.970s after H) C_sky=(53, 13, 70) C_full=(103, 33, 147) ch=B delta=77
  (2) decreases > tol after T: 0 []; rise last-0 f59@5.276 -> first-1 f67@6.004 span=727 ms -> ok
  (4) fully in 2.045 s after H -> FAIL          [old anchor; 6.004 - 4.929 = 1.075 s after T]
bs-dark-2: frames=89 H=f44@3.189s T(tile settled)=f62@3.704s (0.515s after H) C_sky=(53, 13, 70) C_full=(103, 33, 147) ch=B delta=77
  (2) decreases > tol after T: 0 []; rise last-0 f64@4.271 -> first-1 f89@4.992 span=721 ms -> ok
bs-dark-2: ch=B C_sky=70 C_full=147 eps=0.0260 | A=f71@1.401s C=112 f=0.5455 | B=f88@1.749s C=144 f=0.9610 | first-at-1 f89@1.803s C=147 | dt=348 ms D=743 ms plain=836 ms -> ok | (2)(a) max back=0 cv -> ok
```

## BD-23 (d), every item under the amended line
| Item | Verdict | Source |
|---|---|---|
| (1) | PASS 10/10 | `qa-engineer-j5.md` |
| (2) | PASS 10/10 under B-11c, D 486–733 ms | `qa-engineer-bd23d-close.md` desk |
| (3) | PASS 10/10 | `qa-engineer-j5.md` |
| (4) | **PASS 14/14** | this report |
| (5) | PASS (`tap-dark-2`) | `qa-engineer-j5.md` |
| (6) | PASS 4/4 under the Home-frame rule | `qa-engineer-bd23d-close.md` |
| (7) | PASS (`return-dark`) | `qa-engineer-j5.md` |
| Battery-saver clause | **PASS** (4 runs; (1)–(4) at 4/4; rest pose held) | this report |

## Checks
- **Re-derivation of (4) from absolute times**, by hand from the three read files — pass. 14/14 are ≤ 1.56 s, and the maximum is 1.288 s.
- **`diff j5/bd23d/bd23d.py bd23d-close/bd23d.py`** — pass (identical). The same T definition holds for all 14 runs.
- **Bound arithmetic** — pass. 2.0 − 0.44 = 1.56 s, as written in BD-23 (d) (4) at a155ca5.
- **`./gradlew check`** — not run. This is a desk check of documents, outside the task's scope, and there is no device or Gradle.

## Open issues / risks
1. **(4)'s anchor was moved after the data was seen** (the ux-designer's Open issue 1). T itself was defined before the reads, and the bound is derived rather than fitted. The weighing is the code-auditor's.
2. **In dark, the open lags T by up to ≈ 0.59 s** (`bs-dark-2`). This is informative only and sits inside (4). It is worth a look on a physical phone in the 3.0.0 device pass.

## Handoff
Next: producer — BD-23 (d) can close on the existing data. No device run is owed. Route Open issue 1 to the code-auditor.

**VERDICT (BD-23 (d) overall, under the B-11d line): PASS** — (4) is 14/14, at most 1.288 s after T against ≤ 1.56 s. The battery-saver clause is met: four runs, two per system setting, all rendered dark, (1)–(4) at 4/4 and the rest pose held. Items (1)–(3) and (5)–(7) PASS as previously read.
