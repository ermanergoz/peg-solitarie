# ux-designer report — criterion 15e: the cold-start gate, re-specified in the UX spec

Date: 2026-09-28
Branch: feature/phase-15-ux-refresh (f9ad048..this commit — `docs/ux-spec.md` and this report only;
no source file, no `dimens.xml`, no build)
Skills loaded: ergoz-soft:standards (role: ux-designer), android-accessibility, compose-skill
Ran in parallel with: the phase-15 build in this same worktree (`app/src/`, `domain/src/`,
`dimens.xml` — untouched here; the developer's modified `GameScreenSoundTest.kt` was left unstaged),
the phase-14 release track
Loops: 0
Lessons applied: none

Ran on Opus 5 1M (`claude-opus-5[1m]`); `agents/ux-designer.md` pins `model: fable`, whose allowance
is exhausted, so this ran on Opus per the founder's decision **Q-003**. A staffing note on an
authoring task, not an independence question: this is the spec's own role editing its own spec, and
nothing here reviews or approves anyone's work.

## Summary

Criterion **15e** is replaced in `docs/ux-spec.md` with the gate the tech-lead ruled: an interleaved
A/B delta against a **same-sitting null control**, threshold `max(2 × floor, 100 ms)`, **VOID** above
a 150 ms floor, run **once on the follow-up slice that lands §4.2.2** with `develop` as arm A. The
≤ 950 ms absolute leaves the criterion entirely and is reattributed to **PLAT-08 / phase 14** by a
new decision **D-03j**, with the studio's under-2-seconds bar named as the bar of record. §6.5 now
records as a **named gap** that that bar has never been measured on a physical phone, with the
tech-lead's recommendation and its decided default. The retired wording is kept verbatim beneath the
new criterion, because two reviewers returned verdicts against it.

## Outputs

- `docs/ux-spec.md` — amended in seven places, additively, in the document's own amendment style:
  - `:125-154` — header **Amendments**: new "Fifth revision — 2026-09-28" note naming the ruling,
    the statistic error, both null floors, the 1.83× host-load fact, and exactly what changed.
  - `:175` (§1, the D-03 row) — one-line pointer; the "≤ 950 ms cold-start gate" sentence is left
    as it read.
  - `:543-548` (D-03g, whose text runs `:530-548`) — second dated amendment pointer; every hard rule
    for Home's first frame is untouched and still binds.
  - `:587-613` (§3) — **D-03j**, new: the ≤ 950 ms absolute is not a phase-15 gate but a tracked
    target owned by PLAT-08 / phase 14; the bar of record is rules/studio.md's under-2-seconds on a
    named mid-range physical phone; phase 15's gate is relative only.
  - `:1354-1360`, `:1407-1410`, `:1418-1424` (§6.5) — pointer on the section's opening claim,
    pointer on its "Handoff" line ("**not** before this phase closes"), and a superseded-in-part
    banner at the head of the first measurement contract naming which three parts stand (launch
    identity, seeded ladder, evidence location) and which two are retired (blocked arms, the pass
    condition). The first edition's text is untouched.
  - `:1462-1553` (§6.5) — **"How C1 is measured for this phase — second edition"**: why it changed,
    a *which-builds-this-applies-to* table, eight recorded preconditions, a three-step protocol
    (null control first), a three-outcome verdict table, the two computed thresholds, the
    raises-the-bar paragraph, where the numbers go, and why **15f does not travel with 15e**. Filed
    as a named subsection and not a decision, on the same reasoning the first edition used (a gate
    procedure is not a design choice).
  - `:1555-1582` (§6.5) — **"The ≤ 2 s launch bar, and the fact that it has never been measured on
    a phone"**: the bar, the gap, the owner, the tech-lead's recommendation, the decided default
    (proceed with the bar **recorded as unverified**, never silently passed), and where the money
    question goes (the phase-14 release track; no founder question opened from this document).
  - `:2069`, `:2071-2092`, `:2094-2109`, `:2111-2116` (§14) — the group header gains D-03j;
    criterion **15e** replaced; the retired first edition quoted beneath it with both reasons for
    its retirement; **15f** gains a pointer that it is unchanged and stays a phase-15 sign-off item
    on this build.
  - `:2250-2257` (§16) — dated note on the PLAT-08 (C1) row (`:2237`): one substitution
    (second-edition procedure, relative gate, no absolute number) and one addition (both absolutes
    are phase-14's; the 2 s bar is unmeasured).
- `.planning/reports/phase-15/ux-designer-coldstart-criterion.md` — this report.

## Checks

**No build, no Gradle task and no emulator command was run, and that is deliberate, not an
omission.** The task forbade it: a developer is building concurrently in this worktree, and two
concurrent Gradle builds in one worktree corrupt the test-result store — a proven defect here
(`tech-lead-depthsweep.md` F2). Nothing in this change is executable: it is prose in
`docs/ux-spec.md`, no source file, no resource, no test. The check a document amendment can carry is
that every number and citation in it is the ruling's own, verified at the ruling and at the files
the ruling cites; that is the list below. `./gradlew check` on this diff would exercise nothing the
diff touches, and the phase-15 build that must pass it is the developer's, not mine.

- Ruling read in full before editing: `.planning/reports/phase-15/tech-lead-coldstart-gate.md:1-233`
  — pass. Every figure below is quoted from it, not re-derived by me.
- Null floors, as the ruling records them: **57 ms** (developer, `15-BUILD-EVIDENCE.md:52-53`, arm
  medians 957 / 1014 ms, within-arm spreads 172 / 97 ms) and **59 ms** (qa-engineer,
  `qa-engineer.md:162-163`, arm medians 1832 / 1773 ms, spreads 253 / 225 ms, host loads 6.43 /
  8.49) — pass; both appear in the header note, in D-03j, in §6.5's second edition and in the
  retired-15e block, with those `file:line` citations attached.
- The statistic error, as the ruling states it: Phase 6's **46 ms** was the **within-arm range** of
  nine samples of one build (603–649 ms around a **633 ms** median,
  `06-BUILD-EVIDENCE.md:319-320`), promoted to a threshold on a **between-arm median difference** —
  pass; stated in those words in all four places, and it is named as the tech-lead's own
  specification error, not laundered.
- Threshold arithmetic quoted, not recomputed independently: `max(2 × 57, 100)` = **114 ms** and
  `max(2 × 59, 100)` = **118 ms** — pass (`tech-lead-coldstart-gate.md:189`, and §3 "~114–118 ms").
- Host-load fact: same AVD, **957 ms** against **1832 ms** = **1.83×** with nothing changing but
  host load; null delta held **57 → 59 ms** across that swing (5.8% and 3.3% of the pooled medians)
  — pass; used as the reason the absolute is retired and the relative kept, and as the reason QA's
  ~1.8 s "is **neither a pass nor a fail**" against the 2 s bar.
- Same-AVD identity (the ruling's correction to the brief): `qa-engineer.md:41-45` and
  `15-BUILD-EVIDENCE.md:41-43` name the same `Pixel_9a` / API 36 / `emulator-5566` /
  `:app:installDebug` — pass; the spec no longer contains any device-class explanation of the gap.
- Reattribution has a source, so it cannot read as a dropped bar: Phase 6 wrote its figure as the
  before-picture "**PLAT-08 measures against in Phase 14**" (`06-BUILD-EVIDENCE.md:308-309`) — pass;
  quoted in D-03j, and the first edition's own condition-(1)-only escalation clause is cited as the
  routing D-03j generalises.
- Every criterion and section touched, by `file:line` in the amended file — pass:
  `docs/ux-spec.md:125-154` (header), `:175` (§1 D-03 row), `:543-548` (D-03g pointer), `:587-613`
  (**D-03j**), `:1354-1360` (§6.5 opening pointer), `:1407-1410` (§6.5 handoff pointer),
  `:1418-1424` (first-edition superseded banner), `:1462-1553` (second edition), `:1555-1582`
  (the ≤ 2 s gap), `:2069` (criterion group header), `:2071-2092` (criterion **15e**, replaced),
  `:2094-2109` (15e first edition, retired, kept legible), `:2111-2116` (criterion **15f**
  pointer), `:2250-2257` (§16 note).
- The task's two "unmistakably clear" requirements — pass: which builds the gate applies to is a
  three-row table at `:1482-1486` **and** the criterion's own first sentence at `:2072-2074`; and
  "it passed" cannot mean "too noisy to tell" because VOID is a row of the verdict table
  (`:1524`), a clause of the criterion (`:2083-2085`), a consequence of any failed precondition
  (`:1488-1489`) and the penalty for a missing null table (`:1541-1542`).
- Nothing reopened — pass by inspection of the diff's scope: D-03i and §4.2.2, §5.1, the audio
  rulings (D-01h/D-01i), §4.x, §11, §12, §13 and criteria 15g–15j are unedited. §14's only
  substantive edit is 15e; 15f's edit is a dated pointer that changes no observable.
- Files the task put out of bounds were not opened for writing — pass: no `app/src/`, no
  `domain/src/`, no `dimens.xml`, no `15-PLAN.md`, no `STATUS.md`, no `ROADMAP.md`, no `FOUNDER.md`,
  no `VERSION`, and no report authored by another role. The developer's uncommitted
  `app/src/test/.../GameScreenSoundTest.kt` was left unstaged; the commit names its two paths
  explicitly.
- Markdown structure check on the two inserted quote blocks — pass: a blank line separates the
  retired-15e blockquote from criterion 15f (`:2109-2110`), so 15f is not lazily absorbed into the
  quote, and the same for the first-edition banner at `:1424-1426`.

## Open issues / risks

- **The gate now has an owner who is not phase 15, and that hand-off is the risk.** 15e runs once,
  on the follow-up slice that lands §4.2.2 / D-03i. If that slice is planned without the criterion
  attached to it, the gate quietly evaporates — the failure mode is no longer a bad number, it is
  no number and nobody holding it. The spec says the slice "does not close without them"; the
  producer has to make that a line in the slice's plan.
- **Phase 15 now closes with 15e `:NOT RUN`, by ruling.** That is the correct record per the ruling,
  and it will look like an unclosed gate to anyone reading only the criteria list. The pointer is in
  the criterion, in §6.5's build table and in the header note; it cannot be made more visible from
  inside this document.
- **The 2 s bar remains unverified after this edit.** Recording the gap does not close it. It is
  PLAT-08 / phase-14's, it has a decided default, and the physical-device purchase is money and so
  the founder's — routed by the tech-lead into the phase-14 release track. I have not queued a
  `Q-<NNN>` and am not re-routing it; if the producer would rather it were a founder question, it is
  the producer's to open, non-blocking.
- **Criterion 15f is honest but weak, and I have not strengthened it.** It stays "verified by
  reading plus observation" because no instrument in the repo can see time-to-full-display and the
  placeholder frame is shorter than QA's 250 ms screenshot interval (`qa-engineer.md:144`). The
  instrument that would fix it (Macrobenchmark + `reportFullyDrawn()`) is new infrastructure and not
  mine to specify.
- **Two `Pixel_9`-class emulators and concurrent Gradle builds are the protocol's main enemy.** The
  preconditions make a noisy host a VOID rather than a bad pass, which is the right trade, but on a
  host like the one the two nulls ran on a void could be the *usual* outcome — the studio-side fix
  is one build, one worktree, one agent at a time, which is not this document's to rule.

## Handoff

Next: **producer** — three things follow from this edit, none of them mine.

1. **Attach 15e to the follow-up slice.** Its plan must carry the criterion and §6.5's second
   edition, with `develop` as arm A, or the gate has no owner. Phase 15's own closing record for
   15e is `:NOT RUN`.
2. **Carry the ≤ 2 s physical-device gap into the phase-14 release track**, with the decided
   default: proceed on the interleaved emulator gate plus the Macrobenchmark task and record the
   absolute bar as **unverified** in the release evidence — never as a silent pass. The test-phone
   purchase is the founder's call and the tech-lead routed it there; no question is queued from this
   document.
3. **The instrument task** (`:macrobenchmark` + `reportFullyDrawn()`) is the one thing that would
   make 15f decidable, per the ruling. It is the producer's to schedule and the android-game-dev's
   to build; nothing waits on it now.

- **qa-engineer** — do not re-run 15e against this build; `:NOT RUN` is correct. When the follow-up
  slice is ready, run §6.5's second edition exactly: null control first with the same build in both
  positions, then interleaved A/B/A/B blocks of five, ≥ 20 samples per arm, and report **VOID**
  rather than a number if `floor > 150 ms`. Criterion **15f is still yours on this build** and does
  not move with 15e.
- **android-game-dev** — nothing here. No source file, no resource and no `dimens.xml` token
  changed; the §4.2.2 / D-03i layout ruling you are building against is untouched by this edit.
- **code-auditor** — the diff is prose in one document plus this report. The two checks worth making
  are that no bar was lowered (the retired criterion is quoted verbatim beside its replacement, at
  `docs/ux-spec.md:2094-2109`) and that the absolute's reattribution is sourced rather than asserted
  (`06-BUILD-EVIDENCE.md:308-309`, quoted in D-03j).
