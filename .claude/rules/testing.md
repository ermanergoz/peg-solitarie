# Testing rules

- Core logic (game-core, domain layers): unit tests for every public
  function; line coverage ≥ 90% (kover on Kotlin, vitest coverage on web,
  the stack's tool elsewhere).
- Games: table-driven tests for every Move type, invalid moves, terminal
  states; property tests over ≥ 1,000 seeds; fuzz harness ≥ 10,000 seeds in
  CI and ≥ 100,000 before release (rules/puzzle-design.md).
- Apps: every acceptance criterion (Given/When/Then) maps to a named test;
  repositories tested against fakes; ViewModels/state holders tested with
  Turbine or the stack's equivalent.
- UI: one UI test per screen covering its main flow (Compose UI test,
  Playwright e2e, XCTest UI).
- Deterministic tests only: no wall-clock, no network, no unseeded random,
  no sleeps.
- No disabled or skipped tests without an "approved by code-auditor" line.
- Test names read as sentences.
- TDD is required for core logic: failing test first
  (superpowers:test-driven-development).

## QA artifacts
Screenshots, recordings, logcat and console dumps, emulator snapshots, report
JSON and any helper or robot script written for a QA session live under
`.planning/qa/<task-slug>/` in the product repo, which is git-ignored. No QA
artifact — binary or helper script — is ever committed to a product repo;
product assets (icons, sounds, store images) follow the product's own rules.
The QA report lists each artifact by path with its size and what it shows and
quotes the few lines that matter (a logcat or console excerpt of at most 20
lines), so the report stands alone. The producer deletes
`.planning/qa/<task-slug>/` right after the task's STATUS.md Done line and
keeps `.planning/qa/` under 200 MB by deleting the oldest task directories
first. Reports are the record; the retro reads reports, not artifacts.
