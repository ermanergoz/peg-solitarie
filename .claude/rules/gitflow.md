# Gitflow rules

Branches: `main` (production only), `develop` (integration), `feature/*`,
`bugfix/*`, `release/*`, `hotfix/*`. Nothing is committed directly on `main`
or `develop`; the branch guard hook and a git pre-commit hook enforce this.

- Feature work: `git checkout -b feature/<slug> develop`. One worktree per
  concurrently working agent: `git worktree add ../<repo>-<slug> feature/<slug>`.
- Pipeline branches carry planning, docs, config and approval files only and
  merge on the producer's authority once their step's reports exist; they are
  the sole exception to the next bullet: `feature/new-product`,
  `feature/pitch`, `feature/pitch-approval`, `feature/design`,
  `feature/sync-rules`, `feature/milestone-<X.Y.Z>`, `feature/publish-<X.Y.Z>`,
  `feature/retro-<X.Y.Z>`; `feature/gsd-config`, merged by the scaffolder before
  the new-product report exists; in the plugin, `feature/retro-<slug>-<X.Y.Z>`
  after the code-auditor's diff review (rules/memory.md).
- Every other branch merges into develop only after: Check command green,
  code-auditor PASS, qa-engineer PASS, tech-lead sign-off. `git merge --no-ff`.
  Serial merges.
- Release: `git checkout -b release/<X.Y.Z> develop`; bump `VERSION`,
  changelog, full check at release fuzz count where applicable;
  `git checkout main && git merge --no-ff release/<X.Y.Z>`; `git tag v<X.Y.Z>`;
  `git checkout develop && git merge --no-ff main`; delete the branch. Autonomous.
- Hotfix: from `main`, same merge pattern, patch version bump.
- Commits: conventional commits (`feat:`, `fix:`, `docs:`, `test:`, `chore:`,
  `ci:`, `refactor:`), subject ≤ 72 chars, imperative; GSD's
  `type(NN-PP): subject` is that form with a scope. Every commit ends with the
  trailer `Co-Authored-By: Claude Fable 5.1 <noreply@anthropic.com>`.
  `.planning/`, `docs/`, `CLAUDE.md`, `AGENTS.md`, `.claude/` are committed with
  the work.
- Use the `gitflow` skill for mechanics, with the overrides above.
