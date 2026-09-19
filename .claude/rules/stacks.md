# Stack menu and template contract

The tech lead picks one stack per product and records the choice and its
trade-offs in docs/tech-design.md. Defaults by product type: game →
android-compose-game; android-app → android-compose-app; ios-app → swiftui;
kmp-app → kmp-app; web-app and website → web-app.

| Stack | Template | Use when | Trade-offs (researched 2026-09) | Developer | Required skills |
|---|---|---|---|---|---|
| android-compose-game | templates/android-compose-game | Puzzle/board games on Android | Fastest path to Play; first-class tooling; testable game-core; Android only | android-game-dev | compose-skill, compose-ui, android-viewmodel, android-coroutines, android-testing |
| android-compose-app | templates/android-compose-app | General Android apps | Same toolchain; Hilt, DataStore, Retrofit scaffold; Android only | android-app-dev | compose-skill, compose-ui, android-architecture, android-data-layer, android-viewmodel, android-coroutines, android-retrofit, android-testing |
| kmp-app | templates/kmp-app | Android + iOS from one codebase | Compose Multiplatform 1.12 (iOS stable since 2025, Skia canvas); heavy Canvas can bottleneck low-end devices; iOS build needs Xcode (not installed yet) | kmp-dev | kotlin-specialist, compose-skill |
| web-app | templates/web-app | Web apps and websites | Next.js 16 App Router, React 19, TS 5.9; Vercel deploy; async server components e2e-tested only | web-dev | react-expert, nextjs-developer, typescript-pro, playwright-expert, ui-ux-pro-max |
| swiftui | none yet (needs Xcode) | Native iOS apps | Swift 6, SwiftUI, XCTest; App Store via fastlane | ios-dev | swift-expert |
| godot | none yet | Animation-, physics-, particle-heavy 2D games | Free, no royalties; own toolchain (gdtoolkit, GUT, export templates); Play AAB needs the Gradle build template | game-engine-dev | godot-prompter:* |
| defold | none yet | Very small install size, lightweight 2D | Open source, Lua, shipped commercial mobile titles | game-engine-dev | scouted before use |
| libgdx | none yet | Code-first 2D on the JVM | Mature, less momentum than Godot in 2026 | game-engine-dev | kotlin-specialist |
| web-twa | none yet | Reuse a web game/app on Play | Allowed only as a TWA over a real PWA (HTTPS, service worker, offline basics, Lighthouse ≥ 80) that passes the minimum-functionality policy; raw WebView wrappers are rejected | web-dev | react-expert, typescript-pro |

Rules:
- A stack without a template cannot start phase 1. The tech lead escalates to
  the producer, who schedules `feature/template-<stack>` in the plugin
  (tech-lead + the field's developer), reviewed and released like any change.
- Each template ships with a `<stack>.md` rules file and a row above.

Template contract (verified by scripts/check-template.sh):
1. `CLAUDE.md` stub with lines `Product: <default type>`, `Stack: <stack>`,
   `Check command: <cmd>`, `Build command: <cmd producing an installable or
   deployable artifact>`.
2. Root `VERSION` = `0.1.0`, read by the build.
3. The check command runs format/lint, static analysis, unit tests, UI/e2e
   tests where present, and (games) a seeded fuzz harness honouring
   `FUZZ_SEEDS`; non-zero exit on any failure.
4. Placeholders `com.ergozsoft.template`, `Ergoz Template`, `ergoz-template`;
   the template builds unmodified.
5. Games: testable core with GameState, Move, GameRules, GameReducer,
   PuzzleGenerator, PuzzleValidator, Solver, ScoringEngine. Apps: layered
   `ui/`, `domain/`, `data/` (names kept across stacks).
6. `THIRD_PARTY.md`, `.gitignore`, `.editorconfig`, CI workflow running the
   check command, and a gated publish script: `scripts/play-upload.sh`,
   `scripts/appstore-upload.sh`, or `scripts/deploy.sh`.
