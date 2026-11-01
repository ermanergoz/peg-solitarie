# Draft corrections to `CHANGELOG.md` `[3.0.0]` — applied at the cut (checklist 2.4, 2.4a, 2.5)

Not applied to `CHANGELOG.md` yet: the section is final only on `release/3.0.0`. The existing
`[3.0.0] — unreleased` section on `develop` stays the base; these are the edits.

## Remove stale pending markers (merged)
- "A colorblind palette *(pending: `feature/colour-vision`)*" → drop the marker (merged `1bc442f`).
- "A Licenses screen *(pending: `feature/licences-screen`)*" → drop the marker (merged `bc95b2a`).
- "A new glossy look *(pending: `feature/candy-backdrop`)*" → keep until candy merges; delete with its
  store lines if it does not make the cut.

## Add — Added
- **Play the board with TalkBack.** With TalkBack on, every cell of the board is a stop that says
  what is on it; select a peg and jump with a double-tap or from TalkBack's actions menu, and each
  jump is spoken with the pegs left. The tutorial speaks each step's board and instruction, and the
  Results screen reads its summary once.
  *(Allowed by checklist 1.1a: merge `d251c3b`. Store README "Accessibility claims" must be updated in
  the same pass.)*

## Add — Changed
- **Turning off animations takes effect while you play.** Switching "Remove animations" on or off in
  the system settings now reaches the game without restarting it (within about ten seconds, the
  system's own delay). *(RM, `053e9cc`)*
- **When a hint cannot work out a position in time, it says so** instead of claiming there is no
  winning move; "no winning move" now appears only when it is proven. *(R1, `b1a6cf9`; optional raise
  per checklist 2.5a — store copy only if raised there too.)*

## Add — Fixed
- **Home no longer jumps when your Continue chips appear** a moment after launch. *(home-chip-arrival)*
- **TalkBack now reads the game timer's value,** in whole minutes; before, it said only "Elapsed
  time". *(home-chip-arrival, 18f/B-3. The every-few-seconds re-speaking, B-2, existed only on that
  branch's own build and never shipped, so it is not a fix to announce.)*
- **Navigation buttons stay readable in dark mode** with three-button navigation. *(home-chip-arrival, B-1)*

## Fix — Under the hood (stale)
- Replace the bullet "Two studio-rule gaps are **open** … R-2 and R-7" with: "`check` now carries a
  `:domain` line-coverage gate (kover, bound 90 %) and an exhaustive fuzz harness over the whole
  10,000-level Challenge space (`FUZZ_SEEDS`), merged with `feature/phase-14-check-gates` (`dbd0a6c`)."
  The figures come from the 3.1/3.3 run at the cut.
- Add: "Board speech is built by a pure model with no Android dependency, pinned by an architecture
  test; the accessibility overlay is semantics-only above the unchanged tap path."
- Add: "The Licenses screen's Google SDK notices are generated at build time and bundled under
  `assets/licenses/`."

## At the cut
- Date the heading; re-count `git rev-list v1.0..release/3.0.0 --count` (1260 at `d251c3b`) and the
  `feat` count in the preamble ("810 commits", "154 feat" are from 2026-10-21 and are stale).
- `grep -n 'pending:' CHANGELOG.md` prints nothing (2.4a).
