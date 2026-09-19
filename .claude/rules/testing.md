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
