# Gitflow rules

Branches: `main` (production only), `develop` (integration), `feature/*`,
`bugfix/*`, `release/*`, `hotfix/*`. Nothing is committed directly on `main`
or `develop`; the branch guard hook and a git pre-commit hook enforce this.

- Feature work: `git checkout -b feature/<slug> develop`. One worktree per
  concurrently working agent: `git worktree add ../<repo>-<slug> feature/<slug>`.
- Merge into develop only after: Check command green, code-auditor PASS,
  qa-engineer PASS, tech-lead sign-off. `git merge --no-ff`. Serial merges.
- Release: `git checkout -b release/<X.Y.Z> develop`; bump `VERSION`,
  changelog, full check at release fuzz count where applicable;
  `git checkout main && git merge --no-ff release/<X.Y.Z>`; `git tag v<X.Y.Z>`;
  `git checkout develop && git merge --no-ff main`; delete the branch. Autonomous.
- Hotfix: from `main`, same merge pattern, patch version bump.
- Commits: conventional commits (`feat:`, `fix:`, `docs:`, `test:`, `chore:`,
  `ci:`, `refactor:`), subject ≤ 72 chars, imperative. `.planning/`, `docs/`,
  `CLAUDE.md`, `AGENTS.md`, `.claude/` are committed with the work.
- Use the `gitflow` skill for mechanics, with the overrides above.
